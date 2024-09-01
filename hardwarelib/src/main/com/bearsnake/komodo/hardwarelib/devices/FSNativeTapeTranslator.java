/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib.devices;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;

/**
 * Native (to komodo) tape translator.
 * Data is stored as byte blocks, with 4-byte (signed integer) header and footer control words.
 * The control word is simply the length of the data in the data block (not inclusive of the control words).
 * The control word is written in big-endian format.
 * We do support zero-length blocks, although the utility of such is questionable.
 * File marks are written as a single 4-byte stream of all ones. The file mark can be read and written
 * as a control word containing -1.
 */
public class FSNativeTapeTranslator extends FSTapeTranslator {

    private static final byte[] TAPE_MARK = { (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff };
    private static final ByteBuffer TAPE_MARK_BB = ByteBuffer.wrap(TAPE_MARK);

    private final ByteBuffer _controlWordBuffer;

    FSNativeTapeTranslator(
        final FileChannel channel
    ) {
        super(channel);
        _controlWordBuffer = ByteBuffer.allocate(4);
        _controlWordBuffer.order(ByteOrder.BIG_ENDIAN);
    }

    private int readControlWord() throws IOException {
        _controlWordBuffer.clear();
        int bytes = _channel.read(_controlWordBuffer);
        if (bytes < _controlWordBuffer.capacity()) {
            throw new EOFException();
        }
        return _controlWordBuffer.getInt();
    }

    private void writeControlWord(final int controlWord) throws IOException {
        _controlWordBuffer.rewind();
        _controlWordBuffer.putInt(controlWord);
        int bytes = _channel.write(_controlWordBuffer);
        if (bytes < _controlWordBuffer.capacity()) {
            throw new EOFException();
        }
    }

    void moveBackward() throws IOException, EndOfTapeException {
        long currentPosition = _channel.position();
        while (true) {
            long footerPosition = currentPosition - 4;
            if (footerPosition < 0) {
                throw new EndOfTapeException();
            }

            _channel.position(footerPosition);
            int cwf = readControlWord();
            if (cwf == -1) {
                _channel.position(footerPosition);
                break;
            }

            currentPosition = currentPosition - cwf - 8;
        }
    }

    void moveForward() throws IOException {
        long currentPosition = _channel.position();
        int cwh = readControlWord();
        while (cwh != -1) {
            long newPosition = currentPosition + cwh + 8;
            _channel.position(newPosition);
            currentPosition = newPosition;
            cwh = readControlWord();
        }
    }

    ByteBuffer read() throws IOException, DataException, TapeMarkException {
        long currentPosition = _channel.position();

        // read header
        int cwh = readControlWord();
        if (cwh == -1) {
            throw new TapeMarkException();
        }

        // read data
        var bb = ByteBuffer.allocate(cwh);
        long dataPosition = currentPosition + 4;
        _channel.read(bb, dataPosition);

        int cwf = readControlWord();
        if (cwf != cwh) {
            throw new DataException();
        }

        return bb;
    }

    ByteBuffer readBackward() throws IOException, DataException, EndOfTapeException, TapeMarkException {
        long currentPosition = _channel.position();
        if (currentPosition < 4) {
            throw new EndOfTapeException();
        }

        // read previous block footer
        long footerPosition = currentPosition - 4;
        _channel.position(footerPosition);
        int cwf = readControlWord();
        if (cwf == -1) {
            _channel.position(footerPosition);
            throw new TapeMarkException();
        }

        var bb = ByteBuffer.allocate(cwf);
        long dataPosition = footerPosition - cwf;
        _channel.read(bb, dataPosition);

        long headerPosition = dataPosition - 4;
        _channel.position(headerPosition);
        int cwh = readControlWord();
        if (cwf != cwh) {
            throw new DataException();
        }

        _channel.position(headerPosition);
        return bb;
    }

    int write(final ByteBuffer bb) throws IOException {
        int bytes = 0;
        int cw = bb.capacity();
        writeControlWord(cw);
        if (cw > 0) {
            bb.rewind();
            bytes = _channel.write(bb);
            if (bytes < cw) {
                throw new IOException("Insufficient bytes written");
            }
        }
        writeControlWord(cw);
        return bytes;
    }

    int writeTapeMark() throws IOException {
        return _channel.write(TAPE_MARK_BB);
    }
}
