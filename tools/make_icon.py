"""生成 App 图标：蓝底白字、2x2 排列的「旅游登记」。

字形用微软雅黑粗体，再做一次形态学膨胀 + 轻微圆角化，
笔画变粗、转角变圆，接近手写圆体的感觉；纯字体本身偏方。
"""
from PIL import Image, ImageDraw, ImageFont, ImageFilter
import os

RES = r"D:\AIsoft\projects\tour-tool\app\src\main\res"
FONT = r"C:\Windows\Fonts\msyhbd.ttc"
BLUE = (31, 90, 166)      # #1F5AA6
WHITE = (255, 255, 255)

CHARS = "旅游登记"
# 字心位置：桌面图标常被裁成圆形，可见半径是 0.5 画布宽，
# 收到 0.325/0.675 之后即使在圆形遮罩里也不会切到笔画。
CENTERS = [(0.325, 0.325), (0.675, 0.325), (0.325, 0.675), (0.675, 0.675)]

# 在 1024 画布上：MaxFilter(13) 约等于膨胀 3 像素，模糊 2.4 再二值化得到圆角
DILATE = 9
ROUND = 1.8
TEXT_RATIO_LEGACY = 0.215
TEXT_RATIO_FG = TEXT_RATIO_LEGACY * 0.80   # 前景层放大，圆形遮罩下仍留有余量


def draw_icon(size: int, bg, text_ratio: float) -> Image.Image:
    """bg=None 表示透明底（自适应图标的前景层）。"""
    mask = Image.new("L", (size, size), 0)
    d = ImageDraw.Draw(mask)
    font = ImageFont.truetype(FONT, int(size * text_ratio))
    for ch, (rx, ry) in zip(CHARS, CENTERS):
        d.text((size * rx, size * ry), ch, font=font, fill=255, anchor="mm")

    scale = size / 1024.0
    k = int(DILATE * scale)
    if k % 2 == 0:
        k += 1
    if k > 1:
        mask = mask.filter(ImageFilter.MaxFilter(k))
    mask = mask.filter(ImageFilter.GaussianBlur(max(0.5, ROUND * scale)))
    mask = mask.point(lambda p: 255 if p > 120 else 0)

    img = Image.new("RGBA", (size, size), bg if bg else (0, 0, 0, 0))
    white = Image.new("RGBA", (size, size), (255, 255, 255, 255))
    img.paste(white, (0, 0), mask)
    return img


def save(img: Image.Image, folder: str, name: str, px: int):
    path = os.path.join(RES, folder)
    os.makedirs(path, exist_ok=True)
    img.resize((px, px), Image.LANCZOS).save(os.path.join(path, name), "PNG")
    print(f"{folder}/{name} {px}x{px}")


DENSITIES = [
    ("mipmap-mdpi", 48, 108),
    ("mipmap-hdpi", 72, 162),
    ("mipmap-xhdpi", 96, 216),
    ("mipmap-xxhdpi", 144, 324),
    ("mipmap-xxxhdpi", 192, 432),
]

# 先在大画布上画，再逐级缩小，边缘才干净
BIG_LEGACY = draw_icon(1024, BLUE, TEXT_RATIO_LEGACY)
BIG_FG = draw_icon(1024, None, TEXT_RATIO_FG)

for folder, legacy_px, fg_px in DENSITIES:
    save(BIG_LEGACY, folder, "ic_launcher.png", legacy_px)
    save(BIG_FG, folder, "ic_launcher_foreground.png", fg_px)

# 顺手留一张预览图
BIG_LEGACY.resize((512, 512), Image.LANCZOS).save(
    r"D:\AIsoft\projects\tour-tool\dist\icon-preview-512.png", "PNG"
)
print("preview -> dist/icon-preview-512.png")
