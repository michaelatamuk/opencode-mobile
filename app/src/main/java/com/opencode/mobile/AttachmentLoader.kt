package com.opencode.mobile

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * Reads an image the user picked and turns it into an inline attachment.
 * Large screenshots are downscaled so a prompt does not balloon into tens of MB.
 */
object AttachmentLoader {

    private const val MAX_DIMENSION = 1600
    private const val INLINE_LIMIT = 1_200_000

    fun load(context: Context, uri: Uri): Attachment {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "image/jpeg"
        val raw = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Could not read the selected image")
        val name = displayName(context, uri) ?: "image_${System.currentTimeMillis()}.jpg"

        if (raw.size <= INLINE_LIMIT) {
            return Attachment(name, mime, Base64.encodeToString(raw, Base64.NO_WRAP))
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(raw, 0, raw.size, bounds)

        var sample = 1
        while (bounds.outWidth / (sample * 2) >= MAX_DIMENSION ||
            bounds.outHeight / (sample * 2) >= MAX_DIMENSION
        ) {
            sample *= 2
        }

        val decoded = BitmapFactory.decodeByteArray(
            raw,
            0,
            raw.size,
            BitmapFactory.Options().apply { inSampleSize = sample }
        ) ?: return Attachment(name, mime, Base64.encodeToString(raw, Base64.NO_WRAP))

        val scale = minOf(1f, MAX_DIMENSION.toFloat() / maxOf(decoded.width, decoded.height))
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).toInt().coerceAtLeast(1),
                (decoded.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            decoded
        }

        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 82, out)
        if (scaled !== decoded) scaled.recycle()
        decoded.recycle()

        val jpegName = name.substringBeforeLast('.', name) + ".jpg"
        return Attachment(jpegName, "image/jpeg", Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP))
    }

    fun loadFile(context: Context, uri: Uri): Attachment {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "application/octet-stream"
        val name = displayName(context, uri) ?: "file_${System.currentTimeMillis()}"
        val raw = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalStateException("Could not read the selected file")
        if (raw.size > 2_000_000) {
            throw IllegalStateException("File is larger than 2 MB")
        }
        return Attachment(name, mime, Base64.encodeToString(raw, Base64.NO_WRAP))
    }

    private fun displayName(context: Context, uri: Uri): String? =
        context.contentResolver
            .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
}
