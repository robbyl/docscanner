package tz.co.wadau.documentscanner;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import java.util.List;

import tz.co.wadau.documentscanner.adapters.ViewImagesAdapter;
import tz.co.wadau.documentscanner.data.DbHelper;

public class ViewImagesActivity extends AppCompatActivity {

    private final String TAG = ViewImagesActivity.class.getSimpleName();
    public static final String GENERATED_IMAGES_PATH = "tz.co.wadau.documentscanner.GENERATED_IMAGES_PATH";
    private RecyclerView recyclerViewImageView;
    private ProgressBar progressBarViewImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_images);

        Toolbar toolbar = findViewById(R.id.toolbar_view_image);
        recyclerViewImageView = findViewById(R.id.recyclerview_view_image);
        progressBarViewImage = findViewById(R.id.progress_bar_view_image);

        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            String imageDir = bundle.getString(GENERATED_IMAGES_PATH);
            recyclerViewImageView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
            new LoadImages(this, imageDir).execute();
        }
    }

    public class LoadImages extends AsyncTask<Void, Void, Void> {
        Context context;
        String imageDirectory;
        ViewImagesAdapter adapter;

        public LoadImages(Context context, String imageDirectory) {
            this.context = context;
            this.imageDirectory = imageDirectory;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            DbHelper dbHelper = DbHelper.getInstance(context);
            List<Uri> uris = dbHelper.getAllImages(imageDirectory);
            Log.d(TAG, "Images so far " + uris.size());
            adapter = new ViewImagesAdapter(context, uris);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progressBarViewImage.setVisibility(View.GONE);
            recyclerViewImageView.setAdapter(adapter);
        }
    }

}
