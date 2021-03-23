package ccc.chess.gui.chessforall;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;

import com.kalab.chess.enginesupport.ChessEngine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

class FileIO
{

	FileIO(Context context)
	{
		this.context = context;
	}

	public Boolean isSdk30()
	{
		int deviceSdk = android.os.Build.VERSION.SDK_INT;
		int targetSdk = context.getApplicationContext().getApplicationInfo().targetSdkVersion;

//		Log.i(TAG, "isSdk30(), sdkInt: " + deviceSdk + ", targetSdk: " + targetSdk);

		return deviceSdk >= 30 && targetSdk >= 30;
	}

	String getExternalDirectory(int var)
	{
		return Environment.getExternalStorageDirectory().getAbsolutePath() + SEP;
	}

	String[] getExternalDirs()
	{
		String[] dirs = null;
		String externalStorage = Environment.getExternalStorageDirectory().getAbsolutePath();
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
		{
			dirs = new String[1];
			dirs[0] = externalStorage;
			return dirs;
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
		{
			final Set<String> rv = new HashSet<>();
			File[] listExternalDirs = context.getExternalFilesDirs(null);
			for (int i = 0; i < listExternalDirs.length; i++)
			{
				if (listExternalDirs[i] != null)
				{
					String path = listExternalDirs[i].getAbsolutePath();

//					Log.i(TAG, "1 getExternalDirs(), path:         " + i + " " + path);

					int indexMountRoot = path.indexOf(ANDROID_DATA);
					if (indexMountRoot >= 0 && indexMountRoot <= path.length())
					{
						//Get the root path for the external directory
						// mounted test only (read/write)
						String pathSub = path.substring(0, indexMountRoot);
						isStorageStateMounted(pathSub);

						rv.add(path.substring(0, indexMountRoot));
					}
				}
			}
			dirs = rv.toArray(new String[rv.size()]);
			if (dirs == null)
			{
				dirs[0] = externalStorage;
				return dirs;
			}
		}

		return dirs;

	}

	public boolean isExternalDir(String path)
	{
		if (path.endsWith("/"))
			path = path.substring(0, path.length() -1);
		String[] dirs = getExternalDirs();
		for(int i=0; i < dirs.length;i++)
		{

//			Log.i(TAG, "isExternalDir(), dirs[i]: " + dirs[i] + ", path: " + path);

			if (dirs[i].equals(path))
				return true;
		}
		return false;
	}

	public boolean isStorageStateMounted(String path)
	{
		File file = new File(path);
		boolean isMounted = false;
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT)
			isMounted = true;
		else
		{
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
			{
				String externalStorageState = Environment.getExternalStorageState(file);
				if (externalStorageState.equals(Environment.MEDIA_MOUNTED))
					isMounted = true;
			}
			else
			{
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT)
				{
					String storageState = Environment.getStorageState(file);
					if (storageState.equals(Environment.MEDIA_MOUNTED))
						isMounted = true;
				}
			}
		}

//		Log.i(TAG, "isStorageStateMounted(), file: " + file + ", isMounted:  " + isMounted);

		return isMounted;
	}

	public boolean pathExists(String path)
	{
		boolean isPath = false;
		File f = new File(path);
		if(f.isDirectory())
			isPath = true;
		return isPath;
	}


	public boolean canWrite(String path, String file)
	{
		File f = new File(path + file);
		return f.canWrite();
	}

	public boolean fileExists(String path, String file)
	{
		boolean isFile = true;
		String fileName = path + file;
		try
		{ 
			File f = new File(fileName); 							// ... no action
			FileInputStream fileIS = new FileInputStream(f);		// ... no action
			fileIS.available();
		} 
		catch (FileNotFoundException e) {isFile = false;} 		// file not exists! 
		catch (IOException e) 			{isFile = false;} 		// file not exists!

//		Log.i(TAG, "fileName, exists?: " + fileName + ", " + isFile);

		return isFile;
	}

	public boolean fileDelete(String path, String file)
	{
		File f = new File(path + file);
		String pgnDbFile = file.replace(".pgn", ".pgn-db");
		File fDb = new File(path + pgnDbFile);
		fDb.delete();			// delete .pgn-db
		return  f.delete();		// delete .pgn
	}

	public boolean createDir(String path)
	{
		File f = new File(path);
		return  f.mkdirs();
	}

	public boolean copyFile(String srcFile, String targetPath)
	{

//		Log.i(TAG, "copyFile(), srcFile: " + srcFile + "\ntargetPath: " + targetPath);

		boolean copyOk = false;
		if (srcFile.startsWith("file:"))
			srcFile = srcFile.replace("file:", "");
		File fFrom = new File(srcFile);
		File fDir = new File(targetPath);
		File fTo = new File(targetPath, fFrom.getName());

//		Log.i(TAG, "fFrom: " + fFrom.toString() + ", " + fFrom.exists());
//		Log.i(TAG, "fDir: " + fDir.toString() + ", " + fDir.isDirectory());
//		Log.i(TAG, "fTo: " + fTo.toString() + ", " + fTo.exists())

		if (fFrom.exists() & fDir.isDirectory())
		{
			if (!fTo.exists())
			{
				try 
				{
					copyOk = true;
					FileInputStream fileSrc = new FileInputStream(fFrom);
			        FileOutputStream fileDest = new FileOutputStream(fTo);
			        byte buffer[] = new byte[16384];
			        for (int len = fileSrc.read(buffer); len >= 0; len = fileSrc.read(buffer))
			        	fileDest.write(buffer, 0, len);
			        fileSrc.close();
			        fileDest.close();
				}
				catch (IOException e) {copyOk = false;}
			}
		}
		return copyOk; 
	}

	public String getExternalStoragePgnPathFromContent(String content)
	{

		if (content.contains("%2F"))
			content = content.replace("%2F", "/");
		if (content.contains("%3A")) // ':' --> '/'
			content = content.replace("%3A", "/");
		if (content.contains(":"))
			content = content.replace(":", "/");

//		Log.i(TAG, "getExternalStoragePgnPathFromContent(), externalStorageDirectory: " + Environment.getExternalStorageDirectory().getAbsolutePath());
//		Log.i(TAG, "getExternalStoragePgnPathFromContent(), content:                  " + content);

		String checkString = "";
		String[] split = content.split("/");
		for (int i = split.length -1; i >= 0; i--)
		{
			checkString = "/" + split[i] + checkString;
			File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + checkString);
			if (file.exists()) {

//				Log.i(TAG, "getExternalStoragePgnPathFromContent():                           " + Environment.getExternalStorageDirectory().getAbsolutePath() + checkString);

				return Environment.getExternalStorageDirectory().getAbsolutePath() + checkString;
			}
		}
		return content;
	}

	public String[] getFileArrayFromPath(String path, boolean allDirectory, String extension)
    {

//		Log.i(TAG, "getFileArrayFromPath(), path: " + path + ", file extension: " + extension);

		String[] tmpA = null;
		String[] fileA = null;
		File f;
		try
		{ 
			f = new File(path);
			tmpA = f.list();

//			Log.i(TAG, "getFileArrayFromPath(), f.exists(): " + f.exists()+ ", tmpA.length:" + tmpA.length);

			fileA = new String[tmpA.length];
			fileA = initArray(fileA);
			for (int i = 0; i < tmpA.length; i++) 
	    	{
				if (tmpA[i].endsWith(extension))
					fileA[i] = tmpA[i];
				else
				{
					f = new File(path + tmpA[i]);
					if (f.isFile() & extension.equals(""))
						fileA[i] = tmpA[i];
					else
					{
						if (f.isDirectory() & !f.isHidden())
						{

//							Log.i(TAG, "File: " + path + tmpA[i] + ", allDirectory: " + allDirectory);

							isPgnFile = false;
							if (allDirectory)
								fileA[i] = tmpA[i];
							else
							{
								if (isPgnFile)
									fileA[i] = tmpA[i];
							}
						}
					}
				}
	    	}
			fileA = killSpaceFolder(fileA);
			if (fileA != null)
			{
				List<String> tempList = Arrays.asList(fileA);				// sort Array
				Collections.sort(tempList);									// sort Array
				fileA = (String[]) tempList.toArray(new String[0]);			// sort Array
			}
			else
			{
				fileA = new String[1];
				fileA[0] = "";
			}
		} 
		catch (SecurityException e) 		{fileA = null;}
		catch (NullPointerException e) 		{fileA = null;}
		return fileA;
    }

	public String[] initArray(String[] filesA)
    {
		for (int i = 0; i < filesA.length; i++) 
    	{
			filesA[i] = "";
    	}
		return filesA;
    }

	public String[] killSpaceFolder(String[] filesAll)
	{
		String[] newA = null;
		int cntNew = 0;
		int cntSpc = 0;
		try 
		{
			for (int i = 0; i < filesAll.length; i++) 
	    	{
				if (filesAll[i] != null)
				{
					if (filesAll[i].equals(""))
						cntSpc++;
				}
	    	}
		}
		catch (NullPointerException e) {e.printStackTrace();}
		if (cntSpc > 0)
		{
			newA = new String[filesAll.length - cntSpc];
			for (int i = 0; i < filesAll.length; i++) 
	    	{
				if (!filesAll[i].equals(""))
				{
					newA[cntNew] = filesAll[i];
					cntNew++;
				}
	    	}
		}
		else
			newA = filesAll;
		return newA;
	}

	public int getRandomId(long skip) 		
    {	// random number(skip bytes)
    	Random r;
		int ir = 0;
		if (skip < Integer.MAX_VALUE)
			ir = (int) skip;
		else
			ir = Integer.MAX_VALUE - 100;
		r = new Random();
		try		{ir = r.nextInt(ir);}
		catch 	(IllegalArgumentException e) 	{e.printStackTrace(); return 0;}
		return ir;
    }

	public String dataFromFile(String path, String file, String lastGame, int gameControl, long offest)
    {

//		Log.i(TAG, "dataFromFile(), gameControl: " + gameControl);

		String pgnData = "";
		String previousGame = "";
		gameCount = 0;
		pgnStat = "-";
		long startSkip = 0;
		int bufferLength = 15000;
		long fileLength = 0;
		String line;
		lastGameNotFound = false;
		boolean isMoreGames = false;
		boolean isRead = false;
		boolean catchNextGame = false;
		isGameEnd = false;

		RandomAccessFile raf;
		long rafLinePointer = 0;
		long rafLastGamePointer = 0;
		long randomGamePointer = 0;
		
		try 
		{
			raf = new RandomAccessFile(path + file, "r");
			fileLength = raf.length();
		} 
		catch (FileNotFoundException e1) {e1.printStackTrace();	printData(gameControl, lastGame, pgnData, pgnStat, offest, -1); return "";}
		catch (IOException e) {e.printStackTrace(); printData(gameControl, lastGame, pgnData, pgnStat, offest, -1); return "";}	

		if (fileLength == 0)
			return "";

		switch (gameControl)
        {
        	case 0: 	startSkip = offest; 						break;	// next
        	case 1: 	startSkip = 0;								break;	// first
	        case 7: 	startSkip = getRandomId(fileLength); 		break;	// random
	        case 8: 	startSkip = offest - bufferLength; 			break; 	// previous
	        case 9: 	startSkip = fileLength - bufferLength; 		break;	// last
	        case 10: 	printData(gameControl, lastGame, lastGame, pgnStat, offest, gameOffset); return lastGame;	// current
        }
		if (startSkip > fileLength)
			startSkip = fileLength - bufferLength;
		if (startSkip < 0)
			startSkip = 0;
		try 
		{
			raf.seek(startSkip);
			rafLinePointer = raf.getFilePointer();
			if (gameControl == 7)
				randomGamePointer = rafLinePointer;
			line = raf.readLine();
			if (gameControl == 7 & !line.startsWith("[Event "))
			{
				startSkip = startSkip - bufferLength;
				if (startSkip < 0)
					startSkip = 0;
				raf.seek(startSkip);
				rafLinePointer = raf.getFilePointer();
				line = raf.readLine();
			}
			if (line != null)
				pgnStat = "X";
			while (line != null)
			{
				if (line.startsWith("[Event ") & gameCount > 0)
				{
					isRead = false;
					isMoreGames = true;
					if (lastGame.equals(""))
						break;
					else
					{
						switch (gameControl)
				        {
				        	case 0:	// next
					        		if (pgnData.equals(lastGame))
					        		{
					        			catchNextGame = true;
					        			gameOffset = rafLinePointer;
										pgnData = line + "\n";
					        		}
					        		else
					        		{
					        			if (catchNextGame)
					        			{
					        				catchNextGame = false;
					        				printData(gameControl, lastGame, pgnData, pgnStat, offest, gameOffset);
					        				return pgnData;
					        			}
					        			else
					        			{	// error, no lastGame ?!
					        				catchNextGame = false;
					        				printData(gameControl, lastGame, lastGame, pgnStat, offest, offest);
					        				return lastGame;
					        			}
					        		}
					        		break;
				        	case 1: // first
					        		gameOffset = rafLastGamePointer;
				        			pgnStat = "F";
				        			printData(gameControl, lastGame, pgnData, pgnStat, offest, gameOffset);
				        			return pgnData;
				        	case 7: // random
				        			if (rafLinePointer >= randomGamePointer)
				        			{
				        				if (gameCount == 1)
				        					pgnStat = "F";
				        				gameOffset = rafLastGamePointer;
				        				printData(gameControl, lastGame, pgnData, pgnStat, offest, gameOffset);
				        				return pgnData;	
				        			}
				        			else
				        			{
				        				rafLastGamePointer = rafLinePointer;
				        				pgnData = line + "\n";
				        			}
				        			break;
					        case 8: // previous
					        		previousGame = pgnData;
				        			if (rafLinePointer < offest)
				        			{
				        				rafLastGamePointer = rafLinePointer;
				        				pgnData = line + "\n";
				        			}
				        			else
				        			{
				        				if (gameCount == 1)
				        					pgnStat = "F";
				        				gameOffset = rafLastGamePointer;
				        				printData(gameControl, lastGame, previousGame, pgnStat, offest, gameOffset);
				        				return previousGame;
				        			}
				        			break;
					        case 9: // last
							        gameOffset = rafLinePointer;
			        				pgnData = line + "\n";
			        				break;
				        }
						gameCount++;
						if (gameCount > 500)	// no endless loop !
							break;
					}
				}
				else
				{
					isRead = true;
					if (line.startsWith("[Event "))
					{
						rafLastGamePointer = rafLinePointer;
						gameCount++;
					}
					if (gameCount > 0)
						pgnData = pgnData + line + "\n";
				}
				rafLinePointer = raf.getFilePointer();
				line = raf.readLine();
			}
			raf.close();
		} 
		catch (FileNotFoundException e) {e.printStackTrace(); printData(gameControl, lastGame, "", pgnStat, offest, -1); return "";}
		catch (IOException  e) {e.printStackTrace(); printData(gameControl, lastGame, "", pgnStat, offest, -1); return "";}
		catch (IndexOutOfBoundsException  e) {e.printStackTrace(); printData(gameControl, lastGame, "", pgnStat, offest, -1); return "";}
		
		if (isRead)
		{
			if (gameCount == 0)
				lastGameNotFound = true;
			else
			{
				if (gameControl == 9)
					pgnStat = "L";
				if (lastGame.equals(""))
					isGameEnd = true;
			}
		}
		if (startSkip == 0 & gameCount == 1)
			pgnStat = "F";
		if (!isMoreGames)
		{
			if (startSkip == 0)
			{
				if (pgnData.equals(""))
					pgnStat = "-";
				else
					pgnStat = "L";
			}
			else
			{
				if (pgnStat.equals("F"))
					pgnStat = "-";
				else
					pgnStat = "L";
			}
		}
		if (!lastGame.equals(""))
		{
			if (gameControl == 9)
			{
				if (!pgnData.equals(""))
					pgnStat = "L";
			}
		}
		if (pgnData.equals(""))
			pgnStat = "-";
		if (gameControl == 0 & catchNextGame)
			pgnStat = "L";
		if (gameControl == 7 & !pgnData.equals(""))
		{
			pgnStat = "L";
			gameOffset = rafLastGamePointer;
		}
		printData(gameControl, lastGame, pgnData, pgnStat, offest, gameOffset);
		return pgnData;
    }

	private void printData(int gameControl, String lastGame, String newGame, String pgnStat, long gameOffset, long newGameOffset)
	{

		// TEST ONLY !!!
//		Log.i(TAG, "gameControl, pgnStat, gameOffset, newGameOffset: " + gameControl + ", " + pgnStat + ", " + gameOffset + ", " + newGameOffset);
//		Log.i(TAG, "last game: \n" + lastGame + "\n\n");
//		Log.i(TAG, "new game: \n" + newGame);

	}

	String getDataFromInputStream(InputStream is)
    {	// for www(pgn)
		String txt = "";
		if (is != null)
		{
			try
			{ 
				BufferedReader buf = new BufferedReader(new InputStreamReader(is), 50000);
				String readString;
				while((readString = buf.readLine())!= null)
				{

//					Log.i(TAG, readString);

					txt = txt + readString + "\n";
				}
				is.close();
				buf.close();
			} 
			catch (IOException e)				{e.printStackTrace();}
		}
		return txt;
    }

	public String getNewExternalPath(String oldPath)
	{
		if (oldPath.equals(""))
		{
			String pathC4a = getExternalDirectory(0) + "c4a/";
			if (pathExists(pathC4a))
				return pathC4a;
		}
		return getExternalDirectory(0) + oldPath;
	}

	public String getUciExternalPath()
	{
		String pathC4a = getExternalDirectory(0) + "c4a/";
		if (!pathExists(pathC4a))
			createDir(pathC4a);
		String pathC4aUci = pathC4a + "uci/";
		if (pathExists(pathC4aUci))
			return pathC4aUci;
		else {
			if (createDir(pathC4aUci))
				return pathC4aUci;
			else
				return "";
		}
	}

	public String getDataFromUciFile(String path, String file)
	{
		File f = new File(path + file);
		StringBuilder text = new StringBuilder();
		try {
			BufferedReader br = new BufferedReader(new FileReader(f));
			String line;
			while ((line = br.readLine()) != null) {
				text.append(line);
				text.append('\n');
			}
			br.close();
		}
		catch (IOException e) {	return ""; }

//		Log.i(TAG, "getDataFromUciFile(), uciOpts: \n" + text.toString());

		return text.toString();
	}

	public String getPgnStat() {return pgnStat;}

	public void dataToFile(String path, String file, String data, boolean append)
    {

//		Log.i(TAG, "dataToFile(), file: " + path + file + ", append: " + append);
//		Log.i(TAG, "dataToFile(), data: \n" + data );

		try
		{ 
			File f = new File(path + file);
			if (!f.exists())
				f.createNewFile();

			if (!f.exists()) {
				//karl Dialog !
//				Log.i(TAG, "dataToFile(), file create error, file: " + f);
				return;
			}

			if (data.equals(""))
				return;

//			Log.i(TAG, "dataToFile(), f.length(): " + f.length());

			FileOutputStream fOut;
			if (append)
			{
				data = "\n" + data;
				fOut = new FileOutputStream(f, true);
			}
			else
				fOut = new FileOutputStream(f); 
			 
			OutputStreamWriter osw = new OutputStreamWriter(fOut);  
			osw.write(data); 
            osw.flush(); 
            osw.close();

		} 
		catch (FileNotFoundException e) 	{e.printStackTrace();} 
		catch (IOException e)				{e.printStackTrace();}
		catch (NullPointerException e)		{e.printStackTrace();}
    }

	public interface FileNameFilter {
		boolean accept(String filename);
	}

	public static String[] findFilesInDirectory(String dirName, final FileNameFilter filter) {
		File extDir = Environment.getExternalStorageDirectory();
		String sep = File.separator;
		File dir = new File(extDir.getAbsolutePath() + sep + dirName);
		File[] files = dir.listFiles(pathname -> {
			if (!pathname.isFile())
				return false;
			return (filter == null) || filter.accept(pathname.getAbsolutePath());
		});
		if (files == null)
			files = new File[0];
		final int numFiles = files.length;
		String[] fileNames = new String[numFiles];
		for (int i = 0; i < files.length; i++)
			fileNames[i] = files[i].getName();
		Arrays.sort(fileNames, String.CASE_INSENSITIVE_ORDER);
		return fileNames;
	}

	// OEX
	public static String openExchangeFileName(ChessEngine engine) {
		String ret = "";
		if (engine.getPackageName() != null)
			ret += sanitizeString(engine.getPackageName());
		ret += "-";
		if (engine.getFileName() != null)
			ret += sanitizeString(engine.getFileName());
		return ret;
	}

	private static String sanitizeString(String s) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char ch = s.charAt(i);
			if (((ch >= 'A') && (ch <= 'Z')) ||
					((ch >= 'a') && (ch <= 'z')) ||
					((ch >= '0') && (ch <= '9')))
				sb.append(ch);
			else
				sb.append('_');
		}
		return sb.toString();
	}

	// Uri (<= Android sdk 29)
	public String getPathFromURI(Context context, Uri uri) {
		// DocumentProvider, ExternalStorageProvider
		if (DocumentsContract.isDocumentUri(context, uri) && isExternalStorageDocument(uri)) {
			String docId = DocumentsContract.getDocumentId(uri);
			String[] split = docId.split(":");
			String type = split[0];
			if ("primary".equalsIgnoreCase(type))
				return Environment.getExternalStorageDirectory() + "/" + split[1];
		}
		// DocumentProvider, DownloadsProvider
		if (DocumentsContract.isDocumentUri(context, uri) && isDownloadsDocument(uri)) {
			final String id = DocumentsContract.getDocumentId(uri);
			final Uri contentUri = ContentUris.withAppendedId(
					Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
			return getDataColumn(context, contentUri, null, null);
		}
		// File
		if ("file".equalsIgnoreCase(uri.getScheme())) {
			return uri.getPath();
		}

		return null;
	}

	public boolean isExternalStorageDocument(Uri uri) {
		return "com.android.externalstorage.documents".equals(uri.getAuthority());
	}

	public boolean isDownloadsDocument(Uri uri) {
		return "com.android.providers.downloads.documents".equals(uri.getAuthority());
	}

	public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
		Cursor cursor = null;
		String column = "_data";
		String[] projection = {
				column
		};
		try {
			cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
					null);
			if (cursor != null && cursor.moveToFirst()) {
				int column_index = cursor.getColumnIndexOrThrow(column);
				return cursor.getString(column_index);
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return null;
	}

	final String TAG = "FileIO";
	private final String ANDROID_DATA = "/Android/data";
	final String SEP = "/";
	private Context context;
	private boolean isPgnFile = false;

	//karl ???
	private boolean lastGameNotFound = false;
	private boolean isGameEnd = false;

	private int gameCount = 0;
	String pgnStat = "-";
	long gameOffset = 0;

}
