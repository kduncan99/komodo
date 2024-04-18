/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.facilities;

import java.util.LinkedList;
import java.util.List;

import static com.bearsnake.komodo.kexec.facilities.FacStatusMessage.FacStatusMessages;

public class FacStatusResult {

    public static class MessageInstance {

        public FacStatusCode _code;
        public String[] _parameters;

        public MessageInstance(
            final FacStatusCode code,
            final String[] parameters
        ) {
            _code = code;
            _parameters = parameters;
        }
    }

    private final List<MessageInstance> _errors = new LinkedList<>();
    private final List<MessageInstance> _infos = new LinkedList<>();
    private final List<MessageInstance> _warnings = new LinkedList<>();

    public boolean hasErrorMessages() { return !_errors.isEmpty(); }
    public boolean hasInfoMessages() { return !_infos.isEmpty(); }
    public boolean hasWarningMessages() { return !_warnings.isEmpty(); }

    public void postMessage(
        final FacStatusCode code,
        final String[] parameters
    ) {
        var template = FacStatusMessages.get(code);
        var inst = new MessageInstance(code, parameters);
        switch (template.getCategory()) {
        case Error -> _errors.add(inst);
        case Info -> _infos.add(inst);
        case Warning -> _warnings.add(inst);
        }
    }
}
