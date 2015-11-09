# Java Async I/O
## EOFInputStream


### What Is EOFInputStream
An EOFInputStream is special InputStream - it extends the base InputStream with a new property:
``` Java
boolean eof()
```
This property is presumed to be "sticky", i.e. once the implementation returns true, it should only return true from then on.


### Why EOFInputStream Is Needed
The base InputStream has a defficiency in communicating to the caller whether it should wait for bytes to become available or whether it should read on.
InputStream exposes a single property for that purpose:
``` Java
int available()
```
When this property returns 0, it is not clear whether the provider is fetching data in which case a client that doesn't want to block should wait, or 
whether EOF has been reached and the client should should complete the reading loop.
Attempting to read when the provider is fetching data would defeat the whole purpose of an async API.

__Note__: It is possible to provide the needed information even with a single property - the provider can "incorrectly" report that 1 more byte is available
when EOF has been reached, and when the client comes to read that extra byte, the provider returns -1 (from the read() call) to signal EOF. 
Unfortunately, none of the built in providers implement this.


### How to Use the EOFInputStream
The EOFInputStream class allows the app to use additional knowledge to tell the Async I/O package when EOF has been reached.

The class provides built-in factory methods for creating EOFInputStream instances from processes as well as from files.
A process stream is flagged as EOF when the process disappears.
And a file stream is flagged as EOF when as many bytes as the length of the file have been read.

Apps can implement their own custom logic.