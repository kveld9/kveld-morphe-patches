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
    name = "Block Telemetry",
    description = "Disables background metrics dispatch, event logging, daily pings, Google Primes profiling, crash reporting, AppDoctor diagnostics, and Tenor share tracking.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    val blockAdwordsHost by booleanOption(
        key = "blockAdwordsHost",
        default = true,
        title = "Block Adwords Host",
        description = "Rewrites DEX const-string literals containing adwords.google.com and reportingwidget.google.com to 0.0.0.0. Enabled by default.",
        required = false,
    )

    execute {
        val hookedMethods = mutableListOf<String>()

        // 1. Clearcut & Event Telemetry (vze, olo, shc)
        listOf("n", "p", "s").forEach { methodName ->
            val fp = Fingerprint(
                definingClass = "Lvze;",
                name = methodName,
                parameters = if (methodName == "n") listOf("Lvyz;") else emptyList(),
                returnType = "V",
            )
            fp.method.addInstructions(0, "return-void")
            val c = cleanClassName(fp.originalClassDef.type)
            hookedMethods.add("$c.$methodName")
        }

        val fpHcg = Fingerprint(
            definingClass = "Lolo;",
            name = "b",
            parameters = listOf("Looe;"),
            returnType = "V",
        )
        fpHcg.method.addInstructions(0, "return-void")
        val cHcg = cleanClassName(fpHcg.originalClassDef.type)
        hookedMethods.add("$cHcg.b")

        val fpJga = Fingerprint(
            definingClass = "Lshc;",
            name = "dD",
            parameters = listOf("Landroid/content/Context;", "Lwbw;"),
            returnType = "V",
        )
        fpJga.method.addInstructions(0, "return-void")
        val cJga = cleanClassName(fpJga.originalClassDef.type)
        hookedMethods.add("$cJga.dD")

        // 2. Daily Ping Worker (DailyPingWorker.c)
        val fpDailyPing = Fingerprint(
            definingClass = "Lcom/google/android/libraries/inputmethod/dailyping/DailyPingWorker;",
            name = "c",
            parameters = emptyList(),
        )
        fpDailyPing.method.addInstructions(
            0,
            """
                new-instance v0, Lcim;
                invoke-direct {v0}, Lcim;-><init>()V
                invoke-static {v0}, Lahce;->i(Ljava/lang/Object;)Lahcv;
                move-result-object v0
                return-object v0
            """.trimIndent(),
        )
        hookedMethods.add("DailyPingWorker.c")

        // 3. Google Primes & Crash Diagnostics (LifeboatReceiver, xam, acud, NativeCrashHandlerImpl, aabu)
        val fpLifeboat = Fingerprint(
            definingClass = "Lcom/google/android/libraries/performance/primes/transmitter/LifeboatReceiver;",
            name = "onReceive",
            parameters = listOf("Landroid/content/Context;", "Landroid/content/Intent;"),
            returnType = "V",
        )
        fpLifeboat.method.addInstructions(0, "return-void")
        hookedMethods.add("LifeboatReceiver.onReceive")

        val fpLxd = Fingerprint(
            definingClass = "Lxam;",
            name = "dD",
            parameters = listOf("Landroid/content/Context;", "Lwbw;"),
            returnType = "V",
        )
        fpLxd.method.addInstructions(0, "return-void")
        val cLxd = cleanClassName(fpLxd.originalClassDef.type)
        hookedMethods.add("$cLxd.dD")

        val fpOrc = Fingerprint(
            definingClass = "Lacud;",
            name = "b",
            parameters = listOf("Lacud;"),
            returnType = "V",
        )
        fpOrc.method.addInstructions(0, "return-void")
        val cOrc = cleanClassName(fpOrc.originalClassDef.type)
        hookedMethods.add("$cOrc.b")

        val fpCrash = Fingerprint(
            definingClass = "Lcom/google/android/libraries/performance/primes/metrics/crash/NativeCrashHandlerImpl;",
            name = "a",
            parameters = listOf("Lades;"),
            returnType = "V",
        )
        fpCrash.method.addInstructions(0, "return-void")
        hookedMethods.add("NativeCrashHandlerImpl.a")

        val fpNjv = Fingerprint(
            definingClass = "Laabu;",
            name = "get",
            parameters = emptyList(),
            returnType = "Ljava/lang/Object;",
        )
        fpNjv.method.addInstructions(
            0,
            """
                invoke-static {}, Landroid/os/Looper;->getMainLooper()Landroid/os/Looper;
                move-result-object v0
                new-instance v1, Landroid/os/Handler;
                invoke-direct {v1, v0}, Landroid/os/Handler;-><init>(Landroid/os/Looper;)V
                return-object v1
            """.trimIndent(),
        )
        val cNjv = cleanClassName(fpNjv.originalClassDef.type)
        hookedMethods.add("$cNjv.get")

        // 4. AppDoctor Diagnostics (AppDoctorReceiver)
        val fpAppDoctorRecv = Fingerprint(
            definingClass = "Lcom/google/android/libraries/appdoctor/AppDoctorReceiver;",
            name = "onReceive",
            parameters = listOf("Landroid/content/Context;", "Landroid/content/Intent;"),
            returnType = "V",
        )
        fpAppDoctorRecv.method.addInstructions(0, "return-void")
        hookedMethods.add("AppDoctorReceiver.onReceive")

        // 5. Tenor Share Tracking (ioe.F)
        val fpTenor = Fingerprint(
            definingClass = "Lioe;",
            name = "F",
            parameters = listOf("Laglm;"),
            returnType = "V",
        )
        fpTenor.method.addInstructions(0, "return-void")
        val cTenor = cleanClassName(fpTenor.originalClassDef.type)
        hookedMethods.add("$cTenor.F")

        val targetClasses = hookedMethods.map { it.substringBefore('.') }.distinct()

        if (blockAdwordsHost == true) {
            blockAdwordsHostInDex()
        } else {
            println("[Block Telemetry] Skipped blockAdwordsHost: option is disabled.")
        }

        println("[Block Telemetry] Injected Smali hooks into ${hookedMethods.size} telemetry & diagnostic methods across ${targetClasses.size} classes (${targetClasses.joinToString(", ")})")
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
        println("[Block Telemetry] No adwords.google.com or reportingwidget.google.com literals found.")
    } else {
        println("[Block Telemetry] Rewrote $rewrittenStrings adwords.google.com / reportingwidget.google.com literal(s) across $touchedClasses class(es) -> 0.0.0.0.")
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
