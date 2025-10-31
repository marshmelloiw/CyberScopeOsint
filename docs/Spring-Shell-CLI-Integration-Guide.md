# Spring Shell CLI Integration Guide - CyberScope

## 📋 İçindekiler

1. [Spring Shell Nedir?](#spring-shell-nedir)
2. [Kurulum ve Bağımlılıklar](#kurulum-ve-bağımlılıklar)
3. [Temel Yapılandırma](#temel-yapılandırma)
4. [CLI Komutları Oluşturma](#cli-komutları-oluşturma)
5. [CyberScope için Örnek Komutlar](#cyberscope-için-örnek-komutlar)
6. [Çalıştırma ve Kullanım](#çalıştırma-ve-kullanım)
7. [İleri Seviye Özellikler](#ileri-seviye-özellikler)

---

## Spring Shell Nedir?

Spring Shell, Spring Boot uygulamalarına interaktif command-line interface (CLI) eklemek için kullanılan bir framework'tür. Terminal üzerinden uygulamanızı kontrol etmenizi sağlar.

### Avantajları

- ✅ Interaktif shell ortamı
- ✅ Otomatik komut tamamlama (tab completion)
- ✅ Parametre validasyonu
- ✅ Help komutları
- ✅ Komut gruplama ve organize etme
- ✅ History desteği

---

## Kurulum ve Bağımlılıklar

### 1. Maven Dependency Ekleme

`backend/pom.xml` dosyasına Spring Shell bağımlılığını ekleyin:

```xml
<dependencies>
    <!-- Mevcut bağımlılıklar... -->
    
    <!-- Spring Shell CLI -->
    <dependency>
        <groupId>org.springframework.shell</groupId>
        <artifactId>spring-shell-starter</artifactId>
        <version>3.2.0</version>
    </dependency>
</dependencies>
```

**Not:** Spring Boot 3.2.0 kullandığınız için Spring Shell 3.2.0 uyumlu versiyonunu kullanın.

### 2. Web Modülünü Devre Dışı Bırakma (Opsiyonel)

Eğer sadece CLI modunda çalıştırmak istiyorsanız, web bağımlılığını exclude edebilirsiniz veya profile ile yönetebilirsiniz:

```xml
<!-- Web'i exclude etmek için -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-tomcat</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

**Alternatif:** Profile kullanarak hem web hem CLI'ı destekleyebilirsiniz (önerilen).

---

## Temel Yapılandırma

### 1. Application Properties

`application.yml` veya `application.properties` dosyasına ekleyin:

```yaml
# application.yml
spring:
  shell:
    # Shell prompt özelleştirme
    interactive:
      enabled: true
    # Komut grupları
    command:
      stacktrace:
        enabled: true
    
# CLI ve Web'i birlikte çalıştırmak için
spring.main.web-application-type: none  # Sadece CLI için
# veya
spring.main.web-application-type: servlet  # CLI + Web birlikte
```

### 2. Spring Shell Configuration Class

Yeni bir configuration sınıfı oluşturun:

```java
package osint.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.shell.command.annotation.EnableCommand;

@Configuration
@EnableCommand
public class ShellConfig {
    // Spring Shell otomatik olarak @ShellComponent veya @CommandMethod 
    // annotation'larına sahip sınıfları tarayacak
}
```

---

## CLI Komutları Oluşturma

Spring Shell 3.x'te iki yaklaşım var:

1. **Annotation-based (Spring Shell 3.x)** - Önerilen
2. **Legacy Method-based (Spring Shell 2.x)**

### Yaklaşım 1: Annotation-based Commands (Önerilen)

#### Temel Komut Sınıfı

```java
package osint.shell.command;

import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.CommandAvailability;
import org.springframework.shell.command.annotation.Option;
import org.springframework.stereotype.Component;

@Component
@Command(group = "OSINT Commands")
public class OsintShellCommands {
    
    @Command(command = "scan", description = "Perform OSINT scan on IP, domain, or email")
    public String scan(
        @Option(required = true, description = "Target to scan (IP, domain, or email)") String target,
        @Option(description = "Scan type: ip, domain, or email", defaultValue = "ip") String type
    ) {
        return String.format("Scanning %s of type %s...", target, type);
    }
}
```

### Yaklaşım 2: Method-based Commands (Alternatif)

```java
package osint.shell.command;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@ShellComponent("OSINT Commands")
public class OsintShellCommands {
    
    @ShellMethod(key = "scan", value = "Perform OSINT scan on IP, domain, or email")
    public String scan(
        @ShellOption(value = {"-t", "--target"}, help = "Target to scan") String target,
        @ShellOption(value = {"--type"}, defaultValue = "ip", help = "Scan type") String type
    ) {
        return String.format("Scanning %s of type %s...", target, type);
    }
}
```

---

## CyberScope için Örnek Komutlar

### 1. OSINT Scan Komutları

```java
package osint.shell.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.stereotype.Component;
import osint.service.ShodanService;
import osint.service.VirusTotalService;
import osint.service.HaveIBeenPwnedService;
import reactor.core.publisher.Mono;

@Component
@Command(group = "OSINT Operations")
public class OsintScanCommands {
    
    private final ShodanService shodanService;
    private final VirusTotalService virusTotalService;
    private final HaveIBeenPwnedService hibpService;
    
    @Autowired
    public OsintScanCommands(
        ShodanService shodanService,
        VirusTotalService virusTotalService,
        HaveIBeenPwnedService hibpService
    ) {
        this.shodanService = shodanService;
        this.virusTotalService = virusTotalService;
        this.hibpService = hibpService;
    }
    
    @Command(command = "shodan", description = "Query Shodan for IP or domain information")
    public String shodanQuery(
        @Option(required = true, description = "IP address or domain") String target,
        @Option(description = "Query type: ip, domain, or search", defaultValue = "ip") String type
    ) {
        Mono<Map<String, Object>> result;
        
        switch (type.toLowerCase()) {
            case "ip":
                result = shodanService.getHostInfo(target);
                break;
            case "domain":
                result = shodanService.getDomainInfo(target);
                break;
            case "search":
                result = shodanService.searchHosts(target);
                break;
            default:
                return "Invalid type. Use: ip, domain, or search";
        }
        
        return result
            .map(data -> "Results: " + data.toString())
            .onErrorReturn("Error: " + target)
            .block();
    }
    
    @Command(command = "virustotal", description = "Query VirusTotal for threat intelligence")
    public String virusTotalQuery(
        @Option(required = true, description = "IP, domain, or URL") String target,
        @Option(description = "Query type: ip, domain, or url", defaultValue = "ip") String type
    ) {
        Mono<Map<String, Object>> result;
        
        switch (type.toLowerCase()) {
            case "ip":
                result = virusTotalService.getIpReport(target);
                break;
            case "domain":
                result = virusTotalService.getDomainReport(target);
                break;
            default:
                return "Invalid type. Use: ip or domain";
        }
        
        return result
            .map(data -> "VirusTotal Results: " + data.toString())
            .onErrorReturn("Error querying VirusTotal")
            .block();
    }
    
    @Command(command = "breach-check", description = "Check email address against HaveIBeenPwned")
    public String checkBreach(
        @Option(required = true, description = "Email address to check") String email
    ) {
        return hibpService.checkEmailBreach(email)
            .map(data -> "Breach Check Results for " + email + ": " + data.toString())
            .onErrorReturn("Error checking breaches")
            .block();
    }
}
```

### 2. User Management Komutları

```java
package osint.shell.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.stereotype.Component;
import osint.service.AuthService;
import osint.repository.UserRepository;
import osint.model.User;

@Component
@Command(group = "User Management")
public class UserManagementCommands {
    
    private final AuthService authService;
    private final UserRepository userRepository;
    
    @Autowired
    public UserManagementCommands(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }
    
    @Command(command = "user create", description = "Create a new user")
    public String createUser(
        @Option(required = true, description = "Email address") String email,
        @Option(required = true, description = "Password") String password
    ) {
        try {
            authService.register(email, password);
            return "User created successfully: " + email;
        } catch (Exception e) {
            return "Error creating user: " + e.getMessage();
        }
    }
    
    @Command(command = "user list", description = "List all users")
    public String listUsers() {
        var users = userRepository.findAll();
        StringBuilder sb = new StringBuilder("Users:\n");
        users.forEach(user -> {
            sb.append(String.format("- %s (ID: %d, Roles: %s)\n", 
                user.getEmail(), 
                user.getId(),
                user.getRoles().stream()
                    .map(r -> r.getName())
                    .reduce("", (a, b) -> a + ", " + b)));
        });
        return sb.toString();
    }
    
    @Command(command = "user delete", description = "Delete a user by email")
    public String deleteUser(
        @Option(required = true, description = "Email address") String email
    ) {
        try {
            authService.deleteUser(email);
            return "User deleted: " + email;
        } catch (Exception e) {
            return "Error deleting user: " + e.getMessage();
        }
    }
}
```

### 3. API Key Management Komutları

```java
package osint.shell.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.stereotype.Component;
import osint.service.ApiKeyService;

@Component
@Command(group = "API Key Management")
public class ApiKeyCommands {
    
    private final ApiKeyService apiKeyService;
    
    @Autowired
    public ApiKeyCommands(ApiKeyService apiKeyService) {
        this.apiKeyService = apiKeyService;
    }
    
    @Command(command = "apikey set", description = "Set API key for OSINT service")
    public String setApiKey(
        @Option(required = true, description = "Service: shodan, virustotal, or hibp") String service,
        @Option(required = true, description = "API key value") String key
    ) {
        switch (service.toLowerCase()) {
            case "shodan":
                apiKeyService.setShodanKey(key);
                return "Shodan API key set successfully";
            case "virustotal":
            case "vt":
                apiKeyService.setVirusTotalKey(key);
                return "VirusTotal API key set successfully";
            case "hibp":
            case "haveibeenpwned":
                apiKeyService.setHibpKey(key);
                return "HaveIBeenPwned API key set successfully";
            default:
                return "Invalid service. Use: shodan, virustotal, or hibp";
        }
    }
    
    @Command(command = "apikey status", description = "Check API key configuration status")
    public String checkApiKeys() {
        StringBuilder sb = new StringBuilder("API Key Status:\n");
        sb.append("- Shodan: ").append(
            apiKeyService.getShodanKey().isEmpty() ? "NOT SET" : "SET"
        ).append("\n");
        sb.append("- VirusTotal: ").append(
            apiKeyService.getVirusTotalKey().isEmpty() ? "NOT SET" : "SET"
        ).append("\n");
        sb.append("- HaveIBeenPwned: ").append(
            apiKeyService.getHibpKey().isEmpty() ? "NOT SET" : "SET"
        ).append("\n");
        return sb.toString();
    }
}
```

### 4. System Info Komutları

```java
package osint.shell.command;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.shell.command.annotation.Command;
import org.springframework.stereotype.Component;
import osint.repository.UserRepository;
import osint.repository.RoleRepository;

@Component
@Command(group = "System Information")
public class SystemInfoCommands {
    
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    
    @Autowired
    public SystemInfoCommands(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }
    
    @Command(command = "info", description = "Display system information")
    public String systemInfo() {
        long userCount = userRepository.count();
        long roleCount = roleRepository.count();
        
        return String.format(
            "CyberScope System Information:\n" +
            "- Total Users: %d\n" +
            "- Total Roles: %d\n" +
            "- Version: 1.0.0\n" +
            "- Status: Running",
            userCount, roleCount
        );
    }
    
    @Command(command = "health", description = "Check system health")
    public String health() {
        try {
            userRepository.count(); // Database connectivity check
            return "System is healthy ✓";
        } catch (Exception e) {
            return "System health check failed: " + e.getMessage();
        }
    }
}
```

---

## Çalıştırma ve Kullanım

### 1. Web Server'ı Devre Dışı Bırakma (Sadece CLI için)

`application.yml`:

```yaml
spring:
  main:
    web-application-type: none  # Sadece CLI modu
```

### 2. CLI'ı Başlatma

```bash
cd backend
mvn spring-boot:run
```

Veya JAR olarak:

```bash
mvn clean package
java -jar target/osint-backend-0.0.1-SNAPSHOT.jar
```

### 3. Interaktif Shell Kullanımı

Uygulama başladığında interaktif shell açılır:

```
shell:>help
AVAILABLE COMMANDS

OSINT Operations
    breach-check: Check email address against HaveIBeenPwned
    shodan: Query Shodan for IP or domain information
    virustotal: Query VirusTotal for threat intelligence

User Management
    user create: Create a new user
    user delete: Delete a user by email
    user list: List all users

API Key Management
    apikey set: Set API key for OSINT service
    apikey status: Check API key configuration status

System Information
    health: Check system health
    info: Display system information

Built-In Commands
    clear: Clear the shell screen
    exit, quit: Exit the shell
    help: Display help about available commands
    script: Read and execute commands from a file
    stacktrace: Display the full stacktrace of the last error
```

### 4. Komut Örnekleri

```bash
# Shodan IP sorgusu
shell:>shodan --target 8.8.8.8 --type ip

# VirusTotal domain sorgusu
shell:>virustotal --target google.com --type domain

# Email breach kontrolü
shell:>breach-check --email test@example.com

# Yeni kullanıcı oluştur
shell:>user create --email newuser@example.com --password SecurePass123!

# Kullanıcı listesi
shell:>user list

# API key ayarla
shell:>apikey set --service shodan --key YOUR_SHODAN_API_KEY

# Sistem bilgisi
shell:>info

# Çıkış
shell:>exit
```

### 5. Non-Interactive Mode (Tek Komut)

```bash
# Tek bir komut çalıştır ve çık
java -jar target/osint-backend-0.0.1-SNAPSHOT.jar --spring.shell.interactive.enabled=false shodan --target 8.8.8.8

# veya
mvn spring-boot:run -Dspring-boot.run.arguments="shodan --target 8.8.8.8"
```

---

## İleri Seviye Özellikler

### 1. Komut Gruplama ve Organizasyon

```java
@Component
@Command(group = "Advanced OSINT")
public class AdvancedOsintCommands {
    
    @Command(command = "scan", subcommands = {
        @Command(command = "ip", description = "Scan IP address"),
        @Command(command = "domain", description = "Scan domain"),
        @Command(command = "email", description = "Scan email")
    })
    public String scan() {
        return "Use: scan ip, scan domain, or scan email";
    }
    
    @Command(command = "scan ip", description = "Comprehensive IP scan")
    public String scanIp(
        @Option(required = true) String ip
    ) {
        // Implementation
        return "Scanning IP: " + ip;
    }
}
```

### 2. Validasyon ve Error Handling

```java
@Command(command = "user create")
public String createUser(
    @Option(required = true, description = "Email address") 
    @Pattern(regexp = "^[A-Za-z0-9+_.-]+@(.+)$", message = "Invalid email format")
    String email,
    
    @Option(required = true, description = "Password")
    @Size(min = 8, message = "Password must be at least 8 characters")
    String password
) {
    // Implementation
}
```

### 3. Tab Completion

Spring Shell otomatik olarak tab completion sağlar. Custom completion için:

```java
@Component
public class IpCompletionProvider implements CompletionProvider {
    
    @Override
    public List<CompletionProposal> complete(CompletionContext context) {
        return Arrays.asList(
            new CompletionProposal("8.8.8.8"),
            new CompletionProposal("1.1.1.1"),
            new CompletionProposal("192.168.1.1")
        );
    }
}
```

### 4. History ve Scripting

```bash
# Komut geçmişi
shell:>history

# Script dosyası çalıştırma
shell:>script --file commands.txt
```

### 5. Web ve CLI Birlikte Çalıştırma

Hem web hem CLI'ı birlikte çalıştırmak için profile kullanın:

```yaml
# application.yml
spring:
  main:
    web-application-type: servlet  # Hem web hem CLI

---
# application-cli.yml (CLI profile)
spring:
  main:
    web-application-type: none  # Sadece CLI

---
# application-web.yml (Web profile)
spring:
  shell:
    interactive:
      enabled: false  # CLI devre dışı
```

Çalıştırma:
```bash
# CLI modu
java -jar app.jar --spring.profiles.active=cli

# Web modu
java -jar app.jar --spring.profiles.active=web

# Her ikisi de (default)
java -jar app.jar
```

---

## Özet

✅ Spring Shell ile CyberScope'a interaktif CLI ekledik
✅ OSINT scan, user management, API key management komutları oluşturduk
✅ Hem interaktif hem non-interactive modları destekliyor
✅ Web ve CLI'ı birlikte veya ayrı çalıştırabiliyoruz
✅ Tab completion ve help sistemi otomatik çalışıyor

Bu yapı ile CyberScope uygulamanızı hem web arayüzünden hem de komut satırından yönetebilirsiniz!

