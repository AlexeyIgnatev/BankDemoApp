package com.esom.bank.common.utils.files

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.esom.bank.screens.history.model.ReceiptModel
import org.w3c.dom.Document
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

object ReceiptFileUtils {
    private const val TEMPLATE_ASSET_NAME = "receipt_template.docx"
    private const val DOCUMENT_XML_ENTRY = "word/document.xml"
    private const val DOCX_MIME_TYPE =
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    private const val WORD_NS = "http://schemas.openxmlformats.org/wordprocessingml/2006/main"

    private const val STATUS_INDEX_1 = 5
    private const val STATUS_INDEX_2 = 6
    private const val AMOUNT_INDEX = 7
    private const val OPERATION_TEXT_START_INDEX = 8
    private const val OPERATION_TEXT_END_INDEX = 14
    private const val DATE_INDEX = 23
    private const val TIME_INDEX = 24
    private const val FEE_VALUE_INDEX = 25
    private const val FEE_SPACE_INDEX = 26
    private const val FEE_CURRENCY_INDEX = 27
    private const val ACCOUNT_DETAILS_INDEX = 28
    private const val PAID_FROM_ACCOUNT_INDEX = 29
    private const val RECEIPT_NUMBER_INDEX = 30
    private const val RECIPIENT_START_INDEX = 31
    private const val RECIPIENT_END_INDEX = 35

    fun saveReceiptToDownloads(context: Context, receipt: ReceiptModel): Uri {
        val docxData = buildReceiptDocx(context, receipt)
        val fileName = buildFileName(receipt.receiptNumber)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            saveWithMediaStore(context, fileName, docxData)
        } else {
            saveLegacy(fileName, docxData)
        }
    }

    private fun buildReceiptDocx(context: Context, receipt: ReceiptModel): ByteArray {
        val output = ByteArrayOutputStream()

        ZipInputStream(context.assets.open(TEMPLATE_ASSET_NAME)).use { zipInput ->
            ZipOutputStream(output).use { zipOutput ->
                var entry = zipInput.nextEntry
                while (entry != null) {
                    val entryData = readCurrentEntryBytes(zipInput)
                    val updatedData = if (entry.name == DOCUMENT_XML_ENTRY) {
                        updateDocumentXml(entryData, receipt)
                    } else {
                        entryData
                    }

                    val newEntry = ZipEntry(entry.name)
                    zipOutput.putNextEntry(newEntry)
                    if (updatedData.isNotEmpty()) {
                        zipOutput.write(updatedData)
                    }
                    zipOutput.closeEntry()
                    zipInput.closeEntry()
                    entry = zipInput.nextEntry
                }
            }
        }

        return output.toByteArray()
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

    private fun updateDocumentXml(originalXml: ByteArray, receipt: ReceiptModel): ByteArray {
        val document = parseXml(originalXml)
        val textNodes = document.getElementsByTagNameNS(WORD_NS, "t")

        if (textNodes.length <= RECIPIENT_END_INDEX) {
            throw IllegalStateException("Unsupported receipt template format")
        }

        fun setText(index: Int, value: String) {
            textNodes.item(index)?.textContent = value
        }

        val statusText = if (receipt.successful) "Проведено" else "Не проведено"
        setText(STATUS_INDEX_1, statusText)
        setText(STATUS_INDEX_2, statusText)

        setText(AMOUNT_INDEX, formatNumber(receipt.amount))

        setText(OPERATION_TEXT_START_INDEX, resolveOperationText(receipt))
        for (index in (OPERATION_TEXT_START_INDEX + 1)..OPERATION_TEXT_END_INDEX) {
            setText(index, "")
        }

        val (dateText, timeText) = formatDateAndTime(receipt.createdAt)
        setText(DATE_INDEX, dateText)
        setText(TIME_INDEX, timeText)

        setText(FEE_VALUE_INDEX, formatNumber(receipt.fee))
        setText(FEE_SPACE_INDEX, " ")
        setText(FEE_CURRENCY_INDEX, formatCurrencyForDocument(receipt.currency))

        setText(ACCOUNT_DETAILS_INDEX, receipt.accountDetails)
        setText(PAID_FROM_ACCOUNT_INDEX, receipt.paidFromAccount)
        setText(RECEIPT_NUMBER_INDEX, receipt.receiptNumber)

        setText(RECIPIENT_START_INDEX, receipt.recipientFullName)
        for (index in (RECIPIENT_START_INDEX + 1)..RECIPIENT_END_INDEX) {
            setText(index, "")
        }

        return toByteArray(document)
    }

    private fun resolveOperationText(receipt: ReceiptModel): String {
        if (receipt.type.equals("CONVERSION", ignoreCase = true)) {
            return "Конвертация средств"
        }
        return if (isCryptoWalletAccount(receipt.accountDetails)) {
            "Перевод по адресу кошелька."
        } else {
            "Перевод по номеру телефона"
        }
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
        return BigDecimal(value.toString()).stripTrailingZeros().toPlainString()
    }

    private fun formatCurrencyForDocument(currency: String): String {
        return if (currency.equals("SOM", ignoreCase = true)) {
            "С"
        } else {
            currency
        }
    }

    private fun parseXml(xml: ByteArray): Document {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val builder = factory.newDocumentBuilder()
        return builder.parse(ByteArrayInputStream(xml))
    }

    private fun toByteArray(document: Document): ByteArray {
        val output = ByteArrayOutputStream()
        val transformer = TransformerFactory.newInstance().newTransformer().apply {
            setOutputProperty(OutputKeys.ENCODING, "UTF-8")
            setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no")
        }
        transformer.transform(DOMSource(document), StreamResult(output))
        return output.toByteArray()
    }

    private fun buildFileName(receiptNumber: String): String {
        val base = if (receiptNumber.isBlank()) {
            "receipt_${System.currentTimeMillis()}"
        } else {
            receiptNumber
        }
        val sanitized = base.replace(Regex("[^A-Za-z0-9._-]"), "_")
        return if (sanitized.endsWith(".docx", ignoreCase = true)) sanitized else "$sanitized.docx"
    }

    private fun saveWithMediaStore(context: Context, fileName: String, data: ByteArray): Uri {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, DOCX_MIME_TYPE)
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
