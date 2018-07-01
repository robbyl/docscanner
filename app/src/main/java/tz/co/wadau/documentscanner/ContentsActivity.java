package tz.co.wadau.documentscanner;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.shockwave.pdfium.PdfDocument;

import java.io.File;

import tz.co.wadau.documentscanner.adapters.BookmarksAdapter;
import tz.co.wadau.documentscanner.adapters.ContentsAdapter;
import tz.co.wadau.documentscanner.adapters.ContentsPagerAdapter;
import tz.co.wadau.documentscanner.models.Bookmark;

import static tz.co.wadau.documentscanner.PDFViewerActivity.CONTENTS_PDF_PATH;
import static tz.co.wadau.documentscanner.PDFViewerActivity.PAGE_NUMBER;

public class ContentsActivity extends AppCompatActivity
        implements BookmarksAdapter.OnBookmarkClickedListener,
        ContentsAdapter.OnContentClickedListener {

    private final String TAG = ContentsActivity.class.getSimpleName();
    TabLayout mTabLayout;
    ViewPager viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contents);
        Intent intent = getIntent();
        String pdfPath = intent.getStringExtra(CONTENTS_PDF_PATH);

        mTabLayout = findViewById(R.id.tab_layout_bookmark);
        Toolbar toolbar = findViewById(R.id.toolbar__contents);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        String pdfName = new File(pdfPath).getName();
        actionBar.setTitle(pdfName);
//        actionBar.setDisplayHomeAsUpEnabled(true);



        viewPager = findViewById(R.id.view_pager_contents);
        ContentsPagerAdapter adapter = new ContentsPagerAdapter(getSupportFragmentManager(), pdfPath);
        viewPager.setAdapter(adapter);

        mTabLayout.addTab(mTabLayout.newTab().setText(R.string.contents));
        mTabLayout.addTab(mTabLayout.newTab().setText(R.string.bookmarks));

        viewPager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(mTabLayout));
        mTabLayout.addOnTabSelectedListener(tabSelectedListener);
    }

    TabLayout.OnTabSelectedListener tabSelectedListener = new TabLayout.OnTabSelectedListener() {
        @Override
        public void onTabSelected(TabLayout.Tab tab) {
            viewPager.setCurrentItem(tab.getPosition());
        }

        @Override
        public void onTabUnselected(TabLayout.Tab tab) {

        }

        @Override
        public void onTabReselected(TabLayout.Tab tab) {

        }
    };

    @Override
    public void onBookmarkClicked(Bookmark bookmark) {
        Intent intent = new Intent();
        intent.putExtra(PAGE_NUMBER, bookmark.getPageNumber());
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onContentClicked(PdfDocument.Bookmark bookmark) {
        Intent intent = new Intent();
        intent.putExtra(PAGE_NUMBER, (int)bookmark.getPageIdx() + 1);
        setResult(RESULT_OK, intent);
        finish();
    }
}
