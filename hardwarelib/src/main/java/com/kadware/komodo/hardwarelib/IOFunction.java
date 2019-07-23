/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

/**
 * Identifies a particular IO operation
 */
@SuppressWarnings("Duplicates")
public enum IOFunction
                                //          Card     Card
{                               // Printer .Punch.. .Reader. ..Disk.. ..Tape..
    None(0),              //
    Close(1),             //            X
    GetInfo(2),           //    X       X        X        X        X
    MoveBlock(3),         //                                       X
    MoveBlockBackward(4), //                                       X
    MoveFile(5),          //                                       X
    MoveFileBackward(6),  //                                       X
    Read(7),              //                     X        X        X
    ReadBackward(8),      //                                       X
    Reset(9),             //    X       X        X        X        X
    Rewind(10),           //                                       X
    RewindInterlock(11),  //                                       X
    SetMode(12),          //                                       X
    Unload(13),           //                                       X
    Write(14),            //    X       X                 X        X
    WriteEndOfFile(15);   //                                       X

    private final int _code;

    IOFunction(int code) {
        _code = code;
    }

    public int getCode() {
        return _code;
    }

    public static IOFunction getValue(
        final int code
    ) {
        switch (code) {
            case 1:
                return Close;
            case 2:
                return GetInfo;
            case 3:
                return MoveBlock;
            case 4:
                return MoveBlockBackward;
            case 5:
                return MoveFile;
            case 6:
                return MoveFileBackward;
            case 7:
                return Read;
            case 8:
                return ReadBackward;
            case 9:
                return Reset;
            case 10:
                return Rewind;
            case 11:
                return RewindInterlock;
            case 12:
                return Unload;
            case 13:
                return Write;
            case 14:
                return WriteEndOfFile;
            default:
                return None;
        }
    }

    public boolean requiresBuffer() {
        return (this == IOFunction.Read)
               || (this == IOFunction.ReadBackward)
               || (this == IOFunction.Write);
    }

    public boolean isReadFunction() {
        return (this == IOFunction.GetInfo)
               || (this == IOFunction.Read)
               || (this == IOFunction.ReadBackward);
    }

    public boolean isWriteFunction() {
        return (this == IOFunction.Write)
               || (this == IOFunction.WriteEndOfFile);
    }
}
