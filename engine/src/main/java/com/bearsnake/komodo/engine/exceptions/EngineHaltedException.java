/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.engine.exceptions;

import com.bearsnake.komodo.engine.Engine;

public class EngineHaltedException extends EngineException {

    public EngineHaltedException(
        final Engine.HaltCode haltCode
    ) {
        super("Engine Halted:" + haltCode);
    }
}
