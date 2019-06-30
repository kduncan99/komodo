/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.ArraySlice;
import com.kadware.komodo.hardwarelib.exceptions.CannotConnectException;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Abstract base class for a hardware node (such as a disk or tape device, a controller, or something like that)
 */
public abstract class Node {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested enumerations
    //  ----------------------------------------------------------------------------------------------------------------------------

    public static enum Category {

        None(0),
        Processor(1),
        ChannelModule(2),
        Controller(3),
        Device(4);

        /**
         * Unique code for this particular Category
         */
        private final int _code;

        /**
         * Constructor
         * <p>
         * @param code
         */
        Category(
            final int code
        ) {
            _code = code;
        }

        /**
         * Retrieves the unique code for this Category
         * <p>
         * @return
         */
        public int getCode(
        ) {
            return _code;
        }

        /**
         * Converts a code to a Category
         * <p>
         * @param code
         * <p>
         * @return
         */
        public static Category getValue(
            final int code
        ) {
            switch (code) {
                case 1:     return Processor;
                case 2:     return ChannelModule;
                case 3:     return Controller;
                default:    return Device;
            }
        }
    }

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Class attributes
    //  ----------------------------------------------------------------------------------------------------------------------------

    //  Hardcoded development/debug aids - maybe we could make these configurable later...
    protected static final boolean LOG_CHANNEL_IOS = true;
    protected static final boolean LOG_CHANNEL_IO_BUFFERS = true;
    protected static final boolean LOG_DEVICE_IOS = true;
    protected static final boolean LOG_DEVICE_IO_BUFFERS = true;
    protected static final boolean LOG_IO_ERRORS = true;

    /**
     * Category of this node
     */
    private final Category _category;

    /**
     * Unique name of this node
     */
    private final String _name;

    /**
     * Set of nodes to which this node connects
     */
    protected Set<Node> _ancestors = new HashSet<>();

    /**
     * Map of nodes which connect to this one, keyed by channel address
     */
    protected Map<Integer, Node> _descendants = new HashMap<>();

    private static final Logger LOGGER = LogManager.getLogger(Node.class);


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructors
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Standard constructor
     * <p>
     * @param category
     * @param name
     */
    protected Node(
        final Category category,
        final String name
    ) {
        _category = category;
        _name = name;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Accessors
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Getter
     * <p>
     * @return
     */
    public Set<Node> getAncestors(
    ) {
        Set<Node> result = new HashSet<>();
        result.addAll(_ancestors);
        return result;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public Category getCategory(
    ) {
        return _category;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public Map<Integer, Node> getDescendants(
    ) {
        Map<Integer, Node> result = new HashMap<>();
        result.putAll(_descendants);
        return result;
    }

    /**
     * Getter
     * <p>
     * @return
     */
    public String getName(
    ) {
        return _name;
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Abstract methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Indicates whether this Node can connect as a descendant to the candidate ancestor Node.
     * <p>
     * @param candidate
     * <p>
     * @return
     */
    public abstract boolean canConnect(
        final Node candidate
    );

    /**
     * Invoked when the config is built, and before we allow anyone into it.
     */
    public abstract void initialize();

    /**
     * Invoked when this object is the source of an IO which has been cancelled or completed
     * <p>
     * @param source indicates which Node was responsible for signalling us (it might be us)
     */
    //???? in light of workerSignal()... do we need this one?  Or what?
    public abstract void signal(
        final Node source
    );

    /**
     * Invoked just before tearing down the configuration.
     */
    public abstract void terminate();


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Instance methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Dumps debug information about this object to the given BufferedWriter
     * Subclasses may override this, but they should call back here before emitting any subclass-specific information
     * <p>
     * @param writer
     */
    public void dump(
        final BufferedWriter writer
    ) {
        try {
            writer.write(String.format("Node %s  Category:%s",
                                       getName(),
                                       getCategory().toString()));

            writer.write("  Ancestors:");
            for (Node node : _ancestors) {
                writer.write(node.getName());
                writer.write(" ");
            }

            writer.write("  Descendants:");
            for (Map.Entry<Integer, Node> entry : _descendants.entrySet()) {
                writer.write(String.format("[%d]:%s ", entry.getKey(), entry.getValue().getName()));
            }

            writer.newLine();
        } catch (IOException ex) {
            LOGGER.catching(ex);
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Static methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Connects two nodes as ancestor/descendant, choosing a unique address.
     * Only one such connection may exist between any two nodes, and only between certain categories.
     * <p>
     * @param ancestor
     * @param descendant
     * <p>
     * @throws CannotConnectException
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
     * <p>
     * @param ancestor
     * @param nodeAddress
     * @param descendant
     * <p>
     * @throws CannotConnectException
     */
    public static void connect(
        final Node ancestor,
        final int nodeAddress,
        final Node descendant
    ) throws CannotConnectException {
        if (!descendant.canConnect(ancestor)) {
            throw new CannotConnectException(String.format("Node %s cannot be an ancestor for Node %s",
                                                           ancestor.getName(),
                                                           descendant.getName()));
        }

        //  Is a descendant already connected at the indicated ancestor address?
        if (ancestor._descendants.containsKey(nodeAddress)) {
            throw new CannotConnectException(String.format("Node %s already has a connection at node address %d",
                                                           ancestor.getName(),
                                                           nodeAddress));
        }

        //  Is this pair already connected?
        if (descendant._ancestors.contains(ancestor)) {
            throw new CannotConnectException(String.format("Node %s is already an ancestor for Node %s",
                                                           ancestor.getName(),
                                                           descendant.getName()));
        }

        //  Create the two-way link
        ancestor._descendants.put(nodeAddress, descendant);
        descendant._ancestors.add(ancestor);
    }

    /**
     * Deserializes a boolean from the current position in the buffer (see serializeBoolean())
     * <p>
     * @param buffer
     * <p>
     * @return
     */
    public static boolean deserializeBoolean(
        final ByteBuffer buffer
    ) {
        return buffer.get() != 0x00;
    }

    /**
     * Deserializes a string from the current position in the buffer (see serializeString())
     * <p>
     * @param buffer
     * <p>
     * @return
     */
    public static String deserializeString(
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
     * <p>
     * @param ancestor
     * @param descendant
     */
    public static void disconnect(
        final Node ancestor,
        final Node descendant
    ) {
        descendant._ancestors.remove(ancestor);
        for (Map.Entry<Integer, Node> entry : ancestor._descendants.entrySet()) {
            if (entry.getValue() == descendant) {
                ancestor._descendants.remove(entry.getKey());
                break;
            }
        }
    }

    /**
     * Logs the contents of a particular buffer of 36-bit values.
     * Leverages the log method of the buffer itself, which the caller could do just as easily,
     * but we implement this method anyway for consistency with the method below, which does the same thing
     * but for byte buffers.
     * <p>
     * @param logger destination for the output
     * @param logLevel log level (i.e., DEBUG, TRACE, etc)
     * @param caption description of the log data.  No caption is produced if this value is empty.
     * @param slice slice to be logged
     */
    public static void logBuffer(
        final org.apache.logging.log4j.Logger logger,
        final org.apache.logging.log4j.Level logLevel,
        final String caption,
        final ArraySlice slice
    ) {
        slice.logOctal(logger, logLevel, caption);
    }

    /**
     * Logs the contents of a particular buffer of bytes.
     * <p>
     * @param logger destination for the output
     * @param logLevel log level (i.e., DEBUG, TRACE, etc)
     * @param caption description of the log data.  No caption is produced if this value is empty.
     * @param buffer buffer to be logged
     */
    public static void logBuffer(
        final org.apache.logging.log4j.Logger logger,
        final org.apache.logging.log4j.Level logLevel,
        final String caption,
        final byte[] buffer
    ) {
        if (!caption.isEmpty()) {
            logger.printf(logLevel, "--[ %s ]--", caption);
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
            logger.printf(logLevel, builder.toString());
        }
    }

    /**
     * Adds a boolean value to the given ByteBuffer encoded as a single byte of value 0x00 or 0x01 for false and true
     * <p>
     * @param buffer
     * @param value
     */
    public static void serializeBoolean(
        final ByteBuffer buffer,
        final boolean value
    ) {
        buffer.put(value ? (byte)0x01 : 0x00);
    }

    /**
     * Serializes a string to the given ByteBuffer, represented as an integer indicating the length of the string
     * in character, and then the individual characters which comprise the string.
     * <p>
     * @param buffer
     * @param value
     */
    public static void serializeString(
        final ByteBuffer buffer,
        final String value
    ) {
        buffer.putInt(value.length());
        for (int vx = 0; vx < value.length(); ++vx) {
            buffer.putChar(value.charAt(vx));
        }
    }
}
