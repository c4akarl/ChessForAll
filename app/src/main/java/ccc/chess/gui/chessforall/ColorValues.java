package ccc.chess.gui.chessforall;

import android.graphics.Color;

public class ColorValues
{
    ColorValues()
    {
        setColors(0, COLORS_0);
    }

    void setColors(int colorId, String colorValues)
    {
        colors = colorValues.split(" ");
        if (colors.length != COLOR_CNT)
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

    public int getColor(int colorId)
    {
        return Color.parseColor(colors[colorId]);
    }

    String[] colors = null;

    // color default values
    private final static String COLORS_0 = "? #f9f9cc #e2a156 #ffffff #000000 #E1698E #FF0023 #f43619 #efe395 #000000 #1BE91B #b2d9e4 #ced1d6 #000000 #f6d2f4 #000000 " +   // brown
            "#c4f8c0 #000000 #f6d2f4 #000000 #c4f8c0 #000000 #f6d2f4 #4412B8 #58A50E #E4682E #2196F3 #23B515 #F4D50B #F3501D #F40657";
    private final static String COLORS_1 = "? #F2E0F2 #9F829E #ffffff #000000 #4FAD52 #12EF0F #EC1855 #DBC39D #121201 #1BE91B #21accf #CCD6E7 #000000 #f6d2f4 #000000 " +   // violet
            "#c4f8c0 #000000 #f6d2f4 #000000 #c4f8c0 #000000 #EAA7E6 #4412B8 #58A50E #E4682E #2196F3 #23B515 #F4D50B #F3501D #F40657";
    private final static String COLORS_2 = "? #BDBDB2 #817D74 #ffffff #000000 #e1817e #ea2915 #f43619 #ABAB95 #100F01 #1BE91B #21accf #ced1d6 #000000 #f6d2f4 #000000 " +   // grey
            "#D5E9D4 #000000 #EBABE8 #000000 #c4f8c0 #000000 #D0AFCE #4412B8 #58A50E #E4682E #2196F3 #23B515 #F4D50B #F3501D #F40657";
    private final static String COLORS_3 = "? #EEEEEE #4C5B75 #F4F4F4 #0B0B0B #e1817e #FF230E #000000 #F0F0F0 #000000 #1BE91B #A0A0F0 #F0F0F0 #000000 #F0F0F0 #000000 " +   // blue
            "#F0F0F0 #000000 #F0F0F0 #000000 #F0F0F0 #000000 #F0B0F0 #4412B8 #58A50E #E4682E #2196F3 #23B515 #F4D50B #F3501D #F40657";
    private final static String COLORS_4 = "? #f9f9cc #85AB81 #ffffff #000000 #e1817e #ea2915 #f43619 #E5DDA5 #000000 #1BE91B #21accf #ced1d6 #000000 #f6d2f4 #000000 " +   // green
            "#c4f8c0 #000000 #f6d2f4 #000000 #c4f8c0 #000000 #f6d2f4 #4412B8 #58A50E #E4682E #2196F3 #23B515 #F4D50B #F3501D #F40657";

    final int COLOR_CNT = 31;
    // color id(COLORS_0...4):
    final static int COLOR_NAME_0                  = 0;     // Bezeichnung des Farbschemas
    final static int COLOR_FIELD_LIGHT_1           = 1;     // Schachbrett, helles Feld
    final static int COLOR_FIELD_DARK_2            = 2;     // Schachbrett, dunkles Feld
    final static int COLOR_PIECE_WHITE_3           = 3;     // Schachbrett, Farbe weisse Steine
    final static int COLOR_PIECE_BLACK_4           = 4;     // Schachbrett, Farbe schwarze Steine
    final static int COLOR_FIELD_FROM_5            = 5;     // Schachbrett, Selektierung von Feld
    final static int COLOR_FIELD_TO_6              = 6;     // Schachbrett, Selektierung bis Feld(er) ["Last Move" | "Possible Moves" ]
    final static int COLOR_COORDINATES_7           = 7;     // Schachbrett, Koordinatenanzeige
    final static int COLOR_MOVES_BACKGROUND_8      = 8;     // Zugliste(msgMoves), Hintergrund
    final static int COLOR_MOVES_TEXT_9            = 9;     // Zugliste(msgMoves), Text
    final static int COLOR_MOVES_SELECTED_10       = 10;    // Zugliste(msgMoves), selektierter Zug
    final static int COLOR_MOVES_ANOTATION_11      = 11;    // Zugliste(msgMoves), Anmerkung zu einen Zug
    final static int COLOR_ENGINE_BACKGROUND_12    = 12;    // Engine(msgEngine), Hintergrund
    final static int COLOR_ENGINE_TEXT_13          = 13;    // Engine(msgEngine), Text
    final static int COLOR_INFO_BACKGROUND_14      = 14;    // App-Info(letzte Zeile), Hintergrund
    final static int COLOR_INFO_TEXT_15            = 15;    // App-Info(letzte Zeile), Text
    final static int COLOR_DATA_BACKGROUND_16      = 16;    // Daten(Name, Elo), Hintergrund
    final static int COLOR_DATA_TEXT_17            = 17;    // Daten(Name, Elo), Text
    final static int COLOR_TIME_BACKGROUND_18      = 18;    // Zeit(Uhr läuft), Hintergrund
    final static int COLOR_TIME_TEXT_19            = 19;    // Zeit(Uhr läuft), Text
    final static int COLOR_TIME2_BACKGROUND_20     = 20;    // Zeit(Uhr angehalten), Hintergrund
    final static int COLOR_TIME2_TEXT_21           = 21;    // Zeit(Uhr angehalten), Text
    final static int COLOR_HIGHLIGHTING_22         = 22;    // hervorheben: Name, Elo (wenn Spieler am Zug); Zeit(die letzten 10 Sekunden)
    final static int COLOR_ARROWS1_23              = 23;    // Pfeile 1
    final static int COLOR_ARROWS2_24              = 24;    // Pfeile 2
    final static int COLOR_ARROWS3_25              = 25;    // Pfeile 3
    final static int COLOR_ARROWS4_26              = 26;    // Pfeile 4
    final static int COLOR_ARROWS5_27              = 27;    // Pfeile 5
    final static int COLOR_ARROWS6_28              = 28;    // Pfeile 6
    final static int COLOR_ARROWS7_29              = 29;    // Pfeile 7
    final static int COLOR_ARROWS8_30              = 30;    // Pfeile 8

}
