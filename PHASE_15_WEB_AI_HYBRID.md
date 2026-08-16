# Phase 15: Web AI Hybrid (Web-Scraped Native Gateway & Element Selector)

## 1. Phase Overview
This expansion phase introduces the **Web AI Hybrid Module** (Type A Hybrid Agent). It allows the native mobile application to wrap any authenticated web-based AI chat interface (e.g., Claude, ChatGPT, Perplexity, DeepSeek Web, or custom self-hosted Web UIs) and expose it as a first-class, uniform AI provider within OmniRoot.

Each configured web endpoint is treated like a virtual API model, enabling users to leverage active web subscriptions, proprietary features, or free web tiers directly in the native Chat UI and within the autonomous Agent Loop.

---

## 2. Systematic Sub-Phases

### Phase 15.1: Directory UI & Profile Data Layer
* **Web AI Collapsible Folder:** Build the `🌐 Web AI (Hybrid)` section in the OmniRoot AI Manager panel (Tab 1: Directory / Add AI) beneath the Local AI section.
* **Search & Add Actions:** Implement the search filter bar and the "+ Add New Site" dialog for custom URLs.
* **Built-in Official Site Templates:**
  - **Frontier AI & Coding:** Claude (`https://claude.ai/new`), ChatGPT (`https://chatgpt.com/`), Gemini Web (`https://gemini.google.com/app`), DeepSeek Web (`https://chat.deepseek.com/`), Grok (`https://grok.com/`), Mistral Le Chat (`https://chat.mistral.ai/chat`), Meta AI (`https://www.meta.ai/`).
  - **Live Search & Research:** Perplexity AI (`https://www.perplexity.ai/`), Genspark AI (`https://www.genspark.ai/`), Phind (`https://www.phind.com/`), Moonshot Kimi (`https://kimi.moonshot.cn/`).
  - **Aggregators & Special Tools:** Poe (`https://poe.com/`), HuggingChat (`https://huggingface.co/chat/`), Microsoft Copilot (`https://copilot.microsoft.com/`), v0 (`https://v0.dev/`).
* **Persistence & Data Schema:** Create the Room database table and JSON configuration manager for storing web endpoint profiles and selectors.

### Phase 15.2: Isolated Auth WebView, Google Master Sign-In & Profile Sandbox
* **Multi-Account Profile Isolation:** Use Android's `WebView.setDataDirectorySuffix("profile_<site_id>")` to sandbox cookies, LocalStorage, and cache per site/account.
* **Persistent Authentication:** Configure `CookieManager.getInstance().setAcceptCookie(true)` and `WebSettings.domStorageEnabled = true` to guarantee login persistence across app restarts (login once, stay logged in forever).
* **Chrome User-Agent Masking:** Strip the `Version/4.0` and `wv` WebView tokens from `WebSettings.userAgentString` and set a standard modern Chrome browser user agent (`Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36`) to prevent Google OAuth `403: disallowed_useragent` blocks.
* **Google Master Account Sign-In (SSO Helper):**
  - Provide a one-tap **"🌐 Sign In to Google"** button in the setup WebView header that navigates directly to `https://accounts.google.com/`.
  - Logging in once preserves Google's session cookies (`SID`, `HSID`, `SSID`) inside that profile's sandbox.
  - When the user subsequently opens Claude, ChatGPT, Perplexity, or Gemini Web in that sandbox and taps *"Continue with Google"*, the native Google Account Chooser screen appears seamlessly for instant one-tap login.
* **Zero-Knowledge Privacy & Security Hardening:**
  - The native `@JavascriptInterface` bridge is detached and disabled during all OAuth and `accounts.google.com` navigation flows to guarantee zero credential/keystroke logging.
  - Strict domain whitelisting in `shouldOverrideUrlLoading` blocks unauthorized external redirects.

### Phase 15.3: Visual Element Selector (Calibration & Self-Healing)
* **Interactive Calibration Overlay:** Build a lightweight JS overlay triggered by an OmniRoot floating action button (FAB) during setup that highlights tapped DOM elements with neon bounding boxes.
* **Smart Auto-Detection:**
  - **Input Detection:** Automatically classifies `contenteditable` divs (ProseMirror/Lexical) vs. standard `<textarea>` elements.
  - **Output Hierarchy Detection:** Automatically walks up DOM nodes to extract both the parent scroll container and the repeating assistant message bubble class.
* **Default Submission:** Configure synthetic Enter Key events (`send_method: "enter_key"`) as the out-of-the-box default, with an optional toggle for calibrating custom Send buttons on non-standard sites.
* **Runtime Self-Healing Protocol:** Surface a non-blocking `[Web AI: UI Changed - Tap to Re-calibrate]` badge in chat if an input fails to focus after 3 retries, allowing one-tap selector updates without session reset.

### Phase 15.4: Universal JS Injection Engine & Output Streaming
* **Universal Input Injector:** Implement `universalInjectText()` utilizing `document.execCommand('insertText')` to dispatch browser-native input events for rich-text editors (ProseMirror, Draft.js, React state), with prototype descriptor fallbacks for controlled textareas.
* **Race-Free Bubble Targeting:** Record baseline bubble count (`countBefore`) before sending; wait for `currentCount > countBefore` before attaching the stream reader to prevent echoing prior messages.
* **Real-Time Streaming:** Ingest tokens via a `MutationObserver` on the conversation container and pipe real-time text chunks to Kotlin.
* **Tri-State Stream Completion Engine:**
  - Primary trigger: Stop/Regenerate button state flipping back to Send/Arrow.
  - Secondary trigger: Removal of streaming DOM attributes (e.g. `class="result-streaming"`, `data-is-streaming="false"`).
  - Safety fallback: 3000ms debounce on DOM modifications when input is re-enabled.

### Phase 15.5: Virtual API Gateway & Adaptive Agent Loop
* **OmniRoot Router Integration:** Register configured web endpoints into the global model picker (e.g., `web/claude-work-pro`, `web/chatgpt-plus`).
* **Multi-Turn Adaptive Prompting:**
  - **Message 1 (Thread Start):** Injects the full Mega-Prompt containing System Persona, Repo File Tree, Tool Schemas, and the User Task.
  - **Subsequent Messages:** Injects only the delta/tool results to leverage native thread memory without bloating context with duplicate repo dumps.
* **Autonomous Tool Loop:** Injects structured `<tool_call>` definitions, intercepts tool calls from the scraped stream, executes local file/terminal operations on device, and injects `<tool_result>` blocks back into the web composer.

### Phase 15.6: Lifecycle Optimization & Manual Web Modal
* **On-Demand Lifecycle Manager:** Pause background WebViews (`webView.onPause()`) when not in active use to ensure zero CPU consumption and minimal RAM usage.
* **Manual Web Inspection Modal:** Add a top-bar **"Open Web Window"** action to slide up the active WebView for Cloudflare challenges, proprietary file uploads (e.g., native PDF parsing on Claude), and viewing web artifacts.

---

## 3. Module Data Schema (`web_ai_configs.json`)

```json
{
  "id": "web_claude_pro_main",
  "name": "Claude Pro (Main)",
  "provider": "web_hybrid",
  "url": "https://claude.ai/new",
  "profile_suffix": "profile_claude_pro_main",
  "selectors": {
    "conversation_container": "[data-testid='conversation']",
    "input_box": "div.ProseMirror[contenteditable='true']",
    "send_button": "button[aria-label='Send Message']",
    "latest_response_bubble": "[data-testid='conversation'] > div:last-child .font-claude-message"
  },
  "send_method": "enter_key",
  "is_configured": true,
  "created_at": 1770984000000
}
```
