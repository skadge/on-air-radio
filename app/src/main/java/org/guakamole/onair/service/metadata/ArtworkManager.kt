package org.guakamole.onair.service.metadata

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Size
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Handles fetching and processing of artwork images. */
class ArtworkManager(private val context: Context) {

    private val imageLoader = ImageLoader(context)

    suspend fun loadArtwork(uri: Uri): ByteArray? =
            withContext(Dispatchers.IO) {
                val request =
                        ImageRequest.Builder(context)
                                .data(uri)
                                .size(Size.ORIGINAL)
                                .allowHardware(false)
                                .build()

                val result = imageLoader.execute(request)
                if (result is SuccessResult) {
                    val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                    bitmap?.let { compressBitmap(it) }
                } else {
                    null
                }
            }

    private fun compressBitmap(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }
}
