"use strict";

/**
 * Browser client for the speech-to-text API.
 *
 * Every visible change goes through setState(), so the button, the status line
 * and the ARIA attributes can never disagree. Every failure maps to a specific
 * sentence the user can act on. Audio is captured mono at 16 kHz as Opus and
 * recorded in timeslices, which is where the network saving comes from.
 */

const TRANSCRIBE_URL = "/api/v1/transcribe";
const STATS_URL = "/api/v1/global/stats";

const SAMPLE_RATE = 16000;        // speech carries nothing above ~8 kHz
const BITS_PER_SECOND = 16000;
const TIMESLICE_MS = 3000;        // chunked as it records, not buffered whole
const TIMEOUT_MS = 60000;
const MIN_BYTES = 1024;

const $ = (id) => document.getElementById(id);
const recordButton = $("record"), recordLabel = $("record-label");
const statusLine = $("status"), errorBox = $("error");
const transcript = $("transcript"), savings = $("savings"), stats = $("stats");

/** Each failure gets its own sentence: "something went wrong" is not a good practice. */
const MIC_ERRORS = {
    NotAllowedError: "Microphone access was blocked. Allow it for this site in your browser's address bar, then try again.",
    SecurityError: "Microphone access was blocked. Allow it for this site in your browser's address bar, then try again.",
    NotFoundError: "No microphone was found. Connect one and try again.",
    NotReadableError: "The microphone is already in use by another application.",
    OverconstrainedError: "Your microphone does not support the required audio settings.",
};

/** Used only when the server sends no ErrorResponse message of its own. */
const HTTP_ERRORS = {
    400: "The recording was rejected as malformed.",
    413: "That recording is too large to upload. Record a shorter message.",
    415: "The server did not accept this audio format.",
    502: "The transcription service is unavailable. Please try again shortly.",
    503: "The transcription service is unavailable. Please try again shortly.",
};

let recorder = null, stream = null, chunks = [], startedAt = 0, isRecording = false;

/** The single place the interface changes, so its parts cannot drift apart. */
function setState(state, message) {
    isRecording = state === "recording";
    recordButton.setAttribute("aria-pressed", String(isRecording));
    recordButton.disabled = state === "busy";
    recordLabel.textContent = isRecording ? "Stop recording" : "Start recording";
    transcript.setAttribute("aria-busy", String(state === "busy"));
    statusLine.textContent = message;
}

function fail(message) {
    setState("idle", "Ready");
    errorBox.textContent = message;
    errorBox.hidden = false;
}

async function startRecording() {
    errorBox.hidden = true;
    setState("busy", "Waiting for microphone permission…");

    try {
        // pause here until the user answers
        stream = await navigator.mediaDevices.getUserMedia({
            audio: {
                channelCount: 1, sampleRate: SAMPLE_RATE,
                echoCancellation: true, noiseSuppression: true, autoGainControl: true,
            },
        });
    } catch (error) {
        fail(MIC_ERRORS[error.name] ?? "The microphone could not be started. Please try again.");
        return;
    }

    // Opus is the smallest format for speech; Safari falls back to mp4.
    const mimeType = ["audio/webm;codecs=opus", "audio/ogg;codecs=opus", "audio/mp4"]
        .find((type) => MediaRecorder.isTypeSupported(type));

    recorder = new MediaRecorder(stream, { mimeType, audioBitsPerSecond: BITS_PER_SECOND });
    chunks = [];
    recorder.addEventListener("dataavailable", (e) => e.data.size && chunks.push(e.data));
    recorder.addEventListener("stop", finishRecording);
    recorder.start(TIMESLICE_MS);

    startedAt = Date.now();
    transcript.textContent = "";
    savings.hidden = true;
    setState("recording", "Recording — speak now.");
}

function stopRecording() {
    setState("busy", "Transcribing…");
    recorder.stop();        // fires "stop", which calls finishRecording
}

async function finishRecording() {
    stream.getTracks().forEach((track) => track.stop());    // releases the mic light

    const blob = new Blob(chunks, { type: recorder.mimeType || "audio/webm" });
    chunks = [];

    if (blob.size < MIN_BYTES) {
        fail("That recording was too short. Try again and speak for at least a second.");
    } else {
        await upload(blob);
    }
}

async function upload(blob) {
    if (navigator.onLine === false) {
        fail("You appear to be offline. Reconnect and try again.");
        return;
    }

    const extension = blob.type.includes("ogg") ? "ogg" : blob.type.includes("mp4") ? "m4a" : "webm";
    const form = new FormData();
    form.append("file", blob, `recording.${extension}`);

    try {
        const response = await fetch(TRANSCRIBE_URL, {
            method: "POST", body: form, signal: AbortSignal.timeout(TIMEOUT_MS),
        });

        if (!response.ok) {
            // The server's own ErrorResponse message is preferred where there is one.
            const { message } = await response.json().catch(() => ({}));
            throw new Error(message || HTTP_ERRORS[response.status]
                || `The server responded with an error (${response.status}).`);
        }

        const { text } = await response.json();
        showTranscript(text, blob.size);
    } catch (error) {
        // TimeoutError and TypeError come from fetch itself; anything else is
        // the message thrown just above, already written for the user.
        fail(error.name === "TimeoutError" ? "The server took too long to answer. Please try again."
            : error.name === "TypeError" ? "Could not reach the server. Check your connection and try again."
                : error.message);
    }
}

function showTranscript(text, bytes) {
    transcript.textContent = text?.trim() || "(no speech detected)";
    transcript.focus();     // puts a screen reader straight onto the new text
    setState("idle", "Ready");

    // Concrete evidence of the compression rather than a claim: uncompressed
    // 16-bit mono PCM at 16 kHz costs 32000 bytes per second.
    const seconds = Math.max((Date.now() - startedAt) / 1000, 0.1);
    savings.textContent = `Uploaded ${(bytes / 1024).toFixed(1)} KB for ${seconds.toFixed(1)}s of audio, `
        + `about ${((seconds * SAMPLE_RATE * 2) / bytes).toFixed(0)}x smaller than uncompressed audio.`;
    savings.hidden = false;

    refreshStats();
}

async function refreshStats() {
    try {
        const { inputTokens, outputTokens } = await (await fetch(STATS_URL)).json();
        stats.textContent = `${inputTokens.toLocaleString()} input and `
            + `${outputTokens.toLocaleString()} output tokens used since the server started.`;
    } catch {
        stats.textContent = "Server statistics are unavailable.";
    }
}

// Recording is impossible on insecure origins and in older browsers. Say which,
// and hide the button rather than offering a control that cannot work.
const blocked =
    !window.isSecureContext
        ? "Microphone access needs a secure connection. Open this page over HTTPS, or on localhost."
        : !navigator.mediaDevices?.getUserMedia || typeof MediaRecorder === "undefined"
            ? "This browser cannot capture audio. Try a current version of Chrome, Edge, Firefox or Safari."
            : null;

if (blocked) {
    $("unsupported").textContent = blocked;
    $("unsupported").hidden = false;
    recordButton.hidden = true;
} else {
    recordButton.addEventListener("click", () => isRecording ? stopRecording() : startRecording());
}

refreshStats();