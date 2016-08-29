package com.mde.checklistclient;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import com.mde.checklistclient.net.models.Task;

import java.util.List;

public class ElementAdapter extends BaseAdapter {

    private final Context context;
    private final List<Task> elements;
    private final LayoutInflater mInflator;

    public ElementAdapter(Context context, List<Task> elements) {
        this.context = context;
        this.elements = elements;
        this.mInflator = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return elements.size();
    }

    @Override
    public Object getItem(int i) {
        return elements.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        View view = convertView;
        if (view == null) {
            view = mInflator.inflate(R.layout.list_element, viewGroup, false);
        }

        TextView descriptionView = (TextView) view.findViewById(R.id.element_description);
        descriptionView.setText(elements.get(i).getDescription());

        CheckBox isDone = (CheckBox) view.findViewById(R.id.element_checked);
        isDone.setChecked(elements.get(i).isCompleted());

        return view;
    }
}
