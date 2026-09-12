let hydra = null;

// ---------------------------------------------------------------------------
// Microphone lifecycle
//
// The Hydra constructor creates ONE Audio object (hydra.synth.a) and the eval
// sandbox binds the global `a` to it exactly once, at construction. Recovery
// must therefore never swap in a new Audio object (the previous approach did,
// leaving every patch reading the orphaned original, whose fft stays frozen at
// zeros while the render loop ticks the new object). Instead we attach the mic
// stream to the SAME object using the globally exposed Meyda
// (window.Meyda.createMeydaAnalyzer).
// ---------------------------------------------------------------------------

let micInitInFlight = false;
let lastMicAttempt = 0;
const MIC_RETRY_MS = 3000;

// Collapse concurrent getUserMedia calls with identical constraints into a
// single request, so Hydra's constructor call and our recovery call can never
// race each other into duplicate streams and orphaned analyzers.
function gateGetUserMedia() {
    const md = window.navigator && window.navigator.mediaDevices;
    if (!md || !md.getUserMedia || md.__gumGated) return;
    const native = md.getUserMedia.bind(md);
    const inflight = {};
    md.getUserMedia = function (constraints) {
        const key = (constraints && constraints.video ? "v" : "-") +
                    (constraints && constraints.audio ? "a" : "-");
        if (!inflight[key]) {
            inflight[key] = native(constraints).finally(() => { delete inflight[key]; });
        }
        return inflight[key];
    };
    md.__gumGated = true;
}

function ensureMic(a, force) {
    if (a.stream || micInitInFlight) return;
    if (!window.navigator || !window.navigator.mediaDevices || !window.Meyda) return;
    if (!force && Date.now() - lastMicAttempt < MIC_RETRY_MS) return;

    lastMicAttempt = Date.now();
    micInitInFlight = true;
    console.log("Requesting microphone stream...");

    window.navigator.mediaDevices.getUserMedia({ audio: true, video: false })
        .then(stream => {
            if (a.meyda) {
                // Hydra's constructor attached an analyzer to this same shared
                // stream while our request was in flight — nothing to do.
                console.log("Mic already attached by Hydra constructor");
                return;
            }
            a.stream = stream;
            if (!a.context) a.context = new window.AudioContext();
            if (a.context.state === "suspended") {
                a.context.resume().catch(e => console.error("Error resuming AudioContext:", e));
            }
            const source = a.context.createMediaStreamSource(stream);
            a.meyda = window.Meyda.createMeydaAnalyzer({
                audioContext: a.context,
                source: source,
                featureExtractors: ["loudness"]
            });
            if (typeof a.meyda.start === "function") a.meyda.start();
            console.log("Microphone ready, AudioContext state:", a.context.state);
        })
        .catch(err => {
            const reason = err && err.name ? err.name : String(err);
            // NotAllowedError only means the app-level permission is not
            // granted yet (dialog pending or refused); the native side
            // already messages that case. Surface hardware/WebView failures.
            if (reason !== "NotAllowedError" && window.AndroidBridge) {
                window.AndroidBridge.onMicError(reason);
            }
            console.error("Mic unavailable:", reason, err);
        })
        .finally(() => { micInitInFlight = false; });
}

// Always fetch audio through synth.a and keep the global `a` pointing at it —
// patches and the a0..aN helper closures read the global.
function liveAudio() {
    if (!hydra || !hydra.synth) return null;
    const a = hydra.synth.a;
    if (a) window.a = a;
    return a || null;
}

function resumeAudio(force) {
    try {
        const a = liveAudio();
        if (!a) return;
        if (a.context && a.context.state === "suspended") {
            a.context.resume()
                .then(() => console.log("AudioContext resumed, state:", a.context.state))
                .catch(e => console.error("Error resuming AudioContext:", e));
        }
        if (a.meyda && typeof a.meyda.start === "function") {
            a.meyda.start();
        }
        if (!a.stream) ensureMic(a, force);
    } catch (e) {
        console.error("Audio resume error:", e);
    }
}

// User gestures only unlock a suspended AudioContext; they never re-request
// the mic (re-initializing on every touch leaked AudioContexts and analyzers).
function unlockAudioContext() {
    try {
        const a = liveAudio();
        if (!a) return;
        if (a.context && a.context.state === "suspended") {
            a.context.resume().catch(() => {});
        }
        if (a.meyda && typeof a.meyda.start === "function") {
            a.meyda.start();
        }
    } catch (e) {
        // ignore — gesture unlock is best effort
    }
}

function initHydra() {
    if (!hydra) {
        try {
            gateGetUserMedia();
            hydra = new Hydra({
                canvas: document.getElementById("myCanvas"),
                detectAudio: true,
                enableHMR: false
            });
            hydra.setResolution(window.innerWidth, window.innerHeight);
            window.addEventListener('resize', () => {
                if (hydra) {
                    hydra.setResolution(window.innerWidth, window.innerHeight);
                }
            });

            // Handle user gestures to unlock WebAudio AudioContext if browser suspended it
            const resumeEvents = ['click', 'touchstart', 'keydown', 'pointerdown'];
            resumeEvents.forEach(evt => window.addEventListener(evt, unlockAudioContext, { passive: true }));
        } catch (e) {
            console.error("Hydra initialization error:", e);
        }
    }
    resumeAudio();
}

function stopHydra() {
    try {
        if (typeof hush === 'function') hush();
        if (typeof solid === 'function') solid(0, 0, 0).out();
        if (typeof a !== 'undefined' && a.hide) a.hide();
        if (typeof s0 !== 'undefined' && s0.clear) s0.clear();
        if (typeof s1 !== 'undefined' && s1.clear) s1.clear();
        if (typeof s2 !== 'undefined' && s2.clear) s2.clear();
        if (typeof s3 !== 'undefined' && s3.clear) s3.clear();
    } catch (e) {
        console.error("Error stopping hydra", e);
    }
}

function runCode(code) {
    initHydra();
    resumeAudio();

    try {
        // Clear camera/sources before running new patch if code doesn't use s0
        if (code.indexOf('s0') === -1 && typeof s0 !== 'undefined' && s0.clear) {
            s0.clear();
        }

        // Hide audio fft overlay if code doesn't explicitly call a.show()
        if (code.indexOf('a.show') === -1 && typeof a !== 'undefined' && a.hide) {
            a.hide();
        }

        // Evaluate the patch script
        const func = new Function(code);
        func();

        if (window.AndroidBridge) {
            window.AndroidBridge.onSuccess("Script running");
        }
    } catch (e) {
        console.error("Hydra evaluation error:", e);
        if (window.AndroidBridge) {
            window.AndroidBridge.onError(e.message || e.toString());
        }
    }
}

// Global initialization on load
window.addEventListener('DOMContentLoaded', () => {
    initHydra();
    // Start initial default animation
    try {
        osc(10, 0.1, 0.8).rotate(0.2, 0.1).color(1.2, 0.5, 2.0).out();
    } catch (e) {
        console.error("Default patch error:", e);
    }
});
