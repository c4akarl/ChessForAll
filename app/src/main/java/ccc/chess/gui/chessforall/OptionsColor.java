package ccc.chess.gui.chessforall;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class OptionsColor extends Activity implements margaritov.preference.colorpicker.ColorPickerDialog.OnColorChangedListener
{
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        u = new Util();
        userPrefs = getSharedPreferences("user", 0);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        u.updateFullscreenStatus(this, userPrefs.getBoolean("user_options_gui_StatusBar", true));
        setContentView(R.layout.optionscolor);

        colorId = getIntent().getExtras().getInt("colorId");
        title = (TextView) findViewById(R.id.title);
        eName = (EditText) findViewById(R.id.eName);
        eName.setHint(getColorHint(colorId));

        iFields1 = (TextView) findViewById(R.id.iFields1);
        iFields2 = (TextView) findViewById(R.id.iFields2);
        iPieces1 = (TextView) findViewById(R.id.iPieces1);
        iPieces2 = (TextView) findViewById(R.id.iPieces2);
        iFromTo1 = (TextView) findViewById(R.id.iFromTo1);
        iFromTo2 = (TextView) findViewById(R.id.iFromTo2);
        iMoveList1 = (TextView) findViewById(R.id.iMoveList1);
        iMoveList2 = (TextView) findViewById(R.id.iMoveList2);
        iMoveList3 = (TextView) findViewById(R.id.iMoveList3);
        iMoveList4 = (TextView) findViewById(R.id.iMoveList4);
        iEngineList1 = (TextView) findViewById(R.id.iEngineList1);
        iEngineList2 = (TextView) findViewById(R.id.iEngineList2);
        iInfo1 = (TextView) findViewById(R.id.iInfo1);
        iInfo2 = (TextView) findViewById(R.id.iInfo2);
        iData1 = (TextView) findViewById(R.id.iData1);
        iData2 = (TextView) findViewById(R.id.iData2);
        iTime1 = (TextView) findViewById(R.id.iTime1);
        iTime2 = (TextView) findViewById(R.id.iTime2);
        iTime3 = (TextView) findViewById(R.id.iTime3);
        iTime4 = (TextView) findViewById(R.id.iTime4);
        iHighlighting1 = (TextView) findViewById(R.id.iHighlighting1);
        iCoordinates1 = (TextView) findViewById(R.id.iCoordinates1);
        tReset = (TextView) findViewById(R.id.tReset);
        tReset.setFocusable(false);
        tApply = (TextView) findViewById(R.id.tApply);
        tApply.setFocusable(false);

        initColors(colorId);
        setUi(colorId);

    }

    public void myClickHandler(View view)
    {
        Intent returnIntent;
        switch (view.getId())
        {

            case R.id.iFields1: showDialog(null, cv.COLOR_FIELD_LIGHT_1, getString(R.string.colorFields) + "(1)"); currentView = iFields1; currentColor = cv.COLOR_FIELD_LIGHT_1; break;
            case R.id.iFields2: showDialog(null, cv.COLOR_FIELD_DARK_2, getString(R.string.colorFields) + "(2)"); currentView = iFields2; currentColor = cv.COLOR_FIELD_DARK_2; break;
            case R.id.iPieces1: showDialog(null, cv.COLOR_PIECE_WHITE_3, getString(R.string.colorPieces) + "(1)"); currentView = iPieces1; currentColor = cv.COLOR_PIECE_WHITE_3; break;
            case R.id.iPieces2: showDialog(null, cv.COLOR_PIECE_BLACK_4, getString(R.string.colorPieces) + "(2)"); currentView = iPieces2; currentColor = cv.COLOR_PIECE_BLACK_4; break;
            case R.id.iFromTo1: showDialog(null, cv.COLOR_FIELD_FROM_5, getString(R.string.colorFromTo) + "(1)"); currentView = iFromTo1; currentColor = cv.COLOR_FIELD_FROM_5; break;
            case R.id.iFromTo2: showDialog(null, cv.COLOR_FIELD_TO_6, getString(R.string.colorFromTo) + "(2)"); currentView = iFromTo2; currentColor = cv.COLOR_FIELD_TO_6; break;
            case R.id.iCoordinates1: showDialog(null, cv.COLOR_COORDINATES_7, getString(R.string.colorCoordinates)); currentView = iCoordinates1; currentColor = cv.COLOR_COORDINATES_7; break;
            case R.id.iMoveList1: showDialog(null, cv.COLOR_MOVES_BACKGROUND_8, getString(R.string.colorMoveList) + "(1)"); currentView = iMoveList1; currentColor = cv.COLOR_MOVES_BACKGROUND_8; break;
            case R.id.iMoveList2: showDialog(null, cv.COLOR_MOVES_TEXT_9, getString(R.string.colorMoveList) + "(2)"); currentView = iMoveList2; currentColor = cv.COLOR_MOVES_TEXT_9; break;
            case R.id.iMoveList3: showDialog(null, cv.COLOR_MOVES_SELECTED_10, getString(R.string.colorMoveList) + "(3)"); currentView = iMoveList3; currentColor = cv.COLOR_MOVES_SELECTED_10; break;
            case R.id.iMoveList4: showDialog(null, cv.COLOR_MOVES_ANOTATION_11, getString(R.string.colorMoveList) + "(4)"); currentView = iMoveList4; currentColor = cv.COLOR_MOVES_ANOTATION_11; break;
            case R.id.iEngineList1: showDialog(null, cv.COLOR_ENGINE_BACKGROUND_12, getString(R.string.colorEngineList) + "(1)"); currentView = iEngineList1; currentColor = cv.COLOR_ENGINE_BACKGROUND_12; break;
            case R.id.iEngineList2: showDialog(null, cv.COLOR_ENGINE_TEXT_13, getString(R.string.colorEngineList) + "(2)"); currentView = iEngineList2; currentColor = cv.COLOR_ENGINE_TEXT_13; break;
            case R.id.iInfo1: showDialog(null, cv.COLOR_INFO_BACKGROUND_14, getString(R.string.colorInfo) + "(1)"); currentView = iInfo1; currentColor = cv.COLOR_INFO_BACKGROUND_14; break;
            case R.id.iInfo2: showDialog(null, cv.COLOR_INFO_TEXT_15, getString(R.string.colorInfo) + "(2)"); currentView = iInfo2; currentColor = cv.COLOR_INFO_TEXT_15; break;
            case R.id.iData1: showDialog(null, cv.COLOR_DATA_BACKGROUND_16, getString(R.string.colorData) + "(1)"); currentView = iData1; currentColor = cv.COLOR_DATA_BACKGROUND_16; break;
            case R.id.iData2: showDialog(null, cv.COLOR_DATA_TEXT_17, getString(R.string.colorData) + "(2)"); currentView = iData2; currentColor = cv.COLOR_DATA_TEXT_17; break;
            case R.id.iTime1: showDialog(null, cv.COLOR_TIME_BACKGROUND_18, getString(R.string.colorTime) + "(1)"); currentView = iTime1; currentColor = cv.COLOR_TIME_BACKGROUND_18; break;
            case R.id.iTime2: showDialog(null, cv.COLOR_TIME_TEXT_19, getString(R.string.colorTime) + "(2)"); currentView = iTime2; currentColor = cv.COLOR_TIME_TEXT_19; break;
            case R.id.iTime3: showDialog(null, cv.COLOR_TIME2_BACKGROUND_20, getString(R.string.colorTime) + "(3)"); currentView = iTime3; currentColor = cv.COLOR_TIME2_BACKGROUND_20; break;
            case R.id.iTime4: showDialog(null, cv.COLOR_TIME2_TEXT_21, getString(R.string.colorTime) + "(4)"); currentView = iTime4; currentColor = cv.COLOR_TIME2_TEXT_21; break;
            case R.id.iHighlighting1: showDialog(null, cv.COLOR_HIGHLIGHTING_22, getString(R.string.colorHighlighting)); currentView = iHighlighting1; currentColor = cv.COLOR_HIGHLIGHTING_22; break;

            case R.id.btnMenu:
                removeDialog(MENU_COLOR_SETTINGS);
                showDialog(MENU_COLOR_SETTINGS);
                break;

            case R.id.tReset:
                resetPrefs(colorId);
                initColors(colorId);
                setUi(colorId);
                break;

            case R.id.tApply:
                setPrefs(colorId);
                returnIntent = new Intent();
                setResult(RESULT_OK, returnIntent);
                finish();
                break;

        }
    }

    protected void showDialog(Bundle state, int color, String info)
    {
        dialog = new margaritov.preference.colorpicker.ColorPickerDialog(this, cv.getColor(color), info);
        dialog.setOnColorChangedListener(this);
        if (alphaSliderEnabled)
            dialog.setAlphaSliderVisible(true);
        if (state != null)
            dialog.onRestoreInstanceState(state);
        dialog.show();
    }

    @Override
    protected Dialog onCreateDialog(int id)
    {
        if (id == MENU_COLOR_SETTINGS)
        {
            final int MENU_COLOR_SETTINGS_STANDARD    	= 0;
            final int MENU_COLOR_SETTINGS_BROWN 		= 1;
            final int MENU_COLOR_SETTINGS_GREY    		= 2;
            final int MENU_COLOR_SETTINGS_BLUE   		= 3;
            final int MENU_COLOR_SETTINGS_GREEN    		= 4;
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice);
            List<Integer> actions = new ArrayList<Integer>();
            arrayAdapter.add(getColorName(MENU_COLOR_SETTINGS_STANDARD)); 		actions.add(MENU_COLOR_SETTINGS_STANDARD);
            arrayAdapter.add(getColorName(MENU_COLOR_SETTINGS_BROWN)); 			actions.add(MENU_COLOR_SETTINGS_BROWN);
            arrayAdapter.add(getColorName(MENU_COLOR_SETTINGS_GREY)); 			actions.add(MENU_COLOR_SETTINGS_GREY);
            arrayAdapter.add(getColorName(MENU_COLOR_SETTINGS_BLUE)); 			actions.add(MENU_COLOR_SETTINGS_BLUE);
            arrayAdapter.add(getColorName(MENU_COLOR_SETTINGS_GREEN)); 			actions.add(MENU_COLOR_SETTINGS_GREEN);
            final List<Integer> finalActions = actions;
            AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
            builder.setCancelable(true);
            TextView tv = new TextView(getApplicationContext());
            tv.setText(R.string.menu_colorsettings);
            tv.setTextAppearance(this, R.style.c4aDialogTitle);
            tv.setGravity(Gravity.CENTER_HORIZONTAL);
            builder.setCustomTitle(tv );
            builder.setSingleChoiceItems(arrayAdapter, userPrefs.getInt("colorId", 0), new DialogInterface.OnClickListener()
            {
                public void onClick(DialogInterface dialog, int item)
                {
                    removeDialog(MENU_COLOR_SETTINGS);
                    colorId = item;
                    initColors(colorId);
                    setUi(colorId);
                }
            });
            AlertDialog alert = builder.create();
            return alert;
        }
        return null;
    }

    public String getColorName(int colorId)
    {
        String colorName = "";
        switch (colorId)
        {
            case 0:
                colorName = getString(R.string.menu_colorsettings_standard);
                if (!userPrefs.getString("colors_0", "").equals(""))
                {
                    String[] split = userPrefs.getString("colors_0", "").split(" ");
                    if (!split[0].equals("?"))
                        colorName = split[0];
                }
                break;
            case 1:
                colorName = getString(R.string.menu_colorsettings_brown);
                if (!userPrefs.getString("colors_1", "").equals(""))
                {
                    String[] split = userPrefs.getString("colors_1", "").split(" ");
                    if (!split[0].equals("?"))
                        colorName = split[0];
                }
                break;
            case 2:
                colorName = getString(R.string.menu_colorsettings_grey);
                if (!userPrefs.getString("colors_2", "").equals(""))
                {
                    String[] split = userPrefs.getString("colors_2", "").split(" ");
                    if (!split[0].equals("?"))
                        colorName = split[0];
                }
                break;
            case 3:
                colorName = getString(R.string.menu_colorsettings_blue);
                if (!userPrefs.getString("colors_3", "").equals(""))
                {
                    String[] split = userPrefs.getString("colors_3", "").split(" ");
                    if (!split[0].equals("?"))
                        colorName = split[0];
                }
                break;
            case 4:
                colorName = getString(R.string.menu_colorsettings_green);
                if (!userPrefs.getString("colors_4", "").equals(""))
                {
                    String[] split = userPrefs.getString("colors_4", "").split(" ");
                    if (!split[0].equals("?"))
                        colorName = split[0];
                }
                break;
        }
        return colorName;
    }

    @Override
    public void onColorChanged(int intColor)
    {
        String hexColor = String.format("#%06X", (0xFFFFFF & intColor));
        cv.colors[currentColor] = hexColor;
        setTextViewColors(currentView, currentColor);
        setPrefs(colorId);
    }

    protected void setPrefs(int colorId)
    {
        String colors = "";
        String name = "";
        if (eName.length() != 0)
            name = eName.getText().toString();
        else
            name = "?";
        if (!name.equals(""))
            cv.colors[0] = name;
        for (int i = 0; i < cv.colors.length; i++)
        {
            if (i == 0 & name.equals("?"))
            {
                switch (colorId)
                {
                    case 0:
                        cv.colors[i] = getString(R.string.menu_colorsettings_brown);
                        break;
                    case 1:
                        cv.colors[i] = getString(R.string.menu_colorsettings_violet);
                        break;
                    case 2:
                        cv.colors[i] = getString(R.string.menu_colorsettings_grey);
                        break;
                    case 3:
                        cv.colors[i] = getString(R.string.menu_colorsettings_blue);
                        break;
                    case 4:
                        cv.colors[i] = getString(R.string.menu_colorsettings_green);
                        break;
                }
            }
            colors = colors + cv.colors[i] + " ";
        }

//karl: zum Kopieren der Farben von Logcat ---> ColorValues.COLORS_0...4
//Log.i(TAG, "colors: " + colors);

        SharedPreferences.Editor ed = userPrefs.edit();
        ed.putInt("colorId", colorId);
        switch (colorId)
        {
            case 0:
                ed.putString("colors_0", colors);
                break;
            case 1:
                ed.putString("colors_1", colors);
                break;
            case 2:
                ed.putString("colors_2", colors);
                break;
            case 3:
                ed.putString("colors_3", colors);
                break;
            case 4:
                ed.putString("colors_4", colors);
                break;
        }

        ed.commit();
    }

    public void initColors(int colorId)
    {
        cv = new ColorValues();
        switch (colorId)
        {
            case 0:
                cv.setColors(colorId, userPrefs.getString("colors_0", ""));
                break;
            case 1:
                cv.setColors(colorId, userPrefs.getString("colors_1", ""));
                break;
            case 2:
                cv.setColors(colorId, userPrefs.getString("colors_2", ""));
                break;
            case 3:
                cv.setColors(colorId, userPrefs.getString("colors_3", ""));
                break;
            case 4:
                cv.setColors(colorId, userPrefs.getString("colors_4", ""));
                break;
        }
    }

    protected void resetPrefs(int colorId)
    {
        SharedPreferences.Editor ed = userPrefs.edit();
        switch (colorId)
        {
            case 0:
                ed.putString("colors_0", "");
                break;
            case 1:
                ed.putString("colors_1", "");
                break;
            case 2:
                ed.putString("colors_2", "");
                break;
            case 3:
                ed.putString("colors_3", "");
                break;
            case 4:
                ed.putString("colors_4", "");
                break;
        }
        ed.commit();
    }

    protected void setUi(int colorId)
    {
        if (cv.colors[cv.COLOR_NAME_0].equals("?"))
        {
            eName.setHint(getColorHint(colorId));
            eName.setText("");
        }
        else
            eName.setText(cv.colors[cv.COLOR_NAME_0]);

        setTextViewColors(iFields1, cv.COLOR_FIELD_LIGHT_1);
        setTextViewColors(iFields2, cv.COLOR_FIELD_DARK_2);
        setTextViewColors(iPieces1, cv.COLOR_PIECE_WHITE_3);
        setTextViewColors(iPieces2, cv.COLOR_PIECE_BLACK_4);
        setTextViewColors(iFromTo1, cv.COLOR_FIELD_FROM_5);
        setTextViewColors(iFromTo2, cv.COLOR_FIELD_TO_6);
        setTextViewColors(iCoordinates1, cv.COLOR_COORDINATES_7);
        setTextViewColors(iMoveList1, cv.COLOR_MOVES_BACKGROUND_8);
        setTextViewColors(iMoveList2, cv.COLOR_MOVES_TEXT_9);
        setTextViewColors(iMoveList3, cv.COLOR_MOVES_SELECTED_10);
        setTextViewColors(iMoveList4, cv.COLOR_MOVES_ANOTATION_11);
        setTextViewColors(iEngineList1, cv.COLOR_ENGINE_BACKGROUND_12);
        setTextViewColors(iEngineList2, cv.COLOR_ENGINE_TEXT_13);
        setTextViewColors(iInfo1, cv.COLOR_INFO_BACKGROUND_14);
        setTextViewColors(iInfo2, cv.COLOR_INFO_TEXT_15);
        setTextViewColors(iData1, cv.COLOR_DATA_BACKGROUND_16);
        setTextViewColors(iData2, cv.COLOR_DATA_TEXT_17);
        setTextViewColors(iTime1, cv.COLOR_TIME_BACKGROUND_18);
        setTextViewColors(iTime2, cv.COLOR_TIME_TEXT_19);
        setTextViewColors(iTime3, cv.COLOR_TIME2_BACKGROUND_20);
        setTextViewColors(iTime4, cv.COLOR_TIME2_TEXT_21);
        setTextViewColors(iHighlighting1, cv.COLOR_HIGHLIGHTING_22);

    }

    protected String getColorHint(int colorId)
    {
        switch (colorId)
        {
            case 0: return getString(R.string.menu_colorsettings_standard);
            case 1: return getString(R.string.menu_colorsettings_brown);
            case 2: return getString(R.string.menu_colorsettings_grey);
            case 3: return getString(R.string.menu_colorsettings_blue);
            case 4: return getString(R.string.menu_colorsettings_green);
        }
        return "";
    }

    public void setTextViewColors(TextView tv, int color)
    {
        GradientDrawable tvBackground = (GradientDrawable) tv.getBackground();
        tvBackground.setColor(cv.getColor(color));
    }

    final String TAG = "OptionsColor";
    Util u;
    SharedPreferences userPrefs;

    EditText eName = null;
    TextView iFields1 = null;
    TextView iFields2 = null;
    TextView iPieces1 = null;
    TextView iPieces2 = null;
    TextView iFromTo1 = null;
    TextView iFromTo2 = null;
    TextView iMoveList1 = null;
    TextView iMoveList2 = null;
    TextView iMoveList3 = null;
    TextView iMoveList4 = null;
    TextView iEngineList1 = null;
    TextView iEngineList2 = null;
    TextView iInfo1 = null;
    TextView iInfo2 = null;
    TextView iData1 = null;
    TextView iData2 = null;
    TextView iTime1 = null;
    TextView iTime2 = null;
    TextView iTime3 = null;
    TextView iTime4 = null;
    TextView iHighlighting1 = null;
    TextView iCoordinates1 = null;

    TextView tReset = null;
    TextView tApply = null;

    int colorId;

    TextView currentView = null;
    int currentColor = 0;
    final static int MENU_COLOR_SETTINGS = 732;

    ColorValues cv;

    margaritov.preference.colorpicker.ColorPickerDialog dialog;
    private boolean alphaSliderEnabled = false;

    TextView title;

}
