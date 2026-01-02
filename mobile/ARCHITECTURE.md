# CyberScope Android App - Architecture Diagram

## 📐 Application Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         PRESENTATION LAYER                       │
│                         (Jetpack Compose)                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────────┐              ┌──────────────────┐         │
│  │  MainActivity    │              │  CyberScopeApp   │         │
│  │                  │──────────────│  (Navigation)    │         │
│  │  - onCreate()    │              │  - NavHost       │         │
│  │  - setContent()  │              │  - BottomNav     │         │
│  └──────────────────┘              └──────────────────┘         │
│                                              │                   │
│                          ┌───────────────────┴────────────────┐ │
│                          │                                     │ │
│                ┌─────────▼─────────┐              ┌───────────▼──────────┐
│                │  ScansScreen      │              │  ReportsScreen       │
│                │                   │              │                      │
│                │  - Scan List      │              │  - Report List       │
│                │  - Pull Refresh   │              │  - AI Analysis       │
│                │  - Detail Dialog  │              │  - Risk Levels       │
│                └─────────┬─────────┘              └───────────┬──────────┘
│                          │                                     │
└──────────────────────────┼─────────────────────────────────────┼──────────┘
                           │                                     │
┌──────────────────────────┼─────────────────────────────────────┼──────────┐
│                          │      VIEWMODEL LAYER                │          │
│                          │      (State Management)             │          │
├──────────────────────────┼─────────────────────────────────────┼──────────┤
│                          │                                     │          │
│                    ┌─────▼─────────────────────────────────────▼─────┐   │
│                    │         ReportViewModel                         │   │
│                    │                                                  │   │
│                    │  - scansState: StateFlow<UiState<List<Scan>>>  │   │
│                    │  - reportsState: StateFlow<UiState<List<Scan>>>│   │
│                    │  - selectedScan: StateFlow<Scan?>              │   │
│                    │                                                  │   │
│                    │  + loadScans()                                  │   │
│                    │  + loadReports()                                │   │
│                    │  + loadScanDetails(scanId)                      │   │
│                    │  + selectScan(scan)                             │   │
│                    └──────────────────┬───────────────────────────────┘   │
│                                       │                                   │
└───────────────────────────────────────┼───────────────────────────────────┘
                                        │
┌───────────────────────────────────────┼───────────────────────────────────┐
│                                       │   REPOSITORY LAYER                │
│                                       │   (Data Management)               │
├───────────────────────────────────────┼───────────────────────────────────┤
│                                       │                                   │
│                          ┌────────────▼────────────┐                     │
│                          │   ReportRepository      │                     │
│                          │                         │                     │
│                          │  + getAllScans()        │                     │
│                          │  + getReports()         │                     │
│                          │  + getScanDetails()     │                     │
│                          │  + downloadPdfReport()  │                     │
│                          │  + downloadHtmlReport() │                     │
│                          └────────────┬────────────┘                     │
│                                       │                                   │
└───────────────────────────────────────┼───────────────────────────────────┘
                                        │
┌───────────────────────────────────────┼───────────────────────────────────┐
│                                       │   NETWORK LAYER                   │
│                                       │   (API Communication)             │
├───────────────────────────────────────┼───────────────────────────────────┤
│                                       │                                   │
│                          ┌────────────▼────────────┐                     │
│                          │      ApiClient          │                     │
│                          │                         │                     │
│                          │  - Retrofit Builder     │                     │
│                          │  - OkHttpClient         │                     │
│                          │  - Logging Interceptor  │                     │
│                          │  - Gson Converter       │                     │
│                          └────────────┬────────────┘                     │
│                                       │                                   │
│                          ┌────────────▼────────────┐                     │
│                          │   CyberScopeApi         │                     │
│                          │   (Interface)           │                     │
│                          │                         │                     │
│                          │  GET /scans             │                     │
│                          │  GET /scans/reports     │                     │
│                          │  GET /scans/status/{id} │                     │
│                          │  GET /scans/{id}/pdf    │                     │
│                          │  GET /scans/{id}/html   │                     │
│                          └────────────┬────────────┘                     │
│                                       │                                   │
└───────────────────────────────────────┼───────────────────────────────────┘
                                        │
                                        │ HTTP/HTTPS
                                        │
┌───────────────────────────────────────▼───────────────────────────────────┐
│                         BACKEND API                                       │
│                    (CyberScope Spring Boot)                               │
│                                                                            │
│                    http://localhost:8080/api/                             │
└────────────────────────────────────────────────────────────────────────────┘
```

## 🔄 Data Flow

### Loading Scans Flow
```
User Opens App
     │
     ▼
MainActivity.onCreate()
     │
     ▼
CyberScopeApp Composable
     │
     ▼
ReportViewModel.init()
     │
     ▼
loadScans() called
     │
     ▼
_scansState = Loading
     │
     ▼
ReportRepository.getAllScans()
     │
     ▼
ApiClient.api.getAllScans()
     │
     ▼
HTTP GET /api/scans
     │
     ▼
Backend Response
     │
     ├─ Success ──▶ _scansState = Success(scans)
     │                    │
     │                    ▼
     │              UI Updates (Recomposition)
     │                    │
     │                    ▼
     │              Display Scan Cards
     │
     └─ Error ────▶ _scansState = Error(message)
                          │
                          ▼
                    UI Shows Error
                          │
                          ▼
                    Retry Button
```

### Viewing Scan Details Flow
```
User Taps Scan Card
     │
     ▼
onClick() triggered
     │
     ▼
viewModel.selectScan(scan)
     │
     ▼
_selectedScan = scan
     │
     ▼
StateFlow emits new value
     │
     ▼
Composable recomposes
     │
     ▼
ScanDetailsDialog shows
     │
     ▼
Display scan details
```

## 🏛️ Layer Responsibilities

### Presentation Layer (UI)
**Responsibility**: Display data and handle user interactions

**Components**:
- `MainActivity`: Entry point, sets up Compose
- `CyberScopeApp`: Navigation and app structure
- `ScansScreen`: Displays scan list
- `ReportsScreen`: Displays reports list
- `ScanDetailsDialog`: Shows scan details
- `ReportDetailsDialog`: Shows report details

**Technologies**:
- Jetpack Compose
- Material 3
- Navigation Compose

### ViewModel Layer
**Responsibility**: Manage UI state and business logic

**Components**:
- `ReportViewModel`: Manages scans and reports state
- `UiState`: Sealed class for state representation

**Technologies**:
- Kotlin StateFlow
- Coroutines
- ViewModel

### Repository Layer
**Responsibility**: Abstract data sources and provide clean API

**Components**:
- `ReportRepository`: Handles all data operations

**Technologies**:
- Kotlin Coroutines
- Result type for error handling

### Network Layer
**Responsibility**: Handle HTTP communication

**Components**:
- `ApiClient`: Retrofit configuration
- `CyberScopeApi`: API endpoint definitions

**Technologies**:
- Retrofit
- OkHttp
- Gson

### Data Layer
**Responsibility**: Define data structures

**Components**:
- `Scan`: Scan data model
- `ScanResult`: Result data model
- `ScanTarget`: Target data model
- Response wrappers

**Technologies**:
- Kotlin data classes
- Gson annotations

## 🎨 UI Component Hierarchy

```
CyberScopeApp
│
├── Scaffold
│   ├── TopAppBar
│   │   └── Text("CyberScope Reports")
│   │
│   ├── BottomNavigationBar
│   │   ├── NavigationBarItem (Scans)
│   │   └── NavigationBarItem (Reports)
│   │
│   └── NavHost
│       ├── ScansScreen
│       │   ├── Header Row
│       │   │   ├── Text("All Scans")
│       │   │   └── IconButton(Refresh)
│       │   │
│       │   └── LazyColumn
│       │       └── ScanCard (for each scan)
│       │           ├── Card
│       │           │   ├── Scan Name
│       │           │   ├── StatusChip
│       │           │   ├── InfoRows
│       │           │   └── Target Info
│       │           │
│       │           └── onClick → ScanDetailsDialog
│       │
│       └── ReportsScreen
│           ├── Header Row
│           │   ├── Column
│           │   │   ├── Text("Gemini Reports")
│           │   │   └── Text("AI-powered...")
│           │   └── IconButton(Refresh)
│           │
│           └── LazyColumn
│               └── ReportCard (for each report)
│                   ├── Card
│                   │   ├── Report Name
│                   │   ├── Scan ID
│                   │   ├── ResultSummaryChips
│                   │   ├── Target Info
│                   │   └── Completion Date
│                   │
│                   └── onClick → ReportDetailsDialog
```

## 🔐 State Management

### UiState Sealed Class
```kotlin
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
```

### State Flow
```
ViewModel                    UI (Composable)
    │                              │
    ├─ StateFlow<UiState>          │
    │      │                       │
    │      ├─ emit(Loading)  ─────▶│ Show Loading
    │      │                       │
    │      ├─ emit(Success)  ─────▶│ Show Data
    │      │                       │
    │      └─ emit(Error)    ─────▶│ Show Error
    │                              │
    └─ collectAsState() ◀──────────┘
```

## 🌐 Network Configuration

### API Client Setup
```
OkHttpClient
    │
    ├── Logging Interceptor (Debug)
    ├── Connect Timeout (30s)
    ├── Read Timeout (30s)
    └── Write Timeout (30s)
         │
         ▼
    Retrofit Builder
         │
         ├── Base URL (configurable)
         ├── OkHttp Client
         └── Gson Converter
              │
              ▼
         CyberScopeApi Interface
              │
              ├── getAllScans()
              ├── getReports()
              ├── getScanStatus()
              ├── downloadPdfReport()
              └── downloadHtmlReport()
```

## 📱 Screen Flow

```
App Launch
    │
    ▼
MainActivity
    │
    ▼
CyberScopeApp
    │
    ├─────────────────┬─────────────────┐
    │                 │                 │
    ▼                 ▼                 ▼
ScansScreen    ReportsScreen    (Future Screens)
    │                 │
    ├─ View List      ├─ View List
    ├─ Pull Refresh   ├─ Pull Refresh
    ├─ Tap Card       ├─ Tap Card
    │                 │
    ▼                 ▼
ScanDetailsDialog  ReportDetailsDialog
    │                 │
    ├─ View Details   ├─ View AI Analysis
    ├─ See Targets    ├─ See Risk Levels
    ├─ See Results    ├─ See Findings
    │                 │
    ▼                 ▼
Close Dialog      Close Dialog
```

## 🔄 Lifecycle

```
App Start
    │
    ▼
Application.onCreate()
    │
    ▼
MainActivity.onCreate()
    │
    ▼
setContent { CyberScopeApp() }
    │
    ▼
Compose Initialization
    │
    ▼
ViewModel Creation
    │
    ▼
ViewModel.init()
    │
    ├─ loadScans()
    └─ loadReports()
         │
         ▼
    API Calls (Background)
         │
         ▼
    StateFlow Updates
         │
         ▼
    UI Recomposition
         │
         ▼
    Display Data
```

---

**Architecture Pattern**: MVVM (Model-View-ViewModel)  
**UI Framework**: Jetpack Compose  
**State Management**: Kotlin StateFlow  
**Dependency Injection**: Manual (Ready for Hilt)  
**Navigation**: Jetpack Navigation Compose  
**Networking**: Retrofit + OkHttp + Gson  
**Async**: Kotlin Coroutines  

**Design Philosophy**: Clean Architecture, Separation of Concerns, Single Responsibility
