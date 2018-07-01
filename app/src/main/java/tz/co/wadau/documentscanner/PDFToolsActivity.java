package tz.co.wadau.documentscanner;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.AppCompatTextView;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import tz.co.wadau.documentscanner.adapters.ToolsAdapter;
import tz.co.wadau.documentscanner.data.ToolsData;
import tz.co.wadau.documentscanner.models.Tool;
import tz.co.wadau.documentscanner.utils.AdManager;
import tz.co.wadau.documentscanner.utils.PDFTools;
import tz.co.wadau.documentscanner.utils.Utils;

public class PDFToolsActivity extends AppCompatActivity
        implements ToolsAdapter.OnToolClickListener {

    private final String TAG = PDFToolsActivity.class.getSimpleName();
    private final int SELECT_PDF_REQUEST_CODE = 4842; //Abritary number
    public static final String MULTI_SELECTION = "tz.co.wadau.documentscanner.MULTI_SELECTION";
    public static final String IS_DIRECTORY = "tz.co.wadau.documentscanner.IS_DIRECTORY";
    public static final String DIRECTORY_PATH = "tz.co.wadau.documentscanner.DIRECTORY_PATH";
    public static final String PDF_PATHS = "tz.co.wadau.documentscanner.PDF_PATHS";
    public static final String PDF_PATH = "tz.co.wadau.documentscanner.PDF_PATH";
    private int toolPosition;
    private Context context;
    private ConstraintLayout progressView;

    public static final String ADMOB_APP_ID = "ca-app-pub-6949253770172194~4483174067";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdftools);

        Toolbar toolbar = findViewById(R.id.toolbar_tools);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        context = this;

        RecyclerView toolsRecyclerview = findViewById(R.id.tools_recyclerview);
        progressView = findViewById(R.id.progress_view);

        GridLayoutManager layoutManager = new GridLayoutManager(this, 3, GridLayoutManager.VERTICAL, false);

        List<Tool> tools = ToolsData.getTools();
        ToolsAdapter toolsAdapter = new ToolsAdapter(this, tools);
        toolsRecyclerview.setLayoutManager(layoutManager);
        toolsRecyclerview.setAdapter(toolsAdapter);

        new ClearTempFolderContents().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);

        //Pre Load ADs
        AdManager adManager = new AdManager(this, "ca-app-pub-6949253770172194/8133331389");
        adManager.createAd();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SELECT_PDF_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {

                ArrayList<String> pdfPaths = data.getStringArrayListExtra(PDF_PATHS);
                PDFTools pdfTools = new PDFTools();

                switch (toolPosition) {

                    case 0:
//                        mergePDFFiles(pdfPaths);
                        Log.d(TAG, "Just called but can't do anything");
                        break;
                    case 1:
                        new LoadSplittingOptions(pdfPaths.get(0)).execute();
                        break;
                    case 2:
                        showImageQualityDialog(pdfPaths.get(0));
                        break;
                    case 3:
                        pdfTools.new ConvertPDFAsPictures(this, pdfPaths.get(0), progressView).execute();
                        break;
                    case 4:
                        showOrganizePages(pdfPaths.get(0));
                        break;
                    case 5:
                        showDocumentMetadata(pdfPaths.get(0));
                        break;
                    case 6:
                        showCompressionOptions(pdfPaths.get(0));
                        break;
                    case 7:
                        showExtractTextsPages(pdfPaths.get(0));
                        break;
                    default:
                        Toast.makeText(this, "Clicked", Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        }
    }

    @Override
    public void onBackPressed() {

        if (progressView.findViewById(R.id.close_progress_view).getVisibility() == View.VISIBLE)
            closeProgressView(progressView);
        else if (progressView.getVisibility() == View.VISIBLE) {
            //Do nothing
        } else super.onBackPressed();
    }


    public void showDocumentMetadata(String filePath) {
        Intent intent = new Intent(this, EditMetadataActivity.class);
        intent.putExtra(PDF_PATH, filePath);
        startActivity(intent);
    }

    public void showOrganizePages(String filePath) {
        Intent intent = new Intent(this, OrganizePagesActivity.class);
        intent.putExtra(PDF_PATH, filePath);
        startActivity(intent);
    }

    public void showExtractTextsPages(String filePath) {
        Intent intent = new Intent(this, ExtractTextsPagesActivity.class);
        intent.putExtra(PDF_PATH, filePath);
        startActivity(intent);
    }

    public void showSelectImages() {

    }

    public class ClearTempFolderContents extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... voids) {
            String allPdfPictureDir = Environment.getExternalStorageDirectory() + "/Pictures/AllPdf/tmp/";
            File dir = new File(allPdfPictureDir);
            if (dir.exists()) {
                Log.d(TAG, "Start clearing temp folder");
                Utils.deleteFiles(allPdfPictureDir);

            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            Log.d(TAG, "Cleared temp folder");
        }
    }

    @Override
    public void onToolClicked(int position) {
        toolPosition = position;

        switch (position) {
            case 8:
                Intent intent = new Intent(this, SelectImagesActivity.class);
                startActivityForResult(intent, SELECT_PDF_REQUEST_CODE);
                break;
            case 9:
            case 10:
            case 11:
                Utils.showPremiumFeatureDialog(this);
                break;
            default:
                Intent mIntent = new Intent(this, SelectPDFActivity.class);
                if (position == 0)
                    mIntent.putExtra(MULTI_SELECTION, true);
                startActivityForResult(mIntent, SELECT_PDF_REQUEST_CODE);
                break;
        }
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
        TextView description = progressView.findViewById(R.id.description);
        description.setVisibility(View.GONE);

        progressBar.setProgress(0);
        progressPercent.setText("0%");
        textView.setText("");
        description.setText("");
        Utils.clearLightStatusBar(this);
    }


    public void showCompressionLevelDialog(final String PdfPath) {
        final String CHECKED_COMPRESSION_QUALITY = "prefs_checked_compression_quality";
        final int[] compressionQuality = {50}; // default compression level
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        int checkedItem = sharedPreferences.getInt(CHECKED_COMPRESSION_QUALITY, 1);

        builder.setTitle(R.string.compression_level)
                .setSingleChoiceItems(R.array.compression_level, checkedItem, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        editor.putInt(CHECKED_COMPRESSION_QUALITY, i);
                        switch (i) {
                            case 0:
                                compressionQuality[0] = 70; //Low compression level (low reduced file size)
                                break;
                            case 1:
                                compressionQuality[0] = 50; // Medium compression level (medium reduced file size)
                                break;
                            case 2:
                                compressionQuality[0] = 20; // High compression level (Very high reduced file size)
                                break;
                        }
                    }
                })
                .setPositiveButton(R.string.compress, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        editor.apply();
                        PDFTools mPdfTools = new PDFTools();
                        mPdfTools.new CompressPDF(context, PdfPath, compressionQuality[0], progressView).execute();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public void showSplittingOptionsDialog(final String pdfPath, final int numPages) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.split_pdf)
                .setView(getLayoutInflater().inflate(R.layout.dialog_split_options, null))
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null);

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        AppCompatSpinner spinner = alertDialog.findViewById(R.id.spinner_splitting_options);
        final AppCompatEditText from = alertDialog.findViewById(R.id.edit_from);
        final AppCompatEditText to = alertDialog.findViewById(R.id.edit_to);
        AppCompatTextView editTextNumPages = alertDialog.findViewById(R.id.num_pages);
        String strNumPages = getString(R.string.number_of_pages) + " " + numPages;
        editTextNumPages.setText(strNumPages);
        final int[] selectedOption = {0};

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                switch (i) {
                    case 0:
                        selectedOption[0] = 0;
                        from.setVisibility(View.INVISIBLE);
                        to.setVisibility(View.INVISIBLE);
                        from.setText("");
                        to.setText("");
                        break;
                    case 1:
                        selectedOption[0] = 1;
                        from.setVisibility(View.VISIBLE);
                        to.setVisibility(View.INVISIBLE);
                        from.setHint(R.string.at);
                        from.setText("");
                        to.setText("");
                        break;
                    case 2:
                        selectedOption[0] = 2;
                        from.setVisibility(View.VISIBLE);
                        to.setVisibility(View.VISIBLE);
                        from.setHint(R.string.from);
                        from.setText("");
                        to.setText("");
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PDFTools pdfTools = new PDFTools();

                switch (selectedOption[0]) {
                    case 0:
                        //Split all pages
                        pdfTools.new SplitPDF(context, pdfPath, progressView).execute();
                        alertDialog.cancel();
                        break;
                    case 1:
                        //Split a specified page
                        int splitAt = TextUtils.isEmpty(from.getText().toString()) ? 0 : Integer.valueOf(from.getText().toString());
                        if (splitAt > 0 && splitAt <= numPages) {
                            pdfTools.new SplitPDF(context, pdfPath, progressView, splitAt).execute();
                            alertDialog.cancel();
                        } else {
                            from.setError(getString(R.string.invalid_value));
                        }
                        break;
                    case 2:
                        //Split at specified range
                        int splitFrom = TextUtils.isEmpty(from.getText().toString()) ? 0 : Integer.valueOf(from.getText().toString());
                        int splitTo = TextUtils.isEmpty(to.getText().toString()) ? 0 : Integer.valueOf(to.getText().toString());

                        if (splitFrom <= 0 || splitFrom > numPages) {
                            from.setError(getString(R.string.invalid_value));
                        } else if ((splitTo <= 0 || splitTo > numPages) || splitTo <= splitFrom) {
                            to.setError(getString(R.string.invalid_value));
                        } else {
                            pdfTools.new SplitPDF(context, pdfPath, progressView, splitFrom, splitTo).execute();
                            alertDialog.cancel();
                        }

                        break;
                }
            }
        });
    }

    public class LoadSplittingOptions extends AsyncTask<Void, Void, Void> {

        String mPdfPath;
        int numPages;
        ProgressDialog progressDialog;

        public LoadSplittingOptions(String pdfPath) {
            mPdfPath = pdfPath;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = new ProgressDialog(context);
            progressDialog.setMessage(getString(R.string.loading_wait));
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            PdfiumCore pdfiumCore = new PdfiumCore(context);
            Uri fileUri = Uri.fromFile(new File(mPdfPath));

            try {
                ParcelFileDescriptor fd = context.getContentResolver().openFileDescriptor(fileUri, "r");
                PdfDocument document = pdfiumCore.newDocument(fd);
                numPages = pdfiumCore.getPageCount(document);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progressDialog.dismiss();
            showSplittingOptionsDialog(mPdfPath, numPages);
        }
    }


    public void showImageQualityDialog(final String PdfPath) {
        final String CHECKED_IMG_QUALITY = "prefs_checked_img_quality";
        final int[] compressionQuality = {50}; // default compression level
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        final SharedPreferences.Editor editor = sharedPreferences.edit();
        int checkedItem = sharedPreferences.getInt(CHECKED_IMG_QUALITY, 1);

        builder.setTitle(R.string.image_quality)
                .setSingleChoiceItems(R.array.compression_level, checkedItem, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        editor.putInt(CHECKED_IMG_QUALITY, i);

                        switch (i) {
                            case 0:
                                compressionQuality[0] = 30; //Low image quality
                                break;
                            case 1:
                                compressionQuality[0] = 65; // Medium image quality
                                break;
                            case 2:
                                compressionQuality[0] = 100; // High image quality
                                break;
                        }
                    }
                }).setPositiveButton(R.string.extract, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                //Store last user selected quality so it can used as a default the next time this dialog is opened
                editor.apply();

                PDFTools mPdfTools = new PDFTools();
                mPdfTools.new ExtractImages(context, PdfPath, compressionQuality[0], progressView).execute();
            }
        })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public void showCompressionOptions(String pdfPath) {
        ActivityManager.MemoryInfo memoryInfo = Utils.getAvailableMemory(this);
        if (!memoryInfo.lowMemory) {
            showCompressionLevelDialog(pdfPath);
        } else {
            Toast.makeText(this, R.string.cant_compress_low_memory, Toast.LENGTH_LONG).show();
        }
    }
}
