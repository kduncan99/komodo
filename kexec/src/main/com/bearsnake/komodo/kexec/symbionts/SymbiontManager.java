/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.symbionts;

import com.bearsnake.komodo.kexec.Manager;
import com.bearsnake.komodo.kexec.exceptions.KExecException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.StopCode;
import com.bearsnake.komodo.logger.LogManager;

import java.io.PrintStream;

public class SymbiontManager implements Manager {

    private static final int POLL_DELAY = 100;
    static final String LOG_SOURCE = "SymbMgr";

    private Poller _poller = null;

    // TODO
    //   We need print and punch queues
    //   We need an input queue (affectionately known as backlog)
    //   We need some way to tie input devices and output devices into groups
    //   We need to track state for all print, punch, and read devices

    public SymbiontManager() {
        Exec.getInstance().managerRegister(this);
    }

    @Override
    public void boot(boolean recoveryBoot) throws KExecException {
        LogManager.logTrace(LOG_SOURCE, "boot(%s)", recoveryBoot);
        _poller = new Poller();
        new Thread(_poller).start();

        LogManager.logTrace(LOG_SOURCE, "boot complete", recoveryBoot);
    }

    @Override
    public void close() {
        LogManager.logTrace(LOG_SOURCE, "close()");
    }

    @Override
    public void dump(PrintStream out, String indent, boolean verbose) {
        out.printf("%sSymbiontManager ********************************\n", indent);
        // TODO dump input and output queues
    }

    @Override
    public void initialize() throws KExecException {
        LogManager.logTrace(LOG_SOURCE, "initialize()");
    }

    @Override
    public void stop() {
        LogManager.logTrace(LOG_SOURCE, "stop()");
        _poller._terminate = true;
        _poller = null;
    }

    /**
     * Handle the image printer state machine for each output device.
     * Do NOT do any blocking IO - symbiont IO is conventionally slow (although most of the virtual devices are not).
     * @return true if any of the output devices are busy
     */
    private boolean pollPrint() {
        return false;// TODO
    }

    /**
     * Handle the image writer state machine for each output device.
     * Do NOT do any blocking IO - symbiont IO is conventionally slow (although most of the virtual devices are not).
     * @return true if any of the output devices are busy
     */
    private boolean pollPunch() {
        return false;// TODO
    }

    /**
     * If any input symbionts have transitioned to ready do initialization of the symbiont and set its state
     * accordingly. For all symbionts which are ready, handle their state machines.
     * Do NOT do any blocking IO - symbiont IO is conventionally slow (although most of the virtual devices are not).
     * @return true if any of the input devices are busy
     */
    private boolean pollRead() {
        return false;// TODO
    }

    private class Poller implements Runnable {

        public boolean _terminate = false;

        @Override
        public void run() {
            var exec = Exec.getInstance();
            while (!_terminate && !exec.isStopped()) {
                try {
                    int delay = (pollPrint() | pollPunch() | pollRead()) ? 0 : POLL_DELAY;
                    Exec.sleep(delay);
                } catch (Throwable t) {
                    LogManager.logCatching(LOG_SOURCE, t);
                    Exec.getInstance().stop(StopCode.ExecActivityTakenToEMode);
                }
            }
        }
    }
}
