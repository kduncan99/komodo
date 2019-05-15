/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.directives;

import com.kadware.em2200.minalib.*;

@SuppressWarnings("Duplicates")
public class BASICDirective extends Directive {

    @Override
    public void process(
            final Context context,
            final TextLine textLine,
            final LabelFieldComponents labelFieldComponents
    ) {
        if (extractFields(context, textLine, false, 2)) {
            context._codeMode = CodeMode.Basic;
        }
    }
}
