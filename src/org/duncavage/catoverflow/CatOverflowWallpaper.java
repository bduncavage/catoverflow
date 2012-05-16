package org.duncavage.catoverflow;

import java.io.File;
import java.util.Enumeration;
import java.util.Vector;

import org.duncavage.catoverflow.CatNapper.OnCatNappingCompleteListener;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Handler;
import android.view.SurfaceHolder;

public class CatOverflowWallpaper extends AnimatedWallpaper {
	private static final String CAT_PREFS = "CatsPreferCheezburgers";
	private static final String CAT_VERSION_KEY = "catVersion";
	private static final String CAT_STORE = "kennel";
	
	private CatOverflowEngine engine = new CatOverflowEngine();
	private CatNapper cat_napper = new CatNapper();
	
	private Vector<String> recentlyNappedCats;
	
	@Override
	public void onCreate() {
		android.os.Debug.waitForDebugger(); 
		
		final SharedPreferences settings = getSharedPreferences(CAT_PREFS, 0);
	    final String catVersion = settings.getString(CAT_VERSION_KEY, "");
	    
	    
	    
		cat_napper.startNappingCats(catVersion,  new OnCatNappingCompleteListener() {
			public void OnComplete(Vector<String> cats) {
				recentlyNappedCats = cats;
				if(cats != null) {
					Editor editor = settings.edit();
					//editor.putString(CAT_VERSION_KEY, cats.elementAt(0));
					editor.putString(CAT_VERSION_KEY, "");
					editor.commit();
					processCats(new OnNewCatsAdoptedListener() {
						@Override
						public void OnAdpoptionComplete() {
							
						}
					});
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
		return engine;
	}
	
	private void processCats(final OnNewCatsAdoptedListener listener) {
		// first get the cats we already have
		File catStore = getDir(CAT_STORE, MODE_PRIVATE);
		File[] catFiles = catStore.listFiles();
		Vector<String> storedCats = new Vector<String>(catFiles.length);
		// wasteful? maybe, but i'm lazy
		for(int i = 0; i < catFiles.length; i++) {
			storedCats.add(catFiles[i].getName());
		}
		
		Vector<String> catsToDestroy = new Vector<String>();
		final Vector<String> catsToAdopt = new Vector<String>();
		
		for(int i = 0; i < recentlyNappedCats.size(); i++) {
			Uri catUri = Uri.parse(recentlyNappedCats.elementAt(i));
			String catName = catUri.getLastPathSegment();
			if(!storedCats.contains(catName)) {
				catsToAdopt.add(catUri.toString());
			}
			if(!recentlyNappedCats.contains(storedCats.elementAt(i))) {
				catsToDestroy.add(storedCats.elementAt(i));
			}
		}
		
		for(int i = 0; i < catsToDestroy.size(); i++) {
			File deadCat = new File(catStore.getAbsolutePath() + catsToDestroy.elementAt(i));
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
		File catStore = getDir(CAT_STORE, MODE_PRIVATE);
		while(enumeratedCats.hasMoreElements()) {
			CatTransporter.adoptCat(enumeratedCats.nextElement(), catStore);
		}
		
		new Handler().post(new Runnable() {
			public void run() {
				listener.OnAdpoptionComplete();
			}
		});
	}

	private class CatOverflowEngine extends AnimationEngine {
		
		private int offset_x;
		private int offset_y;
		private int height;
		private int width;
		private int visible_width;
		
		@Override
		public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height)
		{
			this.height = height;
			   if (this.isPreview()) {
			      this.width = width;
			   } else {
			      this.width = 2 * width;
			   }
			   this.visible_width = width;
			 
			   super.onSurfaceChanged(holder, format, width, height);
		}
		
		public void onOffsetsChanged(float xOffset, float yOffset,
			      float xOffsetStep, float yOffsetStep, int xPixelOffset,
			      int yPixelOffset) 
		{
			   // store the offsets
			   this.offset_x = xPixelOffset;
			   this.offset_y = yPixelOffset;
			 
			   super.onOffsetsChanged(xOffset, yOffset, xOffsetStep, yOffsetStep,
			         xPixelOffset, yPixelOffset);
		}
		
		@Override
		protected void drawFrame() {
			// TODO Auto-generated method stub
			
		}
	}
	
	private abstract class OnNewCatsAdoptedListener {
		public abstract void OnAdpoptionComplete();
	}
}
