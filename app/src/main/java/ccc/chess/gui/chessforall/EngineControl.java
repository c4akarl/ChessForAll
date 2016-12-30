package ccc.chess.gui.chessforall;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import ccc.chess.book.BookOptions;
import ccc.chess.book.C4aBook;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Environment;
import android.util.AndroidRuntimeException;
import android.util.Log;
//import android.util.Log;

public class EngineControl 
{
	EngineControl(C4aMain cM)
    {
		c4aM = cM;						// main thread (for UI-actions)
		supportedServices = c4aM.getResources().getStringArray(R.array.engineServices); // supported engineServices
		createEngines();
    }
	public void createEngines()
	{	//>311 initializing en_1 and en_2, findEnginesOnDevice(), autoSetEngine1()
		en_1 = new ChessEngine(c4aM, 1);	// engine number 1
		en_2 = new ChessEngine(c4aM, 2);	// engine number 2
		en_1.engineName = "";
		en_1.engineServiceName = "";
		book = new C4aBook(c4aM);
		book.getInstance();
		findEnginesOnDevice();
		autoSetEngine1();
	}
	public final void setBookOptions() 
	{
		bookOptions.filename = c4aM.userP.getString("user_options_enginePlay_OpeningBookName", "");
		if (bookOptions.filename.length() > 0) 
        {
            File extDir = Environment.getExternalStorageDirectory();
            CharSequence sep = File.separator;
            bookOptions.filename = extDir.getAbsolutePath() + sep + bookOptions.filename;
        }
        book.setOptions(bookOptions);
    }
	public void autoSetEngine1()
	{	//>312 auto set engine 1 if no engine is selected
		findEnginesOnDevice();
		if (engineList.size() == 0)
		{
			SharedPreferences.Editor ed = c4aM.userP.edit();
			ed.putString("user_play_engine_1", "");
			ed.putString("user_play_engine_2", "");
	        ed.commit();
	        
		}
		else
		{
			if (c4aM.userP.getString("user_play_engine_1", "").equals(""))
			{
				if (engineList.size() > 0)
				{
					CharSequence tmp[] = engineList.get(0).toString().split("\b");
		    		if (tmp.length > 1)
		    		{
		    			SharedPreferences.Editor ed = c4aM.userP.edit();
		    			ed.putString("user_play_engine_1", tmp[1].toString());
		    	        ed.commit();
//		    	        Log.i(TAG, "createEngines: " + c4aM.userP.getString("user_play_engine_1", ""));
		    		}
				}
			}
		}
	}
	public ArrayList<CharSequence> findEnginesOnDevice()
    {	//>313 find engines on device and check with supported engines
    	final Intent i = new Intent("android.intent.action.MAIN");
    	i.addCategory("ccc.chess.ENGINE");	// from the ccc.chess.engine.name manifest
    	engineList = new ArrayList<CharSequence>();
    	PackageManager pm = c4aM.getPackageManager();
    	final List<ResolveInfo> appList = pm.queryIntentActivities(i, 0);
    	for (ResolveInfo info : appList) 
    	{
//    		info.dump(new LogPrinter(Log.INFO, TAG), "DUMP/");
    		CharSequence label = pm.getText(info.activityInfo.packageName, info.activityInfo.labelRes, null).toString();
    		CharSequence packageName = info.activityInfo.packageName + engineInterfaceName;
//            Log.i(TAG, "label, packageName: " + label + ", " + packageName);
            if (isServiceSupported(packageName, supportedServices))
            	engineList.add(label + "\b" + packageName);
        }
    	return engineList;
    }
	public CharSequence getEngineAppNameFromEngineList(CharSequence engineServiceName)
    {
		for (int i = 0; i < engineList.size(); i++)
		{
			CharSequence tmp[] = engineList.get(i).toString().split("\b");
    		if (tmp.length > 1 & engineServiceName.equals(tmp[1]))
    			return tmp[0];
		}
		return "";
    }
	public boolean isServiceSupported(CharSequence serviceName, CharSequence[] supportedServices)												
    {	//>314 validate running service with supported services(R.array.engineServices)
		boolean isSupported = false;
		for (int i = 0; i < supportedServices.length; i++)
		{
			if (serviceName.equals(supportedServices[i]))
		  	{
		  		isSupported = true;
		  		break;
		  	}
		}
		return isSupported;
    }
	public boolean isServiceOnDevice(CharSequence serviceName, ArrayList<CharSequence> runningServices)												
    {	//>315 validate serviceName with running services
		boolean serviceFound = false;
		for (int i = 0; i < runningServices.size(); i++)
		{
			CharSequence tmp[] = runningServices.get(i).toString().split("\b");
			if (tmp.length > 1 & serviceName.equals(tmp[1]))
			{
				serviceFound = true;
		  		break;
		  	}
		}
		return serviceFound;
    }
	public int getIdxFromSupportedServices(CharSequence serviceName, CharSequence[] supportedServices)												
    {	//>316 getting ChessEngineServiceInterface from R.array.engineServices
		int serviceId = -1;
		for (int i = 0; i < supportedServices.length; i++)
		{
			if (serviceName.equals(supportedServices[i]))
		  	{
				serviceId = i;
		  		break;
		  	}
		}
		return serviceId;
    }
	public void setPlaySettings(SharedPreferences userP)												
    {	// get play settings data from userPrefs + setting engine number
		// play settings data from userPrefs
		chessEnginesConnected = false;
		chessEnginePlayMod = userP.getInt("user_play_playMod", 1);
		autoSetEngine1();
		en_1.engineServiceName = userP.getString("user_play_engine_1", "");
		en_2.engineServiceName = userP.getString("user_play_engine_2", "");
		if (		chessEnginePlayMod == 1 | chessEnginePlayMod == 2 | chessEnginePlayMod == 4
				| 	userP.getString("user_play_engine_1", "").equals(userP.getString("user_play_engine_2", ""))
				|	userP.getString("user_play_engine_2", "").equals(""))
		{
			twoEngines = false;
		}
		else	// two different engines!
		{
			twoEngines = true;
		}
		if (chessEnginePlayMod == 3 | chessEnginePlayMod == 4)	// engine vs engine | analysis
			chessEngineSearching = true;
//		Log.i(TAG, "chessEnginesConnected, getEngineServiceIsReady(1, 2): " + chessEnginesConnected
//				+ ", " + getEngineServiceIsReady(1) + ", " + getEngineServiceIsReady(2));
    }
	public void setPlayData(SharedPreferences userP)												
    {	// setting the PGN-Data 
		chessEngineEvent = "Android " + android.os.Build.VERSION.RELEASE;
		chessEngineSite = android.os.Build.MODEL;
		chessEngineRound = "-";
		if (chessEngineAutoRun)
		{
			chessEngineRound = 	userP.getInt("user_play_eve_round", 1) 
								+ "." + userP.getInt("user_play_eve_gameCounter", 1);
		}
		if (twoEngines)
		{
			chessEnginePlayerWhite = en_1.engineName;
    		chessEnginePlayerBlack = en_2.engineName;
		}
		else
		{
			switch (chessEnginePlayMod)
	        {
	        	case 1:
	        		chessEnginePlayerWhite = userP.getString("user_options_gui_playerName", "Me");
	        		chessEnginePlayerBlack = en_1.engineName;
	        		break;
	        	case 2:
	        		chessEnginePlayerWhite = en_1.engineName;
	        		chessEnginePlayerBlack = userP.getString("user_options_gui_playerName", "Me");
	        		break;
	        	case 3:	
	        		chessEnginePlayerWhite = en_1.engineName;
	        		chessEnginePlayerBlack = en_1.engineName;
	        		break;
	        	case 4:	
	        		chessEnginePlayerWhite = c4aM.gc.cl.history.getGameTagValue("White");
	        		chessEnginePlayerBlack = c4aM.gc.cl.history.getGameTagValue("Black");
	        		break;
	        }
		}
    }
//	EngineControl >>> EngineService(en_1 | en_2)		EngineControl >>> EngineService(en_1 | en_2)
	public void setEngineNumber(int eNumber) 
	{	//>361 engine Number: for better controlling multiple ChessEngines in GUI
		engineNumber = eNumber; 
	}
	public ChessEngine getEngine()
    {	//>362 getEngine() instead of chessEngine
    	switch (engineNumber)
        {
        	case 1:		return en_1;
        	case 2:		return en_2;
        	default:	return en_1;
        }
    }
	public void initEngines()
    {	//>370 start c4aM.uic.chessEngineBindTask.execute("1", "2")
		findEnginesOnDevice();
		c4aM.uic.cancelBindTask();
		c4aM.uic.cancelSearchTask();
		chessEngineBindCounter = 1;
		c4aM.uic.chessEngineBindTask = c4aM.uic.new ChessEngineBindTask();
		c4aM.uic.chessEngineBindTask.execute("1", "2");	// bind engine 1 & engine 2
    }
	public boolean bindToEngineService()
    {	//>371 binds the main activity to the ChessEngineService
    	boolean ret = false;
    	chessEnginesNotFound = false;
//    	Log.i(TAG, "bindToEngineService(), engineServiceName: " + getEngine().engineServiceName);
    	if (!isServiceOnDevice(getEngine().engineServiceName, engineList))
    	{	// the current engineService is not on device.
    		if (engineList.size() != 0)
    		{	// take first entry of engineList
    			CharSequence tmp[] = engineList.get(0).toString().split("\b");
	    		if (tmp.length > 1)
	    		{
	    			getEngine().engineServiceName = tmp[1];
	    			if (!isServiceSupported(getEngine().engineServiceName, supportedServices))
	    				getEngine().engineServiceName = "";
	    		}
    		}
    		else
    		{
//    			Log.i(TAG, "bindToEngineService(), engineServiceName = SPACE");
    			getEngine().engineServiceName = "";		// no engine on device
    		}
    	}
    	try
    	{
    		if (isServiceSupported(getEngine().engineServiceName, supportedServices))
	    	{
		    	Intent i;
		    	getEngine().setEngineServiceId(getIdxFromSupportedServices(getEngine().engineServiceName, supportedServices));
	    		i = new Intent(getEngine().engineServiceName.toString());
//				i.setPackage("ccc.chess.engine.stockfish");
				String packageName;
				if (getEngine().engineServiceName.equals(SERVICE_CHESS_ENGINES))
					packageName = "ccc.chess.engines";
				else
					packageName = "ccc.chess.engine.stockfish";
				i.setPackage(packageName);
				Log.i(TAG, "engineServiceName: " + getEngine().engineServiceName + ", package: " + packageName);
	    		//>373 binds to the ChessEngineService --->154 (ccc_engine)
		    	ret = c4aM.bindService(i, getEngine(), Context.BIND_AUTO_CREATE);
		    	if (ret)
		    	{
		    		c4aM.startService(i);	//>374 for starting/stopping the ChessEngineService --->156 (ccc_engine)
		    	}
	    	}
    	}
    	catch (AndroidRuntimeException e) {e.printStackTrace();}
//    	Log.i(TAG, "engineServiceName: " + getEngine().engineNumber + ", " + getEngine().engineServiceName);
//    	Log.i(TAG, "bindToEngineService() bound: " + ret);
   		try {Thread.sleep(sleepTime);} 
		catch (InterruptedException e) {}
        if (ret)
        	getEngine().isBound = true;
        else
    	{
    		chessEnginesNotFound = true;
//    		Log.i(TAG, "NO_CHESS_ENGINE_DIALOG: " + ret);
// ERROR	v1.7	06.11.2011 22:47:06
//   			c4aM.uic.c4aShowDialog(UiControl.NO_CHESS_ENGINE_DIALOG);
    	}
		return ret;
    }
	public void stopAllEngines() 
    {	//>381 shutdownEngine() and releaseEngineService()
    	setEngineNumber(1);
    	if (getEngine() != null)	
    		stopComputerThinking(true);
    	setEngineNumber(2);
    	if (getEngine() != null)		
    		stopComputerThinking(true);
    	if (!c4aM.isAppEnd)
    		chessEnginePaused = true;
    }
	public void setStartPlay(CharSequence color)
    {
		if (twoEngines)
		{
			makeMove = true;
			if (color.equals("b")) 
			{
				en_2.startPlay = true;
				en_1.startPlay = false;
			}
			else
			{
				en_1.startPlay = true;
				en_2.startPlay = false;
			}
		}
		else
		{
			if 	(		chessEnginePlayMod == 3
					|	chessEnginePlayMod == 4
					|	chessEnginePlayMod == 1 & color.equals("b")
					| 	chessEnginePlayMod == 2 & color.equals("w")
				)
			{
				en_1.startPlay = true;
				makeMove = true;
			}
			else
			{
				en_1.startPlay = false;
				makeMove = false;
			}
			en_2.startPlay = false;
		}
		if (c4aM.gc.isGameLoaded)
		{
			en_1.startPlay = false;
			en_2.startPlay = false;
			makeMove = false;
		}
//		Log.i(TAG, "playMod, twoEngines, color, startPlay: " + chessEnginePlayMod + ", " + twoEngines + ", " + color);
//		Log.i(TAG, "startPlay en_1, en_2: " + en_1.startPlay + ", " + en_2.startPlay);
    }
	public final synchronized void stopComputerThinking(boolean shutDown) 
    {
//		Log.i(TAG, "stopComputerThinking, processAlive, shutDown: " + getEngine().processAlive + ", " + shutDown);
		chessEngineStopSearch = true;
//		if (chessEngineSearching & !shutDown)
//		{
//			chessEngineStopSearch = true;
//			return;
//		}
		try 
		{
			if (getEngine().processAlive)
	    	{
	    		getEngine().stopSearch();
	    		if (c4aM.uic.chessEngineSearchTask != null)
		    		c4aM.uic.chessEngineSearchTask.cancel(true);
	    	}
			if (shutDown)
	    	{
				getEngine().shutDown();
				if (c4aM.uic.chessEngineSearchTask != null)
	    			c4aM.uic.chessEngineSearchTask.cancel(true);
				releaseEngineService();
				try {Thread.sleep(sleepTime);} 
				catch (InterruptedException e) {}
	    	}
		}
		catch (NullPointerException e) {e.printStackTrace();}
    }
	public void releaseEngineService()
    {	//>382 unbinds the main activity C4aMain from the ChessEngineService
 		if (getEngine() != null)
    	{
			try 
			{
//				Log.i(TAG, "eNumber, engineServiceIsReady: " + getEngine().engineNumber + ", " + getEngine().engineServiceIsReady);
//				Log.i(TAG, "getEngine().isBound, engineServiceName: " + getEngine().isBound + ", " + getEngine().engineServiceName);
				if (getEngine().isBound)
					c4aM.unbindService(getEngine());
				getEngine().engineServiceIsReady = false;
				getEngine().isBound = false;
				getEngine().engineName = "";
				getEngine().engineServiceName = "";
			}
			catch (IllegalArgumentException e) {e.printStackTrace();}
    	}
    }
	public void newGame(boolean chess960, CharSequence fen) 
    {	//>391 set options and getEngine().newGame()
		getEngine().setIsChess960(chess960);
		getEngine().setStartFen(fen);
		getEngine().newGame();
    }

	final String TAG = "EngineControl";
	private static final CharSequence SERVICE_STOCKFISH = "ccc.chess.engine.stockfish.IChessEngineService";	// android service id
	private static final CharSequence SERVICE_CHESS_ENGINES = "ccc.chess.engines.IChessEngineService";	// android service id
	C4aMain c4aM;
	ChessEngine en_1;						// ChessEngineServiceConnection (engine number 1)
	ChessEngine en_2;						// ChessEngineServiceConnection (engine number 2)
	C4aBook book;
	BookOptions bookOptions = new BookOptions();
	int engineNumber = 1;					// for controlling ChessEngines: en_1 | en_2
	int tmpEngineNumber = 1;					
	CharSequence[] supportedServices;				// res/values/arrays/resources/string-array/engineServices
	ArrayList<CharSequence> engineList = null;	// findEnginesOnDevice() - supportedServices
	ArrayList<CharSequence> engineNameList;
	public CharSequence engineInterfaceName = ".IChessEngineService";
	boolean twoEngines = false;				// true if two different engines(b/w)
	boolean makeMove = false;				// engine makes first move
	boolean onBinding = false;				// engine makes first move
    int chessEnginePlayMod = 1;				// 1 = player vs engine, 2 = engine vs player, 3 = engine vs engine, 4 = engine analysis
    				                        // 5 = player vs player, 6 = edit
    long sleepTime = 200;
    
    public boolean chessEngineInit = false;
    public boolean chessEnginesNotFound = false;
    public boolean chessEnginesConnected = false;
    public boolean chessEnginesOpeningBook = false;
    public boolean chessEngineSearching = false;
    public boolean chessEnginePaused = false;
    public boolean chessEngineProblem = false;
    public boolean lastChessEnginePaused = false;
    public boolean chessEngineAutoRun = false;
    public boolean chessEngineAnalysis = false;
    public boolean chessEngineStopSearch = false;
    public int chessEngineBindCounter = 0; 	
    public int chessEngineAnalysisStat = 0; 	// 0 = no analysis, 1 = make move ans stop engine, 2 = make move and continue analysis

    CharSequence chessEnginePlayerWhite = "Me";
    CharSequence chessEnginePlayerBlack = "";
    CharSequence chessEngineEvent = ""; 
    CharSequence chessEngineSite = "";
    CharSequence chessEngineRound = "";
}
