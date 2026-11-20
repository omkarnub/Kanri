import json
import math
import numpy as np

# Spring properties
K = 280.0
D = 26.0
M = 1.0
WD = math.sqrt(K - (D / 2.0) ** 2)  # sqrt(111) = 10.53565375...

def spring_s(t):
    if t <= 0:
        return 0.0
    return 1.0 - math.exp(-13.0 * t) * (math.cos(WD * t) + (13.0 / WD) * math.sin(WD * t))

# Solve t85 numerically
t_test = np.linspace(0, 0.5, 5000)
s_vals = [spring_s(t) for t in t_test]
idx_85 = np.where(np.array(s_vals) >= 0.85)[0][0]
t85 = float(t_test[idx_85])
print(f"Computed t85: {t85:.4f} s (expected ~0.157 s)")

# Camera calculation: scale = fraction * 1440 / max(focusW, focusH)
# Screen size = 1440 x 1440
# Cursor rest position P0 relative to frame center: (+300, +230) -> (720+300, 720+230) = (1020, 950)
P0 = [300.0, 230.0]

beats_data = [
    # Beat 1: 0.0s
    {
        "beat": 1, "t": 0.0, "state": "add_pill",
        "shape": {"w": 220, "h": 56, "r": 28, "x": 0, "y": 0, "color": "#121212"},
        "cam": {"s": 4.58, "x": 0, "y": 0, "lag_t": 0.0},
        "cursor": {"target": "pill_center", "screen_pos": [0, 0], "press": 0.0, "press_t": 0.40},
        "sfx": [{"t": 0.0, "type": "KICK"}]
    },
    # Beat 2: 0.5s
    {
        "beat": 2, "t": 0.5, "state": "amount_entry",
        "shape": {"w": 440, "h": 200, "r": 36, "x": 0, "y": 0, "color": "#121212"},
        "cam": {"s": 2.29, "x": 0, "y": 0, "lag_t": 0.56},
        "cursor": {"target": "chip_500", "screen_pos": [0, 0], "press": 0.0},
        "sfx": [{"t": 0.50, "type": "CLK_UP"}, {"t": 0.50, "type": "THOCK"}]
    },
    # Beat 3: 1.0s
    {
        "beat": 3, "t": 1.0, "state": "amount_stepped",
        "shape": {"w": 440, "h": 200, "r": 36, "x": 0, "y": 0, "color": "#121212"},
        "cam": {"s": 2.29, "x": 0, "y": 0, "lag_t": 1.06},
        "cursor": {"target": "chip_500", "press": 0.0, "press_t": 0.90, "release_t": 1.00},
        "sfx": [{"t": 0.90, "type": "CLK_DN"}, {"t": 1.00, "type": "CLK_UP"},
                {"t": 1.00, "type": "TICK"}, {"t": 1.04, "type": "TICK"}, {"t": 1.08, "type": "TICK"},
                {"t": 1.12, "type": "TICK"}, {"t": 1.16, "type": "TICK"}]
    },
    # Beat 4: 1.5s
    {
        "beat": 4, "t": 1.5, "state": "amount_footer",
        "shape": {"w": 440, "h": 288, "r": 40, "x": 0, "y": 0, "color": "#121212"},
        "cam": {"s": 2.29, "x": 0, "y": 0, "lag_t": 1.56},
        "cursor": {"target": "save_btn", "press": 0.0, "press_t": 1.90, "release_t": 2.00},
        "sfx": [{"t": 1.50, "type": "THOCK"}]
    },
    # Beat 5: 2.0s
    {
        "beat": 5, "t": 2.0, "state": "circle_spinner",
        "shape": {"w": 128, "h": 128, "r": 64, "x": 0, "y": 0, "color": "#121212"},
        "cam": {"s": 7.90, "x": 0, "y": 0, "lag_t": 2.06},
        "cursor": {"target": "drift_aside", "press": 0.0},
        "sfx": [{"t": 2.00, "type": "CLK_UP"}, {"t": 2.00, "type": "THOCK"}]
    },
    # Beat 6: 2.5s
    {
        "beat": 6, "t": 2.5, "state": "circle_check",
        "shape": {"w": 128, "h": 128, "r": 64, "x": 0, "y": 0, "color": "#30A46C"},
        "cam": {"s": 7.90, "x": 0, "y": 0, "lag_t": 2.56},
        "cursor": {"target": "drift_aside", "press": 0.0},
        "sfx": [{"t": 2.50, "type": "CHIME"}]
    },
    # Beat 7: 3.0s
    {
        "beat": 7, "t": 3.0, "state": "capsule_debit",
        "shape": {"w": 400, "h": 72, "r": 36, "x": 0, "y": 0, "color": "#121212"},
        "cam": {"s": 2.52, "x": 0, "y": 0, "lag_t": 3.06},
        "cursor": {"target": "capsule_bottom_right", "press": 0.0},
        "sfx": [{"t": 3.00, "type": "THOCK"}, {"t": 3.00, "type": "TOAST"}]
    },
    # Beat 8: 3.5s
    {
        "beat": 8, "t": 3.5, "state": "capsule_expanded",
        "shape": {"w": 560, "h": 120, "r": 40, "x": 0, "y": 0, "color": "#121212"},
        "cam": {"s": 1.80, "x": 0, "y": 0, "lag_t": 3.56},
        "cursor": {"target": "split_chip", "press": 0.0, "press_t": 3.90, "release_t": 4.00},
        "sfx": [{"t": 3.50, "type": "TICK"}, {"t": 3.50, "type": "SWEEP"}]
    },
    # Beat 9: 4.0s
    {
        "beat": 9, "t": 4.0, "state": "split_card",
        "shape": {"w": 480, "h": 264, "r": 40, "x": 0, "y": 0, "color": "#121212"},
        "cam": {"s": 2.10, "x": 0, "y": 0, "lag_t": 4.06},
        "cursor": {"target": "slider_thumb", "press": 0.0},
        "sfx": [{"t": 4.00, "type": "CLK_UP"}, {"t": 4.00, "type": "THOCK"}]
    },
    # Beat 10: 4.5s
    {
        "beat": 10, "t": 4.5, "state": "split_slider_grab",
        "shape": {"w": 480, "h": 264, "r": 40, "x": 0, "y": 0, "color": "#121212"},
        "cam": {"s": 2.10, "x": 0, "y": 0, "lag_t": 4.56},
        "cursor": {"target": "slider_thumb", "press": 0.0, "press_t": 4.90},
        "sfx": [{"t": 4.90, "type": "CLK_DN"}]
    },
    # Beat 11: 5.0s
    {
        "beat": 11, "t": 5.0, "state": "split_slider_drag",
        "shape": {"w": 480, "h": 264, "r": 40, "x": 0, "y": 0, "color": "#121212"},
        "cam": {"s": 2.10, "x": 0, "y": 0, "lag_t": 5.06},
        "cursor": {"target": "slider_drag_right", "press": 1.0},
        "sfx": [{"t": 5.00, "type": "TICK"}, {"t": 5.10, "type": "TICK"},
                {"t": 5.20, "type": "TICK"}, {"t": 5.30, "type": "TICK"}]
    },
    # Beat 12: 5.5s
    {
        "beat": 12, "t": 5.5, "state": "split_slider_release",
        "shape": {"w": 480, "h": 264, "r": 40, "x": 0, "y": 0, "color": "#121212"},
        "cam": {"s": 2.10, "x": 0, "y": 0, "lag_t": 5.56},
        "cursor": {"target": "slider_thumb_notch3", "press": 0.0, "release_t": 5.50},
        "sfx": [{"t": 5.50, "type": "CLK_UP"}, {"t": 5.50, "type": "SNAP"}]
    },
    # Beat 13: 6.0s
    {
        "beat": 13, "t": 6.0, "state": "segmented_owe",
        "shape": {"w": 340, "h": 72, "r": 36, "x": 0, "y": 0, "color": "#121212"},
        "cam": {"s": 2.96, "x": 0, "y": 0, "lag_t": 6.06},
        "cursor": {"target": "owed_tab", "press": 0.0, "press_t": 6.40, "release_t": 6.50},
        "sfx": [{"t": 6.00, "type": "THOCK"}]
    },
    # Beat 14: 6.5s
    {
        "beat": 14, "t": 6.5, "state": "segmented_owed",
        "shape": {"w": 340, "h": 72, "r": 36, "x": 0, "y": 0, "color": "#121212"},
        "cam": {"s": 2.96, "x": 0, "y": 0, "lag_t": 6.56},
        "cursor": {"target": "owed_tab", "press": 0.0},
        "sfx": [{"t": 6.50, "type": "CLK_UP"}, {"t": 6.50, "type": "SNAP"}]
    },
    # Beat 15: 7.0s
    {
        "beat": 15, "t": 7.0, "state": "goal_card",
        "shape": {"w": 460, "h": 248, "r": 40, "x": 0, "y": 0, "color": "#121212"},
        "cam": {"s": 2.19, "x": 0, "y": 0, "lag_t": 7.06},
        "cursor": {"target": "goal_chip_5000", "press": 0.0, "press_t": 7.40, "release_t": 7.50},
        "sfx": [{"t": 7.00, "type": "THOCK"}]
    },
    # Beat 16: 7.5s
    {
        "beat": 16, "t": 7.5, "state": "goal_filled",
        "shape": {"w": 460, "h": 248, "r": 40, "x": 0, "y": 0, "color": "#121212"},
        "cam": {"s": 2.19, "x": 0, "y": 0, "lag_t": 7.56},
        "cursor": {"target": "goal_chip_5000", "press": 0.0},
        "sfx": [{"t": 7.50, "type": "CLK_UP"},
                {"t": 7.50, "type": "TICK"}, {"t": 7.54, "type": "TICK"}, {"t": 7.58, "type": "TICK"}]
    },
    # Beat 17: 8.0s
    {
        "beat": 17, "t": 8.0, "state": "insights_card",
        "shape": {"w": 560, "h": 400, "r": 44, "x": 0, "y": 0, "color": "#121212"},
        "cam": {"s": 1.80, "x": 0, "y": 0, "lag_t": 8.06},
        "cursor": {"target": "bar_sat", "press": 0.0},
        "sfx": [{"t": 8.00, "type": "THOCK"}, {"t": 8.00, "type": "SWEEP"}]
    },
    # Beat 18: 8.5s
    {
        "beat": 18, "t": 8.5, "state": "insights_sprout",
        "shape": {"w": 560, "h": 400, "r": 44, "x": 0, "y": 0, "color": "#121212"},
        "cam": {"s": 1.80, "x": 0, "y": 0, "lag_t": 8.56},
        "cursor": {"target": "bar_sat", "press": 0.0},
        "sfx": [{"t": 8.50 + i * 0.05, "type": "TICK"} for i in range(7)]
    },
    # Beat 19: 9.0s
    {
        "beat": 19, "t": 9.0, "state": "insights_tooltip",
        "shape": {"w": 560, "h": 400, "r": 44, "x": 0, "y": 0, "color": "#121212"},
        "cam": {"s": 1.80, "x": 0, "y": 0, "lag_t": 9.06},
        "cursor": {"target": "bar_sat", "press": 0.0},
        "sfx": [{"t": 9.00, "type": "TOAST"}]
    },
    # Beat 20: 9.5s
    {
        "beat": 20, "t": 9.5, "state": "insights_condense",
        "shape": {"w": 520, "h": 360, "r": 40, "x": 0, "y": 0, "color": "#121212"},
        "cam": {"s": 1.94, "x": 0, "y": 0, "lag_t": 9.56},
        "cursor": {"target": "lift_off", "press": 0.0},
        "sfx": [{"t": 9.50, "type": "WHOOSH_SOFT"}]
    },
    # Beat 21: 10.0s
    {
        "beat": 21, "t": 10.0, "state": "search_pill",
        "shape": {"w": 440, "h": 72, "r": 36, "x": 0, "y": 0, "color": "#121212"},
        "cam": {"s": 2.29, "x": 0, "y": 0, "lag_t": 10.06},
        "cursor": {"target": "search_field", "press": 0.0},
        "sfx": [{"t": 10.00, "type": "THOCK"}]
    },
    # Beat 22: 10.5s
    {
        "beat": 22, "t": 10.5, "state": "search_results",
        "shape": {"w": 440, "h": 144, "r": 32, "x": 0, "y": 0, "color": "#121212"},
        "cam": {"s": 2.29, "x": 0, "y": 0, "lag_t": 10.56},
        "cursor": {"target": "keycap_return", "press": 0.0, "press_t": 10.90, "release_t": 11.00},
        "sfx": [{"t": 10.50 + i * 0.083, "type": "KEY"} for i in range(6)]
    },
    # Beat 23: 11.0s
    {
        "beat": 23, "t": 11.0, "state": "vault_badge",
        "shape": {"w": 380, "h": 80, "r": 40, "x": 0, "y": 0, "color": "#121212"},
        "cam": {"s": 2.65, "x": 0, "y": 0, "lag_t": 11.06},
        "cursor": {"target": "badge_drift", "press": 0.0},
        "sfx": [{"t": 11.00, "type": "CLK_UP"}, {"t": 11.00, "type": "THOCK"}]
    },
    # Beat 24: 11.5s
    {
        "beat": 24, "t": 11.5, "state": "vault_lock_close",
        "shape": {"w": 380, "h": 80, "r": 40, "x": 0, "y": 0, "color": "#121212"},
        "cam": {"s": 2.65, "x": 0, "y": 0, "lag_t": 11.56},
        "cursor": {"target": "badge_drift", "press": 0.0},
        "sfx": [{"t": 11.50, "type": "LOCK"}]
    },
    # Beat 25: 12.0s
    {
        "beat": 25, "t": 12.0, "state": "keystore_toast",
        "shape": {"w": 480, "h": 64, "r": 32, "x": 0, "y": 320, "color": "#121212"},
        "cam": {"s": 2.10, "x": 0, "y": 320, "lag_t": 12.06},
        "cursor": {"target": "toast_drift", "press": 0.0},
        "sfx": [{"t": 12.00, "type": "TOAST"}, {"t": 12.00, "type": "SWEEP"}]
    },
    # Beat 26: 12.5s
    {
        "beat": 26, "t": 12.5, "state": "balance_badge",
        "shape": {"w": 300, "h": 88, "r": 44, "x": 0, "y": 0, "color": "#121212"},
        "cam": {"s": 3.36, "x": 0, "y": 0, "lag_t": 12.56},
        "cursor": {"target": "balance_drift", "press": 0.0},
        "sfx": [{"t": 12.50, "type": "THOCK"}]
    },
    # Beat 27: 13.0s
    {
        "beat": 27, "t": 13.0, "state": "balance_flip",
        "shape": {"w": 420, "h": 88, "r": 44, "x": 0, "y": 0, "color": "#121212"},
        "cam": {"s": 2.40, "x": 0, "y": 0, "lag_t": 13.06},
        "cursor": {"target": "balance_center", "press": 0.0},
        "sfx": [{"t": 13.00, "type": "TICK"}, {"t": 13.04, "type": "TICK"},
                {"t": 13.08, "type": "TICK"}, {"t": 13.12, "type": "TICK"}]
    },
    # Beat 28: 13.5s
    {
        "beat": 28, "t": 13.5, "state": "balance_hold",
        "shape": {"w": 420, "h": 88, "r": 44, "x": 0, "y": 0, "color": "#121212"},
        "cam": {"s": 2.40, "x": 0, "y": 0, "lag_t": 13.56},
        "cursor": {"target": "balance_center", "press": 0.0, "press_t": 13.90, "release_t": 14.00},
        "sfx": [{"t": 13.50, "type": "SWEEP"}]
    },
    # Beat 29: 14.0s
    {
        "beat": 29, "t": 14.0, "state": "neutral_capsule",
        "shape": {"w": 300, "h": 64, "r": 32, "x": 0, "y": 0, "color": "#121212"},
        "cam": {"s": 3.36, "x": 0, "y": 0, "lag_t": 14.06},
        "cursor": {"target": "p0", "press": 0.0},
        "sfx": [{"t": 14.00, "type": "CLK_UP"}, {"t": 14.00, "type": "THOCK"}]
    },
    # Beat 30: 14.5s (LAST GEOMETRY RETARGET)
    {
        "beat": 30, "t": 14.5, "state": "initial_capsule",
        "shape": {"w": 220, "h": 56, "r": 28, "x": 0, "y": 0, "color": "#121212"},
        "cam": {"s": 4.58, "x": 0, "y": 0, "lag_t": 14.56},
        "cursor": {"target": "p0", "press": 0.0},
        "sfx": [{"t": 14.50, "type": "SNAP"}]
    },
    # Beat 31: 15.0s (Content reveal)
    {
        "beat": 31, "t": 15.0, "state": "reveal_add",
        "shape": {"w": 220, "h": 56, "r": 28, "x": 0, "y": 0, "color": "#121212"},
        "cam": {"s": 4.58, "x": 0, "y": 0, "lag_t": 15.00},
        "cursor": {"target": "p0", "press": 0.0},
        "sfx": [{"t": 15.00, "type": "TICK"}]
    },
    # Beat 32: 15.5s (Hold settled)
    {
        "beat": 32, "t": 15.5, "state": "settled_start",
        "shape": {"w": 220, "h": 56, "r": 28, "x": 0, "y": 0, "color": "#121212"},
        "cam": {"s": 4.58, "x": 0, "y": 0, "lag_t": 15.50},
        "cursor": {"target": "p0", "press": 0.0},
        "sfx": []
    }
]

# Extract channel events
channels = {
    "shape_w": {"base": 220.0, "events": []},
    "shape_h": {"base": 56.0, "events": []},
    "shape_r": {"base": 28.0, "events": []},
    "shape_x": {"base": 0.0, "events": []},
    "shape_y": {"base": 0.0, "events": []},
    "cam_log_scale": {"base": math.log(4.58), "events": []},
    "cam_x": {"base": 0.0, "events": []},
    "cam_y": {"base": 0.0, "events": []},
}

# Populate channel events from beats_data
curr_targets = {
    "shape_w": 220.0,
    "shape_h": 56.0,
    "shape_r": 28.0,
    "shape_x": 0.0,
    "shape_y": 0.0,
    "cam_log_scale": math.log(4.58),
    "cam_x": 0.0,
    "cam_y": 0.0,
}

for b in beats_data:
    t = b["t"]
    sh = b["shape"]
    cm = b["cam"]
    lag_t = cm.get("lag_t", t + 0.060)

    for ch_name, val in [
        ("shape_w", float(sh["w"])),
        ("shape_h", float(sh["h"])),
        ("shape_r", float(sh["r"])),
        ("shape_x", float(sh["x"])),
        ("shape_y", float(sh["y"])),
    ]:
        if abs(val - curr_targets[ch_name]) > 1e-4:
            channels[ch_name]["events"].append({"t": round(t, 4), "target": val})
            curr_targets[ch_name] = val

    # Camera events lag shape
    target_log_s = math.log(cm["s"])
    if abs(target_log_s - curr_targets["cam_log_scale"]) > 1e-4:
        channels["cam_log_scale"]["events"].append({"t": round(lag_t, 4), "target": target_log_s})
        curr_targets["cam_log_scale"] = target_log_s

    target_cam_x = float(cm["x"])
    if abs(target_cam_x - curr_targets["cam_x"]) > 1e-4:
        channels["cam_x"]["events"].append({"t": round(lag_t, 4), "target": target_cam_x})
        curr_targets["cam_x"] = target_cam_x

    target_cam_y = float(cm["y"])
    if abs(target_cam_y - curr_targets["cam_y"]) > 1e-4:
        channels["cam_y"]["events"].append({"t": round(lag_t, 4), "target": target_cam_y})
        curr_targets["cam_y"] = target_cam_y

# Verify that for each channel, final target == base (so sum of delta = 0)
print("\n--- Verifying Channel Periodicity (Sum of Delta = 0) ---")
all_periodic = True
for name, ch in channels.items():
    events = ch["events"]
    if not events:
        print(f"Channel {name}: static (OK)")
        continue
    first_target = ch["base"]
    final_target = events[-1]["target"]
    total_delta = final_target - first_target
    last_t = events[-1]["t"]
    print(f"Channel {name:14s}: base={first_target:8.3f}, final={final_target:8.3f}, total_delta={total_delta:8.3f}, last_t={last_t}s")
    if abs(total_delta) > 1e-4:
        print(f"ERROR: Channel {name} is NOT periodic! total_delta = {total_delta}")
        all_periodic = False
    if last_t > 14.6:
        print(f"WARNING: Channel {name} last event at {last_t}s (> 14.5s)")

# Collect all SFX cues
all_sfx = []
for b in beats_data:
    for s in b.get("sfx", []):
        all_sfx.append(s)
all_sfx.sort(key=lambda x: x["t"])

timeline_payload = {
    "meta": {
        "bpm": 120,
        "bars": 8,
        "beats": 32,
        "duration_seconds": 16.000,
        "seconds_per_beat": 0.500,
        "spring": {"k": K, "d": D, "m": M, "wd": WD, "t85": round(t85, 4), "overshoot": 0.02072},
        "cursor_p0": P0,
        "stage_size": [1440, 1440]
    },
    "beats": beats_data,
    "channels": channels,
    "sfx_cues": all_sfx
}

with open("kanri-motion/timeline.json", "w", encoding="utf-8") as f:
    json.dump(timeline_payload, f, indent=2)

print("\ntimeline.json generated successfully. All periodic:", all_periodic)

