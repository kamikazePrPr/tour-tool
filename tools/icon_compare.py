"""对比几种字体和处理方式，挑一个最接近「粗圆体」的效果。

只用来出对比图，不进 App。
"""
from PIL import Image, ImageDraw, ImageFont, ImageFilter
import os

S = 512
BLUE = (31, 90, 166)
CHARS = "旅游登记"
CENTERS = [(0.325, 0.325), (0.675, 0.325), (0.325, 0.675), (0.675, 0.675)]
FONTS = r"C:\Windows\Fonts"


def render(font_path: str, ratio: float, dilate: int, round_px: float) -> Image.Image:
    mask = Image.new("L", (S, S), 0)
    d = ImageDraw.Draw(mask)
    font = ImageFont.truetype(font_path, int(S * ratio))
    for ch, (rx, ry) in zip(CHARS, CENTERS):
        d.text((S * rx, S * ry), ch, font=font, fill=255, anchor="mm")
    if dilate > 1:
        mask = mask.filter(ImageFilter.MaxFilter(dilate))
    if round_px > 0:
        mask = mask.filter(ImageFilter.GaussianBlur(round_px))
        mask = mask.point(lambda p: 255 if p > 120 else 0)
    img = Image.new("RGBA", (S, S), BLUE)
    white = Image.new("RGBA", (S, S), (255, 255, 255, 255))
    img.paste(white, (0, 0), mask)
    return img


variants = [
    ("msyhbd", os.path.join(FONTS, "msyhbd.ttc"), 0.225, 3, 0.8),
    ("msyhbd+fat", os.path.join(FONTS, "msyhbd.ttc"), 0.225, 7, 1.2),
    ("simyou+fat", os.path.join(FONTS, "SIMYOU.TTF"), 0.24, 9, 1.6),
    ("dengb+fat", os.path.join(FONTS, "Dengb.ttf"), 0.225, 5, 1.0),
]

sheet = Image.new("RGBA", (S * len(variants), S), (255, 255, 255, 255))
for i, (name, path, ratio, dil, rnd) in enumerate(variants):
    img = render(path, ratio, dil, rnd)
    sheet.paste(img, (i * S, 0))
    print(f"{name}: ratio={ratio} dilate={dil} round={rnd}")

out = r"D:\AIsoft\projects\tour-tool\dist\icon-compare.png"
sheet.save(out, "PNG")
print("->", out)
