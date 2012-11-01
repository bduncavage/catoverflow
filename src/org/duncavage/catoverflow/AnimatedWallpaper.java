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
				drawFrame();
			} else {
				// stop the animation
				handler.removeCallbacks(iterator);
			}
		}

		@Override
		public void onSurfaceChanged(SurfaceHolder holder, int format,
				int width, int height) {
			super.onSurfaceChanged(holder, format, width, height);
			drawFrame();
		}

		@Override
		public void onSurfaceDestroyed(SurfaceHolder holder) {
			super.onSurfaceDestroyed(holder);
			mVisible = false;
			// stop the animation
			handler.removeCallbacks(iterator);
		}

		@Override
		public void onOffsetsChanged(float xOffset, float yOffset,
				float xOffsetStep, float yOffsetStep, int xPixelOffset,
				int yPixelOffset) {
			drawFrame();
		}

		protected abstract void drawFrame();

		protected void iterate() {
			// Reschedule the next redraw in 40ms
			handler.removeCallbacks(iterator);
			if (mVisible) {
				handler.postDelayed(iterator, 1000 / 25);
			}
		}
	}
}