package ccc.chess.gui.chessforall;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
//import android.util.Log;

public class NotificationHelper 
{
    private Context mContext;
    public int notivicationId = 1;
    private Notification mNotification;
    private NotificationManager mNotificationManager;
    private NotificationCompat.Builder builder;
    private PendingIntent mContentIntent;
    private CharSequence mContentTitle;
    public NotificationHelper(Context context, int notivicationId)
    {
        mContext = context;
        this.notivicationId = notivicationId;
    }
    public void createNotification(String fileName) 
    {
        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        int icon = R.drawable.stat_db_add;
        CharSequence tickerText = mContext.getString(R.string.app_pgnFileManager);
        long when = System.currentTimeMillis();
//        mNotification = new Notification(icon, tickerText, when);
        mContentTitle = mContext.getString(R.string.fmCreatingPgnDatabase)  + fileName; 
        CharSequence contentText = "0%";
        Intent notificationIntent = new Intent();
        mContentIntent = PendingIntent.getActivity(mContext, 0, notificationIntent, 0);
//        mNotification.setLatestEventInfo(mContext, mContentTitle, contentText, mContentIntent);
//        mNotification.flags = Notification.FLAG_ONGOING_EVENT;
//        mNotificationManager.notify(notivicationId, mNotification);
        builder = new NotificationCompat.Builder(mContext);
        mNotification = builder.setContentIntent(mContentIntent)
                .setSmallIcon(icon).setTicker(tickerText).setWhen(when)
                .setAutoCancel(true).setContentTitle(mContentTitle)
                .setContentText(contentText).build();
        mNotification.flags = Notification.FLAG_ONGOING_EVENT;
        mNotificationManager.notify(notivicationId, mNotification);
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
    		contentText = fParsedPercentage + "%, " + fGames + " games, " + fParsedD + "(" + fLengthD + ")" + km;
//        mNotification.setLatestEventInfo(mContext, mContentTitle, contentText, mContentIntent);
        builder.setContentText(contentText);
        mNotification = builder.getNotification();
        mNotification.flags = Notification.FLAG_ONGOING_EVENT;
        mNotificationManager.notify(notivicationId, mNotification);
    }
    public void completed()    
    {
        mNotificationManager.cancel(notivicationId);
    }
    
    final String TAG = "NotificationHelper";
}
