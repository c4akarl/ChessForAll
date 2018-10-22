package ccc.chess.gui.chessforall;

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
import android.database.sqlite.SQLiteCantOpenDatabaseException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabaseCorruptException;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import android.graphics.drawable.GradientDrawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager.BadTokenException;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

public class FileManager extends Activity implements Ic4aDialogCallback, DialogInterface.OnCancelListener, TextWatcher
{
	public void onCreate(Bundle savedInstanceState)
	{
//		Log.i(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);

//		dbTest = DB_CREATE;
		dbTest = DB_CREATE_TRANSACTION;
//		dbTest = DB_CREATE_SQL_STM;
//		dbTest = DB_CREATE_SQL_STM_TRANSACTION;

		u = new Util();
        setQueryData = 99;	// query none
        runP = getSharedPreferences("run", 0);
        userPrefs = getSharedPreferences("user", 0);
        fmPrefs = getSharedPreferences("fm", 0);
		ce = new ChessEngine(this, 9);

	}

	@Override
    protected void onResume()
    {
		super.onResume();
        startPfm();
    }

    @Override
    protected void onDestroy() 					
    {
     	super.onDestroy();
    }

    public void startPfm()
	{
		fileActionCode = getIntent().getExtras().getInt("fileActionCode");
        fileIO = new FileIO(this);
        pgnDb = new PgnDb();
		baseDir = fileIO.getExternalDirectory(0);
		getPreferences();
        if (fm_extern_load_path.equals("") & fileIO.pathExists(baseDir + "c4a/"))
        	fm_extern_load_path = baseDir + "c4a/";
        if (fm_extern_save_path.equals("") & !fm_extern_load_path.equals(""))
			fm_extern_save_path = fm_extern_load_path;
        if (fm_extern_save_path.equals("") & fileIO.pathExists(baseDir + "c4a/"))
			fm_extern_save_path = baseDir + "c4a/";
        fm_file_extension = PGN_EXTENSION;
        if (fileActionCode == 9)
        {
        	isPriviousGame = true;
        	fileActionCode = 1;
        }

        if (getIntent().getExtras().getInt("displayActivity") == 1)
        {
			u.updateFullscreenStatus(this, userPrefs.getBoolean("user_options_gui_StatusBar", false));
	        setContentView(R.layout.filemanager);
	        relLayout = (RelativeLayout) findViewById(R.id.fmLayout);
	        queryGameIdLayout = (RelativeLayout) findViewById(R.id.queryGameId);
	        queryGameIdLayout.setVisibility(RelativeLayout.INVISIBLE);
	        queryPlayerLayout = (RelativeLayout) findViewById(R.id.queryPlayer);
	        queryPlayerLayout.setVisibility(RelativeLayout.INVISIBLE);
	        queryEventLayout = (RelativeLayout) findViewById(R.id.queryEvent);
	        queryEventLayout.setVisibility(RelativeLayout.INVISIBLE);
	        queryEcoLayout = (RelativeLayout) findViewById(R.id.queryEco);
	        queryEcoLayout.setVisibility(RelativeLayout.INVISIBLE);
	        title = (TextView) findViewById(R.id.title);
	        lblFile = (TextView) findViewById(R.id.fmLblFile);
	        etPath = (EditText) findViewById(R.id.fmEtPath);
			etPath.setText("");
            setTextViewColors(etPath, cv.COLOR_DATA_BACKGROUND_16, cv.COLOR_DATA_TEXT_17);
			etPath.addTextChangedListener(new TextWatcher()
			{
				@Override
				public void onTextChanged(CharSequence s, int start, int before, int count) { }
				@Override
				public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

				@Override
				public void afterTextChanged(Editable s)
				{
					if (!etPath.getText().equals("") & (fileActionCode == 1 | fileActionCode == 2))
					{
						// fmPrefs ---> Main, download pgn !!!
                        SharedPreferences.Editor ed = fmPrefs.edit();
                        ed.putString("fm_last_selected_folder", etPath.getText().toString());
                        ed.commit();
					}
				}
			});

	        etUrl  = (EditText) findViewById(R.id.fmEtUrl);
	        etFile = (EditText) findViewById(R.id.fmEtFile);
			if (fileActionCode == 2)
			{
				etFile.setFocusable(true);
				etFile.addTextChangedListener(this);

			}
			else
				etFile.setFocusable(false);
	        etFile.setText("");

	        etUrl.setText("");
	        btnMenu = (ImageView) findViewById(R.id.btnMenu);
	        btnDirBack = (ImageView) findViewById(R.id.btnDirBack);
	        fmBtnAction = (ImageView) findViewById(R.id.fmBtnAction);
	        fmBtnGames = (ImageView) findViewById(R.id.fmBtnGames);
	        fmBtnGames.setVisibility(ImageView.INVISIBLE);
	        lvFiles = (ListView) findViewById(R.id.fmLvFiles);
	        lvGames = (ListView) findViewById(R.id.fmGameView);
	        lvGames.setVisibility(ListView.INVISIBLE);
	        emptyLv = (TextView)findViewById(R.id.emptyLv);
	        
			fmInfo = (TextView)findViewById(R.id.fmInfo);
			fmInfo.setMovementMethod(new ScrollingMovementMethod());

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
				case 2:															// Save
					if (fm_extern_save_path.equals(""))
			        {
						etPath.setText(baseDir);
						etFile.setText("");
			        	defaultFolder = "c4a";
			        	removeDialog(ADD_FOLDER_DIALOG);
						showDialog(ADD_FOLDER_DIALOG);
			        }
					else
						startSave();
					break;
				case 5: 														// engine autoPlay(get path/file)
					startSave();
					break;
				case 81: 														// load extern engine
					fm_location = 1;
					fm_file_extension = ALL_EXTENSION;
					startLoad(fm_location, false);
					break;
				case 82: 														// load intern engine from //data
					fm_location = 2;
					startLoad(fm_location, false);
					break;
				case 91: 														// load opening book (.bin)
					fm_location = 1;
					fm_file_extension = BIN_EXTENSION;
					startLoad(fm_location, false);
					break;
			}
        }
        else			// no screen(batch)
        {
//Log.i(TAG, "no screen(batch), fm_location: " + fm_location);
			int gameLoad = getIntent().getExtras().getInt("gameLoad");
			if (getIntent().getExtras().getString("queryControl").equals("i"))
			{
				fm_extern_db_key_id = 0;
				fm_extern_db_game_id = gameLoad;
				gameLoad = 10;
			}
			else
			{
				if (getIntent().getExtras().getInt("gameLoad") == 9)
					isLastGame = true;
				if (getIntent().getExtras().getInt("gameLoad") == 7)
					isSkipRandom = true;
				if (getIntent().getExtras().getInt("gameLoad") == 5)
					isFindGame = true;
			}
        	switch (fileActionCode) 											// Load | Save | Delete
			{
				case 1: 														// Load
					fm_location = fmPrefs.getInt("fm_load_location", 1);
					if (fm_location == 1 & userPrefs.getBoolean("user_options_gui_usePgnDatabase", true))
						loadGameFromDb(fm_extern_load_path, fm_extern_load_file, fm_extern_db_game_id, gameLoad);
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
					fileIO = new FileIO(this);
					fileActionCode = 1;
					if (fm_location == 1 & userPrefs.getBoolean("user_options_gui_usePgnDatabase", true))
						loadGameFromDb(fm_extern_load_path, fm_extern_load_file, fm_extern_db_game_id, getIntent().getExtras().getInt("gameLoad"));
					else
						startLoadNoScreen();
					break;
			}
        }
	}

//	Dialog, Listener, Handler		Dialog, Listener, Handler		Dialog, Listener, Handler
    public void myClickHandler(View view) 		
    {
    	SharedPreferences.Editor ed = fmPrefs.edit();
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
						if 	(fm_location == 1 &	userPrefs.getBoolean("user_options_gui_usePgnDatabase", true))
						{
							db_state = getDbFileState(etFile.getText().toString(), fm_location);
							if 	(db_state == STATE_DB_OK)
							{
								if 	(pgnDb.openDb(etPath.getText().toString(), etFile.getText().toString(), SQLiteDatabase.OPEN_READONLY))
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
									fileIO.pgnStat = pgnDb.pgnStat;
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
										finishAfterLoad(etPath.getText().toString(), etFile.getText().toString());
									}
								}
							}
							else
								handleDb(etPath.getText().toString(), etFile.getText().toString(), db_state, false);
						}
						else
							loadFile();
					}
					break;
				case 2: 														// Save
					if (!etFile.getText().toString().endsWith(PGN_EXTENSION))
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
							if (fileIO.fileExists(etPath.getText().toString(), etFile.getText().toString()))
								saveFile(true);
							else
								saveFile(false);
						}
					}
					break;
				case 5: 														// engine autoPlay
					getPathFile();											
					break;
				case 81: 														// load engine from SD card
//Log.i(TAG, "myClickHandler(), fileActionCode: 81, engine path/file: " + etPath.getText().toString() + "/" + etFile.getText().toString());
					String path = etPath.getText().toString();
					String file = etFile.getText().toString();
					if (!path.equals("") & !file.equals(""))
					{
						if (ce.initProcessFromFile(path, file))
						{
							ed.putString("fm_extern_engine_path", etPath.getText().toString());
							ed.putString("fm_extern_engine_file", etFile.getText().toString());
							ed.commit();
						}
						removeDialog(ENGINE_INSTALL_DIALOG);
						showDialog(ENGINE_INSTALL_DIALOG);
					}
					break;
				case 82: 														// load engine from //data
//Log.i(TAG, "myClickHandler(), fileActionCode: 82, engine path/file: " + etPath.getText().toString() + "/" + etFile.getText().toString());
					ed.putString("fm_intern_engine_path", etPath.getText().toString());
					ed.putString("fm_intern_engine_file", etFile.getText().toString());
					ed.commit();
					returnIntent = new Intent();
					returnIntent.putExtra("filePath", etPath.getText().toString());
					returnIntent.putExtra("fileName", etFile.getText().toString());
					setResult(RESULT_OK, returnIntent);
					finish();
					break;
				case 91:														// load opening book
					if (!etFile.getText().toString().endsWith(BIN_EXTENSION))
						etFile.setText("");
					ed.putString("fm_extern_book_path", etPath.getText().toString());
					ed.putString("fm_extern_book_file", etFile.getText().toString());
					ed.commit();
					returnIntent = new Intent();
					returnIntent.putExtra("filePath", etPath.getText().toString());
					returnIntent.putExtra("fileName", etFile.getText().toString());
					setResult(RESULT_OK, returnIntent);
					finish();
					break;
			}
			break;
		case R.id.btnDirBack:
			if (isQuery) return;
			if (fileActionCode != 91)
			{
				fmBtnGames.setVisibility(ImageView.INVISIBLE);
				fmBtnAction.setVisibility(ImageView.INVISIBLE);
			}
			else
				setFmInfo(getString(R.string.fmInfoLoadBook));
			emptyLv.setVisibility(TextView.INVISIBLE);
			if (lvGames.isShown())
			{
				setFmInfo(getString(R.string.fmInfoSelectFile));
				lvGames.setVisibility(ListView.INVISIBLE);
				if (fileActionCode == 1)
					title.setText(getString(R.string.fmTitleLoad));
				else
					title.setText(getString(R.string.fmTitleSave));
			}
			else
			{
				if (fm_location == 1)
				{
					String newPath = getNewPath(etPath.getText().toString());
					etPath.setText(newPath);
					etPath.setSelection(etPath.getText().length());
					if (fileActionCode == 1 | fileActionCode == 91)
						etFile.setText("");
					showFileList(newPath);
				}
			}
			break;
		case R.id.btnMenu:
			if (lvGames.isShown())
				showDialog(MENU_GAME_DIALOG);
			else
				showDialog(MENU_FILE_DIALOG);
			break;
		case R.id.fmBtnGames:
			if (!isQuery)
				showDialog(MENU_QUERY_DIALOG);
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
						if (pgnDb.initPgnFiles(etPath.getText().toString(), etFile.getText().toString()))
			        	{
							setPlayerData();
			        		fm_extern_db_key_id = SET_QUERY_DATA_PLAYER;
			        		scroll_game_id = 1;
							startQueryTask(etPath.getText().toString(), etFile.getText().toString(), false, false);
			        	}
						break;
					case SET_QUERY_DATA_DATE:
						fm_extern_db_key_id = SET_QUERY_DATA_DATE;
		        		scroll_game_id = 1;
						break;
					case SET_QUERY_DATA_EVENT:
						if (pgnDb.initPgnFiles(etPath.getText().toString(), etFile.getText().toString()))
			        	{
							setEventData();
							fm_extern_db_key_id = SET_QUERY_DATA_EVENT;
			        		scroll_game_id = 1;
			        		startQueryTask(etPath.getText().toString(), etFile.getText().toString(), false, false);
			        	}
						break;
					case SET_QUERY_DATA_ECO:
						if (pgnDb.initPgnFiles(etPath.getText().toString(), etFile.getText().toString()))
			        	{
							setEcoData();
							fm_extern_db_key_id = SET_QUERY_DATA_ECO;
			        		scroll_game_id = 1;
			        		startQueryTask(etPath.getText().toString(), etFile.getText().toString(), false, false);
			        	}
						break;
					case SET_QUERY_DATA_GAME:
					default:
						if (pgnDb.initPgnFiles(etPath.getText().toString(), etFile.getText().toString()))
			        	{
			        		fm_extern_db_key_id = SET_QUERY_DATA_GAME;
			        		fm_extern_db_game_id = Integer.parseInt(qCurrentId.getText().toString());
			        		pgnDb.pgnGameId = fm_extern_db_game_id;
			        		scroll_game_id = fm_extern_db_game_id;
			        		fm_extern_db_game_desc = qGameDescCb.isChecked();
			        		fm_extern_db_game_max_items = Integer.parseInt(qMaxItems.getText().toString());
							startQueryTask(etPath.getText().toString(), etFile.getText().toString(), false, false);
			        	}
						break;
				}
			}
			break;
		}
	}

	public void getCallbackValue(int btnValue)
    { 
		if (activDialog == DELETE_FILE_DIALOG & btnValue == 2)
		{
			if (!fileIO.canWrite(etPath.getText().toString(), fileName))
			{
				removeDialog(FILE_NO_WRITE_PERMISSIONS);
				showDialog(FILE_NO_WRITE_PERMISSIONS);
				return;
			}
			fileIO.fileDelete(etPath.getText().toString(), fileName);
			etFile.setText("");
			if (fileActionCode == 82) // load engines from app data after delete a file
				showFileListFromData("");
			else
				showFileList(etPath.getText().toString());
			fmBtnAction.setVisibility(ImageView.INVISIBLE);
		}

		if (activDialog == FILE_EXISTS_DIALOG & btnValue == 1)
		{
			saveFile(false);
		}

		if (activDialog == ADD_FOLDER_DIALOG & btnValue == 2)
		{
			String newFolder = etPath.getText().toString() + addFolderDialog.getNumber() + "/";
			if (fileIO.createDir(newFolder))
			{
				etFile.setText(fm_file_extension);
				fm_extern_save_path = etPath.getText().toString() + addFolderDialog.getNumber() + "/";
				etPath.setText(etPath.getText().toString() + addFolderDialog.getNumber() + "/");
				etPath.setSelection(etPath.getText().length());
				showFileList(newFolder);
				if (!defaultFolder.equals(""))
					startSave();
			}
		}

		if (activDialog == ADD_FILE_DIALOG & btnValue == 2)
		{
			String folder = etPath.getText().toString();
			String newFile = addFileDialog.getNumber();

			if (!fileIO.canWrite(folder, ""))
			{
				removeDialog(FILE_NO_WRITE_PERMISSIONS);
				showDialog(FILE_NO_WRITE_PERMISSIONS);
				return;
			}
			if (fileIO.pathExists(folder))
			{
				if (!newFile.endsWith(PGN_EXTENSION))
					showDialog(FILE_NOT_ENDS_WITH_PGN_DIALOG);
				else
				{
					if (!fileIO.fileExists(folder, newFile))
					{
						fileIO.dataToFile(folder, newFile, "", false);
						etFile.setText(newFile);
						showFileList(folder);
						int position = -1;
						for (int i = 0; i < files.getCount(); i++)
						{
							if (files.getItem(i).equals(newFile))
							{
								position = i;
								break;
							}
						}
						if (position >= 0 & fileActionCode == 2)
							showPositionInFileList(position, newFile);
					}
					else
						showDialog(FILE_EXISTS_DIALOG);
				}
			}
			else
				showDialog(PATH_NOT_EXISTS_DIALOG);
		}

		if (getIntent().getExtras().getInt("displayActivity") == 0)
			finish();
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
        	mes = getString(R.string.fmPathError) + "\n<" + etPath.getText().toString()  + ">";
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
		if (id == FILE_NO_WRITE_PERMISSIONS)
		{
			String mesCreateDb = "";
			if (isCreateDb)
			{
				isCreateDb = false;
				mesCreateDb = getString(R.string.fmCreatingPgnDatabase) + "\n";
			}
			String path = "";
			String file = "";
			if (etPath != null)
				path = etPath.getText().toString();
			if (etFile != null)
				path = etFile.getText().toString();
			mes = mesCreateDb + getString(R.string.fmFileNoWritePermissions) + "\n" + path + file;

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
        	String fileName = "";
        	if (etFile != null)
				fileName = etFile.getText().toString();
        	mes = getString(R.string.fmPgnError) + " (" + fileName + ")";
        	pgnDialog = new C4aDialog(this, this, getString(R.string.dgTitleFileDialog),
        			"", getString(R.string.btn_Ok), "", mes, 0, "");
        	pgnDialog.setOnCancelListener(this);
            return pgnDialog;
        } 
		if (id == DELETE_FILE_DIALOG)
		{
			String delText = getString(R.string.fmDeleteFileQuestion) + " " + fileName + "?";
			deleteDialog = new C4aDialog(this, this, getString(R.string.dgTitleFileDialog),
					"", getString(R.string.btn_Ok), "", delText, 0, "");
			deleteDialog.setOnCancelListener(this);
			return deleteDialog;
		}

		if (id == DELETE_GAME_DIALOG)
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
			builder.setTitle(getString(R.string.delete_game));
			builder.setMessage("#" + gameDbId + "\n" + gamePlayerWhite + "\n" + gamePlayerBlack);
			builder.setPositiveButton(getString(R.string.btn_Ok), new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int id)
				{
					startEditPgnTask(gamePath, gameFile, gameData, Integer.toString(6),
							Long.toString(gameReplaceStart), Long.toString(gameReplaceEnd), Long.toString(gameMoveStart));
				}
			});
			builder.setNegativeButton(getString(R.string.btn_Cancel), null);
			AlertDialog alert = builder.create();
			return alert;
		}

		if (id == ENGINE_INSTALL_DIALOG)
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.AlertDialogTheme);
			builder.setTitle(getString(R.string.menu_enginesettings_install) + ": " + etFile.getText());
			builder.setMessage(ce.mesInitProcess);
			builder.setPositiveButton(getString(R.string.btn_Ok), null);
			builder.setNegativeButton(getString(R.string.menu_enginesettings_select), new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int id)
				{
					returnIntent = new Intent();
					returnIntent.putExtra("filePath", etPath.getText().toString());
					returnIntent.putExtra("fileName", etFile.getText().toString());
					setResult(82, returnIntent);
					finish();
				}
			});
			builder.setCancelable(true);
			AlertDialog alert = builder.create();
			return alert;
		}

		if (id == ADD_FOLDER_DIALOG)
        {
        	mes = getString(R.string.fmAddFolder);
        	addFolderDialog = new C4aDialog(this, this, getString(R.string.dgTitleFileDialog), 
        			"", getString(R.string.btn_Ok), "", mes, 1, defaultFolder);
        	addFolderDialog.setOnCancelListener(this);
            return addFolderDialog;
        }

		if (id == ADD_FILE_DIALOG)
		{
			mes = getString(R.string.fmAddFile);
			addFileDialog = new C4aDialog(this, this, getString(R.string.dgTitleFileDialog),
					"", getString(R.string.btn_Ok), "", mes, 1, PGN_EXTENSION);
			addFileDialog.setOnCancelListener(this);
			return addFileDialog;
		}

        if (id == DATABASE_LOCKED_DIALOG) 
        {
			c4aDialog = new C4aDialog(this, this, getString(R.string.dgTitleFileDialog),
					"", getString(R.string.btn_Ok), "", getString(R.string.fmDatabaseLocked), 0, "");
			return c4aDialog;
        }
        if (id == COMING_SOON) 
        {
			c4aDialog = new C4aDialog(this, this, getString(R.string.dgTitleFileDialog),
					"", getString(R.string.btn_Ok), "", getString(R.string.comingSoon), 0, "");
			return c4aDialog;
        }
		if (id == FILE_LOAD_PROGRESS_DIALOG)
		{
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage(getString(R.string.fmProgressDialog));
			progressDialog.setCancelable(true);
			progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener()
			{
				public void onCancel(DialogInterface dialog) { }
			});
			return progressDialog;
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

		if (id == MENU_QUERY_DIALOG)
		{
			final int QUERY_GAME_ID     = 0;
			final int QUERY_PLAYER  	= 1;
			final int QUERY_EVENT      	= 2;
			final int QUERY_ECO      	= 3;
			List<CharSequence> lst = new ArrayList<CharSequence>();
			List<Integer> actions = new ArrayList<Integer>();
			lst.add(getString(R.string.menu_query_gameId));     	actions.add(QUERY_GAME_ID);
			lst.add(getString(R.string.menu_query_player)); 		actions.add(QUERY_PLAYER);
			lst.add(getString(R.string.menu_query_event));      	actions.add(QUERY_EVENT);
			lst.add(getString(R.string.menu_query_eco));     		actions.add(QUERY_ECO);
			final List<Integer> finalActions = actions;
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.menu_query);
			builder.setItems(lst.toArray(new CharSequence[lst.size()]), new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int item)
				{
					switch (finalActions.get(item))
					{
						case QUERY_GAME_ID:
							dataQueryGameID();
							break;
						case QUERY_PLAYER:
							dataQueryPlayer();
							break;
						case QUERY_EVENT:
							dataQueryEvent();
							break;
						case QUERY_ECO:
							dataQueryEco();
							break;
					}
				}
			});
			AlertDialog alert = builder.create();
			return alert;
		}

		if (id == MENU_FILE_DIALOG)
		{
			final int FILE_NEW     	= 0;
			final int FILE_DELETE  	= 1;
			final int FOLDER_NEW  	= 2;
			List<CharSequence> lst = new ArrayList<CharSequence>();
			List<Integer> actions = new ArrayList<Integer>();
			lst.add(getString(R.string.menu_pgn_create));     	actions.add(FILE_NEW);
			lst.add(getString(R.string.menu_pgn_delete)); 		actions.add(FILE_DELETE);
			lst.add(getString(R.string.fmAddFolder));       	actions.add(FOLDER_NEW);
			final List<Integer> finalActions = actions;
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.fmLblFile);
			builder.setItems(lst.toArray(new CharSequence[lst.size()]), new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int item)
				{
					switch (finalActions.get(item))
					{
						case FILE_NEW:
							removeDialog(ADD_FILE_DIALOG);
							showDialog(ADD_FILE_DIALOG);
							break;
						case FILE_DELETE:
							removeDialog(DELETE_FILE_DIALOG);
							showDialog(DELETE_FILE_DIALOG);
							break;
						case FOLDER_NEW:
							removeDialog(ADD_FOLDER_DIALOG);
							showDialog(ADD_FOLDER_DIALOG);
							break;
					}
				}
			});
			AlertDialog alert = builder.create();
			return alert;
		}

		if (id == MENU_EDIT_PGN)
		{
			gameData = getIntent().getExtras().getString("pgnData");
			final int EDIT_PGN_BEFORE     	= 1;
			final int EDIT_PGN_AFTER  		= 2;
			final int EDIT_PGN_REPLACE      = 3;
			final int EDIT_PGN_START      	= 0;
			final int EDIT_PGN_END    		= 9;
			List<CharSequence> lst = new ArrayList<CharSequence>();
			List<Integer> actions = new ArrayList<Integer>();
			lst.add(getString(R.string.menu_edit_pgn_before));    	 	actions.add(EDIT_PGN_BEFORE);
			lst.add(getString(R.string.menu_edit_pgn_after)); 			actions.add(EDIT_PGN_AFTER);
			lst.add(getString(R.string.menu_edit_pgn_replace));   		actions.add(EDIT_PGN_REPLACE);
			lst.add(getString(R.string.menu_edit_pgn_start));     	actions.add(EDIT_PGN_START);
			lst.add(getString(R.string.menu_edit_pgn_end));     		actions.add(EDIT_PGN_END);
			final List<Integer> finalActions = 	actions;
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.menu_edit_pgn);
			builder.setItems(lst.toArray(new CharSequence[lst.size()]), new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int item)
				{
					switch (finalActions.get(item))
					{
						case EDIT_PGN_BEFORE:
							startEditPgnTask(gamePath, gameFile, gameData, Integer.toString(EDIT_PGN_BEFORE),
									Long.toString(gameReplaceStart), Long.toString(0), Long.toString(0));
							break;
						case EDIT_PGN_AFTER:
							if (gameReplaceEnd == pgnLength)
								saveFile(true);
							else
							startEditPgnTask(gamePath, gameFile, gameData, Integer.toString(EDIT_PGN_AFTER),
									Long.toString(gameReplaceEnd +1), Long.toString(0), Long.toString(0));
							break;
						case EDIT_PGN_REPLACE:
							startEditPgnTask(gamePath, gameFile, gameData, Integer.toString(EDIT_PGN_REPLACE),
									Long.toString(gameReplaceStart), Long.toString(gameReplaceEnd), Long.toString(0));
							break;
						case EDIT_PGN_START:
							startEditPgnTask(gamePath, gameFile, gameData, Integer.toString(EDIT_PGN_START),
									Long.toString(0), Long.toString(0), Long.toString(0));
							break;
						case EDIT_PGN_END:
							saveFile(true);
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
		isQueryInputError = false;
		if (fileActionCode == 2 & !fileIO.fileExists(etPath.getText().toString(), etFile.getText().toString()))
		{
		    if (lvGames != null)
            {
				if (lvGames.isShown())
					lvGames.setVisibility(ListView.INVISIBLE);
            }
		}
		if (setQueryData == 0)
		{	// query game-ID
			int game_count = Integer.parseInt(qGameCount.getText().toString());
			int game_current = 0;
			int game_max_items = 0;
			try 	{game_current = Integer.parseInt(qCurrentId.getText().toString());}
			catch 	(NumberFormatException e) {qCurrentId.setBackgroundResource(R.drawable.rectanglepink); isQueryInputError = true; return;}
			try 	{game_max_items = Integer.parseInt(qMaxItems.getText().toString());}
			catch 	(NumberFormatException e) {qMaxItems.setBackgroundResource(R.drawable.rectanglepink); isQueryInputError = true; return;}
			if (game_current < 1 | game_current > game_count)
			{
				qCurrentId.setBackgroundResource(R.drawable.rectanglepink);
				isQueryInputError = true;
			}
			else
				qCurrentId.setBackgroundResource(R.drawable.rectanglegreen);
			if (game_max_items < 100 | (game_max_items > 4000))
			{
				qMaxItems.setBackgroundResource(R.drawable.rectanglepink);
				isQueryInputError = true;
			}
			else
				qMaxItems.setBackgroundResource(R.drawable.rectanglegreen);
		}
		if (setQueryData == 1)
		{	// query player
			if (!qPlayer.getText().toString().equals(""))
				qPlayer.setBackgroundResource(R.drawable.rectanglegreen);
			else
			{
				qPlayer.setBackgroundResource(R.drawable.rectanglepink);
				isQueryInputError = true;
			}
			if (isDateOk(qDateFrom.getText().toString()))
				qDateFrom.setBackgroundResource(R.drawable.rectanglegreen);
			else
			{
				qDateFrom.setBackgroundResource(R.drawable.rectanglepink);
				isQueryInputError = true;
			}
			if (isDateOk(qDateTo.getText().toString()))
				qDateTo.setBackgroundResource(R.drawable.rectanglegreen);
			else
			{
				qDateTo.setBackgroundResource(R.drawable.rectanglepink);
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
						qDateFrom.setBackgroundResource(R.drawable.rectanglepink);
						qDateTo.setBackgroundResource(R.drawable.rectanglepink);
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
//Log.i(TAG, "startLoad(), fileActionCode: " + fileActionCode + ", location: " + location + ", isStart: " + isStart);
		title.setText(getString(R.string.fmTitleLoad));
		if (fileActionCode == 81)
			title.setText(getString(R.string.menu_enginesettings_install));
		if (fileActionCode == 82)
			title.setText(getString(R.string.menu_enginesettings_select));
		if (fileActionCode == 91)
			title.setText(getString(R.string.epOpeningBook));
		relLayout.setBackgroundColor(getResources().getColor(R.color.fm_load));
		String path = "";
		lblFile.setVisibility(ListView.VISIBLE);
		etUrl.setVisibility(ListView.INVISIBLE);
		etUrl.setEnabled(false);
		switch (location) 											// External | Intern | WWW
		{
			case 1:		// External
				etPath.setVisibility(ListView.VISIBLE);
				etFile.setVisibility(ListView.VISIBLE);
				lvFiles.setVisibility(ListView.VISIBLE);
				etPath.setText(baseDir);
				etFile.setText("");
				fmBtnAction.setVisibility(ImageView.INVISIBLE);
				fmBtnGames.setVisibility(ImageView.INVISIBLE);
				if (isStart)
				{
					etPath.setText(fm_extern_load_path);
					if (!fm_extern_load_file.equals(fm_file_extension))
					{
						etFile.setText(fm_extern_load_file);
						if (fmPrefs.getBoolean("fm_isGamesShown", true))
						{
							db_state = getDbFileState(fm_extern_load_file, fm_location);
							handleDb(fm_extern_load_path, fm_extern_load_file, db_state, false);
						}
						else
						{
							if (fileActionCode == 1)
							{
//Log.i(TAG, "startLoad(), fm_extern_load_last_path: " + fmPrefs.getString("fm_extern_load_last_path", fmPrefs.getString("fm_extern_load_path", "")));
								etPath.setText(fmPrefs.getString("fm_extern_load_last_path", fmPrefs.getString("fm_extern_load_path", "")));
								etFile.setText(fmPrefs.getString("fm_extern_load_last_file", fmPrefs.getString("fm_extern_load_file", "")));
							}
						}
					}
					else
						etFile.setText("");
				}
				if (fileActionCode == 81) // load engine from sd
				{
					if (!fmPrefs.getString("fm_extern_engine_path", "").equals(""))
					{
						fmBtnAction.setVisibility(ImageView.VISIBLE);
						btnMenu.setVisibility(ImageView.INVISIBLE);
						fmBtnGames.setVisibility(ImageView.INVISIBLE);
						etPath.setText(fmPrefs.getString("fm_extern_engine_path", ""));
						etFile.setText(fmPrefs.getString("fm_extern_engine_file", ""));
					}
				}
				if (fm_file_extension.endsWith(".bin"))
				{
					if (!fmPrefs.getString("fm_extern_book_path", "").equals(""))
					{
						fmBtnAction.setVisibility(ImageView.VISIBLE);
						btnMenu.setVisibility(ImageView.INVISIBLE);
						fmBtnGames.setVisibility(ImageView.INVISIBLE);
						etPath.setText(fmPrefs.getString("fm_extern_book_path", ""));
						etFile.setText(fmPrefs.getString("fm_extern_book_file", ""));
					}
				}
				path = path + etPath.getText();
				etPath.setSelection(etPath.getText().length());
				showFileList(path);
				if (isStart)
				{
					if (!etFile.getText().toString().equals(""))
					{
						if 	(fileActionCode == 1 & userPrefs.getBoolean("user_options_gui_usePgnDatabase", true))
						{
							fmBtnAction.setVisibility(ImageView.VISIBLE);
							fmBtnGames.setVisibility(ImageView.VISIBLE);
						}
						if (fileActionCode == 1)
							setFmInfo(getString(R.string.fmInfoLoadGame));
						if (fileActionCode == 2)
							setFmInfo(getString(R.string.fmInfoSaveGame));
					}
				}
				break;
			case 2:		// Intern
				fmBtnAction.setVisibility(ImageView.VISIBLE);
				btnDirBack.setVisibility(ImageView.INVISIBLE);
				btnMenu.setVisibility(ImageView.INVISIBLE);
				fmBtnGames.setVisibility(ImageView.INVISIBLE);
				String setFileName = fmPrefs.getString("fm_intern_engine_file", ce.assetsEngineProcessName);
				if (!getIntent().getExtras().getString("fileName").equals(""))
					setFileName = getIntent().getExtras().getString("fileName");
				if (fileActionCode == 82) // load engine from //data
				{
					etPath.setText(fmPrefs.getString("fm_intern_engine_path", ce.efm.dataEnginesPath));
					etPath.setSelection(etPath.getText().length());
					etFile.setText(setFileName);
				}
				showFileListFromData(setFileName);
				break;
			case 3:		// WWW
				etUrl.setVisibility(ListView.VISIBLE);
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
		title.setText(getString(R.string.fmTitleSave));
		relLayout.setBackgroundColor(getResources().getColor(R.color.fm_save));
		lblFile.setVisibility(ListView.VISIBLE);
		etUrl.setVisibility(ListView.INVISIBLE);
		etUrl.setEnabled(false);
		String save_path = fm_extern_save_path;
		String save_file = fm_extern_save_file;
		if (fileActionCode == 5)
		{
			title.setText(getString(R.string.fmTitleEngineAutoPlay));
			save_path = fm_extern_save_auto_path;
			save_file = fm_extern_save_auto_file;
		}
		etPath.setText(save_path);
		etPath.setSelection(etPath.getText().length());
		etFile.setVisibility(ListView.VISIBLE);
		if (save_file.equals(""))
		{
			etFile.setText(fm_file_extension);
		}
		else
		{
			if (fileIO.fileExists(save_path, save_file))
			{
				etFile.setText(save_file);
				if (fileActionCode == 2)
				{
					db_state = getDbFileState(fm_extern_save_file, fm_location);
					save_is_start = true;
					handleDb(fm_extern_save_path, fm_extern_save_file, db_state, false);
				}
			}
			else
				etFile.setText(fm_file_extension);
		}
		fm_extern_db_key_id = 0;	// save with gameId!
		if (!etPath.getText().equals(""))
			showFileList(etPath.getText().toString());
		else
			showFileList(baseDir);
		if (fileActionCode == 2)
			setFmInfo(getString(R.string.fmInfoSaveGame));
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
		if (!fileIO.canWrite(path, file))
		{
			removeDialog(FILE_NO_WRITE_PERMISSIONS);
			showDialog(FILE_NO_WRITE_PERMISSIONS);
			return;
		}
		if (fileIO.pathExists(path))
		{
			String data = getIntent().getExtras().getString("pgnData");
			boolean fileExists = fileIO.fileExists(path, file);
			if (userPrefs.getBoolean("user_options_gui_usePgnDatabase", true))
			{
				boolean pgnFilesOk = pgnDb.initPgnFiles(path, file);
				if (pgnFilesOk)
				{

//					pgnDb.openDb(path, file, SQLiteDatabase.OPEN_READONLY);
					if (pgnDb.openDb(path, file, SQLiteDatabase.OPEN_READONLY))
					{
						long pgnOldLength = pgnDb.pgnLength;
						fileIO.dataToFile(path, file, data, fileExists);
						pgnDb.closeDb();
						startCreateDatabaseTask(path, file, Long.toString(pgnOldLength), "");
					}

				}
				else
				{
					fileIO.dataToFile(path, file, data, fileExists);
					pgnDb.deleteDbFile();
					startCreateDatabaseTask(path, file, "0", "");
				}
			}
			else
				fileIO.dataToFile(path, file, data, fileExists);
		}
		returnIntent = new Intent();
		setResult(22, returnIntent);
	}

	public void showFileList(String path)
	{
//Log.i(TAG, "showFileList(), path: " + path);
		if (fileActionCode == 91 | fileActionCode == 5 | fileActionCode == 81 | fileActionCode == 82)
			fmBtnAction.setVisibility(ImageView.VISIBLE);
		else
			fmBtnAction.setVisibility(ImageView.INVISIBLE);
		fmBtnGames.setVisibility(ImageView.INVISIBLE);
		String[] fileA;

		if (fileIO.pathExists(path) | path.equals(""))
        {
			if (path.equals(""))
				fileA = fileIO.getExternalDirs();
			else
				fileA = fileIO.getFileArrayFromPath(path, true, fm_file_extension);
			if (fileA != null)
			{
				files = new ArrayAdapter<String>(this, R.layout.c4alistitem, fileA);
				lvFiles.setAdapter(files);
				lvFiles.setTextFilterEnabled(true);
				lvFiles.setVisibility(ListView.VISIBLE);
				lvFiles.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
				int scrollId = -1;
				for (int i = 0; i < fileA.length; i++)
				{
					if (fileA[i].equals(etFile.getText().toString()))
					{
						scrollId = i;
						break;
					}
				}
				if (scrollId >= 0)
				{
					lvFiles.setItemChecked(scrollId, true);
					lvFiles.setSelection(scrollId);
					int h1 = lvFiles.getHeight();
					int h2 = 100;
					lvFiles.setSelectionFromTop(scrollId, h1 / 2 - h2 / 2);
				}
			}

			lvFiles.setOnItemClickListener(new OnItemClickListener()
			{
				@Override
				public void onItemClick(AdapterView<?> listView, View view, int position, long id)
				{
					if (isQuery) return;
					String itemName = files.getItem(position);
					boolean isEngineFile = false;
					File f = new File(etPath.getText().toString(), itemName);
					if (fm_file_extension.equals("") & f.isFile() & !f.isDirectory())
						isEngineFile = true;
					if ((itemName.endsWith(fm_file_extension) & !fm_file_extension.equals("")) | isEngineFile)
                    {
//Log.i(TAG, "showFileList(), onItemClick(), itemName: " + itemName);
                        showPositionInFileList(position, itemName);
                    }
					else
					{
						etPath.setText(etPath.getText().toString() + itemName  + "/");
						etPath.setSelection(etPath.getText().length());
						if (fileActionCode != 2)
							etFile.setText("");
						else
							etFile.setText(fm_file_extension);
						if (fm_location == 1)
							showFileList(etPath.getText().toString());
					}
				}
			});

			lvFiles.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
			{
				@Override
				public boolean onItemLongClick(AdapterView<?> parent, View view,  int position, long id)
				{
					String itemName = files.getItem(position);
					if (itemName.endsWith(fm_file_extension))
					{
						if (fileIO.fileExists(etPath.getText().toString(), itemName))
						{
							etFile.setText(itemName);
							fileName = itemName;
							// delete .pgn and .pgn-db file
							removeDialog(DELETE_FILE_DIALOG);
							showDialog(DELETE_FILE_DIALOG);
						}
					}
					return true;
				}
			});

			switch (fileActionCode)
			{
				case 81: setFmInfo(getString(R.string.fmInfoInstallEngine)); break;
				case 91: setFmInfo(getString(R.string.fmInfoLoadBook)); break;
				default: setFmInfo(getString(R.string.fmInfoSelectFile)); break;
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

	public void showFileListFromData(String setFileName)
	{
		fmBtnAction.setVisibility(ImageView.VISIBLE);
		String[] fileA = ce.efm.getFileArrayFromData(ce.efm.dataEnginesPath);
		if (fileA != null)
		{
			files = new ArrayAdapter<String>(this, R.layout.c4alistitem, fileA);
			lvFiles.setAdapter(files);
			lvFiles.setTextFilterEnabled(true);
			lvFiles.setVisibility(ListView.VISIBLE);
			lvFiles.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
			int scrollId = -1;
			for (int i = 0; i < fileA.length; i++)
			{
				if (fileA[i].equals(setFileName))
				{
					scrollId = i;
					break;
				}
			}
			if (fileA.length > 0 & scrollId == -1)
			{
				scrollId = 0;
				etFile.setText(fileA[0]);
				SharedPreferences.Editor ed = fmPrefs.edit();
				ed.putString("fm_intern_engine_file", etFile.getText().toString());
				ed.commit();
			}
			if (scrollId >= 0)
			{
				lvFiles.setItemChecked(scrollId, true);
				lvFiles.setSelection(scrollId);
				int h1 = lvFiles.getHeight();
				int h2 = 100;
				lvFiles.setSelectionFromTop(scrollId, h1 / 2 - h2 / 2);
			}

			lvFiles.setOnItemClickListener(new OnItemClickListener()
			{
				@Override
				public void onItemClick(AdapterView<?> listView, View view, int position, long id)
				{
					String itemName = files.getItem(position);
//Log.i(TAG, "showFileList(), onItemClick(), itemName: " + itemName);
					showPositionInFileList(position, itemName);
					etPath.setSelection(etPath.getText().length());
				}
			});

			lvFiles.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
			{
				@Override
				public boolean onItemLongClick(AdapterView<?> parent, View view,  int position, long id)
				{
					String itemName = files.getItem(position);
					if (fileIO.fileExists(etPath.getText().toString(), itemName))
					{
						etFile.setText(itemName);
						fileName = itemName;
						removeDialog(DELETE_FILE_DIALOG);
						showDialog(DELETE_FILE_DIALOG);
					}
					return true;
				}
			});
			setFmInfo(getString(R.string.fmInfoSelectEngine));
		}
		else
		{
			etPath.setText("");
			etFile.setText("");
			removeDialog(PATH_NOT_EXISTS_DIALOG);
			showDialog(PATH_NOT_EXISTS_DIALOG);
		}
	}

	public void showPositionInFileList(int position, String fileName)
	{
		int scrollId = position;
		lvFiles.setItemChecked(scrollId, true);
		lvFiles.setSelection(scrollId);
		int h1 = lvFiles.getHeight();
		int h2 = 100;
		lvFiles.setSelectionFromTop(scrollId, h1/2 - h2/2);
		etFile.setText(fileName);
		fmBtnAction.setVisibility(ImageView.VISIBLE);
		if 	(!userPrefs.getBoolean("user_options_gui_usePgnDatabase", true))	// no db
		{
			if (fileActionCode == 1)
			{
				fm_extern_skip_bytes = 0;
				loadFile();
			}
			if (fileActionCode == 2)
			{
				fmBtnAction.setVisibility(ImageView.VISIBLE);
			}
			return;
		}

		if 	((fileActionCode == 1 | fileActionCode == 2) & fm_location == 1 & userPrefs.getBoolean("user_options_gui_usePgnDatabase", true))
		{
			if (fileActionCode == 2)
			{
				SharedPreferences.Editor ed = fmPrefs.edit();
				ed.putString("fm_extern_save_path", etPath.getText().toString());
				ed.putString("fm_extern_save_file", etFile.getText().toString());
				ed.commit();
			}
			scroll_game_id = 1;
			fm_extern_db_key_id = 0;
			db_state = getDbFileState(fileName, fm_location);
//Log.i(TAG, "showPositionInFileList(), db_state: " + db_state);
			handleDb(etPath.getText().toString(), fileName, db_state, true);
			if (fileActionCode == 1)
			{
				SharedPreferences.Editor ed = fmPrefs.edit();
				ed.putString("fm_extern_load_last_path", etPath.getText().toString());
				ed.putString("fm_extern_load_last_file", etFile.getText().toString());
				ed.commit();
			}
		}
	}

	public void loadFile()
	{
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
		returnIntent = new Intent();
		if (fileIO.pathExists(path))
        {
			if (!path.equals(fm_extern_load_path) | !file.equals(fm_extern_load_file))
				gameControl = 1;
//Log.i(TAG, "loadExternFile(), gameControl: " + gameControl  + ", fm_extern_game_offset: " + fm_extern_game_offset);
        	fileData = fileIO.dataFromFile(path, file, fm_extern_last_game, gameControl, fm_extern_game_offset);
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
//Log.i(TAG, "finishAfterLoad()");
		returnIntent = new Intent();
		returnIntent.putExtra("pgnStat", fileIO.getPgnStat());
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
		{
			setPreferences(fileData);
		}
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
            String target = fileIO.getExternalDirectory(0) + fmPrefs.getString("fm_extern_load_path", "") + "/test.pgn";
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
		if (!fileIO.canWrite(etPath.getText().toString(), fileName))
		{
			removeDialog(FILE_NO_WRITE_PERMISSIONS);
			showDialog(FILE_NO_WRITE_PERMISSIONS);
			return;
		}
		returnIntent = new Intent();
		String data = getIntent().getExtras().getString("pgnData");

		if (data == null)
			return;
		if (data.equals(""))
			return;

		fileName = etFile.getText().toString();
		save_action_id = 5;
		if (userPrefs.getBoolean("user_options_gui_usePgnDatabase", true))
		{
			boolean createDb = true;
			boolean pgnFilesOk = pgnDb.initPgnFiles(etPath.getText().toString(), fileName);
			if (pgnFilesOk)
			{
				if (pgnDb.openDb(etPath.getText().toString(), fileName, SQLiteDatabase.OPEN_READONLY))
				{
					long pgnOldLength = pgnDb.pgnLength;
					save_selected_scroll_game_id = pgnDb.getRowCount(PgnDb.TABLE_NAME);
					save_scroll_game_id = save_selected_scroll_game_id 	+1;
//Log.i(TAG, "saveFile(), save_action_id: " + save_action_id);
//Log.i(TAG, "saveFile(), save_selected_scroll_game_id: " + save_selected_scroll_game_id + ", save_scroll_game_id: " + save_scroll_game_id);
					fileIO.dataToFile(etPath.getText().toString(), fileName, data, append);
					pgnDb.closeDb();
					createDb = false;
					startCreateDatabaseTask(etPath.getText().toString(), fileName, Long.toString(pgnOldLength), "");
				}
			}
			if (createDb)
			{
				fileIO.dataToFile(etPath.getText().toString(), fileName, data, append);
				startCreateDatabaseTask(etPath.getText().toString(), fileName, "0", "");
			}
		}
		else
		{
			fileIO.dataToFile(etPath.getText().toString(), fileName, data, append);
			Toast.makeText(this, getString(R.string.game_saved), Toast.LENGTH_SHORT).show();
			setPreferences("");
			finish();
		}

		setPreferences("");

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
		if (fileIO.isExternalDir(oldPath))
			return newPath;
		int lastDirPos = 0;
		for (int i = 0; i < oldPath.length(); i++) 
    	{
			if (oldPath.charAt(i) == '/' & i != oldPath.length() -1)
				lastDirPos = i +1;
    	}
		if (lastDirPos > 0)
			newPath = oldPath.substring(0, lastDirPos);
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
		if (!fileIO.pathExists(fm_extern_save_path))
			fm_extern_save_path = fileIO.getNewExternalPath(fm_extern_save_path);
		fm_extern_save_file = fmPrefs.getString("fm_extern_save_file", "");
		fm_extern_save_auto_path = fmPrefs.getString("fm_extern_save_auto_path", "");
		if (!fileIO.pathExists(fm_extern_save_auto_path))
			fm_extern_save_auto_path = fileIO.getNewExternalPath(fm_extern_save_auto_path);
		fm_extern_save_auto_file = fmPrefs.getString("fm_extern_save_auto_file", "");
		fm_extern_skip_bytes = fmPrefs.getLong("fm_extern_skip_bytes", 0);
		fm_extern_game_offset = fmPrefs.getLong("fm_extern_game_offset", 0);
		if (fm_location == 1)
			fm_extern_last_game = fmPrefs.getString("fm_extern_last_game", "");

		fm_url = fmPrefs.getString("fm_url", pgnUrl);
		if (fm_url.equals(""))
			fm_url = pgnUrl;
		showGameListView = true;
		fm_extern_load_path = fmPrefs.getString("fm_extern_load_path", "");
		if (!fileIO.pathExists(fm_extern_load_path))
			fm_extern_load_path = fileIO.getNewExternalPath(fm_extern_load_path);
		fm_extern_load_file = fmPrefs.getString("fm_extern_load_file", "");

		String bookPath = fmPrefs.getString("fm_extern_book_path", "");
		if (!fileIO.pathExists(bookPath))
		{
			if (bookPath.equals(""))
				bookPath = fileIO.getExternalDirectory(0);
			else
				bookPath = fileIO.getNewExternalPath(bookPath);
			SharedPreferences.Editor ed = fmPrefs.edit();
			ed.putString("fm_extern_book_path", bookPath);
			ed.commit();
		}

		String enginePath = fmPrefs.getString("fm_extern_engine_path", "");
		if (!fileIO.pathExists(enginePath))
		{
			if (enginePath.equals(""))
				enginePath = fileIO.getExternalDirectory(0);
			else
				enginePath = fileIO.getNewExternalPath(enginePath);
			SharedPreferences.Editor ed = fmPrefs.edit();
			ed.putString("fm_extern_engine_path", enginePath);
			ed.commit();
		}

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

		if (fm_extern_db_key_id == 0)
			scroll_game_id = fm_extern_db_game_id;
		else
		{
			gameIdList = getGameIdList(fm_extern_db_game_id_list);
			if (gameIdList != null)
			{
				fm_extern_db_cursor_count = gameIdList.length;
			}
			scroll_game_id = fm_extern_db_cursor_id +1;
		}
	}

	public void setPreferences(String gameData) 
	{
//Log.i(TAG, "setPreferences(), gameData\n" + gameData);
		if (etPath == null)
			return;

        SharedPreferences.Editor ed = fmPrefs.edit();
        if (lvGames != null)
		{
			ed.putBoolean("fm_isGamesShown", lvGames.isShown());
			if (!lvGames.isShown() & fileActionCode == 1)
			{
				ed.putString("fm_extern_load_last_path", etPath.getText().toString());
				ed.putString("fm_extern_load_last_file", etFile.getText().toString());
			}
		}
        else
			ed.putBoolean("fm_isGamesShown", true);
        ed.putInt("fm_location", fm_location);
        if (fileActionCode == 1)
        	ed.putInt("fm_load_location", fm_location);
//Log.i(TAG, "fm_location: " + fm_location + ", fileActionCode: " + fileActionCode);
        switch (fm_location)
        { 
        case 1:
        	if (fileActionCode == 1)	// load
        	{
//Log.i(TAG, "setPreferences(), gameData\n" + gameData);
				ed.putInt("fm_last_file_action_code", fileActionCode);
				ed.putString("fm_extern_load_path", etPath.getText().toString());
				ed.putString("fm_extern_load_file", etFile.getText().toString());
//Log.i(TAG, "pgnDb.pgnGameOffset, fileIO.gameOffset: " + pgnDb.pgnGameOffset + ", " + fileIO.gameOffset);
				if (userPrefs.getBoolean("user_options_gui_usePgnDatabase", true))
					ed.putLong("fm_extern_game_offset", pgnDb.pgnGameOffset);
				else
					ed.putLong("fm_extern_game_offset", fileIO.gameOffset);
				if (!gameData.equals(""))
					ed.putString("fm_extern_last_game", gameData);
				if (userPrefs.getBoolean("user_options_gui_usePgnDatabase", true))
					setDbPreferences();
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
			ed.putString("fm_intern_engine_path", etPath.getText().toString());
			ed.putString("fm_intern_engine_file", etFile.getText().toString());
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
//Log.i(TAG, "setSkipPreferences(), gameData\n" + gameData);
        SharedPreferences.Editor ed = fmPrefs.edit();
        if (location == 1)
        {
//        	Log.i(TAG, "\nfm_extern_skip_bytes: " + fileIO.getSkipBytes());
        	if (!gameData.equals(""))
        	{
	        	ed.putString("fm_extern_last_game", gameData);
	        	if (userPrefs.getBoolean("user_options_gui_usePgnDatabase", true))
	        		ed.putLong("fm_extern_game_offset", pgnDb.pgnGameOffset);
	        	else
	        		ed.putLong("fm_extern_game_offset", fileIO.gameOffset);
	        	if (userPrefs.getBoolean("user_options_gui_usePgnDatabase", true))
	        		setDbPreferences();
	        	else
	        		ed.putLong("fm_extern_game_offset", fileIO.gameOffset);
        	}
        }
        ed.commit();
	}

	public void setDbPreferences() 
	{
//Log.i(TAG, "setDbPreferences()");
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
//Log.i(TAG, "setQueryPreferences(), gameId: " + gameId);
		SharedPreferences.Editor ed = fmPrefs.edit();
		String white = "";
		String black = "";
		String event = "";
		String site = "";
		String eco = "";
		String opening = "";
		String variation = "";
		if (pgnDb.openDb(fm_extern_load_path, fm_extern_load_file, SQLiteDatabase.OPEN_READONLY))
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
                data = fileIO.getDataFromInputStream(in);
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
        pgnDb.initPgnFiles(fm_extern_load_path, fm_extern_load_file);
 		if (pgnDb.openDb(fm_extern_load_path, fm_extern_load_file, SQLiteDatabase.OPEN_READONLY))
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
			title.setText(gameId + "[" + gameCount + "]");
		else
			title.setText(queryId + "(" + queryCount + "), " + gameId + "[" + gameCount + "]");
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
			qDateFrom.setBackgroundResource(R.drawable.rectanglegreen);
		else
		{
			qDateFrom.setBackgroundResource(R.drawable.rectanglepink);
			isQueryInputError = true;
		}
    	qDateTo.setText(fm_extern_db_key_player_date_to);
    	if (isDateOk(qDateTo.getText().toString()))
    		qDateTo.setBackgroundResource(R.drawable.rectanglegreen);
		else
		{
			qDateTo.setBackgroundResource(R.drawable.rectanglepink);
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

	//	pgn / pgn-db tasks			pgn / pgn-db tasks			pgn / pgn-db tasks			pgn / pgn-db tasks
	public class EditPgnTask extends AsyncTask<String, Long, String>
	{
		public EditPgnTask(Context context, int notificationId)
		{
			this.context = context;
			this.notificationId = notificationId;
			mNotificationHelper = new NotificationHelper(context, notificationId);
			if (pgnLength > 50000)
				Toast.makeText(context, getString(R.string.fmCreatingPgnDatabaseToast), Toast.LENGTH_LONG).show();

		}

		@Override
		protected String doInBackground(String... params)
		{
			pgnPath = params[0];
			pgnFile = params[1];
			pgnData = params[2];
			action = 0;
			replaceStart = 0;
			replaceEnd = 0;
			moveStart = 0;
			try
			{
				action = Integer.parseInt(params[3]);
				replaceStart = Long.parseLong(params[4]);
				replaceEnd = Long.parseLong(params[5]);
				moveStart = Long.parseLong(params[6]);
			}
			catch (NumberFormatException e)
			{
				return "NumberFormatException, doInBackground()";
			}
			save_action_id = action;
//Log.i(TAG, "pgnPath: " + pgnPath + ", pgnFile: " + pgnFile + ", pgnData:\n" + pgnData);
//Log.i(TAG, "action: " + action + ", replaceStart: " + replaceStart + ", replaceEnd: " + replaceEnd + ", moveStart: " + moveStart);
			mNotificationHelper.createNotification(getString(R.string.rebuild_pgn_file) + " " + pgnFile,
                    FileManager.PGN_ACTION_UPDATE, pgnPath + pgnFile);

			RandomAccessFile pgnRaf;
			try {pgnRaf = new RandomAccessFile(pgnPath + pgnFile, "r");}
			catch (FileNotFoundException e)
			{
				e.printStackTrace();
				return "FileNotFoundException: " + pgnPath + pgnFile;
			}

			long rafLinePointer = 0;
			long prevPercentage = 0;
			String line;
			tmpFile = pgnFile.replace(".pgn", ".tmp");;
			File f = new File(pgnPath + tmpFile);
			if (f.exists())
			{
				f.delete();
				return "file exists: " + tmpFile;
			}
			FileOutputStream fOut;

			try
			{
				long startTime = System.currentTimeMillis();
				fOut = new FileOutputStream(f);
				OutputStreamWriter osw = new OutputStreamWriter(fOut);

				pgnRaf.seek(0);
				rafLinePointer = pgnRaf.getFilePointer();
				line = pgnRaf.readLine();
				while (line != null)
				{
					boolean isWriteLine = false;
					switch (action)
					{
						case 0:		// beginning of file
						case 1:		// before selected
						case 2:		// after selected
							if (replaceStart == rafLinePointer)
								osw.write(pgnData + "\n");
							isWriteLine = true;
							break;
						case 3:		// replace selected
						case 6:		// delete selected
						case 7:		// delete selected and save current game (move)
							if (action == 3 & replaceStart == rafLinePointer)
								osw.write(pgnData + "\n");
							if (action == 7 & moveStart == rafLinePointer)
								osw.write(pgnData + "\n");
							if (rafLinePointer < replaceStart)
								isWriteLine = true;
							if (rafLinePointer > replaceEnd)
								isWriteLine = true;
							break;
					}

					if (isWriteLine)
						osw.write(line + "\n");

					rafLinePointer = pgnRaf.getFilePointer();
					long percentage = 0;
					try	{ percentage = (rafLinePointer * 100) / pgnRaf.length(); }
					catch (ArithmeticException e) {}
//Log.i(TAG, "prevPercentage: " + prevPercentage + ", percentage: " + percentage);
					if (prevPercentage != percentage & System.currentTimeMillis() - startTime > 2000)
					{
						publishProgress(percentage, rafLinePointer, pgnRaf.length());
						prevPercentage = percentage;
					}
					line = pgnRaf.readLine();

				}
				osw.flush();
				osw.close();
			}
			catch (FileNotFoundException e) 	{e.printStackTrace(); return "FileNotFoundException";}
			catch (IOException e)				{e.printStackTrace(); return "IOException";}


			return "OK";

		}

		@Override
		protected void onProgressUpdate(Long... progress)
		{
			if (pgnLength > 50000)
				mNotificationHelper.progressUpdate(progress[0], progress[1], progress[2], -0L);
		}

		@Override
		protected void onPostExecute(String msg)
		{
			mNotificationHelper.completed();
			if (!msg.equals("OK"))
			{
				Toast.makeText(FileManager.this, msg, Toast.LENGTH_SHORT).show();
				return;
			}
			else	// doInBackground() OK !
			{
				File f = new File(pgnPath + tmpFile);
				if (f.exists())
				{
					if (pgnDb.openDb(pgnPath, pgnFile, SQLiteDatabase.OPEN_READONLY))
						pgnDb.closeDb();
					boolean isDeletedPgn = true;
					boolean isDeletedPgnDb = true;
					File fPgn = new File(pgnPath + pgnFile);
					File fPgnDb = new File(pgnPath + pgnFile + "-db");
					if (fPgn.exists())
						isDeletedPgn = fileIO.fileDelete(pgnPath, pgnFile);
					if (fPgnDb.exists())
						isDeletedPgnDb = fileIO.fileDelete(pgnPath, pgnFile + "-db");
					if (isDeletedPgn & isDeletedPgnDb)
					{
						File from      = new File(pgnPath, tmpFile);
						File to      = new File(pgnPath, pgnFile);
						boolean isRename = from.renameTo(to);
						if (isRename)
						{
							save_scroll_game_id = save_selected_scroll_game_id;
							switch (action)
							{
								case 0:		// beginning of file
									save_scroll_game_id = 1;
								break;
								case 1:		// before selected
								case 3:		// replace selected
								case 6:		// delete selected
									save_scroll_game_id = save_selected_scroll_game_id;
									break;
								case 2:		// after selected
									save_scroll_game_id = save_selected_scroll_game_id 	+1;
									break;
								case 7:		// delete selected and save current game (move)

								break;
							}
							pgnDb = new PgnDb();
							pgnDb.initPgnFiles(pgnPath, pgnFile);
//							notificationId--;
							handleDb(pgnPath, pgnFile, STATE_DB_NO_DBFILE, false);
						}
					}
				}
			}
		}

		final String TAG = "EditPgnTask";
		String tmpFile = "";
		Context context;
		private NotificationHelper mNotificationHelper;
		int notificationId = 0;

		String pgnPath;
		String pgnFile;
		String pgnData;
		int action = 0;
		long replaceStart = 0;
		long replaceEnd = 0;
		long moveStart = 0;
	}

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
//Log.i(TAG, "pgnFile: " + pgnPath + pgnFile);
//Log.i(TAG, "CreateDatabaseTask(), pgnOffset: " + pgnOffset + ", dropCreateIdx: " + dropCreateIdx + ", dropIdx: " + dropIdx);
            startCreateDatabase = System.currentTimeMillis();
            fPgn = new File(pgnPath + pgnFile);
			if (fPgn.exists())
				pgnFileExists = true;
			else
			{
				pgnFileExists = false;
				return null;
			}
//Log.i(TAG, "CreateDatabaseTask(), fPgn.length(): " + fPgn.length());

			if (pgnOffset == 0 & fPgn.length() == 0)	// no data, no create db!
				return null;

			pgnDbFile = pgnFile.replace(".pgn", ".pgn-db");
			mNotificationHelper.createNotification(getString(R.string.fmCreatingPgnDatabase) + " " + pgnDbFile,
                    FileManager.PGN_ACTION_CREATE_DB, pgnPath + pgnFile);

			fPgnDb = new File(pgnPath + pgnDbFile);
			if (pgnOffset == 0 & !dropCreateIdx)
			{
				try	{db = SQLiteDatabase.openOrCreateDatabase(fPgnDb, null);}	// create database (file: .pgn-db)
				catch (SQLiteDiskIOException e) {e.printStackTrace(); runMessage = "EX 99"; db.close(); return null;}
				catch (SQLiteCantOpenDatabaseException e) {e.printStackTrace(); runMessage = "EX 99"; db.close(); return null;}
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
					publishProgress(999L, rafParsed, rafLength, GameId);
					executeSQLScript(db, "drop_idx.sql");		// drop indexes ON pgn
					executeSQLScript(db, "create_idx.sql");		// create indexes ON pgn
					db.setVersion(pgnDb.DB_VERSION);
					db.close();
					runMessage = "dropCreateIdx - 0";
					return null;
				}
				pgnRaf = new RandomAccessFile(pgnPath + pgnFile, "r");
				rafParsed = +pgnOffset;
				rafLength = pgnRaf.length();
				GameId = 0;
//Log.i(TAG, "rafParsed: " + rafParsed + ", rafLength: " + rafLength + ", GameId: " + GameId);
				publishProgress(getPercentageComplete(), rafParsed, rafLength, GameId);
			} 
			catch (FileNotFoundException e) {e.printStackTrace(); runMessage = "EX 1"; db.close(); return null;}	// error file not exist
			catch (IOException e) {e.printStackTrace(); runMessage = "EX 2"; db.close(); return null;}

			if (dbTest == DB_CREATE_TRANSACTION | dbTest == DB_CREATE_SQL_STM_TRANSACTION)
				db.beginTransaction();

			try
			{
				initGameValues();
				pgnRaf.seek(pgnOffset);
				rafLinePointer = pgnRaf.getFilePointer();
				line = pgnRaf.readLine();

                SQLiteStatement sqlInsert = getSqlInsert();

				while (line != null)
				{
//					Log.i(TAG, "line: " + line);
					if (line.startsWith("[Event "))
					{
						hasGames = true;
						if (isFirstGame)
						{
							isFirstGame = false;
							GameId++;
						}
						else
						{
							gameLength = (int) (rafLinePointer - rafGamePointer);
							setGameOffsets(rafGamePointer, gameLength, startMoveSection);
							if (!pgnDb.existsDbFile(pgnPath, pgnDbFile))
							{
								pgnRaf.close();
//								db.setTransactionSuccessful();
//								db.endTransaction();
								db.close();
								runMessage = "db not exists";
								return null;
							}

							boolean isInsertOk;
							if (dbTest < 10)
                                isInsertOk = insertValuesToDb();
							else
                                isInsertOk = insertValuesToDb(sqlInsert);
//							if (!insertValuesToDb())
							if (!isInsertOk)
							{	// SQLException
//                                Log.i(TAG, "1 !insertValuesToDb()");
								pgnRaf.close();

								db.setTransactionSuccessful();
								db.endTransaction();

								db.close();
								pgnDb.deleteDbFile();
								runMessage = "insertValuesToDb() - 1";
								return null;
							}
//							if (GameId % 25 == 0)
//								publishProgress(getPercentageComplete(), rafParsed, rafLength, GameId);
							if (GameId % 100 == 0)
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
//Log.i(TAG, "hasGames: " + hasGames);
				if (hasGames)
				{
					gameLength = (int) (pgnRaf.length() - rafGamePointer);
					setGameOffsets(rafGamePointer, gameLength, startMoveSection);

                    boolean isInsertOk;
                    if (dbTest < 10)
                        isInsertOk = insertValuesToDb();
                    else
                        isInsertOk = insertValuesToDb(sqlInsert);

//					if (!insertValuesToDb())
					if (!isInsertOk)
					{	// SQLException
//                        Log.i(TAG, "2 !insertValuesToDb()");
						pgnRaf.close();

						db.setTransactionSuccessful();
						db.endTransaction();

						db.close();
						pgnDb.deleteDbFile();
						runMessage = "insertValuesToDb() - 2";
						return null;
					}
					publishProgress(getPercentageComplete(), rafParsed, rafLength, GameId);
				}
				pgnRaf.close();
			}

			catch (IOException e) {e.printStackTrace(); runMessage = "EX 3"; return null;}
			catch (SQLiteDiskIOException e) {e.printStackTrace(); runMessage = "EX 4"; return null;}
			catch (SQLiteDatabaseCorruptException e) {e.printStackTrace(); runMessage = "EX 5"; return null;}
			catch (RuntimeException e) {e.printStackTrace(); runMessage = "EX 6"; return null;}

			stopTransaction();

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
			runMessage = "OK!";
			return null;

		}

		@Override  
		protected void onProgressUpdate(Long... progress) 
		{
	        mNotificationHelper.progressUpdate(progress[0], progress[1], progress[2], progress[3]);
//Log.i(TAG, "onProgressUpdate(), isStartTask: " + isStartTask + ", save_action_id: " + save_action_id);
	        if (isStartTask)
			{
				if (save_action_id == 0 & rafLength > 50000)
					Toast.makeText(context, getString(R.string.fmCreatingPgnDatabaseToast), Toast.LENGTH_LONG).show();
				isStartTask = false;
			}
	    }

		@Override  
	    protected void onPostExecute(Void l) 
		{

//			Log.i(TAG, "onPostExecute(), runMessage: " + runMessage);

			if (!runMessage.equals("OK!"))
			{
//				Log.i(TAG, "onPostExecute(), runMessage: " + runMessage);
				mNotificationHelper.completed();
				return;
			}

		    long diffTime = System.currentTimeMillis() - startCreateDatabase;
            String runningTime = String.format("%02d:%02d:%02d",
                    TimeUnit.MILLISECONDS.toHours(diffTime),
                    TimeUnit.MILLISECONDS.toMinutes(diffTime) -
                            TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(diffTime)), // The change is in this line
                    TimeUnit.MILLISECONDS.toSeconds(diffTime) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(diffTime)));

			Log.i(TAG, "runningTime: " + runningTime + ", games: " + (GameId -1));

            if (pgnOffset == 0 & fPgn.length() == 0)	// no data, no create db!
			{
				try
				{
					fmBtnAction.setVisibility(ImageView.VISIBLE);
					lvGames.removeAllViewsInLayout();
					lvGames.setVisibility(ListView.VISIBLE);
					emptyLv.setVisibility(TextView.VISIBLE);
				}
				catch (NullPointerException e) {e.printStackTrace();}

				return;
			}

			if (!pgnFileExists)
	        {
	        	fileName = pgnPath + pgnFile;
				removeDialog(FILE_NOT_EXISTS_DIALOG);
				showDialog(FILE_NOT_EXISTS_DIALOG);
	        	return;
	        }
	        mNotificationHelper.completed();
	        if (getIntent().getExtras().getInt("displayActivity") == 1)
	        {
// 13. Okt. 2018 23:27 in der App-Version 72 : java.lang.IllegalStateException:
//	        	pgnDb.openDb(pgnPath, pgnFile, SQLiteDatabase.OPEN_READONLY);
	        	if (pgnDb.openDb(pgnPath, pgnFile, SQLiteDatabase.OPEN_READONLY))
				{
					setInfoValues("DB-Test - onPostExecute()", runMessage, pgnDb.getDbVersion());
					pgnDb.closeDb();
				}

		        if (pgnPath.equals(etPath.getText().toString()) & pgnFile.equals(etFile.getText().toString()))
		        {
		        	if (pgnDb.initPgnFiles(pgnPath, pgnFile))
		        	{
//Log.i(TAG, "onPostExecute(), fileActionCode: " + fileActionCode + ", fm_extern_db_key_id: " + fm_extern_db_key_id);

//Log.i(TAG, "onPostExecute(), pgnDb.initPgnFiles(), pgnFile: " + pgnPath + pgnFile);
//                        if (true)
//                            return;

		        		fm_extern_db_key_id = 0;
						save_path = gamePath;
						save_file = gameFile;
						if (fileActionCode == 2)
						{
							setPreferences("");
							if (pgnPath == save_path & pgnFile == save_file)
								save_is_done = true;
							if (save_action_id == 6)
								Toast.makeText(context, getString(R.string.game_deleted), Toast.LENGTH_SHORT).show();
							else
							{
								if (!(pgnOffset == 0 & fPgn.length() == 0))
									Toast.makeText(context, getString(R.string.game_saved), Toast.LENGTH_SHORT).show();
							}
						}
						startQueryTask(pgnPath, pgnFile, false, true);
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
//Log.i(TAG, "insertValuesToDb()");
//			Long pgnId = 0L;
			Long pgnId;
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
//Log.i(TAG, "insertValuesToDb(), pgnId: " + pgnId);
			if (pgnId >= 0)
				GameId = pgnId +1;
			return true;
	    }

        public SQLiteStatement getSqlInsert()
        {
            String sql = "INSERT INTO " + TABLE_NAME + "("
                    + "GameFileOffset,GameLength,GameMovesOffset,Event,Site,"
                    + "Date,Round,White,Black,Result,"
                    + "SetUp,FEN,WhiteTitle,BlackTitle,WhiteElo,"
                    + "BlackElo,ECO,Opening,Variation,WhiteTeam,"
                    + "BlackTeam,WhiteFideId,BlackFideId,EventDate,EventType"
                    +") values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
//			SQLiteStatement stmt = db.compileStatement(sql);
            return db.compileStatement(sql);
        }

        public boolean insertValuesToDb(SQLiteStatement stmt)
        {
            rafParsed = rafParsed +GameLength;
//            Long pgnId = 0L;
            try
            {
//                SQLiteStatement stmt = db.compileStatement(sqlInsert);

                stmt.bindLong(1, GameFileOffset);
                stmt.bindLong(2, GameLength);
                stmt.bindLong(3, GameMovesOffset);
                stmt.bindString(4, Event);
                stmt.bindString(5, Site);
                stmt.bindString(6, Date);
                stmt.bindString(7, Round);
                stmt.bindString(8, White);
                stmt.bindString(9, Black);
                stmt.bindString(10, Result);
                stmt.bindString(11, SetUp);
                stmt.bindString(12, FEN);
                stmt.bindString(13, WhiteTitle);
                stmt.bindString(14, BlackTitle);
                stmt.bindString(15, WhiteElo);
                stmt.bindString(16, BlackElo);
                stmt.bindString(17, ECO);
                stmt.bindString(18, Opening);
                stmt.bindString(19, Variation);
                stmt.bindString(20, WhiteTeam);
                stmt.bindString(21, BlackTeam);
                stmt.bindString(22, WhiteFideId);
                stmt.bindString(23, BlackFideId);
                stmt.bindString(24, EventDate);
                stmt.bindString(25, EventType);

//                pgnId = stmt.executeInsert();
                stmt.execute();
//                if (pgnId >= 0)
//                    GameId = pgnId +1;
//                stmt.clearBindings();
                GameId++;

            }
            catch (Exception  e) 	{e.printStackTrace(); GameId = 0; return false;}

            return true;
        }

		public void stopTransaction()
		{
			if (db.inTransaction())
			{
				db.setTransactionSuccessful();
				db.endTransaction();
			}
		}

		public Long getPercentageComplete()
	    {
			long percentage = 0;
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
		boolean isStartTask = true;
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
//Log.i(TAG, "QueryTask, path/file: " + gamePath + gameFile + ", setInfo: " + setInfo + ", isNewFile: " + isNewFile);
			if (pgnDb.openDb(gamePath, gameFile, SQLiteDatabase.OPEN_READONLY))
			{
				int rowCnt = pgnDb.getRowCount(PgnDb.TABLE_NAME);

//Log.i(TAG, "QueryTask, save_selected_scroll_game_id: " + save_selected_scroll_game_id + ", save_scroll_game_id: " + save_scroll_game_id);
				if (isNewFile & rowCnt > 0)
				{
					if (fm_extern_db_game_desc | fileActionCode == 2)
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
				if (fileActionCode == 2)
				{
					if (save_scroll_game_id < 0)
						scroll_game_id = save_selected_scroll_game_id;
					else
						scroll_game_id = save_scroll_game_id;
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
				try
				{
					removeDialog(QUERY_PROGRESS_DIALOG);
		    		showDialog(QUERY_PROGRESS_DIALOG);
				}
//	    		catch (BadTokenException e) { Log.i(TAG, "2 QueryTask.onPreExecute(), BadTokenException"); }
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
					setFmInfo(getString(R.string.qPlayer) + ": " + pl);
					break;
				case SET_QUERY_DATA_EVENT:
					setFmInfo(ev_event + ", " + ev_site);
					break;
				case SET_QUERY_DATA_ECO:
					setFmInfo(ec_eco + ", " + ec_opening + ", " + ec_variation);
					break;
				case SET_QUERY_DATA_GAME:
				default:	// gameId(primary)
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

			if (fileActionCode == 2)
			{
				gameId = scroll_game_id;
				if (isNewFile)
				{
					save_selected_scroll_game_id = queryCursor.getCount();
					gameId = save_selected_scroll_game_id;
				}
				if (save_is_done & gamePath == save_path & gameFile == save_file)
				{
					save_selected_scroll_game_id = save_scroll_game_id;
					gameId = save_selected_scroll_game_id;
				}
				else
				{
					save_path = gamePath;
					save_file = gameFile;
				}
				save_is_done = false;
			}

			setQueryDataToTitle(fm_extern_db_key_id, gameId, pgnDb.getRowCount(PgnDb.TABLE_NAME), scroll_game_id, queryCount);

			if (setInfo)
			{
//				pgnDb.openDb(gamePath, gameFile, SQLiteDatabase.OPEN_READONLY);
				if (pgnDb.openDb(gamePath, gameFile, SQLiteDatabase.OPEN_READONLY))
				{
					setInfoValues("DB-Test - startQueryTask()", "cursor_cnt: " + queryCursor.getCount(), pgnDb.getDbVersion());
					pgnDb.closeDb();
				}
			}

			String[] columns = new String[]	{PgnDb.PGN_ID, PgnDb.PGN_WHITE, PgnDb.PGN_BLACK, PgnDb.PGN_EVENT, PgnDb.PGN_DATE, PgnDb.PGN_RESULT};
			int[] to = new int[] {R.id.text1, R.id.text2, R.id.text3, R.id.text4, R.id.text5, R.id.text6};
			pgnAdapter = new SimpleCursorAdapter(FileManager.this, R.layout.dbquery, queryCursor, columns, to, 0);
			pgnAdapter.notifyDataSetChanged();
			lvGames.setVisibility(ListView.VISIBLE);
			lvGames.setAdapter(pgnAdapter);
			lvGames.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

			int scrollId = scroll_game_id -1;
			lvGames.setItemChecked(scrollId, true);
			lvGames.setSelection(scrollId);
			int h1 = lvGames.getHeight();
			int h2 = 100;
			lvGames.setSelectionFromTop(scrollId, h1/2 - h2/2);
//Log.i(TAG, "queryCursor, db_key_id: " + fm_extern_db_key_id + ", scrollId: " + scrollId + ", save_selected_scroll_game_id: " + save_selected_scroll_game_id);

			if (queryCursor.getCount() == 0)
			{
				isQueryResult = false;
				emptyLv.setVisibility(TextView.VISIBLE);
			}
			else
				emptyLv.setVisibility(TextView.INVISIBLE);
			if (fm_extern_db_key_id != 0 & queryCursor.getCount() > 0)
			{
			   fm_extern_db_cursor_id = 1;
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
//Log.i(TAG, "lvGames.onItemLongClick(), lvPosition: " + position + ", gameId: " + gameId);
						if (fileActionCode == 2)
						{
							if (pgnDb.openDb(gamePath, gameFile, SQLiteDatabase.OPEN_READONLY))
							{
								save_selected_scroll_game_id = position +1;
								save_path = gamePath;
								save_file = gameFile;
								save_is_done = false;
								pgnDb.getFieldsFromGameId(gameId +1);
								long replaceEnd = Long.parseLong(pgnDb.fCur.getString(pgnDb.fCur.getColumnIndex("GameFileOffset"))) -1;
								pgnDb.getFieldsFromGameId(gameId);
								try
								{
									gameReplaceStart = Long.parseLong(pgnDb.fCur.getString(pgnDb.fCur.getColumnIndex("GameFileOffset")));
									long gameLength = Long.parseLong(pgnDb.fCur.getString(pgnDb.fCur.getColumnIndex("GameLength")));
									gameReplaceEnd = gameReplaceStart + gameLength;
									if (replaceEnd > 0 & replaceEnd > gameReplaceStart)
										gameReplaceEnd = replaceEnd;
									pgnLength = pgnDb.pgnLength;
//Log.i(TAG, "gameReplaceEnd: " + gameReplaceEnd + ", pgnDb.pgnLength: " + pgnDb.pgnLength);
								}
								catch (NumberFormatException e)	{return;}
								gameMoveStart = 0;
								pgnDb.closeDb();
								removeDialog(MENU_EDIT_PGN);
								showDialog(MENU_EDIT_PGN);
							}
							return;
						}

						if (pgnDb.openDb(gamePath, gameFile, SQLiteDatabase.OPEN_READONLY))
						{
							int queryCount = 1;
							if (fm_extern_db_key_id != 0)
							   queryCount = queryCursor.getCount();
//Log.i(TAG, "fm_extern_db_key_id, gameId: " + fm_extern_db_key_id + ", " + gameId);
							scroll_game_id = position +1;
							setQueryDataToTitle(fm_extern_db_key_id, gameId, pgnDb.getRowCount(PgnDb.TABLE_NAME), scroll_game_id, queryCount);
							pgnDb.getGameId(gameId, 10);	// set pgnStat value
							fileIO.pgnStat = pgnDb.pgnStat;
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

			lvGames.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener()
			{
				@Override
				public boolean onItemLongClick(AdapterView<?> parent, View view,  int position, long id)
				{
					// Dialog delete game
					if (isQuery | fileActionCode != 2) return true;		// delete game only in save mode (fileActionCode == 2)
					if (queryCursor.moveToPosition(position))
					{
						int gameId = queryCursor.getInt(queryCursor.getColumnIndex("_id"));
//Log.i(TAG, "lvGames.onItemLongClick(), lvPosition: " + position + ", gameId: " + gameId);
						if (pgnDb.openDb(gamePath, gameFile, SQLiteDatabase.OPEN_READONLY))
						{
							try
							{
								pgnDb.getFieldsFromGameId(gameId +1);
								long replaceEnd = Long.parseLong(pgnDb.fCur.getString(pgnDb.fCur.getColumnIndex("GameFileOffset"))) -1;
								pgnDb.getFieldsFromGameId(gameId);
								gameReplaceStart = Long.parseLong(pgnDb.fCur.getString(pgnDb.fCur.getColumnIndex("GameFileOffset")));
								long gameLength = Long.parseLong(pgnDb.fCur.getString(pgnDb.fCur.getColumnIndex("GameLength")));
								gameReplaceEnd = gameReplaceStart + gameLength;
								if (replaceEnd > 0 & replaceEnd > gameReplaceStart)
									gameReplaceEnd = replaceEnd;
							}
							catch (NumberFormatException e)	{return true;}
							save_selected_scroll_game_id = position +1;
							save_path = gamePath;
							save_file = gameFile;
							save_is_done = false;
							gameMoveStart = 0;
							gameDbId = pgnDb.fCur.getString(pgnDb.fCur.getColumnIndex("_id"));
							gamePlayerWhite = pgnDb.fCur.getString(pgnDb.fCur.getColumnIndex("White"));
							gamePlayerBlack = pgnDb.fCur.getString(pgnDb.fCur.getColumnIndex("Black"));
							pgnDb.closeDb();
							removeDialog(DELETE_GAME_DIALOG);
							showDialog(DELETE_GAME_DIALOG);
							return true;
						}
					}
					return true;
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
		if (!pgnDb.initPgnFiles(path, file))
		{
			startCreateDatabaseTask(path, file, "0", "");
			pgnDbNotExists = true;
			fileData = "";
		}
		else
		{
			if (pgnDb.openDb(path, file, SQLiteDatabase.OPEN_READONLY))
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
						startCreateDatabaseTask(path, file, "0", "");
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

	public int getDbFileState(String fileName, int location)
    {
		if (!fileName.endsWith(fm_file_extension) | location != 1)
			return STATE_DB_NO_PGN_ACTION;
		if (!(userPrefs.getBoolean("user_options_gui_usePgnDatabase", true)))
			return STATE_DB_DISABLED;
		String path = etPath.getText().toString() + fileName;
		String dbPath = path.replace(".pgn", ".pgn-db");
		String dbJournalPath = path.replace(".pgn", ".pgn-db-journal");
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

//				if ((ct -lmPgnDb) <= 3000 | fPgnDbJournal.exists())
				if (fPgnDbJournal.exists())
					return STATE_DB_LOCKED;
				else
				{
					pgnDb.initPgnFiles(etPath.getText().toString(), fileName);
					if 	(pgnDb.openDb(etPath.getText().toString(), fileName, SQLiteDatabase.OPEN_READONLY))
					{
						int state = pgnDb.getStateFromLastGame();
//Log.i(TAG, "state: "  + state);
						pgnDb.closeDb();
						switch (state) 
						{
							case 1:		return STATE_DB_OK;
							case 5:		return STATE_DB_IN_TRANSACTION;
							case 6:		return STATE_DB_NO_ROWS;
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
//Log.i(TAG, "path/file: " + path + file + ", state: " + state + ", isNewFile: " + isNewFile);
		switch (state)
		{
			case STATE_DB_OK: 
				if (pgnDb.initPgnFiles(path, file))
				{
					if (fileActionCode == 2 & save_is_start)
					{
						save_is_start = false;
						if 	(pgnDb.openDb(path, file, SQLiteDatabase.OPEN_READONLY))
						{
							if (getIntent().getExtras().getBoolean("isGameLoaded")
									& fm_extern_load_path.equals(fm_extern_save_path)
									& fm_extern_load_file.equals(fm_extern_save_file))
								save_selected_scroll_game_id = fm_extern_db_game_id;
							else
								save_selected_scroll_game_id = pgnDb.getRowCount(PgnDb.TABLE_NAME);
							pgnDb.closeDb();
						}
					}
					if (showGameListView)
						startQueryTask(path, file, true, isNewFile);
					else
						showGameListView = true;
				}
				break;
			case STATE_DB_LOCKED: 
			case STATE_DB_IN_TRANSACTION:
				removeDialog(DATABASE_LOCKED_DIALOG);
				showDialog(DATABASE_LOCKED_DIALOG);
				break;
			case STATE_DB_NO_DBFILE:
				if (!pgnDb.initPgnFiles(path, file))
				{
					startCreateDatabaseTask(path, file, "0", "");
				}
				break;
			case STATE_DB_NO_ROWS:
				if (pgnDb.initPgnFiles(path, file))
				{
					pgnDb.deleteDbFile();
					handleDb(path, file, STATE_DB_NO_DBFILE, false);
				}
				break;
			case STATE_DB_WRONG_VERSION:
				Toast.makeText(this, getString(R.string.fmDropCreateIndex), Toast.LENGTH_LONG).show();
				startCreateDatabaseTask(path, file, "0", "idx");
				break;
			case STATE_DB_UNCOMPLETED:
				pgnDb.initPgnFiles(path, file);
				if (pgnDb.openDb(path, file, SQLiteDatabase.OPEN_READONLY))
				{
					switch (pgnDb.getStateFromLastGame()) 
					{
						case 1:	// no data after last game, unlock .pgn-db(set lastModified from .pgn)
							pgnDb.closeDb();
							handleDb(path, file, STATE_DB_OK, false);
							break;
						case 2:	// games after last game(db), complite db
							pgnDb.closeDb();
							startCreateDatabaseTask(path, file, Long.toString(pgnDb.pgnRafOffset), "dropidx");
							break;
						case 0:	// delete .pgn-db and start new create db(STATE_DB_NO_DBFILE)
							pgnDb.closeDb();
							pgnDb.deleteDbFile();
							handleDb(path, file, STATE_DB_NO_DBFILE, false);
							break;
					}
				}
				else
				{
					pgnDb.deleteDbFile();
					handleDb(path, file, STATE_DB_NO_DBFILE, false);
				}
				break;
			default: 
				break;
		}
	}

	private void startEditPgnTask(String path, String file, String gameData, String actionId,
								  	String replaceStart, String replaceEnd, String moveStart)
	{
		if (!fileIO.canWrite(path, file))
		{
			removeDialog(FILE_NO_WRITE_PERMISSIONS);
			showDialog(FILE_NO_WRITE_PERMISSIONS);
			return;
		}
		if (lvGames.isShown())
			lvGames.setVisibility(ListView.INVISIBLE);
        setNotificationId();
		editPgnTask = new EditPgnTask(FileManager.this, notificationId);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			editPgnTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, path, file, gameData, actionId,
									replaceStart, replaceEnd, moveStart);
		else
			editPgnTask.execute(path, file, gameData, actionId,	replaceStart, replaceEnd, moveStart);
	}

	private void startCreateDatabaseTask(String path, String file, String pgnOffset, String idxControl)
	{
		if (!fileIO.canWrite(path, file))
		{
			isCreateDb = true;
			removeDialog(FILE_NO_WRITE_PERMISSIONS);
			showDialog(FILE_NO_WRITE_PERMISSIONS);
			return;
		}
        setNotificationId();
		createDatabaseTask = new CreateDatabaseTask(this, notificationId);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			createDatabaseTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, path, file, pgnOffset, idxControl);
		else
			createDatabaseTask.execute(path, file, pgnOffset, idxControl);
	}

	private void startQueryTask(String path, String file, boolean setInfo, boolean isNewFile)
	{
//Log.i(TAG, "startQueryTask(), path/file: " + path + file + ", setInfo: " + setInfo + ", isNewFile: " + isNewFile);
		fmBtnAction.setVisibility(ImageView.VISIBLE);
		if (fileActionCode == 1)
		{
			fmBtnGames.setVisibility(ImageView.VISIBLE);
			setFmInfo(getString(R.string.fmInfoLoadGame));
		}
		else
		{
			fmBtnGames.setVisibility(ImageView.INVISIBLE);
			setFmInfo(getString(R.string.fmInfoSaveGame));
		}
		queryTask = new QueryTask();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			queryTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, path, file, Boolean.toString(setInfo), Boolean.toString(isNewFile));
		else
			queryTask.execute(path, file, Boolean.toString(setInfo), Boolean.toString(isNewFile));
		return;
	}

    public void setNotificationId()
    {
        notificationId = fmPrefs.getInt("fm_notificationId", 0);
        notificationId++;
        SharedPreferences.Editor ed = fmPrefs.edit();
        ed.putInt("fm_notificationId", notificationId);
        ed.commit();
//Log.i(TAG, "startCreateDatabaseTask(), notificationId: " + notificationId);
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

	public void setFmInfo(String info)
	{
		fmInfo.scrollTo(0,0);
		fmInfo.setText(info);
	}

    public void setTextViewColors(TextView tv, int tvColor, int tvTextColor)
    {
        initColors();
        GradientDrawable tvBackground = (GradientDrawable) tv.getBackground();
        tvBackground.setColor(cv.getColor(tvColor));
        tv.setTextColor(cv.getColor(tvTextColor));
    }
    public void initColors()
    {
        cv = new ColorValues();
        int colorId = userPrefs.getInt("colorId", 0);
        switch (colorId)
        {
            case 0:
                cv.setColors(colorId, userPrefs.getString("colors_0", ""));
                break;
            case 1:
                cv.setColors(colorId, userPrefs.getString("colors_1", ""));
                break;
            case 2:
                cv.setColors(colorId, userPrefs.getString("colors_2", ""));
                break;
            case 3:
                cv.setColors(colorId, userPrefs.getString("colors_3", ""));
                break;
            case 4:
                cv.setColors(colorId, userPrefs.getString("colors_4", ""));
                break;
        }
    }


	final String TAG = "FileManager";
    public static final String PGN_ACTION_CREATE_DB = "pgn-create-db";
    public static final String PGN_ACTION_UPDATE = "pgn-update";
	// Dialogs
	private static final int PATH_NOT_EXISTS_DIALOG = 1;
	private static final int FILE_NOT_EXISTS_DIALOG = 2;
	private static final int FILE_EXISTS_DIALOG = 3;
	private static final int FILE_NOT_ENDS_WITH_PGN_DIALOG = 4;
	private static final int FILE_NO_WRITE_PERMISSIONS = 14;
	private static final int PGN_ERROR_DIALOG = 5;
	private static final int DELETE_FILE_DIALOG = 6;
	private static final int WEB_FILE_NOT_EXISTS_DIALOG = 7;
	private static final int ADD_FOLDER_DIALOG = 8;
	private static final int ADD_FILE_DIALOG = 9;
	private static final int MENU_FILE_DIALOG = 10;
	private static final int MENU_QUERY_DIALOG = 12;
	private static final int DATABASE_LOCKED_DIALOG = 21;
	private static final int QUERY_PROGRESS_DIALOG = 22;
	private static final int MENU_GAME_DIALOG = 23;
	private static final int MENU_EDIT_PGN = 24;
	private static final int DELETE_GAME_DIALOG = 26;
	private static final int ENGINE_INSTALL_DIALOG = 80;
	private static final int COMING_SOON = 91;  // not activated
	private static final int FILE_LOAD_PROGRESS_DIALOG = 99;

	// database file state
	private static final int STATE_DB_NO_PGN_ACTION = 200;
	private static final int STATE_DB_OK = 201;
	private static final int STATE_DB_NO_DBFILE = 202;
	private static final int STATE_DB_LOCKED = 203;
	private static final int STATE_DB_UNCOMPLETED = 204;
	private static final int STATE_DB_IN_TRANSACTION = 205;
	private static final int STATE_DB_NO_ROWS = 206;
	private static final int STATE_DB_WRONG_VERSION = 208;
	private static final int STATE_DB_DISABLED = 209;

	Util u;
	Intent returnIntent;
	FileIO fileIO;
    ColorValues cv;
	PgnDb pgnDb;
	ChessEngine ce;
	int db_state = STATE_DB_NO_PGN_ACTION;
	EditPgnTask editPgnTask = null;
	CreateDatabaseTask createDatabaseTask = null;
	QueryTask queryTask = null;
    long startCreateDatabase;
	int notificationId = 0;
	SimpleCursorAdapter pgnAdapter = null;
	int fileActionCode;	// 1=pgn load, 2=pgn save, 5=pgn save(automatic play), 91=opening book, 92=engine
	int lastFileActionCode = 1;

//		SharedPreferences		SharedPreferences		SharedPreferences		SharedPreferences	
	SharedPreferences fmPrefs;
	SharedPreferences userPrefs;
	SharedPreferences runP;
	int fm_location = 2;	// 1 = external(sdCard)	2 = intern(resource/assets)	3 = WWW(Internet)
	final String PGN_EXTENSION = ".pgn";
	final String BIN_EXTENSION = ".bin";
	final String ALL_EXTENSION = "";
	String fm_file_extension = PGN_EXTENSION;
	String fm_extern_load_path = "";
	String fm_extern_load_file = "";
	String fm_extern_save_path = "";
	String fm_extern_save_file = "";
	String fm_extern_save_auto_path = "";
	String fm_extern_save_auto_file = "";
	long fm_extern_skip_bytes = 0;
	long fm_extern_game_offset = 0;
	String fm_extern_last_game = "";

	String fm_url = "";
		
	boolean isCreateDb = false;
	boolean isQuery = false;
	boolean isQueryResult = true;
	private static final int SET_QUERY_DATA_GAME 	= 0;
	private static final int SET_QUERY_DATA_PLAYER 	= 1;
	private static final int SET_QUERY_DATA_DATE 	= 2;
	private static final int SET_QUERY_DATA_EVENT	= 3;
	private static final int SET_QUERY_DATA_ECO 	= 9;

	int setQueryData = 0;

	int fm_extern_db_key_id = 0;			// 0 game, 1 player, 2 date, 3 event, 9 eco
	int fm_extern_db_game_id = 0;			// game-ID(primary key) [current game]
	int fm_extern_db_game_count = 0;		// game count
	boolean fm_extern_db_game_desc = false;	// game-ID: descending
	int fm_extern_db_game_max_items = 4000;	// max. items for viewAdapter
	int fm_extern_db_cursor_id = 0;			// 0 . . .  fm_extern_db_cursor_count -1
	int fm_extern_db_cursor_count = 0;		// cursor row counter
	String fm_extern_db_game_id_list = "";

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

	int scroll_game_id = 1;

	int save_action_id = 0;
	String save_path = "";
	String save_file = "";
	int save_selected_scroll_game_id = 1;	// selected game id 	(auto set or/and selected by user)
	int save_scroll_game_id = -1;			// saved game id		(depanding on user menu select)
	boolean save_is_start = false;
	boolean save_is_done = false;

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
	TextView title;
	TextView lblFile;
	EditText etPath;
	EditText etUrl;
	EditText etFile;

	ListView lvFiles;
	ListView lvGames;
	TextView emptyLv;

	ImageView btnMenu = null;
	ImageView fmBtnAction = null;
	ImageView btnDirBack = null;
	ImageView fmBtnGames = null;
	TextView fmInfo;
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
	C4aDialog c4aDialog;
	C4aDialog pathDialog;
	C4aDialog fileNotExistsDialog;
	C4aDialog fileExistsDialog;
	C4aDialog fileNotEndsWithPgnDialog;
	C4aDialog webFileNotExistsDialog;
	C4aDialog addFolderDialog;
	C4aDialog addFileDialog;
	C4aDialog pgnDialog;
	C4aDialog deleteDialog;
	ProgressDialog progressDialog = null;

	int activDialog = 0;
	String pgnUrl = "http://www.chessok.com/broadcast/getpgn.php?action=save&saveid=sofia2010_12.pgn";
	String fileData = "";
	String fileName = "";
	String baseDir = "";
	String defaultFolder = "";
	boolean isSkipRandom = false;
	boolean isPriviousGame = false;
	boolean isLastGame = false;
	boolean isFindGame = false;
	boolean isQueryInputError = false;

	public String gamePath = "";
	public String gameFile = "";
	public String gameData = "";
	public String gameDbId = "";
	public String gamePlayerWhite = "";
	public String gamePlayerBlack = "";
	public long gameReplaceStart = 0;
	public long gameReplaceEnd = 0;
	public long gameMoveStart = 0;
	public long pgnLength = 0;

//		DB-TEST			DB-TEST			DB-TEST			DB-TEST
	String runMessage = "";
	int dbTest;		// 0 = no transaction  1 = +transaction
    final int DB_CREATE = 0;
    final int DB_CREATE_TRANSACTION = 1;
    final int DB_CREATE_SQL_STM = 10;
    final int DB_CREATE_SQL_STM_TRANSACTION = 11;
}
