package com.cylonid.nativealpha.ui

import dagger.hilt.android.AndroidEntryPoint

/**
 * Per-app sandbox WebView activities.
 *
 * Each variant runs in its own dedicated Android process (`:webapp_N`) declared in
 * AndroidManifest.xml. Combined with [com.cylonid.nativealpha.util.WebAppRouter] and
 * the per-slot data-directory suffix logic in [com.cylonid.nativealpha.util.App],
 * this gives every web app a fully isolated WebView profile — cookies, localStorage,
 * IndexedDB, cache and service workers are never shared between apps.
 *
 * Each app is mapped to a slot via `appId % 8`. If a different app was previously
 * loaded in the same slot, [com.cylonid.nativealpha.util.WebAppRouter] kills that
 * slot's process so the next launch starts fresh with the correct per-app suffix.
 */
@AndroidEntryPoint class WebViewActivity0 : WebViewActivity()
@AndroidEntryPoint class WebViewActivity1 : WebViewActivity()
@AndroidEntryPoint class WebViewActivity2 : WebViewActivity()
@AndroidEntryPoint class WebViewActivity3 : WebViewActivity()
@AndroidEntryPoint class WebViewActivity4 : WebViewActivity()
@AndroidEntryPoint class WebViewActivity5 : WebViewActivity()
@AndroidEntryPoint class WebViewActivity6 : WebViewActivity()
@AndroidEntryPoint class WebViewActivity7 : WebViewActivity()
