# Java Async I/O
## Async Ring Buffer Watcher


The async ring buffer watcher is a "terminator" kind of async agent.
It watches a ring buffer and invokes a callback when the watched ring buffer has items available to read.

The watcher itself doesn't consume any items from the ring buffer. 
That is the callback's job.

When the callback is invoked there may be more than 1 item available to read.


