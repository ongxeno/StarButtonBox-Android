# StarButtonBox - Building from Source Guide

This guide is for developers or advanced users who wish to build the StarButtonBox Android application and/or the PC server software from the source code.

If you simply want to use the application, please refer to the **[Installation Guide (Using Pre-built Releases)](INSTALLATION.md)**.

## Table of Contents

1.  [Prerequisites](#1-prerequisites)
    * [For Android App](#for-android-app)
    * [For PC Server (Python)](#for-pc-server-python)
2.  [Cloning the Repository](#2-cloning-the-repository)
3.  [Building the Android Application (.apk)](#3-building-the-android-application-apk)
4.  [Building and Packaging the PC Server](#4-building-and-packaging-the-pc-server)
    * [4.1. Building the Executable (.exe) with PyInstaller](#41-building-the-executable-exe-with-pyinstaller)
    * [4.2. Creating the Installer Package with Inno Setup](#42-creating-the-installer-package-with-inno-setup)
5.  [Installing Your Built Artifacts](#5-installing-your-built-artifacts)
6.  [Development Notes](#6-development-notes)

## 1. Prerequisites

### For Android App

* **Android Studio:** The latest stable version is recommended (e.g., Hedgehog, Iguana, or newer). Download from the [Android Developer website](https://developer.android.com/studio).
* **Java Development Kit (JDK):** Android Studio usually comes with an embedded JDK, but having a system JDK (e.g., JDK 17 or newer) can be beneficial.
* **Android SDK:** Install the required SDK platforms and build tools through the Android Studio SDK Manager. This project targets `targetSdk = 35` and `minSdk = 24`.
* **Android Device or Emulator:** For testing the built `.apk`.

### For PC Server (Python)

* **Python:** Python 3.8 or newer is recommended. Download from [python.org](https://www.python.org/). Ensure Python and Pip are added to your system's PATH during installation.
* **Pip:** Python's package installer (usually comes with Python).
* **Required Python Libraries:**
    ```bash
    pip install pydirectinput pyautogui zeroconf pystray Pillow
    ```
* **PyInstaller (for building .exe):**
    ```bash
    pip install pyinstaller
    ```
* **Inno Setup (for creating Windows installer):**
  Download and install from [jrsoftware.org](https://jrsoftware.org/isinfo.php).

## 2. Cloning the Repository

First, clone the StarButtonBox repository to your local machine:

```bash
git clone [https://github.com/ongxeno/starbuttonbox-android.git](https://github.com/ongxeno/starbuttonbox-android.git)
cd starbuttonbox-android
```

## 3. Building the Android Application (.apk)

1.  **Open in Android Studio:**
    * Launch Android Studio.
    * Select "Open" or "Open an Existing Project."
    * Navigate to the cloned `starbuttonbox-android` directory and open it.
    * Allow Android Studio to sync Gradle files and download any necessary dependencies. This might take a few minutes.

2.  **Build the APK:**
    * Once the project is synced, go to **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
    * Android Studio will build the debug APK.
    * When the build is complete, a notification will appear. Click "locate" to find the generated `.apk` file. It's usually in `app/build/outputs/apk/debug/app-debug.apk`.

3.  **(Optional) Build a Release APK:**
    * For a release (signed) APK, you'll need to configure signing keys. Follow the official Android documentation for [signing your app](https://developer.android.com/studio/publish/app-signing).
    * Then, go to **Build > Generate Signed Bundle / APK...** and select "APK."

## 4. Building and Packaging the PC Server

All commands for building the PC server should be run from within the `/server` directory of the cloned repository.

### 4.1. Building the Executable (.exe) with PyInstaller

PyInstaller bundles your Python application and its dependencies.

1.  **Navigate to the Server Directory:**
    Open your command prompt or PowerShell and change to the server directory:
    ```bash
    cd server
    ```

2.  **Prepare Assets:**
    * Ensure `icon.ico` (for the application/installer icon) and `tray_icon.png` (for the system tray icon) are present in the `/server` directory.

3.  **Review the `.spec` File (`StarButtonBoxServer.spec`):**
    * This file controls how PyInstaller builds the executable. It's pre-configured in the `/server` directory.
    * Key sections to verify:
        * `Analysis -> datas`: Should include `('tray_icon.png', '.')` and `('icon.ico', '.')`.
        * `Analysis -> hiddenimports`: Lists modules PyInstaller might miss.
        * `EXE -> icon`: Should be set to `'icon.ico'`.
        * `EXE -> console`: Should be `False` for a GUI application.

4.  **Run PyInstaller:**
    ```bash
    pyinstaller --noconfirm StarButtonBoxServer.spec
    ```
    * This command uses the `.spec` file.
    * PyInstaller will create `build` and `dist` subdirectories within `/server`.
    * The bundled application will be in `/server/dist/StarButtonBoxServer/`. This folder contains `StarButtonBoxServer.exe` and all its necessary supporting files.

### 4.2. Creating the Installer Package with Inno Setup

Inno Setup creates a user-friendly Windows installer from the files generated by PyInstaller.

1.  **Prepare PyInstaller Output:**
    * Ensure you have successfully run PyInstaller, and the `/server/dist/StarButtonBoxServer/` folder exists and contains your application.

2.  **The Inno Setup Script (`StarButtonBoxServer_Installer.iss`):**
    * This script is located in the `/server` directory. Review its contents, especially:
        * **`[Setup] -> AppId`**: **IMPORTANT!** If you are distributing your own build, you should generate a new unique GUID (e.g., from [guidgenerator.com](https://www.guidgenerator.com/)) and replace the placeholder.
        * **`[Files] -> Source: "dist\StarButtonBoxServer\*"`**: This path is relative to the `.iss` file. It tells Inno Setup to package everything from the PyInstaller output directory.
        * **`[Files] -> Source: "{src}\icon.ico"`**: Ensures your `icon.ico` (which should be in the same `/server` directory as the `.iss` file) is included for shortcuts.
        * **`[Setup] -> SetupIconFile={src}\icon.ico`**: Sets the icon for the installer itself.

    ```ini
    ; StarButtonBoxServer_Installer.iss (ensure this is in your /server directory)
    ; Inno Setup script for StarButtonBox PC Server
    
    [Setup]
    AppId={{YOUR_UNIQUE_GUID_HERE}} ; <<< REPLACE WITH YOUR OWN UNIQUE GUID
    AppName=StarButtonBox Server
    AppVersion=1.0 
    AppPublisher=OngXeno
    AppPublisherURL=[https://github.com/ongxeno/starbuttonbox-android](https://github.com/ongxeno/starbuttonbox-android)
    DefaultDirName={autopf}\StarButtonBox Server
    DefaultGroupName=StarButtonBox Server
    AllowNoIcons=yes
    OutputBaseFilename=StarButtonBoxServer_Installer_v1.0
    SetupIconFile={src}\icon.ico 
    Compression=lzma
    SolidCompression=yes
    WizardStyle=modern
    UninstallDisplayIcon={app}\StarButtonBoxServer.exe
    
    [Languages]
    Name: "english"; MessagesFile: "compiler:Default.isl"
    
    [Tasks]
    Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked
    Name: "quicklaunchicon"; Description: "{cm:CreateQuickLaunchIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked; OnlyBelowVersion: 0,6.1 
    
    [Files]
    Source: "dist\StarButtonBoxServer\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
    Source: "{src}\icon.ico"; DestDir: "{app}"; Flags: ignoreversion 
    
    [Icons]
    Name: "{group}\StarButtonBox Server"; Filename: "{app}\StarButtonBoxServer.exe"; IconFilename: "{app}\icon.ico"
    Name: "{group}\{cm:ProgramOnTheWeb,StarButtonBox Server}"; Filename: "[https://github.com/ongxeno/starbuttonbox-android](https://github.com/ongxeno/starbuttonbox-android)"
    Name: "{group}\{cm:UninstallProgram,StarButtonBox Server}"; Filename: "{uninstallexe}"
    Name: "{autodesktop}\StarButtonBox Server"; Filename: "{app}\StarButtonBoxServer.exe"; Tasks: desktopicon; IconFilename: "{app}\icon.ico"
    Name: "{userappdata}\Microsoft\Internet Explorer\Quick Launch\StarButtonBox Server"; Filename: "{app}\StarButtonBoxServer.exe"; Tasks: quicklaunchicon; IconFilename: "{app}\icon.ico"; OnlyBelowVersion: 0,6.1
    
    [Run]
    Filename: "{app}\StarButtonBoxServer.exe"; Description: "{cm:LaunchProgram,StarButtonBox Server}"; Flags: nowait postinstall skipifsilent unchecked
    
    [UninstallDelete]
    Type: filesandordirs; Name: "{app}"
    Type: filesandordirs; Name: "{userappdata}\StarButtonBoxServer"
    ```

3.  **Compile the Installer:**
    * Open the Inno Setup Compiler.
    * Go to **File > Open...** and select your `/server/StarButtonBoxServer_Installer.iss` file.
    * Go to **Build > Compile** (or press F9).
    * The installer (e.g., `StarButtonBoxServer_Installer_v1.0.exe`) will be created in an `Output` subfolder within your `/server` directory.

## 5. Installing Your Built Artifacts

Once you have built the Android `.apk` and the PC Server Installer `.exe`:

1.  **Install the Android App:**
    * Transfer the generated `.apk` (e.g., `app-debug.apk`) to your Android device.
    * Follow the steps outlined in the **[Installation Guide - Section 3: Installing the Android Application (.apk)](INSTALLATION.md#3-installing-the-android-application-apk)**.

2.  **Install the PC Server:**
    * Copy the generated PC Server Installer (e.g., `StarButtonBoxServer_Installer_v1.0.exe`) to your Windows PC.
    * Follow the steps outlined in the **[Installation Guide - Section 4: Installing the PC Server (.exe)](INSTALLATION.md#4-installing-the-pc-server-exe)**.

3.  **Initial Setup & Connection:**
    * Follow the steps in the **[Installation Guide - Section 5: Initial Setup & Connection](INSTALLATION.md#5-initial-setup--connection)** to configure the Android app to connect to your PC server.

## 6. Development Notes

* **Server Directory:** The PC server code resides in the `/server` subdirectory.
* **Android Assets for Server Setup:**
    * The Android app's initial setup flow can serve the PC server installer. For this to work with your self-built installer, place your compiled `StarButtonBoxServer_Installer_vX.Y.Z.exe` into the Android project at `app/src/main/assets/server_files/`.
    * The HTML page served by the Android app for PC setup instructions is `app/src/main/assets/web_pages/pc_server_setup.html`. Ensure the download link in this HTML file points to the correct installer filename.
* **Logging:**
    * Android App: Check Logcat in Android Studio.
    * PC Server: Logs to `server.log` located in `%APPDATA%\StarButtonBoxServer\` when installed, or in the `/server` directory if run directly from Python scripts.

This guide should provide all the necessary steps for building the project from source.
