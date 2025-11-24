"""
01c_filter_domain_by_catalog.py
- 입력: domain_sales.csv, sku_catalog.csv
- 처리:
  * cat_low 표준화(공백/특수문자/대소문자)
  * 카탈로그(cat_low) 화이트리스트 기준으로 미지정/기타/불일치 전부 DROP
  * 드랍된 product_id / 원본 cat_low 통계 로그 생성
- 출력: cleaned_domain_sales.csv, unmapped_summary.csv
"""
from pathlib import Path
import pandas as pd
import re

BASE = Path(__file__).resolve().parent
DOMAIN = BASE / "domain_sales.csv"
SKU_CAT = BASE / "sku_catalog.csv"
OUT_FULL = BASE / "cleaned_domain_sales.csv"
LOG_UNMAPPED = BASE / "unmapped_summary.csv"

MISC_KEYS = {"", "기타", "기타가전", "기타가전류", "기타제품", "etc", "misc", "others", "other"}

def _norm_cat(x: str) -> str:
    if pd.isna(x):
        return ""
    x = str(x).strip()
    x = re.sub(r"\s+", "", x)  # 모든 공백 제거
    x = x.replace("_", "").replace("-", "")  # 언더스코어/하이픈 제거
    return x.lower()

def main():
    if not DOMAIN.exists():
        raise FileNotFoundError(f"{DOMAIN} not found")
    if not SKU_CAT.exists():
        raise FileNotFoundError(f"{SKU_CAT} not found")

    domain_dtypes = {
        "warehouse_id": "Int64",
        "store_id": "Int64",
        "product_id": "Int64",
        "product_name": "string",
        "cat_top": "string",
        "cat_mid": "string",
        "cat_low": "string",
        "region": "string",
    }
    d = pd.read_csv(DOMAIN, parse_dates=["target_date"], dtype=domain_dtypes, low_memory=False)
    s = pd.read_csv(SKU_CAT, dtype={"cat_low": "string"}, low_memory=False)

    # 화이트리스트 준비
    allowed = {_norm_cat(v) for v in s["cat_low"].dropna().astype(str).tolist()}

    # 표준화
    d["cat_low_norm"] = d["cat_low"].apply(_norm_cat)

    # 남길 행: allowed & not misc
    keep_mask = d["cat_low_norm"].isin(allowed) & (~d["cat_low_norm"].isin(MISC_KEYS))
    before = len(d)
    cleaned = d[keep_mask].copy()
    dropped = before - len(cleaned)

    # 로그: 드랍 사유 요약
    dropped_df = d[~keep_mask].copy()
    unmapped = (
        dropped_df.assign(cat_low_raw=dropped_df["cat_low"].astype("string").fillna(""))
                  .groupby(["product_id", "cat_low_raw"], dropna=False)
                  .size().reset_index(name="rows")
                  .sort_values("rows", ascending=False)
    )
    unmapped.to_csv(LOG_UNMAPPED, index=False, encoding="utf-8-sig")

    # 표준화된 cat_low로 치환
    cleaned["cat_low"] = cleaned["cat_low_norm"]
    cleaned = cleaned.drop(columns=["cat_low_norm"])

    cleaned.to_csv(OUT_FULL, index=False)
    print(f"saved: {OUT_FULL} ({len(cleaned):,} rows)")
    print(f"dropped (misc/unknown/out-of-catalog): {dropped:,} / {before:,}")
    print(f"log: {LOG_UNMAPPED}")

if __name__ == "__main__":
    main()
