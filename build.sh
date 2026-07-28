#!/bin/bash

# Script para build do Sentinela
# Uso: ./build.sh

set -e

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

log() {
    echo -e "${BLUE}[$(date +'%Y-%m-%d %H:%M:%S')]${NC} $1"
}

success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

error() {
    echo -e "${RED}[ERROR]${NC} $1"
    exit 1
}

warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log "Buildando Sentinela..."

# Configurar ambiente Java e Android
JAVA_HOME="${JAVA_HOME:-/opt/homebrew/opt/openjdk@17}"
ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
export JAVA_HOME ANDROID_HOME
export PATH="$ANDROID_HOME/platform-tools:$JAVA_HOME/bin:$PATH"

if [ ! -d "$ANDROID_HOME" ]; then
    warning "ANDROID_HOME não encontrado em $ANDROID_HOME. Tentando continuar mesmo assim..."
fi

# Build
./gradlew assembleDebug
if [ $? -eq 0 ]; then
    success "✅ Sentinela buildado com sucesso"
    cp app/build/outputs/apk/debug/app-debug.apk ./sentinela-debug.apk
    log "APK salvo em: ./sentinela-debug.apk"
    echo ""
    echo "📱 Para instalar:"
    echo "   adb install sentinela-debug.apk"
else
    error "❌ Falha no build do Sentinela"
fi
