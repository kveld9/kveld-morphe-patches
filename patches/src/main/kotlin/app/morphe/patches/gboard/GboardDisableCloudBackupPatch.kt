package app.morphe.patches.gboard

import app.morphe.patcher.patch.resourcePatch
import app.morphe.patches.shared.ANDROID_XML_NAMESPACE
import app.morphe.patches.shared.Constants
import org.w3c.dom.Element

private val BACKUP_ATTRIBUTES = listOf(
    "backupAgent",
    "fullBackupContent",
    "dataExtractionRules",
    "backupInForeground",
)

@Suppress("unused")
val gboardDisableCloudBackupPatch = resourcePatch(
    name = "Disable Cloud Backup",
    description = "Disables Android backup for Gboard (allowBackup=false and backup agent removed) so keyboard settings, learned words, and personal dictionary data are never uploaded to Google Drive backups or copied by device-to-device transfer.",
    default = true,
) {
    compatibleWith(Constants.COMPATIBILITY_GBOARD)

    execute {
        val manifestFile = get("AndroidManifest.xml")
        if (!manifestFile.exists()) {
            println("[Disable Cloud Backup] Skipped: AndroidManifest.xml not found.")
            return@execute
        }

        var removedAttributes = 0
        document(manifestFile.absolutePath).use { doc ->
            val application = doc.getElementsByTagName("application").item(0) as? Element
            if (application == null) {
                println("[Disable Cloud Backup] Skipped: <application> element not found.")
                return@use
            }

            // Decoded manifests may carry android attributes with or without a bound namespace.
            if (application.getAttributeNodeNS(ANDROID_XML_NAMESPACE, "allowBackup") != null) {
                application.setAttributeNS(ANDROID_XML_NAMESPACE, "android:allowBackup", "false")
            } else {
                application.setAttribute("android:allowBackup", "false")
            }
            for (attribute in BACKUP_ATTRIBUTES) {
                val node = application.getAttributeNodeNS(ANDROID_XML_NAMESPACE, attribute)
                    ?: application.getAttributeNode("android:$attribute")
                if (node != null) {
                    application.removeAttributeNode(node)
                    removedAttributes++
                }
            }

            println("[Disable Cloud Backup] Set allowBackup=false and removed $removedAttributes backup attribute(s) from <application>.")
        }
    }
}
