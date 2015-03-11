package ccc.chess.gui.chessforall;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

public class OptionsChessBoard extends Activity
{
//	C4aMain RequestCode: OPTIONS_CHESSBOARD_REQUEST_CODE
	final String TAG = "PlaySettings";
	SharedPreferences userPrefs;
	int chessBoard = 1;
//	GUI	
	RadioGroup rgChessBoard;
	RadioButton rbCbC4a;
	RadioButton rbCbStandard;
	RadioButton rbCbGreen;
	RadioButton rbCbBrown;
	public void onCreate(Bundle savedInstanceState) 
	{
        super.onCreate(savedInstanceState);
        setContentView(R.layout.optionschessboard);
        userPrefs = getSharedPreferences("user", 0);
        getPrefs();
        rgChessBoard = (RadioGroup) findViewById(R.id.rgChessBoard); 
        rbCbC4a = (RadioButton) findViewById(R.id.rbCbC4a); 
        rbCbStandard = (RadioButton) findViewById(R.id.rbCbStandard); 
        rbCbGreen = (RadioButton) findViewById(R.id.rbCbGreen); 
        rbCbBrown = (RadioButton) findViewById(R.id.rbCbBrown);
        if (chessBoard == 1) {rbCbC4a.setChecked(true);}
        if (chessBoard == 2) {rbCbStandard.setChecked(true);}
        if (chessBoard == 3) {rbCbGreen.setChecked(true);}
        if (chessBoard == 4) {rbCbBrown.setChecked(true);}
        rgChessBoard.setOnCheckedChangeListener(rgListener);
        setTitle(getString(R.string.app_optionsChessBoard));
	}
	public void myClickHandler(View view) 				
    {	// ClickHandler	(ButtonEvents)
		Intent returnIntent;
		switch (view.getId()) 
		{
		case R.id.btnCbOk:
			setPrefs();
        	returnIntent = new Intent();
       		setResult(RESULT_OK, returnIntent);
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
			if (rbCbC4a.getId() == checkedId) 				
				chessBoard = 1;
			if (rbCbStandard.getId() == checkedId) 				
				chessBoard = 2;
			if (rbCbGreen.getId() == checkedId) 				
				chessBoard = 3;
			if (rbCbBrown.getId() == checkedId) 				
				chessBoard = 4;
		} 
	};
	protected void setPrefs() 
	{
		SharedPreferences.Editor ed = userPrefs.edit();
        ed.putInt("user_options_chessBoard", chessBoard);
        ed.commit();
	}
	protected void getPrefs() 
	{
		chessBoard = userPrefs.getInt("user_options_chessBoard", 1);
	}
 }
