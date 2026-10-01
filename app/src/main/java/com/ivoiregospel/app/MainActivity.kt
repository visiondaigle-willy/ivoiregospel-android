package com.ivoiregospel.app

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.View
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

/**
 * MainActivity — enveloppe WebView du site IvoireGospel.
 *
 * Principe : une seule WebView charge le site réel (même code, mêmes
 * mises à jour côté serveur, rien à republier sur le Play Store pour un
 * changement de contenu). Cette classe ajoute ce qu'un navigateur mobile
 * standard n'offre pas nativement : écran hors-ligne, tirer-pour-actualiser,
 * ouverture des liens externes (Facebook, YouTube, WhatsApp...) dans leurs
 * propres applications, et téléchargement des MP3 via le gestionnaire de
 * téléchargements du système.
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "IvoireGospel"
        private const val SITE_URL = "https://www.ivoiregospel.com/"

        // Domaines qui restent DANS la WebView.
        private val DOMAINES_INTERNES = listOf("ivoiregospel.com", "www.ivoiregospel.com")

        // Domaines qui s'ouvrent toujours dans une application/navigateur externe.
        private val DOMAINES_EXTERNES = listOf(
            "facebook.com", "web.facebook.com", "youtube.com", "youtu.be",
            "twitter.com", "x.com", "wa.me", "whatsapp.com", "instagram.com"
        )
    }

    private lateinit var webView: WebView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var offlineView: View

    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    // Sélecteur de fichier moderne (remplace l'ancien startActivityForResult),
    // utilisé quand le site (notamment le panneau admin) propose un input file.
    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultat ->
        val donnees = resultat.data
        val uris: Array<Uri>? = when {
            resultat.resultCode != RESULT_OK -> null
            donnees?.clipData != null -> {
                val clip = donnees.clipData!!
                Array(clip.itemCount) { i -> clip.getItemAt(i).uri }
            }
            donnees?.data != null -> arrayOf(donnees.data!!)
            else -> null
        }
        filePathCallback?.onReceiveValue(uris)
        filePathCallback = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Le thème de lancement (fond violet + icône, voir Theme.Launcher dans
        // le manifeste) est actif jusqu'ici ; on repasse au thème normal.
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        progressBar = findViewById(R.id.progressBar)
        offlineView = findViewById(R.id.offlineView)

        findViewById<View>(R.id.btnReessayer).setOnClickListener {
            if (estConnecte()) {
                offlineView.visibility = View.GONE
                webView.visibility = View.VISIBLE
                webView.reload()
            } else {
                Toast.makeText(this, "Toujours pas de connexion.", Toast.LENGTH_SHORT).show()
            }
        }

        configurerWebView()

        swipeRefresh.setOnRefreshListener { webView.reload() }
        swipeRefresh.setColorSchemeResources(R.color.orange)

        // Le bouton retour du téléphone navigue dans l'historique de la
        // WebView avant de quitter l'application.
        onBackPressedDispatcher.addCallback(this) {
            if (webView.canGoBack()) {
                webView.goBack()
            } else {
                isEnabled = false
                onBackPressedDispatcher.onBackPressed()
            }
        }

        if (savedInstanceState == null) {
            if (estConnecte()) {
                webView.loadUrl(SITE_URL)
            } else {
                afficherHorsLigne()
            }
        }
    }

    private fun configurerWebView() {
        val reglages = webView.settings
        reglages.javaScriptEnabled = true
        reglages.domStorageEnabled = true // requis : le lecteur radio utilise sessionStorage
        reglages.mediaPlaybackRequiresUserGesture = false // le site attend déjà un clic avant de jouer
        reglages.loadWithOverviewMode = true
        reglages.useWideViewPort = true
        reglages.cacheMode = WebSettings.LOAD_DEFAULT
        reglages.setSupportMultipleWindows(false)

        webView.webViewClient = object : WebViewClient() {

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                val hote = uri.host ?: return false

                if (uri.scheme == "tel" || uri.scheme == "mailto") {
                    ouvrirExterne(uri)
                    return true
                }
                if (DOMAINES_EXTERNES.any { hote.contains(it) }) {
                    ouvrirExterne(uri)
                    return true
                }
                if (DOMAINES_INTERNES.any { hote == it }) {
                    return false // navigation normale, reste dans la WebView
                }
                // Domaine ni reconnu comme interne ni comme externe habituel :
                // par prudence, on l'ouvre à l'extérieur plutôt que de risquer
                // de piéger l'utilisateur dans un site tiers sans barre d'adresse.
                ouvrirExterne(uri)
                return true
            }

            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                swipeRefresh.isRefreshing = false
                progressBar.visibility = View.GONE
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                super.onReceivedError(view, request, error)
                if (request.isForMainFrame) {
                    afficherHorsLigne()
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {

            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progressBar.progress = newProgress
                progressBar.visibility = if (newProgress in 1..99) View.VISIBLE else View.GONE
            }

            override fun onShowFileChooser(
                webView: WebView,
                callback: ValueCallback<Array<Uri>>,
                params: FileChooserParams
            ): Boolean {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = callback
                return try {
                    fileChooserLauncher.launch(params.createIntent())
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "Sélecteur de fichier indisponible", e)
                    filePathCallback = null
                    false
                }
            }
        }

        // Téléchargement des MP3 (bouton "Télécharger" des fiches musique) via
        // le gestionnaire de téléchargements du système, comme dans un navigateur.
        webView.setDownloadListener { url, _, contentDisposition, mimeType, _ ->
            try {
                val nomFichier = URLUtil.guessFileName(url, contentDisposition, mimeType)
                val requete = DownloadManager.Request(url.toUri())
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, nomFichier)
                    .setTitle(nomFichier)
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true)
                val gestionnaire = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                gestionnaire.enqueue(requete)
                Toast.makeText(this, "Téléchargement démarré : $nomFichier", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "Échec du téléchargement", e)
                Toast.makeText(this, "Le téléchargement a échoué.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun ouvrirExterne(uri: Uri) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: Exception) {
            Toast.makeText(this, "Aucune application disponible pour ouvrir ce lien.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun estConnecte(): Boolean {
        val gestionnaire = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val reseau = gestionnaire.activeNetwork ?: return false
        val capacites = gestionnaire.getNetworkCapabilities(reseau) ?: return false
        return capacites.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun afficherHorsLigne() {
        webView.visibility = View.GONE
        offlineView.visibility = View.VISIBLE
        swipeRefresh.isRefreshing = false
        progressBar.visibility = View.GONE
    }

    override fun onDestroy() {
        webView.destroy()
        super.onDestroy()
    }
}
