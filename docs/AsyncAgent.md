# Java Async I/O
## Async Agent


In general, an async agent is a class that consumes the "When Ready" pattern.

This package provides an abstract AsyncAgent class that wraps around the WhenReady API.
It proved very useful for the implementation of the rest of the package classes.

If you want to implement your own async agent, derive from this class.
You'll have to override the following methods: 

* ready() - this is the *ready* predicate.
* action() - if you plan to call applyAsync().
* done() - this is the *done* predicate if you plan to call startApplyLoopAsync().

All method names correspond to WhenReady names.
For details, see [The "When Ready" Pattern](WhenReady.md).
