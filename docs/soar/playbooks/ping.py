#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
SOAR playbook connectivity probe.

Contract implemented by LocalPythonScriptExecutor:
  stdin   : workflow variables as a single JSON object (may be empty)
  stdout  : exactly one JSON object; optional keys "output" and "trace"
  stderr  : free-form logs, ignored by the executor
  exit 0  : success -> taskService.skip() advances the flow
  exit !=0: failure -> the whole flow instance is terminated

Environment variables injected by the Java side:
  SOAR_RUN_ID, SOAR_BUSINESS_ID, SOAR_FLOW_CODE, SOAR_NODE_CODE, SOAR_SCRIPT_PATH

Usage:
  echo '{"alertId":"A-001"}' | python3 ping.py
"""

import json
import os
import sys
import time

# Simulated work duration in seconds. Set to 0 to make the flow finish instantly.
# Keeping it small but non-zero lets you observe the outbox row in status=1.
SIMULATED_WORK_SEC = 1


def read_payload():
    raw = sys.stdin.read().strip()
    if not raw:
        return {}
    try:
        data = json.loads(raw)
    except ValueError as exc:
        print(
            "ping: stdin is not valid JSON (%s), using raw text" % exc, file=sys.stderr
        )
        return {"_raw": raw[:500]}
    if not isinstance(data, dict):
        return {"_value": data}
    return data


def main():
    ctx = read_payload()

    started = time.time()
    time.sleep(SIMULATED_WORK_SEC)
    cost_ms = int((time.time() - started) * 1000)

    sys.stderr.write("ping: input keys = %s\n" % sorted(ctx.keys()))
    sys.stderr.write("ping: runId = %s\n" % os.environ.get("SOAR_RUN_ID", ""))
    sys.stderr.flush()

    result = {
        "output": {
            "ping": "ok",
            "message": "SOAR playbook chain is healthy",
            "runId": os.environ.get("SOAR_RUN_ID", ""),
            "businessId": os.environ.get("SOAR_BUSINESS_ID", ""),
            "flowCode": os.environ.get("SOAR_FLOW_CODE", ""),
            "nodeCode": os.environ.get("SOAR_NODE_CODE", ""),
            "inputKeys": sorted(str(k) for k in ctx.keys()),
        },
        "trace": [
            {
                "stepNo": 1,
                "action": "ping",
                "result": "success",
                "costMs": cost_ms,
            }
        ],
    }

    # ASCII-safe JSON: avoids any stdout encoding issue under LANG=C.
    print(json.dumps(result, ensure_ascii=True))
    return 0


if __name__ == "__main__":
    sys.exit(main())
