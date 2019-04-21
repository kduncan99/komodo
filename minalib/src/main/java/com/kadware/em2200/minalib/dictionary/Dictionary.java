/*
 * Copyright (c) 2018-2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.dictionary;

import com.kadware.em2200.baselib.exceptions.*;
import java.util.HashMap;
import java.util.TreeSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents a collection of values, tagged with a label
 * Dictionaries are structured as such:
 *  SystemDictionary (contains system labels which may be overwritten)
 *  GlobalDictionary (contains labels defined by the program which are to be externalized)
 *  MainDictionary (top level general dictionary, containing top-level non-global labels)
 *  SubDictionary (subordinate to MainDictionary, created during proc definition, execution, etc)
 */
public class Dictionary {

    public class ValueAndLevel {
        final public int _level;
        final public Value _value;

        public ValueAndLevel(
            final int level,
            final Value value
        ) {
            _level = level;
            _value = value;
        }
    }

    private final Dictionary _upperLevelDictionary;
    private final Map<String, Value> _values = new HashMap<>();

    /**
     * Constructor for top-level (System) dictionary
     */
    public Dictionary(
    ) {
        _upperLevelDictionary = null;
    }

    /**
     * Constructor for any other dictionary
     * @param upperLevelDictionary reference to higher level dictionary
     */
    public Dictionary(
        final Dictionary upperLevelDictionary
    ) {
        _upperLevelDictionary = upperLevelDictionary;
    }

    /**
     * Adds or replaces a value corresponding to the given label, in either this dictionary
     * or some higher-level dictionary.
     * The label will be folded to uppercase.
     * @param level - zero means this dictionary, one means the next higher-level, etc.
     * @param label label to be used
     * @param value value to be associated with the label
     */
    public void addValue(
        final int level,
        final String label,
        final Value value
    ) {
        if ((_upperLevelDictionary != null) && (level > 0)) {
            _upperLevelDictionary.addValue(level - 1, label, value);
        } else {
            _values.put(label.toUpperCase(), value);
        }
    }

    /**
     * Getter
     * @return list of labels
     */
    public Set<String> getLabels(
    ) {
        Set<String> result = new TreeSet<>();
        if (_upperLevelDictionary != null) {
            result.addAll(_upperLevelDictionary.getLabels());
        }
        result.addAll(_values.keySet());
        return result;
    }

    /**
     * Retrieves a particular Value object
     * @param label label to be retrieved
     * @return object if found
     * @throws NotFoundException if the value is not found in this, or any upper-level dictionary
     */
    public Value getValue(
        final String label
    ) throws NotFoundException {
        Value result = _values.get(label.toUpperCase());
        if ((result == null) && (_upperLevelDictionary != null)) {
            result = _upperLevelDictionary.getValue(label);
        }
        if (result == null) {
            throw new NotFoundException(label);
        }
        return result;
    }

    /**
     * Retrieves a particular Value object along with its level, 0 representing the current dictionary,
     * anything greater representing higher-level dictionaries.
     * @param label label to be retrieved
     * @return object if found
     * @throws NotFoundException if the value is not found in this, or any upper-level dictionary
     */
    public ValueAndLevel getValueAndLevel(
            final String label
    ) throws NotFoundException {
        Value v = _values.get(label.toUpperCase());
        if (v == null) {
            if (_upperLevelDictionary != null) {
                ValueAndLevel upperResult = _upperLevelDictionary.getValueAndLevel( label );
                return new ValueAndLevel(upperResult._level + 1, upperResult._value);
            } else {
                throw new NotFoundException( label );
            }
        }

        return new ValueAndLevel(0, v);
    }

    /**
     * Checks for the existence of a particular Value object
     * @param label label to be queried
     * @return true if object exists, else false
     */
    public boolean hasValue(
        final String label
    ) {
        return (_values.containsKey(label.toUpperCase())
                || ((_upperLevelDictionary != null) && _upperLevelDictionary.hasValue(label)));
    }

    /**
     * Tests a label to see if it is valid... i.e., contains one to twelve characters consisting of:
     *      upper- and lower-case alphabetic characters
     *      digits (but cannot start with a digit)
     *      '$'
     *      '_'
     * @param label label to be checked
     * @return true if label is valid
     */
    public static boolean isValidLabel(
        final String label
    ) {
        return label.matches("[a-zA-Z_$][a-zA-Z0-9_$]{0,11}");
    }

    /**
     * Tests a label to see if it is valid... i.e., contains one to twelve characters consisting of:
     *      upper- and lower-case alphabetic characters
     *      digits (but cannot start with a digit)
     *      '$' (but cannot start with '$')
     *      '_'
     * @param label checks whether the label is valid
     * @return true if label is valid
     */
    public static boolean isValidUserLabel(
        final String label
    ) {
        return label.matches("[a-zA-Z_][a-zA-Z0-9_$]{0,11}");
    }
}
