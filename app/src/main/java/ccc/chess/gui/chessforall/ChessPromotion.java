package ccc.chess.gui.chessforall;

import android.app.Dialog;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
//import android.util.Log;

public class ChessPromotion extends Dialog implements OnClickListener 
{
	C4aMain chessGame;
	public interface MyDialogListener 
	{ 
        public void onOkClick(int promValue); 
	} 
	final String TAG = "ChessPromotion";
	ImageButton btnQ = null;
	ImageButton btnR = null;
	ImageButton btnB = null;
	ImageButton btnN = null;
	private MyDialogListener promListener;
    public ChessPromotion(C4aMain cg, MyDialogListener listener)
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
    	char pc = 'l';
    	if (!chessGame.gc.cl.p_color.equals("w"))
    		pc = 'd';
    	char fc = 'd';
    	btnQ.setImageDrawable(chessGame.getResources().getDrawable(chessGame.getResources().getIdentifier("q" + pc + fc, "drawable", chessGame.getPackageName())));
    	btnR.setImageDrawable(chessGame.getResources().getDrawable(chessGame.getResources().getIdentifier("r" + pc + fc, "drawable", chessGame.getPackageName())));
    	btnB.setImageDrawable(chessGame.getResources().getDrawable(chessGame.getResources().getIdentifier("b" + pc + fc, "drawable", chessGame.getPackageName())));
    	btnN.setImageDrawable(chessGame.getResources().getDrawable(chessGame.getResources().getIdentifier("n" + pc + fc, "drawable", chessGame.getPackageName())));
    }
}
