import os
import subprocess
from playwright.sync_api import sync_playwright

OUT_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "out", "stills"))
os.makedirs(OUT_DIR, exist_ok=True)

TIMESTAMPS = [0.00, 1.00, 2.60, 4.40, 5.30, 6.20, 7.80, 10.20, 12.20, 14.00, 15.00]

with sync_playwright() as p:
    browser = p.chromium.launch(
        args=[
            "--force-color-profile=srgb",
            "--font-render-hinting=none",
            "--disable-lcd-text",
            "--hide-scrollbars",
            "--run-all-compositor-stages-before-draw"
        ]
    )
    page = browser.new_page(viewport={"width": 1920, "height": 1080}, device_scale_factor=1)
    page.goto("http://127.0.0.1:8765/showcase.html")
    page.wait_for_function("window.__ready === true", timeout=15000)

    for t in TIMESTAMPS:
        t_wrapped = ((t % 15.000) + 15.000) % 15.000
        page.evaluate(f"window.seek({t_wrapped})")
        out_png = os.path.join(OUT_DIR, f"still_{t:05.2f}.png")
        page.screenshot(path=out_png)
        print(f"Captured {out_png}")

    browser.close()

# Verify seam
s0_size = os.path.getsize(os.path.join(OUT_DIR, "still_00.00.png"))
s15_size = os.path.getsize(os.path.join(OUT_DIR, "still_15.00.png"))
print(f"Seam check: 0.00s={s0_size} bytes, 15.00s={s15_size} bytes. Identical={s0_size == s15_size}")
