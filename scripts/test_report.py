#!/usr/bin/env python3
"""Builds the pipeline's test report from JUnit XML (Surefire + Failsafe) and JaCoCo's CSV.

    python3 scripts/test_report.py --out site [--summary "$GITHUB_STEP_SUMMARY"] [--run-url URL]

Writes site/index.html (a self-contained report) and, if asked, a Markdown summary for the GitHub job page.
Exits 0 even when tests failed: Maven already decides pass/fail; this script only reports.
"""
import argparse
import csv
import datetime as dt
import glob
import html
import os
import shutil
import xml.etree.ElementTree as ET

LAYERS = [("Unit", "target/surefire-reports"), ("Integration", "target/failsafe-reports")]


def load_suites():
    suites = []
    for layer, folder in LAYERS:
        for path in sorted(glob.glob(os.path.join(folder, "TEST-*.xml"))):
            root = ET.parse(path).getroot()
            for suite in ([root] if root.tag == "testsuite" else root.findall("testsuite")):
                cases = []
                for tc in suite.findall("testcase"):
                    status, message = "passed", ""
                    for tag in ("failure", "error"):
                        el = tc.find(tag)
                        if el is not None:
                            status, message = "failed", (el.get("message") or el.text or "").strip()
                    if tc.find("skipped") is not None:
                        status = "skipped"
                    cases.append({"name": tc.get("name", ""), "time": float(tc.get("time") or 0),
                                  "status": status, "message": message})
                suites.append({"layer": layer, "name": suite.get("name", os.path.basename(path)),
                               "time": float(suite.get("time") or 0), "cases": cases})
    return suites


def load_coverage(path="target/site/jacoco/jacoco.csv"):
    if not os.path.exists(path):
        return None
    totals = {"LINE": [0, 0], "BRANCH": [0, 0], "INSTRUCTION": [0, 0]}
    with open(path, newline="") as f:
        for row in csv.DictReader(f):
            for k in totals:
                totals[k][0] += int(row[f"{k}_MISSED"])
                totals[k][1] += int(row[f"{k}_COVERED"])
    return {k: (c / (m + c) * 100 if m + c else 100.0) for k, (m, c) in totals.items()}


def counts(cases):
    return {s: sum(1 for c in cases if c["status"] == s) for s in ("passed", "failed", "skipped")}


def markdown(suites, cov, run_url):
    all_cases = [c for s in suites for c in s["cases"]]
    n = counts(all_cases)
    icon = "✅" if n["failed"] == 0 and all_cases else "❌"
    lines = [f"## {icon} {len(all_cases)} tests: {n['passed']} passed, {n['failed']} failed, {n['skipped']} skipped", ""]
    if cov:
        lines += [f"**Coverage:** {cov['LINE']:.1f}% lines, {cov['BRANCH']:.1f}% branches", ""]
    lines += ["| Layer | Suite | Cases | Failed | Time |", "|---|---|---:|---:|---:|"]
    for s in suites:
        c = counts(s["cases"])
        lines.append(f"| {s['layer']} | {s['name']} | {len(s['cases'])} | {c['failed']} | {s['time']:.2f}s |")
    failed = [(s, c) for s in suites for c in s["cases"] if c["status"] == "failed"]
    if failed:
        lines += ["", "### Failures", ""]
        for s, c in failed[:30]:
            lines.append(f"- **{s['name']}** › {c['name']}: `{c['message'][:200]}`")
    if run_url:
        lines += ["", f"[Workflow run]({run_url})"]
    return "\n".join(lines) + "\n"


def page(suites, cov, run_url, commit):
    all_cases = [c for s in suites for c in s["cases"]]
    n = counts(all_cases)
    ok = n["failed"] == 0 and bool(all_cases)
    when = dt.datetime.now(dt.timezone.utc).strftime("%d %b %Y, %H:%M UTC")

    def esc(x):
        return html.escape(str(x))

    blocks = []
    for i, s in enumerate(suites):
        c = counts(s["cases"])
        rows = "".join(
            f'<tr class="{t["status"]}"><td><span class="dot" aria-hidden="true"></span>{esc(t["name"])}'
            + (f'<div class="msg">{esc(t["message"][:400])}</div>' if t["message"] else "")
            + f'</td><td>{t["status"]}</td><td>{t["time"]*1000:.0f} ms</td></tr>'
            for t in s["cases"])
        blocks.append(f"""
<details{" open" if c["failed"] else ""}>
  <summary><span class="layer">{esc(s["layer"])}</span><span class="sname">{esc(s["name"])}</span>
    <span class="count">{len(s["cases"])} cases{f' · <b class="bad">{c["failed"]} failed</b>' if c["failed"] else ""}</span></summary>
  <table><thead><tr><th>Case</th><th>Result</th><th>Time</th></tr></thead><tbody>{rows}</tbody></table>
</details>""")

    cov_html = ""
    if cov:
        def bar(label, v):
            return (f'<div class="metric"><div class="mlabel">{label}</div><div class="mval">{v:.1f}%</div>'
                    f'<div class="track"><div class="fill" style="width:{v:.1f}%"></div></div></div>')
        cov_html = bar("Line coverage", cov["LINE"]) + bar("Branch coverage", cov["BRANCH"])

    links = '<a href="coverage/index.html">Full coverage report →</a>' if cov else ""
    if run_url:
        links += f' <a href="{esc(run_url)}">Pipeline run →</a>'

    return f"""<!doctype html>
<html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width, initial-scale=1">
<title>ParcelPilot test report</title>
<style>
:root {{ --bg:#f6f1e7; --card:#fffdf7; --ink:#1d1a16; --muted:#5d5446; --ok:#166534; --bad:#b91c1c; --line:#d9cdb6; --accent:#c2410c; }}
@media (prefers-color-scheme: dark) {{ :root:not([data-theme="light"]) {{ --bg:#16140f; --card:#211e18; --ink:#f3ede1; --muted:#b5aa96; --ok:#4ade80; --bad:#f87171; --line:#3a342a; --accent:#fb923c; }} }}
* {{ box-sizing:border-box; }}
body {{ margin:0; background:var(--bg); color:var(--ink); font:15px/1.5 system-ui,-apple-system,"Segoe UI",Roboto,sans-serif; }}
main {{ max-width:1000px; margin:0 auto; padding:32px 16px 64px; }}
h1 {{ font-size:clamp(26px,5vw,38px); letter-spacing:-.02em; margin:0 0 4px; }}
.sub {{ color:var(--muted); margin:0 0 24px; }}
.hero {{ display:grid; grid-template-columns:repeat(auto-fit,minmax(160px,1fr)); gap:12px; margin-bottom:24px; }}
.metric {{ background:var(--card); border:1px solid var(--line); border-radius:10px; padding:14px 16px; }}
.mlabel {{ color:var(--muted); font-size:13px; }}
.mval {{ font-size:28px; font-weight:800; font-variant-numeric:tabular-nums; }}
.mval.ok {{ color:var(--ok); }} .mval.bad {{ color:var(--bad); }}
.track {{ height:6px; background:var(--line); border-radius:3px; margin-top:6px; overflow:hidden; }}
.fill {{ height:100%; background:var(--ok); }}
.links {{ margin:0 0 24px; display:flex; gap:18px; flex-wrap:wrap; }} .links a {{ color:var(--accent); font-weight:600; }}
details {{ background:var(--card); border:1px solid var(--line); border-radius:10px; margin-bottom:10px; }}
summary {{ cursor:pointer; padding:12px 16px; display:flex; gap:10px; align-items:baseline; flex-wrap:wrap; }}
.layer {{ font:600 11px ui-monospace,Menlo,monospace; text-transform:uppercase; letter-spacing:.08em; border:1px solid var(--line); border-radius:4px; padding:1px 6px; color:var(--muted); }}
.sname {{ font-weight:700; flex:1; min-width:200px; }} .count {{ color:var(--muted); font-size:13px; }}
.bad {{ color:var(--bad); }}
table {{ width:100%; border-collapse:collapse; font-size:14px; }}
th, td {{ text-align:left; padding:7px 16px; border-top:1px solid var(--line); vertical-align:top; }}
th {{ color:var(--muted); font-weight:600; font-size:12px; }}
td:nth-child(2), td:nth-child(3) {{ white-space:nowrap; color:var(--muted); width:1%; }}
.dot {{ display:inline-block; width:8px; height:8px; border-radius:50%; background:var(--ok); margin-right:8px; }}
tr.failed .dot {{ background:var(--bad); }} tr.skipped .dot {{ background:var(--muted); }}
tr.failed td:nth-child(2) {{ color:var(--bad); font-weight:700; }}
.msg {{ font:12px ui-monospace,Menlo,monospace; color:var(--bad); margin-top:4px; white-space:pre-wrap; word-break:break-word; }}
footer {{ color:var(--muted); font-size:13px; margin-top:24px; }}
</style></head>
<body><main>
<h1>ParcelPilot test report</h1>
<p class="sub">Built {esc(when)}{f" from commit <code>{esc(commit[:7])}</code>" if commit else ""}. Data-driven JUnit 5 suites: unit tests (Surefire) and HTTP integration tests (Failsafe).</p>
<div class="hero">
  <div class="metric"><div class="mlabel">Result</div><div class="mval {'ok' if ok else 'bad'}">{'Passing' if ok else 'Failing'}</div></div>
  <div class="metric"><div class="mlabel">Test cases</div><div class="mval">{len(all_cases)}</div></div>
  <div class="metric"><div class="mlabel">Failed</div><div class="mval {'bad' if n['failed'] else ''}">{n['failed']}</div></div>
  {cov_html}
</div>
<p class="links">{links}</p>
{''.join(blocks)}
<footer>Generated by scripts/test_report.py from JUnit XML and JaCoCo data.</footer>
</main></body></html>
"""


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default="site")
    ap.add_argument("--summary")
    ap.add_argument("--run-url", default="")
    ap.add_argument("--commit", default=os.environ.get("GITHUB_SHA", ""))
    a = ap.parse_args()

    suites = load_suites()
    cov = load_coverage()
    os.makedirs(a.out, exist_ok=True)
    with open(os.path.join(a.out, "index.html"), "w", encoding="utf-8") as f:
        f.write(page(suites, cov, a.run_url, a.commit))
    if os.path.isdir("target/site/jacoco"):
        shutil.copytree("target/site/jacoco", os.path.join(a.out, "coverage"), dirs_exist_ok=True)
    md = markdown(suites, cov, a.run_url)
    if a.summary:
        with open(a.summary, "a", encoding="utf-8") as f:
            f.write(md)
    print(md)


if __name__ == "__main__":
    main()
