package osint.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class ReportPdfService {

    private final ScanService scanService;

    @Autowired
    public ReportPdfService(ScanService scanService) {
        this.scanService = scanService;
    }

    public byte[] generatePdfReport(String scanId) throws IOException {
        // Get scan data
        ScanService.ScanStatus scanStatus = scanService.getScanStatus(scanId);
        if (scanStatus == null) {
            throw new RuntimeException("Scan not found: " + scanId);
        }

        Map<String, Object> results = scanStatus.getResult();
        @SuppressWarnings("unchecked")
        Map<String, Object> geminiReports = (Map<String, Object>) results.get("gemini_reports");

        if (geminiReports == null || geminiReports.isEmpty()) {
            throw new RuntimeException("No Gemini reports found for scan: " + scanId);
        }

        // Create PDF document
        PDDocument document = new PDDocument();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        try {
            // Create first page
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            float margin = 50;
            float yPosition = PDRectangle.A4.getHeight() - margin;
            float lineHeight = 20;
            float currentY = yPosition;

            // Title
            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 20);
            contentStream.newLineAtOffset(margin, currentY);
            contentStream.showText("CyberScope OSINT - AI Analysis Report");
            contentStream.endText();
            currentY -= lineHeight * 2;

            // Scan Info
            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            contentStream.newLineAtOffset(margin, currentY);
            contentStream.showText("Scan ID: " + scanId);
            contentStream.endText();
            currentY -= lineHeight;

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
            contentStream.newLineAtOffset(margin, currentY);
            contentStream.showText("Generated: " + LocalDateTime.now().format(formatter));
            contentStream.endText();
            currentY -= lineHeight * 2;

            // Gemini Reports
            contentStream.beginText();
            contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
            contentStream.newLineAtOffset(margin, currentY);
            contentStream.showText("AI Analysis Reports (Gemini)");
            contentStream.endText();
            currentY -= lineHeight * 1.5f;

            // Process each Gemini report
            for (Map.Entry<String, Object> entry : geminiReports.entrySet()) {
                String key = entry.getKey();
                Object reportData = entry.getValue();

                // Check if we need a new page
                if (currentY < margin + 100) {
                    contentStream.close();
                    page = new PDPage(PDRectangle.A4);
                    document.addPage(page);
                    contentStream = new PDPageContentStream(document, page);
                    currentY = PDRectangle.A4.getHeight() - margin;
                }

                // Report header
                String[] keyParts = key.split("_");
                String provider = keyParts.length > 0 ? keyParts[0] : "Unknown";
                String target = keyParts.length > 1 ? keyParts[1] : "Unknown";

                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 12);
                contentStream.newLineAtOffset(margin, currentY);
                contentStream.showText(provider + " - " + target);
                contentStream.endText();
                currentY -= lineHeight * 1.2f;

                // Report content - extract markdown text
                String reportText = extractReportText(reportData);

                // Clean markdown formatting for PDF (remove markdown syntax, keep plain text)
                reportText = cleanMarkdownForPdf(reportText);

                // Split text into lines that fit the page width
                float pageWidth = PDRectangle.A4.getWidth() - (2 * margin);
                PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
                String[] lines = wrapText(reportText, pageWidth, font, 10);

                for (String line : lines) {
                    if (currentY < margin + 50) {
                        contentStream.close();
                        page = new PDPage(PDRectangle.A4);
                        document.addPage(page);
                        contentStream = new PDPageContentStream(document, page);
                        currentY = PDRectangle.A4.getHeight() - margin;
                    }

                    // Only draw non-empty lines
                    if (line != null && !line.trim().isEmpty()) {
                        contentStream.beginText();
                        contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                        contentStream.newLineAtOffset(margin, currentY);
                        contentStream.showText(line);
                        contentStream.endText();
                    }
                    currentY -= lineHeight;
                }

                currentY -= lineHeight; // Space between reports
            }

            contentStream.close();

            // Save document to byte array
            document.save(outputStream);
        } finally {
            document.close();
        }

        return outputStream.toByteArray();
    }

    private String extractReportText(Object reportData) {
        if (reportData == null) {
            return "No report data available.";
        }

        if (reportData instanceof String) {
            return (String) reportData;
        }

        if (reportData instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> reportMap = (Map<String, Object>) reportData;

            // Try to extract markdown first (same as frontend resolveReportMarkdown)
            if (reportMap.containsKey("markdown")) {
                Object markdown = reportMap.get("markdown");
                if (markdown instanceof String && ((String) markdown).trim().length() > 0) {
                    return ((String) markdown).trim();
                }
            }

            // Try to extract text from common fields
            if (reportMap.containsKey("analysis")) {
                Object analysis = reportMap.get("analysis");
                if (analysis instanceof String && ((String) analysis).trim().length() > 0) {
                    return ((String) analysis).trim();
                }
            }

            if (reportMap.containsKey("raw_text")) {
                Object rawText = reportMap.get("raw_text");
                if (rawText instanceof String && ((String) rawText).trim().length() > 0) {
                    return ((String) rawText).trim();
                }
            }

            if (reportMap.containsKey("report")) {
                Object report = reportMap.get("report");
                if (report instanceof String && ((String) report).trim().length() > 0) {
                    return ((String) report).trim();
                }
            }

            if (reportMap.containsKey("content")) {
                Object content = reportMap.get("content");
                if (content instanceof String && ((String) content).trim().length() > 0) {
                    return ((String) content).trim();
                }
            }

            // Build fallback markdown (same as frontend buildFallbackMarkdown)
            StringBuilder fallback = new StringBuilder();
            if (reportMap.containsKey("summary")) {
                Object summary = reportMap.get("summary");
                if (summary instanceof String && ((String) summary).trim().length() > 0) {
                    fallback.append("## Summary\n\n").append(((String) summary).trim()).append("\n\n");
                }
            }
            if (reportMap.containsKey("analysis")) {
                Object analysis = reportMap.get("analysis");
                if (analysis instanceof String && ((String) analysis).trim().length() > 0) {
                    fallback.append("## Detailed Analysis\n\n").append(((String) analysis).trim()).append("\n\n");
                }
            }
            if (reportMap.containsKey("recommendations")) {
                Object recommendations = reportMap.get("recommendations");
                if (recommendations instanceof String && ((String) recommendations).trim().length() > 0) {
                    fallback.append("## Recommendations\n\n").append(((String) recommendations).trim()).append("\n\n");
                }
            }
            if (reportMap.containsKey("keyFindings")) {
                Object keyFindings = reportMap.get("keyFindings");
                if (keyFindings instanceof String && ((String) keyFindings).trim().length() > 0) {
                    fallback.append("## Key Findings\n\n").append(((String) keyFindings).trim()).append("\n\n");
                }
            }

            if (fallback.length() > 0) {
                return fallback.toString();
            }

            // If no specific field found, convert entire map to string
            return reportMap.toString();
        }

        return reportData.toString();
    }

    /**
     * Clean markdown formatting for PDF - removes markdown syntax but preserves
     * structure
     */
    private String cleanMarkdownForPdf(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "";
        }

        String cleaned = markdown;

        // Split into lines for line-by-line processing
        String[] lines = cleaned.split("\\r?\\n");
        StringBuilder result = new StringBuilder();

        for (String line : lines) {
            String processedLine = line;

            // Remove code blocks markers (```)
            if (processedLine.trim().startsWith("```")) {
                continue; // Skip code block markers
            }

            // Convert headers to plain text (remove # markers) - must be at start of line
            if (processedLine.matches("^\\s*#{1,6}\\s+.*")) {
                processedLine = processedLine.replaceAll("^\\s*#{1,6}\\s+", "");
            }

            // Remove horizontal rules
            if (processedLine.trim().matches("^[-*_]{3,}$")) {
                continue; // Skip horizontal rules
            }

            // Remove list markers but keep indentation
            processedLine = processedLine.replaceAll("^\\s*[-*+]\\s+", "  ");
            processedLine = processedLine.replaceAll("^\\s*\\d+\\.\\s+", "  ");

            // Remove bold markers (**text** or __text__) but keep the text
            processedLine = processedLine.replaceAll("\\*\\*([^*]+)\\*\\*", "$1");
            processedLine = processedLine.replaceAll("__([^_]+)__", "$1");

            // Remove italic markers (*text* or _text_) but keep the text (be careful with
            // bold)
            // First handle single asterisks that are not part of bold
            processedLine = processedLine.replaceAll("(?<!\\*)\\*([^*]+)\\*(?!\\*)", "$1");
            processedLine = processedLine.replaceAll("(?<!_)_([^_]+)_(?!_)", "$1");

            // Remove inline code (`code`) but keep the content
            processedLine = processedLine.replaceAll("`([^`]+)`", "$1");

            // Remove links but keep the text [text](url) -> text
            processedLine = processedLine.replaceAll("\\[([^\\]]+)\\]\\([^\\)]+\\)", "$1");

            // Remove images ![alt](url) -> alt
            processedLine = processedLine.replaceAll("!\\[([^\\]]*)\\]\\([^\\)]+\\)", "$1");

            result.append(processedLine).append("\n");
        }

        cleaned = result.toString();

        // Clean up multiple blank lines
        cleaned = cleaned.replaceAll("\\n{3,}", "\n\n");

        return cleaned.trim();
    }

    private String[] wrapText(String text, float maxWidth, PDType1Font font, float fontSize) {
        if (text == null || text.isEmpty()) {
            return new String[] { "" };
        }

        // Simple text wrapping - split by newlines first
        String[] paragraphs = text.split("\n");
        java.util.List<String> lines = new java.util.ArrayList<>();

        for (String paragraph : paragraphs) {
            if (paragraph.trim().isEmpty()) {
                lines.add("");
                continue;
            }

            // Split paragraph into words
            String[] words = paragraph.split("\\s+");
            StringBuilder currentLine = new StringBuilder();

            for (String word : words) {
                String testLine = currentLine.length() > 0
                        ? currentLine.toString() + " " + word
                        : word;

                // Estimate width (rough calculation: ~0.6 * fontSize per character)
                float estimatedWidth = testLine.length() * fontSize * 0.6f;

                if (estimatedWidth > maxWidth && currentLine.length() > 0) {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                } else {
                    if (currentLine.length() > 0) {
                        currentLine.append(" ");
                    }
                    currentLine.append(word);
                }
            }

            if (currentLine.length() > 0) {
                lines.add(currentLine.toString());
            }
        }

        return lines.toArray(new String[0]);
    }
}
