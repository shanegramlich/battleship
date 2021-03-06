package co.gramlich.battleship.shared;

import java.util.LinkedList;
import java.util.List;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.MotionEvent;
import android.view.View;


import co.gramlich.battleship.BattleshipActivity;
import co.gramlich.battleship.R;
import co.gramlich.battleship.SettingsActivity;
import co.gramlich.battleship.ScoreBoardActivity;
import co.gramlich.battleship.skins.Skins;
import co.gramlich.battleship.skins.RetroSkin;
import co.gramlich.battleship.skins.SteroidSkin;
import co.gramlich.battleship.sprites.Direction;
import co.gramlich.battleship.sprites.FakeQueue;
import co.gramlich.battleship.sprites.Sprite;
import co.gramlich.battleship.sprites.Airplane;
import co.gramlich.battleship.sprites.Battleship;
import co.gramlich.battleship.sprites.Bullet;
import co.gramlich.battleship.sprites.DepthCharge;
import co.gramlich.battleship.sprites.Enemy;
import co.gramlich.battleship.sprites.Gunsmoke;
import co.gramlich.battleship.sprites.Submarine;


public class GameView extends View {
    public static Skins skin;
    private static int seaLevel;
    boolean initialized = false;
    float timerTextWidth;
    boolean gameOver;
    private Timer timer;
    private boolean paused;
    private boolean backgrounded;
    private List<Airplane> airplanes = new LinkedList<>();
    private List<Submarine> submarines = new LinkedList<>();
    private Paint paint = new Paint();
    private Bitmap water;
    private Battleship battleship;
    private long timeLeft;
    private int score;
    private Canvas canvas;
    private FakeQueue<Bullet> bullets = new FakeQueue<>();
    private FakeQueue<DepthCharge> depthCharges = new FakeQueue<>();
    private Gunsmoke leftGunsmoke;
    private Gunsmoke rightGunsmoke;
    private boolean showLeftGunsmoke;
    private boolean showRightGunsmoke;
    private SoundFX soundFX;
    private BattleshipActivity battleshipActivity;
    private SettingsActivity settingsActivity;

    public GameView(Context context) {
        super(context);
        battleshipActivity = (BattleshipActivity) context;
        soundFX = new SoundFX(context);
        paint.setTypeface(Typeface.DEFAULT_BOLD);
        timeLeft = SettingsActivity.getGameLength(getContext());
        settingsActivity = new SettingsActivity();
        switch (settingsActivity.getSkin(context)) {
            case 1:
                skin = new RetroSkin();
                break;
            case 2:
                skin = new SteroidSkin();
                break;
        }
    }

    @Override
    public void onDraw(Canvas canvas) {
        initialize(canvas);
        drawBackground(canvas);
        drawWater(canvas);
        battleship.draw(canvas);
        drawEnemies(canvas);
        for (Bullet bullet : bullets) {
            bullet.draw(canvas);
        }
        if (showLeftGunsmoke) {
            leftGunsmoke.draw(canvas);
            showLeftGunsmoke = false;
        }
        if (showRightGunsmoke) {
            rightGunsmoke.draw(canvas);
            showRightGunsmoke = false;
        }
        for (DepthCharge depthCharge : depthCharges) {
            depthCharge.draw(canvas);
        }

        paint.setColor(skin.getTextColor());

        String scoreText = getResources().getString(R.string.score) + ": " + score;
        canvas.drawText(scoreText, 5, canvas.getHeight() / 2 - paint.ascent(), paint);

        String timerText = String.format("TIME:" + " %d:%02d", timeLeft / 60, timeLeft % 60);
        canvas.drawText(timerText, canvas.getWidth() - timerTextWidth - 5, canvas.getHeight() / 2 - paint.ascent(), paint);

        if (paused) {
            String paused1 = getResources().getString(R.string.pause_option);
            String paused2 = getResources().getString(R.string.continue_playing);
            float pausedWidth = paint.measureText(paused1);
            canvas.drawText(paused1, (getWidth() - pausedWidth) / 2, getHeight() * 0.25f, paint);
            pausedWidth = paint.measureText(paused2);
            canvas.drawText(paused2, (getWidth() - pausedWidth) / 2, getHeight() * 0.25f - paint.ascent(), paint);
        }
        //debugging
        //paint.setTextSize(10);
        //c.drawText(""+bullets.size(), 5, 10, paint);
    }

    private void initialize(Canvas canvas) {
        if (initialized) {
            return;
        }
        initialized = true;
        this.canvas = canvas;
        Sprite.canvasWidth = canvas.getWidth();
        Sprite.canvasHeight = canvas.getHeight();
        if (Math.min(Sprite.canvasHeight, Sprite.canvasWidth) < 400) {
            paint.setTextSize(20);
        } else {
            paint.setTextSize(40);
        }
        timerTextWidth = paint.measureText(getResources().getString(R.string.time) + ": 0:00");
        battleship = Battleship.getBattleship(canvas);
        battleship.setBottom(canvas.getHeight() / 2);
        battleship.setCenterX(canvas.getWidth() / 2);

        leftGunsmoke = new Gunsmoke(canvas);
        leftGunsmoke.setBottom(battleship.getRightGunPosition().y);
        leftGunsmoke.setRight(battleship.getLeftGunPosition().x);

        rightGunsmoke = new Gunsmoke(canvas);
        rightGunsmoke.setBottom(battleship.getRightGunPosition().y);
        rightGunsmoke.setLeft(battleship.getRightGunPosition().x);

        for (int i = 0; i < SettingsActivity.getNumPlanes(getContext()); ++i) {
            airplanes.add(new Airplane(canvas));
        }
        for (int i = 0; i < SettingsActivity.getNumSubs(getContext()); ++i) {
            submarines.add(new Submarine(canvas));
        }
        timer = new Timer();
        timer.subscribe(airplanes);
        timer.subscribe(submarines);
        //timer.subscribe(this);
    }

    private void drawBackground(Canvas canvas) {
        if (gameOver || paused) {
            canvas.drawColor(Color.YELLOW);
        } else {
            canvas.drawColor(Color.WHITE);
        }
    }

    private void drawWater(Canvas canvas) {
        water = BattleshipActivity.loadBitmap(skin.getWater());
        seaLevel = setSeaLevel(canvas);
        float ww = water.getScaledWidth(canvas);
        for (int x = 0; x < canvas.getWidth(); x += ww) {
            canvas.drawBitmap(water, x, seaLevel, paint);
        }
    }

    private int setSeaLevel(Canvas canvas) {
        return canvas.getHeight() / 2 - water.getScaledHeight(canvas);
    }

    public static int getSeaLevel() {
        return seaLevel;
    }

    private void drawEnemies(Canvas canvas) {
        for (Airplane airplane : airplanes) {
            airplane.draw(canvas);
        }
        for (Submarine submarine : submarines) {
            submarine.draw(canvas);
        }
    }

    public void restart() {
        gameOver = false;
        score = 0;
        timeLeft = SettingsActivity.getGameLength(getContext());
        paused = false;
        backgrounded = false;
        showLeftGunsmoke = false;
        showRightGunsmoke = false;
        timer.unsubscribe(bullets);
        bullets.clear();
        timer.unsubscribe(depthCharges);
        depthCharges.clear();
        timer.restart();
    }

    public boolean isPaused() {
        return paused;
    }

    public void pauseButtonClicked() {
        paused = !paused;
        invalidate();
    }

    public void goToBackground(boolean bg) {
        backgrounded = bg;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!paused) {
            float x = 0, y = 0;
            if (event.getAction() == MotionEvent.ACTION_DOWN
                    ||
                    (event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_DOWN) {
                int pointerCount = event.getPointerCount();
                for (int p = 0; p < pointerCount; p++) {
                    x = event.getX(p);
                    y = event.getY(p);
                    if (y < getHeight() / 2) {
                        if (x < getWidth() / 2) {
                            fireLeftGun();
                        } else {
                            fireRightGun();
                        }
                    } else {
                        dropDepthCharge();
                    }
                }
            }
        } else {
            paused = false;
            invalidate();
        }
        return true;
    }

    private void fireRightGun() {
        //		float magic = 168f/183f;
        //		float y = Sprite.canvasHeight/2 - battleship.getHeight();
        //		float x = (Sprite.canvasWidth - battleship.getWidth())/2 + battleship.getWidth()*magic;
        if (newBullet(battleship.getRightGunPosition(), Direction.RIGHT)) {
            //			gunsmoke.setBottom(y);
            //			gunsmoke.setLeft(x);
            showRightGunsmoke = true;
            soundFX.rightGun();
        }
    }

    private void fireLeftGun() {
        //		float magic = 23f/183f;
        //		float y = Sprite.canvasHeight/2 - battleship.getHeight();
        //		float x = (Sprite.canvasWidth - battleship.getWidth())/2 + battleship.getWidth()*magic;
        if (newBullet(battleship.getLeftGunPosition(), Direction.LEFT)) {
            //			gunsmoke.setBottom(y);
            //			gunsmoke.setRight(x);
            showLeftGunsmoke = true;
            soundFX.leftGun();
        }
    }

    private boolean newBullet(PointF pointF, Direction direction) {
        if (!(SettingsActivity.getRapidGuns(getContext()))) {
            //			Bullet existing = bullets.peekLast();
            //			if (existing != null
            //					&& existing.getDirection() == d
            //					&& existing.isVisible()) {
            //				return false;
            //			}
            List<Bullet> existing = bullets.peekLast2();
            if (existing != null) {
                Bullet bullet1 = existing.get(0);
                Bullet bullet2 = existing.get(1);
                if ((bullet1.getDirection() == direction && bullet1.isVisible())
                        || (bullet2.getDirection() == direction && bullet2.isVisible())) {
                    return false;
                }
            }
        }
        Bullet b = new Bullet(new PointF(pointF.x, pointF.y), direction);
        timer.subscribe(b);
        Bullet doomed = bullets.add(b);
        if (doomed != null) {
            timer.unsubscribe(doomed);
        }
        return true;
    }

    private void dropDepthCharge() {
        //if rapid-fire is off, AND if there's already
        //a depth-charge sinking, then don't do anything
        if (!(SettingsActivity.getRapidDC(getContext()))) {
            DepthCharge existing = depthCharges.peekLast();
            if (existing != null) {
                if (existing.isSinking()) {
                    return;
                }
            }
        }
        //Otherwise, launch a new depth-charge
        DepthCharge b = new DepthCharge(canvas);
        timer.subscribe(b);
        DepthCharge doomed = depthCharges.add(b);
        if (doomed != null) {
            timer.unsubscribe(doomed);
        }

    }

    private void checkForCollisions() {
        Bullet used = null;
        DepthCharge detonatedDepthCharge = null;
        for (Enemy enemyPlane : airplanes) {
            for (Bullet b : bullets) {
                if (enemyPlane.collidesWith(b)) {
                    used = b;
                    //if (e.collidesWith(leftBullet) || e.collidesWith(rightBullet)) {
                    score += enemyPlane.getPointValue();
                    enemyPlane.explode();
                    soundFX.planeExplode();
                    break;//nice try
                }
            }
            bullets.remove(used);
        }
        for (Enemy enemySubmarine : submarines) {
            for (DepthCharge depthCharge : depthCharges) {
                if (enemySubmarine.collidesWith(depthCharge)) {
                    detonatedDepthCharge = depthCharge;
                    score += enemySubmarine.getPointValue();
                    enemySubmarine.explode();
                    soundFX.subExplode();
                    //bomb = null;
                    break;
                }
            }
            depthCharges.remove(detonatedDepthCharge);
        }

    }

    public void stop() {
        timer.removeMessages(0);
    }

    public void resume() {
        timer.restart();
    }

//	@Override
//	public void tick() {
//		if (gameOver) {
//			Bundle bundle = new Bundle();
//			bundle.putInt("score", score);
//			Intent newIntent = new Intent(GameView.this.getContext(), ScoreBoardActivity.class);
//			newIntent.putExtras(bundle);
//			timer.removeMessages(0);
//			battleshipActivity.startActivityForResult(newIntent, BattleshipActivity.HIGH_SCORE_DIALOG);
//			return;
//		}
//		if (!(paused || backgrounded)) {
//			timer.timeNow = System.currentTimeMillis();
//			if (timer.timeNow - timer.timeBefore >= 1000) {
//				--timeLeft;
//				timer.timeBefore = timer.timeNow;
//			}
//			if (timeLeft < 1) {
//				gameOver = true;
//				backgrounded = true;
//				invalidate();
//			}
//			checkForCollisions();
//			invalidate();
//		}
//	}

    //	private void showToast(String msg) {
    //		Toast toast = Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT);
    //		toast.setGravity(Gravity.CENTER, 0, 0);
    //		toast.show();
    //	}


    private class Timer extends Handler {

        long timeNow, timeBefore;
        private List<TickListener> listeners;

        public Timer() {
            super();
            listeners = new LinkedList<TickListener>();
            restart();
        }

        public void restart() {
            timeBefore = System.currentTimeMillis();
            handleMessage(obtainMessage(0));
        }

        public void subscribe(TickListener tl) {
            listeners.add(tl);
        }

        public void subscribe(List<? extends TickListener> tls) {
            listeners.addAll(tls);
        }

        public void unsubscribe(TickListener tl) {
            listeners.remove(tl);
        }

        public void unsubscribe(Iterable<? extends TickListener> tickListeners) {
            for (TickListener tickListener : tickListeners) {
                listeners.remove(tickListener);
            }
        }

        @Override
        public void handleMessage(Message m) {
            if (gameOver) {
                Bundle bundle = new Bundle();
                bundle.putInt("score", score);
                Intent newIntent = new Intent(GameView.this.getContext(), ScoreBoardActivity.class);
                newIntent.putExtras(bundle);
                timer.removeMessages(0);
                battleshipActivity.startActivityForResult(newIntent, BattleshipActivity.HIGH_SCORE_DIALOG);
                return;
            }
            if (!(paused || backgrounded)) {
                timeNow = System.currentTimeMillis();
                if (timeNow - timeBefore >= 1000) {
                    --timeLeft;
                    timeBefore = timer.timeNow;
                }
                if (timeLeft < 1) {
                    gameOver = true;
                    backgrounded = true;
                    invalidate();
                }
                checkForCollisions();
                invalidate();
            }

            for (TickListener tickListener : listeners) {
                tickListener.tick();
            }
            removeMessages(0);
            sendMessageDelayed(obtainMessage(0), 50);
        }

    }

}

