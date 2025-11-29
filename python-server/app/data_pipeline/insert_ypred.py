import pandas as pd
from pathlib import Path
from app.db import get_connection

BASE = Path(__file__).resolve().parent
CSV = BASE / "prediction_2025.csv"


def insert_ypred_2025():
    print(f"Loading {CSV} ...")
    df = pd.read_csv(CSV, parse_dates=["target_date"])

    SNAPSHOT = "2025-01-01 00:00:00"

    with get_connection() as conn:
        with conn.cursor() as cur:

            for idx, row in df.iterrows():

                try:
                    cur.execute(
                        """
                        INSERT INTO demand_forecast (
                            warehouse_id,
                            store_id,
                            product_id,
                            target_week,
                            y_pred,
                            snapshot_at,
                            updated_at
                        )
                        SELECT
                            %s,             -- warehouse_id
                            %s,             -- store_id
                            p.product_id,   -- product_id 매핑
                            %s,             -- target_week
                            %s,             -- y_pred
                            %s,             -- snapshot_at
                            NOW()
                        FROM product p
                        WHERE p.product_code = %s
                        """,
                        (
                            int(row["warehouse_id"]),
                            int(row["store_id"]),
                            row["target_date"].date(),
                            float(row["y_pred"]),
                            SNAPSHOT,
                            row["product_code"],
                        ),
                    )

                except Exception as e:
                    print("\nERROR inserting row index:", idx)
                    print("DATA:", row.to_dict())
                    raise e

        conn.commit()

    print("[OK] Inserted y_pred for 2025 (no-duplicate mode).")


if __name__ == "__main__":
    insert_ypred_2025()
