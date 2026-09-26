"""Repeatable on-device Cards flings; writes raw gfxinfo and Perfetto evidence.

Run against the isolated benchmark APK on an unlocked portrait phone. First-use
means the first traversal in a new process, not a cold install or cleared cache.
The original com.koto.app installation is never touched.
"""
import argparse
import json
import re
import subprocess
import time
import xml.etree.ElementTree as ET
from pathlib import Path


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--adb", required=True)
    parser.add_argument("--package", default="com.koto.app.benchmark",
                        choices=["com.koto.app.benchmark", "com.koto.app.benchmarkdebug"])
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--runs", type=int, default=5)
    parser.add_argument("--screens", nargs="+", choices=["grid", "preview"], default=["grid", "preview"])
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)

    def adb(*cmd, binary=False):
        return subprocess.check_output([args.adb, *map(str, cmd)], text=not binary,
                                       encoding=None if binary else "utf-8", errors=None if binary else "replace")

    def shell(*cmd):
        return adb("shell", *cmd)

    def tree(name):
        shell("uiautomator", "dump", "/data/local/tmp/koto-perf-ui.xml")
        xml = shell("cat", "/data/local/tmp/koto-perf-ui.xml")
        (args.output / f"{name}.xml").write_text(xml, encoding="utf-8")
        return ET.fromstring(xml)

    def center(node):
        x1, y1, x2, y2 = map(int, re.findall(r"\d+", node.attrib["bounds"]))
        return (x1 + x2) // 2, (y1 + y2) // 2

    def tap_text(root, text):
        nodes = [n for n in root.iter("node") if n.get("text") == text]
        if not nodes:
            raise RuntimeError(f"Missing expected UI: {text}")
        shell("input", "tap", *center(nodes[-1]))

    width, height = map(int, re.search(r"(\d+)x(\d+)", shell("wm", "size")).groups())
    # Ratios keep the gestures in the scrolling viewport, above the anchored CTA.
    x, low, high = width // 2, int(height * .73), int(height * .27)
    config = f'''buffers {{ size_kb: 32768 fill_policy: RING_BUFFER }}
duration_ms: 30000
data_sources {{ config {{ name: "android.surfaceflinger.frametimeline" }} }}
data_sources {{ config {{ name: "linux.process_stats" process_stats_config {{ scan_all_processes_on_start: true }} }} }}
data_sources {{ config {{ name: "linux.ftrace" ftrace_config {{
    ftrace_events: "sched/sched_switch"
    ftrace_events: "sched/sched_waking"
    ftrace_events: "power/cpu_frequency"
    atrace_categories: "gfx"
    atrace_categories: "view"
    atrace_categories: "dalvik"
    atrace_apps: "{args.package}"
}} }} }}'''
    config_file = args.output / "trace-config.pbtxt"
    config_file.write_text(config)
    adb("push", config_file, "/data/local/tmp/koto-perf-config.pbtxt")
    metadata = {"package": args.package, "runs": args.runs,
                "display": shell("dumpsys", "SurfaceFlinger"),
                "thermal_start": shell("dumpsys", "thermalservice"),
                "package_info": shell("dumpsys", "package", args.package),
                "compilation": shell("cmd", "package", "compile", "-m", "speed", "-f", args.package)}
    (args.output / "metadata.json").write_text(json.dumps(metadata, indent=2))
    for screen in args.screens:
        for run in range(1, args.runs + 1):
            shell("am", "force-stop", args.package)
            shell("am", "start", "-W", "-n", f"{args.package}/com.koto.app.MainActivity")
            tap_text(tree("launch"), "Cards")
            root = tree("grid")
            if screen == "preview":
                # Longest authored deck: Months, Dates & Years (29 cards).
                for _ in range(5):
                    if any(n.get("text") == "Months, Dates & Years" for n in root.iter("node")):
                        break
                    shell("input", "swipe", x // 2, low, x // 2, low - height // 4, 450)
                    root = tree("find-deck")
                tap_text(root, "Months, Dates & Years")
                root = tree("preview")
                if not any(n.get("text") == "START FLASHCARDS" for n in root.iter("node")):
                    raise RuntimeError("Deck did not open")
                # Expose previews so the first measured fling starts on a star.
                shell("input", "swipe", x, low, x, high, 450)
                root = tree("preview-ready")

            for phase in ("first", "warm"):
                name = f"{screen}-{run}-{phase}"
                favorites = [n for n in root.iter("node") if n.get("content-desc", "").startswith("Favorite ")
                             and high + 200 < center(n)[1] < low]
                if not favorites:
                    raise RuntimeError(f"No visible favorite button for {name}")
                sx, sy = center(favorites[-1])
                remote_trace = f"/data/misc/perfetto-traces/koto-{name}.perfetto-trace"
                pid_output = subprocess.check_output(
                    [args.adb, "shell", "perfetto", "--background-wait", "--txt", "-c", "-", "-o", remote_trace],
                    input=config, text=True, encoding="utf-8")
                pid = re.search(r"\b\d+\b", pid_output).group()
                gestures = [(sx, sy, sx, high), (x, low, x, high),
                            (x, high, x, low), (x, high, x, low)]
                for step, coords in enumerate(gestures):
                    shell("dumpsys", "gfxinfo", args.package, "reset")
                    shell("input", "swipe", *coords, 130)
                    time.sleep(1.4)
                    raw = shell("dumpsys", "gfxinfo", args.package, "framestats")
                    (args.output / f"{name}-{step}.gfxinfo.txt").write_text(raw)
                shell("kill", "-TERM", pid)
                time.sleep(.4)
                adb("pull", remote_trace, args.output / f"{name}.perfetto-trace")
                shell("rm", remote_trace)
                root = tree(name)
                expected = "START FLASHCARDS" if screen == "preview" else "Cards"
                if not any(n.get("text") == expected for n in root.iter("node")):
                    raise RuntimeError(f"Scrolling activated a control: {name}")
                if any(n.get("selected") == "true" for n in root.iter("node")
                       if n.get("content-desc", "").startswith("Favorite ")):
                    raise RuntimeError(f"Scrolling toggled a favorite: {name}")
                (args.output / f"{name}.png").write_bytes(adb("exec-out", "screencap", "-p", binary=True))
                print(f"Captured {name}", flush=True)
                # Restore the exact starting viewport by reopening the screen.
                # The process and its compiled/font/shader caches remain warm.
                if phase == "first":
                    if screen == "preview":
                        shell("input", "keyevent", "KEYCODE_BACK")
                        root = tree("warm-grid")
                        for _ in range(5):
                            if any(n.get("text") == "Months, Dates & Years" for n in root.iter("node")):
                                break
                            shell("input", "swipe", x // 2, low, x // 2, low - height // 4, 450)
                            root = tree("warm-find-deck")
                        tap_text(root, "Months, Dates & Years")
                        shell("input", "swipe", x, low, x, high, 450)
                    else:
                        for _ in range(3):
                            shell("input", "swipe", x, high, x, low, 130)
                        time.sleep(1.4)
                    root = tree("warm-ready")
    (args.output / "thermal-end.txt").write_text(shell("dumpsys", "thermalservice"))


if __name__ == "__main__":
    main()
