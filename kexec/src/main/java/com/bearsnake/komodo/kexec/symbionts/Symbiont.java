/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.symbionts;

import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.StopCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Information (and more importantly, common functionality) relating to any symbiont device.
 */
public abstract class Symbiont implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(Symbiont.class);

    protected static final int POLL_DELAY_MILLISECONDS = 1000;
    protected static final int POLL_LONG_DELAY_MILLISECONDS = 5000;

    private final String _symbiontName;
    protected boolean _terminate = false;

    protected Symbiont(
        final String symbiontName
    ) {
        _symbiontName = symbiontName;
    }

    public final String getSymbiontName() {
        return _symbiontName;
    }

    /**
     * State machine for subclass
     * @return true if we did something useful, false if we are waiting for something.
     */
    abstract boolean poll() throws ExecStoppedException;

    /**
     * Starts the run method
     */
    final void start() {
        _terminate = false;
        Thread thread = new Thread(this);
        thread.start();
    }

    /**
     * Stops the run method
     */
    final void terminate() {
        _terminate = true;
    }

    /**
     * Creates a string indicating the state of the symbiont device, to be sent to the console
     */
    public abstract String getStateString();

    /**
     * For handling SM * I keyins - initiates the device
     * (Re)starts the device.
     */
    public abstract void initialize() throws ExecStoppedException;

    public abstract boolean isInputSymbiont();
    public abstract boolean isOnSiteSymbiont();
    public abstract boolean isOutputSymbiont();
    public abstract boolean isPrintSymbiont();
    public abstract boolean isRemoteSymbiont();

    /**
     * For handling SM * L keyins - Locks out the device
     * Any current file runs to completion, but no subsequent file is processed
     */
    public abstract void lockDevice();

    /**
     * For handling SM * R keyins - Rewinds or advances by a number of pages or cards
     * For input files the current file is bypassed until the next @RUN image, and the run is discarded --
     * if count is specified, it is ignored (not specifying a count is only allowed for input).
     * For output files a negative count rewinds by the given number of cards or pages and then output is resumed.
     * A positive count advances printing by the given number of cards or pages.
     * @param count number of cards or pages (must be zero for input symbiont)
     */
    public abstract void reposition(final int count) throws ExecStoppedException;

    /**
     * For handling SM * RALL keyins - Rewinds an entire output file.
     * For input files the current file is bypassed until the next @RUN image, and the run is discarded.
     */
    public abstract void repositionAll() throws ExecStoppedException;

    /**
     * For handling SM * Q keyins - Re-queues current file and locks the device
     * Does not apply to input symbionts.
     */
    public abstract void requeue();

    /**
     * For handling SM * C[HANGE] keyins - applies only to print symbions, local or remote
     * If any particular parameter is null, its value is not changed.
     */
    public abstract void setPageGeometry(final Integer linesPerPage,
                                         final Integer topMargin,
                                         final Integer bottomMargin,
                                         final Integer linesPerInch);

    /**
     * For handling SM * S keyins - Suspends the device
     */
    public abstract void suspend();

    /**
     * For handling SM * T keyin
     */
    public abstract void terminateDevice() throws ExecStoppedException;

    /**
     * For handling SM * E keyin
     */
    public abstract void terminateFile() throws ExecStoppedException;

    /**
     * Thread run method
     */
    public final void run() {
        LOGGER.trace("Starting symbiont thread {}", _symbiontName);
        var exec = Exec.getInstance();

        while (!_terminate) {
            try {
                int delay = 0;
                if (!exec.getGenFileInterface().isReady()) {
                    delay = POLL_LONG_DELAY_MILLISECONDS;
                } else {
                    delay = poll() ? 0 : POLL_DELAY_MILLISECONDS;
                }
                Thread.sleep(delay);
            } catch (InterruptedException ex) {
                LOGGER.info("{} interrupted", _symbiontName);
            } catch (ExecStoppedException ex) {
                LOGGER.info("{} Exec Stopped", _symbiontName);
                _terminate = true;
            } catch (Throwable t) {
                LOGGER.error("{} caught unexpected exception", _symbiontName, t);
                exec.stop(StopCode.ExecActivityTakenToEMode);
                _terminate = true;
            }
        }

        LOGGER.trace("{} symbiont thread exiting", _symbiontName);
    }
}
