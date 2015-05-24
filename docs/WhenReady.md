# Java Async I/O
## The "When Ready" Pattern


### Disclaimer
I'm not into pattern classification. 
I don't claim to have invented yet another pattern.
A pattern for this may very well exist already.
Hopefully I'm not infringing somebody's "invention".

I needed a name for this behavior, so I could name my class and its methods.
The phrase "when ready, do something" is very comprehensible yet it accurately represents the behavior. 
Hence the "When Ready" pattern.


### The Pattern
#### The "ready" Predicate
The most important element of the "When Ready" pattern is a "ready" predicate.
This predicate is assumed to be *sticky*, i.e. once it returns true, it continues returning true until some explicit action is taken.

#### Complete a Future
This is the most basic of all applications.

    public static CompletableFuture<R> WhenReady.completeAsync(Predicate<S> ready, R result, S state)

The mechanics behind this method are very simple - once the *ready* predicate returns true, the returned future gets completed with the given *result*.

The practical application of this method is to trigger a chain of async continuations. 

#### Apply a Function
    public static CompletableFuture<R> WhenReady.completeAsync(Predicate<S> ready, Function<S, R> action, S state)

When the *ready* predicate starts returning true, the *action* gets called, and the returned CompletableFuture gets completed with the result of the *action*.

#### Continuously Apply a Function Until Done
    public static CompletableFuture<R> WhenReady.startApplyLoopAsync(Predicate<S> ready, Predicate<S> done, Function<S, R> action, S state)

The *action* gets called whenever the *ready* predicate returns true. 
The loop ends when the *done* predicate returns true.
The returned CompletableFuture gets completed with the result of the last iteration.
