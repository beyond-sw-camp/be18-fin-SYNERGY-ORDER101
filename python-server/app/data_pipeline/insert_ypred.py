import pandas as pd
from pathlib import Path
from app.db import get_connection

BASE = Path(__file__).resolve().parent
CSV = BASE / "prediction_2025.csv"

def update_ypred_2025():
    print(f"Loading {CSV} ...")
    df = pd.read_csv(CSV, parse_dates=["target_date"])

    SNAPSHOT = "2025-01-01 00:00:00"   
    with get_connection() as conn:
        with conn.cursor() as cur:
            for idx, row in df.iterrows():
                try:
                    cur.execute(
                        """
                        UPDATE demand_forecast df
                        JOIN product p ON df.product_id = p.product_id
                        SET 
                            df.y_pred = %s,
                            df.updated_at = NOW()
                        WHERE 
                            df.warehouse_id = %s
                            AND df.store_id = %s
                            AND df.target_week = %s
                            AND df.snapshot_at = %s
                            AND p.product_code = %s
                        """,
                        (
                            float(row["y_pred"]),
                            int(row["warehouse_id"]),
                            int(row["store_id"]),
                            row["target_date"].date(),
                            SNAPSHOT,
                            row["product_code"],
                        ),
                    )

                except Exception as e:
                    print("\nERROR on row:", idx)
                    print("DATA:", row.to_dict())
                    raise e

        conn.commit()

    print("[OK] Updated y_pred for all 2025 records.")

if __name__ == "__main__":
    update_ypred_2025()
