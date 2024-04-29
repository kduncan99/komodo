/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.mfd;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tracks information for a particular file cycle which is currently assigned to at least one run.
 */
public class AcceleratedCycleInfo {

    private AtomicInteger _assignCount = new AtomicInteger(0);
    private final FileCycleInfo _fileCycleInfo;
    private final FileAllocationSet _fileAllocationSet;

    public AcceleratedCycleInfo(
        final FileCycleInfo cycleInfo,
        final FileAllocationSet allocationSet
    ) {
        _fileCycleInfo = cycleInfo;
        _fileAllocationSet = allocationSet;
    }

    public int decrementAssignCount() { return _assignCount.decrementAndGet(); }
    public FileCycleInfo getFileCycleInfo() { return _fileCycleInfo; }
    public FileAllocationSet getFileAllocationSet() { return _fileAllocationSet; }
    public int incrementAssignCount() { return _assignCount.incrementAndGet(); }
}
