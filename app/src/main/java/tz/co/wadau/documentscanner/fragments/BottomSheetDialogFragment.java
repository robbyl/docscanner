package tz.co.wadau.documentscanner.fragments;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.CoordinatorLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatImageView;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.greenrobot.eventbus.EventBus;

import java.io.File;

import tz.co.wadau.documentscanner.DataUpdatedEvent;
import tz.co.wadau.documentscanner.R;
import tz.co.wadau.documentscanner.ShareAsPictureActivity;
import tz.co.wadau.documentscanner.data.DbHelper;
import tz.co.wadau.documentscanner.utils.Utils;

import static tz.co.wadau.documentscanner.utils.Utils.shareFile;

public class BottomSheetDialogFragment extends android.support.design.widget.BottomSheetDialogFragment
        implements View.OnClickListener {

    public static final String PDF_PATH = "tz.co.wadau.documentscanner.PDF_PATH";
    public static final String FROM_RECENT = "tz.co.wadau.documentscanner.FROM_RECENT";
    public final String TAG = BottomSheetDialogFragment.class.getSimpleName();
    String pdfPath;
    String fileName;
    Boolean fromRecent;
    AppCompatImageView toggleStared;
    Context context;

    @SuppressLint("RestrictedApi")
    @Override
    public void setupDialog(Dialog dialog, int style) {
        super.setupDialog(dialog, style);

        Bundle bundle = getArguments();
        pdfPath = bundle.getString(PDF_PATH);
        fileName = new File(pdfPath).getName();
        fromRecent = bundle.getBoolean(FROM_RECENT);
        context = getContext();

        View contentView = View.inflate(context, R.layout.fragment_bottom_sheet_dialog, null);
        dialog.setContentView(contentView);

        TextView textViewFileName = contentView.findViewById(R.id.file_name);
        toggleStared = contentView.findViewById(R.id.toggle_star);
        textViewFileName.setText(fileName);
        setupStared();

        CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) ((View) contentView.getParent()).getLayoutParams();
        CoordinatorLayout.Behavior behavior = params.getBehavior();

        contentView.findViewById(R.id.action_share_container).setOnClickListener(this);
        contentView.findViewById(R.id.action_edit_container).setOnClickListener(this);
        contentView.findViewById(R.id.action_print_container).setOnClickListener(this);
        contentView.findViewById(R.id.action_delete_container).setOnClickListener(this);
        contentView.findViewById(R.id.action_share_as_picture_container).setOnClickListener(this);
        contentView.findViewById(R.id.action_location_container).setOnClickListener(this);
        toggleStared.setOnClickListener(toggleStaredListener);

        if (behavior != null && behavior instanceof BottomSheetBehavior) {
            ((BottomSheetBehavior) behavior).setBottomSheetCallback(mBottomSheetBehaviorCallback);
        }
    }

    private BottomSheetBehavior.BottomSheetCallback mBottomSheetBehaviorCallback = new BottomSheetBehavior.BottomSheetCallback() {

        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismiss();
            }
        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {
        }
    };

    @Override
    public void onClick(View view) {

        dismiss();

        switch (view.getId()) {
            case R.id.action_share_container:
                shareFile(context, pdfPath);
                break;
            case R.id.action_share_as_picture_container:
                showShareAsPicture(pdfPath);
                break;
            case R.id.action_edit_container:
                renamePdf();
                break;
            case R.id.action_print_container:
                Utils.print(context, pdfPath);
                break;
            case R.id.action_location_container:
                Toast.makeText(context, pdfPath, Toast.LENGTH_LONG).show();
                break;
            case R.id.action_delete_container:
                deletePdfFile();
                break;
        }
    }

    View.OnClickListener toggleStaredListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {

            dismiss();
            DbHelper dbHelper = DbHelper.getInstance(context);

            if (dbHelper.isStared(pdfPath)) {
                dbHelper.removeStaredPDF(pdfPath);
            } else {
                dbHelper.addStaredPDF(pdfPath);
            }

            EventBus.getDefault().post(new DataUpdatedEvent.RecentPDFStaredEvent());
            EventBus.getDefault().post(new DataUpdatedEvent.DevicePDFStaredEvent());
        }
    };

    public void setupStared() {
        DbHelper dbHelper = DbHelper.getInstance(context);
        if (dbHelper.isStared(pdfPath))
            toggleStared.setImageDrawable(context.getResources().getDrawable(R.drawable.ic_action_star_yellow));
    }

    public void deletePdfFile() {

        if (fromRecent) {
            DbHelper dbHelper = DbHelper.getInstance(context);
            dbHelper.deleteRecentPDF(pdfPath);
            Log.d(TAG, "Delete from history");
        } else {
            showConfirmDialog();
        }
    }

    public void showConfirmDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle(R.string.permanently_delete_file)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        Log.d(TAG, "Delete from device");
                        File devPdfFile = new File(pdfPath);
                        if (devPdfFile.delete()) {

                            //Delete file thumbnail
                            String thumbnailPath = context.getCacheDir() + "/Thumbnails/" + Utils.removeExtension(devPdfFile.getName()) + ".jpg";
                            File thumbnail = new File(thumbnailPath);
                            thumbnail.delete();

                            // Delete file details from MediaStore.
                            MediaScannerConnection.scanFile(context, new String[]{pdfPath}, null,
                                    new MediaScannerConnection.OnScanCompletedListener() {
                                        @Override
                                        public void onScanCompleted(String path, Uri uri) {

                                            EventBus.getDefault().post(new DataUpdatedEvent.PermanetlyDeleteEvent());
                                            Log.d(TAG, "File deleted " + pdfPath);
                                        }
                                    });
                        } else {
                            Toast.makeText(context, "Can't delete file", Toast.LENGTH_LONG).show();
                        }
                    }
                }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public void renamePdf() {

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        final File renameFile = new File(pdfPath);
        final String fileName = Utils.removeExtension(renameFile.getName());

        float dpi = context.getResources().getDisplayMetrics().density;
        final EditText editText = new EditText(context);
        editText.setText(fileName);
        editText.setSelectAllOnFocus(true);

        builder.setTitle(R.string.rename_file)
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });

        final AlertDialog alertDialog = builder.create();

        alertDialog.setView(editText, (int) (24 * dpi), (int) (8 * dpi), (int) (24 * dpi), (int) (5 * dpi));
        alertDialog.show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String newFileName = editText.getText().toString();

                if (!TextUtils.equals(fileName, newFileName)) {

                    if (Utils.isFileNameValid(newFileName)) {
                        final String newpdfPath = pdfPath.replace(fileName, newFileName);

                        if (renameFile.renameTo(new File(newpdfPath))) {
                            alertDialog.dismiss();
                            DbHelper dbHelper = DbHelper.getInstance(context);
                            dbHelper.updateHistory(pdfPath, newpdfPath);
                            dbHelper.updateStaredPDF(pdfPath, newpdfPath);
                            dbHelper.updateBookmarkPath(pdfPath, newpdfPath);
                            dbHelper.updateLastOpenedPagePath(pdfPath, newpdfPath);

                            //Rename file thumbnail if exists
                            String thumbDir = context.getCacheDir() + "/Thumbnails/";
                            String thumbnailOld = thumbDir + Utils.removeExtension(renameFile.getName()) + ".jpg";
                            String thumbnailNew = thumbDir + Utils.removeExtension(newFileName) + ".jpg";
                            Log.d(TAG, "Rename thumbnail from " + thumbnailOld);
                            Log.d(TAG, "Rename thumbnail to " + thumbnailNew);
                            new File(thumbnailOld).renameTo(new File(thumbnailNew));

                            //Add renamed file to Media Store
                            MediaScannerConnection.scanFile(context,
                                    new String[]{newpdfPath},
                                    null,
                                    new MediaScannerConnection.OnScanCompletedListener() {
                                        @Override
                                        public void onScanCompleted(String path, Uri uri) {

                                            EventBus.getDefault().post(new DataUpdatedEvent.PdfRenameEvent());
                                            Log.d(TAG, "Old pdf path" + pdfPath);
                                            Log.d(TAG, "New pdf path" + newpdfPath);
                                        }
                                    });
                        } else {
                            Toast.makeText(context, R.string.failed_to_rename_file, Toast.LENGTH_LONG).show();
                        }
                    } else {
                        editText.setError(context.getString(R.string.invalid_file_name));
                    }
                } else {
                    alertDialog.dismiss();
                    Log.d(TAG, "File name not changed so do nothing");
                }
            }
        });
    }

    public void showShareAsPicture(String filePath) {
        Intent intent = new Intent(context, ShareAsPictureActivity.class);
        intent.putExtra(PDF_PATH, filePath);
        startActivity(intent);
    }
}
