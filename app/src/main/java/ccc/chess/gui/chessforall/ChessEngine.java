package ccc.chess.gui.chessforall;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.XmlResourceParser;
import android.util.Log;

import com.kalab.chess.enginesupport.ChessEngineResolver;

import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ChessEngine
{
    ChessEngine(Context con, int eNumber)
    {
        this.context = con;
        engineNumber = eNumber;
        processAlive = false;
        isReady = false;
        userPrefs = context.getSharedPreferences("user", 0);
        isLogOn = userPrefs.getBoolean("user_options_enginePlay_logOn", false);
    }

    public boolean initProcess(String processName)
    {

//Log.i(TAG, "1 initProcess(), processName: " + processName);

        if (process != null)
            destroyProcess();
        process = null;
        reader = null;
        writer = null;

        boolean isInitOk = false;
        engineProcess = processName;
        if (startNewProcess(true))
            isInitOk = true;
        else
            engineProcess = "";

//		Log.i(TAG, "2 initProcess(), processName: " + processName + ", isInitOk: " +isInitOk);

        return isInitOk;

    }

    public void initPv()
    {
        pvValuesChanged = false;
        statPv = new ArrayList<CharSequence>();
        statPvAction = "";
        statPvIdx = 0;
        statPvScore = 0;
        statPvBestScore = 0;
        statPvMoves = "";
        statPvBestMove = "";
        statPvPonderMove = "";
        statPVDepth = 0;
        statTime = 0;
        statCurrDepth = 0;
        statCurrSelDepth = 0;
        statCurrMoveNr = 0;
        statCurrMoveCnt = 0;
        statCurrMove = "";
        statCurrNodes = 0;
        statCurrNps = 0;
        statIsMate = false;
    }

    //karl EditUciOptions : edit und save if != default in sd/c4a/uci pro engine mit uciPath/fileName (von Droidfish)
    //karl setEngineOptions(sd/c4a/uci) --> engine
    private synchronized boolean readUCIOptions()
    {
        int timeout = 1000;
        long startTime = System.currentTimeMillis();
        long checkTime = startTime;
        isUciPonder = false;
        while (checkTime - startTime <= MAX_SYNC_TIME)
        {
			if (Thread.currentThread().isInterrupted())
			{

				if (isLogOn)
					Log.i(TAG, "readUCIOptions(), Thread.currentThread().isInterrupted()");

				return false;
			}
            CharSequence s = readLineFromProcess(timeout);
            if (s.equals("ERROR"))
			{

				if (isLogOn)
					Log.i(TAG, "readUCIOptions(), ERROR");

				return false;
			}
            if (s.toString().contains("option") & s.toString().contains("Ponder"))
                isUciPonder = true;
            CharSequence[] tokens = tokenize(s);
            if (tokens[0].equals("uciok"))
                return true;
            checkTime = System.currentTimeMillis();
        }

		if (isLogOn)
			Log.i(TAG, "readUCIOptions(), timeout");

        return false;

    }



    public CharSequence[] tokenize(CharSequence cmdLine)
    {
        cmdLine = cmdLine.toString().trim();
        return cmdLine.toString().split("\\s+");
    }

    public synchronized void stopSearch(EngineState eState)
    {

//Log.i(TAG, "1 stopSearch(), eState: " + eState);

        if (process == null)
            return;

        engineState = eState;


        writeLineToProcess("stop");

    }

//    public boolean syncReady()
    public synchronized boolean syncReady()
    {

//Log.i(TAG, "syncReady(), start");

        if (process == null)
            return false;

//        isReady = false;
		engineState = EngineState.WAIT_READY;
        writeLineToProcess("isready");
        long startTime = System.currentTimeMillis();
        long checkTime = startTime;
        int cntSpace = 0;

        while (checkTime - startTime <= MAX_SYNC_TIME)
        {
            CharSequence s = readLineFromProcess(1000);
            if (s.equals("ERROR"))
                return false;
            if (s.equals(""))   // null
                cntSpace++;
            else
                cntSpace = 0;
            if (s.equals("readyok"))
            {
//                if (engineState == EngineState.WAIT_READY)
                engineState = EngineState.IDLE;
//                isReady = true;
                return true;
            }
            checkTime = System.currentTimeMillis();
        }

        return false;

    }

    public boolean newGame()
    {
        if (isChess960)
            writeLineToProcess("setoption name UCI_Chess960 value true");
        else
            writeLineToProcess("setoption name UCI_Chess960 value false");
        writeLineToProcess("ucinewgame");
        return true;
    }

    public void startSearch(CharSequence fen, CharSequence moves, int wTime, int bTime,	int wInc, int bInc,
                            int movesTime, int movesToGo, boolean isInfinite, boolean isGoPonder, int mate)
    {

//Log.i(TAG, "startSearch(), fen: " + fen + "\nmoves: " + moves + ", isGoPonder: " + isGoPonder);

        if (isChess960)
            fen = convertCastlingRight(fen);
        String posStr = "";
        posStr = posStr + "position fen ";								// position(FEN)
        posStr = posStr.toString() + fen;
        if (!moves.equals(""))
            posStr = posStr + " moves " + moves;						// + move
        writeLineToProcess(posStr);										// writeLineToProcess
        String goStr = "";
        goStr = goStr + "go ";
		engineState = EngineState.SEARCH;
        if (isGoPonder)
		{
			engineState = EngineState.PONDER;
			goStr = goStr + " ponder ";
		}
        // go
        if (mate > 0)
            goStr = goStr + " mate " + mate;	                        // mate
        else
        {
            if (isInfinite)
			{
				engineState = EngineState.ANALYZE;
				goStr = goStr + " infinite ";                                // search until the "stop" command
			}
            else
            {
                if (movesTime > 0)
                    goStr = goStr + " movetime  " + movesTime;			// movetime
                else
                {
                    goStr = goStr + " wtime " + wTime + " btime " + bTime;	// wtime + btime + winc + binc + movestogo
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
        statPvIdx = 0;
        try
        {
            pvValuesChanged = false;
            int nTokens = tokens.length;
            int i = 1;
            while (i < nTokens - 1)
            {
                CharSequence is = tokens[i++];

//Log.i(TAG,  "tokens, i: " + i + "(" + (nTokens -1) + "), is: " + is);

                if (is.equals("depth"))     {statCurrDepth = Integer.parseInt(tokens[i++].toString());}
                if (is.equals("seldepth"))  {statCurrSelDepth = Integer.parseInt(tokens[i++].toString());}
                if (is.equals("time"))      {statTime = Integer.parseInt(tokens[i++].toString());}
                if (is.equals("nodes"))     {statCurrNodes = Integer.parseInt(tokens[i++].toString());}
                if (is.equals("nps"))       {statCurrNps = Integer.parseInt(tokens[i++].toString());}
                if (is.equals("multipv"))
                {
                    statCurrMoveNr = Integer.parseInt(tokens[i++].toString());
                    statPvIdx = statCurrMoveNr -1;
                    engineWithMultiPv = true;
                }
                if (is.equals("pv"))
                {
                        infoHasPvValues = true;
                        statPv.clear();
                        while (i < nTokens)
                            statPv.add(tokens[i++]);
                        if (statPvIdx == 0)
                        {
                            statPvBestMove = statPv.get(0);
                            statPvPonderMove = "";
                            if (statPv.size() > 1)
                                statPvPonderMove = statPv.get(1);
                        }
                        statCurrMove = statPv.get(0);
                        statPVDepth = statCurrDepth;
                }
                if (is.equals("cp"))
                    statPvScore = Integer.parseInt(tokens[i++].toString());
                if (statPvIdx == 0)
                    statPvBestScore = statPvScore;
                if (is.equals("mate"))
                {
                    statIsMate = true;
                    statPvScore = Integer.parseInt(tokens[i++].toString());
                    if (statPvScore < 0)
                        statPvScore = statPvScore * -1;
                }
                if (is.equals("currmove"))
                    statCurrMove = tokens[i++];
                if (is.equals("currmovenumber"))
                    statCurrMoveNr = Integer.parseInt(tokens[i++].toString());
                if (statCurrMoveNr > statCurrMoveCnt)
                    statCurrMoveCnt = statCurrMoveNr;
            }
        }
        catch (NumberFormatException nfe) {}
        catch (ArrayIndexOutOfBoundsException aioob) {	}

        if (statPv.size() > 0)
        {
            if (infoHasPvValues)
            {
                statPvMoves = getMoves(statPv, infoPvMoveMax);

//                Log.i(TAG,  "parseInfoCmd(), moves: " + statPvMoves);

            }
        }

//Log.i(TAG,  "size, values?, score, moves: " + statPv.size() + ", " + infoHasPvValues + ", " + statPvScore + "\nstatPvMoves: " + statPvMoves);

    }

    public CharSequence getDisplayMoves(CharSequence moves, int cntMoves)
    {
        CharSequence displayMoves = "";
        String[] split = moves.toString().split(" ");
        if (split.length >= 0) {
            for (int i = 0; i < split.length; i++) {
                if (i < cntMoves)
                    displayMoves = displayMoves + split[i] + " ";
            }
        }

//        Log.i(TAG,  "getDisplayMoves(), displayMoves: " + displayMoves);

        return  displayMoves;
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
        }

//        Log.i(TAG,  "getMoves(), moves: " + moves);

        return moves;
    }

    public CharSequence convertCastlingRight(CharSequence fen)	// using for chess960(castle rook's line instead of "QKqk")
    {
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
            convertFen = convertFen.toString() + tokens[i] + " ";

//Log.i(TAG,  "FEN sta: " + startFen);
//Log.i(TAG,  "FEN fen: " + fen);
//Log.i(TAG,  "FEN new: " + convertFen);

        return convertFen;
    }

    public void setIsChess960(boolean chess960) {isChess960 = chess960;}
    public void setStartFen(CharSequence fen) {startFen = fen;}

    public CharSequence getRandomFirstMove()
    {
        CharSequence move = "e2e4";
        Random r;
        r = new Random();
        move = firstMove[randomMove[r.nextInt(100)]];
        return move;
    }

    public synchronized final void shutDown()
    {	//quit the ChessEngine(Shut down process)

//        Log.i(TAG,  "shutDown(), engine process: " + engineProcess);

		engineState = EngineState.DEAD;
        writeLineToProcess("quit");
        processAlive = false;

    }

    public boolean  startNewProcess(boolean fromFile)
    {

        if (isLogOn)
            Log.i(TAG,  "startNewProcess(), engine process started: " + engineProcess);

        mesInitProcess = "";
        processAlive = false;

        processAlive = startProcess();

        if (processAlive)
        {
            if (isLogOn)
                Log.i(TAG,  "startNewProcess(), engine process started: " + engineProcess);
			engineState = EngineState.READ_OPTIONS;
            writeLineToProcess("uci");
            processAlive = readUCIOptions();
            if (fromFile & processAlive)
            {
                if (engineState == EngineState.READ_OPTIONS)
                    engineState = EngineState.IDLE;
                mesInitProcess = mesInitProcess + "uciok" + "\n";
            }
        }
        if (!processAlive)
        {
            if (isLogOn)
                Log.i(TAG,  "startNewProcess(), start error, engine process: " + engineProcess);
            if (fromFile)
            {
                mesInitProcess = mesInitProcess + engineProcess + ": " + context.getString(R.string.engineNoRespond) + "\n";
                mesInitProcess = mesInitProcess + "\n" + engineProcess + " "  + context.getString(R.string.engineNotInstalled);
            }
        }
        if (processAlive & fromFile)
            mesInitProcess = mesInitProcess + "\n" + engineProcess + " "  + context.getString(R.string.engineInstalled);
        return processAlive;
    }

    public void writeLineToProcess(String data)
    {

//Log.i(TAG,  "data: " + data);

        try {writeToProcess(data + "\n");}
        catch (IOException e)
        {

//            Log.i(TAG,  "IOException, writeLineToProcess()");

            e.printStackTrace();
            engineName = "";
            processAlive = false;
        }
        if (data.equals("quit"))
        {
            engineName = "";
            if (process != null) {
                process.destroy();
                process = null;
            }
        }
    }

    public String readLineFromProcess(int timeoutMillis)
    {
        String message = "";
        try{message =  readFromProcess();}
        catch (IOException e)
        {

//            Log.i(TAG,  "IOException, readLineFromProcess()");

            e.printStackTrace();
            engineName = "";
            processAlive = false;
            process = null;
            return "ERROR";
        }
// 28. Aug. 11:44 in der App-Version 70 : ccc.chess.gui.chessforall.ChessEngine.readFromProcess
        catch (NullPointerException e)
        {

//            Log.i(TAG,  "NullPointerException, readLineFromProcess()");

            e.printStackTrace();
            engineName = "";
            processAlive = false;
            process = null;
            return "ERROR";
        }
        if (message != null)
        {
            if (message.startsWith("id name ") | message.startsWith("White(1): id name "))
                setChessEngineName(message);
            if (message.startsWith("option name ") & message.contains("UCI_Elo"))
            {
                isUciStrength = true;
                isUciEloOption = true;
                setUciEloValues(message);
            }
            if (message.startsWith("option name ") & message.contains("Skill Level"))
            {
                isUciStrength = true;
                isUciSkillOption = true;
                setUciSkillLevelValues(message);
            }
            if (isLogOn)
                Log.i(TAG,  engineName + ": " + message);
        }
        else
            message = "";
        return message;
    }

    private void setChessEngineName(String uciIdName)
    {
        engineName = uciIdName.substring(8, uciIdName.length());
        if (uciIdName.startsWith("White(1): id name "))
            engineName = uciIdName.substring(18, uciIdName.length());
    }

    public boolean getSearchAlive() {return searchAlive;}

    void setUciEloValues(String message)
    {
        String[] messageSplit = message.split(" ");
        String min = "";
        String max = "";
        if (messageSplit.length >= 0)
        {
            for (int i = 0; i < messageSplit.length; i++)
            {
                if (messageSplit[i].equals("min") & messageSplit.length > i)
                    min = messageSplit[i +1];
                if (messageSplit[i].equals("max") & messageSplit.length > i)
                    max = messageSplit[i +1];
            }
            try
            {
                uciEloMin = Integer.parseInt(min);
                uciEloMax = Integer.parseInt(max);
            }
            catch 	(NumberFormatException e) { }
        }
    }
    void setUciSkillLevelValues(String message)
    {
        String[] messageSplit = message.split(" ");
        String min = "";
        String max = "";
        if (messageSplit.length >= 0)
        {
            for (int i = 0; i < messageSplit.length; i++)
            {
                if (messageSplit[i].equals("min") & messageSplit.length > i)
                    min = messageSplit[i +1];
                if (messageSplit[i].equals("max") & messageSplit.length > i)
                    max = messageSplit[i +1];
            }
            try
            {
                uciSkillLevelMin = Integer.parseInt(min);
                uciSkillLevelMax = Integer.parseInt(max);
            }
            catch 	(NumberFormatException e) { }
        }
    }

    void setUciMultiPV(int multiPV)
    {
        try
        {
            writeToProcess("setoption name MultiPV value " + multiPV + "\n");
        }
        catch (IOException e) {e.printStackTrace();}
    }

    void setUciHash(int hash)
    {
        try
        {
            writeToProcess("setoption name Hash value " + hash + "\n");
        }
        catch (IOException e) {e.printStackTrace();}
    }

    void setUciPonder(boolean ponder)
    {
        try
        {
            writeToProcess("setoption name Ponder value " + ponder + "\n");
        }
        catch (IOException e) {e.printStackTrace();}
    }

    void setUciStrength(int userStrength)
    {
        if (!isUciStrength)
            return;
        int eloBase = uciEloMax - uciEloMin;
        int skillStrength = uciEloMin + ((eloBase / 100) * userStrength);
        int skillLevel = (uciSkillLevelMax * userStrength) / 100;

        try
        {
            if (isUciEloOption)
            {
                writeToProcess("setoption name UCI_LimitStrength value true\n");
                writeToProcess("setoption name UCI_Elo value " + skillStrength + "\n");
                uciStrength = skillStrength;
            }
            if (isUciSkillOption)
            {
                writeToProcess("setoption name Skill Level value " + skillLevel + "\n");	// stockfish
                uciStrength = skillLevel;
            }
        }
        catch (IOException e) {e.printStackTrace();}
    }

    void setUciContempt(int contempt)
    {
        try
        {
            writeToProcess("setoption name Contempt value " + contempt + "\n");
        }
        catch (IOException e) {e.printStackTrace();}
    }

    public final synchronized boolean engineStop() {
        switch (engineState) {
            case STOP_IDLE:
            case STOP_MOVE:
            case STOP_MOVE_CONTINE:
            case STOP_CONTINUE:
            case STOP_QUIT:
                return true;
            default:
                return false;
        }
    }

    public final synchronized boolean engineSearching() {
        switch (engineState) {
            case SEARCH:
            case PONDER:
            case ANALYZE:
                return true;
            default:
                return false;
        }
    }

    // NATIVE METHODS		NATIVE METHODS		NATIVE METHODS		NATIVE METHODS		NATIVE METHODS
    private final boolean startProcess()
    {

//        Log.i(TAG,  "startProcess(), engineProcess: " + engineProcess);

		processBuilder = null;

        ChessEngineResolver resolver = new ChessEngineResolver(context);
        List<com.kalab.chess.enginesupport.ChessEngine> engines = resolver.resolveEngines();
        for (com.kalab.chess.enginesupport.ChessEngine engine : engines)
        {
            if (engine.getName().equals(engineProcess))
            {
                if (isLogOn)
                    Log.i(TAG,  "startProcess(), OEX engine, enginePath: " + engine.getEnginePath());

                processBuilder = new ProcessBuilder(engine.getEnginePath());
                break;
            }
        }

        // intern engine
        if (processBuilder == null)
        {
            XmlResourceParser parser = context.getResources().getXml(R.xml.enginelist);
            try
            {
                int eventType = parser.getEventType();
                while (eventType != XmlResourceParser.END_DOCUMENT)
                {
                    try
                    {
                        if (eventType == XmlResourceParser.START_TAG)
                        {
                            if (parser.getName().equalsIgnoreCase("engine"))
                            {
                                if (parser.getAttributeValue(null, "name").equals(engineProcess)
                                        || !engineProcess.endsWith(INTERN_ENGINE_NAME_END))
                                {
                                    engineProcess = parser.getAttributeValue(null, "name");
                                    String enginePath = context.getApplicationInfo().nativeLibraryDir + "/" + parser.getAttributeValue(null, "filename");
                                    if (isLogOn)
                                        Log.i(TAG,  "startProcess(), intern engine, enginePath: " + enginePath);
                                    processBuilder = new ProcessBuilder(enginePath);
                                    break;
                                }
                            }
                        }
                        eventType = parser.next();
                    }
                    catch (IOException e)
                    {

//                        Log.e, e.getLocalizedMessage(), e);

                    }
                }
            }
            catch (XmlPullParserException e)
            {

//                Log.e, e.getLocalizedMessage(), e);

            }
        }

        if (processBuilder == null) {

//			Log.i(TAG,  "startProcess(), processBuilder: " + processBuilder);

            return false;
        }

        else
        {
            try
            {
                process = processBuilder.start();
                OutputStream stdout = process.getOutputStream();
                InputStream stdin = process.getInputStream();
                reader = new BufferedReader(new InputStreamReader(stdin));
                writer = new BufferedWriter(new OutputStreamWriter(stdout));
                return true;
            }
            catch (IOException e)
            {
                if (isLogOn)
                    Log.i(TAG,  engineName + ": startProcess, IOException");
                return false;
            }
        }

    }

    private void destroyProcess()
    {
        process.destroy();;
    }

    private final void writeToProcess(String data) throws IOException
    {
        if (writer != null)
        {
            if (isLogOn)
                Log.i(TAG, "C4A: " + data);
            writer.write(data);
            writer.flush();
        }
    }

    private final String readFromProcess() throws IOException
    {
        String line = null;
        if (reader != null && reader.ready())
        {
            line = reader.readLine();
        }
        return line;
    }

    final String TAG = "ChessEngine";
    final long MAX_SYNC_TIME = 2000;
    final int SYNC_CNT = 200;

    Context context;
    public int engineNumber = 1;		                    // default engine (Stockfish)
    private static final String ENGINE_TYPE = "UCI";		//> ChessEngine type: CE(Chess Engines)
    SharedPreferences userPrefs;		                    // user preferences(LogFile on/off . . .)
    String engineName = "";				                    // the uci engine name
    public CharSequence engineNameStrength = "";	        // native engine name + strength
    String engineProcess = "";			                    // the compiled engine process name (file name)
    final String INTERN_ENGINE_NAME_END = " CfA";
    String mesInitProcess = "";

    ProcessBuilder processBuilder;
    public Process process;
    private BufferedReader reader = null;
    private BufferedWriter writer = null;

	public enum EngineState {
		READ_OPTIONS,       // "uci" command sent, waiting for "option" and "uciok" response.
		WAIT_READY,         // "isready" sent, waiting for "readyok".
		IDLE,               // engine not searching.
		SEARCH,             // "go" sent, waiting for "bestmove"
		PONDER,             // "go" sent, waiting for "bestmove"
		ANALYZE,            // "go" sent, waiting for "bestmove" (which will be ignored)
		BOOK,               // book move
		STOP_IDLE,   	    // "stop" sent, waiting for "bestmove", and set to IDLE
		STOP_MOVE,   	    // "stop" sent, waiting for "bestmove", and make move
		STOP_MOVE_CONTINE,  // "stop" sent, waiting for "bestmove", make move and continue: start next search ? go, ponder, infinite ?
        STOP_CONTINUE,      // "stop" sent, ignore "bestmove", continue with next "search"
        STOP_QUIT,          // "stop" sent and quit
        STOP_QUIT_RESTART,  // "stop" sent and quit and restart an engine
		DEAD,               // engine process has terminated
	}
	public EngineState engineState = EngineState.DEAD;

    boolean processAlive;
    boolean isReady = false;
    boolean isChess960 = false;
    boolean startPlay = false;

    boolean isUciStrength = false;
    boolean isUciEloOption = false;
    boolean isUciSkillOption = false;
    boolean isUciPonder = false;
    int uciEloMin = 1200;
    int uciEloMax = 4000;
    int uciSkillLevelMin = 1;
    int uciSkillLevelMax = 100;
    int uciStrength = 4000;

    CharSequence startFen = "";
    CharSequence continueFen = "";
    public boolean searchAlive = true;
    boolean engineWithMultiPv = false;

    boolean pvValuesChanged = false;
    ArrayList<CharSequence> statPv = new ArrayList<CharSequence>();
    CharSequence statPvAction = "";
    int statPvIdx = 0;
    int statPvScore = 0;
    int statPvBestScore = 0;
    CharSequence statPvMoves = "";
    CharSequence statPvBestMove = "";
    CharSequence statPvPonderMove = "";
    int statPVDepth = 0;
    int statTime = 0;
    int statCurrDepth = 0;
    int statCurrSelDepth = 0;
    int statCurrMoveNr = 0;
    int statCurrMoveCnt = 0;
    CharSequence statCurrMove = "";
    int statCurrNodes = 0;
    int statCurrNps = 0;
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

    boolean isLogOn;			// LogFile on/off(SharedPreferences)

}
