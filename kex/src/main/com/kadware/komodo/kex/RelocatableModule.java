/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex;

import com.kadware.komodo.baselib.FieldDescriptor;
import com.kadware.komodo.baselib.Word36;
import com.kadware.komodo.kex.kasm.exceptions.ParameterException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Represents a relocatable module in the external (kex) environment.
 * Contains a portion of code which is linked with potentially many other such portions
 * to create an absolute module (loadable executable).
 * This class is vaguely analagous to the SYSLIB ROR routines.
 */
public class RelocatableModule {

    //  Constants --------------------------------------------------------------

    public static final int MAX_LOCATION_COUNTER_INDEX = 511;


    //  Inner classes ----------------------------------------------------------

    /**
     * Contains all the information necessary to describe a single relocatable word
     */
    public static class RelocatableWord extends Word36 {

        public final RelocatableItem[] _relocatableItems;

        public RelocatableWord(
            final Word36 baseValue,
            final RelocatableItem[] relocatableItems
        ) {
            super(baseValue);
            _relocatableItems = relocatableItems;
        }
    }

    /**
     * Base class for a single relocatable item
     */
    public static abstract class RelocatableItem {

        public final FieldDescriptor _fieldDescriptor;  //  indicates the bits affected by this relocation
        public final boolean _subtraction;              //  indicates the relocation is negative (to be subtracted)

        public RelocatableItem(
            final FieldDescriptor fieldDescriptor,
            final boolean subtraction
        ) {
            _fieldDescriptor = fieldDescriptor;
            _subtraction = subtraction;
        }
    }

    /**
     * Describes a relocatable item which depends upon the mapped location of a particular location counter
     */
    public static class RelocatableItemLocationCounter extends RelocatableItem {

        public final int _locationCounterIndex;

        public RelocatableItemLocationCounter(
            final int locationCounterIndex,
            final FieldDescriptor fieldDescriptor,
            final boolean subtraction
        ) {
            super(fieldDescriptor, subtraction);
            _locationCounterIndex = locationCounterIndex;
        }

        @Override
        public String toString() {
            return _fieldDescriptor.toString() + (_subtraction ? "-" : "+") + "$(" + _locationCounterIndex + ")";
        }
    }

    /**
     * Describes a relocatable item which depends upon the value of a currently-undefined symbol
     */
    public static class RelocatableItemSymbol extends RelocatableItem {

        public final String _undefinedSymbol;

        public RelocatableItemSymbol(
            final String undefinedSymbol,
            final FieldDescriptor fieldDescriptor,
            final boolean subtraction
        ) {
            super(fieldDescriptor, subtraction);
            _undefinedSymbol = undefinedSymbol;
        }

        @Override
        public String toString() {
            return _fieldDescriptor.toString() + (_subtraction ? "-" : "+") + _undefinedSymbol;
        }
    }

    /**
     * Describes an 'entry point' - in actuality, this is used for any symbol which is externalized
     * such that the linker (or any other entity) has access thereto.
     */
    public static abstract class EntryPoint {

        public final String _name;
        public final long _value;

        public EntryPoint(
            final String name,
            final long value
        ) {
            _name = name;
            _value = value;
        }
    }

    /**
     * An absolute value externalized from the generated code
     */
    public static class AbsoluteEntryPoint extends EntryPoint {

        public AbsoluteEntryPoint(
            final String name,
            final long value
        ) {
            super(name, value);
        }
    }

    /**
     * A location-counter-based symbol, externalized from the generated code.
     * It *could* be used as an entry-point for execution.
     */
    public static class RelativeEntryPoint extends EntryPoint {

        public final int _locationCounterIndex;

        public RelativeEntryPoint(
            final String name,
            final int value,
            final int locationCounterIndex
        ) {
            super(name, value);
            _locationCounterIndex = locationCounterIndex;
        }
    }

    /**
     * Base class for all control information entities
     */
    public static abstract class ControlInformation {

        final int _infoClass;           //  identifies the class of this entry
        final String _identifier;       //  meaning depends on the subclass - this could be null

        ControlInformation(
            final int infoClass
        ) {
            _infoClass = infoClass;
            _identifier = null;
        }

        ControlInformation(
            final int infoClass,
            final String identifier
        ) {
            _infoClass = infoClass;
            _identifier = identifier;
        }
    }

    /**
     * INFO 10 control information
     * Used to describe location counter pools which require extended mode
     */
    public static class Info10ControlInformation extends ControlInformation {

        final int _locationCounterIndex;

        public Info10ControlInformation(
            final int locationCounterIndex
        ) throws ParameterException {
            super(10);
            if ((locationCounterIndex < 0) || (locationCounterIndex > MAX_LOCATION_COUNTER_INDEX)) {
                throw new ParameterException("Invalid location counter index");
            }
            _locationCounterIndex = locationCounterIndex;
        }
    }

    /**
     * Describes a location counter pool
     */
    public static class RelocatablePool {

        public final int _locationCounterIndex;
        public final boolean _requiresExtendedMode;
        public final RelocatableWord[] _content;

        RelocatablePool(
            final int lcIndex,
            final RelocatableWord[] content,
            final boolean requiresExtendedMode
        ) {
            _content = content;
            _locationCounterIndex = lcIndex;
            _requiresExtendedMode = requiresExtendedMode;
        }
    }


    //  Data -------------------------------------------------------------------

    /**
     * Contains all the control info provided by the entity which is generating the code we contain.
     * Keyed by the info class value.
     */
    private final TreeMap<Integer, List<ControlInformation>> _controlInformation = new TreeMap<>();

    private final TreeMap<String, EntryPoint> _entryPoints = new TreeMap<>();

    /**
     * Contains all the information necessary to describe the contents of a single location counter pool.
     * This includes (but is not limited to) the contiguous set of RelocatableWords defined for the pool.
     * Note that a location counter pool can be empty - as is the case for LC's 0 and 1 (normally, anyway).
     * Keyed by location counter index
     */
    private final TreeMap<Integer, RelocatablePool> _locationCounterPools = new TreeMap<>();

    private int _elementTableFlagBits;
    private final String _moduleName;
    private final boolean _afcmClear;               //  AFCM clear is required for the module
    private final boolean _afcmSet;                 //  AFCM set is required for this module
    private final boolean _quarterWordSet;          //  This module is QW sensitive
    private final boolean _thirdWordSet;            //  This module is TW sensitive
    private final RelativeEntryPoint _startAddress; //  If this module as a program start address


    //  Constructor ------------------------------------------------------------

    public RelocatableModule(
        final String moduleName,
        final boolean afcmClear,
        final boolean afcmSet,
        final boolean qwSet,
        final boolean twSet,
        final RelativeEntryPoint startAddress   // could be null
    ) {
        _moduleName = moduleName;
        _afcmClear = afcmClear;
        _afcmSet = afcmSet;
        _quarterWordSet = qwSet;
        _thirdWordSet = twSet;
        _startAddress = startAddress;
    }


    //  Code -------------------------------------------------------------------

    /**
     * Adds a ControlInformation object to the current set of such objects
     * @param infoObject object to be added
     */
    public void addControlInformation(
        final ControlInformation infoObject
    ) {
        if (!_controlInformation.containsKey(infoObject._infoClass)) {
            _controlInformation.put(infoObject._infoClass, new LinkedList<>());
        }
        _controlInformation.get(infoObject._infoClass).add(infoObject);
    }

    /**
     * Adds a new entry point to the existing entry point table.
     * If an entry point with the given name already exists, it is overwritten.
     * Note that this module is case-sensitive; however, the output of this module on disk
     * is in Fieldata, and therefore is *not* case-sensitive - thus, it is possible to
     * create an invalid relocatable element by providing two entry points which have
     * names differing only in case.
     * @param entryPoint EntryPoint object to be added
     */
    public void addEntryPoint(
        final EntryPoint entryPoint
    ) {
        _entryPoints.put(entryPoint._name, entryPoint);
    }

    /**
     * Sets the entire content of ControlInformation objects for this module.
     * Any existing content is removed.
     */
    public void establishControlInformation(
        final ControlInformation[] content
    ) {
        _controlInformation.clear();
        for (ControlInformation ci : content) {
            if (!_controlInformation.containsKey(ci._infoClass)) {
                _controlInformation.put(ci._infoClass, new LinkedList<>());
            }
            _controlInformation.get(ci._infoClass).add(ci);
        }
    }

    /**
     * Establishes the flag bits which will be stored in the element table index
     * if and when this module is written to an element.
     * @param flagBits The bits are normally stored in T1 of word +03 in the element table item.
     *                 Bit 0: table item is deleted
     *                 Bit 5: Arithmetic Fault non-interrupt mode requested
     *                 Bit 6: Arithmetic Fault compatibility mode requested
     *                 Bit 9: Code is third-word sensitive
     *                 Bit 10: Code is quarter-word sensitive
     *                 Bit 11: Element is marked in error
     */
    public void establishElementTableFlagBits(
        final int flagBits
    ) {
        _elementTableFlagBits = flagBits;
    }

    /**
     * Sets the entire content of EntryPoint objects for this module.
     * Any existing content is removed.
     */
    public void establishEntryPoints(
        final EntryPoint[] content
    ) {
        _entryPoints.clear();
        for (EntryPoint ep : content) {
            _entryPoints.put(ep._name, ep);
        }
    }

    /**
     * Establishes the entirety of a particular location counter pool.
     * If a pool already exists for the given lc index, it is replaced.
     * @param locationCounterIndex index of the location counter pool
     * @param content content to be established
     */
    public void establishLocationCounterPool(
        final int locationCounterIndex,
        final boolean requiresExtendedMode,
        final RelocatableWord[] content
    ) throws ParameterException {
        if ((locationCounterIndex < 0) || (locationCounterIndex > MAX_LOCATION_COUNTER_INDEX)) {
            throw new ParameterException("Invalid location counter index");
        }

        RelocatablePool relPool = new RelocatablePool(locationCounterIndex, content, requiresExtendedMode);
        _locationCounterPools.put(locationCounterIndex, relPool);
    }

    /**
     * Retrieves entry points (externalized symbols)
     */
    public Map<String, EntryPoint> getEntryPoints() {
        return _entryPoints;
    }

    /**
     * Retrieves an ordered copy of the set of all location counter indices
     */
    public Set<Integer> getEstablishedLocationCounterIndices() {
        return new TreeSet<>(_locationCounterPools.keySet());
    }

    /**
     * Retrieves the element table flag bits
     */
    public int getElementTableFlagBits() {
        return _elementTableFlagBits;
    }

    public RelocatablePool getLocationCounterPool(
        final int locationCounterIndex
    ) throws ParameterException {
        if ((locationCounterIndex < 0) || (locationCounterIndex > MAX_LOCATION_COUNTER_INDEX)) {
            throw new ParameterException("Invalid location counter index");
        }
        return _locationCounterPools.get(locationCounterIndex);
    }

    /**
     * Retrieves the name of this module
     */
    public String getModuleName() {
        return _moduleName;
    }

    public boolean getAFCMClearSensitivity() {
        return _afcmClear;
    }

    public boolean getAFCMSetSensitivity() {
        return _afcmSet;
    }

    public boolean getQuarterWordSensitivity() {
        return _quarterWordSet;
    }

    public RelativeEntryPoint getStartAddress() {
        return _startAddress;
    }

    public boolean getThirdWordSensitivity() {
        return _thirdWordSet;
    }

    /**
     * Reads a relocatable element from a program-file-formatted external file.
     * Only an undeleted element will be read.
     * @param externalFileName host system name of the external file
     * @param elementName element name to be located (should not be space-filled)
     * @param elementVersion version name to be located (should not be space-filled)
     */
    public static RelocatableModule readElement(
        final String externalFileName,
        final String elementName,
        final String elementVersion
    ) {
        return null;//TODO
    }

    /**
     * Writes an element into a program-file-formatted external file.
     * If an undeleted element with the given name/version combination already exists, it is marked deleted.
     * If any cycles exist with the given name/version combination, the new cycle number will be set appropriately.
     * @param externalFileName host system name of the external file
     * @param elementName element name to be used  (should not be space-filled - will be stored in Fieldata)
     * @param elementVersion verison name to be used  (should not be space-filled - will be stored in Fieldata)
     * @param format 0 for original format, 1 for new format
     */
    public void writeElement(
        final String externalFileName,
        final String elementName,
        final String elementVersion,
        final int format
    ) {
        //TODO
    }
}
