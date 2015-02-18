package com.aim.framework;

import java.util.List;

/**
 * Created by Administrator on 2/18/15.
 */
public interface MultiSelectAdapter {
    boolean isPositionSelected(int position);
    boolean toggleSelection(int position);
    void clearSelections();
    int getSelectedItemCount();
    List<Integer> getSelectedPositions();
}

