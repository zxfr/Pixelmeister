# SVG to PDF Conversion

PD4ML has parsed and rendered embedded SVG vector graphics since v3.8.5, covering the basic SVG shape model, matrix transforms, transparency, and stroke/fill styling. SVG can appear in a source document either referenced through an ordinary `<img>` tag or written inline as raw `<svg>` markup, and either form converts as part of the surrounding page like any other content.

{% hint style="info" %}
This page was checked directly against the current PD4ML source (the `com.pd4ml.svg.parser` package, and its integration points in the main HTML layout/paint pipeline) rather than transcribed from older documentation as-is. The underlying SVG support is substantial and clearly still actively maintained, but two things have changed since the original version of this page: DOCX output (added after this page was first written) embeds SVG natively as vector graphics, and there's a newer, currently experimental rendering engine, unrelated to normal usage, that doesn't implement SVG yet -- both covered below.
{% endhint %}

## Referencing vs. inlining SVG

An external SVG file behaves like any other image reference:

```html
<img align=right border="1" src="chart.svg" width="300" style="background-color: tomato; page-break-inside: avoid">
```

Or the SVG markup can be embedded directly in the HTML source, mixed with ordinary content:

```html
<svg width="200" height="150" xmlns="http://www.w3.org/2000/svg" version="1.1">
  <g fill="none" stroke="black" stroke-width="4">
    <path stroke-dasharray="5,5" d="M5 20 l215 0" />
    <path fill="none" stroke-dasharray="10,10" d="M5 40 l215 0" />
    <path stroke-dasharray="20,10,5,5,5,10" d="M5 60 l215 0" />
  </g>
</svg>
```

## Two rendering backends

Not documented on the original version of this page: PD4ML actually ships two independent SVG renderers. If [Apache Batik](https://xmlgraphics.apache.org/batik/) is present on the classpath, PD4ML uses it first, for Batik's broader SVG specification coverage; if Batik isn't available (or has been explicitly disabled), PD4ML falls back to its own bundled, lightweight SVG parser -- the one the support table below describes. Setting `PD4ML.disableBatik = true` forces the bundled parser even when Batik is on the classpath, which is worth doing deliberately for a smaller deployment footprint if Batik's extra coverage isn't needed.

## What's supported

| SVG element                                        | Support                                                                                                      |
| -------------------------------------------------- | ------------------------------------------------------------------------------------------------------------ |
| `<svg>`                                            | Supported; nested `<svg>` elements are not.                                                                  |
| `<style>`                                          | Supported; some complex CSS selector types aren't yet.                                                       |
| `<text>`                                           | Supported                                                                                                    |
| `<tspan>`                                          | Partially supported                                                                                          |
| `<defs>`                                           | Supported                                                                                                    |
| `<g>`                                              | Supported                                                                                                    |
| `<symbol>`                                         | Partially supported                                                                                          |
| `<use>`                                            | Supported                                                                                                    |
| `<rect>`                                           | Supported                                                                                                    |
| `<line>`                                           | Supported                                                                                                    |
| `<circle>`                                         | Supported                                                                                                    |
| `<ellipse>`                                        | Supported                                                                                                    |
| `<polygon>`                                        | Supported                                                                                                    |
| `<polyline>`                                       | Supported                                                                                                    |
| `<path>`                                           | Supported                                                                                                    |
| `<title>`                                          | Ignored                                                                                                      |
| `<desc>`                                           | Ignored                                                                                                      |
| `<metadata>`                                       | Ignored                                                                                                      |
| `<linearGradient>` / `<radialGradient>` / `<stop>` | Partially supported: gradients render when converting to a raster image, but not yet when converting to PDF. |
| `<pattern>`                                        | Ignored                                                                                                      |
| `<clipPath>`                                       | Supported                                                                                                    |

This table describes the bundled parser specifically; the Batik backend, when active, follows Batik's own (considerably broader) SVG support instead.

## Output-format notes

* **PDF** -- the primary, fully supported target, as above.
* **DOCX** -- added to PD4ML after this page was originally written, and worth calling out specifically: PD4ML embeds SVG images in a DOCX output as genuine vector graphics, via the same `asvg:svgBlip` DrawingML extension Microsoft Office itself uses (Office 2016+), rather than rasterizing them first.
* **RTF** -- the original caveat here ("HTML+SVG-to-RTF conversion is planned for later") doesn't appear to have been superseded by anything comparable to the DOCX support above; treat SVG content in RTF output as not guaranteed to survive as vector graphics.
* **Raster images** (`renderAsImages()`) -- supported under PD4ML's default rendering path, the same one used for PDF/RTF/DOCX. Separately, a newer rendering engine exists that's opted into explicitly via `useHtmlRenderer(Constants.LayoutEngine.v42, ...)`; as of this writing it doesn't render SVG content at all (silently skipping it, the same way any other unsupported element would be) and doesn't yet implement PDF/RTF/DOCX output either -- relevant only to a codebase that has deliberately selected it, not to ordinary `renderAsImages()` usage.

## Examples

Two worked examples from the original cookbook page, still hosted on `old.pd4ml.com`:

* [Source HTML](http://old.pd4ml.com/i/charts.htm) ([as text](http://old.pd4ml.com/i/charts.txt)) and its [PDF result](http://old.pd4ml.com/i/charts.pdf) -- a set of SVG charts.
* [Source HTML](http://old.pd4ml.com/i/svgsource.htm) ([as text](http://old.pd4ml.com/i/svgsource.txt)) and its [PDF result](http://old.pd4ml.com/i/svgresult.pdf) -- a broader tour of individual SVG elements.

## See also

* [HTML to Raster Image Conversion](html-to-raster-image-conversion.md) -- more on `renderAsImages()`, including where the experimental v42 engine mentioned above would come into play.
* [RTF and DOCX Output](rtf-and-docx-output.md) -- the DOCX/RTF output paths SVG content flows through, described above.
