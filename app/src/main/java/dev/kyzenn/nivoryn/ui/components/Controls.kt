package dev.kyzenn.nivoryn.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import dev.kyzenn.nivoryn.ui.theme.*

@Composable fun ActionButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier, secondary: Boolean = false, enabled: Boolean = true) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val reduced = LocalReducedMotion.current
    val scale by animateFloatAsState(if(pressed && !reduced) .98f else 1f, tween(Motion.Press, easing=EaseOutCustom), label="press feedback")
    val m = modifier.heightIn(min=48.dp).graphicsLayer { scaleX=scale; scaleY=scale }
    if (secondary) OutlinedButton(onClick, m, enabled=enabled, shape=RoundedCornerShape(6.dp), interactionSource=interaction) { Text(label) }
    else Button(onClick, m, enabled=enabled, shape=RoundedCornerShape(6.dp), interactionSource=interaction) { Text(label) }
}
@Composable fun TextAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick, modifier.heightIn(min=48.dp), shape=RoundedCornerShape(6.dp)) { Text(label, color=MaterialTheme.colorScheme.secondary) }
}
@Composable fun PageTitle(title: String, subtitle: String = "") {
    Text(title, style=MaterialTheme.typography.headlineMedium, modifier=Modifier.semantics { heading() })
    if(subtitle.isNotEmpty()) Text(subtitle, color=MaterialTheme.colorScheme.onSurfaceVariant)
}
@Composable fun SectionTitle(title: String) { Text(title, style=MaterialTheme.typography.titleMedium, modifier=Modifier.padding(top=12.dp).semantics { heading() }) }
@Composable fun Notice(text: String) {
    Surface(color=MaterialTheme.colorScheme.surfaceVariant, shape=RoundedCornerShape(6.dp)) {
        Text(text, Modifier.padding(14.dp), style=MaterialTheme.typography.bodyMedium)
    }
}
@Composable fun FormField(value: String, label: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, multiline: Boolean = false) {
    OutlinedTextField(value, onChange, modifier.fillMaxWidth(), label={ Text(label) }, minLines=if(multiline) 4 else 1,
        maxLines=if(multiline) 12 else 1, shape=RoundedCornerShape(6.dp), singleLine=!multiline)
}
@Composable fun Disclosure(title: String, text: String, initiallyOpen: Boolean = false) {
    var open by rememberSaveable(title) { mutableStateOf(initiallyOpen) }
    val reduced=LocalReducedMotion.current
    val angle by animateFloatAsState(if(open) 180f else 0f, tween(if(reduced) 0 else Motion.Disclosure), label="disclosure arrow")
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().heightIn(min=52.dp).clickable(role=Role.Button) { open=!open }.semantics { stateDescription=if(open) "Expanded" else "Collapsed" }.padding(vertical=12.dp), horizontalArrangement=Arrangement.SpaceBetween) {
            Text(title, Modifier.weight(1f), style=MaterialTheme.typography.titleMedium)
            Text("⌄", Modifier.padding(horizontal=12.dp).graphicsLayer { rotationZ=angle })
        }
        AnimatedVisibility(open, enter=expandVertically(tween(if(reduced) 0 else 200))+fadeIn(), exit=shrinkVertically(tween(if(reduced) 0 else 180))+fadeOut()) {
            Text(text, Modifier.padding(bottom=16.dp), style=MaterialTheme.typography.bodyLarge)
        }
        HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
    }
}
@Composable fun SettingSwitch(title: String, detail: String, checked: Boolean, onChange: (Boolean)->Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical=8.dp), horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        Column(Modifier.weight(1f)) { Text(title); Text(detail, style=MaterialTheme.typography.bodyMedium, color=MaterialTheme.colorScheme.onSurfaceVariant) }
        Switch(checked,onChange, modifier=Modifier.semantics { contentDescription=title; stateDescription=if(checked) "On" else "Off" })
    }
}

/** Shared original outline family. Icons supplement, never replace, visible labels. */
@Composable fun ProductGlyph(kind:Int,modifier:Modifier=Modifier) {
    val ink=MaterialTheme.colorScheme.onSurface
    androidx.compose.foundation.Canvas(modifier) {
        val stroke=1.75.dp.toPx()
        val w=size.width;val h=size.height
        fun line(x1:Float,y1:Float,x2:Float,y2:Float)=drawLine(ink,androidx.compose.ui.geometry.Offset(x1*w,y1*h),androidx.compose.ui.geometry.Offset(x2*w,y2*h),strokeWidth=stroke,cap=androidx.compose.ui.graphics.StrokeCap.Round)
        when(kind) {
            0 -> { line(.15f,.55f,.45f,.25f);line(.45f,.25f,.7f,.25f);line(.55f,.75f,.85f,.45f);line(.3f,.75f,.55f,.75f);line(.35f,.65f,.65f,.35f) }
            1 -> { line(.15f,.2f,.85f,.2f);line(.85f,.2f,.85f,.7f);line(.85f,.7f,.4f,.7f);line(.4f,.7f,.2f,.85f);line(.2f,.85f,.2f,.7f);line(.15f,.7f,.15f,.2f);line(.32f,.4f,.68f,.4f);line(.32f,.54f,.57f,.54f) }
            2 -> { drawRect(ink,androidx.compose.ui.geometry.Offset(.18f*w,.14f*h),androidx.compose.ui.geometry.Size(.64f*w,.72f*h),style=androidx.compose.ui.graphics.drawscope.Stroke(stroke));line(.28f,.65f,.45f,.45f);line(.45f,.45f,.71f,.7f) }
            3 -> { line(.25f,.12f,.6f,.12f);line(.6f,.12f,.8f,.32f);line(.8f,.32f,.8f,.88f);line(.8f,.88f,.25f,.88f);line(.25f,.88f,.25f,.12f);line(.6f,.12f,.6f,.32f);line(.6f,.32f,.8f,.32f);line(.37f,.52f,.65f,.52f);line(.37f,.68f,.6f,.68f) }
            4 -> { line(.12f,.45f,.5f,.12f);line(.5f,.12f,.88f,.45f);line(.23f,.4f,.23f,.86f);line(.23f,.86f,.77f,.86f);line(.77f,.86f,.77f,.4f);line(.43f,.86f,.43f,.6f);line(.43f,.6f,.6f,.6f);line(.6f,.6f,.6f,.86f) }
            5 -> { drawCircle(ink,.34f*w,androidx.compose.ui.geometry.Offset(.5f*w,.5f*h),style=androidx.compose.ui.graphics.drawscope.Stroke(stroke));line(.5f,.28f,.5f,.53f);line(.5f,.53f,.68f,.62f) }
            else -> { drawCircle(ink,.3f*w,androidx.compose.ui.geometry.Offset(.5f*w,.5f*h),style=androidx.compose.ui.graphics.drawscope.Stroke(stroke));drawCircle(ink,.08f*w,androidx.compose.ui.geometry.Offset(.5f*w,.5f*h),style=androidx.compose.ui.graphics.drawscope.Stroke(stroke));line(.5f,.05f,.5f,.2f);line(.5f,.8f,.5f,.95f);line(.05f,.5f,.2f,.5f);line(.8f,.5f,.95f,.5f) }
        }
    }
}
