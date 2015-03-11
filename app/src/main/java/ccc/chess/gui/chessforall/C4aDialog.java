package ccc.chess.gui.chessforall;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class C4aDialog extends Dialog implements View.OnClickListener
{
	final String TAG = "C4aDialog";
	private final Ic4aDialogCallback c4aCallback;
	TextView message = null;
	EditText inputText = null;
	Button btn1 = null;
	Button btn2 = null;
	Button btn3 = null;
	String txtTitle = "";
	String txtMessage = "";
	String txtButton1 = "";
	String txtButton2 = "";
	String txtButton3 = "";
	String number = "1";
	int editControl = 0;
	String editText = "";
	public C4aDialog(Context context, Ic4aDialogCallback callback, String title, String txtBtn1, String txtBtn2, 
			String txtBtn3, String mess, int editControl, String editText)
    {
		super(context);
		c4aCallback = callback;
		txtTitle = title;
		txtButton1 = txtBtn1;
		txtButton2 = txtBtn2;
		txtButton3 = txtBtn3;
		txtMessage = mess;
		this.editControl = editControl;
		this.editText = editText;
    }
    protected void onCreate(Bundle savedInstanceState) 
	{
//    	Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.c4adialog);
        this.setTitle(txtTitle);
		message = (TextView) findViewById(R.id.dgMessage);
		inputText = (EditText) findViewById(R.id.dgInputText);
		if (editControl == 0)
			inputText.setVisibility(TextView.INVISIBLE);
		else
		{
			if (editControl == 2)
				inputText.setInputType(InputType.TYPE_CLASS_NUMBER);
			inputText.setText(editText);
			inputText.setOnFocusChangeListener(new View.OnFocusChangeListener() 
			{
                @Override
                public void onFocusChange(View v, boolean hasFocus) 
                {
                    if (hasFocus) 
                        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            });
		}
		btn1 = (Button) findViewById(R.id.dgBtn1);
		btn2 = (Button) findViewById(R.id.dgBtn2);
		btn3 = (Button) findViewById(R.id.dgBtn3);
		message.setText(txtMessage);
		if (!txtButton1.equals(""))
		{
			btn1.setOnClickListener(this);
			btn1.setText(txtButton1);
		}
		else
			btn1.setVisibility(Button.INVISIBLE);
		if (!txtButton2.equals(""))
		{
			btn2.setOnClickListener(this);
			btn2.setText(txtButton2);
		}
		else
			btn2.setVisibility(Button.INVISIBLE);
		if (!txtButton3.equals(""))
		{
			btn3.setOnClickListener(this);
			btn3.setText(txtButton3);
		}
		else
			btn3.setVisibility(Button.INVISIBLE);
    }
	public void onClick(View view) 		
    {	// ClickHandler (ButtonEvents)
		int btnValue = 0;
		switch (view.getId()) 
		{
		case R.id.dgBtn1: btnValue = 1; break;
		case R.id.dgBtn2: 
			btnValue = 2; 
			if (!txtButton2.equals("") & editControl > 0)
			{
				number = inputText.getText().toString();
				InputMethodManager imm = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
				imm.hideSoftInputFromWindow(inputText.getWindowToken(), 0);
				try {Thread.sleep(100);} 
				catch (InterruptedException e) {}
			}
			break;
		case R.id.dgBtn3: btnValue = 3; break;
		}
		c4aCallback.getCallbackValue(btnValue);
    	dismiss();
	}
	public String getNumber() {return number;}
//	@Override
//	public void onCancel(DialogInterface dialog) 
//	{
//		c4aCallback.getCallbackValue(9);
//    	dismiss();
//	}
}
