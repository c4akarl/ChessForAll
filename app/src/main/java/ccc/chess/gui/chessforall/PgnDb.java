package ccc.chess.gui.chessforall;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Random;
//import android.content.ContentValues;
//import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
//import android.util.Log;

public class PgnDb 
{
//	public PgnDb(Context c) 
//	{	//	Constructor
//		context = c;
//	}
	// .pgn 	- using RAF (Random Access File)
	public String pgnDataFromRaf(String pgnPath, String pgnFile, long gameOffset, int gameLength)
    {
		String data = "";
		String searchFile = pgnPath + pgnFile;
		File f = new File(searchFile);
		if (!f.exists())
			return "";
		RandomAccessFile raf;
		int readedBytes = 0;
		byte[] buffer = new byte[gameLength];
		try 
		{
			raf = new RandomAccessFile(f, "r");
			raf.seek(gameOffset);
			readedBytes = raf.read (buffer, 0, gameLength);
			raf.close();
			if (buffer != null & readedBytes > 0 & readedBytes <= gameLength)
			{
//				Log.i(TAG, "seekStart, gameLength, readedBytes,  + buffer.length: " + gameOffset + ", " + gameLength + ", " + readedBytes  + ", " + buffer.length);
				data = new String(buffer);
//				Log.i(TAG, "file(length): " + searchFile + "(" + rafLength + ")");
//				Log.i(TAG, "seekStart, gameLength, readedBytes: " + gameOffset + ", " + gameLength + ", " + readedBytes);
//				Log.i(TAG, "\n" + data);
				return data;
			}
			else
			{
//				Log.i(TAG, "buffer, readedBytes: " + buffer + ", " + readedBytes);
				return "";
			}
		} 
		catch (FileNotFoundException e) {e.printStackTrace(); return "";}
		catch (IOException  e) {e.printStackTrace(); return "";}
		catch (IndexOutOfBoundsException  e) {e.printStackTrace(); return "";}
    }
	
	// .pgn-db (sqlite databse)			.pgn-db (sqlite databse)			.pgn-db (sqlite databse) 
	public boolean initPgnFiles(String pgnPath, String pgnFile)
    {	// setting the file values and return true if the files ".pgn" and ".pgn-db" exists	
		this.pgnPath = ""; 
		this.pgnFile = "";
		this.pgnDbFile = "";
		File fPgn = new File(pgnPath + pgnFile);
		if (!fPgn.exists())
			return false;
		String pgnDbFile = pgnFile.replace(".pgn", ".pgn-db");
		this.pgnPath = pgnPath; 
		this.pgnFile = pgnFile;
		pgnLength = fPgn.length();
		this.pgnDbFile = pgnDbFile; 
		File fPgnDb = new File(pgnPath + pgnDbFile);
		if (fPgnDb.exists())
			return true;
		else
			return false;
    }
	public void deleteDbFile()
    {
		File fPgnDb = new File(pgnPath + pgnDbFile);
		if (fPgnDb.exists() & pgnDbFile.endsWith(".pgn-db"))
			fPgnDb.delete();
    }
	public boolean openDb(String path, String file, int flags)
    {
		if (db == null)
			initPgnFiles(path, file);
		try 
		{
			db = SQLiteDatabase.openDatabase(pgnPath + pgnDbFile, null, flags);
//			Log.i(TAG, "db.getVersion(): "+ pgnPath + pgnDbFile + ", " + db.getVersion());
			return db.isOpen();
		} 
		catch (SQLiteException e) {e.printStackTrace(); return false;}	// database open error
    }
	public void closeDb()
    {
		try		{db.close();}
		catch 	(SQLiteException e) {e.printStackTrace();}	// database close error
    }
	public boolean dbToLarge(String pgnPath, String pgnFile)
    {
		boolean dbToLarge = false;
		File fPgn = new File(pgnPath + pgnFile);
		if (fPgn.exists())
		{
//			Log.i(TAG, "pgn.length(): " + fPgn.length());
			if (fPgn.length() > MAX_AUTO_CREATE_LENGTH)
			{
				String pgnDbFile = pgnFile.replace(".pgn", ".pgn-db");
				File fPgnDb = new File(pgnPath + pgnDbFile);
				if (!fPgnDb.exists())
					dbToLarge = true;
			}
		}
		return dbToLarge;
    }
	public void initGameValues()
    {
		Event = "";
		Site = "";
		Date = "";
		Round = "";
		White = "";
		Black = "";
		Result = "";
		SetUp = "";
		FEN = "";
		WhiteTitle = "";
		BlackTitle = "";
		WhiteElo = "";
		BlackElo = "";
		ECO = "";
		Opening = "";
		Variation = "";
		WhiteTeam = "";
		BlackTeam  = "";
		WhiteFideId = "";
		BlackFideId = "";
		EventDate = "";
		EventType  = "";
    }
	public void setTagValue(String lineValue)
    {
		String tagName = "";
		String tagValue = "";
		int startValues = lineValue.indexOf('"') +1;
		if (startValues > 1 & startValues < lineValue.length())
		{
			for (int i = startValues; i < lineValue.length(); i++)
		    {
				if (lineValue.charAt(i) == '"' & lineValue.charAt(i +1) == ']')
					break;
				tagValue = tagValue + lineValue.charAt(i);
		    }
		}
		String[] txtSplit = lineValue.split(" ");
		if (txtSplit.length > 0)
			tagName =  txtSplit[0].replace("[", "");
//		Log.i(TAG, "tagName, tagValue: " + tagName + ", " + tagValue);
		if (tagName.equals("Event")) 		Event = tagValue;
		if (tagName.equals("Site")) 		Site = tagValue;
		if (tagName.equals("Date")) 		Date = tagValue;
		if (tagName.equals("Round")) 		Round = tagValue;
		if (tagName.equals("White")) 		White = tagValue;
		if (tagName.equals("Black")) 		Black = tagValue;
		if (tagName.equals("Result")) 		Result = tagValue;
		if (tagName.equals("SetUp")) 		SetUp = tagValue;
		if (tagName.equals("FEN")) 			FEN = tagValue;
		if (tagName.equals("WhiteTitle")) 	WhiteTitle = tagValue;
		if (tagName.equals("BlackTitle")) 	BlackTitle = tagValue;
		if (tagName.equals("WhiteElo")) 	WhiteElo = tagValue;
		if (tagName.equals("BlackElo")) 	BlackElo = tagValue;
		if (tagName.equals("ECO")) 			ECO = tagValue;
		if (tagName.equals("Opening")) 		Opening = tagValue;
		if (tagName.equals("Variation")) 	Variation = tagValue;
		if (tagName.equals("WhiteTeam")) 	WhiteTeam = tagValue;
		if (tagName.equals("BlackTeam")) 	BlackTeam = tagValue;
		if (tagName.equals("WhiteFideId")) 	WhiteFideId = tagValue;
		if (tagName.equals("BlackFideId")) 	BlackFideId = tagValue;
		if (tagName.equals("EventDate")) 	EventDate = tagValue;
		if (tagName.equals("EventType")) 	EventType = tagValue;
		
    }
	public void setGameOffsets(long gameFileOffset, int gameLength, int gameMovesOffset)
    {
		GameFileOffset = gameFileOffset;
		GameLength = gameLength;
		GameMovesOffset = gameMovesOffset;
    }
//	public void insertValuesToDb()
//    {
//		ContentValues cv = new ContentValues();
//		cv.put("GameFileOffset", GameFileOffset);
//		cv.put("GameLength", GameLength);
//		cv.put("GameMovesOffset", GameMovesOffset);
//		cv.put("Event", Event);
//		cv.put("Site", Site);
//		cv.put("Date", Date);
//		cv.put("Round", Round);
//		cv.put("White", White);
//		cv.put("Black", Black);
//		cv.put("Result", Result);
//		cv.put("SetUp", SetUp);
//		cv.put("FEN", FEN);
//		cv.put("WhiteTitle", WhiteTitle);
//		cv.put("BlackTitle", BlackTitle);
//		cv.put("WhiteElo", WhiteElo);
//		cv.put("BlackElo", BlackElo);
//		cv.put("ECO", ECO);
//		cv.put("Opening", Opening);
//		cv.put("Variation", Variation);
//		cv.put("WhiteTeam", WhiteTeam);
//		cv.put("BlackTeam", BlackTeam);
//		cv.put("WhiteFideId", WhiteFideId);
//		cv.put("BlackFideId", BlackFideId);
//		cv.put("EventDate", EventDate);
//		cv.put("EventType", EventType);
//		db.insertOrThrow(TABLE_NAME, null, cv);
//    }
	
	public Cursor queryPgn(int gameId, int maxCursorRow, boolean isDesc)
    {	// query games from db(MAX_CURSOR_ROWS) and put the result to Cursor(return)
		if (db != null)
		{
//			Log.i(TAG, "gameId: " + gameId);
			int rowCnt = getRowCount(PgnDb.TABLE_NAME);
			int fromRow = 1;
			int toRow = getRowCount(PgnDb.TABLE_NAME);
			String query = PGN_QUERY;
			String qOrder = " ORDER BY _id ";
			if (isDesc)
				qOrder = qOrder + "DESC ";
			if (isDesc)
				scrollGameId = toRow - gameId + 1;
			else
				scrollGameId = gameId;
//			Log.i(TAG, "1, gameId, maxCursorRow, isDesc, fromRow, toRow, rowCnt, scrollGameId: \n" 
//				+ gameId + ", " + maxCursorRow + ", " + isDesc + ", " + fromRow + ", " + toRow + ", " + rowCnt + ", " + scrollGameId);
			if (toRow > maxCursorRow)
			{
				if (isDesc)
				{
					toRow = gameId + (maxCursorRow / 2);
					if (toRow > rowCnt)
						toRow = rowCnt;
					fromRow = toRow - maxCursorRow;
					if (fromRow < 1)
						fromRow = 1;
					scrollGameId = toRow - gameId + 1;
				}
				else
				{
					if (gameId <= (maxCursorRow / 2))
						toRow = maxCursorRow;
					else
					{
						scrollGameId = (maxCursorRow / 2) + 1;
						fromRow = gameId - (maxCursorRow / 2);
						toRow = fromRow + maxCursorRow;
					}
				}
//				Log.i(TAG, "2, gameId, fromRow, toRow, scrollGameId: " + gameId + ", " + fromRow + ", " + toRow + ", " + scrollGameId);
				query = PGN_QUERY + "WHERE _id >= " + fromRow + " AND _id <= " + toRow;
			}
			pgnC = db.rawQuery(query +qOrder, null);
			if (pgnC != null)
				pgnC.moveToFirst();
			return pgnC;
		}
		else
			return null;
    }
	public Cursor queryPgnIdxPlayer(String player, int color, String dateFrom, String dateTo, boolean dateDesc, String event, String site)
    {	// select all games from player(para 1) and put the result to Cursor(return)
		// control(para 2): 0=White, 1=Black, 2=White and Black
		if (dateTo.equals(""))
			dateTo = "9999.99.99";
		player = player.replace("'", "''");
		player = player.toLowerCase() + "%";
		event = event.replace("'", "''");
		event = event.toLowerCase() + "%";
		site = site.replace("'", "''");
		site = site.toLowerCase() + "%";
		if (db != null)
		{
			String qWhere = "";
			String qEvent = "";
			String qSite = 	"";
			String qDate = 	" AND Date BETWEEN '" +dateFrom +"' AND '" +dateTo +"' ";
			qEvent = " AND LOWER(Event) LIKE '" +event +"' ";
			qSite = " AND LOWER(Site) LIKE '" +site +"' ";
			String qOrder = "ORDER BY Date ";
			if (dateDesc)
				qOrder = qOrder + "DESC ";
			switch (color)
	        {
	        	case 0: qWhere = "WHERE LOWER(White) LIKE '" +player +"'" +qDate +qEvent +qSite;
	        			break;	// 0=White
	        	case 1: qWhere = "WHERE LOWER(Black) LIKE '" +player +"'" +qDate +qEvent +qSite;
	        			break;	// 1=Black
	        	case 2:	qWhere = 	"WHERE LOWER(White) LIKE '" +player +"'" +qDate +qEvent +qSite 
	        						+" OR LOWER(Black) LIKE'" +player +"'" +qDate +qEvent +qSite;
		    			break; 	// 2=White and Black
	        }
			
//			explainQueryPlan("EXPLAIN QUERY PLAN " +PGN_QUERY +qWhere +qOrder); // test only
			pgnC = db.rawQuery(PGN_QUERY +qWhere +qOrder, null);
			if (pgnC != null)
				pgnC.moveToFirst();
			return pgnC;
		}
		else
			return null;
    }
	public Cursor queryPgnIdxEvent(String event, String site)
    {	// select all games from event, site
//		Log.i(TAG, "event, site: " + event + ", " + site);
		if (event.equals("") & site.equals(""))
			return null;
		event = event.replace("'", "''");
		event = event.toLowerCase() + "%";
		site = site.replace("'", "''");
		site = site.toLowerCase() + "%";
		if (db != null)
		{
			String qEvent = "";
			String qSite = 	"";
//			if (event.equals("%"))
//			{
//				qSite =  "WHERE LOWER(Site) LIKE '" +site +"' ";
//				pgnC = db.rawQuery(PGN_QUERY +qSite, null);
//			}
//			else
			{
				qEvent = "WHERE LOWER(Event) LIKE '" +event +"' ";
				qSite =  " AND LOWER(Site) LIKE '" +site +"' ";
				pgnC = db.rawQuery(PGN_QUERY +qEvent +qSite, null);
			}
			if (pgnC != null)
				pgnC.moveToFirst();
			return pgnC;
		}
		else
			return null;
    }
	public Cursor queryPgnIdxEco(String eco, String opening, String variation, String dateFrom, String dateTo, boolean dateDesc)
    {	// select all games from player(para 1) and put the result to Cursor(return)
		if (eco.equals("") & opening.equals(""))
			return null;
		if (dateFrom.equals(""))
			dateFrom = "0000.00.00";
		if (dateTo.equals(""))
			dateTo = "9999.99.99";
		eco = eco.replace("'", "''");
		eco = eco.toLowerCase() + "%";
		opening = opening.replace("'", "''");
		opening = opening.toLowerCase() + "%";
		variation = variation.replace("'", "''");
		variation = variation.toLowerCase() + "%";
		if (db != null)
		{
			String qEco = 		"WHERE LOWER(ECO) LIKE '" +eco +"' ";
			String qOpening = 	" AND LOWER(Opening) LIKE '" +opening +"' ";
			String qVariation = " AND LOWER(Variation) LIKE '" +variation +"' ";
			String qDate = 	" AND Date BETWEEN '" +dateFrom +"' AND '" +dateTo +"' ";
			String qOrder = "ORDER BY Date ";
			if (dateDesc)
				qOrder = qOrder + "DESC ";
//			explainQueryPlan("EXPLAIN QUERY PLAN " +PGN_QUERY +qEvent +qSite); // test only
			pgnC = db.rawQuery(PGN_QUERY +qEco +qOpening +qVariation +qDate +qOrder, null);
			if (pgnC != null)
				pgnC.moveToFirst();
			return pgnC;
		}
		else
			return null;
    }
	public void explainQueryPlan(String stm)
    {
		Cursor pgnC = db.rawQuery(stm, null);
		if(pgnC.moveToFirst()) 
		{
		    do 
		    {
		        StringBuilder sb = new StringBuilder();
		        for(int i = 0; i < pgnC.getColumnCount(); i++) 
		        {
		            sb.append(pgnC.getColumnName(i)).append(":").append(pgnC.getString(i)).append(", ");
		        }
//		        Log.i("EXPLAIN",sb.toString());
		    } 
		    while(pgnC.moveToNext());
		}
    }
	
	public int getGameId(int gamePos, int gameLoadControl)
    {
		int gameId = 0;
		pgnStat = "-";
		int gameCnt = getRowCount(TABLE_NAME);
		if (gameCnt > 0)
		{
			pgnStat = "X";
			switch (gameLoadControl)
	        {
	        	case 0: gameId = gamePos +1;	break;	// next
	        	case 1: gameId = 1;				break;	// first
		        case 7:     							// random
			        	Random r;
			        	r = new Random();
			        	gameId = r.nextInt(gameCnt);
			            break;
		        case 8: gameId = gamePos -1; 	break; 	// previous
		        case 9: gameId = gameCnt; 		break;	// last
		        case 10: gameId = gamePos; 		break;	// current
	        }
			if (gameId < 1) 		gameId = 1;
			if (gameId > gameCnt) 	gameId = gameCnt;
			if (gameId == 1) 		pgnStat = "F";
			if (gameId == gameCnt) 	pgnStat = "L";
		}
		return gameId;
    }
	public String getDataFromGameId(int gameId)
    {	// 	game data from pgn(_id)
//		Log.i(TAG, "getDataFromGameId()");
		String gameData = "";
		int gameCnt = getRowCount(TABLE_NAME);
		if (gameCnt > 0)
		{
			if (gameId > gameCnt) 	gameId = gameCnt;
			if (gameId < 1) 		gameId = 1;
			Cursor cur = db.rawQuery("SELECT GameFileOffset, GameLength FROM " + TABLE_NAME + " WHERE _id=" + gameId, null);
			cur.moveToFirst();
			long gameFileOffset = cur.getInt(cur.getColumnIndex("GameFileOffset"));
			int gameLength = cur.getInt(cur.getColumnIndex("GameLength"));
			gameData = pgnDataFromRaf(pgnPath, pgnFile, gameFileOffset, gameLength);
			if (!gameData.equals(""))
			{
				pgnGameId = gameId;
				pgnGameCount = gameCnt;
				pgnGameOffset = gameFileOffset;
			}
//			Log.i(TAG, "games, gameId, gameFileOffset, gameLength: " + gameCnt + ", " + gameId + ", " + gameFileOffset + ", " + gameLength);
		}
		return gameData;
    }
	public void getFieldsFromGameId(int gameId)
    {	
		fCur = null;
		int gameCnt = getRowCount(TABLE_NAME);
		if (gameCnt > 0)
		{
			if (gameId > gameCnt) 	gameId = gameCnt;
			if (gameId < 1) 		gameId = 1;
			fCur = db.rawQuery("SELECT * FROM " + TABLE_NAME + " WHERE _id=" + gameId, null);
			fCur.moveToFirst();
		}
    }
	public String getGameData(int curserPos)
    {	// Cursor pgnC; curserPos: 0=current, 1=previus, 2=next, 3=first, 4=last, 9=random; 
		String gameData = "";
		long gameFileOffset = 0;
		int gameLength = 0;
		if (pgnC != null ) 
		{
			boolean posOk = false;
			int cnt = pgnC.getCount();
			if (cnt < 1)
				return "";
			switch (curserPos)
	        {
	        	case 0:     // current
	        		if (pgnC.getPosition() < 0)
	        			posOk = pgnC.moveToFirst();
	        		else	
	        			posOk = true;
	        		break;    
	        	case 1:     // previus
	        		posOk = pgnC.moveToPrevious();
		        	break;
		        case 2:     // next
		        	posOk = pgnC.moveToNext();
		            break;
		        case 3:     // first
		        	posOk = pgnC.moveToFirst();
		            break;
		        case 4:     // last
		        	posOk = pgnC.moveToLast();
		            break;
		        case 9:     // random
		        	Random r;
		        	r = new Random();
		        	cnt = r.nextInt(cnt);
		        	posOk = pgnC.moveToPosition(cnt);
		            break;
	        }
			if (posOk)
			{
				gameFileOffset = pgnC.getInt(pgnC.getColumnIndex("GameFileOffset"));
				gameLength = pgnC.getInt(pgnC.getColumnIndex("GameLength"));
				gameData = pgnDataFromRaf(pgnPath, pgnFile, gameFileOffset, gameLength);
			}
		}
//		Log.i(TAG, "\n" + " ");
//		Log.i(TAG, "curserAction, gId, gOffset, gLength: " + curserPos + ", " + pgnC.getInt(pgnC.getColumnIndex("_id")) + ", " + gameFileOffset + ", " + gameLength);
//		Log.i(TAG, "\n" + gameData);
		return gameData;
    }
	public int getStateFromLastGame()
    {
		int gameCnt = getRowCount(TABLE_NAME);
		pgnRafOffset = 0;
		if (gameCnt > 0)
		{
			File fPgn = new File(pgnPath + pgnFile);
			long pgnLength = fPgn.length();
			if (db.getVersion() != DB_VERSION)
				return 8;
			Cursor cur = db.rawQuery("SELECT GameFileOffset, GameLength FROM " + TABLE_NAME + " WHERE _id=" + gameCnt, null);
			cur.moveToFirst();
			long gameFileOffset = cur.getInt(cur.getColumnIndex("GameFileOffset"));
			long gameLength = cur.getInt(cur.getColumnIndex("GameLength"));
//			Log.i(TAG, "pgnLength, gameFileOffset, gameLength " + pgnLength + ", " + gameFileOffset + ", " + gameLength);
			if (pgnLength == gameFileOffset + gameLength)
				return 1;
			else
			{
				if (pgnLength > gameFileOffset + gameLength)
				{
					RandomAccessFile pgnRaf = null;
					String line;
					try {pgnRaf = new RandomAccessFile(pgnPath + pgnFile, "r");} 
					catch (FileNotFoundException e) {e.printStackTrace(); return 0;}	// error file not exist
					try 
					{
						pgnRaf.seek(gameFileOffset);
						line = pgnRaf.readLine();
						pgnRaf.seek(gameFileOffset + gameLength);
						line = pgnRaf.readLine();
						while (line != null)
						{
							if (line.startsWith("[Event "))
							{
								pgnRafOffset = gameFileOffset + gameLength;
								return 2;
							}
							else
							{
								if (line.startsWith(""))
									line = pgnRaf.readLine();
								else
									return 0;
							}
						}
						return 0;
					} catch (IOException e) {e.printStackTrace(); return 0;}
				}
				else
					return 0;
			}
		}
		else
			return 1;
    }
	public int getRowCount(String tableName)
    {
		int id = 0; 
		String query = "SELECT MAX(_id) AS max_id FROM " + tableName;
		try	
		{ 
			Cursor cursor = db.rawQuery(query, null);
			if (cursor.moveToFirst())
			{
			    do {id = cursor.getInt(0);} 
			    while(cursor.moveToNext());           
			}
		}
		catch (IllegalStateException e) 	{e.printStackTrace(); return 0;}
		catch (SQLException e) 				{e.printStackTrace(); return 0;}
//		Log.i(TAG, "row count table " + tableName + ": " + id);
		return id;
    }
	public String getDbVersion()
    {
		String sqliteVersion = "";
		try
		{
			Cursor cursor = db.rawQuery("select sqlite_version() AS sqlite_version", null);
			while(cursor.moveToNext())
			{
			   sqliteVersion += cursor.getString(0);
			}
		}
		catch (SQLException e) 	{e.printStackTrace(); sqliteVersion = "?";}
		return sqliteVersion;
    }
	
	final String TAG = "PgnDb";
	final int DB_VERSION = 1;
	final int MAX_AUTO_CREATE_LENGTH = 100000;
	public static final String TABLE_NAME = "pgn";
	public static final String PGN_ID = "_id";
	public static final String PGN_GAME_FILE_OFFSET = "GameFileOffset";
	public static final String PGN_GAME_LENGTH = "GameLength";
	public static final String PGN_GAME_MOVES_OFFSET = "GameMovesOffset ";
	public static final String PGN_EVENT = "Event";
	public static final String PGN_SITE = "Site";
	public static final String PGN_DATE = "Date";
	public static final String PGN_ROUND = "Round";
	public static final String PGN_WHITE = "White";
	public static final String PGN_BLACK = "Black";
	public static final String PGN_RESULT = "Result";
	public static final String PGN_SET_UP = "SetUp";
	public static final String PGN_FEN = "FEN";
	public static final String PGN_WHITE_TITLE = "WhiteTitle";
	public static final String PGN_BLACK_TITLE = "BlackTitle";
	public static final String PGN_WHITE_ELO = "WhiteElo";
	public static final String PGN_BLACK_ELO = "BlackElo";
	public static final String PGN_ECO = "ECO";
	public static final String PGN_OPENING = "Opening";
	public static final String PGN_VARIATION = "Variation";
	public static final String PGN_WHITE_TEAM = "WhiteTeam";
	public static final String PGN_BLACK_TEAM = "BlackTeam";
	public static final String PGN_WHITE_FIDE_ID = "WhiteFideId";
	public static final String PGN_BLACK_FIDE_ID = "BlackFideId";
	public static final String PGN_EVENT_DATE = "EventDate";
	public static final String PGN_EVENT_TYPE = "EventType";
	
	public static final String PGN_QUERY = "SELECT _id, GameFileOffset, GameLength, GameMovesOffset, " +
				"White, Black, Date, Result, Event, Site, ECO, Opening, Variation FROM pgn ";
	
//	Context context;
	//	pgn file variable
	String pgnPath = ""; 
	String pgnFile = "";
	String pgnDbFile = "";
	long pgnLength = 0;
	long pgnRafOffset = 0;
	String pgnStat = "-";
//	pgn-db variable
	SQLiteDatabase db = null;	//	database instance from file .pgn-db
	Cursor pgnC = null;			//	result from query
	Cursor fCur = null;			// 	cursor for fields
	int scrollGameId = 1;		//	cursor scroll gameID
	int pgnGameId = 1;			//	= primary key(_id) from table: pgn
	int pgnGameCount = 0;		//	pgn: game count
	long pgnGameOffset = 0;		//	game offset (skeep)
	int pgnCurserId = 1;		//	curser position(result)

	// pgn-db columns
	long GameFileOffset = 0;
	int GameLength = 0;
	int GameMovesOffset = 0;
	String Event = "";
	String Site = "";
	String Date = "";
	String Round = "";
	String White = "";
	String Black = "";
	String Result = "";
	String SetUp = "";
	String FEN = "";
	String WhiteTitle = "";
	String BlackTitle = "";
	String WhiteElo = "";
	String BlackElo = "";
	String ECO = "";
	String Opening = "";
	String Variation = "";
	String WhiteTeam = "";
	String BlackTeam  = "";
	String WhiteFideId = "";
	String BlackFideId = "";
	String EventDate = "";
	String EventType  = "";
}