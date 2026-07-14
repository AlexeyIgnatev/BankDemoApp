package com.esom.bank.common.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.esom.bank.BuildConfig
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.MultiFormatWriter
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object QrShareUtils {
    fun createQrBitmap(
        address: String,
        currency: CurrencyEnum,
        size: Int = 720
    ): Bitmap {
        val content = AppQrCode.buildContent(address, currency)
        val matrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        canvas.drawColor(Color.WHITE)
        paint.color = Color.parseColor("#1D1D1B")

        val cellSize = size / matrix.width.toFloat()
        val radius = cellSize * 0.3f

        for (x in 0 until matrix.width) {
            for (y in 0 until matrix.height) {
                if (!matrix.get(x, y)) continue
                val left = x * cellSize
                val top = y * cellSize
                val right = left + cellSize
                val bottom = top + cellSize
                canvas.drawRoundRect(left, top, right, bottom, radius, radius, paint)
            }
        }

        return bitmap
    }

    fun createShareBitmap(
        title: String,
        subtitle: String? = null,
        qrBitmap: Bitmap
    ): Bitmap {
        val width = 1280
        val padding = 84f
        val titleSize = 58f
        val subtitleSize = 36f
        val cardRadius = 44f
        val cardPadding = 44f
        val qrSize = min(900, width - ((padding + cardPadding) * 2).toInt())

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1A1A1A")
            textSize = titleSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#777777")
            textSize = subtitleSize
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.LEFT
        }
        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#A5A5A5")
            textSize = 32f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val titleLayout = buildTextLayout(title, titlePaint, width - (padding * 2).toInt())
        val subtitleLayout = subtitle?.takeIf { it.isNotBlank() }?.let {
            buildTextLayout(it, subtitlePaint, width - (padding * 2).toInt())
        }
        val headerHeight = titleLayout.height + 40f + (subtitleLayout?.height ?: 0) + if (subtitleLayout != null) 18f else 0f
        val footerLayoutHeight = 52f
        val cardHeight = qrSize + (cardPadding * 2)
        val height = (
            padding * 2 +
            headerHeight +
            10 +
            cardHeight +
            28 +
            footerLayoutHeight
        ).toInt()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val contentLeft = padding
        val contentRight = width - padding
        val centerX = width / 2f
        var y = padding
        val headerBottom = y + headerHeight
        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FAFAFA")
        }
        val headerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EBEBEB")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(contentLeft, y, contentRight, headerBottom, 36f, 36f, headerPaint)
        canvas.drawRoundRect(contentLeft, y, contentRight, headerBottom, 36f, 36f, headerBorderPaint)
        canvas.drawRoundRect(
            contentLeft + 18f,
            y + 18f,
            contentLeft + 92f,
            y + 54f,
            18f,
            18f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#E62324") }
        )
        canvas.drawText("Esom Bank", contentLeft + 104f, y + 43f, footerPaint)
        drawMultilineText(canvas, title, titlePaint, centerX, y + 82f, width - padding * 2)
        y += headerHeight + 10f

        subtitle?.takeIf { it.isNotBlank() }?.let {
            drawMultilineText(canvas, it, subtitlePaint, centerX, y, width - padding * 2)
            y += subtitleLayout?.height?.toFloat() ?: 0f
            y += 22f
        }

        val scaledQr = Bitmap.createScaledBitmap(qrBitmap, qrSize, qrSize, true)
        val cardLeft = padding
        val cardTop = y
        val cardRight = width - padding
        val cardBottom = cardTop + cardHeight
        val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            setShadowLayer(18f, 0f, 8f, 0x18000000)
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#E8E8E8")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRoundRect(cardLeft, cardTop, cardRight, cardBottom, cardRadius, cardRadius, cardPaint)
        canvas.drawRoundRect(cardLeft, cardTop, cardRight, cardBottom, cardRadius, cardRadius, borderPaint)

        val qrLeft = cardLeft + cardPadding + ((cardRight - cardLeft) - (cardPadding * 2) - qrSize) / 2f
        val qrTop = cardTop + cardPadding
        canvas.drawBitmap(scaledQr, qrLeft, qrTop, null)

        if (scaledQr !== qrBitmap) scaledQr.recycle()
        return bitmap
    }

    fun shareBitmap(
        context: Context,
        bitmap: Bitmap,
        fileNamePrefix: String,
        chooserTitle: String
    ) {
        val cacheDir = File(context.cacheDir, "shared_receipts").apply { mkdirs() }
        val file = File(cacheDir, "${fileNamePrefix}_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        val uri = FileProvider.getUriForFile(
            context,
            "${BuildConfig.APPLICATION_ID}.fileprovider",
            file
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, chooserTitle))
    }

    fun decodeQrFromBitmap(bitmap: Bitmap): String? {
        val hints = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
            DecodeHintType.TRY_HARDER to true
        )
        return listOf(
            bitmap,
            centerCrop(bitmap, 0.82f),
            centerCrop(bitmap, 0.70f),
        ).flatMap { candidate ->
            listOf(candidate, rotate(candidate, 90), rotate(candidate, 180), rotate(candidate, 270))
        }.asSequence().mapNotNull { candidate ->
            runCatching {
                val pixels = IntArray(candidate.width * candidate.height)
                candidate.getPixels(pixels, 0, candidate.width, 0, 0, candidate.width, candidate.height)
                val source = RGBLuminanceSource(candidate.width, candidate.height, pixels)
                val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
                MultiFormatReader().decode(binaryBitmap, hints).text
            }.getOrNull()
        }.firstOrNull()
    }

    private fun centerCrop(bitmap: Bitmap, factor: Float): Bitmap {
        val cropWidth = max(1, (bitmap.width * factor).toInt())
        val cropHeight = max(1, (bitmap.height * factor).toInt())
        val left = max(0, (bitmap.width - cropWidth) / 2)
        val top = max(0, (bitmap.height - cropHeight) / 2)
        return Bitmap.createBitmap(bitmap, left, top, cropWidth, cropHeight)
    }

    private fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees % 360 == 0) return bitmap
        val matrix = android.graphics.Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun drawMultilineText(
        canvas: Canvas,
        text: String,
        paint: TextPaint,
        centerX: Float,
        top: Float,
        maxWidth: Float
    ) {
        val availableWidth = maxWidth.toInt()
        val layout = android.text.StaticLayout.Builder
            .obtain(text, 0, text.length, paint, availableWidth)
            .setAlignment(android.text.Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1.1f)
            .setIncludePad(false)
            .build()
        canvas.save()
        canvas.translate(centerX - availableWidth / 2f, top)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun buildTextLayout(text: String, paint: TextPaint, maxWidth: Int): android.text.StaticLayout {
        return android.text.StaticLayout.Builder
            .obtain(text, 0, text.length, paint, maxWidth)
            .setAlignment(android.text.Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(0f, 1.12f)
            .setIncludePad(false)
            .build()
    }

    fun buildTitle(currency: CurrencyEnum, name: String): String {
        val personSuffix = if (name.isBlank()) "" else " от $name"
        return when (currency) {
            CurrencyEnum.SOM -> "Это номер телефона$personSuffix"
            CurrencyEnum.ESOM -> "Это кошелек САЛАМ$personSuffix"
            CurrencyEnum.USDT_TRC20 -> "Это кошелек USDT$personSuffix"
        }
    }

    fun shortUserName(firstName: String, middleName: String?, lastName: String): String {
        val initials = when {
            !middleName.isNullOrBlank() -> middleName.trim().firstOrNull()
            !lastName.isBlank() -> lastName.trim().firstOrNull()
            else -> null
        }?.uppercaseChar()
        return if (initials == null) firstName.trim() else "${firstName.trim()} $initials."
    }

    fun formatCurrencyTitle(currency: CurrencyEnum): String = when (currency) {
        CurrencyEnum.SOM -> "номер телефона"
        CurrencyEnum.ESOM -> "кошелька САЛАМ"
        CurrencyEnum.USDT_TRC20 -> "кошелька USDT"
    }
}
