# FarmConnect - Project Survival Guide 🚜

**READ THIS IF OPENING AFTER A LONG TIME (e.g., 5 MONTHS)**

This project has two parts:
1.  **Backend** (The Brain/Database) - *Must run first!*
2.  **Android App** (The Screen)

---

## 🚀 How to Run the Project

### Step 1: Start the Backend (CRITICAL)
The Android app will NOT work (Login/Network Errors) if the backend is off.

1.  Open **VS Code**.
2.  Open the `backend` folder.
3.  Open a Terminal (`Ctrl + ~`).
4.  Type this command and hit Enter:
    ```bash
    npm run dev
    ```
5.  Wait until you see: `Server running on port 3000`.
    *   *Keep this window OPEN. Do not close it.*

### Step 2: Run the Android App
1.  Open **Android Studio**.
2.  Open the `android` folder.
3.  Connect your Emulator or Physical Device.
4.  Click the Green **Run (Play)** button.

---

## ❓ Frequently Asked Questions

**Q: The app says "Network Error" or "Connection Refused".**
**A:** You forgot Step 1. The backend server is not running. Run `npm run dev` in the backend folder.

**Q: Where is my data?**
**A:** All user data, listings, and contracts are stored in:
`backend/data/farmconnect.db`

**Q: Did my data expire?**
**A:** No. As long as you have the `.db` file, your data is safe. It creates itself if missing.

**Q: Can I submit this project?**
**A:** Yes. This is a complete local Full-Stack Application (Node.js + SQLite + Android).

---
**Good Luck with your submission! 🎓**
