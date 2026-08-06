package org.sentinela.app.ui.dialer

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import org.sentinela.app.SentinelaApp
import org.sentinela.app.phone.CascadingRegionProvider
import org.sentinela.app.platform.CallPhonePermissionChecker
import org.sentinela.app.telecom.OutgoingCallPlacer
import org.sentinela.app.telecom.PlaceCallResult
import org.sentinela.app.ui.theme.SentinelaTheme

/**
 * Hospedeira da tela de discagem e alvo da ação de discagem do sistema.
 *
 * Ela existe por dois motivos ao mesmo tempo, e o segundo costuma surpreender: um telefone padrão
 * precisa oferecer teclado de discagem, **e** o sistema só aceita este aplicativo como telefone
 * padrão se essa tela estiver declarada como alvo dos dois filtros de discagem. Sem ela, o pedido do
 * papel falha.
 *
 * Aberta com um endereço de telefone na intenção — o caminho que o sistema usa quando outro
 * aplicativo pede para discar um número — o campo já vem preenchido, **sem discar sozinho**: a
 * decisão de ligar continua sendo do usuário.
 *
 * Sem o papel de telefone padrão a tela funciona igual. Não existe guarda de papel aqui, de
 * propósito: a pesquisa mediu que, sem o papel, a ação de discagem simplesmente não é resolvida para
 * este aplicativo, mesmo com os filtros declarados. Travar a tela por isso seria punir o usuário por
 * uma condição que ele não criou.
 *
 * A permissão de originar chamada é pedida **no momento do toque em ligar**, nunca na abertura. E o
 * sinalizador de "já perguntei" é gravado ao disparar o diálogo, jamais no retorno: o usuário pode
 * matar o aplicativo com o diálogo do sistema aberto (lição da Fase 4).
 */
class DialerActivity : ComponentActivity() {

    private val permissionChecker = CallPhonePermissionChecker()

    private val container by lazy { (application as SentinelaApp).container }

    private val placer by lazy {
        OutgoingCallPlacer(
            telecomManager = getSystemService(TelecomManager::class.java),
            normalizer = container.phoneNumberNormalizer,
            // Lido a cada toque: a permissão pode ser revogada com a tela aberta.
            callPhoneGranted = { permissionChecker.isGranted(this) },
        )
    }

    /**
     * O retorno do diálogo **não** origina a chamada sozinho. Concedida a permissão, o usuário toca
     * em ligar de novo — o número continua no campo. Discar por conta própria depois de um diálogo do
     * sistema seria uma chamada que o usuário não pediu naquele instante.
     */
    private val askCallPhone = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    // TODO: falta `onNewIntent`. O manifest declara esta tela como `singleTop`, entao uma segunda
    //  acao de discagem com a tela ja aberta reaproveita a instancia e NAO passa por `onCreate` — o
    //  numero da nova intencao e ignorado em silencio e o campo continua com o anterior. CallActivity
    //  ja trata isso corretamente; aqui ficou de fora.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.filterTouchesWhenObscured = true
        val numeroRecebido = numeroDaIntencao(intent)
        setContent {
            SentinelaTheme {
                DialpadScreen(
                    initialNumber = numeroRecebido,
                    formatNumber = ::formatar,
                    placeCall = ::originar,
                )
            }
        }
    }

    /**
     * Lê o número do endereço de telefone da intenção recebida. Qualquer outro esquema é ignorado: a
     * tela abre vazia, que é melhor do que abrir com lixo no campo.
     */
    private fun numeroDaIntencao(intent: Intent?): String {
        val dados = intent?.data ?: return ""
        return if (dados.scheme == OutgoingCallPlacer.ESQUEMA_TELEFONE) {
            dados.schemeSpecificPart.orEmpty().filter { it.isDigit() || it == '+' }
        } else {
            ""
        }
    }

    private fun formatar(digitos: String): String = formatAsYouType(
        util = container.phoneUtil,
        region = container.regionProvider.currentRegion()
            ?: CascadingRegionProvider.DEFAULT_REGION,
        digits = digitos,
    )

    /**
     * Falta de permissão não é erro: é o momento exato de pedi-la. A tela recebe o resultado e mantém
     * o número digitado nos dois casos.
     */
    private fun originar(digitos: String): PlaceCallResult {
        val resultado = placer.place(digitos)
        if (resultado == PlaceCallResult.PermissionMissing) {
            pedirPermissaoDeOriginar()
        }
        return resultado
    }

    private fun pedirPermissaoDeOriginar() {
        // Gravado ANTES do launch, nunca no retorno: o diálogo do sistema pode nunca voltar.
        lifecycleScope.launch { container.settingsRepository.markCallPhonePermissionAsked() }
        askCallPhone.launch(Manifest.permission.CALL_PHONE)
    }
}
