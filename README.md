# Rotavideo
Aplicativo Android para girar, espelhar e exportar vídeos, com interface em português, com prévia estática que preserva a proporção do vídeo.

## Compilar no GitHub
1. Envie este projeto para um repositório GitHub.
2. Abra **Actions** e acompanhe o fluxo **Android build** após um push.
3. Baixe **giravideo-debug-apk** nos artefatos da execução concluída.

O projeto gera um APK de depuração em `app/build/outputs/apk/debug/app-debug.apk`. Também é possível compilar com Java 17 e Gradle 8.9+: `gradle assembleDebug`.

## Usar
Escolha um vídeo (a busca começa em DCIM e lembra a última pasta), ajuste a rotação (−90°, +90° ou 180°) e o espelhamento horizontal/vertical. Toque em **Exportar vídeo** para salvar automaticamente em DCIM/Rotavideo, com o nome original acrescido de _rotavideo. A exportação é feita no aparelho com Media3 Transformer, codificando a saída em H.264.

Requisitos: Android 8.0 ou superior. O Samsung Galaxy A54 é compatível.
