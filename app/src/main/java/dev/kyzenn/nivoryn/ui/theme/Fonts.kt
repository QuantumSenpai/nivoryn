package dev.kyzenn.nivoryn.ui.theme

import android.os.Build
import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.core.content.res.ResourcesCompat
import dev.kyzenn.nivoryn.R

@Composable fun bundledFontFamily(hindi:Boolean): FontFamily {
    val context=LocalContext.current
    val resources=LocalResources.current
    return remember(resources,hindi) {
        if(Build.VERSION.SDK_INT>=29) {
            val latin=android.graphics.fonts.FontFamily.Builder(android.graphics.fonts.Font.Builder(resources,R.font.dm_sans).build()).build()
            val deva=android.graphics.fonts.FontFamily.Builder(android.graphics.fonts.Font.Builder(resources,R.font.noto_devanagari).build()).build()
            FontFamily(Typeface.CustomFallbackBuilder(latin).addCustomFallback(deva).setSystemFallback("sans-serif").build())
        } else FontFamily(ResourcesCompat.getFont(context,if(hindi) R.font.noto_devanagari else R.font.dm_sans)!!)
    }
}
