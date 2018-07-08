package tz.co.wadau.documentscanner;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;

import java.io.IOException;

import tz.co.wadau.documentscanner.ScalingUtilities.ScalingLogic;

public class ImageResizer {

    // ===========================================================
    // Constants
    // ===========================================================

    // ===========================================================
    // Fields
    // ===========================================================

    // ===========================================================
    // Constructors
    // ===========================================================

    // ===========================================================
    // Getter & Setter
    // ===========================================================

    // ===========================================================
    // Methods for/from SuperClass/Interfaces
    // ===========================================================

    // ===========================================================
    // Methods
    // ===========================================================

    /**
     * @return Resized Bitmap Image, If image was resized, old bitmap is recycled
     */
    public static Bitmap resizeImage(Bitmap unscaledBitmap, int desiredWidth, int desiredHeight, boolean recycleOldOne) throws IOException {

        try {
            // Part 1: Decode image
            if (!(unscaledBitmap.getWidth() <= desiredWidth && unscaledBitmap.getHeight() <= desiredHeight)) {
                // Part 2: Scale image
                Bitmap scaledBitmap = ScalingUtilities.createScaledBitmap(unscaledBitmap, desiredWidth, desiredHeight, ScalingLogic.FIT);
                if (recycleOldOne) unscaledBitmap.recycle();
                return scaledBitmap;
            } else {

                return unscaledBitmap;
            }

        } catch (Throwable e) {
            throw new IOException(e);
        }
    }

    public static Bitmap resizeImage(Bitmap unscaledBitmap, int desiredWidth, int desiredHeight) throws IOException {
        return resizeImage(unscaledBitmap, desiredWidth, desiredHeight, true);
    }

    public static Bitmap scaleBitmap(Bitmap bitmap, int width, int height) {
        Matrix m = new Matrix();
        m.setRectToRect(new RectF(0, 0, bitmap.getWidth(), bitmap.getHeight()), new RectF(0, 0, width, height), Matrix.ScaleToFit.CENTER);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
    }

    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================

}
