/*
 * Copyright (c) 2019 by Kurt Duncan - All Rights Reserved
 */

package com.kadware.komodo.hardwarelib;

import com.kadware.komodo.baselib.ArraySlice;
import com.kadware.komodo.baselib.Word36;
import com.kadware.komodo.hardwarelib.interrupts.AddressingExceptionInterrupt;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.util.List;

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

    SystemConsole(
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
