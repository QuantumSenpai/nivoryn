package dev.kyzenn.nivoryn.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import dev.kyzenn.nivoryn.ui.components.ProductGlyph
import dev.kyzenn.nivoryn.ui.theme.*

@Composable fun LocalDashboard(hi:Boolean,kind:String,onKind:(String)->Unit,onImport:(String)->Unit,compact:Boolean=true) {
    val modes=listOf("Link","Text","Screenshot","Document")
    val labels=if(hi) listOf("लिंक","संदेश","स्कैन","दस्तावेज़") else listOf("Link","Message","Scan / OCR","Document")
    val reduce=LocalReducedMotion.current
    BoxWithConstraints(Modifier.fillMaxWidth().semantics { isTraversalGroup=true }) {
        val fontScale=androidx.compose.ui.platform.LocalDensity.current.fontScale
        val columns=if(compact && maxWidth>=280.dp && fontScale<=1.3f) 4 else 2
        val width=(maxWidth-8.dp*(columns-1))/columns
        val tileHeight=(if(compact) 82.dp else 94.dp)*maxOf(1f,fontScale*.9f)
        val height by animateDpAsState(tileHeight*((4+columns-1)/columns)+8.dp*((4+columns-1)/columns-1),tween(if(reduce) 0 else Motion.Enter,easing=EaseInOutCustom),label="selector bounds")
        Box(Modifier.fillMaxWidth().height(height)) {
            modes.forEachIndexed { index,mode -> key(mode) {
                val x by animateDpAsState((width+8.dp)*(index%columns),tween(if(reduce) 0 else Motion.Enter,easing=EaseInOutCustom),label="feature position x")
                val y by animateDpAsState((tileHeight+8.dp)*(index/columns),tween(if(reduce) 0 else Motion.Enter,easing=EaseInOutCustom),label="feature position y")
                val w by animateDpAsState(width,tween(if(reduce) 0 else Motion.Enter,easing=EaseInOutCustom),label="feature width")
                val active=kind==mode || (mode=="Screenshot" && kind=="QR")
                val tone by animateColorAsState(if(active) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainer,tween(if(reduce) 0 else Motion.Press),label="feature tone")
                Surface(onClick={ if(mode=="Screenshot") onImport(mode) else onKind(mode) },
                    modifier=Modifier.offset { IntOffset(x.roundToPx(),y.roundToPx()) }.width(w).heightIn(min=tileHeight).semantics {
                        role=Role.RadioButton; selected=active; stateDescription=if(active) (if(hi) "चयनित" else "Selected") else ""
                    },shape=MaterialTheme.shapes.small,color=tone,
                    border=BorderStroke(if(active) 2.dp else 1.dp,if(active) BrandRed else MaterialTheme.colorScheme.outlineVariant)) {
                    Column(Modifier.padding(8.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(8.dp)) {
                        ProductGlyph(index,Modifier.size(24.dp))
                        Text(labels[index],style=MaterialTheme.typography.labelSmall)
                    }
                }
            } }
        }
    }
}
