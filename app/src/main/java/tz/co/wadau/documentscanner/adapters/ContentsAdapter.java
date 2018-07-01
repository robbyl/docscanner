package tz.co.wadau.documentscanner.adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.shockwave.pdfium.PdfDocument;

import java.util.List;

import tz.co.wadau.documentscanner.R;

public class ContentsAdapter extends RecyclerView.Adapter<ContentsAdapter.ContentsViewHolder> {
    private final String TAG = ContentsAdapter.class.getSimpleName();
    private List<PdfDocument.Bookmark> bookmarks;
    private Context mContext;
    private OnContentClickedListener onContentClickedListener;

    public class ContentsViewHolder extends RecyclerView.ViewHolder {
        private TextView contentTitle, contentPage;
        private RelativeLayout contentWrapper;

        public ContentsViewHolder(View view) {
            super(view);

            contentTitle = view.findViewById(R.id.content_title);
            contentPage = view.findViewById(R.id.content_page);
            contentWrapper = view.findViewById(R.id.content_wrapper);
        }
    }

    public ContentsAdapter(Context context, List<PdfDocument.Bookmark> bookmarks) {
        this.bookmarks = bookmarks;
        this.mContext = context;

        if (mContext instanceof OnContentClickedListener) {
            onContentClickedListener = (OnContentClickedListener) mContext;
        } else {
            throw new RuntimeException(mContext.toString() + " must implement OnContentClickedListener");
        }
    }

    @Override
    public ContentsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View bookView = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_contents, parent, false);
        return new ContentsViewHolder(bookView);
    }

    @Override
    public void onBindViewHolder(final ContentsViewHolder holder, final int position) {
        final PdfDocument.Bookmark bookmark = bookmarks.get(position);
        holder.contentTitle.setText(bookmark.getTitle());
        String pageNo = mContext.getString(R.string.page) + " " + (bookmark.getPageIdx() + 1);
        holder.contentPage.setText(pageNo);

        holder.contentWrapper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                contentClicked(bookmark);
            }
        });
    }

    @Override
    public int getItemCount() {
        return bookmarks.size();
    }

    public void contentClicked(PdfDocument.Bookmark bookmark) {
        onContentClickedListener.onContentClicked(bookmark);
    }

    public interface OnContentClickedListener {
        void onContentClicked(PdfDocument.Bookmark bookmark);
    }
}
