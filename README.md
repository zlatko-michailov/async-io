# Summary
## Async I/O
The main purpose of this project is to enable async interaction with the existing, sync, InputStream and OutputStream primitives.

All I/O operations are performed on the ForkJoinPool thread pool. 
A CompletableFuture is returned from each async operation, so that it can be continued from.

### Input
Input operations never block as long as the InputStream implementation correctly reports its available bytes. 

### Output
Output operations are not guaranteed to be non-blocking because OutputStream doesn't report its availability.

## WhenReady
This whole async stack is based on a generic facility, WhenReady, that can be used for other kinds of async execution.

WhenReady takes a *ready* predicate eventually along with an *action* function, a *result* value, and/or a *done* predicate.
When the *ready* predicate returns true, it may assign the result, it may execute the action once, or it may start executing 
the action in a loop until the *done* predicate returns true.


# Support
You are strongly encouraged to visit (http://zlatko.michailov.org/search/label/Java%20Async%20I%2FO)
where you'll find articles dedicated to the primitives from this package. 


# Build Sources
This project uses Gradle to automate the build and test process.

    gradle java
    
## Run Tests
    gradle test

## Prepare for Eclipse
    gradle eclipse

    
# Download Binaries
Relatively stable versions are published to the Maven repository.
Search for:

* Group = michailov
* Package = async-io


# Contribution
To help make this produc better, please file maningful bugs with clearly described expectations and reliable repro apps.


