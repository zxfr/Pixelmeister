# PD4ML PDF Merge API - Programmer's Manual

_Available starting from PD4ML v4.1.1_

## 1. Architecture overview

`com.pd4ml.pdf.merge` combines page ranges selected independently from one or more existing PDF files into a single output document - built entirely on top of `com.pd4ml.pdf.cos`, the same foundation `com.pd4ml.pdf.sign` is built on.

**Dependency direction is one-way**: `com.pd4ml.pdf.merge` depends on `com.pd4ml.pdf.cos` (and, for optional output encryption, `com.pd4ml.pdf.cos.security`); `com.pd4ml.pdf.cos` never depends on it. Merge never touches PDF bytes directly - it deep-clones `COSDictionary`/`COSArray`/`COSStream` objects out of each parsed source document and hands the result to `com.pd4ml.pdf.cos.writer` to serialize.

### 1.1 What a merge actually does

Every selected page is deep-cloned into a **fresh object-numbering space**, so the sources' indirect object numbers never collide - regardless of how much they overlap in the originals (two independently-produced PDFs routinely both start at `1 0 obj`). Each selected page's accessibility structure (tagging) is reconciled into one merged `/StructTreeRoot` on a best-effort basis: pages from a tagged source keep their structure, pages from an untagged source are simply included without any, and the merged document is tagged overall only if at least one selected page contributed structure.

```
[ source A: pages 2-5, odd ]  [ source B: pages 1,3,6+ ]
                 \                      /
                  v                    v
        COSObjectImporter (renumbers, dedupes shared resources)
                          |
                          v
        StructureTreeMerger (splices + prunes /StructTreeRoot)
                          |
                          v
             COSDocumentWriter (one fresh xref + trailer)
```

The generic mechanics of deep-cloning a reachable object graph into a fresh document with compact numbering - allocating object numbers, preserving object identity/sharing, breaking cycles - live in `com.pd4ml.pdf.cos.util.COSObjectImporter`, which has no knowledge of merging specifically. It's a general-purpose primitive also used by `com.pd4ml.pdf.optimizer` (which seeds it from a single document's own `/Root` to drop unreferenced objects rather than from several sources' selected pages).

### 1.2 Key abstraction: `COSObjectImporter`

```java
public COSBase importValue(COSDocument sourceDoc, COSBase value);      // clone, following an indirect ref if given one
public COSObject importIndirect(COSDocument sourceDoc, COSObject ref); // clone, always returns an indirect target ref
public COSObject allocate(COSBase value);                              // register a freshly-built (non-cloned) object
```

One instance is shared across an entire merge operation (every source, every selected page, plus the structure tree), keyed internally by a `Map<COSDocument, Map<COSObjectKey, COSObject>>` per-source cache. That cache is what makes cloning **identity-preserving**: the same source object reached via two different paths (a font shared by two pages of the same source, or a `StructElem` parent reached from two different children) is cloned exactly once, and every reference to it in the merged graph points at that single clone. Cycles (a page's `/Parent` chain, a `StructElem`'s `/P` chain) are broken by registering a not-yet-populated placeholder in that cache _before_ recursing into an object's own content.

## 2. `com.pd4ml.pdf.merge` - the public API

### 2.1 Entry point: `PdfMerger`

```java
PdfMerger merger = new PdfMerger();
merger.addSource(pdfABytes).selectPages("2-5,odd");
merger.addSource(pdfBBytes, "sourceBPassword").selectPages("1,3,6+");
merger.encryptOutput("openPassword", "ownerPassword");
byte[] merged = merger.mergeToBytes();
```

| Method                                                        | Notes                                                       |
| ------------------------------------------------------------- | ----------------------------------------------------------- |
| `addSource(byte[])` / `addSource(byte[], password)`           | parses via `COSParser.load`; returns a `PdfMergeSource`     |
| `addSource(File)` / `addSource(File, password)`               | reads the file, then as above                               |
| `addSource(InputStream)` / `addSource(InputStream, password)` | reads the stream fully (not closed), then as above          |
| `encryptOutput(userPassword, ownerPassword)`                  | AES-256 output encryption, the modern default               |
| `encryptOutput(PdfEncryptor.Options)`                         | full control - algorithm choice, permission bits (see §4.2) |
| `mergeToBytes()`                                              | returns the merged PDF as `byte[]`                          |
| `merge(OutputStream)` / `merge(File)`                         | writes straight to a destination                            |

Every `addSource` overload accepts an encrypted source: pass its user _or_ owner password (an empty/omitted password is tried too, covering the common case of a document with no open password but a restricted owner password - see §4.1). A source is rejected only if the given password fails to open it.

`PdfMerger` is **not thread-safe** - build and merge one instance per merge operation.

### 2.2 Selecting pages: `PdfMergeSource`

`addSource(...)` returns a `PdfMergeSource`; call `selectPages(String)` on it directly:

```java
PdfMergeSource source = merger.addSource(pdfBytes);
source.selectPages("2-5,8,10-12");   // 1-based, comma-separated
```

`selectPages` may be called again to replace a previous selection. See §3 for the full page-range grammar (ranges, `odd`/`even`, `N+`, deduplication rules).

Single-source shortcut With exactly **one** source added, calling `selectPages(...)` is optional: with no selection, the result is that source's own pages, in their original order - decrypted, if the source was encrypted, and re-encrypted only if `encryptOutput` was also called. In effect, a filter/decrypt/re-encrypt pass rather than a true merge. With **two or more** sources, every source still requires an explicit `selectPages(...)` call - an unselected source with company is ambiguous and fails with `PdfMergeException`.

### 2.3 Output ordering

Pages appear in the merged output in **source-addition order**, and within each source in the order its own `selectPages` range string produced. Nothing prevents calling `addSource` a third time - the API is forward-compatible with more than two sources even though a typical call merges exactly two.

## 3. Page-range grammar

A `selectPages` argument is a comma-separated list of tokens, each contributing (or, for `odd`/`even`, filtering) 1-based page numbers.

| Token  | Meaning                                                                          |
| ------ | -------------------------------------------------------------------------------- |
| `N`    | a single page                                                                    |
| `N-M`  | pages `N` through `M` inclusive; `M < N` walks descending (`5-2` visits 5,4,3,2) |
| `N+`   | page `N` through the last page of that source                                    |
| `odd`  | **not** a range of its own - see §3.1                                            |
| `even` | **not** a range of its own - see §3.1                                            |

### 3.1 `odd` / `even` are filters, not ranges

Every other token in the same `selectPages` call is unioned first; `odd` (or `even`) then **suppresses** the opposite parity from that combined set, rather than contributing pages of its own:

```java
source.selectPages("1-2,4+,odd");
// 1-2 union 4+  = 1,2,4,5,6,7,8,9,10   (on a 10-page source)
// odd suppresses the even ones         -> 1,5,7,9
```

Used with **no other token** (just `"odd"` or `"even"` alone), the base to filter defaults to every page in the document, so `"odd"` alone means "every odd page" and `"even"` alone means "every even page" - the ordinary, unsurprising reading when there's nothing else to filter.

`odd` and `even` together in one `selectPages` call are rejected outright as contradictory (`PdfMergeException`), rather than silently producing an empty selection.

### 3.2 Deduplication

The combined result is deduplicated: each page appears **at most once**, at the position where it was _first_ mentioned.

```java
source.selectPages("1,1,2,2");        // -> 1,2  (not 1,1,2,2)
source.selectPages("2,5-6,odd");      // 2 union 5-6 = 2,5,6; odd suppresses 2 and 6 -> 5
```

A descending sub-range like `"5-2"` is still honored for ordering (its pages are visited 5,4,3,2) - it just can't reintroduce a page a smaller-numbered token already placed earlier.

### 3.3 Validation

Every page number is validated against that source's own page count as soon as `selectPages` is called (not deferred to `merge()`); an out-of-range page, an unparsable token, or a blank spec all throw `PdfMergeException` immediately, naming the offending token.

## 4. Encryption

### 4.1 Input

`COSParser` decrypts transparently while parsing, so every page cloned from an encrypted source is handled exactly like one from a plain PDF - no special-casing anywhere else in the merge pipeline. `addSource` rejects a source only when `COSDocument.isEncrypted()` is true **and** `getEncryptionError()` is non-null, i.e. the given (or empty) password genuinely failed to authenticate as either the user or the owner password.

### 4.2 Output

The merged output is **not** encrypted by default, even if one or more sources were. Opt in explicitly:

```java
merger.encryptOutput("openPassword", "ownerPassword");                 // AES-256 (default algorithm)

PdfEncryptor.Options options = new PdfEncryptor.Options()
        .userPassword("openPassword")
        .ownerPassword("ownerPassword")
        .algorithm(PdfEncryptor.Algorithm.AES_128)                      // RC4_128 | AES_128 | AES_256
        .permissions(-4);                                                // raw /P bits, default "everything allowed"
merger.encryptOutput(options);
```

`PdfEncryptor` (`com.pd4ml.pdf.cos.security`) is the write-side counterpart of the read-only `StandardSecurityHandler` - it generalizes the same forward key-derivation math the test suite's `EncryptedPdfSampleBuilder` demonstrates into a real encryption pass over an arbitrary built `COSDocument`, walking every indirect object's strings/streams and encrypting them in place before a fresh `/Encrypt` dictionary and `/ID` are registered. It is shared, general-purpose infrastructure - not specific to merge.

## 5. Structure-tree (tagging) reconciliation

`com.pd4ml.pdf.merge.internal.StructureTreeMerger` implements the relevant parts of the PDF structure-tree model directly (`com.pd4ml.pdf.cos` itself has no built-in awareness of `/StructTreeRoot`/`StructElem`/`ParentTree`/MCID - it's read-only plumbing).

* A page whose source isn't tagged (or that itself has no `/StructParents`) is simply included with no structure - best-effort, not all-or-nothing.
* A tagged page's `ParentTree` entry is cloned (via the same `COSObjectImporter`, so a shared ancestor `StructElem` is cloned once) and re-keyed into the merged document's own `ParentTree`; the cloned page's `/StructParents` is rewritten to that new key.
* **Cross-page pruning**: a source `StructElem` that spans both selected and excluded pages (e.g. a `<Table>` whose rows are split across two pages, only one of which was selected) is pruned back to just the children belonging to pages actually included in the merge; a `StructElem` left with no children after pruning is dropped entirely.
* `/RoleMap` entries from every tagged source are unioned; a genuine conflict (the same custom role name mapped to a different standard type across two sources) keeps whichever was seen first.
* `/IDTree` (structure-element `/ID` lookup) is merged for the common flat case (no `/Kids`); a colliding `/ID` string from a second source is prefixed (`"dup:" + id`) rather than silently overwriting the first. A source whose `IDTree` is large enough to need `/Kids` simply keeps its element IDs un-indexed in the merged tree.
* If **no** selected page from **any** source contributed structure, the merged document gets no `/StructTreeRoot` at all - a clean, ordinary PDF, not a vacuous tagged shell.

## 6. Error handling

Every checked failure from `com.pd4ml.pdf.merge`'s public API surfaces as one type: `PdfMergeException` (`getCause()` carries the real underlying exception, if any).

| Message (paraphrased)                                                  | Cause                                                            |
| ---------------------------------------------------------------------- | ---------------------------------------------------------------- |
| "No source PDFs were added"                                            | `mergeToBytes()`/`merge(...)` called before any `addSource(...)` |
| "selectPages(...) was never called for one of the merge sources"       | two or more sources, one with no selection                       |
| "Page N is out of range"                                               | a page number beyond that source's own page count                |
| "specifies both 'odd' and 'even', which is contradictory"              | see §3.1                                                         |
| "is encrypted and the given password (or no password) did not open it" | wrong/missing password for an encrypted source                   |
| "has no usable Catalog"                                                | malformed/non-PDF input                                          |
| "Failed to encrypt merged output"                                      | wraps a `COSSecurityException` from `PdfEncryptor`               |
| "Failed to write merged PDF"                                           | wraps a `COSException` from `COSDocumentWriter`                  |

## 7. Limitations (out of scope for merge)

* **`/AcroForm` field hierarchy** is not reconciled - a page's widget annotations still come through fine (they're ordinary entries in that page's own `/Annots`, cloned generically), but the document-level interactive-form field tree and calculation order are not merged.
* **Named destinations and outlines/bookmarks** are not merged.
* **`/Info`** (document metadata) is taken from the first source only, unmodified.
* `/RoleMap` conflicts and `/IDTree` size are handled with the first-wins/flat-only fallbacks described in §5, not full reconciliation.

None of these silently corrupt the output - a feature that isn't merged is simply absent from the merged document, not partially/incorrectly present.

## 8. See also

* [PD4ML Programmer's Manual](https://app.gitbook.com/s/33dI66uC6sTAcaPjppX2/) - the core HTML/CSS-to-PDF engine `com.pd4ml.pdf.merge` and the rest of the post-processing toolkit sit alongside.
* [pd4mergecli Reference](pd4mergecli-reference.md) - the companion command-line documentation for `com.pd4ml.pdf.merge.cli.Pd4MergeCli`
* [PD4ML COS API Manual](../pd4ml-pdf-cos/pd4ml-cos-api-manual.md) - standalone `com.pd4ml.pdf.cos` programmer's manual (object model, parsing, writing, filters, encryption reading)
* [pd4coscli Reference](../pd4ml-pdf-cos/pd4coscli-reference.md) - command-line reference for `com.pd4ml.pdf.cos.cli.CosCli`
* [PD4ML Signing Manual](../pd4ml-pdf-signing/pd4ml-signing-manual.md) - `com.pd4ml.pdf.sign`, which shares the same `com.pd4ml.pdf.cos` foundation and, via `com.pd4ml.pdf.cos.security`, the same `PdfEncryptor`/decryption code merge uses
* [PD4ML XFDF Manual](../pd4ml-pdf-xfdf/pd4ml-xfdf-manual.md) - `com.pd4ml.pdf.xfdf`, a natural companion once pages are merged: bring in reviewers' annotations and form field values from an XFDF file
