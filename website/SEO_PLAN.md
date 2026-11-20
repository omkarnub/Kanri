# Kanri Search Engine Optimization (SEO) & Discoverability Plan

This strategic document outlines the keyword architecture, on-page optimization, repository-side configuration, and indexing launch checklist to establish search visibility for **Kanri** as the premier offline-first Android expense tracker.

---

## 1. Keyword Mapping Matrix

| Page URL | Primary Target Keyword | Secondary Keywords | Search Intent |
| :--- | :--- | :--- | :--- |
| **`/` (Home)** | `offline expense tracker android` | `UPI expense tracker no cloud`, `SMS expense tracker India`, `private budget app Android` | High Commercial / Download |
| **`/download`** | `kanri apk download` | `download offline expense tracker apk`, `kanri android release` | Navigational / Download |
| **`/docs`** | `kanri documentation` | `offline expense tracker architecture`, `how kanri works` | Informational |
| **`/docs/capture-dedupe`** | `auto expense tracker SMS India` | `UPI notification reader android`, `sms deduplication expense tracker` | Technical / Informational |
| **`/docs/wallets`** | `cash and bank expense tracker` | `ATM withdrawal transfer expense tracker`, `dual wallet budgeting android` | Informational |
| **`/docs/lend-borrow`** | `lend and borrow tracker android` | `split bills upi deep link`, `whatsapp payment reminder generator` | Feature-Specific |
| **`/docs/budgets-goals`** | `salary cycle budget app` | `safe to spend daily allowance calculator`, `payday budget tracker android` | Feature-Specific |
| **`/docs/widgets`** | `nothing dot matrix widgets android` | `pixel dottiles widgets`, `home screen expense widgets android` | Feature-Specific |
| **`/docs/security-model`** | `sqlcipher expense tracker` | `encrypted budget app android`, `offline finance privacy keystore` | High-Trust Research |
| **`/privacy`** | `kanri privacy policy` | `android expense tracker no analytics`, `drive.file scope expense backup` | Compliance & Trust |
| **`/faq`** | `is kanri safe` | `does kanri read otp`, `which banks work with kanri` | Reassurance & FAQ |

---

## 2. Repo-Side Metadata Checklist

GitHub repository ranking is one of the fastest signals for technical and open-source search queries:

1. **Repository "About" Description:**
   - Update from short summary to:
     > *Offline-first personal finance ledger for Android. Parses Indian bank SMS & UPI notifications on-device with zero cloud servers, zero ads, and SQLCipher AES-256 encryption.*
2. **Repository Website Link:**
   - Set GitHub Website field to: `https://omkarnub.github.io/Kanri/`
3. **Repository Topics (Tags):**
   - Add the following exact tags to the GitHub repository:
     `android`, `expense-tracker`, `kotlin`, `jetpack-compose`, `privacy`, `offline-first`, `upi`, `finance`, `sqlcipher`, `material-design-3`, `open-source`, `gplv3`
4. **README Cross-Linking:**
   - Add a prominent badge and link at the top of `README.md` pointing to the official documentation and website.
5. **GitHub Social Preview Image:**
   - Upload `website/assets/media/poster.png` as the repository's custom Open Graph social preview under *Settings > General > Social preview*.

---

## 3. Search Engine Indexing & Launch Checklist

- [ ] **Google Search Console Setup:**
  - Verify property for `https://omkarnub.github.io/Kanri/` (using existing verification token `DRPrIOg3RE7EbOSP2WX3x3-eEF0cA1l-8SiI9AXF26E`).
  - Submit `https://omkarnub.github.io/Kanri/sitemap.xml`.
  - Request priority indexing for `/` and `/download`.
- [ ] **Bing Webmaster Tools:**
  - Import verified property from Google Search Console.
  - Submit sitemap.
- [ ] **robots.txt & sitemap.xml Verification:**
  - Verify `robots.txt` returns HTTP 200 and allows all crawlers.
  - Ensure all internal URLs are absolute and canonicalized.
- [ ] **Structured Data Testing:**
  - Validate `SoftwareApplication`, `FAQPage`, and `WebSite` JSON-LD schemas using Google's Rich Results Test tool.
- [ ] **Factual LLM Manifest (llms.txt):**
  - Verify `https://omkarnub.github.io/Kanri/llms.txt` is accessible for AI search scrapers (Perplexity, ChatGPT Search, Gemini).
- [ ] **Honest Outreach & Directories:**
  - Submit to privacy and open-source directories: F-Droid / IzzyOnDroid (if applicable), Awesome-Android, AlternativeTo (tagged: "Offline", "Open Source", "No Accounts").
  - Post technical engineering walkthrough on Dev.to / Hashnode / Reddit (`r/androiddev`, `r/privacy`, `r/personalfinanceindia`).

---

## 4. Realistic Search Expectations

> [!NOTE]
> Search ranking algorithms (Google, Bing, DuckDuckGo) evaluate domain authority, historical crawl frequency, backlink velocity, and organic engagement. Ranking for high-competition keywords like "expense tracker" takes 3 to 6 months of steady organic discovery. Niche long-tail queries such as "offline UPI expense tracker android" and "open source SMS expense tracker India" will index and surface significantly faster. Ranking cannot be guaranteed, but strict adherence to semantic HTML, schema metadata, fast Core Web Vitals, and authoritative docs maximizes algorithmic trust.
