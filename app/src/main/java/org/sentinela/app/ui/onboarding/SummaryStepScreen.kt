package org.sentinela.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.data.contacts.ContactsPermissionState
import org.sentinela.app.settings.OriginPolicy
import org.sentinela.app.ui.components.CheckRow
import org.sentinela.app.ui.components.HonestyCard
import org.sentinela.app.ui.components.SentinelaTopBar
import org.sentinela.app.ui.components.StepHeader
import org.sentinela.app.ui.theme.CallAccept
import org.sentinela.app.ui.theme.SentinelaTheme
import org.sentinela.app.ui.theme.ShapePill
import org.sentinela.app.ui.theme.StatusAttention

private const val STEP_NUMBER = 6
private const val STEP_TOTAL = 6
private const val CHECK_ROW_COUNT = 4
private const val CHECK_COLUMN_COUNT = 1

private val ScreenPadding = 16.dp
private val TopGap = 24.dp
private val HeroCircleSize = 96.dp
private val HeroIconSize = 48.dp
private val HeroToTitleGap = 16.dp
private val TitleToBodyGap = 8.dp
private val BodyToChecksGap = 24.dp
private val ChecksToHonestyGap = 24.dp
private val HonestyToCtaGap = 32.dp
private val CtaHeight = 56.dp

/** Opacidade do círculo do veredito — o mesmo destaque leve dos containers da Fase 6. */
private const val HERO_CIRCLE_ALPHA = 0.15f

/**
 * Passo 6 de 6 — a verificação final, e o ecrã onde a honestidade do onboarding é medida.
 *
 * **O veredito nunca é falsamente positivo.** Com o papel de triagem ausente, o título é a variante
 * parcial, o círculo usa a cor de atenção e a primeira linha traz a ação de correção. É a mesma
 * regra do zero mentiroso da home, aplicada aqui: o Sentinela não escreve que está tudo pronto
 * quando não está. Um "tudo pronto" verde sobre um app que não recebeu o papel seria a pior falha
 * possível de um onboarding cujo propósito inteiro é o usuário entender o próprio estado — ele
 * sairia daqui confiando numa proteção que não existe e descobriria a verdade na primeira chamada
 * que passou.
 *
 * Cada linha comunica o estado por ÍCONE e por TEXTO, nunca só por cor: quem não distingue verde de
 * vermelho lê exatamente a mesma informação.
 *
 * **Sem a ação de pular.** Pular está disponível nos passos 1 a 5; aqui já é o fim, e o único
 * caminho é concluir. Oferecer "pular" na tela de conferência não teria nada a pular.
 *
 * **O cartão de honestidade repete EXATAMENTE os três itens do passo 1**, na mesma ordem e com os
 * mesmos identificadores de recurso. A repetição é deliberada, e não descuido: este é o último ecrã
 * antes de o usuário passar a confiar no aplicativo, e o único em que ele já viu o aplicativo
 * inteiro — no passo 1 ele leu as três frases sem ter contexto para avaliá-las. Reescrevê-las aqui
 * seria o caminho de volta à promessa falsa; as três nasceram da fonte da plataforma nas Fases 5 e
 * 6 e não se tocam.
 *
 * Semântica: a lista declara informação de coleção com quatro linhas; cada linha é anunciada como
 * rótulo seguido do estado; e as ações de correção são botões focáveis SEPARADOS, irmãos do nó da
 * linha e nunca filhos dele — dentro do nó mesclado o botão ficaria inalcançável pelo leitor de
 * tela, porque o nó da linha responderia por ele. É o ponto de risco medido em 07-03.
 */
@Composable
fun SummaryStepScreen(
    roleHeld: Boolean,
    contactsPermission: ContactsPermissionState,
    unknownPolicy: OriginPolicy,
    contactsPolicy: OriginPolicy,
    whitelistPolicy: OriginPolicy,
    onFixRole: () -> Unit,
    onGrantContacts: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().safeDrawingPadding()) {
        SentinelaTopBar(center = { StepHeader(step = STEP_NUMBER, total = STEP_TOTAL) })
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            Veredito(roleHeld = roleHeld)
            ListaDeVerificacao(
                roleHeld = roleHeld,
                contactsPermission = contactsPermission,
                unknownPolicy = unknownPolicy,
                contactsPolicy = contactsPolicy,
                whitelistPolicy = whitelistPolicy,
                onFixRole = onFixRole,
                onGrantContacts = onGrantContacts,
            )
            HonestyCard(
                title = stringResource(R.string.onboarding_scope_title),
                items = listOf(
                    stringResource(R.string.dialer_activation_unchanged_3),
                    stringResource(R.string.onboarding_scope_dnd),
                    stringResource(R.string.settings_hide_native_log_desc),
                ),
                itemIcon = Icons.Outlined.Info,
                modifier = Modifier
                    .padding(horizontal = ScreenPadding)
                    .padding(top = ChecksToHonestyGap),
            )
            Button(
                onClick = onFinish,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = ScreenPadding)
                    .padding(top = HonestyToCtaGap, bottom = HonestyToCtaGap)
                    .height(CtaHeight),
                shape = ShapePill,
            ) {
                Text(
                    text = stringResource(R.string.onboarding_finish),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

/**
 * O círculo, o título e o corpo do veredito.
 *
 * A cor e o título saem do MESMO booleano, de propósito: separá-los abriria a possibilidade de um
 * círculo verde sobre um título parcial.
 */
@Composable
private fun Veredito(roleHeld: Boolean) {
    val cor: Color = if (roleHeld) CallAccept else StatusAttention
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = ScreenPadding)
            .padding(top = TopGap),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .clearAndSetSemantics {}
                .requiredSize(HeroCircleSize)
                .clip(CircleShape)
                .background(cor.copy(alpha = HERO_CIRCLE_ALPHA)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Shield,
                contentDescription = null,
                modifier = Modifier.size(HeroIconSize),
                tint = cor,
            )
        }
        Text(
            text = stringResource(
                if (roleHeld) {
                    R.string.onboarding_summary_title_ok
                } else {
                    R.string.onboarding_summary_title_partial
                },
            ),
            modifier = Modifier.padding(top = HeroToTitleGap),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.onboarding_summary_body),
            modifier = Modifier.padding(top = TitleToBodyGap),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * As quatro linhas de verificação.
 *
 * As duas primeiras têm estado real do sistema e ganham ação de correção quando falta algo; as duas
 * últimas relatam a escolha que o usuário fez e não têm o que corrigir aqui — o corpo do veredito
 * já diz onde mudar depois.
 */
@Composable
private fun ListaDeVerificacao(
    roleHeld: Boolean,
    contactsPermission: ContactsPermissionState,
    unknownPolicy: OriginPolicy,
    contactsPolicy: OriginPolicy,
    whitelistPolicy: OriginPolicy,
    onFixRole: () -> Unit,
    onGrantContacts: () -> Unit,
) {
    val concedido = stringResource(R.string.onboarding_check_granted)
    val ausente = stringResource(R.string.onboarding_check_missing)
    val agendaConcedida = contactsPermission == ContactsPermissionState.GRANTED
    Column(
        modifier = Modifier
            .padding(top = BodyToChecksGap)
            .semantics {
                collectionInfo = CollectionInfo(
                    rowCount = CHECK_ROW_COUNT,
                    columnCount = CHECK_COLUMN_COUNT,
                )
            },
        verticalArrangement = Arrangement.spacedBy(TitleToBodyGap),
    ) {
        CheckRow(
            label = stringResource(R.string.onboarding_check_role),
            stateText = if (roleHeld) concedido else ausente,
            ok = roleHeld,
            actionLabel = if (roleHeld) {
                null
            } else {
                stringResource(R.string.dashboard_fix_configuration)
            },
            onAction = if (roleHeld) null else onFixRole,
        )
        CheckRow(
            label = stringResource(R.string.onboarding_check_contacts),
            stateText = if (agendaConcedida) concedido else ausente,
            ok = agendaConcedida,
            actionLabel = if (agendaConcedida) {
                null
            } else {
                stringResource(R.string.dialer_activation_grant_contacts)
            },
            onAction = if (agendaConcedida) null else onGrantContacts,
        )
        CheckRow(
            label = stringResource(R.string.onboarding_check_unknown),
            stateText = stringResource(rotuloDeDesconhecidos(unknownPolicy)),
            ok = true,
        )
        CheckRow(
            label = stringResource(R.string.onboarding_check_origins),
            stateText = stringResource(
                R.string.state_label_with_value,
                stringResource(rotuloDeContatos(contactsPolicy)),
                stringResource(rotuloDeWhitelist(whitelistPolicy)),
            ),
            ok = true,
        )
    }
}

/**
 * Rótulo da política de desconhecidos.
 *
 * O passo 2 oferece três opções — bloquear, silenciar e permitir — e o enum tem quatro entradas.
 * "Nunca silenciar" cai em "permitir" porque é o que ele significa para esta origem: a chamada
 * chega tocando. Inventar um quarto rótulo aqui mostraria ao usuário uma palavra que ele nunca viu
 * na tela em que escolheu.
 */
private fun rotuloDeDesconhecidos(policy: OriginPolicy): Int = when (policy) {
    OriginPolicy.BLOCK -> R.string.unknown_option_block
    OriginPolicy.SILENCE -> R.string.unknown_option_silence
    OriginPolicy.RING, OriginPolicy.NEVER_SILENCE -> R.string.unknown_option_allow
}

/** Rótulo da política de contatos, com as mesmas palavras do passo 3. */
private fun rotuloDeContatos(policy: OriginPolicy): Int = when (policy) {
    OriginPolicy.RING -> R.string.contacts_option_ring
    OriginPolicy.BLOCK -> R.string.contacts_option_block
    OriginPolicy.SILENCE -> R.string.contacts_option_silence
    OriginPolicy.NEVER_SILENCE -> R.string.contacts_option_never_silence
}

/** Rótulo da política da whitelist, com as mesmas palavras do passo 4. */
private fun rotuloDeWhitelist(policy: OriginPolicy): Int = when (policy) {
    OriginPolicy.RING -> R.string.whitelist_option_ring
    OriginPolicy.BLOCK -> R.string.whitelist_option_block
    OriginPolicy.SILENCE -> R.string.whitelist_option_silence
    OriginPolicy.NEVER_SILENCE -> R.string.whitelist_option_never_silence
}

@Preview(widthDp = 411, heightDp = 891)
@Composable
private fun SummaryStepCompletoPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            SummaryStepScreen(
                roleHeld = true,
                contactsPermission = ContactsPermissionState.GRANTED,
                unknownPolicy = OriginPolicy.BLOCK,
                contactsPolicy = OriginPolicy.RING,
                whitelistPolicy = OriginPolicy.NEVER_SILENCE,
                onFixRole = {},
                onGrantContacts = {},
                onFinish = {},
            )
        }
    }
}

@Preview(widthDp = 411, heightDp = 891)
@Composable
private fun SummaryStepParcialPreview() {
    SentinelaTheme(darkTheme = true, dynamicColor = false) {
        Surface {
            SummaryStepScreen(
                roleHeld = false,
                contactsPermission = ContactsPermissionState.DENIED_ONCE,
                unknownPolicy = OriginPolicy.SILENCE,
                contactsPolicy = OriginPolicy.NEVER_SILENCE,
                whitelistPolicy = OriginPolicy.RING,
                onFixRole = {},
                onGrantContacts = {},
                onFinish = {},
            )
        }
    }
}
