let hydra = null;

function resumeAudio() {
    try {
        if (hydra && hydra.synth) {
            const a = hydra.synth.a;
            if (a) {
                if (a.context && a.context.state === 'suspended') {
                    a.context.resume().then(() => {
                        console.log("Microphone AudioContext resumed successfully, state:", a.context.state);
                    }).catch(e => {
                        console.error("Error resuming Microphone AudioContext:", e);
                    });
                }
                if (!a.stream && window.navigator && window.navigator.mediaDevices) {
                    console.log("Mic stream missing, re-initializing microphone audio...");
                    if (typeof hydra.synth._initAudio === 'function') {
                        hydra.synth._initAudio();
                    }
                }
            } else if (typeof hydra.synth._initAudio === 'function') {
                hydra.synth._initAudio();
            }
        }
    } catch (e) {
        console.error("Audio resume error:", e);
    }
}

function initHydra() {
    if (!hydra) {
        try {
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
            const handleResume = () => {
                resumeAudio();
            };
            resumeEvents.forEach(evt => window.addEventListener(evt, handleResume, { passive: true }));
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
