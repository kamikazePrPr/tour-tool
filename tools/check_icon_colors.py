"""取样桌面截图里图标区域的颜色，客观判断是「蓝底白字」还是「白底蓝字」。

不靠肉眼，数像素：图标区域内蓝像素多就是蓝底，白像素多就是白底。
"""
import sys
from PIL import Image

path = sys.argv[1]
# 图标在截图里的位置（1080x2400，应用抽屉最后一排第三个）
box = (600, 2140, 760, 2300)
im = Image.open(path).convert("RGB").crop(box)
w, h = im.size
px = im.load()

blue = white = other = 0
for y in range(h):
    for x in range(w):
        r, g, b = px[x, y]
        if abs(r - 31) < 40 and abs(g - 90) < 40 and abs(b - 166) < 45:
            blue += 1
        elif r > 225 and g > 225 and b > 225:
            white += 1
        else:
            other += 1

total = w * h
print(f"region {w}x{h}")
print(f"  blue  {blue:6d}  {blue*100/total:5.1f}%")
print(f"  white {white:6d}  {white*100/total:5.1f}%")
print(f"  other {other:6d}  {other*100/total:5.1f}%")
im.resize((w * 2, h * 2), Image.NEAREST).save(path.rsplit(".", 1)[0] + "-crop.png")
print("crop saved")
