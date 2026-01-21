import socket
import time
import random
import argparse
import json

class UDPSender:
    def __init__(self, target_host='127.0.0.1', target_port=5800):
        self.target_host = target_host
        self.target_port = target_port
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    
    def send_message(self, message):
        """Send a single UDP message"""
        try:
            if isinstance(message, str):
                data = message.encode('utf-8')
            else:
                data = message
            
            self.socket.sendto(data, (self.target_host, self.target_port))
            print(f"Sent: {message} to {self.target_host}:{self.target_port}")
            return True
        except Exception as e:
            print(f"Error sending message: {e}")
            return False
    
    def send_test_messages(self):
        """Send a series of test messages"""
        test_messages = [
            "Hello UDP Logger!",
            "This is a test message",
            "Message with timestamp: " + str(time.time()),
            "Special characters: !@#$%^&*()",
            "Numbers: 12345",
            json.dumps({"type": "test", "data": "JSON message", "timestamp": time.time()}),
            "🚀 Unicode test message 🎯",
            "Long message: " + "A" * 100
        ]
        
        print(f"Sending {len(test_messages)} test messages to {self.target_host}:{self.target_port}")
        print("-" * 50)
        
        for i, message in enumerate(test_messages, 1):
            print(f"[{i}/{len(test_messages)}] ", end="")
            self.send_message(message)
            time.sleep(1)  # Wait 1 second between messages
        
        print("-" * 50)
        print("All test messages sent!")
    
    def send_continuous_messages(self, interval=2):
        """Send messages continuously at specified intervals"""
        print(f"Sending continuous messages every {interval} seconds to {self.target_host}:{self.target_port}")
        print("Press Ctrl+C to stop...")
        print("-" * 50)
        
        counter = 1
        try:
            while True:
                timestamp = time.strftime("%Y-%m-%d %H:%M:%S")
                message = f"Continuous message #{counter} at {timestamp}"
                self.send_message(message)
                counter += 1
                time.sleep(interval)
        except KeyboardInterrupt:
            print("\nStopped sending continuous messages.")
    
    def send_random_data(self, count=10):
        """Send random binary data"""
        print(f"Sending {count} random binary messages to {self.target_host}:{self.target_port}")
        print("-" * 50)
        
        for i in range(count):
            # Generate random binary data
            data_length = random.randint(10, 100)
            random_data = bytes([random.randint(0, 255) for _ in range(data_length)])
            
            try:
                self.socket.sendto(random_data, (self.target_host, self.target_port))
                print(f"[{i+1}/{count}] Sent random binary data ({data_length} bytes): {random_data.hex()[:40]}...")
            except Exception as e:
                print(f"Error sending random data: {e}")
            
            time.sleep(0.5)
        
        print("Random data sending complete!")
    
    def interactive_mode(self):
        """Interactive mode for sending custom messages"""
        print(f"Interactive UDP sender - Target: {self.target_host}:{self.target_port}")
        print("Type your messages (press Enter to send, 'quit' to exit):")
        print("-" * 50)
        
        while True:
            try:
                message = input("Message: ")
                if message.lower() in ['quit', 'exit', 'q']:
                    print("Exiting interactive mode.")
                    break
                elif message.strip():
                    self.send_message(message)
                else:
                    print("Please enter a message or 'quit' to exit.")
            except KeyboardInterrupt:
                print("\nExiting interactive mode.")
                break
    
    def close(self):
        """Close the socket"""
        self.socket.close()

def main():
    parser = argparse.ArgumentParser(description='UDP Message Sender for testing UDP Logger')
    parser.add_argument('--host', '-H', default='127.0.0.1', help='Target host (default: 127.0.0.1)')
    parser.add_argument('--port', '-p', type=int, default=5800, help='Target port (default: 5800)')
    parser.add_argument('--mode', '-m', choices=['test', 'continuous', 'random', 'interactive'], 
                       default='test', help='Sending mode (default: test)')
    parser.add_argument('--interval', '-i', type=float, default=2.0, 
                       help='Interval between messages in continuous mode (default: 2.0 seconds)')
    parser.add_argument('--count', '-c', type=int, default=10, 
                       help='Number of messages to send in random mode (default: 10)')
    parser.add_argument('--message', help='Single message to send')
    
    args = parser.parse_args()
    
    # Create sender instance
    sender = UDPSender(args.host, args.port)
    
    try:
        if args.message:
            # Send single custom message
            sender.send_message(args.message)
        elif args.mode == 'test':
            # Send predefined test messages
            sender.send_test_messages()
        elif args.mode == 'continuous':
            # Send continuous messages
            sender.send_continuous_messages(args.interval)
        elif args.mode == 'random':
            # Send random binary data
            sender.send_random_data(args.count)
        elif args.mode == 'interactive':
            # Interactive mode
            sender.interactive_mode()
    finally:
        sender.close()

if __name__ == "__main__":
    main()