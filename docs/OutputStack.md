# Java Async I/O
## Output Stack Async Agents


### Text-to-Bytes Transformation
In order for an app to write text lines, the following transformation must take place:

1. A line break is appended after each line, and lines are joined in a contiguous char stream. (AsyncLineJoiner)
2. Chars are encoded into a byte stream. (AsyncCharEncoder)
3. Bytes are written into the OutputStream. (AsyncByteStreamWriter)


### Async Line Joiner
Reads lines from an input StringRingBuffer, appends a line break char after each line, and joins the lines in a contiguous char stream in an output CharRingBuffer.


### Async Char Encoder
Encodes chars from an input CharRingBuffer and into an output ByteRingBuffer.


### Async Byte Stream Writer
Reads bytes from an input ByteRingBuffer and writes them into an output OutputStream.


### Async Text Stream Writer
This agent is a convenience wrapper that hides the above processing chain into a single async agent. 
It's also a good illustration of how the actual async agents are chained together.
