/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.symbionts;

import com.bearsnake.komodo.hardwarelib.DeviceType;
import com.bearsnake.komodo.hardwarelib.SymbiontPrinterDevice;
import com.bearsnake.komodo.hardwarelib.SymbiontReaderDevice;
import com.bearsnake.komodo.hardwarelib.SymbiontWriterDevice;
import com.bearsnake.komodo.kexec.Manager;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exceptions.KExecException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.StopCode;
import com.bearsnake.komodo.kexec.facilities.NodeStatus;
import com.bearsnake.komodo.logger.LogManager;

import java.io.PrintStream;
import java.util.LinkedList;
import java.util.List;

public class SymbiontManager implements Manager {

    private static final int POLL_DELAY = 100;
    static final String LOG_SOURCE = "SymbMgr";

    private Poller _poller = null;

    // TODO
    //   We need print and punch queues
    //   We need an input queue (affectionately known as backlog)
    //   We need some way to tie input devices and output devices into groups
    //   We need to track state for all print, punch, and read devices

    // TODO locked out symbionts (for SM symbiont L keyin)

    // TODO
    //   LOST RUN - run-id/site-id RECOVERED nn SYMBIONT FILES
    //(Exec) A previously opened run was lost during a recovery boot. This message
    //specifies the number of symbiont files recovered.

    // TODO
    //   symbiont NOT ACTIVE
    //(Exec) A UP symbiont keyin was entered. The specified remote device is not
    //active.

    // TODO
    //   symbiont SUSPENDED
    //(Exec) This message is displayed in response to a UR symbiont or a SM symbiont S keyin.
    // The specified remote device has been suspended.

    private final List<InputSymbiontInfo> _inputSymbiontInfos = new LinkedList<>();

    public SymbiontManager() {
        Exec.getInstance().managerRegister(this);
    }

    /**
     * Invoked for all managers when the exec boots
     */
    @Override
    public void boot(boolean recoveryBoot) throws KExecException {
        LogManager.logTrace(LOG_SOURCE, "boot(%s)", recoveryBoot);
        _poller = new Poller();
        new Thread(_poller).start();

        LogManager.logTrace(LOG_SOURCE, "boot complete", recoveryBoot);
    }

    /**
     * Invoked for all managers when the exec is being closed out
     */
    @Override
    public void close() {
        LogManager.logTrace(LOG_SOURCE, "close()");
    }

    /**
     * For debugging
     */
    @Override
    public void dump(PrintStream out, String indent, boolean verbose) {
        out.printf("%sSymbiontManager ********************************\n", indent);

        out.printf("%s  Input Symbionts:\n", indent);
        for (var isInfo : _inputSymbiontInfos) {
            out.printf("%s    %s Lock:%s Susp:%s State:%s Wait:%s Abort:%s\n", indent,
                       isInfo._nodeInfo.getNode().getNodeName(),
                       isInfo._isLocked,
                       isInfo._isSuspended,
                       isInfo._state,
                       isInfo._isWaiting,
                       isInfo._abort);
        }

        out.printf("%s  Output Symbionts:\n", indent);
        // TODO

        out.printf("%s  Print Symbionts:\n", indent);
        // TODO
    }

    /**
     * Invoked for all managers when the exec is instantiated (presumably when the application starts).
     * We expect fac mgr to already be initialized.
     */
    @Override
    public void initialize() throws KExecException {
        LogManager.logTrace(LOG_SOURCE, "initialize()");

        var exec = Exec.getInstance();
        var fm = exec.getFacilitiesManager();
        var nodeInfos = fm.getNodeInfos(DeviceType.SymbiontDevice);
        for (var nodeInfo : nodeInfos) {
            if (nodeInfo.getNode() instanceof SymbiontReaderDevice srd) {
                _inputSymbiontInfos.add(new InputSymbiontInfo(nodeInfo));
            } else if (nodeInfo.getNode() instanceof SymbiontWriterDevice) {
                // TODO
            } else if (nodeInfo.getNode() instanceof SymbiontPrinterDevice) {
                // TODO
            }
        }

        // Get symbiont group configuration
        //   TODO
    }

    /**
     * Invoked for all managers when the exec stops
     */
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
    private boolean pollPrinters() {
        return false;// TODO
    }

    /**
     * Handle the image writer state machine for each output device.
     * Do NOT do any blocking IO - symbiont IO is conventionally slow (although most of the virtual devices are not).
     * @return true if any of the output devices are busy
     */
    private boolean pollPunchers() {
        return false;// TODO
    }

    /**
     * Handle state machines for all input symbionts which are UP and accessible, and neither locked nor suspended.
     * Do NOT do any blocking IO - symbiont IO is conventionally slow (although most of the virtual devices are not).
     * @return true if any of the input devices are busy
     */
    private boolean pollReaders() throws ExecStoppedException {
        boolean didSomething = false;
        synchronized (_inputSymbiontInfos) {
            var exec = Exec.getInstance();
            var fm = exec.getFacilitiesManager();

            for (var isInfo : _inputSymbiontInfos) {
                if ((isInfo._nodeInfo.getNodeStatus() == NodeStatus.Up)
                    && fm.isDeviceAccessible(isInfo._nodeIdentifier)
                    && !isInfo._isLocked
                    && !isInfo._isSuspended) {
                    didSomething |= isInfo.poll();
                }
            }
        }

        return didSomething;
    }

    private class Poller implements Runnable {

        public boolean _terminate = false;

        @Override
        public void run() {
            var exec = Exec.getInstance();
            while (!_terminate && !exec.isStopped()) {
                // TODO slow wait if the exec is not ready for symbionts
                try {
                    int delay = (pollPrinters() | pollPunchers() | pollReaders()) ? 0 : POLL_DELAY;
                    Exec.sleep(delay);
                } catch (Throwable t) {
                    LogManager.logCatching(LOG_SOURCE, t);
                    Exec.getInstance().stop(StopCode.ExecActivityTakenToEMode);
                }
            }
        }
    }
}
