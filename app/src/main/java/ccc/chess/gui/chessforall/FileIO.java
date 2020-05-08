package ccc.chess.gui.chessforall;

import android.content.Context;
import android.os.Build;
import android.os.Environment;

import com.kalab.chess.enginesupport.ChessEngine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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

//Log.i(TAG, "1 getExternalDirs(), path:         " + i + " " + path);
//Log.i(TAG, "1 getExternalDirs(), storageState: " + i + " " + storageState);

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

//			for(int i=0; i < dirs.length;i++)
//			{
//				Log.i(TAG, "getExternalDirs(), external dirs: " + dirs[i]);
//			}

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

//Log.i(TAG, "isExternalDir(), dirs[i]: " + dirs[i] + ", path: " + path);

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

//Log.i(TAG, "isStorageStateMounted(), file: " + file + ", isMounted:  " + isMounted);

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
//Log.i(TAG, "fileName, exists?: " + fileName + ", " + isFile);
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

//Log.i(TAG, "fFrom: " + fFrom.toString() + ", " + fFrom.exists());
//Log.i(TAG, "fDir: " + fDir.toString() + ", " + fDir.isDirectory());
//Log.i(TAG, "fTo: " + fTo.toString() + ", " + fTo.exists())
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

	public String getExternalStorageFromContent(String content)
	{

//Log.i(TAG, "getExternalStorageFromContent(), content: " + content);

		if (content.startsWith(CONTENT))
		{
			if (content.contains(ANDROID_DATA))
			{
				int x = content.indexOf(ANDROID_DATA);
				String srcFile = Environment.getExternalStorageDirectory().getAbsolutePath() + content.substring(x, content.length());
				File file = new File(srcFile);
				if (file.exists())
					return srcFile;
			}
			else
			{
				if (content.contains(EXTERNAL + "/"))
				{
					int x = content.indexOf(EXTERNAL);
					String srcFile = content.substring(x, content.length());
					srcFile = srcFile.replace(EXTERNAL, Environment.getExternalStorageDirectory().getAbsolutePath());
					File file = new File(srcFile);
					if (file.exists())
						return srcFile;
				}
			}
		}
		return content;
	}

	public String[] getFileArrayFromPath(String path, boolean allDirectory, String extension)
    {

//Log.i(TAG, "getFileArrayFromPath(), path: " + path + ", file extension: " + extension);

		String[] tmpA = null;
		String[] fileA = null;
		File f;
		try
		{ 
			f = new File(path);
			tmpA = f.list();
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

//Log.i(TAG, "File: " + path + tmpA[i] + ", allDirectory: " + allDirectory);

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

//Log.i(TAG, "dataFromFile(), gameControl: " + gameControl);

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

//Log.i(TAG, readString);

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

	public String getPgnStat() {return pgnStat;}

	public void dataToFile(String path, String file, String data, boolean append)
    {
		try
		{ 
			File f = new File(path + file);
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
//20180501, at ccc.chess.gui.chessforall.FileIO.dataToFile: (FileIO.java:596)java.lang.NullPointerException:
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

//	final String TAG = "FileIO";
	private final String CONTENT = "content:";
	private final String EXTERNAL = "/external";
	private final String ANDROID_DATA = "/Android/data";
	final String BASE_PATH = "c4a/pgn/";
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
