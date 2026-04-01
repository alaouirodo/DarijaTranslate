const API_BASE = "http://localhost:8081/DarijaTranslator/api";

const loginSection = document.getElementById("loginSection");
const mainSection = document.getElementById("mainSection");
const loginBtn = document.getElementById("loginBtn");
const usernameInput = document.getElementById("username");
const passwordInput = document.getElementById("password");
const loginStatus = document.getElementById("loginStatus");
const inputText = document.getElementById("inputText");
const clearBtn = document.getElementById("clearBtn");
const micBtn = document.getElementById("micBtn");
const outputContainer = document.getElementById("outputContainer");
const speakBtn = document.getElementById("speakBtn");
const copyBtn = document.getElementById("copyBtn");
const statusBar = document.getElementById("statusBar");
const settingsBtn = document.getElementById("settingsBtn");

let jwtToken = null;
let debounceTimer = null;
let currentDarija = { arabic: "", latin: "" };
let isRecording = false;
let mediaRecorder = null;
let audioChunks = [];
let audioStream = null;
let currentTtsAudio = null;

document.addEventListener("DOMContentLoaded", async () => {
  const result = await chrome.storage.session.get(["jwtToken"]);
  if (result.jwtToken) {
    jwtToken = result.jwtToken;
    showMainUI();
  } else {
    loginSection.classList.remove("hidden");
  }
});

loginBtn.addEventListener("click", async () => {
  loginBtn.disabled = true;
  loginStatus.textContent = "Authenticating...";
  try {
    const res = await fetch(`${API_BASE}/auth/login`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username: usernameInput.value, password: passwordInput.value })
    });
    if (!res.ok) throw new Error("Login failed");
    const data = await res.json();
    jwtToken = data.token;
    await chrome.storage.session.set({ jwtToken });
    showMainUI();
  } catch (err) {
    loginStatus.textContent = err.message || "Connection error";
    loginStatus.classList.add("error");
  } finally {
    loginBtn.disabled = false;
  }
});

function showMainUI() {
  loginSection.classList.add("hidden");
  mainSection.classList.remove("hidden");
}

clearBtn.addEventListener("click", () => {
  inputText.value = "";
  outputContainer.innerHTML = '<div class="placeholder-text">Translation will appear here...</div>';
  outputContainer.className = "output-content";
  speakBtn.disabled = true;
  copyBtn.disabled = true;
  currentDarija = { arabic: "", latin: "" };
});

inputText.addEventListener("input", () => {
  clearTimeout(debounceTimer);
  const text = inputText.value.trim();
  if (!text) { clearBtn.click(); return; }
  statusBar.textContent = "Translating...";
  debounceTimer = setTimeout(() => doTranslate(text), 600);
});

async function doTranslate(text) {
  try {
    const res = await fetch(`${API_BASE}/translate`, {
      method: "POST",
      headers: { "Content-Type": "application/json", "Authorization": `Bearer ${jwtToken}` },
      body: JSON.stringify({ text })
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || "Translation failed");
    currentDarija = { arabic: data.translation, latin: data.latin || data.translation };
    renderTranslation();
    statusBar.textContent = "";
  } catch (err) {
    statusBar.textContent = "Translation error.";
    outputContainer.innerHTML = `<div class="error">Error: ${err.message}</div>`;
  }
}

function renderTranslation() {
  if (!currentDarija.arabic) return;
  outputContainer.textContent = currentDarija.arabic;
  outputContainer.className = "output-content arabic";
  speakBtn.disabled = false;
  copyBtn.disabled = false;
}

copyBtn.addEventListener("click", () => {
  navigator.clipboard.writeText(outputContainer.textContent);
  const orig = copyBtn.textContent;
  copyBtn.textContent = "Copied!";
  setTimeout(() => copyBtn.textContent = orig, 1500);
});

settingsBtn.addEventListener("click", () => {
  chrome.runtime.openOptionsPage();
});

micBtn.addEventListener("click", async () => {
  if (isRecording) { stopRecording(); return; }
  try {
    audioStream = await navigator.mediaDevices.getUserMedia({ audio: true });
    startRecording(audioStream);
  } catch (err) {
    if (err.name === "NotFoundError" || err.name === "DevicesNotFoundError") {
      statusBar.textContent = "No microphone found.";
    } else if (err.name === "NotAllowedError" || err.name === "PermissionDeniedError") {
      statusBar.innerHTML = "";
      const msg = document.createElement("span");
      msg.textContent = "Mic blocked. ";
      const link = document.createElement("a");
      link.textContent = "Fix it";
      link.href = "#";
      link.style.cssText = "color:#2563EB;text-decoration:underline;cursor:pointer";
      link.addEventListener("click", (e) => {
        e.preventDefault();
        chrome.tabs.create({ url: "chrome://settings/content/microphone" });
      });
      statusBar.appendChild(msg);
      statusBar.appendChild(link);
    } else {
      statusBar.textContent = "Mic error: " + err.message;
    }
    micBtn.classList.remove("recording");
    micBtn.textContent = "mic";
    isRecording = false;
  }
});

function startRecording(stream) {
  audioChunks = [];
  const mimeType = ["audio/webm;codecs=opus", "audio/webm", "audio/ogg;codecs=opus"]
    .find(t => MediaRecorder.isTypeSupported(t)) || "";
  mediaRecorder = new MediaRecorder(stream, mimeType ? { mimeType } : {});
  mediaRecorder.ondataavailable = e => { if (e.data.size > 0) audioChunks.push(e.data); };
  mediaRecorder.onstop = processAudio;
  mediaRecorder.start();
  isRecording = true;
  micBtn.classList.add("recording");
  micBtn.textContent = "stop";
  statusBar.textContent = "Listening... click to stop.";
}

function stopRecording() {
  if (mediaRecorder && mediaRecorder.state !== "inactive") mediaRecorder.stop();
  if (audioStream) { audioStream.getTracks().forEach(t => t.stop()); audioStream = null; }
  isRecording = false;
  micBtn.classList.remove("recording");
  micBtn.textContent = "mic";
  statusBar.textContent = "Processing speech...";
}

async function processAudio() {
  const blob = new Blob(audioChunks, { type: mediaRecorder.mimeType || "audio/webm" });
  const reader = new FileReader();
  reader.readAsDataURL(blob);
  reader.onloadend = async () => {
    const base64Audio = reader.result.split(',')[1];
    try {
      const res = await fetch(`${API_BASE}/transcribe`, {
        method: "POST",
        headers: { "Content-Type": "application/json", "Authorization": `Bearer ${jwtToken}` },
        body: JSON.stringify({ audio: base64Audio })
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error || "Transcription failed");
      inputText.value = data.transcript;
      doTranslate(data.transcript);
    } catch (err) {
      statusBar.textContent = "Transcription error: " + err.message;
    }
  };
}

speakBtn.addEventListener("click", async () => {
  if (currentTtsAudio && !currentTtsAudio.paused) {
    currentTtsAudio.pause();
    currentTtsAudio = null;
    speakBtn.classList.remove("speaking");
    speakBtn.innerHTML = '<span class="icon">Listen</span>';
    return;
  }
  const text = currentDarija.arabic || currentDarija.latin;
  if (!text) return;
  speakBtn.disabled = true;
  statusBar.textContent = "Loading audio...";
  try {
    const res = await fetch(`${API_BASE}/tts`, {
      method: "POST",
      headers: { "Content-Type": "application/json", "Authorization": `Bearer ${jwtToken}` },
      body: JSON.stringify({ text })
    });
    const data = await res.json();
    if (!res.ok) throw new Error(data.error || "TTS Failed");
    currentTtsAudio = new Audio(`data:${data.mimeType || 'audio/wav'};base64,${data.audioContent}`);
    currentTtsAudio.onplay = () => {
      speakBtn.classList.add("speaking");
      speakBtn.innerHTML = '<span class="icon">Stop</span>';
      speakBtn.disabled = false;
      statusBar.textContent = "Speaking...";
    };
    currentTtsAudio.onended = () => {
      speakBtn.classList.remove("speaking");
      speakBtn.innerHTML = '<span class="icon">Listen</span>';
      statusBar.textContent = "";
    };
    currentTtsAudio.play();
  } catch (err) {
    statusBar.textContent = "TTS Error: " + err.message;
    speakBtn.disabled = false;
  }
});
