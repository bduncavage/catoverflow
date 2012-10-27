package org.duncavage.catoverflow;

import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class CatNapper {
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private ScheduledFuture catBeamerHandle;
	
	private String currentVersion;
	private OnCatNappingCompleteListener listener;
	
	private final Runnable catBeamer = new Runnable() {
		public void run() {
			// get cats
			Vector<String> cats = CatTransporter.getCatList("http://catoverflow.com/db/gifs.catdb", CatNapper.this.currentVersion);
			listener.OnComplete(cats);
		}
	};
	
	public void startNappingCats(String currentVersion, OnCatNappingCompleteListener listener)
	{
		this.currentVersion = currentVersion;
		this.listener = listener;
		catBeamerHandle = scheduler.scheduleAtFixedRate(catBeamer, 0, 60 * 60 * 24, TimeUnit.SECONDS);
	}
	
	public void stopNappingCats()
	{
		catBeamerHandle.cancel(true);
	}
	
	public static abstract class OnCatNappingCompleteListener {
		public abstract void OnComplete(Vector<String> cats);
	}
}
