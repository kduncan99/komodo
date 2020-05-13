/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.kex.kasm.directives;

import com.kadware.komodo.kex.kasm.Assembler;
import com.kadware.komodo.kex.kasm.CharacterMode;
import com.kadware.komodo.kex.kasm.LabelFieldComponents;
import com.kadware.komodo.kex.kasm.TextLine;

@SuppressWarnings("Duplicates")
public class ASCIIDirective extends Directive {

    @Override
    public void process(
        final Assembler assembler,
        final TextLine textLine,
        final LabelFieldComponents labelFieldComponents
    ) {
        if (extractFields(assembler, textLine, false, 2)) {
            assembler.setCharacterMode(CharacterMode.ASCII);
        }
    }
}
