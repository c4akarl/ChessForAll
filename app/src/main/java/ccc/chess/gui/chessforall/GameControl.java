package ccc.chess.gui.chessforall;

import java.util.ArrayList;

import ccc.chess.logic.c4aservice.ChessLogic;
import ccc.chess.logic.c4aservice.ChessMove;

public class GameControl 
{
	GameControl(ArrayList<CharSequence> stringValues, CharSequence mvHistory)
    {
		cl = new ChessLogic(stringValues, mvHistory);
    }
    public void setGameOver(CharSequence gameResult)													
    {	// game over ?
		if 	(		cl.p_moveRank.equals("0")			// rank
    			&	cl.p_moveVariant.equals("0")		// variant
    			&	cl.p_moveIsLastInVariation			// move is last in variation
    			&	!gameResult.equals("*")
    		)
    		isGameOver = true;
    	else
    		isGameOver = false;
//Log.i(TAG, "gameResult, isGameOver: " + gameResult + ", " + isGameOver);
    }
    public CharSequence getValueFromFen(CharSequence fen, int value)	
    {	// get split value from fenMes
    	CharSequence[] split = fen.toString().split(" ");
		if (split.length >= 6 & value > 0 & value < 7)
			return split[value -1];
		else
			return "";
    }
	
    final String TAG = "GameControl";
	ChessLogic cl;					// direct access to ChessLogic, Chess960, ChessHistory
	ChessMove chessMove = new ChessMove();
    CharSequence oldGameResult = "";
	boolean initPrefs = false;		// preferences init (first call after app installation[gameStat = 0])
	CharSequence pgnStat = "-";
	boolean isChess960 = false;
	CharSequence startFen = "";
	CharSequence standardFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
	boolean isAutoLoad = false;
	boolean isAutoPlay = false;
	boolean isGameLoaded = false;
	boolean isBoardTurn = false;
	boolean isGameOver = false;
	boolean isGameUpdated = true; 
	boolean isPlayerPlayer = false;
	boolean isMoveError = false;
	CharSequence fileBase = "";
	CharSequence filePath = "";
	CharSequence fileName = "";
	CharSequence fen = "";
	CharSequence move = "";
	CharSequence promotionMove = "";
	CharSequence startPgn = "";
    int startMoveIdx = 0;
    int autoPlayValue = 1500;
    boolean hasVariations = false;
    ArrayList<CharSequence> variationsList = new ArrayList<CharSequence>();	// for contextMenu (btnVariation)
    CharSequence selectedVariationTitle = "";
    CharSequence errorMessage = "";
    CharSequence errorPGN = "";

}
