#!/usr/bin/env python3
"""Gera os gráficos da ficha da Google Play a partir da arte do launcher.

Por quê um gerador e não PNGs soltos: o ícone da loja e o ícone do app precisam
ser a mesma arte. Se alguém trocar `ic_launcher_foreground.xml` e os PNGs da loja
forem arquivos avulsos, a ficha passa a mostrar um ícone que o aparelho não mostra
— e ninguém percebe. Aqui os dois saem da mesma fonte.

Saídas em docs/loja/graficos/:
  icone-512.png              512x512  — ícone da ficha (obrigatório)
  feature-graphic-<loc>.png  1024x500 — banner da ficha (obrigatório), por idioma

Uso:
  python3 scripts/build-store-assets.py

Requer Google Chrome (renderização). Sobrescreve as saídas.
"""

import re
import subprocess
import sys
import tempfile
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
VECTOR = ROOT / "app/src/main/res/drawable/ic_launcher_foreground.xml"
COLORS = ROOT / "app/src/main/res/values/colors.xml"
OUT = ROOT / "docs/loja/graficos"
CHROME = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"

ANDROID_NS = "http://schemas.android.com/apk/res/android"

# Texto do banner. Fonte: docs/loja/PLAY-STORE.md (descrição curta de cada idioma),
# encurtado — banner com frase longa fica ilegível no card da loja.
LOCALES = {
    "pt-BR": ("Número desconhecido não toca", "100% offline · sem anúncios · sem nuvem"),
    "en-US": ("Unknown numbers don't ring", "100% offline · no ads · no cloud"),
    "es-419": ("Los desconocidos no suenan", "100% sin conexión · sin anuncios · sin nube"),
}


def falhar(msg):
    print(f"erro: {msg}", file=sys.stderr)
    sys.exit(1)


def ler_cor_de_fundo():
    """Lê ic_launcher_background de colors.xml — a mesma cor que o adaptive icon usa."""
    raiz = ET.parse(COLORS).getroot()
    for cor in raiz.findall("color"):
        if cor.get("name") == "ic_launcher_background":
            return cor.text.strip()
    falhar(f"ic_launcher_background não encontrado em {COLORS}")


def pontos_do_path(d):
    """Pontos de um pathData absoluto (M/L/H/V/C/Z), para bounding box.

    Os pontos de controle do C entram junto: o resultado fica conservador (nunca
    corta a arte), que é o que importa para enquadrar.
    """
    tokens = re.findall(r"[MLHVCZmlhvcz]|-?\d*\.?\d+", d)
    pontos, x, y, i = [], 0.0, 0.0, 0
    while i < len(tokens):
        cmd = tokens[i]
        i += 1
        if cmd in "Zz":
            continue
        if cmd not in "MLHVCmlhvc":
            continue
        while i < len(tokens) and re.match(r"^-?\d", tokens[i]):
            if cmd in "MLml":
                x, y = float(tokens[i]), float(tokens[i + 1])
                i += 2
            elif cmd in "Hh":
                x = float(tokens[i])
                i += 1
            elif cmd in "Vv":
                y = float(tokens[i])
                i += 1
            else:  # C
                for k in range(3):
                    pontos.append((float(tokens[i + k * 2]), float(tokens[i + k * 2 + 1])))
                x, y = float(tokens[i + 4]), float(tokens[i + 5])
                i += 6
            pontos.append((x, y))
    return pontos


def ler_arte():
    """Converte o VectorDrawable do launcher em fragmento SVG.

    O <group> do Android aplica translate depois de scale (pivot 0,0), que é
    exatamente a ordem que o SVG lê em `transform="translate(...) scale(...)"`.

    Devolve também a caixa da arte visível: o adaptive icon reserva margem grande
    em volta, então quem quiser o escudo cheio (o banner) precisa desse recorte,
    e quem precisa da geometria do launcher (o ícone) usa o viewport inteiro.
    """
    raiz = ET.parse(VECTOR).getroot()
    viewport_w = float(raiz.get(f"{{{ANDROID_NS}}}viewportWidth"))
    viewport_h = float(raiz.get(f"{{{ANDROID_NS}}}viewportHeight"))
    todos = []

    def paths_de(no, transformar):
        saida = []
        for path in no.findall("path"):
            d = path.get(f"{{{ANDROID_NS}}}pathData")
            fill = path.get(f"{{{ANDROID_NS}}}fillColor", "#000000")
            saida.append(f'<path fill="{fill}" d="{d}"/>')
            todos.extend(transformar(p) for p in pontos_do_path(d))
        return saida

    partes = paths_de(raiz, lambda p: p)
    for grupo in raiz.findall("group"):
        sx = float(grupo.get(f"{{{ANDROID_NS}}}scaleX", "1"))
        sy = float(grupo.get(f"{{{ANDROID_NS}}}scaleY", "1"))
        tx = float(grupo.get(f"{{{ANDROID_NS}}}translateX", "0"))
        ty = float(grupo.get(f"{{{ANDROID_NS}}}translateY", "0"))
        interno = "".join(paths_de(grupo, lambda p: (tx + sx * p[0], ty + sy * p[1])))
        partes.append(f'<g transform="translate({tx},{ty}) scale({sx},{sy})">{interno}</g>')

    if not partes or not todos:
        falhar(f"nenhum <path> utilizável em {VECTOR}")

    xs = [p[0] for p in todos]
    ys = [p[1] for p in todos]
    caixa = (min(xs), min(ys), max(xs) - min(xs), max(ys) - min(ys))
    return viewport_w, viewport_h, "".join(partes), caixa


def renderizar(html, destino, largura, altura):
    """Screenshot do Chrome headless no tamanho exato pedido.

    `--headless=old` de propósito: o headless novo grava o PNG e às vezes não
    encerra o processo, o que pendurava este script para sempre. O timeout é a
    segunda linha de defesa — se o arquivo saiu, matar o Chrome não é problema.
    """
    destino.unlink(missing_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        pagina = Path(tmp) / "page.html"
        pagina.write_text(html, encoding="utf-8")
        erro = ""
        try:
            proc = subprocess.run(
                [
                    CHROME,
                    "--headless=old",
                    "--disable-gpu",
                    "--hide-scrollbars",
                    "--force-device-scale-factor=1",
                    f"--screenshot={destino}",
                    f"--window-size={largura},{altura}",
                    f"--user-data-dir={tmp}/chrome",
                    pagina.as_uri(),
                ],
                capture_output=True,
                text=True,
                timeout=90,
            )
            erro = proc.stderr[-800:]
        except subprocess.TimeoutExpired:
            erro = "Chrome não encerrou em 90s"
        if not destino.exists():
            falhar(f"Chrome não gerou {destino.name}: {erro}")

    saiu = subprocess.run(
        ["sips", "-g", "pixelWidth", "-g", "pixelHeight", str(destino)],
        capture_output=True,
        text=True,
    ).stdout
    medidas = [int(n) for n in re.findall(r"pixel(?:Width|Height):\s*(\d+)", saiu)]
    if medidas != [largura, altura]:
        falhar(f"{destino.name} saiu em {medidas}, esperado [{largura}, {altura}]")
    print(f"  ✓ {destino.relative_to(ROOT)}  {largura}x{altura}")


BASE_CSS = """
* { margin: 0; padding: 0; box-sizing: border-box; }
html, body { width: 100%; height: 100%; overflow: hidden; }
body {
  font-family: -apple-system, 'SF Pro Display', 'Helvetica Neue', Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
}
"""


def gerar_icone(fundo, vw, vh, arte):
    """512x512. A Play recomenda o ícone sem cantos arredondados — a máscara é dela."""
    destino = OUT / "icone-512.png"
    html = f"""<!doctype html><meta charset="utf-8"><style>{BASE_CSS}
    body {{ background: {fundo}; display: grid; place-items: center; }}
    svg {{ width: 100%; height: 100%; display: block; }}
    </style>
    <svg viewBox="0 0 {vw} {vh}" xmlns="http://www.w3.org/2000/svg">{arte}</svg>"""
    renderizar(html, destino, 512, 512)

    # A Play recusa o ícone que não for PNG de 32 bits. O Chrome entrega 24 bits
    # quando o fundo é opaco, então o canal alfa entra aqui — opaco, só presente.
    subprocess.run(
        ["magick", str(destino), "-alpha", "set", "-define", "png:color-type=6", str(destino)],
        check=True,
        capture_output=True,
    )
    canais = subprocess.run(
        ["magick", "identify", "-format", "%[channels]", str(destino)],
        capture_output=True,
        text=True,
    ).stdout.strip()
    if "a" not in canais:
        falhar(f"icone-512.png ficou sem canal alfa (channels={canais})")


def gerar_banner(locale, titulo, subtitulo, fundo, arte, caixa):
    """1024x500. Conteúdo longe das bordas: a Play recorta o banner em vários formatos."""
    destino = OUT / f"feature-graphic-{locale}.png"
    # Recorte da arte com uma folga de 6% — sem isso o escudo encosta na borda do bloco.
    cx, cy, cw, ch = caixa
    folga = max(cw, ch) * 0.06
    view = f"{cx - folga} {cy - folga} {cw + folga * 2} {ch + folga * 2}"
    html = f"""<!doctype html><meta charset="utf-8"><style>{BASE_CSS}
    body {{
      background:
        radial-gradient(760px 460px at 88% -18%, #17306b 0%, transparent 62%),
        radial-gradient(520px 380px at 4% 118%, #10233f 0%, transparent 60%),
        {fundo};
      display: flex; align-items: center; justify-content: center; gap: 60px;
      padding: 0 76px; color: #E8EEFB;
    }}
    .marca {{ flex: 0 0 210px; height: 258px; }}
    .marca svg {{ width: 100%; height: 100%; display: block; }}
    .nome {{ font-size: 78px; font-weight: 700; letter-spacing: -1.8px; line-height: 1; }}
    .titulo {{ font-size: 36px; font-weight: 600; color: #ADC6FF; margin-top: 22px; line-height: 1.2; }}
    .sub {{ font-size: 24px; font-weight: 400; color: #93A4C4; margin-top: 18px; letter-spacing: 0.2px; }}
    </style>
    <div class="marca">
      <svg viewBox="{view}" xmlns="http://www.w3.org/2000/svg">{arte}</svg>
    </div>
    <div>
      <div class="nome">Sentinela</div>
      <div class="titulo">{titulo}</div>
      <div class="sub">{subtitulo}</div>
    </div>"""
    renderizar(html, destino, 1024, 500)


def main():
    if not Path(CHROME).exists():
        falhar(f"Google Chrome não encontrado em {CHROME}")
    if subprocess.run(["which", "magick"], capture_output=True).returncode != 0:
        falhar("ImageMagick (magick) não encontrado — brew install imagemagick")
    for origem in (VECTOR, COLORS):
        if not origem.exists():
            falhar(f"fonte da arte não encontrada: {origem}")

    OUT.mkdir(parents=True, exist_ok=True)
    fundo = ler_cor_de_fundo()
    vw, vh, arte, caixa = ler_arte()

    print(f"arte: {VECTOR.relative_to(ROOT)} · fundo {fundo}")
    print(f"caixa da arte: x={caixa[0]:.0f} y={caixa[1]:.0f} w={caixa[2]:.0f} h={caixa[3]:.0f}")
    gerar_icone(fundo, vw, vh, arte)
    for locale, (titulo, subtitulo) in LOCALES.items():
        gerar_banner(locale, titulo, subtitulo, fundo, arte, caixa)


if __name__ == "__main__":
    main()
