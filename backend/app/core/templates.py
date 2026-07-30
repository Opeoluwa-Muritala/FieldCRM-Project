"""Shared Jinja construction with request render timing."""
from __future__ import annotations

from time import perf_counter

from fastapi.templating import Jinja2Templates

from app.config import settings
from app.core.performance import record_duration
from app.core.template_utils import csp_nonce_context


_shared_templates: dict[str, "TimedJinja2Templates"] = {}


class TimedJinja2Templates(Jinja2Templates):
    def TemplateResponse(self, *args, **kwargs):
        started_at = perf_counter()
        try:
            return super().TemplateResponse(*args, **kwargs)
        finally:
            record_duration("render", started_at)


def date_format_filter(value, format_str='%d %b %Y, %H:%M'):
    if not value:
        return ''
    if isinstance(value, str):
        try:
            from datetime import datetime
            clean_val = value.replace('Z', '+00:00')
            # Handle space instead of T separator if present
            dt = datetime.fromisoformat(clean_val)
            return dt.strftime(format_str)
        except Exception:
            return value
    try:
        return value.strftime(format_str)
    except Exception:
        return str(value)


def create_templates(directory: str) -> TimedJinja2Templates:
    if directory in _shared_templates:
        return _shared_templates[directory]
    templates = TimedJinja2Templates(
        directory=directory,
        context_processors=[csp_nonce_context],
    )
    templates.env.filters['date_format'] = date_format_filter
    templates.env.auto_reload = not settings.is_production
    _shared_templates[directory] = templates
    return templates
