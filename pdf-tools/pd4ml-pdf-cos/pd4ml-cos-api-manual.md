# PD4ML COS API - Programmer's Manual

_Available starting from PD4ML v4.1.1_

This manual covers `com.pd4ml.pdf.cos` on its own: the PDF COS (Carousel Object Structure) object model, parser, COS-path query language, and incremental-update writer - independent of `com.pd4ml.pdf.sign`. For digital signing and PAdES-LT built on top of this layer, see the [PD4ML Signing Manual](../pd4ml-pdf-signing/pd4ml-signing-manual.md) (§§2-4 there summarize the same object model briefly before moving on to signing; this manual goes deeper on `com.pd4ml.pdf.cos` itself, including the general editing recipe behind `coscli set`/`delete` - see the [pd4coscli Reference](pd4coscli-reference.md)).

## 1. Overview

`com.pd4ml.pdf.cos` is a hand-rolled PDF COS object model, parser, and writer, built independently rather than on top of an existing PDF library. It has exactly **one** third-party dependency, BouncyCastle, used _solely_ inside `com.pd4ml.pdf.cos.security` for standard-security-handler decryption (RC4/AES) of an already-encrypted input document. Every other part of `com.pd4ml.pdf.cos` - the object model itself, the parser, the COS-path evaluator, the writer, the filter implementations - is dependency-free, plain Java 1.8.

`com.pd4ml.pdf.cos` never depends on `com.pd4ml.pdf.sign`; the reverse is true (`com.pd4ml.pdf.sign` is built entirely on top of it). This means `com.pd4ml.pdf.cos` is fully usable standalone for any PDF-reading or incremental-update-writing task that has nothing to do with signing: form inspection, metadata extraction, structural validation, adding annotations, or a custom incremental-update feature built the same way `com.pd4ml.pdf.sign.pdf.IncrementalPdfSigner` is.

## 2. The object model

| Type                      | Represents                                                             |
| ------------------------- | ---------------------------------------------------------------------- |
| `COSDictionary`           | a PDF dictionary (`<< /Key value ... >>`)                              |
| `COSArray`                | a PDF array (`[ ... ]`)                                                |
| `COSStream`               | a `COSDictionary` plus a byte stream (extends `COSDictionary`)         |
| `COSString`               | a PDF string, stored as raw bytes; `getBytes()`/`getString()`          |
| `COSName`                 | a PDF name (`/Foo`) - interned via `COSName.getPDFName(String)`        |
| `COSInteger` / `COSFloat` | numbers (both extend `COSNumber`)                                      |
| `COSBoolean`              | `true`/`false` - the two singletons `COSBoolean.TRUE`/`FALSE`          |
| `COSNull`                 | the `null` object (singleton `COSNull.NULL`)                           |
| `COSObject`               | an indirect reference (`N G R`) - `resolve()` follows it               |
| `COSObjectKey`            | an object number + generation pair, the identity of an indirect object |
| `COSDocument`             | the whole document: object table + trailer                             |

Every `COSBase` (the common superclass of all of the above) can carry an assigned `COSObjectKey` (`getKey()`/`setKey()`) - that's what makes it an _indirect_ object when serialized; without one, it's written inline wherever it appears as a value.

`COSDictionary` offers both indirection-transparent and indirection-aware accessors: `getItem(key)` returns the raw value (possibly a `COSObject` reference), `getDictionaryObject(key)` resolves it first. Typed convenience getters (`getString`, `getInt`, `getLong`, `getFloat`, `getBoolean`, `getDictionary`, `getArray`, `getStream`, `getNameAsString`, `getCOSName`, ...) all resolve indirection automatically and return a sensible default/`null` on a type mismatch rather than throwing - the common case (an optional key) never needs an `instanceof` check. `COSArray` mirrors this on the array side (`get(index)` raw, `getObject(index)` resolved, plus `getInt`/`getFloat`/`getName` convenience accessors and `toFloatArray()` for a `MediaBox`-style entry).

## 3. Parsing

```java
COSDocument doc = COSParser.load(pdfBytes);                    // empty password
COSDocument doc = COSParser.load(pdfBytes, "ownerPassword");   // tried as user AND owner password
```

`COSDocument.isEncrypted()` reports whether the trailer named an `/Encrypt` dictionary; `getEncryptionError()` is non-null if a password was given but didn't authenticate (strings/streams are then still in their raw encrypted form). If encryption succeeded (or the document was never encrypted), every accessor throughout the API already returns plaintext - decryption happens once, in place, as the document is parsed.

`COSDocument.getCatalog()` resolves `trailer → /Root`. `getTrailer()` returns the trailer dictionary directly. `getObjectKeys()`/`getObjects()` enumerate the whole indirect-object table (this is what `coscli list` walks); `getObject(key)`/`hasObject(key)` look up one entry directly, and `getHighestObjectNumber()` reports the largest object number currently in use (what a new object's number would need to exceed).

## 4. COS-path queries

A small, filesystem-like path language for navigating the object graph - used by `coscli inspect`/`dump` and internally by `pdfsigncli inspect`.

```java
COSBase result = COSPathEvaluator.evaluate(doc, "/Root/Pages/Kids[0]/MediaBox"); // null on any miss
COSBase result = COSPathEvaluator.evaluateStrict(doc, "...");                    // throws COSPathException instead
```

**Root** (where navigation starts):

| Form                                       | Starts at                           |
| ------------------------------------------ | ----------------------------------- |
| _(empty)_, a leading `/`, or `trailer/...` | the trailer dictionary              |
| `Root/...` or `catalog/...`                | `trailer/Root` (the catalog)        |
| `N G R/...`                                | an explicit indirect object `N G R` |

**Steps** (repeated after the root):

| Form                         | Meaning                                                        |
| ---------------------------- | -------------------------------------------------------------- |
| `Name` or `/Name`            | look up a key in the current dictionary/stream node            |
| `[N]`, or a bare integer `N` | index into the current array node                              |
| `N G R`                      | jump directly to an indirect object, ignoring the current node |

A name step may contain `\/`, `\[`, `\\` escapes and `#xx` hex escapes (the same convention PDF name objects themselves use), for a key whose literal text would otherwise be ambiguous. Indirect references are followed transparently at every step - a path never needs `.resolve()` spelled out. Examples: `/Root/Pages/Kids[0]/Contents`, `root/Pages/Kids/0/MediaBox` (bare-integer index, no brackets needed), `catalog/Outlines/First/Title`, `12 0 R/Filter`, `trailer/Info`.

`evaluate(...)` is lenient: any failure (a missing key, an out-of-range index, a step applied to the wrong kind of node) yields `COSNull.NULL` rather than throwing - convenient for exploratory lookups. `evaluateStrict(...)` instead throws `COSPathException` naming exactly which step failed and why, e.g. `"No such key 'Foo' (after /Root/Pages)"`

* what `coscli`/`pdfsigncli` use, since a silent `null` would be a confusing CLI result.

## 5. Writing

`com.pd4ml.pdf.cos.writer` is where new or changed content gets serialized.

### 5.1 `COSWriter`

A generic `ICOSVisitor<Void>` implementation: give it any `COSBase` and it writes correct PDF syntax for it, deciding reference-vs-inline per value (an already-indirect nested value is written as `"N G R"`; everything else is written inline). Building blocks: `writeIndirectObject(key, value)`, `writeValue(value)`, `writeName(...)`, `writeString(...)`, `writeHex(...)`, static `formatFloat(float)`, plus raw `writeAscii`/`writeBytes` escape hatches and `position()` for exact byte-offset tracking.

### 5.2 `IncrementalUpdateWriter`

The generic incremental-update mechanics (ISO 32000-1 §7.5.6): allocate object numbers, track byte offsets, emit a correct cross-reference table and trailer - with zero PDF-_semantic_ knowledge of what's actually being added or changed. The original file's bytes are never modified; this only _appends_.

```java
IncrementalUpdateWriter update = new IncrementalUpdateWriter(originalBytes, document);

COSDictionary newThing = new COSDictionary();
update.assignNewKey(newThing);              // gives it an indirect identity
someExistingDict.setItem("Foo", newThing);  // wire it into the graph
update.writeObject(someExistingDict);       // rewrite: it changed
update.writeObject(newThing);               // write: it's new

byte[] result = update.finish();            // writes xref + trailer, returns the whole file
```

`writeObject(obj)` requires `obj.getKey() != null` - only an already-indirect object (one with its own `N G obj`) can be independently rewritten this way; an object embedded inline inside its parent has no identity of its own to target. `writeObjectCustom(key, bodyWriter)` is the escape hatch for the rare case where a caller needs precise control over an object's own byte layout (this is exactly how `com.pd4ml.pdf.sign.pdf.IncrementalPdfSigner` reserves the `/Contents`/`/ByteRange` placeholders for a signature).

`finish()` writes the cross-reference table for every object written so far, then a trailer (`/Size`, `/Root` from the parsed document's catalog, `/Info` if the original had one, a fresh `/ID`, and `/Prev` chained back to the base file's own `startxref`), and returns the complete file (original bytes + this update). It throws if nothing was written, if the base file's own `startxref` couldn't be located, or if the document has no indirect `/Root`.

### 5.3 Editing recipe: locate, mutate, rewrite

The general pattern behind `coscli set`/`delete` (see the [pd4coscli Reference](pd4coscli-reference.md)), for any caller that wants to change one existing key or array element rather than build new structure from scratch:

```java
COSDocument document = COSParser.load(pdfBytes);

// 1. Locate the container to edit -- must resolve to an INDIRECT
//    COSDictionary or COSArray (its own N G obj), not a value embedded
//    inline inside something else.
COSBase parent = COSPathEvaluator.evaluateStrict(document, "/Root/Pages/Kids[0]");
if (parent.getKey() == null) {
    throw new IllegalStateException("not independently addressable");
}

// 2. Mutate it in place.
((COSDictionary) parent).setItem("Rotate", COSInteger.get(90));

// 3. Rewrite just that object as an incremental update.
IncrementalUpdateWriter update = new IncrementalUpdateWriter(pdfBytes, document);
update.writeObject(parent);
byte[] result = update.finish();
```

The same shape works for `COSArray` (`set(index, value)`/`add(value)` to replace or append, `remove(index)` to delete) in place of `COSDictionary.setItem`/`removeItem`. Removing a dictionary key entirely is `dict.setItem(key, null)` or the equivalent `dict.removeItem(key)`.

This only works on an **unencrypted** document (or one read with the right password but _not_ re-encrypted on write - see §8): `COSWriter` applies no encryption, so appending freshly-plaintext objects into a document whose base trailer still declares `/Encrypt` would produce a file most readers can't parse correctly. `coscli set`/`delete` refuse encrypted input outright for this reason, rather than emit a broken file.

`com.pd4ml.pdf.cos.util.PageTree.findPage(catalog, index)` (below) is a common way to locate the parent for a page-level edit like the one above.

## 6. Page tree

```java
COSDictionary page = PageTree.findPage(catalog, 0); // zero-based index
```

Walks `/Root/Pages/Kids` (recursively, since a page tree can nest intermediate `/Pages` nodes), resolving indirect references at every step, with an identity-based cycle guard against a malformed/cyclic tree. Returns `null` if the catalog has no usable page tree or the index is out of range. General-purpose COS navigation, useful to anything that needs "page N" - not just signing (a visible signature's placement) but form filling, annotation placement, or per-page metadata.

## 7. Filters

`com.pd4ml.pdf.cos.filter` implements `FlateDecode`, `ASCII85Decode`, `ASCIIHexDecode`, and the PNG/TIFF predictors layered on top of `FlateDecode`/`LZWDecode`. `COSStream.getDecodedBytes()` applies the full `/Filter` chain automatically (each filter's output feeding the next), stopping - and returning whatever was decoded so far - at the first _unsupported_ filter, typically an image codec such as `DCTDecode` that's expected to be handed to an image decoder directly rather than "decoded" as generic PDF filter data. `isFullyDecodable()` tells you in advance whether `getDecodedBytes()` will reach the end of the chain; `getFilterNames()` lists the chain itself (a single `COSName`, or every entry of a `/Filter` array).

## 8. Security (reading encrypted PDFs)

`com.pd4ml.pdf.cos.security` implements the PDF standard security handler - RC4 and AES, including both the legacy and the AES-256 (ISO 32000-2 hardened-hash) key derivation algorithms - for **reading** an encrypted PDF: given the right password, `COSParser.load(bytes, password)` transparently decrypts every string and stream as it parses, so nothing elsewhere in the API needs to know the document was ever encrypted.

There is deliberately **no encryption/write support**: `com.pd4ml.pdf.cos` cannot produce a re-encrypted or newly-encrypted PDF, and (§5.3) cannot correctly append an incremental update to an encrypted one either, since the writer never applies encryption to what it writes. This is also why `com.pd4ml.pdf.sign`'s signer refuses encrypted input up front (see the PD4ML Signing Manual's §5) - the same underlying limitation.

## 9. Known limits

* No signature verification, and (per §8) no encryption/write support - `com.pd4ml.pdf.cos` reads an encrypted PDF but never writes one.
* Incremental-update editing (§5.3, and `coscli set`/`delete`) requires the target to already be an indirect object; a value embedded inline in its parent isn't independently addressable and can't be rewritten without also rewriting that parent (which the caller must locate and target itself - there's no automatic "walk up to the nearest indirect ancestor").
* `DCTDecode` and other image codecs are intentionally left undecoded by `getDecodedBytes()` (§7) - they're expected to be handed directly to an image decoder, not treated as generic filtered PDF data.

## 10. See also

* [PD4ML Programmer's Manual](https://app.gitbook.com/s/33dI66uC6sTAcaPjppX2/) - the core HTML/CSS-to-PDF engine `com.pd4ml.pdf.cos` and the rest of the post-processing toolkit sit alongside.
* [pd4coscli Reference](pd4coscli-reference.md) - the command-line tool for everything in this manual, no code required.
* [PD4ML Signing Manual](../pd4ml-pdf-signing/pd4ml-signing-manual.md) - the `com.pd4ml.pdf.sign` digital-signing and PAdES-LT layer built on top of `com.pd4ml.pdf.cos`.
* [PD4ML Signing Examples](../pd4ml-pdf-signing/pd4ml-signing-examples.md) - runnable use-case examples, including two (`Ex17_CosInspectionAndPathQuery`, `Ex18_ListSignatureFields`) that use `com.pd4ml.pdf.cos` with no signing involved at all.
* [PD4ML COS Examples](pd4ml-cos-examples.md) - runnable `com.pd4ml.pdf.cos` use-case examples, no signing involved.
* [PD4ML PDF Merge Manual](../pd4ml-pdf-merge/pd4ml-merge-manual.md) - combining page ranges from multiple PDFs, also built on `com.pd4ml.pdf.cos`.
* [PD4ML PDF Optimizer Manual](../pd4ml-pdf-optimizer/pd4ml-optimize-manual.md) - cleaning up unreferenced objects and incremental-update history.
* [PD4ML XFDF Manual](../pd4ml-pdf-xfdf/pd4ml-xfdf-manual.md) - `com.pd4ml.pdf.xfdf`, importing/exporting annotations and form field values, also built on `com.pd4ml.pdf.cos`.
