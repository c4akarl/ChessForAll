package ccc.chess.gui.chessforall;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
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
		userPrefs = getSharedPreferences("user", 0);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		u.updateFullscreenStatus(this, userPrefs.getBoolean("user_options_gui_StatusBar", false));
        setContentView(R.layout.optionstimecontrol);
        tc = new TimeControl();
        getPrefs();
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
        btnPlayer = findViewById(R.id.btnPlayer);
		u.setTextViewColors(btnPlayer, "#BAB8B8");
        btnEngine = findViewById(R.id.btnEngine);
        u.setTextViewColors(btnEngine, "#BAB8B8");
        tvPlayer = (TextView) findViewById(R.id.tvPlayer);
		u.setTextViewColors(tvPlayer, "#c4f8c0");
        tvEngine = (TextView) findViewById(R.id.tvEngine);
		u.setTextViewColors(tvEngine, "#c4f8c0");
		btnTimeDelayReplay = findViewById(R.id.btnTimeDelayReplay);
		u.setTextViewColors(btnTimeDelayReplay, "#BAB8B8");
		tvTimeDelayReplay = findViewById(R.id.tvTimeDelayReplay);
		u.setTextViewColors(tvTimeDelayReplay, "#c4f8c0");
		tvTimeDelayReplay.setText(tc.getShowValues(userPrefs.getInt("user_options_timer_autoPlay", 1500), false));

		btnTcCancel = findViewById(R.id.btnTcCancel);
		u.setTextViewColors(btnTcCancel, "#BAB8B8");
		btnTcOk = findViewById(R.id.btnTcOk);
		u.setTextViewColors(btnTcOk, "#BAB8B8");

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

//				Log.i(TAG, "getCallbackValue(), timeSettingsDialog.getTime(): " + timeSettingsDialog.getTime());

				SharedPreferences.Editor ed = userPrefs.edit();
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
						ed.putInt("user_time_player_move", timeSettingsDialog.getTime());
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
					case 41: 	// timer auto play
						ed.putInt("user_options_timer_autoPlay", timeSettingsDialog.getBonus());
						break;
				}
				ed.apply();
				tvTimeDelayReplay.setText(tc.getShowValues(userPrefs.getInt("user_options_timer_autoPlay", 1500), false));
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
//	        	timePlayer = tc.getShowValues(userPrefs.getInt("user_time_player_clock", 300000), false);
	        	timePlayer = tc.getShowValues(userPrefs.getInt("user_time_player_clock", TIME_PLAYER_CLOCK), false);
//	        	bonusPlayer = " +" + tc.getShowValues(userPrefs.getInt("user_bonus_player_clock", 3000), false);
	        	bonusPlayer = " +" + tc.getShowValues(userPrefs.getInt("user_bonus_player_clock", TIME_PLAYER_BONUS), false);
//	        	timeEngine = tc.getShowValues(userPrefs.getInt("user_time_engine_clock", 60000), false);
	        	timeEngine = tc.getShowValues(userPrefs.getInt("user_time_engine_clock", TIME_ENGINE_CLOCK), false);
//	        	bonusEngine = " +" + tc.getShowValues(userPrefs.getInt("user_bonus_engine_clock", 3000), false);
	        	bonusEngine = " +" + tc.getShowValues(userPrefs.getInt("user_bonus_engine_clock", TIME_ENGINE_BONUS), false);
	        	break;
	        case 2:		
//	        	timePlayer = tc.getShowValues(userPrefs.getInt("user_time_player_move", 600000), false);
	        	timePlayer = tc.getShowValues(userPrefs.getInt("user_time_player_move", TIME_PLAYER_MOVE), false);
//	        	timeEngine = tc.getShowValues(userPrefs.getInt("user_time_engine_move", 60000), false);
	        	timeEngine = tc.getShowValues(userPrefs.getInt("user_time_engine_move", TIME_ENGINE_MOVE), false);
	        	break;
	        case 3:		
//	        	timePlayer = tc.getShowValues(userPrefs.getInt("user_time_player_sand", 600000), false);
	        	timePlayer = tc.getShowValues(userPrefs.getInt("user_time_player_sand", TIME_PLAYER_SAND), false);
//	        	timeEngine = tc.getShowValues(userPrefs.getInt("user_time_engine_sand", 60000), false);
	        	timeEngine = tc.getShowValues(userPrefs.getInt("user_time_engine_sand", TIME_ENGINE_SAND), false);
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
		case R.id.btnTimeDelayReplay:
		case R.id.tvTimeDelayReplay:
				chessClockMessage = getString(R.string.ccsMessageAutoPlay);
				chessClockControl = 41;
				chessClockTimeGame = -1;
				chessClockTimeBonus = userPrefs.getInt("user_options_timer_autoPlay", 1500);
				c4aShowDialog(TIME_SETTINGS_DIALOG);
				break;
		case R.id.btnTcCancel:
			finish();
			break;
		case R.id.btnTcOk:
			setPrefs();
			returnIntent = new Intent();
			setResult(RESULT_OK, returnIntent);
			finish();
			break;
		}
	}

//	public void setTextViewColors(TextView tv, String color)
//	{
//		if (tv != null) {
//			GradientDrawable tvBackground = (GradientDrawable) tv.getBackground();
//			if (tvBackground != null)
//				tvBackground.setColor(Color.parseColor(color));
//		}
//	}

	public void setTime(boolean isPlayer) 				
    {	
		switch (timeControl) 
        { 
	        case 1:
	        	if (isPlayer)
	        	{
	        		chessClockMessage = getString(R.string.ccsMessagePlayerClock);
	        		chessClockControl = 11;
//					chessClockTimeGame = userPrefs.getInt("user_time_player_clock", 300000);
					chessClockTimeGame = userPrefs.getInt("user_time_player_clock", TIME_PLAYER_CLOCK);
//					chessClockTimeBonus = userPrefs.getInt("user_bonus_player_clock", 3000);
					chessClockTimeBonus = userPrefs.getInt("user_bonus_player_clock", TIME_PLAYER_BONUS);
	        	}
	        	else
	        	{
	        		chessClockMessage = getString(R.string.ccsMessageEngineClock);
	        		chessClockControl = 12;
//					chessClockTimeGame = userPrefs.getInt("user_time_engine_clock", 60000);
					chessClockTimeGame = userPrefs.getInt("user_time_engine_clock", TIME_ENGINE_CLOCK);
//					chessClockTimeBonus = userPrefs.getInt("user_bonus_engine_clock", 3000);
					chessClockTimeBonus = userPrefs.getInt("user_bonus_engine_clock", TIME_ENGINE_BONUS);
	        	}
	            break;
	        case 2:
	        	if (isPlayer)
	        	{
	        		chessClockMessage = getString(R.string.ccsMessagePlayerMove);
					chessClockControl = 21;
//					chessClockTimeGame = userPrefs.getInt("user_time_player_move", 600000);
					chessClockTimeGame = userPrefs.getInt("user_time_player_move", TIME_PLAYER_MOVE);
					chessClockTimeBonus = -1;
	        	}
	        	else
	        	{
	        		chessClockMessage = getString(R.string.ccsMessageEngineMove);
					chessClockControl = 22;
//					chessClockTimeGame = userPrefs.getInt("user_time_engine_move", 60000);
					chessClockTimeGame = userPrefs.getInt("user_time_engine_move", TIME_ENGINE_MOVE);
					chessClockTimeBonus = -1;
	        	}
	            break; 
	        case 3:
	        	if (isPlayer)
	        	{
	        		chessClockMessage = getString(R.string.ccsMessagePlayerSand);
					chessClockControl = 31;
//					chessClockTimeGame = userPrefs.getInt("user_time_player_sand", 600000);
					chessClockTimeGame = userPrefs.getInt("user_time_player_sand", TIME_PLAYER_SAND);
					chessClockTimeBonus = -1;
	        	}
	        	else
	        	{
	        		chessClockMessage = getString(R.string.ccsMessageEngineSand);
					chessClockControl = 32;
//					chessClockTimeGame = userPrefs.getInt("user_time_engine_sand", 60000);
					chessClockTimeGame = userPrefs.getInt("user_time_engine_sand", TIME_ENGINE_SAND);
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
		SharedPreferences.Editor ed = userPrefs.edit();
        ed.putInt("user_options_timeControl", timeControl);
        ed.commit();
	}

	protected void getPrefs()
	{
		timeControl = userPrefs.getInt("user_options_timeControl", 1);
	}
	
	final String TAG = "OptionsTimeControl";
	Util u;
	final static int TIME_SETTINGS_DIALOG = 400;

	final static int TIME_PLAYER_CLOCK = 300000;
	final static int TIME_PLAYER_BONUS = 10000;
	final static int TIME_PLAYER_MOVE = 600000;
	final static int TIME_PLAYER_SAND = 600000;
	final static int TIME_ENGINE_CLOCK = 60000;
	final static int TIME_ENGINE_BONUS = 3000;
	final static int TIME_ENGINE_MOVE = 5000;
	final static int TIME_ENGINE_SAND = 10000;

	TimeControl tc;
	SharedPreferences userPrefs;
	TimeSettingsDialog timeSettingsDialog;
	int activDialog = 0;
	int timeControl = 1;

	RadioGroup rgTimeControl;
	RadioButton rbTcGameClock;
	RadioButton rbTcMoveTime;
	RadioButton rbTcSandGlass;
	RadioButton rbTcNone;
	TextView btnPlayer = null;
	TextView btnEngine = null;
	TextView tvPlayer = null;
	TextView tvEngine = null;
	TextView btnTimeDelayReplay = null;
	TextView tvTimeDelayReplay = null;

	TextView btnTcCancel = null;
	TextView btnTcOk = null;

//  variables time settings
	int chessClockControl = 0;
	String chessClockTitle = "";
	String chessClockMessage = "";
	int chessClockTimeGame = 0;
	int chessClockTimeBonus = 0;
	int chessClockMovesToGo = -1;

}
