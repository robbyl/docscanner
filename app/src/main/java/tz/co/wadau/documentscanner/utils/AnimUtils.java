package tz.co.wadau.documentscanner.utils;

import android.animation.Animator;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.LinearInterpolator;

public class AnimUtils {

    private static final String TAG = AnimUtils.class.getSimpleName();

    public static void cycularReveal(View view) {


        int cX = view.getWidth();
        int cY = view.getHeight() / 2;

        Log.d(TAG, "x cord " + cX);
        Log.d(TAG, "y cord half " + cY);
        int finalRadius = (int) Math.hypot(cX, cY);
        finalRadius = finalRadius - 24;
        Log.d(TAG, "Final radius " + finalRadius);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Animator animator = ViewAnimationUtils.createCircularReveal(view, cX, cY, 0, finalRadius);
            animator.setInterpolator(new LinearInterpolator());
            animator.setDuration(1000);
            view.setVisibility(View.VISIBLE);
            animator.start();
        }
    }

}
