#!/usr/bin/env python3
"""Compile/run only the pure SDK recurrence and duration codecs with an existing JDK.
No Android provider, network, device, pixel or native-runtime verification.
"""
import argparse
import hashlib
import json
from pathlib import Path
import subprocess
import tempfile
from datetime import datetime, timezone


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--jdk', type=Path, required=True)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    sources = [root / 'sdk/src/main/java/me/ancal/wmdroid/sdk/WmRecurrenceRule.java',
               root / 'sdk/tests/WmRecurrenceRuleChecks.java',
               root / 'sdk/src/main/java/me/ancal/wmdroid/sdk/WmCalendarDuration.java',
               root / 'sdk/tests/WmCalendarDurationChecks.java']
    javac, java = (args.jdk.resolve() / 'bin' / name for name in ('javac', 'java'))
    for path in sources + [javac, java]:
        if not path.is_file():
            parser.error('Required existing file missing: ' + str(path))
    base = root / 'build/sdk-standardization/recurrence-rule-jvm'
    base.mkdir(parents=True, exist_ok=True)
    run = Path(tempfile.mkdtemp(prefix=datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ-'), dir=base))
    classes = run / 'classes'
    classes.mkdir()
    record = {'scope': 'pure JVM codec only; no Android/provider/native runtime',
              'status': 'running', 'sources': {str(p.relative_to(root)): sha(p) for p in sources},
              'compiler': {'path': str(javac), 'sha256': sha(javac)},
              'runtime': {'path': str(java), 'sha256': sha(java)}, 'commands': [], 'artifacts': {}}
    def execute(name, command):
        result = subprocess.run([str(x) for x in command], stdout=subprocess.PIPE,
                                stderr=subprocess.STDOUT, text=True, encoding='utf-8', errors='replace', timeout=120)
        log = run / (name + '.log')
        log.write_text(result.stdout, encoding='utf-8')
        record['commands'].append({'name': name, 'argv': [str(x) for x in command],
                                   'returncode': result.returncode, 'stdout': log.name})
        return result.returncode
    code = 1
    try:
        execute('compiler-version', [javac, '-version'])
        code = execute('compile', [javac, '--release', '8', '-encoding', 'UTF-8', '-d', classes] + sources)
        if code == 0:
            results = [execute(name, [java, '-ea', '-cp', classes, 'me.ancal.wmdroid.sdk.' + test])
                       for name, test in [('recurrence-assertions', 'WmRecurrenceRuleChecks'),
                                          ('duration-assertions', 'WmCalendarDurationChecks')]]
            code = next((result for result in results if result != 0), 0)
        record['status'] = 'passed' if code == 0 else 'failed'
    except Exception as error:
        record['status'] = 'runner_error'
        record['error'] = str(error)
        raise
    finally:
        for artifact in sorted(run.rglob('*')):
            if artifact.is_file():
                record['artifacts'][str(artifact.relative_to(run))] = sha(artifact)
        record['sources_unchanged'] = all(sha(p) == record['sources'][str(p.relative_to(root))] for p in sources)
        if not record['sources_unchanged']:
            record['status'] = 'source_changed_during_run'
            code = 1
        (run / 'report.json').write_text(json.dumps(record, indent=2) + '\n', encoding='utf-8')
        print(record['status'] + ': ' + str(run / 'report.json'))
    return code


if __name__ == '__main__':
    raise SystemExit(main())
