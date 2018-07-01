package tz.co.wadau.documentscanner.data;

import android.provider.BaseColumns;

public class DbContract {

    //Pdf files
    public static class RecentPDFEntry implements BaseColumns {

        public static final String TABLE_NAME = "history_pdfs";

        //Columns
        public static final String COLUMN_PATH = "path";
        public static final String COLUMN_LAST_ACCESSED_AT = "last_accessed_at";
    }

    //Stared Pdf files
    public static class StarredPDFEntry implements BaseColumns {

        public static final String TABLE_NAME = "stared_pdfs";

        //Columns
        public static final String COLUMN_PATH = "path";
        public static final String COLUMN_CREATED_AT = "created_at";
    }

    //Last PDF last opened page
    public  static class LastOpenedPageEntry implements BaseColumns {
        public static final String TABLE_NAME = "last_opened_page";

        //Columns
        public static final String COLUMN_PATH = "path";
        public static final String COLUMN_PAGE_NUMBER = "page_number";
    }

    //Bookmarks
    public  static class BookmarkEntry implements BaseColumns {
        public static final String TABLE_NAME = "bookmarks";

        //Columns
        public static final String COLUMN_PATH = "path";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_PAGE_NUMBER = "page_number";
        public static final String COLUMN_CREATED_AT = "created_at";
    }
}
