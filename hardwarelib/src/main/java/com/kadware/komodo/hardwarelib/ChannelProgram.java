/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.ArraySlice;
import com.kadware.komodo.baselib.BlockId;
import com.kadware.komodo.baselib.Word36;
import com.kadware.komodo.hardwarelib.exceptions.UPINotAssignedException;
import com.kadware.komodo.hardwarelib.exceptions.UPIProcessorTypeException;
import com.kadware.komodo.hardwarelib.interrupts.AddressingExceptionInterrupt;

/**
 * Sort of a channel program.
 * Essentially it is a packet in storage which describes a requested IO.
 *
 * The channel program is formatted as follows:
 *      +----------+----------+----------+----------+----------+----------+
 * +00: | IOP UPI  |  CHMOD   |  DEVNUM  | FUNCTION |  FORMAT  |  #ACWS   |
 *      +----------+----------+----------+----------+----------+----------+
 * +01: |                      DEVICE_BLOCK_ADDRESS                       |
 *      +----------+----------+----------+----------+----------+----------+
 * +02: | CHAN_STS | DEV_STS  |RES_BYTES |            reserved            |
 *      +----------+----------+----------+----------+----------+----------+
 * +03: |            reserved            |       WORDS_TRANSFERRED        |
 *      +----------+----------+----------+----------+----------+----------+
 * +04: |                {n} 3-WORD ACCESS_CONTROL_WORDS                  |
 *      |                           (see +0 S6)                           |
 *      +----------+----------+----------+----------+----------+----------+
 *
 *      IOP UPI:                UPI number of IOP which should process this request
 *                                  (mostly ignored, as this is already being handled by the IOP in question)
 *      CHMOD:                  Index of channel module relative to the IOP
 *      DEVNUM:                 Device number of the device relative to the  channel module
 *      FUNCTION:               See IOFunction class
 *      FORMAT:                 Only for byte transfers
 *                                  00: Format A, quarter word -> byte, high bit not transferred
 *                                      if high bit is set on input, transfer ends
 *                                  01: Format B, sixth word -> byte
 *                                  02: Format C, 2 words unpacked into 9 bytes
 *                                  03: Format D, same as A except high bit is always ignored
 *      #ACS:                   Number of access control words present in the packet
 *      DEVICE_BLOCK_ADDRESS:   For disk IO - starting block number of the IO
 *      CHAN_STS:               Channel status word - see ChannelStatus class
 *      DEV_STS:                Device status word - see DeviceStatus class
 *                                  only valid if CHAN_STS indicates a device status is present
 *      RES_BYTES:              Number of bytes/frames in excess of a full word, written to or read
 *                                  i.e., at 4 bytes per word, an 11 byte transfer has a residual bytes value of 3
 *                                  only applies to byte transfers
 *      WORDS_TRANSFERRED:      Number of complete or partial words actually transferred
 *      ACCESS_CONTROL_WORDS:   See AccessControlWord class
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

    /**
     * For write operations - creates a temporary contiguous buffer comprised of the individual
     * parts of buffers represented by the ACWs.
     */
    public ArraySlice createContiguousBuffer(
    ) throws AddressingExceptionInterrupt,
             UPINotAssignedException,
             UPIProcessorTypeException {
        long[] destination = new long[getCumulativeTransferWords()];
        int destx = 0;
        for (int acwx = 0; acwx < getAccessControlWordCount(); ++acwx) {
            AccessControlWord acw = getAccessControlWord(acwx);
            MainStorageProcessor msp = InventoryManager.getInstance().getMainStorageProcessor(acw.getBufferAddress()._upiIndex);
            ArraySlice source = msp.getStorage(acw.getBufferAddress()._segment);

            int increment = 0;
            if (acw.getAddressModifier() == AccessControlWord.AddressModifier.Increment) {
                increment = 1;
            } else if (acw.getAddressModifier() == AccessControlWord.AddressModifier.Decrement) {
                increment = -1;
            }

            for (int sc = 0, srcx = source._offset + acw.getBufferAddress()._offset;
                 sc < acw.getBufferSize();
                 ++sc, srcx += increment) {
                if ((srcx < 0) || (srcx > source._length)) {
                    throw new AddressingExceptionInterrupt(AddressingExceptionInterrupt.Reason.FatalAddressingException,
                                                           0,
                                                           0);
                }
                destination[++destx] = source._array[srcx];
            }
        }

        return new ArraySlice(destination);
    }

    //  Getters
    public int getIopUpiIndex() { return (int) Word36.getS1(get(0)); }
    public int getChannelModuleIndex() { return (int) Word36.getS2(get(0)); }
    public int getDeviceAddress() { return (int) Word36.getS3(get(0)); }
    public IOFunction getFunction() { return IOFunction.getValue((int) Word36.getS4(get(0))); }
    public ByteTranslationFormat getByteTranslationFormat() { return ByteTranslationFormat.getValue((int) Word36.getS5(get(0))); }
    public int getAccessControlWordCount() { return (int) Word36.getS6(get(0)); }
    public BlockId getBlockAddress() { return new BlockId(get(1)); }
    public int getChannelStatus() { return (int) Word36.getS1(get(2)); }
    public int getDeviceStatus() { return (int) Word36.getS2(get(2)); }
    public int getResidualBytes() { return (int) Word36.getS3(get(2)); }
    public int getWordsTransferred() { return (int) Word36.getH2(get(3)); }

    public AccessControlWord getAccessControlWord(
        final int index
    ) {
        return new AccessControlWord(this, 4 + 3 * index);
    }

    //  Setters
    public void setChannelStatus(ChannelStatus value) { Word36.setS1(get(2), value.getCode()); }
    public void setDeviceStatus(DeviceStatus value) { Word36.setS2(get(2), value.getCode()); }
    public void setResidualBytes(int value) { Word36.setS3(get(2), value); }
    public void setWordsTransferred(int value) { Word36.setH2(get(3), value); }

    public int getCumulativeTransferWords() {
        int result = 0;
        for (int acwx = 0; acwx < getAccessControlWordCount(); ++acwx) {
            result += getAccessControlWord(acwx).getBufferSize();
        }

        return result;
    }
}
