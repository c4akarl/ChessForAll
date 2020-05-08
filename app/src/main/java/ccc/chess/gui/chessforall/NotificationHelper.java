package ccc.chess.gui.chessforall;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

public class NotificationHelper 
{
    private Context mContext;
    public int notificationId = 1;
    private Notification mNotification;
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder builder;
    private PendingIntent mContentIntent;
    private PendingIntent pendingIntentContinue;
    private PendingIntent pendingIntentCancel;
    private CharSequence mContentTitle;
    public NotificationHelper(Context context, int notificationId)
    {
        mContext = context;
        this.notificationId = notificationId;
    }
    public void createNotification(String title, String actionTyp, String pgnFileName)
    {

//        Log.i(TAG, "createNotification(), notificationId: " + notificationId + ", pgnFileName: " + pgnFileName);

        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        int icon = R.drawable.stat_db_add;
        CharSequence tickerText = mContext.getString(R.string.app_pgnFileManager);
        long when = System.currentTimeMillis();
        mContentTitle = title;
        CharSequence contentText = "0%";

        Intent notificationIntent = new Intent();
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContentIntent = PendingIntent.getActivity(mContext, notificationId, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentCancel = new Intent(mContext, NotificationReceiver.class);
        intentCancel.setAction(ACTION_CANCEL);
        intentCancel.putExtra("notificationId", notificationId);
        intentCancel.putExtra("pgnFileName", pgnFileName);
        pendingIntentCancel = PendingIntent.getBroadcast(mContext, notificationId, intentCancel, PendingIntent.FLAG_UPDATE_CURRENT);

        Intent intentContinue = new Intent(mContext, NotificationReceiver.class);
        intentContinue.setAction(ACTION_CONTINUE);
        intentContinue.putExtra("notificationId", notificationId);
        intentContinue.putExtra("pgnFileName", pgnFileName);
        pendingIntentContinue = PendingIntent.getBroadcast(mContext, notificationId, intentContinue, PendingIntent.FLAG_UPDATE_CURRENT);

        builder = new NotificationCompat.Builder(mContext);
        mNotification = builder.setContentIntent(mContentIntent)
                .setSmallIcon(icon).setTicker(tickerText).setWhen(when)
                .setAutoCancel(true)
                .setContentTitle(mContentTitle)
                .setContentText(contentText)
                .addAction(R.drawable.ic_action_cancel, mContext.getString(R.string.btn_Cancel), pendingIntentCancel)
                .addAction(R.drawable.ic_action_continue, mContext.getString(R.string.btn_Continue), pendingIntentContinue)
                .setPriority(Notification.PRIORITY_MAX)
                .build();
        mNotification.flags = Notification.FLAG_ONGOING_EVENT;
        mNotificationManager.notify(notificationId, mNotification);
    }

    public void progressUpdate(Long fParsedPercentage, Long fParsed, Long fLength, Long fGames) 
    {
    	char km = 'K';
    	double fParsedD = fParsed;
    	double fLengthD = fLength;
    	if (fLength < 1000000)
    	{
    		fParsedD = fParsed / 100;
    		fParsedD = fParsedD / 10;
    		fLengthD = fLength / 100;
    		fLengthD = fLengthD / 10;
    	}
    	else
    	{
    		km = 'M';
    		fParsedD = fParsed / 100000;
    		fParsedD = fParsedD / 10;
    		fLengthD = fLength / 100000;
    		fLengthD = fLengthD / 10;
    	}
    	CharSequence contentText = "";
    	if (fParsedPercentage > 990)
    	{
    		if (fParsedPercentage == 998)
    			contentText = "droping indexes, please wait  . . .";
    		if (fParsedPercentage == 999)
    			contentText = "100%, creating indexes, please wait  . . .";
    	}
    	else
        {
            if (fGames >= 0)
                contentText = fParsedPercentage + "%, " + fGames + " games, " + fParsedD + "(" + fLengthD + ")" + km;
            else
                contentText = fParsedPercentage + "%, " + fParsedD + "(" + fLengthD + ")" + km;
        }
        builder.setContentText(contentText);
        mNotification = builder.getNotification();
        mNotification.flags = Notification.FLAG_ONGOING_EVENT;
        mNotificationManager.notify(notificationId, mNotification);
    }
    public void completed()    
    {
        if (mNotificationManager != null)
            mNotificationManager.cancel(notificationId);
    }
    
    final String TAG = "NotificationHelper";
    public static final String ACTION_CONTINUE = "ccc.chess.gui.chessforall.CONTINUE";
    public static final String ACTION_CANCEL = "ccc.chess.gui.chessforall.CANCEL";

}
