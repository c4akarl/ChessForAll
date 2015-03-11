package ccc.chess.gui.chessforall;

import java.util.ArrayList;
import java.util.Random;
import ccc.chess.logic.c4aservice.Chess960;
import ccc.chess.logic.c4aservice.ChessLogic;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnTouchListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;
import android.util.Log;

public class EditChessBoard extends Activity implements Ic4aDialogCallback, DialogInterface.OnCancelListener, OnTouchListener
{
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
        super.onCreate(savedInstanceState);
        userP = getSharedPreferences("user", 0);
        runP = getSharedPreferences("run", 0);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.editchessboard);
        if (!userP.getBoolean("user_options_gui_StatusBar", false))
    		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    	else
    		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN, WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        setStringsValues();
        cl = new ChessLogic(stringValues, "");
        chess960 = new Chess960();
        currentFen = getIntent().getExtras().getString("currentFen");
        gridViewSize = getIntent().getExtras().getInt("gridViewSize");
        newFen = initFen(currentFen);
        setFenTo518(newFen);
        fieldSize = getIntent().getExtras().getInt("fieldSize");
        chessBoard = new ChessBoard(this, newFen, fieldSize, 1);
        btnOk = (ImageView) findViewById(R.id.btnOk);
        btnFen = (ImageView) findViewById(R.id.btnFen);
        registerForContextMenu(btnFen);
        btnFen.setOnTouchListener((OnTouchListener) this);
        btnBoard = (ImageView) findViewById(R.id.btnBoard);
        registerForContextMenu(btnBoard);
        btnBoard.setOnTouchListener((OnTouchListener) this);
        btnCamera = (ImageView) findViewById(R.id.btnCamera);
        
//        if (!runP.getBoolean("run_isActivate", false))
        
//        	btnCamera.setVisibility(ImageView.INVISIBLE);
        
        help = (ImageView) findViewById(R.id.help);
        message = (TextView) findViewById(R.id.message);
        if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_SMALL)
        	message.setVisibility(TextView.INVISIBLE);
        trash = (ImageView) findViewById(R.id.trash);
        color = (ImageView) findViewById(R.id.color);
        wKing = (ImageView) findViewById(R.id.wKing);
        wQueen = (ImageView) findViewById(R.id.wQueen);
        wRook = (ImageView) findViewById(R.id.wRook);
        wBishop = (ImageView) findViewById(R.id.wBishop);
        wKnight = (ImageView) findViewById(R.id.wKnight);
        wPawn = (ImageView) findViewById(R.id.wPawn);
        bKing = (ImageView) findViewById(R.id.bKing);
        bQueen = (ImageView) findViewById(R.id.bQueen);
        bRook = (ImageView) findViewById(R.id.bRook);
        bBishop = (ImageView) findViewById(R.id.bBishop);
        bKnight = (ImageView) findViewById(R.id.bKnight);
        bPawn = (ImageView) findViewById(R.id.bPawn);
        gridview = (GridView) findViewById(R.id.gridview);
        gridview.setAdapter(chessBoard);
        gridview.setClickable(true);
        gridview.setOnItemClickListener(itemClickListener);
		gridview.invalidate();
        deleteBackgroundBorder();
        piece = 'P'; 
        wPawn.setImageBitmap(setBackground(R.drawable.pll, true));
        showChessBoard();
	}
	public boolean onTouch(View view, MotionEvent event)									
	{
		if (view.getId() == R.id.btnFen & event.getAction() == MotionEvent.ACTION_UP)
			openContextMenu(btnFen);
		if (view.getId() == R.id.btnBoard & event.getAction() == MotionEvent.ACTION_UP)
			openContextMenu(btnBoard);
		return false;
	}
	public OnItemClickListener itemClickListener = new OnItemClickListener() 
	{
	    public void onItemClick(AdapterView<?> parent, View view, int position, long id) 
	    {
	    	if (gridview.isClickable())
				moveAction(position);
	    }
	};
	public void moveAction(int position) 											// move action (chessboard) 		(ButtonEvents)
	{
		CharSequence nFen = "";
    	if (chess960Id == 518)
    	{
    		selectedPosition = position;
        	if (creatingChess960)
        	{
        		if (position > 55)
        		{
            		nFen = setChess960(newFen, position, piece);
            		if (!nFen.equals(""))
                		newFen = nFen;
        		}
        	}
        	else
        	{
        		char p = '-';
        		if (piece != ' ')
        			p = piece;
            	nFen = setPieceToFen(newFen, position, p);
            	if (!nFen.equals(""))
            		newFen = nFen;
        	}
            showChessBoard();
    	}
	}
//	MENU		MENU		MENU		MENU		MENU		MENU		MENU
	@Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
    {
    	super.onCreateContextMenu(menu, v, menuInfo);
    	if (v == btnFen)
		{
			MenuInflater inflater = getMenuInflater();
		    inflater.inflate(R.menu.c4a_menu_edit_fen, menu);
		}
    	if (v == btnBoard)
		{
			MenuInflater inflater = getMenuInflater();
		    inflater.inflate(R.menu.c4a_menu_edit_board, menu);
		}
    	showChessBoard();
    }
	@Override
	public boolean onContextItemSelected(MenuItem item)
    {
		super.onContextItemSelected(item);
		chess960Id = 518;
		creatingChess960 = false;
		chess960Status = 0;
		showChessBoard();
        switch(item.getItemId())
        {
	        case R.id.menu_edit_fen_color:
	        	removeDialog(COLOR_DIALOG);
	    		showDialog(COLOR_DIALOG);
	        	break;
	        case R.id.menu_edit_fen_castling:
	        	removeDialog(CASTLING_DIALOG);
	    		showDialog(CASTLING_DIALOG);
	        	break;
	        case R.id.menu_edit_fen_ep:
	        	removeDialog(ENPASSANT_DIALOG);
	    		showDialog(ENPASSANT_DIALOG);
	        	break;
	        case R.id.menu_edit_fen_draw50:
	        	removeDialog(DRAW50_DIALOG);
	    		showDialog(DRAW50_DIALOG);
	        	break;
	        case R.id.menu_edit_fen_movecounter:
	        	removeDialog(MOVE_COUNTER_DIALOG);
	    		showDialog(MOVE_COUNTER_DIALOG);
	        	break;
	        case R.id.menu_edit_board_clear:
	        	newFen = clearFen;
	        	deleteBackgroundBorder();
	            piece = 'P'; 
	            wPawn.setImageBitmap(setBackground(R.drawable.pll, true));
	        	break;
	        case R.id.menu_edit_board_current:
	        	newFen = currentFen;
	        	deleteBackgroundBorder();
	            piece = 'P'; 
	            wPawn.setImageBitmap(setBackground(R.drawable.pll, true));
	        	break;
	        case R.id.menu_edit_board_standard:
	        	newFen = standardFen;
	        	newBoardFen = newFen;
	        	deleteBackgroundBorder();
	            piece = 'P'; 
	            wPawn.setImageBitmap(setBackground(R.drawable.pll, true));
	        	break;
	        case R.id.menu_edit_board_chess960_manual:
	        	initChess960();
	        	break;
	        case R.id.menu_edit_board_chess960_random:
	        	Random r;
	    		int ir = 518;
	    		while (ir == 518)
	    		{
	    			r = new Random();
	    	        ir = r.nextInt(960);
	    		}
	    		chess960Id = ir;
	    		newFen = chess960.createChessPosition(ir);
	    		newBoardFen = newFen;
	        	break;
	        case R.id.menu_edit_board_chess960_number:
	        	removeDialog(CHESS960_ID_DIALOG);
	    		showDialog(CHESS960_ID_DIALOG);
	            break;
        }
        showChessBoard();
        return true;
    }
	public void myClickHandler(View view) 		
    {	// ClickHandler	(ButtonEvents)
		if (view.getId() != R.id.color & view.getId() != R.id.help)
			deleteBackgroundBorder();
		switch (view.getId()) 
		{
		case R.id.btnOk:
			finishActivity();
			break;
		case R.id.btnCamera:
			try
			{
				Intent i = getPackageManager().getLaunchIntentForPackage("ccc.chessboard.recognition");
	    		i.setFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK);	 // + onActivityResult()
	    		i.putExtra("calling-activity", "GUI");
				startActivityForResult(i, CHESSBOARD_RECOGNITION_REQUEST_CODE);
			}
			catch (NullPointerException e)
			{
				removeDialog(NO_ACTIVITY_DIALOG);
	    		showDialog(NO_ACTIVITY_DIALOG);
			}
			break;
		case R.id.trash:
			trash.setImageDrawable(getResources().getDrawable(R.drawable.trash_selected));
			if (creatingChess960)
	        	initChess960();
			else
			{
				piece = ' ';
        		selectedPosition = -1;
			}
			break;
		case R.id.color:
			newFen = changeColor(newFen);
			showChessBoard();
			break;
		case R.id.help:
			showDialog(HELP_DIALOG);
			break;
		case R.id.wKing: 	piece = 'K'; wKing.setImageBitmap(setBackground(R.drawable.kll, true)); break;
		case R.id.wQueen: 	piece = 'Q'; wQueen.setImageBitmap(setBackground(R.drawable.qll, true)); break;
		case R.id.wRook: 	piece = 'R'; wRook.setImageBitmap(setBackground(R.drawable.rll, true)); break;
		case R.id.wBishop:	piece = 'B'; wBishop.setImageBitmap(setBackground(R.drawable.bll, true)); break;
		case R.id.wKnight:	piece = 'N'; wKnight.setImageBitmap(setBackground(R.drawable.nll, true)); break;
		case R.id.wPawn:	piece = 'P'; wPawn.setImageBitmap(setBackground(R.drawable.pll, true)); break;
		case R.id.bKing:	piece = 'k'; bKing.setImageBitmap(setBackground(R.drawable.kdl, true)); break;
		case R.id.bQueen:	piece = 'q'; bQueen.setImageBitmap(setBackground(R.drawable.qdl, true)); break;
		case R.id.bRook:	piece = 'r'; bRook.setImageBitmap(setBackground(R.drawable.rdl, true)); break;
		case R.id.bBishop:	piece = 'b'; bBishop.setImageBitmap(setBackground(R.drawable.bdl, true)); break;
		case R.id.bKnight:	piece = 'n'; bKnight.setImageBitmap(setBackground(R.drawable.ndl, true)); break;
		case R.id.bPawn:	piece = 'p'; bPawn.setImageBitmap(setBackground(R.drawable.pdl, true)); break;
		}
	}
	public void onActivityResult(int requestCode, int resultCode, Intent data)			
    {	// subActivity result
		switch(requestCode) 
	    {
		    case CHESSBOARD_RECOGNITION_REQUEST_CODE:
		    	if (resultCode == C4aMain.RESULT_OK)
		    	{
			    	String fen1 = data.getStringExtra("fen");
			    	Log.i(TAG, "fen1: " + fen1);
			    	CharSequence tmp[] = ((String) newFen).split(" ");
					if (tmp.length == 6)
					{
						newFen = fen1 + " " + tmp[1] + " " + tmp[2] + " " + tmp[3] + " " + tmp[4] + " " + tmp[5];
						showChessBoard();
					}
		    	}
				break;
	    }
    }
	public void finishActivity() 		
    {
//		Log.i(TAG, "finishActivity(), newFen, chess960Id: " + newFen + ", " + chess960Id);
		returnIntent.putExtra("newFen", newFen);
		returnIntent.putExtra("chess960Id", Integer.toString(chess960Id));
		setResult(RESULT_OK, returnIntent);
		finish();
    }
	public Dialog onCreateDialog(int id)
	{
		CharSequence castling = "";
		CharSequence ep = "";
		CharSequence draw50 = "";
		CharSequence moveCounter = "";
		CharSequence tmp[] = ((String) newFen).split(" ");
		if (tmp.length == 6)
		{
			castling = tmp[2];
			ep = tmp[3];
			draw50 = tmp[4];
			moveCounter = tmp[5];
		}
		activDialog = id;
		if (id == CHESS960_ID_DIALOG)  
        {
			c4aDialog = new C4aDialog(this, this, getString(R.string.dgTitleDialog), 
				"", getString(R.string.btn_Ok), "", getString(R.string.inputDialog_chess960Id), 2, "");
			c4aDialog.setOnCancelListener(this);
            return c4aDialog;
        }
		if (id == COLOR_DIALOG)  
        {
			c4aDialog = new C4aDialog(this, this, getString(R.string.dgTitleDialog), 
					getString(R.string.daTxtLblWhite), "", getString(R.string.daTxtLblBlack), 
					getString(R.string.inputDialog_color), 0, "");
			c4aDialog.setOnCancelListener(this);
            return c4aDialog;
        }
		if (id == CASTLING_DIALOG)  
        {
			c4aDialog = new C4aDialog(this, this, getString(R.string.dgTitleDialog), 
				"", getString(R.string.btn_Ok), "", getString(R.string.inputDialog_castling), 1, (String) castling);
			c4aDialog.setOnCancelListener(this);
            return c4aDialog;
        }
		if (id == ENPASSANT_DIALOG)  
        {
			c4aDialog = new C4aDialog(this, this, getString(R.string.dgTitleDialog), 
				"", getString(R.string.btn_Ok), "", getString(R.string.inputDialog_enPassant), 1, (String) ep);
			c4aDialog.setOnCancelListener(this);
            return c4aDialog;
        }
		if (id == DRAW50_DIALOG)  
        {
			c4aDialog = new C4aDialog(this, this, getString(R.string.dgTitleDialog), 
				"", getString(R.string.btn_Ok), "", getString(R.string.inputDialog_draw50), 2, (String) draw50);
			c4aDialog.setOnCancelListener(this);
            return c4aDialog;
        }
		if (id == MOVE_COUNTER_DIALOG)  
        {
			c4aDialog = new C4aDialog(this, this, getString(R.string.dgTitleDialog), 
				"", getString(R.string.btn_Ok), "", getString(R.string.inputDialog_moveCounter), 2, (String) moveCounter);
			c4aDialog.setOnCancelListener(this);
            return c4aDialog;
        }
		if (id == NO_ACTIVITY_DIALOG)  
        {
			String noA = "Application not installed";
			c4aDialog = new C4aDialog(this, this, noA, 
				"", getString(R.string.btn_Ok), "", "", 0, "");
			c4aDialog.setOnCancelListener(this);
            return c4aDialog;
        }
		if (id == HELP_DIALOG)  
        {
			helpDialog = new HelpDialog(this, this, 1, getString(R.string.edit_board), getString(R.string.edit_board_text));
			helpDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			return helpDialog;
        }
		if (id == PROGRESS_DIALOG) 
        {
			progressDialog = new ProgressDialog(this);
			progressDialog.setMessage(this.getString(R.string.editChessboardDetection));
			progressDialog.setCancelable(true);
			progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() 
	        {
	            public void onCancel(DialogInterface dialog) { finish(); }
	        });
	        return progressDialog;
        }
		return null;
	}
	public void getCallbackValue(int btnValue) 
	{
		if (activDialog == CHESS960_ID_DIALOG)
		{
    		if (btnValue == 2)
    		{
    			int id = 0;
    	    	try		{id = Integer.parseInt(c4aDialog.getNumber());}
    	    	catch 	(NumberFormatException e) {id = 0;}
    			if (id < 0 | id > 959)
    				id = 518;
    			chess960Id = id;
    			newFen = chess960.createChessPosition(id);
    			newBoardFen = newFen;
    			showChessBoard();
    		}
 		}
		if (activDialog == COLOR_DIALOG)
		{
    		if (btnValue == 1 | btnValue == 3)
    		{
    			CharSequence bw = "w";
    			if (btnValue == 3)
    				bw = "b";
    			CharSequence tmp[] = ((String) newFen).split(" ");
    			if (tmp.length == 6)
    				newFen = tmp[0] + " " + bw + " "  + tmp[2] + " "  + tmp[3] + " "  + tmp[4] + " "  + tmp[5];
    			showChessBoard();
    		}
 		}
		if (activDialog == CASTLING_DIALOG)
		{
    		if (btnValue == 2)
    		{
    			CharSequence castling = c4aDialog.getNumber();
    	    	if (castling.equals(""))
    	    		castling = "-";
    	    	CharSequence tmp[] = ((String) newFen).split(" ");
    			if (tmp.length == 6)
    				newFen = tmp[0] + " " + tmp[1] + " "  + castling + " "  + tmp[3] + " "  + tmp[4] + " "  + tmp[5];
    			showChessBoard();
    		}
 		}
		if (activDialog == ENPASSANT_DIALOG)
		{
    		if (btnValue == 2)
    		{
    			CharSequence ep = c4aDialog.getNumber();
    	    	if (ep.equals(""))
    	    		ep = "-";
    	    	CharSequence tmp[] = ((String) newFen).split(" ");
    			if (tmp.length == 6)
    				newFen = tmp[0] + " " + tmp[1] + " "  + tmp[2] + " "  + ep + " "  + tmp[4] + " "  + tmp[5];
    			showChessBoard();
    		}
 		}
		if (activDialog == DRAW50_DIALOG)
		{
    		if (btnValue == 2)
    		{
    			int id = 0;
    	    	try		{id = Integer.parseInt(c4aDialog.getNumber());}
    	    	catch 	(NumberFormatException e) {id = 0;}
    	    	CharSequence tmp[] = ((String) newFen).split(" ");
    			if (tmp.length == 6)
    				newFen = tmp[0] + " " + tmp[1] + " "  + tmp[2] + " "  + tmp[3] + " "  + id + " "  + tmp[5];
    			showChessBoard();
    		}
 		}
		if (activDialog == MOVE_COUNTER_DIALOG)
		{
    		if (btnValue == 2)
    		{
    			int id = 0;
    	    	try		{id = Integer.parseInt(c4aDialog.getNumber());}
    	    	catch 	(NumberFormatException e) {id = 1;}
    	    	CharSequence tmp[] = ((String) newFen).split(" ");
    			if (tmp.length == 6)
    				newFen = tmp[0] + " " + tmp[1] + " "  + tmp[2] + " "  + tmp[3] + " "  + tmp[4] + " "  + id;
    			showChessBoard();
    		}
 		}
	}
	@Override
	public void onCancel(DialogInterface dialog) {	}
	public void initChess960() 		
    {
		creatingChess960 = true;
    	chess960Status = 1;
    	newFen = start960Fen;
		cntB = 0;
		cntQ = 0;
		cntN = 0;
		cntEmpty = 8;
		selectedPosition = -2;
    }
	public CharSequence setChess960(CharSequence fen, int pos, char p) 		
    {
//		Log.i(TAG, "pos, piece, fen: " + pos + ", " + p + ", " + fen );
		if (cntB == 1)
		{
			if ((cntBPos + pos) % 2 == 0)	// error: both bishops on same field color
				return fen;
		}
		CharSequence nFen = "";
		char[] fen64 = getFen64(fen);
		if (fen64.length == 64)
		{
			if (fen64[pos] == '-')
			{
				if (p == 'B') {cntB++; cntEmpty--;}
				if (cntB == 1)
					cntBPos = pos;
				if (cntB > 1) {cntB = 0; chess960Status = 2;}	// next Q
				if (p == 'Q') {cntQ++; cntEmpty--; chess960Status = 3;}	// next N
				if (p == 'N') {cntN++; cntEmpty--;}
				nFen = setPieceToFen(fen, pos, p);
				if (cntN > 1 & cntEmpty == 3)							// set RKR
				{
					fen64 = getFen64(nFen);
					int cnt = 1;
					for (int i = 56; i < fen64.length; i++)
				    {
						if (fen64[i] == '-')
						{
							if (cnt == 2)
								fen64[i] = 'K';
							else
								fen64[i] = 'R';
							cnt++;
						}
				    }
					CharSequence s = getFenFromChar(fen64);
					CharSequence tmp[] = ((String) nFen).split(" ");
					if (tmp.length == 6)
						nFen = s + " " + tmp[1] + " " + "KQkq" + " " + tmp[3] + " " + tmp[4] + " " + tmp[5];
				}
			}
			else
				return fen;
			}
		else
			return fen;
		return nFen;
    }
	public void showChess960() 		
    {
		char[] fen64 = getFen64(newFen);
		int cnt = 0;
		if (fen64.length == 64)
		{
			for (int i = 56; i < fen64.length; i++)
		    {
				if (fen64[i] != '-')
					fen64[cnt] = Character.toLowerCase(fen64[i]);
				cnt++;
		    }
		}
		CharSequence s = getFenFromChar(fen64);
		CharSequence tmp[] = ((String) newFen).split(" ");
		if (tmp.length == 6)
			newFen = s + " " + tmp[1] + " " + tmp[2] + " " + tmp[3] + " " + tmp[4] + " " + tmp[5];
    }
	public void setPiecesVisible() 		
    {
		if (chess960Status == 0)
		{
			if (chess960Id == 518)
			{
				if ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) 
		        		!= Configuration.SCREENLAYOUT_SIZE_SMALL)
					btnFen.setVisibility(ImageView.VISIBLE);
				trash.setVisibility(ImageView.VISIBLE);
				color.setVisibility(ImageView.VISIBLE);
				wKing.setVisibility(ImageView.VISIBLE);
	        	wQueen.setVisibility(ImageView.VISIBLE);
	        	wQueen.setBackgroundResource(R.drawable.layout_border_transparent);
	        	wRook.setVisibility(ImageView.VISIBLE);
	        	wBishop.setVisibility(ImageView.VISIBLE);
	        	wBishop.setBackgroundResource(R.drawable.layout_border_transparent);
	        	wKnight.setVisibility(ImageView.VISIBLE);
	        	wKnight.setBackgroundResource(R.drawable.layout_border_transparent);
	        	wPawn.setVisibility(ImageView.VISIBLE);
	        	bKing.setVisibility(ImageView.VISIBLE);
	        	bQueen.setVisibility(ImageView.VISIBLE);
	        	bRook.setVisibility(ImageView.VISIBLE);
	        	bBishop.setVisibility(ImageView.VISIBLE);
	        	bKnight.setVisibility(ImageView.VISIBLE);
	        	bPawn.setVisibility(ImageView.VISIBLE);
			}
			else
			{
				btnFen.setVisibility(ImageView.INVISIBLE);
				trash.setVisibility(ImageView.INVISIBLE);
				color.setVisibility(ImageView.INVISIBLE);
				wKing.setVisibility(ImageView.INVISIBLE);
	        	wQueen.setVisibility(ImageView.INVISIBLE);
	        	wRook.setVisibility(ImageView.INVISIBLE);
	        	wBishop.setVisibility(ImageView.INVISIBLE);
	        	wKnight.setVisibility(ImageView.INVISIBLE);
	        	wPawn.setVisibility(ImageView.INVISIBLE);
	        	bKing.setVisibility(ImageView.INVISIBLE);
	        	bQueen.setVisibility(ImageView.INVISIBLE);
	        	bRook.setVisibility(ImageView.INVISIBLE);
	        	bBishop.setVisibility(ImageView.INVISIBLE);
	        	bKnight.setVisibility(ImageView.INVISIBLE);
	        	bPawn.setVisibility(ImageView.INVISIBLE);
			}	
		}
		else
		{
			btnFen.setVisibility(ImageView.INVISIBLE);
			color.setVisibility(ImageView.INVISIBLE);
			wKing.setVisibility(ImageView.INVISIBLE);
			wRook.setVisibility(ImageView.INVISIBLE);
			wPawn.setVisibility(ImageView.INVISIBLE);
			bKing.setVisibility(ImageView.INVISIBLE);
        	bQueen.setVisibility(ImageView.INVISIBLE);
        	bRook.setVisibility(ImageView.INVISIBLE);
        	bBishop.setVisibility(ImageView.INVISIBLE);
        	bKnight.setVisibility(ImageView.INVISIBLE);
        	bPawn.setVisibility(ImageView.INVISIBLE);
        	if (chess960Status == 1)	// Bishop
        	{
        		piece = 'B'; 
        		wBishop.setVisibility(ImageView.VISIBLE);
        		wBishop.setBackgroundResource(R.drawable.layout_border_red);
        		wQueen.setVisibility(ImageView.INVISIBLE);
        		wKnight.setVisibility(ImageView.INVISIBLE);
        	}
        	if (chess960Status == 2)	// Queen
        	{
        		piece = 'Q';
        		wQueen.setVisibility(ImageView.VISIBLE);
        		wQueen.setBackgroundResource(R.drawable.layout_border_red);
        		wBishop.setVisibility(ImageView.INVISIBLE);
        		wKnight.setVisibility(ImageView.INVISIBLE);
        	}
        	if (chess960Status == 3)	// Knight
        	{
        		piece = 'N';
        		wKnight.setVisibility(ImageView.VISIBLE);
        		wKnight.setBackgroundResource(R.drawable.layout_border_red);
        		wQueen.setVisibility(ImageView.INVISIBLE);
        		wBishop.setVisibility(ImageView.INVISIBLE);
        	}
		}
    }
	public Bitmap setBackground(int resourceId, boolean background) 		
    {
		
		Bitmap itemBitmap = BitmapFactory.decodeResource(getResources(), resourceId).copy(Bitmap.Config.ARGB_8888, true);
		Canvas itemCanvas = new Canvas();
		itemCanvas.setBitmap(itemBitmap);
		Paint itemPaint = new Paint();
		if (background)
		{
			int itemWidth = itemCanvas.getWidth() -3;
			itemPaint.setARGB(100, 0, 180, 0);
			itemCanvas.drawRect(3, 3, itemWidth, itemWidth, itemPaint);
			
		}
		else
		{
			itemCanvas.drawARGB(120, 0, 0, 0);
		}
        return itemBitmap;
    }
	public void deleteBackgroundBorder() 		
    {
	    wKing.setImageBitmap(setBackground(R.drawable.kll, false));
	    wQueen.setImageBitmap(setBackground(R.drawable.qll, false));
		wRook.setImageBitmap(setBackground(R.drawable.rll, false));
		wBishop.setImageBitmap(setBackground(R.drawable.bll, false));
		wKnight.setImageBitmap(setBackground(R.drawable.nll, false));
		wPawn.setImageBitmap(setBackground(R.drawable.pll, false));
		bKing.setImageBitmap(setBackground(R.drawable.kdl, false));
		bQueen.setImageBitmap(setBackground(R.drawable.qdl, false));
		bRook.setImageBitmap(setBackground(R.drawable.rdl, false));
		bBishop.setImageBitmap(setBackground(R.drawable.bdl, false));
		bKnight.setImageBitmap(setBackground(R.drawable.ndl, false));
		bPawn.setImageBitmap(setBackground(R.drawable.pdl, false));
		trash.setImageDrawable(getResources().getDrawable(R.drawable.trash));
    }
	public void setFen(CharSequence fen) 		
    {
		CharSequence tmp[] = ((String) fen).split(" ");
		if (tmp.length == 6)
		{
			fenColor = tmp[1];
			fenCastling = tmp[2];
			fenEnPassant = tmp[3];
			draw50 = tmp[4];
			moveCounter = tmp[5];
		}
		CharSequence mes = "";
		if (fenColor.equals("w"))
			mes = getString(R.string.editWhiteMoves) + ", FEN: ";
		else
			mes = getString(R.string.editBlackMoves) + ", FEN: ";
		mes = mes.toString() + fenColor + " " + fenCastling + " " + fenEnPassant + " " + draw50 + " " + moveCounter;
		message.setText(mes);
    }
	public void setFenTo518(CharSequence fen) 		
    {
//		Log.i(TAG, "fen: " + fen);
		chess960Id = 518;
		boolean castling_Q = false;
		boolean castling_K = false;
		boolean castling_q = false;
		boolean castling_k = false;
		CharSequence newCastling = "";
		char[] fen64 = getFen64(fen);
		if (fen64[4] == 'k')
		{
			if (fen64[0] == 'r')
				castling_q = true;
			if (fen64[7] == 'r')
				castling_k = true;
		}
		if (fen64[60] == 'K')
		{
			if (fen64[56] == 'R')
				castling_Q = true;
			if (fen64[63] == 'R')
				castling_K = true;
		}
		CharSequence tmp[] = ((String) fen).split(" ");
		for (int i = 0; i < tmp[2].length(); i++)
	    {
			if (tmp[2].charAt(i) == 'K' & castling_K)
				newCastling = newCastling.toString() + tmp[2].charAt(i);
			if (tmp[2].charAt(i) == 'Q' & castling_Q)
				newCastling = newCastling.toString() + tmp[2].charAt(i);
			if (tmp[2].charAt(i) == 'k' & castling_k)
				newCastling = newCastling.toString() + tmp[2].charAt(i);
			if (tmp[2].charAt(i) == 'q' & castling_q)
				newCastling = newCastling.toString() + tmp[2].charAt(i);
	    }
		if (newCastling.equals(""))
			newCastling = "-";
		if (tmp.length == 6)
			newFen = tmp[0] + " " + tmp[1] + " " + newCastling + " " + tmp[3] + " " + tmp[4] + " 1";
    }
	public CharSequence initFen(CharSequence fen) 		
    {	// FEN: initialize move counter
		CharSequence nFen = "";
		CharSequence tmp[] = ((String) fen).split(" ");
		if (tmp.length == 6)
			nFen = tmp[0] + " " + tmp[1] + " " + tmp[2] + " " + tmp[3] + " " + tmp[4] + " 1";
		return nFen;
    }
	public CharSequence changeColor(CharSequence fen) 		
    {
		CharSequence tmp[] = ((String) fen).split(" ");
		if (tmp.length == 6)
		{
			if (tmp[1].equals("w"))
				return tmp[0] + " " + "b" + " " + tmp[2] + " " + tmp[3] + " " + tmp[4] + " " + tmp[5];
			else
				return tmp[0] + " " + "w" + " " + tmp[2] + " " + tmp[3] + " " + tmp[4] + " " + tmp[5];
		}
		else
			return fen;
    }
	public CharSequence setPieceToFen(CharSequence fen, int pos, char p) 		
    {	// set piece to FEN
//		Log.i(TAG, "oldfen: " + fen + " >>> " + pos + ", " + p);
		CharSequence nFen = "";
		char[] fen64 = getFen64(fen);
// ERROR	v1.21		Jun 13, 2012 6:05:15
		if (fen64.length == 64 & pos < 64)
		{
			fen64[pos] = p;
			CharSequence s = getFenFromChar(fen64);
			CharSequence tmp[] = ((String) fen).split(" ");
			if (tmp.length == 6)
				nFen = s + " " + tmp[1] + " " + tmp[2] + " " + tmp[3] + " " + tmp[4] + " " + tmp[5];
			if (p == 'P' | p == 'p')
			{
				if (pos < 8 | pos > 55)
					nFen = newFen;
			}
			return nFen;
		}
		else
			return fen;
//		Log.i(TAG, "newFen: " + nFen);
    }
	public CharSequence copyPieceFromFen(CharSequence fen, int fromPos, int toPos) 		
    {	// copy piece from position to position
//		Log.i(TAG, "oldFen: " + fen + " >>> " + fromPos + ", " + toPos);
		CharSequence nFen = "";
		char[] fen64 = getFen64(fen);
		char copyPiece = fen64[fromPos];
		fen64[fromPos] = '-';
		fen64[toPos] = copyPiece;
		CharSequence s = getFenFromChar(fen64);
		CharSequence tmp[] = ((String) fen).split(" ");
		if (tmp.length == 6)
			nFen = s + " " + tmp[1] + " " + tmp[2] + " " + tmp[3] + " " + tmp[4] + " " + tmp[5];
		if (copyPiece == 'P' | copyPiece == 'p')
		{
			if (toPos < 8 | toPos > 55)
				nFen = newFen;
		}
//		Log.i(TAG, "newFen: " + nFen);
		return nFen;
    }
	public CharSequence deletePieceFromFen(CharSequence fen, int pos) 		
    {	// delete a piece from position
		return setPieceToFen(fen, pos, '-');
    }
	public char[] getFen64(CharSequence fen) 		
    {	// changing fen position to 64 characters(return)
		CharSequence nFen = "";
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
		return ((String) nFen).toCharArray();
    }
	public CharSequence getFenFromChar(char[] fen64) 		
    {	// changing fen position to 64 characters(return)
		CharSequence nFen = "";
		int emptyCnt = 0;
		int lineCnt = 0;
		if (fen64.length == 64)
		{
			for (int i = 0; i < fen64.length; i++)
		    {
				if (lineCnt == 8)
				{
					if (emptyCnt != 0)
					{
						nFen = nFen.toString() + emptyCnt;
						emptyCnt = 0;
					}
					nFen = nFen + "/";
					lineCnt = 0;
				}
				if (fen64[i] != '-')
				{
					if (emptyCnt != 0)
					{
						nFen = nFen.toString() + emptyCnt;
						emptyCnt = 0;
					}
					nFen = nFen.toString() + fen64[i];
				}
				else
					emptyCnt++;
				lineCnt++;
		    }
		}
		if (emptyCnt != 0)
			nFen = nFen.toString() + emptyCnt;
		return nFen;
    }
	public boolean checkKing(CharSequence fen) 		
    {	// check FEN: 1 white king, 1 black king, w/b king position >= 3 pieces
		int cntPieces = 0;
		int cntKw = 0;
		int cntKb = 0;
		int posKw = 0;
		int posKb = 0;
		int posDif = 0;
		int rowKw = 0;
		int rowKb = 0;
		int rowDif = 0;
		boolean posOk = true;
		
		char[] fen64 = getFen64(fen);
		if (fen64.length == 64)
		{
			for (int i = 0; i < fen64.length; i++)
		    {
				if (fen64[i] != '-')
					cntPieces++;
				if (fen64[i] == 'K')
				{
					cntKw++;
					posKw = i;
					rowKw = i / 8;
				}
				if (fen64[i] == 'k')
				{
					cntKb++;
					posKb = i;
					rowKb = i / 8;
				}
		    }
		}
		if (posKw > posKb)
			posDif = posKw -posKb;
		else
			posDif = posKb -posKw;
		if (rowKw > rowKb)
			rowDif = rowKw -rowKb;
		if (rowKb > rowKw)
			rowDif = rowKb -rowKw;
		if ((posDif == 1 & rowDif == 0) | rowDif == 1 & (posDif == 7 | posDif == 8 | posDif == 9))
			posOk = false;
		if (cntPieces > 2 & cntKw == 1 & cntKb == 1 & posOk)
			return true;
		else
			return false;
    }
	public boolean checkFen(CharSequence fen) 		
    {
		if (fen.toString().contains("?"))
		{
			messageChessLogic = getString(R.string.cl_fenError) + ": ?";
			return false;
		}
		if (!checkKing(fen))
		{
			messageChessLogic = getString(R.string.cl_wrongBasePosition);
			return false;
		}
    	cl.newPosition("518", fen, "", "", "", "", "", "");
    	if (cl.p_stat.equals("1"))
    		return true;
	  	else
	  	{
	  		messageChessLogic = "";
	  		if (cl.p_mate | cl.p_stalemate)	// mate, steal mate?
	  		{
	  			if (cl.p_mate)
	  				messageChessLogic = messageChessLogic + " (" + getString(R.string.cl_mate) + ")";
				if (cl.p_stalemate)
					messageChessLogic = messageChessLogic + " (" + getString(R.string.cl_stealmate) + ")";
	  		}
	  		else
	  		{
    	  		if (!cl.p_message.equals(""))
    	  			messageChessLogic = cl.p_message;
    	  		else
    	  			messageChessLogic = getString(R.string.editError);
	  		}
	  		return false;
	  	}
    }
	public void showPosibleMoves()
    {	// graphic on chess board -  possible moves 
		CharSequence[] fieldList = {"", "", "", "", "", "", "", ""};
		char[] fen64 = getFen64(newFen);
		int cnt = 0;
		if (fen64.length == 64)
		{
			for (int i = 56; i < fen64.length; i++)
		    {
				if (fen64[i] == '-')
				{
					if (cntB == 1)
					{
						if ((cntBPos + i) % 2 != 0)	// show: both bishops not on different field color
						{
							fieldList[cnt] = chessBoard.getChessField(i, false);
							cnt++;
						}
					}
					else
					{
						fieldList[cnt] = chessBoard.getChessField(i, false);
						cnt++;
					}
				}
		    }
		}
		setDrawValues(-1, 0, true);
    	for (int i = 0; i < fieldList.length; i++) 
    	{
	    	if (fieldList[i].length() == 2)
	    		setDrawValues(chessBoard.getPosition(fieldList[i], false), 1, false);
	    	else
	    		setDrawValues(chessBoard.getPosition(fieldList[i], false), 0, false);
    	}
    }
	public void showChessBoard() 		
    {
//		Log.i(TAG, "showChessBoard, selectedPosition, piece: " + selectedPosition + ", " + piece);
		if (creatingChess960 & cntEmpty == 3)
		{
			showChess960();
			showPosibleMoves();
			creatingChess960 = false;
			chess960Status = 0;
			chess960Id = chess960.createChessPosition(newFen);
			newBoardFen = newFen;
		}
		setPiecesVisible();
		if (creatingChess960)
		{
			EditChessBoard.this.setTitle(getString(R.string.app_editChessBoard) 
					+ " (" + getString(R.string.editChess960) + ")");
			showChess960();
		}
		else
		{
			EditChessBoard.this.setTitle(getString(R.string.app_editChessBoard));
			if (chess960Id != 518)
				EditChessBoard.this.setTitle(getString(R.string.app_editChessBoard) + " (960-ID: " + chess960Id + ")");
			if (newFen.equals(""))
				newFen = currentFen;
		}
		if (chess960Id == 518)
			setFen(newFen);
		else
			message.setText("Chess960-ID: " + chess960Id);
		if (fenColor.equals("w"))
			color.setImageDrawable(getResources().getDrawable(R.drawable.white));
		else
			color.setImageDrawable(getResources().getDrawable(R.drawable.black));
		if (checkFen(newFen))
		{
			btnOk.setVisibility(Button.VISIBLE);
			if (chess960Id == 518)
				setFen(newFen);
			else
				message.setText("Chess960-ID: " + chess960Id);
			positionOk = true;
		}
		else
		{
			btnOk.setVisibility(Button.INVISIBLE);
			message.setText(messageChessLogic);
			positionOk = false;
		}
//		Log.i(TAG, "newFen: " + newFen);
		if (chessBoard == null)
			chessBoard = new ChessBoard(this, newFen, fieldSize, 1);
		chessBoard.getChessBoardFromFen(newFen, false, 1);
		if (creatingChess960)
			showPosibleMoves();
		else
			setDrawValues(selectedPosition, 1, true);
		gridview.setAdapter(chessBoard);
		gridview.invalidate();
    }
	public void setDrawValues(int position, int drawId, boolean initDraw)
	{
		if (initDraw)
			chessBoard.initDrawId();
		if (position < 64 & position >= 0)
			chessBoard.drawId[position] = drawId;
	}
	public void setStringsValues()
	{
    	stringValues.clear();
    	stringValues.add(0, "");
    	stringValues.add(1, getString(R.string.cl_unknownPiece));
    	stringValues.add(2, getString(R.string.cl_wrongBasePosition));
    	stringValues.add(3, getString(R.string.cl_resultWhite));
    	stringValues.add(4, getString(R.string.cl_resultBlack));
    	stringValues.add(5, getString(R.string.cl_resultDraw));
    	stringValues.add(6, getString(R.string.cl_gameOver));
    	stringValues.add(7, getString(R.string.cl_50MoveRule));
    	stringValues.add(8, getString(R.string.cl_position3Times));
    	stringValues.add(9, getString(R.string.cl_moveError));
    	stringValues.add(10, getString(R.string.cl_moveWhite));
    	stringValues.add(11, getString(R.string.cl_moveBlack));
    	stringValues.add(12, getString(R.string.cl_moveMultiple));
    	stringValues.add(13, getString(R.string.cl_moveNo));
    	stringValues.add(14, getString(R.string.cl_moveWrong));
    	stringValues.add(15, getString(R.string.cl_mate));
    	stringValues.add(16, getString(R.string.cl_stealmate));
    	stringValues.add(17, getString(R.string.cl_check));
    	stringValues.add(18, getString(R.string.cl_castlingError));
    	stringValues.add(19, getString(R.string.cl_castlingCheck));
    	stringValues.add(20, getString(R.string.cl_emptyField));
    	stringValues.add(21, getString(R.string.cl_kingError));
    	stringValues.add(22, getString(R.string.cl_fenError));
    	stringValues.add(23, getString(R.string.cl_notationError));
    	stringValues.add(24, getString(R.string.cl_variationError));
    	stringValues.add(25, getString(R.string.cl_checkOponent));
    	stringValues.add(26, getString(R.string.nag_$1));
    	stringValues.add(27, getString(R.string.nag_$2));
    	stringValues.add(28, getString(R.string.nag_$3));
    	stringValues.add(29, getString(R.string.nag_$4));
    	stringValues.add(30, getString(R.string.nag_$5));
    	stringValues.add(31, getString(R.string.nag_$6));
	}

	final String TAG = "EditChessBoard";
	SharedPreferences userP;
	SharedPreferences runP;
	C4aDialog c4aDialog;
	HelpDialog helpDialog;
	ProgressDialog progressDialog = null;
	ChessBoard chessBoard;
	ChessLogic cl;				// direct access to ChessLogic, Chess960, ChessHistory
	Chess960 chess960;

//	subActivities RequestCode
	final static int CHESSBOARD_RECOGNITION_REQUEST_CODE = 101;
    CharSequence messageChessLogic = "";
    public ArrayList<CharSequence> stringValues = new ArrayList<CharSequence>();
	boolean hasWindowTitle = true;
	final static int CHESS960_ID_DIALOG = 1;
	final static int COLOR_DIALOG = 11;
	final static int CASTLING_DIALOG = 12;
	final static int ENPASSANT_DIALOG = 13;
	final static int DRAW50_DIALOG = 14;
	final static int MOVE_COUNTER_DIALOG = 15;
	final static int PROGRESS_DIALOG = 22;
	final static int NO_ACTIVITY_DIALOG = 30;
	final static int HELP_DIALOG = 901;
	int activDialog = 0;
	CharSequence currentFen = "";
	CharSequence standardFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
	CharSequence clearFen = "8/8/8/8/8/8/8/8 w - - 0 1";
	CharSequence newBoardFen = "";
//	graphic on chess board (over GridView)
	Bitmap boardBitmap;						
    Canvas boardCanvas;						
	Paint boardPaint;
	int gridViewSize = 0;
	int downX = 0;
	int downY = 0;
	int upX = 0;
	int upY = 0;
	int wField = 0;
	int fromField = 0;
	int toField = 0;
	int viewAdd = 0;
	int circle1 = 0;
	int circle2 = 0;
	int circle3 = 0;
	int stroke1 = 0;
	int fieldPatch1 = 0;
	int fieldPatch2 = 0;
	
	CharSequence start960Fen = "8/pppppppp/8/8/8/8/PPPPPPPP/8 w - - 0 1";
	boolean creatingChess960 = false;
	boolean positionOk = true;
	int chess960Status = 0;
	int cntBPos = 0;
	int cntB = 0;
	int cntQ = 0;
	int cntN = 0;
	int cntEmpty = 8;
	
	CharSequence newFen = "";
	int chess960Id = 518;
	int fieldSize = 38;
	char piece = ' ';
	int selectedPosition = -1;
	CharSequence fenColor = "w";		// char?
	CharSequence fenCastling = "-";
	CharSequence fenEnPassant = "-";
	CharSequence draw50 = "0";		// int?
	CharSequence moveCounter = "1";	// int?
	Intent returnIntent = new Intent();
	GridView gridview;
	ImageView trash;
	ImageView color;
	ImageView wKing;
	ImageView wQueen;
	ImageView wRook;
	ImageView wBishop;
	ImageView wKnight;
	ImageView wPawn;
	ImageView bKing;
	ImageView bQueen;
	ImageView bRook;
	ImageView bBishop;
	ImageView bKnight;
	ImageView bPawn;
	TextView message;
	ImageView btnOk = null;
	ImageView btnFen = null;
	ImageView btnBoard = null;
//	SurfaceView surfaceView =null;
	
	ImageView btnCamera = null;
	ImageView help;
//	ImageView cbShot;
//	FrameLayout cameraLayout;
}
