/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kuteTest;

import com.bearsnake.komodo.kutelib.messages.UTSMessage;
import com.bearsnake.komodo.kutelib.messages.TextMessage;
import com.bearsnake.komodo.kutelib.uts.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.LinkedList;

public class KuteTestServer implements UTSSocketListener {

    private static class SessionInfo {

        private final UTSSocketHandler _handler;
        private final LinkedList<Application> _applications = new LinkedList<>();

        public SessionInfo(final UTSSocketHandler handler) {
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
        buffer.setPointer(0);
        synchronized (_sessions) {
            for (var session : _sessions) {
                if (session._applications.contains(application)) {
                    session._handler.write(new TextMessage(buffer.getBuffer()));
                    return;
                }
            }
        }
        IO.println("Internal error - cannot find application for sendMessage()");
    }

    @Override
    public void socketClosed(final UTSSocketHandler source) {
        synchronized (_sessions) {
            for (SessionInfo session : _sessions) {
                if (session._handler == source) {
                    session._handler.close();
                    return;
                }
            }
        }
    }

    /**
     * Receives input from the users via the various UTSSocketHandlers.
     * We find the session that the UTSSocketHandler belongs to and send the message
     * to the most recent application for that session.
     * @param source UTSSocketHandler that sent the data
     * @param message the message to be processed by the listener
     */
    @Override
    public void socketTrafficReceived(final UTSSocketHandler source,
                                      final UTSMessage message) {
        synchronized (_sessions) {
            for (var session : _sessions) {
                if (session._handler == source) {
                    var app = session._applications.peek();
                    if (app != null) {
                        app.handleInput(message);
                    }
                    return;
                }
            }
        }
        IO.println("Internal error - cannot find application for trafficReceived()");
    }

    /**
     * One application can transfer control to another application (expecting to regain control when it terminates).
     * @param fromApplication application that is requesting the transfer
     * @param toApplication new application to be run
     */
    protected void transferApplication(final Application fromApplication,
                                       final Application toApplication) {
        synchronized (_sessions) {
            for (var session : _sessions) {
                if (session._applications.contains(fromApplication)) {
                    session._applications.push(toApplication);
                    toApplication.start();
                    return;
                }
            }
        }
    }

    // Main loop
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
                        IO.println("New connection from " + socketChannel.socket().getRemoteSocketAddress());
                        var handler = new UTSSocketHandler(socketChannel, this);
                        var session = new SessionInfo(handler);
                        var app = new MenuApp(this);
                        session._applications.push(app);
                        _sessions.push(session);
                        app.start();
                        didSomething = true;
                    }
                } else {
                    // check for dead applications
                    synchronized (_sessions) {
                        var iter = _sessions.iterator();
                        while (iter.hasNext()) {
                            var session = iter.next();
                            var app = session._applications.peek();
                            if (app != null) {
                                if (app.isTerminated()) {
                                    IO.println("Application " + app.getClass()
                                                                   .getSimpleName() + " terminated");
                                    session._applications.pop();
                                    var prevApp = session._applications.peek();
                                    if (prevApp != null) {
                                        prevApp.returnFromTransfer();
                                    }
                                    didSomething = true;
                                }
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
