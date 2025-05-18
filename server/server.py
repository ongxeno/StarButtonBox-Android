# server.py
# Main script for the StarButtonBox PC server.

import socket
import json
import time
import sys
import signal
import threading
import logging 
from concurrent.futures import ThreadPoolExecutor

import config
import input_simulator
import mdns_handler 
import dialog_handler
import auto_drag_handler
import config_manager # Import config_manager to get the log path

server_thread = None
stop_server_event = threading.Event()
server_socket = None 
executor = None 

# --- Logging Setup ---
# Use the LOG_FILE_PATH from config_manager
# Ensure config_manager.APP_SETTINGS_DIR is initialized before this logging setup if server.py is run directly
# This typically happens when config_manager is imported and its top-level code runs.
try:
    # Ensure the directory for the log file exists
    log_dir = os.path.dirname(config_manager.LOG_FILE_PATH)
    if not os.path.exists(log_dir):
        os.makedirs(log_dir)
        print(f"Created log directory: {log_dir}") # Print for direct run scenario
    
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s', # Changed from threadName for broader scope
        filename=config_manager.LOG_FILE_PATH, 
        filemode='a' 
    )
except Exception as e:
    # Fallback logging to console if file setup fails
    print(f"Error setting up file logging to {config_manager.LOG_FILE_PATH}: {e}. Logging to console.", file=sys.stderr)
    logging.basicConfig(
        level=logging.INFO,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
        stream=sys.stdout # Log to stdout if file logging fails
    )

logger = logging.getLogger("StarButtonBoxServer") # Get logger instance

log_to_gui_callback = None
update_gui_status_callback = None

# ... (rest of your server.py code remains the same) ...
# Make sure to replace the old logging.basicConfig call with the block above.

# Example of how _server_loop_task would look (no changes needed inside this function itself
# regarding logging, as it uses the 'logger' instance which is now configured globally)
def _server_loop_task(port_to_use, mdns_service_enabled):
    global server_socket, executor, log_to_gui_callback, update_gui_status_callback

    if executor is None: 
        logger.error("ThreadPoolExecutor not initialized before server loop task.")
        if update_gui_status_callback:
            update_gui_status_callback("Error: Executor not ready.")
        return

    if mdns_service_enabled:
        if not mdns_handler.register_mdns_service(port_to_use): 
            logger.warning("Failed to initialize mDNS. Server will run without mDNS.")
            if update_gui_status_callback:
                update_gui_status_callback(f"Running on Port {port_to_use} (mDNS Failed)")
        else:
            logger.info(f"mDNS service registered successfully for port {port_to_use}.")
            if update_gui_status_callback:
                 update_gui_status_callback(f"Running on Port {port_to_use} (mDNS Active)")
    else:
        logger.info("mDNS service is disabled by configuration.")
        if update_gui_status_callback:
            update_gui_status_callback(f"Running on Port {port_to_use} (mDNS Disabled)")

    try:
        server_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        server_socket.bind(('0.0.0.0', port_to_use))
        server_socket.settimeout(1.0) 
        logger.info(f"UDP server listening on port {port_to_use}...")
        if log_to_gui_callback:
            log_to_gui_callback(f"INFO: UDP server listening on port {port_to_use}...")

    except Exception as e:
        logger.error(f"Critical error binding server socket to port {port_to_use}: {e}", exc_info=True)
        if log_to_gui_callback:
            log_to_gui_callback(f"ERROR: Could not bind to port {port_to_use}: {e}")
        if update_gui_status_callback:
            update_gui_status_callback(f"Error: Port {port_to_use} in use?")
        if mdns_service_enabled:
            mdns_handler.unregister_mdns_service() 
        return

    while not stop_server_event.is_set():
        try:
            data_bytes, addr = server_socket.recvfrom(config.BUFFER_SIZE)
            packet_received_time_ns = time.perf_counter_ns()
            json_string = data_bytes.decode('utf-8').strip()

            try:
                packet_data = json.loads(json_string)
            except json.JSONDecodeError as json_e:
                logger.warning(f"Invalid JSON from {addr}: {json_e} - Data: '{json_string[:100]}'")
                if log_to_gui_callback:
                    log_to_gui_callback(f"WARN: Invalid JSON from {addr}: {json_e}")
                continue

            packet_type = packet_data.get('type')
            packet_id = packet_data.get('packetId')
            payload_str = packet_data.get('payload')
            
            gui_log_entry = f"RX: {packet_type} (ID: {packet_id})"
            logger.info(f"Received packet: Type='{packet_type}', ID='{packet_id}', From={addr}, Payload='{str(payload_str)[:50]}...'")
            if log_to_gui_callback:
                log_to_gui_callback(f"INFO: {gui_log_entry}")

            # ... (rest of packet handling logic) ...
            if packet_type == config.PACKET_TYPE_HEALTH_CHECK_PING:
                if packet_id:
                    pong_timestamp = int(time.time() * 1000)
                    pong_packet = {
                        "packetId": packet_id, "timestamp": pong_timestamp,
                        "type": config.PACKET_TYPE_HEALTH_CHECK_PONG, "payload": None
                    }
                    try:
                        server_socket.sendto(json.dumps(pong_packet).encode('utf-8'), addr)
                    except Exception as send_e:
                        logger.error(f"Error sending PONG: {send_e}")
                else:
                    logger.warning("PING missing packetId.")

            elif packet_type == config.PACKET_TYPE_MACRO_COMMAND:
                if not packet_id:
                    logger.warning("MACRO_COMMAND missing packetId. Cannot send ACK or process.")
                    continue
                ack_timestamp = int(time.time() * 1000)
                ack_packet = {
                    "packetId": packet_id, "timestamp": ack_timestamp,
                    "type": config.PACKET_TYPE_MACRO_ACK, "payload": None
                }
                try:
                    server_socket.sendto(json.dumps(ack_packet).encode('utf-8'), addr)
                    logger.info(f"Sent ACK (ID: {packet_id})")
                except Exception as send_e:
                    logger.error(f"Error sending ACK for MACRO_COMMAND (ID: {packet_id}): {send_e}")

                if payload_str and executor:
                    executor.submit(input_simulator.process_macro_in_thread, payload_str, packet_id, packet_received_time_ns)
                elif not payload_str:
                    logger.warning(f"MACRO_COMMAND (ID: {packet_id}) missing payload.")
                elif not executor:
                    logger.error("Executor not available for MACRO_COMMAND.")


            elif packet_type == config.PACKET_TYPE_TRIGGER_IMPORT_BROWSER:
                logger.info(f"Handling TRIGGER_IMPORT_BROWSER (ID: {packet_id})")
                if payload_str:
                    try:
                        trigger_data = json.loads(payload_str)
                        url_to_open = trigger_data.get('url')
                        if url_to_open:
                            dialog_handler.trigger_pc_browser(url_to_open)
                        else:
                            logger.error("Missing 'url' in TRIGGER_IMPORT_BROWSER payload.")
                    except Exception as e:
                        logger.error(f"Error processing TRIGGER_IMPORT_BROWSER payload: {e}")
                else:
                    logger.warning(f"TRIGGER_IMPORT_BROWSER (ID: {packet_id}) missing payload.")

            elif packet_type == config.PACKET_TYPE_CAPTURE_MOUSE_POSITION:
                logger.info(f"Handling CAPTURE_MOUSE_POSITION (ID: {packet_id})")
                if payload_str:
                    try:
                        capture_payload = json.loads(payload_str)
                        purpose = capture_payload.get('purpose')
                        if purpose in ["SRC", "DES"]:
                            auto_drag_handler.capture_mouse_position(purpose)
                        else:
                            logger.error(f"Invalid 'purpose' ('{purpose}') in CAPTURE_MOUSE_POSITION payload.")
                    except Exception as e:
                        logger.error(f"Error processing CAPTURE_MOUSE_POSITION: {e}")
                else:
                    logger.warning(f"CAPTURE_MOUSE_POSITION (ID: {packet_id}) missing payload.")

            elif packet_type == config.PACKET_TYPE_AUTO_DRAG_LOOP_COMMAND:
                logger.info(f"Handling AUTO_DRAG_LOOP_COMMAND (ID: {packet_id})")
                if payload_str:
                    try:
                        loop_payload = json.loads(payload_str)
                        action = loop_payload.get('action')
                        if action == "START":
                            auto_drag_handler.start_auto_drag_loop()
                        elif action == "STOP":
                            auto_drag_handler.stop_auto_drag_loop()
                        else:
                            logger.error(f"Invalid 'action' ('{action}') in AUTO_DRAG_LOOP_COMMAND payload.")
                    except Exception as e:
                        logger.error(f"Error processing AUTO_DRAG_LOOP_COMMAND: {e}")
                else:
                    logger.warning(f"AUTO_DRAG_LOOP_COMMAND (ID: {packet_id}) missing payload.")
            else:
                logger.warning(f"Unknown packet type '{packet_type}'.")


        except socket.timeout:
            continue
        except UnicodeDecodeError:
            logger.error(f"Cannot decode UTF-8 from {addr if 'addr' in locals() else 'unknown sender'}.")
        except Exception as loop_e:
            logger.error(f"Error processing packet from {addr if 'addr' in locals() else 'unknown sender'}: {loop_e}", exc_info=True)

    logger.info("Server loop task stopping.")
    if server_socket:
        server_socket.close()
        logger.info("Server socket closed in loop task.")
    if mdns_service_enabled:
        mdns_handler.unregister_mdns_service()
        logger.info("mDNS service unregistered in loop task.")
    if update_gui_status_callback:
        update_gui_status_callback("Server Stopped")


def start_server(port, mdns_enabled, gui_log_cb, gui_status_cb):
    global server_thread, stop_server_event, executor
    global log_to_gui_callback, update_gui_status_callback

    log_to_gui_callback = gui_log_cb
    update_gui_status_callback = gui_status_cb

    if server_thread and server_thread.is_alive():
        logger.warning("Server is already running.")
        if log_to_gui_callback:
            log_to_gui_callback("WARN: Server is already running.")
        return False

    if executor is None or executor._shutdown: 
        executor = ThreadPoolExecutor(max_workers=10, thread_name_prefix='MacroWorker')
        logger.info(f"ThreadPoolExecutor initialized/re-initialized with max_workers={executor._max_workers}")

    stop_server_event.clear() 
    server_thread = threading.Thread(
        target=_server_loop_task,
        args=(port, mdns_enabled),
        name="ServerLoopThread",
        daemon=True 
    )
    server_thread.start()
    logger.info(f"Server thread started. Target port: {port}, mDNS: {mdns_enabled}")
    if log_to_gui_callback:
        log_to_gui_callback(f"INFO: Server thread starting. Port: {port}, mDNS: {mdns_enabled}")
    if update_gui_status_callback:
        update_gui_status_callback("Server Starting...")
    return True

def stop_server():
    global server_thread, stop_server_event, server_socket, executor
    global log_to_gui_callback, update_gui_status_callback

    logger.info("Attempting to stop server...")
    if log_to_gui_callback:
        log_to_gui_callback("INFO: Attempting to stop server...")

    stop_server_event.set() 
    auto_drag_handler.stop_auto_drag_loop()

    if server_thread and server_thread.is_alive():
        logger.info("Waiting for server thread to join...")
        server_thread.join(timeout=3.0) 
        if server_thread.is_alive():
            logger.warning("Server thread did not join in time.")
            if log_to_gui_callback:
                log_to_gui_callback("WARN: Server thread did not join in time.")
        else:
            logger.info("Server thread joined successfully.")
            if log_to_gui_callback:
                log_to_gui_callback("INFO: Server thread stopped.")
    else:
        logger.info("Server thread was not running or already stopped.")
        if log_to_gui_callback:
            log_to_gui_callback("INFO: Server was not running.")

    if server_socket:
        try:
            server_socket.close()
            logger.info("Server socket closed by stop_server function.")
        except Exception as e:
            logger.error(f"Error closing server socket in stop_server: {e}")
    server_socket = None

    mdns_handler.unregister_mdns_service() 

    if executor and not executor._shutdown:
        logger.info("Shutting down ThreadPoolExecutor...")
        executor.shutdown(wait=True) 
        logger.info("ThreadPoolExecutor shutdown complete.")
    executor = None 

    if update_gui_status_callback:
        update_gui_status_callback("Server Stopped")
    logger.info("Server stop process complete.")

