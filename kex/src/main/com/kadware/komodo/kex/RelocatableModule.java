/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex;

import com.kadware.komodo.baselib.Word36;

/**
 * Represents a relocatable module - a portion of code which is collected with
 * potentially many other such portions to create an absolute module (loadable executable).
 */
public class RelocatableModule {

    //  Relocatable Word -------------------------------------------------------

    static class RelocatableWord {

        final Word36 _baseValue;
        final RelocatableItem[] _relocatableItems;

        RelocatableWord(
            final Word36 baseValue,
            final RelocatableItem[] relocatableItems
        ) {
            _baseValue = baseValue;
            _relocatableItems = relocatableItems;
        }
    }

    static abstract class RelocatableItem {

        final int _incrementFactor;

        RelocatableItem(
            final int incrementFactor
        ) {
            _incrementFactor = incrementFactor;
        }
    }

    static class LocationCounterRelocatableItem extends RelocatableItem {

        final int _locationCounterIndex;

        LocationCounterRelocatableItem(
            final int locationCounterIndex,
            final int incrementFactor
        ) {
            super(incrementFactor);
            _locationCounterIndex = locationCounterIndex;
        }
    }

    static class UndefinedSymbolRelocatableItem extends RelocatableItem {

        final String _undefinedSymbol;

        UndefinedSymbolRelocatableItem(
            final String undefinedSymbol,
            final int incrementFactor
        ) {
            super(incrementFactor);
            _undefinedSymbol = undefinedSymbol;
        }
    }

    //  Location Counter Pools -------------------------------------------------

    static class LocationCounterPool {

    }

    //  Undefined Symbols ------------------------------------------------------

    static class UndefinedSymbol {

        final String _value;

        UndefinedSymbol(
            final String value
        ) {
            _value = value;
        }
    }

    //  Entry points -----------------------------------------------------------

    static abstract class EntryPoint {

        final String _name;
        final long _value;

        EntryPoint(
            final String name,
            final long value
        ) {
            _name = name;
            _value = value;
        }
    }

    static class AbsoluteEntryPoint extends EntryPoint {

        AbsoluteEntryPoint(
            final String name,
            final long value
        ) {
            super(name, value);
        }
    }

    static class RelativeEntryPoint extends EntryPoint {

        final int _locationCounterIndex;

        RelativeEntryPoint(
            final String name,
            final int value,
            final int locationCounterIndex
        ) {
            super(name, value);
            _locationCounterIndex = locationCounterIndex;
        }
    }

    //  Control Information ----------------------------------------------------

    static class ControlInformation {
        //  TODO tbd
    }

    //  Constants --------------------------------------------------------------

    private static final int TEXT_BLOCK_SIZE = 14;

    //  Data -------------------------------------------------------------------

    //  Code -------------------------------------------------------------------

}
