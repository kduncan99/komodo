/*
 * Copyright (c) 2018 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.dictionary;

import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.Diagnostics;
import com.kadware.em2200.minalib.exceptions.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Class which represents a value
 */
public class NodeValue extends Value {

    private static class Selector {

        public final long[] _value;

        public Selector(
            final long[] value
        ) {
            _value = new long[2];
            _value[0] = value[0];
            _value[1] = value[1];
        }

        @Override
        public boolean equals(
            final Object obj
        ) {
            return (obj instanceof Selector) && Arrays.equals(_value, ((Selector)obj)._value);
        }
    }

    private final Map<Selector, Value> _values = new HashMap<>();

    /**
     * Normal constructor
     * <p>
     * @param flagged (i.e., has leading asterisk)
     */
    public NodeValue(
        final boolean flagged
    ) {
        super(flagged);
    }

    /**
     * Compares an object to this object
     * <p>
     * @param obj
     * <p>
     * @return -1 if this object sorts before (is less than) the given object
     *         +1 if this object sorts after (is greater than) the given object,
     *          0 if both objects sort to the same position (are equal)
     * <p>
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
     * <p>
     * @param newFlagged
     * <p>
     * @return
     */
    @Override
    public NodeValue copy(
        final boolean newFlagged
    ) {
        return new NodeValue(newFlagged);
    }

    /**
     * Create a new copy of this object, with the given signed value
     * <p>
     * @param newSigned
     * <p>
     * @return
     * <p>
     * @throws TypeException
     */
    @Override
    public NodeValue copy(
        final Signed newSigned
    ) throws TypeException {
        throw new TypeException();
    }

    /**
     * Create a new copy of this object, with the given precision value
     * <p>
     * @param newPrecision
     * <p>
     * @return
     * <p>
     * @throws TypeException
     */
    @Override
    public NodeValue copy(
        final Precision newPrecision
    ) throws TypeException {
        throw new TypeException();
    }

    /**
     * Check for equality
     * <p>
     * @param obj
     * <p>
     * @return
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

        for (Map.Entry<Selector, Value> entry : _values.entrySet()) {
            Selector key = entry.getKey();
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
     * <p>
     * @return
     */
    @Override
    public ValueType getType(
    ) {
        return ValueType.Node;
    }

    /**
     * Transform the value to an FloatingPointValue, if possible
     * <p>
     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
     * @param diagnostics where we post any necessary diagnostics
     * <p>
     * @return
     * <p>
     * @throws TypeException
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
     * <p>
     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
     * @param diagnostics where we post any necessary diagnostics
     * <p>
     * @return
     * <p>
     * @throws TypeException
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
     * <p>
     * @param locale locale of the instigating bit of text, for reporting diagnostics as necessary
     * @param characterMode desired character mode
     * @param diagnostics where we post any necessary diagnostics
     * <p>
     * @return
     * <p>
     * @throws TypeException
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
     * <p>
     * @param selector
     * <p>
     * @return
     * <p>
     * @throws NotFoundException
     */
    public Value getValue(
        final IntegerValue selector
    ) throws NotFoundException {
        Value value = _values.get(new Selector(selector.getValue()));
        if (value == null) {
            throw new NotFoundException();
        } else {
            return value;
        }
    }

    /**
     * Establishes or replaces a value for a given selector
     * <p>
     * @param selector
     * @param value
     */
    public void setValue(
        final IntegerValue selector,
        final Value value
    ) {
        _values.put(new Selector(selector.getValue()), value);
    }

    /**
     * Removes a value given a selector
     * <p>
     * @param selector
     * <p>
     * @throws NotFoundException
     */
    public void deleteValue(
        final IntegerValue selector
    ) throws NotFoundException {
        Selector sel = new Selector(selector.getValue());
        Value value = _values.get(sel);
        if (value == null) {
            throw new NotFoundException();
        } else {
            _values.remove(sel);
        }
    }
}
