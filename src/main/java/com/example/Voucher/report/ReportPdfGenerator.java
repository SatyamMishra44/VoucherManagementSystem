package com.example.Voucher.report;

import com.example.Voucher.entity.Transaction;
import com.example.Voucher.entity.UserVoucher;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ReportPdfGenerator {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy");
    private static final DecimalFormat AMOUNT_FORMAT = new DecimalFormat("#,##0.00");

    private final String reportStoragePath;

    public ReportPdfGenerator(@Value("${report.storage.path}") String reportStoragePath) {
        this.reportStoragePath = reportStoragePath;
    }

    public Path generatePdf(Long reportJobId, ReportRenderData data) {
        try {
            Path outputDir = Path.of(reportStoragePath);
            Files.createDirectories(outputDir);
            Path outputFile = outputDir.resolve("report-" + reportJobId + ".pdf");

            String html = buildHtml(reportJobId, data);
            try (OutputStream out = Files.newOutputStream(outputFile)) {
                PdfRendererBuilder builder = new PdfRendererBuilder();
                builder.useFastMode();
                builder.withHtmlContent(html, null);
                builder.toStream(out);
                builder.run();
            }
            return outputFile;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to generate PDF report: " + ex.getMessage(), ex);
        }
    }

    private String buildHtml(Long reportJobId, ReportRenderData data) {
        StringBuilder sb = new StringBuilder(24_000);
        sb.append("<!DOCTYPE html><html><head><meta charset='UTF-8'/>");
        sb.append("<style>");
        sb.append("@page { size: A4; margin: 24mm 12mm 16mm 12mm; }");
        sb.append("body { font-family: Arial, Helvetica, sans-serif; font-size: 10px; color: #1f2937; }");
        sb.append("h1 { font-size: 16px; margin: 0 0 8px 0; color: #0f172a; }");
        sb.append("h2 { font-size: 12px; margin: 14px 0 6px 0; color: #0f172a; }");
        sb.append(".meta { border: 1px solid #d1d5db; border-radius: 4px; padding: 8px; margin-bottom: 10px; }");
        sb.append(".meta-row { margin: 2px 0; }");
        sb.append(".label { font-weight: 700; color: #111827; }");
        sb.append("table { width: 100%; border-collapse: collapse; margin-bottom: 10px; table-layout: fixed; }");
        sb.append("thead { display: table-header-group; }");
        sb.append("th { background: #e5e7eb; color: #111827; font-weight: 700; border: 1px solid #cbd5e1; padding: 6px; text-align: left; }");
        sb.append("td { border: 1px solid #e2e8f0; padding: 5px; word-wrap: break-word; }");
        sb.append("tbody tr:nth-child(even) { background: #f8fafc; }");
        sb.append(".right { text-align: right; }");
        sb.append(".small { font-size: 9px; color: #4b5563; }");
        sb.append(".footer { margin-top: 10px; font-size: 9px; color: #6b7280; }");
        sb.append("</style></head><body>");

        sb.append("<h1>").append(escape(data.getTitle())).append("</h1>");
        sb.append("<div class='meta'>");
        metaRow(sb, "Report ID", "RPT-" + reportJobId);
        metaRow(sb, "Scope", data.getScopeLabel());
        metaRow(sb, "Date Range", data.getFromDate().format(DATE_FORMATTER) + " to " + data.getToDate().format(DATE_FORMATTER));
        metaRow(sb, "Generated At", data.getGeneratedAt());
        sb.append("</div>");

        sb.append("<h2>Summary</h2>");
        sb.append("<table><thead><tr><th style='width:70%'>Metric</th><th class='right' style='width:30%'>Value</th></tr></thead><tbody>");
        summaryRow(sb, "Total Transactions", String.valueOf(data.getTotalTransactions()), false);
        summaryRow(sb, "Gross Amount", money(data.getGrossAmount()), true);
        summaryRow(sb, "Final Amount", money(data.getFinalAmount()), true);
        summaryRow(sb, "Redeemed Amount", money(data.getRedeemedAmount()), true);
        summaryRow(sb, "Total Vouchers", String.valueOf(data.getTotalVouchers()), false);
        summaryRow(sb, "Fully Redeemed Vouchers", String.valueOf(data.getFullyRedeemedVouchers()), false);
        summaryRow(sb, "Partially Redeemed Vouchers", String.valueOf(data.getPartiallyRedeemedVouchers()), false);
        summaryRow(sb, "Not Redeemed Vouchers", String.valueOf(data.getNotRedeemedVouchers()), false);
        summaryRow(sb, "Active Vouchers", String.valueOf(data.getActiveVouchers()), false);
        summaryRow(sb, "Inactive Vouchers", String.valueOf(data.getInactiveVouchers()), false);
        summaryRow(sb, "Total Voucher Purchased Amount", money(data.getTotalVoucherPurchasedAmount()), true);
        summaryRow(sb, "Total Voucher Remaining Amount", money(data.getTotalVoucherRemainingAmount()), true);
        sb.append("</tbody></table>");

        renderVoucherTable(sb, data.getVouchers());
        renderTransactionTable(sb, data.getTransactions());

        sb.append("<div class='footer'>Generated by Voucher Management System | Report ID: RPT-")
                .append(reportJobId)
                .append("</div>");
        sb.append("</body></html>");
        return sb.toString();
    }

    private void renderVoucherTable(StringBuilder sb, List<UserVoucher> vouchers) {
        sb.append("<h2>Voucher Details</h2>");
        sb.append("<table><thead><tr>")
                .append("<th style='width:7%'>Vch ID</th>")
                .append("<th style='width:7%'>User ID</th>")
                .append("<th style='width:14%'>Code</th>")
                .append("<th style='width:14%'>Issued On</th>")
                .append("<th style='width:10%'>Expiry</th>")
                .append("<th style='width:15%' class='right'>Purchased</th>")
                .append("<th style='width:15%' class='right'>Remaining</th>")
                .append("<th style='width:18%'>State/Status</th>")
                .append("</tr></thead><tbody>");

        if (vouchers.isEmpty()) {
            sb.append("<tr><td colspan='8' class='small'>No voucher records found for selected date range.</td></tr>");
        } else {
            for (UserVoucher voucher : vouchers) {
                String redemptionState = redemptionState(voucher);
                sb.append("<tr>")
                        .append("<td>").append(voucher.getId()).append("</td>")
                        .append("<td>").append(voucher.getUser().getId()).append("</td>")
                        .append("<td>").append(escape(voucher.getVoucherTemplate().getCode())).append("</td>")
                        .append("<td>").append(voucher.getPurchasedAt().format(DATE_TIME_FORMATTER)).append("</td>")
                        .append("<td>").append(voucher.getVoucherTemplate().getExpiryDate().format(DATE_FORMATTER)).append("</td>")
                        .append("<td class='right'>").append(money(voucher.getTotalPurchasedAmount())).append("</td>")
                        .append("<td class='right'>").append(money(voucher.getRemainingBalance())).append("</td>")
                        .append("<td>").append(escape(redemptionState + " / " + voucher.getStatus().name())).append("</td>")
                        .append("</tr>");
            }
        }
        sb.append("</tbody></table>");
    }

    private void renderTransactionTable(StringBuilder sb, List<Transaction> transactions) {
        sb.append("<h2>Transaction Details</h2>");
        sb.append("<table><thead><tr>")
                .append("<th style='width:8%'>Txn ID</th>")
                .append("<th style='width:16%'>Time</th>")
                .append("<th style='width:8%'>User</th>")
                .append("<th style='width:8%'>Bill</th>")
                .append("<th style='width:18%' class='right'>Total</th>")
                .append("<th style='width:18%' class='right'>Final</th>")
                .append("<th style='width:18%' class='right'>Redeemed</th>")
                .append("<th style='width:6%'> </th>")
                .append("</tr></thead><tbody>");

        if (transactions.isEmpty()) {
            sb.append("<tr><td colspan='8' class='small'>No transaction records found for selected date range.</td></tr>");
        } else {
            for (Transaction transaction : transactions) {
                BigDecimal redeemed = transaction.getTotalAmount().subtract(transaction.getFinalAmount());
                sb.append("<tr>")
                        .append("<td>").append(transaction.getId()).append("</td>")
                        .append("<td>").append(transaction.getCreatedAt().format(DATE_TIME_FORMATTER)).append("</td>")
                        .append("<td>").append(transaction.getUser().getId()).append("</td>")
                        .append("<td>").append(transaction.getBill().getId()).append("</td>")
                        .append("<td class='right'>").append(money(transaction.getTotalAmount())).append("</td>")
                        .append("<td class='right'>").append(money(transaction.getFinalAmount())).append("</td>")
                        .append("<td class='right'>").append(money(redeemed)).append("</td>")
                        .append("<td></td>")
                        .append("</tr>");
            }
        }
        sb.append("</tbody></table>");
    }

    private void metaRow(StringBuilder sb, String label, String value) {
        sb.append("<div class='meta-row'><span class='label'>")
                .append(escape(label))
                .append(":</span> ")
                .append(escape(value))
                .append("</div>");
    }

    private void summaryRow(StringBuilder sb, String metric, String value, boolean numeric) {
        sb.append("<tr><td>").append(escape(metric)).append("</td><td")
                .append(numeric ? " class='right'>" : ">")
                .append(escape(value))
                .append("</td></tr>");
    }

    private String redemptionState(UserVoucher voucher) {
        if (voucher.getRemainingBalance().compareTo(BigDecimal.ZERO) == 0) {
            return "FULLY_REDEEMED";
        }
        if (voucher.getRemainingBalance().compareTo(voucher.getTotalPurchasedAmount()) == 0) {
            return "NOT_REDEEMED";
        }
        return "PARTIALLY_REDEEMED";
    }

    private String money(BigDecimal value) {
        if (value == null) {
            return "INR 0.00";
        }
        return "INR " + AMOUNT_FORMAT.format(value);
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}

