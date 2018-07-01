package tz.co.wadau.documentscanner;

import android.animation.Animator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.InterstitialAd;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import tz.co.wadau.documentscanner.adapters.OrganizeImagesAdapter;
import tz.co.wadau.documentscanner.models.ImagePage;
import tz.co.wadau.documentscanner.utils.AdManager;
import tz.co.wadau.documentscanner.utils.Utils;

import static tz.co.wadau.documentscanner.BrowsePDFActivity.GRID_VIEW_ENABLED;
import static tz.co.wadau.documentscanner.BrowsePDFActivity.PDF_LOCATION;
import static tz.co.wadau.documentscanner.PDFToolsActivity.IS_DIRECTORY;

public class OrganizeImagesActivity extends AppCompatActivity {

    final String TAG = OrganizeImagesActivity.class.getSimpleName();
    private RecyclerView recyclerView;
    private Context context;
    private ProgressBar progressBar;
    private ProgressBar progressBarLoadPagePreview;
    private String allPdfDocuments;
    private ArrayList<String> imageUris;
    private List<ImagePage> imagePages = new ArrayList<>();
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
    public static final String IMAGE_URIS = "tz.co.wadau.documentscanner.IMAGE_URIS";
    private OrganizeImagesAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organize_images);

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
        imageUris = intent.getStringArrayListExtra(IMAGE_URIS);

        if (showOrganizePagesTip) {
            infoTapMoreOptions.setVisibility(View.VISIBLE);
        } else {
            infoTapMoreOptions.setVisibility(View.GONE);
        }

        new LoadPageThumbnails(imageUris).execute();

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (imagePages.size() >= 1) {
                    showFileNameDialog();
                } else {
                    Toast.makeText(context, R.string.select_at_least_one_image, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public class LoadPageThumbnails extends AsyncTask<Void, Void, Void> {

        public LoadPageThumbnails(ArrayList<String> imageUri) {
            imageUris = imageUri;
        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {

            for (int i = 0; i < imageUris.size(); i++) {
                imagePages.add(new ImagePage(i + 1, Uri.parse(imageUris.get(i))));
            }

            adapter = new OrganizeImagesAdapter(context, imagePages);
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            int spanCount = Utils.isTablet(context) ? 6 : 3;
            recyclerView.setLayoutManager(new GridLayoutManager(context, spanCount, GridLayoutManager.VERTICAL, false));
            progressBarLoadPagePreview.setVisibility(View.GONE);
            recyclerView.setAdapter(adapter);
            btnSave.setVisibility(View.VISIBLE);

            int swipDirs = ItemTouchHelper.UP | ItemTouchHelper.DOWN |
                    ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;

            ItemTouchHelper touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(swipDirs, 0) {
                @Override
                public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                    int fromPos = viewHolder.getAdapterPosition();
                    int toPos = target.getAdapterPosition();

                    ImagePage dPage = imagePages.remove(toPos);
                    imagePages.add(fromPos, dPage);
                    adapter.notifyItemMoved(toPos, fromPos);
                    Log.d(TAG, "moved from " + fromPos + " to position " + toPos);
                    return true;
                }


                @Override
                public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

                }

                @Override
                public void clearView(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                    super.clearView(recyclerView, viewHolder);
                    Log.d(TAG, "Page order after swap " + getOrganizedPages(imagePages).toString());
                }
            });

            touchHelper.attachToRecyclerView(recyclerView);

        }
    }

    public class SaveOrganizedPages extends AsyncTask<Void, Integer, Void> {
        private List<Integer> organizedPages;
        private int numPages;
        private String generatedPDFPath;
        private String newFileName;

        public SaveOrganizedPages(List<Integer> pages, String fileName, ConstraintLayout progressView) {
            this.organizedPages = pages;
            this.newFileName = fileName;
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
            currentAction.setText(R.string.converting);
            mProgressView.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            boolean isGridViewEnabled = sharedPreferences.getBoolean(GRID_VIEW_ENABLED, false);

            try {
                File folder = new File(allPdfDocuments);
                generatedPDFPath = allPdfDocuments + newFileName + ".pdf";

                if (!folder.exists())
                    folder.mkdirs();

                PDFBoxResourceLoader.init(context);
                PDDocument document = new PDDocument();

                for (int i = 0; i < numPages && !isCancelled(); i++) {

                    String imagePath = imagePages.get(i).getImageUri().getPath();
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                    Bitmap bitmap = BitmapFactory.decodeFile(imagePath, options);

                    ExifInterface exifInterface = new ExifInterface(imagePath);
                    int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

                    bitmap = rotateBitmap(bitmap, orientation); //Rotate bitmap incase the image has rotation info.
                    float imgWidth = bitmap.getWidth();
                    float imgHeight = bitmap.getHeight();

                    PDPage page = new PDPage(new PDRectangle(imgWidth, imgHeight));
                    document.addPage(page);

                    PDImageXObject imageXObject = JPEGFactory.createFromImage(document, bitmap);
                    PDPageContentStream contentStream = new PDPageContentStream(document, page, true, true, true);
                    contentStream.drawImage(imageXObject, 0, 0, imgWidth, imgHeight);
                    contentStream.close();

                    publishProgress(i + 1);
                }

                document.save(generatedPDFPath);
                document.close();


                if (isGridViewEnabled)
                    Utils.generatePDFThumbnail(context, generatedPDFPath);

                MediaScannerConnection.scanFile(context, new String[]{generatedPDFPath}, new String[]{"application/pdf"}, null);
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
            setupOpenPath(context, context.getString(R.string.open_file), generatedPDFPath, true);
        }
    }

    public Bitmap rotateBitmap(Bitmap bitmap, int orientation) {

        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_NORMAL:
                return bitmap;
            case ExifInterface.ORIENTATION_FLIP_HORIZONTAL:
                matrix.setScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.setRotate(180);
                break;
            case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                matrix.setRotate(180);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_TRANSPOSE:
                matrix.setRotate(90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.setRotate(90);
                break;
            case ExifInterface.ORIENTATION_TRANSVERSE:
                matrix.setRotate(-90);
                matrix.postScale(-1, 1);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.setRotate(-90);
                break;
            default:
                return bitmap;
        }
        try {
            Bitmap bmRotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            bitmap.recycle();
            return bmRotated;
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<Integer> getOrganizedPages(List<ImagePage> imagePages) {
        List<Integer> pagesToKeep = new ArrayList<>();
        for (int i = 0; i < imagePages.size(); i++) {
            pagesToKeep.add(imagePages.get(i).getPageNumber());
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
                    //Open file brawser to the specified folder
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

    public void showFileNameDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        final String fileName = "Image_PDF_" + System.currentTimeMillis();

        float dpi = context.getResources().getDisplayMetrics().density;
        final EditText editText = new EditText(context);
        editText.setText(fileName);
        editText.setSelectAllOnFocus(true);

        builder.setTitle(R.string.enter_file_name)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null);

        final AlertDialog alertDialog = builder.create();

        alertDialog.setView(editText, (int) (24 * dpi), (int) (8 * dpi), (int) (24 * dpi), (int) (5 * dpi));
        alertDialog.show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String newFileName = editText.getText().toString();

                if (Utils.isFileNameValid(newFileName)) {
                    alertDialog.dismiss();
                    new SaveOrganizedPages(getOrganizedPages(imagePages), newFileName, progressView).execute();
                } else {
                    editText.setError(getString(R.string.invalid_file_name));
                }
            }
        });
    }
}
