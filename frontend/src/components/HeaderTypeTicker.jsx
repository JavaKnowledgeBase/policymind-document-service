import { useEffect, useMemo, useState } from "react";

const TOTAL_WINDOW_MS = 20000;
const TYPING_WINDOW_MS = 14000;
const FALLBACK_ITEMS = [
  "PolicyMind AI converts complex contracts and policies into practical risk insights."
];

function nextIndex(current, length) {
  if (length <= 1) {
    return current;
  }
  return (current + 1) % length;
}

export default function HeaderTypeTicker() {
  const [items, setItems] = useState(FALLBACK_ITEMS);
  const [activeIndex, setActiveIndex] = useState(0);
  const [typedText, setTypedText] = useState("");
  const activeText = useMemo(() => items[activeIndex] || "", [items, activeIndex]);

  useEffect(() => {
    let ignore = false;
    const loadItems = async () => {
      try {
        const apiBase = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";
        const response = await fetch(`${apiBase}/content/header-lines`);
        if (!response.ok) {
          return;
        }
        const payload = await response.json();
        const lines = Array.isArray(payload?.items) ? payload.items.filter(Boolean) : [];
        if (!ignore && lines.length) {
          setItems(lines);
          setActiveIndex(0);
        }
      } catch (error) {
        // Keep fallback content when API is unavailable.
      }
    };

    loadItems();
    return () => {
      ignore = true;
    };
  }, []);

  useEffect(() => {
    if (!activeText) {
      return undefined;
    }

    let charIndex = 0;
    setTypedText("");

    const typingIntervalMs = Math.max(30, Math.floor(TYPING_WINDOW_MS / activeText.length));
    const typeInterval = window.setInterval(() => {
      charIndex += 1;
      setTypedText(activeText.slice(0, charIndex));
      if (charIndex >= activeText.length) {
        window.clearInterval(typeInterval);
      }
    }, typingIntervalMs);

    const holdDelayMs = Math.max(1000, TOTAL_WINDOW_MS - typingIntervalMs * activeText.length);
    const advanceTimer = window.setTimeout(() => {
      setActiveIndex((current) => nextIndex(current, items.length));
    }, typingIntervalMs * activeText.length + holdDelayMs);

    return () => {
      window.clearInterval(typeInterval);
      window.clearTimeout(advanceTimer);
    };
  }, [activeText, items.length]);

  return (
    <section className="header-ticker" aria-live="polite">
      <p>{typedText}</p>
    </section>
  );
}
