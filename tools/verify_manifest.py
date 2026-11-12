#!/usr/bin/env python3
"""Fail if any merged app manifest asks for more than the camera.

Copied from ../stave-android/tools/verify_manifest.py (12 Nov 2026) and turned around for a camera
app: CAMERA moves from forbidden to required, and the list of what may be declared is closed
rather than open. Stave forbids a list; Ikshana allows a list (CAMERA, VIBRATE, and the
signature-level receiver permission androidx.core declares for the app itself), so a permission
nobody thought to forbid fails here too. Network, storage, location, contacts and the microphone
are named as well, so the failure message says which line of the owner's rule broke.

Checked for every merged manifest AGP produced (debug and release) because a release build merges
the same library manifests and must come out identical on these points.
"""
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

paths = [p for p in Path('app/build/intermediates').glob('**/AndroidManifest.xml')
         if 'merged_manifests' in str(p) and 'androidtest' not in str(p).lower()]
if not paths:
    sys.exit('No merged app manifest; run assembleDebug first.')

A = '{http://schemas.android.com/apk/res/android}'
REQUIRED = {'android.permission.CAMERA'}
ALLOWED = REQUIRED | {'android.permission.VIBRATE', 'com.layerbit.ikshana.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'}
NAMED = {
    'network': {'android.permission.INTERNET', 'android.permission.ACCESS_NETWORK_STATE',
                'android.permission.ACCESS_WIFI_STATE', 'android.permission.CHANGE_NETWORK_STATE'},
    'storage': {'android.permission.MANAGE_EXTERNAL_STORAGE', 'android.permission.READ_EXTERNAL_STORAGE',
                'android.permission.WRITE_EXTERNAL_STORAGE', 'android.permission.READ_MEDIA_IMAGES',
                'android.permission.READ_MEDIA_VIDEO', 'android.permission.READ_MEDIA_AUDIO',
                'android.permission.READ_MEDIA_VISUAL_USER_SELECTED'},
    'location': {'android.permission.ACCESS_FINE_LOCATION', 'android.permission.ACCESS_COARSE_LOCATION',
                 'android.permission.ACCESS_BACKGROUND_LOCATION'},
    'contacts': {'android.permission.READ_CONTACTS', 'android.permission.WRITE_CONTACTS',
                 'android.permission.GET_ACCOUNTS'},
    'microphone': {'android.permission.RECORD_AUDIO'},
    'phone': {'android.permission.READ_PHONE_STATE'},
}

for p in paths:
    root = ET.parse(p).getroot()
    declared = {e.get(A + 'name') for e in root.findall('uses-permission')} | \
               {e.get(A + 'name') for e in root.findall('uses-permission-sdk-23')}
    for kind, names in NAMED.items():
        if names & declared:
            sys.exit(f'{p}: {kind} permission declared: {sorted(names & declared)}')
    if not REQUIRED <= declared:
        sys.exit(f'{p}: CAMERA is not declared; the app cannot see.')
    extra = declared - ALLOWED
    if extra:
        sys.exit(f'{p}: permissions outside the allowed list: {sorted(extra)}')
    app = root.find('application')
    if app.get(A + 'allowBackup') != 'false':
        sys.exit(f'{p}: nothing the app keeps may be backed up.')
    if app.get(A + 'dataExtractionRules') != '@xml/data_extraction_rules':
        sys.exit(f'{p}: device-transfer exclusions must be declared.')
    for provider in app.findall('provider'):
        if any(m.get(A + 'name') == 'androidx.emoji2.text.EmojiCompatInitializer' for m in provider.findall('meta-data')):
            sys.exit(f'{p}: automatic emoji-font downloads must stay disabled; remove the initializer in the manifest.')
    # Compose's ui-tooling PreviewActivity is debugImplementation only, so it may appear in a debug
    # manifest and must never appear in a release one.
    launcher = {'.MainActivity', 'com.layerbit.ikshana.MainActivity'}
    if 'debug' in str(p).lower():
        launcher.add('androidx.compose.ui.tooling.PreviewActivity')
    for activity in app.findall('activity'):
        if activity.get(A + 'exported') == 'true' and activity.get(A + 'name') not in launcher:
            sys.exit(f'{p}: exported activity {activity.get(A + "name")}: only the launcher may be.')

rules = ET.parse('app/src/main/res/xml/data_extraction_rules.xml').getroot()
for mode in ('cloud-backup', 'device-transfer'):
    excluded = {e.get('domain') for e in rules.find(mode).findall('exclude')}
    if not {'root', 'file', 'database', 'sharedpref'}.issubset(excluded):
        sys.exit(f'Missing private-data exclusions in {mode}')
print(f'Manifest check passed ({len(paths)} merged manifests): CAMERA, and nothing it may not have.')
