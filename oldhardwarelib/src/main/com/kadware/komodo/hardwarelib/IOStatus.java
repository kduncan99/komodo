/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

/**
 * Identifies a particular Device IO status
 */
public enum IOStatus {
    //                                           Card      Card
    //                                 Printer. .Punch.. .Reader. ..Disk.. ..Tape..
    Successful(0),            //    X        X        X        X        X
    BufferTooSmall(1),        //                               X        X
    DeviceBusy(2),            //                               X        ?
    EndOfTape(3),             //                                        X
    FileMark(4),              //                                        X
    InvalidBlockCount(5),     //                               X
    InvalidBlockId(6),        //                               X
    InvalidBlockSize(7),      //                               X
    InvalidFunction(010),     //    X        X        X        X        X
    InvalidMode(011),         //                                        X
    InvalidTransferSize(012), //                                        X
    InProgress(040),          //                               X
    LostPosition(013),        //                                        X
    MediaError(014),          //                      X
    NoInput(015),             //                      X
    NotPrepped(016),          //                               X
    NotReady(017),            //    X        X        X        X        X
    QueueFull(020),           //                               X
    SystemException(021),     //    X        X        X        X        X
    UnitAttention(022),       //                               X        ?
    WriteProtected(023),      //                               X        X
    InvalidStatus(077);

    private final int _code;

    IOStatus(int code) {
        _code = code;
    }

    public int getCode() {
        return _code;
    }

    public static IOStatus getValue(
        final int code
    ) {
        return switch (code) {
            case 0 -> Successful;
            case 1 -> BufferTooSmall;
            case 2 -> DeviceBusy;
            case 3 -> EndOfTape;
            case 4 -> FileMark;
            case 5 -> InvalidBlockCount;
            case 6 -> InvalidBlockId;
            case 7 -> InvalidBlockSize;
            case 010 -> InvalidFunction;
            case 011 -> InvalidMode;
            case 012 -> InvalidTransferSize;
            case 013 -> LostPosition;
            case 014 -> MediaError;
            case 015 -> NoInput;
            case 016 -> NotPrepped;
            case 017 -> NotReady;
            case 020 -> QueueFull;
            case 021 -> SystemException;
            case 022 -> UnitAttention;
            case 023 -> WriteProtected;
            case 040 -> InProgress;
            default -> InvalidStatus;
        };
    }
}
