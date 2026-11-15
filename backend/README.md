# SiteSnag & NCR Tracker — Backend Setup Guide (Google Apps Script)

This companion backend provides automated, single-threaded vendor email dispatch and scheduled quality summaries for **SiteSnag & NCR Tracker**.

---

## 🌟 Key Architecture & Capabilities

1. **Single-Thread per Vendor (`Vendor_Threads` Tab):**
   - Automatically maintains a single Gmail thread per vendor:
     `[<Project Name>] Site Observations & NCR Tracker - <Vendor Name>`
   - All subsequent NCR alerts, updates, and periodic summaries are posted as threaded replies (`replyAll`) into this exact same conversation.
   - Automatically CCs the project's default corporate Outlook/QA email on every message.

2. **Immediate NCR Dispatch (10-Minute Polling Trigger):**
   - Continuously monitors the `NCR_Register` sheet for rows where `NCR_Email_Sent != "YES"`.
   - Locates the generated PDF (`NCR_<Number>_<Block>_<Vendor>.pdf`) directly from Google Drive.
   - Dispatches an urgent email alert with the PDF attached to the vendor's existing thread.
   - Updates `NCR_Email_Sent` to `"YES"` to guarantee idempotency.

3. **Scheduled Batch Summaries (`Settings` Tab):**
   - Configurable frequency: **`Daily`**, **`Alternate Days`**, or **`Weekly`**.
   - Global toggle: **`Auto-Email Active`** (`YES` / `NO`).
   - Compiles all active **Open Observations** and **Open NCRs** into a clean, responsive HTML summary table with severity badges and target resolution dates.
   - Automatically skips vendors who have zero open items.

---

## 🚀 Step-by-Step Deployment

### Step 1: Create the Master Google Sheet
1. Open [Google Sheets](https://sheets.new) and create a new spreadsheet named **"SiteSnag & NCR Master Register"**.
2. Note the Spreadsheet ID from the URL:
   `https://docs.google.com/spreadsheets/d/<SPREADSHEET_ID>/edit`

### Step 2: Paste the Apps Script Code
1. In Google Sheets, click **Extensions > Apps Script**.
2. Rename the project to **"SiteSnag Backend"**.
3. Replace all content in `Code.gs` with the contents of `backend/Code.gs`.
4. Click the **Save** (💾) icon.

### Step 3: Run `setupTriggers()`
1. In the function dropdown at the top of the Apps Script editor, select **`setupTriggers`**.
2. Click **Run**.
3. A popup will ask for permissions (Gmail and Drive access):
   - Click **Review permissions**.
   - Select your Google account.
   - Click **Advanced > Go to SiteSnag Backend (unsafe)**.
   - Click **Allow**.
4. The script will automatically:
   - Create and style the `Vendor_Threads` and `Settings` tabs in your spreadsheet.
   - Install a 10-minute trigger for `processPendingNCRDispatches`.
   - Install a daily 8:00 AM trigger for `sendScheduledBatchSummaries`.

### Step 4: Configure Settings
Switch to the **Settings** tab in your Google Sheet:
| Setting Key | Value | Description |
|---|---|---|
| **Project Name** | Metro Tower Phase 2 | Used in email subject lines and headers |
| **Default CC Email** | qa-team@contractor.com | Corporate Outlook / QA email to CC on all threads |
| **Auto-Email Active** | YES | Set to `YES` to enable scheduled summaries, `NO` to pause |
| **Summary Frequency** | Daily | Choose `Daily`, `Alternate Days`, or `Weekly` |

---

## 📂 Sheet Schema Reference

### `Observations` Tab
- `Entry ID` (e.g. `OBS-001`)
- `Date Logged` (e.g. `2026-11-15`)
- `Block/Zone` (e.g. `Tower A`)
- `Floor/Level` (e.g. `Level 3`)
- `Specific Location` (e.g. `Flat 302, Master Bath`)
- `Vendor` (e.g. `Apex Waterproofing`)
- `Vendor Email`
- `CC Emails`
- `Trade/Category` (e.g. `Waterproofing`)
- `Priority` (`High` / `Medium` / `Low`)
- `Target Date`
- `Status` (`Open` / `Rectified`)
- `Rectified Date`
- `Issue Description`
- `Before Photo URL`
- `After Photo URL`
- Dynamic Custom Fields (Appended dynamically)

### `NCR_Register` Tab
- Same as above, plus:
- `Root Cause`
- `Corrective Action Required`
- `NCR_PDF_URL`
- `NCR_PDF_FileName`
- `NCR_Email_Sent` (`YES` / `NO`)
