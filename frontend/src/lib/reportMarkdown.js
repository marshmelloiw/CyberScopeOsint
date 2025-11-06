const buildFallbackMarkdown = (reportData) => {
  if (!reportData || typeof reportData !== 'object') {
    return null;
  }

  const sections = [];

  if (typeof reportData.summary === 'string' && reportData.summary.trim()) {
    sections.push(`## Özet\n\n${reportData.summary.trim()}`);
  }

  if (typeof reportData.analysis === 'string' && reportData.analysis.trim()) {
    sections.push(`## Detaylı Analiz\n\n${reportData.analysis.trim()}`);
  }

  if (typeof reportData.recommendations === 'string' && reportData.recommendations.trim()) {
    sections.push(`## Öneriler\n\n${reportData.recommendations.trim()}`);
  }

  if (typeof reportData.keyFindings === 'string' && reportData.keyFindings.trim()) {
    sections.push(`## Önemli Bulgular\n\n${reportData.keyFindings.trim()}`);
  }

  if (sections.length === 0) {
    return reportData && Object.keys(reportData).length
      ? `\n\n\`\`\`json\n${JSON.stringify(reportData, null, 2)}\n\`\`\``
      : null;
  }

  return sections.join('\n\n');
};

export const resolveReportMarkdown = (report) => {
  if (!report) {
    return null;
  }

  if (typeof report === 'string') {
    return report;
  }

  if (typeof report.markdown === 'string' && report.markdown.trim()) {
    return report.markdown.trim();
  }

  if (typeof report.analysis === 'string' && report.analysis.trim()) {
    return report.analysis.trim();
  }

  if (typeof report.raw_text === 'string' && report.raw_text.trim()) {
    return report.raw_text.trim();
  }

  return buildFallbackMarkdown(report);
};

