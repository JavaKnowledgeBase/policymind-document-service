from __future__ import annotations

import re
import textwrap
from pathlib import Path

from reportlab.lib.pagesizes import letter
from reportlab.pdfgen import canvas


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "INTERVIEW_LINE_BY_LINE_GUIDE.pdf"


def explain_line(path: Path, line: str) -> str:
    s = line.strip()
    ext = path.suffix.lower()

    if not s:
        return "Blank line for readability and structure."

    if s.startswith("//") or s.startswith("/*") or s.startswith("*") or s.startswith("*/"):
        return "Comment line that documents intent or behavior."

    if s.startswith("package "):
        return "Declares the Java package namespace for this class."

    if s.startswith("import "):
        return "Imports a dependency/type used later in the file."

    if s.startswith("@"):
        return "Annotation that configures framework behavior at runtime."

    if s in {"{", "}", "};"}:
        return "Block delimiter used to scope code."

    if s.startswith("class ") or " class " in s:
        return "Declares a class type and its name."

    if s.startswith("interface ") or " interface " in s:
        return "Declares an interface contract."

    if re.match(r"^(public|private|protected)\s+.*\)\s*\{?$", s):
        return "Method or constructor signature defining callable behavior."

    if s.startswith("if ") or s.startswith("if("):
        return "Conditional branch that runs code only when condition is true."

    if s.startswith("else"):
        return "Alternative branch when previous condition is false."

    if s.startswith("for ") or s.startswith("while "):
        return "Loop statement that repeats code while condition/range applies."

    if s.startswith("try"):
        return "Starts exception handling block."

    if s.startswith("catch"):
        return "Handles an exception thrown in the try block."

    if s.startswith("return "):
        return "Returns a value to the caller."

    if s == "return;" or s.startswith("return;"):
        return "Ends the method early without returning a value."

    if "new " in s and "=" in s:
        return "Creates a new object instance and assigns it."

    if "=" in s and not s.startswith("=="):
        return "Assignment/configuration of a value used by later logic."

    if "->" in s:
        return "Lambda expression defining inline function behavior."

    if ext in {".jsx", ".js"}:
        if s.startswith("import "):
            return "Imports module/component dependency used in this file."
        if s.startswith("export default"):
            return "Exports this component/module as the default export."
        if s.startswith("function ") or s.startswith("const ") and "=>" in s:
            return "Defines a React component or helper function."
        if s.startswith("use") and "(" in s:
            return "React hook call for component state/effects."
        if s.startswith("<") and s.endswith(">"):
            return "JSX markup used to render UI elements."
        return "JavaScript/React logic used by the component."

    if ext == ".css":
        if s.endswith("{"):
            return "Starts a CSS selector block."
        if ":" in s and s.endswith(";"):
            return "CSS property declaration controlling style."
        if s == "}":
            return "Ends a CSS selector block."
        return "CSS rule content for layout or visual styling."

    if ext in {".yml", ".yaml"}:
        if s.endswith(":"):
            return "YAML key section header."
        if ":" in s:
            return "YAML key/value configuration entry."
        return "YAML structural content."

    if ext == ".json":
        if s.startswith("{") or s.startswith("}"):
            return "JSON object boundary."
        return "JSON configuration field/value."

    if path.name.lower() == "dockerfile":
        if s.upper().startswith("FROM "):
            return "Selects the base image for this build stage."
        if s.upper().startswith("WORKDIR "):
            return "Sets working directory for subsequent Docker steps."
        if s.upper().startswith("COPY "):
            return "Copies files into the container image."
        if s.upper().startswith("RUN "):
            return "Executes build-time command while creating image."
        if s.upper().startswith("EXPOSE "):
            return "Documents container port intended for runtime traffic."
        if s.upper().startswith("ENTRYPOINT "):
            return "Defines startup command for the container."
        return "Docker build/runtime instruction."

    if path.name == "docker-compose.yml":
        return "Compose configuration line defining service/network/volume behavior."

    return "Code statement participating in the current method/module flow."


def collect_files() -> list[Path]:
    patterns = [
        "src/main/java/**/*.java",
        "src/main/resources/application.yml",
        "frontend/src/**/*.js",
        "frontend/src/**/*.jsx",
        "frontend/src/**/*.css",
        "frontend/index.html",
        "frontend/vite.config.js",
        "frontend/package.json",
        "Dockerfile",
        "docker-compose.yml",
        "frontend/Dockerfile",
        "frontend/nginx.conf",
    ]
    files: set[Path] = set()
    for pattern in patterns:
        files.update(ROOT.glob(pattern))
    return sorted(p for p in files if p.is_file())


def write_pdf(files: list[Path]) -> None:
    c = canvas.Canvas(str(OUTPUT), pagesize=letter)
    width, height = letter

    left = 40
    top = height - 40
    line_h = 12
    y = top

    def new_page() -> None:
        nonlocal y
        c.showPage()
        c.setFont("Helvetica", 9)
        y = top

    c.setFont("Helvetica-Bold", 14)
    c.drawString(left, y, "PolicyMind Document Service - Line-by-Line Interview Guide")
    y -= 18
    c.setFont("Helvetica", 10)
    c.drawString(left, y, "Scope: Frontend + Backend source/config files in this repository.")
    y -= 20

    c.setFont("Helvetica", 9)
    for file_path in files:
        rel = file_path.relative_to(ROOT).as_posix()
        if y < 70:
            new_page()
        c.setFont("Helvetica-Bold", 10)
        c.drawString(left, y, f"File: {rel}")
        y -= 14
        c.setFont("Helvetica", 9)

        lines = file_path.read_text(encoding="utf-8", errors="replace").splitlines()
        if not lines:
            if y < 50:
                new_page()
            c.drawString(left + 10, y, "File is empty.")
            y -= 12
            continue

        for idx, raw in enumerate(lines, start=1):
            code = raw.replace("\t", "    ")
            expl = explain_line(file_path, raw)

            code_prefix = f"L{idx:04d} | "
            wrapped_code = textwrap.wrap(code, width=88) or [""]
            wrapped_expl = textwrap.wrap(f"Why: {expl}", width=92)

            if y < (len(wrapped_code) + len(wrapped_expl) + 2) * line_h + 30:
                new_page()
                c.setFont("Helvetica-Bold", 10)
                c.drawString(left, y, f"File: {rel} (cont.)")
                y -= 14
                c.setFont("Helvetica", 9)

            for i, part in enumerate(wrapped_code):
                prefix = code_prefix if i == 0 else " " * len(code_prefix)
                c.drawString(left + 6, y, f"{prefix}{part}")
                y -= line_h

            for part in wrapped_expl:
                c.drawString(left + 26, y, part)
                y -= line_h

            y -= 3

        y -= 8

    c.save()


def main() -> None:
    files = collect_files()
    write_pdf(files)
    print(f"Generated: {OUTPUT}")
    print(f"Files included: {len(files)}")


if __name__ == "__main__":
    main()
