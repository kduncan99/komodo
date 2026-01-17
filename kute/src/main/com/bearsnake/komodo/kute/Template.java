/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kute;

import static com.bearsnake.komodo.kute.TransmitMode.ALL;

public class Template {

    private String _templateName;
    private String _hostName;
    private int _port;
    private int _rows;
    private int _columns;
    private UTSColor _backgroundColor;
    private UTSColor _textColor;
    private boolean _returnKeyIsXMIT;
    private boolean _spaceBarIsEraseChar;
    private boolean _sendExpandedFCCs;
    private boolean _sendColorFCCs;

    public Template(final String templateName,
                    final String hostName,
                    final int port,
                    final int rows,
                    final int columns,
                    final UTSColor backgroundColor,
                    final UTSColor textColor,
                    final boolean returnKeyIsXMIT,
                    final boolean spaceBarIsEraseChar,
                    final boolean sendExpandedFCCs,
                    final boolean sendColorFCCs) {
        _templateName = templateName;
        _hostName = hostName;
        _port = port;
        _rows = rows;
        _columns = columns;
        _backgroundColor = backgroundColor;
        _textColor = textColor;
        _returnKeyIsXMIT = returnKeyIsXMIT;
        _spaceBarIsEraseChar = spaceBarIsEraseChar;
        _sendExpandedFCCs = sendExpandedFCCs;
        _sendColorFCCs = sendColorFCCs;
    }

    public Template(final String templateName,
                    final String hostName,
                    final int port) {
        _templateName = templateName;
        _hostName = hostName;
        _port = port;
        _rows = 24;
        _columns = 80;
        _backgroundColor = UTSColor.BLACK;
        _textColor = UTSColor.GREEN;
        _returnKeyIsXMIT = false;
        _spaceBarIsEraseChar = true;
        _sendExpandedFCCs = true;
        _sendColorFCCs = true;
    }

    public String getTemplateName() { return _templateName; }
    public String getHostName() { return _hostName; }
    public int getPort() { return _port; }
    public int getRows() { return _rows; }
    public int getColumns() { return _columns; }
    public UTSColor getBackgroundColor() { return _backgroundColor; }
    public UTSColor getTextColor() { return _textColor; }
    public boolean getReturnKeyIsXMIT() { return _returnKeyIsXMIT; }
    public boolean getSpaceBarIsEraseChar() { return _spaceBarIsEraseChar; }
    public boolean getSendExpandedFCCs() { return _sendExpandedFCCs; }
    public boolean getSendColorFCCs() { return _sendColorFCCs; }

    public Template setTemplateName(final String value) { _templateName = value; return this; }
    public Template setHostName(final String value) { _hostName = value; return this; }
    public Template setPort(final int value) { _port = value; return this; }
    public Template setRows(final int value) { _rows = value; return this; }
    public Template setColumns(final int value) { _columns = value; return this; }
    public Template setBackgroundColor(final UTSColor value) { _backgroundColor = value; return this; }
    public Template setTextColor(final UTSColor value) { _textColor = value; return this; }
    public Template setReturnKeyIsXMIT(final boolean value) { _returnKeyIsXMIT = value; return this; }
    public Template setSpaceBarIsEraseChar(final boolean value) { _spaceBarIsEraseChar = value; return this; }
    public Template setSendExpandedFCCs(final boolean value) { _sendExpandedFCCs = value; return this; }
    public Template setSendColorFCCs(final boolean value) { _sendColorFCCs = value; return this; }
}
