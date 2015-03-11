package ccc.chess.gui.chessforall;

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
import java.util.List;
import java.util.Random;
import android.os.Environment;
//import android.util.Log;

public class PgnIO
{
	public String getExternalDirectory(int var)
	{
		return Environment.getExternalStorageDirectory().getAbsolutePath()  + "/";
	}
	public boolean pathExists(String path)
	{
		boolean isPath = false;
		File f = new File(path);
		if(f.isDirectory())
			isPath = true;
		return isPath;
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
		boolean copyOk = false;
		if (srcFile.startsWith("file:"))
			srcFile = srcFile.replace("file:", "");
		File fFrom = new File(srcFile);
		File fDir = new File(targetPath);
		File fTo = new File(targetPath, fFrom.getName());
//		Log.i(TAG, "fFrom: " + fFrom.toString() + ", " + fFrom.exists());
//		Log.i(TAG, "fDir: " + fDir.toString() + ", " + fDir.isDirectory());
//		Log.i(TAG, "fTo: " + fTo.toString() + ", " + fTo.exists());
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
	public boolean moveFile(String srcFile, String targetPath)
	{
		boolean copyOk = false;
		if (srcFile.startsWith("file:"))
			srcFile = srcFile.replace("file:", "");
		File fFrom = new File(srcFile);
		File fDir = new File(targetPath);
		String fileName = fFrom.getName();
		File fTo = new File(targetPath, fileName);
		if (fFrom.exists() & fDir.isDirectory())
		{
			if (!fTo.exists())
			{
				fFrom.renameTo(fTo);
				fTo = new File(targetPath, fileName);
				if (fTo.exists())
					copyOk = true;
			}
		}
		return copyOk; 
	}
	public String[] getFileArrayFromPath(String path, boolean allDirectory, String extension)
    {
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
					if (f.isDirectory() & !f.isHidden())
					{
//						Log.i(TAG, "File: " + path + tmpA[i]);
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
	public void search(File f) 
	{ 	// canceled 20120928(v1.29)
        if ( !f.exists() ) return; 
        String name = f.getName();
        if ( f.isDirectory() ) 
        {
	        File[] files = f.listFiles();
// ERROR	v1.4.1  	26.09.2011 05:49:08	        
	        if (files.length > 0)
	        {
		        for( int i = 0 ; i < files.length; i++ ) 
		        {
		        	search( files[i] );
		        }
	        }
        }
        if (name.endsWith(".pgn"))
        {
//        	Log.i(TAG, "File: " + f);
        	isPgnFile = true;
        	return;
        }
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
	public void printData(int gameControl, String lastGame, String newGame, String pgnStat, long gameOffset, long newGameOffset)
	{	// TEST ONLY !!!
//		Log.i(TAG, "gameControl, pgnStat, gameOffset, newGameOffset: " + gameControl + ", " + pgnStat + ", " + gameOffset + ", " + newGameOffset);
//		Log.i(TAG, "last game: \n" + lastGame + "\n\n");
//		Log.i(TAG, "new game: \n" + newGame);
	}
	public String getDataFromInputStream(InputStream is) 
    {	// for www(pgn)
		String txt = "";
		if (is != null)
		{
			try
			{ 
				BufferedReader buf = new BufferedReader(new InputStreamReader(is), 50000);
				String readString = new String();
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
    }
	
	final String TAG = "PgnIO"; 
	boolean isPgnFile = false;
	boolean lastGameNotFound = false;
	boolean isGameEnd = false;
	int gameCount = 0;
	String pgnStat = "-";
	long gameOffset = 0;
}
