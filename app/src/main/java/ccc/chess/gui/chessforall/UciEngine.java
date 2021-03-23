package ccc.chess.gui.chessforall;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
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

import ccc.chess.logic.c4aservice.ChessLogic;

public class UciEngine
{
    UciEngine(Context con, int engineId, String processName, EngineListener listener)
    {

//        Log.i(TAG, "ChessEngine(Context)");

        this.context = con;
        cl = new ChessLogic(null, "");
        this.engineId = engineId;
        this.listener = listener;
        processAlive = false;
        userPrefs = context.getSharedPreferences("user", 0);
        isLogOn = userPrefs.getBoolean("user_options_enginePlay_logOn", false);

        if (processName != null)
            initProcess(processName);

    }

    public void initProcess(String processName)
    {

//Log.i(TAG, "1 initProcess(), processName: " + processName);

        if (process != null)
            destroyProcess();
        process = null;
        reader = null;
        writer = null;
        errorMessage = "";
        engineProcess = processName;
        startEngine();

//		Log.i(TAG, "2 initProcess(), processName: " + processName + ", isInitOk: " +isInitOk);

    }

    public void initPv()
    {
        infoPv = new ArrayList<>();
        infoMessage = new ArrayList<>();
        for (int i = 0; i < multiPV; i++)
        {
            infoPv.add("");
            infoMessage.add("");
        }
        statPv = new ArrayList<>();
        pvValuesChanged = false;
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

    synchronized boolean readUCIOptions()
    {
        long startTime = System.currentTimeMillis();
        long checkTime = startTime;
        isUciPonder = false;
        uciOptions = "";
        StringBuilder strB = new StringBuilder();

//        Log.i(TAG, "readUCIOptions(), uciOptions: ");

        while (checkTime - startTime <= MAX_UCI_TIME)
        {
			if (Thread.currentThread().isInterrupted())
			{

				if (isLogOn)
					Log.i(TAG, "readUCIOptions(), Thread.currentThread().isInterrupted()");

				return false;
			}
            CharSequence s = readLineFromProcess();
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
                        catch (NumberFormatException ignored) {  }
                    }
                }
                if (eloMin != 0 && eloMax != 0) {
                    SharedPreferences.Editor ed = userPrefs.edit();
                    ed.putInt("uci_elo_min", eloMin);
                    ed.putInt("uci_elo_max", eloMax);
                    ed.apply();
                }
            }

            if (s.toString().contains("option name")) {
                strB.append(s);
                strB.append("\n");
            }
            if (s.toString().contains("option") & s.toString().contains("Ponder"))
                isUciPonder = true;
            CharSequence[] tokens = tokenize(s);
            if (tokens[0].equals("uciok")) {

//                if (isLogOn)
//                    Log.i(TAG, "readUCIOptions(), uciok");

                if (strB.length() > 0)
                    uciOptions = strB.toString();

                return true;
            }

            checkTime = System.currentTimeMillis();

        }

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

    public String[] tokenize(String cmdLine)
    {
        cmdLine = cmdLine.trim();
        return cmdLine.split("\\s+");
    }

    public synchronized void stopSearch(EngineState eState)
    {

        if (process == null)
            return;

        engineState = eState;

//        Log.i(TAG, "2 stopSearch(), engineState: " + engineState);

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

            CharSequence s = readLineFromProcess();

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

    public void newGame(Boolean isUciNewGame)
    {
        if (uciOptions.contains("UCI_Chess960"))
            writeLineToProcess("setoption name UCI_Chess960 value " + isChess960);
        if (isUciNewGame)
            writeLineToProcess("ucinewgame");
    }

    public boolean applyPonderhit(String currMove, String currFen)
    {

//        Log.i(TAG, engineName + ": applyPonderhit(), uciEngineName: " + uciEngineName + ", engineState: " + engineState + ", ponderMove: " + ponderMove + ", currMove: " + currMove);

        if (engineState == EngineState.PONDER && !ponderMove.equals("") && currMove.equals(ponderMove)) {

//            Log.i(TAG, engineName + ": applyPonderhit(), currMove: " + currMove + ", currFen: " + currFen);

            ponderMove = "";
            ponderFen = currFen;
            searchRequest.fen = currFen;
            engineState = EngineState.SEARCH;
            writeLineToProcess("ponderhit");
            return false;
        }
        else
            return true;
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

//                        Log.i(TAG,  engineName + ": parseInfoCmd(), engineId: " + engineId + ", engineState: " + engineState + ", ponderMove: " + ponderMove + ", tokens[i]: " + tokens[i]);

                        if (!ponderMove.equals("") && engineState == EngineState.PONDER)
                            statPv.add(ponderMove);
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
                if (is.equals("cp")) {
                    statPvScore = Integer.parseInt(tokens[i++].toString());
                    if (engineState == EngineState.PONDER)
                        statPvScore = statPvScore * -1;
                }
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
        catch (NumberFormatException | ArrayIndexOutOfBoundsException ignored) {}

        if (statPv.size() > 0)
        {
            if (infoHasPvValues)
            {
                statPvMoves = getMoves(statPv, infoPvMoveMax);

//                Log.i(TAG,  engineName + ": parseInfoCmd(), moves: " + statPvMoves + ",   engineState: " + engineState);

            }
        }

//Log.i(TAG,  "size, values?, score, moves: " + statPv.size() + ", " + infoHasPvValues + ", " + statPvScore + "\nstatPvMoves: " + statPvMoves);

    }

    public CharSequence getDisplayMoves(CharSequence moves, int cntMoves)
    {
        CharSequence displayMoves = "";
        StringBuilder strB = new StringBuilder();
        String[] split = moves.toString().split(" ");
        if (split.length >= 0) {
            for (int i = 0; i < split.length; i++) {
                if (i < cntMoves) {
                    strB.append(split[i]);
                    strB.append(" ");
                }
            }
            displayMoves = strB.toString();
        }

//        Log.i(TAG,  "getDisplayMoves(), displayMoves: " + displayMoves);

        return  displayMoves;
    }

    public CharSequence getMoves(ArrayList<CharSequence> statPv, int infoPvMoveMax)
    {
        CharSequence moves = "";
        StringBuilder strB = new StringBuilder();
        for (int i = 0; i < statPv.size(); i++)
        {
            if (i >= infoPvMoveMax)
                break;
            if (statPv.get(i).toString().length() < 4 | statPv.get(i).toString().length() > 5)
                break;
            if (i == statPv.size() -1 | i == infoPvMoveMax -1)
                strB.append(statPv.get(i));
            else {
                strB.append(statPv.get(i));
                strB.append(" ");
            }
        }

        if (strB.length() > 0)
            moves = strB.toString();

//        Log.i(TAG,  "getMoves(), moves: " + moves);

        return moves;
    }

    //karl shredder chess960 castling
//    public CharSequence convertCastlingRight(CharSequence fen, CharSequence startFen)	// using for chess960(castle rook's line instead of "KQkq)
//    {
//
////        Log.i(TAG,  "convertCastlingRight(), startFen: " + startFen);
////        Log.i(TAG,  "convertCastlingRight(), fen:      " + fen);
//
//        CharSequence convertFen = "";
//        CharSequence startLineBlack = "abcdefgh";
//        CharSequence startLineWhite = "ABCDEFGH";
//        char cast_K = ' ';
//        char cast_Q = ' ';
//        char cast_k = ' ';
//        char cast_q = ' ';
//        CharSequence castling = "";
//        boolean firstRook = true;
//        // start FEN
//        if (startFen.length() > 7)
//        {
//            CharSequence fenBaseLine = startFen.subSequence (0, 8);
//            for (int i = 0; i < 8; i++)
//            {
//                if (fenBaseLine.charAt(i) == 'r')
//                {
//                    if (firstRook)
//                    {
//                        firstRook = false;
//                        cast_Q = startLineWhite.charAt(i);
//                        cast_q = startLineBlack.charAt(i);
//                    }
//                    else
//                    {
//                        cast_K = startLineWhite.charAt(i);
//                        cast_k = startLineBlack.charAt(i);
//                    }
//                }
//            }
//        }
//        // current FEN
//        CharSequence[] tokens = tokenize(fen);
//        for (int i = 0; i < tokens[2].length(); i++)
//        {
//            if (tokens[2].charAt(i) == 'K')
//                castling = castling.toString() + cast_K;
//            if (tokens[2].charAt(i) == 'k')
//                castling = castling.toString() + cast_k;
//            if (tokens[2].charAt(i) == 'Q')
//                castling = castling.toString() + cast_Q;
//            if (tokens[2].charAt(i) == 'q')
//                castling = castling.toString() + cast_q;
//            if (tokens[2].charAt(i) == '-')
//                castling = "-";
//        }
//        tokens[2] = castling;
//        for (CharSequence token : tokens) convertFen = convertFen.toString() + token + " ";
//
////Log.i(TAG,  "FEN sta: " + startFen);
////Log.i(TAG,  "FEN fen: " + fen);
////Log.i(TAG,  "FEN new: " + convertFen);
//
//        return convertFen;
//    }

    public void setIsChess960(boolean chess960) {isChess960 = chess960;}

    public CharSequence getRandomFirstMove()
    {
        CharSequence move;
        Random r;
        r = new Random();
        move = firstMove[randomMove[r.nextInt(100)]];
        return move;
    }

    public synchronized final void shutDown()
    {	//quit the ChessEngine(Shut down process)

//        Log.i(TAG,  "shutDown(), engineId: " + engineId + ", uciEngineName: " + uciEngineName);

		engineState = EngineState.DEAD;
        writeLineToProcess("quit");
        processAlive = false;

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

    public String readLineFromProcess()
    {
        String message;
        try{message =  readFromProcess();}
        catch (IOException | NullPointerException e)
        {

//            Log.i(TAG,  "IOException, readLineFromProcess()");

            e.printStackTrace();
            engineName = "";
            processAlive = false;
            process = null;
            return "ERROR";
        }
        //            Log.i(TAG,  "NullPointerException, readLineFromProcess()");

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
        engineName = uciIdName.substring(8);
        uciEngineName = uciIdName.substring(8);
        if (uciIdName.startsWith("White(1): id name ")) {
            engineName = uciIdName.substring(18);
            uciEngineName = uciIdName.substring(18);
        }
        engineName = uciEngineName + engineNameElo;
    }

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

                if (isLogOn)
                    Log.i(TAG,  "setUciEloValues(), uciEloMin: " + uciEloMin + ", uciEloMax: " + uciEloMax);

            }
            catch 	(NumberFormatException ignored) { }
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
            catch 	(NumberFormatException ignored) { }
        }
    }

    void setUciMultiPV(int multiPV)
    {
        try
        {
            if (uciOptions.contains("MultiPV"))
                writeToProcess("setoption name MultiPV value " + multiPV + "\n");
            this.multiPV = multiPV;
        }
        catch (IOException e) {e.printStackTrace();}
    }

    void setUciHash()
    {
        try
        {
            if (uciOptions.contains("Hash"))
                writeToProcess("setoption name Hash value " + 16 + "\n");
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
            for (String s : split) {
                if (s.startsWith("setoption")) {
                    try {
                        if (isFilePathOK(s))
                            writeToProcess(s + "\n");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
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

    public final synchronized boolean engineStop() {
        return engineState.toString().startsWith("STOP");
    }

    public final synchronized boolean engineSearching() {
        switch (engineState) {
            case SEARCH:
            case PONDER:
            case ANALYZE:
            case BOOK:
                return true;
            default:
                return false;
        }
    }

    // OEX ENGINE METHODS           OEX ENGINE METHODS          OEX ENGINE METHODS          OEX ENGINE METHODS          OEX ENGINE METHODS
    boolean startProcess()
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

        // intern oex engine (rodent, stockfish)
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

    private void writeToProcess(String data) throws IOException
    {
        if (writer != null)
        {
            if (isLogOn)
                Log.i(TAG, "C4A->" + engineName + ": " + data);
            writer.write(data);
            writer.flush();
        }
    }

    private String readFromProcess() throws IOException
    {
        String line = null;
        if (reader != null && reader.ready())
        {
            line = reader.readLine();
        }
        return line;
    }

    void startEngine() {

//        Log.i(TAG,  "1 startEngine(), engineThreadID: " + engineThreadID);

        if (startProcess()) {
            Thread engineMonitor = new Thread(this::monitorLoop);
            engineMonitor.start();
            writeLineToProcess("uci");
            engineState = EngineState.READ_OPTIONS;
        }
        else {

            listener.reportEngineError(engineId, "startEngine(), startProcess() error, process: \n" + engineProcess);

        }
    }

//    private final static long guiUpdateInterval = 200;
    private final static long guiUpdateInterval = 400;
    private long lastGUIUpdate = 0;

    private void monitorLoop() {

        while (true) {

//            Log.i(TAG,  "1 monitorLoop(), engineId: " + engineId + ", timeout: " + timeout);

            if (Thread.currentThread().isInterrupted())
                return;

            String s = readLineFromProcess();

//            Log.i(TAG,  " monitorLoop(), s: " + s);

            long t0 = System.currentTimeMillis();

            while (s != null && !s.isEmpty()) {

                if (Thread.currentThread().isInterrupted())
                    return;

                processEngineOutput(s);

                s = readLineFromProcess();

                long t1 = System.currentTimeMillis();
                if (t1 - t0 >= 1000)
                    break;

//                Log.i(TAG,  "2 monitorLoop(), engineId: " + engineId + ", t1 - t0: " + (t1 - t0));

            }

            if ((s == null) || Thread.currentThread().isInterrupted())
                return;

            processEngineOutput(s);

            if (Thread.currentThread().isInterrupted())
                return;

        }

    }

    private synchronized void processEngineOutput(String s) {

//        Log.i(TAG,  "processEngineOutput(), engineState: " + engineState + ", s: " + s);

        if (Thread.currentThread().isInterrupted())
            return;

        if (engineState == EngineState.IDLE) {
            engineInfoString = "";
            return;
        }

        if (s.length() == 0)
            return;

//        if (isLogOn)
//            Log.i(TAG, engineName + ": " + s);

        if (s.startsWith("id name ") | s.startsWith("White(1): id name "))
            setChessEngineName(s);
        if (s.startsWith("option name ") & s.contains("UCI_Elo"))
        {
            isUciStrength = true;
            isUciEloOption = true;
            setUciEloValues(s);

            if (isLogOn)
                Log.i(TAG,  engineName + ": uciEloMin: " + uciEloMin + ", uciEloMax: " + uciEloMax);

        }

        String[] tokens = tokenize(s);
        String bestMove = "";
        String ponderMv = "";
        if (tokens.length > 1) {
            if (tokens[0].equals("bestmove")) {
                bestMove = tokens[1];
                if ((tokens.length >= 4) && (tokens[2].equals("ponder")))
                    ponderMv = tokens[3];
            }
        }

//        Log.i(TAG,  "processEngineOutput(), engineState: " + engineState + ", bestMove: " + bestMove + ", ponderMv: " + ponderMv);

        switch (engineState) {
            case READ_OPTIONS: {
                if (readUCIOptions()) {
                    writeLineToProcess("ucinewgame");
                    writeLineToProcess("isready");
                    engineState = EngineState.WAIT_READY;
                }
                break;
            }
            case WAIT_READY: {
                if ("readyok".equals(s)) {

                    engineInfoString = "";
                    engineState = EngineState.IDLE;

                    if (isLogOn)
                        Log.i(TAG, engineName  + " (" + engineId + ") initialized");

                    listener.notifyEngineInitialized(engineId);

                }
                break;
            }
            case SEARCH:
            case PONDER:
            case ANALYZE:
            case BOOK:{

//                Log.i(TAG,  "processEngineOutput(), SEARCH, PONDER, ANALYZE, bestMove: " + bestMove);
//                Log.i(TAG,  engineName + ": processEngineOutput(), engineState: " + engineState + ", bestMove: " + bestMove + ", ponderMv: " + ponderMv + ", tokens[0]: " + tokens[0]);

                if (!bestMove.equals("")) {
                    if (engineState == EngineState.SEARCH) {
                        ponderMove = ponderMv;
                    }
                    else
                        ponderMv = "";
                    if (engineState != EngineState.ANALYZE)
                        engineState = EngineState.IDLE;
                    String newFen = searchRequest.fen;
                    if (!ponderFen.equals(""))
                        newFen = ponderFen;
                    listener.notifySearchResult(searchRequest.engineId, searchRequest.searchId, newFen, bestMove, ponderMv);
                }
                else {
                    boolean isPV = false;
                    if (tokens[0].equals("info")) {
                        if (userPrefs.getBoolean("user_options_enginePlay_debugInformation", false)) {
                            if (s.contains("info string") && s.length() > 14 && !engineInfoString.toString().contains(s.subSequence(12, s.length() - 1)))
                                engineInfoString = engineInfoString + "" + s.subSequence(12, s.length() - 1) + "\n";
                        }
                        else
                            engineInfoString = "";
                        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
                            parseInfoCmd(tokens, userPrefs.getInt("user_options_enginePlay_PvMoves", Settings.MOVES_DEFAULT));
                        else
                            parseInfoCmd(tokens, userPrefs.getInt("user_options_enginePlay_PvMoves_land", Settings.MOVES_DEFAULT_LAND));
                        engineStat = getInfoStat(statCurrDepth, statCurrSelDepth, statCurrMoveNr, statCurrMoveCnt, statCurrNodes, statCurrMove, searchRequest.fen);

//					Log.i(TAG, "engineStat: " + engineStat);

                        if (!s.contains("multipv") & s.contains(" pv "))
                        {
                            statCurrMoveNr = 0;
                            statCurrMoveCnt = 0;
                            engineMes = getInfoPv(0, statPvMoves, statPvScore, statIsMate, searchRequest.fen);
                            isPV = true;
                        }
                        if (s.contains("multipv") && !s.contains("bound nodes"))		// !upperbound, !lowerbound
                        {
                            try
                            {
                                if (statPvIdx == 0)
                                    multiPvCnt++;
                                int pvNr = statPvIdx +1;
                                if 	(multiPV == pvNr)
                                    isPV = true;

//							Log.i(TAG, "ec.getEngine().statPvIdx: " + ec.getEngine().statPvIdx);

                                engineMes = getInfoPv(statPvIdx, statPvMoves, statPvScore, statIsMate, searchRequest.fen);

//                                Log.i(TAG,  engineName + ": processEngineOutput(), engineState: " + engineState + ", isPV: " + isPV + ", engineMes: " + engineMes);

                            }
                            catch (NullPointerException e) {e.printStackTrace(); engineMes = "";}
                        }
                        else
                        {
                            if (statPvIdx == 0) {
                                bestScore = getBestScore(statPvScore, searchRequest.fen);
                            }
                        }

//					Log.i(TAG, "ec.getEngine().statPvAction: >" + ec.getEngine().statPvAction + "<, infoShowPv: " + infoShowPv
//						+ ", engineMes: " + engineMes + ", engineStat: " + engineStat + "\ns: " + s);

                    }

//                    Log.i(TAG,  engineName + ": processEngineOutput(), engineState: " + engineState + ", isPV: " + isPV + ", engineMes: " + engineMes);

                    if (!engineStat.equals("") && (isPV || s.contains(" mate ")))
                    {
                        String engineMessage = "" + engineStat + engineMes + engineInfoString;
                        notifyGUI(searchRequest.engineId, searchRequest.searchId, engineMessage,  searchDisplayMoves.toString());
                    }

                }
                break;
            }

            case STOP_IDLE:
            case STOP_MOVE:
            case STOP_MOVE_CONTINUE:
            case STOP_CONTINUE:
            case STOP_NEW_GAME:
            case STOP_MULTI_ENGINES_RESTART:
            case STOP_QUIT_RESTART:
            case STOP_QUIT:
                {

                if (!bestMove.equals("")) {

//                    Log.i(TAG,  "processEngineOutput(), engineState: " + engineState + ", sr ponderMove: " + searchRequest.ponderMove + ", sr ponderEnabled: " + searchRequest.ponderEnabled);

                    String engineMessage = "" + engineStat + engineMes + engineInfoString;
                    notifyGUI(searchRequest.engineId, searchRequest.searchId, engineMessage, searchDisplayMoves.toString());
                    if (searchRequest.ponderEnabled && !searchRequest.ponderMove.equals(""))
                        listener.notifyStop(searchRequest.engineId, searchRequest.searchId, engineState, searchRequest.fen, searchRequest.ponderMove);
                    else
                        listener.notifyStop(searchRequest.engineId, searchRequest.searchId, engineState, searchRequest.fen, bestMove);
                    if (engineState != EngineState.STOP_MULTI_ENGINES_RESTART)
                        engineState = EngineState.IDLE;

                }

                break;
            }
            default: {

                if (isLogOn)
                    Log.i(TAG,  engineName + ": processEngineOutput(), engineState ??? : " + engineState);

                break;
            }
        }
    }

    private synchronized void notifyGUI(int engineId, int id, String engineMessage, String searchDisplayMoves) {

        long now = System.currentTimeMillis();

//        Log.i(TAG, engineName + ": notifyGUI(), now: " + now + ", lastGUIUpdate: " + lastGUIUpdate + ", guiUpdateInterval: " + guiUpdateInterval);

        if (now < lastGUIUpdate + guiUpdateInterval)
            return;

        listener.notifyPV(engineId, id, engineMessage, searchDisplayMoves);

        lastGUIUpdate = now;

    }

    CharSequence getInfoStat(int depth, int selDepth, int moveNumber, int moveNumberCnt, int nodes, CharSequence move, CharSequence fen)
    {
        CharSequence infoStat;
        if (engineState != EngineState.PONDER)
            infoStat = uciEngineName;
        else
            infoStat = "[" + uciEngineName + "]";
        CharSequence notation = cl.getNotationFromInfoPv(fen, move);
        notation = cl.history.getAlgebraicNotation(notation, userPrefs.getInt("user_options_gui_PieceNameId", 0));
        int nodesK = nodes / 1000;
        String moveInfo = "";
        if (moveNumberCnt > 0)
            moveInfo = + moveNumber + "(" + moveNumberCnt + "): ";
        infoStat = infoStat + ":  " + moveInfo + notation + "  d:" + depth + "/" + selDepth + "  n:" + nodesK + "k\n";
        return infoStat;
    }

    CharSequence getInfoPv(int statPvIdx, CharSequence statPvMoves, int statPvScore, boolean isMate, CharSequence fen)
    {
        sbInfo.setLength(0);

//        Log.i(TAG, engineName + ": getInfoPv(), statPvIdx: " + statPvIdx + ", infoPv.size(): " + infoPv.size()  + ", multiPV: " + multiPV + ", statPvScore: " + statPvScore + ", statPvMoves: " + statPvMoves + ", fen: " + fen);
        
        if 	(	infoPv.size() 	== 	multiPV
                & statPvIdx 	< 	multiPV
        )
        {
            infoPv.set(statPvIdx, statPvMoves);
            if (statPvIdx == 0) {

//					Log.i(TAG, "getInfoPv, statPvIdx: " + statPvIdx + ", infoPv.size(): " + infoPv.size() + ", statPvScore: " + statPvScore + ", statPvMoves: " + statPvMoves);

                searchDisplayMoves = getDisplayMoves(statPvMoves, userPrefs.getInt("user_options_gui_arrows", Settings.ARROWS_DEFAULT));
                bestScore = getBestScore(statPvScore, fen);
            }
            CharSequence displayScore = getDisplayScore(statPvScore, fen);
            if (isMate & statPvScore > 0)
                displayScore = "M" + statPvScore;
            sbMoves.setLength(0); sbMoves.append("*"); sbMoves.append((statPvIdx +1)); sbMoves.append("(");
            CharSequence notation = cl.getNotationFromInfoPv(fen, statPvMoves);

//            Log.i(TAG, engineName + ": getInfoPv(), notation: " + notation);

            if (notation.equals(""))
                return "";
            notation = cl.history.getAlgebraicNotation(notation, userPrefs.getInt("user_options_gui_PieceNameId", 0));
            sbMoves.append(displayScore); sbMoves.append(") "); sbMoves.append(notation);

//				Log.i(TAG, "taskFen: " + taskFen);
//				Log.i(TAG, "statPvMoves: " + statPvMoves);
//				Log.i(TAG, "notation: "  + notation);
//				Log.i(TAG, "sbMoves: "  + sbMoves);

            infoMessage.set(statPvIdx, sbMoves.toString());
        }
        for (int i = 0; i < infoMessage.size(); i++)
        {
            if (!infoMessage.get(i).toString().equals(""))
            {
                sbInfo.append(infoMessage.get(i));
                sbInfo.append("\n");
            }
        }
        return sbInfo.toString();
    }

    int getBestScore(int score, CharSequence fen)
    {
        char color = 'w';
        CharSequence[] fenSplit = fen.toString().split(" ");
        if (fenSplit.length >= 0)
        {
            if (fenSplit[1].equals("b"))
                color = 'b';
        }
        if (color == 'b')
            score =  score * -1;

//			Log.i(TAG, "getBestScore: " + score);

        return score;
    }

    public CharSequence getDisplayScore(int score, CharSequence fen)
    {
        char color = 'w';
        CharSequence[] fenSplit = fen.toString().split(" ");
        if (fenSplit.length >= 0)
        {
            if (fenSplit[1].equals("b"))
                color = 'b';
        }
        int s1 = score / 100;
        int s2 = score % 100;
        CharSequence s = "";		// score
        if (s1 < 0)	s1 = s1 * -1;
        if (s2 < 0)	s2 = s2 * -1;
        if (color == 'w')
        {
            if (score < 0)
                s = s + "-";
            else
                s = s + "+";
        }
        else
        {
            if (score < 0)
                s = s + "+";
            else
                s = s + "-";
        }
        if (s2 < 10)
            s = s + Integer.toString(s1) + ".0" + s2;
        else
            s = s + Integer.toString(s1) + "." + s2;
        return s;
    }

    public static final class SearchRequest {
        int engineId;           // Unique engine identifier --> GUI
        int searchId;           // Unique identifier for this search request
        long startTime;         // System time (milliseconds) when search request was created

        String fen;             // current fen
        String moves;           // Moves after prevPos (EMPTY)
        String startFen;        // initial fen (for Chess960)
        int chess960Id;         // 0...959

        boolean drawOffer;      // True if other side made draw offer
        boolean isSearch;       // True if regular search or ponder search
        boolean isAnalysis;     // True if analysis search
        String firstMove = "";  // auto first move (engine: white)
        String bookMove = "";   // play book move if != ""

        int wTime;              // White remaining time, milliseconds
        int bTime;              // Black remaining time, milliseconds
        int wInc;               // White time increment per move, milliseconds
        int bInc;               // Black time increment per move, milliseconds
        int movesToGo;          // Number of moves to next time control
        int moveTime;           // time for one move

        String engine;          // Engine name (identifier)
        int elo;                // Engine UCI_Elo setting, or Integer.MAX_VALUE for full strength
        int numPV;              // Number of PV lines to compute

        boolean ponderEnabled;  // True if pondering enabled, for engine time management
        String ponderMove;      // Ponder move, or null if not a ponder search

        public static SearchRequest searchRequest(int engineId, int id, long now,
                                                  String fen, String moves, String startFen, int chess960Id, String firstMove, String bookMove,
                                                  boolean drawOffer, boolean isSearch, boolean isAnalysis,
                                                  int wTime, int bTime, int wInc, int bInc, int movesToGo, int moveTime,
                                                  boolean ponderEnabled, String ponderMove,
                                                  String engine, int elo)
        {
            SearchRequest sr = new SearchRequest();
            sr.engineId = engineId;
            sr.searchId = id;
            sr.startTime = now;
            sr.firstMove = firstMove;
            sr.bookMove = bookMove;
            sr.fen = fen;
            sr.moves = moves;
            sr.startFen = startFen;
            sr.chess960Id = chess960Id;
            sr.drawOffer = drawOffer;
            sr.isSearch = isSearch;
            sr.isAnalysis = isAnalysis;
            sr.wTime = wTime;
            sr.bTime = bTime;
            sr.wInc = wInc;
            sr.bInc = bInc;
            sr.movesToGo = movesToGo;
            sr.moveTime = moveTime;
            sr.engine = engine;
            sr.elo = elo;
            sr.numPV = 1;
            sr.ponderEnabled = ponderEnabled;
            sr.ponderMove = ponderMove;
            return sr;
        }

    }

    void startSearch(SearchRequest sr) {

        if (sr == null)
            return;

        if (!sr.isSearch && !sr.isAnalysis) {
            return;
        }

        searchRequest = sr;
        startSearchId = sr.searchId;

//        Log.i(TAG, "startSearch(), sr.chess960Id: " + sr.chess960Id + ", sr.fen: " + sr.fen);

        cl.newPosition(Integer.toString(sr.chess960Id), sr.fen, "", "", "", "", "", "");

        //karl??? 960; --> KQkq  ? A..G a..g ?
        //karl shredder chess960 castling;
//        if (isChess960)
//            sr.fen = (String) convertCastlingRight(sr.fen, sr.startFen);

        if (sr.isAnalysis) {
            if (uciOptions.contains("UCI_AnalyseMode"))
                writeLineToProcess("setoption name UCI_AnalyseMode value true");
            setElo(false, uciEloMax);
        }
        else {
            if (uciOptions.contains("UCI_AnalyseMode"))
                writeLineToProcess("setoption name UCI_AnalyseMode value false");

            if (withUciElo)
                setElo(true, uciElo);
            else
                setElo(false, uciEloMax);
        }

        String posStr = "";
        posStr = posStr + "position fen ";								// position(FEN)
        posStr = posStr + sr.fen;
        if (sr.ponderEnabled && !sr.ponderMove.equals(""))
            posStr = posStr + " moves " + sr.ponderMove;				// + move
        if (!sr.moves.equals(""))
            posStr = posStr + " moves " + sr.moves;						// + move
        writeLineToProcess(posStr);										// writeLineToProcess
        String goStr = "";
        goStr = goStr + "go ";
        engineState = EngineState.SEARCH;
        if (sr.ponderEnabled && !sr.ponderMove.equals(""))
        {
            engineState = EngineState.PONDER;
            goStr = goStr + " ponder ";
        }

        if (sr.isAnalysis)   // go analyze
        {
            engineState = EngineState.ANALYZE;
            goStr = goStr + " infinite ";                                // search until the "stop" command
        }
        else                // go search
        {
            if (sr.moveTime > 0)
                goStr = goStr + " movetime  " + sr.moveTime;		// movetime
            else
            {
                goStr = goStr + " wtime " + sr.wTime + " btime " + sr.bTime;	// wtime + btime + winc + binc + movestogo
                goStr = goStr + " winc " + sr.wInc + " binc " + sr.bInc;
                if (sr.movesToGo > 0)
                    goStr = goStr + " movestogo " + sr.movesToGo;
            }
        }

        writeLineToProcess(goStr);										// writeLineToProcess

        if (engineState != EngineState.PONDER) {
            ponderMove = "";
            ponderFen = "";
        }

    }

    final String TAG = "UciEngine";
    static final int UCI_ELO_STANDARD = 3000;
    final long MAX_UCI_TIME = 2000;
    final long MAX_ISREADY_TIME = 6000;

    Context context;
    ChessLogic cl;				                            // direct access to ChessLogic, Chess960, ChessHistory
    public int engineId;                                    // idx from EngineControl.UciEngine[] uciEngines
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
    String errorMessage = "";

    ProcessBuilder processBuilder;
    public Process process;
    private BufferedReader reader = null;
    private BufferedWriter writer = null;
    private SearchRequest searchRequest = null;
    private final EngineListener listener;
    String ponderMove = "";
    String ponderFen = "";

    int startSearchId = 0;

	public enum EngineState {
		READ_OPTIONS,               // "uci" command sent, waiting for "option" and "uciok" response.
		WAIT_READY,                 // "isready" sent, waiting for "readyok".
		IDLE,                       // engine not searching.
		SEARCH,                     // "go" sent, waiting for "bestmove"
		PONDER,                     // "go" sent, waiting for "bestmove"
		ANALYZE,                    // "go" sent, waiting for "bestmove" (which will be ignored)
		BOOK,                       // book move
		STOP_IDLE,   	            // "stop" sent, ignore "bestmove", and set to IDLE
		STOP_MOVE,   	            // "stop" sent, waiting for "bestmove", and make bestmove
		STOP_MOVE_CONTINUE,         // "stop" sent, waiting for "bestmove", and make bestmove, and continue with next "search" (continueFen)
        STOP_CONTINUE,              // "stop" sent, ignore "bestmove", continue with next "search" (continueFen)
        STOP_NEW_GAME,              // "stop" sent, ignore "bestmove", start new game
        STOP_MULTI_ENGINES_RESTART, // "stop" / "quit" all engines and restart engines
        STOP_QUIT_RESTART,          // "stop" sent and quit and restart an engine
        STOP_QUIT,                  // "stop" sent and quit
		DEAD,                       // engine process has terminated
	}
	public EngineState engineState = EngineState.DEAD;

    boolean processAlive;
    boolean isChess960 = false;
    boolean startPlay = false;

    String uciOptions = "";
    int multiPV = 1;

    boolean isUciStrength = false;
    boolean isUciEloOption = false;
    boolean isUciSkillOption = false;
    boolean isUciPonder = false;
    Boolean withUciElo = false;

    int uciEloMin = 800;
    int uciEloMax = UCI_ELO_STANDARD;
    int uciElo = UCI_ELO_STANDARD;
    int uciSkillLevelMin = 1;
    int uciSkillLevelMax = 100;

    boolean engineWithMultiPv = false;

    boolean pvValuesChanged = false;
    ArrayList<CharSequence> statPv = new ArrayList<>();
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

    CharSequence engineMes = "";
    CharSequence engineStat = "";
    CharSequence engineInfoString= "";
    ArrayList<CharSequence> infoPv;
    ArrayList<CharSequence> infoMessage;
    StringBuilder sbMoves = new StringBuilder(100);
    StringBuilder sbInfo = new StringBuilder(100);
    CharSequence searchDisplayMoves = "";
    int bestScore = 0;
    int multiPvCnt = 0;

    final CharSequence[] firstMove =	{	"a2a3", "a2a4", "b2b3", "b2b4",
            "c2c3", "c2c4", "d2d3", "d2d4",
            "e2e3", "e2e4", "f2f3", "f2f4",
            "g2g3", "g2g4", "h2h3", "h2h4",
            "b1a3", "b1c3", "g1f3", "g1h3"};
    final int[] randomMove =	{	0, 1, 2, 2,	2, 3, 3, 3, 4, 4,
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
