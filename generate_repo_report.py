#!/usr/bin/env python3
"""
generate_repo_report.py

Usage:
  - Run locally in a git repo: `python3 generate_repo_report.py`
  - OR set these environment variables for GitHub Actions use:
      GITHUB_REPOSITORY -> owner/repo  (optional, script tries to read from git remote)
      GITHUB_TOKEN -> (optional) to call GitHub API for languages & more

What it does (best-effort without external services):
  - runs `cloc` (if available) to count LOC per language
  - falls back to a pure-python line-counter by extension
  - runs `sloccount` (if available) to produce COCOMO-ish estimates
  - reads git history to get commits, first/last commit dates, contributors
  - queries GitHub languages API (if GITHUB_TOKEN or public rate limits allow)
  - generates REPORT.md (markdown) using the evaluation template

Drop this into your repository root and call it from a GitHub Action or locally.

Note: SonarQube / JaCoCo and other hosted services require separate setup and tokens.
"""

import os
import sys
import subprocess
import json
import datetime
from collections import defaultdict, Counter

REPO = os.getenv('GITHUB_REPOSITORY')  # owner/repo
GITHUB_TOKEN = os.getenv('GITHUB_TOKEN')

OUTFILE = 'REPO_ANALYSIS_REPORT.md'

EXT_LANG_MAP = {
    # minimal mapping for Maven + Thymeleaf projects
    '.java': 'Java',
    '.kt': 'Kotlin',
    '.html': 'HTML',
    '.thymeleaf': 'HTML',
    '.xml': 'XML',
    '.properties': 'Properties',
    '.yml': 'YAML',
    '.yaml': 'YAML',
    '.js': 'JavaScript',
    '.css': 'CSS',
    '.scss': 'SCSS',
    '.sql': 'SQL',
    '.md': 'Markdown'
}

# ---------------- helper funcs ----------------

def run(cmd, capture=True):
    try:
        if capture:
            return subprocess.check_output(cmd, shell=True, stderr=subprocess.DEVNULL).decode('utf-8').strip()
        else:
            subprocess.check_call(cmd, shell=True)
            return ''
    except Exception:
        return None


def detect_git_root():
    p = run('git rev-parse --show-toplevel')
    return p


def cloc_available():
    return run('which cloc') is not None


def sloccount_available():
    return run('which sloccount') is not None


def run_cloc():
    if not cloc_available():
        return None
    out = run('cloc --json .')
    if not out:
        return None
    try:
        data = json.loads(out)
        # remove metadata keys
        data.pop('header', None)
        data.pop('SUM', None)
        return data
    except Exception:
        return None


def run_sloccount():
    if not sloccount_available():
        return None
    out = run('sloccount --details .')
    return out


def languages_via_github(repo):
    if not repo:
        return None
    owner_repo = repo
    import urllib.request
    url = f'https://api.github.com/repos/{owner_repo}/languages'
    req = urllib.request.Request(url)
    if GITHUB_TOKEN:
        req.add_header('Authorization', f'token {GITHUB_TOKEN}')
    try:
        with urllib.request.urlopen(req) as r:
            return json.load(r)
    except Exception:
        return None


def count_lines_by_extension():
    mapping = defaultdict(int)
    total = 0
    for root, dirs, files in os.walk('.'):
        # skip .git and target
        if '/.git' in root or root.startswith('./.git') or '/target' in root:
            continue
        for f in files:
            path = os.path.join(root, f)
            _, ext = os.path.splitext(f)
            try:
                with open(path, 'rb') as fh:
                    lines = fh.read().count(b'\n')
            except Exception:
                continue
            lang = EXT_LANG_MAP.get(ext.lower(), ext.lower().lstrip('.') or 'other')
            mapping[lang] += lines
            total += lines
    return dict(mapping), total


def git_stats():
    stats = {}
    # commits
    commits = run('git rev-list --count HEAD')
    stats['commit_count'] = int(commits) if commits and commits.isdigit() else None
    # first/last commit
    first = run("git log --reverse --pretty=format:'%ci' | head -n1")
    last = run("git log -1 --pretty=format:'%ci'")
    stats['first_commit'] = first
    stats['last_commit'] = last
    # contributors
    contribs = run("git shortlog -sne | awk '{$1=$1; print $0}'")
    if contribs:
        lines = [l for l in contribs.splitlines() if l.strip()]
        contributors = []
        for l in lines:
            parts = l.strip().split('\t')
            if len(parts) == 2:
                cnt, rest = parts
                contributors.append({'count': int(cnt.strip()), 'who': rest.strip()})
            else:
                # fallback split on space
                p = l.strip().split(' ', 1)
                if len(p) == 2:
                    contributors.append({'count': int(p[0]), 'who': p[1]})
        stats['contributors'] = contributors
    else:
        stats['contributors'] = []
    return stats


def estimate_effort_cocomo(total_loc):
    # VERY rough: use basic productivity numbers. This is illustrative only.
    # COCOMO-like: person-months = a * (KSLOC)^b . Use small project constants
    try:
        ks = max(1.0, total_loc / 1000.0)
        a = 2.4
        b = 1.05
        pm = a * (ks ** b)
        # cost assuming $7k per person-month
        cost = pm * 7000
        return {'person_months': round(pm, 2), 'est_cost_usd': round(cost, 2)}
    except Exception:
        return None


def generate_markdown(report):
    lines = []
    lines.append('# Repository Analysis Report')
    lines.append('')
    lines.append(f"Generated: {datetime.datetime.utcnow().isoformat()} UTC")
    lines.append('')
    if report.get('repo'):
        lines.append(f"**Repository:** {report['repo']}")
        lines.append('')

    lines.append('## Quick stats')
    lines.append('')
    lines.append(f"- Commits: {report['git'].get('commit_count')}")
    lines.append(f"- First commit: {report['git'].get('first_commit')}")
    lines.append(f"- Last commit: {report['git'].get('last_commit')}")
    lines.append(f"- Contributors (top): {', '.join([c['who'] for c in report['git'].get('contributors',[])][:5])}")
    lines.append('')

    lines.append('## Language breakdown (lines of code)')
    lines.append('')
    lines.append('| Language | Lines | % |')
    lines.append('|---|---:|---:|')
    total = report['total_loc'] or 0
    for lang, loc in sorted(report['by_lang'].items(), key=lambda i: -i[1]):
        percent = (loc/total*100) if total else 0
        lines.append(f"| {lang} | {loc:,} | {percent:.1f}% |")
    lines.append('')

    if report.get('cloc_raw'):
        lines.append('> cloc raw summary available in JSON output `cloc_results.json`')
        lines.append('')

    if report.get('sloccount'):
        lines.append('## sloccount output (excerpt)')
        lines.append('')
        sl = report['sloccount']
        lines.append('```')
        lines.append(sl[:4000])
        lines.append('```')
        lines.append('')

    # COCOMO-ish
    if report.get('cocomo'):
        lines.append('## Rough effort estimate')
        lines.append('')
        lines.append(f"- Estimated person-months: **{report['cocomo']['person_months']}**")
        lines.append(f"- Estimated development cost (USD): **{report['cocomo']['est_cost_usd']:,}**")
        lines.append('')

    lines.append('## Manual checklist (fill in)')
    lines.append('')
    lines.append('''
### Architecture & Code Quality
- [ ] Maven build works out of the box
- [ ] Layered architecture (controller/service/repo)
- [ ] Low duplication / DRY followed
- [ ] JavaDoc and comments where helpful

### Security & Config
- [ ] No secrets in code
- [ ] Input validation and sanitization
- [ ] Secure auth (Spring Security)

### Tests & CI
- [ ] Unit / integration tests present
- [ ] Test coverage reported (JaCoCo)
- [ ] CI (GitHub Actions) runs build & tests

### Documentation
- [ ] README contains setup & run instructions
- [ ] License present
- [ ] Example data or demo
''')
    lines.append('')

    lines.append('## Notes / Next steps')
    lines.append('')
    lines.append('- Integrate SonarCloud or SonarQube for richer quality & security metrics.')
    lines.append('- Add JaCoCo and a code coverage badge to README if missing.')
    lines.append('- Consider automating dependency checks (OWASP deps) and secret scanning.')

    return '\n'.join(lines)


# ---------------- main ----------------

def main():
    git_root = detect_git_root()
    if not git_root:
        print('Not a git repository or git not found. Run inside a repo.')
        sys.exit(1)

    repo = REPO
    if not repo:
        # try to read origin remote
        rem = run('git config --get remote.origin.url')
        if rem:
            # convert possible git@github.com:owner/repo.git or https url
            if rem.startswith('git@'):
                # git@github.com:owner/repo.git
                repo = rem.split(':',1)[1].rstrip('.git')
            else:
                # https://github.com/owner/repo.git
                repo = rem.rstrip('.git').split('/')[-2:]
                repo = '/'.join(repo)

    report = {'repo': repo}

    print('Collecting git stats...')
    report['git'] = git_stats()

    print('Counting lines (cloc if available, else fallback)...')
    cloc = run_cloc()
    if cloc:
        # transform cloc output
        by_lang = {}
        total = 0
        for k,v in cloc.items():
            # v has 'code' key
            if isinstance(v, dict):
                loc = v.get('code', 0)
                by_lang[k] = loc
                total += loc
        report['cloc_raw'] = cloc
        report['by_lang'] = by_lang
        report['total_loc'] = total
        with open('cloc_results.json','w') as fh:
            json.dump(cloc, fh, indent=2)
    else:
        by_lang, total = count_lines_by_extension()
        report['cloc_raw'] = None
        report['by_lang'] = by_lang
        report['total_loc'] = total

    print('Running sloccount (optional)...')
    sl = run_sloccount()
    report['sloccount'] = sl

    # COCOMO-ish estimate
    coc = estimate_effort_cocomo(report['total_loc'] or 0)
    report['cocomo'] = coc

    # GitHub languages API
    gh_langs = None
    if repo:
        print('Querying GitHub languages API (if rate limits allow)...')
        gh_langs = languages_via_github(repo)
    report['github_languages'] = gh_langs

    # write markdown
    md = generate_markdown(report)
    with open(OUTFILE, 'w', encoding='utf-8') as fh:
        fh.write(md)

    print(f'Report written to {OUTFILE}')
    print('Done.')


if __name__ == '__main__':
    main()


# ----------------- GitHub Actions workflow example -----------------

# Save the following YAML as .github/workflows/repo-analysis.yml
# It runs on push and writes the REPORT.md as an artifact.

workflow_yaml = '''
name: Repo Analysis Report

on:
  push:
    branches: [ main, master ]
  workflow_dispatch:

jobs:
  analysis:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Install dependencies for cloc/sloccount (apt)
        run: |
          sudo apt-get update
          sudo apt-get install -y cloc sloccount
      - name: Run analysis script
        env:
          GITHUB_REPOSITORY: ${{ github.repository }}
          GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}
        run: |
          python3 generate_repo_report.py
      - name: Upload report artifact
        uses: actions/upload-artifact@v4
        with:
          name: repo-analysis-report
          path: REPO_ANALYSIS_REPORT.md
'''

# You can print this YAML to a file or copy-paste it into your repo.

print('\n---')
print('\nA sample GitHub Actions workflow YAML is embedded in this script as a string variable named workflow_yaml.')
