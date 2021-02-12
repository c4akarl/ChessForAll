package ccc.chess.gui.chessforall;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import ccc.chess.logic.c4aservice.ChessHistory;

public class Settings  extends Activity implements Ic4aDialogCallback
{
    public void onCreate(Bundle savedInstanceState)
    {

//		Log.i(TAG, "onCreate(), variants: " + variants);

        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        u = new Util();
        fileIO = new FileIO(this);
        userPrefs = getSharedPreferences("user", 0);
    }

    @Override
    protected void onResume()
    {

//		Log.i(TAG, "onResume(), variants: " + variants);

        super.onResume();
        start();

    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig)
    {

//		Log.i(TAG, "onConfigurationChanged(), variants: " + variants);

        super.onConfigurationChanged(newConfig);
        setPrefs();
    }

    protected void start() {
        u.updateFullscreenStatus(this, userPrefs.getBoolean("user_options_gui_StatusBar", false));
        setContentView(R.layout.settings);
        optionsColorIntent = new Intent(this, OptionsColor.class);
        optionsTimeControlIntent = new Intent(this, OptionsTimeControl.class);
        fileManagerIntent = new Intent(this, FileManager.class);

        // Engine Settings
        engineMessage = findViewById(R.id.cbEpEngineMessage);
        ponder = findViewById(R.id.cbEpPonder);
        randomFirstMove = findViewById(R.id.cbEpRandomFirstMove);
        autoStartEngine =  findViewById(R.id.cbEpAutoStartEngine);
        debugInformation =  findViewById(R.id.debugInformation);
        logOn =  findViewById(R.id.logOn);

        // Appearance
        btn_color_settings =  findViewById(R.id.btn_color_settings);
        u.setTextViewColors(btn_color_settings, "#BAB8B8");
        cbGuCoordinates =  findViewById(R.id.cbGuCoordinates);
        cbGuPosibleMoves =  findViewById(R.id.cbGuPosibleMoves);
        cbGuQuickMove =  findViewById(R.id.cbGuQuickMove);
        cbGuStatusBar =  findViewById(R.id.cbGuStatusBar);
        cbGuDisableScreenTimeout =  findViewById(R.id.cbGuDisableScreenTimeout);
        cbGuFlipBoard =  findViewById(R.id.cbGuFlipBoard);
        cbGuGameNavigationBoard =  findViewById(R.id.cbGuGameNavigationBoard);
        cbGuBlindMode =  findViewById(R.id.cbGuBlindMode);

        // Time, Clock
        btn_time_settings =  findViewById(R.id.btn_time_settings);
        u.setTextViewColors(btn_time_settings, "#BAB8B8");

        // Opening Book
        openingBook =  findViewById(R.id.cbEpOpeningBook);
        showBookHints =  findViewById(R.id.cbEpShowBookHints);
        epBook =  findViewById(R.id.epBook);
        bookName =  findViewById(R.id.tvEpBookName);
        bookName.setHint(R.string.epBookHint);

        // Other
        etGuPlayerName =  findViewById(R.id.etGuPlayerName);
        cbGuUsePgnDatabase =  findViewById(R.id.cbGuUsePgnDatabase);
        cbGuLastPosition =  findViewById(R.id.cbGuLastPosition);
        cbGuEnableSounds =  findViewById(R.id.cbGuEnableSounds);

        getPrefs();

        variantsMinus = findViewById(R.id.variantsMinus);
        variantsMinus.setOnClickListener(v -> {
            variants--;
            setVariants();
        });
        variantsValue = findViewById(R.id.variantsValue);
        u.setTextViewColors(variantsValue, "#efe395");
        variantsValue.setOnClickListener(v -> {
            variants = VARIANTS_DEFAULT;
            setVariants();
        });
        variantsValue.setText(String.format("%d", variants));
        variantsPlus = findViewById(R.id.variantsPlus);
        variantsPlus.setOnClickListener(v -> {
            variants++;
            setVariants();
        });
        setVariants();

        movesMinus = findViewById(R.id.movesMinus);
        movesMinus.setOnClickListener(v -> {
            moves--;
            setMoves();
        });
        movesValue = findViewById(R.id.movesValue);
        u.setTextViewColors(movesValue, "#efe395");
        movesValue.setOnClickListener(v -> {
            moves = MOVES_DEFAULT;
            setMoves();
        });
        movesValue.setText(String.format("%d", moves));
        movesPlus = findViewById(R.id.movesPlus);
        movesPlus.setOnClickListener(v -> {
            moves++;
            setMoves();
        });
        setMoves();

        linesMinus = findViewById(R.id.linesMinus);
        linesMinus.setOnClickListener(v -> {
            lines--;
            setLines();
        });
        linesValue = findViewById(R.id.linesValue);
        u.setTextViewColors(linesValue, "#efe395");
        linesValue.setOnClickListener(v -> {
            lines = LINES_DEFAULT;
            setLines();
        });
        linesValue.setText(String.format("%d", lines));
        linesPlus = findViewById(R.id.linesPlus);
        linesPlus.setOnClickListener(v -> {
            lines++;
            setLines();
        });
        setLines();

        arrowsMinus = findViewById(R.id.arrowsMinus);
        arrowsMinus.setOnClickListener(v -> {
            arrows--;
            setArrows();
        });
        arrowsValue = findViewById(R.id.arrowsValue);
        arrowsValue.setText(String.format("%d", arrows));
        arrowsValue.setOnClickListener(v -> {
            arrows = ARROWS_DEFAULT;
            setArrows();
        });
        arrowsPlus = findViewById(R.id.arrowsPlus);
        arrowsPlus.setOnClickListener(v -> {
            arrows++;
            setArrows();
        });
        setArrows();

        piecesMinus =  findViewById(R.id.piecesMinus);
        piecesMinus.setOnClickListener(v -> {
            pieceNameId--;
            setPieces();
        });
        piecesValue = findViewById(R.id.piecesValue);
        piecesValue.setOnClickListener(v -> {
            pieceNameId = PIECES_DEFAULT;
            setPieces();
        });
        piecesPlus = findViewById(R.id.piecesPlus);
        piecesPlus.setOnClickListener(v -> {
            pieceNameId++;
            setPieces();
        });
        setPieces();

        textsizeMinus =  findViewById(R.id.textsizeMinus);
        textsizeMinus.setOnClickListener(v -> {
            textsizeId--;
            setTextsize();
        });
        textsizeValue = findViewById(R.id.textsizeValue);
        textsizeValue.setOnClickListener(v -> {
            textsizeId = TEXTSIZE_DEFAULT;
            setTextsize();
        });
        textsizePlus = findViewById(R.id.textsizePlus);
        textsizePlus.setOnClickListener(v -> {
            textsizeId++;
            setTextsize();
        });
        textsizePixel = textsizeValue.getTextSize();
        setTextsize();

        btnSettingsCancel = findViewById(R.id.btnSettingsCancel);
        u.setTextViewColors(btnSettingsCancel, "#BAB8B8");
        btnSettingsOk = findViewById(R.id.btnSettingsOk);
        u.setTextViewColors(btnSettingsOk, "#BAB8B8");

    }

    public void setVariants()
    {
        u.setTextViewColors(variantsValue, "#efe395");
        u.setTextViewColors(variantsMinus, "#f6d2f4");
        u.setTextViewColors(variantsPlus, "#c4f8c0");
        if (variants <= VARIANTS_MIN) {
            variants = VARIANTS_MIN;
            u.setTextViewColors(variantsMinus, "#767a76");
        }
        if (variants >= VARIANTS_MAX) {
            variants = VARIANTS_MAX;
            u.setTextViewColors(variantsPlus, "#767a76");
        }
        variantsValue.setText(String.format("%d", variants));
    }

    public void setMoves()
    {
        u.setTextViewColors(movesValue, "#efe395");
        u.setTextViewColors(movesMinus, "#f6d2f4");
        u.setTextViewColors(movesPlus, "#c4f8c0");
        if (moves <= MOVES_MIN) {
            moves = MOVES_MIN;
            u.setTextViewColors(movesMinus, "#767a76");
        }
        if (moves >= MOVES_MAX) {
            moves = MOVES_MAX;
            u.setTextViewColors(movesPlus, "#767a76");
        }
        movesValue.setText(String.format("%d", moves));
    }

    public void setLines()
    {
        u.setTextViewColors(linesValue, "#efe395");
        u.setTextViewColors(linesMinus, "#f6d2f4");
        u.setTextViewColors(linesPlus, "#c4f8c0");
        if (lines <= LINES_MIN) {
            lines = LINES_MIN;
            u.setTextViewColors(linesMinus, "#767a76");
        }
        if (lines >= LINES_MAX) {
            lines = LINES_MAX;
            u.setTextViewColors(linesPlus, "#767a76");
        }
        linesValue.setText(String.format("%d", lines));
    }

    public void setArrows()
    {
        u.setTextViewColors(arrowsMinus, "#f6d2f4");
        u.setTextViewColors(arrowsPlus, "#c4f8c0");
        if (arrows <= ARROWS_MIN) {
            arrows = ARROWS_MIN;
            u.setTextViewColors(arrowsMinus, "#767a76");
        }
        if (arrows >= ARROWS_MAX) {
            arrows = ARROWS_MAX;
            u.setTextViewColors(arrowsPlus, "#767a76");
        }
        arrowsValue.setText(String.format("%d", arrows));
        u.setTextViewColors(arrowsValue, "#efe395");
    }

    public void setPieces()
    {
        u.setTextViewColors(piecesMinus, "#f6d2f4");
        u.setTextViewColors(piecesPlus, "#c4f8c0");
        if (pieceNameId <= PIECES_MIN) {
            pieceNameId = PIECES_MIN;
            u.setTextViewColors(piecesMinus, "#767a76");
        }
        if (pieceNameId >= PIECES_MAX) {
            pieceNameId = PIECES_MAX;
            u.setTextViewColors(piecesPlus, "#767a76");
        }
        piecesValue.setText(getPieceNames(pieceNameId));
        u.setTextViewColors(piecesValue, "#efe395");
    }

    public void setTextsize()
    {
        u.setTextViewColors(textsizeMinus, "#f6d2f4");
        u.setTextViewColors(textsizePlus, "#c4f8c0");
        if (textsizeId <= TEXTSIZE_MIN) {
            textsizeId = TEXTSIZE_MIN;
            u.setTextViewColors(textsizeMinus, "#767a76");
        }
        if (textsizeId >= TEXTSIZE_MAX) {
            textsizeId = TEXTSIZE_MAX;
            u.setTextViewColors(textsizePlus, "#767a76");
        }
        float ts = textsizePixel;
        switch (textsizeId)
        {
            case 1:
                fontSize = FONTSIZE_SMALL;
                ts = ts * 0.55F;
                break;
            case 2:
            default:
                fontSize = FONTSIZE_MEDIUM;
                ts = ts * 0.7F;
                break;
            case 3:
                fontSize = FONTSIZE_LARGE;
                ts = ts * 0.85F;
                break;
            case 4:
                fontSize = FONTSIZE_LARGEST;
                break;
        }
        textsizeValue.setText(getTextsizeNames(textsizeId));
        u.setTextViewColors(textsizeValue, "#efe395");
        textsizeMinus.setTextSize(TypedValue.COMPLEX_UNIT_PX, ts);
        textsizeValue.setTextSize(TypedValue.COMPLEX_UNIT_PX, ts);
        textsizePlus.setTextSize(TypedValue.COMPLEX_UNIT_PX, ts);
    }

    public String getPieceNames(int pieceNameId)
    {
        switch (pieceNameId)
        {
            case 1:		// local piece symbol
                return 	getString(R.string.piece_K) +
                        getString(R.string.piece_Q) +
                        getString(R.string.piece_R) +
                        getString(R.string.piece_B) +
                        getString(R.string.piece_N);
            case 2:		// figurin piece symbol
                return "" + ChessHistory.HEX_K + ChessHistory.HEX_Q + ChessHistory.HEX_R + ChessHistory.HEX_B + ChessHistory.HEX_N;
            default:	// english piece symbol
                return "KQRBN";
        }
    }

    public String getTextsizeNames(int textsizeId)
    {
        switch (textsizeId)
        {
            case 1:	            return	getString(R.string.small);
            case 2:	default:    return	getString(R.string.medium);
            case 3:	            return	getString(R.string.large);
            case 4:	            return	getString(R.string.larger);
        }
    }

//    @SuppressWarnings("deprecation")
    public void myClickHandler(View view)
    {	// ClickHandler	(ButtonEvents)
        int id = view.getId();

//        Log.i(TAG, "myClickHandler(), id: " + id);

        if (id == R.id.btnSettingsOk) {
            setPrefs();
            Intent returnIntent;
            returnIntent = new Intent();
            setResult(3, returnIntent);
            finish();
        }
        if (id == R.id.btnSettingsCancel) {
            finish();
        }
        if (id == R.id.epBook || id == R.id.tvEpBookName) {
            if (fileIO.isSdk30()) {
                removeDialog(NO_FILE_ACTIONS_DIALOG);
                showDialog(NO_FILE_ACTIONS_DIALOG);
                return;
            }
            fileManagerIntent.putExtra("fileActionCode", LOAD_OPENING_BOOK_REQUEST_CODE);
            fileManagerIntent.putExtra("displayActivity", 1);
            this.startActivityForResult(fileManagerIntent, LOAD_OPENING_BOOK_REQUEST_CODE);
        }
        if (id == R.id.btn_color_settings) {
            removeDialog(MENU_COLOR_SETTINGS);
            showDialog(MENU_COLOR_SETTINGS);
        }
        if (id == R.id.btn_time_settings) {
            startActivityForResult(optionsTimeControlIntent, OPTIONS_TIME_CONTROL_REQUEST_CODE);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected Dialog onCreateDialog(int id)
    {
        if (id == MENU_COLOR_SETTINGS)
        {
            final int MENU_COLOR_SETTINGS_BROWN    		= 0;
            final int MENU_COLOR_SETTINGS_VIOLET 		= 1;
            final int MENU_COLOR_SETTINGS_GREY    		= 2;
            final int MENU_COLOR_SETTINGS_BLUE   		= 3;
            final int MENU_COLOR_SETTINGS_GREEN    		= 4;
            ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_single_choice);
            List<Integer> actions = new ArrayList<>();
            arrayAdapter.add(getColorName(MENU_COLOR_SETTINGS_BROWN)); 			actions.add(MENU_COLOR_SETTINGS_BROWN);
            arrayAdapter.add(getColorName(MENU_COLOR_SETTINGS_VIOLET)); 		actions.add(MENU_COLOR_SETTINGS_VIOLET);
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
            builder.setSingleChoiceItems(arrayAdapter, userPrefs.getInt("colorId", 0), (dialog, item) -> {
                SharedPreferences.Editor ed = userPrefs.edit();
                ed.putInt("colorId", finalActions.get(item));
                ed.apply();
                startActivityForResult(optionsColorIntent, OPTIONS_COLOR_SETTINGS);
                removeDialog(MENU_COLOR_SETTINGS);
            });
            return builder.create();
        }
        if (id == NO_FILE_ACTIONS_DIALOG)
        {
            c4aDialog = new C4aDialog(this, this, getString(R.string.dgTitleDialog),
                    "", getString(R.string.btn_Ok), "", getString(R.string.noFileActions), 0, "");
            return c4aDialog;
        }

        return null;

    }

    @Override
    public void getCallbackValue(int btnValue)
    {

    }

    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == LOAD_OPENING_BOOK_REQUEST_CODE && resultCode == RESULT_OK) {
            if (!Objects.requireNonNull(data.getStringExtra("fileName")).endsWith(".bin"))
                bookName.setText("");
            else {
                String fp = data.getStringExtra("filePath") + data.getStringExtra("fileName");
                bookName.setText(fp);
            }
            bookName.setSelection(bookName.getText().length());
            setPrefs();
        }
    }

    public String getColorName(int colorId)
    {
        String colorName = "";
        switch (colorId)
        {
            case 0:
                colorName = getString(R.string.menu_colorsettings_brown);
                if (!userPrefs.getString("colors_0", "").equals(""))
                {
                    String[] split = userPrefs.getString("colors_0", "").split(" ");
                    if (!split[0].equals("?"))
                        colorName = split[0];
                }
                break;
            case 1:
                colorName = getString(R.string.menu_colorsettings_violet);
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

    protected void setPrefs()
    {
        SharedPreferences.Editor ed = userPrefs.edit();
        // Engine Settings
        ed.putInt("user_options_enginePlay_MultiPv", variants);
        ed.putInt("user_options_enginePlay_PvMoves", moves);
        ed.putInt("user_options_enginePlay_displayedLines", lines);
        ed.putInt("user_options_gui_arrows", arrows);
        ed.putBoolean("user_options_enginePlay_EngineMessage", engineMessage.isChecked());
        ed.putBoolean("user_options_enginePlay_Ponder", ponder.isChecked());
        ed.putBoolean("user_options_enginePlay_RandomFirstMove", randomFirstMove.isChecked());
        ed.putBoolean("user_options_enginePlay_AutoStartEngine", autoStartEngine.isChecked());
        ed.putBoolean("user_options_enginePlay_debugInformation", debugInformation.isChecked());
        ed.putBoolean("user_options_enginePlay_logOn", logOn.isChecked());

        // Appearance
        ed.putInt("user_options_gui_PieceNameId", pieceNameId);
        ed.putInt("user_options_gui_textsizeId", textsizeId);
        ed.putInt("user_options_gui_fontSize", fontSize);
        ed.putBoolean("user_options_gui_Coordinates", cbGuCoordinates.isChecked());
        ed.putBoolean("user_options_gui_posibleMoves", cbGuPosibleMoves.isChecked());
        ed.putBoolean("user_options_gui_quickMove", cbGuQuickMove.isChecked());
        ed.putBoolean("user_options_gui_StatusBar", cbGuStatusBar.isChecked());
        ed.putBoolean("user_options_gui_disableScreenTimeout", cbGuDisableScreenTimeout.isChecked());
        ed.putBoolean("user_options_gui_FlipBoard", cbGuFlipBoard.isChecked());
        ed.putBoolean("user_options_gui_gameNavigationBoard", cbGuGameNavigationBoard.isChecked());
        ed.putBoolean("user_options_gui_BlindMode", cbGuBlindMode.isChecked());

        // Opening Book
        ed.putBoolean("user_options_enginePlay_OpeningBook", openingBook.isChecked());
        ed.putBoolean("user_options_enginePlay_ShowBookHints", showBookHints.isChecked());
        ed.putString("user_options_enginePlay_OpeningBookName", bookName.getText().toString());

        // Other
        if (!etGuPlayerName.getText().toString().equals(""))
            ed.putString("user_options_gui_playerName", etGuPlayerName.getText().toString());
        else
            ed.putString("user_options_gui_playerName", getString(R.string.qPlayer));
        ed.putBoolean("user_options_gui_usePgnDatabase", cbGuUsePgnDatabase.isChecked());
        ed.putBoolean("user_options_gui_LastPosition", cbGuLastPosition.isChecked());
        ed.putBoolean("user_options_gui_enableSounds", cbGuEnableSounds.isChecked());

        ed.apply();
    }

    protected void getPrefs()
    {

        // Engine Settings
        variants = userPrefs.getInt("user_options_enginePlay_MultiPv", variants);
        moves = userPrefs.getInt("user_options_enginePlay_PvMoves", moves);
        lines = userPrefs.getInt("user_options_enginePlay_displayedLines", lines);
        arrows = userPrefs.getInt("user_options_gui_arrows", arrows);
        engineMessage.setChecked(userPrefs.getBoolean("user_options_enginePlay_EngineMessage", true));
        ponder.setChecked(userPrefs.getBoolean("user_options_enginePlay_Ponder", false));
        randomFirstMove.setChecked(userPrefs.getBoolean("user_options_enginePlay_RandomFirstMove", false));
        autoStartEngine.setChecked(userPrefs.getBoolean("user_options_enginePlay_AutoStartEngine", true));
        debugInformation.setChecked(userPrefs.getBoolean("user_options_enginePlay_debugInformation", false));
        logOn.setChecked(userPrefs.getBoolean("user_options_enginePlay_logOn", false));

        // Appearance
        pieceNameId = userPrefs.getInt("user_options_gui_PieceNameId", pieceNameId);
        textsizeId = userPrefs.getInt("user_options_gui_textsizeId", textsizeId);
        fontSize = userPrefs.getInt("user_options_gui_fontSize", fontSize);
        cbGuCoordinates.setChecked(userPrefs.getBoolean("user_options_gui_Coordinates", false));
        cbGuPosibleMoves.setChecked(userPrefs.getBoolean("user_options_gui_posibleMoves", true));
        cbGuQuickMove.setChecked(userPrefs.getBoolean("user_options_gui_quickMove", true));
        cbGuStatusBar.setChecked(userPrefs.getBoolean("user_options_gui_StatusBar", false));
        cbGuDisableScreenTimeout.setChecked(userPrefs.getBoolean("user_options_gui_disableScreenTimeout", false));
        cbGuFlipBoard.setChecked(userPrefs.getBoolean("user_options_gui_FlipBoard", false));
        cbGuGameNavigationBoard.setChecked(userPrefs.getBoolean("user_options_gui_gameNavigationBoard", false));
        cbGuBlindMode.setChecked(userPrefs.getBoolean("user_options_gui_BlindMode", false));

        // Opening Book
        openingBook.setChecked(userPrefs.getBoolean("user_options_enginePlay_OpeningBook", true));
        showBookHints.setChecked(userPrefs.getBoolean("user_options_enginePlay_ShowBookHints", true));
        bookName.setText(userPrefs.getString("user_options_enginePlay_OpeningBookName", ""));
        bookName.setSelection(bookName.getText().length());

        // Other
        etGuPlayerName.setText(userPrefs.getString("user_options_gui_playerName", getString(R.string.qPlayer)));
        cbGuUsePgnDatabase.setChecked(userPrefs.getBoolean("user_options_gui_usePgnDatabase", true));
        cbGuLastPosition.setChecked(userPrefs.getBoolean("user_options_gui_LastPosition", false));
        cbGuEnableSounds.setChecked(userPrefs.getBoolean("user_options_gui_enableSounds", true));

    }

//    final String TAG = "Settings";
    Util u;
    FileIO fileIO;
    final static int LOAD_OPENING_BOOK_REQUEST_CODE = 91;
    final static int NO_FILE_ACTIONS_DIALOG = 193;
    final static int MENU_COLOR_SETTINGS = 732;
    final static int OPTIONS_COLOR_SETTINGS = 22;
    final static int OPTIONS_TIME_CONTROL_REQUEST_CODE = 18;

    Intent optionsColorIntent;
    Intent optionsTimeControlIntent;

    Intent fileManagerIntent;
    SharedPreferences userPrefs;
    C4aDialog c4aDialog;

    final static int VARIANTS_DEFAULT = 1;
    int VARIANTS_MIN = 1;
    int VARIANTS_MAX = 4;
    int variants = VARIANTS_DEFAULT;

    final static int MOVES_DEFAULT = 16;
    int MOVES_MIN = 1;
    int MOVES_MAX = 30;
    int moves = MOVES_DEFAULT;

    final static int LINES_DEFAULT = 4;
    int LINES_MIN = 1;
    int LINES_MAX = 9;
    int lines = LINES_DEFAULT;

    final static int ARROWS_DEFAULT = 6;
    int ARROWS_MIN = 0;
    int ARROWS_MAX = 6;
    int arrows = ARROWS_DEFAULT;

    final static int PIECES_DEFAULT = 0;
    int PIECES_MIN = 0;
    int PIECES_MAX = 2;
    int pieceNameId = PIECES_DEFAULT;

    final static int TEXTSIZE_DEFAULT = 2;
    int TEXTSIZE_MIN = 1;
    int TEXTSIZE_MAX = 4;
    int textsizeId = TEXTSIZE_DEFAULT;
    int FONTSIZE_SMALL = 10;
    final static int FONTSIZE_MEDIUM = 14;
    int FONTSIZE_LARGE = 18;
    int FONTSIZE_LARGEST = 22;
    int fontSize = FONTSIZE_MEDIUM;
    float textsizePixel = 0F;

    // Engine Settings
    TextView variantsMinus;
    TextView variantsValue;
    TextView variantsPlus;
    TextView movesMinus;
    TextView movesValue;
    TextView movesPlus;
    TextView linesMinus;
    TextView linesValue;
    TextView linesPlus;
    TextView arrowsMinus;
    TextView arrowsValue;
    TextView arrowsPlus;
    CheckBox engineMessage;
    CheckBox ponder;
    CheckBox randomFirstMove;
    CheckBox autoStartEngine;
    CheckBox debugInformation;
    CheckBox logOn;

    // Appearance
    TextView btn_color_settings;
    TextView piecesMinus;
    TextView piecesValue;
    TextView piecesPlus;
    TextView textsizeMinus;
    TextView textsizeValue;
    TextView textsizePlus;
    CheckBox cbGuCoordinates;
    CheckBox cbGuPosibleMoves;
    CheckBox cbGuQuickMove;
    CheckBox cbGuStatusBar;
    CheckBox cbGuDisableScreenTimeout;
    CheckBox cbGuFlipBoard;
    CheckBox cbGuGameNavigationBoard;
    CheckBox cbGuBlindMode;

    // Time, Clock
    TextView btn_time_settings;

    // Opening Book
    CheckBox openingBook;
    CheckBox showBookHints;
    ImageView epBook = null;
    EditText bookName = null;

    // Other
    EditText etGuPlayerName = null;
    CheckBox cbGuUsePgnDatabase;
    CheckBox cbGuLastPosition;
    CheckBox cbGuEnableSounds;

    TextView btnSettingsCancel;
    TextView btnSettingsOk;

}
