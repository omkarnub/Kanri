import os
from PIL import Image

STILLS_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "out", "stills"))
OUT_PATH = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "deliverables", "showcase_contact_sheet.png"))

files = [
    "still_00.00.png", "still_01.00.png", "still_02.60.png",
    "still_04.40.png", "still_05.30.png", "still_06.20.png",
    "still_07.80.png", "still_10.20.png", "still_12.20.png",
    "still_14.00.png"
]

cols = 5
rows = 2
thumb_w = 480
thumb_h = 270

sheet = Image.new("RGB", (cols * thumb_w, rows * thumb_h), (10, 10, 15))

for idx, f in enumerate(files):
    img_path = os.path.join(STILLS_DIR, f)
    if os.path.exists(img_path):
        im = Image.open(img_path).resize((thumb_w, thumb_h), Image.Resampling.LANCZOS)
        c = idx % cols
        r = idx // cols
        sheet.paste(im, (c * thumb_w, r * thumb_h))

sheet.save(OUT_PATH)
print(f"Contact sheet saved to {OUT_PATH} ({os.path.getsize(OUT_PATH)} bytes)")
