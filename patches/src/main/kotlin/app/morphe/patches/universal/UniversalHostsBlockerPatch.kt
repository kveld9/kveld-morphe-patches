package app.morphe.patches.universal

import app.morphe.patcher.extensions.InstructionExtensions.instructionsOrNull
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.booleanOption
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.filePathOption
import app.morphe.patcher.patch.stringOption
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.Instruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.ReferenceInstruction
import com.android.tools.smali.dexlib2.iface.reference.StringReference
import java.io.File
import java.util.Locale

private const val TAG = "[Universal Hosts Blocker]"
private const val DEFAULT_SINK_IP = "0.0.0.0"

private val RESERVED_HOSTS = setOf(
    "localhost",
    "localhost6",
    "localhost.localdomain",
    "0.0.0.0",
    "127.0.0.1",
    "::1",
)

private val IPV4_PATTERN = Regex("""^\d{1,3}(\.\d{1,3}){3}$""")

private data class PendingRewrite(val index: Int, val register: Int, val replacement: String)

@Suppress("unused")
val universalHostsBlockerPatch = bytecodePatch(
    name = "Universal Hosts Blocker",
    description = "Rewrites Dalvik const-string URL/host literals matching a user-supplied hosts blocklist to a sink IP (default 0.0.0.0). The blocklist file is read at patch time, so daily DNS updates apply without patch releases.",
    default = false,
) {
    // Universal patch: applies to any target APK in Morphe Manager / CLI (no compatibleWith)
    val hostsFile by filePathOption(
        key = "hostsFile",
        default = null,
        title = "Hosts blocklist file",
        description = "Path to a hosts or plain-domain blocklist file on the patching machine (e.g. Hagezi native.tiktok-onlydomains.txt). Leave empty to skip.",
        required = false,
    )

    val sinkIp by stringOption(
        key = "sinkIp",
        title = "Sink IP address",
        description = "IPv4 address replacing blocked hosts inside const-string literals (e.g. '0.0.0.0').",
        default = DEFAULT_SINK_IP,
        required = false,
    )

    val matchSubdomains by booleanOption(
        key = "matchSubdomains",
        title = "Match subdomains",
        description = "When enabled, a blocklist entry also matches its subdomains (entry 'example.com' matches 'a.example.com').",
        default = true,
        required = false,
    )

    execute {
        val selectedPath = hostsFile?.trim().orEmpty()
        if (selectedPath.isEmpty()) {
            println("$TAG Skipped: no hosts blocklist file selected (hostsFile is empty).")
            return@execute
        }

        val listFile = File(selectedPath)
        if (!listFile.isFile) {
            println("$TAG Skipped: hosts blocklist file not found: $selectedPath")
            return@execute
        }

        val blocklist = parseBlocklistFile(listFile)
        if (blocklist.isEmpty()) {
            println("$TAG Skipped: 0 parseable hosts in ${listFile.name}.")
            return@execute
        }

        val sink = (sinkIp?.trim().orEmpty()).ifEmpty { DEFAULT_SINK_IP }
        if (!IPV4_PATTERN.matches(sink)) {
            println("$TAG Skipped: sink IP is not a valid IPv4 address: $sink")
            return@execute
        }

        val useSubdomains = matchSubdomains ?: true
        var rewrittenStrings = 0
        var touchedClasses = 0
        val blockedRoots = mutableSetOf<String>()

        classDefForEach { classDef ->
            if (!hasAnyTargetInstruction(classDef, blocklist, useSubdomains)) return@classDefForEach

            val mutableClass = mutableClassDefBy(classDef)
            var classModified = false

            for (method in mutableClass.methods) {
                val rewrites = collectConstStringRewrites(method, blocklist, sink, useSubdomains, blockedRoots)
                if (rewrites.isNotEmpty()) {
                    applyRewrites(method, rewrites)
                    rewrittenStrings += rewrites.size
                    classModified = true
                }
            }

            if (classModified) touchedClasses++
        }

        if (rewrittenStrings == 0) {
            println("$TAG No const-string matched ${blocklist.size} blocklist entr(ies) (0 literals rewritten).")
            return@execute
        }

        println("$TAG Rewrote $rewrittenStrings const-string literal(s) across $touchedClasses class(es) covering ${blockedRoots.size} blocked host(s) -> $sink.")
        blockedRoots.sorted().forEach { host ->
            println("$TAG Blocked host: $host")
        }
    }
}

private fun parseBlocklistFile(file: File): Set<String> {
    val entries = LinkedHashSet<String>()
    file.useLines { lines ->
        for (rawLine in lines) {
            parseBlocklistLine(rawLine, entries)
        }
    }
    if (entries.size > 100000) {
        println("$TAG Large blocklist (${entries.size} entries): expect higher memory use and longer patch time on low-RAM devices.")
    }
    return entries
}

private fun parseBlocklistLine(rawLine: String, out: MutableSet<String>) {
    val line = rawLine.substringBefore('#').trim()
    if (line.isEmpty() || line.startsWith("@@")) return

    val tokens = line.split(Regex("\\s+")).filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return

    val candidates = dropLeadingAddressToken(tokens)
    for (candidate in candidates) {
        normalizeCandidateHost(candidate)?.let { out.add(it) }
    }
}

private fun dropLeadingAddressToken(tokens: List<String>): List<String> {
    if (tokens.size < 2) return tokens
    val first = tokens.first().lowercase(Locale.US)
    if (IPV4_PATTERN.matches(first) || first == "::1" || first == "::") return tokens.drop(1)
    return tokens
}

private fun normalizeCandidateHost(candidate: String): String? {
    var host = candidate.trim()
    if (host.isEmpty()) return null
    if (host.startsWith("||")) host = host.removePrefix("||")
    if (host.startsWith("*.")) host = host.removePrefix("*.")

    host = host.substringBefore('^').substringBefore('$').trim()
    if (host.isEmpty() || host.contains(' ') || host.contains('*')) return null

    host = extractLiteralHost(host) ?: return null
    if (host in RESERVED_HOSTS || !isValidDomain(host)) return null
    return host
}

private fun extractLiteralHost(value: String): String? {
    var host = value.trim().lowercase(Locale.US)
    if (host.isEmpty()) return null
    if (host.startsWith("http://") || host.startsWith("https://")) {
        host = host.substringAfter("://")
    }
    host = host.substringBefore('/').substringBefore('?').substringBefore(':')
    host = host.trimEnd('.')
    if (host.isEmpty() || host.contains(' ') || host.contains('/')) return null
    return host
}

private fun isValidDomain(host: String): Boolean {
    if (host.length < 4 || host.length > 253 || !host.contains('.')) return false
    if (host.startsWith('.') || host.endsWith('.') || host.startsWith('-')) return false
    val labels = host.split('.')
    if (labels.size < 2) return false
    for (label in labels) {
        if (label.isEmpty() || label.length > 63) return false
        if (!label.all { it.isLetterOrDigit() || it == '-' }) return false
    }
    return true
}

private fun findBlockedRoot(host: String, blocklist: Set<String>, useSubdomains: Boolean): String? {
    if (blocklist.contains(host)) return host
    if (!useSubdomains) return null
    var parent = host.substringAfter('.', missingDelimiterValue = "")
    while (parent.contains('.')) {
        if (blocklist.contains(parent)) return parent
        parent = parent.substringAfter('.')
    }
    if (parent.isNotEmpty() && blocklist.contains(parent)) return parent
    return null
}

private fun hasAnyTargetInstruction(
    classDef: ClassDef,
    blocklist: Set<String>,
    useSubdomains: Boolean,
): Boolean {
    for (method in classDef.methods) {
        if (hasMethodTargetInstruction(method, blocklist, useSubdomains)) return true
    }
    return false
}

private fun hasMethodTargetInstruction(
    method: Method,
    blocklist: Set<String>,
    useSubdomains: Boolean,
): Boolean {
    val instructions = method.instructionsOrNull ?: return false
    for (instruction in instructions) {
        if (isTargetInstruction(instruction, blocklist, useSubdomains)) return true
    }
    return false
}

private fun isTargetInstruction(
    instruction: Instruction,
    blocklist: Set<String>,
    useSubdomains: Boolean,
): Boolean {
    return resolveBlockedRoot(instruction, blocklist, useSubdomains) != null
}

private fun isConstStringOpcode(opcode: Opcode): Boolean {
    return opcode == Opcode.CONST_STRING || opcode == Opcode.CONST_STRING_JUMBO
}

private fun resolveBlockedRoot(
    instruction: Instruction,
    blocklist: Set<String>,
    useSubdomains: Boolean,
): Pair<String, String>? {
    if (!isConstStringOpcode(instruction.opcode)) return null
    val literal = (instruction as? ReferenceInstruction)?.reference as? StringReference ?: return null
    val original = literal.string
    val host = extractLiteralHost(original) ?: return null
    val root = findBlockedRoot(host, blocklist, useSubdomains) ?: return null
    return original to root
}

private fun buildPendingRewrite(
    instruction: Instruction,
    index: Int,
    original: String,
    root: String,
    sink: String,
): PendingRewrite? {
    val replacement = original.replace(root, sink, ignoreCase = true)
    if (replacement.equals(original, ignoreCase = true)) return null
    val register = (instruction as? OneRegisterInstruction)?.registerA ?: return null
    return PendingRewrite(index, register, replacement)
}

private fun findPendingRewrite(
    instruction: Instruction,
    index: Int,
    blocklist: Set<String>,
    sink: String,
    useSubdomains: Boolean,
): Pair<PendingRewrite, String>? {
    val (original, root) = resolveBlockedRoot(instruction, blocklist, useSubdomains) ?: return null
    val rewrite = buildPendingRewrite(instruction, index, original, root, sink) ?: return null
    return rewrite to root
}

private fun collectConstStringRewrites(
    method: MutableMethod,
    blocklist: Set<String>,
    sink: String,
    useSubdomains: Boolean,
    blockedRoots: MutableSet<String>,
): List<PendingRewrite> {
    val instructions = method.instructionsOrNull?.toList() ?: return emptyList()
    val rewrites = mutableListOf<PendingRewrite>()

    for ((index, instruction) in instructions.withIndex()) {
        val (rewrite, root) = findPendingRewrite(instruction, index, blocklist, sink, useSubdomains) ?: continue
        blockedRoots.add(root)
        rewrites.add(rewrite)
    }

    return rewrites
}

private fun applyRewrites(method: MutableMethod, rewrites: List<PendingRewrite>) {
    for (rewrite in rewrites.sortedByDescending { it.index }) {
        val opcode = if (rewrite.register > 255) "const-string/jumbo" else "const-string"
        method.replaceInstruction(rewrite.index, "$opcode v${rewrite.register}, \"${escapeSmaliLiteral(rewrite.replacement)}\"")
    }
}

private fun escapeSmaliLiteral(value: String): String {
    val escaped = StringBuilder(value.length)
    for (char in value) {
        when (char) {
            '\\' -> escaped.append("\\\\")
            '"' -> escaped.append("\\\"")
            '\n' -> escaped.append("\\n")
            '\r' -> escaped.append("\\r")
            '\t' -> escaped.append("\\t")
            else -> escaped.append(char)
        }
    }
    return escaped.toString()
}
