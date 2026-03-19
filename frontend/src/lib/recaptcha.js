const RECAPTCHA_SITE_KEY = import.meta.env.VITE_RECAPTCHA_SITE_KEY || "";
const RECAPTCHA_SCRIPT_ID = "google-recaptcha-v3";

function hasRecaptcha() {
  return typeof window !== "undefined"
    && typeof window.grecaptcha !== "undefined"
    && typeof window.grecaptcha.ready === "function"
    && typeof window.grecaptcha.execute === "function";
}

function injectScript() {
  return new Promise((resolve, reject) => {
    const existingScript = document.getElementById(RECAPTCHA_SCRIPT_ID);
    if (existingScript) {
      if (hasRecaptcha()) {
        resolve();
        return;
      }

      existingScript.addEventListener("load", resolve, { once: true });
      existingScript.addEventListener("error", () => reject(new Error("reCAPTCHA script failed to load.")), { once: true });
      return;
    }

    const script = document.createElement("script");
    script.id = RECAPTCHA_SCRIPT_ID;
    script.src = `https://www.google.com/recaptcha/api.js?render=${encodeURIComponent(RECAPTCHA_SITE_KEY)}`;
    script.async = true;
    script.defer = true;
    script.addEventListener("load", resolve, { once: true });
    script.addEventListener("error", () => reject(new Error("reCAPTCHA script failed to load.")), { once: true });
    document.head.appendChild(script);
  });
}

function waitForRecaptcha(timeoutMs = 10000) {
  return new Promise((resolve, reject) => {
    if (!RECAPTCHA_SITE_KEY) {
      resolve(null);
      return;
    }

    injectScript().catch(reject);

    const startedAt = Date.now();
    const poll = () => {
      if (hasRecaptcha()) {
        resolve(window.grecaptcha);
        return;
      }

      if (Date.now() - startedAt > timeoutMs) {
        reject(new Error("reCAPTCHA did not load in time."));
        return;
      }

      window.setTimeout(poll, 100);
    };

    poll();
  });
}

export function isRecaptchaEnabled() {
  return Boolean(RECAPTCHA_SITE_KEY);
}

export function preloadRecaptcha() {
  if (!RECAPTCHA_SITE_KEY) {
    return;
  }

  injectScript().catch(() => {});
}

export async function executeRecaptcha(action) {
  if (!RECAPTCHA_SITE_KEY) {
    return "";
  }

  const grecaptcha = await waitForRecaptcha();
  if (!grecaptcha) {
    return "";
  }

  return new Promise((resolve, reject) => {
    grecaptcha.ready(() => {
      if (typeof grecaptcha.execute !== "function") {
        reject(new Error("reCAPTCHA is not ready yet."));
        return;
      }

      grecaptcha.execute(RECAPTCHA_SITE_KEY, { action })
        .then((token) => resolve(token || ""))
        .catch((error) => reject(error));
    });
  });
}
