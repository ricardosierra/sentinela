package org.sentinela.app.telecom

import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry

/**
 * Execucao de comando de sistema a partir do teste instrumentado, com a saida de erro separada
 * da saida normal.
 *
 * **Por que a saida de erro e nao o codigo de saida.** O caminho de execucao de comando oferecido
 * a instrumentacao entrega o comando direto ao sistema operacional, sem interprete de linha de
 * comando no meio: nao existe encadeamento, nao existe ponto-e-virgula e, principalmente, nao
 * existe a variavel que carrega o codigo de saida do comando anterior. Tentar montar
 * `<comando>; echo <codigo>` produz uma lista de palavras que o interprete nunca ve. A prova
 * disponivel, e ela e forte, e a **saida de erro**: os comandos de papel usados aqui terminam em
 * silencio absoluto quando dao certo e imprimem a excecao da plataforma quando a elegibilidade e
 * negada — foi exatamente esse par que a pesquisa da fase mediu, com codigo 255 acompanhado de
 * texto de excecao. Saida de erro vazia e, portanto, o equivalente observavel do codigo zero, e
 * cada assercao deste pacote ainda confere o **efeito** do comando (quem detem o papel) em vez de
 * confiar apenas no silencio.
 */
internal object TelecomShell {

    /** Papel de telefone padrao do aparelho. */
    const val ROLE_DIALER: String = "android.app.role.DIALER"

    /** Papel de triagem de chamadas, que a Fase 5 usa e que precisa sobreviver a reversao. */
    const val ROLE_SCREENING: String = "android.app.role.CALL_SCREENING"

    /** Detentor do papel de telefone padrao pre-instalado no aparelho virtual. */
    const val PRELOADED_DIALER: String = "com.google.android.dialer"

    data class Result(val output: String, val error: String) {
        /** Terminou sem nada na saida de erro — o equivalente observavel do codigo zero. */
        val succeeded: Boolean get() = error.isBlank()
    }

    fun run(command: String): Result {
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        val descriptors = automation.executeShellCommandRwe(command)
        descriptors[STDIN].close()
        val saida = readAndClose(descriptors[STDOUT])
        val erro = readAndClose(descriptors[STDERR])
        return Result(saida, erro)
    }

    /** Comando de concessao de papel — o caminho que VERIFICA a elegibilidade do aplicativo. */
    fun addRoleHolderCommand(role: String, packageName: String): String =
        "cmd role add-role-holder $role $packageName"

    /** Concede o papel pelo caminho que VERIFICA a elegibilidade do aplicativo. */
    fun addRoleHolder(role: String, packageName: String): Result =
        run(addRoleHolderCommand(role, packageName))

    fun removeRoleHolder(role: String, packageName: String): Result =
        run("cmd role remove-role-holder $role $packageName")

    fun roleHolders(role: String): String =
        run("cmd role get-role-holders $role").output.trim()

    fun holds(role: String, packageName: String): Boolean =
        roleHolders(role).contains(packageName)

    /** Diagnostico do sistema de telefonia — usado para afirmar sobrevivencia de chamada. */
    fun telecomDump(): String = run("dumpsys telecom").output

    /** Comando de limpeza de chamadas presas, verificado na pesquisa da fase. */
    const val CLEANUP_STUCK_CALLS_COMMAND: String = "telecom cleanup-stuck-calls"

    fun cleanupStuckCalls(): Result = run(CLEANUP_STUCK_CALLS_COMMAND)

    fun placeOutgoingCall(number: String): Result =
        run("am start -a android.intent.action.CALL -d tel:$number")

    private fun readAndClose(descriptor: ParcelFileDescriptor): String =
        ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { stream ->
            stream.readBytes().toString(Charsets.UTF_8)
        }

    private const val STDOUT = 0
    private const val STDIN = 1
    private const val STDERR = 2
}
