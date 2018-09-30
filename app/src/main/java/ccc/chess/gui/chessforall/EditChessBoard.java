package ccc.chess.gui.chessforall;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.text.ClipboardManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Random;

import ccc.chess.logic.c4aservice.Chess960;
import ccc.chess.logic.c4aservice.ChessLogic;

public class EditChessBoard extends Activity implements Ic4aDialogCallback, DialogInterface.OnCancelListener, OnTouchListener
{
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
//Log.i(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        u = new Util();
        userPrefs = getSharedPreferences("user", 0);
        initColors();
        runP = getSharedPreferences("run", 0);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
		u.updateFullscreenStatus(this, userPrefs.getBoolean("user_options_gui_StatusBar", false));

		setGui();

	}

	public void setGui()
	{
		setContentView(R.layout.editchessboard);

		setStringsValues();
		cl = new ChessLogic(stringValues, "");
		chess960 = new Chess960();

		if (isStartActivity)
		{
			currentFen = getIntent().getExtras().getString("currentFen");
			gridViewSize = getIntent().getExtras().getInt("gridViewSize");
			newFen = initFen(currentFen);
			isStartActivity = false;
		}
//Log.i(TAG, "setGui(), currentFen: " + currentFen);
//Log.i(TAG, "setGui(), newFen: " + newFen + ", chess960Id: " + chess960Id);
		fieldSize = getIntent().getExtras().getInt("fieldSize");

		btnCancel = (TextView) findViewById(R.id.btnCancel);
		btnOk = (TextView) findViewById(R.id.btnOk);
		title = (TextView) findViewById(R.id.title);
		message = (TextView) findViewById(R.id.message);

		wKing = (ImageView) findViewById(R.id.wKing);
		wQueen = (ImageView) findViewById(R.id.wQueen);
		wRook = (ImageView) findViewById(R.id.wRook);
		wBishop = (ImageView) findViewById(R.id.wBishop);
		wKnight = (ImageView) findViewById(R.id.wKnight);
		wPawn = (ImageView) findViewById(R.id.wPawn);
		trash = (ImageView) findViewById(R.id.trash);
		turnBoard = (ImageView) findViewById(R.id.turnBoard);

		bKing = (ImageView) findViewById(R.id.bKing);
		bQueen = (ImageView) findViewById(R.id.bQueen);
		bRook = (ImageView) findViewById(R.id.bRook);
		bBishop = (ImageView) findViewById(R.id.bBishop);
		bKnight = (ImageView) findViewById(R.id.bKnight);
		bPawn = (ImageView) findViewById(R.id.bPawn);
		color = (ImageView) findViewById(R.id.color);
		options = (ImageView) findViewById(R.id.options);

		boardView = (BoardView) findViewById(R.id.editBoardView);
		boardView.setColor();
		boardView.setOnTouchListener(this);
		userPrefs.getBoolean("user_options_gui_Coordinates", false);
		piece = 'P';
		if (runP.getString("run_piece", "P").length() > 0)
			piece = runP.getString("run_piece", "P").charAt(0);
		setPieces(piece);
		showChessBoard();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig)
	{
		super.onConfigurationChanged(newConfig);
//Log.i(TAG, "onConfigurationChanged()");
		setGui();
	}

	public boolean onTouch(View view, MotionEvent event)
	{
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

	public void myClickHandler(View view)
    {	// ClickHandler	(ButtonEvents)
		if (view.getId() != R.id.color & view.getId() != R.id.turnBoard & view.getId() != R.id.options)
			setPieces(piece);
		switch (view.getId())
		{
		case R.id.btnCancel:
			finish();
			break;
		case R.id.btnOk:
			finishActivity();
			break;
		case R.id.trash:
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
			break;
		case R.id.turnBoard:
			SharedPreferences.Editor ed = runP.edit();
			if (runP.getBoolean("run_game0_is_board_turn", false))
				ed.putBoolean("run_game0_is_board_turn", false);
			else
				ed.putBoolean("run_game0_is_board_turn", true);
            ed.commit();
			break;
		case R.id.options:
			removeDialog(OPTIONS_DIALOG);
			showDialog(OPTIONS_DIALOG);
			break;
		case R.id.wKing: 	piece = 'K'; break;
		case R.id.wQueen: 	piece = 'Q'; break;
		case R.id.wRook: 	piece = 'R'; break;
		case R.id.wBishop:	piece = 'B'; break;
		case R.id.wKnight:	piece = 'N'; break;
		case R.id.wPawn:	piece = 'P'; break;
		case R.id.bKing:	piece = 'k'; break;
		case R.id.bQueen:	piece = 'q'; break;
		case R.id.bRook:	piece = 'r'; break;
		case R.id.bBishop:	piece = 'b'; break;
		case R.id.bKnight:	piece = 'n'; break;
		case R.id.bPawn:	piece = 'p'; break;
		}
		if (!creatingChess960)
		{
			SharedPreferences.Editor ed = runP.edit();
			ed.putString("run_piece", Character.toString(piece));
			ed.commit();
		}

		showChessBoard();
	}

	public void finishActivity()
    {
//Log.i(TAG, "finishActivity(), newFen, chess960Id: " + newFen + ", " + chess960Id);
		returnIntent.putExtra("newFen", newFen);
		returnIntent.putExtra("chess960Id", Integer.toString(chess960Id));
		setResult(RESULT_OK, returnIntent);
		finish();
    }
	@SuppressLint("ClickableViewAccessibility")
    public Dialog onCreateDialog(int id)
	{
		activDialog = id;
		if (id == OPTIONS_DIALOG)
		{
			Dialog dialog = new Dialog(this);
			dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			dialog.setContentView(R.layout.dialog_edit_board_options);

			MyViewListener myViewListener = new MyViewListener();
//Log.i(TAG, "onCreateDialog(), newFen: " + newFen + ", chess960Id: " + chess960Id);
			getCastleValuesFromFen(newFen);
			getValuesFromFen(newFen);

			d_whiteKingCastle = dialog.findViewById(R.id.whiteKingCastle);
			d_whiteKingCastle.setChecked(isWhiteKingCastle);
			d_whiteKingCastle.setOnClickListener(myViewListener);
			d_whiteQueenCastle = dialog.findViewById(R.id.whiteQueenCastle);
			d_whiteQueenCastle.setChecked(isWhiteQueenCastle);
			d_whiteQueenCastle.setOnClickListener(myViewListener);
			d_blackKingCastle = dialog.findViewById(R.id.blackKingCastle);
			d_blackKingCastle.setChecked(isBlackKingCastle);
			d_blackKingCastle.setOnClickListener(myViewListener);
			d_blackQueenCastle = dialog.findViewById(R.id.blackQueenCastle);
			d_blackQueenCastle.setChecked(isBlackQueenCastle);
			d_blackQueenCastle.setOnClickListener(myViewListener);
			d_en_passant_item = dialog.findViewById(R.id.en_passant_item);
			d_en_passant_item.setText(en_passant);
			d_en_passant_left = dialog.findViewById(R.id.en_passant_left);
            d_en_passant_left.setOnTouchListener(new OnTouchListener()
            {
                @Override
                public boolean onTouch(View v, MotionEvent event)
                {
                    switch(event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            pressedUp = true;
                            pressedLeft = true;
                            isUpdated = false;
                            handlerEnPassant.removeCallbacks(mUpdateEnPassant);
                            handlerEnPassant.postDelayed(mUpdateEnPassant, HANDLER_DELAY_START);
                            break;
                        case MotionEvent.ACTION_UP:
                            pressedUp = false;
                            if (!isUpdated)
                                setEnPassant(pressedLeft);
                            break;
                    }
                    return true;
                }
            });
            d_en_passant_right = dialog.findViewById(R.id.en_passant_right);
            d_en_passant_right.setOnTouchListener(new OnTouchListener()
            {
                @Override
                public boolean onTouch(View v, MotionEvent event)
                {
                    switch(event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            pressedUp = true;
                            pressedLeft = false;
                            isUpdated = false;
                            handlerEnPassant.removeCallbacks(mUpdateEnPassant);
                            handlerEnPassant.postDelayed(mUpdateEnPassant, HANDLER_DELAY_START);
                            break;
                        case MotionEvent.ACTION_UP:
                            pressedUp = false;
                            if (!isUpdated)
                                setEnPassant(pressedLeft);
                            break;
                    }
                    return true;
                }
            });
			d_draw50_item = dialog.findViewById(R.id.draw50_item);
			d_draw50_item.setText("" + draw50);
            d_draw50_left = dialog.findViewById(R.id.draw50_left);
            d_draw50_left.setOnTouchListener(new OnTouchListener()
            {
                @Override
                public boolean onTouch(View v, MotionEvent event)
                {
                    switch(event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            pressedUp = true;
                            pressedLeft = true;
                            isUpdated = false;
                            handlerDraw50.removeCallbacks(mUpdateDraw50);
                            handlerDraw50.postDelayed(mUpdateDraw50, HANDLER_DELAY_START);
                            break;
                        case MotionEvent.ACTION_UP:
                            pressedUp = false;
                            if (!isUpdated)
                                setDraw50(pressedLeft);
                            break;
                    }
                    return true;
                }
            });
            d_draw50_right = dialog.findViewById(R.id.draw50_right);
            d_draw50_right.setOnTouchListener(new OnTouchListener()
            {
                @Override
                public boolean onTouch(View v, MotionEvent event)
                {
                    switch(event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            pressedUp = true;
                            pressedLeft = false;
                            isUpdated = false;
                            handlerDraw50.removeCallbacks(mUpdateDraw50);
                            handlerDraw50.postDelayed(mUpdateDraw50, HANDLER_DELAY_START);
                            break;
                        case MotionEvent.ACTION_UP:
                            pressedUp = false;
                            if (!isUpdated)
                                setDraw50(pressedLeft);
                            break;
                    }
                    return true;
                }
            });
			d_move_counter_item = dialog.findViewById(R.id.move_counter_item);
			d_move_counter_item.setText("" + move_counter);
            d_move_counter_left = dialog.findViewById(R.id.move_counter_left);
			d_move_counter_left.setOnTouchListener(new OnTouchListener()
			{
				@Override
				public boolean onTouch(View v, MotionEvent event)
				{
					switch(event.getAction()) {
						case MotionEvent.ACTION_DOWN:
							pressedUp = true;
							pressedLeft = true;
							isUpdated = false;
							handlerMoveCounter.removeCallbacks(mUpdateMoveCounter);
							handlerMoveCounter.postDelayed(mUpdateMoveCounter, HANDLER_DELAY_START);
							break;
						case MotionEvent.ACTION_UP:
							pressedUp = false;
							if (!isUpdated)
								setMoveCounter(pressedLeft);
							break;
					}
					return true;
				}
			});
            d_move_counter_right = dialog.findViewById(R.id.move_counter_right);
			d_move_counter_right.setOnTouchListener(new OnTouchListener()
			{
				@Override
				public boolean onTouch(View v, MotionEvent event)
                {
					switch(event.getAction()) {
						case MotionEvent.ACTION_DOWN:
							pressedUp = true;
							pressedLeft = false;
							isUpdated = false;
							handlerMoveCounter.removeCallbacks(mUpdateMoveCounter);
							handlerMoveCounter.postDelayed(mUpdateMoveCounter, HANDLER_DELAY_START);
							break;
						case MotionEvent.ACTION_UP:
							pressedUp = false;
							if (!isUpdated)
								setMoveCounter(pressedLeft);
							break;
					}
					return true;
				}
			});
            d_chess960_manual = dialog.findViewById(R.id.chess960_manual);
            d_chess960_manual.setOnClickListener(myViewListener);
            d_chess960_random = dialog.findViewById(R.id.chess960_random);
            d_chess960_random.setOnClickListener(myViewListener);
            d_chess960_number = dialog.findViewById(R.id.chess960_number);
            d_chess960_number.setOnClickListener(myViewListener);
			d_to_clipboard = dialog.findViewById(R.id.to_clipboard);
			d_to_clipboard.setOnClickListener(myViewListener);
			d_from_clipboard = dialog.findViewById(R.id.from_clipboard);
			d_from_clipboard.setOnClickListener(myViewListener);
			d_standard = dialog.findViewById(R.id.standard);
			d_standard.setOnClickListener(myViewListener);
			d_current = dialog.findViewById(R.id.current);
			d_current.setOnClickListener(myViewListener);
			d_clear = dialog.findViewById(R.id.clear);
			d_clear.setOnClickListener(myViewListener);

			setEnPassantImages();
			setDraw50Images();
			setMoveCounterImages();

			return dialog;

		}

		if (id == CHESS960_ID_DIALOG)  
        {
			c4aDialog = new C4aDialog(this, this, getString(R.string.dgTitleDialog), 
				"", getString(R.string.btn_Ok), "", getString(R.string.inputDialog_chess960Id), 2, "");
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
			piece = 'P';
			if (runP.getString("run_piece", "P").length() > 0)
				piece = runP.getString("run_piece", "P").charAt(0);
			setPieces(piece);
		}
		else
		{
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

	public void setPieces(char pieceSelected)
	{
		turnBoard.setBackgroundResource(R.drawable.turn_board);
		turnBoard.setBackgroundColor(cv.getColor(cv.COLOR_FIELD_DARK_2));
		color.setBackgroundResource(R.drawable.turn_board);
		color.setBackgroundColor(cv.getColor(cv.COLOR_FIELD_DARK_2));
		options.setBackgroundResource(R.drawable.setting);
		options.setBackgroundColor(cv.getColor(cv.COLOR_FIELD_LIGHT_1));

		setImageView();

		switch (pieceSelected)
		{
			case 'K': wKing = setBorderToImageView(wKing);  	break;
			case 'Q': wQueen = setBorderToImageView(wQueen);  	break;
			case 'R': wRook = setBorderToImageView(wRook);  	break;
			case 'B': wBishop = setBorderToImageView(wBishop);  break;
			case 'N': wKnight = setBorderToImageView(wKnight);  break;
			case 'P': wPawn = setBorderToImageView(wPawn);  	break;
			case 'k': bKing = setBorderToImageView(bKing);  	break;
			case 'q': bQueen = setBorderToImageView(bQueen);  	break;
			case 'r': bRook = setBorderToImageView(bRook);  	break;
			case 'b': bBishop = setBorderToImageView(bBishop);  break;
			case 'n': bKnight = setBorderToImageView(bKnight);  break;
			case 'p': bPawn = setBorderToImageView(bPawn);  	break;
			case ' ': trash = setBorderToImageView(trash);  	break;
		}

	}

	public void setImageView()
	{
		Canvas canvas;
		if (wKingBmap == null)
		{
			wKingBmap = BitmapFactory.decodeResource(getResources(), R.drawable._1_wk).copy(Bitmap.Config.ARGB_8888, true);
			canvas = new Canvas();
			canvas.setBitmap(wKingBmap);
		}
		wKing.setImageBitmap(wKingBmap);
		wKing.setBackgroundColor(cv.getColor(cv.COLOR_FIELD_LIGHT_1));

		if (wQueenBmap == null)
		{
			wQueenBmap = BitmapFactory.decodeResource(getResources(), R.drawable._1_wq).copy(Bitmap.Config.ARGB_8888, true);
			canvas = new Canvas();
			canvas.setBitmap(wQueenBmap);
		}
		wQueen.setImageBitmap(wQueenBmap);
		wQueen.setBackgroundColor(cv.getColor(cv.COLOR_FIELD_DARK_2));

		if (wRookBmap == null)
		{
			wRookBmap = BitmapFactory.decodeResource(getResources(), R.drawable._1_wr).copy(Bitmap.Config.ARGB_8888, true);
			canvas = new Canvas();
			canvas.setBitmap(wRookBmap);
		}
		wRook.setImageBitmap(wRookBmap);
		wRook.setBackgroundColor(cv.getColor(cv.COLOR_FIELD_LIGHT_1));

		if (wBishopBmap == null)
		{
			wBishopBmap = BitmapFactory.decodeResource(getResources(), R.drawable._1_wb).copy(Bitmap.Config.ARGB_8888, true);
			canvas = new Canvas();
			canvas.setBitmap(wBishopBmap);
		}
		wBishop.setImageBitmap(wBishopBmap);
		wBishop.setBackgroundColor(cv.getColor(cv.COLOR_FIELD_DARK_2));

		if (wKnightBmap == null)
		{
			wKnightBmap = BitmapFactory.decodeResource(getResources(), R.drawable._1_wn).copy(Bitmap.Config.ARGB_8888, true);
			canvas = new Canvas();
			canvas.setBitmap(wKnightBmap);
		}
		wKnight.setImageBitmap(wKnightBmap);
		wKnight.setBackgroundColor(cv.getColor(cv.COLOR_FIELD_LIGHT_1));

		if (wPawnBmap == null)
		{
			wPawnBmap = BitmapFactory.decodeResource(getResources(), R.drawable._1_wp).copy(Bitmap.Config.ARGB_8888, true);
			canvas = new Canvas();
			canvas.setBitmap(wPawnBmap);
		}
		wPawn.setImageBitmap(wPawnBmap);
		wPawn.setBackgroundColor(cv.getColor(cv.COLOR_FIELD_DARK_2));

		if (bKingBmap == null)
		{
			bKingBmap = BitmapFactory.decodeResource(getResources(), R.drawable._1_bk).copy(Bitmap.Config.ARGB_8888, true);
			canvas = new Canvas();
			canvas.setBitmap(bKingBmap);
		}
		bKing.setImageBitmap(bKingBmap);
		bKing.setBackgroundColor(cv.getColor(cv.COLOR_FIELD_DARK_2));

		if (bQueenBmap == null)
		{
			bQueenBmap = BitmapFactory.decodeResource(getResources(), R.drawable._1_bq).copy(Bitmap.Config.ARGB_8888, true);
			canvas = new Canvas();
			canvas.setBitmap(bQueenBmap);
		}
		bQueen.setImageBitmap(bQueenBmap);
		bQueen.setBackgroundColor(cv.getColor(cv.COLOR_FIELD_LIGHT_1));

		if (bRookBmap == null)
		{
			bRookBmap = BitmapFactory.decodeResource(getResources(), R.drawable._1_br).copy(Bitmap.Config.ARGB_8888, true);
			canvas = new Canvas();
			canvas.setBitmap(bRookBmap);
		}
		bRook.setImageBitmap(bRookBmap);
		bRook.setBackgroundColor(cv.getColor(cv.COLOR_FIELD_DARK_2));

		if (bBishopBmap == null)
		{
			bBishopBmap = BitmapFactory.decodeResource(getResources(), R.drawable._1_bb).copy(Bitmap.Config.ARGB_8888, true);
			canvas = new Canvas();
			canvas.setBitmap(bBishopBmap);
		}
		bBishop.setImageBitmap(bBishopBmap);
		bBishop.setBackgroundColor(cv.getColor(cv.COLOR_FIELD_LIGHT_1));

		if (bKnightBmap == null)
		{
			bKnightBmap = BitmapFactory.decodeResource(getResources(), R.drawable._1_bn).copy(Bitmap.Config.ARGB_8888, true);
			canvas = new Canvas();
			canvas.setBitmap(bKnightBmap);
		}
		bKnight.setImageBitmap(bKnightBmap);
		bKnight.setBackgroundColor(cv.getColor(cv.COLOR_FIELD_DARK_2));

		if (bPawnBmap == null)
		{
			bPawnBmap = BitmapFactory.decodeResource(getResources(), R.drawable._1_bp).copy(Bitmap.Config.ARGB_8888, true);
			canvas = new Canvas();
			canvas.setBitmap(bPawnBmap);
		}
		bPawn.setImageBitmap(bPawnBmap);
		bPawn.setBackgroundColor(cv.getColor(cv.COLOR_FIELD_LIGHT_1));

		if (trashBmap == null)
		{
			trashBmap = BitmapFactory.decodeResource(getResources(), R.drawable.trash).copy(Bitmap.Config.ARGB_8888, true);
			canvas = new Canvas();
			canvas.setBitmap(trashBmap);
		}
		trash.setImageBitmap(trashBmap);
		trash.setBackgroundColor(cv.getColor(cv.COLOR_FIELD_LIGHT_1));
	}

	public ImageView setBorderToImageView(ImageView imageView)
	{
		if (imageView == null)
			return imageView;
		imageView.setDrawingCacheEnabled(true);
		imageView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
				View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
		imageView.layout(0, 0, imageView.getMeasuredWidth(), imageView.getMeasuredHeight());
		Bitmap bmap = imageView.getDrawingCache();
		if (bmap == null)
			return imageView;
		int stroke = Math.min(bmap.getWidth(), bmap.getHeight()) / 6;
		Canvas canvas = new Canvas(bmap);
		final Paint paint = new Paint();
		final Rect rect = new Rect(0, 0, bmap.getWidth(), bmap.getHeight());
		paint.setColor(cv.getColor(cv.COLOR_FIELD_TO_6));
		paint.setStyle(Paint.Style.STROKE);
		paint.setStrokeWidth(stroke);
		canvas.drawRect(rect, paint);
		imageView.setImageBitmap(bmap);
		return imageView;
	}

	public void setFen(CharSequence fen)
    {
		CharSequence tmp[] = ((String) fen).split(" ");
		if (tmp.length == 6)
		{
			fenColor = tmp[1];
			fenCastling = tmp[2];
			fenEnPassant = tmp[3];
			fenDraw50 = tmp[4];
			fenMoveCounter = tmp[5];
		}
		CharSequence mes = "";
		if (fenColor.equals("w"))
			message.setText(getString(R.string.editWhiteMoves));
		else
			message.setText(getString(R.string.editBlackMoves));

    }

	public CharSequence initFen(CharSequence fen)
    {	// FEN: initialize move counter
		CharSequence nFen = "";
		CharSequence tmp[] = ((String) fen).split(" ");
		if (tmp.length == 6)
			nFen = tmp[0] + " " + tmp[1] + " " + tmp[2] + " " + tmp[3] + " " + tmp[4] + " " + tmp[5];
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
		if (runP.getBoolean("run_game0_is_board_turn", false))
		{
//Log.i(TAG, "setPieceToFen(), p: "+ p + ", pos: " + pos + ", turnPos: " + turnPos);
			pos = 63 - pos;
		}
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
//Log.i(TAG, "checkKing(), cntPieces: " + cntPieces + ", cntKw: " + cntKw + ", cntKb: " + cntKb + ", posOk: " + posOk);
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
//Log.i(TAG, "showChessBoard(), board_turn: " + runP.getBoolean("run_game0_is_board_turn", false));
		boardView.updateBoardView(newFen, runP.getBoolean("run_game0_is_board_turn", false), null, null, null,
				true);
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

    public void initColors()
    {
        cv = new ColorValues();
        int colorId = userPrefs.getInt("colorId", 0);
        switch (colorId)
        {
            case 0:
                cv.setColors(colorId, userPrefs.getString("colors_0", ""));
                break;
            case 1:
                cv.setColors(colorId, userPrefs.getString("colors_1", ""));
                break;
            case 2:
                cv.setColors(colorId, userPrefs.getString("colors_2", ""));
                break;
            case 3:
                cv.setColors(colorId, userPrefs.getString("colors_3", ""));
                break;
            case 4:
                cv.setColors(colorId, userPrefs.getString("colors_4", ""));
                break;
        }
    }

	public void setToClipboard(CharSequence text)
	{
		Toast.makeText(this, getString(R.string.menu_info_clipboardCopyPgn), Toast.LENGTH_SHORT).show();
		ClipboardManager cm = (ClipboardManager)getSystemService(Context.CLIPBOARD_SERVICE);
		cm.setText(text);
	}

	public CharSequence getFenFromClipboard()
	{
		CharSequence fen = "";
		CharSequence pgnData = "";
		try
		{
			Toast.makeText(this, getString(R.string.menu_info_clipboardPaste), Toast.LENGTH_SHORT).show();
			ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
			pgnData = (String) cm.getText();
		}
		catch (ClassCastException e)
		{
			return "";
		}
		if (pgnData == null)
			return "";
		CharSequence[] pgnSplit = pgnData.toString().split(" ");
		if (pgnSplit.length > 0)
		{
			if (pgnSplit[0].toString().contains("/"))
			{
				if (pgnSplit.length == 6)
				{
					pgnSplit[4] = "0";
					pgnSplit[5] = "1";
					fen = pgnSplit[0] + " " + pgnSplit[1] + " " + pgnSplit[2] + " " + pgnSplit[3] + " " + pgnSplit[4] + " " + pgnSplit[5];
					return fen;
				}
				else
					return "";
			}
		}
		return "";
	}

	public void getCastleValuesFromFen(CharSequence fen)
	{
		if (!fen.toString().equals(""))
		{
			CharSequence tmp[] = ((String) fen).split(" ");
			if (tmp.length == 6)
			{
				isWhiteKingCastle = false;
				isWhiteQueenCastle = false;
				isBlackKingCastle = false;
				isBlackQueenCastle = false;
				if (tmp[2].length() >= 0)
				{
					for (int i = 0; i < tmp[2].length(); i++)
					{
						if (tmp[2].toString().charAt(i) == 'K')	isWhiteKingCastle = true;
						if (tmp[2].toString().charAt(i) == 'Q')	isWhiteQueenCastle = true;
						if (tmp[2].toString().charAt(i) == 'k')	isBlackKingCastle = true;
						if (tmp[2].toString().charAt(i) == 'q')	isBlackQueenCastle = true;
					}
				}
			}
		}
	}

	public void getValuesFromFen(CharSequence fen)
	{
		if (!fen.toString().equals(""))
		{
			String tmp[] = ((String) fen).split(" ");
			if (tmp.length == 6)
			{
				en_passant = "-";
				if (tmp[3].length() == 2)
				{
					for (int i = 0; i < EN_PASSANT_VALUES.length(); i++)
					{
						if (tmp[3].charAt(0) == EN_PASSANT_VALUES.charAt(i))
							en_passant = "" + tmp[3].charAt(0);
					}
				}
				try 	{ draw50 = Integer.parseInt(tmp[4]); }
				catch 	(NumberFormatException exception) { draw50 = DRAW50_MIN; }
				try 	{ move_counter = Integer.parseInt(tmp[5]); }
				catch 	(NumberFormatException exception) { move_counter = MOVE_COUNTER_MIN; }
//Log.i(TAG, "getValuesFromFen(), fen: " + fen);
//Log.i(TAG, "getValuesFromFen(), en_passant: " + en_passant + ", draw50: " + draw50 + ", move_counter: " + move_counter);
			}
		}
	}

	public void setCastleValuesToFen()
	{
		if (!newFen.toString().equals(""))
		{
			CharSequence tmp[] = ((String) newFen).split(" ");
			if (tmp.length == 6)
			{
				CharSequence newCastle = "";
				if (isWhiteKingCastle) newCastle = newCastle + "K";
				if (isWhiteQueenCastle) newCastle = newCastle + "Q";
				if (isBlackKingCastle) newCastle = newCastle + "k";
				if (isBlackQueenCastle) newCastle = newCastle + "q";
				if (newCastle.toString().equals(""))
					newCastle = "-";
				newFen = tmp[0] + " " + tmp[1] + " " + newCastle + " " + tmp[3] + " " + tmp[4] + " " + tmp[5];
			}
		}
	}

	public void setEnPassant(boolean isLeft)
	{
		int epIdx = 0;
		for (int i = 0; i < EN_PASSANT_VALUES.length(); i++)
		{
			if (EN_PASSANT_VALUES.charAt(i) == en_passant.charAt(0))
				epIdx = i;
		}
		if (isLeft)
		{
			if (epIdx > 0)
				epIdx--;
		}
		else
		{
			if (epIdx <8)
				epIdx++;
		}
		en_passant = "" + EN_PASSANT_VALUES.charAt(epIdx);
		if (!newFen.toString().equals(""))
		{
			CharSequence tmp[] = ((String) newFen).split(" ");
			if (tmp.length == 6)
			{
				String newEp = en_passant + "6";
				if (tmp[1].equals("b"))
					newEp = en_passant + "3";
				newFen = tmp[0] + " " + tmp[1] + " " + tmp[2] + " " + newEp + " " + tmp[4] + " " + tmp[5];
			}
		}
		d_en_passant_item.setText(en_passant);
		setEnPassantImages();
	}

	public void setEnPassantImages()
	{
		d_en_passant_left.setImageResource(R.drawable.arrow_left_white);
		d_en_passant_right.setImageResource(R.drawable.arrow_right_white);
		if (EN_PASSANT_VALUES.charAt(0) == en_passant.charAt(0))
		{
			d_en_passant_left.setImageResource(R.drawable.arrow_left_grey);
			pressedUp = false;
		}
		if (EN_PASSANT_VALUES.charAt(8) == en_passant.charAt(0))
		{
			d_en_passant_right.setImageResource(R.drawable.arrow_right_grey);
			pressedUp = false;
		}
	}

	public void setDraw50(boolean isLeft)
	{
		if (isLeft)
		{
			if (draw50 > DRAW50_MIN)
				draw50--;
		}
		else
		{
			if (draw50 < DRAW50_MAX)
				draw50++;
		}
		if (!newFen.toString().equals(""))
		{
			CharSequence tmp[] = ((String) newFen).split(" ");
			if (tmp.length == 6)
			{
				newFen = tmp[0] + " " + tmp[1] + " " + tmp[2] + " " + tmp[3] + " " + draw50 + " " + tmp[5];
			}
		}
		d_draw50_item.setText("" + draw50);
		setDraw50Images();
	}

	public void setDraw50Images()
	{
		d_draw50_left.setImageResource(R.drawable.arrow_left_white);
		d_draw50_right.setImageResource(R.drawable.arrow_right_white);
		if (draw50 <= DRAW50_MIN)
		{
			d_draw50_left.setImageResource(R.drawable.arrow_left_grey);
			pressedUp = false;
		}
		if (draw50 >= DRAW50_MAX)
		{
			d_draw50_right.setImageResource(R.drawable.arrow_right_grey);
			pressedUp = false;
		}
	}

	public void setMoveCounter(boolean isLeft)
	{
//Log.i(TAG, "setMoveCounter(), isLeft: " + isLeft + ", move_counter: " + move_counter);
		if (isLeft)
		{
			if (move_counter > MOVE_COUNTER_MIN)
				move_counter--;
		}
		else
		{
			if (move_counter < MOVE_COUNTER_MAX)
				move_counter++;
		}
		if (!newFen.toString().equals(""))
		{
			CharSequence tmp[] = ((String) newFen).split(" ");
			if (tmp.length == 6)
			{
				newFen = tmp[0] + " " + tmp[1] + " " + tmp[2] + " " + tmp[3] + " " + tmp[4] + " " + move_counter;
			}
		}
		d_move_counter_item.setText("" + move_counter);
		setMoveCounterImages();
	}

	public void setMoveCounterImages()
	{
		d_move_counter_left.setImageResource(R.drawable.arrow_left_white);
		d_move_counter_right.setImageResource(R.drawable.arrow_right_white);
		if (move_counter <= MOVE_COUNTER_MIN)
		{
			d_move_counter_left.setImageResource(R.drawable.arrow_left_grey);
			pressedUp = false;
		}
		if (move_counter >= MOVE_COUNTER_MAX)
		{
			d_move_counter_right.setImageResource(R.drawable.arrow_right_grey);
			pressedUp = false;
		}
	}

    public class MyViewListener implements View.OnClickListener
    {
        public void onClick(View v)
        {
			chess960Id = 518;
            switch (v.getId())
            {
                case R.id.whiteKingCastle:
					isWhiteKingCastle = ((CheckBox)v).isChecked();
					setCastleValuesToFen();
                    break;
                case R.id.whiteQueenCastle:
					isWhiteQueenCastle = ((CheckBox)v).isChecked();
					setCastleValuesToFen();
                    break;
                case R.id.blackKingCastle:
					isBlackKingCastle = ((CheckBox)v).isChecked();
					setCastleValuesToFen();
                    break;
                case R.id.blackQueenCastle:
					isBlackQueenCastle = ((CheckBox)v).isChecked();
					setCastleValuesToFen();
                    break;
                case R.id.chess960_manual:
					if (runP.getBoolean("run_game0_is_board_turn", false))
					{
						SharedPreferences.Editor ed = runP.edit();
						ed.putBoolean("run_game0_is_board_turn", false);
						ed.commit();
					}
					initChess960();
					removeDialog(OPTIONS_DIALOG);
                    break;
                case R.id.chess960_random:
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
					removeDialog(OPTIONS_DIALOG);
                    break;
                case R.id.chess960_number:
					removeDialog(CHESS960_ID_DIALOG);
					showDialog(CHESS960_ID_DIALOG);
					removeDialog(OPTIONS_DIALOG);
                    break;
                case R.id.to_clipboard:
					if (checkFen(newFen))
						setToClipboard(newFen);
					removeDialog(OPTIONS_DIALOG);
                    break;
                case R.id.from_clipboard:
					CharSequence fenC = getFenFromClipboard();
					if (!fenC.toString().equals(""))
						newFen = fenC;
					removeDialog(OPTIONS_DIALOG);
                    break;
                case R.id.standard:
					newFen = standardFen;
					newBoardFen = newFen;
					removeDialog(OPTIONS_DIALOG);
					break;
                case R.id.current:
					newFen = currentFen;
					removeDialog(OPTIONS_DIALOG);
					break;
                case R.id.clear:
					newFen = clearFen;
					removeDialog(OPTIONS_DIALOG);
                    break;
            }
            if (v.getId() != R.id.chess960_manual)
			{
				creatingChess960 = false;
				chess960Status = 0;
			}
			showChessBoard();
        }
    }

    public Runnable mUpdateEnPassant = new Runnable()
    {
        public void run()
        {
            if (pressedUp)
            {
                setEnPassant(pressedLeft);
                isUpdated = true;
                handlerEnPassant.postDelayed(mUpdateEnPassant, HANDLER_DELAY_NEXT);
            }
        }
    };

    public Runnable mUpdateDraw50 = new Runnable()
    {
        public void run()
        {
            if (pressedUp)
            {
                setDraw50(pressedLeft);
                isUpdated = true;
                handlerDraw50.postDelayed(mUpdateDraw50, HANDLER_DELAY_NEXT);
            }
        }
    };

	public Runnable mUpdateMoveCounter = new Runnable()
	{
		public void run()
		{
			if (pressedUp)
			{
				setMoveCounter(pressedLeft);
				isUpdated = true;
				handlerMoveCounter.postDelayed(mUpdateMoveCounter, HANDLER_DELAY_NEXT);
			}
		}
	};


	final String TAG = "EditChessBoard";
	Util u;
	SharedPreferences userPrefs;
	SharedPreferences runP;
	C4aDialog c4aDialog;
	ProgressDialog progressDialog = null;
	ChessLogic cl;				// direct access to ChessLogic, Chess960, ChessHistory
	Chess960 chess960;
    ColorValues cv;

    CharSequence messageChessLogic = "";
    public ArrayList<CharSequence> stringValues = new ArrayList<CharSequence>();
	final static int OPTIONS_DIALOG = 1;
	final static int CHESS960_ID_DIALOG = 2;
	final static int PROGRESS_DIALOG = 22;
	int activDialog = 0;
	CharSequence currentFen = "";
	CharSequence standardFen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";
	CharSequence clearFen = "8/8/8/8/8/8/8/8 w - - 0 1";
	CharSequence newBoardFen = "";
	int gridViewSize = 0;

	CharSequence start960Fen = "8/pppppppp/8/8/8/8/PPPPPPPP/8 w - - 0 1";
	boolean isStartActivity = true;
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
	char piece = 'P';
	int selectedPosition = -1;
	CharSequence fenColor = "w";
	CharSequence fenCastling = "-";
	CharSequence fenEnPassant = "-";
	CharSequence fenDraw50 = "0";
	CharSequence fenMoveCounter = "1";
	Intent returnIntent = new Intent();
	BoardView boardView;

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

    ImageView trash;
    ImageView turnBoard;
	ImageView color;
	ImageView options;

	TextView title;
	TextView message;
    TextView btnCancel = null;
    TextView btnOk = null;

    Bitmap  wKingBmap;
    Bitmap  wQueenBmap;
    Bitmap  wRookBmap;
    Bitmap  wBishopBmap;
    Bitmap  wKnightBmap;
    Bitmap  wPawnBmap;
    Bitmap  bKingBmap;
    Bitmap  bQueenBmap;
    Bitmap  bRookBmap;
    Bitmap  bBishopBmap;
    Bitmap  bKnightBmap;
    Bitmap  bPawnBmap;
    Bitmap  trashBmap;

    // OPTIONS_DIALOG values
	CheckBox d_whiteKingCastle;
	CheckBox d_whiteQueenCastle;
	CheckBox d_blackKingCastle;
	CheckBox d_blackQueenCastle;
	TextView d_en_passant_item;
    ImageView d_en_passant_left;
    ImageView d_en_passant_right;
	TextView d_draw50_item;
    ImageView d_draw50_left;
    ImageView d_draw50_right;
	TextView d_move_counter_item;
    ImageView d_move_counter_left;
    ImageView d_move_counter_right;
	TextView d_chess960_manual;
	TextView d_chess960_random;
	TextView d_chess960_number;
	TextView d_to_clipboard;
	TextView d_from_clipboard;
	TextView d_standard;
	TextView d_current;
	TextView d_clear;

    boolean isWhiteKingCastle = false;
    boolean isWhiteQueenCastle = false;
    boolean isBlackKingCastle = false;
    boolean isBlackQueenCastle = false;

    final String EN_PASSANT_VALUES = "-abcdefgh";
    final int DRAW50_MIN = 0;
    final int DRAW50_MAX = 999;
	final int MOVE_COUNTER_MIN = 1;
	final int MOVE_COUNTER_MAX = 999;
    String en_passant = "-";
    int draw50 = DRAW50_MIN;
    int move_counter = MOVE_COUNTER_MIN;

    boolean pressedUp = false;
    boolean pressedLeft = false;
    boolean isUpdated = false;
	final long HANDLER_DELAY_START = 100;
	final long HANDLER_DELAY_NEXT = 20;
	public Handler handlerEnPassant = new Handler();
	public Handler handlerDraw50 = new Handler();
	public Handler handlerMoveCounter = new Handler();

}
