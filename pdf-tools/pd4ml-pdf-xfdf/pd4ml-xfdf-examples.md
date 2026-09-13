# Examples

_Available starting from PD4ML v4.1.1_

All examples live under `src/test/java/com/pd4ml/pdf/xfdf/example/usecases/` (test scope -- not shipped in the distributed jar) and are ordinary runnable classes with a `main(String[] args)`. Easiest from an IDE: right-click the class, Run. From the command line:

```bash
mvn test-compile
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt
java -cp "target/test-classes;target/classes;$(cat cp.txt)" \
     com.pd4ml.pdf.xfdf.example.usecases.<ClassName> <args...>
```

Every example builds its own small sample PDF(s) in memory (via `com.pd4ml.examples.support.SamplePdf`, no external fixture files needed) and was actually run end-to-end -- these aren't untested snippets.

| #  | Class                               | Demonstrates                                                                                                                 |
| -- | ----------------------------------- | ---------------------------------------------------------------------------------------------------------------------------- |
| 01 | `Ex01_BasicExportAndImport`         | The minimal round trip: export a PDF's annotations to XFDF, then import that XFDF into a different PDF. Start here.          |
| 02 | `Ex02_InspectingTheParsedModel`     | `XfdfReader.read(...)` turns XFDF bytes into a plain `XfdfDocument`/`XfdfAnnotation` model you can inspect directly.         |
| 03 | `Ex03_UpdateIfExistsSwitch`         | `Options.updateIfExists(boolean)` -- update a matching annotation in place vs. always adding a new one.                      |
| 04 | `Ex04_FormFieldValues`              | Form field values travel through `<fields>`, only ever updating an existing AcroForm field -- including checkbox `/AS` sync. |
| 05 | `Ex05_TypeAndIdFiltering`           | `Options.types(String)`/`ids(String)` restrict export or import to a comma-separated subset.                                 |
| 06 | `Ex06_EncryptedSourceExport`        | Exporting from an encrypted PDF -- transparent decryption via the password overload.                                         |
| 07 | `Ex07_PostImportOptimizeAndEncrypt` | `Options.optimizeOutput(boolean)` + `encryptOutput(...)` chained after an import.                                            |
| 08 | `Ex08_ErrorHandlingPatterns`        | Every `XfdfException` failure mode and how to recognize it.                                                                  |

For a ready-made command-line tool covering the same ground without writing any code, see the [pd4xfdfcli Reference](pd4xfdfcli-reference.md) and run `com.pd4ml.pdf.xfdf.cli.Pd4XfdfCli`.

## See also

* [PD4ML XFDF Manual](pd4ml-xfdf-manual.md) -- the programmer's manual behind these examples.
* [pd4xfdfcli Reference](pd4xfdfcli-reference.md) -- command-line reference for `com.pd4ml.pdf.xfdf.cli.Pd4XfdfCli`.
* [PD4ML COS API Manual](../pd4ml-pdf-cos/pd4ml-cos-api-manual.md) / [PD4ML COS Examples](../pd4ml-pdf-cos/pd4ml-cos-examples.md) -- standalone `com.pd4ml.pdf.cos` foundation this package is built on.
* [pd4coscli Reference](../pd4ml-pdf-cos/pd4coscli-reference.md) -- command-line reference for `com.pd4ml.pdf.cos.cli.CosCli`.
* [PD4ML PDF Merge Manual](../pd4ml-pdf-merge/pd4ml-merge-manual.md) / [PD4ML Merge Examples](../pd4ml-pdf-merge/pd4ml-merge-examples.md) -- combining page ranges from several PDFs, a natural companion to annotation import/export.
* [PD4ML PDF Optimizer Manual](../pd4ml-pdf-optimizer/pd4ml-optimize-manual.md) -- `Options.optimizeOutput(boolean)` delegates here.
* [PD4ML Signing Manual](../pd4ml-pdf-signing/pd4ml-signing-manual.md) -- `com.pd4ml.pdf.sign`, which shares the same `com.pd4ml.pdf.cos` foundation and, via `com.pd4ml.pdf.cos.security`, the same `PdfEncryptor` this package's output encryption uses.
