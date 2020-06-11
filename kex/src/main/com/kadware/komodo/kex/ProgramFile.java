/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex;

import com.kadware.komodo.baselib.Word36;
import java.io.File;
import java.io.IOException;

/**
 * Represents a program file stored natively in the host system.
 */
public class ProgramFile extends SparseDataFile {

    public ProgramFile (
        final File file
    ) {
        super(file);
    }

    public ProgramFile (
        final String fileName
    ) {
        super(fileName);
    }

    //  ----------------------------------------------------------------------------------------------------------------------------

    @Override
    public void close(
    ) throws IOException {
        //TODO if an element is open, close it
        super.close();
    }

    @Override
    public void open(
    ) throws IOException {
        super.open();
    }

    @Override
    public Word36[] readSector(
        final long sectorId
    ) throws IOException {
        return null;//TODO - do we really want to do this? prefer not allowing sectorId
    }

    @Override
    public Word36[] readSectors(
        final long sectorId,
        final int sectorCount
    ) throws IOException {
        return null;//TODO - do we really want to do this? prefer not allowing sectorId
    }

    @Override
    public void writeSector(
        final long sectorId,
        final Word36[] buffer
    ) throws IOException {
        //TODO - do we really want to do this? prefer not allowing sectorId
    }

    @Override
    public void writeSectors(
        final long sectorId,
        final int sectorCount,
        final Word36[] buffer
    ) throws IOException {
        //TODO - do we really want to do this? prefer not allowing sectorId
    }

    //  ----------------------------------------------------------------------------------------------------------------------------

    //TODO something to retrieve directory of element info objects?

    //TODO maybe should return the element info?
    public void createElement(
        final String elementName,
        final String elementVersion,
        final int elementType
    ) {
        //TODO
    }

    public void deleteElement(
        final String elementName,
        final String elementVersion,
        final int elementType
    ) {
        //TODO
    }

    //TODO maybe should return the element info?
    public void openElement(
        final String elementName,
        final String elementVersion,
        final int elementType
    ) {
        //TODO
    }

    public void pack() {
        //TODO
    }

    public void prep() {
        //TODO
    }

    public Word36[] readNextSector(
    ) throws IOException {
        return null;
    }

    public void recoverElement(
        final String elementName,
        final String elementVersion,
        final int elementType,
        final int elementCycle
    ) {
        //TODO
    }
}
