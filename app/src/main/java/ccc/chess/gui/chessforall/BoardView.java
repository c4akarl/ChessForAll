package ccc.chess.gui.chessforall;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import java.util.ArrayList;

import ccc.chess.logic.c4aservice.ChessHistory;

public class BoardView extends View
{
    public BoardView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        this.context = context;
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        if (Math.min(display.getWidth(), display.getHeight()) / 100 > STROKE_SIZE)
            STROKE_SIZE = Math.min(display.getWidth(), display.getHeight()) / 100;

//Log.d(TAG, "BoardView(), STROKE_SIZE: " + STROKE_SIZE);

    }

    public void setColor()
    {
        userPrefs = this.context.getSharedPreferences("user", 0);
        initColors();
        initPieces();
    }
    private void initPieces()
    {
        mPaint = new Paint();
        mRect = new Rect();

        whiteK = BitmapFactory.decodeResource(getResources(), R.drawable._1_wk).copy(Bitmap.Config.ARGB_8888, true);
        whiteQ = BitmapFactory.decodeResource(getResources(), R.drawable._1_wq).copy(Bitmap.Config.ARGB_8888, true);
        whiteR = BitmapFactory.decodeResource(getResources(), R.drawable._1_wr).copy(Bitmap.Config.ARGB_8888, true);
        whiteB = BitmapFactory.decodeResource(getResources(), R.drawable._1_wb).copy(Bitmap.Config.ARGB_8888, true);
        whiteN = BitmapFactory.decodeResource(getResources(), R.drawable._1_wn).copy(Bitmap.Config.ARGB_8888, true);
        whiteP = BitmapFactory.decodeResource(getResources(), R.drawable._1_wp).copy(Bitmap.Config.ARGB_8888, true);
        blackK = BitmapFactory.decodeResource(getResources(), R.drawable._1_bk).copy(Bitmap.Config.ARGB_8888, true);
        blackQ = BitmapFactory.decodeResource(getResources(), R.drawable._1_bq).copy(Bitmap.Config.ARGB_8888, true);
        blackR = BitmapFactory.decodeResource(getResources(), R.drawable._1_br).copy(Bitmap.Config.ARGB_8888, true);
        blackB = BitmapFactory.decodeResource(getResources(), R.drawable._1_bb).copy(Bitmap.Config.ARGB_8888, true);
        blackN = BitmapFactory.decodeResource(getResources(), R.drawable._1_bn).copy(Bitmap.Config.ARGB_8888, true);
        blackP = BitmapFactory.decodeResource(getResources(), R.drawable._1_bp).copy(Bitmap.Config.ARGB_8888, true);

        changePieceColor(whiteK, Color.WHITE, cv.getColor(cv.COLOR_PIECE_WHITE_3));
        changePieceColor(whiteK, Color.BLACK, cv.getColor(cv.COLOR_PIECE_BLACK_4));
        changePieceColor(whiteQ, Color.WHITE, cv.getColor(cv.COLOR_PIECE_WHITE_3));
        changePieceColor(whiteQ, Color.BLACK, cv.getColor(cv.COLOR_PIECE_BLACK_4));
        changePieceColor(whiteR, Color.WHITE, cv.getColor(cv.COLOR_PIECE_WHITE_3));
        changePieceColor(whiteR, Color.BLACK, cv.getColor(cv.COLOR_PIECE_BLACK_4));
        changePieceColor(whiteB, Color.WHITE, cv.getColor(cv.COLOR_PIECE_WHITE_3));
        changePieceColor(whiteB, Color.BLACK, cv.getColor(cv.COLOR_PIECE_BLACK_4));
        changePieceColor(whiteN, Color.WHITE, cv.getColor(cv.COLOR_PIECE_WHITE_3));
        changePieceColor(whiteN, Color.BLACK, cv.getColor(cv.COLOR_PIECE_BLACK_4));
        changePieceColor(whiteP, Color.WHITE, cv.getColor(cv.COLOR_PIECE_WHITE_3));
        changePieceColor(whiteP, Color.BLACK, cv.getColor(cv.COLOR_PIECE_BLACK_4));
        changePieceColor(blackK, Color.WHITE, cv.getColor(cv.COLOR_PIECE_WHITE_3));
        changePieceColor(blackK, Color.BLACK, cv.getColor(cv.COLOR_PIECE_BLACK_4));
        changePieceColor(blackQ, Color.WHITE, cv.getColor(cv.COLOR_PIECE_WHITE_3));
        changePieceColor(blackQ, Color.BLACK, cv.getColor(cv.COLOR_PIECE_BLACK_4));
        changePieceColor(blackR, Color.WHITE, cv.getColor(cv.COLOR_PIECE_WHITE_3));
        changePieceColor(blackR, Color.BLACK, cv.getColor(cv.COLOR_PIECE_BLACK_4));
        changePieceColor(blackB, Color.WHITE, cv.getColor(cv.COLOR_PIECE_WHITE_3));
        changePieceColor(blackB, Color.BLACK, cv.getColor(cv.COLOR_PIECE_BLACK_4));
        changePieceColor(blackN, Color.WHITE, cv.getColor(cv.COLOR_PIECE_WHITE_3));
        changePieceColor(blackN, Color.BLACK, cv.getColor(cv.COLOR_PIECE_BLACK_4));
        changePieceColor(blackP, Color.WHITE, cv.getColor(cv.COLOR_PIECE_WHITE_3));
        changePieceColor(blackP, Color.BLACK, cv.getColor(cv.COLOR_PIECE_BLACK_4));

    }

    public void updateBoardView(CharSequence fen, boolean boardTurn, int arrowMode, ArrayList<CharSequence> displayArrows, ArrayList<CharSequence> scoreArrows, ArrayList<CharSequence> possibleMoves,
                                ArrayList<CharSequence> possibleMovesTo, CharSequence lastMove, CharSequence selectedMove, boolean coordinates, boolean blindMode)
    {

//        Log.i(TAG, "updateBoardView(), fen: " + fen);

        if (fen.equals(""))
            fen = ChessHistory.fenStandardPosition;

//        Log.i(TAG, "updateBoardView(), arrowMode: " + arrowMode + ", displayArrows: " + displayArrows);

        posFen = "";
        isBoardTurn = boardTurn;
        this.arrowMode = arrowMode;
        this.displayArrows = displayArrows;
        this.scoreArrows = scoreArrows;
        this.possibleMoves = possibleMoves;
        this.possibleMovesTo = possibleMovesTo;
        this.lastMove = lastMove;
        this.selectedMove = selectedMove;
        isCoordinates = coordinates;
        isBlindMode = blindMode;
        for (int i = 0; i < fen.length(); i++)
        {
            if (fen.charAt(i) == ' ')
                break;
            else
            {
                if (fen.charAt(i) > '8' | fen.charAt(i) == '/')
                {
                    if (fen.charAt(i) != '/')
                        posFen = posFen.toString() +  fen.charAt(i);
                }
                else
                {
                    if (fen.charAt(i) == '1') {
                        posFen = posFen + "-";}
                    if (fen.charAt(i) == '2') {
                        posFen = posFen + "--";}
                    if (fen.charAt(i) == '3') {
                        posFen = posFen + "---";}
                    if (fen.charAt(i) == '4') {
                        posFen = posFen + "----";}
                    if (fen.charAt(i) == '5') {
                        posFen = posFen + "-----";}
                    if (fen.charAt(i) == '6') {
                        posFen = posFen + "------";}
                    if (fen.charAt(i) == '7') {
                        posFen = posFen + "-------";}
                    if (fen.charAt(i) == '8') {
                        posFen = posFen + "--------";}
                }
            }
        }

//Log.d(TAG, "posFen: " + posFen + "\nisBoardTurn: " + isBoardTurn + ", lastMove: " + lastMove);

        if (posFen.length() == 64)
        {
            if (!boardTurn)
            {
                for (int i = 0; i < posFen.length(); i++)
                {
                    charFen[i] = posFen.charAt(i);
                }
            }
            else
            {
                int cnt = 64;
                for (int i = 0; i < posFen.length(); i++)
                {
                    cnt--;
                    charFen[cnt] = posFen.charAt(i);
                }
            }
        }
        invalidate();
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

    public int getPositionFromTouch(int screenX, int screenY, int viewX, int viewY)
    {
        int x = screenX;
        int y = screenY;
        if (screenX > viewX)
            x = screenX - viewX;
        if (screenY > viewY)
            y = screenY - viewY;
        int posX = x / fieldSize;
        int posY = y / fieldSize;

//Log.i(TAG, "getPositionFromTouch(), posX: " + posX + ", posY: " + posY);

        return posX + (posY * 8);
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

    void changePieceColor(Bitmap bitmap, int originalColor, int newColor)
    {

//        Log.i(TAG, "changePieceColor(), originalColor:     " + originalColor + ", newColor: " + newColor);

        if (Math.abs(originalColor - newColor) <= COLOR_CHECK_DIFF)
            return;

        int[] pixels = new int[bitmap.getHeight() * bitmap.getWidth()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        for (int i=0; i < pixels.length; i++)
        {
            if (pixels[i] < 0)
            {
                if (originalColor == -1)
                {
                    if (pixels[i] > COLOR_CHECK_WHITE_BLACK)
                        pixels[i] = newColor;
                } else
                {
                    if (pixels[i] <= COLOR_CHECK_WHITE_BLACK)
                        pixels[i] = newColor;
                }
            }
        }
        bitmap.setPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
    {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getMeasuredWidth();
        int height = getMeasuredHeight();
        int sqSizeW = getSqSizeW(width);
        int sqSizeH = getSqSizeH(height);
        int sqSize = Math.min(sqSizeW, sqSizeH);
        if (height > width) {
            int p = getMaxHeightPercentage();
            height = Math.min(getHeight(sqSize), height * p / 100);
        } else {
            int p = getMaxWidthPercentage();
            width = Math.min(getWidth(sqSize), width * p / 100);
        }
        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT)
            boardSize = width;
        else
            boardSize = height;
        fieldSize = getSqSizeW(boardSize);
        textSize = fieldSize / 3;

//Log.d(TAG, "onMeasure(), BoardView, width: " + width + ", height: " + height + ", boardSize: " + boardSize + ", fieldSize: " + fieldSize);

        setMeasuredDimension(boardSize, boardSize);
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        boolean isBlack = true;
        int boardPos = 0;   // 0 . . . 63
        int lastMoveFrom = -1;
        int lastMoveTo = -1;
        int selPos = -1;


        if (lastMove != null)
        {
            if (lastMove.length() == 4)
            {
                lastMoveFrom    = getPosition(lastMove.subSequence(0, 2), isBoardTurn);
                lastMoveTo      = getPosition(lastMove.subSequence(2, 4), isBoardTurn);
            }
        }

//Log.d(TAG, "onDraw(), lastMove: " + lastMove);

        // 1. draw field, coordinates . . .
        for (int y = 0; y < 8; y++)
        {
            isBlack = !isBlack;
            for (int x = 0; x < 8; x++)
            {
                isBlack = !isBlack;
                mRect.set(x*fieldSize, y*fieldSize, x*fieldSize + fieldSize, y*fieldSize + fieldSize);
                int circleX = (x * fieldSize) + fieldSize / 2;
                int circleY = y * fieldSize + fieldSize / 2;
                int circleR = fieldSize / 4;
                mPaint.setStyle(Paint.Style.FILL);
                if (isBlack)
                    mPaint.setColor(cv.getColor(cv.COLOR_FIELD_LIGHT_1));
                else
                    mPaint.setColor(cv.getColor(cv.COLOR_FIELD_DARK_2));
                canvas.drawRect(mRect, mPaint);

//Log.d(TAG, "fenMes, boardPos: " + boardPos + ", Char: " + posFen.charAt(boardPos));

                if (!isBlindMode)
                {
                    switch (charFen[boardPos])
                    {
                        case 'K':
                            canvas.drawBitmap(whiteK, null, mRect, null);
                            break;
                        case 'Q':
                            canvas.drawBitmap(whiteQ, null, mRect, null);
                            break;
                        case 'R':
                            canvas.drawBitmap(whiteR, null, mRect, null);
                            break;
                        case 'B':
                            canvas.drawBitmap(whiteB, null, mRect, null);
                            break;
                        case 'N':
                            canvas.drawBitmap(whiteN, null, mRect, null);
                            break;
                        case 'P':
                            canvas.drawBitmap(whiteP, null, mRect, null);
                            break;
                        case 'k':
                            canvas.drawBitmap(blackK, null, mRect, null);
                            break;
                        case 'q':
                            canvas.drawBitmap(blackQ, null, mRect, null);
                            break;
                        case 'r':
                            canvas.drawBitmap(blackR, null, mRect, null);
                            break;
                        case 'b':
                            canvas.drawBitmap(blackB, null, mRect, null);
                            break;
                        case 'n':
                            canvas.drawBitmap(blackN, null, mRect, null);
                            break;
                        case 'p':
                            canvas.drawBitmap(blackP, null, mRect, null);
                            break;
                    }
                }

				if (selectedMove == null)
				{
					if (lastMoveFrom == boardPos)
					{
						mPaint.setColor(Color.TRANSPARENT);
						mPaint.setStyle(Paint.Style.FILL);
						canvas.drawRect(mRect, mPaint);
						mPaint.setColor(cv.getColor(cv.COLOR_FIELD_FROM_5));
						canvas.drawCircle(circleX, circleY, circleR, mPaint);
					}

					if (possibleMoves != null & !isBlindMode)
					{
						if (possibleMoves.size() > 0)
						{
							mPaint.setColor(cv.getColor(cv.COLOR_FIELD_TO_6));
							mPaint.setStyle(Paint.Style.FILL);

							for (int i = 0; i < possibleMoves.size(); i++)
							{

//Log.i(TAG, "updatePosibleMoves: " + possibleMoves.get(i));

								if (possibleMoves.get(i).length() == 4)
								{
									int posibleMovePos = getPosition(possibleMoves.get(i).subSequence(2, 4), isBoardTurn);
									if (posibleMovePos == boardPos & userPrefs.getBoolean("user_options_gui_posibleMoves", true))
									{
										mPaint.setColor(cv.getColor(cv.COLOR_FIELD_FROM_5));
										canvas.drawCircle(circleX, circleY, circleR, mPaint);
									}
								} else
									break;
							}
						}
					}
					if (possibleMovesTo != null & !isBlindMode)
					{
						if (possibleMovesTo.size() >= 2)
						{
							mPaint.setColor(cv.getColor(cv.COLOR_FIELD_TO_6));
							mPaint.setStyle(Paint.Style.FILL);

							for (int i = 0; i < possibleMovesTo.size(); i++)
							{

//Log.i(TAG, "updatePosibleMoves: " + possibleMovesTo.get(i));

								if (possibleMovesTo.get(i).length() >= 4)
								{
									int posibleMovePos = getPosition(possibleMovesTo.get(i).subSequence(0, 2), isBoardTurn);
									if (posibleMovePos == boardPos & userPrefs.getBoolean("user_options_gui_posibleMoves", true))
									{
										mPaint.setColor(cv.getColor(cv.COLOR_FIELD_FROM_5));
										canvas.drawCircle(circleX, circleY, circleR, mPaint);
									}
								} else
									break;
							}
						}
					}
				}

                if (isCoordinates)
                {
                    int x18 = x*fieldSize +5;
                    int y18 = y*fieldSize +5 + textSize;
                    int xAH = x*fieldSize +fieldSize -(fieldSize/4);
                    int yAH = y*fieldSize +fieldSize -8;
                    mPaint.setColor(cv.getColor(cv.COLOR_COORDINATES_7));
                    mPaint.setTextSize(textSize);
                    mPaint.setStrokeWidth(0);
                    CharSequence coo = coordinates[boardPos];
                    String coo18 = "";
                    String cooAH = "";
                    if (isBoardTurn)
                        coo = coordinatesTurn[boardPos];
                    if (!coo.equals(""))
                    {
                        if (coo.length() == 2)
                        {
                            coo18 = String.valueOf(coo.charAt(0));
                            cooAH = String.valueOf(coo.charAt(1));
                        }
                        else
                        {
                            if (Character.isDigit(coo.charAt(0)))
                                coo18 = String.valueOf(coo.charAt(0));
                            else
                                cooAH = String.valueOf(coo.charAt(0));
                        }
                    }
                    if (!coo18.equals(""))
                    {
                        canvas.drawText(coo18, x18, y18, mPaint);
                    }
                    if (!cooAH.equals(""))
                    {
                        canvas.drawText(cooAH, xAH, yAH, mPaint);
                    }
                }

                boardPos++;

            }
        }

		if (selectedMove != null)
		{
			boardPos = 0;
			selPos = getPosition(selectedMove, isBoardTurn);
			int mvPos = -1;
			if (lastMove.length() == 4) {
				if (lastMove.toString().startsWith(selectedMove.toString()))
					mvPos = getPosition(lastMove.subSequence(2, 4), isBoardTurn);
				else
					mvPos = getPosition(lastMove.subSequence(0, 2), isBoardTurn);
			}

//			Log.i(TAG, "lastMove, lastMove: " + lastMove + ", selectedMove: " + selectedMove + ", mvPos: " + mvPos);

			for (int y = 0; y < 8; y++)
			{
				for (int x = 0; x < 8; x++)
				{
					if (selPos == boardPos) {
						mPaint.setColor(Color.TRANSPARENT);
						mPaint.setStyle(Paint.Style.FILL);
						canvas.drawRect(mRect, mPaint);
						mRect.set(x*fieldSize +STROKE_SIZE_2, y*fieldSize +STROKE_SIZE_2, x*fieldSize + fieldSize -STROKE_SIZE_2, y*fieldSize + fieldSize -STROKE_SIZE_2);
						mPaint.setColor(cv.getColor(cv.COLOR_FIELD_TO_6));
						mPaint.setStrokeWidth(STROKE_SIZE);
						mPaint.setStyle(Paint.Style.STROKE);
						canvas.drawRect(mRect, mPaint);
					}
					if (mvPos == boardPos && displayArrows == null) {
						mPaint.setColor(Color.TRANSPARENT);
						mPaint.setStyle(Paint.Style.FILL);
						canvas.drawRect(mRect, mPaint);
						mPaint.setColor(cv.getColor(cv.COLOR_FIELD_FROM_5));
						int circleX = (x * fieldSize) + fieldSize / 2;
						int circleY = y * fieldSize + fieldSize / 2;
						int circleR = fieldSize / 4;
						canvas.drawCircle(circleX, circleY, circleR, mPaint);
					}
					boardPos++;
				}
			}
			return;
		}


//		Log.i(TAG, "onDraw, possibleMoves: " + possibleMoves);
//		Log.i(TAG, "onDraw, possibleMovesTo: " + possibleMovesTo);

        // 2. draw stroke
        boardPos = 0;
        for (int y = 0; y < 8; y++)
        {
            for (int x = 0; x < 8; x++)
            {
				if (lastMoveTo == boardPos)
				{

//Log.i(TAG, "onDraw, lastMoveTo: " + lastMoveTo + ", boardPos: " + boardPos);

					mPaint.setColor(Color.TRANSPARENT);
					mPaint.setStyle(Paint.Style.FILL);
					canvas.drawRect(mRect, mPaint);
					mRect.set(x * fieldSize + STROKE_SIZE_2, y * fieldSize + STROKE_SIZE_2, x * fieldSize + fieldSize - STROKE_SIZE_2, y * fieldSize + fieldSize - STROKE_SIZE_2);
					mPaint.setColor(cv.getColor(cv.COLOR_FIELD_TO_6));
					mPaint.setStrokeWidth(STROKE_SIZE);
					mPaint.setStyle(Paint.Style.STROKE);
					canvas.drawRect(mRect, mPaint);
				}
				if (possibleMoves != null)
				{
					if (possibleMoves.get(0).length() == 4)
					{
						int selectedFieldPos = getPosition(possibleMoves.get(0).subSequence(0, 2), isBoardTurn);

//Log.i(TAG, "onDraw, from lastMove: " + lastMove + ", selectedFieldPos: " + selectedFieldPos);

						if (selectedFieldPos == boardPos)
						{
							mPaint.setColor(Color.TRANSPARENT);
							mPaint.setStyle(Paint.Style.FILL);
							canvas.drawRect(mRect, mPaint);
							mRect.set(x * fieldSize + STROKE_SIZE_2, y * fieldSize + STROKE_SIZE_2, x * fieldSize + fieldSize - STROKE_SIZE_2, y * fieldSize + fieldSize - STROKE_SIZE_2);
							mPaint.setColor(cv.getColor(cv.COLOR_FIELD_TO_6));
							mPaint.setStrokeWidth(STROKE_SIZE);
							mPaint.setStyle(Paint.Style.STROKE);
							canvas.drawRect(mRect, mPaint);
						}
					}
				}
				if (possibleMovesTo != null)
				{
					if (possibleMovesTo.size() > 0)
					{
						if (possibleMovesTo.get(0).length() >= 4)
						{
							int selectedFieldPos = getPosition(possibleMovesTo.get(0).subSequence(2, 4), isBoardTurn);

//Log.i(TAG, "onDraw, to lastMove: " + lastMove + ", selectedFieldPos: " + selectedFieldPos);

							if (selectedFieldPos == boardPos)
							{
								mPaint.setColor(Color.TRANSPARENT);
								mPaint.setStyle(Paint.Style.FILL);
								canvas.drawRect(mRect, mPaint);
								mRect.set(x * fieldSize + STROKE_SIZE_2, y * fieldSize + STROKE_SIZE_2, x * fieldSize + fieldSize - STROKE_SIZE_2, y * fieldSize + fieldSize - STROKE_SIZE_2);
								mPaint.setColor(cv.getColor(cv.COLOR_FIELD_TO_6));
								mPaint.setStrokeWidth(STROKE_SIZE);
								mPaint.setStyle(Paint.Style.STROKE);
								canvas.drawRect(mRect, mPaint);
							}
						}
					}
				}

                boardPos++;

            }
        }

        // BCM
        if (displayArrows != null)
        {

//            Log.i(TAG, "onDraw, displayArrows.size: " + displayArrows.size());

            if (displayArrows.size() > 0) {
                for (int i = 0; i < displayArrows.size(); i++) {
                    int from = getPosition(displayArrows.get(i).subSequence(0, 2), isBoardTurn);
                    int to = getPosition(displayArrows.get(i).subSequence(2, 4), isBoardTurn);

                    int SameCount = 0;
                    int SameNr = 0;
                    for (int j = 0; j < displayArrows.size(); j++)
                        if (from == getPosition(displayArrows.get(j).subSequence(0, 2), isBoardTurn) &&
                                to == getPosition(displayArrows.get(j).subSequence(2, 4), isBoardTurn)) {
                            SameCount++;
                            if (j < i)
                                SameNr++;
                        }

                    // Set individual Design (default first)
                    int colorFill = Color.WHITE;
                    int colorStroke = Color.BLACK;
                    int strokeWidth = 2;
                    int size = 80;
                    int star = 0;

                    switch (arrowMode) {
                        case BoardView.ARROWS_BEST_VARIANT:

                            if (i % 2 == 0) { // player to move next
                                colorFill = cv.getColor(cv.COLOR_ARROWS1_23);
                            } else { // other player
                                colorFill = cv.getColor(cv.COLOR_ARROWS2_24);
                            }

                            strokeWidth = 3;

                            size = 90;
                            size = (int) (size - (size - size * 1.5 / displayArrows.size()) * i / (displayArrows.size() - 0.99999999));

                            int alpha = 0xFF * size / 100;

                            colorFill = Color.argb(alpha, Color.red(colorFill), Color.green(colorFill), Color.blue(colorFill));

                            double darkenF = 1 + 4 * (alpha / 255.0);
                            colorStroke = Color.argb(0xFF,
                                    (int) (Color.red(colorFill) / darkenF),
                                    (int) (Color.green(colorFill) / darkenF),
                                    (int) (Color.blue(colorFill) / darkenF));

                            star = 100;

                            break;
                        case BoardView.ARROWS_BEST_MOVES:
                            break;
                    }

                    // DrawArrow: from, to, size, star, colorFill, colorStroke, strokeWidth, SameCount, SameNr

                    int x0 = ((from % 8) * fieldSize) + fieldSize / 2;
                    int y0 = ((from / 8) * fieldSize) + fieldSize / 2;
                    int x1 = ((to % 8) * fieldSize) + fieldSize / 2;
                    int y1 = ((to / 8) * fieldSize) + fieldSize / 2;

                    float deltaX = x1 - x0;
                    float deltaY = y1 - y0;
                    float distance = (float) Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));

                    float dX = deltaX * fieldSize / distance;
                    float dY = deltaY * fieldSize / distance;

                    float shift = (float) 1 / 2 - (float) 1 / (SameCount + 1) - (float) SameNr / (SameCount + 1);
                    x0 += dY * shift * star / 100;
                    y0 -= dX * shift * star / 100;
                    x1 += dY * shift;
                    y1 -= dX * shift;

                    dX *= (float) size / 100 / 8;
                    dY *= (float) size / 100 / 8;

                    Path path = new Path();
                    path.setFillType(Path.FillType.EVEN_ODD);
                    path.moveTo(x0, y0);
                    path.lineTo(x1 + dY - 4 * dX, y1 - dX - 4 * dY);
                    path.lineTo(x1 + 4 * dY - 6 * dX, y1 - 4 * dX - 6 * dY);
                    path.lineTo(x1, y1);
                    path.lineTo(x1 - 4 * dY - 6 * dX, y1 + 4 * dX - 6 * dY);
                    path.lineTo(x1 - dY - 4 * dX, y1 + dX - 4 * dY);
                    path.lineTo(x0, y0);
                    path.close();

                    mPaint.setStyle(Paint.Style.FILL);
                    mPaint.setColor(colorFill);
                    canvas.drawPath(path, mPaint);

                    mPaint.setStyle(Paint.Style.STROKE);
                    mPaint.setColor(colorStroke);
                    mPaint.setStrokeWidth(strokeWidth);
                    mPaint.setAntiAlias(true);
                    mPaint.setDither(true);
                    canvas.drawPath(path, mPaint);

                }
            }
        }

    }

    final String TAG = "BoardView";

    Context context;
    SharedPreferences userPrefs;
    ColorValues cv;
    final static int COLOR_CHECK_DIFF = 1000000;
    final static int COLOR_CHECK_WHITE_BLACK = -10000000;

    Bitmap whiteK;
    Bitmap whiteQ;
    Bitmap whiteR;
    Bitmap whiteB;
    Bitmap whiteN;
    Bitmap whiteP;
    Bitmap blackK;
    Bitmap blackQ;
    Bitmap blackR;
    Bitmap blackB;
    Bitmap blackN;
    Bitmap blackP;

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
    final CharSequence coordinates[] =
            {		"8", "", "", "", "", "", "", "",
                    "7", "", "", "", "", "", "", "",
                    "6", "", "", "", "", "", "", "",
                    "5", "", "", "", "", "", "", "",
                    "4", "", "", "", "", "", "", "",
                    "3", "", "", "", "", "", "", "",
                    "2", "", "", "", "", "", "", "",
                    "1a", "b", "c", "d", "e", "f", "g", "h"};
    final CharSequence coordinatesTurn[] =
            {		"1", "", "", "", "", "", "", "",
                    "2", "", "", "", "", "", "", "",
                    "3", "", "", "", "", "", "", "",
                    "4", "", "", "", "", "", "", "",
                    "5", "", "", "", "", "", "", "",
                    "6", "", "", "", "", "", "", "",
                    "7", "", "", "", "", "", "", "",
                    "8h", "g", "f", "e", "d", "c", "b", "a"};

    protected int getWidth(int sqSize) { return sqSize * 8 + 4; }
    protected int getHeight(int sqSize) {
        return sqSize * 8 + 4;
    }
    protected int getSqSizeW(int width) { return (width - 4) / 8; }
    protected int getSqSizeH(int height) {
        return (height - 4) / 8;
    }
    protected int getMaxHeightPercentage() {
        return 75;
    }
    protected int getMaxWidthPercentage() {
        return 65;
    }
    int boardSize = 0;
    int fieldSize = 100;
    int textSize = 20;
    int STROKE_SIZE = 3;
    int STROKE_SIZE_2 = (STROKE_SIZE / 3 * 2);

    private Paint mPaint;
    private Rect mRect;
    CharSequence posFen = "";
    boolean isBoardTurn;
    int arrowMode = ARROWS_NONE;
    ArrayList<CharSequence> displayArrows;
    ArrayList<CharSequence> scoreArrows;
    ArrayList<CharSequence> possibleMoves;
    ArrayList<CharSequence> possibleMovesTo;
    CharSequence lastMove;
    CharSequence selectedMove;
    boolean isCoordinates;
    boolean isBlindMode = false;

    final static int ARROWS_NONE            = 900000;
    final static int ARROWS_BEST_VARIANT    = 120000;
    final static int ARROWS_BEST_MOVES      = 240000;

    private char[] charFen = new char[64];

}
