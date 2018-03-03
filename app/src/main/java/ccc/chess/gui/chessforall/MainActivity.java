package ccc.chess.gui.chessforall;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.ClipboardManager;
import android.text.InputType;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.BackgroundColorSpan;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import ccc.chess.book.ChessParseError;
import ccc.chess.book.Move;
import ccc.chess.book.TextIO;
import ccc.chess.logic.c4aservice.Chess960;
import ccc.chess.logic.c4aservice.ChessPosition;

public class MainActivity extends Activity implements Ic4aDialogCallback, OnTouchListener
{
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
		u = new Util();
        runP = getSharedPreferences("run", 0);		//	run Preferences
        moveHistoryP = getSharedPreferences("moves", 0);	//	moveHistory Preferences
        moveHistory = moveHistoryP.getString("run_moveHistory", "");
        userPrefs = getSharedPreferences("user", 0);	// 	user Preferences
        fmPrefs = getSharedPreferences("fm", 0);		// 	fileManager(PGN) Preferences
        setStringsValues();
        gc = new GameControl(stringValues, moveHistory);
        ec = new EngineControl(MainActivity.this);				//>011 engine controller
        ec.setBookOptions();
		tc = new TimeControl();
		getPermissions();
		chess960 = new Chess960();	// needed for "create your own chess position"
		pgnIO = new PgnIO(this);

        startC4a();

    }


	public void startC4a()
	{
		getRunPrefs();	// run preferences
		PowerManager pm = (PowerManager)getSystemService(Context.POWER_SERVICE);
		setWakeLock(false);
		wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "c4a");
		wakeLock.setReferenceCounted(false);
		useWakeLock = userPrefs.getBoolean("user_options_gui_disableScreenTimeout", false);
		setWakeLock(useWakeLock);
		getChessFieldSize();

		startGui();

		mSoundPool = new SoundPool(2, AudioManager.STREAM_RING, 100);
		soundsMap = new HashMap<Integer, Integer>();
		soundsMap.put(1, mSoundPool.load(this, R.raw.move_ok, 1));
		soundsMap.put(2, mSoundPool.load(this, R.raw.move_wrong, 1));
		gc.cl.history.setFigurineAlgebraicNotation(getString(R.string.pieces));
		if (gc.initPrefs & userPrefs.getBoolean("user_options_gui_StatusBar", true))
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
		if (		userPrefs.getBoolean("user_options_enginePlay_AutoStartEngine", true)
				& 	!gc.isGameOver & !ec.chessEnginePaused)
		{
			ec.chessEnginePaused = false;
			ec.lastChessEnginePaused = false;
//Log.i(TAG, "moveIdx: " + gc.cl.p_moveIdx );
//Log.i(TAG, "history.getStartFen(): "  + gc.cl.history.getStartFen());
			setMoveTime();
			if (gc.cl.p_moveIdx == 0 & gc.cl.history.getStartFen().toString().contains("/8/8/8/8/"))	// move idx 0, new game
				startPlay(true, true);
			else
				startPlay(false, true);
		}
		else
		{
			if (!userPrefs.getBoolean("user_options_enginePlay_AutoStartEngine", true))
			{
				ec.chessEnginePaused = true;
				setInfoMessage(getEnginePausedMessage(), null, null);
			}
			if (gc.isGameOver | gc.cl.p_variationEnd)
			{
				ec.chessEnginePaused = true;
				ec.chessEngineSearching = false;
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
			if (ec.chessEnginePlayMod == 5)
				startEdit(false, true);
		}
		updateCurrentPosition("");
		getDataFromIntent(getIntent());

		if (userPrefs.getInt("user", 0) == 0)
		{
			SharedPreferences.Editor ed = userPrefs.edit();
			ed.putInt("user", 1);
			ed.commit();
			showDialog(C4A_NEW_DIALOG);
		}

	}

	//	GUI		GUI		GUI		GUI		GUI		GUI		GUI		GUI		GUI		GUI
	public void startGui()
	{
		u.updateFullscreenStatus(this, userPrefs.getBoolean("user_options_gui_StatusBar", true));
		aspectRatio = u.getAspectRatio(this);
		if (aspectRatio > 150)
			setContentView(R.layout.main);
		else
			setContentView(R.layout.main150);

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
		msgShort2 = (TextView) findViewById(R.id.msgShort2);
		msgShort2.setOnTouchListener(this);
		scrlMsgMoves = (ScrollView) findViewById(R.id.scrlMsgMoves);
		scrlMsgMoves.setOnTouchListener(this);
		scrlMsgMoves.setVerticalFadingEdgeEnabled(false);
		msgMoves = (TextView) findViewById(R.id.msgMoves);
		msgMoves.setOnTouchListener(this);

		scrlMsgEngine = (ScrollView) findViewById(R.id.scrlMsgEngine);
		msgEngine = (TextView) findViewById(R.id.msgEngine);
		msgEngine.setMovementMethod(new ScrollingMovementMethod());
//		msgEngine.setOnTouchListener(this);

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
		boardView.setOnTouchListener(this);
		boardView.updateBoardView(gc.fen, gc.isBoardTurn, null, null,
				null, false);

		initDrawers();

		playEngineIntent = new Intent(this, PlayEngineSettings.class);
		fileManagerIntent = new Intent(this, PgnFileManager.class);
		gameDataIntent = new Intent(this, ChessGameData.class);
		notationIntent = new Intent(this, ChessNotation.class);
		moveTextIntent = new Intent(this, ChessMoveText.class);
		optionsGuiIntent = new Intent(this, OptionsGUI.class);
		optionsTimeControlIntent = new Intent(this, OptionsTimeControl.class);
		optionsPlayIntent = new Intent(this, OptionsPlay.class);
		optionsEnginePlayIntent = new Intent(this, OptionsEnginePlay.class);
		editChessBoardIntent = new Intent(this, EditChessBoard.class);
	}

	// Initialize the drawer part of the user interface.
	final int MENU_EDIT  		= 1;
	final int MENU_PGN    		= 2;
	final int MENU_ENGINE      	= 3;
	final int MENU_SETTINGS    	= 4;
	final int MENU_INFO    		= 5;
	final int MENU_MANUAL  		= 6;

	final int MENU_NEW_GAME     = 10;
	final int MENU_GAME_MODE    = 11;
	final int MENU_GAME_RESIGN  = 12;
	final int MENU_GAME_DRAW    = 13;
	final int MENU_COMMENTS    	= 14;
	final int MENU_NAG      	= 15;
	final int MENU_GAME_DATA  	= 16;
	final int MENU_PGN_DATA    	= 17;

	private void initDrawers()
	{
		drawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
		leftDrawer = (ListView)findViewById(R.id.left_drawer);
		rightDrawer = (ListView)findViewById(R.id.right_drawer);
		final DrawerItem[] leftItems = new DrawerItem[]
		{
			new DrawerItem(MENU_EDIT, R.string.menu_modes_edit),
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
//err ANR: at ccc.chess.gui.chessforall.MainActivity$2.onItemClick (MainActivity.java:337)
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
		switch (itemId)
		{
			// leftDrawer
			case MENU_EDIT:
				removeDialog(MENU_EDIT_DIALOG);
				showDialog(MENU_EDIT_DIALOG);
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
				removeDialog(MENU_SETTINGS_DIALOG);
				showDialog(MENU_SETTINGS_DIALOG);
				break;
			case MENU_INFO:
				removeDialog(MENU_ABOUT_DIALOG);
				showDialog(MENU_ABOUT_DIALOG);
				break;
			case MENU_MANUAL:
//				showHtml("manual", R.string.menu_about_userManual);
				showHtml(R.raw.manual, R.string.menu_about_userManual);
				break;

			// rightDrawer
			case MENU_NEW_GAME:
				if (userPrefs.getInt("user_play_playMod", 1) != 4)
				{
					gc.isGameLoaded = false;
					ec.chessEnginePaused = false;
					ec.lastChessEnginePaused = false;
					ec.chessEngineInit = false;
					initChessClock();
					startPlay(true, true);
				}
				else
					startActivityForResult(optionsPlayIntent, OPTIONS_PLAY_REQUEST_CODE);
				break;
			case MENU_GAME_MODE:
				startActivityForResult(optionsPlayIntent, OPTIONS_PLAY_REQUEST_CODE);
				break;
			case MENU_GAME_RESIGN:
				CharSequence result = "";
				if (ec.chessEnginePlayMod == 1)
					result = "0-1";
				else
					result = "1-0";
				startC4aService315(result, "");
				break;
			case MENU_GAME_DRAW:
				if (ec.getEngine().statPvScore <= -500)
					startC4aService315("1/2-1/2", "");
				else
					setInfoMessage(getString(R.string.engineDeclinesDraw), null, null);
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
    	isAppEnd = true;
    	isSearchTaskStopped = true;
    	stopTimeHandler(true);
    	try {Thread.sleep(200);} 
		catch (InterruptedException e) {}
    	setInfoMessage("onDestroy", "", "");
    	updateCurrentPosition("");
    	setRunMoveHistory();
    	setRunPrefs();
     	if (chessEngineSearchTask != null)
    	{
     		chessEngineSearchTask.cancel(true);
     		chessEngineSearchTask = null;
    	}
     	wakeLock.release();
    	super.onDestroy();
     }

    @Override
    protected void onResume()
    {
//Log.i(TAG, "onResume(): " + ec.chessEnginePaused + ", isAppStart: " + isAppStart + ", gc.isAutoPlay: " + gc.isAutoPlay);
		u.updateFullscreenStatus(this, userPrefs.getBoolean("user_options_gui_StatusBar", true));
    	if (!isAppStart & gc.isAutoPlay)
				handlerAutoPlay.postDelayed(mUpdateAutoplay, 100);
    	else
    		isAppStart = false;
    	setWakeLock(useWakeLock);
    	if (progressDialog != null)
    	{
	    	if (progressDialog.isShowing())
	    		dismissDialog(FILE_LOAD_PROGRESS_DIALOG);
    	}
        super.onResume();
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
        getDataFromIntent(intent);
    }

	public boolean getDataFromIntent(Intent intent)
	{
		boolean isOk = false;
//Log.i(TAG, "intent.getFlags(): " + intent.getFlags() + ", FLAG_ACTIVITY_NEW_TASK: " + Intent.FLAG_ACTIVITY_NEW_TASK);
		// call from another Activity, passing the FEN(String)
		if (intent.getType() != null)
		{
			if (intent.getType().endsWith("x-chess-fenMes"))
			{
				if (intent.getStringExtra("fenMes") != null & !intent.getStringExtra("fenMes").equals(""))
				{
					editChessBoardIntent.putExtra("currentFen", intent.getStringExtra("fenMes"));
					editChessBoardIntent.putExtra("gridViewSize", gridViewSize);
					editChessBoardIntent.putExtra("fieldSize", getChessFieldSize());
					startActivityForResult(editChessBoardIntent, EDIT_CHESSBOARD_REQUEST_CODE);
					return true;
				}
			}
		}
		// call from another Activity, passing the PGN(File)
		if (intent.getData() != null)
		{
			if (intent.getType() == null)
			{
				Toast.makeText(this, "This MIME type is not supported.", Toast.LENGTH_LONG).show();
				return false;
			}
			String intentData = intent.getDataString();
//Log.i(TAG, "intentTyp: " + intent.getType() + ", intentData: " + intentData);
			if (intent.getType().endsWith("x-chess-pgn") & intentData.endsWith(".pgn"))	//".pgn-db" canceled
			{
				pgnIO = new PgnIO(this);
				String target = fmPrefs.getString("fm_extern_load_path", "");
				File fUri = new File(intentData);
				String fName = fUri.getName();
				String fPath = fUri.getParent();
				if (fPath.startsWith("file:"))
					fPath = fPath.replace("file:", "");
				if (!fPath.endsWith("/"))
					fPath = fPath + "/";
				if (pgnIO.fileExists(fPath, fName))
				{
					isOk = true;
					SharedPreferences.Editor ed = fmPrefs.edit();
					ed.putString("fm_extern_load_path", fPath);
					ed.putString("fm_extern_load_file", fName);
					ed.commit();
					startFileManager(LOAD_GAME_REQUEST_CODE, 1, 0);
				}
				else
				{
					if (pgnIO.copyFile(intentData, target))
					{
						isOk = true;
						SharedPreferences.Editor ed = fmPrefs.edit();
						ed.putString("fm_extern_load_file", fName);
						ed.commit();
						startFileManager(LOAD_GAME_REQUEST_CODE, 1, 0);
					}
				}
			}
		}
		return isOk;
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
					}
				}
				return;
		}
	}

//	SUBACTIVITYS		SUBACTIVITYS		SUBACTIVITYS		SUBACTIVITYS
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
//    	Log.i(TAG, "fileActionCode, displayActivity, gameLoad: " 	+ fileActionCode + ", " + displayActivity
//    																+ ", " + gameLoad);
		if ((fileActionCode == LOAD_GAME_REQUEST_CODE | fileActionCode == LOAD_GAME_PREVIOUS_CODE)
				& displayActivity == 0)
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
			startActivityForResult(fileManagerIntent, fileActionCode);		// start PgnFileManager - Activity(with GUI)
		}
		else
		{
			switch (fileActionCode) 										// Load | Save | Delete
			{
				case 1: 														// Load
					startActivityForResult(fileManagerIntent, fileActionCode);	// start PgnFileManager - Activity(no GUI)
					break;
				case 2: 														// Save
					startSaveFile(gc.cl.history.createPgnFromHistory(1));		// saveFile(using class: PgnIO)
					break;
				case 7: 														// Save(old game), Load(new game)
				case 71: 														// Save(old game, MateAnalysis OK), Load(new game)
				case 72: 														// Save(old game, MateAnalysis ERROR), Load(new game)
				default:
					startActivityForResult(fileManagerIntent, fileActionCode);	// start PgnFileManager - Activity(no GUI)
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
		pgnIO = new PgnIO(this);
		if (!pgnIO.canWrite(path, file))
			return;
		if (pgnIO.pathExists(path))
		{
			if (!file.equals(""))
			{
				if (pgnIO.fileExists(path, file))
					pgnIO.dataToFile(path, file, data.toString(), true);
				else
					pgnIO.dataToFile(path, file, data.toString(), false);
			}
		}
		startEngineAutoplay();
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
		notationIntent.putExtra("moves", gc.cl.history.createGameNotationFromHistory(600, false, true, true, false, false, true, 0));
		notationIntent.putExtra("moves_text", gc.cl.history.createGameNotationFromHistory(600, true, true, true, false, false, true, 2));
		notationIntent.putExtra("pgn", gc.cl.history.createPgnFromHistory(1));
		notationIntent.putExtra("white", gc.cl.history.getGameTagValue("White"));
		gc.cl.history.getGameTagValue("FEN");
		notationIntent.putExtra("black", gc.cl.history.getGameTagValue("Black"));
		startActivityForResult(notationIntent, NOTATION_REQUEST_CODE);
	}

	public void startSaveGame(int displayActivity)
	{
		startFileManager(SAVE_GAME_REQUEST_CODE, displayActivity, 0);
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
		gc.cl.newPositionFromMove(gc.fen, gc.promotionMove);
		gc.promotionMove = "";
		if (gc.cl.p_stat.equals("1"))
		{
			if (!ec.chessEnginePaused)
			{
				ec.chessEngineSearching = true;
				ec.chessEnginePaused = false;
				ec.lastChessEnginePaused = false;
				updateGui();
				stopThreads(false);
				startPlay(false, true);
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

	@Override
    protected Dialog onCreateDialog(int id)
	{
		activDialog = id;
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
//					showHtml("manual", R.string.menu_about_userManual);
					showHtml(R.raw.manual, R.string.menu_about_userManual);
				}
			});
			builder.setPositiveButton(R.string.btn_Continue, null);
			AlertDialog alert = builder.create();
			alert.setCancelable(true);
			return alert;
		}
		if (id == PGN_ERROR_DIALOG)
		{
			String message = gc.errorMessage + "\n\n" + getString(R.string.sendEmail);
			c4aImageDialog = new C4aImageDialog(this, this, getString(R.string.dgTitleDialog), message,
					R.drawable.button_ok, 0, R.drawable.button_cancel);
			c4aImageDialog.setOnCancelListener(new DialogInterface.OnCancelListener()
			{
				public void onCancel(DialogInterface dialog) { onCancelDialog(); }
			});
			return c4aImageDialog;
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
							stopThreads(false);
							startPlay(false, true);
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
			promotionDialog = new ChessPromotion(this, new OnPromotionListener());
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
		if (id == ENGINE_SEARCH_DIALOG)
		{
			setInfoMessage(getString(R.string.engineNoEnginesOnDevice), null, null);
			c4aImageDialog = new C4aImageDialog(this, this, getString(R.string.dgTitleDialog), getString(R.string.engineSearchMessage),
					R.drawable.button_ok, 0, R.drawable.button_cancel);
			c4aImageDialog.setOnCancelListener(new DialogInterface.OnCancelListener()
			{
				public void onCancel(DialogInterface dialog) { onCancelDialog(); }
			});
			return c4aImageDialog;
		}
		if (id == ENGINE_SEARCH_NO_INTERNET_DIALOG)
		{
			c4aImageDialog = new C4aImageDialog(this, this, getString(R.string.dgTitleDialog), getString(R.string.engineSearchNoInternetMessage),
					0, R.drawable.button_ok, 0);
			c4aImageDialog.setOnCancelListener(new DialogInterface.OnCancelListener()
			{
				public void onCancel(DialogInterface dialog) { onCancelDialog(); }
			});
			return c4aImageDialog;
		}
		if (id == INFO_DIALOG)
		{
			String mes = runP.getString("infoMessage", "") + "\n\n";
			mes = mes + "Model: " + runP.getString("infoModelNumber", "") + "\n";
			mes = mes + "Android-Version: " + runP.getString("infoAndroidVersion", "") + "\n";
			mes = mes + "DB-Version: " + runP.getString("infoDbVersion", "");
			c4aImageDialog = new C4aImageDialog(this, this, runP.getString("infoTitle", ""), mes, 0, R.drawable.button_ok, 0);
			c4aImageDialog.setOnCancelListener(new DialogInterface.OnCancelListener()
			{
				public void onCancel(DialogInterface dialog) { onCancelDialog(); }
			});
			return c4aImageDialog;
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
//Log.i(TAG, "GAME_ID_DIALOG, gameId: " + input.getText());
					int gameId = Integer.parseInt(input.getText().toString());
					queryControl = "i";
					startFileManager(LOAD_GAME_REQUEST_CODE, 0, gameId);
				}
			});

			AlertDialog alert = builder.create();
			alert.setCancelable(true);
			return alert;
		}

		if (id == MENU_BOARD_DIALOG)
		{
			final int MENU_EDIT_BOARD     	= 0;
			final int MENU_CLIPBOARD    	= 1;
//			final int MENU_COLOR_SETTINGS  	= 2;
			final int MENU_COORDINATES      = 3;
			final int MENU_FLIP_BOARD    	= 4;
			final int MENU_FILE  			= 5;
			ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.drawer_list_item);
			List<Integer> actions = new ArrayList<Integer>();
			arrayAdapter.add(getString(R.string.menu_edit_board));     				actions.add(MENU_EDIT_BOARD);
			arrayAdapter.add(getString(R.string.menu_board_clipboard)); 						actions.add(MENU_CLIPBOARD);
//			arrayAdapter.add(getString(R.string.menu_board_color_settings)); 				actions.add(MENU_COLOR_SETTINGS);
			arrayAdapter.add(getString(R.string.menu_board_coordinates)); 					actions.add(MENU_COORDINATES);
			arrayAdapter.add(getString(R.string.menu_board_flip)); 	actions.add(MENU_FLIP_BOARD);
			arrayAdapter.add(getString(R.string.fmLblFile)); 	actions.add(MENU_FILE);
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
							editChessBoardIntent.putExtra("currentFen", gc.fen);
							editChessBoardIntent.putExtra("gridViewSize", gridViewSize);
							editChessBoardIntent.putExtra("fieldSize", getChessFieldSize());
							startActivityForResult(editChessBoardIntent, EDIT_CHESSBOARD_REQUEST_CODE);
							break;
						case MENU_CLIPBOARD:
							removeDialog(MENU_CLIPBOARD_DIALOG);
							showDialog(MENU_CLIPBOARD_DIALOG);
							break;
//						case MENU_COLOR_SETTINGS:
//							// different color design for th board
//							break;
						case MENU_COORDINATES:
							SharedPreferences.Editor ed = userPrefs.edit();
							if (userPrefs.getBoolean("user_options_gui_Coordinates", false))
								ed.putBoolean("user_options_gui_Coordinates", false);
							else
								ed.putBoolean("user_options_gui_Coordinates", true);
							ed.commit();
							updateGui();
							break;
						case MENU_FLIP_BOARD:
							SharedPreferences.Editor ed2 = userPrefs.edit();
							if (userPrefs.getBoolean("user_options_gui_FlipBoard", false))
								ed2.putBoolean("user_options_gui_FlipBoard", false);
							else
								ed2.putBoolean("user_options_gui_FlipBoard", true);
							ed2.commit();
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

		if (id == MENU_EDIT_DIALOG)
		{
			final int MENU_EDIT_BOARD     		= 0;
			final int MENU_EDIT_PGN  			= 1;
			final int MENU_EDIT_NOTIFICATION    = 2;
			final int MENU_EDIT_NAG      		= 3;
			final int MENU_EDIT_NOTATION    	= 4;
			final int MENU_EDIT_TURN_BOARD    	= 5;
			ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.drawer_list_item);
			List<Integer> actions = new ArrayList<Integer>();
			arrayAdapter.add(getString(R.string.menu_edit_board));     			actions.add(MENU_EDIT_BOARD);
			arrayAdapter.add(getString(R.string.menu_pgn_edit)); 				actions.add(MENU_EDIT_PGN);
			arrayAdapter.add(getString(R.string.menu_info_moveNotification));   	actions.add(MENU_EDIT_NOTIFICATION);
			arrayAdapter.add(getString(R.string.menu_info_nag));     			actions.add(MENU_EDIT_NAG);
			arrayAdapter.add(getString(R.string.menu_info_moveNotation)); 		actions.add(MENU_EDIT_NOTATION);
			arrayAdapter.add(getString(R.string.menu_info_turnBoard)); 			actions.add(MENU_EDIT_TURN_BOARD);
			final List<Integer> finalActions = actions;
			AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
			builder.setCancelable(true);
			TextView tv = new TextView(getApplicationContext());
			tv.setText(R.string.menu_modes_edit);
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
							editChessBoardIntent.putExtra("currentFen", gc.fen);
							editChessBoardIntent.putExtra("gridViewSize", gridViewSize);
							editChessBoardIntent.putExtra("fieldSize", getChessFieldSize());
							startActivityForResult(editChessBoardIntent, EDIT_CHESSBOARD_REQUEST_CODE);
							break;
						case MENU_EDIT_PGN:
							startGameData();
							break;
						case MENU_EDIT_NOTIFICATION:
							startMoveText();
							break;
						case MENU_EDIT_NAG:
							c4aShowDialog(NAG_DIALOG);
							break;
						case MENU_EDIT_NOTATION:
							startNotation(3);
							break;
						case MENU_EDIT_TURN_BOARD:
							startTurnBoard();
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
			arrayAdapter.add(getString(R.string.menu_pgn_load));     			actions.add(MENU_PGN_LOAD);
			arrayAdapter.add(getString(R.string.menu_pgn_save)); 				actions.add(MENU_PGN_SAVE);
			arrayAdapter.add(getString(R.string.menu_load_www));     			actions.add(MENU_PGN_DOWNLOAD);
			arrayAdapter.add(getString(R.string.menu_info_clipboardCopyPgn)); 	actions.add(MENU_PGN_CB_COPY);
			arrayAdapter.add(getString(R.string.menu_info_clipboardCopyFen)); 	actions.add(MENU_PGN_CB_COPY_POS);
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
							stopTimeHandler(false);
							startFileManager(LOAD_GAME_REQUEST_CODE, 1, 0);
							break;
						case MENU_PGN_SAVE:
							startSaveGame(1);
							break;
						case MENU_PGN_DOWNLOAD:
							stopTimeHandler(false);
							startPgnDownload();
							break;
						case MENU_PGN_CB_COPY:
							setToClipboard(gc.cl.history.createPgnFromHistory(1));
							break;
						case MENU_PGN_CB_COPY_POS:
							setToClipboard(gc.cl.history.getMoveFen(gc.cl.history.getMoveIdx()));
							break;
						case MENU_PGN_CB_PAST:
							stopTimeHandler(false);
							getFromClipboard();
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
			arrayAdapter.add(getString(R.string.menu_info_clipboardPaste)); 		actions.add(MENU_CLIPBOARD_PAST);
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
							stopTimeHandler(false);
							getFromClipboard();
							break;
					}
				}
			});
			AlertDialog alert = builder.create();
			return alert;
		}

		if (id == MENU_ENGINES_DIALOG)
		{
			final int MENU_ENGINE_SETTINGS 		= 0;
			final int MENU_ENGINE_AUTOPLAY 		= 1;
			final int MENU_ENGINE_SHUTDOWN 		= 2;
			ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.drawer_list_item);
			List<Integer> actions = new ArrayList<Integer>();
			arrayAdapter.add(getString(R.string.menu_enginesettings_playOptions));     		actions.add(MENU_ENGINE_SETTINGS);
			arrayAdapter.add(getString(R.string.menu_specialities_engine_autoplay)); 		actions.add(MENU_ENGINE_AUTOPLAY);
			arrayAdapter.add(getString(R.string.menu_enginesettings_shutdown));   			actions.add(MENU_ENGINE_SHUTDOWN);
			final List<Integer> finalActions = actions;
			AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
			builder.setCancelable(true);
			TextView tv = new TextView(getApplicationContext());
			tv.setText(R.string.menu_enginesettings);
			tv.setTextAppearance(this, R.style.c4aDialogTitle);
			tv.setGravity(Gravity.CENTER_HORIZONTAL);
			builder.setCustomTitle(tv );
			builder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int item)
				{
					switch (finalActions.get(item))
					{
						case MENU_ENGINE_SETTINGS:
							startActivityForResult(optionsEnginePlayIntent, OPTIONS_ENGINE_PLAY_REQUEST_CODE);
							break;
						case MENU_ENGINE_AUTOPLAY:
							startActivityForResult(playEngineIntent, OPTIONS_ENGINE_AUTO_PLAY_REQUEST_CODE);
							break;
						case MENU_ENGINE_SHUTDOWN:
							stopAllEnginesAndInit();
							break;
					}
				}
			});
			AlertDialog alert = builder.create();
			return alert;
		}

		if (id == MENU_PAUSE_CONTINUE)
		{
			final int MENU_PAUSE_PLAYOPTIONS 		= 0;
			final int MENU_PAUSE_MODE_NEW_GAME 		= 1;
			final int MENU_PAUSE_MODE_SET_CLOCK 	= 2;
			final int MENU_PAUSE_MODE_CONTINUE 		= 3;
			final int MENU_PAUSE_ANALYSIS 			= 9;
			ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.drawer_list_item);
			List<Integer> actions = new ArrayList<Integer>();
			arrayAdapter.add(getString(R.string.app_optionsPlay));     		actions.add(MENU_PAUSE_PLAYOPTIONS);
			if (pause_mode == 0)
				pause_mode = userPrefs.getInt("user_play_playMod", 1);
			if (pause_mode != 4)
			{
				String modeText;
				String actionNewGame = getString(R.string.play_newGame);
				String actionSetClock = getString(R.string.play_set_clock);
				String actionContinue = getString(R.string.play_continue);
				switch (pause_mode)
				{
					case 1:     // white
						modeText = getString(R.string.play_white);
						arrayAdapter.add(modeText + ", " + actionNewGame);	actions.add(MENU_PAUSE_MODE_NEW_GAME);
						arrayAdapter.add(modeText + ", " + actionSetClock);	actions.add(MENU_PAUSE_MODE_SET_CLOCK);
						arrayAdapter.add(modeText + ", " + actionContinue);	actions.add(MENU_PAUSE_MODE_CONTINUE);
						break;
					case 2:     // black
						modeText = getString(R.string.play_black);
						arrayAdapter.add(modeText + ", " + actionNewGame);	actions.add(MENU_PAUSE_MODE_NEW_GAME);
						arrayAdapter.add(modeText + ", " + actionSetClock);	actions.add(MENU_PAUSE_MODE_SET_CLOCK);
						arrayAdapter.add(modeText + ", " + actionContinue);	actions.add(MENU_PAUSE_MODE_CONTINUE);
						break;
					case 3:     // engine vs engine
						modeText = getString(R.string.play_engine);
						arrayAdapter.add(modeText + ", " + actionNewGame);	actions.add(MENU_PAUSE_MODE_NEW_GAME);
						arrayAdapter.add(modeText + ", " + actionSetClock);	actions.add(MENU_PAUSE_MODE_SET_CLOCK);
						arrayAdapter.add(modeText + ", " + actionContinue);	actions.add(MENU_PAUSE_MODE_CONTINUE);
						break;
					case 5:     // two players
						modeText = getString(R.string.play_two_players_flip);
						arrayAdapter.add(modeText + ", " + actionNewGame);	actions.add(MENU_PAUSE_MODE_NEW_GAME);
						arrayAdapter.add(modeText + ", " + actionSetClock);	actions.add(MENU_PAUSE_MODE_SET_CLOCK);
						arrayAdapter.add(modeText + ", " + actionContinue);	actions.add(MENU_PAUSE_MODE_CONTINUE);
						break;
					case 6:     // edit (two players)
						modeText = getString(R.string.play_two_players);
						arrayAdapter.add(modeText + ", " + actionNewGame);	actions.add(MENU_PAUSE_MODE_NEW_GAME);
						arrayAdapter.add(modeText + ", " + actionContinue);	actions.add(MENU_PAUSE_MODE_CONTINUE);
						break;
				}
//Log.i(TAG, "MENU_PAUSE_CONTINUE, pause_mode: " + pause_mode + ", modeTextId: " + modeTextId);
			}
			arrayAdapter.add(getString(R.string.play_analysis));	actions.add(MENU_PAUSE_ANALYSIS);
			final List<Integer> finalActions = actions;
			AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
			builder.setCancelable(true);
			TextView tv = new TextView(getApplicationContext());
			tv.setText(R.string.menu_play_options);
			tv.setTextAppearance(this, R.style.c4aDialogTitle);
			tv.setGravity(Gravity.CENTER_HORIZONTAL);
			builder.setCustomTitle(tv );
			builder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int item)
				{
					switch (finalActions.get(item))
					{
						case MENU_PAUSE_PLAYOPTIONS:
							startActivityForResult(optionsPlayIntent, OPTIONS_PLAY_REQUEST_CODE);
							break;
						case MENU_PAUSE_MODE_NEW_GAME:
							twoPlayerPaused = false;
							setPlayModPrefs(pause_mode);
							switch (pause_mode)
							{
								case 1:     // white
								case 2:     // black
								case 3:     // engine vs engine
									ec.chessEnginePaused = false;
									ec.chessEngineInit = false;
									startPlay(true, true);
									break;
								case 5:     // two players
									analysisMessage = "";
									startEdit(true, true);
									break;
								case 6:     // edit
									analysisMessage = "";
									startEdit(true, false);
									break;
							}
							break;
						case MENU_PAUSE_MODE_SET_CLOCK:
							twoPlayerPaused = false;
							setPlayModPrefs(pause_mode);
							switch (pause_mode)
							{
								case 1:     // white
								case 2:     // black
								case 3:     // engine vs engine
									ec.chessEnginePaused = false;
									ec.chessEngineInit = false;
									startPlay(false, true);
									break;
								case 5:     // two players
									analysisMessage = "";
									startEdit(false, true);
									break;
								case 6:     // edit
									analysisMessage = "";
									startEdit(false, false);
									break;
							}
							break;
						case MENU_PAUSE_MODE_CONTINUE:
							twoPlayerPaused = false;
							setPlayModPrefs(pause_mode);
							switch (pause_mode)
							{
								case 1:     // white
								case 2:     // black
								case 3:     // engine vs engine
									ec.chessEnginePaused = false;
									ec.chessEngineInit = false;
									getRunPrefsTime();
									startPlay(false, false);
									break;
								case 5:     // two players
									analysisMessage = "";
									getRunPrefsTime();
									startEdit(false, false);
									break;
								case 6:     // edit
									analysisMessage = "";
									startEdit(false, false);
									break;
							}
							break;
						case MENU_PAUSE_ANALYSIS:
							twoPlayerPaused = false;
							setPlayModPrefs(4);
							ec.chessEnginePaused = false;
							ec.chessEngineInit = false;
							ec.initClockAfterAnalysis = true;
							updateCurrentPosition("");
							startPlay(false, true);
							break;
					}
				}
			});
			AlertDialog alert = builder.create();
			return alert;
		}

		if (id == MENU_SETTINGS_DIALOG)
		{
			final int MENU_SETTINGS_GUI     		= 0;
			final int MENU_SETTINGS_COMPUTER		= 1;
			final int MENU_SETTINGS_TIME_CONTROL    = 2;
			final int MENU_SETTINGS_TIMER_AUTOPLAY	= 3;
			final int MENU_SETTINGS_TIME_WHITE 		= 4;
			final int MENU_SETTINGS_TIME_BLACK 		= 5;
			ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.drawer_list_item);
			List<Integer> actions = new ArrayList<Integer>();
			arrayAdapter.add(getString(R.string.menu_usersettings_gui));     									actions.add(MENU_SETTINGS_GUI);
			arrayAdapter.add(getString(R.string.menu_enginesettings_playOptions)); 	actions.add(MENU_SETTINGS_COMPUTER);
			arrayAdapter.add(getString(R.string.menu_usersettings_timeControl));   									actions.add(MENU_SETTINGS_TIME_CONTROL);
			arrayAdapter.add(getString(R.string.menu_usersettings_timerAutoPlay));     								actions.add(MENU_SETTINGS_TIMER_AUTOPLAY);
			if (ec.chessEnginePlayMod < 4)
			{
				arrayAdapter.add(getString(R.string.menu_usersettings_time_white)); 									actions.add(MENU_SETTINGS_TIME_WHITE);
				arrayAdapter.add(getString(R.string.menu_usersettings_time_black)); 									actions.add(MENU_SETTINGS_TIME_BLACK);
			}
			final List<Integer> finalActions = actions;
			AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_HOLO_DARK);
			builder.setCancelable(true);
			TextView tv = new TextView(getApplicationContext());
			tv.setText(R.string.menu_usersettings);
			tv.setTextAppearance(this, R.style.c4aDialogTitle);
			tv.setGravity(Gravity.CENTER_HORIZONTAL);
			builder.setCustomTitle(tv );
			builder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int item)
				{
					switch (finalActions.get(item))
					{
						case MENU_SETTINGS_GUI:
							startActivityForResult(optionsGuiIntent, OPTIONS_GUI_REQUEST_CODE);
							break;
						case MENU_SETTINGS_COMPUTER:
							startActivityForResult(optionsEnginePlayIntent, OPTIONS_ENGINE_PLAY_REQUEST_CODE);
							break;
						case MENU_SETTINGS_TIME_CONTROL:
							startActivityForResult(optionsTimeControlIntent, OPTIONS_TIME_CONTROL_REQUEST_CODE);
							break;
						case MENU_SETTINGS_TIMER_AUTOPLAY:
							chessClockMessage = getString(R.string.ccsMessageAutoPlay);
							chessClockControl = 41;
							chessClockTimeGame = -1;
							chessClockTimeBonus = userPrefs.getInt("user_options_timer_autoPlay", 1500);
							c4aShowDialog(TIME_SETTINGS_DIALOG);
							break;
						case MENU_SETTINGS_TIME_WHITE:
							chessClockMessage = getString(R.string.ccsMessageWhite);
							chessClockControl = 1;
							chessClockTimeGame = tc.timeWhite;
							chessClockTimeBonusSaveWhite = tc.bonusWhite;
							chessClockTimeBonus = -1;
							c4aShowDialog(TIME_SETTINGS_DIALOG);
							break;
						case MENU_SETTINGS_TIME_BLACK:
							chessClockMessage = getString(R.string.ccsMessageBlack);
							chessClockControl = 2;
							chessClockTimeGame = tc.timeBlack;
							chessClockTimeBonusSaveBlack = tc.bonusBlack;
							chessClockTimeBonus = -1;
							c4aShowDialog(TIME_SETTINGS_DIALOG);
							break;
					}
				}
			});
			AlertDialog alert = builder.create();
			return alert;
		}

		if (id == MENU_ABOUT_DIALOG)
		{
			final int MENU_ABOUT_NEW     		= 0;
			final int MENU_ABOUT_APPS	    	= 4;
			final int MENU_ABOUT_WEBSITE    	= 5;
			final int MENU_ABOUT_SOURCECODE    	= 6;
			final int MENU_ABOUT_CONTACT    	= 7;
			ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.drawer_list_item);
			List<Integer> actions = new ArrayList<Integer>();
			arrayAdapter.add(getString(R.string.menu_about_new));     				actions.add(MENU_ABOUT_NEW);
			arrayAdapter.add(getString(R.string.menu_about_apps)); 					actions.add(MENU_ABOUT_APPS);
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
//							showHtml("whats_new", R.string.whatsNew);
							showHtml(R.raw.whats_new, R.string.menu_about_userManual);
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
							Intent send = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", APP_EMAIL.toString(), null));
							send.putExtra(android.content.Intent.EXTRA_SUBJECT, "");
							send.putExtra(android.content.Intent.EXTRA_TEXT, "");
							startActivity(Intent.createChooser(send, getString(R.string.sendEmail)));
							break;
					}
				}
			});
			AlertDialog alert = builder.create();
			return alert;
		}

		return null;

	}

	public void onCancelDialog()
	{

	}

	@Override
	public void getCallbackValue(int btnValue)
	{
//Log.i(TAG, "getCallbackValue(), ec.chessEnginePaused: " + ec.chessEnginePaused + ", ec.lastChessEnginePaused: " + ec.lastChessEnginePaused);
		if (activDialog == PGN_ERROR_DIALOG)
		{
			if (btnValue == 1)
			{
				Intent send = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", APP_EMAIL.toString(), null));
				send.putExtra(android.content.Intent.EXTRA_SUBJECT, gc.errorMessage);
				send.putExtra(android.content.Intent.EXTRA_TEXT, gc.errorPGN);
				startActivity(Intent.createChooser(send, getString(R.string.sendEmail)));
			}
		}
		if (activDialog == TIME_SETTINGS_DIALOG)
		{	// chessClockControl
			if (btnValue == 2)
			{
				SharedPreferences.Editor ed = userPrefs.edit();
				tc.timeControl = chessClockControl;
				switch (chessClockControl)
				{
					case 1: 	// set current time: white
						tc.timeWhite = timeSettingsDialog.getTime();
						tc.bonusWhite = chessClockTimeBonusSaveWhite;
						break;
					case 2: 	// set current time: black
						tc.timeBlack = timeSettingsDialog.getTime();
						tc.bonusBlack = chessClockTimeBonusSaveBlack;
						break;
					case 41: 	// timer auto play
						ed.putInt("user_options_timer_autoPlay", timeSettingsDialog.getBonus());
						break;
				}
				ed.commit();
				if (ec.chessEnginePaused)
				{
					tc.setCurrentShowValues();
					updateCurrentPosition("");
				}
			}
		}
		if (activDialog == ENGINE_SEARCH_DIALOG)
		{
			if (btnValue == 1)
			{
				ec.chessEngineInit = true;
				try
				{
					Intent i = new Intent(Intent.ACTION_VIEW);
					i.setData(Uri.parse("market://search?q=ccc.chess.engine OR ccc.chess.engines"));
					startActivityForResult(i, ENGINE_SEARCH_REQUEST_CODE);
				}
				catch (ActivityNotFoundException e) {e.printStackTrace(); c4aShowDialog(ENGINE_SEARCH_NO_INTERNET_DIALOG);}
			}
		}
	}

//	USER-ACTIONS		USER-ACTIONS		USER-ACTIONS		USER-ACTIONS
//	TOUCH, CLICK		TOUCH, CLICK		TOUCH, CLICK		TOUCH, CLICK

//errANR >>> at ccc.chess.gui.chessforall.MainActivity.dispatchTouchEvent (MainActivity.java:1668)
//	@Override
//	public boolean dispatchTouchEvent(MotionEvent event)
//	{
//		// scrlMsgEngine: trigger a touch action
//		if (event.getAction() == MotionEvent.ACTION_DOWN)
//			touchTime = System.currentTimeMillis();
//		if (event.getAction() == MotionEvent.ACTION_UP)
//		{
//			upRawX = event.getRawX();
//			upRawY = event.getRawY();
//			long diffTime = System.currentTimeMillis() - touchTime;
//			boolean isScrlMsgEngine = u.isViewInBounds(scrlMsgEngine, (int) upRawX, (int) upRawY);
////Log.i(TAG, "dispatchTouchEvent(), upRawX: " + upRawX + ", upRawY: "+  upRawY + ", isScrlMsgEngine: "+  isScrlMsgEngine);
//			if (isScrlMsgEngine & diffTime < longTouchTime & !(leftDrawer.isShown() | rightDrawer.isShown()))
//				startActivityForResult(optionsEnginePlayIntent, OPTIONS_ENGINE_PLAY_REQUEST_CODE);
//		}
//		return super.dispatchTouchEvent(event);
//	};

	@Override
	public boolean onTouch(View view, MotionEvent event)
	{
//Log.i(TAG, "onTouch(), view.getId(), event.getAction(): " + view.getId() + ", " + event.getAction() + ", x: " + event.getRawX() + ", y: " + event.getRawY());
		if (view.getId() != R.id.boardView)
		{
			gc.cl.p_hasPossibleMovesTo = false;
			gc.cl.p_possibleMoveToList.clear();
		}
		if (event.getAction() == MotionEvent.ACTION_DOWN)
		{
//Log.i(TAG, "onTouch(), MotionEvent.ACTION_DOWN, id: " + view.getId());
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
			lastTouchID = 0;

			upRawX = event.getRawX();
			upRawY = event.getRawY();
//Log.i(TAG, "0 onTouch(), minScrollingWidth: " + minScrollingWidth + ", downRawX: " + downRawX + ", upRawX: " + upRawX);
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
//Log.i(TAG, "8 onTouch(), upRawX: " + upRawX + ", leftBtnBorder: " + leftBtnBorder + ", rightBtnBorder: " + rightBtnBorder);
//Log.i(TAG, "9 onTouch(), upRawY: " + upRawY + ", topBtnBorder: " + topBtnBorder + ", bottomBtnBorder: " + bottomBtnBorder);
				if (view.getId() == R.id.boardView & userPrefs.getBoolean("user_options_gui_gameNavigationBoard", true))
				{
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
						stopThreads(false);
						startPlay(false, true);
					}
				}
//Log.i(TAG, "1 ??? onTouch(), view.getId(): " + view.getId() + ", downViewId: " + downViewId + ", isDownBtn: " + isDownBtn);
				if (isDownBtn)
				{
//Log.i(TAG, "2 ??? onTouch(), minScrollingWidth: " + minScrollingWidth + ", downRawX: " + downRawX + ", upRawX: " + upRawX);
					if ((aspectRatio > 150 & upRawX > downRawX) | (aspectRatio <= 150 & upRawY > downRawY))			// game(file) control
					{
						if (view.getId() == R.id.btn_1 & u.isViewInBounds(btn_7, (int) upRawX, (int) upRawY))
						{
							if (fmPrefs.getInt("fm_location", 1) == 1)
							{
								stopThreads(false);
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
								stopThreads(false);
								startFileManager(LOAD_GAME_REQUEST_CODE, 0, 9);      // last
							}
							else
							{
								if (!gc.pgnStat.equals("L"))
								{
									stopThreads(false);
									startFileManager(LOAD_GAME_REQUEST_CODE, 0, 0);  // next
								}
							}
						}
					}
					else
					{
						if (view.getId() == R.id.btn_7 & u.isViewInBounds(btn_1, (int) upRawX, (int) upRawY))
						{
							stopThreads(false);
							startFileManager(LOAD_GAME_REQUEST_CODE, 0, 7);			// random
						}
						else
						{
							if ((aspectRatio > 150 & upRawX <= leftBtnBorder) | (aspectRatio <= 150 & upRawY <= topBtnBorder))
							{
								stopThreads(false);
								startFileManager(LOAD_GAME_REQUEST_CODE, 0, 1);       // first
							} else
							{
								if (!gc.pgnStat.equals("F"))
								{
									stopThreads(false);
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
//err ANR: at ccc.chess.gui.chessforall.MainActivity.dispatchTouchEvent (MainActivity.java:1668)
//err ANR: at ccc.chess.gui.chessforall.MainActivity.onTouch (MainActivity.java:1814)
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
		if (view.getId() == R.id.btn_1 | view.getId() == R.id.btn_2 | view.getId() == R.id.btn_3 | view.getId() == R.id.btn_4)
		{
			if (gc.isAutoPlay)
			{
				stopAutoPlay(false);
				return;
			}
		}
		switch (view.getId())
		{
			case R.id.boardView:    // make a move on board
				startBoardMoveAction(event);
				break;

			case R.id.btn_1:    // play mode - new game
				startActivityForResult(optionsPlayIntent, OPTIONS_PLAY_REQUEST_CODE);
				break;
			case R.id.btn_2:    // pause game | start analysis
				gc.isGameLoaded = false;
				if (ec.chessEnginePlayMod <= 3 | (ec.chessEnginePlayMod == 5 & !twoPlayerPaused))
					setRunPrefsTime();
//Log.i(TAG, "1 onTouchAction(), btn_2, ec.chessEnginePaused: " + ec.chessEnginePaused + ", ec.chessEngineSearching: " + ec.chessEngineSearching);
				if (!ec.chessEnginePaused & ec.chessEngineSearching)
				{
					if (pause_mode == 6)
					{
						stopThreads(false);
						setPlayModPrefs(pause_mode);
						ec.chessEngineSearching = false;
						setInfoMessage(getString(R.string.menu_modes_edit), null, null);
					}
					else
					{
						setPauseValues(false, gc.fen, ec.chessEnginePlayMod, messageEngine);
						stopThreads(false);
						setInfoMessage(getString(R.string.engine_paused), null, null);
					}
					ec.chessEngineAnalysisStat = 0;
				}
				else
				{
					if (getPauseValues(false, gc.fen, ec.chessEnginePlayMod) & ec.chessEnginePlayMod == 4)
					{
						setPlayModPrefs(4);
						ec.initClockAfterAnalysis = true;
						ec.chessEnginePaused = false;
						ec.chessEngineInit = false;
						updateCurrentPosition("");
						startPlay(false, true);
					}
					else
					{
						if (ec.chessEnginePlayMod == 6 & ec.chessEnginePaused)		// edit
						{
//Log.i(TAG, "onTouchAction(), ec.chessEnginePaused: " + ec.chessEnginePaused + ", ec.chessEngineSearching: " + ec.chessEngineSearching);
							pause_mode = ec.chessEnginePlayMod;		// switch: edit ---> analysis
							setPlayModPrefs(4);
							ec.initClockAfterAnalysis = true;
							ec.chessEnginePaused = false;
							ec.chessEngineInit = false;
							updateCurrentPosition("");
							startPlay(false, true);
						}
						else
						{
							if ((ec.chessEnginePlayMod == 1 | ec.chessEnginePlayMod == 2 | ec.chessEnginePlayMod == 5) & tc.clockIsRunning)
							{
								tc.stopChessClock(System.currentTimeMillis());
								if (ec.chessEnginePlayMod == 5)
								{
									twoPlayerPaused = true;
									setInfoMessage(getString(R.string.play_two_players_flip) + " (" + getString(R.string.clock_stopped) + ")", null, null);
								}
								else
								{
									ec.chessEnginePaused = true;
									setPauseValues(false, gc.fen, ec.chessEnginePlayMod, messageEngine);
									setInfoMessage(getString(R.string.clock_stopped) + ", " + getString(R.string.engine_paused), null, null);
								}
							}
							else
							{
								removeDialog(MENU_PAUSE_CONTINUE);
								showDialog(MENU_PAUSE_CONTINUE);
							}
						}
					}
				}
				break;
			case R.id.btn_3:    // force computer move and continue searching
				if (ec.chessEnginePaused)
					startActivityForResult(optionsPlayIntent, OPTIONS_PLAY_REQUEST_CODE);
				else
					startForceComputerMove();
				break;
			case R.id.btn_4:    // delete move(s)
				deleteMoves(true);
				if (ec.chessEnginePaused)
					setInfoMessage(getString(R.string.engine_paused), "", null);
				break;
			case R.id.btn_5:    // turn board
				startTurnBoard();
				break;
			case R.id.btn_6:    // move back
				nextMove(1, 0);
				if (!ec.chessEnginePaused)
				{
					stopThreads(false);
					startPlay(false, true);
				}
				break;
			case R.id.btn_7:    // next move
				nextMove(2, 0);
				if (!ec.chessEnginePaused)
				{
					stopThreads(false);
					startPlay(false, true);
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
//			case R.id.msgEngine:
//				startActivityForResult(optionsEnginePlayIntent, OPTIONS_ENGINE_PLAY_REQUEST_CODE);
//				break;
		}
	}

	private void onLongTouchAction(View view)
	{
		switch (view.getId())
		{
			case R.id.boardView:    // board menu
				removeDialog(MENU_BOARD_DIALOG);
				showDialog(MENU_BOARD_DIALOG);
				break;

			case R.id.btn_1:    // play options (???)
				removeDialog(MENU_SETTINGS_DIALOG);
				showDialog(MENU_SETTINGS_DIALOG);
				break;
			case R.id.btn_2:	// computer settings
				startActivityForResult(optionsEnginePlayIntent, OPTIONS_ENGINE_PLAY_REQUEST_CODE);
				break;
			case R.id.btn_3:    // auto play
				startStopAutoPlay();
				break;
			case R.id.btn_4:    //

				break;
			case R.id.btn_5:    //

				break;
			case R.id.btn_6:    // first move (initial position)
				gc.isGameOver = false;
				nextMove(3, 0);
				if (!ec.chessEnginePaused)
				{
					stopThreads(false);
					startPlay(false, true);
				}
				break;
			case R.id.btn_7:    // last move
				if (startVariation())
				c4aShowDialog(VARIATION_DIALOG);
			else
			{
//Log.i(TAG, "onLongTouchAction(), user_play_playMod, btn_7");
				nextMove(4, 0);
				if (!ec.chessEnginePaused)
				{
					stopThreads(false);
					startPlay(false, true);
				}
			}
				break;

			case R.id.lblPlayerTimeA:    // time control
			case R.id.lblPlayerTimeB:
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
//    	Log.i(TAG, "onActivityResult, user_play_playMod: " + requestCode + ", " + userPrefs.getInt("user_play_playMod", 1));
//    	Log.i(TAG, "ec.chessEnginePaused, ec.lastChessEnginePaused: " + ec.chessEnginePaused + ", " + ec.lastChessEnginePaused);
		updateCurrentPosition("");
		SharedPreferences.Editor ed = userPrefs.edit();
		boolean continuePausedGame = true;
		boolean isNewGame = false;
		if (data != null)
			isNewGame = data.getBooleanExtra("newGame", false);
		else
		{
			if (requestCode == OPTIONS_PLAY_REQUEST_CODE)
				isNewGame = false;
			else
			{
				if (requestCode != ENGINE_SEARCH_REQUEST_CODE)
					return;
			}
		}
		if (resultCode > 0 | requestCode == ENGINE_SEARCH_REQUEST_CODE)
			stopThreads(false);
		switch(requestCode)
		{
			case OPTIONS_PLAY_REQUEST_CODE:
			case ENGINE_SEARCH_REQUEST_CODE:
			case OPTIONS_ENGINE_PLAY_REQUEST_CODE:
				if (resultCode == 3 | requestCode == ENGINE_SEARCH_REQUEST_CODE | requestCode == OPTIONS_ENGINE_PLAY_REQUEST_CODE)
				{	// set playOption and play
					gc.isGameLoaded = false;
					stopThreads(false);
					ec.chessEngineAutoRun = false;
					twoPlayerPaused = false;
					if (requestCode == OPTIONS_ENGINE_PLAY_REQUEST_CODE)
						ec.getEngine().isLogOn = userPrefs.getBoolean("user_options_enginePlay_logOn", false);
					ec.chessEnginePlayMod = userPrefs.getInt("user_play_playMod", 1);
//Log.i(TAG, "onActivityResult(), ec.chessEnginePlayMod: " + ec.chessEnginePlayMod);
					if (ec.chessEnginePlayMod == 4)
						ec.initClockAfterAnalysis = true;
					switch (userPrefs.getInt("user_play_playMod", 1))
					{
						case 1:     // white
						case 2:     // black
						case 3:     // engine vs engine
							continuePausedGame = false;
							if (requestCode == OPTIONS_PLAY_REQUEST_CODE | requestCode == OPTIONS_ENGINE_PLAY_REQUEST_CODE)
							{
								ec.chessEnginePaused = false;
								ec.chessEngineInit = false;
							}
							if (requestCode == ENGINE_SEARCH_REQUEST_CODE)
								ec.chessEngineInit = true;
							startPlay(isNewGame, true);
							break;
						case 4:     // analysis
							ec.chessEnginePaused = false;
							ec.chessEngineInit = false;
							updateCurrentPosition("");
							startPlay(isNewGame, true);
							break;
						case 5:     // two players
							analysisMessage = "";
							startEdit(isNewGame, true);
							break;
						case 6:     // edit
							analysisMessage = "";
							startEdit(isNewGame, true);
							break;
					}
				}
				break;
			case OPTIONS_ENGINE_AUTO_PLAY_REQUEST_CODE:
				if (resultCode == MainActivity.RESULT_OK)
				{
					gc.isGameLoaded = false;
					stopTimeHandler(false);
					ec.chessEngineAutoRun = true;
					ec.chessEnginePaused = false;
					ec.lastChessEnginePaused = false;
					setPlayModPrefs(3);
					cntResult = 0;
					initChessClock();
					continuePausedGame = false;
					gc.startPgn = gc.cl.history.createPgnFromHistory(1);
					gc.startMoveIdx = gc.cl.history.getMoveIdx();
//Log.i(TAG, "gc.startMoveIdx: " + gc.startMoveIdx);
//Log.i(TAG, "gc.startPgn: \n" + gc.startPgn);
					if 	(!userPrefs.getBoolean("user_play_eve_autoCurrentGame", false))
						startPlay(true, true);
					else
					{
						setRunMoveHistory();
						startC4aService316();	// current date
						startPlay(false, true);
					}
				}
				break;
			case LOAD_GAME_REQUEST_CODE:
			case LOAD_GAME_PREVIOUS_CODE:
			case SAVE_LOAD_GAME_REQUEST_CODE:
			case SAVE_OK_LOAD_GAME_REQUEST_CODE:
			case SAVE_ERROR_LOAD_GAME_REQUEST_CODE:
				initChessClock();
				gc.isAutoLoad = false;
				gc.isPlayerPlayer = false;
				gc.pgnStat = "-";
				if (resultCode == MainActivity.RESULT_OK)
				{
					gc.isGameOver = false;
					gc.isGameUpdated = true;
					ec.chessEngineAutoRun = false;
					setPauseValues(false, "", 4, "");
					if (requestCode == LOAD_GAME_REQUEST_CODE | requestCode == LOAD_GAME_PREVIOUS_CODE)
					{
						messageInfo 		= "";
						messageEngine 		= "";
						messageEngineShort  = "";
					}
					gc.pgnStat = data.getStringExtra("pgnStat");
					if (requestCode == SAVE_OK_LOAD_GAME_REQUEST_CODE
							& userPrefs.getBoolean("user_batch_ma_counterOn", true))
					{
						ed.putInt("user_batch_ma_gameCounter", userPrefs.getInt("user_batch_ma_gameCounter", 1) +1);
						ed.commit();
					}
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
					if (requestCode == LOAD_GAME_REQUEST_CODE | requestCode == LOAD_GAME_PREVIOUS_CODE)
					{
						ec.chessEnginePaused = true;
						gc.isGameLoaded = true;
						setInfoMessage("", null, null);
					}
					else
					{
						ec.chessEnginePaused = false;
						startPlay(isNewGame, true);
					}
				}
				break;
			case GAME_DATA_REQUEST_CODE:
				if (resultCode == MainActivity.RESULT_OK)
					gc.cl.history.setNewGameTags(data.getCharSequenceExtra("gameTags").toString());
				updateCurrentPosition("");
				break;
			case NOTATION_REQUEST_CODE:
				updateCurrentPosition("");
				break;
			case OPTIONS_CHESSBOARD_REQUEST_CODE:
			case OPTIONS_GUI_REQUEST_CODE:
				if (requestCode == OPTIONS_GUI_REQUEST_CODE)
				{
					useWakeLock = userPrefs.getBoolean("user_options_gui_disableScreenTimeout", false);
					setWakeLock(useWakeLock);
				}
				u.updateFullscreenStatus(this, userPrefs.getBoolean("user_options_gui_StatusBar", true));
				updateCurrentPosition("");
				break;
			case OPTIONS_TIME_CONTROL_REQUEST_CODE:
				if (resultCode == 101)	// apply
				{
					ec.chessEnginePaused = false;
					ec.lastChessEnginePaused = false;
					ec.chessEngineInit = false;
					initChessClock();
					startPlay(false, true);
				}
				break;
			case EDIT_CHESSBOARD_REQUEST_CODE:
				if (resultCode == MainActivity.RESULT_OK)
				{
					gc.isGameLoaded = false;
					stopTimeHandler(false);
					messageEngine 		= "";
					messageEngineShort  = "";
					gc.errorMessage = "";
					gc.errorPGN = "";
					ec.chessEngineAutoRun = false;
					CharSequence chess960Id = data.getStringExtra("chess960Id");
					CharSequence fen = data.getStringExtra("newFen");
					if (!chess960Id.equals("518"))
						fen = "";
//Log.i(TAG, "chess960Id, fenMes: " + chess960Id + ", " + fenMes);
					gc.cl.newPosition(chess960Id, fen, "", "", "", "", "", "");
					if (gc.cl.p_stat.equals("1"))
					{
						gc.isGameOver = false;
						gc.isGameUpdated = true;
						gc.fen = gc.cl.p_fen;
						if (gc.cl.p_chess960ID == 518)
							gc.isChess960 = false;
						else
							gc.isChess960 = true;
						ed.putInt("user_game_chess960Id", gc.cl.p_chess960ID);
						ed.commit();
						setInfoMessage("", "", "");
						updateGui();
						if (ec.chessEnginePlayMod == 3 | ec.chessEnginePlayMod == 4)
							startPlay(false, true);
					}
				}
				break;
			case MOVETEXT_REQUEST_CODE:
				if (resultCode == MainActivity.RESULT_OK)
				{
					gc.isGameUpdated = false;
					gc.cl.history.setMoveText(data.getStringExtra("text"));
					updateCurrentPosition("");
				}
				break;
			case SAVE_GAME_REQUEST_CODE:
				if (resultCode == MainActivity.RESULT_OK)
				{
					gc.isGameUpdated = true;
					gc.fileBase = data.getStringExtra("fileBase");
					gc.filePath = data.getStringExtra("filePath");
					gc.fileName = data.getStringExtra("fileName");
				}
				if (resultCode == 22) 					// chessEngineAutoPlay | writeNewFile
					startEngineAutoplay();
				else
					updateCurrentPosition("");
				break;
			case CHESS960_POSITION_REQUEST_CODE:
				if (resultCode == MainActivity.RESULT_OK)
				{
					setChess960IdPref(chess960.createChessPosition(data.getStringExtra("chess960BaseLine")));
				}
				break;
			case ENGINE_SETTING_REQUEST_CODE:
				ec.chessEnginePaused = true;
				updateCurrentPosition("");
				setInfoMessage(getEnginePausedMessage(), null, null);
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
		if (Build.VERSION.SDK_INT >= 23)
		{
			String[] permissions = new String[]
					{Manifest.permission.INTERNET, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.WAKE_LOCK};
			ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST_CODE);
		}

	}

	//	PREFERENCES		PREFERENCES		PREFERENCES		PREFERENCES		PREFERENCES
	public void setRunPrefs()
	{
//    	Log.i(TAG, "setRunPrefs()");
    	SharedPreferences.Editor ed = runP.edit();
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
		ed.putBoolean("run_lastChessEnginePaused", ec.lastChessEnginePaused);
		ed.putBoolean("run_chessEngineSearching", ec.chessEngineSearching);
		ed.putBoolean("run_chessEngineAutoRun", ec.chessEngineAutoRun);
		ed.putBoolean("run_twoPlayerPaused", twoPlayerPaused);
		ed.putString("run_selectedVariationTitle", (String) gc.selectedVariationTitle);

		ed.commit();

		setRunPrefsTime();

	}

	public void setRunPrefsTime()
	{
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
//Log.i(TAG, "setRunMoveHistory()");
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
        ec.lastChessEnginePaused = runP.getBoolean("run_lastChessEnginePaused", false);
        ec.chessEngineSearching = runP.getBoolean("run_chessEngineSearching", false);
        ec.chessEngineAutoRun = runP.getBoolean("run_chessEngineAutoRun", false);
		twoPlayerPaused = runP.getBoolean("run_twoPlayerPaused", false);
		ec.chessEnginePlayMod = userPrefs.getInt("user_play_playMod", 1);
		if (ec.chessEnginePlayMod == 4)
			ec.initClockAfterAnalysis = true;
        gc.selectedVariationTitle = runP.getString("run_selectedVariationTitle", "");
		getRunPrefsTime();
	}

	public void getRunPrefsTime()
	{
		tc.timeControl = runP.getInt("run_timeControl", 1);
		tc.timeWhite = runP.getInt("run_timeWhite", 300000);
		tc.timeBlack = runP.getInt("run_timeBlack", 300000);
		tc.movesToGo = runP.getInt("run_movesToGo", 0);
		tc.bonusWhite = runP.getInt("run_bonusWhite", 0);
		tc.bonusBlack = runP.getInt("run_bonusBlack", 0);
		tc.initChessClock(tc.timeControl, tc.timeWhite, tc.timeBlack, tc.movesToGo, tc.bonusWhite, tc.bonusBlack);
		tc.setCurrentShowValues();
	}

	public void startEngineAutoplay()
	{
		SharedPreferences.Editor ed = userPrefs.edit();
		int gameCounter = userPrefs.getInt("user_play_eve_gameCounter", 1);
		gameCounter++;
		ed.putInt("user_play_eve_gameCounter", gameCounter);
		ed.commit();
		ec.chessEngineRound = userPrefs.getInt("user_play_eve_round", 1)
				+ "." + userPrefs.getInt("user_play_eve_gameCounter", 1);
		cntChess960 = 0;
		nextGameEngineAutoPlay();
	}

	public void setMoveTime()
	{
//		Log.i(TAG, "setMoveTime(), tc.timeControl: " + tc.timeControl);
		if (tc.timeControl == 2)
		{
			if (ec.chessEnginePlayMod == 1)
			{
				tc.timeWhite = userPrefs.getInt("user_time_player_move", 1000);
				tc.timeBlack = userPrefs.getInt("user_time_engine_move", 1000);
			}
			if (ec.chessEnginePlayMod == 2)
			{
				tc.timeWhite = userPrefs.getInt("user_time_engine_move", 1000);
				tc.timeBlack = userPrefs.getInt("user_time_player_move", 1000);
			}
			if (ec.chessEnginePlayMod == 3)
			{
				tc.timeWhite = userPrefs.getInt("user_time_engine_move", 1000);
				tc.timeBlack = userPrefs.getInt("user_time_engine_move", 1000);
			}
		}
	}

	public void setPauseEnginePlay(boolean shutDown)
	{
//		Log.i(TAG, "setPauseEnginePlay");
		if (shutDown)
			ec.stopAllEngines();
		else
		{
			ec.stopComputerThinking(false);
			ec.lastChessEnginePaused = ec.chessEnginePaused;
			ec.chessEnginePaused = true;
			if (!gc.isGameOver & !gc.cl.p_variationEnd)
				setInfoMessage(getEnginePausedMessage(), null, null);
		}
	}

	public void pauseEnginePlay(int engineAction)
	{	// start/stop engine(button)
//Log.i(TAG, "pauseEnginePlay, ec.chessEnginePaused: " + ec.chessEnginePaused + ", engineAction: " + engineAction);
		if (ec.chessEnginePaused)
		{
			startChessClock();
			ec.chessEnginePaused = false;
			ec.chessEngineAnalysisStat = 0;
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
				chessEngineBestMove(gc.cl.p_fen, "");
		}
		else
		{
			if (ec.chessEnginePlayMod <= 4)
			{
				switch (engineAction)
				{
					case 0:		// stop engine
						ec.chessEngineAnalysisStat = 0;
						break;
					case 1:		// make best move and stop engine (analysis)
						ec.chessEngineAnalysisStat = 1;
						break;
					case 2:		// make best move and continue engine search (analysis)
						ec.chessEngineAnalysisStat = 2;
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
//Log.i(TAG, "pauseEnginePlay, ec.chessEnginePaused, engineAction: " + ec.chessEnginePaused + ", " + engineAction);
		ec.lastChessEnginePaused = ec.chessEnginePaused;
	}

	public void nextGameEngineAutoPlay()
	{
		gc.isGameOver = false;
		gc.isGameUpdated = true;
		ec.chessEnginePaused = false;
		ec.lastChessEnginePaused = false;

		int chess960Id = userPrefs.getInt("user_game_chess960Id", 518);
		if (chess960Id != 518)
		{
			if (ec.twoEngines)
			{
				if (cntChess960 == 0)
					cntChess960++;
				else
				{
					cntChess960 = 0;
				}
			}
		}
		ec.setPlaySettings(userPrefs);
		if 	(!userPrefs.getBoolean("user_play_eve_autoCurrentGame", false))
		{
			getGameData(gc.fileBase, gc.filePath, gc.fileName, "", false, false, gc.startMoveIdx, true);
			startPlay(true, true);
		}
		else
		{
			gc.isGameOver = false;
			gc.cl.history.setGameTag("Result", "*");
			gc.cl.moveHistoryPrefs = moveHistoryP.getString("run_moveHistory", "");
			getGameData(gc.fileBase, gc.filePath, gc.fileName, gc.startPgn, true, false, gc.startMoveIdx, true);
			startPlay(false, true);
		}
	}

	public void getGameData(CharSequence fileBase, CharSequence filePath, CharSequence fileName,
							CharSequence startPgn, boolean withMoveHistory, boolean isEndPos, int moveIdx, boolean isUpdateGui)
	{
//Log.i(TAG, "getGameData(), startPgn: \n" + startPgn);
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
				gc.errorPGN = ">>>PGN-PARSE-DATA<<< \n" + gc.cl.history.createGameNotationFromHistory(600, false, true, true, false, false, true, 2) + "\n\n"
						+ ">>>PGN-INPUT-DATA<<< \n" + startPgn + "\n";
				updateGui();
				if (!gc.cl.p_stat.equals("4"))
					c4aShowDialog(PGN_ERROR_DIALOG);
			}
		}
		catch (NullPointerException e) {e.printStackTrace();}
	}

	public void setChess960IdPref(int chess960Id)
	{	// if isLastGame set old game preferences
		SharedPreferences.Editor ed = userPrefs.edit();
		ed.putInt("user_game_chess960Id", chess960Id);
		ed.commit();
	}

	public void setPlayModPrefs(int playMod)
	{
		ec.chessEnginePlayMod = playMod;
		SharedPreferences.Editor ed = userPrefs.edit();
		ed.putInt("user_play_playMod", playMod);
		ed.commit();
	}

	public void nextMove(int moveDirection, int moveIdx)
	{	// next move(History) and updateGui
//Log.i(TAG, "nextMove, moveDirection, moveIdx: " + moveDirection + ", " + moveIdx);
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
			if (moveDirection == 12)	gc.cl.getPositionFromMoveHistory(12, 0);			// two moves back(engine play)
			if (moveDirection == 19)	gc.cl.getPositionFromMoveHistory(19, moveIdx);		// position from moveIdx
//Log.i(TAG, "gc.cl.p_stat, gc.isAutoPlay, gc.cl.p_gameOver, gc.cl.p_gameEnd: " + gc.cl.p_stat + ", " + gc.isAutoPlay + ", " + gc.cl.p_gameOver + ", " + gc.cl.p_gameEnd);
//Log.i(TAG, "nextMove(), moveDirection: " + moveDirection + ", moveIdx: " + moveIdx + ", gc.cl.p_stat: " + gc.cl.p_stat);
			if (gc.cl.p_stat.equals("1"))
				updateGui();
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
		gc.cl.deleteMovesFromMoveHistory(deleteMoveIdx);

		if (gc.cl.p_stat.equals("1"))
		{
			gc.move = "";
			gc.isGameOver = false;
			updateGui();
		}

		if (!ec.chessEnginePaused)
		{
			stopThreads(false);
			startPlay(false, true);
		}

	}

	public void startEdit(boolean isNewGame, boolean setClock)
	{
//Log.i(TAG, "startEdit, isNewGame: " + isNewGame);
		ec.chessEngineSearching = false;
		gc.isGameOver = false;
		gc.isGameUpdated = true;
		gc.isPlayerPlayer = false;
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
				initChessClock();
			if (!twoPlayerPaused)
				startChessClock();
		}
	}

	public void startPlay(boolean isNewGame, boolean setClock)
	{
//Log.i(TAG, "1 startPlay, isNewGame: " + isNewGame + ", chessEngineSearching: " + ec.chessEngineSearching);
		ec.chessEngineSearching = false;
		ec.chessEnginePaused = false;

		messageEngine 		= "";
		messageEngineShort  = "";
		messageNotification = "";
		gc.cl.pos = new ChessPosition(gc.cl.history.chess960Id);
		gc.cl.posPV = new ChessPosition(gc.cl.history.chess960Id);
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
		if (isNewGame | (!gc.cl.p_mate & !gc.cl.p_stalemate))	// !mate, steal mate?
		{
			gc.isGameOver = false;
			gc.isGameUpdated = true;
			ec.chessEnginePlayMod = userPrefs.getInt("user_play_playMod", 1);
			if (ec.chessEnginePlayMod == 2)
				gc.isBoardTurn = true;
			ec.setPlaySettings(userPrefs);
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
			initInfoArrays(false);
//Log.i(TAG, "2 startPlay, isNewGame: " + isNewGame + ", mod: " + ec.chessEnginePlayMod + ", ec.initClockAfterAnalysis: " + ec.initClockAfterAnalysis);
			if (!isAppStart)
			{
				ec.initClockAfterAnalysis = false;
				// setClock option ?
				if (isNewGame | setClock)
					initChessClock();
			}
			if (ec.chessEnginePlayMod <= 5)
				startChessClock();
			if (ec.chessEnginePlayMod <= 4)
				startEnginePlay();
			else
				setInfoMessage(getEnginePausedMessage(), null, null);
		}
		else
			setInfoMessage(getGameOverMessage(), null, null);
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
			lblPlayerEloA.setText("");
			lblPlayerEloB.setText("");
			gc.cl.p_color = "w";
			gc.cl.p_message = "";
			if (!ec.chessEngineAutoRun)
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

	public void startEnginePlay()
	{	//setting play options and start engine play
		ec.setPlaySettings(userPrefs);
Log.i(TAG, "startEnginePlay(), gc.fen: " + gc.fen);
		ec.setStartPlay(gc.getValueFromFen(gc.fen, 2));
		cancelSearchTask();
		if (!ec.getEngine().isReady)
		{
			initEngineStartTime = System.currentTimeMillis();
			startInitEngine = true;
			startInitEnginePlay = true;
			handlerInitEngine.postDelayed(mUpdateInitEngine, 50);
		}
		else
			startEnginePlayIsReady();
		engineMes = "";
		engineStat = "";
		initInfoArrays(false);
	}

	public void startEnginePlayIsReady()
	{
		startNewGame(ec.getEngine().engineNumber);
		ec.chessEnginePaused = false;
		ec.chessEngineInit = false;
		ec.setPlayData(userPrefs);
		startC4aService319();
		if (ec.makeMove)
		{
//Log.i(TAG, "startEnginePlayIsReady(), ec.makeMove");
			ec.chessEngineSearching = true;
			if (!ec.chessEngineAutoRun)
				setInfoMessage(getEngineThinkingMessage(), null, null);
			isGoPonder = false;
			if (!gc.cl.p_fen.equals(""))
				chessEngineBestMove(gc.cl.p_fen, "");
		}
		else
		{
			ec.chessEngineProblem = true;
//Log.i(TAG, "startEnginePlayIsReady(), player_move");
			startChessClock();
			messageEngineShort  = "";
			setInfoMessage(getString(R.string.player_move), null, null);
			ec.chessEngineSearching = false;
		}
	}

	public void startNewGame(int engineNumber)
	{
		switch (engineNumber)
		{
			case 1:	// default engine Stockfish
				ec.getEngine().setUciMultiPV(userPrefs.getInt("user_options_enginePlay_MultiPv", 4));
				ec.getEngine().setIsChess960(gc.isChess960);
				ec.getEngine().setUciStrength(userPrefs.getInt("user_options_enginePlay_strength", 100));
				ec.getEngine().setUciContempt(userPrefs.getInt("user_options_enginePlay_aggressiveness", 0));
				ec.getEngine().setUciHash(16);
				ec.getEngine().setUciPonder(isPonder);
				ec.getEngine().setStartFen(gc.startFen);
				ec.getEngine().newGame();
				break;
			case 2:

				break;
		}
	}

	public void chessEngineBestMove(CharSequence fen, CharSequence moves)
	{
		if (!ec.chessEnginePaused & !fen.equals(""))
		{
			if (ec.twoEngines)
			{
				if (gc.getValueFromFen(fen, 2).equals("b"))
					ec.engineNumber = 2;
				else
					ec.engineNumber = 1;
			}



Log.i(TAG, "chessEngineBestMove(), fen, w/b, engine: " + fen + ",   moves: " + moves);
Log.i(TAG, "isGoPonder: " + isGoPonder + ", ec.chessEngineSearchingPonder: " + ec.chessEngineSearchingPonder);
Log.i(TAG, "ec.ponderUserFen: " + ec.ponderUserFen + ", ec.ponderUserMove: " + ec.ponderUserMove);
			if (!isGoPonder & ec.chessEngineSearchingPonder)
			{
				ec.ponderUserMove = gc.move;
				ec.ponderUserFen = fen;
			}
			else
			{
				if (!isGoPonder)
					cancelSearchTask();
				searchTaskFen = fen;
				searchTaskMoves = moves;
				chessEngineSearchTask = new ChessEngineSearchTask();
				chessEngineSearchTask.execute(fen, moves);    //>249 starts the chess engine search task
			}
		}
	}

	public void cancelSearchTask()
	{
		if (chessEngineSearchTask != null)
		{
Log.i(TAG, "cancelSearchTask(), ec.chessEngineSearchingPonder: " + ec.chessEngineSearchingPonder);
			chessEngineSearchTask.cancel(true);
		}
	}

	public CharSequence getEngineThinkingMessage()
	{
		if (ec.chessEnginePlayMod == 5)	// player vs player
		{
			messageEngine = "";
			messageEngineShort  = "";
			if (!twoPlayerPaused)
				messageInfo = getString(R.string.play_two_players_flip);
			else
				messageInfo = getString(R.string.play_two_players_flip) + " (" + getString(R.string.clock_stopped) + ")";
			return messageInfo;
		}
		if (ec.chessEnginePlayMod == 6)	// edit
		{
			messageEngine = "";
			messageEngineShort  = "";
			messageInfo = getString(R.string.menu_modes_edit);
			return messageInfo;
		}
		if (ec.chessEnginePlayMod != 4)
		{
			if (!ec.chessEnginePaused)
				return ec.getEngine().engineName + " " + ec.getEngine().engineNameStrength;
			else
				return getString(R.string.engine_paused);
		}
		else
		{
			if (ec.chessEngineAnalysisStat == 9)
			{
				CharSequence analizeEngineName = ec.en_1.engineName;
				if (analizeEngineName.toString().endsWith("%)") & analizeEngineName.toString().contains("("))
				{
					int startChar = analizeEngineName.toString().indexOf("(") -1;
					analizeEngineName = analizeEngineName.toString().subSequence(0, startChar);
				}
				return getString(R.string.engineAnalysisSearch) + "  " + analizeEngineName;
			}
			else
				return messageInfo;
		}
	}

	public CharSequence getEnginePausedMessage()
	{
//Log.i(TAG, "getEnginePausedMessage(), ec.chessEnginePlayMod: " + ec.chessEnginePlayMod);
		if (ec.chessEnginePlayMod == 5)	// player vs player
		{
			messageEngine = "";
			messageEngineShort  = "";
			if (!twoPlayerPaused)
				messageInfo = getString(R.string.play_two_players_flip);
			else
				messageInfo = getString(R.string.play_two_players_flip) + " (" + getString(R.string.clock_stopped) + ")";
			return messageInfo;
		}
		if (ec.chessEnginePlayMod == 6)	// edit
		{
			messageEngine = "";
			messageEngineShort  = "";
			messageInfo = getString(R.string.menu_modes_edit);
			return messageInfo;
		}
		if (ec.chessEnginePlayMod != 4)
		{
			if (ec.chessEnginePlayMod == 1 | ec.chessEnginePlayMod == 2)
				return getString(R.string.clock_stopped) + ", " + getString(R.string.engine_paused);
			else
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

	public void initChessClock()
	{
		int timeWhite = 300000;
		int timeBlack = 300000;
		int movesToGo = 0;
		int bonusWhite = 0;
		int bonusBlack = 0;
		int timeControl = userPrefs.getInt("user_options_timeControl", 1);
		switch (ec.chessEnginePlayMod)
		{
			case 1:
				switch (timeControl)
				{
					case 1:
						timeWhite = userPrefs.getInt("user_time_player_clock", 300000);
						timeBlack = userPrefs.getInt("user_time_engine_clock", 300000);
						bonusWhite = userPrefs.getInt("user_bonus_player_clock", 3000);
						bonusBlack = userPrefs.getInt("user_bonus_engine_clock", 3000);
						break;
					case 2:
						timeWhite = userPrefs.getInt("user_time_player_move", 40000);
						timeBlack = userPrefs.getInt("user_time_engine_move", 5000);
						break;
					case 3:
						timeWhite = userPrefs.getInt("user_time_player_sand", 300000);
						timeBlack = userPrefs.getInt("user_time_engine_sand", 10000);
						break;
					case 4:
						timeWhite = 0;
						timeBlack = 0;
						break;
				}
				break;
			case 2:
				switch (timeControl)
				{
					case 1:
						timeWhite = userPrefs.getInt("user_time_engine_clock", 300000);
						timeBlack = userPrefs.getInt("user_time_player_clock", 300000);
						bonusWhite = userPrefs.getInt("user_bonus_engine_clock", 3000);
						bonusBlack = userPrefs.getInt("user_bonus_player_clock", 3000);
						break;
					case 2:
						timeWhite = userPrefs.getInt("user_time_engine_move", 5000);
						timeBlack = userPrefs.getInt("user_time_player_move", 40000);
						break;
					case 3:
						timeWhite = userPrefs.getInt("user_time_engine_sand", 10000);
						timeBlack = userPrefs.getInt("user_time_player_sand", 300000);
						break;
					case 4:
						timeWhite = 0;
						timeBlack = 0;
						break;
				}
				break;
			case 3:
				switch (timeControl)
				{
					case 1:
						timeWhite = userPrefs.getInt("user_time_engine_clock", 300000);
						timeBlack = userPrefs.getInt("user_time_engine_clock", 300000);
						bonusWhite = userPrefs.getInt("user_bonus_engine_clock", 3000);
						bonusBlack = userPrefs.getInt("user_bonus_engine_clock", 3000);
						break;
					case 2:
						timeWhite = userPrefs.getInt("user_time_engine_move", 5000);
						timeBlack = userPrefs.getInt("user_time_engine_move", 5000);
						break;
					case 3:
						timeWhite = userPrefs.getInt("user_time_engine_sand", 10000);
						timeBlack = userPrefs.getInt("user_time_engine_sand", 10000);
						break;
					case 4:
						timeWhite = 0;
						timeBlack = 0;
						break;
				}
				break;
			case 4:
				if (timeControl != 4)
					timeControl = 11;
				timeWhite = 0;
				timeBlack = 0;
				bonusWhite = 0;
				bonusBlack = 0;
				break;
			case 5:
				switch (timeControl)
				{
					case 1:
						timeWhite = userPrefs.getInt("user_time_player_clock", 300000);
						timeBlack = userPrefs.getInt("user_time_player_clock", 300000);
						bonusWhite = userPrefs.getInt("user_bonus_player_clock", 2000);
						bonusBlack = userPrefs.getInt("user_bonus_player_clock", 2000);
						break;
					case 2:
						timeWhite = userPrefs.getInt("user_time_player_move", 40000);
						timeBlack = userPrefs.getInt("user_time_player_move", 40000);
						break;
					case 3:
						timeWhite = userPrefs.getInt("user_time_player_sand", 300000);
						timeBlack = userPrefs.getInt("user_time_player_sand", 300000);
						break;
					case 4:
						timeWhite = 0;
						timeBlack = 0;
						break;
				}
				break;
		}
//Log.i(TAG, "initChessClock(), mod: " + ec.chessEnginePlayMod + ", timeControl: " + timeControl);
//Log.i(TAG, "initChessClock(), tw: " + timeWhite + ", tb: " + timeBlack + ", movesToGo: " + movesToGo + ", bw: " + bonusWhite + ", bb: " + bonusBlack);
		tc.initChessClock(timeControl, timeWhite, timeBlack, movesToGo, bonusWhite, bonusBlack);
	}

	public void stopChessClock()
	{
//Log.i(TAG, "stopChessClock()");
		handlerChessClock.removeCallbacks(mUpdateChessClock);
		tc.stopChessClock(System.currentTimeMillis());
	}

	public void startChessClock()
	{
		if (gc.cl.p_color.equals("w"))
			tc.startChessClock(true, System.currentTimeMillis());
		else
			tc.startChessClock(false, System.currentTimeMillis());

		handlerChessClock.removeCallbacks(mUpdateChessClock);
		handlerChessClock.postDelayed(mUpdateChessClock, 100);
	}

	public void c4aShowDialog(int dialogId)
	{
		removeDialog(dialogId);
		showDialog(dialogId);
	}

	public void showHtml(int resId, int resTitleId)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

//		String file = "file:///android_res/raw/" + htmlFileName;
//		String title = getString(resTitleId);
//		WebView wv = new WebView(this);
//		builder.setView(wv);
//		wv.loadUrl(file);

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
//		wv.loadData(prompt,"text/html","utf-8");
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

	public void getFromClipboard()
	{
		CharSequence fen = "";
		CharSequence pgnData = "";
		try
		{
			Toast.makeText(this, getString(R.string.menu_info_clipboardPaste), Toast.LENGTH_SHORT).show();
			ClipboardManager cm = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
			pgnData = (String) cm.getText();
		}
		catch (ClassCastException e) {return;}
		if (pgnData == null)
			return;
//Log.i(TAG,"getFromClipboard(), pgnData: \n" + pgnData);
		CharSequence[] pgnSplit = pgnData.toString().split(" ");
//Log.i(TAG,"pgnData, pgnSplit.length: " + pgnData + ", " + pgnSplit.length);
		if (pgnSplit.length > 0)
		{
			if (pgnSplit[0].toString().contains("/"))
			{
				if (pgnSplit.length == 6)
				{
					pgnSplit[4] = "0";
					pgnSplit[5] = "1";
					fen = pgnSplit[0] + " " + pgnSplit[1] + " " + pgnSplit[2] + " " + pgnSplit[3] + " " + pgnSplit[4] + " " + pgnSplit[5];
				}
				else
					fen = pgnData;
			}
		}
		if (fen.equals(""))
		{
			getGameData("", "", "", pgnData, false, getIsEndPosition(), 0, false);
			gc.startFen = gc.cl.history.getStartFen();
		}
		else
		{
			gc.cl.newPositionFromFen(fen);
			gc.startFen = fen;
		}
		if (!gc.cl.p_stat.equals("1"))
			return;
		if (gc.cl.p_chess960ID == 518)
			gc.isChess960 = false;
		else
			gc.isChess960 = true;
		gc.isGameLoaded = false;
		gc.isGameOver = false;
		gc.isGameUpdated = true;
		gc.isPlayerPlayer = false;
		ec.chessEngineAutoRun = false;
		gc.isChess960 = false;
		gc.fen = gc.cl.p_fen;
		setPauseValues(false, "", 4, "");
		SharedPreferences.Editor ed = userPrefs.edit();
		ed.putInt("user_game_chess960Id", 518);

		ed.commit();

		updateGui();

	}

	public void playSound(int idx, int loop)
	{
		if 	(userPrefs.getBoolean("user_options_gui_enableSounds", true))
			mSoundPool.play(soundsMap.get(idx), 0.2f, 0.2f, 1, loop, 1.0f);
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

	public void setInfoMessage(CharSequence info, CharSequence engine, CharSequence moveNotification)
	{
//Log.i(TAG,"setInfoMessage(), gc.cl.p_moveText: " + gc.cl.p_moveText);
//    	Log.i(TAG,"setInfoMessage()info: " + info);
//    	Log.i(TAG,"engine: " + engine);
//    	Log.i(TAG,"engine: " + engine + ", pause_messageEngine:\n" + pause_messageEngine);
//    	Log.i(TAG,"move notification: " + moveNotification);
//    	Log.i(TAG, "infoContent: " + infoContent);
//    	Log.i(TAG, "gc.cl.p_fen  : " + gc.cl.p_fen);
//    	Log.i(TAG, "searchTaskFen: " + searchTaskFen);
//		boolean isPauseValues = getPauseValues(false, gc.fenMes, ec.chessEnginePlayMod);
		if (getPauseValues(false, gc.fen, ec.chessEnginePlayMod))
		{
			if (engine == null)
				engine = pause_messageEngine;
			else
			{
				if (engine.equals(""))
					engine = pause_messageEngine;
			}
		}
		if (engine != null)
		{
			if (engine.toString().endsWith("\n"))
				engine = engine.toString().substring(0, engine.length() - 1);
			if (!engine.equals("") & ec.chessEnginePlayMod <= 4 & userPrefs.getBoolean("user_options_enginePlay_EngineMessage", true))
				messageEngineShort = getInfoShort(engine);
			else
				messageEngineShort = "";
		}
		if (!gc.cl.p_fen.equals(searchTaskFen))
			engine = "";

		if (msgMoves == null | gc.cl.p_stat.equals(""))
			return;

		if (info != null)
		{
			if (gc.isGameLoaded & ec.chessEnginePaused)
			{
				messageInfo = ".../" + gc.fileName + ", " + getGameInfo();
//				gc.isGameLoaded = false;
			}
			else
				messageInfo = info;
		}
		messageEngine = "";
		if (engine != null)
			messageEngine = engine;

		// msgShort
		CharSequence message = "";
		CharSequence messageShort = messageEngineShort;
		if (!messageInfo.equals(""))
		{
//Log.i(TAG,"messageEngineShort: " + messageEngineShort);
		if (!messageEngineShort.equals("") & !messageEngineShort.toString().startsWith("M"))
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

		msgShort.setText(messageShort);
		msgShort2.setText(messageInfo);

		// msgMoves
		if (gc.errorMessage.equals(""))
			msgMoves.setBackgroundResource(R.drawable.borderyellow);
		else
			msgMoves.setBackgroundResource(R.drawable.borderpink);
		msgMoves.setText(gc.cl.history.createGameNotationFromHistory(600, false, true, true, false, false, true, 2));

		if (msgMoves.getText().toString().equals(" *"))
		{
			setPlayModeButton(ec.chessEnginePlayMod, gc.cl.p_color, ec.chessEnginePaused, ec.chessEngineSearching, gc.isBoardTurn);
			return;
		}

		setSpanableToMsgMoves();

		// msgEngine
		if (!messageEngine.equals("") & userPrefs.getBoolean("user_options_enginePlay_EngineMessage", true))
		{
			msgEngine.setVisibility(TextView.VISIBLE);
			msgEngine.setText(messageEngine);
		}
		else
		{
			msgEngine.setVisibility(TextView.GONE);
			msgEngine.setText("");
		}
		gc.cl.p_message = "";
		setPlayModeButton(userPrefs.getInt("user_play_playMod", 1), gc.cl.p_color, ec.chessEnginePaused, ec.chessEngineSearching, gc.isBoardTurn);
	}

	public void setPlayModeButton(int playMode, CharSequence color, boolean isEnginePaused, boolean isEngineSearching, boolean isBoardTurn)
	{	// btn_1
//Log.i(TAG,"setPlayModeButton(), playMode: " +playMode + ", color: " +color + ", isEnginePaused: " +isEnginePaused + ", isEngineSearching: " +isEngineSearching + ", isBoardTurn: " +isBoardTurn);
		Bitmap drawBitmap = null;
		Bitmap imageBackground = null;
		Bitmap imageHuman = BitmapFactory.decodeResource(getResources(), R.drawable.btn_human);
		Bitmap imageComputer = BitmapFactory.decodeResource(getResources(), R.drawable.btn_computer);
		Bitmap imageAnalysis = BitmapFactory.decodeResource(getResources(), R.drawable.btn_analysis);
		Bitmap imageWhite = BitmapFactory.decodeResource(getResources(), R.drawable.btn_white);
		Bitmap imageBlack = BitmapFactory.decodeResource(getResources(), R.drawable.btn_black);
		try
		{
			if (playMode == 5)
			{
				if (twoPlayerPaused)
					imageBackground = BitmapFactory.decodeResource(getResources(), R.drawable.btn_blue);
				else
					imageBackground = BitmapFactory.decodeResource(getResources(), R.drawable.btn_yellow);
			}
			else
			{
				if (isEnginePaused)
				{
					if (playMode == 6)
						imageBackground = BitmapFactory.decodeResource(getResources(), R.drawable.btn_yellow);
					else
						imageBackground = BitmapFactory.decodeResource(getResources(), R.drawable.btn_blue);
				}
				else
				{
					if (playMode == 4)
						imageBackground = BitmapFactory.decodeResource(getResources(), R.drawable.btn_pink);
					else
					{
						if (isEngineSearching)
							imageBackground = BitmapFactory.decodeResource(getResources(), R.drawable.btn_pink);
						else
							imageBackground = BitmapFactory.decodeResource(getResources(), R.drawable.btn_yellow);
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
					sb.setSpan(new BackgroundColorSpan(Color.rgb(120, 175, 100)), infoMoveStartX, infoMoveEndX, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);	// green (move)
				else
					sb.setSpan(new BackgroundColorSpan(Color.rgb(154, 218, 236)), infoMoveStartX, infoMoveEndX, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);	// blue (move comment)
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


			scrlMsgMoves.post(new Runnable()
			{
				//		        	@Override
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
		catch (IndexOutOfBoundsException e) {e.printStackTrace();}
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
//Log.i(TAG, "offset, moves.length(): " + offset + ", " + moves.length());
			if (moves.length() <= 2 | offset < 1)
				return;
			if (moves.charAt(offset) == ' ')
				offset--;
			infoMoveStartX = offset;
//Log.i(TAG, "offset: " + offset + " >" + moves.charAt(offset) + "<");
			try
			{
				boolean isDot = false;
				if (Character.isDigit(moves.charAt(infoMoveStartX)) | moves.charAt(infoMoveStartX) == '.')
				{
					for (int i = infoMoveStartX; i < moves.length(); i++)
					{
//Log.i(TAG, "isDigit(): " + " >" + moves.charAt(i) + "<");
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
//Log.i(TAG, "infoMoveStartX, infoMoveEndX: " + infoMoveStartX + ", " + infoMoveEndX);
			}
			catch (StringIndexOutOfBoundsException e)
			{
				e.printStackTrace();
			}
//Log.i(TAG, "index, infoMoveStartX, infoMoveEndX, move: " + offset + ", " + infoMoveStartX + ", " + infoMoveEndX + ", " + selectedMove);
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
						if (Character.isLetter(moves.charAt(i)))
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
					if (Character.isLetter(moves.charAt(i)))
						moveIdx++;
					if (i >= infoMoveStartX)
						break;
				}
			}
		}
//Log.i(TAG, "getMoveIdxFromInfo, infoMoveStartX, moveIdx: " + infoMoveStartX + ", " + moveIdx);
		return moveIdx;
	}

	protected CharSequence getInfoShort(CharSequence engineMes)
	{
		String infoShort = "";
		if (engineMes.toString().contains("*1("))
		{
			String[] txtSplit = engineMes.toString().split(" ");
			if (txtSplit[0].startsWith("*1("))
			{
				String bestMove = txtSplit[1];
				if (bestMove.contains("..."))
					bestMove = bestMove + txtSplit[2];
				String bestScore = txtSplit[0].replace("*1(", "");
				bestScore = bestScore.replace(")", "");
				infoShort = bestScore + "  " + bestMove;
			}
		}
//Log.i(TAG, "getInfoShort(), engineMes: " + engineMes + "\ninfoShort: " + infoShort);
		return infoShort;
	}

	public CharSequence getGameOverMessage()
	{
		CharSequence mes = getString(R.string.cl_gameOver);
		if (gc.cl.p_mate)
			mes = mes + " (" + getString(R.string.cl_mate) + ")";
		if (gc.cl.p_stalemate)
			mes = mes + " (" + getString(R.string.cl_stealmate) + ")";
		mes = mes + " (" + gc.cl.history.getGameTagValue("Result") + ")";
		return mes;
	}

	public void updateCurrentPosition(CharSequence message)
	{
//Log.i(TAG, "updateCurrentPosition(), message: " + message);
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
				setInfoMessage("", "", "");
		}
		catch (IndexOutOfBoundsException e) {e.printStackTrace();}
		catch (NullPointerException e) 		{e.printStackTrace();}
	}

	public void startForceComputerMove()
	{
		if (!ec.chessEnginePaused)
		{
			switch (ec.chessEnginePlayMod)
			{
				case 1:     // player vs engine
				case 2:     // engine vs player
				case 3:     // computer vs computer
				case 4:     // analysis
					pauseEnginePlay(2);	// move & continue analysis
					break;
				case 6:     // edit
					break;
				default:
					Toast.makeText(this, getString(R.string.engine_paused), Toast.LENGTH_SHORT).show();
					pauseEnginePlay(0);
					break;
			}
		}
		gc.move = "";
	}

	public void startTurnBoard()
	{	// turn board and updateGui
		if (!gc.isBoardTurn)
			gc.isBoardTurn = true;
		else
			gc.isBoardTurn = false;
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

	public void startC4aService315(CharSequence result, CharSequence moveText)
	{	// set game result and updateGui
		gc.cl.history.setGameTag("Result", result.toString());
		gc.cl.history.setMoveText(moveText);
		gc.isGameOver = true;
		gc.isGameUpdated = false;
		updateCurrentPosition("");
	}

	public void startC4aService316()
	{	// set gameDate in History
		gc.cl.history.setGameTag("Date", gc.cl.history.getDateYYYYMMDD().toString());
	}

	public void startC4aService319()
	{	// set engineData to c4aService and updateGui
		gc.cl.history.setGameTag("Event", ec.chessEngineEvent.toString());
		gc.cl.history.setGameTag("Site", ec.chessEngineSite.toString());
		gc.cl.history.setGameTag("Round", ec.chessEngineRound.toString());
		gc.cl.history.setGameTag("White", ec.chessEnginePlayerWhite.toString());
		gc.cl.history.setGameTag("Black", ec.chessEnginePlayerBlack.toString());
		updateCurrentPosition("");
	}

	//	HANDLER, TIMER		HANDLER, TIMER		HANDLER, TIMER		HANDLER, TIMER
	public Runnable mUpdateInitEngine = new Runnable()
	{	// init engine: Handler(Timer)
		public void run()
		{
			if (startInitEngine)
				ec.getEngine().initProcess();
			startInitEngine = false;
			if (ec.getEngine().isReady)
			{
//Log.i(TAG, "handlerInitEngine, engine isReady");
				if (startInitEnginePlay)
					startEnginePlayIsReady();
				handlerInitEngine.removeCallbacks(mUpdateInitEngine);
				return;
			}
			if (System.currentTimeMillis() - initEngineStartTime >= 1500)
			{
//Log.i(TAG, "handlerInitEngine, time out");
				handlerInitEngine.removeCallbacks(mUpdateInitEngine);
				return;
			}
			handlerInitEngine.postDelayed(mUpdateInitEngine, 50);
		}
	};

	public Runnable mUpdateAutoplay = new Runnable()
	{	// AutoPlay: Handler(Timer)
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
	{	// stop handler, threads, asyncTasks
		stopTimeHandler(shutDown);
	}

	public void stopTimeHandler(boolean shutDown)
	{	// stop handler, thread, task and updateGui
		stopChessClock();
		setPauseEnginePlay(shutDown);
		stopAutoPlay(shutDown);
	}

	public void stopAutoPlay(boolean shutDown)
	{	// stop Auto Play and updateGui
		if (gc.isAutoPlay)
		{
			if (!shutDown)
				gc.isAutoPlay = false;
			handlerAutoPlay.removeCallbacks(mUpdateAutoplay);
			setInfoMessage(getString(R.string.ccsMessageAutoPlayStopped), "", "");
		}
	}

	public Runnable mUpdateChessClock = new Runnable()
	{	// ChessClock: timer white/black
		public void run()
		{
//Log.i(TAG, "mUpdateChessClock(), tc.clockIsRunning: " + tc.clockIsRunning);
			if (tc.clockIsRunning)
			{
				if (gc.isGameOver | gc.cl.p_variationEnd)
				{
					tc.stopChessClock(System.currentTimeMillis());
					ec.chessEnginePaused = true;
					ec.chessEngineSearching = false;
					updateCurrentPosition("");
					setInfoMessage(getGameOverMessage(), null, null);
				}
				else
					updateTime(gc.cl.p_color);
			}
		}
	};

	//  ENGINE-SearchTask		ENGINE-SearchTask		ENGINE-SearchTask		ENGINE-SearchTask		ENGINE-SearchTask
	public class ChessEngineSearchTask extends AsyncTask<CharSequence, CharSequence, CharSequence> 	// engine / player - task
	{
		@Override
		protected CharSequence doInBackground(CharSequence... args)
		{
			taskFen = args[0];
			taskMoves = args[1];
Log.i(TAG, "searchTask, doInBackground(), taskFen, taskMoves: " + taskFen + ", " +taskMoves);

			CharSequence firstMove = setRandomFirstMove(taskFen);
			if (!firstMove.equals(""))
				return firstMove;
			ec.chessEnginesOpeningBook = false;
			if 	(		userPrefs.getBoolean("user_options_enginePlay_OpeningBook", true)
					& 	ec.chessEnginePlayMod != 4 & !ec.chessEngineSearchingPonder
				)
			{	// using openingBook
				Move bookMove = null;
				try {bookMove = ec.book.getBookMove(TextIO.readFEN(taskFen.toString()));}
				catch (ChessParseError e1) {e1.printStackTrace();}
				if (bookMove != null)
				{
					ec.chessEnginesOpeningBook = true;
					return bookMove.toString();
				}
			}
			if (!tc.clockIsRunning | ec.chessEnginePlayMod == 4)
			{
				if (ec.chessEnginePlayMod == 4)
					initChessClock();
				startChessClock();
				publishProgress("1", "", "");
			}
			setSearchTime();
			ec.getEngine().statPvAction = "0";
			ec.getEngine().statTime = 0;
			ec.getEngine().statPvBestMove = "";
			ec.getEngine().statPvScore = 0;
			if (ec.getEngine().searchIsReady())
			{
				if (ec.chessEnginePlayMod == 4)
				{
					ec.chessEngineAnalysis = true;
					ec.chessEngineAnalysisStat = 9;
				}
				else
				{
					ec.chessEngineAnalysis = false;
					ec.chessEngineAnalysisStat = 0;
				}
//				ec.getEngine().startSearch(taskFen, taskMoves, wTime, bTime, wInc, bInc, moveTime, movesToGo, ec.chessEngineAnalysis, isDrawOffer, mate);
				ec.getEngine().startSearch(taskFen, taskMoves, wTime, bTime, wInc, bInc, moveTime, movesToGo, ec.chessEngineAnalysis, isGoPonder, mate);
				isGoPonder = false;
			}
			else
			{
//Log.i(TAG, "searchTask, !ec.getEngine().searchIsReady()");
				return "";
			}
			while(true)
			{
//Log.i(TAG, "doInBackground() while(true)");
				if (ec.chessEngineStopSearch & !cancelTask)
				{
//Log.i(TAG, "doInBackground(), isCancelled()");
					if (ec.chessEnginePlayMod != 4)
						ec.chessEngineStopSearch = false;
					cancelTask = true;
				}
				currentBestMove = ec.getEngine().statPvBestMove;		// current best move
				currentPonderMove = ec.getEngine().statPvPonderMove;	// current ponder move
				CharSequence s = ec.getEngine().readLineFromProcess(engineSearchTimeout);
				if (s == null | s.length() == 0)
				{
//Log.i(TAG, "line: " + s);
					s = "";
				}
				CharSequence[] tokens = ec.getEngine().tokenize(s);

				if (tokens[0].equals("info"))
				{
					ec.getEngine().parseInfoCmd(tokens, userPrefs.getInt("user_options_enginePlay_PvMoves", 6));
					engineStat = getInfoStat(ec.getEngine().statCurrDepth, ec.getEngine().statCurrMoveNr, ec.getEngine().statCurrMove,
							ec.getEngine().statTime, ec.getEngine().statNodes, ec.getEngine().statNps);
					if(ec.getEngine().statPvAction.equals("1"))
					{
						if (infoShowPv)
						{
							try
							{
								engineMes = getInfoPv(ec.getEngine().statPvIdx, ec.getEngine().statPvMoves,
										ec.getEngine().statPvScore, ec.getEngine().statIsMate, taskFen);
							}
							catch (NullPointerException e) {e.printStackTrace(); engineMes = "";}
						}
						else
						{
							engineMes = "";
							if (ec.getEngine().statPvIdx == 0)
								bestScore = getBestScore(ec.getEngine().statPvScore, taskFen);
						}
					}
//Log.i(TAG, "ec.getEngine().statPvAction: " + ec.getEngine().statPvAction + ", infoShowPv: " + infoShowPv
//		+ ", engineMes: " + engineMes + ", engineStat: " + engineStat + "\ns: " + s);
				}
				if (!s.equals("") & ec.getEngine().statPvAction.toString().equals("1"))
				{
					prevEngineMes = engineMes;
					prevEngineStat = engineStat;
					publishProgress(ec.getEngine().statPvAction, engineMes, engineStat);
				}

				if (tokens[0].equals("bestmove") & ec.chessEnginePlayMod != 4)
				{	// get best move if not analysis
					handlerChessClock.removeCallbacks(mUpdateChessClock);
					return tokens[1];	//>242	return bestmove
				}
				if (!s.equals(""))
				{
					searchStartTimeInfo = System.currentTimeMillis();
					isTimeOut = false;
				}

				if 	(ec.chessEngineAnalysisStat == 2) // stop, make move and continue
				{	// engine searching has stopped by user
//Log.i(TAG, "analysis has stopped, stat, bestMove: " + ec.chessEngineAnalysisStat + ", " + currentBestMove);
					if (!currentBestMove.equals(""))
					{
						ec.getEngine().writeLineToProcess("stop");
						isNoRespond = false;
						cancelTask = false;
						return currentBestMove;
					}
				}

				// pondering: user did make a move (ec.chessEnginePlayMod == 1 | ec.chessEnginePlayMod == 2)
				if 	(isPonder & ec.chessEngineSearchingPonder & (!ec.ponderUserMove.equals("") | !ec.ponderUserFen.equals(""))) // engine pondering on
				{
Log.i(TAG, "!!! ec.chessEngineSearchingPonder: " + ec.chessEngineSearchingPonder + ", move: " + ec.ponderUserMove + "\nfen" + ec.ponderUserFen);
					taskFen = ec.ponderUserFen;
					taskMoves = "";
					engineMes = "";
					engineStat = "";
					initInfoArrays(true);
					if (ec.ponderUserMove.equals(ponderMove))	// ponderhit (ponder move)
					{
						ec.getEngine().writeLineToProcess("ponderhit");
					}
					else
					{
						if (!ec.ponderUserFen.equals(""))		// set position (not the ponder move)
						{
//							String posStr = "position fen " + ec.ponderUserFen;
//							ec.getEngine().writeLineToProcess(posStr);
Log.i(TAG, "doInBackground(), isGoPonder: " + isGoPonder + ", ec.chessEngineSearchingPonder: " + ec.chessEngineSearchingPonder);
							ec.getEngine().writeLineToProcess("stop");
							if (ec.getEngine().syncReady())
								ec.getEngine().startSearch(ec.ponderUserFen, "", wTime, bTime, wInc, bInc, moveTime, movesToGo, ec.chessEngineAnalysis, isGoPonder, mate);
						}
					}
					ec.chessEngineSearchingPonder = false;
					isNoRespond = false;
					cancelTask = false;
					ec.chessEngineSearchingPonder = false;
					isGoPonder = false;
					ec.ponderUserMove = "";
					ec.ponderUserFen = "";
				}

				if (cancelTask & !s.equals(""))
				{
//Log.i(TAG, "cancelTask, s: " + s);
					if (ec.chessEnginePlayMod == 4 & ec.chessEngineAnalysisStat < 9 & !currentBestMove.equals(""))
					{	// analysis has stopped
						ec.getEngine().writeLineToProcess("stop");
						isNoRespond = false;
						return currentBestMove;
					}
					else
						return "CANCEL";			// ChessEngineSearchTask is cancelled
				}
				if (isNoRespond)
				{
					Log.i(TAG, "searchTask, isNoRespond");
					return "";
				}
				if (isTimeOut)
				{
					ec.getEngine().writeLineToProcess("stop");
					isTimeOut = false;
					return "";
				}
			}
		}

		protected void onPreExecute()
		{
			isTimeOut = false;
			cancelTask = false;
			ec.chessEngineStopSearch = false;
			searchStartTimeInfo = System.currentTimeMillis();
			isNoRespond = false;
			engineMes = "";
			engineStat = "";
			if (infoPv == null | ec.chessEngineInit)
				initInfoArrays(true);
			else
				initInfoArrays(false);
			isSearchTaskStopped = false;
			if (!ec.chessEngineAutoRun)
			{
				if (userPrefs.getBoolean("user_options_enginePlay_OpeningBook", true) & ec.chessEnginesOpeningBook)
					setInfoMessage(getString(R.string.engine_openingBook), "", null);
				else
					setInfoMessage(getEngineThinkingMessage(), "", null);
			}
		}

		protected void onProgressUpdate(CharSequence... args)
		{	// called from doInBackground() : publishProgress()
			currentTime = System.currentTimeMillis();
			if (cancelTask & ((int) (currentTime - searchStartTimeInfo) > MAX_SEARCH_CANCEL_TIMEOUT))
				isNoRespond = true;
			if ((int) (currentTime - searchStartTimeInfo) > MAX_SEARCH_TIMEOUT)
				isNoRespond = true;
			CharSequence pvAction = args[0];
			CharSequence engineMes = args[1];
			CharSequence engineStat = args[2];
//Log.i(TAG, "onProgressUpdate(), engineMes: " + engineMes + ", preEngineMes: " + preEngineMes);
			if (engineMes.equals(preEngineMes) & !pvAction.equals("0"))
				return;

			preEngineMes = engineMes;
//Log.i(TAG, "ec.chessEnginePaused, pvAction, engineMes: " + ec.chessEnginePaused + ", " + pvAction);
//Log.i(TAG, engineMes.toString());
//Log.i(TAG, "onProgressUpdate(), gc.isGameOver, gc.cl.p_variationEnd: " + gc.isGameOver + ", " + gc.cl.p_variationEnd);
			if (tc.clockIsRunning)
			{
				if (gc.isGameOver | gc.cl.p_variationEnd)
				{
					tc.stopChessClock(System.currentTimeMillis());
					if (!ec.chessEngineAutoRun)
					{
						ec.chessEnginePaused = true;
						ec.chessEngineSearching = false;
					}
					updateCurrentPosition("");
					setInfoMessage(getGameOverMessage(), engineMes, null);
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
					if (!ec.chessEngineAutoRun)
						setInfoMessage(getString(R.string.engine_openingBook), "", null);
					else
						setInfoMessage(getString(R.string.engine_autoPlay) + showGameCount + "\n" + getString(R.string.engine_openingBook), "", null);
				}
				else
				{
					if(pvAction.equals("0"))
						engineMes = "";

					if (!ec.chessEngineAutoRun)
					{
						if (getEngineThinkingMessage().equals(getString(R.string.engineAnalysisStopWait)))
							engineStat = "";
						setInfoMessage(getEngineThinkingMessage() + " " + engineStat, engineMes, null);
					}
					else
						setInfoMessage(null, engineMes, null);
				}
			}
		}

		protected void onPostExecute(CharSequence result)
		{
Log.i(TAG, "onPostExecute(), result, isSearchTaskStopped: " + result + ", " + isSearchTaskStopped);
			ec.chessEngineAnalysis = false;
			if (isNoRespond)
			{
				stopChessClock();
				ec.chessEngineSearching = false;
				ec.stopComputerThinking(true);
				ec.chessEnginePaused = true;
				ec.chessEngineInit = true;
				updateCurrentPosition("");
				if (!cancelTask)
					setInfoMessage(getString(R.string.engine_timeout), null, null);
				return;
			}
			if (result.equals("CANCEL") | cancelTask)
			{
				ec.chessEngineSearching = false;
				ec.chessEngineStopSearch = false;
				ec.stopComputerThinking(false);
				if (ec.chessEnginePlayMod == 4)
				{
					ec.chessEnginePaused = true;
					updateGui();
					stopChessClock();
					setInfoMessage(getEnginePausedMessage(), null, null);
					Toast.makeText(MainActivity.this, getString(R.string.engineAnalysisStop), Toast.LENGTH_SHORT).show();
				}
				return;
			}
			if (!isSearchTaskStopped)
			{
				if (!ec.getEngine().getSearchAlive())
				{
					Log.i(TAG, "!ec.getEngine().getSearchAlive()");
					ec.getEngine().searchAlive = true;
					if (ec.chessEngineInit)
					{
						ec.chessEnginePaused = true;
						setInfoMessage(getString(R.string.engine_paused), null, null);
					}
					else
					{
						ec.stopComputerThinking(false);
						ec.chessEngineInit = true;
						ec.chessEnginePaused = true;
						updateGui();
						stopChessClock();
						setInfoMessage(getString(R.string.engine_noRespond), null, null);
					}
				}
				else
				{
					initInfoArrays(false);
					enginePlay(result, taskFen, currentPonderMove);		//>243 analyse best move and set next action
				}
			}
		}

		protected void setSearchTime()
		{
			switch (userPrefs.getInt("user_options_timeControl", 1))
			{
				case 1:     // game clock
				case 3:     // sand glass
					wTime = tc.timeWhite;
					bTime = tc.timeBlack;
					wInc = tc.bonusWhite;
					bInc = tc.bonusBlack;
					movesToGo = 00;
					moveTime = 0;
//Log.i(TAG, "taskFen: " + taskFen);
//Log.i(TAG, "wt, bt, wi, bi: " + wTime + ", " + bTime + ", " + wInc + ", " + bInc);
					break;
				case 2:     // move time
					wTime = 0;
					bTime = 0;
					wInc = 0;
					bInc = 0;
					movesToGo = 1;
					setMoveTime();
					moveTime = userPrefs.getInt("user_time_engine_move", 1000);
					break;
				case 4:     // no time control

					break;
			}
			if (wTime < 1)	wTime = 100;
			if (bTime < 1)	bTime = 100;
		}

		protected CharSequence setRandomFirstMove(CharSequence fen)
		{
			taskMoves = "";
			if (taskFen.equals(gc.standardFen) & userPrefs.getBoolean("user_options_enginePlay_RandomFirstMove", false))
				taskMoves = ec.getEngine().getRandomFirstMove();
			return taskMoves;
		}

		protected CharSequence getInfoPv(int statPvIdx, CharSequence statPvMoves, int statPvScore, boolean isMate, CharSequence fen)
		{
			sbInfo.setLength(0);
//Log.i(TAG, "getInfoPv, statPvIdx: " + statPvIdx + ", infoPv.size(): " + infoPv.size() + ", statPvScore: " + statPvScore);
			if 	(	infoPv.size() 	== 	userPrefs.getInt("user_options_enginePlay_MultiPv", 4)
					& statPvIdx 	< 	userPrefs.getInt("user_options_enginePlay_MultiPv", 4)
					)
			{
				infoPv.set(statPvIdx, statPvMoves);
				if (statPvIdx == 0)
					bestScore = getBestScore(statPvScore, fen);
				CharSequence displayScore = getDisplayScore(statPvScore, fen);
				if (isMate & statPvScore > 0)
					displayScore = "M" + statPvScore;
				sbMoves.setLength(0); sbMoves.append("*"); sbMoves.append((statPvIdx +1)); sbMoves.append("(");
				CharSequence notation = gc.cl.getNotationFromInfoPv(taskFen, statPvMoves);
				if (notation.equals(""))
					return "";
				sbMoves.append(displayScore); sbMoves.append(") "); sbMoves.append(notation);
//Log.i(TAG, "taskFen: " + taskFen);
//Log.i(TAG, "statPvMoves: " + statPvMoves);
//Log.i(TAG, "sbMoves: "  + sbMoves);
				infoMessage.set(statPvIdx, sbMoves.toString());
			}
			for (int i = 0; i < infoPv.size(); i++)
			{
				if (!infoMessage.get(i).toString().equals(""))
				{
					sbInfo.append(infoMessage.get(i)); sbInfo.append("\n");
				}
			}
			return sbInfo.toString();
		}

		protected CharSequence getInfoStat(int statPVDepth, int statCurrMoveNr, CharSequence statCurrMove, int statTime, int statNodes, int statNps)
		{
			CharSequence infoStat = "";
			infoStat = "  <" + statPVDepth + ">";
			return infoStat;
		}

		protected int getBestScore(int score, CharSequence fen)
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
//Log.i(TAG, "getBestScore: " + score);
			return score;
		}

		int wTime = 100;
		int bTime = 100;
		int wInc = 1000;
		int bInc = 1000;
		int engineSearchTimeout = 1000;
		int moveTime = 1000;
		int movesToGo = 0;
		CharSequence currentBestMove = "";
		CharSequence currentPonderMove = "";
		boolean isDrawOffer = false;
		boolean isNoRespond = false;
		long currentTime = 0;
		CharSequence taskFen = "";
		CharSequence taskMoves = "";
		StringBuilder sbMoves = new StringBuilder(100);
		StringBuilder sbInfo = new StringBuilder(100);

		long searchStartTimeInfo = 0;		// info != ""
		int MAX_SEARCH_TIMEOUT = 180000;	// max. search time engine timeout (3 min: no info message)
		int MAX_SEARCH_CANCEL_TIMEOUT = 1500;	// max. search time engine timeout
		boolean cancelTask = false;

		CharSequence preEngineMes = "";

	}
	//  end ENGINE-SearchTask

	public void enginePlay(CharSequence result, CharSequence taskFen, CharSequence currentPonderMove)
	{
Log.i(TAG, "enginePlay() start, result, taskFen: " + result + "\n" + taskFen);
		if (!result.equals(""))
		{
			searchTaskRestart = false;
			if (!ec.chessEnginePaused & !result.equals("(none)"))
			{
				cancelSearchTask();
				CharSequence newFen = "";
				if (ec.chessEnginePlayMod == 4 | ec.chessEngineAnalysisStat == 2)
				{	// analysis | make move by user action
					if (ec.chessEngineAnalysisStat == 1 | ec.chessEngineAnalysisStat == 2)
						newFen = chessEngineGui(taskFen, result);
					if (ec.chessEnginePlayMod == 1 | ec.chessEnginePlayMod == 2)
					{
						ec.chessEngineSearching = false;
						if (userPrefs.getInt("user_options_timeControl", 1) == 2)
							setMoveTime();
						playSound(1, 0);
						handlerChessClock.removeCallbacks(mUpdateChessClock);
						handlerChessClock.postDelayed(mUpdateChessClock, 20);
						return;
					}

					if (ec.chessEngineAnalysisStat == 0 | ec.chessEngineAnalysisStat == 1)
					{
						newFen = "";
						setPauseEnginePlay(false);
						ec.getEngine().newGame();
					}
					stopChessClock();
					boolean engineIsReady = ec.getEngine().isReady;
					if (!engineIsReady)
					{
						ec.stopComputerThinking(true);
						ec.chessEnginePaused = true;
						ec.chessEngineInit = true;
						updateCurrentPosition("");
						setInfoMessage(getEnginePausedMessage(), null, null);
					}
					else
					{
						if (ec.chessEngineAnalysisStat == 2)
						{
							ec.getEngine().newGame();
							startChessClock();
							isGoPonder = false;
							chessEngineBestMove(newFen, "");
							if (ec.chessEnginePlayMod == 4)
								ec.chessEngineSearching = true;
						}
						else
							updateCurrentPosition("");
					}
					ec.chessEngineStopSearch = false;
				}
				else
				{
					newFen = chessEngineGui(taskFen, result);
//Log.i(TAG, "enginePlay() newFen: " + newFen);
					if (!newFen.equals(""))
					{
						if (ec.chessEnginePlayMod == 3)
						{	// engine vs engine
							// autostop enginePlay if >  < , canceled (error: statPvBestScore)
							if (ec.chessEngineAutoRun)
							{
//Log.i(TAG, "gameOver, variationEnd, result: " + gc.isGameOver + ", " + gc.cl.p_variationEnd + ", " + gc.cl.history.getGameResult());
								if 		(	gc.isGameOver | result.equals("(none)")
										| gc.cl.p_variationEnd | gc.cl.p_mate | gc.cl.p_stalemate
										)
								{
									messageEngine 		= "";
									if (userPrefs.getBoolean("user_play_eve_autoSave", true))
										startSaveGame(0);	// >>> onActivityResult(), SAVE_GAME_REQUEST_CODE
									else
										nextGameEngineAutoPlay();
								}
								else
								{
									ec.chessEngineSearching = true;
									isGoPonder = false;
									chessEngineBestMove(newFen, "");
								}
							}
							else
							{
//Log.i(TAG, "gc.isGameOver, gc.cl.p_variationEnd, gc.cl.p_mate, gc.cl.p_stalemate: " + gc.isGameOver + ", " + gc.cl.p_variationEnd + ", " + gc.cl.p_mate + ", " + gc.cl.p_stalemate);
								if 	(	!gc.isGameOver & !gc.cl.p_variationEnd & !result.equals("(none)")
										& !gc.cl.p_mate & !gc.cl.p_stalemate
										)
								{
									ec.chessEngineSearching = true;
									isGoPonder = false;
									chessEngineBestMove(newFen, "");
								}
								else
								{
									ec.stopComputerThinking(false);
									ec.chessEnginePaused = true;
									updateCurrentPosition("");
									setInfoMessage(getEnginePausedMessage(), null, null);
								}
							}
						}
						else
						{
							ec.chessEngineSearching = false;
							if (userPrefs.getInt("user_options_timeControl", 1) == 2)
								setMoveTime();
						}

Log.i(TAG, "enginePlay(), newFen: " + newFen + "\nresult: " + result + ", ponderMove: " + currentPonderMove);
						if 	(		isPonder & (ec.chessEnginePlayMod == 1 | ec.chessEnginePlayMod == 2)
								& 	!currentPonderMove.equals("")
							)
						{
							ec.chessEngineSearchingPonder = true;
							isGoPonder = true;
							ponderMove = currentPonderMove;
Log.i(TAG, "1 chessEngineBestMove()");
							chessEngineBestMove(newFen, currentPonderMove);	// ponderMove !!!
						}
						else
						{
							ec.chessEngineSearchingPonder = false;
							isGoPonder = false;
							ec.ponderUserMove = "";
							ec.ponderUserFen = "";
						}


						if (ec.chessEnginePlayMod == 1 | ec.chessEnginePlayMod == 2)
							playSound(1, 0);
						handlerChessClock.removeCallbacks(mUpdateChessClock);
						handlerChessClock.postDelayed(mUpdateChessClock, 20);
					}
				}
			}
		}
		else
		{
			if (!searchTaskRestart)
			{
				searchTaskRestart = true;
				ec.chessEngineSearching = true;
				isGoPonder = false;
				chessEngineBestMove(searchTaskFen, searchTaskMoves);
			}
			else
			{
				ec.chessEngineInit = true;
				ec.chessEnginePaused = true;
				updateGui();
				setInfoMessage(getString(R.string.engine_noRespond), null, null);
			}
		}
	}

	public CharSequence chessEngineGui(CharSequence taskFen, CharSequence bestMove)
	{
		CharSequence newFen = "";
		if (bestMove.length() > 4)			// promotion
		{
			if (bestMove.charAt(4) == 'q') bestMove = ((String) bestMove).substring(0, 4) + 'Q';
			if (bestMove.charAt(4) == 'r') bestMove = ((String) bestMove).substring(0, 4) + 'R';
			if (bestMove.charAt(4) == 'b') bestMove = ((String) bestMove).substring(0, 4) + 'B';
			if (bestMove.charAt(4) == 'n') bestMove = ((String) bestMove).substring(0, 4) + 'N';
		}
		gc.cl.newPositionFromMove(taskFen, bestMove);
		if (gc.cl.p_stat.equals("1"))
		{
			if (ec.chessEnginePlayMod <= 3 & userPrefs.getBoolean("user_options_gui_FlipBoard", false))
				startTurnBoard();
			if (ec.chessEnginePlayMod != 3)
				ec.chessEngineSearching = false;
			gc.setGameOver(gc.cl.history.getGameTagValue("Result"));
			updateGui();
			newFen = gc.cl.p_fen;
			gc.move = "";
		}
		else
		{
			gc.cl.p_fen = "";
			gc.cl.p_color = "";
			updateGui();
		}
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
//Log.i(TAG, "displayWidth, minScrollingWidth: " + displayWidth + ", " + minScrollingWidth);
		return size;
	}

	public void initInfoArrays(boolean createArrays)
	{
		infoPv = new ArrayList<CharSequence>();
		infoMessage = new ArrayList<CharSequence>();
		for (int i = 0; i < userPrefs.getInt("user_options_enginePlay_MultiPv", 4); i++)
		{
			infoPv.add("");
			infoMessage.add("");
		}
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
		int lastMoveIdx = gc.cl.p_moveIdx;
		setInfoMoveValuesFromView(view, event);
		nextMove(19, getMoveIdxFromInfo());		// set moveIdx
		if (!ec.chessEnginePaused & gc.cl.p_stat.equals("1"))
		{
			stopThreads(false);
			startPlay(false, true);
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
//Log.i(TAG, "startBoardMoveAction(), onScreenTouch, x: " + event.getRawX() + ", y: " + event.getRawY());
//Log.i(TAG, "startBoardMoveAction(), boardView.getLocationOnScreen(), x: " + screenXY[0] + ", y: " + screenXY[1] + ", pos: " + pos);
Log.i(TAG, "startBoardMoveAction(), ec.chessEngineSearching: " + ec.chessEngineSearching + ", ec.chessEngineSearchingPonder: "+ ec.chessEngineSearchingPonder);
			gc.isMoveError = false;
			if (ec.chessEngineSearching & !ec.chessEnginePaused)
			{
				if (ec.chessEnginePlayMod == 1 | ec.chessEnginePlayMod == 2)
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
Log.i(TAG, "moveAction(), position: " + position);
		if (gc.cl.p_mate | gc.cl.p_stalemate)	// mate, steal mate?
		{
			setInfoMessage(getGameOverMessage(), null, null);
			return;
		}
		if (!gc.isGameOver & !gc.cl.p_variationEnd)	// move actions only on: edit/play
		{
			CharSequence field = "";
			try
			{
				field = boardView.getChessField(position, gc.isBoardTurn);
				if (!gc.move.equals(field))
					gc.move = gc.move.toString() + field;
				if (gc.move.length() >= 4)
				{
					if (gc.move.subSequence (0, 2).equals(gc.move.subSequence (2, 4)))
						gc.move = gc.move.subSequence (2, gc.move.length());
				}
//Log.i(TAG, "gc.move: " + gc.move);
				gc.cl.newPositionFromMove(gc.fen, gc.move);
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
					if (gc.cl.p_stat.equals("1"))
					{
						if (userPrefs.getBoolean("user_options_gui_FlipBoard", false))
							startTurnBoard();
						gc.isGameUpdated = false;
						gc.move = "";
						if (ec.chessEnginePaused)
							setInfoMessage(getString(R.string.engine_paused), "", null);
					}

					if (!(ec.chessEnginePlayMod == 5 | ec.chessEnginePlayMod == 6))
					{
						gc.setGameOver(gc.cl.history.getGameTagValue("Result"));
						if (gc.cl.p_stat.equals("1") & !gc.isGameOver & !gc.cl.p_variationEnd)
							ec.chessEngineSearching = true;
					}
					if (ec.chessEnginePlayMod == 5 & twoPlayerPaused & gc.cl.p_stat.equals("1"))
					{
						twoPlayerPaused = false;
						startChessClock();
					}
					updateGui();
Log.i(TAG, "1 moveAction(), ec.chessEngineSearching: " + ec.chessEngineSearching);
					if 	(		ec.chessEngineSearching & gc.cl.p_stat.equals("1")
							& 	!gc.isGameOver & !gc.cl.p_variationEnd
							)
					{
						if (!ec.chessEnginePaused & ec.chessEnginePlayMod == 4)
						{
							stopThreads(false);
							startEnginePlay();
						}
						else
						{
Log.i(TAG, "2 moveAction(), gc.cl.p_fen: " + gc.cl.p_fen);
							isGoPonder = false;
//							ec.chessEngineSearchingPonder = false;
//							ec.ponderUserMove = "";
//							ec.ponderUserFen = "";
							chessEngineBestMove(gc.cl.p_fen, "");
						}
					}
				}
			}
			catch (NullPointerException e) {e.printStackTrace();}
//Log.i(TAG, "stat, gc.move, move, hasMoves: " + gc.cl.p_stat + ", " + gc.move + ", " + gc.cl.p_move + ", " + gc.cl.p_hasPossibleMoves);
		}
	}

	public void setPlayerData()
	{
		if (!gc.isBoardTurn)
		{
			lblPlayerNameA.setText(gc.cl.history.getGameTagValue("Black"));
			lblPlayerEloA.setText(gc.cl.history.getGameTagValue("BlackElo"));
			lblPlayerNameB.setText(gc.cl.history.getGameTagValue("White"));
			lblPlayerEloB.setText(gc.cl.history.getGameTagValue("WhiteElo"));
		}
		else
		{
			lblPlayerNameA.setText(gc.cl.history.getGameTagValue("White"));
			lblPlayerEloA.setText(gc.cl.history.getGameTagValue("WhiteElo"));
			lblPlayerNameB.setText(gc.cl.history.getGameTagValue("Black"));
			lblPlayerEloB.setText(gc.cl.history.getGameTagValue("BlackElo"));
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
//Log.i(TAG, "updateTime(), gc.startFen: " + gc.startFen);
//Log.i(TAG, "updateTime(), ec.chessEnginePaused: " + ec.chessEnginePaused);
			if (color.equals("w"))
				tc.switchChessClock(true, System.currentTimeMillis());
			else
				tc.switchChessClock(false, System.currentTimeMillis());
			if (ec.chessEnginePlayMod == 4)
			{
				updateTimeBackground(lblPlayerTimeA, 2);
				updateTimeBackground(lblPlayerTimeB, 2);
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
					updateTimeBackground(lblPlayerTimeA, tc.showModusBlack);
					lblPlayerTimeA.setText(tc.showBlackTime);
					updateTimeBackground(lblPlayerTimeB, tc.showModusWhite);
					lblPlayerTimeB.setText(tc.showWhiteTime);
				}
				else
				{
					updateTimeBackground(lblPlayerTimeA, tc.showModusWhite);
					lblPlayerTimeA.setText(tc.showWhiteTime);
					updateTimeBackground(lblPlayerTimeB, tc.showModusBlack);
					lblPlayerTimeB.setText(tc.showBlackTime);
				}
			}

			infoShowPv = true;

			oldTimeWhite = tc.showWhiteTime;
			oldTimeBlack = tc.showBlackTime;
			handlerChessClock.removeCallbacks(mUpdateChessClock);
			handlerChessClock.postDelayed(mUpdateChessClock, 250);
		}
	}

	public void updateTimeBackground(TextView tv, int colorId)
	{
		switch (colorId)
		{
			case 1:     // >= 10 min
			case 2:     // >= 10 sec
				tv.setBackgroundResource(R.drawable.bordergrey);
				break;
			case 3:     // < 10 sec
				tv.setBackgroundResource(R.drawable.borderpink);
				break;
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
		ec.stopComputerThinking(false);
		ec.setPlaySettings(userPrefs);
		ec.chessEngineInit = true;
		ec.chessEnginePaused = true;
		initChessClock();
		updateCurrentPosition("");
	}

	public void setPauseValues(boolean auto, CharSequence fen, int mode, CharSequence messageEngine)
	{
//Log.i(TAG, "setPauseValues(), auto: " + auto + ", fenMes: " + fenMes + ", mode: " + mode + "\nmessageEngine:\n" + messageEngine);
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

	public void updateGui()
	{
//Log.i(TAG, "start updateGui()");
		CharSequence messInfo = 	"";
		if (!gc.isGameOver & !gc.cl.p_variationEnd & gc.cl.p_message.equals(""))
		{
			showGameCount = "";
			if (ec.chessEnginePlayMod == 3 & ec.chessEngineAutoRun)
			{
				int cnt = userPrefs.getInt("user_play_eve_gameCounter", 1);
				showGameCount = " #" + cnt;
			}
			else
				showGameCount = "";
			if (ec.chessEngineSearching)
			{
				if (!ec.chessEnginePaused)
				{
					if (ec.chessEngineAutoRun)
					{
						if (gc.oldGameResult.equals(""))
							messInfo = getString(R.string.engine_autoPlay) + showGameCount;
						else
							messInfo = gc.oldGameResult;
					}
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
				if (!gc.isGameOver & !gc.cl.p_variationEnd)
				{
					if (ec.chessEnginePaused | ec.chessEnginePlayMod == 4)
						messInfo = getEnginePausedMessage();
					else
					{
//Log.i(TAG, "updateGui(), player_move");
						messInfo = getString(R.string.player_move);
					}
				}
				if (gc.isAutoPlay)
					messInfo = getString(R.string.ccsMessageAutoPlay);
			}
			ec.chessEngineProblem = false;
		}

		setPlayerData();
		if (!gc.cl.p_fen.equals(""))
		{
			gc.fen = gc.cl.p_fen;
			if (gc.cl.p_color.equals("w"))
			{
				if (!gc.isBoardTurn)
				{
					lblPlayerNameA.setBackgroundResource(R.drawable.bordergreen);
					lblPlayerNameB.setBackgroundResource(R.drawable.borderpink);
				}
				else
				{
					lblPlayerNameA.setBackgroundResource(R.drawable.borderpink);
					lblPlayerNameB.setBackgroundResource(R.drawable.bordergreen);
				}
			}
			else
			{
				if (!gc.isBoardTurn)
				{
					lblPlayerNameA.setBackgroundResource(R.drawable.borderpink);
					lblPlayerNameB.setBackgroundResource(R.drawable.bordergreen);
				}
				else
				{
					lblPlayerNameA.setBackgroundResource(R.drawable.bordergreen);
					lblPlayerNameB.setBackgroundResource(R.drawable.borderpink);
				}
			}
		}

		if (gc.cl.p_message.equals("*"))
			gc.cl.p_message = "";

		if (gc.cl.p_moveText.equals(""))
		{
			if (!gc.errorMessage.equals(""))
			{
				msgMoves.setBackgroundResource(R.drawable.borderpink);
				messInfo = gc.errorMessage;
			}
		}
		else
			msgMoves.setBackgroundResource(R.drawable.borderblue);
		if (!messInfo.equals(""))
		{
			if (gc.cl.p_message.equals(""))
				setInfoMessage(gc.cl.p_message + "" + messInfo, null, gc.cl.p_moveText);
			else
				setInfoMessage(gc.cl.p_message + ", " + messInfo, null, gc.cl.p_moveText);
		}
		else
		{
			setInfoMessage(gc.cl.p_message, null, gc.cl.p_moveText);
		}
		if (gc.cl.p_moveHasVariations | gc.cl.p_moveIsFirstInVariation)	// move has variation? | variation start
			gc.hasVariations = true;
		else
			gc.hasVariations = false;


		if (!gc.cl.p_fen.equals(""))
		{
//Log.i(TAG, "gc.cl.p_move, possibleMoves: " + gc.cl.p_move + ", " + gc.cl.p_hasPossibleMoves);
//Log.i(TAG, "gc.cl.p_move1, gc.cl.p_move2: " + gc.cl.p_move1 + ", " + gc.cl.p_move2);
//Log.i(TAG, "gc.cl.p_moveShow1, gc.cl.p_moveShow2: " + gc.cl.p_moveShow1 + ", " + gc.cl.p_moveShow2);
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
			boardView.updateBoardView(gc.cl.p_fen, gc.isBoardTurn, possibleMoves, possibleMovesTo,
					lastMove, userPrefs.getBoolean("user_options_gui_Coordinates", false));
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
		}
	}


	final String TAG = "MainActivity";
	final CharSequence APP_EMAIL = "c4akarl@gmail.com";
	final String MY_APPS = "https://play.google.com/store/apps/developer?id=Karl+Schreiner";
//	final String MY_APPS = "https://play.google.com/store/apps/details?id=com.androidheads.vienna.escalero";

	private static final int PERMISSIONS_REQUEST_CODE = 50;
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

	// pause control (manually | automatically)
	boolean 		pause_auto 					= false;
	CharSequence 	pause_fen 					= "";
	int 			pause_mode 					= 0;
	CharSequence 	pause_messageEngine 		= "";

	GameControl gc;
	EngineControl ec;
	TimeControl tc;
	Chess960 chess960;

	ChessPromotion promotionDialog;
	PgnIO pgnIO;
	ChessEngineSearchTask chessEngineSearchTask = null;
	private SoundPool mSoundPool;
	private HashMap<Integer, Integer> soundsMap;

	//	subActivities intents
	Intent playEngineIntent;
	Intent fileManagerIntent;
	Intent gameDataIntent;
	Intent notationIntent;
	Intent moveTextIntent;
	Intent optionsGuiIntent;
	Intent optionsTimeControlIntent;
	Intent optionsPlayIntent;
	Intent optionsEnginePlayIntent;
	Intent editChessBoardIntent;

	//	subActivities RequestCode
	final static int LOAD_GAME_REQUEST_CODE = 1;
	final static int LOAD_GAME_PREVIOUS_CODE = 9;
	final static int SAVE_GAME_REQUEST_CODE = 2;
	final static int SAVE_LOAD_GAME_REQUEST_CODE = 7;
	final static int SAVE_OK_LOAD_GAME_REQUEST_CODE = 71;
	final static int SAVE_ERROR_LOAD_GAME_REQUEST_CODE = 72;
	final static int GAME_DATA_REQUEST_CODE = 49;
	final static int NOTATION_REQUEST_CODE = 5;
	final static int MOVETEXT_REQUEST_CODE = 6;
	final static int OPTIONS_CHESSBOARD_REQUEST_CODE = 13;
	final static int OPTIONS_GUI_REQUEST_CODE = 14;
	final static int OPTIONS_ENGINE_AUTO_PLAY_REQUEST_CODE = 15;
	final static int OPTIONS_TIME_CONTROL_REQUEST_CODE = 18;
	final static int OPTIONS_PLAY_REQUEST_CODE = 19;
	final static int OPTIONS_ENGINE_PLAY_REQUEST_CODE = 21;
	final static int EDIT_CHESSBOARD_REQUEST_CODE = 17;
	final static int CHESS960_POSITION_REQUEST_CODE = 20;
	final static int ENGINE_SEARCH_REQUEST_CODE = 40;
	final static int ENGINE_SETTING_REQUEST_CODE = 41;
	final static int RATE_REQUEST_CODE = 42;

	//  dialogs RequestCode
	final static int MOVE_NOTIFICATION_DIALOG = 110;
	final static int PGN_ERROR_DIALOG = 198;
	final static int FILE_LOAD_PROGRESS_DIALOG = 199;
	final static int VARIATION_DIALOG = 200;
	final static int NAG_DIALOG = 201;
	final static int PROMOTION_DIALOG = 300;
	final static int TIME_SETTINGS_DIALOG = 400;
	final static int ENGINE_SEARCH_DIALOG = 500;
	final static int ENGINE_SEARCH_NO_INTERNET_DIALOG = 501;
	final static int QUERY_DIALOG = 600;
	final static int GAME_ID_DIALOG = 601;
	final static int MENU_BOARD_DIALOG = 701;
	final static int MENU_EDIT_DIALOG = 703;
	final static int MENU_ENGINES_DIALOG = 704;
	final static int MENU_SETTINGS_DIALOG = 705;
	final static int MENU_ABOUT_DIALOG = 706;
	final static int MENU_PAUSE_CONTINUE = 707;
	final static int MENU_PGN_DIALOG = 730;
	final static int MENU_CLIPBOARD_DIALOG = 731;
	final static int INFO_DIALOG = 909;
	final static int C4A_NEW_DIALOG = 999;

	//	GUI (R.layout.main)
//	RelativeLayout mainView = null;

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
	C4aImageDialog c4aImageDialog;
	ProgressDialog progressDialog = null;
	TimeSettingsDialog timeSettingsDialog;
	int activDialog = 0;

	//	handler
	public Handler handlerInitEngine = new Handler();
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
	boolean isSearchTaskStopped = false;
	long initEngineStartTime = 0;
	int lastTouchID = 0;
	long touchTime = 0;
	long longTouchTime = 600;
	CharSequence searchTaskFen = "";
	CharSequence searchTaskMoves = "";
	boolean searchTaskRestart = false;
	boolean startInitEngine = false;
	boolean startInitEnginePlay = false;

	int cntChess960 = 0;
	int cntResult = 0;

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
	int downViewId = 0;
	int infoMoveStartX = 0;
	int infoMoveEndX = 0;
	boolean infoMoveIsSelected = false;
	boolean infoShowPv = false;

	//  variables
	int gridViewSize = 0;
	CharSequence oldTimeWhite = "";
	CharSequence oldTimeBlack = "";
	CharSequence 	messageNotification = "";
	CharSequence 	messageInfo 		= "";
	CharSequence 	messageEngine 		= "";
	CharSequence 	messageEngineShort 	= "";
	CharSequence prevEngineMes = "";
	CharSequence prevEngineStat = "";
	CharSequence engineMes = "";
	CharSequence engineStat = "";
	ArrayList<CharSequence> infoPv;
	ArrayList<CharSequence> infoMessage;
	int bestScore = 0;
	CharSequence analysisMessage = "";
	CharSequence showGameCount = "";
	SpannableStringBuilder sb = new SpannableStringBuilder();
	String queryControl = "w";

	boolean isTimeOut = false;
	int mate = 0;

// c4aPonder
	boolean isGoPonder = false;		// set to true if start: go ponder
	CharSequence ponderMove = null;		// get ponderMove from computer move (bestmove e2e4 ponder e7e6)

//v2.2 ---> user preferences: ponder (Vorausberechnung)
	boolean isPonder = false;

}