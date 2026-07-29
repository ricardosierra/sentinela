package org.sentinela.app

import android.app.Application

/**
 * DI manual (sem Hilt/Koin): o container é criado sob demanda para não
 * penalizar o cold start do CallScreeningService.
 *
 * A criação do processo NÃO é contada como abertura do app, de propósito. O sistema
 * também cria este processo quando precisa vincular o serviço de triagem para uma
 * chamada recebida, sem que ninguém tenha aberto o app; contar esses starts inflaria
 * o contador de aberturas (ENG-01) e faria o convite de avaliação da Fase 9 aparecer
 * no momento errado. Quem conta é a Activity, que só existe quando há alguém olhando
 * para a tela.
 *
 * Pelo mesmo motivo, nada aqui faz I/O síncrono: esta classe define o cold start do
 * caminho de resposta ao Telecom, que tem orçamento apertado.
 */
class SentinelaApp : Application() {

    val container: AppContainer by lazy { AppContainer(this) }
}
