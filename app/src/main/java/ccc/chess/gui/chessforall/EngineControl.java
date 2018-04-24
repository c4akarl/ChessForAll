package ccc.chess.gui.chessforall;

import android.content.Context;
import android.content.SharedPreferences;

import ccc.chess.book.BookOptions;
import ccc.chess.book.C4aBook;

public class EngineControl 
{
	EngineControl(Context context)
    {
		this.context = context;
		userPrefs = context.getSharedPreferences("user", 0);
		createEngines();
    }
	public void createEngines()
	{
		en_1 = new ChessEngine(context, 1);	// engine number 1
//		book = new C4aBook(context);
//		book.getInstance();
	}
	public final void setBookOptions() 
	{
		book = new C4aBook(context);
		book.getInstance();
		bookOptions.filename = userPrefs.getString("user_options_enginePlay_OpeningBookName", "");
        book.setOptions(bookOptions);
    }

	public void setPlaySettings(SharedPreferences userP, CharSequence color)
    {	// get play settings data from userPrefs + setting engine number
		// play settings data from userPrefs
		chessEnginePlayMod = userP.getInt("user_play_playMod", 1);
		if (chessEnginePlayMod == 4)
			initClockAfterAnalysis = true;
		twoEngines = false;
		if (chessEnginePlayMod == 3 | chessEnginePlayMod == 4)	// engine vs engine | analysis
			chessEngineSearching = true;
		if 	(		(chessEnginePlayMod == 1 & color.equals("b"))
				| 	(chessEnginePlayMod == 2 & color.equals("w"))
			)
		{
			chessEngineSearching = true;
			chessEnginePaused = false;
		}
    }
	public void setPlayData(SharedPreferences userP, String engineWhite, String engineBlack)
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
//	        		chessEnginePlayerWhite = mainA.gc.cl.history.getGameTagValue("White");
	        		chessEnginePlayerWhite = engineWhite;
//	        		chessEnginePlayerBlack = mainA.gc.cl.history.getGameTagValue("Black");
	        		chessEnginePlayerBlack = engineBlack;
	        		break;
	        }
		}
    }

	public void setEngineNumber(int eNumber) 
	{	//>361 engine Number: for better controlling multiple ChessEngines in GUI
		engineNumber = eNumber; 
	}
	public ChessEngine getEngine()
    {
    	switch (engineNumber)
        {
        	case 1:		return en_1;
//        	case 2:		return en_2;
        	default:	return en_1;
        }
    }

	public void setStartPlay(CharSequence color)
    {
		if (twoEngines)
		{

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
		}
//		Log.i(TAG, "setStartPlay(), playMod: " + chessEnginePlayMod + ", color: " + color + ", makeMove: " + makeMove);
    }

	final String TAG = "EngineControl";
	Context context;
	SharedPreferences userPrefs;		                    // user preferences(LogFile on/off . . .)
	ChessEngine en_1;	// default: Stockfish
	C4aBook book;
	BookOptions bookOptions = new BookOptions();
	int engineNumber = 1;					// for controlling ChessEngines: en_1 | en_2
	boolean twoEngines = false;				// true if two different engines(b/w)
	boolean makeMove = false;				// engine makes first move
    int chessEnginePlayMod = 1;				// 1 = player vs engine, 2 = engine vs player, 3 = engine vs engine, 4 = engine analysis
    				                        // 5 = player vs player, 6 = edit
	boolean initClockAfterAnalysis = false;

    public boolean chessEngineInit = false;
    public boolean chessEnginesOpeningBook = false;
    public boolean chessEngineSearching = false;
    public boolean chessEngineIsInSearchTask = false;

    public boolean chessEngineSearchingPonder = false;
    public CharSequence ponderUserFen = "";

    public boolean chessEnginePaused = false;
    public boolean chessEngineProblem = false;
    public boolean chessEngineAutoRun = false;
    public boolean chessEngineAnalysis = false;
    public boolean chessEngineStopSearch = false;
    public int chessEngineAnalysisStat = 0; 	// 0 = no analysis, 1 = make move ans stop engine, 2 = make move and continue analysis

    CharSequence chessEnginePlayerWhite = "Me";
    CharSequence chessEnginePlayerBlack = "";
    CharSequence chessEngineEvent = ""; 
    CharSequence chessEngineSite = "";
    CharSequence chessEngineRound = "";

}
