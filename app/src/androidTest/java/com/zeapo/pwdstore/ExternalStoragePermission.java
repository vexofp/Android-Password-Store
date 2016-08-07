package com.zeapo.pwdstore;

import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * To write, check this http://blog.egorand.me/testing-runtime-permissions-lessons-learned/
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ExternalStoragePermission {
    @Rule
    public ActivityTestRule<PasswordStore> mActivityRule = new ActivityTestRule<>(PasswordStore.class);

    @Test
    public void permissionIsRequested() {
    }


}
