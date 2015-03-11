package ccc.chess.gui.chessforall;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

public class C4aImageDialog extends Dialog implements View.OnClickListener
{	// dialog with 3 image buttons
	final String TAG = "C4aImageDialog";
	private final Ic4aDialogCallback c4aCallback;
	Context context;
	TextView message = null;
	ImageView btn1 = null;
	ImageView btn2 = null;
	ImageView btn3 = null;
	int btn1Id = 0;
	int btn2Id = 0;
	int btn3Id = 0;
	String txtTitle = "";
	String txtMessage = "";
	public C4aImageDialog(Context context, Ic4aDialogCallback callback, String title, String message,
							int btn1Id, int btn2Id, int btn3Id)
    {
		super(context);
		this.context = context;
		c4aCallback = callback;
		txtTitle = title;
		this.btn1Id = btn1Id;
		this.btn2Id = btn2Id;
		this.btn3Id = btn3Id;
		txtMessage = message;
    }
	protected void onCreate(Bundle savedInstanceState) 
	{
        super.onCreate(savedInstanceState);
        setContentView(R.layout.c4aimagedialog);
        this.setTitle(txtTitle);
		message = (TextView) findViewById(R.id.engineSearchMessage);
		btn1 = (ImageView) findViewById(R.id.btn1);
		btn2 = (ImageView) findViewById(R.id.btn2);
		btn3 = (ImageView) findViewById(R.id.btn3);
		message.setText(txtMessage);
		if (btn1Id != 0)
		{
			btn1.setImageDrawable(context.getResources().getDrawable(btn1Id));
			btn1.setOnClickListener(this);
		}
		else
			btn1.setVisibility(ImageView.INVISIBLE);
		if (btn2Id != 0)
		{
			btn2.setImageDrawable(context.getResources().getDrawable(btn2Id));
			btn2.setOnClickListener(this);
		}
		else
			btn2.setVisibility(ImageView.INVISIBLE);
		if (btn3Id != 0)
		{
			btn3.setImageDrawable(context.getResources().getDrawable(btn3Id));
			btn3.setOnClickListener(this);
		}
		else
			btn3.setVisibility(ImageView.INVISIBLE);
    }
	@Override
	public void onClick(View view) 
	{	// ClickHandler (ButtonEvents)
		int btnValue = 0;
		switch (view.getId()) 
		{
		case R.id.btn1: btnValue = 1; break;
		case R.id.btn2: btnValue = 2; break;
		case R.id.btn3: btnValue = 3; break;
		}
		c4aCallback.getCallbackValue(btnValue);
    	dismiss();
	}
}
