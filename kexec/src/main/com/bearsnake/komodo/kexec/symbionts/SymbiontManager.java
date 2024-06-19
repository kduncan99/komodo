/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.symbionts;

import com.bearsnake.komodo.hardwarelib.ChannelProgram;
import com.bearsnake.komodo.hardwarelib.DeviceType;
import com.bearsnake.komodo.hardwarelib.IoStatus;
import com.bearsnake.komodo.hardwarelib.SymbiontDevice;
import com.bearsnake.komodo.kexec.Manager;
import com.bearsnake.komodo.kexec.exceptions.ExecStoppedException;
import com.bearsnake.komodo.kexec.exceptions.KExecException;
import com.bearsnake.komodo.kexec.exceptions.NoRouteForIOException;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.exec.StopCode;
import com.bearsnake.komodo.kexec.facilities.NodeInfo;
import com.bearsnake.komodo.logger.LogManager;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

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
    //(Exec) This message is displayed in response to a UR symbiont or a SM symbiont S keyin. The specified remote device has been suspended.

    private static class InputSession {

        public final NodeInfo _nodeInfo;
        public boolean _hasRunInfo;
        public boolean _isDone;

        public InputSession(
            final NodeInfo nodeInfo
        ) {
            _hasRunInfo = false;
            _isDone = false;
            _nodeInfo = nodeInfo;
        }
    }

    private final Map<NodeInfo, InputSession> _inputSessions = new HashMap<>();

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
        // TODO dump input and output queues
    }

    /**
     * Invoked for all managers when the exec is instantiated (presumably when the application starts)
     */
    @Override
    public void initialize() throws KExecException {
        LogManager.logTrace(LOG_SOURCE, "initialize()");
        // TODO read group configuration (we have this janky temporary code for now)

        // TODO end janky
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

    private void inputSessionHandle(
        final InputSession session
    ) {
        // TODO
        // FACILITY WAIT FOR RUN-ID: run-id SOURCE - symbiont AT
        //(Exec) The specified run being processed by the card reader symbiont cannot assign the facilities requested on the run @FILE card. The card reader idles until you answer and any necessary label checks are done.
        //A assigns facilities or specifies that a tape unit has been reserved.
        //T bypasses the remaining runstream, marks the run removed, and initiates the card reader.

//        if (!session._hasRunInfo) {
//            //
//        } else {
//
//        }
        session._isDone = true; // TODO temporary
    }

    private void inputSessionStart(
        final NodeInfo nodeInfo
    ) {
        var exec = Exec.getInstance();
        var msg = nodeInfo.getNode().getNodeName() + " ACTIVE";
        exec.sendExecReadOnlyMessage(msg);

        var is = new InputSession(nodeInfo);
        _inputSessions.put(nodeInfo, is);
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
    private boolean pollRead() throws ExecStoppedException {
        boolean didSomething;
        synchronized (_inputSessions) {
            var exec = Exec.getInstance();
            var fm = exec.getFacilitiesManager();
            var nodeInfos = fm.getNodeInfos(DeviceType.SymbiontDevice);

            // Look for nodes which are ready, but there is no active session.
            for (var nodeInfo : nodeInfos) {
                // TODO is the thing DN or locked out or suspended?
                if (nodeInfo.getNode() instanceof SymbiontDevice symDev) {
                    if (symDev.isReady() && !_inputSessions.containsKey(nodeInfo)) {
                        inputSessionStart(nodeInfo);
                    }
                }
            }

            // Iterate over active input sessions
            didSomething = !_inputSessions.isEmpty();
            for (var ssn : _inputSessions.values()) {
                if (!ssn._isDone) {
                    inputSessionHandle(ssn);
                } else {
                    resetNode(ssn._nodeInfo);
                    var msg = ssn._nodeInfo.getNode().getNodeName() + " INACTIVE";
                    exec.sendExecReadOnlyMessage(msg);
                    _inputSessions.remove(ssn._nodeInfo);
                }
            }
        }

        return didSomething;
    }

    /**
     * Resets the indicated node
     * @param nodeInfo describes the node
     * @return true generally, false if the IO failed
     */
    private boolean resetNode(
        final NodeInfo nodeInfo
    ) throws ExecStoppedException {
        var exec = Exec.getInstance();
        try {
            var cp = new ChannelProgram().setNodeIdentifier(nodeInfo.getNode().getNodeIdentifier())
                                         .setFunction(ChannelProgram.Function.Control)
                                         .setSubFunction(ChannelProgram.SubFunction.Reset);
            exec.getFacilitiesManager().routeIo(cp);
            while (cp.getIoStatus() == IoStatus.InProgress) {
                Exec.sleep(10);
            }
            if (cp.getIoStatus() != IoStatus.Complete) {
                var msg = String.format("%s IO error - %s", nodeInfo.getNode().getNodeName(), cp.getIoStatus());
                // TODO DN the device
                return false;
            }
        } catch (NoRouteForIOException e) {
            var msg = String.format("%s IO error - no route to device", nodeInfo.getNode().getNodeName());
            // TODO DN the device
            return false;
        }

        return true;
    }

    private class Poller implements Runnable {

        public boolean _terminate = false;

        @Override
        public void run() {
            var exec = Exec.getInstance();
            while (!_terminate && !exec.isStopped()) {
                // TODO slow wait if the exec is not ready for symbionts
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
