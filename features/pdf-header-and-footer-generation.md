# PDF Header and Footer Generation

PD4ML has long supported three levels of header/footer complexity: a plain text-only template, rich HTML-formatted content, and inline tags with fine-grained per-page-range scoping. This page first documents that original, pre-v4 approach -- built around a `PD4PageMark` object -- exactly as it has existed for a long time, then walks through how each of the same four patterns looks under the current v4 API, which replaces `PD4PageMark` with direct, string-based calls.

## The legacy approach: `PD4PageMark`

### 1. Text-only headers/footers

The simplest form sets a page-number template plus a handful of presentation properties -- alignment, color, font size -- either as attributes on the `<pd4ml:footer>` JSP tag or as setters on a `PD4PageMark` object from the Java API:

```jsp
<pd4ml:footer
    pageNumberTemplate="page $[page] of $[total]"
    titleAlignment="left"
    pageNumberAlignment="right"
    color="#008000"
    initialPageNumber="1"
    pagesToSkip="1"
    fontSize="14"
    areaHeight="18"/>
```

```java
PD4PageMark footer = new PD4PageMark();
footer.setPageNumberTemplate("page $[page] of $[total]");
footer.setTitleAlignment(PD4PageMark.LEFT_ALIGN);
footer.setPageNumberAlignment(PD4PageMark.RIGHT_ALIGN);
footer.setColor(new Color(0x008000));
footer.setInitialPageNumber(1);
footer.setPagesToSkip(1);
footer.setFontSize(14);
footer.setAreaHeight(18);
pd4ml.setPageFooter(footer);
```

`pagesToSkip` excludes the given number of leading pages from getting a footer at all, and `initialPageNumber` lets the visible page count start somewhere other than 1 -- both useful for documents with an unnumbered cover page.

### 2. HTML-formatted headers/footers (PD4ML Pro)

For anything beyond plain text, the same tag/object pair instead takes a full HTML fragment. Setting `areaHeight` to `-1` asks PD4ML to compute the reserved height automatically from the content, rather than specifying it up front:

```jsp
<pd4ml:footer areaHeight="-1">
<font color="red"><i>page $[page] of $[total]</i></font>
</pd4ml:footer>
```

```java
PD4PageMark footer = new PD4PageMark();
footer.setHtmlTemplate("<font color=\"red\"><i>page $[page] of $[total]</i></font>");
footer.setAreaHeight(-1);
pd4ml.setPageFooter(footer);
```

### 3. Inline headers/footers with scope control (PD4ML Pro)

Rather than going through the Java API or a single JSP tag at all, `<pd4ml:page.footer>` can be declared directly in the source HTML. Without a `scope`, it applies to every page; with one, it targets a specific page or range, and a later occurrence in the document overrides an earlier one from that point on:

```html
<pd4ml:page.footer>
footer: $[page] of $[total]
</pd4ml:page.footer>
```

```html
<pd4ml:page.footer scope="1">
first page footer: $[page] of $[total]<br> <img src="img1.gif">
</pd4ml:page.footer>
<pd4ml:page.footer scope="2+">
footer: $[page] of $[total]<br>
<img src="img2.gif">
</pd4ml:page.footer>
```

The example above defines one footer for the first page and a different one from the second page onward. `scope` also understands `even`, `odd`, and `skiplast` modifiers, combinable with explicit pages and ranges in one comma-separated expression:

```
scope="2,5-10,even,skiplast"
```

### API-based per-page conditional content

For logic too dynamic to express as a static scope string, subclassing `PD4PageMark` and overriding `getHtmlTemplate(int pageNumber)` computes different markup for every page individually -- alternating left/right-aligned content by odd/even page, in this example:

```java
PD4PageMark footer = new PD4PageMark() {
    public String getHtmlTemplate(int pageNumber) {
        if (pageNumber % 2 == 0) {
            return "<html><body>some left aligned stuff...";
        } else {
            return "<html><body>some right aligned stuff...";
        }
    }
};
pd4ml.setPageFooter(footer);
```

`PD4PageMark` exposes the same per-page override pattern for other properties too, when a single static value across the whole scope isn't enough:

```java
PD4PageMark.getPageNumberTemplate(int pageNr);
PD4PageMark.getPageNumberAlignment(int pageNr);
PD4PageMark.getTitleTemplate(int pageNr);
```

See also this [PDF header/footer forum discussion](http://old.pd4ml.com/support/html-pdf-faq-f1/pdf-page-headers-footers-definition-options-t41.html) from PD4ML's own support archive for further worked examples of this API.

## The current (v4) API

v4 removes the `PD4PageMark` object from this picture entirely. A header or footer is just an HTML string passed to `setPageHeader(...)`/`setPageFooter(...)`, together with a pixel height and an optional scope string -- the same scope grammar used elsewhere in v4 for page size and margins (see the Programmer's Manual's [§7, Page layout](https://app.gitbook.com/s/33dI66uC6sTAcaPjppX2/)): a single page number, a range, `"N+"` for "this page onward", and modifiers including `odd` (the Programmer's Manual's own scope-grammar examples show `odd`; whether `even` and the legacy `skiplast` modifier specifically carry over is worth confirming against the current Javadoc before depending on them). The same content can equally be declared inline via `<pd4ml:page.header>`/`<pd4ml:page.footer>`, which take effect for the current and all following pages until superseded by a later occurrence -- a positional form of scoping, rather than an explicit `scope` attribute on the tag itself.

### Text-only becomes plain HTML plus CSS

There's no dedicated "text-only" mode in v4 -- the individual `PD4PageMark` properties (`titleAlignment`, `pageNumberAlignment`, `color`, `fontSize`) are simply ordinary CSS on whatever HTML string is passed in, and `pagesToSkip` becomes a matter of choosing a scope that starts later rather than a dedicated property:

```java
pd4ml.setPageFooter(
    "<div style='text-align:right; color:#008000; font-size:14pt'>page $[page] of $[total]</div>",
    18, "2+");   // "2+" skips the footer on page 1, in place of pagesToSkip="1"
```

`initialPageNumber` -- relabeling page 1 as some other starting number -- has no documented v4 equivalent as of this writing; a deployment relying on it should confirm current support before migrating.

### HTML-formatted footer

The `areaHeight="-1"` auto-sizing behavior doesn't carry over -- a v4 footer's height is always given explicitly, as the second argument:

```java
pd4ml.setPageFooter("<font color='red'><i>page $[page] of $[total]</i></font>", 24);
```

### Inline, scoped headers/footers

```html
<pd4ml:page.footer height="18">footer: $[page] of $[total]</pd4ml:page.footer>
```

```html
<pd4ml:page.footer height="18">
first page footer: $[page] of $[total]<br><img src="img1.gif">
</pd4ml:page.footer>
<pd4ml:page.break>
<pd4ml:page.footer height="18">
footer: $[page] of $[total]<br><img src="img2.gif">
</pd4ml:page.footer>
```

Or, from the API, with an explicit scope string instead of relying on document position:

```java
pd4ml.setPageFooter("first page footer: $[page] of $[total]", 18, "1");
pd4ml.setPageFooter("footer: $[page] of $[total]", 18, "2+");
```

### Per-page conditional content, without a callback

Where the legacy API needed an anonymous `PD4PageMark` subclass overriding `getHtmlTemplate(int pageNumber)` to alternate content by page, v4's plain-string API reaches the same result with two scoped calls instead of a callback -- odd/even is exactly what the scope grammar's modifiers are for:

```java
pd4ml.setPageFooter("<div style='text-align:left'>some left-aligned stuff...</div>", 20, "even");
pd4ml.setPageFooter("<div style='text-align:right'>some right-aligned stuff...</div>", 20, "odd");
```

## See also

* [PD4ML Programmer's Manual](https://app.gitbook.com/s/33dI66uC6sTAcaPjppX2/) -- §8 covers `setPageHeader`/`setPageFooter`, the built-in `$[page]`/`$[total]`/`$[title]` placeholders, and `setDynamicData(...)` for custom placeholders beyond those three.
* [PD4ML v3 to v4 Migration Guide](https://app.gitbook.com/s/33dI66uC6sTAcaPjppX2/pd4ml-v3-to-v4-migration-guide) -- §4 walks through the same `PD4PageMark`-to-string-API change side by side.
