/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.ers;

import com.bearsnake.komodo.engine.ActivityState;
import com.bearsnake.komodo.kexec.exec.RunControlEntry;

public abstract class ERHandler {

    public abstract void handle(
        final RunControlEntry runControlEntry,
        final ActivityState activityState);
}
