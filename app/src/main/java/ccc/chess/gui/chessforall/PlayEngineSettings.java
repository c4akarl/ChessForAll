package ccc.chess.gui.chessforall;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

public class PlayEngineSettings extends Activity implements Ic4aDialogCallback
{

	public void onCreate(Bundle savedInstanceState) 
	{
        super.onCreate(savedInstanceState);
		u = new Util();
		fmPrefs = getSharedPreferences("fm", 0);
		userPrefs = getSharedPreferences("user", 0);
		runPrefs = getSharedPreferences("run", 0);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		u.updateFullscreenStatus(this, userPrefs.getBoolean("user_options_gui_StatusBar", false));
		setContentView(R.layout.playenginesettings);
        fileManagerIntent = new Intent(this, FileManager.class);
        fileIO = new FileIO(this);
        currentBase = runPrefs.getString("run_game0_file_base", "");
		if (!currentBase.equals("") & !currentBase.equals("assets/") & !currentBase.equals("url"))	// == sd-card
		{
			currentPath = runPrefs.getString("run_game0_file_path", "");
			currentFile = runPrefs.getString("run_game0_file_name", "");
		}
		else
		{
			currentPath = fmPrefs.getString("fm_extern_save_path", "");
			currentFile = fmPrefs.getString("fm_extern_save_file", "");
		}
        getPrefs();
		title = findViewById(R.id.title);
		u.setTextViewColors(title, "#6b2c2d", "#f1e622");
        etPePath = (EditText) findViewById(R.id.etPePath);
		u.setTextViewColors(etPePath, "#ADE4A7", "#000000");
        etPeFile = (EditText) findViewById(R.id.etPeFile);
		u.setTextViewColors(etPeFile, "#ADE4A7", "#000000");
        etPeRound = (EditText) findViewById(R.id.etPeRound);
		u.setTextViewColors(etPeRound, "#ADE4A7", "#000000");
        etPeGameCounter = (EditText) findViewById(R.id.etPeGameCounter);
		u.setTextViewColors(etPeGameCounter, "#ADE4A7", "#000000");
        etPeMessage = (TextView) findViewById(R.id.etPeMessage);
        cbPeAutoSave = (CheckBox) findViewById(R.id.cbPeAutoSave);
        cbPeAutoFlipColor = (CheckBox) findViewById(R.id.cbPeAutoFlipColor);
        cbPeAutoCurrentGame = (CheckBox) findViewById(R.id.cbPeAutoCurrentGame);
        etPePath.setText(path);
        etPeFile.setText(file);
        etPeMessage.setVisibility(EditText.INVISIBLE);
        etPeRound.setText(Integer.toString(round));
        etPeGameCounter.setText(Integer.toString(gameCounter));
        cbPeAutoSave.setChecked(autoSave);
        cbPeAutoFlipColor.setChecked(autoFlipColor);
        cbPeAutoCurrentGame.setChecked(autoCurrentGame);
	}

	@Override
    protected void onDestroy() 					
    {
    	if (progressDialog != null)
    	{
	    	if (progressDialog.isShowing())
	     		dismissDialog(ENGINE_PROGRESS_DIALOG);
    	}
     	super.onDestroy();
    }

	public void myClickHandler(View view) 				
    {	// ClickHandler	(ButtonEvents)
		Intent returnIntent;
		returnIntent = new Intent();
		switch (view.getId()) 
		{
		case R.id.btnPeAutoPlay:
			if (checkFileData(etPePath.getText().toString(), etPeFile.getText().toString()))
			{
				setTitle(getString(R.string.engineProgressDialog));
				setPrefs();
				setResult(RESULT_OK, returnIntent);
				finish();
			}
			break;
		case R.id.btnPeAutoSetFile:
			if (fileIO.isSdk30()) {
				removeDialog(NO_FILE_ACTIONS_DIALOG);
				showDialog(NO_FILE_ACTIONS_DIALOG);
				return;
			}
			fileManagerIntent.putExtra("fileActionCode", 5);
	    	fileManagerIntent.putExtra("displayActivity", 1);
	    	startActivityForResult(fileManagerIntent, ENGINE_AUTOPLAY_REQUEST_CODE);		// start FileManager - Activity(with GUI)
			break;
		case R.id.etPePath:
		case R.id.etPeFile:
			etPeMessage.setVisibility(EditText.INVISIBLE);
			break;
		}
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data)			
    {
		switch(requestCode) 
	    {
		    case ENGINE_AUTOPLAY_REQUEST_CODE: 
		    	if (resultCode == RESULT_OK)
				{
			    	etPePath.setText(data.getStringExtra("filePath"));
			    	etPeFile.setText(data.getStringExtra("fileName"));
			    	etPeMessage.setVisibility(EditText.INVISIBLE);
				}
				break;
	    }
    }

	@Override
    protected Dialog onCreateDialog(int id) 
	{
		if (id == ENGINE_PROGRESS_DIALOG) 
        {
			String mes = getString(R.string.engineProgressDialog);
        	progressDialog = new ProgressDialog(this);
			progressDialog.setMessage(mes);
	        progressDialog.setCancelable(true);
	        progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() 
	        {
	            public void onCancel(DialogInterface dialog) 
	            {
	            	finish();
	            }
	        });
	        return progressDialog;
        }
		if (id == NO_FILE_ACTIONS_DIALOG)
		{
			c4aDialog = new C4aDialog(this, this, getString(R.string.dgTitleDialog),
					"", getString(R.string.btn_Ok), "", getString(R.string.noFileActions), 0, "");
			return c4aDialog;
		}
        return null;
	}

	@Override
	public void getCallbackValue(int btnValue) { }

	protected boolean checkFileData(String path, String file) 
	{
		boolean fileOk = true;
		if (!etPeFile.getText().toString().endsWith(".pgn"))
		{
			etPeMessage.setVisibility(EditText.VISIBLE);
			etPeMessage.setText(getString(R.string.fmFileNotEndsWithPgn));
			fileOk = false;
		}
		if (etPeFile.getText().toString().equals(".pgn"))
		{
			etPeMessage.setVisibility(EditText.VISIBLE);
			etPeMessage.setText(getString(R.string.fmPgnError));
			fileOk = false;
		}
		if (!path.endsWith("/"))
		{
			path = path + "/";
			etPePath.setText(path);
		}
		if (!fileIO.pathExists(path))
		{
			etPeMessage.setVisibility(EditText.VISIBLE);
			etPeMessage.setText(getString(R.string.fmPathError));
			fileOk = false;
		}
		return fileOk;
	}

	protected void getPrefs() 
	{
		path = userPrefs.getString("user_play_eve_path", currentPath);
		file = userPrefs.getString("user_play_eve_file", currentFile);
		round = userPrefs.getInt("user_play_eve_round", 1);
		gameCounter = userPrefs.getInt("user_play_eve_gameCounter", 1);
		autoSave = userPrefs.getBoolean("user_play_eve_autoSave", true);
		autoFlipColor = userPrefs.getBoolean("user_play_eve_autoFlipColor", true);
		autoCurrentGame = userPrefs.getBoolean("user_play_eve_autoCurrentGame", false);
	}

	protected void setPrefs() 
	{
		path = etPePath.getText().toString();
		file = etPeFile.getText().toString();
		round = Integer.parseInt (etPeRound.getText().toString());
		gameCounter = Integer.parseInt (etPeGameCounter.getText().toString());
		SharedPreferences.Editor ed = userPrefs.edit();
		ed.putInt("user_play_playMod", 3);
		ed.putString("user_play_ceWhite", userPrefs.getString("user_play_engineName", ""));
        ed.putString("user_play_ceBlack", userPrefs.getString("user_play_engineName", ""));
		ed.putString("user_play_eve_path", path);
		ed.putString("user_play_eve_file", file);
        ed.putInt("user_play_eve_round", round);
        ed.putInt("user_play_eve_gameCounter", gameCounter);
        ed.putBoolean("user_play_eve_autoSave", cbPeAutoSave.isChecked());
        ed.putBoolean("user_play_eve_autoFlipColor", cbPeAutoFlipColor.isChecked());
        ed.putBoolean("user_play_eve_autoCurrentGame", cbPeAutoCurrentGame.isChecked());
        ed.commit();
	}

//	final String TAG = "PlayEngineSettings";
	final static int ENGINE_AUTOPLAY_REQUEST_CODE = 50;
	Util u;
	Intent fileManagerIntent;
	private static final int ENGINE_PROGRESS_DIALOG = 1;
	final static int NO_FILE_ACTIONS_DIALOG = 193;
	ProgressDialog progressDialog = null;
	C4aDialog c4aDialog;
	FileIO fileIO;
	String path = "";
	String file = "";
	int round = 1;
	int gameCounter = 1;
	boolean autoSave = true;
	boolean autoFlipColor = true;
	boolean autoCurrentGame = true;
	String currentBase = "";
	String currentPath = "";
	String currentFile = "";
	SharedPreferences fmPrefs;
	SharedPreferences userPrefs;
	SharedPreferences runPrefs;
	TextView title;
	EditText etPePath = null;
	EditText etPeFile = null;
	EditText etPeRound = null;
	EditText etPeGameCounter = null;
	TextView etPeMessage = null;
	CheckBox cbPeAutoSave;
	CheckBox cbPeAutoFlipColor;
	CheckBox cbPeAutoCurrentGame;

}
