/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.dictionary;

import com.kadware.komodo.baselib.exceptions.NotFoundException;
import com.kadware.komodo.kex.kasm.Locale;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a collection of values, tagged with a label
 * Dictionaries are structured as such:
 *  SystemDictionary (contains system labels which may be overwritten)
 *  GlobalDictionary (contains labels defined by the program which are to be externalized)
 *  MainDictionary (top level general dictionary, containing top-level non-global labels)
 *  SubDictionary (subordinate to MainDictionary, created during proc definition, execution, etc)
 */
public class Dictionary {

    private static class ValueEntry {

        public final Locale _locale;
        public final Value _value;

        private ValueEntry(
            final Locale locale,
            final Value value
        ) {
            _locale = locale;
            _value = value;
        }
    }

    public static class ValueInfo {

        public final String _label;
        public final int _level;
        public final Locale _locale;
        public final Value _value;

        private ValueInfo(
            final String label,
            final int level,
            final Locale locale,
            final Value value
        ) {
            _label = label;
            _level = level;
            _locale = locale;
            _value = value;
        }
    }

    private final int _level;
    private final Dictionary _upperLevelDictionary;
    private final Map<String, ValueEntry> _values = new HashMap<>();

    /**
     * Constructor for top-level (System) dictionary
     * To be invoked by SystemDictionary constructor only
     */
    Dictionary(
    ) {
        _level = 0;
        _upperLevelDictionary = null;
    }

    /**
     * Constructor for any other dictionary
     * @param upperLevelDictionary reference to higher level dictionary
     */
    public Dictionary(
        final Dictionary upperLevelDictionary
    ) {
        _level = upperLevelDictionary._level;
        _upperLevelDictionary = upperLevelDictionary;
    }

    /**
     * Adds or replaces a value corresponding to the given label, in either this dictionary
     * or some higher-level dictionary.
     * The label will be folded to uppercase.
     * @param relativeLevel - zero means this dictionary, one means the next higher-level, etc.
     * @param label label to be used
     * @param value value to be associated with the label
     */
    public void addValue(
        final int relativeLevel,
        final String label,
        final Locale locale,
        final Value value
    ) {
        if ((relativeLevel > 0) && (_upperLevelDictionary != null) && !(_upperLevelDictionary instanceof SystemDictionary)) {
            _upperLevelDictionary.addValue(relativeLevel - 1, label, locale, value);
        } else {
            _values.put(label.toUpperCase(), new ValueEntry(locale, value));
        }
    }

    /**
     * Retrieve ValueInfo objects for all values in this dictionary
     */
    public ValueInfo[] getAllValueInfos() {
        ValueInfo[] result = new ValueInfo[_values.size()];
        int rx = 0;
        for (Map.Entry<String, ValueEntry> entry : _values.entrySet()) {
            result[rx++] = new ValueInfo(entry.getKey(), _level, entry.getValue()._locale, entry.getValue()._value);
        }
        return result;
    }

    /**
     * Retrieves a particular ValueInfo object
     * @param label label of the object to be retrieved
     * @return object if found
     * @throws NotFoundException if the value is not found in this, or any upper-level dictionary
     */
    public ValueInfo getValueInfo(
        final String label
    ) throws NotFoundException {
        ValueEntry entry = _values.get(label.toUpperCase());
        if (entry != null) {
            return new ValueInfo(label.toUpperCase(), _level, entry._locale, entry._value);
        }

        if (_upperLevelDictionary != null) {
            return _upperLevelDictionary.getValueInfo(label);
        }

        throw new NotFoundException(label);
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
     * @param maxLength maximum length allowed for a label
     * @return true if label is valid
     */
    public static boolean isValidLabel(
        final String label,
        final int maxLength
    ) {
        String pattern = String.format("[a-zA-Z_$][a-zA-Z0-9_$]{0,%d}", maxLength);
        return label.matches(pattern);
    }
}
