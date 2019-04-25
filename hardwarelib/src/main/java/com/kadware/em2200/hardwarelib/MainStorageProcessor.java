/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib;

import com.kadware.em2200.baselib.exceptions.*;
import com.kadware.em2200.baselib.Word36Array;
import java.io.BufferedWriter;
import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Class which models a Main Storage Processor.
 * An MSP contains the main storage, or memory, for the emulated mainframe complex.
 * There must be at least one and there may be more depending upon the size of the host's memory and the number of other
 * Processors in the configuration (there must be at least one IP and one IOP as well).
 * Max size for any single MSP is 2GB, although smaller sizes should work fine.
 *
 * The rest of the processing complex deals with various types of addresses down to absolute addresses.
 * Such absolute addresses are formatted as 36-bit words, with the top 4 bits containing the UPI of a particular MSP
 * and the bottom 32 bits containing the signed offset from the start of that MSP's memory.
 */
public class MainStorageProcessor extends Processor {

    private static final Logger LOGGER = LogManager.getLogger(MainStorageProcessor.class);
    private final Word36Array _storage; //  keep this private so we can control what goes into it

    /**
     * constructor
     * @param name node name of the MSP
     * @param upi UPI for the MSP
     * @param sizeInWords size of the MSP in words
     */
    public MainStorageProcessor(
        final String name,
        final short upi,
        final int sizeInWords
    ) {
        super(Processor.ProcessorType.MainStorageProcessor, name, upi);

        if (sizeInWords <= 0) {
            throw new InvalidArgumentRuntimeException(String.format("Bad size for MSP:%d words", sizeInWords));
        }

        _storage = new Word36Array(sizeInWords);
    }

    /**
     * Getter
     * @return value
     */
    public Word36Array getStorage(
    ) {
        return _storage;
    }

    /**
     * MSPs have no ancestors
     * @param ancestor candidate ancestor
     * @return always false
     */
    @Override
    public boolean canConnect(
        final Node ancestor
    ) {
        return false;
    }

    /**
     * For debugging
     * @param writer destination for output
     */
    @Override
    public void dump(
        final BufferedWriter writer
    ) {
        super.dump(writer);
        try {
            writer.write(String.format("  Storage: %d words\n", _storage.getArraySize()));
        } catch (IOException ex) {
            LOGGER.catching(ex);
        }
    }

    /**
     * Invoked when configuration is presented - does nothing
     */
    @Override
    public void initialize() {}

    /**
     * Invoked when this object is the source of an IO which has been cancelled or completed
     * @param source the Node which is signalling us
     */
    @Override
    public void signal(
        final Node source
    ) {
        //  nothing to do
    }

    /**
     * Invoked just before tearing down the configuration - does nothing
     */
    @Override
    public void terminate() {}
}
