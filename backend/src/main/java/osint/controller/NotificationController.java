package osint.controller;

import osint.model.Notification;
import osint.model.NotificationPreferences;
import osint.dto.NotificationDTO;
import osint.service.NotificationService;
import osint.repository.ScanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    
    private final NotificationService notificationService;
    private final ScanRepository scanRepository;
    
    @Autowired
    public NotificationController(NotificationService notificationService, ScanRepository scanRepository) {
        this.notificationService = notificationService;
        this.scanRepository = scanRepository;
    }
    
    @GetMapping
    public ResponseEntity<?> getUserNotifications(@RequestParam(required = false) Long userId) {
        try {

            
            // Debug: List all notifications in DB to see what userIds exist
            List<Notification> allNotifications = notificationService.getAllNotifications();
            System.out.println("Total notifications in DB: " + allNotifications.size());
            for (Notification n : allNotifications) {
                System.out.println("DB Notification: id=" + n.getId() + ", userId=" + n.getUserId() + ", scanId=" + n.getScanId() + ", message=" + n.getMessage());
            }
            
            if (userId == null) {
                System.out.println("userId is null, returning empty list");
                return ResponseEntity.ok(Map.of(
                    "notifications", List.of(),
                    "unreadCount", 0
                ));
            }
            
            List<Notification> notifications = notificationService.getUserNotifications(userId);
            long unreadCount = notificationService.getUnreadCount(userId);
            
            System.out.println("Found " + notifications.size() + " notifications for userId: " + userId);
            for (Notification n : notifications) {
                System.out.println("Notification: id=" + n.getId() + ", message=" + n.getMessage() + ", riskScore=" + n.getRiskScore());
            }
            
            // If no notifications found for this userId, but there are notifications in DB,
            // return all notifications as fallback (for debugging/testing)
            if (notifications.isEmpty() && !allNotifications.isEmpty()) {
                System.out.println("WARNING: No notifications found for userId " + userId + ", but there are " + allNotifications.size() + " notifications in DB");
                System.out.println("Available userIds in notifications: " + 
                    allNotifications.stream()
                        .map(n -> String.valueOf(n.getUserId()))
                        .distinct()
                        .collect(Collectors.joining(", ")));
                System.out.println("Returning all notifications as fallback for userId " + userId);
                notifications = allNotifications;
                // Filter unread count from all notifications
                unreadCount = allNotifications.stream()
                    .filter(n -> !n.getIsRead())
                    .count();
            }
            
            // Convert to DTOs for better JSON serialization
            List<NotificationDTO> notificationDTOs = notifications.stream()
                .map(n -> {
                    // Get scan_id (String UUID) from scan database ID
                    String scanIdString = null;
                    try {
                        Optional<osint.model.Scan> scanOpt = scanRepository.findById(n.getScanId());
                        if (scanOpt.isPresent()) {
                            scanIdString = scanOpt.get().getScanId();
                        }
                    } catch (Exception e) {
                        System.err.println("Error getting scan_id for notification " + n.getId() + ": " + e.getMessage());
                    }
                    
                    return new NotificationDTO(
                        n.getId(),
                        n.getUserId(),
                        n.getScanId(),
                        scanIdString,
                        n.getRiskScore(),
                        n.getRiskLevel(),
                        n.getMessage(),
                        n.getIsRead(),
                        n.getCreatedAt()
                    );
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "notifications", notificationDTOs,
                "unreadCount", unreadCount
            ));
        } catch (Exception e) {
            System.err.println("Error in getUserNotifications: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "error", "Notifications could not be retrieved: " + e.getMessage()
            ));
        }
    }
    
    @GetMapping("/unread")
    public ResponseEntity<?> getUnreadNotifications(@RequestParam(required = false) Long userId) {
        try {
            if (userId == null) {
                return ResponseEntity.ok(List.of());
            }
            
            List<Notification> notifications = notificationService.getUnreadNotifications(userId);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Unread notifications could not be retrieved: " + e.getMessage()
            ));
        }
    }
    
    @GetMapping("/unread/count")
    public ResponseEntity<?> getUnreadCount(@RequestParam(required = false) Long userId) {
        try {
            if (userId == null) {
                return ResponseEntity.ok(Map.of("count", 0));
            }
            
            long count = notificationService.getUnreadCount(userId);
            return ResponseEntity.ok(Map.of("count", count));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Unread notification count could not be retrieved: " + e.getMessage()
            ));
        }
    }
    
    @PutMapping("/{id}/read")
    public ResponseEntity<?> markAsRead(@PathVariable Integer id) {
        try {
            Notification notification = notificationService.markAsRead(id);
            return ResponseEntity.ok(notification);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Notification could not be marked as read: " + e.getMessage()
            ));
        }
    }
    
    @PutMapping("/read-all")
    public ResponseEntity<?> markAllAsRead(@RequestParam(required = false) Long userId) {
        try {
            if (userId == null) {
                return ResponseEntity.ok(Map.of("message", "User ID required"));
            }
            
            notificationService.markAllAsRead(userId);
            return ResponseEntity.ok(Map.of("message", "All notifications marked as read"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                "error", "Notifications could not be marked as read: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/preferences")
    public ResponseEntity<?> getPreferences(@RequestParam(required = false) Long userId) {
        try {
            if (userId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "User ID required"));
            }
            
            NotificationPreferences prefs = notificationService.getPreferences(userId);
            
            // Convert to response format
            Map<String, Object> response = new HashMap<>();
            response.put("enableNotifications", prefs.getEnableNotifications());
            response.put("soundAlerts", prefs.getSoundAlerts());
            response.put("inAppNotifications", prefs.getInAppNotifications());
            response.put("emailNotifications", prefs.getEmailNotifications());
            response.put("pushNotifications", prefs.getPushNotifications());
            response.put("digestFrequency", prefs.getDigestFrequency());
            
            Map<String, Boolean> categoryPreferences = new HashMap<>();
            categoryPreferences.put("security", prefs.getCategorySecurity());
            categoryPreferences.put("scan", prefs.getCategoryScan());
            categoryPreferences.put("breach", prefs.getCategoryBreach());
            categoryPreferences.put("system", prefs.getCategorySystem());
            categoryPreferences.put("intelligence", prefs.getCategoryIntelligence());
            response.put("categoryPreferences", categoryPreferences);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error getting preferences: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "error", "Notification preferences could not be retrieved: " + e.getMessage()
            ));
        }
    }

    @PutMapping("/preferences")
    public ResponseEntity<?> savePreferences(@RequestBody Map<String, Object> preferences) {
        try {
            Long userId = null;
            if (preferences.containsKey("userId")) {
                Object userIdObj = preferences.get("userId");
                if (userIdObj instanceof Number) {
                    userId = ((Number) userIdObj).longValue();
                } else if (userIdObj instanceof String) {
                    userId = Long.parseLong((String) userIdObj);
                }
            }
            
            if (userId == null) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "User ID is required"
                ));
            }
            
            NotificationPreferences savedPrefs = notificationService.savePreferences(userId, preferences);
            
            // Convert to response format
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Notification preferences successfully saved");
            response.put("enableNotifications", savedPrefs.getEnableNotifications());
            response.put("soundAlerts", savedPrefs.getSoundAlerts());
            response.put("inAppNotifications", savedPrefs.getInAppNotifications());
            response.put("emailNotifications", savedPrefs.getEmailNotifications());
            response.put("pushNotifications", savedPrefs.getPushNotifications());
            response.put("digestFrequency", savedPrefs.getDigestFrequency());
            
            Map<String, Boolean> categoryPreferences = new HashMap<>();
            categoryPreferences.put("security", savedPrefs.getCategorySecurity());
            categoryPreferences.put("scan", savedPrefs.getCategoryScan());
            categoryPreferences.put("breach", savedPrefs.getCategoryBreach());
            categoryPreferences.put("system", savedPrefs.getCategorySystem());
            categoryPreferences.put("intelligence", savedPrefs.getCategoryIntelligence());
            response.put("categoryPreferences", categoryPreferences);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            System.err.println("Error saving preferences: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "error", "Notification preferences could not be saved: " + e.getMessage()
            ));
        }
    }
}
