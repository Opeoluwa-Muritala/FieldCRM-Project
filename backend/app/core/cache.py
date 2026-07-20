"""Redis-first response caching for authenticated FieldCRM reads.

Postgres remains the system of record.  A response is read from Redis first
and reaches Postgres only after a cache miss or a targeted invalidation.
"""
from __future__ import annotations

import json
import logging
from functools import wraps
from hashlib import sha256
from inspect import signature
from typing import Any

from fastapi.encoders import jsonable_encoder
from starlette.types import ASGIApp, Message, Receive, Scope, Send

from app.config import settings

log = logging.getLogger(__name__)
_redis = None
_PREFIX = "fieldcrm:cache:v2"


def _digest(value: object) -> str:
    return sha256(str(value).encode("utf-8")).hexdigest()[:20]


async def init_cache() -> None:
    global _redis
    if _redis is not None or not settings.CACHE_REDIS_URL:
        return
    try:
        from redis.asyncio import Redis

        client = Redis.from_url(
            settings.CACHE_REDIS_URL,
            encoding="utf-8",
            decode_responses=True,
            socket_connect_timeout=1,
            socket_timeout=1,
        )
        await client.ping()
        _redis = client
        log.info("Redis response cache connected")
    except Exception:
        log.warning("Redis response cache is unavailable; continuing without it.")


async def close_cache() -> None:
    global _redis
    if _redis is not None:
        await _redis.aclose()
        _redis = None


async def get_json(key: str) -> Any | None:
    if _redis is None:
        return None
    try:
        value = await _redis.get(key)
        return json.loads(value) if value is not None else None
    except Exception:
        log.warning("Redis cache read failed", exc_info=True)
        return None


async def set_json(key: str, value: Any, ttl_seconds: int, *, only_if_absent: bool = False) -> None:
    """Store JSON with a TTL; deployment warm-up uses ``only_if_absent``."""
    if _redis is None:
        return
    try:
        await _redis.set(
            key,
            json.dumps(jsonable_encoder(value), separators=(",", ":")),
            ex=ttl_seconds,
            nx=only_if_absent,
        )
    except Exception:
        log.warning("Redis cache write failed", exc_info=True)


def _scope_key(scope: str, identifier: object) -> str:
    return f"{_PREFIX}:version:{scope}:{_digest(identifier)}"


async def _scope_versions(scopes: list[tuple[str, object]]) -> list[str]:
    if _redis is None:
        return ["0"] * len(scopes)
    keys = [_scope_key(scope, identifier) for scope, identifier in scopes]
    try:
        values = await _redis.mget(keys)
        return [value or "0" for value in values]
    except Exception:
        log.warning("Redis cache-version read failed", exc_info=True)
        return ["0"] * len(scopes)


async def invalidate_scopes(*scopes: tuple[str, object]) -> None:
    """Invalidate only the response groups affected by a committed write."""
    if _redis is None or not scopes:
        return
    try:
        async with _redis.pipeline(transaction=True) as pipeline:
            for scope, identifier in scopes:
                pipeline.incr(_scope_key(scope, identifier))
            await pipeline.execute()
    except Exception:
        log.warning("Redis cache invalidation failed", exc_info=True)


def _auth_user_key(user_id: object) -> str:
    return f"{_PREFIX}:auth-user:{_digest(user_id)}"


async def get_cached_auth_user(user_id: object) -> dict[str, Any] | None:
    """Return a short-lived authorization profile without exposing password hashes."""
    return await get_json(_auth_user_key(user_id))


async def cache_auth_user(user: Any) -> None:
    await set_json(
        _auth_user_key(user.id),
        {
            "id": user.id,
            "org_id": user.org_id,
            "full_name": user.full_name,
            "email": user.email,
            "password_hash": "",
            "role": user.role,
            "active": user.active,
            "last_login_at": user.last_login_at,
            "created_at": user.created_at,
        },
        ttl_seconds=30,
    )


async def invalidate_auth_user(user_id: object) -> None:
    if _redis is None:
        return
    try:
        await _redis.delete(_auth_user_key(user_id))
    except Exception:
        log.warning("Redis auth-profile invalidation failed", exc_info=True)


def cache_response(ttl_seconds: int, *, application_scoped: bool = False, notification_scoped: bool = False):
    """Cache an authenticated GET response with org/user/application boundaries."""
    def decorator(handler):
        handler_signature = signature(handler)

        @wraps(handler)
        async def wrapped(*args, **kwargs):
            bound = handler_signature.bind_partial(*args, **kwargs)
            user = bound.arguments.get("current_user")
            if user is None:
                return await handler(*args, **kwargs)

            scopes: list[tuple[str, object]] = [("org", user.org_id), ("user", user.id)]
            application_id = bound.arguments.get("application_id")
            if application_scoped and application_id is not None:
                scopes.append(("application", application_id))
            if notification_scoped:
                scopes.append(("notifications", user.id))

            request_values = {
                name: value
                for name, value in bound.arguments.items()
                if name not in {"conn", "current_user", "request"}
            }
            fingerprint = sha256(
                json.dumps(jsonable_encoder(request_values), sort_keys=True, separators=(",", ":")).encode("utf-8")
            ).hexdigest()
            versions = await _scope_versions(scopes)
            scope_token = ":".join(_digest(identifier) + "-" + version for (_, identifier), version in zip(scopes, versions))
            key = f"{_PREFIX}:response:{handler.__name__}:{scope_token}:{fingerprint}"
            cached = await get_json(key)
            if cached is not None:
                log.debug("Redis cache hit: %s", handler.__name__)
                return cached

            response = await handler(*args, **kwargs)
            await set_json(key, response, ttl_seconds, only_if_absent=True)
            log.debug("Redis cache miss: %s", handler.__name__)
            return response

        return wrapped
    return decorator


class ResponseCacheInvalidationMiddleware:
    """Invalidate affected cache scopes after successful authenticated writes."""
    _MUTATING_METHODS = {"POST", "PUT", "PATCH", "DELETE"}

    def __init__(self, app: ASGIApp):
        self.app = app

    async def __call__(self, scope: Scope, receive: Receive, send: Send) -> None:
        if scope["type"] != "http" or scope["method"] not in self._MUTATING_METHODS:
            await self.app(scope, receive, send)
            return

        successful_write = False

        async def send_with_tracking(message: Message) -> None:
            nonlocal successful_write
            if message["type"] == "http.response.start":
                successful_write = 200 <= message["status"] < 400
            await send(message)

        await self.app(scope, receive, send_with_tracking)
        user = scope.get("state", {}).get("cache_user")
        if not successful_write or user is None:
            return

        affected: list[tuple[str, object]] = [("org", user.org_id), ("user", user.id)]
        path_parts = [part for part in scope.get("path", "").split("/") if part]
        if "applications" in path_parts:
            index = path_parts.index("applications")
            if len(path_parts) > index + 1:
                affected.append(("application", path_parts[index + 1]))
        if "notifications" in path_parts:
            affected.append(("notifications", user.id))
        await invalidate_scopes(*affected)
        if "users" in path_parts:
            index = path_parts.index("users")
            if len(path_parts) > index + 1:
                await invalidate_auth_user(path_parts[index + 1])
        await invalidate_auth_user(user.id)
