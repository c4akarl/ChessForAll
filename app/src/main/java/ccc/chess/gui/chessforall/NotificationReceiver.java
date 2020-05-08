package ccc.chess.gui.chessforall;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.io.File;

public class NotificationReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        if (intent.getAction().equals(NotificationHelper.ACTION_CONTINUE))
        {
            Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            context.sendBroadcast(it);
        }
        if (intent.getAction().equals(NotificationHelper.ACTION_CANCEL))
        {

            int notificationId = intent.getIntExtra("notificationId", 0);
            String fileName = intent.getStringExtra("pgnFileName");

//Log.i(TAG, "onReceive(), ACTION_CANCEL, fileName: " + fileName);

            String fileNameDb = fileName + "-db";
            String fileNameJournal = fileName + "-db-journal";
            File fileDb = new File(fileNameDb);
            if (fileDb.exists())
                fileDb.delete();
            File fileJournal = new File(fileNameJournal);
            if (fileJournal.exists())
                fileJournal.delete();
            Intent it = new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
            context.sendBroadcast(it);
            String ns = Context.NOTIFICATION_SERVICE;
            NotificationManager nMgr = (NotificationManager) context.getSystemService(ns);
            nMgr.cancel(notificationId);
        }
    }

//    final String TAG = "NotificationReceiver";

}
