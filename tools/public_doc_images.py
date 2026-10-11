"""Pinned SDK control renders: Android API14 drawing code and bundled OFL Gulim."""
import hashlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
IMAGES = {
    'sdk/docs/images/WmChoice.png': 'b8fdc06982e5d15f5f2e6fd77e10c839942d0af2fde6a17c4693e2d13147a8aa',
    'sdk/docs/images/WmThreeStateCheck.png': '39d80976299d6e597d44809369bf8cd1c634dbd2c864feccd22b622366498163',
    'sdk/docs/images/WmTabStrip.png': 'a5bfd5d2f1307b8b81a5ba964aad4f6715187cec9609c0dc7fffc0a4743a5d77',
    'sdk/docs/images/WmScrollBar.png': 'c3b524d9ba1f20cf21d489e27b57caf105f5554c905fc4bae6e14a6f8b1ec283',
}


def validate_doc_image(name, data):
    expected = IMAGES.get(name)
    if expected is None:
        return False
    if hashlib.sha256(data).hexdigest() != expected:
        raise ValueError('Changed documentation render: ' + name)
    return True


def documentation_images():
    result = {}
    for name in IMAGES:
        path = ROOT / name
        if path.is_symlink():
            raise ValueError('Documentation image symlink rejected: ' + name)
        data = path.read_bytes()
        validate_doc_image(name, data)
        result[name] = data
    return result
