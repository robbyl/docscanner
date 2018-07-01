package tz.co.wadau.documentscanner.adapters;

import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentInfo;
import android.text.TextUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class PrintDocumentAdapter extends android.print.PrintDocumentAdapter {
    private File fileToPrint;
    String fileName;

    public PrintDocumentAdapter(File fileToPrint) {
        this.fileToPrint = fileToPrint;
        fileName = !TextUtils.isEmpty(fileToPrint.getName()) ? fileToPrint.getName() : "Unknown";
    }

    @Override
    public void onWrite(PageRange[] pages, ParcelFileDescriptor destination, CancellationSignal cancellationSignal, WriteResultCallback callback) {

        try {

            InputStream input = new FileInputStream(fileToPrint);
            OutputStream output = new FileOutputStream(destination.getFileDescriptor());

            byte[] buf = new byte[1024];
            int bytesRead;

            while ((bytesRead = input.read(buf)) > 0) {
                output.write(buf, 0, bytesRead);
            }

            callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
            input.close();
            output.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes, CancellationSignal cancellationSignal, LayoutResultCallback callback, Bundle extras) {

        if (cancellationSignal.isCanceled()) {
            callback.onLayoutCancelled();
            return;
        }

        PrintDocumentInfo pdi = new PrintDocumentInfo.Builder(fileName)
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).build();

        callback.onLayoutFinished(pdi, true);
    }


//    File mPdfFile;
//    Context mContext;
//    PrintedPdfDocument mPdfDocument;
//
//    public PrintDocumentAdapter(Context context, File pdfFile) {
//        mPdfFile = pdfFile;
//        mContext = context;
//    }
//
//    @Override
//    public void onLayout(PrintAttributes printAttributes, PrintAttributes printAttributes1, CancellationSignal cancellationSignal, LayoutResultCallback layoutResultCallback, Bundle bundle) {
//        // Create a new PdfDocument with the requested page attributes
//        mPdfDocument = new PrintedPdfDocument(mContext, printAttributes);
//
//        // Respond to cancellation request
//        if (cancellationSignal.isCancelled()) {
//            callback.onLayoutCancelled();
//            return;
//        }
//
//        // Compute the expected number of printed pages
//        int pages = computePageCount(printAttributes);
//
//        if (pages > 0) {
//            // Return print information to print framework
//            PrintDocumentInfo info = new PrintDocumentInfo
//                    .Builder("print_output.pdf")
//                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
//                    .setPageCount(pages)
//                    .build();
//            // Content layout reflow is complete
//            callback.onLayoutFinished(info, true);
//        } else {
//            // Otherwise report an error to the print framework
//            callback.onLayoutFailed("Page count calculation failed.");
//        }
//    }
//
//    @Override
//    public void onWrite(PageRange[] pageRanges, ParcelFileDescriptor parcelFileDescriptor, CancellationSignal cancellationSignal, WriteResultCallback writeResultCallback) {
//        // Iterate over each page of the document,
//        // check if it's in the output range.
//        for (int i = 0; i < totalPages; i++) {
//            // Check to see if this page is in the output range.
//            if (containsPage(pageRanges, i)) {
//                // If so, add it to writtenPagesArray. writtenPagesArray.size()
//                // is used to compute the next output page index.
//                writtenPagesArray.append(writtenPagesArray.size(), i);
//                PdfDocument.Page page = mPdfDocument.startPage(i);
//
//                // check for cancellation
//                if (cancellationSignal.isCancelled()) {
//                    callback.onWriteCancelled();
//                    mPdfDocument.close();
//                    mPdfDocument = null;
//                    return;
//                }
//
//                // Draw page content for printing
//                drawPage(page);
//
//                // Rendering is complete, so page can be finalized.
//                mPdfDocument.finishPage(page);
//            }
//        }
//
//        // Write PDF document to file
//        try {
//            mPdfDocument.writeTo(new FileOutputStream(
//                    destination.getFileDescriptor()));
//        } catch (IOException e) {
//            callback.onWriteFailed(e.toString());
//            return;
//        } finally {
//            mPdfDocument.close();
//            mPdfDocument = null;
//        }
//        PageRange[] writtenPages = computeWrittenPages();
//        // Signal the print framework the document is complete
//        callback.onWriteFinished(writtenPages);
//    }
//
//    private int computePageCount(PrintAttributes printAttributes) {
//        int itemsPerPage = 4; // default item count for portrait mode
//
//        PrintAttributes.MediaSize pageSize = printAttributes.getMediaSize();
//        if (!pageSize.isPortrait()) {
//            // Six items per page in landscape orientation
//            itemsPerPage = 6;
//        }
//
//        // Determine number of print items
//        int printItemCount = getPrintItemCount();
//
//        return (int) Math.ceil(printItemCount / itemsPerPage);
//    }
//
//    private void drawPage(PdfDocument.Page page) {
//        Canvas canvas = page.getCanvas();
//
//        // units are in points (1/72 of an inch)
//        int titleBaseLine = 72;
//        int leftMargin = 54;
//
//        Paint paint = new Paint();
//        paint.setColor(Color.BLACK);
//        paint.setTextSize(36);
//        canvas.drawText("Test Title", leftMargin, titleBaseLine, paint);
//
//        paint.setTextSize(11);
//        canvas.drawText("Test paragraph", leftMargin, titleBaseLine + 25, paint);
//
//        paint.setColor(Color.BLUE);
//        canvas.drawRect(100, 100, 172, 172, paint);
//    }
}
