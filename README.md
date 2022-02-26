# CPEN431 2022 A7

---
**Group ID:** G10 <br />
**Verification Code:** 3655741786 <br />


### How to Build

```shell
./gradlew build
```

### How to Run
The server runs on Port #16589.

```shell
java "-Dsun.stdout.encoding=UTF-8" "-Dsun.err.encoding=UTF-8" -Xmx64m -Xms64m -jar A7.jar serverList.yml 1

serverList.yml is the filename of your .yml file, 1 refers to your own ip address listed in serverList.yml going downwards
```
or (no need to build)
```shell
./gradlew run
```

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
