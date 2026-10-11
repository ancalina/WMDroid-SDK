#!/usr/bin/env python3
"""Build the local WMDroid SDK AAR and complete source ZIP without downloads."""
import argparse
import hashlib
import io
import json
import subprocess
import tempfile
import zipfile
from public_fonts import font_assets
from public_doc_images import documentation_images
from dependency_notices import source_files
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SDK = ROOT / 'sdk'
LICENSE_FILES = ('LICENSE', 'NOTICE', 'LICENSES.md', 'sdk/LICENSE', 'sdk/NOTICE',
                 'sdk/sample/LICENSE', 'sdk/widgets/LICENSE', 'sdk/today-probe/LICENSE', 'tools/LICENSE')
SDK_CHECK_FILES = ('tools/run_recurrence_rule_checks.py',
                   'sdk/tests/WmRecurrenceRuleChecks.java',
                   'sdk/tests/WmCalendarDurationChecks.java',
                   'tools/run_reminder_checks.py', 'sdk/tests/WmReminderChecks.java',
                   'tools/run_appointment_text_checks.py', 'sdk/tests/WmAppointmentTextChecks.java')
SDK_DOC_FILES = ('sdk/docs/package.json', 'sdk/docs/package-lock.json',
                 'sdk/docs/astro.config.mjs', 'sdk/docs/tsconfig.json',
                 'sdk/docs/integrations/search.mjs', 'sdk/docs/integrations/contracts.mjs',
                 'sdk/docs/src/components/Search.astro', 'sdk/docs/src/components/Head.astro',
                 'sdk/docs/src/components/PageTitle.astro',
                 'sdk/docs/images/favicon.svg',
                 'sdk/docs/src/content/docs/404.md',
                 'sdk/docs/src/content.config.ts', 'sdk/docs/src/styles/custom.css',
                 'sdk/docs/src/content/i18n/en.json')
MANIFEST = b'''<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="me.ancal.wmdroid.sdk">
 <uses-sdk android:minSdkVersion="14" />
 <uses-permission android:name="android.permission.INTERNET" />
 <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
 <uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
 <uses-permission android:name="android.permission.ACCESS_NOTIFICATION_POLICY" />
 <uses-permission android:name="android.permission.READ_PHONE_STATE" />
 <queries><intent><action android:name="android.intent.action.MAIN" /><category android:name="android.intent.category.LAUNCHER" /></intent><package android:name="me.ancal.wmdroid" /><provider android:authorities="me.ancal.wmdroid.shell" /></queries>
 <application />
</manifest>
'''


def sha(data):
    return hashlib.sha256(data).hexdigest()


def license_declarations():
    entries = {}
    for name in LICENSE_FILES:
        path = ROOT / name
        if path.is_symlink() or not path.is_file():
            raise ValueError('Required license declaration missing or symlinked: ' + name)
        entries[name] = path.read_bytes()
    return entries


def archive(entries):
    output = io.BytesIO()
    with zipfile.ZipFile(output, 'w', zipfile.ZIP_DEFLATED) as target:
        for name, data in sorted(entries.items()):
            info = zipfile.ZipInfo(name, (1980, 1, 1, 0, 0, 0))
            info.compress_type = zipfile.ZIP_DEFLATED
            info.external_attr = (0o40755 if name.endswith('/') else 0o100644) << 16
            target.writestr(info, data)
    result = output.getvalue()
    with zipfile.ZipFile(io.BytesIO(result)) as check:
        if check.testzip() is not None or set(check.namelist()) != set(entries):
            raise RuntimeError('Archive integrity or file-list verification failed')
        for name, expected in entries.items():
            if sha(check.read(name)) != sha(expected):
                raise RuntimeError(f'Archive content verification failed: {name}')
    return result


def sdk_license_declarations():
    entries = {}
    for name in ('sdk/LICENSE', 'sdk/NOTICE', 'tools/LICENSE'):
        path = ROOT / name
        if path.is_symlink() or not path.is_file():
            raise ValueError('Required SDK license declaration missing or symlinked: ' + name)
        entries[name] = path.read_bytes()
    entries['LICENSE'] = entries['sdk/LICENSE']
    entries['NOTICE'] = entries['sdk/NOTICE']
    entries['LICENSES.md'] = b'''# License scope

WMDroid-authored SDK code, templates and documentation use Apache-2.0. See LICENSE.

WMDroid-authored files under tools/ use GPL-3.0-or-later (tools/LICENSE), except build_sdk.py, public_fonts.py and public_doc_images.py, which use Apache-2.0.

Third-party files retain their licenses. The bundled Gulim font uses SIL Open Font License 1.1; see sdk/src/main/assets/fonts/gulim/OFL.txt and AUTHORS.txt. Dependency notices are under third_party/.

Applications that use the SDK need not adopt the launcher's GPL license.
'''
    return entries


def documentation_sources():
    entries = {}
    paths = [ROOT / name for name in SDK_DOC_FILES]
    for language in ('ko', 'en'):
        paths.extend(sorted((SDK / 'docs/src/content/docs' / language).rglob('*.md')))
    for path in paths:
        relative = path.relative_to(ROOT)
        if any(part in {'node_modules', 'dist', '.astro'} for part in relative.parts):
            continue
        for parent in (path, *path.parents):
            if parent == ROOT:
                break
            if parent.is_symlink():
                raise ValueError('SDK documentation symlink rejected: ' + str(path))
        if not path.is_file():
            raise ValueError('Required SDK documentation source missing: ' + str(path))
        entries[relative.as_posix()] = path.read_bytes()
    entries.update(documentation_images())
    return entries


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--sdk', type=Path, required=True)
    parser.add_argument('--jdk', type=Path, required=True)
    parser.add_argument('--platform', default='android-35')
    args = parser.parse_args()
    android = args.sdk / 'platforms' / args.platform / 'android.jar'
    javac = args.jdk / 'bin/javac'
    if not android.is_file() or not javac.is_file():
        parser.error('Existing android.jar and JDK javac are required; nothing was downloaded.')
    source_root = SDK / 'src/main/java'
    sources = sorted(source_root.rglob('*.java'))
    if not sources:
        parser.error('SDK Java sources are missing.')
    declarations = sdk_license_declarations()
    sdk_assets = SDK / 'src/main/assets'
    # Public AAR includes only authored catalogs and pinned unmodified OFL Gulim.
    # User packs supply optional original artwork, fonts, sounds and palettes.
    assets = sorted((sdk_assets / 'i18n').glob('*.json'))
    if {path.name for path in assets} != {'ko.json', 'en.json'}:
        parser.error('Korean and English UI catalogs are required.')
    build = SDK / 'build'
    build.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='compile-', dir=build) as temporary:
        classes = Path(temporary)
        subprocess.run([str(javac), '--release', '17', '-g:none', '-encoding', 'UTF-8',
                        '-classpath', str(android), '-d', str(classes),
                        *map(str, sources)], check=True, cwd=ROOT)
        compiled = {path.relative_to(classes).as_posix(): path.read_bytes()
                    for path in sorted(classes.rglob('*.class'))}
        expected = {path.relative_to(source_root).with_suffix('.class').as_posix()
                    for path in sources}
        if not expected.issubset(compiled):
            raise RuntimeError('Compiled SDK top-level class list is incomplete')
        if any(data[:4] != b'\xca\xfe\xba\xbe' or int.from_bytes(data[6:8], 'big') != 61
               for data in compiled.values()):
            raise RuntimeError('Expected Java 17 class files')
        classes_jar = archive(compiled)
    asset_entries = {'assets/' + path.relative_to(sdk_assets).as_posix(): path.read_bytes()
                     for path in assets}
    asset_entries.update({'assets/' + name: data for name, data in font_assets().items()})
    aar_entries = {'AndroidManifest.xml': MANIFEST, 'classes.jar': classes_jar,
                   'R.txt': b'', 'res/': b'', 'LICENSE': declarations['sdk/LICENSE'],
                   'NOTICE': declarations['sdk/NOTICE'], **asset_entries}
    source_entries = {path.relative_to(ROOT).as_posix(): path.read_bytes() for path in sources}
    source_entries.update(declarations)
    for name in ('.gitignore', '.gitattributes', 'DEPENDENCIES.md'):
        source_entries[name] = (ROOT / name).read_bytes()
    source_entries.update(source_files())
    source_entries.update({path.relative_to(ROOT).as_posix(): path.read_bytes()
                           for path in assets})
    source_entries.update({'sdk/src/main/assets/' + name: data for name, data in font_assets().items()})
    source_entries['sdk/src/main/AndroidManifest.xml'] = MANIFEST
    source_entries.update({path.relative_to(ROOT).as_posix(): path.read_bytes() for path in sorted((SDK / 'templates').rglob('*')) if path.is_file()})
    for name in ('README.md', 'README.ko.md'):
        text = (SDK / name).read_text(encoding='utf-8')
        source_entries['sdk/' + name] = text.encode('utf-8')
        for before, after in (('](docs/', '](sdk/docs/'),
                              ('](../DEPENDENCIES.md)', '](DEPENDENCIES.md)'),
                              ('](../LICENSES.md)', '](LICENSES.md)'),
                              ('](LICENSE)', '](sdk/LICENSE)'),
                              ('](NOTICE)', '](sdk/NOTICE)')):
            text = text.replace(before, after)
        source_entries[name] = text.encode('utf-8')
    source_entries['.github/workflows/docs.yml'] = (SDK / 'docs-pages.yml').read_bytes()
    source_entries.update(documentation_sources())
    for name in SDK_CHECK_FILES:
        path = ROOT / name
        if path.is_symlink() or not path.is_file():
            raise ValueError('Required SDK check source missing or symlinked: ' + name)
        source_entries[name] = path.read_bytes()
    source_entries['tools/build_sdk.py'] = Path(__file__).read_bytes()
    for helper in ('public_fonts.py', 'public_doc_images.py', 'dependency_notices.py', 'build_apk.py', 'check_sdk_aar.py'):
        source_entries['tools/' + helper] = (ROOT / 'tools' / helper).read_bytes()
    for dependency in ('desugar_jdk_libs/2.1.5', 'multidex/2.0.1'):
        name = 'tools/cache/' + dependency + '/dependencies.json'
        source_entries[name] = (ROOT / name).read_bytes()
    outputs = {'WMDroid-sdk.aar': archive(aar_entries),
               'WMDroid-sdk-sources.zip': archive(source_entries)}
    for name, data in outputs.items():
        (build / name).write_bytes(data)
    report = {'package': 'me.ancal.wmdroid.sdk', 'min_sdk': 14,
              'java_release': 17, 'platform': args.platform,
              'source_count': len(sources), 'class_count': len(compiled),
              'runtime_verified': False,
              'license_declarations_present': True,
              'checks': {'zip_crc': True, 'archive_file_lists': True,
                         'source_completeness': True, 'class_completeness': True,
                         'asset_bytes_match_sdk': True,
                         'public_asset_allowlist': True},
              'artifacts': {name: {'bytes': len(data), 'sha256': sha(data)}
                            for name, data in outputs.items()},
              'aar_files': sorted(aar_entries), 'source_files': sorted(source_entries),
              'vendor_asset_count': 0,
              'assets': {name: sha(data) for name, data in asset_entries.items()}}
    (build / 'sdk-build-report.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'outputs': report['artifacts'], 'sources': len(sources),
                      'classes': len(compiled), 'checks': report['checks']}, indent=2))


if __name__ == '__main__':
    main()
