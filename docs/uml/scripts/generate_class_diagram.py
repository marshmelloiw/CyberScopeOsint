import argparse
import re
from collections import defaultdict
from dataclasses import dataclass, field, replace
from pathlib import Path
from typing import Dict, List, Optional, Sequence


@dataclass
class TypeReference:
    name: str
    package: str
    kind: str
    full_name: str
    extends: List[str] = field(default_factory=list)
    implements: List[str] = field(default_factory=list)
    fields: List[tuple[str, str]] = field(default_factory=list)
    alias: str = ""
    is_stub: bool = False


KEYWORD_PATTERN = re.compile(r"\b(class|interface|enum|record)\s+([A-Za-z_][A-Za-z0-9_]*)")
PACKAGE_PATTERN = re.compile(r"package\s+([\w\.]+)\s*;")
ANNOTATION_PATTERN = re.compile(r"@[^\s(]+(?:\([^)]*\))?")
MODIFIERS_PATTERN = re.compile(
    r"\b(public|protected|private|static|final|abstract|transient|volatile|synchronized|default)\b"
)


def _replace_with_spaces(match: re.Match) -> str:
    return " " * len(match.group(0))


def sanitise_source(source: str) -> str:
    without_block_comments = re.sub(r"/\*.*?\*/", _replace_with_spaces, source, flags=re.S)
    without_line_comments = re.sub(r"//.*", _replace_with_spaces, without_block_comments)
    without_double_quotes = re.sub(
        r'"(?:\\.|[^"\\])*"',
        _replace_with_spaces,
        without_line_comments,
        flags=re.S,
    )
    without_single_quotes = re.sub(r"'(?:\\.|[^'\\])*'", _replace_with_spaces, without_double_quotes)
    return without_single_quotes


def compute_depth_map(text: str) -> List[int]:
    depth = 0
    depths: List[int] = [0] * (len(text) + 1)
    for idx, ch in enumerate(text):
        depths[idx] = depth
        if ch == "{":
            depth += 1
        elif ch == "}":
            depth = max(depth - 1, 0)
    depths[len(text)] = depth
    return depths


def find_matching_brace(text: str, start: int) -> Optional[int]:
    depth = 0
    for idx in range(start, len(text)):
        ch = text[idx]
        if ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
            if depth == 0:
                return idx
    return None


def normalise_whitespace(value: str) -> str:
    return " ".join(value.split())


def extract_types(type_section: str) -> List[str]:
    no_generics = re.sub(r"<[^<>]*>", "", type_section)
    parts = [part.strip() for part in no_generics.split(",") if part.strip()]
    return parts


def extract_field_candidates(field_type: str) -> List[str]:
    cleaned = (
        field_type.replace("<", " ")
        .replace(">", " ")
        .replace("[", " ")
        .replace("]", " ")
        .replace(",", " ")
        .replace("?", " ")
    )
    cleaned = cleaned.replace("extends", " ").replace("super", " ")
    cleaned = cleaned.replace("&", " ")
    cleaned = cleaned.replace("|", " ")
    tokens = re.findall(r"[A-Z][A-Za-z0-9_]*", cleaned)
    return tokens


def resolve_targets(
    raw_name: str,
    current_package: str,
    full_name_map: Dict[str, TypeReference],
    simple_name_map: Dict[str, List[TypeReference]],
) -> List[TypeReference]:
    name = raw_name.strip()
    if not name:
        return []
    if name in full_name_map:
        return [full_name_map[name]]
    simple = name.split(".")[-1]
    candidates = simple_name_map.get(simple, [])
    if not candidates:
        return []
    if len(candidates) == 1:
        return candidates
    preferred = [c for c in candidates if c.package == current_package]
    return preferred or candidates


def parse_field_from_snippet(snippet: str) -> Optional[tuple[str, str]]:
    cleaned = snippet.strip().rstrip(";")
    if not cleaned:
        return None
    cleaned = re.sub(r"//.*", " ", cleaned)
    cleaned = re.sub(r"/\*.*?\*/", " ", cleaned, flags=re.S)
    cleaned = ANNOTATION_PATTERN.sub(" ", cleaned)
    cleaned = MODIFIERS_PATTERN.sub(" ", cleaned)
    cleaned = normalise_whitespace(cleaned)
    if not cleaned or "(" in cleaned or cleaned.startswith("class ") or cleaned.startswith("interface "):
        return None
    if cleaned.startswith("enum ") or cleaned.startswith("record "):
        return None
    if "=" in cleaned:
        cleaned = cleaned.split("=", 1)[0].strip()
    match = re.match(r"(.+)\s+([A-Za-z_][A-Za-z0-9_]*)$", cleaned)
    if not match:
        return None
    field_type = match.group(1).strip()
    field_name = match.group(2).strip()
    if not field_type or not field_name:
        return None
    return field_type, field_name


def extract_fields(
    original_body: str,
    sanitised_body: str,
) -> List[tuple[str, str]]:
    fields: List[tuple[str, str]] = []
    depth = 0
    current_original: List[str] = []
    for idx, ch in enumerate(sanitised_body):
        orig_ch = original_body[idx]
        if ch == "{":
            depth += 1
        elif ch == "}":
            depth = max(depth - 1, 0)
        elif ch == ";" and depth == 0:
            current_original.append(orig_ch)
            snippet = "".join(current_original)
            field = parse_field_from_snippet(snippet)
            if field:
                fields.append(field)
            current_original = []
            continue
        current_original.append(orig_ch)
    return fields


def make_alias(full_name: str) -> str:
    return full_name.replace(".", "_").replace("$", "_")


def gather_type_references(root: Path) -> List[TypeReference]:
    types: List[TypeReference] = []
    for java_path in root.rglob("*.java"):
        source = java_path.read_text(encoding="utf-8")
        sanitised = sanitise_source(source)
        depth_map = compute_depth_map(sanitised)
        package_match = PACKAGE_PATTERN.search(sanitised)
        package_name = package_match.group(1) if package_match else ""
        for match in KEYWORD_PATTERN.finditer(sanitised):
            start_idx = match.start()
            if depth_map[start_idx] != 0:
                continue
            kind = match.group(1)
            name = match.group(2)
            brace_idx = sanitised.find("{", match.end())
            if brace_idx == -1:
                continue
            body_end = find_matching_brace(sanitised, brace_idx)
            if body_end is None:
                continue
            header = sanitised[start_idx:brace_idx]
            extends_match = re.search(r"extends\s+([^\{]+?)(?:implements|$)", header)
            implements_match = re.search(r"implements\s+([^\{]+)$", header)
            extends = extract_types(extends_match.group(1)) if extends_match else []
            implements = extract_types(implements_match.group(1)) if implements_match else []
            body_start = brace_idx + 1
            body = source[body_start:body_end]
            body_sanitised = sanitised[body_start:body_end]
            fields = extract_fields(body, body_sanitised)
            full_name = f"{package_name}.{name}" if package_name else name
            types.append(
                TypeReference(
                    name=name,
                    package=package_name,
                    kind=kind,
                    full_name=full_name,
                    extends=[normalise_whitespace(item) for item in extends],
                    implements=[normalise_whitespace(item) for item in implements],
                    fields=fields,
                    alias=make_alias(full_name),
                )
            )
    return types


def unique_alias_map(types: Sequence[TypeReference]) -> Dict[str, str]:
    alias_counts: Dict[str, int] = defaultdict(int)
    alias_map: Dict[str, str] = {}
    for t in types:
        base = t.alias or make_alias(t.full_name)
        alias_counts[base] += 1
        alias = base if alias_counts[base] == 1 else f"{base}_{alias_counts[base]}"
        alias_map[t.full_name] = alias
    return alias_map


def build_group_diagram(
    primary_types: Sequence[TypeReference],
    full_name_map: Dict[str, TypeReference],
    simple_name_map: Dict[str, List[TypeReference]],
) -> List[str]:
    included: Dict[str, TypeReference] = {t.full_name: t for t in primary_types}
    edges: List[tuple[str, str, str, Optional[str]]] = []

    def ensure_included(target: TypeReference, stub: bool = False) -> TypeReference:
        if target.full_name in included:
            return included[target.full_name]
        if stub:
            clone = replace(target)
            clone.fields = []
            clone.extends = []
            clone.implements = []
            clone.is_stub = True
            included[clone.full_name] = clone
            return clone
        included[target.full_name] = target
        return target

    for t in primary_types:
        for base in t.extends:
            base = normalise_whitespace(base)
            targets = resolve_targets(base, t.package, full_name_map, simple_name_map)
            for target in targets:
                ensure_included(target, stub=target not in primary_types)
                edges.append((t.full_name, "extends", target.full_name, None))
        for interface in t.implements:
            interface = normalise_whitespace(interface)
            targets = resolve_targets(interface, t.package, full_name_map, simple_name_map)
            for target in targets:
                ensure_included(target, stub=target not in primary_types)
                edges.append((t.full_name, "implements", target.full_name, None))

    field_relations: Dict[tuple[str, str], List[str]] = defaultdict(list)

    primary_full_names = {t.full_name for t in primary_types}

    for t in primary_types:
        for field_type, field_name in t.fields:
            candidate_names = extract_field_candidates(field_type)
            for candidate in candidate_names:
                targets = resolve_targets(candidate, t.package, full_name_map, simple_name_map)
                for target in targets:
                    ensure_included(target, stub=target.full_name not in primary_full_names)
                    key = (t.full_name, target.full_name)
                    field_relations[key].append(field_name)

    for (source_full, target_full), names in field_relations.items():
        label = ", ".join(sorted(set(names)))
        edges.append((source_full, "association", target_full, label if label else None))

    ordered_types = sorted(included.values(), key=lambda item: (item.package, item.name))
    alias_map = unique_alias_map(ordered_types)

    lines: List[str] = ["classDiagram", "", "direction TB", ""]

    for t in ordered_types:
        display_name = t.name if t.name else t.full_name
        lines.append(f'class {display_name} {{')
        stereotype = None
        if t.is_stub:
            stereotype = "<<external>>"
        elif t.kind == "interface":
            stereotype = "<<interface>>"
        elif t.kind == "enum":
            stereotype = "<<enum>>"
        elif t.kind == "record":
            stereotype = "<<record>>"
        if stereotype:
            lines.append(f"    {stereotype}")
        if not t.is_stub:
            for field_type, field_name in t.fields:
                field_descriptor = normalise_whitespace(field_type)
                lines.append(f"    {field_descriptor} {field_name}")
        lines.append("}")

    written_relations = set()
    for source_full, relation_kind, target_full, label in edges:
        source_type = full_name_map.get(source_full)
        target_type = full_name_map.get(target_full)
        if not source_type or not target_type:
            continue
        source_name = source_type.name if source_type.name else source_full.split('.')[-1]
        target_name = target_type.name if target_type.name else target_full.split('.')[-1]
        relation_key = (source_name, relation_kind, target_name, label)
        if relation_key in written_relations:
            continue
        written_relations.add(relation_key)
        if relation_kind == "extends":
            lines.append(f"{source_name} --|> {target_name}")
        elif relation_kind == "implements":
            lines.append(f"{source_name} ..|> {target_name}")
        elif relation_kind == "association":
            if label:
                lines.append(f"{source_name} --> {target_name} : {label}")
            else:
                lines.append(f"{source_name} --> {target_name}")

    return lines


def main() -> None:
    parser = argparse.ArgumentParser(description="Generate Mermaid class diagrams for Java sources")
    parser.add_argument(
        "--source",
        type=Path,
        default=Path("backend/src/main/java"),
        help="Root directory containing Java source files",
    )
    parser.add_argument(
        "--output-dir",
        type=Path,
        default=Path("docs/uml"),
        help="Directory to write Mermaid diagram files",
    )
    default_groups = [
        "controllers=osint.controller",
        "services=osint.service",
        "repositories=osint.repository",
        "models=osint.model",
    ]
    parser.add_argument(
        "--groups",
        nargs="*",
        default=default_groups,
        help="Group mappings in the form name=package.prefix. Defaults to controllers/services/repositories/models",
    )
    args = parser.parse_args()

    source_root = args.source.resolve()
    if not source_root.exists():
        raise SystemExit(f"Source directory not found: {source_root}")

    group_mappings: Dict[str, str] = {}
    for item in args.groups:
        if "=" not in item:
            raise SystemExit(f"Invalid group specification: {item}. Expected name=package.prefix")
        name, prefix = item.split("=", 1)
        name = name.strip()
        prefix = prefix.strip()
        if not name or not prefix:
            raise SystemExit(f"Invalid group specification: {item}. Name and prefix required.")
        group_mappings[name] = prefix

    types = gather_type_references(source_root)
    if not types:
        raise SystemExit("No Java types found.")

    full_name_map: Dict[str, TypeReference] = {t.full_name: t for t in types}
    simple_name_map: Dict[str, List[TypeReference]] = defaultdict(list)
    for t in types:
        simple_name_map[t.name].append(t)

    output_dir = args.output_dir.resolve()
    output_dir.mkdir(parents=True, exist_ok=True)

    for group_name, prefix in group_mappings.items():
        primary = [t for t in types if t.package.startswith(prefix)]
        if not primary:
            continue
        lines = build_group_diagram(primary, full_name_map, simple_name_map)
        output_path = output_dir / f"{group_name}.mmd"
        output_path.write_text("\n".join(lines) + "\n", encoding="utf-8")
        print(f"Mermaid class diagram written to {output_path}")


if __name__ == "__main__":
    main()

