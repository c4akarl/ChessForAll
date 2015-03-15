package ccc.chess.gui.chessforall;
import ccc.chess.book.ChessParseError;

import ccc.chess.book.Move;
import ccc.chess.book.TextIO;
import ccc.chess.logic.c4aservice.Chess960;
import ccc.chess.logic.c4aservice.ChessPosition;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.PowerManager;
import android.view.ContextMenu;
import android.view.Display;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.TextView.BufferType;
import android.widget.Toast;
import android.text.ClipboardManager;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.StyleSpan;
//import android.util.Log;

public class UiControl
{	//>210 	the user interface(sub activity from C4aMain) 
	//		all engine actions are running in Play-Modus(gc.gameStat == 3)
	public UiControl(C4aMain cM, GameControl gameC, EngineControl engineC, SharedPreferences userPrefs)				
    {	
		c4aM = cM;	// main thread for UI-actions !
		gc = gameC;
		ec = engineC;
		tc = new TimeControl();
		userP = userPrefs;
		runP = c4aM.getSharedPreferences("run", 0);		//	run Preferences
		fmPrefs = c4aM.getSharedPreferences("fm", 0);
		chess960 = new Chess960();	// needed for "create your own chess position"
		pgnIO = new PgnIO();
    }
	public void startC4a()																		
    {	// initializing application
    	c4aM.getRunPrefs();	// run preferences
    	PowerManager pm = (PowerManager)c4aM.getSystemService(Context.POWER_SERVICE);
    	c4aM.setWakeLock(false);
    	c4aM.wakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "c4a");
    	c4aM.wakeLock.setReferenceCounted(false);
    	c4aM.useWakeLock = userP.getBoolean("user_options_gui_disableScreenTimeout", false);
    	c4aM.setWakeLock(c4aM.useWakeLock);
        startGui();
        mSoundPool = new SoundPool(2, AudioManager.STREAM_RING, 100);
        soundsMap = new HashMap<Integer, Integer>();
        soundsMap.put(1, mSoundPool.load(c4aM, R.raw.move_ok, 1));
        soundsMap.put(2, mSoundPool.load(c4aM, R.raw.move_wrong, 1));
        gc.cl.history.setFigurineAlgebraicNotation(c4aM.getString(R.string.pieces));
        if (gc.initPrefs & userP.getBoolean("user_options_gui_StatusBar", false))
        {
        	gc.initPrefs = false;
     		updateFullscreenStatus(false);
        }
        setButtonPosition(btnScrollId);
	    getGameData(gc.fileBase, gc.filePath, gc.fileName, gc.startPgn, true, false, gc.startMoveIdx);
    	if (gc.isGameLoaded & !gc.filePath.equals(""))
    		viewModeContent = gc.filePath.toString() + gc.fileName + "\n" + getGameInfo();
	    if ((ec.chessEnginePlayMod == 3 | ec.chessEnginePlayMod == 4) & !gc.isGameOver & !gc.cl.p_variationEnd)
    		ec.chessEngineSearching = true;
		gc.startFen = gc.cl.history.getStartFen();
//		Log.i(TAG, "gc.startFen: " + gc.startFen);
    	if (gc.cl.p_chess960ID == 518)
			gc.isChess960 = false;
		else
			gc.isChess960 = true;
    	SharedPreferences.Editor ed = c4aM.fmP.edit();
    	if (!gc.fileBase.equals(""))
    	{
    		if (gc.fileBase.equals("assets/"))
    		{
    			ed.putString("fm_intern_path", gc.filePath.toString());
            	ed.putString("fm_intern_file", gc.fileName.toString());
    		}
    		else
    		{
    			if (!gc.fileBase.equals("url"))
    				ed.putString("fm_url", gc.filePath.toString());
    			else
    			{
    				ed.putString("fm_extern_path", gc.filePath.toString());
    	        	ed.putString("fm_extern_file", gc.fileName.toString());
    			}
    		}
    	}
    	ed.commit();
    	if (progressDialog != null)
    	{
	    	if (progressDialog.isShowing())
	     		c4aM.dismissDialog(FILE_LOAD_PROGRESS_DIALOG);
    	}
    	ec.chessEngineInit = true;
//    	if (gc.gameStat == 3)
    	{	//>215 start engine play if "AutoStartEngine" is enabled
	    	if (		userP.getBoolean("user_options_enginePlay_AutoStartEngine", true) 
	    			& 	!gc.isGameOver & !ec.chessEnginePaused)
	    	{	// autoStart engines after loading application
	    		ec.chessEnginePaused = false;
	    		ec.lastChessEnginePaused = false;
//	    		Log.i(TAG, "moveIdx: " + gc.cl.p_moveIdx );
//	    		Log.i(TAG, "history.getStartFen(): "  + gc.cl.history.getStartFen());
	    		setMoveTime();
	    		if (gc.cl.p_moveIdx == 0 & gc.cl.history.getStartFen().toString().contains("/8/8/8/8/"))	// move idx 0, new game
	    			startPlay(true);
	    		else
	    			startPlay(false);
	    	}
	    	else
	    	{
	    		if (!userP.getBoolean("user_options_enginePlay_AutoStartEngine", true))
	    		{
	    			ec.chessEnginePaused = true;
	    			setInfoMessage(false, true, null, getEnginePausedMessage(), null, null);
	    		}
	    		if (gc.isGameOver | gc.cl.p_variationEnd)
	    		{
	    			ec.chessEnginePaused = true;
					ec.chessEngineSearching = false;
					setInfoMessage(false, true, null, getEnginePausedMessage(), null, null);
	    		}
	    		else
	    		{
		    		if (ec.chessEnginePaused)
		    			setInfoMessage(false, true, null, getEnginePausedMessage(), null, null);
		    		else
		    		{
		    			if (gc.cl.p_message.equals(""))
		    				setInfoMessage(false, true, null, c4aM.getString(R.string.engine_pausedNotConnected), null, null);
		    			else
		    				setInfoMessage(false, true, null, gc.cl.p_message, null, null);
		    		}
	    		}
				ec.chessEnginePaused = true;
				ec.chessEngineSearching = false;
	    	}
    	}
    	updateCurrentPosition("");
    	getPgnFromIntent();
    }
//	MENU		MENU		MENU		MENU		MENU		MENU		MENU	
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) 
    {
		menuSelected = false;
		gc.move = "";
		if (v == btn_game)	
		{
			MenuInflater inflater = c4aM.getMenuInflater();
		    inflater.inflate(R.menu.c4a_menu_game, menu);
		    menu.setHeaderTitle(c4aM.getString(R.string.menu_info));
		    if (!gc.isGameLoaded & (ec.chessEnginePlayMod == 1 | ec.chessEnginePlayMod == 2))
		    {
			    menu.findItem(R.id.menu_player_resign).setVisible(true);
			    menu.findItem(R.id.menu_player_draw).setVisible(true);
		    }
		    else
		    {
				menu.findItem(R.id.menu_player_resign).setVisible(false);
				menu.findItem(R.id.menu_player_draw).setVisible(false);
		    }
		    if (gc.hasVariations)
		    	menu.findItem(R.id.menu_info_variations).setVisible(true);
		    else
		    	menu.findItem(R.id.menu_info_variations).setVisible(false);
		    menu.findItem(R.id.menu_info_moveNotation).setVisible(false);
		}
		
		if (v == btn_engines)	
		{
			MenuInflater inflater = c4aM.getMenuInflater();
		    inflater.inflate(R.menu.c4a_menu_engines, menu);
		    menu.setHeaderTitle(c4aM.getString(R.string.menu_enginesettings));
		    CharSequence e1 = c4aM.getString(R.string.menu_enginesettings_engine_1);	// displaying engineName on menuItem
	    	CharSequence e2 = c4aM.getString(R.string.menu_enginesettings_engine_2);
	    	if (ec.en_1.isBound)
	    	{
	    		en1Selected = false;
	    		e1 = c4aM.getString(R.string.menu_enginesettings_engine_1) + ": " + ec.en_1.engineName;
	    	}
	    	else
	    	{
	    		if (!userP.getString("user_play_engine_1", "").equals(""))
	    		{
	    			e1 = 	c4aM.getString(R.string.menu_enginesettings_engine_1) + " ("
	    					+ ec.getEngineAppNameFromEngineList(userP.getString("user_play_engine_1", "")) + ")";
	    		}
	    	}
	    	if (ec.en_2.isBound)
	    	{
	    		en2Selected = false;
	    		e2 = c4aM.getString(R.string.menu_enginesettings_engine_2) + ": " + ec.en_2.engineName;
	    	}
	    	else
	    	{
	    		if (!userP.getString("user_play_engine_2", "").equals(""))
	    		{
	    			e2 = 	c4aM.getString(R.string.menu_enginesettings_engine_2) + " (" 
	    					+ ec.getEngineAppNameFromEngineList(userP.getString("user_play_engine_2", "")) + ")";
	    		}
	    	}
	    	menu.findItem(R.id.menu_enginesettings_engine_1).setTitle(e1);
	    	menu.findItem(R.id.menu_enginesettings_engine_2).setTitle(e2);
	    	if (ec.en_1.isBound & ec.en_2.isBound)
	    		menu.findItem(R.id.menu_enginesettings_switch).setVisible(true);
	    	else
	    		menu.findItem(R.id.menu_enginesettings_switch).setVisible(false);
		}
		if (v == btn_settings)	
		{
			MenuInflater inflater = c4aM.getMenuInflater();
		    inflater.inflate(R.menu.c4a_menu_settings, menu);
		    menu.setHeaderTitle(c4aM.getString(R.string.menu_usersettings));
			if (!gc.isGameLoaded & ec.chessEnginePlayMod < 4)
			{
		    	menu.findItem(R.id.menu_usersettings_time_white).setVisible(true);
		    	menu.findItem(R.id.menu_usersettings_time_black).setVisible(true);
			}
		    else
		    {
		    	menu.findItem(R.id.menu_usersettings_time_white).setVisible(false);
		    	menu.findItem(R.id.menu_usersettings_time_black).setVisible(false);
		    }
		}
		if (v == btn_menu)	// main menu
		{
			MenuInflater inflater = c4aM.getMenuInflater();
		    inflater.inflate(R.menu.c4a_menu_main, menu);
		    menu.setHeaderTitle(c4aM.getString(R.string.menu_title));
			gc.move = "";
		}
    }
	public boolean onContextItemSelected(MenuItem item)
    {
		menuSelected = true;
		ec.chessEngineAutoRun = false;
    	SharedPreferences.Editor ed = userP.edit();
		ed.putBoolean("user_options_gui_FlipBoard", false);
		ed.commit();
        switch(item.getItemId())
        {
	        case R.id.menu_play:
	        case R.id.menu_edit:
	        case R.id.menu_pgn:
	        case R.id.menu_enginesettings:
	        case R.id.menu_usersettings:
	        case R.id.menu_about:
		    	if (!ec.chessEnginePaused & ec.chessEnginePlayMod == 4)
					pauseEnginePlay(0);			// start/stop engine play(analysis)
				else
					stopTimeHandler(false);
		    	if (item.getItemId() == R.id.menu_play) {c4aM.removeDialog(MENU_PLAY_DIALOG); c4aM.showDialog(MENU_PLAY_DIALOG);}
		    	if (item.getItemId() == R.id.menu_edit) {c4aM.removeDialog(MENU_EDIT_DIALOG); c4aM.showDialog(MENU_EDIT_DIALOG);}
		    	if (item.getItemId() == R.id.menu_pgn) {c4aM.removeDialog(MENU_PGN_DIALOG); c4aM.showDialog(MENU_PGN_DIALOG);}
		    	if (item.getItemId() == R.id.menu_enginesettings) {c4aM.removeDialog(MENU_ENGINES_DIALOG); c4aM.showDialog(MENU_ENGINES_DIALOG);}
		    	if (item.getItemId() == R.id.menu_usersettings) {c4aM.removeDialog(MENU_SETTINGS_DIALOG); c4aM.showDialog(MENU_SETTINGS_DIALOG);}
		    	if (item.getItemId() == R.id.menu_about) {c4aM.removeDialog(MENU_ABOUT_DIALOG); c4aM.showDialog(MENU_ABOUT_DIALOG);}
	        	return true;
	        
	        case R.id.menu_enginesettings_engine_1:		//>215 select engine 1
	        	selectEngine(1, ec.findEnginesOnDevice()); 
	        	return true;
	        case R.id.menu_enginesettings_engine_2:		//>216 select engine 2
	        	selectEngine(2, ec.findEnginesOnDevice()); 
	        	return true;
	        case R.id.menu_enginesettings_switch:		// switch E1 / E2
	        	switchEngines();
	        	return true;
	        case R.id.menu_enginesettings_settings:		// engine settings(E1, E2)
	        	selectEngine(9, ec.findEnginesOnDevice()); 
	        	return true;
	        case R.id.menu_enginesettings_playOptions:	// engine play options
	        	c4aM.startActivityForResult(optionsEnginePlayIntent, OPTIONS_ENGINE_PLAY_REQUEST_CODE);
	        	return true;
	        case R.id.menu_enginesettings_shutdown:		//>217 shutdown all engines
	        	ec.stopAllEngines();
	        	ec.setPlaySettings(userP);
				ec.chessEngineInit = true;
				ec.chessEnginePaused = true;
				ec.en_1.engineServiceName = "";
				ec.en_2.engineServiceName = "";
				ed.putString("user_play_engine_1", "");
				ed.putString("user_play_engine_2", "");
				ed.commit();
				en1Selected = false;
				en2Selected = false;
				setInfoMessage(false, true, null, c4aM.getString(R.string.engine_pausedNotConnected), null, null);
				updateCurrentPosition("");
	        	return true;
	        case R.id.menu_specialities_engine_autoplay:	// >>> menu engine
	        	c4aM.startActivityForResult(playEngineIntent, OPTIONS_ENGINE_AUTO_PLAY_REQUEST_CODE);
	        	return true;
	        case R.id.menu_find_engines:
	        	engineSearchIntent = new Intent(Intent.ACTION_VIEW);
	        	engineSearchIntent.setData(Uri.parse("market://search?q=ccc.chess.engine OR ccc.chess.engines"));
				c4aM.startActivityForResult(engineSearchIntent, ENGINE_SEARCH_REQUEST_CODE);
	        	return true;
	        	
	        case R.id.menu_player_resign:
	        	CharSequence result = "";
    			if (ec.chessEnginePlayMod == 1)
    				result = "0-1";
		    	else
		    		result = "1-0";
    			startC4aService315(result, "");
	            return true;
	        case R.id.menu_player_draw:
	        	if (ec.getEngine().statPvScore <= -500)
    				startC4aService315("1/2-1/2", "");
    			else
    			{
    				if (!ec.chessEnginePaused)
    					continuePausedEngine(false);
    				setInfoMessage(true, true, c4aM.getString(R.string.engineDeclinesDraw), null, null, null);
    			}
	            return true;    
	        case R.id.menu_info_variations:
				stopThreads(false);
				startVariation();
				c4aShowDialog(VARIATION_DIALOG);
	            return true;
	        case R.id.menu_info_moveNotification:
	        	startMoveText();
	            return true;
	        case R.id.menu_info_moveNotation:
				startNotation(3);
	            return true;
	        case R.id.menu_info_nag:
	        	stopThreads(false);
				c4aShowDialog(NAG_DIALOG);
	            return true;
	            
	        case R.id.menu_usersettings_gui:
	        	c4aM.startActivityForResult(optionsGuiIntent, OPTIONS_GUI_REQUEST_CODE);
	        	return true;	
	        case R.id.menu_usersettings_chessBoard:
	        	c4aM.startActivityForResult(optionsChessBoardIntent, OPTIONS_CHESSBOARD_REQUEST_CODE);
	        	return true;
	        case R.id.menu_usersettings_timeControl:
	        	c4aM.startActivityForResult(optionsTimeControlIntent, OPTIONS_TIME_CONTROL_REQUEST_CODE);
	        	return true;
	        case R.id.menu_usersettings_timerAutoPlay:
	        	chessClockMessage = c4aM.getString(R.string.ccsMessageAutoPlay);
				chessClockControl = 41;
				chessClockTimeGame = -1;  
				chessClockTimeBonus = userP.getInt("user_options_timer_autoPlay", 1500);
	        	c4aShowDialog(TIME_SETTINGS_DIALOG);
	        	return true;
	        case R.id.menu_usersettings_time_white:
				chessClockMessage = c4aM.getString(R.string.ccsMessageWhite);
				chessClockControl = 1;
				chessClockTimeGame = tc.timeWhite;
				chessClockTimeBonusSaveWhite = tc.bonusWhite;
				chessClockTimeBonus = -1;
	        	c4aShowDialog(TIME_SETTINGS_DIALOG);
	        	return true;
	        case R.id.menu_usersettings_time_black:
				chessClockMessage = c4aM.getString(R.string.ccsMessageBlack);
				chessClockControl = 2;
				chessClockTimeGame = tc.timeBlack;
				chessClockTimeBonusSaveBlack = tc.bonusBlack;
				chessClockTimeBonus = -1;
	        	c4aShowDialog(TIME_SETTINGS_DIALOG);
	        	return true;
		}
        return true;
    }
//	DIALOG		DIALOG		DIALOG		DIALOG		DIALOG		DIALOG		DIALOG
	public class OnPromotionListener implements ChessPromotion.MyDialogListener 							
    {	// Promotion Dialog - Listener 
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
//        	if (!gc.isGameLoaded)
        	if (!gc.isGameLoaded & !ec.chessEnginePaused)
        	{
        		ec.chessEngineSearching = true;
        		ec.chessEnginePaused = false;
        		ec.lastChessEnginePaused = false;
        		updateGui();
        		chessEngineBestMove(gc.cl.p_fen, "");
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
	public Dialog onCreateDialog(int id)
	{	// creating dialog
		if (id != PROMOTION_DIALOG)
			stopTimeHandler(false);
		activDialog = id;
		if (id == NO_CHESS_ENGINE_DIALOG)  
        {
			ec.chessEngineProblem = false;
			CharSequence notFound = "";
			if (!ec.getEngine().getEngineServiceIsReady())	
			{
//				Log.i(TAG, "NO_CHESS_ENGINE_DIALOG");
				if (ec.getEngine().getCurrentProcess().equals(""))
				{
					ec.chessEnginePaused = true;
					ec.chessEngineProblem = true;
					notFound = c4aM.getString(R.string.engineNotFound);
				}
				else
				{
//					Log.i(TAG, "NO_CHESS_ENGINE_DIALOG, engineAlreadyRunning");
					notFound = c4aM.getString(R.string.engineAlreadyRunning);
				}
			}
			updateGui();
			setInfoMessage(false, true, null, notFound, null, null);
            return c4aDialog;
        }
		if (id == PGN_ERROR_DIALOG)  
        {
			if (gc.isGameShow)
			{
//				Log.i(TAG, "PGN_ERROR: " + gc.filePath.toString() + gc.fileName + ", "
//						+ fmPrefs.getInt("fm_extern_db_game_id", 0) + "(" + fmPrefs.getInt("fm_extern_db_game_count", 1) + ")");
				String message = "PGN_ERROR: " + fmPrefs.getInt("fm_extern_db_game_id", 0) + "(" + fmPrefs.getInt("fm_extern_db_game_count", 1) + ")";
				writePgnErrorFile(message);
				return null;
			}
			String message = gc.errorMessage + "\n\n" + c4aM.getString(R.string.sendEmail);
			c4aImageDialog = new C4aImageDialog(c4aM, c4aM, c4aM.getString(R.string.dgTitleDialog), message, 
					R.drawable.button_ok, 0, R.drawable.button_cancel);
			c4aImageDialog.setOnCancelListener(new DialogInterface.OnCancelListener() 
			{
			public void onCancel(DialogInterface dialog) { onCancelDialog(); }
			});
			return c4aImageDialog;
        }
		if (id == FILE_LOAD_PROGRESS_DIALOG) 
        {
			progressDialog = new ProgressDialog(c4aM);
			progressDialog.setMessage(c4aM.getString(R.string.fmProgressDialog));
			progressDialog.setCancelable(true);
			progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() 
	        {
	            public void onCancel(DialogInterface dialog) { c4aM.finish(); }
	        });
	        return progressDialog;
        } 
		if (id == VARIATION_DIALOG)  
        {
			CharSequence[] items = new CharSequence[gc.variationsList.size()];
			if (gc.variationsList.size() > 0)
	    	  {
		    	  for (int i = 0; i < gc.variationsList.size(); i++)
		          {
		    		  items[i] = gc.chessMove.getVal(gc.variationsList.get(i), 5);
		          }
	    	  }
			AlertDialog.Builder builder = new AlertDialog.Builder(c4aM);
			builder.setItems(items, new DialogInterface.OnClickListener() 
			{
				
			    public void onClick(DialogInterface dialog, int item) 
			    {
			    	showNewVariation(item);
			    }
			});
			AlertDialog alert = builder.create();
            return alert;
        }
		if (id == NAG_DIALOG)  
        {
			final CharSequence[] items = new CharSequence[] 
				{	c4aM.getString(R.string.nag_$0), 
					c4aM.getString(R.string.nag_$10), 
					c4aM.getString(R.string.nag_$14), 
					c4aM.getString(R.string.nag_$16),
					c4aM.getString(R.string.nag_$18),
					c4aM.getString(R.string.nag_$15),
					c4aM.getString(R.string.nag_$17),
					c4aM.getString(R.string.nag_$19),
					c4aM.getString(R.string.nag_$1),
					c4aM.getString(R.string.nag_$2),
					c4aM.getString(R.string.nag_$3),
					c4aM.getString(R.string.nag_$4),
					c4aM.getString(R.string.nag_$5),
					c4aM.getString(R.string.nag_$6)
				};
			AlertDialog.Builder builder = new AlertDialog.Builder(c4aM);
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
			int listSize = 3;
			if (!opening.equals(""))
				listSize = 4;
			CharSequence[] items = new CharSequence[listSize];
			items[0] = white;
			items[1] = black;
			items[2] = event;
			if (!opening.equals(""))
				items[3] = opening;
			AlertDialog.Builder builder = new AlertDialog.Builder(c4aM);
			builder.setTitle(c4aM.getString(R.string.qDatabaseQuery));
			builder.setItems(items, new DialogInterface.OnClickListener() 
			{
			    public void onClick(DialogInterface dialog, int item) 
			    {
			    	switch (item) 										
					{
						case 0: queryControl = "w"; break;
						case 1: queryControl = "b"; break;
						case 2: queryControl = "e"; break;
						case 3: queryControl = "o"; break;
					}
			    	startFileManager(LOAD_GAME_REQUEST_CODE, 1, 1);
			    }
			});
			AlertDialog alert = builder.create();
            return alert;
        }
		if (id == PROMOTION_DIALOG)  
        {
			promotionDialog = new ChessPromotion(c4aM, new OnPromotionListener());
			return promotionDialog;
        }
		if (id == HELP_DIALOG)  
        {
			helpDialog = new HelpDialog(c4aM, c4aM, helpGameStat, helpTitle.toString(), helpText.toString());
			helpDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			return helpDialog;
        }
		if (id == TIME_SETTINGS_DIALOG)  
        {
			chessClockTitle = c4aM.getString(R.string.ccsTitle);
			timeSettingsDialog = new TimeSettingsDialog(c4aM, c4aM, chessClockTitle.toString(), chessClockMessage.toString(), 
					chessClockTimeGame, chessClockTimeBonus, chessClockMovesToGo);
			timeSettingsDialog.setOnCancelListener(new DialogInterface.OnCancelListener() 
	        {
	            public void onCancel(DialogInterface dialog) { onCancelDialog(); }
	        });
			return timeSettingsDialog;
        }
		if (id == ENGINE_SEARCH_DIALOG)  
        {
			setInfoMessage(false, true, null, c4aM.getString(R.string.engineNoEnginesOnDevice), null, null);
			c4aImageDialog = new C4aImageDialog(c4aM, c4aM, c4aM.getString(R.string.dgTitleDialog), c4aM.getString(R.string.engineSearchMessage), 
									R.drawable.button_ok, 0, R.drawable.button_cancel);
			c4aImageDialog.setOnCancelListener(new DialogInterface.OnCancelListener() 
	        {
	            public void onCancel(DialogInterface dialog) { onCancelDialog(); }
	        });
            return c4aImageDialog;
        }
		if (id == ENGINE_SEARCH_NO_INTERNET_DIALOG)  
        {
			c4aImageDialog = new C4aImageDialog(c4aM, c4aM, c4aM.getString(R.string.dgTitleDialog), c4aM.getString(R.string.engineSearchNoInternetMessage), 
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
            c4aImageDialog = new C4aImageDialog(c4aM, c4aM, runP.getString("infoTitle", ""), mes, 0, R.drawable.button_ok, 0);
            c4aImageDialog.setOnCancelListener(new DialogInterface.OnCancelListener() 
	        {
	            public void onCancel(DialogInterface dialog) { onCancelDialog(); }
	        });
			return c4aImageDialog;
        }
		if (id == MENU_PLAY_DIALOG) 
        {
			final int MENU_NEW_GAME     	= 0;
            final int MENU_PLAY_OPTIONS  	= 1;
            final int MENU_PLAYER_RESIGN    = 2;
            final int MENU_PLAYER_DRAW      = 3;
            final int MENU_VARIATIONS    	= 4;
            List<CharSequence> lst = new ArrayList<CharSequence>();
            List<Integer> actions = new ArrayList<Integer>();
            lst.add(c4aM.getString(R.string.menu_new_game));     		actions.add(MENU_NEW_GAME);
            lst.add(c4aM.getString(R.string.menu_play_options)); 		actions.add(MENU_PLAY_OPTIONS);
            if (!gc.isGameLoaded & (ec.chessEnginePlayMod == 1 | ec.chessEnginePlayMod == 2))
            {
	            lst.add(c4aM.getString(R.string.enginePlayerResign));   actions.add(MENU_PLAYER_RESIGN);
	            lst.add(c4aM.getString(R.string.enginePlayerDraw));     actions.add(MENU_PLAYER_DRAW);
            }
            if (gc.hasVariations)
            	{lst.add(c4aM.getString(R.string.menu_info_variations)); actions.add(MENU_VARIATIONS);}
            final List<Integer> finalActions = actions;
            AlertDialog.Builder builder = new AlertDialog.Builder(c4aM);
            builder.setTitle(R.string.menu_modes_view);
            builder.setItems(lst.toArray(new CharSequence[lst.size()]), new DialogInterface.OnClickListener() 
            {
                public void onClick(DialogInterface dialog, int item) 
                {
                    switch (finalActions.get(item)) 
                    {
                    case MENU_NEW_GAME:
                    	if (userP.getInt("user_play_playMod", 1) != 4)
        	        	{	// no new game if analysis
        	         		gc.isGameLoaded = false;
        	         		ec.chessEnginePaused = false;
        	         		ec.lastChessEnginePaused = false;
        		    		ec.chessEngineInit = false;
        		    		initChessClock();
        		    		startPlay(true);
        	        	}
        	        	else
        	        	{
        	        		stopThreads(false);
        	    			c4aM.startActivityForResult(optionsPlayIntent, OPTIONS_PLAY_REQUEST_CODE);
        	        	}
                    	break;
                    case MENU_PLAY_OPTIONS: 
                    	c4aM.startActivityForResult(optionsPlayIntent, OPTIONS_PLAY_REQUEST_CODE);
                        break;
                    case MENU_PLAYER_RESIGN: 
                    	CharSequence result = "";
            			if (ec.chessEnginePlayMod == 1)
            				result = "0-1";
        		    	else
        		    		result = "1-0";
            			startC4aService315(result, "");
                        break;
                    case MENU_PLAYER_DRAW:
                    	if (ec.getEngine().statPvScore <= -500)
            				startC4aService315("1/2-1/2", "");
            			else
            			{
            				if (!ec.chessEnginePaused)
            					continuePausedEngine(false);
            				setInfoMessage(true, true, c4aM.getString(R.string.engineDeclinesDraw), null, null, null);
            			}
                        break;
                    case MENU_VARIATIONS:
                    	stopThreads(false);
        				startVariation();
        				c4aShowDialog(VARIATION_DIALOG);
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
            List<CharSequence> lst = new ArrayList<CharSequence>();
            List<Integer> actions = new ArrayList<Integer>();
            lst.add(c4aM.getString(R.string.menu_edit_board));     			actions.add(MENU_EDIT_BOARD);
            lst.add(c4aM.getString(R.string.menu_pgn_edit)); 				actions.add(MENU_EDIT_PGN);
            lst.add(c4aM.getString(R.string.menu_info_moveNotification));   actions.add(MENU_EDIT_NOTIFICATION);
            lst.add(c4aM.getString(R.string.menu_info_nag));     			actions.add(MENU_EDIT_NAG);
        	lst.add(c4aM.getString(R.string.menu_info_moveNotation)); 		actions.add(MENU_EDIT_NOTATION);
        	lst.add(c4aM.getString(R.string.menu_info_turnBoard)); 			actions.add(MENU_EDIT_TURN_BOARD);
            final List<Integer> finalActions = actions;
            AlertDialog.Builder builder = new AlertDialog.Builder(c4aM);
            builder.setTitle(R.string.menu_modes_edit);
            builder.setItems(lst.toArray(new CharSequence[lst.size()]), new DialogInterface.OnClickListener() 
            {
                public void onClick(DialogInterface dialog, int item) 
                {
                    switch (finalActions.get(item)) 
                    {
                    case MENU_EDIT_BOARD:
                    	editChessBoardIntent.putExtra("currentFen", gc.fen);
        	        	editChessBoardIntent.putExtra("gridViewSize", gridViewSize);
        	        	editChessBoardIntent.putExtra("fieldSize", getChessFieldSize());
        	        	c4aM.startActivityForResult(editChessBoardIntent, EDIT_CHESSBOARD_REQUEST_CODE);
                    	break;
                    case MENU_EDIT_PGN: 
        	        	startGameData();
                        break;
                    case MENU_EDIT_NOTIFICATION: 
                    	startMoveText();
                        break;
                    case MENU_EDIT_NAG:
                    	stopThreads(false);
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
            final int MENU_PGN_DELETE    		= 2;
            final int MENU_PGN_DOWNLOAD    		= 3;
            final int MENU_PGN_PDB    			= 4;
            final int MENU_PGN_CB_COPY    		= 5;
            final int MENU_PGN_CB_COPY_POS 		= 6;
            final int MENU_PGN_CB_PAST    		= 7;
            List<CharSequence> lst = new ArrayList<CharSequence>();
            List<Integer> actions = new ArrayList<Integer>();
            lst.add(c4aM.getString(R.string.menu_pgn_load));     			actions.add(MENU_PGN_LOAD);
            lst.add(c4aM.getString(R.string.menu_pgn_save)); 				actions.add(MENU_PGN_SAVE);
            lst.add(c4aM.getString(R.string.menu_pgn_delete));   			actions.add(MENU_PGN_DELETE);
            lst.add(c4aM.getString(R.string.menu_load_www));     			actions.add(MENU_PGN_DOWNLOAD);
            if (runP.getBoolean("run_isActivate", false))
            	{lst.add("PDB to PGN"); 									actions.add(MENU_PGN_PDB);}
        	lst.add(c4aM.getString(R.string.menu_info_clipboardCopyPgn)); 	actions.add(MENU_PGN_CB_COPY);
        	lst.add(c4aM.getString(R.string.menu_info_clipboardCopyFen)); 	actions.add(MENU_PGN_CB_COPY_POS);
        	lst.add(c4aM.getString(R.string.menu_info_clipboardPaste)); 	actions.add(MENU_PGN_CB_PAST);
            final List<Integer> finalActions = actions;
            AlertDialog.Builder builder = new AlertDialog.Builder(c4aM);
            builder.setTitle(R.string.menu_pgn);
            builder.setItems(lst.toArray(new CharSequence[lst.size()]), new DialogInterface.OnClickListener() 
            {
                public void onClick(DialogInterface dialog, int item) 
                {
                    switch (finalActions.get(item)) 
                    {
                    case MENU_PGN_LOAD:
                    	startFileManager(LOAD_GAME_REQUEST_CODE, 1, 0);
                    	break;
                    case MENU_PGN_SAVE: 
                    	startSaveGame(1);
                    	break;
                    case MENU_PGN_DELETE: 
                    	startFileManager(DELETE_GAME_REQUEST_CODE, 1, 0);
                        break;
                    case MENU_PGN_DOWNLOAD:
                    	startPgnDownload();
                        break;
                    case MENU_PGN_PDB:
                    	c4aM.startActivityForResult(pdbToPgnIntent, PDB_TO_PGN_CODE);
                        break;
                    case MENU_PGN_CB_COPY:
                    	setToClipboard(gc.cl.history.createPgnFromHistory(1));
                        break;
                    case MENU_PGN_CB_COPY_POS:
                    	setToClipboard(gc.cl.history.getMoveFen(gc.cl.history.getMoveIdx()));
                        break;
                    case MENU_PGN_CB_PAST:
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
			final int MENU_ENGINE_E1     		= 0;
            final int MENU_ENGINE_E2 			= 1;
            final int MENU_ENGINE_SWITCH   		= 2;
            final int MENU_ENGINE_SETTINGS 		= 3;
            final int MENU_ENGINE_PLAY_OPTIONS	= 4;
            final int MENU_ENGINE_AUTOPLAY 		= 5;
            final int MENU_ENGINE_SHUTDOWN 		= 6;
            final int MENU_ENGINE_FIND    		= 7;
            List<CharSequence> lst = new ArrayList<CharSequence>();
            List<Integer> actions = new ArrayList<Integer>();
            CharSequence e1 = c4aM.getString(R.string.menu_enginesettings_engine_1);	// displaying engineName on menuItem
	    	CharSequence e2 = c4aM.getString(R.string.menu_enginesettings_engine_2);
	    	if (ec.en_1.isBound)
	    	{
	    		en1Selected = false;
	    		e1 = c4aM.getString(R.string.menu_enginesettings_engine_1) + ": " + ec.en_1.engineName;
	    	}
	    	else
	    	{
	    		if (!userP.getString("user_play_engine_1", "").equals(""))
	    		{
	    			e1 = 	c4aM.getString(R.string.menu_enginesettings_engine_1) + " ("
	    					+ ec.getEngineAppNameFromEngineList(userP.getString("user_play_engine_1", "")) + ")";
	    		}
	    	}
	    	if (ec.en_2.isBound)
	    	{
	    		en2Selected = false;
	    		e2 = c4aM.getString(R.string.menu_enginesettings_engine_2) + ": " + ec.en_2.engineName;
	    	}
	    	else
	    	{
	    		if (!userP.getString("user_play_engine_2", "").equals(""))
	    		{
	    			e2 = 	c4aM.getString(R.string.menu_enginesettings_engine_2) + " (" 
	    					+ ec.getEngineAppNameFromEngineList(userP.getString("user_play_engine_2", "")) + ")";
	    		}
	    	}
            lst.add(e1);     														actions.add(MENU_ENGINE_E1);
            lst.add(e2); 															actions.add(MENU_ENGINE_E2);
            if (ec.en_1.isBound & ec.en_2.isBound)
            	{lst.add(c4aM.getString(R.string.menu_enginesettings_switch));   	actions.add(MENU_ENGINE_SWITCH);}
            lst.add(c4aM.getString(R.string.menu_enginesettings_settings));     	actions.add(MENU_ENGINE_SETTINGS);
            lst.add(c4aM.getString(R.string.menu_enginesettings_playOptions));     	actions.add(MENU_ENGINE_PLAY_OPTIONS);
         	lst.add(c4aM.getString(R.string.menu_specialities_engine_autoplay)); 	actions.add(MENU_ENGINE_AUTOPLAY);
        	lst.add(c4aM.getString(R.string.menu_enginesettings_shutdown)); 		actions.add(MENU_ENGINE_SHUTDOWN);
        	lst.add(c4aM.getString(R.string.menu_find_engines)); 					actions.add(MENU_ENGINE_FIND);
            final List<Integer> finalActions = actions;
            AlertDialog.Builder builder = new AlertDialog.Builder(c4aM);
            builder.setTitle(R.string.menu_enginesettings);
            builder.setItems(lst.toArray(new CharSequence[lst.size()]), new DialogInterface.OnClickListener() 
            {
                public void onClick(DialogInterface dialog, int item) 
                {
                    switch (finalActions.get(item)) 
                    {
                    case MENU_ENGINE_E1:
                    	selectEngine(1, ec.findEnginesOnDevice()); 
                    	break;
                    case MENU_ENGINE_E2: 
                    	selectEngine(2, ec.findEnginesOnDevice());
                    	break;
                    case MENU_ENGINE_SWITCH: 
                    	switchEngines();
                        break;
                    case MENU_ENGINE_SETTINGS:
                    	selectEngine(9, ec.findEnginesOnDevice());
                        break;
                    case MENU_ENGINE_PLAY_OPTIONS:
                    	c4aM.startActivityForResult(optionsEnginePlayIntent, OPTIONS_ENGINE_PLAY_REQUEST_CODE);
                        break;
                    case MENU_ENGINE_AUTOPLAY:
                    	c4aM.startActivityForResult(playEngineIntent, OPTIONS_ENGINE_AUTO_PLAY_REQUEST_CODE);
                        break;
                    case MENU_ENGINE_SHUTDOWN:
                    	ec.stopAllEngines();
        	        	ec.setPlaySettings(userP);
        				ec.chessEngineInit = true;
        				ec.chessEnginePaused = true;
        				ec.en_1.engineServiceName = "";
        				ec.en_2.engineServiceName = "";
        				SharedPreferences.Editor ed = userP.edit();
        				ed.putString("user_play_engine_1", "");
        				ed.putString("user_play_engine_2", "");
        				ed.commit();
        				en1Selected = false;
        				en2Selected = false;
        				setInfoMessage(false, true, null, c4aM.getString(R.string.engine_pausedNotConnected), null, null);
        				updateCurrentPosition("");
                        break;
                    case MENU_ENGINE_FIND:
                    	engineSearchIntent = new Intent(Intent.ACTION_VIEW);
        	        	engineSearchIntent.setData(Uri.parse("market://search?q=ccc.chess.engine OR ccc.chess.engines"));
        				c4aM.startActivityForResult(engineSearchIntent, ENGINE_SEARCH_REQUEST_CODE);
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
            final int MENU_SETTINGS_BOARD			= 1;
            final int MENU_SETTINGS_TIME_CONTROL    = 2;
            final int MENU_SETTINGS_TIMER_AUTOPLAY	= 3;
            final int MENU_SETTINGS_TIME_WHITE 		= 4;
            final int MENU_SETTINGS_TIME_BLACK 		= 5;
            List<CharSequence> lst = new ArrayList<CharSequence>();
            List<Integer> actions = new ArrayList<Integer>();
            lst.add(c4aM.getString(R.string.menu_usersettings_gui));     			actions.add(MENU_SETTINGS_GUI);
            lst.add(c4aM.getString(R.string.menu_usersettings_chessBoard)); 		actions.add(MENU_SETTINGS_BOARD);
            lst.add(c4aM.getString(R.string.menu_usersettings_timeControl));   		actions.add(MENU_SETTINGS_TIME_CONTROL);
            lst.add(c4aM.getString(R.string.menu_usersettings_timerAutoPlay));     	actions.add(MENU_SETTINGS_TIMER_AUTOPLAY);
            if (!gc.isGameLoaded & ec.chessEnginePlayMod < 4)
            {
	        	lst.add(c4aM.getString(R.string.menu_usersettings_time_white)); 	actions.add(MENU_SETTINGS_TIME_WHITE);
	        	lst.add(c4aM.getString(R.string.menu_usersettings_time_black)); 	actions.add(MENU_SETTINGS_TIME_BLACK);
            }
            final List<Integer> finalActions = actions;
            AlertDialog.Builder builder = new AlertDialog.Builder(c4aM);
            builder.setTitle(R.string.menu_usersettings);
            builder.setItems(lst.toArray(new CharSequence[lst.size()]), new DialogInterface.OnClickListener() 
            {
                public void onClick(DialogInterface dialog, int item) 
                {
                    switch (finalActions.get(item)) 
                    {
                    case MENU_SETTINGS_GUI:
                    	c4aM.startActivityForResult(optionsGuiIntent, OPTIONS_GUI_REQUEST_CODE);
                    	break;
                    case MENU_SETTINGS_BOARD: 
                    	c4aM.startActivityForResult(optionsChessBoardIntent, OPTIONS_CHESSBOARD_REQUEST_CODE);
                        break;
                    case MENU_SETTINGS_TIME_CONTROL: 
                    	c4aM.startActivityForResult(optionsTimeControlIntent, OPTIONS_TIME_CONTROL_REQUEST_CODE);
                        break;
                    case MENU_SETTINGS_TIMER_AUTOPLAY:
                    	chessClockMessage = c4aM.getString(R.string.ccsMessageAutoPlay);
        				chessClockControl = 41;
        				chessClockTimeGame = -1;  
        				chessClockTimeBonus = userP.getInt("user_options_timer_autoPlay", 1500);
        	        	c4aShowDialog(TIME_SETTINGS_DIALOG);
                        break;
                    case MENU_SETTINGS_TIME_WHITE:
                    	chessClockMessage = c4aM.getString(R.string.ccsMessageWhite);
        				chessClockControl = 1;
        				chessClockTimeGame = tc.timeWhite;
        				chessClockTimeBonusSaveWhite = tc.bonusWhite;
        				chessClockTimeBonus = -1;
        	        	c4aShowDialog(TIME_SETTINGS_DIALOG);
                        break;
                    case MENU_SETTINGS_TIME_BLACK:
                    	chessClockMessage = c4aM.getString(R.string.ccsMessageBlack);
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
            final int MENU_ABOUT_USER_MANUAL	= 1;
            final int MENU_ABOUT_C4A    		= 2;
            final int MENU_ABOUT_DEVELOPER 		= 3;
            final int MENU_ABOUT_APPS	    	= 4;
            final int MENU_ABOUT_WEBSITE    	= 5;
            final int MENU_ABOUT_SOURCECODE    	= 6;
            final int MENU_ABOUT_CONTACT    	= 7;
            List<CharSequence> lst = new ArrayList<CharSequence>();
            List<Integer> actions = new ArrayList<Integer>();
            lst.add(c4aM.getString(R.string.menu_about_new));     			actions.add(MENU_ABOUT_NEW);
            lst.add(c4aM.getString(R.string.menu_about_userManual)); 		actions.add(MENU_ABOUT_USER_MANUAL);
            lst.add(c4aM.getString(R.string.menu_about_c4a));   			actions.add(MENU_ABOUT_C4A);
            lst.add(c4aM.getString(R.string.menu_about_developer));     	actions.add(MENU_ABOUT_DEVELOPER);
        	lst.add(c4aM.getString(R.string.menu_about_apps)); 				actions.add(MENU_ABOUT_APPS);
        	lst.add(c4aM.getString(R.string.menu_about_website)); 			actions.add(MENU_ABOUT_WEBSITE);
        	lst.add(c4aM.getString(R.string.menu_about_sourcecode)); 		actions.add(MENU_ABOUT_SOURCECODE);
        	lst.add(c4aM.getString(R.string.menu_about_contact)); 			actions.add(MENU_ABOUT_CONTACT);
            final List<Integer> finalActions = actions;
            AlertDialog.Builder builder = new AlertDialog.Builder(c4aM);
            builder.setTitle(R.string.menu_about);
            builder.setItems(lst.toArray(new CharSequence[lst.size()]), new DialogInterface.OnClickListener() 
            {
                public void onClick(DialogInterface dialog, int item) 
                {
                    switch (finalActions.get(item)) 
                    {
                    case MENU_ABOUT_NEW:
                    	showHelpDialog(10, c4aM.getString(R.string.whatsNew), c4aM.getString(R.string.whatsNew_text));
                    	break;
                    case MENU_ABOUT_USER_MANUAL: 
                    	startUserManual();
                        break;
                    case MENU_ABOUT_C4A: 
                    	showHelpDialog(10, c4aM.getString(R.string.m_about_c4a), c4aM.getString(R.string.m_about_c4a_text));
                        break;
                    case MENU_ABOUT_DEVELOPER:
                    	showHelpDialog(10, c4aM.getString(R.string.m_about_developer), c4aM.getString(R.string.m_about_developer_text));
                        break;
                    case MENU_ABOUT_APPS:
                    	Intent ir = new Intent(Intent.ACTION_VIEW);
        				ir.setData(Uri.parse("market://search?q=pub:Karl Schreiner"));
        				c4aM.startActivityForResult(ir, RATE_REQUEST_CODE);
                        break;
                    case MENU_ABOUT_WEBSITE:
                    	Intent irw = new Intent(Intent.ACTION_VIEW);
        				irw.setData(Uri.parse("http://c4akarl.blogspot.com/"));
        				c4aM.startActivityForResult(irw, RATE_REQUEST_CODE);
                        break;
                    case MENU_ABOUT_SOURCECODE:
                        Intent irs = new Intent(Intent.ACTION_VIEW);
                        irs.setData(Uri.parse("https://github.com/c4akarl/ChessForAll"));
                        c4aM.startActivityForResult(irs, RATE_REQUEST_CODE);
                        break;
                    case MENU_ABOUT_CONTACT:
                        Intent send = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", APP_EMAIL.toString(), null));
                        send.putExtra(android.content.Intent.EXTRA_SUBJECT, "");
                        send.putExtra(android.content.Intent.EXTRA_TEXT, "");
                        c4aM.startActivity(Intent.createChooser(send, c4aM.getString(R.string.sendEmail)));
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
		ec.chessEnginePaused = ec.lastChessEnginePaused;
		if (!ec.chessEnginePaused)
			continuePausedEngine(false);
	}
	public void getCallbackValue(int btnValue) 
	{
		ec.chessEnginePaused = ec.lastChessEnginePaused;
		if (activDialog == NO_CHESS_ENGINE_DIALOG)
		{
			if (btnValue == 2)	// ok
    		{
//				ec.stopAllEngines();
				ec.setPlaySettings(userP);
				ec.chessEngineInit = true;
				ec.chessEnginePaused = true;
				setInfoMessage(false, true, null, c4aM.getString(R.string.engine_pausedNotConnected), null, null);
    		}
			else
			{
    			if (!ec.chessEnginePaused)
    				continuePausedEngine(false);
    		}
 		}
		if (activDialog == PGN_ERROR_DIALOG)
		{
    		if (btnValue == 1)
    		{
    			Intent send = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", APP_EMAIL.toString(), null)); 
	        	send.putExtra(android.content.Intent.EXTRA_SUBJECT, gc.errorMessage); 
	        	send.putExtra(android.content.Intent.EXTRA_TEXT, gc.errorPGN); 
	        	c4aM.startActivity(Intent.createChooser(send, c4aM.getString(R.string.sendEmail)));
    		}
 		}
		if (activDialog == HELP_DIALOG)
		{
    		if (btnValue == 2)
    		{
				ec.chessEnginePaused = ec.lastChessEnginePaused;
				if (!ec.chessEnginePaused)
					continuePausedEngine(false);
				updateCurrentPosition("");
    		}
    		if (btnValue == 3)
    			startUserManual();
 		}
		if (activDialog == TIME_SETTINGS_DIALOG)
		{	// chessClockControl
			if (btnValue == 2)
    		{
				SharedPreferences.Editor ed = userP.edit();
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
				if (!ec.chessEnginePaused)
					continuePausedEngine(false);
				else
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
	    			c4aM.startActivityForResult(i, ENGINE_SEARCH_REQUEST_CODE);
    			}
    			catch (ActivityNotFoundException e) {e.printStackTrace(); c4aShowDialog(ENGINE_SEARCH_NO_INTERNET_DIALOG);}
    		}
 		}
	}
	public void c4aShowDialog(int dialogId)					
    {	// show dialog (remove and show)
		c4aM.removeDialog(dialogId);
		c4aM.showDialog(dialogId);
    }
	public void showHelpDialog(int gameStat, CharSequence title, CharSequence text)					
    {	// show help dialog 
		helpGameStat = gameStat;
	    helpTitle = title;
	    helpText = text;
		c4aShowDialog(HELP_DIALOG);
    }
//	SUBACTIVITYS		SUBACTIVITYS		SUBACTIVITYS		SUBACTIVITYS		
	public void startPgnDownload() 
	{
		String url = "http://c4akarl.blogspot.co.at/p/pgn-download.html";	// PGN download from "Karl's Blog" (MediaFire file links)
		Intent i = new Intent(Intent.ACTION_VIEW);
		i.setData(Uri.parse(url));
		c4aM.startActivity(i);
	}
	public void startMoveText()																	
    {	// startActivity: moveText
		moveTextIntent.putExtra("move_text", gc.cl.p_moveText);
		c4aM.startActivityForResult(moveTextIntent, MOVETEXT_REQUEST_CODE);
    }
    public void startFileManager(int fileActionCode, int displayActivity, int gameLoad)			
    {	// startActivity: fileManager(PGN)
//    	Log.i(TAG, "fileActionCode, displayActivity, gameLoad: " 	+ fileActionCode + ", " + displayActivity 
//    																+ ", " + gameLoad);
    	if ((fileActionCode == LOAD_GAME_REQUEST_CODE | fileActionCode == LOAD_GAME_PREVIOUS_CODE)
    			& displayActivity == 0 & !gc.isGameShow)
    	{
    		if 	(	userP.getBoolean("user_options_gui_usePgnDatabase", true) 
    				& fmPrefs.getInt("fm_extern_db_game_id", 0) == 0 
    				& fmPrefs.getInt("fm_location", 1) == 1
    			)
    		{
//    			Log.i(TAG, "startFileManager()"); 
    			startFileManager(LOAD_GAME_REQUEST_CODE, 1, 0);
	            return;
    		}
    		c4aM.removeDialog(FILE_LOAD_PROGRESS_DIALOG);
    		c4aM.showDialog(FILE_LOAD_PROGRESS_DIALOG);
    	}
    	fileManagerIntent.putExtra("fileActionCode", fileActionCode);
    	fileManagerIntent.putExtra("displayActivity", displayActivity);
    	fileManagerIntent.putExtra("gameLoad", gameLoad);
		fileManagerIntent.putExtra("findProblemId", "");
		fileManagerIntent.putExtra("queryControl", queryControl);
    	if (fileActionCode == 2 | fileActionCode == 7 | fileActionCode == 71 | fileActionCode == 72)
    		fileManagerIntent.putExtra("pgnData", gc.cl.history.createPgnFromHistory(1));
    	
    	if (displayActivity == 1)
    	{
    		if (gameLoad == 1)
    		{	// query for player games
    			c4aM.removeDialog(FILE_LOAD_PROGRESS_DIALOG);
        		c4aM.showDialog(FILE_LOAD_PROGRESS_DIALOG);
    		}
    		c4aM.startActivityForResult(fileManagerIntent, fileActionCode);		// start PgnFileManager - Activity(with GUI)
    	}
    	else						 
    	{
    		switch (fileActionCode) 										// Load | Save | Delete
			{
			case 1: 														// Load
				c4aM.startActivityForResult(fileManagerIntent, fileActionCode);	// start PgnFileManager - Activity(no GUI)									
				break;
			case 2: 														// Save
				startSaveFile(gc.cl.history.createPgnFromHistory(1));		// saveFile(using class: PgnIO)
				break;
			case 7: 														// Save(old game), Load(new game)
			case 71: 														// Save(old game, MateAnalysis OK), Load(new game)
			case 72: 														// Save(old game, MateAnalysis ERROR), Load(new game)
			default:
				c4aM.startActivityForResult(fileManagerIntent, fileActionCode);	// start PgnFileManager - Activity(no GUI)
				break;
			}
    	}
    }
    public void startSaveFile(CharSequence data)														
    {	// start saveFile(PgnIO) and startEngineAutoplay
//    	Log.i(TAG, "startSaveFile()");
    	CharSequence path = "";
    	CharSequence file = "";
		path = userP.getString("user_play_eve_path", "");
		file = userP.getString("user_play_eve_file", ""); 
		pgnIO = new PgnIO();
        String baseDir = pgnIO.getExternalDirectory(0);
        if (pgnIO.pathExists(baseDir + path))
		{
        	if (!file.equals(""))
        	{
				if (pgnIO.fileExists(baseDir.toString() + path, file.toString())) 
					pgnIO.dataToFile(baseDir.toString() + path, file.toString(), data.toString(), true);
				else
					pgnIO.dataToFile(baseDir.toString() + path, file.toString(), data.toString(), false);
        	}
		}
        startEngineAutoplay();
    }
    public void startGameData()																		
    {	// startActivity: pgnData
    	gameDataIntent.putExtra("gameStat", "1");
    	gameDataIntent.putExtra("gameTags", gc.cl.history.gameTags);
        c4aM.startActivityForResult(gameDataIntent, GAME_DATA_REQUEST_CODE);
    }
    public void startNotation(int textValue)													
    {	// startActivity: pgnNotation
    	notationIntent.putExtra("textValue", textValue);
    	notationIntent.putExtra("moves", gc.cl.history.createGameNotationFromHistory(600, false, true, true, false, false, true, 0));
    	notationIntent.putExtra("moves_text", gc.cl.history.createGameNotationFromHistory(600, true, true, true, false, false, true, 2));
    	notationIntent.putExtra("pgn", gc.cl.history.createPgnFromHistory(1));
    	notationIntent.putExtra("white", gc.cl.history.getGameTagValue("White"));
    	gc.cl.history.getGameTagValue("FEN");
    	notationIntent.putExtra("black", gc.cl.history.getGameTagValue("Black"));
    	c4aM.startActivityForResult(notationIntent, NOTATION_REQUEST_CODE);
    }
    public void startSaveGame(int displayActivity)												
    {	// startActivity: FileManager(saveGame)
       	startFileManager(SAVE_GAME_REQUEST_CODE, displayActivity, 0);
    }
    public void onActivityResult(int requestCode, int resultCode, Intent data)			
    {	// subActivity result
//    	Log.i(TAG, "onActivityResult, user_play_playMod: " + requestCode + ", " + userP.getInt("user_play_playMod", 1));
//    	Log.i(TAG, "ec.chessEnginePaused, ec.lastChessEnginePaused: " + ec.chessEnginePaused + ", " + ec.lastChessEnginePaused);
    	
    	mate = 0;
    	
    	updateCurrentPosition("");
    	SharedPreferences.Editor ed = userP.edit();
	    boolean continuePausedGame = true;
// ERROR	v1.15.1	Mar 12, 2012 11:36:52
	    boolean isNewGame = false;
	    if (data != null)
	    	isNewGame = data.getBooleanExtra("newGame", false);
	    else
	    {
	    	if (requestCode != ENGINE_SEARCH_REQUEST_CODE)
	    		return;
	    }
    	switch(requestCode) 
	    {
	    case OPTIONS_PLAY_REQUEST_CODE:
	    case ENGINE_SEARCH_REQUEST_CODE:
	    	if (resultCode == 3 | requestCode == ENGINE_SEARCH_REQUEST_CODE) 					
			{	// set playOption and play
	    		gc.isGameLoaded = false;
	    		setButtonPosition(2);
	    		ec.chessEngineAutoRun = false;
	    		ed.putBoolean("user_options_gui_FlipBoard", false);
	    		ed.commit();
	    		ec.chessEnginePlayMod = userP.getInt("user_play_playMod", 1);
	    		switch (userP.getInt("user_play_playMod", 1))
		        {
			        case 1:     // white
			        case 2:     // black
			        case 3:     // engine vs engine
			        	continuePausedGame = false;
			        	if (requestCode == OPTIONS_PLAY_REQUEST_CODE)
			        	{
//			        		ec.stopAllEngines();
				        	ec.chessEnginePaused = false;
			        		ec.chessEngineInit = false;
			        	}
			        	if (requestCode == ENGINE_SEARCH_REQUEST_CODE)
			        	{
			        		ec.chessEngineInit = true;
			        		engineJustInstalled = true;
			        	}
			        	
//			        	if (userP.getInt("user_play_playMod", 1) == 2)
//			        		mate = 3;
			        	
			        	startPlay(isNewGame);
			            break;
			        case 4:     // analysis
//			        	ec.stopAllEngines();
			        	ec.chessEnginePaused = false;
			    		ec.chessEngineInit = false;
			    		updateCurrentPosition("");
			    		
//			    		if (userP.getInt("user_play_playMod", 1) == 4)
//			        		mate = 3;
			    		
			    		startPlay(isNewGame);
			    		break;
			        case 5:     // two players (flip)
			        	analysisMessage = "";
			        	ed.putBoolean("user_options_gui_FlipBoard", true);
			    		ed.commit();
			    		startEdit(isNewGame);
			    		break;
			        case 6:     // edit (two players)
			        	analysisMessage = "";
			    		startEdit(isNewGame);
			    		break;
		        }
			}
	    	break;
	    case OPTIONS_ENGINE_AUTO_PLAY_REQUEST_CODE: 
	    	if (resultCode == C4aMain.RESULT_OK)
	    	{
	    		gc.isGameLoaded = false;
	    		stopTimeHandler(false);
	    		ec.chessEngineAutoRun = true;
	    		ec.chessEnginePaused = false;
	    		ec.lastChessEnginePaused = false;
	    		ec.chessEnginePlayMod = 3;
	    		cntResult = 0;
	    		initChessClock();
	    		continuePausedGame = false;
	    		gc.startPgn = gc.cl.history.createPgnFromHistory(1);
	    		gc.startMoveIdx = gc.cl.history.getMoveIdx();
//	    		Log.i(TAG, "gc.startMoveIdx: " + gc.startMoveIdx);
//	    		Log.i(TAG, "gc.startPgn: \n" + gc.startPgn);
	    		if 	(!userP.getBoolean("user_play_eve_autoCurrentGame", false))
	    			startPlay(true);
	    		else
	    		{
	    			c4aM.setRunMoveHistory();
	    			startC4aService316();	// current date
	    			startPlay(false);
	    		}
	    	}
			break;
	    case LOAD_GAME_REQUEST_CODE:
	    case LOAD_GAME_PREVIOUS_CODE:
	    case SAVE_LOAD_GAME_REQUEST_CODE:
	    case SAVE_OK_LOAD_GAME_REQUEST_CODE:
	    case SAVE_ERROR_LOAD_GAME_REQUEST_CODE:
//	    	ec.chessEngineInit = true;
	    	initChessClock();
	    	gc.isAutoLoad = false;
	    	gc.isPlayerPlayer = false;
	    	gc.pgnStat = "-";
			if (resultCode == C4aMain.RESULT_OK) 					
			{
				if (requestCode == LOAD_GAME_REQUEST_CODE | requestCode == LOAD_GAME_PREVIOUS_CODE)
				{
					gc.isGameLoaded = true;
					viewModeContent = data.getStringExtra("filePath") + data.getStringExtra("fileName") + "\n" + getGameInfo();
				}
//				if (requestCode == LOAD_GAME_REQUEST_CODE & !gc.isGameShow)
//					stopThreads(true);
				gc.isGameOver = false;
				gc.isGameUpdated = true;
				ec.chessEngineAutoRun = false;
				if (requestCode == LOAD_GAME_REQUEST_CODE | requestCode == LOAD_GAME_PREVIOUS_CODE)
				{
//					setButtonPosition(3);
					messageInfo 		= "";
				    messageEngine 		= "";
				}
				gc.pgnStat = data.getStringExtra("pgnStat");
				if (requestCode == SAVE_OK_LOAD_GAME_REQUEST_CODE 
						& userP.getBoolean("user_batch_ma_counterOn", true))
				{
					ed.putInt("user_batch_ma_gameCounter", userP.getInt("user_batch_ma_gameCounter", 1) +1);
					ed.commit();
				}
				getGameData(data.getStringExtra("fileBase"), data.getStringExtra("filePath"), data.getStringExtra("fileName"), 
							data.getStringExtra("fileData"), false, getIsEndPosition(), 0);
				gc.fileBase = gc.cl.history.getFileBase();
				gc.filePath = gc.cl.history.getFilePath();
				gc.fileName = gc.cl.history.getFileName();
				if (gc.cl.p_chess960ID == 518)
					gc.isChess960 = false;
	    		else
	    			gc.isChess960 = true;
				gc.startFen = gc.cl.history.getStartFen();
				if (gc.isGameShow)
				{
					gc.isAutoLoad = true;
					gc.isGameOver = false;
					gc.isAutoPlay = true;
					handlerGameShow.removeCallbacks(mUpdateGameShow);
					handlerGameShow.postDelayed(mUpdateGameShow, 100);
				}
				else
				{
					c4aM.setRunMoveHistory();
					c4aM.setRunPrefs();
				}
			}
			break;
	    case GAME_DATA_REQUEST_CODE: 
			if (resultCode == C4aMain.RESULT_OK) 					
		        gc.cl.history.setNewGameTags(data.getCharSequenceExtra("gameTags").toString());
			updateCurrentPosition("");
			break;
	    case NOTATION_REQUEST_CODE:
	    	updateCurrentPosition("");
			break;
	    case OPTIONS_CHESSBOARD_REQUEST_CODE:
	    case OPTIONS_GUI_REQUEST_CODE:
	    	if (requestCode == OPTIONS_CHESSBOARD_REQUEST_CODE)
	    		chessBoard.setImageSet(getImageSet());
	    	if (requestCode == OPTIONS_GUI_REQUEST_CODE)
	    		{
	    			c4aM.useWakeLock = userP.getBoolean("user_options_gui_disableScreenTimeout", false);
	    			c4aM.setWakeLock(c4aM.useWakeLock);
	    		}
	    	if (userP.getBoolean("user_options_gui_StatusBar", false))
	    		updateFullscreenStatus(false);
	    	else
	    		updateFullscreenStatus(true);
	    	updateCurrentPosition("");
			break;
	    case OPTIONS_TIME_CONTROL_REQUEST_CODE:
	    	if (resultCode == 101)	// apply
	    	{
//	    		ec.stopAllEngines();
	        	ec.chessEnginePaused = false;
	        	ec.lastChessEnginePaused = false;
	    		ec.chessEngineInit = false;
	    		initChessClock();
	    		continuePausedGame = false;
	    		startPlay(false);
	    	}
	    	if (resultCode == 1110 | resultCode == 2110)	// start/end TEST
	    		c4aM.finish();
	    	break;
	    case OPTIONS_ENGINE_PLAY_REQUEST_CODE:
	    	if (resultCode == C4aMain.RESULT_OK) 
	    	{
	    		ec.setBookOptions();
//	    		if (!ec.chessEnginePaused)
//	    			pauseEnginePlay(0);	// stop
//	    		ec.stopAllEngines();
	    		ec.stopComputerThinking(true);
	        	ec.setPlaySettings(userP);
				ec.chessEngineInit = true;
				ec.chessEnginePaused = true;
				initInfoArrays(true);
				messageEngine 		= "";
				updateCurrentPosition("");
	    	}
	    	break;
	    case EDIT_CHESSBOARD_REQUEST_CODE:
	    	if (resultCode == C4aMain.RESULT_OK) 					
			{
	    		setButtonPosition(2);
	    		messageEngine 		= "";
	    		gc.errorMessage = "";
	        	gc.errorPGN = "";
	    		ec.chessEngineAutoRun = false;
	        	CharSequence chess960Id = data.getStringExtra("chess960Id");
	        	CharSequence fen = data.getStringExtra("newFen");
	        	if (!chess960Id.equals("518"))
	        		fen = "";
//	        	Log.i(TAG, "chess960Id, fen: " + chess960Id + ", " + fen);
	        	gc.cl.newPosition(chess960Id, fen, "", "", "", "", "", "");
//	        	Log.i(TAG, "gc.gameStat, gc.cl.p_stat: " + gc.gameStat + ", " + gc.cl.p_stat);
	        	if (gc.cl.p_stat.equals("1"))
	    	  	{
//	        		Log.i(TAG, "EDIT_CHESSBOARD_REQUEST_CODE, OK");
	    	  		gc.isGameOver = false;
	    			gc.isGameUpdated = true;
	    			gc.fen = gc.cl.p_fen;
	    			if (gc.cl.p_chess960ID == 518)
	    				gc.isChess960 = false;
	    			else
	    				gc.isChess960 = true;
    				ed.putInt("user_game_chess960Id", gc.cl.p_chess960ID);
    		        ed.commit();
    		        setInfoMessage(false, false, "", "", "", "");
	    	  		updateGui();
	    	  	}
			}
			break;
	    case MOVETEXT_REQUEST_CODE:
	    	if (resultCode == C4aMain.RESULT_OK) 					
			{
	    		gc.isGameUpdated = false;
		        gc.cl.history.setMoveText(data.getStringExtra("text"));
		        updateCurrentPosition("");
			}
			break;
	    case SAVE_GAME_REQUEST_CODE:
	    	if (resultCode == C4aMain.RESULT_OK) 			
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
	    	if (resultCode == C4aMain.RESULT_OK) 					
			{	
	            setChess960IdPref(chess960.createChessPosition(data.getStringExtra("chess960BaseLine")));
			}
	    	break;
	    case ENGINE_SETTING_REQUEST_CODE:
	    	ec.chessEnginePaused = true;
	    	updateCurrentPosition("");
			setInfoMessage(false, true, null, getEnginePausedMessage(), null, null);
	    	break;
	    case PDB_TO_PGN_CODE:
	    	if (resultCode == C4aMain.RESULT_OK)
	    	{
	    		int playMod = 3;
	    		ec.chessEnginePlayMod = playMod;
        		setPlayModPrefs(playMod);
	    		userP = c4aM.getSharedPreferences("user", 0);
	
	    		engineMes = ""; 
	    		initInfoArrays(false);
        		ec.chessEnginePaused = false;
        		ec.chessEngineInit = true;
	    		initPdbPgn();
	    		setInfoMessage(true, true, "", "Start PDB to PGN . . .", null, null);
	    		handlerPdbPgn.removeCallbacks(mUpdatePdbPgn);
	    		handlerPdbPgn.postDelayed(mUpdatePdbPgn, 0);
	    	}
	    	break;
	    }
	    if (progressDialog != null)
    	{
	    	if (progressDialog.isShowing())
	    		c4aM.dismissDialog(FILE_LOAD_PROGRESS_DIALOG);
    	}
	    if (continuePausedGame)
	    {
		    if (!gc.isGameLoaded & resultCode == 0 & !gc.isGameOver & !gc.cl.p_variationEnd & !ec.chessEnginePaused)
		    	continuePausedEngine(false);
	    }
    } 
    //	HANDLER, TIMER		HANDLER, TIMER		HANDLER, TIMER		HANDLER, TIMER
    public Runnable mUpdateAutoplay = new Runnable() 		
	{	// AutoPlay: Handler(Timer)
		   public void run() 
		   {
			   if (gc.isAutoPlay)
			   {
				   if (gc.isGameOver)
					   stopAutoPlay();
				   else
				   {
				       nextMove(2, 0);
				       handlerAutoPlay.postDelayed(mUpdateAutoplay, userP.getInt("user_options_timer_autoPlay", gc.autoPlayValue));
				   }
			   }
			   else
				   stopAutoPlay();
		   }
	};
	public Runnable mUpdateGameShow = new Runnable() 		
	{	// GameShow: Handler(Timer)
		   public void run() 
		   {
//			   Log.i(TAG, "isGameShow, gc.isAutoPlay, isGameOver, pgnStat: " + gc.isGameShow + ", " + gc.isAutoPlay + ", " + gc.isGameOver + ", " + gc.pgnStat);
			   if (gc.isGameShow)
			   {
				   if (runP.getBoolean("run_isActivate", false))
				   {
					   if (gc.pgnStat.equals("L"))
					   {
						   analysisEndTime =  System.currentTimeMillis();
						   stopGameShow();
					   }
			    	   else
			    	   {
			    		   try {Thread.sleep(10);} 
			    		   catch (InterruptedException e) {}
			    		   startFileManager(LOAD_GAME_REQUEST_CODE, 0, 0);
			    	   }
				   }
				   else
				   {
				       if (gc.isGameOver)
				       {
				    	   if (gc.pgnStat.equals("L"))
				    		   startFileManager(LOAD_GAME_REQUEST_CODE, 0, 1);
				    	   else
				    		   startFileManager(LOAD_GAME_REQUEST_CODE, 0, 0);
				    	   gc.isGameOver = false;
		    			   gc.isAutoPlay = false;
				       }
				       else
				       {
				    	   if (gc.isAutoPlay)
				    	   {
				    		   nextMove(2, 0);		// next move
			    			   handlerGameShow.postDelayed(mUpdateGameShow, userP.getInt("user_options_timer_autoPlay", gc.gameShowValue));
				    	   }
				       }
				   }
			   }
		   }
	};
	public Runnable mUpdateChessClock = new Runnable() 		
	{	// ChessClock: timer white/black
		   public void run() 
		   {
			   if (tc.clockIsRunning)
			   {
				   if (gc.isGameOver | gc.cl.p_variationEnd)
				   {
					   tc.stopChessClock(System.currentTimeMillis());
					   ec.chessEnginePaused = true;
					   ec.chessEngineSearching = false;
					   updateCurrentPosition("");
					   setInfoMessage(false, true, null, getEnginePausedMessage(), null, null);
				   }
				   else
					   updateTime(gc.cl.p_color);
			   }
		   }
	};
//  ENGINE		ENGINE		ENGINE		ENGINE		ENGINE		ENGINE
	public class ChessEngineBindTask extends AsyncTask<CharSequence, Void, Boolean> 	
	{	//>230 bind engine to the main activity(C4A)
		@Override
		protected Boolean doInBackground(CharSequence... args) 
		{
			ec.onBinding = true;
			eNumber = args[0];
			bindEngine2 = args[1];
			if (!ec.twoEngines)
				bindEngine2 = "";
			if (eNumber.equals("2"))
				ec.setEngineNumber(2);
			else
				ec.setEngineNumber(1);
			if (!ec.getEngine().isBound | !ec.getEngine().engineServiceIsReady)
			{
//				Log.i(TAG, "engine 1: " + userP.getString("user_play_engine_1", ""));
//				Log.i(TAG, "engine 2: " + userP.getString("user_play_engine_2", ""));
//				Log.i(TAG, "start binding: " + ec.getEngine().engineServiceName );
				if (!ec.getEngine().engineServiceName.equals(""))
				{
					boolean isBounded =  ec.bindToEngineService();	//>231 calls ChessEngine.onServiceConnected()
					if (!isBounded)
					{
						try {Thread.sleep(200);} 
						catch (InterruptedException e) {}
					}
					return isBounded;
				}
				else
					return false;
			}
			else
				return true;
		}
		protected void onPostExecute(Boolean isBound) 
		{
			boolean engine1StartPlay = false;
			ec.onBinding = false;
			ec.chessEnginesNotFound = false;
//			Log.i(TAG, "engineNumber, isBound: " + ec.getEngine().engineNumber + ", " + isBound);
			if (!ec.getEngine().engineServiceIsReady)
			{	// isBond but another process is running: unbind service!
//				Log.i(TAG, "engineNumber, engineServiceIsReady, isBound: " + ec.getEngine().engineNumber 
//						+ ", " + ec.getEngine().engineServiceIsReady + ", " + ec.getEngine().isBound);
				if (ec.chessEngineBindCounter < 4 & ec.getEngine().isBound)
				{	// try again, max 3
					ec.chessEngineBindCounter++;
					setInfoMessage(false, true, "", c4aM.getString(R.string.engineProgressDialog) + " (" + ec.chessEngineBindCounter + ")", "", null);
					try {Thread.sleep(1000);} 
					catch (InterruptedException e) {}
					chessEngineBindTask = new ChessEngineBindTask();
    	    		chessEngineBindTask.execute(eNumber, "2");	
					return;
				}
				else
				{
					ec.releaseEngineService();
					ec.chessEnginesNotFound = true;
					isBound = false;
				}
			}
			if (!isBound)
			{
//				Log.i(TAG, "!isBound");
				ec.chessEngineProblem = false;
				stopChessClock();
				CharSequence mes = c4aM.getString(R.string.engineNoEnginesOnDevice);
				if (!ec.getEngine().getEngineServiceIsReady())	
				{
					ec.chessEngineProblem = true;
					if (ec.getEngine().getCurrentProcess().equals(""))
					{
//						Log.i(TAG, "Bound error: ");
						if (ec.engineList.size() == 0)
						{
//							ec.stopAllEngines();
							ec.chessEnginesNotFound = false;
							c4aShowDialog(ENGINE_SEARCH_DIALOG);
							return;
						}
						else
						{
							if (engineJustInstalled)
							{
								engineJustInstalled = false;
								mes = c4aM.getString(R.string.engineJustInstalled);
							}
							else
							{
								ec.chessEngineInit = true;
								mes = c4aM.getString(R.string.engineNotFound);
							}
						}
					}
					else
					{
//						Log.i(TAG, "Bound error, engineAlreadyRunning: " + ec.getEngine().getCurrentProcess());
						mes = c4aM.getString(R.string.engineAlreadyRunning);
					}
				}
				ec.chessEnginePaused = true;
				updateGui();
				setInfoMessage(false, true, null, mes, null, null);
			}
			else
			{
//				Log.i(TAG, "isBound");
				if 	(ec.getEngine().engineNumber == 1 & bindEngine2.equals("2"))
				{	//> 232 start ChessEngineBindTask for Engine 2
//					Log.i(TAG, "start second Engine!");
					engine1StartPlay = ec.getEngine().startPlay;
					ec.chessEngineBindCounter = 2;
					chessEngineBindTask = new ChessEngineBindTask();
    	    		chessEngineBindTask.execute("2", "2");	// second Engine!
    	    		return;
				}
				else
				{
					if (bindEngine2.equals(""))
						startEnginePlayIsReady();			//>233 start engine play (engine 1) after bind
					else
					{
						if (!engine1StartPlay)
						{
							ec.setEngineNumber(1);
							ec.newGame(c4aM.gc.isChess960, c4aM.gc.startFen);
							try {Thread.sleep(sleepTime);} 
							catch (InterruptedException e) {}
							if ( ec.getEngine().isReady)
							{
								ec.setEngineNumber(2);
								startEnginePlayIsReady();	//>234 start engine play (engine 2) after bind
							}
							else
							{
//								Log.i(TAG, "Bound engine 1, engineNotFound: ");
								c4aM.uic.c4aShowDialog(UiControl.NO_CHESS_ENGINE_DIALOG);
							}
						}
						else
						{
							ec.setEngineNumber(2);			
							ec.newGame(c4aM.gc.isChess960, c4aM.gc.startFen);
							try {Thread.sleep(sleepTime);} 
							catch (InterruptedException e) {}
							if ( ec.getEngine().isReady)
							{
								ec.setEngineNumber(1);
								startEnginePlayIsReady();	//>233 start engine play (engine 1) after bind
							}
							else
							{
//								Log.i(TAG, "Bound engine 2, engineNotFound: ");
								c4aM.uic.c4aShowDialog(UiControl.NO_CHESS_ENGINE_DIALOG);
							}
						}
					}
				}
			}
		}
		CharSequence eNumber = "";
		CharSequence bindEngine2 = "";
	}
	public class ChessEngineSearchTask extends AsyncTask<CharSequence, CharSequence, CharSequence> 	// engine / player - task
	{	//>240 the GUI/Engine search thread
		@Override
		protected CharSequence doInBackground(CharSequence... args) 
		{
			taskFen = args[0];
			taskMoves = args[1];
//			Log.i(TAG, "searchTask, doInBackground(), taskFen, taskMoves: " + taskFen + ", " +taskMoves);
			handlerChessClock.removeCallbacks(mUpdateChessClock);
			CharSequence firstMove = setRandomFirstMove(taskFen);
			if (!firstMove.equals(""))
				return firstMove;
			ec.chessEnginesOpeningBook = false;
			if (userP.getBoolean("user_options_enginePlay_OpeningBook", true) & ec.chessEnginePlayMod != 4)
	       	{	// using openingBook
				Move bookMove = null;
				try {bookMove = ec.book.getBookMove(TextIO.readFEN(taskFen.toString()));} 
				catch (ChessParseError e1) {e1.printStackTrace();}
	            if (bookMove != null)
	            {
	            	ec.chessEnginesOpeningBook = true;
	            	if (tc.clockIsRunning)
	            		stopChessClock();
					return bookMove.toString();
	            }
	       	}
			if (!ec.chessEnginesOpeningBook & !tc.clockIsRunning)
			{
				initChessClock();
				startChessClock();
				publishProgress("1", "", "");
			}
			setSearchTime();
			//>241 
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
				ec.getEngine().startSearch(taskFen, taskMoves, wTime, bTime, wInc, bInc, moveTime, movesToGo, ec.chessEngineAnalysis, isDrawOffer, mate);
			}
			else
			{
//				Log.i(TAG, "searchTask, !ec.getEngine().searchIsReady()");
				return "";
			}
			while(true) 
			{
//				Log.i(TAG, "doInBackground() while(true)");
				if (ec.chessEngineStopSearch & !cancelTask)
				{
//					Log.i(TAG, "doInBackground(), isCancelled()");
					if (ec.chessEnginePlayMod != 4)
						ec.chessEngineStopSearch = false;
					cancelTask = true;
				}
				currentBestMove = ec.getEngine().statPvBestMove;	// current best move
				CharSequence s = ec.getEngine().readLineFromProcess(engineSearchTimeout);
				if (s == null | s.length() == 0)
				{
//					Log.i(TAG, "line: " + s);
					s = "";
				}
				CharSequence[] tokens = ec.getEngine().tokenize(s);
				if (tokens[0].equals("info"))
				{
					ec.getEngine().parseInfoCmd(tokens, c4aM.userP.getInt("user_options_enginePlay_PvMoves", 30));
					engineStat = getInfoStat(ec.getEngine().statCurrDepth, ec.getEngine().statCurrMoveNr, ec.getEngine().statCurrMove, 
							ec.getEngine().statTime, ec.getEngine().statNodes, ec.getEngine().statNps);
					if(ec.getEngine().statPvAction.equals("1"))
					{
//						if ((infoContent == 2 & infoShowPv) | mate > 0)
						if (infoShowPv | mate > 0)
						{
							// ERROR	v1.38  	02.07.2013
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
//					Log.i(TAG, "info, engineMes: " + engineMes);
				}
				
				publishProgress(ec.getEngine().statPvAction, engineMes, engineStat);
				
				if (tokens[0].equals("bestmove") & ec.chessEnginePlayMod != 4)
				{	// get best move if not analysis
					if (mate == 0)
						handlerChessClock.removeCallbacks(mUpdateChessClock);
					else
					{
						engineMessage = engineMes.toString();
					}
					return tokens[1];	//>242	return bestmove
				}
				if (!s.equals(""))
				{
					searchStartTimeInfo = System.currentTimeMillis();
					isTimeOut = false;
				}
				
//				publishProgress(ec.getEngine().statPvAction, engineMes, engineStat);
				
				if (ec.chessEnginePlayMod == 4 & ec.chessEngineAnalysisStat < 9)
				{	// analysis has stopped
//					Log.i(TAG, "analysis has stopped, stat, bestMove: " + ec.chessEngineAnalysisStat + ", " + currentBestMove);
					if (!currentBestMove.equals(""))
					{
						ec.getEngine().writeLineToProcess("stop");
						isNoRespond = false;
						cancelTask = false;
						return currentBestMove;
					}
				}
				
				if (cancelTask & !s.equals(""))
				{
//					Log.i(TAG, "cancelTask, s: " + s);
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
//					Log.i(TAG, "searchTask, isNoRespond");
					if (mate == 0)
						return "";
					else
					{
						engineMessage = "$$NO_RESPOND";
						return "$$NO_RESPOND";
					}	
				}
				if (isTimeOut)
				{
					ec.getEngine().writeLineToProcess("stop");
					isTimeOut = false;
					return "";
				}
//				if (ec.chessEnginePlayMod == 4 & ec.chessEngineAnalysisStat < 9)
//				{	// analysis has stopped
////					Log.i(TAG, "analysis has stopped, stat, bestMove: " + ec.chessEngineAnalysisStat + ", " + currentBestMove);
//					if (!currentBestMove.equals(""))
//					{
//						ec.getEngine().writeLineToProcess("stop");
//						isNoRespond = false;
//						return currentBestMove;
//					}
//				}
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
			if (mate > 0)
				searchStartTime = System.currentTimeMillis();
			if (infoPv == null | ec.chessEngineInit)
				initInfoArrays(true);
			else
				initInfoArrays(false);
			isSearchTaskStopped = false;
			if (!ec.chessEngineAutoRun)
			{
				if (userP.getBoolean("user_options_enginePlay_OpeningBook", true) & ec.chessEnginesOpeningBook)
					setInfoMessage(false, true, "", c4aM.getString(R.string.engine_openingBook), "", null);
				else
					setInfoMessage(false, true, "", getEngineThinkingMessage(), "", null);
			}
		}
		protected void onProgressUpdate(CharSequence... args)
	    {	
			long currentTime = System.currentTimeMillis();
			if (cancelTask & ((int) (currentTime - searchStartTimeInfo) > MAX_SEARCH_CANCEL_TIMEOUT))
		    	isNoRespond = true;
		    if ((int) (currentTime - searchStartTimeInfo) > MAX_SEARCH_TIMEOUT)
		    	isNoRespond = true;
//		    Log.i(TAG, "currentTime, searchStartTimeInfo, isNoRespond: " + currentTime + ", " + searchStartTimeInfo + ", " + isNoRespond);
			if (mate > 0)
			{
				engineMessage = (String) args[1];
				updateTime("w");
				if ((int) (currentTime - searchStartTime) > MAX_SEARCH_TIME)
				{
					isTimeOut = true;
					engineMessage = "$$TIMEOUT";
				}
				return;
			}
			CharSequence pvAction = args[0];
			CharSequence engineMes = args[1];
			CharSequence engineStat = args[2];
//			if (!args[2].toString().equals(""))
//				engineMes = args[2] + "\n" + engineMes;
//			Log.i(TAG, "ec.chessEnginePaused, pvAction, engineMes: " + ec.chessEnginePaused + ", " + pvAction);
//			Log.i(TAG, engineMes.toString());
//			Log.i(TAG, "onProgressUpdate(), gc.isGameOver, gc.cl.p_variationEnd: " + gc.isGameOver + ", " + gc.cl.p_variationEnd);
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
					setInfoMessage(false, false, null, getEnginePausedMessage(), engineMes, null);
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
						setInfoMessage(false, false, "", c4aM.getString(R.string.engine_openingBook), "", null);
					else
						setInfoMessage(false, false, "", c4aM.getString(R.string.engine_autoPlay) + showGameCount 
								+ "\n" + c4aM.getString(R.string.engine_openingBook), "", null);
				}
				else
				{
					if(pvAction.equals("0")) 
						engineMes = "";
					if (!ec.chessEngineAutoRun)
					{
						if (getEngineThinkingMessage().equals(c4aM.getString(R.string.engineAnalysisStopWait)))
							engineStat = "";
						setInfoMessage(false, false, "", getEngineThinkingMessage() + " " + engineStat, engineMes, null);
					}
					else
						setInfoMessage(false, false, null, null, engineMes, null);
				}
			}
	    }
		protected void onPostExecute(CharSequence result) 
		{
//			Log.i(TAG, "engineServiceName, ec.ENGINE_ALIVE: " + ec.getEngine().engineServiceName + ", " + ec.getEngine().getInfoFromEngineService("ENGINE_ALIVE"));
//			Log.i(TAG, "onPostExecute(), result, isSearchTaskStopped: " + result + ", " + isSearchTaskStopped);
			ec.chessEngineAnalysis = false;
//			if (isNoRespond & !cancelTask)
			if (isNoRespond)
			{
//				Log.i(TAG, "isNoRespond");
				stopChessClock();
				ec.chessEngineSearching = false;
				ec.stopComputerThinking(true);
				ec.releaseEngineService();
				ec.chessEnginePaused = true;
				ec.chessEngineInit = true;
				updateCurrentPosition("");
				if (!cancelTask)
					setInfoMessage(false, false, "", c4aM.getString(R.string.engine_timeout), null, null);
				return;
			}
			if (result.equals("CANCEL") | cancelTask)
			{
//				Log.i(TAG, "isCANCEL");
				ec.chessEngineSearching = false;
				ec.chessEngineStopSearch = false;
				ec.stopComputerThinking(false);
				if (ec.chessEnginePlayMod == 4)
				{
					ec.chessEnginePaused = true;
					updateGui();
					stopChessClock();
					setInfoMessage(false, false, null, getEnginePausedMessage(), null, null);
					Toast.makeText(c4aM, c4aM.getString(R.string.engineAnalysisStop), Toast.LENGTH_SHORT).show();
				}
				return;
			}
			if (!isSearchTaskStopped)
			{
				if (!ec.getEngine().getSearchAlive())	
				{
//					Log.i(TAG, "!ec.getEngine().getSearchAlive()");
					if (mate > 0)
					{
						engineMessage = "$$NO_RESPOND";
						result = "$$NO_RESPOND";
						updateGui();
						setInfoMessage(false, false, "", c4aM.getString(R.string.engine_timeout), null, null);
						enginePlay(result, taskFen);
						return;
					}
					ec.getEngine().searchAlive = true;
					if (ec.chessEngineInit)
					{
						ec.chessEnginePaused = true;
						setInfoMessage(false, false, "", c4aM.getString(R.string.engine_pausedNotConnected), null, null);
					}
					else
					{
						ec.stopAllEngines();
						ec.chessEngineInit = true;
						ec.chessEnginePaused = true;
						updateGui();
						stopChessClock();
						setInfoMessage(false, false, "", c4aM.getString(R.string.engine_noRespond), null, null);
					}
				}
				else
				{
					initInfoArrays(false);
					enginePlay(result, taskFen);		//>243 analyse best move and set next action
				}
			}
	    }
		protected void setSearchTime() 
		{
			if (mate != 0)
			{
				wTime = 0;
	        	bTime = 0;
	        	wInc = 0;
	        	bInc = 0;
				moveTime = 0;
				return;
			}
			switch (userP.getInt("user_options_timeControl", 1))
	        {
		        case 1:     // game clock
		        case 3:     // sand glass
		        	wTime = tc.timeWhite;
		        	bTime = tc.timeBlack;
		        	wInc = tc.bonusWhite;
		        	bInc = tc.bonusBlack;
		        	movesToGo = 00;
		        	moveTime = 0;
//		        	Log.i(TAG, "taskFen: " + taskFen);
//		        	Log.i(TAG, "wt, bt, wi, bi: " + wTime + ", " + bTime + ", " + wInc + ", " + bInc);
		        	break;
		        case 2:     // move time
		        	wTime = 0;
		        	bTime = 0;
		        	wInc = 0;
		        	bInc = 0;
		        	movesToGo = 1;
		        	setMoveTime();
		        	moveTime = userP.getInt("user_time_engine_move", 1000);
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
			if (taskFen.equals(gc.standardFen) & userP.getBoolean("user_options_enginePlay_RandomFirstMove", false))
				taskMoves = ec.getEngine().getRandomFirstMove();
			return taskMoves;
		}
		protected CharSequence getInfoPv(int statPvIdx, CharSequence statPvMoves, int statPvScore, boolean isMate, CharSequence fen) 
		{
			sbInfo.setLength(0);
			if 	(	infoPv.size() 	== 	c4aM.userP.getInt("user_options_enginePlay_MultiPv", 4) 
					& statPvIdx 	< 	c4aM.userP.getInt("user_options_enginePlay_MultiPv", 4)
				)
	    	{
	    		infoPv.set(statPvIdx, statPvMoves);
	    		if (statPvIdx == 0)
	    			bestScore = getBestScore(statPvScore, fen);
				CharSequence displayScore = getDisplayScore(statPvScore, fen);
				if (isMate & statPvScore > 0)
					displayScore = "M" + statPvScore;
				//sbMoves.setLength(0); sbMoves.append("\u0095"); sbMoves.append((statPvIdx +1)); sbMoves.append("(");
				sbMoves.setLength(0); sbMoves.append("*"); sbMoves.append((statPvIdx +1)); sbMoves.append("(");
				CharSequence notation = gc.cl.getNotationFromInfoPv(taskFen, statPvMoves);
				if (notation.equals(""))
					return "";
				sbMoves.append(displayScore); sbMoves.append(") "); sbMoves.append(notation);
//				Log.i(TAG, "taskFen: " + taskFen);
//				Log.i(TAG, "statPvMoves: " + statPvMoves);
//				Log.i(TAG, "sbMoves: "  + sbMoves);
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
//			CharSequence curMove = "";
//			CharSequence curTime = "";
//			if (statCurrMoveNr > 0)
//			{
//				CharSequence notation = gc.cl.getNotationFromInfoPv(taskFen, statCurrMove);
//				Log.i(TAG, "notation: " + notation);
//				if (notation.toString().contains("."))
//				{
//					CharSequence[] split = notation.toString().split("\\.");
//					if (split.length > 0)
//						notation = split[1];
//					notation = notation.toString().replace(" ", "");
//				}
//				if (!notation.equals(""))
//					curMove = " " + statCurrMoveNr + ":" + notation + " ";
//			}
//			if (statTime > 1000)
//				curTime = " t:" + (statTime / 1000) + "s ";
//			infoStat = "<" + statPVDepth + ">" + curMove + curTime + " n:" + (statNodes / 1000) + "k, nps:" + (statNps / 1000) + "k";
//			infoStat = "<" + statPVDepth + ">" + curMove;
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
//    		Log.i(TAG, "getBestScore: " + score);
    		return score;
		}
		
		GameControl gcPv = new GameControl(c4aM.stringValues, "");
		int wTime = 100;
    	int bTime = 100;
    	int wInc = 1000;
    	int bInc = 1000;
    	int engineSearchTime = 0;
    	int engineSearchTimeout = 1000;
    	int engineSearchTimeoutMin = 1000;
    	int engineSearchTimeoutMax = 3000;
    	int moveTime = 1000;
    	int timeLeft = 0;
//    	int mate = 0;
    	long sleepSearch = 20;
    	int movesToGo = 0;
    	CharSequence currentBestMove = "";
    	boolean isDrawOffer = false;
    	boolean isNoRespond = false;
    	CharSequence taskFen = "";
    	CharSequence taskMoves = "";
    	StringBuilder sbMoves = new StringBuilder(100);
    	StringBuilder sbInfo = new StringBuilder(100);
    	
    	long searchStartTimeInfo = 0;		// info != ""
//        int MAX_SEARCH_TIME_INFO_ALIVE = 10000;		// max. search time engine alive
//        int MAX_SEARCH_TIME_INFO_NOT_ALIVE = 5000;	// max. search time engine not alive
//        int MAX_SEARCH_TIMEOUT = 10000;	// max. search time engine timeout
//        int MAX_SEARCH_TIMEOUT = 60000;	// max. search time engine timeout (1 min)
        int MAX_SEARCH_TIMEOUT = 180000;	// max. search time engine timeout (3 min: no info message)
        int MAX_SEARCH_CANCEL_TIMEOUT = 1500;	// max. search time engine timeout
//        boolean engineAliveBreak = false;
//        boolean engineNotAliveBreak = false;
        boolean cancelTask = false;
	}
	public void enginePlay(CharSequence result, CharSequence taskFen) 
	{
//		Log.i(TAG, "enginePlay() start, result, taskFen: " + result + "\n" + taskFen);
//		Log.i(TAG, "enginePlay() mate, taskFen: " + mate + "\n" + taskFen);
		if (mate > 0)
		{
//			engineMessage = (String) messageEngine;
//			searchTaskRestart = false;
			cancelSearchTask();
//			ec.getEngine().stopSearch();
			isSearchResult = true;
//			Log.i(TAG, "enginePlay() pdb to pgn, engineMessage: " + engineMessage);
			handlerPdbPgn.removeCallbacks(mUpdatePdbPgn);
    		handlerPdbPgn.postDelayed(mUpdatePdbPgn, 0);
    		return;
		}
		if (!result.equals(""))
		{
			searchTaskRestart = false;
			if (!ec.chessEnginePaused & !result.equals("(none)"))
			{
				cancelSearchTask();
				CharSequence newFen = "";
				if (ec.chessEnginePlayMod == 4)
				{	// analysis 
					if (ec.chessEngineAnalysisStat == 1 | ec.chessEngineAnalysisStat == 2)
						newFen = chessEngineGui(taskFen, result);
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
						setInfoMessage(false, false, null, getEnginePausedMessage(), null, null);
					}
					else
					{
						if (ec.chessEngineAnalysisStat == 2)
						{
							ec.getEngine().newGame();
							startChessClock();
							chessEngineBestMove(newFen, "");
						}
						else
							updateCurrentPosition("");
					}
					ec.chessEngineStopSearch = false;
				}
				else
				{
					newFen = chessEngineGui(taskFen, result);
//					Log.i(TAG, "enginePlay() newFen: " + newFen);
					if (!newFen.equals(""))
					{
	    				if (ec.chessEnginePlayMod == 3)
	    				{	// engine vs engine
	    					// autostp enginePlay if >  < , canceled (error: statPvBestScore)
	    					if (ec.chessEngineAutoRun)
	    					{
//	    						Log.i(TAG, "gameOver, variationEnd, result: " + gc.isGameOver + ", " + gc.cl.p_variationEnd + ", " + gc.cl.history.getGameResult());
	    						if 		(	gc.isGameOver | result.equals("(none)") 
											| gc.cl.p_variationEnd | gc.cl.p_mate | gc.cl.p_stalemate
	    								)
	    						{
									messageEngine 		= "";
									if (userP.getBoolean("user_play_eve_autoSave", true))
										startSaveGame(0);	// >>> onActivityResult(), SAVE_GAME_REQUEST_CODE
									else
										nextGameEngineAutoPlay();
								}
								else
								{
		    						ec.chessEngineSearching = true;
			    					chessEngineBestMove(newFen, "");
		    					}
	    					}
	    					else
	    					{
//	    						Log.i(TAG, "gc.isGameOver, gc.cl.p_variationEnd, gc.cl.p_mate, gc.cl.p_stalemate: " + gc.isGameOver + ", " + gc.cl.p_variationEnd + ", " + gc.cl.p_mate + ", " + gc.cl.p_stalemate);
	    						if 	(	!gc.isGameOver & !gc.cl.p_variationEnd & !result.equals("(none)") 
		    							& !gc.cl.p_mate & !gc.cl.p_stalemate
		    						)
	    						{
		    						ec.chessEngineSearching = true;
			    					chessEngineBestMove(newFen, "");
		    					}
		    					else
		    					{
		    						ec.stopComputerThinking(false);
		    						ec.chessEnginePaused = true;
		    						updateCurrentPosition("");
	    							setInfoMessage(false, false, null, getEnginePausedMessage(), null, null);
		    					}
	    					}
	    				}
	    				else
	    				{
	    					ec.chessEngineSearching = false;
	    					if (userP.getInt("user_options_timeControl", 1) == 2)
	    						setMoveTime();
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
//			Log.i(TAG, "engine_noRespond 2");
			if (!searchTaskRestart)
			{
				searchTaskRestart = true;
				ec.chessEngineSearching = true;
				chessEngineBestMove(searchTaskFen, searchTaskMoves);
			}
			else
			{
//				ec.stopAllEngines();
				ec.chessEngineInit = true;
				ec.chessEnginePaused = true;
				updateGui();
				setInfoMessage(false, false, null, c4aM.getString(R.string.engine_noRespond), null, null);
			}
		}
	}
	public void cancelBindTask()															
    {
// ERROR(ANR)	v1.7	06.11.2011 17:53:52
		if (chessEngineBindTask != null)
			chessEngineBindTask.cancel(true);
    }
	public void cancelSearchTask()															
    {
		if (chessEngineSearchTask != null)
			chessEngineSearchTask.cancel(true);
    }
	public void startEngineAutoplay()															
    {
		SharedPreferences.Editor ed = userP.edit();
		int gameCounter = userP.getInt("user_play_eve_gameCounter", 1);
		gameCounter++;
        ed.putInt("user_play_eve_gameCounter", gameCounter);
        ed.commit();
        ec.chessEngineRound = userP.getInt("user_play_eve_round", 1) 
        					+ "." + userP.getInt("user_play_eve_gameCounter", 1);
        cntChess960 = 0;
        nextGameEngineAutoPlay();
    }
	public void chessEngineBestMove(CharSequence fen, CharSequence moves) 
    {
    	if (!ec.onBinding & !ec.chessEnginePaused & !fen.equals(""))
		{
    		if (ec.twoEngines)
    		{
    			if (gc.getValueFromFen(fen, 2).equals("b"))
    				ec.engineNumber = 2;
    			else
    				ec.engineNumber = 1;
    		}
    		cancelBindTask();
    		cancelSearchTask();
//    		Log.i(TAG, "chessEngineBestMove, fen, w/b, engine, mate: " + fen + ", " + gc.getValueFromFen(fen, 2)  + ", " + ec.engineNumber + ", " + mate);
    		searchTaskFen = fen;
    	    searchTaskMoves = moves;
    		chessEngineSearchTask = new ChessEngineSearchTask();
    		chessEngineSearchTask.execute(fen, moves);	//>249 starts the chess engine search task
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
	public void startEnginePlay()					
    {	//>251 setting play options and start engine play
		ec.setPlaySettings(userP);
		ec.setStartPlay(gc.getValueFromFen(gc.fen, 2));
		ec.initEngines();
//		if (!ec.en_1.isReady | !ec.en_2.isReady)
//			ec.initEngines();
		engineMes = ""; 
		engineStat = ""; 
		initInfoArrays(false);
	}
	public void startEnginePlayIsReady()			
    {	//>252 start engine play after binding
		ec.newGame(gc.isChess960, gc.startFen);		
		ec.chessEnginePaused = false;
		ec.chessEngineInit = false;
		ec.setPlayData(userP);
		startC4aService319();
		if (ec.makeMove)
		{
			ec.chessEngineSearching = true;
//			if (!ec.chessEngineAutoRun)
			if (!ec.chessEngineAutoRun & mate == 0)
				setInfoMessage(false, true, null, getEngineThinkingMessage(), null, null);
			if (!gc.cl.p_fen.equals(""))
				chessEngineBestMove(gc.cl.p_fen, "");
		}
		else
		{
			ec.chessEngineProblem = true;
			if (mate == 0)
				setInfoMessage(false, true, null, c4aM.getString(R.string.player_move), null, null);
			ec.chessEngineSearching = false;
		}
    }
	public CharSequence getEngineThinkingMessage()			
    {
		if (mate > 0)
		{
			messageInfo = getPdbToPgnMessage();
//			Log.i(TAG, "getEngineThinkingMessage(), messageInfo: " + messageInfo);
			return messageInfo;
		}
		if (!gc.isGameLoaded)
		{
			if (ec.chessEnginePlayMod == 5)	// player vs player
			{
				messageEngine = "";
				messageInfo = c4aM.getString(R.string.play_two_players_flip);
				return messageInfo;
			}
			if (ec.chessEnginePlayMod == 6)	// edit
			{
				messageEngine = "";
				messageInfo = c4aM.getString(R.string.menu_modes_edit);
				return messageInfo;
			}
		}
		if (ec.chessEnginePlayMod != 4)
		{
//			Log.i(TAG, "getEngineThinkingMessage(), ec.chessEnginePaused: " + ec.chessEnginePaused);
			if (!ec.chessEnginePaused)
//				return c4aM.getString(R.string.engine_thinking);
				return c4aM.getString(R.string.engine_thinking) + " " + ec.getEngine().engineNameStrength;
			else
				return c4aM.getString(R.string.engine_paused);
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
				return c4aM.getString(R.string.engineAnalysisSearch) + "  <" + analizeEngineName + ">";
			}
			else
				return messageInfo;
		}
    }
	public CharSequence getEnginePausedMessage()			
    {
		if (!gc.isGameLoaded)
		{
			if (ec.chessEnginePlayMod == 5)	// player vs player
			{
				messageEngine = "";
				messageInfo = c4aM.getString(R.string.play_two_players_flip);
				return messageInfo;
			}
			if (ec.chessEnginePlayMod == 6)	// edit
			{
				messageEngine = "";
				messageInfo = c4aM.getString(R.string.menu_modes_edit);
				return messageInfo;
			}
		}
		if (ec.chessEnginePlayMod != 4)
			return c4aM.getString(R.string.engine_paused);
		else
		{
			if (messageInfo.toString().startsWith(c4aM.getString(R.string.engineAnalysisSearch)))
				return messageInfo;
			else
			{
//				messageEngine = "";
//				Log.i(TAG, "getEnginePausedMessage: " + messageInfo.toString());
				return c4aM.getString(R.string.engineAnalysisStop);
			}
		}
    }
	public void initInfoArrays(boolean createArrays)
    {
		if (infoPv == null | createArrays)
		{
			infoPv = new ArrayList<CharSequence>();
			infoMessage = new ArrayList<CharSequence>();
			for (int i = 0; i < c4aM.userP.getInt("user_options_enginePlay_MultiPv", 4); i++)
			{
	    		infoPv.add("");
	    		infoMessage.add("");
			}
		}
		else
		{
	    	for (int i = 0; i < c4aM.userP.getInt("user_options_enginePlay_MultiPv", 4); i++)
			{
	    		infoPv.set(i, "");
	    		infoMessage.set(i, "");
			}
		}
    }
	public void setMoveTime() 
	{
//		Log.i(TAG, "tc.timeControl: " + tc.timeControl);
		if (tc.timeControl == 2)
		{
			if (ec.chessEnginePlayMod == 1)
	    	{
	    		tc.timeWhite = userP.getInt("user_time_player_move", 1000);
	    		tc.timeBlack = userP.getInt("user_time_engine_move", 1000);
	    	}
	    	if (ec.chessEnginePlayMod == 2)
	    	{
	    		tc.timeWhite = userP.getInt("user_time_engine_move", 1000);
	    		tc.timeBlack = userP.getInt("user_time_player_move", 1000);
	    	}
	    	if (ec.chessEnginePlayMod == 3)
	    	{
				tc.timeWhite = userP.getInt("user_time_engine_move", 1000);
				tc.timeBlack = userP.getInt("user_time_engine_move", 1000);
	    	}
		}
	}
	public void setPauseEnginePlay(boolean shutDown)	
    {	//>256 pause engine and shutdown if true
//		Log.i(TAG, "setPauseEnginePlay");
		if (shutDown)
			ec.stopAllEngines();
		else
		{
			ec.stopComputerThinking(false);
			ec.lastChessEnginePaused = ec.chessEnginePaused;
			ec.chessEnginePaused = true;
//			if (gc.gameStat == 3 & !gc.isGameOver & !gc.cl.p_variationEnd)
			if (!gc.isGameOver & !gc.cl.p_variationEnd)
				setInfoMessage(false, true, null, getEnginePausedMessage(), null, null);
		}
    }
	public void continuePausedEngine(boolean isPaused)		
    {	//>257 set pause status(isPaused)
//		Log.i(TAG, "continuePausedEngine");
		ec.chessEnginePaused = isPaused;
		if (!gc.isGameLoaded)
    	{
    		if (!ec.chessEnginePaused)
    		{
    			startChessClock();
    			ec.chessEngineSearching = false;
//    			Log.i(TAG, "gc.fen: " + gc.fen);
    			if (!gc.fen.equals(""))
    			{
	    			if 	(		ec.chessEnginePlayMod == 3
	    					|	ec.chessEnginePlayMod == 1 & gc.getValueFromFen(gc.fen, 2).equals("b")
	    					| 	ec.chessEnginePlayMod == 2 & gc.getValueFromFen(gc.fen, 2).equals("w")
	    				)
	    				ec.chessEngineSearching = true;
    			}
    			updateCurrentPosition("");
				if (ec.chessEngineSearching)
					chessEngineBestMove(gc.cl.p_fen, "");
     		}
    	}
		else
			ec.chessEnginePaused = true;
    }
	public void pauseEnginePlay(int engineAction)		
    {	//258 start/stop engine(button)
//		Log.i(TAG, "pauseEnginePlay, ec.chessEnginePaused, engineAction: " + ec.chessEnginePaused + ", " + engineAction);
//		if (ec.chessEnginePlayMod != 4)
//			ec.stopComputerThinking(false);
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
    		if (ec.chessEngineSearching)
      			chessEngineBestMove(gc.cl.p_fen, "");
     	}
    	else
    	{
    		if (ec.chessEnginePlayMod == 4)
    		{
    			setInfoMessage(false, true, null, c4aM.getString(R.string.engineAnalysisStopWait), null, null);
    			
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
//		Log.i(TAG, "pauseEnginePlay, ec.chessEnginePaused, engineAction: " + ec.chessEnginePaused + ", " + engineAction);
		ec.lastChessEnginePaused = ec.chessEnginePaused;
    }
	public void nextGameEngineAutoPlay()	
    {	// next game(engine auto play)
		gc.isGameOver = false;
		gc.isGameUpdated = true;
		ec.chessEnginePaused = false;
 		ec.lastChessEnginePaused = false;
		if 	(	userP.getBoolean("user_play_eve_autoFlipColor", true)
				& !ec.en_2.engineServiceName.equals("")
			)
		{
			CharSequence e1Name = ec.en_1.engineServiceName;
			CharSequence e2Name = ec.en_2.engineServiceName;
//			ec.stopAllEngines();
			try {Thread.sleep(150);} 
			catch (InterruptedException e) {}
			SharedPreferences.Editor ed = c4aM.userP.edit();
			ed.putString("user_play_engine_1", e2Name.toString());
			ed.putString("user_play_engine_2", e1Name.toString());
			ed.commit();
     		ec.chessEnginePaused = false;
     		ec.lastChessEnginePaused = false;
    		ec.chessEngineInit = true;
		}
		int chess960Id = userP.getInt("user_game_chess960Id", 518);
		if (chess960Id != 518)
		{
			if (ec.twoEngines)
			{
				if (cntChess960 == 0)
					cntChess960++;
				else
				{
					cntChess960 = 0;
					chess960Id = 999;	// == random
				}
			}
			else
				chess960Id = 999;	// == random
		}
		ec.setPlaySettings(userP);
		if 	(!userP.getBoolean("user_play_eve_autoCurrentGame", false))
		{
			getGameData(gc.fileBase, gc.filePath, gc.fileName, "", false, false, gc.startMoveIdx);
			startPlay(true);
		}
		else                                                                        
		{
			gc.isGameOver = false;
			gc.cl.history.setGameTag("Result", "*");
	        gc.cl.moveHistoryPrefs = c4aM.moveHistoryP.getString("run_moveHistory", "");
			getGameData(gc.fileBase, gc.filePath, gc.fileName, gc.startPgn, true, false, gc.startMoveIdx);
			startPlay(false);
		}
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
	public boolean autoStopEngineAutoPlay(int bestScore, CharSequence fen)	
    {
		boolean autoStop = false;
		if (ec.getEngine().statIsMate)
			return false;
		CharSequence display = getDisplayScore(bestScore, fen);
		if (bestScore > 899 | bestScore < -899)
		{	// !!! set auto game result/NAG: > < 600: NAG, > < 900: NAG; moveText: score
			if (!display.toString().startsWith("-"))
				autoStopResult = c4aM.getString(R.string.cl_resultWhite);
			else
				autoStopResult = c4aM.getString(R.string.cl_resultBlack);
//			Log.i(TAG, "bestScore, autoStopResult: " + bestScore + ", " + autoStopResult);
			autoStop = true;
		}
		if (bestScore == 0 & !ec.chessEnginesOpeningBook)
		{
			if (autoStopDrawCount >= 3)
			{
				autoStopResult = c4aM.getString(R.string.cl_resultDraw);
				autoStop = true;
			}
			else
				autoStopDrawCount++;
//			Log.i(TAG, "bestScore == 0, autoStopDrawCount: " + autoStopDrawCount);
		}
		else
			autoStopDrawCount = 0;
		if (autoStop)
		{
			gc.isGameOver = true;
			autoStopMoveText = c4aM.getString(R.string.engineAutoStop) + " " + display;
			startC4aService315(autoStopResult, autoStopMoveText);
		}
		return autoStop;
    }
	public void selectEngine(int engineNumber, ArrayList<CharSequence> engineList) 
	{
		ec.tmpEngineNumber = engineNumber;
		ec.engineNameList = new ArrayList<CharSequence>();
		for (int i = 0; i < engineList.size(); i++)
		{
			CharSequence tmp[] = engineList.get(i).toString().split("\b");
    		if (tmp.length > 1)
    			ec.engineNameList.add(tmp[0]);
		}
		CharSequence[] items = new CharSequence[ec.engineNameList.size()];
		if (ec.engineNameList.size() > 0)
		{
			for (int i = 0; i < ec.engineNameList.size(); i++)
			{
				items[i] = ec.engineNameList.get(i);
			}
		}
		AlertDialog.Builder builder = new AlertDialog.Builder(c4aM);
		builder.setTitle(R.string.engineSelectDialog);
		builder.setItems(items, new DialogInterface.OnClickListener() 
		{
		    public void onClick(DialogInterface dialog, int item) 
		    {
		    	CharSequence tmp[] = ec.engineList.get(item).toString().split("\b");
//		    	Log.i(TAG, "item, tmp[1]: " + item + ", " + tmp[1]);
		    	CharSequence serviceId = "";
				SharedPreferences.Editor ed = c4aM.userP.edit();
				
				if (tmp.length > 1)
				{
					serviceId = tmp[1];
//					Log.i(TAG, "serviceId: " + ec.tmpEngineNumber + ", " + serviceId);
//					Log.i(TAG, "ec.en_1.engineServiceName: " + ec.en_1.engineServiceName);
//					Log.i(TAG, "ec.en_2.engineServiceName: " + ec.en_2.engineServiceName);
					switch (ec.tmpEngineNumber)
			        {
				        case 1:     // E1
				        	if (!ec.en_1.engineServiceName.equals(serviceId))
							{
								ec.setEngineNumber(1);
						    	if (ec.getEngine() != null)	
						    		ec.stopComputerThinking(true);
								ed.putString("user_play_engine_1", serviceId.toString());
								ed.commit();
								ec.en_1.engineServiceName = "";
								ec.chessEnginePaused = true;
								ec.lastChessEnginePaused = true;
					    		ec.chessEngineInit = true;
					    		en1Selected = true;
					    		updateCurrentPosition("");
							}
				        	break;
				        case 2:     // E2
				        	if (!ec.en_2.engineServiceName.equals(serviceId))
							{
								ec.setEngineNumber(2);
						    	if (ec.getEngine() != null)	
						    		ec.stopComputerThinking(true);
								ed.putString("user_play_engine_2", serviceId.toString());
						        ed.commit();
						        ec.en_2.engineServiceName = "";
						        ec.chessEnginePaused = true;
								ec.lastChessEnginePaused = true;
					    		ec.chessEngineInit = true;
					    		en2Selected = true;
					    		updateCurrentPosition("");
							}
				            break;
				        case 9:     // engine settings (start Activity "Chess Engines" or "Stockfish Engine"
				        	if (serviceId.toString().endsWith(".IChessEngineService"))
				        	{
				        		isSearchTaskStopped = true;
				        		ec.stopAllEngines();
								ec.chessEngineInit = true;
								en1Selected = true;
								en2Selected = true;
								ec.chessEnginePaused = true;
								CharSequence packageId = serviceId.toString().replace(".IChessEngineService", "");
//				        		Log.i(TAG, "serviceId: " + serviceId);
//				        		Log.i(TAG, "packageId: " + packageId);
				        		Intent i = c4aM.getPackageManager().getLaunchIntentForPackage((String) packageId);
				        		i.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);	 // + onActivityResult()
				        		c4aM.startActivityForResult(i, ENGINE_SETTING_REQUEST_CODE);
				        	}
				            break;
			        }
				}
		    }
		});
		builder.create().show();
	}
	public void switchEngines() 
	{
		SharedPreferences.Editor ed = userP.edit();
		String en1 = userP.getString("user_play_engine_1", "");
		String en2 = userP.getString("user_play_engine_2", "");
		ec.stopAllEngines();
    	ec.setPlaySettings(userP);
		ec.chessEngineInit = true;
		ec.en_1.engineServiceName = "";
		ec.en_2.engineServiceName = "";
		ed.putString("user_play_engine_1", en2);
		ed.putString("user_play_engine_2", en1);
		ed.commit();
		en1Selected = true;
		en2Selected = true;
		if (!gc.isGameOver & !gc.cl.p_variationEnd)
		{
			ec.chessEnginePaused = ec.lastChessEnginePaused;
			if (ec.chessEnginePaused)
				setInfoMessage(false, true, null, c4aM.getString(R.string.engine_pausedNotConnected), null, null);
			else
			{
				engineMes = ""; initInfoArrays(false);
				setInfoMessage(false, true, null, c4aM.getString(R.string.engineProgressDialog), "", null);
				startChessClock();
				startEnginePlay();
			}
		}
		else
		{
			ec.chessEnginePaused = true;
			ec.lastChessEnginePaused = true;
			updateCurrentPosition("");
		}
	}
//	PDB to PGN		PDB to PGN		PDB to PGN		PDB to PGN		PDB to PGN
	public Runnable mUpdatePdbPgn = new Runnable() 		
	{	// PDB to PGN: Handler
		   String pgn;
		   boolean isEngineOk = true;
		   public void run() 
		   {
//			   	Log.i(TAG, "start mUpdatePdbPgn");
			   	if (isMessageResult)
			   	{
			   		isMessageResult = false;
			   		pdbToPgnUpdateFiles(pgn, logMessage);
//			   		Log.i(TAG, "isMessageResult: " + logMessage);
			   		gameData = "";
			   	}
			   	
			   	if (isSearchResult)
			   	{	// return fromd searchTask
			   	   isSearchResult = false;
//				   Log.i(TAG, "engineMessage: " + engineMessage);
				   pgn = getPgnFromEngineAnalysis(pgnTags, engineMessage, gameResult);
				   if (pgn.startsWith("[Event "))
					   pdbToPgnSetLogMessage(puzzleId, PDB_OK);
				   pdbToPgnUpdateFiles(pgn, logMessage);
				   if (engineMessage.equals("$$NO_RESPOND"))
				   {
					   	mate = 0;
				   		handlerPdbPgn.removeCallbacks(mUpdatePdbPgn);
				   		return;
				   }
//				   Log.i(TAG, "isSearchResult: " + logMessage);
				   gameData = "";
			   	}
			   	
			   	if (isPdbEnd)
			   	{
			   		pdbToPgnEnd();
			   		return;
			   	}
			   	
			   	String line;
		    	try 
		    	{
		    		pgnRaf.seek(gameOffset);
		    		line = pgnRaf.readLine();
//		    		Log.i(TAG, "gameOffset: " + gameOffset);
		    		while (line != null)
					{
						if (line.startsWith("p=") & !gameData.equals(""))
						{
//							Log.i(TAG, gameData);
//							Log.i(TAG, " ");
							pgn = getPgnFromPdb(gameData);
							if (pgn.startsWith("[Event "))
							{	// start search
//								Log.i(TAG, "start chessEngineSearchTask, searchTaskFen: " + searchTaskFen);
								pgnTags = pgn;
								isEngineOk = pdbToPgnSearchTask(searchTaskFen, "");
								if (!isEngineOk)
									pdbToPgnUpdateFiles("", logMessage);
					    		handlerPdbPgn.removeCallbacks(mUpdatePdbPgn);
					    		return;
							}
							else
							{	// error? > message
								isMessageResult = true;
								handlerPdbPgn.removeCallbacks(mUpdatePdbPgn);
					    		handlerPdbPgn.postDelayed(mUpdatePdbPgn, 0);
					    		return;
							}
						}
						if (!line.equals(""))
							gameData = gameData + line + "\n";
						gameOffset = pgnRaf.getFilePointer();
						line = pgnRaf.readLine();
					}
		    		// 	line == null: end
		    		isPdbEnd = true;
//		    		Log.i(TAG, "PDB to PGN . . .    end, gameData:\n" + gameData);
		    		if (!gameData.equals(""))
		    		{
						pgn = getPgnFromPdb(gameData);
//						Log.i(TAG, "getPgnFromPdb():\n" + pgn);
						if (pgn.startsWith("[Event "))
						{	// start search (last game)
							ec.getEngine().newGame();
							pgnTags = pgn;
				    		isEngineOk = pdbToPgnSearchTask(searchTaskFen, "");
							if (!isEngineOk)
							{
								pdbToPgnUpdateFiles("", logMessage);
								pdbToPgnEnd();
								handlerPdbPgn.removeCallbacks(mUpdatePdbPgn);
							}
				    		return;
						}
						else
						{	// error? > message (last game)
							isMessageResult = true;
							handlerPdbPgn.removeCallbacks(mUpdatePdbPgn);
				    		handlerPdbPgn.postDelayed(mUpdatePdbPgn, 0);
				    		return;
						}
		    		}
		    		else
		    		{	// error? > message (last game)
						isMessageResult = true;
						handlerPdbPgn.removeCallbacks(mUpdatePdbPgn);
			    		handlerPdbPgn.postDelayed(mUpdatePdbPgn, 0);
			    		return;
					}
		    	} 
		    	catch (FileNotFoundException e) {e.printStackTrace(); }
		    	catch (IOException e) {e.printStackTrace(); }
		   }
	};
	public boolean pdbToPgnSearchTask(CharSequence fen, CharSequence moves)																
    {
		boolean isSearchOk = true;
//		if (ec.chessEngineInit)
		if (ec.chessEngineInit | !ec.getEngine().getIsProcessAlive() | !ec.getEngine().getIsReady())
		{
			if (!ec.getEngine().getIsProcessAlive() | !ec.getEngine().getIsReady())
				isSearchOk = false;
			ec.chessEnginePaused = false;
	    	ec.lastChessEnginePaused = false;
			ec.chessEngineInit = false;
			initChessClock();
			gc.fen = fen;
			gc.cl.p_fen = fen;
			startEnginePlay();
		}
		else
		{
//			if (!ec.getEngine().getIsProcessAlive() | !ec.getEngine().getIsReady())
//			{
//				pdbToPgnSetLogMessage(puzzleId, PDB_ENGINE_ERROR);
//				isSearchOk = false;
//			}
//			else
			ec.getEngine().newGame();
			chessEngineBestMove(fen, moves);
		}
		return isSearchOk;
    }
	public String getPdbToPgnMessage()																
    {
		String message = "PDB to PGN\n";
		message = message + "File: " + userP.getString("user_pdb_pgn_input", "") + "\n";
		message = message + "Puzzle-ID: " + puzzleId + "\n";
		message = message + "Progress: " + gameOffset / 1000 + "(" + inputSize + ") KB\n";
		message = message + "Puzzles, PDB: " + puzzlesPdb + ", PGN: " + puzzlesPgn;
		return message;
    }
	public boolean initPdbPgn()																
    {
		logMessage = "";
		isPdbEnd = false;
		isSearchResult = false;
		inputSize = 0;
		gameOffset = 0;
		puzzlesPdb = 0;
		puzzlesPgn = 0;
		File fo = new File(userP.getString("user_pdb_pgn_output", ""));
    	if (fo.exists())
    		fo.delete();	// delete *.pgn
    	File fd = new File(userP.getString("user_pdb_pgn_output", "") + "-db");
    	if (fd.exists())
    		fd.delete();	// delete *.pgn-db
    	File fl = new File(userP.getString("user_pdb_pgn_logfile", ""));
    	if (fl.exists())
    		fl.delete();	// delete *.log
    	try 
    	{
    		pgnRaf = new RandomAccessFile(userP.getString("user_pdb_pgn_input", ""), "r");
    		inputSize = pgnRaf.length() / 1000;
    	}
    	catch (FileNotFoundException e) {e.printStackTrace(); return false;}
    	catch (IOException e) {e.printStackTrace(); return false;}
    	return true;
    }
	public String getPgnFromPdb(String pdbData)																
    {
    	pdbData = pdbData.replace("0-0-0", "O-O-O");
    	pdbData = pdbData.replace("0-0", "O-O");
    	String pgn = PDB_ERROR_UNKNOWN;
    	String[] lines = pdbData.split("\\n");
    	String event = "?";
    	String site = "?";
    	String date = "????.??.??";
    	String round = "-";
    	String white = "?";
    	String black = "?";
    	String result = "1-0";
    	String setUp = "1";
    	String fen = "";
    	String puzzleGenre = "?";
    	String puzzleAuthor = "?";
    	String puzzleYear = "?";
    	String puzzleSource = "?";
    	String puzzlePeaceN = "?";
    	String puzzlePeaceW = "?";
    	String puzzlePeaceB = "?";
    	
    	String peaces = "?";
    	String castling = "-";
    	String ep = "-";
    	int lCnt = 0;
    	int tCnt = 0;
    	
    	for(String s: lines)
    	{
    		// logfile: messages, errors
    		if (!pdbData.startsWith("p="))
    			{pdbToPgnSetLogMessage("?", PDB_ERROR_ID);  return "";}
    	    if (s.startsWith("p="))
    	    	puzzleId = s.substring(2, s.length());
    	    if (s.startsWith("l=")) 
    	    {
    	    	lCnt++;
    	    }
    	    if (s.startsWith("t="))
    		{
    	    	tCnt++;
    			if (!s.startsWith("t=#") | s.contains("h") | s.contains(" "))
    				{pdbToPgnSetLogMessage(puzzleId, PDB_ERROR_GENRE);  return "";}
    		}
    	    if (s.startsWith("n="))
    	    {
    	    	s = s.replace("n=", "");
    	    	String[] sSplit = s.split("\t");
    	    	if (sSplit.length > 1)
    	    		puzzleAuthor = sSplit[0];
    	    	else
    	    		puzzleAuthor = s;
    	    }
    	    if (s.startsWith("r=")) 
    	    {
    	    	if (s.contains("RA"))
    	    		{pdbToPgnSetLogMessage(puzzleId, PDB_ERROR_RA + s);  return "";}
    	    }
//    	    if (pdbData.contains("EPw"))
//    	    	{pdbToPgnSetLogMessage(puzzleId, PDB_EP_ERROR);  return "";}
    	    if (s.startsWith("s="))
    	    {
    	    	s = s.replace("s=", "");
    	    	Calendar c = Calendar.getInstance();
    	    	boolean isYear = false;
    	    	int year = c.get(Calendar.YEAR);
    	    	int y = 0;
	    		int m = 0;
	    		int d = 0;
    	    	String[] sSplit = s.split("\t");
    	    	site = "";
    	    	for(int i = 0; i < sSplit.length; i++)
                {
    	    		if (i == 0)
    	    			event = sSplit[i];
    	    		else
    	    		{
    	    			if (site.equals(""))
    	    				site = site + sSplit[i];
    	    			else
    	    				site = site + " " + sSplit[i];
    	    			if (!isYear)
    	    			{
	    	    			if (sSplit[i].contains("/"))
	        	    		{
	    	    				String[] sDate = sSplit[i].split("/");
	    	    				String disY = "????";
	    	    				String disM = "??";
	    	    				String disD = "??";
	    	    				for(int j = sDate.length -1; j >= 0; j--)
	    	                    {
	    	    					if (j == sDate.length -1)
	    	    					{
	    	    						try {y = Integer.parseInt(sDate[j]);}
	    	                        	catch (NumberFormatException e) {y = 0;}
	    	    						if (y > 0)
	    	    							disY = sDate[j];
	    	    					}
	    	    					if (j == sDate.length -2)
	    	    					{
	    	    						try {m = Integer.parseInt(sDate[j]);}
	    	                        	catch (NumberFormatException e) {m = 0;}
	    	    						if (m > 0)
	    	    							disM = sDate[j];
	    	    					}
	    	    					if (j == sDate.length -3)
	    	    					{
	    	    						try {d = Integer.parseInt(sDate[j]);}
	    	                        	catch (NumberFormatException e) {d = 0;}
	    	    						if (d > 0)
	    	    							disD = sDate[j];
	    	    					}
	    	                    }
	   	    					puzzleYear = disY;
	   	    					date = disY + "." + disM + "." + disD;
	   	    					isYear = true;
	        	    		}
	    	    			else
	    	    			{
	    	    				try {y = Integer.parseInt(sSplit[i]);}
	                        	catch (NumberFormatException e) {y = 0;}
	    	    				if (y > 1400 & y <= year)
	    	    				{
	    	    					puzzleYear = Integer.toString(y);
	    	    					date = puzzleYear + ".??.??";
	     	    				}
	    	    			}
    	    			}
    	    		}
                }
    	    	if (site.equals(""))
    	    		site = "?";
    	    }
    	    if (s.startsWith("f="))
    	    {
    	    	fen = s.replace("f=", "");
//    	    	Log.i(TAG, "pdb fen: " + fen + ", " + fen.length());
    	    	fen = fen.replace('B', 'P');
    	    	fen = fen.replace('b', 'p');
    	    	fen = fen.replace('D', 'Q');
    	    	fen = fen.replace('d', 'q');
    	    	fen = fen.replace('T', 'R');
    	    	fen = fen.replace('t', 'r');
    	    	fen = fen.replace('L', 'B');
    	    	fen = fen.replace('l', 'b');
    	    	fen = fen.replace('S', 'N');
    	    	fen = fen.replace('s', 'n');
    	    	if (!pdbToPgnCheckFen(fen))
    	    		{pdbToPgnSetLogMessage(puzzleId, PDB_FEN_ERROR + fen);  return "";}
    	    	if (pdbData.contains("O-O"))
    	    		castling = pdbToPgnCastling(pdbData);
    	    	fen = fen + " w " + castling + " " + ep + " 0 1";
//    	    	gc.cl.newPosition("518", fen, "", "", "", "", "", "");
//    	    	if (!gc.cl.p_stat.equals("1"))
//    	    	{
//    	    		gc.cl.newPosition("518", gc.cl.history.fenStandardPosition, "", "", "", "", "", "");
//    	    		{pdbToPgnSetLogMessage(puzzleId, PDB_FEN_ERROR + fen);  return "";}
//    	    	}
    	    	searchTaskFen = fen;
    	    }
    	    if (s.startsWith("t="))
    	    {
    	    	puzzleGenre = s.substring(2, s.length());
    	    	String mateStr = puzzleGenre.replace("#", "");
    	    	try {mate = Integer.parseInt(mateStr);}
            	catch (NumberFormatException e) {mate = 0;}
    	    	if (mate < 2)
    				{pdbToPgnSetLogMessage(puzzleId, PDB_MATE_ERROR);  return "";}
    	    }
    	    if (s.startsWith("c="))
    	    {
    	    	s = s.replace("c=", "");
    	    	peaces = s;
    	    	String cntW = "";
    	    	String cntB = "";
    	    	s = s.replace("(", "");
    	    	s = s.replace(")", "");
    	    	boolean isFirst = true;
    	    	int peaceCnt = 0;
    	    	int wPeaceCnt = 0;
    	    	for (int i = 0; i < s.length(); i++)
		        {
    	    		
    	    		if (s.charAt(i) != '+')
    	    		{
	    	    		if (isFirst)
	    	    			cntW = cntW + s.charAt(i);
	    	    		else
	    	    			cntB = cntB + s.charAt(i);
    	    		}
    	    		else
    	    			isFirst = false;
		        }
    	    	try 
    	    	{
    	    		peaceCnt = Integer.parseInt(cntW) + Integer.parseInt(cntB);
    	    		wPeaceCnt = Integer.parseInt(cntW);
    	    	}
            	catch (NumberFormatException e) {peaceCnt = 0;}
     	    	puzzlePeaceN = Integer.toString(peaceCnt);
    	    	puzzlePeaceW = cntW;
    	    	puzzlePeaceB = cntB;
    	    	if (peaceCnt < 5)
    	    		{pdbToPgnSetLogMessage(puzzleId, PDB_PEACE_COUNT_LESS_5);  return "";}
    	    	if (wPeaceCnt < 3)
	    			{pdbToPgnSetLogMessage(puzzleId, PDB_WHITE_PEACE_COUNT_LESS_3);  return "";}
    	    }
    	}
    	// check data before creating PGN
    	if (tCnt != 1)
			{pdbToPgnSetLogMessage(puzzleId, PDB_T_ERROR);  return "";}
    	if (lCnt == 0)
    		{pdbToPgnSetLogMessage(puzzleId, PDB_NO_SOLUTION);  return "";}
    	
    	puzzleSource = event;
    	if (!site.equals("?"))
    		puzzleSource = puzzleSource + " " + site;
    	white = puzzleAuthor;
    	black = puzzleGenre + "\t" + peaces;
    	// OK: create pgn
	    pgn = "[Event \"" + event + "\"]\n";
	    pgn = pgn + "[Site \"" + site + "\"]\n";
	    pgn = pgn + "[Date \"" + date + "\"]\n";
	    pgn = pgn + "[Round \"" + round + "\"]\n";
	    pgn = pgn + "[White \"" + white + "\"]\n";
	    pgn = pgn + "[Black \"" + black + "\"]\n";
	    pgn = pgn + "[Result \"" + result + "\"]\n";
	    pgn = pgn + "[SetUp \"" + setUp + "\"]\n";
	    pgn = pgn + "[FEN \"" + fen + "\"]\n";
	    pgn = pgn + "[PuzzleGenre \"" + puzzleGenre + "\"]\n";
	    pgn = pgn + "[PuzzleId \"" + puzzleId + "\"]\n";
	    pgn = pgn + "[PuzzleAuthor \"" + puzzleAuthor + "\"]\n";
	    pgn = pgn + "[PuzzleYear \"" + puzzleYear + "\"]\n";
	    pgn = pgn + "[PuzzleSource \"" + puzzleSource + "\"]\n";
	    pgn = pgn + "[PuzzlePeacesN \"" + puzzlePeaceN + "\"]\n";
	    pgn = pgn + "[PuzzlePeacesW \"" + puzzlePeaceW + "\"]\n";
	    pgn = pgn + "[PuzzlePeacesB \"" + puzzlePeaceB + "\"]\n\n";

	    String first = getFirstMove(pdbData);
	    if (first.startsWith("??"))
	    	{pdbToPgnSetLogMessage(puzzleId, first);  return "";}
	    else
	    {
	    	firstMove = first;
	    	String pgnCheck = pgn + "\n\n" + result;
		    gc.cl.newPositionFromPgnData("", "", "", pgnCheck, false, 0, false);
			if (!gc.cl.p_stat.equals("1"))
				{pdbToPgnSetLogMessage(puzzleId, PDB_PGN_PARSE_ERROR + gc.cl.p_message);  return "";}
	    }
    	return pgn;
    }
	public String getPgnFromEngineAnalysis(String pgnTags, String engineMessage, String result)																
    {
		String pgn = pgnTags;
		if (engineMessage.equals("$$TIMEOUT"))
			{pdbToPgnSetLogMessage(puzzleId, PDB_TIMEOUT + "> "+ (MAX_SEARCH_TIME / 1000) + " sec.");  return "";}
		if (engineMessage.equals("$$NO_RESPOND"))
			{pdbToPgnSetLogMessage(puzzleId, PDB_ENGINE_ERROR);  return "";}
		if (!engineMessage.contains("1."))
			{pdbToPgnSetLogMessage(puzzleId, PDB_NO_ENGINE_MATE + engineMessage);  return "";}
		firstMove = firstMove.replace('D', 'Q');
		firstMove = firstMove.replace('T', 'R');
		firstMove = firstMove.replace('L', 'B');
		firstMove = firstMove.replace('S', 'N');
		firstMove = firstMove.replace('P', ' ');
		engineMessage = engineMessage.replace(".", ". ");
		String[] split = engineMessage.split(" ");
		if (split.length > 0)
		{
			if (!split[0].contains("(M"))
				{pdbToPgnSetLogMessage(puzzleId, PDB_NO_ENGINE_MATE + engineMessage);  return "";}
			if (!split[2].equals(firstMove) & !firstMove.equals(""))
				{pdbToPgnSetLogMessage(puzzleId, PDB_FIRST_MOVE_PDB_PGN + firstMove + ", " + split[2]);  return "";}
			engineMessage = engineMessage.replace(split[0] + " ", "");
		}
		else
			{pdbToPgnSetLogMessage(puzzleId, PDB_NO_ENGINE_MATE + engineMessage);  return "";}
		gameData = gameData.replace("{", "(");
		gameData = gameData.replace("}", ")");
		gameData = gameData.replace("\n", "||");
//		pgn = pgn + "\n\n" + engineMessage + " {" + gameData + "} " + result + "\n";
		pgn = pgn + engineMessage + " {" + gameData + "} " + result + "\n";
		return pgn;
    }
	public boolean pdbToPgnCheckFen(String fen)																
    {	// validate FEN
		for (int i = 0; i < fen.length(); i++)
        {
    		CharSequence cs = "" + fen.charAt(i);
    		if (!FEN_CHARACTERS.contains(cs))
    			return false;
        }
		if (!fen.contains("k") | !fen.contains("K"))
			return false;
		return true;
    }
    public String pdbToPgnCastling(String pdbData)																
    {	// get castling from move section
    	String castling = "-";
    	String castK = "";
    	String castQ = "";
    	String castk = "";
    	String castq = "";
    	String[] lines = pdbData.split("\\n");
    	for(String s: lines)
    	{
    		if (s.startsWith("l="))
			{
    			String[] s2 = s.split(" ");
    			for (int i = 0; i < s2.length; i++) 
    	    	{
    				if (s2[i].contains("O-O") & !s2[i].contains("O-O-O"))
    				{
    					if (s2[i -1].endsWith("...") | !s2[i -1].endsWith("."))
    						castk = "k";
    					else
    						castK = "K";
    				}
    				if (s2[i].contains("O-O-O"))
    				{
    					if (s2[i -1].endsWith("...") | !s2[i -1].endsWith("."))
    						castq = "q";
    					else
    						castQ = "Q";
    				}
    	    	}
    		}
    		if (!castK.equals("") | !castQ.equals("") | !castk.equals("") | !castq.equals(""))
    			castling = castK + castQ + castk + castq;
    	}
    	return castling;
    }
    public String getFirstMove(String pdbData)																
    {
    	firstMove = "";
    	String[] lines = pdbData.split("\\n");
    	for(String s: lines)
    	{
    		s = s.replace("l= 1.", "l=1.");
    		if (s.startsWith("l=1. "))
			{
    			String[] first = s.split(" ");
    			if (first[1].contains("+"))
    				return PDB_FIRST_MOVE_CHECK;
    			if (!first[1].contains("?"))
    			{
    				first[1] = first[1].replace("!", "");
    				return first[1];
    			}
    		}
    	}
		return PDB_NO_FIRST_MOVE;
    }
    public void pdbToPgnUpdateFiles(String pgnData, String logMessage)																
    {
    	puzzlesPdb++;
    	if (pgnData.startsWith("[Event "))
		{
//			Log.i(TAG, pgnData);
			try
			{ 
				File f = new File(userP.getString("user_pdb_pgn_output", ""));
				FileOutputStream fOut;
				pgnData = "\n" + pgnData;
				fOut = new FileOutputStream(f, true);
				OutputStreamWriter osw = new OutputStreamWriter(fOut);  
				osw.write(pgnData); 
	            osw.flush(); 
	            osw.close();
	            puzzlesPgn++;
			} 
			catch (FileNotFoundException e) 	{e.printStackTrace();} 
			catch (IOException e)				{e.printStackTrace();} 
			updateGui();
		}
    	if (!logMessage.equals(""))
    	{
	    	try
			{ 
				File f = new File(userP.getString("user_pdb_pgn_logfile", ""));
				FileOutputStream fOut;
				fOut = new FileOutputStream(f, true);
				OutputStreamWriter osw = new OutputStreamWriter(fOut);  
				osw.write(logMessage); 
	            osw.flush(); 
	            osw.close();
			} 
			catch (FileNotFoundException e) 	{e.printStackTrace();} 
			catch (IOException e)				{e.printStackTrace();}
    	}
//    	updateGui();
    }
    public void pdbToPgnSetLogMessage(String puzzleId, String message)																
    {
    	logMessage = puzzleId + ", " + message + ", O:"+ gameOffset + "\n";
    }
    public void pdbToPgnEnd()																
    {
//    	Log.i(TAG, "pdbToPgnEnd()");
    	mate = 0;
   		handlerPdbPgn.removeCallbacks(mUpdatePdbPgn);
    	pauseEnginePlay(0);
    	updateCurrentPosition("");
    	ec.stopComputerThinking(false);
    	gc.isGameOver = false;
    	gc.cl.p_variationEnd = false;
    }
//	HELPERS		HELPERS		HELPERS		HELPERS		HELPERS		HELPERS
	public int getImageSet() 
    {	// chess board -  get image
    	return userP.getInt("user_options_chessBoard", 1);
    }
	public int getChessFieldSize()
	{	// graphic on chess board -  get chess field size
    	int size = 29;		// small
    	Display display = ((WindowManager) c4aM.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        displayWidth = display.getWidth();
        if (displayWidth >= 320)
        	size = (displayWidth -16) / 8;		
        if (displayWidth >= 0)
        	scrollingWidth = (displayWidth / 10) * 4;
//        Log.i(TAG, "displayWidth, scrollingWidth: " + displayWidth + ", " + scrollingWidth);
    	return size;
	}
	public void setButtonPosition(boolean directionRight)
	{
		if (directionRight)
			btnScrollId++;
		else
			btnScrollId--;
		if (btnScrollId > 4)
			btnScrollId = 1;
		if (btnScrollId < 1)
			btnScrollId = 4;
		setButtonPosition(btnScrollId);
	}
	public void setButtonPosition(int position)
	{
		btn_a1.setVisibility(RelativeLayout.INVISIBLE);
		btn_a2.setVisibility(RelativeLayout.INVISIBLE);
		btn_a3.setVisibility(RelativeLayout.INVISIBLE);
		btn_a4.setVisibility(RelativeLayout.INVISIBLE);
		if (position > 4 | position < 1)
			position = 1;
		btnScrollId = position;
		switch (btnScrollId)
		{
		case 1:
			btn_a1.setVisibility(RelativeLayout.VISIBLE);
			scroll_left.setImageDrawable(c4aM.getResources().getDrawable(R.drawable.scroll_red));
			scroll_right.setImageBitmap(rotateImage(R.drawable.scroll_yellow, 180));
			break;
		case 2:	
			btn_a2.setVisibility(RelativeLayout.VISIBLE);
			scroll_left.setImageDrawable(c4aM.getResources().getDrawable(R.drawable.scroll_green));
			scroll_right.setImageBitmap(rotateImage(R.drawable.scroll_blue, 180));
			break;
		case 3:
			btn_a3.setVisibility(RelativeLayout.VISIBLE);
			scroll_left.setImageDrawable(c4aM.getResources().getDrawable(R.drawable.scroll_yellow));
			scroll_right.setImageBitmap(rotateImage(R.drawable.scroll_red, 180));
			break;
		case 4:
			btn_a4.setVisibility(RelativeLayout.VISIBLE);
			scroll_left.setImageDrawable(c4aM.getResources().getDrawable(R.drawable.scroll_blue));
			scroll_right.setImageBitmap(rotateImage(R.drawable.scroll_green, 180));
			break;
		}
	}
	public int getIntFromString(CharSequence intValue)
	{
    	int valInt = 0;
    	try		{valInt = Integer.parseInt(intValue.toString());}
    	catch 	(NumberFormatException e) {valInt = 0;}
    	return valInt;
	}
	public void writePgnErrorFile(String message)
	{
		if (runP.getBoolean("run_isActivate", false))
		{
			try
			{ 
				String path = pgnIO.getExternalDirectory(0) + fmPrefs.getString("fm_extern_load_path", "");
				String file = "pgn.err";
				File f = new File(path + file);
				boolean append = f.exists();
				FileOutputStream fOut;
				if (append)
					fOut = new FileOutputStream(f, true);
				else
					fOut = new FileOutputStream(f); 
				OutputStreamWriter osw = new OutputStreamWriter(fOut);
				message = message + "\n";
				osw.write(message); 
	            osw.flush(); 
	            osw.close();
			} 
			catch (FileNotFoundException e) 	{e.printStackTrace();} 
			catch (IOException e)				{e.printStackTrace();} 
		}
	}
	public boolean getPgnFromIntent() 
	{
		boolean copyOk = false;
        Intent intent = c4aM.getIntent();
        if (intent.getData() != null)
        {
	        String intentUri = intent.getDataString();
//	        Log.i(TAG, "intentUri: " + intentUri);
	        if (intentUri.endsWith(".pgn") | intentUri.endsWith(".pgn-db"))	//".pgn-db" canceled
	        {
	        	pgnIO = new PgnIO();
	        	String target = pgnIO.getExternalDirectory(0) + fmPrefs.getString("fm_extern_load_path", "");
	        	File fUri = new File(intentUri);
	        	String fName = fUri.getName();
	        	if (pgnIO.moveFile(intentUri, target))
	        	{
//	        		Log.i(TAG, "moveFile OK");
	        		copyOk = true;
	        	}
	        	else
	        	{
//	        		Log.i(TAG, "copyFile OK");
	        		if (pgnIO.fileExists(target, fName))
	        		{
	        			Toast.makeText(c4aM, c4aM.getString(R.string.fmDownloadFileExists), Toast.LENGTH_LONG).show();
	        			return false;
	        		}
	        		if (pgnIO.copyFile(intentUri, target))
	        			copyOk = true;
	        	}
        		if (copyOk)
        		{
        			if (intentUri.endsWith(".pgn"))
        			{
	        			SharedPreferences.Editor ed = fmPrefs.edit();
	        			ed.putString("fm_extern_load_file", fName);
	        			ed.commit();
	        			startFileManager(LOAD_GAME_REQUEST_CODE, 1, 0);
        			}
        			if (intentUri.endsWith(".pgn-db"))
        				Toast.makeText(c4aM, c4aM.getString(R.string.fmFileDownloaded) + " " + fName, Toast.LENGTH_LONG).show();
        		}
        		else
        			Toast.makeText(c4aM, c4aM.getString(R.string.fmDownloadError) + " " + fName, Toast.LENGTH_LONG).show();
	        }
        }
        return copyOk;
    }
	public String getAnalizesTime()
	{
		String time = "time: ";
		long sec = 0;
		long min = 0;
		long diff = analysisEndTime - analysisStartTime;
		if (diff > 0)
		{
			sec = diff / 1000;
			min = diff / 60000;
			if (min > 0)
			{
				sec = sec % 60;
				return time + min + " min " + sec + " sec";
			}
			else
				return time + sec + " sec";
		}
		else
			return "";
	}
    public boolean getIsEndPosition()
    {
    	boolean isEndPos = false;
    	if (!gc.isGameShow)
    		isEndPos = userP.getBoolean("user_options_gui_LastPosition", false);
    	else
    		isEndPos = false;
    	return isEndPos;
    }
    public CharSequence getGameOverMessage()
    {
    	CharSequence mes = c4aM.getString(R.string.cl_gameOver);
		if (gc.cl.p_mate)
				mes = mes + " (" + c4aM.getString(R.string.cl_mate) + ")";
		if (gc.cl.p_stalemate)
			mes = mes + " (" + c4aM.getString(R.string.cl_stealmate) + ")";
		return mes;
    }
    public void setToClipboard(CharSequence text)
    {
    	Toast.makeText(c4aM, c4aM.getString(R.string.menu_info_clipboardCopyPgn), Toast.LENGTH_SHORT).show();
    	ClipboardManager cm = (ClipboardManager)c4aM.getSystemService(Context.CLIPBOARD_SERVICE);
    	cm.setText(text);
    }
    public void getFromClipboard()
    {
    	CharSequence fen = "";
    	CharSequence pgnData = "";
// ERROR	v1.19  	Apr 11, 2012 3:34:08 PM
    	try
    	{
    		Toast.makeText(c4aM, c4aM.getString(R.string.menu_info_clipboardPaste), Toast.LENGTH_SHORT).show();
	    	ClipboardManager cm = (ClipboardManager)c4aM.getSystemService(Context.CLIPBOARD_SERVICE);
	    	pgnData = (String) cm.getText();
    	}
    	catch (ClassCastException e) {return;}
// ERROR	v1.18	Mar 13, 2012 10:42:08
    	if (pgnData == null)
    		return;
//    	Log.i(TAG,"getFromClipboard(), pgnData: \n" + pgnData);
		stopThreads(false);
		CharSequence[] pgnSplit = pgnData.toString().split(" ");
//		Log.i(TAG,"pgnData, pgnSplit.length: " + pgnData + ", " + pgnSplit.length);
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
			getGameData("", "", "", pgnData, false, getIsEndPosition(), 0);
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
		gc.isGameOver = false;
		gc.isGameUpdated = true;
		gc.isPlayerPlayer = false;
		ec.chessEngineAutoRun = false;
		gc.isChess960 = false;
		gc.fen = gc.cl.p_fen;
		SharedPreferences.Editor ed = userP.edit();
		ed.putInt("user_game_chess960Id", 518);
		ed.putBoolean("user_options_gui_FlipBoard", false);
        ed.commit();
        updateGui();
    }
    public Bitmap combineImages(int image1, int image2, int image3) 
	{	// paint over ImageView
    	Bitmap drawBitmap = null;
    	try
    	{
	    	Bitmap image1Bitmap = BitmapFactory.decodeResource(c4aM.getResources(), image1);
	    	Bitmap image2Bitmap = BitmapFactory.decodeResource(c4aM.getResources(), image2);
	    	Bitmap image3Bitmap = null;
	    	if (image3 != 0)
	    		image3Bitmap = BitmapFactory.decodeResource(c4aM.getResources(), image3);
	    	drawBitmap = Bitmap.createBitmap(image1Bitmap.getWidth(), image1Bitmap.getHeight(), Config.ARGB_8888);
			Canvas canvas = new Canvas(drawBitmap);
			canvas.drawBitmap(image1Bitmap, 0, 0, null);
			canvas.drawBitmap(image2Bitmap, 0, 0, null);
			if (image3 != 0)
				canvas.drawBitmap(image3Bitmap, 0, 0, null);
    	}
		catch (NullPointerException e) {e.printStackTrace();}
    	return drawBitmap;
	}
    public Bitmap rotateImage(int image, int rotate) 
	{	// rotate a drawable(resource)
    	Bitmap bmpOriginal = BitmapFactory.decodeResource(c4aM.getResources(), image);
    	Bitmap bmResult = Bitmap.createBitmap(bmpOriginal.getWidth(), bmpOriginal.getHeight(), Bitmap.Config.ARGB_8888);
    	Canvas tempCanvas = new Canvas(bmResult); 
    	tempCanvas.rotate(rotate, bmpOriginal.getWidth()/2, bmpOriginal.getHeight()/2);
    	tempCanvas.drawBitmap(bmpOriginal, 0, 0, null);
    	return bmResult;
	}
    public void playSound(int idx, int loop)
    {
    	if 	(userP.getBoolean("user_options_gui_enableSounds", true))
    		mSoundPool.play(soundsMap.get(idx), 0.2f, 0.2f, 1, loop, 1.0f);
    }
    public CharSequence getGameInfo()
    {
    	String gameInfo = "";
		if 	( 	userP.getBoolean("user_options_gui_usePgnDatabase", true) 
				& fmPrefs.getInt("fm_extern_db_game_count", 1) != 0
				& fmPrefs.getInt("fm_load_location", 1) == 1
			)
		{
			if (fmPrefs.getInt("fm_extern_db_key_id", 0) == 0)
				gameInfo = "" + fmPrefs.getInt("fm_extern_db_game_id", 0) + "(" + fmPrefs.getInt("fm_extern_db_game_count", 1) + ")";
			else
				gameInfo = "" + (fmPrefs.getInt("fm_extern_db_cursor_id", 0) +1) + "(" + fmPrefs.getInt("fm_extern_db_cursor_count", 0) + "), "
							+ fmPrefs.getInt("fm_extern_db_game_id", 0) + "[" + fmPrefs.getInt("fm_extern_db_game_count", 1) + "]";
			gameInfo = gameInfo + "\n" + fmPrefs.getString("fm_extern_db_key_info", "");
		}
		return gameInfo;
    }
    public void setInfoSize(int layoutId, boolean change) 
   	{
//    	Log.i(TAG,"setInfoSize");
	    LayoutParams params = (RelativeLayout.LayoutParams)scrlInfo.getLayoutParams();
	    if (change)
	    {
			int[] rules = params.getRules();
			if (rules[RelativeLayout.ALIGN_PARENT_BOTTOM] == 0)
			{
				params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);	// -1
				layoutId = RelativeLayout.TRUE;
			}
			else
			{
				params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
				layoutId = 0;
			}
	    }
	    else
	    	params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, layoutId);
	    infoExpand = layoutId;
   	}
    public void setInfoMessage(	boolean showToast, boolean updateMoves, CharSequence app, 
    							CharSequence info, CharSequence engine, CharSequence moveNotification)
    {
//    	Log.i(TAG,"showToast, updateMoves, app, info: " + showToast + ", " + updateMoves + ", " + app + ", " + info);
//    	Log.i(TAG,"engine, move(text): " + engine + ", " + move);
//    	Log.i(TAG, "gc.gameStat, infoContent: " + gc.gameStat + ", " + infoContent);
//    	Log.i(TAG, "gc.cl.p_fen  : " + gc.cl.p_fen);
//    	Log.i(TAG, "searchTaskFen: " + searchTaskFen);
    	if (!gc.cl.p_fen.equals(searchTaskFen))
    		engine = "";
    	if (lblInfo == null | gc.cl.p_stat.equals(""))
    		return;
    	if (infoContent == 3 & !updateMoves)
    		return;
    	lblInfo.setBackgroundResource(R.drawable.borderyellow);
    	if (app != null)
    		messageApp = app;
    	if (info != null)
    		messageInfo = info;
    	if (engine != null)
    		messageEngine = engine;
    	if (infoContent == 1 & !messageEngine.equals(""))
    		infoContent = 2;
    	if (moveNotification != null)
    		messageMove = moveNotification;
    	CharSequence message = "";
    	if (!messageInfo.equals(""))
   			message = messageInfo;
    	if (!messageApp.equals(""))
   			message = messageApp;
    	if (gc.isGameLoaded & !viewModeContent.equals(""))
			message = viewModeContent;
    	if (showToast & !messageApp.equals(""))
   			Toast.makeText(c4aM, messageApp, Toast.LENGTH_SHORT).show();
    	switch (infoContent)
		{
		case 2:		// app message & engine message
			if (userP.getBoolean("user_options_enginePlay_EngineMessage", true) & !gc.isGameLoaded)
	    	{
				if (!messageEngine.equals(""))
					message = message + "\n" + messageEngine;
	    	}
			if (!messageMove.equals(""))
	    	{
	    		if (message.equals(""))
	    			message = messageMove;
	    		else
	    		{
	    			if (message.toString().endsWith("\n"))
	    				message = message + "\n" + messageMove;
	    			else
	    				message = message + "\n\n" + messageMove;
	    		}
	    		lblInfo.setBackgroundResource(R.drawable.borderblue);
	    	}
			lblInfo.setText(message);
			break;
		case 3:		// moves
			if (ec.chessEngineProblem)
			{
				Toast.makeText(c4aM, message, Toast.LENGTH_SHORT).show();
				ec.chessEngineProblem = false;
			}
			if (gc.errorMessage.equals(""))
	    		lblInfo.setBackgroundResource(R.drawable.borderyellow);
    		else
	    		lblInfo.setBackgroundResource(R.drawable.borderpink);
			lblInfo.setText(gc.cl.history.createGameNotationFromHistory(600, false, true, true, false, false, true, 2));
			if (lblInfo.getText().toString().equals(" *"))
				return;
			try
			{
				sb.clearSpans();
		        sb.clear();
		        sb.append(lblInfo.getText());
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
		        lblInfo.setText(sb, BufferType.SPANNABLE);
		        scrlInfo.post(new Runnable() 
		        { 
//		        	@Override
		            public void run() 
		            {
		            	try
		    			{
			            	Layout layout = lblInfo.getLayout();
//			            	Log.i(TAG, "layout: " + layout);
			            	if (layout != null)
			            	{
				            	int scrollLine = layout.getLineForOffset(infoMoveStartX);
				            	if (scrollLine > 0)
				            		scrollLine--;
				        	    if (!infoMoveIsSelected)
				        	        scrlInfo.scrollTo(0, layout.getLineTop(scrollLine));
			            	}
//			        	    Log.i(TAG,"scrollLine, infoMoveStartX, infoMoveIsSelected: " +scrollLine + ", " + infoMoveStartX + ", " + infoMoveIsSelected);
			        	    infoMoveIsSelected = false;
		    			}
// ERROR	v1.18	Mar 13, 2012 12:10:36 		            	
		            	catch (NullPointerException e) {e.printStackTrace();}
		            	catch (IndexOutOfBoundsException e) {}
		            } 
		        }); 
			}
			catch (IndexOutOfBoundsException e) {e.printStackTrace();}
			break;
		default:	// app message
			if (gc.isChess960 & gc.cl.p_chess960ID != 518)
	    	{
	    		if (!message.equals(""))
	    			lblInfo.setText(message + "\n[Chess960-ID: " + gc.cl.p_chess960ID + "]");
	    		else
	    			lblInfo.setText("[Chess960-ID: " + gc.cl.p_chess960ID + "]");
	    	}
			else
				lblInfo.setText(message);
			break;
		}
    	gc.cl.p_message = "";
    }
    //	TOUCH, CLICK		TOUCH, CLICK		TOUCH, CLICK		TOUCH, CLICK
    public OnItemClickListener itemClickListener = new OnItemClickListener() 
	{
	    public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
	    {
//	    	Log.i(TAG,"Position clicked: " + position);
//	    	Log.i(TAG,"Clickable: " + gridview.isClickable());
    		gc.isMoveError = false;
			if (ec.chessEngineSearching & !ec.chessEnginePaused)
			{
				stopThreads(false);
				Toast.makeText(c4aM, c4aM.getString(R.string.engine_paused), Toast.LENGTH_SHORT).show();
			}
			else
			{
				if (!gc.isAutoPlay)
					moveAction(position);
			}
	    }
	};
	public boolean onTouch(View view, MotionEvent event)
	{	// Touch Listener
//    	Log.i(TAG, "view.getId(), event.getAction(): " + view.getId() + ", " + event.getAction());
    	boolean changeMultiView = false;
     	if (view.getId() == R.id.lblInfo)
 	    {
	    	if (event.getAction() == MotionEvent.ACTION_DOWN)
	    	{
//	    		Log.i(TAG, "onTouch, ACTION_DOWN");
	    		downRawX = event.getRawX();
	    	    downRawY = event.getRawY();
	    	    upRawX = 0;
	    	    upRawY = 0;
//	    	    Log.i(TAG, "downRawX, displayWidth: " + downRawX + ", " + displayWidth);
	    	    if (downRawX > (displayWidth - scrollingWidth))
	    	    {
	    	    	scrlInfo.setOnTouchListener(null);
	    	    	infoIsScrolling = true;
	    	    }
	    	    else
	    	    {
	    	    	scrlInfo.setOnTouchListener((OnTouchListener) c4aM);
	    	    	infoIsScrolling = false;
	    	    }
//	    	    Log.i(TAG, "onTouch, ACTION_DOWN, expand, infoIsScrolling: " + infoIsScrolling);
	    	}
	    	if (event.getAction() == MotionEvent.ACTION_MOVE)
	    	{
	    		upRawX = event.getRawX();
	    	    upRawY = event.getRawY();
	    	}
	    	if (event.getAction() == MotionEvent.ACTION_UP | event.getAction() == MotionEvent.ACTION_CANCEL)	// info(messages, moves) > set ACTION
	    	{
//	    		Log.i(TAG, "onTouch, ACTION_UP");
	    		if (event.getAction() == MotionEvent.ACTION_UP)
	    		{
		    		upRawX = event.getRawX();
		    	    upRawY = event.getRawY();
	    		}
	    	    float diffX = downRawX - upRawX;
	    	    float diffY = downRawY - upRawY;
	    	    if (diffX < 0) diffX = diffX * -1;
	    	    if (diffY < 0) diffY = diffY * -1;
	    	    if (diffX > dragWidth)
	    	    {
//	    	    	Log.i(TAG, "onTouch, ACTION_UP, infoContent");
	    	    	switch (infoContent)
	    			{
	    			case 1:		// app message
	    			case 2:		// engine message
	    				infoContent = 3;
	    				break;
	    			case 3:		// moves
	    	    		if (userP.getBoolean("user_options_enginePlay_EngineMessage", true))
	    	    			infoContent = 2;
	    	    		else
	    	    			infoContent = 1;
	    				break;
	    			case 9:		// help modus
	    				if (userP.getBoolean("user_options_enginePlay_EngineMessage", true))
	    	    			infoContent = 2;
	    	    		else
	    	    			infoContent = 1;
	    				break;
	    			}
	    	    	changeMultiView = true;
	    	    }
//	    	    Log.i(TAG, "dX, uX, dY, uY, dragWidth, infoContent: " 
//	    	    		+ downRawX + ", " + upRawX + ", " + downRawY + ", " + upRawY + ", " + dragWidth + ", " + infoContent);
	    	    if (!infoIsScrolling)
	    	    {
	    	    	if (diffY > dragWidth | event.getAction() == MotionEvent.ACTION_CANCEL)
	    	    	{
//		    	    	Log.i(TAG, "onTouch, ACTION_UP, expand");
		    	    	setInfoSize(0, true);
		    	    	changeMultiView = true;
	    	    	}
	    	    }
	    	    if (changeMultiView)
	    	    {
	    	    	setInfoMessage(false, true, null, null, null, null);
	    	    	scrlInfo.setOnTouchListener(null);
	    	    	updateCurrentPosition("");
	    	    	return true;
	    	    }
//	    	    Log.i(TAG, "onTouch, infoContent, diffX, diffY: " + infoContent + ", " + diffX + ", " + diffY);
    	    	if (infoContent == 3 & diffX <= touchWidth & diffY <= touchWidth)
    	    	{
//    	    		Log.i(TAG, "onTouch, ACTION_UP, setInfoMoveValues");
    	    		setInfoMoveValuesFromView(view, event);
 	    			if (!gc.isGameLoaded)
 	    			{
     	    			if (ec.chessEnginePaused)
     	    			{
       	    				nextMove(19, getMoveIdxFromInfo());		// set moveIdx
       	    				setInfoMessage(false, true, null, getEnginePausedMessage(), "", null);
     	    			}
     	    			else
     	    			{
     	    				if (ec.chessEnginePlayMod == 4)
	     	           			pauseEnginePlay(0);	// stop analysis
     	    				else
     	    				{
	     	    				if (ec.chessEngineSearching | ec.chessEnginePlayMod < 3)
	     	    				{
	     	    					stopThreads(false);
	     	    					Toast.makeText(c4aM, c4aM.getString(R.string.engine_paused), Toast.LENGTH_SHORT).show();
	     	    				}
	     	    				else
	     	    					Toast.makeText(c4aM, getEngineThinkingMessage(), Toast.LENGTH_SHORT).show();
     	    				}
     	    			}
 	    			}
 	    			else
 	    				nextMove(19, getMoveIdxFromInfo());		// set moveIdx
    	    	}
	    	}
			scrlInfo.setOnTouchListener(null);
			if (event.getAction() == MotionEvent.ACTION_UP)
				downRawX = 0;
			return true;
 	    }
 		if 	(	view.getId() == R.id.btn_edit_board | view.getId() == R.id.btn_pgn_data | view.getId() == R.id.btn_game |
     			view.getId() == R.id.btn_notation | view.getId() == R.id.btn_settings | view.getId() == R.id.btn_menu |
     			
     			view.getId() == R.id.btn_play_start | view.getId() == R.id.btn_play_options | view.getId() == R.id.btn_engines |
     			view.getId() == R.id.btn_analyses_moves_previous | view.getId() == R.id.btn_analyses_moves_next | view.getId() == R.id.btn_moves_cancel |
     			
     			view.getId() == R.id.btn_moves_start | view.getId() == R.id.btn_moves_first | view.getId() == R.id.btn_moves_previous |
     			view.getId() == R.id.btn_moves_next | view.getId() == R.id.btn_moves_last | view.getId() == R.id.btn_turn |
     			
     			view.getId() == R.id.btn_pgn_load | view.getId() == R.id.btn_pgn_save | view.getId() == R.id.btn_pgn_delete |
     			view.getId() == R.id.btn_pgn_download | view.getId() == R.id.btn_cb_copy | view.getId() == R.id.btn_cb_past |
     			view.getId() == R.id.btn_a1 | view.getId() == R.id.btn_a2 | view.getId() == R.id.btn_a3 | view.getId() == R.id.btn_a4 
     		)		
     	{	
    	    if (event.getAction() == MotionEvent.ACTION_DOWN)
	    	{
    	    	downRawX = event.getRawX();
    	    	upRawX = 0;
	    	}
    	    if (event.getAction() == MotionEvent.ACTION_MOVE)
	    	{
    	    	if (downRawX == 0)
    	    	{
    	    		downRawX = event.getRawX();
        	    	upRawX = 0;
    	    	}
	    	}
    	    if (event.getAction() == MotionEvent.ACTION_UP)
	    	{
		    	upRawX = event.getRawX();
		    	float diff = downRawX - upRawX;
				downRawX = 0;
		    	if (diff < -25)
		    	{
		    		setButtonPosition(false);
		    		return true;
		    	}
		    	if (diff >  25)
		    	{
		    		setButtonPosition(true);
		    		return true;
		    	}
//		    	Log.i(TAG, "btnScrollId, downRawX, upRawX, diff: " + btnScrollId + ", " + downRawX + ", " + upRawX + ", " + diff);
	    	}
     	}
		if (event.getAction() == MotionEvent.ACTION_UP)
		{
			if (view.getId() == R.id.btn_play_start & !btn_play_start_enabled) return true;
			if (view.getId() == R.id.btn_analyses_moves_previous & !btn_analyses_moves_previous_enabled) return true;
			if (view.getId() == R.id.btn_analyses_moves_next & !btn_analyses_moves_next_enabled) return true;
			
			if (view.getId() == R.id.btn_moves_start & !btn_moves_start_enabled) return true;
			if (view.getId() == R.id.btn_moves_first & !btn_moves_first_enabled) return true;
			if (view.getId() == R.id.btn_moves_previous & !btn_moves_previous_enabled) return true;
			if (view.getId() == R.id.btn_moves_next & !btn_moves_next_enabled) return true;
			if (view.getId() == R.id.btn_moves_last & !btn_moves_last_enabled) return true;
			myClickHandlerEvents(view);
		}
		return true;
	}
    public void setInfoMoveValuesFromView(View view, MotionEvent event) 													
    {	// set the values of a selected move(lblInfo)
    	CharSequence moves = lblInfo.getText();
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
//	        Log.i(TAG, "offset, moves.length(): " + offset + ", " + moves.length());
	        if (moves.length() <= 2 | offset < 1)
	        	return;
	        if (moves.charAt(offset) == ' ')
	        	offset--;
	        infoMoveStartX = offset;
//	        Log.i(TAG, "offset: " + offset + " >" + moves.charAt(offset) + "<");
	        try 
	        {
	        	boolean isDot = false;
	        	if (Character.isDigit(moves.charAt(infoMoveStartX)) | moves.charAt(infoMoveStartX) == '.')
		        {
		        	for (int i = infoMoveStartX; i < moves.length(); i++) 
			    	{
//		        		Log.i(TAG, "isDigit(): " + " >" + moves.charAt(i) + "<");
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
//		        Log.i(TAG, "infoMoveStartX, infoMoveEndX: " + infoMoveStartX + ", " + infoMoveEndX);
	        }
	        catch (StringIndexOutOfBoundsException e) 
	        {
	        	e.printStackTrace();
	        }
//	        Log.i(TAG, "index, infoMoveStartX, infoMoveEndX, move: " + offset + ", " + infoMoveStartX + ", " + infoMoveEndX + ", " + selectedMove);
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
    {	// get moveIdx from lblInfo
    	int moveIdx = 0;
    	boolean newToken = false;
    	CharSequence moves = lblInfo.getText();
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
//    	Log.i(TAG, "getMoveIdxFromInfo, infoMoveStartX, moveIdx: " + infoMoveStartX + ", " + moveIdx);
    	return moveIdx;
    }
    public void myClickHandler(View view) 													
    {	// ClickHandler (Events control)
//    	Log.i(TAG, "myClickHandler, view.getId(): " + view.getId());
    	myClickHandlerEvents(view);
	}
    public void myClickHandlerEvents(View view)													
    {	// ClickHandler (Events)
//    	Log.i(TAG, "myClickHandlerEvents, view.getId(): " + view.getId());
    	SharedPreferences.Editor ed = userP.edit();
    	ed.putBoolean("user_options_gui_FlipBoard", false);
        ed.commit();
    	gc.move = "";
		switch (view.getId())
		{
		case R.id.lblPlayerNameA:		// load random game
			stopThreads(false);
			startFileManager(LOAD_GAME_REQUEST_CODE, 0, 7);	
			break;
		case R.id.lblPlayerNameB:		// query for games in database
			if (fmPrefs.getInt("fm_location", 1) == 1 & userP.getBoolean("user_options_gui_usePgnDatabase", true))
			{
				stopThreads(false);
				if (!fmPrefs.getString("fm_query_white", "").equals(""))
					c4aShowDialog(QUERY_DIALOG);
				else
					startFileManager(LOAD_GAME_REQUEST_CODE, 0, 1);	
			}
			break;
		case R.id.lblPlayerEloA:		// load first game
			stopThreads(false);
			startFileManager(LOAD_GAME_REQUEST_CODE, 0, 1);	
			break;
		case R.id.lblPlayerEloB:		// load last game
			stopThreads(false);
			startFileManager(LOAD_GAME_REQUEST_CODE, 0, 9);	
			break;
		case R.id.lblPlayerTimeA:		// load previous game
			if (!gc.pgnStat.equals("F"))
			{
				stopThreads(false);
				startFileManager(LOAD_GAME_PREVIOUS_CODE, 0, 8);
			}
            break;
		case R.id.lblPlayerTimeB:		// load next game
			if (!gc.pgnStat.equals("L"))
			{
				stopThreads(false);
				startFileManager(LOAD_GAME_REQUEST_CODE, 0, 0);	
			}
            break;
        // btn_a1		control (green)
		case R.id.btn_edit_board:
			stopThreads(false);
			editChessBoardIntent.putExtra("currentFen", gc.fen);
        	editChessBoardIntent.putExtra("gridViewSize", gridViewSize);
        	editChessBoardIntent.putExtra("fieldSize", getChessFieldSize());
        	c4aM.startActivityForResult(editChessBoardIntent, EDIT_CHESSBOARD_REQUEST_CODE);
			break;
		case R.id.btn_pgn_data:		
			startGameData();
			break;
		case R.id.btn_game:		
			c4aM.openContextMenu(btn_game); 
			break;
		case R.id.btn_notation:		
			startNotation(3);
			break;
		case R.id.btn_settings:		
			c4aM.openContextMenu(btn_settings);
			break;
		case R.id.btn_menu:		
			c4aM.openContextMenu(btn_menu);
			break;
		// btn_a2		play (yellow)
		case R.id.btn_play_start:	// btn_play_stop
			if (isTimeOut)
				return;
			gc.isGameLoaded = false;
			stopAutoPlay();
			if (ec.chessEngineInit)
        	{
        		initInfoArrays(false);
        		if (ec.chessEnginePaused & infoContent == 3)
        			Toast.makeText(c4aM, c4aM.getString(R.string.engineProgressDialog), Toast.LENGTH_SHORT).show();
        		else
        			setInfoMessage(false, true, "", c4aM.getString(R.string.engineProgressDialog), "", null);
        		ec.chessEngineInit = false;
        		ec.setPlaySettings(userP);	
        		ec.setStartPlay(gc.getValueFromFen(gc.fen, 2));
        		ec.initEngines();
        		if (ec.chessEnginePaused)
            	{
        			ec.chessEnginePaused = false;
	        		ec.lastChessEnginePaused = false;
            	}
        	}
        	else
        	{
        		ec.chessEngineStopSearch = true;
	        	if (ec.getEngine().getIsProcessAlive() & ec.getEngine().getIsReady())
	        	{
//	        		Log.i(TAG, "btn_play_start, getIsProcessAlive(): true");
	        		if (ec.chessEnginePaused & infoContent == 3)
	        			Toast.makeText(c4aM, c4aM.getString(R.string.engineProgressDialog), Toast.LENGTH_SHORT).show();
        			pauseEnginePlay(0);	// stop
	        	}
	        	else
	        	{
//	        		Log.i(TAG, "btn_play_start, getIsProcessAlive(): false");
	        		cancelSearchTask();
	        		if (ec.getEngine().engineNumber == 1)
	        			ed.putString("user_play_engine_1", "");
	        		if (ec.getEngine().engineNumber == 2)
	        			ed.putString("user_play_engine_2", "");
	    			ed.commit();
	    			ec.getEngine().engineServiceName = "";
	        		ec.setPlaySettings(userP);
					ec.chessEngineInit = true;
					ec.chessEnginePaused = true;
					ec.chessEngineAnalysisStat = 0;
					c4aM.uic.c4aShowDialog(NO_CHESS_ENGINE_DIALOG);		// !!!
	        	}
        	}
			break;
		case R.id.btn_play_options:		
			stopThreads(false);
			c4aM.startActivityForResult(optionsPlayIntent, OPTIONS_PLAY_REQUEST_CODE);
			break;
		case R.id.btn_engines:
//			stopThreads(true);
			c4aM.openContextMenu(btn_engines);
			break;
		case R.id.btn_analyses_moves_previous:
			stopThreads(false);
			gc.isGameOver = false;
        	nextMove(1, 0);	// move back
			break;
		case R.id.btn_analyses_moves_next:
			stopThreads(false);
			nextMove(2, 0);	// next move
			break;
		case R.id.btn_moves_cancel:	
			startMoveDelete();	
			break;
		// btn_a3		PGN-Viewer (blue)
		case R.id.btn_moves_start:	// btn_moves_stop
			startStopAutoPlay();		// start/stop auto play
			break;
		case R.id.btn_moves_first:
			if (!gc.isAutoPlay)
				stopThreads(false);
			gc.isGameOver = false;
        	nextMove(3, 0);	// start position
			break;
		case R.id.btn_moves_previous:
			if (!gc.isAutoPlay)
				stopThreads(false);
			gc.isGameOver = false;
        	nextMove(1, 0);	// move back
			break;
		case R.id.btn_moves_next:
			if (!gc.isAutoPlay)
				stopThreads(false);
			nextMove(2, 0);	// next move
			break;
		case R.id.btn_moves_last:
			if (!gc.isAutoPlay)
				stopThreads(false);
			nextMove(4, 0);
			break;
		case R.id.btn_turn:		
			startTurnBoard();
			break;
		// btn_a4		PGN-Manager (red)
		case R.id.btn_pgn_load:		
			startFileManager(LOAD_GAME_REQUEST_CODE, 1, 0);
			break;
		case R.id.btn_pgn_save:		
			startSaveGame(1);
			break;
		case R.id.btn_pgn_delete:		
			startFileManager(DELETE_GAME_REQUEST_CODE, 1, 0);
			break;
		case R.id.btn_pgn_download:		
			startPgnDownload();
			break;
		case R.id.btn_cb_copy:		
			setToClipboard(gc.cl.history.createPgnFromHistory(1));
			break;
		case R.id.btn_cb_past:
			stopThreads(false);
			initChessClock();
			getFromClipboard();
			break;
		}
    }
    //	GUI		GUI		GUI		GUI		GUI		GUI		GUI		GUI		GUI		GUI	
    public void startGui()																		
    {	// init application (GUI)
    	if (!userP.getBoolean("user_options_gui_StatusBar", false))
    		c4aM.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        else
        	c4aM.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN, WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
//    	Log.i(TAG, "resourceName: " + c4aM.getResources().getResourceName(0x7f020006));
    	c4aM.setContentView(R.layout.main);
        mainView = (RelativeLayout) c4aM.findViewById(R.id.mainView);
        // lblMessage only for layout size ("LARGE" | "MEDIUM")
        lblMessage = (TextView) c4aM.findViewById(R.id.lblMessage);
//        Log.i(TAG, "Layout: " + lblMessage.getText());
// ERROR	v1.5  11.10.2011 23:54:34
        if (lblMessage.getText().equals("LARGE") | lblMessage.getText().equals("MEDIUM"))		
        	gc.isLargeScreen = true;
        else
        	gc.isLargeScreen = false;
        if (userP.getInt("user", 0) == 0)
        {	// initializing prefs
        	gc.initPrefs = true;
        }
        
        lblMessage.setVisibility(TextView.INVISIBLE);
        chessBoard = new ChessBoard(c4aM, gc.fen, getChessFieldSize(), getImageSet());
        
        lblPlayerNameA = (TextView) c4aM.findViewById(R.id.lblPlayerNameA);
        lblPlayerEloA = (TextView) c4aM.findViewById(R.id.lblPlayerEloA);
        lblPlayerTimeA = (TextView) c4aM.findViewById(R.id.lblPlayerTimeA);
        lblPlayerNameB = (TextView) c4aM.findViewById(R.id.lblPlayerNameB);
        lblPlayerEloB = (TextView) c4aM.findViewById(R.id.lblPlayerEloB);
        lblPlayerTimeB = (TextView) c4aM.findViewById(R.id.lblPlayerTimeB);
        scrlInfo = (ScrollView) c4aM.findViewById(R.id.scrlInfo);
        scrlInfo.setOnTouchListener((OnTouchListener) c4aM);
        scrlInfo.setVerticalFadingEdgeEnabled(false);
        lblInfo = (TextView) c4aM.findViewById(R.id.lblInfo);
        lblInfo.setOnTouchListener((OnTouchListener) c4aM);
        setInfoSize(infoExpand, false);
        btn_a1 = (RelativeLayout ) c4aM.findViewById(R.id.btn_a1);
        btn_a2 = (RelativeLayout ) c4aM.findViewById(R.id.btn_a2);
        btn_a3 = (RelativeLayout ) c4aM.findViewById(R.id.btn_a3);
        btn_a4 = (RelativeLayout ) c4aM.findViewById(R.id.btn_a4);
        btn_a1.setOnTouchListener((OnTouchListener) c4aM);
        btn_a2.setOnTouchListener((OnTouchListener) c4aM);
        btn_a3.setOnTouchListener((OnTouchListener) c4aM);
        btn_a4.setOnTouchListener((OnTouchListener) c4aM);
        scroll_left = (ImageView) c4aM.findViewById(R.id.scroll_left);
        scroll_right = (ImageView) c4aM.findViewById(R.id.scroll_right);
        // btn_a1
        btn_edit_board = (ImageView) c4aM.findViewById(R.id.btn_edit_board);
        btn_edit_board.setImageBitmap(combineImages(R.drawable.btn_b1, R.drawable.btn_edit_board, 0));
        btn_edit_board.setOnTouchListener((OnTouchListener) c4aM);
        btn_pgn_data = (ImageView) c4aM.findViewById(R.id.btn_pgn_data);
        btn_pgn_data.setImageBitmap(combineImages(R.drawable.btn_b1, R.drawable.btn_pgn_data, 0));
        btn_pgn_data.setOnTouchListener((OnTouchListener) c4aM);
        btn_game = (ImageView) c4aM.findViewById(R.id.btn_game);
        btn_game.setImageBitmap(combineImages(R.drawable.btn_b1, R.drawable.btn_game, 0));
        c4aM.registerForContextMenu(btn_game);
        btn_game.setOnTouchListener((OnTouchListener) c4aM);
        btn_notation = (ImageView) c4aM.findViewById(R.id.btn_notation);
        btn_notation.setImageBitmap(combineImages(R.drawable.btn_b1, R.drawable.btn_notation, 0));
        btn_notation.setOnTouchListener((OnTouchListener) c4aM);
        btn_settings = (ImageView) c4aM.findViewById(R.id.btn_settings);
        btn_settings.setImageBitmap(combineImages(R.drawable.btn_b1, R.drawable.btn_settings, 0));
        c4aM.registerForContextMenu(btn_settings);
        btn_settings.setOnTouchListener((OnTouchListener) c4aM);
        btn_menu = (ImageView) c4aM.findViewById(R.id.btn_menu);
        btn_menu.setImageBitmap(combineImages(R.drawable.btn_b1, R.drawable.btn_menu, 0));
        c4aM.registerForContextMenu(btn_menu);
        btn_menu.setOnTouchListener((OnTouchListener) c4aM);
        // btn_a2
        btn_play_start = (ImageView) c4aM.findViewById(R.id.btn_play_start);
        btn_play_start.setImageBitmap(combineImages(R.drawable.btn_b2, R.drawable.btn_play_start, 0));
        c4aM.registerForContextMenu(btn_play_start);
        btn_play_start.setOnTouchListener((OnTouchListener) c4aM);
        btn_play_options = (ImageView) c4aM.findViewById(R.id.btn_play_options);
        btn_play_options.setImageBitmap(combineImages(R.drawable.btn_b2, R.drawable.btn_play_options, 0));
        btn_play_options.setOnTouchListener((OnTouchListener) c4aM);
        btn_engines = (ImageView) c4aM.findViewById(R.id.btn_engines);
        btn_engines.setImageBitmap(combineImages(R.drawable.btn_b2, R.drawable.btn_engines, 0));
        c4aM.registerForContextMenu(btn_engines);
        btn_engines.setOnTouchListener((OnTouchListener) c4aM);
        btn_analyses_moves_previous = (ImageView) c4aM.findViewById(R.id.btn_analyses_moves_previous);
        btn_analyses_moves_previous.setImageBitmap(combineImages(R.drawable.btn_b2, R.drawable.btn_moves_previous, 0));
        btn_analyses_moves_previous.setOnTouchListener((OnTouchListener) c4aM);
        btn_analyses_moves_next = (ImageView) c4aM.findViewById(R.id.btn_analyses_moves_next);
        btn_analyses_moves_next.setImageBitmap(combineImages(R.drawable.btn_b2, R.drawable.btn_moves_next, 0));
        btn_analyses_moves_next.setOnTouchListener((OnTouchListener) c4aM);
        btn_moves_cancel = (ImageView) c4aM.findViewById(R.id.btn_moves_cancel);
        btn_moves_cancel.setImageBitmap(combineImages(R.drawable.btn_b2, R.drawable.btn_moves_cancel, 0));
        btn_moves_cancel.setOnTouchListener((OnTouchListener) c4aM);
        // btn_a3
        btn_moves_start = (ImageView) c4aM.findViewById(R.id.btn_moves_start);
        btn_moves_start.setImageBitmap(combineImages(R.drawable.btn_b3, R.drawable.btn_moves_start, 0));
        btn_moves_start.setOnTouchListener((OnTouchListener) c4aM);
        btn_moves_first = (ImageView) c4aM.findViewById(R.id.btn_moves_first);
        btn_moves_first.setImageBitmap(combineImages(R.drawable.btn_b3, R.drawable.btn_moves_first, 0));
        btn_moves_first.setOnTouchListener((OnTouchListener) c4aM);
        btn_moves_previous = (ImageView) c4aM.findViewById(R.id.btn_moves_previous);
        btn_moves_previous.setImageBitmap(combineImages(R.drawable.btn_b3, R.drawable.btn_moves_previous, 0));
        btn_moves_previous.setOnTouchListener((OnTouchListener) c4aM);
        btn_moves_next = (ImageView) c4aM.findViewById(R.id.btn_moves_next);
        btn_moves_next.setImageBitmap(combineImages(R.drawable.btn_b3, R.drawable.btn_moves_next, 0));
        btn_moves_next.setOnTouchListener((OnTouchListener) c4aM);
        btn_moves_last = (ImageView) c4aM.findViewById(R.id.btn_moves_last);
        btn_moves_last.setImageBitmap(combineImages(R.drawable.btn_b3, R.drawable.btn_moves_last, 0));
        btn_moves_last.setOnTouchListener((OnTouchListener) c4aM);
        btn_turn = (ImageView) c4aM.findViewById(R.id.btn_turn);
        btn_turn.setImageBitmap(combineImages(R.drawable.btn_b3, R.drawable.btn_turn_wb, 0));
        btn_turn.setOnTouchListener((OnTouchListener) c4aM);
        // btn_a4
        btn_pgn_load = (ImageView) c4aM.findViewById(R.id.btn_pgn_load);
        btn_pgn_load.setImageBitmap(combineImages(R.drawable.btn_b4, R.drawable.btn_pgn_load, 0));
        btn_pgn_load.setOnTouchListener((OnTouchListener) c4aM);
        btn_pgn_save = (ImageView) c4aM.findViewById(R.id.btn_pgn_save);
        btn_pgn_save.setImageBitmap(combineImages(R.drawable.btn_b4, R.drawable.btn_pgn_save, 0));
        btn_pgn_save.setOnTouchListener((OnTouchListener) c4aM);
        btn_pgn_delete = (ImageView) c4aM.findViewById(R.id.btn_pgn_delete);
        btn_pgn_delete.setImageBitmap(combineImages(R.drawable.btn_b4, R.drawable.btn_pgn_delete, 0));
        btn_pgn_delete.setOnTouchListener((OnTouchListener) c4aM);
        btn_pgn_download = (ImageView) c4aM.findViewById(R.id.btn_pgn_download);
        btn_pgn_download.setImageBitmap(combineImages(R.drawable.btn_b4, R.drawable.btn_pgn_download, 0));
        btn_pgn_download.setOnTouchListener((OnTouchListener) c4aM);
        btn_cb_copy = (ImageView) c4aM.findViewById(R.id.btn_cb_copy);
        btn_cb_copy.setImageBitmap(combineImages(R.drawable.btn_b4, R.drawable.btn_cb_copy, 0));
        btn_cb_copy.setOnTouchListener((OnTouchListener) c4aM);
        btn_cb_past = (ImageView) c4aM.findViewById(R.id.btn_cb_past);
        btn_cb_past.setImageBitmap(combineImages(R.drawable.btn_b4, R.drawable.btn_cb_past, 0));
        btn_cb_past.setOnTouchListener((OnTouchListener) c4aM);
        
        gridview = (GridView) c4aM.findViewById(R.id.gridview);
        gridview.setAdapter(chessBoard);
        gridview.setClickable(true);
        gridview.setOnItemClickListener(itemClickListener);
		gridview.invalidate();
        playEngineIntent = new Intent(c4aM, PlayEngineSettings.class);
        fileManagerIntent = new Intent(c4aM, PgnFileManager.class);
        gameDataIntent = new Intent(c4aM, ChessGameData.class);
        notationIntent = new Intent(c4aM, ChessNotation.class);
        moveTextIntent = new Intent(c4aM, ChessMoveText.class);
        optionsChessBoardIntent = new Intent(c4aM, OptionsChessBoard.class);
        optionsGuiIntent = new Intent(c4aM, OptionsGUI.class);
        optionsTimeControlIntent = new Intent(c4aM, OptionsTimeControl.class);
        optionsPlayIntent = new Intent(c4aM, OptionsPlay.class);
        optionsEnginePlayIntent = new Intent(c4aM, OptionsEnginePlay.class);
        editChessBoardIntent = new Intent(c4aM, EditChessBoard.class);
        pdbToPgnIntent = new Intent(c4aM, PdbToPgnActivity.class);
    }
    public void updateFullscreenStatus(boolean bUseFullscreen)
{	// full screen on/off
    if(bUseFullscreen)
    {
        c4aM.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        c4aM.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
    }
    else
    {
        c4aM.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        c4aM.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
    mainView.requestLayout();
}
    public char getFieldColor(CharSequence field, boolean boardTurn)									
    {return chessBoard.getFieldColor(field, boardTurn);}
    public void getGameData(CharSequence fileBase, CharSequence filePath, CharSequence fileName, 
    			CharSequence startPgn, boolean withMoveHistory, boolean isEndPos, int moveIdx)					
    {	// gameData to c4aService and updateGui
//    	Log.i(TAG, "getGameData()");
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
    public void getNewChessPosition(CharSequence chess960Id)											
    {	// new Game, new ChessPosition  and updateGui
    	gc.cl.newPosition(chess960Id, "", gc.cl.history.getGameTagValue("Event"), gc.cl.history.getGameTagValue("Site"), "", 
    			gc.cl.history.getGameTagValue("Round"), gc.cl.history.getGameTagValue("White"), gc.cl.history.getGameTagValue("Black"));
    	if (gc.cl.p_stat.equals("1"))
	  	{
	  		if (gc.cl.history.getGameTagValue("Event").equals(""))
	  			gc.cl.history.setGameTag("Event", c4aM.getString(R.string.app_name));
	  		if (gc.cl.p_chess960ID != 518)
	  		{
	  			SharedPreferences.Editor ed = userP.edit();
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
    public void setChess960IdPref(int chess960Id)											
    {	// if isLastGame set old game preferences
    	SharedPreferences.Editor ed = userP.edit();
		ed.putInt("user_game_chess960Id", chess960Id);
		ed.commit();
    }
    public void setPlayModPrefs(int playMod)											
    {	
    	SharedPreferences.Editor ed = userP.edit();
		ed.putInt("user_play_playMod", playMod);
		ed.commit();
    }
    public void nextMove(int moveDirection, int moveIdx)														
    {	// next move(History) and updateGui
//    	Log.i(TAG, "nextMove, moveDirection, moveIdx: " + moveDirection + ", " + moveIdx);
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
//			Log.i(TAG, "gc.cl.p_stat, gc.isAutoPlay, gc.cl.p_gameOver, gc.cl.p_gameEnd: " + gc.cl.p_stat + ", " + gc.isAutoPlay + ", " + gc.cl.p_gameOver + ", " + gc.cl.p_gameEnd);
	        if (gc.cl.p_stat.equals("1"))
	        	updateGui();
	        if (gc.isAutoPlay & !gc.isGameShow)
	        {
	        	gc.setGameOver("end");
	        	if (gc.isGameOver)
	        		stopAutoPlay();
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
//        Log.i(TAG, "deleteMoves(), continueAnalysis: " + continueAnalysis);
    	if (continueAnalysis)
		{
    		continueAnalysis = false;
//	        	Log.i(TAG, "deleteMoves(), continueAnalysis: " + continueAnalysis);
    		pauseEnginePlay(0);			// start/stop engine play
		}
        else
		{
    		messageInfo = "";
    		setInfoMessage(false, true, null, getEnginePausedMessage(), null, null);
    	}	
    }
    public void startEdit(boolean isNewGame)																		
    {	// start Edit (gameStat = 1) and new chess position
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
			getNewChessPosition(Integer.toString(userP.getInt("user_game_chess960Id", 518)));
    }
    public void startPlay(boolean isNewGame)																		
    {	// start play (gameStat = 3) and updateGui
//    	Log.i(TAG, "startPlay, isNewGame: " + isNewGame);
    	
//		if (userP.getInt("user_play_playMod", 1) == 4)
//    		mate = 3;
		
    	messageEngine 		= "";
    	messageMove 		= "";
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
    			infoContent = 1;
    			setInfoMessage(true, true, "", gc.errorMessage, null, null);
    			return;
    		}
    	}
    	analysisMessage = "";
    	if (isNewGame | (!gc.cl.p_mate & !gc.cl.p_stalemate))	// mate, steal mate?
    	{
	   		gc.isGameOver = false;
	   		gc.isGameUpdated = true;
			ec.chessEnginePlayMod = userP.getInt("user_play_playMod", 1);
			if (ec.chessEnginePlayMod == 2)
				gc.isBoardTurn = true;
			else
				gc.isBoardTurn = false;
			ec.setPlaySettings(userP);
			if (isNewGame)
				getNewChessPosition(Integer.toString(userP.getInt("user_game_chess960Id", 518)));
			if (gc.cl.p_chess960ID == 518)
				gc.isChess960 = false;
			else
				gc.isChess960 = true;
			gc.startFen = gc.cl.history.getStartFen();
			engineMes = ""; initInfoArrays(false);
			setInfoMessage(false, true, "", c4aM.getString(R.string.engineProgressDialog), "", null);
			initChessClock();
			startEnginePlay();
    	}
    	else
    	{
    		setInfoMessage(true, true, getGameOverMessage(), null, null, null);
    	}
    }
    public void startStopAutoPlay()																
    {	// start|stop autoPlay and updateGui 
    	if (!gc.isGameShow)
    	{
	    	if (!gc.isAutoPlay)
	    	{
	    		gc.isAutoPlay = true;
	    		handlerAutoPlay.removeCallbacks(mUpdateAutoplay);
	    		handlerAutoPlay.postDelayed(mUpdateAutoplay, 100);
	    		updateCurrentPosition("");
	    	}
	    	else
	    		stopAutoPlay();
    	}
    }
    public void stopThreads(boolean shutDown)
    {	// stop handler, threads, asyncTasks
    	stopGameShow();
		stopTimeHandler(shutDown);
    }
    public void stopTimeHandler(boolean shutDown)
    {	// stop handler, thread, task and updateGui
    	stopChessClock();
    	setPauseEnginePlay(shutDown);
    	stopAutoPlay();
     }
    public void stopAutoPlay()
    {	// stop Auto Play and updateGui 
		gc.isAutoPlay = false;
		handlerAutoPlay.removeCallbacks(mUpdateAutoplay);
		updateCurrentPosition("");
    }
    public void initChessClock()																	
    {
    	int timeWhite = 300000;
    	int timeBlack = 300000;
    	int movesToGo = 0;
    	int bonusWhite = 0;
    	int bonusBlack = 0;
    	int timeControl = userP.getInt("user_options_timeControl", 1);
    	switch (ec.chessEnginePlayMod)
        {
	        case 1:
	        	switch (timeControl)
	            {
	            case 1:
		        	timeWhite = userP.getInt("user_time_player_clock", 300000);
		        	timeBlack = userP.getInt("user_time_engine_clock", 60000);
		        	bonusWhite = userP.getInt("user_bonus_player_clock", 2000);
		        	bonusBlack = userP.getInt("user_bonus_engine_clock", 2000);
		        	break;
	            case 2:
	            	timeWhite = userP.getInt("user_time_player_move", 40000);
		        	timeBlack = userP.getInt("user_time_engine_move", 5000);
	            	break;
	            case 3:
	            	timeWhite = userP.getInt("user_time_player_sand", 300000);
		        	timeBlack = userP.getInt("user_time_engine_sand", 10000);
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
		        	timeWhite = userP.getInt("user_time_engine_clock", 60000);
		        	timeBlack = userP.getInt("user_time_player_clock", 300000);
		        	bonusWhite = userP.getInt("user_bonus_engine_clock", 2000);
		        	bonusBlack = userP.getInt("user_bonus_player_clock", 2000);
		        	break;
	            case 2:
	            	timeWhite = userP.getInt("user_time_engine_move", 5000);
		        	timeBlack = userP.getInt("user_time_player_move", 40000);
	            	break;
	            case 3:
	            	timeWhite = userP.getInt("user_time_engine_sand", 10000);
		        	timeBlack = userP.getInt("user_time_player_sand", 300000);
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
		        	timeWhite = userP.getInt("user_time_engine_clock", 60000);
		        	timeBlack = userP.getInt("user_time_engine_clock", 60000);
		        	bonusWhite = userP.getInt("user_bonus_engine_clock", 2000);
		        	bonusBlack = userP.getInt("user_bonus_engine_clock", 2000);
		        	break;
	            case 2:
	            	timeWhite = userP.getInt("user_time_engine_move", 5000);
		        	timeBlack = userP.getInt("user_time_engine_move", 5000);
	            	break;
	            case 3:
	            	timeWhite = userP.getInt("user_time_engine_sand", 10000);
		        	timeBlack = userP.getInt("user_time_engine_sand", 10000);
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
        }
    	tc.initChessClock(timeControl, timeWhite, timeBlack, movesToGo, bonusWhite, bonusBlack);
    }
    public void stopChessClock()																	
    {	
    	handlerChessClock.removeCallbacks(mUpdateChessClock);
    	tc.stopChessClock(System.currentTimeMillis());
    }
    public void startChessClock()																	
    {	
//    	if (gc.gameStat == 3)
		{	// no chessClock running if analysis!

			if (gc.cl.p_color.equals("w"))
				tc.startChessClock(true, System.currentTimeMillis());
			else
				tc.startChessClock(false, System.currentTimeMillis());
			handlerChessClock.removeCallbacks(mUpdateChessClock);
	    	handlerChessClock.postDelayed(mUpdateChessClock, 100);
		}
    }
    public void stopGameShow()																	
    {	// stop gameShow and updateGui 
    	if (gc.isGameShow)
    	{
    		analysisEndTime =  System.currentTimeMillis();
    		String message = "END, game-ID: " + fmPrefs.getInt("fm_extern_db_game_id", 1) + "\n" + getAnalizesTime();
    		writePgnErrorFile(message);
    	}
		gc.isGameShow = false;
		updateGui();
		handlerGameShow.removeCallbacks(mUpdateGameShow);
    }
    public void startMoveDelete()																
    {	// delete last move and updateGui
    	if (!ec.chessEnginePaused)
    	{
//    		Toast.makeText(c4aM, c4aM.getString(R.string.engine_paused), Toast.LENGTH_SHORT).show();
    		if (ec.chessEnginePlayMod == 4)	// analysis running
    		{
    			if (!ec.chessEngineStopSearch)
    			{
	    			ec.chessEngineStopSearch = true;
	    			pauseEnginePlay(2);	// move & analysis
    			}
    		}
    		else
    		{
    			Toast.makeText(c4aM, c4aM.getString(R.string.engine_paused), Toast.LENGTH_SHORT).show();
    			pauseEnginePlay(0);
    		}
    	}
    	else
    		deleteMoves(true);
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
    public void startVariation()																
    {	// start variation(option menu) and updateGui
        ArrayList<CharSequence> variList = gc.cl.history.getVariationsFromMoveHistory();
        gc.variationsList.clear();
    	if (variList.size() > 0)
    	{
    		for (int i = 0; i < variList.size(); i++)
            {
        		gc.variationsList.add(variList.get(i));
            }
    	}
    	updateCurrentPosition("");
    }
    public void startUserManual()																
    {
    	Intent ium = new Intent(Intent.ACTION_VIEW);
    	ium.setData(Uri.parse("http://c4akarl.blogspot.co.at/p/user-manual_22.html"));
		c4aM.startActivityForResult(ium, RATE_REQUEST_CODE);
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
    public void updateCurrentPosition(CharSequence message)	
    {	// get current position from c4aService and updateGui
    	if (lblInfo == null | message == null | gc.cl.p_stat.equals(""))
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
		}
        catch (IndexOutOfBoundsException e) {e.printStackTrace();}
        catch (NullPointerException e) 		{e.printStackTrace();}
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
    public void moveAction(int position) 											
	{	// move action (chessboard)
    	if (ec.chessEnginePlayMod == 5)
    	{
    		SharedPreferences.Editor ed = userP.edit();
	    	ed.putBoolean("user_options_gui_FlipBoard", true);
			ed.commit();
    	}
    	if (gc.cl.p_mate | gc.cl.p_stalemate)	// mate, steal mate?
    	{
    		setInfoMessage(true, true, getGameOverMessage(), null, null, null);
    		return;
    	}
		if (!gc.isGameOver & !gc.cl.p_variationEnd)	// move actions only on: edit/play
		{
			CharSequence field = "";
			try 
			{
				field = chessBoard.getChessField(position, gc.isBoardTurn);
				if (!gc.move.equals(field))
					gc.move = gc.move.toString() + field;
				if (gc.move.length() >= 4)
				{
					if (gc.move.subSequence (0, 2).equals(gc.move.subSequence (2, 4)))
						gc.move = gc.move.subSequence (2, gc.move.length());
				}
//				Log.i(TAG, "gc.move: " + gc.move);
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
		        		gc.isGameUpdated = false;
		        		gc.move = "";
		        	}
		        	if (!gc.isGameLoaded & !(ec.chessEnginePlayMod == 5 | ec.chessEnginePlayMod == 6))
		        	{
		        		gc.setGameOver(gc.cl.history.getGameTagValue("Result"));
			        	if (gc.cl.p_stat.equals("1") & !gc.isGameOver & !gc.cl.p_variationEnd)
			        		ec.chessEngineSearching = true;
		        	}
		        	updateGui();
		        	if 	(		ec.chessEngineSearching & gc.cl.p_stat.equals("1")
		        			& 	!gc.isGameOver & !gc.cl.p_variationEnd
		        		)
		        	{
		    			if (!ec.chessEnginePaused & ec.chessEnginePlayMod == 4)
		    				pauseEnginePlay(0);			// start/stop engine play
		    			else
		    				chessEngineBestMove(gc.cl.p_fen, "");
		        	}
		        }
		    } 
			catch (NullPointerException e) {e.printStackTrace();}
//			Log.i(TAG, "stat, gc.move, move, hasMoves: " + gc.cl.p_stat + ", " + gc.move + ", " + gc.cl.p_move + ", " + gc.cl.p_hasPossibleMoves);
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
		if (gc.isGameLoaded)
		{
			if (!gc.pgnStat.equals("-"))
			{
				lblPlayerTimeA.setText(" <<<");
				lblPlayerTimeB.setText(" >>>");
				if (gc.pgnStat.equals("F"))		// first game
					lblPlayerTimeA.setText("");
				if (gc.pgnStat.equals("L"))		// last game
					lblPlayerTimeB.setText("");
			}
			else
			{
				lblPlayerTimeA.setText("");
				lblPlayerTimeB.setText("");
			}
		}
		else
		{
			if (ec.chessEnginePlayMod == 5 | ec.chessEnginePlayMod == 6)
			{
				lblPlayerTimeA.setText("");
				lblPlayerTimeB.setText("");
			}
		}
    }
    public void updatePosibleMoves()
    {	// NEW
//    	Log.i(TAG, "updatePosibleMoves, stat, size: " + gc.cl.p_stat + ", " + gc.cl.p_possibleMoveList.size());
		setDrawValues(-1, 0, true);
    	if (gc.cl.p_stat.equals("0"))					
        {
    		for (int i = 0; i < gc.cl.p_possibleMoveList.size(); i++) 
	    	{
//    			Log.i(TAG, "updatePosibleMoves: " + gc.cl.p_possibleMoveList.get(i));
		    	if (gc.cl.p_possibleMoveList.get(i).length() == 4)
 			    	setDrawValues(chessBoard.getPosition(gc.cl.p_possibleMoveList.get(i).subSequence (2, 4), gc.isBoardTurn), 12, false);
 		    	else
		    		break;
	    	}
	    	if (gc.cl.p_possibleMoveList.get(0).length() == 4)
 		    	setDrawValues(chessBoard.getPosition(gc.cl.p_possibleMoveList.get(0).subSequence (0, 2), gc.isBoardTurn), 13, false);
		}
    }
    public void updateMoveOnBoard(CharSequence move)	
    {	// show move on board 
    	setDrawValues(-1, 0, true);
    	if (move.length() >= 4)
    	{
    		setDrawValues(chessBoard.getPosition(move.subSequence (0, 2), gc.isBoardTurn), 2, false);
    		setDrawValues(chessBoard.getPosition(move.subSequence (2, 4), gc.isBoardTurn), 1, false);
    	}
    }
    public void setDrawValues(int position, int drawId, boolean initDraw)
	{
		if (initDraw)
			chessBoard.initDrawId();
		if (position < 64 & position >= 0)
			chessBoard.drawId[position] = drawId;
	}
    public void updateTime(CharSequence color)
    {
    	if (!color.equals(""))
    	{
    		if (!ec.chessEnginesOpeningBook)
    		{
		    	if (color.equals("w"))
		    		tc.switchChessClock(true, System.currentTimeMillis());
				else
					tc.switchChessClock(false, System.currentTimeMillis());
    		}
    		if (ec.chessEnginePlayMod == 4 | mate > 0)
    		{
    			updateTimeBackground(lblPlayerTimeA, 2);
    			lblPlayerTimeA.setText("");
    			if (mate == 0)
    				updateTimeBackground(lblPlayerTimeB, 2);
    			else
    				updateTimeBackground(lblPlayerTimeB, tc.showModusWhite);
				lblPlayerTimeB.setText(tc.showWhiteTime);
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
    		if 	(		ec.chessEnginePlayMod != 4
    				&	(
    						(color.equals("w") & tc.timeWhite < 10000)
						| 	(color.equals("b") & tc.timeBlack < 10000)
    					)
    			)
    			infoShowPv = false;
    		else
    			infoShowPv = true;
	    	oldTimeWhite = tc.showWhiteTime;
	        oldTimeBlack = tc.showBlackTime;
	        if (!ec.chessEnginesOpeningBook)
	        {
		        handlerChessClock.removeCallbacks(mUpdateChessClock);
		        handlerChessClock.postDelayed(mUpdateChessClock, 250);
	        }
    	}
    }
    public void updateTimeBackground(TextView tv, int colorId)
    {
    	switch (colorId)
        {
	        case 1:     // >= 10 min
	        	tv.setBackgroundResource(R.drawable.bordergreen);
	        	break;
	        case 2:     // >= 10 sec
	        	tv.setBackgroundResource(R.drawable.borderyellow);
	        	break;
	        case 3:     // < 10 sec
	        	tv.setBackgroundResource(R.drawable.borderpink);
	        	break;
        }
    }
    public void updateGui()
    {	// NEW
//    	Log.i(TAG, "updateGui()");
    	CharSequence messInfo = 	"";
    	if (!gc.isGameLoaded & !gc.isGameOver & !gc.cl.p_variationEnd & gc.cl.p_message.equals(""))
    	{
    		showGameCount = "";
    		if (ec.chessEnginePlayMod == 3 & ec.chessEngineAutoRun)
    		{
    			int cnt = userP.getInt("user_play_eve_gameCounter", 1);
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
    						messInfo = c4aM.getString(R.string.engine_autoPlay) + showGameCount;
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
    					messInfo = c4aM.getString(R.string.engine_pausedNotConnected);
    			}
    		}
    		else
    		{
    			if (!gc.isGameOver & !gc.cl.p_variationEnd & mate == 0)
    			{
    				if (ec.chessEnginePaused | ec.chessEnginePlayMod == 4)
    					messInfo = getEnginePausedMessage();
    				else
    					messInfo = c4aM.getString(R.string.player_move);
    			}
    		}
    		ec.chessEngineProblem = false;
    		if (ec.chessEnginesNotFound)
    		{
//    			Log.i(TAG, "updateGui(), engineNotFound");
    			ec.chessEnginePaused = true;
    			ec.chessEngineProblem = true;
    			messInfo = c4aM.getString(R.string.engineNotFound);
    		}
     	}
    	if (userP.getBoolean("user_options_gui_FlipBoard", false))	// player vs player(edit)
			gc.isBoardFlip = true;
		else
			gc.isBoardFlip = false;
	  	if (gc.isBoardFlip & !gc.cl.p_fen.equals(""))
	  	{
	  		if (gc.cl.p_color.equals("b"))
	  			gc.isBoardTurn = true;
	  		else
	  			gc.isBoardTurn = false;
	  	}
	  	if (!gc.isBoardTurn)
	  		btn_turn.setImageBitmap(combineImages(R.drawable.btn_b3, R.drawable.btn_turn_wb, 0));
	  	else
	  		btn_turn.setImageBitmap(combineImages(R.drawable.btn_b3, R.drawable.btn_turn_bw, 0));
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
    	if (!gc.isGameLoaded & gc.cl.p_message.equals("*"))
    		gc.cl.p_message = "";
    	if (gc.cl.p_moveText.equals(""))
    	{
    		if (gc.errorMessage.equals(""))
	    		lblInfo.setBackgroundResource(R.drawable.borderyellow);
    		else
    		{
	    		lblInfo.setBackgroundResource(R.drawable.borderpink);
    			messInfo = gc.errorMessage;
    		}
    	}
    	else
    		lblInfo.setBackgroundResource(R.drawable.borderblue);
    	if (!messInfo.equals(""))
    		setInfoMessage(false, true, gc.cl.p_message, messInfo, null, gc.cl.p_moveText);
    	else
    	{
    		if (gc.cl.p_message.equals(""))
    			setInfoMessage(false, true, gc.cl.p_message, null, null, gc.cl.p_moveText);
    		else
    			setInfoMessage(true, true, gc.cl.p_message, null, null, gc.cl.p_moveText);
    	}
    	if (gc.cl.p_moveHasVariations | gc.cl.p_moveIsFirstInVariation)	// move has variation? | variation start
    		gc.hasVariations = true;
    	else
    		gc.hasVariations = false;
	  	if (!gc.cl.p_fen.equals(""))
	  	{
			chessBoard.getChessBoardFromFen(gc.cl.p_fen, gc.isBoardTurn, 1);
//			Log.i(TAG, "gc.cl.p_move, possibleMoves: " + gc.cl.p_move + ", " + gc.cl.p_hasPossibleMoves);
//			Log.i(TAG, "gc.cl.p_move1, gc.cl.p_move2: " + gc.cl.p_move1 + ", " + gc.cl.p_move2);
//			Log.i(TAG, "gc.cl.p_moveShow1, gc.cl.p_moveShow2: " + gc.cl.p_moveShow1 + ", " + gc.cl.p_moveShow2);
			if (!gc.cl.p_hasPossibleMoves)
			{
				{
					if (gc.cl.p_moveShow1.equals(""))
				  		updateMoveOnBoard(gc.cl.p_move1.toString() + gc.cl.p_move2);
				  	else
				  		updateMoveOnBoard(gc.cl.p_moveShow1.toString() + gc.cl.p_moveShow2);
				}
			}
			else
				updatePosibleMoves();
		  	gridview.setAdapter(chessBoard);
		  	gridview.setOnItemClickListener(itemClickListener);
		  	gridview.invalidate();
	  	}
	  	if (ec.chessEnginePlayMod == 5 | ec.chessEnginePlayMod == 6)	// player vs player | edit
	  	{
	  		btn_play_start_enabled = false;
	  		btn_play_start.setImageBitmap(combineImages(R.drawable.btn_b2, R.drawable.btn_play_start, R.drawable.btn_disable));
	  	}
	  	else
	  	{
	  		btn_play_start_enabled = true;
	    	if (ec.chessEnginePaused)
	    		btn_play_start.setImageBitmap(combineImages(R.drawable.btn_b2, R.drawable.btn_play_start, 0));
	    	else
	    		btn_play_start.setImageBitmap(combineImages(R.drawable.btn_b2, R.drawable.btn_play_stop, 0));
	  	}
	  	if (!ec.chessEnginePaused & ec.chessEnginePlayMod == 4)	// analysis mode: make move and stop | move cancel
    		btn_moves_cancel.setImageBitmap(combineImages(R.drawable.btn_b2, R.drawable.btn_play_move_continue, 0));
    	else
    		btn_moves_cancel.setImageBitmap(combineImages(R.drawable.btn_b2, R.drawable.btn_moves_cancel, 0));
	  	// B3 moves
    	btn_moves_start_enabled = true;
    	btn_moves_first_enabled = true;
    	btn_moves_previous_enabled = true;
    	btn_moves_next_enabled = true;
    	btn_moves_last_enabled = true;
    	btn_analyses_moves_previous_enabled = true;
    	btn_analyses_moves_next_enabled = true;
    	if (gc.isAutoPlay)
    		btn_moves_start.setImageBitmap(combineImages(R.drawable.btn_b3, R.drawable.btn_moves_pause, 0));
     	else
    		btn_moves_start.setImageBitmap(combineImages(R.drawable.btn_b3, R.drawable.btn_moves_start, 0));
    	btn_moves_first.setImageBitmap(combineImages(R.drawable.btn_b3, R.drawable.btn_moves_first, 0));
    	btn_moves_previous.setImageBitmap(combineImages(R.drawable.btn_b3, R.drawable.btn_moves_previous, 0));
    	btn_moves_next.setImageBitmap(combineImages(R.drawable.btn_b3, R.drawable.btn_moves_next, 0));
    	btn_moves_last.setImageBitmap(combineImages(R.drawable.btn_b3, R.drawable.btn_moves_last, 0));
    	btn_analyses_moves_previous.setImageBitmap(combineImages(R.drawable.btn_b2, R.drawable.btn_moves_previous, 0));
    	btn_analyses_moves_next.setImageBitmap(combineImages(R.drawable.btn_b2, R.drawable.btn_moves_next, 0));
//    	Log.i(TAG, "gc.cl.p_moveIsFirst, gc.cl.p_moveIsFirstInVariation: " + gc.cl.p_moveIsFirst + ", " + gc.cl.p_moveIsFirstInVariation);
//    	Log.i(TAG, "gc.cl.p_gameEnd, gc.cl.p_moveIsLastInVariation: " + gc.cl.p_gameEnd + ", " + gc.cl.p_moveIsLastInVariation);
    	if (gc.cl.p_moveIsFirst | gc.cl.p_moveIsFirstInVariation)	// first move in gameNotation | variationStart
	  	{
	  		btn_moves_first_enabled = false;
	  		btn_moves_previous_enabled = false;
	  		btn_analyses_moves_previous_enabled = false;
	  		btn_moves_first.setImageBitmap(combineImages(R.drawable.btn_b3, R.drawable.btn_moves_first, R.drawable.btn_disable));
	  		btn_moves_previous.setImageBitmap(combineImages(R.drawable.btn_b3, R.drawable.btn_moves_previous, R.drawable.btn_disable));
	  		btn_analyses_moves_previous.setImageBitmap(combineImages(R.drawable.btn_b2, R.drawable.btn_moves_previous, R.drawable.btn_disable));
	  	}
	  	if (gc.cl.p_gameEnd | gc.cl.p_moveIsLastInVariation)	// gameEnd | variationEnd
	  	{
	  		if (gc.isAutoPlay)
	    		stopAutoPlay();
	  		btn_moves_start_enabled = false;
	  		btn_moves_next_enabled = false;
	  		btn_moves_last_enabled = false;
	  		btn_analyses_moves_next_enabled = false;
	  		if (gc.isAutoPlay)
	  			btn_moves_start.setImageBitmap(combineImages(R.drawable.btn_b3, R.drawable.btn_moves_pause, R.drawable.btn_disable));
	  		else
	  			btn_moves_start.setImageBitmap(combineImages(R.drawable.btn_b3, R.drawable.btn_moves_start, R.drawable.btn_disable));
	  		btn_moves_next.setImageBitmap(combineImages(R.drawable.btn_b3, R.drawable.btn_moves_next, R.drawable.btn_disable));
	  		btn_moves_last.setImageBitmap(combineImages(R.drawable.btn_b3, R.drawable.btn_moves_last, R.drawable.btn_disable));
	  		btn_analyses_moves_next.setImageBitmap(combineImages(R.drawable.btn_b2, R.drawable.btn_moves_next, R.drawable.btn_disable));
	  	}
	  	if (!gc.isGameLoaded & !(ec.chessEnginePlayMod == 5 | ec.chessEnginePlayMod == 6))
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
    
    final String TAG = "UiControl";
    final CharSequence APP_PACKAGE_NAME = "ccc.chess.gui.chessforall";
    final CharSequence APP_EMAIL = "c4akarl@gmail.com";
    
    C4aMain c4aM;
    SharedPreferences userP;
    SharedPreferences runP;
    SharedPreferences fmPrefs;
	GameControl gc;
	EngineControl ec;
	TimeControl tc;
	Chess960 chess960;
	ChessBoard chessBoard;
	ChessPromotion promotionDialog;
	PgnIO pgnIO;
	ChessEngineBindTask chessEngineBindTask = null;
    ChessEngineSearchTask chessEngineSearchTask = null;
//    PdbToPgnTask pdbToPgnTask = null;
    private SoundPool mSoundPool;
    private HashMap<Integer, Integer> soundsMap;
//	subActivities intents
    Intent playEngineIntent;
    Intent fileManagerIntent;
    Intent gameDataIntent;
    Intent notationIntent;
    Intent moveTextIntent;
    Intent optionsGameVariant;
    Intent optionsChessBoardIntent;
    Intent optionsGuiIntent;
    Intent optionsTimeControlIntent;
    Intent optionsPlayIntent;
    Intent optionsEnginePlayIntent;
    Intent editChessBoardIntent;
    Intent engineSearchIntent;
    Intent pdbToPgnIntent;
//	subActivities RequestCode
	final static int LOAD_GAME_REQUEST_CODE = 1;
	final static int LOAD_GAME_PREVIOUS_CODE = 9;
	final static int SAVE_GAME_REQUEST_CODE = 2;
	final static int SAVE_LOAD_GAME_REQUEST_CODE = 7;
	final static int SAVE_OK_LOAD_GAME_REQUEST_CODE = 71;
	final static int SAVE_ERROR_LOAD_GAME_REQUEST_CODE = 72;
	final static int DELETE_GAME_REQUEST_CODE = 3;
	final static int DATA_REQUEST_CODE = 4;
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
	final static int PDB_TO_PGN_CODE = 43;
//  dialogs RequestCode
	final static int HELP_DIALOG = 901;
	final static int NO_CHESS_ENGINE_DIALOG = 109;
	final static int PGN_ERROR_DIALOG = 198;
	final static int FILE_LOAD_PROGRESS_DIALOG = 199;
	final static int VARIATION_DIALOG = 200;
	final static int NAG_DIALOG = 201;
	final static int PROMOTION_DIALOG = 300;
	final static int TIME_SETTINGS_DIALOG = 400;
	final static int ENGINE_SEARCH_DIALOG = 500;
	final static int ENGINE_SEARCH_NO_INTERNET_DIALOG = 501;
	final static int QUERY_DIALOG = 600;
	final static int MENU_PLAY_DIALOG = 701;
	final static int MENU_EDIT_DIALOG = 702;
	final static int MENU_PGN_DIALOG = 703;
	final static int MENU_ENGINES_DIALOG = 704;
	final static int MENU_SETTINGS_DIALOG = 705;
	final static int MENU_ABOUT_DIALOG = 706;
	final static int INFO_DIALOG = 909;

//	GUI (R.layout.main)
	RelativeLayout mainView = null;
    TextView lblPlayerNameA = null;
    TextView lblPlayerEloA = null;
    TextView lblPlayerTimeA = null;
    TextView lblPlayerNameB = null;
    TextView lblPlayerEloB = null;
    TextView lblPlayerTimeB = null;
    TextView lblMessage = null;
    ScrollView scrlInfo = null;
    TextView lblInfo = null;
    GridView gridview;

    RelativeLayout btn_a1 = null;
    RelativeLayout btn_a2 = null;
    RelativeLayout btn_a3 = null;
    RelativeLayout btn_a4 = null;
    ImageView scroll_left = null;
    ImageView scroll_right = null;
    int btnScrollId = 2;
    // btn_a1
    ImageView btn_edit_board = null;
    ImageView btn_pgn_data = null;
    ImageView btn_game = null;
    ImageView btn_notation = null;
    ImageView btn_settings = null;	
    ImageView btn_menu = null;		
    // btn_a2
    ImageView btn_play_start = null; boolean btn_play_start_enabled = true;
    ImageView btn_play_options = null;
    ImageView btn_engines = null;	
    ImageView btn_analyses_moves_previous = null; boolean btn_analyses_moves_previous_enabled = true;
    ImageView btn_analyses_moves_next = null; boolean btn_analyses_moves_next_enabled = true;
    ImageView btn_moves_cancel = null;
    // btn_a3
    ImageView btn_moves_start = null; boolean btn_moves_start_enabled = true;
    ImageView btn_moves_first = null; boolean btn_moves_first_enabled = true;
    ImageView btn_moves_previous = null; boolean btn_moves_previous_enabled = true;
    ImageView btn_moves_next = null; boolean btn_moves_next_enabled = true;
    ImageView btn_moves_last = null; boolean btn_moves_last_enabled = true;
    ImageView btn_turn = null;
    // btn_a4
    ImageView btn_pgn_load = null;
    ImageView btn_pgn_save = null;
    ImageView btn_pgn_delete = null;
    ImageView btn_pgn_download = null;
    ImageView btn_cb_copy = null;
    ImageView btn_cb_past = null;

//	Dialog
	C4aDialog c4aDialog;
	C4aImageDialog c4aImageDialog;
	ProgressDialog progressDialog = null;
	HelpDialog helpDialog;
	TimeSettingsDialog timeSettingsDialog;
    boolean withCheckBox = true;
    int activDialog = 0;
    int helpGameStat = 1;
    CharSequence helpTitle = "";
    CharSequence helpText = "";
//	handler
    public Handler handlerAutoPlay = new Handler();	
	public Handler handlerGameShow = new Handler();
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
    boolean en1Selected = false;			// engine 1 selected(menu)
	boolean en2Selected = false;			// engine 2 selected(menu)
	boolean engineJustInstalled = false;	// engine downloaded from market
	boolean continueAnalysis = false;		// continue analysis(after delete move)
    long analysisStartTime = 0;
    long analysisEndTime = 0;
    long sleepTime = 100;
    CharSequence searchTaskFen = "";
    CharSequence searchTaskMoves = "";
    boolean searchTaskRestart = false;		
    
    CharSequence pgnMoves = "";
    int moveList1WIdx = 0;
    int moveList1BIdx = 0;
    int cntPgnMoveList = 0;
    int cntBatchMaOk = 0;
    int cntBatchMaError = 0;
    int cntChess960 = 0;
    int cntResult = 0;
//  touch variables
    int displayWidth = 0;
    int scrollingWidth = 150;
    int dragWidth = 40;
    int touchWidth = 12;
    float downRawX = 0;
    float downRawY = 0;
    float upRawX = 0;
    float upRawY = 0;
    int infoMoveStartX = 0;
    int infoMoveEndX = 0;
    int infoMoveIndex = 0;
    boolean infoIsScrolling = false;
    boolean infoMoveIsSelected = false;
    boolean infoShowPv = false;
//  variables 
    int gridViewSize = 0;
	int arrayId = 100;
    boolean menuSelected = false;
    CharSequence oldTimeWhite = "";
    CharSequence oldTimeBlack = "";
    CharSequence 	messageApp		= "";
    CharSequence 	messageMove 		= "";
    CharSequence 	messageInfo 		= "";
    CharSequence 	messageEngine 		= "";
    int infoContent = 3;	// 1 = app message, 2 = engine message, 3 = moves
    int prevInfoContent = 3;	// 1 = app message, 2 = engine message, 3 = moves
    int infoExpand = 0;		// 0 = standard, -1 = bottom
    CharSequence engineMes = "";
    CharSequence engineStat = "";
    ArrayList<CharSequence> infoPv;
	ArrayList<CharSequence> infoMessage;
	int bestScore = 0;
    CharSequence analysisMessage = "";
    CharSequence showGameCount = "";
    CharSequence autoStopResult = "";
    CharSequence autoStopMoveText = "";
    CharSequence viewModeContent = "";
    int autoStopDrawCount = 0;
    SpannableStringBuilder sb = new SpannableStringBuilder();
    String queryControl = "w";
    
//	PDB to PGN		PDB to PGN		PDB to PGN		PDB to PGN		PDB to PGN  
    final static String PDB_OK = "!!801 OK";
	final static String PDB_ERROR_UNKNOWN = "??901 unknown error";
	final static String PDB_ERROR_ID = "??902 Id error";
	final static String PDB_ERROR_GENRE = "??903 Genre error: ";
	final static String PDB_PGN_PARSE_ERROR = "??904 parse error: ";
	final static String PDB_NO_SOLUTION = "??905 no solution";
	final static String PDB_WRONG_SAN_CHARACTER = "??906 wrong SAN Character: ";
	final static String PDB_FIRST_CHAR_NOT_MOVE_NUMBER = "??907 first character not a move number: ";
	final static String PDB_ANY_MOVE_ERROR = "??908 ~ (any move error)";
	final static String PDB_T_ERROR = "??909 multiple/no: 't='";
	final static String PDB_MATE_ERROR = "??910 RA error: ";
	final static String PDB_ERROR_RA = "??911 RA error: ";
	final static String PDB_FIRST_MOVE_CHECK = "??912 first move check";
	final static String PDB_FIRST_MOVE_PDB_PGN = "??913 first move PDB/PGN unequal: ";
	final static String PDB_NO_FIRST_MOVE = "??914 no first move(PDB)";
//	final static String PDB_NO_ENGINE_MOVE = "??915 no engine move (1. ... )";
	final static String PDB_NO_ENGINE_MATE = "??916 no engine mate: ";
	final static String PDB_PEACE_COUNT_LESS_5 = "??917 peace count < 5";
	final static String PDB_WHITE_PEACE_COUNT_LESS_3 = "??918 white peace count < 3";
	final static String PDB_FEN_ERROR = "??919 FEN error: ";
	final static String PDB_TIMEOUT = "??920 timeout: ";
	final static String PDB_ENGINE_ERROR = "??921 engine error";
	final static String PDB_EP_ERROR = "??922 e.p. error";
	
	final String SAN_CHARACTERS_DE = "0123456789abcdefghKDTLSBO.x-=+#!?()$ ";
	final String FEN_CHARACTERS = "12345678KQRBNPkqrbnp/";
	public Handler handlerPdbPgn = new Handler();
//	int pdbChessEnginePlayMod = 3;
	String gameData = "";
	String puzzleId = "?";
	String pgnTags = "";
	String logMessage = "";
	String engineMessage = "";
	String firstMove = "";
	String gameResult = "1-0";
	RandomAccessFile pgnRaf = null;
	boolean isSearchResult = false;
	boolean isMessageResult = false;
	boolean isPdbEnd = false;
	long inputSize = 0;
	long gameOffset = 0;
	int puzzlesPdb = 0;
	int puzzlesPgn = 0;
    long searchStartTime = 0;		// PDB to PGN
    int MAX_SEARCH_TIME = 20000;	// PDB to PGN, 10 sec.
//    int MAX_SEARCH_TIME = 20000;	// PDB to PGN, 20 sec.
    boolean isTimeOut = false;
//    boolean engineTimeout = false;
    int mate = 0;	// > 0 PDB to PGN batch run
}
