# Java Async I/O
## Async I/O Principles


### Agent Classes
Each of these agents is implemented by a class with a corresponding name.
Look up those classes in the [API Reference](http://zlatko-michailov.github.io/async-io/docs/javadoc/).


### Active Agents
These agents are *active* - once started, they continuously look for work to do until they are signaled as done.
At this point, an agent cannot be restarted, i.e. once done, an agent cannot go back to work.
In future, a "pause" functionality may be added if a compelling use case is identified.


### Processing Extensibility
Agents transform input from an InputStream or from a ring buffer to another ring buffer or to an OutputStream.
That keeps each agent cohesive, and allows processing to be extended.


### Binary Streams
Streams are always binary, not text, i.e. streams are sequences of bytes, not sequences of chars.
Sometimes high-level app developers perfer not to see the complexity of the transformation of text to bytes and vice versa.
This article is for developers who want to understand how things actually work.

