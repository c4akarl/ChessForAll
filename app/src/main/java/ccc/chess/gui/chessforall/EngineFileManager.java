package ccc.chess.gui.chessforall;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

public class EngineFileManager 
{
	//  managing engines on package file system: /data/data
	//	FILE (DATA)		FILE (DATA)		FILE (DATA)		FILE (DATA)		FILE (DATA)
	public String[] getFileArrayFromData(String dataPath)
    {
		String[] sortedFiles = null;
		File f;
		f = new File(dataPath);
		sortedFiles = f.list();
		if (sortedFiles != null)
			Arrays.sort(sortedFiles, new AlphabeticComparator());
		return sortedFiles;
    }

//	public boolean writeEngineToData(String filePath, String fileName, InputStream is)
//	{
//		// if no engine exists in >/data/data/ccc.chess.engines/engines< install default engine! !!! dataEnginesPath
//        File file = new File(dataEnginesPath);
//		if (!file.exists())
//		{
//			if (!file.mkdir())
//				return false;
//		}
//		boolean engineUpdated = false;
//		File f = new File(dataEnginesPath + fileName);
//		if (f.exists())
//		{
//			try
//			{
////Log.i(TAG, "1 writeEngineToData(), f.exists(): " + f);
//				String cmd[] = { "chmod", "744", f.getAbsolutePath() };
//				Process process = Runtime.getRuntime().exec(cmd);
//				try
//				{
//					process.waitFor();
//					engineUpdated = true;
//				}
//				catch (InterruptedException e)
//				{
//					deleteFileFromData(fileName);
//					return false;
//				}
//			}
//			catch (IOException e)
//			{
//				deleteFileFromData(fileName);
//				return false;
//			}
////Log.i(TAG, "2 writeEngineToData(), f.exists(): " + f);
//		}
//		else
//		{
//			try
//			{
//				InputStream istream;
//				if (is != null)
//					istream = is;
//				else
//				{
//					f = new File(filePath, fileName);
//					istream = new FileInputStream(f);
//				}
//                FileOutputStream fout = new FileOutputStream(dataEnginesPath + fileName);
//				byte[] b = new byte[1024];
//				int noOfBytes = 0;
//				while ((noOfBytes = istream.read(b)) != -1)
//				{
//					fout.write(b, 0, noOfBytes);
//				}
//				istream.close();
//				fout.close();
//				try
//				{
//                    String cmd[] = { "chmod", "744", dataEnginesPath + fileName };
//					Process process = Runtime.getRuntime().exec(cmd);
//					try
//					{
//						process.waitFor();
//						engineUpdated = true;
//					}
//					catch (InterruptedException e)
//					{
//						deleteFileFromData(fileName);
//						return false;
//					}
////Log.i(TAG, "3 writeEngineToData(), engineUpdated: " + engineUpdated);
//				}
//				catch (IOException e)
//				{
//					deleteFileFromData(fileName);
//					return false;
//				}
//				if (!isEngineProcess(fileName))	// not an engine process: kill data and return!
//				{
////Log.i(TAG, "4 writeEngineToData(), !isEngineProcess(), engineUpdated: " + engineUpdated);
//					deleteFileFromData(fileName);
//					return false;
//				}
//			}
//			catch (IOException e)
//			{
//				deleteFileFromData(fileName);
//				return false;
//			}
//		}
//		return engineUpdated;
//	}

	public boolean dataFileExist(String file)
	{
		File f = new File(dataEnginesPath, file);
		return f.exists();
	}

//	public boolean deleteFileFromData(String file)
//	{
//		File f = new File(dataEnginesPath, file);
//		return  f.delete();
//	}

	//	VALIDATE PROCESS		VALIDATE PROCESS		VALIDATE PROCESS		VALIDATE PROCESS
//	private boolean isEngineProcess(String file)
//	{
//		boolean isProcess = false;
//    	Process process;
//    	ProcessBuilder builder = new ProcessBuilder(dataEnginesPath + "/" + file);
//    	try
//		{
//    		process = builder.start();
//    		OutputStream stdout = process.getOutputStream();
//			InputStream stdin = process.getInputStream();
//			reader = new BufferedReader(new InputStreamReader(stdin));
//			writer = new BufferedWriter(new OutputStreamWriter(stdout));
//		}
//		catch (IOException e)
//		{
//			return false;
//		}
//		try
//		{
////			writeToProcess("isready" + "\n");
//			writeToProcess("uci" + "\n");
//		}
//		catch (IOException e) {e.printStackTrace();}
//		String line = "";
//		int cnt = 0;
//		while (cnt < 100)
//		{
//			try
//			{
//				line = readFromProcess();
////				Log.i(TAG, "cnt, line: " + cnt + ", " + line);
//				if (line != null)
//				{
////					if (line.contains("ok"))
//					if (!line.equals(""))
//					{
//						isProcess = true;
//						cnt = 100;
//					}
//				}
//			}
//			catch (IOException e) {e.printStackTrace();}
//			try {Thread.sleep(10);}
//			catch (InterruptedException e) {}
//			cnt++;
//		}
//		process.destroy();
//		return isProcess;
//	}
//	private final synchronized void writeToProcess(String data) throws IOException //write data to the process
//	{
//		if (writer != null)
//		{
//			writer.write(data);
//			writer.flush();
//			writer.close();
//		}
//	}
//	private final String readFromProcess() throws IOException //>read a line of data from the process
//	{
//		String line = null;
//		if (reader != null)
//		{
//			line = reader.readLine();
//		}
//		return line;
//	}

	//	HELPERS		HELPERS		HELPERS		HELPERS		HELPERS		HELPERS		HELPERS
	class AlphabeticComparator implements Comparator<Object> 
	{
		  public int compare(Object o1, Object o2) 
		  {
		    String s1 = (String) o1;
		    String s2 = (String) o2;
		    return s1.toLowerCase().compareTo(s2.toLowerCase());
		  }
	}
	
	final String TAG = "EngineFileManager";
	public String dataEnginesPath = "";
	private BufferedReader reader = null;
	private BufferedWriter writer = null;

}
