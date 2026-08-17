#!/usr/bin/env python3
"""Converte a ficha versionada da Play no formato aceito pelo Gradle Play Publisher.

Os textos humanos continuam em ``docs/loja/ficha``. Este script só materializa a
árvore efêmera ``app/src/main/play`` imediatamente antes da publicação, para não
existirem duas fontes editáveis da mesma ficha.
"""

from __future__ import annotations

import argparse
import re
import shutil
import sys
from dataclasses import dataclass
from pathlib import Path


ROOT = Path(__file__).resolve().parent.parent
LISTING_SOURCE = ROOT / "docs/loja/ficha"
GRAPHICS_SOURCE = ROOT / "docs/loja/graficos"
METADATA_TARGET = ROOT / "app/src/main/play"
FDROID_TARGET = ROOT / "fastlane/metadata/android"
EXPECTED_LISTINGS = 74
MAX_LENGTHS = {
    "title": 30,
    "short-description": 80,
    "full-description": 4000,
    "release-note": 500,
}

HEADING = re.compile(r"^## .*? — `(?P<locale>[^`]+)`.*$", re.MULTILINE)
FIELD = {
    "title": re.compile(r"\*\*Nome do app\*\*\s*\n\n```\n(?P<value>.*?)\n```", re.DOTALL),
    "short-description": re.compile(
        r"\*\*Descrição curta\*\*\s*\n\n```\n(?P<value>.*?)\n```", re.DOTALL
    ),
    "full-description": re.compile(
        r"\*\*Descrição completa\*\*\s*\n\n```\n(?P<value>.*?)\n```", re.DOTALL
    ),
    "release-note": re.compile(r"\*\*Novidades\*\*\s*\n\n```\n(?P<value>.*?)\n```", re.DOTALL),
}


@dataclass(frozen=True)
class Listing:
    locale: str
    title: str
    short_description: str
    full_description: str
    release_note: str


def parse_listings(source_dir: Path) -> list[Listing]:
    """Lê todos os blocos Markdown, preservando quebras de linha da descrição."""
    listings: list[Listing] = []
    for source in sorted(source_dir.glob("bloco-*.md")):
        text = source.read_text(encoding="utf-8")
        headings = list(HEADING.finditer(text))
        if not headings:
            raise ValueError(f"nenhuma ficha encontrada em {source.relative_to(ROOT)}")
        for index, heading in enumerate(headings):
            end = headings[index + 1].start() if index + 1 < len(headings) else len(text)
            block = text[heading.start() : end]
            values: dict[str, str] = {}
            for name, pattern in FIELD.items():
                match = pattern.search(block)
                if match is None:
                    locale = heading.group("locale")
                    raise ValueError(f"campo {name} ausente em {locale} ({source.name})")
                values[name] = match.group("value").strip()
            listings.append(
                Listing(
                    locale=heading.group("locale"),
                    title=values["title"],
                    short_description=values["short-description"],
                    full_description=values["full-description"],
                    release_note=values["release-note"],
                )
            )
    validate_listings(listings)
    return listings


def validate_listings(listings: list[Listing]) -> None:
    locales = [listing.locale for listing in listings]
    if len(listings) != EXPECTED_LISTINGS:
        raise ValueError(f"esperadas {EXPECTED_LISTINGS} fichas, encontradas {len(listings)}")
    if len(locales) != len(set(locales)):
        raise ValueError("há locale repetido na ficha da Play")
    for listing in listings:
        fields = {
            "title": listing.title,
            "short-description": listing.short_description,
            "full-description": listing.full_description,
            "release-note": listing.release_note,
        }
        for name, value in fields.items():
            if not value:
                raise ValueError(f"{name} vazio em {listing.locale}")
            if len(value) > MAX_LENGTHS[name]:
                raise ValueError(
                    f"{name} em {listing.locale} tem {len(value)} caracteres; "
                    f"limite {MAX_LENGTHS[name]}"
                )


def require_graphics(graphics_source: Path) -> None:
    required = [graphics_source / "icone-512.png"]
    required.extend(
        graphics_source / f"feature-graphic-{locale}.png" for locale in ("pt-BR", "en-US", "es-419")
    )
    screenshot_names = (
        "1-inicio.png",
        "2-historico.png",
        "3-protecao.png",
        "4-politicas.png",
        "5-permitidos.png",
    )
    for source_locale in ("pt-BR", "en", "es"):
        for device in ("telefone", "tablet7", "tablet10"):
            required.extend(
                graphics_source / "screenshots" / device / source_locale / filename
                for filename in screenshot_names
            )
    missing = [path.relative_to(ROOT) for path in required if not path.is_file()]
    if missing:
        raise ValueError("gráfico ausente: " + ", ".join(map(str, missing)))


def write_text(path: Path, value: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(value.rstrip() + "\n", encoding="utf-8")


def copy_graphics(target: Path, graphics_source: Path) -> None:
    locale_sources = {"pt-BR": "pt-BR", "en-US": "en", "es-419": "es"}
    for locale, source_locale in locale_sources.items():
        destination = target / "listings" / locale / "graphics"

        def copy(source: Path, category: str) -> None:
            category_dir = destination / category
            category_dir.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, category_dir / source.name)

        copy(graphics_source / "icone-512.png", "icon")
        copy(graphics_source / f"feature-graphic-{locale}.png", "feature-graphic")
        for source_device, category in (
            ("telefone", "phone-screenshots"),
            ("tablet7", "tablet-screenshots"),
            ("tablet10", "large-tablet-screenshots"),
        ):
            source_dir = graphics_source / "screenshots" / source_device / source_locale
            for source in sorted(source_dir.glob("*.png")):
                copy(source, category)


def sync(listings: list[Listing], target: Path, graphics_source: Path) -> None:
    # O diretório é inteiramente gerado e ignorado pelo Git; nunca misturar com fonte manual.
    if target.exists():
        shutil.rmtree(target)
    write_text(target / "default-language.txt", "pt-BR")
    for listing in listings:
        base = target / "listings" / listing.locale
        write_text(base / "title.txt", listing.title)
        write_text(base / "short-description.txt", listing.short_description)
        write_text(base / "full-description.txt", listing.full_description)
        write_text(target / "release-notes" / listing.locale / "default.txt", listing.release_note)
    copy_graphics(target, graphics_source)


def copy_graphics_fdroid(target: Path, graphics_source: Path) -> None:
    locale_sources = {"pt-BR": "pt-BR", "en-US": "en", "es-419": "es"}
    for locale, source_locale in locale_sources.items():
        destination = target / locale / "images"

        def copy(source: Path, category: str, new_name: str | None = None) -> None:
            category_dir = destination if category == "" else destination / category
            category_dir.mkdir(parents=True, exist_ok=True)
            shutil.copy2(source, category_dir / (new_name or source.name))

        copy(graphics_source / "icone-512.png", "", "icon.png")
        copy(graphics_source / f"feature-graphic-{locale}.png", "", "featureGraphic.png")
        for source_device, category in (
            ("telefone", "phoneScreenshots"),
            ("tablet7", "sevenInchScreenshots"),
            ("tablet10", "tenInchScreenshots"),
        ):
            source_dir = graphics_source / "screenshots" / source_device / source_locale
            for source in sorted(source_dir.glob("*.png")):
                copy(source, category)


def sync_fdroid(listings: list[Listing], target: Path, graphics_source: Path) -> None:
    if target.exists():
        shutil.rmtree(target)
    for listing in listings:
        base = target / listing.locale
        write_text(base / "title.txt", listing.title)
        write_text(base / "short_description.txt", listing.short_description)
        write_text(base / "full_description.txt", listing.full_description)
    copy_graphics_fdroid(target, graphics_source)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="valida fontes sem escrever metadados")
    args = parser.parse_args()
    try:
        listings = parse_listings(LISTING_SOURCE)
        require_graphics(GRAPHICS_SOURCE)
        if not args.check:
            sync(listings, METADATA_TARGET, GRAPHICS_SOURCE)
            sync_fdroid(listings, FDROID_TARGET, GRAPHICS_SOURCE)
    except (OSError, ValueError) as error:
        print(f"erro: {error}", file=sys.stderr)
        return 1

    action = "validada" if args.check else f"gerada em {METADATA_TARGET.relative_to(ROOT)} e {FDROID_TARGET.relative_to(ROOT)}"
    print(f"ficha da Play {action}: {len(listings)} idiomas")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
