package tz.co.wadau.documentscanner.ui;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.AppCompatImageView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;

import tz.co.wadau.documentscanner.R;

public class MaterialSearchView extends FrameLayout {
    private final String TAG = MaterialSearchView.class.getSimpleName();
    private Context context;
    private ConstraintLayout materialSearchContainer;
    private EditText editText;
    private AppCompatImageView closeSearch;
    private CharSequence mCurrentQuery;
    private OnQueryTextListener mOnQueryTextListener;
    private AppCompatImageView clearSearch;
    private int parentWidth;
    private Float parentHeight;
    View parent;

    int radius;
    int cY;
    int cX;

    public MaterialSearchView(Context context) {
        super(context);
        this.context = context;
        init();
    }

    public MaterialSearchView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        init();
    }

    public MaterialSearchView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.context = context;
        init();
    }

    public MaterialSearchView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.context = context;
        init();
    }

    private void init() {
        LayoutInflater.from(context).inflate(R.layout.material_search_view, this, true);
        materialSearchContainer = findViewById(R.id.material_search_container);
        materialSearchContainer.setVisibility(GONE);

        parent = (View) materialSearchContainer.getParent();

        editText = materialSearchContainer.findViewById(R.id.edit_text_search);
        editText.setOnFocusChangeListener(focusChangeListener);
        closeSearch = materialSearchContainer.findViewById(R.id.action_close_search);
        clearSearch = materialSearchContainer.findViewById(R.id.action_clear_search);
        closeSearch.setOnClickListener(closeListerner);
        clearSearch.setOnClickListener(clearSearchListner);

//        DisplayMetrics displayMetrics = new DisplayMetrics();
//        ((Activity) context).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
//        Float density = displayMetrics.density;
//        parentWidth = (int) (displayMetrics.heightPixels / 2);
//        parentHeight = displayMetrics.widthPixels * density;
//        cX = (int) (200 * density);
//        cY = (int) (24 * density);
//        radius = (int) Math.hypot(parentHeight, cY);
//
////        parentWidth = parent.getWidth();
////        parentHeight = parent.getHeight();
//        Log.d(TAG, "Width " + parentWidth);
//        Log.d(TAG, "Height " + parentHeight);


        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                MaterialSearchView.this.onTextChanged(charSequence);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
//        this.setVisibility(GONE);
    }

    @Override
    public void clearFocus() {
        hideKeyboard(this);
        super.clearFocus();
        editText.clearFocus();
    }


    private void onTextChanged(CharSequence newText) {
        // Get current query
        mCurrentQuery = editText.getText();

        // If the text is not empty, show the empty button and hide the voice button
        if (!TextUtils.isEmpty(mCurrentQuery)) {
            clearSearch.setVisibility(VISIBLE);

        } else {
            clearSearch.setVisibility(GONE);
        }

        // If we have a query listener and the text has changed, call it.
        if (mOnQueryTextListener != null) {
            mOnQueryTextListener.onQueryTextChange(newText.toString());
        }
    }

    public void openSearch() {
        editText.setText("");
        editText.requestFocus();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            AnimUtils.cycularReveal(materialSearchContainer);
            materialSearchContainer.setVisibility(VISIBLE);
        } else {
            materialSearchContainer.setVisibility(VISIBLE);
        }
    }

    public void closeSearch() {
        materialSearchContainer.setVisibility(GONE);
        editText.setText("");
        editText.clearFocus();
        hideKeyboard(editText);
    }

    public boolean isSearchOpen() {
        return materialSearchContainer.getVisibility() == VISIBLE;
    }

    OnClickListener closeListerner = new OnClickListener() {
        @Override
        public void onClick(View view) {
//            if (isSearchOpen()) {
            closeSearch();
            Log.d(TAG, "Search is closed");
//            }
        }
    };

    OnClickListener clearSearchListner = new OnClickListener() {
        @Override
        public void onClick(View view) {
            editText.setText("");
            clearSearch.setVisibility(GONE);
        }
    };

    OnFocusChangeListener focusChangeListener = new OnFocusChangeListener() {
        @Override
        public void onFocusChange(View view, boolean hasFocus) {
            if (hasFocus) {
                showKeyboard(editText);
            }
        }
    };

    private boolean isHardKeyboardAvailable() {
        return context.getResources().getConfiguration().keyboard != Configuration.KEYBOARD_NOKEYS;
    }

    private void showKeyboard(View view) {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1 && view.hasFocus()) {
            view.clearFocus();
        }

        view.requestFocus();

        if (!isHardKeyboardAvailable()) {
            InputMethodManager inputMethodManager = (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.showSoftInput(view, 0);
        }
    }

    private void hideKeyboard(View view) {
        InputMethodManager inputMethodManager =
                (InputMethodManager) view.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public void setOnQueryTextListener(MaterialSearchView.OnQueryTextListener mOnQueryTextListener) {
        this.mOnQueryTextListener = mOnQueryTextListener;
    }

    public interface OnQueryTextListener {
        /**
         * Called when a search query is submitted.
         *
         * @param query The text that will be searched.
         * @return True when the query is handled by the listener, false to let the SearchView handle the default case.
         */
        boolean onQueryTextSubmit(String query);

        /**
         * Called when a search query is changed.
         *
         * @param newText The new text of the search query.
         * @return True when the query is handled by the listener, false to let the SearchView handle the default case.
         */
        boolean onQueryTextChange(String newText);
    }
}
