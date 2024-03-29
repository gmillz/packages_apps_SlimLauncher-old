/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.slim.slimlauncher;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Region;
import android.graphics.Region.Op;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.widget.TextView;

/**
 * An icon on a PagedView, specifically for items in the launcher's paged view (with compound
 * drawables on the top).
 */
public class PagedViewIcon extends TextView {
    /** A simple callback interface to allow a PagedViewIcon to notify when it has been pressed */
    public static interface PressedCallback {
        void iconPressed(PagedViewIcon icon);
    }

    private int mCount;

    @SuppressWarnings("unused")
    private static final String TAG = "PagedViewIcon";
    private static final float PRESS_ALPHA = 0.4f;

    private PagedViewIcon.PressedCallback mPressedCallback;
    private boolean mLockDrawableState = false;

    private int notificationBadgePosition = 0;

    private Bitmap mIcon;

    public PagedViewIcon(Context context) {
        this(context, null);
    }

    public PagedViewIcon(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public PagedViewIcon(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void onFinishInflate() {
        super.onFinishInflate();

        // Ensure we are using the right text size
        LauncherAppState app = LauncherAppState.getInstance();
        DeviceProfile grid = app.getDynamicGrid().getDeviceProfile();
        setTextSize(TypedValue.COMPLEX_UNIT_SP, grid.iconTextSize);
    }

    public void applyFromApplicationInfo(AppInfo info, boolean scaleUp,
            PagedViewIcon.PressedCallback cb) {
        mIcon = info.iconBitmap;
        mPressedCallback = cb;
        setCompoundDrawables(null, Utilities.createIconDrawable(mIcon),
                null, null);
        setText(info.title);
        setTag(info);
    }

    public void lockDrawableState() {
        mLockDrawableState = true;
    }

    public void resetDrawableState() {
        mLockDrawableState = false;
        post(new Runnable() {
            @Override
            public void run() {
                refreshDrawableState();
            }
        });
    }

    protected void drawableStateChanged() {
        super.drawableStateChanged();

        // We keep in the pressed state until resetDrawableState() is called to reset the press
        // feedback
        if (isPressed()) {
            setAlpha(PRESS_ALPHA);
            if (mPressedCallback != null) {
                mPressedCallback.iconPressed(this);
            }
        } else if (!mLockDrawableState) {
            setAlpha(1f);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        // If text is transparent, don't draw any shadow
        if (getCurrentTextColor() == getResources().getColor(android.R.color.transparent)) {
            getPaint().clearShadowLayer();
            super.draw(canvas);
            return;
        }

        // We enhance the shadow by drawing the shadow twice
        getPaint().setShadowLayer(BubbleTextView.SHADOW_LARGE_RADIUS, 0.0f,
                BubbleTextView.SHADOW_Y_OFFSET, BubbleTextView.SHADOW_LARGE_COLOUR);
        super.draw(canvas);
        canvas.save(Canvas.CLIP_SAVE_FLAG);
        canvas.clipRect(getScrollX(), getScrollY() + getExtendedPaddingTop(),
                getScrollX() + getWidth(),
                getScrollY() + getHeight(), Region.Op.INTERSECT);
        getPaint().setShadowLayer(BubbleTextView.SHADOW_SMALL_RADIUS, 0.0f, 0.0f,
                BubbleTextView.SHADOW_SMALL_COLOUR);
        super.draw(canvas);
        canvas.restore();

        Log.d("TEMP", "mCount=" + mCount);
        drawBadge(canvas);
    }

    public void setNotificationCount(int count, int id) {
        ItemInfo info = (ItemInfo) getTag();
        Integer idCount = info.mCounts.get(id);
        if (idCount != null) {
            info.mNotificationCount -= idCount;
            info.mCounts.remove(id);
        }
        if (id == -1) {
            info.mCounts.clear();
            info.mNotificationCount = 0;
        }
        if (count > 0) {
            info.mNotificationCount += count;
            info.mCounts.put(id, count);
        }
        if (info.mCounts.size() <= 0) {
            info.mNotificationCount = 0;
        }
        setNotificationCount(info.mNotificationCount);
    }

    public void setNotificationCount(int count) {
        mCount = count;
        postInvalidate();
    }

    private void drawBadge(Canvas c) {
        if (mCount != 0) {
            Drawable d = new Badge(getContext(), mCount).getDrawable();
            d.setBounds(0, 0, d.getIntrinsicWidth(), d.getIntrinsicHeight());
            c.translate(getTranslationX(d), getTranslationY(d));
            d.draw(c);
        }
    }

    private float getTranslationX(Drawable d) {
        if (notificationBadgePosition == 0
                || notificationBadgePosition == 3) {
            return getScrollX()
                    + (getWidth() / 2)
                    - (getCompoundDrawables()[1].getIntrinsicWidth() / 2);
        }

        return getScrollX()
                + (getWidth() / 2)
                + (getCompoundDrawables()[1].getIntrinsicWidth() / 2)
                - d.getIntrinsicWidth();
    }

    private float getTranslationY(Drawable d) {
        if (notificationBadgePosition == 0
                || notificationBadgePosition == 1) {
            return getScrollY()
                    + getCompoundDrawablePadding() * 2;
        }

        return getScrollY()
                + (getCompoundDrawablePadding() * 2)
                + getCompoundDrawables()[1].getIntrinsicHeight()
                - d.getIntrinsicHeight();
    }
}
