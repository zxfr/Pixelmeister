# PD4ML Proprietary CSS Media Types, Properties, and Tags

Beyond the standard CSS it supports, PD4ML defines a handful of its own media types, CSS properties, and HTML tags for capabilities that have no standard CSS/HTML equivalent -- excluding specific elements from the PDF output, controlling PDF bookmarks and hyperlink destinations, and building headers, footers, watermarks, footnotes, endnotes, attachments, and a table of contents. Each of these is a deliberate PD4ML extension rather than an oversight in CSS/HTML support: there is simply no standard way to express "only in the PDF, never on screen" or "attach this file to the PDF" in either specification, so PD4ML introduces its own vocabulary for exactly those gaps. This page collects all of them in one place, starting from the proprietary `pdf` media type, moving through the properties and tags the original article already covered, and then continuing further into properties and tags that article never mentioned at all.

{% hint style="info" %}
This page was checked against the current PD4ML source, not just carried over from the original article. Two things changed since it was first written:

1. **`setDynamicParams(Map)` is v3-only.** The v4 equivalent of toggling the `print`/`screen` media types is the single-key/value `setParam(String, String)` method -- shown as the "v4" tab below, alongside the original v3 form.
2. **"CSS media queries are not supported" is no longer accurate.** The current CSS engine (`com.pd4ml.css.MediaQuery`) implements real `min-width`/`max-width` feature queries, evaluated against the document's `htmlWidth`. See [CSS media queries](pd4ml-proprietary-css-media-types-properties-and-tags.md#css-media-queries) below.
{% endhint %}

## Controlling the `print` and `screen` media types

By default, PD4ML applies CSS rules written for the `screen` media type (or with no media type specified at all, which is equivalent to `all`) -- rules scoped to `print` are ignored unless explicitly enabled.

{% tabs %}
{% tab title="v4" %}
```java
// enables `print` in addition to `screen`
pd4ml.setParam(Constants.PD4ML_MEDIA_TYPE_PRINT, "add");
```

```java
// disables `screen` and enables `print` instead
pd4ml.setParam(Constants.PD4ML_MEDIA_TYPE_PRINT, "override");
```
{% endtab %}

{% tab title="v3" %}
```java
Map m = new HashMap();
m.put(PD4Constants.PD4ML_MEDIA_TYPE_PRINT, "add");
pd4ml.setDynamicParams(m);
```

```java
Map m = new HashMap();
m.put(PD4Constants.PD4ML_MEDIA_TYPE_PRINT, "override");
pd4ml.setDynamicParams(m);
```
{% endtab %}
{% endtabs %}

## The proprietary `pdf` media type

For styling that should apply to a PDF conversion only -- never to an ordinary browser rendering of the same source document -- PD4ML recognizes a proprietary `pdf` media type. Because it's unknown to regular browsers, rules scoped to it are simply ignored outside PD4ML, with no feature-detection needed on the author's side:

```css
@media pdf {
  TR, IMG {page-break-inside: avoid;}
}
```

Unlike `print`, which must be explicitly enabled as shown above, `pdf` is always active and needs no corresponding `setParam()` call.

## CSS media queries

The original version of this article stated flatly that CSS media queries aren't supported. That's no longer the case: the current CSS engine evaluates `min-width`/`max-width` feature queries against the document's `htmlWidth` setting, so a rule like this now takes effect:

```css
@media (min-width: 700px) {
  .sidebar { display: none; }
}
```

This can be combined with the proprietary `pdf` type and the `print`/`screen` types shown above (e.g. `@media pdf and (min-width: 700px) { ... }`), following ordinary CSS media-query syntax. Feature types beyond `min-width`/`max-width` (such as `orientation` or `resolution`) are not implemented -- only those two are evaluated.

## Proprietary CSS properties

### `pd4ml-visibility` and `pd4ml-display`

These exclude a specific element from the PDF output, with syntax mirroring the standard `visibility` and `display` properties:

```html
<input style="pd4ml-display: none; pd4ml-visibility: hidden"
type=submit value="Get the page as PDF">
```

The reverse is just as possible -- the PD4ML-specific properties take precedence over the standard ones inside PD4ML, while an ordinary browser, which doesn't recognize them at all, simply falls back to the standard properties:

```html
<div style="display: none; visibility: hidden; pd4ml-display: block; pd4ml-visibility: visible">
The section is visible only in PDF report
</div>
```

### `pd4toc`

Applied to `<H1>`-`<H6>` heading tags, this suppresses page-number entries for matching headings in a table of contents built with `<pd4ml:toc>` (see [PD4ML proprietary tags](pd4ml-proprietary-css-media-types-properties-and-tags.md#pd4ml-proprietary-tags) below). It can target every heading of a given level:

```css
H3 {pd4toc: nopagenum}
```

or be applied to individual tags one at a time.

### `pd4ml-bookmark-visibility`

Excludes specific items from the generated PDF bookmarks structure:

```css
H3 { pd4ml-bookmark-visibility: hidden }
```

or, for a single heading, `<h3 style="pd4ml-bookmark-visibility: hidden">`.

### `pd4ml-new-page-table-header-copy`

By default, a table's `<thead>` section (or a leading `<tr>` of `<th>`-only cells, which implies one) is replicated at the top of the table's continuation on every page the table spans. That's not always desirable, and can be turned off:

```css
TABLE { pd4ml-new-page-table-header-copy: never }
```

Currently confirmed to affect PDF, RTF, and DOCX table output alike, not just PDF.

## Other proprietary CSS properties

A few more PD4ML-specific properties exist in the current CSS engine beyond the ones the original article covered:

| Property                                       | Purpose                                                                                                                                                                      |
| ---------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `pd4ml-toc-visibility`                         | Like `pd4ml-bookmark-visibility`, but controls whether a heading gets an entry in a `<pd4ml:toc>`-generated table of contents, independently of its PDF bookmark.            |
| `pd4ml-anchor-href`                            | Turns any element -- not just `<a>` -- into a hyperlink source, for cases where wrapping the element in an actual `<a>` tag isn't practical.                                 |
| `pd4ml-anchor-name` / `pd4ml-destination-name` | The CSS-property equivalent of an `<a name="...">` anchor: makes an arbitrary element a named PDF destination that a hyperlink (standard or `pd4ml-anchor-href`) can target. |
| `pd4ml-anchor-title`                           | Sets a tooltip title on a `pd4ml-anchor-href` link, the CSS-property equivalent of `<a title="...">`.                                                                        |

{% hint style="warning" %}
The current source also declares a family of `pd4ml-page-break-border-{top,bottom}[-color\|-style\|-width]` shorthand/longhand properties (intended, by their naming, to let a border be drawn specifically at a forced page break -- for instance, on a table row split across pages -- rather than an element's ordinary border). They parse without error, but no renderer in the current codebase (`BlockRenderer`, `TableLayout`, `RowRenderer`, or the paged-image/RTF/DOCX equivalents) was found reading them back out during this check. Treat them as declared but currently unconfirmed/likely non-functional rather than a documented, supported feature, and verify empirically before relying on them.
{% endhint %}

## PD4ML proprietary tags

Beyond CSS, PD4ML recognizes a family of its own `pd4ml:`-namespaced HTML tags for paged-media features that have no HTML equivalent at all -- page decoration, notes, bookmarks, a table of contents, embedded attachments, and content inclusion. Every one of these tags also accepts a hyphenated spelling as a synonym for environments where a colon in a tag name is inconvenient to author or to pass through an existing templating pipeline: `<pd4ml:page.break>` and `<pd4ml-page-break>` parse identically, and the same pairing holds for every tag below -- including, as covered further down, a couple of tags that only ever appear in that hyphenated form, as CSS selectors rather than HTML markup. Each subsection gives a deliberately minimal, self-contained example -- just enough markup to see the tag do something -- rather than the full attribute set, which for the two tags with dedicated pages is covered there in depth.

### `<pd4ml:page.break>`

Forces a page break at its position in the document. Used bare, with no attributes, it does exactly what its name says and nothing more:

```html
<p>Page one content.</p>
<pd4ml:page.break/>
<p>Page two content.</p>
```

A much larger set of attributes -- a new page size or orientation, a mid-document `htmlWidth` change, restarting the page counter, and more -- builds on this same minimal form. See [Dynamic Page Format Changes Within a Document](dynamic-page-format-changes-within-a-document.md) for the full reference.

### `<pd4ml:page.header>` and `<pd4ml:page.footer>`

Defines content repeated on every page it applies to, at the top or bottom of the page respectively. With no `scope` attribute given, it applies to every page from its own position in the document onward:

```html
<pd4ml:page.header>Company Confidential</pd4ml:page.header>
<pd4ml:page.footer>Page $[page] of $[total]</pd4ml:page.footer>
```

`$[page]` and `$[total]` are placeholder variables PD4ML substitutes with the current and final page numbers. Scoping to a page range, explicit sizing, and more are covered in full in [PDF Header and Footer Generation](pdf-header-and-footer-generation.md).

### `<pd4ml:page.background>`

The same per-page placement model as the header/footer tags above, but painted behind the body content across the whole page rather than confined to a header/footer band -- typically an image, for a letterhead or a pre-printed form background:

```html
<pd4ml:page.background><img src="letterhead.png"></pd4ml:page.background>
```

### `<pd4ml:watermark>`

Paints content on top of the page, over the body content, rather than behind it. Unlike header/footer/background, several watermarks can apply to the same page at once, and each is matched purely by its `scope` attribute rather than by its position in the document. Position, rotation, scale, opacity, and the `screen`/`print` media it applies to are all carried in the `style` attribute rather than as separate HTML attributes, following PD4ML's own long-standing convention for this tag:

```html
<pd4ml:watermark style="opacity:30%; angle:45; left:150; top:400">DRAFT</pd4ml:watermark>
```

### `<pd4ml:footnote>`

A footnote is authored as a single tag, written inline at the exact point in the text it annotates; its content becomes the footnote's body, pinned to the bottom of whichever page that position ends up on once pagination is resolved:

```html
<p>Statement needing a citation<pd4ml:footnote>Source: internal audit, 2026.</pd4ml:footnote>.</p>
```

Numbering is automatic unless an explicit `nr` attribute overrides it, and `noref` renders the note without a visible in-text marker. Behind the scenes, PD4ML expands every bare `<pd4ml:footnote>` into two further tags: a `<pd4ml:footnote.ref>` superscript marker, inserted exactly where the footnote tag itself was written, and a `<pd4ml:footnote.dest>` label, prepended to the note body as its numbered heading. Both are auto-generated -- like the `.dest`/`.ref` pairs elsewhere on this page, they are never written directly in a document, only produced by PD4ML itself while expanding the tag above. They remain valid CSS selector targets, though, most conveniently through their hyphenated alias:

```css
pd4ml-footnote-ref { color: #900; }
```

An explicit `<pd4ml:footnote.caption>`, by contrast, is meant to be authored: nest it inside the `<pd4ml:footnote>` tag to give that note a caption of its own.

### `<pd4ml:endnote>`

Structurally identical to `<pd4ml:footnote>` above -- the same `nr`/`noref` handling, the same auto-generated, CSS-only `.ref`/`.dest` pair (styled via `pd4ml-endnote-ref`/`pd4ml-endnote-dest`), the same author-nestable `.caption` -- with one difference: the collected notes end up gathered at the very end of the document, rather than at the bottom of whichever page each one was referenced on.

```html
<p>Statement needing a citation<pd4ml:endnote>Source: internal audit, 2026.</pd4ml:endnote>.</p>
```

### `<pd4ml:bookmark>`

Adds an explicit entry to the PDF's bookmarks panel, independent of any bookmark PD4ML might otherwise generate from a heading tag at the same position:

```html
<pd4ml:bookmark name="Appendix A"/>
```

### `<pd4ml:toc>`

Marks the position where a table of contents should be inserted, built from the document's own headings (and respecting `pd4toc: nopagenum` and `pd4ml-toc-visibility`, both described above):

```html
<pd4ml:toc/>
```

Its `pncorr` attribute corrects the page numbers the generated table of contents lists, useful when the numbers a reader should see don't start counting from the PDF's own first physical page.

### `<pd4ml:attachment>`

Embeds a file as a genuine PDF attachment -- something a reader can open or save from within their PDF viewer, distinct from an ordinary hyperlink to an external file:

```html
<pd4ml:attachment src="invoice.xlsx" description="Invoice source data" icon="tag"/>
```

`icon` selects the small icon most PDF viewers show at the attachment's position on the page -- `graph`, `pushpin`, `tag`, or the default, `paperclip`.

### `<pd4ml:include>`

Pulls in external or dynamically-supplied content at its position, via either a `src` URL/path or a `dynvalue`/`dynblock` pair for content supplied programmatically rather than read from a fixed location:

```html
<pd4ml:include src="footer-legal.html"/>
```

## See also

* [Dynamic Page Format Changes Within a Document](dynamic-page-format-changes-within-a-document.md) -- full attribute reference for `<pd4ml:page.break>`.
* [PDF Header and Footer Generation](pdf-header-and-footer-generation.md) -- full attribute reference and examples for `<pd4ml:page.header>`/`<pd4ml:page.footer>`.
* [PD4ML v3 to v4 Migration Guide](https://app.gitbook.com/s/33dI66uC6sTAcaPjppX2/pd4ml-v3-to-v4-migration-guide) -- background on the `setDynamicParams()` → `setParam()` change used above.
