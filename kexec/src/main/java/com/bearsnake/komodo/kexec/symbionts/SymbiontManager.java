/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.symbionts;

import com.bearsnake.komodo.hardwarelib.devices.DeviceType;
import com.bearsnake.komodo.hardwarelib.devices.SymbiontPrinterDevice;
import com.bearsnake.komodo.hardwarelib.devices.SymbiontPunchDevice;
import com.bearsnake.komodo.hardwarelib.devices.SymbiontReaderDevice;
import com.bearsnake.komodo.kexec.Manager;
import com.bearsnake.komodo.kexec.exceptions.KExecException;
import com.bearsnake.komodo.kexec.exec.Exec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

public class SymbiontManager implements Manager {

    private static final Logger LOGGER = LogManager.getLogger(SymbiontManager.class);

    // TODO
    //   symbiont NOT ACTIVE
    //(Exec) A UP symbiont keyin was entered. The specified remote device is not
    //active.

    // TODO
    //   symbiont SUSPENDED
    //(Exec) This message is displayed in response to a UR symbiont or a SM symbiont S keyin.
    // The specified remote device has been suspended.

    private final Map<String, Symbiont> _symbiontInfos = new HashMap<>();

    public SymbiontManager() {
        Exec.getInstance().managerRegister(this);
    }

    /**
     * Invoked for all managers when the exec boots
     */
    @Override
    public void boot(boolean recoveryBoot) throws KExecException {
        LOGGER.traceEntry("boot({})", recoveryBoot);
        _symbiontInfos.values().forEach(Symbiont::start);
        LOGGER.traceExit("boot({})", recoveryBoot);
    }

    /**
     * Invoked for all managers when the exec is being closed out
     */
    @Override
    public void close() {
        LOGGER.traceEntry("close()");
        LOGGER.traceExit("close()");
    }

    /**
     * For debugging
     */
    @Override
    public void dump(PrintStream out, String indent, boolean verbose) {
        out.printf("%sSymbiontManager ********************************\n", indent);

        out.printf("%s  Symbionts:\n", indent);
        _symbiontInfos.values().forEach(symInfo -> out.printf("%s    %s\n", indent, symInfo.getStateString()));
        // TODO  sym groups
    }

    public Symbiont getSymbiontInfo(final String symbiontName) {
        return _symbiontInfos.get(symbiontName);
    }

    /**
     * Invoked for all managers when the exec is instantiated (presumably when the application starts).
     * We expect fac mgr to already be initialized.
     */
    @Override
    public void initialize() throws KExecException {
        LOGGER.traceEntry("initialize()");

        // Symbionts configuration
        var exec = Exec.getInstance();
        var fm = exec.getFacilitiesManager();
        var nodeInfos = fm.getNodeInfos(DeviceType.SymbiontDevice);
        for (var nodeInfo : nodeInfos) {
            if (nodeInfo.getNode() instanceof SymbiontReaderDevice) {
                _symbiontInfos.put(nodeInfo.getNode().getNodeName(), new OnSiteReaderSymbiont(nodeInfo));
            } else if (nodeInfo.getNode() instanceof SymbiontPunchDevice) {
                _symbiontInfos.put(nodeInfo.getNode().getNodeName(), new OnSitePunchSymbiont(nodeInfo));
            } else if (nodeInfo.getNode() instanceof SymbiontPrinterDevice) {
                _symbiontInfos.put(nodeInfo.getNode().getNodeName(), new OnSitePrinterSymbiont(nodeInfo));
            }
        }

        LOGGER.traceExit("initialize()");
    }

    /**
     * Invoked for all managers when the exec stops
     */
    @Override
    public void stop() {
        LOGGER.traceEntry("stop()");
        _symbiontInfos.values().forEach(Symbiont::terminate);
        LOGGER.traceExit("stop()");
    }
}
