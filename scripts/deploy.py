import os
import argparse

# Usage: In the project root directory

# update the following variables for each assignment
jar = "A8.jar" # jar file name
client_jar = "a8_2022_eval_tests_v1.jar" # must be in the current dir
n_nodes = 40 # number of nodes

# do not edit the following variables
username = "ubuntu"
work_dir = f"/home/{username}"
servers_yml = "servers.yml"
servers_yml_local = "servers_local.yml"
servers_txt = "servers.txt"
start_py = "scripts/start.py" # script for starting multiple servers
start_port = 20000
epidemic_port = 40000
single_port = 43100
single_epidemic_port = 43101
additional_pub_key = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCfxrmM7cpw0hRWnshggt/2XpQ/nVi/Olotia6pxK7Aacg1wt4yDdS8eZ3Lto4JsRiI+hUZZ2uyuZSy3zgtWai+mGu3TshKP1bz+wC7RHenZwgEXuPn2WuIofTTjFfGIq6uOPWc5D6v0We2bVlrXFbYvHP4J24QkH9vsAfkpL5UGa+l+PT+L3phgVeTPs/BxuhaBp6J+j9g+UVF86di7u01fTuevjKRojzJe9kBRFQ4t95XsL0QDXOIB2GD7PSblxaftVINmiPpeyJPGMyZWqDuXyhhF37rRgaq9gHnnuNo4SVrR+8lJbd14c+bgHCM/FSZI43vkynnBPX6XFSJN7qv cpen431_pop"


def send_file_to_server(file):
    os.system(f"scp -i {args.key} {file} {username}@{args.server_ip}:{work_dir}/")

def send_file_to_client(file):
    os.system(f"scp -i {args.key} {file} {username}@{args.client_ip}:{work_dir}/")

def download_folder_from_server(folder):
    os.system(f"scp -i {args.key} -r {username}@{args.server_ip}:{work_dir}/{folder} ./")

def run_command_on_client(command):
    os.system(f"ssh -i {args.key} {username}@{args.client_ip} \"{command}\"")

def run_command_on_server(command):
    os.system(f"ssh -i {args.key} {username}@{args.server_ip} \"{command}\"")

def op_fetch_logs():
    download_folder_from_server("logs")

# Generate servers_local.yml
def op_yml():
    with open(servers_yml, 'w') as yml_file:
        yml_file.write("epidemicProtocolInfo:\n")
        port = epidemic_port
        for _ in range(n_nodes):
            yml_file.write(f"  - IP: {args.server_ip}\n")
            yml_file.write(f"    port: {port}\n")
            port += 1
        yml_file.write("serverInfo:\n")
        port = start_port
        for _ in range(n_nodes):
            yml_file.write(f"  - IP: {args.server_ip}\n")
            yml_file.write(f"    port: {port}\n")
            port += 1

# Generate servers_local.yml, which contains only 127.0.0.1:43100
def op_yml_local():
    with open(servers_yml_local, 'w') as yml_file:
        yml_file.write("epidemicProtocolInfo:\n")
        yml_file.write(f"  - IP: 127.0.0.1\n")
        yml_file.write(f"    port: {single_epidemic_port}\n")
        yml_file.write("serverInfo:\n")
        yml_file.write(f"  - IP: 127.0.0.1\n")
        yml_file.write(f"    port: {single_port}\n")

# Generate servers.txt for the client
def op_txt():
    with open(servers_txt, 'w') as txt_file:
        port = start_port
        for _ in range(n_nodes):
            txt_file.write(f"{args.server_ip}:{port}\n")
            port += 1

def op_init():
    run_command_on_server("sudo apt update; sudo apt -y install openjdk-8-jre-headless net-tools;")
    run_command_on_server("ip a")
    run_command_on_server("sudo tc qdisc add dev lo   root netem delay 5msec loss 2.5%")
    run_command_on_server("sudo tc qdisc add dev ens5 root netem delay 5msec loss 2.5%")
    run_command_on_server(f"echo '{additional_pub_key}' >> ~/.ssh/authorized_keys")

    run_command_on_client("sudo apt update; sudo apt -y install openjdk-8-jre-headless;")

def op_shutdown():
    run_command_on_server("pkill java")
    run_command_on_client("pkill java")

def op_servers():
    op_yml()
    send_file_to_server(jar)
    send_file_to_server(servers_yml)
    send_file_to_server(start_py)
    run_command_on_server(f"python3 {work_dir}/start.py 0 {n_nodes - 1}")

def op_single():
    op_yml_local()
    send_file_to_client(jar)
    send_file_to_client(servers_yml_local)
    run_command_on_client(f"java -Xmx64m -Xms64m -jar {jar} {servers_yml_local} 0")

def op_client():
    op_txt()
    send_file_to_client(servers_txt)
    send_file_to_client(client_jar)
    run_command_on_client(f"java -jar {client_jar} -servers-list={servers_txt} -submit -secret=3655741786")

op_map = {"init": op_init, "shutdown": op_shutdown, "servers": op_servers, "single": op_single, "client": op_client, "fetch_logs": op_fetch_logs}

parser = argparse.ArgumentParser()
parser.add_argument('op', nargs='?', choices=[*op_map])
parser.add_argument('--key', type=str, required=True, metavar='ssh_key.pem')
parser.add_argument('--server_ip', type=str, required=True, metavar='12.34.56.78')
parser.add_argument('--client_ip', type=str, required=True, metavar='12.34.56.78')
args = parser.parse_args()

op_map[args.op]()
