package tz.co.wadau.documentscanner.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
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

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.List;

import tz.co.wadau.documentscanner.DataUpdatedEvent;
import tz.co.wadau.documentscanner.R;
import tz.co.wadau.documentscanner.adapters.RecentPdfsAdapter;
import tz.co.wadau.documentscanner.data.DbHelper;
import tz.co.wadau.documentscanner.models.Pdf;
import tz.co.wadau.documentscanner.ui.MaterialSearchView;

import static tz.co.wadau.documentscanner.BrowsePDFActivity.GRID_VIEW_ENABLED;
import static tz.co.wadau.documentscanner.BrowsePDFActivity.GRID_VIEW_NUM_OF_COLUMNS;

public class RecentPdfFragment extends Fragment
        implements MaterialSearchView.OnQueryTextListener {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    private final String TAG = RecentPdfFragment.class.getCanonicalName();
    private RecyclerView historyPdfRecyclerView;
    private LinearLayout emptyState;
    private ProgressBar progressBar;
    private RecentPdfsAdapter historyPdfsAdapter;
    private Context mContext;
    private List<Pdf> historyPdfs = new ArrayList<>();
    private BottomSheetBehavior mBottomSheetBehavior;
    public boolean isGridViewEnabled;
    private int numberOfColumns;
    private SharedPreferences sharedPreferences;
    private MaterialSearchView searchView;
    private boolean isFragmentVisibleToUser;
    private SwipeRefreshLayout swipeRefreshLayout;
    private DbHelper dbHelper;

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnRecentPdfClickListener mListener;

    public RecentPdfFragment() {
        // Required empty public constructor
    }

    public static RecentPdfFragment newInstance(String param1, String param2) {
        RecentPdfFragment fragment = new RecentPdfFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContext = getContext();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        isGridViewEnabled = sharedPreferences.getBoolean(GRID_VIEW_ENABLED, false);
        numberOfColumns = sharedPreferences.getInt(GRID_VIEW_NUM_OF_COLUMNS, 2);
        historyPdfsAdapter = new RecentPdfsAdapter(historyPdfs, mContext);
        dbHelper = DbHelper.getInstance(mContext);

        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        new UpdateHistoryPdfFiles().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
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
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        historyPdfRecyclerView = view.findViewById(R.id.recycler_view_history_pdf);
        emptyState = view.findViewById(R.id.empty_state_recent);
        searchView = getActivity().findViewById(R.id.search_view);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);
        searchView.setOnQueryTextListener(this);

        if (isGridViewEnabled) {
            setupForGridView(mContext, historyPdfRecyclerView, numberOfColumns);
        } else {
            setupForListView(mContext, historyPdfRecyclerView);
        }

        progressBar = view.findViewById(R.id.progress_bar_history_pdfs);
        View bottomSheet = view.findViewById(R.id.relative_layout);

        mBottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        new LoadHistoryPfdFiles().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                new UpdateHistoryPdfFiles().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_recents_pdf, container, false);
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onRecentPdfClick(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnRecentPdfClickListener) {
            mListener = (OnRecentPdfClickListener) context;
        } else {
            throw new RuntimeException(context.toString() + " must implement OnRecentPdfClickListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
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
        Log.d(TAG, "Search query from recent fragment " + query);
        if (isFragmentVisibleToUser)
            searchPDFFiles(query);
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        //Perform filter when the current fragment in viewpager is open to user
        Log.d(TAG, "Search query from recent fragment " + newText);
        if (isFragmentVisibleToUser)
            searchPDFFiles(newText);
        return true;
    }

    public interface OnRecentPdfClickListener {
        void onRecentPdfClick(Uri uri);
    }

    @Subscribe
    public void onRecentPdfInsert(DataUpdatedEvent.RecentPdfInsert event) {
        Log.d(TAG, "onRecentPdfInsert from recent");
        new UpdateHistoryPdfFiles().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Subscribe
    public void onRecentPdfDeleteEvent(DataUpdatedEvent.RecentPdfDeleteEvent event) {
        Log.d(TAG, "onRecentPdfDeleteEvent from recent");
        new UpdateHistoryPdfFiles().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Subscribe
    public void onPermanetlyDeleteEvent(DataUpdatedEvent.PermanetlyDeleteEvent event) {
        Log.d(TAG, "onPermanetlyDeleteEvent from recent");
        new UpdateHistoryPdfFiles().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Subscribe
    public void onRecentPdfClearEvent(DataUpdatedEvent.RecentPdfClearEvent event) {
        Log.d(TAG, "onRecentPdfClearEvent from recent");
        new UpdateHistoryPdfFiles().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Subscribe
    public void onPdfRenameEvent(DataUpdatedEvent.PdfRenameEvent event) {
        Log.d(TAG, "onPdfRenameEvent from recent");
        new UpdateHistoryPdfFiles().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Subscribe
    public void onDevicePDFStaredEvent(DataUpdatedEvent.DevicePDFStaredEvent event) {
        Log.d(TAG, "onDevicePDFStaredEvent");
        historyPdfRecyclerView.setAdapter(historyPdfsAdapter);
    }

    @Subscribe
    public void onToggleGridViewEvent(DataUpdatedEvent.ToggleGridViewEvent event) {
        Log.d(TAG, "onToggleGridViewEvent from recent fragment");
        isGridViewEnabled = sharedPreferences.getBoolean(GRID_VIEW_ENABLED, false);
        numberOfColumns = sharedPreferences.getInt(GRID_VIEW_NUM_OF_COLUMNS, 2);

        if (isGridViewEnabled) {
            setupForGridView(mContext, historyPdfRecyclerView, numberOfColumns);
        } else {
            setupForListView(mContext, historyPdfRecyclerView);
        }
        Log.d(TAG, "Recent item size " + historyPdfs.size());
        historyPdfsAdapter = new RecentPdfsAdapter(historyPdfs, mContext);
        historyPdfRecyclerView.setAdapter(historyPdfsAdapter);
        historyPdfsAdapter.notifyDataSetChanged();
    }


    public class LoadHistoryPfdFiles extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            DbHelper dbHelper = DbHelper.getInstance(mContext);

            historyPdfs.clear();
            historyPdfs = dbHelper.getRecentPDFs();
            historyPdfsAdapter = new RecentPdfsAdapter(historyPdfs, mContext);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            progressBar.setVisibility(View.GONE);

            if (historyPdfs.isEmpty()) {
                emptyState.setVisibility(View.VISIBLE);
            } else {
                emptyState.setVisibility(View.GONE);
            }

            historyPdfRecyclerView.setAdapter(historyPdfsAdapter);
        }
    }

    public class UpdateHistoryPdfFiles extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... voids) {

            if (historyPdfRecyclerView != null) {
                historyPdfs = dbHelper.getRecentPDFs();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if (historyPdfs.isEmpty()) {
                emptyState.setVisibility(View.VISIBLE);
            } else {
                emptyState.setVisibility(View.GONE);
            }

            swipeRefreshLayout.setRefreshing(false);
            historyPdfsAdapter.updateData(historyPdfs);
        }
    }

    public void setupForGridView(Context context, RecyclerView recyclerView, int numOfColumns) {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        Float density = metrics.density;
        GridLayoutManager gridLayoutManager = new GridLayoutManager(context, numOfColumns,
                GridLayoutManager.VERTICAL, false);
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

        for (Pdf file : historyPdfs) {
            if (file.getName().toLowerCase().contains(query.toLowerCase())) {
                matchedFiles.add(file);
            }

            historyPdfsAdapter.filter(matchedFiles);
        }
    }
}
