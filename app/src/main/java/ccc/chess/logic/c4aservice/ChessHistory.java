package ccc.chess.logic.c4aservice;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import android.util.Log;

public class ChessHistory 
{
    public ChessHistory(ArrayList<CharSequence> values)
    {	// Constructor
    	stringValues = values;
	    moveHistory = new ArrayList<CharSequence>();
	    moveList = new ArrayList<CharSequence>();
    }
    public ChessHistory()
    {	// Constructor: final CharSequence tagState[]
    	
    }
    public void initGameData()
    {
	    isGameEnd = false;
	    pgnData = "";
	    
	    gameTags = "";
	    gameTags = gameTags + "Event" + "\b" + "?" + "\n";
	    gameTags = gameTags + "Site" + "\b" + "?" + "\n";
	    gameTags = gameTags + "Date" + "\b" + getDateYYYYMMDD() + "\n";
	    gameTags = gameTags + "Round" + "\b" + "-" + "\n";
	    gameTags = gameTags + "White" + "\b" + "?" + "\n";
	    gameTags = gameTags + "Black" + "\b" + "?" + "\n";
	    gameTags = gameTags + "Result" + "\b" + "*" + "\n";
	    gameNotation = "";
	    gamePos = 0;
	    gameColor = 'l';
	    gameText = "";
	    resultMessage = "";
	    chess960Id = 0;
	    moveHistory.clear();
	    setMoveIdx(0);
	    setMoveRank("0");
	    setMoveVariant("0");
	    moveHasVariations = false;			
	    moveIsFirstInVariation = false;		
	    moveIsLastInVariation = false;		
    }
    public void initMoveHistory(CharSequence fen, CharSequence isMate, CharSequence isStaleMate)			
    {
    	moveHistory.clear();
    	chessMove.setMove("0", "0", "", "", fen, "", "f", isMate, isStaleMate, fen, "f", "0");
        addToMoveHistory(chessMove, false);	// start variation ("0")
        chessMove.setMove("0", "0", "", "", fen, "", "f", isMate, isStaleMate, fen, "f", "9");
        addToMoveHistory(chessMove, false);	// end   variation ("9")
        setMoveIdx(0);			// set move-history-Index to first index
        setNextMoveHistory(0);
    }
    public void addToMoveHistory(ChessMove chessMove, boolean addToVariationEnd)
    {
    	if (addToVariationEnd)	// insert to variationEnd(ChessMove.control : "9")	-	new move
    	{
    		moveIsFirst = false;
    		int existingIdx = getMoveIdxFromExistingVariation(getMoveIdx(), chessMove);
    		if (variationExist)
    		{
    			setMoveIdx(existingIdx);
    			setNextMoveHistory(0);
    			return;
    		}
    		int idxEnd = getNextMoveIdxFromVariation(getMoveIdx(), chessMove);
//    		Log.i(TAG, "getMoveIdx(), idxEndVariation, moveIsLastInVariation: " + getMoveIdx() + ", " + idxEnd + ", " + moveIsLastInVariation);
    		if (isNewBranche)
    		{
    	        int insertIdx = newBrancheMoveIdx;
    	        ChessMove cm = new ChessMove();
    			cm.setMove(newBrancheR, newBrancheV, "", "", 
    					chessMove.getFen(), chessMove.getTxt(), chessMove.getIsCheck(), chessMove.getIsMate(), 
    					chessMove.getIsStealMate(), chessMove.getBaseFen(), chessMove.getBaseIsCheck(), "9");
    			moveHistory.add(insertIdx, cm.getMoveHistory());			// variation end
    			
    			cm = new ChessMove();
    			cm.setMove(newBrancheR, newBrancheV, chessMove.getFields(), chessMove.getPgn(), 
    					chessMove.getFen(), chessMove.getTxt(), chessMove.getIsCheck(), chessMove.getIsMate(), 
    					chessMove.getIsStealMate(), chessMove.getBaseFen(), chessMove.getBaseIsCheck(), "1");
    			moveHistory.add(insertIdx, cm.getMoveHistory());		// move
    			
    			cm = new ChessMove();
    			cm.setMove(newBrancheR, newBrancheV, "", "", 
    					newBrancheFen, chessMove.getTxt(), chessMove.getIsCheck(), chessMove.getIsMate(), 
    					chessMove.getIsStealMate(), newBrancheFen, chessMove.getBaseIsCheck(), "0");
    			moveHistory.add(insertIdx, cm.getMoveHistory());			// variation start
    			
    			setMoveIdx(insertIdx +1);
    		    setNextMoveHistory(0);
//    			Log.i(TAG, "getMoveIdx(): " + getMoveIdx());
    		}
    		else
    		{
	    		if (idxEnd != -1)
	    		{	// move to variation end
	    			ChessMove cm = new ChessMove();
	    			cm.setMoveFromHistory(moveHistory.get(idxEnd));
	    			cm.setFen(chessMove.getFen());
	    			moveHistory.set(idxEnd, cm.getMoveHistory());
	    			CharSequence prevMove = getPreviousValueFromMoveHistory(idxEnd, chessMove.getRank(), chessMove.getVariant(), "9");
	    			chessMove.setBaseFen(chessMove.getVal(prevMove, 5));
	    			moveHistory.add(idxEnd, chessMove.getMoveHistory());
	   				isGameEnd = true;
	    			moveIsFirstInVariation = false;
	    			setMoveIdx(idxEnd);
	    		}
    		}
    	}
    	else					// insert to moveHistory end	-	create moveHistory
    	{
	    	moveHistory.add(chessMove.getMoveHistory());
	    	setMoveIdx(moveHistory.size() -1);
    	}
    }
    @SuppressWarnings("unchecked")
	public void deleteMovesFromHistory(int fromIdx)
    {
//    	Log.i(TAG, "fromIdx: " + fromIdx);
    	CharSequence initRank = "";
    	CharSequence initVariant = "";
    	boolean movesDeleted = false;
    	boolean deleteFullVariation = false;
    	moveHistoryCopy = (ArrayList<CharSequence>)moveHistory.clone();
    	try
    	{
	    	if (moveHistory.size() >= fromIdx)
	    	{
	    		initRank = chessMove.getVal(moveHistory.get(fromIdx), 1);	
	    		initVariant = chessMove.getVal(moveHistory.get(fromIdx), 2);
	    		if (fromIdx > 0)
    			{
	    			if (		chessMove.getVal(moveHistory.get(fromIdx -1), 12).equals("0")
	    					& 	chessMove.getVal(moveHistory.get(fromIdx -1), 1).equals(initRank)
	    					& 	chessMove.getVal(moveHistory.get(fromIdx -1), 2).equals(initVariant))
	    			{
	    				fromIdx--;
	    				deleteFullVariation = true;
	    			}
    			}
	    	}
	    	for (int i = fromIdx; i <= moveHistory.size() -1; i = i+0)
	        {
	    		if	(		chessMove.getVal(moveHistory.get(i), 1).equals(initRank) 
	    				& 	chessMove.getVal(moveHistory.get(i), 2).equals(initVariant)
	    				&	chessMove.getVal(moveHistory.get(i), 12).equals("9")
	    			)
	    		{
	    			if (deleteFullVariation)
	    				moveHistory.remove(i);
	    			break;
	    		}
	    		else
	    		{
	    			movesDeleted = true;
	    			moveHistory.remove(i);
	    		}
	        }
	    	if (movesDeleted)
	    	{
	    		int newMoveIdx = fromIdx -1;
	    		boolean hasPrevVar = false;
	    		for (int i = newMoveIdx; i >= 0; i--)
    	        {
    	    		if (chessMove.getVal(moveHistory.get(i), 12).equals("1"))
    	    		{
    	    			newMoveIdx = i;
    	    			if (!hasPrevVar)
    	    				break;
    	    			else
    	    			{
    	    				if (chessMove.getVal(moveHistory.get(i -1), 12).equals("0"))
    	    					break;
    	    			}
     	    		}
    	    		else
    	    		{
    	    			if (chessMove.getVal(moveHistory.get(i), 12).equals("9"))
    	    			{
//    	    				Log.i(TAG, "prev. idx: " + i);
    	    				hasPrevVar = true;
    	    			}
    	    		}
    	        }
     			setMoveIdx(newMoveIdx);
	    		setGameTag("Result", "*");
	    	}
    	}
    	catch (IndexOutOfBoundsException e) {e.printStackTrace(); moveHistory = (ArrayList<CharSequence>)moveHistoryCopy.clone(); return;}
    }
    public void printMoveHistory()	// TEST only
    {
    	if (moveHistory.size() != 0)
    	{
	    	Log.i(TAG, "MoveHistory(Idx, Control, Rank, Variant, Fields, Pgn, Fen):\n");
	    	for (int i = 0; i < moveHistory.size(); i++)
	        {
	    		Log.i(TAG, 	i + " "
	    				+	chessMove.getVal(moveHistory.get(i), 12) + " "
	    				+	chessMove.getVal(moveHistory.get(i), 1) + " "
	    				+	chessMove.getVal(moveHistory.get(i), 2) + " "
	    				+	chessMove.getVal(moveHistory.get(i), 3) + " "
	    				+	chessMove.getVal(moveHistory.get(i), 4) + " "
	    				+	chessMove.getVal(moveHistory.get(i), 5) + " ");
//	    		Log.i(TAG, 	"   >>> baseFen: "
//	    				+	chessMove.getVal(moveHistory.get(i), 10));
	        }
	    	Log.i(TAG, "moveIdx: " + getMoveIdx());
//	    	Log.i(TAG, "moveIdx   , baseFEN: " + getMoveIdx() + ", " + chessMove.getVal(moveHistory.get(getMoveIdx()), 10));
    	}
    }
    public CharSequence getPreviousValueFromMoveHistory(int idx, CharSequence rank, CharSequence variant, CharSequence control)
    {
    	CharSequence previousValue = "";
//    	Log.i(TAG, "idx, rank, variant, control: " + idx + ", " + rank + ", " + variant + ", " + control);
    	idx--;
    	if (idx >= 0)
    	{
    		if (control.equals("1"))
        		return moveHistory.get(idx);
    		if (control.equals("9"))
    		{
	        	for (int i = idx; i >= 0; i--)
		        {
		    		if 	(		chessMove.getVal(moveHistory.get(i), 1).equals(rank)
		    				& 	chessMove.getVal(moveHistory.get(i), 2).equals(variant)
		    				& 	(chessMove.getVal(moveHistory.get(i), 12).equals("1") | chessMove.getVal(moveHistory.get(i), 12).equals("0"))
		    			)
					{
		    			previousValue = moveHistory.get(i);
//		    			Log.i(TAG, "prev. idx: " + i);
		    			break;
					}
		        }
    		}
    	}
//    	Log.i(TAG, "previousValue(SPACE): " + previousValue);
    	return previousValue;
    }
    public CharSequence getBaseFenForNewVariation(int idx, CharSequence newRank, CharSequence newVariant)
    {
    	CharSequence baseFen = "";
    	boolean isFirst = true;
    	CharSequence preRank = "";
    	CharSequence preVariant = "";
    	for (int i = idx; i >= 0; i--)
        {
    		if (isFirst)
    		{
    			isFirst = false;
    			if (chessMove.getVal(moveHistory.get(i), 12).equals("1"))				// new move
    			{
    				return chessMove.getVal(moveHistory.get(i), 10);					// baseFen from last move
    			}
    			if (chessMove.getVal(moveHistory.get(i), 12).equals("9"))				// end previous variation
    			{
    				preRank = chessMove.getVal(moveHistory.get(i), 1);
    				preVariant = chessMove.getVal(moveHistory.get(i), 2);
    			}
    		}
    		else
    		{
    			if (		chessMove.getVal(moveHistory.get(i), 12).equals("0")
    					& 	chessMove.getVal(moveHistory.get(i), 1).equals(preRank)
    					&	chessMove.getVal(moveHistory.get(i), 2).equals(preVariant))	// start previous variation
    				return chessMove.getVal(moveHistory.get(i), 10);					// baseFen from previous variation
    		}
        }
    	return baseFen;
    }
    public CharSequence getBaseFenFromLastMove(int idx, CharSequence newRank, CharSequence newVariant)
    {
    	CharSequence baseFen = "";
    	for (int i = idx -1; i >= 0; i--)
        {
    		if (		chessMove.getVal(moveHistory.get(i), 1).equals(newRank)
    				& 	chessMove.getVal(moveHistory.get(i), 2).equals(newVariant))
    			return chessMove.getVal(moveHistory.get(i), 5);							// fen from last move == baseFen
        }
    	return baseFen;
    }
    public int getMoveIdxFromVariationStart(int currentMoveIdx, boolean lastSixMoves)
    {
    	int startIdx = currentMoveIdx;
    	boolean searchVariation = false;
    	CharSequence rank = chessMove.getVal(moveHistory.get(currentMoveIdx), 1);
    	CharSequence variant = chessMove.getVal(moveHistory.get(currentMoveIdx), 2);
    	if (lastSixMoves)
    	{
    		int startSixIdx;
    		if (currentMoveIdx - 6 < 0)
    			startSixIdx = 0;
    		else
    			startSixIdx = currentMoveIdx - 6;
    		CharSequence sixRank = chessMove.getVal(moveHistory.get(startSixIdx), 1);
    		CharSequence sixVariant = chessMove.getVal(moveHistory.get(startSixIdx), 2);
	    	if (	sixRank.equals(rank)
    			& 	sixVariant.equals(variant))
	    	{
	    		startIdx = startSixIdx;
	    	}
	    	else
	    		searchVariation = true;
    	}
    	else
    		searchVariation = true;
    	if (searchVariation)
    	{
	    	for (int i = currentMoveIdx -1; i >= 0; i--)
	        {
	    		if (		chessMove.getVal(moveHistory.get(i), 1).equals(rank)
	    				& 	chessMove.getVal(moveHistory.get(i), 2).equals(variant))
				{
	    			if (chessMove.getVal(moveHistory.get(i), 12).equals("0"))			// new variation
	    				break;
	    			else
	    				startIdx = i;
				}
	        }
    	}
    	return startIdx;
    }
    public int getMoveIdxFromExistingVariation(int currentMoveIdx, ChessMove newChessMove)
    {
    	int existingIdx = -1;
    	boolean isFirstVariaton = false;
    	boolean isFirstMove = true;
    	isNewBranche = false;
    	hasVariations = false;
    	variationExist = false;
    	if (moveHistory.size() > 0)
    	{	
    		CharSequence rank = chessMove.getVal(moveHistory.get(currentMoveIdx), 1);
    		CharSequence variant = chessMove.getVal(moveHistory.get(currentMoveIdx), 2);
    		CharSequence varRank = "";
	    	for (int i = currentMoveIdx +1; i <= moveHistory.size() -1; i++)
	        {
	    		if 	(		chessMove.getVal(moveHistory.get(i), 1).equals(rank) 
	    				& 	chessMove.getVal(moveHistory.get(i), 2).equals(variant)
	    				& 	chessMove.getVal(moveHistory.get(i), 12).equals("9")
	    			)	// end of variation
	    			break;
	    		if (!varRank.equals("") & chessMove.getVal(moveHistory.get(i), 1).equals(varRank))
	    		{
	    			if (newChessMove != null & chessMove.getVal(moveHistory.get(i), 12).equals("0"))				
	    			{	// has variations!
	    				
	    				if (newChessMove.getPgn().equals(chessMove.getVal(moveHistory.get(i +1), 4)))
						{	// variation exist
							variationExist = true;
							existingIdx = i +1;
	    					break;
						}
	    			}
	    		}
	    		if (chessMove.getVal(moveHistory.get(i), 1).equals(rank))
	    		{
	    			if (!varRank.equals("") & chessMove.getVal(moveHistory.get(i -1), 1).equals(varRank))	
	    				break;
	    			if (newChessMove != null & chessMove.getVal(moveHistory.get(i), 12).equals("1"))	// new branch!
	    			{
//		    			Log.i(TAG, "moveHistory, i: " + moveHistory.get(i) + ">" + i  + "<");
	    				if (!isFirstMove)
	    					break;
	    				isFirstMove = false;
	    				if (newChessMove.getPgn().equals(chessMove.getVal(moveHistory.get(i), 4)))
						{
							variationExist = true;
	    					existingIdx = i;
	    					break;
						}
		    			if (!isNewBranche)
	    				{
		    				if (chessMove.getVal(moveHistory.get(i +1), 12).equals("0"))
		    				{
		    					if (!isFirstVariaton)
			    				{
		    						isFirstVariaton = true;
			    					hasVariations = true;
			    					varRank = chessMove.getVal(moveHistory.get(i +1), 1);
			    				}
		    				}
	    				}
	    			}
	    		}
	        }
    	}
    	return existingIdx;
    }
    public int getNextMoveIdxFromVariation(int currentMoveIdx, ChessMove newChessMove)
    {
    	int endIdx = -1;
    	boolean isFirst = true;
    	isNewBranche = false;
    	hasVariations = false;
        newBrancheMoveIdx = 0;
        newBrancheV = "1";
    	if (moveHistory.size() > 0)
    	{	
    		CharSequence rank = chessMove.getVal(moveHistory.get(currentMoveIdx), 1);
    		CharSequence variant = chessMove.getVal(moveHistory.get(currentMoveIdx), 2);
    		CharSequence varRank = "";
	    	for (int i = currentMoveIdx +1; i <= moveHistory.size() -1; i++)
	        {
	    		if (!varRank.equals("") & chessMove.getVal(moveHistory.get(i), 1).equals(varRank))
	    		{
	    			if (newChessMove != null & chessMove.getVal(moveHistory.get(i), 12).equals("0"))				
	    			{	// has variations!
	    				
    					hasVariations = true;
    					isNewBranche = false;
    					newBrancheR = varRank;
    			        newBrancheV = chessMove.getVal(moveHistory.get(i), 2);
    			        newBrancheV = String.valueOf(Integer.parseInt(newBrancheV.toString()) +1);
	    			}
	    		}
	    		if (chessMove.getVal(moveHistory.get(i), 1).equals(rank))
	    		{
		    		if 	(chessMove.getVal(moveHistory.get(i), 2).equals(variant))
					{
		    			if (newChessMove != null & chessMove.getVal(moveHistory.get(i), 12).equals("1"))	// new branch!
		    			{
			    			if (!isNewBranche)
			    				{
				    				if (!chessMove.getVal(moveHistory.get(i +1), 12).equals("0"))
				    				{
					    				isNewBranche = true;
					    				if (hasVariations)
					    				{
					    					newBrancheMoveIdx = i;
					    					newBrancheV = String.valueOf(Integer.parseInt((String) chessMove.getVal(moveHistory.get(i -1), 2)) +1);	
					    				}
					    				else
					    				{
					    					newBrancheMoveIdx = i +1;
					    					newBrancheV = "1";
					    				}
//					    				Log.i(TAG, "idx, newBrancheMoveIdx: " + i + ", " + newBrancheMoveIdx);
					    				if (isFirst)
					    				{
						    				cntRank++;
						    				newBrancheR = String.valueOf(cntRank);
					    				}
					    				else
					    					newBrancheR = varRank;
					    				newBrancheFen = chessMove.getVal(moveHistory.get(currentMoveIdx), 5);
//					    		        Log.i(TAG, "hasVariations, newBrancheMoveIdx: " + hasVariations + ", " + newBrancheMoveIdx);
					    		        break;
				    				}
				    				else
				    				{
				    					if (isFirst)
					    				{
					    					isFirst = false;
					    					hasVariations = true;
					    					varRank = chessMove.getVal(moveHistory.get(i +1), 1);
					    				}
				    				}
			    				}
		    			}
		    			if 	(chessMove.getVal(moveHistory.get(i), 12).equals("9"))				// end variation
		    			{
		    				endIdx = i;
		    				break;
		    			}
					}
	    		}
	        }
	    	if (hasVariations & !isNewBranche)
	    	{
	    		isNewBranche = true;
				newBrancheMoveIdx = endIdx;
	    	}
    	}
    	return endIdx;
    }
    public int getBaseMoveIdxFromVariation(int currentMoveIdx)
    {
    	int baseMoveIdx = currentMoveIdx;
    	boolean isStart = false;
    	CharSequence rank = chessMove.getVal(moveHistory.get(currentMoveIdx), 1);
    	CharSequence control = chessMove.getVal(moveHistory.get(currentMoveIdx), 12);
    	if (control.equals("0"))	// start variation
    	{
    		isStart = true;
	    	for (int i = currentMoveIdx -1; i >= 0; i--)
	        {
	    		if (chessMove.getVal(moveHistory.get(i), 1).equals(rank))
				{
	    			if (chessMove.getVal(moveHistory.get(i), 12).equals("0"))			// start variation
    					isStart = true;
	    			else
	    				isStart = false;
				}
	    		else
	    		{
	    			if (isStart)
	    			{
	    				isStart = false;
	    				if (chessMove.getVal(moveHistory.get(i), 12).equals("1"))		// baseMove
	    				{
	    					baseMoveIdx = i;
		    				break;
	    				}
	    			}
	    		}
	        }
    	}
    	return baseMoveIdx;
    }
    public boolean isLastMoveInVariation(int currentMoveIdx)
    {
    	boolean isLastMove = false;
    	CharSequence rank = chessMove.getVal(moveHistory.get(currentMoveIdx), 1);
    	CharSequence variant = chessMove.getVal(moveHistory.get(currentMoveIdx), 2);
    	for (int i = currentMoveIdx +1; i <= moveHistory.size(); i++)
        {
    		if (chessMove.getVal(moveHistory.get(i), 1).equals(rank) & chessMove.getVal(moveHistory.get(i), 2).equals(variant))
    		{
    			if (chessMove.getVal(moveHistory.get(i), 12).equals("1"))
    				break;
    			if (chessMove.getVal(moveHistory.get(i), 12).equals("9"))
    			{
    				isLastMove = true;
    				break;
    			}
    		}
        }
    	return isLastMove;
    }
    public int getMoveHistorySize() {return moveHistory.size();}
    public void setGameTag(String tagName, String tagValue)
    {
//    	Log.i(TAG, "tagName, tagValue: " + tagName + " >" + tagValue + "<");
    	if (tagValue.equals(""))
    		return;
    	String newGameTags = "";
    	boolean isTag = false;
    	String[] txtSplit = gameTags.toString().split("\n");
		for(int i = 0; i < txtSplit.length; i++)
	    {
//			Log.i(TAG, "txtSplit[i]: " + txtSplit[i]);
			if (txtSplit[i].contains("\b"))
			{
				String[] txtTags = txtSplit[i].split("\b");
				if (txtTags[0].equals(tagName))
				{
					newGameTags = newGameTags + txtTags[0] + "\b" + tagValue + "\n";
					isTag = true;
				}
				else 	
					newGameTags = newGameTags + txtTags[0] + "\b" + txtTags[1] + "\n";
			}
	    }
		if (!isTag)
			newGameTags = newGameTags + tagName + "\b" + tagValue + "\n";
		gameTags = newGameTags;
//		Log.i(TAG, "gameTags: \n" + gameTags);
    }
    public String getGameTagValue(String tagName)
    {
//    	Log.i(TAG, "get(), tagName: " + tagName);
//    	Log.i(TAG, "gameTags: " + gameTags);
    	String tagValue = "";
    	String[] txtSplit = gameTags.toString().split("\n");
		for(int i = 0; i < txtSplit.length; i++)
	    {
			if (txtSplit[i].contains("\b"))
			{
				String[] txtTags = txtSplit[i].split("\b");
				if (txtTags.length > 1 & txtTags[0].equals(tagName))
				{
					tagValue = txtTags[1];
//					Log.i(TAG, "get(), tagName, tagValue: " + txtTags[0] + ", " + tagValue);
					break;
				}
			}
	    }
    	return tagValue;
    }
    public CharSequence createGameDataFromHistory(int tagVar)
    {
        sbData.setLength(0);
        String[] txtSplit = gameTags.toString().split("\n");
		for(int i = 0; i < txtSplit.length; i++)
	    {
			if (txtSplit[i].contains("\b"))
			{
//				Log.i(TAG, "txtSplit[i]: " + txtSplit[i]);
				String[] txtTags = txtSplit[i].split("\b");
				String tagName = "";
				String tagValue = "";
				if (txtTags.length > 0)
					tagName = txtTags[0];
				if (txtTags.length > 1)
					tagValue = txtTags[1];
				if (!tagName.equals(""))
				{
					if (tagValue.equals(""))
						getInitValueFromTag(tagName);
					char xx = '"';
			        sbGameData.setLength(0);
			        sbGameData.append("[");	
			        sbGameData.append(tagName);
			        sbGameData.append(" ");
			        sbGameData.append(xx);
			        sbGameData.append(tagValue);
			        sbGameData.append(xx);
			        sbGameData.append("]\n");
			        sbData.append(sbGameData);
				}
			}
	    }
        return sbData;
    }
    public CharSequence createGameNotationFromHistory(int moveIdx, boolean isOutputMoveText, boolean isResult, boolean figurineAlgebraicNotaion, 
    							boolean rankOnly, boolean lastSixMoves, boolean isSpaceAfterNumber, int nagControl)
    {	// nagControl: 0 = none, 1 = variable name(e.g. $11), 2 nag symbol (e.g.: =)
//    	Log.i(TAG, "createGameNotationFromHistory(): " + isOutputMoveText);
    	sbNotation.setLength(0);
        boolean isFirstMove = true;
        boolean isNewVariation = false;
        boolean isEndVariation = false;
        boolean isMoveText = false;
        boolean isNewLine = false;
        ChessMove cm = new ChessMove();
        ChessMove cmPrev = new ChessMove();
        ChessMove cmNext = new ChessMove();
        CharSequence rank;
        CharSequence variant;
        int cntIndent = 0;
        int fromIdx = 0;
        if (moveIdx > moveHistory.size() -1)
        	moveIdx = moveHistory.size() -1;
// ERROR	v1.8	24.11.2011 10:25:37
        try
        {
	        if (rankOnly)
	        {	// only rank: "0" (canceled)
	        	cm.setMoveFromHistory(moveHistory.get(moveIdx));
	        	rank = cm.getRank();
	        	variant = cm.getVariant();
	        	fromIdx = getMoveIdxFromVariationStart(moveIdx, lastSixMoves);
	        	if (!rank.equals("0"))					// variation!
	        	{
	        		if (moveIsFirstInVariation)			// move is first in variation
	        			isNewVariation = true;
	        		if (cm.getControl().equals("0"))	// start variation: set first move
	        		{
		        		isNewVariation = true;
		        		moveIdx++;
		        		cm.setMoveFromHistory(moveHistory.get(moveIdx));
		            	rank = cm.getRank();
		            	variant = cm.getVariant();
		            	fromIdx = moveIdx;
	        		}
	        	}
	        }
	        else
	        {	// with variations
	        	cm.setMoveFromHistory(moveHistory.get(0));
	        	rank = cm.getRank();
	        	variant = cm.getVariant();
	        	fromIdx = 0;
	        }
	        for (int i = fromIdx; i <= moveIdx; i++)
	        {
	        	cm.setMoveFromHistory(moveHistory.get(i));
	        	if 	(	cm.getControl().equals("0") & cm.getRank().equals("0") & cm.getVariant().equals("0") 
	        			& 	!cm.getTxt().equals("") & isOutputMoveText
	        		)	// comment before first move
	        	{
	        		sbNotation.append("{");
	            	sbNotation.append(cm.getTxt());
	            	sbNotation.append("} ");
	        	}
	        	if (i > 0)
	        		cmPrev.setMoveFromHistory(moveHistory.get(i -1));
	        	if (i < moveIdx)
	        		cmNext.setMoveFromHistory(moveHistory.get(i +1));
//	        	Log.i(TAG, "move, C, R, V: " + cm.getPgn() + ", " + cm.getControl() + ", " + cm.getRank() + ", " + cm.getVariant());
	        	if (!rankOnly & cm.getControl().equals("0") & !cm.getRank().equals(rank))	// new variation
	        	{
	        		cntIndent++;
	            	for(int j = 0; j < cntIndent; j++)
	                {
	            		if (j == 0)
	            		{
	            			if (!isNewLine)
	            			{
	            				sbNotation.append("\n");
	            				sbNotation.append(pgnIndent);
	            				isNewLine = true;
	            			}
	            			else
	            				sbNotation.append(pgnIndent);
	            		}
	            		else
	            			sbNotation.append(pgnIndent);
	                }
	            	sbNotation.append("(");
	            	CharSequence moveText = cm.getTxt();
	            	if (!moveText.equals("") & isOutputMoveText)
		            {
		            	sbNotation.append("{");
		            	sbNotation.append(moveText);
		            	sbNotation.append("} ");
		            }
	         		isNewVariation = true;
	        		isEndVariation = false;
	        	}
	        	if (!rankOnly | (rankOnly & rank.equals(cm.getRank()) & variant.equals(cm.getVariant())))
	        	{
		        	if (cm.getControl().equals("1"))
		        	{
		        		CharSequence spaceAfterMove = "";
		        		if (i +1 < moveHistory.size())
		        		{
			        		if (!cm.getVal(moveHistory.get(i +1), 12).equals("9"))
			        		{
			        			spaceAfterMove = " ";
			        		}
		        		}
		        		if (isEndVariation)
		        		{
		        			if (cmNext.getRank().equals("0"))
		        			{
		        				if (!isNewLine)
		        				{
		        					sbNotation.append("\n");
		        					isNewLine = true;
		        				}
		        			}
		        			else
		        			{
		        				if (cntIndent == 0)
		        					sbNotation.append("\n");
		        				else
		        				{
			    	            	for(int j = 0; j < cntIndent; j++)
			    	                {
			    	            		if (!isNewLine)
			    	            		{
			    	            			sbNotation.append("\n");
			    	            			sbNotation.append(pgnIndent);
			    	            			isNewLine = true;
			    	            		}
			    	            		else
			    	            			sbNotation.append(pgnIndent);
			    	                }
		        				}
		        			}
		        		}
			            if (getValueFromFen(2, cm.getBaseFen()).equals("w"))
			            {
			            	sbNotation.append(getValueFromFen(6, cm.getBaseFen()));
			            	if (isSpaceAfterNumber)
			            		sbNotation.append(". ");
			            	else
			            		sbNotation.append(".");
			            	sbNotation.append(getFigurineAlgebraicNotation(cm.getPgn(), figurineAlgebraicNotaion));
			            }
			            else
			            {
//			            	Log.i(TAG, "rankOnly: " + rankOnly + ", " + cm.getRank() + ", " + cmPrev.getControl());
			            	if (isNewVariation | isEndVariation | isFirstMove | isMoveText | rankOnly & cmPrev.getControl().equals("0"))
			            	{
			            		if (!isMoveText)
			            		{
			            			sbNotation.append(getValueFromFen(6, cm.getBaseFen()));
			            			sbNotation.append("... ");
			            		}
			            		else
			            		{
				            		if (isOutputMoveText)
				            		{
				            			sbNotation.append(getValueFromFen(6, cm.getBaseFen()));
				            			sbNotation.append("... ");
				            		}
			            		}
			            		isMoveText = false;
			            	}
			            	sbNotation.append(getFigurineAlgebraicNotation(cm.getPgn(), figurineAlgebraicNotaion));
			            }
			            isFirstMove = false;
			            isNewVariation = false;
			            isEndVariation = false;
			            isNewLine = false;
			            CharSequence moveText = cm.getTxt();
			            CharSequence nagText = "";
			            if (moveText.toString().startsWith("$"))
			            {
			            	if (moveText.toString().contains(" "))
			            	{
			            		CharSequence[] txtSplit = moveText.toString().split(" ");
			            		if (txtSplit.length > 0)
			            			nagText = txtSplit[0];
			            		if (txtSplit.length > 1)
			            		{
				            		moveText = moveText.subSequence(txtSplit[0].length() +1, moveText.length());
			            		}
			            		else
				            	{
				            		moveText = "";
				            	}
			            	}
			            	else
			            	{
			            		nagText = moveText;
			            		moveText = "";
			            	}
			            	if (!nagText.toString().equals(""))
			            	{
				            	switch (nagControl)
				    	        {
				    		        case 1:	sbNotation.append(" "); sbNotation.append(nagText); break;
				    		        case 2: sbNotation.append("{"); sbNotation.append(getNagStringFromString(nagText)); 
				    		        		sbNotation.append("}"); break;
				    	        }
			            	}
			            }
//			            if (!moveText.equals("") | !nagText.equals(""))		// TEST  only
//			            {
//					     Log.i(TAG, "nagText, moveText, nagControl, isOutputMoveText: <" + nagText + "><" + moveText 
//					    		 + "><" + nagControl + "><" + isOutputMoveText + ">");
//			            }
			            if (!moveText.equals("") & isOutputMoveText)
			            {
//			            	Log.i(TAG, "move text: " + cm.getTxt());
			            	sbNotation.append(" {");
			            	sbNotation.append(moveText);
			            	sbNotation.append("}");
			            	isMoveText = true;
			            }
			            sbNotation.append(spaceAfterMove);
		        	}
	        	}
	        	if (!rankOnly & cm.getControl().equals("9") & !cm.getRank().equals(rank))	// end variation
	        	{
	        		sbNotation.append(")");
	        		cntIndent--;
	        		isEndVariation = true;
	        	}
	        }
        }
        catch (IndexOutOfBoundsException e) {e.printStackTrace(); return "";}
        if (isResult)
        {
        	if (sbNotation.toString().endsWith(")"))
        		sbNotation.append("\n");
        	else
        		sbNotation.append(pgnIndent);
        	sbNotation.append(getGameTagValue("Result"));
        }
//        if (!isOutputMoveText)
//        	Log.i(TAG, "sbNotation: \n" + sbNotation);
        return sbNotation.toString();
    }
    public CharSequence createPgnFromHistory(int nagControl)
    {
        sbPgn.setLength(0);
        sbPgn.append(createGameDataFromHistory(0));
        sbPgn.append("\n");
        sbPgn.append(createGameNotationFromHistory(moveHistory.size(), true, true, false, false, false, true, 1));
        sbPgn.append("\n");
        return sbPgn.toString();
    }
    public CharSequence getNagStringFromString(CharSequence nat) 
   	{
       	if (nat.equals("$1")) {nat = stringValues.get(26);}
       	if (nat.equals("$2")) {nat = stringValues.get(27);}
       	if (nat.equals("$3")) {nat = stringValues.get(28);}
       	if (nat.equals("$4")) {nat = stringValues.get(29);}
       	if (nat.equals("$5")) {nat = stringValues.get(30);}
       	if (nat.equals("$6")) {nat = stringValues.get(31);}
       	if (nat.equals("$7")) {nat = stringValues.get(32);}
       	if (nat.equals("$8")) {nat = stringValues.get(33);}
       	if (nat.equals("$9")) {nat = stringValues.get(34);}
       	if (nat.equals("$10")) {nat = stringValues.get(35);}
       	if (nat.equals("$11")) {nat = stringValues.get(36);}
       	if (nat.equals("$12")) {nat = stringValues.get(37);}
       	if (nat.equals("$13")) {nat = stringValues.get(38);}
       	if (nat.equals("$14")) {nat = stringValues.get(39);}
       	if (nat.equals("$15")) {nat = stringValues.get(40);}
       	if (nat.equals("$16")) {nat = stringValues.get(41);}
       	if (nat.equals("$17")) {nat = stringValues.get(42);}
       	if (nat.equals("$18")) {nat = stringValues.get(43);}
       	if (nat.equals("$19")) {nat = stringValues.get(44);}
       	
       	if (nat.toString().startsWith("$"))
       		nat = "";
       	return nat;
   	}
// set methods		set methods		set methods		set methods		set methods		set methods	
    public void setFileBase(CharSequence gFileBase) {fileBase = gFileBase;}
    public void setFilePath(CharSequence gFilePath) {filePath = gFilePath;}
    public void setFileName(CharSequence gFileName) {fileName = gFileName;}
    public void setGameData(CharSequence pgn)	// NEW
    {
    	if (pgn.equals(""))
    		return;
//    	Log.i(TAG, "setGameData, pgn: " + pgn);
    	boolean isTagSection = true;
	    setPgnData(pgn);
	    CharSequence[] pgnSplit = pgn.toString().split("\n|\r");
	    sbGameNotation.setLength(0);
	    gameTags = "";
	    for(int i = 0; i < pgnSplit.length; i++)
	    {
//	    	Log.i(TAG, "line: >" + pgnSplit[i] + "<");
	    	
	    	for (int j = 0; j < pgnSplit[i].length(); j++)
	    	{
		    	if (pgnSplit[i].toString().startsWith(" ") & isTagSection)
		    		pgnSplit[i] = pgnSplit[i].subSequence(1, pgnSplit[i].length());
		    	else
		    		break;
	    	}
	    	
	    	if (pgnSplit[i].toString().startsWith("[") & isTagSection)
	    		setGameTags(pgnSplit[i].toString());
	    	else
	    	{
	    		if (!pgnSplit[i].equals(""))
	    		{
		    		isTagSection = false;
			    	sbGameNotation.append(pgnSplit[i]);
			    	sbGameNotation.append("\n");
	    		}
	    	}
	    }
	    
	    // gameTags: insert all seven tag rosters !!!
	    gameTags = validateGameTags(gameTags);	
	    gameNotation = sbGameNotation;
    }
    public void setPgnData(CharSequence gPgnData) {pgnData = gPgnData;}
    public void setGameTags(String lineValue)	
    {
    	String tagName = "";
		String tagValue = "";
		String nextTag = "";
		try
		{
			int startValues = lineValue.indexOf('"') +1;
//			Log.i(TAG, "lineValue, startValues: " + lineValue + ", " + startValues);
			if (startValues > 1 & startValues < lineValue.length())
			{
				for (int i = startValues; i < lineValue.length(); i++)
			    {
					if (lineValue.charAt(i) == '"' & lineValue.charAt(i +1) == ']')
					{
						for (int h = i +2; h < lineValue.length(); h++)
					    {
							if (lineValue.charAt(h) == '[')
								nextTag = lineValue.substring(h, lineValue.length());
					    }
						break;
					}
					tagValue = tagValue + lineValue.charAt(i);
			    }
			}
			String[] txtSplit = lineValue.split(" ");
			if (txtSplit.length > 0)
				tagName =  txtSplit[0].replace("[", "");
		}
		catch (IndexOutOfBoundsException e) {e.printStackTrace(); tagValue = "";}
		if (tagValue.equals(""))
			tagValue = getInitValueFromTag(tagName);
//		Log.i(TAG, "tagName, tagValue: " + tagName + ", " + tagValue);
		gameTags = gameTags + tagName + "\b" + tagValue + "\n";
        if (!nextTag.equals(""))
        	setGameTags(nextTag);
    }
    public String validateGameTags(String gameTags)	
    {
    	String newGameTags = "";
    	newGameTags = addNewTag(newGameTags, gameTags, GAME_TAG_EVENT);
    	newGameTags = addNewTag(newGameTags, gameTags, GAME_TAG_SITE);
    	newGameTags = addNewTag(newGameTags, gameTags, GAME_TAG_DATE);
    	newGameTags = addNewTag(newGameTags, gameTags, GAME_TAG_ROUND);
    	newGameTags = addNewTag(newGameTags, gameTags, GAME_TAG_WHITE);
    	newGameTags = addNewTag(newGameTags, gameTags, GAME_TAG_BLACK);
    	newGameTags = addNewTag(newGameTags, gameTags, GAME_TAG_RESULT);
    	newGameTags = addNewTag(newGameTags, gameTags, "");
    	return newGameTags;
    }
    public String addNewTag(String newTags, String oldTags, String tag)	
    {
    	String[] txtSplit;
    	if (tag.equals(""))
    	{
    		// all other tags !!!
//    		GAME_EVENT
    		txtSplit = oldTags.toString().split("\n");
    		for(int i = 0; i < txtSplit.length; i++)
    	    {
    			if (txtSplit[i].contains("\b"))
    			{
    				String[] txtTags = txtSplit[i].split("\b");
    				if 	(		!txtTags[0].equals(GAME_EVENT)
    						&	!txtTags[0].equals(GAME_SITE)
    						&	!txtTags[0].equals(GAME_DATE)
    						&	!txtTags[0].equals(GAME_ROUND)
    						&	!txtTags[0].equals(GAME_WHITE)
    						&	!txtTags[0].equals(GAME_BLACK)
    						&	!txtTags[0].equals(GAME_RESULT)
    					)
    				{
      					newTags = newTags + txtSplit[i] + "\n";
    				}
    			}
    	    }
    		return newTags;
    	}
    	else
    	{
    		// if tag in oldTags: add old tag !!!
    		String newTag = "";
    		String[] tagName = tag.split("\b");
    		txtSplit = oldTags.toString().split("\n");
    		for(int i = 0; i < txtSplit.length; i++)
    	    {
    			if (txtSplit[i].contains("\b"))
    			{
//    				Log.i(TAG, "txtSplit[i]: " + txtSplit[i]);
    				String[] txtTags = txtSplit[i].split("\b");
//    				Log.i(TAG, "txtTags[0], tagName: " + txtTags[0] + ", " + tagName[0]);
    				boolean dateOk = true;
    				if (txtTags[0].equals(tagName[0]))
    				{
    					// validate Date !!!
    					if (txtTags[0].equals(GAME_DATE))
    					{
    						for(int h = 0; h < tagState.length; h++)
    						{
    							if 	(		txtTags[0].charAt(h) == '.'
    									|	txtTags[0].charAt(h) == '?'
    									|	Character.isDigit(txtTags[0].charAt(h))
    								)
    							{
    								
    							}
    							else
    							{
    								dateOk = false;
    								break;
    							}
    						}
    					}
    					if (dateOk)
    						newTag = txtSplit[i] + "\n";
    					break;
    				}
    			}
    	    }
    		if (!newTag.equals(""))
    			return newTags + newTag;
    		else
    			return newTags + tag;
    	}
    }
    public String getInitValueFromTag(String tag)	
    {
    	String initValue = "?";
    	for(int i = 0; i < tagState.length; i++)
	    {	// used for tagList (TAG_LIST_DIALOG)
			String[] txtTagStats = tagState[i].toString().split("\b");
			if (txtTagStats[0].equals(tag))
			{
				initValue = txtTagStats[1];
				break;
			}
	    }
    	return initValue;
    }
    public void setNewGameTags(String tags)	{gameTags = tags;}
    public void setGamePos(int posNumber) {gamePos = posNumber;}
    public void setIsGameEnd(boolean isEnd) {isGameEnd = isEnd;}
    public void setMoveIdx(int mvIdx) 
    {
    	if (mvIdx >= moveHistory.size())
    		mvIdx = moveHistory.size() -1;
//    	Log.i(TAG, "moveHistory.size(), mvIdx: " +moveHistory.size() + ", " + mvIdx);
    	moveIdx = mvIdx;
    }
    public void setMoveRank(CharSequence mRank) {moveRank = mRank;}
    public void setMoveVariant(CharSequence mVariant) {moveVariant = mVariant;}
    public void setMoveText(CharSequence moveText) 
    {
    	ChessMove cm = new ChessMove(moveHistory.get(getMoveIdx()));
    	CharSequence oldMoveText = cm.getTxt();
    	CharSequence[] txtSplit = oldMoveText.toString().split(" ");
//    	Log.i(TAG, "oldMoveText, moveText, txtSplit.length: " + oldMoveText + ", " + moveText + ", " + txtSplit.length);
    	if (oldMoveText.toString().startsWith("$"))
    	{
    		if (moveText.toString().startsWith("$"))
        	{
        		if (txtSplit.length > 1)
        		{
        			if (moveText.toString().equals("$0"))
        				moveText = oldMoveText.subSequence(txtSplit[0].length() +1, oldMoveText.length());
        			else
        				moveText = moveText.toString() + " " + oldMoveText.subSequence(txtSplit[0].length() +1, oldMoveText.length());
        		}
        		else
        		{
        			if (moveText.toString().equals("$0"))
        				moveText = "";
        		}
        	}
    		else
    			moveText = txtSplit[0] + " " + moveText.toString();
    	}
    	else
    	{
			if (moveText.toString().startsWith("$") & !moveText.toString().equals("$0"))
				moveText = moveText.toString() + " " + oldMoveText;
    	}
    	cm.setTxt(moveText.toString());
//    	Log.i(TAG, "new moveText: " + moveText);
    	moveHistory.set(getMoveIdx(), cm.getMoveHistory());
    }
    public void setNextMoveHistory(int keyState)
    {
    	isGameEnd = false;
    	moveIsFirst = false;
    	moveHasVariations = false;			
        moveIsFirstInVariation = false;		
        moveIsLastInVariation = false;	
        int idx = getMoveIdx();
//        Log.i(TAG, "MoveIdx, moveHistory, keyState: " + idx + ", " + moveHistory.size() + ", " + keyState);
        int lastIdx = 0;
        ChessMove cm;		// current move in moveHistory
        ChessMove cmPrev;	// previous move in moveHistory	
        ChessMove cmNext;	// next move in moveHistory
        cm = new ChessMove(moveHistory.get(getMoveIdx()));
    	idx = getMoveIdx();
        setMoveRank(cm.getRank());
        setMoveVariant(cm.getVariant());
         switch (keyState)
        {
        	case 0:     // current move
        	case 19:	// set move
        		for (int i = idx +1; i < moveHistory.size(); i++)
                {
                	cmNext = new ChessMove(moveHistory.get(i));
                	if (cm.getRank().equals(cmNext.getRank()) & cm.getVariant().equals(cmNext.getVariant()))
                	{
                		if (cmNext.getControl().equals("1"))
                			break;	
                		if (cmNext.getControl().equals("9"))
                		{
                			if (cmNext.getRank().equals("0"))
                    			isGameEnd = true;
                			break;
                		}
                	}
                }
	            break;
            case 1:     // LEFT_Button     	---> move back
                if (idx > 0)
                {
                    for (int i = idx -1; i >= 0; i--)
                    {
                    	cmPrev = new ChessMove(moveHistory.get(i));
                    	if (cm.getRank().equals(cmPrev.getRank()) & cm.getVariant().equals(cmPrev.getVariant()))
                    	{
                    		idx = i;
                    		break;
                    	}
                    }
                }
                break;
            case 2:     // RIGHT_Button    	---> next move
            	boolean isFirst = true;
                for (int i = idx +1; i < moveHistory.size(); i++)
                {
                	cmNext = new ChessMove(moveHistory.get(i));
                	if (cm.getRank().equals(cmNext.getRank()) & cm.getVariant().equals(cmNext.getVariant()))
                	{
                		if (cmNext.getControl().equals("1"))
                		{
                			if (isFirst)
                			{
                				idx = i;
                				isFirst = false;
                			}
                			else
                				break;
                		}
                		if (cmNext.getControl().equals("9"))
                		{
                			if (cmNext.getRank().equals("0"))
                    			isGameEnd = true;
                			break;
                		}
                	}
                 }
                break;
            case 3:     // START_Button    	---> start position
            	if (idx > 0)
                {
                    for (int i = idx -1; i >= 0; i--)
                    {
                    	cmPrev = new ChessMove(moveHistory.get(i));
                    	if (cm.getRank().equals(cmPrev.getRank()) & cmPrev.getControl().equals("0"))
                    	{
                    		idx = i;
                    		break;
                    	}
                    }
                }
                break;
            case 4:     // END_Button     	---> end position
            	for (int i = idx +1; i < moveHistory.size(); i++)
                {
                	cmNext = new ChessMove(moveHistory.get(i));
                	if (cm.getRank().equals(cmNext.getRank()) & cm.getVariant().equals(cmNext.getVariant()))
                	{
                		if (cmNext.getControl().equals("9"))
                		{
                			if (cmNext.getRank().equals("0"))
                    			isGameEnd = true;
                			break;
                		}
                		if (cmNext.getControl().equals("1"))
                			lastIdx = i;	
                	}
                }
            	idx = lastIdx;
                break;
            case 12:     // LEFT_Button     ---> two moves back(player vs engine)
            	if (cm.getRank().equals("0") & cm.getVariant().equals("0"))
            	{
	                if (idx > 1)
	                    idx = idx -2;
            	}
                break;
        }
        cm = new ChessMove(moveHistory.get(idx));
        if (!cm.getRank().equals("0"))
        {
        	if (cm.getControl().equals("0"))
        	{
        		idx++;
        		cm = new ChessMove(moveHistory.get(idx));
        		moveIsFirstInVariation = true;
        	}
        	else
        	{
	        	if (cm.getControl().equals("1") & idx > 0)
	        	{
	        		cmPrev = new ChessMove(moveHistory.get(idx -1));
	        		if (cmPrev.getControl().equals("0"))
	    	        	moveIsFirstInVariation = true;
	        	}
        	}
        }
        else
        {
        	if (idx == 0)
        		moveIsFirst = true;
        	else
        	{
        		if (cm.getControl().equals("9"))
    				moveIsLastInVariation = true;
        		if (idx +1 < moveHistory.size())
        		{
	        		cmNext = new ChessMove(moveHistory.get(idx +1));
	    			if (cmNext.getControl().equals("9"))
	    				moveIsLastInVariation = true;
        		}
        		if (isGameEnd)
        			moveIsLastInVariation = true;
        	}
        }
//        Log.i(TAG, "setNextMoveHistory, MoveIdx: " + idx);
        if (keyState == 3)
        {
	        if (idx == 0 | idx == 1)
	    		moveIsFirst = true;
        }
        setMoveIdx(idx);	
        if (!cm.getRank().equals("0"))
        {
	        if (cm.getControl().equals("0"))
	        	moveIsFirstInVariation = true;
	        if (cm.getControl().equals("9"))
	        	moveIsLastInVariation = true;
	        if (!cm.getControl().equals("9") & (idx +1) < moveHistory.size())
				moveIsLastInVariation = isLastMoveInVariation(idx);
        }
        if (cm.getControl().equals("1") & (idx +1) < moveHistory.size())
        {
        	cmNext = new ChessMove(moveHistory.get(idx +1));
        	if (cmNext.getControl().equals("0"))
        		moveHasVariations = true;
        }
        if (!isGameEnd)
        {
	        if (moveHistory.size() == 2)
	        	isGameEnd = true;	// only control: "0" + "9"
        }
        if (moveHistory.size() < 4)	// only one move: "0" + "1" + "9"
        	isGameEnd = true;	
        if (moveHistory.size() < 3)	// only "0" + "9"
        {	
       		moveIsFirstInVariation = true;	
        	moveIsLastInVariation = true;
        }
//        Log.i(TAG, "isGameEnd, moveIsFirstInVariation, moveIsLastInVariation: " + isGameEnd + ", " + moveIsFirstInVariation + ", " + moveIsLastInVariation);
    }
    public void setChess960Id(int id) {chess960Id = id;}
    public void setFigurineAlgebraicNotation(CharSequence requestList)
    {
    	if (requestList.length() == 5)
    	{
    		
    		HEX_K = requestList.charAt(0);
    		HEX_Q = requestList.charAt(1);
    		HEX_R = requestList.charAt(2);
    		HEX_B = requestList.charAt(3);
    		HEX_N = requestList.charAt(4);
//    		Log.i(TAG, "isDefined: >" + Character.isDefined(fan.charAt(0)) + "<");
//    		Log.i(TAG, "FAN: >" + HEX_K + HEX_Q + HEX_R + HEX_B + HEX_N);
    	}
    }
    public boolean isFenBasePosition(CharSequence fen)
    {
    	boolean isBase = true;
    	if (fen.equals(""))
    		isBase = false;
    	char[] fen64 = getFen64(fen);
    	if (fen64.length == 64)
		{
			for (int i = 0; i < fen64.length; i++)
		    {
				if (i < 8 | i > 55)
				{
				      if (fen64[i] == '-')
				    	  return false;
				}
				if (i > 7 & i < 16)
				{
				      if (fen64[i] != 'p')
				    	  return false;
				}
				if (i > 47 & i < 56)
				{
				      if (fen64[i] != 'P')
				    	  return false;
				}
				if (i > 15 & i < 48)
				{
				      if (fen64[i] != '-')
				    	  return false;
				}
		    }
		}
    	return isBase;
    }
    public char[] getFen64(CharSequence fen) 		
    {	// changing fen position to 64 characters(return)
//    	Log.i(TAG, "getFen64, fen: >" + fen);
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
		return sbFenCheck.toString().toCharArray();
    }
    public void setResultMessage(CharSequence resultMsg) {resultMessage = resultMsg;}
    // get methods		get methods		get methods		get methods		get methods
    public CharSequence getDateYYYYMMDD()
    {
    	sbDate.setLength(0);
        newDate = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(newDate);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH) + 1;
        int year = calendar.get(Calendar.YEAR);
        sbDate.append(year);
        sbDate.append(".");
        if (month < 10)
        {
            sbDate.append("0");
            sbDate.append(month);
            sbDate.append(".");
        }
        else
        {
            sbDate.append(month);
            sbDate.append(".");
        }
        if (day < 10)
        {
            sbDate.append("0");
            sbDate.append(day);
        }
        else
        	sbDate.append(day);
        return sbDate.toString();
    }
    public CharSequence getFigurineAlgebraicNotation()
    {
    	CharSequence fan = "" + HEX_K + HEX_Q + HEX_R + HEX_B + HEX_N;
    	return fan;
    }
    public CharSequence getFigurineAlgebraicNotation(CharSequence pgnMove, boolean figurineAlgebraicNotaion)
    {
    	CharSequence move = "";
    	if (figurineAlgebraicNotaion)
    	{
    		for (int i = 0; i < pgnMove.length(); i++)
            {
        		switch (pgnMove.charAt(i)) 
        		{
        			case 'K':	move = move.toString() + HEX_K; break;
        			case 'Q':	move = move.toString() + HEX_Q; break;
        			case 'R':	move = move.toString() + HEX_R; break;
        			case 'B':	move = move.toString() + HEX_B; break;
        			case 'N':	move = move.toString() + HEX_N; break;
        			default:	move = move.toString() + pgnMove.charAt(i); break;
        		}
            }
    	}
    	else
    		move = pgnMove;
     	return move;
    }
    public CharSequence getFileBase() {return fileBase;}
    public CharSequence getFilePath() {return filePath;}
    public CharSequence getFileName() {return fileName;}
    public char getGameColor() {return gameColor;}
    public CharSequence getPgnData() {return pgnData;}
    public CharSequence getGameData(CharSequence name, CharSequence data)
    {
//    	Log.i(TAG, "getGameData");
        char xx = '"';
        sbGameData.setLength(0);
        sbGameData.append("[");	
        sbGameData.append(name);
        sbGameData.append(" ");
        sbGameData.append(xx);
        sbGameData.append(data);
        sbGameData.append(xx);
        sbGameData.append("]\n");
        return sbGameData.toString();
    }
    public CharSequence getStartFen() 
    {
    	CharSequence startFen = getGameTagValue("FEN");
		if (startFen.equals(""))
			startFen = fenStandardPosition;
//		Log.i(TAG, "startFen: " + startFen);
		return startFen;
    }
    public CharSequence getGameNotation() {return gameNotation;}
    public int getGamePos() {return gamePos;}
    public CharSequence getGameText() {return gameText;}
    public boolean getIsGameEnd() {return isGameEnd;}
    public boolean getMoveIsFirst() {return moveIsFirst;}
    public boolean getIsMoveVariant() {return isMoveVariant;}
    public boolean getMoveHasVariations() {return moveHasVariations;}
    public boolean getMoveIsFirstInVariation() {return moveIsFirstInVariation;}
    public boolean getMoveIsLastInVariation() {return moveIsLastInVariation;}
    public CharSequence getMoveFen(int idx) 
    {	
    	try
    	{
	    	if (idx >= moveHistory.size())
	    		idx = moveHistory.size() -1;
	    	return chessMove.getVal(moveHistory.get(idx), 5);
    	}
    	catch (IndexOutOfBoundsException e) {e.printStackTrace(); return "";}
    }
    public CharSequence getNextMoveFen(int idx) 
    {
    	CharSequence fen = "";
    	if (idx < moveHistory.size() - 1)
    	{
    		fen = chessMove.getVal(moveHistory.get(idx +1), 5);
    	}
    	return fen;
    }
    public CharSequence getPreviousMoveFen(int idx) 
    {
    	return chessMove.getVal(moveHistory.get(idx), 10);
     }
    public int getMoveIdx() {return moveIdx;}
    public CharSequence getMoveRank() {return moveRank;}
    public CharSequence getMoveVariant() {return moveVariant;}
    public CharSequence getMoveInfo(int idx)	//is gameEnd?
    {
    	ChessMove cm = new ChessMove();
    	ChessMove cmNext = new ChessMove();
    	cm.setMoveFromHistory(moveHistory.get(idx));
    	if (idx < moveHistory.size() -1 )
    	{
    		cmNext.setMoveFromHistory(moveHistory.get(idx +1));
    		if (cmNext.getControl().equals("9") & cmNext.getRank().equals("0"))
    			isGameEnd = true;
    	}
        return "";
    }
    public CharSequence getResultMessage() {return resultMessage;}
    public CharSequence getMoveMessage(int idx)
    {
    	CharSequence msg = "";
        msg = createMessage(idx -2, msg, true);
        msg = createMessage(idx -1, msg, true);
        msg = createMessage(idx, msg, true);
        return msg;
    }
    public CharSequence createMessage(int idx, CharSequence msg, boolean figurineAlgebraicNotaion)
    {
        if (idx > 0)
        {
        	CharSequence fen = getMoveFen(idx);
            if (getValueFromFen(2, fen).equals("b"))
            	msg = msg + " " + getValueFromFen(6, fen) + "." + getFigurineAlgebraicNotation(getMovePgn(idx), figurineAlgebraicNotaion);
            else
           		msg = msg + " " + getFigurineAlgebraicNotation(getMovePgn(idx), figurineAlgebraicNotaion);
        }
        return msg;
    }
    public CharSequence getMove(int idx) 
    {
    	return chessMove.getVal(moveHistory.get(idx), 3);
    }
    public CharSequence getMovePgn(int idx) 
    {
    	return chessMove.getVal(moveHistory.get(idx), 4);
    }
    public CharSequence getMoveTxt(int idx) 
    {
    	CharSequence moveText = chessMove.getVal(moveHistory.get(idx), 6);
        if (moveText.toString().startsWith("$"))
        {
        	if (moveText.toString().contains(" "))
        	{
        		CharSequence[] txtSplit = moveText.toString().split(" ");
        		if (txtSplit.length > 0)
            		moveText = moveText.subSequence(txtSplit[0].length() +1, moveText.length());
         	}
        	else
        		moveText = "";
        }
        if (moveText.toString().contains("||"))
        	moveText = moveText.toString().replace("||", "\n");
    	return moveText;
    }
    public ArrayList<CharSequence> getVariationsFromMoveHistory()
    {
//    	Log.i(TAG, "getVariationsFromMoveHistory");
//    	printMoveHistory();
    	ArrayList<CharSequence> variList = new ArrayList<CharSequence>();
    	StringBuilder sb = new StringBuilder(30);
    	boolean nextIsVariationStart = true;
    	int idx = getMoveIdx();
    	ChessMove cm = new ChessMove();
    	ChessMove cmPrev = new ChessMove();
    	cm.setMoveFromHistory(moveHistory.get(idx));
    	if (cm.getControl().equals("1"))
    	{
    		cmPrev.setMoveFromHistory(moveHistory.get(idx -1));
    		if (cmPrev.getControl().equals("0"))
	    	{
    			if (idx > 1)
    			{
		    		idx = getBaseMoveIdxFromVariation(idx -1);
		    		cmPrev.setMoveFromHistory(moveHistory.get(idx -1));
    			}
 	    		cm.setMoveFromHistory(moveHistory.get(idx));
	    	}
    		else
    		{
    			if (cmPrev.getControl().equals("1"))
					cmPrev.setMoveFromHistory(moveHistory.get(idx -1));
    		}
    	}
//    	Log.i(TAG, "CRV, pgn, idx: " + cm.getControl() + cm.getRank() + cm.getVariant() + " " + cm.getPgn() + " " + idx);
    	CharSequence color = "";
    	CharSequence number = "";
    	CharSequence movePgn = "";
		color = cm.getColorFromFen(cm.getBaseFen());
		number = cm.getMoveNumberFromFen(cm.getBaseFen());
		movePgn = "";
		if (color.equals("l"))
		{
			movePgn = number + ". " + cm.getPgn() + "   <<<";
			if (cmPrev != null)
				movePgn = cmPrev.getMoveNumberFromFen(cmPrev.getBaseFen()) + "... " + cmPrev.getPgn() + " " 
						+ number + ". " + cm.getPgn() + "   <<<";
		}
		else
		{
			movePgn = number + "... " + cm.getPgn() + "   <<<";
			if (cmPrev != null)
				movePgn = number + ". " + cmPrev.getPgn() + " " + cm.getPgn() + "   <<<";
		}
		sb.setLength(0);
		sb.append(Integer.toString(idx)); 	sb.append("\b");
		sb.append(cm.getPgn()); 			sb.append("\b");
		sb.append(color); 					sb.append("\b");
		sb.append(number); 					sb.append("\b");
		sb.append(movePgn); 				sb.append("\b");
		sb.append(cm.getRank()); 			sb.append("\b");
		sb.append(cm.getVariant()); 		sb.append("\b");
    	variList.add(sb.toString());
    	if (cm.getVal(moveHistory.get(idx), 12).equals("1"))	// moveControl
    	{
    		for (int i = idx +1; i < moveHistory.size(); i++)
	        {
    			if (nextIsVariationStart)
    			{
    				if (!cm.getVal(moveHistory.get(i), 12).equals("0"))
    					break;
    				else
    				{
	    				nextIsVariationStart = false;
	    				cm.setMoveFromHistory(moveHistory.get(i +1));
	    				color = cm.getColorFromFen(cm.getBaseFen());
	    				number = cm.getMoveNumberFromFen(cm.getBaseFen());
	    				movePgn = "";
	    				if (color.equals("l"))
	    					movePgn = number + ". " + cm.getPgn();
	    				else
	    					movePgn = number + "... " + cm.getPgn();
	    				sb.setLength(0);
	    				sb.append(Integer.toString(i +1)); 	sb.append("\b");
	    				sb.append(cm.getPgn()); 			sb.append("\b");
	    				sb.append(color); 					sb.append("\b");
	    				sb.append(number); 					sb.append("\b");
	    				sb.append(movePgn); 				sb.append("\b");
	    				sb.append(cm.getRank()); 			sb.append("\b");
	    				sb.append(cm.getVariant()); 		sb.append("\b");
	    		    	variList.add(sb.toString());
    				}
    			}
    			else
    			{
    				if (cm.getVal(moveHistory.get(i), 1).equals(cm.getRank()) 
    						& cm.getVal(moveHistory.get(i), 2).equals(cm.getVariant()))
    				{
    					if (cm.getVal(moveHistory.get(i), 12).equals("9"))
    						nextIsVariationStart = true;
    				}
    			}
	        }
    	}
    	return variList;
    }
    public int getCountEvenPosition(CharSequence fen)
    {
        int cntFen = 0;
        try 
		{
        	ChessMove cm = new ChessMove();
	        int idxSpace = fen.toString().indexOf(" ");
	        CharSequence compareFen = fen.subSequence(0, idxSpace + 2);
	        CharSequence rank = cm.getVal(moveHistory.get(getMoveIdx()), 1);
	        CharSequence variant = cm.getVal(moveHistory.get(getMoveIdx()), 2);
	        for (int i = getMoveIdx(); i > 0; i--)
	        {
	        	CharSequence mControl = cm.getVal(moveHistory.get(i), 12);
	        	CharSequence mRank = cm.getVal(moveHistory.get(i), 1);
	        	CharSequence mVariant = cm.getVal(moveHistory.get(i), 2);
	        	CharSequence mFen = cm.getVal(moveHistory.get(i), 5);
            	if (mRank.equals(rank) & mVariant.equals(variant))
            	{
	            	if (mControl.equals("1"))	
	            	{
		                idxSpace = mFen.toString().indexOf(" ");
		                CharSequence compareFen2 = mFen.subSequence(0, idxSpace + 2);
		                if (compareFen.equals(compareFen2))
		                {
		                    cntFen++;
//Log.i(TAG, "idx, pgn, cntFen: " + i + ", " + cm.getVal(moveHistory.get(i), 4) + ", " + cntFen);
//Log.i(TAG, "compareFen : " + i + ", " + compareFen);
//Log.i(TAG, "compareFen2: " + i + ", " + compareFen2);
		                }
	            	}
            	}
            	else
            	{
            		if (!mRank.equals("0") & mVariant.equals("0"))
            			break;
            	}
	        }
		}
        catch (IndexOutOfBoundsException e)
        {
        	cntFen = 0;
//        	Log.i(TAG, "fen: " + fen);
        }
        return cntFen;
    }
    
    public void parseGameNotation(CharSequence gameNotation)	// !?!
    {	// parse: PGN move section
//    	Log.i(TAG, "GameNotation:\n" + gameNotation);
    	parseInit(gameNotation);	// create: CharSequence[] pTokens
    	parseAddToMoveList(moveList.size());		// start variation: R0, V0 	(pMoveControl = 0)
		for(int i = 0; i < pTokens.length; i++)
        {
//			Log.i(TAG, "pTokens[i]: " + ">" + pTokens[i] + "<");
//			if (pTokens[i].equals("0-0") | pTokens[i].equals("0-0-0"))
			if (pTokens[i].contains("0-0"))
				pTokens[i] = pTokens[i].replace("0", "O");
			parseToken(pTokens[i]);
			if (pIsEnd & !pErrorMessage.toString().equals(""))
			{
				pErrorMessage = stringValues.get(23) + " [P7]\n" + pErrorMessage; // cl_notationError
				gameText = pErrorMessage;
//				Log.i(TAG, "pErrorMessage, pIsEnd, pTokens.length: " + pErrorMessage + ", " + pIsEnd + ", " + pTokens.length);
				break;
			}
        }
		if (pCountVariationStart != pCountVariationEnd)
		{
//			Log.i(TAG, "pCountVariationStart, pCountVariationEnd: " + pCountVariationStart + ", " + pCountVariationEnd);
			if (pErrorMessage.toString().equals(""))
			{
				pErrorMessage = stringValues.get(23) + " [P8]\n" + stringValues.get(24);	// cl_notationError, cl_variationError
				gameText = pErrorMessage;
//				Log.i(TAG, "pErrorMessage(diff var): " + pErrorMessage);
			}
		}
		if (pMoveR == 0 & pMoveV == 0)
			parseSetVariationEndToMoveList();		// end variation: R0, V0 	(pMoveControl = 9)
		else
		{
			if (pErrorMessage.toString().equals(""))
			{
				pErrorMessage = stringValues.get(23) + " [P9]\n" + stringValues.get(24);
				gameText = pErrorMessage;
//				Log.i(TAG, "pErrorMessage(end error): " + pErrorMessage);
			}
		}
    }
    public void parseToken(CharSequence tok)	// parsing a token
    {
    	CharSequence token = tok;
//    	Log.i(TAG, "token, length: >" + token + "< " + token.length());
    	pMoveControl = -1;											// unknown error
		if 	(		token.toString().equals(GAME_RESULT_NONE) 		// *	
				| 	token.toString().equals(GAME_RESULT_WHITE)		// 1-0 	
				| 	token.toString().equals(GAME_RESULT_BLACK) 		// 0-1
				| 	token.toString().equals(GAME_RESULT_DRAW1)		// 1/2-1/2
				| 	token.toString().equals(GAME_RESULT_DRAW2)		// �-�
			)
    	{
    		pMoveControl = 99;
    		isGameEnd = true;
			pIsEnd = true;
			pErrorMessage = "";
    	}
    	else
    	{
    		if (pIsComment)									// comment: { ... }
    		{
    			pMoveControl = 98;
    			if (!token.toString().contains("}"))
    				pMoveComment = pMoveComment.toString() + " " + token;
    			else
    			{
//    				Log.i(TAG, "1. token with }: >" + token + "<");
    				if (token.toString().endsWith("}") | token.toString().endsWith(")"))
    				{
    					int min = 0;
    					if (token.toString().endsWith(")"))
    					{
							for (int i = 0; i < token.length(); i++)
						    {
								if (token.charAt(i) == '}')
									min = 0;
						        if (token.charAt(i) == ')')
						        	min++;
 						    }
    					}
    					pMoveComment = pMoveComment.toString() + " " + token.subSequence(0, token.length() - (min +1));
//    					Log.i(TAG, "1. token with }: >" + token + "< >" + pMoveComment + "<");
    					if (!pMoveComment.toString().equals(""))
    						parseSetCommentToMoveList(moveList.size() -1, pMoveComment);
    					for (int i = 0; i < min; i++)
					    {
							pCountVariationEnd++;
							parseSetVariationEndToMoveList();	// end variation 	(pMoveControl = 9)
						}
    				}
    				else
    				{
    					pErrorMessage = token;				// PGN-error
    					pIsEnd = true;
    				}
    				pIsComment = false;
    			}
//    			Log.i(TAG, "pMoveComment: " + pMoveComment);
    		}
    		else											//
    		{
    			boolean isVariationEnd = false;
				boolean isParseMoveError = false;
				int cntVarEnd = 0;
    			parseInitMove();
    			if (token.toString().startsWith("{") | token.toString().startsWith("$") | token.toString().startsWith("({"))	// start comment: { ... } or $12 (NAG)
				{
    				pMoveControl = 98;
    				
    				if (token.toString().startsWith("({"))	// start variation + comment
    				{
    					pMoveControl = 0;
    					pCountVariationStart++;
    					if (!pIsVariationEnd)
        				{
        					pMoveR++;
        					if (pMoveR > cntRank)
        						cntRank = pMoveR;
        					pMoveV = 1;
        				}
        				else
        				{
        					pIsVariationEnd = false;
        					pMoveR = pLastMoveR;
        					pMoveV = pLastMoveV +1;
        				}
    					parseAddToMoveList(moveList.size());		// start variation 	(pMoveControl = 0)
    					token = token.toString().substring(1);
    				}
    				if (token.toString().startsWith("$"))
    				{
    					if (token.toString().endsWith(")"))
    					{
    						cntVarEnd = 0;
							for (int i = 0; i < token.length(); i++)
						    {
						        if (token.charAt(i) == ')')
						        	cntVarEnd++;
 						    }
    						token = token.subSequence(0, token.length() -cntVarEnd);
    						pMoveComment = token;
            				parseSetCommentToMoveList(moveList.size() -1, pMoveComment);
            				pIsComment = false;
    						for (int i = 0; i < cntVarEnd; i++)
						    {
    							pCountVariationEnd++;
    							parseSetVariationEndToMoveList();	// end variation 	(pMoveControl = 9)
 						    }
    						if (isParseMoveError)
	    					{
		    					pErrorMessage = token;					// PGN-error
		    					pIsEnd = true;
		    				}
    					}
    					else
    					{
	        				pMoveComment = token;
	        				parseSetCommentToMoveList(moveList.size() -1, pMoveComment);
	        				pIsComment = false;
    					}
    				}
    				else
    				{
    					CharSequence tmpToken = token.toString().substring(1);
	    				if (!tmpToken.toString().contains("}"))
	    				{
	    					pMoveComment = tmpToken;
	    					pIsComment = true;
	    					pMoveControl = 98;
	    				}
	    				else
	    				{
//	    					Log.i(TAG, "2. token with }: >" + token + "<");
	    					if (tmpToken.toString().endsWith("}"))
	        				{
	    						pMoveComment = tmpToken.subSequence(0, tmpToken.length() -1);
//	    						Log.i(TAG, "2. token with }: >" + tmpToken + "< >" + pMoveComment + "<");
	    						parseSetCommentToMoveList(moveList.size() -1, pMoveComment);
	        					pIsComment = false;
	        				}
	    					else
	    					{
	    						if (tmpToken.toString().endsWith(")"))
	        					{
	        						cntVarEnd = 0;
	    							for (int i = 0; i < tmpToken.length(); i++)
	    						    {
	    								if (tmpToken.charAt(i) == '}')
	    									cntVarEnd = 0;
	    						        if (tmpToken.charAt(i) == ')')
	    						        	cntVarEnd++;
	     						    }
	    							tmpToken = tmpToken.subSequence(0, tmpToken.length() - (cntVarEnd +1));
	        						pMoveComment = tmpToken;
//	        						Log.i(TAG, "3. token with }: >" + tmpToken + "< >" + pMoveComment + "<");
	                				parseSetCommentToMoveList(moveList.size() -1, pMoveComment);
	                				pIsComment = false;
	        						for (int i = 0; i < cntVarEnd; i++)
	    						    {
	        							pCountVariationEnd++;
	        							parseSetVariationEndToMoveList();	// end variation 	(pMoveControl = 9)
	     						    }
	        						if (isParseMoveError)
	    	    					{
	    		    					pErrorMessage = tmpToken;					// PGN-error
	    		    					pIsEnd = true;
	    		    				}
	        					}
	    					}
	    				}
    				}
				}
    			else
    			{
    				// parse move section
    				if (!token.equals(""))
    				{
//    					Log.i(TAG, "start parse move section: " + token);
    					if (token.toString().contains("!"))
    						token = token.toString().replace("!", "");
    					if (token.toString().contains("?"))
    						token = token.toString().replace("?", "");
    					if (token.toString().startsWith("("))		// start variation
	    				{
	    					pMoveControl = 0;
	    					pCountVariationStart++;
	    					if (!pIsVariationEnd)
	        				{
	        					pMoveR++;
	        					if (pMoveR > cntRank)
	        						cntRank = pMoveR;
	        					pMoveV = 1;
	        				}
	        				else
	        				{
	        					pIsVariationEnd = false;
	        					pMoveR = pLastMoveR;
	        					pMoveV = pLastMoveV +1;
	        				}
	    					parseAddToMoveList(moveList.size());		// start variation 	(pMoveControl = 0)
	    					if (!token.toString().endsWith(")"))
	        					token = token.toString().substring(1);
	    					else
	    					{
	    						token = token.subSequence(1, token.length() -1);
	    						isVariationEnd = true;
	    					}
	    					if (parseIsDigit(token.toString()))
	    						token = parseGetMoveFromDigitToken(token);
	    					if (!token.equals(""))
	    						isParseMoveError = parseMove(token);	// move to moveList (pMoveControl = 1)
	    					if (isVariationEnd)
	    						parseSetVariationEndToMoveList();		// end variation 	(pMoveControl = 9)
	    					if (isParseMoveError)
	    					{
		    					pErrorMessage = token;					// PGN-error
		    					pIsEnd = true;
		    				}
	    				}
    					else		// no start variation
    					{
    						if (token.toString().endsWith(")"))
	    					{
    							cntVarEnd = 0;
    							for (int i = 0; i < token.length(); i++)
    						    {
    						        if (token.charAt(i) == ')')
    						        	cntVarEnd++;
     						    }
	    						token = token.subSequence(0, token.length() -cntVarEnd);
	    						isVariationEnd = true;
	    					}
    						if (parseIsDigit(token))
	    						token = parseGetMoveFromDigitToken(token);
    						if (!token.equals(""))
	    						isParseMoveError = parseMove(token);	// move to moveList (pMoveControl = 1)
    						if (isVariationEnd)
    						{
	    						for (int i = 0; i < cntVarEnd; i++)
    						    {
	    							pCountVariationEnd++;
	    							parseSetVariationEndToMoveList();	// end variation 	(pMoveControl = 9)
     						    }
    						}
    						if (isParseMoveError)
	    					{
		    					pErrorMessage = token;					// PGN-error
		    					pIsEnd = true;
		    				}
    					}
    				}
    			}
    		}
    	}
    	if (!token.equals("") & pMoveControl == -1)
    	{
    		if (pErrorMessage.toString().equals(""))
    			pErrorMessage = "unknown error: " + token;
			pIsEnd = true;
		}
    }
    public CharSequence parseGetNextToken(int idx)	
    {
    	CharSequence token = "";
    	if (idx < pTokens.length)
    		token = pTokens[idx].toString();
    	return token;
    }
    public void parseAddToMoveList(int idx)	
    {
//    	Log.i(TAG, "parseAddToMoveList, idx, pMoveControl: " + idx + ", " + pMoveControl);
    	if (pMoveControl >= 0 & pMoveControl < 10)
    	{
			sbMoveValues.setLength(0);
			sbMoveValues.append(pMoveControl);		sbMoveValues.append("\b");
			sbMoveValues.append(pMoveR);			sbMoveValues.append("\b");
			sbMoveValues.append(pMoveV);			sbMoveValues.append("\b");
			sbMoveValues.append(pMovePgn);			sbMoveValues.append("\b");
			if (pMoveControl == 1)
				sbMoveValues.append(pMoveColor);	sbMoveValues.append("\b");
			sbMoveValues.append(pMoveComment);		sbMoveValues.append("\b");
			moveList.add(idx, sbMoveValues.toString());
//			Log.i(TAG, "idx, move: " + idx + ", " + moveList.get(idx));
			if (pMoveControl == 1 & !pMovePgn.toString().equals(""))
				parseSetNextColor();
    	}
    }
    public void parseSetCommentToMoveList(int idx, CharSequence comment)	// commentToken (update "moveList")
    {
//    	Log.i(TAG, "parseSetCommentToMoveList, idx, comment: " + idx + ", " + comment);
    	CharSequence moveTmp = moveList.get(idx).toString();
    	if (chessMove.getVal(moveTmp, 6).toString().startsWith("$"))
    		comment = chessMove.getVal(moveTmp, 6).toString() + " " + comment;
		sbMoveValues.setLength(0);
		sbMoveValues.append(chessMove.getVal(moveTmp, 1));	sbMoveValues.append("\b");
		sbMoveValues.append(chessMove.getVal(moveTmp, 2));	sbMoveValues.append("\b");
		sbMoveValues.append(chessMove.getVal(moveTmp, 3));	sbMoveValues.append("\b");
		sbMoveValues.append(chessMove.getVal(moveTmp, 4));	sbMoveValues.append("\b");
		sbMoveValues.append(chessMove.getVal(moveTmp, 5));	sbMoveValues.append("\b");
		sbMoveValues.append(comment);						sbMoveValues.append("\b");
		moveList.set(idx, sbMoveValues.toString());
    }
    public void parseSetNextColor()	
    {
    	if (pMoveColor.toString().equals("l"))
			pMoveColor = "d";
		else
			pMoveColor = "l";
    }
    public void parseInit(CharSequence notation)	// initialize parsing
    {
    	moveList.clear();
    	pIsEnd = false;
    	pIsVariationEnd = false;
    	pCountVariationStart = 0;
    	pCountVariationEnd = 0;
    	pMoveControl = 0;
    	cntRank = 0;
    	pMoveR = 0;
        pMoveV = 0;
        pLastMoveR = 0;
	    pLastMoveV = 0;
        parseInitMove();
        pMoveColor = "l";
        notation = notation.toString().replace('\n', ' ');
        notation = notation.toString().replace('\r', ' ');
        notation = notation.toString().replace('\t', ' ');
        pTokens = notation.toString().split("\\ ");
        if (pTokens == null)
        	pIsEnd = true;
        else
        {
        	if (pTokens.length < 1)
        		pIsEnd = true;
        }
//        Log.i(TAG, "pIsEnd, pTokens.length: " + pIsEnd + ", " + pTokens.length);
    }
    public void parseInitMove()	// initialize parsing move
    {
        pIsComment = false;
        pMovePgn = "";
        pMoveComment = "";
        pMoveColor = "l";
        pErrorMessage = "";
    }
    public void parseSetVariationEndToMoveList()	
    {
    	pMoveControl = 9;
		pIsVariationEnd = true;
		pLastMoveR = pMoveR;
		pMoveV = parseGetLastVariantFromRank(pMoveR);
		pLastMoveV = pMoveV;
		pMovePgn = "";
        pMoveComment = "";
		parseAddToMoveList(moveList.size());	// end variation 	(pMoveControl = 9)
		if (pMoveR > 0)
		{
			pMoveR--;
			if (pMoveR == 0)
				pMoveV = 0;
			else
			{
				pMoveV = parseGetLastVariantFromRank(pMoveR);
				pLastMoveV = pMoveV;
			}
		}
    }
    public int parseGetLastVariantFromRank(int rank)	
    {
//    	Log.i(TAG, "rank, moveList.size(): " + rank + ", " + moveList.size());
    	if (moveList.size() > 0)
    	{
	    	for(int i = moveList.size() -1; i > 0; i--)
	        {
//	    		Log.i(TAG, "R, V: " + i + ", " + chessMove.getRank() + ", " + chessMove.getVariant());
	    		chessMove.setMoveFromMoveList(moveList.get(i).toString());
	    		if (chessMove.getRank().equals(Integer.toString(rank)))
	    			return Integer.parseInt((String) chessMove.getVariant());
	        }
    	}
    	return 0;
    }
    public boolean parseMove(CharSequence checkMove)	
    {
    	boolean isMoveError = true;
    	if (checkMove.length() > 0)
    	{
	    	switch (checkMove.charAt(0)) // token, 1. character
			{
				case 'K': case 'Q': case 'R': case 'B': case 'N': case 'O':
				case 'a': case 'b': case 'c': case 'd': case 'e': case 'f': case 'g': case 'h':
					isMoveError = false;
					break;
			}
    	}
    	if (!isMoveError)
    	{
    		boolean charOk = true;
	    	for(int i = 1; i < checkMove.length(); i++)
	        {
	    		switch (checkMove.charAt(i)) // check possible character
				{
					case 'Q': case 'R': case 'B': case 'N': case 'O':
					case 'a': case 'b': case 'c': case 'd': case 'e': case 'f': case 'g': case 'h':
					case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8':
					case 'x': case '-': case '+': case '#': case '=': 
						break;
					default:
						charOk = false;
						break;
				}
	        }
	    	if (!charOk)
	    		isMoveError = true;
    	}
    	if (!isMoveError)
    	{
			pMoveControl = 1;	// move
			pMovePgn = checkMove;
			parseAddToMoveList(moveList.size());	// start variation (pMoveControl = 1)
    	}
//    	Log.i(TAG, "parseMove: " + checkMove + ", " + isMoveError);
    	return isMoveError;
    }
    public boolean parseIsDigit(CharSequence checkMove) // check move number	
    {
    	boolean isDigit = false;
    	if (checkMove.length() > 0)
    	{
	    	switch (checkMove.charAt(0)) // token, 1. character
			{
				case '1': case '2': case '3': case '4': case '5': case '6': case '7': case '8': case '9': case '.':
					isDigit = true;
					break;
			}
    	}
    	return isDigit;
    }
    public CharSequence parseGetMoveFromDigitToken(CharSequence digitToken) // move after digit(move number)?	
    {
    	CharSequence mv = "";
//    	Log.i(TAG, "parseIsDigit: " + digitToken);
		pMoveColor = "l";
		if (digitToken.toString().contains(".."))
			pMoveColor = "d";
		if (digitToken.toString().endsWith("."))
		{
			pMoveControl = 97;
			mv = "";
		}
		else
		{
			CharSequence tmp = "";
			for (int i = digitToken.length() -1; i > 0; i--)
	        {
				if (digitToken.charAt(i) != '.')
					tmp = digitToken.charAt(i) + tmp.toString();
				else
					break;
//				Log.i(TAG, "tmp: " + tmp);
	        }
			mv = tmp;
		}
//		Log.i(TAG, "parseMove: " + mv);
    	return mv;
    }
    public CharSequence getValueFromFen(int value, CharSequence fen)
    {
    	CharSequence txt = "";
	    int cnt = 1;
	    for (int i = 0; i < fen.length(); i++)
	    {
	        if (fen.charAt(i) == ' ')
	            cnt++;
	        else
	        {
	            if (cnt == value)
	                txt = txt.toString() + fen.charAt(i);
	        }
	    }
	    return txt;
    }
    public CharSequence getRkrFromFen(CharSequence fen)
    {
//    	Log.i(TAG, "getRkrFromFen, fen: " + fen);
    	CharSequence txtRkr = "";
	    final CharSequence field[] = {"a", "b", "c", "d", "e", "f", "g", "h"};
	    if (!fen.equals(""))
	    {
		    for (int i = 0; i < 8; i++)
		    {
		        if (fen.charAt(i) == 'r')
		        {
		        	if (txtRkr == "")
		        		txtRkr = "" + field[i];
		        	else
		        		txtRkr = txtRkr.toString() + field[i];
		        }
		        if (fen.charAt(i) == 'k')
		        	txtRkr = txtRkr.toString() + field[i];
		    }
	    }
	    return txtRkr;
    }
    public int getChess960Id() {return chess960Id;}

    final String TAG = "ChessHistory";
    ArrayList<CharSequence> stringValues;
	ChessMove chessMove = new ChessMove();
	public CharSequence pgnData;
	public String gameTags;
	public final String GAME_EVENT = 	"Event";
	public final String GAME_SITE = 	"Site";
	public final String GAME_DATE = 	"Date";
	public final String GAME_ROUND = 	"Round";
	public final String GAME_WHITE = 	"White";
	public final String GAME_BLACK = 	"Black";
	public final String GAME_RESULT = 	"Result";
	public final String GAME_TAG_EVENT = 	"Event\b?\n";
	public final String GAME_TAG_SITE = 	"Site\b?\n";
	public final String GAME_TAG_DATE = 	"Date\b????.??.??\n";
	public final String GAME_TAG_ROUND = 	"Round\b-\n";
	public final String GAME_TAG_WHITE = 	"White\b?\n";
	public final String GAME_TAG_BLACK = 	"Black\b?\n";
	public final String GAME_TAG_RESULT = 	"Result\b*\n";
	public final CharSequence tagState[] =
	    {	// . . . EventType [twic]	
			"Event\b?\b0\b0", 
			"Site\b?\b0\b0", 
			"Date\b????.??.??\b2\b10", 
			"Round\b-\b0\b0", 
			"White\b?\b0\b0", 
			"Black\b?\b0\b0", 
			"Result\b*\b9\b0",
			"WhiteTitle\b-\b0\b0", 
			"BlackTitle\b-\b0\b0", 
			"WhiteElo\b-\b1\b4", 
			"BlackElo\b-\b1\b4",
			"NIC\b?\b0\b0",
			"ECO\b?\b0\b6", 
			"Opening\b?\b0\b0", 
			"Variation\b?\b0\b0", 
			"SubVariation\b?\b0\b0",
			"EventDate\b????.??.??\b2\b10", 
			"WhiteFideId\b-\b1\b8", 
			"BlackFideId\b-\b1\b8", 
			"WhiteTeam\b?\b0\b0", 
			"BlackTeam\b?\b0\b0",
			"EventType\b?\b0\b0", 
			"WhiteUSCF\b?\b1\b4", 
			"BlackUSCF\b?\b1\b4", 
			"WhiteNA\b?\b0\b0", 
			"BlackNA\b?\b0\b0", 
			"WhiteType\b?\b0\b0", 
			"BlackType\b?\b0\b0",
			"EventSponsor\b?\b0\b0", 
			"Section\b?\b0\b0", 
			"Stage\b?\b0\b0", 
			"Board\b?\b1\b4", 
			"Time\b??:??:??\b3\b8", 
			"UTCTime\b??:??:??\b3\b8",
			"WhiteClock\b??:??:??\b3\b8",
			"BlackClock\b??:??:??\b3\b8",
			"Clock\bW/??:??:??\b4\b10",
			"UTCDate\b????.??.??\b2\b10", 
			"TimeControl\b?\b0\b0", 
			"Termination\bunterminated\b0\b0", 
			"Annotator\b?\b0\b0", 
			"Mode\b?\b0\b0", 
			"PlyCount\b0\b1\b3",
			"Variant\b?\b0\b0" 
		};
// PGN-Reference - Movetext section
	public CharSequence gameNotation;
    public int chess960Id = 518;	// 0 ... 959, 518 = standard
// parse PGN - Move Section
    ArrayList<CharSequence> moveList;	// parse result
    StringBuilder sbPgn = new StringBuilder(10000);
    StringBuilder sbData = new StringBuilder(2000);
    StringBuilder sbNotation = new StringBuilder(8000);
    StringBuilder sbGameNotation = new StringBuilder(8000);
    StringBuilder sbFenCheck = new StringBuilder(200);
    StringBuilder sbDate = new StringBuilder(10);
    StringBuilder sbMoveValues = new StringBuilder(50);
    StringBuilder sbGameData = new StringBuilder(200);
    
    String[] pTokens;
    private final String GAME_RESULT_NONE = "*";
    private final String GAME_RESULT_WHITE = "1-0";
    private final String GAME_RESULT_BLACK = "0-1";
    private final String GAME_RESULT_DRAW1 = "1/2-1/2";
    private final String GAME_RESULT_DRAW2 = "\\u00BD-\\u00BD";
    boolean pIsComment = false;
    boolean pIsEnd = false;
    boolean pIsVariationEnd = false;
    int pCountVariationStart = 0;
    int pCountVariationEnd = 0;
    int pMoveControl = 0;
    int pMoveR = 0;	// moveRank
    int pMoveV = 0;	// moveVariant
    int pLastMoveR = 0;
    int pLastMoveV = 0;
    CharSequence pMovePgn = "";
    CharSequence pMoveComment = "";
    CharSequence pMoveColor = "l";
    public CharSequence pErrorMessage = "";
// moveHistory - control
    public ArrayList<CharSequence> moveHistory;
    ArrayList<CharSequence> moveHistoryCopy;
    int moveIdx;								// index of moveHistory
// variation control
    int cntRank = 0;	// moveRank
    CharSequence moveRank = "";						// variation control (rank)
    CharSequence moveVariant = "";					// variation control (variant)
    boolean moveIsFirst = false;				// is first move in gameNotation!
    boolean moveHasVariations = false;			// has the current move variations?
    boolean moveIsFirstInVariation = false;		// first move in variation?
    boolean moveIsLastInVariation = false;		// last move in variation?
    CharSequence pgnIndent = " ";						// PGN-output, indent for variation
    
    boolean isNewBranche = false;
    boolean hasVariations = false;
    boolean variationExist = false;
    int newBrancheMoveIdx = 0;
    CharSequence newBrancheR = "1";
    CharSequence newBrancheV = "1";
    CharSequence newBrancheFen = "";
    
    boolean isGameEnd;
    boolean isMoveVariant = false;
    CharSequence fileBase;
    CharSequence filePath;
    CharSequence fileName;
    CharSequence gameText;
    CharSequence resultMessage;	
    int gamePos = -1;
    char gameColor = 'l';
    public final CharSequence fenStandardPosition = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
    private static char HEX_K = ' ';
	private static char HEX_Q = ' ';
	private static char HEX_R = ' ';
	private static char HEX_B = ' ';
	private static char HEX_N = ' ';
    Date newDate;
}
