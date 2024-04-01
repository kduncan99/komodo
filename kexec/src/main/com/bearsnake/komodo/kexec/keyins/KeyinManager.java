/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.kexec.Manager;
import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.exec.Exec;
import java.io.PrintStream;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;

public class KeyinManager implements Manager, Runnable {

    private static final long THREAD_DELAY = 100;

    private final LinkedList<PostedKeyin> _postedKeyins = new LinkedList<>();

    public synchronized void postKeyin(final ConsoleId source,
                                       final String text) {
        _postedKeyins.add(new PostedKeyin(source, text));
    }

    @Override
    public void boot() throws Exception {
        _postedKeyins.clear();
    }

    @Override
    public void dump(PrintStream out, String indent) {
        // TODO
    }

    @Override
    public void initialize() throws Exception {
        Exec.getInstance().getExecutor().scheduleWithFixedDelay(this,
                                                                THREAD_DELAY,
                                                                THREAD_DELAY,
                                                                TimeUnit.MILLISECONDS);
    }

    @Override
    public void stop() {
        // nothing to do... i think.
    }

    @Override
    public void run() {
        if (!Exec.getInstance().isStopped()) {
            // TODO
        }
    }
}
