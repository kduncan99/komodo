/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.ArraySlice;
import com.kadware.komodo.baselib.types.BlockId;
import com.kadware.komodo.baselib.Word36;

/**
 * Sort of a channel program.
 * Essentially it is a packet in storage which describes a requested IO.
 *
 * The channel program is formatted as follows:
 *      +----------+----------+----------+----------+----------+----------+
 * +00: | IOP UPI  |  CHMOD   |  DEVNUM  | FUNCTION |          |  #ACWS   |
 *      +----------+----------+----------+----------+----------+----------+
 * +01: |                      DEVICE_BLOCK_ADDRESS                       |
 *      +----------+----------+----------+----------+----------+----------+
 * +02: | CHAN_STS | DEV_STS  |RES_BYTES |           RES_WORDS            |
 *      +----------+----------+----------+----------+----------+----------+
 * +03: |        WORDS_REQUESTED         |       WORDS_TRANSFERRED        |
 *      +----------+----------+----------+----------+----------+----------+
 * +04: |                {n} 3-word Access Control Words                  |
 *      |                           (see +0 S6)                           |
 *      +----------+----------+----------+----------+----------+----------+
 *
 *      IOP UPI:                UPI number of IOP which should process this request
 *                                  (mostly ignored, as this is already received by the IOP in question)
 *      CHMOD:                  Index of channel module relative to the IOP
 *      DEVNUM:                 Device number of the device relative to the  channel module
 *      FUNCTION:               See IOFunction class
 *      #ACS:                   Number of access control words present in the packet
 *      DEVICE_BLOCK_ADDRESS:   Mostly for disk IO - starting block number of the IO
 *      CHAN_STS:               Channel status word - see ChannelStatus class
 *      DEV_STS:                Device status word - see DeviceStatus class
 *                                  only valid if CHAN_STS indicates a device status is present
 *      RES_BYTES:              Number of bytes not written to or read from the target to
 *                                  represent a full word
 *      RES_WORD:               Difference between IO size indicated and words actually transferred
 *      WORDS_REQUESTED:        Number of words the caller wished to read/write
 *                                  This should correspond to the sum of all the buffer sizes representd
 *                                  in the ACW list
 *      WORDS_TRANSFERRED:      Number of complete or partial words actually transferred
 *
 * ACW format: see AccessControlWord object
 */
@SuppressWarnings("Duplicates")
public class ChannelProgram extends ArraySlice {

    public ChannelProgram (
        final ArraySlice baseSlice,
        final int offset,
        final int length
    ) {
        super(baseSlice, offset, length);
    }

    public int getIopUpi() { return (int) Word36.getS1(get(0)); }
    public int getChannelModuleIndex() { return (int) Word36.getS2(get(0)); }
    public int getDeviceAddress() { return (int) Word36.getS3(get(0)); }
    public IOFunction getFunction() { return IOFunction.getValue((int) Word36.getS4(get(0))); }
    public int getAccessControlWordCount() { return (int) Word36.getS6(get(0)); }
    public BlockId getBlockAddress() { return new BlockId(get(1)); }
    public int getWordsRequested() { return (int) Word36.getH1(get(3)); }

    public AccessControlWord getAccessControlWord(
        final int index
    ) {
        return new AccessControlWord(this, 4 + 3 * index);
    }

    public void setChannelStatus(ChannelStatus value) { Word36.setS1(get(2), value.getCode()); }
    public void setDeviceStatus(DeviceStatus value) { Word36.setS2(get(2), value.getCode()); }
    public void setResidueBytes(int value) { Word36.setS3(get(2), value); }
    public void setResidueWords(int value) { Word36.setH2(get(2), value); }
    public void setWordsTransferred(int value) { Word36.setH2(get(3), value); }
}
