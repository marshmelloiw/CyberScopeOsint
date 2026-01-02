# CyberScope Android App - Quick Start Guide

## 🚀 Quick Setup (5 Minutes)

### Step 1: Open in Android Studio
1. Launch **Android Studio**
2. Click **Open** → Navigate to `CyberScope/mobile`
3. Wait for Gradle sync to complete

### Step 2: Configure Backend Connection

**For Android Emulator** (Default):
- No changes needed! Already configured for `http://10.0.2.2:8080/api/`

**For Physical Device**:
1. Find your computer's IP address:
   - Windows: Open CMD → Type `ipconfig`
   - Mac/Linux: Open Terminal → Type `ifconfig`
   - Look for IPv4 address (e.g., `192.168.1.100`)

2. Edit `app/src/main/java/com/cyberscope/reports/data/api/ApiClient.kt`:
   ```kotlin
   private var baseUrl = "http://YOUR_IP_HERE:8080/api/"
   // Example: "http://192.168.1.100:8080/api/"
   ```

### Step 3: Run the App
1. Connect your device or start an emulator
2. Click the **Run** button (green play icon) in Android Studio
3. Select your device/emulator
4. Wait for the app to install and launch

### Step 4: Ensure Backend is Running
Make sure the CyberScope backend is running:
```bash
cd backend
mvn spring-boot:run
```

## ✅ Verification

The app should:
- ✅ Launch successfully
- ✅ Show "Scans" and "Reports" tabs
- ✅ Load data from the backend
- ✅ Display scans in a list

## 🐛 Common Issues

### "Connection failed" error
- ✅ Backend is running on port 8080
- ✅ Correct IP address in ApiClient.kt
- ✅ Firewall allows connections
- ✅ Device and computer on same network

### "Gradle sync failed"
- ✅ Internet connection active
- ✅ Android Studio updated
- ✅ JDK 17 installed

### App crashes
- ✅ Check Logcat in Android Studio
- ✅ Clean and rebuild project
- ✅ Invalidate caches and restart

## 📱 Testing

### Test with Sample Data
1. Create a scan in the web frontend
2. Pull down to refresh in the mobile app
3. Tap on a scan to view details
4. Navigate to Reports tab to see AI analysis

## 🎨 Features to Try

1. **View Scans**: Browse all security scans
2. **Scan Details**: Tap any scan for detailed information
3. **Reports**: Check Gemini AI analysis
4. **Refresh**: Pull down to refresh data
5. **Dark Mode**: Toggle system dark mode to see theme change

## 📞 Need Help?

- Check the full README.md for detailed documentation
- Review Logcat for error messages
- Ensure backend API is accessible

---

**Happy Testing! 🎉**
