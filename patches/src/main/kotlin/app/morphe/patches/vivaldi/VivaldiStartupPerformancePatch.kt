package app.morphe.patches.vivaldi

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.addInstructionsWithLabels
import app.morphe.patcher.opcode
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.Constants
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.TwoRegisterInstruction
import org.w3c.dom.Element

val vivaldiSplitCompatibilityPatch = resourcePatch(
    name = "Split Bundle Compatibility",
    description = "Removes split APK constraints (isSplitRequired, isolatedSplits) and split metadata from AndroidManifest.xml for standalone APK installation.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_VIVALDI)

    execute {
        val manifestFile = get("AndroidManifest.xml")
        if (!manifestFile.exists()) return@execute

        document(manifestFile.absolutePath).use { doc ->
            val root = doc.documentElement
            if (root != null) {
                listOf(
                    "android:requiredSplitTypes",
                    "android:splitTypes",
                    "android:isolatedSplits",
                    "android:isSplitRequired",
                ).forEach { attr ->
                    if (root.hasAttribute(attr)) {
                        root.removeAttribute(attr)
                    }
                }
            }

            val metaElements = doc.getElementsByTagName("meta-data")
            val splitsMeta = mutableListOf<Element>()
            for (i in 0 until metaElements.length) {
                val elem = metaElements.item(i) as? Element ?: continue
                if (elem.getAttribute("android:name") == "com.android.vending.splits") {
                    splitsMeta.add(elem)
                }
            }
            splitsMeta.forEach { it.parentNode?.removeChild(it) }
        }
        println("[Split Compatibility] Stripped split APK attributes & metadata from AndroidManifest.xml")
    }
}

@Suppress("unused")
val vivaldiStartupPerformancePatch = bytecodePatch(
    name = "Vivaldi Startup Performance Optimization",
    description = "Guards against fatal CompositorView NullPointerException crashes on cold startup and bypasses carrier partner customizations.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_VIVALDI)
    dependsOn(vivaldiSplitCompatibilityPatch)

    execute {
        // 1. Neutralize PartnerBrowserCustomizations.initializeAsync(Context).
        // Immediately marks the component as initialized (Boolean.TRUE in field 'd') and returns,
        // avoiding main-thread SharedPreferences reads, background ThreadPool tasks,
        // ContentResolver queries, and 10-second timeout task scheduling.
        val fp1 = Fingerprint(
            definingClass = "Lorg/chromium/chrome/browser/partnercustomizations/PartnerBrowserCustomizations;",
            returnType = "V",
            parameters = listOf("Landroid/content/Context;"),
            strings = listOf(
                "Chrome.Homepage.PartnerCustomizedDefaultGurl",
                "Chrome.Homepage.PartnerCustomizedDefaultUri",
            ),
        )
        val boolField = fp1.originalClassDef.fields.firstOrNull { it.type == "Ljava/lang/Boolean;" }?.name ?: "d"
        fp1.method.addInstructions(
            0,
            """
                sget-object v0, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;
                iput-object v0, p0, Lorg/chromium/chrome/browser/partnercustomizations/PartnerBrowserCustomizations;->$boolField:Ljava/lang/Boolean;
                return-void
            """,
        )
        val c1 = app.morphe.patches.shared.LocaleUtils.cleanClassName(fp1.originalClassDef.type)

        // 2. Guard against early CompositorView LayoutStateObserver NPE.
        // On cold startup, CompositorView layout updates can trigger onStartedShowing(2) before
        // TabbedRootUiCoordinator has initialized field U1 (vn5).
        // Without this null check, dereferencing vn5.C causes a fatal NullPointerException.
        val fp2 = Fingerprint(
            returnType = "V",
            parameters = listOf("I"),
            filters = listOf(
                opcode(Opcode.IGET_OBJECT),
                opcode(Opcode.CONST_4),
                opcode(Opcode.IF_NE),
                opcode(Opcode.IGET_OBJECT),
                opcode(Opcode.IGET_OBJECT),
                opcode(Opcode.IF_EQZ),
                opcode(Opcode.INVOKE_VIRTUAL),
            ),
        )
        val targetReg = (fp2.method.implementation?.instructions?.elementAtOrNull(3) as? TwoRegisterInstruction)?.registerA ?: 2
        fp2.method.addInstructionsWithLabels(
            4,
            """
                if-nez v$targetReg, :safe_continue
                return-void
                :safe_continue
                nop
            """,
        )
        val c2 = app.morphe.patches.shared.LocaleUtils.cleanClassName(fp2.originalClassDef.type)

        // 3. Android Automotive OEM Reflection Guard:
        // CarDataProvider.isModel(String) attempts Class.forName reflection on car OEM classes,
        // throwing ClassNotFoundException and logging error spam on non-automotive Android devices.
        var automotivePatched = false
        try {
            Fingerprint(
                definingClass = "Lorg/vivaldi/browser/oem_extensions/CarDataProvider;",
                name = "isModel",
                parameters = listOf("Ljava/lang/String;"),
                returnType = "Z",
            ).method.apply {
                addInstructions(0, "const/4 v0, 0x0\nreturn v0")
                automotivePatched = true
            }
        } catch (e: Exception) {
            println("[Startup Performance] CarDataProvider hook note: ${e.message}")
        }

        val autoNote = if (automotivePatched) " & suppressed Automotive reflection" else ""
        println("[Startup Performance] Neutralized $c1 async OEM init, patched $c2 CompositorView null safety$autoNote")
    }
}
