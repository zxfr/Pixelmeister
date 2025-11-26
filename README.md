# Pixelmeister

`Pixelmeister` is a platform-independent tool designed to simplify the development of graphical user interfaces for microelectronic devices. It currently emulates the `Pixels` and `UTFT` graphics libraries (including `uText`).
`Pixelmeister` helps simulate user interfaces without the need to download and test the code on the target device after each UI customization step. This tool can also convert `TTF`/`OTF` images, icons, and fonts into a very compact format suitable for use in microcontroller programs.

![Image](i/pixelmeister14.png "icon")

![Image](i/pixelmeister17.jpg "icon")

## Installation

It will take some time for the `Pixelmester` installer generators and online update repository to be revived.

Therefore, the current available installation method requires pre-installing the `Eclipse RCP` environment and downloading and running `Pixelmester` as a regular project.

## Features

You can find a slightly updated Arduino-centric user guide [at this link](i/MANUAL.md). The ability to export a ready-to-compile Arduino sketch is probably not particularly useful these days, given the many alternative RAD tools available to developers that are superior to the native Arduino IDE, which is unfriendly to complex projects.

But the ability to copy generated code snippets and imported resources and paste them into the RAD of your choice makes this tool a useful addition to any development environment.

## Tools

### Image import

[Read about the image import](i/IMAGES.md)

### True Type Font Import

[Read about the font import](i/FONTS.md)

## uText Library

The current state of the library can be found at [this link](https://github.com/zxfr/uText).
The library can be easily ported to any platform that supports horizontal and vertical line rendering.

### uText API

[Read about uText Library](i/MANUAL.md#utext)

