package tz.co.wadau.documentscanner;

import android.media.ExifInterface;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExifInterfaceUtils {

    public static void copyExifAttribute(ExifInterface source, ExifInterface target, String tag) {
        String value = source.getAttribute(tag);
        if (value == null) {
            return;
        }
        target.setAttribute(tag, value);
    }

    public static void copyExifAttributes(ExifInterface source, ExifInterface target, String... tags) {
        for (String tag : tags) {
            copyExifAttribute(source, target, tag);
        }
    }

    public static void copyExifAttributes(ExifInterface source, ExifInterface target, List<String> tags) {
        for (String tag : tags) {
            copyExifAttribute(source, target, tag);
        }
    }

    public static void removeTag(String pathToFile, String tag) {
        copyExifInterface(pathToFile, pathToFile, tag);
    }

    public static void copyExifInterface(String sourceLocation, String targetLocation, String excludeExifTag) {
        ExifInterface sourceExif = null;
        try {
            sourceExif = new ExifInterface(sourceLocation);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        ExifInterface targetExif = null;
        try {
            targetExif = new ExifInterface(targetLocation);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        final String[] EXIF_ATTRIBUTES_TO_COPY = {
                ExifInterface.TAG_DATETIME,
                ExifInterface.TAG_MAKE,
                ExifInterface.TAG_MODEL,
                ExifInterface.TAG_FLASH,
                ExifInterface.TAG_GPS_LATITUDE,
                ExifInterface.TAG_GPS_LONGITUDE,
                ExifInterface.TAG_GPS_LATITUDE_REF,
                ExifInterface.TAG_GPS_LONGITUDE_REF,
                ExifInterface.TAG_EXPOSURE_TIME,
                ExifInterface.TAG_APERTURE,
                ExifInterface.TAG_ISO,
                ExifInterface.TAG_GPS_ALTITUDE,
                ExifInterface.TAG_GPS_ALTITUDE_REF,
                ExifInterface.TAG_GPS_TIMESTAMP,
                ExifInterface.TAG_GPS_DATESTAMP,
                ExifInterface.TAG_WHITE_BALANCE,
                ExifInterface.TAG_GPS_PROCESSING_METHOD,
                ExifInterface.TAG_ORIENTATION
        };

        List<String> exifAttributesToCopyList = new ArrayList<>(Arrays.asList(EXIF_ATTRIBUTES_TO_COPY));
        if (!Utils.isStringEmpty(excludeExifTag)) {
            exifAttributesToCopyList.remove(excludeExifTag);
        }


        ExifInterfaceUtils.copyExifAttributes(sourceExif, targetExif, exifAttributesToCopyList);

        try {
            targetExif.saveAttributes();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
