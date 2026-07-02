package com.fieldcrm.android.core.security

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import java.io.File

class EncryptedDocumentStore(private val context: Context) {

    companion object {
        private const val DOCS_DIR = "field_docs"
        private const val KEY_ALIAS = "fieldcrm_doc_key"
    }

    private val docsDir: File get() {
        val dir = File(context.filesDir, DOCS_DIR)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun masterKey(): MasterKey = MasterKey.Builder(context, KEY_ALIAS)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    fun writeEncrypted(id: String, rawBytes: ByteArray): Pair<String, String> {
        val encFile = File(docsDir, "$id.enc")
        if (encFile.exists()) encFile.delete()

        val ef = EncryptedFile.Builder(
            context,
            encFile,
            masterKey(),
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        ef.openFileOutput().use { it.write(rawBytes) }
        return Pair(encFile.absolutePath, "tink_managed")
    }

    fun readDecrypted(encPath: String): ByteArray? {
        return try {
            val encFile = File(encPath)
            if (!encFile.exists()) return null
            val ef = EncryptedFile.Builder(
                context,
                encFile,
                masterKey(),
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()
            ef.openFileInput().use { it.readBytes() }
        } catch (e: Exception) { null }
    }

    fun deleteFile(encPath: String) {
        try { File(encPath).delete() } catch (_: Exception) {}
    }
}
