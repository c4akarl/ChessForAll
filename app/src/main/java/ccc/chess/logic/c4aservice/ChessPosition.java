package ccc.chess.logic.c4aservice;

import java.util.ArrayList;

import chesspresso.Chess;
import chesspresso.move.IllegalMoveException;
import chesspresso.move.Move;
import chesspresso.position.Position;

public class ChessPosition 
{
	public ChessPosition(int chess960Id)
    {	// constructor
		cpPosition = new Position();
		chess960SetValues(chess960Id);
    }

	public void setPosition(CharSequence posFen)				
    {
//		Log.i(TAG, "start setPosition(), fen: " + posFen);
		if (posFen.equals(""))
			return;
		if (isChess960)
			fen = chess960GetFEN(posFen);
		else
			fen = posFen;
		fast_move = 0;
//		Log.i(TAG, "next setPosition(), fen: " + fen);
		fen = ((String) fen).replace("  ", " ");
    	try 
    	{
    		cpPosition = new Position(fen.toString());
    		setMoveNumber(fen);
    	} 
    	catch (IllegalArgumentException e) {moveList.clear(); isFenError = true; return;}
    	getAllMoves();
    }

	public boolean validMove(CharSequence move)				
    {	// is in moveList? [cpPosition.getAllMoves()]
		if (isChess960 & isChess960Castling)
			return true;
		isPromotion = false;
		lanCastMove = "";
		for (int i = 0; i < moveList.size(); i++)
		{
//			Log.i(TAG, "validMove(), move, moveList: " + ">" + move + "<, >" + moveList.get(i) + "<");
			if (moveList.get(i).toString().length() == 9)
				lanCastMove = moveList.get(i).toString().substring(5, 9);
			if (move.length() == 4 & moveList.get(i).toString().substring(0, 4).equals(move.toString()))
			{
				if (moveList.get(i).length() == 5)
					isPromotion = true;
				return true;
			}
			if (move.length() == 4 & moveList.get(i).toString().length() == 9)
			{
				if (moveList.get(i).toString().substring(5, 9).equals(move.toString()))
				return true;
			}
			if (move.length() == 5 & moveList.get(i).toString().equals(move.toString()))
				return true;
		}
//		Log.i(TAG, "validMove(): false");
		return false;
    }

	public short isValidMove(int from, int to, int piece) 
	{	// validate fields (from, to)
        try 
        {
            short[] moves = cpPosition.getAllMoves();
            short m1 = cpPosition.getMove(from, to, 0);
            short m2 = Move.getEPMove(from, to);
            short m3 = cpPosition.getMove(from, to, piece);
            for (short move : moves) 
            {
                if (m1 == move) 	return m1;
                if (m2 == move) 	return m2;
                if (m3 == move) 	return m3;
            }
            return 0;
        }
        catch (Exception e) { return 0; }
	}

	public void doMove(int from, int to, int piece)
    {
		short m = isValidMove(from, to, piece);
    	try	{cpPosition.doMove(m);} 
    	catch (IllegalMoveException e) { e.printStackTrace(); } // ??? message
    }

	public void doChess960Castling(Position position, CharSequence castlingMove)
    {
		isChess960Error = false;
		if (castlingMove.length() == 4)
		{
			position.setStone(getSQI(castlingMove.subSequence(0, 2)), 0);	// empty field (king)
			position.setStone(getSQI(castlingMove.subSequence(2, 4)), 0);	// empty field (rook)
			if (castlingMove.equals(wShortCastC4aLan.subSequence(0, 4)))
			{	// white short castling
				position.setStone(getSQI(WK_SHORT), -6);	// king
				position.setStone(getSQI(WR_SHORT), -3);	// rook
				position.setToPlay(1);
				chess960SAN = "O-O";
				isChess960WhiteMove = true;
			}
			if (castlingMove.equals(wLongCastC4aLan.subSequence(0, 4)))
			{	// white long castling
				position.setStone(getSQI(WK_LONG), -6);	// king
				position.setStone(getSQI(WR_LONG), -3);	// rook
				position.setToPlay(1);
				chess960SAN = "O-O-O";
				isChess960WhiteMove = true;
			}
			if (castlingMove.equals(bShortCastC4aLan.subSequence(0, 4)))
			{	// black short castling
				position.setStone(getSQI(BK_SHORT), 6);	// king
				position.setStone(getSQI(BR_SHORT), 3);	// rook
				position.setToPlay(0);
				chess960SAN = "O-O";
				isChess960WhiteMove = false;
			}
			if (castlingMove.equals(bLongCastC4aLan.subSequence(0, 4)))
			{	// black long castling
				position.setStone(getSQI(BK_LONG), 6);	// king
				position.setStone(getSQI(BR_LONG), 3);	// rook
				position.setToPlay(0);
				chess960SAN = "O-O-O";
				isChess960WhiteMove = false;
			}
			position.setSqiEP(-1);
			position.setPlyNumber(position.getPlyNumber() +1);
			try {position = new Position(position.getFEN());} 
	    	catch (IllegalArgumentException e) {e.printStackTrace(); isChess960Error = true;}
			setMoveNumber(position.getFEN());
		}
    }

	public ArrayList<String> getAllMoves()				
    {	
		moveList.clear();
		short[] moves = cpPosition.getAllMoves();
		Move.normalizeOrder(moves);
		
		for (int i = 0; i < moves.length; i++)
        {
			String mv = Move.getString(moves[i]);
			// if Chess960 possible move create and add to moveList
			if (mv.startsWith("O"))
				mv = getCastlingLAN(fen, mv);
			mv = mv.replace("-", "");
			mv = mv.replace("x", "");
			moveList.add(mv);
        }

		// for TEST only !
//		String printList = "";
//		for (int i = 0; i < moveList.size(); i++)
//        {
//			if (i == moveList.size() -1)
//				printList = printList + moveList.get(i);
//			else
//				printList = printList + moveList.get(i) + ", ";
//        }
//		Log.i(TAG, "move, SAN: " + cpPosition.getMovesAsString(moves, true));
//		Log.i(TAG, "move, LAN: " + printList);

        return moveList;
    }
	public ArrayList<Short> getShortMovesFromSAN(Position pos, CharSequence sanMove)				
    {	// SAN = PGN-move; return: short move
		ArrayList<Short> shortMoves = new ArrayList<Short>();
		if (sanMove.toString().startsWith("O-"))
		{
			if (pos.getToPlay() == 0)
			{
				if (sanMove.toString().equals("O-O"))
					shortMoves.add((short) 29060);
				if (sanMove.toString().equals("O-O-O"))
					shortMoves.add((short) 28804);
			}
			else
			{
				if (sanMove.toString().equals("O-O"))
					shortMoves.add((short) 32700);
				if (sanMove.toString().equals("O-O-O"))
					shortMoves.add((short) 32444);
			}
			return shortMoves;
		}
		int stone = 0;
		CharSequence from = "";
		CharSequence to = "";
		int toSqi = 0;
		int promoPiece = 0;
		sanMove = sanMove.toString().replace("-", "");
		sanMove = sanMove.toString().replace("+", "");
		sanMove = sanMove.toString().replace("#", "");
		sanMove = sanMove.toString().replace("x", "");
		if (sanMove.toString().contains("="))
		{
			promoPiece = 4;
			if (Character.isUpperCase(sanMove.charAt(sanMove.length() -1)))
			{
				switch (sanMove.charAt(sanMove.length() -1))
		        {
		        	case 'N': promoPiece = 1; break;	
		        	case 'B': promoPiece = 2; break;	
		        	case 'R': promoPiece = 3; break;	
		        	case 'Q': promoPiece = 4; break;	
		        }
			}
			if (sanMove.charAt(sanMove.length() -2) == '=')
				sanMove = sanMove.subSequence(0, sanMove.length() -2);
		}
		if (sanMove.length() > 0)
		{
			if (Character.isLowerCase(sanMove.charAt(0)))
				stone = 5;
			else
			{
				switch (sanMove.charAt(0))
		        {
		        	case 'N': stone = 1; break;	
		        	case 'B': stone = 2; break;	
		        	case 'R': stone = 3; break;	
		        	case 'Q': stone = 4; break;	
		        	case 'P': stone = 5; break;	
		        	case 'K': stone = 6; break;	
		        }
			}
			if (stone == 5)
			{
				if (sanMove.length() > 2)
					from = sanMove.subSequence(0, sanMove.length() -2);
				if (sanMove.length() == 2)
					from = sanMove.subSequence(0, 1);
			}
			else
			{
				if (sanMove.length() > 3)
					from = sanMove.subSequence(1, sanMove.length() -2);
			}
			if (pos.getToPlay() == 0)
				stone = stone * -1;
			to = sanMove.subSequence(sanMove.length() -2, sanMove.length());
			toSqi = getSQI(to);
			isLanNotation = false;
			if (from.length() == 2)
			{
				shortMoves.add(pos.getMove(getSQI(from), toSqi, promoPiece));
				isLanNotation = true;
			}
			else
			{
				if (stone == 5 | stone == -5)
				{	// pawn move
					int fromPawnSqi = -1;
					boolean isFirst = true;
					for (int i = 0; i < 64; i++)
			        {
						if (pos.getStone(i) == stone & from.length() == 1 & cpFields[i].toString().contains(from))
						{
							if (pos.getToPlay() == 0)
							{	// white
								if (i < toSqi)
									fromPawnSqi = i;
							}
							else
							{	// black
								if (i > toSqi & isFirst)
								{
									fromPawnSqi = i;
									isFirst = false;
								}
							}
						}
			        }
					if (fromPawnSqi > -1 & fromPawnSqi < 64)
					{
						if ((stone == 5 | stone == -5) & sanMove.length() == 3)
							shortMoves.add(pos.getPawnMove(Chess.sqiToCol(fromPawnSqi), toSqi, promoPiece));
						else
							shortMoves.add(pos.getMove(fromPawnSqi, toSqi, promoPiece));
					}
				}
				else
				{
					for (int i = 0; i < 64; i++)
			        {
						if (pos.getStone(i) == stone)
						{
	//						Log.i(TAG, "sanMove, from, to, fromSqi, toSqi, promo: " 
	//								+ sanMove + ", " + from + ", " + to + ", " + i + ", " + toSqi + ", " + promoPiece);
							if (from.length() == 1)
							{
								if (cpFields[i].toString().contains(from))
									shortMoves.add(pos.getMove(i, toSqi, promoPiece));
							}
							else
								shortMoves.add(pos.getMove(i, toSqi, promoPiece));
						}
			        }
				}
			}
		}
		return shortMoves;
    }

		public CharSequence getMoveFromSAN(CharSequence fen, CharSequence sanMove)
    {	// SAN = PGN-move; return: LAN
//Log.i(TAG, sanMove + ", " + fen);
		if (isChess960)
			fen = chess960GetFEN(fen);
//Log.i(TAG, "getMoveFromSAN(), isChess960: " + isChess960 + ", sanMove: " + sanMove);
    	try {cpPgnPosition = new Position(fen.toString(), true);}
    	catch (IllegalArgumentException e) {e.printStackTrace(); return "";}
    	setMoveNumber(fen);
    	if (isChess960 & sanMove.toString().startsWith("O"))
    	{
    		if (sanMove.toString().startsWith("O"))
    		{
    			if (cpPgnPosition.getToPlay() == 0 & sanMove.equals("O-O"))
    				lanMove = wShortCastC4aLan.subSequence(0, 4).toString();
    			if (cpPgnPosition.getToPlay() == 0 & sanMove.equals("O-O-O"))
    				lanMove = wLongCastC4aLan.subSequence(0, 4).toString();
    			if (cpPgnPosition.getToPlay() == 1 & sanMove.equals("O-O"))
    				lanMove = bShortCastC4aLan.subSequence(0, 4).toString();
    			if (cpPgnPosition.getToPlay() == 1 & sanMove.equals("O-O-O"))
    				lanMove = bLongCastC4aLan.subSequence(0, 4).toString();
    			doChess960Castling(cpPgnPosition, lanMove);
   				chess960SetFenCastling(lanMove);
//Log.i(TAG, "getMoveFromSAN(), isChess960: " + isChess960 + ", lanMove: " + lanMove);
    			return lanMove;	
    		}
    	}
    	
//		start NEW
    	CharSequence lan = getNewMove(fen, sanMove);
    	if (!lan.equals(""))
		{
//Log.i(TAG, "NEW, sanMove: " + sanMove + ", lanMove: " + lanMove);
			return lan;
		}
    	else
    	{
    		try {cpPgnPosition = new Position(fen.toString(), true);} 
        	catch (IllegalArgumentException e) {e.printStackTrace(); return "";}
        	setMoveNumber(fen);
    	}
//    	end NEW
    	
    	//	OLD
		short[] moves = cpPgnPosition.getAllMoves();
		Move.normalizeOrder(moves);
		tmp = cpPgnPosition.getMovesAsString(moves, true);
		tmp = tmp.replaceAll("\\{", "");
		tmp = tmp.replaceAll("\\}", "");
		sanMoves = tmp.split(",");
		String tmpSanMove = posSanMove;
		Boolean isPosSanMove = false;
		for (int i = 0; i < sanMoves.length; i++)
        {
			if (sanMove.toString().equals(sanMoves[i]))
			{
//				Log.i(TAG, "sanMove, moves[i]: " + sanMove + ", " + moves[i]);
				try {cpPgnPosition.doMove(moves[i]);} 
				catch (IllegalMoveException e) { e.printStackTrace(); return "";}
				posSanMove = cpPgnPosition.getLastMove().getSAN();
				lanMove = Move.getString(moves[i]);
//Log.i(TAG, "OLD, sanMove, lanMove, moves[i]: " + sanMove + ", " + lanMove + ", " + moves[i]);
				if (lanMove.startsWith("O"))
					lanMove = getCastlingLAN(fen, lanMove);
				lanMove = lanMove.replace("-", "");
				lanMove = lanMove.replace("x", "");
//				if (lanMove.startsWith("O"))
//					lanMove = getCastlingLAN(fen, lanMove);
				if (isChess960)
    				chess960SetFenCastling(lanMove);
//Log.i(TAG, "OLD, sanMove: " + sanMove + ", lanMove: " + lanMove);
				return lanMove;								// move lan (g1f3)
			}
			if (tmpSanMove.equals(sanMoves[i]))
				isPosSanMove = true;
        }
        if (isPosSanMove)
		{
			for (int i = 0; i < sanMoves.length; i++)
			{
				if (tmpSanMove.equals(sanMoves[i]))
				{
					try {cpPgnPosition.doMove(moves[i]);}
					catch (IllegalMoveException e) { e.printStackTrace(); return "";}
					posSanMove = cpPgnPosition.getLastMove().getSAN();
					lanMove = Move.getString(moves[i]);
					lanMove = lanMove.replace("-", "");
					lanMove = lanMove.replace("x", "");
					if (isChess960)
						chess960SetFenCastling(lanMove);
					return lanMove;								// move lan (g1f3)
				}
			}
		}
        return "";
    }



	public String getLanMoveFromSanMove(CharSequence fen, String[] possibleSanMoves, CharSequence sanMove)
	{
//Log.i(TAG, "1 getLanMoveFromSanMove(), sanMove: " + sanMove + "\nfen: " + fen);
		String lanMove = "";
		if (possibleSanMoves != null)
		{
			for (int i = 0; i < possibleSanMoves.length; i++)
			{
				if (possibleSanMoves[i].equals(sanMove.toString()))
					lanMove = getMoveFromSAN(fen, possibleSanMoves[i]).toString();
			}
		}
		if (isChess960 & lanMove.equals("") & sanMove.toString().startsWith("O-"))
		{
			lanMove = getMoveFromSAN(fen, sanMove).toString();
		}
//Log.i(TAG, "2 getLanMoveFromSanMove(), sanMove: " + sanMove + ", lanMove: " + lanMove + ", posSanMove: " + posSanMove);
		return lanMove;
	}

	public CharSequence getNewMove(CharSequence fen, CharSequence sanMove)				
    {	// NEW methode, get lan
		try {cpPgnPosition = new Position(fen.toString(), true);} 
    	catch (IllegalArgumentException e) {e.printStackTrace(); return "";}
		ArrayList<Short> shortMoves = getShortMovesFromSAN(cpPgnPosition, sanMove);
    	boolean isMultiplePawnMoves = false;
    	if (shortMoves != null)
    	{
	    	if (shortMoves.size() > 1)
	    	{
	    		if (!Character.isUpperCase(sanMove.charAt(0)))
	    		{
	    			isMultiplePawnMoves = true;
//	    			Log.i(TAG, "isMultiplePawnMoves, sanMove:    " + sanMove);
	    		}
	    	}
    	}
    	if (shortMoves != null & !isMultiplePawnMoves)
    	{
//    		Log.i(TAG, "shortMoves.size(): " + shortMoves.size());
    		boolean isMoveOk = true;
	    	for (int i = 0; i < shortMoves.size(); i++)
	        {
	    		try {cpPgnPosition.doMove(shortMoves.get(i));}
				catch (IllegalMoveException e) 	{isMoveOk = false;}
	    		catch (RuntimeException e) 		{isMoveOk = false;}

	    		if (isMoveOk)
	    		{
	    			posSanMove = cpPgnPosition.getLastMove().getSAN();
//Log.i(TAG, "getShortMovesFromSAN(), sanMove: " + sanMove + ", shortMoves: " + shortMoves.get(i) + ", posSanMove: " + posSanMove);
		    		if (!isLanNotation & !sanMove.equals(posSanMove))
		    		{
		    			String tmp = posSanMove;
		    			tmp = tmp.replace("x", "");
		    			tmp = tmp.replace("+", "");
	    				tmp = tmp.replace("#", "");
	    				if (tmp.length() == 3)
	    				{
	    					if (Character.isUpperCase(tmp.charAt(0)))
	    						isMoveOk = true;
	    				}
	    				else
							isMoveOk = false;
		    		}
	    		}
	    		if (isMoveOk)
	    		{
	    			lanMove = Move.getString(shortMoves.get(i));
					if (lanMove.startsWith("O"))
						lanMove = getCastlingLAN(fen, lanMove);
					lanMove = lanMove.replace("-", "");
					lanMove = lanMove.replace("x", "");
					if (isChess960)
	    				chess960SetFenCastling(lanMove);
//Log.i(TAG, "NEW, sanMove, posSanMove, lanMove, shortMoves.get(i): " + sanMove + ", " + posSanMove + ", " + lanMove + ", " + shortMoves.get(i));
					return lanMove;		
	    		}
	    		else
	    		{
	    			try {cpPgnPosition = new Position(fen.toString(), true);}
	    	    	catch (IllegalArgumentException e) {e.printStackTrace(); i = shortMoves.size();}
	    			setMoveNumber(fen);
	    			isMoveOk = true;
	    		}
	        }
    	}
    	return "";
    }

	public String getCastlingLAN(CharSequence fen, String moveCastling)	
	{	// O-O ---> e1h1>e1g1 [Chess960: e1h1]
		String castMove = "";
		boolean isWhiteMove = true;
		String[] tmpList = fen.toString().split(" ");
		if (tmpList.length > 1)
		{
			if (!tmpList[1].equals("w"))
				isWhiteMove = false;
		}
		// castling: set chesspresso fields (e1g1)
		if (isWhiteMove)
		{
			if (moveCastling.equals("O-O"))		
			{
				if (!isChess960) castMove = "e1h1|e1g1"; else castMove = "e1h1|e1h1";	// !!! set Chess960 fields !!!
			}
			if (moveCastling.equals("O-O-O"))
			{
				castMove = "e1c1";
				if (!isChess960) castMove = "e1a1|e1c1"; else castMove = "e1a1|e1a1";	// !!! set Chess960 fields !!!
			}
		}
		else
		{
			if (moveCastling.equals("O-O"))
			{
				if (!isChess960) castMove = "e8h8|e8g8"; else castMove = "e8h8|e8h8";	// !!! set Chess960 fields !!!
			}
			if (moveCastling.equals("O-O-O"))
			{
				if (!isChess960) castMove = "e8a8|e8c8"; else castMove = "e8a8|e8a8";	// !!! set Chess960 fields !!!
			}
		}
//		Log.i(TAG, "moveCastling, castMove, lanCastMove: " + moveCastling + ", " + castMove + ", " + lanCastMove);
		return castMove;
	}
	
	public CharSequence getFastMove(CharSequence mv1)	
	{	// fastMove return: "": multiple moves toField; listFrom: multiple moves fromField; fastMove: only one move(fast move)
//		Log.i(TAG, "getFastMove(), mv1: " + mv1);
		CharSequence fastMove = "";
		CharSequence listFrom = "";
		CharSequence prevMove = "";
		boolean isFrom = false;
		boolean isTo = false;
		boolean isPromo = false;
		isPromotion = false;
		
		int cntFrom = 0;
		int cntTo = 0;
		for (int i = 0; i < moveList.size(); i++)
		{
//			Log.i(TAG, "moveList.get(i): " + moveList.get(i));
			if (moveList.get(i).length() >= 4)
			{
				if (moveList.get(i).toString().substring(0, 2).equals(mv1.toString()))
				{
					isFrom = true;
					if (moveList.get(i).length() == 5)
						isPromo = true;
					if (!isPromo)
					{
						if (moveList.get(i).length() == 4)
							cntFrom++;
					}
					else
					{
						if (moveList.get(i).toString().substring(4, 5).equals("Q"))
							cntFrom++;
					}
					if (!isPromo)
					{
						if (moveList.get(i).length() == 4)
							fastMove = moveList.get(i);
					}
					else
					{
						if (moveList.get(i).toString().substring(4, 5).equals("Q"))
							fastMove = moveList.get(i).toString().substring(0, 4);
					}
					if (!moveList.get(i).toString().substring(0, 4).equals(prevMove.toString()))
					{
						listFrom = listFrom + moveList.get(i).toString().substring(0, 4) + " ";
						if (moveList.get(i).length() == 9)
						{
							cntFrom++;
							listFrom = listFrom + moveList.get(i).toString().substring(5, 9) + " ";
						}
					}
				}
				if (moveList.get(i).length() != 9 & moveList.get(i).toString().substring(2, 4).equals(mv1.toString()))
				{
					isTo = true;
					if (moveList.get(i).length() == 5)
						isPromo = true;
//					Log.i(TAG, "move(4), prev(4): " + moveList.get(i).toString().substring(0, 4) + ", " + prevMove.toString());
					if (!isPromo)
						cntTo++;
					else
					{
						if (moveList.get(i).toString().substring(4, 5).equals("Q"))
							cntTo++;
					}
					if (!isPromo)
						fastMove = moveList.get(i);
					else
					{
						if (moveList.get(i).toString().substring(4, 5).equals("Q"))
							fastMove = moveList.get(i).toString().substring(0, 4);
					}
				}
				prevMove = moveList.get(i).toString().substring(0, 4);
			}
		}
		if (isPromo & (cntFrom == 1 | cntTo == 1))
			isPromotion = true;
//		Log.i(TAG, "fastMove, cntFrom, cntTo, isPromo, isPromotion: " + fastMove + ", " + cntFrom + ", " + cntTo + ", " + isPromo + ", " + isPromotion);
		if(isPromotion)
		{
			fast_move = FAST_MOVE_PROMOTION_TO;
			return fastMove;
		}
		else
		{
			if((isFrom & cntFrom == 1) | (isTo & cntTo == 1)) 
			{
				fast_move = FAST_MOVE_OK;
				return fastMove;
			}
			else
			{
				if(isFrom & cntFrom > 1 )
				{
					fast_move = FAST_MOVE_MULTIPLE_FROM;
					return listFrom;
				}
				else
				{
					if(cntFrom == 0 & cntTo == 0)
						fast_move = FAST_MOVE_NO_MOVE;
					if(cntTo > 1)
						fast_move = FAST_MOVE_MULTIPLE_TO;
					return "";
				}
			}
		}
	}

	public CharSequence getFEN(Position cpPos)	
	{
		if (isChess960)
			return chess960SetNewFEN(cpPos.getFEN(), chess960OldCast);
		else
			return cpPos.getFEN();
	}

	public CharSequence getSAN()	
	{
		try
		{
			Move m = cpPosition.getLastMove();
			if (isChess960Castling)
				return chess960SAN;
			else
				return m.getSAN();
		}
		catch (NullPointerException e) { return ""; } 
	}

	public boolean isWhiteMove()	
	{
		if (isChess960Castling)
			return isChess960WhiteMove;
		else
		{
			if (cpPosition.getToPlay() == 1)
				return true;
			else
				return false;
		}
	}
	
	public boolean isSquareEmpty(int sqi)	{ return cpPosition.isSquareEmpty(sqi); }

	public int getColor(int sqi)	{ return cpPosition.getColor(sqi); }	// 0 = white, 1 = black, -1 = empty

	public int getToPlay()			{ return cpPosition.getToPlay(); }		// 0 = white, 1 = black
	
	public boolean isLegal()		
	{ 
		if (moveList.size() > 0)
			return true;
		else
			return false;
	}

	public boolean canMove()		{ return cpPosition.canMove(); }

	public boolean isCheck()		{ return cpPosition.isCheck(); }

	public boolean isMate()			{ return cpPosition.isMate(); }

	public boolean isStaleMate()	{ return cpPosition.isStaleMate(); }

	public int getMoveNumber()		{ return moveNumber; }

	public void setMoveNumber(CharSequence posFen)		
	{ 
		String[] fenSplit = posFen.toString().split(" ");
		if (fenSplit.length == 6)
		{
			try		{moveNumber = Integer.parseInt(fenSplit[5]);}
	    	catch 	(NumberFormatException e) {moveNumber = 0;}
		}
		else
			moveNumber = 0;
	}

	public int getHalfMoveClock()	{ return cpPosition.getHalfMoveClock(); }

	public int getSQI(CharSequence field) 
	{
		int position = 0;
		for (int i = 0; i < 64; i++)
        {
			if (cpFields[i].equals(field))
			{
				position = i;
				break;
			}
        }
		return position;
	}

	//	Chess960 castling methods
	public void chess960SetValues(int chess960ID)				
    {
//		Log.i(TAG, "chess960SetValues(), chess960ID: " + chess960ID);
		wK = "";
		bK = "";
		wRS = "";
		wRL = "";
		bRS = "";
		bRL= "";
		wShortCastC4aLan = "";
		wLongCastC4aLan = "";
		bShortCastC4aLan = "";
		bLongCastC4aLan = "";
		this.chess960ID = chess960ID;
		if (chess960ID == 518)
			isChess960 = false;
		else
		{
			isChess960 = true;
		}
			chess960Fen = chess960.createChessPosition(chess960ID);
			if (chess960Fen.length() >= 8)
			for (int i = 0; i < 8; i++)
	        {
				if (chess960Fen.charAt(i) == 'k')
				{
					wK = cpFields[i];
					bK = cpFields[i +56];
				}
				if (chess960Fen.charAt(i) == 'r')
				{
					if (wRL.equals(""))
					{
						wRL = cpFields[i];
						bRL = cpFields[i +56];
					}
					else
					{
						wRS = cpFields[i];
						bRS = cpFields[i +56];
					}
				}
	        }
			wShortCastC4aLan = "" + wK + wRS + "|" + wK + wRS;
			wLongCastC4aLan = "" + wK + wRL + "|" + wK + wRL;
			bShortCastC4aLan = "" + bK + bRS + "|" + bK + bRS;
			bLongCastC4aLan = "" + bK + bRL + "|" + bK + bRL;
//			Log.i(TAG, "wShortCastC4aLan, wLongCastC4aLan: " + wShortCastC4aLan + ", " + wLongCastC4aLan);
//			Log.i(TAG, "bShortCastC4aLan, bLongCastC4aLan: " + bShortCastC4aLan + ", " + bLongCastC4aLan);
//		}
    }

	public boolean chess960CanCastling(CharSequence gameFen, CharSequence fen, int toPlay, CharSequence move)	
	{		// move: d1b1(LAN) | O-O (SAN)
		if (move.toString().startsWith("O"))
		{	// SAN to c4aLAN
			if (toPlay == 0 & move.toString().equals("O-O")) move = "" + wK + wRS;
			if (toPlay == 0 & move.toString().equals("O-O-O")) move = "" + wK + wRL;
			if (toPlay == 1 & move.toString().equals("O-O")) move = "" + bK + bRS;
			if (toPlay == 1 & move.toString().equals("O-O-O")) move = "" + bK + bRL;
		}
		if (move.length() != 4) return false;
		// validate c4aLAN
		castChar = " ";
		if (move.toString().equals(wShortCastC4aLan.subSequence(0, 4))) castChar = "K";
		if (move.toString().equals(wLongCastC4aLan.subSequence(0, 4))) castChar = "Q";
		if (move.toString().equals(bShortCastC4aLan.subSequence(0, 4))) castChar = "k";
		if (move.toString().equals(bLongCastC4aLan.subSequence(0, 4))) castChar = "q";
		if (castChar.equals(" ")) return false;
		String[] tmpList = fen.toString().split(" ");
		if (tmpList.length > 2)
		{	// current fen: can castle?
			if (!tmpList[2].contains(castChar))
				return false;
		}
		String checkFen = tmpList[0] + " " + tmpList[1] + " " + "-" + " " + tmpList[3] + " " +tmpList[4] + " " + tmpList[5];
		// is castle way(K ---> R) free?
		try {cpCastPosition = new Position(checkFen);} 
    	catch (IllegalArgumentException e) {e.printStackTrace();}
		int kFrom = getSQI(move.subSequence(0, 2));
		int kTo = 0;
		int kColor = 0;
		if (castChar.equals("K")) {kTo = getSQI(WK_SHORT); kColor = -6;}
		if (castChar.equals("Q")) {kTo = getSQI(WK_LONG); kColor = -6;}
		if (castChar.equals("k")) {kTo = getSQI(BK_SHORT); kColor = 6;}
		if (castChar.equals("q")) {kTo = getSQI(BK_LONG); kColor = 6;}
		int rTo = getSQI(move.subSequence(2, 4));
		int sqiLow = kFrom;
		int sqiHigh = kFrom;
		if (kTo < kFrom) sqiLow = kTo;
		if (rTo < kTo) sqiLow = rTo;
		if (kTo > kFrom) sqiHigh = kTo;
		if (rTo > kTo) sqiHigh = rTo;
		for (int i = sqiLow; i <= sqiHigh; i++)
        {
			if (!cpFields[i].equals(move.subSequence(0, 2)) & !cpFields[i].equals(move.subSequence(2, 4)))
			{
				if (cpCastPosition.getColor(i) != -1) return false;	// castling way not empty
			}
        }
		// is king checked(all k-fields)
		sqiLow = kFrom;
		sqiHigh = kFrom;
		if (kTo < kFrom) sqiLow = kTo;
		if (kTo > kFrom) sqiHigh = kTo;
//		Log.i(TAG, "sqiLow, sqiHigh: " + sqiLow + ", " + sqiHigh);
		cpCastPosition.setStone(kFrom, 0);
		for (int i = sqiLow; i <= sqiHigh; i++)
        {
			cpCastPosition.setStone(i, kColor);
			String castFen =cpCastPosition.getFEN();
			try {cpCastPosition = new Position(castFen);} 
	    	catch (IllegalArgumentException e) {e.printStackTrace(); return false;}
			if (cpCastPosition.isCheck()) return false;	// castling way king is checked!
			cpCastPosition.setStone(i, 0);
        }
//		Log.i(TAG, "can castle !!!");
		return true;	// castling OK!
	}

	public CharSequence chess960GetFEN(CharSequence posFEN)	
	{
		CharSequence chess960FEN = "";
		String[] tmpList = posFEN.toString().split(" ");
		if (tmpList.length > 5)
		{
			chess960FEN = "" + tmpList[0] + " " + tmpList[1] + " - " + tmpList[3] + " " + tmpList[4] + " " + tmpList[5];
			chess960OldCast = tmpList[2];
		}
		return chess960FEN;
	}

	public CharSequence chess960SetNewFEN(CharSequence posFEN, CharSequence oldCast)	
	{
		CharSequence chess960FEN = "";
		String[] tmpList = posFEN.toString().split(" ");
		if (tmpList.length > 5)
		{
			chess960FEN = "" + tmpList[0] + " " + tmpList[1] + " " + oldCast + " " + tmpList[3] + " " + tmpList[4] + " " + tmpList[5];
		}
//		Log.i(TAG, "chess960FEN: " + chess960FEN);
		return chess960FEN;
	}

	public void chess960SetFenCastling(CharSequence move)	
	{
		CharSequence newCast = "";
		if (chess960OldCast.equals("-"))
			return;
		boolean delWS = false;
		boolean delWL = false;
		boolean delBS = false;
		boolean delBL = false;
		if (move.length() == 4)
		{
			if (move.subSequence(0, 2).equals(wK))
			{
				delWS = true;
				delWL = true;
			}
			if (move.subSequence(0, 2).equals(wRS) | move.subSequence(2, 4).equals(wRS))
				delWS = true;
			if (move.subSequence(0, 2).equals(wRL) | move.subSequence(2, 4).equals(wRL))
				delWL = true;
			if (move.subSequence(0, 2).equals(bK))
			{
				delBS = true;
				delBL = true;
			}
			if (move.subSequence(0, 2).equals(bRS) | move.subSequence(2, 4).equals(bRS))
				delBS = true;
			if (move.subSequence(0, 2).equals(bRL) | move.subSequence(2, 4).equals(bRL))
				delBL = true;
			for (int i = 0; i < chess960OldCast.length(); i++)
			{
				if (chess960OldCast.charAt(i) == 'K' & !delWS) newCast = newCast.toString() + chess960OldCast.charAt(i);
				if (chess960OldCast.charAt(i) == 'Q' & !delWL) newCast = newCast.toString() + chess960OldCast.charAt(i);
				if (chess960OldCast.charAt(i) == 'k' & !delBS) newCast = newCast.toString() + chess960OldCast.charAt(i);
				if (chess960OldCast.charAt(i) == 'q' & !delBL) newCast = newCast.toString() + chess960OldCast.charAt(i);
			}
			if (newCast.equals(""))
				newCast = "-";
			chess960OldCast = newCast;
		}
	}

	public void chess960SetCanCast(CharSequence fen)	
	{
		
		canCastWS = false;
		canCastWL = false;
		canCastBS = false;
		canCastBL = false;
		String[] tmpList = fen.toString().split(" ");
		if (tmpList.length > 5)
		{
			if (tmpList[2].contains("K")) canCastWS = true;
			if (tmpList[2].contains("Q")) canCastWL = true;
			if (tmpList[2].contains("k")) canCastBS = true;
			if (tmpList[2].contains("q")) canCastBL = true;
		}
//		Log.i(TAG, "chess960SetCanCast(), fen: " + fen);
//		Log.i(TAG, "canCastWS, canCastWL, canCastBL, : " + canCastWS +  ", " + canCastWL + ", " + canCastBS + ", " + canCastBL);
	}
	
	final String TAG = "ChessPosition";
	Chess960 chess960 = new Chess960();
	int fast_move = 0;
	final int FAST_MOVE_NO_MOVE = 0;
	final int FAST_MOVE_OK = 1;
	final int FAST_MOVE_MULTIPLE_FROM = 2;
	final int FAST_MOVE_MULTIPLE_TO = 3;
	final int FAST_MOVE_PROMOTION_TO = 4;

	// c4aLogic
	CharSequence fen = "";
	//  chesspresso
	Position cpPosition;
	final CharSequence cpFields[] =
        {
        "a1", "b1", "c1", "d1", "e1", "f1", "g1", "h1",
        "a2", "b2", "c2", "d2", "e2", "f2", "g2", "h2",
        "a3", "b3", "c3", "d3", "e3", "f3", "g3", "h3",
        "a4", "b4", "c4", "d4", "e4", "f4", "g4", "h4",
        "a5", "b5", "c5", "d5", "e5", "f5", "g5", "h5",
        "a6", "b6", "c6", "d6", "e6", "f6", "g6", "h6",
        "a7", "b7", "c7", "d7", "e7", "f7", "g7", "h7",
        "a8", "b8", "c8", "d8", "e8", "f8", "g8", "h8"
        };

// parse PGN-data
	Position cpPgnPosition;
	int moveNumber = 0;
	String tmp = "";
	String lanMove = "";
	String posSanMove = "";
	String lanCastMove = "";
	String[] sanMoves;

	// chess960 - castling
	Position cpCastPosition;
	boolean isChess960 = false;
	boolean isChess960Castling = false;
	boolean isChess960WhiteMove = false;
	boolean isChess960Error = false;
	boolean canCastWS = false;
	boolean canCastWL = false;
	boolean canCastBS = false;
	boolean canCastBL = false;
	int chess960ID = 518;
	CharSequence chess960Fen = "";
	CharSequence chess960OldCast = "";
	CharSequence chess960SAN = "";
	CharSequence castChar = " ";	// " ": no castling, "K": white short, "Q": white long, "k": black short, "q": black long 
	final CharSequence WK_SHORT = "g1";
	final CharSequence WK_LONG = "c1";
	final CharSequence WR_SHORT = "f1";
	final CharSequence WR_LONG = "d1";
	final CharSequence BK_SHORT = "g8";
	final CharSequence BK_LONG = "c8";
	final CharSequence BR_SHORT = "f8";
	final CharSequence BR_LONG = "d8";
	CharSequence wK = "";
	CharSequence bK = "";
	CharSequence wRS = "";
	CharSequence wRL = "";
	CharSequence bRS = "";
	CharSequence bRL = "";
	CharSequence wShortCastC4aLan = "";
	CharSequence wLongCastC4aLan = "";
	CharSequence bShortCastC4aLan = "";
	CharSequence bLongCastC4aLan = "";
	
	// variables
	public ArrayList<String> moveList = new ArrayList<String>();
	boolean isPromotion = false;
	boolean isLanNotation = false;
	boolean isFenError = false;

}
