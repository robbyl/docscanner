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
import android.view.View;
import android.widget.LinearLayout;

import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

import tz.co.wadau.documentscanner.adapters.StarredPDFAdapter;
import tz.co.wadau.documentscanner.data.DbHelper;
import tz.co.wadau.documentscanner.models.Pdf;
import tz.co.wadau.documentscanner.ui.MaterialSearchView;
import tz.co.wadau.documentscanner.utils.Utils;

import static tz.co.wadau.documentscanner.BrowsePDFActivity.GRID_VIEW_ENABLED;
import static tz.co.wadau.documentscanner.BrowsePDFActivity.GRID_VIEW_NUM_OF_COLUMNS;
import static tz.co.wadau.documentscanner.BrowsePDFActivity.PDF_LOCATION;

public class StarredPDFActivity extends AppCompatActivity
        implements StarredPDFAdapter.OnStaredPdfClickListener,
        MaterialSearchView.OnQueryTextListener {

    private static final String TAG = StarredPDFActivity.class.getSimpleName();
    private RecyclerView recyclerviewStared;
    private LinearLayout emptyState;
    private MaterialSearchView searchView;
    private StarredPDFAdapter starredPDFAdapter;
    private boolean gridViewEnabled;
    private SharedPreferences sharedPreferences;
    private MenuItem menuListViewItem;
    private MenuItem menuGridViewItem;
    private int numberOfColumns;
    private List<Pdf> starredPdfs = new ArrayList<>();
    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_starred_pdf);
        context = this;

        recyclerviewStared = findViewById(R.id.recyclerview_stared);
        emptyState = findViewById(R.id.empty_state_recent);
        searchView = findViewById(R.id.search_view);
        Toolbar toolbar = findViewById(R.id.toolbar_stared);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        searchView.setOnQueryTextListener(this);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        gridViewEnabled = sharedPreferences.getBoolean(GRID_VIEW_ENABLED, false);
        numberOfColumns = sharedPreferences.getInt(GRID_VIEW_NUM_OF_COLUMNS, 2);

//        if (gridViewEnabled)
//            new Utils.BackgroundGenerateThumbnails(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//        loadStarePDF();
        new LoadStarredPdf().execute();
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
        }
        return super.onOptionsItemSelected(item);
    }

    public void loadStarePDF() {

    }

    public class LoadStarredPdf extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            DbHelper dbHelper = DbHelper.getInstance(context);
            starredPdfs = dbHelper.getStarredPdfs();

            starredPDFAdapter = new StarredPDFAdapter(context, starredPdfs);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if (starredPdfs.isEmpty()) {
                emptyState.setVisibility(View.VISIBLE);
            } else {
                emptyState.setVisibility(View.GONE);
            }

            if (gridViewEnabled) {
                setupForGridView(context, recyclerviewStared, numberOfColumns);
            } else {
                setupForListView(context, recyclerviewStared);
            }

            recyclerviewStared.setAdapter(starredPDFAdapter);
        }
    }

    @Subscribe
    public void onPdfRenameEvent(DataUpdatedEvent.PdfRenameEvent event) {
        Log.d(TAG, "PdfRenameEvent from stared");
       new LoadStarredPdf().execute();
    }

    public void openFileInPDFViewer(String pdfPath) {
        //Opening pdf file in pdf viewer
        Intent pdfViewerIntent = new Intent(this, PDFViewerActivity.class);
        pdfViewerIntent.putExtra(PDF_LOCATION, pdfPath);
        Log.d(TAG, "Pdf location " + pdfPath);
        startActivity(pdfViewerIntent);
    }

    public void showListView() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(GRID_VIEW_ENABLED, false);
        editor.apply();
        setupForListView(this, recyclerviewStared);
        starredPDFAdapter = new StarredPDFAdapter(this, starredPdfs);
        recyclerviewStared.setAdapter(starredPDFAdapter);
        menuListViewItem.setVisible(false);
        menuGridViewItem.setVisible(true);
    }

    public void showGridView(int numOfColumns) {
        new Utils.BackgroundGenerateThumbnails(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(GRID_VIEW_ENABLED, true);
        editor.putInt(GRID_VIEW_NUM_OF_COLUMNS, numOfColumns);
        editor.apply();
        setupForGridView(this, recyclerviewStared, numOfColumns);
        starredPDFAdapter = new StarredPDFAdapter(this, starredPdfs);
        recyclerviewStared.setAdapter(starredPDFAdapter);
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

    @Override
    public void onStaredPdfClicked(Pdf pdfFile) {
        openFileInPDFViewer(pdfFile.getAbsolutePath());
    }


    public void searchPDFFiles(String query) {
        List<Pdf> matchedFiles = new ArrayList<>();

        for (Pdf file : starredPdfs) {
            if (file.getName().toLowerCase().contains(query.toLowerCase())) {
                matchedFiles.add(file);
            }

            starredPDFAdapter.filter(matchedFiles);
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        Log.d(TAG, "This is called form onQueryTextSubmit");
        searchPDFFiles(query);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        Log.d(TAG, "This is called form onQueryTextSubmit");
        searchPDFFiles(newText);
        return true;
    }
}
