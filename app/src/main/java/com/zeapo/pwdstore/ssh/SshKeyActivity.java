package com.zeapo.pwdstore.ssh;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.zeapo.pwdstore.R;

import org.apache.commons.io.filefilter.FileFilterUtils;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;

import static org.apache.commons.io.FilenameUtils.getExtension;

public class SshKeyActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private SshKeyRecyclerAdapter recyclerAdapter;
    private RecyclerView.LayoutManager layoutManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ssh_key_recycler_view);
        recyclerView = (RecyclerView) findViewById(R.id.ssh_key_recycler);

        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        recyclerAdapter = new SshKeyRecyclerAdapter(getSshKeys());
        recyclerView.setAdapter(recyclerAdapter);

        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL_LIST));

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });

        recyclerAdapter.setListener(new SshKeyRecyclerAdapter.OnViewHolderClickListener() {
            @Override
            public void onImageClick(ImageView imageView) {
                Log.d("click", "image");
            }

            @Override
            public void onViewClick(View view) {
                Log.d("click", "elsewhere");
            }
        });
    }

    public ArrayList<SshKeyItem> getSshKeys() {
        File path = new File(getFilesDir() + "/ssh_keys/");
        File[] directories = path.listFiles((FileFilter) FileFilterUtils.directoryFileFilter());

        if (directories == null) return new ArrayList<>();

        ArrayList<SshKeyItem> keyList = new ArrayList<>();
        Arrays.sort(directories);
        for (File dir : directories) {
            File[] keys = dir.listFiles((FileFilter) FileFilterUtils.fileFileFilter());
            if (keys == null) continue;
            SshKeyItem keyItem = new SshKeyItem(dir.getName());
            for (File key : keys) {
                if (getExtension(key.getName()).equals("pub")) {
                    keyItem.setPublic(key);
                } else {
                    keyItem.setPrivate(key);
                }
            }
            keyList.add(keyItem);
        }
        return keyList;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return true;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                filterAdapter(s);
                return true;
            }
        });

        // When using the support library, the setOnActionExpandListener() method is
        // static and accepts the MenuItem object as an argument
        MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                refreshAdapter();
                return true;
            }

            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    public void refreshAdapter () {
        recyclerAdapter.clear();
        recyclerAdapter.addAll(getSshKeys());
    }

    public void filterAdapter (String filter) {
        if (filter.isEmpty()) {
            refreshAdapter();
        } else {
            ArrayList<SshKeyItem> items = getSshKeys();
            for (SshKeyItem item : items) {
                boolean matches = item.getName().toLowerCase().contains(filter.toLowerCase());
                boolean inAdapter = recyclerAdapter.getKeys().contains(item);
                if (matches && !inAdapter) {
                    recyclerAdapter.add(item);
                } else if (!matches && inAdapter) {
                    recyclerAdapter.remove(recyclerAdapter.getKeys().indexOf(item));
                }
            }
        }
    }

}

