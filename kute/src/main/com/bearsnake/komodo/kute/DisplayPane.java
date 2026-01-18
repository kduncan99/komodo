/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kute;

import javafx.application.Platform;
import javafx.scene.canvas.Canvas;

import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.stream.IntStream;

import static com.bearsnake.komodo.kute.Constants.*;

/*
 * A DisplayPane is the actual character display for a Terminal.
 * It contains enough information to render a terminal display,
 * but most of the logic for manipulating the display belongs to the containing Terminal.
 */
public class DisplayPane extends Canvas {

    protected final Coordinates _cursorPosition = Coordinates.HOME_POSITION.copy();
    protected final StatusPane _statusPane;
    private UTSColor _bgColor;
    private UTSColor _textColor;
    private boolean _dimDisplay;
    private final FontInfo _fontInfo;
    protected DisplayGeometry _geometry;

    protected CharacterCell[] _characterCells;
    protected final TreeMap<Coordinates, Field> _fields = new TreeMap<>();

    // flashing assistance
    private Timer _blinkTimer;
    private int _blinkCounter;
    private boolean _blinkCursorFlag;
    private boolean _blinkCharacterFlag;

    public DisplayPane(final DisplayGeometry initialGeometry,
                       final FontInfo initialFontInfo,
                       final UTSColorSet initialDefaultColors,
                       final StatusPane statusPane) {
        _fontInfo = initialFontInfo;
        _bgColor = initialDefaultColors.getBGColor();
        _textColor = initialDefaultColors.getFGColor();
        _statusPane = statusPane;

        reconfigure(initialGeometry);

        _blinkTimer = new Timer(true);
        _blinkTimer.schedule(new BlinkTask(), 250, 250);
    }

    public void advanceCoordinates(final Coordinates coordinates) {
        int row = coordinates.getRow();
        int column = coordinates.getColumn();
        if (++column > _geometry.getColumns()) {
            column = 1;
            if (++row > _geometry.getRows()) {
                row = 1;
            }
        }
        coordinates.set(row, column);
    }

    public void backupCoordinates(final Coordinates coordinates) {
        int row = coordinates.getRow();
        int column = coordinates.getColumn();
        if (--column == 0) {
            column = _geometry.getColumns();
            if (--row == 0) {
                row = _geometry.getRows();
            }
        }
        coordinates.set(row, column);
    }

    /**
     * Cancels the blink timer and performs any other necessary cleanup
     */
    public void close() {
        _blinkTimer.cancel();
        _blinkTimer = null;
    }

    /*
     * Converts a byte to a character for display, handling special characters and cursor blinking.
     */
    private char convertByteToCharacter(final byte b, final boolean atCursor) {
        char ch = ' ';
        if ((atCursor && _blinkCursorFlag) && (!_dimDisplay)) {
            ch = '█';
        } else {
            if (b == ASCII_SOE) {
                ch = '▷';
            } else if (b == ASCII_HT) {
                ch = '⇥';
            } else if (b == ASCII_LF) {
                ch = '↓';
            } else if (b == ASCII_FF) {
                ch = '↖';
            } else if (b == ASCII_CR) {
                ch = '↲';
            } else if (b == ASCII_DEL) {
                ch = '░';
            } else if (b == ASCII_ESC) {
                ch = '∙';
            } else if (b == ASCII_FS) {
                ch = '«';
            } else if (b == ASCII_GS) {
                ch = '»';
            } else if (b >= ASCII_SP) {
                ch = (char)b;
            }
        }
        return ch;
    }

    private boolean coordinatesAtEndOfDisplay(final Coordinates coordinates) {
        return (coordinates.getRow() == _geometry.getRows())
               && (coordinates.getColumn() == _geometry.getColumns());
    }

    protected boolean coordinatesAtEndOfField(final Coordinates coordinates) {
        var ix = getIndex(coordinates);
        return (ix == _geometry.getCellCount())
            || (_characterCells[ix].getField() != _characterCells[ix + 1].getField());
    }

    private boolean coordinatesAtEndOfLine(final Coordinates coordinates) {
        return (coordinates.getColumn() == _geometry.getColumns());
    }

    public final void cycleBackgroundColor() {
        _bgColor = _bgColor.nextColor();
        _statusPane.notifyColorChange(new UTSColorSet(_textColor, _bgColor));
        scheduleDrawDisplay();
    }

    public final void cycleTextColor() {
        _textColor = _textColor.nextColor();
        _statusPane.notifyColorChange(new UTSColorSet(_textColor, _bgColor));
        scheduleDrawDisplay();
    }

    /**
     * Sets or clears the flag to dim the display (and hide the cursor).
     * This is useful for main displays when they are overlaid with a control page pane
     * @param flag true to hide the cursor, false to show it
     */
    public void dimDisplay(final boolean flag) {
        _dimDisplay = flag;
        scheduleDrawDisplay();
    }

    /*
     * Draws the character display.
     * Do not invoke this directly - use scheduleDrawStatusAction() instead.
     */
    private void drawDisplay() {
        var gcDisplay = getGraphicsContext2D();
        gcDisplay.setFont(_fontInfo.getFont());

        var coord = Coordinates.HOME_POSITION.copy();
        var cx = 0;
        do {
            var cell = _characterCells[cx];
            var field = cell.getField();
            var utsBgColor = field.getBackgroundColor() == null ? _bgColor : field.getBackgroundColor();
            var utsTextColor = field.getTextColor() == null ? _textColor : field.getTextColor();
            var intensity = field.getIntensity();
            var blink = field.isBlinking();
            var reverse = field.isReverseVideo();

            var byteChar = cell.getCharacter();
            var atCursor = (coord.getRow() == _cursorPosition.getRow()) && (coord.getColumn() == _cursorPosition.getColumn());
            var ch = convertByteToCharacter(byteChar, atCursor);
            var effectiveBlink = blink || (byteChar == ASCII_FS) || (byteChar == ASCII_GS);

            var jfxBgColor = utsBgColor.getFxTextColor();
            var jfxTextColor = utsTextColor.getFxTextColor();
            if (effectiveBlink && _blinkCharacterFlag) {
                jfxTextColor = jfxBgColor;
            }

            if (_dimDisplay) {
                jfxBgColor = jfxBgColor.darker().darker();
                jfxTextColor = jfxTextColor.darker().darker();
            } else if (intensity == Intensity.LOW) {
                jfxBgColor = jfxBgColor.darker();
                jfxTextColor = jfxTextColor.darker();
            }

            if (reverse) {
                var temp = jfxTextColor;
                jfxTextColor = jfxBgColor;
                jfxBgColor = temp;
            }

            // draw background first
            var x = (coord.getColumn() - 1) * _fontInfo.getCharacterWidth();
            var yRect = ((coord.getRow() - 1) * _fontInfo.getCharacterHeight());
            gcDisplay.setFill(jfxBgColor);
            gcDisplay.fillRect(x, yRect, _fontInfo.getCharacterWidth() + 1, _fontInfo.getCharacterHeight() + 1);

            // now draw text
            var yText = yRect + _fontInfo.getCharacterHeight() - 4;
            gcDisplay.setFill(jfxTextColor);// text color
            gcDisplay.fillText(String.valueOf(ch), x, yText);

            // now draw emphasis (if any)
            if (cell.getEmphasis().isColumnSeparator()) {
                gcDisplay.setStroke(jfxTextColor);// text color
                gcDisplay.setLineWidth(1.0);
                var y = yRect + _fontInfo.getCharacterHeight() - 1;
                gcDisplay.strokeLine(x, y, x, yRect);
            }
            if (cell.getEmphasis().isStrikeThrough()) {
                gcDisplay.setStroke(jfxTextColor);// text color
                gcDisplay.setLineWidth(1.0);
                var y = yRect + (_fontInfo.getCharacterHeight() / 2);
                gcDisplay.strokeLine(x, y, x + _fontInfo.getCharacterWidth() - 1, y);
            }
            if (cell.getEmphasis().isUnderscore()) {
                gcDisplay.setStroke(jfxTextColor);// text color
                gcDisplay.setLineWidth(1.0);
                var y = yRect + _fontInfo.getCharacterHeight() - 1;
                gcDisplay.strokeLine(x, y, x + _fontInfo.getCharacterWidth() - 1, y);
            }

            advanceCoordinates(coord);
            cx++;
        } while (!coord.atHome());
    }

    /*
     * Retrieves a reference to the character cell indicated by the given coordinates
     */
    protected CharacterCell getCharacterCell(final Coordinates coordinates) {
        return _characterCells[getIndex(coordinates)];
    }

    /**
     * Retrieves the color set for this display.
     * Intended only for use by Terminal, which must NOT update the object
     * @return UTSColorSet containing current default colors for this Display
     */
    public UTSColorSet getColorSet() {
        return new UTSColorSet(_textColor, _bgColor);
    }

    /**
     * Retrieves the cursor position from this display.
     * Intended only for the containing Terminal, which it can, but should not, update manually.
     * @return reference to our cursor position object
     */
    public Coordinates getCursorPosition() {
        return _cursorPosition;
    }

    /**
     * Retrieves the font information for this display.
     * Intended only for use by Terminal, which must NOT update the object
     * @return FontInfo
     */
    public FontInfo getFontInfo() {
        return _fontInfo;
    }

    /**
     * Retrieves the geometry this display is currently using.
     * Intended only for use by Terminal, which must NOT update the object
     * @return DisplayGeometry
     */
    public DisplayGeometry getGeometry() {
        return _geometry;
    }

    /*
     * Calculates the character cell index indicated by the given coordinates
     */
    protected int getIndex(final Coordinates coordinates) {
        return getIndex(coordinates.getRow(), coordinates.getColumn());
    }

    /*
     * Calculates the character cell index indicated by the given row and column
     */
    private int getIndex(final int row,
                         final int column) {
        return ((row - 1) * _geometry.getColumns()) + (column - 1);
    }

    /*
     * Finds the field which follows the given field.
     * If the given field is the end of the display, we return the home field.
     */
    private Field getNextField(final Field baseField) {
        if (baseField == _fields.lastEntry().getValue()) {
            return _fields.firstEntry().getValue();
        } else {
            return _fields.higherEntry(baseField.getCoordinates()).getValue();
        }
    }

    /**
     * Reconfigures our base class to the appropriate size
     */
    public void reconfigure(final DisplayGeometry geometry) {
        _geometry = geometry;
        _characterCells = new CharacterCell[_geometry.getCellCount()];
        setHeight(_geometry.getRows() * _fontInfo.getCharacterHeight());
        setWidth(_geometry.getColumns() * _fontInfo.getCharacterWidth());
        reset();
    }

    /*
     * Iterate over fields and character cells, rebuilding the field references in the
     * character cells according to the existing fields. This is used in cases where it
     * is difficult to fix up the references during line insertion, deletion, duplication,etc.
     */
    protected void repairFieldReferences() {
        var fIter = _fields.values().iterator();
        var field = fIter.next();
        var nextField = fIter.hasNext() ? fIter.next() : null;
        var coord = Coordinates.HOME_POSITION.copy();
        var cx = 0;
        while (cx < _geometry.getCellCount()) {
            if ((nextField != null) && coord.equals(nextField.getCoordinates())) {
                field = nextField;
                nextField = fIter.hasNext() ? fIter.next() : null;
            }
            _characterCells[cx++].setField(field);
            advanceCoordinates(coord);
        }
    }

    /**
     * Resets the display to its initial state, clearing all content and restoring default settings.
     */
    public void reset() {
        IntStream.range(0, _geometry.getCellCount())
                 .forEach(cx -> _characterCells[cx] = new CharacterCell());
        _fields.clear();
        _fields.put(Coordinates.HOME_POSITION, new ImplicitField());
        _cursorPosition.set(Coordinates.HOME_POSITION);
        _statusPane.notifyCursorPositionChange(_cursorPosition.getRow(), _cursorPosition.getColumn());
        repairFieldReferences();
        scheduleDrawDisplay();
    }

    /**
     * Moves the cursor to the next unprotected cell if the current cell is protected.
     * If there are no unprotected cells, the cursor is moved to the home position.
     * We do redraw the display.
     */
    public void resolveProtectedCell() {
        var baseField = getCharacterCell(_cursorPosition).getField();
        if (baseField.isProtected()) {
            var field = getNextField(baseField);
            do {
                if (!field.isProtected()) {
                    _cursorPosition.set(field.getCoordinates());
                    _statusPane.notifyCursorPositionChange(_cursorPosition.getRow(), _cursorPosition.getColumn());
                    scheduleDrawDisplay();
                    return;
                }
                field = getNextField(field);
            } while (!field.getCoordinates().equals(baseField.getCoordinates()));
            _statusPane.notifyCursorPositionChange(_cursorPosition.getRow(), _cursorPosition.getColumn());
            scheduleDrawDisplay();
        }
    }

    /*
     * Notifies the platform that it should schedule drawDisplay() to run in the graphics thread.
     */
    public void scheduleDrawDisplay() {
        Platform.runLater(this::drawDisplay);
    }

    /**
     * Updates the cursor position so the display can draw it properly
     * @param row row to be set
     * @param column column to be set
     */
    public void setCursorPosition(final int row,
                                  final int column) {
        _cursorPosition.setRow(row);
        _cursorPosition.setColumn(column);
        _statusPane.notifyCursorPositionChange(row, column);
        scheduleDrawDisplay();
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * A deleting back-space function.
     * If the character to the left of the cursor is not protected, delete it and move the cursor over the
     * location. This is very similar to just moving the cursor left and doing delete-to-end-of-field.
     * Only for keyboard activation - returns false if the space behind the cursor is protected.
     * Does nothing if the cursor is at the home position (but returns true).
     */
    public boolean backSpace() {
        if (!_cursorPosition.atHome()) {
            var cx = getIndex(_cursorPosition) - 1;
            if (!_characterCells[cx].getField()
                                    .isProtected()) {
                backupCoordinates(_cursorPosition);
                _statusPane.notifyCursorPositionChange(_cursorPosition.getRow(), _cursorPosition.getColumn());
                deleteInLine();
                _blinkCounter |= 0x01;
                scheduleDrawDisplay();
                return true;
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Clears changed status of all FCCs on the display.
     * @return always true
     */
    public boolean clearChangedBits() {
        for (var field : _fields.values()) {
            field.setChanged(false);
        }
        return true;
    }

    /**
     * Moves the cursor to the first column of the next line, wrapping to row one if necessary.
     * @return always true
     */
    public boolean cursorReturn() {
        _cursorPosition.setColumn(1);
        _cursorPosition.setRow(_cursorPosition.getRow() + 1);
        if (_cursorPosition.getRow() > _geometry.getRows()) {
            _cursorPosition.setRow(1);
        }
        _statusPane.notifyCursorPositionChange(_cursorPosition.getRow(), _cursorPosition.getColumn());
        _blinkCounter |= 0x01;
        scheduleDrawDisplay();
        return true;
    }

    /**
     * Moves the cursor to the home position
     * @return always true
     */
    public boolean cursorToHome() {
        _cursorPosition.set(Coordinates.HOME_POSITION.copy());
        _statusPane.notifyCursorPositionChange(_cursorPosition.getRow(), _cursorPosition.getColumn());
        _blinkCounter |= 0x01;
        scheduleDrawDisplay();
        return true;
    }

    /**
     * If the cursor is not protected, delete the character under the cursor
     * and shift subsequent characters left, up to the end of the field or the end of the display.
     * In our implementation, all cells are in a field, and the last field extends to the end of the display.
     * So we can achieve our goal by simply deleting to the end of the field.
     * @return true if successful, false if the field is protected
     */
    public boolean deleteInDisplay() {
        var coord = _cursorPosition.copy();
        var cell = getCharacterCell(_cursorPosition);
        var baseField = cell.getField();
        if (!baseField.isProtected()) {
            var nextCoord = coord.copy();
            advanceCoordinates(nextCoord);
            var nextCell = getCharacterCell(nextCoord);
            while (nextCell.getField() == baseField) {
                cell.setCharacter(nextCell.getCharacter());
                if (!baseField.isProtectedEmphasis()) {
                    cell.getEmphasis().set(nextCell.getEmphasis());
                }
                coord.set(nextCoord);
                advanceCoordinates(nextCoord);
            }

            cell = getCharacterCell(coord);
            cell.setCharacter(ASCII_SP);
            if (!baseField.isProtectedEmphasis()) {
                cell.getEmphasis().clear();
            }

            baseField.setChanged(true);
            _statusPane.notifyCursorPositionChange(_cursorPosition.getRow(), _cursorPosition.getColumn());
            _blinkCounter |= 0x01;
            scheduleDrawDisplay();
            return true;
        }
        return false;
    }

    /**
     * If the cursor is not protected, delete the character under the cursor
     * and shift subsequent characters left, up to the end of the field or the end of the line.
     * @return always true
     */
    public boolean deleteInLine() {
        var coord = _cursorPosition.copy();
        var cell = getCharacterCell(_cursorPosition);
        var baseField = cell.getField();
        if (!baseField.isProtected()) {
            var nextCoord = coord.copy();
            advanceCoordinates(nextCoord);
            var nextCell = getCharacterCell(nextCoord);
            while (nextCell.getField() == baseField) {
                cell.setCharacter(nextCell.getCharacter());
                if (!baseField.isProtectedEmphasis()) {
                    cell.getEmphasis().set(nextCell.getEmphasis());
                }
                coord.set(nextCoord);
                if (coordinatesAtEndOfLine(coord)) {
                    break;
                }
                advanceCoordinates(nextCoord);
            }

            cell = getCharacterCell(coord);
            cell.setCharacter(ASCII_SP);
            if (!baseField.isProtectedEmphasis()) {
                cell.getEmphasis().clear();
            }

            baseField.setChanged(true);
        }
        _statusPane.notifyCursorPositionChange(_cursorPosition.getRow(), _cursorPosition.getColumn());
        _blinkCounter |= 0x01;
        scheduleDrawDisplay();
        return true;
    }

    /**
     * Deletes the line under the cursor, shifting all subsequent lines (if any) up by one row.
     * The bottom row is initialized with blanks and no FCCs
     * @return always true
     */
    public boolean deleteLine() {
        // Shift character cells up by one row, from the bottom of the display to the cursor row
        for (int row = _cursorPosition.getRow(); row < _geometry.getRows(); row++) {
            for (int column = 1; column < _geometry.getColumns(); column++) {
                _characterCells[getIndex(row, column)] = _characterCells[getIndex(row + 1, column)];
            }
        }

        // Set the bottom row to blank characters
        var cx = getIndex(_geometry.getRows(), 1);
        while (cx < _geometry.getCellCount()) {
            _characterCells[cx++] = new CharacterCell();
        }

        // Delete the fields on the cursor row and shift those on subsequent rows upward by one row.
        var temp = new HashSet<Field>();
        var iter = _fields.keySet().iterator();
        while (iter.hasNext()) {
            var coord = iter.next();
            if (coord.getRow() == _cursorPosition.getRow()) {
                iter.remove();
            } else if (coord.getRow() > _cursorPosition.getRow()) {
                temp.add(_fields.get(coord));
                iter.remove();
            }
        }

        for (var field : temp) {
            field.getCoordinates().setRow(field.getCoordinates().getRow() - 1);
            _fields.put(field.getCoordinates(), field);
        }

        repairFieldReferences();
        _statusPane.notifyCursorPositionChange(_cursorPosition.getRow(), _cursorPosition.getColumn());
        _blinkCounter |= 0x01;
        scheduleDrawDisplay();
        return true;
    }

    /**
     * Duplicates the line containing the cursor to the line below, then moves the cursor to that line.
     * Ineffective if the cursor is already on the last line.
     * @return true if effective
     */
    public boolean duplicateLine() {
        if (_cursorPosition.getRow() < _geometry.getRows()) {
            var index = getIndex(_cursorPosition.getRow(), 0);
            for (int cx = 0; cx < _geometry.getColumns(); cx++) {
                _characterCells[index] = _characterCells[index + _geometry.getColumns()].copy();
            }

            var iter = _fields.keySet().iterator();
            while (iter.hasNext()) {
                var coord = iter.next();
                if (coord.getRow() == _cursorPosition.getRow()) {
                    iter.remove();
                } else if (coord.getRow() > _cursorPosition.getRow()) {
                    var newCoord = new Coordinates(_cursorPosition.getRow() - 1, _geometry.getColumns());
                    var newField = _fields.get(coord).copy(newCoord);
                    _fields.put(newCoord, newField);
                }
            }

            repairFieldReferences();
            _cursorPosition.setRow(_cursorPosition.getRow() + 1);
            _statusPane.notifyCursorPositionChange(_cursorPosition.getRow(), _cursorPosition.getColumn());
            _blinkCounter |= 0x01;
            scheduleDrawDisplay();
            return true;
        }
        return false;
    }

    /**
     * Erases all data from the cursor to the end of the screen - protected or otherwise (including FCCs).
     * This is NOT the same as the host-initiated function.
     * @return always true
     */
    public boolean eraseDisplay() {
        var baseCoord = _cursorPosition.copy();
        var baseCell = getCharacterCell(baseCoord);
        var baseField = baseCell.getField();

        // Replace characters with blanks, clear emphasis, and fix up field references.
        var coord = baseCoord.copy();
        do {
            var cell = getCharacterCell(coord);
            cell.setCharacter(ASCII_SP);
            cell.getEmphasis().clear();
            cell.setField(baseField);
            advanceCoordinates(coord);
        } while (!coord.atHome());

        // Remove subsequent FCCs
        _fields.keySet().removeIf(fieldCoord -> fieldCoord.compareTo(baseField.getCoordinates()) > 0);

        baseField.setChanged(true);
        scheduleDrawDisplay();
        return true;
    }

    /**
     * Erases unprotected data from the cursor to the end of the screen,
     * setting (unprotected) changed field bits to false.
     * No host-initiated action here, only keyboard.
     * @return always true
     */
    public boolean eraseToEndOfDisplay() {
        var cx = getIndex(_cursorPosition);
        while (cx < _geometry.getCellCount()) {
            var cell = _characterCells[cx];
            var field = cell.getField();
            if (!field.isProtected()) {
                cell.setCharacter(ASCII_SP);
                if (!cell.getField().isProtectedEmphasis()) {
                    cell.getEmphasis().clear();
                }
                field.setChanged(false);
            }
        }
        return true;
    }

    /**
     * Only allowed if the cursor is not in a protected field.
     * Erases all characters to the end of the field, or to the end of the display.
     * @return true if allowed, else false
     */
    public boolean eraseToEndOfField() {
        var baseField = getCharacterCell(_cursorPosition).getField();
        if (!baseField.isProtected()) {
            var coord = _cursorPosition.copy();
            var cx = getIndex(coord);
            do {
                _characterCells[cx].setCharacter(ASCII_SP);
                if (!baseField.isProtectedEmphasis()) {
                    _characterCells[cx].getEmphasis().clear();
                }
                advanceCoordinates(coord);
                cx++;
            } while (!coord.atHome() && (_characterCells[cx].getField() == baseField));

            baseField.setChanged(true);
            scheduleDrawDisplay();
            return true;
        }
        return false;
    }

    /**
     * Only allowed if the cursor is not in a protected field...
     * Erases all characters to the end of the field, or to the end of the line
     * @return true if allowed, else false
     */
    public boolean eraseToEndOfLine() {
        var baseField = getCharacterCell(_cursorPosition).getField();
        if (!baseField.isProtected()) {
            var coord = _cursorPosition.copy();
            var cx = getIndex(coord);
            do {
                _characterCells[cx].setCharacter(ASCII_SP);
                if (!baseField.isProtectedEmphasis()) {
                    _characterCells[cx].getEmphasis().clear();
                }
                if (coordinatesAtEndOfLine(coord)) {
                    break;
                }
                advanceCoordinates(coord);
                cx++;
            } while (_characterCells[cx].getField() == baseField);

            baseField.setChanged(true);
            return true;
        }
        scheduleDrawDisplay();
        return false;
    }

    /**
     * Erases all unprotected data (not including FCCs) from the cursor to the end of the display.
     * Moves across multiple FCCs (does not stop at the end of the current field).
     * @return always true
     */
    public boolean eraseUnprotectedData() {
        var cx = getIndex(_cursorPosition);
        do {
            var cell = _characterCells[cx];
            if (!cell.getField().isProtected()) {
                _characterCells[cx].setCharacter(ASCII_SP);
                if (!cell.getField().isProtectedEmphasis()) {
                    _characterCells[cx].getEmphasis().clear();
                }
            }
            cx++;
        } while (cx < _geometry.getCellCount());
        scheduleDrawDisplay();
        return true;
    }

    /**
     * Erases the FCC controlling the cursor position, if any
     * @return true if successful, else false
     */
    public boolean fccClear() {
        var field = _fields.floorEntry(_cursorPosition).getValue();
        if (field.isExplicit()) {
            if (field.getCoordinates().atHome()) {
                // field to be deleted is at the home position, and it is not implicit.
                // create a new implicit field to replace it.
                var implicit = new ImplicitField();
                Arrays.stream(_characterCells).forEach(cell -> cell.setField(implicit));
                _fields.put(implicit.getCoordinates(), implicit);
            } else {
                // delete field, and let the previous field adopt all its cells
                var prev = _fields.lowerEntry(field.getCoordinates()).getValue();
                Arrays.stream(_characterCells).forEach(cell -> cell.setField(prev));
                _fields.remove(field.getCoordinates());
            }
            _blinkCounter |= 0x01;
            scheduleDrawDisplay();
            return true;
        }
        return false;
    }

    /**
     * Enables the FCCs on the display - initiated by keyboard only
     * @return always true
     */
    public boolean fccEnable() {
        _fields.values().forEach(f -> f.setEnabled(true));
        return true;
    }

    /**
     * Moves the cursor to the next field, and disables all FCCs in the display.
     * @return always true
     */
    public boolean fccLocate() {
        var nextField = getNextField(getCharacterCell(_cursorPosition).getField());
        _cursorPosition.set(nextField.getCoordinates());
        _fields.values().forEach(f -> f.setEnabled(false));
        _statusPane.notifyCursorPositionChange(_cursorPosition.getRow(), _cursorPosition.getColumn());
        _blinkCounter |= 0x01;
        scheduleDrawDisplay();
        return true;
    }

    /**
     * If the cursor is not in a protected field, the characters from the cursor to the end of the field
     * or the end of the display, are shifted right and a blank is placed under the cursor.
     * The last character in the field or display is lost.
     * If we are in an emphasis-protected field, the emphasis characters are not affected.
     * FCCs are not affected, and the cursor does not move.
     * @return true if we are not in a protected field, else false
     */
    public boolean insertInDisplay() {
        var baseIndex = getIndex(_cursorPosition.copy());
        var baseField = _characterCells[baseIndex].getField();
        if (!baseField.isProtected()) {
            // Find the index of the last position in the field.
            // If this is the last field in the display, it will also be the last position in the display.
            var nextIndex = baseIndex++;
            while ((_characterCells[nextIndex].getField() == baseField) && (nextIndex < (_geometry.getCellCount()))) {
                nextIndex++;
            }
            nextIndex--;

            // Now shift characters (and maybe emphasis) to the right, from the right.
            while (nextIndex != baseIndex) {
                _characterCells[nextIndex].setCharacter(_characterCells[nextIndex - 1].getCharacter());
                if (!baseField.isProtectedEmphasis()) {
                    _characterCells[nextIndex].getEmphasis().set(_characterCells[nextIndex - 1].getEmphasis());
                }
                nextIndex--;
            }

            // Blank the character under the cursor
            _characterCells[baseIndex].setCharacter(ASCII_SP);
            if (!baseField.isProtectedEmphasis()) {
                _characterCells[baseIndex].getEmphasis().clear();
            }

            baseField.setChanged(true);
            _blinkCounter |= 0x01;
            scheduleDrawDisplay();
            return true;
        }
        return false;
    }

    /**
     * If the cursor is not in a protected field, the characters from the cursor to the end of the field
     * or the end of the line, are shifted right and a blank is placed under the cursor.
     * The last character in the field or line is lost.
     * If we are in an emphasis-protected field, the emphasis characters are not affected.
     * FCCs are not affected, and the cursor does not move.
     * @return true if we are not in a protected field, else false
     */
    public boolean insertInLine() {
        var baseIndex = getIndex(_cursorPosition.copy());
        var baseField = _characterCells[baseIndex].getField();
        if (!baseField.isProtected()) {
            // Find the index of the last position in the field or the current line.
            var nextIndex = baseIndex++;
            var nextColumn = _cursorPosition.getColumn() + 1;
            while ((_characterCells[nextIndex].getField() == baseField) && (nextColumn < _geometry.getColumns())) {
                nextIndex++;
            }
            nextIndex--;

            // Now shift characters (and maybe emphasis) to the right, from the right.
            while (nextIndex != baseIndex) {
                _characterCells[nextIndex].setCharacter(_characterCells[nextIndex - 1].getCharacter());
                if (!baseField.isProtectedEmphasis()) {
                    _characterCells[nextIndex].getEmphasis().set(_characterCells[nextIndex - 1].getEmphasis());
                }
                nextIndex--;
            }

            // Blank the character under the cursor
            _characterCells[baseIndex].setCharacter(ASCII_SP);
            if (!baseField.isProtectedEmphasis()) {
                _characterCells[baseIndex].getEmphasis().clear();
            }

            baseField.setChanged(true);
            _blinkCounter |= 0x01;
            scheduleDrawDisplay();
            return true;
        }
        return false;
    }

    /**
     * Shifts all the lines starting at the cursor down by one line,
     * with the last line on the display simply being dropped.
     * The line under the cursor is then overwritten with blanks.
     * @return always true
     */
    public boolean insertLine() {
        // handle character cells
        if (_cursorPosition.getRow() < _geometry.getRows()) {
            var lastRowIndex = getIndex(_geometry.getRows(), 0);
            for (int cx = 0; cx < _geometry.getColumns(); cx++) {
                _characterCells[lastRowIndex + cx] = _characterCells[lastRowIndex - 80 + cx];
            }
        }
        int ix = getIndex(_cursorPosition.getRow(), 0);
        for (int cx = 0; cx < _geometry.getColumns(); cx++) {
            _characterCells[ix + cx] = new CharacterCell();
        }

        // handle fields
        var tempFields = new LinkedList<Field>(_fields.values());
        _fields.clear();
        for (var field : tempFields) {
            var fieldCoords = field.getCoordinates();
            var fieldRow = fieldCoords.getRow();
            if (fieldRow < _geometry.getRows()) {
                if (field.getCoordinates().getRow() >= _cursorPosition.getRow()) {
                    fieldCoords.setRow(fieldRow + 1);
                }
                _fields.put(fieldCoords, field);
            }
        }

        repairFieldReferences();
        _blinkCounter |= 0x01;
        scheduleDrawDisplay();
        return true;
    }

    /**
     * Initiated from keyboard.
     * If the area under the cursor is not protected, place the character on-screen at the cursor,
     * and advance the cursor to the next unprotected location.
     * We clear any special emphasis IFF the cell we are in is not emphasis-protected.
     * We also observe right-justification.
     * @return true if not prohibited, else false
     */
    public boolean kbPutCharacter(final byte ch) {
        // Check prohibitions
        var cell = getCharacterCell(_cursorPosition);
        var field = cell.getField();
        if (field.isProtected()
            || (field.isNumericOnly() && !Character.isDigit(ch))
            || (field.isAlphabeticOnly() && !Character.isLetter(ch))) {
            return false;
        }

        // Are we in the first column of a right-justified field?
        if (field.isRightJustified() && (_cursorPosition.equals(field.getCoordinates()))) {
            // Shift character in this field left until we get to the end of the field or the line.
            // If emphasis is not protected, shift that as well.
            // Note whether we shifted a non-blank into the cursor position.
            var leftCoord = _cursorPosition.copy();
            var rightCoord = leftCoord.copy();
            advanceCoordinates(rightCoord);

            var isFirst = true;
            var fieldFull = false;
            while (!coordinatesAtEndOfField(leftCoord) && !coordinatesAtEndOfLine(leftCoord)) {
                var lx = getIndex(leftCoord);
                _characterCells[lx].setCharacter(_characterCells[lx + 1].getCharacter());
                if (isFirst && (_characterCells[lx].getCharacter() != ASCII_SP)) {
                    fieldFull = true;
                }

                if (!field.isProtectedEmphasis()) {
                    _characterCells[lx].getEmphasis().set(_characterCells[lx + 1].getEmphasis());
                }
                isFirst = false;
            }

            // Move the new character into the last position. If emphasis is not protected, clear it.
            var lx = getIndex(leftCoord);
            _characterCells[lx].setCharacter(ch);
            if (!field.isProtectedEmphasis()) {
                _characterCells[lx].getEmphasis().clear();
            }

            field.setChanged(true);

            // If the field is full, move cursor to first unprotected character after the field.
            // We *know* there is at least one unprotected field, we are in one right now.
            if (fieldFull) {
                var nextField = getNextField(field);
                while (nextField.isProtected()) {
                    nextField = getNextField(nextField);
                }
                _cursorPosition.set(nextField.getCoordinates());
            }
        } else {
            // Are we in a right-justified field AND on a row below the start of the field?
            // If so, we cannot allow the user to enter anything here...
            if (field.isRightJustified() && (_cursorPosition.getRow() > field.getCoordinates().getRow())) {
                return false;
            }

            cell.setCharacter(ch);
            if (!field.isProtectedEmphasis()) {
                cell.getEmphasis().clear();
            }
            advanceCoordinates(_cursorPosition);
        }

        _statusPane.notifyCursorPositionChange(_cursorPosition.getRow(), _cursorPosition.getColumn());
        _blinkCounter |= 0x01;
        scheduleDrawDisplay();
        return true;
    }

    /**
     * For character placement initiated by the host.
     * We place the character (excepting in the case of SUB, which merely moves the cursor),
     * and apply the current emphasis setting if the target cell is not emphasis-protected.
     * Advances the cursor with no regard to field protection.
     * @param ch character to be placed
     * @param emphasisAction emphasis action to be taken
     * @param emphasis emphasis character for the emphasis action
     * @return always true
     */
    public boolean putCharacter(final byte ch,
                                final EmphasisAction emphasisAction,
                                final Emphasis emphasis) {
        var cell = getCharacterCell(_cursorPosition);
        if (ch != ASCII_SUB) {
            cell.setCharacter(ch);
        }

        if (cell.getField().isProtectedEmphasis()) {
            switch (emphasisAction) {
                case NONE -> {}
                case ADD -> cell.getEmphasis().add(emphasis);
                case REMOVE -> cell.getEmphasis().remove(emphasis);
                case SET -> cell.getEmphasis().set(emphasis);
            }
        }

        advanceCoordinates(_cursorPosition);
        _statusPane.notifyCursorPositionChange(_cursorPosition.getRow(), _cursorPosition.getColumn());
        scheduleDrawDisplay();
        return true;
    }

    /**
     * Places an FCC based on the provided Field
     * @return always true
     */
    public boolean putFCC(final Field field) {
        _fields.put(field.getCoordinates(), field);
        repairFieldReferences();
        return true;
    }

    /**
     * Moves the cursor down by one line, wrapping around to the first line if necessary
     * @return always true
     */
    public boolean scanDown() {
        _cursorPosition.setRow(_cursorPosition.getRow() + 1);
        if (_cursorPosition.getRow() > _geometry.getRows()) {
            _cursorPosition.setRow(1);
        }
        _statusPane.notifyCursorPositionChange(_cursorPosition.getRow(), _cursorPosition.getColumn());
        return true;
    }

    /**
     * Moves the cursor left by one character, wrapping to the previous row if necessary.
     * @return always true
     */
    public boolean scanLeft() {
        _cursorPosition.setColumn(_cursorPosition.getColumn() - 1);
        if (_cursorPosition.getColumn() == 0) {
            _cursorPosition.setColumn(_geometry.getColumns());
            _cursorPosition.setRow(_cursorPosition.getRow() - 1);
            if (_cursorPosition.getRow() == 0) {
                _cursorPosition.setRow(_geometry.getRows());
            }
        }
        _statusPane.notifyCursorPositionChange(_cursorPosition.getRow(), _cursorPosition.getColumn());
        return true;
    }

    /**
     * Moves the cursor right by one character, wrapping to the next row if necessary.
     * @return always true
     */
    public boolean scanRight() {
        _cursorPosition.setColumn(_cursorPosition.getColumn() + 1);
        if (_cursorPosition.getColumn() > _geometry.getColumns()) {
            _cursorPosition.setColumn(1);
            _cursorPosition.setRow(_cursorPosition.getRow() + 1);
            if (_cursorPosition.getRow() > _geometry.getRows()) {
                _cursorPosition.setRow(1);
            }
        }
        _statusPane.notifyCursorPositionChange(_cursorPosition.getRow(), _cursorPosition.getColumn());
        return true;
    }

    /**
     * Moves the cursor up by one line, wrapping around to the last line if necessary
     * @return always true
     */
    public boolean scanUp() {
        _cursorPosition.setRow(_cursorPosition.getRow() - 1);
        if (_cursorPosition.getRow() == 0) {
            _cursorPosition.setRow(_geometry.getRows());
        }
        _statusPane.notifyCursorPositionChange(_cursorPosition.getRow(), _cursorPosition.getColumn());
        return true;
    }

    /**
     * Sets the cursor position
     * @return always true
     */
    public boolean setCursorPosition(final Coordinates coordinates) {
        _cursorPosition.setRow(Math.min(coordinates.getRow(), _geometry.getRows()));
        _cursorPosition.setColumn(Math.min(coordinates.getColumn(), _geometry.getColumns()));
        _statusPane.notifyCursorPositionChange(_cursorPosition.getRow(), _cursorPosition.getColumn());
        return true;
    }

    /**
     * Move the cursor to the previous tab stop.
     * If this is an FCC with tab-stop set, we stop on that position.
     * If it is a set-tab tab stop, the cursor moves to the first unprotected cell after that tab stop.
     * However, if that set-tab tab stop is protected and protected and the cursor starts immediately
     * following that protected field, the cursor goes to the next previous unprotected tab stop
     * (FCC or set-tab).
     * If there are no tab stops back to the home position, or if all cells back to the home position
     * are protected, the cursor is placed at the home position.
     * @return always true
     */
     public boolean tabBackward() {
        // TODO
        return true;
    }

    /**
     * Move the cursor to the next tab stop.
     * This might be an FCC with tab-stop set, in which case we do not worry about field protection.
     * If it is a tab stop established by the set-tab (i.e., a tab taking up a screen position),
     * we place the cursor at the first unprotected location following that tab-stop, potentially
     * continuing from the home position after reaching the end of the display.
     * If there are no tab stops or no unprotected cells following a tab-set,
     * the cursor is placed at the home position.
     * @return always true
     */
    public boolean tabForward() {
        var startingPoint = _cursorPosition.copy();
        var foundTabSet = false;
        do {
            advanceCoordinates(_cursorPosition);
            var cell = getCharacterCell(_cursorPosition);
            if (cell.getField().isTabStop()) {
                _statusPane.notifyCursorPositionChange(_cursorPosition.getRow(), _cursorPosition.getColumn());
                return true;
            }

            if (cell.getCharacter() == ASCII_HT) {
                foundTabSet = true;
            } else if (foundTabSet && !cell.getField().isProtected()) {
                _statusPane.notifyCursorPositionChange(_cursorPosition.getRow(), _cursorPosition.getColumn());
                return true;
            }
        } while (!_cursorPosition.atHome());

        // If we had found a tab on the display, we keep going until we find an unprotected cell,
        // or it's clear there are no unprotected cells.
        if (foundTabSet) {
            do {
                var cell = getCharacterCell(_cursorPosition);
                if (!cell.getField().isProtected()) {
                    break;
                }
                advanceCoordinates(_cursorPosition);
            } while (!_cursorPosition.equals(startingPoint));
        }

        _statusPane.notifyCursorPositionChange(_cursorPosition.getRow(), _cursorPosition.getColumn());
        return true;
    }

    /**
     * Toggles the specified emphasis on the character cell at the cursor position.
     * If the field is protected, the emphasis is not toggled.
     * @param emphasis the emphasis to toggle
     * @return true if the emphasis was toggled, false if the field is protected
     */
    public boolean toggleEmphasis(final Emphasis emphasis) {
        var cell = getCharacterCell(_cursorPosition);
        if (!cell.getField().isProtectedEmphasis()) {
            var ce = cell.getEmphasis();
            if (emphasis.isColumnSeparator()) {
                ce.setColumnSeparator(!ce.isColumnSeparator());
            }
            if (emphasis.isStrikeThrough()) {
                ce.setStrikeThrough(!ce.isStrikeThrough());
            }
            if (emphasis.isUnderscore()) {
                ce.setUnderscore(!ce.isUnderscore());
            }
            return true;
        } else {
            return false;
        }
    }

    // -----------------------------------------------------------------------------------------------------------------

    /**
     * Invokes the appropriate submethod based on the provided PrintMode
     */
    public byte[] getPrintStream(final PrintMode mode) {
        return switch (mode) {
            case PRINT -> getPrintStreamAll();
            case FORM -> getPrintStreamForm();
            case TRANSPARENT -> getPrintStreamTransparent();
        };
    }

    /**
     * Retrieves a print image from the content of the display from the SOE (non-inclusive) or the home position,
     * to the cursor (inclusive). Trailing blanks are removed from the ends of rows, and each row is terminated by a CR
     * excepting the row with the cursor.
     * @return the print stream
     */
    private byte[] getPrintStreamAll() {
        var strm = new ByteArrayOutputStream(2048);
        /* TODO
        var region = determinePrintRegion();
        var coord = region.getStartingCoordinate();

        var attr = getControllingAttributes(coord);
        var isProtected = (attr != null) && attr.isProtected();
        var pending = new ByteArrayOutputStream();
        var blanks = new ByteArrayOutputStream();

        for (int cx = 0; cx < region.getExtent(); cx++) {
            var cell = getCharacterCell(coord);
            attr = cell.getAttributes();
            if (attr != null) {
                isProtected = attr.isProtected();
            }

            var ch = (!isProtected || printAll) ? cell.getCharacter() : ASCII_SP;
            if (ch != ASCII_SP) {
                pending.write(blanks.toByteArray(), 0, blanks.size());
                blanks.reset();
                pending.write(cell.getCharacter());
                    } else {
                blanks.write(ASCII_SP);
            }

            advanceCoordinates(coord);
            if (coord.getColumn() == 1) {
                pending.write(ASCII_CR);
                blanks.reset();
            }
        }

        pending.write(blanks.toByteArray(), 0, blanks.size());
        sendToPrinter(pending.toByteArray(), pending.size());
         */
        return strm.toByteArray();
    }

    /**
     * Retrieves a print image from the content of the screen from the SOE (non-inclusive) or the home position,
     * to the cursor (inclusive). Protected content is replaced by blanks.
     * Trailing blanks are removed from the ends of rows, and each row is terminated by a CR
     * excepting the row with the cursor.
     * @return the print stream
     */
    private byte[] getPrintStreamForm() {
        var strm = new ByteArrayOutputStream(2048);
        // TODO
        return strm.toByteArray();
    }

    // Print everything from the SOE most-previous to the cursor (non-inclusive)
    // up to the cursor (inclusive) - do not translate anything, do not send CRs at the end
    // of display lines. Ignore FCCs.
    private byte[] getPrintStreamTransparent() {
        var strm = new ByteArrayOutputStream(2048);
        var cursorIndex = getIndex(_cursorPosition);
        for (int cx = 0; cx <= cursorIndex; cx++) {
            if (_characterCells[cx].getCharacter() == ASCII_SOE) {
                strm.reset();
            } else {
                strm.write(_characterCells[cx].getCharacter());
            }
        }
        return strm.toByteArray();
    }

    public byte[] getTransmitStream(final TransmitMode mode) {
        return switch (mode) {
            case ALL -> getTransmitStreamAll();
            case CHANGED -> getTransmitStreamChanged();
            case VARIABLE -> getTransmitStreamVariable();
        };
    }

    private byte[] getTransmitStreamAll() {
        var strm = new ByteArrayOutputStream(2048);
        // TODO
        return strm.toByteArray();
    }

    private byte[] getTransmitStreamChanged() {
        var strm = new ByteArrayOutputStream(1024);
        // TODO
        return strm.toByteArray();
    }

    private byte[] getTransmitStreamVariable() {
        var strm = new ByteArrayOutputStream(1024);
        // TODO
        return strm.toByteArray();
    }

    // -----------------------------------------------------------------------------------------------------------------

    public class BlinkTask extends TimerTask {

        public void run() {
            _blinkCounter = (_blinkCounter + 1) & 0x3;
            _blinkCursorFlag = (_blinkCounter & 0x01) != 0;
            _blinkCharacterFlag = (_blinkCounter & 0x02) != 0;
            scheduleDrawDisplay();
        }
    }
}
