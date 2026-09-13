# PD4ML Programmer's Manual

_Available starting from PD4ML v4.1.1_

PD4ML is a pure-Java document generation engine that turns HTML and CSS into production-quality **PDF**, **DOCX**, and **RTF** — no browser, no native dependencies, no external rendering service. It runs anywhere a JVM does: inside a web application, a batch job, a desktop tool, or straight from the command line.

Beyond the core HTML-to-PDF conversion covered in this manual, PD4ML also ships a small suite of PDF _post-processing_ packages -- signing, merging, optimizing, COS-level editing, and XFDF annotation import/export -- each with its own dedicated manual; see §15.

## 1. What PD4ML does

At its core, PD4ML is a layout engine: it parses HTML/CSS (or arbitrary XML plus an XSL stylesheet) the same way a browser would, paginates the result, and writes it out in one of several formats. What sets it apart from "print to PDF" approaches built on a headless browser:

* **Deterministic, print-focused pagination** -- explicit page breaks, repeating headers/footers, running page numbers, widow/orphan control, and multi-column layout, all driven by ordinary CSS plus a handful of PD4ML-specific extensions.
* **Standards-aware output** -- PDF/A-1/2/3/4 (long-term archival), PDF/UA (accessible, tagged PDF), and ZUGFeRD/Factur-X (machine-readable e-invoices embedded in a human-readable PDF) are first-class output targets, not an afterthought (§11).
* **Full Unicode and font control** -- TrueType/OpenType embedding, glyph subsetting, kerning, and ligature substitution, with correct shaping for Latin, Cyrillic, CJK, Arabic, and Hebrew scripts.
* **Multiple output formats from one conversion** -- the same parsed document can be written as PDF, DOCX, RTF, or rendered to raster images, without re-parsing the source (§6.3).
* **Deployment flexibility** -- a plain Java API, a self-contained command-line tool (§4), a GUI previewer (§5), and a JSP tag library for web applications (§13).

PD4ML is commercially licensed, with a free evaluation mode (watermarked output, no restriction on functionality) so an integration can be built and tested before a license is purchased.

## 2. Obtaining PD4ML

PD4ML is distributed as a single, self-contained jar plus a required Bouncy Castle companion (`com.pd4ml.pdf.encryption` -- standard PDF permissions/ protection, §11.1 -- and `com.pd4ml.pdf.sign`, §15.2, both depend on it; as of v4.1.0 it is no longer an optional, signing-only add-on) through PD4ML's own Maven repository:

```xml
<dependency>
    <groupId>com.pd4ml</groupId>
    <artifactId>pd4ml</artifactId>
    <version>4.1.1</version>
</dependency>

<!-- Required as of v4.1.0 -- com.pd4ml.pdf.encryption (document protection,
     &sect;11.1) and com.pd4ml.pdf.sign (&sect;15.2) both depend on it. A Maven
     build already pulls these in transitively via the dependency above;
     declare them explicitly only to pin a specific version. -->
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcprov-jdk18on</artifactId>
    <version>1.84</version>
</dependency>
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcpkix-jdk18on</artifactId>
    <version>1.84</version>
</dependency>

<repository>
    <id>pd4ml</id>
    <url>https://pd4ml.com/maven2/</url>
</repository>
```

A separate, credentialed repository (`https://pd4ml.com/maven2-src/`) also publishes source jars, for stepping through PD4ML's own code in a debugger during integration work.

Without Maven, the plain jar plus its `bouncycastle-runtime.jar` companion (a single jar merging the Bouncy Castle artifacts above -- see the note in §15.2) can be downloaded directly and placed on the classpath; nothing else needs to be installed to run it.

## 3. Licensing

```java
PD4ML pd4ml = new PD4ML();                 // looks for pd4ml.lic on the classpath / working directory
PD4ML pd4ml = new PD4ML("SERIAL-NUMBER");   // the serial number from the license email
PD4ML pd4ml = new PD4ML("https://internal.example.com/pd4ml.lic");  // license file URI, instead of a serial number
```

With no license installed, PD4ML runs in **evaluation mode**: every feature is available, but output carries a watermark. This is normal and expected during development -- there is nothing to configure to unlock full, unwatermarked output beyond installing a valid license file or serial number.

```java
if (pd4ml.isDemoMode()) {
    log.warn("PD4ML is running unlicensed -- output will be watermarked");
}
System.out.println("PD4ML " + PD4ML.getVersion() + " built " + PD4ML.getVersionBuildDate());
```

## 4. Command-line tool: `Pd4Cmd`

`com.pd4ml.tools.Pd4Cmd` is `pd4ml.jar`'s own `Main-Class` -- run it directly with `java -jar`, no classpath assembly required:

```
java -Djava.awt.headless=true -Xmx512m -jar pd4ml.jar "https://example.com" 1200
```

The two required positional arguments are the **source** (a URL, or a local `file:` path) and the **`htmlWidth`** in pixels -- the virtual browser viewport width PD4ML lays the page out against before scaling it to the target paper size (see §7 for the scaling math). Everything else is an optional flag.

### 4.1 Common flags

| Flag                                             | Purpose                                                |
| ------------------------------------------------ | ------------------------------------------------------ |
| `-out <file>`                                    | Output file (default: stdout)                          |
| `LETTER`, `A4`, `LEGAL`, ... (positional)        | Target page format                                     |
| `-orientation PORTRAIT\|LANDSCAPE`               | Page orientation                                       |
| `-insets <l,t,r,b[,mm\|points]>`                 | Page margins                                           |
| `-bookmarks HEADINGS\|ANCHORS`                   | Generate a bookmark tree                               |
| `-header "<html>"`, `-footer "<html>"`           | Repeating page header/footer HTML                      |
| `-watermark "<url,left,top,width,height,angle>"` | Image watermark                                        |
| `-bgcolor <#rrggbb>`, `-bgimage <url>`           | Page background                                        |
| `-pdfforms`                                      | Convert HTML `<form>` controls to real PDF form fields |
| `-outformat pdf\|pdfa\|pdfua\|...`               | PDF/A / PDF/UA / ZUGFeRD conformance (§11.2)           |
| `-password <pw>`                                 | Owner password + default permissions                   |
| `-permissions <n>`                               | Explicit permission bitmask (§11.1)                    |
| `-ttf <dir>`                                     | TrueType/OpenType font directory to embed from         |
| `-kerning`, `-ligatures`                         | Font shaping refinements (§9)                          |
| `-threads <n>`                                   | Async resource-loader thread pool size                 |
| `-nohyperlinks`                                  | Disable hyperlink conversion                           |
| `-adjustwidth`                                   | Auto-fit `htmlWidth` to the document's natural width   |
| `-fitapage`                                      | Downscale the whole layout to fit one page vertically  |
| `-multicolumn <columns,gap[,mm]>`                | Multi-column page layout                               |
| `-debug <level>`                                 | Verbosity (0-5)                                        |

Run `java -jar pd4ml.jar` with no arguments for the complete, always-current flag reference.

### 4.2 Examples

Convert a live page to a bookmarked, form-enabled Letter PDF:

```
java -Djava.awt.headless=true -Xmx512m -jar pd4ml.jar \
    "https://example.com/report.html" 1200 LETTER \
    -bookmarks HEADINGS -pdfforms -out report.pdf
```

Render a local file with a running header/footer and page numbers:

```
java -jar pd4ml.jar "file:/data/invoice.html" 900 A4 \
    -header "<div style='text-align:right'>$[title]</div>" \
    -footer "<div style='text-align:center'>Page $[page] of $[total]</div>" \
    -out invoice.pdf
```

`Pd4Cmd` also carries a handful of built-in, single-document PDF **post-processing** flags for quick one-off tasks -- `-tools <file> ...` switches it from HTML conversion into edit mode:

```
# Keep only pages 2-3 and everything from page 5 onward
java -jar pd4ml.jar -tools file:/docs/source.pdf -pagerange 2-3,5+ -out subset.pdf

# Append another PDF after the source document
java -jar pd4ml.jar -tools file:/docs/source.pdf -merge file:/docs/appendix.pdf after -out combined.pdf

# Read back permission flags, author, title and page count
java -jar pd4ml.jar -tools file:/docs/source.pdf -printpermissions -printauthor -printtitle -printpagenum
```

For anything beyond these simple cases -- selecting page ranges from _two_ independent source documents with accessibility-tag reconciliation, reachability-based cleanup and duplicate-content dedup, or arbitrary COS-level structural edits -- reach for the dedicated `pd4mergecli`, `pd4optimizecli` and `pd4coscli` tools instead (§15).

### 4.3 Font indexing

```
java -Xmx512m -jar pd4ml.jar -configure.fonts /path/to/fonts [pd4fonts.properties]
```

Scans a directory of TrueType/OpenType font files and writes (or updates) a font-mapping properties file PD4ML uses to resolve CSS `font-family` names to actual font files at conversion time -- run once per font directory, ahead of time, rather than paying the directory scan cost on every conversion.

## 5. GUI application: `PD4Browser`

A lightweight preview tool bundled in the same jar -- useful for iterating on CSS/layout without a full round trip through your application:

```
java -jar pd4ml.jar -gui
```

`[...]` opens an HTML or SVG file; `[GO]` re-renders it; `[PDF]`/`[RTF]`/ `[DOCX]` convert the current document to that format. Its own behavior (page format, margins, default fonts, proxy, bookmark generation, ...) is controlled by a `pd4browser.properties` file in the working directory -- the same settings a Java integration would set via the API in §6-9, exposed as plain properties for quick experimentation.

## 6. The Java API

### 6.1 The two-phase model

Every conversion is **read** once, then **written** one or more times: a `readHTML(...)` call parses and lays out the source; any number of subsequent `write*(...)`/`renderAsImages()` calls then serialize that same parsed document to different formats or destinations, without re-parsing.

```java
import com.pd4ml.PD4ML;
import java.io.*;

PD4ML pd4ml = new PD4ML();
String html = "<html><body><h1>Hello, World!</h1></body></html>";
pd4ml.readHTML(new ByteArrayInputStream(html.getBytes("UTF-8")), null, "UTF-8");

try (FileOutputStream out = new FileOutputStream("hello.pdf")) {
    pd4ml.writePDF(out);
}
```

### 6.2 Reading the source

```java
pd4ml.readHTML(inputStream);                          // encoding auto-detected
pd4ml.readHTML(inputStream, baseUrl);                  // relative links/images resolve against baseUrl
pd4ml.readHTML(inputStream, baseUrl, "windows-1251");  // explicit source encoding
pd4ml.readHTML(new URL("https://example.com/report"));  // fetch directly
```

A relative-URL `baseUrl` matters whenever the source HTML references external images, stylesheets, or fonts by relative path -- omitting it (or getting it wrong) is the most common cause of a converted document missing images that render fine in a browser.

### 6.3 Writing the output

```java
pd4ml.writePDF(pdfOutputStream);
pd4ml.writeRTF(rtfOutputStream, false);   // false: keep raster images as PNG/JPEG rather than converting to WMF
pd4ml.writeDOCX(docxOutputStream);

BufferedImage[] pages = pd4ml.renderAsImages();           // one image per page, in memory
byte[][] pngBytes     = pd4ml.renderAsImages("png");      // ... or already PNG-encoded
File dir              = pd4ml.renderAsImages(outDir, "page", "png");  // ... or written straight to disk
```

Because reading happens once, converting the same source to PDF _and_ a page-image thumbnail set costs one HTML parse, not two:

```java
pd4ml.readHTML(new URL("https://example.com/report"));
pd4ml.writePDF(new FileOutputStream("report.pdf"));
BufferedImage[] thumbnails = pd4ml.renderAsImages();
```

## 7. Page layout: size, margins, and scaling

Three settings govern how source content maps onto printed pages:

| Setting       | Meaning                                                    | Default         |
| ------------- | ---------------------------------------------------------- | --------------- |
| `pageSize`    | Target paper format                                        | A4, portrait    |
| `pageMargins` | Blank border reserved on every page                        | 10 mm all sides |
| `htmlWidth`   | Virtual viewport width (px) the source is laid out against | 727 px          |

```java
import com.pd4ml.PageSize;
import com.pd4ml.PageMargins;
import com.pd4ml.Dimensions.Units;

pd4ml.setPageSize(PageSize.LETTER);
pd4ml.setPageMargins(new PageMargins(15, 20, 15, 20, Units.MM));
pd4ml.setHtmlWidth(1000);
```

Content is scaled uniformly to fit the printable area:

```
scale = (pageWidth - marginLeft - marginRight) / htmlWidth
```

A narrower `htmlWidth` means a larger scale factor -- text and images appear bigger on the page, at the cost of how much content fits horizontally before wrapping. `pd4ml.adjustHtmlWidth(true)` (or the CLI's `-adjustwidth`) instead measures the document's own natural width and uses that, useful for source HTML that isn't already responsive-width-aware.

`setPageSize(...)`/`setPageMargins(...)` also accept an optional **scope** string (`"1"`, `"2+"`, `"3-7,odd"`, ...) to vary page size or margins across the document -- the same scope grammar used by headers/footers (§8) and by `Pd4Cmd -pagerange`.

`fitPageVertically(Constants.CENTER_ALIGN)` downscales the entire layout to guarantee it fits one single page, rather than paginating -- handy for "print this as a single-page summary" use cases; it's mutually exclusive with multi-column mode (§10.1).

## 8. Headers, footers, watermarks, and backgrounds

Repeating page decorations are themselves HTML/CSS fragments, not a separate mini-format to learn:

```java
pd4ml.setPageHeader("<b>$[title]</b>", 30, "1");             // first page only
pd4ml.setPageHeader("<b>$[title]</b> &mdash; $[page]/$[total]", 30, "2+");  // every page after
pd4ml.setPageFooter(
    "<div style='width:100%; text-align:right; font-size:8pt'>Page $[page] of $[total]</div>",
    24, "1+");
```

Three placeholders are always available: `$[page]` (current page number), `$[total]` (total page count), and `$[title]` (document title). Arbitrary additional placeholders come from `setDynamicData(...)`:

```java
Map<String, String> data = new HashMap<>();
data.put("customer", "Acme Corp.");
data.put("invoiceNo", "INV-2026-0031");
pd4ml.setDynamicData(data);
pd4ml.setPageFooter("<div>Invoice $[invoiceNo] &mdash; $[customer]</div>", 24, "1+");
```

A watermark, and a full-bleed page background rendered underneath every page's content, follow the same HTML-fragment-plus-scope model -- the watermark content is itself HTML (an `<img>`, styled text, or anything else CSS can lay out), not a bare image URL:

```java
pd4ml.setWatermark("<img src='https://example.com/draft-stamp.png'/>",
        50, 50,      // left, top
        0.15f,       // opacity
        30,          // rotation angle, degrees
        1.0f,        // scale
        true, true,  // visible on screen and in print
        "1+");

pd4ml.setPageBackground("<div style='background:#f7f7f7; height:100%'></div>", "1+");
```

## 9. Fonts, bookmarks, and hyperlinks

```java
pd4ml.useTTF("/opt/fonts");                 // TrueType/OpenType directory to embed glyphs from
pd4ml.embedTTFs(true, false);               // embed only the glyphs actually used, not the full font
pd4ml.applyKerning(true);                   // honor the font's own kerning pairs
pd4ml.enableLigatures(true);                // build compound glyphs (fi, fl, ...) where the font defines them

pd4ml.generateBookmarksFromHeadings(true);  // one bookmark per <h1>-<h6>
pd4ml.generateBookmarksFromAnchors(true);   // ... or from named <a name="..."> anchors instead
pd4ml.enableHyperlinks(true);               // convert <a href> into live PDF link annotations (default: on)
```

PD4ML ships with broad Unicode coverage out of the box; `useTTF(...)` matters when the source document needs a _specific_ font family (a corporate typeface, a CJK font with particular glyph coverage, ...) rather than PD4ML's own font substitution.

## 10. Forms, multi-column layout, and dynamic content

### 10.1 Interactive PDF forms

```java
pd4ml.generateForms(true, null);   // convert <input>/<select>/<textarea> to real PDF form fields
```

The resulting fields are ordinary AcroForm widgets -- readable and fillable in any PDF viewer, and, downstream, exactly what `com.pd4ml.pdf.xfdf` (§15.5) reads values out of and writes values back into via XFDF.

### 10.2 Multi-column pages

```java
pd4ml.generateMulticolumn(2, 20, true);   // 2 columns, 20mm gap
```

Not combinable with `fitPageVertically(...)` (§7) -- both compete to control the overall page layout.

### 10.3 Injecting HTML and custom resource providers

```java
pd4ml.injectHtml("<div class='cover-note'>Generated " + LocalDate.now() + "</div>", true);
```

`injectHtml` splices a fragment right after the opening `<body>` (or, with `prependBodyContent=false`, right before the closing `</body>`) -- useful for adding a fixed cover note or trailer without having to reassemble the whole source document string.

A custom `ResourceProvider` lets PD4ML resolve resources through a scheme of your own choosing -- a CMS attachment ID, a database BLOB, a signed internal URL -- rather than only `http(s)://`/`file:`:

```java
public class DatabaseImageProvider extends ResourceProvider {

    public static final String PROTOCOL = "db-image";

    @Override
    public boolean canLoad(String resource, FileCache cache) {
        return resource.toLowerCase().startsWith(PROTOCOL);
    }

    @Override
    public BufferedInputStream getResourceAsStream(String resource, FileCache cache) throws IOException {
        String imageId = resource.substring(PROTOCOL.length() + 1);
        byte[] bytes = imageRepository.loadBytes(imageId);   // your own lookup
        return new BufferedInputStream(new ByteArrayInputStream(bytes));
    }
}
```

```java
pd4ml.addCustomResourceProvider("com.example.DatabaseImageProvider");
```

```html
<img src="db-image:41208" alt="Signature" />
```

## 11. Document security, PDF/A, PDF/UA, and ZUGFeRD/Factur-X

### 11.1 Permissions and encryption

`setPermissions(...)` is implemented by `com.pd4ml.pdf.encryption`, which depends on Bouncy Castle -- see §2 for the required Maven artifacts (or `bouncycastle-runtime.jar` alongside the plain jar).

```java
import com.pd4ml.Constants;

// Owner password "secret"; readers can view and print, nothing else
pd4ml.setPermissions("secret", Constants.AllowPrint);

// Everything allowed except modifying the document
pd4ml.setPermissions(null, Constants.DefaultPermissions ^ Constants.AllowModify);

// AES-256 instead of the default encryption method (AES-128)
pd4ml.setPermissions("secret", Constants.DefaultPermissions, Constants.AES_256BIT);
```

`Constants` defines one bit flag per PDF permission (`AllowPrint`, `AllowModify`, `AllowCopy`, `AllowAnnotate`, `AllowFillingForms`, `AllowContentExtraction`, `AllowAssembly`, `AllowDegradedPrint`, ...) -- combine with `|`, or start from `DefaultPermissions` (everything allowed) and subtract with `^`. The optional third argument to `setPermissions` picks the encryption method: `RC4_40BIT`, `RC4_128BIT`, `AES_128BIT` (the default), or `AES_256BIT`.

### 11.2 PDF/A, PDF/UA, and e-invoicing formats

```java
pd4ml.writePDF(out, Constants.PDFA);    // PDF/A-1b: long-term archival, all fonts embedded
pd4ml.writePDF(out, Constants.PDFUA);   // PDF/UA + PDF/A-2a: accessible, tagged PDF
```

As of v4.1.0, the `Constants` shortcuts above are just two fixed points on a much larger surface exposed by `writePDF(OutputStream, PdfSpec)`. A `PdfSpec` names one target specification -- a PDF version, a PDF/A or PDF/UA conformance level, or one of the e-invoicing presets below -- and `combine()` layers any number of them onto a single call:

```java
import com.pd4ml.PdfSpec;

pd4ml.writePDF(out, PdfSpec.PDFA_3B);
pd4ml.writePDF(out, PdfSpec.PDF_1_7_8.combine(PdfSpec.PDFA_3B));
```

**PDF version.** `PdfSpec` names every PDF version from 1.4 through 2.0, including the numbered ISO extension levels that Acrobat itself has historically used to track incremental additions to the 1.7 line:

```java
PdfSpec.PDF_1_4
PdfSpec.PDF_1_5
PdfSpec.PDF_1_6
PdfSpec.PDF_1_7
PdfSpec.PDF_1_7_1   // through
PdfSpec.PDF_1_7_11  // ISO 32000-1 extension levels 1-11
PdfSpec.PDF_2_0     // ISO 32000-2
```

**PDF/A (long-term archival).** Ten constants cover PDF/A-1 through PDF/A-4, each named for its conformance level: `a` (**A**ccessible -- adds tagged-PDF structure on top of `b`), `b` (**B**asic -- visual reproducibility only), and, from PDF/A-2 onward, `u` (**U**nicode -- guarantees text is extractable/searchable without necessarily being fully tagged). PDF/A-4 drops the letter scheme in favor of a plain baseline plus an `e` (**E**mbedded-files) variant, continuing PDF/A-3's relaxation of the earlier prohibition on arbitrary embedded files:

```java
PdfSpec.PDFA_1A
PdfSpec.PDFA_1B
PdfSpec.PDFA_2A
PdfSpec.PDFA_2B
PdfSpec.PDFA_2U
PdfSpec.PDFA_3A
PdfSpec.PDFA_3B
PdfSpec.PDFA_3U
PdfSpec.PDFA_4
PdfSpec.PDFA_4E
```

**PDF/UA (accessibility).** Two levels are available, tied to the two ISO base specifications: `PDFUA_1` (built on PDF 1.7 / ISO 32000-1) and `PDFUA_2` (built on PDF 2.0 / ISO 32000-2 -- it can only be combined with `PdfSpec.PDF_2_0`, not an earlier version):

```java
PdfSpec.PDFUA_1
PdfSpec.PDFUA_2   // requires PdfSpec.PDF_2_0
```

Any of these can be layered together in a single call, as long as the combination is one the PDF/A and PDF/UA specifications themselves permit -- PDF version, archival level, and accessibility level are independent axes:

```java
pd4ml.writePDF(out, PdfSpec.PDFUA_1);
pd4ml.writePDF(out, PdfSpec.PDF_1_7_8.combine(PdfSpec.PDFUA_1));
pd4ml.writePDF(out, PdfSpec.PDF_1_7_11.combine(PdfSpec.PDFA_3A).combine(PdfSpec.PDFUA_1));
```

**ZUGFeRD and Factur-X.** Four further constants bundle the exact PDF version and PDF/A level each e-invoicing standard requires into one name, so the underlying `combine()` call never needs to be written out by hand. The plain and `_a` forms differ only in PDF/A conformance level -- `_a` produces a PDF/A-3**a** (accessible, tagged) container instead of the baseline PDF/A-3**b**:

| Constant            | Equivalent to                              |
| ------------------- | ------------------------------------------ |
| `PdfSpec.ZUGFeRD`   | `PdfSpec.PDF_1_7.combine(PdfSpec.PDFA_3B)` |
| `PdfSpec.ZUGFeRD_a` | `PdfSpec.PDF_1_7.combine(PdfSpec.PDFA_3A)` |
| `PdfSpec.FacturX`   | `PdfSpec.PDF_1_7.combine(PdfSpec.PDFA_3B)` |
| `PdfSpec.FacturX_a` | `PdfSpec.PDF_1_7.combine(PdfSpec.PDFA_3A)` |

```java
pd4ml.writePDF(os, PdfSpec.ZUGFeRD);
pd4ml.writePDF(os, PdfSpec.FacturX);
```

These presets only select the container's PDF/A conformance level -- they don't attach the invoice XML itself, which is a separate step covered in full, including the required attachment filenames and a working end-to-end example, on the [ZUGFeRD and Factur-X PDF Invoices page](https://app.gitbook.com/s/cjbNvevStmXi1uYduM7U/) in the Features section of this site.

Not every combination is meaningful -- pairing `PDFUA_2` with a pre-2.0 PDF version is the clearest example -- and `combine()` rejects an invalid pairing by throwing `PdfSpecViolationException` or `PdfSpecUnsupportedException` rather than silently producing a non-conformant file; catch (or let propagate) whichever of the two applies while the exact target spec for a given document is still being worked out.

PDF/UA conformance depends on the _source_ HTML being well-structured (real headings, `<table>` markup for tabular data, `alt` text on images, ...) as much as on PD4ML's own output -- pairing any of the targets above with `setErrorPolicy(Constants.ErrorPolicy.PEDANTIC)` surfaces a source document that doesn't actually satisfy the requested standard, rather than silently emitting non-conformant output.

## 12. Extensibility and diagnostics

```java
pd4ml.setErrorPolicy(Constants.ErrorPolicy.PEDANTIC);  // fail fast on malformed source / non-conformant output
pd4ml.setLogLevel(3);                                   // 0 (silent) .. 5 (very verbose)

// ProgressListener callback: messageID identifies the conversion phase (see
// ProgressListener's own constants), progress is a 0-50 estimate, msec is
// elapsed time since conversion start
pd4ml.monitorProgressWith((messageID, progress, message, msec) ->
        System.out.println(progress + "% - " + message));

// Fetch images/attachments with up to 8 concurrent threads (avoid in a
// Java EE container that restricts application-managed thread creation)
pd4ml.asyncResourceLoader(8);

pd4ml.addMetadata("InvoiceNumber", "INV-2026-0031", false);            // custom PDF document-info entry
pd4ml.addDocumentActionHandler("OpenAction", "app.alert('Welcome');"); // PDF-embedded JavaScript
```

`getLastRenderInfo(Constants.PD4ML_TOTAL_PAGES)` and `getLastRenderInfo(Constants.PD4ML_PDFA_STATUS)` retrieve information about the just-completed render -- how many pages it produced, or whether the requested PDF/A conformance was actually achieved -- after `writePDF(...)` returns.

## 13. Web integration: the JSP tag library

Copy `pd4ml*.jar` into `WEB-INF/lib` and the tag library is available to any JSP page, no further deployment step needed. Two taglib URIs are published, for a classic `javax.servlet.jsp` container and a Jakarta EE 9+/`jakarta.servlet.jsp` one respectively -- pick the one matching your servlet container:

```jsp
<%@ taglib uri="http://pd4ml.com/tlds/4.1" prefix="pd4tl" %>      <%-- javax --%>
<%@ taglib uri="http://pd4ml.com/tlds/4.1-jk" prefix="pd4tl" %>   <%-- jakarta --%>
```

```jsp
<%@ taglib uri="http://pd4ml.com/tlds/4.1" prefix="pd4tl" %><%@ page
    contentType="application/pdf; charset=UTF-8" %><pd4tl:transform
    screenWidth="1000"
    pageFormat="A4"
    pageInsets="15,20,15,20,mm">
<html>
<head><style>body { font-family: "Helvetica", sans-serif; }</style></head>
<body>
    <h1>${invoice.number}</h1>
    <pd4ml:page.footer height="24">Page $[page] of $[total]</pd4ml:page.footer>
    <!-- ... document body ... -->
</body>
</html>
</pd4tl:transform>
```

No leading whitespace Nothing may precede the `<%@ taglib %>` directive - the tag streams binary PDF bytes straight to the response, and even a single leading newline emitted before it corrupts the output. Frameworks that wrap every JSP in a shared layout (Struts, JSF, ...) should render the PD4ML page from its own, undecorated JSP rather than through the framework's normal view pipeline.

To save the generated file server-side instead of streaming it to the client:

```jsp
<pd4tl:savefile
    dir="/var/spool/generated-pdfs"
    redirect="/app/download.jsp"
    debug="false" />
```

`redirect` receives a `filename` request parameter naming the file just written under `dir` -- `download.jsp` (or a plain servlet) then streams, emails, or archives it as your application requires.

## 14. IBM Notes/Domino integration

A Notes/Domino document can be converted the same way any other HTML source is -- either fetched live over HTTP:

```
http://<host>/<database>/<view>/<documentUNID>?OpenDocument
```

or exported to **DXL** (Domino's XML document representation) and converted through an XSL stylesheet PD4ML ships for that purpose:

```
java -Djava.awt.headless=true -Xmx512m -jar pd4ml.jar document.dxl 1200 -xsl notesdefault
```

Either approach works equally well from a scheduled Domino agent (Java or LotusScript, shelling out to `Pd4Cmd`) or an external batch process with network access to the Domino server.

## 15. The PDF post-processing toolkit

Alongside HTML-to-PDF conversion, PD4ML ships five focused packages for working with _existing_ PDFs -- each is a complete, independently-documented feature with its own manual, runnable examples, and command-line tool; this section only orients you to which one covers which job.

### 15.1 `com.pd4ml.pdf.cos` -- the PDF object model

The low-level PDF object model, parser, COS-path query language, and incremental-update writer everything below is built on. Reach for it directly for structural edits nothing else here covers -- rotating a page, patching an arbitrary dictionary key, auditing a document's object graph. See the [PD4ML COS API Manual](https://app.gitbook.com/s/plLYBuldCPBrA6JYjseN/pd4ml-pdf-cos/pd4ml-cos-api-manual) and [pd4coscli Reference](https://app.gitbook.com/s/plLYBuldCPBrA6JYjseN/pd4ml-pdf-cos/pd4coscli-reference).

### 15.2 `com.pd4ml.pdf.sign` -- digital signatures

PKCS#12/PKCS#11/HSM/cloud-KMS signing, RFC 3161 timestamping, DocMDP certification, and PAdES-LT/LTV long-term validation. The only one of the five post-processing packages that needs Bouncy Castle on the classpath -- already required regardless by the core engine's own document protection, §11.1 -- see the [PD4ML Signing Manual](https://app.gitbook.com/s/plLYBuldCPBrA6JYjseN/pd4ml-pdf-signing/pd4ml-signing-manual) and [pdfsigncli Reference](https://app.gitbook.com/s/plLYBuldCPBrA6JYjseN/pd4ml-pdf-signing/pd4signcli-reference).

### 15.3 `com.pd4ml.pdf.merge` -- combining PDFs

Selects independent page ranges from two or more source PDFs and merges them into one structurally-correct document, reconciling tagged (accessibility) structure across sources. See the [PD4ML PDF Merge Manual](https://app.gitbook.com/s/plLYBuldCPBrA6JYjseN/pd4ml-pdf-merge/pd4ml-merge-manual) and [pd4mergecli Reference](https://app.gitbook.com/s/plLYBuldCPBrA6JYjseN/pd4ml-pdf-merge/pd4mergecli-reference).

### 15.4 `com.pd4ml.pdf.optimizer` -- cleanup and size reduction

Drops unreferenced objects, deduplicates byte-identical content, and collapses incremental-update history into one fresh, compact file. See the [PD4ML PDF Optimizer Manual](https://app.gitbook.com/s/plLYBuldCPBrA6JYjseN/pd4ml-pdf-optimizer/pd4ml-optimize-manual) and [pd4optimizecli Reference](https://app.gitbook.com/s/plLYBuldCPBrA6JYjseN/pd4ml-pdf-optimizer/pd4optimizecli-reference).

### 15.5 `com.pd4ml.pdf.xfdf` -- annotation & form-value exchange

Exports a PDF's annotations and AcroForm field values to XFDF (ISO 19444-1), or imports an XFDF file's back in -- the same format most PDF viewers use for "export/import comments", including tagging newly-added annotations into an already-accessible target. See the [PD4ML XFDF Manual](https://app.gitbook.com/s/plLYBuldCPBrA6JYjseN/pd4ml-pdf-xfdf/pd4ml-xfdf-manual) and [pd4xfdfcli Reference](https://app.gitbook.com/s/plLYBuldCPBrA6JYjseN/pd4ml-pdf-xfdf/pd4xfdfcli-reference).

## 16. See also

* [PD4ML COS API Manual](https://app.gitbook.com/s/plLYBuldCPBrA6JYjseN/pd4ml-pdf-cos/pd4ml-cos-api-manual) / [pd4coscli Reference](https://app.gitbook.com/s/plLYBuldCPBrA6JYjseN/pd4ml-pdf-cos/pd4coscli-reference)
* [PD4ML Signing Manual](https://app.gitbook.com/s/plLYBuldCPBrA6JYjseN/pd4ml-pdf-signing/pd4ml-signing-manual) / [pdfsigncli Reference](https://app.gitbook.com/s/plLYBuldCPBrA6JYjseN/pd4ml-pdf-signing/pd4signcli-reference)
* [PD4ML PDF Merge Manual](https://app.gitbook.com/s/plLYBuldCPBrA6JYjseN/pd4ml-pdf-merge/pd4ml-merge-manual) / [pd4mergecli Reference](https://app.gitbook.com/s/plLYBuldCPBrA6JYjseN/pd4ml-pdf-merge/pd4mergecli-reference)
* [PD4ML PDF Optimizer Manual](https://app.gitbook.com/s/plLYBuldCPBrA6JYjseN/pd4ml-pdf-optimizer/pd4ml-optimize-manual) / [pd4optimizecli Reference](https://app.gitbook.com/s/plLYBuldCPBrA6JYjseN/pd4ml-pdf-optimizer/pd4optimizecli-reference)
* [PD4ML XFDF Manual](https://app.gitbook.com/s/plLYBuldCPBrA6JYjseN/pd4ml-pdf-xfdf/pd4ml-xfdf-manual) / [pd4xfdfcli Reference](https://app.gitbook.com/s/plLYBuldCPBrA6JYjseN/pd4ml-pdf-xfdf/pd4xfdfcli-reference)
* [ZUGFeRD and Factur-X PDF Invoices](https://app.gitbook.com/s/cjbNvevStmXi1uYduM7U/) -- the full e-invoicing walkthrough referenced in §11.2
* [pd4ml.com](https://pd4ml.com) -- product site, licensing, and online Javadoc
