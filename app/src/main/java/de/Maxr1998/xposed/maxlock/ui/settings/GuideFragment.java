package de.Maxr1998.xposed.maxlock.ui.settings;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import java.util.Locale;

import de.Maxr1998.xposed.maxlock.R;

public class GuideFragment extends Fragment {

    Integer avoidLoopCounter = 2; //2 to be able to back to the application with 3x back
    private WebView webView;
    private ProgressBar progressBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_guide, container, false);
        webView = (WebView) rootView.findViewById(R.id.guideView);
        progressBar = (ProgressBar) rootView.findViewById(R.id.page_loading_bar);
        progressBar.setMax(100);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                String url = failingUrl;
                int index = url.indexOf("#");
                if (index != -1) {
                    url = url.substring(0, index);
                }
                if (avoidLoopCounter > 0) {
                    webView.loadUrl(url);
                    avoidLoopCounter = avoidLoopCounter - 1;
                }
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                progressBar.setProgress(newProgress);
            }
        });
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl("http://vault-technosparks.rhcloud.com/maxlock/guide?client=inapp&lang=" + getLanguageCode());
        return rootView;
    }

    public String getLanguageCode() {
        String language = Locale.getDefault().getLanguage();
        String country = Locale.getDefault().getCountry();
        if (language.equals("")) {
            return "en-GB";
        }
        if (!country.equals("")) {
            return language + "-" + country;
        } else return language;
    }

    public boolean back() {
        if (webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return false;
    }
}
