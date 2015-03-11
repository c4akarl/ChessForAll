package ccc.chess.gui.chessforall;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class ChessNotation extends Activity
{
	final String TAG = "ChessNotation";
	TextView notationText = null;
	public void onCreate(Bundle savedInstanceState) 
	{
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notation);
        notationText = (TextView) findViewById(R.id.ntTv);
        switch (getIntent().getExtras().getInt("textValue")) 
		{
		case 1:
			notationText.setText(getIntent().getExtras().getString("moves"));
			break;
		case 2:
			notationText.setText(getIntent().getExtras().getString("moves_text"));
			break;
		case 3:
			notationText.setText(getIntent().getExtras().getString("pgn"));
			break;
		}
        
        this.setTitle(getIntent().getExtras().getString("white") + " - " + getIntent().getExtras().getString("black"));
 	}
	public void myClickHandler(View view) 		
    {	// ClickHandler	(ButtonEvents)
		Intent returnIntent;
		switch (view.getId()) 
		{
		case R.id.ntBtnOk:
			returnIntent = new Intent();
			setResult(RESULT_OK, returnIntent);
			finish();
			break;
		case R.id.ntBtnMove:
			notationText.setText(getIntent().getExtras().getString("moves"));
			break;
		case R.id.ntBtnMoveText:
			notationText.setText(getIntent().getExtras().getString("moves_text"));
			break;
		case R.id.ntBtnPgn:
			notationText.setText(getIntent().getExtras().getString("pgn"));
			break;
		}
	}
}
