from pathlib import Path
import pandas as pd

from ..db import get_connection

BASE = Path(__file__).resolve().parents[1] / "data_pipeline"
CSV = BASE / "product_master_load.csv"


# product_category_id 매핑
def _category_id_from_code(code: str) -> int | None:
    prefix = code.split("-")[0]
    mapping = {
        "TV": 12,
        "FR": 13,
        "WM": 14,
        "DRY": 15,
        "VAC": 16,
        "AC": 17,
        "AIR": 18,
        "MW": 19,
        "OV": 20,
        "DW": 21,
        "COF": 22,
        "TOA": 23,
        "MIX": 24,
        "NBK": 25,
        "DES": 26,
        "MON": 27,
        "PHN": 28,
        "WAT": 29,
        "PRN": 30,
        "RTR": 31,
    }
    return mapping.get(prefix)


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
                code = row["product_code"]
                cat_id = _category_id_from_code(code)
                cur.execute(
                    sql,
                    {
                        "product_code": code,
                        "product_name": row["product_name"],
                        "price": 0.00,   # 임시 가격
                        "status": 1,    
                        "product_category_id": cat_id,
                    },
                )
        conn.commit()

    print(f"[OK] seeded product table from {CSV} (rows={len(df)})")
