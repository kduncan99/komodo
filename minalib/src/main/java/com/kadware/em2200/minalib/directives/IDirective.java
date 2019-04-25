/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.em2200.minalib.directives;

import com.kadware.em2200.minalib.*;
import com.kadware.em2200.minalib.diagnostics.*;
import com.kadware.em2200.minalib.dictionary.*;

public interface IDirective {

    /**
     * Retrieves the token which triggers this directive - should start with '$'
     * @return token
     */
    public String getToken();

    /**
     * Processes this directive
     * @param assembler reference to the assembler
     * @param context reference to the context in which this directive is to execute
     * @param labelFieldComponents LabelFieldComponents describing the label field on the line containing this directive
     * @param operatorField operator field for this directive
     * @param operandField operand field for this directive
     * @param diagnostics where diagnostics should be posted if necessary
     */
    public void process(
            final Assembler assembler,
            final Context context,
            final LabelFieldComponents labelFieldComponents,
            final TextField operatorField,
            final TextField operandField,
            final Diagnostics diagnostics
    );
}
