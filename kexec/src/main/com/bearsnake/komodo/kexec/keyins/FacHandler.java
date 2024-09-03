/*
 * Copyright (c) 2018-2024 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kexec.keyins;

import com.bearsnake.komodo.baselib.Parser;
import com.bearsnake.komodo.kexec.consoles.ConsoleId;
import com.bearsnake.komodo.kexec.exec.Exec;
import com.bearsnake.komodo.kexec.facilities.ChannelNodeInfo;
import com.bearsnake.komodo.kexec.facilities.FacilitiesManager;
import com.bearsnake.komodo.kexec.facilities.NodeInfo;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

public abstract class FacHandler extends KeyinHandler {

    protected final FacilitiesManager _facMgr;

    public FacHandler(final ConsoleId source,
                      final String options,
                      final String arguments) {
        super(source, options, arguments);
        _facMgr = Exec.getInstance().getFacilitiesManager();
    }

    protected void displayStatusForNode(final NodeInfo nodeInfo) {
        displayStatusForNodes(Collections.singletonList(nodeInfo));
    }

    protected void displayStatusForNodes(final Collection<NodeInfo> nodeInfos) {
        var componentStrings = new LinkedList<String>();
        for (var ni : nodeInfos) {
            try {
                componentStrings.add(_facMgr.getNodeStatusString(ni.getNode().getNodeIdentifier()));
            } catch (Exception ex) {
                // should never happen
            }
        }

        var sb = new StringBuilder();
        for (var str : componentStrings) {
            if (str.length() > 20) {
                if (!sb.isEmpty()) {
                    Exec.getInstance().sendExecReadOnlyMessage(sb.toString(), _source);
                    sb.setLength(0);
                }
                Exec.getInstance().sendExecReadOnlyMessage(str, _source);
            } else {
                if (sb.isEmpty()) {
                    sb.append(String.format("%-20s", str));
                } else {
                    sb.append(str);
                    Exec.getInstance().sendExecReadOnlyMessage(sb.toString(), _source);
                    sb.setLength(0);
                }
            }
        }

        if (!sb.isEmpty()) {
            Exec.getInstance().sendExecReadOnlyMessage(sb.toString(), _source);
        }
    }

    protected Collection<NodeInfo> getNodeInfoList() {
        var nodeInfos = new LinkedList<NodeInfo>();
        var error = false;
        var split = _arguments.split(",");
        for (var str : split) {
            var ni = _facMgr.getNodeInfo(str);
            if (ni == null) {
                var msg = String.format("%s is not a configured node", str.toUpperCase());
                Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
                error = true;
            } else {
                nodeInfos.add(ni);
            }
        }

        return error ? null : nodeInfos;
    }

    protected Collection<NodeInfo> getNodeInfoListForChannel() {
        var chName = _arguments.toUpperCase();
        if (!Parser.isValidNodeName(chName)) {
            var msg = "SYNTAX ERROR";
            Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
            return null;
        }

        var ni = _facMgr.getNodeInfo(_arguments);
        if (!(ni instanceof ChannelNodeInfo cni)) {
            var msg = String.format("%s is not a configured channel", chName);
            Exec.getInstance().sendExecReadOnlyMessage(msg, _source);
            return null;
        }

        return _facMgr.getNodeInfosForChannel(cni);
    }
}
