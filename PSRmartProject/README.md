# TCV Executive Manager
### Deep Navy Minimalist Business Command Center

A premium Android app for managing your fish/vege supply business.
Built with **Kotlin + Jetpack Compose** — modern, fast, and beautiful.

---

## 🚀 HOW TO OPEN IN ANDROID STUDIO

1. **Unzip** this folder anywhere on your computer
2. Open **Android Studio** (Hedgehog 2023.1.1 or newer recommended)
3. Click **"Open"** → select the `TCVExecutiveManager` folder
4. Wait for Gradle to sync (first time may take 3–5 minutes)
5. Connect your Android phone (USB debugging ON) or use an emulator
6. Click the green **▶ Run** button

> Minimum Android version: **Android 8.0 (API 26)**

---

## 📱 APP FEATURES

### LEFT PANE — Visual Catalogue
- Grid view of all stock items with emoji icons
- Add unlimited custom **categories** (Ayam, Ikan, Kobis, etc.)
- Move items between categories freely
- See **buy price vs sell price** with live margin % on each card
- Search bar to find items instantly

### CENTER PANE — Maybank Command Center
- **Expected Balance** auto-calculated from all paid invoices
- **Manual Actual Balance** entry for comparison
- **Discrepancy Alert** — flags even RM 2 differences
- Log expenses: Parking, Toll, Petrol, etc.
- Full ledger with income/expense history

### RIGHT PANE — Precision Invoicing
- **New Invoice tab**: drag-in items from your stock catalogue
- Enter **buy price + sell price** per line → instant net profit calc
- Select customer from saved profiles, or Walk-in
- Auto-generates invoice number (INV-1001, INV-1002...)
- **History tab**: all past invoices, mark as paid, view profit

### SETTINGS
- Edit all company info (name, address, phone, logo)
- Custom invoice fields (SSM No., vehicle numbers, etc.)
- Control which fields appear on invoices
- Tax settings (GST/SST)
- Payment terms customization

---

## 📁 PROJECT STRUCTURE

```
app/src/main/java/com/tcv/executivemanager/
├── MainActivity.kt              ← 3-Pane Swipe System entry point
├── data/
│   ├── model/Models.kt          ← All data classes + Room entities
│   ├── database/TCVDatabase.kt  ← Room DB + DAOs
│   └── repository/TCVRepository.kt ← Data access layer
├── viewmodel/TCVViewModel.kt    ← All business logic + state
└── ui/
    ├── theme/Theme.kt           ← Deep Navy color palette + typography
    ├── components/Components.kt ← Reusable UI components
    └── screens/
        ├── CatalogueScreen.kt   ← LEFT pane
        ├── DashboardScreen.kt   ← CENTER pane
        ├── InvoiceScreen.kt     ← RIGHT pane
        └── SettingsScreen.kt    ← Settings screen
```

---

## 🎨 DESIGN SYSTEM

- **Primary**: Deep Navy `#1A2E5A`
- **Accent**: Electric Blue `#4A9EFF`
- **Success/Profit**: `#2ECC8C`
- **Error/Loss**: `#FF5F6D`
- Corner radius: 12–20dp cards, pill chips
- Elevation: subtle 1dp shadows only

---

## 🔧 CUSTOMIZATION

To change your business name before building:
1. Open `SettingsScreen.kt` (or just run the app and edit in Settings)

To add more stock categories:
- Run the app → Left Pane → tap **"+ Category"**

To change the invoice prefix (INV → TAX, etc.):
- Run the app → Settings → Invoice Numbering

---

## ❓ TROUBLESHOOTING

**"Gradle sync failed"**
→ Make sure you have Android Studio Hedgehog (2023.1.1) or newer
→ Check internet connection (Gradle downloads dependencies)

**"itext7-core" not found**
→ Open `app/build.gradle` and change itext7 version or remove it (PDF export is optional)

**Build errors on first run**
→ File > Invalidate Caches → Restart

---

*Built for TCV Enterprise — Privacy-First, Zero Bank Connection*
