import os
import sys
import numpy as np
from PIL import Image, ImageDraw, ImageFont
from playwright.sync_api import sync_playwright

def generate_contact_sheets():
    deliverables_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "deliverables"))
    out_dir = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", "out", "sheets"))
    os.makedirs(deliverables_dir, exist_ok=True)
    os.makedirs(out_dir, exist_ok=True)

    settled_times = [b * 0.5 + 0.47 for b in range(32)]
    mid_times = [b * 0.5 + 0.16 for b in range(32)]

    sheet_w, sheet_h = 8 * 360, 4 * 360  # 2880 x 1440 sheet (8 cols x 4 rows, 360x360 thumbnail each)

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
        page = browser.new_page(viewport={"width": 1440, "height": 1440}, device_scale_factor=1)
        page.goto("http://127.0.0.1:8765/index.html")
        page.wait_for_function("window.__ready === true", timeout=15000)

        # Helper to render a 32-frame contact sheet
        def render_sheet(times, filename, title):
            sheet = Image.new("RGB", (sheet_w, sheet_h), "#FAF9F5")
            draw = ImageDraw.Draw(sheet)

            for i, t in enumerate(times):
                beat_num = i + 1
                page.evaluate(f"window.seek({t})")
                png_bytes = page.screenshot()
                img = Image.open(io_bytes := __import__('io').BytesIO(png_bytes))
                thumb = img.resize((360, 360), Image.Resampling.LANCZOS)

                col = i % 8
                row = i // 8
                x = col * 360
                y = row * 360

                sheet.paste(thumb, (x, y))

                # Overlay label
                t_box = Image.new("RGBA", (140, 36), (18, 18, 18, 200))
                sheet.paste(t_box, (x + 12, y + 12), t_box)
                draw.text((x + 20, y + 20), f"Beat {beat_num} ({t:.2f}s)", fill="#F2F2F2")

            out_path = os.path.join(deliverables_dir, filename)
            sheet.save(out_path, quality=95)
            print(f"Saved contact sheet {title}: {out_path} ({sheet_w}x{sheet_h})")

        print("Rendering settled contact sheet (t = beat + 0.47s)...")
        render_sheet(settled_times, "contact_sheet_settled.png", "Settled")

        print("Rendering mid-flight contact sheet (t = beat + 0.16s)...")
        render_sheet(mid_times, "contact_sheet_mid.png", "Mid-flight")

        browser.close()

if __name__ == "__main__":
    generate_contact_sheets()

