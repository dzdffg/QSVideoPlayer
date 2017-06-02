package org.song.demo.listvideo;

import android.graphics.Rect;
import android.os.Handler;
import android.view.View;

/**
 * list列表 item的生命周期控制(即将滚出屏幕 和 进入活动状态的item 监听)
 * item高度大于listview的一半以上最好
 * Created by song on 2017/6/1.
 */

public class ListCalculator {
    private Getter getter;
    private CallBack callBack;

    private int VISIBILITY_PERCENTS = 70;


    public ListCalculator(Getter getter, CallBack callBack) {
        this.getter = getter;
        this.callBack = callBack;
    }

    public int getCurrentActiveItem() {
        return currentActiveItem;
    }

    //todo 如果外部手动点了播放 需要设置这里的活动item
    public void setCurrentActiveItem(int currentActiveItem) {
        if (this.currentActiveItem != currentActiveItem) {
            int firstVisiblePosition = getter.getFirstVisiblePosition();
            View currentView = getter.getChildAt(this.currentActiveItem - firstVisiblePosition);
            if (currentView != null)
                callBack.deactivate(currentView, this.currentActiveItem);

            currentView = getter.getChildAt(currentActiveItem - firstVisiblePosition);
            if (currentView != null) {
                callBack.setActive(currentView, currentActiveItem);
                this.currentActiveItem = currentActiveItem;
            }
        }
    }

    private int currentActiveItem = 0;//当前活动的item

    /**
     * true 滚动结束后才会播放
     * false 滚动过程就会播放
     */
    public void setScrollIdel(boolean scrollIdel) {
        isScrollIdel = scrollIdel;
    }

    private boolean isScrollUp = true;//滚动方向
    private boolean isScrollIdel = true;//是否停止滚动才设置播放
    private boolean isActiveFlag = false;//
    private boolean isFristFlag = true;//

    /**
     * 滚动中
     */
    public void onScrolling() {
        if (!checkUnDown())
            return;

        int firstVisiblePosition = getter.getFirstVisiblePosition();
        int lastVisiblePosition = getter.getLastVisiblePosition();


        int activeItem = currentActiveItem;
        //确保 活动item 在屏幕里了
        if (activeItem < firstVisiblePosition || activeItem > lastVisiblePosition) {
            activeItem = isScrollUp ? firstVisiblePosition : lastVisiblePosition;
        }
        //计算当前活动的应该是哪个item
        View currentView = getter.getChildAt(activeItem - firstVisiblePosition);
        int currentP = getVisibilityPercents(currentView);
        if (isScrollUp) {//往上滚动

            if (lastVisiblePosition >= activeItem + 1) {//存在下一个item
                View nextView = getter.getChildAt(activeItem + 1 - firstVisiblePosition);
                int nextP = getVisibilityPercents(nextView);
                if (enoughPercentsForDeactivation(currentP, nextP)) {
                    activeItem = activeItem + 1;
                }
            }

        } else {//往下滚动

            if (firstVisiblePosition <= activeItem - 1) {//存在上一个item
                View preView = getter.getChildAt(activeItem - 1 - firstVisiblePosition);
                int preP = getVisibilityPercents(preView);
                if (enoughPercentsForDeactivation(currentP, preP)) {
                    activeItem = activeItem - 1;
                }
            }
        }
        //不一样说明活动的item改变了
        if (activeItem != currentActiveItem) {
            View v1 = getter.getChildAt(currentActiveItem - firstVisiblePosition);
            if (v1 != null)
                callBack.deactivate(v1, currentActiveItem);
            if (!isScrollIdel) {
                View v2 = getter.getChildAt(activeItem - firstVisiblePosition);
                if (v2 != null)
                    callBack.setActive(v2, activeItem);
            } else
                isActiveFlag = true;
            currentActiveItem = activeItem;
        }

        if (isFristFlag) {
            isFristFlag = false;
            if (currentView != null)
                callBack.setActive(currentView, activeItem);
        }


    }

    private boolean enoughPercentsForDeactivation(int visibilityPercents, int nextVisibilityPercents) {
        return (visibilityPercents < VISIBILITY_PERCENTS && nextVisibilityPercents >= VISIBILITY_PERCENTS) ||
                (visibilityPercents <= 20 && visibilityPercents > 0) ||//活动的item快要消失
                (visibilityPercents < 95 && nextVisibilityPercents == 100);//下一个item100%显示了 上一个活动item只需要隐藏一点就切换
    }

    /**
     * 停止滚动
     */
    public void onScrolled(int delayed) {
        if (isActiveFlag) {
            isActiveFlag = false;
            handler.removeCallbacks(run);
            handler.postDelayed(run, delayed);
        }
    }

    private Handler handler = new Handler();
    private Runnable run = new Runnable() {
        @Override
        public void run() {
            View v = getter.getChildAt(currentActiveItem - getter.getFirstVisiblePosition());
            if (v != null)
                callBack.setActive(v, currentActiveItem);
        }
    };


    private int mOldTop;
    private int mOldFirstVisibleItem;

    //检测滑动方向
    private boolean checkUnDown() {
        View view = getter.getChildAt(0);
        if (view == null)
            return false;
        int top = view.getTop();

        int firstVisibleItem = getter.getFirstVisiblePosition();
        if (firstVisibleItem == mOldFirstVisibleItem) {
            if (top > mOldTop) {
                isScrollUp = false;
            } else if (top < mOldTop) {
                isScrollUp = true;
            }
        } else {
            isScrollUp = firstVisibleItem > mOldFirstVisibleItem;
        }

        mOldTop = top;
        mOldFirstVisibleItem = firstVisibleItem;
        return true;
    }


    public int getVisibilityPercents(View view) {
        final Rect currentViewRect = new Rect();

        int percents = 100;

        int height = (view == null || view.getVisibility() != View.VISIBLE) ? 0 : view.getHeight();

        if (height == 0) {
            return 0;
        }

        view.getLocalVisibleRect(currentViewRect);

        if (viewIsPartiallyHiddenTop(currentViewRect)) {
            // view is partially hidden behind the top edge
            percents = (height - currentViewRect.top) * 100 / height;
        } else if (viewIsPartiallyHiddenBottom(currentViewRect, height)) {
            percents = currentViewRect.bottom * 100 / height;
        }

        return percents;
    }

    private boolean viewIsPartiallyHiddenBottom(Rect currentViewRect, int height) {
        return currentViewRect.bottom > 0 && currentViewRect.bottom < height;
    }

    private boolean viewIsPartiallyHiddenTop(Rect currentViewRect) {
        return currentViewRect.top > 0;
    }
}