import pandas as pd
from pathlib import Path
from app.db import get_connection

BASE = Path(__file__).resolve().parent.parent / "data_pipeline"

FEATURES = BASE / "features_all.csv"
OUTPUT = BASE / "actual_2025.csv"


def load_product_table():
    """DB에서 product 테이블 로드"""
    with get_connection() as conn:
        query = "SELECT product_id, product_code, product_name FROM product"
        return pd.read_sql(query, conn)


def make_actual_2025():
    print("Loading features_all.csv ...")
    df = pd.read_csv(FEATURES, parse_dates=["target_date"])

    print("Loading PRODUCT from DB...")
    prod = load_product_table()


    merged = df.merge(
        prod,
        how="left",
        on="product_code"    
    )

    mask = (merged["target_date"] >= "2025-01-01") & \
           (merged["target_date"] <= "2025-12-15")

    out = merged.loc[mask, [
        "target_date",
        "product_code",
        "product_id",
        "product_name",
        "actual_order_qty"
    ]]

    out["actual_order_qty"] = out["actual_order_qty"].fillna(0)

    out.to_csv(OUTPUT, index=False, encoding="utf-8-sig")
    print(f"[OK] Saved → {OUTPUT}")


if __name__ == "__main__":
    make_actual_2025()
