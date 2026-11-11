//! واجهة الإدارة: مصادقة بجلسات Redis + صلاحيات (admin | editor). كل تعديل يبطل كاش التطبيق.

use argon2::{
    password_hash::{rand_core::OsRng, PasswordHash, PasswordHasher, PasswordVerifier, SaltString},
    Argon2,
};
use axum::{
    extract::{Path, Query, Request, State},
    http::{header, StatusCode},
    middleware::{self, Next},
    response::Response,
    routing::{get, post, put},
    Extension, Json, Router,
};
use rand::RngCore;
use redis::AsyncCommands;
use serde::Deserialize;
use serde_json::{json, Value};
use uuid::Uuid;

use crate::{error::{ApiError, ApiResult}, public::bump_cache, AppState};

const SESSION_TTL: u64 = 12 * 3600;

#[derive(Clone)]
pub struct Auth {
    pub user: Uuid,
    pub role: String,
}

impl Auth {
    fn admin(&self) -> ApiResult<()> {
        if self.role == "admin" { Ok(()) } else { Err(ApiError::Forbidden) }
    }
}

pub fn routes(st: AppState) -> Router<AppState> {
    let protected = Router::new()
        .route("/logout", post(logout))
        .route("/stats", get(stats))
        .route("/items", get(items_list).post(item_create))
        .route("/items/:id", put(item_update).delete(item_delete))
        .route("/items/:id/seasons", get(seasons_list).post(season_create))
        .route("/seasons/:id", put(season_update).delete(season_delete))
        .route("/seasons/:id/episodes", post(episode_create))
        .route("/episodes/:id", put(episode_update).delete(episode_delete))
        .route("/categories", get(cats_list).post(cat_create))
        .route("/categories/:id", put(cat_update).delete(cat_delete))
        .route("/home-sections", get(home_get).put(home_put))
        .route("/config", get(config_get).put(config_put))
        .route("/users", get(users_list))
        .route("/users/:id", put(user_update))
        .route("/users/:id/subscription", post(sub_grant))
        .route("/notifications", get(notif_list).post(notif_create))
        .route("/ads", get(ads_list).post(ad_create))
        .route("/ads/:id", put(ad_update).delete(ad_delete))
        .route_layer(middleware::from_fn_with_state(st, require_auth));

    Router::new().route("/login", post(login)).merge(protected)
}

// ---------- المصادقة ----------

pub async fn seed_admin(st: &AppState) -> anyhow::Result<()> {
    let (Ok(email), Ok(pass)) = (std::env::var("ADMIN_EMAIL"), std::env::var("ADMIN_PASSWORD")) else { return Ok(()) };
    if email.is_empty() || pass.len() < 10 { return Ok(()); }
    let exists: bool = sqlx::query_scalar("SELECT EXISTS(SELECT 1 FROM users WHERE role = 'admin')").fetch_one(&st.db).await?;
    if exists { return Ok(()); }
    let hash = hash_pw(&pass).map_err(|e| anyhow::anyhow!(e))?;
    sqlx::query("INSERT INTO users(email, pass_hash, role) VALUES($1, $2, 'admin')").bind(email).bind(hash).execute(&st.db).await?;
    tracing::info!("تم إنشاء حساب المسؤول الأول");
    Ok(())
}

fn hash_pw(p: &str) -> Result<String, String> {
    let salt = SaltString::generate(&mut OsRng);
    Argon2::default().hash_password(p.as_bytes(), &salt).map(|h| h.to_string()).map_err(|e| e.to_string())
}

fn verify_pw(p: &str, hash: &str) -> bool {
    PasswordHash::new(hash).map(|h| Argon2::default().verify_password(p.as_bytes(), &h).is_ok()).unwrap_or(false)
}

#[derive(Deserialize)]
struct LoginIn { email: String, password: String }

async fn login(State(st): State<AppState>, Json(b): Json<LoginIn>) -> ApiResult<Json<Value>> {
    let row: Option<(Uuid, Option<String>, String, bool)> =
        sqlx::query_as("SELECT id, pass_hash, role, banned FROM users WHERE email = $1 AND role IN ('admin','editor')")
            .bind(b.email.trim().to_lowercase()).fetch_optional(&st.db).await?;
    // نفس الزمن تقريباً عند عدم وجود المستخدم
    let (id, hash, role, banned) = match row {
        Some(r) => r,
        None => { let _ = verify_pw(&b.password, "$argon2id$v=19$m=19456,t=2,p=1$c29tZXNhbHRzb21l$AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"); return Err(ApiError::Unauthorized) }
    };
    if banned || !verify_pw(&b.password, hash.as_deref().unwrap_or("")) { return Err(ApiError::Unauthorized); }

    let mut raw = [0u8; 32];
    rand::thread_rng().fill_bytes(&mut raw);
    let token = hex::encode(raw);
    let mut r = st.redis.clone();
    let _: () = r.set_ex(format!("sess:{token}"), format!("{id}:{role}"), SESSION_TTL).await?;
    Ok(Json(json!({ "token": token, "role": role })))
}

async fn require_auth(State(st): State<AppState>, mut req: Request, next: Next) -> Result<Response, ApiError> {
    let token = req.headers().get(header::AUTHORIZATION).and_then(|v| v.to_str().ok())
        .and_then(|v| v.strip_prefix("Bearer ")).ok_or(ApiError::Unauthorized)?.to_string();
    if token.len() != 64 || !token.chars().all(|c| c.is_ascii_hexdigit()) { return Err(ApiError::Unauthorized); }
    let mut r = st.redis.clone();
    let v: Option<String> = r.get(format!("sess:{token}")).await?;
    let v = v.ok_or(ApiError::Unauthorized)?;
    let (u, role) = v.split_once(':').ok_or(ApiError::Unauthorized)?;
    req.extensions_mut().insert(Auth { user: Uuid::parse_str(u).map_err(|_| ApiError::Unauthorized)?, role: role.to_string() });
    // الجلسة المنزلقة
    let _: Result<bool, _> = r.expire(format!("sess:{token}"), SESSION_TTL as i64).await;
    Ok(next.run(req).await)
}

async fn logout(State(st): State<AppState>, req: axum::http::HeaderMap) -> ApiResult<StatusCode> {
    if let Some(t) = req.get(header::AUTHORIZATION).and_then(|v| v.to_str().ok()).and_then(|v| v.strip_prefix("Bearer ")) {
        let mut r = st.redis.clone();
        let _: Result<i64, _> = r.del(format!("sess:{t}")).await;
    }
    Ok(StatusCode::NO_CONTENT)
}

// ---------- مساعدات ----------

async fn rows(st: &AppState, sql: &str) -> ApiResult<Json<Value>> {
    let v: Value = sqlx::query_scalar(&format!("SELECT COALESCE(jsonb_agg(t), '[]'::jsonb) FROM ({sql}) t"))
        .fetch_one(&st.db).await?;
    Ok(Json(v))
}

fn id_or_new(id: Option<String>) -> ApiResult<String> {
    match id {
        Some(i) if !i.is_empty() => {
            if i.len() > 100 || !i.chars().all(|c| c.is_ascii_alphanumeric() || c == '-' || c == '_') {
                Err(ApiError::BadRequest("id".into()))
            } else { Ok(i) }
        }
        _ => Ok(Uuid::new_v4().to_string()),
    }
}

fn kind_ok(k: &str) -> ApiResult<()> {
    if ["movie", "series", "channel"].contains(&k) { Ok(()) } else { Err(ApiError::BadRequest("kind".into())) }
}

fn url_ok(u: &Option<String>) -> ApiResult<()> {
    match u {
        Some(s) if !s.is_empty() && !(s.starts_with("https://") || s.starts_with("http://")) => Err(ApiError::BadRequest("url".into())),
        _ => Ok(()),
    }
}

async fn done(st: &AppState) -> ApiResult<StatusCode> {
    bump_cache(st).await;
    Ok(StatusCode::NO_CONTENT)
}

// ---------- الإحصائيات ----------

async fn stats(State(st): State<AppState>) -> ApiResult<Json<Value>> {
    let counts: Value = sqlx::query_scalar(
        "SELECT jsonb_build_object(
            'movies', (SELECT count(*) FROM items WHERE kind='movie'),
            'series', (SELECT count(*) FROM items WHERE kind='series'),
            'channels', (SELECT count(*) FROM items WHERE kind='channel'),
            'users', (SELECT count(*) FROM users WHERE role='user'),
            'activeSubs', (SELECT count(*) FROM subscriptions WHERE expires_at > now()),
            'plays7d', (SELECT COALESCE(sum(plays),0) FROM view_stats WHERE day > CURRENT_DATE - 7))",
    ).fetch_one(&st.db).await?;
    let top: Value = sqlx::query_scalar(
        "SELECT COALESCE(jsonb_agg(t), '[]'::jsonb) FROM (
            SELECT i.id, i.title, sum(v.plays) AS plays FROM view_stats v JOIN items i ON i.id = v.item_id
            WHERE v.day > CURRENT_DATE - 7 GROUP BY i.id, i.title ORDER BY plays DESC LIMIT 10) t",
    ).fetch_one(&st.db).await?;
    Ok(Json(json!({ "counts": counts, "top": top })))
}

// ---------- العناصر ----------

#[derive(Deserialize)]
struct ItemsQ { kind: Option<String>, q: Option<String>, cursor: Option<String> }

async fn items_list(State(st): State<AppState>, Query(q): Query<ItemsQ>) -> ApiResult<Json<Value>> {
    let off: i64 = q.cursor.and_then(|c| c.parse().ok()).filter(|n: &i64| *n >= 0).unwrap_or(0);
    let pat = q.q.map(|s| format!("%{}%", s.replace('%', "\\%").replace('_', "\\_")));
    let v: Value = sqlx::query_scalar(
        "SELECT COALESCE(jsonb_agg(t), '[]'::jsonb) FROM (
            SELECT id, kind, title, description, poster, backdrop, logo, year, duration_min AS \"durationMin\", quality,
                   category_id AS \"categoryId\", live, online, featured, premium, published, position,
                   stream_url AS \"streamUrl\", created_at AS \"createdAt\"
            FROM items WHERE ($1::text IS NULL OR kind = $1) AND ($2::text IS NULL OR title ILIKE $2)
            ORDER BY created_at DESC LIMIT 50 OFFSET $3) t",
    ).bind(q.kind).bind(pat).bind(off).fetch_one(&st.db).await?;
    Ok(Json(v))
}

#[derive(Deserialize)]
struct ItemIn {
    id: Option<String>,
    kind: String,
    title: String,
    description: Option<String>,
    poster: Option<String>,
    backdrop: Option<String>,
    logo: Option<String>,
    year: Option<i32>,
    #[serde(rename = "durationMin")]
    duration_min: Option<i32>,
    quality: Option<String>,
    #[serde(rename = "categoryId")]
    category_id: Option<String>,
    #[serde(default)] live: bool,
    #[serde(default = "yes")] online: bool,
    #[serde(default)] featured: bool,
    #[serde(default)] premium: bool,
    #[serde(default = "yes")] published: bool,
    #[serde(default)] position: i32,
    #[serde(rename = "streamUrl")]
    stream_url: Option<String>,
}
fn yes() -> bool { true }

const UPSERT_ITEM: &str = "INSERT INTO items(id,kind,title,description,poster,backdrop,logo,year,duration_min,quality,category_id,live,online,featured,premium,published,position,stream_url)
    VALUES($1,$2,$3,$4,$5,$6,$7,$8,$9,$10,$11,$12,$13,$14,$15,$16,$17,$18)
    ON CONFLICT (id) DO UPDATE SET kind=$2,title=$3,description=$4,poster=$5,backdrop=$6,logo=$7,year=$8,duration_min=$9,quality=$10,
      category_id=$11,live=$12,online=$13,featured=$14,premium=$15,published=$16,position=$17,stream_url=$18";

async fn save_item(st: &AppState, id: String, b: ItemIn) -> ApiResult<()> {
    kind_ok(&b.kind)?;
    url_ok(&b.stream_url)?;
    if b.title.trim().is_empty() || b.title.len() > 300 { return Err(ApiError::BadRequest("title".into())); }
    sqlx::query(UPSERT_ITEM)
        .bind(id).bind(&b.kind).bind(b.title.trim()).bind(b.description).bind(b.poster).bind(b.backdrop).bind(b.logo)
        .bind(b.year).bind(b.duration_min).bind(b.quality).bind(b.category_id.filter(|c| !c.is_empty()))
        .bind(b.live).bind(b.online).bind(b.featured).bind(b.premium).bind(b.published).bind(b.position)
        .bind(b.stream_url.filter(|u| !u.is_empty()))
        .execute(&st.db).await?;
    Ok(())
}

async fn item_create(State(st): State<AppState>, Json(mut b): Json<ItemIn>) -> ApiResult<Json<Value>> {
    let id = id_or_new(b.id.take())?;
    save_item(&st, id.clone(), b).await?;
    bump_cache(&st).await;
    Ok(Json(json!({ "id": id })))
}

async fn item_update(State(st): State<AppState>, Path(id): Path<String>, Json(b): Json<ItemIn>) -> ApiResult<StatusCode> {
    save_item(&st, id, b).await?;
    done(&st).await
}

async fn item_delete(State(st): State<AppState>, Extension(a): Extension<Auth>, Path(id): Path<String>) -> ApiResult<StatusCode> {
    a.admin()?;
    sqlx::query("DELETE FROM items WHERE id = $1").bind(id).execute(&st.db).await?;
    done(&st).await
}

// ---------- المواسم والحلقات ----------

async fn seasons_list(State(st): State<AppState>, Path(id): Path<String>) -> ApiResult<Json<Value>> {
    let v: Value = sqlx::query_scalar(
        "SELECT COALESCE(jsonb_agg(jsonb_build_object('id', s.id, 'number', s.number, 'title', s.title,
            'episodes', (SELECT COALESCE(jsonb_agg(jsonb_build_object('id', e.id, 'number', e.number, 'title', e.title,
                'durationMin', e.duration_min, 'poster', e.poster, 'streamUrl', e.stream_url) ORDER BY e.number), '[]'::jsonb)
                FROM episodes e WHERE e.season_id = s.id)) ORDER BY s.number), '[]'::jsonb)
         FROM seasons s WHERE s.series_id = $1",
    ).bind(id).fetch_one(&st.db).await?;
    Ok(Json(v))
}

#[derive(Deserialize)]
struct SeasonIn { id: Option<String>, number: i32, #[serde(default)] title: String }

async fn season_create(State(st): State<AppState>, Path(series): Path<String>, Json(mut b): Json<SeasonIn>) -> ApiResult<Json<Value>> {
    let id = id_or_new(b.id.take())?;
    sqlx::query("INSERT INTO seasons(id, series_id, number, title) VALUES($1,$2,$3,$4)")
        .bind(&id).bind(series).bind(b.number).bind(b.title).execute(&st.db).await?;
    bump_cache(&st).await;
    Ok(Json(json!({ "id": id })))
}

async fn season_update(State(st): State<AppState>, Path(id): Path<String>, Json(b): Json<SeasonIn>) -> ApiResult<StatusCode> {
    sqlx::query("UPDATE seasons SET number=$2, title=$3 WHERE id=$1").bind(id).bind(b.number).bind(b.title).execute(&st.db).await?;
    done(&st).await
}

async fn season_delete(State(st): State<AppState>, Extension(a): Extension<Auth>, Path(id): Path<String>) -> ApiResult<StatusCode> {
    a.admin()?;
    sqlx::query("DELETE FROM seasons WHERE id=$1").bind(id).execute(&st.db).await?;
    done(&st).await
}

#[derive(Deserialize)]
struct EpisodeIn {
    id: Option<String>,
    number: i32,
    #[serde(default)] title: String,
    #[serde(rename = "durationMin")] duration_min: Option<i32>,
    poster: Option<String>,
    #[serde(rename = "streamUrl")] stream_url: Option<String>,
}

async fn episode_create(State(st): State<AppState>, Path(season): Path<String>, Json(mut b): Json<EpisodeIn>) -> ApiResult<Json<Value>> {
    url_ok(&b.stream_url)?;
    let id = id_or_new(b.id.take())?;
    sqlx::query("INSERT INTO episodes(id, season_id, number, title, duration_min, poster, stream_url) VALUES($1,$2,$3,$4,$5,$6,$7)")
        .bind(&id).bind(season).bind(b.number).bind(b.title).bind(b.duration_min).bind(b.poster).bind(b.stream_url.filter(|u| !u.is_empty()))
        .execute(&st.db).await?;
    bump_cache(&st).await;
    Ok(Json(json!({ "id": id })))
}

async fn episode_update(State(st): State<AppState>, Path(id): Path<String>, Json(b): Json<EpisodeIn>) -> ApiResult<StatusCode> {
    url_ok(&b.stream_url)?;
    sqlx::query("UPDATE episodes SET number=$2, title=$3, duration_min=$4, poster=$5, stream_url=$6 WHERE id=$1")
        .bind(id).bind(b.number).bind(b.title).bind(b.duration_min).bind(b.poster).bind(b.stream_url.filter(|u| !u.is_empty()))
        .execute(&st.db).await?;
    done(&st).await
}

async fn episode_delete(State(st): State<AppState>, Extension(a): Extension<Auth>, Path(id): Path<String>) -> ApiResult<StatusCode> {
    a.admin()?;
    sqlx::query("DELETE FROM episodes WHERE id=$1").bind(id).execute(&st.db).await?;
    done(&st).await
}

// ---------- التصنيفات ----------

#[derive(Deserialize)]
struct CatIn { id: Option<String>, kind: String, name: String, #[serde(default)] position: i32 }

async fn cats_list(State(st): State<AppState>) -> ApiResult<Json<Value>> {
    rows(&st, "SELECT id, kind, name, position FROM categories ORDER BY kind, position, name").await
}

async fn cat_create(State(st): State<AppState>, Json(mut b): Json<CatIn>) -> ApiResult<Json<Value>> {
    kind_ok(&b.kind)?;
    let id = id_or_new(b.id.take())?;
    sqlx::query("INSERT INTO categories(id, kind, name, position) VALUES($1,$2,$3,$4)")
        .bind(&id).bind(b.kind).bind(b.name).bind(b.position).execute(&st.db).await?;
    bump_cache(&st).await;
    Ok(Json(json!({ "id": id })))
}

async fn cat_update(State(st): State<AppState>, Path(id): Path<String>, Json(b): Json<CatIn>) -> ApiResult<StatusCode> {
    kind_ok(&b.kind)?;
    sqlx::query("UPDATE categories SET kind=$2, name=$3, position=$4 WHERE id=$1").bind(id).bind(b.kind).bind(b.name).bind(b.position)
        .execute(&st.db).await?;
    done(&st).await
}

async fn cat_delete(State(st): State<AppState>, Extension(a): Extension<Auth>, Path(id): Path<String>) -> ApiResult<StatusCode> {
    a.admin()?;
    sqlx::query("DELETE FROM categories WHERE id=$1").bind(id).execute(&st.db).await?;
    done(&st).await
}

// ---------- الصفحة الرئيسية والإعدادات ----------

#[derive(Deserialize)]
struct SectionIn { id: String, r#type: String, #[serde(default)] title: String, source: String, #[serde(default = "yes")] enabled: bool }

async fn home_get(State(st): State<AppState>) -> ApiResult<Json<Value>> {
    rows(&st, "SELECT id, type, title, source, position, enabled FROM home_sections ORDER BY position").await
}

/// استبدال كامل: الترتيب هو ترتيب المصفوفة المرسلة.
async fn home_put(State(st): State<AppState>, Json(list): Json<Vec<SectionIn>>) -> ApiResult<StatusCode> {
    if list.len() > 40 { return Err(ApiError::BadRequest("too many".into())); }
    let mut tx = st.db.begin().await?;
    sqlx::query("DELETE FROM home_sections").execute(&mut *tx).await?;
    for (i, s) in list.iter().enumerate() {
        if !["hero", "row", "channels", "continue", "categories", "suggest"].contains(&s.r#type.as_str()) {
            return Err(ApiError::BadRequest("type".into()));
        }
        sqlx::query("INSERT INTO home_sections(id,type,title,source,position,enabled) VALUES($1,$2,$3,$4,$5,$6)")
            .bind(&s.id).bind(&s.r#type).bind(&s.title).bind(&s.source).bind(i as i32).bind(s.enabled)
            .execute(&mut *tx).await?;
    }
    tx.commit().await?;
    done(&st).await
}

async fn config_get(State(st): State<AppState>) -> ApiResult<Json<Value>> {
    let v: Option<Value> = sqlx::query_scalar("SELECT value FROM app_config WHERE key='app'").fetch_optional(&st.db).await?;
    Ok(Json(v.unwrap_or_else(|| json!({}))))
}

async fn config_put(State(st): State<AppState>, Extension(a): Extension<Auth>, Json(v): Json<Value>) -> ApiResult<StatusCode> {
    a.admin()?;
    if !v.is_object() { return Err(ApiError::BadRequest("object".into())); }
    sqlx::query("INSERT INTO app_config(key, value) VALUES('app', $1) ON CONFLICT (key) DO UPDATE SET value = $1").bind(v).execute(&st.db).await?;
    done(&st).await
}

// ---------- المستخدمون والاشتراكات ----------

async fn users_list(State(st): State<AppState>, Extension(a): Extension<Auth>) -> ApiResult<Json<Value>> {
    a.admin()?;
    rows(&st, "SELECT u.id, u.email, u.device, u.role, u.banned, u.created_at AS \"createdAt\", u.last_seen AS \"lastSeen\",
        (SELECT max(expires_at) FROM subscriptions s WHERE s.user_id = u.id) AS \"subscribedUntil\"
        FROM users u ORDER BY u.last_seen DESC LIMIT 200").await
}

#[derive(Deserialize)]
struct UserIn { role: Option<String>, banned: Option<bool> }

async fn user_update(State(st): State<AppState>, Extension(a): Extension<Auth>, Path(id): Path<Uuid>, Json(b): Json<UserIn>) -> ApiResult<StatusCode> {
    a.admin()?;
    if let Some(r) = &b.role {
        if !["user", "editor", "admin"].contains(&r.as_str()) { return Err(ApiError::BadRequest("role".into())); }
        if id == a.user && r != "admin" { return Err(ApiError::BadRequest("لا يمكنك إزالة صلاحيتك".into())); }
    }
    sqlx::query("UPDATE users SET role = COALESCE($2, role), banned = COALESCE($3, banned) WHERE id = $1")
        .bind(id).bind(b.role).bind(b.banned).execute(&st.db).await?;
    Ok(StatusCode::NO_CONTENT)
}

#[derive(Deserialize)]
struct SubIn { plan: String, days: i32 }

async fn sub_grant(State(st): State<AppState>, Extension(a): Extension<Auth>, Path(id): Path<Uuid>, Json(b): Json<SubIn>) -> ApiResult<StatusCode> {
    a.admin()?;
    if !(1..=3660).contains(&b.days) { return Err(ApiError::BadRequest("days".into())); }
    sqlx::query("INSERT INTO subscriptions(user_id, plan, expires_at) VALUES($1,$2, now() + make_interval(days => $3))")
        .bind(id).bind(b.plan).bind(b.days).execute(&st.db).await?;
    Ok(StatusCode::NO_CONTENT)
}

// ---------- الإشعارات والإعلانات ----------

#[derive(Deserialize)]
struct NotifIn { title: String, body: String }

async fn notif_list(State(st): State<AppState>) -> ApiResult<Json<Value>> {
    rows(&st, "SELECT id, title, body, created_at AS \"createdAt\" FROM notifications ORDER BY created_at DESC LIMIT 100").await
}

/// تُسجَّل هنا فقط. الإرسال الفعلي (FCM وغيره) خدمة مستقلة تقرأ هذا الجدول.
async fn notif_create(State(st): State<AppState>, Extension(a): Extension<Auth>, Json(b): Json<NotifIn>) -> ApiResult<StatusCode> {
    a.admin()?;
    if b.title.trim().is_empty() || b.body.trim().is_empty() { return Err(ApiError::BadRequest("empty".into())); }
    sqlx::query("INSERT INTO notifications(title, body, created_by) VALUES($1,$2,$3)").bind(b.title).bind(b.body).bind(a.user)
        .execute(&st.db).await?;
    Ok(StatusCode::NO_CONTENT)
}

#[derive(Deserialize)]
struct AdIn { id: Option<String>, placement: String, image: Option<String>, target: Option<String>, #[serde(default = "yes")] enabled: bool, #[serde(default)] position: i32 }

async fn ads_list(State(st): State<AppState>) -> ApiResult<Json<Value>> {
    rows(&st, "SELECT id, placement, image, target, enabled, position FROM ads ORDER BY placement, position").await
}

async fn ad_create(State(st): State<AppState>, Json(mut b): Json<AdIn>) -> ApiResult<Json<Value>> {
    let id = id_or_new(b.id.take())?;
    sqlx::query("INSERT INTO ads(id, placement, image, target, enabled, position) VALUES($1,$2,$3,$4,$5,$6)")
        .bind(&id).bind(b.placement).bind(b.image).bind(b.target).bind(b.enabled).bind(b.position).execute(&st.db).await?;
    Ok(Json(json!({ "id": id })))
}

async fn ad_update(State(st): State<AppState>, Path(id): Path<String>, Json(b): Json<AdIn>) -> ApiResult<StatusCode> {
    sqlx::query("UPDATE ads SET placement=$2, image=$3, target=$4, enabled=$5, position=$6 WHERE id=$1")
        .bind(id).bind(b.placement).bind(b.image).bind(b.target).bind(b.enabled).bind(b.position).execute(&st.db).await?;
    Ok(StatusCode::NO_CONTENT)
}

async fn ad_delete(State(st): State<AppState>, Extension(a): Extension<Auth>, Path(id): Path<String>) -> ApiResult<StatusCode> {
    a.admin()?;
    sqlx::query("DELETE FROM ads WHERE id=$1").bind(id).execute(&st.db).await?;
    Ok(StatusCode::NO_CONTENT)
}
