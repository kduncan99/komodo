/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib.channels;

import com.bearsnake.komodo.hardwarelib.devices.Device;
import com.bearsnake.komodo.hardwarelib.Node;
import com.bearsnake.komodo.hardwarelib.NodeCategory;
import com.bearsnake.komodo.logger.LogManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Base class for all Channel classes.
 * Note that we do not implement the concept of channel programs.
 * We do not support scatter/gather IO.
 * We *DO* support transfer formats A, C, and D (depending upon the particular subclass).
 * We do not (currently) support direction (including skip data)
 */
public abstract class Channel extends Node implements Runnable {

    // key is node identifier
    protected final HashMap<Integer, Device> _devices = new HashMap<>();
    private boolean _terminate = false;

    public Channel(final String nodeName) {
        super(nodeName);
        var t = new Thread(this);
        t.setDaemon(true);
        t.start();
    }

    /**
     * Attaches the given device to this channel - do NOT invoke before checking canAttach()
     */
    public final void attach(final Device device) {
        _devices.put(device.getNodeIdentifier(), device);
    }

    /**
     * Detaches the given device from this channel.
     */
    public final void detach(final Device device) {
        _devices.remove(device.getNodeIdentifier());
    }

    /**
     * Asks whether the given device can be attached to this channel
     */
    public abstract boolean canAttach(final Device device);

    /**
     * Invoked when we are breaking down the configuration
     */
    @Override
    public void close() {
        _terminate = true;
    }

    /**
     * Retrieves ChannelType from the subclass
     */
    public abstract ChannelType getChannelType();

    /**
     * Retrieves a collection of the devices which are attached to this channel
     */
    public Collection<Device> getDevices() {
        return new HashSet<>(_devices.values());
    }

    /**
     * Retrieves the node category - for us, it is Channel.
     */
    @Override
    public NodeCategory getNodeCategory() { return NodeCategory.Channel; }

    /**
     * Routes an IO as appropriate, preprocessing if and when necessary
     */
    public abstract void routeIo(final ChannelIoPacket packet);

    @Override
    public synchronized String toString() {
        return String.format("%s %s:%s",
                             getNodeName(),
                             getNodeCategory(),
                             getChannelType());
    }

    /**
     * This is a thread which runs in the background in order to periodically probe the devices
     * to which we are connected. If a device is connected to more than one channel, it gets multiply-probed.
     */
    @Override
    public void run() {
        LogManager.logTrace(getNodeName(), "probe daemon starting");

        while (!_terminate) {
            synchronized (this) {
                getDevices().forEach(Device::probe);
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                LogManager.logCatching(getNodeName(), e);
            }
        }

        LogManager.logTrace(getNodeName(), "probe daemon stopping");
    }
}
