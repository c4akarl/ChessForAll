package ccc.chess.gui.chessforall;

public class TimeControl
{

	public TimeControl()
    {
//Log.i(TAG, "TimeControl(), initChessClock()");
		initChessClock(1, 300000, 300000, 0, 0, 0);	// init: 5 minutes + 2 seconds(move)
		setCurrentShowValues(1);
    }

	public void initChessClock(int timeControl, int timeWhite, int timeBlack, int movesToGo, int bonusWhite, int bonusBlack)
    {	
		this.timeControl = timeControl;
		this.bonusWhite = bonusWhite;
		this.bonusBlack = bonusBlack;
		this.timeWhite = timeWhite;
		this.timeBlack = timeBlack;
		this.movesToGo = movesToGo;
		clockIsRunning = false;
		whiteMoves = true;
		startTime = 0;
		showModusWhite = 2;
		showModusBlack = 2;
		showWhiteTime = "";
		showBlackTime = "";
		setCurrentShowValues(1);
    }

	public void startChessClock(boolean whiteMoves, long currentTime, int playMod)
    {
//Log.i(TAG, "1 startChessClock(), clockIsRunning: " + clockIsRunning + ", timeControl: " + timeControl);
		if (!clockIsRunning & timeControl != 4)
		{
			clockIsRunning = true;
			this.whiteMoves = whiteMoves;
			startTime = currentTime;
			if (timeControl == 11)	// analysis
				timeWhite = 0;
			setCurrentShowValues(playMod);
		}
    }

	public void stopChessClock(long currentTime, int playMod)
    {
//Log.i(TAG, "stopChessClock()");
		if (clockIsRunning)
		{
			clockIsRunning = false;
			if (timeControl == 11)	// analysis
				timeWhite = timeWhite + (int) (currentTime - startTime);
			else
			{
				if (whiteMoves)
				{
					timeWhite = timeWhite - (int) (currentTime - startTime);
					if (timeControl == 3)
						timeBlack = timeBlack + (int) (currentTime - startTime);
				}
				else
				{
					timeBlack = timeBlack - (int) (currentTime - startTime);
					if (timeControl == 3)
						timeWhite = timeWhite + (int) (currentTime - startTime);
				}
			}
			setCurrentShowValues(playMod);
		}
    }

	public void switchChessClock(boolean whiteMoves, long currentTime, int playMod)
    {
		if (this.whiteMoves & whiteMoves | !this.whiteMoves & !whiteMoves)
			colorChanged = false;
		else
			colorChanged = true;
//Log.i(TAG, "OLD: timeControl: " + timeControl + ", whiteMoves: " + whiteMoves + ", currentTime: " + currentTime + ", startTime: " + startTime);
//Log.i(TAG, "OLD: timeWhite: " + timeWhite + ", bonusWhite: " + bonusWhite + ", clockIsRunning: " + clockIsRunning);
//Log.i(TAG, "OLD: timeBlack: " + timeBlack + ", bonusBlack: " + bonusBlack + ", colorChanged: " + colorChanged);
		if (clockIsRunning)
		{
			if (timeControl == 11)	// analysis
            {
                timeWhite = timeWhite + (int) (currentTime - startTime);
            }
			else
			{
				if (this.whiteMoves)
				{
					timeWhite = timeWhite - (int) (currentTime - startTime);
					if (timeWhite < 0) 	timeWhite = 0;
					if (timeControl == 3)
						timeBlack = timeBlack + (int) (currentTime - startTime);
					else
					{
						if (colorChanged)
						{
							timeWhite = timeWhite + bonusWhite;
							if (timeWhite <= bonusWhite)
								timeWhite = timeWhite + MIN_TIME;
						}
					}
				}
				else
				{
					timeBlack = timeBlack - (int) (currentTime - startTime);
					if (timeBlack < 1) 	timeBlack = 0;
					if (timeControl == 3)
						timeWhite = timeWhite + (int) (currentTime - startTime);
					else
					{
						if (colorChanged)
						{
							timeBlack = timeBlack + bonusBlack;
							if (timeBlack <= bonusBlack)
								timeBlack = timeBlack + MIN_TIME;
						}
					}
				}
			}
//Log.i(TAG, "NEW: timeWhite: " + timeWhite + ", bonusWhite: " + bonusWhite);
//Log.i(TAG, "NEW: timeBlack: " + timeBlack + ", bonusBlack: " + bonusBlack);
			startTime = currentTime;
			this.whiteMoves = whiteMoves;
			setCurrentShowValues(playMod);
		}
    }

	public int getValueFromMilSeconds(int milSeconds, int modus)
    {
		int h = 0;
		int m = 0;
		int s = 0;
		int ms = milSeconds % 1000;
		s = milSeconds / 1000;
		m = milSeconds / 60000;
		if (m > 0)
		{
			if (m > 59)
			{
				h = m / 60;
				m = m % 60;
			}
			s = s -((h * 3600) + (m * 60));
		}
		switch (modus)
        {
	        case 1:		return ms;   
	        case 2:		return s;   
	        case 3:		return m;   
	        case 4:		return h;   
	        default: 	return 0;    
        }
    }

	public String getShowValues(int milSeconds)
    {
		String showTime = "";
		if (milSeconds > 0)
		{
			int h = 0;
			int m = 0;
			int s = 0;
			int ms = 0;
			
			ms = getValueFromMilSeconds(milSeconds, 1);
			s = getValueFromMilSeconds(milSeconds, 2);
			m = getValueFromMilSeconds(milSeconds, 3);
			h = getValueFromMilSeconds(milSeconds, 4);
			if (h != 0)
				showTime = h + ":";
			if (m != 0 | !showTime.equals(""))
			{
				if (m < 10 & !showTime.equals(""))
					showTime = showTime + "0";
				showTime = showTime + m + ":";
			}
			if (s < 10 & !showTime.equals(""))
				showTime = showTime + "0";
			showTime = showTime + s;
			if (ms != 0)
			{
				showTime = showTime + ".";
				if (ms < 100)
				{
					if (ms < 10)
						showTime = showTime + "00";
					else
						showTime = showTime + "0";
				}
				showTime = showTime + ms;
			}
		}
		return showTime;
    }

	public void setCurrentShowValues(int playMod)
    {	// set the current white/black time on screen
//Log.i(TAG, "OLD time White: " + showModusWhite + ", " + timeWhite + ", " + showWhiteTime);
//Log.i(TAG, "OLD time Black: " + showModusBlack + ", " + timeBlack + ", " + showBlackTime);
		int t;
		int h = 0;
		int m = 0;
		int s = 0;
		int ms = 0;
		StringBuilder showH;
		StringBuilder showM;
		StringBuilder showS;
		StringBuilder showMs;
		if (timeWhite < 0)
			timeWhite = 0;
		if (timeBlack < 0)
			timeBlack = 0;
		boolean msWhiteGreater = true;
		if (timeControl == 3)
		{
			if ((timeBlack % 1000) > 499)
				msWhiteGreater = false;
		}
		if (timeWhite >= 0)
		{
			t = timeWhite;
			if (t >= showTime1)
				showModusWhite = 1;
			else
			{
				if (t >= showTime2)
					showModusWhite = 2;
				else
					showModusWhite = 3;
			}
			if (timeControl == 11)
				showModusWhite = 2;
			ms = getValueFromMilSeconds(timeWhite, 1);
			s = getValueFromMilSeconds(timeWhite, 2);
			m = getValueFromMilSeconds(timeWhite, 3);
			h = getValueFromMilSeconds(timeWhite, 4);
			if (h == 0)
				showModusWhite = 2;
			if (timeControl == 3 & msWhiteGreater)
				s = s + 1;
			showH = new StringBuilder(10);
			showH.append(h);
			showM = new StringBuilder(10);
			if (m < 10 & showModusWhite == 1)
			{
				showM.append("0");
				showM.append(m);
			}
			else
				showM.append(m);
			showS = new StringBuilder(10);
			if (s < 10 & (showModusWhite == 1 |showModusWhite == 2))
			{
				if (h == 0 & m == 0 & playMod != 4)
					showModusWhite = 3;
				else
					showS.append("0");
				showS.append(s);
			}
			else
				showS.append(s);
			showMs = new StringBuilder(10);
			if (ms < 10)
				showMs.append("00");
			else
			{
				if (ms < 100)
					showMs.append("0");
			}
			showMs.append(ms);
			if (showModusWhite == 1)
				showWhiteTime = showH + ":" + showM + ":" + showS;
			if (showModusWhite == 2)
				showWhiteTime = showM + ":" + showS;
			if (showModusWhite == 3)
				showWhiteTime = showS + "." + showMs;
		}
		if (timeBlack >= 0 & timeControl != 11)
		{
			t = timeBlack;
			if (t >= showTime1)
				showModusBlack = 1;
			else
			{
				if (t >= showTime2)
					showModusBlack = 2;
				else
					showModusBlack = 3;
			}
			ms = getValueFromMilSeconds(timeBlack, 1);
			s = getValueFromMilSeconds(timeBlack, 2);
			m = getValueFromMilSeconds(timeBlack, 3);
			h = getValueFromMilSeconds(timeBlack, 4);
			if (h == 0)
				showModusBlack = 2;
			if (timeControl == 3 & !msWhiteGreater)
				s = s + 1;
			showH = new StringBuilder(10);
			showH.append(h);
			showM = new StringBuilder(10);
			if (m < 10 & showModusBlack == 1)
			{
				showM.append("0");
				showM.append(m);
			}
			else
				showM.append(m);
			showS = new StringBuilder(10);
			if (s < 10 & (showModusBlack == 1 | showModusBlack == 2))
			{
				if (h == 0 & m == 0 & playMod != 4)
					showModusBlack = 3;
				else
					showS.append("0");
				showS.append(s);
			}
			else
				showS.append(s);
			showMs = new StringBuilder(10);
			if (ms < 10)
				showMs.append("00");
			else
			{
				if (ms < 100)
					showMs.append("0");
			}
			showMs.append(ms);
			if (showModusBlack == 1)
				showBlackTime = showH + ":" + showM + ":" + showS;
			if (showModusBlack == 2)
				showBlackTime = showM + ":" + showS;
			if (showModusBlack == 3)
				showBlackTime = showS + "." + showMs;
		}
		if (timeControl == 4)
		{
			showWhiteTime = "";
			showBlackTime = "";
		}
//Log.i(TAG, "NEW time White: " + showModusWhite + ", " + timeWhite + ", " + showWhiteTime);
//Log.i(TAG, "NEW time Black: " + showModusBlack + ", " + timeBlack + ", " + showBlackTime);
    }
	
	final String TAG = "TimeControl";
	final int MIN_TIME = 50; 		// add minimum rest time
	final int showTime1 = 600000; 	// >= 10 minutes
	final int showTime2 =  10000; 	// >= 10 seconds
	boolean clockIsRunning;
	boolean whiteMoves;
	boolean colorChanged = false;
	long startTime;

	// current clock control
	int timeControl; // 1 game clock, 2 move time, 3 sand glass, 4 none, 11 analysis
	int timeWhite;
	int timeBlack;
	int movesToGo;
	int bonusWhite;
	int bonusBlack;

	// show data
	int showModusWhite; // 1 = h,m   2 = m,s   [3 = s,ms]
	int showModusBlack; 
	String showWhiteTime;	// 05:00 (m, s)
	String showBlackTime;

}
