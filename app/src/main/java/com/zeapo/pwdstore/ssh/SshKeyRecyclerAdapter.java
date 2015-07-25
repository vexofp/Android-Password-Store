package com.zeapo.pwdstore.ssh;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import com.zeapo.pwdstore.R;

import java.lang.reflect.Array;
import java.util.ArrayList;

public class SshKeyRecyclerAdapter extends RecyclerView.Adapter<SshKeyRecyclerAdapter.ViewHolder> {
    private final ArrayList<SshKeyItem> keys;
    public OnViewHolderClickListener listener;
    public RadioButton pickedKey;

    // Provide a reference to the views for each data item
    // Complex data items may need more than one view per item, and
    // you provide access to all the views for a data item in a view holder
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        public View view;
        public TextView name;
        // public TextView repository;
        public ImageView publicKeyIcon;
        public RadioButton pickKey;

        public ViewHolder(View view) {
            super(view);
            this.view = view;
            name = (TextView) view.findViewById(R.id.key_name);
            // repository = (TextView) view.findViewById(R.id.repository);
            publicKeyIcon = (ImageView) view.findViewById(R.id.public_key_icon);
            pickKey = (RadioButton) view.findViewById(R.id.pick_key);
            view.setOnClickListener(this);
            view.findViewById(R.id.show_key).setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (v instanceof ImageView) {
                listener.onImageClick(this.getLayoutPosition(), (ImageView) v);
            } else {
                listener.onViewClick(this.getLayoutPosition(), v);
            }
        }
    }

    public interface OnViewHolderClickListener {
        void onImageClick(int position, ImageView imageView);
        void onViewClick(int position, View view);
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

        final SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(holder.view.getContext());
        final String sshKeyName = settings.getString("ssh_key_name", ".ssh_key");
        if (holder.name.getText().equals(sshKeyName)) {
            holder.pickKey.setChecked(true);
            pickedKey = holder.pickKey;
        }

        holder.pickKey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RadioButton radioButton = (RadioButton) v;
                if (!(holder.name.getText()).equals(sshKeyName)) {
                    if (pickedKey != null) {
                        pickedKey.setChecked(false);
                    }
                    radioButton.setChecked(true);
                    pickedKey = radioButton;
                    settings.edit().putString("ssh_key_name", holder.name.getText().toString()).apply();
                }
            }
        });
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
