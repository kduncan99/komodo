/*
 * Copyright (c) 2018-2020 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

/**
 * Class which implements the functionality necessary for a rudimentary system console as a web page.
 * The console will have a 24x80 output screen, and a separate 1x80 input area.
 */
@SuppressWarnings("Duplicates")
public class SystemConsole {

    public interface Client {
        void notify(final String inputText);
    }

    public enum Color {
        Black,
        Red,
        Blue,
        Green,
        Cyan,
        Yellow,
        Magenta,
        White,
    }

    private SystemConsole(
        final int port,
        final Color defaultColor,
        final Client client
    ) {
        //TODO
    }

    public void clearInputLock() {
        //TODO
    }

    public void deleteLine(
        final int lineNumber
    ) {
        //TODO
    }

    public void reset() {
        //TODO
    }

    public void setInputLock() {
        //TODO
    }

    public void writeLine(
        final int lineNumber,
        final Color color,
        final String text
    ) {
        //TODO
    }
}
