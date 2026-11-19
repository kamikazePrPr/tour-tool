"""把用户给的那张「旅游登记」字样直接拿来做图标：只换颜色，不换字形。

原图是白底黑字，渲染成 蓝底白字：
  1. 取亮度反相 → 字变白、底变黑，直接当 alpha 通道
  2. 二值化 + 放大到 1024，边缘保住锐度
  3. 白色贴到蓝色画布上
"""
from PIL import Image, ImageDraw, ImageFont, ImageFilter
import os

SRC = r"C:\Users\Kitsune\.dsh\attachments\v1\objects\ce\cef0781d315f7b837dc434d67fcae69007d3648e9a01662d45762f136c81c3c5"
RES = r"D:\AIsoft\projects\tour-tool\app\src\main\res"
BLUE = (31, 90, 166)      # #1F5AA6
WHITE = (255, 255, 255)

src = Image.open(SRC).convert("L")
print("source size:", src.size)

# 反相：黑字 → 白
inv = Image.eval(src, lambda p: 255 - p)
# 二值化，去掉放大时容易出现的灰边
mask_small = inv.point(lambda p: 255 if p > 110 else 0)

# 放到正方形画布中央，保持长宽比
side = max(mask_small.size)
square = Image.new("L", (side, side), 0)
square.paste(
    mask_small,
    ((side - mask_small.width) // 2, (side - mask_small.height) // 2),
)

# 放大到 1024，再阈值化，避免模糊
BIG = 1024
mask_src = square.resize((BIG, BIG), Image.LANCZOS).point(lambda p: 255 if p > 110 else 0)

# 原图的字笔画特别粗，占满整块画布；直接缩小到桌面图标大小，
# 白色会连成一片、蓝色的缝隙反而像字，看起来就成"白底蓝字"了。
# 所以整块字缩到画布的 80%，四周留出蓝边，缩到 48px 也仍能看出是蓝底。
INNER = 0.80
inner_side = int(BIG * INNER)
inner = mask_src.resize((inner_side, inner_side), Image.LANCZOS)
mask = Image.new("L", (BIG, BIG), 0)
mask.paste(inner, ((BIG - inner_side) // 2, (BIG - inner_side) // 2))

# 原图只有 133px，放大后边是台阶状；轻微模糊再二值化把台阶磨平
mask = mask.filter(ImageFilter.GaussianBlur(3.0)).point(lambda p: 255 if p > 128 else 0)

legacy = Image.new("RGBA", (BIG, BIG), BLUE)
legacy.paste(Image.new("RGBA", (BIG, BIG), WHITE), (0, 0), mask)

# 自适应图标前景：透明底白字，整块缩到安全区里
fg_side = int(BIG * 0.80)
fg_mask = mask.resize((fg_side, fg_side), Image.LANCZOS)
fg_layer = Image.new("RGBA", (fg_side, fg_side), (0, 0, 0, 0))
fg_layer.paste(Image.new("RGBA", (fg_side, fg_side), WHITE), (0, 0), fg_mask)
foreground = Image.new("RGBA", (BIG, BIG), (0, 0, 0, 0))
foreground.paste(fg_layer, ((BIG - fg_side) // 2, (BIG - fg_side) // 2))


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

for folder, legacy_px, fg_px in DENSITIES:
    save(legacy, folder, "ic_launcher.png", legacy_px)
    save(foreground, folder, "ic_launcher_foreground.png", fg_px)

legacy.resize((512, 512), Image.LANCZOS).save(
    r"D:\AIsoft\projects\tour-tool\dist\icon-preview-512.png", "PNG"
)
print("preview -> dist/icon-preview-512.png")
