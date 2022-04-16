# script for starting multiple server nodes at once

import subprocess
import signal
import sys

jar = "A11.jar"

if len(sys.argv) != 3:
    print("Usage: python3 start.py first_index last_index")
    exit(1)

# parse first_index and last_index
first_index = int(sys.argv[1])
last_index = int(sys.argv[2])
if first_index > last_index or first_index < 0:
    print("Invalid first_index or last_index")
    exit(2)

# capture Ctrl-C
def sigint_handler(signum, frame):
    print("Waiting servers to terminate...")
signal.signal(signal.SIGINT, sigint_handler)

print(f"Starting nodes")

# start servers
servers_subprocesses = []
for i in range(first_index, last_index + 1):
    commands = ["java", "-Xmx512m", "-Xms512m", "-jar", jar, "servers.yml", f"{i}"]
    p = subprocess.Popen(commands, stdout=subprocess.DEVNULL)
    servers_subprocesses.append(p)

print(f"Nodes {first_index} to {last_index} are running, press Ctrl-C to terminate")
