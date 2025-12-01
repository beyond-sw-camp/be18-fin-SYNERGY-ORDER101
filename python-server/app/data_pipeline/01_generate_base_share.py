import pandas as pd
import numpy as np
from pathlib import Path

BASE = Path(__file__).resolve().parent
IN = BASE / "sku_catalog.csv"                  
OUT = BASE / "sku_catalog_ml_with_share.csv"   

# --------------------------------------
# 브랜드 선호도 (간단 버전)
# --------------------------------------
BRAND_WEIGHT = {
    "Samsung": 1.25,
    "LG": 1.20,
    "Dyson": 1.30,
    "Apple": 1.40,
    "Sony": 1.10,
}

DEFAULT_BRAND_W = 1.0

# 옵션 선호도
OPTION_WEIGHT = {
    "75인치": 1.30,
    "65인치": 1.20,
    "55인치": 1.10,
    "4K": 1.05,
    "8K": 1.10,
    "512GB": 1.20,
    "1TB": 1.30,
    "16GB": 1.20,
    "32GB": 1.30,
}

DEFAULT_OPT_W = 1.0


def extract_brand(product_name: str) -> str:
    """
    예: 'Samsung TV 55인치 4K' → 'Samsung'
    """
    if not isinstance(product_name, str):
        return ""
    parts = product_name.split()
    return parts[0] if parts else ""


def extract_minor_option(product_name: str) -> str:
    """
    예: 'Samsung TV 55인치 4K' → '55인치-4K'
    (브랜드, 카테고리명을 제외한 나머지 옵션들을 '-'로 연결)
    """
    if not isinstance(product_name, str):
        return ""
    parts = product_name.split()
    if len(parts) <= 2:
        return ""
    return "-".join(parts[2:])


def calc_option_weight(opt_str: str) -> float:
    if not isinstance(opt_str, str):
        return 1.0
    w = 1.0
    for key, val in OPTION_WEIGHT.items():
        if key in opt_str:
            w *= val
    return w


def main():
    print(f"Loading SKU catalog from {IN} ...")
    df = pd.read_csv(IN)

    # sku_id = product_id
    df["sku_id"] = df["product_id"]

    # brand / minor_option 파생
    df["brand"] = df["product_name"].apply(extract_brand)
    df["minor_option"] = df["product_name"].apply(extract_minor_option)

    # 가격  msrp_krw
    df["msrp_krw"] = df["price"].astype(float)

    # cat_low = category_name
    df["cat_low"] = df["category_name"]

    # 가중치 계산
    df["brand_weight"] = df["brand"].apply(lambda x: BRAND_WEIGHT.get(x, DEFAULT_BRAND_W))
    df["option_weight"] = df["minor_option"].apply(calc_option_weight)
    df["price_weight"] = np.log1p(df["msrp_krw"])

    # 가격 50%, 브랜드 30%, 옵션 20% 정도 비중
    df["raw_weight"] = (
        df["price_weight"] ** 0.5
        * df["brand_weight"] ** 0.3
        * df["option_weight"] ** 0.2
    )

    # 카테고리별 base_share 정규화
    df["base_share"] = df.groupby("cat_low")["raw_weight"].transform(
        lambda x: x / x.sum()
    )
    df["base_share"] = df["base_share"].clip(1e-6, None)

    out_cols = [
        "sku_id",
        "product_code",
        "product_name",
        "brand",
        "minor_option",
        "msrp_krw",
        "cat_low",
        "base_share",
    ]
    df_out = df[out_cols].copy()

    df_out.to_csv(OUT, index=False, encoding="utf-8-sig")
    print(f"base_share 생성 완료 → {OUT}")
    print(f"총 SKU: {len(df_out):,}")
    print(df_out.head(10))


if __name__ == "__main__":
    main()
