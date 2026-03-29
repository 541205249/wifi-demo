from __future__ import annotations

import argparse
import re
from collections import Counter
from dataclasses import dataclass
from pathlib import Path


SCAN_ROOTS = (
    "app/src/main/java",
    "demo/src/main/java",
    "lib/src/main/java",
)


@dataclass(frozen=True)
class Candidate:
    relative_path: str
    line_count: int
    mirror_count: int


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="列出还没进入 decision-patterns.md 的候选 Java 类，便于继续补样本。")
    parser.add_argument("keywords", nargs="*", help="可选关键词，只保留路径里包含这些词的候选类。")
    parser.add_argument("--limit", type=int, default=30, help="最多显示多少条结果，默认 30。")
    parser.add_argument(
        "--root",
        type=Path,
        default=Path(__file__).resolve().parent.parent,
        help="skill 根目录，默认取脚本所在 skill 目录",
    )
    return parser


def normalize(text: str) -> str:
    return text.casefold()


def resolve_repo_root(skill_root: Path) -> Path:
    return skill_root.parents[2]


def load_covered_paths(skill_root: Path) -> set[str]:
    decision_patterns = skill_root / "references" / "decision-patterns.md"
    text = decision_patterns.read_text(encoding="utf-8")
    return {
        normalize(match.group(1).replace("\\", "/"))
        for match in re.finditer(r"`((?:app|demo|lib)/src/main/java/[^`]+\.java)`", text)
    }


def count_lines(path: Path) -> int:
    return path.read_text(encoding="utf-8", errors="ignore").count("\n") + 1


def collect_candidates(repo_root: Path, covered_paths: set[str]) -> list[Candidate]:
    java_paths: list[str] = []
    raw_candidates: list[tuple[str, int]] = []
    for scan_root in SCAN_ROOTS:
        root = repo_root / scan_root
        if not root.is_dir():
            continue
        for path in root.rglob("*.java"):
            relative_path = path.relative_to(repo_root).as_posix()
            normalized_path = normalize(relative_path)
            if normalized_path in covered_paths:
                continue
            java_paths.append(path.name)
            raw_candidates.append((relative_path, count_lines(path)))

    mirror_counter = Counter(java_paths)
    candidates = [
        Candidate(
            relative_path=relative_path,
            line_count=line_count,
            mirror_count=mirror_counter[Path(relative_path).name],
        )
        for relative_path, line_count in raw_candidates
    ]
    candidates.sort(
        key=lambda item: (
            item.line_count,
            item.mirror_count,
            item.relative_path,
        ),
        reverse=True,
    )
    return candidates


def match_keywords(candidate: Candidate, keywords: list[str]) -> bool:
    if not keywords:
        return True
    path_text = normalize(candidate.relative_path)
    return all(keyword in path_text for keyword in keywords)


def main() -> int:
    args = build_parser().parse_args()
    skill_root = args.root.resolve()
    repo_root = resolve_repo_root(skill_root)
    keywords = [normalize(keyword) for keyword in args.keywords if keyword.strip()]

    covered_paths = load_covered_paths(skill_root)
    candidates = collect_candidates(repo_root, covered_paths)
    filtered = [candidate for candidate in candidates if match_keywords(candidate, keywords)]

    if not filtered:
        print("没有找到符合条件的未覆盖候选类。")
        return 0

    print(f"已覆盖样本：{len(covered_paths)}")
    print(f"未覆盖候选：{len(filtered)}")
    if keywords:
        print(f"关键词过滤：{' / '.join(keywords)}")
    print(f"展示前 {min(args.limit, len(filtered))} 条：")
    for index, candidate in enumerate(filtered[: args.limit], start=1):
        mirror_text = f"，同名镜像 {candidate.mirror_count} 个" if candidate.mirror_count > 1 else ""
        print(f"{index}. {candidate.line_count:4d} 行 | {candidate.relative_path}{mirror_text}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
