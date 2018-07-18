package ccc.chess.gui.chessforall;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

public class OptionsTimeControl extends Activity implements Ic4aDialogCallback
{

	public void onCreate(Bundle savedInstanceState) 
	{
        super.onCreate(savedInstanceState);
		u = new Util();
		userP = getSharedPreferences("user", 0);
		runP = getSharedPreferences("run", 0);		//	run Preferences
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		u.updateFullscreenStatus(this, userP.getBoolean("user_options_gui_StatusBar", true));
        setContentView(R.layout.optionstimecontrol);
        resultCode = 101;
        tc = new TimeControl();
        getPrefs();
        btnTcApply = (ImageView) findViewById(R.id.btnTcApply);
        rgTimeControl = (RadioGroup) findViewById(R.id.rgTimeControl); 
        rbTcGameClock = (RadioButton) findViewById(R.id.rbTcGameClock); 
        rbTcMoveTime = (RadioButton) findViewById(R.id.rbTcMoveTime); 
        rbTcSandGlass = (RadioButton) findViewById(R.id.rbTcSandGlass); 
        rbTcNone = (RadioButton) findViewById(R.id.rbTcNone);
        if (timeControl == 1) {rbTcGameClock.setChecked(true);}
        if (timeControl == 2) {rbTcMoveTime.setChecked(true);}
        if (timeControl == 3) {rbTcSandGlass.setChecked(true);}
        if (timeControl == 4) {rbTcNone.setChecked(true);}
        rgTimeControl.setOnCheckedChangeListener(rgListener);
        btnPlayer = (ImageView) findViewById(R.id.btnPlayer);
        btnEngine = (ImageView) findViewById(R.id.btnEngine);
        tvPlayer = (TextView) findViewById(R.id.tvPlayer);
        tvEngine = (TextView) findViewById(R.id.tvEngine);
        showTimeValues(timeControl);
	}

    public Dialog onCreateDialog(int id)
	{
    	activDialog = id;
		if (id == TIME_SETTINGS_DIALOG)  
        {
			chessClockTitle = getString(R.string.ccsTitle);
			timeSettingsDialog = new TimeSettingsDialog(this, this, chessClockTitle, chessClockMessage,
					chessClockTimeGame, chessClockTimeBonus, chessClockMovesToGo);
			return timeSettingsDialog;
        }
		return null;
	}

    @Override
	public void getCallbackValue(int btnValue) 
    {
    	if (activDialog == TIME_SETTINGS_DIALOG)
		{	// chessClockControl
			if (btnValue == 2)
    		{
				SharedPreferences.Editor ed = userP.edit();
				switch (chessClockControl) 										
				{
					case 11: 	// player (game clock)
	    				ed.putInt("user_time_player_clock", timeSettingsDialog.getTime());
	    				ed.putInt("user_bonus_player_clock", timeSettingsDialog.getBonus());
	    				break;
					case 12: 	// engine (game clock)
	    				ed.putInt("user_time_engine_clock", timeSettingsDialog.getTime());
	    				ed.putInt("user_bonus_engine_clock", timeSettingsDialog.getBonus());
						break;
					case 21: 	// player (move)
						resultCode = timeSettingsDialog.getBonus();
						ed.putInt("user_time_player_move", timeSettingsDialog.getTime());
						resultCode = 101;
	    				break;
					case 22: 	// engine (move)
	    				ed.putInt("user_time_engine_move", timeSettingsDialog.getTime());
	    				break;
					case 31: 	// player (sand glass)
	    				ed.putInt("user_time_player_sand", timeSettingsDialog.getTime());
	    				break;
					case 32: 	// engine (sand glass)
	    				ed.putInt("user_time_engine_sand", timeSettingsDialog.getTime());
	    				break;
				}
				ed.commit();
				showTimeValues(timeControl);
    		}
		}
		
	}

    public void c4aShowDialog(int dialogId)					
    {
		removeDialog(dialogId);
		showDialog(dialogId);
    }

    public void showTimeValues(int timeControl)					
    {
    	btnPlayer.setVisibility(Button.VISIBLE);
    	btnEngine.setVisibility(Button.VISIBLE);
    	tvPlayer.setVisibility(TextView.VISIBLE);
    	tvEngine.setVisibility(TextView.VISIBLE);
    	String timePlayer = "";
    	String timeEngine = "";
    	String bonusPlayer = "";
    	String bonusEngine = "";
    	switch (timeControl)
        {
	        case 1:	
	        	timePlayer = tc.getShowValues(userP.getInt("user_time_player_clock", 300000), false);
	        	bonusPlayer = " +" + tc.getShowValues(userP.getInt("user_bonus_player_clock", 3000), false);
	        	timeEngine = tc.getShowValues(userP.getInt("user_time_engine_clock", 60000), false);
	        	bonusEngine = " +" + tc.getShowValues(userP.getInt("user_bonus_engine_clock", 3000), false);
	        	break;   
	        case 2:		
	        	timePlayer = tc.getShowValues(userP.getInt("user_time_player_move", 600000), false);
	        	timeEngine = tc.getShowValues(userP.getInt("user_time_engine_move", 60000), false);
	        	break;
	        case 3:		
	        	timePlayer = tc.getShowValues(userP.getInt("user_time_player_sand", 600000), false);
	        	timeEngine = tc.getShowValues(userP.getInt("user_time_engine_sand", 60000), false);
	        	break;
	        case 4:
	        	btnPlayer.setVisibility(Button.INVISIBLE);
	        	btnEngine.setVisibility(Button.INVISIBLE);
	        	tvPlayer.setVisibility(TextView.INVISIBLE);
	        	tvEngine.setVisibility(TextView.INVISIBLE);
	        	break;
        }
    	tvPlayer.setText(timePlayer + bonusPlayer);
    	tvEngine.setText(timeEngine + bonusEngine);
    }

	public void myClickHandler(View view) 				
    {
		Intent returnIntent;
		switch (view.getId()) 
		{
		case R.id.tvPlayer:
		case R.id.btnPlayer:
			setTime(true);
			c4aShowDialog(TIME_SETTINGS_DIALOG);
			break;	
		case R.id.btnEngine:
		case R.id.tvEngine:
			setTime(false);
			c4aShowDialog(TIME_SETTINGS_DIALOG);
			break;
		case R.id.btnTcOk:
			setPrefs();
        	returnIntent = new Intent();
       		setResult(RESULT_OK, returnIntent);
			finish();
			break;
		case R.id.btnTcApply:
			setTitle(getString(R.string.engineProgressDialog));
			setPrefs();
        	returnIntent = new Intent();
       		setResult(resultCode, returnIntent);
			finish();
			break;
		}
	}

	public void setTime(boolean isPlayer) 				
    {	
		switch (timeControl) 
        { 
	        case 1:
	        	if (isPlayer)
	        	{
	        		chessClockMessage = getString(R.string.ccsMessagePlayerClock);
	        		chessClockControl = 11;
					chessClockTimeGame = userP.getInt("user_time_player_clock", 300000);  
					chessClockTimeBonus = userP.getInt("user_bonus_player_clock", 3000);
	        	}
	        	else
	        	{
	        		chessClockMessage = getString(R.string.ccsMessageEngineClock);
	        		chessClockControl = 12;
					chessClockTimeGame = userP.getInt("user_time_engine_clock", 60000);
					chessClockTimeBonus = userP.getInt("user_bonus_engine_clock", 3000);
	        	}
	            break;
	        case 2:
	        	if (isPlayer)
	        	{
	        		chessClockMessage = getString(R.string.ccsMessagePlayerMove);
					chessClockControl = 21;
					chessClockTimeGame = userP.getInt("user_time_player_move", 600000);
					chessClockTimeBonus = -1;
	        	}
	        	else
	        	{
	        		chessClockMessage = getString(R.string.ccsMessageEngineMove);
					chessClockControl = 22;
					chessClockTimeGame = userP.getInt("user_time_engine_move", 60000);
					chessClockTimeBonus = -1;
	        	}
	            break; 
	        case 3:
	        	if (isPlayer)
	        	{
	        		chessClockMessage = getString(R.string.ccsMessagePlayerSand);
					chessClockControl = 31;
					chessClockTimeGame = userP.getInt("user_time_player_sand", 600000);
					chessClockTimeBonus = -1;
	        	}
	        	else
	        	{
	        		chessClockMessage = getString(R.string.ccsMessageEngineSand);
					chessClockControl = 32;
					chessClockTimeGame = userP.getInt("user_time_engine_sand", 60000);
					chessClockTimeBonus = -1;
	        	}
	            break; 
        }
    }

	private OnCheckedChangeListener rgListener = new OnCheckedChangeListener()		
	{	// Radio Button Listener
		@Override 
		public void onCheckedChanged(RadioGroup arg0, int checkedId) 
		{ 
			if (rbTcGameClock.getId() == checkedId)
				timeControl = 1;
			if (rbTcMoveTime.getId() == checkedId) 				
				timeControl = 2;
			if (rbTcSandGlass.getId() == checkedId) 				
				timeControl = 3;
			if (rbTcNone.getId() == checkedId) 				
				timeControl = 4;
			showTimeValues(timeControl);
		} 
	};

	protected void setPrefs() 
	{
		SharedPreferences.Editor ed = userP.edit();
        ed.putInt("user_options_timeControl", timeControl);
        ed.commit();
	}

	protected void getPrefs()
	{
		timeControl = userP.getInt("user_options_timeControl", 1);
	}
	
//	MainActivity RequestCode: OPTIONS_TIME_CONTROL_REQUEST_CODE
	final String TAG = "OptionsTimeControl";
	Util u;
	final static int TIME_SETTINGS_DIALOG = 400;
	TimeControl tc;
	SharedPreferences userP;
	SharedPreferences runP;
	TimeSettingsDialog timeSettingsDialog;
	int resultCode = 0;
	int activDialog = 0;
	int timeControl = 1;
	RadioGroup rgTimeControl;
	RadioButton rbTcGameClock;
	RadioButton rbTcMoveTime;
	RadioButton rbTcSandGlass;
	RadioButton rbTcNone;
	ImageView btnPlayer = null;
	ImageView btnEngine = null;
	ImageView btnTcApply = null;
	TextView tvPlayer = null;
	TextView tvEngine = null;

//  variables time settings
	int chessClockControl = 0;
	String chessClockTitle = "";
	String chessClockMessage = "";
	int chessClockTimeGame = 0;
	int chessClockTimeBonus = 0;
	int chessClockMovesToGo = -1;

}
