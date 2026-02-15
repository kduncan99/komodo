/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.tasks;

/**
 * Represents a nexe (native executable) task - that is, a program which acts as a compiled 36-bit program
 * in the 2200 world, but is actually a Java compiled class (or classes) which execute in the host
 * operating system space.
 */
public abstract class NexeTask extends Task {
    // TODO

    public NexeTask(
        final String programName
    ) {
        super(programName);
    }
}
