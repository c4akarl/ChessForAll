package ccc.chess.gui.chessforall;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
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
		cbGuGameNavigationBoard = (CheckBox) findViewById(R.id.cbGuGameNavigationBoard);
        cbGuUsePgnDatabase = (CheckBox) findViewById(R.id.cbGuUsePgnDatabase);
        cbGuEnableSounds = (CheckBox) findViewById(R.id.cbGuEnableSounds);
        cbGuCoordinates = (CheckBox) findViewById(R.id.cbGuCoordinates);
		tvGuPieceName = (TextView) findViewById(R.id.tvGuPieceName);
        getPrefs();
		tvGuPieceName.setText(getString(R.string.pieceNames) + ": " + getPieceNames(pieceNameId));
	}

	public void myClickHandler(View view) 				
    {
		Intent returnIntent;
		switch (view.getId()) 
		{
		case R.id.btnGuOk:
			setPrefs();
        	returnIntent = new Intent();
       		setResult(RESULT_OK, returnIntent);
			finish();
			break;
		case R.id.tvGuPieceName:
			removeDialog(MENU_PIECE_NAMES);
			showDialog(MENU_PIECE_NAMES);
			break;
		}
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
        ed.putBoolean("user_options_gui_gameNavigationBoard", cbGuGameNavigationBoard.isChecked());
       	ed.putBoolean("user_options_gui_usePgnDatabase", cbGuUsePgnDatabase.isChecked());
       	ed.putBoolean("user_options_gui_enableSounds", cbGuEnableSounds.isChecked());
       	ed.putBoolean("user_options_gui_Coordinates", cbGuCoordinates.isChecked());
       	ed.putInt("user_options_gui_PieceNameId", pieceNameId);
        ed.commit();
	}

	protected void getPrefs() 
	{
		etGuPlayerName.setText(userPrefs.getString("user_options_gui_playerName", "Me"));
		cbGuStatusBar.setChecked(userPrefs.getBoolean("user_options_gui_StatusBar", false));	// fullScreen !!!
		cbGuFlipBoard.setChecked(userPrefs.getBoolean("user_options_gui_FlipBoard", false));
		cbGuLastPosition.setChecked(userPrefs.getBoolean("user_options_gui_LastPosition", false));
		cbGuDisableScreenTimeout.setChecked(userPrefs.getBoolean("user_options_gui_disableScreenTimeout", false));
		cbGuGameNavigationBoard.setChecked(userPrefs.getBoolean("user_options_gui_gameNavigationBoard", false));
		cbGuUsePgnDatabase.setChecked(userPrefs.getBoolean("user_options_gui_usePgnDatabase", true));
		cbGuEnableSounds.setChecked(userPrefs.getBoolean("user_options_gui_enableSounds", true));
		cbGuCoordinates.setChecked(userPrefs.getBoolean("user_options_gui_Coordinates", false));
		pieceNameId = userPrefs.getInt("user_options_gui_PieceNameId", pieceNameId);
	}

	final String TAG = "PlaySettings";
	Util u;
	SharedPreferences userPrefs;
	SharedPreferences runPrefs;
	final static int MENU_PIECE_NAMES = 100;
	EditText etGuPlayerName = null;
	CheckBox cbGuStatusBar;
	CheckBox cbGuFlipBoard;
	CheckBox cbGuLastPosition;
	CheckBox cbGuDisableScreenTimeout;
	CheckBox cbGuGameNavigationBoard;
	CheckBox cbGuUsePgnDatabase;
	CheckBox cbGuEnableSounds;
	CheckBox cbGuCoordinates;
	TextView tvGuPieceName;

	int pieceNameId = 0;

}
