package tz.co.wadau.documentscanner.adapters;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import tz.co.wadau.documentscanner.R;
import tz.co.wadau.documentscanner.models.PDFPage;

public class OrganizePagesAdapter extends RecyclerView.Adapter<OrganizePagesAdapter.OrganizePagesViewHolder> {

    private final String TAG = OrganizePagesAdapter.class.getSimpleName();
    private List<PDFPage> pdfPages;
    private Context mContext;
    private ActionMode actionMode;
    private ActionModeCallback actionModeCallback;
    private SparseBooleanArray selectedPages = new SparseBooleanArray();

    public class OrganizePagesViewHolder extends RecyclerView.ViewHolder {
        private RelativeLayout pdfWrapper;
        private TextView pageNumber;
        AppCompatImageView thumbnail;
        LinearLayout highlightSelectedItem;

        private OrganizePagesViewHolder(View view) {
            super(view);
            pdfWrapper = view.findViewById(R.id.pdf_wrapper);
            pageNumber = view.findViewById(R.id.page_number);
            thumbnail = view.findViewById(R.id.pdf_thumbnail);
            highlightSelectedItem = view.findViewById(R.id.highlight_selected_item);
        }
    }

    public OrganizePagesAdapter(Context context, List<PDFPage> pdfPages) {
        this.pdfPages = pdfPages;
        this.mContext = context;
        actionModeCallback = new ActionModeCallback();
        Log.d(TAG, "number of thumbs " + pdfPages.size());
    }

    @Override
    public OrganizePagesViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View bookView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_organize_pages_grid, parent, false);
        return new OrganizePagesViewHolder(bookView);
    }

    @Override
    public void onBindViewHolder(final OrganizePagesViewHolder holder, final int position) {
        PDFPage page = pdfPages.get(position);
        Uri imageUri = page.getThumbnailUri();
        Picasso.with(mContext).load(imageUri).fit().into(holder.thumbnail);
        int pageNum = page.getPageNumber();
        holder.pageNumber.setText(String.valueOf(pageNum));
        toggleSelectionBackground(holder, position);

        holder.pdfWrapper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int mPosition = holder.getAdapterPosition();

                if (actionMode == null) {
                    actionMode = ((AppCompatActivity) mContext).startSupportActionMode(actionModeCallback);
                }

                toggleSelection(mPosition);
                Log.d(TAG, "Clicked position " + mPosition);
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

    private void toggleSelectionBackground(OrganizePagesViewHolder holder, int position) {
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
        return pdfPages.size();
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
        pdfPages.remove(position);
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
            pdfPages.remove(positionStart);
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
