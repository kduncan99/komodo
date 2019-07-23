/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import java.io.BufferedWriter;
import java.io.IOException;

import com.kadware.komodo.baselib.ArraySlice;
import com.kadware.komodo.baselib.PrepFactor;
import com.kadware.komodo.baselib.Word36;
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

    /**
     * IO counters for this device - they are to be kept updated by the various device terminal subclasses.
     * TODO:Should a RESET function clear these?
     */
    long _miscCount = 0;        //  anything non-read/write
    long _readBytes = 0;        //  total bytes transferred from storage
    long _readCount = 0;
    long _writeBytes = 0;       //  total bytes transferred to storage
    long _writeCount = 0;


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
            writer.write(String.format("  Type:            %s\n", _deviceType.toString()));
            writer.write(String.format("  Model:           %s\n", _deviceModel.toString()));
            writer.write(String.format("  Ready:           %s\n", String.valueOf(_readyFlag)));
            writer.write(String.format("  Unit Attention:  %s\n", String.valueOf(_unitAttentionFlag)));
            writer.write(String.format("  Misc IO Count:   %d", _miscCount));
            writer.write(String.format("  Read IO Count:   %d", _readCount));
            writer.write(String.format("  Read Bytes:      %d", _readBytes));
            writer.write(String.format("  Write IO Count:  %d", _writeCount));
            writer.write(String.format("  Write Bytes:     %d", _writeBytes));
        } catch (IOException ex) {
            LOGGER.catching(ex);
        }
    }

    /**
     * Produces an array slice which represents, in 36-bit mode, the device info data which is presented to the requestor.
     * Subclasses should call here first to get an ArraySlice with the basic info, add more info, and return the whole thing.
     * The format of the basic info block is:
     *      +----------+----------+----------+----------+----------+----------+
     *  +0  |  FLAGS   |  MODEL   |   TYPE   |             zeroes             |
     *      +----------+----------+----------+----------+----------+----------+
     *  +1  |                          MISC_IO_COUNT                          |
     *      +----------+----------+----------+----------+----------+----------+
     *  +2  |                          READ_IO_COUNT                          |
     *      +----------+----------+----------+----------+----------+----------+
     *  +3  |                          WRITE_IO_COUNT                         |
     *      +----------+----------+----------+----------+----------+----------+
     *  +4  |                          READ_IO_BYTES                          |
     *      +----------+----------+----------+----------+----------+----------+
     *  +5  |                          WRITE_IO_BYTES                         |
     *      +----------+----------+----------+----------+----------+----------+
     *  +6  |                                                                 |
     *  ..  |                      device-specific data                       |
     * +27  |                                                                 |
     *      +----------+----------+----------+----------+----------+----------+
     *
     * FLAGS:
     *      Bit 0:  device_ready
     * MODEL:       integer code for the DeviceModel
     * TYPE:        integer code for the DeviceType
     */
    protected ArraySlice getInfo() {
        long[] buffer = new long[28];
        buffer[0] = Word36.setS1(buffer[0], _readyFlag ? 040 : 0);
        buffer[0] = Word36.setS2(buffer[0], _deviceModel.getCode());
        buffer[0] = Word36.setS3(buffer[0], _deviceType.getCode());
        buffer[1] = _miscCount;
        buffer[2] = _readCount;
        buffer[3] = _writeCount;
        buffer[4] = _readBytes;
        buffer[5] = _writeBytes;
        return new ArraySlice(buffer);
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
