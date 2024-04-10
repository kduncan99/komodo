/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.bearsnake.komodo.baselib.ArraySlice;
import com.bearsnake.komodo.logger.Level;
import com.bearsnake.komodo.logger.LogManager;
import com.kadware.komodo.hardwarelib.exceptions.CannotConnectException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Abstract base class for a hardware node (such as a disk or tape device, a controller, or something like that)
 * Although there is nothing explicitly preventing it, the configuration should most definitely not be altered
 * while a session is active.  Add/remove/update components *only* while the OS is stopped.
 *
 * Our hardware model is a simplified version of the legacy 2200 model.
 * We have one SystemProcessor which does a heck of a lot of stuff, and is required, and There Can Be Only One.
 * We require 1:n InstructionProcessors - there is no architectural upper bound other than common sense.
 *      Each IP runs a separate Java thread, so keep that in mind for performance considerations
 * We require 1:n InputOutputProcessors - there is no architectural upper bound other than common sense
 *      Each IOP runs a separate Java thread, so keep that in mind for performance considerations
 * We require 1:n MainStorageProcessors - there is no architectural upper bound other than common sense
 *      Each MSP has a configurably-sized fixed storage bank, and can support up to {n} dynamically-allocated
 *      additional storage banks, managed by the operating system.
 * The Processors are all interconnected via the Send/Ack UPI business, which is used mainly for IO.
 * Previous comments notwithstanding, the InventoryManager does impose upper limits for the create methods.
 *
 * Connected to the InputOutputProcessor(s) is/are the ChannelModules. The ChannelModule is where data is translated
 *      (if necessary) from Word36 format to byte format.  We need at least one CM for any byte devices, and one for
 *      any word devices. We can have more... we impose no architectural limit on max CMs.
 *
 * We do not implement virtual controller nodes - there seemed to be very little need for doing so.
 *
 * Devices are 'connected' directly to channel modules, and are addressed via a device index.
 */
public abstract class Node {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Class attributes
    //  ----------------------------------------------------------------------------------------------------------------------------

    private static final String LOG_SOURCE = "NODE";

    //TODO  Hardcoded development/debug aids - maybe we could make these configurable later...
    static final boolean LOG_CHANNEL_IOS = true;
    static final boolean LOG_CHANNEL_IO_BUFFERS = true;
    static final boolean LOG_DEVICE_IOS = true;
    static final boolean LOG_DEVICE_IO_BUFFERS = true;
    static final boolean LOG_IO_ERRORS = true;

    /**
     * Category of this node
     */
    final NodeCategory _category;

    /**
     * Unique name of this node
     */
    final String _name;

    /**
     * Set of nodes to which this node connects
     */
    final Set<Node> _ancestors = new HashSet<>();

    /**
     * Map of nodes which connect to this one, keyed by channel address
     */
    Map<Integer, Node> _descendants = new HashMap<>();

    /**
     * Standard constructor
     */
    protected Node(
        final NodeCategory category,
        final String name
    ) {
        _category = category;
        _name = name;
        LogManager.logTrace(name, "ctor");
    }

    /**
     * Indicates whether this Node can connect as a descendant to the candidate ancestor Node.
     */
    public abstract boolean canConnect(Node candidate);

    /**
     * Invoked when a new session is started by the system processor.
     * Any nodes which have work to do to enter the cleared state, should do it here.
     */
    public abstract void clear();

    /**
     * Invoked when the config is built, and before we allow anyone into it.
     */
    public abstract void initialize();

    /**
     * Invoked just before tearing down the configuration.
     */
    public abstract void terminate();

    /**
     * Dumps debug information about this object to the given BufferedWriter
     * Subclasses may override this, but they should call back here before emitting any subclass-specific information
     */
    public void dump(
        final PrintStream out,
        final String indent
    ) {
        out.printf("%sNode %s  Category:%s\n", indent, _name, _category.toString());

        out.printf("%s  Ancestors:%s\n", indent, _ancestors.isEmpty() ? " none" : "");
        for (Node node : _ancestors) {
            out.printf("%s    %s\n", indent, node._name);
        }

        out.printf("%s  Descendants:%s\n", indent, _descendants.isEmpty() ? " none" : "");
        for (Map.Entry<Integer, Node> entry : _descendants.entrySet()) {
            out.printf("%s    [%d]:%s\n", indent, entry.getKey(), entry.getValue()._name);
        }
    }

    /**
     * Connects two nodes as ancestor/descendant, choosing a unique address.
     * Only one such connection may exist between any two nodes, and only between certain categories.
     */
    public static void connect(
        final Node ancestor,
        final Node descendant
    ) throws CannotConnectException {
        int nodeAddress = 0;
        while (ancestor._descendants.containsKey(nodeAddress)) {
            ++nodeAddress;
        }

        connect(ancestor, nodeAddress, descendant);
    }

    /**
     * Connects two nodes in an ancestor/descendant relationship
     */
    public static void connect(
        final Node ancestor,
        final int nodeAddress,
        final Node descendant
    ) throws CannotConnectException {
        LogManager.logTrace(LOG_SOURCE,
                            "connect(ancestor=%s nodeAddress=%d descendant=%s)",
                            ancestor._name,
                            nodeAddress,
                            descendant._name);

        if (!descendant.canConnect(ancestor)) {
            throw new CannotConnectException(String.format("Node %s cannot be an ancestor for Node %s",
                                                           ancestor._name,
                                                           descendant._name));
        }

        //  Is a descendant already connected at the indicated ancestor address?
        if (ancestor._descendants.containsKey(nodeAddress)) {
            throw new CannotConnectException(String.format("Node %s already has a connection at node address %d",
                                                           ancestor._name,
                                                           nodeAddress));
        }

        //  Is this pair already connected?
        if (descendant._ancestors.contains(ancestor)) {
            throw new CannotConnectException(String.format("Node %s is already an ancestor for Node %s",
                                                           ancestor._name,
                                                           descendant._name));
        }

        //  Create the two-way link
        ancestor._descendants.put(nodeAddress, descendant);
        descendant._ancestors.add(ancestor);
        LogManager.logTrace(LOG_SOURCE, "connect() exit");
    }

    /**
     * Deserializes a boolean from the current position in the buffer (see serializeBoolean())
     * For Inquiry operations
     */
    static boolean deserializeBoolean(
        final ByteBuffer buffer
    ) {
        return buffer.get() != 0x00;
    }

    /**
     * Deserializes a string from the current position in the buffer (see serializeString())
     * For Inquiry operations
     */
    static String deserializeString(
        final ByteBuffer buffer
    ) {
        int size = buffer.getInt();
        StringBuilder sb = new StringBuilder();
        for (int sx = 0; sx < size; ++sx) {
            sb.append(buffer.getChar());
        }
        return sb.toString();
    }

    /**
     * Disconnects the given nodes
     */
    static void disconnect(
        final Node ancestor,
        final Node descendant
    ) {
        LogManager.logTrace("NODE",
                            "disconnect(ancestor=%s descendant=%s)",
                            ancestor._name,
                            descendant._name);

        descendant._ancestors.remove(ancestor);
        for (Map.Entry<Integer, Node> entry : ancestor._descendants.entrySet()) {
            if (entry.getValue() == descendant) {
                ancestor._descendants.remove(entry.getKey());
                break;
            }
        }

        LogManager.logTrace("NODE", "disconnect() exit");
    }

    /**
     * Logs the contents of a particular buffer of 36-bit values.
     * Leverages the log method of the buffer itself, which the caller could do just as easily,
     * but we implement this method anyway for consistency with the method below, which does the same thing
     * but for byte buffers.
     * @param logLevel log level (i.e., DEBUG, TRACE, etc)
     * @param caption description of the log data.  No caption is produced if this value is empty.
     * @param slice slice to be logged
     */
    static void logBuffer(
        final Level logLevel,
        final String caption,
        final ArraySlice slice
    ) {
        slice.logOctal(logLevel, LOG_SOURCE, caption);
    }

    /**
     * Logs the contents of a particular buffer of bytes.
     * @param logLevel log level (i.e., DEBUG, TRACE, etc)
     * @param caption description of the log data.  No caption is produced if this value is empty.
     * @param buffer buffer to be logged
     */
    static void logBuffer(
        final Level logLevel,
        final String caption,
        final byte[] buffer
    ) {
        if (!caption.isEmpty()) {
            LogManager.log(logLevel, LOG_SOURCE, "--[ %s ]--", caption);
        }

        int bx = 0;
        int remainingBytes = buffer.length;
        for (int rowIndex = 0; remainingBytes > 0; rowIndex += 16, bx += 16) {
            StringBuilder builder = new StringBuilder();
            builder.append(String.format("%08X:", rowIndex));

            for (int by = bx; by < bx + 16; ++by) {
                if (remainingBytes > 0) {
                    builder.append(String.format("%02X ", buffer[by]));
                    --remainingBytes;
                }
            }

            //  Log the output
            LogManager.log(logLevel, LOG_SOURCE, builder.toString());
        }
    }

    /**
     * Adds a boolean value to the given ByteBuffer encoded as a single byte of value 0x00 or 0x01 for false and true
     * For Inquiry operations
     */
    static void serializeBoolean(
        final ByteBuffer buffer,
        final boolean value
    ) {
        buffer.put(value ? (byte)0x01 : 0x00);
    }

    /**
     * Serializes a string to the given ByteBuffer, represented as an integer indicating the length of the string
     * in character, and then the individual characters which comprise the string.
     * For Inquiry operations
     */
    static void serializeString(
        final ByteBuffer buffer,
        final String value
    ) {
        buffer.putInt(value.length());
        for (int vx = 0; vx < value.length(); ++vx) {
            buffer.putChar(value.charAt(vx));
        }
    }
}
