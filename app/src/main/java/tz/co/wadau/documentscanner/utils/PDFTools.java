package tz.co.wadau.documentscanner.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatImageView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.InterstitialAd;
import com.itextpdf.text.pdf.PRStream;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfNumber;
import com.itextpdf.text.pdf.PdfObject;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfStream;
import com.itextpdf.text.pdf.parser.PdfImageObject;
import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfiumCore;
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility;
import com.tom_roush.pdfbox.multipdf.Splitter;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import com.tom_roush.pdfbox.util.PDFBoxResourceLoader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import tz.co.wadau.documentscanner.PDFToolsActivity;
import tz.co.wadau.documentscanner.PDFViewerActivity;
import tz.co.wadau.documentscanner.R;
import tz.co.wadau.documentscanner.SelectPDFActivity;
import tz.co.wadau.documentscanner.ViewImagesActivity;

import static android.text.format.Formatter.formatShortFileSize;
import static tz.co.wadau.documentscanner.BrowsePDFActivity.GRID_VIEW_ENABLED;
import static tz.co.wadau.documentscanner.BrowsePDFActivity.PDF_LOCATION;
import static tz.co.wadau.documentscanner.PDFToolsActivity.IS_DIRECTORY;
import static tz.co.wadau.documentscanner.ViewImagesActivity.GENERATED_IMAGES_PATH;


public class PDFTools {

    private static final String TAG = PDFTools.class.getSimpleName();
    private ConstraintLayout mProgressView;
    private TextView percent;
    private TextView currentAction;
    private ProgressBar progressBar;
    private TextView savedPath, description;
    private AppCompatImageView successIcon, closeProgressView;
    private AppCompatButton btnOpenFile, btnCancelProgress;

    public class ExtractImages extends AsyncTask<Void, Integer, Void> {
        Context mContext;
        String pdfPath;
        String allPdfPictureDir;
        int compressionQuality;
        int xrefSize;

        public ExtractImages(Context context, String pdfPath, int compressionQuality, ConstraintLayout progressView) {
            mContext = context;
            this.pdfPath = pdfPath;
            mProgressView = progressView;
            initializeProgressView();
            Utils.setLightStatusBar(context);
            this.compressionQuality = compressionQuality;

            btnCancelProgress.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ExtractImages.this.cancel(true); //Stop splitting
                    closeProgressView(mContext);
                }
            });
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            currentAction.setText(R.string.extracting);
            mProgressView.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            ArrayList<String> stringArrayList = new ArrayList<>();
            ArrayList<String> mineTypes = new ArrayList<>();
            String extractedImageName = "";


            String fileName = new File(pdfPath).getName();
            allPdfPictureDir = Environment.getExternalStorageDirectory() + "/Pictures/AllPdf/" + Utils.removeExtension(fileName) + "/";
            File folder = new File(allPdfPictureDir);

            // Create directory if it does't exist
            if (!folder.exists())
                folder.mkdirs();

            try {
                PdfReader reader = new PdfReader(pdfPath);
                xrefSize = reader.getXrefSize();
                progressBar.setMax(xrefSize);

                PdfObject object;
                PRStream stream;
                PdfObject pdfsubtype;
                int imgNum = 1;
                // Look for image and manipulate image stream
                for (int i = 0; i < xrefSize && !isCancelled(); i++) {
                    object = reader.getPdfObject(i);
                    if (object == null || !object.isStream())
                        continue;
                    stream = (PRStream) object;
                    pdfsubtype = stream.get(PdfName.SUBTYPE);

                    if (pdfsubtype != null && pdfsubtype.toString().equals(PdfName.IMAGE.toString())) {
                        try {
                            PdfImageObject image = new PdfImageObject(stream);
                            byte[] imageBytes = image.getImageAsBytes();

                            Bitmap bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                            if (bmp == null) continue;

                            extractedImageName = allPdfPictureDir + "image-" + imgNum + ".jpg";
                            FileOutputStream extractedImage = new FileOutputStream(extractedImageName);
                            bmp.compress(Bitmap.CompressFormat.JPEG, compressionQuality, extractedImage);
                            Log.d(TAG, "Image extracted " + allPdfPictureDir + "image-" + imgNum + ".jpg");
                            stream.clear();

                            if (!bmp.isRecycled()) bmp.recycle();
                            bmp = null;
                            imgNum++;
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }

                    stringArrayList.add(extractedImageName);
                    mineTypes.add("image/jpg");
                    publishProgress(i + 1);
                }

                //Update Mediafile db so the new files
                MediaScannerConnection.scanFile(mContext, stringArrayList.toArray(new String[stringArrayList.size()]),
                        mineTypes.toArray(new String[mineTypes.size()]), null);

            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }


        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            updateProgressPercent(values[0], xrefSize);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            percent.setText(R.string.hundred_percent);
            progressBar.setProgress(xrefSize);
            currentAction.setText(R.string.done);
            btnCancelProgress.setOnClickListener(null);
            showInterstialAd(mContext, "", "", allPdfPictureDir);
            openImageDirectory(mContext, mContext.getString(R.string.open_directory), allPdfPictureDir);
        }
    }


    public class ConvertPDFAsPictures extends AsyncTask<Void, Integer, Void> {

        Context mContext;
        String fileName;
        String pdfPath;
        int numPages;
        PdfiumCore pdfiumCore;
        PdfDocument pdfDocument;
        String allPdfPictureDir;

        public ConvertPDFAsPictures(Context context, String pdfPath, ConstraintLayout progressView) {
            mContext = context;
            this.pdfPath = pdfPath;
            mProgressView = progressView;
            initializeProgressView();
            Utils.setLightStatusBar(context);

            btnCancelProgress.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ConvertPDFAsPictures.this.cancel(true); //Stop Convert to Pictures
                    closeProgressView(mContext);
                }
            });
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            currentAction.setText(R.string.converting);
            mProgressView.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... voids) {

            File file = new File(pdfPath);
            fileName = Utils.removeExtension(file.getName());
            String fileName = new File(pdfPath).getName();
            ArrayList<String> stringArrayList = new ArrayList<>();
            ArrayList<String> mineTypes = new ArrayList<>();

            allPdfPictureDir = Environment.getExternalStorageDirectory() + "/Pictures/AllPdf/" + Utils.removeExtension(fileName) + "/";
            File folder = new File(allPdfPictureDir);

            // Create directory if it does't exist
            if (!folder.exists())
                folder.mkdirs();

            pdfiumCore = new PdfiumCore(mContext);
            Uri fileUri = Uri.fromFile(new File(pdfPath));

            try {

                ParcelFileDescriptor fd = mContext.getContentResolver().openFileDescriptor(fileUri, "r");
                pdfDocument = pdfiumCore.newDocument(fd);
                numPages = pdfiumCore.getPageCount(pdfDocument);
                progressBar.setMax(numPages);

                for (int pageNumber = 0; pageNumber < numPages && !isCancelled(); pageNumber++) {

                    String saveImageName = allPdfPictureDir + Utils.removeExtension(fileName) + "-Page" + (pageNumber + 1) + ".jpg";
                    FileOutputStream image = new FileOutputStream(saveImageName);
                    Log.d(TAG, "Creating page image " + saveImageName);

                    pdfiumCore.openPage(pdfDocument, pageNumber);
                    int width = pdfiumCore.getPageWidthPoint(pdfDocument, pageNumber) * 2;
                    int height = pdfiumCore.getPageHeightPoint(pdfDocument, pageNumber) * 2;

                    try {
                        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                        pdfiumCore.renderPageBitmap(pdfDocument, bmp, pageNumber, 0, 0, width, height, true);

                        bmp.compress(Bitmap.CompressFormat.JPEG, 60, image);
                    } catch (OutOfMemoryError error) {
                        Toast.makeText(mContext, R.string.failed_low_memory, Toast.LENGTH_LONG).show();
                        error.printStackTrace();
                    }

                    stringArrayList.add(saveImageName);
                    mineTypes.add("image/jpg");
                    publishProgress(pageNumber + 1);
                }

                pdfiumCore.closeDocument(pdfDocument); // important!

                //Update Mediafile db so the new files
                MediaScannerConnection.scanFile(mContext, stringArrayList.toArray(new String[stringArrayList.size()]),
                        mineTypes.toArray(new String[mineTypes.size()]), null);

            } catch (Exception e) {
                return null;
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
            showInterstialAd(mContext, "", "", allPdfPictureDir);
//            openSpecifiedFolder(mContext, mContext.getString(R.string.open_directory), allPdfPictureDir);
            openImageDirectory(mContext, mContext.getString(R.string.open_directory), allPdfPictureDir);
        }
    }


    public class SplitPDF extends AsyncTask<Void, Integer, Void> {

        private Context mContext;
        private String pdfPath;
        private int numPages;
        private String splittedPdfDocumentDir;
        private int splitFrom = 0;
        private int splitTo = 0;
        private int splitAt = 0;

        public SplitPDF(Context context, String pdfPath, ConstraintLayout progressView) {
            mContext = context;
            this.pdfPath = pdfPath;
            mProgressView = progressView;
            initializeProgressView();
            Utils.setLightStatusBar(context);

            btnCancelProgress.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PDFTools.SplitPDF.this.cancel(true); //Stop splitting
                    closeProgressView(mContext);
                }
            });
        }

        public SplitPDF(Context context, String pdfPath, ConstraintLayout progressView, int from, int to) {
            mContext = context;
            this.pdfPath = pdfPath;
            mProgressView = progressView;
            initializeProgressView();
            Utils.setLightStatusBar(context);
            splitFrom = from;
            splitTo = to;

            btnCancelProgress.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PDFTools.SplitPDF.this.cancel(true); //Stop splitting
                    closeProgressView(mContext);
                }
            });
        }

        public SplitPDF(Context context, String pdfPath, ConstraintLayout progressView, int at) {
            mContext = context;
            this.pdfPath = pdfPath;
            mProgressView = progressView;
            initializeProgressView();
            Utils.setLightStatusBar(context);
            splitAt = at;

            btnCancelProgress.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PDFTools.SplitPDF.this.cancel(true); //Stop splitting
                    closeProgressView(mContext);
                }
            });
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setIndeterminate(true);
            currentAction.setText(R.string.splitting);
            mProgressView.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... voids) {

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
            boolean isGridViewEnabled = sharedPreferences.getBoolean(GRID_VIEW_ENABLED, false);
            File pdfFile = new File(pdfPath);
            String fileName = pdfFile.getName();
            splittedPdfDocumentDir = Environment.getExternalStorageDirectory() + "/Documents/AllPdf/" + Utils.removeExtension(fileName) + "/";
            File folder = new File(splittedPdfDocumentDir);

            // Create directory if it does't exist
            if (!folder.exists())
                folder.mkdirs();

            PDFBoxResourceLoader.init(mContext);

            try {
                PDDocument document = PDDocument.load(pdfFile);
                Splitter splitter = new Splitter();

                if (splitFrom != 0 && splitTo != 0) {
                    splitter.setStartPage(splitFrom);
                    splitter.setEndPage(splitTo);
                } else if (splitAt != 0) {
                    splitter.setSplitAtPage(splitAt);
                }

                List<PDDocument> pages = splitter.split(document);
                numPages = pages.size();
                progressBar.setMax(numPages);
                Iterator<PDDocument> iterator = pages.listIterator();
                ArrayList<String> stringArrayList = new ArrayList<>();
                ArrayList<String> mineTypes = new ArrayList<>();
                removeProgressBarIndeterminate(mContext, progressBar);

                int i = 1;
                while (iterator.hasNext() && !isCancelled()) {
                    String splitedFilePath = splittedPdfDocumentDir + Utils.removeExtension(fileName) + "-Page" + i + ".pdf";
                    PDDocument doc = iterator.next();
                    try {
                        doc.save(splitedFilePath);
                        doc.close();

                        stringArrayList.add(splitedFilePath);
                        mineTypes.add("application/pdf");

                        if (isGridViewEnabled)
                            Utils.generatePDFThumbnail(mContext, splitedFilePath);

                        Log.d(TAG, "Created " + splitedFilePath);
                        publishProgress(i);
                        i++;
                    } catch (Exception exc) {
                        Toast.makeText(mContext, R.string.no_enough_disk_space, Toast.LENGTH_LONG).show();
                        this.cancel(true);
                    }
                }

                document.close();

                if (!isCancelled()) {
                    //Update Mediafile db so the new files from plit will be shown in pdf list
                    MediaScannerConnection.scanFile(mContext, stringArrayList.toArray(new String[stringArrayList.size()]),
                            mineTypes.toArray(new String[mineTypes.size()]), null);
                } else {
                    //Remove partially splitted files since user has cancelled the process
                    deleteFiles(splittedPdfDocumentDir);
                }
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
            showInterstialAd(mContext, "", "", splittedPdfDocumentDir);
            setupOpenPath(mContext, mContext.getString(R.string.open_directory), splittedPdfDocumentDir, false);
        }
    }

    public class CompressPDF extends AsyncTask<Void, Integer, Void> {

        Context mContext;
        String pdfPath;
        String allPdfDocumentDir;
        String compressedPDF;
        String uncompressedFileSize;
        String compressedFileSize;
        Long uncompressedFileLength;
        Long compressedFileLength;
        String reducedPercent;
        int compressionQuality;
        int xrefSize;
        boolean isEcrypted = false;


        public CompressPDF(Context context, String pdfPath, int compressionQuality, ConstraintLayout progressView) {
            mContext = context;
            this.pdfPath = pdfPath;
            mProgressView = progressView;
            initializeProgressView();
            Utils.setLightStatusBar(context);
            this.compressionQuality = compressionQuality;

            btnCancelProgress.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    CompressPDF.this.cancel(true); //Stop splitting
                    closeProgressView(mContext);
                }
            });
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            currentAction.setText(R.string.compressing);
            mProgressView.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... voids) {

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
            boolean isGridViewEnabled = sharedPreferences.getBoolean(GRID_VIEW_ENABLED, false);
            File myFile = new File(pdfPath);
            String fileName = myFile.getName();
            uncompressedFileLength = myFile.length();
            uncompressedFileSize = formatShortFileSize(mContext, uncompressedFileLength);
            allPdfDocumentDir = Environment.getExternalStorageDirectory() + "/Documents/AllPdf/";
            compressedPDF = allPdfDocumentDir + Utils.removeExtension(fileName) + "-Compressed.pdf";
            File folder = new File(allPdfDocumentDir);

            // Create directory if it does't exist
            if (!folder.exists())
                folder.mkdirs();

            try {
                PdfReader reader = new PdfReader(pdfPath);

                if (reader.isEncrypted()) {
                    isEcrypted = true;
                    return null;
                }

                xrefSize = reader.getXrefSize();
                progressBar.setMax(xrefSize);

                PdfObject object;
                PRStream stream;
                Bitmap bmp = null;
                // Look for image and manipulate image stream
                for (int i = 0; i < xrefSize && !isCancelled(); i++) {
                    object = reader.getPdfObject(i);
                    if (object == null || !object.isStream())
                        continue;
                    stream = (PRStream) object;
                    // if (value.equals(stream.get(key))) {
                    PdfObject pdfsubtype = stream.get(PdfName.SUBTYPE);

                    if (pdfsubtype != null && pdfsubtype.toString().equals(PdfName.IMAGE.toString())) {
                        try {
                            PdfImageObject image = new PdfImageObject(stream);
                            byte[] imageBytes = image.getImageAsBytes();

                            final BitmapFactory.Options options = new BitmapFactory.Options();

                            if (bmp != null)
                                options.inBitmap = bmp;
                            options.inPurgeable = true;
                            options.inInputShareable = true;
                            options.inTempStorage = new byte[16 * 1024];

                            bmp = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);
                            if (bmp == null) continue;

                            int width = bmp.getWidth();
                            int height = bmp.getHeight();

//                        Bitmap outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//                        Canvas outCanvas = new Canvas(outBitmap);
//                        outCanvas.drawBitmap(bmp, 0f, 0f, null);

                            ByteArrayOutputStream imgBytes = new ByteArrayOutputStream();

                            bmp.compress(Bitmap.CompressFormat.JPEG, compressionQuality, imgBytes);
                            Log.d(TAG, "Compressing image");

                            stream.setData(imgBytes.toByteArray(), false, PdfStream.BEST_COMPRESSION);
                            stream.put(PdfName.FILTER, PdfName.DCTDECODE);

                            imgBytes.close();
                            stream.clear();

                            stream.setData(imgBytes.toByteArray(), false, PRStream.NO_COMPRESSION);
                            stream.put(PdfName.TYPE, PdfName.XOBJECT);
                            stream.put(PdfName.SUBTYPE, PdfName.IMAGE);
                            stream.put(PdfName.FILTER, PdfName.DCTDECODE);
                            stream.put(PdfName.WIDTH, new PdfNumber(width));
                            stream.put(PdfName.HEIGHT, new PdfNumber(height));
                            stream.put(PdfName.BITSPERCOMPONENT, new PdfNumber(8));
                            stream.put(PdfName.COLORSPACE, PdfName.DEVICERGB);

                            if (!bmp.isRecycled()) bmp.recycle();
                            bmp = null;
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }

                    publishProgress(i + 1);
                }

                reader.removeUnusedObjects();
                // Save altered PDF
                PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(compressedPDF));
                stamper.setFullCompression();
                stamper.close();

                //Get compressed file size
                compressedFileLength = new File(compressedPDF).length();
                compressedFileSize = formatShortFileSize(mContext, compressedFileLength);
                reducedPercent = (100 - (int) ((compressedFileLength * 100) / uncompressedFileLength)) + "%";

                //Update mediastore
                MediaScannerConnection.scanFile(mContext, new String[]{compressedPDF}, new String[]{"application/pdf"}, null);

                if (isGridViewEnabled)
                    Utils.generatePDFThumbnail(mContext, compressedPDF);

            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            updateProgressPercent(values[0], xrefSize);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);

            if (!isEcrypted) {
                String compressResults = mContext.getString(R.string.reduced_from) + " "
                        + uncompressedFileSize + " " + mContext.getString(R.string.to) + " " + compressedFileSize;

                percent.setText(R.string.hundred_percent);
                progressBar.setProgress(xrefSize);
                currentAction.setText(R.string.done);
                btnCancelProgress.setOnClickListener(null);
                showInterstialAd(mContext, reducedPercent, compressResults, allPdfDocumentDir);
                setupOpenPath(mContext, mContext.getString(R.string.open_file), compressedPDF, true);
            } else {
                closeProgressView(mContext);
                Toast.makeText(mContext, R.string.file_protected_unprotect, Toast.LENGTH_LONG).show();
            }
        }
    }


    public class MergePDFFiles extends AsyncTask<Void, Integer, Void> {

        Context mContext;
        ArrayList<String> pdfPaths;
        String mergedFileName;
        String allPdfMergedDir;
        boolean mergeSuccess = true;
        String mergedFilePath;
        int numFiles;

        public MergePDFFiles(Context context, ArrayList<String> pdfPaths, String mergedFileName, ConstraintLayout progressView) {
            mContext = context;
            this.pdfPaths = pdfPaths;
            this.mergedFileName = mergedFileName;
            mProgressView = progressView;
            initializeProgressView();
            Utils.setLightStatusBar(context);

            btnCancelProgress.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PDFTools.MergePDFFiles.this.cancel(true); //Stop splitting
                    closeProgressView(mContext);
                }
            });
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setIndeterminate(true);
            currentAction.setText(R.string.merging);
            mProgressView.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... voids) {

            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
            boolean isGridViewEnabled = sharedPreferences.getBoolean(GRID_VIEW_ENABLED, false);
            allPdfMergedDir = Environment.getExternalStorageDirectory() + "/Documents/AllPdf/Merged/";
            File folder = new File(allPdfMergedDir);

            // Create directory if it does't exist
            if (!folder.exists())
                folder.mkdirs();

            mergedFilePath = allPdfMergedDir + mergedFileName + ".pdf";
            numFiles = pdfPaths.size();
            progressBar.setMax(numFiles + 1);

            PDFBoxResourceLoader.init(mContext);
            PDFMergerUtility mergerUtility = new PDFMergerUtility();
            mergerUtility.setDestinationFileName(mergedFilePath);
            removeProgressBarIndeterminate(mContext, progressBar);

            try {
                for (int i = 0; i < numFiles && !isCancelled(); i++) {
                    mergerUtility.addSource(new File(pdfPaths.get(i)));
                    publishProgress(i + 1);
                }

                mergerUtility.mergeDocuments();
                publishProgress(numFiles + 1);

                if (isCancelled())
                    new File(mergedFilePath).delete();

                MediaScannerConnection.scanFile(mContext, new String[]{mergedFilePath}, new String[]{"application/pdf"}, null);

                if (isGridViewEnabled)
                    Utils.generatePDFThumbnail(mContext, mergedFilePath);
            } catch (Exception e) {
                e.printStackTrace();
                mergeSuccess = false;
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            updateProgressPercent(values[0], numFiles + 1);
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            currentAction.setText(R.string.done);
            btnCancelProgress.setOnClickListener(null);
            showInterstialAd(mContext, "", "", allPdfMergedDir);
            setupOpenPath(mContext, mContext.getString(R.string.open_file), mergedFilePath, true);

            if (!mergeSuccess)
                Toast.makeText(mContext, R.string.merge_failed, Toast.LENGTH_LONG).show();
        }

    }

    public class ExtractText extends AsyncTask<Void, Integer, Void> {

        Context mContext;
        String pdfPath;
        String extractedTextFilePath;
        String extractedTextDir;
        String errorMessage;
        boolean textExtractSuccess = true;

        public ExtractText(Context context, String pdfPath, ConstraintLayout progressView) {
            mContext = context;
            this.pdfPath = pdfPath;
            mProgressView = progressView;
            initializeProgressView();
            Utils.setLightStatusBar(context);

            btnCancelProgress.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PDFTools.ExtractText.this.cancel(true); //Stop splitting
                    closeProgressView(mContext);
                }
            });
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setIndeterminate(true);
            currentAction.setText(R.string.extracting);
            mProgressView.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... voids) {

            extractedTextDir = Environment.getExternalStorageDirectory() + "/Documents/AllPdf/Texts/";
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
                    String text = textStripper.getText(document);

                    FileOutputStream stream = new FileOutputStream(new File(extractedTextFilePath));
                    stream.write(text.getBytes());

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
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            currentAction.setText(R.string.done);
            btnCancelProgress.setOnClickListener(null);
            showInterstialAd(mContext, "", "", extractedTextDir);
//            setupOpenPath(mContext, mContext.getString(R.string.open_file), mergedFilePath, true);

            if (!textExtractSuccess)
                Toast.makeText(mContext, errorMessage, Toast.LENGTH_LONG).show();
        }
    }


    private static void deleteFiles(String path) {

        File file = new File(path);

        if (file.exists()) {
            String deleteCmd = "rm -r " + path;
            Runtime runtime = Runtime.getRuntime();
            try {
                runtime.exec(deleteCmd);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void initializeProgressView() {
        percent = mProgressView.findViewById(R.id.percent);
        currentAction = mProgressView.findViewById(R.id.current_action);
        progressBar = mProgressView.findViewById(R.id.progress_bar);
        description = mProgressView.findViewById(R.id.description);
        savedPath = mProgressView.findViewById(R.id.saved_path);
        successIcon = mProgressView.findViewById(R.id.success_icon);
        btnOpenFile = mProgressView.findViewById(R.id.open_file);
        btnCancelProgress = mProgressView.findViewById(R.id.cancel_progress);
        closeProgressView = mProgressView.findViewById(R.id.close_progress_view);
    }

    private void processingFinished(Context context, String currAction, String mDescription, String splitPath) {
        percent.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.INVISIBLE);
        successIcon.setVisibility(View.VISIBLE);
        closeProgressView.setVisibility(View.VISIBLE);
        btnOpenFile.setVisibility(View.VISIBLE);

        btnCancelProgress.setVisibility(View.GONE);

        String splittedFilePaths = context.getString(R.string.saved_to) + " " + splitPath;

        if (!TextUtils.isEmpty(currAction))
            currentAction.setText(currAction);

        if (!TextUtils.isEmpty(mDescription)) {
            description.setText(mDescription);
            description.setVisibility(View.VISIBLE);
        }

        savedPath.setText(splittedFilePaths);
    }

    private void closeProgressView(Context context) {
        mProgressView.setVisibility(View.GONE);
        successIcon.setVisibility(View.GONE);
        btnOpenFile.setVisibility(View.GONE);
        closeProgressView.setVisibility(View.GONE);
        description.setVisibility(View.GONE);

        progressBar.setVisibility(View.VISIBLE);
        percent.setVisibility(View.VISIBLE);
        btnCancelProgress.setVisibility(View.VISIBLE);

        progressBar.setProgress(0);
        percent.setText("0%");
        description.setText("");
        savedPath.setText("");
        Utils.clearLightStatusBar(context);
    }

    public void showInterstialAd(final Context context, final String currAction, final String description, final String savedToPath) {
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
                                    processingFinished(context, currAction, description, savedToPath);
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
                    processingFinished(context, currAction, description, savedToPath);
                }
            }
        }, 1000);
    }

    private void updateProgressPercent(int curProgress, int totalProgress) {
        int percentNo = (int) (curProgress * 100F) / totalProgress;
        String percentText = percentNo + "%";
        percent.setText(percentText);
        progressBar.setProgress(curProgress);
    }

    private void setupOpenPath(final Context context, final String buttonText, final String filePath, final boolean isFile) {
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

    public void openSpecifiedFolder(final Context context, String buttonText, final String filePath) {
        btnOpenFile.setText(buttonText);
        btnOpenFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse(filePath);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "resource/folder");
                Log.d(TAG, "Open directory " + filePath);
                if (intent.resolveActivity(context.getPackageManager()) != null) {
                    context.startActivity(intent);
                } else {
                    Toast.makeText(context, "Can't open directory", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    public void openImageDirectory(final Context context, String buttonText, final String directory) {
        btnOpenFile.setText(buttonText);
        btnOpenFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context, ViewImagesActivity.class);
                intent.putExtra(GENERATED_IMAGES_PATH, directory);
                context.startActivity(intent);
            }
        });
    }

    public void removeProgressBarIndeterminate(Context context, final ProgressBar progressBar) {
        ((Activity) context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setIndeterminate(false);
            }
        });
    }
}