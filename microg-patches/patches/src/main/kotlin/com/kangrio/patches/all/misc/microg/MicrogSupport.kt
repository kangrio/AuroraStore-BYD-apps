package com.kangrio.patches.all.misc.microg

import app.morphe.patcher.PackageMetadata
import app.morphe.patcher.PatcherConfig
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.ResourcePatchContext
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.patch.resourcePatch
import app.morphe.util.adoptChild
import app.morphe.util.findElementByAttributeValue
import com.android.apksig.apk.ApkUtils
import com.android.apksig.internal.apk.v2.V2SchemeVerifier
import com.android.apksig.util.DataSources
import com.android.apksig.util.RunnablesExecutor
import org.w3c.dom.Element
import java.io.File
import java.io.RandomAccessFile
import java.util.Base64

@Suppress("unused")
val spoofSignature = resourcePatch(
    name = "MicroG Support",
    description = "Make support MicroG of this project https://github.com/kangrio/AuroraStore-BYD",
    default = false
) {
    dependsOn(
        bytecodePatch {
            extendWith("extensions/shared.mpe")
        }
    )

    execute {
        val signature = getSignatureBase64(packageMetadata)
        var patchVersion = Int.MAX_VALUE

        try {
            val file = File("patch-version.txt")
            if (file.exists()) {
                val version = file.readText().trim()
                val patchVersion = version.replace(Regex("[^0-9]"), "").toInt()
            }
        }catch (e: Throwable) {
            e.printStackTrace()
        }

        document("AndroidManifest.xml").use { document ->
            val childNodes = document.documentElement.childNodes
            childNodes.findElementByAttributeValue("android:name", "android.permission.QUERY_ALL_PACKAGES") ?: run {
                document.documentElement.adoptChild("uses-permission") {
                    setAttribute("android:name", "android.permission.QUERY_ALL_PACKAGES")
                }
            }

            val applicationNode =
                document
                    .getElementsByTagName("application")
                    .item(0) as Element

            applicationNode.setAttribute(
                "android:appComponentFactory",
                "com.kangrio.extension.shared.spoof.SpoofAppComponentFactory"
            )

            applicationNode.adoptChild("meta-data") {
                setAttribute("android:name", "org.microg.gms.spoofed_certificates")
                setAttribute("android:value", signature)
            }
            applicationNode.adoptChild("meta-data") {
                setAttribute("android:name", "morphe_version")
                setAttribute("android:value", patchVersion.toString())
            }
        }
    }
}

private fun getSignatureBase64(packageMetadata: PackageMetadata): String {
    val cert = packageMetadata.signingCertificates.values.firstOrNull()?.firstOrNull()
        ?: throw PatchException("No certificate found")

    return Base64.getMimeEncoder()
        .encodeToString(cert.encoded)
}

private fun getSignatureBase64Reflection(resourcePatchContext: ResourcePatchContext): String {
    try {
        val config = resourcePatchContext.javaClass.getDeclaredField("config").let {
            it.isAccessible = true
            it.get(resourcePatchContext) as PatcherConfig
        }

        val apkFile = config.javaClass.getDeclaredField("apkFile").let {
            it.isAccessible = true
            it.get(config) as File
        }

        val dataSource = DataSources.asDataSource(RandomAccessFile(apkFile.path, "r"))
        val zipSections = ApkUtils.findZipSections(dataSource)
        val v2 = V2SchemeVerifier.verify(
            RunnablesExecutor.SINGLE_THREADED,
            dataSource,
            zipSections,
            mapOf(2 to "APK Signature Scheme v2"),
            hashSetOf(2),
            24,
            Int.MAX_VALUE
        )
        return Base64.getMimeEncoder().encodeToString(v2.signers[0].certs[0].encoded)
    } catch (e: Exception) {
        throw PatchException("Failed to get signature", e)
    }
}