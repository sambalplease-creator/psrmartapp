package com.psrmart.app.util

import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.psrmart.app.data.model.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class InvoicePrinter(private val context: Context) {

    private val PAGE_W    = 595f
    private val PAGE_H    = 842f
    private val MARGIN_L  = 40f
    private val MARGIN_R  = 555f
    private val COL_SNUM  = 42f
    private val COL_DESC  = 72f
    private val COL_QTY_R  = 338f
    private val COL_UNIT_R = 392f
    private val COL_RATE_R = 472f
    private val COL_AMT_R  = MARGIN_R
    private val BODY_MAX_Y = PAGE_H - 110f  // leave room for footer

    // ── Paints (recreated per render so they're safe) ───────────────────

    private fun pTitleC() = Paint().apply { typeface = Typeface.DEFAULT_BOLD; textSize = 22f; color = Color.BLACK; textAlign = Paint.Align.CENTER }
    private fun pBoldL()  = Paint().apply { typeface = Typeface.DEFAULT_BOLD; textSize = 11f; color = Color.BLACK; textAlign = Paint.Align.LEFT }
    private fun pBoldR()  = Paint().apply { typeface = Typeface.DEFAULT_BOLD; textSize = 11f; color = Color.BLACK; textAlign = Paint.Align.RIGHT }
    private fun pNormL()  = Paint().apply { typeface = Typeface.DEFAULT; textSize = 10f; color = Color.BLACK; textAlign = Paint.Align.LEFT }
    private fun pNormR()  = Paint().apply { typeface = Typeface.DEFAULT; textSize = 10f; color = Color.BLACK; textAlign = Paint.Align.RIGHT }
    private fun pSmallL() = Paint().apply { typeface = Typeface.DEFAULT; textSize = 9f; color = Color.DKGRAY; textAlign = Paint.Align.LEFT }
    private fun pSmallR() = Paint().apply { typeface = Typeface.DEFAULT; textSize = 9f; color = Color.DKGRAY; textAlign = Paint.Align.RIGHT }
    private fun pLine()   = Paint().apply { color = Color.BLACK; strokeWidth = 0.8f; style = Paint.Style.STROKE }
    private fun pGray()   = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.4f; style = Paint.Style.STROKE }
    private fun pPageNo() = Paint().apply { typeface = Typeface.DEFAULT; textSize = 8f; color = Color.GRAY; textAlign = Paint.Align.RIGHT }

    // ── Sections ────────────────────────────────────────────────────────

    private fun drawHeader(c: Canvas, invoice: Invoice, settings: BusinessSettings, dateStr: String): Float {
        var y = 50f
        c.drawText("INVOICE", PAGE_W / 2f, y, pTitleC()); y += 30f
        c.drawText("Bill To:", MARGIN_L, y, pBoldL())
        c.drawText(settings.companyName, MARGIN_R, y, pBoldR()); y += 16f
        c.drawText(invoice.customerSnapshot.name, MARGIN_L, y, pBoldL())
        if (settings.email.isNotBlank()) c.drawText(settings.email, MARGIN_R, y, pSmallR())
        y += 14f
        if (invoice.customerSnapshot.company.isNotBlank()) { c.drawText(invoice.customerSnapshot.company, MARGIN_L, y, pNormL()); y += 13f }
        if (invoice.customerSnapshot.phone.isNotBlank())   { c.drawText(invoice.customerSnapshot.phone, MARGIN_L, y, pNormL()); y += 13f }
        if (invoice.customerSnapshot.address.isNotBlank()) { c.drawText(invoice.customerSnapshot.address.take(55), MARGIN_L, y, pNormL()); y += 13f }
        y += 8f
        c.drawText("Invoice No:", MARGIN_L, y, pBoldL())
        c.drawText(invoice.invoiceNumber, MARGIN_L + 76f, y, pNormL()); y += 14f
        c.drawText("Date:", MARGIN_L, y, pBoldL())
        c.drawText(dateStr, MARGIN_L + 76f, y, pNormL())
        invoice.dueDate?.let { c.drawText("Due: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it))}", MARGIN_R, y, pSmallR()) }
        y += 22f
        return y
    }

    private fun drawContinuationHeader(c: Canvas, invoice: Invoice, dateStr: String, page: Int, total: Int): Float {
        var y = 28f
        val p = pNormL(); p.textSize = 10f
        c.drawText("${invoice.invoiceNumber}  ·  ${invoice.customerSnapshot.name}  ·  $dateStr  (Page $page of $total)", MARGIN_L, y, p)
        y += 20f
        return y
    }

    private fun drawTableHeader(c: Canvas, y: Float): Float {
        var yy = y
        c.drawLine(MARGIN_L, yy, MARGIN_R, yy, pLine()); yy += 14f
        c.drawText("No.", COL_SNUM, yy, pBoldL())
        c.drawText("Description", COL_DESC, yy, pBoldL())
        c.drawText("Qty", COL_QTY_R, yy, pBoldR())
        c.drawText("Unit", COL_UNIT_R, yy, pBoldR())
        c.drawText("Rate/Unit", COL_RATE_R, yy, pBoldR())
        c.drawText("Amount (RM)", COL_AMT_R, yy, pBoldR())
        yy += 5f; c.drawLine(MARGIN_L, yy, MARGIN_R, yy, pLine()); yy += 14f
        return yy
    }

    private fun drawItems(c: Canvas, items: List<InvoiceItem>, startIdx: Int, endIdx: Int, y: Float): Float {
        var yy = y
        for (i in startIdx until endIdx) {
            val item = items[i]
            val lines = if (item.description.length <= 32) listOf(item.description) else item.description.chunked(32)
            c.drawText("${i + 1}", COL_SNUM, yy, pNormL())
            c.drawText(lines[0], COL_DESC, yy, pNormL())
            c.drawText("%.2f".format(item.qty), COL_QTY_R, yy, pNormR())
            c.drawText(item.unit.take(6), COL_UNIT_R, yy, pNormR())
            c.drawText("%.2f".format(item.sellPrice), COL_RATE_R, yy, pNormR())
            c.drawText("%.2f".format(item.lineTotal), COL_AMT_R, yy, pNormR())
            yy += 13f
            lines.drop(1).forEach { line -> c.drawText("   $line", COL_DESC, yy, pNormL()); yy += 12f }
            c.drawLine(MARGIN_L, yy - 2f, MARGIN_R, yy - 2f, pGray())
        }
        return yy
    }

    private fun drawTotalsAndFooter(c: Canvas, invoice: Invoice, settings: BusinessSettings, y: Float) {
        var yy = y + 10f
        val totX = 375f
        c.drawLine(totX, yy, MARGIN_R, yy, pGray()); yy += 13f
        c.drawText("Subtotal", totX, yy, pNormL()); c.drawText("RM %.2f".format(invoice.subtotal), COL_AMT_R, yy, pNormR()); yy += 14f
        if (invoice.discountAmount > 0) { c.drawText("Discount", totX, yy, pNormL()); c.drawText("(RM %.2f)".format(invoice.discountAmount), COL_AMT_R, yy, pNormR()); yy += 14f }
        if (invoice.roundOff != 0.0)    { c.drawText("Round Off", totX, yy, pNormL()); c.drawText("%s%.2f".format(if (invoice.roundOff >= 0) "+" else "", invoice.roundOff), COL_AMT_R, yy, pNormR()); yy += 14f }
        if (invoice.taxPercent > 0)     { val tax = invoice.subtotal * invoice.taxPercent / 100.0; c.drawText("Tax", totX, yy, pNormL()); c.drawText("RM %.2f".format(tax), COL_AMT_R, yy, pNormR()); yy += 14f }
        c.drawLine(totX, yy, MARGIN_R, yy, pLine()); yy += 14f
        c.drawText("TOTAL", totX, yy, pBoldL()); c.drawText("RM %.2f".format(invoice.totalAmount), COL_AMT_R, yy, pBoldR())
        if (invoice.amountPaid > 0 && invoice.status != InvoiceStatus.PAID) {
            yy += 14f; c.drawText("Amount Paid", totX, yy, pNormL()); c.drawText("RM %.2f".format(invoice.amountPaid), COL_AMT_R, yy, pNormR())
            yy += 14f; c.drawText("Balance Due", totX, yy, pBoldL()); c.drawText("RM %.2f".format(invoice.totalAmount - invoice.amountPaid), COL_AMT_R, yy, pBoldR())
        }
        yy += 8f; c.drawLine(MARGIN_L, yy, MARGIN_R, yy, pLine()); yy += 18f
        val footer = invoice.notes.ifBlank { "Payment via ${settings.bankName} · ${settings.accountName} · Acc: ${settings.accountNumber}" }
        footer.chunked(88).forEach { line -> c.drawText(line, MARGIN_L, yy, pSmallL()); yy += 13f }
        yy += 4f
        c.drawText("Name: ${settings.accountName}", MARGIN_L, yy, pSmallL())
        c.drawText("Authorized Signatory", MARGIN_R, yy, pSmallR())
        yy += 18f
        c.drawText("Thank you for your business.", MARGIN_L, yy, pSmallL())
    }

    private fun itemRowHeight(item: InvoiceItem): Float {
        val lines = if (item.description.length <= 32) 1 else ((item.description.length + 31) / 32)
        return 13f + (lines - 1) * 12f
    }

    // ── Public: PDF export with automatic page breaks ───────────────────

    fun printInvoice(invoice: Invoice, settings: BusinessSettings) {
        val pdf     = PdfDocument()
        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(invoice.issuedAt))
        val items   = invoice.items

        // Plan pages
        data class Page(val startIdx: Int, val endIdx: Int, val isFirst: Boolean)
        val plan = mutableListOf<Page>()
        var idx = 0
        var isFirst = true
        while (idx <= items.size) {
            val startY = if (isFirst) {
                // estimate header height
                var h = 50f + 30f + 16f + 14f
                if (invoice.customerSnapshot.company.isNotBlank()) h += 13f
                if (invoice.customerSnapshot.phone.isNotBlank())   h += 13f
                if (invoice.customerSnapshot.address.isNotBlank()) h += 13f
                h + 8f + 14f + 22f + 19f + 14f
            } else 28f + 20f + 19f + 14f
            var y = startY
            val start = idx
            while (idx < items.size) {
                val h = itemRowHeight(items[idx])
                if (y + h > BODY_MAX_Y) break
                y += h; idx++
            }
            plan.add(Page(start, idx, isFirst))
            if (idx >= items.size) break
            isFirst = false
        }
        val totalPages = plan.size

        plan.forEachIndexed { pNum, pg ->
            val info = PdfDocument.PageInfo.Builder(PAGE_W.toInt(), PAGE_H.toInt(), pNum + 1).create()
            val page = pdf.startPage(info)
            val c    = page.canvas
            var y    = if (pg.isFirst) drawHeader(c, invoice, settings, dateStr)
                       else drawContinuationHeader(c, invoice, dateStr, pNum + 1, totalPages)
            y = drawTableHeader(c, y)
            y = drawItems(c, items, pg.startIdx, pg.endIdx, y)
            if (pNum == totalPages - 1) drawTotalsAndFooter(c, invoice, settings, y)
            c.drawText("Page ${pNum + 1} of $totalPages", MARGIN_R, PAGE_H - 12f, pPageNo())
            pdf.finishPage(page)
        }

        val file = File(context.filesDir, "invoice_${invoice.invoiceNumber.replace("-", "_")}.pdf")
        FileOutputStream(file).use { pdf.writeTo(it) }
        pdf.close()
        shareFile(file, "application/pdf", "Share Invoice PDF")
    }

    // ── Public: PNG export — single tall image, full invoice ────────────

    fun exportAsPng(invoice: Invoice, settings: BusinessSettings) {
        val scale   = 2f
        val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(invoice.issuedAt))

        // Calculate exact height needed
        var totalH = 50f + 30f + 16f + 14f
        if (invoice.customerSnapshot.company.isNotBlank()) totalH += 13f
        if (invoice.customerSnapshot.phone.isNotBlank())   totalH += 13f
        if (invoice.customerSnapshot.address.isNotBlank()) totalH += 13f
        totalH += 8f + 14f + 22f + 19f + 14f  // end of header + table col header
        invoice.items.forEach { totalH += itemRowHeight(it) }
        totalH += 160f  // totals + footer
        totalH = totalH.coerceAtLeast(PAGE_H)

        val bmp    = Bitmap.createBitmap((PAGE_W * scale).toInt(), (totalH * scale).toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)
        canvas.scale(scale, scale)

        var y = drawHeader(canvas, invoice, settings, dateStr)
        y = drawTableHeader(canvas, y)
        y = drawItems(canvas, invoice.items, 0, invoice.items.size, y)
        drawTotalsAndFooter(canvas, invoice, settings, y)

        val file = File(context.filesDir, "invoice_${invoice.invoiceNumber.replace("-", "_")}.png")
        FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 95, it) }
        bmp.recycle()
        shareFile(file, "image/png", "Share Invoice Image")
    }

    private fun shareFile(file: File, mimeType: String, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, chooserTitle).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    }
}
