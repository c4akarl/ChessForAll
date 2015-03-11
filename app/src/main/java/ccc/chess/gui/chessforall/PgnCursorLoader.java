package ccc.chess.gui.chessforall;
import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.AsyncTaskLoader;
//  https://raw.github.com/gist/1217628/e37643ff3eee74d3a2bc717017cb5923016715c6/SimpleCursorLoader.java
public abstract class PgnCursorLoader extends AsyncTaskLoader<Cursor> 
{
    private Cursor mCursor;
    public PgnCursorLoader(Context context) 
    {
        super(context);
    }
    @Override	// Runs on a worker thread
    public abstract Cursor loadInBackground();			
    @Override	// Runs on the UI thread
    public void deliverResult(Cursor cursor) 
    {
        if (isReset()) 
        {
            if (cursor != null) 
            {	// An async query came in while the loader is stopped
            	cursor.close();
            }
            return;
        }
        Cursor oldCursor = mCursor;
        mCursor = cursor;
        if (isStarted()) 
        	super.deliverResult(cursor);

        if (oldCursor != null && oldCursor != cursor && !oldCursor.isClosed()) 
            oldCursor.close();
    }
    @Override	// Must be called from the UI thread
    protected void onStartLoading() 
    {
        if (mCursor != null)
            deliverResult(mCursor);
        if (takeContentChanged() || mCursor == null)
            forceLoad();
    }
    @Override	// Must be called from the UI thread
    protected void onStopLoading() 
    {	// Attempt to cancel the current load task if possible.
        cancelLoad();
    }
    @Override
    public void onCanceled(Cursor cursor) 
    {
        if (cursor != null && !cursor.isClosed())
            cursor.close();
    }
    @Override
    protected void onReset() 
    {
        super.onReset();
        onStopLoading();
        if (mCursor != null && !mCursor.isClosed())
            mCursor.close();
        mCursor = null;
    }
}
