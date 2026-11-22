package dev.kyzenn.nivoryn.ui
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import dev.kyzenn.nivoryn.ui.components.*
@Composable fun GuidanceScreen(hi:Boolean) {
    PageTitle(if(hi)"अगला कदम, शांति से।"else"A calmer next step.",if(hi)"दबाव में तुरंत जवाब देना ज़रूरी नहीं।"else"You do not have to respond on someone else’s deadline.")
    Disclosure(if(hi)"बैंक, KYC और OTP"else"Banking, KYC and OTPs",if(hi)"OTP, PIN या पासवर्ड साझा न करें। संदेश के लिंक की जगह बैंक का परिचित ऐप या विश्वसनीय संपर्क खोलें।"else"Keep OTPs, PINs and passwords private. Use your bank’s known app or contact details instead of a message link.",true)
    Disclosure(if(hi)"धमकी या डिजिटल अरेस्ट"else"Threats and digital-arrest pressure",if(hi)"रुकें और विश्वसनीय व्यक्ति से बात करें। संदेश में दिए नंबर से अलग, आधिकारिक माध्यम से सत्यापन करें।"else"Pause and speak to someone you trust. Verify a notice independently through an official channel, separate from the contact pressuring you.")
    Disclosure(if(hi)"काम या भुगतान का प्रस्ताव"else"Task offers and payment requests",if(hi)"कमाई पाने से पहले जमा राशि माँगने पर रुकें। पहचान स्वतंत्र रूप से सत्यापित करें। QR अपने आप पहचान की पुष्टि नहीं करता।"else"Pause when money is required before earnings can be released. Verify the organisation independently. A QR code does not establish who owns a payment address.")
    SectionTitle(if(hi)"अगर पैसे चले गए हैं"else"If money has already moved")
    Text(if(hi)"अपने बैंक या भुगतान प्रदाता से उसके आधिकारिक माध्यम से तुरंत संपर्क करें। लेन-देन ID, समय और संबंधित संदेश रखें। और पैसे भेजकर नुकसान वसूलने के वादों से सावधान रहें।"else"Contact your bank or payment provider promptly through its official channel. Keep transaction IDs, times and relevant messages. Be cautious of anyone asking for more money to recover a loss.")
    SectionTitle(if(hi)"रिकवरी सूची"else"A practical recovery checklist")
    Text(if(hi)"• आगे भुगतान रोकें।\n• संबंधित संदेश और रसीदें रखें।\n• साझा किए पासवर्ड विश्वसनीय डिवाइस से बदलें।\n• बैंक और आधिकारिक रिपोर्टिंग विकल्प से संपर्क करें।"else"• Pause further payments.\n• Preserve relevant messages and receipts.\n• Change exposed passwords from a trusted device.\n• Contact the bank and use an official reporting channel.")
    Notice(if(hi)"नीचे का लिंक केवल आपके दबाने पर ब्राउज़र में खुलता है। इंटरनेट ज़रूरी है। ऐप कॉल, रिपोर्ट या संदेश अपने आप नहीं भेजता।"else"The official portal below opens in your browser only when selected. Internet is required. This app never calls, sends or reports automatically.")
    ExternalLink(if(hi)"राष्ट्रीय साइबर अपराध पोर्टल खोलें"else"Open the National Cyber Crime portal","https://cybercrime.gov.in/")
}
