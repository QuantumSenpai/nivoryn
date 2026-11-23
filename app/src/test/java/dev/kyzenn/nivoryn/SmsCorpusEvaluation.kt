package dev.kyzenn.nivoryn

import dev.kyzenn.nivoryn.domain.*
import java.io.File
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale

/** Standalone evaluation entry point; not a test silently skipped when data is absent. */
object SmsCorpusEvaluation {
    @JvmStatic fun main(args: Array<String>) {
        require(args.size == 2) { "Expected corpus and report paths" }
        val file=File(args[0]); require(file.length() in 1..20_000_000)
        val rows=file.readLines(Charsets.UTF_8).map { line ->
            val pair=line.split('\t',limit=2); require(pair.size==2 && pair[0] in listOf("ham","spam"))
            (pair[0]=="spam") to pair[1]
        }
        fun key(s:String)=Normalizer.normalize(s,Normalizer.Form.NFKC).lowercase(Locale.ROOT).trim().replace(Regex("\\s+")," ")
        val groups=rows.groupBy { key(it.second) }
        val conflicts=groups.values.count { it.map { row -> row.first }.distinct().size>1 }
        val unique=groups.values.filter { it.map { row -> row.first }.distinct().size==1 }.map { it.first() }
        var tp=0; var fp=0; var tn=0; var fn=0
        val engine=AnalyseInput()
        unique.forEach { (positive,text) ->
            val verdict=engine.analyse(CheckDraft(text=text)).assessment
            val flagged=verdict in listOf(Assessment.HIGH,Assessment.SUSPICIOUS)
            when { positive && flagged -> tp++; positive -> fn++; flagged -> fp++; else -> tn++ }
        }
        val hash=MessageDigest.getInstance("SHA-256").digest(file.readBytes()).joinToString("") { "%02x".format(it) }
        fun ratio(a:Int,b:Int)=if(b==0) "null" else (a.toDouble()/b).toString()
        val output="""{
          "dataset":"UCI SMS Spam Collection / DOI 10.24432/C5CC84", "licence":"CC BY 4.0",
          "scope":"Spam proxy only; spam is not scam ground truth. No Llama inference in detector evaluation.",
          "split":"No training or tuning; all nonconflicting deduplicated rows evaluated once. No benchmark-based accuracy claim.",
          "corpusSha256":"$hash", "rawRows":${rows.size}, "evaluatedRows":${unique.size}, "conflictingDuplicateGroups":$conflicts,
          "tp":$tp, "fp":$fp, "tn":$tn, "fn":$fn, "precision":${ratio(tp,tp+fp)}, "recall":${ratio(tp,tp+fn)}
        }""".trimIndent()
        val target=File(args[1]); target.parentFile?.mkdirs(); target.writeText(output + "\n")
        println(output)
    }
}
