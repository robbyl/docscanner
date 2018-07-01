package tz.co.wadau.documentscanner;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;

import tz.co.wadau.documentscanner.adapters.SelectImagesAdapter;
import tz.co.wadau.documentscanner.data.DbHelper;
import tz.co.wadau.documentscanner.utils.Utils;

import static tz.co.wadau.documentscanner.BrowsePDFActivity.GRID_VIEW_NUM_OF_COLUMNS;
import static tz.co.wadau.documentscanner.OrganizeImagesActivity.IMAGE_URIS;

public class SelectImagesActivity extends AppCompatActivity
        implements SelectImagesAdapter.OnImageSelectedListener {

    public static final String TAG = SelectImagesActivity.class.getSimpleName();
    private RecyclerView imagesRecyclerView;
    private DbHelper dbHelper;
    private Context context;
    private ProgressBar progressBar;
    private SharedPreferences sharedPreferences;
    private int numberOfColumns;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_images);

        Toolbar toolbar = findViewById(R.id.toolbar_select_images);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        Spinner spinnerImgDirectories = findViewById(R.id.spinner_img_directories);
        imagesRecyclerView = findViewById(R.id.recycler_view_select_images);
        progressBar = findViewById(R.id.progress_bar_select_images);
        dbHelper = DbHelper.getInstance(this);
        context = this;

        int defaultNumColumns = Utils.isTablet(this) ? 6 : 3;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        numberOfColumns = sharedPreferences.getInt(GRID_VIEW_NUM_OF_COLUMNS, defaultNumColumns);

        spinnerImgDirectories.setSelection(3);
        spinnerImgDirectories.setOnItemSelectedListener(selectedListener);
    }

    public class LoadImages extends AsyncTask<Void, Void, Void> {
        private SelectImagesAdapter adapter;
        private String imageDir;


        public LoadImages(String imgDir) {
            this.imageDir = imgDir;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... voids) {

            List<Uri> imageUris = dbHelper.getAllImages(Environment.getExternalStorageDirectory() + imageDir);
            adapter = new SelectImagesAdapter(context, imageUris);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progressBar.setVisibility(View.GONE);
            imagesRecyclerView.setLayoutManager(new GridLayoutManager(context, numberOfColumns, GridLayoutManager.VERTICAL, false));
            imagesRecyclerView.setAdapter(adapter);
        }
    }

    @Override
    public void onMultiSelectedPDF(ArrayList<String> selectedImageUris) {
        Intent intent = new Intent(this, OrganizeImagesActivity.class);
        intent.putStringArrayListExtra(IMAGE_URIS, selectedImageUris);
        startActivity(intent);
    }

    AdapterView.OnItemSelectedListener selectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {

            switch (i) {
                case 0:
                    new LoadImages("/").execute();
                    break;
                case 1:
                    new LoadImages("/DCIM/").execute();
                    break;
                case 2:
                    new LoadImages("/Download/").execute();
                    break;
                case 3:
                    new LoadImages("/Pictures/").execute();
                    break;
                case 4:
                    new LoadImages("/WhatsApp/Media/WhatsApp Images/").execute();
                    break;
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> adapterView) {
        }
    };

}
