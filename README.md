# CPEN431 2022 A7

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
java -Xmx64m -Xms64m -jar A7.jar servers.yml N
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

OPERATION:
- `servers`: Run multiple servers on `server_ip`
- `single`: Run a single server on `client_ip:43100`
- `client`: Run the test client on `client_ip`, the client jar should be in the project root directory
- `fetch_logs`: Download logs from `server_ip`

```shell
# In project root directory
python3 scripts/deploy.py --key aws.pem --server_ip 35.84.193.206 --client_ip 25.45.193.111 OPERATION
```

### Logs
Server logs should be saved to `logs/server-N.log` where `N` is the index of the server.

### Design Choices
- Make extensive use of `ByteBuffer` and `ByteString` to avoid copying byte arrays as much as possible.
- Clear separation between layers with interface `RequestReplyApplication` and class `NotificationCenter`.
- Avoid caching replies for idempotent request without breaking layering.

### Additional Tests
There are some unit tests in `com.g10.cpen431.ByteUtilTest` for utility functions.

### Source
- Files in the package `com.matei.eece411.util` and the folder `src/main/proto` are provided by the course instructor.
- `Process.getCurrentProcessId()` is adapted from https://stackoverflow.com/a/43399977.
- `ByteUtil.bytesToHexString(Iterable<Byte>)` is adapted from `StringUtils.byteArrayToHexString(byte[])`.
- `src/main/resources/log4j2.xml` is modified from https://logging.apache.org/log4j/2.x/manual/configuration.html#AutomaticReconfiguration.
