//! واجهة التطبيق (قراءة + مزامنة). مصدر الحقيقة لكل ما يظهر في Android.

use std::future::Future;

use axum::{
    extract::{Path, Query, State},
    http::{HeaderMap, StatusCode},
    routing::{get, post},
    Json, Router,
};
use hmac::{Hmac, Mac};
use redis::AsyncCommands;
use serde::{Deserialize, Serialize};
use serde_json::{json, Value};
use sha2::Sha256;
use uuid::Uuid;

use crate::{error::{ApiError, ApiResult}, AppState};

const PAGE: i64 = 24;
const COLS: &str = "i.id, i.kind, i.title, i.poster, i.backdrop, i.logo, i.year, i.description, \
    i.duration_min, i.quality, c.name AS category, i.live, i.online, NULL::text AS parent_id";
const FROM: &str = "FROM items i LEFT JOIN categories c ON c.id = i.category_id";

#[derive(Serialize, sqlx::FromRow)]
pub struct ItemOut {
    pub id: String,
    pub kind: String,
    pub title: String,
    pub poster: Option<String>,
    pub backdrop: Option<String>,
    pub logo: Option<String>,
    pub year: Option<i32>,
    pub description: Option<String>,
    #[serde(rename = "durationMin")]
    pub duration_min: Option<i32>,
    pub quality: Option<String>,
    pub category: Option<String>,
    pub live: bool,
    pub online: bool,
    #[serde(rename = "parentId")]
    pub parent_id: Option<String>,
}

impl ItemOut {
    fn bare(id: String, kind: &str, title: String) -> Self {
        ItemOut { id, kind: kind.into(), title, poster: None, backdrop: None, logo: None, year: None, description: None,
            duration_min: None, quality: None, category: None, live: false, online: true, parent_id: None }
    }
}

pub fn routes() -> Router<AppState> {
    Router::new()
        .route("/home", get(home))
        .route("/config", get(config))
        .route("/categories", get(categories))
        .route("/channels", get(channels))
        .route("/catalog", get(catalog))
        .route("/items/:id", get(detail))
        .route("/search", get(search))
        .route("/search/suggest", get(suggest))
        .route("/play/:kind/:id", get(play))
        .route("/sync/history", post(sync_history))
        .route("/sync/favorites", post(sync_favorites))
}

// ---------- مساعدات ----------

fn device(h: &HeaderMap) -> ApiResult<String> {
    let d = h.get("x-device").and_then(|v| v.to_str().ok()).unwrap_or("");
    if (8..=64).contains(&d.len()) && d.chars().all(|c| c.is_ascii_alphanumeric() || c == '-') {
        Ok(d.to_string())
    } else {
        Err(ApiError::BadRequest("invalid device".into()))
    }
}

async fn user_for_device(st: &AppState, dev: &str) -> ApiResult<Uuid> {
    let (id, banned): (Uuid, bool) = sqlx::query_as(
        "INSERT INTO users(device) VALUES($1) ON CONFLICT(device) DO UPDATE SET last_seen = now() RETURNING id, banned",
    )
    .bind(dev)
    .fetch_one(&st.db)
    .await?;
    if banned { return Err(ApiError::Forbidden); }
    Ok(id)
}

pub async fn bump_cache(st: &AppState) {
    let mut r = st.redis.clone();
    let _: Result<i64, _> = r.incr("cache:v", 1).await;
}

/// تخزين مؤقت في Redis مع رقم إصدار؛ أي تعديل من لوحة التحكم يرفع الإصدار فيُبطل كل شيء.
async fn cached<F, Fut>(st: &AppState, key: &str, ttl: u64, f: F) -> ApiResult<Json<Value>>
where
    F: FnOnce() -> Fut,
    Fut: Future<Output = ApiResult<Value>>,
{
    let mut r = st.redis.clone();
    let v: i64 = r.get("cache:v").await.unwrap_or(0);
    let k = format!("c:{v}:{key}");
    if let Ok(Some(s)) = r.get::<_, Option<String>>(&k).await {
        if let Ok(val) = serde_json::from_str::<Value>(&s) { return Ok(Json(val)); }
    }
    let val = f().await?;
    let _: Result<(), _> = r.set_ex(&k, val.to_string(), ttl).await;
    Ok(Json(val))
}

fn offset(c: &Option<String>) -> i64 {
    c.as_deref().and_then(|s| s.parse::<i64>().ok()).filter(|n| *n >= 0 && *n < 100_000).unwrap_or(0)
}

fn page(mut rows: Vec<ItemOut>, off: i64) -> Value {
    let next = if rows.len() as i64 > PAGE { rows.truncate(PAGE as usize); Some((off + PAGE).to_string()) } else { None };
    json!({ "items": rows, "next": next })
}

// ---------- الصفحة الرئيسية والإعدادات ----------

#[derive(sqlx::FromRow)]
struct SecRow { id: String, r#type: String, title: String, source: String }

async fn fetch_items(st: &AppState, filter: &str, order: &str, limit: i64) -> ApiResult<Vec<ItemOut>> {
    // filter/order ثوابت داخلية فقط، ليست من المستخدم
    let sql = format!("SELECT {COLS} {FROM} WHERE i.published AND {filter} ORDER BY {order} LIMIT {limit}");
    Ok(sqlx::query_as::<_, ItemOut>(&sql).fetch_all(&st.db).await?)
}

async fn section_items(st: &AppState, source: &str) -> ApiResult<Vec<ItemOut>> {
    match source {
        "featured" => fetch_items(st, "i.featured AND i.kind <> 'channel'", "i.position, i.created_at DESC", 8).await,
        "latest_movies" => fetch_items(st, "i.kind = 'movie'", "i.created_at DESC", 20).await,
        "latest_series" => fetch_items(st, "i.kind = 'series'", "i.created_at DESC", 20).await,
        "channels_featured" => fetch_items(st, "i.kind = 'channel' AND i.featured", "i.position, i.title", 20).await,
        "sports" => fetch_items(st, "i.kind = 'channel' AND i.online AND i.category_id = 'sports'", "i.position, i.title", 20).await,
        "recent" => fetch_items(st, "i.kind IN ('movie','series')", "i.created_at DESC", 20).await,
        "recommended" => fetch_items(st, "i.kind IN ('movie','series') AND i.featured", "i.position, i.created_at DESC", 20).await,
        // شريط الاقتراح في الرئيسية: المميز أولاً ثم الأحدث، أفلام ومسلسلات معاً
        "suggested" => fetch_items(st, "i.kind IN ('movie','series')", "i.featured DESC, i.position, i.created_at DESC", 16).await,
        "categories" => {
            let rows: Vec<(String, String)> = sqlx::query_as(
                "SELECT id, name FROM categories WHERE kind IN ('movie','series') ORDER BY position, name LIMIT 30",
            ).fetch_all(&st.db).await?;
            Ok(rows.into_iter().map(|(id, n)| ItemOut::bare(id, "category", n)).collect())
        }
        _ => Ok(vec![]), // continue وغيره: يملؤه العميل محلياً
    }
}

async fn home(State(st): State<AppState>) -> ApiResult<Json<Value>> {
    cached(&st, "home", 30, || async {
        let secs: Vec<SecRow> = sqlx::query_as(
            "SELECT id, type, title, source FROM home_sections WHERE enabled ORDER BY position",
        ).fetch_all(&st.db).await?;
        let mut out = Vec::with_capacity(secs.len());
        for s in secs {
            let items = section_items(&st, &s.source).await?;
            if items.is_empty() && s.r#type != "continue" { continue; } // إخفاء الأقسام الفارغة
            out.push(json!({ "id": s.id, "type": s.r#type, "title": s.title, "items": items }));
        }
        Ok(json!({ "sections": out }))
    }).await
}

async fn config(State(st): State<AppState>) -> ApiResult<Json<Value>> {
    cached(&st, "config", 60, || async {
        let v: Option<Value> = sqlx::query_scalar("SELECT value FROM app_config WHERE key = 'app'")
            .fetch_optional(&st.db).await?;
        Ok(v.unwrap_or_else(|| json!({})))
    }).await
}

// ---------- القوائم ----------

#[derive(Deserialize)]
struct ListQ { kind: Option<String>, category: Option<String>, cursor: Option<String>, sort: Option<String> }

async fn categories(State(st): State<AppState>, Query(q): Query<ListQ>) -> ApiResult<Json<Value>> {
    let kind = q.kind.unwrap_or_else(|| "movie".into());
    if !["movie", "series", "channel"].contains(&kind.as_str()) { return Err(ApiError::BadRequest("kind".into())); }
    cached(&st, &format!("cats:{kind}"), 60, || async {
        let rows: Vec<(String, String)> = sqlx::query_as("SELECT id, name FROM categories WHERE kind = $1 ORDER BY position, name")
            .bind(&kind).fetch_all(&st.db).await?;
        Ok(json!(rows.into_iter().map(|(id, name)| json!({"id": id, "name": name})).collect::<Vec<_>>()))
    }).await
}

async fn list(st: &AppState, kinds: &str, category: Option<String>, cursor: &Option<String>, order: &str) -> ApiResult<Json<Value>> {
    let off = offset(cursor);
    let sql = format!(
        "SELECT {COLS} {FROM} WHERE i.published AND i.kind = {kinds} AND ($1::text IS NULL OR i.category_id = $1) \
         ORDER BY {order} LIMIT $2 OFFSET $3"
    );
    let rows = sqlx::query_as::<_, ItemOut>(&sql).bind(category).bind(PAGE + 1).bind(off).fetch_all(&st.db).await?;
    Ok(Json(page(rows, off)))
}

async fn channels(State(st): State<AppState>, Query(q): Query<ListQ>) -> ApiResult<Json<Value>> {
    list(&st, "'channel'", q.category, &q.cursor, "i.position, i.title").await
}

async fn catalog(State(st): State<AppState>, Query(q): Query<ListQ>) -> ApiResult<Json<Value>> {
    let kind = match q.kind.as_deref() { Some("series") => "'series'", _ => "'movie'" };
    // sort=recent: الأحدث إضافة أولاً (للشريط الأفقي)، وإلا حسب ترتيب الإدارة
    let order = if q.sort.as_deref() == Some("recent") { "i.created_at DESC" } else { "i.position, i.created_at DESC" };
    list(&st, kind, q.category, &q.cursor, order).await
}

// ---------- التفاصيل ----------

async fn detail(State(st): State<AppState>, Path(id): Path<String>) -> ApiResult<Json<Value>> {
    if id.len() > 100 { return Err(ApiError::BadRequest("id".into())); }
    cached(&st, &format!("item:{id}"), 60, || async {
        let sql = format!("SELECT {COLS} {FROM} WHERE i.id = $1 AND i.published AND i.kind <> 'channel'");
        let item: ItemOut = sqlx::query_as(&sql).bind(&id).fetch_one(&st.db).await?;

        let mut seasons = vec![];
        if item.kind == "series" {
            let ss: Vec<(String, i32, String)> =
                sqlx::query_as("SELECT id, number, title FROM seasons WHERE series_id = $1 ORDER BY number")
                    .bind(&id).fetch_all(&st.db).await?;
            let es: Vec<(String, String, i32, String, Option<i32>, Option<String>)> = sqlx::query_as(
                "SELECT e.id, e.season_id, e.number, e.title, e.duration_min, e.poster FROM episodes e \
                 JOIN seasons s ON s.id = e.season_id WHERE s.series_id = $1 ORDER BY e.number",
            ).bind(&id).fetch_all(&st.db).await?;
            for (sid, num, title) in ss {
                let eps: Vec<Value> = es.iter().filter(|e| e.1 == sid)
                    .map(|e| json!({"id": e.0, "number": e.2, "title": e.3, "durationMin": e.4, "poster": e.5})).collect();
                seasons.push(json!({"id": sid, "number": num, "title": title, "episodes": eps}));
            }
        }

        let related: Vec<ItemOut> = sqlx::query_as(&format!(
            "SELECT {COLS} {FROM} WHERE i.published AND i.id <> $1 AND i.kind = $2 \
             AND i.category_id IS NOT DISTINCT FROM (SELECT category_id FROM items WHERE id = $1) \
             ORDER BY i.created_at DESC LIMIT 12"
        )).bind(&id).bind(&item.kind).fetch_all(&st.db).await?;

        Ok(json!({ "item": item, "seasons": seasons, "related": related }))
    }).await
}

// ---------- البحث ----------

#[derive(Deserialize)]
struct SearchQ { q: Option<String>, r#type: Option<String>, cursor: Option<String> }

fn like(q: &str) -> String {
    let esc = q.trim().replace('\\', "\\\\").replace('%', "\\%").replace('_', "\\_");
    format!("%{esc}%")
}

async fn search(State(st): State<AppState>, Query(q): Query<SearchQ>) -> ApiResult<Json<Value>> {
    let text = q.q.unwrap_or_default();
    if text.trim().chars().count() < 2 || text.len() > 100 { return Ok(Json(json!({"items": [], "next": null}))); }
    let t = q.r#type.filter(|t| ["movie", "series", "channel", "episode"].contains(&t.as_str()));
    let off = offset(&q.cursor);
    let sql = format!(
        "SELECT id, kind, title, poster, backdrop, logo, year, description, duration_min, quality, category, live, online, parent_id FROM ( \
           SELECT {COLS}, 0 AS rk {FROM} \
             WHERE i.published AND i.title ILIKE $1 AND ($2::text IS NULL OR i.kind = $2) \
           UNION ALL \
           SELECT e.id, 'episode', i.title || ' - ' || e.title, COALESCE(e.poster, i.poster), i.backdrop, NULL::text, i.year, \
                  NULL::text, e.duration_min, i.quality, c.name, FALSE, TRUE, i.id, 1 \
             FROM episodes e JOIN seasons s ON s.id = e.season_id JOIN items i ON i.id = s.series_id \
             LEFT JOIN categories c ON c.id = i.category_id \
             WHERE i.published AND (i.title ILIKE $1 OR e.title ILIKE $1) AND ($2::text IS NULL OR $2 = 'episode') \
         ) r ORDER BY rk, title LIMIT $3 OFFSET $4"
    );
    let rows = sqlx::query_as::<_, ItemOut>(&sql).bind(like(&text)).bind(t).bind(PAGE + 1).bind(off).fetch_all(&st.db).await?;
    Ok(Json(page(rows, off)))
}

async fn suggest(State(st): State<AppState>, Query(q): Query<SearchQ>) -> ApiResult<Json<Value>> {
    let text = q.q.unwrap_or_default();
    if text.trim().chars().count() < 2 || text.len() > 100 { return Ok(Json(json!([]))); }
    let pat = format!("{}%", like(&text).trim_start_matches('%').trim_end_matches('%'));
    let rows: Vec<String> = sqlx::query_scalar(
        "SELECT DISTINCT title FROM items WHERE published AND title ILIKE $1 ORDER BY title LIMIT 8",
    ).bind(pat).fetch_all(&st.db).await?;
    Ok(Json(json!(rows)))
}

// ---------- التشغيل (روابط محمية) ----------

fn sign(st: &AppState, url: String) -> String {
    let Some(secret) = &st.cfg.sign_secret else { return url };
    let exp = chrono::Utc::now().timestamp() + st.cfg.sign_ttl_secs;
    let mut mac = Hmac::<Sha256>::new_from_slice(secret.as_bytes()).expect("hmac key");
    mac.update(format!("{url}|{exp}").as_bytes());
    let sig = hex::encode(mac.finalize().into_bytes());
    let sep = if url.contains('?') { '&' } else { '?' };
    format!("{url}{sep}exp={exp}&sig={sig}")
}

async fn play(State(st): State<AppState>, headers: HeaderMap, Path((kind, id)): Path<(String, String)>) -> ApiResult<Json<Value>> {
    let uid = user_for_device(&st, &device(&headers)?).await?;
    let row: Option<(Option<String>, bool, bool, String)> = match kind.as_str() {
        "movie" | "channel" => sqlx::query_as(
            "SELECT stream_url, live, premium, id FROM items WHERE id = $1 AND published AND kind = $2",
        ).bind(&id).bind(&kind).fetch_optional(&st.db).await?,
        "episode" => sqlx::query_as(
            "SELECT e.stream_url, FALSE, i.premium, i.id FROM episodes e JOIN seasons s ON s.id = e.season_id \
             JOIN items i ON i.id = s.series_id WHERE e.id = $1 AND i.published",
        ).bind(&id).fetch_optional(&st.db).await?,
        _ => return Err(ApiError::BadRequest("kind".into())),
    };
    let (url, live, premium, item_id) = row.ok_or(ApiError::NotFound)?;
    let url = url.filter(|u| u.starts_with("https://") || u.starts_with("http://")).ok_or(ApiError::NotFound)?;

    if premium {
        let ok: bool = sqlx::query_scalar("SELECT EXISTS(SELECT 1 FROM subscriptions WHERE user_id = $1 AND expires_at > now())")
            .bind(uid).fetch_one(&st.db).await?;
        if !ok { return Err(ApiError::Forbidden); }
    }

    let db = st.db.clone();
    tokio::spawn(async move {
        let _ = sqlx::query(
            "INSERT INTO view_stats(item_id, plays) VALUES($1, 1) ON CONFLICT (item_id, day) DO UPDATE SET plays = view_stats.plays + 1",
        ).bind(item_id).execute(&db).await;
    });

    Ok(Json(json!({ "url": sign(&st, url), "live": live || kind == "channel" })))
}

// ---------- المزامنة ----------

#[derive(Deserialize)]
struct ItemRef { id: String }

#[derive(Deserialize)]
struct HistIn {
    key: String,
    item: ItemRef,
    #[serde(rename = "episodeId")]
    episode_id: Option<String>,
    #[serde(rename = "positionMs")]
    position_ms: i64,
    #[serde(rename = "durationMs")]
    duration_ms: i64,
}

async fn sync_history(State(st): State<AppState>, headers: HeaderMap, Json(b): Json<HistIn>) -> ApiResult<StatusCode> {
    if b.key.len() > 100 || b.item.id.len() > 100 || b.position_ms < 0 || b.duration_ms < 0 {
        return Err(ApiError::BadRequest("invalid".into()));
    }
    let uid = user_for_device(&st, &device(&headers)?).await?;
    sqlx::query(
        "INSERT INTO history(user_id, key, item_id, episode_id, position_ms, duration_ms) VALUES($1,$2,$3,$4,$5,$6) \
         ON CONFLICT (user_id, key) DO UPDATE SET position_ms = $5, duration_ms = $6, updated_at = now()",
    ).bind(uid).bind(&b.key).bind(&b.item.id).bind(&b.episode_id).bind(b.position_ms).bind(b.duration_ms)
    .execute(&st.db).await?;
    Ok(StatusCode::NO_CONTENT)
}

#[derive(Deserialize)]
struct FavIn { #[serde(rename = "itemId")] item_id: String, on: bool }

async fn sync_favorites(State(st): State<AppState>, headers: HeaderMap, Json(b): Json<FavIn>) -> ApiResult<StatusCode> {
    if b.item_id.len() > 100 { return Err(ApiError::BadRequest("invalid".into())); }
    let uid = user_for_device(&st, &device(&headers)?).await?;
    if b.on {
        sqlx::query("INSERT INTO favorites(user_id, item_id) SELECT $1, id FROM items WHERE id = $2 ON CONFLICT DO NOTHING")
            .bind(uid).bind(&b.item_id).execute(&st.db).await?;
    } else {
        sqlx::query("DELETE FROM favorites WHERE user_id = $1 AND item_id = $2").bind(uid).bind(&b.item_id).execute(&st.db).await?;
    }
    Ok(StatusCode::NO_CONTENT)
}
