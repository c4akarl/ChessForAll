package ccc.chess.gui.chessforall;

import android.Manifest;
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
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
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
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.kalab.chess.enginesupport.ChessEngine;
import com.kalab.chess.enginesupport.ChessEngineResolver;

import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import ccc.chess.book.ChessParseError;
import ccc.chess.book.Move;
import ccc.chess.book.Pair;
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
		initColors();
        setStringsValues();
        gc = new GameControl(stringValues, moveHistory);
        ec = new EngineControl(this);				//>011 engine controller
        ec.setBookOptions();
		tc = new TimeControl();

		getPermissions();

		chess960 = new Chess960();	// needed for "create your own chess position"
		fileIO = new FileIO(this);

		startGui();

    }

	//	GUI		GUI		GUI		GUI		GUI		GUI		GUI		GUI		GUI		GUI
	public void startGui()
	{
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
		msgShort2 = (TextView) findViewById(R.id.msgShort2);
		msgShort2.setOnTouchListener(this);
		scrlMsgMoves = (ScrollView) findViewById(R.id.scrlMsgMoves);
		scrlMsgMoves.setVerticalFadingEdgeEnabled(false);
		msgMoves = (TextView) findViewById(R.id.msgMoves);
		msgMoves.setOnTouchListener(this);

		scrlMsgEngine = (ScrollView) findViewById(R.id.scrlMsgEngine);
		msgEngine = (TextView) findViewById(R.id.msgEngine);
		msgEngine.setMovementMethod(new ScrollingMovementMethod());
		msgEngine.setMaxLines(userPrefs.getInt("user_options_enginePlay_displayedLines", OptionsEnginePlay.DISPLAYED_LINES));
		msgEngine.setLines(userPrefs.getInt("user_options_enginePlay_displayedLines", OptionsEnginePlay.DISPLAYED_LINES));

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
		boardView.updateBoardView(gc.fen, gc.isBoardTurn, null, null,
				null,null, false, userPrefs.getBoolean("user_options_gui_BlindMode", false));

		initDrawers();

		playEngineIntent = new Intent(this, PlayEngineSettings.class);
		fileManagerIntent = new Intent(this, FileManager.class);
		gameDataIntent = new Intent(this, ChessGameData.class);
		notationIntent = new Intent(this, ChessNotation.class);
		moveTextIntent = new Intent(this, ChessMoveText.class);
		optionsGuiIntent = new Intent(this, OptionsGUI.class);
		optionsTimeControlIntent = new Intent(this, OptionsTimeControl.class);
		optionsPlayIntent = new Intent(this, OptionsPlay.class);
		optionsEnginePlayIntent = new Intent(this, OptionsEnginePlay.class);
		optionsColorIntent = new Intent(this, OptionsColor.class);
		editChessBoardIntent = new Intent(this, EditChessBoard.class);
	}

	public void startGame()
	{
		mSoundPool = new SoundPool(2, AudioManager.STREAM_RING, 100);
		soundsMap = new HashMap<Integer, Integer>();
//		soundsMap.put(1, mSoundPool.load(this, R.raw.move_ok, 1));
		soundsMap.put(1, mSoundPool.load(this, R.raw.move_sound, 1));
		soundsMap.put(2, mSoundPool.load(this, R.raw.move_wrong, 1));
		setPieceName(userPrefs.getInt("user_options_gui_PieceNameId", 0));
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
		if  (	isNewApp
				|   (       userPrefs.getBoolean("user_options_enginePlay_AutoStartEngine", true)
				& 	!gc.isGameOver & !ec.chessEnginePaused
		)
				)
		{

			ec.chessEnginePaused = false;
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
				setInfoMessage(getEnginePausedMessage(), null, null, false);
			}
			if (gc.isGameOver | gc.cl.p_variationEnd)
			{
				ec.chessEnginePaused = true;
				ec.chessEngineSearching = false;
				setInfoMessage(getGameOverMessage(), null, null, false);
			}
			else
			{

				if (ec.chessEnginePaused)
					setInfoMessage(getEnginePausedMessage(), null, null, false);
				else
				{
					if (gc.cl.p_message.equals(""))
						setInfoMessage(getString(R.string.engine_paused), null, null, false);
					else
						setInfoMessage(gc.cl.p_message, null, null, false);
				}
			}
			ec.chessEnginePaused = true;
			ec.chessEngineSearching = false;
			if (ec.chessEnginePlayMod == 5)
				startEdit(false, false);
		}
		updateCurrentPosition("");
		getDataFromIntent(getIntent());

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
				removeDialog(MENU_SETTINGS_DIALOG);
				showDialog(MENU_SETTINGS_DIALOG);
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
				if (userPrefs.getInt("user_play_playMod", 1) != 4)
				{
					stopThreads(false);
					gc.isGameLoaded = false;
					ec.chessEnginePaused = false;
					ec.chessEngineInit = false;
					initChessClock();
					msgEngine.setVisibility(TextView.GONE);
					messageInfo 		= "";
					messageEngine 		= "";
					messageEngineShort  = "";
					if (ec.chessEnginePlayMod == 5)
						startEdit(true, true);
					else
						startPlay(true, true);
				}
				else
				{
					removeDialog(PLAY_DIALOG);
					showDialog(PLAY_DIALOG);
				}
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
				double scoreDouble;
				try{scoreDouble =	Double.parseDouble(getDisplayScore(ec.getEngine().statPvBestScore, gc.fen).toString());}
				catch(NumberFormatException e){	scoreDouble = 0;}
				if (ec.chessEnginePlayMod == 1 | ec.chessEnginePlayMod == 2)
				{
					if ((ec.chessEnginePlayMod == 1 & scoreDouble >= 4) | (ec.chessEnginePlayMod == 2 & scoreDouble <= -4))
						setTagResult("1/2-1/2", "");
					else
					{
						setInfoMessage(getString(R.string.engineDeclinesDraw), null, null, false);
						Toast.makeText(this, getString(R.string.engineDeclinesDraw), Toast.LENGTH_LONG).show();
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
    	isAppEnd = true;
    	isSearchTaskStopped = true;

    	stopTimeHandler(true);
    	try {Thread.sleep(200);} 
		catch (InterruptedException e) {}
    	setInfoMessage("onDestroy", "", "", false);
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
		super.onResume();

//Log.i(TAG, "onResume(): " + ec.chessEnginePaused + ", isAppStart: " + isAppStart + ", gc.isAutoPlay: " + gc.isAutoPlay);

		if (isAppStart)
			startGame();

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
//Log.i(TAG, "onPause()(), isAppStart: " + isAppStart);
		super.onPause();
		if (!isAppStart)
        	setTextViewColors(lblPlayerNameA, cv.COLOR_DATA_BACKGROUND_16, cv.COLOR_DATA_TEXT_17);
		isAppStart = false;
	}

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
//Log.i(TAG, "onNewIntent(), start getDataFromIntent(intent): ");
        getDataFromIntent(intent);
    }

	public boolean getDataFromIntent(Intent intent)
	{
//Log.i(TAG, "getDataFromIntent(), intentType: " + intent.getType());
		boolean isOk = false;
		// call from another Activity, passing the FEN(String)
		if (intent.getType() != null)
		{
			if (intent.getType().endsWith("x-chess-fenMes"))
			{
				if (intent.getStringExtra("fenMes") != null & !intent.getStringExtra("fenMes").equals(""))
				{
					startEditBoard(intent.getStringExtra("fenMes"), false);
					return true;
				}
			}
		}

		if (intent.getType() != null)
		{
			if (intent.getType().equals(FileManager.PGN_ACTION_CREATE_DB))
			{
			    int id = intent.getIntExtra("notificationId", 1);

//Log.i(TAG, "getDataFromIntent(), intentType: " + intent.getType() + ", id: " + id + ", pgnFileName: " + pgnFileName);

				String ns = Context.NOTIFICATION_SERVICE;
				NotificationManager nMgr = (NotificationManager) getApplicationContext().getSystemService(ns);
				nMgr.cancel(id);

				return true;
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
			intentData = fileIO.getExternalStorageFromContent(intentData); // ? content://
//Log.i(TAG, "getDataFromIntent(), intentData: " + intentData);
			if (intent.getType().endsWith("x-chess-pgn") & intentData.endsWith(".pgn"))	//".pgn-db" canceled
			{
				fileIO = new FileIO(this);
				String externLoadPath = fmPrefs.getString("fm_extern_load_path", "");
				if (!fmPrefs.getString("fm_last_selected_folder", "").equals(""))
					externLoadPath = fmPrefs.getString("fm_last_selected_folder", "");
				if (externLoadPath.equals(""))
				{
					String basePath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/" + fileIO.BASE_PATH;
					File f = new File(basePath);
					if (f.isDirectory())
						externLoadPath = basePath;
					else
					{
						if (fileIO.createDir(basePath))
							externLoadPath = basePath;
					}
				}
				File fUri = new File(intentData);
				String fName = fUri.getName();
				String fPath = fUri.getParent();
				boolean isDownload = false;
				if (fPath.contains("ownload") | fPath.contains("/Android") | fPath.contains("ontent"))
					isDownload = true;
				if (fPath.startsWith("file:"))
					fPath = fPath.replace("file:", "");
				if (!fPath.endsWith("/"))
					fPath = fPath + "/";
//Log.i(TAG, "getDataFromIntent(), fPath: " + fPath + ", fName: " + fName);
				if (!isDownload & fileIO.fileExists(fPath, fName))
				{
					isOk = true;

					File file = new File(fPath + fName);
					java.util.Date lastModDate = new java.util.Date(file.lastModified());
					SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.US);
					long timeInMilliseconds = 0;
					try
					{
						Date mDate = sdf.parse(lastModDate.toString());
						timeInMilliseconds = mDate.getTime();
//Log.i(TAG, "getDataFromIntent(), timeInMilliseconds: " + timeInMilliseconds);
					}
					catch (ParseException e) {e.printStackTrace();}
					long currentTime = System.currentTimeMillis();
//Log.i(TAG, "getDataFromIntent(), timeDiff: " + (currentTime - timeInMilliseconds));
					if ((currentTime - timeInMilliseconds) > MAX_DOWNLOAD_DIFF)		// from sd-card
					{
						SharedPreferences.Editor ed = fmPrefs.edit();
						ed.putString("fm_extern_load_path", fPath);
						ed.putString("fm_extern_load_file", fName);
						ed.commit();
						startFileManager(LOAD_GAME_REQUEST_CODE, 1, 0);
						return isOk;
					}
				}

				if (fileIO.copyFile(intentData, externLoadPath))							// download
				{
					isOk = true;
					SharedPreferences.Editor ed = fmPrefs.edit();
					ed.putString("fm_extern_load_path", externLoadPath);
					ed.putString("fm_extern_load_file", fName);
					ed.commit();
					startFileManager(LOAD_GAME_REQUEST_CODE, 1, 0);
				}
				else
				{
					String error = "\n\ndownload file:\n" + intentData + "\n\ncopy path:\n" + externLoadPath;
					downloadErrorMessage = getString(R.string.menu_load_www) + "\n" + getString(R.string.fmPgnError) + error;
					c4aShowDialog(DOWNLOAD_ERROR_DIALOG);
				}
			}
		}
		return isOk;
	}

	public void startEditBoard(CharSequence fen, Boolean startOptions) {
		editChessBoardIntent.putExtra("currentFen", fen);
		editChessBoardIntent.putExtra("gridViewSize", gridViewSize);
		editChessBoardIntent.putExtra("fieldSize", getChessFieldSize());
		editChessBoardIntent.putExtra("startOptions", startOptions);
		SharedPreferences.Editor ed3 = runP.edit();
		ed3.putBoolean("run_game0_is_board_turn", gc.isBoardTurn);
		ed3.commit();
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
//Log.i(TAG, "fileActionCode,: "	+ fileActionCode + ", displayActivity: " + displayActivity + ", gameLoad: " + gameLoad);
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
				case 1: 														// Load
					startActivityForResult(fileManagerIntent, fileActionCode);	// start FileManager - Activity(no GUI)
					break;
				case 2: 														// Save
					startSaveFile(gc.cl.history.createPgnFromHistory(1));		// saveFile(using class: FileIO)
					break;
				case 7: 														// Save(old game), Load(new game)
				case 71: 														// Save(old game, MateAnalysis OK), Load(new game)
				case 72: 														// Save(old game, MateAnalysis ERROR), Load(new game)
				default:
					startActivityForResult(fileManagerIntent, fileActionCode);	// start FileManager - Activity(no GUI)
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
		gc.cl.newPositionFromMove(gc.fen, gc.promotionMove, true);
		gc.promotionMove = "";
		if (gc.cl.p_stat.equals("1"))
		{
			if (!ec.chessEnginePaused)
			{
				ec.chessEngineSearching = true;
				ec.chessEnginePaused = false;
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

	private static boolean reservedEngineName(String name) {
		//karl defalt engines ???
		return "cuckoochess".equals(name) ||
				"stockfish".equals(name) ||
				name.endsWith(".ini");
	}


	private boolean storageAvailable() {
		return storagePermission == PermissionState.GRANTED;
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
					showHtml(R.raw.manual, R.string.menu_about_userManual);
				}
			});
			builder.setPositiveButton(R.string.btn_Continue, null);
			AlertDialog alert = builder.create();
			alert.setCancelable(true);
			return alert;
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

            if (ec.chessEnginePaused)
			    isPaused = true;
            else
                isPaused = false;
			dNewGame = false;
            setTextViewColors(lblPlayerNameB, cv.COLOR_DATA_BACKGROUND_16, cv.COLOR_DATA_TEXT_17);
			Dialog dialog = new Dialog(this);
			dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			dialog.setContentView(R.layout.dialogplay);

			MyViewListener myViewListener = new MyViewListener();

			d_cb_fullScreen = dialog.findViewById(R.id.cb_fullScreen);
			d_cb_fullScreen.setChecked(userPrefs.getBoolean("user_options_gui_StatusBar", false));
			d_cb_fullScreen.setOnClickListener(myViewListener);

			d_cb_lastPosition = dialog.findViewById(R.id.cb_lastPosition);
			d_cb_lastPosition.setChecked(userPrefs.getBoolean("user_options_gui_LastPosition", true));
			d_cb_lastPosition.setOnClickListener(myViewListener);

			d_cb_screenTimeout = dialog.findViewById(R.id.cb_screenTimeout);
			d_cb_screenTimeout.setChecked(userPrefs.getBoolean("user_options_gui_disableScreenTimeout", false));
			d_cb_screenTimeout.setOnClickListener(myViewListener);

			d_cb_audio = dialog.findViewById(R.id.cb_audio);
			d_cb_audio.setChecked(userPrefs.getBoolean("user_options_gui_enableSounds", true));
			d_cb_audio.setOnClickListener(myViewListener);

			d_cb_posibleMoves = dialog.findViewById(R.id.cb_posibleMoves);
			d_cb_posibleMoves.setChecked(userPrefs.getBoolean("user_options_gui_posibleMoves", true));
			d_cb_posibleMoves.setOnClickListener(myViewListener);

			d_cb_quickMove = dialog.findViewById(R.id.cb_quickMove);
			d_cb_quickMove.setChecked(userPrefs.getBoolean("user_options_gui_quickMove", true));
			d_cb_quickMove.setOnClickListener(myViewListener);

			d_cb_coordinates = dialog.findViewById(R.id.cb_coordinates);
			d_cb_coordinates.setChecked(userPrefs.getBoolean("user_options_gui_Coordinates", false));
			d_cb_coordinates.setOnClickListener(myViewListener);

			d_cb_blindMode = dialog.findViewById(R.id.cb_blindMode);
			d_cb_blindMode.setChecked(userPrefs.getBoolean("user_options_gui_BlindMode", false));
			d_cb_blindMode.setOnClickListener(myViewListener);

			d_cb_pgnDb = dialog.findViewById(R.id.cb_pgnDb);
			d_cb_pgnDb.setChecked(userPrefs.getBoolean("user_options_gui_usePgnDatabase", true));
			d_cb_pgnDb.setOnClickListener(myViewListener);

			d_cb_engineAutostart = dialog.findViewById(R.id.cb_engineAutostart);
			d_cb_engineAutostart.setChecked(userPrefs.getBoolean("user_options_enginePlay_AutoStartEngine", true));
			d_cb_engineAutostart.setOnClickListener(myViewListener);

			d_cb_openingBook = dialog.findViewById(R.id.cb_openingBook);
			d_cb_openingBook.setChecked(userPrefs.getBoolean("user_options_enginePlay_OpeningBook", true));
			d_cb_openingBook.setOnClickListener(myViewListener);

			d_cb_openingBookHints = dialog.findViewById(R.id.cb_openingBookHints);
			d_cb_openingBookHints.setChecked(userPrefs.getBoolean("user_options_enginePlay_ShowBookHints", true));
			d_cb_openingBookHints.setOnClickListener(myViewListener);

			d_cb_engineThinking = dialog.findViewById(R.id.cb_engineThinking);
			d_cb_engineThinking.setChecked(userPrefs.getBoolean("user_options_enginePlay_EngineMessage", true));
			d_cb_engineThinking.setOnClickListener(myViewListener);

			d_cb_ponder = dialog.findViewById(R.id.cb_ponder);
			d_cb_ponder.setChecked(userPrefs.getBoolean("user_options_enginePlay_Ponder", false));
			d_cb_ponder.setOnClickListener(myViewListener);

			d_cb_moveList = dialog.findViewById(R.id.cb_moveList);
			d_cb_moveList.setChecked(userPrefs.getBoolean("user_options_gui_moveList", true));
			d_cb_moveList.setOnClickListener(myViewListener);

			dChessEnginePlayMod = ec.chessEnginePlayMod;
			d_btn_white = dialog.findViewById(R.id.btn_white);
			d_btn_white.setOnClickListener(myViewListener);
			d_btn_black = dialog.findViewById(R.id.btn_black);
			d_btn_black.setOnClickListener(myViewListener);
			d_btn_engine = dialog.findViewById(R.id.btn_engine);
			d_btn_engine.setOnClickListener(myViewListener);

			d_btn_player = dialog.findViewById(R.id.btn_player);
			d_btn_player.setOnClickListener(myViewListener);
			d_btn_edit = dialog.findViewById(R.id.btn_edit);
			d_btn_edit.setOnClickListener(myViewListener);
			d_btn_analysis = dialog.findViewById(R.id.btn_analysis);
			d_btn_analysis.setOnClickListener(myViewListener);
            setPlayModBackground(dChessEnginePlayMod);

			d_btn_menu = dialog.findViewById(R.id.btn_menu);
			d_btn_menu.setOnClickListener(myViewListener);
			d_btn_960 = dialog.findViewById(R.id.btn_960);
			d_btn_960.setOnClickListener(myViewListener);
			d_btn_960.setOnLongClickListener(myViewListener);
			d_cb_newGame = dialog.findViewById(R.id.cb_newGame);
			d_cb_newGame.setOnClickListener(myViewListener);
			d_btn_ok = dialog.findViewById(R.id.btn_ok);
			d_btn_ok.setOnClickListener(myViewListener);


			return dialog;
		}

		if (id == PAUSE_DIALOG)
		{
			restartPauseDialog = false;
			isStopAutoPlay = false;
			if (ec.chessEnginePaused)
				isPaused = true;
			else
				isPaused = false;
			setTextViewColors(lblPlayerNameB, cv.COLOR_DATA_BACKGROUND_16, cv.COLOR_DATA_TEXT_17);
			Dialog dialog = new Dialog(this);
			dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			dialog.setContentView(R.layout.dialogpause);

			MyViewListener myViewListener = new MyViewListener();

			d_btn_timeWhite = dialog.findViewById(R.id.btn_timeWhite);
			d_btn_timeWhite.setOnClickListener(myViewListener);
			d_btn_timeBlack = dialog.findViewById(R.id.btn_timeBlack);
			d_btn_timeBlack.setOnClickListener(myViewListener);
			d_btn_timeControl = dialog.findViewById(R.id.btn_timeControl);
			d_btn_timeControl.setOnClickListener(myViewListener);
			d_btn_autom_play = dialog.findViewById(R.id.btn_autom_play);
			d_btn_autom_play.setOnClickListener(myViewListener);
			d_btn_delay = dialog.findViewById(R.id.btn_delay);
			d_btn_delay.setOnClickListener(myViewListener);
			d_play_mode_name = dialog.findViewById(R.id.play_mode_name);
			switch (pausePlayMod)
			{
				case 1:
					d_play_mode_name.setText(R.string.pause_white);
					break;
				case 2:
					d_play_mode_name.setText(R.string.pause_black);
					break;
				case 3:
					d_play_mode_name.setText(R.string.pause_engine);
					break;
				case 5:
					d_play_mode_name.setText(R.string.pause_two_players_flip);
					break;
				default:
					d_play_mode_name.setText(R.string.pause_white);
					break;
			}
			d_play_mode_name.setGravity(Gravity.CENTER);
			d_btn_newGame = dialog.findViewById(R.id.btn_newGame);
			d_btn_newGame.setOnClickListener(myViewListener);
			d_btn_setClock = dialog.findViewById(R.id.btn_setClock);
			d_btn_setClock.setOnClickListener(myViewListener);
			d_btn_continue = dialog.findViewById(R.id.btn_continue);
			d_btn_continue.setOnClickListener(myViewListener);

			d_btn_pause_edit = dialog.findViewById(R.id.btn_pause_edit);
			d_btn_pause_edit.setOnClickListener(myViewListener);
			d_btn_pause_analysis = dialog.findViewById(R.id.btn_pause_analysis);
			d_btn_pause_analysis.setOnClickListener(myViewListener);

			d_btn_pause_menu = dialog.findViewById(R.id.btn_pause_menu);
			d_btn_pause_menu.setOnClickListener(myViewListener);
			d_btn_pause_ok = dialog.findViewById(R.id.btn_pause_ok);
			d_btn_pause_ok.setOnClickListener(myViewListener);

			if (ec.chessEnginePlayMod != 4 & ec.chessEnginePlayMod != 6)
			{
				SharedPreferences.Editor ed = runP.edit();
				ed.putInt("run_timeControl_pause", tc.timeControl);
				ed.putInt("run_time_white_pause", tc.timeWhite);
				ed.putInt("run_bonus_white_pause", tc.bonusWhite);
				ed.putInt("run_time_black_pause", tc.timeBlack);
				ed.putInt("run_bonus_black_pause", tc.bonusBlack);
				ed.commit();
			}
			setPauseModValues(ec.chessEnginePlayMod, pausePlayMod, ec.chessEnginePaused, 3);

			return dialog;
		}

		if (id == RATE_DIALOG)
		{
			Dialog dialog = new Dialog(this);
			dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			dialog.setContentView(R.layout.dialograte);
			MyViewListener myViewListener = new MyViewListener();
			btn_rate = dialog.findViewById(R.id.btn_rate);
			setTextViewColors(btn_rate, cv.COLOR_DATA_BACKGROUND_16, cv.COLOR_DATA_TEXT_17);
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

		if (id == MENU_BOARD_DIALOG)
		{
			final int MENU_EDIT_BOARD     	= 0;
			final int MENU_CLIPBOARD    	= 1;
			final int MENU_COLOR		  	= 2;
			final int MENU_COORDINATES      = 3;
			final int MENU_FLIP_BOARD    	= 4;
			final int MENU_FILE  			= 5;
			ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.drawer_list_item);
			List<Integer> actions = new ArrayList<Integer>();
			arrayAdapter.add(getString(R.string.menu_edit_board));     				actions.add(MENU_EDIT_BOARD);
			arrayAdapter.add(getString(R.string.menu_board_clipboard)); 			actions.add(MENU_CLIPBOARD);
			arrayAdapter.add(getString(R.string.menu_board_color_settings)); 		actions.add(MENU_COLOR);
			arrayAdapter.add(getString(R.string.menu_board_coordinates)); 			actions.add(MENU_COORDINATES);
			arrayAdapter.add(getString(R.string.menu_board_flip)); 					actions.add(MENU_FLIP_BOARD);
			arrayAdapter.add(getString(R.string.fmLblFile)); 						actions.add(MENU_FILE);
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
						case MENU_CLIPBOARD:
							removeDialog(MENU_CLIPBOARD_DIALOG);
							showDialog(MENU_CLIPBOARD_DIALOG);
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
							startEditBoard(gc.fen, false);
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
							messageEngine = "";
							messageEngineShort  = "";
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
							messageEngine = "";
							messageEngineShort  = "";
							getFromClipboard();
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
			final int MENU_ENGINE_SETTINGS 		= 1;
			final int MENU_ENGINE_AUTOPLAY 		= 2;
			final int MENU_ENGINE_SHUTDOWN 		= 3;
			ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.drawer_list_item);
			List<Integer> actions = new ArrayList<Integer>();
			arrayAdapter.add(getString(R.string.menu_enginesettings_select));		actions.add(MENU_ENGINE_SELECT);
			arrayAdapter.add(getString(R.string.menu_enginesettings_playOptions));	actions.add(MENU_ENGINE_SETTINGS);
			arrayAdapter.add(getString(R.string.menu_specialities_engine_autoplay)); 									actions.add(MENU_ENGINE_AUTOPLAY);
			arrayAdapter.add(getString(R.string.menu_enginesettings_shutdown));   								actions.add(MENU_ENGINE_SHUTDOWN);
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
						case MENU_ENGINE_SELECT:
							stopThreads(true);
							internEngineName = "";
							removeDialog(MENU_SELECT_ENGINE_FROM_OEX);
							showDialog(MENU_SELECT_ENGINE_FROM_OEX);
							break;
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
							removeDialog(PLAY_DIALOG);
							showDialog(PLAY_DIALOG);
							break;
						case MENU_PAUSE_MODE_NEW_GAME:
							twoPlayerPaused = false;
							setPlayModPrefs(pause_mode);
							switch (pause_mode)
							{
								case 1:     // white
								case 2:     // black
								case 3:     // engine vs engine
									stopThreads(false);
									ec.chessEnginePaused = false;
									ec.chessEngineInit = false;
									msgEngine.setVisibility(TextView.GONE);
									messageInfo 		= "";
									messageEngine 		= "";
									messageEngineShort  = "";
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
			final int MENU_SETTINGS_COLOR			= 2;
			final int MENU_SETTINGS_TIME_CONTROL    = 3;
			final int MENU_SETTINGS_TIMER_AUTOPLAY	= 4;
			final int MENU_SETTINGS_TIME_WHITE 		= 5;
			final int MENU_SETTINGS_TIME_BLACK 		= 6;
			ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.drawer_list_item);
			List<Integer> actions = new ArrayList<Integer>();
			arrayAdapter.add(getString(R.string.menu_usersettings_gui));     			actions.add(MENU_SETTINGS_GUI);
			arrayAdapter.add(getString(R.string.menu_enginesettings_playOptions)); 		actions.add(MENU_SETTINGS_COMPUTER);
			arrayAdapter.add(getString(R.string.menu_colorsettings)); 					actions.add(MENU_SETTINGS_COLOR);
			arrayAdapter.add(getString(R.string.menu_usersettings_timeControl));   		actions.add(MENU_SETTINGS_TIME_CONTROL);
			arrayAdapter.add(getString(R.string.menu_usersettings_timerAutoPlay));     	actions.add(MENU_SETTINGS_TIMER_AUTOPLAY);
			if (ec.chessEnginePlayMod < 4)
			{
				arrayAdapter.add(getString(R.string.menu_usersettings_time_white)); 	actions.add(MENU_SETTINGS_TIME_WHITE);
				arrayAdapter.add(getString(R.string.menu_usersettings_time_black)); 	actions.add(MENU_SETTINGS_TIME_BLACK);
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
				@Override
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
						case MENU_SETTINGS_COLOR:
							removeDialog(MENU_COLOR_SETTINGS);
							showDialog(MENU_COLOR_SETTINGS);
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
			final int MENU_ABOUT_RATE     		= 1;
			final int MENU_ABOUT_APPS	    	= 4;
			final int MENU_ABOUT_WEBSITE    	= 5;
			final int MENU_ABOUT_SOURCECODE    	= 6;
			final int MENU_ABOUT_CONTACT    	= 7;
			ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.drawer_list_item);
			List<Integer> actions = new ArrayList<Integer>();
			arrayAdapter.add(getString(R.string.menu_about_new));     		actions.add(MENU_ABOUT_NEW);
			arrayAdapter.add(getString(R.string.rate_title));     		actions.add(MENU_ABOUT_RATE);
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

		if (id == MENU_SHOW_LIST)
		{
			final int MENU_SHOW_LIST_MOVES     	= 0;
			final int MENU_SHOW_LIST_ENGINE    	= 1;
			ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this, R.layout.drawer_list_item);
			List<Integer> actions = new ArrayList<Integer>();
			arrayAdapter.add(getString(R.string.colorMoveList));     				actions.add(MENU_SHOW_LIST_MOVES);
			arrayAdapter.add(getString(R.string.colorEngineList)); 					actions.add(MENU_SHOW_LIST_ENGINE);
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
							setInfoMessage("", null, "", false);
							break;
						case MENU_SHOW_LIST_ENGINE:
							if (userPrefs.getBoolean("user_options_enginePlay_EngineMessage", true))
							{
								ed.putBoolean("user_options_enginePlay_EngineMessage", false);
								ed.commit();
								setInfoMessage("", "", null, false);
							}
							else
							{
								ed.putBoolean("user_options_enginePlay_EngineMessage", true);
								ed.commit();
								setInfoMessage("", null, null, false);
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

//			String natDir = getApplicationInfo().nativeLibraryDir;

			final ArrayList<String> items = new ArrayList<>();
//			items.add(ec.en_1.assetsEngineProcessName);
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
						Log.e(TAG, e.getLocalizedMessage(), e);
					}
				}
			} catch (XmlPullParserException e) {
				Log.e(TAG, e.getLocalizedMessage(), e);
//				items.add(ec.en_1.assetsEngineProcessName);
			}

//			Log.i(TAG, "MENU_SELECT_ENGINE_FROM_OEX, natDir: " + natDir);

			if (storageAvailable()) {
				ChessEngineResolver resolver = new ChessEngineResolver(this);

//				Log.i(TAG, "MENU_SELECT_ENGINE_FROM_OEX, resolver.target: " + resolver.target);

				List<com.kalab.chess.enginesupport.ChessEngine> engines = resolver.resolveEngines();
				ArrayList<android.util.Pair<String,String>> oexEngines = new ArrayList<>();
				for (ChessEngine engine : engines) {
					if ((engine.getName() != null) && (engine.getFileName() != null) &&
							(engine.getPackageName() != null)) {
						oexEngines.add(new android.util.Pair<>(FileIO.openExchangeFileName(engine),
								engine.getName()));

Log.i(TAG, "MENU_SELECT_ENGINE_FROM_OEX,  engine.getEnginePath(): " + engine.getEnginePath());

					}
				}
				Collections.sort(oexEngines, (lhs, rhs) -> lhs.second.compareTo(rhs.second));
				for (android.util.Pair<String,String> eng : oexEngines) {
					items.add(eng.second);
				}

				String[] fileNames = FileIO.findFilesInDirectory(engineDir,
						fname -> !reservedEngineName(fname));
				for (String file : fileNames) {
					items.add(file);
				}
			}
			else {

//				Log.i(TAG, "MENU_SELECT_ENGINE_FROM_OEX, storageAvailable: false");

			}

			String currEngine = runP.getString("run_engineProcess", "");

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

						boolean engineIsReady = restartEngine();

						//iii
						// 1 engineIsReady = restartEngine(items.get(item));
						// 2 if (engineIsReady) --> edR.putString("run_engineProcess", items.get(item)); else ?

//						Log.i(TAG, "MENU_SELECT_ENGINE_FROM_OEX, engine: " + items.get(item) + ", engineIsReady: " + engineIsReady);

//						if (engineIsReady & !ec.chessEnginePausedPrev)
						if (engineIsReady)
							startPlay(false, false);

					});
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
//Log.i(TAG, "getCallbackValue(), btnValue: " + btnValue);
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
				Intent send = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", APP_EMAIL.toString(), null));
				send.putExtra(android.content.Intent.EXTRA_SUBJECT, gc.errorMessage);
				send.putExtra(android.content.Intent.EXTRA_TEXT, gc.errorPGN + c4aVersion + androidVersion + deviceName);
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
					tc.setCurrentShowValues(ec.chessEnginePlayMod);
					updateCurrentPosition("");
					if (restartPauseDialog)
					{
						pauseStopPlay(false);
						removeDialog(PAUSE_DIALOG);
						showDialog(PAUSE_DIALOG);
					}
				}
			}
		}
	}

	public void setPlayModBackground(int playmod)
	{
        d_btn_white.setBackgroundResource(R.drawable.rectanglewhite);
        d_btn_black.setBackgroundResource(R.drawable.rectanglewhite);
        d_btn_engine.setBackgroundResource(R.drawable.rectanglewhite);
        d_btn_player.setBackgroundResource(R.drawable.rectanglewhite);
        d_btn_edit.setBackgroundResource(R.drawable.rectanglewhite);
        d_btn_analysis.setBackgroundResource(R.drawable.rectanglewhite);
		setTextViewColors(lblPlayerNameB, cv.COLOR_DATA_BACKGROUND_16, cv.COLOR_DATA_TEXT_17);
		switch (playmod)
		{
			case 1:
                d_btn_white.setBackgroundResource(R.drawable.dialoggreen);
				break;
			case 2:
                d_btn_black.setBackgroundResource(R.drawable.dialoggreen);
				break;
			case 3:
                d_btn_engine.setBackgroundResource(R.drawable.dialoggreen);
				break;
			case 4:
                d_btn_analysis.setBackgroundResource(R.drawable.dialoggreen);
				break;
			case 5:
                d_btn_player.setBackgroundResource(R.drawable.dialoggreen);
				break;
			case 6:
                d_btn_edit.setBackgroundResource(R.drawable.dialoggreen);
				break;
		}
	}

	public void setPauseModValues(int playMod, int pauseMod, boolean enginePaused, int continueId)
	{
//Log.i(TAG, "setPauseModValues(), playMod: " + playMod + ", pauseMod: " + pauseMod + ", enginePaused: " + enginePaused + ", continueId: " + continueId);
		if (enginePaused)
		{
			isSearching = false;
			d_btn_pause_ok.setText(R.string.pause_start);
		}
		else
		{
			isSearching = true;
			d_btn_pause_ok.setText(R.string.pause_stop);
		}
		dChessEnginePlayMod = pauseMod;
		if (playMod == 4 | playMod == 6)
			dChessEnginePlayMod = playMod;
		d_btn_newGame.setBackgroundResource(R.drawable.rectanglewhite);
		d_btn_newGame.setText(getString(R.string.play_newGame) + getTimeValues(pauseMod, 1));
		d_btn_setClock.setBackgroundResource(R.drawable.rectanglewhite);
		d_btn_setClock.setText(getString(R.string.play_set_clock) + getTimeValues(pauseMod, 2));
		d_btn_continue.setBackgroundResource(R.drawable.rectanglewhite);
		d_btn_continue.setText(getString(R.string.play_continue) + getTimeValues(pauseMod, 3));
		d_btn_pause_edit.setBackgroundResource(R.drawable.rectanglewhite);
		d_btn_pause_analysis.setBackgroundResource(R.drawable.rectanglewhite);
		d_play_mode_name.setTextColor(getResources().getColor(R.color.text_light));
		d_btn_autom_play.setBackgroundResource(R.drawable.rectanglewhite);
		setTextViewColors(lblPlayerNameB, cv.COLOR_DATA_BACKGROUND_16, cv.COLOR_DATA_TEXT_17);
		d_btn_pause_ok.setBackgroundResource(R.drawable.dialoggreen);
		if (dChessEnginePlayMod != 4 & dChessEnginePlayMod != 6)
		{
			d_btn_timeWhite.setText(getString(R.string.menu_usersettings_time_white) + "\n" + tc.getShowValues((tc.timeWhite / 1000) * 1000, false));
			d_btn_timeBlack.setText(getString(R.string.menu_usersettings_time_black) + "\n" + tc.getShowValues((tc.timeBlack / 1000) * 1000, false));
			String timeControl = "";
			switch (userPrefs.getInt("user_options_timeControl", 1))
			{
				case 1:
					timeControl = getString(R.string.tcGameClock);
					break;
				case 2:
					timeControl = getString(R.string.tcMoveTime);
					break;
				case 3:
					timeControl = getString(R.string.tcSandGlass);
					break;
				case 4:
					timeControl = getString(R.string.tcNone);
					break;
			}
			d_btn_timeControl.setText(getString(R.string.menu_usersettings_timeControl) + "\n" + timeControl);
		}
		else
		{
			d_btn_timeWhite.setText(getString(R.string.menu_usersettings_time_white) + "\n");
			d_btn_timeBlack.setText(getString(R.string.menu_usersettings_time_black) + "\n");
			d_btn_timeControl.setText(getString(R.string.menu_usersettings_timeControl) + "\n");
		}
		d_btn_autom_play.setText(getString(R.string.menu_specialities_engine_autoplay) + "\n");
		d_btn_delay.setText(getString(R.string.pause_delay) + "\n" + tc.getShowValues(userPrefs.getInt("user_options_timer_autoPlay", 1500), true));
		if (gc.isAutoPlay)
		{
			d_btn_autom_play.setBackgroundResource(R.drawable.dialoggreen);
			dChessEnginePlayMod = 6;
		}
		if (dChessEnginePlayMod == 4)
		{
			d_btn_pause_analysis.setBackgroundResource(R.drawable.dialoggreen);
			d_play_mode_name.setTextColor(getResources().getColor(R.color.text_white));
		}
		if (dChessEnginePlayMod == 6)
		{
			d_btn_pause_edit.setBackgroundResource(R.drawable.dialoggreen);
			d_play_mode_name.setTextColor(getResources().getColor(R.color.text_white));
		}
		if (!isSearching)
		{
			if (dChessEnginePlayMod == pauseMod)
			{
				switch (continueId)
				{
					case 1:
						d_btn_newGame.setBackgroundResource(R.drawable.dialoggreen);
						break;
					case 2:
						d_btn_setClock.setBackgroundResource(R.drawable.dialoggreen);
						break;
					case 3:
						d_btn_continue.setBackgroundResource(R.drawable.dialoggreen);
						break;
				}
			}
		}
	}

	public void setTimeValuesFromPauseMod(int pauseMod, int continueId)
	{
		int tPlayer = 0;
		int tEngine = 0;
		switch (userPrefs.getInt("user_options_timeControl", 1))
		{
			case 1:
				tPlayer = userPrefs.getInt("user_time_player_clock", 300000) + userPrefs.getInt("user_bonus_player_clock", 3000);
				tEngine = userPrefs.getInt("user_time_engine_clock", 60000) + userPrefs.getInt("user_bonus_engine_clock", 3000);
				break;
			case 2:
				tPlayer = userPrefs.getInt("user_time_player_move", 10000);
				tEngine = userPrefs.getInt("user_time_engine_move", 3000);
				break;
			case 3:
				tPlayer = userPrefs.getInt("user_time_player_sand", 600000);
				tEngine = userPrefs.getInt("user_time_engine_sand", 10000);
				break;
		}
		switch (continueId)
		{
			case 1:		// new game
			case 2:		// set clock
				switch (pauseMod)
				{
					case 1:
						tc.timeWhite = 	tPlayer;
						tc.timeBlack = 	tEngine;
						break;
					case 2:
						tc.timeWhite = 	tEngine;
						tc.timeBlack = 	tPlayer;
						break;
					case 3:
						tc.timeWhite = 	tEngine;
						tc.timeBlack = 	tEngine;
						break;
					case 5:
						tc.timeWhite = 	tPlayer;
						tc.timeBlack = 	tPlayer;
						break;
				}
				break;
			case 3:		// continue
				tc.timeWhite = 	runP.getInt("run_time_white_pause", 300000);
				tc.timeBlack = 	runP.getInt("run_time_black_pause", 300000);
				break;
		}
	}

	public String getTimeValues(int playMod,  int continueId)
	{
		int tPlayer = 0;
		int tEngine = 0;
		switch (userPrefs.getInt("user_options_timeControl", 1))
		{
			case 1:
				tPlayer = userPrefs.getInt("user_time_player_clock", 300000) + userPrefs.getInt("user_bonus_player_clock", 3000);
				tEngine = userPrefs.getInt("user_time_engine_clock", 60000) + userPrefs.getInt("user_bonus_engine_clock", 3000);
				break;
			case 2:
				tPlayer = userPrefs.getInt("user_time_player_move", 10000);
				tEngine = userPrefs.getInt("user_time_engine_move", 3000);
				break;
			case 3:
				tPlayer = userPrefs.getInt("user_time_player_sand", 600000);
				tEngine = userPrefs.getInt("user_time_engine_sand", 10000);
				break;
		}

		String timePlayer = 	tc.getShowValues((tPlayer / 1000) * 1000, false);
		String timeEngine = 	tc.getShowValues((tEngine / 1000) * 1000, false);
		String timeWhite = 	tc.getShowValues((runP.getInt("run_time_white_pause", 300000) / 1000) * 1000, false);
		String timeBlack = 	tc.getShowValues((runP.getInt("run_time_black_pause", 300000) / 1000) * 1000, false);

//Log.i(TAG, "getTimeValues(), timePlayer: " + timePlayer + ", timeEngine: " + timeEngine + ", continueId: " + continueId);
		switch (continueId)
		{
			case 1:		// new game
			case 2:		// set clock
				switch (playMod)
				{
					case 1:
						return  "\n" + timePlayer + " | " + timeEngine;
					case 2:
						return  "\n" + timeEngine + " | " + timePlayer;
					case 3:
						return  "\n" + timeEngine + " | " + timeEngine;
					case 5:
						return  "\n" + timePlayer + " | " + timePlayer;
					case 4:
					case 6:
						return "";
				}
				break;
			case 3:		// continue
				return  "\n" + timeWhite + " | " + timeBlack;
		}
		return "";
	}

//	USER-ACTIONS		USER-ACTIONS		USER-ACTIONS		USER-ACTIONS
//	TOUCH, CLICK		TOUCH, CLICK		TOUCH, CLICK		TOUCH, CLICK
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

			if (view.getId() == R.id.btn_4)
				isDownBtn4 = true;
			else
				isDownBtn4 = false;

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
//Log.i(TAG, "onTouch(), MotionEvent.ACTION_UP, id: " + view.getId());
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
//Log.i(TAG, "onTouch(), isUpBtn: " + isUpBtn + ", isUpMsgView: " + isUpMsgView);
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
						stopThreads(false);
						startPlay(false, true);
					}
				}
//    Log.i(TAG, "onTouch(), isDownBtn: " + isDownBtn + ", isUpBtn: " + isUpBtn);
				if (isDownBtn & isUpBtn)
				{
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
		if (view.getId() == R.id.btn_1  | view.getId() == R.id.btn_3 | view.getId() == R.id.btn_4)
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
				cancelEngineMessage();
				startBoardMoveAction(event);
				break;
			case R.id.btn_1:    // play mode - new game
				isStopAutoPlay = false;
				removeDialog(PLAY_DIALOG);
				showDialog(PLAY_DIALOG);
				break;
			case R.id.btn_2:    // pause game | start PAUSE_DIALOG
				if (gc.isAutoPlay)
				{
					startStopAutoPlay();
					isStopAutoPlay = true;
				}
				else
				{
					if (ec.chessEnginePaused)
					{
						if (ec.chessEnginePlayMod == 5 & tc.clockIsRunning)    // two players
						{
							tc.stopChessClock(System.currentTimeMillis(), ec.chessEnginePlayMod);
							twoPlayerPaused = true;
							setInfoMessage(getString(R.string.clock_stopped), null, null, false);
						}
						else
						{
							removeDialog(PAUSE_DIALOG);
							showDialog(PAUSE_DIALOG);
						}
					} else
						pauseStopPlay(false);
				}
				break;
			case R.id.btn_3:
				// ec.chessEnginePaused:  start searching
				// !ec.chessEnginePaused: force computer move and continue searching ( startForceComputerMove() )
				if (isStopAutoPlay)
				{
					startStopAutoPlay();
					isStopAutoPlay = false;
				}
				else
				{
					boolean isPlayer = isPlayerMove(ec.chessEnginePlayMod, gc.getValueFromFen(gc.fen, 2));
//Log.i(TAG, "1 onTouchAction(), mod: " + ec.chessEnginePlayMod + ", paused: " + ec.chessEnginePaused + ", isPlayer: " + isPlayer);
					ec.setStartPlay(gc.getValueFromFen(gc.fen, 2));
					if (ec.chessEnginePaused)
					{
						if (isPlayer & !tc.clockIsRunning)
							playSound(1, 0);
						setPlayModPrefs(ec.chessEnginePlayMod);
						ec.chessEnginePaused = false;
						ec.chessEngineInit = false;
						updateCurrentPosition("");
						startPlay(false, false);
					}
					else
					{
						if (isPlayer & !ec.chessEngineSearchingPonder)
						{
							if (tc.clockIsRunning)
							{
								removeDialog(PAUSE_DIALOG);
								showDialog(PAUSE_DIALOG);
							}
							else
								setInfoMessage(getString(R.string.player_move), null, null, false);
						}
						else
							startForceComputerMove();
					}
				}
				break;
			case R.id.btn_4:    // delete move(s)
				isStopAutoPlay = false;
				cancelEngineMessage();
				deleteMoves(true);
				break;
			case R.id.btn_5:    // turn board
				startTurnBoard();
				break;
			case R.id.btn_6:    // move back
				cancelEngineMessage();
				nextMove(1, 0);
				if (ec.chessEngineSearchingPonder)
					engineStopPonder(gc.cl.p_fen, ec.chessEnginePlayMod);
				else
				{
					if (!ec.chessEnginePaused)
					{
						stopThreads(false);
						startPlay(false, true);
					}
				}
				break;
			case R.id.btn_7:    // next move
				cancelEngineMessage();
				nextMove(2, 0);
				if (ec.chessEngineSearchingPonder)
					engineStopPonder(gc.cl.p_fen, ec.chessEnginePlayMod);
				else
				{
					if (!ec.chessEnginePaused)
					{
						stopThreads(false);
						startPlay(false, true);
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
		if (ec.chessEngineSearchingPonder)
		{
			setPauseEnginePlay(false);
			initPonder();
		}
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
				cancelEngineMessage();
				gc.isGameOver = false;
				nextMove(3, 0);
				if (!ec.chessEnginePaused)
				{
					stopThreads(false);
					startPlay(false, true);
				}
				break;
			case R.id.btn_7:    // last move
				cancelEngineMessage();
				if (startVariation())
					c4aShowDialog(VARIATION_DIALOG);
				else
				{
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

//Log.i(TAG, "onActivityResult, requestCode: " + requestCode + ", " + "resultCode: " + resultCode);
//Log.i(TAG, "onActivityResult, user_play_playMod: " + requestCode + ", " + userPrefs.getInt("user_play_playMod", 1));
//Log.i(TAG, "onActivityResult, data: " + data);

		updateCurrentPosition("");
		SharedPreferences.Editor ed = userPrefs.edit();
		boolean isNewGame = false;
		if (data != null)
			isNewGame = data.getBooleanExtra("newGame", false);
		else
		{
			if (requestCode == OPTIONS_PLAY_REQUEST_CODE)
				isNewGame = false;
			else
					return;
		}

		if (resultCode > 0)
			stopThreads(false);

		switch(requestCode)
		{
			case OPTIONS_PLAY_REQUEST_CODE:
			case OPTIONS_ENGINE_PLAY_REQUEST_CODE:
				if (resultCode == 3)
				{	// set playOption and play
					gc.isGameLoaded = false;
					msgEngine.setVisibility(TextView.GONE);
					messageInfo 		= "";
					messageEngine 		= "";
					messageEngineShort  = "";
					ec.chessEngineAutoRun = false;
					twoPlayerPaused = false;
					if (requestCode == OPTIONS_ENGINE_PLAY_REQUEST_CODE)
					{
						ec.getEngine().isLogOn = userPrefs.getBoolean("user_options_enginePlay_logOn", false);
						ec.setBookOptions();
						msgEngine.setMaxLines(userPrefs.getInt("user_options_enginePlay_displayedLines", OptionsEnginePlay.DISPLAYED_LINES));
						msgEngine.setLines(userPrefs.getInt("user_options_enginePlay_displayedLines", OptionsEnginePlay.DISPLAYED_LINES));
					}
					ec.chessEnginePlayMod = userPrefs.getInt("user_play_playMod", 1);
					pausePlayMod = userPrefs.getInt("user_pause_playMod", 1);
					if (ec.chessEnginePlayMod == 4)
						ec.initClockAfterAnalysis = true;
					switch (userPrefs.getInt("user_play_playMod", 1))
					{
						case 1:     // white
						case 2:     // black
						case 3:     // engine vs engine
							if (requestCode == OPTIONS_PLAY_REQUEST_CODE | requestCode == OPTIONS_ENGINE_PLAY_REQUEST_CODE)
							{
								ec.chessEnginePaused = false;
								ec.chessEngineInit = false;
							}
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
				if (resultCode == RESULT_OK)
				{
					gc.isGameLoaded = false;
					stopTimeHandler(false);
					ec.chessEngineAutoRun = true;
					ec.chessEnginePaused = false;
					setPlayModPrefs(3);
					cntResult = 0;
					initChessClock();
					gc.startPgn = gc.cl.history.createPgnFromHistory(1);
					gc.startMoveIdx = gc.cl.history.getMoveIdx();
					if 	(!userPrefs.getBoolean("user_play_eve_autoCurrentGame", false))
						startPlay(true, true);
					else
					{
						setRunMoveHistory();
						setTagDate();	// current date
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
				if (resultCode == RESULT_OK)
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
                    setInfoMessage("", "", "", true);
					ec.chessEnginePaused = true;
					gc.isGameLoaded = true;
					setInfoMessage("", null, null, false);
				}
				break;
//			case LOAD_EXTERN_ENGINE_REQUEST_CODE:
//				if (resultCode == LOAD_INTERN_ENGINE_REQUEST_CODE)
//				{
//					internEngineName = data.getStringExtra("fileName");
//					startFileManager(LOAD_INTERN_ENGINE_REQUEST_CODE, 1, 0);
//				}
//				break;
//			case LOAD_INTERN_ENGINE_REQUEST_CODE:
//				if (resultCode == RESULT_OK)
//				{
//					String newEngine = data.getStringExtra("fileName");
//					if (!newEngine.equals("") & !data.getStringExtra("fileName").equals(ec.getEngine().engineProcess))
//					{
//
////Log.i(TAG, "onActivityResult(), current engineProcess: " + ec.getEngine().engineProcess + ", new engineProcess: " + data.getStringExtra("fileName"));
//
//						Toast.makeText(this, getString(R.string.engine_new) + " " + newEngine, Toast.LENGTH_SHORT).show();
//						SharedPreferences.Editor edR = runP.edit();
//						edR.putString("run_engineProcess", newEngine);
//						edR.commit();
//
//						boolean engineIsReady = restartEngine();
//						if (engineIsReady & !ec.chessEnginePausedPrev)
//							startPlay(false, false);
//					}
//				}
//				break;
			case GAME_DATA_REQUEST_CODE:
				if (resultCode == RESULT_OK)
					gc.cl.history.setNewGameTags(data.getCharSequenceExtra("gameTags").toString());
				setInfoMessage("", "", "", true);
				break;
			case NOTATION_REQUEST_CODE:
				setInfoMessage("", "", "", true);
				updateCurrentPosition("");
				break;
			case OPTIONS_COLOR_SETTINGS:
                initColors();
				boardView.setColor();
				updateGui();
				break;
			case OPTIONS_CHESSBOARD_REQUEST_CODE:
			case OPTIONS_GUI_REQUEST_CODE:
				if (requestCode == OPTIONS_GUI_REQUEST_CODE)
				{
					useWakeLock = !userPrefs.getBoolean("user_options_gui_disableScreenTimeout", false);
					setWakeLock(useWakeLock);
					setPieceName(userPrefs.getInt("user_options_gui_PieceNameId", 0));
					setInfoMessage("", "", "", true);
				}
				u.updateFullscreenStatus(this, userPrefs.getBoolean("user_options_gui_StatusBar", false));
				updateCurrentPosition("");
				break;
			case OPTIONS_TIME_CONTROL_REQUEST_CODE:
				if (resultCode == 101)	// apply
				{
					ec.chessEnginePaused = false;
					ec.chessEngineInit = false;
					initChessClock();
					if (restartPauseDialog)
					{
						pauseStopPlay(false);
						removeDialog(PAUSE_DIALOG);
						showDialog(PAUSE_DIALOG);
					}
					else
						startPlay(false, true);
				}
				break;
			case EDIT_CHESSBOARD_REQUEST_CODE:

//				Log.i(TAG, "onActivityResult, EDIT_CHESSBOARD_REQUEST_CODE" + ", resultCode: " + resultCode);

				if (resultCode == RESULT_OK)
				{
                    gc.isBoardTurn = runP.getBoolean("run_game0_is_board_turn", false);
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
						setInfoMessage("", "", "", false);
						updateGui();
						if (ec.chessEnginePlayMod == 3 | ec.chessEnginePlayMod == 4)
							startPlay(false, true);
					}
				}
				break;
			case MOVETEXT_REQUEST_CODE:
				if (resultCode == RESULT_OK)
				{
					gc.isGameUpdated = false;
					gc.cl.history.setMoveText(data.getStringExtra("text"));
					updateCurrentPosition("");
				}
				break;
			case SAVE_GAME_REQUEST_CODE:
				if (resultCode == RESULT_OK)
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
				if (resultCode == RESULT_OK)
				{
					setChess960IdPref(chess960.createChessPosition(data.getStringExtra("chess960BaseLine")));
				}
				break;
			case ENGINE_SETTING_REQUEST_CODE:
				ec.chessEnginePaused = true;
				updateCurrentPosition("");
				setInfoMessage(getEnginePausedMessage(), null, null, false);
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
//		if (Build.VERSION.SDK_INT >= 23)
//		{
//			String[] permissions = new String[]
//					{Manifest.permission.INTERNET, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.WAKE_LOCK};
//			ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST_CODE);
//		}

		if (storagePermission == PermissionState.UNKNOWN) {
			String extStorage = Manifest.permission.WRITE_EXTERNAL_STORAGE;
			if (ContextCompat.checkSelfPermission(this, extStorage) ==
					PackageManager.PERMISSION_GRANTED) {
				storagePermission = PermissionState.GRANTED;
			} else {
				storagePermission = PermissionState.REQUESTED;
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

	}

	//	PREFERENCES		PREFERENCES		PREFERENCES		PREFERENCES		PREFERENCES
	public void setRunPrefs()
	{
    	SharedPreferences.Editor ed = runP.edit();

//		Log.i(TAG, "setRunPrefs(), current engineProcess: " + ec.getEngine().engineProcess);

    	if (!ec.getEngine().engineProcess.equals(""))
			ed.putString("run_engineProcess", ec.getEngine().engineProcess);
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
		ed.putBoolean("run_chessEngineAutoRun", ec.chessEngineAutoRun);
		ed.putBoolean("run_twoPlayerPaused", twoPlayerPaused);
		ed.putString("run_selectedVariationTitle", (String) gc.selectedVariationTitle);

		ed.commit();

		setRunPrefsTime();

	}

	public void setRunPrefsTime()
	{
//Log.i(TAG, "setRunPrefsTime(), tc.timeControl: " + tc.timeControl + ", tc.timeWhite: " + tc.timeWhite + ", tc.timeBlack: " + tc.timeBlack);
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
            ed.putString("run_engineProcess", "");
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
        ec.chessEngineAutoRun = runP.getBoolean("run_chessEngineAutoRun", false);
		twoPlayerPaused = runP.getBoolean("run_twoPlayerPaused", false);
		ec.chessEnginePlayMod = userPrefs.getInt("user_play_playMod", 1);
		pausePlayMod = userPrefs.getInt("user_pause_playMod", 1);
		if (ec.chessEnginePlayMod == 4)
			ec.initClockAfterAnalysis = true;
        gc.selectedVariationTitle = runP.getString("run_selectedVariationTitle", "");

		getRunPrefsTime();
	}

	public void getRunPrefsTime()
	{
		tc.timeControl = runP.getInt("run_timeControl", 1);
//Log.i(TAG, "getRunPrefsTime(), tc.timeControl: " + tc.timeControl);
		tc.timeWhite = runP.getInt("run_timeWhite", 300000);
		tc.timeBlack = runP.getInt("run_timeBlack", 60000);
		tc.movesToGo = runP.getInt("run_movesToGo", 0);
		tc.bonusWhite = runP.getInt("run_bonusWhite", 0);
		tc.bonusBlack = runP.getInt("run_bonusBlack", 0);
		tc.initChessClock(tc.timeControl, tc.timeWhite, tc.timeBlack, tc.movesToGo, tc.bonusWhite, tc.bonusBlack);
		tc.setCurrentShowValues(ec.chessEnginePlayMod);
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
//Log.i(TAG, "setPauseEnginePlay, ec.chessEnginePaused: " + ec.chessEnginePaused);
		ec.chessEnginePausedPrev = ec.chessEnginePaused;
		if (shutDown)
			stopAllEngines(isAppEnd);
		else
		{
			stopComputerThinking(false);
			ec.chessEngineSearchingPonder = false;
			ec.chessEnginePaused = true;
			if (!gc.isGameOver & !gc.cl.p_variationEnd)
				setInfoMessage(getEnginePausedMessage(), null, null, false);
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
//Log.i(TAG, "2 pauseEnginePlay, ec.chessEnginePaused: " + ec.chessEnginePaused + ", engineAction: " + engineAction);
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
	}

	public final synchronized void stopComputerThinking(boolean shutDown)
	{
//Log.i(TAG, "stopComputerThinking, processAlive: " + ec.getEngine().processAlive + ", shutDown: " + shutDown + ", ec.chessEngineIsInSearchTask: " + ec.chessEngineIsInSearchTask);
		ec.chessEngineStopSearch = true;
		if (shutDown)
		{
			ec.getEngine().shutDown();
			return;
		}

		if (!ec.chessEngineIsInSearchTask)
		{
			ec.chessEngineStopSearch = false;
			return;
		}

		if (chessEngineSearchTask != null)
		{
			chessEngineSearchTask.cancel(true);
			try {Thread.sleep(sleepTime);}
			catch (InterruptedException e) {}
		}

		boolean isStop = false;
		if (ec.getEngine().process == null)
			isStop = true;
		else
		{
			if (ec.getEngine().isError())
				isStop = true;
		}
		if (isStop)
		{
//Log.i(TAG, "stopComputerThinking, getEngine().isError()");
            if (ec.getEngine().process != null)
			    ec.getEngine().process.destroy();
			stopChessClock();
			ec.chessEngineSearching = false;
			ec.chessEnginePaused = true;
			ec.chessEngineInit = true;
			ec.getEngine().processAlive = false;
			return;
		}

		try
		{
			if (ec.getEngine().processAlive)
			{
				if (!ec.getEngine().syncStopSearch(false))
				{
//Log.i(TAG, "stopComputerThinking, !getEngine().syncStopSearch(): false");
				}
			}
		}
		catch (NullPointerException e) {e.printStackTrace();}
	}

	public void stopAllEngines(boolean isAppEnd)
	{	//>381 shutdownEngine() and releaseEngineService()
		ec.setEngineNumber(1);
		if (ec.getEngine() != null)
			stopComputerThinking(true);
		if (!isAppEnd)
			ec.chessEnginePaused = true;
	}


	public void nextGameEngineAutoPlay()
	{
		gc.isGameOver = false;
		gc.isGameUpdated = true;
		ec.chessEnginePaused = false;

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
		ec.setPlaySettings(userPrefs, gc.cl.p_color);
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

	public void setChess960IdPref(int chess960Id)
	{	// if isLastGame set old game preferences
		SharedPreferences.Editor ed = userPrefs.edit();
		ed.putInt("user_game_chess960Id", chess960Id);
		ed.commit();
	}

	public void setPlayModPrefs(int playMod)
	{
		int oldPlayMod = userPrefs.getInt("user_play_playMod", 1);
		if ((oldPlayMod != 4 & oldPlayMod != 6) & (playMod == 4 | playMod == 6))
		{
//Log.i(TAG, "setPlayModPrefs(), tc.timeControl: " + tc.timeControl + ", tc.timeWhite: " + tc.timeWhite);
			SharedPreferences.Editor ed = runP.edit();
			ed.putInt("run_timeControl_pause", tc.timeControl);
			ed.putInt("run_time_white_pause", tc.timeWhite);
			ed.putInt("run_bonus_white_pause", tc.bonusWhite);
			ed.putInt("run_time_black_pause", tc.timeBlack);
			ed.putInt("run_bonus_black_pause", tc.bonusBlack);
			ed.commit();
		}
		ec.chessEnginePlayMod = playMod;
		if (ec.chessEnginePlayMod != 4 & ec.chessEnginePlayMod != 6)
			pausePlayMod = ec.chessEnginePlayMod;
		SharedPreferences.Editor ed = userPrefs.edit();
		ed.putInt("user_play_playMod", playMod);
		ed.putInt("user_pause_playMod", pausePlayMod);
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
		gc.cl.deleteMovesFromMoveHistory(deleteMoveIdx);
		if (gc.cl.p_stat.equals("1"))
		{
			gc.move = "";
			gc.isGameOver = false;
			gc.cl.p_hasPossibleMoves = false;
			updateGui();
		}
		if (ec.chessEngineSearchingPonder)
			engineStopPonder(gc.cl.p_fen, ec.chessEnginePlayMod);
		else
		{
			if (!ec.chessEnginePaused)
			{
				stopThreads(false);
				startPlay(false, true);
			}
			if (ec.chessEnginePaused)
				setInfoMessage(getString(R.string.engine_paused), "", null, false);
		}
	}

	public void startEdit(boolean isNewGame, boolean setClock)
	{
//Log.i(TAG, "startEdit(), isNewGame: " + isNewGame + ", setClock: " + setClock);
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

//Log.i(TAG, "startPlay(), isNewGame: " + isNewGame + ", setClock: " + setClock);

		ec.chessEngineSearching = false;
		ec.chessEnginePaused = false;
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
				setInfoMessage(gc.errorMessage, null, null, false);
				return;
			}
		}
		analysisMessage = "";
		if (isNewGame | (!gc.cl.p_mate & !gc.cl.p_stalemate & !gc.cl.p_auto_draw))	// !mate, steal mate, auto draw?
		{
			gc.isGameOver = false;
			gc.isGameUpdated = true;
			ec.chessEnginePlayMod = userPrefs.getInt("user_play_playMod", 1);
			pausePlayMod = userPrefs.getInt("user_pause_playMod", 1);
			if (ec.chessEnginePlayMod == 1)
				gc.isBoardTurn = false;
			if (ec.chessEnginePlayMod == 2)
				gc.isBoardTurn = true;
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
			initInfoArrays(false);
//Log.i(TAG, "startPlay(), isAppStart: " + isAppStart);
			if (!isAppStart)
			{
				ec.initClockAfterAnalysis = false;
				if (isNewGame | setClock)
					initChessClock();
			}
			if (ec.chessEnginePlayMod <= 5)
			{
				if (!(gc.fen.equals(gc.startFen) & (ec.chessEnginePlayMod == 1 | ec.chessEnginePlayMod == 5)))
					startChessClock();
			}
			if (ec.chessEnginePlayMod <= 4)
				startEnginePlay(isNewGame);
			else
				setInfoMessage(getEnginePausedMessage(), null, null, false);
		}
		else
			setInfoMessage(getGameOverMessage(), null, null, false);
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

	public void startEnginePlay(boolean newGame)
	{	//setting play options and start engine play
		ec.setPlaySettings(userPrefs, gc.cl.p_color);
//Log.i(TAG, "startEnginePlay(), gc.fen: " + gc.fen);
		ec.setStartPlay(gc.getValueFromFen(gc.fen, 2));
		boolean	isReady = false;
//Log.i(TAG, "1 startEnginePlay(), ec.getEngine().process: " + ec.getEngine().process);
		if (ec.getEngine().process != null)
			isReady = ec.getEngine().syncReady();
//Log.i(TAG, "startEnginePlay(), isReady : " + isReady);
		if (!isReady)
		{
			boolean isError = false;
			if (ec.getEngine().process != null)
			{
				isError = ec.getEngine().isError();
			}

			if (ec.getEngine().process == null | isError)
			{
//Log.i(TAG, "2 startEnginePlay(), restartEngine(), isError: " + isError);
				if (!restartEngine())
					return;
			}

		}
		startEnginePlayIsReady(newGame);
	}

	public void startEnginePlayIsReady(boolean newGame)
	{
//Log.i(TAG, "startEnginePlayIsReady(), newGame: " + newGame);
		if (newGame)
		{
			engineMes = "";
			engineStat = "";
			initInfoArrays(false);
		}

		ec.chessEnginePaused = false;
		ec.chessEngineInit = false;
		ec.setPlayData(userPrefs, gc.cl.history.getGameTagValue("White"), gc.cl.history.getGameTagValue("Black"));
		setTagGameData();

		boolean isError = ec.getEngine().isError();
//Log.i(TAG, "startEnginePlayIsReady(), isError: " + isError);
		if (!isError)
		{
			if (!startNewGame(ec.getEngine().engineNumber))
			{
				stopChessClock();
				ec.chessEngineSearching = false;
				ec.chessEnginePaused = true;
				ec.chessEngineInit = true;
				setInfoMessage(getString(R.string.engine_noRespond) + " (2)", null, null, false);

				return;
			}
		}
		else
		{
			stopChessClock();
			ec.chessEngineSearching = false;
			ec.chessEnginePaused = true;
			ec.chessEngineInit = true;
			setInfoMessage(getString(R.string.engine_noRespond) + " (3)", null, null, false);
			return;
		}

//Log.i(TAG, "startEnginePlayIsReady(), ec.makeMove: " + ec.makeMove);
		if (ec.makeMove)
		{
			ec.chessEngineSearching = true;
			if (!ec.chessEngineAutoRun)
				setInfoMessage(getEngineThinkingMessage(), null, null, false);
			isGoPonder = false;
			if (!gc.cl.p_fen.equals(""))
				chessEngineBestMove(gc.cl.p_fen, "");
		}
		else
		{
			ec.chessEngineProblem = true;
			if (!(gc.fen.equals(gc.startFen) & ec.chessEnginePlayMod == 1))
				startChessClock();
			messageEngineShort  = "";
			setInfoMessage(getString(R.string.player_move), null, null, false);
			ec.chessEngineSearching = false;
		}
	}


	public boolean restartEngine()
	{

//Log.i(TAG, "restartEngine(), process: " + ec.getEngine().process);
//Log.i(TAG, "restartEngine(), run_engineProcess: " + runP.getString("run_engineProcess", ""));

		if (ec.getEngine().initProcess(runP.getString("run_engineProcess", "")))
		{
			startNewGame(ec.getEngine().engineNumber);
			return true;
		}
		else
		{
			stopChessClock();
			ec.chessEngineSearching = false;
			ec.chessEnginePaused = true;
			ec.chessEngineInit = true;
			stopThreads(false);
			setInfoMessage(getString(R.string.engine_noRespond) + " (restartEngine)", null, null, false);
			return false;
		}
	}

	public boolean startNewGame(int engineNumber)
	{
		switch (engineNumber)
		{
			case 1:	// default engine Stockfish
				ec.getEngine().setUciMultiPV(userPrefs.getInt("user_options_enginePlay_MultiPv", OptionsEnginePlay.PV_MULTI));
				ec.getEngine().setIsChess960(gc.isChess960);
				ec.getEngine().setUciStrength(userPrefs.getInt("user_options_enginePlay_strength", 100));
				ec.getEngine().setUciContempt(userPrefs.getInt("user_options_enginePlay_aggressiveness", 0));
				ec.getEngine().setUciHash(16);
				ec.getEngine().setUciPonder(userPrefs.getBoolean("user_options_enginePlay_Ponder", false));
				ec.getEngine().setStartFen(gc.startFen);
				ec.getEngine().newGame();
				break;
			case 2:

				break;
		}
		return true;
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
//Log.i(TAG, "chessEngineBestMove(), fen, w/b, engine: " + fen + ",   moves: " + moves);
//Log.i(TAG, "isGoPonder: " + isGoPonder + ", ec.chessEngineSearchingPonder: " + ec.chessEngineSearchingPonder);
//Log.i(TAG, "ec.ponderUserFen: " + ec.ponderUserFen);
			if (!isGoPonder & ec.chessEngineSearchingPonder)
			{
				searchTaskFen = fen;
				ec.ponderUserFen = fen;
			}
			else
			{

//Log.i(TAG, "chessEngineBestMove(), fen, w/b, engine: " + fen + ",   moves: " + moves);

				boolean isError = false;
				if (ec.getEngine().process != null)
					isError = ec.getEngine().isError();
				if (ec.getEngine().process == null | isError)
				{
					if (!restartEngine())
						return;
				}

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
			chessEngineSearchTask.cancel(true);
	}

	public CharSequence getEngineThinkingMessage()
	{
//Log.i(TAG, "getEngineThinkingMessage(), ec.chessEnginePlayMod: " + ec.chessEnginePlayMod);
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
			{
				if 	(		userPrefs.getBoolean("user_options_enginePlay_Ponder", false)
						&	ec.getEngine().isUciPonder
						& 	ec.chessEngineSearchingPonder & (ec.chessEnginePlayMod == 1 | ec.chessEnginePlayMod == 2)
					)
				{
					return getString(R.string.player_move);
				}
				else
					return ec.getEngine().engineName + " " + ec.getEngine().engineNameStrength;
			}
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
			return getString(R.string.engine_paused);
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
			messageEngine = "";
			messageEngineShort  = "";
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
						timeBlack = userPrefs.getInt("user_time_engine_clock", 60000);
						bonusWhite = userPrefs.getInt("user_bonus_player_clock", 3000);
						bonusBlack = userPrefs.getInt("user_bonus_engine_clock", 3000);
						timeWhite = timeWhite + bonusWhite;
						timeBlack = timeBlack + bonusBlack;
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
						timeWhite = userPrefs.getInt("user_time_engine_clock", 60000);
						timeBlack = userPrefs.getInt("user_time_player_clock", 300000);
						bonusWhite = userPrefs.getInt("user_bonus_engine_clock", 3000);
						bonusBlack = userPrefs.getInt("user_bonus_player_clock", 3000);
						timeWhite = timeWhite + bonusWhite;
						timeBlack = timeBlack + bonusBlack;
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
						timeWhite = userPrefs.getInt("user_time_engine_clock", 60000);
						timeBlack = userPrefs.getInt("user_time_engine_clock", 60000);
						bonusWhite = userPrefs.getInt("user_bonus_engine_clock", 3000);
						bonusBlack = userPrefs.getInt("user_bonus_engine_clock", 3000);
						timeWhite = timeWhite + bonusWhite;
						timeBlack = timeBlack + bonusBlack;
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
						timeWhite = timeWhite + bonusWhite;
						timeBlack = timeBlack + bonusBlack;
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
		handlerChessClock.removeCallbacks(mUpdateChessClock);
		tc.stopChessClock(System.currentTimeMillis(), ec.chessEnginePlayMod);
	}

	public void startChessClock()
	{
		if (gc.cl.p_color.equals("w"))
			tc.startChessClock(true, System.currentTimeMillis(), ec.chessEnginePlayMod);
		else
			tc.startChessClock(false, System.currentTimeMillis(), ec.chessEnginePlayMod);
		engineControlTime = System.currentTimeMillis();
		handlerChessClock.removeCallbacks(mUpdateChessClock);
		handlerChessClock.postDelayed(mUpdateChessClock, CLOCK_START);
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
		return userPrefs.getBoolean("user_options_gui_LastPosition", true);
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
		CharSequence[] pgnSplit = pgnData.toString().split(" ");
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
				{
					fen = pgnData;
					if (pgnSplit.length == 1)
						fen = fen + " w - - 0 1";
				}
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

//			Log.i(TAG, "getFromClipboard(), gc.startFen: " + gc.startFen);

		}
		if (!gc.cl.p_stat.equals("1"))
			return;
//Log.i(TAG, "getFromClipboard(), mod: " + ec.chessEnginePlayMod + ", chessEnginePaused: " + ec.chessEnginePaused);


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
		if (ec.chessEnginePausedPrev)
			updateGui();
		else
			startPlay(false, true);

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

	public void setInfoMessage(CharSequence info, CharSequence engine, CharSequence moveNotification, boolean fullGameNotation)
	{
//Log.i(TAG,"1 setInfoMessage(), gc.cl.p_moveText: " + gc.cl.p_moveText);
//    	Log.i(TAG,"setInfoMessage()info: " + info);
//    	Log.i(TAG,"setInfoMessage(), engine: " + engine);
//    	Log.i(TAG,"engine: " + engine + ", pause_messageEngine:\n" + pause_messageEngine);
//    	Log.i(TAG,"move notification: " + moveNotification);
//    	Log.i(TAG, "infoContent: " + infoContent);
//    	Log.i(TAG, "gc.cl.p_fen  : " + gc.cl.p_fen);
//    	Log.i(TAG, "searchTaskFen: " + searchTaskFen);
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
			if (ec.chessEnginesOpeningBook)
				messageEngineShort = getString(R.string.engine_openingBook);
		}
		if (!gc.cl.p_fen.equals(searchTaskFen) & !ec.chessEngineSearchingPonder)
		{
//Log.i(TAG,"setInfoMessage(), gc.cl.p_fen:   " + gc.cl.p_fen);
//Log.i(TAG,"setInfoMessage(), searchTaskFen: " + searchTaskFen);
//Log.i(TAG,"setInfoMessage(), engine: " + engine + ", messageEngine: " + messageEngine);
			if (!messageEngine.equals(""))
				engine = messageEngine;		// show current engine message
			else
				engine = "";

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
		if (engine != null)
		{
			if (!engine.equals("") & ec.chessEnginePaused)
			{
				if (userPrefs.getBoolean("user_options_enginePlay_EngineMessage", true))
				{
					engine = engine.toString().replace(getString(R.string.epPonder) + ":", getString(R.string.engine_paused));
					engine = engine.toString().replace(getString(R.string.engineThinking) + ":", getString(R.string.engine_paused));
				}
			}
			if (!engine.equals("") & !userPrefs.getBoolean("user_options_enginePlay_EngineMessage", true))
			{
				engine = engine.toString().replace(getString(R.string.epPonder) + ":", "");
				engine = engine.toString().replace(getString(R.string.engineThinking) + ":", "");
			}
			messageEngine = engine;
		}

		// show book hints
		if 	(
				 	userPrefs.getBoolean("user_options_enginePlay_ShowBookHints", true)
				& 	(ec.chessEnginePlayMod == 1 | ec.chessEnginePlayMod == 2 | ec.chessEnginePlayMod == 5)
				& 	messageEngine.equals("")
			)
		{
			Pair<String, ArrayList<Move>> bi = null;
			try {bi = ec.book.getAllBookMoves(TextIO.readFEN(gc.cl.p_fen.toString()));}
			catch (ChessParseError e1) {e1.printStackTrace();}
			if (bi != null)
			{
				if (bi.first != null)
				{
					if (!bi.first.equals(""))
					{
						messageEngine = getString(R.string.engine_openingBook) + ":\n";
						messageEngine = messageEngine + bi.first;
					}
				}
			}
		}

		// msgShort
		CharSequence message = "";
		CharSequence messageShort = messageEngineShort;
		if (!messageInfo.equals(""))
		{
//Log.i(TAG,"messageEngineShort: " + messageEngineShort);
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

		setTextViewColors(msgShort, cv.COLOR_INFO_BACKGROUND_14, cv.COLOR_INFO_TEXT_15);
		setTextViewColors(msgShort2, cv.COLOR_INFO_BACKGROUND_14, cv.COLOR_INFO_TEXT_15);
		msgShort.setText(" " + messageShort);
		msgShort2.setText(messageInfo);

		// msgMoves
		if (gc.errorMessage.equals(""))
			setTextViewColors(msgMoves, cv.COLOR_MOVES_BACKGROUND_8, cv.COLOR_MOVES_TEXT_9);
		else
			setTextViewColors(msgMoves, cv.COLOR_HIGHLIGHTING_22, cv.COLOR_MOVES_TEXT_9);
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

		// msgEngine
//Log.i(TAG,"setInfoMessage(), messageEngine: " + messageEngine);
		if (!messageEngine.equals(""))
		{
			msgEngine.setVisibility(TextView.VISIBLE);
			setTextViewColors(msgEngine, cv.COLOR_ENGINE_BACKGROUND_12, cv.COLOR_ENGINE_TEXT_13);
			if (userPrefs.getBoolean("user_options_enginePlay_EngineMessage", true))
			{
				msgEngine.setLines(userPrefs.getInt("user_options_enginePlay_displayedLines", OptionsEnginePlay.DISPLAYED_LINES));
				msgEngine.setText(messageEngine);
			}
			else
			{
				String[] lineSplit = messageEngine.toString().split("\n");
				String mEngine = "";
				if (lineSplit.length > 0)
					mEngine = lineSplit[0];
				msgEngine.setText("");
				msgEngine.setVisibility(TextView.GONE);
				msgShort.setText(mEngine);
			}
		}
		else
		{
				msgEngine.setVisibility(TextView.GONE);
				msgEngine.setText("");
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
		setPlayModeButton(userPrefs.getInt("user_play_playMod", 1), gc.cl.p_color, ec.chessEnginePaused, ec.chessEngineSearching, gc.isBoardTurn);
	}

	public void setPlayModeButton(int playMode, CharSequence color, boolean isEnginePaused, boolean isEngineSearching, boolean isBoardTurn)
	{	// btn_1
//Log.i(TAG,"1 setPlayModeButton(), playMode: " +playMode + ", color: " +color + ", isEnginePaused: " +isEnginePaused + ", isEngineSearching: " +isEngineSearching + ", isBoardTurn: " +isBoardTurn);
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
								if (userPrefs.getBoolean("user_options_enginePlay_Ponder", false) & !msgEngine.getText().toString().equals(""))
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
//Log.i(TAG, "getMoveIdxFromInfo, infoMoveStartX, moveIdx: " + infoMoveStartX + ", " + moveIdx);
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
		String infoShort = "";
		String[] lineSplit = engineMes.toString().split("\n");
		for (int i = 0; i < lineSplit.length; i++)
		{
			if (lineSplit[i].contains("*1(") | lineSplit[i].contains(">1("))
			{
				String[] txtSplit = lineSplit[i].split(" ");
				if (txtSplit[0].startsWith("*1(") | txtSplit[0].startsWith(">1("))
				{
					String bestMove = txtSplit[1];
					if (bestMove.contains("..."))
						bestMove = bestMove + txtSplit[2];
					String bestScore = txtSplit[0].replace("*1(", "");
					bestScore = bestScore.replace(">1(", "");
					bestScore = bestScore.replace(")", "");
					infoShort = bestScore + "  " + bestMove;
				}
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
		if (gc.cl.p_auto_draw)
			mes = mes + " (" + getString(R.string.cl_draw) + ")";
		mes = mes + " (" + gc.cl.history.getGameTagValue("Result") + ")";
		return mes;
	}

	public void updateCurrentPosition(CharSequence message)
	{
//Log.i(TAG, "updateCurrentPosition(), msgMoves: " + msgMoves + ", message: " + message + ", gc.cl.p_stat: " + gc.cl.p_stat);
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
				setInfoMessage("", "", "", false);
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
					if ((ec.chessEnginePlayMod == 1 | ec.chessEnginePlayMod == 2) & ec.chessEngineSearchingPonder)
					{
						if (chessEngineSearchTask.currentBestMove != "")
						{
							gc.cl.newPositionFromMove(gc.fen, chessEngineSearchTask.currentBestMove, true);
							gc.promotionMove = "";
							if (gc.cl.p_stat.equals("1"))
							{
								updateGui();
								gc.move = "";
								ec.ponderUserFen = gc.cl.p_fen;
							}
							else
							{
								if (ec.getEngine().syncStopSearch(false))
									return;
							}
						}
					}
					else
					{
//Log.i(TAG, "startForceComputerMove(), ec.makeMove: " + ec.makeMove + ", playMod: " + ec.chessEnginePlayMod + "\ngc.fen: " + gc.fen);
						if (ec.makeMove)
							pauseEnginePlay(2);    // move & continue analysis
						else
						{
//Log.i(TAG, "startForceComputerMove() Btn 3, ec.makeMove: " + ec.makeMove);
							ec.chessEnginePlayMod = 4;
							setPlayModPrefs(ec.chessEnginePlayMod);
							ec.chessEnginePaused = false;
							ec.chessEngineInit = false;
							updateCurrentPosition("");
							startPlay(false, false);
						}
					}
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

	public void setTagResult(CharSequence result, CharSequence moveText)
	{	// set game result and updateGui
		gc.cl.history.setGameTag("Result", result.toString());
		gc.cl.history.setMoveText(moveText);
		gc.isGameOver = true;
		gc.isGameUpdated = false;
		updateCurrentPosition("");
		setInfoMessage(gc.cl.p_message, messageEngine, gc.cl.p_moveText, true);
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
		updateCurrentPosition("");
	}

	//	HANDLER, TIMER		HANDLER, TIMER		HANDLER, TIMER		HANDLER, TIMER
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
//Log.i(TAG, "stopThreads(), shutDown: " + shutDown);
		if (ec.chessEngineSearchingPonder & !shutDown)
		{
			setPauseEnginePlay(false);
		}
		else
			stopTimeHandler(shutDown);
		initPonder();
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
			setInfoMessage(getString(R.string.ccsMessageAutoPlayStopped), "", "", false);
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
					setInfoMessage(getGameOverMessage(), null, null, false);
				}
				else
				{
					boolean engineControlError = false;
					if (ec.chessEngineSearching & !ec.chessEngineIsInSearchTask)
					{
						if (System.currentTimeMillis() - engineControlTime >= 3000)
							engineControlError = true;
					}
					else
						engineControlTime = System.currentTimeMillis();
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
							stopComputerThinking(false);
							ec.chessEngineSearchingPonder = false;
							ec.chessEnginePaused = true;
							ec.chessEngineInit = true;
							updateCurrentPosition("");
							setInfoMessage(getString(R.string.engine_noRespond) + " (4)" + getString(R.string.engine_paused), null, null, false);
						}
						else
						{
							ec.chessEngineSearching = true;
							ec.chessEnginePaused = false;
							updateGui();
							stopThreads(false);
							startPlay(false, false);
						}

						return;
					}
					updateTime(gc.cl.p_color);
				}
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
//Log.i(TAG, "searchTask, doInBackground(), taskFen, taskMoves: " + taskFen + ", " +taskMoves);

			CharSequence firstMove = setRandomFirstMove(taskFen);
			if (!firstMove.equals(""))
				return firstMove;
			ec.chessEnginesOpeningBook = false;
			if (ec.chessEnginePlayMod == 3)
				tc.clockIsRunning = true;
			if 	(		userPrefs.getBoolean("user_options_enginePlay_OpeningBook", true)
					& 	ec.chessEnginePlayMod != 4 & !ec.chessEngineSearchingPonder
				)
			{	// using openingBook
//Log.i(TAG, "searchTask, doInBackground(), taskFen: " + taskFen);
				Move bookMove = null;
				try {bookMove = ec.book.getBookMove(TextIO.readFEN(taskFen.toString()));}
				catch (ChessParseError e1) {e1.printStackTrace();}
				if (bookMove != null)
				{
//Log.i(TAG, "searchTask, doInBackground(), tc.clockIsRunning: " + tc.clockIsRunning + ", tc.timeWhite: " + tc.timeWhite + ", tc.timeBlack: " + tc.timeBlack);
//Log.i(TAG, "searchTask, doInBackground(), bookMove: " + bookMove.toString());

					if (ec.chessEnginePlayMod == 3)
						tc.clockIsRunning = false;
					ec.chessEnginesOpeningBook = true;
					if (!tc.clockIsRunning & ec.chessEnginePlayMod == 1)
						startChessClock();
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
			ec.getEngine().statTime = 0;
			ec.getEngine().statPvBestMove = "";
			ec.getEngine().statPvScore = 0;

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
			ec.getEngine().startSearch(taskFen, taskMoves, wTime, bTime, wInc, bInc, moveTime, movesToGo, ec.chessEngineAnalysis, isGoPonder, mate);
			isGoPonder = false;

			isTimeCheck = false;
			timeCheckStart = System.currentTimeMillis();
			if (ec.chessEnginePlayMod == 1 & gc.getValueFromFen(taskFen, 2).equals("b"))
			{
				isTimeCheck = true;
				timeCheck = bTime;
			}
			if (ec.chessEnginePlayMod == 2 & gc.getValueFromFen(taskFen, 2).equals("w"))
			{
				isTimeCheck = true;
				timeCheck = wTime;
			}

			while(true)
			{
				ec.chessEngineIsInSearchTask = true;

				if (ec.chessEngineStopSearch)
				{
//Log.i(TAG, "searchTask, ec.chessEngineStopSearch: " + ec.chessEngineStopSearch);
					return "CANCEL";
				}

				currentBestMove = ec.getEngine().statPvBestMove;		// current best move
				currentPonderMove = ec.getEngine().statPvPonderMove;	// current ponder move

				CharSequence s = ec.getEngine().readLineFromProcess(engineSearchTimeout);

				if (s.equals("ERROR"))
					return "ERROR";

				if (s == null | s.length() == 0)
				{
//Log.i(TAG, "line: " + s);
					s = "";
				}

				if (!s.equals(""))
				{
//Log.i(TAG, "line: " + s);
					searchStartTimeInfo = System.currentTimeMillis();
				}
				else
				{
					currentTime = System.currentTimeMillis();
					if (cancelTask & ((int) (currentTime - searchStartTimeInfo) > MAX_SEARCH_CANCEL_TIMEOUT))
						return "NO_RESPOND";
					if ((int) (currentTime - searchStartTimeInfo) > MAX_SEARCH_TIMEOUT)
						return "NO_RESPOND";
				}

				CharSequence[] tokens = ec.getEngine().tokenize(s);

				boolean isInfo = false;
				boolean isPV = false;
				if (tokens[0].equals("info"))
				{
					ec.getEngine().parseInfoCmd(tokens, userPrefs.getInt("user_options_enginePlay_PvMoves", OptionsEnginePlay.PV_MOVES));
					int depth = ec.getEngine().statCurrDepth;
					int selDepth = ec.getEngine().statCurrSelDepth;
					int moveNumber = ec.getEngine().statCurrMoveNr;
					int moveNumberCnt = ec.getEngine().statCurrMoveCnt;
					CharSequence move = ec.getEngine().statCurrMove;
					int nodes = ec.getEngine().statCurrNodes;
					engineStat = getInfoStat(depth, selDepth, moveNumber, moveNumberCnt, nodes, move, taskFen);
					isPV = true;
//Log.i(TAG, "engineStat: " + engineStat);
					if (infoShowPv & !s.toString().contains("multipv") & s.toString().contains(" pv "))
					{
						isInfo = true;
						ec.getEngine().statCurrMoveNr = 0;
						ec.getEngine().statCurrMoveCnt = 0;
						engineMes = getInfoPv(0, ec.getEngine().statPvMoves,
								ec.getEngine().statPvScore, ec.getEngine().statIsMate, taskFen);
					}
					if (infoShowPv & s.toString().contains("multipv"))
					{
						try
						{
							isInfo = true;
							int multiPv = userPrefs.getInt("user_options_enginePlay_MultiPv", OptionsEnginePlay.PV_MULTI);
							int pvNr = ec.getEngine().statPvIdx +1;
							if 	(multiPv == pvNr & depth >= 8)
								isPV = true;
							engineMes = getInfoPv(ec.getEngine().statPvIdx, ec.getEngine().statPvMoves,
									ec.getEngine().statPvScore, ec.getEngine().statIsMate, taskFen);
						}
						catch (NullPointerException e) {e.printStackTrace(); engineMes = "";}
					}
					else
					{
						if (ec.getEngine().statPvIdx == 0)
							bestScore = getBestScore(ec.getEngine().statPvScore, taskFen);
					}
//Log.i(TAG, "ec.getEngine().statPvAction: >" + ec.getEngine().statPvAction + "<, infoShowPv: " + infoShowPv
//		+ ", engineMes: " + engineMes + ", engineStat: " + engineStat + "\ns: " + s);
				}
				if ((!s.equals("") & isInfo) | isPV)
				{
					if (searchStartTimeInfo - publishTime >= MIN_PUBLISH_TIME | isPV)
					{
						publishTime = searchStartTimeInfo;
						publishProgress(ec.getEngine().statPvAction, "" + engineStat + engineMes, "");
					}
				}

				if (isInfo & ec.chessEnginePlayMod <= 3 & taskFen.equals(gc.standardFen) & currentBestMove != null)
				{
					if (ec.getEngine().syncStopSearch(false))
						currentBestMove = ec.getEngine().stopBestMove;
					if (!currentBestMove.equals(""))
						return currentBestMove;
				}

				if (tokens[0].equals("bestmove") & ec.chessEnginePlayMod != 4)
				{	// get best move if not analysis
					handlerChessClock.removeCallbacks(mUpdateChessClock);
					if (tokens.length == 4)
					{
						currentBestMove = tokens[1];
						currentPonderMove = tokens[3];
					}
					return tokens[1];	//>242	return bestmove
				}

				if 	(ec.chessEngineAnalysisStat == 2) // stop, make move and continue
				{	// engine searching has stopped by user

					if (!currentBestMove.equals(""))
					{
						boolean isStop = ec.getEngine().syncStopSearch(true);
//Log.i(TAG, "analysis has stopped, stat: " + ec.chessEngineAnalysisStat + ", bestMove: " + currentBestMove + ", isStop: " + isStop);
						if (isStop & !ec.getEngine().stopBestMove.equals(""))
						{
							currentBestMove = ec.getEngine().stopBestMove;
							currentPonderMove = ec.getEngine().stopPonderMove;
						}
						if 	(		userPrefs.getBoolean("user_options_enginePlay_Ponder", false)
								&	ec.getEngine().isUciPonder
								& 	(ec.chessEnginePlayMod == 1 | ec.chessEnginePlayMod == 2)
								& 	!currentPonderMove.equals("") & !ec.chessEngineSearchingPonder
							)
							ec.chessEngineAnalysisStat = 0;	// trigger ponder searching
						cancelTask = false;
						return currentBestMove;
					}
				}

				if 	(isTimeCheck & !currentBestMove.equals("") & !ec.chessEngineSearchingPonder) // stop, time control <= 200ms
				{
					long currentTime = System.currentTimeMillis();
					timeCheck = timeCheck - currentTime + timeCheckStart;
					timeCheckStart = currentTime;
					if (timeCheck < timeCheckControl)
					{
						if (ec.getEngine().syncStopSearch(false))
						{
							if (isGoPonder)
								timeCheck = timeCheckControl + 100;
							else
							{
								if (!ec.getEngine().stopBestMove.equals(""))
									currentBestMove = ec.getEngine().stopBestMove;
								if (!ec.getEngine().stopPonderMove.equals(""))
									currentPonderMove = ec.getEngine().stopPonderMove;
								return currentBestMove;
							}
						}
						else
							return currentBestMove;
					}
				}

				// user move action: stop ponder search and start new search (ec.ponderUserFen)
				if 	(		userPrefs.getBoolean("user_options_enginePlay_Ponder", false)
						&	ec.getEngine().isUciPonder
						& 	ec.chessEngineSearchingPonder & !ec.ponderUserFen.equals("")
					)
				{
					taskFen = ec.ponderUserFen;
					taskMoves = "";
					initInfoArrays(true);
					if (ec.getEngine().syncStopSearch(false))
						return "PONDER_ACTION";
					else
					{
						cancelTask = true;
						return "CANCEL";
					}
				}

				if (cancelTask & !s.equals(""))
				{
//Log.i(TAG, "cancelTask, s: " + s);
					if (ec.chessEnginePlayMod == 4 & ec.chessEngineAnalysisStat < 9 & !currentBestMove.equals(""))
					{	// analysis has stopped
						if (ec.getEngine().syncStopSearch(false) & !ec.getEngine().stopBestMove.equals(""))
						{
							currentBestMove = ec.getEngine().stopBestMove;
							currentPonderMove = ec.getEngine().stopPonderMove;
						}
						return currentBestMove;
					}
					else
						return "CANCEL";			// ChessEngineSearchTask is cancelled
				}
			}
		}

		protected void onPreExecute()
		{
			cancelTask = false;
			ec.chessEngineStopSearch = false;
			ec.chessEngineIsInSearchTask = false;
			searchStartTimeInfo = System.currentTimeMillis();
			currentBestMove = "";
			currentPonderMove = "";
			ec.getEngine().statCurrMoveCnt = 0;
			if (infoPv == null | ec.chessEngineInit)
				initInfoArrays(true);
			else
				initInfoArrays(false);
			isSearchTaskStopped = false;
			if (!ec.chessEngineAutoRun)
			{
				if (userPrefs.getBoolean("user_options_enginePlay_OpeningBook", true) & ec.chessEnginesOpeningBook)
					setInfoMessage(getString(R.string.engine_openingBook), "", null, false);
				else
					setInfoMessage(getEngineThinkingMessage(), "", null, false);
			}
		}

		protected void onProgressUpdate(CharSequence... args)
		{	// called from doInBackground() : publishProgress()
			CharSequence pvAction = args[0];
			CharSequence engineMes = args[1];
			CharSequence engineStat = args[2];
			String engineUpdate = "";
			if (isUpdated)
				engineUpdate = "ENGINE_UPDATE";
//Log.i(TAG, "onProgressUpdate(), pvAction: " + pvAction);
//Log.i(TAG, "onProgressUpdate(), engineMes: " + engineMes);
//Log.i(TAG, "onProgressUpdate(), engineStat: " + engineStat);

			if (ec.chessEngineStopSearch)
				return;

			if (tc.clockIsRunning)
			{
				if (gc.isGameOver | gc.cl.p_variationEnd)
				{
					tc.stopChessClock(System.currentTimeMillis(), ec.chessEnginePlayMod);
					if (!ec.chessEngineAutoRun)
					{
						ec.chessEnginePaused = true;
						ec.chessEngineSearching = false;
					}
					updateCurrentPosition("");
					setInfoMessage("   " + getGameOverMessage(), engineMes, engineUpdate, false);
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
						setInfoMessage(getString(R.string.engine_openingBook), "", engineUpdate, false);
					else
						setInfoMessage(getString(R.string.engine_autoPlay) + showGameCount + "\n" + getString(R.string.engine_openingBook), "", engineUpdate, false);
				}
				else
				{
					if (!ec.chessEngineAutoRun)
					{
						if (getEngineThinkingMessage().equals(getString(R.string.engineAnalysisStopWait)))
							engineStat = "";
						if (!ec.chessEngineStopSearch)
							setInfoMessage(getEngineThinkingMessage() + " " + engineStat, engineMes, engineUpdate, false);
					}
					else
						setInfoMessage(null, engineMes, engineUpdate, false);
				}
			}
			isUpdated = true;
		}

		protected void onPostExecute(CharSequence result)
		{
//Log.i(TAG, "onPostExecute(), result: " + result + ", prevResult: " + prevResult);
			ec.chessEngineIsInSearchTask = false;
			ec.chessEngineAnalysis = false;
			if (result.equals("NO_RESPOND"))
			{
				stopChessClock();
				ec.chessEngineSearching = false;
				stopComputerThinking(true);
				ec.chessEngineSearchingPonder = false;
				ec.chessEnginePaused = true;
				ec.chessEngineInit = true;
				updateCurrentPosition("");
				if (!cancelTask)
					setInfoMessage(getString(R.string.engine_timeout), null, null, false);
				return;
			}

			if (result.equals("ERROR") | result.equals("ERROR_READY"))
			{
				stopChessClock();
				ec.chessEngineSearching = false;
				ec.chessEnginePaused = true;
				ec.chessEngineInit = true;
				setInfoMessage(getString(R.string.engine_noRespond) + " (10)", null, null, false);
				return;
			}

			if (result.equals("PONDER_ACTION"))
			{
				// !!! stop ponder searching and start new search: ec.ponderUserFen
//Log.i(TAG, "onPostExecute(), PONDER_ACTION, ec.ponderUserFen: " + ec.ponderUserFen);
				CharSequence newFen = ec.ponderUserFen;
				initPonder();
				if (!newFen.equals(""))
					chessEngineBestMove(newFen, "");
				return;
			}

			if (result.equals("CANCEL") | cancelTask)
			{
				ec.chessEngineSearching = false;
				ec.chessEngineStopSearch = false;
				stopChessClock();
				ec.chessEngineSearchingPonder = false;
				if (ec.chessEnginePlayMod == 4)
				{
					ec.chessEnginePaused = true;
					updateGui();
					setInfoMessage(getEnginePausedMessage(), null, null, false);
					Toast.makeText(MainActivity.this, getString(R.string.engineAnalysisStop), Toast.LENGTH_SHORT).show();
				}
				return;
			}

			if (!result.equals(""))
			{
				if (Character.isUpperCase(result.charAt(0)))
				{
					stopChessClock();
					ec.chessEngineSearching = false;
					ec.chessEnginePaused = true;
					ec.chessEngineInit = true;
					setInfoMessage(getString(R.string.engine_noRespond) + " (6): " + result, null, null, false);
					return;
				}
			}


			if (!isSearchTaskStopped)
			{
				if (!ec.getEngine().getSearchAlive())
				{
					ec.getEngine().searchAlive = true;
					if (ec.chessEngineInit)
					{
						ec.chessEnginePaused = true;
						setInfoMessage(getString(R.string.engine_paused), null, null, false);
					}
					else
					{
						stopComputerThinking(false);
						ec.chessEngineSearchingPonder = false;
						ec.chessEngineInit = true;
						ec.chessEnginePaused = true;
						updateGui();
						stopChessClock();
						setInfoMessage(getString(R.string.engine_noRespond) + " (7)", null, null, false);
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
			if 	(	infoPv.size() 	== 	userPrefs.getInt("user_options_enginePlay_MultiPv", OptionsEnginePlay.PV_MULTI)
					& statPvIdx 	< 	userPrefs.getInt("user_options_enginePlay_MultiPv", OptionsEnginePlay.PV_MULTI)
					)
			{
				infoPv.set(statPvIdx, statPvMoves);
				if (statPvIdx == 0)
					bestScore = getBestScore(statPvScore, fen);
				CharSequence displayScore = getDisplayScore(statPvScore, fen);
				if (isMate & statPvScore > 0)
					displayScore = "M" + statPvScore;
				sbMoves.setLength(0); sbMoves.append("*"); sbMoves.append((statPvIdx +1)); sbMoves.append("(");
				CharSequence notation = gc.cl.getNotationFromInfoPv(fen, statPvMoves);
				if (notation.equals(""))
					return "";
				notation = gc.cl.history.getAlgebraicNotation(notation, userPrefs.getInt("user_options_gui_PieceNameId", 0));
				sbMoves.append(displayScore); sbMoves.append(") "); sbMoves.append(notation);
//Log.i(TAG, "taskFen: " + taskFen);
//Log.i(TAG, "statPvMoves: " + statPvMoves);
//Log.i(TAG, "notation: "  + notation);
//Log.i(TAG, "sbMoves: "  + sbMoves);
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

		protected CharSequence getInfoStat(int depth, int selDepth, int moveNumber, int moveNumberCnt, int nodes, CharSequence move, CharSequence fen)
		{
			CharSequence infoStat = "";
			if (!ec.chessEngineSearchingPonder)
				infoStat = getString(R.string.engineThinking);
			else
				infoStat = getString(R.string.epPonder);
			CharSequence notation = gc.cl.getNotationFromInfoPv(fen, move);
			notation = gc.cl.history.getAlgebraicNotation(notation, userPrefs.getInt("user_options_gui_PieceNameId", 0));
			int nodesK = nodes / 1000;
			String moveInfo = "";
			if (moveNumberCnt > 0)
				moveInfo = + moveNumber + "(" + moveNumberCnt + "): ";
			infoStat = infoStat + ":  " + moveInfo + notation + "  d:" + depth + "/" + selDepth + "  n:" + nodesK + "k\n";
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
		CharSequence prevResult = "";
		long currentTime = 0;
		long publishTime = 0;
		CharSequence taskFen = "";
		CharSequence taskMoves = "";
		StringBuilder sbMoves = new StringBuilder(100);
		StringBuilder sbInfo = new StringBuilder(100);

		long searchStartTimeInfo = 0;		// info != ""
		int MAX_SEARCH_TIMEOUT = 180000;	// max. search time engine timeout (3 min: no info message)
		int MAX_SEARCH_CANCEL_TIMEOUT = 1500;	// max. search time engine timeout
		int MIN_PUBLISH_TIME = 100;				// min. time for publishing
		boolean cancelTask = false;

		boolean isTimeCheck = false;
		long timeCheckStart = 0;
		long timeCheck = 0;
		long timeCheckControl = 300;
		boolean isUpdated = false;
	}
	//  end ENGINE-SearchTask

	public void enginePlay(CharSequence result, CharSequence taskFen, CharSequence currentPonderMove)
	{
//Log.i(TAG, "enginePlay(), result: " + result + "\ntaskFen: " + taskFen);
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
						engineControlTime = System.currentTimeMillis();
						handlerChessClock.removeCallbacks(mUpdateChessClock);
						handlerChessClock.postDelayed(mUpdateChessClock, CLOCK_START);
						return;
					}

					if (ec.chessEngineAnalysisStat == 0 | ec.chessEngineAnalysisStat == 1)
					{
						newFen = "";
						setPauseEnginePlay(false);
					}

					stopChessClock();

					if (ec.chessEngineAnalysisStat == 2)
					{
						startChessClock();
						isGoPonder = false;
						chessEngineBestMove(newFen, "");
						ec.chessEngineSearching = true;
					}
					else
						updateCurrentPosition("");

					ec.chessEngineStopSearch = false;
				}
				else
				{
					newFen = chessEngineGui(taskFen, result);
					if (!newFen.equals(""))
					{
						if (ec.chessEnginePlayMod == 3)
						{	// engine vs engine
							if (ec.chessEngineAutoRun)
							{
//Log.i(TAG, "gameOver, variationEnd, result: " + gc.isGameOver + ", " + gc.cl.p_variationEnd + ", " + gc.cl.history.getGameResult());
								if 		(		gc.isGameOver | result.equals("(none)")
											| 	gc.cl.p_variationEnd | gc.cl.p_mate | gc.cl.p_stalemate | gc.cl.p_auto_draw
										)
								{
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
										& !gc.cl.p_mate & !gc.cl.p_stalemate & !gc.cl.p_auto_draw
										)
								{
									ec.chessEngineSearching = true;
									isGoPonder = false;
									chessEngineBestMove(newFen, "");
								}
								else
								{
									stopComputerThinking(false);
									ec.chessEngineSearchingPonder = false;
									ec.chessEnginePaused = true;
									updateCurrentPosition("");
									setInfoMessage(getEnginePausedMessage(), null, null, false);
								}
							}
						}
						else
						{
							ec.chessEngineSearching = false;
							if (userPrefs.getInt("user_options_timeControl", 1) == 2)
								setMoveTime();
						}

//Log.i(TAG, "enginePlay(), newFen: " + newFen + "\nresult: " + result + ", ponderMove: " + currentPonderMove);
						if 	(		userPrefs.getBoolean("user_options_enginePlay_Ponder", false)
								&	ec.getEngine().isUciPonder
								& 	(ec.chessEnginePlayMod == 1 | ec.chessEnginePlayMod == 2)
								& 	!currentPonderMove.equals("") & !ec.chessEngineSearchingPonder
							)
						{
							enginePlayPonder(newFen, currentPonderMove);
							return;
						}
						else
							initPonder();
//						if (ec.chessEnginePlayMod == 1 | ec.chessEnginePlayMod == 2)
						if (ec.chessEnginePlayMod == 1 | ec.chessEnginePlayMod == 2 | ec.chessEnginePlayMod == 3)
							playSound(1, 0);
						engineControlTime = System.currentTimeMillis();
						handlerChessClock.removeCallbacks(mUpdateChessClock);
						handlerChessClock.postDelayed(mUpdateChessClock, CLOCK_START);
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
				ec.chessEngineSearching = false;
				ec.chessEngineInit = true;
				ec.chessEnginePaused = true;
				updateGui();
				stopChessClock();

				if (ec.getEngine().initProcess(runP.getString("run_engineProcess", "")))
					setInfoMessage(getString(R.string.engine_paused) + " (8)", null, null, false);
				else
					setInfoMessage(getString(R.string.engine_noRespond) + " (searchTaskRestart)", null, null, false);
				return;
			}
		}
	}

	public void initPonder()
	{
		ec.chessEngineSearchingPonder = false;
		isGoPonder = false;
		ec.ponderUserFen = "";
	}

	public void enginePlayPonder(CharSequence fen, CharSequence move)
	{
//Log.i(TAG, "enginePlayPonder(), fen: " + fen + ", ponderMove: " + move);
		ec.chessEngineSearchingPonder = false;
		isGoPonder = false;
		ponderMove = move;
		boolean isStop = ec.getEngine().syncStopSearch(false);
		if (isStop | ponderMove != null)
		{
			if (!ponderMove.equals(""))
			{
				ec.chessEngineSearchingPonder = true;
				isGoPonder = true;
				chessEngineBestMove(fen, ponderMove);    // ponderMove !!!
				playSound(1, 0);
				return;
			}
		}

		stopComputerThinking(false);
		ec.chessEngineSearchingPonder = false;
		ec.chessEnginePaused = true;
		updateCurrentPosition("");
		setInfoMessage(getString(R.string.player_move), null, null, false);

	}

	public void engineStopPonder(CharSequence fen, int playMod)
	{
		if (ec.chessEngineSearchingPonder)
		{
//Log.i(TAG, "engineStopPonder(), ec.chessEngineSearchingPonder, gc.cl.p_fen: " + fen);
			if 	(		(gc.getValueFromFen(fen, 2).equals("b") & playMod == 1)
					|	(gc.getValueFromFen(fen, 2).equals("w") & playMod == 2)
				)
			{
				ec.ponderUserFen = fen;	// ---> chessEngineSearchTask : stop ponder search and start search (ec.ponderUserFen)
			}
			else
			{
				setPauseEnginePlay(false);
				startChessClock();
				messageEngineShort  = "";
				setInfoMessage(getString(R.string.player_move), null, null, false);
				initPonder();
			}
		}
	}

	public CharSequence chessEngineGui(CharSequence taskFen, CharSequence bestMove)
	{
//Log.i(TAG, "chessEngineGui(), taskFen: " + taskFen + "    bestMove: " + bestMove);
		CharSequence newFen = "";
		if (bestMove.length() > 4)			// promotion
		{
			if (bestMove.charAt(4) == 'q') bestMove = ((String) bestMove).substring(0, 4) + 'Q';
			if (bestMove.charAt(4) == 'r') bestMove = ((String) bestMove).substring(0, 4) + 'R';
			if (bestMove.charAt(4) == 'b') bestMove = ((String) bestMove).substring(0, 4) + 'B';
			if (bestMove.charAt(4) == 'n') bestMove = ((String) bestMove).substring(0, 4) + 'N';
		}
		gc.cl.newPositionFromMove(taskFen, bestMove, true);
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
		for (int i = 0; i < userPrefs.getInt("user_options_enginePlay_MultiPv", OptionsEnginePlay.PV_MULTI); i++)
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

	public void setTextViewColors(TextView tv, int tvColor, int tvTextColor)
	{
		GradientDrawable tvBackground = (GradientDrawable) tv.getBackground();
		tvBackground.setColor(cv.getColor(tvColor));
		tv.setTextColor(cv.getColor(tvTextColor));
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
		cancelEngineMessage();
		int lastMoveIdx = gc.cl.p_moveIdx;
		setInfoMoveValuesFromView(view, event);
		nextMove(19, getMoveIdxFromInfo());		// set moveIdx
		if (ec.chessEngineSearchingPonder)
			engineStopPonder(gc.cl.p_fen, ec.chessEnginePlayMod);
		else
		{
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
//Log.i(TAG, "startBoardMoveAction(), ec.chessEngineSearching: " + ec.chessEngineSearching + ", ec.chessEngineSearchingPonder: "+ ec.chessEngineSearchingPonder);
//Log.i(TAG, "startBoardMoveAction(), gc.fen: " + gc.fen);
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
//Log.i(TAG, "moveAction(), position: " + position);
		if (gc.cl.p_mate | gc.cl.p_stalemate)	// mate, steal mate?
		{
			setInfoMessage(getGameOverMessage(), null, null, false);
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

				//karl no quick move
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
						boardView.updateBoardView(gc.cl.p_fen, gc.isBoardTurn, gc.cl.p_possibleMoveList, gc.cl.p_possibleMoveToList,
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
							stopThreads(false);
						updateGui();
						setInfoMessage(getGameOverMessage(), null, null, false);
						return;
					}
					if (gc.cl.p_stat.equals("1"))
					{
						playSound(1, 0);
						if (userPrefs.getBoolean("user_options_gui_FlipBoard", false))
							startTurnBoard();
						gc.isGameUpdated = false;
						gc.move = "";
						if (ec.chessEnginePaused)
							setInfoMessage(getString(R.string.engine_paused), "", null, false);
					}

					if (!(ec.chessEnginePlayMod == 5 | ec.chessEnginePlayMod == 6))
					{
						gc.setGameOver(gc.cl.history.getGameTagValue("Result"));
						if (gc.cl.p_stat.equals("1") & !gc.isGameOver & !gc.cl.p_variationEnd)
							ec.chessEngineSearching = true;
					}
					if (!tc.clockIsRunning & ec.chessEnginePlayMod == 5 & gc.cl.p_stat.equals("1"))
					{
						twoPlayerPaused = false;
						startChessClock();
					}
					if (ec.chessEnginePlayMod == 5 & twoPlayerPaused & gc.cl.p_stat.equals("1"))
					{
						twoPlayerPaused = false;
						startChessClock();
					}
					updateGui();
					if 	(		ec.chessEngineSearching & gc.cl.p_stat.equals("1")
							& 	!gc.isGameOver & !gc.cl.p_variationEnd
						)
					{
						if (!ec.chessEnginePaused & (ec.chessEnginePlayMod == 3 | ec.chessEnginePlayMod == 4))
						{
							stopThreads(false);
							startEnginePlay(false);
						}
						else
						{
							isGoPonder = false;
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
//Log.i(TAG, "updateTimeBackground(), tv.getId(): " + tv.getId() + ", isClockRunning: " + isClockRunning + ", isWhiteMove: " + isWhiteMove);
		if (!isClockRunning)
        {
            setTextViewColors(lblPlayerTimeA, cv.COLOR_TIME2_BACKGROUND_20, cv.COLOR_TIME2_TEXT_21);
            setTextViewColors(lblPlayerTimeB, cv.COLOR_TIME2_BACKGROUND_20, cv.COLOR_TIME2_TEXT_21);
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
							setTextViewColors(tv, cv.COLOR_TIME_BACKGROUND_18, cv.COLOR_TIME_TEXT_19);
							setTextViewColors(lblPlayerTimeA, cv.COLOR_TIME2_BACKGROUND_20, cv.COLOR_TIME2_TEXT_21);
						}
						if (!isWhiteMove & lblPlayerTimeA.getId() == tv.getId())
						{
							setTextViewColors(tv, cv.COLOR_TIME_BACKGROUND_18, cv.COLOR_TIME_TEXT_19);
							setTextViewColors(lblPlayerTimeB, cv.COLOR_TIME2_BACKGROUND_20, cv.COLOR_TIME2_TEXT_21);
						}
					}
					else
					{
						if (isWhiteMove & lblPlayerTimeA.getId() == tv.getId())
						{
							setTextViewColors(tv, cv.COLOR_TIME_BACKGROUND_18, cv.COLOR_TIME_TEXT_19);
							setTextViewColors(lblPlayerTimeB, cv.COLOR_TIME2_BACKGROUND_20, cv.COLOR_TIME2_TEXT_21);
						}
						if (!isWhiteMove & lblPlayerTimeB.getId() == tv.getId())
						{
							setTextViewColors(tv, cv.COLOR_TIME_BACKGROUND_18, cv.COLOR_TIME_TEXT_19);
							setTextViewColors(lblPlayerTimeA, cv.COLOR_TIME2_BACKGROUND_20, cv.COLOR_TIME2_TEXT_21);
						}
					}
					break;
				case 3:     // < 10 sec
					setTextViewColors(tv, cv.COLOR_HIGHLIGHTING_22, cv.COLOR_TIME_TEXT_19);
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
		stopComputerThinking(false);
		ec.setPlaySettings(userPrefs, gc.cl.p_color);
		ec.chessEngineSearchingPonder = false;
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

	public boolean isPlayerMove(int playMode, CharSequence color)
	{
//Log.i(TAG, "isPlayerMove(), playMode: " + playMode + ", color: " + color);
		if (playMode == 1 & color.equals("w"))
			return true;
		if (playMode == 2 & color.equals("b"))
			return true;
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
		boolean isSearching = ec.chessEngineSearching;
		if (ec.chessEngineSearchingPonder)
		{
			setPauseEnginePlay(false);
			initPonder();
		}
		gc.isGameLoaded = false;
		if (ec.chessEnginePlayMod <= 3 | (ec.chessEnginePlayMod == 5 & !twoPlayerPaused))
			setRunPrefsTime();
		if (!ec.chessEnginePaused)
		{
			if (pause_mode == 6)
			{
				stopThreads(false);
				setPlayModPrefs(pause_mode);
				ec.chessEngineSearching = false;
				setInfoMessage(getString(R.string.menu_modes_edit), null, null, false);
			}
			else
			{
				setPauseValues(false, gc.fen, ec.chessEnginePlayMod, messageEngine);
				stopThreads(false);
				updateTime(gc.cl.p_color);
				if (isSearching)
					setInfoMessage(getString(R.string.engine_paused), null, null, false);
				else
					setInfoMessage(getString(R.string.clock_stopped), null, null, false);
			}
			ec.chessEngineAnalysisStat = 0;
		}
		dContinueId = 3;

		if (isFromDialog)
			setPauseModValues(ec.chessEnginePlayMod, pausePlayMod, ec.chessEnginePaused, dContinueId);

	}

	public void pauseStartPlay(int playMod, int continueId)
	{
//		Log.i(TAG, "pauseStartPlay(), playMod: " + playMod + ", continueId: " + continueId);
		twoPlayerPaused = false;
		boolean isNewGame = false;
		if (continueId == 1)
			isNewGame = true;
		boolean isSetClock = true;
		if (continueId == 3)
		{
			tc.timeControl = runP.getInt("run_timeControl_pause", 1);
			tc.movesToGo = runP.getInt("run_movesToGo", 0);
			tc.timeWhite = runP.getInt("run_time_white_pause", 300000);
			tc.bonusWhite = runP.getInt("run_bonus_white_pause", 3000);
			tc.timeBlack = runP.getInt("run_time_black_pause", 300000);
			tc.bonusBlack = runP.getInt("run_bonus_black_pause", 3000);
//Log.i(TAG, "pauseStartPlay(), tc.timeWhite: " + tc.timeWhite + ", tc.bonusWhite: " + tc.bonusWhite
//		+ ", tc.timeBlack: " + tc.timeBlack + ", tc.bonusBlack: " + tc.bonusBlack + ", tc.timeControl: " + tc.timeControl);
			tc.initChessClock(tc.timeControl, tc.timeWhite, tc.timeBlack, tc.movesToGo, tc.bonusWhite, tc.bonusBlack);
			tc.setCurrentShowValues(playMod);
			isSetClock = false;
		}
		setPlayModPrefs(playMod);
		switch (playMod)
		{
			case 1:     // white
			case 2:     // black
			case 3:     // engine vs engine
				ec.chessEnginePaused = false;
				ec.chessEngineInit = false;
				if (isNewGame)
				{
					msgEngine.setVisibility(TextView.GONE);
					messageInfo = "";
					messageEngine = "";
					messageEngineShort = "";
				}
				startPlay(isNewGame, isSetClock);
				break;
			case 4:     // analysis
				ec.initClockAfterAnalysis = true;
				ec.chessEnginePaused = false;
				ec.chessEngineInit = false;
				updateCurrentPosition("");
				startPlay(false, true);
				break;
			case 5:     // two players
				analysisMessage = "";
				startEdit(isNewGame, isSetClock);
				break;
			case 6:     // edit
				analysisMessage = "";
				startEdit(isNewGame, false);
				break;
		}

		removeDialog(PAUSE_DIALOG);

	}

	public void updateGui()
	{
//Log.i(TAG, "1 updateGui(), gc.isBoardTurn: " + gc.isBoardTurn + ", gc.cl.p_color: " + gc.cl.p_color);
		CharSequence messInfo = 	"";
		if (!gc.isGameOver & !gc.cl.p_variationEnd & gc.cl.p_message.equals(""))
		{
			showGameCount = "";
			if (ec.chessEnginePlayMod == 3 & ec.chessEngineAutoRun)
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
				if (ec.chessEnginePlayMod == 1 | ec.chessEnginePlayMod == 2)
				{
					messageEngine 		= "";
					messageEngineShort  = "";
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

//Log.i(TAG, "2 updateGui(), gc.cl.p_fen: " + gc.cl.p_fen + ", gc.isBoardTurn: " + gc.isBoardTurn);

		if (!gc.cl.p_fen.equals(""))
		{
			gc.fen = gc.cl.p_fen;
			if (gc.cl.p_color.equals("w"))
			{
				if (!gc.isBoardTurn)
				{
					setTextViewColors(lblPlayerNameA, cv.COLOR_DATA_BACKGROUND_16, cv.COLOR_DATA_TEXT_17);
					setTextViewColors(lblPlayerEloA, cv.COLOR_DATA_BACKGROUND_16, cv.COLOR_DATA_TEXT_17);
					setTextViewColors(lblPlayerNameB, cv.COLOR_HIGHLIGHTING_22, cv.COLOR_DATA_TEXT_17);
					setTextViewColors(lblPlayerEloB, cv.COLOR_HIGHLIGHTING_22, cv.COLOR_DATA_TEXT_17);
				}
				else
				{
					setTextViewColors(lblPlayerNameA, cv.COLOR_HIGHLIGHTING_22, cv.COLOR_DATA_TEXT_17);
					setTextViewColors(lblPlayerEloA, cv.COLOR_HIGHLIGHTING_22, cv.COLOR_DATA_TEXT_17);
					setTextViewColors(lblPlayerNameB, cv.COLOR_DATA_BACKGROUND_16, cv.COLOR_DATA_TEXT_17);
					setTextViewColors(lblPlayerEloB, cv.COLOR_DATA_BACKGROUND_16, cv.COLOR_DATA_TEXT_17);
				}
			}
			else
			{
				if (!gc.isBoardTurn)
				{
					setTextViewColors(lblPlayerNameA, cv.COLOR_HIGHLIGHTING_22, cv.COLOR_DATA_TEXT_17);
					setTextViewColors(lblPlayerEloA, cv.COLOR_HIGHLIGHTING_22, cv.COLOR_DATA_TEXT_17);
					setTextViewColors(lblPlayerNameB, cv.COLOR_DATA_BACKGROUND_16, cv.COLOR_DATA_TEXT_17);
					setTextViewColors(lblPlayerEloB, cv.COLOR_DATA_BACKGROUND_16, cv.COLOR_DATA_TEXT_17);
				}
				else
				{
					setTextViewColors(lblPlayerNameA, cv.COLOR_DATA_BACKGROUND_16, cv.COLOR_DATA_TEXT_17);
					setTextViewColors(lblPlayerEloA, cv.COLOR_DATA_BACKGROUND_16, cv.COLOR_DATA_TEXT_17);
					setTextViewColors(lblPlayerNameB, cv.COLOR_HIGHLIGHTING_22, cv.COLOR_DATA_TEXT_17);
					setTextViewColors(lblPlayerEloB, cv.COLOR_HIGHLIGHTING_22, cv.COLOR_DATA_TEXT_17);
				}
			}
		}
//Log.i(TAG, "3 updateGui()");
		if (gc.cl.p_message.equals("*"))
			gc.cl.p_message = "";

		if (gc.cl.p_moveText.equals(""))
		{
			if (!gc.errorMessage.equals(""))
			{
				setTextViewColors(msgMoves, cv.COLOR_HIGHLIGHTING_22, cv.COLOR_DATA_TEXT_17);
				messInfo = gc.errorMessage;
			}
		}
		else
			setTextViewColors(msgMoves, cv.COLOR_MOVES_ANOTATION_11, cv.COLOR_DATA_TEXT_17);
		if (!messInfo.equals(""))
		{

			if (gc.cl.p_message.equals(""))
				setInfoMessage(gc.cl.p_message + "" + messInfo, messageEngine, gc.cl.p_moveText, false);
			else
				setInfoMessage(gc.cl.p_message + ", " + messInfo, messageEngine, gc.cl.p_moveText, false);
		}
		else
		{
			setInfoMessage(gc.cl.p_message, messageEngine, gc.cl.p_moveText, false);
		}
		if (gc.cl.p_moveHasVariations | gc.cl.p_moveIsFirstInVariation)	// move has variation? | variation start
			gc.hasVariations = true;
		else
			gc.hasVariations = false;


		if (!gc.cl.p_fen.equals(""))
		{

//Log.i(TAG, "4 updateGui()");
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

//Log.i(TAG, "possibleMoves: " + possibleMoves + ", possibleMovesTo: " + possibleMovesTo + ", lastMove: " + lastMove + ", gc.move: " + gc.move);
//Log.i(TAG, "possibleMovesTo: " + possibleMovesTo + ", possibleMovesTo.size(): " + possibleMovesTo.size());

//Log.i(TAG, "3 updateGui(), lastMove: " + lastMove);
			boardView.updateBoardView(gc.cl.p_fen, gc.isBoardTurn, possibleMoves, possibleMovesTo,
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
//Log.i(TAG, "6 updateGui()");
        }
	}

	public class MyViewListener implements View.OnClickListener, View.OnLongClickListener
	{
		// chess960, random position
		public boolean onLongClick(View v)
		{
			if (v.getId() == R.id.btn_960)
			{
				removeDialog(PLAY_DIALOG);
				startEditBoard(gc.fen, true);
			}
			return true;
		}

		public void onClick(View v)
		{
			SharedPreferences.Editor ed = userPrefs.edit();
			switch (v.getId())
			{
				// PLAY_DIALOG
				case R.id.btn_ok:
//Log.i(TAG, "MyViewListener(), isPaused: " + isPaused + ", playMod: " + dChessEnginePlayMod + ", dNewGame: " + dNewGame);
					if (!isPaused)
					{
						removeDialog(PLAY_DIALOG);
						return;
					}

					setPlayModPrefs(dChessEnginePlayMod);

					gc.isGameLoaded = false;
					msgEngine.setVisibility(TextView.GONE);
					messageInfo 		= "";
					messageEngine 		= "";
					messageEngineShort  = "";
					ec.chessEngineAutoRun = false;
					twoPlayerPaused = false;
					ec.chessEnginePlayMod = userPrefs.getInt("user_play_playMod", 1);
					if (ec.chessEnginePlayMod == 4)
						ec.initClockAfterAnalysis = true;
					switch (userPrefs.getInt("user_play_playMod", 1))
					{
						case 1:     // white
						case 2:     // black
						case 3:     // engine vs engine
							ec.chessEnginePaused = false;
							ec.chessEngineInit = false;
							startPlay(dNewGame, true);
							break;
						case 4:     // analysis
							ec.chessEnginePaused = false;
							ec.chessEngineInit = false;
							updateCurrentPosition("");
							startPlay(dNewGame, true);
							break;
						case 5:     // two players
							analysisMessage = "";
							startEdit(dNewGame, true);
							break;
						case 6:     // edit
							stopChessClock();
							analysisMessage = "";
							ec.chessEnginePaused = true;
							startEdit(dNewGame, false);
							break;
					}
					removeDialog(PLAY_DIALOG);
					break;

				case R.id.cb_newGame:
					if (!isPaused)
						setPauseEnginePlay(false);
					isPaused = true;
					if ( ((CheckBox)v).isChecked() )
						dNewGame = true;
					else
						dNewGame = false;
					break;

				case R.id.btn_menu:
					removeDialog(PLAY_DIALOG);
					drawerLayout.openDrawer(Gravity.LEFT);
					break;

				case R.id.btn_960:
					if (!ec.chessEnginePaused)
						pauseStopPlay(false);
					Random r;
					int ir = 518;
					while (ir == 518)
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
						if (gc.cl.p_chess960ID == 518)
							gc.isChess960 = false;
						else
							gc.isChess960 = true;
						ed.putInt("user_game_chess960Id", gc.cl.p_chess960ID);
						ed.commit();
						setInfoMessage("", "", "", false);
						updateGui();
						startPlay(true, true);
					}
					removeDialog(PLAY_DIALOG);
					break;
                case R.id.btn_white:
					if (!isPaused)
						setPauseEnginePlay(false);
					isPaused = true;
                    dChessEnginePlayMod = 1; setPlayModBackground(dChessEnginePlayMod);
                    break;
                case R.id.btn_black:
					if (!isPaused)
						setPauseEnginePlay(false);
					isPaused = true;
                    dChessEnginePlayMod = 2; setPlayModBackground(dChessEnginePlayMod);
                    break;
                case R.id.btn_engine:
					if (!isPaused)
						setPauseEnginePlay(false);
					isPaused = true;
                    dChessEnginePlayMod = 3; setPlayModBackground(dChessEnginePlayMod);
                    break;
                case R.id.btn_player:
					if (!isPaused)
						setPauseEnginePlay(false);
					isPaused = true;
                    dChessEnginePlayMod = 5; setPlayModBackground(dChessEnginePlayMod);
                    break;
                case R.id.btn_edit:
					if (!isPaused)
						setPauseEnginePlay(false);
					isPaused = true;
                    dChessEnginePlayMod = 6; setPlayModBackground(dChessEnginePlayMod);
                    break;
                case R.id.btn_analysis:
					if (!isPaused)
						setPauseEnginePlay(false);
					isPaused = true;
                    dChessEnginePlayMod = 4; setPlayModBackground(dChessEnginePlayMod);
                    break;

				case R.id.cb_fullScreen:
					u.updateFullscreenStatus(MainActivity.this, ((CheckBox)v).isChecked());
					ed.putBoolean("user_options_gui_StatusBar", ((CheckBox)v).isChecked());
					ed.commit();
					break;
				case R.id.cb_lastPosition:
					ed.putBoolean("user_options_gui_LastPosition", ((CheckBox)v).isChecked());
					ed.commit();
					break;
				case R.id.cb_screenTimeout:
					ed.putBoolean("user_options_gui_disableScreenTimeout", ((CheckBox)v).isChecked());
					ed.commit();
					useWakeLock = !userPrefs.getBoolean("user_options_gui_disableScreenTimeout", false);
					setWakeLock(useWakeLock);
					break;

				case R.id.cb_audio:
					ed.putBoolean("user_options_gui_enableSounds", ((CheckBox)v).isChecked());
					ed.commit();
					if (((CheckBox)v).isChecked())
						playSound(1, 0);
					break;
				case R.id.cb_posibleMoves:
					ed.putBoolean("user_options_gui_posibleMoves", ((CheckBox)v).isChecked());
					ed.commit();
					updateGui();
					break;
				case R.id.cb_quickMove:
					ed.putBoolean("user_options_gui_quickMove", ((CheckBox)v).isChecked());
					ed.commit();
					updateGui();
					break;

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
				case R.id.cb_pgnDb:
					ed.putBoolean("user_options_gui_usePgnDatabase", ((CheckBox)v).isChecked());
					ed.commit();
					updateGui();
					break;

				case R.id.cb_engineAutostart:
					ed.putBoolean("user_options_enginePlay_AutoStartEngine", ((CheckBox)v).isChecked());
					ed.commit();
					break;
				case R.id.cb_openingBook:
					ed.putBoolean("user_options_enginePlay_OpeningBook", ((CheckBox)v).isChecked());
					ed.commit();
					msgEngine.setVisibility(TextView.VISIBLE);
					if 	(		msgEngine.getText().toString().startsWith(getString(R.string.engine_openingBook))
							| 	msgEngine.getText().toString().startsWith(getString(R.string.play_openingBookHints))
							| 	msgEngine.getText().toString().equals("")
						)
					{
						if (userPrefs.getBoolean("user_options_enginePlay_OpeningBook", true))
							msgEngine.setText(getString(R.string.engine_openingBook) + "\n" + getString(R.string.enabled));
						else
							msgEngine.setText(getString(R.string.engine_openingBook) + "\n" + getString(R.string.disabled));
					}
					break;
				case R.id.cb_openingBookHints:
					ed.putBoolean("user_options_enginePlay_ShowBookHints", ((CheckBox)v).isChecked());
					ed.commit();
					msgEngine.setVisibility(TextView.VISIBLE);
					if 	(		msgEngine.getText().toString().startsWith(getString(R.string.engine_openingBook))
							| 	msgEngine.getText().toString().startsWith(getString(R.string.play_openingBookHints))
							| 	msgEngine.getText().toString().equals("")
						)
					{
						if (userPrefs.getBoolean("user_options_enginePlay_ShowBookHints", true))
							msgEngine.setText(getString(R.string.play_openingBookHints) + "\n" + getString(R.string.enabled));
						else
							msgEngine.setText(getString(R.string.play_openingBookHints) + "\n" + getString(R.string.disabled));
					}
					break;
				case R.id.cb_engineThinking:
					if (!((CheckBox)v).isChecked())
						Toast.makeText(MainActivity.this, getString(R.string.displayDisabled), Toast.LENGTH_SHORT).show();
					ed.putBoolean("user_options_enginePlay_EngineMessage", ((CheckBox)v).isChecked());
					ed.commit();
					updateGui();
					break;
				case R.id.cb_ponder:
					ed.putBoolean("user_options_enginePlay_Ponder", ((CheckBox)v).isChecked());
					ed.commit();
					if (userPrefs.getBoolean("user_options_enginePlay_Ponder", false))
					{
						Toast.makeText(MainActivity.this, getString(R.string.epPonder) + " " + getString(R.string.enabled), Toast.LENGTH_SHORT).show();
					}
					else
					{
						Toast.makeText(MainActivity.this, getString(R.string.epPonder) + " " + getString(R.string.disabled), Toast.LENGTH_SHORT).show();
						if (ec.chessEngineSearchingPonder)
						{
							setPauseEnginePlay(false);
							initPonder();
							startChessClock();
							messageEngineShort = "";
							ec.chessEnginePaused = false;
						}
						else
							updateGui();
					}
					setInfoMessage(getEngineThinkingMessage(), null, null, false);
					break;
				case R.id.cb_moveList:
					ed.putBoolean("user_options_gui_moveList", ((CheckBox)v).isChecked());
					ed.commit();
					updateGui();
					break;

				// PAUSE_DIALOG
				case R.id.btn_pause_ok:
					if (isSearching)
						pauseStopPlay(true);
					else
						pauseStartPlay(dChessEnginePlayMod, dContinueId);
					break;
				case R.id.btn_timeWhite:
					restartPauseDialog = true;
					setTimeWhiteBlack(1);
					break;
				case R.id.btn_timeBlack:
					restartPauseDialog = true;
					setTimeWhiteBlack(2);
					break;
				case R.id.btn_timeControl:
					restartPauseDialog = true;
					removeDialog(PAUSE_DIALOG);
					startActivityForResult(optionsTimeControlIntent, OPTIONS_TIME_CONTROL_REQUEST_CODE);
					break;
				case R.id.btn_autom_play:
					startStopAutoPlay();
					setPauseModValues(dChessEnginePlayMod, pausePlayMod, ec.chessEnginePaused, dContinueId);

					break;
				case R.id.btn_delay:
					restartPauseDialog = true;
					chessClockMessage = getString(R.string.ccsMessageAutoPlay);
					chessClockControl = 41;
					chessClockTimeGame = -1;
					chessClockTimeBonus = userPrefs.getInt("user_options_timer_autoPlay", 1500);
					c4aShowDialog(TIME_SETTINGS_DIALOG);
					break;
				case R.id.btn_newGame:
					if (isSearching)
						pauseStopPlay(true);
					dContinueId = 1;
                    dChessEnginePlayMod = pausePlayMod;
					setTimeValuesFromPauseMod(pausePlayMod, dContinueId);
					setPauseModValues(dChessEnginePlayMod, pausePlayMod, ec.chessEnginePaused, dContinueId);

					break;
				case R.id.btn_setClock:
					if (isSearching)
						pauseStopPlay(true);
					dContinueId = 2;
                    dChessEnginePlayMod = pausePlayMod;
					setTimeValuesFromPauseMod(pausePlayMod, dContinueId);
					setPauseModValues(dChessEnginePlayMod, pausePlayMod, ec.chessEnginePaused, dContinueId);
					break;
				case R.id.btn_continue:
					if (isSearching)
						pauseStopPlay(true);
					dContinueId = 3;
                    dChessEnginePlayMod = pausePlayMod;
					setTimeValuesFromPauseMod(pausePlayMod, dContinueId);
					setPauseModValues(dChessEnginePlayMod, pausePlayMod, ec.chessEnginePaused, dContinueId);
					break;
				case R.id.btn_pause_edit:
					if (isSearching)
						pauseStopPlay(true);
					dChessEnginePlayMod = 6;
					setPauseModValues(dChessEnginePlayMod, pausePlayMod, ec.chessEnginePaused, dContinueId);
					break;
				case R.id.btn_pause_analysis:
					if (isSearching)
						pauseStopPlay(true);
					dChessEnginePlayMod = 4;
					setPauseModValues(dChessEnginePlayMod, pausePlayMod, ec.chessEnginePaused, dContinueId);
					break;
				case R.id.btn_pause_menu:
					removeDialog(PAUSE_DIALOG);
					drawerLayout.openDrawer(Gravity.RIGHT);
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
	final long CLOCK_DELAY = 250;
	final long MAX_DOWNLOAD_DIFF = 20000;

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

	// pause control (manually | automatically)
	boolean 		pause_auto 					= false;
	CharSequence 	pause_fen 					= "";
	int 			pause_mode 					= 0;
	CharSequence 	pause_messageEngine 		= "";

	GameControl gc;
	EngineControl ec;
	TimeControl tc;
	Chess960 chess960;
	ColorValues cv;

	ChessPromotion promotionDialog;
	FileIO fileIO;
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
	Intent optionsColorIntent;
	Intent editChessBoardIntent;

	//	subActivities RequestCode
	final static int LOAD_GAME_REQUEST_CODE = 1;
	final static int LOAD_GAME_PREVIOUS_CODE = 9;
	final static int SAVE_GAME_REQUEST_CODE = 2;
	final static int SAVE_LOAD_GAME_REQUEST_CODE = 7;
	final static int SAVE_OK_LOAD_GAME_REQUEST_CODE = 71;
	final static int SAVE_ERROR_LOAD_GAME_REQUEST_CODE = 72;
	final static int LOAD_EXTERN_ENGINE_REQUEST_CODE = 81;
	final static int LOAD_INTERN_ENGINE_REQUEST_CODE = 82;
	final static int GAME_DATA_REQUEST_CODE = 49;
	final static int NOTATION_REQUEST_CODE = 5;
	final static int MOVETEXT_REQUEST_CODE = 6;
	final static int OPTIONS_CHESSBOARD_REQUEST_CODE = 13;
	final static int OPTIONS_GUI_REQUEST_CODE = 14;
	final static int OPTIONS_ENGINE_AUTO_PLAY_REQUEST_CODE = 15;
	final static int OPTIONS_TIME_CONTROL_REQUEST_CODE = 18;
	final static int OPTIONS_PLAY_REQUEST_CODE = 19;
	final static int OPTIONS_ENGINE_PLAY_REQUEST_CODE = 21;
	final static int OPTIONS_COLOR_SETTINGS = 22;
	final static int EDIT_CHESSBOARD_REQUEST_CODE = 17;
	final static int CHESS960_POSITION_REQUEST_CODE = 20;
	final static int ENGINE_SETTING_REQUEST_CODE = 41;
	final static int RATE_REQUEST_CODE = 42;

	//  dialogs RequestCode
	final static int PLAY_DIALOG = 100;
	final static int PAUSE_DIALOG = 109;
	final static int MOVE_NOTIFICATION_DIALOG = 110;
	final static int DOWNLOAD_ERROR_DIALOG = 195;
	final static int PGN_ERROR_DIALOG = 198;
	final static int FILE_LOAD_PROGRESS_DIALOG = 199;
	final static int VARIATION_DIALOG = 200;
	final static int NAG_DIALOG = 201;
	final static int PROMOTION_DIALOG = 300;
	final static int TIME_SETTINGS_DIALOG = 400;
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
	final static int MENU_COLOR_SETTINGS = 732;
	final static int MENU_SHOW_LIST = 733;
	final static int MENU_SELECT_ENGINE_FROM_OEX = 740;
	final static int INFO_DIALOG = 909;	// device info; not activated
	final static int RATE_DIALOG = 910;
	final static int C4A_NEW_DIALOG = 999;

	final static int FLAG_ACTIVITY_PGN_UPDATE = 268468224;

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
	int lastTouchID = 0;
	long touchTime = 0;
	long longTouchTime = 600;
	CharSequence searchTaskFen = "";
	CharSequence searchTaskMoves = "";
	boolean searchTaskRestart = false;
	long sleepTime = 100;

	long engineControlTime = 0;

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
	boolean isUpBtn = false;
	boolean isDownBtn4 = false;
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
	CharSequence 	messageEngine 		= "";
	CharSequence 	messageEngineShort 	= "";
	CharSequence engineMes = "";
	CharSequence engineStat = "";
	ArrayList<CharSequence> infoPv;
	ArrayList<CharSequence> infoMessage;
	int bestScore = 0;
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

	int mate = 0;

// c4aPonder
	boolean isGoPonder = false;		// set to true if start: go ponder
	CharSequence ponderMove = null;		// get ponderMove from computer move (bestmove e2e4 ponder e7e6)

	// PLAY_DIALOG
	CheckBox d_cb_fullScreen;
	CheckBox d_cb_lastPosition;
	CheckBox d_cb_screenTimeout;
	CheckBox d_cb_audio;
	CheckBox d_cb_posibleMoves;
	CheckBox d_cb_quickMove;
	CheckBox d_cb_coordinates;
	CheckBox d_cb_blindMode;
	CheckBox d_cb_pgnDb;
	CheckBox d_cb_engineAutostart;
	CheckBox d_cb_openingBook;
	CheckBox d_cb_openingBookHints;
	CheckBox d_cb_engineThinking;
	CheckBox d_cb_ponder;
	CheckBox d_cb_moveList;

	TextView d_btn_white;
	TextView d_btn_black;
	TextView d_btn_engine;
	TextView d_btn_player;
	TextView d_btn_edit;
	TextView d_btn_analysis;

	TextView d_btn_menu;
	TextView d_btn_960;
	CheckBox d_cb_newGame;
	TextView d_btn_ok;

	// PAUSE_DIALOG
	TextView d_btn_timeWhite;
	TextView d_btn_timeBlack;
	TextView d_btn_timeControl;
	TextView d_btn_autom_play;
	TextView d_btn_delay;

	TextView d_play_mode_name;
	TextView d_btn_newGame;
	TextView d_btn_setClock;
	TextView d_btn_continue;

	TextView d_btn_pause_edit;
	TextView d_btn_pause_analysis;

	TextView d_btn_pause_menu;
	TextView d_btn_pause_ok;

	// RATE_DIALOG
	TextView btn_rate;
	TextView btn_no;

	boolean isPaused = false;
	boolean restartPauseDialog = false;
	boolean dNewGame = false;
	int dChessEnginePlayMod = 1;

	int pausePlayMod = 1;
	boolean isSearching = false;
	boolean isStopAutoPlay = false;
	int dContinueId = 3; 	// 1 new game, 2 continue, set clock, 3 continue

}