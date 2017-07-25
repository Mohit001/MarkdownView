package br.tiagohm.markdownview;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import br.tiagohm.markdownview.css.InternalStyleSheet;
import br.tiagohm.markdownview.ext.localization.LocalizationExtension;

public class MarkdownView extends WebView {

    private OnElementListener mOnElementListener;
    private MarkdownParser markdownParser = new MarkdownParser();

    public MarkdownView(Context context) {
        this(context, null);
    }

    public MarkdownView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MarkdownView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        markdownParser.addOption(LocalizationExtension.LOCALIZATION_CONTEXT, context);

        try {
            setWebChromeClient(new WebChromeClient());
            getSettings().setJavaScriptEnabled(false);
            getSettings().setLoadsImagesAutomatically(false);
            addJavascriptInterface(new EventDispatcher(), "android");
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            TypedArray attr = getContext().obtainStyledAttributes(attrs, R.styleable.MarkdownView);
            markdownParser.setEscapeHtml(attr.getBoolean(R.styleable.MarkdownView_escapeHtml, true));
            attr.recycle();
        } catch (Exception e) {
            e.printStackTrace();
        }

        markdownParser.basicsOnly();

//        markdownParser.addJavascript(JQUERY_3);
    }

    public void setOnElementListener(OnElementListener listener) {
        mOnElementListener = listener;
    }


    public void loadMarkdown(String markdown) {
        loadDataWithBaseURL("",
                markdownParser.buildHtml(markdown),
                "text/html",
                "UTF-8",
                "");
    }

    public void loadMarkdownFromAsset(String path) {
        loadMarkdown(Utils.getStringFromAssetFile(getContext().getAssets(), path));
    }

    public void loadMarkdownFromFile(File file) {
        loadMarkdown(Utils.getStringFromFile(file));
    }

    public void loadMarkdownFromUrl(String url) {
        new LoadMarkdownUrlTask().execute(url);
    }

    public MarkdownView addStyleSheet(InternalStyleSheet style) {
        markdownParser.addStyleSheet(style);
        return this;
    }

    public interface OnElementListener {
        void onButtonTap(String id);

        void onCodeTap(String lang, String code);

        void onHeadingTap(int level, String text);

        void onImageTap(String src, int width, int height);

        void onLinkTap(String href, String text);

        void onKeystrokeTap(String key);

        void onMarkTap(String text);
    }


    private class LoadMarkdownUrlTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String url = params[0];
            InputStream is = null;
            try {
                URLConnection connection = new URL(url).openConnection();
                connection.setReadTimeout(5000);
                connection.setConnectTimeout(5000);
                connection.setRequestProperty("Accept-Charset", "UTF-8");
                return Utils.getStringFromInputStream(is = connection.getInputStream());
            } catch (Exception e) {
                e.printStackTrace();
                return "";
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(String s) {
            loadMarkdown(s);
        }
    }

    protected class EventDispatcher {
        @JavascriptInterface
        public void onButtonTap(String id) {
            if (mOnElementListener != null) {
                mOnElementListener.onButtonTap(id);
            }
        }

        @JavascriptInterface
        public void onCodeTap(String lang, String code) {
            if (mOnElementListener != null) {
                mOnElementListener.onCodeTap(lang, code);
            }
        }

        @JavascriptInterface
        public void onHeadingTap(int level, String text) {
            if (mOnElementListener != null) {
                mOnElementListener.onHeadingTap(level, text);
            }
        }

        @JavascriptInterface
        public void onImageTap(String src, int width, int height) {
            if (mOnElementListener != null) {
                mOnElementListener.onImageTap(src, width, height);
            }
        }

        @JavascriptInterface
        public void onMarkTap(String text) {
            if (mOnElementListener != null) {
                mOnElementListener.onMarkTap(text);
            }
        }

        @JavascriptInterface
        public void onKeystrokeTap(String key) {
            if (mOnElementListener != null) {
                mOnElementListener.onKeystrokeTap(key);
            }
        }

        @JavascriptInterface
        public void onLinkTap(String href, String text) {
            if (mOnElementListener != null) {
                mOnElementListener.onLinkTap(href, text);
            }
        }
    }
}
