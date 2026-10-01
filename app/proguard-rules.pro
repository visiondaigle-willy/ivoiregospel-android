# Ce fichier est volontairement minimal : l'application est un conteneur
# WebView, il n'y a pas de code Kotlin/Java complexe à obfusquer.
# Si vous activez isMinifyEnabled = true plus tard, gardez ces règles :

-keepclassmembers class * extends android.webkit.WebViewClient {
    public *;
}
-keepclassmembers class * extends android.webkit.WebChromeClient {
    public *;
}
