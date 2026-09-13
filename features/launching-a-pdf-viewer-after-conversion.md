# Launching a PDF Viewer After Conversion

In a desktop application, it's common to want the just-generated PDF to open immediately in whatever PDF viewer is installed, rather than being written silently to disk. This is entirely outside PD4ML's own API -- once `writePDF(...)` has produced the file, opening it is a plain Java/OS question -- but it's a common enough follow-up step to a conversion that it's worth documenting properly, including the one piece of this that's changed since the original version of this page: the standard-library API for doing it.

## Hardcoded, viewer-specific approaches

Before `java.awt.Desktop` existed, opening a file in its default application meant shelling out to a specific, hardcoded command -- which only works on the OS (and, often, only the exact installed application) it was written for:

```java
// Windows, invoking a specific Acrobat Reader install directly
String params = "C:\\Program Files\\Adobe\\Reader 9.0\\Reader\\AcroRD32.exe " + pdfFileName;
Runtime.getRuntime().exec(params);

// older macOS, invoking a specific application by name
String params = "open /Applications/Safari.app " + resultPdf.getAbsolutePath();
Runtime.getRuntime().exec(params);

// SWT-based desktop applications (Eclipse RCP, etc.)
org.eclipse.swt.program.Program.launch("file:" + resultPdf.getAbsolutePath());

// Windows, via the shell's own file-type association instead of a specific app
Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + resultPdf.getAbsolutePath());
```

Each of these is brittle in its own way -- a hardcoded install path breaks the moment Reader is upgraded or simply installed somewhere else, and every approach is tied to one specific OS or GUI toolkit. When driving Adobe Reader directly by its executable path, Reader itself additionally accepts a range of its own command-line open parameters (target page, view mode, and so on) appended after the file path; see Adobe's [PDFOpenParameters.pdf](http://old.pd4ml.com/i/PDFOpenParameters.pdf) reference for the full set, if working with this specific legacy technique.

## The current, portable approach: `java.awt.Desktop`

Since Java 6, none of the above is necessary. `Desktop.getDesktop().open(file)` asks the operating system to open the file with whatever application is registered as its default handler -- exactly what double-clicking the file in a file manager would do -- without PD4ML or the calling application needing to know that application's identity or install location at all:

```java
if (Desktop.isDesktopSupported()) {
    Desktop.getDesktop().open(resultPdf);
} else {
    System.out.println("Awt Desktop is not supported!");
}
```

One small refinement worth knowing: alongside the broad `isDesktopSupported()` check above, the same API also exposes a narrower `Desktop.getDesktop().isSupported(Desktop.Action.OPEN)`, which checks for exactly this one capability rather than desktop integration support in general -- worth preferring when `open(...)` is the only `Desktop` feature actually in use.

## Complete example

```java
package samples;

import java.awt.Desktop;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidParameterException;

import com.pd4ml.Dimensions.Units;
import com.pd4ml.PD4ML;
import com.pd4ml.PageMargins;
import com.pd4ml.PageSize;

public class PdfViewerStarter {
    protected int topValue = 10;
    protected int leftValue = 20;
    protected int rightValue = 10;
    protected int bottomValue = 10;
    protected int userSpaceWidth = 1300;

    public static void main(String[] args) {
        try {
            PdfViewerStarter jt = new PdfViewerStarter();
            jt.doConversion("http://old.pd4ml.com/sample.htm", "c:/pd4ml.pdf");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void doConversion(String url, String outputPath)
                throws InvalidParameterException, MalformedURLException, IOException {
        File output = new File(outputPath);
        FileOutputStream fos = new FileOutputStream(output);

        PD4ML pd4ml = new PD4ML();
        pd4ml.setHtmlWidth(userSpaceWidth);
        pd4ml.setPageSize(PageSize.A4.rotate());
        pd4ml.setPageMargins(new PageMargins(topValue, leftValue, bottomValue, rightValue, Units.MM));
        pd4ml.useTTF("c:/windows/fonts", true);

        pd4ml.readHTML(new URL(url));
        pd4ml.writePDF(fos);
        fos.close();

        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().open(output);
        } else {
            System.out.println("Awt Desktop is not supported!");
        }

        System.out.println(outputPath + "\ndone.");
    }
}
```

## PD4ML's own preview path

When the goal is just visually inspecting a conversion during development, rather than opening the result in an end user's own default viewer from a running application, PD4ML's bundled GUI previewer is a more direct option than any of the above: `java -jar pd4ml.jar -gui` (or `Pd4Cmd`'s `-gui` flag from a script) launches `PD4Browser` directly, with its own re-render/preview loop built in.

## See also

* [PD4ML Programmer's Manual](https://app.gitbook.com/s/33dI66uC6sTAcaPjppX2/) -- §5 covers `PD4Browser` in full.
* [Pd4Cmd Command-Line Reference](https://app.gitbook.com/s/33dI66uC6sTAcaPjppX2/pd4cmd-command-line-reference) -- the `-gui` flag referenced above.
