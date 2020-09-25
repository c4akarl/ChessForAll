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
	private void createEngines()
	{
		en_1 = new ChessEngine(context, 1);	// engine number 1
	}
	final void setBookOptions()
	{
		book = new C4aBook(context);
		bookOptions.filename = userPrefs.getString("user_options_enginePlay_OpeningBookName", "");
        book.setOptions(bookOptions);
    }

	void setPlaySettings(SharedPreferences userP, CharSequence color)
    {
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
	void setPlayData(SharedPreferences userP, String white, String black)
    {
    	// setting the PGN-Data

//Log.i(TAG, "setPlayData(), white: " + white + ", black: " + black);

		chessEngineEvent = "Android " + android.os.Build.VERSION.RELEASE;
		chessEngineSite = android.os.Build.MODEL;
		chessEngineRound = "-";
		if (chessEngineAutoRun)
		{
			chessEngineRound = 	userP.getInt("user_play_eve_round", 1) 
								+ "." + userP.getInt("user_play_eve_gameCounter", 1);
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
	        		chessEnginePlayerWhite = white;
	        		chessEnginePlayerBlack = black;
	        		break;
	        }
		}
    }

	void setEngineNumber()
	{	//>361 engine Number: for better controlling multiple ChessEngines in GUI
		engineNumber = 1;
	}
	public ChessEngine getEngine()
    {
		return en_1;
    }

	void setStartPlay(CharSequence color)
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

//		Log.i(TAG, "setStartPlay(), playMod: " + chessEnginePlayMod + ", color: " + color + ", makeMove: " + makeMove);

    }

//	final String TAG = "EngineControl";
	private Context context;
	private SharedPreferences userPrefs;		                    // user preferences(LogFile on/off . . .)
	ChessEngine en_1;
	C4aBook book;
	private BookOptions bookOptions = new BookOptions();
	int engineNumber = 1;					// for controlling ChessEngines: en_1 | en_2
	boolean twoEngines = false;				// true if two different engines(b/w)
	boolean makeMove = false;				// engine makes first move
    int chessEnginePlayMod = 1;				// 1 = player vs engine, 2 = engine vs player, 3 = engine vs engine, 4 = engine analysis, 5 = player vs player, 6 = edit
	boolean initClockAfterAnalysis = false;
    boolean chessEngineInit = false;
    boolean chessEnginesOpeningBook = false;
    boolean chessEngineSearching = false;
    boolean chessEnginePaused = false;
    boolean chessEngineProblem = false;
    boolean chessEngineAutoRun = false;
    boolean chessEngineAnalysis = false;
    CharSequence chessEnginePlayerWhite = "Me";
    CharSequence chessEnginePlayerBlack = "";
    CharSequence chessEngineEvent = ""; 
    CharSequence chessEngineSite = "";
    CharSequence chessEngineRound = "";

}
