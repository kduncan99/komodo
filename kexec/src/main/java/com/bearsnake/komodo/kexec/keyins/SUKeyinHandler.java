/*
 * Copyright (c) 2018-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.hardwarelib.devices.Device;
import com.bearsnake.komodo.hardwarelib.devices.DeviceType;
import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.facilities.DeviceNodeInfo;
import com.bearsnake.komodo.kexec.facilities.NodeStatus;

class SUKeyinHandler extends FacHandler implements Runnable {

    /* TODO
    SU component INITIALIZES AND ADDS device TO FIXED MS, PROCESS? YN
SU KEYIN - ALREADY PERFORMED FOR component
SU [dev] MAY BE FATAL (EXERR 052) - TERMINATE SU KEYIN? Y/N
SU OF component IS NOT ALLOWED
SU OF component NOT PERFORMED - KEYIN ABORTED
     */

    private static final String[] HELP_TEXT = {
        "SU[,ALL] node_name",
        "Makes devices available for use, however space cannot be allocated.",
    };

    private static final String[] SYNTAX_TEXT = {
        "SU[,ALL] node_name",
    };

    public static final String COMMAND = "SU";

    public SUKeyinHandler(final ConsoleId source,
                          final String options,
                          final String arguments) {
        super(source, options, arguments);
    }

    @Override
    boolean checkSyntax() {
        return _options == null && _arguments != null;
    }

    @Override String getCommand() { return COMMAND; }
    @Override String[] getHelp() { return HELP_TEXT; }
    @Override String[] getSyntax() { return SYNTAX_TEXT; }

    @Override
    boolean isAllowed() {
        // TODO depends on @@CONS privilege
        return true;
    }

    @Override
    void process() {
        if (_options != null) {
            processAll();
        } else {
            processNode();
        }
    }

    private void processAll() {
        var nodeInfos = getNodeInfoListForChannel();
        if (nodeInfos == null) {
            return;
        }

        for (var ni : nodeInfos) {
            if (((Device)ni.getNode()).getDeviceType() != DeviceType.DiskDevice) {
                var msg = String.format("SU keyin not allowed for %s", ni.getNode().getNodeName());
                Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
                return;
            }
        }

        nodeInfos.forEach(ni -> ni.setNodeStatus(NodeStatus.Suspended));
        displayStatusForNodes(nodeInfos);
        // TODO For devices which were DN or RV...
        //  how do we initiate scan of fixed disk(s) - send device ready?
    }

    private void processNode() {
        var devName = _arguments.toUpperCase();
        var ni = _facMgr.getNodeInfo(devName);
        if (!(ni instanceof DeviceNodeInfo)) {
            var msg = String.format("%s is not a configured device", devName);
            Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
            return;
        }

        if (((Device)ni.getNode()).getDeviceType() != DeviceType.DiskDevice) {
            var msg = String.format("SU keyin not allowed for %s", ni.getNode().getNodeName());
            Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
            return;
        }

        if (ni.getNodeStatus() == NodeStatus.Suspended) {
            var msg = String.format("%s is already suspended", ni.getNode().getNodeName());
            Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
            return;
        }

        var wasUnavailable = ni.getNodeStatus() == NodeStatus.Down || ni.getNodeStatus() == NodeStatus.Reserved;
        ni.setNodeStatus(NodeStatus.Suspended);
        displayStatusForNode(ni);
        // TODO for wasUnavailable - how do we initiate scan of fixed disk - send device ready?
    }
}
