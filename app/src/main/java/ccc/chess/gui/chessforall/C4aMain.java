package ccc.chess.gui.chessforall;

import java.util.ArrayList;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.PowerManager.WakeLock;
import android.view.ContextMenu;
import android.view.KeyEvent;
//import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnTouchListener;
//import android.util.Log;

public class C4aMain extends Activity implements Ic4aDialogCallback, OnTouchListener
{
//	ACTIVITY(C4aMain) LIFECYCLE		ACTIVITY(C4aMain) LIFECYCLE		ACTIVITY(C4aMain) LIFECYCLE
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        runP = getSharedPreferences("run", 0);		//	run Preferences
        moveHistoryP = getSharedPreferences("moves", 0);	//	moveHistory Preferences
        moveHistory = moveHistoryP.getString("run_moveHistory", "");
//        Log.i(TAG, "moveHistory: " + moveHistory);
        userP = getSharedPreferences("user", 0);	// 	user Preferences
        fmP = getSharedPreferences("fm", 0);		// 	fileManager(PGN) Preferences
        setStringsValues();
        gc = new GameControl(stringValues, moveHistory);
        ec = new EngineControl(C4aMain.this);				//>011 engine controller
        ec.setBookOptions();
        uic = new UiControl(C4aMain.this, gc, ec, userP);	//>012 gui controller
        uic.startC4a();
    }
    @Override
    protected void onDestroy()
    {
//    	Log.i(TAG, "onDestroy()");
    	isAppEnd = true;
    	uic.isSearchTaskStopped = true;
    	uic.stopGameShow();
    	uic.stopTimeHandler(true);
    	try {Thread.sleep(200);} 
		catch (InterruptedException e) {}
    	uic.setInfoMessage(false, true, "", "onDestroy", "", "");
    	uic.updateCurrentPosition("");	
    	setRunMoveHistory();
    	setRunPrefs();
     	if (uic.chessEngineSearchTask != null)
    	{
     		uic.chessEngineSearchTask.cancel(true);
     		uic.chessEngineSearchTask = null;
    	}
     	wakeLock.release();
    	super.onDestroy();
     }
    @Override
    protected void onPause()
    {
//    	Log.i(TAG, "onPause(): " + gc.isOnPause);
    	if (gc.isOnPause)
    	{
	    	if (gc.isGameShow)
	    	{
	    		uic.handlerGameShow.removeCallbacks(uic.mUpdateGameShow);
				if (gc.isAutoPlay)
					uic.stopAutoPlay();
	    	}
	    	uic.stopTimeHandler(false);
    	}
     	super.onPause();
    }
    @Override
    protected void onResume()
    {
//    	Log.i(TAG, "onResume(): " + ec.chessEnginePaused);
    	if (!isAppStart)
    	{
	        if (gc.isGameShow)
			{
	        	ec.chessEnginePaused = ec.lastChessEnginePaused;
	        	gc.isAutoPlay = true;
	        	uic.handlerGameShow.removeCallbacks(uic.mUpdateGameShow);
	        	uic.handlerGameShow.postDelayed(uic.mUpdateGameShow, 10);
			}
    	}
    	else
    		isAppStart = false;
    	setWakeLock(useWakeLock);
    	if (uic.progressDialog != null)
    	{
	    	if (uic.progressDialog.isShowing())
	    		dismissDialog(UiControl.FILE_LOAD_PROGRESS_DIALOG);
    	}
        super.onResume();
    }
    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
        uic.getDataFromIntent(intent);
    }
//	MENU		MENU		MENU		MENU		MENU		MENU		MENU
    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
    {
    	super.onCreateContextMenu(menu, v, menuInfo);
    	uic.onCreateContextMenu(menu, v, menuInfo);
    }
    @Override
    public boolean onContextItemSelected(MenuItem item)
    {
        super.onContextItemSelected(item);
        return uic.onContextItemSelected(item);
    }
//    @Override 
//    public void onContextMenuClosed(Menu menu)
//    {uic.onContextMenuClosed(menu);}
//	DIALOG		DIALOG		DIALOG		DIALOG		DIALOG		DIALOG		DIALOG    
	@Override
    protected Dialog onCreateDialog(int id)
	{return uic.onCreateDialog(id);	}
	@Override
	public void getCallbackValue(int btnValue)
	{uic.getCallbackValue(btnValue);}
//	USER-ACTIONS		USER-ACTIONS		USER-ACTIONS		USER-ACTIONS
//	@Override
	public void onWindowFocusChanged(boolean hasFocus) 
	{
	    if (setButtonScrollPosition)
	    {
	    	super.onWindowFocusChanged(hasFocus);
		    uic.setButtonPosition(uic.btnScrollId);
		    setButtonScrollPosition = false;
	    }
	}
	@Override
	public boolean onTouch(View view, MotionEvent event)
		{return uic.onTouch(view, event);}
	public void myClickHandler(View view)
    {uic.myClickHandler(view);}
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) 
	{	
	    if (keyCode == KeyEvent.KEYCODE_MENU) 
	    	openContextMenu(uic.btn_menu);
	    return super.onKeyUp(keyCode, event);
	}
	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data)
    {uic.onActivityResult(requestCode, resultCode, data);}
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
//		Log.i(TAG, "run_game0_pgn: \n" + pgn);
		ed.putString("run_game0_pgn", pgn.toString());
		ed.putBoolean("run_game0_is_board_turn", gc.isBoardTurn);
		ed.putBoolean("run_game0_is_updated", gc.isGameUpdated);
		ed.putBoolean("run_isGameLoaded", gc.isGameLoaded);
		ed.putString("run_game0_file_base", gc.fileBase.toString());
		ed.putString("run_game0_file_path", gc.filePath.toString());
		ed.putString("run_game0_file_name", gc.fileName.toString());
    	ed.putInt("run_gridViewSize", uic.gridViewSize);
    	ed.putBoolean("run_chessEnginePaused", ec.chessEnginePaused);
		ed.putBoolean("run_lastChessEnginePaused", ec.lastChessEnginePaused);
		ed.putBoolean("run_chessEngineSearching", ec.chessEngineSearching);
		ed.putBoolean("run_chessEngineAutoRun", ec.chessEngineAutoRun);
		ed.putInt("run_chessEnginePlayMod", ec.chessEnginePlayMod);
		ed.putString("run_selectedVariationTitle", (String) gc.selectedVariationTitle);
		ed.putInt("run_timeControl", uic.tc.timeControl);
		ed.putInt("run_timeWhite", uic.tc.timeWhite);
		ed.putInt("run_timeBlack", uic.tc.timeBlack);
		ed.putInt("run_movesToGo", uic.tc.movesToGo);
		ed.putInt("run_bonusWhite", uic.tc.bonusWhite);
		ed.putInt("run_bonusBlack", uic.tc.bonusBlack);
		ed.putInt("run_infoContent", uic.infoContent);
		ed.putInt("run_infoExpand", uic.infoExpand);
		ed.putInt("btnScrollId", uic.btnScrollId);
		ed.commit();
	}
    public void setRunMoveHistory() 
	{
//    	Log.i(TAG, "setRunMoveHistory()");
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
    	uic.gridViewSize = runP.getInt("run_gridViewSize", 464);
        gc.pgnStat = runP.getString("run_pgnStat", "-");
        gc.startPgn = runP.getString("run_game0_pgn", "");
        gc.startMoveIdx = runP.getInt("run_game0_move_idx", 0);
        gc.isBoardTurn = runP.getBoolean("run_game0_is_board_turn", false);
        gc.isGameUpdated = runP.getBoolean("run_game0_is_updated", true);
        gc.isGameLoaded = runP.getBoolean("run_isGameLoaded", false);
        gc.fileBase = runP.getString("run_game0_file_base", "");
        gc.filePath = runP.getString("run_game0_file_path", "");
        gc.fileName = runP.getString("run_game0_file_name", "");
        ec.chessEnginePaused = runP.getBoolean("run_chessEnginePaused", false);
        ec.lastChessEnginePaused = runP.getBoolean("run_lastChessEnginePaused", false);
        ec.chessEngineSearching = runP.getBoolean("run_chessEngineSearching", false);
        ec.chessEngineAutoRun = runP.getBoolean("run_chessEngineAutoRun", false);
        ec.chessEnginePlayMod = runP.getInt("run_chessEnginePlayMod", 1);
        gc.selectedVariationTitle = runP.getString("run_selectedVariationTitle", "");
        uic.infoContent = runP.getInt("run_infoContent", 2);
        uic.infoExpand = runP.getInt("run_infoExpand", 0);
        uic.tc.timeControl = runP.getInt("run_timeControl", 1);
        uic.tc.timeWhite = runP.getInt("run_timeWhite", 300000);
        uic.tc.timeBlack = runP.getInt("run_timeBlack", 300000);
        uic.tc.movesToGo = runP.getInt("run_movesToGo", 0);
        uic.tc.bonusWhite = runP.getInt("run_bonusWhite", 0);
        uic.tc.bonusBlack = runP.getInt("run_bonusBlack", 0);
        uic.tc.initChessClock(uic.tc.timeControl, uic.tc.timeWhite, uic.tc.timeBlack, uic.tc.movesToGo, uic.tc.bonusWhite, uic.tc.bonusBlack);
        uic.tc.setCurrentShowValues();
        uic.btnScrollId = runP.getInt("btnScrollId", 2);
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
    	
    	stringValues.add(26, getString(R.string.nag_$1));
    	stringValues.add(27, getString(R.string.nag_$2));
    	stringValues.add(28, getString(R.string.nag_$3));
    	stringValues.add(29, getString(R.string.nag_$4));
    	stringValues.add(30, getString(R.string.nag_$5));
    	stringValues.add(31, getString(R.string.nag_$6));
    	stringValues.add(32, getString(R.string.nag_$7));
    	stringValues.add(33, getString(R.string.nag_$8));
    	stringValues.add(34, getString(R.string.nag_$9));
    	stringValues.add(35, getString(R.string.nag_$10));
    	stringValues.add(36, getString(R.string.nag_$11));
    	stringValues.add(37, getString(R.string.nag_$12));
    	stringValues.add(38, getString(R.string.nag_$13));
    	stringValues.add(39, getString(R.string.nag_$14));
    	stringValues.add(40, getString(R.string.nag_$15));
    	stringValues.add(41, getString(R.string.nag_$16));
    	stringValues.add(42, getString(R.string.nag_$17));
    	stringValues.add(43, getString(R.string.nag_$18));
    	stringValues.add(44, getString(R.string.nag_$19));
	}
    
	final String TAG = "C4aMain";
    GameControl gc;
    EngineControl ec;
    UiControl uic;
	public SharedPreferences userP;
	public SharedPreferences runP;
	public SharedPreferences moveHistoryP;
	public CharSequence moveHistory;
	public ArrayList<CharSequence> stringValues = new ArrayList<CharSequence>();
	SharedPreferences fmP;
	boolean isAppStart = true;
	boolean isAppEnd = false;
	public WakeLock wakeLock = null;
    public boolean useWakeLock = false;
    boolean setButtonScrollPosition = true;
}