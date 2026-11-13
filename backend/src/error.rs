use axum::{http::StatusCode, response::{IntoResponse, Response}, Json};
use serde_json::json;

#[derive(Debug)]
pub enum ApiError {
    NotFound,
    Unauthorized,
    Forbidden,
    BadRequest(String),
    TooMany,
    Internal(String),
}

pub type ApiResult<T> = Result<T, ApiError>;

impl IntoResponse for ApiError {
    fn into_response(self) -> Response {
        let (code, msg) = match self {
            ApiError::NotFound => (StatusCode::NOT_FOUND, "not_found".to_string()),
            ApiError::Unauthorized => (StatusCode::UNAUTHORIZED, "unauthorized".into()),
            ApiError::Forbidden => (StatusCode::FORBIDDEN, "forbidden".into()),
            ApiError::BadRequest(m) => (StatusCode::BAD_REQUEST, m),
            ApiError::TooMany => (StatusCode::TOO_MANY_REQUESTS, "rate_limited".into()),
            ApiError::Internal(m) => {
                tracing::error!(error = %m, "internal error");
                (StatusCode::INTERNAL_SERVER_ERROR, "internal".into())
            }
        };
        (code, Json(json!({ "error": msg }))).into_response()
    }
}

impl From<sqlx::Error> for ApiError {
    fn from(e: sqlx::Error) -> Self {
        match e {
            sqlx::Error::RowNotFound => ApiError::NotFound,
            other => ApiError::Internal(other.to_string()),
        }
    }
}
impl From<redis::RedisError> for ApiError {
    fn from(e: redis::RedisError) -> Self { ApiError::Internal(e.to_string()) }
}
impl From<serde_json::Error> for ApiError {
    fn from(e: serde_json::Error) -> Self { ApiError::Internal(e.to_string()) }
}
