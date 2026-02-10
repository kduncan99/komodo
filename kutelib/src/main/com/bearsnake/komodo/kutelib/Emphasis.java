/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib;

public class Emphasis {

    private boolean _columnSeparator;
    private boolean _strikeThrough;
    private boolean _underscore;

    public Emphasis() {
        _columnSeparator = false;
        _strikeThrough = false;
        _underscore = false;
    }

    public Emphasis(final boolean columnSeparator,
                    final boolean strikeThrough,
                    final boolean underscore) {
        _columnSeparator = columnSeparator;
        _strikeThrough = strikeThrough;
        _underscore = underscore;
    }

    public Emphasis(final byte utsCode) {
        _columnSeparator = (utsCode & 0x01) == 0x01;
        _strikeThrough = (utsCode & 0x08) == 0x08;
        _underscore = (utsCode & 0x04) == 0x04;
    }

    public Emphasis copy() {
        return new Emphasis(_columnSeparator, _strikeThrough, _underscore);
    }

    public byte getCode() {
        int value = 0x20;
        value |= _columnSeparator ? 0x01 : 0;
        value |= _strikeThrough ? 0x08 : 0;
        value |= _underscore ? 0x04 : 0;
        return (byte)value;
    }

    public void add(final Emphasis e) {
        _columnSeparator |= e._columnSeparator;
        _strikeThrough |= e._strikeThrough;
        _underscore |= e._underscore;
    }

    public void remove(final Emphasis e) {
        _columnSeparator &= !e._columnSeparator;
        _strikeThrough &= !e._strikeThrough;
        _underscore &= !e._underscore;
    }

    public void clear() {
        _columnSeparator = false;
        _strikeThrough = false;
        _underscore = false;
    }

    public boolean isColumnSeparator() { return _columnSeparator; }
    public boolean isStrikeThrough() { return _strikeThrough; }
    public boolean isUnderscore() { return _underscore; }
    public boolean allFlagsClear() { return !_columnSeparator && !_strikeThrough && !_underscore; }

    public void set(final Emphasis emphasis) {
        _columnSeparator = emphasis._columnSeparator;
        _strikeThrough = emphasis._strikeThrough;
        _underscore = emphasis._underscore;
    }

    public void setColumnSeparator(final boolean flag) { _columnSeparator = flag; }
    public void setStrikeThrough(final boolean flag) { _strikeThrough = flag; }
    public void setUnderscore(final boolean flag) { _underscore = flag; }

    @Override
    public String toString() {
        return String.format("{Col%c:Stk%c:Und%c}",
                             _columnSeparator ? '+' : '-',
                             _strikeThrough ? '+' : '-',
                             _underscore ? '+' : '-'); }
}
