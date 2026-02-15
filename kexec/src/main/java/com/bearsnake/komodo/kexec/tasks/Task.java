/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.tasks;

/**
 * Represents an exec Task - i.e., a program
 */
public abstract class Task {
    // TODO

    private final String _programName;

    public Task(String programName) {
        _programName = programName;
    }

    public String getProgramName() { return _programName; }
}
