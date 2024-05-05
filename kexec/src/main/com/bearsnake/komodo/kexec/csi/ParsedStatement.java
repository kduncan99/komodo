/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.csi;

import com.bearsnake.komodo.kexec.facilities.FacStatusResult;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ParsedStatement {

    public String _originalStatement;
    public String _label;
    public String _mnemonic;
    public final List<String> _optionsFields = new LinkedList<>();
    public final Map<SubfieldSpecifier, String> _operandFields = new HashMap<>();
    public FacStatusResult _facStatusResult = new FacStatusResult();
}
