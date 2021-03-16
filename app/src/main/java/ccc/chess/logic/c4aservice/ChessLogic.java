package ccc.chess.logic.c4aservice;

import android.util.Log;

import java.util.ArrayList;

import chesspresso.move.Move;
import chesspresso.position.Position;

public class ChessLogic
{
    public ChessLogic(ArrayList<CharSequence> values, CharSequence mvHistory)
    {	// constructor
    	stringValues = values;
    	moveHistoryPrefs = mvHistory;
    	history = new ChessHistory(values);
    	history.initGameData();
    	chessMove = new ChessMove();
    	chess960 = new Chess960();
    	p_possibleMoveList = new ArrayList<CharSequence>();		// mv1
    	p_possibleMoveToList = new ArrayList<CharSequence>();	// move to
    }

//  C H E S S    L O G I C  -  C H E S S P R E S S O
    public void newPosition(CharSequence chess960ID, CharSequence fen, CharSequence event, CharSequence site,
    		CharSequence date, CharSequence round, CharSequence white, CharSequence black)			// new game	(initialization)
    {
//    	Log.i(TAG, "newPosition()");
    	CharSequence isMate = "f";
    	CharSequence isStaleMate = "f";
     	history.initGameData();
     	history.setFilePath("");
    	history.setFileName("");
     	if (chess960ID.length() >= 8) 
     		chess960.createChessPosition(chess960ID);						// create ChessPosition from startLine(FEN)
     	else
     		chess960.createChessPosition(Integer.parseInt(chess960ID.toString()));	// create ChessPosition from chess960-ID
  		history.setChess960Id(chess960.getChess960Id());					// set chess960 ID in history
  		if (!fen.equals(""))
  			chess960.setFen(fen);
        if (history.getChess960Id() != 518 | !chess960.getFen().equals(history.fenStandardPosition))
        	history.setGameTag("FEN", chess960.getFen().toString());

//		Log.i(TAG, "newPosition(), chess960.getFen(): " + chess960.getFen());

        CharSequence stat = "0";
        CharSequence message = "";
        pos = new ChessPosition(history.chess960Id);
        pos.setPosition(chess960.getFen());
//Log.i(TAG, "isLegal, canMove, isMate, isStaleMate: " + pos.isLegal() + ", " + pos.canMove() + ", " + pos.isMate() + ", " + pos.isStaleMate() );
        if (pos.isLegal() & pos.canMove() & !pos.isMate()  & !pos.isStaleMate())
        {
        	stat = "1";														// processing OK
        	message = "";
      		if (history.getChess960Id() != 518)
      			history.setGameTag("Variant", "chess 960");
            history.setIsGameEnd(false);                            		// history: enable move end 
        }
        else
        {
        	if (pos.isMate())		{stat = "5"; isMate = "t";}
        	if (pos.isStaleMate())	{stat = "6"; isStaleMate = "t";}
        	if (!pos.isMate() & !pos.isStaleMate())
        	{
	        	stat = "2";													
	        	message = stringValues.get(2);		// cl_wrongBasePosition
//Log.i(TAG, "newPosition(), message: " + message);
			}
        	history.setIsGameEnd(true);                             		// ERROR: set game end
            history.moveIsFirstInVariation = true;
            history.moveIsLastInVariation = true;
        }
        history.setGameTag("Event", event.toString());
    	history.setGameTag("Site", site.toString());
    	if (date.equals(""))
    		date = history.getDateYYYYMMDD();
    	history.setGameTag("Date", date.toString());
    	history.setGameTag("Round", round.toString());
    	history.setGameTag("White", white.toString());
    	history.setGameTag("Black", black.toString());
        history.initMoveHistory(chess960.getFen(), isMate, isStaleMate);
        setPositionValues(stat, message);
    }

    public void newPositionFromMove(CharSequence fen, CharSequence mv, Boolean addToHistory)					// new position	(new move)
    {

    	//karl mv --> startMove, if fastMove ends with mv : stat = 7 && switch in MainActivity !!!

//		Log.i(TAG, "newPositionFromMove(), mv: " + mv + ",   fen: " + fen);

//err>>>: at ccc.chess.logic.c4aservice.ChessLogic.newPositionFromMove (ChessLogic.java:119)
		if (mv.length() < 2)
			return;

		CharSequence stat = "9";
    	CharSequence gameResult = "*";
    	CharSequence message = "";
    	CharSequence fastMove = "";
    	boolean gameOver = false;
    	boolean validMove = false;
    	p_variationEnd = false;

		if (p_hasPossibleMovesTo & p_possibleMoveToList.size() >= 2)
		{
			for (int i = 0; i < p_possibleMoveToList.size(); i++)
			{
				if (p_possibleMoveToList.get(i).toString().substring(0, 2).equals(mv))
				{
					mv = p_possibleMoveToList.get(i).toString().substring(0, 4);
					p_hasPossibleMovesTo = false;
					p_possibleMoveToList.clear();
					break;
				}
			}
		}

    	if (mv.length() >= 4)                                               	
        {	// move correction: multiple input
        	if (mv.subSequence(0, 2).equals(mv.subSequence(2, 4)))
        		mv = mv.subSequence(2, mv.length());
        }
     	if (mv.length() == 6)			
		{	// promotion? format e7e8=q  ---> e7e8Q
			if (mv.charAt(5) == 'q') mv = "" + mv.subSequence(0, 4) + 'Q';
			if (mv.charAt(5) == 'r') mv = "" + mv.subSequence(0, 4) + 'R';
			if (mv.charAt(5) == 'b') mv = "" + mv.subSequence(0, 4) + 'B';
			if (mv.charAt(5) == 'n') mv = "" + mv.subSequence(0, 4) + 'N';
		}
    	pos.setPosition(fen);
    	pos.isChess960Castling = false;
//    	Log.i(TAG, "mv, fen: " + mv + ", " + fen);
//    	Log.i(TAG, "mv: " + mv);
//    	Log.i(TAG, "wShortCastC4aLan: " + pos.wShortCastC4aLan);


//err: at ccc.chess.logic.c4aservice.ChessLogic.newPositionFromMove (ChessLogic.java:119)
    	if (pos.isChess960 & (mv.subSequence(0, 2).equals(pos.wK) | mv.subSequence(0, 2).equals(pos.bK)))
		{
    		if (mv.length() >= 4)
    		{
    			pos.isChess960Castling = pos.chess960CanCastling(history.getGameTagValue("FEN"), fen, pos.cpPosition.getToPlay(), mv.subSequence(0, 4));
//    			Log.i(TAG, "moveLAN, canCast: " + mv + ", " + pos.isChess960Castling);
    		}
    		else
    		{

//				Log.i(TAG, "newPositionFromMove(), moveLAN, canCast: " + mv + ", " + pos.isChess960Castling);

    			if (pos.cpPosition.getToPlay() == 0)
    			{	// white
     				if (pos.chess960CanCastling(history.getStartFen(), fen, pos.cpPosition.getToPlay(), pos.wShortCastC4aLan.subSequence(0, 4)))
     					pos.moveList.add(pos.wShortCastC4aLan.toString());
    				if (pos.chess960CanCastling(history.getStartFen(), fen, pos.cpPosition.getToPlay(), pos.wLongCastC4aLan.subSequence(0, 4)))
    					pos.moveList.add(pos.wLongCastC4aLan.toString());
     			}
    			if (pos.cpPosition.getToPlay() == 1)
    			{	// black
    				if (pos.chess960CanCastling(history.getStartFen(), fen, pos.cpPosition.getToPlay(), pos.bShortCastC4aLan.subSequence(0, 4)))
    					pos.moveList.add(pos.bShortCastC4aLan.toString());
    				if (pos.chess960CanCastling(history.getStartFen(), fen, pos.cpPosition.getToPlay(), pos.bLongCastC4aLan.subSequence(0, 4)))
    					pos.moveList.add(pos.bLongCastC4aLan.toString());
    			}
    		}
		}

//    	Log.i(TAG, "mv chess960: " + mv);

    	if (mv.length() == 2)			
		{	// fast move
    		fastMove = pos.getFastMove(mv);

//			Log.i(TAG, "mv chess960: " + mv + ", fastMove: " + fastMove + ", pos.isChess960Castling: " + pos.isChess960Castling);

    		switch (pos.fast_move)
            {
    	        case 0:     // FAST_MOVE_NO_MOVE
//Log.i(TAG, "fast move");
    	        	message = stringValues.get(9)  + ": " + mv;
    	        	if (pos.isSquareEmpty(pos.getSQI(mv.subSequence(0, 2))))
    	        		message = stringValues.get(13);	// no move
    	        	if (pos.isCheck())
    	        		message = stringValues.get(17);	// check!
    	        	if (pos.getToPlay() == 0 & pos.getColor(pos.getSQI(mv.subSequence(0, 2))) == 1)
    	        		message = stringValues.get(10);	// white to move!
    	        	if (pos.getToPlay() == 1 & pos.getColor(pos.getSQI(mv.subSequence(0, 2))) == 0)
    	        		message = stringValues.get(11);	// black to move!
    	        	break;
    	        case 1:     // FAST_MOVE_OK
    	        	mv = fastMove;
    	            break;
    	        case 2:     // FAST_MOVE_MULTIPLE_FROM
    	        	stat = "0";
    	            break;
    	        case 3:     // FAST_MOVE_MULTIPLE_TO
    	        	stat = "2";
    	            break;
    	        case 4:     // FAST_MOVE_PROMOTION_TO
    	        	stat = "5";
    	        	mv = fastMove;
    	            break;
            }

//Log.i(TAG, "fastMove: " + fastMove + "\npos.fast_move: " + pos.fast_move + ", stat: " + stat + ", message: " + message);

		}
		//err java.lang.StringIndexOutOfBoundsException: 12. Mai 15:10 in der App-Version 67
		if (mv.length() >= 2)
		{
			if (!pos.isChess960 & pos.cpPosition.getPiece(pos.getSQI(mv.subSequence(0, 2))) == 6)
			{
				if (mv.equals(pos.wShortCastC4aLan.subSequence(0, 4)))
					mv = mv.subSequence(0, 2).toString() + pos.WK_SHORT;
				if (mv.equals(pos.wLongCastC4aLan.subSequence(0, 4)))
					mv = mv.subSequence(0, 2).toString() + pos.WK_LONG;
				if (mv.equals(pos.bShortCastC4aLan.subSequence(0, 4)))
					mv = mv.subSequence(0, 2).toString() + pos.BK_SHORT;
				if (mv.equals(pos.bLongCastC4aLan.subSequence(0, 4)))
					mv = mv.subSequence(0, 2).toString() + pos.BK_LONG;
			}
		}
    	validMove = pos.validMove(mv);
//    	Log.i(TAG, "mv, validMove: " + mv + ", " + validMove);
    	if (pos.isPromotion)
    		fastMove = mv;

//     	if (mv.length() >= 4 & !pos.isPromotion)
     	if (mv.length() >= 4 & !pos.isPromotion & addToHistory)
		{
    		if (validMove)
    		{
    			stat = "1";
    			int piece = 0;
    			if (mv.length() == 5)
    			{
    				if (mv.charAt(4) == 'Q') piece = 4;
    				if (mv.charAt(4) == 'R') piece = 3;
    				if (mv.charAt(4) == 'B') piece = 2;
    				if (mv.charAt(4) == 'N') piece = 1;
    			}
    			if (pos.isChess960Castling)
    				pos.doChess960Castling(pos.cpPosition, mv);
    			else
    				pos.doMove(pos.getSQI(mv.subSequence(0, 2)), pos.getSQI(mv.subSequence(2, 4)), piece);
    			CharSequence mvPgn = pos.getSAN();
    			if (pos.isChess960)
    			{
    				pos.chess960SetFenCastling(mv);
    				CharSequence tmpFen = pos.cpPosition.getFEN();

//					Log.i(TAG, "1 newPositionFromMove(), mv: " + mv + ", tmpFen: " + tmpFen);

     				pos.cpPosition = new Position(pos.chess960SetNewFEN(tmpFen, pos.chess960OldCast).toString());

//					Log.i(TAG, "2 newPositionFromMove(), newFen: " + pos.cpPosition.getFEN());

    			}
	    		CharSequence newFen = pos.cpPosition.getFEN();
	    		CharSequence moveText = "";
	            CharSequence isCheck = "f";
	            CharSequence isMate = "f";
	            CharSequence isStaleMate = "f";
	            if (pos.isCheck()) isCheck = "t";
	            if (pos.isMate()) isMate = "t";
	            if (pos.isStaleMate()) isStaleMate = "t";
//	            Log.i(TAG, "mv, mvPgn, validMove: " + mv + ", " + mvPgn + ", " + validMove);
//	            Log.i(TAG, "newFen: " + newFen);
//	            Log.i(TAG, "isCheck, isMate, isStealMate: " + isCheck + ", " + isMate + ", " + isStaleMate);
	            chessMove.setMoveFromHistory(history.moveHistory.get(history.getMoveIdx()));
	            
	            // set gameResult + gameMessage
	            if (pos.isMate() | pos.isStaleMate())
	            {
	            	if (chessMove.getRank().equals("0"))
	            	{
	            		gameOver = true;
	            		if (pos.isMate())
	            		{
		            		if (pos.isWhiteMove())
		            			gameResult = stringValues.get(3);
		                    else
		                    	gameResult = stringValues.get(4);
	            		}
	            		if (pos.isStaleMate())
	            			gameResult = stringValues.get(5);
	            		message = gameResult.toString() + " (" + stringValues.get(6) + ")";
		                history.setGameTag("Result", gameResult.toString());
	            	}
	            	else
	            	{
	            		p_variationEnd = true;
	            	}
	            }
	            if (pos.getHalfMoveClock() >= 100)
	            {
	            	gameOver = true;
	            	gameResult = stringValues.get(5);
	            	if (!chessMove.getRank().equals("0"))
	            		moveText = "$12";
	            	message = gameResult.toString() + " (" + stringValues.get(6) + ", " + stringValues.get(7) + ")";
	            }
	            if (gameStat != 2)
	            {
	    	        if (history.getCountEvenPosition(newFen) >= 3)
	    	        {
	    	            gameOver = true;
		            	gameResult = stringValues.get(5);
		            	if (!chessMove.getRank().equals("0"))
		            		moveText = "$12";
		            	message = gameResult.toString() + " (" + stringValues.get(6) + ", " + stringValues.get(8) + ")";
	    	        }
	            }
	            chessMove.setMove(chessMove.getRank(), chessMove.getVariant(), mv, mvPgn, newFen, moveText, 
	            		isCheck, isMate, isStaleMate, chessMove.getFen(), chessMove.getIsCheck(), "1");
	            history.addToMoveHistory(chessMove, true);
    		}
    		else
    		{
//Log.i(TAG, "mv, validMove: " + mv + ", " + validMove);
    			stat = "2";
	        	message = stringValues.get(13);
    		}
		}

		if (mv.length() >= 4 & !pos.isPromotion & !addToHistory & validMove)
		{
			quickMove = fastMove;
			stat = "2";
		}

    	if (pos.isPromotion)
			stat = "5";
    	if (gameOver)                    					                       	// GameOver!
        {
        	if (chessMove.getRank().equals("0") & chessMove.getVariant().equals("0"))	// main move section
        		history.setGameTag("Result", gameResult.toString());
        	else
        		p_variationEnd = true;
        }
        else
        {
        	if 	(	chessMove.getRank().equals("0") & chessMove.getVariant().equals("0") 
        			& history.getIsGameEnd() 
        		)	// main move section
        		history.setGameTag("Result", "*");
        }

		p_hasPossibleMovesTo = false;
		if (stat.equals("0"))
			setPossileMoves(fastMove);
		else
		{
			p_hasPossibleMoves = false;
			if (pos.fast_move == 3 & stat.equals("2"))
				setPossileMovesTo(mv, pos.moveList);
		}


//		Log.i(TAG, "newPositionFromMove(), stat: " + stat + ", pos.moveList: " + pos.moveList.size());

		setPositionValues(stat, message);
		if (pos.isPromotion)
 			p_move = fastMove;
		if (stat.equals("0"))
		{
			p_move = mv;
			p_move1 = mv;
		}
    }

    public void newPositionFromFen(CharSequence fen)						// new game	from a FEN(initialization)
    {
    	if (fen.equals(""))
    		return;
    	CharSequence stat = "0";
    	CharSequence isMate = "f";
    	CharSequence isStaleMate = "f";
     	history.initGameData();
     	history.setFilePath("");
    	history.setFileName("");
  		history.setChess960Id(518);											// set chess960 ID in history
  		pos = new ChessPosition(history.chess960Id);
        CharSequence message = "";
        pos.setPosition(fen);
        if (pos.isLegal() & pos.canMove() & !pos.isMate()  & !pos.isStaleMate())
        {
        	stat = "1";														// processing OK
        	message = "";
      		history.setGameTag("FEN", fen.toString());
            history.setIsGameEnd(false);                            		// history: enable move end 
        }
        else
        {
        	if (pos.isMate())		{stat = "5"; isMate = "t";}
        	if (pos.isStaleMate())	{stat = "6"; isStaleMate = "t";}
        	if (!pos.isMate() & !pos.isStaleMate())
        	{
	        	stat = "2";													
	        	message = stringValues.get(2);								// cl_wrongBasePosition
//Log.i(TAG, "newPositionFromFen(), message: " + message);
        	}
        	history.setIsGameEnd(true);                             		// ERROR: set game end
            history.moveIsFirstInVariation = true;
            history.moveIsLastInVariation = true;
        }
        history.getDateYYYYMMDD();
        history.initMoveHistory(fen, isMate, isStaleMate);
        setPositionValues(stat, message);
    }

    public void newPositionFromPgnData(CharSequence fBase, CharSequence fPath, CharSequence fName, CharSequence gameData,
    		boolean isEndPosition, int moveIdx, boolean withMoveHistory)					
    {	// game PGN-data (file)
//    	Log.i(TAG, "newPositionFromPgnData(), gameData: " + gameData);
    	history.initGameData();
    	history.setFileBase(fBase);
    	history.setFilePath(fPath);
    	history.setFileName(fName);
        history.setGameData(gameData);
        CharSequence stat = "0";
        CharSequence message = "";
        if (!history.getStartFen().equals(""))
        {
        	chess960.createChessPosition(history.getStartFen());				// creating chess960-ID from FEN
        	history.setChess960Id(chess960.getChess960Id());				// set Chess960 ID in history
        	pos = new ChessPosition(history.chess960Id);
//        	Log.i(TAG, "history.getGameFen(): "+ history.getGameFen());
//        	Log.i(TAG, "chess960.fen: "+ chess960.fen);
            if (withMoveHistory & !moveHistoryPrefs.equals(""))	// moveHistory from runPrefs
            {
            	history.pErrorMessage = "";
            	String[] tmpList = moveHistoryPrefs.toString().split("\n");
            	for(int i = 0; i < tmpList.length; i++)
                {
            		history.moveHistory.add(tmpList[i]);
                }
            }
            else
            {
            	if (history.getGameNotation().toString().equals(" *") | history.getGameNotation().toString().equals("\n *\n"))
            	{
            		newPositionFromFen(history.getStartFen());
            		return;
            	}
            	else
            	{
            		message = createMoveHistory();
            	}
            }
            if (message.equals(""))
            {
            	try 
            	{
	            	if (isEndPosition)							// end position
	            		history.setIsGameEnd(true);
	            	else
	            	{
		                history.setIsGameEnd(false);                        	// start position | current position
		                history.setMoveIdx(moveIdx);
		                if (history.moveHistory.size() -1 == moveIdx)
		                {
		                	history.setIsGameEnd(true);
		                	message = history.getGameText();
		                }
	            	}
	            	if (history.getIsGameEnd() & history.getMoveIdx() > 0)		// last move(history): moveIdx -1
	            		history.setMoveIdx(history.getMoveIdx() -1);
	                stat = "1";	
	                
	                history.setNextMoveHistory(0);// processing OK
	                getPositionFromMoveHistory(0, 0);
            	}
            	catch (IndexOutOfBoundsException e) {e.printStackTrace();}
//                Log.i(TAG, "mvIdx, moveFen, isEnd: " + history.getMoveIdx() + ", " + moveFen + ", " + isEndPosition);
            }
            else
            {
            	stat = "3";														// error message: Notation Error

            }
        }
        else
        {
        	stat = "2";															// error message: FEN Error
        	message = stringValues.get(22) + ": " + history.getStartFen();	// cl_fenError			
        }
        setPositionValues(stat, message);
    }

    public void getPositionFromMoveHistory(int keyState, int moveIdx)				// get position from moveHistory
    {
    	CharSequence stat = "0";
    	CharSequence message = "";
    	
    	if (keyState == 19)
    		history.setMoveIdx(moveIdx);
    	history.setNextMoveHistory(keyState);                        	// activate next logic move in history (keyState) 
        stat = "1";														// processing OK 
		message = history.getMoveInfo(history.getMoveIdx());			// Info text-2: move info
		ChessMove cm = new ChessMove();
		cm.setMoveFromHistory(history.moveHistory.get(history.getMoveIdx()));
		if (message.equals("*"))
			message = "";
		setPositionValues(stat, message);
    }

    public void deleteMovesFromMoveHistory(boolean deleteMoveIdx)			// delete all moves from moveIdx to variation end in History
    {

//    	Log.i(TAG, "history.getMoveIdx(): " + history.getMoveIdx());

    	if (history.getMoveIdx() > 0)                                  	// moves in History?
        {
    		if (deleteMoveIdx)
    		{
    			history.deleteMovesFromHistory(history.getMoveIdx());
    		}
    		else
    		{
   				history.deleteMovesFromHistory(history.getMoveIdx() +1);
    		}
    		if (history.moveHistory.size() == 0)
    			history.initMoveHistory(history.getStartFen(), "f", "f");
    		getPositionFromMoveHistory(0, 0);
        }
    }

    public CharSequence getNotationFromInfoPv(CharSequence fen, CharSequence pvMoves)		// notation(PGN) from infoPV moves
    {

//    	Log.i(TAG, "getNotationFromInfoPv, fen: " + fen);
//    	Log.i(TAG, "getNotationFromInfoPv, pvMoves: " + pvMoves);

    	posPV = new ChessPosition(history.chess960Id);
    	String pgnMoves = "";
    	CharSequence mvPgn = "";
    	CharSequence pgnNotation = "";
    	boolean validMove = false;
    	boolean isStart = true;
    	CharSequence[] moveSplit = pvMoves.toString().split(" ");
		if (moveSplit.length > 0)
		{
			for (int i = 0; i < moveSplit.length; i++)
			{
				posPV.setPosition(fen);
				CharSequence move = moveSplit[i];
				if (move.length() > 4)			// promotion
				{
					if (move.charAt(4) == 'q') move = ((String) move).substring(0, 4) + 'Q';
					if (move.charAt(4) == 'r') move = ((String) move).substring(0, 4) + 'R';
					if (move.charAt(4) == 'b') move = ((String) move).substring(0, 4) + 'B';
					if (move.charAt(4) == 'n') move = ((String) move).substring(0, 4) + 'N';
				}
				posPV.chess960SAN = "";
				posPV.isChess960Castling = false;
				if (posPV.isChess960)
				{
					posPV.chess960SetCanCast(fen);
					if (posPV.canCastWS & move.equals(posPV.wShortCastC4aLan.subSequence(0, 4).toString())) posPV.chess960SAN = "O-O";
					if (posPV.canCastWL & move.equals(posPV.wLongCastC4aLan.subSequence(0, 4).toString())) posPV.chess960SAN = "O-O-O";
					if (posPV.canCastBS & move.equals(posPV.bShortCastC4aLan.subSequence(0, 4).toString())) posPV.chess960SAN = "O-O";
					if (posPV.canCastBL & move.equals(posPV.bLongCastC4aLan.subSequence(0, 4).toString())) posPV.chess960SAN = "O-O-O";
					if (!posPV.chess960SAN.equals(""))
						posPV.isChess960Castling = true;
				}

				validMove = posPV.validMove(move);

//				if (move.equals("e1g1")) {
//					Log.i(TAG, "getNotationFromInfoPv, fen: " + fen);
//					Log.i(TAG, "getNotationFromInfoPv, move: " + move + ", posPV.isChess960: " + posPV.isChess960 + ", isChess960Castling: " + posPV.isChess960Castling + ", validMove: " + validMove);
//					Log.i(TAG, "getNotationFromInfoPv, posPV.canCastWS: " + posPV.canCastWS + ", posPV.wShortCastC4aLan: " + posPV.wShortCastC4aLan);
//				}

				if (move.length() >= 4)			
				{
		    		if (validMove)
		    		{
		    			int piece = 0;
		    			if (move.length() == 5)
		    			{
		    				if (move.charAt(4) == 'Q') piece = 4;
		    				if (move.charAt(4) == 'R') piece = 3;
		    				if (move.charAt(4) == 'B') piece = 2;
		    				if (move.charAt(4) == 'N') piece = 1;
		    			}
		    			if (posPV.isChess960Castling)
		    			{
		    				posPV.doChess960Castling(posPV.cpPosition, move);
		    				if (posPV.isChess960Error)
		    					return pgnMoves;
		    			}
		    			else
		    				posPV.doMove(posPV.getSQI(move.subSequence(0, 2)), posPV.getSQI(move.subSequence(2, 4)), piece);
		    			mvPgn = posPV.getSAN();
		    			if (mvPgn.equals(""))
		    				return "";
		    			if (posPV.isChess960)
		    			{
		    				posPV.chess960SetFenCastling(move);
		    				CharSequence tmpFen = posPV.cpPosition.getFEN();
		    				posPV.cpPosition = new Position(posPV.chess960SetNewFEN(tmpFen, posPV.chess960OldCast).toString());
		    			}
			    		fen = posPV.cpPosition.getFEN();
			    		if (isStart)
			    		{
			    			isStart = false;
			    			if (posPV.isWhiteMove())
			    				pgnMoves = posPV.getMoveNumber() + "." + mvPgn.toString() + " ";
			    			else
			    				pgnMoves = posPV.getMoveNumber() + "... " + mvPgn.toString() + " ";
			    		}
			    		else
			    		{
			    			if (posPV.isWhiteMove())
			    				pgnMoves = pgnMoves + posPV.getMoveNumber() + "." + mvPgn.toString() + " ";
			    			else
			    				pgnMoves = pgnMoves + mvPgn.toString() + " ";
			    		}
		    		}
				}
			}
			pgnNotation = pgnMoves;	// pgnMoves + moveNumber
//			Log.i(TAG, "NotationPGN: " + pgnNotation);
		}
    	return pgnNotation;
    }

//  C H E S S    L O G I C  -  C H E S S P R E S S O   (intern)
    public void initPositionValues()	
    {	// init position values (p_xxx)
    	p_stat = "";
        p_message = "";
        p_fen = "";
        p_color = "w";
        p_chess960ID = 518;
        p_moveIdx = 0;
        p_move = "";
        p_movePgn = "";
        p_move1 = "";
        p_move2 = "";
        p_moveShow1 = "";
        p_moveShow2 = "";
        p_moveText = "";
        p_moveRank = "";
        p_moveVariant = "";
        p_moveIsFirst = false;
        p_moveHasVariations = false;
        p_moveIsFirstInVariation = false;
        p_moveIsLastInVariation = false;
        p_gameEnd = false;
        p_gameOver = false;
        p_mate = false;
        p_stalemate = false;
		p_auto_draw = false;
    }

    public void setPositionValues(CharSequence stat, CharSequence message)	
    {	// set position values (p_xxx)
//    	Log.i(TAG, "start setPositionValues, message: " + message);
    	try
    	{
    		initPositionValues();
    		chessMove.setMoveFromHistory(history.moveHistory.get(history.getMoveIdx()));
//    		Log.i(TAG, "Matt, Patt: " + chessMove.getIsMate() + ", " + chessMove.getIsStealMate());
    		p_stat = stat;											// processing status
	    	if (history.getMoveIdx() < 0 | history.getMoveIdx() > history.moveHistory.size())
	    	{
	    		if (history.moveHistory.size() > 0)
	    			history.setMoveIdx(1);
	    		else
	    			history.setMoveIdx(0);
	    	}
	    	p_message = message;									// info/error message
	    	p_fen = history.getMoveFen(history.getMoveIdx());		// FEN
//	    	Log.i(TAG, "history.getMoveIdx(), p_fen: " + history.getMoveIdx() + ", " + p_fen);
	    	p_color = history.getValueFromFen(2, p_fen);			// active color (w/b)
	    	p_chess960ID = history.getChess960Id();					// Chess960 ID (Integer))
	    	p_moveIdx = history.getMoveIdx();						// Move-Index
	    	p_move = history.getMove(history.getMoveIdx());  		// move
	    	p_movePgn = history.getMovePgn(history.getMoveIdx());	// move (PGN)
	    	if (history.getMove(history.getMoveIdx()).length() >= 4)
			{	// for showing moves on chessboard
	    		p_move1 = history.getMove(history.getMoveIdx()).subSequence (0, 2);
	    		p_move2 = history.getMove(history.getMoveIdx()).subSequence (2, 4);
	    		if (p_movePgn.equals("O-O") | p_movePgn.equals("O-O-O"))
				{
					CharSequence castKing = "c";
					CharSequence castRook = "d";
					if (p_movePgn.equals("O-O"))
					{
						castKing = "g";
						castRook = "f";
					}
					p_moveShow1 = castKing.toString() + p_move1.subSequence(1, 2);
					p_moveShow2 = castRook.toString() + p_move2.subSequence(1, 2);
				}
			}
	    	p_moveText = history.getMoveTxt(history.getMoveIdx());			// Text to a move
	    	if (history.getMoveIsFirstInVariation() & history.getMoveIdx() > 0)
	    	{
	    		ChessMove cmTmp = new ChessMove();
	    		cmTmp.setMoveFromHistory(history.moveHistory.get(history.getMoveIdx() -1));
	    		if (!cmTmp.getTxt().equals(""))
	    		{
	    			p_moveText = cmTmp.getTxt() + "  .  .  .\n" + p_moveText;
	    		}
	    	}
	    	p_moveRank = history.getMoveRank();								// variation: moveRank
	    	p_moveVariant = history.getMoveVariant();						// variation: moveVariant
	    	p_moveIsFirst = history.getMoveIsFirst();						// first move in gameNotation!
	    	p_moveHasVariations = history.getMoveHasVariations();			// move has variations?
	    	p_moveIsFirstInVariation = history.getMoveIsFirstInVariation();	// variation start
	    	p_moveIsLastInVariation = history.getMoveIsLastInVariation();	// variation end
	    	if (history.moveHistory.size() -1 == p_moveIdx)					// game end (last move idx)
	    		p_gameEnd = true;
	    	else
	    		p_gameEnd = false;
			cntPieces(p_fen);
			p_auto_draw = isAutoDraw();										// auto draw
			if (p_auto_draw)
				history.setGameTag("Result", "1/2-1/2");
	    	if (p_gameEnd & !history.getGameTagValue("Result").equals("*"))	// game over
	    		p_gameOver = true;
//Log.i(TAG, "setPositionValues(), p_gameOver: " + p_gameOver + ", p_auto_draw: " + p_auto_draw);
	    	if (chessMove.getIsMate().equals("t"))							// mate
	    		p_mate = true;
	    	else
	    		p_mate = false;	
	    	if (chessMove.getIsStealMate().equals("t"))						// stalemate
	    		p_stalemate = true;
	    	else
	    		p_stalemate = false;

    	}
        catch (IndexOutOfBoundsException e) {e.printStackTrace(); p_stat = "0";}
//    	Log.i(TAG, "p_stat, p_fen: " + p_stat + ", " + p_fen);
//    	Log.i(TAG, "p_move, p_move1, p_move2: " + p_move + ", " + p_move1 + ", " + p_move2);
    }

    private CharSequence createMoveHistory()	                               	
    {	// create move-History from PGN-Data
    	CharSequence errorMessage = "";
    	try 
    	{
    		ChessMove cmPrev = new ChessMove();
    		CharSequence fen = "";
    		CharSequence move = "";
    		history.pErrorMessage = "";
//Log.i(TAG, "1 createMoveHistory()");
    		history.parseGameNotation(history.getGameNotation());
    		pos.cpPgnPosition = new Position();
    		pos.setPosition(history.getStartFen());
    		for(int i = 0; i < history.moveList.size(); i++)
            {	// creating history.moveHistory from history.moveList
    			// chessMove : set current move variables from moveList(parse PGN-move section)
    			chessMove.setMoveFromMoveList(history.moveList.get(i).toString());
    			if (chessMove.getControl().equals("1"))		// move!
    			{
	    			// cmPrev : get previous move variables
	    			cmPrev.setMoveFromHistory(	history.getPreviousValueFromMoveHistory(history.getMoveHistorySize(),
	    										chessMove.getRank(), chessMove.getVariant(), chessMove.getControl()));
	    			String[] possibleSanMoves = getPossibleSanMoves(cmPrev.getFen());
					if (chessMove.getPgn().toString().equals("--") & possibleSanMoves.length > 0)
						chessMove.setPgn(possibleSanMoves[0]);
					if (chessMove.getPgn().toString().startsWith("Px"))
					{
						CharSequence sanM = getSanMoveFromPx(possibleSanMoves, chessMove.getPgn(), "P");
						chessMove.setPgn(sanM);
					}
					if (chessMove.getPgn().toString().endsWith("+"))
					{
						CharSequence sanM = getSanMoveIsMate(possibleSanMoves, chessMove.getPgn());
						chessMove.setPgn(sanM);
					}
					if (chessMove.getPgn().length() >= 4)
					{
						if 	(		Character.isLowerCase(chessMove.getPgn().charAt(0))
								& 	Character.isLowerCase(chessMove.getPgn().charAt(2))
								&	Character.isDigit(chessMove.getPgn().charAt(1))
								&	Character.isDigit(chessMove.getPgn().charAt(3))
							)
						{
							int lastChar = chessMove.getPgn().length() -1;
							if 	(		Character.isUpperCase(chessMove.getPgn().charAt(lastChar))
									& 	chessMove.getPgn().charAt(lastChar -1) != '='
								)
							{
								String replaceFrom = "" + chessMove.getPgn().charAt(lastChar);
								String replaceTo = "=" + chessMove.getPgn().charAt(lastChar);
								chessMove.setPgn(chessMove.getPgn().toString().replace(replaceFrom, replaceTo));
							}
							String replace = "" + chessMove.getPgn().charAt(0) + chessMove.getPgn().charAt(1);
							CharSequence sanM = getSanMoveFromPx(possibleSanMoves, chessMove.getPgn(), replace);
							chessMove.setPgn(sanM);
						}
					}

					if (chessMove.getPgn().toString().startsWith("O-"))
					{
						CharSequence castleMove = getCastleCheck(possibleSanMoves, chessMove.getPgn());
//	Log.i(TAG, "sanMove: " + chessMove.getPgn() + ", castleMove: " + castleMove);
						chessMove.setPgn(castleMove);
					}

					if (chessMove.getPgn().length() == 4 & !chessMove.getPgn().toString().contains("x"))
					{
						CharSequence correctlyLanMove = getCorrectlyLanMove4(possibleSanMoves, chessMove.getPgn());
//Log.i(TAG, "correctlyLanMove: " + chessMove.getPgn() + ", correctlyLanMove: " + correctlyLanMove);
						chessMove.setPgn(correctlyLanMove);
					}

                    if (chessMove.getPgn().length() > 4)
                    {
                        CharSequence correctlyLanMove = getCorrectlyLanMove5(possibleSanMoves, chessMove.getPgn());
//Log.i(TAG, "correctlyLanMove: " + chessMove.getPgn() + ", correctlyLanMove: " + correctlyLanMove);
                        chessMove.setPgn(correctlyLanMove);
                    }

	    			// chessMove set baseFen, baseIsCheck, even variation(rank, variant)
	    			if (		chessMove.getRank().equals(cmPrev.getRank()) 
	    					& 	chessMove.getVariant().equals(cmPrev.getVariant()))
	    			{
	        			chessMove.setBaseFen(cmPrev.getFen());
	        			chessMove.setBaseIsCheck(cmPrev.getIsCheck());
	    			}
	    			// chessMove set baseFen, baseIsCheck, new variation(rank, variant)
	    			else
	    			{
	    				fen = history.getBaseFenFromLastMove(history.getMoveHistorySize() -1,
								chessMove.getRank(), chessMove.getVariant());
	    				if (!fen.equals(""))
		    				chessMove.setBaseFen(fen);
	    				else
	            			return stringValues.get(23) + " [P1]\n" + chessMove.getPgn() + " (Index: " + i + ")";
	    			}
	    			if (chessMove.getBaseFen().equals(""))
	    			{
	    				errorMessage = stringValues.get(23) + " [P2]\n" + chessMove.getPgn() + " (Index: " + i + ")";
	    				history.gameText = stringValues.get(23) + ": " + chessMove.getPgn();	// cl_notationError
	    				break;
	    			}

					move = pos.getLanMoveFromSanMove(cmPrev.getFen(), possibleSanMoves, chessMove.getPgn());

//Log.i(TAG, "lanMove: " + move + ", sanMove: " + chessMove.getPgn() + ", pos.posSanMove: " + pos.posSanMove);

	                chessMove.setFields(move);

					if (move.length() >= 4)
	                {
	                    fen = pos.getFEN(pos.cpPgnPosition);
//	                    Log.i(TAG, "pos.pgnFen: " + pos.getFEN(pos.cpPgnPosition));
	                    if (!fen.equals(""))
	            		{
	    	    	    	chessMove.setFen(fen);
	    	    	    	if (chessMove.getPgn().toString().endsWith("+"))
	    	    	    		chessMove.setIsCheck("t");
	    	    	    	else
	    	    	    		chessMove.setIsCheck("f");
	    	    	    	if (chessMove.getPgn().toString().endsWith("#"))
	    	    	    		chessMove.setIsMate("t");
	    	    	    	else
	    	    	    		chessMove.setIsMate("f");
	   	    	    		chessMove.setIsStealMate("f");
	    	                history.addToMoveHistory(chessMove, false);	// add new move variables to moveHistory
	            		}
	            		else
						{
							return stringValues.get(23) + " [P3]\n" + chessMove.getPgn() + " (Index: " + i + ")";
						}
	                }
	                else
					{
//Log.i(TAG, "3 move: " + move);
						if (pos.isFenError)
							return stringValues.get(22) + "\n" + "FEN: " + pos.fen;
						else
						{
							CharSequence moveNR = history.getValueFromFen(6, cmPrev.getFen());
							String pt = ". ";
							if (history.getValueFromFen(2, cmPrev.getFen()).equals("b"))
								pt = "... ";
							return stringValues.get(23) + " [P5]:\n" + moveNR + pt + chessMove.getPgn();
						}
					}
    			}
    			else	// variation
    			{
    				if (chessMove.getControl().equals("0"))	// start variation 
    				{
    					if (chessMove.getRank().equals("0") & chessMove.getVariant().equals("0"))	// main
    					{
     						chessMove.setFen(history.getStartFen());
    						chessMove.setBaseFen(history.getStartFen());
    						history.addToMoveHistory(chessMove, false);
    					}
    					else
    					{
    						// variation change, set FEN
      						fen = history.getBaseFenForNewVariation(history.getMoveHistorySize() -1, 
    								chessMove.getRank(), chessMove.getVariant());
      						if (!fen.equals(""))
    	            		{
	    						chessMove.setFen(fen);
	    						chessMove.setBaseFen(fen);
	    						history.addToMoveHistory(chessMove, false);
    	            		}
      						else
    	            			return stringValues.get(23) + " [P4]\n" + chessMove.getPgn() + " (Index: " + i + ")";
    					}
    				}
    				if (chessMove.getControl().equals("9"))		// end variation
    				{
    					cmPrev.setMoveFromHistory(	history.getPreviousValueFromMoveHistory(history.getMoveHistorySize(), 
    												chessMove.getRank(), chessMove.getVariant(), chessMove.getControl()));
    					chessMove.setFen(cmPrev.getFen());
    					history.addToMoveHistory(chessMove, false);
    				}
    			}
            }
//Log.i(TAG, "9 createMoveHistory(), end");
    	}
    	catch (IndexOutOfBoundsException e) {e.printStackTrace();}
    	if (!history.pErrorMessage.equals(""))
    		errorMessage = history.pErrorMessage;
//    	history.printMoveHistory();	// TEST only
    	return errorMessage;
    }

	private String[] getPossibleSanMoves(CharSequence fen)
	{
//Log.i(TAG, "1 getPossibleSanMoves(), fen: >" + fen + "< history.chess960Id: " + history.chess960Id);

		String[] possibleMoves = null;
		try
		{
			ChessPosition posPX = new ChessPosition(history.chess960Id);
			posPX.setPosition(fen);
			short[] moves = posPX.cpPosition.getAllMoves();
			Move.normalizeOrder(moves);
//Log.i(TAG, "2 getPossibleSanMoves(), moves.length: " + moves.length);
//			String strMoves = posPX.cpPosition.getMovesAsString(moves, true);
			String strMoves = posPX.cpPosition.getMovesAsString(moves, false);
			strMoves = strMoves.replace("{", "");
			strMoves = strMoves.replace("}", "");
//Log.i(TAG, "getPossibleSanMoves(), fen: " + fen);
//Log.i(TAG, "getPossibleSanMoves(), allSanMoves: " + strMoves);
			possibleMoves = strMoves.split(",");
		}
		catch (RuntimeException e) {e.printStackTrace();}
		return possibleMoves;
	}

	private CharSequence getSanMoveIsMate(String[] possibleMoves, CharSequence move)
	{
		String moveX = move.toString().replace("+", "#");
		if (possibleMoves != null)
		{
			for (int i = 0; i < possibleMoves.length; i++)
			{
				if 	(possibleMoves[i].equals(moveX)					)
					return possibleMoves[i];
			}
		}
		return move;
	}

	private CharSequence getSanMoveFromPx(String[] possibleMoves, CharSequence move, String replace)
	{
		String moveX = move.toString();
		moveX = moveX.replace(replace, "");
		if (possibleMoves != null)
		{
			for (int i = 0; i < possibleMoves.length; i++)
			{
				if (possibleMoves[i].endsWith(moveX) & Character.isLowerCase(possibleMoves[i].charAt(0)))
				{
					return possibleMoves[i];
				}
			}
		}
		return move;
	}

	private CharSequence getCastleCheck(String[] possibleMoves, CharSequence move)
	{
		if (!move.toString().startsWith("O-") | possibleMoves == null | move.toString().endsWith("+"))
			return move;
		else
		{
			for (int i = 0; i < possibleMoves.length; i++)
			{
				if (possibleMoves[i].startsWith("O-") & possibleMoves[i].endsWith("+") & possibleMoves[i].startsWith(move.toString()))
				{
					return possibleMoves[i];
				}
			}
		}
		return move;
	}

	private CharSequence getCorrectlyLanMove4(String[] possibleMoves, CharSequence move)
	{
		if (move.length() != 4)
			return move;
		else
		{
			if (!Character.isUpperCase(move.charAt(0)))
				return move;
			String checkMove = "" + move.charAt(0)+ move.charAt(2)+ move.charAt(3);
			for (int i = 0; i < possibleMoves.length; i++)
			{
				if 	(possibleMoves[i].equals(checkMove)					)
					return possibleMoves[i];
			}
		}
		return move;
	}

    // e.g.: Nb8d7 ---> Nbd7
    private CharSequence getCorrectlyLanMove5(String[] possibleMoves, CharSequence move)
    {
        if (!Character.isUpperCase(move.charAt(0)) | move.charAt(0) == 'O')
            return move;
        String sanToField = "";
        char sanFromChar1 = ' ';
        char sanFromChar2 = ' ';
        if (move.length() > 4)
        {
            sanToField = move.toString().substring(move.length() - 2);
            if (move.toString().endsWith("+"))
                sanToField = move.toString().substring(move.length() - 3);
            sanFromChar1 = move.toString().charAt(1);
            if (Character.isDigit(move.toString().charAt(2)))
                sanFromChar2 = move.toString().charAt(2);
        }
        for (int i = 0; i < possibleMoves.length; i++)
        {
            if 	(move.length() > possibleMoves[i].length())
            {
                if 	(       possibleMoves[i].endsWith(sanToField)
                        & 	possibleMoves[i].charAt(0) == move.toString().charAt(0)
                        &   (possibleMoves[i].charAt(1) == sanFromChar1 | possibleMoves[i].charAt(1) == sanFromChar2)
                    )
                        return possibleMoves[i];
            }
        }

        return move;
    }

    public void setPossileMoves(CharSequence fastMoves)	
    {
    	p_hasPossibleMoves = false;
        p_possibleMoveList.clear();
    	String[] tmpList = fastMoves.toString().split(" ");
    	for(int i = 0; i < tmpList.length; i++)
        {
        	p_possibleMoveList.add(tmpList[i]);
        }
    	if (p_possibleMoveList.size() > 0)
    		p_hasPossibleMoves = true;
    }

	public void cntPieces(CharSequence fen)
	{
//Log.i(TAG, "cntPieces(), fen: " + fen);
		StringBuilder sbFenCheck = new StringBuilder(200);
		sbFenCheck.setLength(0);
		for (int i = 0; i < fen.length(); i++)
		{
			if (fen.charAt(i) == ' ')
				break;
			else
			{
				if (fen.charAt(i) > '8' | fen.charAt(i) == '/')
				{
					if (fen.charAt(i) != '/')
						sbFenCheck.append(fen.charAt(i));
				}
				else
				{
					if (fen.charAt(i) == '1') {sbFenCheck.append("-");}
					if (fen.charAt(i) == '2') {sbFenCheck.append("--");}
					if (fen.charAt(i) == '3') {sbFenCheck.append("---");}
					if (fen.charAt(i) == '4') {sbFenCheck.append("----");}
					if (fen.charAt(i) == '5') {sbFenCheck.append("-----");}
					if (fen.charAt(i) == '6') {sbFenCheck.append("------");}
					if (fen.charAt(i) == '7') {sbFenCheck.append("-------");}
					if (fen.charAt(i) == '8') {sbFenCheck.append("--------");}
				}
			}
		}
		fen = sbFenCheck.toString();
		cntWK = cntWQ = cntWR = cntWBl = cntWBd = cntWN = cntWP = 0;
		cntBK = cntBQ = cntBR = cntBBl = cntBBd = cntBN = cntBP = 0;
		for (int i = 0; i < fen.length(); i++)
		{
			switch (fen.charAt(i))
			{
				case 'K': cntWK++; break;
				case 'Q': cntWQ++; break;
				case 'R': cntWR++; break;
				case 'B': if (fldLD[i].equals("l")) cntWBl++; else cntWBd++; break;
				case 'N': cntWN++; break;
				case 'P': cntWP++; break;

				case 'k': cntBK++; break;
				case 'q': cntBQ++; break;
				case 'r': cntBR++; break;
				case 'b': if (fldLD[i].equals("l")) cntBBl++; else cntBBd++; break;
				case 'n': cntBN++; break;
				case 'p': cntBP++; break;
			}
		}
	}

	public boolean isAutoDraw()
	{
		int cntK = cntWK + cntBK;
		int cntQRP = cntWQ + cntWR + cntWP + cntBQ + cntBR + cntBP;
		int cntN = cntWN + cntBN;
		int cntBl = cntWBl + cntBBl;
		int cntBd = cntWBd + cntBBd;
		int cntB = cntBl + cntBd;
		int cntAll = cntK + + cntQRP + cntN + cntB;

//Log.i(TAG, "isAutoDraw(), cntQRP: " + cntQRP + ", cntBl: " + cntBl + ", cntBd: " + cntBd + ", cntN: " + cntN + ", cntAll: " + cntAll);
		if (cntK != 2 | cntQRP > 0)
		 	return false;
		if (cntWBl > 0 & cntWBd > 0)
			return false;
		if (cntBBl > 0 & cntBBd > 0)
			return false;

		if (cntAll == 2)	// k + k
			return true;
		if (cntAll == 3 & (cntB == 1 | cntN == 1)) // k + k + (b | n)
			return true;
		if (cntQRP == 0 & cntN == 0 & (cntBl > 0 | cntBd == 0)) // k + k + b (color light)
			return true;
		if (cntQRP == 0 & cntN == 0 & (cntBd > 0 | cntBl == 0)) // k + k + b (color dark)
			return true;

		return false;

	}

	public void setPossileMovesTo(CharSequence moveTo, ArrayList<String> moveList)
	{
		p_hasPossibleMovesTo = false;
		p_possibleMoveToList.clear();
		for (int i = 0; i < moveList.size(); i++)
		{

//			Log.i(TAG, "setPossileMovesTo(), moveList.all: " + moveList.get(i));

			if (moveList.get(i).length() == 9)	// castle: e1h1|e1g1
			{
				if (moveList.get(i).toString().substring(7, 9).equals(moveTo))
				{
					p_hasPossibleMovesTo = true;
					p_possibleMoveToList.add(moveList.get(i).toString().substring(5, 9));
//Log.i(TAG, "setPossileMovesTo(), moveTo: " + moveList.get(i).toString().substring(5, 9));
				}
			}
			else
			{
				if (moveList.get(i).length() >= 4)
				{
					if (moveList.get(i).toString().substring(2, 4).equals(moveTo))
					{
						p_hasPossibleMovesTo = true;
						p_possibleMoveToList.add(moveList.get(i).toString().substring(0, 4));
//Log.i(TAG, "setPossileMovesTo(), moveTo: " + moveList.get(i));
					}
				}
			}
		}
	}

    final String TAG = "ChessLogic";
	int gameStat = 0;
    public ChessPosition pos;
    public ChessPosition posPV;
    public ChessHistory history;
    ArrayList<CharSequence> stringValues;	// res/strings
    public CharSequence moveHistoryPrefs;
	public CharSequence quickMove = "";
	public Boolean isMv1 = false;

    ChessMove chessMove;
    public Chess960 chess960;
    // position values
    public CharSequence 	p_stat = "";
    public CharSequence 	p_message = "";
    public CharSequence 	p_fen = "";
    public CharSequence 	p_color = "w";
    public int 				p_chess960ID = 518;
    public int 				p_moveIdx = 0;
    public CharSequence 	p_move = "";
    public CharSequence 	p_movePgn = "";
    public CharSequence 	p_move1 = "";
    public CharSequence 	p_move2 = "";
    public CharSequence 	p_moveShow1 = "";
    public CharSequence 	p_moveShow2 = "";
    public CharSequence 	p_moveText = "";
    public CharSequence 	p_moveRank = "";
    public CharSequence 	p_moveVariant = "";
    public boolean 			p_moveIsFirst = false;
    public boolean 			p_moveHasVariations = false;
    public boolean 			p_moveIsFirstInVariation = false;
    public boolean 			p_moveIsLastInVariation = false;
    public boolean 			p_gameEnd = false;
    public boolean 			p_gameOver = false;
    public boolean 			p_variationEnd = false;
    public boolean 			p_mate = false;
    public boolean 			p_stalemate = false;
    public boolean 			p_auto_draw = false;
    public boolean 			p_hasPossibleMoves = false;
    public boolean 			p_hasPossibleMovesTo = false;
    public ArrayList<CharSequence> p_possibleMoveList;
    public ArrayList<CharSequence> p_possibleMoveToList;

	final CharSequence fldLD[] =
			{		"l", "d", "l", "d", "l", "d", "l", "d",
					"d", "l", "d", "l", "d", "l", "d", "l",
					"l", "d", "l", "d", "l", "d", "l", "d",
					"d", "l", "d", "l", "d", "l", "d", "l",
					"l", "d", "l", "d", "l", "d", "l", "d",
					"d", "l", "d", "l", "d", "l", "d", "l",
					"l", "d", "l", "d", "l", "d", "l", "d",
					"d", "l", "d", "l", "d", "l", "d", "l"};

	int cntWK, cntWQ, cntWR, cntWBl, cntWBd, cntWN, cntWP;
	int cntBK, cntBQ, cntBR, cntBBl, cntBBd, cntBN, cntBP;
}
