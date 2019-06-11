/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib;

import com.kadware.em2200.baselib.ArraySlice;
import com.kadware.em2200.hardwarelib.interrupts.AddressingExceptionInterrupt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.BufferedWriter;

/**
 * Class which models a Main Storage Processor.
 * An MSP contains the main storage, or memory, for the emulated mainframe complex.
 * There must be at least one and there may be more depending upon the size of the host's memory and the number of other
 * Processors in the configuration (there must be at least one IP and one IOP as well).
 * Max size for any single MSP segment is 2GB, although smaller sizes should work fine.
 ** The rest of the processing complex deals with various types of addresses down to absolute addresses.
 */
public abstract class MainStorageProcessor extends Processor {

    private static final Logger LOGGER = LogManager.getLogger(MainStorageProcessor.class);

    /**
     * constructor
     * @param name node name of the MSP
     * @param upi UPI for the MSP
     */
    MainStorageProcessor(
        final String name,
        final short upi
    ) {
        super(ProcessorType.MainStorageProcessor, name, upi);
    }

    /**
     * Getter to retrieve the full storage for a segment
     * @return Word36Array representing the storage for the MSP
     */
    public abstract ArraySlice getStorage(final int segmentIndex) throws AddressingExceptionInterrupt;

    /**
     * MSPs have no ancestors
     * @param ancestor candidate ancestor
     * @return always false
     */
    @Override
    public final boolean canConnect(
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
    }

    /**
     * Invoked when configuration is presented
     */
    @Override
    public abstract void initialize();

    /**
     * Invoked when this object is the source of an IO which has been cancelled or completed
     * @param source the Node which is signalling us
     */
    @Override
    public abstract void signal(final Node source);

    /**
     * Invoked just before tearing down the configuration
     */
    @Override
    public abstract void terminate();
}
