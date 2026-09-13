# coscli - Command-Line Reference

_Available starting from PD4ML v4.1.1_

`com.pd4ml.pdf.cos.cli.CosCli` is a single command-line tool for the `com.pd4ml.pdf.cos` PDF COS object model alone: parsing, structural inspection, COS-path queries, object listing, page-tree walking, and stream extraction (read), plus single-key edits applied as a PDF incremental update (write). It has no dependency on `com.pd4ml.pdf.sign` - for digital signing and PAdES-LT, see the [pdfsigncli Reference](../pd4ml-pdf-signing/pd4signcli-reference.md) instead.

## Invocation

`com.pd4ml.pdf.cos.cli.CosCli` is not `pd4ml.jar`'s own `Main-Class` (that's `com.pd4ml.tools.Pd4Cmd`, a different tool), so run it with `-cp` rather than `-jar`:

```bash
java -cp pd4ml.jar com.pd4ml.pdf.cos.cli.CosCli <command> [args...]
```

Throughout this document, `pd4ml.jar` stands for the pd4ml library jar (present in the current directory in every example below). Running with no arguments, or `help`/`--help`/`-h` as the first argument, prints the same usage summary this document expands on.

Six commands: **`inspect`**, **`list`**, **`pages`**, **`dump`**, **`set`**, **`delete`**. Flags use `--key=value` syntax; a flag with no `=value` (e.g. `--raw`) is a boolean switch, true by its mere presence. There is no short-flag form.

### Exit codes

| Code | Meaning                                                                                                                                                                                                                |
| ---- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| 0    | Success                                                                                                                                                                                                                |
| 1    | The operation failed for a business/runtime reason (bad password, malformed PDF, an unresolvable COS path, an out-of-range index, an attempt to edit a non-indirect object, ...) -- the message on stderr explains why |
| 2    | Usage error (missing/invalid arguments or flags) -- usage text is printed to stderr alongside the message                                                                                                              |

***

## `inspect` - structural summary + COS-path queries

```
coscli inspect <input.pdf> [--password=...] [cos-path-expr ...]
```

Parses `input.pdf` with `com.pd4ml.pdf.cos.parser.COSParser` and prints:

* PDF version
* indirect object count
* whether the document is encrypted (and, if so, whether the given/empty password was accepted)
* page count (`/Root/Pages/Count`)
* whether an `/AcroForm` is present

Each trailing positional argument (not starting with `--`) is evaluated as a **COS path expression** against the parsed document, and the result printed. COS path syntax mirrors a simplified filesystem-like path over the object graph:

| Root                            | Meaning                                       |
| ------------------------------- | --------------------------------------------- |
| _(none)_, `/...`, `trailer/...` | starts at the document trailer dictionary     |
| `Root/...` or `catalog/...`     | shorthand for `trailer/Root/...`              |
| `N G R/...`                     | starts at an explicit indirect object `N G R` |

| Step                        | Meaning                                                        |
| --------------------------- | -------------------------------------------------------------- |
| `/Name` or bare `Name`      | look up a key in the current dictionary (or stream) node       |
| `[N]` or a bare integer `N` | index into the current array node                              |
| `N G R`                     | jump directly to an indirect object, ignoring the current node |

A dictionary key name may contain `\/` / `\[` / `\\` escapes and `#xx` hex escapes, the same convention as PDF name objects. Indirect references are followed transparently at every step, so a path never needs to spell out `.resolve()`. Examples: `/Root/Pages/Count`, `/Root/Pages/Kids[0]/MediaBox`, `trailer/Size`, `trailer/ID[0]`, `12 0 R/Filter`.

A stream value's decoded bytes (or raw bytes, if the filter chain isn't fully understood -- e.g. `DCTDecode` image data) are previewed, truncated to 200 characters - use `dump` (below) to extract the full, untruncated bytes.

**`--password=<pw>`** - tried as both the user and owner password if the PDF is encrypted; omit for an empty password.

### Examples

```bash
java -cp pd4ml.jar com.pd4ml.pdf.cos.cli.CosCli inspect report.pdf
java -cp pd4ml.jar com.pd4ml.pdf.cos.cli.CosCli inspect report.pdf "/Root/Pages/Count" "/Root/Pages/Kids[0]/MediaBox"
java -cp pd4ml.jar com.pd4ml.pdf.cos.cli.CosCli inspect protected.pdf --password=secret "trailer/Size"
```

***

## `list` - enumerate every indirect object

```
coscli list <input.pdf> [--password=...] [--type=<Name>]
```

Prints one line per indirect object in the document's object table: its object number and generation, the resolved Java class (`COSDictionary`/`COSArray`/`COSStream`/`COSString`/...), and - for a dictionary or stream - its `/Type` and `/Subtype` if present. A stream also shows its raw (still-encoded) byte count. A summary line reports how many objects were printed out of the document's total.

**`--type=<Name>`** filters the listing to dictionaries/streams whose `/Type` exactly equals `<Name>` (e.g. `--type=Page`, `--type=Font`, `--type=XObject`).

### Examples

```bash
$ java -cp pd4ml.jar com.pd4ml.pdf.cos.cli.CosCli list report.pdf
1 0 obj  COSDictionary  /Type=/Catalog
2 0 obj  COSDictionary  /Type=/Pages
3 0 obj  COSDictionary  /Type=/Page
4 0 obj  COSStream  (40 raw bytes)
5 0 obj  COSDictionary  /Type=/Font  /Subtype=/Type1

5 objects listed out of 5 total.

$ java -cp pd4ml.jar com.pd4ml.pdf.cos.cli.CosCli list report.pdf --type=Page
3 0 obj  COSDictionary  /Type=/Page

1 object listed (filtered to /Type=/Page) out of 5 total.
```

***

## `pages` - walk the page tree

```
coscli pages <input.pdf> [--password=...]
```

For each page `0..Count-1` (via `com.pd4ml.pdf.cos.util.PageTree`), prints its own object reference (or `(inline, no object number)` if it has none), its `/MediaBox`, its `/Rotate` (default `0` if absent), and whether `/Resources` is set directly on the page dictionary itself (`own`) or must be inherited from an ancestor in the page tree (`inherited`). Prints `(no usable page tree found)` rather than an error if the catalog or `/Pages` is missing or malformed.

### Example

```bash
$ java -cp pd4ml.jar com.pd4ml.pdf.cos.cli.CosCli pages report.pdf
page 0: 3 0 R  MediaBox=[0 0 612 792]  Rotate=0  Resources=own
page 1: 7 0 R  MediaBox=[0 0 612 792]  Rotate=90  Resources=inherited
```

***

## `dump` - extract a stream's bytes, or print any node's PDF syntax

```
coscli dump <input.pdf> <cos-path> [--password=...] [--out=<file>] [--raw]
```

Evaluates `<cos-path>` **strictly** - an unresolvable path is a runtime error (exit 1), never silent empty output. If the result is a stream, writes its **decoded** bytes (the full `/Filter` chain applied) by default, or its **raw**, still-encoded bytes with `--raw`; if the filter chain isn't fully understood (e.g. an image codec such as `DCTDecode`), a note on stderr explains the fallback to raw bytes even without `--raw`. If the result is anything other than a stream, prints its PDF-syntax representation (a dictionary, array, name, number, string, ...).

Without `--out=<file>`, the bytes go straight to stdout - redirect or pipe as needed. With `--out=<file>`, they're written to that file instead and a byte count is printed.

### Examples

```bash
java -cp pd4ml.jar com.pd4ml.pdf.cos.cli.CosCli dump report.pdf "/Root/Pages/Kids[0]/Contents" > page0-content.txt
java -cp pd4ml.jar com.pd4ml.pdf.cos.cli.CosCli dump report.pdf "/Root/Pages/Kids[0]/Resources/XObject/Im0" --raw --out=image0.raw
java -cp pd4ml.jar com.pd4ml.pdf.cos.cli.CosCli dump report.pdf "/Root/Pages/Kids[0]"
<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 5 0 R >> >> /Contents 4 0 R >>
```

***

## `set` - change one dictionary key or array element

```
coscli set <input.pdf> <output.pdf> <parent-cos-path> <key> <value> [--password=...] [--type=string|name|int|float|bool]
```

Resolves `<parent-cos-path>`, applies the edit to that resolved node directly, then appends a PDF **incremental update** (`com.pd4ml.pdf.cos.writer.IncrementalUpdateWriter`) containing just the rewritten object, and writes the result to `<output.pdf>` - the same mechanism `pdfsigncli sign`/`ltv` use to append a signature or `/DSS`, just for an arbitrary key instead. The original bytes are never modified.

**`<parent-cos-path>` must resolve to an indirect (independently object-numbered) `COSDictionary` or `COSArray`** - one that already has its own `N G obj`. A node embedded _inline_ inside its parent (no object number of its own - common for small arrays like `/MediaBox`, sometimes for `/Kids`) can't be independently rewritten by an incremental update; `coscli` reports this plainly (exit 1) rather than silently doing nothing. Use `list` to see which objects in a document are indirect at all, and `inspect`/`dump` to check a specific path first.

**`<key>`** is a dictionary key name (added if new, overwritten if already present) when the parent is a dictionary; an integer array index when the parent is an array - an existing index (`0..size-1`) replaces that element, and exactly `size` appends a new one.

**`--type`** (default `string`) picks how `<value>` is parsed:

| `--type`           | Produces     | Example value                    |
| ------------------ | ------------ | -------------------------------- |
| `string` (default) | `COSString`  | `"Approved by QA"`               |
| `name`             | `COSName`    | `Approved` (becomes `/Approved`) |
| `int`              | `COSInteger` | `90`                             |
| `float`            | `COSFloat`   | `1.5`                            |
| `bool`             | `COSBoolean` | `true`                           |

### Examples

```bash
# rotate a page 90 degrees
java -cp pd4ml.jar com.pd4ml.pdf.cos.cli.CosCli set report.pdf out.pdf "/Root/Pages/Kids[0]" Rotate 90 --type=int

# add a custom flag to the catalog
java -cp pd4ml.jar com.pd4ml.pdf.cos.cli.CosCli set report.pdf out.pdf "/Root" CustomFlag true --type=bool

# append to an indirect array (size 3 -> index 3 appends)
java -cp pd4ml.jar com.pd4ml.pdf.cos.cli.CosCli set report.pdf out.pdf "/Root/Pages/Kids[0]/Extra" 3 "D"
```

***

## `delete` - remove one dictionary key or array element

```
coscli delete <input.pdf> <output.pdf> <parent-cos-path> <key> [--password=...]
```

Same `<parent-cos-path>` requirement as `set` (must resolve to an indirect dictionary or array). `<key>` is a dictionary key name - it must already be present; deleting an absent key is a runtime error (exit 1), not a silent no-op - or an array index, which must be in range (`0..size-1`).

### Example

```bash
java -cp pd4ml.jar com.pd4ml.pdf.cos.cli.CosCli delete report.pdf out.pdf "/Root/Info" Keywords
```

***

## Troubleshooting

**"'\<path>' is not an indirect object (no object number)..."** - the target of a `set`/`delete` is embedded inline inside its parent rather than being its own `N G obj`. Pick a path to an indirect ancestor instead (a Page, the Catalog, an indirect Resources dictionary, ...); `list` shows which objects in the document actually have their own object number.

**"Index N out of range for an array of size M"** - array indices for `set` run `0..size` (size appends); for `delete`, `0..size-1`.

**"Key '\<key>' not present at \<path>"** - `delete` targeted a key that isn't there. Run `inspect`/`dump` on the parent path to see what actually exists.

**"Could not resolve '\<path>': ..."** - the COS path expression itself doesn't exist in this document (a missing key, an out-of-range index, or a step applied to the wrong kind of node) - the message names exactly which step failed.

**Encryption / signing / verification** - `coscli` reads an encrypted PDF transparently once given the right password, but has no support for _writing_ a re-encrypted or decrypted copy - `set`/`delete` refuse encrypted input for that reason. Digital signing, timestamping, DocMDP certification, and PAdES-LT/LTV are entirely out of scope for `coscli` - see the [pdfsigncli Reference](../pd4ml-pdf-signing/pd4signcli-reference.md) for all of that.

**Merging, cleanup, or annotations/form values** - `coscli` only edits one document's COS objects directly; for combining page ranges from several PDFs, see the [pd4mergecli Reference](../pd4ml-pdf-merge/pd4mergecli-reference.md). For reachability/duplicate-content cleanup, see the [pd4optimizecli Reference](../pd4ml-pdf-optimizer/pd4optimizecli-reference.md). For importing or exporting annotations and form field values via XFDF, see the [pd4xfdfcli Reference](../pd4ml-pdf-xfdf/pd4xfdfcli-reference.md).
