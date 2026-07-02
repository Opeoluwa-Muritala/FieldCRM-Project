package com.fieldcrm.android.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.fieldcrm.android.core.security.EncryptedDocumentStore
import com.fieldcrm.shared.db.AppDatabase
import java.io.ByteArrayOutputStream
import java.util.UUID

data class LocalDocumentRecord(
    val id: String,
    val loanId: String,
    val docType: String,
    val filename: String,
    val mimeType: String,
    val encPath: String,
    val ivB64: String,
    val sizeBytes: Long,
    val uploadedAt: Long,
    val synced: Boolean
) {
    fun decryptWith(store: EncryptedDocumentStore): ByteArray? = store.readDecrypted(encPath)
}

class LocalDocumentRepository(
    private val context: Context,
    private val database: AppDatabase
) {
    private val queries = database.appDatabaseQueries
    private val encStore = EncryptedDocumentStore(context)

    fun saveDocument(
        loanId: String,
        docType: String,
        filename: String,
        rawBytes: ByteArray,
        mimeType: String = "image/jpeg"
    ): LocalDocumentRecord {
        val compressed = if (mimeType.contains("pdf")) {
            if (rawBytes.size > 512 * 1024) rawBytes.copyOf(512 * 1024) else rawBytes
        } else {
            compressToJpeg(rawBytes)
        }
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val (encPath, ivB64) = encStore.writeEncrypted(id, compressed)

        queries.insertLocalDocument(
            id = id,
            loan_id = loanId,
            doc_type = docType,
            filename = filename,
            mime_type = if (mimeType.contains("pdf")) mimeType else "image/jpeg",
            enc_path = encPath,
            iv_b64 = ivB64,
            size_bytes = compressed.size.toLong(),
            uploaded_at = now,
            synced = 0L
        )
        return LocalDocumentRecord(id, loanId, docType, filename, "image/jpeg", encPath, ivB64, compressed.size.toLong(), now, false)
    }

    fun saveDocumentFromUri(
        loanId: String,
        docType: String,
        uri: Uri,
        originalFilename: String
    ): LocalDocumentRecord? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val rawBytes = inputStream.readBytes()
            inputStream.close()
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            saveDocument(loanId, docType, originalFilename, rawBytes, mimeType)
        } catch (e: Exception) { null }
    }

    fun getDocumentsForLoan(loanId: String): List<LocalDocumentRecord> {
        return queries.selectLocalDocumentsByLoan(loanId).executeAsList().map { row ->
            LocalDocumentRecord(
                id = row.id, loanId = row.loan_id, docType = row.doc_type,
                filename = row.filename, mimeType = row.mime_type,
                encPath = row.enc_path, ivB64 = row.iv_b64,
                sizeBytes = row.size_bytes, uploadedAt = row.uploaded_at,
                synced = row.synced == 1L
            )
        }
    }

    fun getPendingUploads(): List<LocalDocumentRecord> {
        return queries.selectPendingDocumentUploads().executeAsList().map { row ->
            LocalDocumentRecord(
                id = row.id, loanId = row.loan_id, docType = row.doc_type,
                filename = row.filename, mimeType = row.mime_type,
                encPath = row.enc_path, ivB64 = row.iv_b64,
                sizeBytes = row.size_bytes, uploadedAt = row.uploaded_at,
                synced = false
            )
        }
    }

    fun markSynced(id: String) {
        queries.markDocumentSynced(id)
    }

    fun deleteDocument(id: String) {
        val doc = getDocumentsForLoan("").find { it.id == id }
        doc?.let { encStore.deleteFile(it.encPath) }
        queries.deleteLocalDocument(id)
    }

    fun deleteAllForLoan(loanId: String) {
        getDocumentsForLoan(loanId).forEach { encStore.deleteFile(it.encPath) }
        queries.deleteLocalDocumentsByLoan(loanId)
    }

    fun getStore(): EncryptedDocumentStore = encStore

    private fun compressToJpeg(rawBytes: ByteArray): ByteArray {
        return try {
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, options)
            val maxDim = 800
            val scale = maxOf(options.outWidth.toFloat() / maxDim, options.outHeight.toFloat() / maxDim, 1f)
            val sampleSize = scale.toInt().coerceAtLeast(1)
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size, decodeOptions)
                ?: return rawBytes
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 45, out)
            bitmap.recycle()
            out.toByteArray()
        } catch (e: Exception) {
            if (rawBytes.size > 512 * 1024) rawBytes.copyOf(512 * 1024) else rawBytes
        }
    }
}
