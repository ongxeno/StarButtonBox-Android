# mdns_handler.py
# Handles mDNS service registration/unregistration using Zeroconf.

import socket
import sys
# import ipaddress # Not strictly needed here anymore, but good practice
from zeroconf import ServiceInfo, Zeroconf, IPVersion
import config # Import constants from config.py
import logging

logger = logging.getLogger("StarButtonBoxMDNS") # Specific logger for this module

# --- Module-level variables for Zeroconf instance and service info ---
_zeroconf_instance = None
_service_info_instance = None

def get_local_ip():
    """Attempts to find a non-loopback local IPv4 address."""
    try:
        s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        s.settimeout(0)
        # Doesn't send data, attempts to connect to an external address
        # to determine the interface that would be used.
        s.connect(('8.8.8.8', 1)) 
        ip = s.getsockname()[0]
        s.close()
        if ip and ip != '127.0.0.1':
            return ip
    except Exception:
        # Fallback or log error
        pass # logger.debug("get_local_ip: Could not connect to external to find IP.")

    # Fallback: Try getting IP from hostname
    try:
        hostname = socket.gethostname()
        # Ensure gethostbyname returns IPv4 if multiple are available or prefer IPv4
        # Forcing AF_INET for getaddrinfo might be more robust if issues persist
        ip_addresses = socket.getaddrinfo(hostname, None, socket.AF_INET)
        for res in ip_addresses:
            ip = res[4][0] # Address is in the 5th element, first item of the tuple
            if ip and ip != '127.0.0.1' and not ip.startswith("169.254"): # Avoid link-local
                 logger.debug(f"get_local_ip: Found IP {ip} via hostname method.")
                 return ip
    except socket.gaierror:
        logger.warning("get_local_ip: Could not resolve hostname to IP.")
    except Exception as e:
        logger.error(f"get_local_ip: Unexpected error getting IP via hostname: {e}")


    logger.warning("Could not determine a preferred non-loopback IP address. Using 127.0.0.1 as last resort.")
    return "127.0.0.1" # Last resort

def register_mdns_service(actual_port: int): # <<< MODIFIED: Accept actual_port
    """
    Registers the StarButtonBox service using Zeroconf.
    Uses the actual_port the server is listening on.
    """
    global _zeroconf_instance, _service_info_instance
    if _zeroconf_instance:
        logger.warning("mDNS service already registered or registration in progress.")
        # Optionally, unregister and re-register if port changed,
        # but server restart should handle this.
        return True 

    try:
        # Force IPv4 for Zeroconf to align with typical local network discovery needs
        _zeroconf_instance = Zeroconf(ip_version=IPVersion.V4Only)
        local_ip = get_local_ip()

        # Ensure hostname doesn't have problematic characters for mDNS
        host_name_raw = socket.gethostname().split('.')[0]
        host_name = "".join(c if c.isalnum() or c == '-' else '' for c in host_name_raw)
        if not host_name: # Fallback if hostname becomes empty after sanitizing
            host_name = "StarButtonBoxPC"
        
        service_name_str = f"{host_name} StarButtonBox Server.{config.MDNS_SERVICE_TYPE}"
        server_identifier = f"{host_name}.local."


        _service_info_instance = ServiceInfo(
            type_=config.MDNS_SERVICE_TYPE,
            name=service_name_str,
            addresses=[socket.inet_aton(local_ip)], 
            port=actual_port, # <<< MODIFIED: Use actual_port
            properties={}, # Empty properties for now
            server=server_identifier, 
        )
        logger.info(f"Registering mDNS service:")
        logger.info(f"  Name: {service_name_str}")
        logger.info(f"  Type: {config.MDNS_SERVICE_TYPE}")
        logger.info(f"  Address: {local_ip}")
        logger.info(f"  Port: {actual_port}") # <<< Log actual_port
        
        _zeroconf_instance.register_service(_service_info_instance)
        logger.info("mDNS service registered successfully.")
        return True
    except Exception as e:
        logger.error(f"Error registering mDNS service: {e}", exc_info=True)
        if _zeroconf_instance:
            try:
                _zeroconf_instance.close()
            except Exception as close_e:
                logger.error(f"Error closing Zeroconf instance during register_mdns_service error handling: {close_e}")
        _zeroconf_instance = None
        _service_info_instance = None
        return False

def unregister_mdns_service():
    """Unregisters the mDNS service and closes Zeroconf."""
    global _zeroconf_instance, _service_info_instance
    if _zeroconf_instance and _service_info_instance:
        logger.info("Unregistering mDNS service...")
        try:
            _zeroconf_instance.unregister_service(_service_info_instance)
        except Exception as e:
            # Log error but proceed to close Zeroconf
            logger.error(f"Error during mDNS service unregistration: {e}", exc_info=True)
        
        try:
            _zeroconf_instance.close()
            logger.info("mDNS service unregistered and Zeroconf closed.")
        except Exception as e:
            logger.error(f"Error closing Zeroconf: {e}", exc_info=True)

    elif _zeroconf_instance: # If service_info was None but instance exists
        logger.info("Closing Zeroconf (service likely not registered or already unregistered)...")
        try:
            _zeroconf_instance.close()
        except Exception as e:
            logger.error(f"Error closing Zeroconf (when service_info was None): {e}", exc_info=True)

    _zeroconf_instance = None
    _service_info_instance = None

if __name__ == '__main__':
    # Setup basic console logging for testing this module directly
    logging.basicConfig(stream=sys.stdout, level=logging.INFO,
                        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
    
    logger_test_main = logging.getLogger("MDNSHandlerTest")
    test_port_to_advertise = 5055 # Example port for testing

    logger_test_main.info("--- Testing mDNS Handler ---")
    if register_mdns_service(test_port_to_advertise): # Pass the test port
        logger_test_main.info(f"mDNS service registration initiated on port {test_port_to_advertise} (check with mDNS browser).")
        logger_test_main.info("Press Ctrl+C to unregister and exit test.")
        try:
            while True:
                import time # Keep import local to if __name__
                time.sleep(1)
        except KeyboardInterrupt:
            logger_test_main.info("\nKeyboard interrupt received.")
        finally:
            unregister_mdns_service()
            logger_test_main.info("mDNS test finished.")
    else:
        logger_test_main.error(f"Failed to register mDNS service on port {test_port_to_advertise} during test.")
