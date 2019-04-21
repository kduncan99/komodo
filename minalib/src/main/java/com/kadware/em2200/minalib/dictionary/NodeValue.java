/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.dictionary;

import com.kadware.em2200.baselib.exceptions.*;
import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.Diagnostics;
import com.kadware.em2200.minalib.exceptions.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Class which represents a node value
 */
public class NodeValue extends Value {

    private final Map<Value, Value> _values = new HashMap<>();

    /**
     * Normal constructor
     * @param flagged (i.e., has leading asterisk)
     */
    public NodeValue(
        final boolean flagged
    ) {
        super(flagged);
    }

    /**
     * Compares an object to this object
     * @param obj comparison object
     * @return -1 if this object sorts before (is less than) the given object
     *         +1 if this object sorts after (is greater than) the given object,
     *          0 if both objects sort to the same position (are equal)
     * @throws TypeException if there is no reasonable way to compare the objects
     */
    @Override
    public int compareTo(
        final Object obj
    ) throws TypeException {
        throw new TypeException();
    }

    /**
     * Create a new copy of this object, with the given flagged value
     * @param newFlagged new attribute
     * @return new value
     */
    @Override
    public NodeValue copy(
        final boolean newFlagged
    ) {
        return new NodeValue(newFlagged);
    }

    /**
     * Check for equality
     * @param obj comparison object
     * @return true if comparison object equals this one
     */
    @Override
    public boolean equals(
        final Object obj
    ) {
        if (!(obj instanceof NodeValue)) {
            return false;
        }

        NodeValue node = (NodeValue)obj;
        if (node._values.size() != _values.size()) {
            return false;
        }

        for (Map.Entry<Value, Value> entry : _values.entrySet()) {
            Value key = entry.getKey();
            Value value = entry.getValue();

            Value compValue = node._values.get(key);
            if (!value.equals(compValue)) {
                return false;
            }
        }

        return true;
    }

    /**
     * Getter
     * @return value
     */
    @Override
    public ValueType getType(
    ) {
        return ValueType.Node;
    }

    /**
     * Transform the value to an FloatingPointValue, if possible
     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
     * @param diagnostics where we post any necessary diagnostics
     * @return new value
     * @throws TypeException because we cannot do this
     */
    @Override
    public FloatingPointValue toFloatingPointValue(
        final Locale locale,
        Diagnostics diagnostics
    ) throws TypeException {
        throw new TypeException();
    }

    /**
     * Transform the value to an IntegerValue, if possible
     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
     * @param diagnostics where we post any necessary diagnostics
     * @return new value
     * @throws TypeException because we cannot do this
     */
    @Override
    public IntegerValue toIntegerValue(
        final Locale locale,
        Diagnostics diagnostics
    ) throws TypeException {
        throw new TypeException();
    }

    /**
     * Transform the value to a StringValue, if possible
     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
     * @param diagnostics where we post any necessary diagnostics
     * @return new value
     * @throws TypeException because we cannot do this
     */
    @Override
    public StringValue toStringValue(
        final Locale locale,
        CharacterMode characterMode,
        Diagnostics diagnostics
    ) throws TypeException {
        throw new TypeException();
    }

    /**
     * Retrieves the value associated with a given selector
     * @param selectorValue chosen selector value
     * @return value associated with the selector
     * @throws NotFoundException if we don't have a value for the selector
     */
    public Value getValue(
        final Value selectorValue
    ) throws NotFoundException {
        Value value = _values.get(selectorValue);
        if (value == null) {
            throw new NotFoundException();
        } else {
            return value;
        }
    }

    /**
     * Establishes or replaces a value for a given selector
     * @param key selector key
     * @param value value associated with this key
     */
    public void setValue(
        final Value key,
        final Value value
    ) {
        _values.put(key, value);
    }

    /**
     * Removes a value given a selector
     * @param key selector key
     * @throws NotFoundException if the key is not a selector
     */
    public void deleteValue(
        final Value key
    ) throws NotFoundException {
        Value value = _values.get(key);
        if (value == null) {
            throw new NotFoundException();
        } else {
            _values.remove(key);
        }
    }

    /**
     * For display purposes
     * @return displayable string
     */
    @Override
    public String toString(
    ) {
        return String.format("%s<node>", getFlagged() ? "*" : "");
    }
}
