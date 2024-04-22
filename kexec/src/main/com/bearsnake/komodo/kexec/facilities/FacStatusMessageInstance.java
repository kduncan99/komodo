/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities;

import static com.bearsnake.komodo.kexec.facilities.FacStatusMessage.FacStatusMessages;

public class FacStatusMessageInstance {

    public FacStatusCode _code;
    public Object[] _parameters;

    public FacStatusMessageInstance(
        final FacStatusCode code
    ) {
        _code = code;
        _parameters = new String[]{};
    }

    public FacStatusMessageInstance(
        final FacStatusCode code,
        final Object[] parameters
    ) {
        _code = code;
        _parameters = parameters;
    }

    @Override
    public String toString() {
        var fsMsg = FacStatusMessages.get(_code);
        var text = String.format(fsMsg.getTemplate(), _parameters);
        return String.format("%s:%06o %s", fsMsg.getCategory().toString(), fsMsg.getCode()._value, text);
    }
}
