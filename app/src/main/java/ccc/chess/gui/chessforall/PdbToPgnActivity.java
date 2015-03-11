package ccc.chess.gui.chessforall;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class PdbToPgnActivity extends Activity
{
	public void onCreate(Bundle savedInstanceState) 
	{
        super.onCreate(savedInstanceState);
        userPrefs = getSharedPreferences("user", 0);
        setContentView(R.layout.pdbtopgn);
        tvFileInput = (TextView) findViewById(R.id.tvFileInput);
        tvFileInput.setText(INPUT_TXT);
        tvFileOutput = (TextView) findViewById(R.id.tvFileOutput);
        tvFileOutput.setText(OUTPUT_TXT);
        tvFileLog = (TextView) findViewById(R.id.tvFileLog);
        tvFileLog.setText(FILELOG_TXT);
        tvMessage = (TextView) findViewById(R.id.tvMessage);
        tvMessage.setText(MESSAGE_TXT);
        etFileInput = (EditText) findViewById(R.id.etFileInput);
        etFileOutput = (EditText) findViewById(R.id.etFileOutput);
        etFileLog = (EditText) findViewById(R.id.etFileLog);
        etMessage = (EditText) findViewById(R.id.etMessage);
        getPrefs();
	}
	public void myClickHandler(View view) 				
    {	// ClickHandler	(ButtonEvents)
		Intent returnIntent;
		returnIntent = new Intent();
		switch (view.getId()) 
		{
			case R.id.btnOk:
				setPrefs();
				etMessage.setText("");
				String errorMessage = checkFileData();
				if (errorMessage.equals(""))
				{
					setResult(RESULT_OK, returnIntent);
					finish();
				}
				else
					etMessage.setText(errorMessage);
			break;
		}
	}
	protected void setPrefs() 
	{
		SharedPreferences.Editor ed = userPrefs.edit();
		ed.putString("user_pdb_pgn_input", etFileInput.getText().toString());
		ed.putString("user_pdb_pgn_output", etFileOutput.getText().toString());
		ed.putString("user_pdb_pgn_logfile", etFileLog.getText().toString());
		ed.commit();
	}
	protected void getPrefs() 
	{
		etFileInput.setText(userPrefs.getString("user_pdb_pgn_input", DEFAULT_PATH));
		etFileOutput.setText(userPrefs.getString("user_pdb_pgn_output", DEFAULT_PATH));
		etFileLog.setText(userPrefs.getString("user_pdb_pgn_logfile", DEFAULT_PATH));
	}
	protected String checkFileData() 
	{
		String message = "";
		File fi = new File(etFileInput.getText().toString());
		if (!fi.exists())
			return "input file not exists";
		
		String out = etFileOutput.getText().toString();
		if (out.endsWith("/") | !out.endsWith(".pgn"))
			return "output file not ends with .pgn";
		String[] outSplit = out.split("/");
		if (outSplit.length > 0)
		{
			int idx = out.indexOf(outSplit[outSplit.length -1]);
			String replace = out.substring(idx, out.length());
			out = out.replace(replace, "");
			File fop = new File(out);
			if (!fop.isDirectory())
				return "?pgn: " + out;
		}
		
		out = etFileLog.getText().toString();
		if (out.endsWith("/") | !out.endsWith(".log"))
			return "logfile not ends with .log";
		outSplit = out.split("/");
		if (outSplit.length > 0)
		{
			int idx = out.indexOf(outSplit[outSplit.length -1]);
			String replace = out.substring(idx, out.length());
			out = out.replace(replace, "");
			File fop = new File(out);
			if (!fop.isDirectory())
				return "?log: " + out;
		}
		return message;
	}
	
	final String TAG = "PdbToPgnActivity";
	SharedPreferences userPrefs;
//	final String DEFAULT_PATH = "/mnt/sdcard/c4a/";
	final String DEFAULT_PATH = Environment.getExternalStorageDirectory().getAbsolutePath()  + "/c4a/";
	final String INPUT_TXT = "Input File (*.txt)";
	final String OUTPUT_TXT = "Output File (*.pgn)";
	final String FILELOG_TXT = "Logfile (*.log)";
	final String MESSAGE_TXT = "Messages / errors";
	TextView tvFileInput = null;
	TextView tvFileOutput = null;
	TextView tvFileLog = null;
	TextView tvMessage = null;	
	EditText etFileInput = null;
	EditText etFileOutput = null;
	EditText etFileLog = null;
	EditText etMessage = null;
}
