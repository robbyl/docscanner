package tz.co.wadau.documentscanner;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.List;

import tz.co.wadau.documentscanner.adapters.SelectPDFAdapter;
import tz.co.wadau.documentscanner.data.DbHelper;
import tz.co.wadau.documentscanner.models.Pdf;
import tz.co.wadau.documentscanner.ui.MaterialSearchView;
import tz.co.wadau.documentscanner.utils.Utils;

import static tz.co.wadau.documentscanner.BrowsePDFActivity.GRID_VIEW_ENABLED;
import static tz.co.wadau.documentscanner.BrowsePDFActivity.GRID_VIEW_NUM_OF_COLUMNS;
import static tz.co.wadau.documentscanner.BrowsePDFActivity.PDF_LOCATION;
import static tz.co.wadau.documentscanner.PDFToolsActivity.PDF_PATHS;
import static tz.co.wadau.documentscanner.data.DbHelper.SORT_BY;

public class SelectPDFActivity extends AppCompatActivity
        implements SelectPDFAdapter.OnSelectedPdfClickListener,
        SelectPDFAdapter.OnMultiSelectedPDFListener,
        MaterialSearchView.OnQueryTextListener {

    public final String TAG = SelectPDFAdapter.class.getSimpleName();
    private Boolean isMultiSelect, isDirectory;
    private List<Pdf> mPdfFiles;
    private MaterialSearchView searchView;
    private SelectPDFAdapter selectPDFAdapter;
    private SharedPreferences sharedPreferences;
    private boolean gridViewEnabled;
    private MenuItem menuListViewItem;
    private MenuItem menuGridViewItem;
    private RecyclerView selectRecyclerView;
    private int numberOfColumns;
    private String directoryPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_pdf);

        Toolbar toolbar = findViewById(R.id.toolbar_select_file);
        searchView = findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(this);

        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        isMultiSelect = intent.getBooleanExtra(PDFToolsActivity.MULTI_SELECTION, false);
        isDirectory = intent.getBooleanExtra(PDFToolsActivity.IS_DIRECTORY, false);
        directoryPath = intent.getStringExtra(PDFToolsActivity.DIRECTORY_PATH);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        gridViewEnabled = sharedPreferences.getBoolean(GRID_VIEW_ENABLED, false);
        numberOfColumns = sharedPreferences.getInt(GRID_VIEW_NUM_OF_COLUMNS, 2);

//        if (gridViewEnabled)
//            new Utils.BackgroundGenerateThumbnails(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        if (!isDirectory) {
            loadPDFs();
        } else {
            loadPDFsFromDirectory(directoryPath);
        }
    }

    @Override
    public void onBackPressed() {
        if (searchView.isSearchOpen()) {
            searchView.closeSearch();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_select_pdf, menu);
        menuListViewItem = menu.findItem(R.id.action_list_view);
        menuGridViewItem = menu.findItem(R.id.action_grid_view);
        menuGridViewItem.getSubMenu().clearHeader();

        if (gridViewEnabled) {
            menuListViewItem.setVisible(true);
            menuGridViewItem.setVisible(false);
        } else {
            menuListViewItem.setVisible(false);
            menuGridViewItem.setVisible(true);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_search:
                searchView.openSearch();
                break;
            case R.id.action_list_view:
                showListView();
                break;
            case R.id.action_two_columns:
                showGridView(2);
                break;
            case R.id.action_three_columns:
                showGridView(3);
                break;
            case R.id.action_four_columns:
                showGridView(4);
                break;
            case R.id.action_five_columns:
                showGridView(5);
                break;
            case R.id.action_six_columns:
                showGridView(6);
                break;
            case R.id.action_by_name:
                sortByName();
                break;
            case R.id.action_by_date_modified:
                sortByDateModified();
                break;
            case R.id.action_by_size:
                sortBySize();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onSelectedPdfClicked(Pdf pdfFile) {

        if (!isDirectory) {
            ArrayList<String> pdfPaths = new ArrayList<>();
            pdfPaths.add(pdfFile.getAbsolutePath());

            sendResults(pdfPaths);
        } else {
            Intent openFileIntent = new Intent(this, PDFViewerActivity.class);
            openFileIntent.putExtra(PDF_LOCATION, pdfFile.getAbsolutePath());
            startActivity(openFileIntent);
        }
    }

    @Override
    public void onMultiSelectedPDF(ArrayList<String> selectedPDFPaths) {

        Intent intent = new Intent(this, OrganizeMergePDFActivity.class);
        intent.putStringArrayListExtra(PDF_PATHS, selectedPDFPaths);
        startActivity(intent);
    }

    public void loadPDFs() {

        DbHelper dbHelper = DbHelper.getInstance(this);
        mPdfFiles = dbHelper.getAllPdfs();
        selectPDFAdapter = new SelectPDFAdapter(mPdfFiles, this, isMultiSelect);
        selectRecyclerView = findViewById(R.id.recycler_view_select_file);

        if (gridViewEnabled) {
            setupForGridView(this, selectRecyclerView, numberOfColumns);
        } else {
            setupForListView(this, selectRecyclerView);
        }
        selectRecyclerView.setAdapter(selectPDFAdapter);
    }

    public void loadPDFsFromDirectory(String directory) {
        DbHelper dbHelper = DbHelper.getInstance(this);
        mPdfFiles = dbHelper.getAllPdfFromDirectory(directory);
        selectPDFAdapter = new SelectPDFAdapter(mPdfFiles, this, isMultiSelect);
        selectRecyclerView = findViewById(R.id.recycler_view_select_file);

        if (gridViewEnabled) {
            setupForGridView(this, selectRecyclerView, numberOfColumns);
        } else {
            setupForListView(this, selectRecyclerView);
        }
        selectRecyclerView.setAdapter(selectPDFAdapter);
    }

    public void sendResults(ArrayList<String> paths) {
        Intent intent = new Intent();
        intent.putStringArrayListExtra(PDF_PATHS, paths);
        setResult(RESULT_OK, intent);
        finish();
    }

    public void searchPDFFiles(String query) {
        List<Pdf> matchedFiles = new ArrayList<>();

        for (Pdf file : mPdfFiles) {
            if (file.getName().toLowerCase().contains(query.toLowerCase())) {
                matchedFiles.add(file);
            }

            selectPDFAdapter.filter(matchedFiles);
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        searchPDFFiles(query);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        searchPDFFiles(newText);
        Log.d(TAG, "Searched text " + newText);
        return true;
    }

    public void showListView() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(GRID_VIEW_ENABLED, false);
        editor.apply();
        setupForListView(this, selectRecyclerView);
        selectPDFAdapter = new SelectPDFAdapter(mPdfFiles, this, isMultiSelect);
        selectRecyclerView.setAdapter(selectPDFAdapter);
        menuListViewItem.setVisible(false);
        menuGridViewItem.setVisible(true);
    }

    public void showGridView(int numOfColumns) {
        new Utils.BackgroundGenerateThumbnails(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(GRID_VIEW_ENABLED, true);
        editor.putInt(GRID_VIEW_NUM_OF_COLUMNS, numOfColumns);
        editor.apply();
        setupForGridView(this, selectRecyclerView, numOfColumns);
        selectPDFAdapter = new SelectPDFAdapter(mPdfFiles, this, isMultiSelect);
        selectRecyclerView.setAdapter(selectPDFAdapter);
        menuListViewItem.setVisible(true);
        menuGridViewItem.setVisible(false);
    }

    public void setupForGridView(Context context, RecyclerView recyclerView, int numOfColumns) {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        Float density = metrics.density;
        GridLayoutManager gridLayoutManager = new GridLayoutManager(context, numOfColumns, GridLayoutManager.VERTICAL, false);
        recyclerView.setBackgroundColor(getResources().getColor(R.color.colorLightGray));
        recyclerView.setPadding((int) (4 * density), (int) (4 * density), (int) (6 * density), (int) (5 * density));
        recyclerView.setLayoutManager(gridLayoutManager);
    }

    public void setupForListView(Context context, RecyclerView recyclerView) {
        recyclerView.setBackgroundColor(getResources().getColor(android.R.color.white));
        recyclerView.setPadding(0, 0, 0, 0);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
    }

    public void sortByName() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SORT_BY, "name");
        editor.apply();

        DbHelper dbHelper = DbHelper.getInstance(this);
        mPdfFiles = dbHelper.getAllPdfs();
        selectPDFAdapter.updateData(mPdfFiles);
    }

    public void sortByDateModified() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SORT_BY, "date modified");
        editor.apply();

        DbHelper dbHelper = DbHelper.getInstance(this);
        mPdfFiles = dbHelper.getAllPdfs();
        selectPDFAdapter.updateData(mPdfFiles);
    }

    public void sortBySize() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SORT_BY, "size");
        editor.apply();

        DbHelper dbHelper = DbHelper.getInstance(this);
        mPdfFiles = dbHelper.getAllPdfs();
        selectPDFAdapter.updateData(mPdfFiles);
    }
}
