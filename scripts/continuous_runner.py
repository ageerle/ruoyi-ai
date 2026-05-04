#!/usr/bin/env python3
"""Run a shell command continuously for a fixed duration.

Example:
  python3 scripts/continuous_runner.py --cmd "mvn -pl ruoyi-modules/ruoyi-chat -DskipTests compile"
"""

from __future__ import annotations

import argparse
import subprocess
import time
from datetime import datetime, timedelta


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Run command continuously with retry")
    parser.add_argument("--cmd", required=True, help="Command to run each round")
    parser.add_argument("--minutes", type=int, default=30, help="Total running minutes")
    parser.add_argument("--interval", type=int, default=5, help="Seconds between rounds")
    parser.add_argument("--stop-on-success", action="store_true", help="Stop after first success")
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    deadline = datetime.now() + timedelta(minutes=args.minutes)

    print(f"start={datetime.now().isoformat(timespec='seconds')}")
    print(f"end={deadline.isoformat(timespec='seconds')}")
    print(f"cmd={args.cmd}")

    round_no = 0
    while datetime.now() < deadline:
        round_no += 1
        print(f"\n[{datetime.now().isoformat(timespec='seconds')}] round={round_no}")
        result = subprocess.run(args.cmd, shell=True)
        print(f"exit_code={result.returncode}")

        if result.returncode == 0 and args.stop_on_success:
            print("stop_on_success=true, exiting")
            return 0

        if datetime.now() < deadline:
            time.sleep(max(0, args.interval))

    print(f"done={datetime.now().isoformat(timespec='seconds')}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
