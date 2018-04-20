package ccc.chess.gui.chessforall;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.EditText;

public class OptionsGUI extends Activity
{	//	C4aMain RequestCode: OPTIONS_GUI_REQUEST_CODE

	public void onCreate(Bundle savedInstanceState) 
	{
        super.onCreate(savedInstanceState);
		u = new Util();
		userPrefs = getSharedPreferences("user", 0);
		runPrefs = getSharedPreferences("run", 0);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		u.updateFullscreenStatus(this, userPrefs.getBoolean("user_options_gui_StatusBar", true));
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
        getPrefs();
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
        ed.commit();
	}

	protected void getPrefs() 
	{
		etGuPlayerName.setText(userPrefs.getString("user_options_gui_playerName", "Me"));
		cbGuStatusBar.setChecked(userPrefs.getBoolean("user_options_gui_StatusBar", true));
		cbGuFlipBoard.setChecked(userPrefs.getBoolean("user_options_gui_FlipBoard", false));
		cbGuLastPosition.setChecked(userPrefs.getBoolean("user_options_gui_LastPosition", false));
		cbGuDisableScreenTimeout.setChecked(userPrefs.getBoolean("user_options_gui_disableScreenTimeout", false));
		cbGuGameNavigationBoard.setChecked(userPrefs.getBoolean("user_options_gui_gameNavigationBoard", false));
		cbGuUsePgnDatabase.setChecked(userPrefs.getBoolean("user_options_gui_usePgnDatabase", true));
		cbGuEnableSounds.setChecked(userPrefs.getBoolean("user_options_gui_enableSounds", true));
		cbGuCoordinates.setChecked(userPrefs.getBoolean("user_options_gui_Coordinates", false));
	}

	final String TAG = "PlaySettings";
	Util u;
	SharedPreferences userPrefs;
	SharedPreferences runPrefs;
	EditText etGuPlayerName = null;
	CheckBox cbGuStatusBar;
	CheckBox cbGuFlipBoard;
	CheckBox cbGuLastPosition;
	CheckBox cbGuDisableScreenTimeout;
	CheckBox cbGuGameNavigationBoard;
	CheckBox cbGuUsePgnDatabase;
	CheckBox cbGuEnableSounds;
	CheckBox cbGuCoordinates;

}
