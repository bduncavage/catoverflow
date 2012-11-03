package org.duncavage.catoverflow;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class CatAnimator {
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private ScheduledFuture catAnimatorHandle;
	private CatOverflowWallpaper.CatOverflowEngine engine;
	
	public CatAnimator(CatOverflowWallpaper.CatOverflowEngine engine) {
		this.engine = engine;
	}
	
	private final Runnable catAnimatorRunnable = new Runnable() {
		public void run() {
			// choose a random cat to animate
			engine.animateRandomCat();
		}
	};
	
	public void startAnimating()
	{
		catAnimatorHandle = scheduler.scheduleAtFixedRate(catAnimatorRunnable, 0, 20, TimeUnit.SECONDS);
	}
	
	public void stopAnimating() {
		catAnimatorHandle.cancel(true);
	}
}
