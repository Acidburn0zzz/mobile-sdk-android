/*
 *    Copyright 2013 APPNEXUS INC
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.appnexus.opensdk;

import android.content.Intent;
import android.webkit.WebView;
import com.appnexus.opensdk.shadows.ShadowAsyncTaskNoExecutor;
import com.appnexus.opensdk.shadows.ShadowWebSettings;
import com.appnexus.opensdk.shadows.ShadowWebView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ActivityController;

import static junit.framework.Assert.*;

@Config(shadows = {ShadowAsyncTaskNoExecutor.class,
        ShadowWebView.class, ShadowWebSettings.class},
        manifest = "../sdk/AndroidManifest.xml")
@RunWith(RobolectricTestRunner.class)
public class TestAdActivity extends BaseRoboTest {

    AdActivity adActivity;
    ActivityController<AdActivity> activityController;
    AdActivity.AdActivityImplementation implementation;

    @Override
    public void setup() {
        super.setup();
    }

    @Override
    public void tearDown() {
        super.tearDown();
        AdWebView.BROWSER_QUEUE.clear();
        adActivity = null;
        activityController = null;
        implementation = null;
    }

    // helper functions

    private void createActivity(String activityType) {
        Intent intent = new Intent(Robolectric.application, AdActivity.class);
        intent.putExtra(AdActivity.INTENT_KEY_ACTIVITY_TYPE, activityType);

        activityController = Robolectric.buildActivity(AdActivity.class)
                .withIntent(intent)
                .create().start().resume()
                .visible();
        adActivity = activityController.get();
    }

    private void createBrowserActivity() {
        // creating AdActivity creates an implementation,
        // which consumes a webview
        WebView webView = new WebView(Robolectric.application);
        AdWebView.BROWSER_QUEUE.add(webView);
        createActivity(AdActivity.ACTIVITY_TYPE_BROWSER);

        assertEquals(0, AdWebView.BROWSER_QUEUE.size());
    }

    private void createBrowserImplementation() {
        // create AdActivity object for implementation
        createBrowserActivity();

        // creating an implementation consumes a webview
        WebView webView = new WebView(Robolectric.application);
        AdWebView.BROWSER_QUEUE.add(webView);
        assertEquals(1, AdWebView.BROWSER_QUEUE.size());

        implementation = new BrowserAdActivity(adActivity);
        implementation.create();
        assertNotNull(implementation.getWebView());

        assertEquals(0, AdWebView.BROWSER_QUEUE.size());
    }

    private void runActivityLifecycle() {
        activityController.start();
        activityController.resume();
        activityController.pause();
        activityController.stop();
        activityController.destroy();
    }

    // AdActivity (type Browser) tests

    @Test
    public void testBrowserCreate() {
        createBrowserActivity();
        assertEquals(0, AdWebView.BROWSER_QUEUE.size());
    }

    @Test
    public void testBrowserDestroy() {
        testBrowserCreate();
        runActivityLifecycle();
        assertEquals(1, AdWebView.BROWSER_QUEUE.size());
    }

    @Test
    public void testBrowserBackPressToDestroy() {
        testBrowserCreate();
        adActivity.onBackPressed();
        runActivityLifecycle();
        assertEquals(0, AdWebView.BROWSER_QUEUE.size());
    }

    @Test
    public void testBrowserRotation() {
        testBrowserDestroy();

        createActivity(AdActivity.ACTIVITY_TYPE_BROWSER);
        assertEquals(0, AdWebView.BROWSER_QUEUE.size());

        adActivity.onBackPressed();
        runActivityLifecycle();
        assertEquals(0, AdWebView.BROWSER_QUEUE.size());
    }

    // BrowserAdActivity (implementation) tests

    @Test
    public void testBrowserImplementationCreate() {
        createBrowserImplementation();
    }

    @Test
    public void testBrowserImplementationDestroy() {
        testBrowserImplementationCreate();

        implementation.destroy();
        assertEquals(1, AdWebView.BROWSER_QUEUE.size());
    }

    @Test
    public void testBrowserImplementationBackPressToDestroy() {
        testBrowserImplementationCreate();

        implementation.backPressed();
        implementation.destroy();
        assertEquals(0, AdWebView.BROWSER_QUEUE.size());
    }

    @Test
    public void testBrowserImplementationRotation() {
        // original implementation. 'destroy' it as if rotating
        testBrowserImplementationDestroy();

        // create it again
        implementation = new BrowserAdActivity(adActivity);
        implementation.create();
        assertNotNull(implementation.getWebView());
        assertEquals(0, AdWebView.BROWSER_QUEUE.size());

        // actually destroy it this time
        implementation.backPressed();
        implementation.destroy();
        assertEquals(0, AdWebView.BROWSER_QUEUE.size());
    }

    @Test
    public void testBrowserImplementationCreateWithNullWebView() {
        // check for no crash
        createBrowserActivity();
        implementation = new BrowserAdActivity(adActivity);
        implementation.create();
        assertNull(implementation.getWebView());
        assertEquals(0, AdWebView.BROWSER_QUEUE.size());
    }

}
