import pandas as pd
import numpy as np
from pathlib import Path

BASE = Path(__file__).resolve().parent
IN_PATH = BASE / "sku_catalog.csv"              # 현실 데이터 그대로 사용
OUT_PATH = BASE / "sku_catalog_ml_with_share.csv"  # 04/06이 쓸 파일 이름과 통일

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

# product_code 안의 브랜드 코드 → 실제 브랜드명 매핑
BRAND_MAP = {
    "SAM": "Samsung",
    "LG": "LG",
    "SON": "Sony",
    "APP": "Apple",
    "DYS": "Dyson",
    "DEL": "Dell",
    "HP": "HP",
    "XIA": "Xiaomi",
    "TCL": "TCL",
    "HIS": "Hisense",
}

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


def get_option_weight(option_string: str):
    """minor_option 문자열에서 옵션 키워드 찾아서 weight 곱하기"""
    weight = 1.0
    if pd.isna(option_string):
        return 1.0

    for key, val in OPTION_PREFERENCE.items():
        if key in str(option_string):
            weight *= val

    return weight


def parse_brand_and_option(code: str):
    """
    product_code 예시:
      TV-LG-43인치-8K
      스마-SAM-44MM-실리콘
    → brand_code = 두 번째 토큰 (LG, SAM, XIA, TCL, HIS ...)
    → minor_option = 나머지 뒤쪽 전체
    """
    parts = str(code).split("-")
    if len(parts) >= 3:
        brand_code = parts[1]
        minor_option = "-".join(parts[2:])
    else:
        brand_code = ""
        minor_option = ""
    return brand_code, minor_option


def main():
    print(f"Loading SKU catalog from {IN_PATH} ...")
    df = pd.read_csv(IN_PATH)

    # ---------- cat_low 생성 (카테고리 한글명) ----------
    if "cat_low" not in df.columns:
        if "category_name" in df.columns:
            df["cat_low"] = df["category_name"]
        else:
            raise ValueError("ERROR: cat_low / category_name 컬럼이 없습니다.")

    # ---------- sku_id 생성 (DB product_id와 통일) ----------
    if "sku_id" not in df.columns:
        if "product_id" in df.columns:
            df["sku_id"] = df["product_id"]
        else:
            # product_id도 없으면 그냥 1..N 부여
            df["sku_id"] = range(1, len(df) + 1)

    # ---------- brand / minor_option 파싱 ----------
    brand_codes = []
    minor_options = []
    for code in df["product_code"]:
        b, opt = parse_brand_and_option(code)
        brand_codes.append(b)
        minor_options.append(opt)

    df["brand_code"] = brand_codes
    df["minor_option"] = minor_options
    df["brand"] = df["brand_code"].map(BRAND_MAP).fillna(df["brand_code"])

    # ---------- msrp_krw = price ----------
    if "msrp_krw" not in df.columns:
        if "price" in df.columns:
            df["msrp_krw"] = df["price"].astype(float)
        else:
            raise ValueError("ERROR: price 컬럼이 없습니다. (msrp_krw를 만들 수 없음)")

    # ---------- 가중치 계산 ----------
    df["brand_weight"] = df["brand"].apply(
        lambda x: BRAND_PREFERENCE.get(str(x), DEFAULT_BRAND_WEIGHT)
    )
    df["option_weight"] = df["minor_option"].apply(get_option_weight)
    df["price_weight"] = df["msrp_krw"].apply(lambda x: np.log1p(x))

    # (가격 70%) * (브랜드 20%) * (옵션 10%)
    df["raw_weight"] = (
        df["price_weight"] ** 0.7
        * df["brand_weight"] ** 0.2
        * df["option_weight"] ** 0.1
    )

    # 카테고리별 정규화 → base_share
    df["base_share"] = df.groupby("cat_low")["raw_weight"].transform(
        lambda x: x / x.sum()
    )
    df["base_share"] = df["base_share"].clip(1e-6, None)

    # ---------- 최종 컬럼 정리 ----------
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

    df_out.to_csv(OUT_PATH, index=False, encoding="utf-8-sig")

    print(f"base_share 생성 완료 → {OUT_PATH}")
    print(f"총 SKU: {len(df_out):,}")
    print(df_out.head())


if __name__ == "__main__":
    main()
