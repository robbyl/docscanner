package tz.co.wadau.documentscanner;

import android.animation.Animator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
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
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
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
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import tz.co.wadau.documentscanner.adapters.OrganizePagesAdapter;
import tz.co.wadau.documentscanner.models.PDFPage;
import tz.co.wadau.documentscanner.utils.AdManager;
import tz.co.wadau.documentscanner.utils.Utils;

import static tz.co.wadau.documentscanner.BrowsePDFActivity.GRID_VIEW_ENABLED;
import static tz.co.wadau.documentscanner.BrowsePDFActivity.PDF_LOCATION;
import static tz.co.wadau.documentscanner.PDFToolsActivity.IS_DIRECTORY;
import static tz.co.wadau.documentscanner.PDFToolsActivity.PDF_PATH;

public class OrganizePagesActivity extends AppCompatActivity {

    final String TAG = OrganizePagesActivity.class.getSimpleName();
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
    FloatingActionButton btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organize_pages);

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
                new SaveOrganizedPages(getOrganizedPages(pdfPages), progressView).execute();
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
        OrganizePagesAdapter organizePagesAdapter;
        //        List<Bitmap> bitmaps = new ArrayList<>();


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
            organizePagesAdapter = new OrganizePagesAdapter(context, pdfPages);
            int spanCount = Utils.isTablet(context) ? 6 : 3;
            recyclerView.setLayoutManager(new GridLayoutManager(context, spanCount, GridLayoutManager.VERTICAL, false));
            progressBarLoadPagePreview.setVisibility(View.GONE);
            recyclerView.setAdapter(organizePagesAdapter);
            btnSave.setVisibility(View.VISIBLE);

            int swipeDirs = ItemTouchHelper.UP | ItemTouchHelper.DOWN |
                    ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;

            ItemTouchHelper touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(swipeDirs, 0) {
                @Override
                public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                    int fromPos = viewHolder.getAdapterPosition();
                    int toPos = target.getAdapterPosition();

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

    public class SaveOrganizedPages extends AsyncTask<Void, Integer, Void> {
        private List<Integer> organizedPages;
        private int numPages;
        String organizedFilePath;

        public SaveOrganizedPages(List<Integer> pages, ConstraintLayout progressView) {
            this.organizedPages = pages;
            numPages = this.organizedPages.size();
            mProgressView = progressView;
            initializeProgressView();
            Utils.setLightStatusBar(context);

            btnCancelProgress.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    SaveOrganizedPages.this.cancel(true); //Stop splitting
                    closeProgressView(context);
                }
            });
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setMax(numPages);
            currentAction.setText(R.string.organizing);
            mProgressView.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            boolean isGridViewEnabled = sharedPreferences.getBoolean(GRID_VIEW_ENABLED, false);

            try {
                File folder = new File(allPdfDocuments);
                File mfile = new File(pdfFilePath);
                String mFileName = mfile.getName();
                organizedFilePath = allPdfDocuments + Utils.removeExtension(mFileName) + "-Organized.pdf";

                if (!folder.exists())
                    folder.mkdirs();

                PDFBoxResourceLoader.init(context);
                PDDocument document = PDDocument.load(mfile);
                PDDocument organizedDocument = new PDDocument();

                for (int i = 0; i < numPages && !isCancelled(); i++) {
                    Log.d(TAG, "Get page at from pdf " + (organizedPages.get(i) - 1));
                    PDPage pdfPage = document.getPage(organizedPages.get(i) - 1); //Since getPage starts at index 0
                    organizedDocument.addPage(pdfPage);
                    publishProgress(i + 1);
                }

                organizedDocument.save(new File(organizedFilePath));
                document.close();
                organizedDocument.close();

                if (isGridViewEnabled)
                    Utils.generatePDFThumbnail(context, organizedFilePath);

                MediaScannerConnection.scanFile(context, new String[]{organizedFilePath}, new String[]{"application/pdf"}, null);
                Log.d(TAG, "Page order" + organizedPages);

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            updateProgressPercent(values[0], numPages);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            currentAction.setText(R.string.done);
            btnCancelProgress.setOnClickListener(null);
            showInterstialAd(context, allPdfDocuments);
            setupOpenPath(context, context.getString(R.string.open_file), organizedFilePath, true);
        }
    }

    public List<Integer> getOrganizedPages(List<PDFPage> pdfPages) {
        List<Integer> pagesToKeep = new ArrayList<>();
        for (int i = 0; i < pdfPages.size(); i++) {
            pagesToKeep.add(pdfPages.get(i).getPageNumber());
        }
        return pagesToKeep;
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

    public void setupOpenPath(final Context context, final String buttonText, final String filePath, final boolean isFile) {
        btnOpenFile.setText(buttonText);

        btnOpenFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (isFile) {
                    //Opening pdf file in pdf viewer
                    File pdf = new File(filePath);
                    Intent pdfViewerIntent = new Intent(context, PDFViewerActivity.class);
                    pdfViewerIntent.putExtra(PDF_LOCATION, pdf.getAbsolutePath());
                    Log.d(TAG, "Open PDF from location " + pdf.getAbsolutePath());
                    context.startActivity(pdfViewerIntent);
                } else {
                    //Open file browser to the specified folder
                    Intent intent = new Intent(context, SelectPDFActivity.class);
                    intent.putExtra(IS_DIRECTORY, true);
                    intent = intent.putExtra(PDFToolsActivity.DIRECTORY_PATH, filePath);
                    context.startActivity(intent);
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
}
