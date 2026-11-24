import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest


class ReleasePackagingTest(unittest.TestCase):
    def test_missing_and_mismatched_preview_are_rejected_before_archiving(self):
        script = Path(__file__).with_name('package-android-submission.py')
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            preview = root / 'build/outputs/apk/preview/app-preview.apk'
            for content in [None, b'not the verified preview']:
                with self.subTest(content=content):
                    if content is not None:
                        preview.parent.mkdir(parents=True)
                        preview.write_bytes(content)
                    result = subprocess.run([sys.executable, str(script), '--output', str(root / 'output'),
                        '--video', str(root / 'missing-video.mp4')], capture_output=True, text=True,
                        env=dict(os.environ, NIVORYN_ANDROID_BUILD_DIR=str(root / 'build')))
                    self.assertNotEqual(0, result.returncode)
                    self.assertIn('Preview APK differs from the verified release', result.stderr)
                    self.assertFalse((root / 'output/Nivoryn-native-source.zip').exists())


if __name__ == '__main__':
    unittest.main()
