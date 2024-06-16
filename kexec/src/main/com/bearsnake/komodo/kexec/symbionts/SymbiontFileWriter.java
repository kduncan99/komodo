/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.symbionts;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.kexec.SDFFileType;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exec.Exec;

public class SymbiontFileWriter implements SymbiontWriter {


    private final String _internalFileName;
    private final SDFFileType _fileType;
    private final ArraySlice _buffer;
    private long _nextAddress = 0;        // file-relative sector address of the next sector to be written
    private int _bufferIndex = 0;         // word index of the next word to be written to the buffer
    private int _bufferRemaining = 0;     // number of words not yet written to the buffer
    private int _currentCharacterSet;     // 0 is fieldata, we treat anything else as ASCII-like

    /**
     * For writing an SDF file
     * @param internalFileName internal file name of file containing the symbiont input element
     * @param fileType SDFFileType (probably PRINT$ or PUNCH$)
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
    ) throws ExecStoppedException {
        if (characterSet != _currentCharacterSet) {
            _currentCharacterSet = characterSet;
            var controlWord = ((long)042) << 30 | (characterSet & 077);
            writeWord(controlWord);
        }
    }

    /**
     * Writes a punch image to the output buffer (but with a safeguard in case we are a print file)
     * Splits the image on buffer boundaries.
     * @param image image to be converted and written
     * @throws ExecStoppedException If the exec stops in the middle of this process
     */
    @Override
    public void writeImage(
        final String image
    ) throws ExecStoppedException {
        writeImage(_fileType == SDFFileType.PRINT$ ? 1 : 0, image);
    }

    /**
     * Writes a print control image to the output buffer, converting according to the current character set.
     * Splits the image on buffer boundaries if/as necessary.
     * @param image image to be converted and written
     * @throws ExecStoppedException If the exec stops in the middle of this process
     */
    @Override
    public void writeControlImage(
        final String image
    ) throws ExecStoppedException {
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
     * Writes a print image to the output buffer, converting according to the current character set.
     * Splits the image on buffer boundaries if/as necessary.
     * @param spacing print spacing - leave 0 for punch files
     * @param image image to be converted and written
     * @throws ExecStoppedException If the exec stops in the middle of this process
     */
    @Override
    public void writeImage(
        final int spacing,
        final String image
    ) throws ExecStoppedException {
        var dataBuffer = (_currentCharacterSet == 0)
            ? ArraySlice.stringToWord36Fieldata(image.substring(0, 2047 * 6))
            : ArraySlice.stringToWord36ASCII(image.substring(0, 2047 * 4));

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

    private void writeBuffer() throws ExecStoppedException {
        var exec = Exec.getInstance();
        var fm = exec.getFacilitiesManager();
        fm.ioExecWriteToDiskFile(_internalFileName, _nextAddress, _buffer, _bufferIndex);
        _bufferIndex = 0;
        _bufferRemaining = _buffer.getSize();
    }

    private void writeWord(
        final long word
    ) throws ExecStoppedException {
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
    ) throws ExecStoppedException {
        var wx = offset;
        var remaining = count;
        while (remaining > 0) {
            writeWord(words[wx++]);
            remaining--;
        }
    }
}
