/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.symbionts;

import com.bearsnake.komodo.kexec.exceptions.EndOfFileException;
import com.bearsnake.komodo.kexec.exceptions.ExecIOException;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;

import java.time.Instant;

/**
 * Represents all classes which assist the exec in handling PRINT$ or PUNCH$ IO for a run.
 */
public interface SymbiontWriter {

    /**
     * Writes an EOF control record to the output buffer, then drains the buffer to backing storage.
     * Does not actually close anything.
     */
    public abstract void close() throws ExecStoppedException, ExecIOException;

    public abstract void setCurrentCharacterSet(
        final int characterSet
    ) throws ExecStoppedException, ExecIOException;

    public abstract void writeEndOfFileControlImage(
    ) throws ExecStoppedException, ExecIOException;

    public abstract void writeFileLabelControlImage(
        final int characterSet
    ) throws ExecStoppedException, ExecIOException;

    public abstract void writeFTPLabelControlImage(
    ) throws ExecStoppedException, ExecIOException;

    public abstract void writePRINT$LabelControlImage(
        final int partNumber,
        final int characterSet,
        final String filename,
        final String inputDevice,
        final String runId,
        final Instant timeStamp,
        final String userId,
        final long pageCount,
        final String accountId,
        final String projectId,
        final long fileSizeTracks,
        final String banner
    ) throws ExecStoppedException, EndOfFileException, ExecIOException;

    public abstract void writePUNCH$LabelControlImage(
        final int partNumber,
        final int characterSet,
        final String filename,
        final String inputDevice,
        final String runId,
        final Instant timeStamp,
        final String userId,
        final long cardCount,
        final String accountId,
        final String projectId,
        final long fileSizeTracks,
        final String banner
    ) throws ExecStoppedException, EndOfFileException, ExecIOException;

    public abstract void writeREAD$LabelControlImage(
        final int characterSet,
        final String filename,
        final String inputDevice,
        final String runId,
        final Instant timeStamp
    ) throws ExecStoppedException, EndOfFileException, ExecIOException;

    public abstract void writeDataImage(
        final String image
    ) throws ExecStoppedException, EndOfFileException, ExecIOException;

    public abstract void writeDataImage(
        final int spacing,
        final String image
    ) throws ExecStoppedException, EndOfFileException, ExecIOException;

    public abstract void writePrintControlImage(
        final String image
    ) throws ExecStoppedException, EndOfFileException, ExecIOException;
}
