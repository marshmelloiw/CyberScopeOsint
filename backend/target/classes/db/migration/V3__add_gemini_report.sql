-- Add Gemini AI analysis report column to scan_results table
ALTER TABLE scan_results ADD COLUMN gemini_report JSONB;

-- Create index for better query performance
CREATE INDEX IF NOT EXISTS idx_scan_results_gemini_report ON scan_results USING GIN (gemini_report);

