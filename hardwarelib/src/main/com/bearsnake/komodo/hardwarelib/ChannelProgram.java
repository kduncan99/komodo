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
        ReadBackward,
        Write,
        Control,
    }

    public enum SubFunction {
        MoveBlock,
        MoveBlockBackward,
        MoveFile,
        MoveFileBackward,
        Rewind,
        Unload,
    }

    public static class ControlWord {
        private Direction _direction = Direction.Increment;
        private ArraySlice _buffer;
        private int _bufferOffset;   // index into _buffer
        private int _transferCount;  // in words

        public final Direction getDirection() { return _direction; }
        public final ArraySlice getBuffer() { return _buffer; }
        public final int getBufferOffset() { return _bufferOffset; }
        public final int getTransferCount() { return _transferCount; }

        public ControlWord setDirection(Direction value) { _direction = value; return this; }
        public ControlWord setBuffer(ArraySlice value) { _buffer = value; return this; }
        public ControlWord setBufferOffset(int value) { _bufferOffset = value; return this; }
        public ControlWord setTransferCount(int value) { _transferCount = value; return this; }
    }

    long _blockId;
    final LinkedList<ControlWord> _controlWords = new LinkedList<>();
    Function _function;
    SubFunction _subFunction;
    IoStatus _ioStatus;
    int _nodeIdentifier;
    int _wordsTransferred;

    public long getBlockId() { return _blockId; }
    public Function getFunction() { return _function; }
    public IoStatus getIoStatus() { return _ioStatus; }
    public int getNodeIdentifier() { return _nodeIdentifier; }
    public SubFunction getSubFunction() { return _subFunction; }
    public int getWordsTransferred() { return _wordsTransferred; }

    public ChannelProgram addControlWord(ControlWord value) { _controlWords.add(value); return this; }
    public ChannelProgram setBlockId(long value) { _blockId = value; return this; }
    public ChannelProgram setFunction(Function value) { _function = value; return this; }
    public ChannelProgram setIoStatus(IoStatus value) { _ioStatus = value; return this; }
    public ChannelProgram setNodeIdentifier(int value)  { _nodeIdentifier = value; return this; }
    public ChannelProgram setSubFunction(SubFunction value) { _subFunction = value; return this; }
    public ChannelProgram setWordsTransferred(int value) { _wordsTransferred = value; return this; }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append(String.format("Node:%d Func:%s SubFunc:%s BlkId:%d",
                                _nodeIdentifier, _function, _subFunction, _blockId));
        for (var cw : _controlWords) {
            sb.append(String.format(" [%s off:%d xfr:%d]", cw._direction, cw._bufferOffset, cw._transferCount));
        }
        return sb.toString();
    }
}
