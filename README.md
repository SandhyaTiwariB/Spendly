# Spendly — Smart Money, Simply.
## Enterprise-Grade Personal & Family Finance Management Platform

Spendly is an enterprise-grade full-stack personal and family finance management system built with **Java 21**, **Spring Boot 3.3.4**, **Spring Security**, **Spring Data JPA**, **MySQL 8.x** (with zero-setup **H2 In-Memory** mode), and a high-fidelity frontend (**HTML5, CSS3, Vanilla ES6+ JavaScript, Chart.js**).

Live UI Reference: [https://vrindaatluri611.github.io/Spendly/](https://vrindaatluri611.github.io/Spendly/)

---

## 1. System Overview & Architecture

Spendly delivers complete personal, social, and parental financial control through a unified architecture:

```
Bank Statement Upload (CSV / Excel / PDF)  ──┐
Manual Transactions (Debit / Credit)      ──┼──→ Transaction Engine
Split Bills & Peer Lending/Borrowing       ──┘          ↓
                                                Parent Control Filter
                                                (Strict Category Enforcement & Auditing)
                                                        ↓
                                                Deduplication & Transfer Isolation
                                                (SHA-256 Fingerprinting, 14+ Categories)
                                                        ↓
                                                Relational Persistence & Auditing
                                                (MySQL 8.x / Spring Data JPA)
                                                        ↓
                                                Financial Calculations & Analytics
                                                (Transfers Excluded from Spending/Income)
                                                        ↓
                                                Interactive Spendly UI
                                                (8 Nav Sections, Charts, Modals, Logs)
```

### Visual & Design Identity (Preserved Ground Truth)
- **Primary Green**: Dark forest-green `#0d3b2e` with hover accents `#145a43` and active indicator `#1a5844`.
- **Accent & Brand**: Warm gold `#c8a24a` and Rupee currency symbol `₹`.
- **Canvas & Background**: Cream off-white `#f8f7f1` with crisp white cards (`#ffffff`, `border-radius: 16px`, border `#e5e9e4`).
- **Typography**: Serif headlines (*Playfair Display*) and modern sans-serif body (*DM Sans*).
- **Navigation (Exact Order)**:
  1. `Dashboard`
  2. `Expenses`
  3. `Budgets`
  4. `Split Bills`
  5. `Lending & Borrowing`
  6. `Bank Statements`
  7. `Parent Control`
  8. `Activity Log` (Parent Control Audit Trail)
  9. `Settings`

---

## 2. Core Feature Highlights

### A. Authentication & User Profiles
- **Full Auth Stack**: Spring Security with `BCryptPasswordEncoder` and secure token management.
- **Preloaded Demo Accounts**:
  - **Parent Profile**: `vrinda@spendly.io` / `Spendly@2026` (Full administration, parent controls, monitoring)
  - **Child Profile**: `aarav@spendly.io` / `Aarav@2026` (Subject to parental limits and category restrictions)
- **Instant Demo Fill**: One-click demo login buttons directly on the authentication overlay.

### B. Intelligent Bank Statement Parsing & Deduplication
- **Multi-Format Support**:
  - **CSV**: Apache Commons CSV 1.11.0 with robust header detection and currency cleaning (`₹`, commas).
  - **Excel**: Apache POI 5.3.0 for modern `.xlsx` and legacy `.xls` workbooks.
  - **PDF**: Apache PDFBox 3.0.3 with structural tabular regex parsing and honest scanned PDF detection.
- **SHA-256 Idempotency**: Re-uploading the same statement never creates duplicate transactions.
- **Transfer Isolation**: Internal own-account transfers are automatically flagged (`isTransfer = true`) and strictly excluded from expenditure and income calculations.

### C. Split Bills & Lending / Borrowing (Social Finance)
- **Split Bills**: Group expense tracking (Goa Trip, Flatmates, Besties) with automatic equal/custom division and per-participant settlement tracking (`/api/split-bills/{billId}/settle/{participantId}`).
- **Lending & Borrowing**: Complete ledger of loans given and debts owed with partial repayment tracking (`/api/obligations/{id}/repay`) and full balance settlements.

### D. Parent Control & Category Enforcement (Requirements 13 & 14)
- **Enforceable Category Controls**:
  - `Shopping` (Toggle permission)
  - `Entertainment` (Toggle permission)
  - `Food & Dining` (Toggle permission)
  - `Gaming / Apps` (Default locked: in-app purchases blocked)
- **Strict Enforcement Boundary**:
  - **Spendly Direct Transactions**: Any attempt to record spending in a blocked category is strictly **rejected** with a `400 Bad Request` and logged as a `TRANSACTION_BLOCKED` event.
  - **External Statement Debits**: Transactions that already cleared the banking rail outside Spendly cannot be undone retroactively; they are safely imported, marked with `TRANSACTION_FLAGGED` in the audit log, and generate a parental notification alert.
- **Dedicated Activity Log (Page 8)**:
  - Immutable audit trail of permission changes, monthly limit updates, blocked transactions, and warnings.
  - Multi-criteria filtering by Linked Account, Event Type, and Status.
  - Direct CSV Export via `/api/parent-control/activity-log/export`.

---

## 3. Technology Stack

- **Backend**: Java 21, Spring Boot 3.3.4, Spring Security, Spring Data JPA, Spring Web.
- **Database**: MySQL 8.x (Production) / In-Memory H2 (Test & Zero-Setup Demo).
- **Libraries**: Apache Commons CSV, Apache POI, Apache PDFBox, Project Lombok.
- **Frontend**: HTML5, Vanilla CSS3, Vanilla ES6+ JavaScript, Chart.js 4.4.4.
- **Testing**: JUnit 5, Mockito, Spring Boot MockMvc (19/19 passing tests).

---

## 4. Quick Start & Execution Instructions

### Option 1: Zero-Setup Mode (In-Memory H2 — Recommended for Instant Review)
No MySQL installation required. Runs out-of-the-box in 5 seconds:

```powershell
# Using the pre-built executable JAR:
java -jar target/spendly-1.0.0.jar --spring.profiles.active=h2

# Or running directly via Maven:
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=h2"
```

Once running, navigate to:
```
http://localhost:8080
```
- Click **"Vrinda (Parent)"** on the login view and sign in immediately.
- H2 Database Console is accessible at: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:spendly_db`, User: `sa`, Password: *(empty)*).

---

### Option 2: Production MySQL 8.x Mode
1. Create the database in MySQL:
   ```sql
   CREATE DATABASE spendly_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
2. Configure credentials (if different from default `root`/`root`):
   ```powershell
   $env:SPRING_DATASOURCE_USERNAME="root"
   $env:SPRING_DATASOURCE_PASSWORD="your_password"
   ```
3. Run the application:
   ```powershell
   java -jar target/spendly-1.0.0.jar
   ```

---

## 5. Automated Acceptance & Regression Tests

Run the full automated test suite (19 test cases):
```powershell
.\mvnw.cmd test
```

### Test Coverage Highlights:
- `ParentControlAndSocialFinanceTest` (4 Tests):
  - Strict rejection of blocked category transactions (`Gaming / Apps`).
  - Permitted category transaction processing.
  - Activity log event recording.
  - Split bill participant settlement and net balance verification.
- `DuplicateDetectionAndIdempotencyTest`: SHA-256 fingerprint deduplication across repeated imports.
- `FinancialCalculationServiceTest`: BigDecimal monetary precision and internal transfer exclusion.
- `CsvStatementParserTest`, `ExcelStatementParserTest`, `PdfStatementParserTest`: Multi-format parsing and error tolerance.
- `SpendlyApplicationTests` (4 Tests): Spring Security, context bootstrap, and REST API contract checks.

---

## 6. Complete REST API Reference

All endpoints return JSON and adhere to standard HTTP status codes.

### Authentication
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/auth/login` | Sign in (`{ email, password }`) → returns session token & user info |
| `POST` | `/api/auth/register` | Register new account (`{ fullName, email, password }`) |
| `GET` | `/api/auth/me` | Verify active session and retrieve profile |
| `POST` | `/api/auth/logout` | Terminate session |
| `POST` | `/api/auth/forgot-password` | Request password reset instructions |

### Dashboard & Analytics
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/dashboard?period=month` | Retrieve total spent, remaining, owed to you, you owe, bars, categories |
| `GET` | `/api/reports?period=month` | Retrieve monthly and weekly spending analytics |

### Transactions & Statements
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/transactions` | Query transactions (`search`, `category`, `direction`, `page`) |
| `POST` | `/api/transactions` | Create transaction (enforces parental restrictions) |
| `DELETE`| `/api/transactions/{id}` | Delete transaction |
| `GET` | `/api/transactions/export/csv` | Download transactions in CSV format |
| `POST` | `/api/statements/import` | Upload bank statement (`CSV`, `XLSX`, `PDF`) |
| `GET` | `/api/statements/history` | List previous statement imports and duplicate counts |

### Social Finance (Split Bills & Lending)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/split-bills` | List active group bills and participants |
| `POST` | `/api/split-bills` | Create new shared bill |
| `POST` | `/api/split-bills/{billId}/settle/{pId}` | Settle an individual participant's share |
| `GET` | `/api/split-bills/net-balance` | Calculate net social balance across all groups |
| `GET` | `/api/obligations` | List all lending and borrowing obligations |
| `POST` | `/api/obligations` | Record money lent or borrowed |
| `POST` | `/api/obligations/{id}/repay` | Record a partial repayment |
| `POST` | `/api/obligations/{id}/settle` | Mark full obligation as settled |
| `GET` | `/api/obligations/summary` | Summary of total lent, borrowed, and outstanding |

### Parent Control & Audit Log
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/parent-control/summary` | Child spending limit, usage progress, and rules summary |
| `GET` | `/api/parent-control/rules` | List category permission rules |
| `PUT` | `/api/parent-control/rules/{category}` | Update category rule (`isAllowed: true/false`) |
| `GET` | `/api/parent-control/activity-log` | Filterable immutable audit trail of parent events |
| `GET` | `/api/parent-control/activity-log/export`| Export audit trail as CSV |
| `GET` | `/api/parent-control/alerts` | List parent security alerts |
| `POST` | `/api/parent-control/alerts/mark-read` | Mark all alerts as read |

### Settings & User Preferences
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/settings` | Retrieve user preferences |
| `PUT` | `/api/settings` | Update full name, currency, monthly budget, or password |

---

## 7. Synthetic Statements Directory

Sample bank statements are available in `sample_statements/` for verification:
- `sample_statements/sample_statement.csv`: 9 transactions including salary, dining, transport, shopping, and transfer.
- `sample_statements/sample_statement.xlsx`: Formatted Excel workbook.
- `sample_statements/sample_statement.pdf`: Text-based PDF statement.

---

## 8. Summary of Project Verification Status

- [x] Exact Visual Identity from live reference ([https://vrindaatluri611.github.io/Spendly/](https://vrindaatluri611.github.io/Spendly/)) preserved.
- [x] All 8 Sidebar Navigation Sections + Settings functional.
- [x] Real Database Persistence (zero dummy/static placeholders).
- [x] Full Spring Security authentication with demo profiles.
- [x] Statement parsing with SHA-256 deduplication and transfer isolation.
- [x] Parent Control enforcement (manual transaction rejection + statement flagging).
- [x] Dedicated Parent Control Activity Log with filtering & CSV export.
- [x] 19 of 19 JUnit tests passing cleanly.
- [x] Executable JAR built and packaged at `target/spendly-1.0.0.jar`.
