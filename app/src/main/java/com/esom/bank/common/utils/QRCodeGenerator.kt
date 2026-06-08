import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

object QRCodeGenerator {
    private const val APP_QR_PREFIX = "ESOM_BANK_QR"

    fun generateCryptoQRCodeWithScheme(
        address: String,
        currency: CurrencyEnum,
        width: Int = 600,
        height: Int = 600
    ): Bitmap {
        val qrContent = "$APP_QR_PREFIX|v=1|currency=${currency.name}|contact=$address"
        return generateRoundedQRCode(qrContent, width, height)
    }

    private fun generateRoundedQRCode(content: String, width: Int, height: Int): Bitmap {
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            width,
            height
        )

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        canvas.drawColor(Color.WHITE)

        val darkColor = Color.parseColor("#1D1D1B")
        paint.color = darkColor

        val cellSize = width / bitMatrix.width.toFloat()
        val radius = cellSize * 0.3f

        for (x in 0 until bitMatrix.width) {
            for (y in 0 until bitMatrix.height) {
                if (bitMatrix.get(x, y)) {
                    val left = x * cellSize
                    val top = y * cellSize
                    val right = left + cellSize
                    val bottom = top + cellSize

                    val rect = RectF(left, top, right, bottom)
                    canvas.drawRoundRect(rect, radius, radius, paint)
                }
            }
        }

        return bitmap
    }
}
