package osint.service;

import osint.model.User;
import osint.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserManagementService {
    private static final Logger logger = LoggerFactory.getLogger(UserManagementService.class);

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    public UserManagementService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Get all users
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getAllUsers() {
        try {
            List<User> users = userRepository.findAll();
            return users.stream()
                    .map(this::convertToResponse)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error fetching all users: {}", e.getMessage(), e);
            throw new RuntimeException("Kullanıcılar yüklenemedi: " + e.getMessage(), e);
        }
    }

    /**
     * Get user by ID
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getUserById(Long id) {
        try {
            Optional<User> userOpt = userRepository.findById(id);
            if (!userOpt.isPresent()) {
                throw new RuntimeException("Kullanıcı bulunamadı");
            }
            return convertToResponse(userOpt.get());
        } catch (Exception e) {
            logger.error("Error fetching user {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Kullanıcı bulunamadı: " + e.getMessage(), e);
        }
    }

    /**
     * Create new user
     */
    @Transactional
    public User createUser(String email, String firstName, String lastName,
            String role, String password, String phoneNumber, String userFile) {
        try {
            if (userRepository.existsByEmail(email)) {
                throw new RuntimeException("Bu email zaten kullanılıyor");
            }

            String fullName = (firstName != null && lastName != null)
                    ? firstName + " " + lastName
                    : (firstName != null ? firstName : (lastName != null ? lastName : ""));

            String passwordHash = password != null && !password.isEmpty()
                    ? passwordEncoder.encode(password)
                    : passwordEncoder.encode("TempPassword123!"); // Default password

            User user = User.builder()
                    .email(email)
                    .passwordHash(passwordHash)
                    .fullName(fullName)
                    .role(role != null ? role : "viewer")
                    .isVerified(true) // New users are active by default so they can login immediately
                    .mfaEnabled(false)
                    .phoneNumber(phoneNumber)
                    .userFile(userFile)
                    .createdAt(LocalDateTime.now())
                    .build();

            user = userRepository.save(user);
            logger.info("Created user: {} with email: {}", user.getId(), email);

            return user;
        } catch (Exception e) {
            logger.error("Error creating user: {}", e.getMessage(), e);
            throw new RuntimeException("Kullanıcı oluşturulamadı: " + e.getMessage(), e);
        }
    }

    /**
     * Update user
     */
    @Transactional
    public User updateUser(Long id, String email, String firstName, String lastName,
            String role, String status, Boolean isVerified, Boolean mfaEnabled, String phoneNumber, String userFile) {
        try {
            Optional<User> userOpt = userRepository.findById(id);
            if (!userOpt.isPresent()) {
                throw new RuntimeException("Kullanıcı bulunamadı");
            }

            User user = userOpt.get();

            // Update email if provided and different
            if (email != null && !email.isEmpty() && !email.equals(user.getEmail())) {
                if (userRepository.existsByEmail(email)) {
                    throw new RuntimeException("Bu email zaten kullanılıyor");
                }
                user.setEmail(email);
            }

            // Update full name
            if (firstName != null || lastName != null) {
                String fullName = "";
                if (firstName != null && lastName != null) {
                    fullName = firstName + " " + lastName;
                } else if (firstName != null) {
                    fullName = firstName;
                } else if (lastName != null) {
                    fullName = lastName;
                }
                user.setFullName(fullName);
            }

            // Update role
            if (role != null && !role.isEmpty()) {
                user.setRole(role);
            }

            // Update status (we'll use isVerified to track status)
            // isVerified = true means active, isVerified = false means inactive
            if (status != null) {
                // Map status to isVerified
                if ("active".equals(status)) {
                    user.setIsVerified(true);
                } else if ("inactive".equals(status) || "suspended".equals(status)) {
                    user.setIsVerified(false);
                }
            }

            if (isVerified != null) {
                user.setIsVerified(isVerified);
            }

            if (mfaEnabled != null) {
                user.setMfaEnabled(mfaEnabled);
            }

            // Update phone number
            if (phoneNumber != null) {
                user.setPhoneNumber(phoneNumber);
            }

            // Update user file
            if (userFile != null) {
                user.setUserFile(userFile);
            }

            user = userRepository.save(user);
            logger.info("Updated user: {}", id);

            return user;
        } catch (Exception e) {
            logger.error("Error updating user {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Kullanıcı güncellenemedi: " + e.getMessage(), e);
        }
    }

    /**
     * Delete user
     */
    @Transactional
    public void deleteUser(Long id) {
        try {
            Optional<User> userOpt = userRepository.findById(id);
            if (!userOpt.isPresent()) {
                throw new RuntimeException("Kullanıcı bulunamadı");
            }

            // First, delete related records in user_roles table (if exists)
            try {
                Query deleteUserRolesQuery = entityManager.createNativeQuery(
                        "DELETE FROM user_roles WHERE user_id = :userId");
                deleteUserRolesQuery.setParameter("userId", id);
                int deletedRoles = deleteUserRolesQuery.executeUpdate();
                if (deletedRoles > 0) {
                    logger.debug("Deleted {} role entries for user {}", deletedRoles, id);
                }
            } catch (Exception e) {
                // If table doesn't exist or query fails, log and continue
                logger.debug("Could not delete user_roles for user {}: {}", id, e.getMessage());
            }

            // Also delete from api_keys table if user has API keys
            try {
                Query deleteApiKeysQuery = entityManager.createNativeQuery(
                        "DELETE FROM api_keys WHERE user_id = :userId");
                deleteApiKeysQuery.setParameter("userId", id);
                int deletedKeys = deleteApiKeysQuery.executeUpdate();
                if (deletedKeys > 0) {
                    logger.debug("Deleted {} API keys for user {}", deletedKeys, id);
                }
            } catch (Exception e) {
                logger.debug("Could not delete api_keys for user {}: {}", id, e.getMessage());
            }

            // Now delete the user
            userRepository.delete(userOpt.get());
            logger.info("Deleted user: {}", id);
        } catch (Exception e) {
            logger.error("Error deleting user {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Kullanıcı silinemedi: " + e.getMessage(), e);
        }
    }

    /**
     * Suspend user
     */
    @Transactional
    public User suspendUser(Long id) {
        try {
            Optional<User> userOpt = userRepository.findById(id);
            if (!userOpt.isPresent()) {
                throw new RuntimeException("Kullanıcı bulunamadı");
            }

            User user = userOpt.get();
            user.setIsVerified(false); // Use isVerified to track suspension
            user = userRepository.save(user);
            logger.info("Suspended user: {}", id);

            return user;
        } catch (Exception e) {
            logger.error("Error suspending user {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Kullanıcı askıya alınamadı: " + e.getMessage(), e);
        }
    }

    /**
     * Activate user
     */
    @Transactional
    public User activateUser(Long id) {
        try {
            Optional<User> userOpt = userRepository.findById(id);
            if (!userOpt.isPresent()) {
                throw new RuntimeException("Kullanıcı bulunamadı");
            }

            User user = userOpt.get();
            user.setIsVerified(true);
            user = userRepository.save(user);
            logger.info("Activated user: {}", id);

            return user;
        } catch (Exception e) {
            logger.error("Error activating user {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Kullanıcı aktifleştirilemedi: " + e.getMessage(), e);
        }
    }

    /**
     * Convert User entity to response map
     */
    public Map<String, Object> convertToResponse(User user) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("email", user.getEmail());
        response.put("name", user.getFullName() != null ? user.getFullName() : user.getEmail());
        response.put("role", user.getRole() != null ? user.getRole() : "viewer");

        // Determine status based on isVerified
        String status = "active";
        if (user.getIsVerified() == null || !user.getIsVerified()) {
            status = "inactive";
        }
        response.put("status", status);

        response.put("isVerified", user.getIsVerified() != null ? user.getIsVerified() : false);
        response.put("twoFactorEnabled", user.getMfaEnabled() != null ? user.getMfaEnabled() : false);
        response.put("phoneNumber", user.getPhoneNumber());
        response.put("userFile", user.getUserFile());
        response.put("createdAt", user.getCreatedAt() != null ? user.getCreatedAt().toString() : null);
        response.put("lastLogin", user.getLastLogin() != null ? user.getLastLogin().toString() : null);

        // Generate avatar URL based on email
        String avatarSeed = user.getEmail() != null ? user.getEmail() : "default";
        response.put("avatar", "https://api.dicebear.com/7.x/avataaars/svg?seed=" + avatarSeed);

        // Permissions based on role
        List<String> permissions = getPermissionsByRole(user.getRole());
        response.put("permissions", permissions);

        // Mock data for compatibility
        response.put("lastActive", user.getLastLogin() != null ? user.getLastLogin().toString() : null);
        response.put("loginCount", 0); // Not stored in DB

        return response;
    }

    /**
     * Get permissions based on role
     */
    private List<String> getPermissionsByRole(String role) {
        if (role == null) {
            return Arrays.asList("read");
        }

        switch (role.toLowerCase()) {
            case "admin":
                return Arrays.asList("read", "write", "scan", "reports", "admin");
            case "analyst":
                return Arrays.asList("read", "write", "scan", "reports");
            case "viewer":
            default:
                return Arrays.asList("read");
        }
    }

    /**
     * Export users as CSV
     */
    @Transactional(readOnly = true)
    public String exportUsersAsCsv() {
        try {
            List<User> users = userRepository.findAll();
            StringBuilder csv = new StringBuilder();

            // CSV Header
            csv.append("ID,Email,Name,Role,Status,2FA Enabled,Created At,Last Login\n");

            // CSV Data
            for (User user : users) {
                csv.append(user.getId()).append(",");
                csv.append(escapeCsv(user.getEmail())).append(",");
                csv.append(escapeCsv(user.getFullName() != null ? user.getFullName() : user.getEmail())).append(",");
                csv.append(escapeCsv(user.getRole() != null ? user.getRole() : "viewer")).append(",");
                csv.append(user.getIsVerified() != null && user.getIsVerified() ? "active" : "inactive").append(",");
                csv.append(user.getMfaEnabled() != null && user.getMfaEnabled() ? "Yes" : "No").append(",");
                csv.append(user.getCreatedAt() != null ? user.getCreatedAt().toString() : "").append(",");
                csv.append(user.getLastLogin() != null ? user.getLastLogin().toString() : "").append("\n");
            }

            return csv.toString();
        } catch (Exception e) {
            logger.error("Error exporting users as CSV: {}", e.getMessage(), e);
            throw new RuntimeException("CSV export başarısız: " + e.getMessage(), e);
        }
    }

    /**
     * Escape CSV field values
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        // If value contains comma, quote, or newline, wrap in quotes and escape quotes
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * Get security audit report
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getSecurityAudit() {
        try {
            List<User> allUsers = userRepository.findAll();
            long totalUsers = allUsers.size();
            long activeUsers = allUsers.stream()
                    .filter(u -> u.getIsVerified() != null && u.getIsVerified())
                    .count();
            long inactiveUsers = totalUsers - activeUsers;
            long usersWithMfa = allUsers.stream()
                    .filter(u -> u.getMfaEnabled() != null && u.getMfaEnabled())
                    .count();
            long usersWithoutMfa = totalUsers - usersWithMfa;

            // Users who haven't logged in recently (last 30 days)
            LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
            long usersNeverLoggedIn = allUsers.stream()
                    .filter(u -> u.getLastLogin() == null)
                    .count();
            long usersInactive30Days = allUsers.stream()
                    .filter(u -> u.getLastLogin() != null && u.getLastLogin().isBefore(thirtyDaysAgo))
                    .count();

            // Role distribution
            Map<String, Long> roleDistribution = allUsers.stream()
                    .collect(Collectors.groupingBy(
                            u -> u.getRole() != null ? u.getRole() : "unknown",
                            Collectors.counting()));

            // Recent activity (last 7 days)
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            long usersActive7Days = allUsers.stream()
                    .filter(u -> u.getLastLogin() != null && u.getLastLogin().isAfter(sevenDaysAgo))
                    .count();

            Map<String, Object> audit = new HashMap<>();
            audit.put("totalUsers", totalUsers);
            audit.put("activeUsers", activeUsers);
            audit.put("inactiveUsers", inactiveUsers);
            audit.put("usersWithMfa", usersWithMfa);
            audit.put("usersWithoutMfa", usersWithoutMfa);
            audit.put("usersNeverLoggedIn", usersNeverLoggedIn);
            audit.put("usersInactive30Days", usersInactive30Days);
            audit.put("usersActive7Days", usersActive7Days);
            audit.put("roleDistribution", roleDistribution);
            audit.put("generatedAt", LocalDateTime.now().toString());

            // Security recommendations
            List<String> recommendations = new ArrayList<>();
            if (usersWithoutMfa > 0) {
                recommendations.add(String.format(
                        "%d kullanıcıda 2FA etkin değil. Güvenlik için 2FA'yı etkinleştirmenizi öneririz.",
                        usersWithoutMfa));
            }
            if (usersNeverLoggedIn > 0) {
                recommendations
                        .add(String.format("%d kullanıcı hiç giriş yapmamış. Bu hesapları gözden geçirmenizi öneririz.",
                                usersNeverLoggedIn));
            }
            if (usersInactive30Days > 0) {
                recommendations.add(String.format(
                        "%d kullanıcı son 30 günde aktif olmamış. Bu hesapları askıya almayı düşünebilirsiniz.",
                        usersInactive30Days));
            }
            audit.put("recommendations", recommendations);

            return audit;
        } catch (Exception e) {
            logger.error("Error generating security audit: {}", e.getMessage(), e);
            throw new RuntimeException("Güvenlik denetimi raporu oluşturulamadı: " + e.getMessage(), e);
        }
    }
}
