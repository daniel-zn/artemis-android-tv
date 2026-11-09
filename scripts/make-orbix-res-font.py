#!/usr/bin/env python3
"""Makes res/font/orbix.otf (the app-wide theme font) from assets/fonts/Orbix-Regular.otf without its digits, so
Android draws digits in the system font (Orbix's 1 looks like a 7), as ArtemisFont does by hand. Needs fontTools."""
import pathlib, sys
from fontTools.ttLib import TTFont
root = pathlib.Path(__file__).resolve().parent.parent / "app/src/main"
f = TTFont(root / "assets/fonts/Orbix-Regular.otf")
for t in f["cmap"].tables:
    for c in range(ord("0"), ord("9") + 1):
        t.cmap.pop(c, None)
out = root / "res/font/orbix.otf"
f.save(out)
print(out, "digits left:", [chr(c) for c in f.getBestCmap() if chr(c).isdigit()])
