package com.omkarnub.kanri.ui.common.agsl

import android.os.Build
import androidx.annotation.RequiresApi
import com.omkarnub.kanri.ui.common.GlassBackgroundType

/**
 * Library of native AGSL (Android Graphics Shading Language) fragment shaders.
 * Runs directly inside Compose's Skia RenderNode pipeline on Android 13+ (API 33+).
 *
 * Advantages over WebView:
 * - Zero Chromium process overhead, zero IPC.
 * - Blended natively into Compose without TextureView/SurfaceView compositing.
 * - Buttery smooth 120 FPS on high-refresh-rate displays.
 * - Perfect hardware-accelerated Haze frosted glass blur.
 */
object AgslShaderLibrary {

    fun isAgslSupported(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
    }

    fun hasAgslShader(type: GlassBackgroundType): Boolean {
        if (!isAgslSupported()) return false
        return when (type) {
            GlassBackgroundType.CLOUD_SHADER,
            GlassBackgroundType.AURORA,
            GlassBackgroundType.PRISM_GRADIENT,
            GlassBackgroundType.GRAIN_GRADIENT,
            GlassBackgroundType.INK_RIDGES,
            GlassBackgroundType.HERO_GEOMETRIC -> true
            else -> false
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun getAgslSource(type: GlassBackgroundType): String? {
        return when (type) {
            GlassBackgroundType.CLOUD_SHADER -> CLOUD_SHADER_AGSL
            GlassBackgroundType.AURORA -> AURORA_AGSL
            GlassBackgroundType.PRISM_GRADIENT -> PRISM_GRADIENT_AGSL
            GlassBackgroundType.GRAIN_GRADIENT -> GRAIN_GRADIENT_AGSL
            GlassBackgroundType.INK_RIDGES -> INK_RIDGES_AGSL
            GlassBackgroundType.HERO_GEOMETRIC -> HERO_GEOMETRIC_AGSL
            else -> null
        }
    }

    private const val CLOUD_SHADER_AGSL = """
        uniform float2 u_resolution;
        uniform float u_time;

        const mat2 R = mat2(0.80, 0.60, -0.60, 0.80);

        float hash(vec2 p) {
            return fract(sin(dot(p, vec2(41.31, 289.17))) * 26737.367);
        }

        float vnoise(vec2 p) {
            vec2 i = floor(p);
            vec2 f = fract(p);
            f = f * f * (3.0 - 2.0 * f);
            float a = hash(i);
            float b = hash(i + vec2(1.0, 0.0));
            float c = hash(i + vec2(0.0, 1.0));
            float d = hash(i + vec2(1.0, 1.0));
            return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
        }

        float fbm(vec2 p) {
            float sum = 0.0;
            float amp = 0.5;
            for (int i = 0; i < 4; i++) {
                sum += amp * vnoise(p);
                p = R * p * 2.03 + 19.19;
                amp *= 0.5;
            }
            return sum;
        }

        float billow(vec2 p) {
            float sum = 0.0;
            float amp = 0.5;
            for (int i = 0; i < 4; i++) {
                sum += amp * (1.0 - abs(2.0 * vnoise(p) - 1.0));
                p = R * p * 2.11 + 13.37;
                amp *= 0.5;
            }
            return sum;
        }

        float cloudDensity(vec2 p, vec2 c, vec2 r, float seed, float t) {
            vec2 q = p - c;
            float ry = q.y > 0.0 ? r.y : r.y * 0.42;
            float env = 1.0 - length(vec2(q.x / r.x, q.y / ry));
            if (env < -0.35) return 0.0;

            vec2 dp = q * (2.4 / r.x) + seed;
            dp += 0.6 * vec2(
                fbm(dp * 1.4 + t * 0.04),
                fbm(dp * 1.4 + 7.7 - t * 0.03)
            );
            float detail = billow(dp * 1.6);
            return env + (detail - 0.62) * 0.62;
        }

        half4 main(float2 fragCoord) {
            vec2 uv = fragCoord.xy / u_resolution.xy;
            float t = u_time * 0.8;
            
            vec3 skyTop = vec3(0.22, 0.46, 0.73);
            vec3 skyBottom = vec3(0.55, 0.75, 0.91);
            vec3 cloudCol = vec3(0.98, 0.97, 0.95);
            
            vec3 col = mix(skyTop, skyBottom, uv.y);
            vec2 p = (fragCoord.xy - 0.5 * u_resolution.xy) / u_resolution.y;
            
            for (int i = 0; i < 3; i++) {
                float fi = float(i);
                float speed = 0.03 + fi * 0.015;
                float xOffset = (t * speed + fi * 0.7) - floor((t * speed + fi * 0.7) / 2.5) * 2.5 - 1.25;
                vec2 center = vec2(xOffset, 0.15 - fi * 0.22);
                vec2 radius = vec2(0.55 + fi * 0.12, 0.25 + fi * 0.06);
                float d = cloudDensity(p, center, radius, fi * 31.7, t);
                if (d > 0.02) {
                    float dUp = cloudDensity(p + vec2(0.0, radius.y * 0.4), center, radius, fi * 31.7, t);
                    float occl = clamp((dUp - d) * 1.1 + d * 0.55, 0.0, 1.0);
                    vec3 lit = cloudCol * 1.04;
                    vec3 shadow = mix(cloudCol * 0.60, col, 0.35);
                    vec3 cShaded = mix(shadow, lit, occl);
                    float alpha = smoothstep(0.02, 0.35, d);
                    col = mix(col, cShaded, alpha * 0.92);
                }
            }
            
            return half4(col, 1.0);
        }
    """

    private const val AURORA_AGSL = """
        uniform float2 u_resolution;
        uniform float u_time;

        float hash(vec2 p) {
            return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
        }

        float noise(vec2 p) {
            vec2 i = floor(p);
            vec2 f = fract(p);
            f = f * f * (3.0 - 2.0 * f);
            return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
                       mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);
        }

        float fbm(vec2 p) {
            float v = 0.0;
            float a = 0.5;
            mat2 rot = mat2(0.8, -0.6, 0.6, 0.8);
            for (int i = 0; i < 4; i++) {
                v += a * noise(p);
                p = rot * p * 2.0 + vec2(1.7, 9.2);
                a *= 0.5;
            }
            return v;
        }

        half4 main(float2 fragCoord) {
            vec2 uv = fragCoord.xy / u_resolution.xy;
            float t = u_time * 0.25;
            
            vec3 bg = mix(vec3(0.03, 0.04, 0.07), vec3(0.01, 0.02, 0.04), uv.y);
            vec2 p = uv * vec2(2.5, 3.5);
            
            vec2 q = vec2(fbm(p + vec2(0.0, t * 0.4)), fbm(p + vec2(5.2, 1.3 - t * 0.3)));
            vec2 r = vec2(fbm(p + 4.0 * q + vec2(1.7, 9.2 + t * 0.2)), fbm(p + 4.0 * q + vec2(8.3, 2.8 - t * 0.25)));
            
            float f = fbm(p + 3.5 * r);
            
            float wave1 = sin(uv.x * 4.0 + t * 1.5 + f * 3.0) * 0.5 + 0.5;
            float wave2 = cos(uv.x * 3.0 - t * 1.1 + f * 2.5) * 0.5 + 0.5;
            
            float ribbon = smoothstep(0.15, 0.75, f * wave1) * (1.0 - smoothstep(0.4, 0.95, uv.y));
            float ribbon2 = smoothstep(0.2, 0.8, f * wave2) * (1.0 - smoothstep(0.3, 0.85, uv.y));
            
            vec3 emerald = vec3(0.15, 0.85, 0.55);
            vec3 cyan = vec3(0.20, 0.75, 0.95);
            vec3 violet = vec3(0.65, 0.25, 0.85);
            vec3 coral = vec3(0.95, 0.45, 0.35);
            
            vec3 auroraCol = mix(emerald, cyan, wave1);
            auroraCol = mix(auroraCol, violet, ribbon2 * 0.7);
            auroraCol = mix(auroraCol, coral, clamp(r.x * 0.5, 0.0, 1.0));
            
            vec3 finalCol = bg + auroraCol * (ribbon * 1.4 + ribbon2 * 0.9);
            return half4(clamp(finalCol, 0.0, 1.0), 1.0);
        }
    """

    private const val PRISM_GRADIENT_AGSL = """
        uniform float2 u_resolution;
        uniform float u_time;

        float random(vec2 st) {
            return fract(sin(dot(st.xy, vec2(12.9898, 78.233))) * 43758.5453123);
        }

        float noise(vec2 st) {
            vec2 i = floor(st);
            vec2 f = fract(st);
            vec2 u = f * f * (3.0 - 2.0 * f);
            return mix(mix(random(i), random(i + vec2(1.0, 0.0)), u.x),
                       mix(random(i + vec2(0.0, 1.0)), random(i + vec2(1.0, 1.0)), u.x), u.y);
        }

        half4 main(float2 fragCoord) {
            vec2 uv = fragCoord.xy / u_resolution.xy;
            float t = u_time * 0.3;
            
            vec2 p = uv - 0.5;
            p.x *= u_resolution.x / u_resolution.y;
            
            float n1 = noise(p * 2.2 + t);
            float n2 = noise(p * 3.5 - t * 0.7);
            float angle = n1 * 6.2831853;
            p.x += 0.25 * n2 * cos(angle);
            p.y += 0.25 * n2 * sin(angle);
            
            for (float i = 1.0; i <= 3.0; i++) {
                p.x += 0.3 / i * cos(t + i * 1.5 * p.y);
                p.y += 0.3 / i * cos(t + i * p.x);
            }
            
            float mixer = 0.5 + 0.5 * sin(p.x * 2.5) * cos(p.y * 2.5);
            
            vec3 c1 = vec3(0.04, 0.12, 0.24);
            vec3 c2 = vec3(0.68, 0.28, 0.88);
            vec3 c3 = vec3(0.12, 0.82, 0.78);
            
            vec3 col = mix(c1, c2, smoothstep(0.1, 0.65, mixer));
            col = mix(col, c3, smoothstep(0.45, 0.95, mixer));
            
            return half4(col, 1.0);
        }
    """

    private const val GRAIN_GRADIENT_AGSL = """
        uniform float2 u_resolution;
        uniform float u_time;

        half4 main(float2 fragCoord) {
            vec2 uv = fragCoord.xy / u_resolution.xy;
            float t = u_time * 0.4;
            
            float breath = sin(t * 0.52);
            vec2 p = uv - 0.5;
            p.x *= u_resolution.x / u_resolution.y;
            
            float dist = p.x - (0.15 + breath * 0.12 + sin(p.y * 3.5 + t * 0.38) * 0.08);
            float shadow = smoothstep(-0.4, 0.35, dist);
            
            vec3 darkCol = vec3(0.03, 0.08, 0.11);
            vec3 midCol = vec3(0.12, 0.45, 0.48);
            vec3 lightCol = vec3(0.48, 0.88, 0.78);
            
            vec3 col = mix(lightCol, midCol, smoothstep(-0.6, 0.1, dist));
            col = mix(col, darkCol, shadow);
            
            return half4(col, 1.0);
        }
    """

    private const val INK_RIDGES_AGSL = """
        uniform float2 u_resolution;
        uniform float u_time;

        float hash(vec2 p) {
            return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453123);
        }

        float noise(vec2 p) {
            vec2 i = floor(p);
            vec2 f = fract(p);
            f = f * f * (3.0 - 2.0 * f);
            return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
                       mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);
        }

        half4 main(float2 fragCoord) {
            vec2 uv = fragCoord.xy / u_resolution.xy;
            float t = u_time * 0.15;
            
            vec3 bgPaper = vec3(0.92, 0.90, 0.86);
            vec3 inkBlack = vec3(0.08, 0.09, 0.11);
            vec3 mistCol = vec3(0.85, 0.83, 0.80);
            
            vec3 col = bgPaper;
            
            for (int i = 0; i < 4; i++) {
                float fi = float(i);
                float ridgeY = 0.35 + fi * 0.15 + noise(vec2(uv.x * (1.5 + fi * 0.8) + fi * 12.3, t * 0.1)) * 0.15;
                float ridgeMask = smoothstep(ridgeY - 0.015, ridgeY + 0.015, uv.y);
                float depthMist = (1.0 - fi / 4.0) * 0.45;
                vec3 ridgeInk = mix(inkBlack, mistCol, depthMist);
                col = mix(col, ridgeInk, ridgeMask * (0.35 + fi * 0.2));
            }
            
            return half4(col, 1.0);
        }
    """

    private const val HERO_GEOMETRIC_AGSL = """
        uniform float2 u_resolution;
        uniform float u_time;

        half4 main(float2 fragCoord) {
            vec2 uv = fragCoord.xy / u_resolution.xy;
            float t = u_time * 0.3;
            
            vec2 p = (fragCoord.xy - 0.5 * u_resolution.xy) / u_resolution.y;
            
            float angle = atan(p.y, p.x);
            float dist = length(p);
            
            float fold = sin(angle * 6.0 + t) * 0.5 + 0.5;
            float rings = sin(dist * 12.0 - t * 2.0 + fold * 2.0) * 0.5 + 0.5;
            
            vec3 c1 = vec3(0.08, 0.09, 0.14);
            vec3 c2 = vec3(0.42, 0.22, 0.68);
            vec3 c3 = vec3(0.18, 0.55, 0.88);
            
            vec3 col = mix(c1, c2, fold);
            col = mix(col, c3, rings * 0.4);
            
            return half4(col, 1.0);
        }
    """
}
