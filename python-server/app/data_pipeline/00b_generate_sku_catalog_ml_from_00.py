import pandas as pd
import numpy as np
from pathlib import Path

BASE = Path(__file__).resolve().parent
IN = BASE / "sku_catalog.csv"
OUT = BASE / "sku_catalog_ml_with_share.csv"


BRAND_WEIGHT = {
    "Samsung": 1.25,
    "LG": 1.20,
    "Dyson": 1.30,
    "Apple": 1.40,
    "Sony": 1.10,
}

DEFAULT_BRAND_W = 1.0

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


def extract_brand(product_name):
    return product_name.split()[0]


def extract_minor_option(product_name):

    parts = product_name.split()
    # brand, category 제외 옵션만
    if len(parts) <= 2:
        return ""
    return "-".join(parts[2:])


def calc_option_weight(opt_str):
    w = 1.0
    for key, val in OPTION_WEIGHT.items():
        if key in opt_str:
            w *= val
    return w


def main():
    df = pd.read_csv(IN)

    # SKU ID = product_id
    df["sku_id"] = df["product_id"]

    # brand / minor_option
    df["brand"] = df["product_name"].apply(extract_brand)
    df["minor_option"] = df["product_name"].apply(extract_minor_option)

    df["brand_weight"] = df["brand"].apply(lambda x: BRAND_WEIGHT.get(x, DEFAULT_BRAND_W))
    df["option_weight"] = df["minor_option"].apply(calc_option_weight)

    # 가격 기반 weight
    df["price_weight"] = np.log1p(df["price"])

    # raw weight
    df["raw"] = (
        df["brand_weight"] ** 0.3 *
        df["option_weight"] ** 0.2 *
        df["price_weight"] ** 0.5
    )

    # 카테고리별 base_share 계산
    df["base_share"] = df.groupby("category_name")["raw"].transform(lambda x: x / x.sum())
    df["base_share"] = df["base_share"].clip(1e-6, None)

    # ML용 최종 컬럼
    df_out = df[[
        "sku_id",
        "product_code",
        "product_name",
        "brand",
        "minor_option",
        "price",
        "category_name",
        "base_share",
    ]].copy()

    df_out = df_out.rename(columns={
        "price": "msrp_krw",
        "category_name": "cat_low"
    })

    df_out.to_csv(OUT, index=False, encoding="utf-8-sig")
    print(f"Saved → {OUT}")
    print(df_out.head(20))


if __name__ == "__main__":
    main()
