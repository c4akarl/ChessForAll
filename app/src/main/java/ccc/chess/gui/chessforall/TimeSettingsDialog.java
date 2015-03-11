package ccc.chess.gui.chessforall;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import kankan.wheel.widget.ArrayWheelAdapter;
import kankan.wheel.widget.WheelView;
import kankan.wheel.widget.NumericWheelAdapter;
//import android.util.Log;

public class TimeSettingsDialog extends Dialog implements View.OnClickListener
{
	public TimeSettingsDialog(Context context, Ic4aDialogCallback callback, String title, String message,  
				int timeGame, int timeBonus, int movesToGo)
    {
		super(context);
		this.context = context;
		c4aCallback = callback;
		this.title = title;
		this.message = message;
		this.timeGame = timeGame;
		this.timeBonus = timeBonus;
		this.movesToGo = movesToGo;
		if (timeGame == -1)
		{
			this.timeGame = 0;
			showTime = false;
		}
		if (timeBonus == -1)
		{
			this.timeBonus = 0;
			showBonus = false;
		}
		if (movesToGo == -1)
		{
			this.movesToGo = 0;
			showMovesToGo = false;
		}
    }
	protected void onCreate(Bundle savedInstanceState) 
	{
//    	Log.i(TAG, "onCreate, title: " + title);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chessclocksettingsdialog);
//        this.setTitle(title + " - " + message);
        this.setTitle(message);
//        tvMovesToGo = (TextView) findViewById(R.id.tvMovesToGo);
        tvTime = (TextView) findViewById(R.id.tvTime);
        tvTimeH = (TextView) findViewById(R.id.tvTimeH);
        tvTimeMin = (TextView) findViewById(R.id.tvTimeMin);
        tvTimeSec = (TextView) findViewById(R.id.tvTimeSec);
        
        tvBonus = (TextView) findViewById(R.id.tvBonus);
        tvBonusSec = (TextView) findViewById(R.id.tvBonusSec);
        tvBonusMilSec = (TextView) findViewById(R.id.tvBonusMilSec);
        if (!showTime)
        	tvBonus.setText("");
        ok = (ImageView) findViewById(R.id.ok);
        ok.setOnClickListener(this);
        setClockValues(timeGame, timeBonus);
        setClock();
	}
	@Override
	public void onClick(View view) 
	{	// ClickHandler (ButtonEvents)
		switch (view.getId()) 
		{
			case R.id.ok: 
//				Log.i(TAG, "time h, m, s: " + timeH.getCurrentItem() + ", " 
//							+ timeMin.getCurrentItem()  + ", " 
//							+ timeSec.getCurrentItem());
//				Log.i(TAG, "bonus s, ms: " + bonusSec.getCurrentItem() + ", " 
//							+ msecValues[bonusMsec.getCurrentItem()]);
//				Log.i(TAG, "time, moves to go: " + movesValues[moves.getCurrentItem()]);
				c4aCallback.getCallbackValue(2);
				dismiss();
				break;
		}
	}
	public void setClockValues(int time, int bonus) 
	{
		if (time != 0)
		{
			timeGameS = time / 1000;
			timeGameM = time / 60000;
			if (timeGameM > 0)
			{
				if (timeGameM > 59)
				{
					timeGameH = timeGameM / 60;
					timeGameM = timeGameM % 60;
				}
				timeGameS = timeGameS -((timeGameH * 3600) + (timeGameM * 60));
			}
		}
//		Log.i(TAG, "time, bonus: " + time + ", " + bonus);
//		Log.i(TAG, "h, m, s: " + timeGameH + ", " + timeGameM + ", " + timeGameS);
		if (bonus != 0)
		{
			timeBonusS = bonus / 1000;
			if (timeBonusS > 59 )
			{
				timeBonusS = 59;
				timeBonusMs = 0;
			}
			else
			{
				timeBonusMs = bonus % 1000;
				timeBonusMs = timeBonusMs / 10;
			}
			
		}
	}
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void setClock() 
	{
	// time, hours
		timeH = (WheelView) findViewById(R.id.timeH);
		timeHAdapter = new NumericWheelAdapter(context, 0, 9, "%01d");
		timeHAdapter.setItemResource(R.layout.wheeltextitem);
		timeHAdapter.setItemTextResource(R.id.text);
		timeH.setViewAdapter(timeHAdapter);
		timeH.setCurrentItem(timeGameH);
		timeH.setCyclic(true);
	// time, minutes
		timeMin = (WheelView) findViewById(R.id.timeMin);
		timeMinAdapter = new NumericWheelAdapter(context, 0, 59, "%02d");
		timeMinAdapter.setItemResource(R.layout.wheeltextitem);
		timeMinAdapter.setItemTextResource(R.id.text);
        timeMin.setViewAdapter(timeMinAdapter);
        timeMin.setCurrentItem(timeGameM);
        timeMin.setCyclic(true);
    // time, seconds
        timeSec = (WheelView) findViewById(R.id.timeSec);
        timeSecAdapter = new NumericWheelAdapter(context, 0, 59, "%02d");
        timeSecAdapter.setItemResource(R.layout.wheeltextitem);
        timeSecAdapter.setItemTextResource(R.id.text);
		timeSec.setViewAdapter(timeSecAdapter);
		timeSec.setCurrentItem(timeGameS);
		timeSec.setCyclic(true);
    // moves to go
//		createMovesArray();
//		moves = (WheelView) findViewById(R.id.movesToGo);
//		movesAdapter = new ArrayWheelAdapter(context, movesValues);
//		movesAdapter.setItemResource(R.layout.wheeltextitem);
//		movesAdapter.setItemTextResource(R.id.text);
//		moves.setViewAdapter(movesAdapter);
//		moves.setCurrentItem(movesToGo);
//		moves.setCyclic(true);
			
	// bonus, seconds
		bonusSec = (WheelView) findViewById(R.id.bonusSec);
		bonusSecAdapter = new NumericWheelAdapter(context, 0, 59, "%02d");
		bonusSecAdapter.setItemResource(R.layout.wheeltextitem);
		bonusSecAdapter.setItemTextResource(R.id.text);
        bonusSec.setViewAdapter(bonusSecAdapter);
        bonusSec.setCurrentItem(timeBonusS);
        bonusSec.setCyclic(true);
	// bonus, mil seconds
        createMsecArray();
        bonusMsec = (WheelView) findViewById(R.id.bonusMilSec);
//        Log.i(TAG, "msecValues: " + msecValues[15]);
        bonusMsecAdapter = new ArrayWheelAdapter(context, msecValues);
        bonusMsecAdapter.setItemResource(R.layout.wheeltextitem);
        bonusMsecAdapter.setItemTextResource(R.id.text);
		bonusMsec.setViewAdapter(bonusMsecAdapter);
		bonusMsec.setCurrentItem(timeBonusMs);
		bonusMsec.setCyclic(true);
		
		if (!showTime)
		{
			timeH.setVisibility(WheelView.INVISIBLE);
			timeMin.setVisibility(WheelView.INVISIBLE);
	        timeSec.setVisibility(WheelView.INVISIBLE);
	        tvTime.setVisibility(TextView.INVISIBLE);
	        tvTimeH.setVisibility(TextView.INVISIBLE);
	        tvTimeMin.setVisibility(TextView.INVISIBLE);
	        tvTimeSec.setVisibility(TextView.INVISIBLE);
		}
		if (!showBonus)
		{
			bonusSec.setVisibility(WheelView.INVISIBLE);
			bonusMsec.setVisibility(WheelView.INVISIBLE);
			tvBonus.setVisibility(TextView.INVISIBLE);
			tvBonusSec.setVisibility(TextView.INVISIBLE);
			tvBonusMilSec.setVisibility(TextView.INVISIBLE);
		}
//		if (!showMovesToGo)
//		{
//			moves.setVisibility(WheelView.INVISIBLE);
//			tvMovesToGo.setVisibility(TextView.INVISIBLE);
//		}
	}
	public void createMovesArray() 
	{
		movesValues = context.getResources().getStringArray(R.array.chessClockMoves);
	}
	public void createMsecArray() 
	{
		String msecVal = "000";
		int n = 0;
		for (int i = 0; i < 100; i++)
        {
			n = i * 10;
			if (n >= 100)
				msecVal = Integer.toString(n);
			else
			{
				if (i == 0)
					msecVal = "000";
				else
					msecVal = "0" + Integer.toString(n);
			}
//			Log.i(TAG, "msecVal: " + msecVal);
			msecValues[i] = msecVal;
        }
	}
	public int getTime() 
	{	// return time(mil. seconds)
		return 	timeH.getCurrentItem() * 3600000
			+ 	timeMin.getCurrentItem() * 60000
			+	timeSec.getCurrentItem() * 1000;
	}
	public int getBonus() 
	{	// return time(mil. seconds)
		int msec = 0;
		try		
		{
			msec = Integer.parseInt(msecValues[bonusMsec.getCurrentItem()]);
			if (msec < 0)
				msec = 0;
		}
    	catch 	(NumberFormatException e) {msec = 0;}
		return 	bonusSec.getCurrentItem() * 1000
			+	msec;
	}
//	public int getMovesToGo() 
//	{
//		if (!showMovesToGo)
//			return 0;
//		else
//		{
//			int movesToGo;
//			try		{movesToGo = Integer.parseInt(movesValues[moves.getCurrentItem()]);}
//	    	catch 	(NumberFormatException e) {movesToGo = 0;}
//			return movesToGo;
//		}
//	}
	
	final String TAG = "ChessClockSettingsDialog";
	boolean showMovesToGo = true;
	boolean showTime = true;
	boolean showBonus = true;
	Context context;
	private final Ic4aDialogCallback c4aCallback;
	TextView tvMovesToGo = null;
	TextView tvTime = null;
	TextView tvTimeH = null;
	TextView tvTimeMin = null;
	TextView tvTimeSec = null;
	TextView tvBonus = null;
	TextView tvBonusSec = null;
	TextView tvBonusMilSec = null;
	WheelView timeH;
	WheelView timeMin;
	WheelView timeSec;
//	WheelView moves;
	WheelView bonusSec;
	WheelView bonusMsec;
	NumericWheelAdapter timeHAdapter;
	NumericWheelAdapter timeMinAdapter;
	NumericWheelAdapter timeSecAdapter;
	ArrayWheelAdapter<?> movesAdapter;
	NumericWheelAdapter bonusSecAdapter;
	ArrayWheelAdapter<?> bonusMsecAdapter;
	String movesValues[];
	String msecValues[] = new String[100];
	ImageView ok = null;
	String title = null;
	String message = null;
	int timeGame = 0;
	int timeGameH = 0;
	int timeGameM = 0;
	int timeGameS = 0;
	int timeBonus = 0;
	int timeBonusS = 0;
	int timeBonusMs = 0;
	int movesToGo = 0;
}
