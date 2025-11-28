import pandas as pd
import numpy as np
from pathlib import Path

BASE = Path(__file__).resolve().parent
IN_PATH = BASE / "sku_catalog_ml.csv"
OUT_PATH = BASE / "sku_catalog_ml_with_share.csv"

# -------------------------------------------------------------
# 브랜드 선호도 (시장 현실 기반, 카테고리별 가중치)
# -------------------------------------------------------------
BRAND_PREFERENCE = {
    "Samsung": 1.25,
    "LG": 1.20,
    "Sony": 1.10,
    "Apple": 1.40,
    "Dyson": 1.30,
    "Dell": 1.10,
    "HP": 1.10,
    "Xiaomi": 0.95,
    "TCL": 0.90,
    "Hisense": 0.85,
}

DEFAULT_BRAND_WEIGHT = 1.0

# -------------------------------------------------------------
# 옵션 기반 선호도 테이블
# -------------------------------------------------------------
OPTION_PREFERENCE = {
    "75인치": 1.4,
    "65인치": 1.25,
    "55인치": 1.10,
    "50인치": 1.00,
    "43인치": 0.90,

    "512GB": 1.20,
    "1TB": 1.30,
    "16GB": 1.20,
    "32GB": 1.40,

    "히트펌프": 1.20,
    "스탠드형": 1.10,
    "HEPA": 1.10,
}

DEFAULT_OPT_WEIGHT = 1.0


# -------------------------------------------------------------
# 옵션명에서 중요한 키워드를 추출
# -------------------------------------------------------------
def get_option_weight(option_string: str):
    weight = 1.0
    if pd.isna(option_string):
        return 1.0

    for key, val in OPTION_PREFERENCE.items():
        if key in option_string:
            weight *= val

    return weight


# -------------------------------------------------------------
# base_share 계산
# -------------------------------------------------------------
def main():
    print("Loading SKU catalog...")
    df = pd.read_csv(IN_PATH)

    if "cat_low" not in df.columns:
        raise ValueError("ERROR: cat_low(카테고리)가 CSV에 없습니다.")

    df["brand_weight"] = df["brand"].apply(
        lambda x: BRAND_PREFERENCE.get(x, DEFAULT_BRAND_WEIGHT)
    )

    df["option_weight"] = df["minor_option"].apply(get_option_weight)

    # 가격 기반 가중치: log(price)
    df["price_weight"] = df["msrp_krw"].apply(lambda x: np.log1p(x))

    # ---- 최종 가중치 계산 ----
    # (가격 70%) * (브랜드 20%) * (옵션 10%)
    df["raw_weight"] = (
        df["price_weight"] ** 0.7
        * df["brand_weight"] ** 0.2
        * df["option_weight"] ** 0.1
    )

    # ---------------------------------------------------------
    # 카테고리별 정규화
    # ---------------------------------------------------------
    df["base_share"] = df.groupby("cat_low")["raw_weight"].transform(
        lambda x: x / x.sum()
    )

    # 0~1 스케일에서 너무 작은 값 방지
    df["base_share"] = df["base_share"].clip(1e-6, None)

    # ---------------------------------------------------------
    # 저장
    # ---------------------------------------------------------
    df.to_csv(OUT_PATH, index=False, encoding="utf-8-sig")

    print(f"base_share 생성 완료 → {OUT_PATH}")
    print(f"총 SKU: {len(df):,}")
    print(df[["cat_low", "sku_id", "brand", "minor_option", "base_share"]].head())


if __name__ == "__main__":
    main()
