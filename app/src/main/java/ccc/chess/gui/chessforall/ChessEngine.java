package ccc.chess.gui.chessforall;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.XmlResourceParser;
import android.util.Log;

import com.kalab.chess.enginesupport.ChessEngineResolver;

import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
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
        errorMessage = "";

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

    private synchronized boolean readUCIOptions()
    {
        int timeout = 1000;
        long startTime = System.currentTimeMillis();
        long checkTime = startTime;
        isUciPonder = false;
        uciOptions = "";

        while (checkTime - startTime <= MAX_UCI_TIME)
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

//            if (isLogOn)
//                Log.i(TAG, "readUCIOptions(), s: " + s);

            if (s.toString().startsWith("option name UCI_Elo")) {
                int eloMin = 0;
                int eloMax = 0;
                String[] split = s.toString().split(" ");
                if (split.length >= 0) {
                    for (int i = 0; i < split.length; i++) {
                        try{
                            int num = Integer.parseInt(split[i]);
                            if (split[i -1].equals("min"))
                                eloMin = num;
                            if (split[i -1].equals("max"))
                                eloMax = num;
                        }
                        catch (NumberFormatException e) {  }
                    }
                }
                if (eloMin != 0 && eloMax != 0) {
                    SharedPreferences.Editor ed = userPrefs.edit();
                    ed.putInt("uci_elo_min", eloMin);
                    ed.putInt("uci_elo_max", eloMax);
                    ed.apply();
                }
            }

            if (s.toString().contains("option name"))
                uciOptions = uciOptions + s + "\n";
            if (s.toString().contains("option") & s.toString().contains("Ponder"))
                isUciPonder = true;
            CharSequence[] tokens = tokenize(s);
            if (tokens[0].equals("uciok")) {

                if (isLogOn)
                    Log.i(TAG, "readUCIOptions(), uciok");

                return true;
            }

            checkTime = System.currentTimeMillis();

        }

//		if (isLogOn) {
//            Log.i(TAG, "readUCIOptions(), uciOptions: \n" + uciOptions);
//            Log.i(TAG, "readUCIOptions(), timeout");
//        }

        errorMessage = engineProcess + ":  uci error";
        if (isLogOn)
            Log.i(TAG, "readUCIOptions(), errorMessage: " + errorMessage);

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

    public synchronized boolean syncReady()
    {

//        Log.i(TAG, "1 syncReady(), start");

        if (process == null)
            return false;

//        Log.i(TAG, "2 syncReady(), start");

		engineState = EngineState.WAIT_READY;
        writeLineToProcess("isready");
        long startTime = System.currentTimeMillis();
        long checkTime = startTime;

        while (checkTime - startTime <= MAX_ISREADY_TIME)
        {

//            Log.i(TAG, "3 syncReady(), start");

            CharSequence s = readLineFromProcess(1000);

//            Log.i(TAG, "4 syncReady(), s: " + s);

            if (s.equals("ERROR")) {
                if (isLogOn)
                    Log.i(TAG, "syncReady(), errorMessage: ERROR");

                return false;
            }

            if (s.equals("readyok"))
            {
                engineState = EngineState.IDLE;
                return true;
            }

            checkTime = System.currentTimeMillis();

        }

        errorMessage = engineProcess + ":  isready error";
        if (isLogOn)
            Log.i(TAG, "syncReady(), errorMessage: " + errorMessage);

        return false;

    }

    public void setElo(Boolean withElo, int elo)
    {
        if (uciOptions.contains("UCI_LimitStrength"))
            writeLineToProcess("setoption name UCI_LimitStrength value " + withElo);
        if (uciOptions.contains("UCI_Elo"))
            writeLineToProcess("setoption name UCI_Elo value " + elo);
        if (withElo && uciOptions.contains("UCI_Elo")) {
            engineNameElo = " (" + elo + ")";
        }
        else {
            engineNameElo = "";
        }
        engineName = uciEngineName + engineNameElo;
    }

    public boolean newGame()
    {
        if (uciOptions.contains("UCI_Chess960"))
            writeLineToProcess("setoption name UCI_Chess960 value " + isChess960);
        writeLineToProcess("ucinewgame");
        return true;
    }

    public void startSearch(CharSequence searchFen, CharSequence moves, CharSequence startFen, int wTime, int bTime,	int wInc, int bInc,
                            int movesTime, int movesToGo, boolean isInfinite, boolean isGoPonder, int mate)
    {

//Log.i(TAG, "startSearch(), fen: " + fen + "\nmoves: " + moves + ", isGoPonder: " + isGoPonder);

        CharSequence fen = searchFen;

        if (isChess960)
            fen = convertCastlingRight(fen, startFen);

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
                if (uciOptions.contains("UCI_AnalyseMode"))
                    writeLineToProcess("setoption name UCI_AnalyseMode value true");

                setElo(false, uciEloMax);

				engineState = EngineState.ANALYZE;
				goStr = goStr + " infinite ";                                // search until the "stop" command
			}
            else
            {
                if (uciOptions.contains("UCI_AnalyseMode"))
                    writeLineToProcess("setoption name UCI_AnalyseMode value false");

                //karl TEST
                if (withUciElo)
                    setElo(true, uciElo);
                else
                    setElo(false, uciEloMax);

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

    public CharSequence convertCastlingRight(CharSequence fen, CharSequence startFen)	// using for chess960(castle rook's line instead of "QKqk")
    {

//        Log.i(TAG,  "convertCastlingRight(), startFen: " + startFen);
//        Log.i(TAG,  "convertCastlingRight(), fen:      " + fen);

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
                Log.i(TAG,  "startNewProcess(), engine process started: " + engineProcess + ", processAlive: " + processAlive);
			engineState = EngineState.READ_OPTIONS;
            writeLineToProcess("uci");

            //karl ???
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
        else {
//            if (isLogOn)
//                Log.i(TAG,  engineName + ": " + message);
            message = "";
        }
        return message;
    }

    private void setChessEngineName(String uciIdName)
    {
        engineName = uciIdName.substring(8, uciIdName.length());
        uciEngineName = uciIdName.substring(8, uciIdName.length());
        if (uciIdName.startsWith("White(1): id name ")) {
            engineName = uciIdName.substring(18, uciIdName.length());
            uciEngineName = uciIdName.substring(18, uciIdName.length());
        }
        engineName = uciEngineName + engineNameElo;
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

                Log.i(TAG,  "setUciEloValues(), uciEloMin: " + uciEloMin + ", uciEloMax: " + uciEloMax);

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
            if (uciOptions.contains("MultiPV"))
                writeToProcess("setoption name MultiPV value " + multiPV + "\n");
        }
        catch (IOException e) {e.printStackTrace();}
    }

    void setUciHash(int hash)
    {
        try
        {
            if (uciOptions.contains("Hash"))
                writeToProcess("setoption name Hash value " + hash + "\n");
        }
        catch (IOException e) {e.printStackTrace();}
    }

    void setUciPonder(boolean ponder)
    {
        try
        {
            if (uciOptions.contains("Ponder"))
                writeToProcess("setoption name Ponder value " + ponder + "\n");
        }
        catch (IOException e) {e.printStackTrace();}
    }

    void setUciOptsFromFile(String uciOpts)
    {
        String[] split = uciOpts.split("\n");
        if (split.length >= 0) {
            for (int i = 0; i < split.length; i++) {
                if (split[i].startsWith("setoption")) {
                    try
                    {
                        if (isFilePathOK(split[i]))
                            writeToProcess(split[i] + "\n");
                    }
                    catch (IOException e) {e.printStackTrace();}
                }
            }
        }
    }

    Boolean isFilePathOK(String uciOption)
    {
        if (uciOption.contains("/files/")) {
            String[] split = uciOption.split(" ");
            if (split.length >= 0) {
                File file = new File(split[split.length -1]);
                if (!file.exists()) {
                    if (isLogOn)
                        Log.i(TAG, "file not exists: " + uciOption);
                    return false;
                }
            }
        }
        return true;
    }

    public final synchronized boolean engineInit() {
        switch (engineState) {
            case READ_OPTIONS:
            case WAIT_READY:
                return true;
            default:
                return false;
        }
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
            //karl++
            case BOOK:
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
        oexPackage = "";
        oexFileName = "";

        ChessEngineResolver resolver = new ChessEngineResolver(context);
        List<com.kalab.chess.enginesupport.ChessEngine> engines = resolver.resolveEngines();
        // extern oex engine
        for (com.kalab.chess.enginesupport.ChessEngine engine : engines)
        {
            if (engine.getName().equals(engineProcess))
            {
                processBuilder = new ProcessBuilder(engine.getEnginePath());
                if (processBuilder != null) {
                    oexPackage = engine.getPackageName();
                    oexFileName = engine.getFileName();
                    uciFileName = oexPackage + "_" + oexFileName;
                    uciFileName = uciFileName.replace(".", "_");
                    uciFileName = uciFileName + ".txt";
                    if (isLogOn) {
                        Log.i(TAG, "startProcess(), extern OEX engine, enginePath: " + engine.getEnginePath());
                        Log.i(TAG, "startProcess(), extern OEX engine, packageName: " + oexPackage);
                        Log.i(TAG, "startProcess(), extern OEX engine, filename: " + oexFileName);
                        Log.i(TAG, "startProcess(), uciFileName: " + uciFileName);
                    }
                }

                break;
            }
        }

        // intern oex engine
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
//                                    if (isLogOn)
//                                        Log.i(TAG,  "startProcess(), intern engine, enginePath: " + enginePath);
                                    processBuilder = new ProcessBuilder(enginePath);
                                    if (processBuilder != null) {
                                        oexPackage = "ccc.chess.gui.chessforall";
                                        oexFileName = parser.getAttributeValue(null, "filename");
                                        uciFileName = oexPackage + "_" + oexFileName;
                                        uciFileName = uciFileName.replace(".", "_");
                                        if (isLogOn) {
                                            Log.i(TAG, "startProcess(), intern OEX engine, enginePath: " + enginePath);
                                            Log.i(TAG, "startProcess(), intern OEX engine, packageName: " + oexPackage);
                                            Log.i(TAG, "startProcess(), intern OEX engine, filename: " + oexFileName);
                                            Log.i(TAG, "startProcess(), uciFileName: " + uciFileName);
                                        }
                                    }

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
                if (isLogOn) {
                    Log.i(TAG, engineName + ": startProcess, IOException\n" + e);
                }
                return false;
            }
        }

    }

    private void destroyProcess()
    {
        process.destroy();
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
    static final int UCI_ELO_STANDARD = 3000;
    final long MAX_UCI_TIME = 2000;
    final long MAX_ISREADY_TIME = 6000;

    Context context;
    public int engineNumber = 1;		                    // default engine (Stockfish)
    private static final String ENGINE_TYPE = "UCI";		//> ChessEngine type: CE(Chess Engines)
    SharedPreferences userPrefs;		                    // user preferences(LogFile on/off . . .)
    String uciEngineName = "";				                // the uci engine name
    String engineName = "";				                    // engine name (displayed)
    String engineNameElo = "";				                // elo if engine strength < max
    public CharSequence engineNameStrength = "";	        // native engine name + strength
    String engineProcess = "";			                    // the compiled engine process name (file name)
    String oexPackage = "";			                        // oex package name
    String oexFileName = "";			                    // oex file name
    String uciFileName = "";			                    // uci file name (for saving in ExternalStorage)
    final String INTERN_ENGINE_NAME_END = " CfA";
    String mesInitProcess = "";
    String errorMessage = "";

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
		STOP_IDLE,   	    // "stop" sent, ignore "bestmove", and set to IDLE
		STOP_MOVE,   	    // "stop" sent, waiting for "bestmove", and make move
		STOP_MOVE_CONTINE,  // "stop" sent, waiting for "bestmove", continue with next "search" (continueFen)
        STOP_CONTINUE,      // "stop" sent, ignore "bestmove", continue with next "search" (continueFen)
        STOP_NEW_GAME,      // "stop" sent, ignore "bestmove", start new game
        STOP_QUIT,          // "stop" sent and quit
        STOP_QUIT_RESTART,  // "stop" sent and quit and restart an engine
		DEAD,               // engine process has terminated
	}
	public EngineState engineState = EngineState.DEAD;

    boolean processAlive;
    boolean isReady = false;
    boolean isChess960 = false;
    boolean startPlay = false;

    String uciOptions = "";

    boolean isUciStrength = false;
    boolean isUciEloOption = false;
    boolean isUciSkillOption = false;
    boolean isUciPonder = false;
    Boolean withUciElo;

    int uciEloMin = 800;
    int uciEloMax = UCI_ELO_STANDARD;
    int uciElo = UCI_ELO_STANDARD;
    int uciSkillLevelMin = 1;
    int uciSkillLevelMax = 100;

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
