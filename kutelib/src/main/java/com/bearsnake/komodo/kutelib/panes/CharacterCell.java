/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib.panes;

import com.bearsnake.komodo.utslib.Emphasis;
import com.bearsnake.komodo.utslib.fields.Field;

import static com.bearsnake.komodo.baselib.Constants.ASCII_SP;

public class CharacterCell {

    private byte _character;
    private Emphasis _emphasis;
    private Field _field;

    public CharacterCell() {
        _character = ASCII_SP;
        _emphasis = new Emphasis();
    }

    public CharacterCell copy() {
        var cc = new CharacterCell();
        cc._character = _character;
        cc._emphasis = _emphasis.copy();
        cc._field = _field;
        return cc;
    }

    public byte getCharacter() { return _character; }
    public Emphasis getEmphasis() { return _emphasis; }
    public Field getField() { return _field; }

    public void setCharacter(final byte character) { _character = character; }
    public void setField(final Field field) { _field = field; }
}
