package tz.co.wadau.documentscanner.data;

import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;

import java.util.List;

import tz.co.wadau.documentscanner.models.Pdf;

public class FileDiffCallback extends DiffUtil.Callback {
    private List<Pdf> oldPDFList;
    private List<Pdf> newPDFList;

    public FileDiffCallback(List<Pdf> oldPDFList, List<Pdf> newPDFList) {
        this.oldPDFList = oldPDFList;
        this.newPDFList = newPDFList;
    }

    @Override
    public int getOldListSize() {
        return oldPDFList.size();
    }

    @Override
    public int getNewListSize() {
        return newPDFList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldPDFList.get(oldItemPosition).getAbsolutePath().equals(newPDFList.get(newItemPosition).getAbsolutePath());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return oldPDFList.get(oldItemPosition).equals(newPDFList.get(newItemPosition));
    }

    @Nullable
    @Override
    public Object getChangePayload(int oldItemPosition, int newItemPosition) {
        return super.getChangePayload(oldItemPosition, newItemPosition);
    }
}
