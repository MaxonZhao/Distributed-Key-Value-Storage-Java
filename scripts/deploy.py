#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Script for deploying and testing the distributed key-value store on remote machines. It is intended for deploying the
server and the client each on a newly created virtual private server running Ubuntu Server 20.04 LTS. Please refer to
README.md for the usage of this script.
"""


import os
from argparse import ArgumentParser


# Update the following variables for each assignment
server_program = "./A12.jar"  # path to the server's jar file
client_jar = "./a11_2022_eval_tests_v3.jar"  # path to the client jar file provided by the teaching team
client_log = "A11.log"  # name of the client's log file
n_nodes = 40  # number of server nodes

# Other configuration
start_py = os.path.dirname(__file__) + "/start.py"  # script for starting multiple server nodes
username = "ubuntu"  # ssh username
work_dir = f"/home/{username}"  # working directory on the remote machine
server_start_port = 20000  # lower bound of the port numbers used by server nodes
local_port = 43100  # port number of the single-node instance

# The following files will be generated by this script
servers_yml = "servers.yml"  # filename of the yml file containing node membership information
servers_yml_local = "servers_local.yml"  # for the single-node instance
servers_txt = "servers.txt"  # filename of the txt file containing addresses of the server

# Additional ssh public key added to the server machine to allow the test client to suspend and resume server processes
additional_pub_key = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCfxrmM7cpw0hRWnshggt/2XpQ/nVi/Olotia6pxK7Aacg1wt4yDdS8e" \
                     "Z3Lto4JsRiI+hUZZ2uyuZSy3zgtWai+mGu3TshKP1bz+wC7RHenZwgEXuPn2WuIofTTjFfGIq6uOPWc5D6v0We2bVlrXF" \
                     "bYvHP4J24QkH9vsAfkpL5UGa+l+PT+L3phgVeTPs/BxuhaBp6J+j9g+UVF86di7u01fTuevjKRojzJe9kBRFQ4t95XsL0" \
                     "QDXOIB2GD7PSblxaftVINmiPpeyJPGMyZWqDuXyhhF37rRgaq9gHnnuNo4SVrR+8lJbd14c+bgHCM/FSZI43vkynnBPX6" \
                     "XFSJN7qv cpen431_pop"
additional_client_parameters = "-secret=3655741786"


def send_file_to_server(file):
    os.system(f"scp -i {args.key} {file} {username}@{args.server_public_ip}:{work_dir}/")


def send_file_to_client(file):
    os.system(f"scp -i {args.key} {file} {username}@{args.client_ip}:{work_dir}/")


def run_command_on_client(command):
    os.system(f"ssh -i {args.key} {username}@{args.client_ip} \"{command}\"")


def run_command_on_server(command):
    os.system(f"ssh -i {args.key} {username}@{args.server_public_ip} \"{command}\"")


def download_path_from_server(folder):
    os.system(f"scp -i {args.key} -r {username}@{args.server_public_ip}:{work_dir}/{folder} ./")


def download_path_from_client(folder):
    os.system(f"scp -i {args.key} -r {username}@{args.client_ip}:/{work_dir}/{folder} ./")


def generate_server_yml():
    """Generate servers.yml, which contains node membership information."""
    with open(servers_yml, 'w') as yml_file:
        yml_file.write("serverInfo:\n")
        port = server_start_port
        for _ in range(n_nodes):
            yml_file.write(f"  - ip: {args.server_private_ip}\n")
            yml_file.write(f"    serverPort: {port}\n")
            port += 1
            yml_file.write(f"    udpPort: {port}\n")
            port += 1
            yml_file.write(f"    tcpPort: {port}\n")
            port += 1


def generate_local_server_yml():
    """Generate servers_local.yml, which will contain only a node at 127.0.0.1:43100"""
    with open(servers_yml_local, 'w') as yml_file:
        yml_file.write("serverInfo:\n")
        yml_file.write(f"  - ip: 127.0.0.1\n")
        yml_file.write(f"    serverPort: {local_port}\n")
        yml_file.write(f"    udpPort: {local_port + 1}\n")
        yml_file.write(f"    tcpPort: {local_port + 2}\n")


def generate_servers_txt():
    """Generate servers.txt, which contains addresses of the server, for the client."""
    with open(servers_txt, 'w') as txt_file:
        port = server_start_port
        for _ in range(n_nodes):
            txt_file.write(f"{args.server_public_ip}:{port}\n")
            port += 3


def op_init_server():
    """Setup software environment on the server machine."""
    run_command_on_server("sudo apt update")
    run_command_on_server("sudo apt -y install openjdk-8-jre-headless net-tools")
    # Increase maximum buffer sizes to improve performance
    run_command_on_server("sudo sysctl -w net.core.rmem_max=67108864")
    run_command_on_server("sudo sysctl -w net.core.wmem_max=67108864")
    # Add package loss and delay
    run_command_on_server("sudo tc qdisc add dev lo   root netem delay 5msec loss 0.25%")
    run_command_on_server("sudo tc qdisc add dev ens5 root netem delay 5msec loss 0.25%")
    # Add ssh public key to allow the test client to suspend and resume server processes
    run_command_on_server(f"echo '{additional_pub_key}' >> ~/.ssh/authorized_keys")


def op_init_client():
    """Setup software environment on the client machine."""
    run_command_on_client("sudo apt update")
    run_command_on_client("sudo apt -y install openjdk-8-jre-headless")


def op_server():
    """Deploy server nodes."""
    generate_server_yml()
    send_file_to_server(server_program)
    send_file_to_server(servers_yml)
    send_file_to_server(start_py)
    run_command_on_server(f"python3 {work_dir}/start.py {work_dir}/{server_program} {work_dir}/{servers_yml} {n_nodes}")


def op_single():
    generate_local_server_yml()
    send_file_to_client(server_program)
    send_file_to_client(servers_yml_local)
    run_command_on_client(f"java -Xmx64m -Xms64m -jar {server_program} {servers_yml_local} 0")


def op_client():
    generate_servers_txt()
    send_file_to_client(servers_txt)
    send_file_to_client(client_jar)
    run_command_on_client(f"java -jar {client_jar} -servers-list={servers_txt} {additional_client_parameters}")


def op_fetch_server_log():
    download_path_from_server("logs")


def op_fetch_client_log():
    download_path_from_client(client_log)


op_map = {"init_server": op_init_server,
          "init_client": op_init_client,
          "server": op_server,
          "single": op_single,
          "client": op_client,
          "server_log": op_fetch_server_log,
          "client_log": op_fetch_client_log}

parser = ArgumentParser()
parser.add_argument('op', nargs='?', choices=[*op_map])
parser.add_argument('--key', type=str, required=True, metavar='ssh_key.pem')
parser.add_argument('--server_public_ip', type=str, required=True, metavar='12.34.56.78')
parser.add_argument('--server_private_ip', type=str, required=True, metavar='172.31.18.84')
parser.add_argument('--client_ip', type=str, required=False, metavar='12.34.56.78')

args = parser.parse_args()

# run the selected operation
op_map[args.op]()
