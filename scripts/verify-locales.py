#!/usr/bin/env python3
"""Verifica a integridade das traduções empacotadas e do seletor de idioma Android."""

from __future__ import annotations

import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "app/src/main/res"
BASE = RES / "values/strings.xml"
CONFIG = RES / "xml/locales_config.xml"
MANIFEST = ROOT / "app/src/main/AndroidManifest.xml"
PLACEHOLDER = re.compile(r"%(?:\d+\$)?[a-zA-Z]")
RESOURCE_TAGS = {"string", "plurals", "string-array"}


def fail(message: str) -> None:
    raise ValueError(message)


def element_signature(element: ET.Element) -> tuple:
    if element.tag == "plurals":
        return tuple(
            sorted(
                (
                    child.attrib.get("quantity", ""),
                    tuple(sorted(set(PLACEHOLDER.findall("".join(child.itertext()))))),
                )
                for child in element
            )
        )
    if element.tag == "string-array":
        return tuple(
            tuple(sorted(set(PLACEHOLDER.findall("".join(child.itertext())))))
            for child in element
        )
    return tuple(sorted(set(PLACEHOLDER.findall("".join(element.itertext())))))


def placeholders_compatible(base: ET.Element, translated: ET.Element) -> bool:
    """Exige os placeholders da base, permitindo categorias plurais extras por idioma."""
    if base.tag != translated.tag:
        return False
    if base.tag != "plurals":
        return element_signature(base) == element_signature(translated)
    base_quantities = {
        child.attrib.get("quantity", ""): tuple(
            sorted(set(PLACEHOLDER.findall("".join(child.itertext()))))
        )
        for child in base
    }
    translated_quantities = {
        child.attrib.get("quantity", ""): tuple(
            sorted(set(PLACEHOLDER.findall("".join(child.itertext()))))
        )
        for child in translated
    }
    return all(
        translated_quantities.get(quantity) == placeholders
        for quantity, placeholders in base_quantities.items()
    )


def entries(path: Path) -> dict[str, ET.Element]:
    try:
        root = ET.parse(path).getroot()
    except (ET.ParseError, OSError) as error:
        fail(f"XML inválido em {path.relative_to(ROOT)}: {error}")
    return {
        element.attrib["name"]: element
        for element in root
        if element.tag in RESOURCE_TAGS
        and element.attrib.get("translatable", "true") != "false"
    }


def locale_from_qualifier(qualifier: str) -> str:
    return qualifier.replace("-r", "-")


def main() -> int:
    try:
        if not BASE.is_file():
            fail(f"recurso base ausente: {BASE.relative_to(ROOT)}")
        if not CONFIG.is_file():
            fail(f"configuração de idiomas ausente: {CONFIG.relative_to(ROOT)}")
        if 'android:localeConfig="@xml/locales_config"' not in MANIFEST.read_text(
            encoding="utf-8"
        ):
            fail("AndroidManifest.xml não referencia @xml/locales_config")

        base = entries(BASE)
        if not base:
            fail("nenhuma string traduzível no recurso base")

        resource_locales: set[str] = set()
        files = sorted(RES.glob("values-*/strings.xml"))
        for path in files:
            qualifier = path.parent.name.removeprefix("values-")
            locale = locale_from_qualifier(qualifier)
            resource_locales.add(locale)
            translated = entries(path)
            missing = sorted(set(base) - set(translated))
            extra = sorted(set(translated) - set(base))
            if missing or extra:
                fail(
                    f"{path.parent.name}: chaves divergentes; "
                    f"faltando={missing[:5]}, extras={extra[:5]}"
                )
            for name in base:
                if not placeholders_compatible(base[name], translated[name]):
                    fail(f"{path.parent.name}: placeholders incompatíveis em {name}")

        if "pt-BR" not in resource_locales:
            fail("values-pt-rBR/strings.xml é obrigatório para proteger o pt-BR")

        config_root = ET.parse(CONFIG).getroot()
        configured = {
            element.attrib.get("{http://schemas.android.com/apk/res/android}name")
            for element in config_root
            if element.tag == "locale"
        }
        if configured != resource_locales:
            fail(
                "locales_config.xml diverge dos recursos: "
                f"faltando={sorted(resource_locales - configured)}, "
                f"extras={sorted(configured - resource_locales)}"
            )

        print(
            f"locales Android verificados: {len(resource_locales)} locales, "
            f"{len(base)} entradas traduzíveis por locale, placeholders íntegros"
        )
        return 0
    except (OSError, ValueError, KeyError) as error:
        print(f"erro: {error}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
