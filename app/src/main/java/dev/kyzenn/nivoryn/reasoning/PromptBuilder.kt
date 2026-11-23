package dev.kyzenn.nivoryn.reasoning

import dev.kyzenn.nivoryn.domain.DemoResult

/** No imported text, quotes, numbers or destination addresses enter the native prompt/log surface. */
object PromptBuilder {
    // JSON syntax is prefilled; all explanation words must still be generated and validated.
    const val RESPONSE_PREFIX = "{\"summary\":\""
    fun build(result: DemoResult): String = buildString {
        append("<|begin_of_text|><|start_header_id|>system<|end_header_id|>\n\n")
        append("You explain security findings cautiously. Return JSON with exactly two keys: summary and actions. ")
        append("summary is one short description of the finding using could or may and an inline evidence citation [E1]. ")
        append("actions is an array of approved action IDs. Describe a risk, without giving instructions or declaring fraud. ")
        append("Use the finding vocabulary. At most 40 words. Format: {\"summary\":\"description [E1].\",\"actions\":[\"A1\"]}\n")
        append("<|eot_id|><|start_header_id|>user<|end_header_id|>\n\n")
        append("Assessment: ${result.assessment.name}\n")
        result.evidence.take(8).forEachIndexed { i, e ->
            append("E${i + 1}: ${AdvisoryFacts.describe(e.title)}\n")
        }
        if (result.evidence.isEmpty()) append(AdvisoryFacts.NO_EVIDENCE + "\n")
        append("Approved actions: ")
        append(result.nextSteps.take(6).indices.joinToString(", ") { "A${it + 1}" })
        append('\n')
        append("Return only the completed JSON.\n")
        append("<|eot_id|><|start_header_id|>assistant<|end_header_id|>\n\n")
        append(RESPONSE_PREFIX)
    }.also { require(it.length <= 6000) }
}
