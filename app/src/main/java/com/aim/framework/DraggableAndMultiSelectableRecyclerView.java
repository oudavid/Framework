package com.aim.framework;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewPropertyAnimator;
import android.view.animation.OvershootInterpolator;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A subclass of {@link android.support.v7.widget.RecyclerView} that supports additional optional
 * features like item dragging (including drag-to-delete functionality) and multi-select.
 *
 * To support these features, clients should supply a subccass of {@link android.support.v7.widget.RecyclerView.Adapter}
 * instead of subclassing from the vanilla Adapter. These features can be enabled and disabled
 * programmatically or via XML when declaring the view.
 *
 * Clients can optionally supply an {@link ItemDragAnimator} and/or {@link ItemMultiSelectAnimator} to
 * customize the behavior of dragged items if the default animator fallback that comes with this view
 * doesn't suit your needs.
 *
 * Created by dhull on 11/13/14.
 */
public class DraggableAndMultiSelectableRecyclerView extends RecyclerView {
    private static final String TAG = DraggableAndMultiSelectableRecyclerView.class.getSimpleName();

    private static final boolean DEFAULT_ALLOW_DRAGGING = false;
    private static final boolean DEFAULT_ALLOW_MULTI_SELECT = false;

    private final EnumSet<RecyclerFeature> enabledFeatures = EnumSet.allOf(RecyclerFeature.class);

    private String mMultiSelectActionBarFormattedString;

    private ItemDragAnimator mDragAnimator = new DefaultDragAnimator();
    private ItemMultiSelectAnimator mMultiSelectAnimator = new DefaultMultiSelectAnimator();

    private DraggableAndMultiSelectableRecyclerViewListener mRecyclerViewListener;

    private ActionMode mActionMode;
    private ActionMode.Callback mActionModeCallback;

    public static enum RecyclerFeature {
        ITEM_DRAG,
        MULTI_SELECT
    }

    /**
     * Describes how the user is interacting with the card at the moment
     */
    public enum CardState {
        /**
         * User is neither SWIPING nor long-pressing this card at the moment
         */
        IDLE,

        /**
         * User is actively dragging the card around (has not released)
         */
        SWIPING,

        /**
         * User is actively pressing down (but not dragging) this card.
         */
        HOLDING
    }

    public interface DraggableAndMultiSelectableRecyclerViewListener {
        void onItemSelected(final DraggableAndMultiSelectableRecyclerView.ViewHolder holder);

        /**
         *
         * @param holder
         * @return true if the listener decides to accept this delete (in which case the listener
         * will handle removing the item). returning false instructs the calling SBRecyclerView to
         * animate the given item back into position via the drag animator.
         *
         * This allows clients to (for whatever reason), allow dragging without deletion, or to
         * put constraints on what can be deleted.
         *
         */
        boolean onDeleteItem(final ViewHolder holder);
    }

    // region CONSTRUCTORS
    public DraggableAndMultiSelectableRecyclerView(Context context) {
        this(context, null, 0);
    }

    public DraggableAndMultiSelectableRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);

    }

    public DraggableAndMultiSelectableRecyclerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final InternalTouchManager touchListener = new InternalTouchManager(context);
        addOnItemTouchListener(touchListener);
        setLayoutManager(new DraggableLinearLayoutManager(context, touchListener.getSwipingFlag()));
        // If provided, grab feature flags from given attributes; otherwise, initialize with defaults
        final boolean allowDrag, allowMultiSelect;
        final String multiSelectActionBarString;
        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(
                    attrs,
                    R.styleable.DraggableAndMultiSelectableRecyclerView);
            allowDrag = a.getBoolean(
                    R.styleable.DraggableAndMultiSelectableRecyclerView_sbAllowDragging,
                    DEFAULT_ALLOW_DRAGGING);
            allowMultiSelect = a.getBoolean(
                    R.styleable.DraggableAndMultiSelectableRecyclerView_sbAllowMultiSelect,
                    DEFAULT_ALLOW_MULTI_SELECT);
            multiSelectActionBarString = a.getString(
                    R.styleable.DraggableAndMultiSelectableRecyclerView_sbMultiSelectFormattedABString);
        } else {
            allowDrag = DEFAULT_ALLOW_DRAGGING;
            allowMultiSelect = DEFAULT_ALLOW_MULTI_SELECT;
            multiSelectActionBarString = null;
        }
        setFeatureEnabled(RecyclerFeature.ITEM_DRAG, allowDrag);
        setFeatureEnabled(RecyclerFeature.MULTI_SELECT, allowMultiSelect);
        if (multiSelectActionBarString != null)
            mMultiSelectActionBarFormattedString = multiSelectActionBarString;
        else
            mMultiSelectActionBarFormattedString = context.getString(R.string.s_7_298);
    }

    // endregion

    private void ensureModulesForFeature(RecyclerFeature feature) {
        switch (feature) {
            case ITEM_DRAG:
                if (mDragAnimator == null)
                    mDragAnimator = new DefaultDragAnimator();
                break;
            case MULTI_SELECT:
                if (mMultiSelectAnimator == null)
                    mMultiSelectAnimator = new DefaultMultiSelectAnimator();
                break;
        }
    }

    private void toggleSelection(View view, int itemIndex) {
        final MultiSelectAdapter multiSelectAdapter = (MultiSelectAdapter) getAdapter();
        final boolean didDeSelectLastItem = multiSelectAdapter.toggleSelection(itemIndex);

        final boolean isNowSelected = multiSelectAdapter.isPositionSelected(itemIndex);
        ViewHolder holder = (ViewHolder) getChildViewHolder(view);
        if (holder != null) {
            if (isNowSelected) {
                mMultiSelectAnimator.animateItemChecked(holder);
            } else {
                mMultiSelectAnimator.animateItemUnchecked(holder);
            }
        }

        if (!didDeSelectLastItem) {
            String title = String.format(mMultiSelectActionBarFormattedString, multiSelectAdapter.getSelectedItemCount());
            mActionMode.setTitle(title);
        } else {
            mActionMode.finish();
        }
    }

    public void resetActionMode() {
        mActionMode = null;
        final MultiSelectAdapter multiSelectAdapter = (MultiSelectAdapter) getAdapter();
        List<Integer> selectedItemPositions = multiSelectAdapter.getSelectedPositions();
        mMultiSelectAnimator.onActionModeReset(selectedItemPositions);
    }

    // region GETTERS AND SETTERS
    public String getMultiSelectActionBarFormattedString() {
        return mMultiSelectActionBarFormattedString;
    }

    public void setMultiSelectActionBarFormattedString(@NonNull String multiSelectActionBarFormattedString) {
        mMultiSelectActionBarFormattedString = multiSelectActionBarFormattedString;
    }

    public boolean isFeatureEnabled(RecyclerFeature feature) {
        return enabledFeatures.contains(feature);
    }

    public void setFeatureEnabled(RecyclerFeature feature, boolean enable) {
        if (enable) {
            enabledFeatures.add(feature);
            ensureModulesForFeature(feature);
        }
        else
            enabledFeatures.remove(feature);
    }

    public void setActionModeCallback(ActionMode.Callback actionModeCallback) {
        mActionModeCallback = actionModeCallback;
    }

    public void setRecyclerViewListener(DraggableAndMultiSelectableRecyclerViewListener listener) {
        mRecyclerViewListener = listener;
    }

    public ItemDragAnimator getDragAnimator() {
        return mDragAnimator;
    }

    /** Sets this view's ItemDragAnimator.  Note that calling this does not affect whether or not
     * dragging is currently enabled.
     *
     * @param dragAnimator Set to null to revert to default dragAnimator
     */
    public void setDragAnimator(ItemDragAnimator dragAnimator) {
        if (dragAnimator == null)
            dragAnimator =  new DefaultDragAnimator();
        mDragAnimator = dragAnimator;
    }

    public ItemMultiSelectAnimator getMultiSelectAnimator() {
        return mMultiSelectAnimator;
    }

    /** Sets this view's ItemMultiSelectAnimator.  Note that calling this does not affect whether or not
     * multi-select is currently enabled.
     *
     * @param multiSelectAnimator Set to null to revert to default multi-select animator
     */
    public void setMultiSelectAnimator(ItemMultiSelectAnimator multiSelectAnimator) {
        if (multiSelectAnimator == null)
            multiSelectAnimator = new DefaultMultiSelectAnimator();
        mMultiSelectAnimator = multiSelectAnimator;
    }

    /**
     *
     * @param adapter Must be instance of SBRecyclerView.Adapter
     */
    @Override
    public void setAdapter(RecyclerView.Adapter adapter) {
        if (! (adapter instanceof Adapter) )
            throw new IllegalArgumentException("SBRecyclerView requires it's adapter to subclass SBRecyclerView.Adapter, but was given " + adapter);

        super.setAdapter(adapter);
    }

    // endregion

    // ==========   TOUCH MANAGER
    private final class InternalTouchManager extends GestureDetector.SimpleOnGestureListener implements RecyclerView.OnItemTouchListener {
        private final GestureDetector gestureDetector;
        private final float mSwipeSlop;
        private boolean mIsActiveSwipePastThreshold = false;
        private final AtomicBoolean mIsSwiping = new AtomicBoolean(false);
        @Nullable
        private ViewHolder mLastSwipedView;

        private InternalTouchManager(Context context) {
            gestureDetector = new GestureDetector(context, this);
            mSwipeSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        }

        public AtomicBoolean getSwipingFlag() {
            return mIsSwiping;
        }

        //  =-=-=-= TOUCH MANAGER:  USER EVENT HANDLERS

        private void handleSwipeStart(final DraggableAndMultiSelectableRecyclerView.ViewHolder holder) {
            if (mLastSwipedView != null) {
                // TODO safety-check, probably doesn't need to be here after dev
                if (holder == mLastSwipedView)
                    Log.d(TAG, "Whups: attempting to re-start an active swipe");
                mLastSwipedView.state = CardState.IDLE;
            }
            holder.state = CardState.SWIPING;
            mLastSwipedView = holder;
            // just in case...
            mIsActiveSwipePastThreshold = false;
        }

        private void handleSwipeRelease(final DraggableAndMultiSelectableRecyclerView.ViewHolder holder) {
            mLastSwipedView = null;
            final boolean animateBackHome;

            if (mIsActiveSwipePastThreshold) {
                tempSwipeCheck(holder);
                // allow the animator to handle this animation, which is handled when the callback knows to delete
                animateBackHome = (mRecyclerViewListener == null || ! mRecyclerViewListener.onDeleteItem(holder));
                mIsActiveSwipePastThreshold = false;
            } else {
                animateBackHome = true;
            }
            if (animateBackHome) {
                // reset holder state and animate back in place
                holder.state = DraggableAndMultiSelectableRecyclerView.CardState.IDLE;
                mDragAnimator.animateReturnToHome(holder);
            }
        }

        // =-=-=-=  TOUCH MANAGER:  GESTURE LISTENER METHODS

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            final View tappedView = findChildViewUnder(e.getX(), e.getY());
            if (tappedView == null)
                return super.onSingleTapUp(e);

            if (mActionMode == null) {
                ViewHolder holder = (ViewHolder) getChildViewHolder(tappedView);
                if (holder == null)
                    return super.onSingleTapUp(e);

                if (mRecyclerViewListener != null)
                    mRecyclerViewListener.onItemSelected(holder);

            } else {
                View view = findChildViewUnder(e.getX(), e.getY());
                if (view != null) {
                    final int viewPosition = getChildPosition(view);
                    if (viewPosition != NO_POSITION) {
                        toggleSelection(view, viewPosition);
                    }
                }
            }
            return super.onSingleTapUp(e);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceThisFrameX, float distanceThisFrameY) {

            // never capture if dragging is disabled
            if (! isFeatureEnabled(RecyclerFeature.ITEM_DRAG))
                return false;

            final float deltaX = e2.getX() - e1.getX();
            final float deltaY = e2.getY() - e1.getY();
            final float deltaXAbs = Math.abs(deltaX);

            View v = findChildViewUnder(e2.getX(), e2.getY());

            // ----Ripcord out if no view is found
            if (v == null) {
                Log.d(TAG, "No view found at position " + e2.getX() + " , " + e2.getY());
                return false;
            }

            // Check to ensure we actually want to drag (but cotinue dragging if we already are)

            if (!mIsSwiping.get() && shouldIgnoreDrag(deltaX, deltaY, distanceThisFrameX, distanceThisFrameY)) {
                // TODO Hull remove temp state check?
                if (mLastSwipedView != null) {
                    Log.e(TAG, "Ignoring drag event while drag is in progress.  Releasing...");
                    handleSwipeRelease(mLastSwipedView);
                }
                return false;
            }

            if (mLastSwipedView != null && v != mLastSwipedView.itemView) {
                v = mLastSwipedView.itemView;
            }

            final ViewHolder holder = (ViewHolder) getChildViewHolder(v);

            // Set 'Swiping' to true.  If it just started, do special stuff
            if (!mIsSwiping.getAndSet(true)) {
                handleSwipeStart(holder);
            }

            mDragAnimator.transformDraggingItem(holder, deltaX, deltaY);

            final boolean isBeyondThreshold = deltaXAbs > (v.getWidth() / 4.5f);
            if (isBeyondThreshold != mIsActiveSwipePastThreshold) {
                // handle crossing (or un-crossing) threshold
                mIsActiveSwipePastThreshold = isBeyondThreshold;
                // TODO consider pulling this into a custom mid-level input listener
                HapticFeedbackUtils.provideHapticFeedback(getContext());
            }

            return false;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            // never capture if multi-select is disabled
            if (! isFeatureEnabled(RecyclerFeature.MULTI_SELECT))
                return;

            View view = findChildViewUnder(e.getX(), e.getY());
            if (view == null || mActionMode != null) {
                return;
            }
            mActionMode = startActionMode(mActionModeCallback);
            int viewIndex = getChildPosition(view);
            toggleSelection(view, viewIndex);
            HapticFeedbackUtils.provideHapticFeedback(getContext());
            super.onLongPress(e);
        }

        //=-=-=-=-= TOUCH MANAGER:  TOUCH ITEM LISTENER METHODS
        @Override
        public boolean onInterceptTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {
            // Check if we're swiping, reset to false no matter what
            if (motionEvent.getAction() == MotionEvent.ACTION_UP && mIsSwiping.getAndSet(false)) {
                final ViewHolder holder = mLastSwipedView;
                if (holder != null) {
                    handleSwipeRelease(holder);
                }
                else {
                    Log.e(TAG, "swipe-up intercepted without lastSwipedView being set.");
                }
            }

            return gestureDetector.onTouchEvent(motionEvent);
        }

        @Override
        public void onTouchEvent(RecyclerView recyclerView, MotionEvent motionEvent) {

        }

        // =-=-=-=-= TOUCH MANAGER:  HELPERS

        /**
         * A method used to check and ensure state, so we can capture as many potential bugs as possible without
         * cluttering the code with too much boilerplate.  Will very likely be removed
         */
        private void tempSwipeCheck(DraggableAndMultiSelectableRecyclerView.ViewHolder holder) {
            // TODO remove sanity-checks below after we're solid
            if (holder.state != CardState.SWIPING) {
                Log.e(TAG, "View was never tagged as swiping");
                holder.state = CardState.SWIPING;
            }
        }


        private boolean shouldIgnoreDrag(float deltaX, float deltaY, float distanceThisFrameX, float distanceThisFrameY) {
            // --- First, make sure the view is in a state that allows dragging
            if  (mActionMode != null || Math.abs(deltaX) < mSwipeSlop || getItemAnimator().isRunning() )
                return true;
                // --- Second, make sure that if we're not already dragging, we only start if the user clearly intends to swipe horizontally
            else if (!mIsSwiping.get()) {
                //final float requiredXScale = (getScrollState() != RecyclerView.SCROLL_STATE_IDLE) ? 5f : 7.5f;
                if (Math.abs(distanceThisFrameY) * 5 > Math.abs(distanceThisFrameX))
                    return true;
                // To catch edge cases where the user would scroll down and let their finger
                // rest, eventually triggering a drag
                if (Math.abs(deltaY) > 50 || Math.abs(deltaX) > mSwipeSlop * 5)
                    return true;
            }
            return false;
        }
    }

    /**
     * A subclass of vanilla ViewHolder that also stores a flag that SBRecyclerView uses to
     * handle dragging and other input.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public DraggableAndMultiSelectableRecyclerView.CardState state = DraggableAndMultiSelectableRecyclerView.CardState.IDLE;

        public ViewHolder(View itemView) {
            super(itemView);
        }
    }

    /**
     * Subclass this when creating the Adapter for use with this RecyclerView. This
     * adapter handles multi-select logic (when necessary) and enforces the use of
     * {@link ViewHolder}
     */
    public static abstract class Adapter<T extends ViewHolder>
            extends RecyclerView.Adapter<T> implements MultiSelectAdapter {
        protected final SparseBooleanArray mSelectedItems = new SparseBooleanArray();


        @Override
        public boolean toggleSelection(int position) {
            final int previousSize = mSelectedItems.size();

            if (mSelectedItems.get(position, false)) {
                mSelectedItems.delete(position);
            } else {
                mSelectedItems.put(position, true);
            }

            final int currentSize = mSelectedItems.size();
            return previousSize >= 0 && currentSize == 0;
        }

        @Override
        public void clearSelections() {
            mSelectedItems.clear();
        }

        @Override
        public int getSelectedItemCount() {
            return mSelectedItems.size();
        }

        @Override
        public List<Integer> getSelectedPositions() {
            List<Integer> items = new ArrayList<Integer>(mSelectedItems.size());
            for (int i = 0; i < mSelectedItems.size(); i++) {
                items.add(mSelectedItems.keyAt(i));
            }
            return items;

        }
        @Override
        public boolean isPositionSelected(int position) {
            return (mSelectedItems.get(position, false));
        }
    }


    /** An item can only be dragged left or right; no scaling or alpha.  */
    private static class DefaultDragAnimator implements ItemDragAnimator {

        @Override
        public void transformDraggingItem(ViewHolder holder, float deltaX, float deltaY) {
            holder.itemView.setTranslationX(deltaX);
        }

        @Override
        public void animateReturnToHome(ViewHolder holder) {
            final ViewPropertyAnimator anim = holder.itemView.animate();
            anim.cancel();
            anim.translationX(0);
            anim.setDuration(250);
            anim.setInterpolator(new OvershootInterpolator());
            anim.setStartDelay(0);
            anim.start();
        }
    }

    private static class DefaultMultiSelectAnimator implements ItemMultiSelectAnimator {
        @Override
        public void animateItemChecked(ViewHolder holder) {
            Log.e(TAG, "Stubbed DefaultMultiSelectAnimator is trying to animate a check.");
        }

        @Override
        public void animateItemUnchecked(ViewHolder holder) {
            Log.e(TAG, "Stubbed DefaultMultiSelectAnimator is trying to animate an uncheck.");
        }

        @Override
        public void onActionModeReset(List<Integer> selectedPositions) {
            Log.e(TAG, "Stubbed DefaultMultiSelectAnimator has been called... why?");
        }
    }
}
