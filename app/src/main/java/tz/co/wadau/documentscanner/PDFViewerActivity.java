package tz.co.wadau.documentscanner;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnErrorListener;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.util.FitPolicy;

import java.io.File;

import tz.co.wadau.documentscanner.data.DbHelper;
import tz.co.wadau.documentscanner.utils.Utils;

import static tz.co.wadau.documentscanner.BrowsePDFActivity.PDF_LOCATION;
import static tz.co.wadau.documentscanner.PDFToolsActivity.PDF_PATH;
import static tz.co.wadau.documentscanner.fragments.SettingsFragment.KEY_PREFS_REMEMBER_LAST_PAGE;
import static tz.co.wadau.documentscanner.fragments.SettingsFragment.KEY_PREFS_STAY_AWAKE;
import static tz.co.wadau.documentscanner.fragments.TableContentsFragment.SAVED_STATE;

public class PDFViewerActivity extends AppCompatActivity
        implements ActionMenuView.OnMenuItemClickListener {

    final String TAG = PDFViewerActivity.class.getSimpleName();
    PDFView pdfView;
    ActionBar mActionBar;
    ProgressBar openPdfProgress;
    String filePath, pdfFileLocation;
    private TextView pageNumberTextview;
    private boolean stayAwake, rememberLastPage;
    DbHelper dbHelper;
    Uri uri;
    ActionMenuView actionMenuView;
    private Menu mMenu, topMenu;
    boolean swipeHorizontalEnabled, nightModeEnabled;
    FitPolicy fitPolicy;
    SharedPreferences sharedPreferences;
    final String SWIPE_HORIZONTAL_ENABLED = "prefs_swipe_horizontal_enabled";
    final String NIGHT_MODE_ENABLED = "prefs_night_mode_enabled";
    final static String CONTENTS_PDF_PATH = "tz.co.wadau.documentscanner.CONTENTS_PDF_PATH";
    final static String PAGE_NUMBER = "tz.co.wadau.documentscanner.PAGE_NUMBER";
    final int REQUEST_CODE_PAGE_NO = 7;
    Toolbar toolbar, bottomToolbar;
    Context context;
    View view, divider;
    int colorPrimaryDark, colorPrimaryDarkNight, pageNumber, flags;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_viewer);

        toolbar = findViewById(R.id.toolbar_home);
        bottomToolbar = findViewById(R.id.bottom_toolbar);
        openPdfProgress = findViewById(R.id.progress_bar_open_pdf);
        pageNumberTextview = findViewById(R.id.page_numbers);
        pdfView = findViewById(R.id.pdfView);
        actionMenuView = findViewById(R.id.bottom_action_menu);
        divider = findViewById(R.id.divider);
        actionMenuView.setOnMenuItemClickListener(this);
        context = this;

        setSupportActionBar(toolbar);
        mActionBar = getSupportActionBar();
        mActionBar.setDisplayHomeAsUpEnabled(true);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        stayAwake = sharedPreferences.getBoolean(KEY_PREFS_STAY_AWAKE, true);
        rememberLastPage = sharedPreferences.getBoolean(KEY_PREFS_REMEMBER_LAST_PAGE, true);
        swipeHorizontalEnabled = sharedPreferences.getBoolean(SWIPE_HORIZONTAL_ENABLED, false);
        nightModeEnabled = sharedPreferences.getBoolean(NIGHT_MODE_ENABLED, false);

        view = ((Activity) context).getWindow().getDecorView();
        flags = view.getSystemUiVisibility();
        colorPrimaryDark = context.getResources().getColor(R.color.colorPrimaryDark);
        colorPrimaryDarkNight = context.getResources().getColor(R.color.colorPrimaryDarkNight);

        com.github.barteksc.pdfviewer.util.Constants.THUMBNAIL_RATIO = 0.7f;
        Intent intent = getIntent();
        pdfFileLocation = intent.getStringExtra(PDF_LOCATION);
        uri = intent.getData();
        dbHelper = DbHelper.getInstance(this);
        pdfView.setKeepScreenOn(stayAwake);
        pageNumber = rememberLastPage ? dbHelper.getLastOpenedPage(pdfFileLocation) : 0;
        fitPolicy = Utils.isTablet(this) || swipeHorizontalEnabled ? FitPolicy.HEIGHT : FitPolicy.WIDTH;

        loadPdfFile(pageNumber, swipeHorizontalEnabled, nightModeEnabled, fitPolicy);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_pdf_viewer, menu);
        topMenu = menu;
        mMenu = actionMenuView.getMenu();

        getMenuInflater().inflate(R.menu.activity_pdf_viewer_bottom, mMenu);
        MenuItem menuItemToggleSwipe = mMenu.findItem(R.id.action_toggle_view);
        MenuItem menuItemToggleNightMode = mMenu.findItem(R.id.action_toggle_night_mode);

        setupToggleSwipeIcons(menuItemToggleSwipe, swipeHorizontalEnabled);
        setupNightModeIcons(menuItemToggleNightMode, nightModeEnabled);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PAGE_NO && resultCode == RESULT_OK) {
            int bookmarkPageNumber = data.getIntExtra(PAGE_NUMBER, pdfView.getCurrentPage()) - 1;
            pdfView.jumpTo(bookmarkPageNumber, true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_share:
                Utils.shareFile(this, filePath);
                break;
            case R.id.action_share_as_picture:
                showShareAsPicture(filePath);
                break;
            case R.id.action_print:
                Utils.print(this, filePath);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {

        sharedPreferences.edit().putInt(SAVED_STATE, 0).apply();
        if (rememberLastPage && !TextUtils.isEmpty(pdfFileLocation))
            dbHelper.addLastOpenedPage(filePath, pdfView.getCurrentPage());

        super.onDestroy();
    }

    public void loadPdfFile(int defaultPage, boolean swipeHorizontalEnabled, boolean nightModeEnabled, FitPolicy fitPolicy) {

        if (uri != null) {

            try {
                filePath = uri.getPath();
                File showPdf = new File(filePath);
                mActionBar.setTitle(showPdf.getName());
            } catch (Exception ex) {
                mActionBar.setTitle("View PDF");
                ex.printStackTrace();
            }

            pdfView.fromUri(uri)
//                        .scrollHandle(new ScrollHandle(this))
                    .enableAnnotationRendering(true)
                    .pageFitPolicy(fitPolicy)
                    .spacing(6)
                    .defaultPage(defaultPage)
                    .swipeHorizontal(swipeHorizontalEnabled)
                    .autoSpacing(swipeHorizontalEnabled)
                    .pageFling(swipeHorizontalEnabled)
                    .pageSnap(swipeHorizontalEnabled)
                    .setNightMode(nightModeEnabled)
                    .onPageChange(onPageChangeListener)
                    .onLoad(onLoadCompleteListener)
                    .onError(onErrorListener)
                    .load();

        } else if (!TextUtils.isEmpty(pdfFileLocation)) {

            filePath = pdfFileLocation;
            File showPdf = new File(pdfFileLocation);
            Log.d(TAG, "path from selection " + showPdf.getPath());
            mActionBar.setTitle(showPdf.getName());

            pdfView.fromFile(showPdf)
//                    .scrollHandle(new ScrollHandle(this))
                    .enableAnnotationRendering(true)
                    .pageFitPolicy(fitPolicy)
                    .spacing(6)
                    .defaultPage(defaultPage)
                    .swipeHorizontal(swipeHorizontalEnabled)
                    .autoSpacing(swipeHorizontalEnabled)
                    .pageFling(swipeHorizontalEnabled)
                    .pageSnap(swipeHorizontalEnabled)
                    .setNightMode(nightModeEnabled)
                    .onPageChange(onPageChangeListener)
                    .onLoad(onLoadCompleteListener)
                    .onError(onErrorListener)
                    .load();

            //Store pdf file in recent pdfs
            dbHelper.addRecentPDF(showPdf.getAbsolutePath());
        }
    }

    public String getName(Uri uri) {

        String fileName;

        Cursor returnCursor = getContentResolver().query(uri, null, null, null, null);

        if (returnCursor != null) {
            //        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            int nameIndex = returnCursor.getColumnIndex((OpenableColumns.DISPLAY_NAME));
            returnCursor.moveToFirst();

            fileName = returnCursor.getString(nameIndex);
            String[] parent = returnCursor.getColumnNames();
            returnCursor.close();
        } else {
            fileName = "Unknown";
        }
        return fileName;
    }


    public void showShareAsPicture(String filePath) {
        Intent intent = new Intent(this, ShareAsPictureActivity.class);
        intent.putExtra(PDF_PATH, filePath);
        startActivity(intent);
    }

    public String getPath(Uri uri) {
        String mFilePath = uri.getPath();

        if (!getFileName(uri).contains(".")) {
//            final String id = DocumentsContract.getDocumentId(uri);
            String[] projection = {MediaStore.Files.FileColumns.DATA};
            Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
            if (cursor != null) {
                mFilePath = cursor.getString(cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA));
                cursor.close();
            } else {
                String wholeID = DocumentsContract.getDocumentId(uri);

                // Split at colon, use second item in the array
                String id = wholeID.split(":")[1];

                String[] column = {MediaStore.Files.FileColumns.DATA};

                // where id is equal to
                String sel = MediaStore.Files.FileColumns._ID + "=?";

                Uri externalUri = MediaStore.Files.getContentUri("external");
                Cursor c = getContentResolver().query(externalUri, column, sel, new String[]{id}, null);

                if (c != null && c.moveToFirst()) {
                    mFilePath = c.getString(c.getColumnIndex(column[0]));
                    c.close();
                } else {
                    mFilePath = uri.toString();
                }
            }
        }

        if (mFilePath.contains(":/"))
            mFilePath = mFilePath.split(":")[1];

        return mFilePath;
    }

    public String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                    cursor.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    OnPageChangeListener onPageChangeListener = new OnPageChangeListener() {
        @Override
        public void onPageChanged(int page, int pageCount) {
            String currPageNumber = (page + 1) + " / " + pageCount;
            pageNumberTextview.setText(currPageNumber);
        }
    };

    OnLoadCompleteListener onLoadCompleteListener = new OnLoadCompleteListener() {
        @Override
        public void loadComplete(int nbPages) {
            openPdfProgress.setVisibility(View.GONE);
            pageNumberTextview.setVisibility(View.VISIBLE);
        }
    };

    OnErrorListener onErrorListener = new OnErrorListener() {
        @Override
        public void onError(Throwable t) {
            Toast.makeText(PDFViewerActivity.this, t.getMessage(), Toast.LENGTH_LONG).show();
            openPdfProgress.setVisibility(View.GONE);
        }
    };

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_toggle_view:
                togglePDFView(item);
                break;
            case R.id.action_contents:
                showContents(filePath);
                break;
            case R.id.action_bookmark:
                addPageBookmark(this, filePath, pdfView.getCurrentPage() + 1);
                break;
            case R.id.action_toggle_night_mode:
                toggleNightMode(item);
                break;
        }
        return false;
    }

    public void togglePDFView(MenuItem menuItem) {
        swipeHorizontalEnabled = sharedPreferences.getBoolean(SWIPE_HORIZONTAL_ENABLED, false);
        boolean mNightModeEnabled = sharedPreferences.getBoolean(NIGHT_MODE_ENABLED, false);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        setupToggleSwipeIcons(menuItem, !swipeHorizontalEnabled);

        if (swipeHorizontalEnabled) {
            loadPdfFile(pdfView.getCurrentPage(), !swipeHorizontalEnabled, mNightModeEnabled, FitPolicy.WIDTH);
            editor.putBoolean(SWIPE_HORIZONTAL_ENABLED, !swipeHorizontalEnabled).apply();
        } else {
            loadPdfFile(pdfView.getCurrentPage(), !swipeHorizontalEnabled, mNightModeEnabled, FitPolicy.HEIGHT);
            editor.putBoolean(SWIPE_HORIZONTAL_ENABLED, !swipeHorizontalEnabled).apply();
        }
    }

    public void addPageBookmark(final Context context, final String pdfPath, final int pageNumber) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        final EditText titleEditText = new EditText(context);
        titleEditText.setHint(R.string.enter_title);
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        float dpi = displayMetrics.density;

        builder.setTitle(R.string.add_bookmark)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DbHelper dbHelper = DbHelper.getInstance(context);
                        String bookmarkTitle = TextUtils.isEmpty(titleEditText.getText().toString()) ? getString(R.string.bookmark) : titleEditText.getText().toString();
                        dbHelper.addBookmark(pdfPath, bookmarkTitle, pageNumber);
                        Toast.makeText(context, getString(R.string.page) + " " + pageNumber + " " + getString(R.string.bookmark_added), Toast.LENGTH_SHORT).show();

                    }
                }).setNegativeButton(R.string.cancel, null);

        AlertDialog alertDialog = builder.create();
        alertDialog.setView(titleEditText, (int) (24 * dpi), (int) (8 * dpi), (int) (24 * dpi), (int) (5 * dpi));
        alertDialog.show();
    }

    public void showContents(String pdfPath) {
        Intent intent = new Intent(this, ContentsActivity.class);
        intent.putExtra(CONTENTS_PDF_PATH, pdfPath);
        startActivityForResult(intent, REQUEST_CODE_PAGE_NO);
    }

    public void toggleNightMode(MenuItem menuItem) {
        nightModeEnabled = sharedPreferences.getBoolean(NIGHT_MODE_ENABLED, false);
        setupNightModeIcons(menuItem, !nightModeEnabled);

        pdfView.setNightMode(!nightModeEnabled);
        pdfView.invalidate();
        sharedPreferences.edit().putBoolean(NIGHT_MODE_ENABLED, !nightModeEnabled).apply();

        setupToggleSwipeIcons(mMenu.findItem(R.id.action_toggle_view), sharedPreferences.getBoolean(SWIPE_HORIZONTAL_ENABLED, false));
    }

    public void setupToggleSwipeIcons(MenuItem menuItem, boolean swipeHorizontalEnabled) {
        boolean mNightModeEnabled = sharedPreferences.getBoolean(NIGHT_MODE_ENABLED, false);
        if (swipeHorizontalEnabled) {
            if (mNightModeEnabled) {
                menuItem.setIcon(R.drawable.ic_action_swipe_vertical_night);
            } else {
                menuItem.setIcon(R.drawable.ic_action_swipe_vertical);
                menuItem.setTitle(R.string.swipe_vertical);
            }

        } else {

            if (mNightModeEnabled) {
                menuItem.setIcon(R.drawable.ic_action_swipe_horizontal_night);
            } else {
                menuItem.setIcon(R.drawable.ic_action_swipe_horizontal);
                menuItem.setTitle(R.string.swipe_horizontal);
            }
        }
    }

    public void setupNightModeIcons(MenuItem menuItem, boolean nightModeEnabled) {
        Resources resources = context.getResources();
        if (nightModeEnabled) {
            menuItem.setIcon(R.drawable.ic_action_light_mode_night);
            menuItem.setTitle(R.string.light_mode);

            toolbar.setBackgroundColor(resources.getColor(R.color.colorPrimaryNight));
            toolbar.setTitleTextColor(resources.getColor(R.color.colorTitleTextNight));
            toolbar.setNavigationIcon(R.drawable.ic_action_back_night);
            bottomToolbar.setBackgroundColor(resources.getColor(R.color.colorPrimaryNight));
            pdfView.setBackgroundColor(resources.getColor(R.color.colorPrimaryDarkNight));
            divider.setBackgroundColor(resources.getColor(R.color.colorPrimaryDarkNight));

            topMenu.findItem(R.id.action_share).setIcon(R.drawable.ic_action_share_night);
            topMenu.findItem(R.id.action_share_as_picture).setIcon(R.drawable.ic_action_share_as_picture_night);
            topMenu.findItem(R.id.action_print).setIcon(R.drawable.ic_action_print_night);

            mMenu.findItem(R.id.action_bookmark).setIcon(R.drawable.ic_action_bookmark_night);
            mMenu.findItem(R.id.action_contents).setIcon(R.drawable.ic_action_contents_night);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                view.setSystemUiVisibility(flags);
                ((Activity) context).getWindow().setStatusBarColor(colorPrimaryDarkNight);
            }

        } else {
            menuItem.setIcon(R.drawable.ic_action_night_mode);
            menuItem.setTitle(R.string.night_mode);

            toolbar.setBackgroundColor(Color.WHITE);
            toolbar.setTitleTextColor(context.getResources().getColor(R.color.colorTitleTextLight));
            toolbar.setNavigationIcon(R.drawable.ic_action_back_light);
            bottomToolbar.setBackgroundColor(Color.WHITE);
            pdfView.setBackgroundColor(context.getResources().getColor(R.color.colorPDFViewBg));
            divider.setBackgroundColor(resources.getColor(R.color.colorDividerLight));

            topMenu.findItem(R.id.action_share).setIcon(R.drawable.ic_action_share);
            topMenu.findItem(R.id.action_share_as_picture).setIcon(R.drawable.ic_action_share_as_picture);
            topMenu.findItem(R.id.action_print).setIcon(R.drawable.ic_action_print);

            mMenu.findItem(R.id.action_bookmark).setIcon(R.drawable.ic_action_bookmark);
            mMenu.findItem(R.id.action_contents).setIcon(R.drawable.ic_action_contents);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                view.setSystemUiVisibility(flags);

                ((Activity) context).getWindow().setStatusBarColor(colorPrimaryDark);
            }
        }
    }
}
