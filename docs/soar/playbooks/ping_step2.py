#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
SOAR playbook - step 2 probe.

Used to verify multi-node auto execution: the first middle node runs ping.py,
whose output is written into the flow variables and forwarded to the next
node's create listener. This script asserts it can see that upstream output.

Contract (see LocalPythonScriptExecutor):
  stdin   : workflow variables as JSON
  stdout  : exactly one JSON, optional keys "output" / "trace"
  exit 0   : success -> flow advances to the next node
"""

import json
import os
import sys
import time

SIMULATED_WORK_SEC = 2


def main():
    raw = sys.stdin.read().strip()
    try:
        ctx = json.loads(raw) if raw else {}
    except ValueError:
        ctx = {}

    prev = ctx.get("scriptOutput")
    prev_run_id = prev.get("runId") if isinstance(prev, dict) else None

    started = time.time()
    time.sleep(SIMULATED_WORK_SEC)
    cost_ms = int((time.time() - started) * 1000)

    sys.stderr.write("step2: nodeCode=%s\n" % os.environ.get("SOAR_NODE_CODE", ""))
    sys.stderr.write("step2: upstream scriptOutput present=%s\n" % (prev is not None))
    sys.stderr.flush()

    print(
        json.dumps(
            {
                "output": {
                    "step": 2,
                    "ping": "ok",
                    "runId": os.environ.get("SOAR_RUN_ID", ""),
                    "businessId": os.environ.get("SOAR_BUSINESS_ID", ""),
                    "nodeCode": os.environ.get("SOAR_NODE_CODE", ""),
                    "upstreamScriptOutputPresent": prev is not None,
                    "upstreamRunId": prev_run_id,
                    "inputKeys": sorted(str(k) for k in ctx.keys()),
                },
                "trace": [
                    {
                        "stepNo": 2,
                        "action": "ping-step2",
                        "result": "success",
                        "costMs": cost_ms,
                    }
                ],
            },
            ensure_ascii=True,
        )
    )
    return 0


if __name__ == "__main__":
    sys.exit(main())
