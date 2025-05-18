; StarButtonBoxServer_Installer.iss
; Inno Setup script for StarButtonBox PC Server

[Setup]
; NOTE: The AppId is a unique identifier for your application.
; It's recommended to generate a new GUID if you copy this script for other projects.
; You can generate one here: https://www.guidgenerator.com/
AppId={{99799798-64d2-4231-941f-cbe1742bfaa4}}
AppName=StarButtonBox Server
AppVersion=1.0 
;AppVerName=StarButtonBox Server 1.0
AppPublisher=OngXeno (Your Name/Company)
AppPublisherURL=https://github.com/ongxeno/starbuttonbox-android 
AppSupportURL=https://github.com/ongxeno/starbuttonbox-android/issues
AppUpdatesURL=https://github.com/ongxeno/starbuttonbox-android/releases
DefaultDirName={autopf}\StarButtonBox Server
DefaultGroupName=StarButtonBox Server
AllowNoIcons=yes
LicenseFile=
InfoBeforeFile=
InfoAfterFile=
OutputBaseFilename=StarButtonBoxServer_Installer_v1.0
Compression=lzma
SolidCompression=yes
WizardStyle=modern

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked
Name: "quicklaunchicon"; Description: "{cm:CreateQuickLaunchIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Flags: unchecked; OnlyBelowVersion: 0,6.1;

[Files]
; Source: "Path\To\Your\PyInstaller\Output\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
; IMPORTANT: Adjust the Source path to where your 'StarButtonBoxServer' folder (from PyInstaller's dist) is located
; relative to this .iss script file.
; If StarButtonBoxServer_Installer.iss is in server/ and dist is in server/dist/
Source: "dist\StarButtonBoxServer\*"; DestDir: "{app}"; Flags: ignoreversion recursesubdirs createallsubdirs
; NOTE: Don't forget to include your tray_icon.png if it's not already handled by PyInstaller's .spec file's datas section.
; If tray_icon.png is inside the StarButtonBoxServer folder (e.g., bundled by PyInstaller), the line above covers it.
; If icon.ico for the installer itself is separate:
Source: "icon.ico"; DestDir: "{app}"; Flags: ignoreversion

[Icons]
Name: "{group}\StarButtonBox Server"; Filename: "{app}\StarButtonBoxServer.exe"
Name: "{group}\{cm:ProgramOnTheWeb,StarButtonBox Server}"; Filename: "https://github.com/ongxeno/starbuttonbox-android"
Name: "{group}\{cm:UninstallProgram,StarButtonBox Server}"; Filename: "{uninstallexe}"
Name: "{autodesktop}\StarButtonBox Server"; Filename: "{app}\StarButtonBoxServer.exe"; Tasks: desktopicon
Name: "{userappdata}\Microsoft\Internet Explorer\Quick Launch\StarButtonBox Server"; Filename: "{app}\StarButtonBoxServer.exe"; Tasks: quicklaunchicon; OnlyBelowVersion: 0,6.1;

[Run]
; Run the application after installation, if the user agrees
Filename: "{app}\StarButtonBoxServer.exe"; Description: "{cm:LaunchProgram,StarButtonBox Server}"; Flags: nowait postinstall skipifsilent unchecked

[UninstallDelete]
; Always remove the main application directory
Type: filesandordirs; Name: "{app}"
; Conditionally remove user settings and logs from AppData
Type: filesandordirs; Name: "{userappdata}\StarButtonBoxServer"; Tasks: removeusersettings