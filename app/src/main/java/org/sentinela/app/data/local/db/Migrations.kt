package org.sentinela.app.data.local.db

import androidx.room.migration.Migration

/**
 * Cadeia explicita de migracoes. A migracao destrutiva do Room — o fallback que
 * recria o banco do zero quando falta um caminho de upgrade — e PROIBIDA aqui:
 * apagaria a whitelist do usuario numa atualizacao. scripts/verify-invariants.sh
 * recusa a chamada dela em qualquer lugar de app/src/main, INCLUSIVE em comentario,
 * porque uma linha comentada hoje vira uma linha ativa amanha; por isso este texto
 * descreve o metodo em vez de escrever o nome dele.
 * A v1 e a versao inicial: ainda nao ha migracao.
 * Toda versao nova DEVE acrescentar aqui e ganhar teste em MigrationHarnessTest.
 */
val SENTINELA_MIGRATIONS: Array<Migration> = emptyArray()
