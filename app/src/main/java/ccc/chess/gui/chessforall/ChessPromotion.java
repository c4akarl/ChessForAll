package ccc.chess.gui.chessforall;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.ImageButton;

public class ChessPromotion extends Dialog implements OnClickListener 
{
	private Context context;
	public interface MyDialogListener 
	{ 
        void onOkClick(int promValue);
	}

//	final String TAG = "ChessPromotion";
	private ImageButton btnQ;
	private ImageButton btnR;
	private ImageButton btnB;
	private ImageButton btnN;
	private MyDialogListener promListener;

    ChessPromotion(Context context, MyDialogListener listener, CharSequence color)
    {
    	super(context);
    	promListener = listener;
    	this.requestWindowFeature(Window.FEATURE_NO_TITLE);
    	setContentView(R.layout.promotion);
    	this.context = context;
    	btnQ = findViewById(R.id.promQ);
    	btnR = findViewById(R.id.promR);
    	btnB = findViewById(R.id.promB);
    	btnN = findViewById(R.id.promN);
    	setImages(color);
    	btnQ.setOnClickListener(this);
    	btnR.setOnClickListener(this);
    	btnB.setOnClickListener(this);
    	btnN.setOnClickListener(this);
    }
    public void onClick(View view) 
    {
    	int promValue = 0;
    	switch (view.getId()) 
    	{
    		case R.id.promQ: promValue = 1; break;
    		case R.id.promR: promValue = 2; break;
    		case R.id.promB: promValue = 3; break;
    		case R.id.promN: promValue = 4; break;
    	}
    	promListener.onOkClick(promValue);
    	dismiss();
    }
    private void setImages(CharSequence color)
    {
		if (color.equals("w"))
		{
			btnQ.setImageDrawable(context.getResources().getDrawable(R.drawable._1_wq));
			btnR.setImageDrawable(context.getResources().getDrawable(R.drawable._1_wr));
			btnB.setImageDrawable(context.getResources().getDrawable(R.drawable._1_wb));
			btnN.setImageDrawable(context.getResources().getDrawable(R.drawable._1_wn));
		}
		else
		{
			btnQ.setImageDrawable(context.getResources().getDrawable(R.drawable._1_bq));
			btnR.setImageDrawable(context.getResources().getDrawable(R.drawable._1_br));
			btnB.setImageDrawable(context.getResources().getDrawable(R.drawable._1_bb));
			btnN.setImageDrawable(context.getResources().getDrawable(R.drawable._1_bn));
		}
    }
}
