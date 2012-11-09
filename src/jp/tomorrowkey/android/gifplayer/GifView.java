package jp.tomorrowkey.android.gifplayer;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout.LayoutParams;

public class GifView extends View {

	public static final int IMAGE_TYPE_UNKNOWN = 0;
	public static final int IMAGE_TYPE_STATIC = 1;
	public static final int IMAGE_TYPE_DYNAMIC = 2;

	public static final int DECODE_STATUS_UNDECODE = 0;
	public static final int DECODE_STATUS_DECODING = 1;
	public static final int DECODE_STATUS_DECODED = 2;

	private GifDecoder decoder;
	private Bitmap bitmap;

	public int imageType = IMAGE_TYPE_UNKNOWN;
	public int decodeStatus = DECODE_STATUS_UNDECODE;

	private int width;
	private int height;

	private int drawAtX;
	private int drawAtY;
	
	private long time;
	private int index;

	private int resId;
	private String filePath;

	private volatile boolean playFlag = false;

	public GifView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	/**
	 * Constructor
	 */
	public GifView(Context context) {
		super(context);
	}

	private InputStream getInputStream() {
		if (filePath != null)
			try {
				return new FileInputStream(filePath);
			} catch (FileNotFoundException e) {
			}
		if (resId > 0)
			return getContext().getResources().openRawResource(resId);
		return null;
	}

	/**
	 * set gif file path
	 * 
	 * @param filePath
	 */
	public void setGif(String filePath) {
		Bitmap bitmap = BitmapFactory.decodeFile(filePath);
		setGif(filePath, bitmap);
	}

	/**
	 * set gif file path and cache image
	 * 
	 * @param filePath
	 * @param cacheImage
	 */
	public void setGif(String filePath, Bitmap cacheImage) {
		synchronized(this) {
			this.resId = 0;
			this.filePath = filePath;
			imageType = IMAGE_TYPE_UNKNOWN;
			decodeStatus = DECODE_STATUS_UNDECODE;
			playFlag = false;
			bitmap = cacheImage;
			if (bitmap != null) {
				width = bitmap.getWidth();
				height = bitmap.getHeight();
			}
		}
		setLayoutParams(new LayoutParams(width, height));
	}

	/**
	 * set gif resource id
	 * 
	 * @param resId
	 */
	public void setGif(int resId) {
		Bitmap bitmap = BitmapFactory.decodeStream(getResources().openRawResource(resId));
		setGif(resId, bitmap);
	}

	/**
	 * set gif resource id and cache image
	 * 
	 * @param resId
	 * @param cacheImage
	 */
	public void setGif(int resId, Bitmap cacheImage) {
		synchronized(this) {
			this.filePath = null;
			this.resId = resId;
			imageType = IMAGE_TYPE_UNKNOWN;
			decodeStatus = DECODE_STATUS_UNDECODE;
			playFlag = false;
			bitmap = cacheImage;
			if (bitmap != null) {
				width = bitmap.getWidth();
				height = bitmap.getHeight();
			}
		}
		setLayoutParams(new LayoutParams(width, height));
	}

	private void decode() {
		release();
		index = 0;

		decodeStatus = DECODE_STATUS_DECODING;

		new Thread() {
			@Override
			public void run() {
				GifDecoder localDecoder = null;
				synchronized(this) {
					decoder = new GifDecoder();
					localDecoder = decoder;
				}

				localDecoder.read(getInputStream());
				if (localDecoder.width == 0 || localDecoder.height == 0) {
					imageType = IMAGE_TYPE_STATIC;
				} else {
					imageType = IMAGE_TYPE_DYNAMIC;
				}
				postInvalidate();
				time = System.currentTimeMillis();
				decodeStatus = DECODE_STATUS_DECODED;
			}
		}.start();
	}

	public void release() {
		synchronized(this) {
			if (decoder != null) {
				decoder.stopDecode();
				decoder = null;
			}
		}
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		boolean localPlayFlag;
		GifDecoder localDecoder;
		synchronized(this) {
			localPlayFlag = playFlag;
			localDecoder = decoder;
		}

		if (decodeStatus == DECODE_STATUS_UNDECODE) {
			canvas.drawBitmap(bitmap, drawAtX, drawAtY, null);
			if (localPlayFlag) {
				decode();
				invalidate();
			}
		} else if (decodeStatus == DECODE_STATUS_DECODING) {
			canvas.drawBitmap(bitmap, drawAtX, drawAtY, null);
			invalidate();
		} else if (decodeStatus == DECODE_STATUS_DECODED) {
			if (imageType == IMAGE_TYPE_STATIC) {
				canvas.drawBitmap(bitmap, drawAtX, drawAtY, null);
			} else if (imageType == IMAGE_TYPE_DYNAMIC) {
				if (localPlayFlag && localDecoder != null) {
					long now = System.currentTimeMillis();

					if (time + localDecoder.getDelay(index) < now) {
						time += localDecoder.getDelay(index);
						incrementFrameIndex();
					}
					Bitmap bitmap = localDecoder.getFrame(index);
					if (bitmap != null) {
						canvas.drawBitmap(bitmap, drawAtX, drawAtY, null);
					}
					invalidate();
				} else {
					if(localDecoder == null) {
						return;
					}
					Bitmap bitmap = localDecoder.getFrame(index);
					canvas.drawBitmap(bitmap, drawAtX, drawAtY, null);
				}
			} else {
				canvas.drawBitmap(bitmap, drawAtX, drawAtY, null);
			}
		}
	}

	private void incrementFrameIndex() {
		index++;
		if (index >= decoder.getFrameCount()) {
			index = 0;
		}
	}

	private void decrementFrameIndex() {
		index--;
		if (index < 0) {
			index = decoder.getFrameCount() - 1;
		}
	}

	public void play() {
		time = System.currentTimeMillis();
		playFlag = true;
		invalidate();
	}

	public void pause() {
		playFlag = false;
		invalidate();
	}

	public void stop() {
		playFlag = false;
		index = 0;
		invalidate();
	}

	public void nextFrame() {
		if (decodeStatus == DECODE_STATUS_DECODED) {
			incrementFrameIndex();
			invalidate();
		}
	}

	public void prevFrame() {
		if (decodeStatus == DECODE_STATUS_DECODED) {
			decrementFrameIndex();
			invalidate();
		}
	}
	
	public int getBitmapWidth() {
		return width;
	}
	
	public int getBitmapHeight() {
		return height;
	}
	
	public void setDrawAtX(int x) {
		drawAtX = x;
	}
	
	public int getDrawAtX() {
		return drawAtX;
	}

	public void setDrawAtY(int y) {
		drawAtY = y;
	}
	
	public int getDrawAtY() {
		return drawAtY;
	}

	public Bitmap getBitmap() {
		return bitmap;
	}
}