/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.symbionts;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.baselib.Word36;
import com.bearsnake.komodo.kexec.SDFFileType;
import com.bearsnake.komodo.kexec.SDFRecord;
import com.bearsnake.komodo.kexec.consoles.ConsoleType;
import com.bearsnake.komodo.kexec.exceptions.EndOfFileException;
import com.bearsnake.komodo.kexec.exceptions.ExecIOException;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exec.ERIO$Status;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.facilities.IOResult;

import java.util.Arrays;

/**
 * Assists the exec in handling READ$ IO specifically from an SDF file or element for a run.
 * (Generally not for an element, but it could happen I guess)
 */
public class SymbiontFileReader implements SymbiontReader {

    private final String _internalFileName;
    private final ArraySlice _buffer;
    private long _sectorsRemaining;       // number of sectors not yet read
    private long _nextAddress;            // file-relative sector address of the next sector to be read
    private int _bufferIndex = 0;         // word index of the next word to be taken from the buffer
    private int _bufferRemaining = 0;     // number of words yet to be taken from the buffer
    private boolean _characterSetValid = false;
    private int _currentCharacterSet = 0; // 0 is fieldata, we treat anything else as ASCII-like
    private SDFFileType _fileType = SDFFileType.Generic; // taken from 050 record, word 0 S3

    /**
     * For reading from a symbolic element (needs an initial sector address)...
     * This is unlikely to actually be useful, but it is here just in case.
     * Maybe for @ADD... ?
     * @param internalFileName internal file name of file containing the symbiont input element
     * @param initialSectorAddress sector address where the element text begins
     * @param sectorCount length of the element in sectors
     * @param bufferSize symbiont IO buffer size
     */
    public SymbiontFileReader(
        final String internalFileName,
        final long initialSectorAddress,
        final long sectorCount,
        final int bufferSize
    ) {
        _internalFileName = internalFileName;
        _sectorsRemaining = sectorCount;
        _buffer = new ArraySlice(new long[bufferSize]);
        _nextAddress = initialSectorAddress;
    }

    /**
     * For reading from an SDF file
     * @param internalFileName internal file name of symbiont file
     * @param sectorCount length of the file in sectors
     * @param bufferSize symbiont IO buffer size
     */
    public SymbiontFileReader(
        final String internalFileName,
        final long sectorCount,
        final int bufferSize
    ) {
        this(internalFileName, 0, sectorCount, bufferSize);
    }

    /**
     * Reads the next image from the symbiont file.
     * @return image if found, null if we've reached the end of the file
     * @throws ExecStoppedException in the case of IO error on the symbiont file
     * @throws EndOfFileException if there are no more data images to be read
     */
    @Override
    public String readImage() throws ExecStoppedException, EndOfFileException, ExecIOException {
        String result = null;
        while (result == null) {
            if (Exec.getInstance().isStopped()) {
                throw new ExecStoppedException();
            }

            var record = getNextSDFRecord();
            if (record.isControlRecord()) {
                switch (record.getControlRecordType()) {
                    case 042: // character set change
                        _characterSetValid = true;
                        _currentCharacterSet = (int)Word36.getS6(record.getControlWord());
                        break;

                    case 050: // label control record (first control record in the file)
                        _fileType = SDFFileType.getSDFFileType((int)Word36.getS3(record.getControlWord()));
                        _characterSetValid = true;
                        _currentCharacterSet = (int)Word36.getS6(record.getControlWord());
                        break;

                    case 077: // end of file
                        throw new EndOfFileException();
                }
            } else {
                var charSet = _currentCharacterSet;
                if (!_characterSetValid) {
                    if ((_fileType == SDFFileType.READ$)
                        || (_fileType == SDFFileType.PRINT$)
                        || (_fileType == SDFFileType.PUNCH$)
                        || (_fileType == SDFFileType.AT_FILE)) {
                        charSet = (int)Word36.getS6(record.getControlWord());
                    }
                }

                var sb = new StringBuilder();
                for (var dx = 0; dx < record.getData().length; dx++) {
                    if (charSet == 0) {
                        sb.append(Word36.toStringFromFieldata(record.getData()[dx]));
                    } else {
                        sb.append(Word36.toStringFromASCII(record.getData()[dx]));
                    }
                }
                result = sb.toString();
            }
        }
        
        return result;
    }

    /**
     * Reads the next SDF record from the input file
     * @return SDFRecord object describing the record read from the input file
     * @throws ExecStoppedException in case of an IO error
     * @throws EndOfFileException if we have exhausted the sector count
     */
    private SDFRecord getNextSDFRecord() throws ExecStoppedException, EndOfFileException, ExecIOException {
        var controlWord = getNextWord();
        var dataLength = (int)(Word36.isNegative(controlWord) ? Word36.getS2(controlWord) : Word36.getT1(controlWord));
        var data = getNextWords(dataLength);
        var record = new SDFRecord(controlWord, data);
        while (isNextImageContinuation()) {
            controlWord = getNextWord();
            dataLength = (int)Word36.getS2(controlWord);
            var currentLength = record.getData().length;
            record.setData(Arrays.copyOf(record.getData(), currentLength + dataLength));
            for (int wx = 0; wx < dataLength; wx++) {
                record.getData()[wx + currentLength] = getNextWord();
            }
        }
        return record;
    }

    /**
     * Retrieves the next word from the input file, reloading the buffer if necessary
     * and updating the buffer index and buffer remaining counter appropriately
     * @return the next word from the input file
     * @throws ExecStoppedException in case of an IO error
     * @throws EndOfFileException if we have exhausted the sector count
     */
    private long getNextWord() throws ExecStoppedException, EndOfFileException, ExecIOException {
        if (_bufferRemaining == 0) {
            loadBuffer();
        }
        _bufferRemaining--;
        return _buffer.get(_bufferIndex++);
    }

    /**
     * Retrieves a predetermined number of words from the input file,
     * reloading the buffer if and as necessary.
     * @param wordCount number of words to be read
     * @return array of 36-bit words from the file, wrapped as longs
     * @throws ExecStoppedException in case of an IO error
     * @throws EndOfFileException if we have exhausted the sector count
     */
    private long[] getNextWords(
        final int wordCount
    ) throws ExecStoppedException, EndOfFileException, ExecIOException {
        var result = new long[wordCount];
        for (int wx = 0; wx < wordCount; wx++) {
            result[wx] = getNextWord();
        }
        return result;
    }

    /**
     * Checks to see if the next image in the file is a continuation image
     * @return true if the next word in the file is a control word of type 051 (continuation record)
     * @throws ExecStoppedException in case of an IO error
     */
    private boolean isNextImageContinuation() throws ExecStoppedException {
        try {
            var controlWord = peekNextWord();
            return (Word36.getS1(controlWord) == 051);
        } catch (EndOfFileException | ExecIOException ex) {
            return false;
        }
    }

    /**
     * Loads the buffer from the file beginning with the first un-read sector.
     * @throws ExecStoppedException in case of an IO error
     * @throws EndOfFileException if we have exhausted the sector count
     */
    private void loadBuffer() throws ExecStoppedException, EndOfFileException, ExecIOException {
        // Read the next buffer
        if (_sectorsRemaining == 0) {
            throw new EndOfFileException();
        }

        var exec = Exec.getInstance();
        var fm = exec.getFacilitiesManager();
        var transferCount = (int)Math.min(_buffer.getSize(), _sectorsRemaining * 28);
        var ioResult = new IOResult();
        fm.ioReadFromDiskFile(exec, _internalFileName, _nextAddress, _buffer, transferCount, false, ioResult);
        if (ioResult.getStatus() != ERIO$Status.Success) {
            exec.sendExecReadOnlyMessage("Image input canceled", ConsoleType.System);
            throw new ExecIOException(ioResult.getStatus());
        }

        var sectorsTransferred = transferCount / 28;
        _sectorsRemaining -= sectorsTransferred;
        _nextAddress += sectorsTransferred;
        _bufferIndex = 0;
        _bufferRemaining = transferCount;
    }

    /**
     * Retrieves the next word without adjusting the buffer index or remaining counter
     * @return the next word in the buffer
     * @throws ExecStoppedException in case of an IO error
     * @throws EndOfFileException if we have exhausted the sector count
     */
    private long peekNextWord() throws ExecStoppedException, EndOfFileException, ExecIOException {
        if (_bufferRemaining == 0) {
            loadBuffer();
        }
        return _buffer.get(_bufferIndex);
    }
}
