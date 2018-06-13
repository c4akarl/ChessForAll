package ccc.chess.gui.chessforall;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Random;

import ccc.chess.logic.c4aservice.Chess960;
import ccc.chess.logic.c4aservice.ChessLogic;
//import android.util.Log;

public class EditChessBoard extends Activity implements Ic4aDialogCallback, DialogInterface.OnCancelListener, OnTouchListener
{
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
        super.onCreate(savedInstanceState);
        u = new Util();
        userPrefs = getSharedPreferences("user", 0);
        runP = getSharedPreferences("run", 0);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
		u.updateFullscreenStatus(this, userPrefs.getBoolean("user_options_gui_StatusBar", true));

		if (u.getAspectRatio(this) > 150)
			setContentView(R.layout.editchessboard);
		else
			setContentView(R.layout.editchessboard150);

        setStringsValues();
        cl = new ChessLogic(stringValues, "");
        chess960 = new Chess960();
        currentFen = getIntent().getExtras().getString("currentFen");
        gridViewSize = getIntent().getExtras().getInt("gridViewSize");
        newFen = initFen(currentFen);
        setFenTo518(newFen);
        fieldSize = getIntent().getExtras().getInt("fieldSize");

        btnOk = (ImageView) findViewById(R.id.btnOk);
        btnFen = (ImageView) findViewById(R.id.btnFen);
        registerForContextMenu(btnFen);
        btnFen.setOnTouchListener((OnTouchListener) this);
        btnBoard = (ImageView) findViewById(R.id.btnBoard);
        registerForContextMenu(btnBoard);
        btnBoard.setOnTouchListener((OnTouchListener) this);
        message = (TextView) findViewById(R.id.message);
        fenMes = (TextView) findViewById(R.id.fenMes);
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
		boardView = (BoardView) findViewById(R.id.editBoardView);
		boardView.setColor();
		boardView.setOnTouchListener(this);
		boardView.updateBoardView(newFen, false, null, null, null,
				userPrefs.getBoolean("user_options_gui_Coordinates", false));
        deleteBackgroundBorder();
        piece = 'P'; 
		wPawn.setBackgroundColor(getResources().getColor(R.color.last_move_to));
        showChessBoard();
	}
	public boolean onTouch(View view, MotionEvent event)									
	{
		if (view.getId() == R.id.btnFen & event.getAction() == MotionEvent.ACTION_UP)
			openContextMenu(btnFen);
		if (view.getId() == R.id.btnBoard & event.getAction() == MotionEvent.ACTION_UP)
			openContextMenu(btnBoard);
		if (view.getId() == R.id.editBoardView)
		{
			int screenXY[] = new int[2];
			boardView.getLocationOnScreen(screenXY);
			int position = boardView.getPositionFromTouch((int) event.getRawX(), (int) event.getRawY(), screenXY[0], screenXY[1]);
//Log.i(TAG, "onTouch(), position: " + position);
			moveAction(position);
		}
		return false;
	}
	public void moveAction(int position)
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
//Log.i(TAG, "position: " + position + ", newFen: " + newFen);
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
	        	break;
	        case R.id.menu_edit_board_current:
	        	newFen = currentFen;
	        	deleteBackgroundBorder();
	            piece = 'P'; 
	        	break;
	        case R.id.menu_edit_board_standard:
	        	newFen = standardFen;
	        	newBoardFen = newFen;
	        	deleteBackgroundBorder();
	            piece = 'P'; 
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
		if (view.getId() != R.id.color)
			deleteBackgroundBorder();
		switch (view.getId()) 
		{
		case R.id.btnOk:
			finishActivity();
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
		case R.id.wKing: 	piece = 'K'; wKing.setBackgroundColor(getResources().getColor(R.color.last_move_to)); break;
		case R.id.wQueen: 	piece = 'Q'; wQueen.setBackgroundColor(getResources().getColor(R.color.last_move_to)); break;
		case R.id.wRook: 	piece = 'R'; wRook.setBackgroundColor(getResources().getColor(R.color.last_move_to)); break;
		case R.id.wBishop:	piece = 'B'; wBishop.setBackgroundColor(getResources().getColor(R.color.last_move_to)); break;
		case R.id.wKnight:	piece = 'N'; wKnight.setBackgroundColor(getResources().getColor(R.color.last_move_to)); break;
		case R.id.wPawn:	piece = 'P'; wPawn.setBackgroundColor(getResources().getColor(R.color.last_move_to)); break;
		case R.id.bKing:	piece = 'k'; bKing.setBackgroundColor(getResources().getColor(R.color.last_move_to)); break;
		case R.id.bQueen:	piece = 'q'; bQueen.setBackgroundColor(getResources().getColor(R.color.last_move_to)); break;
		case R.id.bRook:	piece = 'r'; bRook.setBackgroundColor(getResources().getColor(R.color.last_move_to)); break;
		case R.id.bBishop:	piece = 'b'; bBishop.setBackgroundColor(getResources().getColor(R.color.last_move_to)); break;
		case R.id.bKnight:	piece = 'n'; bKnight.setBackgroundColor(getResources().getColor(R.color.last_move_to)); break;
		case R.id.bPawn:	piece = 'p'; bPawn.setBackgroundColor(getResources().getColor(R.color.last_move_to)); break;
		}
	}

	public void finishActivity()
    {
//Log.i(TAG, "finishActivity(), newFen, chess960Id: " + newFen + ", " + chess960Id);
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
//Log.i(TAG, "pos, piece, fenMes: " + pos + ", " + p + ", " + fenMes );
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
				if (cntB > 1) {cntB = 0; chess960Status = 2;}			// next Q
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
	        	wRook.setVisibility(ImageView.VISIBLE);
	        	wBishop.setVisibility(ImageView.VISIBLE);
	        	wKnight.setVisibility(ImageView.VISIBLE);
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

	public void deleteBackgroundBorder() 		
    {
		wKing.setBackgroundColor(getResources().getColor(R.color.field_color_black));
		wQueen.setBackgroundColor(getResources().getColor(R.color.field_color_black));
		wRook.setBackgroundColor(getResources().getColor(R.color.field_color_black));
		wBishop.setBackgroundColor(getResources().getColor(R.color.field_color_black));
		wKnight.setBackgroundColor(getResources().getColor(R.color.field_color_black));
		wPawn.setBackgroundColor(getResources().getColor(R.color.field_color_black));
		bKing.setBackgroundColor(getResources().getColor(R.color.field_color_black));
		bQueen.setBackgroundColor(getResources().getColor(R.color.field_color_black));
		bRook.setBackgroundColor(getResources().getColor(R.color.field_color_black));
		bBishop.setBackgroundColor(getResources().getColor(R.color.field_color_black));
		bKnight.setBackgroundColor(getResources().getColor(R.color.field_color_black));
		bPawn.setBackgroundColor(getResources().getColor(R.color.field_color_black));

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
			message.setText(getString(R.string.editWhiteMoves));
		else
			message.setText(getString(R.string.editBlackMoves));
		fenMes.setText("FEN: "+ fenColor + " " + fenCastling + " " + fenEnPassant + " " + draw50 + " " + moveCounter);

    }
	public void setFenTo518(CharSequence fen) 		
    {
//Log.i(TAG, "fenMes: " + fenMes);
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
		//err java.lang.ArrayIndexOutOfBoundsException: 6. Mai 10:32 in der App-Version 67
		else
			nFen = start960Fen;
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
    {
//Log.i(TAG, "oldfen: " + fenMes + " >>> " + pos + ", " + p);
		CharSequence nFen = "";
		char[] fen64 = getFen64(fen);
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
    }
	public CharSequence copyPieceFromFen(CharSequence fen, int fromPos, int toPos) 		
    {
//Log.i(TAG, "oldFen: " + fenMes + " >>> " + fromPos + ", " + toPos);
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
		return nFen;
    }

	public char[] getFen64(CharSequence fen)
    {	// changing fenMes position to 64 characters(return)
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
    {	// changing fenMes position to 64 characters(return)
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
							fieldList[cnt] = boardView.getChessField(i, false);
							cnt++;
						}
					}
					else
					{
						fieldList[cnt] = boardView.getChessField(i, false);
						cnt++;
					}
				}
		    }
		}
    }
	public void showChessBoard() 		
    {
//Log.i(TAG, "showChessBoard, selectedPosition, piece: " + selectedPosition + ", " + piece);
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
			showChess960();
		else
		{
			if (newFen.equals(""))
				newFen = currentFen;
		}
		fenMes.setText("");
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
//Log.i(TAG, "newFen: " + newFen);
		if (creatingChess960)
			showPosibleMoves();
		boardView.updateBoardView(newFen, false, null, null, null,
				userPrefs.getBoolean("user_options_gui_Coordinates", false));
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
    	stringValues.add(26, getString(R.string.nag_1));
    	stringValues.add(27, getString(R.string.nag_2));
    	stringValues.add(28, getString(R.string.nag_3));
    	stringValues.add(29, getString(R.string.nag_4));
    	stringValues.add(30, getString(R.string.nag_5));
    	stringValues.add(31, getString(R.string.nag_6));
	}

	final String TAG = "EditChessBoard";
	Util u;
	SharedPreferences userPrefs;
	SharedPreferences runP;
	C4aDialog c4aDialog;
	ProgressDialog progressDialog = null;
	ChessLogic cl;				// direct access to ChessLogic, Chess960, ChessHistory
	Chess960 chess960;

    CharSequence messageChessLogic = "";
    public ArrayList<CharSequence> stringValues = new ArrayList<CharSequence>();
	final static int CHESS960_ID_DIALOG = 1;
	final static int COLOR_DIALOG = 11;
	final static int CASTLING_DIALOG = 12;
	final static int ENPASSANT_DIALOG = 13;
	final static int DRAW50_DIALOG = 14;
	final static int MOVE_COUNTER_DIALOG = 15;
	final static int PROGRESS_DIALOG = 22;
	int activDialog = 0;
	CharSequence currentFen = "";
	CharSequence standardFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
	CharSequence clearFen = "8/8/8/8/8/8/8/8 w - - 0 1";
	CharSequence newBoardFen = "";
	int gridViewSize = 0;

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
	CharSequence fenColor = "w";
	CharSequence fenCastling = "-";
	CharSequence fenEnPassant = "-";
	CharSequence draw50 = "0";
	CharSequence moveCounter = "1";
	Intent returnIntent = new Intent();
	BoardView boardView;
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
	TextView fenMes;
	ImageView btnOk = null;
	ImageView btnFen = null;
	ImageView btnBoard = null;

}
