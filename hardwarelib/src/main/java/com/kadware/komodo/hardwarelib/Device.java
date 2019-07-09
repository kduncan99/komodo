/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import java.io.BufferedWriter;
import java.io.IOException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Abstract base class for a device such as a disk or tape unit
 * Devices are child Nodes of ChannelModules, and are the lowest units in the IO chain.
 * All IO to Devices is done asynchronously (as far as we are concerned at this level).
 * The ChannelModule sends a DeviceIOInfo object to the Device.
 * If the IO cannot be scheduled, the DeviceStatus field of the DeviceIOInfo is updated immediately, and the IO is rejected.
 * For IOs which are asynchronous at the system level, the IO is performed, DeviceStatus is updated,
 *   a signal is sent to the sender, and the IO is considered complete.
 * For IOs which are synchronous at the system level, the IO is scheduled, DeviceStatus is updated, and the IO is accepted.
 * When the IO completes at the system level, a signal is sent to the sender.
 * Senders must preserve DeviceIOInfo objects in memory until the Device marks the DeviceStatus field with some
 *   value indicating that the IO is complete, rejected, or has failed.
 * Actual system IO may involve invoking the host system's asynchronous IO facility.
 */
@SuppressWarnings("Duplicates")
public abstract class Device extends Node {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Class attributes
    //  ----------------------------------------------------------------------------------------------------------------------------

    private static final Logger LOGGER = LogManager.getLogger(Device.class);

    final DeviceModel _deviceModel;
    final DeviceType _deviceType;

    /**
     * Indicates the device can do reads and writes.
     * If not ready, the device *may* respond to non-read/write IOs.
     */
    boolean _readyFlag;

    /**
     * Indicates that something regarding the physical characteristics of this device has changed.
     * Setting a device ready should ALWAYS set this flag.
     * All reads and writes will be rejected with DeviceStatus.UnitAttention until an IOFunction.GetInfo is issued.
     */
    boolean _unitAttentionFlag;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructors
    //  ----------------------------------------------------------------------------------------------------------------------------

    protected Device(
        final DeviceType deviceType,
        final DeviceModel deviceModel,
        final String name
    ) {
        super(NodeCategory.Device, name);
        _deviceModel = deviceModel;
        _deviceType = deviceType;
        _readyFlag = false;
        _unitAttentionFlag = false;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Abstract methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Indicates whether this Node can connect as a descendant to the candidate ancestor Node.
     * This is an override of Node.canConnect(), and only appears here as an abstract for documentary purposes.
     */
    @Override
    public abstract boolean canConnect(final Node candidate);

    /**
     * Invoked when a new session is started by the system processor.
     * Any classes which have work to do to enter the cleared state should override this.
     */
    @Override
    public void clear() {}

    /**
     * IO Router - what happens here is depending upon the subclass
     * If we return false, it means we've completed the IO - there will be no scheduling or notification of completion
     */
    public abstract boolean handleIo(DeviceIOInfo ioInfo);

    /**
     * Does this device have a byte interface?
     */
    public abstract boolean hasByteInterface();

    /**
     * Does this device have a word interface?
     */
    public abstract boolean hasWordInterface();

    /**
     * Initializes the subclass if/as necessary
     */
    @Override
    public abstract void initialize();

    /**
     * Invoked just before tearing down the configuration.
     */
    @Override
    public abstract void terminate();

    /**
     * Writes IO buffers to the log.
     * What actually happens is dependant upon the particulars of the various subclasses, so this is abstract
     * @param ioInfo describes the IO buffer(s)
     */
    protected abstract void writeBuffersToLog(DeviceIOInfo ioInfo);


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Instance methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * For debugging purposes
     */
    @Override
    public void dump(
        final BufferedWriter writer
    ) {
        super.dump(writer);
        try {
            writer.write(String.format("  Type:%s  Model:%s\n",
                                       String.valueOf(_deviceType.toString()),
                                       String.valueOf(_deviceModel.toString())));

            writer.write(String.format("  Ready:%s  UnitAttn:%s\n",
                                       String.valueOf(_readyFlag),
                                       String.valueOf(_unitAttentionFlag)));
        } catch (IOException ex) {
            LOGGER.catching(ex);
        }
    }

    /**
     * Subclasses must call here at the end of handling an IO
     */
    void ioEnd(
        final DeviceIOInfo ioInfo
    ) {
        if (LOG_IO_ERRORS) {
            if ((ioInfo._status != DeviceStatus.Successful) && (ioInfo._status != DeviceStatus.NoInput)) {
                LOGGER.error(String.format("IoError:%s", ioInfo.toString()));
            }
        }

        if (LOG_DEVICE_IO_BUFFERS) {
            if (ioInfo._ioFunction.isReadFunction() && (ioInfo._status == DeviceStatus.Successful)) {
                writeBuffersToLog(ioInfo);
            }
        }
    }

    /**
     * Subclasses must call this at the beginning of handling an IO.
     */
    void ioStart(
        final DeviceIOInfo ioInfo
    ) {
        if (LOG_DEVICE_IOS) {
            LOGGER.debug(String.format("IoStart:%s", ioInfo.toString()));
        }

        if (LOG_DEVICE_IO_BUFFERS) {
            if (ioInfo._ioFunction.isWriteFunction()) {
                writeBuffersToLog(ioInfo);
            }
        }
    }

    /**
     * Sets the device ready or not-ready, depending upon the given state parameter.
     * Subclasses may override this to do more specific stuff (such as verifying that the state can be set as request),
     * but at some point they must call back here to actually effect the state change.
     * @param state true to set ready, false to set not-ready
     * @return true if successful
     */
    boolean setReady(
        final boolean state
    ) {
        _readyFlag = state;
        LOGGER.info(String.format("Device %s Ready %s", _name, state ? "Set" : "Cleared"));
        setUnitAttention(state);
        return true;
    }

    /**
     * Sets the unit attention flag (or clears it)
     */
    void setUnitAttention(
        final boolean state
    ) {
        _unitAttentionFlag = state;
        LOGGER.info(String.format("Device %s Unit Attention %s", _name, state ? "Set" : "Cleared"));
    }
}
