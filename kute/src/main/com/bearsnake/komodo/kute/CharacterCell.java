/*
 * Copyright (c) 2025 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kute;

import static com.bearsnake.komodo.kute.Constants.ASCII_SP;

public class CharacterCell {

    private byte _character;
    private Emphasis _emphasis;
    private FieldAttributes _attributes;

    public CharacterCell() {
        _character = ASCII_SP;
        _emphasis = new Emphasis();
        _attributes = null;
    }

    public CharacterCell copy() {
        var cc = new CharacterCell();
        cc._character = _character;
        cc._emphasis = _emphasis.copy();
        cc._attributes = _attributes == null ? null : _attributes.copy();
        return cc;
    }

    public byte getCharacter() { return _character; }
    public Emphasis getEmphasis() { return _emphasis; }
    public FieldAttributes getAttributes() { return _attributes; }

    public void setCharacter(byte character) { _character = character; }
    public void setEmphasis(Emphasis emphasis) { _emphasis = emphasis; }
    public void setAttributes(FieldAttributes attributes) { _attributes = attributes; }
}
