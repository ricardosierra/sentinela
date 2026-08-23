package org.sentinela.app.ui.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.sentinela.app.R
import org.sentinela.app.settings.OriginPolicy
import org.sentinela.app.ui.components.InfoBanner

/** Item 3 — as quatro políticas de contato, mais a nota do pré-requisito da agenda. */
@Composable
internal fun GrupoDeContatos(
    state: SettingsUiState,
    hasWhatsApp: Boolean,
    onContactsPolicy: (OriginPolicy) -> Unit
) {
    SettingsGroup(title = stringResource(R.string.settings_contacts_policy)) {
        EscolhaUnica {
            OpcaoDePolitica(
                title = stringResource(R.string.contacts_option_ring),
                description = stringResource(R.string.contacts_option_ring_desc),
                icon = Icons.Outlined.Phone,
                selected = state.settings.contactsPolicy == OriginPolicy.RING,
                onClick = { onContactsPolicy(OriginPolicy.RING) },
            )
            OpcaoDePolitica(
                title = stringResource(R.string.contacts_option_block),
                description = stringResource(R.string.contacts_option_block_desc),
                icon = Icons.Outlined.Block,
                selected = state.settings.contactsPolicy == OriginPolicy.BLOCK,
                onClick = { onContactsPolicy(OriginPolicy.BLOCK) },
            )
            OpcaoDePolitica(
                title = stringResource(R.string.contacts_option_silence),
                description = stringResource(R.string.contacts_option_silence_desc),
                icon = Icons.Outlined.NotificationsOff,
                selected = state.settings.contactsPolicy == OriginPolicy.SILENCE,
                onClick = { onContactsPolicy(OriginPolicy.SILENCE) },
            )
            OpcaoDePolitica(
                title = stringResource(R.string.contacts_option_never_silence),
                description = stringResource(R.string.contacts_option_never_silence_desc),
                icon = Icons.Outlined.Phone,
                selected = state.settings.contactsPolicy == OriginPolicy.NEVER_SILENCE,
                onClick = { onContactsPolicy(OriginPolicy.NEVER_SILENCE) },
            )
        }
        if (hasWhatsApp) {
            InfoBanner(
                text = stringResource(R.string.whatsapp_contacts_warning_desc),
                actionLabel = null,
                onAction = null,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        NotaDoGrupo(text = stringResource(R.string.settings_contacts_policy_note))
    }
}

@Preview(widthDp = 411, heightDp = 700)
@Composable
private fun GrupoDeContatosPreview() {
    SecaoDeExemplo {
        GrupoDeContatos(
            state = SettingsUiState(loading = false),
            hasWhatsApp = false,
            onContactsPolicy = {},
        )
    }
}

/** Com mensageiro instalado entra o aviso de alcance — o unico ramo do grupo. */
@Preview(widthDp = 411, heightDp = 800)
@Composable
private fun GrupoDeContatosComMensageiroPreview() {
    SecaoDeExemplo {
        GrupoDeContatos(
            state = SettingsUiState(loading = false),
            hasWhatsApp = true,
            onContactsPolicy = {},
        )
    }
}
