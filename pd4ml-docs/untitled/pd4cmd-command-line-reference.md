# Pd4Cmd Command-Line Reference

`com.pd4ml.tools.Pd4Cmd` is `pd4ml.jar`'s own `Main-Class` -- run directly with `java -jar`, no classpath assembly required (see the Programmer's Manual's [§4](./) for the fuller walkthrough this page complements). It covers four distinct jobs from one entry point: converting HTML to PDF, RTF, DOCX, or a raster image; editing an existing PDF in `-tools` mode; indexing TrueType/OpenType fonts; and launching the bundled GUI previewer.

{% hint style="info" %}
This reference was checked directly against the current `com.pd4ml.tools.Pd4Cmd` source and the usage banner it prints when run with no arguments, rather than transcribed from older documentation as-is. Three flags documented on earlier versions of this page -- `-noimagesplit`, `-smarttablesplit`, and `-protectpud` -- no longer exist in the current build and have been dropped below; several flags that do exist weren't previously documented at all and have been added (`-errorpolicy`, `-ligatures`, `-fitandcenterpage`, `-overlay`/`-underlay`/`-overlaypassword`, the `-log` alias for `-debug`, and DOCX/raster/`PdfSpec`-string support in `-outformat`). Always prefer running `java -jar pd4ml.jar` with no arguments over any static reference, including this one, for the definitive, always-current flag list for the exact build in use.
{% endhint %}

## Minimum parameters

```
java -Djava.awt.headless=true -Xmx512m -jar pd4ml.jar "https://pd4ml.com" 1200
```

The two required positional arguments are the source (a `file:`, `http:`, or `https:` URL) and `htmlWidth` in pixels -- the virtual browser viewport width the source is laid out against. A third, optional positional argument names the target page format (a predefined name, or explicit `WIDTHxHEIGHT` in points); omitted, it defaults to A4. With no `-out`, the result streams to stdout rather than a file.

## A more complete conversion

```
java -Djava.awt.headless=true -Xmx512m -jar pd4ml.jar "https://pd4ml.com" 1200 LETTER -bookmarks HEADINGS -pdfforms -debug 3 -out pd4ml.pdf
```

One correction versus older documentation of this same example: `-debug` now requires a numeric verbosity level (`0`-`5`) as its own argument -- a bare `-debug` with nothing after it is rejected outright (`invalid parameter: log level is missing`), rather than behaving as an on/off switch.

## PDF tools mode

Passing `-tools` switches Pd4Cmd from HTML conversion into editing an existing PDF, given as the source in place of an HTML URL.

### Extracting a page range

```
java -Djava.awt.headless=true -Xmx512m -jar pd4ml.jar -tools file:c:/docs/test.pdf -pagerange 2-3,5+ -out c:/docs/newdoc.pdf
```

### Merging two PDFs

```
java -Djava.awt.headless=true -Xmx512m -jar pd4ml.jar -tools file:c:/docs/test.pdf -merge file:c:/docs/tomerge.pdf after -out c:/docs/newdoc.pdf
```

### Overlaying or underlaying one PDF onto another

Not previously documented on this page: `-overlay`/`-underlay` composite one PDF's pages onto another's, entirely within tools mode -- no separate write-then-reread step needed:

```
java -Djava.awt.headless=true -Xmx512m -jar pd4ml.jar -tools file:c:/docs/test.pdf -overlay file:c:/docs/stamp.pdf all all 128 -out c:/docs/newdoc.pdf
```

`srcScope` and `destScope` each accept either the keyword `all` or a page range/list such as `1-3,5`; opacity is `0`-`255`. `-underlay` takes the identical four arguments and paints beneath the base document's content instead of on top of it. `-overlaypassword` supplies the overlay/underlay source's password, alongside `-readpassword` (the primary input) and `-mergepassword` (an `-merge` input) for the other tools-mode password slots. `-merge` and `-overlay`/`-underlay` can't be combined in a single invocation; chain two separate calls, writing and re-reading the intermediate file, to apply both.

### Updating permissions

```
java -Djava.awt.headless=true -Xmx512m -jar pd4ml.jar -tools file:c:/docs/test.pdf -permissions 28 -out c:/docs/newdoc.pdf
```

### Reading document metadata

```
java -Djava.awt.headless=true -Xmx512m -jar pd4ml.jar -tools file:c:/docs/test.pdf -printpermissions -printauthor -printtitle -printpagenum
```

## Indexing TTF fonts

```
java -Xmx512m -jar pd4ml.jar -configure.fonts <fontdir> [pd4fonts.properties location]
```

See the [Configuring PDF Fonts](https://app.gitbook.com/s/cjbNvevStmXi1uYduM7U/configuring-pdf-fonts) page for the fuller picture of when to reach for this versus the other font-indexing options.

## Launching the GUI

```
java -Xmx512m -jar pd4ml.jar -gui
```

Optionally followed by a URL to open immediately, rather than starting from the previewer's blank state.

## Full parameter reference

| Flag                                                          | Purpose                                                                                                                                                                                 |
| ------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `<url>`                                                       | _(positional, required)_ Source document URL -- `file:`, `http:`, or `https:`.                                                                                                          |
| `<htmlWidth>`                                                 | _(positional, required)_ Virtual browser viewport width, in pixels.                                                                                                                     |
| `pageFormatName\|WxH`                                         | _(positional, optional)_ Target page format name, or explicit width x height in points. Default: `A4`.                                                                                  |
| `-gui`                                                        | Launches the bundled GUI previewer/converter instead of converting from the command line.                                                                                               |
| `-tools`                                                      | Switches to PDF-editing mode; the source is an existing PDF, not HTML.                                                                                                                  |
| `-configure.fonts <dir> [loc]`                                | Indexes a TTF/OTF directory, writing `pd4fonts.properties` either alongside the fonts or to the given alternate location.                                                               |
| `-xsl <notesdefault\|url>`                                    | Treats the source as XML and transforms it through the given XSL stylesheet first (`notesdefault` selects the bundled Domino/DXL stylesheet).                                           |
| `-usetmpfiles`                                                | Extracts Base64-encoded Notes/DXL attachments to temporary files rather than passing them through the XSL transform inline.                                                             |
| `-dumphtml`                                                   | Prints the post-XSL-transformation HTML to stdout, for debugging an `-xsl` conversion.                                                                                                  |
| `-errorpolicy <RELAXED\|NORMAL\|PEDANTIC\|STRICT>`            | Controls how strictly malformed source HTML or non-conformant output is treated; see the Programmer's Manual's [§12](./).                                                               |
| `-outformat <fmt>`                                            | Selects the output format: `pdf` (default), `pdfa`, `pdfua`, `docx`, `rtf`, `rtfwmf`, `png8`, `png24`, `tiff`, or a combined `PdfSpec` string such as `"PDF1.7ext8 PDF/A-3a PDF/UA-1"`. |
| `-pdfa`                                                       | Shorthand for requesting PDF/A-compliant output, equivalent to `-outformat pdfa`.                                                                                                       |
| `-bookmarks <HEADINGS\|ANCHORS>`                              | Generates a PDF bookmark tree from `<h1>`-`<h6>` headings or from named anchors.                                                                                                        |
| `-orientation <PORTRAIT\|LANDSCAPE>`                          | Page orientation; `LANDSCAPE` rotates the target format 90°. Default: `PORTRAIT`.                                                                                                       |
| `-insets <T,L,B,R,unit>`                                      | Page margins, `unit` being `mm` or `pt`. Default: `10,10,10,10,mm`.                                                                                                                     |
| `-bgcolor <#RRGGBB>`                                          | Solid background color for every page.                                                                                                                                                  |
| `-bgimage <url>`                                              | Background image for every page, scaled to the full page area.                                                                                                                          |
| `-watermark <url,x,y,w,h,opacity>`                            | Image watermark, positioned and sized in pixels with an opacity from 0-255.                                                                                                             |
| `-pdfforms`                                                   | Converts HTML form controls into interactive PDF (AcroForm) fields.                                                                                                                     |
| `-multicolumn <cols,gap[mm\|pt]>`                             | Lays the document out in multiple columns per page, e.g. `3,10mm`.                                                                                                                      |
| `-adjustwidth`                                                | Measures the source's own natural width and uses that as `htmlWidth`, instead of the given value.                                                                                       |
| `-fitapage`                                                   | Downscales the whole layout to fit a single page vertically, rather than paginating.                                                                                                    |
| `-fitandcenterpage`                                           | As `-fitapage`, additionally centering the downscaled content on the page.                                                                                                              |
| `-nohyperlinks`                                               | Suppresses conversion of HTML hyperlinks into PDF link annotations.                                                                                                                     |
| `-author <name>`                                              | Sets the PDF document-info Author field.                                                                                                                                                |
| `-title <text>`                                               | Sets or overrides the document title (also usable as a `$[title]` placeholder value).                                                                                                   |
| `-header '<html>'`                                            | Page header content as HTML; supports `$[page]`, `$[total]`, `$[title]`.                                                                                                                |
| `-footer '<html>'`                                            | Page footer content as HTML; same placeholders as `-header`.                                                                                                                            |
| `-addstyle <css>`                                             | Applies an additional stylesheet to the source document; repeatable.                                                                                                                    |
| `-cookie <name> <value>`                                      | Sends the given cookie with the source HTML's HTTP request; repeatable.                                                                                                                 |
| `-param <name> <value>`                                       | Defines a dynamic placeholder value (and a named parameter) for the conversion; repeatable.                                                                                             |
| `-encoding <name>`                                            | Overrides the detected source document character encoding.                                                                                                                              |
| `-ttf <dir>`                                                  | TrueType/OpenType font directory to embed glyphs from.                                                                                                                                  |
| `-ttfrefsonly`                                                | References fonts without embedding their glyph data in the output.                                                                                                                      |
| `-kerning`                                                    | Applies the fonts' own kerning pairs.                                                                                                                                                   |
| `-ligatures`                                                  | Builds compound glyphs (fi, fl, ...) where the active font defines them.                                                                                                                |
| `-threads <n>`                                                | Enables asynchronous, concurrent resource loading with the given thread-pool size.                                                                                                      |
| `-pagerange <scope>`                                          | Limits which pages are written to output, e.g. `2+`, `1-2`, `even`, `odd`, `3-7,odd`.                                                                                                   |
| `-permissions <number>`                                       | PDF access permissions, as a sum of individual permission bit values.                                                                                                                   |
| `-password <pw>`                                              | Protects the output document with the given (owner) password.                                                                                                                           |
| `-merge <path> <after\|before>`                               | Appends or prepends an existing PDF to the conversion result.                                                                                                                           |
| `-overlay <path> <srcScope\|all> <destScope\|all> <opacity>`  | _(tools mode)_ Composites another PDF's pages on top of the source's.                                                                                                                   |
| `-underlay <path> <srcScope\|all> <destScope\|all> <opacity>` | _(tools mode)_ As `-overlay`, painted beneath the source's content instead.                                                                                                             |
| `-readpassword <pw>`                                          | _(tools mode)_ Password for the primary input PDF.                                                                                                                                      |
| `-mergepassword <pw>`                                         | _(tools mode)_ Password for the `-merge` input PDF.                                                                                                                                     |
| `-overlaypassword <pw>`                                       | _(tools mode)_ Password for the `-overlay`/`-underlay` input PDF.                                                                                                                       |
| `-printpermissions`                                           | _(tools mode)_ Prints the input PDF's permissions (as hex) to stdout.                                                                                                                   |
| `-printauthor`                                                | _(tools mode)_ Prints the input PDF's Author field to stdout.                                                                                                                           |
| `-printtitle`                                                 | _(tools mode)_ Prints the input PDF's Title field to stdout.                                                                                                                            |
| `-printpagenum`                                               | _(tools mode, and regular conversion)_ Prints the resulting page count to stdout.                                                                                                       |
| `-debug <level>` (alias `-log <level>`)                       | Sets diagnostic verbosity, `0` (silent) through `5` (very verbose).                                                                                                                     |
| `-out <path>`                                                 | Output file path. Omitted, output streams to stdout instead.                                                                                                                            |

## See also

* [PD4ML Programmer's Manual](./) -- §4 introduces `Pd4Cmd` alongside the Java API and JSP taglib as the three ways to drive a conversion.
* [Configuring PDF Fonts](https://app.gitbook.com/s/cjbNvevStmXi1uYduM7U/configuring-pdf-fonts) -- more on `-configure.fonts`/`-ttf` and the other ways to build a font mapping.
