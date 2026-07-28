plugins {
    // AGP 9+ tem Kotlin embutido (built-in Kotlin) — não aplicar org.jetbrains.kotlin.android
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.detekt) apply false
}
