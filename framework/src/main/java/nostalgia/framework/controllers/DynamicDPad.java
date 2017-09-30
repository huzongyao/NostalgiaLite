package nostalgia.framework.controllers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;

import nostalgia.framework.Emulator;
import nostalgia.framework.EmulatorController;
import nostalgia.framework.R;
import nostalgia.framework.ui.gamegallery.GameDescription;
import nostalgia.framework.ui.preferences.PreferenceUtil;

public class DynamicDPad implements EmulatorController {

    private int leftMapped;
    private int rightMapped;
    private int upMapped;
    private int downMapped;
    private SparseIntArray mapping;
    private int port;
    private Emulator emulator;
    private View view;
    private Context context;
    private float dpadCenterX = -1;
    private float dpadCenterY = -1;
    private float currentX;
    private float currentY;
    private TouchController touchController;
    private int minDistX = -1;
    private int minDistY = -1;
    private Paint paint = new Paint();

    public DynamicDPad(Context context, Display display, TouchController touchController) {
        this.context = context;
        this.touchController = touchController;
        DisplayMetrics dm = new DisplayMetrics();
        display.getMetrics(dm);
        float xdpi = dm.xdpi;
        float ydpi = dm.ydpi;
        float xpcm = xdpi / 2.54f;
        float ypcm = ydpi / 2.54f;
        float minDistCm = 0.2f;
        minDistX = (int) (minDistCm * xpcm);
        minDistY = (int) (minDistCm * ypcm);
    }

    @Override
    public void onResume() {
        paint.setAlpha(PreferenceUtil.getControlsOpacity(context));
    }

    @Override
    public void onPause() {
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
    }

    @Override
    public void onGameStarted(GameDescription game) {
    }

    @Override
    public void onGamePaused(GameDescription game) {
    }

    @Override
    public void connectToEmulator(int port, Emulator emulator) {
        this.emulator = emulator;
        this.port = port;
        mapping = emulator.getInfo().getKeyMapping();
        leftMapped = mapping.get(EmulatorController.KEY_LEFT);
        rightMapped = mapping.get(EmulatorController.KEY_RIGHT);
        downMapped = mapping.get(EmulatorController.KEY_DOWN);
        upMapped = mapping.get(EmulatorController.KEY_UP);
    }

    @Override
    public View getView() {
        if (view == null) {
            view = new DPadView(context);
        }

        return view;
    }

    @Override
    public void onDestroy() {
        context = null;
        touchController = null;
    }

    private class DPadView extends View {

        private final int ANGLE = 18;
        Bitmap[] direction = new Bitmap[8];
        int btnW = -1;
        int btnH = -1;
        private int directionIdx = -1;
        private float u1 = 0.0174532925f * ANGLE;
        private float u2 = 0.0174532925f * (90 - ANGLE);
        private float TAN_DIAGONAL_MIN = (float) Math.tan(u1);
        private float TAN_DIAGONAL_MAX = (float) Math.tan(u2);
        private int activePointerId = -1;
        private int optimizationCounter = 0;
        private int vibrationDuration = 100;
        private Vibrator vibrator;

        public DPadView(Context context) {
            super(context);
            paint.setColor(Color.WHITE);
            paint.setStrokeWidth(3);
            vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
            vibrationDuration = PreferenceUtil.getVibrationDuration(context);
            direction[0] = BitmapFactory.decodeResource(getResources(), R.drawable.dynamic_dpad_right);
            btnW = direction[0].getWidth();
            btnH = direction[0].getHeight();
            direction[1] = BitmapFactory.decodeResource(getResources(), R.drawable.dynamic_dpad_right_up);
            direction[2] = BitmapFactory.decodeResource(getResources(), R.drawable.dynamic_dpad_up);
            direction[3] = BitmapFactory.decodeResource(getResources(), R.drawable.dynamic_dpad_left_up);
            direction[4] = BitmapFactory.decodeResource(getResources(), R.drawable.dynamic_dpad_left);
            direction[5] = BitmapFactory.decodeResource(getResources(), R.drawable.dynamic_dpad_left_down);
            direction[6] = BitmapFactory.decodeResource(getResources(), R.drawable.dynamic_dpad_down);
            direction[7] = BitmapFactory.decodeResource(getResources(), R.drawable.dynamic_dpad_right_down);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            if (dpadCenterX != -1 && currentX != -1) {
                int bitmapOffsetX = (int) (dpadCenterX - btnW / 2);
                int bitmapOffsetY = (int) (dpadCenterY - btnH / 2);
                if (directionIdx != -1) {
                    canvas.drawBitmap(direction[directionIdx], bitmapOffsetX, bitmapOffsetY, paint);
                }
            }
        }

        private void release() {
            emulator.setKeyPressed(port, rightMapped, false);
            emulator.setKeyPressed(port, leftMapped, false);
            emulator.setKeyPressed(port, upMapped, false);
            emulator.setKeyPressed(port, downMapped, false);
            dpadCenterX = -1;
            dpadCenterY = -1;
            currentX = -1;
            currentY = -1;
            activePointerId = -1;
            directionIdx = -1;
            invalidate();
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            int actionMasked = event.getActionMasked();

            if (actionMasked == MotionEvent.ACTION_MOVE) {
                optimizationCounter++;
                if (optimizationCounter < 5) {
                    return true;
                } else {
                    optimizationCounter = 0;
                }
            }

            if (actionMasked == MotionEvent.ACTION_DOWN || actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
                if (activePointerId == -1) {
                    activePointerId = event.getPointerId(event.getActionIndex());

                    if (!touchController.isPointerHandled(activePointerId)) {
                        int pointerIndex = event.findPointerIndex(activePointerId);
                        dpadCenterX = event.getX(pointerIndex);
                        dpadCenterY = event.getY(pointerIndex);

                        if (vibrationDuration > 0) {
                            vibrator.vibrate(vibrationDuration);
                        }
                        return true;

                    } else {
                        activePointerId = -1;
                    }
                }
            }

            if (actionMasked == MotionEvent.ACTION_MOVE) {
                if (dpadCenterX != -1) {
                    for (int i = 0; i < event.getPointerCount(); i++) {
                        if (event.getPointerId(i) == activePointerId) {
                            if (!touchController.isPointerHandled(activePointerId)) {
                                float x = event.getX(i);
                                float y = event.getY(i);
                                currentX = x;
                                currentY = y;
                                float dx = (x - dpadCenterX);
                                float dy = (dpadCenterY - y);
                                float tan = Math.abs(dy) / Math.abs(dx);
                                boolean left, right, up, down;
                                right = left = up = down = false;

                                if (dx > minDistX) {
                                    right = true;

                                } else if (dx < -minDistX) {
                                    left = true;
                                }

                                if (tan > TAN_DIAGONAL_MIN) {
                                    if (dy > minDistY) {
                                        up = true;

                                    } else if (dy < -minDistY) {
                                        down = true;
                                    }
                                }

                                if (tan > TAN_DIAGONAL_MAX) {
                                    right = false;
                                    left = false;
                                }

                                emulator.setKeyPressed(port, rightMapped, right);
                                emulator.setKeyPressed(port, leftMapped, left);
                                emulator.setKeyPressed(port, upMapped, up);
                                emulator.setKeyPressed(port, downMapped, down);

                                if (right) {
                                    if (down) {
                                        directionIdx = 7;

                                    } else if (up) {
                                        directionIdx = 1;

                                    } else {
                                        directionIdx = 0;
                                    }

                                } else if (left) {
                                    if (down) {
                                        directionIdx = 5;

                                    } else if (up) {
                                        directionIdx = 3;

                                    } else {
                                        directionIdx = 4;
                                    }

                                } else {
                                    if (up) {
                                        directionIdx = 2;

                                    } else if (down) {
                                        directionIdx = 6;
                                    }
                                }

                                invalidate();
                                return true;

                            } else {
                                release();
                            }
                        }
                    }
                }
            }

            switch (actionMasked) {
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                case MotionEvent.ACTION_POINTER_UP:
                    if (activePointerId != -1 && event.getPointerId(event.getActionIndex()) == activePointerId) {
                        release();
                    }
                    break;
            }
            return true;
        }
    }

}
