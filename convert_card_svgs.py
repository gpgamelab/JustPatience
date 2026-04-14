#!/usr/bin/env python3
from __future__ import annotations

import argparse
import re
import xml.etree.ElementTree as ET
from dataclasses import dataclass
from pathlib import Path

SVG_NS = "http://www.w3.org/2000/svg"
XLINK_NS = "http://www.w3.org/1999/xlink"


@dataclass(frozen=True)
class Transform:
    scale_x: float = 1.0
    scale_y: float = 1.0
    translate_x: float = 0.0
    translate_y: float = 0.0
    rotation: float = 0.0
    pivot_x: float = 0.0
    pivot_y: float = 0.0

    @property
    def is_identity(self) -> bool:
        return (
            self.scale_x == 1.0
            and self.scale_y == 1.0
            and self.translate_x == 0.0
            and self.translate_y == 0.0
            and self.rotation == 0.0
            and self.pivot_x == 0.0
            and self.pivot_y == 0.0
        )


def local_name(tag: str) -> str:
    return tag.rsplit("}", 1)[-1]


def fmt(value: float) -> str:
    rounded = round(value, 6)
    if abs(rounded) < 1e-9:
        rounded = 0.0
    text = f"{rounded:.6f}".rstrip("0").rstrip(".")
    return text or "0"


def parse_view_box(raw: str) -> tuple[float, float, float, float]:
    min_x, min_y, width, height = [float(part) for part in raw.replace(",", " ").split()]
    return min_x, min_y, width, height


def parse_float(value: str | None, default: float = 0.0) -> float:
    return float(value) if value is not None else default


def parse_transform(raw: str | None) -> Transform:
    if not raw:
        return Transform()

    raw = raw.strip()
    if raw.startswith("matrix("):
        values = [float(part) for part in raw[7:-1].split(",")]
        if len(values) != 6:
            raise ValueError(f"Unsupported matrix transform: {raw}")
        a, b, c, d, e, f = values
        if abs(b) > 1e-9 or abs(c) > 1e-9:
            raise ValueError(f"Skew/rotation matrix is not supported: {raw}")
        return Transform(scale_x=a, scale_y=d, translate_x=e, translate_y=f)

    if raw.startswith("translate("):
        values = [float(part) for part in re.split(r"[ ,]+", raw[10:-1].strip()) if part]
        if len(values) == 1:
            values.append(0.0)
        if len(values) != 2:
            raise ValueError(f"Unsupported translate transform: {raw}")
        return Transform(translate_x=values[0], translate_y=values[1])

    if raw.startswith("rotate("):
        values = [float(part) for part in re.split(r"[ ,]+", raw[7:-1].strip()) if part]
        if len(values) == 1:
            return Transform(rotation=values[0])
        if len(values) == 3:
            return Transform(rotation=values[0], pivot_x=values[1], pivot_y=values[2])
        raise ValueError(f"Unsupported rotate transform: {raw}")

    raise ValueError(f"Unsupported transform: {raw}")


def compose_use_transform(
    base: Transform,
    x: float,
    y: float,
    width: float,
    height: float,
    view_box: tuple[float, float, float, float],
) -> Transform:
    if base.rotation != 0.0:
        raise ValueError("Rotated <use> elements are not supported")

    min_x, min_y, vb_width, vb_height = view_box
    scale_x = width / vb_width
    scale_y = height / vb_height
    offset_x = x - (min_x * scale_x)
    offset_y = y - (min_y * scale_y)

    return Transform(
        scale_x=base.scale_x * scale_x,
        scale_y=base.scale_y * scale_y,
        translate_x=(base.scale_x * offset_x) + base.translate_x,
        translate_y=(base.scale_y * offset_y) + base.translate_y,
    )


def normalize_color(value: str | None) -> str | None:
    if not value or value == "none":
        return None

    named = {
        "black": "#FF000000",
        "white": "#FFFFFFFF",
        "red": "#FFFF0000",
    }
    lowered = value.lower()
    if lowered in named:
        return named[lowered]

    if re.fullmatch(r"#[0-9a-fA-F]{6}", value):
        return f"#FF{value[1:].upper()}"

    if re.fullmatch(r"#[0-9a-fA-F]{8}", value):
        return f"#{value[1:].upper()}"

    return value


def rounded_rect_path(x: float, y: float, width: float, height: float, rx: float, ry: float) -> str:
    right = x + width
    bottom = y + height
    return (
        f"M {fmt(x + rx)} {fmt(y)} "
        f"H {fmt(right - rx)} "
        f"A {fmt(rx)} {fmt(ry)} 0 0 1 {fmt(right)} {fmt(y + ry)} "
        f"V {fmt(bottom - ry)} "
        f"A {fmt(rx)} {fmt(ry)} 0 0 1 {fmt(right - rx)} {fmt(bottom)} "
        f"H {fmt(x + rx)} "
        f"A {fmt(rx)} {fmt(ry)} 0 0 1 {fmt(x)} {fmt(bottom - ry)} "
        f"V {fmt(y + ry)} "
        f"A {fmt(rx)} {fmt(ry)} 0 0 1 {fmt(x + rx)} {fmt(y)} Z"
    )


def path_attributes(elem: ET.Element, path_data: str | None = None) -> list[tuple[str, str]]:
    attrs: list[tuple[str, str]] = []

    data = path_data or elem.attrib["d"]
    attrs.append(("android:pathData", data))

    fill = normalize_color(elem.attrib.get("fill"))
    stroke = normalize_color(elem.attrib.get("stroke"))
    if fill is not None:
        attrs.append(("android:fillColor", fill))
    elif stroke is not None:
        attrs.append(("android:fillColor", "#00000000"))
    if stroke is not None:
        attrs.append(("android:strokeColor", stroke))
        stroke_width = float(elem.attrib.get("stroke-width", "1"))
        attrs.append(("android:strokeWidth", fmt(stroke_width)))
        if "stroke-linecap" in elem.attrib:
            attrs.append(("android:strokeLineCap", elem.attrib["stroke-linecap"]))
        if "stroke-miterlimit" in elem.attrib:
            attrs.append(("android:strokeMiterLimit", fmt(float(elem.attrib["stroke-miterlimit"]))))

    return attrs


def emit_self_closing(tag: str, attrs: list[tuple[str, str]], indent: str) -> list[str]:
    lines = [f"{indent}<{tag}"]
    for key, value in attrs:
        lines.append(f"{indent}    {key}=\"{value}\"")
    lines[-1] = f"{lines[-1]} />"
    return lines


def convert_element(
    elem: ET.Element,
    symbols: dict[str, ET.Element],
    symbol_view_boxes: dict[str, tuple[float, float, float, float]],
    indent: str,
) -> list[str]:
    tag = local_name(elem.tag)

    if tag == "rect":
        path_data = rounded_rect_path(
            x=float(elem.attrib["x"]),
            y=float(elem.attrib["y"]),
            width=float(elem.attrib["width"]),
            height=float(elem.attrib["height"]),
            rx=parse_float(elem.attrib.get("rx")),
            ry=parse_float(elem.attrib.get("ry")),
        )
        return emit_self_closing("path", path_attributes(elem, path_data), indent)

    if tag == "path":
        return emit_self_closing("path", path_attributes(elem), indent)

    if tag == "g":
        children = [
            line
            for child in elem
            for line in convert_element(child, symbols, symbol_view_boxes, indent + "    ")
        ]
        if not children:
            return []

        transform = parse_transform(elem.attrib.get("transform"))
        if transform.is_identity:
            return children

        attrs = transform_attributes(transform)
        lines = [f"{indent}<group"]
        for key, value in attrs:
            lines.append(f"{indent}    {key}=\"{value}\"")
        lines[-1] = f"{lines[-1]}>"
        lines.extend(children)
        lines.append(f"{indent}</group>")
        return lines

    if tag == "use":
        href = elem.attrib.get(f"{{{XLINK_NS}}}href") or elem.attrib.get("href")
        if not href or not href.startswith("#"):
            raise ValueError(f"Unsupported <use> reference in {elem.attrib}")

        symbol_id = href[1:]
        symbol = symbols[symbol_id]
        symbol_view_box = symbol_view_boxes[symbol_id]
        base_transform = parse_transform(elem.attrib.get("transform"))
        use_transform = compose_use_transform(
            base=base_transform,
            x=parse_float(elem.attrib.get("x")),
            y=parse_float(elem.attrib.get("y")),
            width=float(elem.attrib["width"]),
            height=float(elem.attrib["height"]),
            view_box=symbol_view_box,
        )

        symbol_children = [
            line
            for child in symbol
            for line in convert_element(child, symbols, symbol_view_boxes, indent + "    ")
        ]
        attrs = transform_attributes(use_transform)
        lines = [f"{indent}<group"]
        for key, value in attrs:
            lines.append(f"{indent}    {key}=\"{value}\"")
        lines[-1] = f"{lines[-1]}>"
        lines.extend(symbol_children)
        lines.append(f"{indent}</group>")
        return lines

    return []


def transform_attributes(transform: Transform) -> list[tuple[str, str]]:
    attrs: list[tuple[str, str]] = []
    if transform.rotation != 0.0:
        attrs.append(("android:rotation", fmt(transform.rotation)))
        if transform.pivot_x != 0.0:
            attrs.append(("android:pivotX", fmt(transform.pivot_x)))
        if transform.pivot_y != 0.0:
            attrs.append(("android:pivotY", fmt(transform.pivot_y)))
    if transform.scale_x != 1.0:
        attrs.append(("android:scaleX", fmt(transform.scale_x)))
    if transform.scale_y != 1.0:
        attrs.append(("android:scaleY", fmt(transform.scale_y)))
    if transform.translate_x != 0.0:
        attrs.append(("android:translateX", fmt(transform.translate_x)))
    if transform.translate_y != 0.0:
        attrs.append(("android:translateY", fmt(transform.translate_y)))
    return attrs


def convert_svg(svg_path: Path, output_path: Path) -> None:
    tree = ET.parse(svg_path)
    root = tree.getroot()
    view_box = parse_view_box(root.attrib["viewBox"])
    min_x, min_y, width, height = view_box

    symbols: dict[str, ET.Element] = {}
    symbol_view_boxes: dict[str, tuple[float, float, float, float]] = {}
    for defs in root.findall(f"{{{SVG_NS}}}defs"):
        for symbol in defs.findall(f"{{{SVG_NS}}}symbol"):
            symbol_id = symbol.attrib["id"]
            symbols[symbol_id] = symbol
            symbol_view_boxes[symbol_id] = parse_view_box(symbol.attrib["viewBox"])

    body_lines = [
        line
        for child in root
        if local_name(child.tag) not in {"defs", "namedview"}
        for line in convert_element(child, symbols, symbol_view_boxes, "        ")
    ]

    vector_lines = [
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>",
        "<vector xmlns:android=\"http://schemas.android.com/apk/res/android\"",
        f"    android:width=\"{fmt(width)}dp\"",
        f"    android:height=\"{fmt(height)}dp\"",
        f"    android:viewportWidth=\"{fmt(width)}\"",
        f"    android:viewportHeight=\"{fmt(height)}\">",
        "    <group",
        f"        android:translateX=\"{fmt(-min_x)}\"",
        f"        android:translateY=\"{fmt(-min_y)}\">",
        *body_lines,
        "    </group>",
        "</vector>",
        "",
    ]
    output_path.write_text("\n".join(vector_lines), encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser(description="Convert card-face SVGs into Android vector drawables.")
    parser.add_argument(
        "drawable_dir",
        nargs="?",
        default="app/src/main/res/drawable",
        help="Drawable resource directory containing ic_*.svg files.",
    )
    parser.add_argument(
        "--remove-source",
        action="store_true",
        help="Delete the original SVG files after generating XML drawables.",
    )
    args = parser.parse_args()

    drawable_dir = Path(args.drawable_dir)
    svg_files = sorted(drawable_dir.glob("ic_*.svg"))
    if not svg_files:
        raise SystemExit(f"No ic_*.svg files found in {drawable_dir}")

    for svg_path in svg_files:
        output_path = svg_path.with_suffix(".xml")
        convert_svg(svg_path, output_path)
        if args.remove_source:
            svg_path.unlink()
        print(f"Converted {svg_path.name} -> {output_path.name}")


if __name__ == "__main__":
    main()


