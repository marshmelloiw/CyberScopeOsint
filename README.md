# CyberScope OSINT Platform - User Manual

## 📋 Table of Contents
1. [System Requirements](#system-requirements)
2. [Installation Steps](#installation-steps)
3. [Database Setup](#database-setup)
4. [Backend Configuration](#backend-configuration)
5. [Frontend Configuration](#frontend-configuration)
6. [Running the Application](#running-the-application)
7. [First Login](#first-login)
8. [API Keys Configuration](#api-keys-configuration)
9. [Troubleshooting](#troubleshooting)
10. [User Guide](#user-guide)

---

## 🖥️ System Requirements

### Required Software
- **Java JDK 17** or higher
- **Node.js 18** or higher
- **PostgreSQL 15** or higher
- **Maven 3.8** or higher
- **Git** (for cloning the project)

### Recommended System
- **Operating System**: Windows 10/11, macOS, Linux
- **RAM**: Minimum 8GB (16GB recommended)
- **Disk Space**: Minimum 5GB free space

---

## 📥 Installation Steps

### 1. Clone the Project

```bash
git clone https://github.com/marshmelloiw/CyberScopeOsint.git
cd CyberScopeOsint
```

---

## 🗄️ Database Setup

### Step 1: Install PostgreSQL

#### Windows
1. Download [PostgreSQL](https://www.postgresql.org/download/windows/)
2. Set a password during installation (e.g., `postgres`)
3. Default port: `5432`

#### macOS
```bash
brew install postgresql@15
brew services start postgresql@15
```

#### Linux (Ubuntu/Debian)
```bash
sudo apt update
sudo apt install postgresql postgresql-contrib
sudo systemctl start postgresql
```

### Step 2: Create Database

Connect using **pgAdmin** or **psql**:

```bash
psql -U postgres
```

Create the database:

```sql
CREATE DATABASE cyberscope_local;
CREATE USER cyber WITH PASSWORD 'your_secure_password';
GRANT ALL PRIVILEGES ON DATABASE cyberscope_local TO cyber;
\q
```

### Step 3: Load Database Schema

#### Method 1: Using pgAdmin (Recommended)

1. Open **pgAdmin**
2. Navigate to **Servers** → **PostgreSQL** → **Databases** → **cyberscope_local**
3. Right-click → **Query Tool**
4. Execute SQL files from `init-db` folder **in order**:
   - `00_sequences.sql`
   - `01_users.sql`
   - `02_roles.sql`
   - `03_user_roles.sql`
   - `04_scans.sql`
   - `05_scan_targets.sql`
   - `06_scan_providers.sql`
   - `07_scan_results.sql`
   - `08_scan_logs.sql`
   - `09_api_keys.sql`
   - `10_notifications.sql`
   - `11_notification_preferences.sql`
   - `12_password_reset_tokens.sql`
   - `13_refresh_tokens.sql`

#### Method 2: Using Command Line

**Windows PowerShell:**
```powershell
cd init-db
Get-ChildItem -Filter *.sql | Sort-Object Name | ForEach-Object {
    Write-Host "Executing: $($_.Name)"
    psql -U cyber -d cyberscope_local -f $_.FullName
}
```

**Linux/macOS:**
```bash
cd init-db
for file in *.sql; do
    echo "Executing: $file"
    psql -U cyber -d cyberscope_local -f "$file"
done
```

### Step 4: Verify Database

```sql
psql -U cyber -d cyberscope_local
\dt  -- List all tables
```

Expected output: 13 tables (users, roles, scans, etc.)

---

## ⚙️ Backend Configuration

### Step 1: Install Dependencies

```bash
cd backend
mvn clean install
```

### Step 2: Configure Application

Edit `backend/src/main/resources/application.yml`:

```yaml
spring:
  application:
    name: cyberscope-osint
  
  datasource:
    url: jdbc:postgresql://localhost:5432/cyberscope_local
    username: cyber
    password: your_database_password
  
  jpa:
    hibernate:
      ddl-auto: none
    show-sql: false
  
  flyway:
    enabled: false

server:
  port: 8080

security:
  jwt:
    secret: CHANGE_THIS_SECRET_KEY_MIN_256_BITS
    expiration: 3600
    refresh-expiration: 604800

# OSINT API Keys (Optional)
osint:
  shodan:
    api-key: YOUR_SHODAN_API_KEY
  virustotal:
    api-key: YOUR_VIRUSTOTAL_API_KEY
  haveibeenpwned:
    api-key: YOUR_HIBP_API_KEY
  gemini:
    api-key: YOUR_GEMINI_API_KEY
  zap:
    url: http://localhost:8090
    api-key: YOUR_ZAP_API_KEY
```

**IMPORTANT**: Change `jwt.secret` in production!

---

## 🎨 Frontend Configuration

### Step 1: Install Dependencies

```bash
cd frontend
npm install
```

### Step 2: Configure API URL

Check `frontend/src/config/api.js`:

```javascript
const API_BASE_URL = 'http://localhost:8080/api';
export default API_BASE_URL;
```

---

## 🚀 Running the Application

### Start Backend

```bash
cd backend
mvn spring-boot:run
```

**Success output:**
```
Started OsintBackendApplication in X.XXX seconds
Tomcat started on port 8080 (http)
```

### Start Frontend

Open a **new terminal**:

```bash
cd frontend
npm run dev
```

**Success output:**
```
VITE v5.x.x  ready in XXX ms
➜  Local:   http://localhost:5173/
```

### Access the Application

- **Frontend**: http://localhost:5173
- **Backend API**: http://localhost:8080/api
- **Health Check**: http://localhost:8080/actuator/health

---

## 🔐 First Login

### Check Existing Users

```sql
psql -U cyber -d cyberscope_local
SELECT email, role FROM users;
```

### Create Admin User (if needed)

```sql
-- Create admin role
INSERT INTO roles (id, name) 
VALUES (nextval('roles_id_seq'), 'ROLE_ADMIN') 
ON CONFLICT DO NOTHING;

-- Create admin user (set your own secure password)
INSERT INTO users (
    user_id, email, password_hash, full_name, 
    role, is_verified, mfa_enabled, created_at
)
VALUES (
    nextval('users_user_id_seq'),
    'admin@cyberscope.com',
    '[Generate your own password hash using BCrypt]',
    'System Administrator',
    'admin',
    'true',
    'false',
    CURRENT_TIMESTAMP::text
);
```

**Login Credentials:**
- Email: `admin@example.com`
- Password: `[secure password]`

**⚠️ IMPORTANT**: Change password after first login!

---

## 🔑 API Keys Configuration

### Shodan API
1. Create account at [Shodan.io](https://account.shodan.io/)
2. Get API key from **My Account**
3. Add to `application.yml`

**Pricing**: Free plan (100 queries/month)

### VirusTotal API
1. Create account at [VirusTotal](https://www.virustotal.com/)
2. Get API key from **Profile** → **API Key**
3. Add to `application.yml`

**Pricing**: Free plan (500 queries/day)

### HaveIBeenPwned API
1. Purchase API key at [HIBP API](https://haveibeenpwned.com/API/Key) ($3.50/month)
2. Add to `application.yml`

### Google Gemini API
1. Create account at [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Click **Get API Key** → **Create API Key**
3. Add to `application.yml`

**Pricing**: Free plan (60 queries/minute)

### OWASP ZAP
1. Download and install [ZAP](https://www.zaproxy.org/download/)
2. Start ZAP (default port: 8090)
3. Get API key: **Tools** → **Options** → **API**
4. Add to `application.yml`

---

## 🛠️ Troubleshooting

### Port Already in Use

**Error**: `Port 8080 already in use`

**Solution:**

**Windows:**
```powershell
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

**Linux/macOS:**
```bash
lsof -ti:8080 | xargs kill -9
```

### Database Connection Error

**Error**: `Connection refused`

**Solution:**

1. Check if PostgreSQL is running:
   ```bash
   # Windows: services.msc → PostgreSQL
   # Linux: sudo systemctl status postgresql
   # macOS: brew services list | grep postgresql
   ```

2. Verify credentials in `application.yml`

3. Check if database exists:
   ```sql
   psql -U postgres
   \l
   ```

### Maven Build Error

**Error**: `BUILD FAILURE`

**Solution:**
```bash
mvn clean install -U
# Or skip tests:
mvn clean install -DskipTests
```

### NPM Install Error

**Error**: `npm ERR!`

**Solution:**
```bash
rm -rf node_modules package-lock.json
npm cache clean --force
npm install
```

---

## 📊 User Guide

### 1. Dashboard
- View system overview
- Recent scans
- Risk distribution
- Notifications

### 2. Creating a Scan

1. Go to **Scans** menu
2. Click **New Scan**
3. Enter scan details:
   - **Scan Name**: e.g., "Google Security Scan"
   - **Target Type**: Domain, IP, Email, URL, or Social
   - **Target Value**: e.g., google.com
   - **Priority**: Low, Medium, High, Critical
4. Select scan tools (Shodan, VirusTotal, etc.)
5. Click **Start Scan**

### 3. Viewing Results

1. Go to **Scans** menu
2. Click on a scan
3. View **Results** tab
4. Export report (PDF/Excel/JSON)

### 4. User Management (Admin only)

1. Go to **Users** menu
2. Click **Add User**
3. Assign roles:
   - **Admin**: Full access
   - **Analyst**: Create and analyze scans
   - **Viewer**: View-only access

---

## 🔒 Security Recommendations

### Production Environment

1. **Change JWT Secret**: Use a strong, random secret (min 256 bits)
2. **Change Default Passwords**: Update all default credentials
3. **Use HTTPS**: Enable SSL/TLS in production
4. **Secure API Keys**: Store in environment variables
5. **Enable Firewall**: Close unnecessary ports
6. **Regular Updates**: Keep dependencies up to date

### Password Policy

- Minimum 8 characters
- At least 1 uppercase letter
- At least 1 lowercase letter
- At least 1 number
- At least 1 special character

---

## 📞 Support

- **GitHub Issues**: https://github.com/marshmelloiw/CyberScopeOsint/issues
- **Email**: support@cyberscope.com
- **Documentation**: https://docs.cyberscope.com

---

## 📝 License

This project is licensed under the [MIT License](LICENSE).

---

## 🙏 Thank You

Thank you for using CyberScope OSINT Platform!

**Happy Scanning! 🔍🛡️**

---

**Last Updated**: January 2, 2026  
**Version**: 1.0.0
