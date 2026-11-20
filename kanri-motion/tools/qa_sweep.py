from playwright.sync_api import sync_playwright

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={"width": 1440, "height": 1440}, device_scale_factor=1)
    page.goto("http://127.0.0.1:8765/index.html")
    page.wait_for_function("window.__ready === true", timeout=10000)

    print("--- 32-BEAT QA SWEEP ---")
    all_ok = True
    for b in range(32):
        beat_num = b + 1
        t_settled = b * 0.5 + 0.47
        res = page.evaluate(f"window.seek({t_settled}); window.qa();")
        ff = res["focusFraction"]
        vl = res["visibleLayers"]
        sh = res["shapeScreen"]

        # Gate 5: Camera focus fraction in 0.65 - 0.75
        pass_cam = 0.65 <= ff <= 0.75
        # Gate 6: Content overlap: visibleLayers <= 1 when settled
        pass_layer = (vl <= 1)

        status = "PASS" if (pass_cam and pass_layer) else "FAIL"
        if status == "FAIL":
            all_ok = False
        print(f"Beat {beat_num:2d} (t={t_settled:5.2f}s): {status} | Focus: {ff:.3f} | VisibleLayers: {vl} | Shape: {sh['width']:.1f}x{sh['height']:.1f}")

    print(f"\nQA Sweep Complete. All Passed: {all_ok}")
    browser.close()

