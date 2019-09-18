/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.ArraySlice;
import com.kadware.komodo.baselib.Word36;

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
 *      DEV_STS:                Device status word - see IOStatus class
 *                                  only valid if CHAN_STS indicates a device status is present
 *      RES_BYTES:              Number of bytes/frames in excess of a full word, written to or read
 *                                  i.e., at 4 bytes per word, an 11 byte transfer has a residual bytes value of 3
 *                                  only applies to byte transfers
 *      WORDS_TRANSFERRED:      Number of complete or partial words actually transferred
 *      ACCESS_CONTROL_WORDS:   See AccessControlWord class
 */
@SuppressWarnings("Duplicates")
public class ChannelProgram extends ArraySlice {

    /**
     * Constructor for builder
     */
    private ChannelProgram(
        final int iopUpiIndex,
        final int channelModuleIndex,
        final int deviceAddress,
        final Device.IOFunction ioFunction,
        final ByteTranslationFormat byteFormat,
        final Long blockId,
        final AccessControlWord[] accessControlWords
    ) {
        super(new long[4 + (accessControlWords != null ? 3 * accessControlWords.length : 0)]);
        setIopUpiIndex(iopUpiIndex);
        setChannelModuleIndex(channelModuleIndex);
        setDeviceAddress(deviceAddress);
        setIOFunction(ioFunction);

        if (byteFormat != null) {
            setByteTranslationFormat(byteFormat);
        }

        if (blockId != null) {
            setBlockId(blockId);
        }

        if (accessControlWords != null) {
            set(0, Word36.setS6(get(0), accessControlWords.length));
            int dx = _offset + 4;
            for (AccessControlWord acw : accessControlWords) {
                _array[dx++] = acw._array[acw._offset];
                _array[dx++] = acw._array[acw._offset + 1];
                _array[dx++] = acw._array[acw._offset + 2];
            }
        }
    }

    /**
     * Constructor from storage
     */
    public ChannelProgram (
        final ArraySlice baseSlice,
        final int offset,
        final int length
    ) {
        super(baseSlice, offset, length);
    }

    /**
     * Constructor from storage, which uses the number of ACWs in the channel program located
     * at the storage offset to determine how long the channel program should be.
     * @param baseSlice describes the general storage where the channel program exists
     * @param offset offset from the start of the storage where the channel program exists
     * @return new ChannelProgram array slice
     */
    public static ChannelProgram create(
        final ArraySlice baseSlice,
        final int offset
    ) {
        int acws = (int) baseSlice.get(offset) & 077;
        int len = 4 + (3 * acws);
        return new ChannelProgram(baseSlice, offset, len);
    }

    //  Getters
    public int getIopUpiIndex() { return (int) Word36.getS1(get(0)); }
    public int getChannelModuleIndex() { return (int) Word36.getS2(get(0)); }
    public int getDeviceAddress() { return (int) Word36.getS3(get(0)); }
    public Device.IOFunction getFunction() { return Device.IOFunction.getValue((int) Word36.getS4(get(0))); }
    public ByteTranslationFormat getByteTranslationFormat() { return ByteTranslationFormat.getValue((int) Word36.getS5(get(0))); }
    public int getAccessControlWordCount() { return (int) Word36.getS6(get(0)); }
    public long getBlockId() { return get(1); }
    public ChannelStatus getChannelStatus() { return ChannelStatus.getValue((int) Word36.getS1(get(2))); }
    public Device.IOStatus getDeviceStatus() { return Device.IOStatus.getValue((int) Word36.getS2(get(2))); }
    public int getResidualBytes() { return (int) Word36.getS3(get(2)); }
    public int getWordsTransferred() { return (int) Word36.getH2(get(3)); }

    public AccessControlWord getAccessControlWord(
        final int index
    ) {
        return new AccessControlWord(this, 4 + 3 * index);
    }

    //  Setters
    public void setIopUpiIndex(int value) { set(0, Word36.setS1(get(0), value)); }
    public void setChannelModuleIndex(int value) { set(0, Word36.setS2(get(0), value)); }
    public void setDeviceAddress(int value) { set(0, Word36.setS3(get(0), value)); }
    public void setIOFunction(Device.IOFunction func) { set(0, Word36.setS4(get(0), func.getCode())); }
    public void setByteTranslationFormat(ByteTranslationFormat fmt) { set(0, Word36.setS5(get(0), fmt.getCode())); }
    public void setBlockId(long addr) { set(1, addr & 0_777777_777777L); }
    public void setChannelStatus(ChannelStatus stat) { set(2, Word36.setS1(get(2), stat.getCode())); }
    public void setDeviceStatus(Device.IOStatus stat) { set(2, Word36.setS2(get(2), stat.getCode())); }
    public void setResidualBytes(int value) { set(2, Word36.setS3(get(2), value)); }
    public void setWordsTransferred(int value) { set(3, Word36.setH2(get(3), value)); }

    public int getCumulativeTransferWords() {
        int result = 0;
        for (int acwx = 0; acwx < getAccessControlWordCount(); ++acwx) {
            result += getAccessControlWord(acwx).getBufferSize();
        }

        return result;
    }

    public static class Builder {

        private Integer _iopUpiIndex = null;
        private Integer _channelModuleIndex = null;
        private Integer _deviceAddress = null;
        private Device.IOFunction _ioFunction = null;
        private ByteTranslationFormat _byteFormat = null;
        private Long _blockId = null;
        private AccessControlWord[] _acws = null;

        public Builder setIopUpiIndex(int value) { _iopUpiIndex = value; return this; }
        public Builder setChannelModuleIndex(int value) { _channelModuleIndex = value; return this; }
        public Builder setDeviceAddress(int value) { _deviceAddress = value; return this; }
        public Builder setIOFunction(Device.IOFunction func) { _ioFunction = func; return this; }
        public Builder setByteTranslationFormat(ByteTranslationFormat fmt) { _byteFormat = fmt; return this; }
        public Builder setBlockId(long id) { _blockId = id & 0_777777_777777L; return this; }
        public Builder setAccessControlWords(AccessControlWord[] acws) { _acws = acws; return this; }

        public ChannelProgram build() {
            assert(_iopUpiIndex != null);
            assert(_channelModuleIndex != null);
            assert(_deviceAddress != null);
            assert(_ioFunction != null);

            if (_ioFunction.isReadFunction()
                || (_ioFunction.isWriteFunction() && (_ioFunction != Device.IOFunction.WriteEndOfFile))) {
                assert(_acws != null);
            }

            return new ChannelProgram(_iopUpiIndex,
                                      _channelModuleIndex,
                                      _deviceAddress,
                                      _ioFunction,
                                      _byteFormat,
                                      _blockId,
                                      _acws);

        }
    }
}
