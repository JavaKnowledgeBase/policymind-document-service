const SITE_KEY = import.meta.env.VITE_RECAPTCHA_SITE_KEY || "";

export function isRecaptchaEnabled() {
  return Boolean(SITE_KEY);
}

export function preloadRecaptcha() {
  if (!SITE_KEY) return;
  if (document.getElementById("recaptcha-script")) return;
  const script = document.createElement("script");
  script.id = "recaptcha-script";
  script.src = `https://www.google.com/recaptcha/api.js?render=${SITE_KEY}`;
  script.async = true;
  document.head.appendChild(script);
}

export async function executeRecaptcha(action) {
  if (!SITE_KEY) return "";
  return new Promise((resolve, reject) => {
    const attempt = () => {
      if (typeof window.grecaptcha === "undefined" || typeof window.grecaptcha.execute === "undefined") {
        setTimeout(attempt, 200);
        return;
      }
      window.grecaptcha.ready(() => {
        window.grecaptcha
          .execute(SITE_KEY, { action })
          .then(resolve)
          .catch(reject);
      });
    };
    attempt();
  });
}
