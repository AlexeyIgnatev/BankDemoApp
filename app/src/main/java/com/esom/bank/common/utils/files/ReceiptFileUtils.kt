package com.esom.bank.common.utils.files

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.res.ResourcesCompat
import com.esom.bank.R
import com.esom.bank.screens.history.enums.ConversionSide
import com.esom.bank.screens.history.model.ReceiptModel
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipInputStream

object ReceiptFileUtils {
    private const val TEMPLATE_ASSET_NAME = "receipt_template.docx"
    private const val LOGO_MEDIA_ENTRY = "word/media/image1.jpeg"
    private const val JPEG_MIME_TYPE = "image/jpeg"

    private const val OUTPUT_WIDTH = 1240
    private const val OUTPUT_HEIGHT = 1754

    fun saveReceiptToDownloads(context: Context, receipt: ReceiptModel): Uri {
        val imageData = buildReceiptJpeg(context, receipt)
        val fileName = buildFileName(receipt.receiptNumber)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveWithMediaStore(context, fileName, imageData)
        } else {
            saveLegacy(fileName, imageData)
        }
    }

    private fun buildReceiptJpeg(context: Context, receipt: ReceiptModel): ByteArray {
        val bitmap = buildReceiptBitmap(context, receipt)
        val output = ByteArrayOutputStream()
        val compressed = bitmap.compress(Bitmap.CompressFormat.JPEG, 95, output)
        bitmap.recycle()
        if (!compressed) {
            throw IllegalStateException("Failed to encode receipt image")
        }
        return output.toByteArray()
    }

    private fun buildReceiptBitmap(context: Context, receipt: ReceiptModel): Bitmap {
        val bitmap = Bitmap.createBitmap(OUTPUT_WIDTH, OUTPUT_HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        val regularTypeface = loadTypeface(context, R.font.mont_regular, Typeface.SANS_SERIF)
        val mediumTypeface = loadTypeface(context, R.font.mont_medium, Typeface.DEFAULT_BOLD)
        val boldTypeface = loadTypeface(context, R.font.mont_bold, Typeface.DEFAULT_BOLD)

        drawHeader(canvas, context, boldTypeface)
        drawStatusChip(canvas, receipt, mediumTypeface)
        drawAmount(canvas, receipt, boldTypeface, regularTypeface)
        drawDetails(canvas, receipt, mediumTypeface, regularTypeface)
        drawSeal(canvas, context)

        return bitmap
    }

    private fun drawHeader(canvas: Canvas, context: Context, titleTypeface: Typeface) {
        val logoBitmap = readMediaBitmapFromTemplate(context, LOGO_MEDIA_ENTRY)
        if (logoBitmap != null) {
            val logoRect = RectF(88f, 80f, 188f, 180f)
            canvas.drawBitmap(logoBitmap, null, logoRect, null)
            logoBitmap.recycle()
        }

        val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#B52F26")
            textSize = 50f
            typeface = titleTypeface
        }
        val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D46B69")
            textSize = 34f
            typeface = titleTypeface
        }

        canvas.drawText("Finance", 214f, 126f, titlePaint)
        canvas.drawText("CreditBank", 214f, 170f, subtitlePaint)
    }

    private fun drawStatusChip(
        canvas: Canvas,
        receipt: ReceiptModel,
        typeface: Typeface
    ) {
        val chipRect = RectF(700f, 84f, 1160f, 226f)
        val borderColor = if (receipt.successful) Color.parseColor("#2AC450") else Color.parseColor("#D94747")
        val fillColor = if (receipt.successful) Color.parseColor("#D9F5E2") else Color.parseColor("#FBE3E3")

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = fillColor
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            color = borderColor
        }
        canvas.drawRoundRect(chipRect, 44f, 44f, fillPaint)
        canvas.drawRoundRect(chipRect, 44f, 44f, borderPaint)

        val iconCenterX = chipRect.left + 62f
        val iconCenterY = chipRect.centerY()
        val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 8f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            color = borderColor
        }

        if (receipt.successful) {
            canvas.drawLine(iconCenterX - 16f, iconCenterY + 2f, iconCenterX - 2f, iconCenterY + 18f, iconPaint)
            canvas.drawLine(iconCenterX - 2f, iconCenterY + 18f, iconCenterX + 26f, iconCenterY - 14f, iconPaint)
        } else {
            canvas.drawLine(iconCenterX - 20f, iconCenterY - 18f, iconCenterX + 20f, iconCenterY + 18f, iconPaint)
            canvas.drawLine(iconCenterX + 20f, iconCenterY - 18f, iconCenterX - 20f, iconCenterY + 18f, iconPaint)
        }

        val statusText = if (receipt.successful) "Проведено" else "Не проведено"
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = borderColor
            textSize = 44f
            this.typeface = typeface
        }

        val maxTextWidth = chipRect.width() - 144f
        while (textPaint.measureText(statusText) > maxTextWidth && textPaint.textSize > 30f) {
            textPaint.textSize -= 1f
        }
        val textY = chipRect.centerY() + textPaint.textSize / 3f
        canvas.drawText(statusText, chipRect.left + 116f, textY, textPaint)
    }

    private fun drawAmount(
        canvas: Canvas,
        receipt: ReceiptModel,
        amountTypeface: Typeface,
        textTypeface: Typeface
    ) {
        val amountPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1C1C1C")
            textSize = 74f
            typeface = amountTypeface
        }
        val currency = formatCurrencyForDocument(receipt.currency)
        val amountText = "${formatNumber(receipt.amount)} $currency"
        val amountWidth = amountPaint.measureText(amountText)
        canvas.drawText(amountText, (OUTPUT_WIDTH - amountWidth) / 2f, 350f, amountPaint)

        val operationPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#727272")
            textSize = 34f
            typeface = textTypeface
        }
        val operationText = resolveOperationText(receipt)
        val maxWidth = OUTPUT_WIDTH - 180f
        val operationSingleLine = TextUtils.ellipsize(
            operationText,
            operationPaint,
            maxWidth,
            TextUtils.TruncateAt.END
        ).toString()
        val operationWidth = operationPaint.measureText(operationSingleLine)
        canvas.drawText(operationSingleLine, (OUTPUT_WIDTH - operationWidth) / 2f, 430f, operationPaint)
    }

    private fun drawDetails(
        canvas: Canvas,
        receipt: ReceiptModel,
        labelTypeface: Typeface,
        valueTypeface: Typeface
    ) {
        val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#727272")
            textSize = 28f
            typeface = labelTypeface
        }
        val valuePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1C1C1C")
            textSize = 31f
            typeface = valueTypeface
            textAlign = Paint.Align.RIGHT
        }
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#EDEDED")
            strokeWidth = 2f
        }

        val (dateText, timeText) = formatDateAndTime(receipt.createdAt)
        val feeText = "${formatNumber(receipt.fee)} ${formatCurrencyForDocument(receipt.currency)}"
        val creditedAmountText =
            "${formatNumber(receipt.amount - receipt.fee)} ${formatCurrencyForDocument(receipt.currency)}"
        val accountDetailsValue = sanitizeOneLineValue(
            formatAccountWithVisibleTail(
                primaryValue = resolveAccountDetailsForReceipt(receipt),
                fallbackValue = firstNotBlank(receipt.absToAccount, receipt.absAccount)
            )
        )
        val recipientValue = sanitizeOneLineValue(receipt.recipientFullName)
        val paidFromAccountValue = sanitizeOneLineValue(
            formatAccountWithVisibleTail(
                primaryValue = resolvePaidFromAccountForReceipt(receipt),
                fallbackValue = firstNotBlank(receipt.absFromAccount, receipt.absAccount)
            )
        )
        val rows = listOf(
            "Дата и время" to "$dateText $timeText",
            "Комиссия" to feeText,
            "\u0421\u0443\u043c\u043c\u0430 \u043a \u0437\u0430\u0447\u0438\u0441\u043b\u0435\u043d\u0438\u044e" to creditedAmountText,
            "Реквизиты счета" to accountDetailsValue,
            "Получатель" to recipientValue,
            "Оплачено со счета" to paidFromAccountValue,
            "Номер квитанции" to sanitizeOneLineValue(receipt.receiptNumber)
        )

        val leftX = 90f
        val rightX = OUTPUT_WIDTH - 90f
        val valueColumnLeft = 530f
        val valueMaxWidth = rightX - valueColumnLeft
        var y = 560f

        rows.forEach { (label, rawValue) ->
            val value = TextUtils.ellipsize(
                rawValue.ifBlank { "-" },
                valuePaint,
                valueMaxWidth,
                TextUtils.TruncateAt.END
            ).toString()

            canvas.drawText(label, leftX, y, labelPaint)
            canvas.drawText(value, rightX, y, valuePaint)
            canvas.drawLine(leftX, y + 36f, rightX, y + 36f, dividerPaint)
            y += 112f
        }
    }

    private fun drawSeal(
        canvas: Canvas,
        context: Context
    ) {
        val sealBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.receipt_stamp_clean)
            ?: return

        val sealRect = RectF(760f, 1140f, 1100f, 1468f)
        canvas.drawBitmap(sealBitmap, null, sealRect, null)
        sealBitmap.recycle()
    }

    private fun sanitizeOneLineValue(value: String): String {
        return value
            .replace(Regex("[\\r\\n\\t]+"), " ")
            .replace(Regex("\\s{2,}"), " ")
            .trim()
    }

    private fun loadTypeface(context: Context, fontRes: Int, fallback: Typeface): Typeface {
        return ResourcesCompat.getFont(context, fontRes) ?: fallback
    }

    private fun readMediaBitmapFromTemplate(context: Context, entryName: String): Bitmap? {
        ZipInputStream(context.assets.open(TEMPLATE_ASSET_NAME)).use { zipInput ->
            var entry = zipInput.nextEntry
            while (entry != null) {
                if (entry.name == entryName) {
                    val bytes = readCurrentEntryBytes(zipInput)
                    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
                zipInput.closeEntry()
                entry = zipInput.nextEntry
            }
        }
        return null
    }

    private fun readCurrentEntryBytes(zipInput: ZipInputStream): ByteArray {
        val buffer = ByteArray(4096)
        val entryOutput = ByteArrayOutputStream()
        var read = zipInput.read(buffer)
        while (read != -1) {
            if (read > 0) {
                entryOutput.write(buffer, 0, read)
            }
            read = zipInput.read(buffer)
        }
        return entryOutput.toByteArray()
    }

    private fun resolveOperationText(receipt: ReceiptModel): String {
        if (receipt.type.equals("CONVERSION", ignoreCase = true)) {
            return "Конвертация средств."
        }
        return if (isCryptoWalletAccount(receipt.accountDetails)) {
            "Перевод по адресу кошелька."
        } else {
            "Перевод по номеру телефона."
        }
    }

    private fun resolveAccountDetailsForReceipt(receipt: ReceiptModel): String {
        if (!receipt.type.equals("CONVERSION", ignoreCase = true)) {
            return receipt.accountDetails
        }

        return when (receipt.conversionSide) {
            ConversionSide.IN -> firstNotBlank(
                receipt.absToAccount,
                receipt.absAccount,
                receipt.accountDetails
            )
            ConversionSide.OUT -> firstNotBlank(
                receipt.accountDetails,
                receipt.absToAccount,
                receipt.absAccount
            )
            null -> firstNotBlank(
                receipt.accountDetails,
                receipt.absToAccount,
                receipt.absAccount
            )
        }
    }

    private fun resolvePaidFromAccountForReceipt(receipt: ReceiptModel): String {
        return if (receipt.type.equals("CONVERSION", ignoreCase = true) &&
            receipt.conversionSide == ConversionSide.OUT
        ) {
            firstNotBlank(receipt.absFromAccount, receipt.absAccount, receipt.paidFromAccount)
        } else {
            firstNotBlank(receipt.paidFromAccount, receipt.absFromAccount, receipt.absAccount)
        }
    }

    private fun firstNotBlank(vararg values: String): String {
        return values.firstOrNull { it.isNotBlank() } ?: ""
    }

    private fun formatAccountWithVisibleTail(primaryValue: String, fallbackValue: String): String {
        val primary = sanitizeOneLineValue(primaryValue)
        val fallback = sanitizeOneLineValue(fallbackValue)

        val candidate = if (primary.contains("*") && fallback.isNotBlank()) fallback else primary
        if (candidate.isBlank()) return "-"

        val compact = candidate.replace(" ", "")
        if (compact.length <= 8) return compact

        val visibleLength = if (compact.length >= 8) 8 else 6
        val tail = compact.takeLast(visibleLength)
        val hiddenCount = (compact.length - visibleLength).coerceAtLeast(0)
        return "${"*".repeat(hiddenCount)}$tail"
    }

    private fun isCryptoWalletAccount(accountDetails: String): Boolean {
        val normalized = accountDetails.replace(" ", "")
        if (normalized.isBlank() || normalized.startsWith("*")) return false
        if (normalized.startsWith("0x", ignoreCase = true)) return true
        if (normalized.startsWith("bc1", ignoreCase = true)) return true
        if (normalized.startsWith("T") && normalized.length >= 30) return true
        return normalized.length >= 20 &&
            normalized.any { it.isLetter() } &&
            normalized.any { it.isDigit() }
    }

    private fun formatDateAndTime(createdAt: Long): Pair<String, String> {
        val date = Date(createdAt)
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale("ru", "RU"))
        val timeFormat = SimpleDateFormat("HH:mm", Locale("ru", "RU"))
        return dateFormat.format(date) to timeFormat.format(date)
    }

    private fun formatNumber(value: Double): String {
        val scaled = BigDecimal(value.toString()).setScale(2, java.math.RoundingMode.HALF_UP)
        return scaled.toPlainString().replace('.', ',')
    }

    private fun formatCurrencyForDocument(currency: String): String {
        return if (currency.equals("SOM", ignoreCase = true)) {
            "С"
        } else {
            currency
        }
    }

    private fun buildFileName(receiptNumber: String): String {
        val base = if (receiptNumber.isBlank()) {
            "receipt_${System.currentTimeMillis()}"
        } else {
            receiptNumber
        }
        val sanitized = base.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return if (sanitized.endsWith(".jpg", ignoreCase = true) || sanitized.endsWith(".jpeg", ignoreCase = true)) {
            sanitized
        } else {
            "$sanitized.jpg"
        }
    }

    private fun saveWithMediaStore(context: Context, fileName: String, data: ByteArray): Uri {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, JPEG_MIME_TYPE)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IllegalStateException("Failed to create download entry")

        resolver.openOutputStream(uri)?.use { stream ->
            stream.write(data)
            stream.flush()
        } ?: throw IllegalStateException("Failed to open output stream for $fileName")

        val completeValues = ContentValues().apply {
            put(MediaStore.Downloads.IS_PENDING, 0)
        }
        resolver.update(uri, completeValues, null, null)
        return uri
    }

    @Suppress("DEPRECATION")
    private fun saveLegacy(fileName: String, data: ByteArray): Uri {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        val file = File(downloadsDir, fileName)
        FileOutputStream(file).use { output ->
            output.write(data)
            output.flush()
        }
        return Uri.fromFile(file)
    }
}
