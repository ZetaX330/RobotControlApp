package com.example.rcapp.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.View
import kotlin.math.max

object PreviewImageHelper {
    fun getPreviewBitmap(context: Context, uri: Uri): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
            // 通过 context.contentResolver 获取
            BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri), null, this)
            inSampleSize = calculateInSampleSize(this)
            inJustDecodeBounds = false
        }
        return BitmapFactory.decodeStream(
            context.contentResolver.openInputStream(uri),
            null,
            options
        )
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options): Int {
        val (height, width) = options.run { outHeight to outWidth }
        var inSampleSize = 1
        val targetSize = 1024
        // 优化计算逻辑：取长边与目标尺寸比较
        val maxDimension = max(height, width)
        if (maxDimension > targetSize) {
            while (maxDimension / inSampleSize > targetSize) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}