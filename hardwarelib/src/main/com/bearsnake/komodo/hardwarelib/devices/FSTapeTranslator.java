/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib.devices;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public abstract class FSTapeTranslator {

    public static class DataException extends Exception{}
    public static class EndOfTapeException extends Exception{}
    public static class TapeMarkException extends Exception{}

    protected final FileChannel _channel;

    FSTapeTranslator(
        final FileChannel channel
    ) {
        _channel = channel;
    }

    abstract void moveBackward() throws IOException, EndOfTapeException;
    abstract void moveForward() throws IOException;
    abstract ByteBuffer read() throws IOException, DataException, TapeMarkException;
    abstract ByteBuffer readBackward() throws IOException, EndOfTapeException, TapeMarkException, DataException;
    abstract int write(final ByteBuffer bb) throws IOException;
    abstract int writeTapeMark() throws IOException;

//    protected int readBytes(final ByteBuffer bb,
//                            final long position,
//                            final int transferSize) throws IOException {
//        var buf = bb.capacity() > transferSize ? bb.slice(0, transferSize) : bb;
//        return _channel.read(buf, position);
//    }
}
