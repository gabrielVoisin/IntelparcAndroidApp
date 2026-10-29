# Application Android Intel Park

Le projet Android Studio est dans `android-app/` (Kotlin, Jetpack Compose, Material 3). Une seule
application, deux espaces :

* **Conducteur** : création de compte en 4 étapes (compte, plaque, adresse, téléphone vérifié par SMS),
  accueil, scan du QR code de la place et paiement de la durée, carte des places libres (OpenStreetMap) et
  place la plus proche avec guidage, réservation ponctuelle, abonnement mensuel, identification Bluetooth
  à la place.
* **Agent / administrateur** : contrôle en direct (rafraîchi toutes les 15 s), carte des signalements et
  guidage GPS, vérification d’une place (recherche par plaque, utilisateur trouvé, état des paiements),
  photo de la plaque avec lecture automatique (ML Kit, sur le téléphone) et envoi de la contravention.

Textes en français (par défaut) et en anglais : `android-app/tools/build_strings.py` génère
`res/values/strings.xml` et `res/values-en/strings.xml`.

## Compiler et installer

1. Ouvrir `android-app/` dans **Android Studio** (Ladybug 2024.2 ou plus récent, JDK 17).
2. Laisser Gradle synchroniser (Android Gradle Plugin 8.7, Kotlin 2.1, Compose BOM 2024.12).
3. Brancher un téléphone Android 8.0 ou plus récent (mode développeur) › **Run**.
   Ou en ligne de commande : `./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`.
4. Au premier lancement : roue dentée de l’écran d’accueil › adresse du serveur (`http://<IP de la Pi>`).
   La valeur par défaut se change dans `app/build.gradle.kts` (`DEFAULT_SERVER_URL`).

> Le projet n’a pas pu être compilé dans l’environnement où il a été écrit (pas d’accès au SDK Android ni
> aux dépôts Google/Maven). Le code Kotlin a été vérifié avec le compilateur Kotlin 2.0 (syntaxe, et
> typage des ViewModels). Signalez-moi les éventuelles erreurs de compilation : elles se corrigent vite.

## Architecture du code

```
app/src/main/java/com/drautomation/intelpark/
├── IntelParkApp.kt, MainActivity.kt     conteneur des dépendances, Compose
├── data/                                Retrofit + OkHttp (JWT, rafraîchissement auto), modèles JSON,
│                                        jetons chiffrés (EncryptedSharedPreferences), formats
├── ble/                                 BleProtocol (UUID, trames), DeviceKey (clé Android Keystore),
│                                        BleIdentifier (scan, GATT, défi, preuve, résultat)
└── ui/                                  thème, composants, navigation, écrans auth / driver / admin
```

* **Même API que le site** : `https://<serveur>/api/v1`. Conducteurs : routes `/app/*` (jeton de type
  « customer ») ; agents : routes de l’équipe (jeton « staff »). Un jeton ne fonctionne jamais dans l’autre
  espace. Sur `401`, le jeton de rafraîchissement est utilisé une fois puis remplacé (rotation).
* **Montants** en centimes + devise renvoyée par le serveur (EUR dans les données de démo), affichés au
  format de la langue du téléphone. **Dates** ISO 8601, affichées dans le fuseau du téléphone.
* **Erreurs** : `{"detail": "<code>"}` traduit dans `ui/common/Platform.kt` (`errorMessage`).

## Autorisations demandées

| Autorisation | Pourquoi | Quand |
|---|---|---|
| Caméra | QR code de la place ; photo de la plaque (agents) | au premier scan / à la première photo |
| Appareils à proximité (Bluetooth, Android 12+) | identification à la place | au premier « M’identifier » |
| Position (Android 11 et moins, pour le Bluetooth ; carte) | place libre la plus proche | ouverture de la carte |

`BLUETOOTH_SCAN` est déclaré `neverForLocation` : l’application ne cherche que les armoires Intel Park.

## Identification Bluetooth

Détail du protocole : [BLUETOOTH.md](BLUETOOTH.md). Dans l’application :

* la clé du téléphone est créée une fois dans l’Android Keystore (non exportable) ;
* le jeton signé par le serveur est demandé à `POST /api/v1/app/ble/token` et gardé 24 h ;
* après chaque paiement (option « Identification automatique » du profil) ou sur le bouton
  « M’identifier à la borne », l’application cherche l’armoire la plus proche (meilleur signal),
  se connecte, signe le défi et affiche la décision : reconnu, à payer, place réservée par un autre…

## Avant une publication sur le Play Store

* Supprimer le HTTP en clair (`res/xml/network_security_config.xml`) : serveur en HTTPS.
* Remplacer le paiement de démonstration par le SDK du prestataire choisi (Stripe, TWINT, Datatrans…).
* Notifications (fin de stationnement, alarmes pour les agents) : Firebase Cloud Messaging, jeton déjà
  enregistrable avec `POST /api/v1/auth/push-token`.
* Logo définitif (`res/drawable/ic_launcher_foreground.xml`), police Manrope (`res/font`), activer R8.
