/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm;

/**
 * Describes the entry point for the start of a program
 */
class ProgramStart {

    final int _locationCounterIndex;
    final int _locationCounterOffset;

    ProgramStart(
        final int locationCounterIndex,
        final int locationCounterOffset
    ) {
        _locationCounterIndex = locationCounterIndex;
        _locationCounterOffset = locationCounterOffset;
    }
}
