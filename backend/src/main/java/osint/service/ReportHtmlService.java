package osint.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class ReportHtmlService {

    private final ScanService scanService;

    @Autowired
    public ReportHtmlService(ScanService scanService) {
        this.scanService = scanService;
    }

    public String generateHtmlReport(String scanId) {
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

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String generatedDate = LocalDateTime.now().format(formatter);

        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang=\"tr\">\n");
        html.append("<head>\n");
        html.append("  <meta charset=\"UTF-8\">\n");
        html.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("  <title>CyberScope OSINT - AI Analysis Report</title>\n");
        html.append("  <style>\n");
        html.append("    * { margin: 0; padding: 0; box-sizing: border-box; }\n");
        html.append("    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif; background: #0f0f1e; color: #e0e0e0; line-height: 1.6; padding: 20px; }\n");
        html.append("    .container { max-width: 1200px; margin: 0 auto; background: #1a1a2e; border-radius: 8px; padding: 30px; box-shadow: 0 4px 6px rgba(0,0,0,0.3); }\n");
        html.append("    h1 { color: #8b5cf6; font-size: 28px; margin-bottom: 10px; border-bottom: 2px solid #8b5cf6; padding-bottom: 10px; }\n");
        html.append("    h2 { color: #a78bfa; font-size: 22px; margin-top: 30px; margin-bottom: 15px; }\n");
        html.append("    h3 { color: #c4b5fd; font-size: 18px; margin-top: 25px; margin-bottom: 12px; }\n");
        html.append("    .meta { color: #9ca3af; font-size: 14px; margin-bottom: 30px; }\n");
        html.append("    .report-section { background: #16213e; border-left: 4px solid #8b5cf6; padding: 20px; margin-bottom: 25px; border-radius: 4px; }\n");
        html.append("    .report-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; padding-bottom: 15px; border-bottom: 1px solid #2d3748; }\n");
        html.append("    .provider-target { font-weight: 600; color: #c4b5fd; font-size: 18px; }\n");
        html.append("    .content { color: #d1d5db; line-height: 1.8; }\n");
        html.append("    .content p { margin-bottom: 12px; }\n");
        html.append("    .content ul, .content ol { margin-left: 20px; margin-bottom: 12px; }\n");
        html.append("    .content li { margin-bottom: 8px; }\n");
        html.append("    .content strong { color: #fff; font-weight: 600; }\n");
        html.append("    .content code { background: #0f0f1e; padding: 2px 6px; border-radius: 3px; font-family: 'Courier New', monospace; font-size: 0.9em; }\n");
        html.append("    .content pre { background: #0f0f1e; padding: 15px; border-radius: 4px; overflow-x: auto; margin: 15px 0; }\n");
        html.append("    .content pre code { background: none; padding: 0; }\n");
        html.append("    .content blockquote { border-left: 4px solid #8b5cf6; padding-left: 15px; margin: 15px 0; color: #9ca3af; font-style: italic; }\n");
        html.append("    .content table { width: 100%; border-collapse: collapse; margin: 15px 0; }\n");
        html.append("    .content table th, .content table td { border: 1px solid #2d3748; padding: 10px; text-align: left; }\n");
        html.append("    .content table th { background: #1a1a2e; color: #c4b5fd; font-weight: 600; }\n");
        html.append("    .content table td { background: #16213e; }\n");
        html.append("    @media print { body { background: white; color: black; } .container { box-shadow: none; } }\n");
        html.append("  </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        html.append("  <div class=\"container\">\n");
        html.append("    <h1>CyberScope OSINT - AI Analysis Report</h1>\n");
        html.append("    <div class=\"meta\">\n");
        html.append("      <p><strong>Scan ID:</strong> ").append(escapeHtml(scanId)).append("</p>\n");
        html.append("      <p><strong>Generated:</strong> ").append(escapeHtml(generatedDate)).append("</p>\n");
        html.append("    </div>\n");
        html.append("    <h2>AI Analysis Reports (Gemini)</h2>\n");

        // Process each Gemini report
        for (Map.Entry<String, Object> entry : geminiReports.entrySet()) {
            String key = entry.getKey();
            Object reportData = entry.getValue();

            // Report header
            String[] keyParts = key.split("_");
            String provider = keyParts.length > 0 ? keyParts[0] : "Unknown";
            String target = keyParts.length > 1 ? keyParts[1] : "Unknown";

            html.append("    <div class=\"report-section\">\n");
            html.append("      <div class=\"report-header\">\n");
            html.append("        <div class=\"provider-target\">").append(escapeHtml(provider)).append(" - ").append(escapeHtml(target)).append("</div>\n");
            html.append("      </div>\n");
            html.append("      <div class=\"content\">\n");

            // Extract and format report content
            String reportText = extractReportText(reportData);
            String formattedContent = formatMarkdownToHtml(reportText);
            
            html.append(formattedContent);
            html.append("      </div>\n");
            html.append("    </div>\n");
        }

        html.append("  </div>\n");
        html.append("</body>\n");
        html.append("</html>\n");

        return html.toString();
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
                    fallback.append("## Özet\n\n").append(((String) summary).trim()).append("\n\n");
                }
            }
            if (reportMap.containsKey("analysis")) {
                Object analysis = reportMap.get("analysis");
                if (analysis instanceof String && ((String) analysis).trim().length() > 0) {
                    fallback.append("## Detaylı Analiz\n\n").append(((String) analysis).trim()).append("\n\n");
                }
            }
            if (reportMap.containsKey("recommendations")) {
                Object recommendations = reportMap.get("recommendations");
                if (recommendations instanceof String && ((String) recommendations).trim().length() > 0) {
                    fallback.append("## Öneriler\n\n").append(((String) recommendations).trim()).append("\n\n");
                }
            }
            if (reportMap.containsKey("keyFindings")) {
                Object keyFindings = reportMap.get("keyFindings");
                if (keyFindings instanceof String && ((String) keyFindings).trim().length() > 0) {
                    fallback.append("## Önemli Bulgular\n\n").append(((String) keyFindings).trim()).append("\n\n");
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
     * Convert markdown to HTML (simple conversion)
     */
    private String formatMarkdownToHtml(String markdown) {
        if (markdown == null || markdown.isEmpty()) {
            return "<p>No content available.</p>";
        }

        // Split into lines for processing
        String[] lines = markdown.split("\\r?\\n");
        StringBuilder html = new StringBuilder();
        boolean inCodeBlock = false;
        String codeBlockLanguage = "";

        for (String line : lines) {
            String trimmed = line.trim();

            // Handle code blocks
            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
                    html.append("</pre></code>\n");
                    inCodeBlock = false;
                    codeBlockLanguage = "";
                } else {
                    codeBlockLanguage = trimmed.length() > 3 ? trimmed.substring(3).trim() : "";
                    html.append("<pre><code");
                    if (!codeBlockLanguage.isEmpty()) {
                        html.append(" class=\"language-").append(escapeHtml(codeBlockLanguage)).append("\"");
                    }
                    html.append(">");
                    inCodeBlock = true;
                }
                continue;
            }

            if (inCodeBlock) {
                html.append(escapeHtml(line)).append("\n");
                continue;
            }

            // Handle headers
            if (trimmed.matches("^#{1,6}\\s+.*")) {
                int level = 0;
                while (level < trimmed.length() && trimmed.charAt(level) == '#') {
                    level++;
                }
                String headerText = trimmed.substring(level).trim();
                html.append("<h").append(level).append(">").append(formatInlineMarkdown(headerText)).append("</h").append(level).append(">\n");
                continue;
            }

            // Handle horizontal rules
            if (trimmed.matches("^[-*_]{3,}$")) {
                html.append("<hr>\n");
                continue;
            }

            // Handle list items
            if (trimmed.matches("^[-*+]\\s+.*")) {
                String listItem = trimmed.substring(2).trim();
                html.append("<li>").append(formatInlineMarkdown(listItem)).append("</li>\n");
                continue;
            }

            if (trimmed.matches("^\\d+\\.\\s+.*")) {
                String listItem = trimmed.replaceFirst("^\\d+\\.\\s+", "");
                html.append("<li>").append(formatInlineMarkdown(listItem)).append("</li>\n");
                continue;
            }

            // Handle blockquotes
            if (trimmed.startsWith(">")) {
                String quoteText = trimmed.substring(1).trim();
                html.append("<blockquote>").append(formatInlineMarkdown(quoteText)).append("</blockquote>\n");
                continue;
            }

            // Handle empty lines
            if (trimmed.isEmpty()) {
                html.append("<br>\n");
                continue;
            }

            // Regular paragraph
            html.append("<p>").append(formatInlineMarkdown(line)).append("</p>\n");
        }

        // Close any open code block
        if (inCodeBlock) {
            html.append("</pre></code>\n");
        }

        return html.toString();
    }

    /**
     * Format inline markdown (bold, italic, code, links)
     */
    private String formatInlineMarkdown(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String result = escapeHtml(text);

        // Bold (**text** or __text__)
        result = result.replaceAll("\\*\\*([^*]+)\\*\\*", "<strong>$1</strong>");
        result = result.replaceAll("__([^_]+)__", "<strong>$1</strong>");

        // Italic (*text* or _text_) - be careful not to conflict with bold
        result = result.replaceAll("(?<!\\*)\\*([^*]+)\\*(?!\\*)", "<em>$1</em>");
        result = result.replaceAll("(?<!_)_([^_]+)_(?!_)", "<em>$1</em>");

        // Inline code (`code`)
        result = result.replaceAll("`([^`]+)`", "<code>$1</code>");

        // Links [text](url)
        result = result.replaceAll("\\[([^\\]]+)\\]\\(([^\\)]+)\\)", "<a href=\"$2\" target=\"_blank\">$1</a>");

        return result;
    }

    /**
     * Escape HTML special characters
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#39;");
    }
}

