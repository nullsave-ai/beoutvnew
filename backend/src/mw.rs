use std::{
    collections::hash_map::DefaultHasher,
    hash::{Hash, Hasher},
    net::SocketAddr,
};

use axum::{
    body::{to_bytes, Body},
    extract::{ConnectInfo, Request, State},
    http::{header, HeaderValue, Method, StatusCode},
    middleware::Next,
    response::{IntoResponse, Response},
};
use redis::AsyncCommands;

use crate::{error::ApiError, AppState};

fn client_key(req: &Request) -> String {
    if let Some(d) = req.headers().get("x-device").and_then(|v| v.to_str().ok()) {
        if (8..=64).contains(&d.len()) { return format!("d:{d}"); }
    }
    if let Some(f) = req.headers().get("x-forwarded-for").and_then(|v| v.to_str().ok()) {
        if let Some(ip) = f.split(',').next() { return format!("i:{}", ip.trim()); }
    }
    req.extensions()
        .get::<ConnectInfo<SocketAddr>>()
        .map(|c| format!("i:{}", c.0.ip()))
        .unwrap_or_else(|| "i:unknown".into())
}

/// تحديد المعدل بنافذة ثابتة (دقيقة) في Redis. للدخول حد أقل.
pub async fn rate_limit(State(st): State<AppState>, req: Request, next: Next) -> Result<Response, ApiError> {
    let is_login = req.uri().path().ends_with("/admin/login");
    let limit = if is_login { st.cfg.login_per_min } else { st.cfg.rate_per_min };
    let minute = chrono::Utc::now().timestamp() / 60;
    let key = format!("rl:{}:{}:{}", if is_login { "login" } else { "api" }, client_key(&req), minute);
    let mut r = st.redis.clone();
    // فشل Redis لا يجب أن يوقف الخدمة: نسمح بالطلب
    if let Ok(n) = r.incr::<_, _, u64>(&key, 1).await {
        if n == 1 { let _: Result<(), _> = r.expire(&key, 70).await; }
        if n > limit { return Err(ApiError::TooMany); }
    }
    Ok(next.run(req).await)
}

/// ETag للـ GET: يدعم الطلبات الشرطية (If-None-Match) فيوفر النطاق الترددي على التطبيق.
pub async fn etag(req: Request, next: Next) -> Response {
    let is_get = req.method() == Method::GET;
    let inm = req.headers().get(header::IF_NONE_MATCH).cloned();
    let res = next.run(req).await;
    if !is_get || res.status() != StatusCode::OK { return res; }

    let (mut parts, body) = res.into_parts();
    let bytes = match to_bytes(body, 8 * 1024 * 1024).await {
        Ok(b) => b,
        Err(_) => return StatusCode::INTERNAL_SERVER_ERROR.into_response(),
    };
    let mut h = DefaultHasher::new();
    bytes.hash(&mut h);
    let tag = format!("\"{:x}\"", h.finish());
    let hv = HeaderValue::from_str(&tag).unwrap();
    if inm.as_ref() == Some(&hv) {
        let mut r = Response::new(Body::empty());
        *r.status_mut() = StatusCode::NOT_MODIFIED;
        r.headers_mut().insert(header::ETAG, hv);
        return r;
    }
    parts.headers.insert(header::ETAG, hv);
    parts.headers.insert(header::CACHE_CONTROL, HeaderValue::from_static("private, max-age=0, must-revalidate"));
    parts.headers.remove(header::CONTENT_LENGTH);
    Response::from_parts(parts, Body::from(bytes))
}
