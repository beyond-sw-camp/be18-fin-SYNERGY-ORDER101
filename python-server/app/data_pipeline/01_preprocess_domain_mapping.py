"""
01_preprocess_domain_mapping.py
- weekly_sales.csv  → domain_sales.csv (창고 1개 가정)
- taxonomy 헤더 표준화 + merge 후 product_name_x/_y 충돌 정리
"""

from pathlib import Path
import pandas as pd

BASE = Path(__file__).resolve().parent
WEEKLY   = BASE / "weekly_sales.csv"
TAXONOMY = BASE / "taxonomy_product_map.csv"
OUT_FULL   = BASE / "domain_sales.csv"
OUT_SAMPLE = BASE / "sample_domain_sales.csv"

SHIFT_YEARS   = 0
WAREHOUSE_ID  = 1
REGION_NAME   = "본사창고"

STD_MAP = {
    "product_id":   {"product_id", "productid", "id", "item", "item_id"},
    "product_name": {"product_name", "name", "product", "상품명"},
    "cat_top":      {"cat_top", "category_top", "대분류", "top"},
    "cat_mid":      {"cat_mid", "category_mid", "중분류", "mid"},
    "cat_low":      {"cat_low", "category_low", "소분류", "low"},
}

def _shift_year_safe(dt, years):
    """
    연도를 +years 만큼 이동.
    2월 29일(윤년 날짜)은 에러가 나므로 2월 28일로 보정.
    """
    if pd.isna(dt):
        return dt
    dt = pd.to_datetime(dt)

    new_year = dt.year + years
    try:
        return dt.replace(year=new_year)
    except ValueError:
        # 2/29 -> 2/28 로 보정 (또는 3/1로 바꿔도 됨)
        if dt.month == 2 and dt.day == 29:
            return dt.replace(year=new_year, month=2, day=28)
        # 혹시 다른 케이스가 있으면 그냥 원래 날짜에 years만 더한 채로 넘기거나 로그 찍어도 됨
        return dt


def _std_cols(df: pd.DataFrame) -> pd.DataFrame:
    # BOM/공백 제거 + 후보군을 표준 컬럼명으로 매핑
    ren = {}
    for c in df.columns:
        ren[c] = str(c).replace("\ufeff", "").strip()
    df = df.rename(columns=ren)

    lower_to_orig = {c.lower(): c for c in df.columns}
    fix = {}
    for std, cands in STD_MAP.items():
        for cand in cands:
            lc = cand.lower()
            if lc in lower_to_orig:
                fix[lower_to_orig[lc]] = std
                break
    return df.rename(columns=fix)

def _ensure_taxonomy(tax: pd.DataFrame) -> pd.DataFrame:
    if "product_id" not in tax.columns:
        raise ValueError(f"taxonomy_product_map.csv에 'product_id'가 없습니다. 현재 컬럼: {list(tax.columns)}")
    if "cat_low" not in tax.columns:
        raise ValueError(f"taxonomy_product_map.csv에 'cat_low'가 없습니다. 현재 컬럼: {list(tax.columns)}")

    if "product_name" not in tax.columns:
        tax["product_name"] = tax["cat_low"]

    for c in ("cat_top", "cat_mid"):
        if c not in tax.columns:
            tax[c] = ""

    tax["product_id"] = tax["product_id"].astype(str).str.strip()
    return tax

def _fix_name_conflict(df: pd.DataFrame) -> pd.DataFrame:
    # merge 후 product_name이 없을 때 _x/_y에서 복구
    if "product_name" not in df.columns:
        if "product_name_y" in df.columns:
            df["product_name"] = df["product_name_y"].fillna(df.get("product_name_x"))
        elif "product_name_x" in df.columns:
            df["product_name"] = df["product_name_x"]
        elif "cat_low" in df.columns:
            df["product_name"] = df["cat_low"]
        else:
            df["product_name"] = df.get("product_id", "").apply(lambda x: f"PRODUCT_{x}")
    # 여분 제거
    dropc = [c for c in ("product_name_x","product_name_y") if c in df.columns]
    if dropc:
        df = df.drop(columns=dropc)
    return df

def main():
    # 1) weekly 로드
    if not WEEKLY.exists():
        raise FileNotFoundError(f"{WEEKLY} not found")
    df = pd.read_csv(WEEKLY, parse_dates=["target_date"])
    print(f"Loaded weekly_sales.csv: {len(df):,} rows")

    if "product_id" not in df.columns:
        raise ValueError(f"weekly_sales.csv에 product_id 컬럼이 없습니다. 현재 컬럼: {list(df.columns)}")
    df["product_id"] = df["product_id"].astype(str).str.strip()

    # 2) taxonomy 로드 + 표준화
    if not TAXONOMY.exists():
        raise FileNotFoundError(f"{TAXONOMY} not found")
    tax = pd.read_csv(TAXONOMY, encoding="utf-8-sig")
    tax = _std_cols(tax)
    tax = _ensure_taxonomy(tax)

    # 3) merge
    merged = df.merge(tax, on="product_id", how="left")  # validate 제거(환경 차이 회피)
    merged = _fix_name_conflict(merged)                  # 충돌 정리

    # taxonomy 누락 알림(있더라도 위에서 fallback 처리됨)
    if merged["product_name"].isna().any():
        missing = merged.loc[merged["product_name"].isna(),"product_id"].dropna().unique()
        print(f"taxonomy에 없는 product_id: {missing}")
        merged.loc[merged["product_name"].isna(),"product_name"] = merged.loc[
            merged["product_name"].isna(),"product_id"
        ].apply(lambda x: f"PRODUCT_{x}")
        for c in ("cat_top","cat_mid","cat_low"):
            if c in merged.columns:
                merged.loc[merged[c].isna(), c] = ""

    # 4) 창고 1개 가정
    merged["warehouse_id"] = WAREHOUSE_ID
    merged["region"]       = REGION_NAME

    # 5) 연도 시프트
    merged["target_date"] = pd.to_datetime(merged["target_date"])


    # 6) 컬럼/저장
    cols = [
        "warehouse_id","region","store_id",
        "product_id","product_name","cat_top","cat_mid","cat_low",
        "target_date","actual_order_qty",
    ]
    for c in cols:
        if c not in merged.columns:
            merged[c] = "" if c != "actual_order_qty" else 0

    merged = merged[cols].sort_values(["product_id","target_date","store_id"]).reset_index(drop=True)
    merged.to_csv(OUT_FULL, index=False)
    merged.sample(min(1000,len(merged)), random_state=42).to_csv(OUT_SAMPLE, index=False)
    print(f"Saved:\n - {OUT_FULL}\n - {OUT_SAMPLE}\nRows: {len(merged):,}")

if __name__ == "__main__":
    main()
