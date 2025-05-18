# StarButtonBox Installation Guide (Using Pre-built Releases)

This guide will walk you through installing the StarButtonBox Android application and the PC server software using pre-built release files. This is the recommended method for most users.

## Table of Contents

1.  [Prerequisites](#1-prerequisites)
2.  [Downloading Release Files](#2-downloading-release-files)
3.  [Installing the Android Application (.apk)](#3-installing-the-android-application-apk)
4.  [Installing the PC Server (.exe)](#4-installing-the-pc-server-exe)
5.  [Initial Setup & Connection](#5-initial-setup--connection)
6.  [Troubleshooting](#6-troubleshooting)

## 1. Prerequisites

* **Android Device:** An Android phone or tablet (Android 7.0 Nougat / API 24 or newer).
* **Windows PC:** A Windows PC to run the game (e.g., Star Citizen) and the StarButtonBox PC server.
* **Local Network:** Both your Android device and your PC must be connected to the **same local network** (e.g., the same Wi-Fi router).
* **GitHub Account (Optional):** For downloading releases, though direct links might be provided.

## 2. Downloading Release Files

The latest pre-built versions of the Android app and the PC server installer can usually be found on the project's **Releases** page on GitHub:

➡️ **[StarButtonBox GitHub Releases Page](https://github.com/ongxeno/starbuttonbox-android/releases)** (Replace with your actual releases page URL if different)

Look for the latest release and download the following files:
* **Android App:** `StarButtonBox.apk` (or a similarly named `.apk` file)
* **PC Server Installer:** `StarButtonBoxServer_Installer_vX.Y.Z.exe` (where X.Y.Z is the version number)

Download these files to a location you can easily access. For the `.apk`, you might download it directly to your Android device or download it to your PC and then transfer it to your Android device.

## 3. Installing the Android Application (.apk)

1.  **Enable Installation from Unknown Sources (if needed):**
    * By default, Android restricts installing apps from outside the Google Play Store. You may need to enable this permission for your file manager or browser app.
    * The process varies slightly by Android version and manufacturer:
        * Go to **Settings** on your Android device.
        * Look for "Install unknown apps," "Special app access," or similar in the "Apps," "Security," or "Biometrics and security" sections.
        * Find the app you'll use to open the `.apk` file (e.g., "My Files," "Chrome," "Firefox") and allow it to install unknown apps.
    * *Be sure to disable this permission again after installation if you prefer.*

2.  **Locate and Install the `.apk`:**
    * Open a file manager app on your Android device.
    * Navigate to where you saved `StarButtonBox.apk`.
    * Tap on the `.apk` file to begin the installation.
    * Follow the on-screen prompts. You may be asked to confirm permissions.

3.  **Launch the App:**
    * Once installed, you should find "StarButtonBox" in your app drawer.

## 4. Installing the PC Server (.exe)

1.  **Transfer Installer to PC (if downloaded elsewhere):**
    * Ensure `StarButtonBoxServer_Installer_vX.Y.Z.exe` is on the Windows PC where you play your games.

2.  **Run the Installer:**
    * Double-click `StarButtonBoxServer_Installer_vX.Y.Z.exe` to start the installation wizard.
    * **Windows Defender SmartScreen Warning:** Windows might display a warning like "Windows protected your PC." This is common for new or unsigned applications.
        * If you see this, click on "**More info**."
        * Then, click the "**Run anyway**" button.
    * Follow the on-screen prompts in the installer. You can usually accept the default installation location (e.g., in `C:\Program Files (x86)\StarButtonBox Server`).
    * The installer may offer to create Desktop and Start Menu shortcuts.

3.  **Launch the PC Server Application:**
    * After installation, find "StarButtonBox Server" in your Start Menu or on your Desktop (if you created a shortcut).
    * Run the application. The "StarButtonBox Server Control" window will appear.

4.  **Start the Server and Allow Firewall Access:**
    * In the "StarButtonBox Server Control" window, click the "**Start Server**" button.
    * **Windows Firewall Prompt:** The first time you start the server, Windows Defender Firewall will likely ask for permission for `StarButtonBoxServer.exe` to communicate on the network.
        * It is <span style="color:orange; font-weight:bold;">very important</span> to check the box for **"Private networks"**. You can also check "Public networks" if you understand the security implications for your network setup, but Private is usually sufficient for home use.
        * Click "**Allow access**".
    * The status in the server control panel should update to indicate it's running (e.g., "Server Running on Port 58009"). Note the IP address and Port displayed.

## 5. Initial Setup & Connection (Android App)

1.  **Ensure Same Network:** Double-check that your Android device and your PC are connected to the **same Wi-Fi network**.

2.  **Launch StarButtonBox on Android:**
    * Open the StarButtonBox app on your Android device.

3.  **First-Time Setup (if prompted):**
    * If this is the very first time you're running the app and you haven't configured a connection yet, you may be guided through a setup process.
    * **Server Installation Query:** It will ask if you've already set up the PC server.
        * Since you just installed it in the previous step, choose "**Yes, it's set up and running**".
    * **Network Configuration:** You'll then be taken to a network configuration screen.
        * **Automatic Discovery (Recommended):**
            * The app will try to find your PC server automatically using mDNS.
            * If your server is found, it will appear in a list (e.g., `YourPCName StarButtonBox Server (192.168.1.123:58009)`). Select it.
        * **Manual Entry (If discovery fails):**
            * If the server isn't found automatically, switch to "Manual Entry".
            * Enter the **IP Address** and **Port** that are displayed in the StarButtonBox Server control panel window on your PC. The default port is usually `58009`.
        * Tap "**Save and Finish Setup**".

4.  **Connecting (After Initial Setup or if Configured Previously):**
    * If you've already configured the connection, the app should attempt to connect automatically.
    * You can check the connection status icon and response time in the top bar of the app's main screen.
    * If you need to change the connection settings later, go to **Settings > Connection** within the Android app.

5.  **Ready to Use:**
    * Once connected, you can start using the buttons in the Android app to control your PC game!

## 6. Troubleshooting

* **No Server Found (Automatic Discovery):**
    * Ensure both devices are on the same Wi-Fi network.
    * Verify the StarButtonBox Server is running on your PC and that its status shows "Server Running".
    * Make sure "mDNS Discovery" is enabled in the PC server's settings (it usually is by default).
    * Check your PC's firewall: ensure `StarButtonBoxServer.exe` is allowed, especially for "Private" networks. Sometimes, third-party antivirus or firewall software can also interfere.
    * Try restarting both the PC server application and the Android app.
    * If issues persist, use the "Manual Entry" option for network configuration.
* **Cannot Connect Manually:**
    * Double-check the IP address and Port number entered in the Android app match exactly what's shown in the PC server GUI.
    * Ensure no other application on your PC is using the same port.
    * Verify firewall settings as mentioned above.
* **PC Server Closes Immediately:**
    * Ensure you ran the installer and are running the `StarButtonBoxServer.exe` from its installed location (e.g., Program Files), not directly from a temporary download folder if you skipped the installer.
    * The server writes logs and settings to `%APPDATA%\StarButtonBoxServer`. If it can't write there, it might fail. This is usually handled by the installer setting correct permissions or the app using user-writable locations.
* **Android App "Connection Lost":**
    * This can happen if the Wi-Fi signal is weak, the PC server is stopped, the PC goes to sleep, or there's a network interruption.
    * Check the PC server status and your network connection.

For further assistance, you can check the project's GitHub page:
➡️ [StarButtonBox GitHub Issues](https://github.com/ongxeno/starbuttonbox-android/issues)
