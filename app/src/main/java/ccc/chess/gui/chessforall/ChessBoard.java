package ccc.chess.gui.chessforall;

import java.util.Arrays;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
//import android.util.Log;

public class ChessBoard extends BaseAdapter
{
	
	public ChessBoard(Context c, CharSequence fen, int fieldSize, int chessSymboleSet) 
	{	//	Constructor
		mContext = c;
		userPrefs = c.getSharedPreferences("user", 0);
		layoutInflater = LayoutInflater.from(mContext);
		imgSize = fieldSize;
		imageSet = chessSymboleSet;
		createImageSet(chessSymboleSet);
		initDrawId();
		itemPaint = new Paint();
		Arrays.fill(cb, 0);
	}
	public CharSequence getChessField(int position, boolean boardTurn) 
	{
//		Log.i(TAG, "position: " + position);
		CharSequence chessField = "";
		if (position >= 0 & position <= 63)
		{
			if (boardTurn)
				chessField = fldTurnData[position];
			else
				chessField = fldData[position];
		}
		return chessField;
	}
	public char getFieldColor(CharSequence field, boolean boardTurn) 
	{
		return ldData[getPosition(field, boardTurn)];
	}
	public int getPosition(CharSequence field, boolean boardTurn) 
	{
		int position = 99;
		CharSequence chessField = "";
		for (int i = 0; i < 64; i++)
        {
			if (boardTurn)
				chessField = fldTurnData[i];
			else
				chessField = fldData[i];
			if (chessField.equals(field))
				position = i;
        }
		return position;
	}
	public void setImageSet(int imgSet) 
	{
		if (imageSet != imgSet)
		{
			imageSet = imgSet;
			createImageSet(imgSet);
		}
	}
	public void initDrawId() 
	{
		for (int i = 0; i < 64; i++)
        {
			drawId[i] = 0;
        }
	}
	public void createImageSet(int imgSet) 
	{
//		Log.i(TAG, "createImageSet");
		switch (imgSet)					// chess symbol set
        {
            case 1:     {createImages(); break;}
            case 2:     {createImages2(); break;}
            case 3:     {createImages3(); break;}
            case 4:     {createImages4(); break;}
            default:    {createImages(); break;}
        }
	}
	public int getCount() {return cb.length;}
	public Object getItem(int position) {return cb[position];}
	public long getItemId(int position) {return position;}
	@Override
	public View getView(int position, View convertView, ViewGroup parent) 
	{
//		ImageView imageView;
        if (convertView == null) 
        {  
       		imageView = new ImageView(mContext);
            imageView.setLayoutParams(new GridView.LayoutParams(imgSize, imgSize));
            imageView.setPadding(0, 0, 0, 0);
            imageView.setScrollContainer(false);
        } 
        else 
            imageView = (ImageView) convertView;
//        Log.i(TAG, "pos, draw, imgSize: " + position + ", " + drawId[position] + ", " + imgSize);
        if (drawId[position] == 0)
        {
        	imageView.setImageResource(cb[position]);
//        	return imageView;
        	
        	// if option coordinates
        	return getDrawItem(position, imageView);
        	
        }
        else
        	return getDrawItem(position, imageView);
	}
	public ImageView getDrawItem(int position, ImageView imageView) 
	{	// paint over current gridViewItem
		itemBitmap = BitmapFactory.decodeResource(mContext.getResources(), cb[position]).copy(Bitmap.Config.ARGB_8888, true);
		if (itemCanvas == null)
			itemCanvas = new Canvas();
		itemCanvas.setBitmap(itemBitmap);
//		Log.i(TAG, "chessField(w, h): " + itemCanvas.getWidth() + ", " + itemCanvas.getHeight());
		itemBorder = 5;
		if (itemCanvas.getWidth() < 60)
			itemBorder = 3;
		if (itemCanvas.getWidth() < 30)
			itemBorder = 1;
		switch (drawId[position])	
        {
            case 1:    // rect green fill
            {
        		itemStart = itemBorder;
        		itemWidth = itemCanvas.getWidth() -itemBorder;
//        		itemPaint.setARGB(100, 0, 180, 0);
        		itemPaint.setARGB(140, 0, 200, 0);
         		if (imageSet == 3)
        			itemPaint.setARGB(100, 0, 0, 240);
        		itemPaint.setStyle(Paint.Style.FILL);
        		itemCanvas.drawRect(itemStart, itemStart, itemWidth, itemWidth, itemPaint);
            	break;
            }
            case 2:    // rect green, transparent
            {
            	itemStart = itemBorder;
        		itemWidth = itemCanvas.getWidth() -itemBorder;
        		itemPaint.setARGB(160, 0, 240, 0);
        		if (imageSet == 3)
        			itemPaint.setARGB(160, 0, 0, 240);
        		itemPaint.setStyle(Paint.Style.FILL);
        		itemCanvas.drawRect(itemStart, itemStart, itemWidth, itemWidth, itemPaint);
            	break;
            }
            case 3:    // rect red, transparent
            {
            	itemStart = itemBorder;
        		itemWidth = itemCanvas.getWidth() -itemBorder;
        		itemPaint.setARGB(70, 200, 80, 90);
        		itemPaint.setStyle(Paint.Style.FILL);
        		itemCanvas.drawRect(itemStart, itemStart, itemWidth, itemWidth, itemPaint);
            	break;
            }
            case 11:    // circle green
            {
            	itemStart = (itemCanvas.getWidth() / 2) - (itemCanvas.getWidth() / 4);
        		itemWidth = itemCanvas.getWidth() / 2;
            	itemPaint.setARGB(200, 110, 180, 100);
            	if (imageSet == 3)
        			itemPaint.setARGB(200, 0, 0, 240);
            	itemPaint.setStyle(Paint.Style.FILL);
            	itemCanvas.drawCircle(itemWidth, itemWidth, itemStart, itemPaint);
            	break;
            }
            case 12:    // circle green transparent
            {
            	itemStart = (itemCanvas.getWidth() / 2) - (itemCanvas.getWidth() / 5);
        		itemWidth = itemCanvas.getWidth() / 2;
            	itemPaint.setARGB(100, 110, 180, 100);
            	if (imageSet == 3)
        			itemPaint.setARGB(100, 0, 0, 240);
            	itemPaint.setStyle(Paint.Style.FILL);
            	itemCanvas.drawCircle(itemWidth, itemWidth, itemStart, itemPaint);
            	break;
            }
            case 13:    // circle red
            {
            	itemStart = (itemCanvas.getWidth() / 2) - (itemCanvas.getWidth() / 4);
        		itemWidth = itemCanvas.getWidth() / 2;
            	itemPaint.setARGB(200, 200, 80, 90);
            	itemPaint.setStyle(Paint.Style.FILL);
            	itemCanvas.drawCircle(itemWidth, itemWidth, itemStart, itemPaint);
            	break;
            }
            case 14:    // circle red transparent
            {
            	itemStart = (itemCanvas.getWidth() / 2) - (itemCanvas.getWidth() / 4);
        		itemWidth = itemCanvas.getWidth() / 2;
            	itemPaint.setARGB(100, 200, 80, 90);
            	itemPaint.setStyle(Paint.Style.FILL);
            	itemCanvas.drawCircle(itemWidth, itemWidth, itemStart, itemPaint);
            	break;
            }
        }
		// drawing chessboard coordinates
		if (userPrefs.getBoolean("user_options_gui_Coordinates", false))
		{
			int textSize = 16;
			// 1-8 | 8-1
			String chessField = "";
			if (!isBoardTurn & fldData[position].subSequence(0, 1).equals("a"))
				chessField = (String) fldData[position].subSequence(1, 2);
			if (isBoardTurn & fldTurnData[position].subSequence(0, 1).equals("h"))
				chessField = (String) fldTurnData[position].subSequence(1, 2);
			if (!chessField.equals(""))
			{
				itemStart = 1;
	    		itemWidth = itemCanvas.getWidth() - 2;
				itemPaint.setColor(Color.BLUE);
				itemPaint.setTextSize(textSize);
		        itemCanvas.drawText(chessField, itemStart, itemWidth, itemPaint);
			}
			// a-h | h-a
			chessField = "";
			if (!isBoardTurn & fldData[position].subSequence(1, 2).equals("1"))
				chessField = (String) fldData[position].subSequence(0, 1);
			if (isBoardTurn & fldTurnData[position].subSequence(1, 2).equals("8"))
				chessField = (String) fldTurnData[position].subSequence(0, 1);
			if (!chessField.equals(""))
			{
				float w = itemPaint.measureText(chessField, 0, chessField.length());
				itemStart = (int) (itemCanvas.getWidth() - (w + 2));
	    		itemWidth = textSize;
				itemPaint.setColor(Color.BLUE);
				itemPaint.setTextSize(textSize);
		        itemCanvas.drawText(chessField, itemStart, itemWidth, itemPaint);
			}
		}

		imageView.setImageBitmap(itemBitmap);
        return imageView;
	}
	public void createImages() 
	{
	   imgL = R.drawable.l;
	   imgD = R.drawable.d;
	   imgUnknownl = R.drawable.unknownl;
	   imgUnknownd = R.drawable.unknownd;
	   imgKld = R.drawable.kld;
	   imgKll = R.drawable.kll;
	   imgKdl = R.drawable.kdl;
	   imgKdd = R.drawable.kdd;
	   imgQll = R.drawable.qll;
	   imgQld = R.drawable.qld;
	   imgQdl = R.drawable.qdl;
	   imgQdd = R.drawable.qdd;
	   imgRll = R.drawable.rll;
	   imgRld = R.drawable.rld;
	   imgRdl = R.drawable.rdl;
	   imgRdd = R.drawable.rdd;
	   imgBll = R.drawable.bll;
	   imgBld = R.drawable.bld;
	   imgBdl = R.drawable.bdl;
	   imgBdd = R.drawable.bdd;
	   imgNll = R.drawable.nll;
	   imgNld = R.drawable.nld;
	   imgNdl = R.drawable.ndl;
	   imgNdd = R.drawable.ndd;
	   imgPll = R.drawable.pll;
	   imgPld = R.drawable.pld;
	   imgPdl = R.drawable.pdl;
	   imgPdd = R.drawable.pdd;
	}
	public void createImages2() 
	{
	   imgL = R.drawable.l2;
	   imgD = R.drawable.d2;
	   imgUnknownl = R.drawable.unknownl2;
	   imgUnknownd = R.drawable.unknownd2;
	   imgKld = R.drawable.kld2;
	   imgKll = R.drawable.kll2;
	   imgKdl = R.drawable.kdl2;
	   imgKdd = R.drawable.kdd2;
	   imgQll = R.drawable.qll2;
	   imgQld = R.drawable.qld2;
	   imgQdl = R.drawable.qdl2;
	   imgQdd = R.drawable.qdd2;
	   imgRll = R.drawable.rll2;
	   imgRld = R.drawable.rld2;
	   imgRdl = R.drawable.rdl2;
	   imgRdd = R.drawable.rdd2;
	   imgBll = R.drawable.bll2;
	   imgBld = R.drawable.bld2;
	   imgBdl = R.drawable.bdl2;
	   imgBdd = R.drawable.bdd2;
	   imgNll = R.drawable.nll2;
	   imgNld = R.drawable.nld2;
	   imgNdl = R.drawable.ndl2;
	   imgNdd = R.drawable.ndd2;
	   imgPll = R.drawable.pll2;
	   imgPld = R.drawable.pld2;
	   imgPdl = R.drawable.pdl2;
	   imgPdd = R.drawable.pdd2;
	}
	public void createImages3() 
	{
	   imgL = R.drawable.l3;
	   imgD = R.drawable.d3;
	   imgUnknownl = R.drawable.unknownl3;
	   imgUnknownd = R.drawable.unknownd3;
	   imgKld = R.drawable.kld3;
	   imgKll = R.drawable.kll3;
	   imgKdl = R.drawable.kdl3;
	   imgKdd = R.drawable.kdd3;
	   imgQll = R.drawable.qll3;
	   imgQld = R.drawable.qld3;
	   imgQdl = R.drawable.qdl3;
	   imgQdd = R.drawable.qdd3;
	   imgRll = R.drawable.rll3;
	   imgRld = R.drawable.rld3;
	   imgRdl = R.drawable.rdl3;
	   imgRdd = R.drawable.rdd3;
	   imgBll = R.drawable.bll3;
	   imgBld = R.drawable.bld3;
	   imgBdl = R.drawable.bdl3;
	   imgBdd = R.drawable.bdd3;
	   imgNll = R.drawable.nll3;
	   imgNld = R.drawable.nld3;
	   imgNdl = R.drawable.ndl3;
	   imgNdd = R.drawable.ndd3;
	   imgPll = R.drawable.pll3;
	   imgPld = R.drawable.pld3;
	   imgPdl = R.drawable.pdl3;
	   imgPdd = R.drawable.pdd3;
	}
	public void createImages4() 
	{
	   imgL = R.drawable.l4;
	   imgD = R.drawable.d4;
	   imgUnknownl = R.drawable.unknownl4;
	   imgUnknownd = R.drawable.unknownd4;
	   imgKld = R.drawable.kld4;
	   imgKll = R.drawable.kll4;
	   imgKdl = R.drawable.kdl4;
	   imgKdd = R.drawable.kdd4;
	   imgQll = R.drawable.qll4;
	   imgQld = R.drawable.qld4;
	   imgQdl = R.drawable.qdl4;
	   imgQdd = R.drawable.qdd4;
	   imgRll = R.drawable.rll4;
	   imgRld = R.drawable.rld4;
	   imgRdl = R.drawable.rdl4;
	   imgRdd = R.drawable.rdd4;
	   imgBll = R.drawable.bll4;
	   imgBld = R.drawable.bld4;
	   imgBdl = R.drawable.bdl4;
	   imgBdd = R.drawable.bdd4;
	   imgNll = R.drawable.nll4;
	   imgNld = R.drawable.nld4;
	   imgNdl = R.drawable.ndl4;
	   imgNdd = R.drawable.ndd4;
	   imgPll = R.drawable.pll4;
	   imgPld = R.drawable.pld4;
	   imgPdl = R.drawable.pdl4;
	   imgPdd = R.drawable.pdd4;
	}
	public int getChessBoardFromFen(CharSequence fen, boolean boardTurn, int fieldId) 
	{
//	   Log.i(TAG, "getChessBoardFromFen");
		isBoardTurn = boardTurn;
		CharSequence nFen = "";
	   int imgId = 0;
	   if (boardTurn)
		   fieldId = 63 - fieldId;
	   for (int i = 0; i < fen.length(); i++)
       {
            if (fen.charAt(i) == ' ')
            	break;
            else
            {
                if (fen.charAt(i) > '8' | fen.charAt(i) == '/')
                {
                	if (fen.charAt(i) != '/')
                		nFen = nFen.toString() +  fen.charAt(i);
                }
                else
                {
                    if (fen.charAt(i) == '1') {nFen = nFen + "-";}
                    if (fen.charAt(i) == '2') {nFen = nFen + "--";}
                    if (fen.charAt(i) == '3') {nFen = nFen + "---";}
                    if (fen.charAt(i) == '4') {nFen = nFen + "----";}
                    if (fen.charAt(i) == '5') {nFen = nFen + "-----";}
                    if (fen.charAt(i) == '6') {nFen = nFen + "------";}
                    if (fen.charAt(i) == '7') {nFen = nFen + "-------";}
                    if (fen.charAt(i) == '8') {nFen = nFen + "--------";}
                }
            }
      }
//	   Log.d(TAG, "FEN, Turn: " + nFen + ", " + boardTurn + nFen.length());
	   int cnt = 64;
	   for (int i = 0; i < nFen.length(); i++)
        {
		   if (!boardTurn)
			   cnt = i;
		   else
			   cnt--;
		   switch (nFen.charAt(i))
	        {
	        	case '-': {if (ldData[i] == 'l') cb[cnt] = imgL;   else cb[cnt] = imgD;   break;}
	        	case '?': {if (ldData[i] == 'l') cb[cnt] = imgUnknownl;   else cb[cnt] = imgUnknownd;   break;}
	            case 'K': {if (ldData[i] == 'l') cb[cnt] = imgKll; else cb[cnt] = imgKld; break;}
	            case 'k': {if (ldData[i] == 'l') cb[cnt] = imgKdl; else cb[cnt] = imgKdd; break;}
	            case 'Q': {if (ldData[i] == 'l') cb[cnt] = imgQll; else cb[cnt] = imgQld; break;}
	            case 'q': {if (ldData[i] == 'l') cb[cnt] = imgQdl; else cb[cnt] = imgQdd; break;}
	            case 'R': {if (ldData[i] == 'l') cb[cnt] = imgRll; else cb[cnt] = imgRld; break;}
	            case 'r': {if (ldData[i] == 'l') cb[cnt] = imgRdl; else cb[cnt] = imgRdd; break;}
	            case 'B': {if (ldData[i] == 'l') cb[cnt] = imgBll; else cb[cnt] = imgBld; break;}
	            case 'b': {if (ldData[i] == 'l') cb[cnt] = imgBdl; else cb[cnt] = imgBdd; break;}
	            case 'N': {if (ldData[i] == 'l') cb[cnt] = imgNll; else cb[cnt] = imgNld; break;}
	            case 'n': {if (ldData[i] == 'l') cb[cnt] = imgNdl; else cb[cnt] = imgNdd; break;}
	            case 'P': {if (ldData[i] == 'l') cb[cnt] = imgPll; else cb[cnt] = imgPld; break;}
	            case 'p': {if (ldData[i] == 'l') cb[cnt] = imgPdl; else cb[cnt] = imgPdd; break;}
	        }
		   if (i == fieldId)
			   imgId = cb[cnt];
//		   Log.d("Piece: ", "i: " + boardTurn + ", " + i + "  " + nFen.charAt(i) + " int: " + cb[i]);
        }
	   return imgId;
   	}
	
	final String TAG = "ChessBoard";
	private Context mContext;
	SharedPreferences userPrefs;
	private int[] cb = new int[64];
	boolean isBoardTurn;
	private int imgSize = 0;
	private int imageSet = 1;
	ImageView imageView = null;
	ImageView grid = null;
	LayoutInflater layoutInflater;
	private int imgL;
	private int imgD;
	private int imgKll;
	private int imgKld;
	private int imgKdl;
	private int imgKdd;
	private int imgQll;
	private int imgQld;
	private int imgQdl;
	private int imgQdd;
	private int imgRll;
	private int imgRld;
	private int imgRdl;
	private int imgRdd;
	private int imgBll;
	private int imgBld;
	private int imgBdl;
	private int imgBdd;
	private int imgNll;
	private int imgNld;
	private int imgNdl;
	private int imgNdd;
	private int imgPll;
	private int imgPld;
	private int imgPdl;
	private int imgPdd;
	private int imgUnknownl;
	private int imgUnknownd;
	final char ldData[] =
    {		'l', 'd', 'l', 'd', 'l', 'd', 'l', 'd',
			'd', 'l', 'd', 'l', 'd', 'l', 'd', 'l',
			'l', 'd', 'l', 'd', 'l', 'd', 'l', 'd',
			'd', 'l', 'd', 'l', 'd', 'l', 'd', 'l',
			'l', 'd', 'l', 'd', 'l', 'd', 'l', 'd',
			'd', 'l', 'd', 'l', 'd', 'l', 'd', 'l',
			'l', 'd', 'l', 'd', 'l', 'd', 'l', 'd',
			'd', 'l', 'd', 'l', 'd', 'l', 'd', 'l'};
	final CharSequence fldData[] =
    {		"a8", "b8", "c8", "d8", "e8", "f8", "g8", "h8",
			"a7", "b7", "c7", "d7", "e7", "f7", "g7", "h7",
			"a6", "b6", "c6", "d6", "e6", "f6", "g6", "h6",
			"a5", "b5", "c5", "d5", "e5", "f5", "g5", "h5",
			"a4", "b4", "c4", "d4", "e4", "f4", "g4", "h4",
			"a3", "b3", "c3", "d3", "e3", "f3", "g3", "h3",
			"a2", "b2", "c2", "d2", "e2", "f2", "g2", "h2",
			"a1", "b1", "c1", "d1", "e1", "f1", "g1", "h1"};
	final CharSequence fldTurnData[] =
    {		"h1", "g1", "f1", "e1", "d1", "c1", "b1", "a1",
			"h2", "g2", "f2", "e2", "d2", "c2", "b2", "a2",
			"h3", "g3", "f3", "e3", "d3", "c3", "b3", "a3",
			"h4", "g4", "f4", "e4", "d4", "c4", "b4", "a4",
			"h5", "g5", "f5", "e5", "d5", "c5", "b5", "a5",
			"h6", "g6", "f6", "e6", "d6", "c6", "b6", "a6",
			"h7", "g7", "f7", "e7", "d7", "c7", "b7", "a7",
			"h8", "g8", "f8", "e8", "d8", "c8", "b8", "a8"};
	// canvas paint 
	int[]  drawId = new int[64];
	Bitmap itemBitmap;						
    Canvas itemCanvas;						
	Paint itemPaint;
	int itemStart = 0;
	int itemWidth = 0;
	int itemBorder = 0;
}
