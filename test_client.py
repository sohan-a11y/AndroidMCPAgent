import asyncio
import websockets
import json

# Configuration
PHONE_IP = input("Enter your phone's IP address (e.g., 192.168.1.50): ")
PORT = 8765
URI = f"ws://{PHONE_IP}:{PORT}/mcp"

async def test_mcp_agent():
    print(f"Connecting to {URI}...")
    try:
        async with websockets.connect(URI) as websocket:
            print("Connected!")
            
            # 1. Pairing
            pairing_code = input("Enter the 6-digit pairing code from the app: ")
            pair_cmd = {
                "id": "init-1",
                "action": "pair",
                "params": {
                    "code": pairing_code,
                    "client_id": "test-script-pc"
                }
            }
            
            print(f"> Sending pair command: {json.dumps(pair_cmd)}")
            await websocket.send(json.dumps(pair_cmd))
            
            response = await websocket.recv()
            print(f"< Received: {response}")
            
            resp_data = json.loads(response)
            if "error" in resp_data:
                print("Pairing failed!")
                return
                
            auth_token = resp_data["result"]["auth_token"]
            print(f"\nAuthenticated! Token: {auth_token}")
            
            # 2. Ping
            ping_cmd = {
                "id": "cmd-1",
                "action": "ping",
                "auth_token": auth_token
            }
            print(f"\n> Sending ping...")
            await websocket.send(json.dumps(ping_cmd))
            print(f"< {await websocket.recv()}")
            
            # 3. Get Device State (Basic info)
            # functionality might vary depending on implementation completeness
            # but let's try a simple command if supported, or just list apps
            list_apps_cmd = {
                "id": "cmd-2",
                "action": "list_apps",
                "auth_token": auth_token
            }
            print(f"\n> Requesting app list...")
            await websocket.send(json.dumps(list_apps_cmd))
            print(f"< {await websocket.recv()}")
            
            print("\nTest complete! The agent is working.")
            
    except ConnectionRefusedError:
        print("\nConnection failed! Make sure:")
        print("1. The app is running and 'Start Server' is clicked.")
        print("2. Your phone and PC are on the same Wi-Fi.")
        print("3. You entered the correct IP address.")
    except Exception as e:
        print(f"\nAn error occurred: {e}")

if __name__ == "__main__":
    print("--- Android MCP Agent Tester ---")
    asyncio.run(test_mcp_agent())
