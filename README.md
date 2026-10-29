# Intel Park · application Android

Kotlin · Jetpack Compose · Material 3 · Android 8.0+ (minSdk 26, targetSdk 35).

```bash
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
```

Ou ouvrir ce dossier dans Android Studio et lancer **Run**. Au premier lancement, saisir l’adresse du
serveur (roue dentée de l’écran d’accueil), par exemple `http://192.168.1.50` pour le Raspberry Pi du
prototype.

* Textes : `python tools/build_strings.py` régénère le français (`values/`) et l’anglais (`values-en/`).
* Documentation complète : `../docs/ANDROID.md` et `../docs/BLUETOOTH.md`.
