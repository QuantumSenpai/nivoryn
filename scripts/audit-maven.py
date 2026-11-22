import json
from pathlib import Path
from urllib.request import Request, urlopen

rows = []
for line in Path('app/gradle.lockfile').read_text().splitlines():
    if not line or line.startswith('#') or '=' not in line:
        continue
    coordinates, configurations = line.split('=', 1)
    if not {'debugRuntimeClasspath', 'releaseRuntimeClasspath'}.intersection(configurations.split(',')):
        continue
    group, name, version = coordinates.split(':')
    rows.append({'package': {'name': f'{group}:{name}', 'ecosystem': 'Maven'}, 'version': version})
assert rows
request = Request('https://api.osv.dev/v1/querybatch', json.dumps({'queries': rows}).encode(), {'Content-Type': 'application/json'})
with urlopen(request, timeout=30) as response:
    result = json.load(response)
assert len(result['results']) == len(rows)
assert not any(r.get('next_page_token') for r in result['results']), 'Audit requires additional result pages'
findings = [{'dependency': package, 'advisories': check.get('vulns', [])} for package, check in zip(rows, result['results']) if check.get('vulns')]
report = {'source': 'OSV querybatch', 'packages': len(rows), 'queries': rows, 'findings': findings}
output = Path('app/build/reports/security-osv.json')
output.parent.mkdir(parents=True, exist_ok=True)
output.write_text(json.dumps(report, indent=2))
print(json.dumps({k: v for k, v in report.items() if k != "queries"}))
