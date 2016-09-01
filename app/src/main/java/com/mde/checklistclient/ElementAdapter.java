package com.mde.checklistclient;

import android.content.Context;
import android.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.TextView;

import com.mde.checklistclient.net.models.Task;

import java.util.List;

public class ElementAdapter extends RecyclerView.Adapter<ElementAdapter.ViewHolder> {

    private final List<Task> elements;
    private final LayoutInflater mInflator;

    public ElementAdapter(Context context, List<Task> elements) {
        this.elements = elements;
        this.mInflator = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        // each data item is just a string in this case
        public View view;
        public CheckBox isDone;
        public TextView descriptionView;
        public ImageButton menuButton;

        public ViewHolder(View v) {
            super(v);

            view = v;
            descriptionView = (TextView) view.findViewById(R.id.element_description);
            isDone = (CheckBox) view.findViewById(R.id.element_checked);
            menuButton = (ImageButton) view.findViewById(R.id.dropdown_menu);

            menuButton.setOnClickListener(x -> {
                PopupMenu popupMenu = new PopupMenu(view.getContext(), menuButton);
                popupMenu.getMenuInflater().inflate(R.menu.item_menu, popupMenu.getMenu());
                popupMenu.show();
            });
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View inflate = mInflator.inflate(R.layout.list_element, parent, false);

        return new ViewHolder(inflate);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Task task = elements.get(position);

        holder.isDone.setChecked(task.isCompleted());
        holder.descriptionView.setText(task.getDescription());
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public int getItemCount() {
        return elements.size();
    }
}
