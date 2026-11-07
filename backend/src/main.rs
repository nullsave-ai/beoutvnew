mod admin;
mod error;
mod mw;
mod public;

use std::{net::SocketAddr, sync::Arc, time::Duration};

use axum::{middleware, routing::get, Router};
use redis::aio::ConnectionManager;
use sqlx::postgres::PgPoolOptions;
use tower_http::{compression::CompressionLayer, cors::CorsLayer, limit::RequestBodyLimitLayer, timeout::TimeoutLayer, trace::TraceLayer};
use tracing_subscriber::EnvFilter;

pub struct Config {
    pub rate_per_min: u64,
    pub login_per_min: u64,
    /// سر توقيع روابط التشغيل (اختياري). عند وجوده تُضاف exp وsig للرابط.
    pub sign_secret: Option<String>,
    pub sign_ttl_secs: i64,
    pub cors_origin: Option<String>,
}

#[derive(Clone)]
pub struct AppState {
    pub db: sqlx::PgPool,
    pub redis: ConnectionManager,
    pub cfg: Arc<Config>,
}

fn env(k: &str) -> Option<String> {
    std::env::var(k).ok().filter(|v| !v.is_empty())
}

#[tokio::main]
async fn main() -> anyhow::Result<()> {
    tracing_subscriber::fmt()
        .json()
        .with_env_filter(EnvFilter::try_from_default_env().unwrap_or_else(|_| EnvFilter::new("info,tower_http=info")))
        .init();

    let db = PgPoolOptions::new()
        .max_connections(env("DB_POOL").and_then(|v| v.parse().ok()).unwrap_or(16))
        .acquire_timeout(Duration::from_secs(5))
        .connect(&env("DATABASE_URL").expect("DATABASE_URL مطلوب"))
        .await?;
    sqlx::migrate!("./migrations").run(&db).await?;

    let redis = ConnectionManager::new(redis::Client::open(env("REDIS_URL").expect("REDIS_URL مطلوب"))?).await?;

    let cfg = Arc::new(Config {
        rate_per_min: env("RATE_PER_MIN").and_then(|v| v.parse().ok()).unwrap_or(240),
        login_per_min: env("LOGIN_PER_MIN").and_then(|v| v.parse().ok()).unwrap_or(10),
        sign_secret: env("STREAM_SIGN_SECRET"),
        sign_ttl_secs: env("STREAM_SIGN_TTL").and_then(|v| v.parse().ok()).unwrap_or(900),
        cors_origin: env("ADMIN_ORIGIN"),
    });
    let state = AppState { db, redis, cfg: cfg.clone() };

    admin::seed_admin(&state).await?;

    // CORS: لوحة التحكم فقط (التطبيق لا يحتاج CORS)
    let cors = match &cfg.cors_origin {
        Some(o) => CorsLayer::new()
            .allow_origin(o.parse::<axum::http::HeaderValue>()?)
            .allow_methods([axum::http::Method::GET, axum::http::Method::POST, axum::http::Method::PUT, axum::http::Method::DELETE])
            .allow_headers([axum::http::header::AUTHORIZATION, axum::http::header::CONTENT_TYPE]),
        None => CorsLayer::new(),
    };

    let v1 = Router::new()
        .merge(public::routes())
        .nest("/admin", admin::routes(state.clone()))
        .layer(middleware::from_fn(mw::etag))
        .layer(middleware::from_fn_with_state(state.clone(), mw::rate_limit));

    let app = Router::new()
        .route("/health", get(|| async { "ok" }))
        .nest("/v1", v1)
        .layer(cors)
        .layer(CompressionLayer::new())
        .layer(RequestBodyLimitLayer::new(256 * 1024))
        .layer(TimeoutLayer::with_status_code(axum::http::StatusCode::REQUEST_TIMEOUT, Duration::from_secs(15)))
        .layer(TraceLayer::new_for_http())
        .with_state(state);

    let addr: SocketAddr = env("BIND").unwrap_or_else(|| "0.0.0.0:8080".into()).parse()?;
    tracing::info!(%addr, "listening (ضع الخدمة خلف HTTPS: nginx/caddy)");
    let listener = tokio::net::TcpListener::bind(addr).await?;
    axum::serve(listener, app.into_make_service_with_connect_info::<SocketAddr>())
        .with_graceful_shutdown(async { let _ = tokio::signal::ctrl_c().await; })
        .await?;
    Ok(())
}
