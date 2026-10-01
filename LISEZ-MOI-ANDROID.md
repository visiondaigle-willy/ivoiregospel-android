# IVOIREGOSPEL — Version application Android

Il y a deux façons de transformer un site web en « application » sur Android. Je vous livre les deux, mais elles ne se valent pas au niveau de l'effort à fournir — lisez ce qui suit avant de choisir.

---

## Option A — Application installable (PWA) — ✅ prête, aucune compilation

C'est la solution que j'ai intégrée **directement dans le site**. Dès que vous mettez en ligne les fichiers, elle fonctionne : aucun outil supplémentaire, aucune compilation, rien à publier sur le Play Store.

### Ce que ça change pour vos visiteurs
Sur Android, dans Chrome, un bandeau *« Installer IvoireGospel sur votre écran d'accueil »* apparaît automatiquement. En un clic :
- Une icône (le disque vinyle orange/violet, cohérent avec le design du site) apparaît sur l'écran d'accueil du téléphone
- L'app s'ouvre en plein écran, sans barre d'adresse, comme une vraie application
- Un appui long sur l'icône propose des raccourcis directs : *Écouter le direct*, *Musique*, *Actualités*, *Agenda*
- Les pages déjà visitées restent consultables hors connexion (page dédiée si le réseau manque totalement)

### Fichiers ajoutés
| Fichier | Rôle |
|---|---|
| `manifest.json` | Nom, icônes, couleurs, raccourcis de l'application |
| `service-worker.js` | Mise en cache intelligente (jamais pour l'audio, les images, ni `/admin/`) |
| `offline.html` | Page affichée si aucune connexion et rien en cache |
| `assets/icons/*.png` | Icônes à toutes les tailles requises |
| `favicon.ico`, `favicon.png` | Icône d'onglet navigateur |

`includes/header.php` et `assets/js/site.js` ont été mis à jour pour tout relier (déjà inclus dans l'archive complète du site).

### Aucune action requise de votre part
Ça fonctionne dès la mise en ligne. Seul point à vérifier : servez le site en **HTTPS** (requis par les navigateurs pour activer un service worker — normalement déjà le cas si vous avez un certificat SSL actif).

---

## Option B — Application native (.apk / Play Store)

Ici, la nuance est importante : **je ne peux pas vous livrer un fichier .apk compilé.** Produire un .apk demande le kit de développement Android (Android SDK) et une chaîne de compilation (Gradle) — des outils qui tournent sur un ordinateur avec Android Studio installé, pas dans cet environnement. Un `.apk` que je prétendrais avoir « compilé » sans ces outils serait non fonctionnel.

Ce que je vous donne à la place : **le projet Android Studio complet et prêt**, code source inclus — il ne reste plus qu'à l'ouvrir et cliquer sur « Build ».

### Ce que fait cette application
Une WebView (un navigateur intégré, sans interface de navigateur autour) qui charge `https://www.ivoiregospel.com/`. Autrement dit : le même site, dans une coquille native. Concrètement, j'ai ajouté ce qu'un navigateur ne fait pas nativement :
- Écran « Pas de connexion » avec bouton Réessayer
- Tirer-pour-actualiser
- Les liens Facebook / YouTube / WhatsApp / Instagram s'ouvrent dans leurs propres applications plutôt que dans la WebView
- Téléchargement des MP3 via le gestionnaire de téléchargements du système
- Bouton retour du téléphone qui navigue dans l'historique du site avant de fermer l'app
- Icône et écran de démarrage aux couleurs du site

### Ce qu'il vous faut pour compiler
1. **Android Studio** (gratuit) — [developer.android.com/studio](https://developer.android.com/studio)
2. Ouvrir le dossier `android-app/` avec *File → Open*
3. Laisser Android Studio synchroniser le projet (il télécharge automatiquement ce qu'il lui manque — SDK, Gradle — au premier lancement, ça peut prendre quelques minutes)
4. *Build → Generate Signed Bundle / APK* pour produire un fichier installable

### Avant de publier sur le Play Store
- **Changez le nom de package** `com.ivoiregospel.app` si vous voulez un identifiant qui vous est propre (clic droit sur le package dans Android Studio → *Refactor → Rename*)
- Un compte développeur Google Play coûte 25 $ (paiement unique)
- Icône 512×512 pour la fiche du store déjà prête : `store-assets/play-store-icon-512.png`

### Ce qui n'est PAS inclus (et pourquoi)
- **Notifications push** — demande de créer un projet Firebase qui vous appartient (identifiants propres à vous, je ne peux pas le faire à votre place). Je peux vous guider pour l'ajouter plus tard si besoin.
- **Fichier .apk compilé** — comme expliqué plus haut, nécessite Android Studio de votre côté.

### Structure du projet
```
android-app/
├── build.gradle.kts, settings.gradle.kts, gradle.properties
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/ivoiregospel/app/MainActivity.kt
│       └── res/  (layout, couleurs, thèmes, icônes à toutes les densités)
└── store-assets/play-store-icon-512.png
```

---

## En résumé : laquelle choisir ?

| | PWA (Option A) | App native (Option B) |
|---|---|---|
| Fonctionne dès maintenant | ✅ | ❌ (à compiler) |
| Outils nécessaires | Aucun | Android Studio |
| Présence sur le Play Store | Non (installable via Chrome) | Oui, possible |
| Effort de maintenance | Aucun — c'est le même site | Recompiler si vous changez l'enveloppe native |
| Mises à jour de contenu | Automatiques (même serveur) | Automatiques aussi (WebView recharge le site à chaque ouverture) |

**Mon conseil :** commencez par l'Option A (déjà active), qui couvre l'essentiel de ce qu'un visiteur attend d'une « app ». Passez à l'Option B seulement si une présence sur le Play Store compte vraiment pour vous.
