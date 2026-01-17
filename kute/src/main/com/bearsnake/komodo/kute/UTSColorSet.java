/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kute;

/*
 * Describes a color set containing FG and BG UTSColor values
 */
public class UTSColorSet {
    
    private final UTSColor _fgColor;
    private final UTSColor _bgColor;
    
    public UTSColorSet(final UTSColor fgColor,
                       final UTSColor bgColor) {
        _fgColor = fgColor;
        _bgColor = bgColor;
    }

    public UTSColor getFGColor() {
        return _fgColor;
    }

    public UTSColor getBGColor() {
        return _bgColor;
    }
}
