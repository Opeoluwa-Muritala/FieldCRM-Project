"""Upload a signed FieldCRM APK to Cloudinary and emit deployment metadata.

The script never edits a checked-in environment file. Copy the printed values
to the deployment environment after verifying the uploaded release.
"""
from __future__ import annotations

import argparse
import hashlib
import os
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "backend"))


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("apk", type=Path)
    parser.add_argument("--version", required=True)
    parser.add_argument("--released-at", required=True, help="ISO date, for example 2026-08-12")
    args = parser.parse_args()

    apk = args.apk.resolve()
    if not apk.is_file() or apk.suffix.lower() != ".apk":
        raise SystemExit(f"APK not found: {apk}")
    if "release" not in apk.name.lower():
        raise SystemExit("Refusing to publish an APK whose filename does not contain 'release'.")

    from app.config import settings
    if not settings.cloudinary_enabled:
        raise SystemExit("Cloudinary credentials are incomplete.")

    import cloudinary
    import cloudinary.uploader
    cloudinary.config(
        cloud_name=settings.CLOUDINARY_CLOUD_NAME,
        api_key=settings.CLOUDINARY_API_KEY,
        api_secret=settings.CLOUDINARY_API_SECRET,
        secure=True,
    )

    payload = apk.read_bytes()
    digest = hashlib.sha256(payload).hexdigest()
    public_id = f"fieldcrm/releases/fieldcrm-android-{args.version}"
    result = cloudinary.uploader.upload_large(
        str(apk),
        resource_type="raw",
        type="upload",
        public_id=public_id,
        overwrite=True,
        invalidate=True,
        tags=["fieldcrm", "android", "release"],
        context={"version": args.version, "sha256": digest, "released_at": args.released_at},
    )
    url = result.get("secure_url")
    if not url:
        raise SystemExit("Cloudinary did not return a secure URL.")

    print("Release uploaded. Configure these deployment values:")
    print(f"ANDROID_APK_URL={url}")
    print(f"ANDROID_APK_VERSION={args.version}")
    print(f"ANDROID_APK_RELEASED_AT={args.released_at}")
    print(f"ANDROID_APK_SIZE_BYTES={len(payload)}")
    print(f"ANDROID_APK_SHA256={digest}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
