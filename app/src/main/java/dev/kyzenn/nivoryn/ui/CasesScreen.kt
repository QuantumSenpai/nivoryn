package dev.kyzenn.nivoryn.ui
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.kyzenn.nivoryn.domain.*
import dev.kyzenn.nivoryn.ui.components.*

@Composable fun CasesScreen(cases:List<DemoCase>,hi:Boolean,onOpen:(String)->Unit,onCreate:()->Unit,onSample:()->Unit) {
    SectionTitle(if(hi)"उदाहरण टाइमलाइन"else"Example timelines")
    if(cases.isEmpty()) { SectionTitle(if(hi)"अभी कोई केस नहीं"else"No saved cases yet");Text(if(hi)"एक डेमो केस बनाएँ या उदाहरण टाइमलाइन देखें।"else"Create a demo case or explore how a later message changes a story.") }
    cases.forEach { case ->
        TextAction(case.name,{onOpen(case.id)})
        Text("${case.sampleIds.size} ${if(hi)"उदाहरण · स्थानीय डेमो"else"examples · saved demo data"}",style=MaterialTheme.typography.bodyMedium)
        HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
    }
    ActionButton(if(hi)"डेमो केस बनाएँ"else"Create a demo case",onCreate)
    TextAction(if(hi)"Scam Story का उदाहरण देखें"else"Explore a sample Scam Story",onSample)
    Notice(if(hi)"अस्थायी ड्राफ़्ट और निजी फ़ाइलें केस में अपने आप नहीं सहेजी जातीं।"else"Temporary drafts and private files are never automatically added to a case.")
}
@Composable fun CaseDetail(case:DemoCase,samples:List<DemoSample>,hi:Boolean,onRename:()->Unit,onDelete:()->Unit,onAdd:()->Unit,onRemove:(String)->Unit,onSample:(DemoSample)->Unit) {
    PageTitle(case.name,if(hi)"सहेजी हुई डेमो टाइमलाइन · जोड़ने का क्रम"else"Saved demo timeline · in the order you added items")
    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) { TextAction(if(hi)"नाम बदलें"else"Rename",onRename);TextAction(if(hi)"केस हटाएँ"else"Delete case",onDelete) }
    if(case.sampleIds.isEmpty()) Text(if(hi)"अभी कोई संबंधित उदाहरण नहीं।"else"No related examples yet. Add an item to begin a timeline.")
    case.sampleIds.forEachIndexed { index,id -> samples.find{it.id==id}?.let { sample ->
        Column(Modifier.fillMaxWidth().padding(start=12.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
            SectionTitle("${index+1}. ${if(hi)sample.titleHi else sample.title}")
            if(case.id=="sample-story" && id=="task") {
                listOf("First message" to "Part-time task offer.", "Next message" to "Join our group to start.", "Later message" to "Deposit ₹999 to release your earnings.").forEach { (whenSent,message) ->
                    Text(whenSent,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("“$message”")
                }
            } else Text(sample.text)
            if(id=="task") Notice(if(hi)"बाद का भुगतान अनुरोध संदर्भ बदलता है: कमाई पाने से पहले जमा राशि माँगी गई है। यह लिखी हुई कहानी है, स्वचालित संबंध पहचान नहीं।"else"The later payment request changes the story: money is now required before earnings can be released. This is an authored example, not automatic case linking.")
            Row { TextAction(if(hi)"उदाहरण खोलें"else"Open example",{onSample(sample)});TextAction(if(hi)"हटाएँ"else"Remove",{onRemove(id)}) }
            HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
        }
    } }
    ActionButton(if(hi)"उदाहरण जोड़ें"else"Add a related example",onAdd,secondary=true)
}
