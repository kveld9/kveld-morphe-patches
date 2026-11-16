package app.morphe.patches.gboard

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.instructionsOrNull
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.BytecodePatchContext
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.Constants
import app.morphe.patches.shared.cleanClassName
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference

val gboardBlockTelemetryPatch = bytecodePatch(
    name = "Block Trackers",
    description = "Stops background metrics, event logging and crash reporting.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    val blockAdwordsHost by booleanOption(
        key = "blockAdwordsHost",
        default = true,
        title = "Block ad servers",
        description = "Also blocks Google ad servers. Turn off to keep them.",
        required = false,
    )

    execute {
        val hookedMethods = mutableListOf<String>()

        // 1. Clearcut & Event Telemetry (lix, hbt, jfk)
        listOf(
            "n" to listOf("Llit;"),
            "q" to emptyList(),
            "t" to emptyList(),
        ).forEach { (methodName, params) ->
            val fp = Fingerprint(
                definingClass = "Llix;",
                name = methodName,
                parameters = params,
                returnType = "V",
            )
            fp.method.addInstructions(0, "return-void")
            val c = cleanClassName(fp.originalClassDef.type)
            hookedMethods.add("$c.$methodName")
        }

        val fpHcg = Fingerprint(
            definingClass = "Lhbt;",
            name = "b",
            parameters = listOf("Lhdl;"),
            returnType = "V",
        )
        fpHcg.method.addInstructions(0, "return-void")
        val cHcg = cleanClassName(fpHcg.originalClassDef.type)
        hookedMethods.add("$cHcg.b")

        val fpJga = Fingerprint(
            definingClass = "Ljfk;",
            name = "eS",
            parameters = listOf("Landroid/content/Context;", "Llko;"),
            returnType = "V",
        )
        fpJga.method.addInstructions(0, "return-void")
        val cJga = cleanClassName(fpJga.originalClassDef.type)
        hookedMethods.add("$cJga.eS")

        // 2. Daily Ping Worker (DailyPingWorker.c)
        val fpDailyPing = Fingerprint(
            definingClass = "Lcom/google/android/libraries/inputmethod/dailyping/DailyPingWorker;",
            name = "c",
            parameters = emptyList(),
        )
        fpDailyPing.method.addInstructions(
            0,
            """
                new-instance v0, Lbeq;
                invoke-direct {v0}, Lbeq;-><init>()V
                new-instance v1, Lrjs;
                invoke-direct {v1, v0}, Lrjs;-><init>(Ljava/lang/Object;)V
                return-object v1
            """.trimIndent(),
        )
        hookedMethods.add("DailyPingWorker.c")

        // 3. Google Primes & Crash Diagnostics (LifeboatReceiver, lyu, NativeCrashHandlerImpl)
        // Note (18.5.1/18.6.3): the legacy Primes client singleton (acud) and provider (aaka)
        // are gone/inlined upstream; coverage retained via Clearcut logger, LifeboatReceiver,
        // PrimesModule (lyu) and NativeCrashHandlerImpl.
        val fpLifeboat = Fingerprint(
            definingClass = "Lcom/google/android/libraries/performance/primes/transmitter/LifeboatReceiver;",
            name = "onReceive",
            parameters = listOf("Landroid/content/Context;", "Landroid/content/Intent;"),
            returnType = "V",
        )
        fpLifeboat.method.addInstructions(0, "return-void")
        hookedMethods.add("LifeboatReceiver.onReceive")

        val fpLxd = Fingerprint(
            definingClass = "Llyu;",
            name = "eS",
            parameters = listOf("Landroid/content/Context;", "Llko;"),
            returnType = "V",
        )
        fpLxd.method.addInstructions(0, "return-void")
        val cLxd = cleanClassName(fpLxd.originalClassDef.type)
        hookedMethods.add("$cLxd.eS")

        val fpCrash = Fingerprint(
            definingClass = "Lcom/google/android/libraries/performance/primes/metrics/crash/NativeCrashHandlerImpl;",
            name = "a",
            parameters = listOf("Loyk;"),
            returnType = "V",
        )
        fpCrash.method.addInstructions(0, "return-void")
        hookedMethods.add("NativeCrashHandlerImpl.a")

        // 4. AppDoctor Diagnostics (AppDoctorReceiver)
        val fpAppDoctorRecv = Fingerprint(
            definingClass = "Lcom/google/android/libraries/appdoctor/AppDoctorReceiver;",
            name = "onReceive",
            parameters = listOf("Landroid/content/Context;", "Landroid/content/Intent;"),
            returnType = "V",
        )
        fpAppDoctorRecv.method.addInstructions(0, "return-void")
        hookedMethods.add("AppDoctorReceiver.onReceive")

        // 5. Tenor Share Tracking (Lehl.O event 328)
        val fpTenor = Fingerprint(
            definingClass = "Lehl;",
            name = "O",
            parameters = listOf("Lsfs;", "I"),
            returnType = "V",
        )
        fpTenor.method.addInstructions(
            0,
            """
                const/16 v0, 328
                if-ne p2, v0, :cond_skip_morphe_tenor
                return-void
                :cond_skip_morphe_tenor
            """.trimIndent(),
        )
        val cTenor = cleanClassName(fpTenor.originalClassDef.type)
        hookedMethods.add("$cTenor.O")

        val targetClasses = hookedMethods.map { it.substringBefore('.') }.distinct()

        if (blockAdwordsHost == true) {
            blockAdwordsHostInDex()
        } else {
            println("[Block Trackers] Skipped blockAdwordsHost: option is disabled.")
        }

        if (hookedMethods.isEmpty()) {
            println("[Block Trackers] WARNING: applied 0 hook(s) -> this feature is NOT active on this APK; its targets moved upstream.")
        } else {
            println("[Block Trackers] Injected Smali hooks into ${hookedMethods.size} telemetry & diagnostic methods across ${targetClasses.size} classes (${targetClasses.joinToString(", ")})")
        }
    }
}

private data class PendingAdwordsRewrite(
    val index: Int,
    val register: Int,
    val replacement: String,
)

private fun BytecodePatchContext.blockAdwordsHostInDex() {
    var rewrittenStrings = 0
    var touchedClasses = 0

    classDefForEach { classDef ->
        if (!hasAdwordsLiteral(classDef)) return@classDefForEach

        val mutableClass = mutableClassDefBy(classDef)
        var classModified = false

        for (method in mutableClass.methods) {
            val count = rewriteAdwordsInMethod(method)
            if (count > 0) {
                rewrittenStrings += count
                classModified = true
            }
        }

        if (classModified) touchedClasses++
    }

    logAdwordsResult(rewrittenStrings, touchedClasses)
}

private fun logAdwordsResult(rewrittenStrings: Int, touchedClasses: Int) {
    if (rewrittenStrings == 0) {
        println("[Block Trackers] No adwords.google.com or reportingwidget.google.com literals found.")
    } else {
        println("[Block Trackers] Rewrote $rewrittenStrings adwords.google.com / reportingwidget.google.com literal(s) across $touchedClasses class(es) -> 0.0.0.0.")
    }
}

private fun rewriteAdwordsInMethod(method: MutableMethod): Int {
    val rewrites = collectAdwordsRewrites(method)
    if (rewrites.isEmpty()) return 0
    applyAdwordsRewrites(method, rewrites)
    return rewrites.size
}

private fun hasAdwordsLiteral(classDef: ClassDef): Boolean =
    classDef.methods.any { methodHasAdwordsLiteral(it) }

private fun methodHasAdwordsLiteral(method: Method): Boolean {
    val instructions = method.instructionsOrNull ?: return false
    return instructions.any { isAdwordsConstString(it) }
}

private fun isConstStringOpcode(opcode: Opcode): Boolean =
    opcode == Opcode.CONST_STRING || opcode == Opcode.CONST_STRING_JUMBO

private fun extractAdwordsLiteral(instruction: Instruction): String? {
    if (!isConstStringOpcode(instruction.opcode)) return null
    val ref = (instruction as? ReferenceInstruction)?.reference as? StringReference ?: return null
    val original = ref.string
    return if (original.contains("adwords.google.com", ignoreCase = true) || original.contains("reportingwidget.google.com", ignoreCase = true)) original else null
}

private fun isAdwordsConstString(instruction: Instruction): Boolean =
    extractAdwordsLiteral(instruction) != null

private fun buildAdwordsRewrite(instruction: Instruction, index: Int): PendingAdwordsRewrite? {
    val original = extractAdwordsLiteral(instruction) ?: return null
    var replacement = original.replace("adwords.google.com", "0.0.0.0", ignoreCase = true)
    replacement = replacement.replace("reportingwidget.google.com", "0.0.0.0", ignoreCase = true)
    val register = (instruction as? OneRegisterInstruction)?.registerA ?: return null
    return PendingAdwordsRewrite(index, register, replacement)
}

private fun collectAdwordsRewrites(method: MutableMethod): List<PendingAdwordsRewrite> {
    val instructions = method.instructionsOrNull?.toList() ?: return emptyList()
    val rewrites = mutableListOf<PendingAdwordsRewrite>()
    for ((index, instruction) in instructions.withIndex()) {
        val rewrite = buildAdwordsRewrite(instruction, index) ?: continue
        rewrites.add(rewrite)
    }
    return rewrites
}

private fun applyAdwordsRewrites(method: MutableMethod, rewrites: List<PendingAdwordsRewrite>) {
    for (rewrite in rewrites.sortedByDescending { it.index }) {
        val opcode = if (rewrite.register > 255) "const-string/jumbo" else "const-string"
        method.replaceInstruction(
            rewrite.index,
            "$opcode v${rewrite.register}, \"${escapeSmaliLiteral(rewrite.replacement)}\"",
        )
    }
}

private fun escapeSmaliLiteral(value: String): String =
    value.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
