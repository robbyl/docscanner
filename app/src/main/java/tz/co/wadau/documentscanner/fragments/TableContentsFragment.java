package tz.co.wadau.documentscanner.fragments;


import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import tz.co.wadau.documentscanner.R;
import tz.co.wadau.documentscanner.adapters.ContentsAdapter;

public class TableContentsFragment extends Fragment {
    final String TAG = TableContentsFragment.class.getSimpleName();
    private static final String PDF_PATH = "pdf_path";
    public static final String SAVED_STATE = "prefs_saved_state";
    private String mPdfPath;
    Context context;
    ContentsAdapter adapter;
    RecyclerView recyclerView;
    SharedPreferences preferences;
    int lastFirstVisiblePosition = 0;
    private LinearLayout emptyState;

    public TableContentsFragment() {
        // Required empty public constructor
    }

    public static TableContentsFragment newInstance(String pdfPath) {
        TableContentsFragment fragment = new TableContentsFragment();
        Bundle args = new Bundle();
        args.putString(PDF_PATH, pdfPath);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getContext();
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (getArguments() != null) {
            mPdfPath = getArguments().getString(PDF_PATH);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recyclerView = view.findViewById(R.id.recycler_view_contents);
        emptyState = view.findViewById(R.id.empty_state_contents);
        new LoadTableOfContents().execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_table_contents, container, false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        int lastFirstVisiblePosition = ((LinearLayoutManager) recyclerView.getLayoutManager()).findFirstCompletelyVisibleItemPosition();
        preferences.edit().putInt(SAVED_STATE, lastFirstVisiblePosition).apply();
    }

    public class LoadTableOfContents extends AsyncTask<Void, Void, Void> {
        private PdfiumCore pdfiumCore;
        private PdfDocument pdfDocument;
        List<PdfDocument.Bookmark> contents = new ArrayList<>();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            lastFirstVisiblePosition = preferences.getInt(SAVED_STATE, 0);

            try {
                pdfiumCore = new PdfiumCore(context);
                Uri fileUri = Uri.fromFile(new File(mPdfPath));
                ParcelFileDescriptor fd = context.getContentResolver().openFileDescriptor(fileUri, "r");
                pdfDocument = pdfiumCore.newDocument(fd);
                contents = pdfiumCore.getTableOfContents(pdfDocument);
            } catch (Exception e) {
                e.printStackTrace();
            }

            adapter = new ContentsAdapter(context, contents);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if (contents.size() == 0) {
                emptyState.setVisibility(View.VISIBLE);
            } else {
                emptyState.setVisibility(View.GONE);
            }

            recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
            recyclerView.setAdapter(adapter);

            recyclerView.getLayoutManager().scrollToPosition(lastFirstVisiblePosition);
        }
    }
}
