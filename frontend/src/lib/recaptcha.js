const RECAPTCHA_SITE_KEY = import.meta.env.VITE_RECAPTCHA_SITE_KEY || "";
const RECAPTCHA_SCRIPT_ID = "google-recaptcha-v3";

function hasRecaptcha() {
  return typeof window !== "undefined" && typeof window.grecaptcha !== "undefined";
}

function injectScript() {
  if (document.getElementById(RECAPTCHA_SCRIPT_ID)) {
    return;
  }

  const script = document.createElement("script");
  script.id = RECAPTCHA_SCRIPT_ID;
  script.src = `https://www.google.com/recaptcha/api.js?render=${encodeURIComponent(RECAPTCHA_SITE_KEY)}`;
  script.async = true;
  script.defer = true;
  document.head.appendChild(script);
}

function waitForRecaptcha(timeoutMs = 10000) {
  return new Promise((resolve, reject) => {
    if (!RECAPTCHA_SITE_KEY) {
      resolve(null);
      return;
    }

    injectScript();

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

  injectScript();
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
    grecaptcha.ready(async () => {
      try {
        const token = await grecaptcha.execute(RECAPTCHA_SITE_KEY, { action });
        resolve(token || "");
      } catch (error) {
        reject(error);
      }
    });
  });
}
