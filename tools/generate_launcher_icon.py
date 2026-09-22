"""Create Android adaptive launcher assets from the supplied Koto artwork."""

from pathlib import Path
import shutil
import sys

from PIL import Image


CANVAS_SIZE = 432
ARTWORK_SIZE = 316
BACKGROUND = (253, 253, 253, 255)


def extract_artwork(source: Image.Image) -> Image.Image:
    rgb = source.convert("RGB")
    foreground = Image.new("RGBA", rgb.size)
    output = []
    for red, green, blue in rgb.get_flattened_data():
        # The JPEG background is 252–255. This converts only that near-white
        # field and its antialiased boundary into transparency.
        alpha = max(0, min(255, (255 - min(red, green, blue)) * 5))
        output.append((red, green, blue, alpha))
    foreground.putdata(output)

    alpha = foreground.getchannel("A")
    bounds = alpha.point(lambda value: 255 if value > 16 else 0).getbbox()
    if bounds is None:
        raise ValueError("No non-white artwork found in the source image")
    return foreground.crop(bounds)


def fit_on_canvas(artwork: Image.Image) -> Image.Image:
    artwork.thumbnail((ARTWORK_SIZE, ARTWORK_SIZE), Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (CANVAS_SIZE, CANVAS_SIZE), (0, 0, 0, 0))
    left = (CANVAS_SIZE - artwork.width) // 2
    top = (CANVAS_SIZE - artwork.height) // 2
    canvas.alpha_composite(artwork, (left, top))
    return canvas


def main() -> None:
    if len(sys.argv) != 2:
        raise SystemExit("usage: generate_launcher_icon.py SOURCE.jpg")

    root = Path(__file__).resolve().parents[1]
    source = Path(sys.argv[1]).resolve()
    source_copy = root / "artwork" / "launcher" / "koto-icon-source.jpg"
    drawable_dir = root / "app" / "src" / "main" / "res" / "drawable-nodpi"
    source_copy.parent.mkdir(parents=True, exist_ok=True)
    drawable_dir.mkdir(parents=True, exist_ok=True)
    shutil.copyfile(source, source_copy)

    foreground = fit_on_canvas(extract_artwork(Image.open(source)))
    foreground.save(drawable_dir / "ic_launcher_foreground.png", optimize=True)

    alpha = foreground.getchannel("A")
    monochrome = Image.new("RGBA", foreground.size, (0, 0, 0, 0))
    monochrome.paste((0, 0, 0, 255), mask=alpha)
    monochrome.save(drawable_dir / "ic_launcher_monochrome.png", optimize=True)

    preview = Image.new("RGBA", foreground.size, BACKGROUND)
    preview.alpha_composite(foreground)
    preview.save(root / "artwork" / "launcher" / "koto-icon-preview.png", optimize=True)


if __name__ == "__main__":
    main()
