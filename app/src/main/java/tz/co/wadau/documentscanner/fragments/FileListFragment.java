package tz.co.wadau.documentscanner.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import tz.co.wadau.documentscanner.R;
import tz.co.wadau.documentscanner.adapters.FileBrowserAdapter;
import tz.co.wadau.documentscanner.models.Pdf;
import tz.co.wadau.documentscanner.utils.Utils;

import static tz.co.wadau.documentscanner.BrowsePDFActivity.GRID_VIEW_ENABLED;
import static tz.co.wadau.documentscanner.BrowsePDFActivity.GRID_VIEW_NUM_OF_COLUMNS;

public class FileListFragment extends Fragment
        implements FileBrowserAdapter.OnPdfClickListener {
    private static final String FILE_PATH = "file_path";

    private String mFilePath;
    String THUMBNAILS_DIR;
    Context context;
    int numberOfColumns;
    boolean isGridViewEnabled;
    FileBrowserAdapter adapter;
    RecyclerView recyclerView;
    ProgressBar progressBarlistDirectory;
    LinearLayout emptyDirectory;
    List<Pdf> dirList = new ArrayList<>();

    public FileListFragment() {
        // Required empty public constructor
    }

    public static FileListFragment newInstance(String filePath) {
        FileListFragment fragment = new FileListFragment();
        Bundle args = new Bundle();
        args.putString(FILE_PATH, filePath);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = getContext();

        if (getArguments() != null) {
            mFilePath = getArguments().getString(FILE_PATH);
            THUMBNAILS_DIR = context.getCacheDir() + "/Thumbnails/";
            int defaultNumColumns = Utils.isTablet(context) ? 6 : 3;
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            numberOfColumns = sharedPreferences.getInt(GRID_VIEW_NUM_OF_COLUMNS, defaultNumColumns);
            isGridViewEnabled = sharedPreferences.getBoolean(GRID_VIEW_ENABLED, false);
        }
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_view_browse_pdf);
        progressBarlistDirectory = view.findViewById(R.id.progress_bar_list_dir);
        emptyDirectory = view.findViewById(R.id.empty_state_directory);
        new ListDirectory().execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_file_list, container, false);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onPdfClicked(Pdf pdfFile) {

    }


    public List<Pdf> getFiles(String dir) {
        File mDir = new File(dir);
        List<Pdf> pdfFiles = new ArrayList<>();

        if (mDir.isDirectory()) {
//            File[] files = mDir.listFiles(fileFilter);
//
//            if (files != null) {

                for (File pdfFile : mDir.listFiles(fileFilter)) {

                    Uri thumbnailUri = !pdfFile.isDirectory() ? Utils.getImageUriFromPath(THUMBNAILS_DIR + Utils.removeExtension(pdfFile.getName()) + ".jpg") : null;
                    int numItems = pdfFile.isDirectory() ? getFiles(pdfFile.getAbsolutePath()).size() : 0;

                    Pdf mPdf = new Pdf();
                    mPdf.setName(pdfFile.getName());
                    mPdf.setAbsolutePath(pdfFile.getAbsolutePath());
                    mPdf.setPdfUri(Uri.fromFile(pdfFile));
                    mPdf.setLength(pdfFile.length());
                    mPdf.setLastModified(pdfFile.lastModified());
                    mPdf.setThumbUri(thumbnailUri);
                    mPdf.setDirectory(pdfFile.isDirectory());
                    mPdf.setNumItems(numItems);

                    pdfFiles.add(mPdf);
                }
//            }
        }

        Collections.sort(pdfFiles, new Comparator<Pdf>() {
            @Override
            public int compare(Pdf pdf1, Pdf pdf2) {
                if (pdf1.isDirectory() && !pdf2.isDirectory()) {
                    return -1;
                } else if (!pdf1.isDirectory() && pdf2.isDirectory()) {
                    return 1;
                } else {
                    return pdf1.getName().compareToIgnoreCase(pdf2.getName());
                }
            }
        });

        return pdfFiles;
    }

    //Accept PDF files and visible Directories
    FileFilter fileFilter = new FileFilter() {
        @Override
        public boolean accept(File pathname) {
            String extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(pathname).toString());
            return (pathname.isDirectory() && !pathname.isHidden()) || TextUtils.equals(extension, "pdf") || TextUtils.equals(extension, "PDF");
        }
    };

    public class ListDirectory extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressBarlistDirectory.setVisibility(View.VISIBLE);

            if (isGridViewEnabled) {
                recyclerView.setBackgroundColor(getResources().getColor(R.color.colorLightGray));
                recyclerView.setLayoutManager(new GridLayoutManager(context, numberOfColumns, GridLayoutManager.VERTICAL, false));
            } else {
                recyclerView.setBackgroundColor(getResources().getColor(android.R.color.white));
                recyclerView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            dirList = getFiles(mFilePath);
            adapter = new FileBrowserAdapter(context, dirList);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progressBarlistDirectory.setVisibility(View.GONE);
            recyclerView.setAdapter(adapter);
            if (dirList.size() == 0) {
                emptyDirectory.setVisibility(View.VISIBLE);
            } else {
                emptyDirectory.setVisibility(View.GONE);
            }
        }
    }
}
