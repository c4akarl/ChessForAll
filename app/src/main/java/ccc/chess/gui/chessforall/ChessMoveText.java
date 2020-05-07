package ccc.chess.gui.chessforall;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.EditText;

import java.util.Objects;

public class ChessMoveText extends Activity
{
	public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		u = new Util();
		userP = getSharedPreferences("user", 0);
		u.updateFullscreenStatus(this, userP.getBoolean("user_options_gui_StatusBar", false));
        setContentView(R.layout.movetext);
        moveText = findViewById(R.id.mtEt);
//        moveText.setText(getIntent().getExtras().getString("move_text"));
        moveText.setText(Objects.requireNonNull(getIntent().getExtras()).getString("move_text"));
        moveText.requestFocus();
 	}
	public void myClickHandler(View view) 		// ClickHandler 					(ButtonEvents)
    {
//		switch (view.getId())
		if (view.getId() == R.id.mtBtnOk)
		{
			String text = moveText.getText().toString();
			if (text.contains("\n"))
				text = text.replace("\n", " ");
			if (text.endsWith(" "))
				text = text.replaceAll("\\s+$", "");
			returnIntent.putExtra("text", text);
			setResult(RESULT_OK, returnIntent);
			finish();
		}
	}

//	final String TAG = "ChessMoveText";
	Util u;
	SharedPreferences userP;
	Intent returnIntent = new Intent();
	EditText moveText;

}
