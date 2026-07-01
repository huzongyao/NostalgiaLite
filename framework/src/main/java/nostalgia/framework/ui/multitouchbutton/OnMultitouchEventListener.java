package nostalgia.framework.ui.multitouchbutton;

/**
 * 多点触控事件监听器接口。
 * <p>
 * 当手指进入或离开多点触控按钮时触发回调。
 * </p>
 */
public interface OnMultitouchEventListener {
    void onMultitouchEnter(MultitouchBtnInterface btn);

    void onMultitouchExit(MultitouchBtnInterface btn);
}
