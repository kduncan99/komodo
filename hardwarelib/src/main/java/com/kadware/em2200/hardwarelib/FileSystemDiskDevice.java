/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.hardwarelib;

import com.kadware.em2200.hardwarelib.exceptions.InvalidBlockSizeException;
import com.kadware.em2200.hardwarelib.exceptions.InvalidTrackCountException;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import com.kadware.komodo.baselib.types.*;

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

public class FileSystemDiskDevice extends DiskDevice {

    //???? This needs to use true async IO

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
    protected static class ScratchPad {

        public static final String EXPECTED_IDENTIFIER = "**FSDD**";
        public static final short EXPECTED_MAJOR_VERSION = 1;
        public static final short EXPECTED_MINOR_VERSION = 1;

        public String _identifier;
        public short _majorVersion;
        public short _minorVersion;
        public PrepFactor _prepFactor;
        public BlockSize _blockSize;
        public BlockCount _blockCount;

        /**
         * Constructor where the caller is establishing values
         * <p>
         * @param prepFactor
         * @param blockSize
         * @param blockCount
         */
        public ScratchPad(
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
        public ScratchPad(
        ) {
            _prepFactor = new PrepFactor();
            _blockSize = new BlockSize();
            _blockCount = new BlockCount();
        }

        /**
         * Deserializes a scratch pad object from the given Serializer
         * <p>
         * @param buffer
         */
        public void deserialize(
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
         * <p>
         * @param buffer
         */
        public void serialize(
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


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Class attributes
    //  ----------------------------------------------------------------------------------------------------------------------------

    private static final Logger LOGGER = LogManager.getLogger(FileSystemDiskDevice.class);

    private RandomAccessFile _file;
    private String _packName;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructors
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Standard constructor
     * <p>
     * @param name
     * @param subsystemIdentifier
     */
    public FileSystemDiskDevice(
        final String name,
        final short subsystemIdentifier
    ) {
        super(DeviceModel.FileSystemDisk, name, subsystemIdentifier);
        _file = null;
        _packName = null;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Accessors
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Retrieves the pack name of the currently mounted media
     * <p>
     * @return pack name if mounted, else an empty string
     */
    public String getPackName(
    ) {
        return isMounted() ? _packName : "";
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Abstract methods
    //  ----------------------------------------------------------------------------------------------------------------------------


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Instance methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Single method for converting a logical block ID to a physical byte offset.
     * Caller must verify blockId is within the blockCount range.
     * <p>
     * @param blockId
     * <p>
     * @return
     */
    protected long calculateByteOffset(
        final BlockId blockId
    ) {
        return (blockId.getValue() + 1) * getBlockSize().getValue();
    }

    /**
     * Checks to see whether this node is a valid descendant of the candidate node
     * @param candidateAncestor
     * @return
     */
    @Override
    public boolean canConnect(
        final Node candidateAncestor
    ) {
        //  We can connect only to ByteDisk controllers
        return candidateAncestor instanceof ByteDiskController;
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
        try {
            writer.write(String.format("  Pack Name:       %s\n", getPackName()));
        } catch (IOException ex) {
            LOGGER.catching(ex);
        }
    }

    /**
     * We are a byte interface device
     * <p>
     * @return
     */
    @Override
    public boolean hasByteInterface(
    ) {
        return true;
    }

    /**
     * We are NOT a word interface device
     * <p>
     * @return
     */
    @Override
    public boolean hasWordInterface(
    ) {
        return false;
    }

    /**
     * Nothing to be done here
     */
    @Override
    public void initialize() {}

    /**
     * Produces a byte stream describing this disk device.
     * This is an immediate IO, no waiting.
     * <p>
     * @param ioInfo
     */
    @Override
    protected void ioGetInfo(
        final IOInfo ioInfo
    ) {
        try {
            //  Create a DiskDeviceInfo object and then serialize it to the IOInfo's buffer
            DiskDeviceInfo devInfo = new DiskDeviceInfo(getDeviceType(),
                                                        getDeviceModel(),
                                                        isReady(),
                                                        getSubsystemIdentifier(),
                                                        getUnitAttentionFlag(),
                                                        getBlockCount(),
                                                        getBlockSize(),
                                                        isMounted(),
                                                        isWriteProtected());

            ByteBuffer bb = ByteBuffer.wrap(ioInfo.getByteBuffer());
            devInfo.serialize(bb);
            if (ioInfo.getTransferCount() < bb.position()) {
                ioInfo.setStatus(IOStatus.InvalidBlockSize);
            } else {
                //  Clear UA flag - user is asking for the info pending for him.
                setUnitAttention(false);

                //  Complete the IOInfo object
                ioInfo.setTransferredCount(bb.position());
                ioInfo.setStatus(IOStatus.Successful);
            }
        } catch (java.nio.BufferOverflowException ex) {
            ioInfo.setStatus(IOStatus.BufferTooSmall);
        }
    }

    /**
     * Reads logical records from the underlying data store.
     * <p>
     * @param ioInfo
     */
    @Override
    protected void ioRead(
        final IOInfo ioInfo
    ) {
        if (!isReady()) {
            ioInfo.setStatus(IOStatus.NotReady);
            return;
        }

        if (getUnitAttentionFlag()) {
            ioInfo.setStatus(IOStatus.UnitAttention);
            return;
        }

        //  We probably never need to check this.  We cannot mount a pack which doesn't have a ScratchPad,
        //  and if it has that, it is already hardware-prepped.
        if (!isPrepped()) {
            ioInfo.setStatus(IOStatus.NotPrepped);
            return;
        }

        if (ioInfo.getByteBuffer().length < ioInfo.getTransferCount()) {
            ioInfo.setStatus(IOStatus.BufferTooSmall);
            return;
        }

        long reqByteCount = ioInfo.getTransferCount();
        if ((reqByteCount == 0) || ((reqByteCount % getBlockSize().getValue()) != 0)) {
            ioInfo.setStatus(IOStatus.InvalidBlockSize);
            return;
        }

        BlockId reqBlockId = ioInfo.getBlockId();
        BlockCount reqBlockCount = new BlockCount(reqByteCount / getBlockSize().getValue());

        if (reqBlockId.getValue() >= getBlockCount().getValue()) {
            ioInfo.setStatus(IOStatus.InvalidBlockId);
            return;
        }

        if ((reqBlockId.getValue() + reqBlockCount.getValue() > getBlockCount().getValue())
                || (reqByteCount > 0x7FFFFFFF)) {
            ioInfo.setStatus(IOStatus.InvalidBlockCount);
            return;
        }

        ioInfo.setStatus(IOStatus.InProgress);
        long byteOffset = calculateByteOffset(reqBlockId);

        try {
            _file.seek(byteOffset);
            _file.read(ioInfo.getByteBuffer(), 0, (int)ioInfo.getTransferCount());
            ioInfo.setStatus(IOStatus.Successful);
        } catch (IOException ex) {
            LOGGER.error(String.format("Device %s IO failed:%s", getName(), ex.getMessage()));
            ioInfo.setStatus(IOStatus.SystemException);
        }
    }

    /**
     * Resets the device - not much to be done
     * <p>
     * @param ioInfo
     */
    @Override
    protected void ioReset(
        final IOInfo ioInfo
    ) {
        if (!isReady()) {
            ioInfo.setStatus(IOStatus.NotReady);
        } else {
            ioInfo.setStatus(IOStatus.Successful);
        }
    }

    /**
     * Unmounts the currently-mounted media, leaving the device not-ready.
     * <p>
     * @param ioInfo
     */
    @Override
    protected void ioUnload(
        final IOInfo ioInfo
    ) {
        if (!isReady()) {
            ioInfo.setStatus(IOStatus.NotReady);
        } else {
            unmount();
            ioInfo.setStatus(IOStatus.Successful);
        }
    }

    /**
     * Writes a data block to the media
     * <p>
     * @param ioInfo
     */
    @Override
    protected void ioWrite(
        final IOInfo ioInfo
    ) {
        if (!isReady()) {
            ioInfo.setStatus(IOStatus.NotReady);
            return;
        }

        if (getUnitAttentionFlag()) {
            ioInfo.setStatus(IOStatus.UnitAttention);
            return;
        }

        //  We probably never need to check this.  We cannot mount a pack which doesn't have a ScratchPad,
        //  and if it has that, it is already hardware-prepped.
        if (!isPrepped()) {
            ioInfo.setStatus(IOStatus.NotPrepped);
            return;
        }

        if (isWriteProtected()) {
            ioInfo.setStatus(IOStatus.WriteProtected);
            return;
        }

        if (ioInfo.getByteBuffer().length < ioInfo.getTransferCount()) {
            ioInfo.setStatus(IOStatus.BufferTooSmall);
            return;
        }

        long reqByteCount = ioInfo.getTransferCount();
        if ((reqByteCount == 0) || ((reqByteCount % getBlockSize().getValue()) != 0)) {
            ioInfo.setStatus(IOStatus.InvalidBlockSize);
            return;
        }

        BlockId reqBlockId = ioInfo.getBlockId();
        BlockCount reqBlockCount = new BlockCount(reqByteCount / getBlockSize().getValue());

        if (reqBlockId.getValue() >= getBlockCount().getValue()) {
            ioInfo.setStatus(IOStatus.InvalidBlockId);
            return;
        }

        if ((reqBlockId.getValue() + reqBlockCount.getValue() > getBlockCount().getValue())
                || (reqByteCount > 0x7FFFFFFF)) {
            ioInfo.setStatus(IOStatus.InvalidBlockCount);
            return;
        }

        ioInfo.setStatus(IOStatus.InProgress);
        long byteOffset = calculateByteOffset(reqBlockId);

        try {
            _file.seek(byteOffset);
            _file.write(ioInfo.getByteBuffer(), 0, (int)ioInfo.getTransferCount());
            ioInfo.setStatus(IOStatus.Successful);
        } catch (IOException ex) {
            LOGGER.error(String.format("Device %s IO failed:%s", getName(), ex.getMessage()));
            ioInfo.setStatus(IOStatus.SystemException);
        }
    }

    /**
     * Mounts the media for this device.
     * For a FileSystemDiskDevice, this entails opening a filesystem file.
     * <p>
     * @param mediaName full path and file name for the pack to be mounted
     * <p>
     * @return true if successful, else false
     */
    public boolean mount(
        final String mediaName
    ) {
        if (isMounted()) {
            LOGGER.error(String.format("Device %s Cannot mount %s:Already mounted", getName(), mediaName));
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
                                           getName(),
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
                                           getName(),
                                           mediaName,
                                           ScratchPad.EXPECTED_IDENTIFIER,
                                           scratchPad._identifier));
                _file.close();
                _file = null;
                return false;
            }

            if (scratchPad._majorVersion != ScratchPad.EXPECTED_MAJOR_VERSION) {
                LOGGER.error(String.format("Device %s Cannot mount %s:ScratchPad MajorVersion expected:%d got:%d",
                                           getName(),
                                           mediaName,
                                           ScratchPad.EXPECTED_MAJOR_VERSION,
                                           scratchPad._majorVersion));
                _file.close();
                _file = null;
                return false;
            }

            if (scratchPad._minorVersion != ScratchPad.EXPECTED_MINOR_VERSION) {
                LOGGER.info(String.format("Device %s:ScratchPad MinorVersion expected:%d got:%d",
                                           getName(),
                                           ScratchPad.EXPECTED_MINOR_VERSION,
                                           scratchPad._minorVersion));
            }

            _packName = mediaName;
            setBlockSize(scratchPad._blockSize);
            setBlockCount(scratchPad._blockCount);
            setIsMounted(true);
            LOGGER.info(String.format("Device %s Mount %s successful", getName(), mediaName));
        } catch (IOException ex) {
            LOGGER.error(String.format("Device %s Cannot mount %s:Caught %s", getName(), mediaName, ex.getMessage()));
            _file = null;
            return false;
        }

        return true;
    }

    /**
     * Overrides the call to set the ready flag, preventing setting true if we're not mounted.
     * <p>
     * @param readyFlag
     * <p>
     * @return
     */
    @Override
    public boolean setReady(
        final boolean readyFlag
    ) {
        if (readyFlag && !isMounted()) {
            return false;
        }

        return super.setReady(readyFlag);
    }

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

    /**
     * Unmounts the currently-mounted media
     * <p>
     * @return true if successful, else false
     */
    public boolean unmount(
    ) {
        if (!isMounted()) {
            LOGGER.error(String.format("Device %s Cannot unmount pack - no pack mounted", getName()));
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

        setBlockCount(null);
        setBlockSize(null);
        setIsMounted(false);
        setIsWriteProtected(true);

        return true;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Static methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Creates a host system file which can be mounted on this type of device as a pack
     * <p>
     * @param fileName - includes any path information, and the required suffix (usually .pack)
     * @param blockSize
     * @param blockCount
     * <p>
     * @throws FileNotFoundException
     * @throws InternalError
     * @throws InvalidBlockSizeException
     * @throws InvalidTrackCountException
     * @throws IOException
     */
    public static void createPack(
        final String fileName,
        final BlockSize blockSize,
        final BlockCount blockCount
    ) throws FileNotFoundException,
             InternalError,
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
