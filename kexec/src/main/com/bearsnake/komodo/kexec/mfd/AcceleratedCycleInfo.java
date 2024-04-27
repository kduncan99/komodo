/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

public class AcceleratedCycleInfo {

    private final FileCycleInfo _fileCycleInfo;
    private final FileAllocationSet _fileAllocationSet;

    public AcceleratedCycleInfo(
        final FileCycleInfo cycleInfo,
        final FileAllocationSet allocationSet
    ) {
        _fileCycleInfo = cycleInfo;
        _fileAllocationSet = allocationSet;
    }

    public FileCycleInfo getFileCycleInfo() { return _fileCycleInfo; }
    public FileAllocationSet getFileAllocationSet() { return _fileAllocationSet; }
}
