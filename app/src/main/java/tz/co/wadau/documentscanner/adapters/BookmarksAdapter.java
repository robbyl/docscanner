package tz.co.wadau.documentscanner.adapters;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import tz.co.wadau.documentscanner.R;
import tz.co.wadau.documentscanner.data.DbHelper;
import tz.co.wadau.documentscanner.models.Bookmark;

public class BookmarksAdapter extends RecyclerView.Adapter<BookmarksAdapter.BookmarksViewHolder> {
    private final String TAG = BookmarksAdapter.class.getSimpleName();
    private List<Bookmark> bookmarks;
    private Context mContext;
    private LinearLayout emptyStateView;
    private OnBookmarkClickedListener onBookmarkClickedListener;
    private ActionMode actionMode;
    private BookmarksAdapter.ActionModeCallback actionModeCallback;
    private SparseBooleanArray selectedBookmarks = new SparseBooleanArray();

    public class BookmarksViewHolder extends RecyclerView.ViewHolder {
        private TextView bookmarkTitle, bookmarkPage;
        private RelativeLayout bookmarkWrapper;
        private LinearLayout highlightSelectedItem;

        public BookmarksViewHolder(View view) {
            super(view);

            bookmarkTitle = view.findViewById(R.id.bookmark_title);
            bookmarkPage = view.findViewById(R.id.bookmark_page);
            bookmarkWrapper = view.findViewById(R.id.bookmark_wrapper);
            highlightSelectedItem = view.findViewById(R.id.highlight_selected_item);
        }
    }

    public BookmarksAdapter(Context context, List<Bookmark> bookmarks, LinearLayout emptyStateView) {
        this.bookmarks = bookmarks;
        this.mContext = context;
        this.emptyStateView = emptyStateView;
        actionModeCallback = new BookmarksAdapter.ActionModeCallback();

        if (mContext instanceof OnBookmarkClickedListener) {
            onBookmarkClickedListener = (OnBookmarkClickedListener) mContext;
        } else {
            throw new RuntimeException(mContext.toString() + " must implement OnBookmarkClickedListener");
        }
    }

    @Override
    public BookmarksAdapter.BookmarksViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View bookView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_bookmark, parent, false);
        return new BookmarksViewHolder(bookView);
    }

    @Override
    public void onBindViewHolder(final BookmarksAdapter.BookmarksViewHolder holder, final int position) {
        final Bookmark bookmark = bookmarks.get(position);
        holder.bookmarkTitle.setText(bookmark.getTitle());
        String pageNo = mContext.getString(R.string.page) + " " + bookmark.getPageNumber();
        holder.bookmarkPage.setText(pageNo);
        toggleSelectionBackground(holder, position);

        holder.bookmarkWrapper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (actionMode != null) {
                    toggleSelection(holder.getAdapterPosition());
                } else {
                    bookmarkSelected(bookmark);
                }
            }
        });

        holder.bookmarkWrapper.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (actionMode == null) {
                    actionMode = ((AppCompatActivity) mContext).startSupportActionMode(actionModeCallback);
                }

                toggleSelection(holder.getAdapterPosition());
                return false;
            }
        });
    }

//    public void filter(List<File> matchedFiles) {
//        bookmarks = matchedFiles;
//        notifyDataSetChanged();
//    }

    public void updateData(List<Bookmark> updated) {
        bookmarks = updated;
        notifyDataSetChanged();
    }

    //BOOKMARK SELECTION
    private void toggleSelection(int position) {

        if (selectedBookmarks.get(position, false)) {
            selectedBookmarks.delete(position);
        } else {
            selectedBookmarks.put(position, true);
        }

        notifyItemChanged(position);
        int count = getSelectedItemCount();
        if (count == 0) {
            actionMode.finish();
        } else {
            actionMode.setTitle(count + " " + mContext.getString(R.string.selected));
            actionMode.invalidate();
        }
    }

    private int getSelectedItemCount() {
        return selectedBookmarks.size();
    }

    private List<Integer> getSelectedImages() {
        int selectedTotal = selectedBookmarks.size();
        List<Integer> items = new ArrayList<>();

        for (int i = 0; i < selectedTotal; i++) {
            items.add(selectedBookmarks.keyAt(i));
        }
        return items;
    }

    private void clearSelection() {
        List<Integer> selection = getSelectedImages();
        selectedBookmarks.clear();

        for (Integer i : selection) {
            notifyItemChanged(i);
        }
    }

    private boolean isSelected(int position) {
        return getSelectedImages().contains(position);
    }

    private class ActionModeCallback implements ActionMode.Callback {

        View view = ((Activity) mContext).getWindow().getDecorView();
        int flags = view.getSystemUiVisibility();
        int colorFrom = mContext.getResources().getColor(R.color.colorPrimaryDark);
        int colorTo = mContext.getResources().getColor(R.color.colorDarkerGray);

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.fragment_bookmarks, menu);

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
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_delete:
                    deleteSelectedBookmarks(mode);
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

    private void toggleSelectionBackground(BookmarksAdapter.BookmarksViewHolder holder, int position) {
        if (isSelected(position)) {
            holder.highlightSelectedItem.setBackgroundColor(ContextCompat.getColor(mContext, R.color.colorSelectedPDFs));
        } else {
            TypedValue outValue = new TypedValue();
            mContext.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            holder.highlightSelectedItem.setBackgroundResource(outValue.resourceId);
        }
    }


//    private ArrayList<String> selectedImages() {
//
//        List<Integer> selectedPDFPositions = getSelectedImages();
//        ArrayList<String> selectedImageUris = new ArrayList<>();
//
//        for (int selectedPDFPosition : selectedPDFPositions) {
//            String mUri = bookmarks.get(selectedPDFPosition).toString();
//            selectedImageUris.add(mUri);
//        }
//
//        return selectedImageUris;
//    }

    @Override
    public int getItemCount() {
        return bookmarks.size();
    }

    public void bookmarkSelected(Bookmark bookmark) {
        onBookmarkClickedListener.onBookmarkClicked(bookmark);
    }

    public interface OnBookmarkClickedListener {
        void onBookmarkClicked(Bookmark bookmark);
    }

    private void deleteSelectedBookmarks(final ActionMode mode) {

        final DbHelper dbHelper = DbHelper.getInstance(mContext);
        List<Integer> selectedBookmarks = getSelectedBookmarks();
        int selectedSize = getSelectedItemCount();
        List<Bookmark> deleteBookmarks = new ArrayList<>();

        for (int m = 0; m < selectedSize; m++) {
            deleteBookmarks.add(bookmarks.get(selectedBookmarks.get(m)));
        }

        removeItems(selectedBookmarks);
        mode.finish();
        dbHelper.deleteBookmarks(deleteBookmarks);
    }

    private void removeItem(int position) {
        bookmarks.remove(position);
        setupEmptyState();
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
            bookmarks.remove(positionStart);
        }

        setupEmptyState();
        notifyItemRangeRemoved(positionStart, itemCount);
    }

    private List<Integer> getSelectedBookmarks() {
        int selectedTotal = selectedBookmarks.size();
        List<Integer> items = new ArrayList<>();

        for (int i = 0; i < selectedTotal; i++) {
            items.add(selectedBookmarks.keyAt(i));
        }
        return items;
    }


    private void setupEmptyState() {
        if (bookmarks.size() > 0) {
            emptyStateView.setVisibility(View.GONE);
//            recyclerView.setVisibility(View.VISIBLE);
        } else {
            emptyStateView.setVisibility(View.VISIBLE);
//            recyclerView.setVisibility(View.GONE);
        }
    }

}
