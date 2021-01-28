package ccc.chess.gui.chessforall;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;

import ccc.chess.book.BookOptions;
import ccc.chess.book.C4aBook;

public class EngineControl 
{
	EngineControl(Context context)
    {
		this.context = context;
		userPrefs = context.getSharedPreferences("user", 0);
		createEngines(null, null);
    }

	void createEngines(ArrayList<String> oexEngines, EngineListener engineListener)
	{
		if (oexEngines == null) {
			uciEngines = new UciEngine[1];
			engineCnt = 1;
			uciEngines[0] = new UciEngine(context, 0,null, null);
			ue = uciEngines[0];
		}
		else {
			if (oexEngines != null && engineListener != null) {
				if (oexEngines.size() > 0) {
					engineCnt = oexEngines.size();
					uciEngines = new UciEngine[engineCnt];
					for (int i = 0; i < engineCnt; i++)
					{
						uciEngines[i] = new UciEngine(context, i, oexEngines.get(i), engineListener);
					}
				}
			}
		}
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

//Log.i(TAG, "setPlayData(), white: " + white + ", black: " + black);

		chessEngineEvent = "Android " + android.os.Build.VERSION.RELEASE;
		chessEngineSite = android.os.Build.MODEL;
		chessEngineRound = "-";
		if (chessEngineAutoRun)
			chessEngineRound = 	userP.getInt("user_play_eve_round", 1) + "." + userP.getInt("user_play_eve_gameCounter", 1);
		String playerName = userP.getString("user_options_gui_playerName", context.getString(R.string.qPlayer));
		if (playerName.equals("") || playerName.equals("?"))
			playerName = context.getString(R.string.qPlayer);
		switch (chessEnginePlayMod)
		{
			case 1:
				chessEnginePlayerWhite = playerName;
				chessEnginePlayerBlack = getEngine().engineName;
				break;
			case 2:
				chessEnginePlayerWhite = getEngine().engineName;
				chessEnginePlayerBlack = playerName;
				break;
			case 3:
				chessEnginePlayerWhite = getEngine().engineName;
				chessEnginePlayerBlack = getEngine().engineName;
				break;
			case 4:
				chessEnginePlayerWhite = white;
				chessEnginePlayerBlack = black;
				break;
		}
    }

	public UciEngine getEngine()
    {
		//karl multiple engines ?!
		if (MainActivity.withMultiEngine)
    		return uciEngines[0];
		else
			return ue;
    }

	void setStartPlay(CharSequence color)
    {
		if 	(		chessEnginePlayMod == 3
				|	chessEnginePlayMod == 4
				|	chessEnginePlayMod == 1 & color.equals("b")
				| 	chessEnginePlayMod == 2 & color.equals("w")
			)
		{
			getEngine().startPlay = true;
			makeMove = true;
		}
		else
		{
			getEngine().startPlay = false;
			makeMove = false;
		}

//		Log.i(TAG, "setStartPlay(), playMod: " + chessEnginePlayMod + ", color: " + color + ", makeMove: " + makeMove);

    }

	private final Context context;
	private final SharedPreferences userPrefs;		 	// user preferences(LogFile on/off . . .)
	UciEngine ue;										// single UciEngine 			MainActivity.withMultiEngine = false
	UciEngine[] uciEngines;								// manage multiple UciEngine	MainActivity.withMultiEngine = true
	int engineCnt = 1;
	int searchId = 0;
	C4aBook book;
	private final BookOptions bookOptions = new BookOptions();
	boolean makeMove = false;				// engine makes first move
	boolean isUciNewGame = true;			// send "ucinewgame" command
    int chessEnginePlayMod = 1;				// 1 = player vs engine, 2 = engine vs player, 3 = engine vs engine, 4 = engine analysis, 5 = player vs player, 6 = edit
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
