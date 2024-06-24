/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec;

/**
 * Status codes for IO completion.
 */
public enum ERIO$Status {

    Success(0),
    EndOfBlock(01),
    EndOfTape(02),
    NotFound(03),
    NonIntegralBlock(04),
    UnallocatedArea(05),
    TapeUnitIncompatibility(06),
    SystemError(07),
    UnlockTimeOut(010),
    BufferedDataNotWritten(010),
    NonRecoverableError(011),
    LostPosition(012),
    DeviceDownOrNotAvailable(013),
    HardwareDataConverterError(014),
    PackDownOrInhibited(015),
    IOPacketPartiallyOutOfLimits(016),
    SystemResourcesUnavailable(017),
    ReadInhibited(020),
    WriteInhibited(020),
    FileNotFound(021),
    CannotExpandFile(022),
    ReadExtentOutOfRange(022),
    IOPacketNotWithinLimits(023),
    InvalidFunction(024),
    FunctionNotAllowed(024),
    BufferOutOfLimits(025),
    TransferSizeLessThanNoiseConstant(025),
    BufferInActivityLocalStorage(025), // for IOI$ or IOWI$
    InsufficientRemovableSpace(026),
    IOAlreadyInProgress(027),
    TaskAbortBitSet(033), // operator terminated the run
    CannotResolveFileRelativeAddress(034), // (due to system error)
    ReadLockOrUnlockFailed(035),
    InvalidWAIT$Request(036),
    InProgress(040);

    private final int _code;

    ERIO$Status(final int code) {
        _code = code;
    }

    public int getCode() { return _code; }
}
