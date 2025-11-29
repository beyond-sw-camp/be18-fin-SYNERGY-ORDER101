import pandas as pd
from pathlib import Path

BASE = Path(__file__).resolve().parent
SRC  = BASE / "sku_catalog.csv"
OUT  = BASE / "sku_catalog_converted.csv"

def main():
    print("Loading sku_catalog.csv ...")

    df = pd.read_csv(SRC)

    # category_name → cat_low로 이름 변경
    if "category_name" in df.columns:
        df.rename(columns={"category_name": "cat_low"}, inplace=True)

    # 필드 검증
    required_cols = ["sku_id", "product_code", "product_name", "brand", "msrp_krw", "cat_low", "minor_option"]
    missing_cols = [c for c in required_cols if c not in df.columns]
    if missing_cols:
        raise ValueError(f"[ERROR] sku_catalog.csv missing columns: {missing_cols}")

    # 변환 저장
    df.to_csv(OUT, index=False, encoding="utf-8-sig")

    print(f"[OK] ML용 변환 완료 → {OUT}")
    print(df.head())

if __name__ == "__main__":
    main()
