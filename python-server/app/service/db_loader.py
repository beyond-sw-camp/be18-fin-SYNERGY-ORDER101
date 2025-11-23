from pathlib import Path
import pandas as pd
from app.db import get_connection

BASE = Path(__file__).resolve().parents[1] / "data_pipeline"
CSV = BASE / "product_master_load.csv"

def load_product_master_once():
    df = pd.read_csv(CSV, dtype=str)

    sql = """
    INSERT INTO product (product_id, product_code, product_name)
    VALUES (%(product_id)s, %(product_code)s, %(product_name)s)
    ON DUPLICATE KEY UPDATE product_name = VALUES(product_name);
    """

    with get_connection() as conn:
        with conn.cursor() as cur:
            for idx, row in df.iterrows():
                cur.execute(sql, {
                    "product_id": 200 + idx,  
                    "product_code": row["product_code"],
                    "product_name": row["product_name"]
                })
        conn.commit()
