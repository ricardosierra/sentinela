package org.sentinela.app.telecom

import android.app.role.RoleManager
import android.content.Context

/**
 * Encapsula o gerenciador de papéis do sistema para o papel de triagem, para a interface nunca
 * falar com a telefonia diretamente.
 *
 * O papel de triagem pode ser perdido a qualquer momento: outro aplicativo assume, o usuário troca
 * a escolha nas configurações do sistema, uma atualização mexe no padrão. A forma da consulta —
 * e a razão de ela ser uma pergunta pontual, e não um observador — está na base comum, que o modo
 * discador reaproveita para o papel dele.
 *
 * A consequência prática é simples: a verificação acontece quando a tela volta ao primeiro plano
 * (Fase 7), e a interface oferece o caminho de corrigir a configuração quando a resposta for
 * negativa.
 */
class ScreeningRoleManager(context: Context) :
    SystemRoleGate(context, RoleManager.ROLE_CALL_SCREENING)
