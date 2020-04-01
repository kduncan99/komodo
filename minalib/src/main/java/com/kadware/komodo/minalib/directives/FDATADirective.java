/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.minalib.directives;

import com.kadware.komodo.minalib.CharacterMode;
import com.kadware.komodo.minalib.Context;
import com.kadware.komodo.minalib.LabelFieldComponents;
import com.kadware.komodo.minalib.TextLine;

@SuppressWarnings("Duplicates")
public class FDATADirective extends Directive {

    @Override
    public void process(
            final Context context,
            final TextLine textLine,
            final LabelFieldComponents labelFieldComponents
    ) {
        if (extractFields(context, textLine, false, 2)) {
            context.setCharacterMode(CharacterMode.Fieldata);
        }
    }
}
