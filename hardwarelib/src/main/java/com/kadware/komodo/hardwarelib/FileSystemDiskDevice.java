/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.*;
import com.kadware.komodo.hardwarelib.exceptions.InvalidBlockSizeException;
import com.kadware.komodo.hardwarelib.exceptions.InvalidTrackCountException;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Class describing and implementing a DiskDevice which uses the native host filesystem for storage
 *
 * Filesystem-hosted disk packs are stored as logical blocks of fixed size, according to
 * the prep factor specified when the disk pack is prepped.  Sizes are as follows:
 *      PrepFactor  BlockSize
 *      in Words:   in Bytes:
 *          28          128
 *          56          256
 *         112          512
 *         224         1024
 *         448         2048
 *         896         4096     (non-standard, but we accept it due to standard filesystem block sizing)
 *        1792         8192
 *
 * Device-relative logical block numbers start at 0, and are mapped to physical byte
 * addresses according to the following algorithm:
 *      physical_byte_addr = (logical_block_number + 1) * block_size
 * This leaves us with a scratch-pad area in the first logical block.
 * The scratch pad exists so that we can identify the logical geometry of the data in the
 * hosted file without having to know the geometry beforehand.
 *
 * We should note that, in observance of emulated hardware conventions, we do not recognize
 * or care about the Word36 world.  Some entity in the stack above us is responsible for
 * conversions between the Word36 world and the byte world.  Thus, we actually have no
 * visibility to prep factors.  Specifically, we do not have any idea how 36-bit data is
 * packaged into the logical blocks, and even the conversion table above, has no relevance
 * to the code here-in.
 */
@SuppressWarnings("Duplicates")
public class FileSystemDiskDevice extends DiskDevice {

    //TODO This needs to use true async IO

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested enumerations
    //  ----------------------------------------------------------------------------------------------------------------------------


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested classes
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * This class contains information about the pack version and geometry.
     * It is maintained in the fist 'physical' block of the pack, which offsets all the logical blocks by 1.
     */
    static class ScratchPad {

        static final String EXPECTED_IDENTIFIER = "**FSDD**";
        static final short EXPECTED_MAJOR_VERSION = 1;
        static final short EXPECTED_MINOR_VERSION = 1;

        String _identifier;
        short _majorVersion;
        short _minorVersion;
        PrepFactor _prepFactor;
        BlockSize _blockSize;
        BlockCount _blockCount;

        /**
         * Constructor where the caller is establishing values
         */
        ScratchPad(
            final PrepFactor prepFactor,
            final BlockSize blockSize,
            final BlockCount blockCount
        ) {
            _identifier = EXPECTED_IDENTIFIER;
            _majorVersion = EXPECTED_MAJOR_VERSION;
            _minorVersion = EXPECTED_MINOR_VERSION;
            _prepFactor = prepFactor == null ? new PrepFactor() : prepFactor;
            _blockSize = blockSize == null ? new BlockSize() : blockSize;
            _blockCount = blockCount == null ? new BlockCount() : blockCount;
        }

        /**
         * Constructor to be used for subsequent deserializing
         */
        ScratchPad(
        ) {
            _prepFactor = new PrepFactor();
            _blockSize = new BlockSize();
            _blockCount = new BlockCount();
        }

        /**
         * Deserializes a scratch pad object from the given Serializer
         */
        void deserialize(
            final ByteBuffer buffer
        ) {
            _identifier = deserializeString(buffer);
            _majorVersion = buffer.getShort();
            _minorVersion = buffer.getShort();
            _prepFactor.deserialize(buffer);
            _blockSize.deserialize(buffer);
            _blockCount.deserialize(buffer);
        }

        /**
         * Serializes this object into the given Serializer
         */
        void serialize(
            final ByteBuffer buffer
        ) {
            serializeString(buffer, _identifier);
            buffer.putShort(_majorVersion);
            buffer.putShort(_minorVersion);
            _prepFactor.serialize(buffer);
            _blockSize.serialize(buffer);
            _blockCount.serialize(buffer);
        }
    };


    private static final Logger LOGGER = LogManager.getLogger(FileSystemDiskDevice.class);
    private RandomAccessFile _file;
    private String _packName;


    /**
     * Standard constructor
     */
    FileSystemDiskDevice(
        final String name
    ) {
        super(DeviceModel.FileSystemDisk, name);
        _file = null;
        _packName = null;
    }

    /**
     * Retrieves the pack name of the currently mounted media
     * @return pack name if mounted, else an empty string
     */
    String getPackName() { return _isMounted ? _packName : ""; }

    /**
     * Single method for converting a logical block ID to a physical byte offset.
     * Caller must verify blockId is within the blockCount range.
     */
    protected long calculateByteOffset(
        final BlockId blockId
    ) {
        return _blockSize == null ? 0 : (blockId.getValue() + 1) * _blockSize.getValue();
    }

    /**
     * Checks to see whether this node is a valid descendant of the candidate node
     */
    @Override
    public boolean canConnect(
        final Node candidateAncestor
    ) {
        //  We can connect only to Byte channel modules
        //TODO
        //    return candidateAncestor instanceof ByteDiskController;
        return false;
    }

    /**
     * For debugging purposes
     */
    @Override
    public void dump(
        final BufferedWriter writer
    ) {
        super.dump(writer);
        try {
            writer.write(String.format("  Pack Name:       %s\n", getPackName()));
        } catch (IOException ex) {
            LOGGER.catching(ex);
        }
    }

    /**
     * We are a byte interface device
     */
    @Override
    public boolean hasByteInterface() { return true; }

    /**
     * We are NOT a word interface device
     */
    @Override
    public boolean hasWordInterface() { return false; }

    /**
     * Nothing to be done here
     */
    @Override
    public void initialize() {}

    /**
     * Produces a byte stream describing this disk device.
     * This is an immediate IO, no waiting.
     */
    @Override
    protected void ioGetInfo(
        final IOInfo ioInfo
    ) {
        try {
            //  Create a DiskDeviceInfo object and then serialize it to the IOInfo's buffer
            DiskDeviceInfo devInfo = new DiskDeviceInfo.Builder().setBlockCount(_blockCount)
                                                                 .setBlockSize(_blockSize)
                                                                 .setDeviceModel(_deviceModel)
                                                                 .build();

            ByteBuffer bb = ByteBuffer.wrap(ioInfo._byteBuffer);
            devInfo.serialize(bb);
            if (ioInfo._transferCount < bb.position()) {
                ioInfo.setStatus(DeviceStatus.InvalidBlockSize);
            } else {
                //  Clear UA flag - user is asking for the info pending for him.
                setUnitAttention(false);

                //  Complete the IOInfo object
                ioInfo.setTransferredCount(bb.position());
                ioInfo.setStatus(DeviceStatus.Successful);
            }
        } catch (java.nio.BufferOverflowException ex) {
            ioInfo.setStatus(DeviceStatus.BufferTooSmall);
        }
    }

    /**
     * Reads logical records from the underlying data store.
     */
    @Override
    protected void ioRead(
        final IOInfo ioInfo
    ) {
        if (!_readyFlag) {
            ioInfo.setStatus(DeviceStatus.NotReady);
            return;
        }

        if (_unitAttentionFlag) {
            ioInfo.setStatus(DeviceStatus.UnitAttention);
            return;
        }

        //  We probably never need to check this.  We cannot mount a pack which doesn't have a ScratchPad,
        //  and if it has that, it is already hardware-prepped.
        if (!isPrepped()) {
            ioInfo.setStatus(DeviceStatus.NotPrepped);
            return;
        }

        if (ioInfo._byteBuffer.length < ioInfo._transferCount) {
            ioInfo.setStatus(DeviceStatus.BufferTooSmall);
            return;
        }

        long reqByteCount = ioInfo._transferCount;
        if ((reqByteCount == 0) || ((reqByteCount % _blockSize.getValue()) != 0)) {
            ioInfo.setStatus(DeviceStatus.InvalidBlockSize);
            return;
        }

        BlockId reqBlockId = ioInfo._blockId;
        BlockCount reqBlockCount = new BlockCount(reqByteCount / _blockSize.getValue());

        if (reqBlockId.getValue() >= _blockCount.getValue()) {
            ioInfo.setStatus(DeviceStatus.InvalidBlockId);
            return;
        }

        if (reqBlockId.getValue() + reqBlockCount.getValue() > _blockCount.getValue()) {
            ioInfo.setStatus(DeviceStatus.InvalidBlockCount);
            return;
        }

        ioInfo.setStatus(DeviceStatus.InProgress);
        long byteOffset = calculateByteOffset(reqBlockId);

        try {
            _file.seek(byteOffset);
            _file.read(ioInfo._byteBuffer, 0, ioInfo._transferCount);
            ioInfo.setStatus(DeviceStatus.Successful);
        } catch (IOException ex) {
            LOGGER.error(String.format("Device %s IO failed:%s", _name, ex.getMessage()));
            ioInfo.setStatus(DeviceStatus.SystemException);
        }
    }

    /**
     * Resets the device - not much to be done
     */
    @Override
    protected void ioReset(
        final IOInfo ioInfo
    ) {
        if (!_readyFlag) {
            ioInfo.setStatus(DeviceStatus.NotReady);
        } else {
            ioInfo.setStatus(DeviceStatus.Successful);
        }
    }

    /**
     * Unmounts the currently-mounted media, leaving the device not-ready.
     */
    @Override
    protected void ioUnload(
        final IOInfo ioInfo
    ) {
        if (!_readyFlag) {
            ioInfo.setStatus(DeviceStatus.NotReady);
        } else {
            unmount();
            ioInfo.setStatus(DeviceStatus.Successful);
        }
    }

    /**
     * Writes a data block to the media
     */
    @Override
    protected void ioWrite(
        final IOInfo ioInfo
    ) {
        if (!_readyFlag) {
            ioInfo.setStatus(DeviceStatus.NotReady);
            return;
        }

        if (_unitAttentionFlag) {
            ioInfo.setStatus(DeviceStatus.UnitAttention);
            return;
        }

        //  We probably never need to check this.  We cannot mount a pack which doesn't have a ScratchPad,
        //  and if it has that, it is already hardware-prepped.
        if (!isPrepped()) {
            ioInfo.setStatus(DeviceStatus.NotPrepped);
            return;
        }

        if (_isWriteProtected) {
            ioInfo.setStatus(DeviceStatus.WriteProtected);
            return;
        }

        if (ioInfo._byteBuffer.length < ioInfo._transferCount) {
            ioInfo.setStatus(DeviceStatus.BufferTooSmall);
            return;
        }

        long reqByteCount = ioInfo._transferCount;
        if ((reqByteCount == 0) || ((reqByteCount % _blockSize.getValue()) != 0)) {
            ioInfo.setStatus(DeviceStatus.InvalidBlockSize);
            return;
        }

        BlockId reqBlockId = ioInfo._blockId;
        BlockCount reqBlockCount = new BlockCount(reqByteCount / _blockSize.getValue());

        if (reqBlockId.getValue() >= _blockCount.getValue()) {
            ioInfo.setStatus(DeviceStatus.InvalidBlockId);
            return;
        }

        if (reqBlockId.getValue() + reqBlockCount.getValue() > _blockCount.getValue()) {
            ioInfo.setStatus(DeviceStatus.InvalidBlockCount);
            return;
        }

        ioInfo.setStatus(DeviceStatus.InProgress);
        long byteOffset = calculateByteOffset(reqBlockId);

        try {
            _file.seek(byteOffset);
            _file.write(ioInfo._byteBuffer, 0, ioInfo._transferCount);
            ioInfo.setStatus(DeviceStatus.Successful);
        } catch (IOException ex) {
            LOGGER.error(String.format("Device %s IO failed:%s", _name, ex.getMessage()));
            ioInfo.setStatus(DeviceStatus.SystemException);
        }
    }

    /**
     * Mounts the media for this device.
     * For a FileSystemDiskDevice, this entails opening a filesystem file.
     * @param mediaName full path and file name for the pack to be mounted
     * @return true if successful, else false
     */
    boolean mount(
        final String mediaName
    ) {
        if (_isMounted) {
            LOGGER.error(String.format("Device %s Cannot mount %s:Already mounted", _name, mediaName));
            return false;
        }

        try {
            _file = new RandomAccessFile(mediaName, "rw");

            //  Read scratch pad to determine pack geometry.
            //  We don't know the block size at this point, which is something of a problem for us.
            //  We ALSO don't know the size of the serialized ScratchPad, and the underlying file might not have anything
            //  written beyond the first block.  So, we need to do an IO which is at least as big as the serialized
            //  ScratchPad, and no bigger than a block.  And we don't know either of those two sizes.
            //  What we *do* know, is the smallest valid blocksize for this device, and we are pretty sure... hopeful...
            //  crossing fingers (and not just here) that the serialized ScratchPad is no bigger than that smallest block size.
            //  So... knowing smallest block size, we use that for the size of our IO.  It should work...
            int bufferSize = 128;
            byte[] buffer = new byte[bufferSize];
            _file.seek(0);
            int bytes = _file.read(buffer);
            if (bytes < bufferSize) {
                LOGGER.error(String.format("Device %s Cannot mount %s:Failed to read %d bytes of scratch pad",
                                           _name,
                                           mediaName,
                                           bufferSize));
                _file.close();
                _file = null;
                return false;
            }

            ScratchPad scratchPad = new ScratchPad();
            scratchPad.deserialize(ByteBuffer.wrap(buffer));
            if (!scratchPad._identifier.equals(ScratchPad.EXPECTED_IDENTIFIER)) {
                LOGGER.error(String.format("Device %s Cannot mount %s:ScratchPad identifier expected:'%s' got:'%s'",
                                           _name,
                                           mediaName,
                                           ScratchPad.EXPECTED_IDENTIFIER,
                                           scratchPad._identifier));
                _file.close();
                _file = null;
                return false;
            }

            if (scratchPad._majorVersion != ScratchPad.EXPECTED_MAJOR_VERSION) {
                LOGGER.error(String.format("Device %s Cannot mount %s:ScratchPad MajorVersion expected:%d got:%d",
                                           _name,
                                           mediaName,
                                           ScratchPad.EXPECTED_MAJOR_VERSION,
                                           scratchPad._majorVersion));
                _file.close();
                _file = null;
                return false;
            }

            if (scratchPad._minorVersion != ScratchPad.EXPECTED_MINOR_VERSION) {
                LOGGER.info(String.format("Device %s:ScratchPad MinorVersion expected:%d got:%d",
                                           _name,
                                           ScratchPad.EXPECTED_MINOR_VERSION,
                                           scratchPad._minorVersion));
            }

            _packName = mediaName;
            _blockSize = scratchPad._blockSize;
            _blockCount = scratchPad._blockCount;
            _isMounted = true;
            LOGGER.info(String.format("Device %s Mount %s successful", _name, mediaName));
        } catch (IOException ex) {
            LOGGER.error(String.format("Device %s Cannot mount %s:Caught %s", _name, mediaName, ex.getMessage()));
            _file = null;
            return false;
        }

        return true;
    }

    /**
     * Overrides the call to set the ready flag, preventing setting true if we're not mounted.
     */
    @Override
    public boolean setReady(
        final boolean readyFlag
    ) {
        if (readyFlag && !_isMounted) {
            return false;
        }

        return super.setReady(readyFlag);
    }

    /**
     * Invoked when this object is the source of an IO which has been cancelled or completed
     * @param source the Node which is signalling us
     */
    @Override
    public void signal(Node source) { /* nothing to do */ }

    /**
     * Invoked just before tearing down the configuration.
     */
    @Override
    public void terminate() {}

    /**
     * Unmounts the currently-mounted media
     * @return true if successful, else false
     */
    boolean unmount(
    ) {
        if (!_isMounted) {
            LOGGER.error(String.format("Device %s Cannot unmount pack - no pack mounted", _name));
            return false;
        }

        //  Clear ready flag to prevent any more IOs from coming in.
        setReady(false);

        // Release the host system file
        try {
            _file.close();
        } catch (IOException ex) {
            LOGGER.catching(ex);
            return false;
        }
        _file = null;
        _packName = null;

        _blockCount = null;
        _blockSize = null;
        _isMounted = false;
        _isWriteProtected = true;

        return true;
    }

    /**
     * Creates a host system file which can be mounted on this type of device as a pack
     * @param fileName - includes any path information, and the required suffix (usually .pack)
     * @param blockSize - block size, in bytes
     * @param blockCount - number of blocks
     */
    static void createPack(
        final String fileName,
        final BlockSize blockSize,
        final BlockCount blockCount
    ) throws InternalError,
             InvalidBlockSizeException,
             InvalidTrackCountException,
             IOException {
        if (!isValidBlockSize(blockSize.getValue())) {
            throw new InvalidBlockSizeException(blockSize);
        }

        PrepFactor prepFactor = PrepFactor.getPrepFactorFromBlockSize(blockSize);
        TrackCount trackCount = new TrackCount(prepFactor.getValue() * blockCount.getValue() / 1792);
        if ((trackCount.getValue() < 10000) || (trackCount.getValue() > 99999)) {
            throw new InvalidTrackCountException(trackCount);
        }

        //  Create system file to contain the pack image
        RandomAccessFile file = new RandomAccessFile(fileName, "rw");

        //  Write scratchpad area.
        ScratchPad scratchPad = new ScratchPad(new PrepFactor(), blockSize, blockCount);
        ByteBuffer bb = ByteBuffer.wrap(new byte[blockSize.getValue()]);
        scratchPad.serialize(bb);
        file.seek(0);
        file.write(bb.array());
        file.close();
    }
}
