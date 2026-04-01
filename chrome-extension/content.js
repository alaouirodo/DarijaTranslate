let tooltipContainer = null;
let shadowRoot = null;
let currentAudio = null;

function initTooltip() {
    if (tooltipContainer) return;

    tooltipContainer = document.createElement("div");
    tooltipContainer.id = "darija-translator-root";
    document.body.appendChild(tooltipContainer);

    shadowRoot = tooltipContainer.attachShadow({ mode: "open" });

    const styleEl = document.createElement("style");
    styleEl.textContent = `
      #panel { position: fixed; width: 300px; background: #ffffff; border: 1px solid #E5E7EB; border-radius: 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); padding: 12px; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; color: #111827; z-index: 2147483647; animation: fadeIn 0.2s ease-out forwards; }
      #panel.hidden { display: none; }
      @keyframes fadeIn { from { opacity: 0; transform: translateY(-4px); } to { opacity: 1; transform: translateY(0); } }
      .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; border-bottom: 1px solid #F3F4F6; padding-bottom: 8px; }
      .lang-tag { font-size: 11px; font-weight: 600; color: #6B7280; text-transform: uppercase; }
      button { background: none; border: none; cursor: pointer; font-family: inherit; font-size: 13px; border-radius: 6px; }
      #closeBtn { color: #9CA3AF; padding: 2px 6px; }
      #closeBtn:hover { background: #F3F4F6; color: #111827; }
      .content { font-size: 14px; line-height: 1.5; min-height: 40px; color: #111827; margin-bottom: 12px; }
      .actions { display: flex; gap: 8px; justify-content: flex-end; }
      .actions button { background: #F9FAFB; border: 1px solid #E5E7EB; padding: 4px 10px; color: #374151; font-weight: 500; }
      .actions button:hover { background: #F3F4F6; }
      @media (prefers-color-scheme: dark) { #panel { background: #111827; border-color: #374151; color: #F9FAFB; } .content { color: #F9FAFB; } .actions button { background: #1F2937; border-color: #374151; color: #D1D5DB; } }
    `;
    shadowRoot.appendChild(styleEl);

    const panel = document.createElement("div");
    panel.id = "panel";
    panel.className = "hidden";
    panel.innerHTML = `
    <div class="header">
      <span class="lang-tag">EN &rarr; DAR</span>
      <button id="closeBtn" title="Close">X</button>
    </div>
    <div class="content" id="outputContent">Translating...</div>
    <div class="actions">
      <button id="speakBtn" title="Speak">Speak</button>
      <button id="copyBtn" title="Copy">Copy</button>
    </div>
  `;
    shadowRoot.appendChild(panel);

    shadowRoot.getElementById("closeBtn").addEventListener("click", hidePanel);

    shadowRoot.getElementById("copyBtn").addEventListener("click", () => {
        const text = shadowRoot.getElementById("outputContent").textContent;
        navigator.clipboard.writeText(text);
        const btn = shadowRoot.getElementById("copyBtn");
        const orig = btn.innerHTML;
        btn.innerHTML = "Copied!";
        setTimeout(() => btn.innerHTML = orig, 1500);
    });

    shadowRoot.getElementById("speakBtn").addEventListener("click", () => {
        const text = shadowRoot.getElementById("outputContent").getAttribute("data-text");
        if (!text) return;
        const btn = shadowRoot.getElementById("speakBtn");

        if (currentAudio && !currentAudio.paused) {
            currentAudio.pause();
            currentAudio = null;
            btn.innerHTML = "Speak";
            return;
        }

        btn.innerHTML = "Loading...";
        chrome.runtime.sendMessage({ type: "API_TTS", text }, (res) => {
            if (res.error) {
                btn.innerHTML = "Error";
                setTimeout(() => btn.innerHTML = "Speak", 2000);
                return;
            }
            currentAudio = new Audio(`data:${res.data.mimeType || 'audio/wav'};base64,${res.data.audioContent}`);
            currentAudio.play();
            btn.innerHTML = "Stop";
            currentAudio.onended = () => {
                btn.innerHTML = "Speak";
            };
        });
    });

    document.addEventListener("mousedown", (e) => {
        if (e.target !== tooltipContainer) {
            hidePanel();
        }
    });
}

function hidePanel() {
    if (shadowRoot) {
        const panel = shadowRoot.getElementById("panel");
        if (panel) panel.classList.add("hidden");
        if (currentAudio) {
            currentAudio.pause();
            currentAudio = null;
            shadowRoot.getElementById("speakBtn").innerHTML = "Speak";
        }
    }
}

function showPanel(rect, text) {
    initTooltip();
    const panel = shadowRoot.getElementById("panel");
    panel.classList.remove("hidden");

    let top = rect.bottom + 8;
    let left = rect.left;

    if (left + 300 > window.innerWidth) left = window.innerWidth - 320;
    if (top + 150 > window.innerHeight) top = rect.top - 158;
    if (left < 8) left = 8;

    panel.style.top = `${top}px`;
    panel.style.left = `${left}px`;

    const output = shadowRoot.getElementById("outputContent");
    output.textContent = "Translating...";
    output.removeAttribute("data-text");

    chrome.runtime.sendMessage({ type: "API_TRANSLATE", text }, (res) => {
        if (!res || res.error) {
            const msg = res?.error || "Service unavailable";
            output.textContent = msg.includes("Not logged in")
                ? "Please login via extension popup first."
                : "Error: " + msg;
            return;
        }

        chrome.storage.sync.get(["preferArabic"], (prefs) => {
            const trans = res.data.translation;
            const latin = res.data.latin || trans;
            const finalTxt = prefs.preferArabic ? trans : latin;

            output.textContent = finalTxt;
            output.setAttribute("data-text", finalTxt);
            if (prefs.preferArabic) {
                output.style.direction = "rtl";
                output.style.fontFamily = "'Noto Sans Arabic', sans-serif";
                output.style.fontSize = "16px";
            } else {
                output.style.direction = "ltr";
                output.style.fontFamily = "'Inter', sans-serif";
                output.style.fontSize = "14px";
            }
        });
    });
}

chrome.runtime.onMessage.addListener((msg, sender, sendResponse) => {
    if (msg.type === "SHOW_TRANSLATION_PANEL") {
        const selection = window.getSelection();
        let rect = { bottom: window.innerHeight / 2, left: window.innerWidth / 2 - 150 };

        if (selection.rangeCount > 0) {
            const selRect = selection.getRangeAt(0).getBoundingClientRect();
            if (selRect.width > 0 || selRect.height > 0) {
                rect = selRect;
            }
        }

        showPanel(rect, msg.text);
    }
});

let quickBtn = null;

function createQuickButton() {
    if (quickBtn) return;

    quickBtn = document.createElement("div");
    quickBtn.id = "darija-quick-btn";
    quickBtn.textContent = "DT";
    quickBtn.title = "Translate to Darija";

    quickBtn.style.position = "absolute";
    quickBtn.style.zIndex = "2147483645";
    quickBtn.style.background = "#2563EB";
    quickBtn.style.color = "white";
    quickBtn.style.width = "28px";
    quickBtn.style.height = "28px";
    quickBtn.style.borderRadius = "50%";
    quickBtn.style.display = "flex";
    quickBtn.style.alignItems = "center";
    quickBtn.style.justifyContent = "center";
    quickBtn.style.fontSize = "13px";
    quickBtn.style.fontWeight = "bold";
    quickBtn.style.cursor = "pointer";
    quickBtn.style.boxShadow = "0 2px 6px rgba(0,0,0,0.2)";
    quickBtn.style.display = "none";
    quickBtn.style.fontFamily = "'Inter', sans-serif";

    document.body.appendChild(quickBtn);

    quickBtn.addEventListener("mousedown", (e) => e.preventDefault());

    quickBtn.addEventListener("click", (e) => {
        e.stopPropagation();
        quickBtn.style.display = "none";

        const selection = window.getSelection();
        const text = selection.toString().trim();
        if (text.length > 0 && selection.rangeCount > 0) {
            const rect = selection.getRangeAt(0).getBoundingClientRect();
            showPanel(rect, text);
        }
    });
}

document.addEventListener("mouseup", (e) => {
    if (e.target === quickBtn || (tooltipContainer && tooltipContainer.contains(e.target))) {
        return;
    }

    createQuickButton();

    setTimeout(() => {
        const selection = window.getSelection();
        const text = selection.toString().trim();

        if (text.length > 0) {
            quickBtn.style.top = `${e.pageY + 10}px`;
            quickBtn.style.left = `${e.pageX + 10}px`;
            quickBtn.style.display = "flex";
        } else {
            quickBtn.style.display = "none";
        }
    }, 10);
});

document.addEventListener("mousedown", (e) => {
    if (quickBtn && e.target !== quickBtn && (!tooltipContainer || !tooltipContainer.contains(e.target))) {
        quickBtn.style.display = "none";
    }
});
