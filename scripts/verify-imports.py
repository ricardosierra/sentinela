#!/usr/bin/env python3
"""Import orfao em Kotlin: o que sobra quando o codigo que o usava sai do arquivo.

Por que este script existe. A regra `UnusedImports` do detekt exige resolucao de
tipo, e o unico task de detekt que este projeto tem (`detekt`, sem variante) roda
sem classpath — a regra fica desligada em silencio. O resultado apareceu na
refatoracao das tres telas gigantes (2026-08-24): 91 linhas de import ficaram
apontando para simbolos que tinham acabado de sair do arquivo, e nenhuma
ferramenta do repositorio reclamou. Toda extracao futura deixaria o mesmo rastro.

Como decide. Import sem curinga tem um nome simples — o ultimo segmento, ou o
apelido quando ha `as`. Se esse nome nao aparece em NENHUMA outra linha do
arquivo, o import esta orfao. O corpo comparado inclui comentario e KDoc de
proposito: `@see Foo` e `[Foo]` sao usos legitimos de um import, e ignora-los
transformaria documentacao correta em falso positivo.

A busca casa o nome depois de ponto. Isso NAO e descuido: metade dos imports de
um arquivo Compose e funcao de extensao, escrita sempre como `Modifier.padding`
— exigir o nome isolado reprovaria todos eles. O preco e o falso NEGATIVO: um
`import kotlinx.coroutines.flow.map` orfao passa despercebido num arquivo que
chama `lista.map { }`. Este script erra sempre para o lado de nao acusar.

O que NAO e decidido aqui. Nomes que a linguagem usa sem cita-los ficam de fora
(delegacao de propriedade e sobrecarga de operador): `by remember { ... }` chama
getValue sem escrever getValue. Estao em NOMES_IMPLICITOS, e a lista so cresce
com motivo escrito.

Uso: python3 scripts/verify-imports.py [caminho ...]
Sem argumento, varre os tres source sets Kotlin do modulo app.
"""

from __future__ import annotations

import re
import sys
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent

ALVOS_PADRAO = (
    "app/src/main/java",
    "app/src/test/java",
    "app/src/androidTest/java",
)

# Nomes que o compilador resolve por convencao, sem que o arquivo os escreva.
NOMES_IMPLICITOS = {
    # Delegacao de propriedade: `var x by mutableStateOf(...)`, `val y by viewModels()`.
    "getValue",
    "setValue",
    "provideDelegate",
    # Sobrecarga de operador: o arquivo escreve o simbolo, nunca o nome da funcao.
    "plus", "minus", "times", "div", "rem", "unaryPlus", "unaryMinus",
    "inc", "dec", "not", "compareTo", "contains", "rangeTo", "rangeUntil",
    "invoke", "iterator", "get", "set", "hasNext", "next",
}
NOMES_IMPLICITOS.update(f"component{n}" for n in range(1, 10))

IMPORT = re.compile(r"^import\s+([\w.`]+)(?:\s+as\s+(\w+))?\s*(?://.*)?$")


def orfaos_do_arquivo(caminho: Path) -> list[tuple[int, str, str]]:
    linhas = caminho.read_text(encoding="utf-8").splitlines()

    importados: list[tuple[int, str, str]] = []
    indices_de_import: set[int] = set()
    for i, linha in enumerate(linhas):
        casamento = IMPORT.match(linha.strip())
        if not casamento:
            continue
        indices_de_import.add(i)
        caminho_importado, apelido = casamento.group(1), casamento.group(2)
        if caminho_importado.endswith(".*"):
            continue  # curinga nao tem nome simples para procurar
        nome = apelido or caminho_importado.rsplit(".", 1)[-1].strip("`")
        if nome in NOMES_IMPLICITOS:
            continue
        importados.append((i + 1, linha.strip(), nome))

    if not importados:
        return []

    corpo = "\n".join(l for i, l in enumerate(linhas) if i not in indices_de_import)
    return [
        (numero, texto, nome)
        for numero, texto, nome in importados
        if not re.search(rf"(?<!\w){re.escape(nome)}\b", corpo)
    ]


def main(argv: list[str]) -> int:
    alvos = [Path(a) for a in argv[1:]] or [RAIZ / a for a in ALVOS_PADRAO]
    arquivos: list[Path] = []
    for alvo in alvos:
        alvo = alvo if alvo.is_absolute() else RAIZ / alvo
        if alvo.is_dir():
            arquivos.extend(sorted(alvo.rglob("*.kt")))
        elif alvo.is_file():
            arquivos.append(alvo)

    total = 0
    for arquivo in arquivos:
        for numero, texto, nome in orfaos_do_arquivo(arquivo):
            total += 1
            relativo = arquivo.relative_to(RAIZ)
            print(f"      {relativo}:{numero}: {texto}  (o nome '{nome}' nao aparece no arquivo)")

    if total:
        print(f"{total} import(s) orfao(s) em {len(arquivos)} arquivo(s) Kotlin")
        return 1
    print(f"nenhum import orfao em {len(arquivos)} arquivos Kotlin")
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
