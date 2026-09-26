"""Summarize raw measure_cards_scroll.py evidence without hiding slow frames."""
import argparse
import csv
import io
import json
import re
import subprocess
from collections import defaultdict
from pathlib import Path


def percentile(values, percent):
    values = sorted(values)
    return round(values[min(len(values) - 1, int((len(values) - 1) * percent / 100))], 3) if values else None


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("directory", type=Path)
    parser.add_argument("--trace-processor", type=Path)
    parser.add_argument("--package", default="com.koto.app.benchmark")
    args = parser.parse_args()
    groups = defaultdict(lambda: {"frames": [], "missed": 0, "reported_frames": 0, "files": 0})
    for path in sorted(args.directory.glob("*.gfxinfo.txt")):
        screen, run, phase, step = path.name.removesuffix(".gfxinfo.txt").split("-")
        text = path.read_text()
        since = int(re.search(r"Stats since: (\d+)ns", text)[1])
        frames = []
        header = None
        for line in text.splitlines():
            if line.startswith("Flags,"):
                header = line.rstrip(",").split(",")
            elif header and re.match(r"^\d+,", line):
                frame = dict(zip(header, map(int, line.rstrip(",").split(","))))
                if frame["Flags"] == 0 and frame["IntendedVsync"] >= since:
                    duration = (frame["FrameCompleted"] - frame["IntendedVsync"]) / 1e6
                    if 0 < duration < 10000:
                        frames.append(duration)
        group = groups[f"{screen}-{phase}"]
        group["frames"].extend(frames)
        group["reported_frames"] += int(re.search(r"Total frames rendered: (\d+)", text)[1])
        group["missed"] += int(re.search(r"Janky frames: (\d+)", text)[1])
        group["files"] += 1
    result = {}
    for name, group in groups.items():
        values = group.pop("frames")
        result[name] = {**group, "sampled_frames": len(values),
                        "deadline_missed_percent": round(group["missed"] / group["reported_frames"] * 100, 3),
                        "frame_ms_p50": percentile(values, 50), "frame_ms_p95": percentile(values, 95),
                        "frame_ms_p99": percentile(values, 99), "frame_ms_max": max(values, default=None),
                        "frames_over_33_3_ms": sum(v > 33.333 for v in values)}
    if args.trace_processor:
        trace_groups = defaultdict(lambda: {"main_ms": [], "jank": defaultdict(int), "slices": defaultdict(list)})
        for path in sorted(args.directory.glob("*.perfetto-trace")):
            screen, run, phase = path.stem.split("-")
            query = f'''select 'frame' as kind, jank_type as name, dur/1e6 as ms
                from actual_frame_timeline_slice join process using(upid) where process.name='{args.package}'
                union all
                select 'main', case when s.name glob 'Choreographer#doFrame*' then 'doFrame' else s.name end, s.dur/1e6
                from slice s join thread_track tt on s.track_id=tt.id join thread t using(utid) join process p using(upid)
                where p.name='{args.package}' and t.tid=p.pid and
                (s.name glob 'Choreographer#doFrame*' or s.name in ('AndroidOwner:measureAndLayout',
                 'Compose:recompose','compose:lazy:prefetch:measure','compose:lazy:prefetch:compose'))'''
            output = subprocess.check_output([str(args.trace_processor), str(path), "-Q", query],
                                             text=True, stderr=subprocess.DEVNULL)
            (args.directory / (path.stem + ".trace.csv")).write_text(output)
            group = trace_groups[f"{screen}-{phase}"]
            for row in csv.DictReader(io.StringIO(output)):
                if row["kind"] == "frame":
                    group["jank"][row["name"]] += 1
                elif row["name"] == "doFrame":
                    group["main_ms"].append(float(row["ms"]))
                else:
                    group["slices"][row["name"]].append(float(row["ms"]))
        for name, group in trace_groups.items():
            result[name]["trace"] = {
                "frame_timeline_counts": dict(group["jank"]),
                "main_doFrame_ms_p50": percentile(group["main_ms"], 50),
                "main_doFrame_ms_p95": percentile(group["main_ms"], 95),
                "main_doFrame_ms_p99": percentile(group["main_ms"], 99),
                "work": {n: {"n": len(v), "mean_ms": round(sum(v)/len(v), 3), "p95_ms": percentile(v, 95)}
                         for n, v in group["slices"].items()},
            }
    (args.directory / "summary.json").write_text(json.dumps(result, indent=2))
    print(json.dumps(result, indent=2))


if __name__ == "__main__":
    main()
