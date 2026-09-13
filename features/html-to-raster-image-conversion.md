# HTML to Raster Image Conversion

PD4ML's two-phase conversion model -- read the source once, write it out one or more times -- makes rasterized image output a natural sibling of PDF, RTF, and [DOCX](rtf-and-docx-output.md) generation rather than a separate feature bolted on beside them. The same parsed HTML layout that would otherwise become a PDF stream can instead be rasterized page by page and handed back as PNG or TIFF, which is what makes it a convenient way to produce thumbnails, print previews, or archival page images from exactly the same source template and layout engine used everywhere else.

## Choosing an output format

The legacy API selects a raster format the same way it selects RTF or DOCX: set the desired `PD4Constants` format, then trigger a single combined `render()` call. The current API instead exposes rasterization as one of several possible destinations for an already-parsed document, via dedicated `renderAsImages()` overloads:

{% tabs %}
{% tab title="v4" %}
```java
pd4ml.readHTML(inputStream); // parse once

BufferedImage[] pages = pd4ml.renderAsImages();           // one image per page, in memory
byte[][] pngBytes     = pd4ml.renderAsImages("png");      // ... or already PNG-encoded
File dir              = pd4ml.renderAsImages(outDir, "page", "png");  // ... or written straight to disk
```
{% endtab %}

{% tab title="v3" %}
```java
pd4ml.outputFormat(PD4Constants.PNG8);
// or
pd4ml.outputFormat(PD4Constants.PNG24);
// or
pd4ml.outputFormat(PD4Constants.TIFF);

pd4ml.render(inputStream, outputStream); // parse and render in one call, using whichever format was set above
```
{% endtab %}
{% endtabs %}

(The current-API overloads are covered in full, alongside the rest of the two-phase read/write model, in the Programmer's Manual's [§6.3, Writing the output](https://app.gitbook.com/s/33dI66uC6sTAcaPjppX2/).) `PNG8` produces an 8-bit indexed-color PNG, trading color depth for a smaller file; `PNG24` keeps full 24-bit color; `TIFF` produces a single, potentially multi-page TIFF file rather than one file per page.

### JSP taglib

```jsp
<pd4ml:transform ... outputFormat="png8"> ... </pd4ml:transform>

<pd4ml:transform ... outputFormat="png24"> ... </pd4ml:transform>

<pd4ml:transform ... outputFormat="tiff"> ... </pd4ml:transform>
```

The taglib sets the corresponding HTTP `Content-Type` header automatically, matching whichever image format was requested.

### Command line

```
java -Xmx512m -Djava.awt.headless=true -cp ./pd4ml.jar Pd4Cmd <URL> 1200 -out thumbnail.png -outformat png8

java -Xmx512m -Djava.awt.headless=true -cp ./pd4ml.jar Pd4Cmd <URL> 1200 -out thumbnail.png -outformat png24

java -Xmx512m -Djava.awt.headless=true -cp ./pd4ml.jar Pd4Cmd <URL> 1200 -out thumbnail.tiff -outformat tiff
```

## What doesn't carry over to raster output

A raster image is a fundamentally simpler artifact than a PDF, and several PDF-oriented features have no equivalent once the target is a bitmap rather than a structured document:

* **Page breaks are handled differently per format.** PNG output ignores page breaks entirely and renders the whole laid-out document as one (potentially very tall) image; TIFF respects them, producing a proper multi-page TIFF with one page per image frame.
* **No headers or footers.** There's no repeating per-page decoration to speak of when PNG doesn't paginate in the first place, and TIFF's per-page images don't carry them either.
* **No footnotes.**
* **No hyperlinks** -- unsurprising, since a raster image has no notion of a clickable region.
* **A generated table of contents loses its page numbers.** The `<pd4ml:toc>` tag still works, but without discrete, numbered PDF pages to point at, its entries can't carry page references the way they do in PDF output.
* **Page insets (margins) aren't applied** -- though the source document's own CSS body margins still are, since those are just ordinary layout, not a PDF-specific page property.

## Memory footprint

Raster output is considerably more memory-hungry than PDF generation, because the entire page has to exist as decoded pixel data at once rather than as compact, structured PDF drawing operators. As a rule of thumb, even a modest 1000x5000px layout needs at least 20 MB just for the raw image bytes, before accounting for `BufferedImage`'s own object overhead -- worth budgeting for explicitly ahead of any batch job that rasterizes many pages or documents concurrently, rather than discovering the ceiling as an `OutOfMemoryError` in production.

## See also

* [PD4ML Programmer's Manual](https://app.gitbook.com/s/33dI66uC6sTAcaPjppX2/) -- §6.3 covers `renderAsImages()` alongside `writePDF`/`writeRTF`/`writeDOCX` in the current API's two-phase model.
* [RTF and DOCX Output](rtf-and-docx-output.md) -- the other two non-PDF output formats PD4ML supports from the same conversion pipeline.
* [PD4ML v3 to v4 Migration Guide](https://app.gitbook.com/s/33dI66uC6sTAcaPjppX2/pd4ml-v3-to-v4-migration-guide) -- covers the broader `outputFormat(String)`-to-`write*()`/`renderAsImages()` API change referenced above.
