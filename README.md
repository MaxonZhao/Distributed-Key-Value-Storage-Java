# CPEN431 2022 A12

---
**Group ID:** G10 <br />
**Verification Code:** 3655741786 <br />


## How to Build
```shell
./gradlew build
```

## How to Run
1. Build
2. The program requires a YAML file for initial group membership information
3. Run command:
#### Run One Instance
```shell
# servers.yml is your YAML file, N is the index of this server instance in servers.yml
java -Xmx64m -Xms64m -jar A12.jar servers.yml N
```

### Run Multiple Instance in Batch
```shell
# the YAML filename must be servers.yml, first_index & last_index indicate the range of instances in servers.yml
python3 scripts/start.py first_index last_index
```

### Deploy and Test
The script should work on newly created AWS EC2 instances.
`servers.yml` and `servers.txt` will be automatically generated.
Required files are automatically copied to the server or client machine.

*OPERATION*:
- `init`: Initialize the server machine and client machine
- `servers`: Run multiple servers on `server_ip`
- `single`: Run a single server on `client_ip:43100`
- `client`: Run the test client on `client_ip`, the client jar should be in the project root directory
- `server_log`: Download server logs from `server_ip`
- `client_log`: Download the client log from `client_ip`

```shell
# In project root directory
python3 scripts/deploy.py --key aws.pem --server_public_ip 35.84.193.206 --server_private_ip 172.31.18.84 --client_ip 25.45.193.111 OPERATION
```
`--server_private_ip` is the private address of the server machine. Using this address is much more efficient than the 
public address for the communication between nodes.


### Logs
Server logs are saved to `logs/server-N.log` where `N` is the index of the server.

### Design Choices (TODO)
- Make extensive use of `ByteBuffer` and `ByteString` to avoid copying byte arrays as much as possible.
- Clear separation between layers with interface `RequestReplyApplication` and class `NotificationCenter`.
- Avoid caching replies for idempotent request without breaking layering.

### Additional Tests
There are some unit tests in `src/test/java`.

### Source
- Files in the package `com.matei.eece411.util` are provided by the course instructor.
- `Process.getCurrentProcessId()` is adapted from https://stackoverflow.com/a/43399977.
- `ByteUtil.bytesToHexString(Iterable<Byte>)` is adapted from `StringUtils.byteArrayToHexString(byte[])`.
- `com.g10.cpen431.a12.membership.Hash.hash` is based on https://www.cnblogs.com/hd-zg/p/5917758.html.
