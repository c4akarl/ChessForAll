package ccc.chess.gui.chessforall;

import android.app.Dialog;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

public class ChessPromotion extends Dialog implements OnClickListener 
{
	MainActivity chessGame;
	public interface MyDialogListener 
	{ 
        void onOkClick(int promValue);
	} 
	final String TAG = "ChessPromotion";
	ImageButton btnQ = null;
	ImageButton btnR = null;
	ImageButton btnB = null;
	ImageButton btnN = null;
	private MyDialogListener promListener;
    public ChessPromotion(MainActivity cg, MyDialogListener listener)
    { 
    	super(cg);
    	promListener = listener;
    	this.requestWindowFeature(Window.FEATURE_NO_TITLE);
    	setContentView(R.layout.promotion);
    	chessGame = cg;
    	btnQ = (ImageButton) findViewById(R.id.promQ);
    	btnR = (ImageButton) findViewById(R.id.promR);
    	btnB = (ImageButton) findViewById(R.id.promB);
    	btnN = (ImageButton) findViewById(R.id.promN);
    	setImages();
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
    public void setImages() 
    {
		if (chessGame.gc.cl.p_color.equals("w"))
		{
			btnQ.setImageDrawable(chessGame.getResources().getDrawable(R.drawable._1_wq));
			btnR.setImageDrawable(chessGame.getResources().getDrawable(R.drawable._1_wr));
			btnB.setImageDrawable(chessGame.getResources().getDrawable(R.drawable._1_wb));
			btnN.setImageDrawable(chessGame.getResources().getDrawable(R.drawable._1_wn));
		}
		else
		{
			btnQ.setImageDrawable(chessGame.getResources().getDrawable(R.drawable._1_bq));
			btnR.setImageDrawable(chessGame.getResources().getDrawable(R.drawable._1_br));
			btnB.setImageDrawable(chessGame.getResources().getDrawable(R.drawable._1_bb));
			btnN.setImageDrawable(chessGame.getResources().getDrawable(R.drawable._1_bn));
		}
    }
}
