package ccc.chess.gui.chessforall;

import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseLockedException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteReadOnlyDatabaseException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Random;

public class PgnDb 
{
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

//Log.i(TAG, "seekStart, gameLength, readedBytes,  + buffer.length: " + gameOffset + ", " + gameLength + ", " + readedBytes  + ", " + buffer.length);

				data = new String(buffer);

//Log.i(TAG, "seekStart, gameLength, readedBytes: " + gameOffset + ", " + gameLength + ", " + readedBytes);
//Log.i(TAG, "\n" + data);

				return data;
			}
			else
			{

//Log.i(TAG, "buffer, readedBytes: " + buffer + ", " + readedBytes);

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

	public boolean existsDbFile(String pgnPath, String pgnDbFile)
	{
		File fPgnDb = new File(pgnPath + pgnDbFile);
		if (fPgnDb.exists() & pgnDbFile.endsWith(".pgn-db"))
			return true;
		else
			return false;
	}

	public boolean openDb(String path, String file, int flags)
    {
		if (db == null)
			initPgnFiles(path, file);
		try 
		{
			db = SQLiteDatabase.openDatabase(pgnPath + pgnDbFile, null, flags);

//Log.i(TAG, "db.getVersion(): "+ pgnPath + pgnDbFile + ", " + db.getVersion());

			return db.isOpen();
		} 
		catch (SQLiteException e) {e.printStackTrace(); return false;}	// database open error
    }

	public void closeDb()
    {
		try		{db.close();}
		catch 	(SQLiteException e) 		{e.printStackTrace();}	// database close error
		catch  	(NullPointerException e)	{e.printStackTrace();}	// 23. Apr. 2020 14:32 in der App-Version 80
    }

	public Cursor queryPgn(int gameId, int maxCursorRow, boolean isDesc)
    {	// query games from db(MAX_CURSOR_ROWS) and put the result to Cursor(return)
		if (db != null)
		{

//Log.i(TAG, "gameId: " + gameId);

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

//	Log.i(TAG, "1, gameId, maxCursorRow, isDesc, fromRow, toRow, rowCnt, scrollGameId: \n"
//		+ gameId + ", " + maxCursorRow + ", " + isDesc + ", " + fromRow + ", " + toRow + ", " + rowCnt + ", " + scrollGameId);

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

//Log.i(TAG, "gameId, fromRow, toRow, scrollGameId: " + gameId + ", " + fromRow + ", " + toRow + ", " + scrollGameId);

				query = PGN_QUERY + "WHERE _id >= " + fromRow + " AND _id <= " + toRow;
			}

// android.database.sqlite.SQLiteReadOnlyDatabaseException App-Version 73
            try { pgnC = db.rawQuery(query + qOrder, null); }
            catch (SQLiteReadOnlyDatabaseException e) 	{e.printStackTrace(); return null;}
			catch (IllegalStateException e) 			{e.printStackTrace(); return null;}

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

//Log.i(TAG, "event, site: " + event + ", " + site);

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
			qEvent = "WHERE LOWER(Event) LIKE '" +event +"' ";
			qSite =  " AND LOWER(Site) LIKE '" +site +"' ";
			pgnC = db.rawQuery(PGN_QUERY +qEvent +qSite, null);
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
			pgnC = db.rawQuery(PGN_QUERY +qEco +qOpening +qVariation +qDate +qOrder, null);
			if (pgnC != null)
				pgnC.moveToFirst();
			return pgnC;
		}
		else
			return null;
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

//Log.i(TAG, "getDataFromGameId()");

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

//Log.i(TAG, "games, gameId, gameFileOffset, gameLength: " + gameCnt + ", " + gameId + ", " + gameFileOffset + ", " + gameLength);

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

	public int getStateFromLastGame()
    {

        if (db.inTransaction() | db.isDbLockedByCurrentThread() | db.isDbLockedByOtherThreads())
        {

//            Log.i(TAG, "getStateFromLastGame(), db.inTransaction()");

            return 5;
        }

		int gameCnt = getRowCount(TABLE_NAME);

//Log.i(TAG, "getStateFromLastGame(), gameCnt: " + gameCnt);

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

//Log.i(TAG, "pgnLength, gameFileOffset, gameLength " + pgnLength + ", " + gameFileOffset + ", " + gameLength);

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
			return 6;
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
		catch (SQLiteDatabaseLockedException e) 				{e.printStackTrace(); return 0;}
		catch (SQLException e) 				{e.printStackTrace(); return 0;}
		catch (NullPointerException e) 				{e.printStackTrace(); return 0;}

//Log.i(TAG, "row count table " + tableName + ": " + id);

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
	public static final String TABLE_NAME = "pgn";
	public static final String PGN_ID = "_id";
//	public static final String PGN_GAME_FILE_OFFSET = "GameFileOffset";
//	public static final String PGN_GAME_LENGTH = "GameLength";
//	public static final String PGN_GAME_MOVES_OFFSET = "GameMovesOffset ";
	public static final String PGN_EVENT = "Event";
//	public static final String PGN_SITE = "Site";
	public static final String PGN_DATE = "Date";
//	public static final String PGN_ROUND = "Round";
	public static final String PGN_WHITE = "White";
	public static final String PGN_BLACK = "Black";
	public static final String PGN_RESULT = "Result";
//	public static final String PGN_SET_UP = "SetUp";
//	public static final String PGN_FEN = "FEN";
//	public static final String PGN_WHITE_TITLE = "WhiteTitle";
//	public static final String PGN_BLACK_TITLE = "BlackTitle";
//	public static final String PGN_WHITE_ELO = "WhiteElo";
//	public static final String PGN_BLACK_ELO = "BlackElo";
//	public static final String PGN_ECO = "ECO";
//	public static final String PGN_OPENING = "Opening";
//	public static final String PGN_VARIATION = "Variation";
//	public static final String PGN_WHITE_TEAM = "WhiteTeam";
//	public static final String PGN_BLACK_TEAM = "BlackTeam";
//	public static final String PGN_WHITE_FIDE_ID = "WhiteFideId";
//	public static final String PGN_BLACK_FIDE_ID = "BlackFideId";
//	public static final String PGN_EVENT_DATE = "EventDate";
//	public static final String PGN_EVENT_TYPE = "EventType";
	
	public static final String PGN_QUERY = "SELECT _id, GameFileOffset, GameLength, GameMovesOffset, " +
				"White, Black, Date, Result, Event, Site, ECO, Opening, Variation FROM pgn ";
	
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

}