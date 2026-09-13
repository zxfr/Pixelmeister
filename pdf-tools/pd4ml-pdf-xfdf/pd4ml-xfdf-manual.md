# PD4ML XFDF API - Programmer's Manual

_Available starting from PD4ML v4.1.1_

## 1. Architecture overview

`com.pd4ml.pdf.xfdf` exports a PDF's markup annotations and AcroForm field values to an XFDF document (ISO 19444-1), and imports an XFDF document's annotations/field values back into a PDF - built entirely on top of `com.pd4ml.pdf.cos`, the same foundation `com.pd4ml.pdf.sign`, `com.pd4ml.pdf.merge` and `com.pd4ml.pdf.optimizer` are built on.

**Dependency direction is one-way**: `com.pd4ml.pdf.xfdf` depends on `com.pd4ml.pdf.cos` (and, for optional output encryption/optimization, `com.pd4ml.pdf.cos.security` and `com.pd4ml.pdf.optimizer`); `com.pd4ml.pdf.cos` never depends on it.

```
export:  COS annotation dict / AcroForm field  --AnnotationMapper/FieldMapper-->  XfdfAnnotation/XfdfField  --XfdfWriter-->  XFDF bytes
import:  XFDF bytes  --XfdfReader-->  XfdfAnnotation/XfdfField  --AnnotationMapper/FieldMapper-->  COS annotation dict / AcroForm field
```

The model classes (`XfdfDocument`, `XfdfAnnotation`, `XfdfField`) and the XML plumbing (`XfdfReader`, `XfdfWriter`) are plain, public, and reusable on their own - inspecting or hand-building an `XfdfDocument` doesn't require touching a PDF at all (see §7 of the [Examples page](pd4ml-xfdf-examples.md)). The actual COS <-> XFDF mapping logic lives in `com.pd4ml.pdf.xfdf.internal` (`AnnotationMapper`, `FieldMapper`), which the public `XfdfExporter`/ `XfdfImporter` classes drive.

### 1.1 What XFDF does and doesn't carry

XFDF is a _data_ format for annotations and form field values - not a structural PDF format. It carries an annotation's visual/content properties (position, color, contents, ...) and a field's current value, but never an annotation's or a Widget's appearance stream, and never a field's widget geometry. Two consequences follow directly, and shape this whole API:

* **Annotations can be created.** An `<annots>` entry always has enough information (`/Subtype`, `/Rect`, ...) to build a real new PDF annotation from scratch, so import can add one.
* **Fields can only be updated.** A `<field>` entry is a bare name/value pair; it can update a field's `/V` (and, for a checkbox/radio, keep its widget's `/AS` in sync), but there is nothing to build a brand-new field _or its widget_ from. Import therefore only ever updates a field already present in the target PDF's `/AcroForm` - see §5.

## 2. `com.pd4ml.pdf.xfdf` - the public API

### 2.1 Export: `XfdfExporter`

```java
byte[] xfdf = XfdfExporter.export(pdfBytes);

XfdfExporter.Options options = new XfdfExporter.Options()
        .password("open-secret")     // if the input is encrypted
        .types("highlight,tx")       // optional subset - see §6
        .ids("note-1");              // optional subset - see §6
byte[] subset = XfdfExporter.export(pdfBytes, options);
```

| Method                                                                           | Notes                            |
| -------------------------------------------------------------------------------- | -------------------------------- |
| `export(byte[])` / `export(byte[], String password)` / `export(byte[], Options)` | returns the XFDF as `byte[]`     |
| `export(File, ...)` / `export(InputStream, ...)`                                 | same, from a file or stream      |
| `export(byte[], Options, OutputStream)` / `export(File, Options, File)`          | writes straight to a destination |

Every page's `/Annots` entries whose `/Subtype` XFDF has an element for (see §3) are exported under `<annots>`; every non-signature terminal `/AcroForm` field's current value is exported under `<fields>` (see §5). A `Widget` annotation is exported only through its field's value, never as its own `<annots>` entry - matching how real XFDF producers use the format. A `Popup` is exported only nested under its owning annotation (see `XfdfAnnotation.getPopup()`), never as a separate top-level entry.

### 2.2 Import: `XfdfImporter`

```java
XfdfImporter.Options options = new XfdfImporter.Options()
        .updateIfExists(true)              // see §4
        .types("square,circle")            // optional subset - see §6
        .optimizeOutput(true)              // see §6.2
        .encryptOutput("open", "owner");   // see §6.2
XfdfImporter.Result result = XfdfImporter.importInto(pdfBytes, xfdfBytes, options);

byte[] importedPdf = result.getPdf();
System.out.println(result.getAnnotationsAdded() + " added, "
        + result.getAnnotationsUpdated() + " updated, "
        + result.getFieldsUpdated() + " field(s) updated");
result.getFieldsNotFound().forEach(System.out::println);
```

| Method                                                                                        | Notes                                             |
| --------------------------------------------------------------------------------------------- | ------------------------------------------------- |
| `importInto(byte[], byte[])` / `importInto(byte[], byte[], Options)`                          | returns a `Result`                                |
| `importInto(File, File, ...)` / `importInto(InputStream, InputStream, Options)`               | same, from files/streams                          |
| `importInto(byte[], byte[], Options, OutputStream)` / `importInto(File, File, Options, File)` | writes the imported PDF straight to a destination |

`Result` reports exactly what happened: `getAnnotationsAdded()`/ `getAnnotationsUpdated()` (a nested `<popup>` counts as its own annotation), `getFieldsUpdated()`, and `getFieldsNotFound()` - the fully-qualified names from `<fields>` that matched no existing AcroForm field, surfaced rather than silently dropped.

Both `XfdfExporter` and `XfdfImporter` are stateless (every method is `static`) - call them freely and concurrently from any number of threads.

## 3. Annotation type coverage

Every markup annotation type XFDF has an element for (ISO 19444-1 §6.4) round-trips: `Text`, `Link`, `FreeText`, `Line`, `Square`, `Circle`, `Polygon`, `PolyLine`, `Highlight`, `Underline`, `Squiggly`, `StrikeOut`, `Stamp`, `Caret`, `Ink`, `Popup`, `FileAttachment`, `Sound`, and `Redact`. `Widget` is deliberately excluded - see §1.1/§5.

Common attributes handled for every type: `name` (`/NM`), `title` (`/T`), `subject` (`/Subj`), `creationdate`/`date` (`/CreationDate`/`/M`), `color`/ `interior-color` (`/C`/`/IC`, hex, with CMYK converted to its nearest RGB for display since XFDF has no CMYK colour syntax), `opacity` (`/CA`), `flags` (`/F`, as a comma-separated name list), border `width`/`style` (`/BS`, plus cloudy borders via `/BE`), `<contents>`/`<contents-richtext>` (`/Contents`/`/RC`), `inreplyto`/`replyType` (`/IRT`/`/RT`), `open`/`icon` (`/Open`/`/Name`), and a nested `<popup>`.

Type-specific handling: FreeText's `justification`/`rotation`/`callout`/ `fringe`/`<defaultappearance>`; Line's `start`/`end`/`head`/`tail`/ `caption`; Square/Circle's `fringe`; Polygon/PolyLine's `<vertices>`; Highlight/Underline/Squiggly/StrikeOut/Redact/Link's `<coords>` (quad points); Ink's `<inklist>`; Redact's `overlay-text`/`overlay-text-repeat`; FileAttachment's `file`/`mimetype`/dates/`size` (metadata only - see the callout below).

What deliberately doesn't round-trip A Link's `/A` navigation action (its `<OnActivation>` target); a FreeText/Line's leader-line geometry (`/LL`/`/LLE`/`/LLO`); a Stamp's custom appearance bitmap (`<imagedata>`); and a FileAttachment/Sound's actual embedded bytes (only the file-specification metadata round-trips). None of these omissions corrupt anything they touch - the annotation itself still imports/exports correctly, just without that one extra piece of unusual data.

## 4. "Update if exists": `Options.updateIfExists(boolean)`

Every `<annots>` entry is matched to an existing PDF annotation by its `name` attribute (the PDF `/NM`), **independently of this switch**:

* **`true`** (default `false`) - an existing match is updated in place (its position, appearance-relevant properties, and content are overwritten from the XFDF; anything the XFDF doesn't mention is left as-is). No match means a new annotation is added, exactly as with the switch off.
* **`false`** - every `<annots>` entry becomes a new annotation, even if its `name` collides with one already present - a plain, literal "add everything" import, matching how a viewer's basic "Import Annotations" command usually behaves.

A reply-to reference (`inreplyto`) is resolved against the combined set of annotations already in the PDF and everything else in the same XFDF batch, in a second pass once every annotation in the batch has been created or matched - so it works regardless of which order the XFDF lists a thread's replies in.

Appearance streams aren't touched Import never regenerates `/AP`. An updated annotation's new position/color/contents are correct in the COS structure immediately, but won't visibly re-render until the next time a viewer regenerates appearances (most PDF viewers do this automatically on open for an annotation whose properties changed without a matching appearance update). The same applies to a text/choice field's new value.

## 5. Form fields: `<fields>`

`FieldMapper` walks `/Root/AcroForm/Fields` recursively, resolving each terminal field's fully-qualified (dot-joined) name. Export: `Tx` (text) -> `/V` string; `Ch` (choice) -> one `<value>` (single-select) or several (multi-select, from a `/V` array); `Btn` (checkbox/radio) -> `/V` as a name, or nothing at all for a pushbutton (`Ff` bit 17). `Sig` (signature) fields are **never** exported or imported - a signature's value is a byte-range/certificate structure, not something XFDF can represent, and touching one risks invalidating it.

Import only ever **updates a field that already exists** (see §1.1); a name with no match is reported via `Result.getFieldsNotFound()`, never used to fabricate a new field. For a checkbox/radio, updating `/V` also keeps every one of that field's widgets' `/AS` in sync: each widget's `/AS` is set to the new value if that widget actually has an appearance for it (i.e. the value is one of the keys under its own `/AP/N`), or `"Off"` otherwise - exactly how a radio group ends up showing only the one widget matching the field's current value as checked.

XFDF allows a field's name to be spelled out flat (one fully-qualified `<field name="a.b.c">`, what `XfdfWriter` always produces) or nested (`<field name="a"><field name="b">...`, mirroring the AcroForm hierarchy); `XfdfReader` accepts either.

## 6. Filtering and post-processing

### 6.1 `Options.types(String)` / `Options.ids(String)`

Both are comma-separated lists, available on export and import alike, and combine as an intersection when both are given.

| Option  | Matches                                                                                                                   | Case        |
| ------- | ------------------------------------------------------------------------------------------------------------------------- | ----------- |
| `types` | an annotation's XFDF element name (e.g. `"square"`, `"freetext"`) _or_ a field's PDF `/FT` code (`"Tx"`, `"Btn"`, `"Ch"`) | insensitive |
| `ids`   | an annotation's own `/NM` _or_ a field's fully-qualified name                                                             | exact       |

The two `types` vocabularies never overlap, so one list naturally selects from both categories at once: `"square,tx"` keeps only Square annotations and text fields. On import, an entry excluded by either filter is simply skipped - for a field, that is **not** the same as a not-found name (see `Result.getFieldsNotFound()`): the field exists, it just wasn't selected for this particular import.

### 6.2 `Options.optimizeOutput(boolean)` / `Options.encryptOutput(...)` (import only)

```java
new XfdfImporter.Options()
        .optimizeOutput(true)                       // com.pd4ml.pdf.optimizer.PdfOptimizer, default settings
        .encryptOutput("openPassword", "ownerPassword");   // AES-256 (default algorithm)
```

`optimizeOutput(true)` runs `com.pd4ml.pdf.optimizer.PdfOptimizer` (reachability cleanup plus duplicate-content merging) on the imported PDF before returning it. Combined with `encryptOutput`, optimization always runs first and encryption is applied to _its_ result - delegated straight into `PdfOptimizer.Options.encryptOutput(...)`, so the optimizer's own reachability walk never has to deal with ciphertext. `encryptOutput` also accepts a full `PdfEncryptor.Options` for algorithm choice and permission bits, exactly like `com.pd4ml.pdf.merge`/`com.pd4ml.pdf.optimizer`.

## 7. Encrypted input

`COSParser` decrypts transparently while parsing, so annotation/field extraction on an encrypted source works exactly like on a plain PDF - pass the password to `export(..., password)`/`Options.password(...)` (either the user or owner password; an empty/omitted password is tried too, covering the common case of a document with no _open_ password but a restricted owner password). A wrong/missing password fails clearly with `XfdfException` rather than silently returning nothing.

## 8. Error handling

Every checked failure from `com.pd4ml.pdf.xfdf`'s public API surfaces as one type: `XfdfException` (`getCause()` carries the real underlying exception, if any).

| Message (paraphrased)                                                  | Cause                                                              |
| ---------------------------------------------------------------------- | ------------------------------------------------------------------ |
| "is encrypted and the given password (or no password) did not open it" | wrong/missing password for an encrypted PDF                        |
| "has no usable Catalog"                                                | malformed/non-PDF input                                            |
| "Not an XFDF document (root element is not \<xfdf>)"                   | well-formed XML, wrong root element                                |
| "Malformed XFDF XML"                                                   | not well-formed XML at all                                         |
| "Failed to parse input PDF"                                            | wraps a `COSParseException`                                        |
| "Failed to optimize imported PDF"                                      | wraps a `PdfOptimizeException` from `Options.optimizeOutput(true)` |
| "Failed to encrypt imported PDF"                                       | wraps a `COSSecurityException` from `Options.encryptOutput(...)`   |
| "Failed to write imported PDF"                                         | wraps a `COSException` from `COSDocumentWriter`                    |

## 9. Tagged (accessibility-structured) target PDFs

Importing into a PDF that already has a `/StructTreeRoot` extends it rather than leaving newly-added annotations invisible to assistive technology. This is deliberately narrow in scope:

* **A target with no existing `/StructTreeRoot` is left untagged**, exactly as before this feature existed. `com.pd4ml.pdf.xfdf` never fabricates a whole tag hierarchy for a document that was never tagged to begin with - that's a much bigger, different problem than keeping an existing tree consistent.
* **Field updates never touch the structure tree either way** (see §5): a `<fields>` entry only ever changes an existing widget's value in place, never adding a new one, so a widget that was already tagged stays exactly as tagged as it was before, with no code needed to preserve that.
* **An updated annotation** (`Options.updateIfExists(true)`, matched by `/NM` - see §4) keeps whatever tagging it already had. Only a genuinely **new** annotation gets wired in.

For each new `<annots>` entry, per the Matterhorn Protocol (the PDF/UA-1 test suite, checkpoint 13):

* A `Popup`, or any annotation flagged `Hidden`/`NoView` (ISO 32000-1 Table 165 - never actually rendered either way), is left untagged. Everything else gets a fresh `StructElem`: `Link` uses the standard `Link` structure type; everything else uses the generic `Annot` type.
* `Annot` is only a standard structure type as of PDF 2.0 (ISO 32000-2 §14.8.4.4.3); on an older-version document (the common case), a `RoleMap` fallback mapping it to PDF 1.7's own `Note` type is added alongside it, so a reader that only knows the older type set still resolves it to something sensible.
* The new `StructElem` carries an `/Alt` copied from the annotation's own `/Contents`, when present, so the tag's accessible name matches what a viewer already shows for the comment.
* The annotation gets a fresh `/StructParent` key, registered into `/StructTreeRoot/ParentTree` (creating `/ParentTree` from scratch if the target didn't have one yet; appending a new `/Kids` leaf if it's already split that way rather than a single flat `/Nums`, for a very large pre-existing tree) and reflected in `/StructTreeRoot/ParentTreeNextKey`.

Where the new tag ends up in the tree Each new `StructElem` is appended directly under `/StructTreeRoot/K` rather than nested under whichever existing top-level element happens to "belong" to the same page - simple and always structurally valid, at the cost of the newly-tagged annotation reading last in the document's overall structure order regardless of which page it's actually on. A document whose accessibility check cares about exact reading order may want to reorder `/StructTreeRoot/K` afterwards; the annotation itself is still correctly and completely tagged either way.

Nothing here is retroactive: importing into an untagged PDF, then later importing again after someone else adds a `/StructTreeRoot`, only tags annotations added by that _second_ import - the first batch's annotations stay exactly as untagged as when they were added.

## 10. See also

* [PD4ML Programmer's Manual](https://app.gitbook.com/s/33dI66uC6sTAcaPjppX2/) - the core HTML/CSS-to-PDF engine `com.pd4ml.pdf.xfdf` and the rest of the post-processing toolkit sit alongside.
* [pd4xfdfcli Reference](pd4xfdfcli-reference.md) - the companion command-line documentation for `com.pd4ml.pdf.xfdf.cli.Pd4XfdfCli`
* [PD4ML XFDF Examples](pd4ml-xfdf-examples.md) - runnable `com.pd4ml.pdf.xfdf` use-case examples
* [PD4ML COS API Manual](../pd4ml-pdf-cos/pd4ml-cos-api-manual.md) - standalone `com.pd4ml.pdf.cos` programmer's manual (object model, parsing, writing, filters, encryption reading)
* [pd4coscli Reference](../pd4ml-pdf-cos/pd4coscli-reference.md) - command-line reference for `com.pd4ml.pdf.cos.cli.CosCli`
* [PD4ML PDF Merge Manual](../pd4ml-pdf-merge/pd4ml-merge-manual.md) - combining page ranges from several PDFs, a natural companion to annotation import/export
* [PD4ML PDF Optimizer Manual](../pd4ml-pdf-optimizer/pd4ml-optimize-manual.md) - `Options.optimizeOutput(boolean)` delegates here
* [PD4ML Signing Manual](../pd4ml-pdf-signing/pd4ml-signing-manual.md) - `com.pd4ml.pdf.sign`, which shares the same `com.pd4ml.pdf.cos` foundation and, via `com.pd4ml.pdf.cos.security`, the same `PdfEncryptor`/decryption code this package's output encryption uses
