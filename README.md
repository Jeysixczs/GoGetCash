<div align="center">

# GoGetCash

<p>
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Platform">
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/Backend-Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black" alt="Firebase">
  <img src="https://img.shields.io/badge/UI-Material3-6750A4?style=for-the-badge&logo=materialdesign&logoColor=white" alt="Material3">
</p>

<p>A comprehensive Android application for tracking cash flow, managing loans, recording transactions, and generating financial reports.</p>

</div>

---

## Key Functions & Features

### 1. Authentication & Security
- **Secure Login & Signup**: User authentication connected to Firebase Realtime Database.
- **Remember Me**: Persistent login credentials using `SharedPreferences`.
- **Password Visibility Toggle**: Show/hide password option for secure input.

### 2. Dashboard
- **Financial Overview**: Displays total loan balance, on-hand cash, and quick summary metrics.
- **Quick Actions**: Direct access to Cash-In, Cash-Out, Add Loaner, Reports, and Transaction History.
- **Recent Transactions**: Live feed of recent cash movements.

### 3. Loan & Loaner Management
- **Add Loans**: Register new loaners with contact selection, phone numbers, amounts, fees, start dates, and due dates.
- **Paid & Unpaid Lists**: Categorized views to track settled and outstanding loans.
- **Loan Details**: Detailed view per borrower with options to add to loans, reduce loans, or mark as paid.

### 4. Cash Flow (Cash-In & Cash-Out)
- **Cash-In**: Record money received from loans with service fees and contact integration.
- **Cash-Out**: Track payouts and cash disbursements.
- **Search & Filter**: Real-time search by borrower name or amount across history and transactions.

### 5. Reports & Analytics
- **Financial Reports**: Summary of income, cash flow, and statistics backed by MPAndroidChart.

### 6. UI & Theming Consistency
- **Fixed Light Theme**: Configured to maintain consistent light mode presentation across all devices regardless of system-level Dark Mode settings, ensuring layout and color stability.

---

## Tech Stack & Libraries

- **Language**: Kotlin
- **UI Toolkit**: Android XML Views & Material Design 3 (`com.google.android.material`)
- **Backend**: Firebase Realtime Database
- **Charts**: MPAndroidChart (`com.github.PhilJay:MPAndroidChart`)
- **Architecture**: Activity-based MVVM / MVC pattern with intent-driven navigation

---

## App Preview & Screenshots

> *Add your app screenshots to a `screenshots/` directory in your repository and reference them below:*

| Login Screen | Dashboard | Cash-In / Cash-Out | Loan Details |
| :---: | :---: | :---: | :---: |
| ![Login](screenshots/login.png) | ![Dashboard](screenshots/dashboard.png) | ![CashIn](screenshots/cash_in.png) | ![LoanDetails](screenshots/loan_details.png) |

---

## Getting Started

1. **Clone the repository**:
   ```bash
   git clone https://github.com/Jeysixczs/GoGetCash.git
   ```
2. **Open in Android Studio**:
   - Open Android Studio and select **Open an Existing Project**.
   - Navigate to the cloned `GoGetCash` directory.
3. **Firebase Setup**:
   - Add your `google-services.json` file into the `app/` directory.
4. **Build & Run**:
   - Sync Gradle and run the app on an Android emulator or physical device (Min SDK 29, Target/Compile SDK 36).
