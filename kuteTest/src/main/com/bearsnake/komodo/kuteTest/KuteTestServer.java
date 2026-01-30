/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kuteTest;

import com.bearsnake.komodo.kutelib.network.SocketChannelHandler;
import com.bearsnake.komodo.kutelib.network.SocketChannelListener;
import com.bearsnake.komodo.kutelib.network.UTSByteBuffer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.LinkedList;

public class KuteTestServer implements SocketChannelListener {

    private static class SessionInfo {

        private final SocketChannelHandler _handler;
        private final LinkedList<Application> _applications = new LinkedList<>();

        public SessionInfo(final SocketChannelHandler handler) {
            _handler = handler;
        }
    }

    private final int _port;
    private boolean _terminate = false;
    private final LinkedList<SessionInfo> _sessions = new LinkedList<>();

    public KuteTestServer(final int port) {
        _port = port;
    }

    protected void sendMessage(final Application application,
                               final UTSByteBuffer buffer) throws IOException {
        synchronized (_sessions) {
            for (var session : _sessions) {
                if (session._applications.contains(application)) {
                    session._handler.send(buffer);
                    return;
                }
            }
        }
        IO.println("Internal error - cannot find application for sendMessage()");
    }

    /**
     * Receives input from the users via the various SocketChannelHandlers.
     * We find the session that the SocketChannelHandler belongs to and send the message
     * to the most recent application for that session.
     * @param source SocketChannelHandler that sent the data
     * @param data data to be processed by the listener
     */
    @Override
    public void trafficReceived(final SocketChannelHandler source,
                                final UTSByteBuffer data) {
        synchronized (_sessions) {
            for (var session : _sessions) {
                if (session._handler == source) {
                    session._applications.getLast().handleInput(data);
                    return;
                }
            }
        }
        IO.println("Internal error - cannot find application for trafficReceived()");
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
                var didSomething = false;
                var socketChannel = channel.accept();
                if (socketChannel != null) {
                    synchronized (_sessions) {
                        var handler = new SocketChannelHandler(socketChannel, this);
                        var session = new SessionInfo(handler);
                        var app = new MenuApp(this);
                        session._applications.addLast(app);
                        _sessions.addLast(session);
                        app.start();
                        didSomething = true;
                    }
                } else {
                    // TODO check for dead applications
                    synchronized (_sessions) {
                        var iter = _sessions.iterator();
                        while (iter.hasNext()) {
                            var session = iter.next();
                            var app = session._applications.getLast();
                            if (app.isTerminated()) {
                                IO.println("Application " + app.getClass()
                                                               .getSimpleName() + " terminated");
                                session._applications.removeLast();
                                didSomething = true;
                            }
                            if (session._applications.isEmpty()) {
                                IO.println("No applications left for session " + session._handler);
                                session._handler.close();
                                iter.remove();
                                didSomething = true;
                            }
                        }
                    }
                }

                if (!didSomething) {
                    Thread.sleep(100);
                }
            } catch (InterruptedException ex) {
                IO.println("Server interrupted");
            } catch (IOException ex) {
                IO.println("Server failed to accept connection");
                break;
            }
        }
    }
}
