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
  --ref PATH          Reference image to edit (uses /v1/images/edits multipart)
  --upscale-ref       Upscale --ref to 1024x1024 before uploading (for tiny originals)
  --out PATH          Single output file (skip density split)
  --out-dir DIR       Save resized density copies (see --density)
  --dir-prefix TEXT   Directory prefix: mipmap (default) or drawable
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


def parse_args():
    p = argparse.ArgumentParser(description="ZM Reborn image asset generator")
    p.add_argument("--prompt",      required=True)
    p.add_argument("--model",       default="gpt-image-2")
    p.add_argument("--size",        default="1024x1024")
    p.add_argument("--transparent", action="store_true")
    p.add_argument("--ref",         help="Reference image path for edit endpoint")
    p.add_argument("--upscale-ref", action="store_true",
                   help="Upscale --ref to 1024x1024 before uploading (good for tiny originals)")
    p.add_argument("--out",         help="Single output PNG path")
    p.add_argument("--out-dir",     help="Android res root (saves to {prefix}-*/)")
    p.add_argument("--dir-prefix",  default="mipmap",
                   help="Directory prefix inside out-dir: mipmap (default) or drawable")
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
        "model":   model,
        "prompt":  args.prompt,
        "n":       1,
        "size":    args.size,
        "quality": args.quality,
    }
    if args.transparent:
        payload["background"]    = "transparent"
        payload["output_format"] = "png"

    url  = args.api_base.rstrip("/") + "/v1/images/generations"
    data = json.dumps(payload).encode()
    req  = urllib.request.Request(url, data=data, headers={
        "Content-Type":  "application/json",
        "Authorization": f"Bearer {args.api_key}",
        "x-api-key":     args.api_key,
    }, method="POST")

    print(f"→ POST {url} model={model} size={args.size}", flush=True)
    with urllib.request.urlopen(req, timeout=180) as resp:
        body = json.loads(resp.read())

    if "error" in body:
        print(f"API error: {body['error']}", file=sys.stderr)
        sys.exit(1)

    return base64.b64decode(body["data"][0]["b64_json"])


def _prepare_ref_bytes(ref_path, upscale):
    """Read ref image; optionally upscale to 1024x1024 as PNG bytes."""
    if upscale:
        from PIL import Image
        import io
        img = Image.open(ref_path).convert("RGBA")
        img = img.resize((1024, 1024), Image.LANCZOS)
        buf = io.BytesIO()
        img.save(buf, format="PNG")
        return buf.getvalue()
    with open(ref_path, "rb") as f:
        return f.read()


def call_edit(args, model):
    """POST /v1/images/edits with multipart/form-data."""
    img_bytes = _prepare_ref_bytes(args.ref, args.upscale_ref)

    boundary = b"----ZMRebornBoundary20260726"

    def part_text(name, value):
        return (
            b"--" + boundary + b"\r\n"
            b"Content-Disposition: form-data; name=\"" + name.encode() + b"\"\r\n\r\n"
            + value.encode() + b"\r\n"
        )

    def part_file(name, filename, data, mime=b"image/png"):
        return (
            b"--" + boundary + b"\r\n"
            b"Content-Disposition: form-data; name=\"" + name.encode()
            + b"\"; filename=\"" + filename.encode() + b"\"\r\n"
            b"Content-Type: " + mime + b"\r\n\r\n"
            + data + b"\r\n"
        )

    body = b""
    body += part_text("model",  model)
    body += part_text("prompt", args.prompt)
    body += part_text("n",      "1")
    body += part_text("size",   args.size)
    body += part_file("image",  os.path.basename(args.ref), img_bytes)
    body += b"--" + boundary + b"--\r\n"

    url = args.api_base.rstrip("/") + "/v1/images/edits"
    req = urllib.request.Request(url, data=body, headers={
        "Content-Type":  f"multipart/form-data; boundary={boundary.decode()}",
        "Authorization": f"Bearer {args.api_key}",
        "x-api-key":     args.api_key,
    }, method="POST")

    label = f"ref={args.ref}" + (" (upscaled)" if args.upscale_ref else "")
    print(f"→ POST {url} model={model} {label}", flush=True)
    with urllib.request.urlopen(req, timeout=180) as resp:
        resp_body = json.loads(resp.read())

    if "error" in resp_body:
        print(f"API error: {resp_body['error']}", file=sys.stderr)
        sys.exit(1)

    return base64.b64decode(resp_body["data"][0]["b64_json"])


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
    png_data = call_edit(args, model) if args.ref else call_generate(args, model)
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
        prefix = args.dir_prefix
        for dens in densities:
            px   = DENSITY_SIZES[dens]
            path = os.path.join(args.out_dir, f"{prefix}-{dens}", args.asset_name)
            save(path, resize_png(png_data, px))
        return

    out = args.asset_name.replace(".png", "_source.png")
    save(out, png_data)


if __name__ == "__main__":
    main()
