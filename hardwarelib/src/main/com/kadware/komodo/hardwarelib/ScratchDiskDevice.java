/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.ArraySlice;
import java.util.HashMap;
import java.util.Map;

/**
 * Class describing and implementing a DiskDevice which uses volatile system memory for storage
 */
@SuppressWarnings("Duplicates")
public class ScratchDiskDevice extends DiskDevice {

    private static final long BYTE_COUNT = 10 * 1024 * 1024;    //  10 MBytes
    private static final int BLOCK_SIZE = 512;
    private static final long BLOCK_COUNT = BYTE_COUNT / BLOCK_SIZE;

    private final Map<Integer, Byte[]> _storage = new HashMap<>();


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructor
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Standard constructor
     */
    public ScratchDiskDevice(
        final String name
    ) {
        super(Model.ScratchDisk, name);
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Node, Device implementations
    //  ----------------------------------------------------------------------------------------------------------------------------

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
        final IOInfo ioInfo
    ) {
        ++_miscCount;

        ArraySlice as = getInfo();
        ioInfo._byteBuffer = new byte[128];
        as.pack(ioInfo._byteBuffer);
        ioInfo._status = IOStatus.Successful;
        _unitAttentionFlag = false;
        ioInfo._source.signal();
    }

    /**
     * Reads logical records from the underlying data store.
     */
    @Override
    protected void ioRead(
        final IOInfo ioInfo
    ) {
        ++_readCount;

        if (!_readyFlag) {
            ioInfo._status = IOStatus.NotReady;
            ioInfo._source.signal();
            return;
        }

        if (_unitAttentionFlag) {
            ioInfo._status = IOStatus.UnitAttention;
            ioInfo._source.signal();
            return;
        }

        if (!isPrepped()) {
            ioInfo._status = IOStatus.NotPrepped;
            ioInfo._source.signal();
            return;
        }

        long reqByteCount = ioInfo._transferCount;
        if ((reqByteCount % _blockSize) != 0) {
            ioInfo._status = IOStatus.InvalidBlockSize;
            ioInfo._source.signal();
            return;
        }

        long reqBlockId = ioInfo._blockId;
        long reqBlockCount = reqByteCount / _blockSize;

        if (reqBlockId >= _blockCount) {
            ioInfo._status = IOStatus.InvalidBlockId;
            ioInfo._source.signal();
            return;
        }

        if (reqBlockId + reqBlockCount > _blockCount) {
            ioInfo._status = IOStatus.InvalidBlockCount;
            ioInfo._source.signal();
            return;
        }

        int firstBlockId = (int)reqBlockId;
        int lastBlockId = (int)(reqBlockId + reqBlockCount - 1);

        for (int bid = firstBlockId, bbx = 0; bid <= lastBlockId; ++bid, bbx += BLOCK_SIZE) {
            Byte[] block = _storage.getOrDefault(bid, new Byte[BLOCK_SIZE]);
            for (int sx = 0, dx = bbx; sx < BLOCK_SIZE; ++sx, ++dx) {
                ioInfo._byteBuffer[dx] = block[sx];
            }
        }

        ioInfo._status = IOStatus.Successful;
    }

    /**
     * Resets the device - not much to be done
     */
    @Override
    protected void ioReset(
        final IOInfo ioInfo
    ) {
        ++_miscCount;

        if (!_readyFlag) {
            ioInfo._status = IOStatus.NotReady;
        } else {
            ioInfo._status = IOStatus.Successful;
        }

        ioInfo._source.signal();
    }

    /**
     * Unmounts the currently-mounted media, leaving the device not-ready.
     */
    @Override
    protected void ioUnload(
        final IOInfo ioInfo
    ) {
        ++_miscCount;

        if (!_readyFlag) {
            ioInfo._status = IOStatus.NotReady;
        } else {
            unmount();
            ioInfo._status = IOStatus.Successful;
        }

        ioInfo._source.signal();
    }

    /**
     * Writes a data block to the media
     */
    @Override
    protected void ioWrite(
        final IOInfo ioInfo
    ) {
        ++_writeCount;

        if (!_readyFlag) {
            ioInfo._status = IOStatus.NotReady;
            ioInfo._source.signal();
            return;
        }

        if (_unitAttentionFlag) {
            ioInfo._status = IOStatus.UnitAttention;
            ioInfo._source.signal();
            return;
        }

        if (!isPrepped()) {
            ioInfo._status = IOStatus.NotPrepped;
            ioInfo._source.signal();
            return;
        }

        if (_isWriteProtected) {
            ioInfo._status = IOStatus.WriteProtected;
            ioInfo._source.signal();
            return;
        }

        if (ioInfo._byteBuffer.length < ioInfo._transferCount) {
            ioInfo._status = IOStatus.BufferTooSmall;
            ioInfo._source.signal();
            return;
        }

        long reqByteCount = ioInfo._transferCount;
        if ((reqByteCount % _blockSize) != 0) {
            ioInfo._status = IOStatus.InvalidBlockSize;
            ioInfo._source.signal();
            return;
        }

        long reqBlockId = ioInfo._blockId;
        long reqBlockCount = reqByteCount / _blockSize;

        if (reqBlockId >= _blockCount) {
            ioInfo._status = IOStatus.InvalidBlockId;
            ioInfo._source.signal();
            return;
        }

        if (reqBlockId + reqBlockCount > _blockCount) {
            ioInfo._status = IOStatus.InvalidBlockCount;
            ioInfo._source.signal();
            return;
        }

        int firstBlockId = (int)reqBlockId;
        int lastBlockId = (int)(reqBlockId + reqBlockCount - 1);

        for (int bid = firstBlockId, bbx = 0; bid <= lastBlockId; ++bid, bbx += BLOCK_SIZE) {
            Byte[] block = new Byte[BLOCK_SIZE];
            for (int dx = 0, sx = bbx; dx < BLOCK_SIZE; ++dx, ++sx) {
                block[dx] = ioInfo._byteBuffer[sx];
            }
            _storage.put(bid, block);
        }

        ioInfo._status = IOStatus.Successful;
    }

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Private methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Mounts the media for this device.
     * @return true if successful, else false
     */
    boolean mount() {
        if (_isMounted) {
            _logger.error("Cannot mount media:Already mounted");
            return false;
        }

        _blockSize = BLOCK_SIZE;
        _blockCount = BLOCK_COUNT;
        _isMounted = true;
        _storage.clear();
        _logger.info("Mount successful");

        return true;
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
     * Unmounts the currently-mounted media
     */
    void unmount() {
        if (!_isMounted) {
            _logger.error("Cannot unmount pack - no pack mounted");
            return;
        }

        setReady(false);

        _storage.clear();
        _blockCount = null;
        _blockSize = null;
        _isMounted = false;
        _isWriteProtected = true;
    }
}
