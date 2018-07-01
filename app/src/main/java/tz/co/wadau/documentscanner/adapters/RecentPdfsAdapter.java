package tz.co.wadau.documentscanner.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;

import java.util.List;

import tz.co.wadau.documentscanner.DataUpdatedEvent;
import tz.co.wadau.documentscanner.R;
import tz.co.wadau.documentscanner.data.DbHelper;
import tz.co.wadau.documentscanner.data.FileDiffCallback;
import tz.co.wadau.documentscanner.fragments.BottomSheetDialogFragment;
import tz.co.wadau.documentscanner.models.Pdf;
import tz.co.wadau.documentscanner.utils.Utils;

import static tz.co.wadau.documentscanner.BrowsePDFActivity.GRID_VIEW_ENABLED;
import static tz.co.wadau.documentscanner.fragments.BottomSheetDialogFragment.FROM_RECENT;
import static tz.co.wadau.documentscanner.fragments.BottomSheetDialogFragment.PDF_PATH;

public class RecentPdfsAdapter extends RecyclerView.Adapter<RecentPdfsAdapter.PdfViewHolder> {
    private final String TAG = RecentPdfsAdapter.class.getSimpleName();
    private List<Pdf> pdfFiles;
    private Context mContext;
    private OnHistoryPdfClickListener historyPdfClickListener;
    //    private ActionMode actionMode;
    private boolean isGridViewEnabled;
//    private ActionModeCallback actionModeCallback;

    public class PdfViewHolder extends RecyclerView.ViewHolder {
        private TextView pdfHeader;
        private TextView lastModified;
        private AppCompatImageView pdfThumbnail;
        private TextView fileSize;
        private AppCompatImageView toggleStar;
        private RelativeLayout pdfWrapper;

        private PdfViewHolder(View view) {
            super(view);

            if (isGridViewEnabled)
                pdfThumbnail = view.findViewById(R.id.pdf_thumbnail);
            pdfHeader = view.findViewById(R.id.pdf_header);
            lastModified = view.findViewById(R.id.pdf_last_modified);
            fileSize = view.findViewById(R.id.pdf_file_size);
            toggleStar = view.findViewById(R.id.toggle_star);
            pdfWrapper = view.findViewById(R.id.pdf_wrapper);
        }
    }

    public RecentPdfsAdapter(List<Pdf> pdfFiles, Context context) {
        this.pdfFiles = pdfFiles;
        this.mContext = context;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        isGridViewEnabled = sharedPreferences.getBoolean(GRID_VIEW_ENABLED, false);
//        actionModeCallback = new ActionModeCallback();

        if (mContext instanceof OnHistoryPdfClickListener) {
            historyPdfClickListener = (OnHistoryPdfClickListener) mContext;
        } else {
            throw new RuntimeException(mContext.toString() + " must implement OnHistoryPdfClickListener");
        }
    }

    @Override
    public RecentPdfsAdapter.PdfViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View bookView;
        if (isGridViewEnabled) {
            bookView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_pdf_grid, parent, false);
        } else {
            bookView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_pdf, parent, false);
        }
        return new PdfViewHolder(bookView);
    }

    @Override
    public void onBindViewHolder(final RecentPdfsAdapter.PdfViewHolder holder, final int position) {
        Pdf pdfFile = pdfFiles.get(position);
        final String pdfPath = pdfFile.getAbsolutePath();
        String pdfName = pdfFile.getName();
        final Long pdfSize = pdfFile.getLength();
        final DbHelper dbHelper = DbHelper.getInstance(mContext);

        holder.pdfHeader.setText(pdfName);
        holder.fileSize.setText(android.text.format.Formatter.formatShortFileSize(mContext, pdfSize));
        holder.lastModified.setText(Utils.formatDateToHumanReadable(pdfFile.getLastModified()));

        if (pdfFile.isStarred()) {
            holder.toggleStar.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_action_star_yellow));
        } else {
            holder.toggleStar.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_action_star));
        }

        if (isGridViewEnabled) {
            Picasso.with(mContext).load(pdfFile.getThumbUri()).into(holder.pdfThumbnail);
        }

        holder.toggleStar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (dbHelper.isStared(pdfPath)) {
                    dbHelper.removeStaredPDF(pdfPath);
                    holder.toggleStar.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_action_star));
                } else {
                    dbHelper.addStaredPDF(pdfPath);
                    holder.toggleStar.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_action_star_yellow));
                }

                EventBus.getDefault().post(new DataUpdatedEvent.RecentPDFStaredEvent());
            }
        });

        holder.pdfWrapper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int mPosition = holder.getAdapterPosition();
                historyPdfClicked(mPosition);
                Log.d(TAG, "Pdf " + mPosition + " clicked");
            }
        });

        holder.pdfWrapper.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                showBottomSheet(holder.getAdapterPosition());
                return true;
            }
        });
    }

    @Override
    public int getItemCount() {
        return pdfFiles.size();
    }

    public void filter(List<Pdf> matchedFiles) {
        pdfFiles = matchedFiles;
        notifyDataSetChanged();
    }

    public interface OnHistoryPdfClickListener {
        void onHistoryPdfClicked(Pdf pdfFile);
    }

    private void historyPdfClicked(int position) {
        if (historyPdfClickListener != null) {
            historyPdfClickListener.onHistoryPdfClicked(pdfFiles.get(position));
        }
    }

    public void updateData(List<Pdf> newData) {

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new FileDiffCallback(this.pdfFiles, newData));
        diffResult.dispatchUpdatesTo(this);
        this.pdfFiles = newData;
    }

    public void showBottomSheet(int position) {
        Pdf myFile = pdfFiles.get(position);
        String myFilePath = myFile.getAbsolutePath();
        Bundle args = new Bundle();
        args.putString(PDF_PATH, myFilePath);
        args.putBoolean(FROM_RECENT, true);

        android.support.design.widget.BottomSheetDialogFragment bottomSheetDialogFragment = new BottomSheetDialogFragment();
        bottomSheetDialogFragment.setArguments(args);
        bottomSheetDialogFragment.show(((AppCompatActivity) mContext).getSupportFragmentManager(), bottomSheetDialogFragment.getTag());
    }
}
