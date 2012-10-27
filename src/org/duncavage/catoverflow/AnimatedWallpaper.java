package org.duncavage.catoverflow;

import android.os.Handler;
import android.service.wallpaper.WallpaperService;
import android.view.SurfaceHolder;

public abstract class AnimatedWallpaper extends WallpaperService {

	protected abstract class AnimationEngine extends Engine {
		private boolean mVisible;
		private Handler handler = new Handler();

		private Runnable iterator = new Runnable() {
			@Override
			public void run() {
				synchronized(this) {
					if(!mVisible) return;
				}
				iterate();
				drawFrame();
			}
		};

		@Override
		public void onDestroy() {
			super.onDestroy();
			// stop the animation
			handler.removeCallbacks(iterator);
		}

		@Override
		public void onVisibilityChanged(boolean visible) {
			mVisible = visible;
			if (visible) {
				iterate();
				drawFrame();
			} else {
				// stop the animation
				handler.removeCallbacks(iterator);
			}
		}

		@Override
		public void onSurfaceChanged(SurfaceHolder holder, int format,
				int width, int height) {
			iterate();
			drawFrame();
		}

		@Override
		public void onSurfaceDestroyed(SurfaceHolder holder) {
			mVisible = false;
			// stop the animation
			handler.removeCallbacks(iterator);
			super.onSurfaceDestroyed(holder);
		}

		@Override
		public void onOffsetsChanged(float xOffset, float yOffset,
				float xOffsetStep, float yOffsetStep, int xPixelOffset,
				int yPixelOffset) {
			iterate();
			drawFrame();
		}

		protected abstract void drawFrame();

		protected void iterate() {
			// Reschedule the next redraw in 40ms
			handler.removeCallbacks(iterator);
			if (mVisible) {
				handler.postDelayed(iterator, 1000 / 100);
			}
		}
	}
}