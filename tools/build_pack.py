"""Zip pack/ into packs/GeyserDisplayEntityPack.mcpack, with fixed timestamps so unchanged content zips identically.

The manifest version is taken from a hash of the content, so Bedrock clients download the pack again when it changes.

Run tools/block_renderer/generate.py first: it writes the block renderer's textures and generated pack files.
"""
import hashlib
import json
import zipfile
from pathlib import Path

root = Path(__file__).resolve().parents[1]
pack = root / "pack"
out = root / "packs/GeyserDisplayEntityPack.mcpack"
files = sorted(p for p in pack.rglob("*") if p.is_file())

content = hashlib.sha256()
for path in files:
    if path.name == "manifest.json" and path.parent == pack:
        continue
    content.update(path.relative_to(pack).as_posix().encode())
    content.update(path.read_bytes())
digest = content.hexdigest()
version = [1, int(digest[:8], 16) % 1000, int(digest[8:16], 16) % 1000]

with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED, compresslevel=9) as z:
    for path in files:
        data = path.read_bytes()
        if path.name == "manifest.json" and path.parent == pack:
            manifest = json.loads(data)
            manifest["header"]["version"] = version
            for module in manifest["modules"]:
                module["version"] = version
            data = json.dumps(manifest, indent=4).encode()
        info = zipfile.ZipInfo(path.relative_to(pack).as_posix(), date_time=(2024, 1, 1, 0, 0, 0))
        info.compress_type = zipfile.ZIP_DEFLATED
        z.writestr(info, data)
print(f"{out} ({out.stat().st_size // 1024} KiB, version {'.'.join(map(str, version))})")
