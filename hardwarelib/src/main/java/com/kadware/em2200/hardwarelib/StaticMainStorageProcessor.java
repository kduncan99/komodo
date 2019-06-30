/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib;

import com.kadware.komodo.baselib.ArraySlice;
import com.kadware.em2200.baselib.exceptions.*;
import com.kadware.em2200.hardwarelib.interrupts.AddressingExceptionInterrupt;
import java.io.BufferedWriter;
import java.io.IOException;

import com.kadware.komodo.baselib.exceptions.InvalidArgumentRuntimeException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Class which models a static Main Storage Processor.
 * Such an MSP contains one fixed-size segment.  The operating system must manage allocation of memory in
 * whatever size chunks are necessary.
 */
public class StaticMainStorageProcessor extends MainStorageProcessor {

    private static final Logger LOGGER = LogManager.getLogger(StaticMainStorageProcessor.class);
    private final ArraySlice _storage;      //  keep this private so we can control what goes into it

    /**
     * constructor
     * @param name node name of the MSP
     * @param upi UPI for the MSP
     * @param sizeInWords size of the MSP in words
     */
    public StaticMainStorageProcessor(
        final String name,
        final short upi,
        final int sizeInWords
    ) {
        super(name, upi);

        if (sizeInWords <= 0) {
            throw new InvalidArgumentRuntimeException(String.format("Bad size for MSP:%d words", sizeInWords));
        }

        _storage = new ArraySlice(new long[sizeInWords]);
    }

    /**
     * Getter
     * @return value
     */
    @Override
    public ArraySlice getStorage(
        final int segmentIndex
    ) throws AddressingExceptionInterrupt {
        if (segmentIndex != 0) {
            throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.FatalAddressingException, 0, 0);
        }
        return _storage;
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
            writer.write(String.format("  Storage: %d words\n", _storage.getSize()));
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
