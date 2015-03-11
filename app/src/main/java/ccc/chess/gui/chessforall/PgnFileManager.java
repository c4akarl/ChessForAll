package ccc.chess.gui.chessforall;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Random;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnTouchListener;
import android.view.WindowManager.BadTokenException;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
//import android.util.Log;

public class PgnFileManager extends Activity implements Ic4aDialogCallback, OnItemClickListener, OnTouchListener,
			DialogInterface.OnCancelListener, TextWatcher
{
	public void onCreate(Bundle savedInstanceState) 
	{
//		Log.i(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setQueryData = 99;	// query none
        runP = getSharedPreferences("run", 0);
        userPrefs = getSharedPreferences("user", 0);
        fmPrefs = getSharedPreferences("fm", 0);
        setAppPausedPref(false);
	}
	@Override
    protected void onResume()
    {
//		Log.i(TAG, "onResume()");
		super.onResume();
        startPfm();
    }
    @Override
    protected void onDestroy() 					
    {
//    	Log.i(TAG, "onDestroy()");
     	super.onDestroy();
    }
    @Override
    protected void onPause() 					
    {
//    	Log.i(TAG, "onPause()");
    	super.onPause();
    	setAppPausedPreferences();
    }
    public void startPfm() 
	{
        getPreferences();
        pgnIO = new PgnIO();
//        pgnDb = new PgnDb(this);
        pgnDb = new PgnDb();
        baseDir = pgnIO.getExternalDirectory(0);
        if (fm_extern_load_path.equals("") & pgnIO.pathExists(baseDir + "c4a/"))
        	fm_extern_load_path = "c4a/";
        if (fm_extern_save_path.equals("") & !fm_extern_load_path.equals(""))
			fm_extern_save_path = fm_extern_load_path;
        if (fm_extern_save_path.equals("") & pgnIO.pathExists(baseDir + "c4a/"))
			fm_extern_save_path = "c4a/";
        fileActionCode = getIntent().getExtras().getInt("fileActionCode");
        fm_file_extension = ".pgn";
        if (fileActionCode == 9)
        {
        	isPriviousGame = true;
        	fileActionCode = 1;
        }
//        Log.i(TAG, "fileActionCode, displayActivity: " + fileActionCode + ", " + getIntent().getExtras().getInt("displayActivity"));
        if (getIntent().getExtras().getInt("displayActivity") == 1)
        {
	        setContentView(R.layout.pgnfilemanager);
	        relLayout = (RelativeLayout) findViewById(R.id.fmLayout);
	        queryGameIdLayout = (RelativeLayout) findViewById(R.id.queryGameId);
	        queryGameIdLayout.setVisibility(RelativeLayout.INVISIBLE);
	        queryPlayerLayout = (RelativeLayout) findViewById(R.id.queryPlayer);
	        queryPlayerLayout.setVisibility(RelativeLayout.INVISIBLE);
	        queryEventLayout = (RelativeLayout) findViewById(R.id.queryEvent);
	        queryEventLayout.setVisibility(RelativeLayout.INVISIBLE);
	        queryEcoLayout = (RelativeLayout) findViewById(R.id.queryEco);
	        queryEcoLayout.setVisibility(RelativeLayout.INVISIBLE);
	        lblPath = (TextView) findViewById(R.id.fmLblPath);
	        lblFile = (TextView) findViewById(R.id.fmLblFile);
	        etBase = (EditText) findViewById(R.id.fmBase);
	        etPath = (EditText) findViewById(R.id.fmEtPath);
	        etUrl  = (EditText) findViewById(R.id.fmEtUrl);
	        etFile = (EditText) findViewById(R.id.fmEtFile);
	        etPath.setText("");
	        etFile.setText("");
	        etUrl.setText("");
	        btnMenu = (ImageView) findViewById(R.id.btnMenu);
	        if (fileActionCode == 1)
	        {
		        registerForContextMenu(btnMenu);
		        btnMenu.setOnTouchListener((OnTouchListener) this);
	        }
	        else
	        	btnMenu.setVisibility(ImageView.INVISIBLE);
	        fmBtnAction = (ImageView) findViewById(R.id.fmBtnAction);
	        btnAddFolder = (ImageView) findViewById(R.id.btnAddFolder);
	        btnAddFolder.setVisibility(ImageView.INVISIBLE);
	        fmBtnGames = (ImageView) findViewById(R.id.fmBtnGames);
	        registerForContextMenu(fmBtnGames);
	        fmBtnGames.setOnTouchListener((OnTouchListener) this);
	        fmBtnGames.setVisibility(ImageView.INVISIBLE);
	        btnOptions = (ImageView) findViewById(R.id.btnOptions);
	        btnOptions.setVisibility(ImageView.INVISIBLE);
	        lvFiles = (ListView) findViewById(R.id.fmLvFiles);
	        lvGames = (ListView) findViewById(R.id.fmGameView);
	        lvGames.setVisibility(ListView.INVISIBLE);
	        emptyLv = (TextView)findViewById(R.id.emptyLv);
	        
	        queryInfo1 = (TextView)findViewById(R.id.fmQueryInfo1);
	        queryInfo2 = (TextView)findViewById(R.id.fmQueryInfo2);
	        
	        qGameCount = (EditText) findViewById(R.id.qGameCount);
	        qGameDescCb = (CheckBox) findViewById(R.id.qGameDescCb);
	        qCurrentId = (EditText) findViewById(R.id.qCurrentId);
	        qMaxItems = (EditText) findViewById(R.id.qMaxItems);
			qCurrentId.setText(Integer.toString(fm_extern_db_game_id));
			qMaxItems.setText(Integer.toString(fm_extern_db_game_max_items));
	        qCurrentId.addTextChangedListener(this);
	        qMaxItems.addTextChangedListener(this);
	        
	        qPlayer = (EditText) findViewById(R.id.qPlayer);
	        qPlayer.addTextChangedListener(this);
	        qColor = (RadioGroup) findViewById(R.id.qColor); 
	        qWhiteRb = (RadioButton) findViewById(R.id.qWhiteRb);
	        qBlackRb = (RadioButton) findViewById(R.id.qBlackRb);
	        qWhiteBlackRb = (RadioButton) findViewById(R.id.qWhiteBlackRb);
	        qDateDescCb = (CheckBox) findViewById(R.id.qDateDescCb);
	        qDateFrom = (EditText) findViewById(R.id.qDateFrom);
	        qDateFrom.addTextChangedListener(this);
	        qDateTo = (EditText) findViewById(R.id.qDateTo);
	        qDateTo.addTextChangedListener(this);
	        qEvent = (EditText) findViewById(R.id.qEvent);
	        qSite = (EditText) findViewById(R.id.qSite);
	        
	        qeEvent = (EditText) findViewById(R.id.qeEvent);
	        qeSite = (EditText) findViewById(R.id.qeSite);
	        
	        qoEco = (EditText) findViewById(R.id.qoEco);
	        qoOpening = (EditText) findViewById(R.id.qoOpening);
	        qoVariation = (EditText) findViewById(R.id.qoVariation);
	        qoDateDescCb = (CheckBox) findViewById(R.id.qoDateDescCb);
	        qoDateFrom = (EditText) findViewById(R.id.qoDateFrom);
	        qoDateTo = (EditText) findViewById(R.id.qoDateTo);
	        
	        qActionBtn = (ImageView) findViewById(R.id.qActionBtn);
//	        Log.i(TAG, "fileActionCode, fm_location: " + fileActionCode + ", " + fm_location);
	        switch (fileActionCode) 											// Load | Save | Delete
			{
				case 1: 														// Load
					fm_location = fmPrefs.getInt("fm_load_location", 1);
					if (fm_location == 1)
					{
						if (userPrefs.getBoolean("user_options_gui_usePgnDatabase", true))
						{
							fmBtnGames.setVisibility(ImageView.VISIBLE);
							if (getIntent().getExtras().getInt("gameLoad") == 1)
							{
								if (getGameIdValues())
								{
									isGameValues = true;
									if (getIntent().getExtras().getString("queryControl").equals("w") | getIntent().getExtras().getString("queryControl").equals("b"))
									{
										fm_extern_db_key_id = 1;
										if (getIntent().getExtras().getString("queryControl").equals("w"))
											fm_extern_db_key_player = pgnDb.fCur.getString(pgnDb.fCur.getColumnIndex("White"));
										if (getIntent().getExtras().getString("queryControl").equals("b"))
											fm_extern_db_key_player = pgnDb.fCur.getString(pgnDb.fCur.getColumnIndex("Black"));
									}
									if (getIntent().getExtras().getString("queryControl").equals("e"))
									{
										fm_extern_db_key_id = 3;
										fm_extern_db_key_event = pgnDb.fCur.getString(pgnDb.fCur.getColumnIndex("Event"));
										fm_extern_db_key_event_site = pgnDb.fCur.getString(pgnDb.fCur.getColumnIndex("Site"));
									}
									if (getIntent().getExtras().getString("queryControl").equals("o"))
									{
										fm_extern_db_key_id = 9;
										fm_extern_db_key_eco = pgnDb.fCur.getString(pgnDb.fCur.getColumnIndex("ECO"));
										fm_extern_db_key_eco_opening = pgnDb.fCur.getString(pgnDb.fCur.getColumnIndex("Opening"));
										fm_extern_db_key_eco_variation = pgnDb.fCur.getString(pgnDb.fCur.getColumnIndex("Variation"));
									}
								}
								scroll_game_id = 1;
							}
						}
					}
					startLoad(fm_location, true);									
					break;
				case 91: 														// Load opening book (.bin)
					fm_location = 1;
					fm_file_extension = ".bin";
					startLoad(fm_location, false);
					break;
				case 2:															// Save
					btnAddFolder.setVisibility(ImageView.VISIBLE);
					if (fm_extern_save_path.equals(""))
			        {
						etBase.setText(baseDir);
						etPath.setText("");
						etFile.setText("");
			        	defaultFolder = "c4a";
			        	removeDialog(ADD_FOLDER_DIALOG);
						showDialog(ADD_FOLDER_DIALOG);
			        }
					else
						startSave();
					break;
				case 3: 														// Delete
					startDelete(true);
					break;
				case 5: 														// engine autoPlay(get path/file)
					startSave();
					break;
			}
        }
        else			// no screen(batch)
        {
//        	Log.i(TAG, "fm_location: " + fm_location);
         	if (getIntent().getExtras().getInt("gameLoad") == 9)
        		isLastGame = true;
        	if (getIntent().getExtras().getInt("gameLoad") == 7)
        		isSkipRandom = true;
        	if (getIntent().getExtras().getInt("gameLoad") == 5)
        		isFindGame = true;
        	switch (fileActionCode) 											// Load | Save | Delete
			{
				case 1: 														// Load
					fm_location = fmPrefs.getInt("fm_load_location", 1);
					if (fm_location == 1 & userPrefs.getBoolean("user_options_gui_usePgnDatabase", true))
						loadGameFromDb(fm_extern_load_path, fm_extern_load_file, fm_extern_db_game_id, getIntent().getExtras().getInt("gameLoad"));
					else
						startLoadNoScreen();									
					break;
				case 2: 														// Save
					startSaveNoScreen(2);
					finish();
					break;
				case 7: 														// Save(old game), Load(new game)
				case 71: 														// Save(old game, MateAnalysis OK), Load(new game)
				case 72: 														// Save(old game, MateAnalysis ERROR), Load(new game)
					int saveOld = fileActionCode;
					fileActionCode = 2;
					startSaveNoScreen(saveOld);
					pgnIO = new PgnIO();
					fileActionCode = 1;
					if (fm_location == 1 & userPrefs.getBoolean("user_options_gui_usePgnDatabase", true))
						loadGameFromDb(fm_extern_load_path, fm_extern_load_file, fm_extern_db_game_id, getIntent().getExtras().getInt("gameLoad"));
					else
						startLoadNoScreen();
					break;
			}
        }
	}
//	ContextMenu			ContextMenu			ContextMenu			ContextMenu			ContextMenu			
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) 
    {
    	if (v == fmBtnGames)
		{
			MenuInflater inflater = getMenuInflater();
		    inflater.inflate(R.menu.c4a_menu_query, menu);
		    menu.setHeaderTitle(getString(R.string.menu_query));
		}
    	if (v == btnMenu)
		{
			MenuInflater inflater = getMenuInflater();
		    inflater.inflate(R.menu.c4a_menu_load, menu);
		    menu.setHeaderTitle(getString(R.string.menu_title));
		    if (fileActionCode == 1)		// 1 = (load game)
		  	{
	    		menu.findItem(R.id.menu_load_extern).setVisible(true);  
	    		menu.findItem(R.id.menu_load_intern).setVisible(true);  
	    		menu.findItem(R.id.menu_load_www).setVisible(true);
	    		if (isQueryResult)
	    			menu.findItem(R.id.menu_load_game).setVisible(true); 
	    		else
	    			menu.findItem(R.id.menu_load_game).setVisible(false);
	    	}
	    	else
	    	{
	    		menu.findItem(R.id.menu_load_extern).setVisible(false);  
	    		menu.findItem(R.id.menu_load_intern).setVisible(false);  
	    		menu.findItem(R.id.menu_load_www).setVisible(false);
	    		menu.findItem(R.id.menu_load_game).setVisible(false);  
	    	}
		    menu.findItem(R.id.menu_load_intern).setVisible(false);	// disable Assets  
		}
    }
    public boolean onContextItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
	        case R.id.menu_load_extern: 
	        	fm_location = 1;
	        	startLoad(fm_location, true);
	            return true;  
	        case R.id.menu_load_intern:  
	        	fm_location = 2;
	        	if (lvGames.isShown())
					lvGames.setVisibility(ListView.INVISIBLE);
	        	startLoad(fm_location, true);
	            return true;
	        case R.id.menu_load_www:
	        	startPgnDownload();
	            return true;
	        case R.id.menu_load_game:
	        	removeDialog(MENU_GAME_DIALOG);
                showDialog(MENU_GAME_DIALOG);
	            return true;
        
	        case R.id.menu_query_gameId:
	        	dataQueryGameID();
	            return true;
	        case R.id.menu_query_player:
	        	dataQueryPlayer();
	            return true;
	        case R.id.menu_query_event:
	        	dataQueryEvent();				
	            return true; 
	        case R.id.menu_query_eco:
        		dataQueryEco();
	            return true; 
        }
        return false;
    }
//	Dialog, Listener, Handler		Dialog, Listener, Handler		Dialog, Listener, Handler	
    public void myClickHandler(View view) 		
    {	// ClickHandler	(ButtonEvents)
    	switch (view.getId()) 
		{
		case R.id.fmBtnAction:
			if (isQuery) return;
			switch (fileActionCode) 											// Load | Save | Delete
			{
				case 1:															// Load
					if (etFile.getText().toString().endsWith(fm_file_extension))
					{
						if (fm_location == 1)
							fm_extern_skip_bytes = 0;
						if (fm_location == 2)
							fm_intern_skip_bytes = 0;
						if 	(fm_location == 1 &	userPrefs.getBoolean("user_options_gui_usePgnDatabase", true))
						{
							db_state = getDbFileState(etFile.getText().toString(), fm_location);
							if 	(db_state == STATE_DB_OK)
							{
								if 	(pgnDb.openDb(baseDir + etPath.getText().toString(), etFile.getText().toString(), SQLiteDatabase.OPEN_READONLY))
								{
									int gId = fm_extern_db_game_id;
									int queryCount = 0;
									if (fm_extern_db_key_id != 0)
										queryCount = gameCursor.getCount();
									scroll_game_id = 1;
									setQueryDataToTitle(fm_extern_db_key_id, gId, pgnDb.getRowCount(PgnDb.TABLE_NAME), scroll_game_id, queryCount);
									if (fm_extern_db_key_id != 0)
									{
										if (gameCursor.getCount() == 0)
										{
											pgnDb.closeDb();
											emptyLv.setVisibility(TextView.INVISIBLE);
											if (lvGames.isShown())
												lvGames.setVisibility(ListView.INVISIBLE);
											return;
										}
										gameCursor.moveToFirst();
										gId = gameCursor.getInt(gameCursor.getColumnIndex("_id"));
									}
									pgnDb.getGameId(gId, 10);	
									pgnIO.pgnStat = pgnDb.pgnStat;
									fileData = pgnDb.getDataFromGameId(gId);
									pgnDb.closeDb();
									if (!fileData.equals(""))
									{
										fm_extern_db_cursor_id = 0;	
										if (fm_extern_db_key_id != 0)
										{
							        		fm_extern_db_cursor_count = gameCursor.getCount();		
											fm_extern_db_game_id_list = getGameIdStringList(gameCursor);
										}
										else
										{
							        		fm_extern_db_cursor_count = 0;		
											fm_extern_db_game_id_list = "";
										}
										finishAfterLoad(baseDir + etPath.getText().toString(), etFile.getText().toString());
									}
								}
							}
							else
								handleDb(baseDir + etPath.getText().toString(), etFile.getText().toString(), db_state, false);
						}
						else
							loadFile();
					}
					break;
				case 91:														// Load opening book
					if (!etFile.getText().toString().endsWith(".bin"))
						etFile.setText("");
					returnIntent = new Intent();
					returnIntent.putExtra("fileBase", baseDir);
					returnIntent.putExtra("filePath", etPath.getText().toString());
					returnIntent.putExtra("fileName", etFile.getText().toString());
					setResult(RESULT_OK, returnIntent);
					finish();					
					break;
				case 2: 														// Save
					if (!etFile.getText().toString().endsWith(fm_file_extension))
					{
						etFile.setText(fm_file_extension);
						removeDialog(FILE_NOT_ENDS_WITH_PGN_DIALOG);
						showDialog(FILE_NOT_ENDS_WITH_PGN_DIALOG);
					}
					else
					{
						if (etFile.getText().toString().equals(fm_file_extension))
						{
							removeDialog(PGN_ERROR_DIALOG);
							showDialog(PGN_ERROR_DIALOG);
						}
						else
						{
							if (pgnIO.fileExists(baseDir + etPath.getText().toString(), etFile.getText().toString()))
								saveFile(true);
							else
								saveFile(false);
						}
					}
					break;
				case 3: 														// Delete
					deleteFile();											
					break;
				case 5: 														// engine autoPlay
					getPathFile();											
					break;
			}
			break;
		case R.id.btnAddFolder:
			if (isQuery) return;
			if (fm_location == 1)
			{
				defaultFolder = "";
				removeDialog(ADD_FOLDER_DIALOG);
				showDialog(ADD_FOLDER_DIALOG);
			}
			break;
		case R.id.btnDirBack:
			if (isQuery) return;
			emptyLv.setVisibility(TextView.INVISIBLE);
			queryInfo1.setVisibility(ImageView.INVISIBLE);
			queryInfo2.setVisibility(ImageView.INVISIBLE);
			if (lvGames.isShown())
			{
				lvGames.setVisibility(ListView.INVISIBLE);
				this.setTitle(getString(R.string.fmTitleLoad));
			}
			else
			{
				if (fm_location == 1)
				{
					String newPath = getNewPath(etPath.getText().toString());
					etPath.setText(newPath);
					if (fileActionCode == 1 | fileActionCode == 3)
					{
						etFile.setText("");
						fmBtnGames.setVisibility(ImageView.INVISIBLE);
						fmBtnAction.setVisibility(ImageView.INVISIBLE);
					}
					showFileList(baseDir + newPath);
				}
				if (fm_location == 2)
				{
					etPath.setText("");
					etFile.setText("");
					fmBtnGames.setVisibility(ImageView.INVISIBLE);
					fmBtnAction.setVisibility(ImageView.INVISIBLE);
					showAssetsDir();
				}
			}
			break;
		case R.id.fmEtFile:
			if (isQuery) return;
			if (etFile.getText().equals(""))
				etFile.setText(fm_file_extension);
			break;
		// query - views
		case R.id.qActionBtn:
		case R.id.qCancel:
			if (view.getId() == R.id.qActionBtn & isQueryInputError)
				return;
			queryGameIdLayout.setVisibility(RelativeLayout.INVISIBLE);
			queryPlayerLayout.setVisibility(RelativeLayout.INVISIBLE);
			queryEventLayout.setVisibility(RelativeLayout.INVISIBLE);
			queryEcoLayout.setVisibility(RelativeLayout.INVISIBLE);
			isQuery = false;
			if (view.getId() == R.id.qActionBtn)
			{
				isGameValues = false;
				switch (setQueryData) 											
				{
					case SET_QUERY_DATA_PLAYER:
						if (pgnDb.initPgnFiles(baseDir + etPath.getText().toString(), etFile.getText().toString()))
			        	{
							setPlayerData();
			        		fm_extern_db_key_id = SET_QUERY_DATA_PLAYER;
			        		scroll_game_id = 1;
							displayGames(baseDir + etPath.getText().toString(), etFile.getText().toString(), false, false);
			        	}
						break;
					case SET_QUERY_DATA_DATE:
						fm_extern_db_key_id = SET_QUERY_DATA_DATE;
		        		scroll_game_id = 1;
						break;
					case SET_QUERY_DATA_EVENT:
						if (pgnDb.initPgnFiles(baseDir + etPath.getText().toString(), etFile.getText().toString()))
			        	{
							setEventData();
							fm_extern_db_key_id = SET_QUERY_DATA_EVENT;
			        		scroll_game_id = 1;
			        		displayGames(baseDir + etPath.getText().toString(), etFile.getText().toString(), false, false);
			        	}
						break;
					case SET_QUERY_DATA_ECO:
						if (pgnDb.initPgnFiles(baseDir + etPath.getText().toString(), etFile.getText().toString()))
			        	{
							setEcoData();
							fm_extern_db_key_id = SET_QUERY_DATA_ECO;
			        		scroll_game_id = 1;
			        		displayGames(baseDir + etPath.getText().toString(), etFile.getText().toString(), false, false);
			        	}
						break;
					case SET_QUERY_DATA_GAME:
					default:
						if (pgnDb.initPgnFiles(baseDir + etPath.getText().toString(), etFile.getText().toString()))
			        	{
			        		fm_extern_db_key_id = SET_QUERY_DATA_GAME;
			        		fm_extern_db_game_id = Integer.parseInt(qCurrentId.getText().toString());
			        		pgnDb.pgnGameId = fm_extern_db_game_id;
			        		scroll_game_id = fm_extern_db_game_id;
			        		fm_extern_db_game_desc = qGameDescCb.isChecked();
			        		fm_extern_db_game_max_items = Integer.parseInt(qMaxItems.getText().toString());
							displayGames(baseDir + etPath.getText().toString(), etFile.getText().toString(), false, false);
			        	}
						break;
				}
			}
			break;
		}
	}
	public void getCallbackValue(int btnValue)
    { 
		if (activDialog == DELETE_DIALOG & btnValue == 1)
		{
			pgnIO.fileDelete(baseDir + etPath.getText().toString(), fileName);
			etFile.setText("");
			showFileList(baseDir + etPath.getText().toString());
		}
		if (activDialog == FILE_EXISTS_DIALOG & btnValue == 1)
		{
			saveFile(false);
		}
		if (activDialog == ADD_FOLDER_DIALOG & btnValue == 2)
		{
			String newFolder = baseDir + etPath.getText().toString() + addFolderDialog.getNumber() + "/";
//			Log.i(TAG, "newFolder: " + newFolder);
			if (pgnIO.createDir(newFolder))
			{
//				Log.i(TAG, "createDir OK");
				etFile.setText(fm_file_extension);
				fm_extern_save_path = etPath.getText().toString() + addFolderDialog.getNumber() + "/";
				etPath.setText(etPath.getText().toString() + addFolderDialog.getNumber() + "/");
				showFileList(newFolder);
				if (!defaultFolder.equals(""))
					startSave();
			}
		}
		if (getIntent().getExtras().getInt("displayActivity") == 0)
			finish();
    }
	@Override
	public void onItemClick(AdapterView<?> l, View v, int position, long id)
	{
		if (isQuery) return;
		if (l == lvFiles) 
        {
			String itemName = files.getItem(position);
			if (itemName.endsWith(fm_file_extension))
			{
				etFile.setText(itemName);
				fmBtnAction.setVisibility(ImageView.VISIBLE);
				if 	(fileActionCode == 1 & fm_location == 1 & userPrefs.getBoolean("user_options_gui_usePgnDatabase", true))
				{
					fmBtnGames.setVisibility(ImageView.VISIBLE);
					scroll_game_id = 1;
					fm_extern_db_key_id = 0;
					db_state = getDbFileState(itemName, fm_location);
					handleDb(baseDir + etPath.getText().toString(), itemName, db_state, true);
				}
			}
			else
			{
				etPath.setText(etPath.getText().toString() + itemName  + "/");
				fmBtnGames.setVisibility(ImageView.INVISIBLE);
				if (fileActionCode != 2)
				{
					fmBtnAction.setVisibility(ImageView.INVISIBLE);
			        etFile.setText("");
				}
				else
				{
					fmBtnAction.setVisibility(ImageView.VISIBLE);
					etFile.setText(fm_file_extension);
				}
				if (fm_location == 1)
					showFileList(baseDir + etPath.getText().toString());
				if (fm_location == 2)
					showAssetsFileList(itemName);
			}
        } 
	}
	@Override
	public boolean onTouch(View view, MotionEvent event) 
	{
		if (isQuery) return false;
		if (view.getId() == R.id.fmBtnGames & event.getAction() == MotionEvent.ACTION_UP)
			openContextMenu(fmBtnGames);
		if (view.getId() == R.id.btnMenu & event.getAction() == MotionEvent.ACTION_UP)
			openContextMenu(btnMenu);
		return true;
	}
	@Override
    protected Dialog onCreateDialog(int id) 
	{
		String mes = "";
		activDialog = id;
		if (id == PATH_NOT_EXISTS_DIALOG) 
        {
			lvFiles.setVisibility(ListView.INVISIBLE);
			fmBtnAction.setVisibility(ImageView.INVISIBLE);
			fmBtnGames.setVisibility(ImageView.INVISIBLE);
        	mes = getString(R.string.fmPathError) + " (" + baseDir + etPath.getText().toString()  + ")";
        	pathDialog = new C4aDialog(this, this, getString(R.string.dgTitleFileDialog), 
        			"", getString(R.string.btn_Ok), "", mes, 0, "");
        	pathDialog.setOnCancelListener(this);
            return pathDialog;
        }
        if (id == FILE_NOT_EXISTS_DIALOG) 
        {
        	mes = fileName + "\n" + getString(R.string.fmFileError);
        	fileNotExistsDialog = new C4aDialog(this, this, getString(R.string.dgTitleFileDialog), 
        			"", getString(R.string.btn_Ok), "", mes, 0, "");
        	fileNotExistsDialog.setOnCancelListener(this);
            return fileNotExistsDialog;
        }
        if (id == FILE_EXISTS_DIALOG) 
        {
        	fileExistsDialog = new C4aDialog(this, this, getString(R.string.dgTitleFileDialog), 
        			getString(R.string.btn_Yes), "", getString(R.string.btn_No), getString(R.string.fmFileExists), 0, "");
        	fileExistsDialog.setOnCancelListener(this);
            return fileExistsDialog;
        }
        if (id == FILE_NOT_ENDS_WITH_PGN_DIALOG) 
        {
        	fileNotEndsWithPgnDialog = new C4aDialog(this, this, getString(R.string.dgTitleFileDialog), 
        			"", getString(R.string.btn_Ok), "", getString(R.string.fmFileNotEndsWithPgn), 0, "");
        	fileNotEndsWithPgnDialog.setOnCancelListener(this);
            return fileNotEndsWithPgnDialog;
        }
        if (id == WEB_FILE_NOT_EXISTS_DIALOG) 
        {
        	mes = getString(R.string.fmWebFileError) + "\n" + etUrl.getText().toString();
        	webFileNotExistsDialog = new C4aDialog(this, this, getString(R.string.dgTitleFileDialog), 
        			"", getString(R.string.btn_Ok), "", mes, 0, "");
        	webFileNotExistsDialog.setOnCancelListener(this);
            return webFileNotExistsDialog;
        }
        if (id == PGN_ERROR_DIALOG) 
        {
        	mes = getString(R.string.fmPgnError) + " (" + etFile.getText().toString() + ")";
        	pgnDialog = new C4aDialog(this, this, getString(R.string.dgTitleFileDialog), 
        			"", getString(R.string.btn_Ok), "", mes, 0, "");
        	pgnDialog.setOnCancelListener(this);
            return pgnDialog;
        } 
        if (id == DELETE_DIALOG) 
        {
        	String delText = getString(R.string.fmDeleteFileQuestion) + " " + fileName + "?";
        	deleteDialog = new C4aDialog(this, this, getString(R.string.dgTitleFileDialog), 
        			getString(R.string.btn_Ok), "", "", delText, 0, "");
        	deleteDialog.setOnCancelListener(this);
            return deleteDialog;
        }
	    if (id == ADD_FOLDER_DIALOG) 
        {
        	mes = getString(R.string.fmAddFolder);
        	addFolderDialog = new C4aDialog(this, this, getString(R.string.dgTitleFileDialog), 
        			"", getString(R.string.btn_Ok), "", mes, 1, defaultFolder);
        	addFolderDialog.setOnCancelListener(this);
            return addFolderDialog;
        }
        if (id == DATABASE_LOCKED_DIALOG) 
        {
            c4aImageDialog = new C4aImageDialog(this, this, getString(R.string.dgTitleFileDialog), getString(R.string.fmDatabaseLocked), 
					0, R.drawable.button_ok, 0);
			c4aImageDialog.setOnCancelListener(this);
			return c4aImageDialog;
        }
        if (id == COMING_SOON) 
        {
            c4aImageDialog = new C4aImageDialog(this, this, getString(R.string.dgTitleFileDialog), getString(R.string.comingSoon), 
					0, R.drawable.button_ok, 0);
			c4aImageDialog.setOnCancelListener(this);
			return c4aImageDialog;
        }
        if (id == QUERY_PROGRESS_DIALOG) 
        {
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage(this.getString(R.string.fmProgressQueryDialog));
			progressDialog.setCancelable(true);
			progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() 
	        {
	            public void onCancel(DialogInterface dialog) { finish(); }
	        });
	        return progressDialog;
        }
        if (id == MENU_GAME_DIALOG) 
        {
        	final int GAME_FIRST     = 0;
            final int GAME_PREVIOUS  = 1;
            final int GAME_NEXT      = 2;
            final int GAME_LAST      = 3;
            final int GAME_RANDOM    = 4;
            List<CharSequence> lst = new ArrayList<CharSequence>();
            List<Integer> actions = new ArrayList<Integer>();
            lst.add(getString(R.string.menu_load_game_first));     	actions.add(GAME_FIRST);
            lst.add(getString(R.string.menu_load_game_previous)); 	actions.add(GAME_PREVIOUS);
            lst.add(getString(R.string.menu_load_game_next));       actions.add(GAME_NEXT);
            lst.add(getString(R.string.menu_load_game_last));     	actions.add(GAME_LAST);
            lst.add(getString(R.string.menu_load_game_random));     actions.add(GAME_RANDOM);
            final List<Integer> finalActions = actions;
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.menu_load_game);
            builder.setItems(lst.toArray(new CharSequence[lst.size()]), new DialogInterface.OnClickListener() 
            {
                public void onClick(DialogInterface dialog, int item) 
                {
                    switch (finalActions.get(item)) 
                    {
                    case GAME_FIRST:
                    	setPathValues();
        	        	if (userPrefs.getBoolean("user_options_gui_usePgnDatabase", true))
        	        		loadGameFromDb(fm_extern_load_path, fm_extern_load_file, fm_extern_db_game_id, 1);
        	        	else
        	        	{
        		        	if (fm_location == 1)
        		        		fm_extern_skip_bytes = 0;
        		        	if (fm_location == 2)
        		        		fm_intern_skip_bytes = 0;
        		        	loadFile();
        	        	}
                    	break;
                    case GAME_PREVIOUS: 
                    	setPathValues();
        	        	if (userPrefs.getBoolean("user_options_gui_usePgnDatabase", true))
        	        		loadGameFromDb(fm_extern_load_path, fm_extern_load_file, fm_extern_db_game_id, 8);
        	        	else
        	        	{
        		        	isPriviousGame = true;
        		        	loadFile();
        	        	}
                        break;
                    case GAME_NEXT: 
                    	setPathValues();
        	        	if (userPrefs.getBoolean("user_options_gui_usePgnDatabase", true))
        	        		loadGameFromDb(fm_extern_load_path, fm_extern_load_file, fm_extern_db_game_id, 0);
        	        	else
        		        	loadFile();
                        break;

                    case GAME_LAST:
                    	setPathValues();
        	        	if (userPrefs.getBoolean("user_options_gui_usePgnDatabase", true))
        	        		loadGameFromDb(fm_extern_load_path, fm_extern_load_file, fm_extern_db_game_id, 9);
        	        	else
        	        	{
        		        	isLastGame = true;
        		        	loadFile();
        	        	}
                        break;
                    case GAME_RANDOM:
                    	setPathValues();
        	        	if (userPrefs.getBoolean("user_options_gui_usePgnDatabase", true))
        	        		loadGameFromDb(fm_extern_load_path, fm_extern_load_file, fm_extern_db_game_id, 7);
        	        	else
        	        	{
        		        	isSkipRandom = true;
        		        	loadFile();
        	        	}
                        break;
                    }
                }
            });
            AlertDialog alert = builder.create();
            return alert;
        }
        return null;
    }
	@Override
	public void onCancel(DialogInterface dialog) { }
	@Override
	public void afterTextChanged(Editable s) 
	{
//		boolean isDateFrom = qDateFrom.isInputMethodTarget();
//		boolean isDateTo = qDateTo.isInputMethodTarget();
//		Log.i(TAG, "isDateFrom, isDateTo: " + isDateFrom + ", " + isDateTo);
		isQueryInputError = false;
		if (setQueryData == 0)
		{	// query game-ID
			int game_count = Integer.parseInt(qGameCount.getText().toString());
			int game_current = 0;
			int game_max_items = 0;
			try 	{game_current = Integer.parseInt(qCurrentId.getText().toString());}
			catch 	(NumberFormatException e) {qCurrentId.setBackgroundResource(R.drawable.borderpink); isQueryInputError = true; return;}
			try 	{game_max_items = Integer.parseInt(qMaxItems.getText().toString());}
			catch 	(NumberFormatException e) {qMaxItems.setBackgroundResource(R.drawable.borderpink); isQueryInputError = true; return;}
			if (game_current < 1 | game_current > game_count)
			{
				qCurrentId.setBackgroundResource(R.drawable.borderpink);
				isQueryInputError = true;
			}
			else
				qCurrentId.setBackgroundResource(R.drawable.bordergreen);
			if (game_max_items < 100 | (game_max_items > 4000))
			{
				qMaxItems.setBackgroundResource(R.drawable.borderpink);
				isQueryInputError = true;
			}
			else
				qMaxItems.setBackgroundResource(R.drawable.bordergreen);
		}
		if (setQueryData == 1)
		{	// query player
			if (!qPlayer.getText().toString().equals(""))
				qPlayer.setBackgroundResource(R.drawable.bordergreen);
			else
			{
				qPlayer.setBackgroundResource(R.drawable.borderpink);
				isQueryInputError = true;
			}
			if (isDateOk(qDateFrom.getText().toString()))
				qDateFrom.setBackgroundResource(R.drawable.bordergreen);
			else
			{
				qDateFrom.setBackgroundResource(R.drawable.borderpink);
				isQueryInputError = true;
			}
			if (isDateOk(qDateTo.getText().toString()))
				qDateTo.setBackgroundResource(R.drawable.bordergreen);
			else
			{
				qDateTo.setBackgroundResource(R.drawable.borderpink);
				isQueryInputError = true;
			}
			if (!isQueryInputError)
			{
				String f = qDateFrom.getText().toString();
				String t = qDateTo.getText().toString();
				int fInt = 0;
				int tInt = 0;
				if (!f.equals("") & !t.equals(""))
				{
					f = f.replace(".", "");
					t = t.replace(".", "");
					try 	{fInt = Integer.parseInt(f);}
					catch 	(NumberFormatException e) { }
					try 	{tInt = Integer.parseInt(t);}
					catch 	(NumberFormatException e) { }
					if (tInt < fInt | fInt == 0 | tInt == 0)
					{
						qDateFrom.setBackgroundResource(R.drawable.borderpink);
						qDateTo.setBackgroundResource(R.drawable.borderpink);
						isQueryInputError = true;
					}
				}
				
			}
		}
	}
	public boolean isDateOk(String date) 
	{
		int day = 0;
	    int month = 0;
	    int year = 0;
		if (date.equals(""))
			return true;
		int cnt = 0;
		for (int i = 0; i < date.length(); i++)
        {
			if (date.charAt(i) == '.')	cnt++;
        }
		if (cnt != 2)
			return false;
		if (date.length() == 10)
		{
			int md = 0;
			try		
			{
				year = Integer.parseInt(date.substring(0, 4));
				month = Integer.parseInt(date.substring(5, 7));
				day = Integer.parseInt(date.substring(8, 10));
			}
	    	catch 	(NumberFormatException e) {return false;}
			Calendar  cal = Calendar.getInstance();
			cal.set(year, month -1, day);
			cal.setLenient(false);
			try 
			{
				cal.get(GregorianCalendar.YEAR);
				cal.get(GregorianCalendar.MONTH);
				cal.get(GregorianCalendar.DAY_OF_MONTH);
				md = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
				if (day > md)
					day = md;
				return true;
			} 
			catch (IllegalArgumentException e) {return false;}
		}
		else
			return false;
	}
	@Override
	public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) { }
// 	pgn methods			pgn methods			pgn methods			pgn methods		pgn methods		pgn methods
	public void startLoad(int location, boolean isStart) 
	{
		this.setTitle(getString(R.string.fmTitleLoad));
		if (fileActionCode == 91)
			this.setTitle(getString(R.string.epOpeningBook));
		relLayout.setBackgroundColor(getResources().getColor(R.color.fm_load));
		String path = "";
		lblPath.setText(getString(R.string.fmLblPath));
		lblFile.setVisibility(ListView.VISIBLE);
		etUrl.setVisibility(ListView.INVISIBLE);
		etUrl.setEnabled(false);
		switch (location) 											// External | Intern | WWW
		{
			case 1:		// External
			case 2:		// Intern
				etBase.setVisibility(ListView.VISIBLE);
				etPath.setVisibility(ListView.VISIBLE);
				etFile.setVisibility(ListView.VISIBLE);
				lvFiles.setVisibility(ListView.VISIBLE);
				etFile.setFocusable(false);
				etBase.setText(baseDir);
				path = baseDir;
				etPath.setText("");
				etFile.setText("");
				fmBtnAction.setVisibility(ImageView.INVISIBLE);
				fmBtnGames.setVisibility(ImageView.INVISIBLE);
				if (isStart)
				{
					etPath.setText(fm_extern_load_path);
					if (!fm_extern_load_file.equals(fm_file_extension))
					{
						etFile.setText(fm_extern_load_file);
						db_state = getDbFileState(fm_extern_load_file, fm_location);
						handleDb(baseDir + fm_extern_load_path, fm_extern_load_file, db_state, false);
					}
					else
						etFile.setText("");
				}
				path = path + etPath.getText();
				showFileList(path);
				if (isStart & fm_location == 1)
				{
					if (!etFile.getText().toString().equals(""))
					{
						fmBtnAction.setVisibility(ImageView.VISIBLE);
						if 	(userPrefs.getBoolean("user_options_gui_usePgnDatabase", true))
							fmBtnGames.setVisibility(ImageView.VISIBLE);
					}
				}
				break;
			case 3:		// WWW
				lblPath.setText(getString(R.string.fmLblUrl));
				etUrl.setVisibility(ListView.VISIBLE);
				etBase.setVisibility(ListView.INVISIBLE);
				etPath.setVisibility(ListView.INVISIBLE);
				lblFile.setVisibility(ListView.INVISIBLE);
				etFile.setVisibility(ListView.INVISIBLE);
				lvFiles.setVisibility(ListView.INVISIBLE);
				etUrl.setEnabled(true);
				etUrl.setText(fm_url);
				break;
		}
	}
	public void startLoadNoScreen() 
	{
		loadExternFile(fm_extern_load_path, fm_extern_load_file, getIntent().getExtras().getInt("gameLoad"));
	}
	public void startSave() 
	{
		fm_location = 1;
		this.setTitle(getString(R.string.fmTitleSave));
		relLayout.setBackgroundColor(getResources().getColor(R.color.fm_save));
		lblPath.setText(getString(R.string.fmLblPath));
		lblFile.setVisibility(ListView.VISIBLE);
		etUrl.setVisibility(ListView.INVISIBLE);
		etUrl.setEnabled(false);
		etBase.setText(baseDir);
		String save_path = fm_extern_save_path;
		String save_file = fm_extern_save_file;
		if (fileActionCode == 5)
		{
			this.setTitle(getString(R.string.fmTitleEngineAutoPlay));
			save_path = fm_extern_save_auto_path;
			save_file = fm_extern_save_auto_file;
		}
		etPath.setText(save_path);
		etFile.setFocusable(true);
		etFile.requestFocus();
		if (save_file.equals(""))
		{
			etFile.setText(fm_file_extension);
		}
		else
		{
			if (pgnIO.fileExists(baseDir + save_path, save_file))
			{
				etFile.setText(save_file);
			}
			else
				etFile.setText(fm_file_extension);
		}
		String path = baseDir;
		if (!etPath.getText().equals(""))						
		{
			path = path + etPath.getText();
			showFileList(path);
		}
		else
			showFileList(baseDir);
		fmBtnAction.setVisibility(ImageView.VISIBLE);
	}
	public void startSaveNoScreen(int saveValue) 
	{	// engine vs engine auto run | mate analysis
		String path = "";
		String file = "";
		switch (saveValue) 
		{
			case 71: 
				path = userPrefs.getString("user_batch_ma_pathOutput", "");
				file = userPrefs.getString("user_batch_ma_fileOutputOk", ""); 
				break;
			case 72: 
				path = userPrefs.getString("user_batch_ma_pathOutput", "");
				file = userPrefs.getString("user_batch_ma_fileOutputError", ""); 
				break;
			case 2: 
			case 7: 
			default: 
				path = userPrefs.getString("user_play_eve_path", "");
				file = userPrefs.getString("user_play_eve_file", ""); 
				break;
		}
		if (pgnIO.pathExists(baseDir + path))
		{
			String data = getIntent().getExtras().getString("pgnData");
			boolean fileExists = pgnIO.fileExists(baseDir + path, file);
			if (userPrefs.getBoolean("user_options_gui_usePgnDatabase", true))
			{
				boolean pgnFilesOk = pgnDb.initPgnFiles(baseDir + path, file);
				if (pgnFilesOk)
				{
//					Log.i(TAG, "startSaveNoScreen(), openDb() true");
					pgnDb.openDb(baseDir + path, file, SQLiteDatabase.OPEN_READONLY);
					long pgnOldLength = pgnDb.pgnLength;
					pgnIO.dataToFile(baseDir + path, file, data, fileExists);
					pgnDb.closeDb();
					notificationId++;
					createDatabaseTask = new CreateDatabaseTask(this, notificationId);
					createDatabaseTask.execute(baseDir + path, file, Long.toString(pgnOldLength));
				}
				else
				{
//					Log.i(TAG, "startSaveNoScreen(), openDb() false");
					pgnIO.dataToFile(baseDir + path, file, data, fileExists);
					pgnDb.deleteDbFile();
					notificationId++;
					createDatabaseTask = new CreateDatabaseTask(this, notificationId);
					createDatabaseTask.execute(baseDir + path, file, "0");
				}
			}
			else
				pgnIO.dataToFile(baseDir + path, file, data, fileExists);
		}
		returnIntent = new Intent();
		setResult(22, returnIntent);
	}
	public void startDelete(boolean isStart) 
	{
		fm_location = 1;
		this.setTitle(getString(R.string.fmTitleDelete));
		relLayout.setBackgroundColor(getResources().getColor(R.color.fm_delete));
		lblPath.setText(getString(R.string.fmLblPath));
		lblFile.setVisibility(ListView.VISIBLE);
		etUrl.setVisibility(ListView.INVISIBLE);
		fmBtnAction.setVisibility(ImageView.VISIBLE);
		etUrl.setEnabled(false);
		etBase.setText(baseDir);
		if (isStart)
		{
			if (lastFileActionCode == 1)
			{
				etPath.setText(fm_extern_load_path);
				etFile.setText(fm_extern_load_file);
			}
			else
			{
				etPath.setText(fm_extern_save_path);
				etFile.setText(fm_extern_save_file);
			}
		}
		else
			etPath.setText("");
		etFile.setFocusable(false);
		String path = baseDir;
		if (!etPath.getText().equals(""))						
		{
			path = path + etPath.getText();
			showFileList(path);
		}
		else
			showFileList(baseDir);
		fmBtnAction.setVisibility(ImageView.VISIBLE);
	}
	public void startPgnDownload() 
	{
		String url = "http://c4akarl.blogspot.co.at/p/pgn-download.html";	// PGN download from "Karl's Blog" (MediaFire file links)
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(url));
		startActivity(i);
	}
	public void showFileList(String path) 
	{
		String[] fileA = null;
		if (pgnIO.pathExists(path))
        {
			lvFiles.setOnItemClickListener(this);
			if (fileActionCode == 91)			// load opening book
				fileA = pgnIO.getFileArrayFromPath(path, true, fm_file_extension);
			else
			{
	        	if (fileActionCode == 2)		// save file
	        		fileA = pgnIO.getFileArrayFromPath(path, true, fm_file_extension);
	        	else							// load/delete file
	        		fileA = pgnIO.getFileArrayFromPath(path, true, fm_file_extension);
			}
        	if (fileA != null)
        	{
		        files = new ArrayAdapter<String>(this, R.layout.c4alistitem, fileA);
		        lvFiles.setAdapter(files);
		        lvFiles.setTextFilterEnabled(true);
		        lvFiles.setVisibility(ListView.VISIBLE);
        	}
        }
		else
		{
			etPath.setText("");
			etFile.setText("");
			removeDialog(PATH_NOT_EXISTS_DIALOG);
			showDialog(PATH_NOT_EXISTS_DIALOG);
		}
	}
	public void showAssetsDir() 
	{
        lvFiles.setOnItemClickListener(this);
		files = new ArrayAdapter<String>(this, R.layout.c4alistitem, assetsDir);
        lvFiles.setAdapter(files);
        lvFiles.setTextFilterEnabled(true);
        lvFiles.setVisibility(ListView.VISIBLE);
        fmBtnAction.setVisibility(ImageView.INVISIBLE);
        fmBtnGames.setVisibility(ImageView.INVISIBLE);
 	}
	public void showAssetsFileList(String path) 
	{
		AssetManager assetManager = getAssets();
		String[] fileA = null;
		try 
		{
			lvFiles.setOnItemClickListener(this);
			fileA = assetManager.list(path);
			files = new ArrayAdapter<String>(this, R.layout.c4alistitem, fileA);
			lvFiles.setAdapter(files);
			lvFiles.setTextFilterEnabled(true);
	        lvFiles.setVisibility(ListView.VISIBLE);
	        fmBtnAction.setVisibility(ImageView.INVISIBLE);
	        fmBtnGames.setVisibility(ImageView.INVISIBLE);
		}
		catch (IOException e) {e.printStackTrace();}
	}
	public void loadFile() 
	{
// ERROR	v1.6.2		24.10.2011 20:09:40
		try
		{
			switch (fm_location) 
			{
				case 1: loadExternFile(etPath.getText().toString(), etFile.getText().toString(), 1); break;
				case 3: loadWebFile(); break;
			}
		}
		catch (NullPointerException e) {e.printStackTrace();}
	}
	public void loadExternFile(String path, String file, int gameControl) 
	{
//		Log.i(TAG, "loadExternFile(), gameControl: " + gameControl);
		returnIntent = new Intent();
		if (pgnIO.pathExists(baseDir + path))
        {
			if (!path.equals(fm_extern_load_path) | !file.equals(fm_extern_load_file))
				gameControl = 1;
        	fileData = pgnIO.dataFromFile(baseDir + path, file, fm_extern_last_game, gameControl, fm_extern_game_offset);
			if (!fileData.equals(""))
				finishAfterLoad(path, file);
			else
			{
				fileName = file;
				removeDialog(FILE_NOT_EXISTS_DIALOG);
				showDialog(FILE_NOT_EXISTS_DIALOG);
			}
	    }
		else
		{
			removeDialog(PATH_NOT_EXISTS_DIALOG);
			showDialog(PATH_NOT_EXISTS_DIALOG);
		}
	}
	
	public void finishAfterLoad(String path, String file) 
	{
		returnIntent = new Intent();
		returnIntent.putExtra("pgnStat", pgnIO.getPgnStat());
		if (!userPrefs.getBoolean("user_options_gui_usePgnDatabase", true))
		{
			setTitle(getString(R.string.fmProgressDialog));
		}
		returnIntent.putExtra("fileData", fileData);
		returnIntent.putExtra("fileBase", baseDir);
		returnIntent.putExtra("filePath", path);
		returnIntent.putExtra("fileName", file);
		setResult(RESULT_OK, returnIntent);
		if (getIntent().getExtras().getInt("displayActivity") == 1)
			setPreferences(fileData);
		else
			setSkipPreferences(1, fileData);
		finish();
	}
	public void loadWebFile()
	{
		returnIntent = new Intent();
		String data = "";
		try
		{
			data = openHttpConnection(etUrl.getText().toString());
			if (!data.equals(""))
			{
				returnIntent.putExtra("fileData", data);
				returnIntent.putExtra("fileBase", "url");
				returnIntent.putExtra("filePath", etUrl.getText().toString());
				returnIntent.putExtra("fileName", "");
				returnIntent.putExtra("pgnStat", "-");
				setResult(RESULT_OK, returnIntent);
				setPreferences("");
				finish();
			}
			else
			{
				removeDialog(WEB_FILE_NOT_EXISTS_DIALOG);
				showDialog(WEB_FILE_NOT_EXISTS_DIALOG);
			}
		}
		catch (IOException e) 
		{
			e.printStackTrace();
			removeDialog(WEB_FILE_NOT_EXISTS_DIALOG);
			showDialog(WEB_FILE_NOT_EXISTS_DIALOG);
		}
	}
	public void downloadFile(String fileUrl) 
	{
		try 
		{
            URL url = new URL(fileUrl);
            URLConnection connection = url.openConnection();
            connection.connect();
            String target = pgnIO.getExternalDirectory(0) + fmPrefs.getString("fm_extern_load_path", "") + "/test.pgn";
            InputStream input = new BufferedInputStream(url.openStream());
            OutputStream output = new FileOutputStream(target);
            byte data[] = new byte[16384];
            int count;
            while ((count = input.read(data)) != -1) 
            {
                output.write(data, 0, count);
            }
            output.flush();
            output.close();
            input.close();
        } 
		catch (Exception e) { e.printStackTrace(); }
	}
	public void saveFile(boolean append) 
	{
		returnIntent = new Intent();
		String data = getIntent().getExtras().getString("pgnData");
		fileName = etFile.getText().toString();
		if (userPrefs.getBoolean("user_options_gui_usePgnDatabase", true))
		{
			boolean createDb = true;
			boolean pgnFilesOk = pgnDb.initPgnFiles(baseDir + etPath.getText().toString(), fileName);
//			Log.i(TAG, "pgnFilesOk: " + pgnFilesOk);
			if (pgnFilesOk)
			{
				if (pgnDb.openDb(baseDir + etPath.getText().toString(), fileName, SQLiteDatabase.OPEN_READONLY))
				{
					long pgnOldLength = pgnDb.pgnLength;
					pgnIO.dataToFile(baseDir + etPath.getText().toString(), fileName, data, append);
					pgnDb.closeDb();
					createDb = false;
					notificationId++;
					createDatabaseTask = new CreateDatabaseTask(this, notificationId);
					createDatabaseTask.execute(baseDir + etPath.getText().toString(), fileName, Long.toString(pgnOldLength));
				}
			}
			if (createDb)
			{
//				Log.i(TAG, "saveFile(), openDb() false");
				pgnIO.dataToFile(baseDir + etPath.getText().toString(), fileName, data, append);
				notificationId++;
				createDatabaseTask = new CreateDatabaseTask(this, notificationId);
				createDatabaseTask.execute(baseDir + etPath.getText().toString(), fileName, "0");
			}
		}
		else
			pgnIO.dataToFile(baseDir + etPath.getText().toString(), fileName, data, append);
		returnIntent.putExtra("fileBase", baseDir);
		returnIntent.putExtra("filePath", etPath.getText().toString());
		returnIntent.putExtra("fileName", etFile.getText().toString());
		returnIntent.putExtra("pgnStat", "-");
		setResult(RESULT_OK, returnIntent);
		setPreferences("");
		closeKeyboard();
		finish();
	}
	public void deleteFile() 
	{
		fileName = etFile.getText().toString();
		if (pgnIO.fileExists(baseDir + etPath.getText().toString(), fileName))
		{
			removeDialog(DELETE_DIALOG);
			showDialog(DELETE_DIALOG);
		}
		else
		{
			removeDialog(FILE_NOT_EXISTS_DIALOG);
			showDialog(FILE_NOT_EXISTS_DIALOG);
		}
	}
	public void getPathFile() 
	{
		returnIntent = new Intent();
		returnIntent.putExtra("fileBase", baseDir);
		returnIntent.putExtra("filePath", etPath.getText().toString());
		returnIntent.putExtra("fileName", etFile.getText().toString());
		setResult(RESULT_OK, returnIntent);
		setPreferences("");
		closeKeyboard();
		finish();
	}
	public String getNewPath(String oldPath) 
	{
		String newPath = "";
		int lastDirPos = 0;
		for (int i = 0; i < oldPath.length(); i++) 
    	{
			if (oldPath.charAt(i) == '/' & i != oldPath.length() -1)
				lastDirPos = i +1;
    	}
		if (lastDirPos > 0)
			newPath = oldPath.substring(0, lastDirPos);
//		Log.i(TAG, "oldPath, newPath: " + oldPath + ", " + newPath);
		return newPath;
	}
	public void closeKeyboard() 
	{
		InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(etFile.getWindowToken(), 0);
	}
	public void getPreferences() 
	{
		fm_location = fmPrefs.getInt("fm_location", 1);
		if (fm_location == 2)
			fm_location = 1;
		fm_extern_save_path = fmPrefs.getString("fm_extern_save_path", "");
		fm_extern_save_file = fmPrefs.getString("fm_extern_save_file", "");
		fm_extern_save_auto_path = fmPrefs.getString("fm_extern_save_auto_path", "");
		fm_extern_save_auto_file = fmPrefs.getString("fm_extern_save_auto_file", "");
		fm_extern_skip_bytes = fmPrefs.getLong("fm_extern_skip_bytes", 0);
		fm_extern_game_offset = fmPrefs.getLong("fm_extern_game_offset", 0);
		if (fm_location == 1)
			fm_extern_last_game = fmPrefs.getString("fm_extern_last_game", "");
		fm_intern_path = fmPrefs.getString("fm_intern_path", "games/");			
		fm_intern_file = fmPrefs.getString("fm_intern_file", "wcc2010.pgn");	
		fm_intern_skip_bytes = fmPrefs.getLong("fm_intern_skip_bytes", 0);
		if (fm_location == 2)
			fm_intern_last_game = fmPrefs.getString("fm_intern_last_game", "");
		fm_url = fmPrefs.getString("fm_url", pgnUrl);
		if (fm_url.equals(""))
			fm_url = pgnUrl;
		if (!fmPrefs.getBoolean("fm_extern_db_app_paused", false))
		{
			showGameListView = true;
			fm_extern_load_path = fmPrefs.getString("fm_extern_load_path", "");
			fm_extern_load_file = fmPrefs.getString("fm_extern_load_file", "");
			lastFileActionCode = fmPrefs.getInt("fm_last_file_action_code", 1);
			
			fm_extern_db_game_id = fmPrefs.getInt("fm_extern_db_game_id", 0);
			fm_extern_db_game_count = fmPrefs.getInt("fm_extern_db_game_count", 0);
			fm_extern_db_game_desc = fmPrefs.getBoolean("fm_extern_db_game_desc", false);
			fm_extern_db_game_max_items = fmPrefs.getInt("fm_extern_db_game_max_items", 4000);
			fm_extern_db_cursor_id = fmPrefs.getInt("fm_extern_db_cursor_id", 0);
			fm_extern_db_cursor_count = fmPrefs.getInt("fm_extern_db_cursor_count", 0);
			fm_extern_db_key_id = fmPrefs.getInt("fm_extern_db_key_id", 0);
			fm_extern_db_game_id_list = fmPrefs.getString("fm_extern_db_game_id_list", "");
			
			fm_extern_db_key_player = fmPrefs.getString("fm_extern_db_key_player", "");
			fm_extern_db_key_player_color = fmPrefs.getInt("fm_extern_db_key_player_color", 2);
			fm_extern_db_key_player_date_from = fmPrefs.getString("fm_extern_db_key_player_date_from", "");
			fm_extern_db_key_player_date_to = fmPrefs.getString("fm_extern_db_key_player_date_to", "");
			fm_extern_db_key_player_date_desc = fmPrefs.getBoolean("fm_extern_db_key_player_date_desc", false);
			fm_extern_db_key_player_event = fmPrefs.getString("fm_extern_db_key_player_event", "");
			fm_extern_db_key_player_site = fmPrefs.getString("fm_extern_db_key_player_site", "");
			
			fm_extern_db_key_event = fmPrefs.getString("fm_extern_db_key_event", "");
			fm_extern_db_key_event_site = fmPrefs.getString("fm_extern_db_key_event_site", "");
			
			fm_extern_db_key_eco = fmPrefs.getString("fm_extern_db_key_eco", "");
			fm_extern_db_key_eco_opening = fmPrefs.getString("fm_extern_db_key_eco_opening", "");
			fm_extern_db_key_eco_variation = fmPrefs.getString("fm_extern_db_key_eco_variation", "");
			fm_extern_db_key_eco_date_from = fmPrefs.getString("fm_extern_db_key_eco_date_from", "");
			fm_extern_db_key_eco_date_to = fmPrefs.getString("fm_extern_db_key_eco_date_to", "");
			fm_extern_db_key_eco_date_desc = fmPrefs.getBoolean("fm_extern_db_key_eco_date_desc", false);
		}
		else
		{
			showGameListView = fmPrefs.getBoolean("p_extern_db_show_game_lv", true);
			fm_extern_load_path = fmPrefs.getString("p_extern_load_path", "");
			fm_extern_load_file = fmPrefs.getString("p_extern_load_file", "");
			lastFileActionCode = fmPrefs.getInt("p_last_file_action_code", 1);
			
			fm_extern_db_game_id = fmPrefs.getInt("p_extern_db_game_id", 0);
			fm_extern_db_game_count = fmPrefs.getInt("p_extern_db_game_count", 0);
			fm_extern_db_game_desc = fmPrefs.getBoolean("p_extern_db_game_desc", false);
			fm_extern_db_game_max_items = fmPrefs.getInt("p_extern_db_game_max_items", 4000);
			fm_extern_db_cursor_id = fmPrefs.getInt("p_extern_db_cursor_id", 0);
			fm_extern_db_cursor_count = fmPrefs.getInt("p_extern_db_cursor_count", 0);
			fm_extern_db_key_id = fmPrefs.getInt("p_extern_db_key_id", 0);
			fm_extern_db_game_id_list = fmPrefs.getString("p_extern_db_game_id_list", "");
			
			fm_extern_db_key_player = fmPrefs.getString("p_extern_db_key_player", "");
			fm_extern_db_key_player_color = fmPrefs.getInt("p_extern_db_key_player_color", 2);
			fm_extern_db_key_player_date_from = fmPrefs.getString("p_extern_db_key_player_date_from", "");
			fm_extern_db_key_player_date_to = fmPrefs.getString("p_extern_db_key_player_date_to", "");
			fm_extern_db_key_player_date_desc = fmPrefs.getBoolean("p_extern_db_key_player_date_desc", false);
			fm_extern_db_key_player_event = fmPrefs.getString("p_extern_db_key_player_event", "");
			fm_extern_db_key_player_site = fmPrefs.getString("p_extern_db_key_player_site", "");
			
			fm_extern_db_key_event = fmPrefs.getString("p_extern_db_key_event", "");
			fm_extern_db_key_event_site = fmPrefs.getString("p_extern_db_key_event_site", "");
			
			fm_extern_db_key_eco = fmPrefs.getString("p_extern_db_key_eco", "");
			fm_extern_db_key_eco_opening = fmPrefs.getString("p_extern_db_key_eco_opening", "");
			fm_extern_db_key_eco_variation = fmPrefs.getString("p_extern_db_key_eco_variation", "");
			fm_extern_db_key_eco_date_from = fmPrefs.getString("p_extern_db_key_eco_date_from", "");
			fm_extern_db_key_eco_date_to = fmPrefs.getString("p_extern_db_key_eco_date_to", "");
			fm_extern_db_key_eco_date_desc = fmPrefs.getBoolean("p_extern_db_key_eco_date_desc", false);
			setAppPausedPref(false);
		}
		if (fm_extern_db_key_id == 0)
			scroll_game_id = fm_extern_db_game_id;
		else
		{
			gameIdList = getGameIdList(fm_extern_db_game_id_list);
			if (gameIdList != null)
			{
//				Log.i(TAG, "gameIdList.length: " + gameIdList.length);
				fm_extern_db_cursor_count = gameIdList.length;
			}
			scroll_game_id = fm_extern_db_cursor_id +1;
		}
	}
	public void setPreferences(String gameData) 
	{
        SharedPreferences.Editor ed = fmPrefs.edit();
        ed.putInt("fm_location", fm_location);
        if (fileActionCode == 1)
        	ed.putInt("fm_load_location", fm_location);
//        Log.i(TAG, "fm_location, fileActionCode: " + fm_location + ", " + fileActionCode);
        switch (fm_location) 
        { 
        case 1:
        	if (fileActionCode == 1)	// load
        	{
        		if (!gameData.equals(""))
        		{
	        		ed.putInt("fm_last_file_action_code", fileActionCode);
		        	ed.putString("fm_extern_load_path", etPath.getText().toString());
		        	ed.putString("fm_extern_load_file", etFile.getText().toString());
//		        	Log.i(TAG, "pgnDb.pgnGameOffset, pgnIO.gameOffset: " + pgnDb.pgnGameOffset + ", " + pgnIO.gameOffset);
		        	if (userPrefs.getBoolean("user_options_gui_usePgnDatabase", true))
		        		ed.putLong("fm_extern_game_offset", pgnDb.pgnGameOffset);
		        	else
		        		ed.putLong("fm_extern_game_offset", pgnIO.gameOffset);
		        	ed.putString("fm_extern_last_game", gameData);
		        	if (userPrefs.getBoolean("user_options_gui_usePgnDatabase", true))
		        		setDbPreferences();
        		}
        	}
        	if (fileActionCode == 2)	// save
        	{
        		ed.putInt("fm_last_file_action_code", fileActionCode);
	        	ed.putString("fm_extern_save_path", etPath.getText().toString());
	        	ed.putString("fm_extern_save_file", etFile.getText().toString());
	        	if (fmPrefs.getString("fm_extern_load_file", "").equals(""))	// no load prefs?, set save prefs 
	        	{
	        		ed.putString("fm_extern_load_path", etPath.getText().toString());
		        	ed.putString("fm_extern_load_file", etFile.getText().toString());
		        	ed.putLong("fm_extern_skip_bytes", 0);
	        	}
        	}
        	if (fileActionCode == 5)	// save engine autoPlay
        	{
        		ed.putInt("fm_last_file_action_code", fileActionCode);
	        	ed.putString("fm_extern_save_auto_path", etPath.getText().toString());
	        	ed.putString("fm_extern_save_auto_file", etFile.getText().toString());
        	}
        	break;
		case 2:
        	ed.putString("fm_intern_path", etPath.getText().toString());
        	ed.putString("fm_intern_file", etFile.getText().toString());
        	ed.putString("fm_intern_last_game", gameData);
			break;
		case 3:
			if (!etUrl.getText().toString().equals(""))
				ed.putString("fm_url", etUrl.getText().toString());
			break;
        }
        ed.commit();
	}
	public void setSkipPreferences(int location, String gameData) 
	{
        SharedPreferences.Editor ed = fmPrefs.edit();
        if (location == 1)
        {
//        	Log.i(TAG, "\nfm_extern_skip_bytes: " + pgnIO.getSkipBytes());
        	if (!gameData.equals(""))
        	{
	        	ed.putString("fm_extern_last_game", gameData);
	        	if (userPrefs.getBoolean("user_options_gui_usePgnDatabase", true))
	        		ed.putLong("fm_extern_game_offset", pgnDb.pgnGameOffset);
	        	else
	        		ed.putLong("fm_extern_game_offset", pgnIO.gameOffset);
	        	if (userPrefs.getBoolean("user_options_gui_usePgnDatabase", true))
	        		setDbPreferences();
	        	else
	        		ed.putLong("fm_extern_game_offset", pgnIO.gameOffset);
        	}
        }
        if (location == 2)
        {
        	ed.putString("fm_intern_last_game", gameData);
        }
        ed.commit();
	}
	public void setDbPreferences() 
	{
		setAppPausedPref(false);
		SharedPreferences.Editor edDb = fmPrefs.edit();
		
		edDb.putInt("fm_extern_db_game_id", pgnDb.pgnGameId);
		edDb.putInt("fm_extern_db_game_count", pgnDb.pgnGameCount);
		edDb.putBoolean("fm_extern_db_game_desc", fm_extern_db_game_desc);
		edDb.putInt("fm_extern_db_game_max_items", fm_extern_db_game_max_items);
		edDb.putInt("fm_extern_db_cursor_id", fm_extern_db_cursor_id);			
		edDb.putInt("fm_extern_db_cursor_count", fm_extern_db_cursor_count);		
		edDb.putInt("fm_extern_db_key_id", fm_extern_db_key_id);					
		if (fm_extern_db_key_id != 0)
			edDb.putInt("fm_extern_db_key_index", fm_extern_db_key_id);					
		edDb.putString("fm_extern_db_game_id_list", fm_extern_db_game_id_list);	
		edDb.putString("fm_extern_db_key_info", getDbKeyInfo(fm_extern_db_key_id));
		
		edDb.putString("fm_extern_db_key_player", fm_extern_db_key_player);		// ???
		edDb.putInt("fm_extern_db_key_player_color", fm_extern_db_key_player_color);
		edDb.putString("fm_extern_db_key_player_date_from", fm_extern_db_key_player_date_from);	
		edDb.putString("fm_extern_db_key_player_date_to", fm_extern_db_key_player_date_to);
		edDb.putBoolean("fm_extern_db_key_player_date_desc", fm_extern_db_key_player_date_desc);
		edDb.putString("fm_extern_db_key_player_event", fm_extern_db_key_player_event);
		edDb.putString("fm_extern_db_key_player_site", fm_extern_db_key_player_site);
		
		edDb.putString("fm_extern_db_key_event", fm_extern_db_key_event);
		edDb.putString("fm_extern_db_key_event_site", fm_extern_db_key_event_site);
		
		edDb.putString("fm_extern_db_key_eco", fm_extern_db_key_eco);
		edDb.putString("fm_extern_db_key_eco_opening", fm_extern_db_key_eco_opening);
		edDb.putString("fm_extern_db_key_eco_variation", fm_extern_db_key_eco_variation);
		edDb.putString("fm_extern_db_key_eco_date_from", fm_extern_db_key_eco_date_from);
		edDb.putString("fm_extern_db_key_eco_date_to", fm_extern_db_key_eco_date_to);
		edDb.putBoolean("fm_extern_db_key_eco_date_desc", fm_extern_db_key_eco_date_desc);
		
		edDb.commit();
	}
	public void setQueryPreferences(int gameId) 
	{
		SharedPreferences.Editor ed = fmPrefs.edit();
		String white = "";
		String black = "";
		String event = "";
		String site = "";
		String eco = "";
		String opening = "";
		String variation = "";
		if (pgnDb.openDb(baseDir + fm_extern_load_path, fm_extern_load_file, SQLiteDatabase.OPEN_READONLY))
		{
			pgnDb.getFieldsFromGameId(gameId);
			pgnDb.closeDb();
			white = pgnDb.fCur.getString(pgnDb.fCur.getColumnIndex("White"));		
			black = pgnDb.fCur.getString(pgnDb.fCur.getColumnIndex("Black"));		
			event = pgnDb.fCur.getString(pgnDb.fCur.getColumnIndex("Event"));		
			site = pgnDb.fCur.getString(pgnDb.fCur.getColumnIndex("Site"));		
			eco = pgnDb.fCur.getString(pgnDb.fCur.getColumnIndex("ECO"));		
			opening = pgnDb.fCur.getString(pgnDb.fCur.getColumnIndex("Opening"));		
			variation = pgnDb.fCur.getString(pgnDb.fCur.getColumnIndex("Variation"));		
//			Log.i(TAG, "gameId, white: " + gameId + ", " + white);
			ed.putString("fm_query_white", white);
			ed.putString("fm_query_black", black);
			ed.putString("fm_query_event", event);
			ed.putString("fm_query_site", site);
			ed.putString("fm_query_eco", eco);
			ed.putString("fm_query_opening", opening);
			ed.putString("fm_query_variation", variation);
			ed.commit();
		}
	}
	public void setAppPausedPreferences() 
	{
       	if (fm_location == 1 & fileActionCode == 1 & getIntent().getExtras().getInt("displayActivity") == 1)	// external storage & load
       		setAppPausedPref(true);
       	else
       	{
       		setAppPausedPref(false);
       		return;
       	}
		SharedPreferences.Editor edDb = fmPrefs.edit();
		
		edDb.putInt("p_last_file_action_code", fileActionCode);
		try
		{
			edDb.putString("p_extern_load_path", etPath.getText().toString());
	    	edDb.putString("p_extern_load_file", etFile.getText().toString());
	    	showGameListView = true;
            // if (lvGames.getVisibility() != 0)
	    	if (lvGames.getVisibility() != View.VISIBLE)
	    		showGameListView = false;
		}
    	catch (NullPointerException e) 
    	{
    		edDb.putString("p_extern_load_path", fm_extern_load_path);
	    	edDb.putString("p_extern_load_file", fm_extern_load_file);
    		showGameListView = true;
    	} 
    	edDb.putBoolean("p_extern_db_show_game_lv", showGameListView);
		edDb.putInt("p_extern_db_game_id", pgnDb.pgnGameId);
		edDb.putInt("p_extern_db_game_count", pgnDb.pgnGameCount);
		edDb.putBoolean("p_extern_db_game_desc", fm_extern_db_game_desc);
		edDb.putInt("p_extern_db_game_max_items", fm_extern_db_game_max_items);
		edDb.putInt("p_extern_db_cursor_id", fm_extern_db_cursor_id);			
		edDb.putInt("p_extern_db_cursor_count", fm_extern_db_cursor_count);		
		edDb.putInt("p_extern_db_key_id", fm_extern_db_key_id);					
		if (fm_extern_db_key_id != 0)
			edDb.putInt("p_extern_db_key_index", fm_extern_db_key_id);					
		edDb.putString("p_extern_db_game_id_list", fm_extern_db_game_id_list);	
		edDb.putString("p_extern_db_key_info", getDbKeyInfo(fm_extern_db_key_id));
		
		edDb.putString("p_extern_db_key_player", fm_extern_db_key_player);		// ???
		edDb.putInt("p_extern_db_key_player_color", fm_extern_db_key_player_color);
		edDb.putString("p_extern_db_key_player_date_from", fm_extern_db_key_player_date_from);	
		edDb.putString("p_extern_db_key_player_date_to", fm_extern_db_key_player_date_to);
		edDb.putBoolean("p_extern_db_key_player_date_desc", fm_extern_db_key_player_date_desc);
		edDb.putString("p_extern_db_key_player_event", fm_extern_db_key_player_event);
		edDb.putString("p_extern_db_key_player_site", fm_extern_db_key_player_site);
		
		edDb.putString("p_extern_db_key_event", fm_extern_db_key_event);
		edDb.putString("p_extern_db_key_event_site", fm_extern_db_key_event_site);
		
		edDb.putString("p_extern_db_key_eco", fm_extern_db_key_eco);
		edDb.putString("p_extern_db_key_eco_opening", fm_extern_db_key_eco_opening);
		edDb.putString("p_extern_db_key_eco_variation", fm_extern_db_key_eco_variation);
		edDb.putString("p_extern_db_key_eco_date_from", fm_extern_db_key_eco_date_from);
		edDb.putString("p_extern_db_key_eco_date_to", fm_extern_db_key_eco_date_to);
		edDb.putBoolean("p_extern_db_key_eco_date_desc", fm_extern_db_key_eco_date_desc);
		
		edDb.commit();
	}
	public String getGameIdStringList(Cursor cursor)
    {
		String list = "";
		if (cursor.getCount() <= 0)
			return "";
		if (cursor.moveToFirst()) 
		{
	        do 		{list = list + Integer.toString(cursor.getInt(cursor.getColumnIndex("_id"))) + ";";} 
	        while 	(cursor.moveToNext());
	    }
		return list;
    }
	public String[] getGameIdList(String gl)
    {
//		Log.i(TAG, "gameIdList: " + gl);
		String[] splitGl = gl.split(";");
		return splitGl;
    }
	private String openHttpConnection(String urlString) throws IOException
    {
		String data = "";
        InputStream in = null;
        int response = -1;
               
        URL url = new URL(urlString); 
        URLConnection conn = url.openConnection();
                 
        if (!(conn instanceof HttpURLConnection))                     
            throw new IOException("Not an HTTP connection");
        try{
            HttpURLConnection httpConn = (HttpURLConnection) conn;
            httpConn.setAllowUserInteraction(false);
            httpConn.setInstanceFollowRedirects(true);
            httpConn.setRequestMethod("GET");
            httpConn.connect(); 

            response = httpConn.getResponseCode(); 
            if (response == HttpURLConnection.HTTP_OK) 
            {
                in = httpConn.getInputStream();
                data = pgnIO.getDataFromInputStream(in);
            }                     
        }
        catch (Exception ex)
        {
            throw new IOException("Error connecting");            
        }
        return data;     
    }
	//	helpers			helpers			helpers			helpers			helpers			
	public boolean getGameIdValues() 
	{
        pgnDb.initPgnFiles(baseDir + fm_extern_load_path, fm_extern_load_file);
 		if (pgnDb.openDb(baseDir + fm_extern_load_path, fm_extern_load_file, SQLiteDatabase.OPEN_READONLY))
		{
			pgnDb.getFieldsFromGameId(fm_extern_db_game_id);
			pgnDb.closeDb();
			return true;
		}
 		return false;
	}
	public void setPathValues() 
	{
		fm_extern_load_path = etPath.getText().toString();
		fm_extern_load_file = etFile.getText().toString();
	}
	public void setAppPausedPref(boolean paused) 
	{
        SharedPreferences.Editor ed = fmPrefs.edit();
        ed.putBoolean("fm_extern_db_app_paused", paused);
        ed.commit();
	}
	public void setPlayerData() 
	{
        int color = 2;
        if (qWhiteRb.isChecked()) color = 0;
        if (qBlackRb.isChecked()) color = 1;
        fm_extern_db_key_player = qPlayer.getText().toString();
		fm_extern_db_key_player_color = color;
		fm_extern_db_key_player_date_from = qDateFrom.getText().toString();
		fm_extern_db_key_player_date_to = qDateTo.getText().toString();
		fm_extern_db_key_player_date_desc = qDateDescCb.isChecked();
		try
        {
			fm_extern_db_key_player_event = qEvent.getText().toString();
			fm_extern_db_key_player_site = qSite.getText().toString();
        }
		catch (NullPointerException e) {} 
	}
	public void setEventData() 
	{
		fm_extern_db_key_event = qeEvent.getText().toString();
		fm_extern_db_key_event_site = qeSite.getText().toString();
	}
	public void setEcoData() 
	{
		fm_extern_db_key_eco = qoEco.getText().toString();
		fm_extern_db_key_eco_opening = qoOpening.getText().toString();
		fm_extern_db_key_eco_variation = qoVariation.getText().toString();
		fm_extern_db_key_eco_date_from = qoDateFrom.getText().toString();
		fm_extern_db_key_eco_date_to = qoDateTo.getText().toString();
		fm_extern_db_key_eco_date_desc = qoDateDescCb.isChecked();
	}
	public void setQueryDataToTitle(int keyId, int gameId, int gameCount, int queryId, int queryCount) 
	{
		if (keyId == 0)
			this.setTitle(gameId + "[" + gameCount + "]");
		else
			this.setTitle(queryId + "(" + queryCount + "), " + gameId + "[" + gameCount + "]");
	}
	public void dataQueryGameID() 
	{
    	setQueryData = SET_QUERY_DATA_GAME;
    	isQuery = true;
    	queryGameIdLayout.setVisibility(RelativeLayout.VISIBLE);
    	qGameCount.setText(Integer.toString(fm_extern_db_game_count));
    	qGameDescCb.setChecked(fm_extern_db_game_desc);
     	qCurrentId.setText(Integer.toString(fm_extern_db_game_id));
    	qMaxItems.setText(Integer.toString(fm_extern_db_game_max_items));
	}
	public void dataQueryPlayer() 
	{
    	setQueryData = SET_QUERY_DATA_PLAYER;
    	isQuery = true;
    	queryPlayerLayout.setVisibility(RelativeLayout.VISIBLE);
   		qPlayer.setText(fm_extern_db_key_player);
    	switch (fm_extern_db_key_player_color)
        {
        	case 0: qWhiteRb.setChecked(true);		break;	// white
        	case 1: qBlackRb.setChecked(true);		break;	// black
	        case 2: qWhiteBlackRb.setChecked(true); break;	// white & black
        }
    	qDateDescCb.setChecked(fm_extern_db_key_player_date_desc);
    	qDateFrom.setText(fm_extern_db_key_player_date_from);
    	if (isDateOk(qDateFrom.getText().toString()))
			qDateFrom.setBackgroundResource(R.drawable.bordergreen);
		else
		{
			qDateFrom.setBackgroundResource(R.drawable.borderpink);
			isQueryInputError = true;
		}
    	qDateTo.setText(fm_extern_db_key_player_date_to);
    	if (isDateOk(qDateTo.getText().toString()))
    		qDateTo.setBackgroundResource(R.drawable.bordergreen);
		else
		{
			qDateTo.setBackgroundResource(R.drawable.borderpink);
			isQueryInputError = true;
		}
    	try
    	{
	    	qEvent.setText(fm_extern_db_key_player_event);
	    	qSite.setText(fm_extern_db_key_player_site);
    	}
    	catch (NullPointerException e) {} 
	}
	public void dataQueryEvent() 
	{
		setQueryData = SET_QUERY_DATA_EVENT;
    	isQuery = true;
    	queryEventLayout.setVisibility(RelativeLayout.VISIBLE);
//    	Log.i(TAG, "queryFromGameId, isGameValues: " + queryFromGameId + ", " + isGameValues);
    	if (isGameValues)
    	{
    		qeEvent.setText(pgnDb.fCur.getString(pgnDb.fCur.getColumnIndex("Event")));
        	qeSite.setText(pgnDb.fCur.getString(pgnDb.fCur.getColumnIndex("Site")));
    	}
    	else
    	{
	    	qeEvent.setText(fm_extern_db_key_event);
	    	qeSite.setText(fm_extern_db_key_event_site);
    	}
	}
	public void dataQueryEco() 
	{
		setQueryData = SET_QUERY_DATA_ECO;
    	isQuery = true;
    	queryEcoLayout.setVisibility(RelativeLayout.VISIBLE);
    	if (isGameValues)
    	{
    		qoEco.setText(pgnDb.fCur.getString(pgnDb.fCur.getColumnIndex("ECO")));
        	qoOpening.setText(pgnDb.fCur.getString(pgnDb.fCur.getColumnIndex("Opening")));
        	qoVariation.setText(pgnDb.fCur.getString(pgnDb.fCur.getColumnIndex("Variation")));
    	}
    	else
    	{
	    	qoEco.setText(fm_extern_db_key_eco);
	    	qoOpening.setText(fm_extern_db_key_eco_opening);
	    	qoVariation.setText(fm_extern_db_key_eco_variation);
    	}
    	qoDateDescCb.setChecked(fm_extern_db_key_eco_date_desc);
    	qoDateFrom.setText(fm_extern_db_key_eco_date_from);
    	qoDateTo.setText(fm_extern_db_key_eco_date_to);
	}
	
	public void displayQueryInfo(String line1, String line2) 
	{
		queryInfo1.setVisibility(ImageView.VISIBLE);
		queryInfo2.setVisibility(ImageView.VISIBLE);
		queryInfo1.setText(line1);
		queryInfo2.setText(line2);
	}
	public String getDbKeyInfo(int keyId) 
	{
		switch (keyId) 
		{
			case SET_QUERY_DATA_PLAYER:		// white | black | white&black	
				return ">" + fm_extern_db_key_player + "<"; 
			case SET_QUERY_DATA_EVENT:
				return ">" 	+ fm_extern_db_key_event + ", " 
							+ fm_extern_db_key_event_site + "<";
			case SET_QUERY_DATA_ECO:
				return 							">"	+ fm_extern_db_key_eco + ", "
												+ fm_extern_db_key_eco_opening + "; "
												+ fm_extern_db_key_eco_variation + "<";
			case SET_QUERY_DATA_GAME:
			default:	// gameId(primary)	
				return ">" + getString(R.string.qgGameId) + "<"; 
		}
	}
	//	pgn-db methods			pgn-db methods			pgn-db methods			pgn-db methods
	public class CreateDatabaseTask extends AsyncTask<String, Long, Void>
	{
		public CreateDatabaseTask(Context context, int notificationId)
		{
	        this.context = context;
	        this.notificationId = notificationId;
	        fmPrefs = context.getSharedPreferences("fm", 0);
	        mNotificationHelper = new NotificationHelper(context, notificationId);
	    }
	 	@Override
		protected Void doInBackground(String... params) 
		{
			pgnPath = params[0];
			pgnFile = params[1];
			try {pgnOffset = Long.parseLong(params[2]);}
			catch (NumberFormatException e) {pgnOffset = 0;}
			boolean dropCreateIdx = false;
			boolean dropIdx = false;
			if (params.length == 4)
			{
				if (params[3].equals("idx"))
					dropCreateIdx = true;
				if (params[3].equals("dropidx"))
					dropIdx = true;
			}
			fPgn = new File(pgnPath + pgnFile);
			if (fPgn.exists())
				pgnFileExists = true;
			else
			{
				pgnFileExists = false;
				return null;
			}
			pgnDbFile = pgnFile.replace(".pgn", ".pgn-db");
			mNotificationHelper.createNotification(" " + pgnDbFile);
			fPgnDb = new File(pgnPath + pgnDbFile);
			if (pgnOffset == 0 & !dropCreateIdx)
			{
//				db = SQLiteDatabase.openOrCreateDatabase(fPgnDb, null);				// create database (file: .pgn-db)
// ERROR	v1.36.1		05.04.2013
				try	{db = SQLiteDatabase.openOrCreateDatabase(fPgnDb, null);}	// create database (file: .pgn-db)
				catch (SQLiteDiskIOException e) {e.printStackTrace(); testMessage = "EX 99"; db.close(); return null;}
	    		executeSQLScript(db, "create.sql");								// create table: pgn
	    		db.close();
			}
			try	{db = SQLiteDatabase.openDatabase(pgnPath + pgnDbFile, null, SQLiteDatabase.OPEN_READWRITE);} 
			catch (SQLiteException e) {e.printStackTrace(); db = null;}
			try 
			{
				if (dropIdx)
				{
					publishProgress(998L, rafParsed, rafLength, GameId);
					executeSQLScript(db, "drop_idx.sql");		// drop indexes ON pgn
				}
				if (dropCreateIdx)
				{
//					Log.i(TAG, "dropCreateIdx");
					publishProgress(999L, rafParsed, rafLength, GameId);
					executeSQLScript(db, "drop_idx.sql");		// drop indexes ON pgn
					executeSQLScript(db, "create_idx.sql");		// create indexes ON pgn
					db.setVersion(pgnDb.DB_VERSION);
					db.close();
					testMessage = "dropCreateIdx - 0";
					return null;
				}
				pgnRaf = new RandomAccessFile(pgnPath + pgnFile, "r");
				rafParsed = +pgnOffset;
				rafLength = pgnRaf.length();
				GameId = 0;
//				Log.i(TAG, "rafParsed, rafLength, GameId: " + rafParsed + ", " + rafLength + ", " + GameId);
				publishProgress(getPercentageComplete(), rafParsed, rafLength, GameId);
			} 
			catch (FileNotFoundException e) {e.printStackTrace(); testMessage = "EX 1"; db.close(); return null;}	// error file not exist
			catch (IOException e) {e.printStackTrace(); testMessage = "EX 2"; db.close(); return null;}	
			try 
			{
				initGameValues();
				pgnRaf.seek(pgnOffset);
				rafLinePointer = pgnRaf.getFilePointer();
				line = pgnRaf.readLine();
				while (line != null)
				{
//					Log.i(TAG, "line: " + line);
					if (line.startsWith("[Event "))
					{
						hasGames = true;
						if (isFirstGame)
							isFirstGame = false;
						else
						{
							gameLength = (int) (rafLinePointer - rafGamePointer);
							setGameOffsets(rafGamePointer, gameLength, startMoveSection);
							if (!insertValuesToDb())
							{	// SQLException
								pgnRaf.close();
								db.close();
								pgnDb.deleteDbFile();
								testMessage = "insertValuesToDb() - 1";
								return null;
							}
							if (GameId % 25 == 0)
								publishProgress(getPercentageComplete(), rafParsed, rafLength, GameId);
							initGameValues();
						}
						isTagSection = true;
						rafGamePointer = rafLinePointer;
					}
					if (isTagSection & line.startsWith("[") & line.contains("]"))
						setTagValue(line);
					else
					{
						if (isTagSection)
						{
							isTagSection = false;
							startMoveSection = (int) (rafLinePointer - rafGamePointer);
						}
					}
					rafLinePointer = pgnRaf.getFilePointer();
					line = pgnRaf.readLine();
				}
				if (hasGames)
				{
					gameLength = (int) (pgnRaf.length() - rafGamePointer);
					setGameOffsets(rafGamePointer, gameLength, startMoveSection);
					if (!insertValuesToDb())
					{	// SQLException
						pgnRaf.close();
						db.close();
						pgnDb.deleteDbFile();
						testMessage = "insertValuesToDb() - 2";
						return null;
					}
					publishProgress(getPercentageComplete(), rafParsed, rafLength, GameId);
				}
				pgnRaf.close();
			} 
			catch (IOException e) {e.printStackTrace(); db.close(); testMessage = "EX 3"; return null;}
			catch (SQLiteDiskIOException e) {e.printStackTrace(); db.close(); testMessage = "EX 4"; return null;}
			catch (SQLiteDatabaseCorruptException e) {e.printStackTrace(); db.close(); testMessage = "EX 5"; return null;}
			
			if (pgnOffset == 0 | dropIdx)
			{
				publishProgress(999L, rafParsed, rafLength, GameId);
		 		executeSQLScript(db, "create_idx.sql");		// create indexes ON pgn
		 		db.setVersion(pgnDb.DB_VERSION);
			}
			String pgnDbFileJournal = pgnFile.replace(".pgn", ".pgn-db-journal");
			File fPgnDbJournal = new File(pgnPath + pgnDbFileJournal);
			if (fPgnDbJournal.exists())
				fPgnDbJournal.delete();
			db.close();
			testMessage = "OK!";
			return null;
		}
		@Override  
		protected void onProgressUpdate(Long... progress) 
		{
	        mNotificationHelper.progressUpdate(progress[0], progress[1], progress[2], progress[3]);
	    }
		@Override  
	    protected void onPostExecute(Void l) 
		{  
			if (!pgnFileExists)
	        {
	        	fileName = pgnPath + pgnFile;
				removeDialog(FILE_NOT_EXISTS_DIALOG);
				showDialog(FILE_NOT_EXISTS_DIALOG);
	        	return;
	        }
	        mNotificationHelper.completed();
//	        Log.i(TAG, "notificationId: " + notificationId);
//	        Log.i(TAG, "pgnPath, pgnFile: " + pgnPath + pgnFile);
	        if (getIntent().getExtras().getInt("displayActivity") == 1)
	        {
	        	pgnDb.openDb(pgnPath, pgnFile, SQLiteDatabase.OPEN_READONLY);
		        setInfoValues("DB-Test - onPostExecute()", testMessage, pgnDb.getDbVersion());
		        pgnDb.closeDb();
		        if (pgnPath.equals(baseDir + etPath.getText().toString()) & pgnFile.equals(etFile.getText().toString()))
		        {
		        	if (pgnDb.initPgnFiles(pgnPath, pgnFile))
		        	{
		        		fm_extern_db_key_id = 0;
						displayGames(pgnPath, pgnFile, false, true);
		        	}
		        }
	        }
	    }
		private void executeSQLScript(SQLiteDatabase database, String dbname) 
		{
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		    byte buf[] = new byte[1024];
		    int len;
		    AssetManager assetManager = getAssets();
		    InputStream inputStream = null;
		    try
		    {
		        inputStream = assetManager.open(dbname);
		        while ((len = inputStream.read(buf)) != -1) 
		        {
		            outputStream.write(buf, 0, len);
		        }
		        outputStream.close();
		        inputStream.close();
		        String[] createScript = outputStream.toString().split(";");
		        for (int i = 0; i < createScript.length; i++) 
		        {
		        	String sqlStatement = createScript[i].trim();
		            if (sqlStatement.length() > 0) 
		            {
		            	database.execSQL(sqlStatement + ";");
		            }
		        }
		    } 
		    catch (IOException e)	{e.printStackTrace();} 
		    catch (SQLException e) 	{e.printStackTrace();}
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
			String nextTag = "";
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
//			Log.i(TAG, "tagName, tagValue: " + tagName + ", " + tagValue);
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
			if (!nextTag.equals(""))
				setTagValue(nextTag);
	    }
		public void setGameOffsets(long gameFileOffset, int gameLength, int gameMovesOffset)
	    {
			GameFileOffset = gameFileOffset;
			GameLength = gameLength;
			GameMovesOffset = gameMovesOffset;
	    }
		public boolean insertValuesToDb()
	    {
			Long pgnId = 0L;
			rafParsed = rafParsed +GameLength;
			ContentValues cv = new ContentValues();
			cv.put("GameFileOffset", GameFileOffset);
			cv.put("GameLength", GameLength);
			cv.put("GameMovesOffset", GameMovesOffset);
			cv.put("Event", Event);
			cv.put("Site", Site);
			cv.put("Date", Date);
			cv.put("Round", Round);
			cv.put("White", White);
			cv.put("Black", Black);
			cv.put("Result", Result);
			cv.put("SetUp", SetUp);
			cv.put("FEN", FEN);
			cv.put("WhiteTitle", WhiteTitle);
			cv.put("BlackTitle", BlackTitle);
			cv.put("WhiteElo", WhiteElo);
			cv.put("BlackElo", BlackElo);
			cv.put("ECO", ECO);
			cv.put("Opening", Opening);
			cv.put("Variation", Variation);
			cv.put("WhiteTeam", WhiteTeam);
			cv.put("BlackTeam", BlackTeam);
			cv.put("WhiteFideId", WhiteFideId);
			cv.put("BlackFideId", BlackFideId);
			cv.put("EventDate", EventDate);
			cv.put("EventType", EventType);
			try 					{pgnId = db.insertOrThrow(TABLE_NAME, null, cv);}
			catch (SQLException e) 	{e.printStackTrace(); GameId = 0; return false;}
			if (pgnId >= 0)
				GameId = pgnId +1;
			return true;
	    }
		public Long getPercentageComplete()
	    {
			long percentage = 0;
// ERROR	v1.34.1		15.02.2013
			try
			{
				percentage = 0;
				percentage = (rafParsed * 100) / rafLength;
				return percentage;
			}
			catch (ArithmeticException e) {return percentage;}
	    }
		
		final String TAG = "CreateDatabaseTask";
		Context context;
		SQLiteDatabase db = null;	//	database instance from file .pgn-db
		SharedPreferences fmPrefs;
		private NotificationHelper mNotificationHelper;
		int notificationId = 0;
		String pgnPath = "";
		String pgnFile = "";
		long pgnOffset = 0;
		String pgnDbFile = pgnFile.replace(".pgn", ".pgn-db");
		File fPgn;
		File fPgnDb;
		FileChannel channelPgn;
		FileChannel channelPgnDb;
		FileLock lockPgn;
		FileLock lockPgnDb;
		public static final String TABLE_NAME = "pgn";
		// pgn-db columns
		long GameId = 0;
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
		// doInBackground() vars
		RandomAccessFile pgnRaf = null;
		boolean pgnFileExists = true;
		boolean hasGames = false;
		boolean isFirstGame = true;
		boolean isTagSection = false;
		long rafLength = 0;
		long rafParsed = 0;
		long rafLinePointer = 0;
		long rafGamePointer = 0;
		int startMoveSection = 0;
		int gameLength = 0;
		String line;
	}
	public class QueryTask extends AsyncTask<String, Void, Void>
	{
		@Override
		protected Void doInBackground(String... params) 
		{
			gamePath = params[0];
			gameFile = params[1];
			setInfo = false;
			if (params[2].equals("true"))
				setInfo = true;
			isNewFile = false;
			if (params[3].equals("true"))
				isNewFile = true;
			if (pgnDb.openDb(gamePath, gameFile, SQLiteDatabase.OPEN_READONLY))
			{
				int rowCnt = pgnDb.getRowCount(PgnDb.TABLE_NAME);
				if (isNewFile & rowCnt > 0)
				{
					if (fm_extern_db_game_desc)
					{
						fm_extern_db_game_id = rowCnt;
						scroll_game_id = rowCnt;
					}
					else
					{
						fm_extern_db_game_id = 1;
						scroll_game_id = 1;
					}
					fm_extern_db_game_count = rowCnt;		
				}
				
				pl = fm_extern_db_key_player;
				pl_color = fm_extern_db_key_player_color;
				pl_date_from = fm_extern_db_key_player_date_from;
				pl_date_to = fm_extern_db_key_player_date_to;
				pl_date_desc = fm_extern_db_key_player_date_desc;
				pl_event = fm_extern_db_key_player_event;
				pl_site = fm_extern_db_key_player_site;
				
				ev_event = fm_extern_db_key_event;
				ev_site = fm_extern_db_key_event_site;
				
				ec_eco = fm_extern_db_key_eco;
				ec_opening = fm_extern_db_key_eco_opening;
				ec_variation = fm_extern_db_key_eco_variation;
				ec_date_from = fm_extern_db_key_eco_date_from;
				ec_date_to = fm_extern_db_key_eco_date_to;
				ec_date_desc = fm_extern_db_key_eco_date_desc;
				switch (fm_extern_db_key_id) 
				{
					case SET_QUERY_DATA_PLAYER:		// white | black | white&black	
						queryCursor = pgnDb.queryPgnIdxPlayer(pl, pl_color, pl_date_from, pl_date_to, pl_date_desc, pl_event, pl_site);
						break;	
					case SET_QUERY_DATA_EVENT:
						queryCursor = pgnDb.queryPgnIdxEvent(ev_event, ev_site);
						break;
					case SET_QUERY_DATA_ECO:
						queryCursor = pgnDb.queryPgnIdxEco(ec_eco, ec_opening, ec_variation, ec_date_from, ec_date_to, ec_date_desc);
						break;
					case SET_QUERY_DATA_GAME:
					default:	// gameId(primary)	
						queryCursor = pgnDb.queryPgn(scroll_game_id, fm_extern_db_game_max_items, fm_extern_db_game_desc);
						break;							
				}
			}
			return null;
		}
		protected void onPreExecute() 
		{
			isQueryResult = true;
			if (fileActionCode == 1)
			{
// ERROR	v1.35	22.02.2013
				try
				{
					removeDialog(QUERY_PROGRESS_DIALOG);
		    		showDialog(QUERY_PROGRESS_DIALOG);
				}
	    		catch (BadTokenException e) { }
			}
		}
		@Override  
	    protected void onPostExecute(Void l) 
		{  
			if (progressDialog != null)
	    	{
		    	if (progressDialog.isShowing())
		     		dismissDialog(QUERY_PROGRESS_DIALOG);
	    	}
			if (queryCursor == null)
			{
				pgnDb.closeDb();
				lvGames.setAdapter(null);
				isQueryResult = false;
				emptyLv.setVisibility(TextView.VISIBLE);
				return;
			}
			switch (fm_extern_db_key_id) 
			{
				case SET_QUERY_DATA_PLAYER:		// white | black | white&black	
					displayQueryInfo(getString(R.string.qPlayer) + ":", pl);
					break;	
				case SET_QUERY_DATA_EVENT:
					displayQueryInfo(ev_event, ev_site);
					break;
				case SET_QUERY_DATA_ECO:
					displayQueryInfo(ec_eco + ", " + ec_opening, ec_variation);
					break;
				case SET_QUERY_DATA_GAME:
				default:	// gameId(primary)	
					displayQueryInfo(getString(R.string.qgGameId) + ":", Integer.toString(fm_extern_db_game_id));
					break;							
			}
			gameCursor = queryCursor;
			int queryCount = 0;
			if (fm_extern_db_key_id != 0)
				queryCount = queryCursor.getCount();
//			Log.i(TAG, "fm_extern_db_key_id, scroll_game_id: " + fm_extern_db_key_id + ", " + scroll_game_id);
			int gameId = 0;
			if (queryCount > 0)
				gameId = queryCursor.getInt(queryCursor.getColumnIndex("_id"));
//			Log.i(TAG, "gameId: " + gameId);
			if (fm_extern_db_key_id == 0)
				gameId = fm_extern_db_game_id;
			setQueryDataToTitle(fm_extern_db_key_id, gameId, pgnDb.getRowCount(PgnDb.TABLE_NAME), scroll_game_id, queryCount);
			if (setInfo)
				setInfoValues("DB-Test - displayGames()", "cursor_cnt: " + queryCursor.getCount(), pgnDb.getDbVersion());
			pgnDb.closeDb();
			String[] columns = new String[]	{PgnDb.PGN_WHITE, PgnDb.PGN_DATE, PgnDb.PGN_BLACK, PgnDb.PGN_RESULT};
			int[] to = new int[] {R.id.text1, R.id.text2, R.id.text3, R.id.text4};
			pgnAdapter = new SimpleCursorAdapter(PgnFileManager.this, R.layout.dbquery, queryCursor, columns, to, 0);
			lvGames.setVisibility(ListView.VISIBLE);
			lvGames.setAdapter(pgnAdapter);
			if (fm_extern_db_key_id == 0)
				lvGames.setSelection(pgnDb.scrollGameId -1);
			else
				lvGames.setSelection(scroll_game_id -1);
			pgnAdapter.notifyDataSetChanged();
			if (queryCursor.getCount() == 0)
			{
//				Log.i(TAG, "queryCursor: " + queryCursor);
				isQueryResult = false;
				emptyLv.setVisibility(TextView.VISIBLE);
			}
			else
				emptyLv.setVisibility(TextView.INVISIBLE);
			if (fm_extern_db_key_id != 0 & queryCursor.getCount() > 0)
			   {
				   fm_extern_db_cursor_id = 1;
				   if (queryCursor.moveToPosition(1))
					   setQueryPreferences(queryCursor.getInt(queryCursor.getColumnIndex("_id")));
				   fm_extern_db_cursor_count = queryCursor.getCount();		
				   fm_extern_db_game_id_list = getGameIdStringList(queryCursor);
				   gameIdList = getGameIdList(fm_extern_db_game_id_list);
			   }
			lvGames.setOnItemClickListener(new OnItemClickListener() 
			{
				@Override
				public void onItemClick(AdapterView<?> listView, View view, int position, long id) 
				{
					if (isQuery) return;
					if (queryCursor.moveToPosition(position))
					{
					   int gameId = queryCursor.getInt(queryCursor.getColumnIndex("_id"));
	//				   Log.i(TAG, "curserId, gameId: " + (position +1) + ", " + gameId);
					   if (pgnDb.openDb(gamePath, gameFile, SQLiteDatabase.OPEN_READONLY))
					   {
						   int queryCount = 1;
						   if (fm_extern_db_key_id != 0)
							   queryCount = queryCursor.getCount();
//						   Log.i(TAG, "fm_extern_db_key_id, gameId: " + fm_extern_db_key_id + ", " + gameId);
						   scroll_game_id = position +1;
						   setQueryDataToTitle(fm_extern_db_key_id, gameId, pgnDb.getRowCount(PgnDb.TABLE_NAME), scroll_game_id, queryCount);
						   pgnDb.getGameId(gameId, 10);	// set pgnStat value
						   pgnIO.pgnStat = pgnDb.pgnStat;
						   fileData = pgnDb.getDataFromGameId(gameId);
						   pgnDb.closeDb();
						   if (!fileData.equals(""))
						   {
							   	
							   if (fm_extern_db_key_id != 0)
							   {
								   fm_extern_db_cursor_id = position;
								   fm_extern_db_cursor_count = queryCursor.getCount();		
								   fm_extern_db_game_id_list = getGameIdStringList(queryCursor);
							   }
							   else
							   {
								   fm_extern_db_cursor_id = 0;
								   fm_extern_db_cursor_count = 0;		
								   fm_extern_db_game_id_list = "";
							   }
							   setQueryPreferences(gameId);
							   finishAfterLoad(gamePath, gameFile);
						   }
					   }
					}
				}
			});
			if (isQuery)
	        {
	        	switch (setQueryData) 											
				{
					case SET_QUERY_DATA_GAME: dataQueryGameID(); break;
					case SET_QUERY_DATA_PLAYER: dataQueryPlayer(); break;
					case SET_QUERY_DATA_EVENT: dataQueryEvent(); break;
					case SET_QUERY_DATA_ECO: dataQueryEco(); break;
				}
	        }
			
		}
		String gamePath = "";
		String gameFile = "";
		boolean setInfo;
		boolean isNewFile;
		
		String pl = "";
		int pl_color = 2;
		String pl_date_from = "";
		String pl_date_to = "";
		boolean pl_date_desc = false;
		String pl_event = "";
		String pl_site = "";
		
		String ev_event = "";
		String ev_site = "";
		
		String ec_eco = "";
		String ec_opening = "";
		String ec_variation = "";
		String ec_date_from = "";
		String ec_date_to = "";
		boolean ec_date_desc = false;
	}
	public void loadGameFromDb(String path, String file, int gameId, int gameLoadControl) 
	{
//		Log.i(TAG, "path, file, fm_extern_db_key_id: " + path + ", " + file + ", " + fm_extern_db_key_id);
//		Log.i(TAG, "gameId, Control, cursor_count, cursor_id, scroll: " 
//				+ gameId + ", " + gameLoadControl + ", " + fm_extern_db_cursor_count + ", " + fm_extern_db_cursor_id + ", " + scroll_game_id);
		returnIntent = new Intent();
		int newGameId = 0;
		boolean pgnDbNotExists = false;
		if (!pgnDb.initPgnFiles(baseDir + path, file))
		{
			notificationId++;
			createDatabaseTask = new CreateDatabaseTask(this, notificationId);
			createDatabaseTask.execute(baseDir + path, file, "0");
			pgnDbNotExists = true;
			fileData = "";
		}
		else
		{
			if (pgnDb.openDb(baseDir + path, file, SQLiteDatabase.OPEN_READONLY))
			{
				int curserIdx = 0;
				if (fm_extern_db_key_id != 0)
				{
					if (scroll_game_id <= fm_extern_db_cursor_count & (gameLoadControl == 0 | gameLoadControl == 8))
						fm_extern_db_cursor_id = scroll_game_id -1;
					switch (gameLoadControl)
			        {
			        	case 0: curserIdx = fm_extern_db_cursor_id +1;		break;	// next
			        	case 1: curserIdx = 0;								break;	// first
				        case 7: try 												// random   	
								{
						        	Random r;
						        	r = new Random();
						        	curserIdx = (r.nextInt(fm_extern_db_cursor_count));
								}
					        	catch (IllegalArgumentException e) {pgnDb.closeDb(); return;}
					        	break; 													
				        case 8: curserIdx = fm_extern_db_cursor_id -1; 		break; 	// previous
				        case 9: curserIdx = fm_extern_db_cursor_count -1; 	break;	// last
				        case 10: curserIdx = fm_extern_db_cursor_id; 		break;	// current
			        }
					if (curserIdx < 0) 		curserIdx = 0;
					if (curserIdx >= fm_extern_db_cursor_count) 	curserIdx = fm_extern_db_cursor_count -1;
					try 
					{
						if (gameIdList != null)
							gameId = Integer.parseInt(gameIdList[curserIdx]);
						else
							gameId = 0;
					}
					catch (NumberFormatException e) {gameId = 0;}
//					Log.i(TAG, "gameLoadControl, fm_extern_db_cursor_id, curserIdx, gameId: " + gameLoadControl + ", " + fm_extern_db_cursor_id + ", " + curserIdx + ", " + gameId);
					if (gameId > 0)
					{
						fm_extern_db_cursor_id = curserIdx;
						gameLoadControl = 10;
					}
				}
				newGameId = pgnDb.getGameId(gameId, gameLoadControl);
				fileData = pgnDb.getDataFromGameId(newGameId);
				pgnDb.closeDb();
				if (fm_extern_db_key_id != 0)
				{
					if (curserIdx == 0) 							pgnDb.pgnStat = "F";
					if (curserIdx == fm_extern_db_cursor_count -1) 	pgnDb.pgnStat = "L";
				}
				if (!fileData.equals(""))
				{
					if (!fileData.startsWith("[Event "))
					{
						pgnDb.deleteDbFile();
						notificationId++;
						createDatabaseTask = new CreateDatabaseTask(this, notificationId);
						createDatabaseTask.execute(baseDir + path, file, "0");
						pgnDbNotExists = true;
						fileData = "";
					}
				}
			}
			else
				fileData = "";
		}
//		Log.i(TAG, "fileData: \n" + fileData);
		returnIntent.putExtra("pgnStat", pgnDb.pgnStat);
		if (!fileData.equals(""))
		{
			setTitle(getString(R.string.fmProgressDialog));
			returnIntent.putExtra("fileData", fileData);
			returnIntent.putExtra("fileBase", baseDir);
			returnIntent.putExtra("filePath", path);
			returnIntent.putExtra("fileName", file);
			setResult(RESULT_OK, returnIntent);
			setQueryPreferences(newGameId);
			if (getIntent().getExtras().getInt("displayActivity") == 1)
				setPreferences(fileData);
			else
				setSkipPreferences(1, fileData);
			finish();
		}
		else
		{
//			Log.i(TAG, "FILE_NOT_EXISTS_DIALOG");
			if (pgnDbNotExists)
			{
				setResult(99, returnIntent);
				removeDialog(DATABASE_LOCKED_DIALOG);
				showDialog(DATABASE_LOCKED_DIALOG);
			}
			else
			{
				setResult(99, returnIntent);
				removeDialog(FILE_NOT_EXISTS_DIALOG);
				showDialog(FILE_NOT_EXISTS_DIALOG);
			}
		}
	}
	public void deleteDbFile(String fileName)
    {
		fileName = baseDir + etPath.getText().toString() + fileName;
		fileName = fileName.replace(".pgn", ".pgn-db");
		File fPgnDb = new File(fileName);
		if (fileName.endsWith(".pgn-db"))
			fPgnDb.delete();
    }
	public int getDbFileState(String fileName, int location)
    {
		if (!fileName.endsWith(fm_file_extension) | location != 1)
			return STATE_DB_NO_PGN_ACTION;
		if (!(userPrefs.getBoolean("user_options_gui_usePgnDatabase", true)))
			return STATE_DB_DISABLED;
		String path = baseDir + etPath.getText().toString() + fileName;
		String dbPath = path.replace(".pgn", ".pgn-db");
		String dbJournalPath = path.replace(".pgn", ".pgn-dbjournal");
		File fPgn = new File(path); 
		File fPgnDb = new File(dbPath);
		File fPgnDbJournal = new File(dbJournalPath);
		long lmPgnDb = fPgnDb.lastModified();
		long ct = System.currentTimeMillis();
		if (!fPgn.exists())
			return STATE_DB_NO_PGN_ACTION;
		else
		{
			if (!fPgnDb.exists())
				return STATE_DB_NO_DBFILE;
			else
			{

				if ((ct -lmPgnDb) <= 3000 | fPgnDbJournal.exists())
					return STATE_DB_LOCKED;
				else
				{
					pgnDb.initPgnFiles(baseDir + etPath.getText().toString(), fileName);
					if 	(pgnDb.openDb(baseDir + etPath.getText().toString(), fileName, SQLiteDatabase.OPEN_READONLY))
					{
						int state = pgnDb.getStateFromLastGame();
//						Log.i(TAG, "state: "  + state);
						pgnDb.closeDb();
						switch (state) 
						{
							case 1:		return STATE_DB_OK;
							case 8:		return STATE_DB_WRONG_VERSION;
							default:	return STATE_DB_UNCOMPLETED;
						}
					}
					else
						return STATE_DB_LOCKED;
				}
			}
		}
    }
	
	private void handleDb(String path, String file, int state, boolean isNewFile) 
	{
//		Log.i(TAG, "file, state: " + path + file + ", " + state);
		switch (state) 
		{
			case STATE_DB_OK: 
				if (pgnDb.initPgnFiles(path, file))
				{
					if (showGameListView)
						displayGames(path, file, true, isNewFile);
					else
						showGameListView = true;
				}
				break;
			case STATE_DB_LOCKED: 
				removeDialog(DATABASE_LOCKED_DIALOG);
				showDialog(DATABASE_LOCKED_DIALOG);
				break;
			case STATE_DB_NO_DBFILE:
				if (!pgnDb.initPgnFiles(path, file))
				{
					Toast.makeText(this, getString(R.string.fmCreatingPgnDatabaseToast), Toast.LENGTH_SHORT).show();
					notificationId++;
					createDatabaseTask = new CreateDatabaseTask(this, notificationId);
					createDatabaseTask.execute(path, file, "0");
				}
				break;
			case STATE_DB_WRONG_VERSION: 
				Toast.makeText(this, getString(R.string.fmDropCreateIndex), Toast.LENGTH_LONG).show();
				notificationId++;
				createDatabaseTask = new CreateDatabaseTask(this, notificationId);
				createDatabaseTask.execute(path, file, "0", "idx");
				break;
			case STATE_DB_UNCOMPLETED:
//				Log.i(TAG, "STATE_DB_UNCOMPLETED");
				pgnDb.initPgnFiles(path, file);
				if (pgnDb.openDb(path, file, SQLiteDatabase.OPEN_READONLY))
				{
					switch (pgnDb.getStateFromLastGame()) 
					{
						case 1:	// no data after last game, unlock .pgn-db(set lastModified from .pgn)
//							Log.i(TAG, "STATE: 1");
							pgnDb.closeDb();
							handleDb(path, file, STATE_DB_OK, false);
							break;
						case 2:	// games after last game(db), complite db
//							Log.i(TAG, "STATE: 2");
							pgnDb.closeDb();
							Toast.makeText(this, getString(R.string.fmCreatingPgnDatabaseToast), Toast.LENGTH_SHORT).show();
							notificationId++;
							createDatabaseTask = new CreateDatabaseTask(this, notificationId);
							createDatabaseTask.execute(path, file, Long.toString(pgnDb.pgnRafOffset), "dropidx");
							break;
						case 0:	// delete .pgn-db and start new create db(STATE_DB_NO_DBFILE)
//							Log.i(TAG, "STATE: 0");
							pgnDb.closeDb();
							pgnDb.deleteDbFile();
							handleDb(path, file, STATE_DB_NO_DBFILE, false);
							break;
					}
				}
				else
				{
//					Log.i(TAG, "STATE: !pgnDb.openDb()");
					pgnDb.deleteDbFile();
					handleDb(path, file, STATE_DB_NO_DBFILE, false);
				}
				break;
			default: 
				break;
		}
	}
	
	private void displayGames(String path, String file, boolean setInfo, boolean isNewFile) 
	{
		queryTask = new QueryTask();
		queryTask.execute(path, file, Boolean.toString(setInfo), Boolean.toString(isNewFile));
		return;
	}
	
	public void setInfoValues(String title, String message, String dbVersion)
    {
		if (runP.getBoolean("run_isActivate", false))
		{
			SharedPreferences.Editor ed = runP.edit();
			ed.putString("infoTitle", title);
			ed.putString("infoMessage", message);
			ed.putString("infoModelNumber", Build.MANUFACTURER + " - " + Build.MODEL);
			ed.putString("infoAndroidVersion", Build.VERSION.RELEASE);
			ed.putString("infoDbVersion", dbVersion);
			ed.commit();
		}
    }
	
	final String TAG = "PgnFileManager";
	// Dialogs
		private static final int PATH_NOT_EXISTS_DIALOG = 1;
		private static final int FILE_NOT_EXISTS_DIALOG = 2;
		private static final int FILE_EXISTS_DIALOG = 3;
		private static final int FILE_NOT_ENDS_WITH_PGN_DIALOG = 4;
		private static final int PGN_ERROR_DIALOG = 5;
		private static final int DELETE_DIALOG = 6;
		private static final int WEB_FILE_NOT_EXISTS_DIALOG = 7;
		private static final int ADD_FOLDER_DIALOG = 8;
		private static final int DATABASE_LOCKED_DIALOG = 21;
		private static final int QUERY_PROGRESS_DIALOG = 22;
		private static final int MENU_GAME_DIALOG = 23;
		private static final int COMING_SOON = 91;
	// database file state
		private static final int STATE_DB_NO_PGN_ACTION = 200;
		private static final int STATE_DB_OK = 201;
		private static final int STATE_DB_NO_DBFILE = 202;
		private static final int STATE_DB_LOCKED = 203;
		private static final int STATE_DB_UNCOMPLETED = 204;
		private static final int STATE_DB_WRONG_VERSION = 208;
		private static final int STATE_DB_DISABLED = 209;
		int fileStat = STATE_DB_NO_PGN_ACTION;

		Intent returnIntent;
		PgnIO pgnIO;
		PgnDb pgnDb;
		int db_state = STATE_DB_NO_PGN_ACTION;
		CreateDatabaseTask createDatabaseTask = null;
		QueryTask queryTask = null;
		int notificationId = 0;
		SimpleCursorAdapter pgnAdapter = null;
		int fileActionCode;
		int lastFileActionCode = 1;
//		SharedPreferences		SharedPreferences		SharedPreferences		SharedPreferences	
		SharedPreferences fmPrefs;
		SharedPreferences userPrefs;
		SharedPreferences runP;
		int fm_location = 2;	// 1 = external(sdCard)	2 = intern(resource/assets)	3 = WWW(Internet)
		String fm_file_extension = ".pgn";
		String fm_extern_load_path = "";
		String fm_extern_load_file = "";
		String fm_extern_save_path = "";
		String fm_extern_save_file = "";
		String fm_extern_save_auto_path = "";
		String fm_extern_save_auto_file = "";
		long fm_extern_skip_bytes = 0;
		long fm_extern_game_offset = 0;
		String fm_extern_last_game = "";
		String fm_intern_path = "";	
		String fm_intern_file = "";	
		long fm_intern_skip_bytes = 0;
		String fm_intern_last_game = "";
		String fm_url = "";
		
		boolean isQuery = false;
		boolean isQueryResult = true;
		private static final int SET_QUERY_DATA_GAME 	= 0;
		private static final int SET_QUERY_DATA_PLAYER 	= 1;
		private static final int SET_QUERY_DATA_DATE 	= 2;
		private static final int SET_QUERY_DATA_EVENT	= 3;
		private static final int SET_QUERY_DATA_ECO 	= 9;
		
		int setQueryData = 0;
		
		boolean fm_extern_db_app_paused = false;// application paused?, call onPause()
		
		int fm_extern_db_key_id = 0;			// 0 game, 1 player, 2 date, 3 event, 9 eco
		// game-ID
		int fm_extern_db_game_id = 0;			// game-ID(primary key) [current game]
		int fm_extern_db_game_count = 0;		// game count
		boolean fm_extern_db_game_desc = false;	// game-ID: descending
		int fm_extern_db_game_max_items = 4000;	// max. items for viewAdapter
		int fm_extern_db_cursor_id = 0;			// 0 . . .  fm_extern_db_cursor_count -1
		int fm_extern_db_cursor_count = 0;		// cursor row counter
		String fm_extern_db_game_id_list = "";
		int scroll_game_id = 1;
		
		String 	fm_extern_db_key_player 			= "";
		int 	fm_extern_db_key_player_color 		= 2;
		String 	fm_extern_db_key_player_date_from	= "";
		String 	fm_extern_db_key_player_date_to		= "";
		boolean fm_extern_db_key_player_date_desc	= false;
		String 	fm_extern_db_key_player_event		= "";
		String 	fm_extern_db_key_player_site		= "";
		
		String 	fm_extern_db_key_event				= "";
		String 	fm_extern_db_key_event_site			= "";
		
		String 	fm_extern_db_key_eco				= "";
		String 	fm_extern_db_key_eco_opening		= "";
		String 	fm_extern_db_key_eco_variation		= "";
		String 	fm_extern_db_key_eco_date_from		= "";
		String 	fm_extern_db_key_eco_date_to		= "";
		boolean fm_extern_db_key_eco_date_desc 		= false;
		
		boolean isGameValues = false;
		Cursor gameCursor;
		Cursor queryCursor;
		String[] gameIdList;
		boolean showGameListView = true;
		
//		GUI		GUI		GUI		GUI		GUI		GUI		GUI		GUI
		RelativeLayout relLayout;
		RelativeLayout queryGameIdLayout;
		RelativeLayout queryPlayerLayout;
		RelativeLayout queryEventLayout;
		RelativeLayout queryEcoLayout;
		TextView lblPath;
		TextView lblFile;
		EditText etBase;
		EditText etPath;
		EditText etUrl;
		EditText etFile;
		ListView lvFiles;
		ListView lvGames;
		TextView emptyLv;
		ImageView btnMenu = null;
		ImageView fmBtnAction = null;
		ImageView fmBtnGames = null;
		ImageView btnOptions = null;
		ImageView btnAddFolder = null;
		// query player
		TextView queryInfo1;
		TextView queryInfo2;
		ImageView qActionBtn = null;
		
		EditText qGameCount;
		CheckBox qGameDescCb;
		EditText qCurrentId;
		EditText qMaxItems;
		
		EditText qPlayer;
		RadioGroup qColor;
		RadioButton qWhiteRb;
		RadioButton qBlackRb;
		RadioButton qWhiteBlackRb;
		CheckBox qDateDescCb;
		EditText qDateFrom;
		EditText qDateTo;
		EditText qEvent;
		EditText qSite;
		
		EditText qeEvent;
		EditText qeSite;
		
		EditText qoEco;
		EditText qoOpening;
		EditText qoVariation;
		CheckBox qoDateDescCb;
		EditText qoDateFrom;
		EditText qoDateTo;
		
		public ArrayAdapter<String> files;
		C4aImageDialog c4aImageDialog;
		C4aDialog pathDialog;
		C4aDialog fileNotExistsDialog;
		C4aDialog fileExistsDialog;
		C4aDialog fileNotEndsWithPgnDialog;
		C4aDialog webFileNotExistsDialog;
		C4aDialog addFolderDialog;
		C4aDialog pgnDialog;
		C4aDialog deleteDialog;
		ProgressDialog progressDialog = null;

		int activDialog = 0;
		String pgnFileName = "";
		String pgnUrl = "http://www.chessok.com/broadcast/getpgn.php?action=save&saveid=sofia2010_12.pgn";
		String fileData = "";
		String liteData = "";
		String fileName = "";
		String baseDir = "";
		String defaultFolder = "";
		final String baseAssets = "assets/";
		final String assetsDir[] = {"games", "puzzles"};
		boolean isSkipRandom = false;
		boolean isPriviousGame = false;
		boolean isLastGame = false;
		boolean isFindGame = false;
		boolean isQueryInputError = false;
		
//		DB-TEST			DB-TEST			DB-TEST			DB-TEST
		String testTitle = "";
		String testMessage = "";
}
