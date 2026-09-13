# PD4ML PDF Optimizer API - Programmer's Manual

_Available starting from PD4ML v4.1.1_

## 1. Architecture overview

`com.pd4ml.pdf.optimizer` cleans a PDF down to exactly what's actually reachable, and collapses its cross-reference/trailer history into one fresh table - built entirely on top of `com.pd4ml.pdf.cos`, the same foundation `com.pd4ml.pdf.sign` and `com.pd4ml.pdf.merge` are built on.

**Dependency direction is one-way**: `com.pd4ml.pdf.optimizer` depends on `com.pd4ml.pdf.cos` (and, for optional output encryption, `com.pd4ml.pdf.cos.security`); `com.pd4ml.pdf.cos` never depends on it.

### 1.1 What accumulates in an incrementally-updated PDF

Two things pile up in a PDF that has been edited via **incremental update** (ISO 32000-1 §7.5.6) - e.g. by `com.pd4ml.pdf.sign`, `CosCli`'s `set`/`delete`, or any other tool that patches a file this way rather than rewriting it from scratch:

* **Unreferenced objects.** An incremental update only _adds_ new object bodies and xref entries; when it drops a reference (e.g. replacing a page dictionary that used to point at some content), the object that's no longer pointed to anywhere still has a live "in use" xref entry and its bytes are still sitting in the file. `COSParser` happily parses it into the document's object table right along with everything still actually reachable.
* **Trailer/xref history.** Each incremental update appends its own xref section and trailer, chained to the previous one via `/Prev`. `COSParser` already resolves that chain down to the current value of each object number (older, superseded bodies are simply never parsed in the first place - see §1.2), but the chain of old xref sections/trailers itself is still physically part of the file until something rewrites it away.

### 1.2 What `optimize(...)` does about it

```
[ base file ] [ update 1 ] [ update 2, /Prev -> update 1's xref ]
        \___________object table (COSParser: latest value per object number)___________/
                                          |
                                          v
                    walk reachable from /Root and /Info only
                                          |
                                          v
              COSObjectImporter (renumbers, dedupes shared resources)
                                          |
                                          v
        ContentDeduplicator (merges byte-identical duplicate objects, if enabled)
                                          |
                                          v
                   COSDocumentWriter (one fresh xref + trailer)
```

`COSParser` already collapses the chain of revisions down to the current value of each object number - a superseded object body from an earlier revision is never even parsed into the resulting `COSDocument`. What it does _not_ do is drop an object that a later revision simply stopped pointing to; that object is still "in use" per the xref table and still gets parsed. `optimize(...)` handles that half: it deep-clones only what's actually reachable by walking from the trailer's `/Root` (and `/Info`, which is otherwise unreferenced by design - it's a trailer-only entry) into a fresh `COSDocument` with compact object numbering, and writes that out as a single, from-scratch xref table and trailer. Anything not reachable is simply never visited, and every old revision's xref/trailer is gone because the output isn't an incremental update at all - it's a full rewrite.

The reachability walk and renumbering are done by `com.pd4ml.pdf.cos.util.COSObjectImporter`, a general-purpose deep-clone primitive with no knowledge of optimizing specifically - it's the same class `com.pd4ml.pdf.merge` uses to combine several sources' selected pages into one document (see the [**PD4ML PDF Merge Manual**](../pd4ml-pdf-merge/pd4ml-merge-manual.md), §1.2, for its full API and identity-preservation guarantees). Here it's seeded from a single document's own `/Root`/`/Info` rather than from several sources' pages, which is the entire difference between "merge" and "optimize" at this layer. By default, the result of that pass is then also deduplicated by content (§3) before being written out.

A side effect worth knowing `/Encrypt` lives only in the trailer - nothing under `/Root` ever points to it. Optimizing an encrypted input document (successfully decrypted via the given password) therefore drops the old `/Encrypt` dictionary along with everything else unreachable, and the output is a plain, unencrypted PDF unless `Options.encryptOutput` is used to re-protect it (§4).

## 2. `com.pd4ml.pdf.optimizer` - the public API

### 2.1 Entry point: `PdfOptimizer`

```java
PdfOptimizeResult result = PdfOptimizer.optimize(pdfBytes);
System.out.println("Removed " + result.getRemovedObjectCount() + " unreferenced object(s)");
Files.write(Paths.get("cleaned.pdf"), result.getOptimizedPdf());
```

`PdfOptimizer` has no instance state - every method is `static`.

| Method                                                     | Notes                                                                                              |
| ---------------------------------------------------------- | -------------------------------------------------------------------------------------------------- |
| `optimize(byte[])` / `optimize(byte[], Options)`           | returns a `PdfOptimizeResult`                                                                      |
| `optimize(File)` / `optimize(File, Options)`               | reads the file, then as above                                                                      |
| `optimize(InputStream)` / `optimize(InputStream, Options)` | reads the stream fully (not closed), then as above                                                 |
| `optimize(byte[], Options, OutputStream)`                  | convenience: optimizes and writes straight to a destination, still returns the `PdfOptimizeResult` |
| `optimize(File, Options, File)`                            | same, file-to-file                                                                                 |

### 2.2 `Options`

```java
PdfOptimizer.Options options = new PdfOptimizer.Options()
        .password("inputPassword")                       // if the input is encrypted
        .encryptOutput("openPassword", "ownerPassword");  // opt-in; see §5.2
```

| Method                                       | Default                | Notes                                                               |
| -------------------------------------------- | ---------------------- | ------------------------------------------------------------------- |
| `password(String)`                           | `""`                   | tried as both the user and owner password if the input is encrypted |
| `encryptOutput(userPassword, ownerPassword)` | _(output stays plain)_ | AES-256 output encryption, the modern default                       |
| `encryptOutput(PdfEncryptor.Options)`        | _(output stays plain)_ | full control - algorithm choice, permission bits (§5.2)             |
| `deduplicateContent(boolean)`                | `true`                 | merge byte-identical duplicate objects into one; see §3             |

### 2.3 `PdfOptimizeResult`

```java
byte[] getOptimizedPdf();
int getInputByteCount();
int getOutputByteCount();
int getInputObjectCount();      // total indirect objects the input's object table had -- includes unreachable ones
int getOutputObjectCount();     // always exactly what's reachable from /Root and /Info, post-dedup
int getRemovedObjectCount();    // getInputObjectCount() - getOutputObjectCount()
int getDuplicateObjectCount();  // subset of removed: merged as duplicate content specifically (see §3)
```

`getInputObjectCount()`/`getOutputObjectCount()` let a caller confirm something was actually cleaned up (or confirm nothing needed to be - `getRemovedObjectCount()` is legitimately `0` for an already-clean document) without re-parsing either the input or the output themselves. `getDuplicateObjectCount()` narrows that down further: it's `0` whenever `deduplicateContent` was turned off, and also legitimately `0` when it was left on but nothing turned out to be duplicated.

## 3. Duplicate-content dedup

By default, `optimize(...)` also merges byte-for-byte duplicate objects still reachable after the pass above - typically the same font, image, or color space embedded twice under two different object numbers (e.g. left over from two separate incremental updates each adding their own copy). Every reference to a duplicate is rewritten to point at one surviving ("canonical") copy, and a second reachability pass drops the now-orphaned duplicate and renumbers everything compactly again - the diagram in §1.2 shows where this fits (`com.pd4ml.pdf.optimizer.internal.ContentDeduplicator`).

### 3.1 What counts as a duplicate

Two indirect objects are duplicates when their fully-resolved content is identical: the same dictionary entries (order-independent - PDF dictionaries are unordered by definition), the same array elements (order-sensitive - arrays are ordered), the same raw (still-encoded) stream bytes, the same primitive values. An indirect reference nested inside either object counts as identical content only once the object it points at has itself already been found to be canonical - comparison walks the object graph bottom-up, so a parent referencing two now-identical children is correctly recognized as a duplicate of another parent that references the single canonical child.

### 3.2 What's deliberately excluded

Two kinds of object are never merged into each other, even when byte-identical: `/Type /Page` dictionaries, `/Type /Annot` dictionaries, and interactive form field dictionaries (anything carrying an `/FT` entry, terminal or not). Two blank/default form fields, for instance, can easily be byte-identical today yet get filled in independently later by a viewer that writes into a specific field by object identity - merging them would make filling in one silently affect the other. Everything else content-bearing - fonts, images, ExtGState/color-space/function dictionaries, content streams, and so on - has no such identity significance and is always safe to merge when byte-identical.

### 3.3 Turning it off

```java
PdfOptimizer.Options options = new PdfOptimizer.Options().deduplicateContent(false);
```

Disable this to skip the extra content-hashing pass on a large document where every bit of processing time matters and duplicate content isn't expected - the reachability cleanup (§1-§2) still runs either way.

## 4. Error handling

Every checked failure from `com.pd4ml.pdf.optimizer`'s public API surfaces as one type: `PdfOptimizeException` (`getCause()` carries the real underlying exception, if any).

| Message (paraphrased)                                                            | Cause                                                 |
| -------------------------------------------------------------------------------- | ----------------------------------------------------- |
| "Failed to parse input PDF"                                                      | wraps a `COSParseException` - malformed/non-PDF input |
| "Input PDF is encrypted and the given password (or no password) did not open it" | wrong/missing password for an encrypted input         |
| "Input PDF has no /Root in its trailer; nothing to optimize"                     | malformed input with no usable Catalog reference      |
| "Failed to encrypt optimized output"                                             | wraps a `COSSecurityException` from `PdfEncryptor`    |
| "Failed to write optimized PDF"                                                  | wraps a `COSException` from `COSDocumentWriter`       |
| "Failed to read `<file>`" / "Failed to write `<file>`"                           | wraps an `IOException` from a `File`-based overload   |

## 5. Encryption

### 5.1 Input

`COSParser` decrypts transparently while parsing, so an encrypted input document is handled exactly like a plain one throughout the rest of the pipeline. `optimize(...)` rejects an input only when `COSDocument.isEncrypted()` is true **and** `getEncryptionError()` is non-null, i.e. the given (or empty) password genuinely failed to authenticate as either the user or the owner password.

### 5.2 Output

The optimized output is **not** encrypted by default, even if the input was (see the callout in §1.2 - the old `/Encrypt` dictionary is simply unreachable garbage from the reachability walk's point of view, and gets dropped like anything else). Opt in explicitly:

```java
PdfOptimizer.Options options = new PdfOptimizer.Options()
        .encryptOutput("openPassword", "ownerPassword");           // AES-256 (default algorithm)

PdfOptimizer.Options fullControl = new PdfOptimizer.Options()
        .encryptOutput(new PdfEncryptor.Options()
                .userPassword("openPassword")
                .ownerPassword("ownerPassword")
                .algorithm(PdfEncryptor.Algorithm.AES_128)          // RC4_128 | AES_128 | AES_256
                .permissions(-4));                                   // raw /P bits, default "everything allowed"
```

`PdfEncryptor` (`com.pd4ml.pdf.cos.security`) is shared, general-purpose infrastructure - the same class `com.pd4ml.pdf.merge`'s `PdfMerger.encryptOutput` uses. See the [**PD4ML PDF Merge Manual**](../pd4ml-pdf-merge/pd4ml-merge-manual.md), §5.2, for how it works internally (it's the write-side counterpart of the read-only `StandardSecurityHandler`).

## 6. Not optimized (out of scope)

**Stream recompression** - an uncompressed or suboptimally-compressed content/image stream is copied through as-is; `optimize(...)` never re-encodes stream data. This is a size optimization orthogonal to both reachability cleanup and content dedup (§3) - a different kind of work, not attempted here.

## 7. Threading

`PdfOptimizer` holds no state at all (every method is `static`); call it freely from any thread. Each `optimize(...)` call is fully independent - there's no shared mutable object comparable to `PdfMerger` that would need one instance per operation. `com.pd4ml.pdf.optimizer.internal.ContentDeduplicator` (§3) is likewise stateless between calls - it builds fresh hash maps for every `optimize(...)` invocation.

## 8. See also

* [PD4ML Programmer's Manual](https://app.gitbook.com/s/33dI66uC6sTAcaPjppX2/) - the core HTML/CSS-to-PDF engine `com.pd4ml.pdf.optimizer` and the rest of the post-processing toolkit sit alongside.
* [pd4optimizecli Reference](pd4optimizecli-reference.md) - the companion command-line documentation for `com.pd4ml.pdf.optimizer.cli.Pd4OptimizeCli`
* [PD4ML PDF Merge Manual](../pd4ml-pdf-merge/pd4ml-merge-manual.md) - `com.pd4ml.pdf.merge`, which shares `COSObjectImporter` and `PdfEncryptor` with this package
* [PD4ML COS API Manual](../pd4ml-pdf-cos/pd4ml-cos-api-manual.md) - standalone `com.pd4ml.pdf.cos` programmer's manual (object model, parsing, writing, filters, encryption reading)
* [pd4coscli Reference](../pd4ml-pdf-cos/pd4coscli-reference.md) - command-line reference for `com.pd4ml.pdf.cos.cli.CosCli`
* [PD4ML XFDF Manual](../pd4ml-pdf-xfdf/pd4ml-xfdf-manual.md) - `com.pd4ml.pdf.xfdf`, which offers its own `--optimize` post-import cleanup delegating to this same package
