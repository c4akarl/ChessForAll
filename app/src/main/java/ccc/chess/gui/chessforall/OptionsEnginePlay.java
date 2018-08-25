package ccc.chess.gui.chessforall;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

public class OptionsEnginePlay extends Activity implements TextWatcher
{
	public void onCreate(Bundle savedInstanceState) 
	{
        super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		u = new Util();
		userPrefs = getSharedPreferences("user", 0);
		runP = getSharedPreferences("run", 0);
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		start();
	}

	protected void start()
	{
		u.updateFullscreenStatus(this, userPrefs.getBoolean("user_options_gui_StatusBar", true));

		setContentView(R.layout.optionsengineplay);

		fileManagerIntent = new Intent(this, FileManager.class);
		engineMessage = (CheckBox) findViewById(R.id.cbEpEngineMessage);
		ponder = (CheckBox) findViewById(R.id.cbEpPonder);
		randomFirstMove = (CheckBox) findViewById(R.id.cbEpRandomFirstMove);
		autoStartEngine = (CheckBox) findViewById(R.id.cbEpAutoStartEngine);
		openingBook = (CheckBox) findViewById(R.id.cbEpOpeningBook);
		showBookHints = (CheckBox) findViewById(R.id.cbEpShowBookHints);
		epBook = (ImageView) findViewById(R.id.epBook);
		bookName = (EditText) findViewById(R.id.tvEpBookName);
		bookName.setHint(R.string.epBookHint);
		multiPv = (EditText) findViewById(R.id.etEpMultiPv);
		pvMoves = (EditText) findViewById(R.id.etEpPvMoves);
		displayedLines = (EditText) findViewById(R.id.etEpDisplayedLines);
		multiPv.addTextChangedListener(this);
		pvMoves.addTextChangedListener(this);
		displayedLines.addTextChangedListener(this);
		logOn = (CheckBox) findViewById(R.id.logOn);
		getPrefs();

		// strength (SeekBar)
		strengthValue = (TextView) this.findViewById(R.id.strengthValue);
		strengthValue.setText(progressStrength + "% " + getString(R.string.epStrength));
		strength = (SeekBar) this.findViewById(R.id.strength);
		strength.setProgress(progressStrength);
		strength.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
		{
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
			{
				progressStrength = progress;
				strengthValue.setText(progressStrength + "% " + getString(R.string.epStrength));
			}
			public void onStartTrackingTouch(SeekBar arg0) { }
			public void onStopTrackingTouch(SeekBar seekBar) { }
		});

		// aggressiveness (SeekBar)
		aggressivenessValue = (TextView) this.findViewById(R.id.aggressivenessValue);
		if (progressAggressiveness - 100 >= 0)
			aggressivenessValue.setText((progressAggressiveness - 100) + " " + getString(R.string.epAggressivness));
		else
			aggressivenessValue.setText((progressAggressiveness - 100) + " " + getString(R.string.epPassivity));
		aggressiveness = (SeekBar) this.findViewById(R.id.aggressiveness);
		aggressiveness.setProgress(progressAggressiveness);
		aggressiveness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
		{
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
			{
				progressAggressiveness = progress;
				if (progressAggressiveness - 100 >= 0)
					aggressivenessValue.setText((progressAggressiveness - 100) + " " + getString(R.string.epAggressivness));
				else
					aggressivenessValue.setText((progressAggressiveness - 100) + " " + getString(R.string.epPassivity));
			}
			public void onStartTrackingTouch(SeekBar arg0) { }
			public void onStopTrackingTouch(SeekBar seekBar) { }
		});
	}

	public void myClickHandler(View view) 				
    {	// ClickHandler	(ButtonEvents)
		Intent returnIntent;
		switch (view.getId()) 
		{
		case R.id.btnGuOk:
			setPrefs();
        	returnIntent = new Intent();
       		setResult(3, returnIntent);
			finish();
			break;
		case R.id.epBook:
		case R.id.tvEpBookName:
			fileManagerIntent.putExtra("fileActionCode", LOAD_OPENING_BOOK_REQUEST_CODE);
	    	fileManagerIntent.putExtra("displayActivity", 1);
	    	this.startActivityForResult(fileManagerIntent, LOAD_OPENING_BOOK_REQUEST_CODE);
			break;
		}
	}
	@Override
	public void afterTextChanged(Editable s) 
	{
		try
		{
			if (multiPv.hasFocus() == true)
			{
				if (s.toString().equals("0"))
					multiPv.setText("1");
				if (Integer.parseInt(s.toString()) > 4)
					multiPv.setText("4");
			}
			if (pvMoves.hasFocus() == true)
			{
				if (s.toString().equals("0"))
					pvMoves.setText("1");
				if (Integer.parseInt(s.toString()) > 30)
					pvMoves.setText("30");
			}
		}
		catch 	(NumberFormatException e) {	}
	}
	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,	int after) { }
	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) { }
	public void onActivityResult(int requestCode, int resultCode, Intent data)			
    {
		switch(requestCode)
	    {
		    case LOAD_OPENING_BOOK_REQUEST_CODE: 
		    	if (resultCode == RESULT_OK)
				{
					if (!data.getStringExtra("fileName").endsWith(".bin"))
						bookName.setText("");
					else
						bookName.setText(data.getStringExtra("filePath") + data.getStringExtra("fileName"));
					bookName.setSelection(bookName.getText().length());
					setPrefs();
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
        ed.putBoolean("user_options_enginePlay_EngineMessage", engineMessage.isChecked());
        ed.putBoolean("user_options_enginePlay_Ponder", ponder.isChecked());
        ed.putBoolean("user_options_enginePlay_RandomFirstMove", randomFirstMove.isChecked());
        ed.putBoolean("user_options_enginePlay_AutoStartEngine", autoStartEngine.isChecked());
        ed.putBoolean("user_options_enginePlay_OpeningBook", openingBook.isChecked());
        ed.putBoolean("user_options_enginePlay_ShowBookHints", showBookHints.isChecked());
        ed.putString("user_options_enginePlay_OpeningBookName", bookName.getText().toString());
        int multiPv = PV_MULTI;
        try {multiPv = Integer.parseInt(this.multiPv.getText().toString());}
        catch 	(NumberFormatException e) {};
        ed.putInt("user_options_enginePlay_MultiPv", multiPv);
        int pvMoves = PV_MOVES;
        try {pvMoves = Integer.parseInt(this.pvMoves.getText().toString());}
        catch 	(NumberFormatException e) {};
        ed.putInt("user_options_enginePlay_PvMoves", pvMoves);
		int lines = DISPLAYED_LINES;
		try {lines = Integer.parseInt(this.displayedLines.getText().toString());}
		catch 	(NumberFormatException e) {};
		ed.putInt("user_options_enginePlay_displayedLines", lines);
		ed.putInt("user_options_enginePlay_strength", progressStrength);
		ed.putInt("user_options_enginePlay_aggressiveness", progressAggressiveness - 100);
		ed.putBoolean("user_options_enginePlay_logOn", logOn.isChecked());
        ed.commit();
	}
	protected void getPrefs() 
	{
		engineMessage.setChecked(userPrefs.getBoolean("user_options_enginePlay_EngineMessage", true));
		ponder.setChecked(userPrefs.getBoolean("user_options_enginePlay_Ponder", true));
		randomFirstMove.setChecked(userPrefs.getBoolean("user_options_enginePlay_RandomFirstMove", false));
		autoStartEngine.setChecked(userPrefs.getBoolean("user_options_enginePlay_AutoStartEngine", true));
		openingBook.setChecked(userPrefs.getBoolean("user_options_enginePlay_OpeningBook", true));
		showBookHints.setChecked(userPrefs.getBoolean("user_options_enginePlay_ShowBookHints", true));
		bookName.setText(userPrefs.getString("user_options_enginePlay_OpeningBookName", ""));
		bookName.setSelection(bookName.getText().length());
		multiPv.setText(Integer.toString(userPrefs.getInt("user_options_enginePlay_MultiPv", PV_MULTI)));
		pvMoves.setText(Integer.toString(userPrefs.getInt("user_options_enginePlay_PvMoves", PV_MOVES)));
		displayedLines.setText(Integer.toString(userPrefs.getInt("user_options_enginePlay_displayedLines", DISPLAYED_LINES)));
		progressStrength = userPrefs.getInt("user_options_enginePlay_strength", 100);
		progressAggressiveness = userPrefs.getInt("user_options_enginePlay_aggressiveness", 0) + 100;
		logOn.setChecked(userPrefs.getBoolean("user_options_enginePlay_logOn", false));
	}

	final String TAG = "PlaySettings";
	Util u;
	final static int LOAD_OPENING_BOOK_REQUEST_CODE = 91;
	final static int PV_MULTI = 2;
	final static int PV_MOVES = 8;
	final static int DISPLAYED_LINES = 3;
	Intent fileManagerIntent;
	SharedPreferences userPrefs;
	SharedPreferences runP;
//	GUI	
	CheckBox engineMessage;
	CheckBox ponder;
	CheckBox randomFirstMove;
	CheckBox autoStartEngine;
	CheckBox openingBook;
	CheckBox showBookHints;
	ImageView epBook = null;
	EditText bookName = null;
	EditText multiPv = null;
	EditText pvMoves = null;
	EditText displayedLines = null;
	SeekBar strength;
	TextView strengthValue;
	SeekBar aggressiveness;
	TextView aggressivenessValue;
	CheckBox logOn;

	int progressStrength = 100;
	int progressAggressiveness = 0;	// uci option contempt, min: -100, max: +100, default: 0 (Stockfish)

}
