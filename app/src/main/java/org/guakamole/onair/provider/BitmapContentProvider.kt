package org.guakamole.onair.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.content.ContextCompat
import java.io.FileOutputStream

/**
 * ContentProvider that serves drawable resources as PNG bitmaps. This is used to provide Android
 * Auto with bitmap versions of vector drawables, preventing the monochrome rendering that Android
 * Auto applies to vectors.
 *
 * URI format: content://org.guakamole.onair.provider.bitmap/{resourceName}
 */
class BitmapContentProvider : ContentProvider() {

    companion object {
        const val AUTHORITY = "org.guakamole.onair.provider.bitmap"
        private const val BITMAP_SIZE = 256

        fun getUriForDrawable(packageName: String, resourceName: String): Uri {
            return Uri.Builder()
                    .scheme("content")
                    .authority(AUTHORITY)
                    .appendPath(resourceName)
                    .build()
        }
    }

    override fun onCreate(): Boolean = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val context = context ?: return null
        val resourceName = uri.lastPathSegment ?: return null

        // Get the drawable resource ID
        val resourceId =
                context.resources.getIdentifier(resourceName, "drawable", context.packageName)

        if (resourceId == 0) {
            return null
        }

        // Load the drawable and render to bitmap
        val drawable = ContextCompat.getDrawable(context, resourceId) ?: return null

        val bitmap = Bitmap.createBitmap(BITMAP_SIZE, BITMAP_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Calculate bounds to preserve aspect ratio and center
        val intrinsicWidth = drawable.intrinsicWidth
        val intrinsicHeight = drawable.intrinsicHeight

        val (drawWidth, drawHeight) =
                if (intrinsicWidth > 0 && intrinsicHeight > 0) {
                    val scale =
                            minOf(
                                    BITMAP_SIZE.toFloat() / intrinsicWidth,
                                    BITMAP_SIZE.toFloat() / intrinsicHeight
                            )
                    Pair((intrinsicWidth * scale).toInt(), (intrinsicHeight * scale).toInt())
                } else {
                    Pair(BITMAP_SIZE, BITMAP_SIZE)
                }

        val left = (BITMAP_SIZE - drawWidth) / 2
        val top = (BITMAP_SIZE - drawHeight) / 2
        drawable.setBounds(left, top, left + drawWidth, top + drawHeight)
        drawable.draw(canvas)

        // Create a pipe to stream the PNG data
        val pipe = ParcelFileDescriptor.createPipe()
        val readSide = pipe[0]
        val writeSide = pipe[1]

        // Write bitmap in a background thread to avoid blocking
        Thread {
                    try {
                        FileOutputStream(writeSide.fileDescriptor).use { outputStream ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("BitmapContentProvider", "Error writing bitmap", e)
                    } finally {
                        try {
                            writeSide.close()
                        } catch (e: Exception) {
                            // Ignore
                        }
                        bitmap.recycle()
                    }
                }
                .start()

        return readSide
    }

    override fun getType(uri: Uri): String = "image/png"

    override fun query(
            uri: Uri,
            projection: Array<out String>?,
            selection: String?,
            selectionArgs: Array<out String>?,
            sortOrder: String?
    ): Cursor? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
            uri: Uri,
            values: ContentValues?,
            selection: String?,
            selectionArgs: Array<out String>?
    ): Int = 0
}
