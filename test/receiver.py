import socket
import datetime
import threading
import signal
import sys

class UDPListener:
    def __init__(self, port=5800, host='0.0.0.0'):
        self.port = port
        self.host = host
        self.socket = None
        self.running = False
        
    def start_listening(self):
        """Start the UDP listener"""
        try:
            # Create UDP socket
            self.socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
            self.socket.bind((self.host, self.port))
            self.running = True
            
            print(f"UDP Logger started - Listening on {self.host}:{self.port}")
            print("Press Ctrl+C to stop...")
            print("-" * 50)
            
            while self.running:
                try:
                    # Receive data from the socket
                    data, address = self.socket.recvfrom(4096)  # Buffer size of 4096 bytes
                    
                    # Get current timestamp
                    timestamp = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S.%f")[:-3]
                    
                    # Log the received data
                    self.log_message(timestamp, address, data)
                    
                except socket.timeout:
                    continue
                except Exception as e:
                    if self.running:  # Only print error if we're still supposed to be running
                        print(f"Error receiving data: {e}")
                        
        except Exception as e:
            print(f"Error starting UDP listener: {e}")
        finally:
            self.stop_listening()
    
    def log_message(self, timestamp, address, data):
        """Log the received UDP message"""
        try:
            # Try to decode as UTF-8 text
            decoded_data = data.decode('utf-8')
            print(f"[{timestamp}] From {address[0]}:{address[1]} - Text: {decoded_data}")
        except UnicodeDecodeError:
            # If not valid UTF-8, display as hex
            hex_data = data.hex()
            print(f"[{timestamp}] From {address[0]}:{address[1]} - Binary: {hex_data}")
        
        # Optional: Write to log file
        self.write_to_log_file(timestamp, address, data)
    
    def write_to_log_file(self, timestamp, address, data):
        """Write the received data to a log file"""
        try:
            with open('udp_log.txt', 'a', encoding='utf-8') as f:
                try:
                    decoded_data = data.decode('utf-8')
                    f.write(f"[{timestamp}] {address[0]}:{address[1]} - {decoded_data}\n")
                except UnicodeDecodeError:
                    hex_data = data.hex()
                    f.write(f"[{timestamp}] {address[0]}:{address[1]} - Binary: {hex_data}\n")
        except Exception as e:
            print(f"Error writing to log file: {e}")
    
    def stop_listening(self):
        """Stop the UDP listener"""
        self.running = False
        if self.socket:
            self.socket.close()
        print("\nUDP Logger stopped.")

def signal_handler(sig, frame):
    """Handle Ctrl+C gracefully"""
    print("\nReceived interrupt signal...")
    listener.stop_listening()
    sys.exit(0)

if __name__ == "__main__":
    # Create UDP listener instance
    listener = UDPListener(port=5800)
    
    # Set up signal handler for graceful shutdown
    signal.signal(signal.SIGINT, signal_handler)
    
    # Start listening
    listener.start_listening()