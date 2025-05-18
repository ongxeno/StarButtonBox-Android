# StarButtonBox

<p align="center">
  <img src="./app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.webp" alt="App Icon" width="128">
</p>

StarButtonBox is a two-part application designed to provide a customizable virtual button box experience on an Android device for controlling PC games, with a primary focus on Star Citizen. It aims to offer a more immersive and efficient way to manage complex in-game actions.

## Table of Contents

- [1. Project Overview](#1-project-overview)
- [2. Core Components & Architecture](#2-core-components--architecture)
    - [2.1. Android Application (Client)](#21-android-application-client)
    - [2.2. Python Server (PC Host)](#22-python-server-pc-host)
- [3. Getting Started](#3-getting-started)
    - [3.1. Installation (Using Pre-built Releases)](#31-installation-using-pre-built-releases)
    - [3.2. Building from Source (For Developers)](#32-building-from-source-for-developers)
- [4. Contributing](#4-contributing)
- [5. License](#5-license)

## 1. Project Overview

The StarButtonBox system consists of:

* **Android Application (Client):** Built with Kotlin and Jetpack Compose, this app serves as the user interface. It displays various buttons organized into customizable layouts (tabs). When a button is pressed, the app sends a corresponding command over the local network using UDP to the PC server.
* **Python Server (PC Host):** A Python script that runs on the PC where the game is played. It listens for UDP packets from the Android app. Upon receiving a command, it parses the data and uses input simulation libraries to execute keyboard or mouse inputs on the PC.

## 2. Core Components & Architecture

### 2.1. Android Application (Client)

* **Technology Stack:** Kotlin, Jetpack Compose (Material 3), Hilt (Dependency Injection), Kotlinx Serialization, Room (Database), Jetpack DataStore (Preferences), Jetpack Navigation, Ktor (for local web server).
* **Key Features:**
    * Splash screen and initial setup flow.
    * Customizable layouts/tabs with various button types (`MomentaryButton`, `SafetyButton`).
    * Free-form layout editor (drag, resize, customize buttons).
    * Layout management (add, edit, delete, reorder, import/export).
    * Macro system with `InputAction` definitions for PC control.
    * UDP networking with mDNS discovery, health checks, and command acknowledgments.
    * Local Ktor web server for PC server setup assistance and layout import.
    * Sound and haptic feedback.

### 2.2. Python Server (PC Host)

* **Location:** The Python server code is located in the `/server` directory of this repository.
* **Technology Stack:** Python 3, Tkinter (for GUI).
* **Key Features:**
    * Listens for UDP commands from the Android client.
    * Simulates keyboard/mouse inputs via `pydirectinput` and `pyautogui`.
    * Advertises on the local network using mDNS (Zeroconf).
    * GUI for configuration, status monitoring, and log viewing.
    * Saves settings and logs to `%APPDATA%\StarButtonBoxServer`.
    * System tray icon for quick access.

## 3. Getting Started

There are two main ways to get StarButtonBox up and running:

### 3.1. Installation (Using Pre-built Releases)

If you want to quickly install and use the application without building it from source, please follow the instructions for installing from pre-built release artifacts:

➡️ **[Installation Instructions (Pre-built Releases)](INSTALLATION.md)**

This guide will walk you through:
* Installing the Android application (`.apk`).
* Downloading and running the PC server installer (`.exe`).
* Configuring the connection between the app and the server.

### 3.2. Building from Source (For Developers)

If you are a developer and want to build the Android app or the PC server from the source code, or contribute to the project, please refer to the following guide:

➡️ **[Building from Source Instructions](BUILDING_FROM_SOURCE.md)**

This guide covers:
* Setting up the development environment for both the Android app and the Python server.
* Building the Android `.apk`.
* Building the PC server `.exe` using PyInstaller.
* Packaging the PC server `.exe` into an installer using Inno Setup.
* It will then link you to the [Installation Instructions](INSTALLATION.md) for installing your self-built artifacts.

## 4. Contributing

Contributions are welcome! Please feel free to submit pull requests, create issues for bugs or feature requests.

*(You can expand this section later with more specific contribution guidelines if needed.)*

## 5. License

*(You should add a LICENSE file to your repository and refer to it here, e.g., MIT License, Apache 2.0, etc.)*

Example:
This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details.
