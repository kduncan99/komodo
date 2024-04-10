/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

/**
 * Identifies a particular IO operation
 */
public enum IOFunction {
    //                                      Card     Card
    //                             Printer .Punch.. .Reader. ..Disk.. ..Tape..
    None(0),              //    X       X        X        X        X
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
        return switch (code) {
            case 1 -> Close;
            case 2 -> GetInfo;
            case 3 -> MoveBlock;
            case 4 -> MoveBlockBackward;
            case 5 -> MoveFile;
            case 6 -> MoveFileBackward;
            case 7 -> Read;
            case 8 -> ReadBackward;
            case 9 -> Reset;
            case 10 -> Rewind;
            case 11 -> RewindInterlock;
            case 12 -> SetMode;
            case 13 -> Unload;
            case 14 -> Write;
            case 15 -> WriteEndOfFile;
            default -> None;
        };
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
