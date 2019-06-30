/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import java.io.BufferedWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Class which models an IO Processor node
 */
public class InputOutputProcessor extends Processor {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested enumerations
    //  ----------------------------------------------------------------------------------------------------------------------------


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested classes
    //  ----------------------------------------------------------------------------------------------------------------------------


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Class attributes
    //  ----------------------------------------------------------------------------------------------------------------------------

    private static final Logger LOGGER = LogManager.getLogger(InputOutputProcessor.class);


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructors
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Constructor
     * <p>
     * @param name
     * @param upi unique identifier for this processor
     */
    public InputOutputProcessor(
        final String name,
        final short upi
    ) {
        super(Processor.ProcessorType.InputOutputProcessor, name, upi);
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Accessors
    //  ----------------------------------------------------------------------------------------------------------------------------


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Abstract methods
    //  ----------------------------------------------------------------------------------------------------------------------------


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Instance methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    //  cancelIo()
    //
    //  Informs IOProcessor (and by extension, the channel program) that we are taking back the
    //  channel program object and its buffer, and it should no longer be updated.
    //
    //  Used in situations where the exec is shutting down, or the caller got tired of waiting.
    //????
//    bool
//    IOProcessor::cancelIO
//    (
//        ChannelModule::ChannelProgram* const  pChannelProgram
//    )
//    {
//        auto itn = m_Descendants.find( pChannelProgram->m_ChannelModuleAddress );
//        if ( itn == m_Descendants.end() )
//            return false;
//
//        ChannelModule* pcm = dynamic_cast<ChannelModule*>( itn->second );
//        return pcm->cancelIO( pChannelProgram );
//    }

    /**
     * We are top-level - there are no ancesors for us
     * <p>
     * @param ancestor
     * @return
     */
    @Override
    public boolean canConnect(
        final Node ancestor
    ) {
        return false;
    }

    /**
     * For debugging purposes
     * <p>
     * @param writer
     */
    @Override
    public void dump(
        final BufferedWriter writer
    ) {
        super.dump(writer);
    }

    /**
     * Nop override
     */
    @Override
    public void initialize() {}

    //  routeIo()
    //
    //  Routes a channel program to the appropriate channel module
    //????
//    public void routeIO(
//        // ChannelProgram
//    ) {
//        auto itn = m_Descendants.find( pChannelProgram->m_ChannelModuleAddress );
//        if ( itn == m_Descendants.end() )
//        {
//            pChannelProgram->m_ChannelStatus = ChannelModule::Status::InvalidChannelModuleAddress;
//            if ( pChannelProgram->m_pSource != 0 )
//                pChannelProgram->m_pSource->workerSignal();
//        }
//        else
//        {
//            pChannelProgram->m_ChannelStatus = ChannelModule::Status::InProgress;
//            ChannelModule* pcm = dynamic_cast<ChannelModule*>( itn->second );
//            pcm->handleIO( pChannelProgram );
//        }
//    }

    /**
     * Invoked when this object is the source of an IO which has been cancelled or completed
     * <p>
     * @param source the Node which is signalling us
     */
    @Override
    public void signal(
        final Node source
    ) {
        //  nothing to do
    }

    /**
     * Invoked just before tearing down the configuration.
     */
    @Override
    public void terminate() {}


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Static methods
    //  ----------------------------------------------------------------------------------------------------------------------------

}
