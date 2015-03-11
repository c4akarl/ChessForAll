package ccc.chess.gui.chessforall;

import java.util.ArrayList;
import java.util.Random;

import android.content.ComponentName;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
//import android.util.Log;

public class ChessEngine implements ServiceConnection
{
    public ChessEngine(C4aMain cM, int eNumber) 
	{
    	c4aM = cM;	// main thread for UI-actions !
    	engineNumber = eNumber;
    	processAlive = false;
    	isReady = false;
	}
	private void readUCIOptions() 
	{
    	int timeout = 1000;
    	while (true) 
    	{
    		CharSequence s = readLineFromProcess(timeout);	//>425 getting a message from ChessEngine
    		CharSequence[] tokens = tokenize(s);
    		if (tokens[0].equals("uciok"))
    		{
    			break;
    		}
    	}
    	CharSequence name = getInfoFromEngineService("ENGINE_NAME");
    	if (!name.equals(""))
    		engineName = name;
    	engineNameStrength = getInfoFromEngineService("GET_ENGINE_NAME_STRENGTH");
    	if (engineNameStrength.equals(""))
    		engineNameStrength = name;
    }
	public CharSequence[] tokenize(CharSequence cmdLine) 
    {
        cmdLine = cmdLine.toString().trim();
        return cmdLine.toString().split("\\s+");
    }
	public boolean syncReady() 
    {
		isReady = false;
    	writeLineToProcess("isready");	//>426 posting a message to the ChessEngine
    	while (true) 
    	{
    		CharSequence s = readLineFromProcess(1000);
    		if (s.equals("readyok"))
    		{
    			isReady = true;
    			break;
    		}
    	}
    	return isReady;
    }
	public boolean searchIsReady() 
    {	
		int cnt = 0;
		writeLineToProcess("isready");	
    	while (cnt < 200) 
    	{
    		CharSequence s = readLineFromProcess(1000);
    		if (s.equals("readyok"))
    			return true;
    		else
				cnt++;
     	}
		return false;
    }
	public boolean getIsProcessAlive() {return processAlive;}	//>443 true if Native Engine Process is alive
	public boolean getEngineServiceIsReady() {return engineServiceIsReady;}	//>442 true if C4aMain is bound to EngineService
	public boolean getIsReady()	{return isReady;}
    public CharSequence getEngineName()	{return engineName;}
    public void newGame() 
    {
    	if (isChess960)
    		writeLineToProcess("setoption name UCI_Chess960 value true");
    	else
    		writeLineToProcess("setoption name UCI_Chess960 value false");
		writeLineToProcess("ucinewgame");
		syncReady();
	}
    public void startSearch(CharSequence fen, CharSequence moves, int wTime, int bTime,	int wInc, int bInc, 
    						int movesTime, int movesToGo, boolean isInfinite, boolean isDrawOffer, int mate) 
    {
//    	writeLineToProcess("setoption multipv value 500");	// TEST: list of all valid moves
//    	setMaxSearchTime(fen, wTime, bTime, movesTime);
    	if (isChess960)
    		fen = convertCastlingRight(fen);
    	CharSequence posStr = "";
    	posStr = posStr + "position fen ";								// position(FEN)
    	posStr = posStr.toString() + fen;
    	if (!moves.equals(""))
    		posStr = posStr + " moves " + moves;						// + move
    	writeLineToProcess(posStr);										// writeLineToProcess
    	// go
    	CharSequence goStr = "";
    	goStr = goStr + "go ";											// go
    	if (mate > 0)
			goStr = goStr + " mate " + mate;	// mate
		else
		{	
	    	if (isInfinite)
	    		goStr = goStr + " infinite ";								// search until the "stop" command
	    	else
	    	{
		    	if (movesTime > 0)												
		    		goStr = goStr + " movetime  " + movesTime;			// movetime
		    	else
		    	{
			    	goStr = goStr + " wtime " + wTime + " btime " + bTime;	// wtime + vtime + winc + binc + movestogo
			    	goStr = goStr + " winc " + wInc + " binc " + bInc;
			    	if (movesToGo > 0)
			    		goStr = goStr + " movestogo " + movesToGo;
		    	}
	    	}
		}
    	writeLineToProcess(goStr);										// writeLineToProcess
    }
    public final void parseInfoCmd(CharSequence[] tokens, int infoPvMoveMax) 
    {
    	boolean infoHasPvValues = false;
    	statIsMate = false;
        statPvAction = "";
        statPvIdx = 0;
    	try 
    	{
    		pvValuesChanged = false;
    		int nTokens = tokens.length;
    		int i = 1;
    		while (i < nTokens - 1) 
    		{
    			CharSequence is = tokens[i++];
    			if (is.equals("depth")) {
    				statCurrDepth = Integer.parseInt(tokens[i++].toString());
    			} else if (is.equals("currmove")) {
    				statCurrMove = tokens[i++];
    			} else if (is.equals("currmovenumber")) {
    				statCurrMoveNr = Integer.parseInt(tokens[i++].toString());
    			} else if (is.equals("time")) {
    				statTime = Integer.parseInt(tokens[i++].toString());
    			} else if (is.equals("nodes")) {
    				statNodes = Integer.parseInt(tokens[i++].toString());
    			} else if (is.equals("nps")) {
    				statNps = Integer.parseInt(tokens[i++].toString());
    			} else if (is.equals("multipv")) {
    				statPvIdx = ((Integer.parseInt(tokens[i++].toString())) -1);
   					engineWithMultiPv = true;
//    				Log.i(TAG, "statMultipv: " + statMultipv);
    			} else if (is.equals("pv")) {
//    				Log.i(TAG, "isDepth, engineWithMultiPv: " + isDepth + ", " + engineWithMultiPv);
//    				if (		isDepth & !engineWithMultiPv
//    						| 	!isDepth & engineWithMultiPv)
//					if (engineWithMultiPv)
    				{
	    				infoHasPvValues = true;
	    				statPv.clear();
	    				while (i < nTokens)
	    					statPv.add(tokens[i++]);
	    				if (statPvIdx == 0)
	    					statPvBestMove = statPv.get(0);
	    				statPVDepth = statCurrDepth;
    				}
    			} else if (is.equals("score")) {
//    				Log.i(TAG, "tokens: " + tokens[i] + ", " + tokens[i +1]);
    				if (tokens[i].equals("cp"))
    					statPvScore = Integer.parseInt(tokens[i +1].toString());
    				if (statPvIdx == 0)
    					statPvBestScore = statPvScore;
    				if (tokens[i].equals("mate"))	
    				{
    					statIsMate = true;
    					statPvScore = Integer.parseInt(tokens[i +1].toString());
    					if (statPvScore < 0)
    						statPvScore = statPvScore * -1;
    				}
    			}
    		}
    	}
		catch (NumberFormatException nfe) {} 
		catch (ArrayIndexOutOfBoundsException aioob) {	}
    	if (statPv.size() > 0)
    	{
	    	if (infoHasPvValues)
	    	{
	    		statPvAction = "1";
	    		statPvMoves = getMoves(statPv, infoPvMoveMax);
	    	}
    	}
//    	Log.i(TAG, "size, values?, score, moves: " + statPv.size() + ", " + infoHasPvValues + ", " + statPvScore + ", " + statPvMoves);
	}
    public CharSequence getMoves(ArrayList<CharSequence> statPv, int infoPvMoveMax)
    {
    	CharSequence moves = "";
    	for (int i = 0; i < statPv.size(); i++)
		{	
    		if (i >= infoPvMoveMax)
    			break;
    		if (statPv.get(i).toString().length() < 4 | statPv.get(i).toString().length() > 5)
    			break;
    		if (i == statPv.size() -1 | i == infoPvMoveMax -1)
    			moves = moves + statPv.get(i).toString();
    		else
    			moves = moves + statPv.get(i).toString() + " ";
//    		if (i >= infoPvMoveMax)
//    			break;
		}
    	return moves;
    }
    public boolean getSearchAlive() {return searchAlive;}
//    public void setSearchAlive(boolean alive) {searchAlive = alive;}
    public void stopSearch() 
    {
    	writeLineToProcess("stop");
    }
    public CharSequence convertCastlingRight(CharSequence fen)	// using for chess960(castle rook's line instead of "QKqk") 
    {	// using for chess960(castle rook's line instead of "QKqk") 
    	CharSequence convertFen = "";
    	CharSequence startLineBlack = "abcdefgh";
    	CharSequence startLineWhite = "ABCDEFGH";
    	char cast_K = ' ';
    	char cast_Q = ' ';
    	char cast_k = ' ';
    	char cast_q = ' ';
    	CharSequence castling = "";
    	boolean firstRook = true;
    	// start FEN
    	if (startFen.length() > 7)
    	{
    		CharSequence fenBaseLine = startFen.subSequence (0, 8);
	    	for (int i = 0; i < 8; i++) 
	    	{
				if (fenBaseLine.charAt(i) == 'r')
				{
					if (firstRook)
					{
						firstRook = false;
						cast_Q = startLineWhite.charAt(i);
						cast_q = startLineBlack.charAt(i);
					}
					else
					{
						cast_K = startLineWhite.charAt(i);
						cast_k = startLineBlack.charAt(i);
					}
				}
			}
    	}
    	// current FEN
    	CharSequence[] tokens = tokenize(fen);
    	for (int i = 0; i < tokens[2].length(); i++) 
    	{
    		if (tokens[2].charAt(i) == 'K')
    			castling = castling.toString() + cast_K;
    		if (tokens[2].charAt(i) == 'k')
    			castling = castling.toString() + cast_k;
    		if (tokens[2].charAt(i) == 'Q')
    			castling = castling.toString() + cast_Q;
    		if (tokens[2].charAt(i) == 'q')
    			castling = castling.toString() + cast_q;
    		if (tokens[2].charAt(i) == '-')
    			castling = "-";
		}
		tokens[2] = castling;
    	for (int i = 0; i < tokens.length; i++) 
    	{
    		convertFen = convertFen.toString() + tokens[i] + " ";
		}
//    	Log.i(TAG, "FEN sta: " + startFen);
//    	Log.i(TAG, "FEN fen: " + fen);
//    	Log.i(TAG, "FEN new: " + convertFen);
    	return convertFen;
    }
    public void setIsChess960(boolean chess960) {isChess960 = chess960;}
    public void setStartFen(CharSequence fen) {startFen = fen;}
    public void setBestMove(CharSequence move) {bestMove = move;}
    public CharSequence getCurrentProcess() {return currentProcess;}	//>441 the currently running process on ChessEngineService --->157
    public CharSequence getBestMove() {return bestMove;}
    public CharSequence getRandomFirstMove() 			
    {
    	CharSequence move = "e2e4";
    	Random r;
    	r = new Random();
		move = firstMove[randomMove[r.nextInt(100)]];
		return move;
    }
//    public CharSequence getProcessLog() {return processLog;}
    public final void shutDown()	
	{	//472 quit the ChessEngine(Shut down process)
		if (processAlive) 
		{
			writeLineToProcess("stop");
			try {Thread.sleep(100);} 
			catch (InterruptedException e) {}
			writeLineToProcess("quit");
			processAlive = false;
			try {Thread.sleep(100);} 
			catch (InterruptedException e) {}
		}
	}
    // 	ENGINE-SERVICE(INIT, CLOSE)			ENGINE-SERVICE(INIT, CLOSE)			ENGINE-SERVICE(INIT, CLOSE)
    @Override
	public void onServiceConnected(ComponentName name, IBinder boundService) 
	{	//> 431 Object ices : bind to IChessEngineService
//		Log.i(TAG, "vor onServiceConnected");
    	switch (engineServiceId)
        {
    	// !!! one entry for each EngineService
    	//>493 E: engine package Name, ices: InterfaceChessEngineService
    	case 0:	ices = ccc.chess.engine.stockfish.IChessEngineService.Stub.asInterface((IBinder) boundService);
	        	break;
	    case 1: ices = ccc.chess.engines.IChessEngineService.Stub.asInterface((IBinder) boundService);
	        	break;
        }
// set UCI options    	
    	optionHash = 16;		// default 8
    	optionMultiPv = c4aM.userP.getInt("user_options_enginePlay_MultiPv", 4);	// default 1
    	optionPonder = false;	// default false
    	initEngineService();	
		c4aM.ec.setEngineNumber(engineNumber);
	}
	@Override
	public void onServiceDisconnected(ComponentName name) 
	{	//> 471 closeProcess()
		closeProcess();
	}
	public void setEngineServiceId(int engineServiceId)	
	{	//>432 getting ChessEngineServiceInterface from R.array.engineServices
    	this.engineServiceId = engineServiceId;
	}
	public void initEngineService()	
	{
//		Log.i(TAG, "ices: " + ices);
		processAlive = false;
		isReady = false;
		if (ices != null) startProcess();	//>433
		if (processAlive) 
		{
			writeLineToProcess("uci");
			readUCIOptions();
			writeLineToProcess("setoption name Hash value " + optionHash); 		// default 8
			writeLineToProcess("setoption name Ponder value " + optionPonder);	// default false
			writeLineToProcess("setoption name MultiPV value " + optionMultiPv);
			writeLineToProcess("ucinewgame");
			syncReady();
		}
	}
	public void closeProcess() 
	{	//>473 processAlive = false; ices = null;
    	processAlive = false;	
    	ices = null;
	}
	// 	GUI/ENGINE-SERVICE(METHODS)		GUI/ENGINE-SERVICE(METHODS)		GUI/ENGINE-SERVICE(METHODS)
    public final void startProcess() 
	{	//>484 calls ChessEngineService method --->164 (ccc_engine)
    	// NATIVE PROCESS: startNewProcess (EngineService, Class: ChessEngineService)
    	engineServiceIsReady = false;
    	try 	
		{ 
    		switch (engineServiceId)
            {
	    		// !!! one entry for each EngineService
	        	case 0:	engineServiceIsReady = ((ccc.chess.engine.stockfish.IChessEngineService) ices).startNewProcess(GUI_PROCESS_NAME.toString()); break;
	        	case 1:	engineServiceIsReady = ((ccc.chess.engines.IChessEngineService) ices).startNewProcess(GUI_PROCESS_NAME.toString()); break;
            }
//    		Log.i(TAG, "engineServiceIsReady: " + engineNumber + ", " + engineServiceIsReady);
			if (engineServiceIsReady)
				processAlive = true;
			else
			{
				currentProcess = getInfoFromEngineService("CURRENT_PROCESS");
				closeProcess();
			}
			engineName = getInfoFromEngineService("ENGINE_PROCESS_NAME");
			engineType = getInfoFromEngineService("ENGINE_TYPE");
//			Log.i(TAG, "engineName, currentProcess: " + engineName + ", " + currentProcess);
		} 
    	catch 	( RemoteException e) 
		{
			e.printStackTrace(); 
			closeProcess();
		}
	}
    public final synchronized void writeLineToProcess(CharSequence data) 
	{	//>485 calls ChessEngineService method --->165 (ccc_engine)
    	// NATIVE PROCESS: writeLineToProcess (EngineService, Class: ChessEngineService)
//    	Log.i(TAG, "writeLine, ENGINE_ALIVE, processAlive: " + getInfoFromEngineService("ENGINE_ALIVE") + ", " + processAlive);
		if (processAlive)
		{
			try 	
			{ 
				switch (engineServiceId)
		        {	// !!! one entry for each EngineService
			    	case 0:	((ccc.chess.engine.stockfish.IChessEngineService) ices).writeLineToProcess(data.toString()); break;
			    	case 1:	((ccc.chess.engines.IChessEngineService) ices).writeLineToProcess(data.toString()); break;
		        }
			} 
			catch 	( RemoteException e) 
			{
				e.printStackTrace(); 
				closeProcess();
			}
		}
	}
    public final CharSequence readLineFromProcess(int timeoutMillis) 
	{	//>486 calls ChessEngineService method --->166 (ccc_engine)
    	// NATIVE PROCESS: readLineFromProcess (EngineService, Class: ChessEngineService)
    	CharSequence ret = "";
//    	Log.i(TAG, "readLine, ENGINE_ALIVE, processAlive: " + getInfoFromEngineService("ENGINE_ALIVE") + ", " + processAlive);
		if (processAlive)
		{
			try 	
			{ 
				switch (engineServiceId)
		        {	// !!! one entry for each EngineService
			    	case 0:	ret = ((ccc.chess.engine.stockfish.IChessEngineService) ices).readLineFromProcess(timeoutMillis); break;
			    	case 1:	ret = ((ccc.chess.engines.IChessEngineService) ices).readLineFromProcess(timeoutMillis); break;
		        }
			} 
			catch 	( RemoteException e) 
			{
				e.printStackTrace(); 
				closeProcess();
			}
			if (ret == null)
			{
//				return null;
//				Log.i(TAG, "readLine == NULL !");
				ret = "";
			}
			if (ret.length() > 0)
			{
//				Log.i(TAG, "Engine -> GUI: " + ret);
			}
		}
		return ret;
	}
	
	public CharSequence getInfoFromEngineService(CharSequence infoId)
	{	//>487 calls ChessEngineService method --->167 (ccc_engine)
		// INFO: getInfoFromEngineService (EngineService, Class: StartChessEngine)
		CharSequence info = "";
    	try 	
    	{
    		switch (engineServiceId)
            {	// !!! one entry for each EngineService
	        	case 0:	info = ((ccc.chess.engine.stockfish.IChessEngineService) ices).getInfoFromEngineService(infoId.toString()); break;
	        	case 1:	info = ((ccc.chess.engines.IChessEngineService) ices).getInfoFromEngineService(infoId.toString()); break;
            }
    	}
    	catch 	(NullPointerException e) 	{info = "";}
		catch 	(RemoteException e) 		{info = "";}
		return info;
	}
	
	final String TAG = "ChessEngine";
	C4aMain c4aM;
	private static final CharSequence GUI_PROCESS_NAME = "C4A";	//>412 an unequal gui process name --->157 (ccc_engine)
	public int engineNumber = 1;			// engine number for client UI handling
	public int engineServiceId = 0;			// index from res/values/resources/string-array/engineServices
	public CharSequence engineServiceName = "";	// IntefaceChessEngineService className
	Object ices = null;						// IntefaceChessEngineService object
    public CharSequence engineName = "";			// native engine name 
    public CharSequence engineNameStrength = "";	// native engine name + strength
    public CharSequence engineType = "";			// native engine type (JNI, ARM)
	boolean isBound = false;	
	boolean engineServiceIsReady = false;
	boolean processAlive;
	boolean isReady = false;
	boolean isChess960 = false;
	boolean startPlay = false;
// UCI options	
	boolean optionPonder = false;
//	int 	optionHash = 16;
	int 	optionHash = 64;
	int 	optionMultiPv = 1;
	int 	minimumThinking = 20;
	int 	optionMinimumThinking = minimumThinking * optionMultiPv;
	
	CharSequence currentProcess = "";
	CharSequence startFen = "";
	CharSequence bestMove = "";
// search control
	boolean searchAlive = true;
	int searchCount = 0;

	ArrayList<CharSequence> statPv = new ArrayList<CharSequence>();
	boolean pvValuesChanged = false;
	boolean engineWithMultiPv = false;
	
	CharSequence statPvAction = "";
	int statPvIdx = 0;
	int statPvScore = 0;
	int statPvBestScore = 0;
	CharSequence statPvMoves = "";
	CharSequence statPvBestMove = "";
	
	int statPVDepth = 0;
    int statTime = 0;
    int statCurrDepth = 0;
    int statCurrMoveNr = 0;
    int statNodes = 0;
    int statNps = 0;
    CharSequence statCurrMove = "";
    boolean statIsMate = false;
    final CharSequence firstMove[] =	{	"a2a3", "a2a4", "b2b3", "b2b4",	
			"c2c3", "c2c4", "d2d3", "d2d4",	
			"e2e3", "e2e4", "f2f3", "f2f4",	
			"g2g3", "g2g4", "h2h3", "h2h4",
			"b1a3", "b1c3", "g1f3", "g1h3"};
    final int randomMove[] =	{	0, 1, 2, 2,	2, 3, 3, 3, 4, 4,
			4, 4, 5, 5,	5, 5, 5, 5, 5, 5,
			5, 5, 5, 5,	6, 6, 6, 6, 7, 7,
			7, 7, 7, 7,	7, 7, 7, 7, 7, 7,
			7, 7, 7, 7,	8, 8, 8, 9, 9, 9,
			9, 9, 9, 9,	9, 9, 9, 9, 9, 9,
			9, 9, 9, 9,	9, 9, 9, 10, 11, 11,
			11, 11, 11, 11,	12, 12, 12, 12, 12, 12,
			13, 13, 13, 14,	15, 16, 17, 17, 17, 17,
			17, 17, 18, 18,	18, 18, 18, 18, 18, 19,};
}
