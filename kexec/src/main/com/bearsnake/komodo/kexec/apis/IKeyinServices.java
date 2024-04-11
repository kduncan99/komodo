/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.apis;

import com.bearsnake.komodo.kexec.consoles.ConsoleId;

public interface IKeyinServices {

    void postKeyin(ConsoleId source, String text);
}
