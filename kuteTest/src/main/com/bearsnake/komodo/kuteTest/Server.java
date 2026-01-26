/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kuteTest;

import com.bearsnake.komodo.kutelib.SocketChannelHandler;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.LinkedList;
import java.util.List;

public class Server
    implements Runnable {

    private final List<Application> _clients = new LinkedList<>();
    private final int _port;
    private final Thread _thread = new Thread(this);
    private boolean _terminate = false;

    public Server(final int port) {
        _port = port;
        _thread.start();
    }

    public void run() {
        IO.println("Server started");

        ServerSocketChannel channel;
        try {
            channel = ServerSocketChannel.open();
            channel.configureBlocking(false);
            channel.socket().bind(new InetSocketAddress(_port));
        } catch (IOException ex) {
            IO.println("Server failed to start");
            return;
        }

        while (!_terminate) {
            try {
                var socketChannel = channel.accept();
                if (socketChannel == null) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                        IO.println("Server interrupted");
                        break;
                    }
                } else {
                    var menuApp = new MenuApp(new SocketChannelHandler(socketChannel));
                    _clients.add(menuApp);
                }
            } catch (IOException ex) {
                IO.println("Server failed to accept connection");
                break;
            }
        }

        try {
            for (var client : _clients) {
                client.close();
            }
            _clients.clear();
            channel.close();
        } catch (IOException ex) {
            // do nothing
        }

        IO.println("Server ended");
    }

    public void terminate() {
        _thread.interrupt();
        _terminate = true;
    }
}
