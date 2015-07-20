package com.zeapo.pwdstore.ssh;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.zeapo.pwdstore.R;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class SshKeyRecyclerAdapter extends RecyclerView.Adapter<SshKeyRecyclerAdapter.ViewHolder> {
    private final ArrayList<SshKeyItem> keys;
    public OnViewHolderClickListener listener;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public View view;
        public TextView name;
        public TextView repository;
        public ImageView privateKeyIcon;
        public ImageView publicKeyIcon;

        public ViewHolder(View view) {
            super(view);
            this.view = view;
            name = (TextView) view.findViewById(R.id.name);
            repository = (TextView) view.findViewById(R.id.repository);
            privateKeyIcon = (ImageView) view.findViewById(R.id.private_key_icon);
            publicKeyIcon = (ImageView) view.findViewById(R.id.public_key_icon);
            view.setOnClickListener(this);
            view.findViewById(R.id.action).setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (v instanceof ImageView) {
                listener.onImageClick((ImageView) v);
            } else {
                listener.onViewClick(v);
            }
        }
    }

    public interface OnViewHolderClickListener {
        void onImageClick(ImageView imageView);
        void onViewClick(View view);
    }

    public void setListener(OnViewHolderClickListener listener) {
        this.listener = listener;
    }

    public SshKeyRecyclerAdapter(ArrayList<SshKeyItem> keys) {
        this.keys = keys;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.ssh_key_row_layout, parent, false);
        return new ViewHolder(v);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        SshKeyItem key = keys.get(position);
        holder.name.setText(key.getName());

        if (!key.hasPublic()) {
            holder.publicKeyIcon.setVisibility(View.GONE);
        }

    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return this.keys.size();
    }

    public ArrayList<SshKeyItem> getKeys() {
        return this.keys;
    }

    public void clear() {
        int itemCount = getItemCount();
        this.keys.clear();
        this.notifyItemRangeRemoved(0, itemCount);
    }

    public void addAll(ArrayList<SshKeyItem> list) {
        int positionStart = getItemCount();
        this.keys.addAll(list);
        this.notifyItemRangeInserted(positionStart, list.size());
    }

    public void add(SshKeyItem item) {
        this.keys.add(item);
        this.notifyItemInserted(keys.size());
    }

    public void remove(int position) {
        this.keys.remove(position);
        this.notifyItemRemoved(position);
    }


}
