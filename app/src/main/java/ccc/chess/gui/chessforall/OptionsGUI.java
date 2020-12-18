package ccc.chess.gui.chessforall;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import ccc.chess.logic.c4aservice.ChessHistory;

public class OptionsGUI extends Activity
{

	public void onCreate(Bundle savedInstanceState) 
	{

//		Log.i(TAG, "onCreate()");

        super.onCreate(savedInstanceState);
		u = new Util();
		userPrefs = getSharedPreferences("user", 0);
		runPrefs = getSharedPreferences("run", 0);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		u.updateFullscreenStatus(this, userPrefs.getBoolean("user_options_gui_StatusBar", false));
        setContentView(R.layout.optionsgui);
		tvTitle = (TextView) findViewById(R.id.title);
		setTextViewColors(tvTitle, "#ced1d6");
        etGuPlayerName = (EditText) findViewById(R.id.etGuPlayerName);
        cbGuStatusBar = (CheckBox) findViewById(R.id.cbGuStatusBar);
		cbGuFlipBoard = (CheckBox) findViewById(R.id.cbGuFlipBoard);
        cbGuLastPosition = (CheckBox) findViewById(R.id.cbGuLastPosition);
        cbGuDisableScreenTimeout = (CheckBox) findViewById(R.id.cbGuDisableScreenTimeout);
        cbGuPosibleMoves = (CheckBox) findViewById(R.id.cbGuPosibleMoves);
        cbGuQuickMove = (CheckBox) findViewById(R.id.cbGuQuickMove);
		cbGuGameNavigationBoard = (CheckBox) findViewById(R.id.cbGuGameNavigationBoard);
        cbGuUsePgnDatabase = (CheckBox) findViewById(R.id.cbGuUsePgnDatabase);
        cbGuEnableSounds = (CheckBox) findViewById(R.id.cbGuEnableSounds);
        cbGuCoordinates = (CheckBox) findViewById(R.id.cbGuCoordinates);
		cbGuBlindMode = (CheckBox) findViewById(R.id.cbGuBlindMode);

		getPrefs();

		piecesMinus =  findViewById(R.id.piecesMinus);
		piecesMinus.setOnClickListener(v -> {
			pieceNameId--;
			setPieces();
		});
		piecesValue = findViewById(R.id.piecesValue);
		piecesValue.setText(getPieceNames(pieceNameId));
		piecesValue.setOnClickListener(v -> {
			pieceNameId = PIECES_DEFAULT;
			setPieces();
		});
		piecesPlus = (TextView) findViewById(R.id.piecesPlus);
		piecesPlus.setOnClickListener(v -> {
			pieceNameId++;
			setPieces();
		});
		setPieces();

		tvMinus = findViewById(R.id.tvMinus);
		tvMinus.setOnClickListener(v -> {
			arrows--;
			setArrows();
		});
		tvValue = findViewById(R.id.tvValue);
		tvValue.setText(Integer.toString(arrows));
		tvValue.setOnClickListener(v -> {
			arrows = ARROWS_DEFAULT;
			setArrows();
		});
		tvPlus = findViewById(R.id.tvPlus);
		tvPlus.setOnClickListener(v -> {
			arrows++;
			setArrows();
		});
		setArrows();

	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{

//		Log.i(TAG, "onConfigurationChanged()");

		super.onConfigurationChanged(newConfig);
		setPrefs();
	}

	public void myClickHandler(View view)
	{

//		Log.i(TAG, "myClickHandler(), view.getId(): " + view.getId());

		Intent returnIntent;
		switch (view.getId())
		{
			case R.id.btnGuOk:
				setPrefs();
				returnIntent = new Intent();
				setResult(RESULT_OK, returnIntent);
				finish();
				break;
		}
	}

	public void setPieces()
	{
		setTextViewColors(piecesMinus, "#f6d2f4");
		setTextViewColors(piecesPlus, "#c4f8c0");
		if (pieceNameId <= PIECES_MIN) {
			pieceNameId = PIECES_MIN;
			setTextViewColors(piecesMinus, "#767a76");
		}
		if (pieceNameId >= PIECES_MAX) {
			pieceNameId = PIECES_MAX;
			setTextViewColors(piecesPlus, "#767a76");
		}
		piecesValue.setText(getPieceNames(pieceNameId));
		setTextViewColors(piecesValue, "#efe395");
	}

	public void setArrows()
	{
		setTextViewColors(tvMinus, "#f6d2f4");
		setTextViewColors(tvPlus, "#c4f8c0");
		if (arrows <= ARROWS_MIN) {
			arrows = ARROWS_MIN;
			setTextViewColors(tvMinus, "#767a76");
		}
		if (arrows >= ARROWS_MAX) {
			arrows = ARROWS_MAX;
			setTextViewColors(tvPlus, "#767a76");
		}
		tvValue.setText(Integer.toString(arrows));
		setTextViewColors(tvValue, "#efe395");
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

	public void setTextViewColors(TextView tv, String color)
	{
		if (tv != null) {
			GradientDrawable tvBackground = (GradientDrawable) tv.getBackground();
			if (tvBackground != null)
				tvBackground.setColor(Color.parseColor(color));
		}
	}

	protected void setPrefs() 
	{
		SharedPreferences.Editor ed = userPrefs.edit();
		ed.putString("user_options_gui_playerName", etGuPlayerName.getText().toString());
        ed.putBoolean("user_options_gui_StatusBar", cbGuStatusBar.isChecked());
        ed.putBoolean("user_options_gui_FlipBoard", cbGuFlipBoard.isChecked());
        ed.putBoolean("user_options_gui_LastPosition", cbGuLastPosition.isChecked());
        ed.putBoolean("user_options_gui_disableScreenTimeout", cbGuDisableScreenTimeout.isChecked());
        ed.putBoolean("user_options_gui_posibleMoves", cbGuPosibleMoves.isChecked());
        ed.putBoolean("user_options_gui_quickMove", cbGuQuickMove.isChecked());
        ed.putBoolean("user_options_gui_gameNavigationBoard", cbGuGameNavigationBoard.isChecked());
       	ed.putBoolean("user_options_gui_usePgnDatabase", cbGuUsePgnDatabase.isChecked());
       	ed.putBoolean("user_options_gui_enableSounds", cbGuEnableSounds.isChecked());
       	ed.putBoolean("user_options_gui_Coordinates", cbGuCoordinates.isChecked());
       	ed.putBoolean("user_options_gui_BlindMode", cbGuBlindMode.isChecked());
       	ed.putInt("user_options_gui_PieceNameId", pieceNameId);
       	ed.putInt("user_options_gui_arrows", arrows);
        ed.commit();
	}

	protected void getPrefs() 
	{
		etGuPlayerName.setText(userPrefs.getString("user_options_gui_playerName", "Me"));
		cbGuStatusBar.setChecked(userPrefs.getBoolean("user_options_gui_StatusBar", false));	// fullScreen !!!
		cbGuFlipBoard.setChecked(userPrefs.getBoolean("user_options_gui_FlipBoard", false));
		cbGuLastPosition.setChecked(userPrefs.getBoolean("user_options_gui_LastPosition", false));
		cbGuDisableScreenTimeout.setChecked(userPrefs.getBoolean("user_options_gui_disableScreenTimeout", false));
		cbGuPosibleMoves.setChecked(userPrefs.getBoolean("user_options_gui_posibleMoves", true));
		cbGuQuickMove.setChecked(userPrefs.getBoolean("user_options_gui_quickMove", true));
		cbGuGameNavigationBoard.setChecked(userPrefs.getBoolean("user_options_gui_gameNavigationBoard", false));
		cbGuUsePgnDatabase.setChecked(userPrefs.getBoolean("user_options_gui_usePgnDatabase", true));
		cbGuEnableSounds.setChecked(userPrefs.getBoolean("user_options_gui_enableSounds", true));
		cbGuCoordinates.setChecked(userPrefs.getBoolean("user_options_gui_Coordinates", false));
		cbGuBlindMode.setChecked(userPrefs.getBoolean("user_options_gui_BlindMode", false));
		pieceNameId = userPrefs.getInt("user_options_gui_PieceNameId", pieceNameId);
		arrows = userPrefs.getInt("user_options_gui_arrows", arrows);
	}

	final String TAG = "OptionsGUI";
	Util u;
	SharedPreferences userPrefs;
	SharedPreferences runPrefs;
	TextView tvTitle;
	EditText etGuPlayerName = null;
	CheckBox cbGuStatusBar;
	CheckBox cbGuFlipBoard;
	CheckBox cbGuLastPosition;
	CheckBox cbGuDisableScreenTimeout;
	CheckBox cbGuPosibleMoves;
	CheckBox cbGuQuickMove;
	CheckBox cbGuGameNavigationBoard;
	CheckBox cbGuUsePgnDatabase;
	CheckBox cbGuEnableSounds;
	CheckBox cbGuCoordinates;
	CheckBox cbGuBlindMode;

	TextView piecesMinus;
	TextView piecesValue;
	TextView piecesPlus;

	TextView tvMinus;
	TextView tvValue;
	TextView tvPlus;

	final static int PIECES_DEFAULT = 0;
	int PIECES_MIN = 0;
	int PIECES_MAX = 2;
	int pieceNameId = PIECES_DEFAULT;

	final static int ARROWS_DEFAULT = 6;
	int ARROWS_MIN = 0;
	int ARROWS_MAX = 6;
	int arrows = ARROWS_DEFAULT;

}
