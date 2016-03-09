// Copyright (c) 2013-2014 Intel Corporation. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.

package org.xwalk.core.xwview.shell;

import java.util.HashMap;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.drawable.ClipDrawable;
import android.os.Bundle;
import android.os.Looper;
import android.os.MessageQueue;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import org.chromium.base.BaseSwitches;
import org.chromium.base.CommandLine;
import org.chromium.base.library_loader.LibraryLoader;
import org.chromium.content.browser.TracingControllerAndroid;
import org.xwalk.core.XWalkActivity;
import org.xwalk.core.XWalkNavigationHistory;
import org.xwalk.core.XWalkPreferences;
import org.xwalk.core.XWalkResourceClient;
import org.xwalk.core.XWalkUIClient;
import org.xwalk.core.XWalkView;
import android.widget.RelativeLayout;
import android.graphics.Color;
import android.view.MotionEvent;


public class XWalkViewShellActivity extends XWalkActivity {
    public static final String COMMAND_LINE_FILE = "/data/local/tmp/xwview-shell-command-line";
    private static final String TAG = XWalkViewShellActivity.class.getName();
    public static final String COMMAND_LINE_ARGS_KEY = "commandLineArgs";
    private static final long COMPLETED_PROGRESS_TIMEOUT_MS = 200;
    private static final String ACTION_LAUNCH_URL = "org.xwalk.core.xwview.shell.launch";

    private XWalkView mXWalkView;
    private TracingControllerAndroid mTracingController;
    private BroadcastReceiver mReceiver;
	private static String TARGET_URL = "file:///storage/emulated/0/index4809-white.html";
	boolean mReady = false;



    TracingControllerAndroid getTracingController() {
        if (mTracingController == null) {
            mTracingController = new TracingControllerAndroid(this);
        }
        return mTracingController;
    }

    private void registerTracingReceiverWhenIdle() {
        // Delay tracing receiver registration until the main loop is idle.
        Looper.myQueue().addIdleHandler(new MessageQueue.IdleHandler() {
            @Override
            public boolean queueIdle() {
                // Will retry if the native library is not initialized yet.
                if (!LibraryLoader.isInitialized()) return true;
                try {
                    getTracingController().registerReceiver(XWalkViewShellActivity.this);
                } catch (SecurityException e) {
                    Log.w(TAG, "failed to register tracing receiver: " + e.getMessage());
                }
                return false;
            }
        });
    }

    private void unregisterTracingReceiver() {
        try {
            getTracingController().unregisterReceiver(this);
        } catch (SecurityException e) {
            Log.w(TAG, "failed to unregister tracing receiver: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "failed to unregister tracing receiver: " + e.getMessage());
        }
    }

    protected void onStart() {
        super.onStart();
		Log.w(TAG, "aalife onStart");
    }


    protected void onResume() {
        super.onResume();
		Log.w(TAG, "aalife onResume");
		if (mReady == true)
		mXWalkView.onShow();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		Log.w(TAG, "aalife onCreate");
        registerTracingReceiverWhenIdle();

        if (!CommandLine.isInitialized()) {
            CommandLine.initFromFile(COMMAND_LINE_FILE);
            String[] commandLineParams = getCommandLineParamsFromIntent(getIntent());
            if (commandLineParams != null) {
                CommandLine.getInstance().appendSwitchesAndArguments(commandLineParams);
            }
        }
        waitForDebuggerIfNeeded();
		mXWalkView = new XWalkView(this, this);
		//mXWalkView.setBackgroundColor(Color.RED);
		RelativeLayout root = new RelativeLayout(this);
		root.addView(mXWalkView);
        setContentView(root);



        IntentFilter intentFilter = new IntentFilter(ACTION_LAUNCH_URL);
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle bundle = intent.getExtras();
                if (bundle == null)
                    return;

                if (bundle.containsKey("url")) {
                    String extra = bundle.getString("url");
                    if (mXWalkView != null)
                        mXWalkView.load(sanitizeUrl(extra), null);
                }
            }
        };
        registerReceiver(mReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mReceiver != null) unregisterReceiver(mReceiver);
        unregisterTracingReceiver();
		mReady = false;
		Log.w(TAG, "aalife onDestroy");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (mXWalkView != null) mXWalkView.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onNewIntent(Intent intent) {
        if (mXWalkView != null) {
            if (!mXWalkView.onNewIntent(intent)) super.onNewIntent(intent);
        }
    }

    private void waitForDebuggerIfNeeded() {
        if (CommandLine.getInstance().hasSwitch(BaseSwitches.WAIT_FOR_JAVA_DEBUGGER)) {
            Log.e(TAG, "Waiting for Java debugger to connect...");
            android.os.Debug.waitForDebugger();
            Log.e(TAG, "Java debugger connected. Resuming execution.");
        }
    }

    private static String[] getCommandLineParamsFromIntent(Intent intent) {
        return intent != null ? intent.getStringArrayExtra(COMMAND_LINE_ARGS_KEY) : null;
    }

    private static String sanitizeUrl(String url) {
        if (url == null) return url;
        if (url.startsWith("www.") || url.indexOf(":") == -1) url = "http://" + url;
        return url;
    }

    private void initializeXWalkViewClients(XWalkView xwalkView) {
        xwalkView.setResourceClient(new XWalkResourceClient(xwalkView) {
            @Override
            public void onProgressChanged(XWalkView view, int newProgress) {
                if (view != mXWalkView) return;

            }

            @Override
            public void onLoadFinished(XWalkView view, String url) {
                if (view != mXWalkView) return;
            }
        });

        xwalkView.setUIClient(new XWalkUIClient(xwalkView) {
            @Override
            public void onReceivedTitle(XWalkView view, String title) {
            }
        });
    }


    @Override
    public void onXWalkReady() {
        XWalkPreferences.setValue(XWalkPreferences.REMOTE_DEBUGGING, true);
		mXWalkView.load(TARGET_URL, null);
		mReady = true;
		Log.w(TAG, "sswap onXWalkReady");
		mXWalkView.setBackgroundColor(Color.RED);
    }
    @Override
	public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
				Log.w(TAG, "sswap onTouchEvent");
				mXWalkView.setBackgroundColor(Color.BLUE);
				break;
        }
        return true;
    }
	@Override
public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
				Log.w(TAG, "sswap dispatchTouchEvent");
				mXWalkView.setBackgroundColor(Color.BLUE);
				break;
        }
    return super.dispatchTouchEvent(event);
} 
}
