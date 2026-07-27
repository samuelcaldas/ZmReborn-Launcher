# ZM Reborn launcher icon

## Design direction

Geometric interlocking **Z/M monogram**. The broad amber construction gives the
launcher a strong silhouette at 48 × 48 px; the ember ribbon makes the M leg
cross the Z structure without relying on text glyphs. The icon uses no
gradients, shadows, mascots, external logos, fonts, or trademark-derived style.

## Design specification

- Canvas: 1024 × 1024 normalized units.
- Field: rounded square at `(64, 64)`, size `896 × 896`, corner diameter `184`.
  Pixels outside the field are transparent.
- Safe zone: 12% (`122.88` units) on every canvas edge. The monogram remains
  inside it, spanning x `176–848` and y `232–792`.
- Main Z/M geometry: editable polygons recorded identically in the SVG and Java
  generator.
- Ember crossing: polygon spanning x `510–848` and y `300–610`.
- Palette:
  - Dark slate field: `#121A21`
  - Amber main mark: `#F2B64A`
  - Ember accent: `#D95C4F`
  - Fog highlight (reserved, not used): `#EAF0F3`
- Exports: source `1024 × 1024`; Android mdpi `48 × 48`, hdpi `72 × 72`,
  xhdpi `96 × 96`, xxhdpi `144 × 144`, xxxhdpi `192 × 192`.
- Android resource name remains `ic_launcher`; exports are plain RGBA PNG
  bitmaps compatible with API 8.

## Deterministic generation

Created with Codex CLI `0.145.0` on `2026-07-26`. From the repository root:

```sh
build_directory="$(mktemp -d)"
javac -d "$build_directory" tools/brand/GenerateLauncherIcons.java
java -cp "$build_directory" GenerateLauncherIcons
java -cp "$build_directory" GenerateLauncherIcons --verify
```

The JDK 17 standard-library generator uses fixed Java2D paths, four-by-four
supersampling, integer channel rounding, and a metadata-free PNG stream
containing only `IHDR`, `IDAT`, and `IEND`. Verify mode regenerates every output
in memory and byte-compares it without writing.

No external source assets were used. This is deterministic geometry and byte
encoding, not a seed/model reproducibility claim.

## SHA-256

```text
a8b62182317d27a8b981bd15aeb5610134469d009ef0b4c1a1b8518896aea69d  docs/branding/zm-reborn-icon.svg
fd13c11805f650cf1ecff6ea7180e0280da10c3b2ced3d4cfd2649ff983723f1  docs/branding/zm-reborn-icon-source.png
42f8803b820e99b3f70ce444553abaeb3dd2a226741f3c1fb31e36f44f7611bc  app/src/main/res/drawable-mdpi/ic_launcher.png
857f1b16ba35362f33352bedd9e3763e162280275aeb2be76ea1b023278fca99  app/src/main/res/drawable-hdpi/ic_launcher.png
d45a7a235621dc57ecb0e76f41c9a5fb7e2fa6b6c212e3fd53aa070fcc901fc2  app/src/main/res/drawable-xhdpi/ic_launcher.png
31b4304e8bf1c417bd862f636d6309489345a64474f9dd5a736d8fa59b278cbf  app/src/main/res/drawable-xxhdpi/ic_launcher.png
419eb6ed5a368256a24d945ba7b6e4598428ba0bfc393f600adade9b492a582d  app/src/main/res/drawable-xxxhdpi/ic_launcher.png
```
