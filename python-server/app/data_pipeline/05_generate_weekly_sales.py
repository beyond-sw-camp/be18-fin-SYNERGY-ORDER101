"""
05_generate_weekly_sales.py
- domain_sales_sku.csv → weekly_sales.csv 변환
- LightGBM 학습에 필요한 최종 구조를 만든다.
"""

import pandas as pd
from pathlib import Path

BASE = Path(__file__).resolve().parent
SRC = BASE / "domain_sales_sku.csv"
OUT = BASE / "weekly_sales.csv"
EXTRA = BASE / "actual_sales_append.csv"

def main():
    print("Loading domain_sales_sku...")
    df = pd.read_csv(SRC, parse_dates=["target_date"])

    if EXTRA.exists():
        print("Loading extra actual sales...")
        extra = pd.read_csv(EXTRA, parse_dates=["target_date"])
        extra = extra.rename(columns={
            "product_id": "sku_id",
            "actual_order_qty": "actual_order_qty",
        })
        extra_final = extra[["target_date", "sku_id", "actual_order_qty"]]
        df2 = pd.concat([df2, extra_final], ignore_index=True)

    # 필요한 컬럼만 추출
    df2 = df[[
        "target_date",
        "warehouse_id",
        "store_id",
        "sku_id",
        "sku_qty"
    ]].copy()

    # 컬럼명 변경 → LightGBM 파이프라인 기준 맞춤
    df2 = df2.rename(columns={
        "sku_qty": "actual_order_qty"
    })

    # 정렬
    df2 = df2.sort_values(["target_date", "sku_id"]).reset_index(drop=True)

    # 저장
    df2.to_csv(OUT, index=False, encoding="utf-8-sig")

    print(f"Saved weekly_sales.csv → {OUT}")
    print(df2.head(20))


if __name__ == "__main__":
    main()
