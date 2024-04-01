/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.exec;

import com.bearsnake.komodo.kexec.consoles.ConsoleManager;

public class Exec {

    private static Exec _instance = null;

    private ConsoleManager _consoleManager;

    public Exec() {
        _consoleManager = new ConsoleManager();
        _instance = new Exec();
    }

    public ConsoleManager getConsoleManager() {
        return _consoleManager;
    }

    public static Exec getInstance() {
        return _instance;
    }
}
