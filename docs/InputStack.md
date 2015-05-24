# Java Async I/O
## Input Stack Async Agents


### Bytes-to-Text Transformation
In order for an app to read text lines, the following transformation must take place:

1. Bytes are read from the InputStream. (AsyncByteStreamReader)
2. Chars are decoded from the byte stream. (AsyncCharDecoder)
3. Line break sequences are recognized from the char stream, and the char stream is split into lines. (AsyncLineSplitter)


### Async Byte Stream Reader
Reads bytes from an input InputStream and writes them into an output ByteRingBuffer.


### Async Char Decoder
Decodes chars from an input ByteRingBuffer and writes them into an output CharRingBuffer.


### Async Line Splitter
Splits a contiguous sequence of chars from an input CharRingBuffer into lines and writes them into an output StringRingBuffer.


### Async Text Stream Reader
This agent is a convenience wrapper that hides the above processing chain into a single async agent. 
It's also a good illustration of how the actual async agents are chained together.
