from pathlib import Path
import pandas as pd
from app.db import get_connection

BASE = Path(__file__).resolve().parents[1] / "data_pipeline"
CSV = BASE / "product_master_load.csv"


def load_product_master_once():

    if not CSV.exists():
        print(f"[WARN] {CSV} not found, skip product seed.")
        return

    df = pd.read_csv(CSV, dtype=str)

    sql = """
    INSERT INTO product (
        product_code,
        product_name,
        price,
        status,
        product_category_id
    )
    VALUES (
        %(product_code)s,
        %(product_name)s,
        %(price)s,
        %(status)s,
        %(product_category_id)s
    )
    ON DUPLICATE KEY UPDATE
        product_name        = VALUES(product_name),
        price               = VALUES(price),
        status              = VALUES(status),
        product_category_id = VALUES(product_category_id)
    """

    with get_connection() as conn:
        with conn.cursor() as cur:
            for _, row in df.iterrows():
                cur.execute(
                    sql,
                    {
                        "product_code": row["product_code"],
                        "product_name": row["product_name"],
                        "price": float(row["price"]),
                        "status": int(row["status"]),
                        "product_category_id": int(row["product_category_id"]),
                    },
                )
        conn.commit()

    print(f"[OK] seeded product table from {CSV} (rows={len(df)})")
