package ccc.chess.gui.chessforall;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class ChessMoveText extends Activity
{
	final String TAG = "ChessMoveText";
	Intent returnIntent = new Intent();
	EditText moveText;
	public void onCreate(Bundle savedInstanceState) 
	{
        super.onCreate(savedInstanceState);
        setContentView(R.layout.movetext);
        moveText = (EditText) findViewById(R.id.mtEt);
        moveText.setText(getIntent().getExtras().getString("move_text"));
        moveText.requestFocus();
        this.setTitle("move text");
 	}
	public void myClickHandler(View view) 		// ClickHandler 					(ButtonEvents)
    {
		switch (view.getId()) 
		{
		case R.id.mtBtnOk:
			returnIntent.putExtra("text", moveText.getText().toString());
			setResult(RESULT_OK, returnIntent);
			finish();
			break;
		}
	}
}
