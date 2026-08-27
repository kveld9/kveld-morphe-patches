package app.morphe.patches.vivaldi

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.opcode
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.Constants
import com.android.tools.smali.dexlib2.Opcode

@Suppress("unused")
val vivaldiStartupPerformancePatch = bytecodePatch(
    name = "Vivaldi Startup Performance Optimization",
    description = "Optimizes cold startup time, eliminates background CPU/disk overhead, and guards against early CompositorView LayoutStateObserver crashes.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_VIVALDI)

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
        fp1.method.addInstructions(
            0,
            """
                sget-object v0, Ljava/lang/Boolean;->TRUE:Ljava/lang/Boolean;
                iput-object v0, p0, Lorg/chromium/chrome/browser/partnercustomizations/PartnerBrowserCustomizations;->d:Ljava/lang/Boolean;
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
            ),
        )
        fp2.method.addInstructions(
            4,
            """
                if-eqz v2, :safe_continue
                return-void
                :safe_continue
            """,
        )
        val c2 = app.morphe.patches.shared.LocaleUtils.cleanClassName(fp2.originalClassDef.type)

        println("[Startup Performance] Neutralized $c1 async OEM init & patched $c2 CompositorView null safety")
    }
}
