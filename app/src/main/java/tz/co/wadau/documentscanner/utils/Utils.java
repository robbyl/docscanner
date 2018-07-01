package tz.co.wadau.documentscanner.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.preference.PreferenceManager;
import android.print.PrintManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.shockwave.pdfium.PdfDocument;
import com.shockwave.pdfium.PdfPasswordException;
import com.shockwave.pdfium.PdfiumCore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import tz.co.wadau.documentscanner.R;
import tz.co.wadau.documentscanner.adapters.PrintDocumentAdapter;
import tz.co.wadau.documentscanner.data.DbHelper;
import tz.co.wadau.documentscanner.models.Pdf;

import static android.content.Context.ACTIVITY_SERVICE;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.support.v4.print.PrintHelper.systemSupportsPrint;

/**
 * Created by admin on 31-Dec-17.
 */

public class Utils {

    private final static String TAG = Utils.class.getSimpleName();

    /* Checks if external storage is available for read and write */
    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /* Checks if external storage is available to at least read */
    public static boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) || Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }


    public static boolean isTablet(Context context) {
        return context.getResources().getBoolean(R.bool.isTablet);
    }

    public static String formatDate(Date date) {
        SimpleDateFormat formatted = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return formatted.format(date);
    }

    public static String formatDateToHumanReadable(Long longDate) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd yyyy", Locale.getDefault());
        Date date = new Date(longDate);
        return dateFormat.format(date);
    }

    public static String formatDateLongFormat(String dateStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        Date date = null;
        try {
            date = sdf.parse(dateStr);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        SimpleDateFormat mFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
        return mFormat.format(date);
    }

    public static String formatMetadataDate(Context context, String dateStr) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault());
        try {
            dateStr = dateStr.split("\\+")[0];
            dateStr = dateStr.split(":")[1];
            Date date = sdf.parse(dateStr);
            SimpleDateFormat mFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
            return mFormat.format(date);
        } catch (Exception e) {
            e.printStackTrace();
            return context.getString(R.string.unknown);
        }
    }

    public static long getTimeInMills(String dateStr) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date date = sdf.parse(dateStr);
        return date.getTime();
    }

    public static Double dateDiffInDays(Long HighMills, Long LowMills) {
        return ((HighMills - LowMills) / (24d * 60d * 60d * 1000d));
    }

    public static String formatToSystemDateFormat(Context context) {
        //Reading system date format
        Format dateFormat = android.text.format.DateFormat.getDateFormat(context);
        String pattern = ((SimpleDateFormat) dateFormat).toLocalizedPattern();

        SimpleDateFormat systemDateFormat = new SimpleDateFormat(pattern, Locale.getDefault());
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        return systemDateFormat.format(calendar.getTime());
    }

    public static String formatColorToHex(int intColor) {
        return String.format("#%06X", (0xFFFFFF & intColor));
    }

    public static String removeExtension(String s) {

        String separator = System.getProperty("file.separator");
        String filename;

        // Remove the path upto the filename.
        int lastSeparatorIndex = s.lastIndexOf(separator);
        if (lastSeparatorIndex == -1) {
            filename = s;
        } else {
            filename = s.substring(lastSeparatorIndex + 1);
        }

        // Remove the extension.
        int extensionIndex = filename.lastIndexOf(".");
        if (extensionIndex == -1)
            return filename;

        return filename.substring(0, extensionIndex);
    }

    public static void shareFile(Context context, String pdfPath) {

        File shareFile = new File(pdfPath);

        try {
            Uri uriToSharedFile = FileProvider.getUriForFile(context, "tz.co.wadau.documentscanner.fileprovider", shareFile);
            Intent shareIntent = ShareCompat.IntentBuilder.from((Activity) context)
                    .setType(context.getContentResolver().getType(uriToSharedFile))
                    .setStream(uriToSharedFile)
                    .getIntent();

//            shareIntent.setData(uriToSharedFile);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            String chooserTitle = context.getResources().getString(R.string.share_this_file_via);
            Intent chooserIntent = Intent.createChooser(shareIntent, chooserTitle);
            chooserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            if (chooserIntent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(chooserIntent);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, R.string.cant_share_file, Toast.LENGTH_LONG).show();
        }
    }

    public static boolean isFileNameValid(String fileName) {
        fileName = fileName.trim();
        return !TextUtils.isEmpty(fileName) && (fileName.matches("[a-zA-Z0-9-_ ]*"));
    }

    public static void deleteFiles(String path) {

        File file = new File(path);

        if (file.exists() && file.isDirectory()) {
            String deleteCmd = "find " + path + " -xdev -mindepth 1 -delete";
            Runtime runtime = Runtime.getRuntime();
            try {
                runtime.exec(deleteCmd);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static Uri getImageUriFromPath(String pdfPath) {
        String imagePath = pdfPath.replace(".pdf", ".jpg");
        File file = new File(imagePath);
        return Uri.fromFile(file);
    }

    public static boolean isThumbnailPresent(Context context, String pdfPath) {
        String fileName = new File(pdfPath).getName();
        String THUMBNAILS_DIR = context.getCacheDir() + "/Thumbnails/";
        File thumbnail = new File(THUMBNAILS_DIR + removeExtension(fileName) + ".jpg");
        return thumbnail.exists();
    }

    public static void generatePDFThumbnail(Context context, String pdfPath) {

        PdfiumCore pdfiumCore = new PdfiumCore(context);
        File file = new File(pdfPath);
        String fileName = file.getName();
        Uri fileUri = Uri.fromFile(file);

        try {

            ParcelFileDescriptor fd = context.getContentResolver().openFileDescriptor(fileUri, "r");
            PdfDocument pdfDocument = pdfiumCore.newDocument(fd);

            String THUMBNAILS_DIR = context.getCacheDir() + "/Thumbnails/";
            File folder = new File(THUMBNAILS_DIR);
            if (!folder.exists())
                folder.mkdirs();

            String imagePath = THUMBNAILS_DIR + removeExtension(fileName) + ".jpg";
            Log.d(TAG, "Generating thumb img " + imagePath);
            FileOutputStream outputStream = new FileOutputStream(imagePath);
            int coverPage = 0;

            pdfiumCore.openPage(pdfDocument, coverPage);
            int width = pdfiumCore.getPageWidthPoint(pdfDocument, coverPage) / 2;
            int height = pdfiumCore.getPageHeightPoint(pdfDocument, coverPage) / 2;

            try {
                Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                pdfiumCore.renderPageBitmap(pdfDocument, bmp, coverPage, 0, 0, width, height, true);
                bmp.compress(Bitmap.CompressFormat.JPEG, 50, outputStream);
            } catch (OutOfMemoryError error) {
                Toast.makeText(context, R.string.failed_low_memory, Toast.LENGTH_LONG).show();
                error.printStackTrace();
            }

            pdfiumCore.closeDocument(pdfDocument); // important!

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static class BackgroundGenerateThumbnails extends AsyncTask<Void, Void, Void> {

        private Context mContext;

        public BackgroundGenerateThumbnails(Context context) {
            this.mContext = context;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            List<Pdf> myPdfs = DbHelper.getInstance(mContext).getAllPdfs();
            int fileNum = myPdfs.size();

            for (int i = 0; i < fileNum; i++) {
                String mPDFPath = myPdfs.get(i).getAbsolutePath();

                if (!Utils.isThumbnailPresent(mContext, mPDFPath)) {
                    Utils.generatePDFThumbnail(mContext, mPDFPath);
                }
            }

            return null;
        }
    }

    public static void print(Context context, String filePath) {

        try {
            PdfiumCore pdfiumCore = new PdfiumCore(context);
            ParcelFileDescriptor fd = context.getContentResolver().openFileDescriptor(Uri.fromFile(new File(filePath)), "r");
            PdfDocument pdfDocument = pdfiumCore.newDocument(fd);

            if (systemSupportsPrint()) {

                // Get a PrintManager instance
                PrintManager printManager = (PrintManager) context.getSystemService(Context.PRINT_SERVICE);

                // Set job name, which will be displayed in the print queue
                String jobName = context.getString(R.string.app_name) + " Document";

                // Start a print job, passing in a PrintDocumentAdapter implementation
                // to handle the generation of a print document
                if (printManager != null) {
                    PrintDocumentAdapter documentAdapter = new PrintDocumentAdapter(new File(filePath));
                    printManager.print(jobName, documentAdapter, null);
                }

            } else {
                Toast.makeText(context, R.string.device_does_not_support_printing, Toast.LENGTH_LONG).show();
            }

        } catch (PdfPasswordException ex) {
            Toast.makeText(context, R.string.cant_print_password_protected_pdf, Toast.LENGTH_LONG).show();
            ex.printStackTrace();

        } catch (IOException io) {
            Toast.makeText(context, R.string.cannot_print_malformed_pdf, Toast.LENGTH_LONG).show();
            io.printStackTrace();

        } catch (Exception e) {
            Toast.makeText(context, R.string.cannot_print_unknown_error, Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    public static List<String> getPDFPathsFromFiles(List<File> pdfFiles) {
        List<String> myPdfPaths = new ArrayList<>();

        for (File file : pdfFiles) {
            myPdfPaths.add(file.getAbsolutePath());
        }

        return myPdfPaths;
    }


    public static void startShareActivity(Context context) {
        String shareText = "Hi! 'am using this nice app 'All PDF' for reading and manipulating PDF files. You can find it on Google Play or at this link https://play.google.com/store/apps/details?id=tz.co.wadau.documentscanner";
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
        shareIntent.setType("text/plain");

        String title = context.getResources().getString(R.string.chooser_title);
        Intent chooser = Intent.createChooser(shareIntent, title);
        chooser.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        if (shareIntent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(new Intent(chooser));
        }
    }


    public static void openProVersionPlayStore(Context context) {
        Uri uri = Uri.parse("market://details?id=tz.co.wadau.documentscanner");
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.setFlags(FLAG_ACTIVITY_NEW_TASK);

        try {
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            try {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=tz.co.wadau.documentscanner"));
                i.setFlags(FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(i);
            } catch (Exception ex) {
                Toast.makeText(context, R.string.unable_to_find_play_store, Toast.LENGTH_LONG).show();
            }
        }
    }

    public static void setLightStatusBar(final Context context) {
        View view = ((Activity) context).getWindow().getDecorView();
        int flags = view.getSystemUiVisibility();
        int colorTo = context.getResources().getColor(R.color.colorAccent);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                view.setSystemUiVisibility(flags);
                ((Activity) context).getWindow().setStatusBarColor(colorTo);
            }
        }
    }

    public static void clearLightStatusBar(final Context context) {
        View view = ((Activity) context).getWindow().getDecorView();
        int flags = view.getSystemUiVisibility();
        int colorFrom = context.getResources().getColor(R.color.colorPrimaryDark);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                view.setSystemUiVisibility(flags);
                ((Activity) context).getWindow().setStatusBarColor(colorFrom);
            }
        }
    }

//    public static void setupTheme(Context context) {
//        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
//        boolean isBlack = sharedPreferences.getBoolean(SettingsFragment.KEY_PREFS_NIGHT_MODE, false);
//        if (isBlack) {
//            context.setTheme(R.style.NightModeTheme);
//        } else {
//            context.setTheme(R.style.AppTheme);
//        }
//    }

//    public static boolean isBlackThemeEnabled(Context context) {
//        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
//        return sharedPreferences.getBoolean(SettingsFragment.KEY_PREFS_NIGHT_MODE, false);
//    }
//
//    public static boolean isMultiColumnView(Context context) {
//        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
//        return sharedPreferences.getBoolean(PREFS_IS_MULTI_COLUMN_VEW, true);
//    }

//    public static int[] colorChoices(Context context) {
//        int[] mColorChoices = null;
//        String[] colorArray = context.getResources().getStringArray(R.array.default_color_choice_values);
//        if (colorArray != null && colorArray.length > 0) {
//            mColorChoices = new int[colorArray.length];
//            for (int i = 0; i < colorArray.length; i++) {
//                mColorChoices[i] = Color.parseColor(colorArray[i]);
//            }
//        }
//        return mColorChoices;
//    }
//


    /*
     * Gets the file path of the given Uri.
     */
    @SuppressLint("NewApi")
    public static String getPath(Context context, Uri uri) throws URISyntaxException {
        final boolean needToCheckUri = Build.VERSION.SDK_INT >= 19;
        String selection = null;
        String[] selectionArgs = null;
        // Uri is different in versions after KITKAT (Android 4.4), we need to
        // deal with different Uris.
        if (needToCheckUri && DocumentsContract.isDocumentUri(context.getApplicationContext(), uri)) {
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                return Environment.getExternalStorageDirectory() + "/" + split[1];
            } else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                uri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
            } else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("image".equals(type)) {
                    uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                selection = "_id=?";
                selectionArgs = new String[]{split[1]};
            }
        }
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = {MediaStore.Images.Media.DATA};

            try {
                Cursor cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                if (cursor.moveToFirst()) {
                    return cursor.getString(column_index);
                }
                cursor.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    // Get a MemoryInfo object for the device's current memory status.
    public static ActivityManager.MemoryInfo getAvailableMemory(Context context) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo memoryInfo = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(memoryInfo);
        return memoryInfo;
    }

    public static void showPremiumFeatureDialog(final Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(((Activity) context).getLayoutInflater().inflate(R.layout.dialog_premium_feature, null));
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        AppCompatButton btnGetProVersion = alertDialog.findViewById(R.id.btn_get_premium);
        AppCompatButton btnLater = alertDialog.findViewById(R.id.btn_later);

        btnGetProVersion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
                openProVersionPlayStore(context);
            }
        });

        btnLater.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                alertDialog.dismiss();
            }
        });
    }

    public static void setUpRateUsDialog(final Context context) {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        int runTimes = preferences.getInt("prefs_run_times", 0);
        boolean showRateUs = preferences.getBoolean("prefs_show_rate_us", true);

        final SharedPreferences.Editor editor = preferences.edit();
        runTimes = runTimes + 1;
        editor.putInt("prefs_run_times", runTimes);
        editor.apply();

        if (runTimes % 2 == 0 && showRateUs) {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setView(R.layout.dialog_rate_us).setPositiveButton(R.string.rate, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Open Google play
                    launchMarket(context);
                }
            }).setNegativeButton(R.string.not_interested, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                    editor.putBoolean("prefs_show_rate_us", false);
                    editor.apply();
                }
            }).setNeutralButton(R.string.later, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    // Do nothing...
                }
            });

            AlertDialog alertDialog = builder.create();
            alertDialog.show();
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(context.getResources().getColor(R.color.colorDarkGreen));
            alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(context.getResources().getColor(R.color.colorDarkGreen));
            alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(context.getResources().getColor(R.color.colorDarkGreen));
        }
    }

    public static void launchMarket(Context context) {
        //Disable rate us dialog
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("prefs_show_rate_us", false);
        editor.apply();

        Uri uri = Uri.parse("market://details?id=" + context.getPackageName());
        Intent myAppLinkToMarket = new Intent(Intent.ACTION_VIEW, uri);
        myAppLinkToMarket.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        try {
            context.startActivity(myAppLinkToMarket);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, R.string.unable_to_find_play_store, Toast.LENGTH_LONG).show();
        }
    }
}
