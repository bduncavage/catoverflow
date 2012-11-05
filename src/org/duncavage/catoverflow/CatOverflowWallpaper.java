package org.duncavage.catoverflow;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Vector;

import jp.tomorrowkey.android.gifplayer.GifView;

import org.duncavage.catoverflow.CatNapper.OnCatNappingCompleteListener;
import org.duncavage.catoverflow.util.CatUtil;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

public class CatOverflowWallpaper extends AnimatedWallpaper {
	private static final String TAG = "CatOverflowWallpaper";
	
	private static final String CAT_PREFS = "CatsPreferCheezburgers";
	private static final String CAT_VERSION_KEY = "catVersion";
	
	private String externalDirPath;
	
	private Handler main_thread_handler = new Handler();
	
	private CatOverflowEngine engine;
	private CatNapper cat_napper = new CatNapper();
	private CatAnimator catAnimator;
	
	private Vector<String> recentlyNappedCats;
	
	private boolean transportComplete;

	@Override
	public void onCreate() {
		android.os.Debug.waitForDebugger(); 
		final SharedPreferences settings = getSharedPreferences(CAT_PREFS, 0);
	    final String catVersion = settings.getString(CAT_VERSION_KEY, "");
	    
	    externalDirPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Android/data/org.duncavage.catoverflow/files/";
	    
	    File storageDir = new File(externalDirPath);
	    if(!storageDir.exists()) {
	    	try {
	    		storageDir.mkdirs();
	    	} catch (Exception e) {
	    		Log.e(TAG, "Exception creating storage dir: " + e.getLocalizedMessage());
	    	}
	    }
	    
		cat_napper.startNappingCats(catVersion,  new OnCatNappingCompleteListener() {
			public void OnComplete(Vector<String> cats) {
				recentlyNappedCats = cats;
				if(cats != null) {
					Editor editor = settings.edit();
					editor.putString(CAT_VERSION_KEY, cats.elementAt(0));
					//editor.putString(CAT_VERSION_KEY, "");
					editor.commit();
					processCats(new OnNewCatsAdoptedListener() {
						@Override
						public void OnAdpoptionComplete() {
							engine.catTransportComplete();
							transportComplete = true;
						}
					});
				} else {
					engine.catTransportComplete();
					transportComplete = true;
				}
			}
		});
		
		super.onCreate();
	}

	@Override
	public void onDestroy() {
		cat_napper.stopNappingCats();
		super.onDestroy();
	}

	@Override
	public Engine onCreateEngine() {
		engine = new CatOverflowEngine();
		catAnimator = new CatAnimator(engine);
		catAnimator.startAnimating();
		if (transportComplete) {
			engine.catTransportComplete();
		}
		return engine;
	}
	
	private void processCats(final OnNewCatsAdoptedListener listener) {
		// first get the cats we already have
		File catStore = new File(externalDirPath);
		File[] catFiles = catStore.listFiles();
		Vector<String> storedCats = new Vector<String>();
		
		int storedCatsLength = 0;
		if(catFiles != null) {
			storedCatsLength = catFiles.length;
		}
		for(int i = 0; i < storedCatsLength; i++) {
			storedCats.add(catFiles[i].getName());
		}
		
		Vector<String> catsFromServer = new Vector<String>(recentlyNappedCats.size());
		// new vector with sanitized names
		String sanitizedName = null;
		for(int i = 0; i < recentlyNappedCats.size(); i++) {
			sanitizedName = Uri.parse(recentlyNappedCats.elementAt(i)).getLastPathSegment();
			catsFromServer.add(sanitizedName);
		}
		
		Vector<String> catsToDestroy = new Vector<String>();
		final Vector<String> catsToAdopt = new Vector<String>();
		// skip over the first element which is not a cat
		for(int i = 1; i < catsFromServer.size(); i++) {
			String catName = catsFromServer.elementAt(i);
			if(!storedCats.contains(catName)) {
				catsToAdopt.add(Uri.parse(recentlyNappedCats.elementAt(i)).toString().trim());
			}
			if(i < storedCats.size() && !catsFromServer.contains(storedCats.elementAt(i))) {
				catsToDestroy.add(storedCats.elementAt(i));
			}
		}
		
		for(int i = 0; i < catsToDestroy.size(); i++) {
			File deadCat = new File(catStore.getAbsolutePath() + "/" + catsToDestroy.elementAt(i));
			deadCat.delete();
		}
		
		new Thread(new Runnable() {
			public void run() {
				adpotCats(catsToAdopt, listener);
			}
		}).start();
	}
	
	private void adpotCats(Vector<String> catsToAdopt, final OnNewCatsAdoptedListener listener) {
		Enumeration<String> enumeratedCats = catsToAdopt.elements();
		File catStore = new File(externalDirPath);
		while(enumeratedCats.hasMoreElements()) {
			CatTransporter.adoptCat(enumeratedCats.nextElement(), catStore);
		}
		
		main_thread_handler.post(new Runnable() {
			public void run() {
				listener.OnAdpoptionComplete();
			}
		});
	}

	public class CatOverflowEngine extends AnimationEngine {
		
		private int offset_x;
		private int height;
		private int width;
		private int upperBoundIndex;
		private int lastAnimatedIndex = -1;
		
		private String[] cats;

		private ArrayList<GifView> catGifViews;
		private final Object monitor = new Object();
		
		Paint mPaint = new Paint();
		PorterDuffXfermode mPorterDuffClear = new PorterDuffXfermode(PorterDuff.Mode.CLEAR);
		PorterDuffXfermode mPorterDuffSrc = new PorterDuffXfermode(PorterDuff.Mode.SRC);
	
		private volatile boolean mStopDrawingCats = false;

		private final int MAX_CATS_IN_MEMORY = 15;

		private Bitmap mBackgroundBmp;

		private Handler mMainThreadHandler;

		@Override
		public void onCreate(SurfaceHolder holder) {
			super.onCreate(holder);
			mMainThreadHandler = new Handler();
			setTouchEventsEnabled(true);
		}
		
		public void catTransportComplete()
		{
			File[] files = new File(CatOverflowWallpaper.this.externalDirPath).listFiles();
			cats = new String[files.length];
			
			for(int i = 0; i < files.length; i++) {
				cats[i] = files[i].getAbsolutePath();
			}
			
			prepareCatViews();
		}

		private void prepareCatViews() {
			synchronized(monitor) {
				mStopDrawingCats = true;
			}

			CatUtil.knuthShuffle(cats);

			final int safeSize = Math.min(MAX_CATS_IN_MEMORY, cats.length);
			catGifViews = new ArrayList<GifView>(safeSize);
			
			final Context context = CatOverflowWallpaper.this;
			
			new Thread() {
				@Override
				public void run() {
					GifView view = null;
					String catFile = null;

					for(int i = 0; i < safeSize; i++) {
						view = new GifView(context);
						
						synchronized(monitor) {
							catFile = cats[i];
						}
						
						view.setGif(catFile);

						synchronized(monitor) {
							catGifViews.add(view);
						}
					}
					synchronized(monitor) {
						mStopDrawingCats = false;
					}
				}
			}.start();
		}
		
		@Override
		public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height)
		{
			this.height = height;
			if (this.isPreview()) {
				this.width = width;
				mStopDrawingCats = true;
				synchronized(monitor) {
					mBackgroundBmp = BitmapFactory.decodeResource(getResources(), R.drawable.background);
					float bh = mBackgroundBmp.getHeight();
					float bw = mBackgroundBmp.getWidth();
					float scaledHeight = bh * (width/ bw);

					mBackgroundBmp = Bitmap.createScaledBitmap(mBackgroundBmp, 2 * width, 2 * (int)scaledHeight, true);
				}
			} else {
				synchronized(monitor) {
					mBackgroundBmp = null;
				}
				mStopDrawingCats = false;
				this.width = 2 * width;
			}
			super.onSurfaceChanged(holder, format, width, height);
		}
		
		public void onOffsetsChanged(float xOffset, float yOffset,
			      float xOffsetStep, float yOffsetStep, int xPixelOffset,
			      int yPixelOffset) 
		{
		   // store the offsets
		   this.offset_x = xPixelOffset;
		 
		   super.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep,
		         xPixelOffset, yPixelOffset);
		}
		
		@Override
		protected void drawFrame() {
			boolean shouldDraw = true;
			ArrayList<GifView> localGifViews = null;
			synchronized(monitor) {
				if(catGifViews == null && !isPreview()) {
					shouldDraw = false;
				}
				localGifViews = catGifViews;
			}
			
			if(!shouldDraw) {
				iterate();
				return;
			}
			
			SurfaceHolder holder = getSurfaceHolder();
			
			Canvas c = null;
			try {
				c = holder.lockCanvas();
				if (c != null) {
					if (mBackgroundBmp != null) {
						int left = (width / 2) - (mBackgroundBmp.getWidth() / 2);
						int top = (height / 2) - (mBackgroundBmp.getHeight() / 2);
						c.drawBitmap(mBackgroundBmp, left, top, null);
					} else {
						// Clear the canvas
						// Totally obvious way to do that right?
						mPaint.setXfermode(mPorterDuffClear);
						c.drawPaint(mPaint);
						mPaint.setXfermode(mPorterDuffSrc);
					}

					if (mStopDrawingCats || isPreview()) {
						iterate();
						return;
					}

					int currentX = offset_x;
					int currentY = 0;
					int rowMaxHeight = 0;
					
					GifView view;
					GifView nextView;
					
					for(int i = 0; i < localGifViews.size(); i++) {
						view = localGifViews.get(i);
						view.setDrawAtX(currentX);
						view.setDrawAtY(currentY);
						view.draw(c);

						currentX += view.getBitmapWidth();
						rowMaxHeight = Math.max(view.getBitmapHeight(), rowMaxHeight);
						if(i + 1 < localGifViews.size()) {
							nextView = localGifViews.get(i + 1);
							if(nextView.getBitmapWidth() + currentX > this.width - Math.abs(offset_x)) {
								currentX = offset_x;
								currentY += rowMaxHeight;
								rowMaxHeight = 0;
								if(currentY > this.height) {
									upperBoundIndex = i;
									break;
								}
							}
						}
					}
				}
			} finally {
				if(c != null) {
					holder.unlockCanvasAndPost(c);
				}
			}
			iterate();
		}
		
		@Override
		public void onTouchEvent(MotionEvent event) {
			super.onTouchEvent(event);
			//catGifViews.get(0).play();
		}

		public void animateRandomCat() {
			synchronized(monitor) {
				if (catGifViews == null || catGifViews.size() < upperBoundIndex) {
					return;
				}
				if (upperBoundIndex > 0) {
					int randIndex = (int) (Math.random() * (upperBoundIndex + 1));
					resetAnimatedCat();
					Log.i(TAG, "Will animate cat at index: " + randIndex);
					catGifViews.get(randIndex).play();
					lastAnimatedIndex = randIndex;
				}
			}
		}

		public void resetAnimatedCat() {
			synchronized(monitor) {
				if (lastAnimatedIndex > -1) {
					GifView view = catGifViews.get(lastAnimatedIndex);
					view.stop();
					view.release();
					view = new GifView(CatOverflowWallpaper.this);
					view.setGif(cats[lastAnimatedIndex]);
					catGifViews.set(lastAnimatedIndex, view);
					lastAnimatedIndex = -1;
				}
			}
		}

		public void randomizeCats() {
			resetAnimatedCat();
			prepareCatViews();
		}
	}
	
	private abstract class OnNewCatsAdoptedListener {
		/**
		 * Called when there are no more cats to retrieve from the server.
		 */
		public abstract void OnAdpoptionComplete();
	}
}
