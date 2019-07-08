/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import java.io.BufferedWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Class which models an IO Processor node.
 * An IO is presented to an IO processor in one of two ways:
 *      1) A UPI is sent to the IOP with the absolute address of an IO packet.  TODO
 *      2) startIO() is invoked, with the absolute address of an IO packet as a parameter.
 * In either case, the IO is routed directly to the channel module object in question.
 * If the channel module index is wrong, the IO is rejected immediately.
 * The channel module may also reject the IO immediately, for any number of reasons including a bad device number.
 * If the IO can be scheduled, it is scheduled on the channel module's IO queue, and the status of the given
 * channel program (see below) is set to in-progress.
 * At some point, the channel module picks up the IO and does various things with it (hopefully sending it on
 * to the targeted device).
 * When the IO is complete, the device notifies the channel module which then notifies the IOP.
 * The IOP will then either SEND to the processor complex resulting in one IP getting a UPI NORMAL interrupt,
 * or it will set up the interrupt directly on an IP.
 * When the IP interrupt is scheduled, the SEND is ACK'd, and the IOP forgets the IO.
 */
@SuppressWarnings("Duplicates")
public class InputOutputProcessor extends Processor {

    private static final Logger LOGGER = LogManager.getLogger(InputOutputProcessor.class);

    /**
     * Constructor
     */
    public InputOutputProcessor(
        final String name,
        final int upi
    ) {
        super(ProcessorType.InputOutputProcessor, name, upi);
    }

    /**
     * We are top-level - there are no ancestors for us
     */
    @Override
    public boolean canConnect(Node ancestor) { return false; }

    /**
     * Nothing to do here - all clearing happens in our subordinate channel modules
     */
    @Override
    public void clear() {
        super.clear();
    }

    /**
     * For debugging purposes
     */
    @Override
    public void dump(BufferedWriter writer) { super.dump(writer); }

    /**
     * Nop override
     */
    @Override
    public void initialize() {}

    /**
     * Starts an IO as defined by the channel program at the given absolute address
     * @return true if the IO was scheduled, false if something immediately wrong was discovered
     */
    public boolean startIO(
        final ChannelProgram channelProgram
    ) {
        ChannelModule channelModule = (ChannelModule) _descendants.get(channelProgram.getChannelModuleIndex());
        if (channelModule == null) {
            channelProgram.setChannelStatus(ChannelStatus.UnconfiguredChannelModule);
            return false;
        }

        channelProgram.setChannelStatus(ChannelStatus.InProgress);
        channelModule.scheduleChannelProgram(channelProgram);
        return true;
    }

    /**
     * Invoked just before tearing down the configuration.
     */
    @Override
    public void terminate() {}

    /**
     * Handle UPI interrupt.  We should get these only from clients which are requesting IOs.
     * @param source processor initiating the interrupt
     * @param broadcast true if this is a broadcast
     */
    @Override
    public void upiHandleInterrupt(
        final Processor source,
        final boolean broadcast
    ) {
        if ((source._processorType == ProcessorType.InstructionProcessor)
            || (source._processorType == ProcessorType.SystemProcessor)) {
            //  Someone wants to do an IO.
            //TODO
        } else {
            //  This shouldn't happen.
            LOGGER.error(String.format("%s received a %s interrupt from %s",
                                       _name,
                                       broadcast ? "broadcast" : "directed",
                                       source._name));
        }
    }
}
