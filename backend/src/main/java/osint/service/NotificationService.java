package osint.service;

import osint.model.Notification;
import osint.model.NotificationPreferences;
import osint.repository.NotificationRepository;
import osint.repository.NotificationPreferencesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class NotificationService {
    
    private final NotificationRepository notificationRepository;
    private final NotificationPreferencesRepository preferencesRepository;
    
    @Autowired
    public NotificationService(NotificationRepository notificationRepository, 
                              NotificationPreferencesRepository preferencesRepository) {
        this.notificationRepository = notificationRepository;
        this.preferencesRepository = preferencesRepository;
    }
    
    @Transactional
    public Notification createNotification(Long userId, Long scanId, BigDecimal riskScore, String riskLevel, String message) {
        Notification notification = new Notification(userId, scanId, riskScore, riskLevel, message);
        return notificationRepository.save(notification);
    }
    
    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }
    
    public List<Notification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }
    
    public long getUnreadCount(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }
    
    @Transactional
    public Notification markAsRead(Integer notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setIsRead(true);
        return notificationRepository.save(notification);
    }
    
    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unreadNotifications = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        for (Notification notification : unreadNotifications) {
            notification.setIsRead(true);
        }
        notificationRepository.saveAll(unreadNotifications);
    }
    
    // Debug method: Get all notifications regardless of userId
    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }

    // Notification Preferences methods
    public NotificationPreferences getPreferences(Long userId) {
        return preferencesRepository.findByUserId(userId)
                .orElseGet(() -> {
                    // Create default preferences if not found
                    NotificationPreferences defaultPrefs = new NotificationPreferences(userId);
                    return preferencesRepository.save(defaultPrefs);
                });
    }

    @Transactional
    public NotificationPreferences savePreferences(Long userId, Map<String, Object> preferencesData) {
        Optional<NotificationPreferences> existingPrefs = preferencesRepository.findByUserId(userId);
        
        NotificationPreferences prefs;
        if (existingPrefs.isPresent()) {
            prefs = existingPrefs.get();
        } else {
            prefs = new NotificationPreferences(userId);
        }

        // Update preferences from request data
        if (preferencesData.containsKey("enableNotifications")) {
            prefs.setEnableNotifications((Boolean) preferencesData.get("enableNotifications"));
        }
        if (preferencesData.containsKey("soundAlerts")) {
            prefs.setSoundAlerts((Boolean) preferencesData.get("soundAlerts"));
        }
        if (preferencesData.containsKey("inAppNotifications")) {
            prefs.setInAppNotifications((Boolean) preferencesData.get("inAppNotifications"));
        }
        if (preferencesData.containsKey("emailNotifications")) {
            prefs.setEmailNotifications((Boolean) preferencesData.get("emailNotifications"));
        }
        if (preferencesData.containsKey("pushNotifications")) {
            prefs.setPushNotifications((Boolean) preferencesData.get("pushNotifications"));
        }
        if (preferencesData.containsKey("digestFrequency")) {
            prefs.setDigestFrequency((String) preferencesData.get("digestFrequency"));
        }

        // Update category preferences
        if (preferencesData.containsKey("categoryPreferences")) {
            @SuppressWarnings("unchecked")
            Map<String, Boolean> categoryPrefs = (Map<String, Boolean>) preferencesData.get("categoryPreferences");
            if (categoryPrefs != null) {
                if (categoryPrefs.containsKey("security")) {
                    prefs.setCategorySecurity(categoryPrefs.get("security"));
                }
                if (categoryPrefs.containsKey("scan")) {
                    prefs.setCategoryScan(categoryPrefs.get("scan"));
                }
                if (categoryPrefs.containsKey("breach")) {
                    prefs.setCategoryBreach(categoryPrefs.get("breach"));
                }
                if (categoryPrefs.containsKey("system")) {
                    prefs.setCategorySystem(categoryPrefs.get("system"));
                }
                if (categoryPrefs.containsKey("intelligence")) {
                    prefs.setCategoryIntelligence(categoryPrefs.get("intelligence"));
                }
            }
        }

        return preferencesRepository.save(prefs);
    }
}
