# 🔧 CyberScope Android App - Troubleshooting Guide

## Common Issues and Solutions

---

## ✅ FIXED: SLF4J LoggerFactory Error

### Problem
```
Unable to load class 'org.slf4j.LoggerFactory'
Gradle's dependency cache may be corrupt
```

### Solution Applied
The issue was caused by conflicting dependencies (MPAndroidChart and Accompanist SwipeRefresh). These have been removed from the project.

### What Was Changed
1. **Root build.gradle**: Added `allprojects` block with repositories
2. **App build.gradle**: Removed problematic dependencies:
   - `com.github.PhilJay:MPAndroidChart:v3.1.0`
   - `com.google.accompanist:accompanist-swiperefresh:0.32.0`

### Next Steps
1. **Sync Gradle**: File → Sync Project with Gradle Files
2. **Clean Project**: Build → Clean Project
3. **Rebuild**: Build → Rebuild Project

If still having issues, try:
```bash
# Stop Gradle daemons
./gradlew --stop

# Clear Gradle cache (in project directory)
rm -rf .gradle
rm -rf build
rm -rf app/build

# Then sync again in Android Studio
```

---

## 🐛 Other Common Issues

### 1. Gradle Sync Failed

**Problem**: Gradle sync fails with various errors

**Solutions**:
1. **Invalidate Caches**:
   - File → Invalidate Caches → Invalidate and Restart

2. **Delete Gradle Files**:
   ```bash
   # Delete these folders
   .gradle/
   .idea/
   build/
   app/build/
   ```

3. **Update Gradle Wrapper**:
   ```bash
   ./gradlew wrapper --gradle-version=8.2
   ```

4. **Check Internet Connection**: Ensure you can download dependencies

---

### 2. SDK Not Found

**Problem**: Android SDK location not set

**Solutions**:
1. **Set SDK Location**:
   - File → Project Structure → SDK Location
   - Set Android SDK location (usually `C:\Users\<username>\AppData\Local\Android\Sdk`)

2. **Create local.properties**:
   ```properties
   sdk.dir=C\:\\Users\\<username>\\AppData\\Local\\Android\\Sdk
   ```

---

### 3. Kotlin Compiler Error

**Problem**: Kotlin compiler version mismatch

**Solutions**:
1. **Check Kotlin Version**: Ensure all Kotlin versions match
   - Root build.gradle: `1.9.20`
   - Compose compiler: `1.5.4`

2. **Update Kotlin Plugin**:
   - File → Settings → Plugins → Update Kotlin plugin

---

### 4. Compose Version Conflicts

**Problem**: Compose dependencies conflict

**Solutions**:
1. **Use BOM**: We're using Compose BOM `2023.10.01`
   ```gradle
   implementation platform('androidx.compose:compose-bom:2023.10.01')
   ```

2. **Don't Specify Versions**: For Compose dependencies, let BOM manage versions
   ```gradle
   implementation 'androidx.compose.ui:ui'  // No version!
   ```

---

### 5. Build Takes Too Long

**Problem**: Gradle build is very slow

**Solutions**:
1. **Enable Gradle Daemon**: In `gradle.properties`:
   ```properties
   org.gradle.daemon=true
   org.gradle.parallel=true
   org.gradle.configureondemand=true
   ```

2. **Increase Memory**: In `gradle.properties`:
   ```properties
   org.gradle.jvmargs=-Xmx4096m -XX:MaxPermSize=1024m
   ```

3. **Use Build Cache**:
   ```properties
   org.gradle.caching=true
   ```

---

### 6. App Crashes on Launch

**Problem**: App crashes immediately after launch

**Solutions**:
1. **Check Logcat**: View → Tool Windows → Logcat
   - Look for red error messages
   - Check stack trace

2. **Common Causes**:
   - Missing permissions in AndroidManifest.xml
   - Network security configuration
   - Incorrect backend URL

3. **Verify Backend URL**: In `ApiClient.kt`:
   ```kotlin
   // For emulator
   private var baseUrl = "http://10.0.2.2:8080/api/"
   
   // For physical device
   private var baseUrl = "http://YOUR_IP:8080/api/"
   ```

---

### 7. Network Connection Failed

**Problem**: Cannot connect to backend API

**Solutions**:
1. **Check Backend**: Ensure backend is running on port 8080
   ```bash
   cd backend
   mvn spring-boot:run
   ```

2. **Check URL**:
   - Emulator: Use `10.0.2.2` instead of `localhost`
   - Device: Use computer's IP address (not localhost)

3. **Check Firewall**: Allow connections on port 8080

4. **Verify Network**: Device and computer on same network

5. **Test Backend**: In browser, visit:
   ```
   http://localhost:8080/api/scans
   ```

---

### 8. Manifest Merger Failed

**Problem**: AndroidManifest.xml merge conflicts

**Solutions**:
1. **Check for Duplicates**: Look for duplicate permissions or activities

2. **Add Tools Namespace**:
   ```xml
   <manifest xmlns:android="http://schemas.android.com/apk/res/android"
       xmlns:tools="http://schemas.android.com/tools">
   ```

3. **Override Conflicts**:
   ```xml
   <application
       android:usesCleartextTraffic="true"
       tools:replace="android:usesCleartextTraffic">
   ```

---

### 9. R Class Not Generated

**Problem**: Cannot resolve symbol 'R'

**Solutions**:
1. **Clean and Rebuild**:
   - Build → Clean Project
   - Build → Rebuild Project

2. **Check XML Files**: Look for errors in XML resources

3. **Sync Gradle**: File → Sync Project with Gradle Files

4. **Invalidate Caches**: File → Invalidate Caches → Restart

---

### 10. Dependency Resolution Failed

**Problem**: Cannot resolve dependencies

**Solutions**:
1. **Check Repositories**: Ensure repositories are configured:
   ```gradle
   repositories {
       google()
       mavenCentral()
       maven { url 'https://jitpack.io' }
   }
   ```

2. **Clear Gradle Cache**:
   ```bash
   ./gradlew clean
   ./gradlew --refresh-dependencies
   ```

3. **Check Internet**: Verify internet connection

4. **Use VPN**: If behind firewall, try VPN

---

## 🔄 Complete Reset Procedure

If all else fails, try this complete reset:

### Step 1: Close Android Studio
Close Android Studio completely

### Step 2: Delete Build Files
```bash
cd CyberScope/mobile

# Delete Gradle files
rm -rf .gradle
rm -rf .idea
rm -rf build
rm -rf app/build

# On Windows PowerShell:
Remove-Item -Recurse -Force .gradle
Remove-Item -Recurse -Force .idea
Remove-Item -Recurse -Force build
Remove-Item -Recurse -Force app\build
```

### Step 3: Delete Global Gradle Cache (Optional)
```bash
# Windows
rm -rf C:\Users\<username>\.gradle\caches

# Linux/Mac
rm -rf ~/.gradle/caches
```

### Step 4: Reopen Project
1. Open Android Studio
2. Open project: `CyberScope/mobile`
3. Wait for Gradle sync
4. Build → Clean Project
5. Build → Rebuild Project

---

## 📞 Getting Help

### Check Logs
1. **Gradle Console**: View → Tool Windows → Build
2. **Logcat**: View → Tool Windows → Logcat
3. **Event Log**: View → Tool Windows → Event Log

### Useful Commands
```bash
# Check Gradle version
./gradlew --version

# List all tasks
./gradlew tasks

# Build with stack trace
./gradlew build --stacktrace

# Build with debug info
./gradlew build --debug

# Stop all Gradle daemons
./gradlew --stop
```

### System Requirements
- ✅ Android Studio Hedgehog (2023.1.1) or later
- ✅ JDK 17 or higher
- ✅ Gradle 8.2
- ✅ Android SDK API 24-34
- ✅ 8GB RAM minimum (16GB recommended)
- ✅ 5GB free disk space

---

## ✅ Verification Checklist

Before reporting issues, verify:
- [ ] Android Studio is up to date
- [ ] JDK 17 is installed and configured
- [ ] Android SDK is installed (API 24-34)
- [ ] Gradle sync completed successfully
- [ ] No errors in build.gradle files
- [ ] Internet connection is working
- [ ] Backend is running (if testing API)
- [ ] Correct backend URL is configured

---

## 🎯 Quick Fixes Summary

| Issue | Quick Fix |
|-------|-----------|
| Gradle sync failed | Invalidate Caches → Restart |
| SDK not found | Set SDK location in Project Structure |
| Dependency error | Clean → Rebuild Project |
| R class missing | Sync Gradle → Clean → Rebuild |
| Build too slow | Increase Gradle memory in gradle.properties |
| Network error | Check backend URL and firewall |
| App crashes | Check Logcat for errors |
| Manifest error | Check for duplicates, add tools namespace |

---

## 📚 Additional Resources

### Official Documentation
- [Android Developer Guide](https://developer.android.com/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Kotlin Documentation](https://kotlinlang.org/docs/home.html)
- [Gradle User Guide](https://docs.gradle.org/)

### Troubleshooting Links
- [Android Studio Troubleshooting](https://developer.android.com/studio/troubleshoot)
- [Gradle Troubleshooting](https://docs.gradle.org/current/userguide/troubleshooting.html)
- [Stack Overflow - Android](https://stackoverflow.com/questions/tagged/android)

---

**Last Updated**: January 2, 2026  
**Version**: 1.0.0  

**If you encounter an issue not listed here, please check the Android Studio Event Log and Logcat for specific error messages.**
