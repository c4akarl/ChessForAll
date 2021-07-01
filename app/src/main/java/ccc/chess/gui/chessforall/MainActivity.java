package ccc.chess.gui.chessforall;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.XmlResourceParser;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.InputType;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.kalab.chess.enginesupport.ChessEngine;
import com.kalab.chess.enginesupport.ChessEngineResolver;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import ccc.chess.book.ChessParseError;
import ccc.chess.book.Move;
import ccc.chess.book.Pair;
import ccc.chess.book.TextIO;
import ccc.chess.logic.c4aservice.Chess960;
import ccc.chess.logic.c4aservice.ChessPosition;
import ccc.chess.gui.chessforall.UciEngine.EngineState;

@RequiresApi(api = Build.VERSION_CODES.N)
public class MainActivity extends Activity implements Ic4aDialogCallback, OnTouchListener
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {

//		Log.i(TAG, "onCreate()");
//		Log.i(TAG, "onCreate(), savedInstanceState: " + savedInstanceState);

        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
		u = new Util();
        runP = getSharedPreferences("run", 0);		//	run Preferences
        moveHistoryP = getSharedPreferences("moves", 0);	//	moveHistory Preferences
        moveHistory = moveHistoryP.getString("run_moveHistory", "");
        userPrefs = getSharedPreferences("user", 0);	// 	user Preferences
        fmPrefs = getSharedPreferences("fm", 0);		// 	fileManager(PGN) Preferences
		initColors();
        setStringsValues();

        gc = new GameControl(stringValues, moveHistory);
		ec = new EngineControl(this);
		tc = new TimeControl();

		ec.isUciNewGame = true;
		ec.setBookOptions();
		getPermissions();
		setEngineDirectories();

		chess960 = new Chess960();	// needed for "create your own chess position"
		fileIO = new FileIO(this);

		startGui();

    }

	//	GUI		GUI		GUI		GUI		GUI		GUI		GUI		GUI		GUI		GUI
	public void startGui()
	{

//		Log.i(TAG, "startGui(), START");

		getRunPrefs();	// run preferences
		PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
		setWakeLock(false);
		wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "c4a:locktag");
		wakeLock.setReferenceCounted(false);
		useWakeLock = !userPrefs.getBoolean("user_options_gui_disableScreenTimeout", false);
		setWakeLock(useWakeLock);
		getChessFieldSize();

		u.updateFullscreenStatus(this, userPrefs.getBoolean("user_options_gui_StatusBar", false));
		aspectRatio = u.getAspectRatio(this);

		try
        {
            if (aspectRatio > 150)
                setContentView(R.layout.main);
            else
                setContentView(R.layout.main150);
        }
        catch (RuntimeException e) {e.printStackTrace(); finish(); return;}

		if (userPrefs.getInt("user", 0) == 0)
			gc.initPrefs = true;

		lblPlayerNameA = (TextView) findViewById(R.id.lblPlayerNameA);
		lblPlayerNameA.setOnTouchListener(this);
		lblPlayerEloA = (TextView) findViewById(R.id.lblPlayerEloA);
		lblPlayerEloA.setOnTouchListener(this);
		lblPlayerTimeA = (TextView) findViewById(R.id.lblPlayerTimeA);
		lblPlayerTimeA.setOnTouchListener(this);
		lblPlayerNameB = (TextView) findViewById(R.id.lblPlayerNameB);
		lblPlayerNameB.setOnTouchListener(this);
		lblPlayerEloB = (TextView) findViewById(R.id.lblPlayerEloB);
		lblPlayerEloB.setOnTouchListener(this);
		lblPlayerTimeB = (TextView) findViewById(R.id.lblPlayerTimeB);
		lblPlayerTimeB.setOnTouchListener(this);

		msgShort = (TextView) findViewById(R.id.msgShort);
		msgShort.setOnTouchListener(this);
		msgShort.setTextSize(userPrefs.getInt("user_options_gui_fontSize", Settings.FONTSIZE_MEDIUM));

		msgShort2 = (TextView) findViewById(R.id.msgShort2);
		msgShort2.setOnTouchListener(this);
		msgShort2.setTextSize(userPrefs.getInt("user_options_gui_fontSize", Settings.FONTSIZE_MEDIUM));

		scrlMsgMoves = (ScrollView) findViewById(R.id.scrlMsgMoves);
		scrlMsgMoves.setVerticalFadingEdgeEnabled(false);
		msgMoves = (TextView) findViewById(R.id.msgMoves);
		msgMoves.setOnTouchListener(this);
		msgMoves.setTextSize(userPrefs.getInt("user_options_gui_fontSize", Settings.FONTSIZE_MEDIUM));

		scrlMsgEngine = (ScrollView) findViewById(R.id.scrlMsgEngine);
		scrlMsgEngine.setVerticalFadingEdgeEnabled(false);
		msgEngine = (TextView) findViewById(R.id.msgEngine);
		msgEngine.setMaxLines(getMsgEngineLines());
		msgEngine.setLines(getMsgEngineLines());
		msgEngine.setTextSize(userPrefs.getInt("user_options_gui_fontSize", Settings.FONTSIZE_MEDIUM));

		btn_1 = (ImageView) findViewById(R.id.btn_1);
		registerForContextMenu(btn_1);
		btn_1.setOnTouchListener(this);
		btn_2 = (ImageView) findViewById(R.id.btn_2);
		registerForContextMenu(btn_2);
		btn_2.setOnTouchListener(this);
		btn_3 = (ImageView) findViewById(R.id.btn_3);
		btn_3.setOnTouchListener(this);
		btn_4 = (ImageView) findViewById(R.id.btn_4);
		btn_4.setOnTouchListener(this);
		btn_5 = (ImageView) findViewById(R.id.btn_5);
		btn_5.setOnTouchListener(this);
		btn_6 = (ImageView) findViewById(R.id.btn_6);
		btn_6.setOnTouchListener(this);
		btn_7 = (ImageView) findViewById(R.id.btn_7);
		btn_7.setOnTouchListener(this);

		boardView = (BoardView) findViewById(R.id.boardView);
        boardView.setColor();
		boardView.setOnTouchListener(this);
		boardView.updateBoardView(gc.fen, gc.isBoardTurn, BoardView.ARROWS_NONE, null, null, null, null,
				null,null, false, userPrefs.getBoolean("user_options_gui_BlindMode", false));

		initDrawers();

		starterIntent = getIntent();
		fileManagerIntent = new Intent(this, FileManager.class);
		gameDataIntent = new Intent(this, ChessGameData.class);
		notationIntent = new Intent(this, ChessNotation.class);
		moveTextIntent = new Intent(this, ChessMoveText.class);
		optionsTimeControlIntent = new Intent(this, OptionsTimeControl.class);
		optionsSettingsIntent = new Intent(this, Settings.class);
		optionsColorIntent = new Intent(this, OptionsColor.class);
		editChessBoardIntent = new Intent(this, EditChessBoard.class);
		editUciOptions = new Intent(this, EditUciOptions.class);
		computerMatch = new Intent(this, ComputerMatch.class);
		analysisOptions = new Intent(this, AnalysisOptions.class);

//		Log.i(TAG, "startGui(), END");

	}

	public void startApp()
	{

//		Log.i(TAG, "startApp(), START");

		mSoundPool = new SoundPool(2, AudioManager.STREAM_RING, 100);
		soundsMap = new HashMap<Integer, Integer>();
		soundsMap.put(1, mSoundPool.load(this, R.raw.move_sound, 1));
		soundsMap.put(2, mSoundPool.load(this, R.raw.move_wrong, 1));
		soundsMap.put(3, mSoundPool.load(this, R.raw.move_ok, 1));
		setPieceName(userPrefs.getInt("user_options_gui_PieceNameId", 0));
		displayMoves = null;
		if (gc.initPrefs & userPrefs.getBoolean("user_options_gui_StatusBar", false))
		{
			gc.initPrefs = false;
			u.updateFullscreenStatus(this, false);
		}
		getGameData(gc.fileBase, gc.filePath, gc.fileName, gc.startPgn, true, false, gc.startMoveIdx,false);
		if ((ec.chessEnginePlayMod == 3 | ec.chessEnginePlayMod == 4) & !gc.isGameOver & !gc.cl.p_variationEnd)
			ec.chessEngineSearching = true;
		gc.startFen = gc.cl.history.getStartFen();
		if (gc.cl.p_chess960ID == 518)
			gc.isChess960 = false;
		else
			gc.isChess960 = true;

		if (progressDialog != null)
		{
			if (progressDialog.isShowing())
				dismissDialog(FILE_LOAD_PROGRESS_DIALOG);
		}
		ec.chessEngineInit = true;
		// autoStart engines after loading application
		boolean isNewApp = false;
		if  (userPrefs.getInt("user", 0) == 0)
		{
			isNewApp = true;
			SharedPreferences.Editor ed = userPrefs.edit();
			ed.putInt("user", 1);
			ed.commit();
		}

//		Log.i(TAG, "startApp(), isNewApp: " + isNewApp + ", ec.chessEnginePlayMod: " + ec.chessEnginePlayMod);

		if  (	isNewApp
				|   (       userPrefs.getBoolean("user_options_enginePlay_AutoStartEngine", true)
				& 	!gc.isGameOver & !ec.chessEnginePaused
					)
			)
		{

			ec.chessEnginePaused = false;

//			Log.i(TAG, "moveIdx: " + gc.cl.p_moveIdx );
//			Log.i(TAG, "history.getStartFen(): "  + gc.cl.history.getStartFen());

			if (gc.cl.p_moveIdx == 0 & gc.cl.history.getStartFen().toString().contains("/8/8/8/8/"))	// move idx 0, new game
				stopSearchAndRestart(true, true);
			else
				stopSearchAndRestart(false, false);
		}
		else
		{
			updateCurrentPosition("");
			if (!userPrefs.getBoolean("user_options_enginePlay_AutoStartEngine", true))
			{
				ec.chessEnginePaused = true;
				setInfoMessage(getEnginePausedMessage(), null, null);
			}
			if (gc.isGameOver | gc.cl.p_variationEnd)
			{
				ec.chessEnginePaused = true;
				ec.chessEngineSearching = false;

//				Log.i(TAG, "1 getGameOverMessage()");

				setInfoMessage(getGameOverMessage(), null, null);
			}
			else
			{
				if (ec.chessEnginePaused)
					setInfoMessage(getEnginePausedMessage(), null, null);
				else
				{
					if (gc.cl.p_message.equals(""))
						setInfoMessage(getString(R.string.engine_paused), null, null);
					else
						setInfoMessage(gc.cl.p_message, null, null);
				}
			}
			ec.chessEnginePaused = true;
			ec.chessEngineSearching = false;
			if (ec.chessEnginePlayMod == 5 || ec.chessEnginePlayMod == 6)
				startEdit(false, false);
		}
		getDataFromIntent(getIntent());

//		Log.i(TAG, "startApp(), END");

// 	!!! DIALOG FOR NEW MESSAGE (do not delete)
//		if (userPrefs.getInt("user", 0) == 0)
//		{
//			SharedPreferences.Editor ed = userPrefs.edit();
//			ed.putInt("user", 1);
//			ed.commit();
//			showDialog(C4A_NEW_DIALOG);
//		}
// 	!!! DIALOG FOR NEW MESSAGE (do not delete)

	}


	// Initialize the drawer part of the user interface.
	final int MENU_EDIT_BOARD   = 1;
	final int MENU_PGN    		= 2;
	final int MENU_ENGINE      	= 3;
	final int MENU_SETTINGS    	= 4;
	final int MENU_INFO    		= 5;
	final int MENU_MANUAL  		= 6;

	final int MENU_NEW_GAME     = 10;
	final int MENU_UCI_ELO     	= 11;
	final int MENU_TIME     	= 12;
	final int MENU_GAME_MODE    = 13;
	final int MENU_GAME_RESIGN  = 14;
	final int MENU_GAME_DRAW    = 15;
	final int MENU_COMMENTS    	= 16;
	final int MENU_NAG      	= 17;
	final int MENU_GAME_DATA  	= 18;
	final int MENU_PGN_DATA    	= 19;

	private void initDrawers()
	{
		drawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
		leftDrawer = (ListView)findViewById(R.id.left_drawer);
		rightDrawer = (ListView)findViewById(R.id.right_drawer);
		final DrawerItem[] leftItems = new DrawerItem[]
		{
			new DrawerItem(MENU_EDIT_BOARD, R.string.menu_edit_board),
			new DrawerItem(MENU_PGN, R.string.fmLblFile),
			new DrawerItem(MENU_ENGINE, R.string.menu_enginesettings),
			new DrawerItem(MENU_SETTINGS, R.string.menu_usersettings),
			new DrawerItem(MENU_INFO, R.string.menu_about),
			new DrawerItem(MENU_MANUAL, R.string.menu_about_userManual),
		};
		leftDrawer.setAdapter(new ArrayAdapter<DrawerItem>(this,
				R.layout.drawer_list_item,
				leftItems));
		leftDrawer.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
									int position, long id)
			{
				DrawerItem di = leftItems[position];
				handleDrawerSelection(di.id);
			}
		});

		final DrawerItem[] rightItems = new DrawerItem[]
		{
			new DrawerItem(MENU_NEW_GAME, R.string.menu_new_game),
			new DrawerItem(MENU_UCI_ELO, R.string.setEngineStrength),
			new DrawerItem(MENU_TIME, R.string.timeClock),
			new DrawerItem(MENU_GAME_MODE, R.string.app_optionsPlay),
			new DrawerItem(MENU_GAME_RESIGN, R.string.enginePlayerResign),
			new DrawerItem(MENU_GAME_DRAW, R.string.enginePlayerDraw),
			new DrawerItem(MENU_COMMENTS, R.string.menu_info_moveNotification),
			new DrawerItem(MENU_NAG, R.string.menu_info_nag),
			new DrawerItem(MENU_GAME_DATA, R.string.app_chessData),
			new DrawerItem(MENU_PGN_DATA, R.string.menu_info_moveNotation),
		};
		rightDrawer.setAdapter(new ArrayAdapter<DrawerItem>(this,
				R.layout.drawer_list_item,
				rightItems));
		rightDrawer.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
									int position, long id) {
				DrawerItem di = rightItems[position];
				handleDrawerSelection(di.id);
			}
		});
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		drawerLayout.openDrawer(Gravity.LEFT);
		return false;
	}

	@Override
	public void onBackPressed()
	{
		if (leftDrawer.isShown())
		{
			drawerLayout.closeDrawer(Gravity.LEFT);
			return;
		}
		if (rightDrawer.isShown())
		{
			drawerLayout.closeDrawer(Gravity.RIGHT);
			return;
		}

		super.onBackPressed();
	}

	// React to a selection in the left/right drawers.
	private void handleDrawerSelection(int itemId)
	{
		drawerLayout.closeDrawer(Gravity.LEFT);
		drawerLayout.closeDrawer(Gravity.RIGHT);
		leftDrawer.clearChoices();
		rightDrawer.clearChoices();

		if (ec.chessEngineMatch) {
			c4aShowDialog(COMPUTER_MATCH_DIALOG);
			return;
		}

		switch (itemId)
		{
			// leftDrawer
			case MENU_EDIT_BOARD:
				startEditBoard(gc.fen, false);
				break;
			case MENU_PGN:
				removeDialog(MENU_PGN_DIALOG);
				showDialog(MENU_PGN_DIALOG);
				break;
			case MENU_ENGINE:
				removeDialog(MENU_ENGINES_DIALOG);
				showDialog(MENU_ENGINES_DIALOG);
				break;
			case MENU_SETTINGS:
				stopComputerThinking(false, false);
				startActivityForResult(optionsSettingsIntent, OPTIONS_SETTINGS);
				break;
			case MENU_INFO:
				removeDialog(MENU_ABOUT_DIALOG);
				showDialog(MENU_ABOUT_DIALOG);
				break;
			case MENU_MANUAL:
				showHtml(R.raw.manual, R.string.menu_about_userManual);
				break;

			// rightDrawer
			case MENU_NEW_GAME:
				if (ec.chessEnginePlayMod != 4 && ec.getEngine() != null)
				{
					gc.isGameLoaded = false;
					ec.chessEnginePaused = false;
					ec.chessEngineInit = false;
					initChessClock(tc, userPrefs.getInt("user_options_timeControl", 1), ec.chessEnginePlayMod);
					messageInfo 		= "";
					messageEngine = new SpannableStringBuilder("");
					messageEngineShort  = "";
					if (ec.chessEnginePlayMod == 5)
						startEdit(true, true);
					else {
						if (ec.getEngine().engineSearching())
							stopSearchAndContinue(EngineState.STOP_NEW_GAME, "", true);
						else
							startPlay(true, true);
					}
				}
				else
				{
					removeDialog(PLAY_DIALOG);
					showDialog(PLAY_DIALOG);
				}
				break;
			case MENU_UCI_ELO:
				removeDialog(UCI_ELO_DIALOG);
				showDialog(UCI_ELO_DIALOG);
				break;
			case MENU_TIME:
				stopSearchAndContinue(EngineState.STOP_IDLE, "", true);
				startActivityForResult(optionsTimeControlIntent, OPTIONS_TIME_CONTROL_REQUEST_CODE);
				break;
			case MENU_GAME_MODE:
				removeDialog(PLAY_DIALOG);
				showDialog(PLAY_DIALOG);
				break;
			case MENU_GAME_RESIGN:
				CharSequence result = "";
				if (ec.chessEnginePlayMod == 1)
					result = "0-1";
				else
					result = "1-0";
				setTagResult(result, "");
				break;
			case MENU_GAME_DRAW:
				if (ec.getEngine() != null) {
					double scoreDouble;
					try{scoreDouble =	Double.parseDouble(getDisplayScore(ec.getEngine().statPvBestScore, gc.fen).toString());}
					catch(NumberFormatException e){	scoreDouble = 0;}
					if (ec.chessEnginePlayMod == 1 | ec.chessEnginePlayMod == 2)
					{
						if ((ec.chessEnginePlayMod == 1 & scoreDouble >= 4) | (ec.chessEnginePlayMod == 2 & scoreDouble <= -4))
							setTagResult("1/2-1/2", "");
						else
						{
							setInfoMessage(getString(R.string.engineDeclinesDraw), null, null);
							Toast.makeText(this, getString(R.string.engineDeclinesDraw), Toast.LENGTH_LONG).show();
						}
					}
				}
				break;
			case MENU_COMMENTS:
				startMoveText();
				break;
			case MENU_NAG:
				c4aShowDialog(NAG_DIALOG);
				break;
			case MENU_GAME_DATA:
				startGameData();
				break;
			case MENU_PGN_DATA:
				startNotation(3);
				break;
		}
	}

	private class DrawerItem
	{
		int id;
		int itemId; // Item string resource id
		DrawerItem(int id, int itemId)
		{
			this.id = id;
			this.itemId = itemId;
		}
		@Override
		public String toString() {
			return getString(itemId);
		}
	}

    @Override
    protected void onDestroy()
    {

//		Log.i(TAG, "onDestroy(), isAppEnd: " + isAppEnd);

		if (!isAppEnd) {
			isAppEnd = true;
			stopSearchAndContinue(EngineState.STOP_QUIT, "", true);
			setRunMoveHistory();
			setRunPrefs();
			wakeLock.release();
		}

    	super.onDestroy();

     }

    @Override
    protected void onResume()
    {
		super.onResume();

//		Log.i(TAG, "onResume(): " + ec.chessEnginePaused + ", isAppStart: " + isAppStart + ", gc.isAutoPlay: " + gc.isAutoPlay);

		if (isAppStart)
			startApp();

		u.updateFullscreenStatus(this, userPrefs.getBoolean("user_options_gui_StatusBar", false));
    	if (!isAppStart & gc.isAutoPlay)
				handlerAutoPlay.postDelayed(mUpdateAutoplay, 100);
    	setWakeLock(useWakeLock);
    	if (progressDialog != null)
    	{
	    	if (progressDialog.isShowing())
	    		dismissDialog(FILE_LOAD_PROGRESS_DIALOG);
    	}

    }

	@Override
	protected void onPause()
	{

//		Log.i(TAG, "onPause()(), isAppStart: " + isAppStart);

		super.onPause();
		if (!isAppStart)
        	u.setTextViewColors(lblPlayerNameA, cv, cv.COLOR_DATA_BACKGROUND_16, cv.COLOR_DATA_TEXT_17);
		isAppStart = false;
	}

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);

//		Log.i(TAG, "onNewIntent(), start getDataFromIntent(intent): ");

        getDataFromIntent(intent);
    }

	public void getDataFromIntent(Intent intent)
	{

//		Log.i(TAG, "getDataFromIntent(), intentType: " + intent.getType());

		// call from another Activity, passing the FEN(String)
		if (intent.getType() != null)
		{
			if (intent.getType().endsWith("x-chess-fenMes"))
			{
				if (intent.getStringExtra("fenMes") != null & !intent.getStringExtra("fenMes").equals(""))
				{
					startEditBoard(intent.getStringExtra("fenMes"), false);

					return;

				}
			}
		}

		if (intent.getType() != null)
		{
			if (intent.getType().equals(FileManager.PGN_ACTION_CREATE_DB))
			{
			    int id = intent.getIntExtra("notificationId", 1);
				String ns = Context.NOTIFICATION_SERVICE;
				NotificationManager nMgr = (NotificationManager) getApplicationContext().getSystemService(ns);
				nMgr.cancel(id);

				return;

			}
		}

		// call from another Activity, passing the PGN(File)
		if (intent.getData() != null)
		{
			if (intent.getType() == null)
			{
				Toast.makeText(this, "This MIME type is not supported.", Toast.LENGTH_LONG).show();

				return;

			}
			String pgnPath = intent.getDataString();
			Uri uri = intent.getData();

			pgnPath = fileIO.getExternalStoragePgnPathFromContent(pgnPath);

//			Log.i(TAG, "getDataFromIntent(), pgnPath: " + pgnPath);

			if (pgnPath.endsWith(".pgn"))
			{
				File file = new File(pgnPath);
				if (!file.exists()) {
					String error = "\n\nload error:\n" + pgnPath;
					downloadErrorMessage = getString(R.string.menu_pgn_load) + error;
					c4aShowDialog(DOWNLOAD_ERROR_DIALOG);

					return;

				}
				String fPath = file.getParent() + "/";
				String fName = file.getName();

//				Log.i(TAG, "getDataFromIntent(), fPath: " + fPath + ", fName: " + fName);

				SharedPreferences.Editor ed = fmPrefs.edit();
				ed.putString("fm_extern_load_path", fPath);
				ed.putString("fm_extern_load_file", fName);
				ed.putString("fm_extern_load_last_path", fPath);
				ed.putString("fm_extern_load_last_file", fName);
				ed.putInt("fm_extern_db_game_id", 1);
				ed.commit();
				startFileManager(LOAD_GAME_REQUEST_CODE, 1, 0);
			}
			else {
				downloadErrorMessage = getString(R.string.menu_pgn_load) + "\n" + getString(R.string.menu_pgn_load);
				c4aShowDialog(DOWNLOAD_ERROR_DIALOG);
			}
		}
	}

	public void startEditUciOptions(String oexFileName) {

//        Log.i(TAG, "1 startEditUciOptions(), oexFileName: " + oexFileName);

		stopComputerThinking(true, false);
		stopChessClock();

		engine = new UciEngine(this, 0,null, null);
		engine.engineProcess = oexFileName;
		if (engine.startProcess()) {

//            Log.i(TAG, "2 startEditUciOptions(), engine.startProcess() OK !");

			engine.engineState = UciEngine.EngineState.READ_OPTIONS;
			engine.writeLineToProcess("uci");
			engine.processAlive = engine.readUCIOptions();
			if (engine.processAlive && !engine.uciOptions.equals("")) {

//                Log.i(TAG, "3 startEditUciOptions(), engine.startProcess(),  engine.uciOptions:\n" + engine.uciOptions);
//                Log.i(TAG, "4 startEditUciOptions(), engine.startProcess(),  engine.uciFileName: " + engine.uciFileName);
//                Log.i(TAG, "5 startEditUciOptions(), engine.startProcess(),  engine.uciEngineName: " + engine.uciEngineName);

				editUciOptions.putExtra("uciOpts", engine.uciOptions);
				editUciOptions.putExtra("uciOptsChanged", fileIO.getDataFromUciFile(fileIO.getUciExternalPath(), engine.uciFileName));
				editUciOptions.putExtra("uciEngineName", engine.uciEngineName);
				startActivityForResult(editUciOptions, EDIT_UCI_OPTIONS);
				engine.shutDown();
			}
		}
	}

	public void startEditBoard(CharSequence fen, Boolean startOptions) {
		editChessBoardIntent.putExtra("currentFen", fen);
		editChessBoardIntent.putExtra("gridViewSize", gridViewSize);
		editChessBoardIntent.putExtra("fieldSize", getChessFieldSize());
		editChessBoardIntent.putExtra("startOptions", startOptions);
		SharedPreferences.Editor ed3 = runP.edit();
		ed3.putBoolean("run_game0_is_board_turn", gc.isBoardTurn);
		ed3.apply();
		startActivityForResult(editChessBoardIntent, EDIT_CHESSBOARD_REQUEST_CODE);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
	{
		switch (requestCode)
		{
			case PERMISSIONS_REQUEST_CODE:
				if (grantResults.length > 0)
				{
					for (int i = 0; i < grantResults.length; i++)
					{

						if (grantResults[i] != PackageManager.PERMISSION_GRANTED)
							Log.i(TAG, permissions[i] + " denied");

						if (permissions[i].equals(Manifest.permission.WRITE_EXTERNAL_STORAGE) && grantResults[i] == PackageManager.PERMISSION_GRANTED)
							storagePermission = PermissionState.GRANTED;
						if (permissions[i].equals(Manifest.permission.INTERNET) && grantResults[i] == PackageManager.PERMISSION_GRANTED)
							internetPermission = PermissionState.GRANTED;
						if (permissions[i].equals(Manifest.permission.WAKE_LOCK) && grantResults[i] == PackageManager.PERMISSION_GRANTED)
							wakeLockPermission = PermissionState.GRANTED;
					}
				}
		}
	}

//	SUBACTIVITIES
	public void startPgnDownload()
	{
		String url = "http://c4akarl.blogspot.co.at/p/pgn-download.html";	// PGN download from "Karl's Blog" (MediaFire file links)
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(url));
		startActivity(i);
	}

	public void startMoveText()
	{
		moveTextIntent.putExtra("move_text", gc.cl.p_moveText);
		startActivityForResult(moveTextIntent, MOVETEXT_REQUEST_CODE);
	}

	public void startFileManager(int fileActionCode, int displayActivity, int gameLoad)
	{

		if (fileIO.isSdk30() && !fileActions) {
			removeDialog(NO_FILE_ACTIONS_DIALOG);
			showDialog(NO_FILE_ACTIONS_DIALOG);
			return;
		}

//		Log.i(TAG, "fileActionCode,: "	+ fileActionCode + ", displayActivity: " + displayActivity + ", gameLoad: " + gameLoad);

		if 	((fileActionCode == LOAD_GAME_REQUEST_CODE | fileActionCode == LOAD_GAME_PREVIOUS_CODE)
				& displayActivity == 0
			)
		{
			if (fmPrefs.getString("fm_extern_load_path", "").equals(""))
			{
				startFileManager(LOAD_GAME_REQUEST_CODE, 1, 0);
				return;
			}
			removeDialog(FILE_LOAD_PROGRESS_DIALOG);
			showDialog(FILE_LOAD_PROGRESS_DIALOG);
		}
		fileManagerIntent.putExtra("fileActionCode", fileActionCode);
		fileManagerIntent.putExtra("displayActivity", displayActivity);
		fileManagerIntent.putExtra("gameLoad", gameLoad);
		fileManagerIntent.putExtra("findProblemId", "");
		fileManagerIntent.putExtra("queryControl", queryControl);
		fileManagerIntent.putExtra("isGameLoaded", gc.isGameLoaded);
		if (fileActionCode == LOAD_INTERN_ENGINE_REQUEST_CODE)
			fileManagerIntent.putExtra("fileName", internEngineName);
		if (fileActionCode == LOAD_GAME_REQUEST_CODE & displayActivity == 1)
		{
			msgShort2.setText("");
			gc.isGameLoaded = false;
		}
		if (queryControl.equals("i"))
			queryControl = "";
		if (fileActionCode == 2 | fileActionCode == 7 | fileActionCode == 71 | fileActionCode == 72)
			fileManagerIntent.putExtra("pgnData", gc.cl.history.createPgnFromHistory(1));

		if (displayActivity == 1)
		{
			if (gameLoad == 1)
			{	// query for player games
				removeDialog(FILE_LOAD_PROGRESS_DIALOG);
				showDialog(FILE_LOAD_PROGRESS_DIALOG);
			}
			startActivityForResult(fileManagerIntent, fileActionCode);		// start FileManager - Activity(with GUI)
		}
		else
		{
			switch (fileActionCode) 										// Load | Save | Delete
			{
				case SAVE_GAME_REQUEST_CODE:
					startSaveFile(gc.cl.history.createPgnFromHistory(1));		// saveFile(using class: FileIO)
					break;
				case SAVE_LOAD_GAME_REQUEST_CODE: 								// Save(old game), Load(new game)
				case SAVE_OK_LOAD_GAME_REQUEST_CODE: 							// Save(old game, MateAnalysis OK), Load(new game)
				case SAVE_ERROR_LOAD_GAME_REQUEST_CODE: 						// Save(old game, MateAnalysis ERROR), Load(new game)
				case LOAD_GAME_REQUEST_CODE: 									// Load
				default:
					startActivityForResult(fileManagerIntent, fileActionCode);	// start FileManager
					break;
			}
		}
	}

	public void startSaveFile(CharSequence data)
	{
		String path = "";
		String file = "";
		path = userPrefs.getString("user_play_eve_path", "");
		file = userPrefs.getString("user_play_eve_file", "");
		fileIO = new FileIO(this);
		if (!fileIO.canWrite(path, file))
			return;
		if (fileIO.pathExists(path))
		{
			if (!file.equals(""))
			{
				if (fileIO.fileExists(path, file))
					fileIO.dataToFile(path, file, data.toString(), true);
				else
					fileIO.dataToFile(path, file, data.toString(), false);
			}
		}
		startEngineMatch();
	}

	public void startGameData()
	{
		gameDataIntent.putExtra("gameStat", "1");
		gameDataIntent.putExtra("gameTags", gc.cl.history.gameTags);
		startActivityForResult(gameDataIntent, GAME_DATA_REQUEST_CODE);
	}

	public void startNotation(int textValue)
	{
		notationIntent.putExtra("textValue", textValue);
        int pieceId = userPrefs.getInt("user_options_gui_PieceNameId", 0);
		notationIntent.putExtra("moves", gc.cl.history.createGameNotationFromHistory(gc.cl.history.MOVE_HISTORY_MAX_50000,
				false, true, false, false, true, 0, pieceId));
		notationIntent.putExtra("moves_text", gc.cl.history.createGameNotationFromHistory(gc.cl.history.MOVE_HISTORY_MAX_50000, true, true,
				 false, false, true, 2, pieceId));
		notationIntent.putExtra("pgn", gc.cl.history.createPgnFromHistory(1));
		notationIntent.putExtra("white", gc.cl.history.getGameTagValue("White"));
		gc.cl.history.getGameTagValue("FEN");
		notationIntent.putExtra("black", gc.cl.history.getGameTagValue("Black"));
		startActivityForResult(notationIntent, NOTATION_REQUEST_CODE);
	}

	public void setMatchValues()	// match (engine vs engine)
	{
		gc.cl.history.setGameTag("MatchId", Integer.toString(userPrefs.getInt("user_play_eve_matchId", 1)));
		gc.cl.history.setGameTag("MatchEngine1", userPrefs.getString("user_play_eve_white", ""));
		gc.cl.history.setGameTag("MatchEngine2", userPrefs.getString("user_play_eve_black", ""));

//		Log.i(TAG, "setMatchValues(), result old: " + userPrefs.getString("user_play_eve_result", "0-0") + ", result new: " + gc.cl.history.getGameTagValue("Result"));

		String currentResult = userPrefs.getString("user_play_eve_result", "0-0");
		currentResult = currentResult.replace("1/2", ".5");
		currentResult = currentResult.replace("\u00BD", ".5");
		currentResult = currentResult.replace("Â", "");
		String[] currentSplit = currentResult.split("-");
		String newResult = gc.cl.history.getGameTagValue("Result");
		if (newResult.equals("*"))
			newResult = "1/2-1/2";
		newResult = newResult.replace("1/2", ".5");
		String[] newSplit = newResult.split("-");
		double wNewResult = Double.parseDouble(newSplit[0]);
		double bNewResult = Double.parseDouble(newSplit[1]);
		int round = userPrefs.getInt("user_play_eve_round", 0);
		if (currentSplit.length == 2) {
			double wResult = Double.parseDouble(currentSplit[0]);
			double bResult = Double.parseDouble(currentSplit[1]);
			if((round%2) == 0 && userPrefs.getBoolean("user_play_eve_autoFlipColor", true)) {
				wResult = wResult + bNewResult;
				bResult = bResult + wNewResult;
			}
			else
			{
				wResult = wResult + wNewResult;
				bResult = bResult + bNewResult;
			}
			String wResultString = Double.toString(wResult);
			String bResultString = Double.toString(bResult);
			if (wResultString.equals("0.5"))
				wResultString = wResultString.replace("0.5", "\u00BD");
			if (bResultString.equals("0.5"))
				bResultString = bResultString.replace("0.5", "\u00BD");
			String newMatchResult = wResultString + "-" + bResultString;
			newMatchResult = newMatchResult.replace(".5", "\u00BD");
			newMatchResult = newMatchResult.replace(".0", "");
			newMatchResult = newMatchResult.replace("Â", "");

//			Log.i(TAG, "setMatchValues(), newMatchResult: " + newMatchResult + ", gc.isBoardTurn: " + gc.isBoardTurn);

			gc.cl.history.setGameTag("MatchResult", newMatchResult);
			SharedPreferences.Editor ed = userPrefs.edit();
			ed.putString("user_play_eve_result", newMatchResult);
			ed.apply();
		}
		else
			gc.cl.history.setGameTag("MatchResult", userPrefs.getString("user_play_eve_result", "0-0"));
	}

	public void setMatchResultToElo()
	{
		String matchResult = userPrefs.getString("user_play_eve_result", "0-0");
		String[] newSplit = matchResult.split("-");
		if (newSplit.length == 2) {
			String w = newSplit[0];
			String b = newSplit[1];
			int round = userPrefs.getInt("user_play_eve_round", 0);
			if((round%2) == 0 && userPrefs.getBoolean("user_play_eve_autoFlipColor", true)) {
				if (!gc.isBoardTurn) {
					lblPlayerEloA.setText(w);
					lblPlayerEloB.setText(b);
				}
				else
				{
					lblPlayerEloA.setText(b);
					lblPlayerEloB.setText(w);
				}
			}
			else
			{
				if (!gc.isBoardTurn) {
					lblPlayerEloA.setText(b);
					lblPlayerEloB.setText(w);
				}
				else
				{
					lblPlayerEloA.setText(w);
					lblPlayerEloB.setText(b);
				}
			}
		}
	}

//	DIALOG		DIALOG		DIALOG		DIALOG		DIALOG		DIALOG		DIALOG
	public class OnPromotionListener implements ChessPromotion.MyDialogListener
	{
		@Override
		public void onOkClick(int promValue)
		{promotionOnOkClick(promValue);}
	}

	public void promotionOnOkClick(int promValue)
	{
		switch (promValue)					// set promotion(q|r|b|n)
		{
			case 1:     {gc.promotionMove = gc.promotionMove + "Q"; break;}
			case 2:     {gc.promotionMove = gc.promotionMove + "R"; break;}
			case 3:     {gc.promotionMove = gc.promotionMove + "B"; break;}
			case 4:     {gc.promotionMove = gc.promotionMove + "N"; break;}
			default:    {gc.promotionMove = gc.promotionMove + "Q"; break;}
		}
		gc.cl.newPositionFromMove(gc.fen, gc.promotionMove, true);
		gc.promotionMove = "";
		if (gc.cl.p_stat.equals("1"))
		{
			if (!ec.chessEnginePaused)
			{
				ec.chessEngineSearching = true;
				ec.chessEnginePaused = false;
				updateGui();
				if (!gc.cl.p_fen.equals(""))
					stopSearchAndContinue(EngineState.STOP_CONTINUE, gc.cl.p_fen, true);
			}
			else
				updateGui();
			gc.move = "";
		}
		else
		{
			gc.cl.p_fen = "";
			gc.cl.p_color = "";
			updateGui();
		}
	}

	private static boolean reservedEngineName(String name) {
		return name.endsWith(".ini");
	}

	private boolean storageAvailable() {
		return storagePermission == PermissionState.GRANTED;
	}

	@SuppressLint("SetTextI18n")
	@RequiresApi(api = Build.VERSION_CODES.O)
	@Override
    protected Dialog onCreateDialog(int id)
	{
		activDialog = id;
		if (id == C4A_DIALOG)
		{
			c4aDialog = new C4aDialog(this, this, getString(R.string.dgTitleDialog),
					"", getString(R.string.btn_Ok), "", msgC4aDialog, 0, "");
			return c4aDialog;
		}
		if (id == C4A_NEW_DIALOG)
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Update ChessForAll 2.0");
			String file = "file:///android_res/raw/" + "update_info";
			WebView wv = new WebView(this);
			wv.loadUrl(file);
			builder.setView(wv);
			builder.setNegativeButton(R.string.menu_about_userManual, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int whichButton)
				{
					showHtml(R.raw.manual, R.string.menu_about_userManual);
				}
			});
			builder.setPositiveButton(R.string.btn_Continue, null);
			AlertDialog alert = builder.create();
			alert.setCancelable(true);
			return alert;
		}
		if (id == NO_FILE_ACTIONS_DIALOG)
		{
			c4aDialog = new C4aDialog(this, this, getString(R.string.dgTitleDialog),
					"", getString(R.string.btn_Ok), "", getString(R.string.noFileActions), 0, "");
			return c4aDialog;
		}
		if (id == DOWNLOAD_ERROR_DIALOG)
		{
			c4aDialog = new C4aDialog(this, this, getString(R.string.dgTitleDialog),
					"", getString(R.string.btn_Ok), "", downloadErrorMessage, 0, "");
			return c4aDialog;
		}
		if (id == PGN_ERROR_DIALOG)
		{
			String message = gc.errorMessage + "\n\n" + getString(R.string.sendEmail);
			c4aDialog = new C4aDialog(this, this, getString(R.string.dgTitleDialog),
					getString(R.string.btn_Ok), "", getString(R.string.btn_Cancel), message, 0, "");
			return c4aDialog;
		}
		if (id == COMPUTER_MATCH_DIALOG)
		{
			c4aDialog = new C4aDialog(this, this, getString(R.string.app_computerMatch),
					getString(R.string.btn_Cancel), getString(R.string.finishGame), getString(R.string.btn_Continue), getString(R.string.endOrContinueMatch), 0, "");
			return c4aDialog;
		}
		if (id == FILE_LOAD_PROGRESS_DIALOG)
		{
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage(getString(R.string.fmProgressDialog));
			progressDialog.setCancelable(true);
			progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener()
			{
				public void onCancel(DialogInterface dialog) { finish(); }
			});
			return progressDialog;
		}
		if (id == VARIATION_DIALOG)
		{
			int listSize = gc.variationsList.size();
			listSize++;
			CharSequence[] items = new CharSequence[listSize];
			if (gc.variationsList.size() > 0)
			{
				for (int i = 0; i < gc.variationsList.size(); i++)
				{
					items[i] = gc.chessMove.getVal(gc.variationsList.get(i), 5);
				}
				items[gc.variationsList.size()] = getString(R.string.endOfVariation);
			}
			else
				return null;
			AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
			builder.setCancelable(true);
			builder.setItems(items, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int item)
				{
					if (item < gc.variationsList.size())
						showNewVariation(item);
					else
					{
						nextMove(4, 0);
						if (!ec.chessEnginePaused)
						{
							if (!gc.cl.p_fen.equals(""))
								stopSearchAndContinue(EngineState.STOP_CONTINUE, gc.cl.p_fen, true);
						}
					}
				}
			});
			AlertDialog alert = builder.create();
			return alert;
		}
		if (id == NAG_DIALOG)
		{
			final CharSequence[] items = new CharSequence[]
					{		getString(R.string.nag_0),
							getString(R.string.nag_10),
							getString(R.string.nag_14),
							getString(R.string.nag_16),
							getString(R.string.nag_18),
							getString(R.string.nag_15),
							getString(R.string.nag_17),
							getString(R.string.nag_19),
							getString(R.string.nag_1),
							getString(R.string.nag_2),
							getString(R.string.nag_3),
							getString(R.string.nag_4),
							getString(R.string.nag_5),
							getString(R.string.nag_6)
					};
			AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
			builder.setCancelable(true);
			builder.setItems(items, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int item)
				{
					if (item == 0) setNagToMoveText("$0");
					if (item == 1) setNagToMoveText("$10");
					if (item == 2) setNagToMoveText("$14");
					if (item == 3) setNagToMoveText("$16");
					if (item == 4) setNagToMoveText("$18");
					if (item == 5) setNagToMoveText("$15");
					if (item == 6) setNagToMoveText("$17");
					if (item == 7) setNagToMoveText("$19");
					if (item == 8) setNagToMoveText("$1");
					if (item == 9) setNagToMoveText("$2");
					if (item == 10) setNagToMoveText("$3");
					if (item == 11) setNagToMoveText("$4");
					if (item == 12) setNagToMoveText("$5");
					if (item == 13) setNagToMoveText("$6");
				}
			});
			AlertDialog alert = builder.create();
			return alert;
		}
		if (id == QUERY_DIALOG)
		{
			String white = fmPrefs.getString("fm_query_white", "");
			String black = fmPrefs.getString("fm_query_black", "");
			String event = fmPrefs.getString("fm_query_event", "") + "\n" + fmPrefs.getString("fm_query_site", "");
			String opening = "";
			if (!fmPrefs.getString("fm_query_eco", "").equals(""))
			{
				opening = fmPrefs.getString("fm_query_eco", "");
				if (!fmPrefs.getString("fm_query_opening", "").equals(""))
					opening = opening + " - " + fmPrefs.getString("fm_query_opening", "");
				if (!fmPrefs.getString("fm_query_variation", "").equals(""))
					opening = opening + "\n" + fmPrefs.getString("fm_query_variation", "");
			}
			final String openingF = opening;
			String gameId = getString(R.string.qgGameId);
			int listSize = 4;
			if (!opening.equals(""))
				listSize = 5;
			CharSequence[] items = new CharSequence[listSize];
			items[0] = white;
			items[1] = black;
			items[2] = event;
			if (!openingF.equals(""))
			{
				items[3] = openingF;
				items[4] = gameId;
			}
			else
				items[3] = gameId;
			AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
			builder.setCancelable(true);
			builder.setTitle(getString(R.string.qDatabaseQuery));
			builder.setItems(items, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int item)
				{
					switch (item)
					{
						case 0: queryControl = "w"; break;
						case 1: queryControl = "b"; break;
						case 2: queryControl = "e"; break;
						case 3:
							if (!openingF.equals(""))
								queryControl = "o";
							else
							{
								queryControl = "i";
								showDialog(GAME_ID_DIALOG);
							}
							break;
						case 4:
							queryControl = "i";
							showDialog(GAME_ID_DIALOG);
							break;
					}
					if (!queryControl.equals("i"))
						startFileManager(LOAD_GAME_REQUEST_CODE, 1, 1);
					else
						queryControl = "";
				}
			});
			AlertDialog alert = builder.create();
			return alert;
		}
		if (id == PROMOTION_DIALOG)
		{
			promotionDialog = new ChessPromotion(this, new OnPromotionListener(), gc.cl.p_color);
			return promotionDialog;
		}
		if (id == TIME_SETTINGS_DIALOG)
		{
			chessClockTitle = getString(R.string.ccsTitle);
			timeSettingsDialog = new TimeSettingsDialog(this, this, chessClockTitle.toString(), chessClockMessage.toString(),
					chessClockTimeGame, chessClockTimeBonus, chessClockMovesToGo);
			timeSettingsDialog.setOnCancelListener(new DialogInterface.OnCancelListener()
			{
				public void onCancel(DialogInterface dialog) { onCancelDialog(); }
			});
			return timeSettingsDialog;
		}

		if (id == INFO_DIALOG)
		{
			String mes = runP.getString("infoMessage", "") + "\n\n";
			mes = mes + "Model: " + runP.getString("infoModelNumber", "") + "\n";
			mes = mes + "Android-Version: " + runP.getString("infoAndroidVersion", "") + "\n";
			mes = mes + "DB-Version: " + runP.getString("infoDbVersion", "");
			c4aDialog = new C4aDialog(this, this, runP.getString("infoTitle", ""),
					"", getString(R.string.btn_Ok), "", mes, 0, "");
			return c4aDialog;
		}

		if (id == PLAY_DIALOG)
		{

//			Log.i(TAG, "1 onCreateDialog(), PLAY_DIALOG, restartPlayDialog: " + restartPlayDialog + ", dRestartEngine: " + dRestartEngine);

			if (!restartPlayDialog)
				dRestartEngine = false;
			restartPlayDialog = false;
			dNewGame = false;
			dSetClock = false;
			dChessEnginePlayMod = ec.chessEnginePlayMod;
            u.setTextViewColors(lblPlayerNameB, cv, cv.COLOR_DATA_BACKGROUND_16, cv.COLOR_DATA_TEXT_17);
			playDialog = new Dialog(this);

			playDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			playDialog.setContentView(R.layout.dialogplay);

			MyViewListener myViewListener = new MyViewListener();

			d_scrollView  = playDialog.findViewById(R.id.scrollView);

			// cb0
			d_cb_debugInformation = playDialog.findViewById(R.id.cb_debugInformation);
			d_cb_debugInformation.setChecked(userPrefs.getBoolean("user_options_enginePlay_debugInformation", false));
			d_cb_debugInformation.setOnClickListener(myViewListener);
			d_cb_logging = playDialog.findViewById(R.id.cb_logging);
			d_cb_logging.setChecked(userPrefs.getBoolean("user_options_enginePlay_logOn", false));
			d_cb_logging.setOnClickListener(myViewListener);
			// cb1
			d_cb_screenTimeout = playDialog.findViewById(R.id.cb_screenTimeout);
			d_cb_screenTimeout.setChecked(userPrefs.getBoolean("user_options_gui_disableScreenTimeout", false));
			d_cb_screenTimeout.setOnClickListener(myViewListener);
			d_cb_engineAutostart = playDialog.findViewById(R.id.cb_engineAutostart);
			d_cb_engineAutostart.setChecked(userPrefs.getBoolean("user_options_enginePlay_AutoStartEngine", true));
			d_cb_engineAutostart.setOnClickListener(myViewListener);
			// cb2
			d_cb_fullScreen = playDialog.findViewById(R.id.cb_fullScreen);
			d_cb_fullScreen.setChecked(userPrefs.getBoolean("user_options_gui_StatusBar", false));
			d_cb_fullScreen.setOnClickListener(myViewListener);
			d_cb_boardFlip = playDialog.findViewById(R.id.cb_boardFlip);
			d_cb_boardFlip.setChecked(userPrefs.getBoolean("user_options_gui_FlipBoard", false));
			d_cb_boardFlip.setOnClickListener(myViewListener);
			// cb3
			d_cb_lastPosition = playDialog.findViewById(R.id.cb_lastPosition);
			d_cb_lastPosition.setChecked(userPrefs.getBoolean("user_options_gui_LastPosition", false));
			d_cb_lastPosition.setOnClickListener(myViewListener);
			d_cb_pgnDb = playDialog.findViewById(R.id.cb_pgnDb);
			d_cb_pgnDb.setChecked(userPrefs.getBoolean("user_options_gui_usePgnDatabase", true));
			d_cb_pgnDb.setOnClickListener(myViewListener);
			// cb4
			d_cb_coordinates = playDialog.findViewById(R.id.cb_coordinates);
			d_cb_coordinates.setChecked(userPrefs.getBoolean("user_options_gui_Coordinates", false));
			d_cb_coordinates.setOnClickListener(myViewListener);
			d_cb_blindMode = playDialog.findViewById(R.id.cb_blindMode);
			d_cb_blindMode.setChecked(userPrefs.getBoolean("user_options_gui_BlindMode", false));
			d_cb_blindMode.setOnClickListener(myViewListener);
			// cb5
			d_cb_openingBook = playDialog.findViewById(R.id.cb_openingBook);
			d_cb_openingBook.setChecked(userPrefs.getBoolean("user_options_enginePlay_OpeningBook", true));
			d_cb_openingBook.setOnClickListener(myViewListener);
			d_cb_openingBookHints = playDialog.findViewById(R.id.cb_openingBookHints);
			d_cb_openingBookHints.setChecked(userPrefs.getBoolean("user_options_enginePlay_ShowBookHints", true));
			d_cb_openingBookHints.setOnClickListener(myViewListener);
			// cb6
			d_cb_posibleMoves = playDialog.findViewById(R.id.cb_posibleMoves);
			d_cb_posibleMoves.setChecked(userPrefs.getBoolean("user_options_gui_posibleMoves", true));
			d_cb_posibleMoves.setOnClickListener(myViewListener);
			d_cb_quickMove = playDialog.findViewById(R.id.cb_quickMove);
			d_cb_quickMove.setChecked(userPrefs.getBoolean("user_options_gui_quickMove", true));
			d_cb_quickMove.setOnClickListener(myViewListener);
			// cb7
			d_cb_audio = playDialog.findViewById(R.id.cb_audio);
			d_cb_audio.setChecked(userPrefs.getBoolean("user_options_gui_enableSounds", true));
			d_cb_audio.setOnClickListener(myViewListener);
			d_cb_moveList = playDialog.findViewById(R.id.cb_moveList);
			d_cb_moveList.setChecked(userPrefs.getBoolean("user_options_gui_moveList", true));
			d_cb_moveList.setOnClickListener(myViewListener);
			// cb8
			d_cb_ponder = playDialog.findViewById(R.id.cb_ponder);
			d_cb_ponder.setChecked(userPrefs.getBoolean("user_options_enginePlay_Ponder", false));
			d_cb_ponder.setOnClickListener(myViewListener);
			d_cb_engineThinking = playDialog.findViewById(R.id.cb_engineThinking);
			d_cb_engineThinking.setChecked(userPrefs.getBoolean("user_options_enginePlay_EngineMessage", true));
			d_cb_engineThinking.setOnClickListener(myViewListener);

			// btn_menues
			d_btn_menu_left = playDialog.findViewById(R.id.btn_menu_left);
			u.setTextViewColors(d_btn_menu_left, "#EFE395");
			d_btn_menu_left.setOnClickListener(myViewListener);
			d_btn_menu_right = playDialog.findViewById(R.id.btn_menu_right);
			u.setTextViewColors(d_btn_menu_right, "#EFE395");
			d_btn_menu_right.setOnClickListener(myViewListener);

			// btn_time
			d_btn_time_setting = playDialog.findViewById(R.id.btn_time_setting);
			d_btn_time_setting.setOnClickListener(myViewListener);
			u.setTextViewColors(d_btn_time_setting, "#b2d9e4");
			d_btn_time_white = playDialog.findViewById(R.id.btn_time_white);
			d_btn_time_white.setOnClickListener(myViewListener);
			if (ec.chessEnginePlayMod <= 3 || ec.chessEnginePlayMod == 5)
				d_btn_time_white.setText(tc.showWhiteTime);
			else
				d_btn_time_white.setText("");
			u.setTextViewColors(d_btn_time_white, "#edebed");
			d_btn_time_black = playDialog.findViewById(R.id.btn_time_black);
			d_btn_time_black.setOnClickListener(myViewListener);
			if (ec.chessEnginePlayMod <= 3 || ec.chessEnginePlayMod == 5)
				d_btn_time_black.setText(tc.showBlackTime);
			else
				d_btn_time_black.setText("");
			u.setTextViewColors(d_btn_time_black, "#000000");
			d_btn_elo = playDialog.findViewById(R.id.btn_elo);

			String showElo = getString(R.string.elo) + " " + userPrefs.getInt("uci_elo", 3000);

			if (ec.getEngine() != null) {
				if (ec.getEngine().uciOptions.equals(""))
					u.setTextViewColors(d_btn_elo, "#efe395");
				else {
					if (ec.getEngine().uciOptions.contains("UCI_Elo")) {
						u.setTextViewColors(d_btn_elo, "#ADE4A7");
						if (ec.getEngine().uciEloMin > +userPrefs.getInt("uci_elo", 3000) || ec.getEngine().uciEloMax < +userPrefs.getInt("uci_elo", 3000))
							showElo = getString(R.string.elo) + " (" + ec.getEngine().uciEloMin + ")";
						if (ec.getEngine().uciEloMax < +userPrefs.getInt("uci_elo", 3000))
							showElo = getString(R.string.elo) + " (" + ec.getEngine().uciEloMax + ")";
					} else {
						u.setTextViewColors(d_btn_elo, "#f6d2f4");
						showElo = getString(R.string.elo) + " (-)";
					}
				}
			}

			d_btn_elo.setText(showElo);
			d_btn_elo.setOnClickListener(myViewListener);

			// btn_engines
			d_btn_engine_select = playDialog.findViewById(R.id.btn_engine_select);
			d_btn_engine_select.setOnClickListener(myViewListener);
			u.setTextViewColors(d_btn_engine_select, "#b2d9e4");
			d_btn_engine_select.setText(runP.getString("run_engineProcess", MainActivity.OEX_DEFAULT_ENGINE_SINGLE));
			d_btn_settings = playDialog.findViewById(R.id.btn_settings);
			d_btn_settings.setOnClickListener(myViewListener);
			u.setTextViewColors(d_btn_settings, "#b2d9e4");
			d_btn_engine_uci_options = playDialog.findViewById(R.id.btn_engine_uci_options);
			d_btn_engine_uci_options.setOnClickListener(myViewListener);
			u.setTextViewColors(d_btn_engine_uci_options, "#b2d9e4");

			// btn_play_a
			d_btn_white = playDialog.findViewById(R.id.btn_white);
			d_btn_white.setOnClickListener(myViewListener);
			d_btn_black = playDialog.findViewById(R.id.btn_black);
			d_btn_black.setOnClickListener(myViewListener);
			d_btn_engine = playDialog.findViewById(R.id.btn_engine);
			if (userPrefs.getBoolean("user_play_eve_engineVsEngine", true))
				d_btn_engine.setText(getString(R.string.play_engine) + " (2)");
			else
				d_btn_engine.setText(getString(R.string.play_engine) + " (1)");
			d_btn_engine.setOnClickListener(myViewListener);
			d_btn_engine.setOnLongClickListener(arg0 -> {
				dChessEnginePlayMod = 3;
				setPlayModBackground(dChessEnginePlayMod);
				stopSearchAndContinue(EngineState.STOP_QUIT, "", true);
				restartPlayDialog = true;
				startActivityForResult(computerMatch, COMPUTER_MATCH);
				return true;
			});

			// btn_play_b
			d_btn_player = playDialog.findViewById(R.id.btn_player);
			d_btn_player.setOnClickListener(myViewListener);
			d_btn_edit = playDialog.findViewById(R.id.btn_edit);
			d_btn_edit.setOnClickListener(myViewListener);
			d_btn_analysis = playDialog.findViewById(R.id.btn_analysis);
			String[] txtSplit = runP.getString("run_engineListAnalysis", OEX_DEFAULT_ENGINES_ANALYSIS).split("\\|");
			if (userPrefs.getBoolean("user_play_multipleEngines", true) && withMultiEngineAnalyse)
				d_btn_analysis.setText(getString(R.string.play_analysis) + " (" + txtSplit.length + ")");
			else
				d_btn_analysis.setText(getString(R.string.play_analysis) + " (1)");
			d_btn_analysis.setOnClickListener(myViewListener);
			d_btn_analysis.setOnLongClickListener(arg0 -> {
				if (withMultiEngineAnalyse) {
					dChessEnginePlayMod = 4;
					dRestartEngine = true;
					setPlayModBackground(dChessEnginePlayMod);
					stopSearchAndContinue(EngineState.STOP_QUIT, "", true);
					restartPlayDialog = true;
					startActivityForResult(analysisOptions, ANALYSIS_OPTIONS);
				}
				return true;
			});
            setPlayModBackground(dChessEnginePlayMod);

			// btn_pos
			d_btn_standard = playDialog.findViewById(R.id.btn_standard);
			d_btn_standard.setOnClickListener(myViewListener);
			d_btn_chess960 = playDialog.findViewById(R.id.btn_chess960);
			d_btn_chess960.setOnClickListener(myViewListener);
			d_btn_continue = playDialog.findViewById(R.id.btn_continue);
			d_btn_continue.setOnClickListener(myViewListener);

			// ScrollView force to bottom
			d_scrollView.postDelayed(new Runnable() {
				@Override
				public void run() {
					d_scrollView.fullScroll(ScrollView.FOCUS_DOWN);
				}
			},100);

//			Log.i(TAG, "9 onCreateDialog(), PLAY_DIALOG");

			return playDialog;

		}

		if (id == RATE_DIALOG)
		{
			Dialog dialog = new Dialog(this);
			dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			dialog.setContentView(R.layout.dialograte);
			MyViewListener myViewListener = new MyViewListener();
			btn_rate = dialog.findViewById(R.id.btn_rate);
			u.setTextViewColors(btn_rate, cv, cv.COLOR_DATA_BACKGROUND_16, cv.COLOR_DATA_TEXT_17);
			btn_rate.setOnClickListener(myViewListener);
			btn_no = dialog.findViewById(R.id.btn_no);
			btn_no.setOnClickListener(myViewListener);
			return dialog;
		}

		if (id == MOVE_NOTIFICATION_DIALOG)
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(this)
				.setMessage(gc.cl.p_moveText);
			AlertDialog alert = builder.create();
			return alert;
		}

		if (id == GAME_ID_DIALOG)
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle("Game ID Dialog");
			final EditText input = new EditText(this);
			input.setInputType(InputType.TYPE_CLASS_NUMBER);
			input.setRawInputType(Configuration.KEYBOARD_12KEY);
			input.setText("1");
			builder.setView(input);
			builder.setPositiveButton("Ok", new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int whichButton)
				{
					int gameId = Integer.parseInt(input.getText().toString());
					queryControl = "i";
					startFileManager(LOAD_GAME_REQUEST_CODE, 0, gameId);
				}
			});

			AlertDialog alert = builder.create();
			alert.setCancelable(true);
			return alert;
		}

		if (id == UCI_ELO_DIALOG)
		{

//			Log.i(TAG, "onCreateDialog(), UCI_ELO_DIALOG");

			if (ec.getEngine() == null) {
				showDialog(PLAY_DIALOG);
				return null;
			}

			elo = userPrefs.getInt("uci_elo", UciEngine.UCI_ELO_STANDARD);
			if (elo < eloMin)
				elo = eloMin;
			if (elo > eloMax)
				elo = eloMax;
			Dialog dialog = new Dialog(this);
			dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			dialog.setContentView(R.layout.dialog_set_uci_elo);
			eloEngine = dialog.findViewById(R.id.eloEngine);

//			Log.i(TAG, "onCreateDialog(), uciOptions: " + ec.getEngine().uciOptions);
//			Log.i(TAG, "onCreateDialog(), engineName: " + ec.getEngine().engineName);

			eloEngine.setText(u.getEngineEloFromC4aElo(elo, ec.getEngine().uciOptions, ec.getEngine().engineName, ec.getEngine().uciEloMin, ec.getEngine().uciEloMax));
			eloSeekBar = dialog.findViewById(R.id.eloSeekBar);
			eloSeekBar.setMax(eloMax);
			eloSeekBar.setProgress(elo);
			info = dialog.findViewById(R.id.info);
			info.setText(getString(R.string.optionSupportInfo));
			if (ec.getEngine().uciOptions.contains("UCI_Elo"))
				info.setText("");
			eloSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {}
				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {}
				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					if (fromUser) {
						elo = progress;
						if(elo < eloMin) {
							seekBar.setProgress(eloMin);
							elo = eloMin;
						}
						eloEngine.setText(u.getEngineEloFromC4aElo(elo, ec.getEngine().uciOptions, ec.getEngine().engineName, ec.getEngine().uciEloMin, ec.getEngine().uciEloMax));
						eloValue.setText(Integer.toString(elo));
					}
				}
			});
			eloValue = dialog.findViewById(R.id.eloValue);
			eloValue.setText(Integer.toString(elo));
			if (ec.getEngine().uciOptions.contains("UCI_Elo"))
				u.setTextViewColors(eloValue, "#ADE4A7");
			else
				u.setTextViewColors(eloValue, "#f6d2f4");
			if (ec.getEngine().uciOptions.equals(""))
				u.setTextViewColors(eloValue, "#efe395");
			eloValue.addTextChangedListener(new TextWatcher() {
				public void afterTextChanged(Editable s) {
					if (!s.toString().equals("")) {
						int e = (Integer.parseInt(s.toString()));
						if (e < eloMin) {
							e = eloMin;
						}
						if (e > eloMax) {
							e = eloMax;
						}
						elo = e;
						eloEngine.setText(u.getEngineEloFromC4aElo(elo, ec.getEngine().uciOptions, ec.getEngine().engineName, ec.getEngine().uciEloMin, ec.getEngine().uciEloMax));
						eloSeekBar.setProgress(e);
					}
				}
				public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
				public void onTextChanged(CharSequence s, int start, int before, int count) {}
			});
			eloInfo = dialog.findViewById(R.id.eloInfo);
			String info = "( " + eloMin + " - " + eloMax + " )";
			eloInfo.setText(info);
			btn_cancel = dialog.findViewById(R.id.btn_cancel);
			u.setTextViewColors(btn_cancel, "#BAB8B8", "#000000");
			btn_cancel.setOnClickListener(v -> {
				removeDialog(UCI_ELO_DIALOG);
				if (restartPlayDialog)
					showDialog(PLAY_DIALOG);
			});
			btn_ok = dialog.findViewById(R.id.btn_ok);
			u.setTextViewColors(btn_ok, "#BAB8B8", "#000000");
			btn_ok.setOnClickListener(v -> {
				SharedPreferences.Editor ed = userPrefs.edit();
				ed.putInt("uci_elo", elo);
				ed.apply();
				removeDialog(UCI_ELO_DIALOG);
				if (restartPlayDialog) {
					dRestartEngine = true;
					showDialog(PLAY_DIALOG);
				}
			});
			dialog.setOnCancelListener(dialogAction ->
				{
					if (restartPlayDialog)
						showDialog(PLAY_DIALOG);
				}
			);
			return dialog;
		}

		if (id == MOVE_NOTIFICATION_DIALOG)
		{
			AlertDialog.Builder builder = new AlertDialog.Builder(this)
					.setMessage(gc.cl.p_moveText);
			AlertDialog alert = builder.create();
			return alert;
		}

		if (id == MENU_BOARD_DIALOG)
		{
			final int MENU_EDIT_BOARD     	= 0;
			final int MENU_COLOR		  	= 1;
			final int MENU_COORDINATES      = 2;
			final int MENU_CLIPBOARD    	= 3;
			final int MENU_FILE  			= 4;
			ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.drawer_list_item);
			List<Integer> actions = new ArrayList<Integer>();
			arrayAdapter.add(getString(R.string.menu_edit_board));     	actions.add(MENU_EDIT_BOARD);
			arrayAdapter.add(getString(R.string.menu_board_color_settings)); 	actions.add(MENU_COLOR);
			arrayAdapter.add(getString(R.string.menu_board_coordinates)); 		actions.add(MENU_COORDINATES);
			arrayAdapter.add(getString(R.string.menu_board_clipboard)); 			actions.add(MENU_CLIPBOARD);
			arrayAdapter.add(getString(R.string.fmLblFile)); 				actions.add(MENU_FILE);
			final List<Integer> finalActions = actions;
			AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
			builder.setCancelable(true);
			TextView tv = new TextView(getApplicationContext());
			tv.setText(R.string.menu_board);
			tv.setTextAppearance(this, R.style.c4aDialogTitle);
			tv.setGravity(Gravity.CENTER_HORIZONTAL);
			builder.setCustomTitle(tv );
			builder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int item)
				{
					switch (finalActions.get(item))
					{
						case MENU_EDIT_BOARD:
							startEditBoard(gc.fen, false);
							break;
						case MENU_COLOR:
							removeDialog(MENU_COLOR_SETTINGS);
							showDialog(MENU_COLOR_SETTINGS);
							break;
						case MENU_COORDINATES:
							SharedPreferences.Editor ed = userPrefs.edit();
							if (userPrefs.getBoolean("user_options_gui_Coordinates", false))
								ed.putBoolean("user_options_gui_Coordinates", false);
							else
								ed.putBoolean("user_options_gui_Coordinates", true);
							ed.commit();
							updateGui();
							break;
						case MENU_CLIPBOARD:
							removeDialog(MENU_CLIPBOARD_DIALOG);
							showDialog(MENU_CLIPBOARD_DIALOG);
							break;
						case MENU_FILE:
							removeDialog(MENU_PGN_DIALOG);
							showDialog(MENU_PGN_DIALOG);
							break;
					}
				}
			});
			AlertDialog alert = builder.create();
			return alert;
		}

		if (id == MENU_PGN_DIALOG)
		{
			final int MENU_PGN_LOAD     		= 0;
			final int MENU_PGN_SAVE  			= 1;
			final int MENU_PGN_DOWNLOAD    		= 3;
			final int MENU_PGN_CB_COPY    		= 5;
			final int MENU_PGN_CB_COPY_POS 		= 6;
			final int MENU_PGN_CB_PAST    		= 7;
			ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.drawer_list_item);
			List<Integer> actions = new ArrayList<Integer>();
			arrayAdapter.add(getString(R.string.menu_pgn_load));     				actions.add(MENU_PGN_LOAD);
			arrayAdapter.add(getString(R.string.menu_pgn_save)); 					actions.add(MENU_PGN_SAVE);
			arrayAdapter.add(getString(R.string.menu_load_www));     		actions.add(MENU_PGN_DOWNLOAD);
			arrayAdapter.add(getString(R.string.menu_info_clipboardCopyPgn)); 	actions.add(MENU_PGN_CB_COPY);
			arrayAdapter.add(getString(R.string.menu_info_clipboardCopyFen)); actions.add(MENU_PGN_CB_COPY_POS);
			arrayAdapter.add(getString(R.string.menu_info_clipboardPaste)); 	actions.add(MENU_PGN_CB_PAST);
			final List<Integer> finalActions = actions;
			AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
			builder.setCancelable(true);
			TextView tv = new TextView(getApplicationContext());
			tv.setText(R.string.menu_pgn);
			tv.setTextAppearance(this, R.style.c4aDialogTitle);
			tv.setGravity(Gravity.CENTER_HORIZONTAL);
			builder.setCustomTitle(tv );
			builder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int item)
				{
					switch (finalActions.get(item))
					{
						case MENU_PGN_LOAD:
							startFileManager(LOAD_GAME_REQUEST_CODE, 1, 0);
							break;
						case MENU_PGN_SAVE:
							startFileManager(SAVE_GAME_REQUEST_CODE, 1, 0);
							break;
						case MENU_PGN_DOWNLOAD:
							startPgnDownload();
							break;
						case MENU_PGN_CB_COPY:
							setToClipboard(gc.cl.history.createPgnFromHistory(1));
							break;
						case MENU_PGN_CB_COPY_POS:
							setToClipboard(gc.cl.history.getMoveFen(gc.cl.history.getMoveIdx()));
							break;
						case MENU_PGN_CB_PAST:
							messageEngine = new SpannableStringBuilder("");
							messageEngineShort  = "";
							getFromClipboard("", 0);
							break;
					}
				}
			});
			AlertDialog alert = builder.create();
			return alert;
		}

		if (id == MENU_CLIPBOARD_DIALOG)
		{
			final int MENU_CLIPBOARD_COPY    		= 0;
			final int MENU_CLIPBOARD_COPY_POS 		= 1;
			final int MENU_CLIPBOARD_PAST    		= 2;
			ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.drawer_list_item);
			List<Integer> actions = new ArrayList<Integer>();
			arrayAdapter.add(getString(R.string.menu_info_clipboardCopyPgn)); 	actions.add(MENU_CLIPBOARD_COPY);
			arrayAdapter.add(getString(R.string.menu_info_clipboardCopyFen)); actions.add(MENU_CLIPBOARD_COPY_POS);
			arrayAdapter.add(getString(R.string.menu_info_clipboardPaste)); 	actions.add(MENU_CLIPBOARD_PAST);
			final List<Integer> finalActions = actions;
			AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
			builder.setCancelable(true);
			TextView tv = new TextView(getApplicationContext());
			tv.setText(R.string.menu_board_clipboard);
			tv.setTextAppearance(this, R.style.c4aDialogTitle);
			tv.setGravity(Gravity.CENTER_HORIZONTAL);
			builder.setCustomTitle(tv );
			builder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int item)
				{
					switch (finalActions.get(item))
					{
						case MENU_CLIPBOARD_COPY:
							setToClipboard(gc.cl.history.createPgnFromHistory(1));
							break;
						case MENU_CLIPBOARD_COPY_POS:
							setToClipboard(gc.cl.history.getMoveFen(gc.cl.history.getMoveIdx()));
							break;
						case MENU_CLIPBOARD_PAST:
							messageEngine = new SpannableStringBuilder("");
							messageEngineShort  = "";
							getFromClipboard("", 0);
							break;
					}
				}
			});
			AlertDialog alert = builder.create();
			return alert;
		}

		if (id == MENU_COLOR_SETTINGS)
		{
			final int MENU_COLOR_SETTINGS_BROWN    		= 0;
			final int MENU_COLOR_SETTINGS_VIOLET 		= 1;
			final int MENU_COLOR_SETTINGS_GREY    		= 2;
			final int MENU_COLOR_SETTINGS_BLUE   		= 3;
			final int MENU_COLOR_SETTINGS_GREEN    		= 4;
			ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice);
			List<Integer> actions = new ArrayList<Integer>();
			arrayAdapter.add(getColorName(MENU_COLOR_SETTINGS_BROWN)); 			actions.add(MENU_COLOR_SETTINGS_BROWN);
			arrayAdapter.add(getColorName(MENU_COLOR_SETTINGS_VIOLET)); 		actions.add(MENU_COLOR_SETTINGS_VIOLET);
			arrayAdapter.add(getColorName(MENU_COLOR_SETTINGS_GREY)); 			actions.add(MENU_COLOR_SETTINGS_GREY);
			arrayAdapter.add(getColorName(MENU_COLOR_SETTINGS_BLUE)); 			actions.add(MENU_COLOR_SETTINGS_BLUE);
			arrayAdapter.add(getColorName(MENU_COLOR_SETTINGS_GREEN)); 			actions.add(MENU_COLOR_SETTINGS_GREEN);
			final List<Integer> finalActions = actions;
			AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
			builder.setCancelable(true);
			TextView tv = new TextView(getApplicationContext());
			tv.setText(R.string.menu_colorsettings);
			tv.setTextAppearance(this, R.style.c4aDialogTitle);
			tv.setGravity(Gravity.CENTER_HORIZONTAL);
			builder.setCustomTitle(tv );
			builder.setSingleChoiceItems(arrayAdapter, userPrefs.getInt("colorId", 0), new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int item)
				{
					SharedPreferences.Editor ed = userPrefs.edit();
					ed.putInt("colorId", finalActions.get(item));
					ed.commit();
					startActivityForResult(optionsColorIntent, OPTIONS_COLOR_SETTINGS);
					removeDialog(MENU_COLOR_SETTINGS);
				}
			});
			AlertDialog alert = builder.create();
			return alert;
		}

		if (id == MENU_ENGINES_DIALOG)
		{
			final int MENU_ENGINE_SELECT 		= 0;
			final int MENU_ENGINE_UCI 			= 1;
			final int MENU_ENGINE_MATCH			= 2;
			final int MENU_ENGINE_ANALYSIS		= 3;
			final int MENU_ENGINE_SHUTDOWN 		= 9;
			ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.drawer_list_item);
			List<Integer> actions = new ArrayList<Integer>();
			arrayAdapter.add(getString(R.string.menu_enginesettings_select));			actions.add(MENU_ENGINE_SELECT);
			arrayAdapter.add(getString(R.string.menu_enginesettings_uciOptions));		actions.add(MENU_ENGINE_UCI);
			arrayAdapter.add(getString(R.string.app_computerMatch));					actions.add(MENU_ENGINE_MATCH);
			arrayAdapter.add(getString(R.string.analysisOptions));						actions.add(MENU_ENGINE_ANALYSIS);
			arrayAdapter.add(getString(R.string.menu_enginesettings_shutdown));   		actions.add(MENU_ENGINE_SHUTDOWN);
			final List<Integer> finalActions = actions;
			AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
			builder.setCancelable(true);
			TextView tv = new TextView(getApplicationContext());
			tv.setText(R.string.menu_enginesettings);
			tv.setTextAppearance(this, R.style.c4aDialogTitle);
			tv.setGravity(Gravity.CENTER_HORIZONTAL);
			builder.setCustomTitle(tv );
			builder.setAdapter(arrayAdapter, (dialog, item) -> {
				switch (finalActions.get(item))
				{
					case MENU_ENGINE_SELECT:
						removeDialog(MENU_SELECT_ENGINE_FROM_OEX);
						showDialog(MENU_SELECT_ENGINE_FROM_OEX);
						break;
					case MENU_ENGINE_UCI:
						startEditUciOptions(runP.getString("run_engineProcess", MainActivity.OEX_DEFAULT_ENGINE_SINGLE));
						break;
					case MENU_ENGINE_MATCH:
						setPlayModBackground(dChessEnginePlayMod);
						stopSearchAndContinue(EngineState.STOP_QUIT, "", true);
						restartPlayDialog = true;
						startActivityForResult(computerMatch, COMPUTER_MATCH);
						break;
					case MENU_ENGINE_ANALYSIS:
						if (withMultiEngineAnalyse) {
							dChessEnginePlayMod = 4;
							dRestartEngine = true;
							setPlayModBackground(dChessEnginePlayMod);
							stopSearchAndContinue(EngineState.STOP_QUIT, "", true);
							restartPlayDialog = true;
							startActivityForResult(analysisOptions, ANALYSIS_OPTIONS);
						}
						break;
					case MENU_ENGINE_SHUTDOWN:
						stopAllEnginesAndInit();
						break;
				}
			});
			AlertDialog alert = builder.create();
			return alert;
		}

		if (id == MENU_ABOUT_DIALOG)
		{
			final int MENU_ABOUT_NEW     		= 0;
			final int MENU_ABOUT_RATE     		= 1;
			final int MENU_ABOUT_APPS	    	= 4;
			final int MENU_ABOUT_WEBSITE    	= 5;
			final int MENU_ABOUT_SOURCECODE    	= 6;
			final int MENU_ABOUT_CONTACT    	= 7;
			ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.drawer_list_item);
			List<Integer> actions = new ArrayList<Integer>();
			arrayAdapter.add(getString(R.string.menu_about_new));     		actions.add(MENU_ABOUT_NEW);
			arrayAdapter.add(getString(R.string.rate_title));     actions.add(MENU_ABOUT_RATE);
			arrayAdapter.add(getString(R.string.menu_about_apps)); 				actions.add(MENU_ABOUT_APPS);
			arrayAdapter.add(getString(R.string.menu_about_website)); 				actions.add(MENU_ABOUT_WEBSITE);
			arrayAdapter.add(getString(R.string.menu_about_sourcecode)); 			actions.add(MENU_ABOUT_SOURCECODE);
			arrayAdapter.add(getString(R.string.menu_about_contact)); 				actions.add(MENU_ABOUT_CONTACT);
			final List<Integer> finalActions = actions;
			AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
			TextView tv = new TextView(getApplicationContext());
			tv.setText(R.string.menu_about);
			tv.setTextAppearance(this, R.style.c4aDialogTitle);
			tv.setGravity(Gravity.CENTER_HORIZONTAL);
			builder.setCustomTitle(tv );
			builder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int item)
				{
					switch (finalActions.get(item))
					{
						case MENU_ABOUT_NEW:
							showHtml(R.raw.whats_new, R.string.whatsNew);
							break;
						case MENU_ABOUT_RATE:
							removeDialog(RATE_DIALOG);
							showDialog(RATE_DIALOG);
							break;
						case MENU_ABOUT_APPS:
							Intent ir = new Intent(Intent.ACTION_VIEW);
							ir.setData(Uri.parse(MY_APPS));
							startActivityForResult(ir, RATE_REQUEST_CODE);
							break;
						case MENU_ABOUT_WEBSITE:
							Intent irw = new Intent(Intent.ACTION_VIEW);
							irw.setData(Uri.parse("http://c4akarl.blogspot.com/"));
							startActivityForResult(irw, RATE_REQUEST_CODE);
							break;
						case MENU_ABOUT_SOURCECODE:
							Intent irs = new Intent(Intent.ACTION_VIEW);
							irs.setData(Uri.parse("https://github.com/c4akarl/ChessForAll"));
							startActivityForResult(irs, RATE_REQUEST_CODE);
							break;
						case MENU_ABOUT_CONTACT:
							Intent intent = new Intent(Intent.ACTION_SENDTO);
							intent.setData(Uri.parse("mailto:"));
							intent.putExtra(Intent.EXTRA_EMAIL, new String[]{APP_EMAIL.toString()});
							intent.putExtra(Intent.EXTRA_SUBJECT, "");
							intent.putExtra(Intent.EXTRA_TEXT,"");
							if (intent.resolveActivity(getPackageManager()) != null) {
								startActivity(intent);
							}
							break;
					}
				}
			});
			AlertDialog alert = builder.create();
			return alert;
		}

		if (id == MENU_SHOW_LIST)
		{
			final int MENU_SHOW_LIST_MOVES     	= 0;
			final int MENU_SHOW_LIST_ENGINE    	= 1;
			ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.drawer_list_item);
			List<Integer> actions = new ArrayList<Integer>();
			arrayAdapter.add(getString(R.string.colorMoveList));     				actions.add(MENU_SHOW_LIST_MOVES);
			arrayAdapter.add(getString(R.string.colorEngineList)); 				actions.add(MENU_SHOW_LIST_ENGINE);
			final List<Integer> finalActions = actions;
			AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
			TextView tv = new TextView(getApplicationContext());
			tv.setText(R.string.menu_show_list);
			tv.setTextAppearance(this, R.style.c4aDialogTitle);
			tv.setGravity(Gravity.CENTER_HORIZONTAL);
			builder.setCustomTitle(tv );
			builder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int item)
				{
					SharedPreferences.Editor ed = userPrefs.edit();
					switch (finalActions.get(item))
					{
						case MENU_SHOW_LIST_MOVES:
							if (userPrefs.getBoolean("user_options_gui_moveList", true))
								ed.putBoolean("user_options_gui_moveList", false);
							else
								ed.putBoolean("user_options_gui_moveList", true);
							ed.commit();
							setInfoMessage("", null, "");
							break;
						case MENU_SHOW_LIST_ENGINE:
							if (userPrefs.getBoolean("user_options_enginePlay_EngineMessage", true))
							{
								ed.putBoolean("user_options_enginePlay_EngineMessage", false);
								ed.commit();
								setInfoMessage("", null, null);
							}
							else
							{
								ed.putBoolean("user_options_enginePlay_EngineMessage", true);
								ed.commit();
								setInfoMessage("", null, null);
							}
							break;
					}
				}
			});
			AlertDialog alert = builder.create();
			return alert;
		}

		if (id == MENU_SELECT_ENGINE_FROM_OEX)
		{
			final ArrayList<String> items = new ArrayList<>();
			XmlResourceParser parser = getResources().getXml(R.xml.enginelist);
			try {
				int eventType = parser.getEventType();
				while (eventType != XmlResourceParser.END_DOCUMENT) {
					try {
						if (eventType == XmlResourceParser.START_TAG) {
							if (parser.getName().equalsIgnoreCase("engine"))
								items.add(parser.getAttributeValue(null, "name"));
						}
						eventType = parser.next();
					} catch (IOException e) {

//						Log.e(TAG, e.getLocalizedMessage(), e);

					}
				}
			} catch (XmlPullParserException e) {

//				Log.e(TAG, e.getLocalizedMessage(), e);

			}

			if (storageAvailable()) {
				ChessEngineResolver resolver = new ChessEngineResolver(this);

//				Log.i(TAG, "MENU_SELECT_ENGINE_FROM_OEX, resolver.target: " + resolver.target);

				List<ChessEngine> engines = resolver.resolveEngines();
				ArrayList<android.util.Pair<String,String>> oexEngines = new ArrayList<>();
				for (ChessEngine engine : engines) {
					if ((engine.getName() != null) && (engine.getFileName() != null) &&
							(engine.getPackageName() != null)) {
						oexEngines.add(new android.util.Pair<>(FileIO.openExchangeFileName(engine),
								engine.getName()));

//						Log.i(TAG, "MENU_SELECT_ENGINE_FROM_OEX,  engine.getEnginePath(): " + engine.getEnginePath());

					}
				}
				Collections.sort(oexEngines, (lhs, rhs) -> lhs.second.compareTo(rhs.second));
				for (android.util.Pair<String,String> eng : oexEngines) {
					if (!eng.second.endsWith(".txt"))
						items.add(eng.second);
				}

				String[] fileNames = FileIO.findFilesInDirectory(engineDir,
						fname -> !reservedEngineName(fname));
				for (String file : fileNames) {
					if (!file.endsWith(".txt"))
						items.add(file);
				}
			}
			else {

//				Log.i(TAG, "MENU_SELECT_ENGINE_FROM_OEX, storageAvailable: false");

			}

			String currEngine = runP.getString("run_engineProcess", MainActivity.OEX_DEFAULT_ENGINE_SINGLE);

			int defaultItem = 0;
			final int nEngines = items.size();
			for (int i = 0; i < nEngines; i++) {

//				Log.i(TAG, "MENU_SELECT_ENGINE_FROM_OEX, items.get(i): " + items.get(i) + ", ids.get(i): " + ids.get(i));

				if (items.get(i).equals(currEngine)) {
					defaultItem = i;
					break;
				}
			}

			if (items.size() > 0)
			{
				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.menu_enginesettings_select);
				builder.setSingleChoiceItems(items.toArray(new String[0]), defaultItem,
					(dialog, item) -> {
						if ((item < 0) || (item >= nEngines))
							return;

						dialog.dismiss();

						Toast.makeText(this, getString(R.string.engine_new) + " " + items.get(item), Toast.LENGTH_SHORT).show();
						SharedPreferences.Editor edR = runP.edit();
						edR.putString("run_engineProcess", items.get(item));
						edR.commit();
						SharedPreferences.Editor ed = userPrefs.edit();
						ed.putBoolean("user_play_eve_engineVsEngine", false);
						ed.apply();
						if (restartPlayDialog) {
							dRestartEngine = true;
							showDialog(PLAY_DIALOG);
						}
						else {
							if (ec.chessEnginePaused)
								restartEngine();
							else
								stopSearchAndRestart(false, true);
						}
					});
				builder.setOnCancelListener(dialog ->
						{
							if (restartPlayDialog)
								showDialog(PLAY_DIALOG);
						}
				);
				builder.setCancelable(true);
				AlertDialog alert = builder.create();
				return alert;
			}
			else {

//				Log.i(TAG, "MENU_SELECT_ENGINE_FROM_OEX, no engine");

			}
		}

		return null;

	}

	public void onCancelDialog()
	{

	}

	@Override
	public void getCallbackValue(int btnValue)
	{

//		Log.i(TAG, "getCallbackValue(), btnValue: " + btnValue + ", activDialog: " + activDialog);

		if (activDialog == PGN_ERROR_DIALOG)
		{
			if (btnValue == 1)
			{
				String versionName = BuildConfig.VERSION_NAME;
				int versionCode = BuildConfig.VERSION_CODE;
				String c4aVersion = "********************\n" + "ChessForAll Version: " + versionName + "(" + versionCode + ")";
				String versionAndroid = android.os.Build.VERSION.RELEASE;
				int versionCodeAndroid = android.os.Build.VERSION.SDK_INT;
				String androidVersion = "\nAndroid Version: " + versionAndroid + "(" + versionCodeAndroid + ")";
				String deviceName = "\nDevice name: " + android.os.Build.MODEL;
				Intent intent = new Intent(Intent.ACTION_SENDTO);
				intent.setData(Uri.parse("mailto:"));
				intent.putExtra(Intent.EXTRA_EMAIL, new String[]{APP_EMAIL.toString()});
				intent.putExtra(Intent.EXTRA_SUBJECT, gc.errorMessage);
				intent.putExtra(Intent.EXTRA_TEXT,gc.errorPGN + c4aVersion + androidVersion + deviceName);
				if (intent.resolveActivity(getPackageManager()) != null) {
					startActivity(intent);
				}
			}
		}
		if (activDialog == TIME_SETTINGS_DIALOG)
		{	// chessClockControl
			if (btnValue == 2)
			{
				SharedPreferences.Editor ed = userPrefs.edit();
				Boolean isPlayDialog = false;
				if (playDialog != null)
					isPlayDialog = true;
				switch (chessClockControl)
				{
					case 1: 	// set current time: white
						if (isPlayDialog && playDialog.isShowing()) {
							dSettingTimeWhite = timeSettingsDialog.getTime();
							d_btn_time_white.setText(tc.getShowValues(dSettingTimeWhite, false));
						}
						else {
							tc.timeWhite = timeSettingsDialog.getTime();
							tc.bonusWhite = chessClockTimeBonusSaveWhite;
						}
						break;
					case 2: 	// set current time: black
						if (isPlayDialog && playDialog.isShowing()) {
							dSettingTimeBlack = timeSettingsDialog.getTime();
							d_btn_time_black.setText(tc.getShowValues(dSettingTimeBlack, false));
						}
						else {
							tc.timeBlack = timeSettingsDialog.getTime();
							tc.bonusBlack = chessClockTimeBonusSaveBlack;
						}
						break;
					case 41: 	// timer auto play
						ed.putInt("user_options_timer_autoPlay", timeSettingsDialog.getBonus());
						break;
				}
				ed.commit();
				if (ec.chessEnginePaused)
				{
					tc.setCurrentShowValues(ec.chessEnginePlayMod);
					updateCurrentPosition("");
				}

//				Log.i(TAG, "getCallbackValue(), playDialog.isShowing(): " + playDialog.isShowing());

			}
		}
		if (activDialog == COMPUTER_MATCH_DIALOG)
		{
			if (btnValue == 1)	// cancel
			{
				stopSearchAndContinue(EngineState.STOP_IDLE, "", true);
				stopChessClock();
				ec.chessEngineMatch = false;
				SharedPreferences.Editor edR = runP.edit();
				edR.putBoolean("run_chessEngineAutoRun", false);
				edR.apply();
			}
			if (btnValue == 2)	// finish game
			{
				ec.chessEngineMatchFinishGame = true;
				updateGui();
			}
			if (btnValue == 3)	// continue game
			{

			}
		}
	}

	public void setPlayModBackground(int playmod)
	{
		u.setTextViewColors(d_btn_white, "#EDEBED");
		u.setTextViewColors(d_btn_black, "#EDEBED");
		u.setTextViewColors(d_btn_engine, "#EDEBED");
		u.setTextViewColors(d_btn_player, "#EDEBED");
		u.setTextViewColors(d_btn_edit, "#EDEBED");
		u.setTextViewColors(d_btn_analysis, "#EDEBED");
		switch (playmod)
		{
			case 1:
				u.setTextViewColors(d_btn_white, "#ADE4A7");
				break;
			case 2:
				u.setTextViewColors(d_btn_black, "#ADE4A7");
				break;
			case 3:
				u.setTextViewColors(d_btn_engine, "#ADE4A7");
				break;
			case 4:
				u.setTextViewColors(d_btn_analysis, "#ADE4A7");
				break;
			case 5:
				u.setTextViewColors(d_btn_player, "#ADE4A7");
				break;
			case 6:
				u.setTextViewColors(d_btn_edit, "#ADE4A7");
				break;
		}
	}

	public void displayPlayModTime(int playmod)
	{
		TimeControl t = new TimeControl();
		initChessClock(t, userPrefs.getInt("user_options_timeControl", 1), playmod);
		d_btn_time_white.setText(t.showWhiteTime);
		d_btn_time_black.setText(t.showBlackTime);
	}

//	USER-ACTIONS		USER-ACTIONS		USER-ACTIONS		USER-ACTIONS
//	TOUCH, CLICK		TOUCH, CLICK		TOUCH, CLICK		TOUCH, CLICK
	@Override
	public boolean onTouch(View view, MotionEvent event)
	{

		if (ec.chessEngineMatch) {
			c4aShowDialog(COMPUTER_MATCH_DIALOG);
			return true;
		}

//		Log.i(TAG, "onTouch(), view.getId(), event.getAction(): " + view.getId() + ", " + event.getAction() + ", x: " + event.getRawX() + ", y: " + event.getRawY());

		if (view.getId() != R.id.boardView)
		{
			gc.cl.p_hasPossibleMovesTo = false;
			gc.cl.p_possibleMoveToList.clear();
		}
		if (event.getAction() == MotionEvent.ACTION_DOWN)
		{

//			Log.i(TAG, "onTouch(), MotionEvent.ACTION_DOWN, id: " + view.getId());

			if (view.getId() == R.id.btn_1 | view.getId() == R.id.btn_2 | view.getId() == R.id.btn_3 | view.getId() == R.id.btn_4
					| view.getId() == R.id.btn_5 | view.getId() == R.id.btn_6 | view.getId() == R.id.btn_7)
				isDownBtn = true;
			else
				isDownBtn = false;

			downViewId = view.getId();
			lastTouchID = view.getId();
			downRawX = event.getRawX();
			downRawY = event.getRawY();
			touchTime = System.currentTimeMillis();
			if (view.getId() == R.id.msgMoves)
				scrlMsgMoves.setOnTouchListener(null);
		}

		if (event.getAction() == MotionEvent.ACTION_UP)
		{

//			Log.i(TAG, "onTouch(), MotionEvent.ACTION_UP, id: " + view.getId());

			lastTouchID = 0;

			upRawX = event.getRawX();
			upRawY = event.getRawY();
			if 	(		u.isViewInBounds(btn_1, (int) upRawX, (int) upRawY)
					|	u.isViewInBounds(btn_2, (int) upRawX, (int) upRawY)
					|	u.isViewInBounds(btn_3, (int) upRawX, (int) upRawY)
					|	u.isViewInBounds(btn_4, (int) upRawX, (int) upRawY)
					|	u.isViewInBounds(btn_5, (int) upRawX, (int) upRawY)
					|	u.isViewInBounds(btn_6, (int) upRawX, (int) upRawY)
					|	u.isViewInBounds(btn_7, (int) upRawX, (int) upRawY)
				)
				isUpBtn = true;
			else
				isUpBtn = false;
			if 	(		u.isViewInBounds(msgMoves, (int) upRawX, (int) upRawY)
					|	u.isViewInBounds(msgEngine, (int) upRawX, (int) upRawY)
				)
				isUpMsgView = true;
			else
				isUpMsgView = false;

//			Log.i(TAG, "onTouch(), isUpBtn: " + isUpBtn + ", isUpMsgView: " + isUpMsgView);
//			Log.i(TAG, "0 onTouch(), minScrollingWidth: " + minScrollingWidth + ", downRawX: " + downRawX + ", upRawX: " + upRawX);

			if (Math.abs(downRawX - upRawX) > minScrollingWidth | Math.abs(downRawY - upRawY) > minScrollingWidth)
			{
				boolean isBoardView = u.isViewInBounds(boardView, (int) upRawX, (int) upRawY);
				int[] loc = new int[2];
				btn_1.getLocationOnScreen(loc);
				int leftBtnBorder = loc[0] + (btn_1.getWidth() / 5);
				int topBtnBorder = loc[1] + (btn_1.getHeight() / 5);
				btn_7.getLocationOnScreen(loc);
				int rightBtnBorder = loc[0] + btn_7.getWidth() - (btn_7.getWidth() / 5);
				int bottomBtnBorder = loc[1] + btn_7.getHeight() - (btn_7.getHeight() / 5);

//				Log.i(TAG, "8 onTouch(), upRawX: " + upRawX + ", leftBtnBorder: " + leftBtnBorder + ", rightBtnBorder: " + rightBtnBorder);
//				Log.i(TAG, "9 onTouch(), upRawY: " + upRawY + ", topBtnBorder: " + topBtnBorder + ", bottomBtnBorder: " + bottomBtnBorder);

				if (view.getId() == R.id.boardView & userPrefs.getBoolean("user_options_gui_gameNavigationBoard", false))
				{
					cancelEngineMessage();
					if (upRawX > downRawX)							// move control
					{
						if (upRawX >= displayWidth - 40 | !isBoardView)
						{
							if (startVariation())
								c4aShowDialog(VARIATION_DIALOG);
							else
								nextMove(4, 0);    // last
						}
						else
							nextMove(2, 0);	// next
					}
					else
					{
						if (upRawX <= 40 | !isBoardView)
							nextMove(3, 0);	// start
						else
							nextMove(1, 0);	// back
					}
					if (!ec.chessEnginePaused)
					{
						if (!gc.cl.p_fen.equals(""))
							stopSearchAndContinue(EngineState.STOP_CONTINUE, gc.cl.p_fen, true);
					}
				}

//    			Log.i(TAG, "onTouch(), isDownBtn: " + isDownBtn + ", isUpBtn: " + isUpBtn);

				if (isDownBtn & isUpBtn)
				{
					if ((aspectRatio > 150 & upRawX > downRawX) | (aspectRatio <= 150 & upRawY > downRawY))			// game(file) control
					{
						if (view.getId() == R.id.btn_1 & u.isViewInBounds(btn_7, (int) upRawX, (int) upRawY))
						{
							if (fmPrefs.getInt("fm_location", 1) == 1)
							{
								if (!fmPrefs.getString("fm_query_white", "").equals(""))
									c4aShowDialog(QUERY_DIALOG);
								else
									startFileManager(LOAD_GAME_REQUEST_CODE, 0, 1);	// query
							}
						}
						else
						{
							if ((aspectRatio > 150 & upRawX >= rightBtnBorder) | (aspectRatio <= 150 & upRawY >= bottomBtnBorder))
							{
								startFileManager(LOAD_GAME_REQUEST_CODE, 0, 9);      // last
							}
							else
							{
								if (!gc.pgnStat.equals("L"))
								{
									startFileManager(LOAD_GAME_REQUEST_CODE, 0, 0);  // next
								}
							}
						}
					}
					else
					{
						if (view.getId() == R.id.btn_7 & u.isViewInBounds(btn_1, (int) upRawX, (int) upRawY))
						{
							startFileManager(LOAD_GAME_REQUEST_CODE, 0, 7);			// random
						}
						else
						{
							if ((aspectRatio > 150 & upRawX <= leftBtnBorder) | (aspectRatio <= 150 & upRawY <= topBtnBorder))
							{
								startFileManager(LOAD_GAME_REQUEST_CODE, 0, 1);       // first
							} else
							{
								if (!gc.pgnStat.equals("F"))
								{
									startFileManager(LOAD_GAME_PREVIOUS_CODE, 0, 8);   // previous
								}
							}
						}
					}
				}

				return true;
			}
			long diffTime = System.currentTimeMillis() - touchTime;
			switch (view.getId())
			{
				case R.id.boardView:
				case R.id.btn_1: case R.id.btn_2: case R.id.btn_3: case R.id.btn_4:
				case R.id.btn_5: case R.id.btn_6: case R.id.btn_7:
				case R.id.lblPlayerTimeA: case R.id.lblPlayerTimeB:
				case R.id.lblPlayerNameA: case R.id.lblPlayerNameB:
				case R.id.lblPlayerEloA:  case R.id.lblPlayerEloB:
				case R.id.msgMoves:
					if (diffTime < longTouchTime)
						onTouchAction(view, event);
					else
						onLongTouchAction(view);
					break;
			}
		}
		return true;
	}

	private void onTouchAction(View view, MotionEvent event)
	{

//		Log.i(TAG, "onTouchAction(), mod: " + ec.chessEnginePlayMod + ", paused: " + ec.chessEnginePaused + ", engineState: " + ec.getEngine().engineState);

		if (view.getId() == R.id.btn_1  | view.getId() == R.id.btn_3 | view.getId() == R.id.btn_4)
		{
			if (gc.isAutoPlay)
			{
				stopAutoPlay(false);
				return;
			}
		}

		boolean isPlayer = isPlayerMove(ec.chessEnginePlayMod, gc.getValueFromFen(gc.fen, 2));

		switch (view.getId())
		{
			case R.id.boardView:    // make a move on board
				cancelEngineMessage();
				startBoardMoveAction(event);
				break;
			case R.id.btn_1:    // play mode - new game
				isStopAutoPlay = false;
				removeDialog(PLAY_DIALOG);
				showDialog(PLAY_DIALOG);
				break;
			case R.id.btn_2:    // pause / start engine || clock (two players)

//				Log.i(TAG, "touch, btn_2, mod: " + ec.chessEnginePlayMod + ", paused: " + ec.chessEnginePaused + ", isPlayer: " + isPlayer + ", engineState: " + ec.getEngine().engineState + ", gameOver: " + isStateGameOver());

				if (gc.isAutoPlay)
				{
					startStopAutoPlay();
					isStopAutoPlay = true;
				}
				else
				{
					if (ec.chessEnginePlayMod == 5 || ec.chessEnginePlayMod == 6)    // two players  ||  edit
					{
						stopComputerThinking(false, false);
						setEnginePausePlayBtn(null, null);
						if (ec.chessEnginePlayMod == 5) {
							if (tc.clockIsRunning)    // two players
								stopChessClock();
							else
								startChessClock();
						}
						setInfoMessage(getEngineThinkingMessage(), null, null);
					}
					else {
						if (!ec.chessEnginePaused) {
							pauseStopPlay(false);
						}
						else
						{

							if (isStateGameOver()) {
								msgEngine.setVisibility(TextView.GONE);
								messageInfo 		= "";
								messageEngine = new SpannableStringBuilder("");
								messageEngineShort  = "";
								return;
							}

							if (ec.getEngine() == null) {
								showDialog(PLAY_DIALOG);
								return;
							}

							setEnginePausePlayBtn(true, null);
							setPlayModPrefs(ec.chessEnginePlayMod);
							ec.chessEnginePaused = false;
							ec.chessEngineInit = false;
							updateCurrentPosition("");

//								Log.i(TAG, "2 onTouchAction(), R.id.btn_2, gc.fen: " + gc.fen + ", isStateGameOver(): " + isStateGameOver() + ", ec.chessEnginePaused: " + ec.chessEnginePaused);

							if (isPlayer & ec.getEngine().engineState != EngineState.PONDER) {
								startChessClock();
								setInfoMessage(getString(R.string.player_move), null, null);
							}
							else
								stopSearchAndContinue(EngineState.STOP_CONTINUE, gc.fen, true);

						}
					}
				}
				break;
			case R.id.btn_3:
				if (isStopAutoPlay)
				{
					startStopAutoPlay();
					isStopAutoPlay = false;
				}
				else
				{

//					Log.i(TAG, "touch, btn_3, mod: " + ec.chessEnginePlayMod + ", paused: " + ec.chessEnginePaused + ", isPlayer: " + isPlayer + ", engineState: " + ec.getEngine().engineState + ", gameOver: " + isStateGameOver());

					if (ec.chessEnginePlayMod == 4) {
						if (ec.uciEngines[ec.analysisBestScoreEngineId] != null) {
							if (ec.chessEnginePlayMod == 4 && userPrefs.getBoolean("user_play_multipleEngines", true) && withMultiEngineAnalyse)
								ec.setCurrentEngineId(ec.analysisBestScoreEngineId);
						}
					}

					if (ec.getEngine() != null) {
						if (!isStateGameOver() && ec.getEngine().engineSearching()) {
							ec.setStartPlay(ec.currentEngineId, gc.getValueFromFen(gc.fen, 2));
							if (isPlayer & ec.getEngine().engineState != EngineState.PONDER) {
								setInfoMessage(getString(R.string.player_move), null, null);
							} else
								startForceComputerMove();
						}
					}

				}
				break;
			case R.id.btn_4:    // delete move(s)

//				Log.i(TAG, "touch, btn_4, mod: " + ec.chessEnginePlayMod + ", paused: " + ec.chessEnginePaused + ", isPlayer: " + isPlayer + ", engineState: " + ec.getEngine().engineState + ", gameOver: " + isStateGameOver());

				isStopAutoPlay = false;
				cancelEngineMessage();
				deleteMoves(true);
				break;
			case R.id.btn_5:    // turn board
				startTurnBoard();
				break;
			case R.id.btn_6:    // move back

//				Log.i(TAG, "touch, btn_6, mod: " + ec.chessEnginePlayMod + ", paused: " + ec.chessEnginePaused + ", isPlayer: " + isPlayer + ", engineState: " + ec.getEngine().engineState + ", gameOver: " + isStateGameOver());

				cancelEngineMessage();
				nextMove(1, 0);
				if (ec.getEngine() != null) {
					if (ec.getEngine().engineState == EngineState.PONDER)
						engineStopPonder(gc.cl.p_fen, ec.chessEnginePlayMod);
					else {
						if (!ec.chessEnginePaused) {
							if (ec.chessEnginePlayMod <= 3)
								pauseStopPlay(false);
							else {
								if (!gc.cl.p_fen.equals(""))
									stopSearchAndContinue(EngineState.STOP_CONTINUE, gc.cl.p_fen, true);
							}
						}
					}
				}

				break;
			case R.id.btn_7:    // next move

//				Log.i(TAG, "touch, btn_7, mod: " + ec.chessEnginePlayMod + ", paused: " + ec.chessEnginePaused + ", isPlayer: " + isPlayer + ", engineState: " + ec.getEngine().engineState + ", gameOver: " + isStateGameOver());

				if (isStateGameOver()) {
					msgEngine.setVisibility(TextView.GONE);
					messageInfo 		= "";
					messageEngine = new SpannableStringBuilder("");
					messageEngineShort  = "";
					return;
				}

				cancelEngineMessage();
				nextMove(2, 0);
				if (ec.getEngine() != null) {
					if (ec.getEngine().engineState == EngineState.PONDER)
						engineStopPonder(gc.cl.p_fen, ec.chessEnginePlayMod);
					else {
						if (!ec.chessEnginePaused) {
							if (ec.chessEnginePlayMod <= 3)
								pauseStopPlay(false);
							else {
								if (!gc.cl.p_fen.equals(""))
									stopSearchAndContinue(EngineState.STOP_CONTINUE, gc.cl.p_fen, true);
							}
						}
					}
				}
				break;
			case R.id.lblPlayerTimeB:	// set time white
				if (ec.chessEnginePlayMod != 4)
				{
					if (!gc.isBoardTurn)
						setTimeWhiteBlack(1);
					else
						setTimeWhiteBlack(2);
				}
				break;
			case R.id.lblPlayerTimeA:    // set time black
				if (ec.chessEnginePlayMod != 4)
				{
					if (!gc.isBoardTurn)
						setTimeWhiteBlack(2);
					else
						setTimeWhiteBlack(1);
				}
				break;
			case R.id.lblPlayerNameA:    // edit game data
			case R.id.lblPlayerNameB:
			case R.id.lblPlayerEloA:
			case R.id.lblPlayerEloB:
				startGameData();
				break;
			case R.id.msgMoves:
				startMsgMoveAction(view, event);
				break;
		}
	}

	private void onLongTouchAction(View view)
	{
		if (ec.getEngine() != null) {
			if (ec.getEngine().engineState == EngineState.PONDER) {
				setPauseEnginePlay(false);
				initPonder();
			}
		}
		switch (view.getId())
		{
			case R.id.boardView:    // board menu
				removeDialog(MENU_BOARD_DIALOG);
				showDialog(MENU_BOARD_DIALOG);
				break;
			case R.id.btn_1:    // play options (???)
				stopComputerThinking(false, false);
				startActivityForResult(optionsSettingsIntent, OPTIONS_SETTINGS);
				break;
			case R.id.btn_2:	// computer settings
				startEditUciOptions(runP.getString("run_engineProcess", MainActivity.OEX_DEFAULT_ENGINE_SINGLE));
				break;
			case R.id.btn_3:    // auto play
				startStopAutoPlay();
				break;
			case R.id.btn_4:    //
				getUndoPgn();
				break;
			case R.id.btn_5:    //

				break;
			case R.id.btn_6:    // first move (initial position)
				cancelEngineMessage();
				gc.isGameOver = false;
				nextMove(3, 0);
				if (!ec.chessEnginePaused)
				{
					if (ec.chessEnginePlayMod <= 3)
						pauseStopPlay(false);
					else {
						if (!gc.cl.p_fen.equals(""))
							stopSearchAndContinue(EngineState.STOP_CONTINUE, gc.cl.p_fen, true);
					}
				}
				break;
			case R.id.btn_7:    // last move
				cancelEngineMessage();
				if (startVariation())
					c4aShowDialog(VARIATION_DIALOG);
				else
				{
					nextMove(4, 0);

					if (isStateGameOver()) {
						return;
					}

					if (!ec.chessEnginePaused)
					{
						if (ec.chessEnginePlayMod <= 3)
							pauseStopPlay(false);
						else {
							if (!gc.cl.p_fen.equals(""))
								stopSearchAndContinue(EngineState.STOP_CONTINUE, gc.cl.p_fen, true);
						}
					}
				}
				break;

			case R.id.lblPlayerTimeA:    // time control
			case R.id.lblPlayerTimeB:
				stopSearchAndContinue(EngineState.STOP_IDLE, "", true);
				startActivityForResult(optionsTimeControlIntent, OPTIONS_TIME_CONTROL_REQUEST_CODE);
				break;
			case R.id.lblPlayerNameA:    // edit game data
			case R.id.lblPlayerNameB:
			case R.id.lblPlayerEloA:
			case R.id.lblPlayerEloB:
				startGameData();
				break;
			case R.id.msgMoves:
				if (startVariation())
					c4aShowDialog(VARIATION_DIALOG);
				break;
		}
	}

//	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
	{

//		Log.i(TAG, "1 onActivityResult, requestCode: " + requestCode + ", " + "resultCode: " + resultCode);
//		Log.i(TAG, "1 onActivityResult, user_play_playMod: " + requestCode + ", " + userPrefs.getInt("user_play_playMod", 1));
//		Log.i(TAG, "1 onActivityResult, data: " + data);

		updateCurrentPosition("");
		SharedPreferences.Editor ed = userPrefs.edit();
		boolean isNewGame = false;
		if (data != null)
			isNewGame = data.getBooleanExtra("newGame", false);

//		Log.i(TAG, "2 onActivityResult, requestCode: " + requestCode + ", " + "resultCode: " + resultCode);

		switch(requestCode) {
			case OPTIONS_SETTINGS:

				if (restartPlayDialog) {
					dRestartEngine = true;
					showDialog(PLAY_DIALOG);
				}

				if (resultCode == 3) {    // set playOption and play

					msgMoves.setTextSize(userPrefs.getInt("user_options_gui_fontSize", Settings.FONTSIZE_MEDIUM));
					msgShort.setTextSize(userPrefs.getInt("user_options_gui_fontSize", Settings.FONTSIZE_MEDIUM));
					msgShort2.setTextSize(userPrefs.getInt("user_options_gui_fontSize", Settings.FONTSIZE_MEDIUM));
					msgEngine.setTextSize(userPrefs.getInt("user_options_gui_fontSize", Settings.FONTSIZE_MEDIUM));

					initColors();
					boardView.setColor();
					updateGui();
					gc.isGameLoaded = false;
					msgEngine.setVisibility(TextView.GONE);
					messageInfo = "";
					messageEngine = new SpannableStringBuilder("");
					messageEngineShort = "";
					ec.chessEngineMatch = false;
					if (ec.getEngine() != null) {
						ec.getEngine().isLogOn = userPrefs.getBoolean("user_options_enginePlay_logOn", false);
					}
					ec.setBookOptions();
					msgEngine.setMaxLines(getMsgEngineLines());
					msgEngine.setLines(getMsgEngineLines());
					switch (ec.chessEnginePlayMod) {
						case 1:     // white
						case 2:     // black
						case 3:     // engine vs engine
						case 4:     // analysis

//							Log.i(TAG, "onActivityResult, OPTIONS_ENGINE_PLAY_REQUEST_CODE, requestCode: " + requestCode + ", " + "resultCode: " + resultCode);

							stopSearchAndRestart(false, true);
							break;
						case 5:     // two players
						case 6:     // edit
							analysisMessage = "";
							startEdit(isNewGame, true);
							break;
					}
				}
				break;
			case LOAD_GAME_REQUEST_CODE:
			case LOAD_GAME_PREVIOUS_CODE:
			case SAVE_LOAD_GAME_REQUEST_CODE:
			case SAVE_OK_LOAD_GAME_REQUEST_CODE:
			case SAVE_ERROR_LOAD_GAME_REQUEST_CODE:
				initChessClock(tc, userPrefs.getInt("user_options_timeControl", 1), ec.chessEnginePlayMod);
				gc.isAutoLoad = false;
				gc.isPlayerPlayer = false;
				gc.pgnStat = "-";
				if (resultCode == RESULT_OK) {

					stopSearchAndContinue(EngineState.STOP_IDLE, "", true);

					gc.isGameOver = false;
					gc.isGameUpdated = true;
					ec.chessEngineMatch = false;
					setPauseValues(false, "", 4, "");
					if (requestCode == LOAD_GAME_REQUEST_CODE | requestCode == LOAD_GAME_PREVIOUS_CODE) {
						messageInfo = "";
						messageEngine = new SpannableStringBuilder("");
						messageEngineShort = "";
					}
					gc.pgnStat = data.getStringExtra("pgnStat");
					if (requestCode == SAVE_OK_LOAD_GAME_REQUEST_CODE
							& userPrefs.getBoolean("user_batch_ma_counterOn", true)) {
						ed.putInt("user_batch_ma_gameCounter", userPrefs.getInt("user_batch_ma_gameCounter", 1) + 1);
						ed.commit();
					}
					displayMoves = null;
					getGameData(data.getStringExtra("fileBase"), data.getStringExtra("filePath"), data.getStringExtra("fileName"),
							data.getStringExtra("fileData"), false, getIsEndPosition(), 0, true);
					gc.fileBase = gc.cl.history.getFileBase();
					gc.filePath = gc.cl.history.getFilePath();
					gc.fileName = gc.cl.history.getFileName();
					if (gc.cl.p_chess960ID == 518)
						gc.isChess960 = false;
					else
						gc.isChess960 = true;
					gc.startFen = gc.cl.history.getStartFen();
					setRunMoveHistory();
					setRunPrefs();
					setInfoMessage("", null, "");
					ec.chessEnginePaused = true;
					gc.isGameLoaded = true;
					setInfoMessage("", null, null);
				}
				break;
			case GAME_DATA_REQUEST_CODE:
				if (resultCode == RESULT_OK)
					gc.cl.history.setNewGameTags(data.getCharSequenceExtra("gameTags").toString());
				setInfoMessage("", null, "");
				break;
			case NOTATION_REQUEST_CODE:
				setInfoMessage("", null, "");
				updateCurrentPosition("");
				break;
			case OPTIONS_COLOR_SETTINGS:
				initColors();
				boardView.setColor();
				updateGui();
				break;
			case OPTIONS_CHESSBOARD_REQUEST_CODE:
			case OPTIONS_GUI_REQUEST_CODE:
				if (requestCode == OPTIONS_GUI_REQUEST_CODE) {
					useWakeLock = !userPrefs.getBoolean("user_options_gui_disableScreenTimeout", false);
					setWakeLock(useWakeLock);
					setPieceName(userPrefs.getInt("user_options_gui_PieceNameId", 0));
					setInfoMessage("", null, "");
				}
				u.updateFullscreenStatus(this, userPrefs.getBoolean("user_options_gui_StatusBar", false));
				updateCurrentPosition("");
				break;
			case OPTIONS_TIME_CONTROL_REQUEST_CODE:

//				Log.i(TAG, "onActivityResult, OPTIONS_TIME_CONTROL_REQUEST_CODE" + ", resultCode: " + resultCode);

				if (resultCode == RESULT_OK)
				{
					if (restartPlayDialog) {
						initChessClock(tc, userPrefs.getInt("user_options_timeControl", 1), ec.chessEnginePlayMod);
						updateGui();
						dRestartEngine = true;
						showDialog(PLAY_DIALOG);
					}
					else
					{
						ec.chessEngineInit = false;
						stopChessClock();
						initChessClock(tc, userPrefs.getInt("user_options_timeControl", 1), ec.chessEnginePlayMod);
						stopSearchAndRestart(false, true);
					}
				}
				else {
					if (restartPlayDialog)
						showDialog(PLAY_DIALOG);
				}
				break;
			case EDIT_CHESSBOARD_REQUEST_CODE:

//				Log.i(TAG, "onActivityResult, EDIT_CHESSBOARD_REQUEST_CODE" + ", resultCode: " + resultCode);

				if (resultCode == RESULT_OK) {
					gc.isBoardTurn = runP.getBoolean("run_game0_is_board_turn", false);
					gc.isGameLoaded = false;
					messageEngine = new SpannableStringBuilder("");
					messageEngineShort = "";
					gc.errorMessage = "";
					gc.errorPGN = "";
					ec.chessEngineMatch = false;
					CharSequence chess960Id = data.getStringExtra("chess960Id");
					CharSequence fen = data.getStringExtra("newFen");
					if (!chess960Id.equals("518"))
						fen = "";
					gc.cl.newPosition(chess960Id, fen, "", "", "", "", "", "");
					if (gc.cl.p_stat.equals("1")) {
						gc.isGameOver = false;
						gc.isGameUpdated = true;
						gc.fen = gc.cl.p_fen;
						if (gc.cl.p_chess960ID == 518)
							gc.isChess960 = false;
						else
							gc.isChess960 = true;
						ed.putInt("user_game_chess960Id", gc.cl.p_chess960ID);
						ed.commit();
						setInfoMessage("", null, "");
						updateGui();
						stopSearchAndRestart(false, true);
					}
				}
				break;
			case MOVETEXT_REQUEST_CODE:
				if (resultCode == RESULT_OK) {
					gc.isGameUpdated = false;
					gc.cl.history.setMoveText(data.getStringExtra("text"));
					updateCurrentPosition("");
				}
				break;
			case SAVE_GAME_REQUEST_CODE:

//				Log.i(TAG, "onActivityResult(), SAVE_GAME_REQUEST_CODE, resultCode: " + resultCode);

				if (resultCode == RESULT_OK) {
					gc.isGameUpdated = true;
					gc.fileBase = data.getStringExtra("fileBase");
					gc.filePath = data.getStringExtra("filePath");
					gc.fileName = data.getStringExtra("fileName");
				}
				if (resultCode == 22)                    // chessEngineAutoPlay | writeNewFile
					startEngineMatch();
				else
					updateCurrentPosition("");
				break;
			case ENGINE_SETTING_REQUEST_CODE:
				stopSearchAndRestart(false, false);
				break;
			case EDIT_UCI_OPTIONS:
				if (restartPlayDialog) {
					dRestartEngine = true;
					showDialog(PLAY_DIALOG);
				}
				if (resultCode == RESULT_OK) {
					FileIO f = new FileIO(this);;
					f.dataToFile(f.getUciExternalPath(), engine.uciFileName, data.getStringExtra("uciOptsChanged"), false);
				}
				break;
			case COMPUTER_MATCH:
				if (resultCode == ComputerMatch.ACTION_MATCH) {
					ec.chessEnginePlayMod = 3;
					if (restartPlayDialog) {
						dRestartEngine = true;
						removeDialog(PLAY_DIALOG);
						gc.isGameLoaded = false;
						ec.chessEngineMatch = true;
						ec.chessEnginePaused = false;
						restartSearchCnt = 0;
						setPlayModPrefs(ec.chessEnginePlayMod);
						initChessClock(tc, userPrefs.getInt("user_options_timeControl", 1), ec.chessEnginePlayMod);
						gc.startPgn = gc.cl.history.createPgnFromHistory(1);
						gc.startMoveIdx = gc.cl.history.getMoveIdx();
						if (userPrefs.getBoolean("user_play_eve_autoCurrentGame", false) && !gc.isChess960) {
							if (userPrefs.getInt("user_play_eve_round", 0) == 0) {
								ed.putString("user_play_eve_fen", (String) gc.cl.p_fen);
							}
						}
						else {
							ed.putBoolean("user_play_eve_autoCurrentGame", false);
							ed.putString("user_play_eve_fen", "");
						}
						ed.apply();
						startEngineMatch();
					}
				}
				if (resultCode == ComputerMatch.ACTION_APPLY) {
					ec.chessEnginePlayMod = 3;
					if (restartPlayDialog) {

//						Log.i(TAG, "onActivityResult(), ComputerMatch.ACTION_APPLY, resultCode: " + resultCode);

						dRestartEngine = true;
						removeDialog(PLAY_DIALOG);
						showDialog(PLAY_DIALOG);

					}
				}
				break;
			case ANALYSIS_OPTIONS:
				if (resultCode == RESULT_OK) {
					ec.chessEnginePlayMod = 4;
					if (restartPlayDialog) {
						dRestartEngine = true;
						removeDialog(PLAY_DIALOG);

//						Log.i(TAG, "onActivityResult(), ANALYSIS_OPTIONS, ec.chessEnginePaused: " + ec.chessEnginePaused);

						if (!ec.chessEnginePaused)
							pauseStopPlay(false);
						else {
//							stopSearchAndContinue(EngineState.STOP_CONTINUE, gc.fen, true);
							dRestartEngine = false;
							stopMultiEnginesAndRestart();
							setInfoMessage(getString(R.string.engineInit), null, null);
						}
					}
				}
				break;

		}

		if (progressDialog != null)
		{
			if (progressDialog.isShowing())
				dismissDialog(FILE_LOAD_PROGRESS_DIALOG);
		}
	}

	public synchronized final void setWakeLock(boolean enableLock) 
	{
        WakeLock wl = wakeLock;
        if (wl != null) 
        {
            if (wl.isHeld())
                wl.release();
            if (enableLock)
                wl.acquire();
        }
    }

	public void getPermissions()
	{

//		Log.i(TAG, "1 getPermissions(), storagePermissions: " + storagePermission);

		if (storagePermission == PermissionState.UNKNOWN) {
			String extStorage = Manifest.permission.WRITE_EXTERNAL_STORAGE;
			if (ContextCompat.checkSelfPermission(this, extStorage) ==
					PackageManager.PERMISSION_GRANTED) {
				storagePermission = PermissionState.GRANTED;

//				Log.i(TAG, "2 getPermissions(), storagePermissions: " + storagePermission);

			} else {
				storagePermission = PermissionState.REQUESTED;

//				Log.i(TAG, "3 getPermissions(), storagePermissions: " + storagePermission);

				ActivityCompat.requestPermissions(this, new String[]{extStorage}, PERMISSIONS_REQUEST_CODE);
			}
		}
		if (internetPermission == PermissionState.UNKNOWN) {
			String extStorage = Manifest.permission.INTERNET;
			if (ContextCompat.checkSelfPermission(this, extStorage) ==
					PackageManager.PERMISSION_GRANTED) {
				internetPermission = PermissionState.GRANTED;
			} else {
				internetPermission = PermissionState.REQUESTED;
				ActivityCompat.requestPermissions(this, new String[]{extStorage}, PERMISSIONS_REQUEST_CODE);
			}
		}
		if (wakeLockPermission == PermissionState.UNKNOWN) {
			String extStorage = Manifest.permission.WAKE_LOCK;
			if (ContextCompat.checkSelfPermission(this, extStorage) ==
					PackageManager.PERMISSION_GRANTED) {
				wakeLockPermission = PermissionState.GRANTED;
			} else {
				wakeLockPermission = PermissionState.REQUESTED;
				ActivityCompat.requestPermissions(this, new String[]{extStorage}, PERMISSIONS_REQUEST_CODE);
			}
		}

//		Log.i(TAG, "getPermissions(), storagePermissions: " + storagePermission);
//		Log.i(TAG, "getPermissions(), internetPermission: " + internetPermission);
//		Log.i(TAG, "getPermissions(), wakeLockPermission: " + wakeLockPermission);

	}

	//	PREFERENCES		PREFERENCES		PREFERENCES		PREFERENCES		PREFERENCES
	public void setRunPrefs()
	{
    	SharedPreferences.Editor ed = runP.edit();

//		Log.i(TAG, "setRunPrefs(), twoPlayerPaused: " + twoPlayerPaused);

		if (ec.getEngine() != null) {
			if (!ec.getEngine().engineProcess.equals(""))
				ed.putString("run_engineProcess", ec.getEngine().engineProcess);
		}
		ed.putString("run_pgnStat", (String) gc.pgnStat);
		ed.putInt("run_game0_move_idx", gc.cl.history.getMoveIdx());
		CharSequence pgn = "";
		try {pgn = gc.cl.history.createPgnFromHistory(1);}
		catch (ArrayIndexOutOfBoundsException e) {e.printStackTrace();}
		ed.putString("run_game0_pgn", pgn.toString());
		ed.putBoolean("run_game0_is_board_turn", gc.isBoardTurn);
		ed.putBoolean("run_game0_is_updated", gc.isGameUpdated);
		ed.putBoolean("run_isGameLoaded", gc.isGameLoaded);
		ed.putBoolean("run_isAutoPlay", gc.isAutoPlay);
		ed.putString("run_game0_file_base", gc.fileBase.toString());
		ed.putString("run_game0_file_path", gc.filePath.toString());
		ed.putString("run_game0_file_name", gc.fileName.toString());
    	ed.putInt("run_gridViewSize", gridViewSize);
    	ed.putBoolean("run_chessEnginePaused", ec.chessEnginePaused);
		ed.putBoolean("run_chessEngineSearching", ec.chessEngineSearching);
		ed.putBoolean("run_chessEngineAutoRun", ec.chessEngineMatch);
		ed.putBoolean("run_twoPlayerPaused", twoPlayerPaused);
		ed.putString("run_selectedVariationTitle", (String) gc.selectedVariationTitle);

		ed.commit();

		setRunPrefsTime();

	}

	public void setRunPrefsTime()
	{

//		Log.i(TAG, "setRunPrefsTime(), tc.timeControl: " + tc.timeControl + ", tc.timeWhite: " + tc.timeWhite + ", tc.timeBlack: " + tc.timeBlack);

		SharedPreferences.Editor ed = runP.edit();
		ed.putInt("run_timeControl", tc.timeControl);
		ed.putInt("run_timeWhite", tc.timeWhite);
		ed.putInt("run_timeBlack", tc.timeBlack);
		ed.putInt("run_movesToGo", tc.movesToGo);
		ed.putInt("run_bonusWhite", tc.bonusWhite);
		ed.putInt("run_bonusBlack", tc.bonusBlack);
		ed.commit();
	}

    public void setRunMoveHistory() 
	{
    	SharedPreferences.Editor edM = moveHistoryP.edit();
		if (isAppEnd & gc.cl.history.moveHistory.size() > 2)
		{
			StringBuilder sb = new StringBuilder(150000);
			for (int i = 0; i < gc.cl.history.moveHistory.size(); i++)
			{
			    sb.append(gc.cl.history.moveHistory.get(i));
			    if (i != gc.cl.history.moveHistory.size() -1)
			    	sb.append("\n");
			}
			edM.putString("run_moveHistory", sb.toString());
		}
		else
			edM.putString("run_moveHistory", "");
		edM.commit();
	}
    public void getRunPrefs()
	{
	    if (runP.getBoolean("run_set_stockfish_10", true))
        {
            SharedPreferences.Editor ed = runP.edit();
            ed.putBoolean("run_set_stockfish_10", false);
            ed.putString("run_engineProcess", MainActivity.OEX_DEFAULT_ENGINE_SINGLE);
            ed.commit();
        }
    	gridViewSize = runP.getInt("run_gridViewSize", 464);
        gc.pgnStat = runP.getString("run_pgnStat", "-");
        gc.startPgn = runP.getString("run_game0_pgn", "");
        gc.startMoveIdx = runP.getInt("run_game0_move_idx", 0);
        gc.isBoardTurn = runP.getBoolean("run_game0_is_board_turn", false);
        gc.isGameUpdated = runP.getBoolean("run_game0_is_updated", true);
        gc.isGameLoaded = runP.getBoolean("run_isGameLoaded", false);
        gc.isAutoPlay = runP.getBoolean("run_isAutoPlay", false);
        gc.fileBase = runP.getString("run_game0_file_base", "");
        gc.filePath = runP.getString("run_game0_file_path", "");
        gc.fileName = runP.getString("run_game0_file_name", "");
        ec.chessEnginePaused = runP.getBoolean("run_chessEnginePaused", false);
        ec.chessEngineSearching = runP.getBoolean("run_chessEngineSearching", false);
        ec.chessEngineMatch = runP.getBoolean("run_chessEngineAutoRun", false);
		twoPlayerPaused = runP.getBoolean("run_twoPlayerPaused", false);
		ec.chessEnginePlayMod = userPrefs.getInt("user_play_playMod", 1);
        gc.selectedVariationTitle = runP.getString("run_selectedVariationTitle", "");
		getRunPrefsTime();
	}

	public void getRunPrefsTime()
	{
		tc.timeControl = runP.getInt("run_timeControl", 1);
		tc.timeWhite = runP.getInt("run_timeWhite", OptionsTimeControl.TIME_PLAYER_CLOCK);
		tc.timeBlack = runP.getInt("run_timeBlack", OptionsTimeControl.TIME_ENGINE_CLOCK);
		tc.movesToGo = runP.getInt("run_movesToGo", 0);
		tc.bonusWhite = runP.getInt("run_bonusWhite", OptionsTimeControl.TIME_PLAYER_BONUS);
		tc.bonusBlack = runP.getInt("run_bonusBlack", OptionsTimeControl.TIME_ENGINE_BONUS);
		tc.initChessClock(tc.timeControl, tc.timeWhite, tc.timeBlack, tc.movesToGo, tc.bonusWhite, tc.bonusBlack);

//		Log.i(TAG, "getRunPrefsTime(), tc.timeControl: " + tc.timeControl + ", tc.timeWhite: " + tc.timeWhite + ", tc.timeBlack: " + tc.timeBlack);

		tc.setCurrentShowValues(ec.chessEnginePlayMod);
	}

	public void startEngineMatch()
	{
		SharedPreferences.Editor ed = userPrefs.edit();
		SharedPreferences.Editor edR = runP.edit();
		int games = userPrefs.getInt("user_play_eve_games", 4);
		int gameRound = userPrefs.getInt("user_play_eve_round", 0);
		gameRound++;
		if (userPrefs.getBoolean("user_play_eve_engineVsEngine", true)) {
			edR.putString("run_engineListMatch", userPrefs.getString("user_play_eve_white", "") + "|" + userPrefs.getString("user_play_eve_black", ""));
			if((gameRound%2) == 0 && userPrefs.getBoolean("user_play_eve_autoFlipColor", true))
				edR.putString("run_engineListMatch", userPrefs.getString("user_play_eve_black", "") + "|" + userPrefs.getString("user_play_eve_white", ""));
			edR.apply();
		}
		if (gc.isChess960) {
			if (!userPrefs.getBoolean("user_play_eve_engineVsEngine", true) || gameRound % 2 != 0)
				initPosition(false);
		}

//		Log.i(TAG, "startEngineAutoplay(), file: " + gc.filePath + gc.fileName + ", gameRound: " + gameRound + "; games: " + games);

		if (gameRound <= games && !ec.chessEngineMatchFinishGame) {
			ed.putInt("user_play_eve_round", gameRound);
			ed.apply();
			ec.chessEngineRound = gameRound	+ "." + games;
			ec.chessEngineEvent = userPrefs.getString("user_play_eve_event", "?");
			ec.chessEngineSite = userPrefs.getString("user_play_eve_site", "?");
			if (!userPrefs.getString("user_play_eve_fen", "").equals(""))
				ec.chessEngineFen = userPrefs.getString("user_play_eve_fen", "");
			else
				ec.chessEngineFen = "";
			cntChess960 = 0;
			nextGameEngineAutoPlay();
		}
		else
		{
			stopSearchAndContinue(EngineState.STOP_IDLE, "", true);
			stopChessClock();
			ec.chessEngineMatch = false;
			ec.chessEngineMatchFinishGame = false;
			edR.putBoolean("run_chessEngineAutoRun", false);
			edR.apply();
			setMatchResultToElo();
			if (!ec.chessEngineMatch)
				playSound(3, 0);

//			Log.i(TAG, "startEngineMatch(), end of match");

			messageEngine = new SpannableStringBuilder("");
			String engineMatch = runP.getString("run_engineProcess", MainActivity.OEX_DEFAULT_ENGINE_SINGLE);
			if (userPrefs.getBoolean("user_play_eve_engineVsEngine", true)) {
				engineMatch = userPrefs.getString("user_play_eve_white", "") + " / " + userPrefs.getString("user_play_eve_black", "");
			}
			String matchId = "M" + userPrefs.getInt("user_play_eve_matchId", 1);
			if (ec.chessEngineMatchFinishGame)
				matchId = "E M" + userPrefs.getInt("user_play_eve_matchId", 1);
			String gameId = "#" + userPrefs.getInt("user_play_eve_round", 0) + "(" + userPrefs.getInt("user_play_eve_games", 4) + ")";
			String engineData = "..." + userPrefs.getString("user_play_eve_file", "") + ", " + matchId + ", " + gameId;
			String engineResult = getString(R.string.result) + ": " + userPrefs.getString("user_play_eve_result", "0-0");

			SpannableStringBuilder chs = new SpannableStringBuilder();
			chs.append(engineMatch + "\n" + engineData + "\n" + engineResult + "\n" + getString(R.string.matchEnd));
			setInfoMessage(null, chs, null);

		}
	}

	public void nextGameEngineAutoPlay()
	{

//		Log.i(TAG, "nextGameEngineAutoPlay(), file: " + gc.filePath + gc.fileName);

		gc.isGameOver = false;
		gc.isGameUpdated = true;
		ec.chessEnginePaused = false;
		ec.setPlaySettings(userPrefs, gc.cl.p_color);

		if 	(!userPrefs.getBoolean("user_play_eve_autoCurrentGame", false))
		{
			getGameData(gc.fileBase, gc.filePath, gc.fileName, "", false, false, gc.startMoveIdx, true);
			stopSearchAndRestart(true, true);
		}
		else
		{
			String fen = userPrefs.getString("user_play_eve_fen", "");

//			Log.i(TAG, "nextGameEngineAutoPlay(), fen: " + fen);

			gc.cl.newPosition("518", fen, "", "", "", "", "", "");
			if (gc.cl.p_stat.equals("1")) {
				gc.isGameOver = false;
				gc.isGameUpdated = true;
				gc.fen = gc.cl.p_fen;
				if (gc.cl.p_chess960ID == 518)
					gc.isChess960 = false;
				else
					gc.isChess960 = true;
				setInfoMessage("", null, "");
				updateGui();
				stopSearchAndRestart(false, true);
			}
		}
	}

	public void setMoveTime()
	{

//		Log.i(TAG, "setMoveTime(), tc.timeControl: " + tc.timeControl);

		if (tc.timeControl == 2)
		{
			if (ec.chessEnginePlayMod == 1)
			{
				tc.timeWhite = userPrefs.getInt("user_time_player_move", OptionsTimeControl.TIME_PLAYER_MOVE);
				tc.timeBlack = userPrefs.getInt("user_time_engine_move", OptionsTimeControl.TIME_ENGINE_MOVE);
			}
			if (ec.chessEnginePlayMod == 2)
			{
				tc.timeWhite = userPrefs.getInt("user_time_engine_move", OptionsTimeControl.TIME_ENGINE_MOVE);
				tc.timeBlack = userPrefs.getInt("user_time_player_move", OptionsTimeControl.TIME_PLAYER_MOVE);
			}
			if (ec.chessEnginePlayMod == 3)
			{
				tc.timeWhite = userPrefs.getInt("user_time_engine_move", OptionsTimeControl.TIME_ENGINE_MOVE);
				tc.timeBlack = userPrefs.getInt("user_time_engine_move", OptionsTimeControl.TIME_ENGINE_MOVE);
			}
		}
	}

	public void setPauseEnginePlay(boolean shutDown)
	{

//		Log.i(TAG, "setPauseEnginePlay, ec.chessEnginePaused: " + ec.chessEnginePaused);

		if (shutDown)
			stopSearchAndContinue(EngineState.STOP_QUIT, "", true);
		else
		{
			stopSearchAndContinue(EngineState.STOP_IDLE, "", true);
			ec.chessEnginePaused = true;
			if (!gc.isGameOver & !gc.cl.p_variationEnd)
				setInfoMessage(getEnginePausedMessage(), null, null);
		}
	}

	public void startStopEnginePlay(int engineAction)
	{	// start/stop engine(button)

//		Log.i(TAG, "startStopEnginePlay, ec.chessEnginePaused: " + ec.chessEnginePaused + ", engineAction: " + engineAction);

		if (ec.getEngine() == null) {
			return;
		}

		if (ec.chessEnginePaused && !ec.getEngine().engineSearching())
		{
			startChessClock();
			ec.chessEnginePaused = false;
			ec.chessEngineSearching = false;
			if (ec.chessEnginePlayMod == 3 | ec.chessEnginePlayMod == 4)
				ec.chessEngineSearching = true;
			if (ec.chessEnginePlayMod == 1 & gc.cl.p_color.equals("b"))
				ec.chessEngineSearching = true;
			if (ec.chessEnginePlayMod == 2 & gc.cl.p_color.equals("w"))
				ec.chessEngineSearching = true;
			updateCurrentPosition("");
			isGoPonder = false;
			if (ec.chessEngineSearching)
				chessEngineSearch(ec.currentEngineId, gc.cl.p_fen, "", "");
		}
		else
		{
			if (ec.chessEnginePlayMod <= 4)
			{

//				Log.i(TAG, "2 startStopEnginePlay, ec.chessEnginePaused: " + ec.chessEnginePaused + ", engineAction: " + engineAction);

				switch (engineAction)
				{
					case 0:		// stop engine

//						Log.i(TAG, "pauseEnginePlay(), syncStopSearch(), eState: STOP_IDLE");

						stopSearchAndContinue(EngineState.STOP_IDLE, "", true);
						break;
					case 1:		// stop engine and make best move

//						Log.i(TAG, "pauseEnginePlay(), syncStopSearch(), eState: STOP_MOVE");

						stopSearchAndContinue(EngineState.STOP_MOVE, "", true);
						break;
					case 2:		// stop engine, make best move and continue engine search

//						Log.i(TAG, "pauseEnginePlay(), syncStopSearch(), eState: STOP_MOVE_CONTINUE");

						if (ec.chessEnginePlayMod  == 4)
							stopSearchAndContinue(EngineState.STOP_MOVE_CONTINUE, "", true);
						else
							stopSearchAndContinue(EngineState.STOP_MOVE_CONTINUE, "", false);
						break;
				}
			}
			else
			{
				stopChessClock();
				ec.chessEnginePaused = true;
				updateCurrentPosition("");
			}
		}

//	Log.i(TAG, "startStopEnginePlay, ec.chessEnginePaused, engineAction: " + ec.chessEnginePaused + ", " + engineAction);

	}

	public void stopSearchAndContinue(EngineState engineState, CharSequence fen, boolean stopAll)
	{

//		Log.i(TAG, "stopSearchAndContinue(), ec.currentEngineId: " + ec.currentEngineId + ", ec.engineCnt: " + ec.engineCnt + ", stopAll: " + stopAll);
//		Log.i(TAG, "stopSearchAndContinue(), engineState: " + engineState + ", fen: " + fen + ", stopAll: " + stopAll + ", ec.currentEngineId: " + ec.currentEngineId);

		continueFen = "";

		if (!fen.equals(""))
			ec.initEngineMessages();

		if (stopAll && ec.engineCnt > 1) {
			EngineState eS = EngineState.STOP_IDLE;
			if (ec.chessEnginePlayMod  == 4) {
				eS = engineState;
				ec.analysisEngineId = ec.currentEngineId;
				ec.analysisEngineCnt = 0;
			}
			if (engineState == EngineState.STOP_QUIT || engineState == EngineState.STOP_QUIT_RESTART)
				eS = engineState;
			for (int i = 0; i < ec.engineCnt; i++)
			{
				if (i != ec.uciEngines[ec.currentEngineId].engineId || (fen.equals("") && !ec.chessEngineMatch)) {
					if (ec.uciEngines[i] != null) {
						if (ec.uciEngines[i].engineSearching())
							ec.uciEngines[i].stopSearch(eS);
						else
						{
							if (engineState == EngineState.STOP_QUIT || engineState == EngineState.STOP_QUIT_RESTART)
								shutDownEngine(i);
						}
					}
				}
			}

			if (fen.equals("") && !ec.chessEngineMatch)
				return;

		}

		if (ec.uciEngines[ec.currentEngineId].engineSearching())
		{

//			Log.i(TAG, "stopSearchAndContinue(), engineSearching(), ec.currentEngineId: " + ec.currentEngineId + ", engineState: " + engineState);

			continueFen = fen;
			ec.uciEngines[ec.currentEngineId].stopSearch(engineState);
			if (!(ec.chessEnginePlayMod  == 3 && ec.engineCnt == 2 && engineState == EngineState.STOP_MOVE_CONTINUE))
				setEnginePausePlayBtn(null, null);
		}
		else {

//			Log.i(TAG, "stopSearchAndContinue(), !engineSearching(), ec.currentEngineId: " + ec.currentEngineId + ", engineState: " + engineState);

			ec.chessEnginePlayMod = userPrefs.getInt("user_play_playMod", 1);
			ec.setPlaySettings(userPrefs, gc.cl.p_color);
			if (ec.uciEngines[ec.currentEngineId].engineState == EngineState.STOP_QUIT) {
				shutDownEngines();
				updateGui();
				setEnginePausePlayBtn(false, null);
			}
			else {
				if (!fen.equals("")) {

//					Log.i(TAG, "1 IDLE, stopSearchAndContinue(), engineName: " + ec.uciEngines[ec.currentEngineId].engineName + "(" + ec.currentEngineId + ") ");

					ec.uciEngines[ec.currentEngineId].engineState = EngineState.IDLE;
					if (dSetClock)
						initChessClock(tc, userPrefs.getInt("user_options_timeControl", 1), ec.chessEnginePlayMod);
					startChessClock();
					setTurnBoard();
					updateGui();

//					Log.i(TAG, "3 stopSearchAndContinue(), ec.uciEngines[ec.currentEngineId].engineState: " + ec.uciEngines[ec.currentEngineId].engineState);

					if (ec.uciEngines[ec.currentEngineId].process != null && ec.chessEnginePlayMod == 4)
						engineSearchAnalysis(fen);
					else
						chessEngineSearch(ec.currentEngineId, fen, "", "");
				}
			}
		}

	}

	public void stopSearchAndRestart(boolean isNewGame, boolean setClock)
	{

//		Log.i(TAG, "1 stopSearchAndRestart(), current engineState: " + ec.uciEngines[ec.currentEngineId].engineState + ", isNewGame: " + isNewGame
//				+ ", setClock: " + setClock + ", ec.chessEnginePaused: " + ec.chessEnginePaused);

		if (ec.uciEngines[ec.currentEngineId].engineSearching())
		{
			continueFen = "";
			stopSearchAndContinue(EngineState.STOP_QUIT_RESTART, "", true);
		}
		else
		{
			ec.chessEnginePlayMod = userPrefs.getInt("user_play_playMod", 1);
			if (ec.uciEngines[ec.currentEngineId].engineState != EngineState.DEAD)
				shutDownEngines();

//			Log.i(TAG, "2 stopSearchAndRestart(), current engineState: " + ec.uciEngines[ec.currentEngineId].engineState
//					+ ", isNewGame: " + isNewGame + ", setClock: " + setClock + ", ec.chessEnginePaused: " + ec.chessEnginePaused);

			if (!ec.chessEnginePaused)
				startPlay(isNewGame, setClock);
			else
				setInfoMessage(getEnginePausedMessage(), null, null);
		}
	}

	public final synchronized void stopComputerThinking(boolean shutDown, boolean gameOver)
	{

//		Log.i(TAG, "stopComputerThinking, ec.uciEngines[ec.currentEngineId].engineState: " + ec.uciEngines[ec.currentEngineId].engineState + ", shutDown: " + shutDown);

		if (ec.uciEngines[ec.currentEngineId] == null)
			return;

		if (shutDown) {
			if (ec.uciEngines[ec.currentEngineId].engineSearching())
				stopSearchAndContinue(EngineState.STOP_QUIT, "", true);
			else {
				if (ec.uciEngines[ec.currentEngineId].engineState != EngineState.DEAD)
					shutDownEngines();
				else
					setEnginePausePlayBtn(false, null);
			}
		}
		else {
			if (gameOver)
				stopSearchAndContinue(EngineState.STOP_QUIT, "", true);
			else {
				if (ec.uciEngines[ec.currentEngineId].engineSearching())
					stopSearchAndContinue(EngineState.STOP_IDLE, "", true);
				else {

//				Log.i(TAG, "2 IDLE, stopComputerThinking(), engineName: " + ec.uciEngines[ec.currentEngineId].engineName + "(" + ec.currentEngineId + ") ");

					ec.uciEngines[ec.currentEngineId].engineState = EngineState.IDLE;
					setEnginePausePlayBtn(false, null);
				}
			}
		}

	}

	public void shutDownEngines()
	{
		if (ec.uciEngines != null) {
			if (ec.engineCnt == 1 && ec.uciEngines[ec.currentEngineId].engineState != EngineState.DEAD)
				shutDownEngine(ec.currentEngineId);
			else {
				if (ec.engineCnt > 0) {
					for (int i = 0; i < ec.engineCnt; i++) {
						if (ec.uciEngines[i] != null && ec.uciEngines[i].engineState != EngineState.DEAD)
							shutDownEngine(i);
					}
				}
			}
		}
	}

	public void shutDownEngine(int engineId)
	{
		if (ec.uciEngines[engineId] != null) {

//			Log.i(TAG,  "1a shutDownEngine(), engineId: " + engineId + ", ec.uciEngines[engineId].engineMonitor: " + ec.uciEngines[engineId].engineMonitor);
//			Log.i(TAG,  "1b shutDownEngine(), engineId: " + engineId + ", ec.uciEngines[engineId].process: " + ec.uciEngines[engineId].process);
//			Log.i(TAG,  "1c shutDownEngine(), engineId: " + engineId + ", ec.uciEngines[engineId].reader: " + ec.uciEngines[engineId].reader);
//			Log.i(TAG,  "1d shutDownEngine(), engineId: " + engineId + ", engineName: " + ec.uciEngines[engineId].engineName);

			ec.uciEngines[engineId].shutDown();
			ec.uciEngines[engineId].engineName = "";

			if (ec.uciEngines[engineId].process != null) {
				ec.uciEngines[engineId].engineMonitor.interrupt();
				ec.uciEngines[engineId].engineMonitor = null;
				ec.uciEngines[engineId].process.destroy();
				ec.uciEngines[engineId].process = null;
				ec.uciEngines[engineId].reader = null;
				ec.uciEngines[engineId].writer = null;
			}

		}

	}

	public void getGameData(CharSequence fileBase, CharSequence filePath, CharSequence fileName,
							CharSequence startPgn, boolean withMoveHistory, boolean isEndPos, int moveIdx, boolean isUpdateGui)
	{

//		Log.i(TAG, "getGameData(), startPgn: \n" + startPgn);

		if (!startPgn.toString().endsWith(" 1/2-1/2") & startPgn.toString().endsWith(" 1/2"))
			startPgn = startPgn.toString().replace("1/2", "1/2-1/2");
		gc.errorMessage = "";
		gc.errorPGN = "";
		try
		{
			gc.cl.newPositionFromPgnData(fileBase, filePath, fileName, startPgn, isEndPos, moveIdx, withMoveHistory);
			if (gc.cl.p_stat.equals("1"))
			{
				gc.setGameOver(gc.cl.history.getGameTagValue("Result"));
				if (gc.cl.p_message.equals("*"))
					gc.cl.p_message = "";
				if (isUpdateGui)
					updateGui();

			}
			else
			{
				gc.errorMessage = gc.cl.p_message;
				gc.errorPGN = ">>>PGN-PARSE-DATA<<< \n" + gc.cl.history.createGameNotationFromHistory(gc.cl.history.MOVE_HISTORY_MAX_50000, false, true,
						false, false, true, 2, 0) + "\n\n"
						+ ">>>PGN-INPUT-DATA<<< \n" + startPgn + "\n";
				updateGui();
				if (!gc.cl.p_stat.equals("4"))
					c4aShowDialog(PGN_ERROR_DIALOG);
			}
		}
		catch (NullPointerException e) {e.printStackTrace();}
	}

	public void setPlayModPrefs(int playMod)
	{
		SharedPreferences.Editor ed = userPrefs.edit();
		ed.putInt("user_play_playMod", playMod);
		ed.commit();
	}

	public void nextMove(int moveDirection, int moveIdx)
	{	// next move(History) and updateGui

//		Log.i(TAG, "nextMove, moveDirection, moveIdx: " + moveDirection + ", " + moveIdx);

		gc.isMoveError = false;
		if (!gc.isAutoPlay)
		{
			gc.isGameOver = false;
			gc.cl.p_variationEnd = false;
		}
		try
		{
			if (moveDirection == 1)		gc.cl.getPositionFromMoveHistory(1, 0);				// move back
			if (moveDirection == 2)		gc.cl.getPositionFromMoveHistory(2, 0);				// next move
			if (moveDirection == 3)		gc.cl.getPositionFromMoveHistory(3, 0);				// start position
			if (moveDirection == 4)		gc.cl.getPositionFromMoveHistory(4, 0);				// end position
			if (moveDirection == 12)	gc.cl.getPositionFromMoveHistory(12, 0);				// two moves back(engine play)
			if (moveDirection == 19)	gc.cl.getPositionFromMoveHistory(19, moveIdx);					// position from moveIdx

//			Log.i(TAG, "gc.cl.p_stat, gc.isAutoPlay, gc.cl.p_gameOver, gc.cl.p_gameEnd: " + gc.cl.p_stat + ", " + gc.isAutoPlay + ", " + gc.cl.p_gameOver + ", " + gc.cl.p_gameEnd);
//			Log.i(TAG, "nextMove(), moveDirection: " + moveDirection + ", moveIdx: " + moveIdx + ", gc.cl.p_stat: " + gc.cl.p_stat);

			if (gc.cl.p_stat.equals("1"))
			{
				gc.move = "";
				gc.cl.p_hasPossibleMoves = false;
				updateGui();
			}
			if (gc.isAutoPlay)
			{
				gc.setGameOver("end");
				if (gc.isGameOver)
					stopAutoPlay(false);
			}
		}
		catch (IndexOutOfBoundsException e) {e.printStackTrace();}
	}

	public void setNagToMoveText(CharSequence nag)
	{
		gc.isGameUpdated = false;
		gc.cl.history.setMoveText(nag);
		updateCurrentPosition("");
	}

	public void deleteMoves(boolean deleteMoveIdx)
	{	// delete moves(History) and updateGui
		setUndoPgn();
		gc.cl.deleteMovesFromMoveHistory(deleteMoveIdx);
		if (gc.cl.p_stat.equals("1"))
		{
			gc.move = "";
			gc.isGameOver = false;
			gc.cl.p_hasPossibleMoves = false;
			updateGui();
		}
		if (ec.uciEngines[ec.currentEngineId].engineState == EngineState.PONDER)
			engineStopPonder(gc.cl.p_fen, ec.chessEnginePlayMod);
		else
		{
			if (!ec.chessEnginePaused)
			{
				if (ec.chessEnginePlayMod <= 3)
					pauseStopPlay(false);
				else {
					if (!gc.cl.p_fen.equals(""))
						stopSearchAndContinue(EngineState.STOP_CONTINUE, gc.cl.p_fen, true);
				}

			}
		}
	}

	public void setUndoPgn()
	{
		if (gc.cl.history.moveHistory.size() - gc.cl.history.getMoveIdx() >= 3) {
			SharedPreferences.Editor ed = runP.edit();
			ed.putString("run_undoPgn", (String) gc.cl.history.createPgnFromHistory(1));
			ed.putInt("run_undoMoveIdx", gc.cl.history.getMoveIdx());
			ed.commit();
		}
	}

	public void getUndoPgn()
	{
		if (!runP.getString("run_undoPgn", "").equals("")) {
			CharSequence pgnData = gc.cl.history.createPgnFromHistory(1);
			int moveIdx = gc.cl.history.getMoveIdx();
			getFromClipboard(runP.getString("run_undoPgn", ""), runP.getInt("run_undoMoveIdx", 0));
			SharedPreferences.Editor ed = runP.edit();
			ed.putString("run_undoPgn", (String) pgnData);
			ed.putInt("run_undoMoveIdx", moveIdx);
			ed.commit();
		}
	}

	public void startEdit(boolean isNewGame, boolean setClock)
	{

//		Log.i(TAG, "startEdit(), isNewGame: " + isNewGame + ", setClock: " + setClock);

		ec.chessEngineSearching = false;
		gc.isGameOver = false;
		gc.isGameUpdated = true;
		gc.isPlayerPlayer = false;
		setEnginePausePlayBtn(null, null);
		if (!gc.errorMessage.equals(""))
		{
			gc.errorMessage = "";
			gc.errorPGN = "";
			isNewGame = true;
		}
		if (!isNewGame)
			updateCurrentPosition("");
		else
			getNewChessPosition(Integer.toString(userPrefs.getInt("user_game_chess960Id", 518)));
		if (ec.chessEnginePlayMod == 5)
		{	// player vs player
			if (isNewGame | setClock)
				initChessClock(tc, userPrefs.getInt("user_options_timeControl", 1), ec.chessEnginePlayMod);
			if (twoPlayerPaused) {
				updateCurrentPosition("");
				setEnginePausePlayBtn(false, null);
			}
			else
            {
                if (!(gc.fen.equals(gc.startFen) & (ec.chessEnginePlayMod == 1 | ec.chessEnginePlayMod == 5)))
                    startChessClock();
                else
					updateCurrentPosition("");
            }
		}
	}

	public void startPlay(boolean isNewGame, boolean setClock)
	{

//		Log.i(TAG, "startPlay(), isNewGame: " + isNewGame + ", setClock: " + setClock + ", ec.chessEnginePlayMod: " + ec.chessEnginePlayMod);

		ec.chessEngineSearching = false;
		ec.chessEnginePaused = false;
		gc.cl.pos = new ChessPosition(gc.cl.history.chess960Id);
		gc.cl.posPV = new ChessPosition(gc.cl.history.chess960Id);
		displayMoves = null;
		if (isNewGame)
		{
			gc.errorMessage = "";
			gc.errorPGN = "";
		}
		else
		{
			if (!gc.errorMessage.equals(""))
			{
				setInfoMessage(gc.errorMessage, null, null);
				return;
			}
		}
		analysisMessage = "";
		if (isNewGame | (!gc.cl.p_mate & !gc.cl.p_stalemate & !gc.cl.p_auto_draw))	// !mate, steal mate, auto draw?
		{
			gc.isGameOver = false;
			gc.isGameUpdated = true;
			ec.chessEnginePlayMod = userPrefs.getInt("user_play_playMod", 1);
			setTurnBoard();
			ec.setPlaySettings(userPrefs, gc.cl.p_color);
			if (isNewGame)
				getNewChessPosition(Integer.toString(userPrefs.getInt("user_game_chess960Id", 518)));
			if (gc.cl.p_chess960ID == 518)
				gc.isChess960 = false;
			else
				gc.isChess960 = true;
			gc.startFen = gc.cl.history.getStartFen();
			if (!gc.cl.p_fen.equals(""))
				gc.fen = gc.cl.p_fen;
			else
				gc.fen = gc.startFen;
			engineMes = "";
			initInfoArrays();

//			Log.i(TAG, "startPlay(), isAppStart: " + isAppStart);
//			Log.i(TAG, "startPlay(), gc.startFen: " + gc.startFen + ", gc.cl.p_fen: " + gc.cl.p_fen + ", gc.fen: " + gc.fen + ", gc.isChess960: " + gc.isChess960);

			if (isAppStart)
				isAppStart = false;
			if (isNewGame | setClock)
				initChessClock(tc, userPrefs.getInt("user_options_timeControl", 1), ec.chessEnginePlayMod);

			setEnginePausePlayBtn(null, null);

			if (ec.chessEnginePlayMod <= 5)
			{
				if (!(gc.fen.equals(gc.startFen) & (ec.chessEnginePlayMod == 1 | ec.chessEnginePlayMod == 5)))
					startChessClock();
			}
			if (ec.chessEnginePlayMod <= 4)
				startEnginePlay(isNewGame);
			else
				setInfoMessage(getEnginePausedMessage(), null, null);
		}
		else {

//			Log.i(TAG, "2 getGameOverMessage()");

			setInfoMessage(getGameOverMessage(), null, null);
		}
	}

	public void getNewChessPosition(CharSequence chess960Id)
	{	// new Game, new ChessPosition  and updateGui
		gc.cl.newPosition(chess960Id, "", gc.cl.history.getGameTagValue("Event"), gc.cl.history.getGameTagValue("Site"), "",
				gc.cl.history.getGameTagValue("Round"), gc.cl.history.getGameTagValue("White"), gc.cl.history.getGameTagValue("Black"));
		if (gc.cl.p_stat.equals("1"))
		{
			if (gc.cl.history.getGameTagValue("Event").equals(""))
				gc.cl.history.setGameTag("Event", getString(R.string.app_name));
			if (gc.cl.p_chess960ID != 518)
			{
				SharedPreferences.Editor ed = userPrefs.edit();
				ed.putInt("user_game_chess960Id", gc.cl.p_chess960ID);
				ed.commit();
			}
			lblPlayerNameA.setText("");
			lblPlayerNameB.setText("");
			if (!ec.chessEngineMatch) {
				lblPlayerEloA.setText("");
				lblPlayerEloB.setText("");
			}
			gc.cl.p_color = "w";
			gc.cl.p_message = "";
			if (!ec.chessEngineMatch)
				updateGui();
		}
		else
		{
			gc.cl.p_fen = "";
			gc.cl.p_color = "";
			gc.cl.p_moveText = "";
			updateGui();
		}
	}

	public void startEnginePlay(boolean newGame)
	{	//setting play options and start engine play

//		Log.i(TAG, "startEnginePlay(), gc.fen: " + gc.fen);
//		Log.i(TAG, "startEnginePlay(), newGame: " + newGame + ", engineState: " + ec.getEngine().engineState);

		if (ec.getEngine() == null) {
			return;
		}

		ec.setPlaySettings(userPrefs, gc.cl.p_color);
		ec.setStartPlay(ec.currentEngineId, gc.getValueFromFen(gc.fen, 2));

		switch (ec.getEngine().engineState)
		{
			case IDLE:
				startEnginePlayIsReady(newGame);
				return;
			case DEAD:
				restartEngine();
				return;
			default:

//				Log.i(TAG, "startEnginePlay(), error, engineState: " + ec.getEngine().engineState);

				ec.chessEngineSearching = false;
				stopComputerThinking(false, false);
				ec.chessEnginePaused = true;
				ec.chessEngineInit = true;
				updateCurrentPosition("");
				setEnginePausePlayBtn(false, null);
				setInfoMessage(getString(R.string.engine_noRespond) + " (9)" + getString(R.string.engine_paused), null, null);

		}

	}

	public void startEnginePlayIsReady(boolean newGame)
	{

//		Log.i(TAG, "startEnginePlayIsReady(), newGame: " + newGame);

		if (ec.chessEngineMatch)
			setMatchResultToElo();

		if (newGame)
		{
			engineMes = "";
			engineStat = "";
			initInfoArrays();
		}

		ec.chessEnginePaused = false;
		ec.chessEngineInit = false;
		displayMoves = null;
		ec.setPlayData(userPrefs, gc.cl.history.getGameTagValue("White"), gc.cl.history.getGameTagValue("Black"));
		setTagGameData();
		boolean isInit = false;
		if (ec.getEngine() == null)
			isInit = true;
		else
		{
			if (ec.getEngine().process == null)
				isInit = true;
		}
		if (isInit) {
			if (!startNewGame(ec.currentEngineId, false)) {
				stopChessClock();
				ec.chessEngineSearching = false;
				ec.chessEnginePaused = true;
				ec.chessEngineInit = true;
				setEnginePausePlayBtn(false, null);
				setInfoMessage(getString(R.string.engine_noRespond) + " (2)", null, null);
				return;
			}
		}

//		Log.i(TAG, "startEnginePlayIsReady(), ec.makeMove: " + ec.makeMove + ", gc.cl.p_fen: " + gc.cl.p_fen + ", ec.currentEngineId: " + ec.currentEngineId);

		if (ec.makeMove)
		{
			setEnginePausePlayBtn(true, true);
			ec.chessEngineSearching = true;
			if (!ec.chessEngineMatch)
				setInfoMessage(getEngineThinkingMessage(), null, null);
			isGoPonder = false;

//			Log.i(TAG, "startEnginePlayIsReady(), ec.makeMove: " + ec.makeMove + ", engineState: " + ec.uciEngines[ec.currentEngineId].engineState);

			if (!gc.cl.p_fen.equals(""))
				chessEngineSearch(ec.currentEngineId, gc.cl.p_fen, "", "");
		}
		else
		{
			setEnginePausePlayBtn(false, null);
			ec.chessEngineProblem = true;
			if (!(gc.fen.equals(gc.startFen) & ec.chessEnginePlayMod == 1))
				startChessClock();
			messageEngineShort  = "";

//			Log.i(TAG, "9 startEnginePlayIsReady(), ec.makeMove: " + ec.makeMove);

			if (ec.chessEnginePlayMod == 1 || ec.chessEnginePlayMod == 2)
				setInfoMessage(getString(R.string.player_move), null, null);
			ec.chessEngineSearching = false;
			ec.chessEnginePaused = true;
			setEnginePausePlayBtn(false, null);

//			Log.i(TAG, "startEnginePlayIsReady(), enginePlayPonder()");

			if 	(		userPrefs.getBoolean("user_options_enginePlay_Ponder", false)
					&	ec.getEngine().isUciPonder
					&	!newGame
					& 	(ec.chessEnginePlayMod == 1 | ec.chessEnginePlayMod == 2)
				)
					enginePlayPonder(gc.cl.p_fen);

		}
	}

	public synchronized void restartEngine(){

//		Log.i(TAG, "1 restartEngine(), ec.chessEnginePlayMod: " + ec.chessEnginePlayMod + ", current engines: " + ec.engineCnt);

		if (ec.chessEngineMatch)
			setMatchResultToElo();

		if (ec.enginesRunning())
		{

//			Log.i(TAG, "2 restartEngine(), stopMultiEnginesAndRestart()");

			stopMultiEnginesAndRestart();
			setInfoMessage(getString(R.string.engineInit), null, null);

			return;

		}

		boolean isSingleEngine =  ec.chessEnginePlayMod < 3 || ec.chessEnginePlayMod > 4 || (!withMultiEngineAnalyse && ec.chessEnginePlayMod == 4);
		if (runP.getString("run_engineProcess", OEX_DEFAULT_ENGINE_SINGLE).equals(OEX_OLD_STOCKFISH)) {
			SharedPreferences.Editor edR = runP.edit();
			edR.putString("run_engineProcess", OEX_DEFAULT_STOCKFISH);
			edR.commit();
		}
		if (isSingleEngine)
			engineNames = getEngineList(runP.getString("run_engineProcess", OEX_DEFAULT_ENGINE_SINGLE));
		else {
			if (ec.chessEnginePlayMod == 3) {
				if (userPrefs.getBoolean("user_play_eve_engineVsEngine", true))
					engineNames = getEngineList(runP.getString("run_engineListMatch", OEX_DEFAULT_ENGINES_MATCH));
				else
					engineNames = getEngineList(runP.getString("run_engineProcess", OEX_DEFAULT_ENGINE_SINGLE));
			}
			if (ec.chessEnginePlayMod == 4) {
				if (userPrefs.getBoolean("user_play_multipleEngines", true))
					engineNames = getEngineList(runP.getString("run_engineListAnalysis", OEX_DEFAULT_ENGINES_ANALYSIS));
				else
					engineNames = getEngineList(runP.getString("run_engineProcess", OEX_DEFAULT_ENGINE_SINGLE));
			}
		}

//		Log.i(TAG, "3 restartEngine(), engineNames: " + engineNames);


		if (!restartFen.toString().equals("")) {
			gc.fen = restartFen;
			restartFen = "";
			ec.setPlaySettings(userPrefs, gc.getValueFromFen(gc.fen, 2));
		}
		listener = new EngineListener();
		if (engineNames != null) {
			if (engineNames.size() > 0) {
				ec.ue = null;
				ec.engineCnt = engineNames.size();
				ec.uciEngines = new UciEngine[ec.engineCnt];
				ec.uciEnginesMessage = new String[ec.engineCnt];
				ec.uciEnginesDisplayMoves = new String[ec.engineCnt];
				ec.uciEnginesScore = new int[ec.engineCnt];
				ec.analysisBestScoreEngineId = 0;
				ec.analysisEngineBestMove = "";
				ec.searchId = 0;
				ec.initEngineMessages();
				int currentEngineId = 0;
				if (ec.chessEnginePlayMod == 3 && ec.engineCnt == 2 && gc.getValueFromFen(gc.fen, 2).toString().equals("b"))
					currentEngineId = 1;
				ec.setCurrentEngineId(currentEngineId);
				SpannableStringBuilder mesInit = new SpannableStringBuilder();
				SpannableString sText;
				mesInit.append(getString(R.string.engineInit) + "\n");
				for (int i = 0; i < engineNames.size(); i++) {
					sText = new SpannableString(engineNames.get(i) + "\n");
					int colorVal;
					switch (i) {
						default: colorVal = ColorValues.COLOR_ARROWS5_27; break;	// case 0
						case 1:  colorVal = ColorValues.COLOR_ARROWS6_28; break;
						case 2:  colorVal = ColorValues.COLOR_ARROWS7_29; break;
						case 3:  colorVal = ColorValues.COLOR_ARROWS8_30; break;
					}
					sText.setSpan(new ForegroundColorSpan(cv.getTransparentColorInt(colorVal, "ee")), 0, engineNames.get(i).length(), Spannable.SPAN_INTERMEDIATE);
					mesInit.append(sText);
				}
				setInfoMessage(getString(R.string.engineInit) + " (" + ec.engineCnt + ")", mesInit, null);
				ec.uciEngines[0] = new UciEngine(this, 0, engineNames.get(0), listener);
			}
		}
		else
		{
			msgC4aDialog = "restartEngine():\n" + getString(R.string.engineNoEnginesOnDevice);
			showDialog(C4A_DIALOG);
		}

	}

	public synchronized void stopMultiEnginesAndRestart()
	{

//		Log.i(TAG, "stopMultiEnginesAndRestart() ");

		if (ec.uciEngines == null)
			restartEngine();
		else {
			for (int i = 0; i < ec.engineCnt; i++)
			{
				if (ec.uciEngines[i] != null)
					shutDownEngine(i);
			}
			restartEngine();
		}

	}

	public synchronized ArrayList<String> getEngineList(String runPrefKey)
	{

		ArrayList<String> engineNames = new ArrayList<>();
		String[] txtSplit = runPrefKey.split("\\|");
		engineNames.addAll(Arrays.asList(txtSplit));

//		Log.i(TAG, "getEngineList(), runPrefKey: " + runPrefKey + ". listSize: " + engineNames.size());
		
		return engineNames;

	}

	public synchronized boolean startNewGame(int engineId, boolean initEngine)
	{

//		Log.i(TAG, "1 startNewGame(), initEngine: " + initEngine + ", gc.isChess960: " + gc.isChess960);

		if (initEngine)
		{
			ec.uciEngines[engineId].setUciMultiPV(getMultiPv());
			ec.uciEngines[engineId].setIsChess960(gc.isChess960);
			if 	(		userPrefs.getBoolean("user_options_enginePlay_Ponder", false)
					&&	ec.uciEngines[engineId].isUciPonder
					&& 	(ec.chessEnginePlayMod == 1 || ec.chessEnginePlayMod == 2 || (ec.chessEnginePlayMod == 3 && ec.chessEngineMatch))
			)
				//PONDER
				if (withMatchPonder)
					ec.uciEngines[engineId].setUciPonder(userPrefs.getBoolean("user_options_enginePlay_Ponder", false));
				else
					ec.uciEngines[engineId].setUciPonder(false);

			FileIO f = new FileIO(this);;
			ec.uciEngines[engineId].setUciOptsFromFile(f.getDataFromUciFile(f.getUciExternalPath(), ec.uciEngines[engineId].uciFileName));

			ec.uciEngines[engineId].setUciHash();
			ec.uciEngines[engineId].withUciElo = false;
			if (ec.chessEnginePlayMod == 1 || ec.chessEnginePlayMod == 2)
				ec.uciEngines[engineId].withUciElo = true;

			ec.uciEngines[engineId].uciElo = userPrefs.getInt("uci_elo", UciEngine.UCI_ELO_STANDARD);
			if (ec.uciEngines[engineId].uciElo < ec.uciEngines[engineId].uciEloMin)
				ec.uciEngines[engineId].uciElo = ec.uciEngines[engineId].uciEloMin;
			if (ec.uciEngines[engineId].uciElo > ec.uciEngines[engineId].uciEloMax)
				ec.uciEngines[engineId].uciElo = ec.uciEngines[engineId].uciEloMax;
			ec.uciEngines[engineId].setElo(ec.uciEngines[engineId].withUciElo, ec.uciEngines[engineId].uciElo);

//			Log.i(TAG, "2 startNewGame(), uci_elo: " + userPrefs.getInt("uci_elo", ccc.chess.gui.chessforall.ChessEngine.UCI_ELO_STANDARD) + ", elo: " + ec.uciEngines[engineId].uciElo);
//			Log.i(TAG, "2 startNewGame(), uciEloMin: " + ec.uciEngines[engineId].uciEloMin + ", uciEloMax: " + ec.uciEngines[engineId].uciEloMax);
//			Log.i(TAG, "2 startNewGame(), withElo: " + ec.uciEngines[engineId].withUciElo + ", engineName: " + ec.uciEngines[engineId].engineName);
//			Log.i(TAG, "2 startNewGame(), withElo: " + ec.uciEngines[engineId].withUciElo + ", engineNameElo: " + ec.uciEngines[engineId].engineNameElo);
//			Log.i(TAG, "2 startNewGame(), ec.isUciNewGame: " + ec.isUciNewGame);

			ec.uciEngines[engineId].newGame(ec.isUciNewGame);
			ec.isUciNewGame = true;

//			Log.i(TAG, "2 startNewGame(), initEngine: " + initEngine);

			Boolean isReadyOk = ec.uciEngines[engineId].syncReady();

//			Log.i(TAG, "3 startNewGame(), isReadyOk: " + isReadyOk);

			return isReadyOk;

		}
		else
			return false;

	}

	public void chessEngineSearch(int engineId, CharSequence fen, CharSequence moves, CharSequence ponderMove)
	{

//		Log.i(TAG, "chessEngineSearch(), fen: " + fen + ", ec.chessEnginePaused: " + ec.chessEnginePaused + ", isGoPonder: " + isGoPonder + ", engineState: " + ec.uciEngines[engineId].engineState);

		if (!fen.equals(""))
		{

//			Log.i(TAG, "chessEngineSearch(), fen: " + fen + ", moves: " + moves);

			if (ec.chessEnginePlayMod == 3 && ponderMove.toString().equals("")) {    // engine vs engine
				ec.setCurrentEngineId(0);
				if (ec.engineCnt == 2 && gc.getValueFromFen(fen, 2).equals("b"))
						ec.setCurrentEngineId(1);
				engineId = ec.currentEngineId;
			}

//			Log.i(TAG, "chessEngineSearch(), fen: " + fen + ", engineId: " + engineId + ", isGoPonder: " + isGoPonder + ", ponderMove: " + ponderMove + ", engineState: " + ec.uciEngines[engineId].engineState);

			if (ec.uciEngines[engineId] == null)
			{

//				Log.i(TAG, "A chessEngineSearch(), restartEngine()");

				restartEngine();
				return;
			}

			if (ec.uciEngines[engineId].process == null)
			{

//				Log.i(TAG, "B chessEngineSearch(), restartEngine()");

				restartEngine();
				return;
			}

			boolean isPlayer = isPlayerMove(ec.chessEnginePlayMod, gc.getValueFromFen(fen, 2));

//			Log.i(TAG, "chessEngineSearch(), engineState: " + ec.uciEngines[engineId].engineState + ", ec.currentEngineId: " + ec.currentEngineId);

			if (ec.uciEngines[engineId].engineState == EngineState.IDLE || isGoPonder) {
				setEnginePausePlayBtn(true, true);
				if (isPlayer && !isGoPonder) {

					btn_3.setBackgroundResource(R.drawable.button);
					startChessClock();
					setInfoMessage(getString(R.string.player_move), null, null);
				}
				else {
					if (!isStateGameOver()) {
						ec.chessEnginePaused = false;
						searchTaskFen = fen;
						searchTaskMoves = moves;
						boolean isSearch = true;
						boolean isAnalyze = false;
						if (ec.chessEnginePlayMod == 4) {
							isSearch = false;
							isAnalyze = true;
						}
						startSearchThread(engineId, fen.toString(), moves.toString(), gc.cl.history.getStartFen().toString(), isSearch, isAnalyze, ponderMove.toString());
					}
				}
			}

		}
	}

	public int getMultiPv()
	{
		if (ec.chessEnginePlayMod == 4 && (!userPrefs.getBoolean("user_play_multipleEngines", true) || !withMultiEngineAnalyse)) {
			if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
				return userPrefs.getInt("user_options_enginePlay_MultiPv", Settings.VARIANTS_DEFAULT);
			else
				return userPrefs.getInt("user_options_enginePlay_MultiPv_land", Settings.VARIANTS_DEFAULT_LAND);
		}
		else
			return 1;
	}

	public CharSequence getEngineMatchMessage()
	{
		String matchId = "M" + userPrefs.getInt("user_play_eve_matchId", 1);
		if (ec.chessEngineMatchFinishGame)
			matchId = "E M" + userPrefs.getInt("user_play_eve_matchId", 1);
		String games = "#" + userPrefs.getInt("user_play_eve_round", 0) + "(" + userPrefs.getInt("user_play_eve_games", 4) + ")";
		String result = getString(R.string.result) + ": " + userPrefs.getString("user_play_eve_result", "0-0");
		return matchId + ", " + games + ", " + result;
	}

	public CharSequence getEngineThinkingMessage()
	{

		if (ec.getEngine() == null) {

//			Log.i(TAG, "1 getEngineThinkingMessage(), engine_paused");

			ec.chessEnginePaused = true;
			return getString(R.string.engine_paused);
		}

//		Log.i(TAG, "getEngineThinkingMessage(), ec.chessEnginePlayMod: " + ec.chessEnginePlayMod + ", ec.chessEngineAnalysis: " + ec.chessEngineAnalysis);

		if (ec.chessEnginePlayMod == 5)	// player vs player
		{
			messageEngine = new SpannableStringBuilder("");
			messageEngineShort  = "";
			if (tc.clockIsRunning)
				messageInfo = getString(R.string.play_two_players_flip);
			else
				messageInfo = getString(R.string.play_two_players_flip) + " (" + getString(R.string.clock_stopped) + ")";
			return messageInfo;
		}
		if (ec.chessEnginePlayMod == 6)	// edit
		{
			messageEngine = new SpannableStringBuilder("");
			messageEngineShort  = "";
			messageInfo = getString(R.string.menu_modes_edit);
			return messageInfo;
		}
		if (ec.chessEnginePlayMod != 4)
		{
			if (!ec.chessEnginePaused)
			{
				if 	(		userPrefs.getBoolean("user_options_enginePlay_Ponder", false)
						&	ec.getEngine().isUciPonder
						& 	ec.getEngine().engineState == EngineState.PONDER & (ec.chessEnginePlayMod == 1 | ec.chessEnginePlayMod == 2)
					)
				{
					return getString(R.string.player_move);
				}
				else
					return ec.getEngine().engineName + " " + ec.getEngine().engineNameStrength;
			}
			else {

//				Log.i(TAG, "2 getEngineThinkingMessage(), engine_paused");

				return getString(R.string.engine_paused);
			}
		}
		else
		{
			if (ec.chessEnginePlayMod == 4 && ec.chessEngineAnalysis)
			{
				CharSequence analizeEngineName = ec.getEngine().engineName;
				if (ec.uciEngines[ec.analysisBestScoreEngineId] != null) {
					if (ec.chessEnginePlayMod == 4 && userPrefs.getBoolean("user_play_multipleEngines", true) && withMultiEngineAnalyse)
						analizeEngineName = ec.uciEngines[ec.analysisBestScoreEngineId].engineName;
				}

				if (analizeEngineName.toString().endsWith("%)") & analizeEngineName.toString().contains("("))
				{
					int startChar = analizeEngineName.toString().indexOf("(") -1;
					analizeEngineName = analizeEngineName.toString().subSequence(0, startChar);
				}

//				Log.i(TAG, "getEngineThinkingMessage(), analizeEngineName: >" + analizeEngineName + "<");

				return getString(R.string.engineAnalysisSearch) + "  " + analizeEngineName;
			}
			else
				return messageInfo;
		}
	}

	public CharSequence getEnginePausedMessage()
	{

//		Log.i(TAG, "getEnginePausedMessage(), ec.chessEnginePlayMod: " + ec.chessEnginePlayMod);

		if (ec.chessEnginePlayMod == 5)	// player vs player
		{
			messageEngine = new SpannableStringBuilder("");
			messageEngineShort  = "";
			if (!twoPlayerPaused)
				messageInfo = getString(R.string.play_two_players_flip);
			else
				messageInfo = getString(R.string.play_two_players_flip) + " (" + getString(R.string.clock_stopped) + ")";
			return messageInfo;
		}
		if (ec.chessEnginePlayMod == 6)	// edit
		{
			messageEngine = new SpannableStringBuilder("");
			messageEngineShort  = "";
			messageInfo = getString(R.string.menu_modes_edit);
			return messageInfo;
		}
		if (ec.chessEnginePlayMod != 4) {

//			Log.i(TAG, "1 getEngineThinkingMessage(), engine_paused");

			return getString(R.string.engine_paused);
		}
		else
		{
			if (ec.chessEnginePaused)
				return getString(R.string.engine_paused);
			else
			{
				if (messageInfo.toString().startsWith(getString(R.string.engineAnalysisSearch)))
					return messageInfo;
				else
					return getString(R.string.engineAnalysisSearch);
			}
		}
	}

	public void cancelEngineMessage()
	{
		if (ec.chessEnginePaused)
		{
			messageEngine = new SpannableStringBuilder("");
			messageEngineShort  = "";
		}
	}

	public void initChessClock(TimeControl tc, int tcMode, int playMod)
	{
		int timeWhite = 300000;
		int timeBlack = 300000;
		int movesToGo = 0;
		int bonusWhite = 0;
		int bonusBlack = 0;
		switch (playMod)
		{
			case 1:
				switch (tcMode)
				{
					case 1:
						timeWhite = userPrefs.getInt("user_time_player_clock", OptionsTimeControl.TIME_PLAYER_CLOCK);
						timeBlack = userPrefs.getInt("user_time_engine_clock", OptionsTimeControl.TIME_ENGINE_CLOCK);
						bonusWhite = userPrefs.getInt("user_bonus_player_clock", OptionsTimeControl.TIME_PLAYER_BONUS);
						bonusBlack = userPrefs.getInt("user_bonus_engine_clock", OptionsTimeControl.TIME_ENGINE_BONUS);
						timeWhite = timeWhite + bonusWhite;
						timeBlack = timeBlack + bonusBlack;
						break;
					case 2:
						timeWhite = userPrefs.getInt("user_time_player_move", OptionsTimeControl.TIME_PLAYER_MOVE);
						timeBlack = userPrefs.getInt("user_time_engine_move", OptionsTimeControl.TIME_ENGINE_MOVE);
						bonusBlack = userPrefs.getInt("user_time_engine_move", OptionsTimeControl.TIME_ENGINE_MOVE);
						break;
					case 3:
						timeWhite = userPrefs.getInt("user_time_player_sand", OptionsTimeControl.TIME_PLAYER_SAND);
						timeBlack = userPrefs.getInt("user_time_engine_sand", OptionsTimeControl.TIME_ENGINE_SAND);
						bonusBlack = 2000;
						break;
					case 4:
						timeWhite = 0;
						timeBlack = 0;
						break;
				}
				break;
			case 2:
				switch (tcMode)
				{
					case 1:
						timeWhite = userPrefs.getInt("user_time_engine_clock", OptionsTimeControl.TIME_ENGINE_CLOCK);
						timeBlack = userPrefs.getInt("user_time_player_clock", OptionsTimeControl.TIME_PLAYER_CLOCK);
						bonusWhite = userPrefs.getInt("user_bonus_engine_clock", OptionsTimeControl.TIME_ENGINE_BONUS);
						bonusBlack = userPrefs.getInt("user_bonus_player_clock", OptionsTimeControl.TIME_PLAYER_BONUS);
						timeWhite = timeWhite + bonusWhite;
						timeBlack = timeBlack + bonusBlack;
						break;
					case 2:
						timeWhite = userPrefs.getInt("user_time_engine_move", OptionsTimeControl.TIME_ENGINE_MOVE);
						bonusWhite = userPrefs.getInt("user_time_engine_move", OptionsTimeControl.TIME_ENGINE_MOVE);
						timeBlack = userPrefs.getInt("user_time_player_move", OptionsTimeControl.TIME_PLAYER_MOVE);
						break;
					case 3:
						timeWhite = userPrefs.getInt("user_time_engine_sand", OptionsTimeControl.TIME_ENGINE_SAND);
						bonusWhite = 2000;
						timeBlack = userPrefs.getInt("user_time_player_sand", OptionsTimeControl.TIME_PLAYER_SAND);
						break;
					case 4:
						timeWhite = 0;
						timeBlack = 0;
						break;
				}
				break;
			case 3:
				switch (tcMode)
				{
					case 1:
						timeWhite = userPrefs.getInt("user_time_engine_clock", OptionsTimeControl.TIME_ENGINE_CLOCK);
						timeBlack = userPrefs.getInt("user_time_engine_clock", OptionsTimeControl.TIME_ENGINE_CLOCK);
						bonusWhite = userPrefs.getInt("user_bonus_engine_clock", OptionsTimeControl.TIME_ENGINE_BONUS);
						bonusBlack = userPrefs.getInt("user_bonus_engine_clock", OptionsTimeControl.TIME_ENGINE_BONUS);
						timeWhite = timeWhite + bonusWhite;
						timeBlack = timeBlack + bonusBlack;
						break;
					case 2:
						timeWhite = userPrefs.getInt("user_time_engine_move", OptionsTimeControl.TIME_ENGINE_MOVE);
						bonusWhite = userPrefs.getInt("user_time_engine_move", OptionsTimeControl.TIME_ENGINE_MOVE);
						timeBlack = userPrefs.getInt("user_time_engine_move", OptionsTimeControl.TIME_ENGINE_MOVE);
						bonusBlack = userPrefs.getInt("user_time_engine_move", OptionsTimeControl.TIME_ENGINE_MOVE);
						break;
					case 3:
						timeWhite = userPrefs.getInt("user_time_engine_sand", OptionsTimeControl.TIME_ENGINE_SAND);
						bonusWhite = 2000;
						timeBlack = userPrefs.getInt("user_time_engine_sand", OptionsTimeControl.TIME_ENGINE_SAND);
						bonusBlack = 2000;
						break;
					case 4:
						timeWhite = 0;
						timeBlack = 0;
						break;
				}
				break;
			case 4:
				if (tcMode != 4)
					tcMode = 11;
				timeWhite = 0;
				timeBlack = 0;
				bonusWhite = 0;
				bonusBlack = 0;
				break;
			case 5:
				switch (tcMode)
				{
					case 1:
						timeWhite = userPrefs.getInt("user_time_player_clock", OptionsTimeControl.TIME_PLAYER_CLOCK);
						timeBlack = userPrefs.getInt("user_time_player_clock", OptionsTimeControl.TIME_PLAYER_CLOCK);
						bonusWhite = userPrefs.getInt("user_bonus_player_clock", OptionsTimeControl.TIME_PLAYER_BONUS);
						bonusBlack = userPrefs.getInt("user_bonus_player_clock", OptionsTimeControl.TIME_PLAYER_BONUS);
						timeWhite = timeWhite + bonusWhite;
						timeBlack = timeBlack + bonusBlack;
						break;
					case 2:
						timeWhite = userPrefs.getInt("user_time_player_move", OptionsTimeControl.TIME_PLAYER_MOVE);
						timeBlack = userPrefs.getInt("user_time_player_move", OptionsTimeControl.TIME_PLAYER_MOVE);
						break;
					case 3:
						timeWhite = userPrefs.getInt("user_time_player_sand", OptionsTimeControl.TIME_PLAYER_SAND);
						timeBlack = userPrefs.getInt("user_time_player_sand", OptionsTimeControl.TIME_PLAYER_SAND);
						break;
					case 4:
						timeWhite = 0;
						timeBlack = 0;
						break;
				}
				break;
		}

//		Log.i(TAG, "initChessClock(), mod: " + playMod + ", timeControl: " + timeControl);
//		Log.i(TAG, "initChessClock(), tw: " + timeWhite + ", tb: " + timeBlack + ", movesToGo: " + movesToGo + ", bw: " + bonusWhite + ", bb: " + bonusBlack);

		tc.initChessClock(tcMode, timeWhite, timeBlack, movesToGo, bonusWhite, bonusBlack);
		tc.setCurrentShowValues(playMod);
	}

	public void stopChessClock()
	{
		handlerChessClock.removeCallbacks(mUpdateChessClock);
		tc.stopChessClock(System.currentTimeMillis(), ec.chessEnginePlayMod);
		if (ec.chessEnginePlayMod == 5) {
			setEnginePausePlayBtn(false, null);
			twoPlayerPaused = true;
		}
	}

	public void startChessClock()
	{

//		Log.i(TAG, "startChessClock(), gc.cl.p_color: " + gc.cl.p_color);

		if (gc.cl.p_color.equals("w"))
			tc.startChessClock(true, System.currentTimeMillis(), ec.chessEnginePlayMod);
		else
			tc.startChessClock(false, System.currentTimeMillis(), ec.chessEnginePlayMod);
		engineControlTime = System.currentTimeMillis();
		engineMatchControlTime = 0;
		handlerChessClock.removeCallbacks(mUpdateChessClock);
		handlerChessClock.postDelayed(mUpdateChessClock, CLOCK_START);
		if (ec.chessEnginePlayMod == 5) {
			setEnginePausePlayBtn(true, null);
			twoPlayerPaused = false;
		}
	}

	public void c4aShowDialog(int dialogId)
	{
		removeDialog(dialogId);
		showDialog(dialogId);
	}

	public void showHtml(int resId, int resTitleId)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		String prompt = "";
		try
		{
			InputStream inputStream = getResources().openRawResource(resId);
			byte[] buffer = new byte[inputStream.available()];
			inputStream.read(buffer);
			prompt = new String(buffer);
			inputStream.close();
		}
		catch (IOException e) {	e.printStackTrace(); }
		WebView wv = new WebView(this);
		builder.setView(wv);
		wv.loadData(prompt, "text/html; charset=UTF-8", null);

		builder.setTitle(getString(resTitleId));
		AlertDialog alert = builder.create();
		alert.show();
	}

	public boolean getIsEndPosition()
	{
		return userPrefs.getBoolean("user_options_gui_LastPosition", false);
	}

	public void setToClipboard(CharSequence text)
	{
		Toast.makeText(this, getString(R.string.menu_info_clipboardCopyPgn), Toast.LENGTH_SHORT).show();
		ClipboardManager cm = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
		cm.setText(text);
	}

	public void getFromClipboard(String undoPgn, int moveIdx)
	{
		CharSequence fen = "";
		CharSequence pgnData = "";
		if (undoPgn.equals("")) {
			try {
				Toast.makeText(this, getString(R.string.menu_info_clipboardPaste), Toast.LENGTH_SHORT).show();
				ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
				pgnData = (String) cm.getText();
			} catch (ClassCastException e) {
				return;
			}
		}
		else
			pgnData = undoPgn;

//		Log.i(TAG, "getFromClipboard(), pgnData: " + pgnData);

		if (pgnData == null)
			return;
		CharSequence[] pgnSplit = pgnData.toString().split(" ");
		if (pgnSplit.length > 0)
		{
			if (pgnSplit[0].toString().contains("/") && !pgnSplit[0].toString().contains("["))
			{
				if (pgnSplit.length == 6)
				{
					pgnSplit[4] = "0";
					pgnSplit[5] = "1";
					fen = pgnSplit[0] + " " + pgnSplit[1] + " " + pgnSplit[2] + " " + pgnSplit[3] + " " + pgnSplit[4] + " " + pgnSplit[5];
				}
				else
				{
					fen = pgnData;
					if (pgnSplit.length == 1)
						fen = fen + " w - - 0 1";
				}
			}
		}

//		Log.i(TAG, "getFromClipboard(), fen: " + fen);

		if (fen.equals(""))
		{
			getGameData("", "", "", pgnData, false, true, 0, false);
			gc.startFen = gc.cl.history.getStartFen();
		}
		else
		{
			gc.cl.newPositionFromFen(fen);
			gc.startFen = fen;

//			Log.i(TAG, "getFromClipboard(), gc.startFen: " + gc.startFen);

		}

//		Log.i(TAG, "getFromClipboard(), gc.cl.p_stat: " + gc.cl.p_stat);

		if (!gc.cl.p_stat.equals("1"))
			return;

//		Log.i(TAG, "getFromClipboard(), mod: " + ec.chessEnginePlayMod + ", chessEnginePaused: " + ec.chessEnginePaused);

		if (gc.cl.p_chess960ID == 518)
			gc.isChess960 = false;
		else
			gc.isChess960 = true;
		gc.isGameLoaded = false;
		gc.isGameOver = false;
		gc.isGameUpdated = true;
		gc.isPlayerPlayer = false;
		ec.chessEngineMatch = false;
		gc.isChess960 = false;
		gc.fen = gc.cl.p_fen;
		if (!undoPgn.equals("") && moveIdx > 0) {
			gc.cl.history.setMoveIdx(moveIdx);
		}

//		Log.i(TAG, "getFromClipboard(), gc.fen: " + gc.fen);

		setPauseValues(false, "", 4, "");
		SharedPreferences.Editor ed = userPrefs.edit();
		ed.putInt("user_game_chess960Id", 518);
		ed.commit();
		updateGui();
		stopSearchAndRestart(false, true);

	}

	public void playSound(int idx, int loop)
	{
		try {
			if (userPrefs.getBoolean("user_options_gui_enableSounds", true)) {

//				Log.i(TAG,"playSound(), idx: " + idx + ", loop: " + loop);

				mSoundPool.play(soundsMap.get(idx), 0.2f, 0.2f, 1, loop, 1.0f);
			}
		}
		catch (NullPointerException e) {e.printStackTrace();}
	}

	public CharSequence getGameInfo()
	{
		String gameInfo = "";
		if 	( 	userPrefs.getBoolean("user_options_gui_usePgnDatabase", true)
				& fmPrefs.getInt("fm_extern_db_game_count", 1) != 0
				& fmPrefs.getInt("fm_load_location", 1) == 1
				)
		{
			if (fmPrefs.getInt("fm_extern_db_key_id", 0) == 0)
				gameInfo = "" + fmPrefs.getInt("fm_extern_db_game_id", 0) + "(" + fmPrefs.getInt("fm_extern_db_game_count", 1) + ")";
			else
				gameInfo = "" + (fmPrefs.getInt("fm_extern_db_cursor_id", 0) +1) + "(" + fmPrefs.getInt("fm_extern_db_cursor_count", 0) + "), "
						+ fmPrefs.getInt("fm_extern_db_game_id", 0) + "[" + fmPrefs.getInt("fm_extern_db_game_count", 1) + "]";
		}
		return gameInfo;
	}

	public void setInfoMessage(CharSequence info, SpannableStringBuilder engineMsg, CharSequence moveNotification)
	{

//		Log.i(TAG,"setInfoMessage(), engineMsg: \n" + engineMsg);
//		Log.i(TAG,"setInfoMessage(), messageEngine: \n" + messageEngine);

		if (messageEngine == null) {
			messageEngine = new SpannableStringBuilder("");
		}
		if (engineMsg != null)
			messageEngine = engineMsg;

		if ((gc.isGameOver | gc.cl.p_variationEnd))
			info = getGameOverMessage();

//		Log.i(TAG,"setInfoMessage()");

//    	Log.i(TAG,"setInfoMessage()info: " + info + ", gc.isGameOver: " + gc.isGameOver + ", gc.cl.p_variationEnd: " + gc.cl.p_variationEnd);

//    	Log.i(TAG,"setInfoMessage(), engineMsg: " + engineMsg);
//    	Log.i(TAG,"engine: " + engine + ", pause_messageEngine:\n" + pause_messageEngine);
//    	Log.i(TAG,"setInfoMessage(), moveNotification: " + moveNotification);
//    	Log.i(TAG, "infoContent: " + infoContent);
//    	Log.i(TAG, "gc.cl.p_fen  : " + gc.cl.p_fen);
//    	Log.i(TAG, "searchTaskFen: " + searchTaskFen);

		if (getPauseValues(false, gc.fen, ec.chessEnginePlayMod))
		{
			if (messageEngine.toString().equals(""))
				messageEngine = new SpannableStringBuilder(pause_messageEngine);
		}
		if (!messageEngine.toString().equals(""))
		{
			if (ec.chessEnginePlayMod <= 4 & userPrefs.getBoolean("user_options_enginePlay_EngineMessage", true))
				messageEngineShort = getInfoShort(messageEngine.toString());
			else
				messageEngineShort = "";
			if (ec.chessEnginesOpeningBook)
				messageEngineShort = getString(R.string.engine_openingBook);
		}

//		Log.i(TAG,"setInfoMessage(), engineMsg: " + engineMsg + ", messageEngineShort: " + messageEngineShort);

		if (!gc.cl.p_fen.equals(searchTaskFen))
		{

//			Log.i(TAG,"setInfoMessage(), gc.cl.p_fen:   " + gc.cl.p_fen);
//			Log.i(TAG,"setInfoMessage(), searchTaskFen: " + searchTaskFen);

		}

		if (msgMoves == null | gc.cl.p_stat.equals(""))
			return;

		if (info != null)
		{
			if (gc.isGameLoaded & ec.chessEnginePaused)
				messageInfo = ".../" + gc.fileName + ", " + getGameInfo();
			else
				messageInfo = info;
		}
		if (!messageEngine.toString().equals(""))
		{
			if (ec.chessEnginePaused)
			{
				if (userPrefs.getBoolean("user_options_enginePlay_EngineMessage", true))
				{

//					Log.i(TAG, "3 getEngineThinkingMessage(), engine_paused");

				}
			}
			if (!userPrefs.getBoolean("user_options_enginePlay_EngineMessage", true))
			{
				CharSequence chs = messageEngine.toString().replace(getString(R.string.epPonder) + ":", "");
				chs = chs.toString().replace(getString(R.string.engineThinking) + ":", "");
				messageEngine = new SpannableStringBuilder(chs);
			}
		}

//		Log.i(TAG,"setInfoMessage(), messageEngine:   " + messageEngine);

		boolean isPlayer = isPlayerMove(ec.chessEnginePlayMod, gc.getValueFromFen(gc.cl.p_fen, 2));
		// show book hints
		if 	(
				 	userPrefs.getBoolean("user_options_enginePlay_ShowBookHints", true)
				& 	(ec.chessEnginePlayMod == 1 | ec.chessEnginePlayMod == 2 | ec.chessEnginePlayMod == 5)
			)
		{

//			Log.i(TAG, "setInfoMessage(), show book hints, gc.cl.p_fen: " + gc.cl.p_fen);
//			Log.i(TAG, "setInfoMessage(), show book hints, gc.fen:      " + gc.fen);

			Pair<String, ArrayList<Move>> bi = null;
			try {bi = ec.book.getAllBookMoves(TextIO.readFEN(gc.cl.p_fen.toString()));}
			catch (ChessParseError e1) {e1.printStackTrace();}
			if (bi != null)
			{
				if (bi.first != null)
				{
					if (!bi.first.equals(""))
					{
						displayMoves = "";
						scoreMoves = "";
						String bookMoves = "";

						if (isPlayer) {
							int arrowCnt = userPrefs.getInt("user_options_gui_arrows", Settings.ARROWS_DEFAULT);
							String txt = bi.second.toString();
							txt = txt.replace("[", "");
							txt = txt.replace("]", "");
							txt = txt.replace(",", "");
							String[] firstSplit = bi.first.split("\n");
							String[] secondSplit = txt.split(" ");
							for (int i = 0; i < arrowCnt; i++) {
								if (i < secondSplit.length) {

//									Log.i(TAG, "setInfoMessage(), BoardView.ARROWS_BOOK: " + BoardView.ARROWS_BOOK);

									arrowsId = BoardView.ARROWS_BOOK;
									displayMoves = displayMoves + secondSplit[i] + " ";
									String[] firstSplit2 = firstSplit[i].split(",");
									if (firstSplit2.length > 0)
										scoreMoves = scoreMoves + firstSplit2[0] + " ";
								}
							}

//							Log.i(TAG, "setInfoMessage(), score: " + scoreMoves + ", moves: " + displayMoves + ", >>> isPlayer: " + isPlayer);

							bookMoves = (String) gc.cl.history.getAlgebraicNotation(bi.first, userPrefs.getInt("user_options_gui_PieceNameId", 0));
						}

						if (!bookMoves.equals("")) {
							String bookName = "book.bin";
							if (!userPrefs.getString("user_options_enginePlay_OpeningBookName", "").equals("")) {
								String[] txtSplit = userPrefs.getString("user_options_enginePlay_OpeningBookName", "").split("/");
								if (txtSplit.length > 0)
									bookName = txtSplit[txtSplit.length -1];
							}
							CharSequence chs = getString(R.string.engine_openingBook) + "(" + bookName + ")" + ":\n";
							messageEngine = new SpannableStringBuilder(chs);

						}
						else
							messageEngineShort = "";
						CharSequence chs = messageEngine.toString() + bookMoves;
						messageEngine = new SpannableStringBuilder(chs);
					}

				}
			}
		}

		CharSequence messageShort = messageEngineShort;
		if (!messageInfo.equals(""))
		{

//		Log.i(TAG,"messageEngineShort: " + messageEngineShort);

			if (!ec.chessEnginesOpeningBook & !messageEngineShort.equals("") & !messageEngineShort.toString().startsWith("M"))
				{
					if (!messageEngineShort.toString().contains("0.00"))
					{
						if (!messageEngineShort.equals(""))
						{
							if (!messageEngineShort.toString().startsWith("-"))
								msgShort.setCompoundDrawablesWithIntrinsicBounds(R.drawable.btn_white, 0, 0, 0);
							else
								msgShort.setCompoundDrawablesWithIntrinsicBounds(R.drawable.btn_black, 0, 0, 0);
						}
					}
					else
						msgShort.setCompoundDrawablesWithIntrinsicBounds(R.drawable.btn_even, 0, 0, 0);

					if (messageShort.toString().startsWith("-") | messageShort.toString().startsWith("+"))
						messageShort = messageShort.subSequence(1, messageShort.length());

					if (messageInfo.toString().endsWith(">"))
					{
						String[] txtSplit = messageInfo.toString().split(" ");
						messageInfo = messageInfo.toString().replace(txtSplit[txtSplit.length -1], "");
						messageShort = messageShort + "  " + txtSplit[txtSplit.length -1];
					}
				}
				else
					msgShort.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
		}

		u.setTextViewColors(msgShort, cv, cv.COLOR_INFO_BACKGROUND_14, cv.COLOR_INFO_TEXT_15);
		u.setTextViewColors(msgShort2, cv, cv.COLOR_INFO_BACKGROUND_14, cv.COLOR_INFO_TEXT_15);

		messageInfo = messageInfo.toString().replace("  ", " ");

//		Log.i(TAG,"setInfoMessage(), messageShort: " + messageShort);
//		Log.i(TAG,"1 setInfoMessage(), messageInfo: >" + messageInfo + "<");

		if (messageInfo.toString().endsWith(" ")) {

			messageInfo = messageInfo.toString().substring(0, messageInfo.length() - 1);

//			Log.i(TAG,"2 setInfoMessage(), messageInfo: >" + messageInfo + "<");

		}

		msgShort.setText(" " + messageShort);
		msgShort2.setText(messageInfo);

		// msgMoves
		if (gc.errorMessage.equals(""))
			u.setTextViewColors(msgMoves, cv, cv.COLOR_MOVES_BACKGROUND_8, cv.COLOR_MOVES_TEXT_9);
		else
			u.setTextViewColors(msgMoves, cv, cv.COLOR_HIGHLIGHTING_22, cv.COLOR_MOVES_TEXT_9);
		boolean updateMoves = true;
		if (moveNotification != null)
		{
			if (moveNotification.equals("ENGINE_UPDATE"))
				updateMoves = false;
		}
		if (updateMoves)
		{
			int pieceId = userPrefs.getInt("user_options_gui_PieceNameId", 0);
			msgMoves.setText(gc.cl.history.createGameNotationFromHistory(gc.cl.history.MOVE_HISTORY_MAX_50000, false, true,
					false, false, true, 2, pieceId));
		}

//		Log.i(TAG,"setInfoMessage(), engineMsg: " + engineMsg);
//		Log.i(TAG,"setInfoMessage(), messageEngine: " + messageEngine);
//		Log.i(TAG,"setInfoMessage(), messageEngineShort: " + messageEngineShort);

		if (!messageEngine.toString().equals(""))
		{
			msgEngine.setVisibility(TextView.VISIBLE);
			u.setTextViewColors(msgEngine, cv, cv.COLOR_ENGINE_BACKGROUND_12, cv.COLOR_ENGINE_TEXT_13);
			if (userPrefs.getBoolean("user_options_enginePlay_EngineMessage", true))
			{
				msgEngine.setMaxLines(getMsgEngineLines());
				msgEngine.setLines(getMsgEngineLines());
				msgEngine.setText(messageEngine);
			}
			else
			{
				CharSequence msgS = getInfoShort(messageEngine);
				if (!msgS.toString().contains("0.00"))
				{
					if (!msgS.equals(""))
					{
						if (!msgS.toString().startsWith("-"))
							msgShort.setCompoundDrawablesWithIntrinsicBounds(R.drawable.btn_white, 0, 0, 0);
						else
							msgShort.setCompoundDrawablesWithIntrinsicBounds(R.drawable.btn_black, 0, 0, 0);
					}
				}
				else
					msgShort.setCompoundDrawablesWithIntrinsicBounds(R.drawable.btn_even, 0, 0, 0);
				if (msgS.toString().startsWith("-") | msgS.toString().startsWith("+"))
					msgS = msgS.subSequence(1, msgS.length());
				msgShort.setText(msgS);
			}
		}
		else
		{
			msgEngine.setText("");
		}

		if (!userPrefs.getBoolean("user_options_enginePlay_EngineMessage", true)) {
			msgEngine.setVisibility(TextView.GONE);
			msgEngine.setText("");
			if (gc.getValueFromFen(gc.cl.p_fen, 2).equals("w"))
				msgShort.setCompoundDrawablesWithIntrinsicBounds(R.drawable.btn_white, 0, 0, 0);
			else
				msgShort.setCompoundDrawablesWithIntrinsicBounds(R.drawable.btn_black, 0, 0, 0);
			msgShort.setText("");
		}

		if (msgMoves.getText().toString().equals(" *"))
		{
			setPlayModeButton(ec.chessEnginePlayMod, gc.cl.p_color, ec.chessEnginePaused, ec.chessEngineSearching, gc.isBoardTurn);
			return;
		}
		if (userPrefs.getBoolean("user_options_gui_moveList", true))
        {
            if (updateMoves)
                setSpanableToMsgMoves();
            else
				scrollMsgMoves();
        }
		else
		{
			String msg = getString(R.string.play_moveList) + " " + getString(R.string.disabled) + "\n\n";
			msg = msg + gc.cl.history.getGameData();
			msgMoves.setText(msg);
		}
		gc.cl.p_message = "";
		setPlayModeButton(ec.chessEnginePlayMod, gc.cl.p_color, ec.chessEnginePaused, ec.chessEngineSearching, gc.isBoardTurn);
	}

	public void setPlayModeButton(int playMode, CharSequence color, boolean isEnginePaused, boolean isEngineSearching, boolean isBoardTurn)
	{

		// btn_1
//		Log.i(TAG,"1 setPlayModeButton(), playMode: " +playMode + ", color: " +color + ", isEnginePaused: " +isEnginePaused + ", isEngineSearching: " +isEngineSearching + ", isBoardTurn: " +isBoardTurn);

		Bitmap drawBitmap = null;
		Bitmap imageBackground = null;

		if (imageHuman == null)
		{
			imageHuman = BitmapFactory.decodeResource(getResources(), R.drawable.btn1_human);
			imageComputer = BitmapFactory.decodeResource(getResources(), R.drawable.btn1_computer);
			imageAnalysis = BitmapFactory.decodeResource(getResources(), R.drawable.btn1_analysis);
			imageWhite = BitmapFactory.decodeResource(getResources(), R.drawable.btn1_white);
			imageBlack = BitmapFactory.decodeResource(getResources(), R.drawable.btn1_black);
			imageBlue = BitmapFactory.decodeResource(getResources(), R.drawable.btn_blue);
			imageYellow = BitmapFactory.decodeResource(getResources(), R.drawable.btn_yellow);
			imagePink = BitmapFactory.decodeResource(getResources(), R.drawable.btn_pink);
		}

		try
		{
			if (playMode == 5)
			{
				if (twoPlayerPaused)
					imageBackground = imageBlue;
				else
					imageBackground = imageYellow;
			}
			else
			{
				if (isEnginePaused)
				{
					if (playMode == 6)
						imageBackground = imageYellow;
					else
						imageBackground = imageBlue;
				}
				else
				{
					if (playMode == 4)
						imageBackground = imagePink;
					else
					{
						if (playMode == 1 | playMode == 2)
						{
							if ((playMode == 1 & color.equals("b")) | (playMode == 2 & color.equals("w")))
								imageBackground = imagePink;
							else
							{
								imageBackground = imageYellow;
								if ((userPrefs.getBoolean("user_options_enginePlay_Ponder", false) || ec.chessEnginesOpeningBook) && !msgEngine.getText().toString().equals(""))
									msgEngine.setVisibility(TextView.VISIBLE);
								else
									msgEngine.setVisibility(TextView.GONE);
							}
						}
						else
						{
							if (isEngineSearching)
								imageBackground = imagePink;
							else
								imageBackground = imageYellow;
						}
					}
				}
			}
			int xTopLeft = imageBackground.getWidth() / 10;
			int yTopLeft = imageBackground.getHeight() / 10;
			int xBottomLeft = imageBackground.getWidth() / 10;
			int yBottomLeft = (imageBackground.getHeight() / 2) + imageBackground.getHeight() / 16;
			int xTopRight = (imageBackground.getWidth() / 2) + imageBackground.getWidth() / 16;
			int yTopRight = imageBackground.getHeight() / 10;
			int xBottomRight = (imageBackground.getWidth() / 2) + imageBackground.getWidth() / 16;
			int yBottomRight = (imageBackground.getHeight() / 2) + imageBackground.getHeight() / 16;

			drawBitmap = Bitmap.createBitmap(imageBackground.getWidth(), imageBackground.getHeight(), Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(drawBitmap);
			canvas.drawBitmap(imageBackground, 0, 0, null);

			if (!isBoardTurn)
			{
				if (color.equals("w"))
					canvas.drawBitmap(imageWhite, xBottomRight, yBottomRight, null);
				else
					canvas.drawBitmap(imageBlack, xTopRight, yTopRight, null);
				if (!gc.isAutoPlay)
				{
					switch (playMode)
					{
						case 1:     // player vs engine
							canvas.drawBitmap(imageComputer, xTopLeft, yTopLeft, null);
							canvas.drawBitmap(imageHuman, xBottomLeft, yBottomLeft, null);
							break;
						case 2:     // engine vs player
							canvas.drawBitmap(imageHuman, xTopLeft, yTopLeft, null);
							canvas.drawBitmap(imageComputer, xBottomLeft, yBottomLeft, null);
							break;
					}
				}
			}
			else
			{
				if (color.equals("w"))
					canvas.drawBitmap(imageWhite, xTopRight, yTopRight, null);
				else
					canvas.drawBitmap(imageBlack, xBottomRight, yBottomRight, null);
				if (!gc.isAutoPlay)
				{
					switch (playMode)
					{
						case 1:     // player vs engine
							canvas.drawBitmap(imageHuman, xTopLeft, yTopLeft, null);
							canvas.drawBitmap(imageComputer, xBottomLeft, yBottomLeft, null);
							break;
						case 2:     // engine vs player
							canvas.drawBitmap(imageComputer, xTopLeft, yTopLeft, null);
							canvas.drawBitmap(imageHuman, xBottomLeft, yBottomLeft, null);
							break;
					}
				}
			}
			if (!gc.isAutoPlay)
			{
				switch (playMode)
				{
					case 3:     // computer vs computer
						canvas.drawBitmap(imageComputer, xTopLeft, yTopLeft, null);
						canvas.drawBitmap(imageComputer, xBottomLeft, yBottomLeft, null);
						break;
					case 4:     // analysis
						if (!isBoardTurn)
						{
							if (color.equals("w"))
								canvas.drawBitmap(imageAnalysis, xBottomLeft, yBottomLeft, null);
							else
								canvas.drawBitmap(imageAnalysis, xTopLeft, yTopLeft, null);
						} else
						{
							if (color.equals("w"))
								canvas.drawBitmap(imageAnalysis, xTopLeft, yTopLeft, null);
							else
								canvas.drawBitmap(imageAnalysis, xBottomLeft, yBottomLeft, null);
						}
						break;
					case 5:     // player vs player
						canvas.drawBitmap(imageHuman, xTopLeft, yTopLeft, null);
						canvas.drawBitmap(imageHuman, xBottomLeft, yBottomLeft, null);
						break;
					case 6:     // edit

						break;
				}
			}
			btn_1.setImageBitmap(drawBitmap);
		}
		catch (NullPointerException e) {e.printStackTrace();}
	}

	public void setSpanableToMsgMoves()
	{
		try
		{
			sb.clearSpans();
			sb.clear();
			sb.append(msgMoves.getText());
			int moveIdx = gc.cl.history.getMoveIdx();
			if (moveIdx == 0)
				moveIdx = 1;
			setInfoMoveValues(sb, moveIdx);
			if (infoMoveEndX > infoMoveStartX & !gc.cl.p_moveIsFirst)
			{
				if (gc.cl.p_moveText.equals(""))
					sb.setSpan(new BackgroundColorSpan(cv.getColor(cv.COLOR_MOVES_SELECTED_10)), infoMoveStartX, infoMoveEndX, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);	// green (move)
				else
					sb.setSpan(new BackgroundColorSpan(cv.getColor(cv.COLOR_MOVES_ANOTATION_11)), infoMoveStartX, infoMoveEndX, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);	// blue (move comment)
			}
			boolean startLine = true;
			boolean isMain = false;
			int startX = 0;
			int endX = 0;
			boolean isSpace = true;
			int cntSpace = 0;
			int firstIndent = 15;
			int nextIndent = 20;
			for (int i = 0; i < sb.length(); i++)
			{
				if (sb.charAt(i) == '\n')
				{
					if (isMain & endX > 0)
						sb.setSpan(new StyleSpan(Typeface.BOLD), startX, endX, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);	// bold
					else
					{
						sb.setSpan(new LeadingMarginSpan.Standard(cntSpace *firstIndent, cntSpace *nextIndent), startX, endX,
								Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
					}
					startLine = true;
					endX = 0;
				}
				else
				{
					if (startLine)
					{
						startX = i;
						startLine = false;
						if (sb.charAt(i) == ' ')
						{
							isMain = false;
							isSpace = true;
							cntSpace = 1;
						}
						else
							isMain = true;
					}
					else
					{
						endX = i;
						if (!isMain)
						{
							if (isSpace & sb.charAt(i) == ' ')
								cntSpace++;
							else
								isSpace = false;
						}
					}
				}
			}
			if (isMain & endX > 0)
				sb.setSpan(new StyleSpan(Typeface.BOLD), startX, sb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);	// bold
			msgMoves.setText(sb, TextView.BufferType.SPANNABLE);

			scrollMsgMoves();

		}
		catch (IndexOutOfBoundsException e) {e.printStackTrace();}
	}

	public void scrollMsgMoves()
	{
		scrlMsgMoves.post(new Runnable()
		{
			public void run()
			{
				try
				{
					Layout layout = msgMoves.getLayout();
					if (layout != null)
					{
						int scrollLine = layout.getLineForOffset(infoMoveStartX);
						if (scrollLine > 0)
							scrollLine--;


						if (!infoMoveIsSelected)
							scrlMsgMoves.scrollTo(0, layout.getLineTop(scrollLine));


					}
					infoMoveIsSelected = false;
				}
				catch (NullPointerException e) {e.printStackTrace();}
				catch (IndexOutOfBoundsException e) {}
			}
		});
	}

	public void setInfoMoveValuesFromView(View view, MotionEvent event)
	{	// set the values of a selected move(msgMoves)
		CharSequence moves = msgMoves.getText();
		CharSequence selectedMove = "";
		Layout layout = ((TextView) view).getLayout();
		int x = (int)event.getX();
		int y = (int)event.getY();
		if (layout!=null)
		{
			int line = layout.getLineForVertical(y);
			infoMoveIsSelected = true;
			int offset = layout.getOffsetForHorizontal(line, x);
			offset--;

//			Log.i(TAG, "offset, moves.length(): " + offset + ", " + moves.length());

			if (moves.length() <= 2 | offset < 1)
				return;
			if (moves.charAt(offset) == ' ')
				offset--;
			infoMoveStartX = offset;

//			Log.i(TAG, "offset: " + offset + " >" + moves.charAt(offset) + "<");

			try
			{
				boolean isDot = false;
				if (Character.isDigit(moves.charAt(infoMoveStartX)) | moves.charAt(infoMoveStartX) == '.')
				{
					for (int i = infoMoveStartX; i < moves.length(); i++)
					{

//						Log.i(TAG, "isDigit(): " + " >" + moves.charAt(i) + "<");

						if (moves.charAt(i) == '.')
						{
							isDot = true;
							infoMoveStartX = i +1;
						}
						else
						{
							if (isDot & (moves.charAt(i) == ' ' | moves.charAt(i) == '\n'))
							{
								infoMoveStartX = i +1;
								break;
							}
							else
							{
								if (!Character.isDigit(moves.charAt(i)))
									break;
							}
						}
					}
				}
				if (!isDot)
				{
					for (int i = offset -1; i > 0; i--)
					{
						infoMoveStartX = i;
						if (moves.charAt(i) == ' ' | moves.charAt(i) == '\n')
							break;
					}
				}
				for (int i = infoMoveStartX; i < moves.length(); i++)
				{
					if (moves.charAt(i) == ' ' | moves.charAt(i) == '\n')
						break;
					else
					{
						infoMoveEndX = i +1;
						selectedMove = selectedMove.toString() + moves.charAt(i);
					}
				}
				if (infoMoveStartX < 0  | infoMoveStartX > moves.length())
					infoMoveStartX = 0;
				if (infoMoveEndX > moves.length() -1)
					infoMoveEndX = moves.length() -1;

//				Log.i(TAG, "infoMoveStartX, infoMoveEndX: " + infoMoveStartX + ", " + infoMoveEndX);

			}
			catch (StringIndexOutOfBoundsException e)
			{
				e.printStackTrace();
			}

//			Log.i(TAG, "index, infoMoveStartX, infoMoveEndX, move: " + offset + ", " + infoMoveStartX + ", " + infoMoveEndX + ", " + selectedMove);

		}
	}

	public void setInfoMoveValues(SpannableStringBuilder moves, int moveIdx)
	{	// set the values for move selection from moveIdx
		boolean newToken = false;
		boolean setMoveEnd = false;
		int cntMove = 0;
		for (int i = 0; i < moves.length(); i++)
		{
			if (moves.charAt(i) == '(' | moves.charAt(i) == ')')
				cntMove++;	// control variations
			if (moves.charAt(i) == ' ' | moves.charAt(i) == '(' | moves.charAt(i) == ')' | moves.charAt(i) == '\n')
			{
				if (setMoveEnd)
					break;
				newToken = true;
			}
			else
			{
				if (setMoveEnd)
					infoMoveEndX = i +1;
				else
				{
					if (newToken)
					{
						newToken = false;
						if (Character.isLetter(moves.charAt(i)) | isFigurinNotation(moves.charAt(i)))
							cntMove++;
						if (cntMove == moveIdx)
						{
							infoMoveStartX = i;
							setMoveEnd = true;
						}
					}
				}
			}
		}
	}

	public int getMoveIdxFromInfo()
	{	// get moveIdx from msgMoves
		int moveIdx = 0;
		boolean newToken = false;
		CharSequence moves = msgMoves.getText();
		for (int i = 0; i < moves.length(); i++)
		{
			if (moves.charAt(i) == '(' | moves.charAt(i) == ')')
				moveIdx++;	// control variations
			if (moves.charAt(i) == ' ' | moves.charAt(i) == '(' | moves.charAt(i) == '\n')
				newToken = true;
			else
			{
				if (newToken)
				{
					newToken = false;
					if (Character.isLetter(moves.charAt(i)) | isFigurinNotation(moves.charAt(i)))
						moveIdx++;
					if (i >= infoMoveStartX)
						break;
				}
			}
		}

//		Log.i(TAG, "getMoveIdxFromInfo, infoMoveStartX, moveIdx: " + infoMoveStartX + ", " + moveIdx);

		return moveIdx;
	}

	protected boolean isFigurinNotation(char pieceLetter)
	{
		if (pieceLetter == gc.cl.history.HEX_K) return  true;
		if (pieceLetter == gc.cl.history.HEX_Q) return  true;
		if (pieceLetter == gc.cl.history.HEX_R) return  true;
		if (pieceLetter == gc.cl.history.HEX_B) return  true;
		if (pieceLetter == gc.cl.history.HEX_N) return  true;
		return false;
	}

	protected CharSequence getInfoShort(CharSequence engineMes)
	{

		if (ec.uciEngines == null)
			return  "";

		String infoShort = "";
		boolean isCurrentEngine = false;
		String[] lineSplit = engineMes.toString().split("\n");
		for (int i = 0; i < lineSplit.length; i++)
		{
			String engineName = "XXX";
			if (ec.uciEngines[ec.currentEngineId] != null)
				engineName = ec.uciEngines[ec.currentEngineId].uciEngineName;
			if (ec.chessEnginePlayMod == 4 && userPrefs.getBoolean("user_play_multipleEngines", true) && withMultiEngineAnalyse) {
				if (ec.analysisBestScoreEngineId < ec.engineCnt) {
					if (ec.uciEngines[ec.analysisBestScoreEngineId] != null) {
						engineName = ec.uciEngines[ec.analysisBestScoreEngineId].uciEngineName;
					}
				}
			}
//			Log.i(TAG, "1 getInfoShort(), engineName: " + engineName);

			if (lineSplit[i].contains(engineName))
				isCurrentEngine = true;
			if (isCurrentEngine && (lineSplit[i].contains("*1(") | lineSplit[i].contains(">1(")))
			{

//				Log.i(TAG, "2 getInfoShort()");

				String[] txtSplit = lineSplit[i].split(" ");
				if (txtSplit[0].startsWith("*1(") | txtSplit[0].startsWith(">1("))
				{

//					Log.i(TAG, "3 getInfoShort()");

					String bestMove = txtSplit[1];
					if (bestMove.contains("..."))
						bestMove = bestMove + txtSplit[2];
					String bestScore = txtSplit[0].replace("*1(", "");
					bestScore = bestScore.replace(">1(", "");
					bestScore = bestScore.replace(")", "");
					infoShort = bestScore + "  " + bestMove;
				}
				break;
			}
		}

//		Log.i(TAG, "getInfoShort(), engineMes: " + engineMes + "\ninfoShort: " + infoShort);

		return infoShort;
	}

	public CharSequence getGameOverMessage()
	{

//		Log.i(TAG, "getGameOverMessage() ");

		CharSequence mes = getString(R.string.cl_gameOver);
		if (gc.cl.p_mate)
			mes = mes + " (" + getString(R.string.cl_mate) + ")";
		if (gc.cl.p_stalemate)
			mes = mes + " (" + getString(R.string.cl_stealmate) + ")";
		if (gc.cl.p_auto_draw)
			mes = mes + " (" + getString(R.string.cl_draw) + ")";
		mes = mes + " (" + gc.cl.history.getGameTagValue("Result") + ")";

		return mes;

	}

	public void updateCurrentPosition(CharSequence message)
	{

//		Log.i(TAG, "updateCurrentPosition(), msgMoves: " + msgMoves + ", message: " + message + ", gc.cl.p_stat: " + gc.cl.p_stat);

//		Log.i(TAG, "updateCurrentPosition(), ec.chessEnginePaused: " + ec.chessEnginePaused + ", ec.getEngine().engineSearching(): " + ec.getEngine().engineSearching());

		if (ec.getEngine() == null)
			return;

		if (ec.chessEnginePlayMod <= 4) {
			if (ec.chessEnginePaused)
				setEnginePausePlayBtn(false, null);
			else {
				if (ec.getEngine().engineSearching())
					setEnginePausePlayBtn(true, true);
				else
					setEnginePausePlayBtn(true, null);
			}
		}

		if (msgMoves == null | message == null | gc.cl.p_stat.equals(""))
			return;

		try
		{
			gc.cl.getPositionFromMoveHistory(0, 0);
			if (gc.cl.p_stat.equals("1"))
			{
				if (!message.equals(""))
					gc.cl.p_message = message;
				updateGui();
			}
			else
				setInfoMessage("", null, "");
		}
		catch (IndexOutOfBoundsException e) {e.printStackTrace();}
		catch (NullPointerException e) 		{e.printStackTrace();}

	}

	public void startForceComputerMove()
	{

		if (ec.getEngine() == null) {
			return;
		}
//		Log.i(TAG, "startForceComputerMove()");

		if (!ec.chessEnginePaused)
		{
			switch (ec.chessEnginePlayMod)
			{
				case 1:     // player vs engine
				case 2:     // engine vs player
				case 3:     // computer vs computer
				case 4:     // analysis
					if ((ec.chessEnginePlayMod == 1 | ec.chessEnginePlayMod == 2) & ec.getEngine().engineState == EngineState.PONDER)
					{

//						Log.i(TAG, "startForceComputerMove()");

						stopSearchAndContinue(EngineState.STOP_MOVE_CONTINUE, "", true);

					}
					else
					{

//						Log.i(TAG, "startForceComputerMove(), ec.makeMove: " + ec.makeMove + ", playMod: " + ec.chessEnginePlayMod + "\ngc.fen: " + gc.fen);

						if (ec.makeMove)
							startStopEnginePlay(2);    // move & continue analysis
						else
						{

//							Log.i(TAG, "startForceComputerMove() Btn 3, ec.makeMove: " + ec.makeMove);

							ec.chessEnginePlayMod = 4;
							setPlayModPrefs(ec.chessEnginePlayMod);
							ec.chessEnginePaused = false;
							ec.chessEngineInit = false;
							updateCurrentPosition("");
							stopSearchAndRestart(false, false);
						}
					}
					break;
				case 6:     // edit
					break;
				default:
					Toast.makeText(this, getString(R.string.engine_paused), Toast.LENGTH_SHORT).show();
					startStopEnginePlay(0);
					break;
			}
		}
		gc.move = "";
	}

	public void setTurnBoard()
	{
		if (ec.chessEnginePlayMod == 1)
			gc.isBoardTurn = false;
		if (ec.chessEnginePlayMod == 2)
			gc.isBoardTurn = true;
	}

	public void startTurnBoard()
	{	// turn board and updateGui
		if (!gc.isBoardTurn)
			gc.isBoardTurn = true;
		else
			gc.isBoardTurn = false;
		handleEngineMessages(ec.currentEngineId, ec.uciEnginesMessage, ec.uciEnginesScore, "");
		updateCurrentPosition("");
	}

	public boolean startVariation()
	{	// start variation(option menu) and updateGui
		ArrayList<CharSequence> variList = gc.cl.history.getVariationsFromMoveHistory();
		gc.variationsList.clear();
		if (variList.size() > 0)
		{
			for (int i = 0; i < variList.size(); i++)
			{
				gc.variationsList.add(variList.get(i));
			}
			updateCurrentPosition("");
			return true;
		}
		return false;
	}

	public void showNewVariation(int id)
	{	// show selected variation and updateGui
		CharSequence moveIdx = "";
		if (id < gc.variationsList.size())
		{
			moveIdx = gc.chessMove.getVal(gc.variationsList.get(id), 1);
			gc.selectedVariationTitle = gc.chessMove.getVal(gc.variationsList.get(id), 5);
		}
		if (!moveIdx.equals(""))
		{
			try
			{
				gc.cl.history.setMoveIdx(Integer.parseInt(moveIdx.toString()));
				updateCurrentPosition("");
			}
			catch 	(NumberFormatException e) {}
		}
	}

	public void setTagResult(CharSequence result, CharSequence moveText)
	{	// set game result and updateGui
		gc.cl.history.setGameTag("Result", result.toString());
		gc.cl.history.setMoveText(moveText);
		gc.isGameOver = true;
		gc.isGameUpdated = false;
		updateCurrentPosition("");
		setInfoMessage(gc.cl.p_message, messageEngine, gc.cl.p_moveText);
	}

	public void setTagDate()
	{	// set gameDate in History
		gc.cl.history.setGameTag("Date", gc.cl.history.getDateYYYYMMDD().toString());
	}

	public void setTagGameData()
	{	// set engineData to c4aService and updateGui
		gc.cl.history.setGameTag("Event", ec.chessEngineEvent.toString());
		gc.cl.history.setGameTag("Site", ec.chessEngineSite.toString());
		gc.cl.history.setGameTag("Round", ec.chessEngineRound.toString());
		gc.cl.history.setGameTag("White", ec.chessEnginePlayerWhite.toString());
		gc.cl.history.setGameTag("Black", ec.chessEnginePlayerBlack.toString());
		if (!ec.chessEngineFen.toString().equals(""))
			gc.cl.history.setGameTag("FEN", ec.chessEngineFen.toString());
		updateCurrentPosition("");
	}

	//	HANDLER, TIMER		HANDLER, TIMER		HANDLER, TIMER		HANDLER, TIMER
	public Runnable mUpdateAutoplay = new Runnable()
{	// AutoPlay, watching a game move by move automatically: Handler(Timer)
	public void run()
	{
		if (gc.isAutoPlay)
		{
			if (gc.isGameOver)
				stopAutoPlay(false);
			else
			{
				nextMove(2, 0);
				handlerAutoPlay.postDelayed(mUpdateAutoplay, userPrefs.getInt("user_options_timer_autoPlay", gc.autoPlayValue));
			}
		}
		else
			stopAutoPlay(false);
		}
	};

	public void startStopAutoPlay()
	{	// start|stop autoPlay and updateGui
		if (!gc.isAutoPlay)
		{
			if (!ec.chessEnginePaused)
			{
				stopChessClock();
				setPauseEnginePlay(false);
			}
			gc.isAutoPlay = true;
			handlerAutoPlay.removeCallbacks(mUpdateAutoplay);
			handlerAutoPlay.postDelayed(mUpdateAutoplay, 100);
		}
		else
			stopAutoPlay(false);
	}

	public void stopThreads(boolean shutDown)
	{

//		Log.i(TAG, "stopThreads(), shutDown: " + shutDown + ", engineState: " + ec.getEngine().engineState);

		if (ec.getEngine() == null) {
			stopTimeHandler(shutDown);
			initPonder();
			return;
		}

		if (ec.getEngine().engineState == EngineState.PONDER & !shutDown)
		{
			setPauseEnginePlay(false);
		}
		else
			stopTimeHandler(shutDown);
		initPonder();
	}

	public void stopTimeHandler(boolean shutDown)
	{	// stop handler, thread, task and updateGui

//		Log.i(TAG, "stopTimeHandler(), shutDown: " + shutDown);

		stopChessClock();
		stopAutoPlay(shutDown);
		setPauseEnginePlay(shutDown);
	}

	public void stopAutoPlay(boolean shutDown)
	{	// stop Auto Play and updateGui
		if (gc.isAutoPlay)
		{
			if (!shutDown)
				gc.isAutoPlay = false;
			handlerAutoPlay.removeCallbacks(mUpdateAutoplay);
			setInfoMessage(getString(R.string.ccsMessageAutoPlayStopped), null, "");
		}
	}

	public Runnable mUpdateChessClock = new Runnable()
	{	// ChessClock: timer white/black
		public void run()
		{
			if (tc.clockIsRunning)
			{

				if (gc.isGameOver | gc.cl.p_variationEnd)
				{
					tc.stopChessClock(System.currentTimeMillis(), ec.chessEnginePlayMod);
					ec.chessEnginePaused = true;
					ec.chessEngineSearching = false;
					updateCurrentPosition("");

//					Log.i(TAG, "4 getGameOverMessage()");

					if (!ec.chessEngineMatch) {
						playSound(3, 0);
						setInfoMessage(getGameOverMessage(), null, null);
					}
				}
				else
				{
					boolean engineControlError = false;
					if (ec.chessEngineSearching)
					{
						if (System.currentTimeMillis() - engineControlTime >= 3000)
							engineControlError = true;
					}
					else
						engineControlTime = System.currentTimeMillis();

//					Log.i(TAG, "mUpdateChessClock, engineControlError: " + engineControlError + ", tc.timeWhite: " + tc.timeWhite + ", tc.timeBlack: " + tc.timeBlack);

					if 	(		ec.chessEnginePlayMod == 1 & gc.cl.p_color.equals("b") & tc.timeBlack == 0
							| 	ec.chessEnginePlayMod == 2 & gc.cl.p_color.equals("w") & tc.timeWhite == 0
							| 	engineControlError
						)
					{
						stopChessClock();
						if (ec.chessEnginePlayMod == 1 & gc.cl.p_color.equals("b") & tc.timeBlack == 0)
                            tc.timeBlack = 1000;
						if (ec.chessEnginePlayMod == 2 & gc.cl.p_color.equals("w") & tc.timeWhite == 0)
                            tc.timeWhite = 1000;

						if (engineControlError)
						{
							ec.chessEngineSearching = false;
							stopComputerThinking(false, false);
							ec.chessEnginePaused = true;
							ec.chessEngineInit = true;
							updateCurrentPosition("");

//							Log.i(TAG, "4 getEngineThinkingMessage(), engine_paused");

							setInfoMessage(getString(R.string.engine_paused), null, null);
						}
						else
						{
							ec.chessEngineSearching = true;
							ec.chessEnginePaused = false;
							updateGui();
							if (!gc.cl.p_fen.equals(""))
								stopSearchAndContinue(EngineState.STOP_CONTINUE, gc.cl.p_fen, true);
						}

						return;

					}
					updateTime(gc.cl.p_color);
				}
			}
		}
	};


	//  NEW ENGINE, searchThread		NEW ENGINE, searchThread		NEW ENGINE, searchThread		NEW ENGINE, searchThread
	void startSearchThread(int engineId, String fen, String moves, String startFen, boolean isSearch, boolean isAnalyze, String ponderMove)
	{

//		Log.i(TAG, "startSearchThread(), updateBoardView()");
//
//		boardView.updateBoardView(gc.fen, gc.isBoardTurn, BoardView.ARROWS_NONE, null, null, null, null,
//				null,null, false, userPrefs.getBoolean("user_options_gui_BlindMode", false));

		ec.uciEngines[engineId].initPv();

//		Log.i(TAG, "startSearchThread(), setInfoMessage(getEngineThinkingMessage(), ec.chessEngineMatch: " + ec.chessEngineMatch);

		if (!ec.chessEngineMatch)
		{
			if (userPrefs.getBoolean("user_options_enginePlay_OpeningBook", true) & ec.chessEnginesOpeningBook) {

//				Log.i(TAG, "startSearchThread(), ec.chessEngineMatch: " + ec.chessEngineMatch + ", ec.chessEnginesOpeningBook: " + ec.chessEnginesOpeningBook);

				setInfoMessage(getString(R.string.engine_openingBook), null, null);
			}
		}
		ec.searchId++;
		long now = System.currentTimeMillis();
		boolean drawOffer = false;
		int wTime = 0;
		int bTime = 0;
		int wInc = 0;
		int bInc = 0;
		int movesToGo = 1;
		int moveTime = 0;
		switch (userPrefs.getInt("user_options_timeControl", 1))
		{
			case 1:     // game clock
			case 3:     // sand glass
				wTime = tc.timeWhite;
				bTime = tc.timeBlack;
				wInc = tc.bonusWhite;
				bInc = tc.bonusBlack;
				movesToGo = 00;
				break;
			case 2:     // move time
				setMoveTime();
				moveTime = userPrefs.getInt("user_time_engine_move", OptionsTimeControl.TIME_ENGINE_MOVE);
				break;
			case 4:     // no time control
				break;
		}
		if (wTime < 1)	wTime = 500;
		if (bTime < 1)	bTime = 500;
		int elo = ec.uciEngines[engineId].uciElo;
		if (isAnalyze || ec.chessEnginePlayMod == 3)
			elo = ec.uciEngines[engineId].uciEloMax;

		String firstMove = "";
		if (fen.equals(gc.standardFen) & userPrefs.getBoolean("user_options_enginePlay_RandomFirstMove", false)) {
			firstMove = ec.uciEngines[engineId].getRandomFirstMove().toString();
			if (!firstMove.equals("")) {
				ec.uciEngines[engineId].engineState = EngineState.BOOK;
				handleSearchResult(ec.currentEngineId, fen, firstMove, "");
				return;
			}
		}

		ec.chessEnginesOpeningBook = false;

		if 	(		userPrefs.getBoolean("user_options_enginePlay_OpeningBook", true)
				&& 	ec.chessEnginePlayMod != 4
				&& 	ec.uciEngines[engineId].engineState != EngineState.PONDER
				&& 	!(ec.chessEngineMatch && userPrefs.getBoolean("user_play_eve_autoCurrentGame", false))
			)
		{
			String bookFen = getBookMoves(fen);
			if (!bookFen.equals(""))
			{
				fen = bookFen;
				if (ec.engineCnt == 2) {
					if (gc.getValueFromFen(fen, 2).equals("w"))
						engineId = 0;
					else
						engineId = 1;
				}
//				if (!ec.chessEngineMatch)
//					playSound(1, 0);
				playSound(1, 0);
				if (!tc.clockIsRunning & ec.chessEnginePlayMod == 1)
					startChessClock();
				if (ec.chessEnginePlayMod != 3)
					ec.chessEnginesOpeningBook = true;
				setEnginePausePlayBtn(true, null);
				ec.uciEngines[engineId].engineState = EngineState.IDLE;
				chessEngineSearch(engineId, fen, "", "");

				return;

			}
		}

		if (!tc.clockIsRunning | ec.chessEnginePlayMod == 4)
		{
			if (ec.chessEnginePlayMod == 4)
				initChessClock(tc, userPrefs.getInt("user_options_timeControl", 1), ec.chessEnginePlayMod);
			startChessClock();
		}

		if (ec.chessEnginePlayMod == 4)
			ec.chessEngineAnalysis = true;
		else
			ec.chessEngineAnalysis = false;

//		Log.i(TAG, "startSearchThread(), bookMove: " + bookMove + ", engineState: " + ec.uciEngines[engineId].engineState + " isGoPonder: " + isGoPonder + ", chess960ID: " + gc.cl.p_chess960ID);
//		Log.i(TAG, "startSearchThread(), ec.engineCnt: " + ec.engineCnt + ", engineState: " + ec.uciEngines[engineId].engineState + " isGoPonder: " + isGoPonder + ", chess960ID: " + gc.cl.p_chess960ID);

		UciEngine.SearchRequest sr = UciEngine.SearchRequest.searchRequest(
			ec.uciEngines[engineId].engineId, ec.searchId, now, ec.engineCnt,
			fen, moves, startFen, gc.cl.p_chess960ID, firstMove, "",
			drawOffer, isSearch, isAnalyze,
			wTime, bTime, wInc, bInc, movesToGo, moveTime,
			isGoPonder, ponderMove,
			ec.uciEngines[engineId].engineName, elo);

		ec.uciEngines[engineId].startSearch(sr);

	}

	String getBookMoves(String fen) {

		String bookMove;
		String newFen = "";
		String loopFen = fen;
		boolean looping = true;

		while(looping)
		{

			Move bm = null;
			try { bm = ec.book.getBookMove(TextIO.readFEN(loopFen));	}
			catch (ChessParseError e1) { looping = false; }
			if (bm != null) {
				bookMove = bm.toString();
				if (!bookMove.equals("")) {
					loopFen = (String) chessEngineGui(loopFen, bookMove, false);
					if (!loopFen.equals("")) {
						newFen = (String) loopFen;
					}
					else
						looping = false;
				}
			}
			else
				looping = false;

			if (ec.chessEnginePlayMod != 3)
				looping = false;

		}

		if (!newFen.equals(""))
			updateGui();

		return newFen;

	}

	//  NEW ENGINE, EngineListener		NEW ENGINE, EngineListener		NEW ENGINE, EngineListener		NEW ENGINE, EngineListener
	final class EngineListener implements ccc.chess.gui.chessforall.EngineListener {

		@Override
		public void notifyEngineInitialized(int engineId) {

//			Log.i(TAG, "EngineListener, notifyEngineInitialized(), engineId: " + engineId);

			MainActivity.this.runOnUiThread(() -> handleEngineInitialized(engineId));

		}

		@Override
		public void notifySearchResult(int engineId, int id, String fen, String bestmove, String ponder) {

//			Log.i(TAG, "EngineListener, notifySearchResult(), engineId: " + engineId + ", fen: " + fen + ", bestmove: " + bestmove + ", ponder: " + ponder);

			MainActivity.this.runOnUiThread(() -> handleSearchResult(engineId, fen, bestmove, ponder));

		}

		@Override
		public void notifyPV(int engineId, int id, String engineMessage, int score, String searchDisplayMoves) {

//			Log.i(TAG, "EngineListener, notifyPV(), engineId: " + engineId + ", id: " + id + ", engineMessage: " + engineMessage + ", searchDisplayMoves: " + searchDisplayMoves);
//			Log.i(TAG, "EngineListener, notifyPV(), engineId: " + engineId + ", id: " + id + ", engineMessage: " + engineMessage + ", score: " + score);
//			Log.i(TAG, "EngineListener, notifyPV(), engineId: " + engineId + ", id: " + id);
//			Log.i(TAG, "notifyPV(), engineId: " + engineId + ", engineState: " + ec.uciEngines[engineId].engineState);

//			if (!ec.uciEngines[engineId].engineSearching())
//				return;

			if (ec.chessEnginePlayMod == 3 && ec.engineCnt == 2 && !userPrefs.getBoolean("user_options_enginePlay_Ponder", false)) {
				if (engineId == 0) {
					ec.uciEnginesMessage[1] = "";
					ec.uciEnginesScore[1] = 0;
				}
				if (engineId == 1) {
					ec.uciEnginesMessage[0] = "";
					ec.uciEnginesScore[0] = 0;
				}
			}
			ec.uciEnginesMessage[engineId] = engineMessage;
			ec.uciEnginesDisplayMoves[engineId] = searchDisplayMoves;
			ec.uciEnginesScore[engineId] = score;

			ec.displayMoves = searchDisplayMoves;
			if (ec.chessEnginePlayMod == 4 && userPrefs.getBoolean("user_play_multipleEngines", true) && withMultiEngineAnalyse) {
				ec.displayMoves = "";
//				if (ec.uciEnginesDisplayMoves != null) {
				if (ec.uciEnginesDisplayMoves != null && ec.uciEngines[engineId].engineSearching()) {
					for (int i = 0; i < ec.engineCnt; i++) {
						if (ec.uciEnginesDisplayMoves[i] != null && !ec.uciEnginesDisplayMoves[i].equals("")) {
							String[] split = ec.uciEnginesDisplayMoves[i].split(" ");
							if (split.length > 0)
								ec.displayMoves = ec.displayMoves + split[0] + " ";
						}
						else
							ec.displayMoves = ec.displayMoves + "null ";
					}
				}
//				else {
//					if (ec.uciEnginesDisplayMoves[engineId] != null)
//						ec.uciEnginesDisplayMoves[engineId] = "";
//				}
			}

			if (ec.uciEnginesDisplayMoves[engineId] != null) {
				if (!ec.uciEngines[engineId].engineSearching()) {
					ec.uciEnginesDisplayMoves[engineId] = "";
					ec.displayMoves = "";
				}
			}
			if (ec.uciEnginesMessage[engineId] != null) {
				if (!ec.uciEngines[engineId].engineSearching())
					ec.uciEnginesMessage[engineId] = "";
			}

//			MainActivity.this.runOnUiThread(() -> handleEngineMessages(engineId, ec.uciEnginesMessage, ec.uciEnginesScore, searchDisplayMoves));
			MainActivity.this.runOnUiThread(() -> handleEngineMessages(engineId, ec.uciEnginesMessage, ec.uciEnginesScore, ec.displayMoves));

		}

		@Override
		public void notifyStop(int engineId, int id, EngineState engineState, String fen, String bestmove) {

//			Log.i(TAG, "EngineListener, notifyStop(), engineId: " + engineId + ", engineState: " + engineState);

			MainActivity.this.runOnUiThread(() -> handleStop(engineId, engineState, fen, bestmove));

		}

		@Override
		public void reportEngineError(int engineId, String errMsg) {

			MainActivity.this.runOnUiThread(() -> handleEngineError(engineId, errMsg));

		}
	}

	public void initPonder()
	{
		isGoPonder = false;
	}

	public void enginePlayPonder(CharSequence fen)
	{

		if (ec.getEngine() == null) {
			return;
		}

//		Log.i(TAG, "1 enginePlayPonder(), fen: " + fen + ", ponderMove: " + ec.getEngine().ponderMove);

		isGoPonder = false;
		if 	(		userPrefs.getBoolean("user_options_enginePlay_Ponder", false)
				&	ec.getEngine().isUciPonder
				& 	(ec.chessEnginePlayMod == 1 | ec.chessEnginePlayMod == 2)
				& 	!gc.cl.p_fen.equals("")
			)
		{

//			Log.i(TAG, "enginePlayPonder(), fen: " + fen);

			if (!ec.getEngine().ponderMove.equals("")) {
				isGoPonder = true;
				chessEngineSearch(ec.currentEngineId, fen, "", ec.getEngine().ponderMove);
			}

			setInfoMessage(getString(R.string.player_move), null, null);

		}

	}

	public void engineStopPonder(CharSequence fen, int playMod)
	{

		if (ec.getEngine() == null) {
			return;
		}

//		Log.i(TAG, "1 engineStopPonder(), engineState: " + ec.getEngine().engineState);

		if (ec.getEngine().engineState == EngineState.PONDER)
		{

//			Log.i(TAG, "engineStopPonder(), gc.cl.p_fen: " + fen + ", playMod: " + playMod);

			if 	(		(gc.getValueFromFen(fen, 2).equals("b") & playMod == 1)
					|	(gc.getValueFromFen(fen, 2).equals("w") & playMod == 2)
				)
			{

//				Log.i(TAG, "2 engineStopPonder(), ponderMove: " + ec.getEngine().ponderMove + ", p_move: " + gc.cl.p_move);

				ec.uciEnginesMessage[ec.currentEngineId] = "";
				if (ec.getEngine().applyPonderhit(gc.cl.p_move.toString(), gc.cl.p_fen.toString()))
					stopSearchAndContinue(EngineState.STOP_CONTINUE, fen, true);

			}
			else
			{
				setPauseEnginePlay(false);
				startChessClock();
				messageEngineShort  = "";
				setInfoMessage(getString(R.string.player_move), null, null);
				initPonder();
			}
		}
	}

	public CharSequence chessEngineGui(CharSequence taskFen, CharSequence bestMove, boolean isUpdate)
	{

//		Log.i(TAG, "chessEngineGui(), taskFen: " + taskFen + "    bestMove: " + bestMove);

		CharSequence newFen = "";
		if (bestMove.length() > 4)			// promotion
		{
			if (bestMove.charAt(4) == 'q') bestMove = ((String) bestMove).substring(0, 4) + 'Q';
			if (bestMove.charAt(4) == 'r') bestMove = ((String) bestMove).substring(0, 4) + 'R';
			if (bestMove.charAt(4) == 'b') bestMove = ((String) bestMove).substring(0, 4) + 'B';
			if (bestMove.charAt(4) == 'n') bestMove = ((String) bestMove).substring(0, 4) + 'N';
		}
		gc.cl.newPositionFromMove(taskFen, bestMove, true);

//		Log.i(TAG, "1 chessEngineGui(), updateGui(), taskFen: " + taskFen + "    gc.cl.p_stat: " + gc.cl.p_stat);

		if (gc.cl.p_stat.equals("1"))
		{
			if (ec.chessEnginePlayMod <= 3 & userPrefs.getBoolean("user_options_gui_FlipBoard", false))
				startTurnBoard();
			if (ec.chessEnginePlayMod != 3)
				ec.chessEngineSearching = false;
			gc.setGameOver(gc.cl.history.getGameTagValue("Result"));

			if (isUpdate)
				updateGui();

			newFen = gc.cl.p_fen;

			if (gc.isGameOver)
				return "";

			gc.move = "";
		}
		else
		{
			gc.cl.p_fen = "";
			gc.cl.p_color = "";

			if (isUpdate)
				updateGui();

		}

//		Log.i(TAG, "2 chessEngineGui(), updateGui(), taskFen: " + taskFen + "    gc.cl.p_stat: " + gc.cl.p_stat);

		return newFen;
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
			s = s + Integer.toString(s1) + ".0" + Integer.toString(s2);
		else
			s = s + Integer.toString(s1) + "." + Integer.toString(s2);
		return s;
	}

	//	HELPERS		HELPERS		HELPERS		HELPERS		HELPERS		HELPERS
	public int getChessFieldSize()
	{	// graphic on chess board -  get chess field size
		int size = 29;		// small
		Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		displayWidth = display.getWidth();
		boardSize = Math.min(display.getWidth(), display.getHeight());
		if (displayWidth >= 320)
			size = (displayWidth -16) / 8;
		if (displayWidth >= 0)
			minScrollingWidth = boardSize / 6;

//		Log.i(TAG, "displayWidth, minScrollingWidth: " + displayWidth + ", " + minScrollingWidth);

		return size;
	}

	public void initInfoArrays()
	{

		infoPv = new ArrayList<CharSequence>();
		infoMessage = new ArrayList<CharSequence>();
		for (int i = 0; i < getMultiPv(); i++)
		{
			infoPv.add("");
			infoMessage.add("");
		}
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

	public String getColorName(int colorId)
	{
		String colorName = "";
		switch (colorId)
		{
			case 0:
				colorName = getString(R.string.menu_colorsettings_brown);
				if (!userPrefs.getString("colors_0", "").equals(""))
				{
					String[] split = userPrefs.getString("colors_0", "").split(" ");
					if (!split[0].equals("?"))
						colorName = split[0];
				}
				break;
			case 1:
				colorName = getString(R.string.menu_colorsettings_violet);
				if (!userPrefs.getString("colors_1", "").equals(""))
				{
					String[] split = userPrefs.getString("colors_1", "").split(" ");
					if (!split[0].equals("?"))
						colorName = split[0];
				}
				break;
			case 2:
				colorName = getString(R.string.menu_colorsettings_grey);
				if (!userPrefs.getString("colors_2", "").equals(""))
				{
					String[] split = userPrefs.getString("colors_2", "").split(" ");
					if (!split[0].equals("?"))
						colorName = split[0];
				}
				break;
			case 3:
				colorName = getString(R.string.menu_colorsettings_blue);
				if (!userPrefs.getString("colors_3", "").equals(""))
				{
					String[] split = userPrefs.getString("colors_3", "").split(" ");
					if (!split[0].equals("?"))
						colorName = split[0];
				}
				break;
			case 4:
				colorName = getString(R.string.menu_colorsettings_green);
				if (!userPrefs.getString("colors_4", "").equals(""))
				{
					String[] split = userPrefs.getString("colors_4", "").split(" ");
					if (!split[0].equals("?"))
						colorName = split[0];
				}
				break;
		}
		return colorName;
	}

    public void setStringsValues()
	{
    	stringValues.clear();
    	stringValues.add(0, "");
    	stringValues.add(1, getString(R.string.cl_unknownPiece));
    	stringValues.add(2, getString(R.string.cl_wrongBasePosition));
    	stringValues.add(3, getString(R.string.cl_resultWhite));
    	stringValues.add(4, getString(R.string.cl_resultBlack));
    	stringValues.add(5, getString(R.string.cl_resultDraw));
    	stringValues.add(6, getString(R.string.cl_gameOver));
    	stringValues.add(7, getString(R.string.cl_50MoveRule));
    	stringValues.add(8, getString(R.string.cl_position3Times));
    	stringValues.add(9, getString(R.string.cl_moveError));
    	stringValues.add(10, getString(R.string.cl_moveWhite));
    	stringValues.add(11, getString(R.string.cl_moveBlack));
    	stringValues.add(12, getString(R.string.cl_moveMultiple));
    	stringValues.add(13, getString(R.string.cl_moveNo));
    	stringValues.add(14, getString(R.string.cl_moveWrong));
    	stringValues.add(15, getString(R.string.cl_mate));
    	stringValues.add(16, getString(R.string.cl_stealmate));
    	stringValues.add(17, getString(R.string.cl_check));
    	stringValues.add(18, getString(R.string.cl_castlingError));
    	stringValues.add(19, getString(R.string.cl_castlingCheck));
    	stringValues.add(20, getString(R.string.cl_emptyField));
    	stringValues.add(21, getString(R.string.cl_kingError));
    	stringValues.add(22, getString(R.string.cl_fenError));
    	stringValues.add(23, getString(R.string.cl_notationError));
    	stringValues.add(24, getString(R.string.cl_variationError));
    	stringValues.add(25, getString(R.string.cl_checkOponent));
    	
    	stringValues.add(26, getString(R.string.nag_1));
    	stringValues.add(27, getString(R.string.nag_2));
    	stringValues.add(28, getString(R.string.nag_3));
    	stringValues.add(29, getString(R.string.nag_4));
    	stringValues.add(30, getString(R.string.nag_5));
    	stringValues.add(31, getString(R.string.nag_6));
    	stringValues.add(32, getString(R.string.nag_7));
    	stringValues.add(33, getString(R.string.nag_8));
    	stringValues.add(34, getString(R.string.nag_9));
    	stringValues.add(35, getString(R.string.nag_10));
    	stringValues.add(36, getString(R.string.nag_11));
    	stringValues.add(37, getString(R.string.nag_12));
    	stringValues.add(38, getString(R.string.nag_13));
    	stringValues.add(39, getString(R.string.nag_14));
    	stringValues.add(40, getString(R.string.nag_15));
    	stringValues.add(41, getString(R.string.nag_16));
    	stringValues.add(42, getString(R.string.nag_17));
    	stringValues.add(43, getString(R.string.nag_18));
    	stringValues.add(44, getString(R.string.nag_19));
	}

	protected void startMsgMoveAction(View view, MotionEvent event)
	{

//		Log.i(TAG, "startMsgMoveAction()");

		int lastMoveIdx = gc.cl.p_moveIdx;
		setInfoMoveValuesFromView(view, event);
		nextMove(19, getMoveIdxFromInfo());		// set moveIdx
		if (!ec.chessEnginePaused & gc.cl.p_stat.equals("1"))
		{
			if (ec.chessEnginePlayMod <= 3)
				pauseStopPlay(false);
			else {
				if (!gc.cl.p_fen.equals(""))
					stopSearchAndContinue(EngineState.STOP_CONTINUE, gc.cl.p_fen, true);
			}
		}
		else
		{
			if (!gc.cl.p_moveText.equals("") & gc.cl.p_moveIdx == lastMoveIdx)
			{
				removeDialog(MOVE_NOTIFICATION_DIALOG);
				showDialog(MOVE_NOTIFICATION_DIALOG);
			}
		}
	}
	protected void startBoardMoveAction(MotionEvent event)
	{
		int screenXY[] = new int[2];
		boardView.getLocationOnScreen(screenXY);
		if (event.getAction() == MotionEvent.ACTION_UP)
		{
			int pos = boardView.getPositionFromTouch((int) event.getRawX(), (int) event.getRawY(), screenXY[0], screenXY[1]);

//			Log.i(TAG, "startBoardMoveAction(), onScreenTouch, x: " + event.getRawX() + ", y: " + event.getRawY());
//			Log.i(TAG, "startBoardMoveAction(), boardView.getLocationOnScreen(), x: " + screenXY[0] + ", y: " + screenXY[1] + ", pos: " + pos);
//			Log.i(TAG, "startBoardMoveAction(), ec.chessEngineSearching: " + ec.chessEngineSearching);
//			Log.i(TAG, "startBoardMoveAction(), gc.fen: " + gc.fen);

			gc.isMoveError = false;
			if (ec.chessEngineSearching & !ec.chessEnginePaused)
			{
				if 	(		(ec.chessEnginePlayMod == 1 & gc.getValueFromFen(gc.fen, 2).equals("b"))
						| 	(ec.chessEnginePlayMod == 2 & gc.getValueFromFen(gc.fen, 2).equals("w"))
					)
					Toast.makeText(this, getString(R.string.engine_searching), Toast.LENGTH_SHORT).show();
				else
					moveAction(pos);
			}
			else
			{
				if (!gc.isAutoPlay)
					moveAction(pos);
			}
		}
	}

	protected void moveAction(int position)
	{

//		Log.i(TAG, "moveAction(), position: " + position);

		if (gc.cl.p_mate | gc.cl.p_stalemate)	// mate, steal mate?
		{

//			Log.i(TAG, "5 getGameOverMessage()");

			setInfoMessage(getGameOverMessage(), null, null);
			return;
		}
		if (!gc.isGameOver & !gc.cl.p_variationEnd)	// move actions only on: edit/play
		{
			CharSequence field = "";
			try
			{
				field = boardView.getChessField(position, gc.isBoardTurn);

//				Log.i(TAG, "moveAction(), gc.cl.quickMove: " + gc.cl.quickMove);

				if (!gc.move.equals(field))
				{
					if (gc.cl.quickMove.length() == 4
							& gc.cl.quickMove.toString().endsWith(gc.move.toString())
							& gc.cl.quickMove.toString().startsWith(field.toString())
					)
						gc.move = gc.cl.quickMove;
					else
						gc.move = gc.move.toString() + field;
					gc.cl.quickMove = "";
				}
				if (gc.move.length() >= 4)
				{
					if (gc.move.subSequence (0, 2).equals(gc.move.subSequence (2, 4)))
						gc.move = gc.move.subSequence (2, gc.move.length());
				}

//				Log.i(TAG, "A moveAction(), gc.move: " + gc.move + ", gc.cl.quickMove: " + gc.cl.quickMove + ", gc.cl.p_stat: " + gc.cl.p_stat);

				if 	(!userPrefs.getBoolean("user_options_gui_quickMove", true) && gc.move.length() == 2 && !gc.cl.isMv1)
				{

//					Log.i(TAG, "B moveAction(), user_options_gui_quickMove");

					gc.cl.isMv1 = true;
					gc.cl.p_possibleMoveList.clear();
					gc.cl.p_possibleMoveToList.clear();
					gc.cl.newPositionFromMove(gc.fen, gc.move, false);

//					Log.i(TAG, "A moveAction(), user_options_gui_quickMove");
//					Log.i(TAG, "A moveAction(), gc.fen: " + gc.fen);
//					Log.i(TAG, "A moveAction(), field: " + field + ", gc.move: " + gc.move);
//					Log.i(TAG, "A gc.cl.p_possibleMoveList: " + gc.cl.p_possibleMoveList.size());
//					Log.i(TAG, "A gc.cl.p_possibleMoveToList: " + gc.cl.p_possibleMoveToList.size());
//					Log.i(TAG, "A gc.cl.p_hasPossibleMovesTo: " + gc.cl.p_hasPossibleMovesTo + ", gc.cl.p_stat: " + gc.cl.p_stat);

					if (	gc.cl.p_possibleMoveList.size() == 0
							&& gc.cl.p_possibleMoveToList.size() == 0
							&& !gc.cl.p_stat.equals("9"))
					{
						boardView.updateBoardView(gc.cl.p_fen, gc.isBoardTurn, BoardView.ARROWS_NONE, null, null, gc.cl.p_possibleMoveList, gc.cl.p_possibleMoveToList,
								gc.cl.quickMove, gc.move, userPrefs.getBoolean("user_options_gui_Coordinates", false),
								userPrefs.getBoolean("user_options_gui_BlindMode", false));
						gc.cl.p_possibleMoveList.clear();
						gc.cl.p_possibleMoveToList.clear();
						return;
					}
				}
				else
					gc.cl.isMv1 = false;

//				Log.i(TAG, "C moveAction(), gc.move: " + gc.move + ", gc.cl.quickMove: " + gc.cl.quickMove + ", gc.cl.p_stat: " + gc.cl.p_stat);
//				Log.i(TAG, "1 moveAction(), fen: " + gc.fen);

				gc.cl.newPositionFromMove(gc.fen, gc.move, true);

				if (gc.cl.p_stat.equals("5"))						// Promotion Dialog
				{
					updateTime(gc.cl.p_color);
					gc.promotionMove = gc.cl.p_move;
					gc.move = "";
					c4aShowDialog(PROMOTION_DIALOG);
				}
				else
				{
					if (gc.cl.p_stat.equals("2") | gc.cl.p_stat.equals("3") | gc.cl.p_stat.equals("9"))	// error message
					{
						if (!(gc.cl.p_hasPossibleMovesTo & gc.cl.p_stat.equals("2")))
							playSound(2, 0);
						gc.move = "";
						if (gc.move.length() >= 2)
						{
							gc.cl.p_hasPossibleMoves = false;
							updateGui();
							return;
						}
					}

					if (gc.cl.p_auto_draw)
					{
						if (!ec.chessEnginePaused)
							stopSearchAndContinue(EngineState.STOP_IDLE, "", true);
						updateGui();

//						Log.i(TAG, "6 getGameOverMessage()");

						setInfoMessage(getGameOverMessage(), null, null);
						return;
					}
					if (gc.cl.p_stat.equals("1"))
					{

						gc.fen = gc.cl.p_fen;

//						Log.i(TAG, "2 moveAction(), fen: " + gc.fen);

						playSound(1, 0);
						if (userPrefs.getBoolean("user_options_gui_FlipBoard", false))
							startTurnBoard();
						gc.isGameUpdated = false;
						gc.move = "";
						if (ec.chessEnginePaused) {

//							Log.i(TAG, "6 getEngineThinkingMessage(), engine_paused");

							setInfoMessage(getString(R.string.engine_paused), null, null);
						}
					}

					gc.setGameOver(gc.cl.history.getGameTagValue("Result"));
					if (!(ec.chessEnginePlayMod == 5 | ec.chessEnginePlayMod == 6))
					{
						if (gc.cl.p_stat.equals("1") & !gc.isGameOver & !gc.cl.p_variationEnd)
							ec.chessEngineSearching = true;
					}
					if (!tc.clockIsRunning & ec.chessEnginePlayMod == 5 & gc.cl.p_stat.equals("1"))
					{
						startChessClock();
					}
					if (ec.chessEnginePlayMod == 5 & twoPlayerPaused & gc.cl.p_stat.equals("1"))
					{
						startChessClock();
					}
					updateGui();

//					Log.i(TAG, "D moveAction(), gc.move: " + gc.move + ", ec.chessEngineSearching: " + ec.chessEngineSearching);

					if 	(		ec.chessEngineSearching & gc.cl.p_stat.equals("1")
							& 	!gc.isGameOver & !gc.cl.p_variationEnd
						)
					{
						if (!ec.chessEnginePaused & (ec.chessEnginePlayMod == 3 | ec.chessEnginePlayMod == 4))
						{
							if (!gc.cl.p_fen.equals(""))
								stopSearchAndContinue(EngineState.STOP_CONTINUE, gc.cl.p_fen, true);
						}
						else
						{
							isGoPonder = false;
							if (ec.getEngine() != null) {
								if (ec.getEngine().engineState != EngineState.PONDER)
									chessEngineSearch(ec.currentEngineId, gc.cl.p_fen, "", "");
								else
									engineStopPonder(gc.cl.p_fen, ec.chessEnginePlayMod);
							}
						}
					}

//					Log.i(TAG, "3 getGameOverMessage()");

					if (gc.isGameOver | gc.cl.p_variationEnd)
						setInfoMessage(getGameOverMessage(), null, null);
				}
			}
			catch (NullPointerException e) {e.printStackTrace();}

//			Log.i(TAG, "E moveAction(), stat, gc.move, move, hasMoves: " + gc.cl.p_stat + ", " + gc.move + ", " + gc.cl.p_move + ", " + gc.cl.p_hasPossibleMoves);

		}
	}

	public void setPlayerData()
	{
		if (!gc.isBoardTurn)
		{
			lblPlayerNameA.setText(gc.cl.history.getGameTagValue("Black"));
			lblPlayerNameB.setText(gc.cl.history.getGameTagValue("White"));
			if (!ec.chessEngineMatch) {
				lblPlayerEloA.setText(gc.cl.history.getGameTagValue("BlackElo"));
				lblPlayerEloB.setText(gc.cl.history.getGameTagValue("WhiteElo"));
			}
		}
		else
		{
			lblPlayerNameA.setText(gc.cl.history.getGameTagValue("White"));
			lblPlayerNameB.setText(gc.cl.history.getGameTagValue("Black"));
			if (!ec.chessEngineMatch) {
				lblPlayerEloA.setText(gc.cl.history.getGameTagValue("WhiteElo"));
				lblPlayerEloB.setText(gc.cl.history.getGameTagValue("BlackElo"));
			}
		}
		if (ec.chessEnginePlayMod == 5 | ec.chessEnginePlayMod == 6)
		{
			lblPlayerTimeA.setText("");
			lblPlayerTimeB.setText("");
		}
	}

	public void updateTime(CharSequence color)
	{
		if (!color.equals(""))
		{

//			Log.i(TAG, "updateTime(), gc.startFen: " + gc.startFen);
//			Log.i(TAG, "updateTime(), ec.chessEnginePaused: " + ec.chessEnginePaused);

			if (color.equals("w"))
				tc.switchChessClock(true, System.currentTimeMillis(), ec.chessEnginePlayMod);
			else
				tc.switchChessClock(false, System.currentTimeMillis(), ec.chessEnginePlayMod);
			if (ec.chessEnginePlayMod == 4)
			{
				updateTimeBackground(lblPlayerTimeA, 2, tc.clockIsRunning, tc.whiteMoves);
				updateTimeBackground(lblPlayerTimeB, 2, tc.clockIsRunning, tc.whiteMoves);
				lblPlayerTimeA.setText("");
				lblPlayerTimeB.setText("");
				if (!gc.isBoardTurn)
				{
					if (color.equals("w"))
						lblPlayerTimeB.setText(tc.showWhiteTime);
					else
						lblPlayerTimeA.setText(tc.showWhiteTime);
				}
				else
				{
					if (color.equals("w"))
						lblPlayerTimeA.setText(tc.showWhiteTime);
					else
						lblPlayerTimeB.setText(tc.showWhiteTime);
				}
			}
			else
			{
				if (!gc.isBoardTurn)
				{
					updateTimeBackground(lblPlayerTimeA, tc.showModusBlack, tc.clockIsRunning, tc.whiteMoves);
					lblPlayerTimeA.setText(tc.showBlackTime);
					updateTimeBackground(lblPlayerTimeB, tc.showModusWhite, tc.clockIsRunning, tc.whiteMoves);
					lblPlayerTimeB.setText(tc.showWhiteTime);
				}
				else
				{
					updateTimeBackground(lblPlayerTimeA, tc.showModusWhite, tc.clockIsRunning, tc.whiteMoves);
					lblPlayerTimeA.setText(tc.showWhiteTime);
					updateTimeBackground(lblPlayerTimeB, tc.showModusBlack, tc.clockIsRunning, tc.whiteMoves);
					lblPlayerTimeB.setText(tc.showBlackTime);
				}
			}

			infoShowPv = true;

			oldTimeWhite = tc.showWhiteTime;
			oldTimeBlack = tc.showBlackTime;
			engineControlTime = System.currentTimeMillis();
			handlerChessClock.removeCallbacks(mUpdateChessClock);
			handlerChessClock.postDelayed(mUpdateChessClock, CLOCK_DELAY);
		}
	}

	public void updateTimeBackground(TextView tv, int colorShowId, boolean isClockRunning, boolean isWhiteMove)
	{

//		Log.i(TAG, "updateTimeBackground(), tv.getId(): " + tv.getId() + ", isClockRunning: " + isClockRunning + ", isWhiteMove: " + isWhiteMove);

		if (!isClockRunning)
        {
            u.setTextViewColors(lblPlayerTimeA, cv, cv.COLOR_TIME2_BACKGROUND_20, cv.COLOR_TIME2_TEXT_21);
            u.setTextViewColors(lblPlayerTimeB, cv, cv.COLOR_TIME2_BACKGROUND_20, cv.COLOR_TIME2_TEXT_21);
        }
		else
		{
			switch (colorShowId)
			{
				case 1:     // >= 10 min
				case 2:     // >= 10 sec
					if (!gc.isBoardTurn)
					{
						if (isWhiteMove & lblPlayerTimeB.getId() == tv.getId())
						{
							u.setTextViewColors(tv, cv, cv.COLOR_TIME_BACKGROUND_18, cv.COLOR_TIME_TEXT_19);
							u.setTextViewColors(lblPlayerTimeA, cv, cv.COLOR_TIME2_BACKGROUND_20, cv.COLOR_TIME2_TEXT_21);
						}
						if (!isWhiteMove & lblPlayerTimeA.getId() == tv.getId())
						{
							u.setTextViewColors(tv, cv, cv.COLOR_TIME_BACKGROUND_18, cv.COLOR_TIME_TEXT_19);
							u.setTextViewColors(lblPlayerTimeB, cv, cv.COLOR_TIME2_BACKGROUND_20, cv.COLOR_TIME2_TEXT_21);
						}
					}
					else
					{
						if (isWhiteMove & lblPlayerTimeA.getId() == tv.getId())
						{
							u.setTextViewColors(tv, cv, cv.COLOR_TIME_BACKGROUND_18, cv.COLOR_TIME_TEXT_19);
							u.setTextViewColors(lblPlayerTimeB, cv, cv.COLOR_TIME2_BACKGROUND_20, cv.COLOR_TIME2_TEXT_21);
						}
						if (!isWhiteMove & lblPlayerTimeB.getId() == tv.getId())
						{
							u.setTextViewColors(tv, cv, cv.COLOR_TIME_BACKGROUND_18, cv.COLOR_TIME_TEXT_19);
							u.setTextViewColors(lblPlayerTimeA, cv, cv.COLOR_TIME2_BACKGROUND_20, cv.COLOR_TIME2_TEXT_21);
						}
					}
					break;
				case 3:     // < 10 sec
					u.setTextViewColors(tv, cv, cv.COLOR_HIGHLIGHTING_22, cv.COLOR_TIME_TEXT_19);
					break;
			}
		}
	}

	public void setTimeWhiteBlack(int wb)
	{
		chessClockControl = wb;
		if (wb == 1)
		{
			chessClockMessage = getString(R.string.ccsMessageWhite);
			chessClockTimeGame = tc.timeWhite;
			chessClockTimeBonusSaveWhite = tc.bonusWhite;
		}
		else
		{
			chessClockMessage = getString(R.string.ccsMessageBlack);
			chessClockTimeGame = tc.timeBlack;
			chessClockTimeBonusSaveBlack = tc.bonusBlack;
		}
		chessClockTimeBonus = -1;
		c4aShowDialog(TIME_SETTINGS_DIALOG);
	}

	public void stopAllEnginesAndInit()
	{
		stopComputerThinking(false, false);
		ec.setPlaySettings(userPrefs, gc.cl.p_color);
		ec.chessEngineInit = true;
		ec.chessEnginePaused = true;
		initChessClock(tc, userPrefs.getInt("user_options_timeControl", 1), ec.chessEnginePlayMod);
		updateCurrentPosition("");
	}

	public void setPauseValues(boolean auto, CharSequence fen, int mode, CharSequence messageEngine)
	{

//		Log.i(TAG, "setPauseValues(), auto: " + auto + ", fenMes: " + fenMes + ", mode: " + mode + "\nmessageEngine:\n" + messageEngine);

		pause_auto = auto;
		pause_fen = fen;
		pause_mode = mode;
		pause_messageEngine = messageEngine;
	}

	public boolean getPauseValues(boolean autoPause, CharSequence fen, int mode)
	{
		if (autoPause == pause_auto & fen.equals(pause_fen) & mode == pause_mode)
			return true;
		else
			return false;
	}

	public boolean isPlayerMove(int playMode, CharSequence color)
	{

//		Log.i(TAG, "isPlayerMove(), playMode: " + playMode + ", color: " + color);

		if (playMode == 1 & color.equals("w"))
			return true;
		if (playMode == 2 & color.equals("b"))
			return true;
		return false;
	}

	public Boolean isStateGameOver()
	{

//		Log.i(TAG, "isStateGameOver() ");

		if (gc.cl.p_mate | gc.cl.p_stalemate | gc.cl.p_auto_draw) {
			ec.chessEngineSearching = false;
			stopChessClock();
			stopComputerThinking(false, true);
			ec.chessEnginePaused = true;
			if (gc.cl.p_mate)
				setInfoMessage(getString(R.string.cl_gameOver) + " (" + getString(R.string.cl_mate) + ")", null, null);
			if (gc.cl.p_stalemate)
				setInfoMessage(getString(R.string.cl_gameOver) + " (" + getString(R.string.cl_stealmate) + ")", null, null);
			if (gc.cl.p_auto_draw)
				setInfoMessage(getString(R.string.cl_gameOver) + " (" + getString(R.string.cl_draw) + ")", null, null);
			if (!ec.chessEngineMatch)
				playSound(3, 0);
			setEnginePausePlayBtn(false, null);
			return true;
		}
		else
			return false;
	}

	public void setPieceName(int pieceNameId)
	{
		switch (pieceNameId)
		{
			case 1:
				gc.cl.history.setAlgebraicNotation
						(
						getString(R.string.piece_K),
						getString(R.string.piece_Q),
						getString(R.string.piece_R),
						getString(R.string.piece_B),
						getString(R.string.piece_N)
						);
				break;
			case 2:
				// figurine notation
				break;
			default:
				gc.cl.history.setAlgebraicNotation("K", "Q", "R", "B", "N");
				break;
		}
	}

	public void pauseStopPlay(boolean isFromDialog)
	{

//		Log.i(TAG, "pauseStopPlay(), ec.chessEnginePaused: " + ec.chessEnginePaused + ", ec.getEngine().engineState: " + ec.getEngine().engineState);

		if (ec.getEngine() != null) {
			if (ec.getEngine().engineState == EngineState.PONDER) {
				setPauseEnginePlay(false);
				initPonder();
			}
		}

		gc.isGameLoaded = false;
		if (ec.chessEnginePlayMod <= 3 | (ec.chessEnginePlayMod == 5 & !twoPlayerPaused))
			setRunPrefsTime();
		if (!ec.chessEnginePaused)
		{
			if (pause_mode == 6)
			{
				stopSearchAndContinue(EngineState.STOP_IDLE, "", true);
				ec.chessEnginePlayMod = 6;
				setPlayModPrefs(ec.chessEnginePlayMod);
				ec.chessEngineSearching = false;
				setInfoMessage(getString(R.string.menu_modes_edit), null, null);
			}
			else
			{
				setPauseValues(false, gc.fen, ec.chessEnginePlayMod, messageEngine);
				stopSearchAndContinue(EngineState.STOP_IDLE, "", true);
				updateTime(gc.cl.p_color);

				setInfoMessage(getString(R.string.engine_paused), null, null);
				if (ec.chessEnginePlayMod <= 5)
					setEnginePausePlayBtn(false, null);
			}
		}

//		dContinueId = 3;

	}

	void handleEngineInitialized(int engineId) {
		if (gc.isChess960 && !ec.uciEngines[engineId].uciOptions.contains("UCI_Chess960")) {
			updateGui();
			setInfoMessage(getString(R.string.engine_paused), null, null);
			msgC4aDialog = getString(R.string.chess960NotSupprted) + "\n\nEngine: " + ec.uciEngines[engineId].uciEngineName;
			removeDialog(C4A_DIALOG);
			showDialog(C4A_DIALOG);
			return;
		}

		if (engineId < ec.engineCnt) {

//			Log.i(TAG, "1 handleEngineInitialized(), ec.engineCnt: " + ec.engineCnt + ", engineId: " + engineId + ", ec.currentEngineId: " + ec.currentEngineId);
//			Log.i(TAG, "2 handleEngineInitialized(), gc.fen: " + gc.fen);

			if (startNewGame(engineId, true)) {
				int newEngineId = engineId +1;

				if (engineId == ec.currentEngineId && ec.engineCnt == 1)
					ec.setStartPlay(engineId, gc.getValueFromFen(gc.fen, 2));

				if (ec.chessEnginePlayMod == 4) {
					ec.setPlayData(userPrefs, gc.cl.history.getGameTagValue("White"), gc.cl.history.getGameTagValue("Black"));
					updateGui();
					ec.chessEnginePaused = false;
					ec.chessEngineInit = false;
					displayMoves = null;
					ec.chessEngineSearching = true;
					chessEngineSearch(engineId, gc.fen, "", "");
				}

				if (newEngineId < ec.engineCnt)
					ec.uciEngines[newEngineId] = new UciEngine(MainActivity.this, newEngineId, engineNames.get(newEngineId), listener);
				else {
					if (ec.chessEnginePlayMod != 4) {
						ec.setPlayData(userPrefs, gc.cl.history.getGameTagValue("White"), gc.cl.history.getGameTagValue("Black"));
						gc.cl.history.setGameTag("White", ec.chessEnginePlayerWhite.toString());
						gc.cl.history.setGameTag("Black", ec.chessEnginePlayerBlack.toString());
						ec.setStartPlay(ec.currentEngineId, gc.getValueFromFen(gc.fen, 2));

//						Log.i(TAG, "3 handleEngineInitialized(), startEnginePlayIsReady(), ec.currentEngineId: " + ec.currentEngineId);

						updateGui();
						startEnginePlayIsReady(true);
					}
				}

			}
			else
			{

//				Log.i(TAG, "9 handleEngineInitialized(), startNewGame() ERROR, engineId: " + engineId + ", engineState: " + ec.uciEngines[engineId].engineState);

				stopChessClock();
				ec.chessEngineSearching = false;
				ec.chessEnginePaused = true;
				ec.chessEngineInit = true;
				setEnginePausePlayBtn(false, null);
				setInfoMessage(getString(R.string.engine_noRespond) + " (3)", null, null);

			}
		}
	}

//	@RequiresApi(api = Build.VERSION_CODES.Q)
void handleSearchResult(int engineId, String fen, String bestmove, String ponderMove) {

		if (ec.uciEngines[engineId] == null) {

//			Log.i(TAG, "handleSearchResult(), engineId: " + engineId + ", NULL");

			return;
		}

		continueFen = "";
		restartFen = "";

		EngineState searchEngineState = ec.uciEngines[engineId].engineState;

		ec.uciEngines[engineId].engineState = EngineState.IDLE;

//		Log.i(TAG, "handleSearchResult(), engineId: " + engineId + ", engineState: " + ec.uciEngines[engineId].engineState + ", fen: " + fen + ", bestmove: " + bestmove + ". ponderMove: " + ponderMove);

		setEnginePausePlayBtn(true, null);

		CharSequence newFen = chessEngineGui(fen, bestmove, true);

//		Log.i(TAG, "2 handleSearchResult(), engineId: " + engineId + ", newFen: " + newFen);

		if (ec.chessEngineMatchError) {
			newFen = "";
			gc.isGameOver = true;
			gc.cl.history.setGameTag("Result", "1/2-1/2");
		}

		if (!newFen.equals(""))
		{

			handleEngineMessages(engineId, ec.uciEnginesMessage, ec.uciEnginesScore, "");

			if (ec.chessEnginePlayMod == 3)
			{	// engine vs engine

				if 	(	!gc.isGameOver & !gc.cl.p_variationEnd
						& !gc.cl.p_mate & !gc.cl.p_stalemate & !gc.cl.p_auto_draw
					)
				{
					ec.chessEngineSearching = true;

//					Log.i(TAG, "handleSearchResult(), bestmove: " + bestmove + ", newFen: " + newFen);
//					Log.i(TAG, "handleSearchResult(), engineId: " + engineId + ", bestmove: " + bestmove + ", newFen: " + newFen);

					int handleEngineId = ec.currentEngineId;
					if (ec.currentEngineId == 0)
						ec.setCurrentEngineId(1);
					else
						ec.setCurrentEngineId(0);
					if (ec.engineCnt == 1)
						ec.setCurrentEngineId(0);

//					Log.i(TAG, "1 handleSearchResult(), ec.currentEngineId: " + ec.currentEngineId + ", newFen: " + newFen);

					//PONDER
					if 	(		userPrefs.getBoolean("user_options_enginePlay_Ponder", false)
							&&  withMatchPonder
							&& 	ec.engineCnt == 2
						)
					{

						if (ec.uciEngines[ec.currentEngineId] != null)
						{

							ec.uciEnginesMessage[handleEngineId] = "";

							ec.setCurrentEngineId(ec.currentEngineId);
							ec.uciEnginesMessage[ec.currentEngineId] = "";

	//						Log.i(TAG, "2 handleSearchResult(), --> applyPonderhit() ??? ec.currentEngineId: " + ec.currentEngineId + ", engineState: " + ec.uciEngines[ec.currentEngineId].engineState);

							if (ec.uciEngines[ec.currentEngineId] == null)
								return;

							if (ec.uciEngines[ec.currentEngineId].engineState == EngineState.PONDER) {
								if (ec.uciEngines[ec.currentEngineId].applyPonderhit(bestmove, newFen.toString())) {     // !"ponderhit"

//								Log.i(TAG, "3 handleSearchResult(), !applyPonderhit() --> stopSearchAndContinue(), ec.currentEngineId: " + ec.currentEngineId + ", engineState: " + ec.uciEngines[ec.currentEngineId].engineState);

									if (ec.uciEngines[ec.currentEngineId].engineSearching()) {
										continueFen = newFen;
										ec.uciEngines[ec.currentEngineId].stopSearch(EngineState.STOP_CONTINUE);
									} else {
										ec.uciEngines[ec.currentEngineId].engineState = EngineState.IDLE;
										chessEngineSearch(ec.currentEngineId, newFen, "", "");
										ec.chessEngineSearching = true;
									}

								}
							}
							else {
								if (ec.uciEngines[ec.currentEngineId].engineState == EngineState.IDLE) {

//								Log.i(TAG, "4 handleSearchResult(), !PONDER, IDLE, ec.currentEngineId: " + ec.currentEngineId + ", engineState: " + ec.uciEngines[ec.currentEngineId].engineState);

									ec.chessEngineSearching = true;
									chessEngineSearch(ec.currentEngineId, newFen, "", "");
								} else {

//								Log.i(TAG, "5 handleSearchResult(), !PONDER, !IDLE, ec.currentEngineId: " + ec.currentEngineId + ", engineState: " + ec.uciEngines[ec.currentEngineId].engineState);
//								Log.i(TAG, "5 handleSearchResult(), !PONDER, !IDLE, searchRequest.fen: " + ec.uciEngines[ec.currentEngineId].searchRequest.fen + ", gc.cl.p_fen: " + gc.cl.p_fen);

									if (!(ec.uciEngines[ec.currentEngineId].engineState == EngineState.SEARCH && ec.uciEngines[ec.currentEngineId].searchRequest.fen.equals(gc.cl.p_fen))) {

//									Log.i(TAG, "6 handleSearchResult(), fen?, STOP_QUIT: " + ec.currentEngineId + ", engineState: " + ec.uciEngines[ec.currentEngineId].engineState);

										stopSearchAndContinue(EngineState.STOP_QUIT, "", true);
										setEnginePausePlayBtn(false, null);

									}

								}

							}

							if (!ponderMove.equals("")) {
								isGoPonder = true;
								chessEngineSearch(handleEngineId, newFen, "", ponderMove);                // "go ponder"
							}

							engineControlTime = System.currentTimeMillis();
							handlerChessClock.removeCallbacks(mUpdateChessClock);
							handlerChessClock.postDelayed(mUpdateChessClock, CLOCK_START);

							updateGui();

						}

						return;

					}
					else {
						isGoPonder = false;
						if (ec.engineCnt == 2) {
							ec.setCurrentEngineId(ec.currentEngineId);
							ec.uciEnginesMessage[handleEngineId] = "";
							ec.uciEnginesMessage[ec.currentEngineId] = "";
							ec.uciEngines[ec.currentEngineId].ponderMove = "";
						}
						else
							ec.setCurrentEngineId(0);

//						Log.i(TAG, "3 handleSearchResult(), --> stopSearchAndContinue()");

						playSound(1, 0);
						stopSearchAndContinue(EngineState.STOP_CONTINUE, newFen, false);			// "stop" & "go" (search)
					}
				}
				else
				{

//					Log.i(TAG, "9 handleSearchResult(), GameOver !!!");

					stopSearchAndContinue(EngineState.STOP_IDLE, "", true);
					ec.chessEnginePaused = true;
					updateCurrentPosition("");
					setInfoMessage(getEnginePausedMessage(), null, null);
				}

//				Log.i(TAG, "1 handleSearchResult(), --> return");

				return;

			}
			else
			{
				ec.chessEngineSearching = false;
				if (userPrefs.getInt("user_options_timeControl", 1) == 2)
					setMoveTime();
			}

//			Log.i(TAG, "3 handleSearchResult(), newFen: " + newFen + ", bestmove: " + bestmove + ", engineState: " + ec.uciEngines[engineId].engineState);

			if 	(		userPrefs.getBoolean("user_options_enginePlay_Ponder", false)
					& 	isPlayerMove(ec.chessEnginePlayMod, gc.getValueFromFen(newFen, 2))
					& 	ec.uciEngines[engineId].engineState != EngineState.BOOK
					&	ec.uciEngines[engineId].isUciPonder
					& 	(ec.chessEnginePlayMod == 1 | ec.chessEnginePlayMod == 2)
			)
			{
//				if (!ec.chessEngineMatch)
//					playSound(1, 0);
				playSound(1, 0);
				enginePlayPonder(newFen);

//				Log.i(TAG, "4 handleSearchResult(), engineId: " + engineId + ", newFen: " + newFen);

				return;

			}
			else
				initPonder();

//			Log.i(TAG, "5 handleSearchResult(), engineId: " + engineId + ", newFen: " + newFen);

			if ((ec.chessEnginePlayMod == 1 | ec.chessEnginePlayMod == 2 | ec.chessEnginePlayMod == 3) && ec.uciEngines[engineId].engineState != EngineState.BOOK)
				playSound(1, 0);
			engineControlTime = System.currentTimeMillis();
			handlerChessClock.removeCallbacks(mUpdateChessClock);
			handlerChessClock.postDelayed(mUpdateChessClock, CLOCK_START);

//			Log.i(TAG, "4 IDLE, handleSearchResult(), engineName: " + ec.uciEngines[ec.currentEngineId].engineName + "(" + ec.currentEngineId + ") ");

			ec.uciEngines[engineId].engineState = EngineState.IDLE;

			updateGui();

		}
		else
		{
			if (ec.chessEnginePlayMod == 3 && ec.chessEngineMatch) {

//				Log.i(TAG, "handleSearchResult(), ec.chessEngineMatch, gameOver: " + gc.isGameOver);

				if (gc.isGameOver | gc.cl.p_variationEnd | gc.cl.p_mate | gc.cl.p_stalemate | gc.cl.p_auto_draw)
				{

//					Log.i(TAG, "handleSearchResult(), gameOver: " + gc.isGameOver + ", mate: " + gc.cl.p_mate
//							+ ", stalemate: " + gc.cl.p_stalemate + ", auto_draw: " + gc.cl.p_auto_draw + ", ec.chessEngineMatchError: " + ec.chessEngineMatchError);

					updateGui();
					if (!ec.chessEngineMatchError)
						setMatchValues();
					if (userPrefs.getBoolean("user_play_eve_autoSave", true) && !ec.chessEngineMatchError) {
						startFileManager(SAVE_GAME_REQUEST_CODE, 0, 0);
					}
					else {
						if (ec.chessEngineMatchError)
							nextGameEngineAutoPlay();
						else
							startEngineMatch();
					}
					ec.chessEngineMatchError = false;
				}
				else
				{

//					Log.i(TAG, "handleSearchResult(), newFen.equals SPACE, engineId: " + engineId + ", fen:    " + fen + ", bestmove: " + bestmove);
//					Log.i(TAG, "handleSearchResult(), newFen.equals SPACE, engineId: " + engineId + ", hisFen: " + gc.cl.history.getMoveFen(gc.cl.history.getMoveIdx()) + ", searchEngineState: " + searchEngineState);

					boolean enginesError = false;
					if (ec.uciEngines[0] != null && ec.uciEngines[1] != null) {
						if (!ec.uciEngines[0].engineSearching() && !ec.uciEngines[1].engineSearching())
							enginesError = true;
						if (ec.uciEngines[0].engineState == EngineState.READ_OPTIONS || ec.uciEngines[1].engineState == EngineState.READ_OPTIONS)
							enginesError = false;
					}
					else {
						enginesError = true;
					}
					if (gc.cl.history.getMoveFen(gc.cl.history.getMoveIdx()).toString().contains("/8/8/8/8/"))
						enginesError = false;

					if (!fen.equals(gc.cl.history.getMoveFen(gc.cl.history.getMoveIdx())) || enginesError)
					{

//						Log.i(TAG, "handleSearchResult(), fen:     " + fen + ", bestmove: " + bestmove+ ", searchEngineState: " + searchEngineState + ", enginesError: " + enginesError);
//						Log.i(TAG, "handleSearchResult(), histFen: " + gc.cl.history.getMoveFen(gc.cl.history.getMoveIdx()) + ", #" + userPrefs.getInt("user_play_eve_round", 0));

						restartSearch();

					}
					else
					{

//						Log.i(TAG, "handleSearchResult(), engineId: " + engineId + ", fen:    " + fen + ", bestmove: " + bestmove + ", enginesError: " + enginesError);
//						Log.i(TAG, "handleSearchResult(), engineId: " + engineId + ", hisFen: " + gc.cl.history.getMoveFen(gc.cl.history.getMoveIdx()) + ", " + searchEngineState);

					}

				}
			}
			else {

//				Log.i(TAG, "6 handleSearchResult(), --> stopSearchAndContinue()");

				stopSearchAndContinue(EngineState.STOP_IDLE, "", true);
				ec.chessEnginePaused = true;
				updateCurrentPosition("");
				setInfoMessage(getEnginePausedMessage(), null, null);
			}
		}

	}

	void handleEngineMessages(int engineId, String[] uciEnginesMessage1, int[] score1, String searchDisplayMoves) {

//		Log.i(TAG, "1 handleEngineMessages(), engineId: " + engineId + ", uciEnginesMessage.length: " + uciEnginesMessage.length);
//		Log.i(TAG, "1 handleEngineMessages(), engineId: " + engineId + ", ec.uciEngines.length: " + ec.uciEngines.length + ", gc.isGameOver: " + gc.isGameOver);

		if (ec.uciEngines == null || uciEnginesMessage1 == null)
			return;
		if (engineId >= ec.uciEngines.length)
			return;
		if (ec.uciEngines[engineId] == null)
			return;
		if (gc.isGameOver)
			return;

		String[] uciEnginesMessage = uciEnginesMessage1.clone();
		int[] score = score1.clone();

//		Log.i(TAG, "2 handleEngineMessages(), engineId: " + engineId + ", uciEnginesMessage.length: " + uciEnginesMessage.length);

		restartFen = "";
		if (ec.chessEnginePlayMod == 3 && ec.chessEngineMatch)
		{
			if (tc.timeWhite > 0 && tc.timeBlack > 0)
				engineMatchControlTime = System.currentTimeMillis();
			else
			{
				if (System.currentTimeMillis() - engineMatchControlTime >= 5000 && engineMatchControlTime != 0)
				{

//					Log.i(TAG, "handleEngineMessages(), restartSearch()" );

					restartSearch();

					return;

				}
			}
		}

		if (uciEnginesMessage[engineId].equals("INFO SPACE")) {

//			Log.i(TAG, "handleEngineMessages(), INFO SPACE, engineId: " + engineId + ", engineState: " + ec.uciEngines[engineId].engineState);

			return;
		}

		String engineUpdate = "ENGINE_UPDATE";

		boolean testSsb = false;

		SpannableStringBuilder engineMes = new SpannableStringBuilder();

		SpannableString sText;

		// IndexOutOfBoundsException !!!
		try {
			if (ec.chessEnginePlayMod == 3 && uciEnginesMessage.length == 2) {

//			Log.i(TAG, "2 handleEngineMessages(), engineId: " + engineId + ", uciEnginesMessage.length: " + uciEnginesMessage.length + ", ec.engineCnt: " + ec.engineCnt);
//			Log.i(TAG, "2b handleEngineMessages(), engineId: " + engineId + ", uciEnginesMessage[1]: " + uciEnginesMessage[1]);

				if (gc.isBoardTurn) {
					sText = new SpannableString(uciEnginesMessage[0]);
					sText.setSpan(new ForegroundColorSpan(cv.getTransparentColorInt(ColorValues.COLOR_ARROWS5_27, "ee")), 0, uciEnginesMessage[0].length(), 0);
					engineMes.append(sText);
					sText = new SpannableString(uciEnginesMessage[1]);
					sText.setSpan(new ForegroundColorSpan(cv.getTransparentColorInt(ColorValues.COLOR_ARROWS6_28, "ee")), 0, uciEnginesMessage[1].length(), 0);
					engineMes.append(sText);
				} else {
					sText = new SpannableString(uciEnginesMessage[1]);
					sText.setSpan(new ForegroundColorSpan(cv.getTransparentColorInt(ColorValues.COLOR_ARROWS6_28, "ee")), 0, uciEnginesMessage[1].length(), 0);
					engineMes.append(sText);
					sText = new SpannableString(uciEnginesMessage[0]);
					sText.setSpan(new ForegroundColorSpan(cv.getTransparentColorInt(ColorValues.COLOR_ARROWS5_27, "ee")), 0, uciEnginesMessage[0].length(), 0);
					engineMes.append(sText);
				}
			} else {
				boolean isAnalysisMessage = false;

//				if (ec.chessEnginePlayMod == 4 && userPrefs.getBoolean("user_play_multipleEngines", true) && score.length == ec.engineCnt) {
//				if (ec.chessEnginePlayMod == 4 && withSortAnalyse && userPrefs.getBoolean("user_play_multipleEngines", true) && score.length == ec.engineCnt) {
				if (ec.chessEnginePlayMod == 4 && userPrefs.getBoolean("user_play_multipleEngines", true) && score.length == ec.engineCnt) {
//					uciEnginesMessage = sortEngineAnalysisMessage(uciEnginesMessage, score);
//					isAnalysisMessage = true;
					sortEngineAnalysisMessage(uciEnginesMessage, score);
				}

				if (ec.chessEnginePlayMod == 4 && ec.engineCnt == 1 && !userPrefs.getBoolean("user_play_multipleEngines", true)) {
					ec.analysisBestScoreEngineId = 0;
					ec.analysisBestScore = ec.uciEngines[0].bestScore;

//					Log.i(TAG, "handleEngineMessages(), bestScore: " + ec.uciEngines[0].bestScore);

				}

//				Log.i(TAG, "3 handleEngineMessages(), uciEnginesMessage1.length: " + uciEnginesMessage1.length + ", uciEnginesMessage.length: " + uciEnginesMessage.length);

				for (int i = 0; i < uciEnginesMessage.length; i++) {

//					Log.i(TAG, "handleEngineMessages(), i: " + i + ", uciEnginesMessage[i]: " + uciEnginesMessage[i]);

					if (uciEnginesMessage[i] != null) {
						sText = new SpannableString(uciEnginesMessage[i]);
						int colorVal;
						int colorId = i;
						if (sortedIdx != null && isAnalysisMessage) {
							colorId = sortedIdx[i];

//						Log.i(TAG, "handleEngineMessages(), i: " + i + ", colorId: " + colorId);

						}
						switch (colorId) {
							default:
								colorVal = ColorValues.COLOR_ARROWS5_27;
								break;    // case 0
							case 1:
								colorVal = ColorValues.COLOR_ARROWS6_28;
								break;
							case 2:
								colorVal = ColorValues.COLOR_ARROWS7_29;
								break;
							case 3:
								colorVal = ColorValues.COLOR_ARROWS8_30;
								break;
						}
						if (ec.chessEnginePlayMod == 3 && ec.engineCnt == 2) {
							colorVal = ColorValues.COLOR_ARROWS5_27;
							if (engineId == 1)
								colorVal = ColorValues.COLOR_ARROWS6_28;

//						Log.i(TAG, "4 handleEngineMessages(), engineId: " + engineId + ", colorVal: " + colorVal);

						}
						sText.setSpan(new ForegroundColorSpan(cv.getTransparentColorInt(colorVal, "ee")), 0, uciEnginesMessage[i].length(), Spannable.SPAN_INTERMEDIATE);
						engineMes.append(sText);
					}
				}
			}
		}
		catch (IndexOutOfBoundsException e) {return;}

//		Log.i(TAG, "handleEngineMessages(), setInfoMessage(), engineId: " + engineId + ", engineMes: " + engineMes);

		if (tc.clockIsRunning)
		{

			if (testSsb)
				return;

			if (gc.isGameOver | gc.cl.p_variationEnd)
			{
				stopChessClock();
				if (!ec.chessEngineMatch)
				{
					ec.chessEnginePaused = true;
					ec.chessEngineSearching = false;
				}
				updateCurrentPosition("");

//				Log.i(TAG, "7 getGameOverMessage()");

				if (!ec.chessEngineMatch)
					playSound(3, 0);
				setInfoMessage("   " + getGameOverMessage(), engineMes, engineUpdate);

			}
			else
			{
				updateTime(gc.cl.p_color);
			}
		}
		else
			updateTime(gc.cl.p_color);
		if (!ec.chessEnginePaused)
		{
			if (ec.chessEnginesOpeningBook)
			{

//				Log.i(TAG, "handleEngineMessages(), ec.chessEnginesOpeningBook: " + ec.chessEnginesOpeningBook);

				if (!ec.chessEngineMatch) {

//					Log.i(TAG, "handleEngineMessages(), ec.chessEnginesOpeningBook: " + ec.chessEnginesOpeningBook + ", ec.chessEngineMatch: " + ec.chessEngineMatch);

					setInfoMessage(getString(R.string.engine_openingBook), null, "");
				}
			}
			else
			{

//				Log.i(TAG, "handleEngineMessages(), setInfoMessage(), engineId: " + engineId);

				if (!ec.chessEngineMatch)
				{
					if (getEngineThinkingMessage().equals(getString(R.string.engineAnalysisStopWait)))
						engineStat = "";
					setInfoMessage(getEngineThinkingMessage() + " " + engineStat, engineMes, engineUpdate);
				}
				else
					setInfoMessage(null, engineMes, engineUpdate);
			}
		}
		else
			setInfoMessage(getEngineThinkingMessage() + " " + engineStat, engineMes, engineUpdate);

		if (searchDisplayMoves != null) {

//			Log.i(TAG, "handleEngineMessages(), searchDisplayMoves: " + searchDisplayMoves);
//			Log.i(TAG, "1 handleEngineMessages(), arrowsId: " + arrowsId + ", searchDisplayMoves: " + searchDisplayMoves);

//			Log.i(TAG, "handleEngineMessages(), engineId: " + engineId + ", engineState: " + ec.uciEngines[engineId].engineState);

			if (ec.chessEnginePlayMod <= 4 && !ec.chessEnginePaused && !searchDisplayMoves.equals("")) {
				boolean isPlayer = isPlayerMove(ec.chessEnginePlayMod, gc.getValueFromFen(gc.cl.p_fen, 2));
				if (isPlayer && !userPrefs.getBoolean("user_options_enginePlay_Ponder", false)) {
					displayMoves = null;
					ec.uciEngines[engineId].searchDisplayMoves = "";
					ec.initEngineMessages();
					arrowsId = BoardView.ARROWS_NONE;
				}
				else
				{
					if (arrowsId != BoardView.ARROWS_BOOK) {
						if (ec.chessEnginePlayMod == 4 && userPrefs.getBoolean("user_play_multipleEngines", true) && withMultiEngineAnalyse)
							arrowsId = BoardView.ARROWS_BEST_MOVES;
						else
							arrowsId = BoardView.ARROWS_BEST_VARIANT;
//						if (!ec.uciEngines[engineId].engineSearching())
//							arrowsId = BoardView.ARROWS_NONE;
					}
					displayMoves = searchDisplayMoves;
				}

				boolean isUpdate = true;
				if (ec.chessEnginePlayMod == 3 && ec.engineCnt == 2 && ec.uciEngines[engineId].engineState == EngineState.PONDER)
					isUpdate = false;

//				Log.i(TAG, "2 handleEngineMessages(), arrowsId: " + arrowsId + ", isPlayer: " + isPlayer
//						+ ", engineId: " + engineId + ", engineState: " + ec.uciEngines[engineId].engineState + ", isUpdate: " + isUpdate);

				if (isUpdate)
					updateGui();

			}
			else {
				displayMoves = null;
				ec.uciEngines[engineId].searchDisplayMoves = "";
				arrowsId = BoardView.ARROWS_NONE;
//				if (ec.chessEnginePlayMod == 4)
//					updateGui();
			}
		}
		else {
			displayMoves = null;
			ec.uciEngines[engineId].searchDisplayMoves = "";
			arrowsId = BoardView.ARROWS_NONE;
		}

	}

	String[] sortEngineAnalysisMessage(String[] uciEnginesMessage, int[] score) {

//		Log.i(TAG, "sortEngineAnalysisMessage(), SDK_INT: " + android.os.Build.VERSION.SDK_INT);

		if(android.os.Build.VERSION.SDK_INT < 24)
			return uciEnginesMessage;

		if (uciEnginesMessage != null) {
			if (uciEnginesMessage.length > 0 && uciEnginesMessage.length == score.length) {
				String[] newEnginesMessage = new String[uciEnginesMessage.length];
				boolean desc = true;
				boolean isMate = false;
				for (int i = 0; i < uciEnginesMessage.length; i++) {
					if (uciEnginesMessage[i].contains("1(M"))
						isMate = true;
				}

//				Log.i(TAG, "sortEngineAnalysisMessage(), score: " + score[0] + " " + score[1] + " " + score[2] + " " + score[3]);

				if (uciEnginesMessage[0].contains("... ")) {
					desc = false;
					for (int i = 0; i < score.length; i++) {
						if (!isMate)
							score[i] = score[i] * -1;
						else {
							if (score[i] < 0)
								score[i] = score[i] * -1;
						}
					}
				}
				else {
					if (!isMate)
						desc = true;
					else {
						desc = false;
						for (int i = 0; i < score.length; i++) {
							if (score[i] < 0)
								score[i] = score[i] * -1;
						}
					}
				}

				Map<Integer, Integer> map = new HashMap<>();
				for (int i = 0; i < score.length; i++) {
					map.put(i, score[i]);
				}

//				Log.i(TAG, "sortEngineAnalysisMessage(), map.size(): " + map.size() + ", score.length: " + score.length);

				sortedScore = new LinkedHashMap<>();
				sortedIdx = new int[uciEnginesMessage.length];

				if (desc) {
					map.entrySet()
							.stream()
							.sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
							.forEachOrdered(x -> sortedScore.put(x.getKey(), x.getValue()));
				}
				else {
					map.entrySet()
							.stream()
							.sorted(Map.Entry.comparingByValue(Comparator.naturalOrder()))
							.forEachOrdered(x -> sortedScore.put(x.getKey(), x.getValue()));
				}

//				Log.i(TAG, "sortEngineAnalysisMessage(), desc: " + desc + ", score: " + map + ", sortedScore: " + sortedScore);

				int k = 0;
				ec.analysisBestScoreEngineId = 0;
				for (Map.Entry<Integer, Integer> entry : sortedScore.entrySet()) {
					Integer idx = entry.getKey();
					if (k == 0) {
						ec.analysisBestScoreEngineId = idx;
						ec.analysisBestScore = entry.getValue();

//						Log.i(TAG, "sortEngineAnalysisMessage(), ec.analysisBestScoreEngineId: " + ec.analysisBestScoreEngineId + ", ec.analysisBestScore: " + ec.analysisBestScore);

					}
					sortedIdx[k] = idx;

//					Log.i(TAG, "sortEngineAnalysisMessage(), k: " + k + ", idx: " + idx);

					newEnginesMessage[k] = uciEnginesMessage[idx];
					k++;
				}

				return newEnginesMessage;

			}
		}

		return uciEnginesMessage;

	}

	void restartSearch() {

		restartSearchCnt++;

		if (!gc.cl.history.getMoveFen(gc.cl.history.getMoveIdx()).equals("") && ec.engineCnt == 2)
		{
			int continueEngine = 0;
			int idleEngine = 1;
			ec.currentEngineId = 0;
			if (gc.getValueFromFen(gc.cl.history.getMoveFen(gc.cl.history.getMoveIdx()), 2).equals("b")) {
				continueEngine = 1;
				idleEngine = 0;
				ec.currentEngineId = 1;
			}

			if (ec.uciEngines[continueEngine] == null || ec.uciEngines[idleEngine] == null) {
				continueFen = gc.cl.history.getMoveFen(gc.cl.history.getMoveIdx());
				stopSearchAndContinue(EngineState.STOP_QUIT_RESTART, "", true);
				return;
			}

//			Log.i(TAG, "restartSearch(), STOP_CONTINUE, continueEngine: " + continueEngine + ", continueFen: " + gc.cl.history.getMoveFen(gc.cl.history.getMoveIdx()));
//			Log.i(TAG, "restartSearch(), STOP_CONTINUE, continueEngine: " + continueEngine + ", gc.fen:      " + gc.fen);

			ec.chessEngineSearching = true;
			if (ec.uciEngines[continueEngine].engineSearching()) {

//				Log.i(TAG, "1 restartSearch(), STOP_CONTINUE, continueEngine");

				continueFen = gc.cl.history.getMoveFen(gc.cl.history.getMoveIdx());
				ec.uciEngines[continueEngine].stopSearch(EngineState.STOP_CONTINUE);
			}
			else {

//				Log.i(TAG, "2 restartSearch(), chessEngineSearch(), continueEngine");

				chessEngineSearch(continueEngine, gc.cl.history.getMoveFen(gc.cl.history.getMoveIdx()), "", "");

			}
			if (ec.uciEngines[idleEngine].engineSearching()) {

//				Log.i(TAG, "3 restartSearch(), STOP_IDLE_NONE, idleEngine");

				ec.uciEngines[idleEngine].stopSearch(EngineState.STOP_IDLE_NONE);
			}
			else {

//				Log.i(TAG, "4 restartSearch(), IDLE, idleEngine");

				ec.uciEngines[idleEngine].engineState = EngineState.IDLE;
			}
		}

	}

	void handleStop(int engineId, UciEngine.EngineState engineState, String fen, String bestmove) {

//		Log.i(TAG, "1 handleStop(), engineId: " + engineId + ", engineState: " + engineState + ", fen: " + fen + ", bestmove: " + bestmove);
//		Log.i(TAG, "handleStop(), engineName: " + ec.uciEngines[ec.currentEngineId].engineName + "(" + engineId + "), engineState: " + engineState + ", fen: " + fen + ", bestmove: " + bestmove);

		if (ec.uciEngines == null)
			return;

		switch (engineState) {
			case STOP_MOVE: {
                setEnginePausePlayBtn(false, null);
                chessEngineGui(fen, bestmove, true);
                stopChessClock();
                updateCurrentPosition("");
			}
			break;
			case STOP_MOVE_CONTINUE: {
				CharSequence newFen = "";
				if (ec.chessEnginePlayMod == 4) {
					ec.analysisEngineCnt++;
					if (engineId == ec.analysisEngineId)
						ec.analysisEngineBestMove = bestmove;
					if (ec.analysisEngineCnt >= ec.engineCnt) {
						playSound(1, 0);
						newFen = chessEngineGui(fen, ec.analysisEngineBestMove, true);
						engineSearchAnalysis(newFen);
					}
				}
				else
				{
					newFen = chessEngineGui(fen, bestmove, true);

					if (newFen.equals("")) {

//						Log.i(TAG, "2 handleStop(), newFen '', engineId: " + engineId + ", engineState: " + engineState + ", ec.chessEnginePlayMod: " + ec.chessEnginePlayMod + ", fen: " + fen + ", bestmove: " + bestmove);

						stopSearchAndContinue(EngineState.STOP_IDLE, "", true);
						return;

					}

					updateCurrentPosition("");
					if ((ec.chessEnginePlayMod == 1 || ec.chessEnginePlayMod == 2) && isPlayerMove(ec.chessEnginePlayMod, gc.getValueFromFen(newFen, 2))) {

						ec.chessEngineSearching = false;
						if (userPrefs.getInt("user_options_timeControl", 1) == 2)
							setMoveTime();
						playSound(1, 0);
						engineControlTime = System.currentTimeMillis();
						handlerChessClock.removeCallbacks(mUpdateChessClock);
						handlerChessClock.postDelayed(mUpdateChessClock, CLOCK_START);
						if (userPrefs.getBoolean("user_options_enginePlay_Ponder", false)
							& ec.uciEngines[engineId].isUciPonder
						)
							enginePlayPonder(newFen);
					}
					else {

//						Log.i(TAG, "3 handleStop(), engineId: " + engineId + ", engineState: " + engineState + ", ec.chessEnginePlayMod: " + ec.chessEnginePlayMod + ", newFen: " + newFen);

						stopChessClock();

						startChessClock();
						isGoPonder = false;
//						if (!ec.chessEngineMatch)
//							playSound(1, 0);
						playSound(1, 0);
						ec.chessEngineSearching = true;

//						Log.i(TAG, "5 IDLE, handleStop(), engineName: " + ec.uciEngines[ec.currentEngineId].engineName + "(" + ec.currentEngineId + ") ");

						ec.uciEngines[engineId].engineState = EngineState.IDLE;

						chessEngineSearch(engineId, newFen, "", "");

					}
				}
			}
			break;
			case STOP_CONTINUE: {
                ec.setPlaySettings(userPrefs, gc.cl.p_color);
                setTurnBoard();

//				Log.i(TAG, "handleStop(), engineId: " + engineId + ", engineState: " + engineState + ", fen: " + fen + ", ec.chessEnginePaused: " + ec.chessEnginePaused);

                if (dSetClock)
                    initChessClock(tc, userPrefs.getInt("user_options_timeControl", 1), ec.chessEnginePlayMod);
                startChessClock();
                updateGui();
                isGoPonder = false;

//				Log.i(TAG, "6 IDLE, handleStop(), engineName: " + ec.uciEngines[ec.currentEngineId].engineName + "(" + ec.currentEngineId + ") ");

				if (ec.uciEngines[engineId] != null) {
					ec.uciEngines[engineId].engineState = EngineState.IDLE;
					if ((ec.chessEnginePlayMod == 1 || ec.chessEnginePlayMod == 2) && isPlayerMove(ec.chessEnginePlayMod, gc.getValueFromFen(continueFen, 2)))
						enginePlayPonder(continueFen);
					else {

//						Log.i(TAG, "handleStop(), STOP_CONTINUE, engineName: " + ec.uciEngines[engineId].engineName + "(" + engineId + "), continueFen:  " + continueFen);

						chessEngineSearch(engineId, continueFen, "", "");
						ec.chessEngineSearching = true;
					}
				}
			}
			break;
			case STOP_IDLE: {
                setEnginePausePlayBtn(false, null);
                ec.chessEnginePaused = true;
				stopTimeHandler(false);
                displayMoves = null;
                if (ec.chessEnginePlayMod == 5) {
                    setEnginePausePlayBtn(null, null);
                    stopChessClock();
                    ec.setPlaySettings(userPrefs, gc.cl.p_color);
                    startEdit(dNewGame, true);
                }
                if (ec.chessEnginePlayMod == 6) {
                    setEnginePausePlayBtn(null, null);
                    stopChessClock();
                    ec.setPlaySettings(userPrefs, gc.cl.p_color);
                    startEdit(dNewGame, false);
                }
			}
			break;
			case STOP_IDLE_NONE: {
				ec.uciEngines[engineId].engineState = EngineState.IDLE;
			}
			break;
			case STOP_QUIT: {
				shutDownEngines();
                setEnginePausePlayBtn(false, null);
				if (isStateGameOver()) {
					msgEngine.setVisibility(TextView.GONE);
					messageInfo 		= "";
					messageEngine = new SpannableStringBuilder("");
					messageEngineShort  = "";
				}
                ec.chessEnginePaused = true;
			}
			break;
			case STOP_NEW_GAME: {
                isGoPonder = false;
                startPlay(true, true);
			}
			break;
			case STOP_QUIT_RESTART: {

                stopChessClock();
                ec.setPlaySettings(userPrefs, gc.cl.p_color);
                setTurnBoard();
				shutDownEngines();
                initChessClock(tc, userPrefs.getInt("user_options_timeControl", 1), ec.chessEnginePlayMod);
                startChessClock();
                updateGui();

//				Log.i(TAG, "handleStop(), STOP_QUIT_RESTART, restartEngine()");

				restartEngine();

			}
			break;
		}
	}

	void handleEngineError(int engineId, String errMsg) {

		if (!errMsg.equals("")) {
			shutDownEngines();
			stopChessClock();
			setEnginePausePlayBtn(false, null);
			if (messageEngine != null) {
				if (!messageEngine.toString().equals(""))
					errMsg = messageEngine.toString() + "\n" + errMsg;
			}
			SpannableStringBuilder engineMes = new SpannableStringBuilder();
			engineMes.append(errMsg);
			setInfoMessage(null, engineMes, null);
		}

	}

	void engineSearchAnalysis(CharSequence fen) {
		for (int i = 0; i < ec.engineCnt; i++)
		{
			chessEngineSearch(i, fen, "", "");
		}
	}

	void setEnginePausePlayBtn(Boolean setPause, Boolean setPlay) {

//		Log.i(TAG, "setEnginePausePlayBtn(), setPause: " + setPause + ", setPlay: " + setPlay);

		if (setPause == null)
			btn_2.setImageResource(R.drawable.button);
		else {
			if (setPause)
				btn_2.setImageResource(R.drawable.btn_paus_analysis);
			else
				btn_2.setImageResource(R.drawable.btn_play);
		}
		if (setPlay == null)
			btn_3.setImageResource(R.drawable.button);
		else {
			if (setPlay)
				btn_3.setImageResource(R.drawable.btn_play_move_continue);
			else
				btn_3.setImageResource(R.drawable.button);
		}
	}

	public void updateGui()
	{

//		Log.i(TAG, "updateGui()");

//		Log.i(TAG, "1 updateGui(), gc.isBoardTurn: " + gc.isBoardTurn + ", gc.cl.p_color: " + gc.cl.p_color);
//		Log.i(TAG, "1 updateGui(), messageEngine: " + messageEngine);

		CharSequence messInfo = 	"";
		if (!gc.isGameOver & !gc.cl.p_variationEnd & gc.cl.p_message.equals(""))
		{
			showGameCount = "";
			if (ec.chessEnginePlayMod == 3 & ec.chessEngineMatch)
			{
				int cnt = userPrefs.getInt("user_play_eve_gameCounter", 1);
				showGameCount = " *" + cnt;
			}
			else
				showGameCount = "";
			if (ec.chessEngineSearching)
			{
				if (!ec.chessEnginePaused)
				{
					if (ec.chessEngineMatch)
						messInfo = getEngineMatchMessage();
					else
						messInfo = getEngineThinkingMessage();
				}
				else
				{
					if (!ec.chessEngineInit)
						messInfo = getEnginePausedMessage().toString() + showGameCount;
					else
						messInfo = getEnginePausedMessage().toString();

				}
			}
			else
			{
				if (ec.chessEnginePlayMod == 1 | ec.chessEnginePlayMod == 2)
				{
					messInfo = getString(R.string.player_move);
				}
				if (!gc.isGameOver & !gc.cl.p_variationEnd)
				{
					if (ec.chessEnginePaused | ec.chessEnginePlayMod == 4)
						messInfo = getEnginePausedMessage();
					else
						messInfo = getString(R.string.player_move);
				}
				if (gc.isAutoPlay)
					messInfo = getString(R.string.ccsMessageAutoPlay);
			}
			ec.chessEngineProblem = false;
		}

		setPlayerData();

//		Log.i(TAG, "2 updateGui(), gc.cl.p_fen: " + gc.cl.p_fen + ", gc.isBoardTurn: " + gc.isBoardTurn);

		if (!gc.cl.p_fen.equals(""))
		{
			gc.fen = gc.cl.p_fen;

			if (ec.chessEnginePlayMod == 4 && ec.uciEngines[ec.analysisBestScoreEngineId] != null && !ec.uciEngines[ec.analysisBestScoreEngineId].engineName.equals("")) {
				double bestScore = ec.analysisBestScore / 100;

//				Log.i(TAG, "3 updateGui(), bestScore: " + bestScore + ", engineName: " + ec.uciEngines[ec.analysisBestScoreEngineId].engineName);

				String bestScoreStr = getDisplayScore(ec.analysisBestScore, gc.cl.p_fen).toString();
				if (ec.uciEnginesMessage[ec.analysisBestScoreEngineId].contains("1(M")) {
					bestScoreStr = "M" + Math.abs(ec.analysisBestScore);
				}
				if (!gc.isBoardTurn) {
					if (gc.getValueFromFen(gc.cl.p_fen, 2).equals("b")) {
						lblPlayerNameA.setText(ec.uciEngines[ec.analysisBestScoreEngineId].engineName);
						lblPlayerEloA.setText(String.valueOf(bestScoreStr));
						lblPlayerNameB.setText("");
					}
					else {
						lblPlayerNameA.setText("");
						lblPlayerNameB.setText(ec.uciEngines[ec.analysisBestScoreEngineId].engineName);
						lblPlayerEloB.setText(String.valueOf(bestScoreStr));
					}
				}
				else {
					if (gc.getValueFromFen(gc.cl.p_fen, 2).equals("w")) {
						lblPlayerNameA.setText(ec.uciEngines[ec.analysisBestScoreEngineId].engineName);
						lblPlayerEloA.setText(String.valueOf(bestScoreStr));
						lblPlayerNameB.setText("");
					}
					else {
						lblPlayerNameA.setText("");
						lblPlayerNameB.setText(ec.uciEngines[ec.analysisBestScoreEngineId].engineName);
						lblPlayerEloB.setText(String.valueOf(bestScoreStr));
					}
				}
			}

			if (gc.cl.p_color.equals("w"))
			{
				if (!gc.isBoardTurn)
				{
					u.setTextViewColors(lblPlayerNameA, cv, cv.COLOR_DATA_BACKGROUND_16, cv.COLOR_DATA_TEXT_17);
					u.setTextViewColors(lblPlayerEloA, cv, cv.COLOR_DATA_BACKGROUND_16, cv.COLOR_DATA_TEXT_17);
					u.setTextViewColors(lblPlayerNameB, cv, cv.COLOR_HIGHLIGHTING_22, cv.COLOR_DATA_TEXT_17);
					u.setTextViewColors(lblPlayerEloB, cv, cv.COLOR_HIGHLIGHTING_22, cv.COLOR_DATA_TEXT_17);

				}
				else
				{
					u.setTextViewColors(lblPlayerNameA, cv, cv.COLOR_HIGHLIGHTING_22, cv.COLOR_DATA_TEXT_17);
					u.setTextViewColors(lblPlayerEloA, cv, cv.COLOR_HIGHLIGHTING_22, cv.COLOR_DATA_TEXT_17);
					u.setTextViewColors(lblPlayerNameB, cv, cv.COLOR_DATA_BACKGROUND_16, cv.COLOR_DATA_TEXT_17);
					u.setTextViewColors(lblPlayerEloB, cv, cv.COLOR_DATA_BACKGROUND_16, cv.COLOR_DATA_TEXT_17);
				}
			}
			else
			{
				if (!gc.isBoardTurn)
				{
					u.setTextViewColors(lblPlayerNameA, cv, cv.COLOR_HIGHLIGHTING_22, cv.COLOR_DATA_TEXT_17);
					u.setTextViewColors(lblPlayerEloA, cv, cv.COLOR_HIGHLIGHTING_22, cv.COLOR_DATA_TEXT_17);
					u. setTextViewColors(lblPlayerNameB, cv, cv.COLOR_DATA_BACKGROUND_16, cv.COLOR_DATA_TEXT_17);
					u.setTextViewColors(lblPlayerEloB, cv, cv.COLOR_DATA_BACKGROUND_16, cv.COLOR_DATA_TEXT_17);
				}
				else
				{
					u.setTextViewColors(lblPlayerNameA, cv, cv.COLOR_DATA_BACKGROUND_16, cv.COLOR_DATA_TEXT_17);
					u.setTextViewColors(lblPlayerEloA, cv, cv.COLOR_DATA_BACKGROUND_16, cv.COLOR_DATA_TEXT_17);
					u.setTextViewColors(lblPlayerNameB, cv, cv.COLOR_HIGHLIGHTING_22, cv.COLOR_DATA_TEXT_17);
					u.setTextViewColors(lblPlayerEloB, cv, cv.COLOR_HIGHLIGHTING_22, cv.COLOR_DATA_TEXT_17);
				}
			}
		}

//		Log.i(TAG, "3 updateGui()");

		if (gc.cl.p_message.equals("*"))
			gc.cl.p_message = "";

		if (gc.cl.p_moveText.equals(""))
		{
			if (!gc.errorMessage.equals(""))
			{
				u.setTextViewColors(msgMoves, cv, cv.COLOR_HIGHLIGHTING_22, cv.COLOR_DATA_TEXT_17);
				messInfo = gc.errorMessage;
			}
		}
		else
			u.setTextViewColors(msgMoves, cv, cv.COLOR_MOVES_ANOTATION_11, cv.COLOR_DATA_TEXT_17);

//		Log.i(TAG, "updateGui(), setInfoMessage()");

		if (!messInfo.equals(""))
		{

			if (gc.cl.p_message.equals(""))
				setInfoMessage(gc.cl.p_message + "" + messInfo, messageEngine, gc.cl.p_moveText);
			else
				setInfoMessage(gc.cl.p_message + ", " + messInfo, messageEngine, gc.cl.p_moveText);
		}
		else
		{
			setInfoMessage(gc.cl.p_message, messageEngine, gc.cl.p_moveText);
		}
		if (gc.cl.p_moveHasVariations | gc.cl.p_moveIsFirstInVariation)	// move has variation? | variation start
			gc.hasVariations = true;
		else
			gc.hasVariations = false;

		if (!gc.cl.p_fen.equals(""))
		{

//			Log.i(TAG, "4 updateGui()");
//			Log.i(TAG, "gc.cl.p_move, possibleMoves: " + gc.cl.p_move + ", " + gc.cl.p_hasPossibleMoves);
//			Log.i(TAG, "gc.cl.p_move1, gc.cl.p_move2: " + gc.cl.p_move1 + ", " + gc.cl.p_move2);
//			Log.i(TAG, "gc.cl.p_moveShow1, gc.cl.p_moveShow2: " + gc.cl.p_moveShow1 + ", " + gc.cl.p_moveShow2);

//			Log.i(TAG, "1 updateGui(), arrowsId: " + arrowsId + ", displayMoves: " + displayMoves);

			ArrayList<CharSequence> displayArrows = new ArrayList<>();
			if (displayMoves != null && (ec.chessEnginePlayMod <= 4)) {

				String[] lineSplit = displayMoves.toString().split(" ");
				for (int i = 0; i < lineSplit.length; i++)
				{
					displayArrows.add(lineSplit[i]);
//					if (arrowsId == BoardView.ARROWS_BEST_MOVES && i == 0)
//						break;
				}

			}

//			Log.i(TAG, "2 updateGui(), arrowsId: " + arrowsId + ", displayMoves: " + displayMoves);

			ArrayList<CharSequence> scoreArrows = new ArrayList<>();
			if (scoreMoves != null && (ec.chessEnginePlayMod == 1 || ec.chessEnginePlayMod == 2 || ec.chessEnginePlayMod == 4)) {
				String[] lineSplit = scoreMoves.toString().split(" ");
				for (int i = 0; i < lineSplit.length; i++)
				{
					scoreArrows.add(lineSplit[i]);
//					if (arrowsId == BoardView.ARROWS_BEST_MOVES && i == 0)
//						break;
				}
			}

			ArrayList<CharSequence> possibleMoves = null;
			ArrayList<CharSequence> possibleMovesTo = null;
			CharSequence lastMove = null;
			if (gc.cl.p_hasPossibleMovesTo & gc.cl.p_possibleMoveToList != null)
			{
				if (gc.cl.p_possibleMoveToList.size() >= 2)
					possibleMovesTo = gc.cl.p_possibleMoveToList;
			}
			else
			{
				if (gc.cl.p_hasPossibleMoves & gc.cl.p_possibleMoveList != null)
				{
					if (gc.cl.p_stat.equals("0") & gc.cl.p_possibleMoveList.size() > 0)
						possibleMoves = gc.cl.p_possibleMoveList;
				} else
				{
					if (gc.cl.p_moveShow1.equals(""))
						lastMove = gc.cl.p_move1.toString() + gc.cl.p_move2;
					else
						lastMove = gc.cl.p_moveShow1.toString() + gc.cl.p_moveShow2;
				}
			}

//			Log.i(TAG, "possibleMoves: " + possibleMoves + ", possibleMovesTo: " + possibleMovesTo + ", lastMove: " + lastMove + ", gc.move: " + gc.move);
//			Log.i(TAG, "possibleMovesTo: " + possibleMovesTo + ", possibleMovesTo.size(): " + possibleMovesTo.size());

//			Log.i(TAG, "3 updateGui(), lastMove: " + lastMove);

//			Log.i(TAG, "updateGui(), updateBoardView(), arrowsId: " + arrowsId + ". displayArrows: " + displayArrows);

			boardView.updateBoardView(gc.cl.p_fen, gc.isBoardTurn, arrowsId, displayArrows, scoreArrows, possibleMoves, possibleMovesTo,
					lastMove, null, userPrefs.getBoolean("user_options_gui_Coordinates", false),
					userPrefs.getBoolean("user_options_gui_BlindMode", false));

		}
		if (ec.chessEnginePlayMod != 6)
		{
			if (!gc.isBoardTurn)
			{
				lblPlayerTimeA.setText(tc.showBlackTime);
				lblPlayerTimeB.setText(tc.showWhiteTime);
			}
			else
			{
				lblPlayerTimeA.setText(tc.showWhiteTime);
				lblPlayerTimeB.setText(tc.showBlackTime);
			}
			updateTime(gc.cl.p_color);
        }

//		Log.i(TAG, "3 updateGui(), END");

	}

	public void initPosition(Boolean isStandard)
	{
		Random r;
		int ir = 518;
		if (!isStandard)
		{
			r = new Random();
			ir = r.nextInt(960);
		}
		CharSequence chess960Id = Integer.toString(ir);
		gc.cl.newPosition(chess960Id, "", "", "", "", "", "", "");
		if (gc.cl.p_stat.equals("1"))
		{
			gc.isGameOver = false;
			gc.isGameUpdated = true;
			gc.fen = gc.cl.p_fen;

//			Log.i(TAG, "initPosition(), gc.fen: " + gc.fen);

			if (gc.cl.p_chess960ID == 518)
				gc.isChess960 = false;
			else
				gc.isChess960 = true;
			SharedPreferences.Editor ed = userPrefs.edit();
			ed.putInt("user_game_chess960Id", gc.cl.p_chess960ID);
			ed.commit();
		}
	}

	public int getMsgEngineLines()
	{
		if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
			return userPrefs.getInt("user_options_enginePlay_displayedLines", Settings.LINES_DEFAULT);
		else
			return userPrefs.getInt("user_options_enginePlay_displayedLines_land", Settings.LINES_DEFAULT_LAND);
	}

	public void setEngineDirectories()
	{
		String PATH_FILES = String.valueOf(getExternalFilesDir(null));	// 		/storage/emulated/0/Android/data/ccc.chess.gui.chessforall/files
		String PATH_FILES_ENGINES = PATH_FILES + File.separator + "engines";
		String PATH_FILES_ENGINES_LOGFILE = PATH_FILES + File.separator + "engines" + File.separator + "logfile";
		String PATH_FILES_ENGINES_NNUE = PATH_FILES + File.separator + "engines" + File.separator + "nnue";
		String PATH_FILES_ENGINES_WEIGHTS = PATH_FILES + File.separator + "engines" + File.separator + "weights";
		File dir = new File(PATH_FILES);
		if (!dir.exists()) { dir.mkdirs(); }
		dir = new File(PATH_FILES_ENGINES);
		if (!dir.exists()) { dir.mkdirs(); }
		dir = new File(PATH_FILES_ENGINES_LOGFILE);
		if (!dir.exists()) { dir.mkdirs(); }
		dir = new File(PATH_FILES_ENGINES_NNUE);
		if (!dir.exists()) { dir.mkdirs(); }
		dir = new File(PATH_FILES_ENGINES_WEIGHTS);
		if (!dir.exists()) { dir.mkdirs(); }
	}

	public class MyViewListener implements View.OnClickListener
	{

		public void onClick(View v)
		{
			SharedPreferences.Editor ed = userPrefs.edit();
			switch (v.getId())
			{
				// cb0
				case R.id.cb_debugInformation:
					ed.putBoolean("user_options_enginePlay_debugInformation", ((CheckBox)v).isChecked());
					ed.commit();
					break;
				case R.id.cb_logging:
					ed.putBoolean("user_options_enginePlay_logOn", ((CheckBox)v).isChecked());
					ed.commit();
					break;
				// cb1
				case R.id.cb_screenTimeout:
					ed.putBoolean("user_options_gui_disableScreenTimeout", ((CheckBox)v).isChecked());
					ed.commit();
					break;
				case R.id.cb_engineAutostart:
					ed.putBoolean("user_options_enginePlay_AutoStartEngine", ((CheckBox)v).isChecked());
					ed.commit();
					break;
				// cb2
				case R.id.cb_fullScreen:
					ed.putBoolean("user_options_gui_StatusBar", ((CheckBox)v).isChecked());
					ed.commit();
					u.updateFullscreenStatus(MainActivity.this, userPrefs.getBoolean("user_options_gui_StatusBar", false));
					updateGui();
					break;
				case R.id.cb_boardFlip:
					ed.putBoolean("user_options_gui_FlipBoard", ((CheckBox)v).isChecked());
					ed.commit();
					updateGui();
					break;
				// cb3
				case R.id.cb_lastPosition:
					ed.putBoolean("user_options_gui_LastPosition", ((CheckBox)v).isChecked());
					ed.commit();
					break;
				case R.id.cb_pgnDb:
					ed.putBoolean("user_options_gui_usePgnDatabase", ((CheckBox)v).isChecked());
					ed.commit();
					break;
				// cb4
				case R.id.cb_coordinates:
					ed.putBoolean("user_options_gui_Coordinates", ((CheckBox)v).isChecked());
					ed.commit();
					updateGui();
					break;
				case R.id.cb_blindMode:
					ed.putBoolean("user_options_gui_BlindMode", ((CheckBox)v).isChecked());
					ed.commit();
					updateGui();
					break;
				// cb5
				case R.id.cb_openingBook:
					ed.putBoolean("user_options_enginePlay_OpeningBook", ((CheckBox)v).isChecked());
					ed.commit();
					break;
				case R.id.cb_openingBookHints:
					ed.putBoolean("user_options_enginePlay_ShowBookHints", ((CheckBox)v).isChecked());
					ed.commit();
					break;
				// cb6
				case R.id.cb_posibleMoves:
					ed.putBoolean("user_options_gui_posibleMoves", ((CheckBox)v).isChecked());
					ed.commit();
					updateGui();
					break;
				case R.id.cb_quickMove:
					ed.putBoolean("user_options_gui_quickMove", ((CheckBox)v).isChecked());
					ed.commit();
					break;
				// cb7
				case R.id.cb_audio:
					ed.putBoolean("user_options_gui_enableSounds", ((CheckBox)v).isChecked());
					ed.commit();
					break;
				case R.id.cb_moveList:
					ed.putBoolean("user_options_gui_moveList", ((CheckBox)v).isChecked());
					ed.commit();
					updateGui();
					break;
				// cb8
				case R.id.cb_ponder:
					ed.putBoolean("user_options_enginePlay_Ponder", ((CheckBox)v).isChecked());
					ed.commit();
					updateGui();
					break;
				case R.id.cb_engineThinking:
					if (!((CheckBox)v).isChecked())
						Toast.makeText(MainActivity.this, getString(R.string.displayDisabled), Toast.LENGTH_SHORT).show();
					ed.putBoolean("user_options_enginePlay_EngineMessage", ((CheckBox)v).isChecked());
					ed.commit();
					updateGui();
					break;

				// btn_menues
				case R.id.btn_menu_left:
					removeDialog(PLAY_DIALOG);
					drawerLayout.openDrawer(Gravity.LEFT);
					break;
				case R.id.btn_menu_right:
					removeDialog(PLAY_DIALOG);
					drawerLayout.openDrawer(Gravity.RIGHT);
					break;

				// btn_time
				case R.id.btn_time_setting:
					stopSearchAndContinue(EngineState.STOP_IDLE, "", true);
					startActivityForResult(optionsTimeControlIntent, OPTIONS_TIME_CONTROL_REQUEST_CODE);
					removeDialog(PLAY_DIALOG);
					restartPlayDialog = true;
					break;
				case R.id.btn_time_white:
					if (ec.chessEnginePlayMod <= 3 || ec.chessEnginePlayMod == 5) {
						setTimeWhiteBlack(1);
					}
					break;
				case R.id.btn_time_black:
					if (ec.chessEnginePlayMod <= 3 || ec.chessEnginePlayMod == 5) {
						setTimeWhiteBlack(2);
					}
					break;
				case R.id.btn_elo:
					removeDialog(UCI_ELO_DIALOG);
					showDialog(UCI_ELO_DIALOG);
					removeDialog(PLAY_DIALOG);
					restartPlayDialog = true;
					break;

				// btn_engines
				case R.id.btn_engine_select:
					showDialog(MENU_SELECT_ENGINE_FROM_OEX);
					removeDialog(PLAY_DIALOG);
					restartPlayDialog = true;
					break;
				case R.id.btn_engine_uci_options:
					startEditUciOptions(runP.getString("run_engineProcess", MainActivity.OEX_DEFAULT_ENGINE_SINGLE));
					removeDialog(PLAY_DIALOG);
					restartPlayDialog = true;
					break;
				case R.id.btn_settings:
					stopComputerThinking(false, false);
					startActivityForResult(optionsSettingsIntent, OPTIONS_SETTINGS);
					removeDialog(PLAY_DIALOG);
					restartPlayDialog = true;
					break;

				// btn_play_a
                case R.id.btn_white:
                    dChessEnginePlayMod = 1;
                    setPlayModBackground(dChessEnginePlayMod);
					displayPlayModTime(dChessEnginePlayMod);
                    break;
                case R.id.btn_black:
					dChessEnginePlayMod = 2;
                    setPlayModBackground(dChessEnginePlayMod);
					displayPlayModTime(dChessEnginePlayMod);
                    break;
                case R.id.btn_engine:
					dChessEnginePlayMod = 3;
                    setPlayModBackground(dChessEnginePlayMod);
					displayPlayModTime(dChessEnginePlayMod);
                    break;

                // btn_play_b
                case R.id.btn_player:
					dChessEnginePlayMod = 5;
                    setPlayModBackground(dChessEnginePlayMod);
					displayPlayModTime(dChessEnginePlayMod);
                    break;
                case R.id.btn_edit:
					dChessEnginePlayMod = 6;
                    setPlayModBackground(dChessEnginePlayMod);
					displayPlayModTime(dChessEnginePlayMod);
                    break;
                case R.id.btn_analysis:
					dChessEnginePlayMod = 4;
                    setPlayModBackground(dChessEnginePlayMod);
					displayPlayModTime(dChessEnginePlayMod);
                    break;

				// btn_pos
				case R.id.btn_standard:
				case R.id.btn_chess960:
				case R.id.btn_continue:
					removeDialog(PLAY_DIALOG);
					setPlayModPrefs(dChessEnginePlayMod);
					if (v.getId() == R.id.btn_standard) {
						dNewGame = true;
						gc.isChess960 = false;
						initPosition(true);
					}
					if (v.getId() == R.id.btn_chess960) {
						dNewGame = true;
						gc.isChess960 = true;
						initPosition(false);
					}
					gc.isGameLoaded = false;
					msgEngine.setVisibility(TextView.GONE);
					messageInfo 		= "";
					messageEngine = new SpannableStringBuilder("");
					messageEngineShort  = "";
					ec.chessEngineMatch = false;
					ec.chessEnginePaused = false;
					ec.chessEngineInit = false;

					stopChessClock();

					if (v.getId() == R.id.btn_continue) {
						dNewGame = false;
						if (dChessEnginePlayMod == ec.chessEnginePlayMod)
							dSetClock = false;
						else
							dSetClock = true;
						if (dTimeControl) {
							dTimeControl = false;
							dSetClock = true;
						}
						if (dSettingTimeWhite != 0)
							tc.timeWhite = dSettingTimeWhite;
						if (dSettingTimeBlack != 0)
							tc.timeBlack = dSettingTimeBlack;

						if ((dRestartEngine || dChessEnginePlayMod != ec.chessEnginePlayMod) && dChessEnginePlayMod <= 4) {
							ec.chessEnginePlayMod = dChessEnginePlayMod;

//							Log.i(TAG, "1 onClick(), btn_continue, stopMultiEnginesAndRestart(), dSetClock: " + dSetClock + ", dRestartEngine: " + dRestartEngine + ", tc.timeBlack: " + tc.timeBlack);

							if (dRestartEngine || dSetClock) {
								initChessClock(tc, userPrefs.getInt("user_options_timeControl", 1), ec.chessEnginePlayMod);
								startChessClock();
							}
							dRestartEngine = false;
							stopMultiEnginesAndRestart();
							setInfoMessage(getString(R.string.engineInit), null, null);
							return;
						}

					}

//					Log.i(TAG, "2 onClick(), btn_continue, dNewGame: " + dNewGame + ", dSetClock: " + dSetClock);

					switch (dChessEnginePlayMod)
					{
						case 1:     // white
						case 2:     // black
						case 3:     // engine vs engine
							if (dNewGame)
								stopSearchAndRestart(dNewGame, dSetClock);
							else
								stopSearchAndContinue(EngineState.STOP_CONTINUE, gc.cl.p_fen, true);
							break;
						case 4:     // analysis
							stopSearchAndRestart(dNewGame, true);
							break;
						case 5:     // two players
							analysisMessage = "";
							ec.chessEnginePaused = true;
							stopComputerThinking(false, false);
							ec.chessEnginePlayMod = dChessEnginePlayMod;
							startEdit(dNewGame, true);
							break;
						case 6:     // edit
							analysisMessage = "";
							ec.chessEnginePaused = true;
							stopComputerThinking(false, false);
							setEnginePausePlayBtn(null, null);
							ec.chessEnginePlayMod = dChessEnginePlayMod;
							startEdit(dNewGame, false);
							break;
					}
					break;

				// RATE_DIALOG
				case R.id.btn_rate:
					removeDialog(RATE_DIALOG);
					Uri uri = Uri.parse("market://details?id=" + getPackageName());
					Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
					goToMarket.addFlags (       Intent.FLAG_ACTIVITY_NO_HISTORY
                                            |	Intent.FLAG_ACTIVITY_NEW_DOCUMENT
                                            |	Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                                        );
					try {startActivity(goToMarket);}
					catch (ActivityNotFoundException e)
                    {
						startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://play.google.com/store/apps/details?id=" + getPackageName())));
					}
					break;
				case R.id.btn_no:
					removeDialog(RATE_DIALOG);
					break;
			}
		}
	}

	final String TAG = "MainActivity";
	final CharSequence APP_EMAIL = "c4akarl@gmail.com";
	final String MY_APPS = "https://play.google.com/store/apps/developer?id=Karl+Schreiner";
	final long CLOCK_START = 0;
//	final long CLOCK_DELAY = 250;
	final long CLOCK_DELAY = 500;

	public final static String OEX_DEFAULT_ENGINE_SINGLE = "Rodent IV 0.32 CfA";
	public final static String OEX_OLD_STOCKFISH = "Stockfish 11 CfA";
	public final static String OEX_DEFAULT_STOCKFISH = "Stockfish 13 CfA";
	public final static String OEX_DEFAULT_ENGINES_MATCH = "Rodent IV 0.32 CfA|Stockfish 13 CfA";
	public final static String OEX_DEFAULT_ENGINES_ANALYSIS = "Rodent IV 0.32 CfA|Stockfish 13 CfA";

	private static final int PERMISSIONS_REQUEST_CODE = 50;
	private enum PermissionState {
		UNKNOWN,
		REQUESTED,
		GRANTED,
		DENIED
	}
	private PermissionState storagePermission = PermissionState.UNKNOWN;
	private PermissionState internetPermission = PermissionState.UNKNOWN;
	private PermissionState wakeLockPermission = PermissionState.UNKNOWN;

	private static String engineDir = "c4a/uci";

	Util u;

	public SharedPreferences moveHistoryP;
	public SharedPreferences userPrefs;
	public SharedPreferences runP;
	public SharedPreferences fmPrefs;

	public CharSequence moveHistory;
	public ArrayList<CharSequence> stringValues = new ArrayList<CharSequence>();
	boolean isAppStart = true;
	boolean isAppEnd = false;
	boolean twoPlayerPaused = false;
	public WakeLock wakeLock = null;
	public boolean useWakeLock = false;
	int restartSearchCnt = 0;
	LinkedHashMap<Integer, Integer> sortedScore = new LinkedHashMap<>();
	int[] sortedIdx;

	// pause control (manually | automatically)
	boolean 		pause_auto 					= false;
	CharSequence 	pause_fen 					= "";
	int 			pause_mode 					= 0;
	CharSequence 	pause_messageEngine 		= "";

	GameControl gc;
	EngineControl ec;
	private EngineListener listener = null;
	ArrayList<String> engineNames = null; // oex engine names
	TimeControl tc;
	Chess960 chess960;
	ColorValues cv;
	UciEngine engine; // for uci options

	ChessPromotion promotionDialog;
	FileIO fileIO;
	private SoundPool mSoundPool;
	private HashMap<Integer, Integer> soundsMap;

	//	subActivities intents
	Intent starterIntent;
	Intent fileManagerIntent;
	Intent gameDataIntent;
	Intent notationIntent;
	Intent moveTextIntent;
	Intent optionsTimeControlIntent;
	Intent optionsSettingsIntent;
	Intent optionsColorIntent;
	Intent editChessBoardIntent;
	Intent editUciOptions;
	Intent computerMatch;
	Intent analysisOptions;

	//	subActivities RequestCode
	final static int LOAD_GAME_REQUEST_CODE = 1;
	final static int LOAD_GAME_PREVIOUS_CODE = 9;
	final static int SAVE_GAME_REQUEST_CODE = 2;
	final static int SAVE_LOAD_GAME_REQUEST_CODE = 7;
	final static int SAVE_OK_LOAD_GAME_REQUEST_CODE = 71;
	final static int SAVE_ERROR_LOAD_GAME_REQUEST_CODE = 72;
	final static int LOAD_INTERN_ENGINE_REQUEST_CODE = 82;
	final static int GAME_DATA_REQUEST_CODE = 49;
	final static int NOTATION_REQUEST_CODE = 5;
	final static int MOVETEXT_REQUEST_CODE = 6;
	final static int OPTIONS_CHESSBOARD_REQUEST_CODE = 13;
	final static int OPTIONS_GUI_REQUEST_CODE = 14;
	final static int OPTIONS_TIME_CONTROL_REQUEST_CODE = 18;
	final static int OPTIONS_SETTINGS = 21;
	final static int OPTIONS_COLOR_SETTINGS = 22;
	final static int EDIT_CHESSBOARD_REQUEST_CODE = 44;
	final static int ENGINE_SETTING_REQUEST_CODE = 41;
	final static int RATE_REQUEST_CODE = 42;
	final static int EDIT_UCI_OPTIONS = 51;
	final static int COMPUTER_MATCH = 61;
	final static int ANALYSIS_OPTIONS = 62;

	//  dialogs RequestCode
	final static int PLAY_DIALOG = 100;
	final static int C4A_DIALOG = 111;
	String msgC4aDialog = "";

	final static int MOVE_NOTIFICATION_DIALOG = 110;
	final static int NO_FILE_ACTIONS_DIALOG = 193;
	final static int DOWNLOAD_ERROR_DIALOG = 195;
	final static int PGN_ERROR_DIALOG = 198;
	final static int FILE_LOAD_PROGRESS_DIALOG = 199;
	final static int VARIATION_DIALOG = 200;
	final static int NAG_DIALOG = 201;
	final static int PROMOTION_DIALOG = 300;
	final static int TIME_SETTINGS_DIALOG = 400;
	final static int QUERY_DIALOG = 600;
	final static int GAME_ID_DIALOG = 601;
	final static int UCI_ELO_DIALOG = 610;
	final static int COMPUTER_MATCH_DIALOG = 620;
	final static int MENU_BOARD_DIALOG = 701;
	final static int MENU_ENGINES_DIALOG = 704;
	final static int MENU_ABOUT_DIALOG = 706;
	final static int MENU_PGN_DIALOG = 730;
	final static int MENU_CLIPBOARD_DIALOG = 731;
	final static int MENU_COLOR_SETTINGS = 732;
	final static int MENU_SHOW_LIST = 733;
	final static int MENU_SELECT_ENGINE_FROM_OEX = 740;
	final static int INFO_DIALOG = 909;	// device info; not activated
	final static int RATE_DIALOG = 910;
	final static int C4A_NEW_DIALOG = 999;

	//	GUI (R.layout.main)
	private DrawerLayout drawerLayout;
	private ListView leftDrawer;
	private ListView rightDrawer;

	TextView lblPlayerNameA = null;
	TextView lblPlayerEloA = null;
	TextView lblPlayerTimeA = null;
	TextView lblPlayerNameB = null;
	TextView lblPlayerEloB = null;
	TextView lblPlayerTimeB = null;

	TextView msgShort = null;
	TextView msgShort2 = null;
	ScrollView scrlMsgMoves = null;
	TextView msgMoves = null;
	ScrollView scrlMsgEngine = null;
	TextView msgEngine = null;

	BoardView boardView;

	ImageView btn_1 = null;
	ImageView btn_2 = null;
	ImageView btn_3 = null;
	ImageView btn_4 = null;
	ImageView btn_5 = null;
	ImageView btn_6 = null;
	ImageView btn_7 = null;

	//	Dialog
	C4aDialog c4aDialog;
	ProgressDialog progressDialog = null;
	TimeSettingsDialog timeSettingsDialog;
	int activDialog = 0;
	boolean restartPlayDialog = false;

	//	handler
	public Handler handlerAutoPlay = new Handler();
	public Handler handlerChessClock = new Handler();

	//  variables time settings
	int chessClockControl = 0;
	CharSequence chessClockTitle = "";
	CharSequence chessClockMessage = "";
	int chessClockTimeGame = 0;
	int chessClockTimeBonus = 0;
	int chessClockMovesToGo = -1;
	int chessClockTimeBonusSaveWhite = 0;
	int chessClockTimeBonusSaveBlack = 0;
	int lastTouchID = 0;
	long touchTime = 0;
	long longTouchTime = 600;
	CharSequence searchTaskFen = "";
	CharSequence searchTaskMoves = "";
	int arrowsId = BoardView.ARROWS_NONE;
	CharSequence displayMoves = null;
	CharSequence scoreMoves = null;

	long engineControlTime = 0;
	long engineMatchControlTime = 0;

	int cntChess960 = 0;

	//  touch variables
	int displayWidth = 0;
	int boardSize = 0;
	int aspectRatio = 0;
	int minScrollingWidth = 150;
	float downRawX = 0;
	float downRawY = 0;
	float upRawX = 0;
	float upRawY = 0;
	boolean isDownBtn = false;
	boolean isUpBtn = false;
	boolean isUpMsgView = false;
	int downViewId = 0;
	int infoMoveStartX = 0;
	int infoMoveEndX = 0;
	boolean infoMoveIsSelected = false;
	boolean infoShowPv = false;

	//  variables
	int gridViewSize = 0;
	CharSequence oldTimeWhite = "";
	CharSequence oldTimeBlack = "";
	CharSequence 	messageInfo 		= "";
	SpannableStringBuilder messageEngine;
	CharSequence 	messageEngineShort 	= "";
	CharSequence engineMes = "";
	CharSequence engineStat = "";
	ArrayList<CharSequence> infoPv;
	ArrayList<CharSequence> infoMessage;
	CharSequence analysisMessage = "";
	CharSequence showGameCount = "";
	SpannableStringBuilder sb = new SpannableStringBuilder();
	String queryControl = "w";
	String internEngineName = "";
	String downloadErrorMessage = "";

	Bitmap imageHuman = null;
	Bitmap imageComputer = null;
	Bitmap imageAnalysis = null;
	Bitmap imageWhite = null;
	Bitmap imageBlack = null;
	Bitmap imageBlue = null;
	Bitmap imageYellow = null;
	Bitmap imagePink = null;

	boolean isGoPonder = false;		// set to true if start: go ponder
	CharSequence continueFen = "";
	CharSequence restartFen = "";

	// PLAY_DIALOG
	Dialog playDialog = null;
	ScrollView d_scrollView;
	// cb0
	CheckBox d_cb_debugInformation;
	CheckBox d_cb_logging;
	// cb1
	CheckBox d_cb_screenTimeout;
	CheckBox d_cb_engineAutostart;
	// cb2
	CheckBox d_cb_fullScreen;
	CheckBox d_cb_boardFlip;
	// cb3
	CheckBox d_cb_lastPosition;
	CheckBox d_cb_pgnDb;
	// cb4
	CheckBox d_cb_coordinates;
	CheckBox d_cb_blindMode;
	// cb5
	CheckBox d_cb_openingBook;
	CheckBox d_cb_openingBookHints;
	// cb6
	CheckBox d_cb_posibleMoves;
	CheckBox d_cb_quickMove;
	// cb7
	CheckBox d_cb_audio;
	CheckBox d_cb_moveList;
	// cb8
	CheckBox d_cb_ponder;
	CheckBox d_cb_engineThinking;

	// btn_menues
	TextView d_btn_menu_left;
	TextView d_btn_menu_right;

	// btn_time
	TextView d_btn_time_setting;
	TextView d_btn_time_white;
	TextView d_btn_time_black;
	TextView d_btn_elo;

	// btn_engines
	TextView d_btn_engine_select;
	TextView d_btn_engine_uci_options;
	TextView d_btn_settings;

	// btn_play_a
	TextView d_btn_white;
	TextView d_btn_black;
	TextView d_btn_engine;

	// btn_play_b
	TextView d_btn_player;
	TextView d_btn_edit;
	TextView d_btn_analysis;

	// btn_pos
	TextView d_btn_standard;
	TextView d_btn_chess960;
	TextView d_btn_continue;

	// RATE_DIALOG
	TextView btn_rate;
	TextView btn_no;

	// UCI_ELO_DIALOG
	int elo = 1600;
	int eloMin = 800;
	int eloMax = 3000;
	public TextView eloEngine;
	public EditText eloValue;
	public TextView eloInfo;
	public TextView info;
	public SeekBar eloSeekBar;
	TextView btn_cancel;
	TextView btn_ok;

	boolean dNewGame = false;
	boolean dRestartEngine = false;
	boolean dSetClock = false;
	boolean dTimeControl = false;
	int dChessEnginePlayMod = 1;
	int dSettingTimeWhite = 0;
	int dSettingTimeBlack = 0;

	boolean isStopAutoPlay = false;
//	int dContinueId = 3; 	// 1 new game, 2 continue, set clock, 3 continue


	//karl --> test settings
//	boolean fileActions = true;		// sdk >= 30
	boolean fileActions = false;

	//karl???
	final static boolean withMatchPonder = false;
//	final static boolean withMatchPonder = true;

//	final static boolean withMultiEngineAnalyse = false;
	final static boolean withMultiEngineAnalyse = true;

	final static boolean withSortAnalyse = false;
//	final static boolean withSortAnalyse = true;

}