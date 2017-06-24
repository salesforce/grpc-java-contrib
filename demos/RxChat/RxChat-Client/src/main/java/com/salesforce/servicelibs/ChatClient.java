/*
 *  Copyright (c) 2017, salesforce.com, inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see LICENSE.txt file in the repo root  or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.servicelibs;

import com.google.protobuf.Empty;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import jline.TerminalFactory;
import jline.console.ConsoleReader;
import jline.console.CursorBuffer;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.TimeUnit;

/**
 * Demonstrates building a gRPC streaming client using RxJava and RxGrpc.
 */
public final class ChatClient {
    private static final int PORT = 9999;

    private ChatClient() { }

    public static void main(String[] args) throws Exception {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", PORT).usePlaintext(true).build();
        RxChatGrpc.RxChatStub stub = RxChatGrpc.newRxStub(channel);

        ConsoleReader console = new ConsoleReader();

        console.println("Press ctrl+D to quit");
        String author = console.readLine("Who are you? > ");
        stub.postMessage(Single.just(ChatProto.ChatMessage.newBuilder().setAuthor(author).setMessage(author + " joined.").build())).subscribe();

        // Subscribe to incoming messages
        Disposable chatSubscription = stub.getMessages(Single.just(Empty.getDefaultInstance())).subscribe(
            message -> {
                CursorBuffer stashed = stashLine(console);
                console.println(">> " + message.getAuthor() + ": " + message.getMessage());
                console.redrawLine();
                unstashLine(console, stashed);
                console.flush();
            }
        );

        String line;
        while ((line = console.readLine("chat> ")) != null) {
            stub.postMessage(Single.just(ChatProto.ChatMessage.newBuilder().setAuthor(author).setMessage(line).build())).subscribe(
                empty -> { },
                throwable -> {
                    console.println(":: FROM SERVER ::");
                    throwable.printStackTrace(new PrintWriter(console.getOutput()));
                    console.flush();
                }
            );
        }

        stub.postMessage(Single.just(ChatProto.ChatMessage.newBuilder().setAuthor(author).setMessage(author + " left.").build())).subscribe();
        chatSubscription.dispose();
        channel.shutdown();
        channel.awaitTermination(1, TimeUnit.SECONDS);
        TerminalFactory.get().restore();

    }

    private static CursorBuffer stashLine(ConsoleReader console) {
        CursorBuffer stashed = console.getCursorBuffer().copy();
        try {
            console.getOutput().write("\u001b[1G\u001b[K");
            console.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stashed;
    }


    private static void unstashLine(ConsoleReader console, CursorBuffer stashed) {
        try {
            console.resetPromptLine(console.getPrompt(), stashed.toString(), stashed.cursor);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
