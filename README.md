# Distributed Key-Value Store (Java)

Java implementation of a distributed, decentralized, and multithreaded in-memory key-value store 
based on consistent hashing. Done by Group G10 for the course 
[CPEN 431: Design of Distributed Software Applications](https://ece.ubc.ca/courses/cpen-431/) (2021WT2). 
A C++ implementation of the same design is available [here](https://github.com/LuuO/distributed-kv-store-cpp).

## Goals
- Sequential Consistency: support sequential consistency in all cases, including failures
- At-Most-Once Semantic: execute the request once or not-at-all
- Fault Tolerance: continue to operate correctly despite failures
- Incremental Scalability: the capacity and performance increase linearly with the number of nodes
- Performance: high throughput and low latency under high load
- Availability: high request success rate

## Design Choices
### Replication
Our implementation uses the non-blocking primary-backup protocol and replicates keys on three successors. 
Although this protocol has some weaknesses in durability, it guarantees sequential consistency 
and has low latency. The protocol works as follows:
1. A server node receives a request with key *k* from the client and forwards it to its primary node *P*
2. The primary node *P* reads or updates its local storage
3. If the type of the request is *PUT* or *REMOVE*, the primary node *P* also forwards the request to the 
backup nodes (the three successors) via TCP, and does not wait for any acknowledgement.
4. The primary node *P* sends a response to the client

### Failure Detection
We used an epidemic protocol inspired by the last question in Midterm 1 to detect node failures in a decentralized manner.

Each node maintains a vector of `N` timestamps, where `N` is the total number of nodes. 
Let `Ta` denote the vector at Node `a`. The timestamp at `Ta[b]` represents the latest time at which Node `a` knows
Node `b` was alive. In other words, `Ta[b] = t` means Node `a` knows that Node `b` was alive at time `t`. 
If the difference between `Ta[b]` and the current time is greater than a predefined value (e.g. 8 seconds), Node `a`
considers Node `b` has failed. 
In addition, `Ta[a]` is updated to the current time every time before `Ta[a]` is accessed.

Each node in our implementation also pushes its timestamp vector to two other random nodes, one from the list of live 
nodes, and one from the list of failed nodes. Doing this can efficiently disseminate the status of each node.
The reason why we choose to send timestamps to one alive node and one failed node rather than any random node 
is that only alive nodes can propagate timestamps. When a significant portion of nodes has failed, it is important to
ensure alive nodes get enough messages in order to maintain the speed of status propagation.

Upon receiving a timestamp vector, the receiving node merges the incoming timestamp vector into its local timestamp 
vector such that `T_new[x] = max(T_old[x], T_remote[x])` for all `x`.

### Replication Recovery
Each node in our system keeps track of 1 preceding node and 3 succeeding nodes, 
and when any of them changes, the node sends out keys through TCP to repair the replicas.

## Architecture (TODO)


## Build, Run, and Test
### Build from source
To create a jar file, run the following command in the project root directory:
```shell
./gradlew build
```
The output jar file (with dependencies) will be automatically copied to the root directory.

### Configuration Files
For a node to communicate with other nodes, it needs to know their addresses.
Thus, the server program requires a YAML file (usually named `servers.yml`) for group membership information.

A sample of the membership file:
```yaml
serverInfo:
  - ip: 192.168.0.1
    serverPort: 20000
    udpPort: 20001
    tcpPort: 20002
  - ip: 192.168.0.2
    serverPort: 20003
    udpPort: 20004
    tcpPort: 20005
```

### Run on Local Machine
#### One Node
To start a server node on the local machine, run
```shell
java -Xmx512m -Xms512m -jar path/to/jar path/to/servers.yml N
```
where N is the index of this server node in `servers.yml`.

#### Multiple Nodes
To start multiple server nodes on the local machine, run
```shell
python3 scripts/start.py path/to/jar path/to/servers.yml number_of_nodes
```

### Test on Remote Machines
To test our implementation, we launch multiple nodes on the same remote machine, 
and use a network emulator, NetEm, to simulate network latency and packet loss.
On a separate machine, we run the test program provided by the teaching team.
The test program will generate many clients to test various aspects of the key-value store,
including performance, fault tolerance, and sequential consistency. 
It will use SSH to log in to the server machine and simulate hardware failures by suspending/resuming processes.

We wrote a script to facilitate the above procedure. 
To use the script, run the following command in the project root directory:

```shell
python3 scripts/deploy.py \
  --key path/to/ssh_key.pem \
  --server_public_ip 35.84.193.206 \
  --server_private_ip 172.31.18.84 \
  --client_ip 25.45.193.111 \
  OPERATION
```

`server_private_ip` is the private address of the server machine. 
It will be used for the communication between nodes.

*OPERATION*:
- `init_server`: Set up the environment on the server machine.
- `init_client`: Set up the environment on the client machine.
- `server`: Run server nodes on the server machine. 
- `client`: Run the test client on the client machine.
- `single`: Run one server node (unrelated to nodes on the server machine) on the client machine. 
- `server_log`: Download the server logs.
- `client_log`: Download the client log.

Note: `server`, `client`, and `single` will automatically generate and upload relevant files.

## Logs
Server logs will be saved to `logs/server-N.log` where `N` is the index of the server.

## Acknowledgements
### Code
- Files in the package `com.matei.eece411.util` are provided by the instructor.
- `Process.getCurrentProcessId()` is adapted from https://stackoverflow.com/a/43399977.
- `ByteUtil.bytesToHexString(Iterable<Byte>)` is adapted from `StringUtils.byteArrayToHexString(byte[])`, which is provided by the instructor.
- `com.g10.cpen431.a12.membership.Hash.hash` is adapted from https://www.cnblogs.com/hd-zg/p/5917758.html.

### Library
Our implementation uses these external libraries:
- [Protobuf Plugin for Gradle](https://github.com/google/protobuf-gradle-plugin)
- [Protocol Buffers](https://github.com/protocolbuffers/protobuf)
- [Project Lombok](https://github.com/projectlombok/lombok)
- [Guava](https://github.com/google/guava)
- [Apache Log4j 2](https://logging.apache.org/log4j/2.x/)
- [Jackson Dataformat YAML](https://mvnrepository.com/artifact/com.fasterxml.jackson.dataformat/jackson-dataformat-yaml)
