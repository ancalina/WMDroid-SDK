#!/usr/bin/env python3
"""Small offline check for the local SDK AAR input and manifest merge."""
import tempfile
import zipfile
from pathlib import Path
import xml.etree.ElementTree as ET
from build_apk import read_sdk_aar, merge_sdk_manifest

ANDROID = '{http://schemas.android.com/apk/res/android}'
MANIFEST = b'''<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="me.ancal.wmdroid.sdk">
 <uses-sdk android:minSdkVersion="14" />
 <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
 <queries><package android:name="me.ancal.wmdroid" /><provider android:authorities="me.ancal.wmdroid.shell" /></queries>
 <application />
</manifest>'''


def check():
    scratch = Path(__file__).resolve().parent.parent / 'build'
    scratch.mkdir(exist_ok=True)
    with tempfile.TemporaryDirectory(dir=scratch, prefix='check-sdk-aar-') as temporary:
        path = Path(temporary) / 'sdk.aar'
        def write(extra=None, manifest=MANIFEST):
            entries = {'AndroidManifest.xml': manifest, 'classes.jar': b'fixture',
                       'R.txt': b'', 'res/': b'', 'assets/fonts/example.ttf': b'font'}
            entries.update(extra or {})
            with zipfile.ZipFile(path, 'w') as archive:
                for name, data in entries.items():
                    archive.writestr(name, data)
        write()
        jar, assets, library = read_sdk_aar(path, 14)
        assert jar == b'fixture' and assets == {'fonts/example.ttf': b'font'}
        app = ET.fromstring('''<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="test.app">
          <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
          <queries><package android:name="me.ancal.wmdroid" />
          </queries><application android:label="app" /></manifest>''')
        merge_sdk_manifest(app, library)
        merge_sdk_manifest(app, library)
        assert len(app.findall('uses-permission')) == 1
        assert len(app.find('queries')) == 2
        assert app.get('package') == 'test.app'
        assert app.find('application').get(ANDROID + 'label') == 'app'
        write({'LICENSE': b'SDK license', 'NOTICE': b'SDK notice'})
        licensed_jar, licensed_assets, _ = read_sdk_aar(path, 14)
        assert licensed_jar == jar and licensed_assets == assets
        empty = ET.fromstring('<manifest><application /></manifest>')
        merge_sdk_manifest(empty, library)
        assert len(empty.findall('uses-permission')) == 1 and len(empty.find('queries')) == 2
        host = ET.fromstring('''<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="me.ancal.wmdroid">
          <application><provider android:authorities="other;me.ancal.wmdroid.shell" /></application></manifest>''')
        merge_sdk_manifest(host, library)
        assert len(host.find('queries')) == 0
        assert len(host.findall('uses-permission')) == 1
        for extra, manifest in [
                ({'assets/../escape': b'x'}, MANIFEST),
                ({'assets//duplicate': b'x'}, MANIFEST),
                ({'res/raw/example.xml': b'x'}, MANIFEST),
                ({'other/LICENSE': b'x'}, MANIFEST),
                ({'R.txt': b'int string example 1'}, MANIFEST),
                ({}, MANIFEST.replace(b'="14"', b'="21"')),
                ({}, MANIFEST.replace(b'me.ancal.wmdroid.sdk', b'other.sdk')),
                ({}, MANIFEST.replace(b'<application />', b'<application android:label="library" />'))]:
            write(extra, manifest)
            try:
                read_sdk_aar(path, 14)
            except SystemExit:
                pass
            else:
                raise AssertionError('Invalid/unsupported AAR was accepted')
    print('PASS local SDK AAR paths, minimum, resource restrictions and idempotent manifest merge')


if __name__ == '__main__':
    check()
