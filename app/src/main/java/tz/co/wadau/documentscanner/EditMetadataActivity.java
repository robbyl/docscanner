package tz.co.wadau.documentscanner;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDDocumentInformation;
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader;

import java.io.File;
import java.io.IOException;

import tz.co.wadau.documentscanner.utils.Utils;

import static tz.co.wadau.documentscanner.PDFToolsActivity.PDF_PATH;

public class EditMetadataActivity extends AppCompatActivity {

    private final String TAG = EditMetadataActivity.class.getSimpleName();

    AppCompatEditText title;
    AppCompatEditText author;
    AppCompatEditText creator;
    AppCompatEditText producer;
    AppCompatEditText subject;
    AppCompatEditText keywords;
    AppCompatEditText createdDate;
    AppCompatEditText modifiedDate;
    String pdfPath;
    Context mContext;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_metadata);

        if (Utils.isTablet(this)) {
            Utils.setLightStatusBar(this);
        } else {
            Utils.clearLightStatusBar(this);
        }

        Toolbar toolbar = findViewById(R.id.toolbar_edit_metadata);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        mContext = this;
        title = findViewById(R.id.edit_text_title);
        author = findViewById(R.id.edit_text_author);
        creator = findViewById(R.id.edit_text_creator);
        producer = findViewById(R.id.edit_text_producer);
        subject = findViewById(R.id.edit_text_subject);
        keywords = findViewById(R.id.edit_text_keywords);
        createdDate = findViewById(R.id.created_date);
        modifiedDate = findViewById(R.id.modified_date);

        Intent intent = getIntent();
        pdfPath = intent.getStringExtra(PDF_PATH);

        new LoadMetadata().execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_edit_metadata, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                new SaveMetadata().execute();
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    public class LoadMetadata extends AsyncTask<Void, Void, Void> {
        ProgressDialog progressDialog;
        PdfDocument.Meta meta;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(mContext);
            progressDialog.setMessage(getString(R.string.loading_wait));
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {

            try {
                PdfiumCore pdfiumCore = new PdfiumCore(mContext);
                Uri fileUri = Uri.fromFile(new File(pdfPath));
                ParcelFileDescriptor fd = mContext.getContentResolver().openFileDescriptor(fileUri, "r");
                PdfDocument document = pdfiumCore.newDocument(fd);
                meta = pdfiumCore.getDocumentMeta(document);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progressDialog.dismiss();

            if (meta != null) {
                title.setText(meta.getTitle());
                author.setText(meta.getAuthor());
                creator.setText(meta.getCreator());
                producer.setText(meta.getProducer());
                subject.setText(meta.getSubject());
                keywords.setText(meta.getKeywords());
                createdDate.setText(Utils.formatMetadataDate(getApplicationContext(), meta.getCreationDate()));
                modifiedDate.setText(Utils.formatMetadataDate(getApplicationContext(), meta.getModDate()));
            }else {
                Toast.makeText(mContext, R.string.cant_load_metadata, Toast.LENGTH_LONG).show();
            }
        }
    }


    public class SaveMetadata extends AsyncTask<Void, Void, Void> {
        ProgressDialog progressDialog;
        boolean isSaved = false;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(mContext);
            progressDialog.setMessage(mContext.getResources().getString(R.string.saving_wait));
            progressDialog.setCancelable(false);
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {

            PDFBoxResourceLoader.init(mContext);

            try {
                PDDocument document = PDDocument.load(new File(pdfPath));

                if (!document.isEncrypted()) {

                    PDDocumentInformation info = document.getDocumentInformation();
                    info.setTitle(title.getText().toString());
                    info.setAuthor(author.getText().toString());
                    info.setCreator(creator.getText().toString());
                    info.setProducer(producer.getText().toString());
                    info.setSubject(subject.getText().toString());
                    info.setKeywords(keywords.getText().toString());

                    document.setDocumentInformation(info);
                    document.save(new File(pdfPath));
                    isSaved = true;

                    MediaScannerConnection.scanFile(mContext, new String[]{pdfPath}, new String[]{"application/pdf"}, null);
                } else {
                    Log.d(TAG, "Document is encrypted");

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showFileProtectedDialog();
                        }
                    });
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progressDialog.dismiss();
            if (isSaved)
                Toast.makeText(mContext, R.string.saved, Toast.LENGTH_LONG).show();
        }
    }

    public void showFileProtectedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.file_protected)
                .setMessage(R.string.file_protected_unprotect)
                .setPositiveButton(R.string.ok, null);

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }
}
