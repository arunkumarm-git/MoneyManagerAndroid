# 💰 Money Manager (Android)

A modern, offline-first personal finance and wealth management Android application built with **Jetpack Compose**, **Material 3**, and **Room Database**.

---

## ✨ Features

### 📊 Financial Tracking & Overview
* **Overview Dashboard**: Instant view of your total net worth, monthly income, monthly expenses, and budget progress.
* **Multi-Account Support**: Manage Cash, Bank Accounts, Wallets, and Credit Cards with balance tracking.
* **Categories & Budgets**: Customizable income and expense categories with monthly spending limits and budget watch alerts.
* **Swipe-to-Delete with Undo**: Delete transactions with a left swipe and restore them instantly with an interactive Undo snackbar.

### 🎯 Savings Goals & Milestones
* **Goal Buckets**: Set targets for vacations, emergency funds, or gadgets.
* **Progress Tracking**: Real-time percentage tracking, target completion dates, and quick deposit dialogs.

### 🔄 Subscriptions & Recurring Transactions
* **Automated Payments**: Schedule recurring rent, salary, subscriptions (Netflix, Spotify), or utility bills across Daily, Weekly, Monthly, or Yearly frequencies.
* **Auto-Apply Engine**: Automatic background logging of recurring transactions when due.

### 📈 Reports & Interactive Analytics
* **Trend Analysis**: 6-month visual comparison of income vs expenses.
* **Expense Breakdown**: Interactive Donut chart by category.
* **Interactive Drilldowns**: Tap any category in the report legend to open a bottom sheet displaying all transactions for that category.

### 🔒 Security & Privacy
* **Biometric App Lock**: Secure financial records using Fingerprint, Face Unlock, or Device PIN/Pattern.
* **Privacy Mask Mode**: One-tap eye icon on the dashboard to mask sensitive balance amounts in public.

### 💾 Data Portability & Backup
* **CSV Export & Import**: Export transaction histories to CSV (for Excel / Google Sheets) or import transactions from external CSV files.
* **Full JSON Backup & Restore**: Create full database snapshots (Accounts, Categories, Transactions) and restore them anytime.

### 🎨 Personalization & UX
* **Curated Themes**: System default, Light, and Dark modes.
* **Multi-Currency Support**: Choose your preferred currency symbol (`₹`, `$`, `€`, `£`, `¥`).
* **Tactile & Sound Feedback**: Optional subtle button tap sound effects.

---

## 🛠️ Tech Stack & Architecture

* **Language**: Kotlin
* **UI Toolkit**: Jetpack Compose (Material 3)
* **Architecture**: MVVM + Repository Pattern
* **Local Database**: Room SQLite DB (Version 2 with database migrations)
* **Asynchronous / Reactive**: Kotlin Coroutines & `StateFlow`
* **Security**: `androidx.biometric:biometric`
* **Background Tasks**: AndroidX WorkManager (for daily reminders & recurring schedules)
* **Typography**: Outfit Variable Font

---

## 🚀 Getting Started

### Prerequisites
* Android Studio Iguana / Ladybug or newer
* Android SDK 34 (Minimum Android 8.0 / API 26)
* JDK 17

### Build & Run

1. **Clone the repository:**
   ```bash
   git clone https://github.com/arunkumarm-git/MoneyManagerAndroid.git
   cd MoneyManagerAndroid
   ```

2. **Build Debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```
   The APK will be generated at:
   `app/build/outputs/apk/debug/app-debug.apk`

3. **Install on a connected device:**
   ```bash
   ./gradlew installDebug
   ```

---

## 📱 Wireless Debugging Setup

To test wirelessly on your phone (Android 11+):

1. Connect your phone and computer to the **same Wi-Fi network**.
2. On your phone: **Settings → Developer Options → Enable Wireless Debugging**.
3. Tap **Pair device with pairing code** and run:
   ```bash
   adb pair <PHONE_IP>:<PAIRING_PORT>
   ```
4. Connect using the IP and Port on the main Wireless Debugging screen:
   ```bash
   adb connect <PHONE_IP>:<PORT>
   ```
5. Deploy:
   ```bash
   ./gradlew installDebug
   ```

---

## 📄 License

This project is licensed under the MIT License.
