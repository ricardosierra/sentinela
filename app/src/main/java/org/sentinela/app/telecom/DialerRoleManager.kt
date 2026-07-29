package org.sentinela.app.telecom

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Encapsula o gerenciador de papéis do sistema para o papel de telefone padrão.
 *
 * Mesma forma da triagem, papel diferente — por isso a consulta vive na base comum. Duas coisas,
 * porém, são específicas deste papel e precisam estar escritas aqui, porque cada uma já custou
 * tempo de quem tentou o contrário:
 *
 * **Não existe aviso de mudança de detentor** para um aplicativo comum, exatamente como na
 * triagem. A perda do papel é descoberta perguntando, quando a tela volta ao primeiro plano. Não
 * há observador a procurar.
 *
 * **Não existe API pública para um aplicativo remover o próprio papel.** A que existe é de uso do
 * sistema e exige permissão proibida neste projeto. Reverter, portanto, é abrir a tela de escolha
 * de aplicativos padrão e deixar o usuário decidir — o que também é o que o produto quer: o
 * aplicativo nunca força a troca.
 *
 * E uma proibição permanente, que vale para sempre e não é questão de estilo: **nunca** desligar o
 * modo discador desabilitando componente próprio pelo gerenciador de pacotes. A plataforma verifica
 * os requisitos do papel continuamente; um aplicativo que deixe de cumpri-los tem o papel removido
 * **e é encerrado** pelo sistema. O usuário perderia o aplicativo na mão.
 */
class DialerRoleManager(private val context: Context) :
    SystemRoleGate(context, RoleManager.ROLE_DIALER) {

    /**
     * Intenção de reversão: a tela de escolha de aplicativos padrão do sistema. Único caminho
     * existente, pela razão registrada na documentação desta classe.
     */
    fun buildRevertIntent(): Intent = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        .putExtra(EXTRA_PACOTE_DE_ORIGEM, context.packageName)

    private companion object {
        /**
         * Identificação de quem abriu a tela. A plataforma usa isto apenas para o botão de voltar
         * das Configurações; nenhum dado do usuário acompanha a intenção.
         */
        const val EXTRA_PACOTE_DE_ORIGEM = "android.provider.extra.APP_PACKAGE"
    }
}
