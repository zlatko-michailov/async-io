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

package org.michailov.async.io.demo.pipe;

import java.io.*;
import java.util.concurrent.*;
import org.michailov.async.io.*;

public final class Main {

    public static void main(String[] args) {
        // Parse the command line options.
        // Upon error, this call will return null.
        CommandLineOptions commandLineOptions = CommandLineOptions.fromArgs(args);
        if (commandLineOptions == null) {
            return;
        }
        
        CompletableFuture<Void> demo;
        if (commandLineOptions.streamProcessingLevel == StreamProcessingLevel.TEXT) {
            demo = demoTextAsync(commandLineOptions.inputStream, commandLineOptions.outputStream);
        }
        else {
            demo = demoBytesAsync(commandLineOptions.inputStream, commandLineOptions.outputStream);
        }
                                                        
        // In general, we can continue with other work.
        // In this simple demo, there is no other work.
        // That's why we have to wait for the processing to complete 
        // to make sure the process doesn't exit before processing has completed.
        try {
            demo.get();
        }
        catch (Throwable ex) {
            System.err.println(String.format("Unexpected exception:\n%1$s", ex.toString()));
        }
        
        System.exit(0);
    }
    
    private static CompletableFuture<Void> demoTextAsync(InputStream inputStream, OutputStream outputStream) {
        // The default async options should be good enough, but
        // if you want to tune up the processing, look up the XxxAsyncOptions classes,
        // and set the desired options explicitly. 
        TextStreamAsyncOptions asyncOptions = new TextStreamAsyncOptions();
        
        // Creating an async writer is not generally necessary.
        // It is needed for this concrete demo that pipes the content from
        // the input stream into the output stream.
        AsyncTextStreamWriter streamWriter = new AsyncTextStreamWriter(outputStream, asyncOptions);
        
        // Start the stream writer.
        CompletableFuture<Void> streamWriterLoop = streamWriter.startApplyLoopAsync();

        // Create an async reader - invoke the Consumer<StringRingBuffer> when one or more text lines are available.
        // See method processTextLines() for details on the processing.
        // Reading will not start until an async loop is explicitly started.
        AsyncTextStreamReader streamReader = new AsyncTextStreamReader(inputStream, ring -> processTextLines(ring, streamWriter), asyncOptions);

        // Start the async loop.
        CompletableFuture<Void> streamReaderLoop = streamReader.startApplyLoopAsync();
        
        // When the read loop is complete, set the output StringRingBUffer as EOF
        // which will trigger EOF down the write chain, and will ultimately complete the write loop.
        streamReaderLoop.whenCompleteAsync((v, ex) -> { streamWriter.getStringRingBuffer().setEOF(); });
        
        // While we have 2 futures, we know that the read loop will have to complete in order for the write loop to complete.
        // That's why we can safely consider the write future to be the only one we need to wait for.
        return streamWriterLoop;
    }
    
    private static void processTextLines(StringRingBuffer ringBuffer, AsyncTextStreamWriter streamWriter) {
        // There may be more than one item/line available.
        // So we should iterate.
        while (ringBuffer.getAvailableToRead() > 0) {
            // Read one line through the ring buffer.
            String line = ringBuffer.read();
            
            // Write the line through the stream writer's ring buffer.
            streamWriter.getStringRingBuffer().write(line);
        }
        
        // At this point there are no more lines available to read.
        // If the ring buffer has been flagged as EOF, then the input stream is over.
        if (ringBuffer.isEOF()) {
            // Flag the stream writer's ring buffer as EOF.
            streamWriter.getStringRingBuffer().setEOF();
        }
    }
    
    private static CompletableFuture<Void> demoBytesAsync(InputStream inputStream, OutputStream outputStream) {
        return null;
    }
    
    final static class CommandLineOptions {
        StreamProcessingLevel streamProcessingLevel;
        
        String command;
        
        InputStream inputStream;
        
        OutputStream outputStream;
        
        private CommandLineOptions() {
            streamProcessingLevel = StreamProcessingLevel.TEXT;
            outputStream = System.out;
        }
        
        static CommandLineOptions fromArgs(String[] args) {
            // There must be at least 1 argument - a command.
            if (args != null && args.length >= 1) {
                CommandLineOptions options = new CommandLineOptions();
                
                for (int i = 0; i < args.length - 1; i++) {
                    System.err.println(String.format("%1$d: '%2$s'.", i, args[i]));
                    
                    if (args[i].equals("-t")) {
                        System.err.println("\ttext");
                        options.streamProcessingLevel = StreamProcessingLevel.TEXT;
                    }
                    else if (args[i].equals("-b")) {
                        System.err.println("\tbytes");
                            options.streamProcessingLevel = StreamProcessingLevel.BYTE;
                    }
                    else {
                        System.err.println(String.format("Unexpected option '%1$s'.", args[i]));
                        
                        printUsage();
                        return null;
                    }
                }
                
                // Execute the command and get its stdout.
                options.command = args[args.length - 1];
                try {
                    Process process = Runtime.getRuntime().exec(options.command);
                    options.inputStream = EOFInputStream.fromProcess(process);
                }
                catch (Throwable ex) {
                    System.err.println(String.format("Failed to execute command '%1$s'.", options.command));
                    System.err.println(ex.toString());
                    
                    printUsage();
                    return null;
                }
                
                return options;
            }
            
            return null;
        }
        
        static void printUsage() {
            System.err.println("Usage:");
            System.err.println("\t[-t|-b] command:");
            System.err.println("\t-t (default) to process the stream as text.");
            System.err.println("\t-b to process the stream as bytes.");
            System.err.println("\tcommand to execute");
        }
    }
    
    enum StreamProcessingLevel {
        BYTE,
        TEXT
    }
    
}
