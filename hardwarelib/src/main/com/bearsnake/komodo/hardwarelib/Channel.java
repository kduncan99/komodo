/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.hardwarelib;

import com.bearsnake.komodo.logger.LogManager;
import java.util.HashMap;
import java.util.HashSet;

public abstract class Channel extends Node {

    // key is node identifier
    private final HashMap<Integer, Device> _devices = new HashMap<>();

    public Channel(final String nodeName) {
        super(nodeName);
    }

    public final void attach(final Device device) {
        _devices.put(device.getNodeIdentifier(), device);
    }

    public final void detach(final Device device) {
        _devices.remove(device.getNodeIdentifier());
    }

    public abstract boolean canAttach(final Device device);
    public abstract void doControl(ChannelProgram channelProgram, Device device);
    public abstract void doRead(ChannelProgram channelProgram, Device device);
    public abstract void doReadBackward(ChannelProgram channelProgram, Device device);
    public abstract void doWrite(ChannelProgram channelProgram, Device device);
    public abstract ChannelType getChannelType();
    public HashSet<Device> getDevices() { return new HashSet<>(_devices.values()); }

    @Override
    public NodeCategory getNodeCategory() { return NodeCategory.Channel; }

    public void routeIo(final ChannelProgram channelProgram) {
        LogManager.logTrace(getNodeName(), "routeIo(%s)", channelProgram.toString());

        var dev = _devices.get(channelProgram._nodeIdentifier);
        if (dev == null) {
            channelProgram.setIoStatus(IoStatus.DeviceIsNotAttached);
        } else {
            switch (channelProgram._function) {
                case Control -> doControl(channelProgram, dev);
                case Read -> doRead(channelProgram, dev);
                case ReadBackward -> doReadBackward(channelProgram, dev);
                case Write -> doWrite(channelProgram, dev);
                default -> channelProgram.setIoStatus(IoStatus.InvalidFunction);
            }
        }

        LogManager.logTrace(getNodeName(), "routeIo(): %s", channelProgram._ioStatus);
    }

    @Override
    public synchronized String toString() {
        return String.format("%s %s:%s",
                             getNodeName(),
                             getNodeCategory(),
                             getChannelType());
    }

    // Default action
    @Override
    public void close() {}
}
