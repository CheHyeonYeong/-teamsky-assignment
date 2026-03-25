import argparse
import json
import random
import statistics
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
from collections import Counter
from concurrent.futures import ThreadPoolExecutor, as_completed


BASE_URL = "http://localhost:8080"


def read_problem_request():
    params = urllib.parse.urlencode(
        {
            "userId": random.choice([1, 2, 3]),
            "chapterId": random.choice([1, 2]),
        }
    )
    return ("GET", f"/api/v1/problems/random?{params}", None, {"Accept": "application/json"})


def read_history_request():
    params = urllib.parse.urlencode(
        {
            "userId": random.choice([1, 2, 3]),
            "chapterId": random.choice([1, 2]),
            "page": 0,
            "size": 20,
        }
    )
    return ("GET", f"/api/v1/submissions/history?{params}", None, {"Accept": "application/json"})


def read_wrong_request():
    params = urllib.parse.urlencode(
        {
            "userId": random.choice([1, 2, 3]),
            "page": 0,
            "size": 20,
        }
    )
    return ("GET", f"/api/v1/submissions/wrong?{params}", None, {"Accept": "application/json"})


def read_user_stats_request():
    user_id = random.choice([1, 2, 3])
    return ("GET", f"/api/v1/stats/users/{user_id}", None, {"Accept": "application/json"})


def write_subjective_request():
    user_id = random.choice([1, 2, 3])
    problem_id, answer = random.choice([(2, "20"), (2, "999"), (4, "2"), (4, "777")])
    payload = {
        "problemId": problem_id,
        "userId": user_id,
        "answerType": "SUBJECTIVE",
        "subjectiveAnswer": answer,
        "timeSpentSeconds": random.randint(3, 30),
        "hintUsed": False,
    }
    return (
        "POST",
        "/api/v1/submissions",
        json.dumps(payload).encode("utf-8"),
        {"Content-Type": "application/json", "Accept": "application/json"},
    )


def mixed_request():
    return random.choice(
        [
            read_problem_request,
            read_history_request,
            read_wrong_request,
            read_user_stats_request,
            write_subjective_request,
        ]
    )()


SCENARIOS = {
    "read-problem": read_problem_request,
    "read-history": read_history_request,
    "read-wrong": read_wrong_request,
    "read-user-stats": read_user_stats_request,
    "write-subjective": write_subjective_request,
    "mixed": mixed_request,
}


def percentile(samples, p):
    if not samples:
        return 0.0
    ordered = sorted(samples)
    index = min(len(ordered) - 1, round((p / 100) * (len(ordered) - 1)))
    return ordered[index]


def make_request(request_factory, timeout):
    method, path, body, headers = request_factory()
    url = BASE_URL + path
    started = time.perf_counter()
    request = urllib.request.Request(url=url, data=body, method=method, headers=headers)

    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            response.read()
            elapsed_ms = (time.perf_counter() - started) * 1000
            return {
                "ok": True,
                "status": response.status,
                "elapsed_ms": elapsed_ms,
                "error": None,
                "path": path,
            }
    except urllib.error.HTTPError as exc:
        elapsed_ms = (time.perf_counter() - started) * 1000
        try:
            body = exc.read().decode("utf-8", errors="replace")
        except Exception:
            body = ""
        return {
            "ok": False,
            "status": exc.code,
            "elapsed_ms": elapsed_ms,
            "error": body[:300],
            "path": path,
        }
    except Exception as exc:
        elapsed_ms = (time.perf_counter() - started) * 1000
        return {
            "ok": False,
            "status": "EXC",
            "elapsed_ms": elapsed_ms,
            "error": str(exc),
            "path": path,
        }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--scenario", choices=sorted(SCENARIOS), required=True)
    parser.add_argument("--requests", type=int, default=500)
    parser.add_argument("--concurrency", type=int, default=20)
    parser.add_argument("--timeout", type=float, default=10.0)
    args = parser.parse_args()

    request_factory = SCENARIOS[args.scenario]
    latencies = []
    status_counts = Counter()
    errors = []
    ok_count = 0
    started = time.perf_counter()
    lock = threading.Lock()

    with ThreadPoolExecutor(max_workers=args.concurrency) as pool:
        futures = [pool.submit(make_request, request_factory, args.timeout) for _ in range(args.requests)]
        for future in as_completed(futures):
            result = future.result()
            with lock:
                latencies.append(result["elapsed_ms"])
                status_counts[str(result["status"])] += 1
                if result["ok"]:
                    ok_count += 1
                elif len(errors) < 10:
                    errors.append(
                        {
                            "status": result["status"],
                            "path": result["path"],
                            "error": result["error"],
                        }
                    )

    duration = time.perf_counter() - started
    report = {
        "scenario": args.scenario,
        "requests": args.requests,
        "concurrency": args.concurrency,
        "duration_sec": round(duration, 3),
        "throughput_rps": round(args.requests / duration, 2) if duration else 0.0,
        "success_rate": round((ok_count / args.requests) * 100, 2) if args.requests else 0.0,
        "status_counts": dict(status_counts),
        "latency_ms": {
            "min": round(min(latencies), 2) if latencies else 0.0,
            "avg": round(statistics.fmean(latencies), 2) if latencies else 0.0,
            "median": round(statistics.median(latencies), 2) if latencies else 0.0,
            "p95": round(percentile(latencies, 95), 2),
            "p99": round(percentile(latencies, 99), 2),
            "max": round(max(latencies), 2) if latencies else 0.0,
        },
        "error_samples": errors,
    }
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
