package tz.co.wadau.documentscanner;

import android.animation.Animator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.InterstitialAd;
import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import tz.co.wadau.documentscanner.adapters.ExtractTextsPagesAdapter;
import tz.co.wadau.documentscanner.models.PDFPage;
import tz.co.wadau.documentscanner.utils.AdManager;
import tz.co.wadau.documentscanner.utils.Utils;

import static tz.co.wadau.documentscanner.PDFToolsActivity.PDF_PATH;

public class ExtractTextsPagesActivity extends AppCompatActivity {

    final String TAG = ExtractTextsPagesActivity.class.getSimpleName();
    private RecyclerView recyclerView;
    private Context context;
    private ProgressBar progressBar;
    private ProgressBar progressBarLoadPagePreview;
    private String allPdfPictureDir;
    private String allPdfDocuments;
    private String pdfFilePath;
    private List<PDFPage> pdfPages = new ArrayList<>();
    private ConstraintLayout mProgressView;
    private TextView percent;
    private TextView currentAction;
    private TextView savedPath;
    private AppCompatImageView successIcon, closeProgressView;
    private AppCompatButton btnOpenFile, btnCancelProgress;
    private ConstraintLayout progressView;
    private RelativeLayout infoTapMoreOptions;
    private AppCompatImageView closeInfo;
    private SharedPreferences sharedPreferences;
    public static String ORGANIZE_PAGES_TIP = "prefs_organize_pages";
    boolean showOrganizePagesTip;
    private FloatingActionButton btnSave;
    private ExtractTextsPagesAdapter extractTextsPagesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_extract_texts_pages);

        allPdfPictureDir = Environment.getExternalStorageDirectory() + "/Pictures/AllPdf/tmp/";
        allPdfDocuments = Environment.getExternalStorageDirectory() + "/Documents/AllPdf/";
        Toolbar toolbar = findViewById(R.id.toolbar_organize_pages);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        this.context = this;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        showOrganizePagesTip = sharedPreferences.getBoolean(ORGANIZE_PAGES_TIP, true);
        recyclerView = findViewById(R.id.recycler_view_organize_pages);
        progressBarLoadPagePreview = findViewById(R.id.progress_bar_organize_pages);
        btnSave = findViewById(R.id.fab_save);
        progressView = findViewById(R.id.progress_view);
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

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                extractTextsPagesAdapter.finishActionMode(false);
                List<Integer> mSelectedPages = extractTextsPagesAdapter.getSelectedPages();

                if (mSelectedPages.size() > 0) {
                    new ExtractText(context, pdfFilePath, extractTextsPagesAdapter.getSelectedPages(), progressView).execute();
                } else {
                    Toast.makeText(context, R.string.select_at_least_one_page, Toast.LENGTH_LONG).show();
                }
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
            Uri fileUri = Uri.fromFile(new File(strings[0]));
            Log.d(TAG, "Loading page thumbs from uri " + fileUri.toString());

            try {

                ParcelFileDescriptor fd = context.getContentResolver().openFileDescriptor(fileUri, "r");
                PdfDocument pdfDocument = pdfiumCore.newDocument(fd);
                int numPages = pdfiumCore.getPageCount(pdfDocument);
                Log.d(TAG, "Total number of pages " + numPages);

                File folder = new File(allPdfPictureDir);
                if (!folder.exists())
                    folder.mkdirs();

                for (int pageNumber = 0; pageNumber < numPages; pageNumber++) {

                    String imagePath = allPdfPictureDir + System.currentTimeMillis() + ".jpg";
                    Log.d(TAG, "Generating temp img " + imagePath);
                    FileOutputStream outputStream = new FileOutputStream(imagePath);

                    pdfiumCore.openPage(pdfDocument, pageNumber);
                    int width = pdfiumCore.getPageWidthPoint(pdfDocument, pageNumber) / 2;
                    int height = pdfiumCore.getPageHeightPoint(pdfDocument, pageNumber) / 2;

                    try {
                        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                        pdfiumCore.renderPageBitmap(pdfDocument, bmp, pageNumber, 0, 0, width, height, true);
                        bmp.compress(Bitmap.CompressFormat.JPEG, 50, outputStream);
                    } catch (OutOfMemoryError error) {
                        Toast.makeText(context, R.string.failed_low_memory, Toast.LENGTH_LONG).show();
                        error.printStackTrace();
                    }

                    Uri uri = Uri.fromFile(new File(imagePath));
                    PDFPage pdfPage = new PDFPage(pageNumber + 1, uri);
                    pdfPages.add(pdfPage);
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
            extractTextsPagesAdapter = new ExtractTextsPagesAdapter(context, pdfPages);
            int spanCount = Utils.isTablet(context) ? 6 : 3;
            recyclerView.setLayoutManager(new GridLayoutManager(context, spanCount, GridLayoutManager.VERTICAL, false));
            progressBarLoadPagePreview.setVisibility(View.GONE);
            recyclerView.setAdapter(extractTextsPagesAdapter);
            btnSave.setVisibility(View.VISIBLE);
        }
    }

    public class ExtractText extends AsyncTask<Void, Integer, Void> {

        Context mContext;
        String pdfPath;
        String extractedTextFilePath;
        String extractedTextDir;
        String errorMessage;
        boolean textExtractSuccess = true;
        List<Integer> selectedPages;
        int mNumPages;

        public ExtractText(Context context, String pdfPath, List<Integer> selectedPages, ConstraintLayout progressView) {
            mContext = context;
            this.pdfPath = pdfPath;
            this.selectedPages = selectedPages;
            mNumPages = selectedPages.size();
            mProgressView = progressView;
            initializeProgressView();
            Utils.setLightStatusBar(context);

            btnCancelProgress.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ExtractText.this.cancel(true); //Stop splitting
                    closeProgressView(mContext);
                }
            });
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setIndeterminate(true);
            progressBar.setMax(mNumPages);
            currentAction.setText(R.string.extracting);
            mProgressView.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... voids) {

            extractedTextDir = allPdfDocuments + "Texts/";
            File folder = new File(extractedTextDir);

            // Create directory if it does't exist
            if (!folder.exists())
                folder.mkdirs();

            try {
                File mFile = new File(pdfPath);
                String fileName = mFile.getName();
                extractedTextFilePath = extractedTextDir + Utils.removeExtension(fileName) + ".txt";

                PDFBoxResourceLoader.init(mContext);
                PDDocument document = PDDocument.load(mFile);

                if (!document.isEncrypted()) {
                    PDFTextStripper textStripper = new PDFTextStripper();
                    StringBuilder text = new StringBuilder();
                    String pageEnd = textStripper.getPageEnd();

                    removeProgressBarIndeterminate(mContext, progressBar);

                    for (int i = 0; i < mNumPages & !isCancelled(); i++) {
                        textStripper.setStartPage(i + 1);
                        textStripper.setEndPage(i + 1);
                        text.append(textStripper.getText(document) + pageEnd);

                        publishProgress(i + 1);
                    }

                    FileOutputStream stream = new FileOutputStream(new File(extractedTextFilePath));
                    stream.write(text.toString().getBytes());

                    document.close();
                    stream.close();
                } else {
                    errorMessage = mContext.getString(R.string.file_protected_unprotect);
                    textExtractSuccess = false;
                }

            } catch (Exception e) {
                e.printStackTrace();
                errorMessage = mContext.getString(R.string.extraction_failed);
                textExtractSuccess = false;
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            updateProgressPercent(values[0], mNumPages);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            currentAction.setText(R.string.done);
            btnCancelProgress.setOnClickListener(null);
            showInterstialAd(mContext, extractedTextDir);
            setupOpenPath(mContext, mContext.getString(R.string.open_file), extractedTextFilePath);

            if (!textExtractSuccess)
                Toast.makeText(mContext, errorMessage, Toast.LENGTH_LONG).show();
        }
    }


    private void initializeProgressView() {
        percent = mProgressView.findViewById(R.id.percent);
        currentAction = mProgressView.findViewById(R.id.current_action);
        progressBar = mProgressView.findViewById(R.id.progress_bar);
        savedPath = mProgressView.findViewById(R.id.saved_path);
        successIcon = mProgressView.findViewById(R.id.success_icon);
        btnOpenFile = mProgressView.findViewById(R.id.open_file);
        btnCancelProgress = mProgressView.findViewById(R.id.cancel_progress);
        closeProgressView = mProgressView.findViewById(R.id.close_progress_view);
    }

    public void processingFinished(Context context, String splitPath) {
        percent.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.INVISIBLE);
        successIcon.setVisibility(View.VISIBLE);
        closeProgressView.setVisibility(View.VISIBLE);
        btnOpenFile.setVisibility(View.VISIBLE);

        btnCancelProgress.setVisibility(View.GONE);

        String splittedFilePaths = context.getString(R.string.saved_to) + " " + splitPath;
        savedPath.setText(splittedFilePaths);
    }

    public void closeProgressView(Context context) {
        mProgressView.setVisibility(View.GONE);
        successIcon.setVisibility(View.GONE);
        btnOpenFile.setVisibility(View.GONE);
        closeProgressView.setVisibility(View.GONE);

        progressBar.setVisibility(View.VISIBLE);
        percent.setVisibility(View.VISIBLE);
        btnCancelProgress.setVisibility(View.VISIBLE);

        progressBar.setProgress(0);
        percent.setText("0%");
        savedPath.setText("");
        Utils.clearLightStatusBar(context);
    }


    public void closeProgressView(View view) {
        progressView.setVisibility(View.GONE);
        progressView.findViewById(R.id.success_icon).setVisibility(View.GONE);
        progressView.findViewById(R.id.open_file).setVisibility(View.GONE);
        progressView.findViewById(R.id.close_progress_view).setVisibility(View.GONE);
        progressView.findViewById(R.id.progress_bar).setVisibility(View.VISIBLE);
        progressView.findViewById(R.id.percent).setVisibility(View.VISIBLE);
        progressView.findViewById(R.id.cancel_progress).setVisibility(View.VISIBLE);

        ProgressBar progressBar = progressView.findViewById(R.id.progress_bar);
        TextView textView = progressView.findViewById(R.id.saved_path);
        TextView progressPercent = progressView.findViewById(R.id.percent);
        progressBar.setProgress(0);
        progressPercent.setText("0%");
        textView.setText("");
        Utils.clearLightStatusBar(this);
    }


    public void updateProgressPercent(int curProgress, int totalProgress) {
        int percentNo = (int) (curProgress * 100F) / totalProgress;
        String percentText = percentNo + "%";
        percent.setText(percentText);
        progressBar.setProgress(curProgress);
    }

    public void showInterstialAd(final Context context, final String savedToPath) {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {

                final InterstitialAd ad = AdManager.getAd();
                if (ad != null) {
                    ad.setAdListener(new AdListener() {

                        @Override
                        public void onAdClosed() {
                            super.onAdClosed();

                            //Pre Load ADs
                            AdManager adManager = new AdManager((AppCompatActivity) context,
                                    "ca-app-pub-6949253770172194/8133331389");
                            adManager.createAd();

                            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    processingFinished(context, savedToPath);
                                    Snackbar.make(closeProgressView, R.string.dont_like_ads, 4000)
                                            .setAction(R.string.remove, new View.OnClickListener() {
                                                @Override
                                                public void onClick(View view) {
                                                    Utils.openProVersionPlayStore(context);
                                                }
                                            }).show();
                                }
                            }, 800);
                        }
                    });

                    ad.show();
                } else {
                    processingFinished(context, savedToPath);
                }
            }
        }, 1000);
    }

    public void setupOpenPath(final Context context, final String buttonText, final String filePath) {
        btnOpenFile.setText(buttonText);

        btnOpenFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                File txtFile = new File(filePath);
                Uri uri = FileProvider.getUriForFile(context, "tz.co.wadau.documentscanner.fileprovider", txtFile);
                Intent txtIntent = ShareCompat.IntentBuilder.from((Activity) context)
                        .setType(context.getContentResolver().getType(uri))
                        .setStream(uri)
                        .getIntent();

                txtIntent.setData(uri);
                txtIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                txtIntent.setAction(Intent.ACTION_VIEW);

                txtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                if (txtIntent.resolveActivity(context.getPackageManager()) != null) {
                    context.startActivity(txtIntent);
                } else {
                    Toast.makeText(context, R.string.no_proper_app_for_opening_text, Toast.LENGTH_SHORT).show();
                }
            }
        });
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
                            mEditor.putBoolean(ORGANIZE_PAGES_TIP, false);
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

    public void removeProgressBarIndeterminate(Context context, final ProgressBar progressBar) {
        ((Activity) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setIndeterminate(false);
            }
        });
    }
}
