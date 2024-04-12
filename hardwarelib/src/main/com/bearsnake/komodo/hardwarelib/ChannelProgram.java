/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib;

import com.bearsnake.komodo.baselib.ArraySlice;

import java.util.LinkedList;

public class ChannelProgram {

    // adjusts the word pointer as data is transferred
    public enum Direction {
        Increment,
        Decrement,
        NoChange,
        SkipData,
    }

    // only for tape
    public enum Format {
        QuarterWord, // AFormat
        Packed,      // CFormat
        SixthWord,   // DFormat
    }

    public enum Function {
        Read,
        Write,
    }

    public static class ControlWord {
        public Direction _direction = Direction.Increment;
        public Format _format = Format.Packed;
        public ArraySlice _buffer;
        public int _bufferOffset;   // index into _buffer
        public int _transferCount;  // in words

        public ControlWord setDirection(Direction value) { _direction = value; return this; }
        public ControlWord setFormat(Format value) { _format = value; return this; }
        public ControlWord setBuffer(ArraySlice value) { _buffer = value; return this; }
        public ControlWord setBufferOffset(int value) { _bufferOffset = value; return this; }
        public ControlWord setTransferCount(int value) { _transferCount = value; return this; }
    }

    public final LinkedList<ControlWord> _controlWords = new LinkedList<>();
    public int _wordsTransferred;
    public IoStatus _ioStatus;

    public ChannelProgram addControlWord(ControlWord value) { _controlWords.add(value); return this; }
    public ChannelProgram setWordsTransferred(int value) { _wordsTransferred = value; return this; }
    public ChannelProgram setIoStatus(IoStatus value) { _ioStatus = value; return this; }
}
