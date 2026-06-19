"""Minimal MCP stdio client to smoke-test the server end-to-end.

Launches the built jar, performs the MCP handshake, then calls each tool and
prints the JSON results. Keeps stdin open so responses are read synchronously
(piping all input at once would race the EOF shutdown).

Usage:  python scripts/smoke_test.py
"""
import json
import subprocess
import sys
import threading
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
JAR = ROOT / "build" / "libs" / "embodied-ai-mcp-server-0.0.1-SNAPSHOT.jar"

if sys.stdout.encoding and sys.stdout.encoding.lower() != "utf-8":
    sys.stdout.reconfigure(encoding="utf-8")


def main():
    proc = subprocess.Popen(
        ["java", "-jar", str(JAR)],
        stdin=subprocess.PIPE,
        stdout=subprocess.PIPE,
        stderr=subprocess.DEVNULL,
        text=True,
        encoding="utf-8",
        bufsize=1,
    )

    def send(obj):
        proc.stdin.write(json.dumps(obj) + "\n")
        proc.stdin.flush()

    def read():
        line = proc.stdout.readline()
        return json.loads(line) if line.strip() else None

    rid = 0

    def request(method, params=None):
        nonlocal rid
        rid += 1
        send({"jsonrpc": "2.0", "id": rid, "method": method, "params": params or {}})
        return read()

    def call_tool(name, args=None):
        resp = request("tools/call", {"name": name, "arguments": args or {}})
        # MCP tool results come back as content blocks; unwrap the text payload.
        content = resp.get("result", {}).get("content", [])
        text = content[0].get("text") if content else json.dumps(resp)
        return text

    try:
        init = request("initialize", {
            "protocolVersion": "2024-11-05",
            "capabilities": {},
            "clientInfo": {"name": "smoke-test", "version": "1.0"},
        })
        print("✓ initialize:", init["result"]["serverInfo"])
        send({"jsonrpc": "2.0", "method": "notifications/initialized"})

        tools = request("tools/list")["result"]["tools"]
        print(f"✓ tools/list: {[t['name'] for t in tools]}\n")

        print("── get_robot_status ──")
        print(call_tool("get_robot_status"), "\n")

        print("── get_opex_breakdown (active = canvas) ──")
        print(call_tool("get_opex_breakdown"), "\n")

        print("── get_economic_viability (active = canvas) ──")
        print(call_tool("get_economic_viability"), "\n")

        print("── list_navigation_policies ──")
        print(call_tool("list_navigation_policies"), "\n")

        print("── set_active_policy('navdp') ──")
        print(call_tool("set_active_policy", {"policyId": "navdp"}), "\n")

        print("── recommend_navigation_policy (active = navdp) ──")
        print(call_tool("recommend_navigation_policy"), "\n")

    finally:
        try:
            proc.stdin.close()
        except Exception:
            pass
        proc.wait(timeout=10)


if __name__ == "__main__":
    main()
