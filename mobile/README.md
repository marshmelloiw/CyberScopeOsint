# CyberScope Reports - Android App

A modern Android application for viewing CyberScope OSINT scan reports and analysis results.

## 📱 Features

### ✨ Core Functionality
- **View All Scans**: Browse all security scans with detailed information
- **Gemini Reports**: Access AI-powered security analysis reports
- **Real-time Updates**: Pull-to-refresh functionality for latest data
- **Detailed Analysis**: View comprehensive scan results and findings
- **Risk Assessment**: Color-coded risk levels (Low, Medium, High, Critical)
- **Provider Results**: See results from multiple OSINT providers (Shodan, VirusTotal, etc.)

### 🎨 Modern UI/UX
- **Material Design 3**: Latest Material You design system
- **Dark/Light Theme**: Automatic theme switching based on system settings
- **Cyber-themed Colors**: Professional blue, purple, and green color scheme
- **Smooth Animations**: Polished transitions and interactions
- **Responsive Layout**: Optimized for all screen sizes
- **Bottom Navigation**: Easy navigation between Scans and Reports

### 🔧 Technical Features
- **Jetpack Compose**: Modern declarative UI framework
- **MVVM Architecture**: Clean separation of concerns
- **Kotlin Coroutines**: Efficient asynchronous operations
- **Retrofit**: Type-safe HTTP client for API communication
- **StateFlow**: Reactive state management
- **Material 3 Components**: Latest UI components

## 🏗️ Architecture

```
mobile/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/cyberscope/reports/
│   │   │   │   ├── data/
│   │   │   │   │   ├── api/          # Retrofit API interfaces
│   │   │   │   │   ├── model/        # Data models
│   │   │   │   │   └── repository/   # Repository layer
│   │   │   │   ├── ui/
│   │   │   │   │   ├── screens/      # Compose screens
│   │   │   │   │   ├── theme/        # App theme
│   │   │   │   │   ├── viewmodel/    # ViewModels
│   │   │   │   │   ├── CyberScopeApp.kt
│   │   │   │   │   └── MainActivity.kt
│   │   │   │   └── CyberScopeApp.kt  # Application class
│   │   │   ├── res/                  # Resources
│   │   │   └── AndroidManifest.xml
│   │   └── test/                     # Unit tests
│   └── build.gradle
├── build.gradle
└── settings.gradle
```

## 🚀 Getting Started

### Prerequisites

- **Android Studio**: Hedgehog (2023.1.1) or later
- **JDK**: 17 or higher
- **Android SDK**: API 24 (Android 7.0) or higher
- **Gradle**: 8.2 or higher

### Installation

1. **Open the project in Android Studio**:
   ```bash
   cd CyberScope/mobile
   # Open this directory in Android Studio
   ```

2. **Sync Gradle**:
   - Android Studio will automatically prompt to sync Gradle
   - Or manually: File → Sync Project with Gradle Files

3. **Configure Backend URL**:
   - The app is pre-configured to use `http://10.0.2.2:8080/api/` (Android emulator localhost)
   - For physical devices, update the URL in `ApiClient.kt`:
     ```kotlin
     private var baseUrl = "http://YOUR_COMPUTER_IP:8080/api/"
     ```

4. **Run the app**:
   - Click the "Run" button in Android Studio
   - Or use: `./gradlew installDebug` (Linux/Mac) or `gradlew.bat installDebug` (Windows)

## 📡 API Configuration

### Default Configuration
The app connects to the CyberScope backend at:
- **Emulator**: `http://10.0.2.2:8080/api/`
- **Physical Device**: Update to your computer's IP address

### Changing the Backend URL

Edit `app/src/main/java/com/cyberscope/reports/data/api/ApiClient.kt`:

```kotlin
private var baseUrl = "http://YOUR_IP_ADDRESS:8080/api/"
```

**Finding your IP address**:
- **Windows**: `ipconfig` → Look for IPv4 Address
- **Linux/Mac**: `ifconfig` or `ip addr` → Look for inet address

## 🎨 Screens

### 1. Scans Screen
- Lists all security scans
- Shows scan status, type, priority, and targets
- Click on a scan to view detailed information
- Pull down to refresh

### 2. Reports Screen
- Displays scans with Gemini AI analysis
- Shows risk levels and findings count
- Color-coded risk indicators
- Detailed AI-generated security reports

## 🔐 Permissions

The app requires the following permissions:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

## 🛠️ Building for Production

### Debug Build
```bash
./gradlew assembleDebug
```

### Release Build
```bash
./gradlew assembleRelease
```

The APK will be generated at:
`app/build/outputs/apk/release/app-release.apk`

### Signing the APK

1. Create a keystore:
   ```bash
   keytool -genkey -v -keystore cyberscope.keystore -alias cyberscope -keyalg RSA -keysize 2048 -validity 10000
   ```

2. Add to `app/build.gradle`:
   ```gradle
   android {
       signingConfigs {
           release {
               storeFile file("cyberscope.keystore")
               storePassword "your_password"
               keyAlias "cyberscope"
               keyPassword "your_password"
           }
       }
       buildTypes {
           release {
               signingConfig signingConfigs.release
               ...
           }
       }
   }
   ```

## 📦 Dependencies

### Core
- **Kotlin**: 1.9.20
- **Compose**: 2023.10.01
- **Material 3**: Latest

### Networking
- **Retrofit**: 2.9.0
- **OkHttp**: 4.12.0
- **Gson**: 2.9.0

### Android Jetpack
- **Navigation**: 2.7.6
- **Lifecycle**: 2.7.0
- **DataStore**: 1.0.0

### UI
- **Coil**: 2.5.0 (Image loading)
- **Accompanist**: 0.32.0 (Swipe refresh)

## 🐛 Troubleshooting

### Connection Issues

**Problem**: "Failed to connect to backend"

**Solutions**:
1. Ensure backend is running on `http://localhost:8080`
2. For emulator, use `10.0.2.2` instead of `localhost`
3. For physical device, use your computer's IP address
4. Check firewall settings
5. Verify network connectivity

### Build Errors

**Problem**: "Gradle sync failed"

**Solutions**:
1. File → Invalidate Caches → Invalidate and Restart
2. Delete `.gradle` and `.idea` folders
3. Sync project again

**Problem**: "SDK not found"

**Solutions**:
1. File → Project Structure → SDK Location
2. Set Android SDK location
3. Install required SDK versions

### Runtime Errors

**Problem**: "App crashes on launch"

**Solutions**:
1. Check Logcat for error messages
2. Verify all dependencies are properly installed
3. Clean and rebuild: Build → Clean Project → Rebuild Project

## 🔄 API Endpoints Used

The app consumes the following CyberScope API endpoints:

| Endpoint | Method | Description |
|----------|--------|-------------|
| `/api/scans` | GET | Get all scans |
| `/api/scans/reports` | GET | Get scans with Gemini reports |
| `/api/scans/status/{scanId}` | GET | Get scan details |
| `/api/scans/{scanId}/report/pdf` | GET | Download PDF report |
| `/api/scans/{scanId}/report/html` | GET | Download HTML report |

## 📱 Supported Android Versions

- **Minimum SDK**: 24 (Android 7.0 Nougat)
- **Target SDK**: 34 (Android 14)
- **Compile SDK**: 34

## 🎯 Future Enhancements

- [ ] Settings screen for API configuration
- [ ] Offline mode with local caching
- [ ] Push notifications for scan completion
- [ ] PDF/HTML report viewer
- [ ] Export and share reports
- [ ] Biometric authentication
- [ ] Multi-language support
- [ ] Dark mode toggle in settings
- [ ] Search and filter functionality
- [ ] Scan history and favorites

## 📄 License

This project is part of the CyberScope OSINT Platform.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## 📞 Support

For issues and questions:
- **GitHub Issues**: https://github.com/marshmelloiw/CyberScopeOsint/issues
- **Email**: support@cyberscope.com

## 🙏 Acknowledgments

- Material Design 3 by Google
- Jetpack Compose team
- CyberScope OSINT Platform contributors

---

**Version**: 1.0.0  
**Last Updated**: January 2, 2026  
**Developed with ❤️ for CyberScope**
