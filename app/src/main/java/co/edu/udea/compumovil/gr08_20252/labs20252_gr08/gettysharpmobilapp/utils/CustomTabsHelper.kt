package co.edu.udea.compumovil.gr08_20252.labs20252_gr08.gettysharpmobilapp.utils

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

object CustomTabsHelper {
    fun openUrl(context: Context, url: String) {
        val builder = CustomTabsIntent.Builder()
        builder.setShowTitle(true)
        builder.setUrlBarHidingEnabled(true)
        
        val customTabsIntent = builder.build()
        customTabsIntent.launchUrl(context, Uri.parse(url))
    }
}

