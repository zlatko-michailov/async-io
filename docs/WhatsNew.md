# Java Async I/O
## What's New


### v0.4
* Enforces EOFInputStream on AsyncByteStreamReader and AsyncTextStreamReader.
That is done because all the platform InputStream implementations don't distinguish between 
"_waiting for more data_" and "_no more data is expected_". 

### v0.3
* Introduces EOFInputStream - a wrapper around InputStream that extends it with a developer-provided eof() property.
EOFInputStream includes properties for file streams as well as for process streams. 

### v0.2
* Introduces AsyncRingBufferWatcher - the async agent itself as well as integration into AsyncTextStreamReader.

