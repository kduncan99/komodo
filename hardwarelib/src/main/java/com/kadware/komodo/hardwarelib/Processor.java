/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.exceptions.InternalErrorRuntimeException;
import java.io.BufferedWriter;
import java.io.IOException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Base class which models a Procesor node
 */
@SuppressWarnings("Duplicates")
public abstract class Processor extends Node {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Class attributes
    //  ----------------------------------------------------------------------------------------------------------------------------

    private static final Logger LOGGER = LogManager.getLogger(Processor.class);

    public final ProcessorType _processorType;

    /**
     * Uniquely identifies each processor in the configuration, in a manner transparent to the configurating entity.
     * Our own requirement is that the UPI must be greater than zero, and less than or equal to 15.
     * This is due to absolute address resolution for MSPs.
     */
    public final int _upi;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructors
    //  ----------------------------------------------------------------------------------------------------------------------------

    public Processor(
        final ProcessorType processorType,
        final String name,
        final int upi
    ) {
        super(NodeCategory.Processor, name);
        _processorType = processorType;
        _upi = upi;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Abstract methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Initializes this Processor if/as necessary
     */
    @Override
    public abstract void initialize();

    /**
     * Invoked just before tearing down the configuration.
     */
    @Override
    public abstract void terminate();


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Instance methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * For debugging purposes
     * @param writer where we write
     */
    @Override
    public void dump(
        final BufferedWriter writer
    ) {
        super.dump(writer);
        try {
            writer.write(String.format("  Type:%s\n", _processorType.toString()));
        } catch (IOException ex) {
            LOGGER.catching(ex);
        }
    }
}
