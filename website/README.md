# Kanri Official Website

This directory contains the official website for **Kanri**, an offline-first personal finance ledger for Android.

## Architecture & Principles
- **100% Zero External Dependencies:** No external tracking scripts, no Google Fonts CDN calls, no third-party cookies, no analytics pixels. All fonts, styles, scripts, and media are self-hosted locally in `assets/`.
- **Progressive Enhancement:** Fully readable and usable with JavaScript disabled. Pure Vanilla ES6 JavaScript powers the client-side parser demo, interactive calculators, theme switcher, and instant search.
- **Static Hosting Ready:** Can be served directly by GitHub Pages, Cloudflare Pages, Netlify, Vercel, Nginx, or Apache.

## Local Previewing

You can run any local static file server from within this directory:

### Python 3:
```bash
python -m http.server 8080 --directory .
```
Then open `http://localhost:8080` in your web browser.

### Node.js (`npx serve`):
```bash
npx serve .
```

## Directory Structure
```
website/
├── index.html                  # Landing page
├── privacy/index.html          # Privacy policy
├── terms/index.html            # Terms of use
├── docs/                       # Documentation Hub (11 chapters)
├── changelog/index.html        # Release changelog (v1.19.8 down to v1.19.0)
├── download/index.html         # Canonical download page with verified SHA-256
├── faq/index.html              # Searchable FAQ accordion
├── security/index.html         # Security architecture & responsible disclosure
├── press/index.html            # Press kit & brand assets
├── 404.html                    # On-brand 404 error page
├── sitemap.xml                 # XML sitemap with 20 canonical endpoints
├── robots.txt                  # Search indexing directives
├── llms.txt                    # Factual AI search manifest
├── SEO_PLAN.md                 # Discoverability strategy & keyword map
├── REPORT.md                   # Verification audit & facts report
├── src/                        # Modular React / GSAP UI component templates
│   └── components/ui/
│       ├── cinematic-hero.tsx  # 3D skeuomorphic ScrollTrigger hero with card tilt & phone mockup
│       ├── component.tsx       # Multi-layer Lenis smooth-scroll + GSAP parallax component
│       ├── parallax.css        # Layout & layer styles for ParallaxComponent
│       └── index.ts            # Component exports
└── assets/
    ├── css/                    # Main styles, typography, components, docs
    ├── js/                     # Config, theme, parser demo, calculators, search
    ├── fonts/                  # Self-hosted Panchang Bold, Google Sans, Ndot57
    ├── images/                 # App icons, screenshots, widget previews
    ├── media/                  # 1440 loop video, webm, poster
    └── backdrops/              # Authentic animated HTML canvas artwork
```

## Configuration
Edit `assets/js/config.js` to customize:
- `CONTACT_EMAIL`: Set your official contact email when domain email is configured.
- `PLAY_STORE_URL`: Set the Play Store URL once the app is live on Google Play.
- `VERSION`, `APK_SIZE`, `APK_SHA256`: Updated with new releases.
