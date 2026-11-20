import time
from playwright.sync_api import sync_playwright

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
    page.wait_for_function("window.__ready === true", timeout=10000)

    # Check font loading
    font_panchang = page.evaluate("document.fonts.check('700 22px Panchang')")
    font_sans = page.evaluate("document.fonts.check('500 16px \"Google Sans\"')")
    print(f"Font Panchang loaded: {font_panchang}")
    print(f"Font Google Sans loaded: {font_sans}")

    # Check qa() at t=0
    qa0 = page.evaluate("window.seek(0); window.qa();")
    print(f"QA at t=0: {qa0}")

    # Test seek at several key beats
    for t_test in [0.0, 1.0, 5.2, 8.6, 9.0, 11.6, 15.9]:
        res = page.evaluate(f"window.seek({t_test}); window.qa();")
        print(f"t={t_test:4.1f}s -> focusFraction={res['focusFraction']:.3f}, visibleLayers={res['visibleLayers']}, shapeScreen={res['shapeScreen']}")

    browser.close()

