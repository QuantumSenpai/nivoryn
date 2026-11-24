package dev.kyzenn.nivoryn.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import dev.kyzenn.nivoryn.ui.theme.*

@Composable fun NavigationGlyph(index:Int,response:Int,modifier:Modifier=Modifier) {
    val reduced=LocalReducedMotion.current
    val phase=remember { Animatable(0f) }
    LaunchedEffect(response,reduced) {
        if(response==0 || reduced) { phase.snapTo(0f); return@LaunchedEffect }
        phase.animateTo(1f,tween(Motion.Press,easing=EaseOutCustom))
        phase.animateTo(0f,tween(Motion.Exit,easing=EaseOutCustom))
    }
    Box(modifier) {
        ProductGlyph(listOf(4,1,5,6)[index],Modifier.fillMaxSize().graphicsLayer {
            if(index==3) rotationZ=phase.value*24f
            if(index==2) rotationZ=-phase.value*12f
            if(index==0) { scaleX=1f-phase.value*.06f; scaleY=scaleX }
        })
        if(index==1 && !reduced) Canvas(Modifier.fillMaxSize()) {
            val y=size.height*(.25f+.5f*phase.value)
            drawLine(BrandRed.copy(alpha=phase.value),Offset(size.width*.2f,y),Offset(size.width*.8f,y),2f)
        }
    }
}
