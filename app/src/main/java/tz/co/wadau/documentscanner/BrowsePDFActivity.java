package tz.co.wadau.documentscanner;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;

import java.io.File;

import tz.co.wadau.documentscanner.adapters.BrowsePdfPagerAdapter;
import tz.co.wadau.documentscanner.adapters.DevicePdfsAdapter;
import tz.co.wadau.documentscanner.adapters.RecentPdfsAdapter;
import tz.co.wadau.documentscanner.data.DbHelper;
import tz.co.wadau.documentscanner.fragments.RecentPdfFragment;
import tz.co.wadau.documentscanner.models.Pdf;
import tz.co.wadau.documentscanner.ui.MaterialSearchView;
import tz.co.wadau.documentscanner.utils.Utils;

import static tz.co.wadau.documentscanner.data.DbHelper.SORT_BY;
import static tz.co.wadau.documentscanner.utils.Utils.launchMarket;
import static tz.co.wadau.documentscanner.utils.Utils.setUpRateUsDialog;

public class BrowsePDFActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        RecentPdfFragment.OnRecentPdfClickListener,
        DevicePdfsAdapter.OnPdfClickListener,
        RecentPdfsAdapter.OnHistoryPdfClickListener,
        MaterialSearchView.OnQueryTextListener {

    private final String TAG = BrowsePDFActivity.class.getSimpleName();
    public static final String PDF_LOCATION = "tz.co.wadau.documentscanner.PDF_LOCATION";
    static final int PICK_PDF_REQUEST = 1;
    public static String GRID_VIEW_ENABLED = "prefs_grid_view_enabled";
    public static String GRID_VIEW_NUM_OF_COLUMNS = "prefs_grid_view_num_of_columns";
    private ViewPager mViewPager;
    private DrawerLayout drawer;
    private MaterialSearchView searchView;
    public BrowsePdfPagerAdapter browsePdfPagerAdapter;
    private TabLayout mTabLayout;
    private SharedPreferences sharedPreferences;
    private boolean gridViewEnabled;
    MenuItem menuListViewItem;
    MenuItem menuGridViewItem;
    static int REQUEST_CODE_SCAN = 11;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_browse_pdf);

        Toolbar mToolbar = findViewById(R.id.toolbar_browse_pdf);
        mTabLayout = findViewById(R.id.tab_layout_browse_pdf);
        drawer = findViewById(R.id.drawer_layout);
        mViewPager = findViewById(R.id.view_pager_browse_pdf);
        searchView = findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(this);

        setSupportActionBar(mToolbar);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        gridViewEnabled = sharedPreferences.getBoolean(GRID_VIEW_ENABLED, false);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        searchView = findViewById(R.id.search_view);
        searchView.setOnQueryTextListener(this);

        if (gridViewEnabled)
            new Utils.BackgroundGenerateThumbnails(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        mTabLayout.addTab(mTabLayout.newTab().setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_recent_tab, null)));
        mTabLayout.addTab(mTabLayout.newTab().setIcon(ResourcesCompat.getDrawable(getResources(), R.drawable.ic_phone_tab, null)));

        browsePdfPagerAdapter = new BrowsePdfPagerAdapter(getSupportFragmentManager());
        mViewPager.setAdapter(browsePdfPagerAdapter);
        mViewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mTabLayout));

        mTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mViewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {

            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {

            }
        });

        setUpRateUsDialog(this);
    }

    @Override
    public void onBackPressed() {
        if (searchView.isSearchOpen()) {
            searchView.closeSearch();
        } else if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(final MenuItem item) {
        drawer.closeDrawer(GravityCompat.START);

        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                Context mContext = getApplicationContext();

                switch (item.getItemId()) {

                    case R.id.nav_stared:
                        startActivity(new Intent(mContext, StarredPDFActivity.class));
                        break;
                    case R.id.nav_scan:
                        Intent intent = new Intent(mContext, ScanActivity.class);
                        intent.putExtra(ScanActivity.EXTRA_BRAND_IMG_RES, R.drawable.ic_crop_white_24dp); // Set image for title icon - optional
                        intent.putExtra(ScanActivity.EXTRA_TITLE, "Crop Document"); // Set title in action Bar - optional
                        intent.putExtra(ScanActivity.EXTRA_ACTION_BAR_COLOR, R.color.blue); // Set title color - optional
                        intent.putExtra(ScanActivity.EXTRA_LANGUAGE, "en"); // Set language - optional
                        startActivityForResult(intent, REQUEST_CODE_SCAN);
                        break;
                    case R.id.nav_tools:
                        startActivity(new Intent(mContext, PDFToolsActivity.class));
                        break;
                    case R.id.nav_rate:
                        launchMarket(mContext);
                        break;
                    case R.id.nav_share:
                        Utils.startShareActivity(mContext);
                        break;
                    case R.id.nav_more_apps:
                        showDevPage();
                        break;
                    case R.id.nav_remove_ads:
                        Utils.openProVersionPlayStore(mContext);
                        break;
                    case R.id.nav_about:
                        startActivity(new Intent(mContext, AboutActivity.class));
                        break;
                    case R.id.nav_settings:
                        startActivity(new Intent(mContext, SettingsActivity.class));
                        break;
                }
            }
        }, 200);


        return true;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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
        return true;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == PICK_PDF_REQUEST && resultCode == RESULT_OK) {
            Uri uri = intent.getData();
            String filePath;

            if (uri != null) {
                filePath = uri.getPath();

                if (filePath.contains(":")) {
                    filePath = filePath.split(":")[1];
                }

                openFileInPdfView(new File(filePath).getAbsolutePath());
            } else {
                Log.d(TAG, "Uri is null");
            }
        }

        if (requestCode == REQUEST_CODE_SCAN && resultCode == Activity.RESULT_OK) {
            String imgPath = intent.getStringExtra(ScanActivity.RESULT_IMAGE_PATH);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Bitmap.Config.ARGB_8888;
            Bitmap bitmap =  BitmapFactory.decodeFile(imgPath, options);
//            viewHolder.image.setImageBitmap(bitmap);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.action_search:
                searchView.openSearch();
                break;
            case R.id.action_clear_recent_pdfs:
                clearRecent();
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
    public void onRecentPdfClick(Uri uri) {

    }

    @Override
    public void onPdfClicked(Pdf pdfFile) {
        openFileInPdfView(pdfFile.getAbsolutePath());
    }


    @Override
    public void onHistoryPdfClicked(Pdf pdfFile) {
        openFileInPdfView(pdfFile.getAbsolutePath());
    }

    public void openFileInPdfView(String pdfPath) {

        //Opening pdf file in pdf viewer
        Intent pdfViewerIntent = new Intent(this, PDFViewerActivity.class);
        pdfViewerIntent.putExtra(PDF_LOCATION, pdfPath);
        Log.d(TAG, "Pdf location " + pdfPath);
        startActivity(pdfViewerIntent);
    }

    public void browsePDF(View view) {

      /*  try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/pdf");
            startActivityForResult(intent, PICK_PDF_REQUEST);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.file_manager_not_found, Toast.LENGTH_SHORT).show();
        }*/

        startActivity(new Intent(this, FileBrowserActivity.class));
    }

    public void clearRecent() {
        DbHelper helper = DbHelper.getInstance(this);
        helper.clearRecentPDFs();
        Toast.makeText(this, R.string.recent_cleared, Toast.LENGTH_SHORT).show();
    }


    public void showListView() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(GRID_VIEW_ENABLED, false);
        editor.apply();
        EventBus.getDefault().post(new DataUpdatedEvent.ToggleGridViewEvent());
        menuListViewItem.setVisible(false);
        menuGridViewItem.setVisible(true);
    }

    public void showGridView(int numOfColumns) {
        new Utils.BackgroundGenerateThumbnails(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(GRID_VIEW_ENABLED, true);
        editor.putInt(GRID_VIEW_NUM_OF_COLUMNS, numOfColumns);
        editor.apply();
        EventBus.getDefault().post(new DataUpdatedEvent.ToggleGridViewEvent());
        menuListViewItem.setVisible(true);
        menuGridViewItem.setVisible(false);
    }

    private void showDevPage() {
        Uri uri = Uri.parse("market://search?q=pub:Robert+Londo");
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/dev?id=6620987035265026854")));
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        Log.d(TAG, newText);
        return true;
    }

    public void sortByName() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SORT_BY, "name");
        editor.apply();
        EventBus.getDefault().post(new DataUpdatedEvent.SortListEvent());
    }

    public void sortByDateModified() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SORT_BY, "date modified");
        editor.apply();
        EventBus.getDefault().post(new DataUpdatedEvent.SortListEvent());
    }

    public void sortBySize() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(SORT_BY, "size");
        editor.apply();
        EventBus.getDefault().post(new DataUpdatedEvent.SortListEvent());
    }

}
