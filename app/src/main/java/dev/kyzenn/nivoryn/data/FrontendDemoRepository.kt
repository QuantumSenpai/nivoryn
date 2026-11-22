package dev.kyzenn.nivoryn.data

import dev.kyzenn.nivoryn.domain.*

/** Fixed, authored examples. This boundary never classifies arbitrary user content. */
class FrontendDemoRepository : DemoRepository {
    private fun sample(id: String, title: String, hi: String, text: String, kind: String,
        assessment: Assessment, explanation: String, explanationHi: String,
        quote: String = "", evidenceTitle: String = "", evidenceBody: String = "",
        next: String = "Pause. Verify through a contact or app you already trust.") = DemoSample(
        id, title, hi, text, kind, DemoResult(assessment, explanation, explanationHi,
            if (quote.isEmpty()) emptyList() else listOf(Evidence(evidenceTitle, quote, evidenceBody)),
            listOf("Authored example and supporting phrases displayed", "No link opened or payment initiated"),
            listOf("Real detection engine", "Live reputation and identity checks", "On-device AI explanation"),
            listOf("Sender identity", "Claims and recipient ownership", "Current booking or account status"),
            listOf(next, "Keep relevant messages if you need to report the incident.")))
    override val samples = listOf(
        sample("kyc", "The urgent KYC message", "तुरंत KYC का संदेश", "Your bank account will be blocked today. Update KYC now at https://bank-verify.example/kyc. Share your OTP to continue.", "Text", Assessment.HIGH,
            "This example combines a deadline, an unfamiliar link and a request for an OTP.", "इस उदाहरण में समय का दबाव, अनजान लिंक और OTP की माँग है।", "Share your OTP", "A secret is requested", "An OTP can authorise access or a transaction. Do not share it.", "Do not share the OTP. Contact your bank through its official app or a known number."),
        sample("arrest", "A digital-arrest threat", "डिजिटल अरेस्ट की धमकी", "Police notice: you are under digital arrest. Stay on video. Transfer ₹50,000 to avoid prosecution. Do not tell your family.", "Text", Assessment.HIGH,
            "Payment pressure and isolation are the warning evidence in this authored example.", "इस उदाहरण में पैसे का दबाव और परिवार से बात न करने की धमकी चेतावनी हैं।", "Do not tell your family", "Isolation and pressure", "The message tries to prevent independent verification.", "End the conversation and contact someone you trust. Verify any notice independently."),
        sample("task", "Pay to unlock earnings", "कमाई पाने के लिए भुगतान", "Part-time task offer. Join our group to start. Pay a fee to release your earnings.", "Text", Assessment.HIGH,
            "The fee request makes payment a condition of receiving promised earnings.", "बाद में जमा राशि माँगने से सामान्य प्रस्ताव का संदर्भ बदल जाता है।", "Pay a fee to release your earnings.", "Upfront payment pressure", "The payment is made a condition of receiving promised income.", "Pause the payment. Verify the employer independently."),
        sample("link", "A familiar name, another host", "नाम परिचित, होस्ट अलग", "Display name: Your Bank. Actual URL: https://your-bank.support-check.example/login", "Link", Assessment.SUSPICIOUS,
            "The displayed bank name does not establish who owns the actual URL host.", "दिखने वाला बैंक का नाम URL के मालिक की पुष्टि नहीं करता।", "your-bank.support-check.example", "Read the actual host", "The host is under support-check.example; the familiar words are only part of the name."),
        sample("qr", "A QR payment request", "QR भुगतान अनुरोध", "upi://pay?pa=example-merchant@upi&pn=Demo%20Merchant&am=999&cu=INR", "QR", Assessment.SUSPICIOUS,
            "This example requests ₹999. A QR payload alone cannot verify the recipient or the reason to pay.", "यह उदाहरण ₹999 का भुगतान माँगता है। QR से प्राप्तकर्ता की पहचान सत्यापित नहीं होती।", "am=999", "A payment, not a receipt", "The sample payload asks for an outgoing payment.", "Read the payee and amount in your trusted payment app before deciding. This demo does not initiate payment."),
        sample("ticket", "A ticket, still unverified", "टिकट की स्थिति अप्रमाणित", "Example ticket\nPassenger: Asha Demo\nJourney: Pune → Mumbai\nTravel date: 24 Nov 2026\nBooking ID: DEMO-4821\nBooking status: unverified", "Document", Assessment.UNKNOWN,
            "Readable ticket fields do not confirm a booking. No signature or booking-system check has run.", "टिकट की जानकारी पढ़ने से बुकिंग की पुष्टि नहीं होती। हस्ताक्षर या बुकिंग जाँच नहीं हुई है।", "Booking status: unverified", "Booking status unverified", "An absent signature does not establish forgery.", "Verify the booking with the operator using a known official channel."),
        sample("legitimate", "An ordinary reminder", "सामान्य अनुस्मारक", "Reminder: the residents’ meeting is at 6 pm in the community hall. No payment is needed.", "Text", Assessment.NONE,
            "This authored example contains no strong warning evidence. That is not a guarantee of safety.", "इस उदाहरण में स्पष्ट चेतावनी नहीं है। यह सुरक्षा की गारंटी नहीं है।", next = "Use the context you already know. Verify unexpected changes independently."),
        sample("insufficient", "Too little to go on", "जानकारी पर्याप्त नहीं", "Hello", "Text", Assessment.UNKNOWN,
            "This example has too little context for a useful assessment.", "उपयोगी आकलन के लिए इस उदाहरण में पर्याप्त जानकारी नहीं है।", next = "Ask for more context without sharing private information.")
    )
    override fun resultFor(draft: CheckDraft): DemoResult? = samples
        .firstOrNull { it.id == draft.sampleId && it.text == draft.text && draft.attachmentUri.isEmpty() }?.result
}
