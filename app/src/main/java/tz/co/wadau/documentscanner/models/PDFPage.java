package tz.co.wadau.documentscanner.models;

import android.net.Uri;

/**
 * Created by admin on 30-Jan-18.
 */

public class PDFPage {

    private int pageNumber;
    private Uri thumbnailUri;

    public PDFPage() {
    }

    public PDFPage(int pageNumber, Uri thumbnailUri) {
        this.pageNumber = pageNumber;
        this.thumbnailUri = thumbnailUri;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public Uri getThumbnailUri() {
        return thumbnailUri;
    }

    public void setThumbnailUri(Uri thumbnailUri) {
        this.thumbnailUri = thumbnailUri;
    }
}
