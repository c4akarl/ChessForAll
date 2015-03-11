package ccc.chess.gui.chessforall;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

public class OptionsGUI extends Activity
{	//	C4aMain RequestCode: OPTIONS_GUI_REQUEST_CODE
	public void onCreate(Bundle savedInstanceState) 
	{
        super.onCreate(savedInstanceState);
        setContentView(R.layout.optionsgui);
        userPrefs = getSharedPreferences("user", 0);
        runPrefs = getSharedPreferences("run", 0);
        etGuPlayerName = (EditText) findViewById(R.id.etGuPlayerName);
        cbGuStatusBar = (CheckBox) findViewById(R.id.cbGuStatusBar); 
        cbGuLastPosition = (CheckBox) findViewById(R.id.cbGuLastPosition); 
//        cbGuShowHelpInfo = (CheckBox) findViewById(R.id.cbGuShowHelpInfo); 
        cbGuDisableScreenTimeout = (CheckBox) findViewById(R.id.cbGuDisableScreenTimeout); 
        cbGuUsePgnDatabase = (CheckBox) findViewById(R.id.cbGuUsePgnDatabase);
        cbGuEnableSounds = (CheckBox) findViewById(R.id.cbGuEnableSounds);
        cbGuCoordinates = (CheckBox) findViewById(R.id.cbGuCoordinates);
        getPrefs();
        setTitle(getString(R.string.app_optionsGUI));
	}
	public void myClickHandler(View view) 				
    {	// ClickHandler	(ButtonEvents)
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
        ed.putBoolean("user_options_gui_LastPosition", cbGuLastPosition.isChecked());
        ed.putBoolean("user_options_gui_disableScreenTimeout", cbGuDisableScreenTimeout.isChecked());
       	ed.putBoolean("user_options_gui_usePgnDatabase", cbGuUsePgnDatabase.isChecked());
       	ed.putBoolean("user_options_gui_enableSounds", cbGuEnableSounds.isChecked());
       	ed.putBoolean("user_options_gui_Coordinates", cbGuCoordinates.isChecked());
        ed.commit();
	}
	protected void getPrefs() 
	{
		etGuPlayerName.setText(userPrefs.getString("user_options_gui_playerName", "Me"));
		cbGuStatusBar.setChecked(userPrefs.getBoolean("user_options_gui_StatusBar", false));
		cbGuLastPosition.setChecked(userPrefs.getBoolean("user_options_gui_LastPosition", false));
//		cbGuShowHelpInfo.setChecked(userPrefs.getBoolean("user_options_gui_showHelpInfo", true));
		cbGuDisableScreenTimeout.setChecked(userPrefs.getBoolean("user_options_gui_disableScreenTimeout", false));
		cbGuUsePgnDatabase.setChecked(userPrefs.getBoolean("user_options_gui_usePgnDatabase", true));
		cbGuEnableSounds.setChecked(userPrefs.getBoolean("user_options_gui_enableSounds", true));
		
//		cbGuCoordinates.setChecked(userPrefs.getBoolean("user_options_gui_Coordinates", true));	// T E S T
		cbGuCoordinates.setChecked(userPrefs.getBoolean("user_options_gui_Coordinates", false));
	}
	
	final String TAG = "PlaySettings";
	SharedPreferences userPrefs;
	SharedPreferences runPrefs;
//	GUI	
	EditText etGuPlayerName = null;
	CheckBox cbGuStatusBar;
	CheckBox cbGuLastPosition;
//	CheckBox cbGuShowHelpInfo;
	CheckBox cbGuDisableScreenTimeout;
	CheckBox cbGuUsePgnDatabase;
	CheckBox cbGuEnableSounds;
	CheckBox cbGuCoordinates;
}
