import android.graphics.Bitmap
import android.graphics.Color
import com.esom.bank.screens.main.enums.CurrencyEnum
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix

object QRCodeGenerator {
    fun generateCryptoQRCodeWithScheme(
        address: String,
        currency: CurrencyEnum,
        width: Int = 400,
        height: Int = 400
    ): Bitmap {
        val scheme = when (currency) {
            CurrencyEnum.BTC -> "bitcoin"
            CurrencyEnum.ETH -> "ethereum"
            CurrencyEnum.USDT_TRC20 -> "tron"
            else -> ""
        }

        val qrContent = if (scheme.isNotEmpty()) "$scheme:$address" else address
        return generateQRCode(qrContent, width, height)
    }

    private fun generateQRCode(content: String, width: Int, height: Int): Bitmap {
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            width,
            height
        )

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }
}