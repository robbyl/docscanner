package tz.co.wadau.documentscanner.data;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import tz.co.wadau.documentscanner.DataUpdatedEvent;
import tz.co.wadau.documentscanner.R;
import tz.co.wadau.documentscanner.data.DbContract.BookmarkEntry;
import tz.co.wadau.documentscanner.data.DbContract.LastOpenedPageEntry;
import tz.co.wadau.documentscanner.data.DbContract.RecentPDFEntry;
import tz.co.wadau.documentscanner.data.DbContract.StarredPDFEntry;
import tz.co.wadau.documentscanner.models.Bookmark;
import tz.co.wadau.documentscanner.models.Pdf;
import tz.co.wadau.documentscanner.utils.Utils;


public class DbHelper extends SQLiteOpenHelper {

    private final String TAG = DbHelper.class.getSimpleName();
    private static final String DATABASE_NAME = "documentscanner.db";
    private static final int DATABASE_VERSION = 1;
    public Context context;
    public static final String SORT_BY = "prefs_sort_by";
    private String THUMBNAILS_DIR;
    private static DbHelper sInstance;
    private SQLiteDatabase mDatabase;
    private int mOpenCounter;


    private final String SQL_CREATE_HISTORY_PDFS_TABLE = "CREATE TABLE IF NOT EXISTS " + RecentPDFEntry.TABLE_NAME
            + " ( " + RecentPDFEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + RecentPDFEntry.COLUMN_PATH + " TEXT UNIQUE, "
            + RecentPDFEntry.COLUMN_LAST_ACCESSED_AT + " DATETIME DEFAULT (DATETIME('now','localtime')))";

    private final String SQL_CREATE_STARED_PDFS_TABLE = "CREATE TABLE IF NOT EXISTS " + StarredPDFEntry.TABLE_NAME
            + " ( " + StarredPDFEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + StarredPDFEntry.COLUMN_PATH + " TEXT UNIQUE, "
            + StarredPDFEntry.COLUMN_CREATED_AT + " DATETIME DEFAULT (DATETIME('now','localtime')))";

    private final String SQL_CREATE_LAST_OPENED_PAGE = "CREATE TABLE IF NOT EXISTS " + LastOpenedPageEntry.TABLE_NAME
            + " ( " + LastOpenedPageEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + LastOpenedPageEntry.COLUMN_PATH + " TEXT UNIQUE, "
            + LastOpenedPageEntry.COLUMN_PAGE_NUMBER + " INTEGER)";

    private final String SQL_CREATE_BOOKMARK = "CREATE TABLE IF NOT EXISTS " + BookmarkEntry.TABLE_NAME
            + " ( " + BookmarkEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
            + BookmarkEntry.COLUMN_TITLE + " TEXT, "
            + BookmarkEntry.COLUMN_PATH + " TEXT, "
            + BookmarkEntry.COLUMN_PAGE_NUMBER + " INTEGER UNIQUE, "
            + BookmarkEntry.COLUMN_CREATED_AT + " DATETIME DEFAULT (DATETIME('now','localtime')))";


    public DbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
        THUMBNAILS_DIR = context.getCacheDir() + "/Thumbnails/";
    }

    public static synchronized DbHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DbHelper(context.getApplicationContext());
        }

        return sInstance;
    }

    public synchronized SQLiteDatabase getReadableDb() {
        mOpenCounter++;
        if (mOpenCounter == 1) {
            // Opening new database
            mDatabase = getWritableDatabase();
        }
        return mDatabase;
    }

    public synchronized void closeDb() {
        mOpenCounter--;
        if (mOpenCounter == 0) {
            // Closing database
            mDatabase.close();
        }
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(SQL_CREATE_HISTORY_PDFS_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_STARED_PDFS_TABLE);
        sqLiteDatabase.execSQL(SQL_CREATE_LAST_OPENED_PAGE);
        sqLiteDatabase.execSQL(SQL_CREATE_BOOKMARK);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
//        switch (oldVersion) {
//            case 1:
//                sqLiteDatabase.execSQL(SQL_CREATE_LAST_OPENED_PAGE);
//                sqLiteDatabase.execSQL(SQL_CREATE_BOOKMARK);
//        }
    }


    public List<Pdf> getAllPdfFromDirectory(String dir) {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String sortBy = sharedPreferences.getString(SORT_BY, "name");

        List<Pdf> allPdfs = new ArrayList<>();

        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = MediaStore.Files.getContentUri("external");
        String sortOrder;

        switch (sortBy) {
            case "date modified":
                sortOrder = MediaStore.Files.FileColumns.DATE_MODIFIED + "  COLLATE NOCASE ASC";
                break;
            case "size":
                sortOrder = MediaStore.Files.FileColumns.SIZE + "  COLLATE NOCASE ASC";
                break;
            case "name":
            default:
                sortOrder = MediaStore.Files.FileColumns.TITLE + "  COLLATE NOCASE ASC";
                break;
        }

        String[] projection = new String[]{MediaStore.Files.FileColumns.DATA};
        String selectionMimeType = MediaStore.Files.FileColumns.MIME_TYPE + "=?" +
                " AND " + MediaStore.Files.FileColumns.DATA + " LIKE ?";
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension("pdf");
        String[] selectionArgsPdf = new String[]{mimeType, "%" + dir + "%"};

        Cursor cursor = contentResolver.query(uri, projection, selectionMimeType, selectionArgsPdf, sortOrder);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String pdfFilePath = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA));
                File pdfFile = new File(pdfFilePath);

                if (pdfFile.length() != 0) {
                    String mThumbPath = THUMBNAILS_DIR + Utils.removeExtension(pdfFile.getName()) + ".jpg";
                    Uri thumbnailUri = Utils.getImageUriFromPath(mThumbPath);

                    Pdf mPdf = new Pdf();
                    mPdf.setName(pdfFile.getName());
                    mPdf.setAbsolutePath(pdfFile.getAbsolutePath());
                    mPdf.setPdfUri(Uri.fromFile(pdfFile));
                    mPdf.setLength(pdfFile.length());
                    mPdf.setLastModified(pdfFile.lastModified());
                    mPdf.setThumbUri(thumbnailUri);
                    mPdf.setStarred(isStared(pdfFile.getAbsolutePath()));

                    allPdfs.add(mPdf);
                }
            } while (cursor.moveToNext());
            cursor.close();
        }

        Log.d(TAG, "no of files in db " + allPdfs.size());
        return allPdfs;
    }

    public List<Pdf> getAllPdfs() {

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String sortBy = sharedPreferences.getString(SORT_BY, "name");

        List<Pdf> allPdfs = new ArrayList<>();

        ContentResolver contentResolver = context.getContentResolver();
        Uri uri = MediaStore.Files.getContentUri("external");
        String sortOrder;

        switch (sortBy) {
            case "date modified":
                sortOrder = MediaStore.Files.FileColumns.DATE_MODIFIED + "  COLLATE NOCASE ASC";
                break;
            case "size":
                sortOrder = MediaStore.Files.FileColumns.SIZE + "  COLLATE NOCASE ASC";
                break;
            case "name":
            default:
                sortOrder = MediaStore.Files.FileColumns.TITLE + "  COLLATE NOCASE ASC";
                break;
        }

        String[] projection = new String[]{MediaStore.Files.FileColumns.DATA};
        String selectionMimeType = MediaStore.Files.FileColumns.MIME_TYPE + "=?";
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension("pdf");
        String[] selectionArgsPdf = new String[]{mimeType};

        try {
            Cursor cursor = contentResolver.query(uri, projection, selectionMimeType, selectionArgsPdf, sortOrder);

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String pdfFilePath = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA));
                    File pdfFile = new File(pdfFilePath);

                    if (pdfFile.length() != 0) {
                        String mThumbPath = THUMBNAILS_DIR + Utils.removeExtension(pdfFile.getName()) + ".jpg";
                        Uri thumbnailUri = Utils.getImageUriFromPath(mThumbPath);

                        Pdf mPdf = new Pdf();
                        mPdf.setName(pdfFile.getName());
                        mPdf.setAbsolutePath(pdfFile.getAbsolutePath());
                        mPdf.setPdfUri(Uri.fromFile(pdfFile));
                        mPdf.setLength(pdfFile.length());
                        mPdf.setLastModified(pdfFile.lastModified());
                        mPdf.setThumbUri(thumbnailUri);
                        mPdf.setStarred(isStared(pdfFile.getAbsolutePath()));

                        allPdfs.add(mPdf);
                    }
                } while (cursor.moveToNext());
                cursor.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return allPdfs;
    }

    public void addRecentPDF(String pdfPath) {

        SQLiteDatabase db = getReadableDb();

        ContentValues values = new ContentValues();
        values.put(RecentPDFEntry.COLUMN_PATH, pdfPath);
        db.replace(RecentPDFEntry.TABLE_NAME, null, values);
        closeDb();
    }

    public List<Pdf> getRecentPDFs() {
        List<Pdf> myPdfs = new ArrayList<>();
        SQLiteDatabase db = getReadableDb();

        final String SQL_GET_ALL_HISTORY_PDFS = "SELECT * FROM " + RecentPDFEntry.TABLE_NAME
                + " ORDER BY " + RecentPDFEntry.COLUMN_LAST_ACCESSED_AT + " DESC";

        Cursor cursor = db.rawQuery(SQL_GET_ALL_HISTORY_PDFS, null);

        if (cursor.moveToFirst()) {
            do {
                String pdfPath = cursor.getString(cursor.getColumnIndex(RecentPDFEntry.COLUMN_PATH));
                File pdfFile = new File(pdfPath); //Get pdf file from path.

                if (pdfFile.exists()) {
                    String mThumbPath = THUMBNAILS_DIR + Utils.removeExtension(pdfFile.getName()) + ".jpg";
                    Uri thumbnailUri = Utils.getImageUriFromPath(mThumbPath);

                    Pdf mPdf = new Pdf();
                    mPdf.setName(pdfFile.getName());
                    mPdf.setAbsolutePath(pdfFile.getAbsolutePath());
                    mPdf.setPdfUri(Uri.fromFile(pdfFile));
                    mPdf.setLength(pdfFile.length());
                    mPdf.setLastModified(pdfFile.lastModified());
                    mPdf.setThumbUri(thumbnailUri);
                    mPdf.setStarred(isStared(db, pdfFile.getAbsolutePath()));

                    myPdfs.add(mPdf);
                } else {
                    //Delete history for the pdf that is no longer available
                    deleteRecentPDF(pdfPath);
                }

            } while (cursor.moveToNext());
        }

        cursor.close();
        closeDb();
        return myPdfs;
    }

    public void deleteRecentPDF(String path) {
        SQLiteDatabase db = getReadableDb();
        db.delete(RecentPDFEntry.TABLE_NAME, RecentPDFEntry.COLUMN_PATH + " =?", new String[]{path});
        closeDb();

        // Notify event bus listeners
        EventBus.getDefault().post(new DataUpdatedEvent.RecentPdfDeleteEvent());
    }

    public void updateHistory(String oldPath, String newPath) {
        try {
            SQLiteDatabase db = getReadableDb();
            ContentValues contentValues = new ContentValues();
            contentValues.put(RecentPDFEntry.COLUMN_PATH, newPath);
            db.update(RecentPDFEntry.TABLE_NAME, contentValues, RecentPDFEntry.COLUMN_PATH + "=?", new String[]{oldPath});
            closeDb();
        } catch (Exception e) {
            Toast.makeText(context, R.string.failed, Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    public void updateStarred(String oldPath, String newPath) {
        SQLiteDatabase db = getReadableDb();
        ContentValues contentValues = new ContentValues();
        contentValues.put(RecentPDFEntry.COLUMN_PATH, newPath);
        db.update(StarredPDFEntry.TABLE_NAME, contentValues, RecentPDFEntry.COLUMN_PATH + "=?", new String[]{oldPath});
        closeDb();
    }

    public void clearRecentPDFs() {
        SQLiteDatabase db = getReadableDb();
        db.delete(RecentPDFEntry.TABLE_NAME, null, null);
        closeDb();

        // Notify event bus listeners
        EventBus.getDefault().post(new DataUpdatedEvent.RecentPdfClearEvent());
    }

    public List<Pdf> getStarredPdfs() {
        List<Pdf> myPdfs = new ArrayList<>();
        SQLiteDatabase db = getReadableDb();

        final String SQL_GET_ALL_STARED_PDFS = "SELECT * FROM " + StarredPDFEntry.TABLE_NAME
                + " ORDER BY " + StarredPDFEntry.COLUMN_CREATED_AT + " DESC";

        Cursor cursor = db.rawQuery(SQL_GET_ALL_STARED_PDFS, null);

        if (cursor.moveToFirst()) {
            do {
                String pdfPath = cursor.getString(cursor.getColumnIndex(StarredPDFEntry.COLUMN_PATH));
                File pdfFile = new File(pdfPath); //Get pdf file from path.

                if (pdfFile.exists()) {
                    String mThumbPath = THUMBNAILS_DIR + Utils.removeExtension(pdfFile.getName()) + ".jpg";
                    Uri thumbnailUri = Utils.getImageUriFromPath(mThumbPath);

                    Pdf mPdf = new Pdf();
                    mPdf.setName(pdfFile.getName());
                    mPdf.setAbsolutePath(pdfFile.getAbsolutePath());
                    mPdf.setPdfUri(Uri.fromFile(pdfFile));
                    mPdf.setLength(pdfFile.length());
                    mPdf.setLastModified(pdfFile.lastModified());
                    mPdf.setThumbUri(thumbnailUri);
                    mPdf.setStarred(true);

                    myPdfs.add(mPdf);
                } else {
                    //Delete stared for the pdf that is no longer available
                    removeStaredPDF(pdfPath);
                }

            } while (cursor.moveToNext());
        }

        cursor.close();
        closeDb();
        return myPdfs;
    }

    public void addStaredPDF(String pdfPath) {
        SQLiteDatabase db = getReadableDb();

        ContentValues ContentValues = new ContentValues();
        ContentValues.put(StarredPDFEntry.COLUMN_PATH, pdfPath);

        db.replace(StarredPDFEntry.TABLE_NAME, null, ContentValues);
        closeDb();
    }

    public void removeStaredPDF(String pdfPath) {
        SQLiteDatabase db = getReadableDb();

        db.delete(StarredPDFEntry.TABLE_NAME, StarredPDFEntry.COLUMN_PATH + " =?", new String[]{pdfPath});
        closeDb();
    }

    public void updateStaredPDF(String oldPath, String newPath) {
        try {
            SQLiteDatabase db = getReadableDb();

            ContentValues contentValues = new ContentValues();
            contentValues.put(StarredPDFEntry.COLUMN_PATH, newPath);

            db.update(StarredPDFEntry.TABLE_NAME, contentValues, StarredPDFEntry.COLUMN_PATH + " =?", new String[]{oldPath});
            closeDb();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isStared(String pdfPath) {
        SQLiteDatabase db = getReadableDb();
        Cursor cursor = db.query(StarredPDFEntry.TABLE_NAME, new String[]{StarredPDFEntry.COLUMN_PATH},
                StarredPDFEntry.COLUMN_PATH + " =?", new String[]{pdfPath}, null, null, null);

        Boolean moved = cursor.moveToFirst();
        cursor.close();
        closeDb();
        return moved;
    }

    public boolean isStared(SQLiteDatabase db, String pdfPath) {
        Cursor cursor = db.query(StarredPDFEntry.TABLE_NAME, new String[]{StarredPDFEntry.COLUMN_PATH},
                StarredPDFEntry.COLUMN_PATH + " =?", new String[]{pdfPath}, null, null, null);

        Boolean moved = cursor.moveToFirst();
        cursor.close();
        return moved;
    }


    public List<Uri> getAllImages(String imageDirectory) {
        List<Uri> imageUris = new ArrayList<>();
        Uri uri = MediaStore.Files.getContentUri("external");
        Cursor cursor = context.getContentResolver().query(uri, null, MediaStore.Images.Media.DATA + " LIKE ? " +
                        "AND " + MediaStore.Images.Media.MIME_TYPE + " LIKE ? ",
                new String[]{"%" + imageDirectory + "%", "%image/%"}, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                String imagePath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                Log.d(TAG, imagePath);
                File imageFile = new File(imagePath);
                imageUris.add(Uri.fromFile(imageFile));
            } while (cursor.moveToNext());

            cursor.close();
        }
        return imageUris;
    }

    public int getLastOpenedPage(String pdfPath) {
        SQLiteDatabase db = getReadableDb();
        int pageNumber = 0;

        try {
            Cursor cursor = db.query(LastOpenedPageEntry.TABLE_NAME, new String[]{LastOpenedPageEntry.COLUMN_PAGE_NUMBER},
                    LastOpenedPageEntry.COLUMN_PATH + " = ? ", new String[]{pdfPath}, null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                pageNumber = cursor.getInt(cursor.getColumnIndex(LastOpenedPageEntry.COLUMN_PAGE_NUMBER));
                cursor.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        closeDb();
        return pageNumber;
    }

    public void addLastOpenedPage(String pagePath, int pageNumber) {
        try {
            SQLiteDatabase db = getReadableDb();

            ContentValues values = new ContentValues();
            values.put(LastOpenedPageEntry.COLUMN_PATH, pagePath);
            values.put(LastOpenedPageEntry.COLUMN_PAGE_NUMBER, pageNumber);
            db.replace(LastOpenedPageEntry.TABLE_NAME, null, values);
            closeDb();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateLastOpenedPagePath(String oldPath, String newPath) {
        try {
            SQLiteDatabase db = getReadableDb();

            ContentValues contentValues = new ContentValues();
            contentValues.put(LastOpenedPageEntry.COLUMN_PATH, newPath);

            db.update(LastOpenedPageEntry.TABLE_NAME, contentValues, LastOpenedPageEntry.COLUMN_PATH + " =?", new String[]{oldPath});
            closeDb();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addBookmark(String pagePath, String title, int pageNumber) {
        try {
            SQLiteDatabase db = getReadableDb();

            ContentValues values = new ContentValues();
            values.put(BookmarkEntry.COLUMN_PATH, pagePath);
            values.put(BookmarkEntry.COLUMN_TITLE, title);
            values.put(BookmarkEntry.COLUMN_PAGE_NUMBER, pageNumber);
            db.replace(BookmarkEntry.TABLE_NAME, null, values);
            closeDb();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Bookmark> getBookmarks(String pdfPath) {
        List<Bookmark> bookmarks = new ArrayList<>();

        try {
            SQLiteDatabase db = getReadableDb();

            Cursor cursor = db.query(BookmarkEntry.TABLE_NAME, new String[]{BookmarkEntry.COLUMN_TITLE, BookmarkEntry.COLUMN_PATH, BookmarkEntry.COLUMN_PAGE_NUMBER},
                    BookmarkEntry.COLUMN_PATH + " = ? ", new String[]{pdfPath}, null, null, BookmarkEntry.COLUMN_CREATED_AT + " DESC");

            if (cursor != null && cursor.moveToFirst()) {
                do {
                    Bookmark bookmark = new Bookmark();
                    bookmark.setTitle(cursor.getString(cursor.getColumnIndex(BookmarkEntry.COLUMN_TITLE)));
                    bookmark.setPageNumber(cursor.getInt(cursor.getColumnIndex(BookmarkEntry.COLUMN_PAGE_NUMBER)));
                    bookmark.setPath(cursor.getString(cursor.getColumnIndex(BookmarkEntry.COLUMN_PATH)));
                    bookmarks.add(bookmark);
                } while (cursor.moveToNext());

                cursor.close();
                closeDb();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bookmarks;
    }

    public void updateBookmarkPath(String oldPath, String newPath) {
        try {
            SQLiteDatabase db = getReadableDb();

            ContentValues contentValues = new ContentValues();
            contentValues.put(BookmarkEntry.COLUMN_PATH, newPath);

            db.update(BookmarkEntry.TABLE_NAME, contentValues, BookmarkEntry.COLUMN_PATH + " =?", new String[]{oldPath});
            closeDb();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteBookmarks(List<Bookmark> bookmarks) {
        try {
            SQLiteDatabase db = getReadableDb();
            int bookmarksSize = bookmarks.size();
            db.beginTransaction();

            for (int z = 0; z < bookmarksSize; z++) {
                db.delete(BookmarkEntry.TABLE_NAME, BookmarkEntry.COLUMN_PATH + " = ? AND "
                                + BookmarkEntry.COLUMN_PAGE_NUMBER + " = ? ",
                        new String[]{bookmarks.get(z).getPath(), String.valueOf(bookmarks.get(z).getPageNumber())});
            }

            db.setTransactionSuccessful();
            db.endTransaction();
            closeDb();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
