package co.gramlich.battleship.shared;



import android.content.Context;
import android.media.MediaPlayer;

import co.gramlich.battleship.R;
import co.gramlich.battleship.SettingsActivity;

public class SoundFX {

	private MediaPlayer leftGun;
	private MediaPlayer rightGun;
	private MediaPlayer planeExplode;
	private MediaPlayer subExplode;
	private MediaPlayer dcBeep;
	private Context context;

	public SoundFX(Context context) {
		this.context = context;
		leftGun = MediaPlayer.create(context, R.raw.left_gun);
		rightGun = MediaPlayer.create(context, R.raw.right_gun);
		planeExplode = MediaPlayer.create(context, R.raw.plane_explode);
		subExplode = MediaPlayer.create(context, R.raw.sub_explode);
		dcBeep = MediaPlayer.create(context, R.raw.depth_charge);
	}

	public void leftGun() {
		if (SettingsActivity.getSoundFX(context)) {
			leftGun.start();
		}
	}

	public void rightGun() {
		if (SettingsActivity.getSoundFX(context)) {
			rightGun.start();
		}
	}


	public void planeExplode() {
		if (SettingsActivity.getSoundFX(context)) {
			planeExplode.start();
		}
	}

	public void subExplode() {
		if (SettingsActivity.getSoundFX(context)) {
			subExplode.start();
		}
	}

	public void dcBeep() {
		if (SettingsActivity.getSoundFX(context)) {
			dcBeep.start();
		}
	}

}
