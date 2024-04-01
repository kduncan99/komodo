/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec;

public enum StopCode {
    FacilitiesComplex(001),
    UseStatementToExecPCTFailed(031),
    FileAssignErrorOccurredDuringSystemInitialization(034),
    InternalExecIOFailed(040),
    FullCycleReachedForRunIds(044),
    ExecRequestForMassStorageFailed(052),
    ErrorAccessingFacilitiesDataStructure(055),
    ConsoleResponseRequiresReboot(055),
    TrackToBeReleasedWasNotAllocated(057),
    NoMainItemLink(057),
    InitializationSystemConfigurationError(064),
    InitializationSystemLibrariesCorruptOrMissing(044),
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
    InvalidLDAT(0253);

    private final int _code;

    StopCode(final int code) {
        _code = code;
    }

    public int getCode() { return _code; }
}
