package tz.co.wadau.documentscanner.adapters;


import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.AppCompatImageView;
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
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import tz.co.wadau.documentscanner.R;
import tz.co.wadau.documentscanner.models.Pdf;
import tz.co.wadau.documentscanner.utils.Utils;

import static tz.co.wadau.documentscanner.BrowsePDFActivity.GRID_VIEW_ENABLED;

public class SelectPDFAdapter extends RecyclerView.Adapter<SelectPDFAdapter.SelectPDFViewHolder> {
    private final String TAG = SelectPDFAdapter.class.getSimpleName();
    private List<Pdf> pdfFiles;
    private Context mContext;
    private Boolean isMultiSelect;
    private boolean isGridViewEnabled;
    private OnSelectedPdfClickListener staredPdfClickListener;
    private OnMultiSelectedPDFListener onMultiSelectedPDFListener;

    private SparseBooleanArray selectedPDFs = new SparseBooleanArray();
    private ActionMode actionMode;
    private ActionModeCallback actionModeCallback;

    public class SelectPDFViewHolder extends RecyclerView.ViewHolder {
        private TextView pdfHeader;
        private TextView lastModified;
        private TextView fileSize;
        private RelativeLayout pdfWrapper;
        private LinearLayout pdfContainer;
        private AppCompatImageView pdfThumbnail;
        private LinearLayout highlightSelectedItem;

        public SelectPDFViewHolder(View view) {
            super(view);

            if (isGridViewEnabled) {
                pdfThumbnail = view.findViewById(R.id.pdf_thumbnail);
                highlightSelectedItem = view.findViewById(R.id.highlight_selected_item);
            }
            pdfHeader = view.findViewById(R.id.pdf_header);
            lastModified = view.findViewById(R.id.pdf_last_modified);
            fileSize = view.findViewById(R.id.pdf_file_size);
            pdfWrapper = view.findViewById(R.id.pdf_wrapper);
            pdfContainer = view.findViewById(R.id.pdf_container);
        }
    }

    public SelectPDFAdapter(List<Pdf> pdfFiles, Context context, Boolean isMultiSelect) {
        this.pdfFiles = pdfFiles;
        this.mContext = context;
        this.isMultiSelect = isMultiSelect;
        actionModeCallback = new ActionModeCallback();
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        isGridViewEnabled = sharedPreferences.getBoolean(GRID_VIEW_ENABLED, false);

        if (mContext instanceof SelectPDFAdapter.OnSelectedPdfClickListener) {
            staredPdfClickListener = (SelectPDFAdapter.OnSelectedPdfClickListener) mContext;
        } else {
            throw new RuntimeException(mContext.toString() + " must implement OnSelectedPdfClickListener");
        }

        if (mContext instanceof OnMultiSelectedPDFListener) {
            onMultiSelectedPDFListener = (SelectPDFAdapter.OnMultiSelectedPDFListener) mContext;
        } else {
            throw new RuntimeException(mContext.toString() + " must implement OnMultiSelectedPDFListener");
        }
    }

    @Override
    public SelectPDFAdapter.SelectPDFViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View bookView;
        if (isGridViewEnabled) {
            bookView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_pdf_select_grid, parent, false);
        } else {
            bookView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_pdf_select, parent, false);
        }
        return new SelectPDFAdapter.SelectPDFViewHolder(bookView);
    }

    @Override
    public void onBindViewHolder(final SelectPDFAdapter.SelectPDFViewHolder holder, final int position) {
        Pdf pdfFile = pdfFiles.get(position);
        String pdfName = pdfFile.getName();
        final Long pdfSize = pdfFile.getLength();

        holder.pdfHeader.setText(pdfName);
        holder.fileSize.setText(android.text.format.Formatter.formatShortFileSize(mContext, pdfSize));
        holder.lastModified.setText(Utils.formatDateToHumanReadable(pdfFile.getLastModified()));
        toggleSelectionBackground(holder, position);

        if (isGridViewEnabled) {
            Picasso.with(mContext).load(pdfFile.getThumbUri()).into(holder.pdfThumbnail);
        }

        holder.pdfWrapper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (actionMode == null && isMultiSelect) {
                    actionMode = ((AppCompatActivity) mContext).startSupportActionMode(actionModeCallback);
                }

                if (isMultiSelect) {
                    toggleSelection(holder.getAdapterPosition());
                } else {
                    selectedPdfClicked(holder.getAdapterPosition());
                }
            }
        });
    }

    public void filter(List<Pdf> matchedFiles) {
        pdfFiles = matchedFiles;
        notifyDataSetChanged();
    }

    public void updateData(List<Pdf> updated) {
        pdfFiles = updated;
        notifyDataSetChanged();
    }

    //PDF SELECTION
    private void toggleSelection(int position) {

        if (selectedPDFs.get(position, false)) {
            selectedPDFs.delete(position);
        } else {
            selectedPDFs.put(position, true);
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
        return selectedPDFs.size();
    }

    private List<Integer> getSelectedPDFs() {
        int selectedTotal = selectedPDFs.size();
        List<Integer> items = new ArrayList<>();

        for (int i = 0; i < selectedTotal; i++) {
            items.add(selectedPDFs.keyAt(i));
        }
        return items;
    }

    private void clearSelection() {
        List<Integer> selection = getSelectedPDFs();
        selectedPDFs.clear();

        for (Integer i : selection) {
            notifyItemChanged(i);
        }
    }

    private boolean isSelected(int position) {
        return getSelectedPDFs().contains(position);
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
                    multiSelectedPDF(selectedPDFs());
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
            if (isGridViewEnabled) {
                holder.highlightSelectedItem.setVisibility(View.VISIBLE);
            } else {
                holder.pdfContainer.setBackgroundColor(ContextCompat.getColor(mContext, R.color.colorSelectedPDFs));
            }

        } else {
            if (isGridViewEnabled) {
                holder.highlightSelectedItem.setVisibility(View.GONE);
            } else {
                TypedValue outValue = new TypedValue();
                mContext.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
                holder.pdfContainer.setBackgroundResource(outValue.resourceId);
            }

        }
    }

    private ArrayList<String> selectedPDFs() {

        List<Integer> selectedPDFPositions = getSelectedPDFs();
        ArrayList<String> selectPDFPaths = new ArrayList<>();

        for (int selectedPDFPosition : selectedPDFPositions) {
            String pdfPath = pdfFiles.get(selectedPDFPosition).getAbsolutePath();
            selectPDFPaths.add(pdfPath);
        }

        return selectPDFPaths;
    }

    @Override
    public int getItemCount() {
        return pdfFiles.size();
    }

    public interface OnSelectedPdfClickListener {
        void onSelectedPdfClicked(Pdf pdfFile);

    }

    private void selectedPdfClicked(int position) {
        if (staredPdfClickListener != null) {
            staredPdfClickListener.onSelectedPdfClicked(pdfFiles.get(position));
        }
    }

    public interface OnMultiSelectedPDFListener {
        void onMultiSelectedPDF(ArrayList<String> selectedPDFPaths);
    }

    private void multiSelectedPDF(ArrayList<String> selectedPaths) {
        if (selectedPaths.size() <= 1) {
            Toast.makeText(mContext, "Please select more than 1 file", Toast.LENGTH_SHORT).show();
        } else {
            if (onMultiSelectedPDFListener != null) {
                onMultiSelectedPDFListener.onMultiSelectedPDF(selectedPaths);
            }
        }
    }
}
