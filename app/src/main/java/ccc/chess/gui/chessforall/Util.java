package ccc.chess.gui.chessforall;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

public class Util
{
    public void updateFullscreenStatus(Activity a, boolean bUseFullscreen)
    {

//Log.i(TAG, "updateFullscreenStatus(), bUseFullscreen: " + bUseFullscreen);

        if(bUseFullscreen)  // from show status bar ---> show full screen !!!
        {
            a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            a.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        }
        else
        {
            a.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            a.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    public int getAspectRatio(Activity a)
    {
        Display display = ((WindowManager) a.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int w = display.getWidth();
        int h = display.getHeight();
        int aspectRatio;
        if (w > h)
            aspectRatio =  (w * 100) / h;
        else
            aspectRatio =  (h * 100) / w;

//Log.i(TAG, "aspectRatio: " + aspectRatio);

        return aspectRatio;
    }

    public boolean isViewInBounds(View view, int x, int y)
    {
        Rect outRect = new Rect();
        int[] location = new int[2];
        view.getDrawingRect(outRect);
        view.getLocationOnScreen(location);
        outRect.offset(location[0], location[1]);
        return outRect.contains(x, y);
    }

    public String getEngineEloFromC4aElo(int c4aElo, String uciOptions, String engineName, int engineMin, int engineMax)
    {
        if (uciOptions.equals(""))
            return "";
        if (!uciOptions.contains("UCI_Elo"))
            return engineName + ": -";
        String engineTxt = engineName + ":  " + c4aElo;
        if (engineMin > c4aElo)
            engineTxt = engineName + ":  " + engineMin;
        if (engineMax < c4aElo)
            engineTxt = engineName + ":  " + engineMax;
        return engineTxt;
    }

    public void setTextViewColors(TextView tv, ColorValues cv, int color)
    {
        GradientDrawable tvBackground = (GradientDrawable) tv.getBackground();
        tvBackground.setColor(cv.getColor(color));
    }

    public void setTextViewColors(TextView tv, String color)
    {
        if (tv != null) {
            GradientDrawable tvBackground = (GradientDrawable) tv.getBackground();
            if (tvBackground != null)
                tvBackground.setColor(Color.parseColor(color));
        }
    }

    public void setTextViewColors(TextView tv, String colorBg, String colorTxt)
    {
        if (tv != null) {
            GradientDrawable tvBackground = (GradientDrawable) tv.getBackground();
            if (tvBackground != null)
                tvBackground.setColor(Color.parseColor(colorBg));
            tv.setTextColor(Color.parseColor(colorTxt));
        }
    }

    public void setTextViewColors(TextView tv, ColorValues cv, int tvColor, int tvTextColor)
    {
        if (tv != null && cv != null) {
            GradientDrawable tvBackground = (GradientDrawable) tv.getBackground();
            tvBackground.setColor(cv.getColor(tvColor));
            tv.setTextColor(cv.getColor(tvTextColor));
        }
    }

    public CharSequence getDisplayScore(int score, CharSequence fen)
    {
        char color = 'w';
        CharSequence[] fenSplit = fen.toString().split(" ");
        if (fenSplit.length >= 0)
        {
            if (fenSplit[1].equals("b"))
                color = 'b';
        }
        int s1 = score / 100;
        int s2 = score % 100;
        CharSequence s = "";		// score
        if (s1 < 0)	s1 = s1 * -1;
        if (s2 < 0)	s2 = s2 * -1;
        if (color == 'w')
        {
            if (score < 0)
                s = s + "-";
            else
                s = s + "+";
        }
        else
        {
            if (score < 0)
                s = s + "+";
            else
                s = s + "-";
        }
        if (s2 < 10)
            s = s + Integer.toString(s1) + ".0" + s2;
        else
            s = s + Integer.toString(s1) + "." + s2;
        return s;
    }

    final String TAG = "Util";

}
