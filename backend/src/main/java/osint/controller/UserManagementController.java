package osint.controller;

import osint.model.User;
import osint.service.UserManagementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserManagementController {

    private final UserManagementService userManagementService;

    @Autowired
    public UserManagementController(UserManagementService userManagementService) {
        this.userManagementService = userManagementService;
    }

    /**
     * Get all users
     */
    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        try {
            List<Map<String, Object>> users = userManagementService.getAllUsers();
            return ResponseEntity.ok(Map.of("users", users, "total", users.size()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Users could not be loaded: " + e.getMessage()));
        }
    }

    /**
     * Get user by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        try {
            Map<String, Object> user = userManagementService.getUserById(id);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "User not found: " + e.getMessage()));
        }
    }

    /**
     * Create new user
     */
    @PostMapping
    public ResponseEntity<?> createUser(@RequestBody Map<String, Object> request) {
        try {
            String email = (String) request.get("email");
            String firstName = (String) request.get("firstName");
            String lastName = (String) request.get("lastName");
            String role = (String) request.get("role");
            String password = (String) request.get("password");
            String phoneNumber = (String) request.get("phoneNumber");
            String userFile = (String) request.get("userFile"); // File path from file upload
            String message = (String) request.get("message"); // Optional invitation message

            User user = userManagementService.createUser(
                    email, firstName, lastName, role, password, phoneNumber, userFile);

            return ResponseEntity.ok(userManagementService.convertToResponse(user));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "User could not be created: " + e.getMessage()));
        }
    }

    /**
     * Update user
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        try {
            String email = (String) request.get("email");
            String firstName = (String) request.get("firstName");
            String lastName = (String) request.get("lastName");
            String role = (String) request.get("role");
            String status = (String) request.get("status");
            String phoneNumber = (String) request.get("phoneNumber");
            String userFile = (String) request.get("userFile"); // File path from file upload
            Boolean isVerified = request.get("isVerified") != null
                    ? Boolean.parseBoolean(request.get("isVerified").toString())
                    : null;
            Boolean mfaEnabled = request.get("mfaEnabled") != null
                    ? Boolean.parseBoolean(request.get("mfaEnabled").toString())
                    : null;

            User user = userManagementService.updateUser(
                    id, email, firstName, lastName, role, status, isVerified, mfaEnabled, phoneNumber, userFile);

            return ResponseEntity.ok(userManagementService.convertToResponse(user));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "User could not be updated: " + e.getMessage()));
        }
    }

    /**
     * Delete user
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        try {
            userManagementService.deleteUser(id);
            return ResponseEntity.ok(Map.of("message", "User successfully deleted"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "User could not be deleted: " + e.getMessage()));
        }
    }

    /**
     * Suspend user
     */
    @PutMapping("/{id}/suspend")
    public ResponseEntity<?> suspendUser(@PathVariable Long id) {
        try {
            User user = userManagementService.suspendUser(id);
            return ResponseEntity.ok(userManagementService.convertToResponse(user));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "User could not be suspended: " + e.getMessage()));
        }
    }

    /**
     * Activate user
     */
    @PutMapping("/{id}/activate")
    public ResponseEntity<?> activateUser(@PathVariable Long id) {
        try {
            User user = userManagementService.activateUser(id);
            return ResponseEntity.ok(userManagementService.convertToResponse(user));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "User could not be activated: " + e.getMessage()));
        }
    }

    /**
     * Export users as CSV
     */
    @GetMapping("/export")
    public ResponseEntity<?> exportUsers(@RequestParam(defaultValue = "csv") String format) {
        try {
            if ("csv".equalsIgnoreCase(format)) {
                String csv = userManagementService.exportUsersAsCsv();
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.TEXT_PLAIN);
                headers.setContentDispositionFormData("attachment", "users_export.csv");
                return ResponseEntity.ok()
                        .headers(headers)
                        .body(csv);
            } else if ("json".equalsIgnoreCase(format)) {
                List<Map<String, Object>> users = userManagementService.getAllUsers();
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.setContentDispositionFormData("attachment", "users_export.json");
                return ResponseEntity.ok()
                        .headers(headers)
                        .body(Map.of("users", users, "total", users.size()));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Unsupported format. Use 'csv' or 'json'."));
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Export operation failed: " + e.getMessage()));
        }
    }

    /**
     * Get security audit report
     */
    @GetMapping("/security-audit")
    public ResponseEntity<?> getSecurityAudit() {
        try {
            Map<String, Object> audit = userManagementService.getSecurityAudit();
            return ResponseEntity.ok(audit);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Security audit report could not be created: " + e.getMessage()));
        }
    }

    /**
     * Upload file for user
     */
    @PostMapping(value = "/upload-file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "No file selected"));
            }

            // Validate file type (PDF only)
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Only PDF files can be uploaded"));
            }

            // Create uploads directory if it doesn't exist
            // Use absolute path from project root
            String projectRoot = System.getProperty("user.dir");
            String uploadDir = projectRoot + "/uploads/user-files";
            File directory = new File(uploadDir);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Generate unique filename
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
            Path filePath = Paths.get(uploadDir, uniqueFilename);

            // Save file
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Return file path (relative path)
            String relativePath = uploadDir + "/" + uniqueFilename;
            return ResponseEntity.ok(Map.of(
                    "filePath", relativePath,
                    "originalFilename", originalFilename,
                    "message", "File successfully uploaded"));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Error occurred while uploading file: " + e.getMessage()));
        }
    }

    /**
     * Download/view file for user
     */
    @GetMapping("/user-file/{userId}")
    public ResponseEntity<?> getUserFile(@PathVariable Long userId) {
        try {
            Map<String, Object> user = userManagementService.getUserById(userId);
            String filePath = (String) user.get("userFile");

            if (filePath == null || filePath.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of(
                        "error", "File not found for user"));
            }

            File file = new File(filePath);
            if (!file.exists()) {
                return ResponseEntity.status(404).body(Map.of(
                        "error", "File not found"));
            }

            Resource resource = new FileSystemResource(file);
            String contentType = "application/pdf";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.getName() + "\"")
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Error occurred while uploading file: " + e.getMessage()));
        }
    }
}
