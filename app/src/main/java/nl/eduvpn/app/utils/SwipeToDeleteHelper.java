/*
 *  This file is part of eduVPN.
 *
 *     eduVPN is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     eduVPN is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with eduVPN.  If not, see <http://www.gnu.org/licenses/>.
 */

package nl.eduvpn.app.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Pair;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import nl.eduvpn.app.R;
import nl.eduvpn.app.adapter.ProfileAdapter;
import nl.eduvpn.app.entity.Instance;
import nl.eduvpn.app.entity.Profile;

/**
 * Swipe to delete helper based on: https://github.com/nemanja-kovacevic/recycler-view-swipe-to-delete
 * Created by Daniel Zolnai on 2016-10-26.
 */
public class SwipeToDeleteHelper extends ItemTouchHelper.SimpleCallback {

    private Drawable _background;
    private Drawable _deleteIcon;
    private int _deleteIconMargin;
    private ProfileAdapter _profileAdapter;

    public SwipeToDeleteHelper(Context context) {
        super(0, ItemTouchHelper.LEFT);
        _background = new ColorDrawable(ContextCompat.getColor(context, R.color.swipeBackgroundColor));
        _deleteIcon = ContextCompat.getDrawable(context, R.drawable.delete_icon);
        _deleteIcon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_ATOP);
        _deleteIconMargin = (int)context.getResources().getDimension(R.dimen.swipe_to_delete_margin);
    }

    @Override
    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
        // Not important, we don't want drag & drop
        return false;
    }

    @Override
    public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
        int position = viewHolder.getAdapterPosition();
        _profileAdapter = (ProfileAdapter)recyclerView.getAdapter();
        if (_profileAdapter.isUndoEnabled() && _profileAdapter.isPendingRemoval(position)) {
            return 0;
        }
        return super.getSwipeDirs(recyclerView, viewHolder);
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
        int swipedPosition = viewHolder.getAdapterPosition();
        boolean undoOn = _profileAdapter.isUndoEnabled();
        if (undoOn) {
            _profileAdapter.pendingRemoval(swipedPosition);
        } else {
            Pair<Instance, Profile> item = _profileAdapter.getItem(swipedPosition);
            _profileAdapter.remove(item);
        }
    }


    @Override

    public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        View itemView = viewHolder.itemView;
        // Not sure why, but this method gets called for view holders that are already swiped away
        if (viewHolder.getAdapterPosition() == -1) {
            // Not interested in those
            return;
        }
        // Draw red _background
        _background.setBounds(itemView.getRight() + (int)dX, itemView.getTop(), itemView.getRight(), itemView.getBottom());
        _background.draw(c);
        // Draw X mark
        int itemHeight = itemView.getBottom() - itemView.getTop();
        int intrinsicWidth = _deleteIcon.getIntrinsicWidth();
        int intrinsicHeight = _deleteIcon.getIntrinsicWidth();
        int xMarkLeft = itemView.getRight() - _deleteIconMargin - intrinsicWidth;
        int xMarkRight = itemView.getRight() - _deleteIconMargin;
        int xMarkTop = itemView.getTop() + (itemHeight - intrinsicHeight) / 2;
        int xMarkBottom = xMarkTop + intrinsicHeight;
        _deleteIcon.setBounds(xMarkLeft, xMarkTop, xMarkRight, xMarkBottom);
        _deleteIcon.draw(c);
        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }
}
