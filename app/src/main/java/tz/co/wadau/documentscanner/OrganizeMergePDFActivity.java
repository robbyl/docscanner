package tz.co.wadau.documentscanner;

import android.animation.Animator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import tz.co.wadau.documentscanner.adapters.OrganizeMergePDFAdapter;
import tz.co.wadau.documentscanner.utils.PDFTools;
import tz.co.wadau.documentscanner.utils.Utils;

import static tz.co.wadau.documentscanner.PDFToolsActivity.PDF_PATHS;

public class OrganizeMergePDFActivity extends AppCompatActivity {

    final String TAG = OrganizeMergePDFActivity.class.getSimpleName();
    private RecyclerView recyclerView;
    private Context context;
    private ProgressBar progressBar;
    private String allPdfPictureDir;
    private String allPdfDocuments;
    private List<String> pdfFilePaths;
    private List<File> mPDFFiles = new ArrayList<>();
    private OrganizeMergePDFAdapter organizePagesAdapter;
    private ConstraintLayout progressView;
    private RelativeLayout infoTapMoreOptions;
    private AppCompatImageView closeInfo;
    private SharedPreferences sharedPreferences;
    public static String ORGANIZE_MERGE_PAGES_TIP = "prefs_organize_merge_pages";
    boolean showOrganizePagesTip;
    FloatingActionButton btnMerge;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_organize_merge_pdf);

        String extDir = Environment.getExternalStorageDirectory().toString();
        allPdfPictureDir = extDir + "/Pictures/AllPdf/tmp/";
        allPdfDocuments = extDir + "/Documents/AllPdf/";
        Toolbar toolbar = findViewById(R.id.toolbar_organize_pages);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        this.context = this;
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        showOrganizePagesTip = sharedPreferences.getBoolean(ORGANIZE_MERGE_PAGES_TIP, true);
        recyclerView = findViewById(R.id.recycler_view_organize_pages);
        progressBar = findViewById(R.id.progress_bar_organize_pages);
        progressView = findViewById(R.id.progress_view);
        btnMerge = findViewById(R.id.fab_save);
        infoTapMoreOptions = findViewById(R.id.info_tap_more_options);
        closeInfo = findViewById(R.id.info_close);
        closeInfo.setOnClickListener(closeMoreInfo);

        Intent intent = getIntent();
        pdfFilePaths = intent.getStringArrayListExtra(PDF_PATHS);

        if (showOrganizePagesTip) {
            infoTapMoreOptions.setVisibility(View.VISIBLE);
        } else {
            infoTapMoreOptions.setVisibility(View.GONE);
        }

        new LoadPageThumbnails().execute(pdfFilePaths);

        btnMerge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                organizePagesAdapter.finishActionMode();

                if (organizePagesAdapter.getPDFsToMerge().size() >= 2) {
                    mergePDFFiles(organizePagesAdapter.getPDFsToMerge());
                } else {
                    Toast.makeText(context, R.string.at_least_two_files, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onBackPressed() {

        if (progressView.findViewById(R.id.close_progress_view).getVisibility() == View.VISIBLE)
            closeProgressView(progressView);
        else if (progressView.getVisibility() == View.VISIBLE) {
            //Do nothing
        } else super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Utils.deleteFiles(allPdfPictureDir);
        Log.d(TAG, "Deleting temp dir " + allPdfPictureDir);
    }

    public class LoadPageThumbnails extends AsyncTask<List<String>, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(List<String>... strings) {

            List<String> mPDFPaths = strings[0];
            int fileNum = mPDFPaths.size();

            for (int i = 0; i < fileNum; i++) {
                String mPDFPath = mPDFPaths.get(i);

                if (!Utils.isThumbnailPresent(context, mPDFPath)) {
                    Utils.generatePDFThumbnail(context, mPDFPath);
                }

                mPDFFiles.add(new File(mPDFPath));
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            organizePagesAdapter = new OrganizeMergePDFAdapter(context, mPDFFiles);
            int spanCount = Utils.isTablet(context) ? 6 : 3;
            recyclerView.setLayoutManager(new GridLayoutManager(context, spanCount, GridLayoutManager.VERTICAL, false));
            progressBar.setVisibility(View.GONE);
            recyclerView.setAdapter(organizePagesAdapter);
            btnMerge.setVisibility(View.VISIBLE);

            int swipDirs = ItemTouchHelper.UP | ItemTouchHelper.DOWN |
                    ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT;

            ItemTouchHelper touchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(swipDirs, 0) {
                @Override
                public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                    int fromPos = viewHolder.getAdapterPosition();
                    int toPos = target.getAdapterPosition();
                    File file = mPDFFiles.remove(toPos);
                    mPDFFiles.add(fromPos, file);
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
                }
            });

            touchHelper.attachToRecyclerView(recyclerView);
        }
    }

    public void mergePDFFiles(List<File> filesToMerge) {

        final ArrayList<String> fileToMergePaths = new ArrayList<>();

        for (File file : filesToMerge) {
            fileToMergePaths.add(file.getAbsolutePath());
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        final String fileName = "Merged" + System.currentTimeMillis();

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
                    PDFTools pdfTools = new PDFTools();
                    pdfTools.new MergePDFFiles(context, fileToMergePaths, newFileName, progressView).execute();
                } else {
                    editText.setError(getString(R.string.invalid_file_name));
                }
            }
        });
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
                            mEditor.putBoolean(ORGANIZE_MERGE_PAGES_TIP, false);
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
