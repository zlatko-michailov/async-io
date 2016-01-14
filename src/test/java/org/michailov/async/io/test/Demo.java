/**
 * Copyright (c) Zlatko Michailov
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.michailov.async.io.test;

import java.io.*;
import java.util.concurrent.*;
import org.junit.*;
import org.michailov.async.io.*;

public class Demo {

    @Test
    public void demoReadProcess() {
        // Get an OS-appropriate command.
        String command;
        String osName = System.getProperty("os.name").toLowerCase();
        if (osName.indexOf("windows") >= 0) {
            command = "cmd /c 'dir'";
        }
        else {
            command = "bash 'ls -l'";
        }
        
        // Obtain an EOFInputStream from the process.
        EOFInputStream inputStream;
        try {
            Process process = Runtime.getRuntime().exec(command);
            inputStream = EOFInputStream.fromProcess(process);
        }
        catch (Throwable ex) {
            Assert.fail(String.format("Failed to execute command '%1$s'.", command));
            Assert.fail(ex.toString());
            return;
        }
        
        // Read the EOFInputStream and write it to StdOut.
        OutputStream outputStream = System.out;
        CompletableFuture<Void> readFuture = readInputStreamAsync(inputStream, outputStream);
        
        // In general, we can continue with other work.
        // In this simple demo, there is no other work.
        // That's why we have to wait for the processing to complete 
        // to make sure the process doesn't exit before processing has completed.
        try {
            readFuture.get();
        }
        catch (Throwable ex) {
            Assert.fail("Failed to complete the async read.");
            Assert.fail(ex.toString());
            return;
        }
    }
    
    @Test
    public void demoReadFile() {
        // Reference a file from this codebase relatively,
        // so that it works on all operating systems.
        String path = "build.gradle";
        
        // Obtain an EOFInputStream from the process.
        EOFInputStream inputStream;
        try {
            File file = new File(path);
            inputStream = EOFInputStream.fromFile(file);
        }
        catch (Throwable ex) {
            Assert.fail(String.format("Failed to open file '%1$s'.", path));
            Assert.fail(ex.toString());
            return;
        }
        
        // Read the EOFInputStream and write it to StdOut.
        OutputStream outputStream = System.out;
        CompletableFuture<Void> readFuture = readInputStreamAsync(inputStream, outputStream);
        
        // In general, we can continue with other work.
        // In this simple demo, there is no other work.
        // That's why we have to wait for the processing to complete 
        // to make sure the process doesn't exit before processing has completed.
        try {
            readFuture.get();
        }
        catch (Throwable ex) {
            Assert.fail("Failed to complete the async read.");
            Assert.fail(ex.toString());
            return;
        }
    }
    
    private static CompletableFuture<Void> readInputStreamAsync(EOFInputStream inputStream, OutputStream outputStream) {
        // The default async options should be good enough, but
        // if you want to tune up the processing, look up the XxxAsyncOptions classes,
        // and set the desired options explicitly. 
        TextStreamAsyncOptions asyncOptions = new TextStreamAsyncOptions();
        
        // Creating an async writer is not generally necessary.
        // It is needed for this concrete demo that pipes the content from
        // the input stream into the output stream.
        AsyncTextStreamWriter streamWriter = new AsyncTextStreamWriter(outputStream, asyncOptions);
        
        // Start the stream writer.
        CompletableFuture<Void> writeLoop = streamWriter.startApplyLoopAsync();

        // Create an async reader - invoke the Consumer<StringRingBuffer> when one or more text lines are available.
        // See method processTextLines() for details on the processing.
        // Reading will not start until an async loop is explicitly started.
        AsyncTextStreamReader streamReader = new AsyncTextStreamReader(inputStream, ring -> processTextLines(ring, streamWriter), asyncOptions);

        // Start the async loop.
        CompletableFuture<Void> readLoop = streamReader.startApplyLoopAsync();
        
        // When the read loop is complete, flag the output StringRingBuffer as EOF
        // which will trigger EOF down the write chain, and which will ultimately complete the write loop.
        readLoop.whenCompleteAsync((v, ex) -> { streamWriter.getStringRingBuffer().setEOF(); });
        
        // While we have 2 futures, we know that the read loop will have to complete in order for the write loop to complete.
        // That's why we can safely consider the write loop future to be the only one we need to wait for.
        return writeLoop;
    }
    
    private static void processTextLines(StringRingBuffer ringBuffer, AsyncTextStreamWriter streamWriter) {
        // There may be more than one item/line available.
        // That's why we iterate.
        while (ringBuffer.getAvailableToRead() > 0) {
            // Read one line through the ring buffer.
            String line = ringBuffer.read();
            
            // Write the line through the stream writer's ring buffer.
            streamWriter.getStringRingBuffer().write(line);
        }
    }

}
