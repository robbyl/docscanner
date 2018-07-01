package tz.co.wadau.documentscanner.data;

import java.util.ArrayList;
import java.util.List;

import tz.co.wadau.documentscanner.R;
import tz.co.wadau.documentscanner.models.Tool;

public class ToolsData {

    public static List<Tool> getTools() {
        List<Tool> tools = new ArrayList<>();

        tools.add(new Tool(1, "Merge", "#6db9e5", R.drawable.ic_action_merge));
        tools.add(new Tool(2, "Split", "#9ccc66", R.drawable.ic_action_split));
        tools.add(new Tool(3, "Extract Images", "#ffb74d", R.drawable.ic_action_extract_images));
        tools.add(new Tool(4, "Save as Pictures", "#7986cb", R.drawable.ic_action_save_photos));
        tools.add(new Tool(5, "Organize Pages", "#78909c", R.drawable.ic_action_reorder));
        tools.add(new Tool(6, "Edit Metadata", "#78909c", R.drawable.ic_action_edit_metadata));
        tools.add(new Tool(7, "Compress", "#7ecdc8", R.drawable.ic_action_compress));
        tools.add(new Tool(8, "Extract Text", "#9761a9", R.drawable.ic_action_extract_text));
        tools.add(new Tool(9, "Images to PDF", "#f2af49", R.drawable.ic_action_image_to_pdf));
        tools.add(new Tool(10, "Protect", "#7986cb", R.drawable.ic_action_protect));
        tools.add(new Tool(11, "Unprotect", "#7ecdc8", R.drawable.ic_action_unprotect));
        tools.add(new Tool(12, "Stamp", "#6db9e5", R.drawable.ic_action_stamp));
        return tools;
    }
}
