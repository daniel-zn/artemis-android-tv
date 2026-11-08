// Makes the TV app's launcher icon and banner from the iPhone app's Icon Composer icon
// (artemis-ios/Artemis/AppIcon.icon), on a Mac with Xcode (Icon Composer's ictool). The PNGs are committed, so the
// Android build doesn't need this. From the repository root:
//   xcrun --sdk macosx swiftc -O tools/icon-images.swift -o /tmp/icon-images && /tmp/icon-images [path/to/AppIcon.icon]
// Writes:
//   res/mipmap-*/ic_launcher.png             the icon as Icon Composer renders it (its rounded square), 48–192 px
//   res/mipmap-*/ic_launcher_foreground.png  the adaptive icon's art: its layers, unglassed, on transparent
//   res/drawable-xhdpi/atv_banner.png        the Android TV banner, 320 × 180: the icon and "Artemis" in Orbix
//   res/drawable-xxhdpi/atv_banner.png       the same, 640 × 360
import AppKit
import CoreText

let root = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent()
let icon = CommandLine.arguments.count > 1 ? URL(fileURLWithPath: CommandLine.arguments[1])
    : root.deletingLastPathComponent().appendingPathComponent("artemis-ios/Artemis/AppIcon.icon")
let res = root.appendingPathComponent("app/src/main/res")

func fail(_ message: String) -> Never { FileHandle.standardError.write("error: icon-images: \(message)\n".data(using: .utf8)!); exit(1) }

func ictool() -> String {
    let select = Process(); let pipe = Pipe()
    select.executableURL = URL(fileURLWithPath: "/usr/bin/xcode-select"); select.arguments = ["-p"]; select.standardOutput = pipe
    try? select.run(); select.waitUntilExit()
    let developer = String(data: pipe.fileHandleForReading.readDataToEndOfFile(), encoding: .utf8)?
        .trimmingCharacters(in: .whitespacesAndNewlines) ?? "/Applications/Xcode.app/Contents/Developer"
    let path = URL(fileURLWithPath: developer).deletingLastPathComponent()
        .appendingPathComponent("Applications/Icon Composer.app/Contents/Executables/ictool").path
    guard FileManager.default.isExecutableFile(atPath: path) else { fail("no ictool at \(path)") }
    return path
}

func context(_ w: Int, _ h: Int) -> CGContext {
    let c = CGContext(data: nil, width: w, height: h, bitsPerComponent: 8, bytesPerRow: w * 4,
                      space: CGColorSpace(name: CGColorSpace.sRGB)!, bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
    c.interpolationQuality = .high
    return c
}

func load(_ url: URL) -> CGImage {
    guard let s = CGImageSourceCreateWithURL(url as CFURL, nil), let i = CGImageSourceCreateImageAtIndex(s, 0, nil) else { fail("can't read \(url.path)") }
    return i
}

func render(size: Int) -> CGImage {
    let out = FileManager.default.temporaryDirectory.appendingPathComponent("artemis-icon-\(UUID().uuidString).png")
    defer { try? FileManager.default.removeItem(at: out) }
    let tool = Process()
    tool.executableURL = URL(fileURLWithPath: ictool())
    tool.arguments = [icon.path, "--export-image", "--output-file", out.path, "--platform", "iOS", "--rendition", "Default",
                      "--width", "\(size)", "--height", "\(size)", "--scale", "1"]
    tool.standardOutput = FileHandle.nullDevice
    do { try tool.run() } catch { fail("ictool: \(error)") }
    tool.waitUntilExit()
    guard tool.terminationStatus == 0 else { fail("ictool couldn't render the icon") }
    let i = load(out)
    let c = context(size, size); c.draw(i, in: CGRect(x: 0, y: 0, width: size, height: size))
    return c.makeImage()!
}

func write(_ image: CGImage, _ url: URL) {
    try? FileManager.default.createDirectory(at: url.deletingLastPathComponent(), withIntermediateDirectories: true)
    guard let d = CGImageDestinationCreateWithURL(url as CFURL, "public.png" as CFString, 1, nil) else { fail("can't write \(url.path)") }
    CGImageDestinationAddImage(d, image, nil)
    guard CGImageDestinationFinalize(d) else { fail("can't write \(url.path)") }
}

let rendered = render(size: 1024)
// Back to front: the hair and dress, the figure, the gold linework (icon.json lists the front first).
let layers = ["back.png", "figure.png", "gold.png"].map { load(icon.appendingPathComponent("Assets/\($0)")) }
let densities: [(String, Double)] = [("mdpi", 1), ("hdpi", 1.5), ("xhdpi", 2), ("xxhdpi", 3), ("xxxhdpi", 4)]
for (name, scale) in densities {
    let px = Int(48 * scale)
    let c = context(px, px); c.draw(rendered, in: CGRect(x: 0, y: 0, width: px, height: px))
    write(c.makeImage()!, res.appendingPathComponent("mipmap-\(name)/ic_launcher.png"))
    // Adaptive icon: 108 dp canvas; the art (square, edge to edge like the iPhone icon) is 90 dp, so the launcher's
    // 72 dp mask shows its middle 80 % and its cut edges stay hidden.
    let fg = Int(108 * scale), art = 90 * scale, inset = (Double(fg) - art) / 2
    let f = context(fg, fg)
    for l in layers { f.draw(l, in: CGRect(x: inset, y: inset, width: art, height: art)) }
    write(f.makeImage()!, res.appendingPathComponent("mipmap-\(name)/ic_launcher_foreground.png"))
}

// The banner: navy, the icon on the left, "Artemis" in Orbix gold.
let font: CTFont = {
    let url = root.appendingPathComponent("app/src/main/assets/fonts/Orbix-Regular.otf")
    guard let descs = CTFontManagerCreateFontDescriptorsFromURL(url as CFURL) as? [CTFontDescriptor], let d = descs.first else { fail("no Orbix") }
    return CTFontCreateWithFontDescriptor(d, 10, nil)
}()
for (dir, scale) in [("drawable-xhdpi", 1.0), ("drawable-xxhdpi", 2.0)] {
    let w = Int(320 * scale), h = Int(180 * scale)
    let c = context(w, h)
    c.setFillColor(CGColor(srgbRed: 0.04706, green: 0.17647, blue: 0.35294, alpha: 1))
    c.fill(CGRect(x: 0, y: 0, width: w, height: h))
    let side = 112 * scale, x0 = 22 * scale
    c.draw(rendered, in: CGRect(x: x0, y: (Double(h) - side) / 2, width: side, height: side))
    let f = CTFontCreateCopyWithAttributes(font, 40 * scale, nil, nil)
    let attrs: [NSAttributedString.Key: Any] = [.font: f, .foregroundColor: CGColor(srgbRed: 0.909, green: 0.722, blue: 0.290, alpha: 1)]
    let line = CTLineCreateWithAttributedString(NSAttributedString(string: "Artemis", attributes: attrs))
    let b = CTLineGetBoundsWithOptions(line, .useGlyphPathBounds)
    let left = x0 + side + 14 * scale, room = Double(w) - left - 14 * scale
    let fit = min(1, room / b.width)
    c.saveGState()
    c.translateBy(x: left + (room - b.width * fit) / 2 - b.minX * fit, y: Double(h) / 2 - (b.midY) * fit)
    c.scaleBy(x: fit, y: fit)
    c.textPosition = .zero
    CTLineDraw(line, c)
    c.restoreGState()
    write(c.makeImage()!, res.appendingPathComponent("\(dir)/atv_banner.png"))
}
print("icon-images: ic_launcher, ic_launcher_foreground and atv_banner from \(icon.path)")
