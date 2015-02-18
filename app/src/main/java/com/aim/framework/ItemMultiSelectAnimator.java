package com.aim.framework;

import java.util.List;

/**
 * Created by dhull on /18/15.
 */
public interface ItemMultiSelectAnimator {
    public void animateItemChecked(DraggableAndMultiSelectableRecyclerView.ViewHolder holder);

    public void animateItemUnchecked(DraggableAndMultiSelectableRecyclerView.ViewHolder holder);

    public void onActionModeReset(List<Integer> selectedPositions);
}
