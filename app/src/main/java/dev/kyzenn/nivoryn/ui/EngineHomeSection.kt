package dev.kyzenn.nivoryn.ui

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import dev.kyzenn.nivoryn.data.*
import dev.kyzenn.nivoryn.ui.components.*

@Composable fun EngineHomeSection(checks:List<SavedCheck>,onOpen:(SavedCheck)->Unit,onPage:(String)->Unit) {
    GroupedSurface("Device and network") {
        ActionRow("Network activity","Live usage totals and capture import. No VPN or traffic blocking.",{onPage("network")},glyph=7)
        HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
        ActionRow("APK inspection","Inspect a file's signing and static indicators without installing it.",{onPage("apk")},glyph=3)
    }
    GroupedSurface("Your activity") {
        if(checks.isEmpty()) Text("Nothing saved yet. Save a result when you want to keep its evidence.",style=MaterialTheme.typography.bodyMedium)
        checks.take(2).forEach { check -> ActionRow(check.preview.take(70),"Saved on this device",{onOpen(check)},glyph=5) }
        ActionRow("Checks and saved cases","Reopen, export or delete saved checks; explore your demo cases.",{onPage("cases")},glyph=5)
    }
    val ai by (LocalContext.current.applicationContext as dev.kyzenn.nivoryn.NivorynApplication).modelSetup.state.collectAsState()
    GroupedSurface("Local resources") {
        ActionRow("Set up local AI",ai.phase+" · "+ai.detail,{onPage("model")},glyph=8)
        ActionRow("Rules and indicators","Bundled rules are ready. Signed indicator updates are optional.",{onPage("rules")},glyph=3)
    }
    TextAction("Explore sample checks · example data",{onPage("samples")})
}

@Composable fun ResourcesScreen(onPage:(String)->Unit) {
    PageTitle("Models and resources","Real engine status and setup, in one place.")
    val ai by (LocalContext.current.applicationContext as dev.kyzenn.nivoryn.NivorynApplication).modelSetup.state.collectAsState()
    GroupedSurface("On-device engines") {
        ActionRow("Llama resources",ai.detail,{onPage("model")},state=ai.phase)
        ActionRow("Rules and indicators","Bundled text/link rules; optional authenticated indicator imports.",{onPage("rules")})
        Text("OCR and QR are bundled. No extra model download is needed for those checks.",style=MaterialTheme.typography.bodySmall)
    }
}
