# ivoiregospel-android

Application Android (WebView) du site [ivoiregospel.com](https://www.ivoiregospel.com/). L'app ne contient aucun contenu : elle affiche le vrai site en direct, donc toute mise à jour du site apparaît automatiquement dans l'app, sans rien recompiler.

Détails complets (ce que fait l'app, structure du projet, publication sur le Play Store…) : voir [`LISEZ-MOI-ANDROID.md`](LISEZ-MOI-ANDROID.md).

## Obtenir un .apk sans installer Android Studio

Ce dépôt compile automatiquement l'APK via **GitHub Actions** à chaque envoi sur `main` :

1. Onglet **Actions** de ce dépôt
2. Workflow **« Compiler l'APK »** → dernière exécution (ou *Run workflow* pour en lancer une manuellement)
3. En bas de la page de l'exécution, section **Artifacts** → télécharger `ivoiregospel-debug-apk` (fichier .zip contenant le .apk)

C'est un APK de **debug** : installable directement sur un téléphone Android (il faut autoriser « Installer des apps inconnues » la première fois) ou à partager pour tester. Pour une publication sur le **Play Store**, il faut un APK/AAB **signé en version release** — voir la section correspondante dans `LISEZ-MOI-ANDROID.md`.
