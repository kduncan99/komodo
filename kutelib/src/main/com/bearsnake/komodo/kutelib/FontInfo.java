/*
 * Copyright (c) 2025-2026 by Kurt Duncan - All Rights Reserved
 */

package com.bearsnake.komodo.kutelib;

import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/*
 * Font-related information shared among classes.
 */
public class FontInfo {

    private final Font _font;
    private final int _characterWidth;
    private final int _characterHeight;

    public FontInfo(final int fontSize)
    {
        _font = Font.font("Courier New", FontWeight.BOLD, fontSize);
        Text text = new Text("ABCDEFGHIJ");
        text.setFont(_font);
        _characterWidth = (int)(text.getLayoutBounds().getWidth() / 10.0) + 1;
        _characterHeight = (int)text.getLayoutBounds().getHeight();
    }

    public Font getFont() { return _font; }
    public int getCharacterWidth() { return _characterWidth; }
    public int getCharacterHeight() { return _characterHeight; }
}
