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


# Documentation
You'll find articles in the **[docs](docs)** folder in this source tree.

Or, you may jump straight to the **[API Reference](http://zlatko-michailov.github.io/async-io/docs/javadoc/)**


# Support
If you don't find an answer for your question, please send an email to [Zlatko Michailov](mailto:zlatko+asyncio@michailov.org).
 

# Building the Sources
This project uses Gradle to automate the build and test process.

    gradle java
    
## Running the Tests
    gradle test

## Preparing for Eclipse
    gradle eclipse

    
# Referencing the Package   
Include these sectons in your build.gradle script:
    
    repositories {
        maven {
            url "http://zlatko-michailov.github.io/async-io/repo/maven"
        }
    }

    dependencies {
        compile 'michailov:async-io:0.4'
    }
    

