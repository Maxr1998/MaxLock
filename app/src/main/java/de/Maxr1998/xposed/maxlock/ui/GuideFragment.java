package de.Maxr1998.xposed.maxlock.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.File;

import de.Maxr1998.xposed.maxlock.R;
import de.Maxr1998.xposed.maxlock.Util;

public class GuideFragment extends Fragment {

    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_guide, container, false);
        webView = (WebView) rootView.findViewById(R.id.guideView);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                String url = failingUrl;
                int index = url.indexOf("#");
                if (index != -1) {
                    url = url.substring(0, index);
                }
                webView.loadUrl(url);
            }
        });
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setJavaScriptEnabled(true);
        String path = Util.dataDir(getActivity()) + File.separator + "guide";
        if (new File(path + File.separator + "index.html").exists())
            webView.loadUrl("file://" + path + File.separator + "index.html");
        return rootView;
    }

    public boolean back() {
        if (webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return false;
    }
}
