package br.tiagohm.markdownview;


import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.vladsch.flexmark.Extension;
import com.vladsch.flexmark.ast.AutoLink;
import com.vladsch.flexmark.ast.FencedCodeBlock;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.Image;
import com.vladsch.flexmark.ast.Link;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.ext.abbreviation.Abbreviation;
import com.vladsch.flexmark.ext.abbreviation.AbbreviationExtension;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.footnotes.FootnoteExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughSubscriptExtension;
import com.vladsch.flexmark.ext.gfm.tasklist.TaskListExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.AttributeProvider;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.IndependentAttributeProviderFactory;
import com.vladsch.flexmark.html.renderer.AttributablePart;
import com.vladsch.flexmark.html.renderer.NodeRendererContext;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.superscript.SuperscriptExtension;
import com.vladsch.flexmark.util.html.Attributes;
import com.vladsch.flexmark.util.options.DataHolder;
import com.vladsch.flexmark.util.options.DataKey;
import com.vladsch.flexmark.util.options.MutableDataHolder;
import com.vladsch.flexmark.util.options.MutableDataSet;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import br.tiagohm.markdownview.css.ExternalStyleSheet;
import br.tiagohm.markdownview.css.StyleSheet;
import br.tiagohm.markdownview.ext.button.ButtonExtension;
import br.tiagohm.markdownview.ext.emoji.EmojiExtension;
import br.tiagohm.markdownview.ext.kbd.Keystroke;
import br.tiagohm.markdownview.ext.kbd.KeystrokeExtension;
import br.tiagohm.markdownview.ext.label.LabelExtension;
import br.tiagohm.markdownview.ext.localization.LocalizationExtension;
import br.tiagohm.markdownview.ext.mark.Mark;
import br.tiagohm.markdownview.ext.mark.MarkExtension;
import br.tiagohm.markdownview.ext.mathjax.MathJax;
import br.tiagohm.markdownview.ext.mathjax.MathJaxExtension;
import br.tiagohm.markdownview.ext.twitter.TwitterExtension;
import br.tiagohm.markdownview.ext.video.VideoLinkExtension;
import br.tiagohm.markdownview.js.ExternalScript;
import br.tiagohm.markdownview.js.JavaScript;

public class MarkdownParser {

    public final static JavaScript JQUERY_3 = new ExternalScript("file:///android_asset/js/jquery-3.1.1.min.js", false, false);
    public final static JavaScript HIGHLIGHTJS = new ExternalScript("file:///android_asset/js/highlight.js", false, true);
    public final static JavaScript MATHJAX = new ExternalScript("https://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS_CHTML", false, true);
    public final static JavaScript HIGHLIGHT_INIT = new ExternalScript("file:///android_asset/js/highlight-init.js", false, true);
    public final static JavaScript MATHJAX_CONFIG = new ExternalScript("file:///android_asset/js/mathjax-config.js", false, true);
    public final static JavaScript TOOLTIPSTER_JS = new ExternalScript("file:///android_asset/js/tooltipster.bundle.min.js", false, true);
    public final static JavaScript TOOLTIPSTER_INIT = new ExternalScript("file:///android_asset/js/tooltipster-init.js", false, true);

    public final static StyleSheet TOOLTIPSTER_CSS = new ExternalStyleSheet("file:///android_asset/css/tooltipster.bundle.min.css");

    private List<Extension> EXTENSIONS = Arrays.asList(TablesExtension.create(),
            TaskListExtension.create(),
            AbbreviationExtension.create(),
            AutolinkExtension.create(),
            MarkExtension.create(),
            StrikethroughSubscriptExtension.create(),
            SuperscriptExtension.create(),
            KeystrokeExtension.create(),
            MathJaxExtension.create(),
            FootnoteExtension.create(),
            EmojiExtension.create(),
            VideoLinkExtension.create(),
            TwitterExtension.create(),
            LabelExtension.create(),
            ButtonExtension.create(),
            LocalizationExtension.create());

    private final DataHolder OPTIONS = new MutableDataSet()
            .set(FootnoteExtension.FOOTNOTE_REF_PREFIX, "[")
            .set(FootnoteExtension.FOOTNOTE_REF_SUFFIX, "]")
            .set(HtmlRenderer.FENCED_CODE_LANGUAGE_CLASS_PREFIX, "")
            .set(HtmlRenderer.FENCED_CODE_NO_LANGUAGE_CLASS, "nohighlight")
            //.set(FootnoteExtension.FOOTNOTE_BACK_REF_STRING, "&#8593")
            ;

    private final List<StyleSheet> mStyleSheets = new LinkedList<>();
    private final HashSet<JavaScript> mScripts = new LinkedHashSet<>();
    private boolean mEscapeHtml = true;

     public String parseBuildAndRender(String text) {
         setEscapeHtml(false);
        Parser parser = Parser.builder(OPTIONS)
                .extensions(EXTENSIONS)
                .build();

        HtmlRenderer renderer = HtmlRenderer.builder(OPTIONS)
                .escapeHtml(mEscapeHtml)
                .attributeProviderFactory(new IndependentAttributeProviderFactory() {
                    @Override
                    public AttributeProvider create(NodeRendererContext context) {
                        return new CustomAttributeProvider();
                    }
                })
                .nodeRendererFactory(new NodeRendererFactoryImpl())
                .extensions(EXTENSIONS)
                .build();

        return renderer.render(parser.parse(text));
    }

    public MarkdownParser basicsOnly() {
        EXTENSIONS = Arrays.asList();
        setEscapeHtml(false);
        mScripts.clear();
        mStyleSheets.clear();
        return this;
    }


    public String buildHtml(String text) {
        setEscapeHtml(false);
        long start = System.currentTimeMillis();
        String html = parseBuildAndRender(text);

        StringBuilder sb = new StringBuilder();
        sb.append("<html>\n");
        sb.append("<head>\n");

        for (StyleSheet s : mStyleSheets) {
            sb.append(s.toHTML());
        }

        for (JavaScript js : mScripts) {
            sb.append(js.toHTML());
        }

        sb.append("</head>\n");
        sb.append("<body>\n");
        sb.append("<div class=\"container\">\n");
        sb.append(html);
        sb.append("</div>\n");
        sb.append("</body>\n");
        sb.append("</html>");

        html = sb.toString();
        long duration = System.currentTimeMillis() - start;
        Log.i("MarkdownParser", "----> Markdown Parsing time: " + duration);
        return html;
    }

    public MarkdownParser setEscapeHtml(boolean flag) {
        mEscapeHtml = flag;
        return this;
    }

    public MarkdownParser setEmojiRootPath(String path) {
        ((MutableDataHolder) OPTIONS).set(EmojiExtension.ROOT_IMAGE_PATH, path);
        return this;
    }

    public MarkdownParser setEmojiImageExtension(String ext) {
        ((MutableDataHolder) OPTIONS).set(EmojiExtension.IMAGE_EXT, ext);
        return this;
    }

    public MarkdownParser addStyleSheet(StyleSheet s) {
        if (s != null && !mStyleSheets.contains(s)) {
            mStyleSheets.add(s);
        }

        return this;
    }

    public MarkdownParser replaceStyleSheet(StyleSheet oldStyle, StyleSheet newStyle) {
        if (oldStyle == newStyle) {
        } else if (newStyle == null) {
            mStyleSheets.remove(oldStyle);
        } else {
            final int index = mStyleSheets.indexOf(oldStyle);

            if (index >= 0) {
                mStyleSheets.set(index, newStyle);
            } else {
                addStyleSheet(newStyle);
            }
        }

        return this;
    }

    public MarkdownParser removeStyleSheet(StyleSheet s) {
        mStyleSheets.remove(s);
        return this;
    }

    public MarkdownParser addJavascript(JavaScript js) {
        mScripts.add(js);
        return this;
    }

    public MarkdownParser removeJavaScript(JavaScript js) {
        mScripts.remove(js);
        return this;
    }

    public void addOption(DataKey<Context> key, Context context) {
        ((MutableDataHolder) OPTIONS).set(key, context);
    }

    public class CustomAttributeProvider implements AttributeProvider {
        @Override
        public void setAttributes(final Node node, final AttributablePart part, final Attributes attributes) {
            if (node instanceof FencedCodeBlock) {
                if (part.getName().equals("NODE")) {
                    String language = ((FencedCodeBlock) node).getInfo().toString();
                    if (!TextUtils.isEmpty(language) &&
                            !language.equals("nohighlight")) {
                        addJavascript(HIGHLIGHTJS);
                        addJavascript(HIGHLIGHT_INIT);

                        attributes.addValue("language", language);
                        attributes.addValue("onclick", String.format("javascript:android.onCodeTap('%s', this.textContent);",
                                language));
                    }
                }
            } else if (node instanceof MathJax) {
                addJavascript(MATHJAX);
                addJavascript(MATHJAX_CONFIG);
            } else if (node instanceof Abbreviation) {
                addJavascript(TOOLTIPSTER_JS);
                addStyleSheet(TOOLTIPSTER_CSS);
                addJavascript(TOOLTIPSTER_INIT);
                attributes.addValue("class", "tooltip");
            } else if (node instanceof Heading) {
                attributes.addValue("onclick", String.format("javascript:android.onHeadingTap(%d, '%s');",
                        ((Heading) node).getLevel(), ((Heading) node).getText()));
            } else if (node instanceof Image) {
                attributes.addValue("onclick", String.format("javascript: android.onImageTap(this.src, this.clientWidth, this.clientHeight);"));
            } else if (node instanceof Mark) {
                attributes.addValue("onclick", String.format("javascript: android.onMarkTap(this.textContent)"));
            } else if (node instanceof Keystroke) {
                attributes.addValue("onclick", String.format("javascript: android.onKeystrokeTap(this.textContent)"));
            } else if (node instanceof Link ||
                    node instanceof AutoLink) {
                attributes.addValue("onclick", String.format("javascript: android.onLinkTap(this.href, this.textContent)"));
            }
        }
    }

}
