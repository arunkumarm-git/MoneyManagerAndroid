# Privacy Policy for Money Manager: Expense Tracker

**Last updated:** August 26, 2026

**Money Manager: Expense Tracker** ("we", "our", or "the App") is developed by Arun Kumar. We are committed to protecting your personal privacy. This Privacy Policy explains our practices regarding the collection, storage, and processing of your information when you use our Android mobile application.

---

### 1. Offline-First & Zero Data Collection

* **100% Local Storage**: Money Manager is built as an offline-first personal finance application. All your data — including accounts, budgets, expenses, income, recurring subscriptions, and savings goals — is stored exclusively on your device in a private, encrypted local SQLite Room database.
* **No Remote Servers or Tracking**: We do **not** operate external tracking servers, analytics servers, or advertising networks. We do **not** collect, transmit, sell, rent, or share your personal or financial data with any third party.

---

### 2. Permissions & Data Usage

The App requests the following Android permissions strictly to provide local app functionality:

#### A. SMS Permissions (`android.permission.RECEIVE_SMS`, `android.permission.READ_SMS`)
* **Purpose**: Used solely to automatically detect and parse transactional SMS messages (such as bank debit/credit alerts) on your device and suggest new transactions.
* **Local Processing**: All SMS processing and parsing occur **100% locally on your device**. No SMS messages, phone numbers, or text contents are ever uploaded, transmitted over the internet, or shared with anyone.
* **Optional**: You can choose whether or not to grant SMS permissions. The app remains fully functional with manual transaction entries if SMS permissions are denied.

#### B. Notification Permission (`android.permission.POST_NOTIFICATIONS`)
* **Purpose**: Used to deliver timely local reminders for credit card payment due dates, upcoming recurring bills, and budget limit alerts.
* **Local Processing**: Notifications are scheduled locally via Android WorkManager without transmitting data to external push notification services.

#### C. Biometric Authentication (`android.permission.USE_BIOMETRIC`)
* **Purpose**: Used to allow you to lock the app and authenticate securely using your device's fingerprint sensor, face unlock, or device PIN/pattern.
* **Security**: Biometric authentication is handled entirely by the Android operating system's secure hardware (KeyStore / BiometricPrompt). The App never accesses or stores your raw biometric data.

---

### 3. Data Backup and Portability

* **User Controlled**: You have complete ownership and control over your financial records.
* **Export / Backup**: When you use the CSV Export or JSON Backup features, the exported files are saved directly to your local device storage or the location you explicitly choose via the Android system file picker.

---

### 4. Children’s Privacy

Money Manager does not knowingly collect or solicit any personal information from children under the age of 13.

---

### 5. Changes to This Privacy Policy

We may update our Privacy Policy from time to time. Any changes will be posted on this page with an updated revision date.

---

### 6. Contact Us

If you have any questions, suggestions, or concerns regarding this Privacy Policy, please contact us at:

* **Developer**: Arun Kumar
* **GitHub**: [https://github.com/arunkumarm-git/MoneyManagerAndroid](https://github.com/arunkumarm-git/MoneyManagerAndroid)
* **Email**: arunkumarm.dev@gmail.com *(or your developer support email)*
