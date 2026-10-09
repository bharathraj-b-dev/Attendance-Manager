package com.bharath.attendancemanager.digicampus;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.bharath.attendancemanager.data.LocalStorage;

public class DigiCampusWebViewManager {

    public interface PageListener {
        void onPageStarted(String url);
        void onPageFinished(String url);
        void onProgressChanged(int progress);
    }

    private final WebView webView;
    private final LocalStorage localStorage;
    private PageListener pageListener;

    public DigiCampusWebViewManager(WebView webView, LocalStorage localStorage) {
        this.webView = webView;
        this.localStorage = localStorage;
        setupSettings();
    }

    public void setPageListener(PageListener listener) {
        this.pageListener = listener;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupSettings() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                if (pageListener != null) {
                    pageListener.onPageStarted(url);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (url != null && !url.equals("about:blank")) {
                    localStorage.setLastUrl(url);
                }
                if (pageListener != null) {
                    pageListener.onPageFinished(url);
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                if (pageListener != null) {
                    pageListener.onProgressChanged(newProgress);
                }
            }
        });
    }

    public void loadInitialOrLastUrl() {
        String lastUrl = localStorage.getLastUrl();
        if (lastUrl != null && !lastUrl.isEmpty() && !lastUrl.equals("about:blank")) {
            webView.loadUrl(lastUrl);
        } else {
            webView.loadUrl(LocalStorage.DEFAULT_DIGICAMPUS_URL);
        }
    }

    public void loadUrl(String url) {
        webView.loadUrl(url);
    }

    public boolean canGoBack() {
        return webView.canGoBack();
    }

    public void goBack() {
        webView.goBack();
    }

    public boolean canGoForward() {
        return webView.canGoForward();
    }

    public void goForward() {
        webView.goForward();
    }

    public void reload() {
        webView.reload();
    }

    public void saveState(Bundle outState) {
        if (webView != null && outState != null) {
            webView.saveState(outState);
        }
    }

    public boolean restoreState(Bundle inState) {
        if (webView != null && inState != null) {
            return webView.restoreState(inState) != null;
        }
        return false;
    }

    public WebView getWebView() {
        return webView;
    }
}
