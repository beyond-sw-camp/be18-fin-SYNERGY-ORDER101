from pathlib import Path
import pandas as pd

BASE = Path(__file__).resolve().parent
SRC  = BASE / "domain_sales_sku.csv"
OUT  = BASE / "weekly_sales.csv"


def main():
    if not SRC.exists():
        raise FileNotFoundError(f"{SRC} not found")

    df = pd.read_csv(SRC, parse_dates=["target_date"], low_memory=False)
    print(f"Loaded domain_sales_sku.csv: {len(df):,} rows")

    # 1) 필요한 컬럼 체크
    need = ["warehouse_id", "store_id", "sku_id", "target_date", "sku_qty"]
    missing = [c for c in need if c not in df.columns]
    if missing:
        raise ValueError(f"Missing columns in domain_sales_sku.csv: {missing}")

    # 2) sku_id를 product_id로 매핑 (카테고리 레벨 파이프라인 호환용)
    df["product_id"] = df["sku_id"]

    # 3) weekly_sales 스키마로 변환
    weekly = (
        df.groupby(["warehouse_id", "store_id", "product_id", "target_date"], as_index=False)
          .agg(actual_order_qty=("sku_qty", "sum"))
    )

    weekly = weekly.sort_values(["product_id", "target_date", "store_id"]).reset_index(drop=True)
    weekly.to_csv(OUT, index=False)
    print(f"saved: {OUT} ({len(weekly):,} rows)")


if __name__ == "__main__":
    main()
