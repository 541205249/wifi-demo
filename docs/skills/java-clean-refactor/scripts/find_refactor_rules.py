from __future__ import annotations

import argparse
import re
from dataclasses import dataclass
from pathlib import Path

IGNORED_PATH_TOKENS = {
    "admin",
    "app",
    "build",
    "com",
    "demo",
    "docs",
    "example",
    "github",
    "gradle",
    "java",
    "lib",
    "main",
    "references",
    "scripts",
    "skills",
    "src",
    "users",
    "wifi",
    "wifidemo",
    "optometry",
}

GENERIC_CODE_TOKENS = {
    "activity",
    "base",
    "client",
    "delegate",
    "fragment",
    "helper",
    "manager",
    "model",
    "repository",
    "service",
    "state",
    "store",
    "summary",
    "ui",
    "viewmodel",
}

SEMANTIC_HINTS = {
    "activity": ["页面", "入口页"],
    "catalog": ["目录"],
    "command": ["命令"],
    "communication": ["通信"],
    "delegate": ["委托"],
    "device": ["设备"],
    "dispatcher": ["分发"],
    "export": ["导出"],
    "formatter": ["格式化"],
    "formatters": ["格式化"],
    "fragment": ["页面"],
    "gateway": ["网关"],
    "history": ["历史"],
    "log": ["日志"],
    "network": ["网络"],
    "patient": ["患者", "台账"],
    "permission": ["权限"],
    "program": ["程序"],
    "report": ["报告"],
    "repository": ["仓库"],
    "service": ["服务"],
    "settings": ["设置"],
    "store": ["事务", "仓库"],
    "summary": ["摘要"],
    "viewmodel": ["协调", "状态"],
    "workbench": ["工作台"],
}


@dataclass
class Section:
    file_name: str
    heading: str
    start_line: int
    lines: list[tuple[int, str]]


@dataclass(frozen=True)
class FileQuery:
    resolved_path: Path
    file_name: str
    path_candidates: list[str]


@dataclass(frozen=True)
class RankedSection:
    matched_keywords: int
    score: int
    section: Section


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="按关键词或 Java 文件路径，在 skill reference 文档里定位最相关的规则与样本。"
    )
    parser.add_argument("keywords", nargs="*", help="一个或多个关键词，例如：设备 连接页")
    parser.add_argument(
        "--file",
        type=Path,
        help="Java 文件路径。传入后会自动从类名和路径推导关键词，并优先匹配对应项目样本。",
    )
    parser.add_argument("--limit", type=int, default=8, help="最多显示多少条结果，默认 8")
    parser.add_argument(
        "--root",
        type=Path,
        default=Path(__file__).resolve().parent.parent,
        help="skill 根目录，默认取脚本所在 skill 目录",
    )
    return parser


def load_sections(reference_dir: Path) -> list[Section]:
    sections: list[Section] = []
    for path in sorted(reference_dir.glob("*.md")):
        lines = path.read_text(encoding="utf-8").splitlines()
        current_heading: str | None = None
        current_start = 1
        current_lines: list[tuple[int, str]] = []
        for line_no, line in enumerate(lines, start=1):
            if line.startswith("## "):
                if current_heading is not None:
                    if current_heading != "目录":
                        sections.append(
                            Section(
                                file_name=path.name,
                                heading=current_heading,
                                start_line=current_start,
                                lines=current_lines,
                            )
                        )
                current_heading = line[3:].strip()
                current_start = line_no
                current_lines = [(line_no, line)]
                continue
            if current_heading is not None:
                current_lines.append((line_no, line))
        if current_heading is not None:
            if current_heading != "目录":
                sections.append(
                    Section(
                        file_name=path.name,
                        heading=current_heading,
                        start_line=current_start,
                        lines=current_lines,
                    )
                )
    return sections


def normalize(text: str) -> str:
    return text.casefold()


def unique_values(words: list[str]) -> list[str]:
    result: list[str] = []
    seen: set[str] = set()
    for word in words:
        normalized = normalize(word.strip())
        if not normalized or normalized in seen:
            continue
        seen.add(normalized)
        result.append(normalized)
    return result


def unique_keywords(words: list[str]) -> list[str]:
    return unique_values(words)


def normalize_path_text(text: str) -> str:
    return re.sub(r"/+", "/", text.replace("\\", "/").casefold())


def split_identifier(text: str) -> list[str]:
    text = re.sub(r"[^0-9A-Za-z\u4e00-\u9fff]+", " ", text)
    parts: list[str] = []
    for piece in text.split():
        piece = re.sub(r"(?<=[a-z0-9])(?=[A-Z])", " ", piece)
        piece = re.sub(r"(?<=[A-Z])(?=[A-Z][a-z])", " ", piece)
        parts.extend(piece.split())
    return [normalize(part) for part in parts if len(part) >= 2]


def derive_keywords_from_file(file_path: Path) -> list[str]:
    keywords: list[str] = [file_path.stem]
    stem_tokens = split_identifier(file_path.stem)
    for token in stem_tokens:
        if token not in GENERIC_CODE_TOKENS:
            keywords.append(token)
        keywords.extend(SEMANTIC_HINTS.get(token, []))
    for part in file_path.parts:
        for token in split_identifier(part):
            if token in IGNORED_PATH_TOKENS:
                continue
            if token not in GENERIC_CODE_TOKENS:
                keywords.append(token)
            keywords.extend(SEMANTIC_HINTS.get(token, []))
    return unique_keywords(keywords)


def build_file_query(file_path: Path) -> FileQuery:
    starts = {"app", "demo", "lib", "src", "java", "kotlin"}
    path_candidates: list[str] = []
    parts = list(file_path.parts)
    for index, part in enumerate(parts):
        if normalize(part) not in starts:
            continue
        suffix = parts[index:]
        if len(suffix) < 4:
            continue
        path_candidates.append(normalize_path_text("/".join(suffix)))

    path_candidates = unique_values(path_candidates)
    if not path_candidates:
        path_candidates = [normalize_path_text(file_path.name)]

    return FileQuery(
        resolved_path=file_path,
        file_name=normalize(file_path.name),
        path_candidates=path_candidates,
    )


def count_matches(text: str, keyword: str) -> int:
    return len(re.findall(re.escape(keyword), text, flags=re.IGNORECASE))


def score_path_candidates(section_text: str, candidates: list[str], base: int, per_depth: int, max_bonus: int) -> int:
    matched_depths = [candidate.count("/") for candidate in candidates if candidate in section_text]
    if not matched_depths:
        return 0
    return min(max_bonus, base + max(matched_depths) * per_depth)


def score_file_query(section: Section, file_query: FileQuery) -> int:
    section_text = normalize_path_text("\n".join(line for _, line in section.lines))
    bonus = 0
    bonus += score_path_candidates(section_text, file_query.path_candidates, base=18, per_depth=2, max_bonus=42)
    if file_query.file_name in section_text:
        bonus += 10
    return bonus


def score_section(section: Section, keywords: list[str], file_query: FileQuery | None = None) -> tuple[int, int]:
    heading = normalize(section.heading)
    file_name = normalize(section.file_name)
    body = normalize("\n".join(line for _, line in section.lines[1:]))
    matched_keywords = 0
    score = 0
    for keyword in keywords:
        matched = False
        if keyword in heading:
            matched = True
            score += 8
        occurrences = count_matches(body, keyword)
        if occurrences > 0:
            matched = True
            score += min(occurrences, 4) * 2
        if keyword in file_name:
            matched = True
            score += 3
        if matched:
            matched_keywords += 1
    if file_query is not None:
        score += score_file_query(section, file_query)
    return matched_keywords, score


def rank_sections(
    sections: list[Section],
    keywords: list[str],
    file_query: FileQuery | None = None,
) -> list[RankedSection]:
    ranked: list[RankedSection] = []
    for section in sections:
        matched_keywords, score = score_section(section, keywords, file_query)
        if score > 0 and matched_keywords > 0:
            ranked.append(
                RankedSection(
                    matched_keywords=matched_keywords,
                    score=score,
                    section=section,
                )
            )

    ranked.sort(
        key=lambda item: (
            item.score,
            item.matched_keywords,
            item.section.file_name,
            item.section.heading,
        ),
        reverse=True,
    )
    return ranked


def find_snippets(section: Section, keywords: list[str], limit: int = 3) -> list[tuple[int, str]]:
    snippets: list[tuple[int, str]] = []
    for line_no, line in section.lines:
        line_text = normalize(line)
        if any(keyword in line_text for keyword in keywords):
            snippets.append((line_no, line.strip()))
        if len(snippets) >= limit:
            break
    return snippets


def main() -> int:
    parser = build_parser()
    args = parser.parse_args()
    root = args.root.resolve()
    reference_dir = root / "references"
    if not reference_dir.is_dir():
        parser.error(f"未找到 references 目录: {reference_dir}")

    derived_keywords: list[str] = []
    file_query: FileQuery | None = None
    if args.file is not None:
        file_path = args.file.resolve()
        if not file_path.is_file():
            parser.error(f"未找到文件: {file_path}")
        derived_keywords = derive_keywords_from_file(file_path)
        file_query = build_file_query(file_path)

    keywords = unique_keywords(derived_keywords + args.keywords)
    if not keywords:
        parser.error("请至少提供一个关键词，或使用 --file 传入 Java 文件路径。")

    sections = load_sections(reference_dir)

    ranked = rank_sections(sections, keywords, file_query)

    if not ranked:
        print("没有找到匹配结果。可以换更具体或更短的关键词再试一次。")
        return 0

    if args.file is not None:
        print(f"文件: {args.file}")
        print(f"自动关键词: {' / '.join(derived_keywords)}")
        if args.keywords:
            print(f"附加关键词: {' / '.join(args.keywords)}")
    print(f"实际查询关键词: {' / '.join(keywords)}")
    print(f"找到 {min(args.limit, len(ranked))} 条最相关结果:")
    for index, result in enumerate(ranked[: args.limit], start=1):
        section = result.section
        print(
            f"{index}. {section.file_name}:{section.start_line} | {section.heading} "
            f"(匹配词={result.matched_keywords}, 分数={result.score})"
        )
        for line_no, snippet in find_snippets(section, keywords):
            print(f"   - L{line_no}: {snippet}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
