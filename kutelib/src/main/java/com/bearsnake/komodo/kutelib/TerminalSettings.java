/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib;

import com.bearsnake.komodo.kutelib.panes.DisplayGeometry;
import com.bearsnake.komodo.kutelib.panes.UTSColorSet;
import com.bearsnake.komodo.utslib.fields.UTSColor;

public class TerminalSettings {

    private String _hostName = "localhost";
    private int _hostPort = 2200;
    private String _fontFamily = "New Courier";
    private int _fontSize = 16;
    private DisplayGeometry _displayGeometry = new DisplayGeometry(24, 80);
    private UTSColorSet _colorSet = new UTSColorSet(UTSColor.GREEN, UTSColor.BLACK); // effective even if _colorSupported is false
    private boolean _returnKeyIsTransmit = true;
    private boolean _escapeKeyIsMessageWait = true;
    private boolean _sendExpandedFCCs = true;

    private boolean _colorSupported = true; // not suggested when _emphasisSupported is true
    private boolean _graphicsSupported = false; // not suggested when _emphasisSupported is true
    private boolean _emphasisSupported = false; // not suggested when _colorSupported or _graphicsSupported is true

    public TerminalSettings() {}

    public String getHostName() { return _hostName; }
    public int getHostPort() { return _hostPort; }
    public String getFontFamily() { return _fontFamily; }
    public int getFontSize() { return _fontSize; }
    public DisplayGeometry getDisplayGeometry() { return _displayGeometry; }
    public UTSColorSet getColorSet() { return _colorSet; }
    public boolean getReturnKeyIsTransmit() { return _returnKeyIsTransmit; }
    public boolean getEscapeKeyIsMessageWait() { return _escapeKeyIsMessageWait; }
    public boolean getSendExpandedFCCs() { return _sendExpandedFCCs; }

    public boolean isColorSupported() { return _colorSupported; }
    public boolean isEmphasisSupported() { return _emphasisSupported; }
    public boolean isGraphicsSupported() { return _graphicsSupported; }

    public TerminalSettings setHostName(final String value) { _hostName = value; return this; }
    public TerminalSettings setHostPort(final int value) { _hostPort = value; return this; }
    public TerminalSettings setFontFamily(final String value) { _fontFamily = value; return this; }
    public TerminalSettings setFontSize(final int value) { _fontSize = value; return this; }
    public TerminalSettings setDisplayGeometry(final DisplayGeometry value) { _displayGeometry = value; return this; }
    public TerminalSettings setColorSet(final UTSColorSet value) { _colorSet = value; return this; }
    public TerminalSettings setReturnKeyIsTransmit(final boolean value) { _returnKeyIsTransmit = value; return this; }
    public TerminalSettings setEscapeKeyIsMessageWait(final boolean value) { _escapeKeyIsMessageWait = value; return this; }
    public TerminalSettings setSendExpandedFCCs(final boolean value) { _sendExpandedFCCs = value; return this; }

    public TerminalSettings setColorIsSupported(final boolean value) { _colorSupported = value; return this; }
    public TerminalSettings setEmphasisIsSupported(final boolean value) { _emphasisSupported = value; return this; }
    public TerminalSettings setGraphicsIsSupported(final boolean value) { _graphicsSupported = value; return this; }
}
