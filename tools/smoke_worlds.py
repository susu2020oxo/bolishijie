#!/usr/bin/env python3
"""Exercise the actual distributed JAR: ordinary world creation and reload.

Only writes to build/production-smoke. No player world is opened or modified.
"""
from pathlib import Path
import os
import queue
import secrets
import socket
import struct
import subprocess
import threading
import time

ROOT = Path(__file__).resolve().parents[1]
RUN = ROOT / "build/production-smoke"


class Rcon:
    def __init__(self, password):
        deadline = time.monotonic() + 15
        while True:
            try:
                self.socket = socket.create_connection(("127.0.0.1", 25578), timeout=5)
                break
            except ConnectionRefusedError:
                if time.monotonic() >= deadline:
                    raise
                time.sleep(0.2)
        self.socket.settimeout(30)
        self.id = 0
        self.request(3, password)

    def receive(self, count):
        value = b""
        while len(value) < count:
            part = self.socket.recv(count - len(value))
            if not part:
                raise RuntimeError("RCON connection closed")
            value += part
        return value

    def request(self, kind, command):
        self.id += 1
        body = struct.pack("<ii", self.id, kind) + command.encode() + b"\0\0"
        self.socket.sendall(struct.pack("<i", len(body)) + body)
        while True:
            length = struct.unpack("<i", self.receive(4))[0]
            if not 10 <= length <= 1048576:
                raise RuntimeError("Invalid RCON response")
            reply = self.receive(length)
            reply_id, reply_kind = struct.unpack("<ii", reply[:8])
            if reply_id == -1:
                raise RuntimeError("RCON authentication failed")
            if reply_id == self.id and (kind != 3 or reply_kind == 2):
                return reply[8:-2].decode(errors="replace")

    def command(self, command):
        response = self.request(2, command)
        print("[WORLD-SMOKE]", command, "=>", response, flush=True)
        return response


def session(label, password, world_name="smoke-world", with_mod=True, converted=True, expand=False):
    command = [str(ROOT / "gradlew"), "runProductionSmokeServer", "--no-daemon", "--console=plain"]
    process = subprocess.Popen(command, cwd=ROOT, stdout=subprocess.PIPE,
                               stderr=subprocess.STDOUT, text=True, bufsize=1,
                               start_new_session=True,
                               env={**os.environ, "BOLISHIJIE_SMOKE_RCON_PASSWORD": password,
                                    "BOLISHIJIE_SMOKE_LEVEL_NAME": world_name,
                                    "BOLISHIJIE_SMOKE_DISABLE_MOD": "0" if with_mod else "1"})
    lines = queue.Queue()

    def collect():
        for line in process.stdout:
            print(line, end="", flush=True)
            lines.put(line)
        lines.put(None)

    threading.Thread(target=collect, daemon=True).start()
    try:
        deadline = time.monotonic() + 300
        while True:
            if time.monotonic() > deadline:
                raise RuntimeError(label + ": world startup timed out")
            try:
                line = lines.get(timeout=1)
            except queue.Empty:
                continue
            if line is None:
                raise RuntimeError(label + ": server exited before ready")
            if 'Done (' in line and 'For help' in line:
                break
        rcon = Rcon(password)
        dimensions = [
            ("minecraft:overworld", -64, "bedrock"),
            ("minecraft:the_nether", 0, "bedrock"),
            ("minecraft:the_end", 50, "end_stone"),
        ]
        def check_dimension(dimension, y, source, offset, glass):
            end = offset + 31
            response = rcon.command(f"execute in {dimension} run forceload add {offset} {offset} {end} {end}")
            if 'Unknown' in response or 'Incorrect' in response:
                raise RuntimeError("Failed to load " + dimension)
            deadline = time.monotonic() + 90
            marker = "Test passed" if source else "The time is"
            block = (f"bolishijie:terrain/{source}" if glass else f"minecraft:{source}") if source else None
            x = offset + 8
            heights = [40, 45, 50, 55, 60] if source == "end_stone" else [y]
            while True:
                # Source identity compatibility makes glass also match a vanilla
                # block predicate. Explicitly exclude the glass variant when
                # verifying that existing vanilla chunks were not converted.
                responses = [rcon.request(2, f"execute in {dimension} if loaded {x} {height} {x} "
                                           + (f"unless block {x} {height} {x} bolishijie:terrain/{source} "
                                              if with_mod and not glass else "")
                                           + (f"if block {x} {height} {x} {block}" if block else "run time query gametime"))
                             for height in heights]
                if any(marker in response for response in responses):
                    print(f"[WORLD-SMOKE] {label}: {dimension} at {x},{x} verified {block or 'new FULL chunk'}", flush=True)
                    break
                if time.monotonic() > deadline:
                    raise RuntimeError(f"{label}: expected terrain {block} not loaded in {dimension}: {responses}")
                time.sleep(1)
        for dimension, y, source in dimensions:
            check_dimension(dimension, y, source, 0, converted)
            if expand:
                # Existing vanilla spawn/ticking tickets generated well beyond
                # the original four chunks. Go far away to test NEW terrain.
                # End terrain here can legitimately be void: assert FULL loading;
                # actual End glass was already checked in create/reload above.
                check_dimension(dimension, y, None if source == "end_stone" else source, 1024, True)
        rcon.command("save-all flush")
        rcon.request(2, "stop")
        rcon.socket.close()
        code = process.wait(timeout=90)
        if code:
            raise RuntimeError(f"{label}: server exit code {code}")
    finally:
        if process.poll() is None:
            import signal
            os.killpg(process.pid, signal.SIGTERM)
            try:
                process.wait(timeout=15)
            except subprocess.TimeoutExpired:
                os.killpg(process.pid, signal.SIGKILL)


def main():
    RUN.mkdir(parents=True, exist_ok=True)
    password = secrets.token_hex(16)
    (RUN / "eula.txt").write_text("eula=true\n")
    (RUN / "server.properties").write_text(
        "server-ip=127.0.0.1\nserver-port=25577\nonline-mode=false\n"
        "enable-rcon=true\nrcon.port=25578\nrcon.password=" + password + "\n"
        "level-name=smoke-world\nlevel-seed=20261010\nlevel-type=minecraft:normal\n"
        "spawn-chunk-radius=1\nview-distance=2\nsimulation-distance=2\n"
        "max-tick-time=60000\n")
    session("create", password)
    assert (RUN / "smoke-world/level.dat").is_file()
    session("reload", password)
    session("create-before-install", password, "preexisting-world", with_mod=False, converted=False)
    session("load-existing-after-install", password, "preexisting-world", converted=False, expand=True)
    print("[WORLD-SMOKE] PASS: new worlds and pre-existing worlds load, save and generate terrain in all three dimensions", flush=True)


if __name__ == "__main__":
    main()
