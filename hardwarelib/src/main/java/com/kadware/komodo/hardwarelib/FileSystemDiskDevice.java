/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.*;
import com.kadware.komodo.hardwarelib.exceptions.*;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
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
public class FileSystemDiskDevice extends DiskDevice implements CompletionHandler<Integer, DeviceIOInfo> {

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
        int _prepFactor;
        int _blockSize;
        long _blockCount;

        /**
         * Constructor where the caller is establishing values
         */
        ScratchPad(
            final int prepFactor,
            final int blockSize,
            final long blockCount
        ) {
            _identifier = EXPECTED_IDENTIFIER;
            _majorVersion = EXPECTED_MAJOR_VERSION;
            _minorVersion = EXPECTED_MINOR_VERSION;
            _prepFactor = prepFactor;
            _blockSize = blockSize;
            _blockCount = blockCount;
        }

        /**
         * Constructor to be used for subsequent deserializing
         */
        ScratchPad() {}

        /**
         * Deserializes a scratch pad object from the given Serializer
         */
        void deserialize(
            final ByteBuffer buffer
        ) {
            _identifier = deserializeString(buffer);
            _majorVersion = buffer.getShort();
            _minorVersion = buffer.getShort();
            _prepFactor = buffer.getInt();
            _blockSize = buffer.getInt();
            _blockCount = buffer.getLong();
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
            buffer.putInt(_prepFactor);
            buffer.putInt(_blockSize);
            buffer.putLong(_blockCount);
        }
    };

    private static final Logger LOGGER = LogManager.getLogger(FileSystemDiskDevice.class);
    private AsynchronousFileChannel _channel;
    private String _fileName;       //  only valid if mounted


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructor
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Standard constructor
     */
    FileSystemDiskDevice(
        final String name
    ) {
        super(DeviceModel.FileSystemDisk, name);
        _channel = null;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  CompletionHandler implementation
    //  ----------------------------------------------------------------------------------------------------------------------------

    public void completed(Integer value, DeviceIOInfo attachment) {
        attachment._transferredCount = value;
        attachment._status = DeviceStatus.Successful;
        attachment._source.signal();
        if (attachment._ioFunction.isReadFunction()) {
            _readBytes += value;
        } else {
            _writeBytes += value;
        }
    }

    /**
     * Async io failed
     */
    public void failed(Throwable t, DeviceIOInfo attachment) {
        attachment._status = DeviceStatus.SystemException;
        attachment._source.signal();
        LOGGER.error(String.format("Device %s IO failed:%s", _name, t.getMessage()));
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Node, Device implementations
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Checks to see whether this node is a valid descendant of the candidate node
     */
    @Override
    public boolean canConnect(
        final Node candidateAncestor
    ) {
        //  We can connect only to Byte channel modules
        return candidateAncestor instanceof ByteChannelModule;
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
     * Invoked just before tearing down the configuration.
     */
    @Override
    public void terminate() {}


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  DiskDevice implementation
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Produces a byte stream describing this disk device.
     * This is an immediate IO, no waiting.
     */
    @Override
    protected void ioGetInfo(
        final DeviceIOInfo ioInfo
    ) {
        ++_miscCount;

        ArraySlice as = getInfo();
        ioInfo._byteBuffer = new byte[128];
        as.pack(ioInfo._byteBuffer);
        ioInfo._status = DeviceStatus.Successful;
        _unitAttentionFlag = false;
        ioInfo._source.signal();
    }

    /**
     * Reads logical records from the underlying data store.
     */
    @Override
    protected void ioRead(
        final DeviceIOInfo ioInfo
    ) {
        ++_readCount;

        if (!_readyFlag) {
            ioInfo._status = DeviceStatus.NotReady;
            ioInfo._source.signal();
            return;
        }

        if (_unitAttentionFlag) {
            ioInfo._status = DeviceStatus.UnitAttention;
            ioInfo._source.signal();
            return;
        }

        //  We probably never need to check this.  We cannot mount a pack which doesn't have a ScratchPad,
        //  and if it has that, it is already hardware-prepped.
        if (!isPrepped()) {
            ioInfo._status = DeviceStatus.NotPrepped;
            ioInfo._source.signal();
            return;
        }

        long reqByteCount = ioInfo._transferCount;
        if ((reqByteCount % _blockSize) != 0) {
            ioInfo._status = DeviceStatus.InvalidBlockSize;
            ioInfo._source.signal();
            return;
        }

        long reqBlockId = ioInfo._blockId;
        long reqBlockCount = reqByteCount / _blockSize;

        if (reqBlockId >= _blockCount) {
            ioInfo._status = DeviceStatus.InvalidBlockId;
            ioInfo._source.signal();
            return;
        }

        if (reqBlockId + reqBlockCount > _blockCount) {
            ioInfo._status = DeviceStatus.InvalidBlockCount;
            ioInfo._source.signal();
            return;
        }

        ioInfo._status = DeviceStatus.InProgress;
        ioInfo._byteBuffer = new byte[ioInfo._transferCount];
        long byteOffset = calculateByteOffset(reqBlockId);
        _channel.read(ByteBuffer.wrap(ioInfo._byteBuffer), byteOffset, ioInfo, this);
    }

    /**
     * Resets the device - not much to be done
     */
    @Override
    protected void ioReset(
        final DeviceIOInfo ioInfo
    ) {
        ++_miscCount;

        if (!_readyFlag) {
            ioInfo._status = DeviceStatus.NotReady;
        } else {
            ioInfo._status = DeviceStatus.Successful;
        }

        ioInfo._source.signal();
    }

    /**
     * Unmounts the currently-mounted media, leaving the device not-ready.
     */
    @Override
    protected void ioUnload(
        final DeviceIOInfo ioInfo
    ) {
        ++_miscCount;

        if (!_readyFlag) {
            ioInfo._status = DeviceStatus.NotReady;
        } else {
            unmount();
            ioInfo._status = DeviceStatus.Successful;
        }

        ioInfo._source.signal();
    }

    /**
     * Writes a data block to the media
     */
    @Override
    protected void ioWrite(
        final DeviceIOInfo ioInfo
    ) {
        ++_writeCount;

        if (!_readyFlag) {
            ioInfo._status = DeviceStatus.NotReady;
            ioInfo._source.signal();
            return;
        }

        if (_unitAttentionFlag) {
            ioInfo._status = DeviceStatus.UnitAttention;
            ioInfo._source.signal();
            return;
        }

        //  We probably never need to check this.  We cannot mount a pack which doesn't have a ScratchPad,
        //  and if it has that, it is already hardware-prepped.
        if (!isPrepped()) {
            ioInfo._status = DeviceStatus.NotPrepped;
            ioInfo._source.signal();
            return;
        }

        if (_isWriteProtected) {
            ioInfo._status = DeviceStatus.WriteProtected;
            ioInfo._source.signal();
            return;
        }

        if (ioInfo._byteBuffer.length < ioInfo._transferCount) {
            ioInfo._status = DeviceStatus.BufferTooSmall;
            ioInfo._source.signal();
            return;
        }

        long reqByteCount = ioInfo._transferCount;
        if ((reqByteCount % _blockSize) != 0) {
            ioInfo._status = DeviceStatus.InvalidBlockSize;
            ioInfo._source.signal();
            return;
        }

        long reqBlockId = ioInfo._blockId;
        long reqBlockCount = reqByteCount / _blockSize;

        if (reqBlockId >= _blockCount) {
            ioInfo._status = DeviceStatus.InvalidBlockId;
            ioInfo._source.signal();
            return;
        }

        if (reqBlockId + reqBlockCount > _blockCount) {
            ioInfo._status = DeviceStatus.InvalidBlockCount;
            ioInfo._source.signal();
            return;
        }

        ioInfo._status = DeviceStatus.InProgress;
        long byteOffset = calculateByteOffset(reqBlockId);
        _channel.write(ByteBuffer.wrap(ioInfo._byteBuffer), byteOffset, ioInfo, this);
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Non-public methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Single method for converting a logical block ID to a physical byte offset.
     * Caller must verify blockId is within the blockCount range.
     */
    long calculateByteOffset(
        final long blockId
    ) {
        return _blockSize == null ? 0 : (blockId + 1) * _blockSize;
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
            Path path = FileSystems.getDefault().getPath(mediaName);
            _channel = AsynchronousFileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE);

            //  Read scratch pad to determine pack geometry.
            //  We don't know the block size at this point, which is something of a problem for us.
            //  We ALSO don't know the size of the serialized ScratchPad, and the underlying file might not have anything
            //  written beyond the first block.  So, we need to do an IO which is at least as big as the serialized
            //  ScratchPad, and no bigger than a block.  And we don't know either of those two sizes.
            //  What we *do* know, is the smallest valid blocksize for this device, and we are pretty sure... hopeful...
            //  crossing fingers (and not just here) that the serialized ScratchPad is no bigger than that smallest block size.
            //  So... knowing smallest block size, we use that for the size of our IO.  It should work...
            ScratchPad scratchPad = readScratchPad();
            if (scratchPad == null) {
                LOGGER.error(String.format("Device %s Cannot mount %s:Failed to read some or all of scratch pad",
                                           _name,
                                           mediaName));
                _channel.close();
                _channel = null;
                return false;
            }

            if (!scratchPad._identifier.equals(ScratchPad.EXPECTED_IDENTIFIER)) {
                LOGGER.error(String.format("Device %s Cannot mount %s:ScratchPad identifier expected:'%s' got:'%s'",
                                           _name,
                                           mediaName,
                                           ScratchPad.EXPECTED_IDENTIFIER,
                                           scratchPad._identifier));
                _channel.close();
                _channel = null;
                return false;
            }

            if (scratchPad._majorVersion != ScratchPad.EXPECTED_MAJOR_VERSION) {
                LOGGER.error(String.format("Device %s Cannot mount %s:ScratchPad MajorVersion expected:%d got:%d",
                                           _name,
                                           mediaName,
                                           ScratchPad.EXPECTED_MAJOR_VERSION,
                                           scratchPad._majorVersion));
                _channel.close();
                _channel = null;
                return false;
            }

            if (scratchPad._minorVersion != ScratchPad.EXPECTED_MINOR_VERSION) {
                LOGGER.info(String.format("Device %s:ScratchPad MinorVersion expected:%d got:%d",
                                          _name,
                                          ScratchPad.EXPECTED_MINOR_VERSION,
                                          scratchPad._minorVersion));
            }

            _blockSize = scratchPad._blockSize;
            _blockCount = scratchPad._blockCount;
            _isMounted = true;
            _fileName = mediaName;
            LOGGER.info(String.format("Device %s Mount %s successful", _name, mediaName));
        } catch (IOException ex) {
            LOGGER.error(String.format("Device %s Cannot mount %s:Caught %s", _name, mediaName, ex.getMessage()));
            _channel = null;
            return false;
        }

        return true;
    }

    private ScratchPad readScratchPad(
    ) {
        int bufferSize = 128;
        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);
        Future<Integer> f = _channel.read(buffer, 0);
        try {
            if (f.get() < bufferSize) {
                return null;
            }

            if (f.isCancelled()) {
                return null;
            } else {
                buffer.rewind();
                ScratchPad scratchPad = new ScratchPad();
                scratchPad.deserialize(buffer);
                return scratchPad;
            }
        } catch (ExecutionException | InterruptedException ex) {
            System.out.println(ex);
            return null;
        }
    }

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Public methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Nothing to clear
     */
    @Override
    public void clear() {}

    /**
     * For debugging purposes
     */
    @Override
    public void dump(
        final BufferedWriter writer
    ) {
        super.dump(writer);
        try {
            writer.write(String.format("  File Name:       %s\n", String.valueOf(_fileName)));
        } catch (IOException ex) {
            LOGGER.catching(ex);
        }
    }

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
            _channel.close();
        } catch (IOException ex) {
            LOGGER.catching(ex);
            return false;
        }

        _channel = null;
        _blockCount = null;
        _blockSize = null;
        _isMounted = false;
        _isWriteProtected = true;
        _fileName = null;

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
        final int blockSize,
        final long blockCount
    ) throws InternalError,
             InvalidBlockSizeException,
             InvalidTrackCountException,
             IOException {
        if (!isValidBlockSize(blockSize)) {
            throw new InvalidBlockSizeException(blockSize);
        }

        int prepFactor = PrepFactor.getPrepFactorFromBlockSize(blockSize);
        long trackCount = prepFactor * blockCount / 1792;
        if ((trackCount < 10000) || (trackCount > 99999)) {
            throw new InvalidTrackCountException(trackCount);
        }

        //  Create system file to contain the pack image
        RandomAccessFile file = new RandomAccessFile(fileName, "rw");

        //  Write scratchpad area.
        ScratchPad scratchPad = new ScratchPad(prepFactor, blockSize, blockCount);
        ByteBuffer bb = ByteBuffer.wrap(new byte[blockSize]);
        scratchPad.serialize(bb);
        file.seek(0);
        file.write(bb.array());
        file.close();
    }
}
