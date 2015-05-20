# Summary
## Java Async I/O
The main purpose of this project is to enable async interaction with the existing, sync, InputStream and OutputStream primitives in Java.

All I/O operations are performed on the ForkJoinPool thread pool. 
A CompletableFuture is returned from each async operation, so that it can be continued from.

### Input
Input operations never block as long as the InputStream implementation correctly reports its available bytes. 

### Output
Output operations are not guaranteed to be non-blocking because OutputStream doesn't report its availability.

## WhenReady
This async stack is based on a generic facility, WhenReady, that can be used for other kinds of async execution.

WhenReady takes a *ready* predicate eventually along with an *action* function, a *result* value, and/or a *done* predicate.
When the *ready* predicate returns true, WhenReady may assign the result, it may execute the action once, or it may start executing 
the action in a loop until the *done* predicate returns true.


# Support
Visit (http://zlatko.michailov.org/search/label/Java%20Async%20I%2FO) where you'll find more information about this API. 


# Building the Sources
This project uses Gradle to automate the build and test process.

    gradle java
    
## Running the Tests
    gradle test

## Preparing for Eclipse
    gradle eclipse

    
# Downloading Binaries
Released versions *will* be published to the Maven Central Repository.
Search for:

* Group = michailov
* Package = async-io


