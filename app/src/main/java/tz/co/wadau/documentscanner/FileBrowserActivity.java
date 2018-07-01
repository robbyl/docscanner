package tz.co.wadau.documentscanner;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;

import tz.co.wadau.documentscanner.adapters.FileBrowserAdapter;
import tz.co.wadau.documentscanner.fragments.FileListFragment;
import tz.co.wadau.documentscanner.models.Pdf;

import static tz.co.wadau.documentscanner.BrowsePDFActivity.PDF_LOCATION;

public class FileBrowserActivity extends AppCompatActivity
        implements FileBrowserAdapter.OnPdfClickListener {

    final String TAG = FileBrowserActivity.class.getSimpleName();
    String rootPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_browser);

        Toolbar toolbar = findViewById(R.id.toolbar_file_browser);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        rootPath = Environment.getExternalStorageDirectory() + "/";
        Log.d(TAG, "Root path " + rootPath);
        listDirFiles(rootPath);
    }

    public void listDirFiles(String dir) {

        File file = new File(dir);

        if (file.isDirectory()) {
            Fragment fragment = FileListFragment.newInstance(dir);

            if (TextUtils.equals(dir, rootPath)) {
                getSupportFragmentManager().beginTransaction().replace(R.id.file_list_container, fragment)
                        .commit();
            } else {
                getSupportFragmentManager().beginTransaction().replace(R.id.file_list_container, fragment)
                        .addToBackStack(null).commit();
            }

        } else {
            //Opening pdf file in pdf viewer
            Intent pdfViewerIntent = new Intent(this, PDFViewerActivity.class);
            pdfViewerIntent.putExtra(PDF_LOCATION, dir);
            startActivity(pdfViewerIntent);
        }
    }

    @Override
    public void onPdfClicked(Pdf pdfFile) {
        listDirFiles(pdfFile.getAbsolutePath());
    }
}
