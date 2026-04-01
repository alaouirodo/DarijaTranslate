const API_BASE = "http://localhost:8081/DarijaTranslator/api";

chrome.runtime.onInstalled.addListener(() => {
    chrome.contextMenus.create({
        id: "translate-darija",
        title: "Translate to Darija",
        contexts: ["selection"]
    });
});

chrome.contextMenus.onClicked.addListener((info, tab) => {
    if (info.menuItemId === "translate-darija" && info.selectionText) {
        if (tab && tab.id) {
            chrome.tabs.sendMessage(tab.id, {
                type: "SHOW_TRANSLATION_PANEL",
                text: info.selectionText
            }).catch(() => {});
        } else {
            chrome.tabs.query({ active: true, currentWindow: true }, (tabs) => {
                if (tabs[0]) {
                    chrome.tabs.sendMessage(tabs[0].id, {
                        type: "SHOW_TRANSLATION_PANEL",
                        text: info.selectionText
                    }).catch(() => {});
                }
            });
        }
    }
});

chrome.runtime.onMessage.addListener((message, sender, sendResponse) => {
    if (message.type === "API_TRANSLATE") {
        chrome.storage.session.get(["jwtToken"], async (res) => {
            if (!res.jwtToken) { sendResponse({ error: "Not logged in" }); return; }
            try {
                const response = await fetch(`${API_BASE}/translate`, {
                    method: "POST",
                    headers: { "Content-Type": "application/json", "Authorization": `Bearer ${res.jwtToken}` },
                    body: JSON.stringify({ text: message.text })
                });
                const data = await response.json();
                if (!response.ok) throw new Error(data.error || "Failed");
                sendResponse({ data });
            } catch (err) {
                sendResponse({ error: err.message });
            }
        });
        return true;
    }

    if (message.type === "API_TTS") {
        chrome.storage.session.get(["jwtToken"], async (res) => {
            if (!res.jwtToken) { sendResponse({ error: "Not logged in" }); return; }
            try {
                const response = await fetch(`${API_BASE}/tts`, {
                    method: "POST",
                    headers: { "Content-Type": "application/json", "Authorization": `Bearer ${res.jwtToken}` },
                    body: JSON.stringify({ text: message.text })
                });
                const data = await response.json();
                if (!response.ok) throw new Error(data.error || "Failed");
                sendResponse({ data });
            } catch (err) {
                sendResponse({ error: err.message });
            }
        });
        return true;
    }
});
