package ccc.chess.gui.chessforall;

import android.graphics.Color;

public class ColorValues
{
    public ColorValues()
    {
        setColors(0, COLORS_0);
    }

    public void setColors(int colorId, String colorValues)
    {
        this.colorId = colorId;
        colors = colorValues.split(" ");
        if (colors.length != colorCnt)
        {
            switch (colorId)
            {
                case 0:
                    colors = COLORS_0.split(" ");
                    break;
                case 1:
                    colors = COLORS_1.split(" ");
                    break;
                case 2:
                    colors = COLORS_2.split(" ");
                    break;
                case 3:
                    colors = COLORS_3.split(" ");
                    break;
                case 4:
                    colors = COLORS_4.split(" ");
                    break;
            }
        }
    }

    public int getColor(int color)
    {
        return Color.parseColor(colors[color]);
    }

    int colorId = 0;
    String[] colors = null;
    final int colorCnt = 24;

    // color values
    final static String COLORS_0 = "? #f9f9cc #e2a156 #ffffff #000000 #e1817e #27ea15 #27ea15 #efe395 #000000 #427e29 #b2d9e4 #ced1d6 #000000 #f6d2f4 #000000 #c4f8c0 #000000 #f6d2f4 #000000 #c4f8c0 #000000 #f6d2f4 #f43619";
    final static String COLORS_1 = "? #E3C392 #C18A48 #ffffff #000000 #e1817e #F7EE04 #27ea15 #DBC39D #121201 #396f39 #21accf #CCD6E7 #000000 #f6d2f4 #000000 #c4f8c0 #000000 #f6d2f4 #000000 #c4f8c0 #000000 #EAA7E6 #EC1855";
    final static String COLORS_2 = "? #BDBDB2 #817D74 #ffffff #000000 #e1817e #E1D465 #27ea15 #ABAB95 #100F01 #396f39 #21accf #ced1d6 #000000 #f6d2f4 #000000 #D5E9D4 #000000 #EBABE8 #000000 #c4f8c0 #000000 #D0AFCE #f43619";
    final static String COLORS_3 = "? #D2EEE9 #85CDDF #c6beaf #000000 #e1817e #F0DC24 #f6e41a #D3E9E8 #101002 #396f39 #21accf #ced1d6 #000000 #f6d2f4 #000000 #c4f8c0 #000000 #f6d2f4 #000000 #c4f8c0 #000000 #f6d2f4 #f43619";
    final static String COLORS_4 = "? #f9f9cc #85AB81 #ffffff #000000 #e1817e #7BA1CC #27ea15 #E5DDA5 #000000 #427e29 #245B6A #ced1d6 #000000 #f6d2f4 #000000 #c4f8c0 #000000 #f6d2f4 #000000 #c4f8c0 #000000 #f6d2f4 #f43619";

    // color id(String array):
    final static int COLOR_NAME_0                  = 0;
    final static int COLOR_FIELD_LIGHT_1           = 1;
    final static int COLOR_FIELD_DARK_2            = 2;
    final static int COLOR_PIECE_WHITE_3           = 3;
    final static int COLOR_PIECE_BLACK_4           = 4;
    final static int COLOR_FIELD_FROM_5            = 5;
    final static int COLOR_FIELD_TO_6              = 6;
    final static int COLOR_LAST_MOVE_7             = 7;
    final static int COLOR_MOVES_BACKGROUND_8      = 8;
    final static int COLOR_MOVES_TEXT_9            = 9;
    final static int COLOR_MOVES_SELECTED_10       = 10;
    final static int COLOR_MOVES_ANOTATION_11      = 11;
    final static int COLOR_ENGINE_BACKGROUND_12    = 12;
    final static int COLOR_ENGINE_TEXT_13          = 13;
    final static int COLOR_INFO_BACKGROUND_14      = 14;
    final static int COLOR_INFO_TEXT_15            = 15;
    final static int COLOR_DATA_BACKGROUND_16      = 16;
    final static int COLOR_DATA_TEXT_17            = 17;
    final static int COLOR_TIME_BACKGROUND_18      = 18;
    final static int COLOR_TIME_TEXT_19            = 19;
    final static int COLOR_TIME2_BACKGROUND_20     = 20;
    final static int COLOR_TIME2_TEXT_21           = 21;
    final static int COLOR_HIGHLIGHTING_22         = 22;
    final static int COLOR_COORDINATES_23          = 23;

}
