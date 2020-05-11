/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */


package com.kadware.komodo.kex;

import com.kadware.komodo.baselib.Word36;
import com.kadware.komodo.kex.exceptions.FileNotOpenException;
import com.kadware.komodo.kex.exceptions.ParameterException;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Represents a raw non-sparse data file - intended for storing things external to the emulator,
 * in a manner easily-translated into the komodo world.  In practice, this means we're simply
 * storing Word36 values bounded inside 64-bit longs, consecutively in a binary file,
 * accessible in 28-word consecutively id'd sectors.
 */
public class DataFile implements Closeable {

    static final int WORDS_PER_SECTOR = 28;
    static final int BYTES_PER_SECTOR = 8 * WORDS_PER_SECTOR;

    private final File _file;
    private RandomAccessFile _openFile;

    public DataFile(
        final File file
    ) {
        _file = file;
    }

    public DataFile(
        final String fileName
    ) {
        _file = new File(fileName);
    }

    public void clear(
    ) throws IOException {
        if (_openFile == null) {
            throw new FileNotOpenException(_file.getName());
        }

        _openFile.setLength(0);
    }

    public void close(
    ) throws IOException {
        if (_openFile == null) {
            throw new IOException("File not open");
        }

        _openFile.close();
        _openFile = null;
    }


    /**
     * Reports the highest sector identifier written - if the file is empty, we return -1
     */
    public long getHighestSectorWritten(
    ) throws IOException {
        if (_openFile == null) {
            throw new FileNotOpenException(_file.getName());
        }

        return (_openFile.length() / BYTES_PER_SECTOR) - 1;
    }
    public void open(
    ) throws IOException {
        if (_openFile != null) {
            throw new IOException("File already open");
        }

        _openFile = new RandomAccessFile(_file, "rw");
    }

    public Word36[] readSector(
        final long sectorId
    ) throws IOException {
        return readSectors(sectorId, 1);
    }

    public Word36[] readSectors(
        final long sectorId,
        final int sectorCount
    ) throws IOException {
        if (_openFile == null) {
            throw new FileNotOpenException(_file.getName());
        }

        long offset = sectorId * BYTES_PER_SECTOR;
        int length = sectorCount * BYTES_PER_SECTOR;
        byte[] byteBuffer = new byte[length];
        _openFile.seek(offset);
        _openFile.read(byteBuffer);

        Word36[] result = new Word36[sectorCount * WORDS_PER_SECTOR];
        for (int wx = 0, bx = 0; wx < result.length; ++wx, bx += 8) {
            long value = (((long)byteBuffer[bx + 3]) << 32)
                         | (((long)byteBuffer[bx + 4]) << 24)
                         | (((long)byteBuffer[bx + 5]) << 16)
                         | (((long)byteBuffer[bx + 6]) << 8)
                         | ((long)byteBuffer[bx + 7]);
            result[wx] = new Word36(value);
        }

        return result;
    }

    public void writeSector(
        final long sectorId,
        final Word36[] buffer
    ) throws IOException {
        writeSectors(sectorId, 1, buffer);
    }

    public void writeSectors(
        final long sectorId,
        final int sectorCount,
        final Word36[] buffer
    ) throws IOException {
        if (_openFile == null) {
            throw new FileNotOpenException(_file.getName());
        }

        long offset = sectorId * BYTES_PER_SECTOR;
        int length = sectorCount * BYTES_PER_SECTOR;
        int wordCount = sectorCount * WORDS_PER_SECTOR;
        if (buffer.length < wordCount) {
            throw new ParameterException("Buffer is too small");
        }

        byte[] byteBuffer = new byte[length];
        for (int wx = 0, bx= 0; wx < wordCount; ++wx, bx += 8) {
            long value = buffer[wx].getW();
            byteBuffer[0] = 0;
            byteBuffer[1] = 0;
            byteBuffer[2] = 0;
            byteBuffer[3] = (byte)((value >> 32) & 0x0f);
            byteBuffer[4] = (byte)(value >> 24);
            byteBuffer[5] = (byte)(value >> 16);
            byteBuffer[6] = (byte)(value >> 8);
            byteBuffer[7] = (byte)value;
        }

        _openFile.seek(offset);
        _openFile.write(byteBuffer);
    }
}
