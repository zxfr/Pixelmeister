# pd4xfdfcli Reference

_Available starting from PD4ML v4.1.1_

Command-line front end for `com.pd4ml.pdf.xfdf`: export a PDF's annotations and form field values to an XFDF file, or import an XFDF file's annotations/field values back into a PDF - no code to write.

**Main-Class:** `com.pd4ml.pdf.xfdf.cli.Pd4XfdfCli`

## 1. Invocation

```
java -cp pd4ml.jar com.pd4ml.pdf.xfdf.cli.Pd4XfdfCli <export|import> [flags...]
```

Throughout this document, `pd4ml.jar` stands for the pd4ml library jar (present in the current directory in every example below) - `com.pd4ml.pdf.xfdf.cli.Pd4XfdfCli` isn't `pd4ml.jar`'s own `Main-Class` (that's `com.pd4ml.tools.Pd4Cmd`, a different tool), so invoke it with `-cp`, not `-jar`. Running with no arguments, or `help`/`--help`/`-h` as the first argument, prints the same usage summary this page expands on.

Two modes, given as the first positional argument: **`export`** and **`import`**. All flags use `--key=value` syntax; a flag with no `=value` (`--update-if-exists`, `--optimize`) is a boolean switch, true by its mere presence. There is no short-flag form.

### Exit codes

| Code | Meaning                                                                                                                      |
| ---- | ---------------------------------------------------------------------------------------------------------------------------- |
| `0`  | Success.                                                                                                                     |
| `1`  | The operation failed for a business/runtime reason - bad password, malformed PDF/XFDF... The message on stderr explains why. |
| `2`  | Usage error - missing/invalid mode or flags. Usage text is printed to stderr alongside the message.                          |

## 2. `export` - PDF annotations/fields -> XFDF

```
pd4xfdfcli export --in=<file.pdf> [--password=<pw>] --out=<output.xfdf> [--types=<csv>] [--ids=<csv>]
```

| Flag                  | Notes                                                                                      |
| --------------------- | ------------------------------------------------------------------------------------------ |
| `--in=<file.pdf>`     | required                                                                                   |
| `--password=<pw>`     | if the input is encrypted (tried as user _and_ owner password; omit for an empty password) |
| `--out=<output.xfdf>` | required                                                                                   |
| `--types=<csv>`       | restrict to these annotation XFDF element names and/or field `/FT` codes - see §4          |
| `--ids=<csv>`         | restrict to these annotation `/NM` values and/or fully-qualified field names - see §4      |

### Examples

Export everything:

```
$ java -cp pd4ml.jar com.pd4ml.pdf.xfdf.cli.Pd4XfdfCli export --in=reviewed.pdf --out=comments.xfdf
Exported reviewed.pdf -> comments.xfdf (1254 bytes)
```

Export only highlights:

```
$ java -cp pd4ml.jar com.pd4ml.pdf.xfdf.cli.Pd4XfdfCli export --in=reviewed.pdf --out=highlights.xfdf --types=highlight
```

Export from an encrypted PDF:

```
$ java -cp pd4ml.jar com.pd4ml.pdf.xfdf.cli.Pd4XfdfCli export --in=protected.pdf --password=secret --out=comments.xfdf
```

## 3. `import` - XFDF -> PDF annotations/fields

```
pd4xfdfcli import --in=<file.pdf> [--password=<pw>]
                   --xfdf=<input.xfdf>
                   --out=<output.pdf>
                   [--update-if-exists]
                   [--types=<csv>] [--ids=<csv>]
                   [--optimize]
                   [--encrypt-user=<pw>] [--encrypt-owner=<pw>]
                   [--encrypt-alg=RC4_128|AES_128|AES_256] [--permissions=<int>]
```

### Input/output

| Flag                  | Notes                     |
| --------------------- | ------------------------- |
| `--in=<file.pdf>`     | required                  |
| `--password=<pw>`     | if the input is encrypted |
| `--xfdf=<input.xfdf>` | required                  |
| `--out=<output.pdf>`  | required                  |

### Matching behavior

| Flag                 | Notes                                                                                                                                                                  |
| -------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `--update-if-exists` | an `<annots>` entry whose `name` matches an annotation already in the PDF updates it in place instead of adding a duplicate; default: always add (see the manual's §4) |
| `--types=<csv>`      | apply only these annotation types and/or field `/FT` codes - see §4                                                                                                    |
| `--ids=<csv>`        | apply only these annotation IDs and/or field names - see §4                                                                                                            |

`<fields>` entries always only update an existing AcroForm field, regardless of `--update-if-exists` - see the manual's §5.

### Post-processing - optional

| Flag                                      | Notes                                                                                               |
| ----------------------------------------- | --------------------------------------------------------------------------------------------------- |
| `--optimize`                              | run the same reachability/dedup cleanup as `pd4optimizecli` on the imported PDF before it's written |
| `--encrypt-user=<pw>`                     | open (user) password for the imported output                                                        |
| `--encrypt-owner=<pw>`                    | owner password for the imported output                                                              |
| `--encrypt-alg=RC4_128\|AES_128\|AES_256` | default `AES_256` when any encrypt-\* flag is given                                                 |
| `--permissions=<int>`                     | raw `/P` permission bits (ISO 32000-1 Table 22); default "everything allowed"                       |

If both `--optimize` and an `--encrypt-*` flag are given, optimization always runs first and encryption is applied to its result (see the manual's §6.2).

### Examples

Add every annotation/field value from an XFDF file:

```
$ java -cp pd4ml.jar com.pd4ml.pdf.xfdf.cli.Pd4XfdfCli import --in=original.pdf --xfdf=comments.xfdf --out=annotated.pdf
Imported comments.xfdf into original.pdf -> annotated.pdf (6 annotation(s) added, 0 updated, 2 field(s) updated)
```

Re-import a revised XFDF, updating matching annotations in place:

```
$ java -cp pd4ml.jar com.pd4ml.pdf.xfdf.cli.Pd4XfdfCli import --in=annotated.pdf --xfdf=comments-revised.xfdf --out=merged.pdf --update-if-exists
```

Import only checkbox/radio field values, skipping everything else in the file:

```
$ java -cp pd4ml.jar com.pd4ml.pdf.xfdf.cli.Pd4XfdfCli import --in=form.pdf --xfdf=answers.xfdf --out=filled.pdf --types=btn
```

Import, clean up, and lock the result:

```
$ java -cp pd4ml.jar com.pd4ml.pdf.xfdf.cli.Pd4XfdfCli import \
      --in=form.pdf --xfdf=answers.xfdf --out=filled-locked.pdf \
      --optimize --encrypt-user=open123 --encrypt-owner=owner456 --encrypt-alg=AES_256
```

A successful run reports what it did, including any field names that matched nothing:

```
Imported answers.xfdf into form.pdf -> filled.pdf (0 annotation(s) added, 0 updated, 2 field(s) updated, 1 field(s) not found)
Fields not found: bonus.signature
```

## 4. `--types` / `--ids` grammar

Both flags take a comma-separated list and are shared, unchanged, between `export` and `import` - see the manual's §6.1 for the full matching rules. In short: `--types` matches an annotation's lower-case XFDF element name (`square`, `freetext`, ...) or a field's PDF `/FT` code (`Tx`/`Btn`/`Ch`, case-insensitive) - the two vocabularies never overlap, so `--types=square,tx` selects Square annotations and text fields at once. `--ids` matches an annotation's exact `/NM` or a field's exact fully-qualified name.

## 5. Troubleshooting

"is encrypted and the given password (or no password) did not open it" Add `--password=<pw>` (either the input's user or owner password authenticates), or double-check the one given."Not an XFDF document (root element is not \<xfdf>)" / "Malformed XFDF XML" The `--xfdf` file isn't well-formed XML, or its root element isn't `<xfdf>` - check it opens cleanly in a text editor and starts with an `<xfdf xmlns="http://ns.adobe.com/xfdf/">` root.A field is listed under "Fields not found" even though it "obviously" exists Either the name in the XFDF doesn't exactly match the field's fully-qualified (dot-joined) name in the target PDF's `/AcroForm` (case-sensitive), the field is a `/FT /Sig` signature field (never touched - see the manual's §5), or it was excluded by `--types`/`--ids` (which is reported this same way, not separately - see §4).An updated annotation/field doesn't look different in my viewer Import never regenerates `/AP` appearance streams (see the manual's §4) - the new value is correct in the PDF immediately, but most viewers only re-render it the next time they regenerate appearances (typically on open).Structural editing, merging, or general COS work `pd4xfdfcli` only handles annotations and form field values; for combining page ranges from several PDFs, see the [pd4mergecli Reference](../pd4ml-pdf-merge/pd4mergecli-reference.md) (`com.pd4ml.pdf.merge.cli.Pd4MergeCli`). For arbitrary dictionary/array edits or COS-path queries, see the [pd4coscli Reference](../pd4ml-pdf-cos/pd4coscli-reference.md) (`com.pd4ml.pdf.cos.cli.CosCli`). For reachability/duplicate-content cleanup on its own, see the [pd4optimizecli Reference](../pd4ml-pdf-optimizer/pd4optimizecli-reference.md) (`com.pd4ml.pdf.optimizer.cli.Pd4OptimizeCli`).

## 6. See also

* [PD4ML XFDF Manual](pd4ml-xfdf-manual.md) - the programmer's manual behind this tool
* [PD4ML XFDF Examples](pd4ml-xfdf-examples.md) - runnable `com.pd4ml.pdf.xfdf` use-case examples
* [pd4optimizecli Reference](../pd4ml-pdf-optimizer/pd4optimizecli-reference.md) - the `--optimize` flag delegates here
* [pd4mergecli Reference](../pd4ml-pdf-merge/pd4mergecli-reference.md) - combining page ranges from several PDFs
* [pd4coscli Reference](../pd4ml-pdf-cos/pd4coscli-reference.md) - command-line reference for `com.pd4ml.pdf.cos.cli.CosCli`
* [pdfsigncli Reference](../pd4ml-pdf-signing/pd4signcli-reference.md) - digital signing and PAdES-LT/LTV
