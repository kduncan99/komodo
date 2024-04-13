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
        public ArraySlice _buffer;
        public int _bufferOffset;   // index into _buffer
        public int _transferCount;  // in words

        public ControlWord setDirection(Direction value) { _direction = value; return this; }
        public ControlWord setBuffer(ArraySlice value) { _buffer = value; return this; }
        public ControlWord setBufferOffset(int value) { _bufferOffset = value; return this; }
        public ControlWord setTransferCount(int value) { _transferCount = value; return this; }
    }

    public long _blockId;
    public final LinkedList<ControlWord> _controlWords = new LinkedList<>();
    public Function _function;
    public IoStatus _ioStatus;
    public int _nodeIdentifier;
    public int _wordsTransferred;

    public ChannelProgram addControlWord(ControlWord value) { _controlWords.add(value); return this; }
    public ChannelProgram setBlockId(long value) { _blockId = value; return this; }
    public ChannelProgram setFunction(Function value) { _function = value; return this; }
    public ChannelProgram setIoStatus(IoStatus value) { _ioStatus = value; return this; }
    public ChannelProgram setNodeIdentifier(int value)  { _nodeIdentifier = value; return this; }
    public ChannelProgram setWordsTransferred(int value) { _wordsTransferred = value; return this; }

    @Override
    public String toString() {
        return String.format("Node:%d Func:%s BlkId:%d", _nodeIdentifier, _function, _blockId);
    }
}
