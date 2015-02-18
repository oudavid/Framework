package com.aim.framework;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by dhull on 11/13/14.
 */
public class DraggableLinearLayoutManager extends LinearLayoutManager {
    private final AtomicBoolean mIsDragging;

    public DraggableLinearLayoutManager(Context context, AtomicBoolean isDragging) {
        super(context);
        mIsDragging = isDragging;
    }

    @Override
    public boolean canScrollVertically() {
        return ! mIsDragging.get();
    }
}
