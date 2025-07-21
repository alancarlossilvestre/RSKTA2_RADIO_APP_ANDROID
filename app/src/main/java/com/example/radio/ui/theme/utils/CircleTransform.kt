package com.example.radio.ui.theme.utils


import android.graphics.*
import com.squareup.picasso.Transformation


class CircleTransform : Transformation {
    override fun transform(source: Bitmap): Bitmap {
        val size = minOf(source.width, source.height)
        val x = (source.width - size) / 2
        val y = (source.height - size) / 2

        val squared = Bitmap.createBitmap(source, x, y, size, size)
        val bitmap = Bitmap.createBitmap(size, size, source.config ?: Bitmap.Config.ARGB_8888)

        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            shader = BitmapShader(squared, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            isAntiAlias = true
        }

        val radius = size / 2f
        canvas.drawCircle(radius, radius, radius, paint)

        source.recycle()
        return bitmap
    }

    override fun key(): String = "circle"
}