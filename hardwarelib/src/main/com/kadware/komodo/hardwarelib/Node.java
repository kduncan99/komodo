/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.ArraySlice;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * Abstract base class for a hardware node (such as a disk or tape device, a controller, or something like that)
 */
@SuppressWarnings("Duplicates")
public abstract class Node {

    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Nested things
    //  ----------------------------------------------------------------------------------------------------------------------------

    public enum NodeCategory {

        Processor(1),
        ChannelModule(2),
        //  Controller(3),  not used
        Device(4),
        InvalidCategory(077);

        private final int _code;
        NodeCategory(int code) { _code = code; }
        public int getCode() { return _code; }

        public static NodeCategory getValue(
            final int code
        ) {
            //  We do not model Controller Nodes, but if we did, they'd be case 3
            return switch (code) {
                case 1 -> Processor;
                case 2 -> ChannelModule;
                case 4 -> Device;
                default -> InvalidCategory;
            };
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Class attributes
    //  ----------------------------------------------------------------------------------------------------------------------------

    //TODO  Hardcoded development/debug aids - maybe we could make these configurable later...
    static final boolean LOG_CHANNEL_IOS = true;
    static final boolean LOG_CHANNEL_IO_BUFFERS = true;
    static final boolean LOG_DEVICE_IOS = true;
    static final boolean LOG_DEVICE_IO_BUFFERS = true;
    static final boolean LOG_IO_ERRORS = true;

    /**
     * Category of this node
     */
    public final NodeCategory _category;

    /**
     * Unique name of this node
     */
    public final String _name;

    /**
     * Set of nodes to which this node connects
     */
    final Set<Node> _ancestors = new HashSet<>();

    /**
     * Map of nodes which connect to this one, keyed by channel address
     */
    Map<Integer, Node> _descendants = new HashMap<>();

    /**
     * Logger for all subclasses, for instance methods
     */
    protected final Logger _logger;


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Constructors
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Standard constructor
     */
    protected Node(
        final NodeCategory category,
        final String name
    ) {
        _category = category;
        _name = name;
        _logger = LogManager.getLogger(_category.name() + "-" + _name);
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Abstract methods
    //  ----------------------------------------------------------------------------------------------------------------------------

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


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Instance methods
    //  ----------------------------------------------------------------------------------------------------------------------------

    /**
     * Dumps debug information about this object to the given BufferedWriter
     * Subclasses may override this, but they should call back here before emitting any subclass-specific information
     */
    public void dump(
        final BufferedWriter writer
    ) {
        try {
            writer.write(String.format("Node %s  Category:%s\n", _name, _category.toString()));

            writer.write(String.format("  Ancestors:%s\n", _ancestors.isEmpty() ? " none" : ""));
            for (Node node : _ancestors) {
                writer.write("    " + node._name + "\n");
            }

            writer.write(String.format("  Descendants:%s\n", _descendants.isEmpty() ? " none" : ""));
            for (Map.Entry<Integer, Node> entry : _descendants.entrySet()) {
                writer.write(String.format("    [%d]:%s\n", entry.getKey(), entry.getValue()._name));
            }
        } catch (IOException ex) {
            _logger.catching(ex);
        }
    }


    //  ----------------------------------------------------------------------------------------------------------------------------
    //  Static methods
    //  ----------------------------------------------------------------------------------------------------------------------------

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
     * Logs the contents of a particular buffer of 36-bit values.
     * Leverages the log method of the buffer itself, which the caller could do just as easily,
     * but we implement this method anyway for consistency with the method below, which does the same thing
     * but for byte buffers.
     * @param logger destination for the output
     * @param logLevel log level (i.e., DEBUG, TRACE, etc)
     * @param caption description of the log data.  No caption is produced if this value is empty.
     * @param slice slice to be logged
     */
    static void logBuffer(
        final Logger logger,
        final Level logLevel,
        final String caption,
        final ArraySlice slice
    ) {
        slice.logOctal(logger, logLevel, caption);
    }

    /**
     * Logs the contents of a particular buffer of bytes.
     * @param logger destination for the output
     * @param logLevel log level (i.e., DEBUG, TRACE, etc)
     * @param caption description of the log data.  No caption is produced if this value is empty.
     * @param buffer buffer to be logged
     */
    static void logBuffer(
        final Logger logger,
        final Level logLevel,
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
