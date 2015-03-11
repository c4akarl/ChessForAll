package ccc.chess.gui.chessforall;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class HelpDialog extends Dialog implements View.OnClickListener
{
	public HelpDialog(Context con, Ic4aDialogCallback callback, int gameControl, String title, String text)
    {
		super(con);
		context  = con;
		c4aCallback = callback;
		this.gameControl = gameControl;
		this.title = Html.fromHtml("<b>" + title + "</b>");	// bold
		this.text = Html.fromHtml(text);
    }
	protected void onCreate(Bundle savedInstanceState) 
	{
        super.onCreate(savedInstanceState);
        setContentView(R.layout.helpdialog);
        helpTitle = (TextView) findViewById(R.id.helpTitle);
        helpText = (TextView) findViewById(R.id.helpText);
        btnOk = (ImageView) findViewById(R.id.btnOk);
        btnOk.setOnClickListener(this);
        btnUserManual = (ImageView) findViewById(R.id.btnUserManual);
        btnUserManual.setOnClickListener(this);
        btnCancelHelp = (ImageView) findViewById(R.id.btnCancelHelp);
        btnCancelHelp.setOnClickListener(this);
        if (gameControl > 9)
        {
			btnCancelHelp.setVisibility(ImageView.INVISIBLE);
			gameControl = gameControl / 10;
        }
		helpTitle.setText(title);
		helpText.setText(text);
    }
	public void onClick(View view) 		
    {	// ClickHandler (ButtonEvents)
		int btnValue = 0;
		switch (view.getId()) 
		{
			case R.id.btnOk: 			btnValue = 1; break;
			case R.id.btnCancelHelp:	btnValue = 2; break;
			case R.id.btnUserManual:	btnValue = 3; break;
		}
		c4aCallback.getCallbackValue(btnValue);
    	dismiss();
	}
	
	private final Ic4aDialogCallback c4aCallback;
	Context context  = null;
	TextView helpTitle = null;
	TextView helpText = null;
	ImageView btnOk = null;
	ImageView btnUserManual = null;
	ImageView btnCancelHelp = null;
	
	int gameControl = 1;
	CharSequence title = "";
	CharSequence text = "";
}
