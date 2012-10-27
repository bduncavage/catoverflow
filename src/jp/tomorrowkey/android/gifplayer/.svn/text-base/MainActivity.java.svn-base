package jp.tomorrowkey.android.gifplayer;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity implements OnClickListener {

	private GifView gifView;
	private Button btnPlay;
	private Button btnPause;
	private Button btnStop;
	private Button btnPrevFrame;
	private Button btnNextFrame;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		gifView = (GifView) findViewById(R.id.gifView);
		btnPlay = (Button) findViewById(R.id.btnPlay);
		btnPause = (Button) findViewById(R.id.btnPause);
		btnStop = (Button) findViewById(R.id.btnStop);
		btnPrevFrame = (Button) findViewById(R.id.btnPrevFrame);
		btnNextFrame = (Button) findViewById(R.id.btnNextFrame);

		gifView.setGif(R.drawable.break_droid);
		btnPlay.setOnClickListener(this);
		btnPause.setOnClickListener(this);
		btnStop.setOnClickListener(this);
		btnPrevFrame.setOnClickListener(this);
		btnNextFrame.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		if (id == R.id.btnPlay) {
			gifView.play();
		} else if (id == R.id.btnPause) {
			gifView.pause();
		} else if (id == R.id.btnStop) {
			gifView.stop();
		} else if (id == R.id.btnPrevFrame) {
			gifView.prevFrame();
		} else if (id == R.id.btnNextFrame) {
			gifView.nextFrame();
		}
	}
}