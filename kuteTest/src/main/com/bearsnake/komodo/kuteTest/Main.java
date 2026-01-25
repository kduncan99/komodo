/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kuteTest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;

public class Main {

    public static final int port = 2200;

    private boolean terminate = false;

    public void serverLoop() throws IOException {
        var serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.socket().bind(new InetSocketAddress(port));

        while (!terminate) {
            var socketChannel = serverChannel.accept();
            if (socketChannel != null) {
                var clock = new ClockApp(new Session(socketChannel));
            }
        }
    }

    public void terminate() {

    }

    static void main() throws IOException {
//    IO.println(String.format("Hello and welcome!"));
        var main = new Main();
        main.serverLoop();
    }
}
