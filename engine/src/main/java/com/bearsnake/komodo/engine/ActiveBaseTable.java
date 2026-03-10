package com.bearsnake.komodo.engine;

import java.util.stream.IntStream;

public class ActiveBaseTable {

    public static class Entry {
        
        private short _bankLevel;
        private int _bankDescriptorIndex;
        private int _subsetSpecification;

        public Entry() {}

        public short getBankLevel() { return _bankLevel; }
        public int getBankDescriptorIndex() { return _bankDescriptorIndex; }
        public int getSubsetSpecification() { return _subsetSpecification; }

        public void set(
            final short bankLevel,
            final int bankDescriptorIndex,
            final int subsetSpecification
        ) {
            _bankLevel = bankLevel;
            _bankDescriptorIndex = bankDescriptorIndex;
            _subsetSpecification = subsetSpecification;
        }

        public Entry setBankLevel(final short bankLevel) { _bankLevel = bankLevel; return this; }
        public Entry setBankDescriptorIndex(final int bankDescriptorIndex) { _bankDescriptorIndex = bankDescriptorIndex; return this; }
        public Entry setSubsetSpecification(final int subsetSpecification) { _subsetSpecification = subsetSpecification; return this; }
    }

    private final Entry[] _entries = new Entry[16];

    public ActiveBaseTable() {
        // leave entry 0 empty
        IntStream.range(1, _entries.length)
                 .forEach(i -> _entries[i] = new Entry());
    }

    public Entry getEntry(final int index) {
        if ((index < 0) || (index >= _entries.length)) {
            throw new RuntimeException(String.format("Invalid index=%d", index));
        }
        return _entries[index];
    }
}
