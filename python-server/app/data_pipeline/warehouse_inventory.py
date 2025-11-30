import pandas as pd
from pathlib import Path
import random
from app.db import get_connection

OUT = Path(__file__).resolve().parents[1] / "data_pipeline" / "warehouse_inventory.csv"

WAREHOUSE_ID = 1
LEDGER_START_ID = 1

def main():
    with get_connection() as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT product_id FROM product")
            products = [row["product_id"] for row in cur.fetchall()]

    print(f"➡ total products in DB: {len(products)}")

    rows = []
    inv_id = LEDGER_START_ID

    for pid in products:
        on_hand_qty = random.randint(50, 300)
        safety_qty = 10

        rows.append([inv_id, WAREHOUSE_ID, pid, on_hand_qty, safety_qty])
        inv_id += 1

    df = pd.DataFrame(rows, columns=[
        "warehouse_inventory_id",
        "warehouse_id",
        "product_id",
        "on_hand_qty",
        "safety_qty"
    ])

    df.to_csv(OUT, index=False, encoding="utf-8-sig")
    print(f"[OK] warehouse_inventory.csv 생성 완료 : {OUT}")

if __name__ == "__main__":
    main()
