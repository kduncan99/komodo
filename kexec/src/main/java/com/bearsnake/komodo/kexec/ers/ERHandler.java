/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.ers;

import com.bearsnake.komodo.engine.ActivityState;
import com.bearsnake.komodo.kexec.scheduleManager.Run;

public abstract class ERHandler {

    public abstract void handle(
        final Run run,
        final ActivityState activityState);
}
