"""生成 App 图标：蓝底白字、2x2 排列的「旅游登记」。

一次性脚本，产物写进 Android 资源目录；以后改配色或字样重跑即可。
"""
from PIL import Image, ImageDraw, ImageFont
import os

RES = r"D:\AIsoft\projects\tour-tool\app\src\main\res"
FONT = r"C:\Windows\Fonts\msyhbd.ttc"
BLUE = (31, 90, 166)      # #1F5AA6
WHITE = (255, 255, 255)

CHARS = "旅游登记"
# 2x2 四个字的中心位置（相对画布）。
# 桌面图标常被裁成圆形，圆形可见半径是 0.5 画布宽，
# 所以字心收到 0.30/0.70、字号压到 0.24，四个角才不会被切掉。
CENTERS = [(0.325, 0.325), (0.675, 0.325), (0.325, 0.675), (0.675, 0.675)]


def draw_icon(size: int, bg, text_ratio: float) -> Image.Image:
    """bg=None 表示透明底（自适应图标的前景层）。"""
    img = Image.new("RGBA", (size, size), bg if bg else (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    font_size = int(size * text_ratio)
    font = ImageFont.truetype(FONT, font_size)
    for ch, (rx, ry) in zip(CHARS, CENTERS):
        d.text((size * rx, size * ry), ch, font=font, fill=WHITE, anchor="mm")
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
BIG_LEGACY = draw_icon(1024, BLUE, 0.225)
BIG_FG = draw_icon(1024, None, 0.225 * 0.66)   # 自适应图标安全区是中间 72/108

for folder, legacy_px, fg_px in DENSITIES:
    save(BIG_LEGACY, folder, "ic_launcher.png", legacy_px)
    save(BIG_FG, folder, "ic_launcher_foreground.png", fg_px)

# 顺手留一张预览图
BIG_LEGACY.resize((512, 512), Image.LANCZOS).save(
    r"D:\AIsoft\projects\tour-tool\dist\icon-preview-512.png", "PNG"
)
print("preview -> dist/icon-preview-512.png")
