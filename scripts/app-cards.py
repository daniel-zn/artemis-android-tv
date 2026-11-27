"""Checks the app grid's cards in a screencap of ArtemisAppGridProbe (emulator-check.sh): cyan Steam art, magenta
Virtual Desktop art (running, focused). Prints PASS/FAIL lines; exits with the number of failures.
  python3 -I scripts/app-cards.py screen.raw   (adb exec-out screencap, raw RGBA)"""
import struct
import sys

d = open(sys.argv[1], 'rb').read()
w, h, _ = struct.unpack('<III', d[:12])
off = 16 if len(d) - 16 == w * h * 4 else 12


def px(x, y):
    i = off + (y * w + x) * 4
    return d[i], d[i + 1], d[i + 2]


def magenta(p): return p[0] > 200 and p[1] < 80 and p[2] > 200
def cyan(p): return p[0] < 80 and p[1] > 200 and p[2] > 200
def gold(p): return abs(p[0] - 0xE8) < 40 and abs(p[1] - 0xB8) < 40 and abs(p[2] - 0x4A) < 50
def background(p): return p[0] < 110 and p[1] < 110  # the navy screen and tiles


def bbox(is_art):
    xs, ys = [], []
    for y in range(0, h, 2):
        for x in range(0, w, 2):
            if is_art(px(x, y)):
                xs.append(x); ys.append(y)
    if not xs:
        return None
    x1, y1, x2, y2 = min(xs), min(ys), max(xs), max(ys)
    # refine to the exact edge (sampled every 2 px)
    while x1 > 0 and any(is_art(px(x1 - 1, y)) for y in range(y1, y2 + 1)): x1 -= 1
    while y1 > 0 and any(is_art(px(x, y1 - 1)) for x in range(x1, x2 + 1)): y1 -= 1
    while x2 < w - 1 and any(is_art(px(x2 + 1, y)) for y in range(y1, y2 + 1)): x2 += 1
    while y2 < h - 1 and any(is_art(px(x, y2 + 1)) for x in range(x1, x2 + 1)): y2 += 1
    return x1, y1, x2, y2


fails = 0


def result(ok, name, detail=''):
    global fails
    print(('PASS ' if ok else 'FAIL ') + name + (' (%s)' % detail if detail else ''))
    fails += 0 if ok else 1


for label, is_art in (('the focused, running card', magenta), ('the unfocused card', cyan)):
    b = bbox(is_art)
    if b is None:
        result(False, label + ' shows its art', 'no art on screen')
        continue
    x1, y1, x2, y2 = b
    cw, ch = x2 - x1 + 1, y2 - y1 + 1
    # Rounded: the art's square corners are clipped away (one radius for art and ring)
    corners = [(x1 + 1, y1 + 1), (x2 - 1, y1 + 1), (x1 + 1, y2 - 1), (x2 - 1, y2 - 1)]
    square = sum(is_art(px(x, y)) for x, y in corners)
    result(square == 0, label + "'s art has rounded corners", '%d of 4 corners square, art %dx%d at %d,%d' % (square, cw, ch, x1, y1))
    # Nothing lies over the artwork (the running mark sits outside the card)
    m = max(4, cw * 15 // 100)
    foreign = sum(1 for y in range(y1 + m, y2 - m) for x in range(x1 + m, x2 - m) if not is_art(px(x, y)))
    result(foreign == 0, 'nothing covers ' + label + "'s art", '%d other pixels inside it' % foreign)
    ring = sum(1 for y in range(max(0, y1 - 30), min(h, y2 + 30)) for x in (x1 - 2, x2 + 2) if 0 <= x < w and gold(px(x, y)))
    if is_art is magenta:
        result(ring > ch // 2, label + ' wears the gold ring', '%d gold pixels beside it' % ring)
        # The ring hugs the card: no background between the ring and the art, left or right, mid-height
        y = (y1 + y2) // 2
        gaps = []
        for start, step, edge in ((x1 - 1, -1, x1), (x2 + 1, 1, x2)):
            x = start
            while 0 <= x < w and abs(x - edge) < 80 and not gold(px(x, y)):
                x += step
            gaps.append(sum(1 for xx in range(min(x, edge), max(x, edge)) if background(px(xx, y))) if 0 <= x < w and abs(x - edge) < 80 else -1)
        result(gaps == [0, 0], 'the ring hugs ' + label, 'background pixels between ring and art, left/right: %s' % gaps)
    else:
        result(ring == 0, label + ' has no ring', '%d gold pixels beside it' % ring)

# The running mark: something that's neither art, gold nor background, just under the magenta card
b = bbox(magenta)
if b:
    x1, y1, x2, y2 = b
    below = sum(1 for y in range(y2 + 4, min(h, y2 + (y2 - y1) // 6)) for x in range(x1, x2)
                if not (magenta(px(x, y)) or gold(px(x, y)) or background(px(x, y))))
    result(below > 20, 'the running app is marked under its card', '%d mark pixels' % below)
sys.exit(fails)
