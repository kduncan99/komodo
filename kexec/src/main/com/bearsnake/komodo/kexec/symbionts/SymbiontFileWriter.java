/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.symbionts;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.kexec.DateConverter;
import com.bearsnake.komodo.kexec.SDFFileType;
import com.bearsnake.komodo.kexec.consoles.ConsoleType;
import com.bearsnake.komodo.kexec.exceptions.ExecIOException;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exec.ERIO$Status;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.facilities.IOResult;
import com.bearsnake.komodo.logger.Level;
import com.bearsnake.komodo.logger.LogManager;

import java.time.Instant;

/**
 * Assists the exec in handling PRINT$ or PUNCH$ IO specifically to an SDF file or element for a run.
 * (Generally not for an element, but it could happen I guess)
 */
public class SymbiontFileWriter implements SymbiontWriter {

    private final String _internalFileName;
    private final SDFFileType _fileType;
    private final ArraySlice _buffer;
    private long _nextAddress = 0;        // file-relative sector address of the next sector to be written
    private int _bufferIndex = 0;         // word index of the next word to be written to the buffer
    private int _bufferRemaining = 0;     // number of words not yet written to the buffer
    private int _currentCharacterSet;     // 0 is fieldata, we treat anything else as ASCII-like

    /**
     * Writes an EOF control record to the output buffer, then drains the buffer to backing storage.
     * Does not actually close anything.
     */
    public void close() throws ExecStoppedException, ExecIOException {
        if (_bufferIndex > 0) {
            writeEndOfFileControlImage();
            writeBuffer();
        }
    }

    /**
     * For writing an SDF file
     * @param internalFileName internal file name of file containing the symbiont output element
     * @param fileType SDFFileType (PRINT$ or PUNCH$)
     * @param bufferSize symbiont IO buffer size
     */
    public SymbiontFileWriter(
        final String internalFileName,
        final SDFFileType fileType,
        final int initialCharacterSet,
        final int bufferSize
    ) {
        _internalFileName = internalFileName;
        _currentCharacterSet = initialCharacterSet;
        _fileType = fileType;
        _buffer = new ArraySlice(new long[bufferSize]);
        _bufferRemaining = bufferSize;
        _nextAddress = 0;
    }

    /**
     * If the current character set is different from the requested value, it is updated to the requested
     * character set and an appropriate control record is emitted.
     * @param characterSet 00 for fieldata, 01 for ASCII
     * @throws ExecStoppedException If the exec stops in the middle of this process
     */
    @Override
    public void setCurrentCharacterSet(
        final int characterSet
    ) throws ExecStoppedException, ExecIOException {
        if (characterSet != _currentCharacterSet) {
            _currentCharacterSet = characterSet;
            var controlWord = ((long)042) << 30 | (characterSet & 077);
            writeWord(controlWord);
        }
    }

    @Override
    public void writeEndOfFileControlImage(
    ) throws ExecStoppedException, ExecIOException {
        var controlWord = 0_770000_000000L;
        writeWord(controlWord);
    }

    @Override
    public void writeFileLabelControlImage(
        final int characterSet
    ) throws ExecStoppedException, ExecIOException {
        var controlWord = 0_500116_000000L | (characterSet & 077);
        var dataWord = Word36.stringToWordFieldata("*SDFF*");
        writeWord(controlWord);
        writeWord(dataWord);
    }

    @Override
    public void writeFTPLabelControlImage(
    ) throws ExecStoppedException, ExecIOException {
        var controlWord = 0_500101_000001L;
        var dataWord = Word36.stringToWordFieldata("*SDFF*");
        writeWord(controlWord);
        writeWord(dataWord);
    }

    @Override
    public void writePRINT$LabelControlImage(
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
    ) throws ExecStoppedException {
        // TODO
    }

    @Override
    public void writePUNCH$LabelControlImage(
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
    ) throws ExecStoppedException {
        // TODO
    }

    @Override
    public void writeREAD$LabelControlImage(
        final int characterSet,
        final String filename,
        final String inputDevice,
        final String runId,
        final Instant timeStamp
    ) throws ExecStoppedException, ExecIOException {
        var controlWord = 0_501110_000000L | (characterSet & 077);
        var dataWords = new long[011];
        var padFileName = String.format("%-12s", filename);
        dataWords[00] = Word36.stringToWordFieldata(padFileName.substring(0, 6));
        dataWords[01] = Word36.stringToWordFieldata(padFileName.substring(6));
        dataWords[02] = Word36.stringToWordFieldata(inputDevice);
        dataWords[03] = Word36.stringToWordFieldata(runId);
        dataWords[04] = 0;
        dataWords[05] = DateConverter.getModifiedSingleWordTime(timeStamp);
        dataWords[06] = 0_050505_050505L;
        dataWords[07] = 0_050505_050505L;
        dataWords[010] = 0_050505_050505L;
        writeWord(controlWord);
        writeWords(dataWords, 0, 011);
    }

    /**
     * Writes a print control image to the output buffer, converting according to the current character set.
     * Splits the image on buffer boundaries if/as necessary.
     * @param image image to be converted and written
     * @throws ExecStoppedException If the exec stops in the middle of this process
     */
    @Override
    public void writePrintControlImage(
        final String image
    ) throws ExecStoppedException, ExecIOException {
        var dataBuffer = (_currentCharacterSet == 0)
            ? ArraySlice.stringToWord36Fieldata(image.substring(0, 63 * 6))
            : ArraySlice.stringToWord36ASCII(image.substring(0, 63 * 4));

        var dataLen = dataBuffer._array.length;
        if (dataLen + 1 <= _bufferRemaining) {
            var controlWord = ((long) 060) << 30;
            controlWord |= ((long) dataLen) << 24;
            controlWord |= _currentCharacterSet & 077;
            writeWord(controlWord);
            writeWords(dataBuffer._array, 0, dataLen);
        } else {
            var subDataLen = _bufferRemaining;
            var controlWord = ((long) 060) << 30;
            controlWord |= ((long) subDataLen) << 24;
            controlWord |= _currentCharacterSet & 077;
            writeWord(controlWord);
            writeWords(dataBuffer._array, 0, subDataLen);

            var offset = subDataLen;
            while (offset < dataLen) {
                subDataLen = Math.min(Math.min(dataLen - offset, (_bufferRemaining - 1)), 63);
                controlWord = ((long)051) << 30;
                controlWord |= ((long)subDataLen) << 24;
                controlWord |= _currentCharacterSet & 077;
                writeWord(controlWord);
                writeWords(dataBuffer._array, offset, subDataLen);
                offset += subDataLen;
            }
        }
    }

    /**
     * Writes a punch image to the output buffer (but with a safeguard in case we are a print file)
     * Splits the image on buffer boundaries.
     * @param image image to be converted and written
     * @throws ExecStoppedException If the exec stops in the middle of this process
     */
    @Override
    public void writeDataImage(
        final String image
    ) throws ExecStoppedException, ExecIOException {
        LogManager.logInfo("SymbiontFileWriter", "writeDataImage:'%s'", image);// TODO remove
        writeDataImage(_fileType == SDFFileType.PRINT$ ? 1 : 0, image);
    }

    /**
     * Writes a print image to the output buffer, converting according to the current character set.
     * Splits the image on buffer boundaries if/as necessary.
     * @param spacing print spacing - leave 0 for punch files
     * @param image image to be converted and written
     * @throws ExecStoppedException If the exec stops in the middle of this process
     */
    @Override
    public void writeDataImage(
        final int spacing,
        final String image
    ) throws ExecStoppedException, ExecIOException {
        LogManager.logInfo("SymbiontFileWriter", "writeDataImage:%d,'%s'", spacing, image);// TODO remove
        ArraySlice dataBuffer = null;
        if (_currentCharacterSet == 0) {
            // fieldata
            var limit = Math.min(image.length(), 2047 * 6);
            dataBuffer = ArraySlice.stringToWord36Fieldata(image.substring(0, limit));
        } else {
            // ascii
            var limit = Math.min(image.length(), 2047 * 4);
            dataBuffer = ArraySlice.stringToWord36ASCII(image.substring(0, limit));
        }

        var dataLen = dataBuffer._array.length;
        if (dataLen + 1 <= _bufferRemaining) {
            var controlWord = ((long) dataLen) << 24;
            controlWord |= (long) spacing << 12;
            controlWord |= _currentCharacterSet & 077;
            writeWord(controlWord);
            writeWords(dataBuffer._array, 0, dataLen);
        } else {
            var subDataLen = _bufferRemaining;
            var controlWord = ((long) subDataLen) << 24;
            controlWord |= (long) spacing << 12;
            controlWord |= _currentCharacterSet & 077;
            writeWord(controlWord);
            writeWords(dataBuffer._array, 0, subDataLen);

            var offset = subDataLen;
            while (offset < dataLen) {
                subDataLen = Math.min(Math.min(dataLen - offset, (_bufferRemaining - 1)), 63);
                controlWord = ((long)051) << 30;
                controlWord |= ((long)subDataLen) << 24;
                controlWord |= _currentCharacterSet & 077;
                writeWord(controlWord);
                writeWords(dataBuffer._array, offset, subDataLen);
                offset += subDataLen;
            }
        }
    }

    private void writeBuffer() throws ExecStoppedException, ExecIOException {
        // TODO remove begin
        _buffer.logMultiFormat(Level.Info, "SymbiontFileWriter", "buffer");
        // TODO remove end
        var exec = Exec.getInstance();
        var fm = exec.getFacilitiesManager();
        var ioResult = new IOResult();
        fm.ioWriteToDiskFile(exec,
                             _internalFileName,
                             _nextAddress,
                             _buffer,
                             false,
                             ioResult);
        if (ioResult.getStatus() != ERIO$Status.Success) {
            exec.sendExecReadOnlyMessage("Image output canceled", ConsoleType.System);
            throw new ExecIOException(ioResult.getStatus());
        }

        _bufferIndex = 0;
        _bufferRemaining = _buffer.getSize();
    }

    private void writeWord(
        final long word
    ) throws ExecStoppedException, ExecIOException {
        LogManager.logInfo("SymbiontFileWriter", "writeWord:%012o", word);// TODO remove
        if (_bufferRemaining == 0) {
            writeBuffer();
        }
        _buffer.set(_bufferIndex++, word);
        _bufferRemaining--;
    }

    private void writeWords(
        final long[] words,
        final int offset,
        final int count
    ) throws ExecStoppedException, ExecIOException {
        LogManager.logInfo("SymbiontFileWriter", "writeWords offset=%d count=%d", offset, count);// TODO remove
        var wx = offset;
        var remaining = count;
        while (remaining > 0) {
            writeWord(words[wx++]);
            remaining--;
        }
    }
}
