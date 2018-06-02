package nostalgia.framework.ui.multitouchbutton;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.util.TypedValue;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;

import nostalgia.framework.EmulatorController;
import nostalgia.framework.R;
import nostalgia.framework.base.EmulatorHolder;
import nostalgia.framework.base.ViewPort;
import nostalgia.framework.base.ViewUtils;
import nostalgia.framework.ui.preferences.PreferenceUtil;
import nostalgia.framework.utils.EmuUtils;
import nostalgia.framework.utils.NLog;

public class MultitouchLayer extends RelativeLayout implements OnTouchListener {

    private static final String TAG = "MultitouchLayer";
    private static final int EMPTY_COLOR = 0x00;
    private static final int BUTTON_MIN_SIZE_DP = 20;
    private static final int[] VIDEOMODE_COLORS = new int[]{0xffff8800, 0xff99cc00};
    private static final int MAX_POINTERS = 6;
    private static final int COUNT_SKIP_MOVE_EVENT = 3;
    Paint videoModeLabelPaint = new Paint();
    int lastW = 0, lastH = 0;
    LinearLayout touchLayer;
    Vibrator vibrator;
    Paint paint = new Paint();
    Paint bitmapRectPaint = new Paint();
    boolean firstRun = true;
    EditElement screenElement = null;
    EditElement menuElement = null;
    Paint editPaint = new Paint();
    Paint redPaint = new Paint();
    int selectIdx = -1;
    float selectW;
    float selectH;
    float startDragX = 0;
    float startDragY = 0;
    float startDragXoffset = 0;
    float startDragYoffset = 0;
    int startTouchX = 0;
    int startTouchY = 0;
    float startDistance = 0;
    RectF lastValidBB = new RectF();
    int lastTouchX = 0;
    int lastTouchY = 0;
    EDIT_MODE editMode = EDIT_MODE.NONE;
    int counter = 0;
    HashMap<String, RectF> viewPortsEnvelops = new HashMap<>();
    boolean isResizing = false;
    Timer timer = new Timer();
    int cacheRotation = -1;
    int cacheW = -1;
    int cacheH = -1;
    private ArrayList<View> btns = new ArrayList<>();
    private SparseIntArray pointerMap = new SparseIntArray();
    private int touchMapWidth;
    private int touchMapHeight;
    private SparseIntArray ridToIdxMap = new SparseIntArray();
    private Paint editElementPaint = new Paint();
    private Bitmap resizeIcon;
    private float buttonMinSizePx = 0;
    private ArrayList<EditElement> editElements = new ArrayList<>();
    private byte[][] maps;
    private Rect[] boundingBoxs;
    private Bitmap[] buttonsBitmaps = new Bitmap[0];
    private Bitmap[] pressedButtonsBitmaps = new Bitmap[0];
    private ArrayList<Integer> dpadRIDs = new ArrayList<>();
    private ArrayList<Integer> btnIdMap = new ArrayList<>();
    private int initCounter = 0;
    private int[] optimCounters = new int[MAX_POINTERS];
    private int vibrationDuration = 100;
    private Bitmap lastGameScreenshot;
    private String lastGfxProfileName = null;
    private boolean loadingSettings = true;
    private boolean staticDPADEnabled = true;
    private Paint pp;

    public MultitouchLayer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public MultitouchLayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public MultitouchLayer(Context context) {
        super(context);
        init(context);
    }

    static String getPrefName(int rot) {
        return "-mtl-".concat(Integer.toString(rot)).concat(".settings");
    }

    private void remapOldMTLprefToNew(SharedPreferences pref, Map<String, ?> prefMap) {
        HashMap<Integer, Integer> oldIdsToNewMap = null;
        Editor editor = pref.edit();
        HashSet<String> keysToRemove = new HashSet<>();
        boolean wrongFormat = false;

        for (Entry<String, ?> entry : prefMap.entrySet()) {
            String value = (String) entry.getValue();
            keysToRemove.add(entry.getKey());
            Integer oldBtnId = Integer.parseInt(entry.getKey());
            Integer newBtnId = oldIdsToNewMap.get(oldBtnId);
            int newKey = btnIdMap.indexOf(newBtnId);
            if (newBtnId == 0 || newKey == -1) {
                NLog.e(TAG, "oldBtnId:" + oldBtnId + " newBtnId:" + newBtnId + " newKey:" + newKey);
                wrongFormat = true;
            } else {
                editor.putString(newKey + "", value);
            }
        }

        if (wrongFormat) {
            editor.clear();
        } else {
            for (String key : keysToRemove) {
                editor.remove(key);
            }
        }
        editor.apply();
    }

    public void setOpacity(int alpha) {
        if (!isInEditMode()) {
            paint.setAlpha(alpha);
        }
    }

    private void init(Context context) {
        dpadRIDs.add(R.id.button_center);
        dpadRIDs.add(R.id.button_down);
        dpadRIDs.add(R.id.button_up);
        dpadRIDs.add(R.id.button_left);
        dpadRIDs.add(R.id.button_right);
        dpadRIDs.add(R.id.button_up_left);
        dpadRIDs.add(R.id.button_up_right);
        dpadRIDs.add(R.id.button_down_left);
        dpadRIDs.add(R.id.button_down_right);
        btnIdMap.add(R.id.button_a);
        btnIdMap.add(R.id.button_a_turbo);
        btnIdMap.add(R.id.button_b);
        btnIdMap.add(R.id.button_b_turbo);
        btnIdMap.add(R.id.button_ab);

        if (EmulatorHolder.getInfo().getKeyMapping().get(EmulatorController.KEY_SELECT) != -1) {
            btnIdMap.add(R.id.button_select);
        }

        btnIdMap.add(R.id.button_start);
        btnIdMap.add(R.id.button_menu);
        btnIdMap.add(R.id.button_down);
        btnIdMap.add(R.id.button_up);
        btnIdMap.add(R.id.button_left);
        btnIdMap.add(R.id.button_right);
        btnIdMap.add(R.id.button_up_left);
        btnIdMap.add(R.id.button_up_right);
        btnIdMap.add(R.id.button_down_left);
        btnIdMap.add(R.id.button_down_right);
        btnIdMap.add(R.id.button_center);
        btnIdMap.add(R.id.button_fast_forward);

        if (!isInEditMode()) {
            initScreenElement(false);
        }

        pp = new Paint();
        pp.setColor(0x5500ff00);

        setBackgroundColor(0x01000000);
        paint.setFilterBitmap(true);
        editElementPaint.setColor(getContext().getResources().getColor(R.color.main_color));
        editElementPaint.setStyle(Style.STROKE);
        DashPathEffect dashPathEffect = new DashPathEffect(new float[]{1, 4}, 0);
        editElementPaint.setPathEffect(dashPathEffect);
        bitmapRectPaint.setStyle(Style.STROKE);
        bitmapRectPaint.setColor(editElementPaint.getColor());
        resizeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.resize_icon);
        buttonMinSizePx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, BUTTON_MIN_SIZE_DP,
                getResources().getDisplayMetrics());
        if (!isInEditMode()) {
            ViewTreeObserver vto = getViewTreeObserver();
            touchLayer = new LinearLayout(getContext());
            vto.addOnGlobalLayoutListener(() -> {
                int w = getMeasuredWidth();
                int h = getMeasuredHeight();
                if (w != lastW || h != lastH) {
                    lastW = w;
                    lastH = h;
                    initMultiTouchMap();
                }
            });
            vibrator = (Vibrator) getContext().getSystemService(Context.VIBRATOR_SERVICE);
        }

        float videoModeLabelSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                14, getResources().getDisplayMetrics());
        videoModeLabelPaint.setTextSize(videoModeLabelSize);
        videoModeLabelPaint.setStyle(Style.STROKE);
    }

    private void initMultiTouchMap() {
        initCounter++;
        for (int i = 0; i < 100; i++)
            pointerMap.put(i, EMPTY_COLOR);
        ridToIdxMap.clear();
        NLog.d(TAG, " create touch map width " + getMeasuredWidth() + " height:" + getMeasuredHeight());
        touchMapWidth = getMeasuredWidth();
        touchMapHeight = getMeasuredHeight();
        Rect r = new Rect();

        if (btns.size() == 0) {
            getAllImageButtons(this, btns);
        }

        int btnsCount = btns.size();
        NLog.i(TAG, " found " + btnsCount + " multitouch btns");
        maps = new byte[btnsCount][];

        if (buttonsBitmaps != null) {
            for (Bitmap bitmap : buttonsBitmaps) {
                if (bitmap != null) {
                    bitmap.recycle();
                }
            }
        }

        if (buttonsBitmaps != null) {
            for (Bitmap bitmap : buttonsBitmaps) {
                if (bitmap != null) {
                    bitmap.recycle();
                }
            }
        }

        boundingBoxs = new Rect[btnsCount];
        buttonsBitmaps = new Bitmap[btnsCount];
        pressedButtonsBitmaps = new Bitmap[btnsCount];
        int idx = 0;

        for (View btn : btns) {
            ridToIdxMap.append(btn.getId(), idx);

            if (btn.getVisibility() != View.GONE) {
                btn.getLocalVisibleRect(r);
                int btnW = btn.getMeasuredWidth();
                int btnH = btn.getMeasuredHeight();
                int btnX = getRelativeLeft(btn, this);
                int btnY = getRelativeTop(btn, this);
                boundingBoxs[idx] = new Rect(btnX, btnY, btnX + btnW, btnY + btnH);
                r.offsetTo(btnX, btnY);

                if (btnW > 0 && btnH > 0) {
                    Bitmap buttonBitmap = Bitmap.createBitmap(btnW, btnH, Bitmap.Config.ARGB_8888);

                    if (buttonBitmap.isRecycled()) {
                        NLog.wtf(TAG, "co se to kurva deje");
                        throw new RuntimeException("netusim");
                    }

                    Canvas buttonCanvas = new Canvas(buttonBitmap);
                    btn.draw(buttonCanvas);

                    if (!(btn instanceof MultitouchTwoButtonArea)) {
                        Bitmap pressedButtonBitmap = Bitmap.createBitmap(btnW, btnH, Bitmap.Config.ARGB_8888);
                        Canvas pressedButtonCanvas = new Canvas(pressedButtonBitmap);
                        btn.setPressed(true);
                        btn.draw(pressedButtonCanvas);
                        btn.setPressed(false);
                        pressedButtonsBitmaps[idx] = pressedButtonBitmap;
                        buttonsBitmaps[idx] = buttonBitmap;

                    } else {
                        buttonsBitmaps[idx] = buttonBitmap;
                        pressedButtonsBitmaps[idx] = null;
                    }

                } else {
                    buttonsBitmaps[idx] = null;
                    pressedButtonsBitmaps[idx] = null;
                    postDelayed(this::initMultiTouchMap, 1000);
                }
            }
            idx++;
        }

        if (touchLayer.getParent() != null) {
            ViewGroup parent = (ViewGroup) touchLayer.getParent();
            parent.removeView(touchLayer);
        }

        touchLayer.setOnTouchListener(this);
        removeAllViews();
        addView(touchLayer, LinearLayout.LayoutParams.MATCH_PARENT, getMeasuredHeight());
        boolean hasSelect = EmulatorHolder.getInfo().getKeyMapping().get(EmulatorController.KEY_SELECT) != -1;
        if (hasSelect) {
            editElements.add(new EditElement(R.id.button_select, true, buttonMinSizePx).saveHistory());
        }

        editElements.add(new EditElement(R.id.button_start, true, buttonMinSizePx).saveHistory());
        EditElement dpad = new EditElement(R.id.button_center, true, buttonMinSizePx * 5);
        dpad.add(R.id.button_down);
        dpad.add(R.id.button_up);
        dpad.add(R.id.button_left);
        dpad.add(R.id.button_right);
        dpad.add(R.id.button_up_left);
        dpad.add(R.id.button_up_right);
        dpad.add(R.id.button_down_left);
        dpad.add(R.id.button_down_right);
        dpad.saveHistory();
        editElements.add(dpad);
        editElements.add(new EditElement(R.id.button_a, true, buttonMinSizePx).saveHistory());
        editElements.add(new EditElement(R.id.button_b, true, buttonMinSizePx).saveHistory());
        editElements.add(new EditElement(R.id.button_a_turbo, true, buttonMinSizePx).saveHistory());
        editElements.add(new EditElement(R.id.button_b_turbo, true, buttonMinSizePx).saveHistory());
        editElements.add(new EditElement(R.id.button_ab, true, buttonMinSizePx).saveHistory());
        editElements.add(new EditElement(R.id.button_fast_forward, true, buttonMinSizePx).saveHistory());
        EditElement menu = new EditElement(R.id.button_menu, false, buttonMinSizePx).saveHistory();
        menu.setOnClickListener(() -> {
            if (editMode != EDIT_MODE.NONE) {
                ((Activity) getContext()).openOptionsMenu();
            }
        });
        editElements.add(menu);
        menuElement = menu;
        reloadTouchProfile();
        setEnableStaticDPAD(staticDPADEnabled);
    }

    public void reloadTouchProfile() {
        if (loadEditElements("") || firstRun || (!isTouchMapsValid())) {
            firstRun = btns.size() == 0;
            int idx = 0;

            for (View btn : btns) {
                if (btn.getVisibility() != View.GONE) {
                    Rect bb = boundingBoxs[idx];

                    if (btn.getId() == R.id.button_fast_forward) {
                        NLog.i(TAG, "fast f btn " + idx + " bb " + bb);
                    }

                    int btnW = bb.width();
                    int btnH = bb.height();
                    Bitmap origButtonBitmap = buttonsBitmaps[idx];
                    Bitmap origPressedButtonBitmap = pressedButtonsBitmaps[idx];

                    if (origPressedButtonBitmap != null) {
                        Bitmap pressedBitmap = Bitmap.createScaledBitmap(
                                origPressedButtonBitmap, btnW, btnH, true);
                        origPressedButtonBitmap.recycle();
                        pressedButtonsBitmaps[idx] = pressedBitmap;
                    }

                    if (origButtonBitmap != null) {
                        Bitmap buttonBitmap = Bitmap.createScaledBitmap(origButtonBitmap, btnW, btnH, true);
                        origButtonBitmap.recycle();
                        buttonsBitmaps[idx] = buttonBitmap;
                        int[] buttonPixels = new int[btnW * btnH];
                        buttonBitmap.getPixels(buttonPixels, 0, btnW, 0, 0, btnW, btnH);
                        byte[] map = new byte[buttonPixels.length];

                        for (int i = 0; i < buttonPixels.length; i++) {
                            int pixel = buttonPixels[i];
                            map[i] = pixel == 0 ? 0 : (byte) (idx + 1);
                        }

                        maps[idx] = map;

                        if (btn instanceof MultitouchTwoButtonArea) {
                            buttonBitmap.recycle();
                            buttonsBitmaps[idx] = null;
                        }
                    }
                }
                idx++;
            }
        } else {
            NLog.i(TAG, hashCode() + " nic se nezmenilo");
        }
    }

    private boolean isTouchMapsValid() {
        int idx = 0;

        for (View btn : btns) {
            if (btn.getVisibility() != View.GONE) {
                Rect bb = boundingBoxs[idx];
                int len = bb.width() * bb.height();
                byte[] map = maps[idx];
                if (map == null || (map.length != len)) {
                    return false;
                }
            }
            idx++;
        }
        return true;
    }

    private void getAllImageButtons(ViewGroup root, ArrayList<View> allButtons) {
        for (int i = 0; i < root.getChildCount(); i++) {
            View v = root.getChildAt(i);

            if (v instanceof ViewGroup) {
                getAllImageButtons((ViewGroup) v, allButtons);
            } else if (v instanceof MultitouchBtnInterface) {
                allButtons.add(v);
            }
        }
    }

    private int getRelativeLeft(View myView, View rootView) {
        ViewParent parent = myView.getParent();
        if (parent == null || parent == rootView)
            return myView.getLeft();
        else
            return myView.getLeft() + getRelativeLeft((View) parent, rootView);
    }

    private int getRelativeTop(View myView, View rootView) {
        ViewParent parent = myView.getParent();

        if (parent == null || parent == rootView)
            return myView.getTop();
        else
            return myView.getTop() + getRelativeTop((View) parent, rootView);
    }

    private void handleTouchEvent(int x, int y, int pointerId, MotionEvent event) {
        if (pointerId < MAX_POINTERS && event.getActionMasked() == MotionEvent.ACTION_MOVE) {
            if (optimCounters[pointerId] < COUNT_SKIP_MOVE_EVENT) {
                optimCounters[pointerId]++;
                return;
            }
            optimCounters[pointerId] = 0;
        }
        if (x < 0 || y < 0 || x >= touchMapWidth || y >= touchMapHeight) {
            return;
        }

        int newBtnIdx = EMPTY_COLOR;
        for (int i = maps.length - 1; i >= 0; i--) {
            Rect boundingBox = boundingBoxs[i];

            if (boundingBox != null && boundingBox.contains(x, y)
                    && btns.get(i).isEnabled()) {
                byte[] map = maps[i];
                int newx = x - boundingBox.left;
                int newy = y - boundingBox.top;

                if (map == null) {
                    boolean debug = EmuUtils.isDebuggable(getContext());

                    if (!debug) {
                        IllegalStateException e =
                                new IllegalStateException("button touch map neni nainicializovany");
                        NLog.e(TAG, e.toString());
                    }
                    newBtnIdx = i;
                    break;
                } else {
                    int idx = newx + newy * boundingBox.width();
                    if (idx < map.length) {
                        int btnIdx = map[idx];
                        if (btnIdx != 0) {
                            newBtnIdx = btnIdx;
                            break;
                        }
                    }
                }
            }
        }

        int oldBtnIdx = pointerMap.get(pointerId);
        if (newBtnIdx != 0) {
            if (oldBtnIdx != newBtnIdx) {
                if (oldBtnIdx != EMPTY_COLOR) {
                    onTouchExit(oldBtnIdx - 1, event);
                }
                onTouchEnter(newBtnIdx - 1, event);
                if (vibrationDuration > 0) {
                    vibrator.vibrate(vibrationDuration);
                }
            }
        } else if (oldBtnIdx != EMPTY_COLOR) {
            onTouchExit(oldBtnIdx - 1, event);
        }
        pointerMap.put(pointerId, newBtnIdx);
    }

    public void setVibrationDuration(int duration) {
        this.vibrationDuration = duration;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (editMode == EDIT_MODE.NONE) {
            if ((event.getActionMasked() == MotionEvent.ACTION_MOVE)) {
                int pointerCount = event.getPointerCount();
                for (int pointerIdx = 0; pointerIdx < pointerCount; pointerIdx++) {
                    int id = event.getPointerId(pointerIdx);
                    int x = (int) event.getX(pointerIdx);
                    int y = (int) event.getY(pointerIdx);
                    handleTouchEvent(x, y, id, event);
                }
            } else if ((event.getActionMasked() == MotionEvent.ACTION_UP)
                    || (event.getActionMasked() == MotionEvent.ACTION_POINTER_UP)
                    || (event.getActionMasked() == MotionEvent.ACTION_CANCEL)) {
                int id = event.getPointerId(event.getActionIndex());
                int oldBtnIdx = pointerMap.get(id);
                if (oldBtnIdx != EMPTY_COLOR) {
                    onTouchExit(oldBtnIdx - 1, event);
                }
                pointerMap.put(id, EMPTY_COLOR);
            } else if ((event.getActionMasked() == MotionEvent.ACTION_DOWN)
                    || (event.getActionMasked() == MotionEvent.ACTION_POINTER_DOWN)) {
                int pointerId = event.getPointerId(event.getActionIndex());
                int pointerIdx = event.findPointerIndex(pointerId);
                if (pointerIdx != -1) {
                    int x = (int) event.getX(pointerIdx);
                    int y = (int) event.getY(pointerIdx);
                    handleTouchEvent(x, y, pointerId, event);
                }
            }
        } else {
            onTouchInEditMode(event);
        }
        return true;
    }

    public boolean isPointerHandled(int pointerId) {
        return pointerMap.get(pointerId) != EMPTY_COLOR;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (!isInEditMode() && editMode == EDIT_MODE.NONE) {
            for (int idx = 0; idx < boundingBoxs.length; idx++) {
                MultitouchBtnInterface btn = (MultitouchBtnInterface) btns.get(idx);
                btn.removeRequestRepaint();
                if (btn.getVisibility() == View.VISIBLE) {
                    Bitmap b = btn.isPressed() ? pressedButtonsBitmaps[idx] : buttonsBitmaps[idx];
                    if (b != null) {
                        Rect bb = boundingBoxs[idx];
                        canvas.drawBitmap(b, bb.left, bb.top, paint);
                        if (editMode != EDIT_MODE.NONE) {
                            canvas.drawRect(bb, pp);
                        }
                    }
                }
            }
        }
        if (editMode != EDIT_MODE.NONE) {
            onDrawInEditMode(canvas);
        }
    }

    private void onDrawInEditMode(Canvas canvas) {
        Paint p = new Paint();
        p.setColor(0xff888888);
        p.setAlpha(255);
        RectF dstScreenShotRect = new RectF();
        if (viewPortsEnvelops.size() > 1 && lastGfxProfileName != null) {
            RectF bb = screenElement.boundingbox;
            RectF env = null;
            int counter = 0;

            for (Entry<String, RectF> entry : viewPortsEnvelops.entrySet()) {
                if (entry.getKey().equals(lastGfxProfileName)) {
                    env = entry.getValue();
                    break;
                }
                counter++;
            }
            dstScreenShotRect.left = bb.left + env.left * bb.width() + (counter * 2) + 2;
            dstScreenShotRect.top = bb.top + env.top * bb.height() + (counter * 2) + 2;
            dstScreenShotRect.right = bb.right - env.right * bb.width() - ((counter * 2) + 1);
            dstScreenShotRect.bottom = bb.bottom - env.bottom * bb.height() - ((counter * 2) + 1);
        } else {
            dstScreenShotRect.set(screenElement.boundingbox);
        }
        if (lastGameScreenshot != null && !lastGameScreenshot.isRecycled()) {
            Rect src = new Rect(0, 0, lastGameScreenshot.getWidth(), lastGameScreenshot.getHeight());
            canvas.drawBitmap(lastGameScreenshot, src, dstScreenShotRect, p);
        } else {
            canvas.drawRect(dstScreenShotRect, p);
        }
        if (editMode == EDIT_MODE.TOUCH) {
            canvas.drawRect(screenElement.boundingbox, bitmapRectPaint);
        } else if (editMode == EDIT_MODE.SCREEN && viewPortsEnvelops.size() > 1) {
            RectF rect = new RectF();
            RectF bb = screenElement.boundingbox;
            int counter = 0;
            for (Entry<String, RectF> entry : viewPortsEnvelops.entrySet()) {
                RectF env = entry.getValue();
                rect.left = bb.left + env.left * bb.width() + (counter * 2) + 2;
                rect.top = bb.top + env.top * bb.height() + (counter * 2) + 2;
                rect.right = bb.right - env.right * bb.width() - ((counter * 2) + 1);
                rect.bottom = bb.bottom - env.bottom * bb.height() - ((counter * 2) + 1);
                videoModeLabelPaint.setColor(VIDEOMODE_COLORS[counter % VIDEOMODE_COLORS.length]);
                canvas.drawRect(rect, videoModeLabelPaint);
                videoModeLabelPaint.setTextAlign(counter % 2 == 0 ? Align.LEFT : Align.RIGHT);
                canvas.drawText(entry.getKey(), counter % 2 == 0 ?
                                (rect.left + videoModeLabelPaint.getTextSize() / 4) :
                                rect.right - videoModeLabelPaint.getTextSize() / 4,
                        rect.bottom - videoModeLabelPaint.getTextSize() / 4, videoModeLabelPaint);
                counter++;
            }
        }

        for (int idx = 0; idx < boundingBoxs.length; idx++) {
            MultitouchBtnInterface btn = (MultitouchBtnInterface) btns.get(idx);
            if (btn.getId() == R.id.button_menu) {
                paint.setAlpha(255);
            } else {
                paint.setAlpha(editMode == EDIT_MODE.SCREEN ? 64 : 255);
            }
            btn.removeRequestRepaint();
            Bitmap b = btn.isPressed() ? pressedButtonsBitmaps[idx] : buttonsBitmaps[idx];
            if (b != null) {
                Rect bb = boundingBoxs[idx];
                Rect bRect = new Rect(0, 0, b.getWidth(), b.getHeight());
                canvas.drawBitmap(b, bRect, bb, paint);
            }
        }
        editPaint.setColor(0x55ff0000);
        if (editMode == EDIT_MODE.TOUCH) {
            for (EditElement e : editElements) {
                if (e.movable) {
                    if (!e.validPosition) {
                        canvas.drawRect(e.boundingbox, editPaint);
                    }
                    canvas.drawRect(e.boundingbox, editElementPaint);
                    RectF r = e.getResizingBox();
                    canvas.drawBitmap(resizeIcon, r.left, r.top, editElementPaint);
                }
            }
        } else {
            EditElement e = screenElement;
            if (e.movable) {
                if (!e.validPosition) {
                    canvas.drawRect(e.boundingbox, editPaint);
                }
                canvas.drawRect(e.boundingbox, editElementPaint);
                RectF r = e.getResizingBox();
                canvas.drawBitmap(resizeIcon, r.left, r.top, editElementPaint);
            }
        }
    }

    private void onTouchInEditMode(MotionEvent event) {
        if (!isResizing) {
            onTouchInEditModeMove(event);
        } else {
            onTouchInEditModeResize(event);
        }
    }

    private boolean onTouchCheck(EditElement e, int idx, int x, int y) {
        RectF boundingBox = e.boundingbox;
        RectF resizingAnchor = e.getResizingBox();
        if (e.listener != null && boundingBox.contains(x, y)) {
            e.listener.onClick();
        } else if ((resizingAnchor.contains(x, y) || boundingBox.contains(x, y)) && e.movable) {
            lastValidBB.set(e.boundingbox);
            isResizing = resizingAnchor.contains(x, y);
            selectIdx = idx;
            selectW = boundingBox.width();
            selectH = boundingBox.height();
            startDragX = boundingBox.left;
            startDragY = boundingBox.top;
            startTouchX = x;
            startTouchY = y;
            startDragXoffset = boundingBox.right - x;
            startDragYoffset = boundingBox.bottom - y;
            if (isResizing) {
                e.resizeRects.clear();
                for (int i = 0; i < e.ids.size(); i++) {
                    int id = e.ids.get(i);
                    e.resizeRects.add(new RectF(boundingBoxs[id]));
                }
            }
            Rect invalR = new Rect();
            boundingBox.round(invalR);
            invalidate(invalR);
            return true;
        }
        return false;
    }

    private void onTouchInEditModeMove(MotionEvent event) {
        int action = event.getAction();
        int x = (int) (event.getX() + 0.5f);
        int y = (int) (event.getY() + 0.5f);
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                int idx = 0;
                if (editMode == EDIT_MODE.TOUCH) {
                    for (EditElement e : editElements) {
                        if (onTouchCheck(e, idx, x, y)) {
                            break;
                        }
                        idx++;
                    }
                } else {
                    onTouchCheck(screenElement, 0, x, y);
                    onTouchCheck(menuElement, 0, x, y);
                }
                break;
            }
            case MotionEvent.ACTION_MOVE:
                if (selectIdx != -1) {
                    EditElement element;
                    if (editMode == EDIT_MODE.TOUCH) {
                        element = editElements.get(selectIdx);
                    } else {
                        element = screenElement;
                    }
                    RectF elementBb = element.boundingbox;
                    int vx = x - startTouchX;
                    int vy = y - startTouchY;
                    RectF r = new RectF(elementBb);
                    float left = startDragX + vx;
                    float top = startDragY + vy;
                    r.set(left - 2, top - 2, left + selectW + 2, top + selectH + 2);
                    element.validPosition = isRectValid(r, element);
                    if (element.validPosition) {
                        lastValidBB.set(left, top, left + selectW, top + selectH);
                    }
                    r.set(left - 10, top - 10, left + selectW + 10, top + selectH + 10);
                    Rect tempRect = new Rect();
                    r.round(tempRect);
                    invalidate(tempRect);
                    element.boundingbox.set(r.left + 10, r.top + 10, r.right - 10, r.bottom - 10);
                    if (editMode == EDIT_MODE.TOUCH)
                        recomputeBtn(element);
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_OUTSIDE: {
                endMovementCheck();
            }
            break;
        }
    }

    private void onTouchInEditModeResize(MotionEvent event) {
        int action = event.getAction();
        int x = (int) (event.getX() + 0.5f);
        lastTouchX = x;
        lastTouchY = Math.round(event.getY());

        switch (action) {
            case MotionEvent.ACTION_MOVE:
                if (selectIdx != -1) {
                    EditElement element = editMode == EDIT_MODE.TOUCH ?
                            editElements.get(selectIdx) : screenElement;
                    RectF elementBb = element.boundingbox;
                    float newW = x - startDragX + startDragXoffset;
                    float scaleFactorW = (newW / selectW);
                    elementBb.set(startDragX, startDragY, x + startDragXoffset,
                            startDragY + selectH * scaleFactorW);
                    if (editMode == EDIT_MODE.TOUCH)
                        recomputeBtn(element);
                    element.validPosition = isRectValid(elementBb, element);
                    if (element.validPosition) {
                        lastValidBB.set(element.boundingbox);
                    }
                    invalidate();
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_OUTSIDE: {
                isResizing = false;
                endMovementCheck();
            }
            break;
        }
    }

    private void recomputeBtn(EditElement element) {
        float scaleFactor = element.boundingbox.width() / element.boundingboxHistory.width();
        for (int i = 0; i < element.ids.size(); i++) {
            int id = element.ids.get(i);
            RectF offset = new RectF(element.offsetshistory.get(i));
            RectF bb = new RectF(element.boundingboxsHistory.get(i));
            RectF elemBB = element.boundingboxHistory;
            bb.offset(-elemBB.left, -elemBB.top);
            bb.left *= scaleFactor;
            bb.top *= scaleFactor;
            bb.right *= scaleFactor;
            bb.bottom *= scaleFactor;
            offset.left *= scaleFactor;
            offset.top *= scaleFactor;
            element.offsets.get(i).set(offset);
            bb.offset(element.boundingbox.left, element.boundingbox.top);
            bb.round(boundingBoxs[id]);
        }
    }

    private void endMovementCheck() {
        if (selectIdx != -1) {
            EditElement element = editMode == EDIT_MODE.TOUCH ? editElements.get(selectIdx) : screenElement;
            if (!element.validPosition) {
                element.boundingbox.set(lastValidBB);
            }
            if (editMode == EDIT_MODE.TOUCH)
                recomputeBtn(element);
            element.validPosition = true;
            selectIdx = -1;
        }
        invalidate();
    }

    private boolean isRectValid(RectF r, EditElement element) {
        boolean isvalid = true;
        RectF globalBox = new RectF(0, 0, touchMapWidth, touchMapHeight);
        if (globalBox.contains(r)) {
            if (editMode == EDIT_MODE.TOUCH) {
                for (EditElement el : editElements) {
                    if (el != element && RectF.intersects(r, el.boundingbox)) {
                        isvalid = false;
                        break;
                    }
                }
            }
        } else {
            isvalid = false;
        }
        if (element.boundingbox.width() < element.minimalSize ||
                element.boundingbox.height() < element.minimalSize) {
            isvalid = false;
        }
        return isvalid;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        for (Bitmap b : buttonsBitmaps) {
            if (b != null) {
                b.recycle();
            }
        }
        for (Bitmap b : pressedButtonsBitmaps) {
            if (b != null) {
                b.recycle();
            }
        }
        buttonsBitmaps = null;
        pressedButtonsBitmaps = null;
        NLog.i(TAG, "on detach");
    }

    private void onTouchEnter(int btnIdx, MotionEvent event) {
        MultitouchBtnInterface btn = (MultitouchBtnInterface) btns.get(btnIdx);
        btn.onTouchEnter(event);
        btn.requestRepaint();
        invalidate(boundingBoxs[btnIdx]);

        if (btn instanceof MultitouchTwoButtonArea) {
            MultitouchTwoButtonArea mtba = (MultitouchTwoButtonArea) btn;
            int idx1 = ridToIdxMap.get(mtba.getFirstBtnRID());
            int idx2 = ridToIdxMap.get(mtba.getSecondBtnRID());
            invalidate(boundingBoxs[idx1]);
            invalidate(boundingBoxs[idx2]);

        } else if (btn instanceof MultitouchTwoButton) {
            MultitouchTwoButton mtba = (MultitouchTwoButton) btn;
            int idx1 = ridToIdxMap.get(mtba.getFirstBtnRID());
            int idx2 = ridToIdxMap.get(mtba.getSecondBtnRID());
            invalidate(boundingBoxs[idx1]);
            invalidate(boundingBoxs[idx2]);
        }
    }

    private void onTouchExit(int btnIdx, MotionEvent event) {
        MultitouchBtnInterface btn = (MultitouchBtnInterface) btns.get(btnIdx);
        btn.onTouchExit(event);
        invalidate(boundingBoxs[btnIdx]);
        btn.requestRepaint();

        if (btn instanceof MultitouchTwoButtonArea) {
            MultitouchTwoButtonArea mtba = (MultitouchTwoButtonArea) btn;
            int idx1 = ridToIdxMap.get(mtba.getFirstBtnRID());
            int idx2 = ridToIdxMap.get(mtba.getSecondBtnRID());
            invalidate(boundingBoxs[idx1]);
            invalidate(boundingBoxs[idx2]);

        } else if (btn instanceof MultitouchTwoButton) {
            MultitouchTwoButton mtba = (MultitouchTwoButton) btn;
            int idx1 = ridToIdxMap.get(mtba.getFirstBtnRID());
            int idx2 = ridToIdxMap.get(mtba.getSecondBtnRID());
            invalidate(boundingBoxs[idx1]);
            invalidate(boundingBoxs[idx2]);
        }
    }

    public void setLastgameScreenshot(Bitmap bitmap, String gfxProfileName) {
        NLog.i(TAG, "set last profile:" + gfxProfileName);
        lastGameScreenshot = bitmap;
        lastGfxProfileName = gfxProfileName;
        initScreenElement(false);
        invalidate();
    }

    public void setEditMode(EDIT_MODE mode) {
        this.editMode = mode;
        invalidate();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                counter++;
                editElementPaint.setPathEffect(new DashPathEffect(new float[]{4, 4}, counter % 8));
                postInvalidate();
            }
        }, 0, 50);

        if (editMode == EDIT_MODE.SCREEN) {
            resizeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.resize_icon_2);
        }
    }

    private void initScreenElement(boolean reset) {
        getPref();
        int topPadding = getResources().getDimensionPixelSize(R.dimen.top_panel_touch_controller_height);
        ViewPort vport = null;
        HashMap<String, ViewPort> viewPorts = null;
        if (reset) {
            vport = ViewUtils.computeInitViewPort(getContext(), cacheW, cacheH, 0, cacheRotation == 0 ? topPadding : 0);
            viewPorts = ViewUtils.computeAllInitViewPorts(getContext(), cacheW, cacheH, 0, cacheRotation == 0 ? topPadding : 0);
        } else {
            vport = ViewUtils.loadOrComputeViewPort(getContext(), null, cacheW, cacheH, 0, cacheRotation == 0 ? topPadding : 0, true);
            viewPorts = ViewUtils.loadOrComputeAllViewPorts(getContext(), cacheW, cacheH, 0, cacheRotation == 0 ? topPadding : 0);
        }
        Rect viewPort = new Rect(vport.x + 1, vport.y, vport.x + vport.width - 1, vport.y + vport.height - 1);

        if (editMode != EDIT_MODE.NONE) {
            if (editMode == EDIT_MODE.SCREEN) {
                for (ViewPort port : viewPorts.values()) {
                    viewPort.left = port.x < viewPort.left ? port.x : viewPort.left;
                    viewPort.top = port.y < viewPort.top ? port.y : viewPort.top;
                    int right = port.x + port.width;
                    viewPort.right = right > viewPort.right ? right : viewPort.right;
                    int bottom = port.y + port.height;
                    viewPort.bottom = bottom > viewPort.bottom ? bottom : viewPort.bottom;
                }

            } else if (lastGfxProfileName != null) {
                ViewPort port = viewPorts.get(lastGfxProfileName);
                if (port != null) {
                    viewPort.left = port.x;
                    viewPort.top = port.y;
                    viewPort.right = port.x + port.width;
                    viewPort.bottom = port.y + port.height;
                }
            }
            viewPortsEnvelops = new HashMap<>(viewPorts.size());

            for (Entry<String, ViewPort> entry : viewPorts.entrySet()) {
                ViewPort port = entry.getValue();
                float w = viewPort.width();
                float h = viewPort.height();
                float relativeLeft = (-viewPort.left + port.x) / w;
                float relativeTop = (-viewPort.top + port.y) / h;
                float relativeRight = (viewPort.right - (port.x + port.width)) / w;
                float relativeBottom = (viewPort.bottom - (port.y + port.height)) / h;
                RectF envelop = new RectF(relativeLeft, relativeTop, relativeRight, relativeBottom);
                viewPortsEnvelops.put(entry.getKey(), envelop);
            }
        }

        int topOffset = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                20, getResources().getDisplayMetrics());
        viewPort.top -= cacheRotation == 0 ? topOffset : 0;
        viewPort.bottom -= cacheRotation == 0 ? topOffset : 0;
        NLog.i(TAG, "init screenlayout " + EmulatorHolder.getInfo().getDefaultGfxProfile().name
                + " vp:" + viewPort.left + "," + viewPort.top + "," + viewPort.width()
                + "," + viewPort.height());
        screenElement = new EditElement(viewPort);
    }

    public void stopEditMode() {
        timer.cancel();
    }

    public void resetEditElement(String gameHash) {
        for (EditElement element : editElements) {
            element.boundingbox.set(element.boundingboxHistory);
            for (int i = 0; i < element.ids.size(); i++) {
                Rect bb = boundingBoxs[element.ids.get(i)];
                bb.set(element.boundingboxsHistory.get(i));
                element.offsets.get(i).set(element.offsetshistory.get(i));
            }
        }
        invalidate();
        Editor edit = getPref().edit();
        edit.clear();
        edit.apply();
    }

    public void resetScreenElement() {
        initScreenElement(true);
        PreferenceUtil.removeViewPortSave(getContext());
    }

    public void disableLoadSettings() {
        loadingSettings = false;

        for (EditElement element : editElements) {
            element.boundingbox.set(element.boundingboxHistory);
            for (int i = 0; i < element.ids.size(); i++) {
                Rect bb = boundingBoxs[element.ids.get(i)];
                bb.set(element.boundingboxsHistory.get(i));
                element.offsets.get(i).set(element.offsetshistory.get(i));
            }
        }
        invalidate();
    }

    private SharedPreferences getPref() {
        if (cacheRotation == -1) {
            WindowManager mWindowManager = (WindowManager) getContext()
                    .getSystemService(Context.WINDOW_SERVICE);
            Display mDisplay = mWindowManager.getDefaultDisplay();
            cacheRotation = mDisplay.getRotation() % 2;
            cacheW = EmuUtils.getDisplayWidth(mDisplay);
            cacheH = EmuUtils.getDisplayHeight(mDisplay);
        }
        return getContext().getSharedPreferences(getPrefName(cacheRotation), Context.MODE_PRIVATE);
    }

    public void saveScreenElement() {
        endMovementCheck();
        RectF bb = screenElement.boundingbox;
        RectF env = viewPortsEnvelops.get(EmulatorHolder.getInfo().getDefaultGfxProfile().name);
        Rect rect = new Rect();
        rect.left = Math.round(bb.left + env.left * bb.width());
        rect.top = Math.round(bb.top + env.top * bb.height());
        rect.right = Math.round(bb.right - env.right * bb.width());
        rect.bottom = Math.round(bb.bottom - env.bottom * bb.height());
        int topOffset = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20,
                getResources().getDisplayMetrics());
        ViewPort vp = new ViewPort();
        vp.x = rect.left;
        vp.y = rect.top + (cacheRotation == 0 ? topOffset : 0);
        vp.width = rect.width();
        vp.height = rect.height();
        NLog.i(TAG, "save screenlayout " + EmulatorHolder.getInfo().getDefaultGfxProfile().name
                + " vp:" + vp.x + "," + vp.y + "," + vp.width + "," + vp.height);
        PreferenceUtil.setViewPort(getContext(), vp, cacheW, cacheH);
    }

    public void saveEditElements() {
        endMovementCheck();
        SharedPreferences pref = getPref();
        Editor editor = pref.edit();

        for (int i = 0; i < btns.size(); i++) {
            View btn = btns.get(i);
            Rect offset = boundingBoxs[i];
            String s = offset.left + "-" + offset.top + "-" + offset.right + "-" + offset.bottom;
            int id = btnIdMap.indexOf(btn.getId());
            editor.putString(id + "", s);
        }
        editor.apply();
    }

    private boolean loadEditElements(String unused) {
        if (!loadingSettings) {
            return false;
        } else {
            SharedPreferences pref = getPref();
            Map<String, ?> prefMap = pref.getAll();
            for (String key : prefMap.keySet()) {
                try {
                    Integer id = Integer.parseInt(key);
                    if (id > 100) {
                        NLog.i(TAG, "Detect old MTL format(" + id + ")!\nTrying repaire it");
                        remapOldMTLprefToNew(pref, prefMap);
                        break;
                    }
                } catch (NumberFormatException e) {
                    Editor editor = pref.edit();
                    editor.clear();
                    editor.apply();
                    break;
                }
            }
            pref = getPref();
            if (pref.getAll().isEmpty()) {
                NLog.i(TAG, "neni ulozene nastaveni");
                for (EditElement elem : editElements) {
                    elem.computeBoundingBox();
                    elem.computeOffsets();
                }
                return false;
            } else {
                boolean isNew = false;
                for (int i = 0; i < btns.size(); i++) {
                    View btn = btns.get(i);
                    int id = btnIdMap.indexOf(btn.getId());
                    String s = pref.getString("" + id, "");
                    if (!s.equals("")) {
                        String[] sa = s.split("-");
                        Rect bb = boundingBoxs[ridToIdxMap.get(btn.getId())];
                        int left = Integer.parseInt(sa[0]);
                        int top = Integer.parseInt(sa[1]);
                        int right = Integer.parseInt(sa[2]);
                        int bottom = Integer.parseInt(sa[3]);
                        if (bb.left != left || bb.top != top || bb.right != right || bb.bottom != bottom) {
                            bb.set(left, top, right, bottom);
                            NLog.i(TAG, hashCode() + " detect change layout");
                            isNew = true;
                        }
                    }
                }
                for (EditElement elem : editElements) {
                    elem.computeBoundingBox();
                    elem.computeOffsets();
                }
                //NLog.i(TAG, hashCode() + " isNew:" + isNew + " " + btns.size() + " " + Arrays.toString(boundingBoxs));
                checkFastForwardButton();
                return isNew;
            }
        }
    }

    private void checkFastForwardButton() {
        if (boundingBoxs != null) {
            int idx = ridToIdxMap.get(R.id.button_fast_forward);
            Rect ff_bb = boundingBoxs[idx];
            //NLog.i(TAG, "fast forward btn " + idx + " rect " + ff_bb);

            for (Rect bb2 : boundingBoxs) {
                if (ff_bb != bb2 && Rect.intersects(ff_bb, bb2)) {
                    //NLog.i(TAG, "colision with " + bb2);
                    int w = getMeasuredWidth();
                    int h = getMeasuredHeight();
                    boolean wrongPosition = false;

                    for (int i = 0; i < 300; i++) {
                        wrongPosition = false;
                        ff_bb.offset(10, 0);

                        if (ff_bb.right >= w) {
                            ff_bb.offsetTo(0, ff_bb.top + 10);
                            if (ff_bb.bottom >= h) {
                                break;
                            }
                        }
                        //NLog.i(TAG, i + " new rect " + ff_bb);
                        for (Rect bb3 : boundingBoxs) {
                            if (ff_bb != bb3 && Rect.intersects(ff_bb, bb3)) {
                                //NLog.i(TAG, "colision with " + bb3);
                                wrongPosition = true;
                                break;
                            }
                        }
                        if (!wrongPosition) {
                            break;
                        }
                    }

                    if (wrongPosition) {
                        NLog.i(TAG, "Nepodarilo se najit vhodnou pozici");
                        resetEditElement("");
                    } else {
                        NLog.i(TAG, "Podarilo se najit vhodnou pozici " + ff_bb + " "
                                + boundingBoxs[btnIdMap.indexOf(R.id.button_fast_forward)]);
                        for (EditElement elem : editElements) {
                            elem.computeBoundingBox();
                            elem.computeOffsets();
                        }
                    }
                }
            }
        }
    }

    public void setEnableStaticDPAD(boolean isEnable) {
        staticDPADEnabled = isEnable;

        for (View btn : btns) {
            if (dpadRIDs.contains(btn.getId())) {
                btn.setVisibility(isEnable ? View.VISIBLE : View.INVISIBLE);
                btn.setEnabled(isEnable);
            }
        }
        invalidate();
    }


    public enum EDIT_MODE {
        NONE, TOUCH, SCREEN
    }

    private interface OnEditItemClickListener {
        void onClick();
    }

    private class EditElement {

        RectF boundingbox = new RectF();

        ArrayList<Integer> ids = new ArrayList<>();
        ArrayList<RectF> offsets = new ArrayList<>();
        ArrayList<RectF> resizeRects = new ArrayList<>();

        boolean movable = true;
        RectF boundingboxHistory = new RectF();
        ArrayList<Rect> boundingboxsHistory = new ArrayList<>();
        ArrayList<RectF> offsetshistory = new ArrayList<>();


        boolean validPosition = true;
        float minimalSize;
        boolean isScreenElement = false;
        private RectF resizingBox = new RectF();
        private OnEditItemClickListener listener = null;

        public EditElement(int rid, boolean movable, float minimalSize) {
            int idx = ridToIdxMap.get(rid);

            if (idx != -1) {
                ids.add(idx);
                boundingbox.set(boundingBoxs[idx]);
                boundingboxHistory.set(boundingbox);
            }

            computeOffsets();
            this.movable = movable;
            this.minimalSize = minimalSize;
        }

        public EditElement(Rect viewPort) {
            isScreenElement = true;
            boundingbox.set(viewPort);
            boundingboxHistory.set(viewPort);
            computeOffsets();
            this.movable = true;
            this.minimalSize = 200;
        }

        public RectF getResizingBox() {
            final int K = resizeIcon.getHeight() / (isScreenElement ? 1 : 2);
            resizingBox.set(boundingbox.right - K, boundingbox.bottom - K,
                    boundingbox.right + K, boundingbox.bottom + K);
            return resizingBox;
        }

        public void add(int rid) {
            int idx = ridToIdxMap.get(rid);
            ids.add(idx);
            RectF tmp = new RectF();
            tmp.set(boundingBoxs[idx]);
            boundingbox.union(tmp);
            boundingboxHistory.set(boundingbox);
            computeOffsets();
        }

        public void computeOffsets() {
            offsets.clear();

            if (isScreenElement) {
                RectF offset = new RectF(boundingbox.left, boundingbox.top, 0, 0);
                offsets.add(offset);
            } else {
                for (Integer id : ids) {
                    Rect r = boundingBoxs[id];
                    RectF offset = new RectF(r.left - boundingbox.left, r.top - boundingbox.top, 0, 0);
                    offsets.add(offset);
                }
            }
        }

        public void computeBoundingBox() {
            if (!isScreenElement) {
                boundingbox.set(boundingBoxs[ids.get(0)]);

                for (Integer id : ids) {
                    Rect r = boundingBoxs[id];
                    RectF tmp = new RectF();
                    tmp.set(r);
                    boundingbox.union(tmp);
                }
            }
        }

        public EditElement saveHistory() {
            boundingboxsHistory.clear();
            offsetshistory.clear();

            if (isScreenElement) {
            } else {
                for (int i = 0; i < offsets.size(); i++) {
                    int id = ids.get(i);
                    boundingboxsHistory.add(new Rect(boundingBoxs[id]));
                    offsetshistory.add(new RectF(offsets.get(i)));
                }
            }
            return this;
        }

        public void setOnClickListener(OnEditItemClickListener listener) {
            this.listener = listener;
        }
    }
}
