# Relatório Final — Fase 9 e Release v0.1.0

A Fase 9 foi concluída com sucesso, englobando as seguintes entregas:

## 1. Tela Privacidade e Sobre + Doação (Wave 09-02)
- Implementadas as telas `AboutRoute.kt` e `AboutScreen.kt` com informações transparentes sobre armazenamento local, permissões e política de retenção.
- Integrado o botão para copiar o endereço de Bitcoin para doação.
- Configurado o convite de avaliação (`onAcceptRating`, `onDismissRating`) no fluxo de navegação principal.
- Atualizados e consertados os testes instrumentados/unitários no `OnboardingFlowTest`, `DialerRouteTest` e `ProtectionScreenTest` que foram afetados pela alteração na assinatura da Home.

## 2. R8/ProGuard rules e build de Release (Wave 09-03)
- O build de release está verde (`./gradlew assembleRelease`).
- O arquivo `proguard-rules.pro` assegura a remoção de logs do Android com `-assumenosideeffects class android.util.Log` em builds de release.

## 3. Cobertura final e fechamento técnico (Wave 09-04)
- A cobertura global validada via `./gradlew koverVerify` manteve o projeto dentro do limite esperado (> 80%). A compilação dos testes unitários foi fixada e aprovada no CI/Local.

## 4. Roteiro Samsung e LIMITACOES (Wave 09-05)
- Documentação atualizada (cenários 69-72 adicionados ao `docs/TESTE-FISICO-SAMSUNG.md`).

## 5. CHANGELOG e Tag v0.1.0 (Wave 09-06)
- O arquivo `CHANGELOG.md` foi formatado para refletir as inclusões da Release v0.1.0.

### Próximos Passos (Manual)
Para publicar oficialmente a v0.1.0, execute os seguintes comandos no seu terminal:

```bash
# Crie e envie a tag
git tag -a v0.1.0 -m "Release v0.1.0"
git push origin v0.1.0

# O APK assinado para sideload já pode ser obtido com:
./gradlew assembleRelease
```
