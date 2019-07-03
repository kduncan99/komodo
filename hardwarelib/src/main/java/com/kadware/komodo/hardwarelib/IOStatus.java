/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

/**
 * Identifies a particular IO status
 */
@SuppressWarnings("Duplicates")
public enum IOStatus
{                                   // Printer. .Punch.. .Reader. ..Disk.. ..Tape.. Terminal
    None(0),                  //
    Successful(1),            //    X        X        X        X        X        X
    BufferTooSmall(2),        //                               X        X
    DeviceBusy(3),            //                               X        ?
    EndOfTape(4),             //                                        X
    FileMark(5),              //                                        X
    InvalidBlockCount(6),     //                               X
    InvalidBlockId(7),        //                               X
    InvalidBlockSize(8),      //                               X
    InvalidDeviceAddress(9),  //    X        X        X        X        X        X    // returned by controller
    InvalidFunction(10),      //    X        X        X        X        X
    InvalidMode(11),          //                                        X
    InvalidTransferSize(12),  //                                        X
    InProgress(13),           //                               X
    LostPosition(14),         //                                        X
    MediaError(15),           //                      X
    NoDevice(16),             //    X        X        X        X        X        X    // returned by controller
    NoInput(17),              //                      X                          X
    NotPrepped(18),           //                               X
    NotReady(19),             //    X        X        X        X        X
    QueueFull(20),            //                               X
    SystemException(21),      //    X        X        X        X        X
    UnitAttention(22),        //                               X        ?
    WriteProtected(23);       //                               X        X

    private final int _code;

    IOStatus(int code) { _code = code; }
    public int getCode() { return _code; }

    public static IOStatus getValue(
        final int code
    ) {
        switch (code) {
            case 1:     return Successful;
            case 2:     return BufferTooSmall;
            case 3:     return DeviceBusy;
            case 4:     return EndOfTape;
            case 5:     return FileMark;
            case 6:     return InvalidBlockCount;
            case 7:     return InvalidBlockId;
            case 8:     return InvalidBlockSize;
            case 9:     return InvalidDeviceAddress;
            case 10:    return InvalidFunction;
            case 11:    return InvalidMode;
            case 12:    return InvalidTransferSize;
            case 13:    return InProgress;
            case 14:    return LostPosition;
            case 15:    return MediaError;
            case 16:    return NoDevice;
            case 17:    return NoInput;
            case 18:    return NotPrepped;
            case 19:    return NotReady;
            case 20:    return QueueFull;
            case 21:    return SystemException;
            case 22:    return UnitAttention;
            case 23:    return WriteProtected;
            default:    return None;
        }
    }
}
