package tz.co.wadau.documentscanner.models;

import android.net.Uri;

public class ImagePage {

    private int pageNumber;
    private Uri imageUri;

    public ImagePage() {
    }

    public ImagePage(int pageNumber, Uri imageUri) {
        this.pageNumber = pageNumber;
        this.imageUri = imageUri;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public Uri getImageUri() {
        return imageUri;
    }

    public void setImageUri(Uri imageUri) {
        this.imageUri = imageUri;
    }
}
