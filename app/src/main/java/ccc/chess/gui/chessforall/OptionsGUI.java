package ccc.chess.gui.chessforall;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import ccc.chess.logic.c4aservice.ChessHistory;

public class OptionsGUI extends Activity implements Ic4aDialogCallback
{	//	C4aMain RequestCode: OPTIONS_GUI_REQUEST_CODE

	public void onCreate(Bundle savedInstanceState) 
	{

		Log.i(TAG, "onCreate()");

        super.onCreate(savedInstanceState);
		u = new Util();
		userPrefs = getSharedPreferences("user", 0);
		runPrefs = getSharedPreferences("run", 0);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		u.updateFullscreenStatus(this, userPrefs.getBoolean("user_options_gui_StatusBar", false));
        setContentView(R.layout.optionsgui);
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
		tvGuPieceName = (TextView) findViewById(R.id.tvGuPieceName);
        getPrefs();
		tvGuPieceName.setText(getString(R.string.pieceNames) + ": " + getPieceNames(pieceNameId));
		tvGuPieceName.setOnClickListener(v -> {
			removeDialog(MENU_PIECE_NAMES);
			showDialog(MENU_PIECE_NAMES);
		});
		tvMinus = (TextView) findViewById(R.id.tvMinus);
		tvMinus.setOnClickListener(v -> {
			arrows--;
			setArrows();
		});
		tvValue = (TextView) findViewById(R.id.tvValue);
		tvValue.setText(Integer.toString(arrows));
		tvPlus = (TextView) findViewById(R.id.tvPlus);
		tvPlus.setOnClickListener(v -> {
			arrows++;
			setArrows();
		});
		setArrows();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{

		Log.i(TAG, "onConfigurationChanged()");

		super.onConfigurationChanged(newConfig);
		setPrefs();
	}

	@Override
	protected Dialog onCreateDialog(int id)
	{
		if (id == MENU_PIECE_NAMES)
		{
			final int MENU_ENGLISH 				= 0;
			final int MENU_LOCAL_LANGUAGE 		= 1;
			final int MENU_FIGURINE_NOTATION 	= 2;
			ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.drawer_list_item);
			List<Integer> actions = new ArrayList<Integer>();
			arrayAdapter.add(getString(R.string.pieceNamesEnglish) 			+ ": " + getPieceNames(0));   actions.add(MENU_ENGLISH);
			arrayAdapter.add(getString(R.string.pieceNamesLocalLanguage) 	+ ": " + getPieceNames(1));   actions.add(MENU_LOCAL_LANGUAGE);
			arrayAdapter.add(getString(R.string.pieceNamesFigurineNotation) + ": " + getPieceNames(2));   actions.add(MENU_FIGURINE_NOTATION);
			final List<Integer> finalActions = actions;
			AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
			builder.setCancelable(true);
			TextView tv = new TextView(getApplicationContext());
			tv.setText(R.string.pieceNames);
			tv.setTextAppearance(this, R.style.c4aDialogTitle);
			tv.setGravity(Gravity.CENTER_HORIZONTAL);
			builder.setCustomTitle(tv );
			builder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int item)
				{
					switch (finalActions.get(item))
					{
						case MENU_ENGLISH:
							pieceNameId = 0;
							break;
						case MENU_LOCAL_LANGUAGE:
							pieceNameId = 1;
							break;
						case MENU_FIGURINE_NOTATION:
							pieceNameId = 2;
							break;
					}
					removeDialog(MENU_PIECE_NAMES);
					tvGuPieceName.setText(getString(R.string.pieceNames) + ": " + getPieceNames(pieceNameId));
				}
			});
			AlertDialog alert = builder.create();
			return alert;
		}

		return null;

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

	public void setArrows()
	{
		tvMinus.setBackgroundResource(R.drawable.rectanglepink);
		tvPlus.setBackgroundResource(R.drawable.rectanglegreen);
		if (arrows <= ARROWS_MIN) {
			arrows = ARROWS_MIN;
			tvMinus.setBackgroundResource(R.drawable.rectanglegrey);
		}
		tvValue.setText(Integer.toString(arrows));
		if (arrows >= ARROWS_MAX) {
			arrows = ARROWS_MAX;
			tvValue.setText(Integer.toString(arrows));
			tvPlus.setBackgroundResource(R.drawable.rectanglegrey);
		}
	}

	@Override
	public void getCallbackValue(int btnValue)
	{

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
	final static int MENU_PIECE_NAMES = 100;
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
	TextView tvGuPieceName;
	TextView tvMinus;
	TextView tvValue;
	TextView tvPlus;

	int pieceNameId = 0;
	final static int ARROWS_DEFAULT = 6;
	int ARROWS_MIN = 0;
	int ARROWS_MAX = 8;
	int arrows = ARROWS_DEFAULT;

}
