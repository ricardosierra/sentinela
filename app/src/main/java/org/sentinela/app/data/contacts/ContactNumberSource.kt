package org.sentinela.app.data.contacts

/**
 * Contrato fino da fonte de numeros da agenda.
 *
 * A implementacao real (`ContactsContractLookupSource`) e a **UNICA** classe do app autorizada a
 * conhecer o provider de contatos do sistema. Nenhum outro arquivo importa esse provider — o
 * repositorio, o cache e o motor de decisao falam somente com esta interface.
 *
 * Aqui nao existe decisao nem cache: a interface so le e responde. Quem transforma o resultado em
 * `ContactLookup` e o repositorio; quem guarda chaves e o cache. Essa separacao e o que permite
 * testar as regras em JVM pura, com uma fonte falsa, sem emulador.
 */
internal interface ContactNumberSource {

    /**
     * Permissao de leitura de contatos concedida agora.
     *
     * Consultado ANTES de qualquer query. Excecao de seguranca nunca e usada como detector de
     * permissao ausente — ela e apenas a rede de protecao para a revogacao que acontece entre a
     * verificacao e o uso.
     */
    fun hasPermission(): Boolean

    /**
     * Sonda dupla: `true` se ALGUMA das consultas casar.
     *
     * Duas sondas porque o provider normaliza os numeros na escrita usando o pais do aparelho:
     * consulta iniciada com `+` nao alcanca contato gravado em formato nacional estrangeiro, e a
     * consulta nacional nao alcanca contato gravado em E.164 estrangeiro (matriz medida em
     * `04-RESEARCH.md`). Le somente a cardinalidade do cursor — nunca uma coluna.
     *
     * @param nationalDigits numero nacional significativo, ou `null` quando nao existe.
     */
    fun probe(e164: String, nationalDigits: String?): Boolean

    /**
     * Numeros CRUS da agenda (apenas a coluna de numero, nada de identidade).
     *
     * Caro: ~1,5 s com 5.000 contatos, contando a normalizacao. Jamais pode ser chamado no
     * caminho de resposta de uma consulta — so em background, para aquecer o cache.
     */
    fun allRawNumbers(): List<String>

    /** Registra observacao de mudancas na agenda. O callback pode vir em rajada. */
    fun observeChanges(onChange: () -> Unit)

    /** Libera a observacao. Ver KDoc da implementacao: em producao nunca e chamado. */
    fun close()
}
