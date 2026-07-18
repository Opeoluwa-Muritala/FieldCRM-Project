"""Small Redis-backed rate limiter suitable for stateless serverless instances."""
from __future__ import annotations

import hashlib
from time import time

from fastapi import HTTPException, Request, status

from app.config import settings

_redis = None


async def init_rate_limiter() -> None:
    global _redis
    if _redis is None and settings.RATE_LIMIT_REDIS_URL:
        from redis.asyncio import Redis
        _redis = Redis.from_url(settings.RATE_LIMIT_REDIS_URL, encoding="utf-8", decode_responses=True)
        await _redis.ping()


async def close_rate_limiter() -> None:
    global _redis
    if _redis is not None:
        await _redis.aclose()
        _redis = None


def _client_ip(request: Request) -> str:
    return request.client.host if request.client else "unknown"


def _hash_identifier(value: str) -> str:
    return hashlib.sha256(value.strip().lower().encode("utf-8")).hexdigest()


async def _enforce(scope: str, key: str, maximum: int, window_seconds: int) -> None:
    if _redis is None:
        # Development/test only. Production requires Redis at settings validation.
        return
    bucket = int(time() // window_seconds)
    redis_key = f"fieldcrm:ratelimit:{scope}:{key}:{bucket}"
    try:
        async with _redis.pipeline(transaction=True) as pipeline:
            pipeline.incr(redis_key)
            pipeline.expire(redis_key, window_seconds + 5)
            count, _ = await pipeline.execute()
    except Exception as exc:
        if settings.is_production:
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail="Authentication rate limiting is temporarily unavailable.",
            ) from exc
        return
    if count > maximum:
        raise HTTPException(
            status_code=status.HTTP_429_TOO_MANY_REQUESTS,
            detail="Too many attempts. Please wait before trying again.",
            headers={"Retry-After": str(window_seconds)},
        )


async def enforce_login_limits(request: Request, account: str) -> None:
    await _enforce("login-ip", _client_ip(request), 10, 15 * 60)
    await _enforce("login-account", _hash_identifier(account), 5, 15 * 60)


async def enforce_reset_limits(request: Request, account_or_token: str) -> None:
    await _enforce("reset-ip", _client_ip(request), 5, 60 * 60)
    await _enforce("reset-account", _hash_identifier(account_or_token), 3, 60 * 60)
