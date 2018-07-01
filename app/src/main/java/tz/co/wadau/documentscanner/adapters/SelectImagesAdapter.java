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
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import tz.co.wadau.documentscanner.R;

public class SelectImagesAdapter extends RecyclerView.Adapter<SelectImagesAdapter.SelectPDFViewHolder> {
    private final String TAG = SelectImagesAdapter.class.getSimpleName();
    private List<Uri> imageUris;
    private Context mContext;
    private OnImageSelectedListener onMultiSelectedImageListener;

    private SparseBooleanArray selectedImages = new SparseBooleanArray();
    private ActionMode actionMode;
    private ActionModeCallback actionModeCallback;

    public class SelectPDFViewHolder extends RecyclerView.ViewHolder {
        private AppCompatImageView imageThumbnail;
        private LinearLayout highlightSelectedItem;

        public SelectPDFViewHolder(View view) {
            super(view);

            imageThumbnail = view.findViewById(R.id.image_thumb);
            highlightSelectedItem = view.findViewById(R.id.highlight_selected_item);
        }
    }

    public SelectImagesAdapter(Context context, List<Uri> imageUris) {
        this.imageUris = imageUris;
        this.mContext = context;
        actionModeCallback = new ActionModeCallback();

        if (mContext instanceof OnImageSelectedListener) {
            onMultiSelectedImageListener = (OnImageSelectedListener) mContext;
        } else {
            throw new RuntimeException(mContext.toString() + " must implement OnImageSelectedListener");
        }
    }

    @Override
    public SelectImagesAdapter.SelectPDFViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View bookView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_select_images_grid, parent, false);
        return new SelectImagesAdapter.SelectPDFViewHolder(bookView);
    }

    @Override
    public void onBindViewHolder(final SelectImagesAdapter.SelectPDFViewHolder holder, final int position) {
        Uri thumbnailUri = imageUris.get(position);
//        String pdfName = pdfFile.getName();
//        final Long pdfSize = pdfFile.length();
        toggleSelectionBackground(holder, position);

        Picasso.with(mContext).load(thumbnailUri).fit().centerCrop().into(holder.imageThumbnail);

        holder.imageThumbnail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (actionMode == null) {
                    actionMode = ((AppCompatActivity) mContext).startSupportActionMode(actionModeCallback);
                }

                toggleSelection(holder.getAdapterPosition());
            }
        });
    }

//    public void filter(List<File> matchedFiles) {
//        imageUris = matchedFiles;
//        notifyDataSetChanged();
//    }

    public void updateData(List<Uri> updated) {
        imageUris = updated;
        notifyDataSetChanged();
    }

    //PDF SELECTION
    private void toggleSelection(int position) {

        if (selectedImages.get(position, false)) {
            selectedImages.delete(position);
        } else {
            selectedImages.put(position, true);
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
        return selectedImages.size();
    }

    private List<Integer> getSelectedImages() {
        int selectedTotal = selectedImages.size();
        List<Integer> items = new ArrayList<>();

        for (int i = 0; i < selectedTotal; i++) {
            items.add(selectedImages.keyAt(i));
        }
        return items;
    }

    private void clearSelection() {
        List<Integer> selection = getSelectedImages();
        selectedImages.clear();

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
            mode.getMenuInflater().inflate(R.menu.selected_pdfs, menu);

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
                case R.id.action_select:
                    multiSelectedPDF(selectedImages());
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

    private void toggleSelectionBackground(SelectPDFViewHolder holder, int position) {
        if (isSelected(position)) {
            holder.highlightSelectedItem.setVisibility(View.VISIBLE);
        } else {
            holder.highlightSelectedItem.setVisibility(View.INVISIBLE);
        }
    }


    private ArrayList<String> selectedImages() {

        List<Integer> selectedPDFPositions = getSelectedImages();
        ArrayList<String> selectedImageUris = new ArrayList<>();

        for (int selectedPDFPosition : selectedPDFPositions) {
            String mUri = imageUris.get(selectedPDFPosition).toString();
            selectedImageUris.add(mUri);
        }

        return selectedImageUris;
    }

    @Override
    public int getItemCount() {
        return imageUris.size();
    }

//    public interface OnSelectedImageClickListener {
//        void onSelectedImageClicked(Uri imageUri);
//    }

    public interface OnImageSelectedListener {
        void onMultiSelectedPDF(ArrayList<String> selectedImageUris);
    }

    private void multiSelectedPDF(ArrayList<String> selectedImageUris) {
        if (onMultiSelectedImageListener != null) {
            onMultiSelectedImageListener.onMultiSelectedPDF(selectedImageUris);
        }
    }
}
