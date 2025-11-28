import pandas as pd
from pathlib import Path
import random
from app.db import get_connection

OUT = Path(__file__).resolve().parents[1] / "data_pipeline" / "store_inventory.csv"

STORE_ID = 1

def main():
    with get_connection() as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT product_id FROM product")
            products = [row["product_id"] for row in cur.fetchall()]

    print(f"total products in DB: {len(products)}")

    rows = []
    for pid in products:
        on_hand = random.randint(0, 20)
        in_transit = random.randint(0, 5)
        rows.append([STORE_ID, pid, on_hand, in_transit])

    df = pd.DataFrame(rows, columns=[
        "store_id", "product_id", "on_hand_qty", "in_transit_qty"
    ])

    df.to_csv(OUT, index=False, encoding="utf-8-sig")
    print(f"[OK] store_inventory.csv 생성 완료 : {OUT}")

if __name__ == "__main__":
    main()
