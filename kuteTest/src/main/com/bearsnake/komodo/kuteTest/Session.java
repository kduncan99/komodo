/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kuteTest;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedList;

import static com.bearsnake.komodo.kute.Constants.*;

public class Session implements Runnable {

    // Input from terminal cannot exceed this size
    private static final int INPUT_BUFFER_SIZE = 8192;

    // Poll for input at this rate under normal conditions
    private static final int NORMAL_POLL_RATE_MSEC = 100;

    // If no input is received for this duration, switch to slow poll mode
    private static final int SLOW_POLL_DELAY_MSEC = 5 * 1000 * 1000;

    // Poll for input at this rate when in slow poll mode
    private static final int SLOW_POLL_RATE_MSEC = 1000;

    // Simple UTS streams
    private static final byte[] STATUS_POLL = {ASCII_SOH, ASCII_ENQ, ASCII_ETX};
    private static final byte[] TRAFFIC_POLL = {ASCII_SOH, ASCII_ETX};
    private static final byte[] SEND_DROP_SESSION = {ASCII_SOH, ASCII_DLE, ASCII_EOT, ASCII_STX, ASCII_ETX};
    private static final byte[] SEND_MSG_WAIT = {ASCII_SOH, ASCII_BEL, ASCII_STX, ASCII_ETX};

    private final SocketChannel _channel;
    private boolean _terminate = false;
    private final Thread _thread = new Thread(this);

    private enum InputState {
        IDLE,           // we have not yet sent a poll
        READING,        // we have read at least an SOH, and have not yet read an ETX
        PENDING_SOH,    // we sent a poll, now we are waiting for SOH
    }

    private ByteBuffer _inputBuffer = ByteBuffer.allocateDirect(INPUT_BUFFER_SIZE);
    private LinkedList<byte[]> _streamQueue = new LinkedList<>();
    private InputState _inputState = InputState.IDLE;
    private Instant _lastInputTime = Instant.now();
    private int _pollRate = NORMAL_POLL_RATE_MSEC;

    public Session(SocketChannel channel) {
        _channel = channel;
        _thread.start();
    }

    public void handleIdle() throws IOException{
        IO.println("handleIdle");//TODO remove
        try {
            if (_pollRate > 0) {
                Thread.sleep(_pollRate);
            }
            sendTrafficPoll();
            _inputState = InputState.PENDING_SOH;
        } catch (InterruptedException ex) {
            IO.println("Session poll delay interrupted");
        }
    }

    public void handlePendingSOH() throws IOException {
        IO.println("handlePendingSOH");//TODO remove
        var sohBuffer = ByteBuffer.allocate(1);
        if (_channel.read(sohBuffer) == 0) {
            // nothing to read. If we've exceeded the poll delay, go back to idle state
            var expiration = _lastInputTime.plusMillis(_pollRate);
            if (Instant.now().isAfter(expiration)) {
                _inputState = InputState.IDLE;
            }
        } else if (sohBuffer.get(0) == ASCII_SOH) {
            // We have an SOH. Reset the input buffer and copy the SOH thereto,
            // then change our input mode.
            _inputBuffer.clear();
            _inputBuffer.put(ASCII_SOH);
            _inputState = InputState.READING;
            _lastInputTime = Instant.now();
        } else {
            // we read something, but it was not an SOH - ignore it but it still counts as input
            _lastInputTime = Instant.now();
        }
    }

    public void handleReading() throws IOException {
        IO.println("handleReading");//TODO remove
        // TODO currently we just check for ETX and throw away anything following it.
        //   there should not be anything there, but there *could* be,
        //   so we should actually handle that correctly.
        _channel.read(_inputBuffer);
        if (_inputBuffer.get(_inputBuffer.position() - 1) == ASCII_ETX) {
            _streamQueue.addLast(Arrays.copyOf(_inputBuffer.array(), _inputBuffer.position()));
            _inputState = InputState.IDLE;
        }
    }

    public byte[] readNextStream() {
        return _streamQueue.pollFirst();
    }

    public void run() {
        IO.println("Session started");

        while (!_terminate) {
            try {
                switch (_inputState) {
                    case IDLE -> handleIdle();
                    case PENDING_SOH -> handlePendingSOH();
                    case READING -> handleReading();
                }
            } catch (IOException ex) {
                IO.println("Session failed to handle input");
                _terminate = true;
            }
        }

        try {
            _channel.close();
        } catch (IOException ex) {
            IO.println("Session failed to close channel");
        }
        IO.println("Session ended");
    }

    //  SOH STX message ETX - message to terminal
    protected void sendMessage(final UTSOutputStream stream) throws IOException {
        var bb = ByteBuffer.allocate(stream.size() + 3);
        bb.put(ASCII_SOH);
        bb.put(ASCII_STX);
        bb.put(stream.toByteArray(), 0, stream.size());
        bb.put(ASCII_ETX);
        bb.flip();
        _channel.write(bb);
    }

    //  SOH BEL STX ETX     - set message waiting on terminal
    public void sendMessageWaiting() throws IOException {
        _channel.write(ByteBuffer.wrap(SEND_MSG_WAIT));
    }

    //  SOH DLE EOT STX ETX - causes terminal to drop the session (host could just drop the TCP session anyway...)
    public void sendDropSession() throws IOException {
        _channel.write(ByteBuffer.wrap(SEND_DROP_SESSION));
    }

    private void sendTrafficPoll() throws IOException {
        // Conventional messages from the host follow this pattern:
        //  SOH RID SID DID ENQ ETX BCC
        // We do not send RID/SID/DID, nor BCC.
        _channel.write(ByteBuffer.wrap(TRAFFIC_POLL));
    }

    public void terminate() {
        _terminate = true;
        _thread.interrupt();
        try {
            _channel.close();
        } catch (IOException e) {
            // do nothing
        }
    }
}
