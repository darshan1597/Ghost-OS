"""USB device acceptance test. Temporarily disables connectivity; restores it in finally.
Usage: python scripts/test_voice_offline.py <adb.exe> <device-serial>
Install the main APK and its androidTest APK first. The test speaks sample sentences.
"""
import json
import pathlib
import subprocess
import sys

adb, serial = sys.argv[1:3]
output = pathlib.Path('build/voice-research')
output.mkdir(parents=True, exist_ok=True)


def run(*args, timeout=30):
    result = subprocess.run([adb, '-s', serial, *args], capture_output=True,
                            text=True, timeout=timeout, check=True)
    return result.stdout.strip()


states = {key: run('shell', 'settings', 'get', 'global', key)
          for key in ['airplane_mode_on', 'wifi_on', 'mobile_data', 'bluetooth_on']}
(output / 'connectivity-before.json').write_text(json.dumps(states))
print('Saved connectivity:', states, flush=True)
try:
    run('shell', 'cmd', 'connectivity', 'airplane-mode', 'enable')
    run('shell', 'svc', 'wifi', 'disable')
    run('shell', 'svc', 'data', 'disable')
    proof = ('Airplane=' + run('shell', 'settings', 'get', 'global', 'airplane_mode_on')
             + '\n' + run('shell', 'cmd', 'wifi', 'status'))
    print(proof, flush=True)
    assert proof.startswith('Airplane=1') and 'Wifi is disabled' in proof
    result = run('shell', 'am', 'instrument', '-w', '-r',
                 'com.ghostos.ghost_os.test/com.ghostos.ghost_os.voice.VoiceInstrumentation', timeout=240)
    (output / 'offline-device-report.txt').write_text(proof + '\n' + result)
    print(result, flush=True)
    if 'INSTRUMENTATION_CODE: -1' not in result or 'FAILED:' in result:
        raise RuntimeError('Device acceptance test failed; see offline-device-report.txt')
finally:
    run('shell', 'cmd', 'connectivity', 'airplane-mode',
        'enable' if states['airplane_mode_on'] == '1' else 'disable')
    run('shell', 'svc', 'wifi', 'enable' if states['wifi_on'] in ['1', '2'] else 'disable')
    run('shell', 'svc', 'data', 'enable' if states['mobile_data'] == '1' else 'disable')
    if states['bluetooth_on'] == '1':
        run('shell', 'cmd', 'bluetooth_manager', 'enable')
    print('Connectivity restored.', flush=True)
