package ccc.chess.gui.chessforall;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

public class OptionsPlay extends Activity
{
	public void onCreate(Bundle savedInstanceState) 
	{
        super.onCreate(savedInstanceState);
        setContentView(R.layout.optionsplay);
        userP = getSharedPreferences("user", 0);
        runP = getSharedPreferences("run", 0);		//	run Preferences
        getPrefs();
        if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_SMALL)
        	getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    	else
    		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN, WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        rgPlay = (RadioGroup) findViewById(R.id.rgPlay); 
        rbPlay_white = (RadioButton) findViewById(R.id.rbPlay_white); 
        rbPlay_black = (RadioButton) findViewById(R.id.rbPlay_black); 
        rbPlay_engine = (RadioButton) findViewById(R.id.rbPlay_engine);
        rbPlay_analysis = (RadioButton) findViewById(R.id.rbPlay_analysis); 
        rbPlay_two_players_flip = (RadioButton) findViewById(R.id.rbPlay_two_players_flip);
        rbPlay_two_players = (RadioButton) findViewById(R.id.rbPlay_two_players);
        cbPlay_newGame = (CheckBox) findViewById(R.id.cbPlay_newGame);
        cbPlay_newGame.setChecked(false);
        cbPlay_newGame.setVisibility(CheckBox.VISIBLE);
        if (playItem == 1) {rbPlay_white.setChecked(true);}
        if (playItem == 2) {rbPlay_black.setChecked(true);}
        if (playItem == 3) {rbPlay_engine.setChecked(true);}
        if (playItem == 4) {rbPlay_analysis.setChecked(true); cbPlay_newGame.setVisibility(CheckBox.INVISIBLE);}
        if (playItem == 5) {rbPlay_two_players_flip.setChecked(true);}
        if (playItem == 6) {rbPlay_two_players.setChecked(true);}
        rgPlay.setOnCheckedChangeListener(rgListener);
        setTitle(getString(R.string.app_optionsPlay));
	}
	@Override
    protected void onDestroy() 					
    {
    	if (progressDialog != null)
    	{
	    	if (progressDialog.isShowing())
	     		dismissDialog(START_PLAY_PROGRESS_DIALOG);
    	}
     	super.onDestroy();
    }
	public void myClickHandler(View view) 				
    {	// ClickHandler	(ButtonEvents)
		Intent returnIntent = new Intent();;
		switch (view.getId()) 
		{
			case R.id.btnPlay:
				setTitle(getString(R.string.engineProgressDialog));
				setPrefs();
	        	returnIntent.putExtra("newGame", cbPlay_newGame.isChecked());
	       		setResult(3, returnIntent);
				finish();
				break;
		}
	}
	private OnCheckedChangeListener rgListener = new OnCheckedChangeListener()		
	{	// Radio Button Listener
		@Override 
		public void onCheckedChanged(RadioGroup arg0, int checkedId) 
		{ 
			// Radio Buttons - select chessBoard
			cbPlay_newGame.setVisibility(CheckBox.VISIBLE);
			if (rbPlay_white.getId() == checkedId) 				
				playItem = 1;
			if (rbPlay_black.getId() == checkedId) 				
				playItem = 2;
			if (rbPlay_engine.getId() == checkedId) 				
				playItem = 3;
			if (rbPlay_analysis.getId() == checkedId) 
			{
				playItem = 4;
				cbPlay_newGame.setVisibility(CheckBox.INVISIBLE);
			}
			if (rbPlay_two_players_flip.getId() == checkedId) 				
				playItem = 5;
			if (rbPlay_two_players.getId() == checkedId) 				
				playItem = 6;
		} 
	};
	@Override
    protected Dialog onCreateDialog(int id) 
	{	// cancelled
		if (id == START_PLAY_PROGRESS_DIALOG) 
        {
        	progressDialog = new ProgressDialog(this);
        	
			progressDialog.setMessage(getString(R.string.engineProgressDialog));
	        progressDialog.setCancelable(true);
	        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() 
	        {
	            public void onCancel(DialogInterface dialog) 
	            {
	            	finish();
	            }
	        });
	        return progressDialog;
        } 
		return null;
	}
	protected void setPrefs() 
	{
		SharedPreferences.Editor ed = userP.edit();
        ed.putInt("user_play_playMod", playItem);
        ed.commit();
	}
	protected void getPrefs() 
	{
		playItem = userP.getInt("user_play_playMod", 1);
	}

	//	C4aMain RequestCode: OPTIONS_PLAY_REQUEST_CODE
	final String TAG = "OptionsPlay";
	SharedPreferences runP;
	SharedPreferences userP;
	ProgressDialog progressDialog = null;
	private static final int START_PLAY_PROGRESS_DIALOG = 1;
//	GUI	
	RadioGroup rgPlay;
	RadioButton rbPlay_white;
	RadioButton rbPlay_black;
	RadioButton rbPlay_engine;
	RadioButton rbPlay_analysis;
	RadioButton rbPlay_two_players;
	RadioButton rbPlay_two_players_flip;
	CheckBox cbPlay_newGame;
	int playItem = 1;
}
