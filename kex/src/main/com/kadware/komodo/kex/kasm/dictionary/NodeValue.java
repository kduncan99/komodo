/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.dictionary;

import com.kadware.komodo.kex.kasm.Locale;
import com.kadware.komodo.kex.kasm.exceptions.TypeException;
import java.util.HashMap;
import java.util.Map;

/**
 * Class which represents a node value.
 * Because node trees are fairly unstructured, we implement them as such:
 *  N(4) is the value of nodeValue1.get(IntegerValue(4))
 *  N(2,'foo') is nodeValue1.get(IntegerValue(2))  // this returns a nested NodeValue...
 *                   .get(StringValue("foo"))      // ... and this produces the final value.
 * Thus, a node with n dimensions will have (n-1) levels of nested NodeValue objects
 */
public class NodeValue extends Value {

    private final Map<Value, Value> _values = new HashMap<>();

    /**
     * Normal constructor
     * @param locale where this was defined
     * @param flagged (i.e., has leading asterisk)
     */
    public NodeValue(
        final Locale locale,
        final boolean flagged
    ) {
        super(locale, flagged);
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
     * @param locale new value for Locale
     * @param newFlagged new attribute
     * @return new value
     */
    @Override
    public Value copy(
        final Locale locale,
        final boolean newFlagged
    ) throws TypeException {
        throw new TypeException();
    }

    /**
     * Create a new copy of this object, with the given precision value
     * @param locale new value for Locale
     * @param newPrecision new value for precision attribute
     * @return new Value
     * @throws TypeException if object cannot be copied
     */
    @Override
    public Value copy(
        final Locale locale,
        final ValuePrecision newPrecision
    ) throws TypeException {
        throw new TypeException();
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
     * For development / debugging
     */
    public void emitNodeTree(
        final String indent
    ) {
        if (indent.isEmpty()) {
            System.out.println("Node Tree:");
        }
        for (Map.Entry<Value, Value> entry : _values.entrySet()) {
            if (entry.getValue() instanceof NodeValue) {
                System.out.println(indent + entry.getKey().toString() + ":<NodeTree>");
                ((NodeValue) entry.getValue()).emitNodeTree(indent + "    ");
            } else {
                System.out.println(indent + entry.getKey().toString() + ":" + entry.getValue().toString());
            }
        }
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

    @Override public ValueType getType() { return ValueType.Node; }

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

    @Override
    public int hashCode(
    ) {
        int code = _flagged ? 0x40000000 : 0;
        for (Value v : _values.keySet()) {
            code ^= v.hashCode();
        }
        return code;
    }

    /**
     * Retrieve number of entities subordinate to this node
     * @return value
     */
    public int getValueCount() {
        return _values.size();
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
     * For display purposes
     * @return displayable string
     */
    @Override
    public String toString(
    ) {
        return String.format("%s<node>", _flagged ? "*" : "");
    }
}
