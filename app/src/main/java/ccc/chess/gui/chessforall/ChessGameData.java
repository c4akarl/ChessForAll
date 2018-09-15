package ccc.chess.gui.chessforall;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;

import ccc.chess.logic.c4aservice.ChessHistory;

public class ChessGameData extends Activity implements OnTouchListener, OnItemSelectedListener, TextWatcher
{
	@SuppressLint("UseSparseArrays")
	public void onCreate(Bundle savedInstanceState) 
	{
        super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		u = new Util();
        _gameStat = getIntent().getExtras().getCharSequence("gameStat");
        _gameTags = getIntent().getExtras().getCharSequence("gameTags");
		userP = getSharedPreferences("user", 0);
		u.updateFullscreenStatus(this, userP.getBoolean("user_options_gui_StatusBar", false));
        setContentView(R.layout.gamedata);
        String state = getString(R.string.menu_modes_edit);
        if (_gameStat.equals("2"))
        	state = getString(R.string.menu_modes_view);
        title = (TextView) findViewById(R.id.title);
        title.setText(getString(R.string.app_chessData) + " (" + state + ")");
        gdBtnOk = (ImageView) findViewById(R.id.gdBtnOk);
        gdBtnTags = (ImageView) findViewById(R.id.gdBtnTags);
        if (_gameStat.equals("2"))
        	gdBtnTags.setVisibility(ImageView.INVISIBLE);
        rl = (RelativeLayout)findViewById(R.id.gdRelativeLayout);
        history = new ChessHistory();
        tItems = new CharSequence[history.tagState.length];
    	tState = new boolean[history.tagState.length];
        setGameTagsToView();
        mSoundPool = new SoundPool(2, AudioManager.STREAM_RING, 100);
        soundsMap = new HashMap<Integer, Integer>();
        soundsMap.put(1, mSoundPool.load(this, R.raw.move_wrong, 1));
	}

	public void setGameTagsToView()
    {
		int prevId = 0;
		getDateData();
		setGameTagsStats();
		String[] txtSplit = _gameTags.toString().split("\n");
		_gameTagsCount = txtSplit.length;
		tagError = new Integer[_gameTagsCount];
		for(int i = 0; i < _gameTagsCount; i++)
	    {
			tagError[i] = 0;
			if (txtSplit[i].contains("\b"))
			{
				String[] txtTags = txtSplit[i].split("\b");
				String name = txtTags[0];
				String value = txtTags[1];
				String defaultValue = txtTags[2];
				int type = 0;
				try	{type = Integer.parseInt(txtTags[3].toString());} catch (NumberFormatException e) {type = 0;}
				int digits = 0;
				try	{digits = Integer.parseInt(txtTags[4].toString());} catch (NumberFormatException e) {digits = 0;}
//Log.i(TAG, "name, value, type, digits: " + name + ", " + value + ", " + type + ", " + digits);
				RelativeLayout.LayoutParams rPar = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				TextView tv = (TextView)getLayoutInflater().inflate(R.layout.c4atextview, null);
				EditText et = null;
				et = (EditText)getLayoutInflater().inflate(R.layout.c4aedittext, null);
				rPar.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
				rPar.setMargins(4, 4, 4, 4);
				if (i > 0)	rPar.addRule(RelativeLayout.BELOW, prevId);
				tv.setText(name);
				tv.setLayoutParams(rPar);
				tv.setId(i +100);
				if (type == 9)				// result
					rPar = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
				else
					rPar = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
				if (i > 0)	rPar.addRule(RelativeLayout.BELOW, prevId);
				if (_gameStat.equals("2"))
					et.setFocusable(false);
				else
				{
					et.setHint(defaultValue);
					if (type == 1)				// integer
						et.setInputType(InputType.TYPE_CLASS_NUMBER);
					if (type == 2)				// date
					{
						et.setInputType(InputType.TYPE_TEXT_VARIATION_FILTER);
						et.addTextChangedListener(this);
					}
					if (type == 3)				// time	(20120909 | 1:47:15)
					{
						et.setInputType(InputType.TYPE_CLASS_NUMBER);
						et.addTextChangedListener(this);
					}
					if (type == 4)				// clock	(W/2:00:00)
						et.addTextChangedListener(this);
					if (type == 9)				// result
					{
						et.setOnTouchListener((OnTouchListener) this);
						et.setFocusable(false);
						et.setMinWidth(100);
					}
					if (digits > 0)
						et.setFilters(new InputFilter[] { new InputFilter.LengthFilter(digits) });
				}
				if (value.equals(""))
					value = history.getInitValueFromTag(name);
				et.setText(value);
				if (name.equals("FEN") | name.equals("Variant"))
					et.setFocusable(false);
				rPar.addRule(RelativeLayout.RIGHT_OF, tv.getId());
				rPar.setMargins(0, 4, 4, 4);
				et.setLayoutParams(rPar);
				et.setId(i +200);
				prevId = i +200;
				rl.addView(tv);
				rl.addView(et);
			}
	    }
    }
	public void setGameTagsStats() 		
    {
		if (initTagState)
		{
			for(int i = 0; i < history.tagState.length; i++)
		    {	// used for tagList (TAG_LIST_DIALOG)
				String[] txtTagStats = history.tagState[i].toString().split("\b");
				tItems[i] = txtTagStats[0];
				if (i < 7)
					tState[i] = true;
				else
					tState[i] = false;
				
		    }
		}
		CharSequence	tmpTags = "";
		boolean isTagState = false;
		_gameTags = _gameTags.toString().replace("\r", "\n");
		String[] txtSplit = _gameTags.toString().split("\n");
		for(int i = 0; i < txtSplit.length; i++)
	    {
			isTagState = false;
			String[] txtTags = txtSplit[i].split("\b");
			if (txtTags.length == 2)
			{
				for(int h = 0; h < history.tagState.length; h++)
			    {
					String[] txtTagStats = history.tagState[h].toString().split("\b");
//Log.i(TAG, "txtTagStats[0], txtTags[0]: " + txtTagStats[0]+ ", " + txtTags[0]);
					if (txtTagStats[0].equals(txtTags[0]))
					{
						if (initTagState)
							tState[h] = true;
						tmpTags = tmpTags +txtSplit[i] +"\b" +txtTagStats[1] +"\b" +txtTagStats[2] +"\b" +txtTagStats[3] +"\n";
						isTagState = true;
						break;
					}
			    }
			}
			if (!isTagState)
				tmpTags = tmpTags +txtSplit[i] +"\b?\b0\b0\n";	// default: text
	    }
		initTagState = false;
		_gameTags = tmpTags;
    }
	public CharSequence getGameTagsFromView() 		
    {
		CharSequence newTags = "";
		String[] txtSplit = _gameTags.toString().split("\n");
		for(int i = 0; i < _gameTagsCount; i++)
	    {
			TextView tv = (TextView) rl.findViewById(i +100);
			EditText et = (EditText) rl.findViewById(i +200);
			String[] txtTagStats = txtSplit[i].toString().split("\b");
			String tagValue = et.getText().toString();
			if (tagValue.equals(""))
				tagValue = txtTagStats[2];
//Log.i(TAG, "tags: " + tv.getText().toString() + ", " + et.getText().toString());
			newTags = newTags + tv.getText().toString() + "\b" + tagValue + "\n";
	    }
		return newTags;
    }
	public void myClickHandler(View view) 		
    {	// ClickHandler	(ButtonEvents)
		switch (view.getId()) 
		{
			case R.id.gdBtnOk:
				returnIntent.putExtra("gameTags", getGameTagsFromView());
				setResult(RESULT_OK, returnIntent);
				finish();
				break;
			case R.id.gdBtnTags:
				removeDialog(TAG_LIST_DIALOG);
				showDialog(TAG_LIST_DIALOG);
				break;
		}
	}
	@Override
	public void afterTextChanged(Editable arg0) 
	{
		EditText et = null;
		try {et = (EditText) rl.findViewById(getCurrentFocus().getId());}
		catch (NullPointerException e) {return;}
		String data = et.getText().toString();
		String[] txtSplit = _gameTags.toString().split("\n");
		int resId = et.getId(); resId -= 200;
		String[] txtTags = txtSplit[resId].split("\b");
		int etType = 0;
		try	{etType = Integer.parseInt(txtTags[3].toString());} catch (NumberFormatException e) {etType = 0;}
		int cnt = 0;
		clockColor = "";
		switch (etType) 
		{
			case 2:	// date
				if (isEditDate)
				{
					isEditDate = false;
					return;
				}
				for (int i = 0; i < data.length(); i++)
		        {
					if (data.charAt(i) == '.')	cnt++;
		        }
				if (cnt != 2)
				{
					setEditData(et, true);
					isEditDate = true;
					et.setText(lastDate);
					return;
				}
				if (data.length() == 10)
				{
					if (!data.startsWith("????.") & data.substring(0, 4).contains("?"))
					{
						setEditData(et, false);
						return;
					}
					if (data.startsWith("????.") | data.contains(".??"))
					{
						if (!data.endsWith(".??") | (data.startsWith("????.") & !data.endsWith(".??.??")))
							setEditData(et, false);
						else
							setEditData(et, true);
						return;
					}
					int md = 0;
					try		
					{
						year = Integer.parseInt(data.substring(0, 4));
						month = Integer.parseInt(data.substring(5, 7));
						day = Integer.parseInt(data.substring(8, 10));
					}
			    	catch 	(NumberFormatException e) {	setToday();	}
					if (year >= dateYear)
					{
						if (year > dateYear)
							setToday();
						else
						{
							if (month > dateMonth)
								setToday();
							else
							{
								if (month == dateMonth & day > dateDay)
									setToday();
							}
						}
					}
					Calendar  cal = Calendar.getInstance();
					cal.set(year, month -1, day);
					cal.setLenient(false);
					try 
					{
						cal.get(GregorianCalendar.YEAR);
						cal.get(GregorianCalendar.MONTH);
						cal.get(GregorianCalendar.DAY_OF_MONTH);
						md = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
						if (day > md)
							day = md;
						lastDate = data;
						setEditData(et, true);
					} 
					catch (IllegalArgumentException e) { setEditData(et, false); }
				}
				else
					setEditData(et, false);
				if (isEditDate)
				{
					et.setText(getDateYYYYMMDD());
					return;
				}
				break;
			case 4:	// clock	(W/hh:mm:ss | B/hh:mm:ss)
				setEditData(et, true);
				if (data.length() < 2)
				{
					setEditData(et, false);
					return;
				}
				if (!(data.substring(1, 2).equals("/") & (data.substring(0, 1).equals("W") | data.substring(0, 1).equals("B"))))
				{
					setEditData(et, false);
					return;
				}
				else
				{
					clockColor = data.substring(0, 2);
					data = data.substring(2, data.length());
				}
			case 3:	// time		(hh:mm:ss)
				setEditData(et, true);
				cnt = 0;
				for (int i = 0; i < data.length(); i++)
		        {
					if (data.charAt(i) == ':')	cnt++;
		        }
				if (cnt != 2)
				{
					setEditData(et, false);
					et.setText("2:00:00");
					if (etType == 4)
						et.setText(clockColor + "2:00:00");
					return;
				}
				if (data.length() < 7 | data.charAt(0) == ':')
				{
					setEditData(et, false);
					return;
				}
				String h = "";
				String m = "";
				String s = "";
				for (int i = 0; i < data.length(); i++)
		        {
					if (!(data.charAt(i) == ':' | Character.isDigit(data.charAt(i))))	
					{
						data = clockColor + data.replace(Character.toString(data.charAt(i)), "");
						et.setText(data);
						return;
					}
		        }
				cnt = 1;
				for (int i = 0; i < data.length(); i++)
		        {
					if (data.charAt(i) == ':')	
						cnt++;
					else
					{
						switch (cnt) 
						{
							case 1:	// h
								h = h + data.charAt(i);
								break;
							case 2:	// m
								m = m + data.charAt(i);
								break;
							case 3:	// s
								s = s + data.charAt(i);
								break;
						}
					}
		        }
//Log.i(TAG, "h, m, s: " + h + ", " + m + ", " + s);
				if (h.length() < 1 | h.length() > 2 | m.length() != 2 | s.length() != 2)
				{
					setEditData(et, false);
					return;
				}
				boolean timeOk = true;
				int hi = 2; try	{hi = Integer.parseInt(h);} catch (NumberFormatException e) {timeOk = false;}
				int mi = 0; try	{mi = Integer.parseInt(m);} catch (NumberFormatException e) {timeOk = false;}
				int si = 0; try	{si = Integer.parseInt(s);} catch (NumberFormatException e) {timeOk = false;}
				if (!timeOk)
				{
					setEditData(et, false);
					return;
				}
				if (hi > 24) {h = "24"; timeOk = false;}
				if (mi > 59) {m = "59"; timeOk = false;}
				if (si > 59) {s = "59"; timeOk = false;}
				if (!timeOk)
					et.setText(clockColor + h + ":" + m + ":" + s);
				setEditData(et, true);
//Log.i(TAG, "h, m, s: " + h + ", " + m + ", " + s);
				break;
		}
	}
	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) { }
	@Override
	public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) 
	{ 
		
	}
	@Override
	public void onNothingSelected(AdapterView<?> arg0) { }
	public void setEditData(EditText et, boolean visible) 
	{
		if (visible)
		{
			et.setBackgroundResource(R.drawable.bordergreen);
			int resId = et.getId(); resId -= 200;
			tagError[resId] = 0;
		}
		else
		{
			et.setBackgroundResource(R.drawable.borderpink);
			int resId = et.getId(); resId -= 200;
			tagError[resId] = 1;
		}
		boolean isError = false;
		int errorId = 0;
		for (int i = 0; i < tagError.length; i++)
        {
			if (tagError[i] == 1)
			{
				isError = true;
				errorId = i +200;
			}
        }
		if (isError)
		{
			gdBtnOk.setVisibility(ImageView.INVISIBLE);
			et = (EditText) rl.findViewById(errorId);
			et.requestFocus();
		}
		else
			gdBtnOk.setVisibility(ImageView.VISIBLE);
	}

	public void getDateData()
    {
		Date newDate = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(newDate);
        dateDay = calendar.get(Calendar.DAY_OF_MONTH);
        dateMonth = calendar.get(Calendar.MONTH) + 1;
        dateYear = calendar.get(Calendar.YEAR);
        lastDate = getDateYYYYMMDD();
    }

	public CharSequence getDateYYYYMMDD()
    {
		sbDate.setLength(0);
        sbDate.append(dateYear);
        sbDate.append(".");
        if (dateMonth < 10)
        {
            sbDate.append("0");
            sbDate.append(dateMonth);
            sbDate.append(".");
        }
        else
        {
            sbDate.append(dateMonth);
            sbDate.append(".");
        }
        if (dateDay < 10)
        {
            sbDate.append("0");
            sbDate.append(dateDay);
        }
        else
        	sbDate.append(dateDay);
        return sbDate.toString();
    }

	public void setToday() 
	{
		year = dateYear;
		month = dateMonth;
		day = dateDay;
		isEditDate = true;
	}

	public void playSound(int idx, int loop)
    {
   		mSoundPool.play(soundsMap.get(idx), 0.2f, 0.2f, 1, loop, 1.0f);
    }

	public boolean onTouch(View view, MotionEvent event)
	{	// Touch Listener
		int resId = view.getId(); resId -= 100;
		TextView tv = (TextView) rl.findViewById(resId);
		if (tv.getText().equals("Result"))
 	    {
			resultViewId = view.getId();
			removeDialog(RESULT_LIST_DIALOG);
			showDialog(RESULT_LIST_DIALOG);
 	    }
		return true;
	}

	public Dialog onCreateDialog(int id)
	{	// creating dialog
		if (id == RESULT_LIST_DIALOG)  
        {
			final CharSequence[] items = new CharSequence[] {"1-0", "0-1", "1/2-1/2", "*"};
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setItems(items, new DialogInterface.OnClickListener() 
			{
			    public void onClick(DialogInterface dialog, int item) 
			    {
			    	EditText et = (EditText) rl.findViewById(resultViewId);
			    	et.setText(items[item]);
			    }
			});
			AlertDialog alert = builder.create();
            return alert;
        }
		if (id == TAG_LIST_DIALOG)  
        {
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setMultiChoiceItems(tItems, tState, new DialogInterface.OnMultiChoiceClickListener()
			{
				@Override
		        public void onClick(DialogInterface dialog, int which, boolean isChecked) 
				{
					if (which < 7)
					{
						playSound(1, 0);
						tState[which] = true;
					}
					if (tItems[which].equals("FEN"))
					{
						playSound(1, 0);
						if (isChecked) 
							tState[which] = false;
						else
							tState[which] = true;
					}
		        }
			});
			builder.setPositiveButton(R.string.btn_Ok, new DialogInterface.OnClickListener() 
			{
                public void onClick(DialogInterface dialog, int which) 
                {
                	String fenGameTag = "";
                	String[] txtSplit = _gameTags.toString().split("\n");
                	for(int h = 0; h < txtSplit.length; h++)
            	    {
    					if (txtSplit[h].startsWith("FEN"))
    					{
    						fenGameTag = txtSplit[h] + "\n";
    						break;
    					}
            	    }
                	rl.removeAllViews();
                	CharSequence newGameTags = "";
                	boolean tagExists = false;
                	for(int i = 0; i < tState.length; i++)
            	    {
                		tagExists = false;
            			if (tState[i])
        				{
            				if (!tItems[i].equals("FEN"))
            				{
	            				for(int h = 0; h < txtSplit.length; h++)
	                    	    {
	            					if (txtSplit[h].startsWith(tItems[i].toString()))
	            					{
	            						newGameTags = newGameTags.toString() + txtSplit[h] + "\n";
	            						tagExists = true;
	            						break;
	            					}
	                    	    }
	            				if (!tagExists )
	            				{
	            					String[] txtTagStats = history.tagState[i].toString().split("\b");
	            					newGameTags = newGameTags.toString() + tItems[i] + "\b" + txtTagStats[1] + "\n";
	            				}
            				}
        				}
            	    }
                	if (!fenGameTag.equals(""))
                		newGameTags = newGameTags.toString() + fenGameTag;
                	_gameTags = newGameTags;
                    setGameTagsToView();
                }
		    });
			AlertDialog alert = builder.create();
            return alert;
        }
		return null;
	}

	final String TAG = "ChessGameData";
	Util u;
	SharedPreferences userP;
	final static int RESULT_LIST_DIALOG = 1;
	final static int TAG_LIST_DIALOG = 2;
	Intent returnIntent = new Intent();
	public ChessHistory history;
	boolean initTagState = true;
	CharSequence[] tItems;
	boolean[] tState;
	Integer tagError[] = null;
	
	RelativeLayout rl = null;
	TextView title;
	ImageView gdBtnOk = null;
	ImageView gdBtnTags = null;
	int      		resultViewId = 0;
	
	CharSequence	_gameStat = "";
	CharSequence	_gameTags = "";
	int      		_gameTagsCount = 0;
	
	boolean isEditDate = false;
	int day = 0;
    int month = 0;
    int year = 0;
	int dateDay = 0;
    int dateMonth = 0;
    int dateYear = 0;
    StringBuilder sbDate = new StringBuilder(10);
    CharSequence lastDate = "";
    CharSequence clockColor = "";
    private SoundPool mSoundPool;
    private HashMap<Integer, Integer> soundsMap;
}
