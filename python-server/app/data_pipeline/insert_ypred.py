import pandas as pd
from pathlib import Path
from app.db import get_connection

BASE = Path(__file__).resolve().parent

CSV = BASE / "prediction_2025.csv"


def load_pred_2025():
    print(f"Loading {CSV} ...")
    df = pd.read_csv(CSV, parse_dates=["target_date"])

    with get_connection() as conn:
        with conn.cursor() as cur:
            for _, row in df.iterrows():

                SNAPSHOT = "2025-01-01 00:00:00"

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
                        1, 1, p.product_id,
                        %s,
                        %s,
                        %s,
                        NOW()
                    FROM product p
                    WHERE p.product_code = %s
                    ON DUPLICATE KEY UPDATE
                        y_pred = VALUES(y_pred),
                        updated_at = NOW()
                    """,
                    (
                        row["target_date"].date(),
                        float(row["y_pred"]),
                        SNAPSHOT,
                        row["product_code"],
                    ),
                )

        conn.commit()

    print("[OK] Updated y_pred for 2025.")


if __name__ == "__main__":
    load_pred_2025()
