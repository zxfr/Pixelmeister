# pd4optimizecli Reference

_Available starting from PD4ML v4.1.1_

Command-line front end for `com.pd4ml.pdf.optimizer`: drop objects no longer reachable from a PDF's `/Root`/`/Info` (typically left behind by an earlier incremental update) and collapse its cross-reference/trailer history into one fresh table - no code to write.

**Main-Class:** `com.pd4ml.pdf.optimizer.cli.Pd4OptimizeCli`

## 1. Invocation

```
java -cp pd4ml.jar com.pd4ml.pdf.optimizer.cli.Pd4OptimizeCli [flags...]
```

Throughout this document, `pd4ml.jar` stands for the pd4ml library jar (present in the current directory in every example below) - `com.pd4ml.pdf.optimizer.cli.Pd4OptimizeCli` isn't `pd4ml.jar`'s own `Main-Class` (that's `com.pd4ml.tools.Pd4Cmd`, a different tool), so invoke it with `-cp`, not `-jar`. Running with no arguments, or `help`/`--help`/`-h` as the first argument, prints the same usage summary this page expands on.

There is exactly one operation (no subcommands, no page selection - the whole document is always cleaned). All flags use `--key=value` syntax; there is no short-flag form.

### Exit codes

| Code | Meaning                                                                                                                             |
| ---- | ----------------------------------------------------------------------------------------------------------------------------------- |
| `0`  | Success.                                                                                                                            |
| `1`  | The operation failed for a business/runtime reason - bad password, malformed PDF, no `/Root`... The message on stderr explains why. |
| `2`  | Usage error - missing/invalid flags. Usage text is printed to stderr alongside the message.                                         |

## 2. Flags

### Input

| Flag              | Notes                                                                                      |
| ----------------- | ------------------------------------------------------------------------------------------ |
| `--in=<file.pdf>` | required                                                                                   |
| `--password=<pw>` | if the input is encrypted (tried as user _and_ owner password; omit for an empty password) |

### Output

| Flag                 | Notes    |
| -------------------- | -------- |
| `--out=<output.pdf>` | required |

### Duplicate-content dedup - optional

| Flag         | Notes                                                                                            |
| ------------ | ------------------------------------------------------------------------------------------------ |
| `--no-dedup` | skip merging byte-identical duplicate objects (e.g. the same font embedded twice); on by default |

Byte-identical duplicate objects reachable after the unreferenced-object cleanup - typically the same font, image, or color space embedded twice under two different object numbers - are merged into one shared copy by default. `/Type /Page`/`/Type /Annot` dictionaries and interactive form fields are never merged this way even if byte-identical (see the API manual's §3.2). Pass `--no-dedup` to skip the extra content-hashing pass and keep only the reachability cleanup.

### Output encryption - optional

| Flag                                      | Notes                                                                         |
| ----------------------------------------- | ----------------------------------------------------------------------------- |
| `--encrypt-user=<pw>`                     | open (user) password for the optimized output                                 |
| `--encrypt-owner=<pw>`                    | owner password for the optimized output                                       |
| `--encrypt-alg=RC4_128\|AES_128\|AES_256` | default `AES_256` when any encrypt-\* flag is given                           |
| `--permissions=<int>`                     | raw `/P` permission bits (ISO 32000-1 Table 22); default "everything allowed" |

Giving _any_ `--encrypt-user`/`--encrypt-owner`/`--encrypt-alg`/ `--permissions` flag turns on output encryption; the output otherwise stays plain even if the input was encrypted (see the callout below).

Encrypted input, plain output by default `/Encrypt` lives only in the input's trailer, not under `/Root` - the reachability walk that drops unreferenced objects drops the old `/Encrypt` dictionary right along with everything else unreachable. Optimizing an encrypted PDF with no `--encrypt-*` flag therefore produces a **plain, unencrypted** output, not a re-encrypted one.

## 3. Examples

Clean a PDF that's accumulated incremental-update cruft:

```
$ java -cp pd4ml.jar com.pd4ml.pdf.optimizer.cli.Pd4OptimizeCli --in=bloated.pdf --out=cleaned.pdf
```

Optimize an encrypted input (output stays plain):

```
$ java -cp pd4ml.jar com.pd4ml.pdf.optimizer.cli.Pd4OptimizeCli --in=protected.pdf --password=secret --out=cleaned.pdf
```

Optimize and re-encrypt the output:

```
$ java -cp pd4ml.jar com.pd4ml.pdf.optimizer.cli.Pd4OptimizeCli \
      --in=A.pdf --out=cleaned-locked.pdf \
      --encrypt-user=open123 --encrypt-owner=owner456 --encrypt-alg=AES_256
```

A successful run reports what it did, including how many of the removed objects were merged as duplicate content specifically:

```
Optimized bloated.pdf -> cleaned.pdf (1070 -> 594 bytes, 2 object(s) removed out of 7 (1 of them duplicate content merged))
```

Skip the dedup pass:

```
$ java -cp pd4ml.jar com.pd4ml.pdf.optimizer.cli.Pd4OptimizeCli --in=bloated.pdf --out=cleaned.pdf --no-dedup
```

## 4. Troubleshooting

"is encrypted and the given password (or no password) did not open it" Add `--password=<pw>` (either the input's user or owner password authenticates), or double-check the one given."Input PDF has no /Root in its trailer; nothing to optimize" The file is malformed or not actually a PDF - there's no Catalog reference to walk from."0 object(s) removed" every time Not a bug - a document with no incremental-update history (or one whose every incremental update only ever added things still referenced today) and no duplicate content genuinely has nothing to clean up. The trailer/xref history is still collapsed to one fresh table either way, even when the object count doesn't change; compare `--in`'s and `--out`'s byte sizes if you want to confirm that part happened.Expecting a smaller file, but it barely shrank (or grew) `pd4optimizecli` removes unreferenced _objects_, merges byte-identical duplicate objects (unless `--no-dedup` was given), and collapses trailer history - but it does not recompress streams, and dedup only catches objects that are byte-for-byte identical, not merely similar (see the API manual's §6, "Not optimized"). A small, already-clean input, or one whose bloat is inside large already-compressed streams rather than unreferenced/duplicate objects, won't shrink much. Output encryption (if requested) also adds a modest amount of overhead (an `/Encrypt` dictionary plus per-object IV padding).Structural editing, merging, or general COS work `pd4optimizecli` only cleans a single document; for combining page ranges from several PDFs, see the [**pd4mergecli Reference**](../pd4ml-pdf-merge/pd4mergecli-reference.md) (`com.pd4ml.pdf.merge.cli.Pd4MergeCli`). For arbitrary dictionary/array edits or COS-path queries, see the [**pd4coscli Reference**](../pd4ml-pdf-cos/pd4coscli-reference.md) (`com.pd4ml.pdf.cos.cli.CosCli`). For importing or exporting annotations and form field values via XFDF (which can itself call this same cleanup via its own `--optimize` flag), see the [pd4xfdfcli Reference](../pd4ml-pdf-xfdf/pd4xfdfcli-reference.md) (`com.pd4ml.pdf.xfdf.cli.Pd4XfdfCli`).
