package tz.co.wadau.documentscanner;

import android.animation.Animator;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tz.co.wadau.documentscanner.adapters.ShareAsPictureAdapter;
import tz.co.wadau.documentscanner.models.PDFPage;
import tz.co.wadau.documentscanner.utils.Utils;

import static tz.co.wadau.documentscanner.PDFToolsActivity.PDF_PATH;

public class ShareAsPictureActivity extends AppCompatActivity {

    final String TAG = ShareAsPictureActivity.class.getSimpleName();
    RecyclerView recyclerView;
    Context context;
    ProgressBar progressBar;
    String allPdfPictureDir;
    String allPdfDocuments;
    String dirAsfileName;
    String pdfFilePath;
    ShareAsPictureAdapter organizePagesAdapter;
    List<PDFPage> pdfPages = new ArrayList<>();
    List<PDFPage> finalPages = new ArrayList<>();
    private RelativeLayout infoTapMoreOptions;
    private AppCompatImageView closeInfo;
    private SharedPreferences sharedPreferences;
    public static String ORGANIZE_SHARE_PAGES_TIP = "prefs_organize_share_pages";
    boolean showOrganizePagesTip;
    FloatingActionButton btnSav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share_as_picture);

        allPdfPictureDir = Environment.getExternalStorageDirectory() + "/Pictures/AllPdf/tmp/";
        allPdfDocuments = Environment.getExternalStorageDirectory() + "/Documents/AllPdf/";
        Toolbar toolbar = findViewById(R.id.toolbar_organize_pages);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        this.context = this;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        showOrganizePagesTip = sharedPreferences.getBoolean(ORGANIZE_SHARE_PAGES_TIP, true);
        recyclerView = findViewById(R.id.recyclerview_share_as_picture);
        progressBar = findViewById(R.id.progress_bar_organize_pages);
        btnSav = findViewById(R.id.fab_save);
        infoTapMoreOptions = findViewById(R.id.info_tap_more_options);
        closeInfo = findViewById(R.id.info_close);
        closeInfo.setOnClickListener(closeMoreInfo);

        Intent intent = getIntent();
        pdfFilePath = intent.getStringExtra(PDF_PATH);

        if (showOrganizePagesTip) {
            infoTapMoreOptions.setVisibility(View.VISIBLE);
        } else {
            infoTapMoreOptions.setVisibility(View.GONE);
        }

        new LoadPageThumbnails().execute(pdfFilePath);

        btnSav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finalPages = organizePagesAdapter.getFinalOrganizedPages();
                new saveShareImagePages(getOrganizedPages(finalPages)).execute();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Utils.deleteFiles(allPdfPictureDir);
        Log.d(TAG, "Deleting temp dir " + allPdfPictureDir);
    }

    public class LoadPageThumbnails extends AsyncTask<String, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(String... strings) {

            PdfiumCore pdfiumCore = new PdfiumCore(context);
            File file = new File(strings[0]);
            dirAsfileName = Utils.removeExtension(file.getName()) + "/";
            Uri fileUri = Uri.fromFile(file);
            Log.d(TAG, "Loading page thumbs from uri " + fileUri.toString());

            try {

                ParcelFileDescriptor fd = context.getContentResolver().openFileDescriptor(fileUri, "r");
                PdfDocument pdfDocument = pdfiumCore.newDocument(fd);
                int numPages = pdfiumCore.getPageCount(pdfDocument);
                Bitmap bmp;
                Log.d(TAG, "Total number of pages " + numPages);

                File folder = new File(allPdfPictureDir + dirAsfileName);
                if (!folder.exists())
                    folder.mkdirs();

                for (int pageNumber = 0; pageNumber < numPages; pageNumber++) {

                    String imagePath = allPdfPictureDir + dirAsfileName + "page-" + (pageNumber + 1) + ".jpg";
                    Log.d(TAG, "Generating temp share img " + imagePath);
                    FileOutputStream outputStream = new FileOutputStream(imagePath);

                    pdfiumCore.openPage(pdfDocument, pageNumber);
                    int width = pdfiumCore.getPageWidthPoint(pdfDocument, pageNumber);
                    int height = pdfiumCore.getPageHeightPoint(pdfDocument, pageNumber);

                    try {
                        bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                        pdfiumCore.renderPageBitmap(pdfDocument, bmp, pageNumber, 0, 0, width, height, true);
                        bmp.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                    } catch (OutOfMemoryError error) {
                        Toast.makeText(context, R.string.failed_low_memory, Toast.LENGTH_LONG).show();
                        error.printStackTrace();
                    }

                    Uri uri = Uri.fromFile(new File(imagePath));
                    PDFPage pdfPage = new PDFPage(pageNumber + 1, uri);
                    pdfPages.add(pdfPage);
                    outputStream.close();
                }

                pdfiumCore.closeDocument(pdfDocument); // important!

            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            organizePagesAdapter = new ShareAsPictureAdapter(context, pdfPages);
            int spanCount = Utils.isTablet(context) ? 6 : 3;
            recyclerView.setLayoutManager(new GridLayoutManager(context, spanCount, GridLayoutManager.VERTICAL, false));
            progressBar.setVisibility(View.GONE);
            recyclerView.setAdapter(organizePagesAdapter);
            btnSav.setVisibility(View.VISIBLE);

            int swipDirs = ItemTouchHelper.UP | ItemTouchHelper.DOWN |
                    ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;

            ItemTouchHelper touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(swipDirs, 0) {
                @Override
                public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                    int fromPos = viewHolder.getAdapterPosition();
                    int toPos = target.getAdapterPosition();
//                    Collections.swap(pdfPages, fromPos, toPos);

                    PDFPage dPage = pdfPages.remove(toPos);
                    pdfPages.add(fromPos, dPage);
                    organizePagesAdapter.notifyItemMoved(toPos, fromPos);
                    Log.d(TAG, "moved from " + fromPos + " to position " + toPos);
                    return true;
                }


                @Override
                public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

                }

                @Override
                public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                    super.clearView(recyclerView, viewHolder);
                    Log.d(TAG, "Page order after swap " + getOrganizedPages(pdfPages).toString());
                }
            });

            touchHelper.attachToRecyclerView(recyclerView);

        }
    }

    public class saveShareImagePages extends AsyncTask<Void, Void, Void> {
        private List<Integer> organizedPages;
        ProgressDialog progressDialog;
        String imageName;
        final int SPACE_BETWEEN_PAGES = 4;

        public saveShareImagePages(List<Integer> pages) {
            this.organizedPages = pages;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage(context.getString(R.string.saving_wait));
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            try {

                File folder = new File(allPdfPictureDir + dirAsfileName);
                String mFileName = new File(pdfFilePath).getName();

                if (!folder.exists())
                    folder.mkdirs();

                imageName = allPdfPictureDir + dirAsfileName + Utils.removeExtension(mFileName) + ".jpg";
                int targetHeight = 0;
                List<Integer> widths = new ArrayList<>();
                int targetWidth;
                int numImagesToMerge = organizedPages.size();
                Log.d(TAG, "Num of pages to merge " + numImagesToMerge);

                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;

                //Calculating the final height of the image
                for (int i = 0; i < numImagesToMerge; i++) {
                    String imagePage = allPdfPictureDir + dirAsfileName + "page-" + organizedPages.get(i) + ".jpg";
                    BitmapFactory.decodeFile(imagePage, options);
                    //Add bottom page separator for all pages but not to the last one
                    targetHeight = ((i + 1) == numImagesToMerge) ? targetHeight + options.outHeight : targetHeight + options.outHeight + SPACE_BETWEEN_PAGES;
                    widths.add(options.outWidth);
                }

                //Find the page with the largest with and set as the width of the background
                targetWidth = Collections.max(widths);

                Log.d(TAG, "Target width " + targetWidth + " Target height " + targetHeight);

                //Creating background for merged pages
                try {
                    Bitmap bitmapOverlay = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
                    bitmapOverlay.eraseColor(getResources().getColor(R.color.colorPDFViewBg)); //Fill the bg with specified color
                    Canvas canvas = new Canvas(bitmapOverlay);

                    //Drawing page images on the generated background
                    int currHeight = 0;
                    for (int i = 0; i < numImagesToMerge; i++) {
                        String imagePage = allPdfPictureDir + dirAsfileName + "page-" + organizedPages.get(i) + ".jpg";
                        Log.d(TAG, "Bitmap decode from " + imagePage);
                        Bitmap bitmap = BitmapFactory.decodeFile(imagePage);

                        canvas.drawBitmap(bitmap, calculateLeft(bitmap.getWidth(), targetWidth), currHeight, null);
                        //Add a bottom line to separate pages except the last page
                        currHeight = ((i + 1) == numImagesToMerge) ? currHeight + bitmap.getHeight() : currHeight + bitmap.getHeight() + SPACE_BETWEEN_PAGES;
                        bitmap.recycle();
                    }

                    //Saving the combined image pages on the SD
                    File mFile = new File(imageName);
                    FileOutputStream mFileOutputStream = new FileOutputStream(mFile);
                    bitmapOverlay.compress(Bitmap.CompressFormat.JPEG, 100, mFileOutputStream);
                    mFileOutputStream.flush();
                    mFileOutputStream.close();
                } catch (OutOfMemoryError error) {
                    Toast.makeText(context, R.string.failed_low_memory, Toast.LENGTH_LONG).show();
                    error.printStackTrace();
                }

                Log.d(TAG, "File to share generated " + allPdfPictureDir + dirAsfileName + Utils.removeExtension(mFileName) + ".jpg");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progressDialog.dismiss();
            Utils.shareFile(context, imageName);
        }
    }

    public List<Integer> getOrganizedPages(List<PDFPage> pdfPages) {
        List<Integer> pagesToKeep = new ArrayList<>();
        for (int i = 0; i < pdfPages.size(); i++) {
            pagesToKeep.add(pdfPages.get(i).getPageNumber());
        }
        return pagesToKeep;
    }


    //Calculate the left position so as the page is centered horizontally in the final image
    public int calculateLeft(int imageWidth, int maxPageWidth) {
        if (maxPageWidth > imageWidth) {
            return ((maxPageWidth - imageWidth) / 2);
        } else {
            return 0;
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
                            mEditor.putBoolean(ORGANIZE_SHARE_PAGES_TIP, false);
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
