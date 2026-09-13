# Usage Examples

PD4ML is distributed with a comprehensive set of runnable Java examples that walk through the API end to end -- from the simplest possible HTML-to-PDF conversion through to advanced resource-loading strategies and standalone PDF post-processing. Because a substantial share of PD4ML's installed base is still running the legacy v3 API, most examples below are presented in two equivalent forms: the current v4 API and, alongside it, the v3-era call sequence that accomplishes the same result. Where the two APIs are identical, or where an example predates the v3/v4 split entirely (a plain HTML fragment, for instance), only a single version is shown. Every example is maintained as complete, buildable source in the [`pd4ml-examples`](https://github.com/zxfr/pd4ml-examples) GitHub repository, linked individually below.

## Basics

Fundamental conversion setup: reading the source HTML, configuring the target page, and applying the core per-page decorations (headers, footers, backgrounds, watermarks) through both the Java API and PD4ML's inline HTML tags.

### Getting Started

The minimal end-to-end conversion: the source document is read from an in-memory HTML string, converted using PD4ML's default settings (A4 output, 10 mm margins on every side), and the resulting PDF is written to a temporary file. Once the file is written, it is opened automatically in the system's default PDF viewer, purely as a convenience for iterating on the example locally.

{% tabs %}
{% tab title="PD4ML v4" %}
```java
PD4ML pd4ml = new PD4ML();

String html = "TEST<pd4ml:page.break><b>Hello, World!</b>";
StringReader bais = new StringReader(html);

File pdf = File.createTempFile("result", ".pdf");
FileOutputStream fos = new FileOutputStream(pdf);

// render and write the result as PDF
pd4ml.render(bais, fos);

// open the just-generated PDF with a default PDF viewer
Desktop.getDesktop().open(pdf);
```
{% endtab %}

{% tab title="PD4ML v3" %}
```java
PD4ML pd4ml = new PD4ML();

String html = "TEST<pd4ml:page.break><b>Hello, World!</b>";
ByteArrayInputStream bais = new ByteArrayInputStream(html.getBytes());

// read and parse HTML
pd4ml.readHTML(bais);

File pdf = File.createTempFile("result", ".pdf");
FileOutputStream fos = new FileOutputStream(pdf);

// render and write the result as PDF
pd4ml.writePDF(fos);

// alternatively or additionally:
// pd4ml.writeRTF(rtfos, false);
// pd4ml.writeDOCX(docxos);
// BufferedImage[] images = pd4ml.renderAsImages();

// open the just-generated PDF with a default PDF viewer
Desktop.getDesktop().open(pdf);
```
{% endtab %}
{% endtabs %}

Source: [E001GettingStarted.java](https://github.com/zxfr/pd4ml-examples/blob/master/src/main/java/basics/E001GettingStarted.java)

### Set Page Format

Page format and page margins are represented by the [`PageSize`](https://pd4ml.com/javadoc/com/pd4ml/PageSize.html) and `PageMargins` classes, respectively. `PageSize` predefines constants for the commonly used paper formats, though an arbitrary format (specified in points or millimeters) is equally valid. Both settings accept an optional scope string to restrict them to a specific page range, and multiple calls are permitted -- where two calls' ranges overlap, the call made later wins. Omitting the scope applies the setting to the entire document.

{% tabs %}
{% tab title="PD4ML v4" %}
```java
// define page format for the first page
pd4ml.setPageSize(PageSize.A5, "1");

// define landscape page format for the second and following pages
pd4ml.setPageSize(PageSize.A4.rotate(), "2+");

// reset page margins for the first two pages
pd4ml.setPageMargins(new PageMargins(0, 0, 0, 0), "1-2");

// set page margins for the third and following (if any) pages
pd4ml.setPageMargins(new PageMargins(0, 0, 0, 0), "3+");
```
{% endtab %}

{% tab title="PD4ML v3" %}
```java
pd4ml.setPageSize(PD4Constants.A5);

// alternatively, define landscape page format
// pd4ml.setPageSize(pd4ml.changePageOrientation(PD4Constants.A5));

pd4ml.setPageInsets(new java.awt.Insets(0, 0, 0, 0));
```
{% endtab %}
{% endtabs %}

Source: [E002SetPageFormat.java](https://github.com/zxfr/pd4ml-examples/blob/master/src/main/java/basics/E002SetPageFormat.java)

### Set Page Header And Footer

`setPageHeader()` and `setPageFooter()` define page headers and footers using HTML as the layout language -- this call signature is unchanged between v3 and v4. An optional scope parameter restricts a given header or footer to a specific page range, and the HTML may reference three built-in placeholders: `$[page]` (the current page number), `$[total]` (the total page count), and `$[title]` (the document title, taken from the `<title>` tag or overridden via `setDocumentTitle()`).

```java
// define page header for the first page, 30px high
pd4ml.setPageHeader("$[title]", 30, "1");

// define page footer for the first page
pd4ml.setPageFooter("Total pages: $[total]", 30, "1");

// define page header for the second page
pd4ml.setPageHeader("<b>$[title]</b> $[page]/$[total]", 30, "2+");

// define page footer for the second page
pd4ml.setPageFooter("<div style='width: 100%; text-align: right'>Page: $[page]</div>", 30, "2+");
```

For logic more elaborate than a scope string can express, a header or footer may instead be supplied as a `PD4PageMark` object, whose `getHtmlTemplate(int pageNumber)` method is invoked once per page and can return arbitrarily different markup for each:

```java
PD4PageMark header = new PD4PageMark() {
    public String getHtmlTemplate(int pageNumber) {
        if (pageNumber == 1) {
            return "<html><body>$[title]";
        } else {
            return "<html><body><b>$[title]</b> $[page]/$[total]";
        }
    }
};

PD4PageMark footer = new PD4PageMark() {
    public String getHtmlTemplate(int pageNumber) {
        if (pageNumber == 1) {
            return "<html><body>Total pages: $[total]";
        } else {
            return "<html><body><div style='width: 100%; text-align: right'>Page: $[page]</div>";
        }
    }
};

header.setAreaHeight(30);
footer.setAreaHeight(30);

// assign page header
pd4ml.setPageHeader(header);
// assign page footer
pd4ml.setPageFooter(footer);
```

Source: [E003SetPageHeaderFooter.java](https://github.com/zxfr/pd4ml-examples/blob/master/src/main/java/basics/E003SetPageHeaderFooter.java)

### Set Page Header Footer Inline

As an alternative to the `setPageHeader()`/`setPageFooter()` API calls, header and footer markup can be declared directly in the source HTML using the proprietary `<pd4ml:page.header>` and `<pd4ml:page.footer>` tags, again with an optional `scope` attribute. The tags may appear anywhere in the document body (though placing them as a direct child of `<body>` is the clearest convention) and take effect for the current page and every subsequent page until overridden by a later occurrence. The same `$[page]`, `$[total]`, and `$[title]` placeholders are available here as well.

```html
<html>
<head>
<title>Header/Footer example</title>
<style>BODY {font-family: Arial}</style>
</head>
<body>

<!-- inline definition of the header/footer for the current and all following pages -->
<pd4ml:page.header height=30>$[title]</pd4ml:page.header>
<pd4ml:page.footer height=30>Total pages: $[total]</pd4ml:page.footer>

First Page

<pd4ml:page.break>

<!-- here it overrides the header/footer set above starting from the current page -->
<pd4ml:page.header height=30>
<b>$[title]</b> $[page]/$[total]
</pd4ml:page.header>
<pd4ml:page.footer height=30>
<div style='width: 100%; text-align: right'>Page: $[page]</div>
</pd4ml:page.footer>

Second Page
</body>
</html>
```

Source: [E004SetPageHeaderFooterInline.java](https://github.com/zxfr/pd4ml-examples/blob/master/src/main/java/basics/E004SetPageHeaderFooterInline.java)

### Set Page Background

`setPageBackground()` defines the background layout of the target media. That layout is itself HTML -- in the simplest case a single full-bleed `<img>` (a scanned form, for example), or arbitrarily elaborate HTML/CSS/SVG. The same `$[page]`, `$[total]`, and `$[title]` placeholders are honored, and the background is painted across the entire page area, ignoring the configured margins. An optional scope parameter restricts it to a specific page range.

```java
// define page background for the first page
pd4ml.setPageBackground("<div style='width: 100%; height: 100%; background-color: rgb(228,255,228);'></div>", "1");

// define page background for the second and following pages
pd4ml.setPageBackground("<div style='width: 100%; height: 100%; background-color: rgb(255,228,228);'></div>", "2+");
```

As with headers and footers, a `PD4PageMark` subclass allows the background to be computed per page rather than declared as a fixed HTML string:

```java
PD4PageMark pageDecoration = new PD4PageMark() {
    @Override
    public Color getPageBackgroundColor(int pageNumber) {
        if (pageNumber == 1) {
            return new Color(228, 255, 228);
        } else {
            return new Color(255, 228, 228);
        }
    }
};

// assign page footer (here it only specifies the background)
pd4ml.setPageFooter(pageDecoration);
```

Source: [E005SetPageBackground.java](https://github.com/zxfr/pd4ml-examples/blob/master/src/main/java/basics/E005SetPageBackground.java)

### Set Page Background Inline

The `<pd4ml:page.background>` tag achieves the same result as `setPageBackground()`, declared inline in the source HTML instead of through the API. Placing a new occurrence of the tag later in the document overrides the background from that point forward -- equivalent to defining the background once at the top of the document and using a `"2+"` scope.

```html
<html>
<head>
<title>Page background example</title>
<style>BODY {font-family: Arial}</style>
</head>
<body>
<pd4ml:page.background>
<div style='width: 100%; height: 100%; background-color: rgb(228,255,228);'></div>
</pd4ml:page.background>
First Page

<pd4ml:page.break>
<!--
  Override the previously defined background with a new one, starting from
  the current page. Note that here the style is applied directly to the
  <pd4ml:page.background> tag's own style attribute, rather than to a nested
  <div>.
-->
<pd4ml:page.background style='width: 100%; height: 100%; background-color: rgb(255,228,228);'></pd4ml:page.background>
Second Page
</body>
</html>
```

Source: [E006SetPageBackgroundInline.java](https://github.com/zxfr/pd4ml-examples/blob/master/src/main/java/basics/E006SetPageBackgroundInline.java)

### Set Page Watermark

PD4ML exposes native PDF watermarking through `setWatermark()`. A watermark's layout is, once again, ordinary HTML/CSS/SVG (though unlike headers, footers, and backgrounds, it does not support the `$[page]`/`$[total]` placeholders), and the call controls its position, opacity, rotation angle, scale, and the page range it applies to, as well as whether it should be visible on screen, in print, or both.

```java
// define watermark for the first page
pd4ml.setWatermark("<b>WATERMARK</b>",
    20,   // offset X
    0,    // offset Y
    .3f,  // opacity
    30,   // angle
    9,    // scale (1 = 100%)
    true, // should the watermark be visible in PDF viewers?
    true, // should the watermark be printed?
    "1"); // page range to apply

// define watermark for the second and following pages
pd4ml.setWatermark("<b style='color: tomato'>WATERMARK</b>", 20, 0, .3f, 30, 9, true, true, "2+");
```

A `PD4PageMark` subclass again allows the watermark to vary per page -- here driven from an image URL rather than inline HTML:

```java
PD4ML pd4ml = new PD4ML();

PD4PageMark pageDecoration = new PD4PageMark() {
    @Override
    public String getWatermarkUrl(int pageNumber) {
        if (pageNumber == 1) {
            return "https://pd4ml.com/i/logo.png";
        } else {
            return "https://pd4ml.com/i/logo.gif";
        }
    }

    @Override
    public int getWatermarkOpacity() {
        // image opacity in range from 0 to 100
        return 30;
    }

    @Override
    public Rectangle getWatermarkBounds() {
        return new Rectangle(10, 10, 200, 200);
    }

    public String getWatermarkUrl() {
        // as getWatermarkUrl(int pageNumber) is already defined above, this
        // overload only needs to return a non-null placeholder so that PD4ML
        // knows to process watermarks at all
        return "defined";
    }
};

// assign page footer (here it only specifies the watermark)
pd4ml.setPageFooter(pageDecoration);
```

Source: [E007SetPageWatermark.java](https://github.com/zxfr/pd4ml-examples/blob/master/src/main/java/basics/E007SetPageWatermark.java)

### Set Page Watermark Inline

The `<pd4ml:watermark>` tag mirrors `setWatermark()` for use directly within the source HTML, with the same geometry and appearance controls expressed through its `style` attribute rather than positional method arguments.

```html
<html>
<head>
<title>Watermarking example</title>
<style>BODY {font-family: Arial}</style>
</head>
<body>

<pd4ml:watermark style="opacity: 30%; left: 20px; top: 0; scale: 900%; angle: 30deg; media: screen, print;" scope="1">
<b>WATERMARK</b>
</pd4ml:watermark>

<pd4ml:watermark style="opacity: 30%; left: 20px; top: 0; scale: 900%; angle: 30deg; media: screen, print;" scope="2+">
<b style='color: tomato'>WATERMARK</b>
</pd4ml:watermark>

First Page

<pd4ml:page.break>

Second Page

</body>
</html>
```

Source: [E008SetPageWatermarkInline.java](https://github.com/zxfr/pd4ml-examples/blob/master/src/main/java/basics/E008SetPageWatermarkInline.java)

### Set Document Password

`setPermissions()` applies the standard PDF security options: a document password, a restricted set of permitted actions (such as suppressing high-resolution printing), or both. Permissions can be expressed either as a positive list of what is allowed, or as the full default set with specific rights subtracted. The v3 and v4 signatures differ only in the permission-flags class used and in a trailing boolean argument that v3 requires:

{% tabs %}
{% tab title="PD4ML v4" %}
```java
// positive list of permissions, no password
pd4ml.setPermissions(null, Constants.AllowAnnotate | Constants.AllowDegradedPrint);

// default permissions minus one, no password
pd4ml.setPermissions(null, Constants.DefaultPermissions ^ Constants.AllowModify);

// protect the document with the "test" password; no permission restrictions applied
pd4ml.setPermissions("test", Constants.DefaultPermissions);
```
{% endtab %}

{% tab title="PD4ML v3" %}
```java
// positive list of permissions, no password
pd4ml.setPermissions("empty", PD4Constants.AllowAnnotate | PD4Constants.AllowDegradedPrint, true);

// default permissions minus one, no password
pd4ml.setPermissions("empty", PD4Constants.DefaultPermissions ^ PD4Constants.AllowModify, true);

// protect the document with the "test" password; no permission restrictions applied
pd4ml.setPermissions("test", PD4Constants.DefaultPermissions, true);
```
{% endtab %}
{% endtabs %}

Source: [E009SetDocumentPassword.java](https://github.com/zxfr/pd4ml-examples/blob/master/src/main/java/basics/E009SetDocumentPassword.java)

### Inject Html

`injectHtml()` splices an arbitrary HTML fragment either immediately after the opening `<body>` tag or immediately before the closing `</body>` tag, unchanged between v3 and v4. Because the fragment is spliced into raw markup rather than parsed as a self-contained document, it is possible to corrupt the surrounding layout with malformed input -- in the extreme case, injecting the start of an HTML comment with no matching close produces a blank PDF.

```java
// insert content just after the opening <body> tag
pd4ml.injectHtml("Some new content at the top of the document", true);

// insert content just before the closing </body> tag
pd4ml.injectHtml("<p style='color: tomato'>Content to append</p>", false);
```

Source: [E010InjectHtml.java](https://github.com/zxfr/pd4ml-examples/blob/master/src/main/java/basics/E010InjectHtml.java)

## HTML Features

Features that operate on the source document itself, most of them exposed through PD4ML's proprietary `<pd4ml:*>` tags.

### Add Style Programmatically

`addStyle()` applies an additional stylesheet to the source document -- supplied either as a literal style string or a reference to an external resource. It accepts multiple invocations and only takes effect when called before `readHTML()`. The example below also illustrates mapping a CSS `@font-face` declaration to a bundled TrueType font, and forcing a page break before every `<h3>` except the first.

```java
pd4ml.setHtmlWidth(900); // render HTML in a virtual frame 900px wide

// Map the "Consolas" font-family to a bundled TTF file (here, the free
// FiraMono-Regular as a stand-in for the original Consolas). Any glyph the
// mapped font doesn't cover renders as '?' in the output PDF -- the fix is to
// build a proper font directory covering the needed character range, index
// it, and point PD4ML at it with useTTF() instead (see Embedding TTF Fonts).
pd4ml.addStyle(
    "@font-face {\n" +
    "  font-family: \"Consolas\";\n" +
    "  src: url(\"java:/html/rc/FiraMono-Regular.ttf\") format(\"ttf\");\n" +
    "}\n", false);

// read and parse HTML
pd4ml.readHTML(new URL("html/H001.htm"));

pd4ml.addStyle(
    "H3 { page-break-before: always; }\n" +
    "H3:first-of-type { page-break-before: auto; }", true);
```

Source: [H001ConvertHtml.java](https://github.com/zxfr/pd4ml-examples/blob/master/src/main/java/html/H001ConvertHtml.java)

### Add TOC

The `<pd4ml:toc>` tag is replaced, at conversion time, with a table of contents automatically generated from the document's `<h1>`-`<h6>` heading hierarchy; the generated table is an ordinary HTML `<table>` and can be restyled with CSS like any other. Adding `pd4toc="nopagenum"` to a given heading suppresses a page-number entry for it in the generated TOC. The example injects the tag at the very top of the document from Java, rather than authoring it directly into the source HTML.

```java
// forces PD4ML to process <pd4ml:toc> as though it appeared in the source
// HTML immediately after the opening <body> tag
pd4ml.injectHtml("<pd4ml:toc>", true);
```

```html
...
<body>
    <pd4ml:toc>
    <hr>
    <h1>Pages</h1>
    <h2>First Page</h2>
    <pd4ml:page.break>
    <h2>Second Page</h2>
...
```

Source: [H002AddTOC.java](https://github.com/zxfr/pd4ml-examples/blob/master/src/main/java/html/H002AddTOC.java)

### Page Number Tag

By default, `<pd4ml:page.number>` is replaced with the current page number. Its optional `of` attribute instead names the `id` of another HTML element, in which case the tag resolves to the page number on which that element is located -- useful for "continued on page N"-style cross-references.

```html
<html>
<body>
Total pages: <pd4ml:page.number><br>
<a href="#continue1"><b>Section 1</b></a> on page <pd4ml:page.number of="continue1"><br>
<a href="#continue2"><b>Section 2</b> on page <pd4ml:page.number of="continue2"></a><br>
<pd4ml:page.break>
<a name="continue1">Section 1</a>
<pd4ml:page.break>
<div id="continue2">Section 2</div>
</body>
</html>
```

### Create Bookmarks

PD4ML supports three ways of generating PDF bookmarks (outlines): from the `<h1>`-`<h6>` heading hierarchy, from named anchors (`<a name="chapter1">Chapter 1</a>`), or explicitly from a structure of `<pd4ml:bookmark>` tags -- the latter is always merged into the bookmark tree regardless of which of the first two methods is also active. The v4 API splits heading- and anchor-based generation into two distinct calls; v3 instead used a single method with a boolean flag choosing between them.

{% tabs %}
{% tab title="PD4ML v4" %}
```java
pd4ml.generateBookmarksFromHeadings(true);
// or, to generate from named anchors instead:
// pd4ml.generateBookmarksFromAnchors(true);
```
{% endtab %}

{% tab title="PD4ML v3" %}
```java
pd4ml.generateOutlines(true); // true = from headings, false = from named anchors
```
{% endtab %}
{% endtabs %}

Source: [H003CreateBookmarks.java](https://github.com/zxfr/pd4ml-examples/blob/master/src/main/java/html/H003CreateBookmarks.java) -- see also [`generateBookmarksFromAnchors()`](https://pd4ml.com/javadoc/com/pd4ml/PD4ML.html#generateBookmarksFromAnchors-boolean-)

### Apply Page Breaks

A page break can be forced with the standard CSS `page-break-before` property or with PD4ML's own `<pd4ml:page.break>` tag. In versions prior to v4, the tag additionally supported rotating the following page, changing the HTML-to-PDF scale factor at the break, and making the break conditional; these extensions have not yet been ported to v4 and are expected in a forthcoming release. The CSS-based approach shown here is unaffected by that gap and works identically on both versions.

```java
pd4ml.addStyle(
    "H3 { page-break-before: always; }\n" +
    "H3:first-of-type { page-break-before: auto; }", true);
```

Source: [H004ApplyPageBreaks.java](https://github.com/zxfr/pd4ml-examples/blob/master/src/main/java/html/H004ApplyPageBreaks.java)

### Add Attachment

The `<pd4ml:attachment>` tag embeds an arbitrary file into the resulting PDF as an attachment, referenced through its `src` attribute. It renders as a clickable icon -- opened, by the viewer, with that file type's default application -- and can be placed anywhere in the document. Four icon styles are available: `graph`, `paperclip`, `pushpin`, and `area` (an invisible icon that turns a neighboring region into the clickable target instead of rendering its own glyph).

```java
// embed the source document itself as an attachment; the icon appears at
// the top right of the page
pd4ml.injectHtml("<div style=\"text-align: right; width: 100%\">"
    + "<pd4ml:attachment style=\"align: right\" type=\"paperclip\" src=\"H001.htm\"/>"
    + "</div>", true);
```

```html
<div style="text-align: right; width: 100%">
<pd4ml:attachment description="desc" style="align: right" type="paperclip" src="src/html/H001.htm"/>
</div>
```

Source: [H005AddAttachment.java](https://github.com/zxfr/pd4ml-examples/blob/master/src/main/java/html/H005AddAttachment.java)

### Footnotes / Endnotes

The `<pd4ml:footnote>` tag moves its nested content out of the normal document flow and down to the bottom of the current page, replacing it in place with an auto-incrementing reference index; a `noref` attribute suppresses that index where a footnote shouldn't be numbered. `<pd4ml:footnote.caption>` defines the separator content placed between the main body and the footnote area. If the accumulated footnotes for a page don't fit in the available space, the overflow is carried to the following page. `<pd4ml:endnote>` and `<pd4ml:endnote.caption>` behave identically, except that the content is collected at the very end of the document rather than the bottom of the current page.

```html
<pd4ml:footnote.caption>
Footnotes
<hr>
</pd4ml:footnote.caption>

<pd4ml:footnote noref>This footnote has no reference from the main text</pd4ml:footnote>

A note is a string of text placed at the bottom of a page in a book or document or at the end of a chapter,
volume or the whole text<pd4ml:footnote>In some editions of the Bible, notes are placed in a narrow column
in the middle of each page between two columns of biblical text.</pd4ml:footnote>.
The note can provide an author's comments on the main text or citations of a
reference work in support of the text, or both.
<p>
Footnotes are notes at the foot of the page while endnotes<pd4ml:footnote>Unlike footnotes, endnotes have the advantage of not
affecting the layout of the main text, but may cause inconvenience to readers who have to move back
and forth between the main text and the endnotes.</pd4ml:footnote> are collected under a separate heading at
the end of a chapter, volume, or entire work.
<p>
```

Source: [H006.htm](https://github.com/zxfr/pd4ml-examples/blob/master/src/main/java/html/H006.htm)

## Internationalization

### Embedding TTF Fonts

Supporting non-Latin character sets requires every referenced TrueType font to be shaped (stripped of unused glyphs) and embedded into the resulting PDF, which in turn requires PD4ML to have direct access to the TTF font files themselves. `useTTF()` points PD4ML at a font directory (or a font folder packaged inside a resource JAR) and can be called multiple times to combine several sources. PD4ML expects to find a `pd4fonts.properties` index file in that directory, mapping font-face names to font files; if the index is missing, auto-indexing can be enabled instead, though for a directory with many fonts, pre-generating the index (see the next example) is significantly faster than indexing on every run. On Windows, the system font directory is typically `c:/windows/fonts`.

```java
public final static String FONTS_DIR = "c:/windows/fonts";

...

PD4ML pd4ml = new PD4ML();
// the second argument forces indexing of FONTS_DIR on this call, which is
// costly for a large font directory -- see "Preparing TTF Fonts" to
// pre-generate the index instead
pd4ml.useTTF(FONTS_DIR, true);
```

Source: [N001TtfFonts.java](https://github.com/zxfr/pd4ml-examples/blob/master/src/main/java/i18n/N001TtfFonts.java) -- see also [`useTTF()`](https://pd4ml.com/javadoc/com/pd4ml/PD4ML.html#useTTF-java.lang.String-)

### Preparing TTF Fonts

Rather than indexing a font directory on every conversion, `pd4fonts.properties` can be generated once, ahead of time -- from a Java application, as shown below, or equivalently from the command line with `java -jar pd4ml.jar -configure.fonts <font.dir> [index.file.location]`. Once generated, the index file is passed to `useTTF()` with auto-indexing disabled. The font-cache class generating the index was renamed between v3 and v4.

{% tabs %}
{% tab title="PD4ML v4" %}
```java
File index = File.createTempFile("pd4fonts", ".properties");
index.deleteOnExit();
PD4Util.generateFontPropertiesFile(FONTS_DIR, index.getAbsolutePath());

// the same can be done from the command line:
// java -jar pd4ml.jar -configure.fonts <font.dir> [index.file.location]

PD4ML pd4ml = new PD4ML();
pd4ml.useTTF(index.getAbsolutePath(), true);
```
{% endtab %}

{% tab title="PD4ML v3" %}
```java
// Indexing a font directory is time- and resource-consuming, so it's worth
// preparing the mapping file once, ahead of time, rather than on every run.
File index = File.createTempFile("pd4fonts", ".properties");
index.deleteOnExit();
FontCache.generateFontPropertiesFile(FONTS_DIR, index.getAbsolutePath(), (short) 0);

System.out.println("font indexing is done.");
// the same can be done from the command line:
// java -jar pd4ml.jar -configure.fonts <font.dir> [index.file.location]

...

pd4ml.useTTF(index.getAbsolutePath());
```
{% endtab %}
{% endtabs %}

Source: [N002TtfFonts.java](https://github.com/zxfr/pd4ml-examples/blob/master/src/main/java/i18n/N002TtfFonts.java)

## Advanced Features

### Popup Print Dialog

Registering the built-in `printdialog` document action makes the generated PDF's print dialog open automatically as soon as it's opened in a viewer; it's equivalent to hand-rolling the same behavior with an explicit `OpenAction` and JavaScript `this.print(...)` call.

```java
pd4ml.addDocumentActionHandler("printdialog", null);
// equivalent to:
// pd4ml.addDocumentActionHandler("OpenAction", "this.print(true);");
```

Source: [A001PopupPrintDialog.java](https://github.com/zxfr/pd4ml-examples/blob/master/src/main/java/advanced/A001PopupPrintDialog.java)

### Silent Print

Similarly, the built-in `silentprint` action sends the document straight to the system's default printer as soon as it opens. In practice, modern PDF viewers still prompt the user for confirmation before printing regardless, so a truly silent print is not achievable through this mechanism alone -- it remains useful for skipping the print _dialog_ itself.

```java
pd4ml.addDocumentActionHandler("silentprint", null);
// equivalent to:
// pd4ml.addDocumentActionHandler("OpenAction", "this.print({bUI: false, bSilent: true});");
```

Source: [A002SilentPrint.java](https://github.com/zxfr/pd4ml-examples/blob/master/src/main/java/advanced/A002SilentPrint.java)

### Read Resources From Classpath

PD4ML supports a non-standard `java:` protocol for addressing resources through the Java classloader rather than the filesystem or the network. Instantiating `PD4ML` implicitly registers a URL stream handler factory for it; an application that needs to resolve `java:` URLs itself (outside of PD4ML) should register the same factory once, typically in a static initializer, as shown below. The two versions differ only in the final call used to trigger the conversion.

{% tabs %}
{% tab title="PD4ML v4" %}
```java
// If your own application code needs to resolve "java:" URLs directly, run
// this registration once -- e.g. in a static initializer.
URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory() {
    public URLStreamHandler createURLStreamHandler(String protocol) {
        return "java".equals(protocol) ? new URLStreamHandler() {
            protected URLConnection openConnection(URL url) throws IOException {
                return new URLConnection(url) {
                    public void connect() throws IOException {
                    }
                };
            }
        } : null;
    }
});

File pdf = File.createTempFile("result", ".pdf");
FileOutputStream fos = new FileOutputStream(pdf);
// render and write the result as PDF
pd4ml.render(new URL("java:/advanced/A003.htm"), fos);
```
{% endtab %}

{% tab title="PD4ML v3" %}
```java
// If your own application code needs to resolve "java:" URLs directly, run
// this registration once -- e.g. in a static initializer.
URL.setURLStreamHandlerFactory(new URLStreamHandlerFactory() {
    public URLStreamHandler createURLStreamHandler(String protocol) {
        return "java".equals(protocol) ? new URLStreamHandler() {
            protected URLConnection openConnection(URL url) throws IOException {
                return new URLConnection(url) {
                    public void connect() throws IOException {
                    }
                };
            }
        } : null;
    }
});

// read and parse HTML
pd4ml.readHTML(new URL("java:/advanced/A003.htm"));
```
{% endtab %}
{% endtabs %}

Source: [A003ReadHtmlFromClasspath.java](https://github.com/zxfr/pd4ml-examples/blob/master/src/main/java/advanced/A003ReadHtmlFromClasspath.java)

### Add Progress Listener

Converting a large document can take long enough that a GUI application benefits from showing real progress rather than a static loading state; PD4ML supports this through a callback interface. The example simply logs every event to standard output, but the same callback could drive a real progress bar instead. Between v3 and v4 the interface was renamed (`ProgressListener` to `PD4ProgressListener`), the registration method was renamed (`monitorProgressWith()` to `monitorProgress()`), and the set of lifecycle constants passed to `progressUpdate()` was revised.

{% tabs %}
{% tab title="PD4ML v4" %}
```java
public static class ProgressMeter implements PD4ProgressListener {

    /**
     * Callback triggered by a progress event. This implementation dumps the
     * events to stdout; it could equally well drive a GUI progress bar.
     */
    public void progressUpdate(int messageID, int progress, String note, long msec) {

        String tick = String.format("%7d", msec);
        String progressString = String.format("%3d", progress);

        String step = "";
        switch (messageID) {
            case CONVERSION_BEGIN:
                step = "conversion begin";
                break;
            case HTML_PARSED:
                step = "html parsed";
                break;
            case DOC_TREE_BUILT:
                step = "document tree structure built";
                break;
            case HTML_LAYOUT_IN_PROGRESS:
                step = "layouting...";
                break;
            case HTML_LAYOUT_DONE:
                step = "layout done";
                break;
            case TOC_GENERATED:
                step = "TOC generated";
                break;
            case DOC_OUTPUT_IN_PROGRESS:
                step = "generating PDF...";
                break;
            case NEW_SRC_DOC_BEGIN:
                step = "proceed to new source document";
                break;
            case CONVERSION_END:
                step = "done.";
                break;
        }

        System.out.println(tick + " " + progressString + " " + step + " " + note);
    }
}

...

pd4ml.monitorProgress(new ProgressMeter());
```
{% endtab %}

{% tab title="PD4ML v3" %}
```java
public static class ProgressMeter implements ProgressListener {

    private long startTime = -1;

    /**
     * Callback triggered by a progress event. This implementation dumps the
     * events to stdout; it could equally well drive a GUI progress bar.
     */
    public void progressUpdate(int messageID, int progress, String note, long msec) {

        if (startTime < 0) {
            startTime = msec;
        }

        String tick = String.format("%7d", msec - startTime);
        String progressString = String.format("%3d", progress);

        String step = "";
        switch (messageID) {
            case CONVERSION_BEGIN:
                step = "conversion begin";
                break;
            case MAIN_DOC_READ:
                step = "doc read";
                break;
            case HTML_PARSED:
                step = "html parsed";
                break;
            case RENDERER_TREE_BUILT:
                step = "document tree structure built";
                break;
            case HTML_LAYOUT_IN_PROGRESS:
                step = "layouting...";
                break;
            case HTML_LAYOUT_DONE:
                step = "layout done";
                break;
            case PAGEBREAKS_ALIGNED:
                step = "pagebreaks aligned";
                break;
            case TOC_GENERATED:
                step = "TOC generated";
                break;
            case DOC_RENDER_IN_PROGRESS:
                step = "generating doc page";
                break;
            case RTF_PRE_RENDER_DONE:
                step = "RTF pre-render done";
                break;
            case DOC_WRITE_BEGIN:
                step = "writing doc...";
                break;
            case CONVERSION_END:
                step = "done.";
                break;
        }
        System.out.println(tick + " " + progressString + " " + step + " " + note);
    }
}

...

pd4ml.monitorProgressWith(new ProgressMeter());
```
{% endtab %}
{% endtabs %}

Source: [A004AddProgressListener.java](https://github.com/zxfr/pd4ml-examples/blob/master/src/main/java/advanced/A004AddProgressListener.java)

### Add Custom Resource Loader

When an HTML resource -- an image, a stylesheet -- can't be reached through the standard mechanisms (filesystem, HTTP(S)), a custom resource "driver" can be plugged in instead. Implementing one is a two-step process: first, choose a URL scheme/addressing convention that fits the use case; second, implement a loader derived from `com.pd4ml.ResourceProvider` that recognizes and resolves it. Between v3 and v4 the abstract method the loader implements changed shape -- `getResourceAsStream()` returning a stream in v3, `getResourceAsBytes()` returning a byte array in v4 -- and the registration mechanism changed from a class-name string passed to `addCustomResourceProvider()` to an entry in the map passed to `setDynamicParams()`.

{% tabs %}
{% tab title="PD4ML v4" %}
```java
public class DummyProvider extends ResourceProvider {

    public final static String PROTOCOL = "dummy";

    @Override
    public byte[] getResourceAsBytes(String resource, boolean debugOn) throws IOException {

        if (!resource.toLowerCase().startsWith(PROTOCOL)) {
            return null;
        }

        // interpret "resource" according to your own protocol (e.g. as a key
        // into a database record); here it's simply resolved to a local file
        ByteArrayOutputStream fos = new ByteArrayOutputStream();
        byte buffer[] = new byte[2048];

        resource = "file:src/html/rc/" + resource.substring(PROTOCOL.length() + 1);

        URL src = new URL(resource);
        URLConnection urlConnect = src.openConnection();
        try {
            urlConnect.connect();
        } catch (Throwable e) {
            return new byte[0];
        }
        InputStream is = urlConnect.getInputStream();
        BufferedInputStream bis = new BufferedInputStream(is);

        int read;
        do {
            read = is.read(buffer, 0, buffer.length);
            if (read > 0) {
                fos.write(buffer, 0, read);
            }
        } while (read > -1);

        fos.close();
        bis.close();
        is.close();

        return fos.toByteArray();
    }
}

PD4ML pd4ml = new PD4ML();

HashMap map = new HashMap();
map.put(PD4Constants.PD4ML_EXTRA_RESOURCE_LOADERS, "advanced.DummyProvider");
pd4ml.setDynamicParams(map);

String html = "<img src=\"dummy:w3c.svg\">";
StringReader bais = new StringReader(html);

File pdf = File.createTempFile("result", ".pdf");
FileOutputStream fos = new FileOutputStream(pdf);
// render and write the result as PDF
pd4ml.render(bais, fos);
```
{% endtab %}

{% tab title="PD4ML v3" %}
```java
public class DummyProvider extends ResourceProvider {

    public final static String PROTOCOL = "dummy";

    @Override
    public BufferedInputStream getResourceAsStream(String resource, FileCache cache) throws IOException {
        if (!resource.toLowerCase().startsWith(PROTOCOL)) {
            return null;
        }

        // interpret "resource" according to your own protocol (e.g. as a key
        // into a database record); here it's simply echoed back verbatim
        String buf = "[" + resource.substring(PROTOCOL.length() + 1) + "]";
        ByteArrayInputStream baos = new ByteArrayInputStream(buf.getBytes());
        return new BufferedInputStream(baos);
    }

    @Override
    public boolean canLoad(String resource, FileCache cache) {
        if (resource.toLowerCase().startsWith(PROTOCOL)) {
            return true;
        }
        return false;
    }
}

...

pd4ml.addCustomResourceProvider("advanced.DummyProvider");
```
{% endtab %}
{% endtabs %}

Source: [A005AddCustomResourceLoader.java](https://github.com/zxfr/pd4ml-examples/blob/master/src/main/java/advanced/A005AddCustomResourceLoader.java)

### Add Image Resampling Resource Loader

Only JPEG (and certain PNG variants) can be embedded into a PDF "as is"; other image formats are re-encoded as native PDF image streams, which compress considerably worse than GIF, let alone JPEG. One way to shrink the resulting file, at some cost to image fidelity, is to resample oversized images and convert them to JPEG on the way in. This resource loader caps embedded images at 800x400px, downscaling and re-encoding anything larger; images already within that budget pass through untouched.

```java
public class ConvertedImageProvider extends ResourceProvider {

    private int maxwidth = 800;
    private int maxheight = 400;

    @Override
    public BufferedInputStream getResourceAsStream(String resource, FileCache cache) throws IOException {
        if (!resource.toLowerCase().startsWith("https:") ||
            !resource.toLowerCase().endsWith(".png")) {
            return null;
        }

        // delegate to the standard HTTPS resource loader to fetch the image bytes
        com.pd4ml.cache.SslResourceProvider14 provider =
            new com.pd4ml.cache.SslResourceProvider14();
        byte[] img = provider.getResourceAsBytes(resource, cache);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        BufferedImage image = ImageIO.read(new ByteArrayInputStream(img));
        int width = image.getWidth();
        int height = image.getHeight();

        // dimensions unavailable, or already within the size budget
        if (width <= 0 || (height <= 0 && width < maxwidth && height < maxheight)) {
            ByteArrayInputStream bais = new ByteArrayInputStream(img);
            return new BufferedInputStream(bais);
        }

        double scale = Math.min((double) maxwidth / width, (double) maxheight / height);

        final BufferedImage convertedImage = new BufferedImage((int) (width * scale),
            (int) (height * scale), BufferedImage.TYPE_INT_RGB);

        if (scale != 1) {
            AffineTransform scaleTransform = AffineTransform.getScaleInstance(scale, scale);
            AffineTransformOp bilinearScaleOp = new AffineTransformOp(scaleTransform,
                AffineTransformOp.TYPE_BILINEAR);
            image = bilinearScaleOp.filter(image, new BufferedImage((int) (width * scale),
                (int) (height * scale), image.getType()));
        }

        convertedImage.createGraphics().drawImage(image, 0, 0, Color.WHITE, null);
        ImageIO.write(convertedImage, "JPEG", baos);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        return new BufferedInputStream(bais);
    }

    @Override
    public boolean canLoad(String resource, FileCache cache) {
        return resource.toLowerCase().startsWith("https:") &&
               resource.toLowerCase().endsWith(".png");
    }
}

...

pd4ml.addCustomResourceProvider("advanced.ConvertedImageProvider");
```

Source: [ConvertedImageProvider.java](https://github.com/zxfr/pd4ml-examples/blob/master/src/main/java/advanced/ConvertedImageProvider.java), [A005AddImageConvertingResourceLoader.java](https://github.com/zxfr/pd4ml-examples/blob/master/src/main/java/advanced/A005AddImageConvertingResourceLoader.java)

### Substitute Placeholders

Static HTML templates can carry dynamic content by way of `$[var1]`, `$[my.variable]`-style placeholders, resolved at conversion time against a supplied map -- a lighter-weight alternative to templating for simple substitutions. The three placeholders `$[page]`, `$[total]`, and `$[title]` are reserved for pagination and cannot be redefined this way. The only change between v3 and v4 is the name of the method the map is passed to.

{% tabs %}
{% tab title="PD4ML v4" %}
```java
HashMap<String, String> map = new HashMap<>();
map.put("var1", "value 1");
map.put("var2", "[value 2]");
map.put("var3", "* value 3 *");
map.put("my.variable", "Dynamically inserted text");
pd4ml.setDynamicParams(map);
```
{% endtab %}

{% tab title="PD4ML v3" %}
```java
HashMap<String, String> map = new HashMap<>();
map.put("var1", "value 1");
map.put("var2", "[value 2]");
map.put("var3", "* value 3 *");
map.put("my.variable", "Dynamically inserted text");
pd4ml.setDynamicData(map);
```
{% endtab %}
{% endtabs %}

Source: [A006SubstitutePlaceholders.java](https://github.com/zxfr/pd4ml-examples/blob/master/src/main/java/advanced/A006SubstitutePlaceholders.java)

### Rendering Status Info

After a conversion completes, `getLastRenderInfo()` retrieves statistics and diagnostics about it: the total page count, the actual layout height of the HTML document in pixels, an estimate of the document's natural width (useful for tuning `htmlWidth` -- if this comes back smaller than the configured `htmlWidth`, that smaller value is likely closer to optimal), and, when writing PDF/A, a structured list of conformance warnings and errors.

```java
// render and write the result as PDF/A
pd4ml.writePDF(fos, Constants.PDFA);

System.out.println("pages: " + (Long) pd4ml.getLastRenderInfo(Constants.PD4ML_TOTAL_PAGES));

// actual HTML document layout height in pixels (depends on the htmlWidth
// conversion parameter)
System.out.println("height: " + (Long) pd4ml.getLastRenderInfo(Constants.PD4ML_DOCUMENT_HEIGHT_PX));

// the document's natural layout width in pixels. If the document has
// root-level elements with width="100%", this will almost always equal
// htmlWidth; a smaller value suggests a smaller htmlWidth may be optimal.
System.out.println("right edge: " + (Long) pd4ml.getLastRenderInfo(Constants.PD4ML_RIGHT_EDGE_PX));

StatusMessage[] msgs =
    (StatusMessage[]) pd4ml.getLastRenderInfo(Constants.PD4ML_PDFA_STATUS);

for (int i = 0; i < msgs.length; i++) {
    System.out.println((msgs[i].isError() ? "ERROR: " : "WARNING: ") + msgs[i].getMessage());
}
```

Source: [A007RenderingStatusInfo.java](https://github.com/zxfr/pd4ml-examples/blob/master/src/main/java/advanced/A007RenderingStatusInfo.java)

### Adding Custom Tag Renderer

Beyond the built-in `<pd4ml:*>` tags, PD4ML allows entirely custom HTML tags to be registered with their own rendering logic -- the same extension point PD4ML itself uses to plug in its MathML and SVG renderers. The example below registers a `<star>` tag that renders, unsurprisingly, a star.

```java
String html = "TEST STAR [<star height=20 width=20 style='border: 1 solid blue'>]";
pd4ml.addCustomTagHandler("star", new StarTag());

ByteArrayInputStream bais = new ByteArrayInputStream(html.getBytes());
pd4ml.readHTML(bais);
```

Source: [A008AddingCustomTagRenderer.java](https://github.com/zxfr/pd4ml-examples/blob/master/src/main/java/advanced/A008AddingCustomTagRenderer.java)

## PDF Tools

Single-document PDF post-processing utilities built into the core engine -- distinct from, and considerably narrower than, the dedicated `com.pd4ml.pdf.*` packages (merge, optimizer, signing, COS, XFDF) documented separately in the PDF Tools section of this site. Note that the underlying class was renamed from `PdfDocument` (v3) to `PD4Document` (v4), alongside a handful of smaller signature adjustments.

### Convert And Merge With PDF

`merge()` appends an existing PDF document -- or a specific range of its pages -- to the result of an HTML-to-PDF conversion, all within the same `PD4ML` instance.

{% tabs %}
{% tab title="PD4ML v4" %}
```java
PD4ML pd4ml = new PD4ML(); // constructor implicitly registers the "java:" protocol

String html = "TEST<pd4ml:page.break><b>Hello, World!</b>";
StringReader bais = new StringReader(html);

PD4PageMark header = new PD4PageMark();
header.setHtmlTemplate("HEADER $[page] of $[total]");
header.setAreaHeight(40);
pd4ml.setPageHeader(header);

File pdfFile = new File("src/pdftools/PDFOpenParameters.pdf");
FileInputStream pdf = new FileInputStream(pdfFile);
// merge only with pages 2 through 4; they are appended to the converted PDF
pd4ml.merge(pdf, 2, 4, true);

File f = File.createTempFile("result", ".pdf");
FileOutputStream fos = new FileOutputStream(f);

// render and write the result as PDF
pd4ml.render(bais, fos);
```
{% endtab %}

{% tab title="PD4ML v3" %}
```java
URL pdfUrl = new URL("java:/pdftools/PDFOpenParameters.pdf");
PdfDocument pdf = new PdfDocument(pdfUrl, null);

File f = File.createTempFile("result", ".pdf");

pd4ml.setPageHeader("HEADER $[page] of $[total]", 40, "1+");

// merge only with pages 2 through 4; they are appended to the converted PDF
pd4ml.merge(pdf, 2, 4, true);

pd4ml.readHTML(new ByteArrayInputStream(html.getBytes()));
pd4ml.writePDF(new FileOutputStream(f));
```
{% endtab %}
{% endtabs %}

Source: [P001ConvertAndMergeWithPDF.java](https://github.com/zxfr/pd4ml-examples/blob/master/src/main/java/pdftools/P001ConvertAndMergeWithPDF.java)

### Merge Two PDFs

Combining two static PDF documents into one is a single `append()` call once each is loaded.

{% tabs %}
{% tab title="PD4ML v4" %}
```java
URL pdfUrl = new URL("java:/pdftools/PDFOpenParameters.pdf");
PD4Document pdf1 = new PD4Document(pdfUrl, null);
PD4Document pdf2 = new PD4Document(pdfUrl, null);

File f = File.createTempFile("pdf", ".pdf");

pdf1.append(pdf2);
pdf1.write(new FileOutputStream(f));
```
{% endtab %}

{% tab title="PD4ML v3" %}
```java
URL pdfUrl1 = new URL("java:/pdftools/doc1.pdf");
URL pdfUrl2 = new URL("java:/pdftools/doc2.pdf");
PdfDocument pdf1 = new PdfDocument(pdfUrl1, null);
PdfDocument pdf2 = new PdfDocument(pdfUrl2, null);

File f = File.createTempFile("pdf", ".pdf");

pdf1.append(pdf2);
pdf1.write(new FileOutputStream(f));
```
{% endtab %}
{% endtabs %}

Source: [P002MergeTwoPDFs.java](https://github.com/zxfr/pd4ml-examples/blob/master/src/main/java/pdftools/P002MergeTwoPDFs.java)

### Merge Two PDFs And Protect With Password

Extending the previous example, the merged result can also be password-protected with a reduced permission set in the same `write()` call.

{% tabs %}
{% tab title="PD4ML v4" %}
```java
URL pdfUrl = new URL("java:/pdftools/PDFOpenParameters.pdf");
PD4Document pdf1 = new PD4Document(pdfUrl, null);
PD4Document pdf2 = new PD4Document(pdfUrl, null);

File f = File.createTempFile("pdf", ".pdf");

pdf1.append(pdf2);
pdf1.write(new FileOutputStream(f), "test",  // protect the result with password "test"
    PD4Constants.AllowDegradedPrint | PD4Constants.AllowAnnotate);
```
{% endtab %}

{% tab title="PD4ML v3" %}
```java
URL pdfUrl1 = new URL("java:/pdftools/doc1.pdf");
URL pdfUrl2 = new URL("java:/pdftools/doc2.pdf");
PdfDocument pdf1 = new PdfDocument(pdfUrl1, null);
PdfDocument pdf2 = new PdfDocument(pdfUrl2, null);

File f = File.createTempFile("pdf", ".pdf");

pdf1.append(pdf2);
pdf1.write(new FileOutputStream(f), "test", // protect the result with password "test"
    Constants.AllowDegradedPrint | Constants.AllowAnnotate);
```
{% endtab %}
{% endtabs %}

Source: [P003MergeTwoPDFsAndProtectWithPassword.java](https://github.com/zxfr/pd4ml-examples/blob/master/src/main/java/pdftools/P003MergeTwoPDFsAndProtectWithPassword.java)

### Update Pdf Meta Info

Document metadata -- title, subject, keywords, modification date -- on an existing PDF can be read and rewritten directly, without a full HTML conversion.

{% tabs %}
{% tab title="PD4ML v4" %}
```java
URL pdfUrl = new URL("java:/pdftools/PDFOpenParameters.pdf");
PD4Document doc = new PD4Document(pdfUrl, null);

System.out.println("document author: " + doc.getAuthor());

doc.setTitle("Document Modification Test");
doc.setSubject("PdfDocument API test");
doc.setKeywords("key1, key2");
doc.setModDate(); // set modification date to now

File f = File.createTempFile("pdf", ".pdf");

doc.write(new FileOutputStream(f), null, -1); // no password, default permissions
```
{% endtab %}

{% tab title="PD4ML v3" %}
```java
PdfDocument doc = new PdfDocument(pdfUrl, null);

System.out.println("document author: " + doc.getAuthor());

doc.setTitle("Document Modification Test");
doc.setSubject("PdfDocument API test");
doc.setKeywords("key1, key2");
doc.setModDate(); // set modification date to now

doc.write(new FileOutputStream(f), null, -1); // no password, default permissions
```
{% endtab %}
{% endtabs %}

Source: [P004UpdatePdfMetaInfo.java](https://github.com/zxfr/pd4ml-examples/blob/master/src/main/java/pdftools/P004UpdatePdfMetaInfo.java)

### Underlay/Overlay

A more specialized form of merging: `overlay()` composites one document's pages on top of another's, and `underlay()` does the reverse, with an explicit opacity value and independent page-range scoping for the base and the composited content.

{% tabs %}
{% tab title="PD4ML v4" %}
```java
URL pdfUrl = new URL("java:/pdftools/PDFOpenParameters.pdf");
PD4Document doc1 = new PD4Document(pdfUrl, null);
PD4Document doc2 = new PD4Document(pdfUrl, null);

// place doc2's content over doc1's:
// "1" restricts the overlay source to doc2's first page
// "2+" applies the overlay to doc1's second page and all pages after it
// 128 is the overlay opacity (~50%)
doc1.overlay(doc2, "1", "2+", 128);
// doc1.underlay(doc2, "1", "2+", 128);

File f = File.createTempFile("pdf", ".pdf");

// write the composited result as a new PDF document
FileOutputStream fos = new FileOutputStream(f);
doc1.write(fos);
```
{% endtab %}

{% tab title="PD4ML v3" %}
```java
PdfDocument doc1 = new PdfDocument(pdfUrl, null);
PdfDocument doc2 = new PdfDocument(pdfUrl, null);

// place doc2's content over doc1's:
// "1" restricts the overlay source to doc2's first page
// "2+" applies the overlay to doc1's second page and all pages after it
// 128 is the overlay opacity (~50%)
doc1.overlay(doc2, "1", "2+", 128);
// doc1.underlay(doc2, "1", "2+", 128);

File f = File.createTempFile("pdf", ".pdf");

// write the composited result as a new PDF document
FileOutputStream fos = new FileOutputStream(f);
doc1.write(fos);
```
{% endtab %}
{% endtabs %}

Source: [P005UnderlayOverlay.java](https://github.com/zxfr/pd4ml-examples/blob/master/src/main/java/pdftools/P005UnderlayOverlay.java)

## Source repository

Every example above -- along with its supporting HTML fixtures and resources -- is part of the [`pd4ml-examples`](https://github.com/zxfr/pd4ml-examples) repository on GitHub, organized by section (`basics/`, `html/`, `i18n/`, `advanced/`, `pdftools/`) and ready to clone and run directly.
