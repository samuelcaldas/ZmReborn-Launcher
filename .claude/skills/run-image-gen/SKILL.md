---
name: run-image-gen
description: generate, recreate, and resize Android image assets using gpt-image-2 or gpt-image-1.5 via the configured proxy; resize to all Android density buckets; use codex exec as alternative for interactive or batch generation
---

# ZM Reborn Image Asset Generator

Generates or recreates PNG image assets by calling the proxy image API
(`api.alanwo.com.br/v1/images/generations`) with `gpt-image-2` (default)
or `gpt-image-1.5` (transparent background), then resizes to Android density
buckets.

Driver: `.claude/skills/run-image-gen/driver.py`
Runtime: `uv run --with pillow` (no separate install step needed)
Auth: `ANTHROPIC_API_KEY` from environment (pre-configured via codex shell env)

## Prerequisites

```bash
# Already installed in this environment:
which uv            # /home/samuelcaldas/.local/bin/uv
which codex         # ~/.nvm/versions/node/.../bin/codex
which claude        # ~/.local/bin/claude
echo $ANTHROPIC_API_KEY   # non-empty (set by codex shell env)
echo $ANTHROPIC_BASE_URL  # https://api.alanwo.com.br
```

No manual install needed. `uv run --with pillow` fetches Pillow on first use.

## Run: driver (agent path)

All paths are relative to the repo root `/home/samuelcaldas/repos/zeam`.

### Generate launcher icons at all Android densities

```bash
uv run --with pillow .claude/skills/run-image-gen/driver.py \
  --prompt "Android launcher icon: dark slate (#121A21) background with rounded corners, bold amber (#F2B64A) letter Z centered, clean flat design, ZM Reborn brand" \
  --model gpt-image-2 \
  --quality high \
  --out-dir app/src/main/res \
  --density all \
  --asset-name ic_launcher.png
```

Output: `app/src/main/res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher.png`
Sizes: 48 / 72 / 96 / 144 / 192 px

### Generate single asset at source size

```bash
uv run --with pillow .claude/skills/run-image-gen/driver.py \
  --prompt "DESCRIBE WHAT TO GENERATE" \
  --model gpt-image-2 \
  --size 1024x1024 \
  --out docs/branding/my_asset.png
```

### Generate transparent foreground (adaptive icon / overlay)

Use `gpt-image-1.5` when the asset needs no background (adaptive icon layer,
foreground-only sprite, etc.):

```bash
uv run --with pillow .claude/skills/run-image-gen/driver.py \
  --prompt "amber (#F2B64A) letter Z bold flat geometric, isolated on transparent background" \
  --transparent \
  --out docs/branding/ic_launcher_fg.png
```

Note: the proxy returns RGB (white background) rather than true RGBA for
`--transparent`. Post-process with the chroma-key script if RGBA is required:

```bash
python3 "${CODEX_HOME:-$HOME/.codex}/skills/.system/imagegen/scripts/remove_chroma_key.py" \
  --input docs/branding/ic_launcher_fg.png \
  --out   docs/branding/ic_launcher_fg_alpha.png \
  --auto-key border --soft-matte --transparent-threshold 12 --opaque-threshold 220 --despill
```

### Enhance prompt with Gemini before generation

Use `claude -p --model gemini-3.1-flash-image` to expand a terse description
into a detailed image prompt, then pipe to the driver:

```bash
ENHANCED=$(claude --bare -p --model gemini-3.1-flash-image \
  "Write a detailed image generation prompt (1-2 sentences) for: ZM Reborn launcher icon, dark slate background, amber Z. Return only the prompt text.")
uv run --with pillow .claude/skills/run-image-gen/driver.py \
  --prompt "$ENHANCED" \
  --model gpt-image-2 \
  --out /tmp/enhanced.png
```

### Recreate or edit an existing asset

Reference an existing file (passed as description context; direct image edits
via `/v1/images/edits` require a multipart upload not yet in the driver — use
a descriptive prompt that references the existing asset instead):

```bash
uv run --with pillow .claude/skills/run-image-gen/driver.py \
  --prompt "Recreate this launcher icon but replace the background with Glass (#D9121A21): bold amber Z on translucent slate, same rounded-corner shape" \
  --model gpt-image-2 \
  --out docs/branding/ic_launcher_glass.png
```

## Run: codex exec (alternative)

`codex exec` invokes the built-in `image_gen` tool and does not require
`ANTHROPIC_API_KEY`. Use this for interactive or batch sessions where you
want the agent to choose prompts and output paths itself.

```bash
codex exec \
  -c 'approval_policy="never"' \
  "Generate an Android launcher icon (192x192 px) for ZM Reborn: dark slate (#121A21) background, amber (#F2B64A) Z bold centered. Save PNG to docs/branding/ic_launcher_codex.png."
```

Warning: codex exec starts an agentic loop and may take 2-5 minutes.
Use the driver script for deterministic, fast generation.

## Driver CLI reference

```
--prompt TEXT       Required. Image description sent to the API.
--model TEXT        gpt-image-2 (default, opaque) | gpt-image-1.5 (transparent).
--size WxH          API generation size. Default: 1024x1024.
--transparent       Switch to gpt-image-1.5 and request transparent background.
--out PATH          Single output file. Skips density resize.
--out-dir DIR       Android res root. Writes mipmap-*/ASSET_NAME.
--density LIST      Comma-separated density names or "all" (default with --out-dir).
--asset-name NAME   Filename inside density dirs. Default: ic_launcher.png.
--quality VALUE     low|medium|high|auto (gpt-image-2 only). Default: high.
--api-base URL      Proxy base. Default: $ANTHROPIC_BASE_URL.
--api-key KEY       API key. Default: $ANTHROPIC_API_KEY.
```

## Android density size table

| Density   | px  | Path                              |
|-----------|-----|-----------------------------------|
| mdpi      | 48  | `app/src/main/res/mipmap-mdpi/`   |
| hdpi      | 72  | `app/src/main/res/mipmap-hdpi/`   |
| xhdpi     | 96  | `app/src/main/res/mipmap-xhdpi/`  |
| xxhdpi    | 144 | `app/src/main/res/mipmap-xxhdpi/` |
| xxxhdpi   | 192 | `app/src/main/res/mipmap-xxxhdpi/`|

## Gotchas

- **Proxy routes `gpt-image-2` and `gpt-image-1.5` to `/v1/images/generations` only.**
  Passing these models to `/v1/messages` returns an error. The driver uses the
  correct endpoint automatically.

- **`gemini-3.1-flash-image` is not supported on the image endpoint** through
  this proxy. It only answers text prompts via `/v1/messages`. Use it for
  prompt enhancement (see above), not image generation.

- **`claude -p --model gpt-image-2`** will time out or hang — claude CLI uses
  `/v1/messages`, which rejects image models. Use the driver instead.

- **Transparent mode returns RGB, not RGBA** through the proxy even with
  `background=transparent`. The generated image has a white background.
  Use the codex chroma-key script to extract the foreground if needed.

- **Generated size may differ from requested.** `gpt-image-2` returned 1254×1254
  when 1024×1024 was requested. The driver always resizes to exact density px.

- **Pillow is not installed system-wide.** Always prefix with `uv run --with pillow`.
  Adding `#!/usr/bin/env python3` and running directly will fail with
  `ModuleNotFoundError: No module named 'PIL'`.

- **`codex exec` times out at 90 s** in restricted environments. Give it at
  least 300 s, or use the driver for deterministic execution.

## Troubleshooting

| Symptom | Fix |
|---|---|
| `API error: model gpt-image-2 is only supported on /v1/images/generations` | You called `/v1/messages` directly. Use the driver, not `curl` to the messages endpoint. |
| `No module named 'PIL'` | Prefix with `uv run --with pillow`. |
| Empty output / no file saved | Check `$ANTHROPIC_API_KEY` is set and non-empty. |
| Generated image is too large (>1MB) | Normal for `gpt-image-2` at `quality=high`. The driver resizes before saving to Android paths. |
| `--transparent` output has white background | Proxy limitation. Apply chroma-key removal script (see above). |
