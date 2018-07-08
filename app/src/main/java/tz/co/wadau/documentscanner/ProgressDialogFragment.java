package tz.co.wadau.documentscanner;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.os.Bundle;

public class ProgressDialogFragment extends DialogFragment {
    // ===========================================================
    // Constants
    // ===========================================================
    public static final String EXTRA_MESSAGE = "message";
    // ===========================================================
    // Methods for/from SuperClass/Interfaces
    // ===========================================================

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        ProgressDialog dialog = new ProgressDialog(getActivity());
        if (getArguments() != null && getArguments().containsKey(EXTRA_MESSAGE)) {
            dialog.setMessage(getArguments().getString(EXTRA_MESSAGE));
        }

        return dialog;
    }

    // ===========================================================
    // Methods
    // ===========================================================

    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================
}
