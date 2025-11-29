package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.utils

import android.content.Intent
import android.net.Uri

object DeepLinkHandler {
    fun extractOAuthCodeFromIntent(intent: Intent): OAuthParams? {
        val data: Uri? = intent.data
        return data?.let { OAuthDeepLinkHandler.extractOAuthParams(it) }
    }
}

