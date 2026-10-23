# Attendance & Wage Tracker

A high-fidelity, visually polished Android application built with modern Kotlin, Jetpack Compose, and Room Database to track work shifts, calculated hours, and accumulated earnings in real-time.

## Key Features

- **Real-Time Shift Tracker**: A pulsing active shift indicator with a live-ticking stopwatch and real-time wage counter calculating earnings second-by-second.
- **Sleek Hero Dashboard**: Displaying comprehensive statistics of total work hours, overall earnings, and active session summaries.
- **Segmented Workspace**:
  - **Shifts Log**: A detailed, clean timeline of historical sessions showing time blocks, total hours worked, overtime details, custom text notes, and earnings.
  - **Adjust Wages**: Dynamic settings panel to customize standard hourly rates, overtime thresholds (e.g., 8 hours/day), overtime rate multipliers (e.g., 1.5x), and native currency symbols (e.g., $, €, £, ₹).
- **Full Offline Persistence (CRUD)**:
  - Local SQLite database backed by **Jetpack Room** (powered by robust KSP compiler) to securely store attendance logs and settings.
  - Interactive Dialogs to add manual shifts (with duration offsets), edit historical rates/notes, or delete logs with full reactive UI updates.
- **Premium Slate UI Theme**: A Material 3 designed edge-to-edge layout, utilizing Slate-Midnight colors coupled with Emerald-Green highlights, support for system Dark/Light modes, and elegant interactive ripples.

## Technology Stack

- **Framework**: Jetpack Compose (Kotlin)
- **Design System**: Material Design 3 (M3)
- **Local Database**: Android Room Persistence with Kotlin Symbol Processing (KSP)
- **Architecture**: MVVM (Model-View-ViewModel) with unidirectional data flow (UDF)
- **State Management**: Kotlin Coroutines & `MutableStateFlow` observed reactively with `collectAsStateWithLifecycle`
