import HeaderTypeTicker from "./HeaderTypeTicker";

export default function BrandBar() {
  return (
    <header className="brand-bar" aria-label="Torilaure Esystems brand">
      <div className="brand-logo" aria-hidden="true">
        <svg viewBox="0 0 520 140" role="img">
          <defs>
            <linearGradient id="teBlue" x1="0%" y1="0%" x2="100%" y2="0%">
              <stop offset="0%" stopColor="#1e90d6" />
              <stop offset="100%" stopColor="#1f9fe9" />
            </linearGradient>
          </defs>
          <g transform="translate(6,16)">
            <path d="M36 92c26-34 60-54 98-58" stroke="#bfc0c2" strokeWidth="10" fill="none" strokeLinecap="round" />
            <path d="M28 72c12-10 24-12 36 2" stroke="#6f7072" strokeWidth="12" fill="none" strokeLinecap="round" />
            <path d="M68 40c-9-8-15-16-19-25" stroke="#b6b7b9" strokeWidth="11" fill="none" strokeLinecap="round" />
            <path d="M88 44c-11-5-18-13-24-23" stroke="#8e8f91" strokeWidth="11" fill="none" strokeLinecap="round" />
            <path d="M66 104c8-24 26-38 52-36" stroke="url(#teBlue)" strokeWidth="10" fill="none" strokeLinecap="round" />
            <path d="M106 74c11 1 21 8 27 20" stroke="#2389c7" strokeWidth="12" fill="none" strokeLinecap="round" />
            <circle cx="20" cy="62" r="8" fill="#b6b7b9" />
            <circle cx="86" cy="26" r="6" fill="#b6b7b9" />
            <circle cx="116" cy="66" r="7" fill="#1fa1ef" />
            <circle cx="56" cy="104" r="8" fill="#1fa1ef" />
          </g>
          <text x="170" y="68" fontSize="68" fill="#2498e0" fontWeight="700" fontFamily="Space Grotesk, Segoe UI, sans-serif">
            Torilaure
          </text>
          <text x="170" y="120" fontSize="62" fill="#42444a" fontWeight="600" fontFamily="Space Grotesk, Segoe UI, sans-serif">
            E-systems
          </text>
        </svg>
      </div>
      <HeaderTypeTicker />
    </header>
  );
}
