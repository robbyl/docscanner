package tz.co.wadau.documentscanner.fragments;


import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.List;

import tz.co.wadau.documentscanner.R;
import tz.co.wadau.documentscanner.adapters.BookmarksAdapter;
import tz.co.wadau.documentscanner.data.DbHelper;
import tz.co.wadau.documentscanner.models.Bookmark;

public class BookmarksFragment extends Fragment {
    private final String TAG = BookmarksFragment.class.getSimpleName();
    private static final String PDF_PATH = "pdf_path";
    private String mPdfPath;
    RecyclerView bookMarkRecyclerView;
    BookmarksAdapter adapter;
    Context context;
    private LinearLayout emptyState;

    public BookmarksFragment() {
        // Required empty public constructor
    }

    public static BookmarksFragment newInstance(String pdfPath) {
        BookmarksFragment fragment = new BookmarksFragment();
        Bundle args = new Bundle();
        args.putString(PDF_PATH, pdfPath);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getContext();

        if (getArguments() != null) {
            mPdfPath = getArguments().getString(PDF_PATH);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_bookmarks, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bookMarkRecyclerView = view.findViewById(R.id.recycler_view_bookmarks);
        emptyState = view.findViewById(R.id.empty_state_bookmark);
        new LoadBookmarks().execute();
    }

    public class LoadBookmarks extends AsyncTask<Void, Void, Void> {
        List<Bookmark> bookmarks = new ArrayList<>();

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            DbHelper dbHelper = DbHelper.getInstance(context);
            bookmarks = dbHelper.getBookmarks(mPdfPath);
            adapter = new BookmarksAdapter(context, bookmarks, emptyState);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            if (bookmarks.size() == 0) {
                emptyState.setVisibility(View.VISIBLE);
            } else {
                bookMarkRecyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
                bookMarkRecyclerView.setAdapter(adapter);
                emptyState.setVisibility(View.GONE);
            }
        }
    }
}
