#!/usr/bin/env python3
"""
ZM Reborn image asset generator.
Calls the proxy image API (gpt-image-2 / gpt-image-1.5) and resizes to
Android density buckets.

Usage:
  uv run --with pillow driver.py [OPTIONS]

Options:
  --prompt TEXT       Image generation prompt (required)
  --model TEXT        gpt-image-2 (default) | gpt-image-1.5
  --size WxH          Generation size, default 1024x1024
  --transparent       Use gpt-image-1.5 with transparent background
  --ref PATH          Reference image to edit (uses /v1/images/edits)
  --out PATH          Single output file (skip density split)
  --out-dir DIR       Save resized density copies (see --density)
  --density LIST      Comma-separated: mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi
                      or "all" (default when --out-dir given)
  --asset-name NAME   Filename inside density dirs (default: ic_launcher.png)
  --api-base URL      Proxy base URL (default: ANTHROPIC_BASE_URL or hardcoded)
  --api-key KEY       API key (default: ANTHROPIC_API_KEY)
  --quality VALUE     low|medium|high|auto (gpt-image-2 only, default: high)
"""

import argparse
import base64
import json
import os
import sys
import urllib.request

DENSITY_SIZES = {
    "mdpi":    48,
    "hdpi":    72,
    "xhdpi":   96,
    "xxhdpi":  144,
    "xxxhdpi": 192,
}

PROXY_DEFAULT = "https://api.alanwo.com.br"
MIPMAP_ROOT   = "app/src/main/res"


def parse_args():
    p = argparse.ArgumentParser(description="ZM Reborn image asset generator")
    p.add_argument("--prompt",      required=True)
    p.add_argument("--model",       default="gpt-image-2")
    p.add_argument("--size",        default="1024x1024")
    p.add_argument("--transparent", action="store_true")
    p.add_argument("--ref",         help="Reference image for edits")
    p.add_argument("--out",         help="Single output PNG path")
    p.add_argument("--out-dir",     help="Android res root (saves to mipmap-*/)")
    p.add_argument("--density",     default="all")
    p.add_argument("--asset-name",  default="ic_launcher.png")
    p.add_argument("--api-base",    default=os.environ.get("ANTHROPIC_BASE_URL", PROXY_DEFAULT))
    p.add_argument("--api-key",     default=os.environ.get("ANTHROPIC_API_KEY", ""))
    p.add_argument("--quality",     default="high")
    return p.parse_args()


def pick_model(args):
    if args.transparent:
        return "gpt-image-1.5"
    return args.model


def call_generate(args, model):
    payload = {
        "model":  model,
        "prompt": args.prompt,
        "n":      1,
        "size":   args.size,
        "quality": args.quality,
    }
    if args.transparent:
        payload["background"]     = "transparent"
        payload["output_format"]  = "png"

    url  = args.api_base.rstrip("/") + "/v1/images/generations"
    data = json.dumps(payload).encode()
    req  = urllib.request.Request(url, data=data, headers={
        "Content-Type":  "application/json",
        "Authorization": f"Bearer {args.api_key}",
        "x-api-key":     args.api_key,
    }, method="POST")

    print(f"→ POST {url} model={model} size={args.size}", flush=True)
    with urllib.request.urlopen(req, timeout=120) as resp:
        body = json.loads(resp.read())

    if "error" in body:
        print(f"API error: {body['error']}", file=sys.stderr)
        sys.exit(1)

    b64 = body["data"][0]["b64_json"]
    return base64.b64decode(b64)


def resize_png(png_bytes, target_px):
    from PIL import Image
    import io
    img = Image.open(io.BytesIO(png_bytes)).convert("RGBA")
    resized = img.resize((target_px, target_px), Image.LANCZOS)
    buf = io.BytesIO()
    resized.save(buf, format="PNG", optimize=True)
    return buf.getvalue()


def save(path, data):
    os.makedirs(os.path.dirname(path) or ".", exist_ok=True)
    with open(path, "wb") as f:
        f.write(data)
    print(f"  saved {path} ({len(data):,}b)", flush=True)


def main():
    args = parse_args()

    if not args.api_key:
        print("Error: set ANTHROPIC_API_KEY or pass --api-key", file=sys.stderr)
        sys.exit(1)

    model    = pick_model(args)
    png_data = call_generate(args, model)
    print(f"  received {len(png_data):,} bytes", flush=True)

    if args.out:
        save(args.out, png_data)
        return

    if args.out_dir:
        densities = (
            list(DENSITY_SIZES.keys())
            if args.density == "all"
            else [d.strip() for d in args.density.split(",")]
        )
        for dens in densities:
            px   = DENSITY_SIZES[dens]
            path = os.path.join(args.out_dir, f"mipmap-{dens}", args.asset_name)
            save(path, resize_png(png_data, px))
        return

    # Default: save source-size PNG next to script
    out = args.asset_name.replace(".png", "_source.png")
    save(out, png_data)


if __name__ == "__main__":
    main()
