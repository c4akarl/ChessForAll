package ccc.chess.gui.chessforall;

import android.content.Context;
import android.content.SharedPreferences;

//import java.util.ArrayList;

import ccc.chess.book.BookOptions;
import ccc.chess.book.C4aBook;

public class EngineControl 
{
	EngineControl(Context context){
		this.context = context;
		userPrefs = context.getSharedPreferences("user", 0);
		createEngines();
    }

	void createEngines() {
		uciEngines = new UciEngine[1];
		uciEnginesMessage = new String[1];
		engineCnt = 1;
		uciEngines[0] = new UciEngine(context, 0,null, null);
		uciEnginesMessage[0] = "";
		setCurrentEngineId(0);
		ue = uciEngines[0];
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

//		Log.i(TAG, "setPlayData(), white: " + white + ", black: " + black);

		if (!chessEngineMatch)
		{
			chessEngineEvent = "Android " + android.os.Build.VERSION.RELEASE;
			chessEngineSite = android.os.Build.MODEL;
			chessEngineRound = "-";
		}

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
				if (engineCnt == 1) {
					chessEnginePlayerWhite = getEngine().engineName;
					chessEnginePlayerBlack = getEngine().engineName;
				}
				else
				{
					chessEnginePlayerWhite = uciEngines[0].engineName;
					chessEnginePlayerBlack = uciEngines[1].engineName;
				}
				break;
			case 4:
				chessEnginePlayerWhite = white;
				chessEnginePlayerBlack = black;
				break;
		}
    }

	public UciEngine getEngine()
    {
		return uciEngines[currentEngineId];
    }

	void setCurrentEngineId(int engineId)
	{
		currentEngineId = engineId;
	}

//	void initEngineMessages(ArrayList<String> engineNames)
	void initEngineMessages()
	{
		if (uciEnginesMessage != null) {
			for (int i = 0; i < engineCnt; i++) {
				uciEnginesMessage[i] = "";
			}
		}
	}

	boolean enginesRunning()
	{
		if (uciEngines != null) {
			for (int i = 0; i < uciEngines.length; i++) {
				uciEnginesMessage[i] = "";
				if (uciEngines[i] != null) {

//					if (uciEngines[i].engineStop() || uciEngines[i].engineSearching())
					if (uciEngines[i].engineSearching())
						return true;
				}
			}
		}
		return false;
	}

	void setStartPlay(int engineId, CharSequence color)
    {
		if 	(		chessEnginePlayMod == 3
				|	chessEnginePlayMod == 4
				|	chessEnginePlayMod == 1 & color.equals("b")
				| 	chessEnginePlayMod == 2 & color.equals("w")
			)
		{
			uciEngines[engineId].startPlay = true;
			makeMove = true;
		}
		else
		{
			uciEngines[engineId].startPlay = false;
			makeMove = false;
		}

//		Log.i(TAG, "setStartPlay(), playMod: " + chessEnginePlayMod + ", engineId: " + engineId + ", color: " + color + ", makeMove: " + makeMove);

    }

//	final String TAG = "EngineControl";
	private final Context context;
	private final SharedPreferences userPrefs;		 	// user preferences(LogFile on/off . . .)
	UciEngine ue;										// single UciEngine 			MainActivity.withMultiEngine = false
	UciEngine[] uciEngines;								// manage multiple UciEngine	MainActivity.withMultiEngine = true
	String[] uciEnginesMessage;							// engine messages from EngineListener
	int engineCnt = 1;
//	int engineRestartCnt = 0;
	public int currentEngineId = 0;                     // current engineId, idx from uciEngines, return from getEngine()
	public int analysisEngineId = 0;                    // analysis engineId, for making move !
	public int analysisEngineCnt = 0;                   // analysis engineId counter, for making move !
	public String analysisEngineBestMove = "";          // analysis engineId best move, for making move !
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
    boolean chessEngineMatch = false;
    boolean chessEngineAnalysis = false;
    CharSequence chessEnginePlayerWhite = "Me";
    CharSequence chessEnginePlayerBlack = "";
    CharSequence chessEngineEvent = ""; 
    CharSequence chessEngineSite = "";
    CharSequence chessEngineRound = "";

}
