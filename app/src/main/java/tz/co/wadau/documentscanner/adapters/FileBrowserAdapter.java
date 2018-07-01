package tz.co.wadau.documentscanner.adapters;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.List;

import tz.co.wadau.documentscanner.R;
import tz.co.wadau.documentscanner.data.FileDiffCallback;
import tz.co.wadau.documentscanner.models.Pdf;
import tz.co.wadau.documentscanner.utils.Utils;

import static tz.co.wadau.documentscanner.BrowsePDFActivity.GRID_VIEW_ENABLED;

public class FileBrowserAdapter extends RecyclerView.Adapter<FileBrowserAdapter.PdfViewHolder> {
    private final String TAG = FileBrowserAdapter.class.getSimpleName();
    private List<Pdf> pdfFiles;
    private Context mContext;
    private OnPdfClickListener pdfClickListener;
    private boolean isGridViewEnabled;
    public int folderColor;

    public class PdfViewHolder extends RecyclerView.ViewHolder {
        private TextView pdfHeader;
        private TextView lastModified;
        private TextView fileSize;
        private RelativeLayout pdfWrapper;
        private AppCompatImageView pdfThumbnail, pdfIcon;


        private PdfViewHolder(View view) {
            super(view);

            if (isGridViewEnabled) {
                pdfThumbnail = view.findViewById(R.id.pdf_thumbnail);
                folderColor = Color.parseColor("#FFED8B28");
            } else {
                pdfIcon = view.findViewById(R.id.pdf_icon);
            }
            pdfHeader = view.findViewById(R.id.pdf_header);
            lastModified = view.findViewById(R.id.pdf_last_modified);
            fileSize = view.findViewById(R.id.pdf_file_size);
            pdfWrapper = view.findViewById(R.id.pdf_wrapper);
        }
    }

    public FileBrowserAdapter(Context context, List<Pdf> pdfFiles) {
        this.pdfFiles = pdfFiles;
        this.mContext = context;
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        isGridViewEnabled = sharedPreferences.getBoolean(GRID_VIEW_ENABLED, false);

        if (mContext instanceof OnPdfClickListener) {
            pdfClickListener = (OnPdfClickListener) mContext;
        } else {
            throw new RuntimeException(mContext.toString() + " must implement OnPdfClickListener");
        }
    }

    @Override
    public FileBrowserAdapter.PdfViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View bookView;
        if (isGridViewEnabled) {
            bookView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_browse_pdf_grid, parent, false);
        } else {
            bookView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_browse_pdf, parent, false);
        }
        return new PdfViewHolder(bookView);
    }


    @Override
    public void onBindViewHolder(final FileBrowserAdapter.PdfViewHolder holder, final int position) {
        final Pdf pdfFile = pdfFiles.get(position);
        String pdfName = pdfFile.getName();
        Long pdfSize = pdfFile.getLength();

        holder.pdfHeader.setText(pdfName);

        if (isGridViewEnabled) {

            if (pdfFile.isDirectory()) {
                holder.pdfThumbnail.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_folder_stacked));
                holder.pdfThumbnail.setScaleType(ImageView.ScaleType.FIT_XY);
                String items = pdfFile.getNumItems() + " " + mContext.getString(R.string.items);
                holder.fileSize.setText(items);
            } else {
                Picasso.with(mContext).load(pdfFile.getThumbUri()).into(holder.pdfThumbnail);
                holder.fileSize.setText(android.text.format.Formatter.formatShortFileSize(mContext, pdfSize));
            }
        } else {
            if (pdfFile.isDirectory()) {
                holder.pdfIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_folder_closed));
                String items = pdfFile.getNumItems() + " " + mContext.getString(R.string.items);
                holder.lastModified.setText(items);
            } else {
                holder.pdfIcon.setImageDrawable(mContext.getResources().getDrawable(R.drawable.ic_pdf_icon));
                holder.lastModified.setText(Utils.formatDateToHumanReadable(pdfFile.getLastModified()));
                holder.fileSize.setText(android.text.format.Formatter.formatShortFileSize(mContext, pdfSize));
            }
        }


        holder.pdfWrapper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int mPosition = holder.getAdapterPosition();
                pdfClicked(mPosition);
            }
        });
    }

    @Override
    public int getItemCount() {
        return pdfFiles.size();
    }

    public interface OnPdfClickListener {
        void onPdfClicked(Pdf pdfFile);

    }

    private void pdfClicked(int position) {
        if (pdfClickListener != null) {
            pdfClickListener.onPdfClicked(pdfFiles.get(position));
        }
    }

    public void updateData(List<Pdf> newData) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new FileDiffCallback(this.pdfFiles, newData));
        diffResult.dispatchUpdatesTo(this);
        this.pdfFiles = newData;
    }
}
