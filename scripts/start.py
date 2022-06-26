#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script for starting multiple server nodes on the current machine.

Usage: python3 start.py path/to/jar path/to/servers.yml number_of_nodes
"""


from subprocess import Popen, DEVNULL
import sys
import functools


# Always flush
print = functools.partial(print, flush=True)


def run(jar, args):
    """Create a new process to run the jar file"""
    commands = ["java", "-Xmx512m", "-Xms512m", "-jar", jar] + args
    return Popen(commands, stdin=DEVNULL, stdout=DEVNULL)


def kill_processes(processes):
    """Kill processes"""
    print(f"Terminating server processes...")
    for p in processes:
        p.kill()


def start_servers(jar, servers_yml, num_nodes):
    """Start multiple nodes of the server"""
    print(f"Starting server nodes")
    processes = []
    for i in range(num_nodes):
        p = run(jar, [servers_yml, f"{i}"])
        processes.append(p)
    print(f"Started {num_nodes} nodes")
    return processes


if len(sys.argv) != 4:
    print("Usage: python3 start.py path/to/jar path/to/servers.yml number_of_nodes")
    exit(1)

# Start server nodes
server_processes = start_servers(sys.argv[1], sys.argv[2], int(sys.argv[3]))

# Wait for stop command
while True:
    user_input = input(f"Type \"stop\" to terminate server_processes: ")
    if user_input == "stop":
        break

# Terminate server processes
kill_processes(server_processes)

# Wait for termination
print("Waiting server processes to terminate...")
for sp in server_processes:
    sp.wait()
print(f"Server processes terminated")
