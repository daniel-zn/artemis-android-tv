#!/bin/bash
# tools/banner-from-image.sh [image] [left offset]: makes the Android TV banner from a picture, full-bleed like
# Muses TV's: cropped to 16:9 (the full height; `left offset` = pixels cut from the left, default: cut both sides
# equally) and scaled to res/drawable-xhdpi/atv_banner.png (320 × 180) and res/drawable-xxhdpi/atv_banner.png
# (640 × 360). No name and square corners: the launcher writes the name and rounds the corners, so keep the figure
# inside the central 5:3. Default: art/artemis-banner-wide.jpg (the banner art, 1790 × 940) with 119 px cut from the
# left, which centres the figure and keeps all of it. A picture narrower than 16:9 is cropped top and bottom. Mac (sips).
set -euo pipefail
root="$(cd "$(dirname "$0")/.." && pwd)"; res="$root/app/src/main/res"
in="${1:-$root/art/artemis-banner-wide.jpg}"; off="${2:-}"
[ $# -eq 0 ] && off=119
w=$(sips -g pixelWidth "$in" | awk '/pixelWidth/{print $2}'); h=$(sips -g pixelHeight "$in" | awk '/pixelHeight/{print $2}')
if [ $((w * 9)) -gt $((h * 16)) ]; then cw=$((h * 16 / 9)); ch=$h; x=${off:-$(((w - cw) / 2))}; y=0
else cw=$w; ch=$((w * 9 / 16)); x=0; y=$(((h - ch) / 2)); fi
[ $((x + cw)) -le "$w" ] || { echo "banner-from-image: offset $x leaves less than ${cw}px" >&2; exit 1; }
tmp=$(mktemp -d); trap 'rm -rf "$tmp"' EXIT
sips -s format png -c "$ch" "$cw" --cropOffset "$y" "$x" "$in" --out "$tmp/crop.png" >/dev/null
sips -z 180 320 "$tmp/crop.png" --out "$res/drawable-xhdpi/atv_banner.png" >/dev/null
sips -z 360 640 "$tmp/crop.png" --out "$res/drawable-xxhdpi/atv_banner.png" >/dev/null
echo "banner from $(basename "$in") (${w}×${h}, ${cw}×${ch} from x=$x y=$y): drawable-xhdpi 320×180, drawable-xxhdpi 640×360"
