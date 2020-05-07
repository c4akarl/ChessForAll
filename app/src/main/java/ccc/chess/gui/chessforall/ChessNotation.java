package ccc.chess.gui.chessforall;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import java.util.Objects;

public class ChessNotation extends Activity
{
	@SuppressLint("SetTextI18n")
	public void onCreate(Bundle savedInstanceState)
	{
        super.onCreate(savedInstanceState);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		u = new Util();
		userP = getSharedPreferences("user", 0);
		u.updateFullscreenStatus(this, userP.getBoolean("user_options_gui_StatusBar", false));
        setContentView(R.layout.notation);
        title = findViewById(R.id.title);
        notationText = findViewById(R.id.ntTv);
        switch (Objects.requireNonNull(getIntent().getExtras()).getInt("textValue"))
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
        title.setText(getIntent().getExtras().getString("white") + " - " + getIntent().getExtras().getString("black"));
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
			notationText.setText(Objects.requireNonNull(getIntent().getExtras()).getString("moves"));
			break;
		case R.id.ntBtnMoveText:
			notationText.setText(Objects.requireNonNull(getIntent().getExtras()).getString("moves_text"));
			break;
		case R.id.ntBtnPgn:
			notationText.setText(Objects.requireNonNull(getIntent().getExtras()).getString("pgn"));
			break;
		}
	}

//	final String TAG = "ChessNotation";
	Util u;
	SharedPreferences userP;
	TextView title = null;
	TextView notationText = null;

}
