/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib;

public enum IoStatus {
    NotStarted,
    Complete,
    InProgress,
    Canceled,
    // -- //
    AtLoadPoint,
    InvalidChannelProgram,
    DeviceDoesNotExist,
    DeviceIsDown,
    DeviceIsNotAccessible,
    DeviceIsNotAttached,
    DeviceIsNotReady,
    EndOfFile,
    EndOfTape,
    InternalError,
    InvalidBlockId,
    InvalidBufferSize,
    InvalidFunction,
    InvalidNodeType,
    InvalidPacket,
    InvalidPackName,
    InvalidPrepFactor,
    InvalidTapeBlock,
    InvalidTrackCount,
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
