package tz.co.wadau.documentscanner.adapters;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import tz.co.wadau.documentscanner.R;

public class ViewImagesAdapter  extends RecyclerView.Adapter<ViewImagesAdapter.ViewImagesViewHolder> {

    private final String TAG = ViewImagesAdapter.class.getSimpleName();
    private List<Uri> imageUris;
    private Context mContext;
    private ActionMode actionMode;
    private ViewImagesAdapter.ActionModeCallback actionModeCallback;
    private SparseBooleanArray selectedPages = new SparseBooleanArray();

    public class ViewImagesViewHolder extends RecyclerView.ViewHolder {
        AppCompatImageView imageView;
        LinearLayout highlightSelectedItem;

        private ViewImagesViewHolder(View view) {
            super(view);
            imageView = view.findViewById(R.id.image_view);
        }
    }

    public ViewImagesAdapter(Context context, List<Uri> imageUris) {
        this.imageUris = imageUris;
        this.mContext = context;
        actionModeCallback = new ViewImagesAdapter.ActionModeCallback();
    }

    @Override
    public ViewImagesAdapter.ViewImagesViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View imageView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_view_image, parent, false);
        return new ViewImagesAdapter.ViewImagesViewHolder(imageView);
    }

    @Override
    public void onBindViewHolder(final ViewImagesAdapter.ViewImagesViewHolder holder, final int position) {
        Picasso.with(mContext).load(imageUris.get(position)).into(holder.imageView);
//        toggleSelectionBackground(holder, position);

        holder.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                int mPosition = holder.getAdapterPosition();
//
//                if (actionMode == null) {
//                    actionMode = ((AppCompatActivity) mContext).startSupportActionMode(actionModeCallback);
//                }
//
//                toggleSelection(mPosition);
//                Log.d(TAG, "Clicked position " + mPosition);
            }
        });
    }

    private void toggleSelection(int position) {
        if (selectedPages.get(position, false)) {
            selectedPages.delete(position);
        } else {
            selectedPages.put(position, true);
        }

        notifyItemChanged(position);

        int count = selectedPages.size();
        if (count == 0) {
            actionMode.finish();
        } else {
            actionMode.setTitle(count + " " + mContext.getString(R.string.selected));
            actionMode.invalidate();
        }
    }

    private void toggleSelectionBackground(ViewImagesAdapter.ViewImagesViewHolder holder, int position) {
        if (isSelected(position)) {
//            holder.pdfWrapper.setBackground(mContext.getResources().getDrawable(R.drawable.border_colored));
            holder.highlightSelectedItem.setVisibility(View.VISIBLE);
        } else {
//            holder.pdfWrapper.setBackground(null);
            holder.highlightSelectedItem.setVisibility(View.GONE);
        }
    }

    private boolean isSelected(int position) {
        return getSelectedPages().contains(position);
    }

    @Override
    public int getItemCount() {
        return imageUris.size();
    }

    private void clearSelection() {
        List<Integer> selection = getSelectedPages();
        selectedPages.clear();

        for (Integer i : selection) {
            notifyItemChanged(i);
        }
    }

    private List<Integer> getSelectedPages() {
        int selectedTotal = selectedPages.size();
        List<Integer> items = new ArrayList<>();

        for (int i = 0; i < selectedTotal; i++) {
            items.add(selectedPages.keyAt(i));
        }
        return items;
    }

    private void removeItem(int position) {
        imageUris.remove(position);
        notifyItemRemoved(position);
    }

    private void removeItems(List<Integer> positions) {
        // Reverse-sort the list
        Collections.sort(positions, new Comparator<Integer>() {
            @Override
            public int compare(Integer lhs, Integer rhs) {
                return rhs - lhs;
            }
        });

        // Split the list in ranges
        while (!positions.isEmpty()) {
            if (positions.size() == 1) {
                removeItem(positions.get(0));
                positions.remove(0);
            } else {
                int count = 1;
                while (positions.size() > count && positions.get(count).equals(positions.get(count - 1) - 1)) {
                    ++count;
                }

                if (count == 1) {
                    removeItem(positions.get(0));
                } else {
                    removeRange(positions.get(count - 1), count);
                }

                for (int i = 0; i < count; ++i) {
                    positions.remove(0);
                }
            }
        }
    }

    private void removeRange(int positionStart, int itemCount) {
        for (int i = 0; i < itemCount; ++i) {
            imageUris.remove(positionStart);
        }
        notifyItemRangeRemoved(positionStart, itemCount);
    }

    private class ActionModeCallback implements ActionMode.Callback {
        View view = ((Activity) mContext).getWindow().getDecorView();
        int flags = view.getSystemUiVisibility();
        int colorFrom = mContext.getResources().getColor(R.color.colorPrimaryDark);
        int colorTo = mContext.getResources().getColor(R.color.colorDarkerGray);

        @Override
        public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
            actionMode.getMenuInflater().inflate(R.menu.activity_organize_pages, menu);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                    flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                    view.setSystemUiVisibility(flags);
                }

                ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
                colorAnimation.setDuration(300); // milliseconds
                colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void onAnimationUpdate(ValueAnimator animator) {
                        ((Activity) mContext).getWindow().setStatusBarColor((int) animator.getAnimatedValue());
                    }

                });

                colorAnimation.start();
            }
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            switch (menuItem.getItemId()) {
                case R.id.action_delete:
                    removeItems(getSelectedPages());
                    actionMode.finish();
                    break;
            }
            return true;
        }


        @Override
        public void onDestroyActionMode(ActionMode mode) {
            clearSelection();

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                    flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                    view.setSystemUiVisibility(flags);
                }

                //return to "old" color of status bar
                ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorTo, colorFrom);
                colorAnimation.setDuration(300); // milliseconds
                colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

                    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void onAnimationUpdate(ValueAnimator animator) {
                        ((Activity) mContext).getWindow().setStatusBarColor((int) animator.getAnimatedValue());
                    }

                });

                colorAnimation.start();

            }
            actionMode = null;
        }
    }

}
