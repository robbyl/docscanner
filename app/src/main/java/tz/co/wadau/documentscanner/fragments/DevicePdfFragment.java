package tz.co.wadau.documentscanner.fragments;

import android.Manifest;
import android.animation.Animator;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

import tz.co.wadau.documentscanner.DataUpdatedEvent;
import tz.co.wadau.documentscanner.R;
import tz.co.wadau.documentscanner.adapters.DevicePdfsAdapter;
import tz.co.wadau.documentscanner.data.DbHelper;
import tz.co.wadau.documentscanner.models.Pdf;
import tz.co.wadau.documentscanner.ui.MaterialSearchView;

import static tz.co.wadau.documentscanner.BrowsePDFActivity.GRID_VIEW_ENABLED;
import static tz.co.wadau.documentscanner.BrowsePDFActivity.GRID_VIEW_NUM_OF_COLUMNS;

public class DevicePdfFragment extends Fragment
        implements MaterialSearchView.OnQueryTextListener {

    final String TAG = DevicePdfFragment.class.getSimpleName();
    private static final int RC_READ_EXTERNAL_STORAGE = 1;

    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    public static String MORE_OPTIONS_TIP = "prefs_more_options_tip";
    private FragmentActivity activityCompat;
    private RecyclerView devicePdfRecyclerView;
    private DevicePdfsAdapter pdfsAdapter;
    private ProgressBar loadingProgressBar;
    private SwipeRefreshLayout swipeRefresh;
    private DbHelper dbHelper;
    private MaterialSearchView searchView;
    private boolean isFragmentVisibleToUser;
    private LinearLayout emptyStateDevice;
    private RelativeLayout infoTapMoreOptions;
    private AppCompatImageView closeInfo;

    private String mParam1;
    private String mParam2;
    public boolean isGridViewEnabled;
    public boolean showMoreOptionsTip;
    int numberOfColumns;
    SharedPreferences sharedPreferences;

    List<Pdf> myPdfs = new ArrayList<>();

    public DevicePdfFragment() {
        // Required empty public constructor
    }

    public static DevicePdfFragment newInstance(String param1, String param2) {
        DevicePdfFragment fragment = new DevicePdfFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        activityCompat = getActivity();
        dbHelper = DbHelper.getInstance(activityCompat);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        isGridViewEnabled = sharedPreferences.getBoolean(GRID_VIEW_ENABLED, false);
        numberOfColumns = sharedPreferences.getInt(GRID_VIEW_NUM_OF_COLUMNS, 2);
        showMoreOptionsTip = sharedPreferences.getBoolean(MORE_OPTIONS_TIP, true);

        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        searchView = activityCompat.findViewById(R.id.search_view);
        emptyStateDevice = view.findViewById(R.id.empty_state_device);
        devicePdfRecyclerView = view.findViewById(R.id.recycler_view_device_pdf);
        loadingProgressBar = view.findViewById(R.id.progress_bar_device_pdfs);
        infoTapMoreOptions = view.findViewById(R.id.info_tap_more_options);
        closeInfo = view.findViewById(R.id.info_close);
        closeInfo.setOnClickListener(closeMoreInfo);
        swipeRefresh = view.findViewById(R.id.swipe_refresh);

        if (showMoreOptionsTip) {
            infoTapMoreOptions.setVisibility(View.VISIBLE);
        } else {
            infoTapMoreOptions.setVisibility(View.GONE);
        }

        if (isGridViewEnabled) {
            setupForGridView(activityCompat, devicePdfRecyclerView, numberOfColumns);
        } else {
            setupForListView(activityCompat, devicePdfRecyclerView);
        }

        if (ActivityCompat.checkSelfPermission(activityCompat, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestStoragePermission();
        } else {
            new LoadPdfFiles().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }

        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new refreshPdfFiles().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == RC_READ_EXTERNAL_STORAGE && grantResults.length >= 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

            Log.d(TAG, "Permission read External storage permission granted");

            new LoadPdfFiles().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            Log.d(TAG, "Permission read External storage permission not granted");
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);

            AlertDialog.Builder builder = new AlertDialog.Builder(activityCompat);
            builder.setTitle(R.string.app_name)
                    .setMessage(R.string.exit_app_has_no_permission)
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            activityCompat.finish();
                        }
                    })
                    .show();
        }
    }

    @Subscribe
    public void onPermanetlyDeleteEvent(DataUpdatedEvent.PermanetlyDeleteEvent event) {
        Log.d(TAG, "onPermanetlyDeleteEvent from device");
        new refreshPdfFiles().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Subscribe
    public void onPdfRenameEvent(DataUpdatedEvent.PdfRenameEvent event) {
        Log.d(TAG, "onPdfRenameEvent from recent");
        new refreshPdfFiles().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Subscribe
    public void onRecentPDFStaredEvent(DataUpdatedEvent.RecentPDFStaredEvent event) {
        Log.d(TAG, "onRecentPDFStaredEvent");
        devicePdfRecyclerView.setAdapter(pdfsAdapter);
    }

    @Subscribe
    public void onToggleGridViewEvent(DataUpdatedEvent.ToggleGridViewEvent event) {
        Log.d(TAG, "onToggleGridViewEvent from devicepdf fragment");
        isGridViewEnabled = sharedPreferences.getBoolean(GRID_VIEW_ENABLED, false);

        if (isGridViewEnabled) {
            numberOfColumns = sharedPreferences.getInt(GRID_VIEW_NUM_OF_COLUMNS, 2);
            setupForGridView(activityCompat, devicePdfRecyclerView, numberOfColumns);
        } else {
            setupForListView(activityCompat, devicePdfRecyclerView);
        }

        Log.d(TAG, "Values " + myPdfs.size());

        pdfsAdapter = new DevicePdfsAdapter(myPdfs, activityCompat);
        devicePdfRecyclerView.setAdapter(pdfsAdapter);
    }

    @Subscribe
    public void onSortListEvent(DataUpdatedEvent.SortListEvent event) {
        new refreshPdfFiles().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_device_pdf, container, false);
    }


    public void requestStoragePermission() {

        final String[] permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};

        if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Toast.makeText(activityCompat, "Read storage permission is required to list files", Toast.LENGTH_SHORT).show();
        }

        requestPermissions(permissions, RC_READ_EXTERNAL_STORAGE);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser) {
            isFragmentVisibleToUser = true;
            if (searchView != null)
                searchView.setOnQueryTextListener(this);
        } else {
            isFragmentVisibleToUser = false;
            if (searchView != null)
                searchView.setOnQueryTextListener(null);
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        //Perform filter when the current fragment in viewpager is open to user
        if (isFragmentVisibleToUser)
            searchPDFFiles(query);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        //Perform filter when the current fragment in viewpager is open to user
        if (isFragmentVisibleToUser)
            searchPDFFiles(newText);
        return true;
    }

    public class LoadPdfFiles extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            myPdfs.clear();
            loadingProgressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            myPdfs = dbHelper.getAllPdfs();
            pdfsAdapter = new DevicePdfsAdapter(myPdfs, activityCompat);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            loadingProgressBar.setVisibility(View.GONE);
            devicePdfRecyclerView.setAdapter(pdfsAdapter);
            if (myPdfs.isEmpty()) {
                emptyStateDevice.setVisibility(View.VISIBLE);
            } else {
                emptyStateDevice.setVisibility(View.GONE);
            }
            pdfsAdapter.updateData(myPdfs);
        }
    }

    public class refreshPdfFiles extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {

            myPdfs = dbHelper.getAllPdfs();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if (myPdfs.isEmpty()) {
                emptyStateDevice.setVisibility(View.VISIBLE);
            } else {
                emptyStateDevice.setVisibility(View.GONE);
            }
            swipeRefresh.setRefreshing(false);
            pdfsAdapter.updateData(myPdfs);
        }
    }

    public void setupForGridView(Context context, RecyclerView recyclerView, int numOfColumns) {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        Float density = metrics.density;
        GridLayoutManager gridLayoutManager = new GridLayoutManager(context, numOfColumns, GridLayoutManager.VERTICAL, false);
        recyclerView.setBackgroundColor(getResources().getColor(R.color.colorLightGray));
        recyclerView.setPadding((int) (4 * density), (int) (4 * density), (int) (6 * density), (int) (80 * density));
        recyclerView.setLayoutManager(gridLayoutManager);
    }

    public void setupForListView(Context context, RecyclerView recyclerView) {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        Float density = metrics.density;
        recyclerView.setBackgroundColor(getResources().getColor(android.R.color.white));
        recyclerView.setPadding(0, 0, (int) (4 * density), (int) (80 * density));
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
    }

    public void searchPDFFiles(String query) {
        List<Pdf> matchedFiles = new ArrayList<>();

        for (Pdf file : myPdfs) {
            if (file.getName().toLowerCase().contains(query.toLowerCase())) {
                matchedFiles.add(file);
            }

            pdfsAdapter.filter(matchedFiles);
        }
    }

    View.OnClickListener closeMoreInfo = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            infoTapMoreOptions.setVisibility(View.GONE);

            infoTapMoreOptions.animate().translationY(-infoTapMoreOptions.getHeight()).alpha(0.0f)
                    .setListener(new Animator.AnimatorListener() {

                        @Override
                        public void onAnimationStart(Animator animator) {

                        }

                        @Override
                        public void onAnimationEnd(Animator animator) {
                            infoTapMoreOptions.setVisibility(View.GONE);
                            SharedPreferences.Editor mEditor = sharedPreferences.edit();
                            mEditor.putBoolean(MORE_OPTIONS_TIP, false);
                            mEditor.apply();
                        }

                        @Override
                        public void onAnimationCancel(Animator animator) {

                        }

                        @Override
                        public void onAnimationRepeat(Animator animator) {

                        }
                    });


        }
    };
}
