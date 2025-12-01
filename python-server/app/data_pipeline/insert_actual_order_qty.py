import pandas as pd
from pathlib import Path
from app.db import get_connection

BASE = Path(__file__).resolve().parent
CSV = BASE / "actual_2025.csv"


def load_actual_2025():
    print(f"Loading {CSV} ...")
    df = pd.read_csv(CSV, parse_dates=["target_date"])


    SNAPSHOT = "2025-01-01 00:00:00"

    with get_connection() as conn:
        with conn.cursor() as cur:
            for _, row in df.iterrows():


                cur.execute(
                    """
                    INSERT INTO demand_forecast (
                        warehouse_id,
                        store_id,
                        product_id,
                        target_week,
                        actual_order_qty,
                        snapshot_at,
                        updated_at
                    )
                    SELECT
                        1,                   -- warehouse_id
                        1,                   -- store_id
                        p.product_id,        -- 매핑한 product_id
                        %s,                  -- target_week = 날짜
                        %s,                  -- actual_order_qty
                        %s,                  -- snapshot_at
                        NOW()
                    FROM product p
                    WHERE p.product_code = %s
                    ON DUPLICATE KEY UPDATE
                        actual_order_qty = VALUES(actual_order_qty),
                        updated_at = NOW()
                    """,
                    (
                        row["target_date"].date(),        
                        float(row["actual_order_qty"]),   
                        SNAPSHOT,                        
                        row["product_code"],             
                    )
                )

        conn.commit()

    print("[OK] Inserted actual_order_qty for 2025 (safe & idempotent).")


if __name__ == "__main__":
    load_actual_2025()
