/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib;

public enum IoStatus {
    NotStarted,
    Successful,
    Canceled,
    // -- //
    AtLoadPoint,
    BufferIsNull,
    DataException, // something in the device meta-data is bad
    DeviceDoesNotExist,
    DeviceIsDown,
    DeviceIsNotAccessible,
    DeviceIsNotAttached,
    DeviceIsNotReady,
    EndOfFile,
    EndOfTape,
    InternalError,
    InvalidAddress, // for Channel IO, address is not a multiple of 2 for Packed transfer format
    InvalidBlockCount,
    InvalidBlockId,
    InvalidBufferSize,
    InvalidFunction,
    InvalidNodeType,
    InvalidPacket,
    InvalidPackName,
    InvalidPrepFactor,
    InvalidTapeBlock,
    InvalidTrackCount,
    InvalidTransferFormat,
    LostPosition,
    MediaAlreadyMounted,
    MediaNotMounted,
    NonIntegralRead,
    PackNotPrepped,
    ReadNotAllowed,
    ReadOverrun,
    SystemError,
    WriteProtected,
}
