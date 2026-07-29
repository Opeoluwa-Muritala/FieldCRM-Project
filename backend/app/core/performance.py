"""Request-scoped performance measurements without sensitive query details."""
from __future__ import annotations

from contextvars import ContextVar
from dataclasses import dataclass, field
from time import perf_counter


@dataclass
class RequestPerformance:
    started_at: float = field(default_factory=perf_counter)
    durations_ms: dict[str, float] = field(default_factory=dict)
    query_count: int = 0
    counters: dict[str, int] = field(default_factory=dict)

    def add(self, name: str, duration_ms: float) -> None:
        self.durations_ms[name] = self.durations_ms.get(name, 0.0) + duration_ms

    def increment(self, name: str) -> None:
        self.counters[name] = self.counters.get(name, 0) + 1


_current_performance: ContextVar[RequestPerformance | None] = ContextVar(
    "fieldcrm_request_performance",
    default=None,
)


def start_request_performance() -> tuple[RequestPerformance, object]:
    performance = RequestPerformance()
    return performance, _current_performance.set(performance)


def finish_request_performance(token: object) -> None:
    _current_performance.reset(token)


def record_duration(name: str, started_at: float) -> None:
    performance = _current_performance.get()
    if performance is not None:
        performance.add(name, (perf_counter() - started_at) * 1000)


def record_query(started_at: float) -> None:
    performance = _current_performance.get()
    if performance is not None:
        performance.query_count += 1
        performance.add("sql", (perf_counter() - started_at) * 1000)


def record_counter(name: str) -> None:
    performance = _current_performance.get()
    if performance is not None:
        performance.increment(name)
