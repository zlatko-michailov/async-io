# Java Async I/O
## Overview


### About Me
I work for Microsoft.
Anything I learn I may use to benefit Microsoft.
You've been warned.

Those who've interacted with me, know me as a C#, C++, or SQL developer, but probably not as a Java developer.
That's reasonable because I haven't done any Java development in the last 10 years.

In a previous life, however, I was developing in Java.


### Why Java?
I recently decided to implement an idea in Java to see how the Java platform has evolved.
To describe nicely what I discovered, Java has created plenty of opportunities for improvement.

The Java I/O stack is one such "opportunity". It is too essential to the platform to be left in this state. 


### Why Not NIO?
The Java Non-blocking I/O (NIO) is exactly what it says - a "non-blocking API".
I can trust NIO's statement that it will return immediately instead of blocking until there is something to do.
However, that's not good enough, because the app that consumes it has to either spin or go to sleep or most likely do a combination of the two.

The end user doesn't, nor should they, care why the app is unresponsive or why it consumes too much CPU or why it has too big of a latency.
That's why apps need APIs that avoid blocking across the whole stack, not just within the API layer. 


### What Is Async I/O?
The Java I/O problem can't be solved on the public surface. 
A real solution would require an async API to be exposed natively from each class. 
I guess that's what NIO was supposed to be, but the API wasn't designed properly.

What this package provides is primitives for async interaction with the existing InputStream and OutputStream.
At it's very core, it implements a spin-and-sleep pattern. See [The "When Ready" Pattern](WhenReady.md).
This implementation schedules futures which should result in a marginally better CPU utilization and responsiveness.

You'll also notice that every async agent type from this package uses ring buffers for its input and output.
That is another thing that NIO didn't get right. 
NIO buffers require serialization between write and read:

1. A writer writes. 
2. The buffer is explicitly "flipped". 
3. A reader reads.

Async I/O ring buffers are virtually contiguous and concurrent - a writer can keep writing while a reader can keep reading at the same time.
Simple and efficient.
