package ccc.chess.logic.c4aservice;

public class ChessMove
{
	final String TAG = "ChessMove";
	
	CharSequence rank = "0";		//  1
	CharSequence variant = "0";		//  2
	CharSequence fields = "";		//  3
	CharSequence pgn = "";			//  4
	CharSequence fen = "";			//  5
	CharSequence txt = "";			//  6
	CharSequence isCheck = "f";		//  7
	CharSequence isMate = "f";		//  8
	CharSequence isStealMate = "f";	//  9
	CharSequence baseFen = "";		// 10
	CharSequence baseIsCheck = "f";	// 11
	CharSequence control = "0";		// 12
	
	CharSequence color = "l";		//  move color
	
	StringBuilder sb = new StringBuilder();
	
	public ChessMove()
    {
    }

	public ChessMove(CharSequence hist)
    {
		setMoveFromHistory(hist);
    }

	public void setMove(CharSequence mRank, CharSequence mVariant, CharSequence mFields, CharSequence mPgn, CharSequence mFen,			
			CharSequence mTxt, CharSequence mIsCheck, CharSequence mIsMate, CharSequence mIsStealMate, CharSequence mBaseFen,	
			CharSequence mBaseIsCheck, CharSequence mControl)		
    {
		rank = mRank;
		variant = mVariant;
		fields = mFields;
		pgn = mPgn;
		fen = mFen;
		txt = mTxt;
		isCheck = mIsCheck;
		isMate = mIsMate;
		isStealMate = mIsStealMate;
		baseFen = mBaseFen;
		baseIsCheck = mBaseIsCheck;
		control = mControl;
		if (!baseFen.equals(""))
			color = getColorFromFen(baseFen);
    }

	public void setMoveFromHistory(CharSequence moveHistory)		
    {	// creating ChessMove from moveHistory
		CharSequence[] split = moveHistory.toString().split("\b");
			rank = split[0];
			variant = split[1];
			fields = split[2];
			pgn = split[3];
			fen = split[4];
			txt = split[5];
			isCheck = split[6];
			isMate = split[7];
			isStealMate = split[8];
			baseFen = split[9];
			baseIsCheck = split[10];
			control = split[11];

		if (!baseFen.equals(""))
			color = getColorFromFen(baseFen);
    }

	public void setMoveFromMoveList(CharSequence moveList)		
    {	// MoveList (parse game notation)
		rank = getVal(moveList, 2);
		variant = getVal(moveList, 3);
		fields = "";
		pgn = getVal(moveList, 4);
		fen = "";
		txt = getVal(moveList, 6);
		isCheck = "f";
		isMate = "f";
		isStealMate = "f";
		baseFen = "";
		baseIsCheck = "f";
		control = getVal(moveList, 1);
		color = getVal(moveList, 5);
    }

	public CharSequence getMoveHistory()		
    {	// creating MoveHistory from ChessMove 
		sb.setLength(0);
		sb.append(rank); 		sb.append("\b");
		sb.append(variant); 	sb.append("\b");
		sb.append(fields); 		sb.append("\b");
		sb.append(pgn); 		sb.append("\b");
		sb.append(fen); 		sb.append("\b");
		sb.append(txt); 		sb.append("\b");
		sb.append(isCheck); 	sb.append("\b");
		sb.append(isMate); 		sb.append("\b");
		sb.append(isStealMate); sb.append("\b");
		sb.append(baseFen); 	sb.append("\b");
		sb.append(baseIsCheck); sb.append("\b");
		sb.append(control); 	sb.append("\b");
//		Log.i(TAG, "sb.capacity(): " + sb.capacity());
		return sb.toString();
    }

	public CharSequence getVal(CharSequence move, int statId)		
    {	// get moveString from MoveHistory
//		Log.i(TAG, "move: " + move);
		CharSequence moveValue = "";
		CharSequence[] moveSplit = move.toString().split("\b");
		if (moveSplit.length >= statId)
			moveValue = moveSplit[statId -1];
//		Log.i(TAG, "statId, moveValue, txt: " + statId + ", " + moveValue + ">" + moveSplit[5] + "<");
		return moveValue;
    }

	public CharSequence getColorFromFen(CharSequence fen)		
    {
//		Log.i(TAG, "getColorFromFen: " + fen);
		CharSequence color = "l";
		CharSequence[] split = fen.toString().split(" ");
//error 20180319: java.lang.ArrayIndexOutOfBoundsException
		if (split.length > 1)
		{
			if (split[1].equals("b"))
				color = "d";
		}
		return color;
    }

	public CharSequence getMoveNumberFromFen(CharSequence fen)		
    {
		CharSequence[] split = fen.toString().split(" ");
		if (split.length >= 6)
			return split[5];
		else
			return "";
    }

	// get methods
	public CharSequence getRank() 			{return rank;}
	public CharSequence getVariant() 		{return variant;}
	public CharSequence getFields() 		{return fields;}
	public CharSequence getPgn() 			{return pgn;}
	public CharSequence getFen() 			{return fen;}
	public CharSequence getTxt() 			{return txt;}
	public CharSequence getIsCheck() 		{return isCheck;}
	public CharSequence getIsMate() 		{return isMate;}
	public CharSequence getIsStealMate() 	{return isStealMate;}
	public CharSequence getBaseFen() 		{return baseFen;}
	public CharSequence getBaseIsCheck() 	{return baseIsCheck;}
	public CharSequence getControl() 		{return control;}
	public CharSequence getColor() 			{return color;}

	// set methods
	public void setRank(CharSequence var) {rank = var;}
	public void setVariant(CharSequence var) {variant = var;}
	public void setFields(CharSequence var) {fields = var;}
	public void setPgn(CharSequence var) {pgn = var;}
	public void setFen(CharSequence var) {fen = var;}
	public void setTxt(CharSequence var) {txt = var;}
	public void setIsCheck(CharSequence var) {isCheck = var;}
	public void setIsMate(CharSequence var) {isMate = var;}
	public void setIsStealMate(CharSequence var) {isStealMate = var;}
	public void setBaseFen(CharSequence var) 
	{
		baseFen = var;
		if (!baseFen.equals(""))
			color = getColorFromFen(baseFen);
	}
	public void setBaseIsCheck(CharSequence var) {baseIsCheck = var;}
	public void setControl(CharSequence var) {control = var;}
	public void setColor(CharSequence var) {color = var;}

}
