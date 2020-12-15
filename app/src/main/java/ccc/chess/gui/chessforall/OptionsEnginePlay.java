package ccc.chess.gui.chessforall;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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

//public class OptionsEnginePlay extends Activity implements TextWatcher, Ic4aDialogCallback
public class OptionsEnginePlay extends Activity implements Ic4aDialogCallback
{
	public void onCreate(Bundle savedInstanceState) 
	{
        super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		u = new Util();
		fileIO = new FileIO(this);
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
		u.updateFullscreenStatus(this, userPrefs.getBoolean("user_options_gui_StatusBar", false));

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
//		multiPv = (EditText) findViewById(R.id.etEpMultiPv);
//		pvMoves = (EditText) findViewById(R.id.etEpPvMoves);
//		displayedLines = (EditText) findViewById(R.id.etEpDisplayedLines);
//		multiPv.addTextChangedListener(this);
//		pvMoves.addTextChangedListener(this);
//		displayedLines.addTextChangedListener(this);
		debugInformation = (CheckBox) findViewById(R.id.debugInformation);
		logOn = (CheckBox) findViewById(R.id.logOn);
		getPrefs();

		variantsMinus = findViewById(R.id.variantsMinus);
		variantsMinus.setOnClickListener(v -> {
			variants--;
			setVariants();
		});
		variantsValue = findViewById(R.id.variantsValue);
//		variantsValue.setBackgroundResource(R.drawable.rectangleyellow);
		setTextViewColors(variantsValue, "#efe395");
		variantsValue.setOnClickListener(v -> {
			variants = VARIANTS_DEFAULT;
			setVariants();
		});
		variantsValue.setText(Integer.toString(variants));
		variantsPlus = findViewById(R.id.variantsPlus);
		variantsPlus.setOnClickListener(v -> {
			variants++;
			setVariants();
		});
		setVariants();

		movesMinus = findViewById(R.id.movesMinus);
		movesMinus.setOnClickListener(v -> {
			moves--;
			setMoves();
		});
		movesValue = findViewById(R.id.movesValue);
//		movesValue.setBackgroundResource(R.drawable.rectangleyellow);
		setTextViewColors(movesValue, "#efe395");
		movesValue.setOnClickListener(v -> {
			moves = MOVES_DEFAULT;
			setMoves();
		});
		movesValue.setText(Integer.toString(moves));
		movesPlus = findViewById(R.id.movesPlus);
		movesPlus.setOnClickListener(v -> {
			moves++;
			setMoves();
		});
		setMoves();

		linesMinus = findViewById(R.id.linesMinus);
		linesMinus.setOnClickListener(v -> {
			lines--;
			setLines();
		});
		linesValue = findViewById(R.id.linesValue);
//		linesValue.setBackgroundResource(R.drawable.rectangleyellow);
		setTextViewColors(linesValue, "#efe395");
		linesValue.setOnClickListener(v -> {
			lines = LINES_DEFAULT;
			setLines();
		});
		linesValue.setText(Integer.toString(lines));
		linesPlus = findViewById(R.id.linesPlus);
		linesPlus.setOnClickListener(v -> {
			lines++;
			setLines();
		});
		setLines();

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

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{

//		Log.i(TAG, "onConfigurationChanged()");

		super.onConfigurationChanged(newConfig);
		setPrefs();
	}

	public void setVariants()
	{
//		variantsValue.setBackgroundResource(R.drawable.rectangleyellow);
		setTextViewColors(variantsValue, "#efe395");
//		variantsMinus.setBackgroundResource(R.drawable.rectanglepink);
		setTextViewColors(variantsMinus, "#f6d2f4");
//		variantsPlus.setBackgroundResource(R.drawable.rectanglegreen);
		setTextViewColors(variantsPlus, "#c4f8c0");
		if (variants <= VARIANTS_MIN) {
			variants = VARIANTS_MIN;
//			variantsMinus.setBackgroundResource(R.drawable.rectanglegrey);
			setTextViewColors(variantsMinus, "#767a76");
		}
		if (variants >= VARIANTS_MAX) {
			variants = VARIANTS_MAX;
//			variantsPlus.setBackgroundResource(R.drawable.rectanglegrey);
			setTextViewColors(variantsPlus, "#767a76");
		}
		variantsValue.setText(Integer.toString(variants));
	}

	public void setMoves()
	{
//		movesValue.setBackgroundResource(R.drawable.rectangleyellow);
		setTextViewColors(movesValue, "#efe395");
//		movesMinus.setBackgroundResource(R.drawable.rectanglepink);
		setTextViewColors(movesMinus, "#f6d2f4");
//		movesPlus.setBackgroundResource(R.drawable.rectanglegreen);
		setTextViewColors(movesPlus, "#c4f8c0");
		if (moves <= MOVES_MIN) {
			moves = MOVES_MIN;
//			movesMinus.setBackgroundResource(R.drawable.rectanglegrey);
			setTextViewColors(movesMinus, "#767a76");
		}
		if (moves >= MOVES_MAX) {
			moves = MOVES_MAX;
//			movesPlus.setBackgroundResource(R.drawable.rectanglegrey);
			setTextViewColors(movesPlus, "#767a76");
		}
		movesValue.setText(Integer.toString(moves));
	}

	public void setLines()
	{
//		linesValue.setBackgroundResource(R.drawable.rectangleyellow);
		setTextViewColors(linesValue, "#efe395");
//		linesMinus.setBackgroundResource(R.drawable.rectanglepink);
		setTextViewColors(linesMinus, "#f6d2f4");
//		linesPlus.setBackgroundResource(R.drawable.rectanglegreen);
		setTextViewColors(linesPlus, "#c4f8c0");
		if (lines <= LINES_MIN) {
			lines = LINES_MIN;
//			linesMinus.setBackgroundResource(R.drawable.rectanglegrey);
			setTextViewColors(linesMinus, "#767a76");
		}
		if (lines >= LINES_MAX) {
			lines = LINES_MAX;
//			linesPlus.setBackgroundResource(R.drawable.rectanglegrey);
			setTextViewColors(linesPlus, "#767a76");
		}
		linesValue.setText(Integer.toString(lines));
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
			if (fileIO.isSdk30()) {
				removeDialog(NO_FILE_ACTIONS_DIALOG);
				showDialog(NO_FILE_ACTIONS_DIALOG);
				return;
			}
			fileManagerIntent.putExtra("fileActionCode", LOAD_OPENING_BOOK_REQUEST_CODE);
	    	fileManagerIntent.putExtra("displayActivity", 1);
	    	this.startActivityForResult(fileManagerIntent, LOAD_OPENING_BOOK_REQUEST_CODE);
			break;
		}
	}

	@Override
	protected Dialog onCreateDialog(int id)
	{
		if (id == NO_FILE_ACTIONS_DIALOG)
		{
			c4aDialog = new C4aDialog(this, this, getString(R.string.dgTitleDialog),
					"", getString(R.string.btn_Ok), "", getString(R.string.noFileActions), 0, "");
			return c4aDialog;
		}
		return null;
	}

	@Override
	public void getCallbackValue(int btnValue)
	{

	}

//	@Override
//	public void afterTextChanged(Editable s)
//	{
//		try
//		{
//			if (multiPv.hasFocus() == true)
//			{
//				if (s.toString().equals("0"))
//					multiPv.setText("1");
//				if (Integer.parseInt(s.toString()) > 4)
//					multiPv.setText("4");
//			}
//			if (pvMoves.hasFocus() == true)
//			{
//				if (s.toString().equals("0"))
//					pvMoves.setText("1");
//				if (Integer.parseInt(s.toString()) > 30)
//					pvMoves.setText("30");
//			}
//		}
//		catch 	(NumberFormatException e) {	}
//	}
//	@Override
//	public void beforeTextChanged(CharSequence s, int start, int count,	int after) { }
//	@Override
//	public void onTextChanged(CharSequence s, int start, int before, int count) { }

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

	public void setTextViewColors(TextView tv, String color)
	{
		GradientDrawable tvBackground = (GradientDrawable) tv.getBackground();
		tvBackground.setColor(Color.parseColor(color));
	}

	protected void setPrefs() 
	{
		SharedPreferences.Editor ed = userPrefs.edit();
		ed.putInt("user_options_enginePlay_MultiPv", variants);
		ed.putInt("user_options_enginePlay_PvMoves", moves);
		ed.putInt("user_options_enginePlay_displayedLines", lines);
        ed.putBoolean("user_options_enginePlay_EngineMessage", engineMessage.isChecked());
        ed.putBoolean("user_options_enginePlay_Ponder", ponder.isChecked());
        ed.putBoolean("user_options_enginePlay_RandomFirstMove", randomFirstMove.isChecked());
        ed.putBoolean("user_options_enginePlay_AutoStartEngine", autoStartEngine.isChecked());
        ed.putBoolean("user_options_enginePlay_OpeningBook", openingBook.isChecked());
        ed.putBoolean("user_options_enginePlay_ShowBookHints", showBookHints.isChecked());
        ed.putString("user_options_enginePlay_OpeningBookName", bookName.getText().toString());

//        int multiPv = PV_MULTI;
//        try {multiPv = Integer.parseInt(this.multiPv.getText().toString());}
//        catch 	(NumberFormatException e) {};
//        ed.putInt("user_options_enginePlay_MultiPv", multiPv);
//        int pvMoves = PV_MOVES;
//        try {pvMoves = Integer.parseInt(this.pvMoves.getText().toString());}
//        catch 	(NumberFormatException e) {};
//        ed.putInt("user_options_enginePlay_PvMoves", pvMoves);
//		int lines = DISPLAYED_LINES;
//		try {lines = Integer.parseInt(this.displayedLines.getText().toString());}
//		catch 	(NumberFormatException e) {};
//		ed.putInt("user_options_enginePlay_displayedLines", lines);

		ed.putInt("user_options_enginePlay_strength", progressStrength);
		ed.putInt("user_options_enginePlay_aggressiveness", progressAggressiveness - 100);
		ed.putBoolean("user_options_enginePlay_debugInformation", debugInformation.isChecked());
		ed.putBoolean("user_options_enginePlay_logOn", logOn.isChecked());
        ed.commit();
	}
	protected void getPrefs() 
	{
		variants = userPrefs.getInt("user_options_enginePlay_MultiPv", variants);
		moves = userPrefs.getInt("user_options_enginePlay_PvMoves", moves);
		lines = userPrefs.getInt("user_options_enginePlay_displayedLines", lines);
		engineMessage.setChecked(userPrefs.getBoolean("user_options_enginePlay_EngineMessage", true));
		ponder.setChecked(userPrefs.getBoolean("user_options_enginePlay_Ponder", false));
		randomFirstMove.setChecked(userPrefs.getBoolean("user_options_enginePlay_RandomFirstMove", false));
		autoStartEngine.setChecked(userPrefs.getBoolean("user_options_enginePlay_AutoStartEngine", true));
		openingBook.setChecked(userPrefs.getBoolean("user_options_enginePlay_OpeningBook", true));
		showBookHints.setChecked(userPrefs.getBoolean("user_options_enginePlay_ShowBookHints", true));
		bookName.setText(userPrefs.getString("user_options_enginePlay_OpeningBookName", ""));
		bookName.setSelection(bookName.getText().length());

//		multiPv.setText(Integer.toString(userPrefs.getInt("user_options_enginePlay_MultiPv", PV_MULTI)));
//		pvMoves.setText(Integer.toString(userPrefs.getInt("user_options_enginePlay_PvMoves", PV_MOVES)));
//		displayedLines.setText(Integer.toString(userPrefs.getInt("user_options_enginePlay_displayedLines", DISPLAYED_LINES)));

		progressStrength = userPrefs.getInt("user_options_enginePlay_strength", 100);
		progressAggressiveness = userPrefs.getInt("user_options_enginePlay_aggressiveness", 0) + 100;
		debugInformation.setChecked(userPrefs.getBoolean("user_options_enginePlay_debugInformation", false));
		logOn.setChecked(userPrefs.getBoolean("user_options_enginePlay_logOn", false));
	}

	final String TAG = "PlaySettings";
	Util u;
	FileIO fileIO;
	final static int LOAD_OPENING_BOOK_REQUEST_CODE = 91;
	final static int NO_FILE_ACTIONS_DIALOG = 193;
	final static int PV_MULTI = 1;
	final static int PV_MOVES = 16;
	final static int DISPLAYED_LINES = 3;
	Intent fileManagerIntent;
	SharedPreferences userPrefs;
	SharedPreferences runP;
	C4aDialog c4aDialog;
	final static int VARIANTS_DEFAULT = 1;
	int VARIANTS_MIN = 1;
	int VARIANTS_MAX = 4;
	int variants = VARIANTS_DEFAULT;
	final static int MOVES_DEFAULT = 16;
	int MOVES_MIN = 1;
	int MOVES_MAX = 30;
	int moves = MOVES_DEFAULT;
	final static int LINES_DEFAULT = 3;
	int LINES_MIN = 1;
	int LINES_MAX = 9;
	int lines = LINES_DEFAULT;
//	GUI
	TextView variantsMinus;
	TextView variantsValue;
	TextView variantsPlus;
	TextView movesMinus;
	TextView movesValue;
	TextView movesPlus;
	TextView linesMinus;
	TextView linesValue;
	TextView linesPlus;
	CheckBox engineMessage;
	CheckBox ponder;
	CheckBox randomFirstMove;
	CheckBox autoStartEngine;
	CheckBox openingBook;
	CheckBox showBookHints;
	ImageView epBook = null;
	EditText bookName = null;
//	EditText multiPv = null;
//	EditText pvMoves = null;
//	EditText displayedLines = null;
	SeekBar strength;
	TextView strengthValue;
	SeekBar aggressiveness;
	TextView aggressivenessValue;
	CheckBox debugInformation;
	CheckBox logOn;

	int progressStrength = 100;
	int progressAggressiveness = 0;	// uci option contempt, min: -100, max: +100, default: 0 (Stockfish)

}
