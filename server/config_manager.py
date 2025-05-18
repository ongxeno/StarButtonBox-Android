# config_manager.py
# Handles loading and saving server configurations from/to a JSON file,
# and manages Windows auto-start registry settings.

import json
import os
import sys
import config # For default values
import winreg # For Windows registry operations
import logging

logger = logging.getLogger("StarButtonBoxServerConfig")

# --- Application Data Directory ---
# This will be %APPDATA%\StarButtonBoxServer for the frozen app,
# or the script's directory for development.
APP_DATA_SUBFOLDER = "StarButtonBoxServer"

def get_app_data_dir():
    """Determines the appropriate directory for application data (settings, logs)."""
    if getattr(sys, 'frozen', False): # Running as a bundled app (EXE)
        try:
            app_data_root = os.getenv('APPDATA')
            if not app_data_root:
                # Fallback if APPDATA is not set (highly unlikely, but defensive)
                app_data_root = os.path.expanduser("~")
                logger.warning(f"APPDATA environment variable not found. Using user home: {app_data_root}")
            
            settings_dir = os.path.join(app_data_root, APP_DATA_SUBFOLDER)
        except Exception as e:
            logger.error(f"Error determining APPDATA path: {e}. Falling back to executable directory.")
            # Fallback to executable's directory (use with caution, may not be writable)
            settings_dir = os.path.join(os.path.dirname(os.path.abspath(sys.executable)), APP_DATA_SUBFOLDER)
    else: # Running as a script
        settings_dir = os.path.dirname(os.path.abspath(__file__))
        # Optionally, for development, you might still want to use a subfolder in APPDATA
        # or a subfolder relative to the script (e.g., "data")
        # For simplicity here, script's dir is used for dev.

    try:
        if not os.path.exists(settings_dir):
            os.makedirs(settings_dir)
            logger.info(f"Created application data directory: {settings_dir}")
    except OSError as e:
        logger.error(f"Could not create application data directory {settings_dir}: {e}")
        # If directory creation fails, subsequent file operations will likely fail too.
        # The application might not be usable if this happens in a frozen app.
    return settings_dir

APP_SETTINGS_DIR = get_app_data_dir()
SETTINGS_FILE_NAME = "server_settings.json"
SETTINGS_FILE_PATH = os.path.join(APP_SETTINGS_DIR, SETTINGS_FILE_NAME)
LOG_FILE_PATH = os.path.join(APP_SETTINGS_DIR, "server.log") # Define log file path here

# --- Default Settings ---
DEFAULT_SETTINGS = {
    "server_port": config.COMMAND_PORT,
    "mdns_enabled": True,
    "autostart_enabled": False,
    "executable_path_for_autostart": "",
    "minimize_to_tray_on_exit": False,
    "start_minimized_to_tray": False
}

# --- Registry Settings for Auto-Start ---
AUTOSTART_REG_KEY_PATH = r"Software\Microsoft\Windows\CurrentVersion\Run"
AUTOSTART_APP_NAME = "StarButtonBoxServer"


def load_settings():
    """
    Loads settings from the JSON file in the application data directory.
    """
    if os.path.exists(SETTINGS_FILE_PATH):
        try:
            with open(SETTINGS_FILE_PATH, 'r') as f:
                content = f.read()
                if not content.strip():
                    logger.info(f"Settings file '{SETTINGS_FILE_PATH}' is empty. Using defaults.")
                    return DEFAULT_SETTINGS.copy()
                # f.seek(0) # Not needed if reading all content first
                settings_from_file = json.loads(content) # Use content directly
                loaded_settings = DEFAULT_SETTINGS.copy()
                loaded_settings.update(settings_from_file)
                return loaded_settings
        except json.JSONDecodeError:
            logger.error(f"Could not decode JSON from '{SETTINGS_FILE_PATH}'. Using default settings.")
            return DEFAULT_SETTINGS.copy()
        except Exception as e:
            logger.error(f"Could not read settings file '{SETTINGS_FILE_PATH}': {e}. Using default settings.")
            return DEFAULT_SETTINGS.copy()
    else:
        logger.info(f"Settings file '{SETTINGS_FILE_PATH}' not found. Using default settings and creating it.")
        _save_settings_to_file(DEFAULT_SETTINGS.copy()) # Create with defaults
        return DEFAULT_SETTINGS.copy()

def save_settings(port=None, mdns_enabled=None, autostart_enabled=None, executable_path=None,
                  minimize_to_tray_on_exit=None, start_minimized_to_tray=None):
    """
    Saves the provided settings to the JSON file in the application data directory.
    """
    current_settings = load_settings() # Load current or defaults

    if port is not None:
        current_settings["server_port"] = int(port)
    if mdns_enabled is not None:
        current_settings["mdns_enabled"] = bool(mdns_enabled)
    if autostart_enabled is not None:
        current_settings["autostart_enabled"] = bool(autostart_enabled)
    if executable_path is not None: # Should be the full path to the installed .exe
        current_settings["executable_path_for_autostart"] = str(executable_path)
    if minimize_to_tray_on_exit is not None:
        current_settings["minimize_to_tray_on_exit"] = bool(minimize_to_tray_on_exit)
    if start_minimized_to_tray is not None:
        current_settings["start_minimized_to_tray"] = bool(start_minimized_to_tray)

    return _save_settings_to_file(current_settings)

def _save_settings_to_file(settings_dict):
    """Internal helper to write the settings dictionary to the file."""
    try:
        # Ensure directory exists before trying to write
        if not os.path.exists(APP_SETTINGS_DIR):
            logger.info(f"Settings directory {APP_SETTINGS_DIR} does not exist. Attempting to create.")
            os.makedirs(APP_SETTINGS_DIR)

        with open(SETTINGS_FILE_PATH, 'w') as f:
            json.dump(settings_dict, f, indent=4)
        logger.info(f"Settings saved to '{SETTINGS_FILE_PATH}'")
        return True
    except Exception as e:
        logger.error(f"Could not save settings to '{SETTINGS_FILE_PATH}': {e}")
        return False

def get_setting(key, default_value=None):
    """Helper to get a specific setting."""
    settings = load_settings()
    return settings.get(key, default_value if default_value is not None else DEFAULT_SETTINGS.get(key))


def set_autostart_in_registry(enable: bool, executable_path_for_autostart: str):
    """
    Configures the application to start with Windows.
    The executable_path_for_autostart should be the full path to the installed EXE.
    """
    if enable and not executable_path_for_autostart:
        logger.error("Cannot enable autostart: executable_path_for_autostart is missing.")
        return False
    if enable and not os.path.exists(executable_path_for_autostart):
        # This check might be problematic if the path is to an installer-generated shortcut.
        # For direct EXE autostart, it's valid.
        logger.warning(f"Autostart executable path '{executable_path_for_autostart}' does not seem to exist. Proceeding with registry write.")
        # return False # Commented out to allow setting even if path seems invalid at this stage

    try:
        # Using HKEY_CURRENT_USER for autostart doesn't require admin rights.
        key = winreg.OpenKey(winreg.HKEY_CURRENT_USER, AUTOSTART_REG_KEY_PATH, 0, winreg.KEY_WRITE)
        if enable:
            # Path should be the installed EXE path, potentially with arguments.
            # Example: "C:\Program Files (x86)\StarButtonBox Server\StarButtonBoxServer.exe" --start-minimized
            path_to_register = f'"{os.path.normpath(executable_path_for_autostart)}"'
            
            # Load current settings to check start_minimized_to_tray
            current_settings = load_settings()
            if current_settings.get("start_minimized_to_tray", False):
                path_to_register += " --start-minimized"
                
            winreg.SetValueEx(key, AUTOSTART_APP_NAME, 0, winreg.REG_SZ, path_to_register)
            logger.info(f"Autostart enabled: Added '{path_to_register}' to registry for '{AUTOSTART_APP_NAME}'.")
        else:
            try:
                winreg.DeleteValue(key, AUTOSTART_APP_NAME)
                logger.info(f"Autostart disabled: Removed '{AUTOSTART_APP_NAME}' from registry.")
            except FileNotFoundError:
                logger.info(f"Autostart was not set for '{AUTOSTART_APP_NAME}' (DeleteValue).")
        winreg.CloseKey(key)
        return True
    except PermissionError:
        logger.error(f"Permission denied when trying to access registry for autostart.")
        return False
    except FileNotFoundError: # This would be for the registry key itself, highly unlikely for HKCU CurrentVersion\Run
        logger.error(f"Registry path '{AUTOSTART_REG_KEY_PATH}' not found for autostart.")
        return False
    except Exception as e:
        logger.error(f"Failed to {'enable' if enable else 'disable'} autostart: {e}")
        return False

# Ensure logging is configured if this script is run directly for testing
if __name__ == '__main__':
    logging.basicConfig(level=logging.DEBUG, format='%(asctime)s - %(name)s - %(levelname)s - %(message)s', stream=sys.stdout)
    logger.info("Testing config_manager.py...")
    print("Application Data Directory:", APP_SETTINGS_DIR)
    print("Settings File Path:", SETTINGS_FILE_PATH)
    print("Log File Path:", LOG_FILE_PATH)

    settings = load_settings()
    print(f"\nInitial/Loaded settings: {settings}")

    save_settings(port=5056, mdns_enabled=False)
    settings = load_settings()
    print(f"Modified settings: {settings}")
    assert settings["server_port"] == 5056
    assert settings["mdns_enabled"] is False

    # Test autostart (this will write to the current user's registry)
    # For a real test, you'd need a dummy executable path.
    # Using this script's path for placeholder.
    test_exe_path_for_autostart = os.path.abspath(sys.executable if getattr(sys, 'frozen', False) else __file__)
    print(f"\nTesting autostart with executable: {test_exe_path_for_autostart}")
    if set_autostart_in_registry(True, test_exe_path_for_autostart):
        print("  Autostart (likely) enabled. Check registry.")
        settings = load_settings() # reload to see if executable_path_for_autostart was updated by save_settings
        # Note: set_autostart_in_registry itself doesn't call save_settings for executable_path_for_autostart
        # That's typically handled by the GUI when the checkbox is toggled.
        # For this test, we'd manually save it if we wanted that field updated in JSON.
        # config_manager.save_settings(autostart_enabled=True, executable_path=test_exe_path_for_autostart)


    if set_autostart_in_registry(False, test_exe_path_for_autostart):
        print("  Autostart (likely) disabled. Check registry.")

    print("\nConfig manager test finished.")
