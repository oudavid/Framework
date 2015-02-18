package com.aim.framework;

/**
 * Allows the client to customize dragging behaviors
 * Created by dhull on 11/26/14.
 */
public interface ItemDragAnimator {
    /**
     * Apply transformation to the given dragged item.  Called every frame that a drag is being handled
     * @param holder
     * @param deltaX
     * @param deltaY
     */
    public void transformDraggingItem(DraggableAndMultiSelectableRecyclerView.ViewHolder holder, float deltaX, float deltaY);

    /**
     * Create and start the animation that will be used to return items to their "home position" after
     * a drag (is not called if the item is being deleted).
     * @param holder
     */
    public void animateReturnToHome(DraggableAndMultiSelectableRecyclerView.ViewHolder holder);
}
