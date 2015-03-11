package ccc.chess.gui.chessforall;

import java.util.HashMap;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

public class SoundManager 
{
	private  SoundPool mSoundPool; 
	private  HashMap<Integer, Integer> mSoundPoolMap; 
	private  Context mContext;
	float volume = 0.2f;
	public void initSounds(Context theContext) 
	{ 
		mContext = theContext;
		mSoundPool = new SoundPool(4, AudioManager.STREAM_MUSIC, 0); 
		mSoundPoolMap = new HashMap<Integer, Integer>(); 
	} 
	public void addSound(int Index,int SoundID)
	{
		mSoundPoolMap.put(Index, mSoundPool.load(mContext, SoundID, 1));
	}
	public void playSound(int index) 
	{ 
	    mSoundPool.play(mSoundPoolMap.get(index), volume, volume, 1, 0, 1f); 
	}
	public void setVolume(float vol) {volume = vol;}
}
