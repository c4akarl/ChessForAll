package ccc.chess.logic.c4aservice;

import android.util.Log;

import java.util.Random;

public class Chess960
{

    public CharSequence createChessPosition(int chess960ID)
    {	// creating a valid Chess960 position(FEN) from id(0 ... 959); id < 0 | id > 959 : random id
    	setStat(0);
    	if (chess960ID < 0 | chess960ID > 959)
    		chess960ID = getRandomId();
    	int kingId = 0;
    	int bishopId = 0;
    	CharSequence kingPieces = kingData[0];
    	CharSequence bishopPieces = bishopData[0];
    	for (int i = 0; i < 60; i++)	
        {	// creating kingPieces value and kingId from King's table  
     		if (chess960ID >= (i * 16))
    		{
    			kingPieces = kingData[i];
    			kingId = i * 16;
    		}
    		else
    			break;
        }
    	// creating bishopId from id - kingId
    	if ((chess960ID - kingId) >= 0)
    		bishopId = chess960ID - kingId;

//    	Log.d(TAG, "kingId, bishopId: " + kingId + ", " + bishopId);

    	// creating bishopPieces from Bishop's table 
    	bishopPieces = bishopData[bishopId];

//    	Log.d(TAG, "kingPieces, bishopPieces: " + kingPieces + ", " + bishopPieces);

    	CharSequence newPos = "";
    	int cnt = 0;
    	for (int i = 0; i < 8; i++)		
        {	// creating chess960 base line from bishopPieces and kingPieces
     		if (bishopPieces.charAt(i) == 'b')
     			newPos = newPos.toString() + bishopPieces.charAt(i);
    		else
    		{
    			newPos = newPos.toString() + kingPieces.charAt(cnt);
    			cnt++;
    		}
        }

//    	Log.d(TAG, "chess960ID, ChessPos: " + id + ", " + newPos);

    	if (newPos.length() == 8)
    	{	// creating FEN from chess960 base line
    		setStat(1);
    		setBaseLine(setCastling(newPos));
    		CharSequence chessPosWhite = newPos.toString().toUpperCase();
	        setFen(newPos.toString() + "/pppppppp/8/8/8/8/PPPPPPPP/" + chessPosWhite + " w " + setCastling(newPos) + " - 0 1");
	        setChess960Id(chess960ID);
	        setMessage("");
    	}
    	return getFen();
    }

    public int createChessPosition(CharSequence fen)
    {	// creating a Chess960ID(0 ... 959) from a chess position(FEN)
    	CharSequence baseLine = "";
    	CharSequence kingPieces = "";
    	CharSequence bishopPieces = "";
    	int kingId = 0;
    	int bishopId = 0;
    	if (fen.length() >= 8)
    	{
    		baseLine = fen.subSequence(0, 8);
    		CharSequence tmp = "";
    		for (int i = 0; i < 8; i++)		
            {	// change baseLine to lower case
     			char lo = Character.toLowerCase(baseLine.charAt(i));
    			tmp = tmp.toString() + lo;
            }
    		baseLine = tmp;
    		for (int i = 0; i < 8; i++)		
            {	// creating a King's table value(CharSequence) and a Bishop's table value(CharSequence)
         		if (baseLine.charAt(i) != 'b')
         		{
         			kingPieces = kingPieces.toString() + baseLine.charAt(i);
         			bishopPieces = bishopPieces.toString() + '-';
         		}
         		else
         			bishopPieces = bishopPieces.toString() + baseLine.charAt(i);
            }
    		for (int i = 0; i < 60; i++)	
            {	// get kingId from King's table value(CharSequence)
    			if (kingPieces.equals(kingData[i]))
    			{
    				kingId = i * 16;
    				break;
    			}
            }
    		for (int i = 0; i < 16; i++)	
            {	// get bishopId from Bishop's table value(CharSequence)
    			if (bishopPieces.equals(bishopData[i]))
    			{
    				bishopId = i;
    				break;
    			}
            }
    		setStat(1);
    		boolean isBaselineOk = true;
    		for (int i = 0; i < 8; i++)		
            {	// validate base line
         		if (!(	baseLine.charAt(i) == 'k' | baseLine.charAt(i) == 'q' | baseLine.charAt(i) == 'r' |
         				baseLine.charAt(i) == 'b' | baseLine.charAt(i) == 'n'))
         			isBaselineOk = false;

            }
    		if (isBaselineOk)
    		{	// create FEN from baseline and Chess960ID(kingId + bishopId)
	    		setBaseLine(baseLine);
	    		CharSequence chessPosWhite = ((String) baseLine).toUpperCase();
		        setFen(baseLine + "/pppppppp/8/8/8/8/PPPPPPPP/" + chessPosWhite + " w KQkq - 0 1");
		        setChess960Id(kingId + bishopId);
    		}
    		else
    			setChess960Id(518);
	        setMessage("");
    	}
    	return getChess960Id();
    }

//  get-methods
    public int getChess960Id() {return chess960Id;}
    public CharSequence getFen() {return fen;}
    public CharSequence getMessage() {return message;}
//  set-methods
    public void setStat(int lStat) {stat = lStat;}
    public void setChess960Id(int requestList) {chess960Id = requestList;}
    public void setFen(CharSequence requestList) {fen = requestList;}
    public void setBaseLine(CharSequence baseLine) {basLine = baseLine;}
    public CharSequence setCastling(CharSequence baseLine)
    {	// not activated, set: castling = "KQkq"; changed: 20201024
    	CharSequence castling = "-";


    	//karl DON'T REMOVE   !!!
		//karl, Shredder-FEN, funktioniert nicht, Anpassung erforderlich
//    	String standard = "rnbqkbnr";
//    	boolean isStandard = true;
//    	CharSequence lineL = "abcdefgh";
//    	CharSequence lineU = "ABCDEFGH";
//    	int posR1 = -1;
//    	int posR2 = -1;
//    	if (!baseLine.toString().equals(standard))
//    		isStandard = false;
//    	if (isStandard)
//    		castling = "KQkq";
//    	else
//    	{
//	    	if (baseLine.length() == 8)
//	    	{
//		    	for (int i = 0; i < baseLine.length(); i++)
//		        {
//		    		if (baseLine.charAt(i) == 'r')
//		    		{
//		    			if (posR1 == -1)
//		    				posR1 = i;
//		    			else
//		    				posR2 = i;
//		    		}
//		        }
//		    	if (posR1 != -1 & posR2 != -1) {
//		    		//karl!!!
////					castling = "" + lineU.charAt(posR1) + lineU.charAt(posR2) + lineL.charAt(posR1) + lineL.charAt(posR2);
//					castling = "" + lineU.charAt(posR2) + lineU.charAt(posR1) + lineL.charAt(posR2) + lineL.charAt(posR1);
//				}
//	    	}
//    	}


    	//karl, Shredder-FEN not supported in Chesspesso !?
    	castling = "KQkq";

//		Log.d(TAG, "setCastling(), baseLine: " + baseLine + ", castling: " + castling);

    	return castling;
    }

    public void setMessage(CharSequence lMessage) {message = lMessage;}

    public int getRandomId() 							
    {	// random number(0 ... 959)
    	Random r;
		int ir = 518;
		while (ir == 518)
		{
			r = new Random();
	        ir = r.nextInt(960);
		}
		return ir;
    }

	final String TAG = "Chess960";
	int stat = 0;
	int chess960Id = 0;
	CharSequence fen = "";
	CharSequence basLine = "";
	CharSequence message = "";
	final CharSequence kingData[] =
			{	//	King's table
					"qnnrkr", "nqnrkr", "nnqrkr", "nnrqkr",
					"nnrkqr", "nnrkrq", "qnrnkr", "nqrnkr",
					"nrqnkr", "nrnqkr", "nrnkqr", "nrnkrq",
					"qnrknr", "nqrknr", "nrqknr", "nrkqnr",
					"nrknqr", "nrknrq", "qnrkrn", "nqrkrn",
					"nrqkrn", "nrkqrn", "nrkrqn", "nrkrnq",
					"qrnnkr", "rqnnkr", "rnqnkr", "rnnqkr",
					"rnnkqr", "rnnkrq", "qrnknr", "rqnknr",
					"rnqknr", "rnkqnr", "rnknqr", "rnknrq",
					"qrnkrn", "rqnkrn", "rnqkrn", "rnkqrn",
					"rnkrqn", "rnkrnq", "qrknnr", "rqknnr",
					"rkqnnr", "rknqnr", "rknnqr", "rknnrq",
					"qrknrn", "rqknrn", "rkqnrn", "rknqrn",
					"rknrqn", "rknrnq", "qrkrnn", "rqkrnn",
					"rkqrnn", "rkrqnn", "rkrnqn", "rkrnnq",
			};

	final CharSequence bishopData[] =
			{	//	Bishop's table
					"bb------", "b--b----", "b----b--", "b------b",
					"-bb-----", "--bb----", "--b--b--", "--b----b",
					"-b--b---", "---bb---", "----bb--", "----b--b",
					"-b----b-", "---b--b-", "-----bb-", "------bb"
			};

}
