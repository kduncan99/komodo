/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

/**
 * Identifies a particular Device status
 */
@SuppressWarnings("Duplicates")
public enum DeviceStatus
{                                   //           Card      Card
                                    // Printer. .Punch.. .Reader. ..Disk.. ..Tape..
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

    DeviceStatus(int code) { _code = code; }

    public int getCode() { return _code; }

    public DeviceStatus getValue(
        final int code
    ) {
        switch (code) {
            case 0:     return Successful;
            case 1:     return BufferTooSmall;
            case 2:     return DeviceBusy;
            case 3:     return EndOfTape;
            case 4:     return FileMark;
            case 5:     return InvalidBlockCount;
            case 6:     return InvalidBlockId;
            case 7:     return InvalidBlockSize;
            case 010:   return InvalidFunction;
            case 011:   return InvalidMode;
            case 012:   return InvalidTransferSize;
            case 013:   return LostPosition;
            case 014:   return MediaError;
            case 015:   return NoInput;
            case 016:   return NotPrepped;
            case 017:   return NotReady;
            case 020:   return QueueFull;
            case 021:   return SystemException;
            case 022:   return UnitAttention;
            case 023:   return WriteProtected;
            case 040:   return InProgress;
            default:    return InvalidStatus;
        }
    }
}
