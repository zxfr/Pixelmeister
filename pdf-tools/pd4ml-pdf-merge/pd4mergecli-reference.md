# pd4mergecli Reference

_Available starting from PD4ML v4.1.1_

Command-line front end for `com.pd4ml.pdf.merge`: combine page ranges from one or more PDFs (each with its own optional password) into one output file, optionally encrypting it - no code to write.

**Main-Class:** `com.pd4ml.pdf.merge.cli.Pd4MergeCli`

## 1. Invocation

```
java -cp pd4ml.jar com.pd4ml.pdf.merge.cli.Pd4MergeCli [flags...]
```

Throughout this document, `pd4ml.jar` stands for the pd4ml library jar (present in the current directory in every example below) - `com.pd4ml.pdf.merge.cli.Pd4MergeCli` isn't `pd4ml.jar`'s own `Main-Class` (that's `com.pd4ml.tools.Pd4Cmd`, a different tool), so invoke it with `-cp`, not `-jar`. Running with no arguments, or `help`/`--help`/`-h` as the first argument, prints the same usage summary this page expands on.

There is exactly one operation (no subcommands, unlike `pd4signcli`). All flags use `--key=value` syntax; there is no short-flag form.

### Exit codes

| Code | Meaning                                                                                                                                    |
| ---- | ------------------------------------------------------------------------------------------------------------------------------------------ |
| `0`  | Success.                                                                                                                                   |
| `1`  | The operation failed for a business/runtime reason - bad password, out-of-range page, malformed PDF... The message on stderr explains why. |
| `2`  | Usage error - missing/invalid flags. Usage text is printed to stderr alongside the message.                                                |

## 2. Flags

### Sources - repeatable

Each `--in=` starts a new source; an optional `--password=` and/or `--pages=` immediately following it (before the _next_ `--in=`, or a global-only flag) apply to that source specifically.

| Flag              | Notes                                                                                                    |
| ----------------- | -------------------------------------------------------------------------------------------------------- |
| `--in=<file.pdf>` | required at least once; repeat to add more sources                                                       |
| `--password=<pw>` | this source's password, if encrypted (tried as user _and_ owner password; omit for an empty password)    |
| `--pages=<range>` | see §3 for the page-range grammar; **optional only when exactly one `--in` is given overall** - see §2.3 |

### Output

| Flag                 | Notes    |
| -------------------- | -------- |
| `--out=<output.pdf>` | required |

### Output encryption - optional

| Flag                                      | Notes                                                                         |
| ----------------------------------------- | ----------------------------------------------------------------------------- |
| `--encrypt-user=<pw>`                     | open (user) password for the merged output                                    |
| `--encrypt-owner=<pw>`                    | owner password for the merged output                                          |
| `--encrypt-alg=RC4_128\|AES_128\|AES_256` | default `AES_256` when any encrypt-\* flag is given                           |
| `--permissions=<int>`                     | raw `/P` permission bits (ISO 32000-1 Table 22); default "everything allowed" |

Giving _any_ `--encrypt-user`/`--encrypt-owner`/`--encrypt-alg`/`--permissions` flag turns on output encryption; the output otherwise stays plain even if one or more sources were encrypted.

### 2.3 Omitting `--pages`

`--pages` may be omitted only when **exactly one** `--in` is given overall

* the result is then that document's own pages, in order, still decrypted (and re-encrypted only if an `--encrypt-*` flag is also given). With two or more `--in` occurrences, every one of them needs its own `--pages`; an unselected source in a multi-source run is a usage/runtime error, not a silent "everything."

## 3. Page-range grammar

`--pages` accepts a 1-based, comma-separated page-range spec:

| Token              | Meaning                                                                                                                              |
| ------------------ | ------------------------------------------------------------------------------------------------------------------------------------ |
| `"2-5,8,10-12"`    | explicit pages/ranges, in the order written                                                                                          |
| `"odd"` / `"even"` | **suppresses** the opposite parity from everything _else_ in the same `--pages` value (not an additive range of its own) - see below |
| `"6+"`             | page 6 through the last page                                                                                                         |

`odd`/`even` filter the union of every other token in the same `--pages` value: `--pages=1-2,4+,odd` on a 10-page source unions `1-2` and `4+` into `1,2,4,5,6,7,8,9,10`, then `odd` suppresses the even ones, leaving `1,5,7,9`. Used alone (`--pages=odd`), the base defaults to every page in the document. `odd` and `even` together in one `--pages` value are rejected as contradictory. The combined result is always deduplicated, each page kept at the position it was first mentioned.

Shell quoting Most shells treat `+` and `,` as ordinary characters, but quote the whole `--pages=...` value anyway (e.g. `--pages="1,3,6+"`) if your source range contains spaces or shell metacharacters, or you're on a shell/OS with different quoting rules.

## 4. Examples

Two sources, mixed ranges and an `odd` filter:

```
$ java -cp pd4ml.jar com.pd4ml.pdf.merge.cli.Pd4MergeCli \
      --in=A.pdf --pages=2-5,odd \
      --in=B.pdf --password=secret --pages=1,3,6+ \
      --out=merged.pdf
```

Decrypt a single protected PDF (no merging, no re-encryption):

```
$ java -cp pd4ml.jar com.pd4ml.pdf.merge.cli.Pd4MergeCli --in=protected.pdf --password=secret --out=plain.pdf
```

Merge and encrypt the output:

```
$ java -cp pd4ml.jar com.pd4ml.pdf.merge.cli.Pd4MergeCli \
      --in=A.pdf --out=locked.pdf \
      --encrypt-user=open123 --encrypt-owner=owner456 --encrypt-alg=AES_256
```

A successful run reports what it did:

```
Merged 2 sources into merged.pdf (10201 bytes)
```

## 5. Troubleshooting

"selectPages(...) was never called for one of the merge sources" Two or more `--in` flags were given, and at least one has no `--pages=`. Add one - omitting `--pages` only works when there's exactly one source overall (§2.3)."specifies both 'odd' and 'even', which is contradictory" A single `--pages` value named both filters, e.g. `--pages=1-10,odd,even`. Use only one of them per source."Page N is out of range" A page number in `--pages` exceeds that specific source's own page count - ranges are validated per source, not against some combined total."is encrypted and the given password (or no password) did not open it" Add `--password=<pw>` for that source (either its user or owner password authenticates), or double-check the one given.Structural editing, tagging inspection, or general COS work `pd4mergecli` only merges; for arbitrary dictionary/array edits, structural inspection, or COS-path queries, see the [**pd4coscli Reference**](../pd4ml-pdf-cos/pd4coscli-reference.md) (`com.pd4ml.pdf.cos.cli.CosCli`). For importing or exporting annotations and form field values via XFDF, see the [pd4xfdfcli Reference](../pd4ml-pdf-xfdf/pd4xfdfcli-reference.md) (`com.pd4ml.pdf.xfdf.cli.Pd4XfdfCli`).
