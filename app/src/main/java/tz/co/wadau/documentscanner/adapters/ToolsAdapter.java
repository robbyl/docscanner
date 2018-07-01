package tz.co.wadau.documentscanner.adapters;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;

import tz.co.wadau.documentscanner.R;
import tz.co.wadau.documentscanner.models.Tool;

public class ToolsAdapter extends RecyclerView.Adapter<ToolsAdapter.ToolsViewHolder> {

    private List<Tool> tools;
    private Context mContext;
    private OnToolClickListener toolClickListener;

    public class ToolsViewHolder extends RecyclerView.ViewHolder {

        public TextView title;
        public AppCompatImageView icon;
        public LinearLayout toolWrapper;


        public ToolsViewHolder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.tool_title);
            icon = itemView.findViewById(R.id.tool_icon);
            toolWrapper = itemView.findViewById(R.id.tool_wrapper);
        }
    }

    public ToolsAdapter(Context context, List<Tool> tools) {
        this.tools = tools;
        this.mContext = context;

        if (mContext instanceof OnToolClickListener) {
            toolClickListener = (OnToolClickListener) mContext;
        } else {
            throw new RuntimeException(mContext.toString() + " must implement OnToolClickListener");
        }
    }

    @Override
    public ToolsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View toolsViewHolder = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_tool, parent, false);
        return new ToolsViewHolder(toolsViewHolder);
    }

    @Override
    public void onBindViewHolder(final ToolsViewHolder holder, final int position) {
        Tool tool = tools.get(position);
        holder.title.setText(tool.getTitle());
        holder.icon.setBackgroundResource(tool.getDrawable());
        holder.icon.setImageDrawable(ContextCompat.getDrawable(mContext, tool.getDrawable()));

        holder.toolWrapper.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toolClicked(holder.getAdapterPosition());
            }
        });
    }


    @Override
    public int getItemCount() {
        return tools.size();
    }

    public interface OnToolClickListener {
        void onToolClicked(int position);
    }

    private void toolClicked(int position) {
        if (toolClickListener != null) {
            toolClickListener.onToolClicked(position);
        }
    }
}
