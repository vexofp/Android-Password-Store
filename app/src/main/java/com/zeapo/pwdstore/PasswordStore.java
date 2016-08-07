package com.zeapo.pwdstore;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.zeapo.pwdstore.crypto.PgpHandler;
import com.zeapo.pwdstore.git.GitActivity;
import com.zeapo.pwdstore.git.GitAsyncTask;
import com.zeapo.pwdstore.pwgen.PRNGFixes;
import com.zeapo.pwdstore.store.StoreManager;
import com.zeapo.pwdstore.utils.PasswordItem;
import com.zeapo.pwdstore.utils.PasswordRecyclerAdapter;
import com.zeapo.pwdstore.utils.PasswordRepository;

import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class PasswordStore extends AppCompatActivity {
    private static final String TAG = "PwdStrAct";
    private File currentDir;
    private SharedPreferences settings;
    private Activity activity;
    private PasswordFragment plist;
    private AlertDialog selectDestinationDialog;

    private StoreManager storeManager;

    private final static int CLONE_REPO_BUTTON = 401;
    private final static int NEW_REPO_BUTTON = 402;
    private final static int HOME = 403;

    private final static int REQUEST_EXTERNAL_STORAGE = 50;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        PRNGFixes.apply();

        super.onCreate(savedInstanceState);

        settings = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        activity = this;
        storeManager = new StoreManager(activity);

        setContentView(R.layout.activity_pwdstore);
    }

    @Override
    public void onResume() {
        super.onResume();
        // do not attempt to initView() if no storage permission: immediate crash
        if (settings.getString("store_location", "external").equals("external")) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    Snackbar snack = Snackbar.make(findViewById(R.id.main_layout),
                            "The store is on the sdcard but the app does not have permission to access it. Please give permission.",
                            Snackbar.LENGTH_INDEFINITE);
                    snack.setAction(R.string.dialog_ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            ActivityCompat.requestPermissions(activity,
                                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                                    REQUEST_EXTERNAL_STORAGE);
                        }
                    });
                    snack.show();
                    View view = snack.getView();
                    TextView tv = (TextView) view.findViewById(android.support.design.R.id.snackbar_text);
                    tv.setTextColor(Color.WHITE);
                    tv.setMaxLines(10);
                } else {
                    // No explanation needed, we can request the permission.
                    ActivityCompat.requestPermissions(activity,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            REQUEST_EXTERNAL_STORAGE);
                }
            } else {
                initView();
            }

        } else {
            initView();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initView();
                }
            }
        }
    }

    /**
     * The main entry point to either show the store content or the welcome screen
     */
    private void initView() {
        File activeStore = storeManager.getActiveStore();

        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();

        // if store_path is empty, then no active store is selected
        if (!settings.getString("store_path", "").isEmpty()) {
            Log.d("PassStr", "Current store: " + activeStore.getAbsolutePath());

            // do not push the fragment if we already have it
            if (fragmentManager.findFragmentByTag("PasswordsList") == null || settings.getBoolean("repo_changed", false)) {
                settings.edit().putBoolean("repo_changed", false).apply();

                // todo move this as it is duplicated upthere!
                if (fragmentManager.findFragmentByTag("PasswordsList") != null) {
                    fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                }

                // clean things up
                if (fragmentManager.findFragmentByTag("ToCloneOrNot") != null) {
                    fragmentManager.popBackStack();
                }

                plist = new PasswordFragment();
                Bundle args = new Bundle();
                args.putString("Path", storeManager.getActiveStore().getAbsolutePath());

                // if the activity was started from the autofill settings, the
                // intent is to match a clicked pwd with app. pass this to fragment
                if (getIntent().getBooleanExtra("matchWith", false)) {
                    args.putBoolean("matchWith", true);
                }

                plist.setArguments(args);

                fragmentTransaction.addToBackStack("passlist");

                // this is a trick to stop the warning
                assert getSupportActionBar() != null;
                getSupportActionBar().show();

                fragmentTransaction.replace(R.id.main_layout, plist, "PasswordsList");
                fragmentTransaction.commit();
            }
        } else {
            // if we still have the pass list (after deleting the repository for instance) remove it
            if (fragmentManager.findFragmentByTag("PasswordsList") != null) {
                fragmentManager.popBackStack();
            }

            // this is a trick to stop the warning
            assert getSupportActionBar() != null;
            getSupportActionBar().hide();

            ToCloneOrNot cloneFrag = new ToCloneOrNot();
            fragmentTransaction.replace(R.id.main_layout, cloneFrag, "ToCloneOrNot");
            fragmentTransaction.commit();
        }
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
                filterListAdapter(s);
                return true;
            }
        });

        // When using the support library, the setOnActionExpandListener() method is
        // static and accepts the MenuItem object as an argument
        MenuItemCompat.setOnActionExpandListener(searchItem, new MenuItemCompat.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                refreshListAdapter();
                return true;
            }

            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }
        });
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        Intent intent;
        Log.d("PASS", "Menu item " + id + " pressed");

        AlertDialog.Builder initBefore = new AlertDialog.Builder(this)
                .setMessage(this.getResources().getString(R.string.creation_dialog_text))
                .setPositiveButton(this.getResources().getString(R.string.dialog_ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                    }
                });

        switch (id) {
            case R.id.user_pref:
                try {
                    intent = new Intent(this, UserPreference.class);
                    startActivity(intent);
                } catch (Exception e) {
                    System.out.println("Exception caught :(");
                    e.printStackTrace();
                }
                return true;
            case R.id.git_push:
                if (storeManager.getActiveStoreVersionning() != StoreManager.STORE_TYPE_GIT) {
                    // todo warn that the store has to be a git repo (non-bare)
                    initBefore.show();
                    break;
                }

                intent = new Intent(this, GitActivity.class);
                intent.putExtra("Operation", GitActivity.REQUEST_PUSH);
                startActivityForResult(intent, GitActivity.REQUEST_PUSH);
                return true;

            case R.id.git_pull:
                if (storeManager.getActiveStoreVersionning() != StoreManager.STORE_TYPE_GIT) {
                    // todo warn that the store has to be a git repo (non-bare)
                    initBefore.show();
                    break;
                }

                intent = new Intent(this, GitActivity.class);
                intent.putExtra("Operation", GitActivity.REQUEST_PULL);
                startActivityForResult(intent, GitActivity.REQUEST_PULL);
                return true;

            case R.id.git_sync:
                if (storeManager.getActiveStoreVersionning() != StoreManager.STORE_TYPE_GIT) {
                    // todo warn that the store has to be a git repo (non-bare)
                    initBefore.show();
                    break;
                }

                intent = new Intent(this, GitActivity.class);
                intent.putExtra("Operation", GitActivity.REQUEST_SYNC);
                startActivityForResult(intent, GitActivity.REQUEST_SYNC);
                return true;

            case R.id.refresh:
                updateListAdapter();
                return true;

            case android.R.id.home:
                Log.d("PASS", "Home pressed");
                this.onBackPressed();
                break;

            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void openSettings(View view) {
        Intent intent;

        try {
            intent = new Intent(this, UserPreference.class);
            startActivity(intent);
        } catch (Exception e) {
            System.out.println("Exception caught :(");
            e.printStackTrace();
        }
    }

    public void cloneExistingRepository(View view) {
        settings.edit().putString("store_path", storeManager.getActiveStorePath()).apply();
        initView();
        // NOT IMPLEMENTED YET
//        initRepository(CLONE_REPO_BUTTON);
    }

    public void createNewRepository(View view) {
        settings.edit().putString("store_path", storeManager.getActiveStorePath()).apply();
        initView();
        // NOT IMPLEMENTED YET
//        initRepository(NEW_REPO_BUTTON);
    }

    private void createRepository() {
//        if (!PasswordRepository.isInitialized()) {
//            PasswordRepository.initialize(this);
//        }
//
//        File localDir = PasswordRepository.getWorkTree();
//
//        localDir.mkdir();
//        try {
//            PasswordRepository.createRepository(localDir);
//            new File(localDir.getAbsolutePath() + "/.gpg-id").createNewFile();
//            settings.edit().putBoolean("repository_initialized", true).apply();
//        } catch (Exception e) {
//            e.printStackTrace();
//            localDir.delete();
//            return;
//        }
//        initView();
    }

    public void initializeRepositoryInfo() {
//        if (settings.getBoolean("git_external", false) && settings.getString("git_external_repo", null) != null) {
//            File dir = new File(settings.getString("git_external_repo", null));
//
//            if (dir.exists() && dir.isDirectory() && !FileUtils.listFiles(dir, null, true).isEmpty() &&
//                    !PasswordRepository.getPasswords(dir, PasswordRepository.getRepositoryDirectory(this)).isEmpty()) {
//                PasswordRepository.closeRepository();
//                initView();
//                return; // if not empty, just show me the passwords!
//            }
//        }
//
//        final Set<String> keyIds = settings.getStringSet("openpgp_key_ids_set", new HashSet<String>());
//
//        if (keyIds.isEmpty())
//            new AlertDialog.Builder(this)
//                    .setMessage(this.getResources().getString(R.string.key_dialog_text))
//                    .setPositiveButton(this.getResources().getString(R.string.dialog_ok), new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialogInterface, int i) {
//                            Intent intent = new Intent(activity, UserPreference.class);
//                            startActivityForResult(intent, GitActivity.REQUEST_INIT);
//                        }
//                    })
//                    .setNegativeButton(this.getResources().getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialogInterface, int i) {
//                            // do nothing :(
//                        }
//                    })
//                    .show();
//
//        createRepository();
    }


    @Override
    public void onBackPressed() {
        if ((null != plist) && plist.isNotEmpty()) {
            plist.popBack();
        } else {
            super.onBackPressed();
        }

        if (null != plist && !plist.isNotEmpty()) {
            assert getSupportActionBar() != null;
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    public void decryptPassword(PasswordItem item) {
        Intent intent = new Intent(this, PgpHandler.class);
        intent.putExtra("NAME", item.toString());
        intent.putExtra("FILE_PATH", item.getFile().getAbsolutePath());
        intent.putExtra("Operation", "DECRYPT");
        startActivityForResult(intent, PgpHandler.REQUEST_CODE_DECRYPT_AND_VERIFY);
    }

    public void editPassword(PasswordItem item) {
        Intent intent = new Intent(this, PgpHandler.class);
        intent.putExtra("NAME", item.toString());
        intent.putExtra("FILE_PATH", item.getFile().getAbsolutePath());
        intent.putExtra("Operation", "EDIT");
        startActivityForResult(intent, PgpHandler.REQUEST_CODE_EDIT);
    }

    public void createPassword() {
        Log.d("PwdStr", "Adding file to : " + storeManager.getActiveStore().getAbsolutePath());

        Intent intent = new Intent(this, PgpHandler.class);
        intent.putExtra("FILE_PATH", storeManager.getActiveStore().getAbsolutePath());
        intent.putExtra("Operation", "ENCRYPT");
        startActivityForResult(intent, PgpHandler.REQUEST_CODE_ENCRYPT);
    }

    // deletes passwords in order from top to bottom
    public void deletePasswords(final PasswordRecyclerAdapter adapter, final Set<Integer> selectedItems) {
        final Iterator it = selectedItems.iterator();
        if (!it.hasNext()) {
            return;
        }
        final int position = (int) it.next();
        final PasswordItem item = adapter.getValues().get(position);
        new AlertDialog.Builder(this).
                setMessage(this.getResources().getString(R.string.delete_dialog_text) +
                        item + "\"")
                .setPositiveButton(this.getResources().getString(R.string.dialog_yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String path = item.getFile().getAbsolutePath();
                        item.getFile().delete();
                        adapter.remove(position);
                        it.remove();
                        adapter.updateSelectedItems(position, selectedItems);

                        setResult(RESULT_CANCELED);

                        if (storeManager.getActiveStoreVersionning() == StoreManager.STORE_TYPE_GIT) {
                            //  todo move this code into a function
                            FileRepositoryBuilder builder = new FileRepositoryBuilder();
                            try {
                                Repository repo = builder.setGitDir(new File(storeManager.getActiveStore(), ".git")).readEnvironment().build();

                                Git git = new Git(repo);
                                GitAsyncTask tasks = new GitAsyncTask(activity, false, true, CommitCommand.class);
                                tasks.execute(
                                        git.rm().addFilepattern(path.replace(PasswordRepository.getWorkTree() + "/", "")),
                                        git.commit().setMessage("[ANDROID PwdStore] Remove " + item + " from store.")
                                );
                            } catch (Exception e) {
                                // should not happen
                            }
                        }
                        deletePasswords(adapter, selectedItems);
                    }
                })
                .setNegativeButton(this.getResources().getString(R.string.dialog_no), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        it.remove();
                        deletePasswords(adapter, selectedItems);
                    }
                })
                .show();
    }

    public void movePasswords(ArrayList<PasswordItem> values) {
        Intent intent = new Intent(this, PgpHandler.class);
        ArrayList<String> fileLocations = new ArrayList<>();
        for (PasswordItem passwordItem : values){
            fileLocations.add(passwordItem.getFile().getAbsolutePath());
        }
        intent.putExtra("Files",fileLocations);
        intent.putExtra("Operation", "SELECTFOLDER");
        startActivityForResult(intent, PgpHandler.REQUEST_CODE_SELECT_FOLDER);
    }

    /**
     * clears adapter's content and updates it with a fresh list of passwords from the root
     */
    public void updateListAdapter() {
        if ((null != plist)) {
            plist.updateAdapter();
        }
    }

    /**
     * Updates the adapter with the current view of passwords
     */
    public void refreshListAdapter() {
        if ((null != plist)) {
            plist.refreshAdapter();
        }
    }

    public void filterListAdapter(String filter) {
        if ((null != plist)) {
            plist.filterAdapter(filter);
        }
    }

    private File getCurrentDir() {
        if ((null != plist)) {
            return plist.getCurrentDir();
        }
        return PasswordRepository.getWorkTree();
    }

    private void commit(String message) {
        //  todo move this code into a function
        FileRepositoryBuilder builder = new FileRepositoryBuilder();
        try {
            Repository repo = builder.setGitDir(new File(storeManager.getActiveStore(), ".git")).readEnvironment().build();

            Git git = new Git(repo);
            GitAsyncTask tasks = new GitAsyncTask(this, false, false, CommitCommand.class);
            tasks.execute(
                    git.add().addFilepattern("."),
                    git.commit().setMessage(message)
            );
        } catch (Exception e) {
            // should not happen
        }
    }

    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case GitActivity.REQUEST_CLONE:
                    // if we get here with a RESULT_OK then it's probably OK :)
                    settings.edit().putBoolean("repository_initialized", true).apply();
                    break;
                case PgpHandler.REQUEST_CODE_DECRYPT_AND_VERIFY:
                    // if went from decrypt->edit and user saved changes, we need to commit
                    if (data.getBooleanExtra("needCommit", false)) {
                        if (storeManager.getActiveStoreVersionning() == StoreManager.STORE_TYPE_GIT) {
                            commit(this.getResources().getString(R.string.edit_commit_text) + data.getExtras().getString("NAME"));
                        }
                        refreshListAdapter();
                    }
                    break;
                case PgpHandler.REQUEST_CODE_ENCRYPT:
                    if (storeManager.getActiveStoreVersionning() == StoreManager.STORE_TYPE_GIT) {
                        commit(this.getResources().getString(R.string.add_commit_text) + data.getExtras().getString("NAME") + this.getResources().getString(R.string.from_store));
                    }
                    refreshListAdapter();
                    break;
                case PgpHandler.REQUEST_CODE_EDIT:
                    if (storeManager.getActiveStoreVersionning() == StoreManager.STORE_TYPE_GIT) {
                        commit(this.getResources().getString(R.string.edit_commit_text) + data.getExtras().getString("NAME"));
                    }
                    refreshListAdapter();
                    break;
                case GitActivity.REQUEST_INIT:
                    initializeRepositoryInfo();
                    break;
                case GitActivity.REQUEST_SYNC:
                case GitActivity.REQUEST_PULL:
                    updateListAdapter();
                    break;
                case HOME:
                    initView();
                    break;
                case NEW_REPO_BUTTON:
                    initializeRepositoryInfo();
                    break;
                case CLONE_REPO_BUTTON:
                    // duplicate code
                    if (settings.getBoolean("git_external", false) && settings.getString("git_external_repo", null) != null) {
                        String externalRepoPath = settings.getString("git_external_repo", null);
                        File dir = externalRepoPath != null ? new File(externalRepoPath) : null;

                        if (dir != null &&
                                dir.exists() &&
                                dir.isDirectory() &&
                                !FileUtils.listFiles(dir, null, true).isEmpty() &&
                                !PasswordRepository.getPasswords(dir, PasswordRepository.getRepositoryDirectory(this)).isEmpty()) {
                            PasswordRepository.closeRepository();
                            initView();
                            return; // if not empty, just show me the passwords!
                        }
                    }
                    Intent intent = new Intent(activity, GitActivity.class);
                    intent.putExtra("Operation", GitActivity.REQUEST_CLONE);
                    startActivityForResult(intent, GitActivity.REQUEST_CLONE);
                    break;
                case PgpHandler.REQUEST_CODE_SELECT_FOLDER:
                    Log.d("Moving","Moving passwords to "+data.getStringExtra("SELECTED_FOLDER_PATH"));
                    Log.d("Moving", TextUtils.join(", ", data.getStringArrayListExtra("Files")));
                    File target = new File(data.getStringExtra("SELECTED_FOLDER_PATH"));
                    if (!target.isDirectory()){
                        Log.e("Moving","Tried moving passwords to a non-existing folder.");
                        break;
                    }

                    Repository repo = PasswordRepository.getRepository(PasswordRepository.getRepositoryDirectory(activity));
                    Git git = new Git(repo);
                    GitAsyncTask tasks = new GitAsyncTask(activity, false, true, CommitCommand.class);

                    for (String string : data.getStringArrayListExtra("Files")){
                        File source = new File(string);
                        if (!source.exists()){
                            Log.e("Moving","Tried moving something that appears non-existent.");
                            continue;
                        }
                        if (!source.renameTo(new File(target.getAbsolutePath()+"/"+source.getName()))){
                            Log.e("Moving","Something went wrong while moving.");
                        }else{
                            tasks.execute(
                                    git.add().addFilepattern(source.getAbsolutePath().replace(PasswordRepository.getWorkTree() + "/", "")),
                                    git.commit().setMessage("[ANDROID PwdStore] Moved "+string.replace(PasswordRepository.getWorkTree() + "/", "")+" to "+target.getAbsolutePath().replace(PasswordRepository.getWorkTree() + "/","")+target.getAbsolutePath()+"/"+source.getName()+".")
                            );
                        }
                    }
                    updateListAdapter();
                    break;
            }
        }
    }

    protected void initRepository(final int operation) {
        PasswordRepository.closeRepository();

        new AlertDialog.Builder(this)
                .setTitle("Repository location")
                .setMessage("Select where to create or clone your password repository.")
                .setPositiveButton("Hidden (preferred)", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        settings.edit().putBoolean("git_external", false).apply();

                        switch (operation) {
                            case NEW_REPO_BUTTON:
                                initializeRepositoryInfo();
                                break;
                            case CLONE_REPO_BUTTON:
                                PasswordRepository.initialize(PasswordStore.this);

                                Intent intent = new Intent(activity, GitActivity.class);
                                intent.putExtra("Operation", GitActivity.REQUEST_CLONE);
                                startActivityForResult(intent, GitActivity.REQUEST_CLONE);
                                break;
                        }
                    }
                })
                .setNegativeButton("SD-Card", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        settings.edit().putBoolean("git_external", true).apply();

                        if (settings.getString("git_external_repo", null) == null) {
                            Intent intent = new Intent(activity, UserPreference.class);
                            intent.putExtra("operation", "git_external");
                            startActivityForResult(intent, operation);
                        } else {
                            new AlertDialog.Builder(activity).
                                    setTitle("Directory already selected").
                                    setMessage("Do you want to use \"" + settings.getString("git_external_repo", null) + "\"?").
                                    setPositiveButton("Use", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            switch (operation) {
                                                case NEW_REPO_BUTTON:
                                                    initializeRepositoryInfo();
                                                    break;
                                                case CLONE_REPO_BUTTON:
                                                    PasswordRepository.initialize(PasswordStore.this);

                                                    Intent intent = new Intent(activity, GitActivity.class);
                                                    intent.putExtra("Operation", GitActivity.REQUEST_CLONE);
                                                    startActivityForResult(intent, GitActivity.REQUEST_CLONE);
                                                    break;
                                            }
                                        }
                                    }).
                                    setNegativeButton("Change", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = new Intent(activity, UserPreference.class);
                                            intent.putExtra("operation", "git_external");
                                            startActivityForResult(intent, operation);
                                        }
                                    }).show();
                        }
                    }
                })
                .show();
    }

    public void matchPasswordWithApp(PasswordItem item) {
        String path = item.getFile().getAbsolutePath();
        path = path.replace(PasswordRepository.getWorkTree() + "/", "").replace(".gpg", "");
        Intent data = new Intent();
        data.putExtra("path", path);
        setResult(RESULT_OK, data);
        finish();
    }
}
