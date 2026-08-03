# Regras mínimas de R8 do Sentinela.
# O CallScreeningService é referenciado pelo manifest e preservado automaticamente.
# libphonenumber-android carrega metadata via recursos próprios — sem regras extras necessárias.

# Remover logs de debug/verbose em release (números nunca são logados completos, mas
# reduz superfície de vazamento e ruído)
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
    public static int wtf(...);
}
