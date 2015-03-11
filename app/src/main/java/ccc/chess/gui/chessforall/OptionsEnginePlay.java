package ccc.chess.gui.chessforall;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Bitmap.Config;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

public class OptionsEnginePlay extends Activity implements TextWatcher
{
	public void onCreate(Bundle savedInstanceState) 
	{
        super.onCreate(savedInstanceState);
        setContentView(R.layout.optionsengineplay);
        userPrefs = getSharedPreferences("user", 0);
        runP = getSharedPreferences("run", 0);
        fileManagerIntent = new Intent(this, PgnFileManager.class);
        cbEpEngineMessage = (CheckBox) findViewById(R.id.cbEpEngineMessage); 
        cbEpRandomFirstMove = (CheckBox) findViewById(R.id.cbEpRandomFirstMove); 
        cbEpAutoStartEngine = (CheckBox) findViewById(R.id.cbEpAutoStartEngine); 
        cbEpOpeningBook = (CheckBox) findViewById(R.id.cbEpOpeningBook);
        epBook = (ImageView) findViewById(R.id.epBook);
        epBook.setImageBitmap(combineImages(R.drawable.btn_b2, R.drawable.btn_pgn_load, 0));
        tvEpBookName = (TextView) findViewById(R.id.tvEpBookName);
        etEpMultiPv = (EditText) findViewById(R.id.etEpMultiPv);
        etEpPvMoves = (EditText) findViewById(R.id.etEpPvMoves);
        etEpMultiPv.addTextChangedListener(this);
        etEpPvMoves.addTextChangedListener(this);
        getPrefs();
        setTitle(getString(R.string.app_optionsEnginePlay));
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
		case R.id.epBook:
		case R.id.tvEpBookName:
			fileManagerIntent.putExtra("fileActionCode", 91);
	    	fileManagerIntent.putExtra("displayActivity", 1);
	    	this.startActivityForResult(fileManagerIntent, 91);
			break;
		}
	}
	@Override
	public void afterTextChanged(Editable s) 
	{
		try
		{
			if (etEpMultiPv.hasFocus() == true)
			{
				if (s.toString().equals("0"))
					etEpMultiPv.setText("1");
				if (Integer.parseInt(s.toString()) > 4)
					etEpMultiPv.setText("4");
			}
			if (etEpPvMoves.hasFocus() == true)
			{
				if (s.toString().equals("0"))
					etEpPvMoves.setText("1");
				if (Integer.parseInt(s.toString()) > 30)
					etEpPvMoves.setText("30");
			}
		}
		catch 	(NumberFormatException e) {	}
	}
	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,	int after) { }
	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) { }
	public void onActivityResult(int requestCode, int resultCode, Intent data)			
    {	// subActivity result
		switch(requestCode) 
	    {
		    case LOAD_OPENING_BOOK_REQUEST_CODE: 
		    	if (resultCode == C4aMain.RESULT_OK) 					
				{
					tvEpBookName.setText(data.getStringExtra("filePath") + data.getStringExtra("fileName"));
				}
				break;
	    }
    }
	public Bitmap combineImages(int image1, int image2, int image3) 
	{	// paint over ImageView
    	Bitmap drawBitmap = null;
    	try
    	{
	    	Bitmap image1Bitmap = BitmapFactory.decodeResource(getResources(), image1);
	    	Bitmap image2Bitmap = BitmapFactory.decodeResource(getResources(), image2);
	    	Bitmap image3Bitmap = null;
	    	if (image3 != 0)
	    		image3Bitmap = BitmapFactory.decodeResource(getResources(), image3);
	    	drawBitmap = Bitmap.createBitmap(image1Bitmap.getWidth(), image1Bitmap.getHeight(), Config.ARGB_8888);
			Canvas canvas = new Canvas(drawBitmap);
			canvas.drawBitmap(image1Bitmap, 0, 0, null);
			canvas.drawBitmap(image2Bitmap, 0, 0, null);
			if (image3 != 0)
				canvas.drawBitmap(image3Bitmap, 0, 0, null);
    	}
		catch (NullPointerException e) {e.printStackTrace();}
    	return drawBitmap;
	}
	protected void setPrefs() 
	{
		SharedPreferences.Editor ed = userPrefs.edit();
        ed.putBoolean("user_options_enginePlay_EngineMessage", cbEpEngineMessage.isChecked());
        ed.putBoolean("user_options_enginePlay_RandomFirstMove", cbEpRandomFirstMove.isChecked());
        ed.putBoolean("user_options_enginePlay_AutoStartEngine", cbEpAutoStartEngine.isChecked());
        ed.putBoolean("user_options_enginePlay_OpeningBook", cbEpOpeningBook.isChecked());
        ed.putString("user_options_enginePlay_OpeningBookName", tvEpBookName.getText().toString());
        int multiPv = 5;
        try {multiPv = Integer.parseInt(etEpMultiPv.getText().toString());}
        catch 	(NumberFormatException e) {};
        ed.putInt("user_options_enginePlay_MultiPv", multiPv);
        int pvMoves = 5;
        try {pvMoves = Integer.parseInt(etEpPvMoves.getText().toString());}
        catch 	(NumberFormatException e) {};
        ed.putInt("user_options_enginePlay_PvMoves", pvMoves);
        ed.commit();
	}
	protected void getPrefs() 
	{
		cbEpEngineMessage.setChecked(userPrefs.getBoolean("user_options_enginePlay_EngineMessage", true));
		cbEpRandomFirstMove.setChecked(userPrefs.getBoolean("user_options_enginePlay_RandomFirstMove", false));
		cbEpAutoStartEngine.setChecked(userPrefs.getBoolean("user_options_enginePlay_AutoStartEngine", true));
		cbEpOpeningBook.setChecked(userPrefs.getBoolean("user_options_enginePlay_OpeningBook", true));
		tvEpBookName.setText(userPrefs.getString("user_options_enginePlay_OpeningBookName", ""));
		etEpMultiPv.setText(Integer.toString(userPrefs.getInt("user_options_enginePlay_MultiPv", 4)));
		etEpPvMoves.setText(Integer.toString(userPrefs.getInt("user_options_enginePlay_PvMoves", 30)));
	}
	
	final String TAG = "PlaySettings";
	final static int LOAD_OPENING_BOOK_REQUEST_CODE = 91;
	Intent fileManagerIntent;
	SharedPreferences userPrefs;
	SharedPreferences runP;
//	GUI	
	CheckBox cbEpEngineMessage;
	CheckBox cbEpRandomFirstMove;
	CheckBox cbEpAutoStartEngine;
	CheckBox cbEpOpeningBook;
	ImageView epBook = null;
	TextView tvEpBookName = null;
	EditText etEpMultiPv = null;
	EditText etEpPvMoves = null;
}
