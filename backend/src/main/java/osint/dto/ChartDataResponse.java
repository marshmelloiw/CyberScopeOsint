package osint.dto;

import java.util.List;
import java.util.Map;

public class ChartDataResponse {
    
    // Vulnerability chart data
    public static class VulnerabilityData {
        private long critical;
        private long high;
        private long medium;
        private long low;
        
        public VulnerabilityData() {}
        
        public VulnerabilityData(long critical, long high, long medium, long low) {
            this.critical = critical;
            this.high = high;
            this.medium = medium;
            this.low = low;
        }
        
        public long getCritical() { return critical; }
        public void setCritical(long critical) { this.critical = critical; }
        
        public long getHigh() { return high; }
        public void setHigh(long high) { this.high = high; }
        
        public long getMedium() { return medium; }
        public void setMedium(long medium) { this.medium = medium; }
        
        public long getLow() { return low; }
        public void setLow(long low) { this.low = low; }
    }
    
    // Tool usage chart data
    public static class ToolUsageData {
        private List<Map<String, Object>> tools;
        
        public ToolUsageData() {}
        
        public ToolUsageData(List<Map<String, Object>> tools) {
            this.tools = tools;
        }
        
        public List<Map<String, Object>> getTools() { return tools; }
        public void setTools(List<Map<String, Object>> tools) { this.tools = tools; }
    }
    
    // User activity chart data
    public static class UserActivityData {
        private long active;
        private long inactive;
        private double activePercentage;
        private double inactivePercentage;
        
        public UserActivityData() {}
        
        public UserActivityData(long active, long inactive) {
            this.active = active;
            this.inactive = inactive;
            long total = active + inactive;
            this.activePercentage = total > 0 ? (active * 100.0 / total) : 0;
            this.inactivePercentage = total > 0 ? (inactive * 100.0 / total) : 0;
        }
        
        public long getActive() { return active; }
        public void setActive(long active) { this.active = active; }
        
        public long getInactive() { return inactive; }
        public void setInactive(long inactive) { this.inactive = inactive; }
        
        public double getActivePercentage() { return activePercentage; }
        public void setActivePercentage(double activePercentage) { this.activePercentage = activePercentage; }
        
        public double getInactivePercentage() { return inactivePercentage; }
        public void setInactivePercentage(double inactivePercentage) { this.inactivePercentage = inactivePercentage; }
    }
    
    // Scan status chart data
    public static class ScanStatusData {
        private List<Map<String, Object>> statuses;
        
        public ScanStatusData() {}
        
        public ScanStatusData(List<Map<String, Object>> statuses) {
            this.statuses = statuses;
        }
        
        public List<Map<String, Object>> getStatuses() { return statuses; }
        public void setStatuses(List<Map<String, Object>> statuses) { this.statuses = statuses; }
    }
}

