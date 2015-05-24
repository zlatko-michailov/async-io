# Java Async I/O
## Ring Buffer


The ring buffer abstraction maximizes buffered throughput by enabling a writer and a reader to "chase" each other around a physical buffer concurrently.
That concept is key in stream processing. 
Think of a watermill wheel - it makes no frictional or flipping motion, it smoothly spins in a  single direction. 

### abstract class RingBuffer
This abstract class implements the chasing of a reader and a writer around a plain array without any element access.

Unfortunately, Java doesn't support generics of primitive types.
That's why there are concrete classes ByteRingBuffer and CharRingBuffer.
A generic RingBuffer<Byte> or RingBuffer<Char> would create too many unnecessary heap allocations and double references that might defeat the benefit of the ring buffer functionality.
