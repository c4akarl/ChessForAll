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

    public void setTextViewColors(TextView tv, String color)
    {
        if (tv != null) {
            GradientDrawable tvBackground = (GradientDrawable) tv.getBackground();
            if (tvBackground != null)
                tvBackground.setColor(Color.parseColor(color));
        }
    }

    final String TAG = "Util";

}
