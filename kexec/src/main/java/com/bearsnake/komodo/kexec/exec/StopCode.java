/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec;

public enum StopCode {
    FacilitiesComplex(001),
    SymbiontNameNotFound(024),
    UseStatementToExecPCTFailed(031),
    UndefinedGENFType(033),
    FileAssignErrorOccurredDuringSystemInitialization(034),
    InternalExecIOFailed(040),
    FullCycleReachedForRunIds(044),
    ExecRequestForMassStorageFailed(052),
    IllegalNumberOfEntriesOnQueue(054),
    ErrorAccessingFacilitiesDataStructure(055),
    ConsoleResponseRequiresReboot(055),
    TrackToBeReleasedWasNotAllocated(057),
    NoMainItemLink(057),
    InitializationSystemConfigurationError(064),
    InitializationSystemLibrariesCorruptOrMissing(064),
    PanicDumpFailed(064),
    ClearTestSetAttemptedWhenNotSet(066),
    ResourceReleaseFailure(067),
    ActivityIdNoLongerExists(073),
    ExecContingencyHandler(0103),
    ExecActivityTakenToEMode(0105),
    IOErrorBootTape(0145),
    OperatorInitiatedRecovery(0150), // i.e., $!
    DirectoryErrors(0151),
    SectorToBeReleasedWasNotAllocated(0157),
    IOPacketErrorForSystemIO(0202),
    ErrorInSystemIOTable(0205),
    LastSystemConsoleDown(0240),
    InvalidLDAT(0253),
    InvalidPrepFactor(0265),
    MassStorageDebug(0266);

    private final int _code;

    StopCode(final int code) {
        _code = code;
    }

    public int getCode() { return _code; }
}
