from __future__ import annotations

from datetime import date
from pathlib import Path

import pandas as pd

from app.db import get_connection 

BASE_DIR = Path(__file__).resolve().parents[1] / "data_pipeline"
PRED_CSV = BASE_DIR / "predictions.csv"


def _load_predictions_for_week(target_week: date) -> pd.DataFrame:
    if not PRED_CSV.exists():
        raise FileNotFoundError(f"predictions.csv not found: {PRED_CSV}")

    df = pd.read_csv(PRED_CSV)
    if "target_date" not in df.columns:
        raise ValueError("predictions.csv 에 'target_date' 컬럼이 없습니다.")

    df["target_date"] = pd.to_datetime(df["target_date"]).dt.date
    return df[df["target_date"] == target_week].copy()


def _build_product_map(conn, sku_codes: list[str]) -> dict[str, int]:
    if not sku_codes:
        return {}

    placeholders = ",".join(["%s"] * len(sku_codes))
    sql = (
        f"SELECT product_id, product_code "
        f"FROM product "
        f"WHERE product_code IN ({placeholders})"
    )

    with conn.cursor() as cur:
        cur.execute(sql, sku_codes)
        rows = cur.fetchall()

    code_to_id = {row["product_code"]: row["product_id"] for row in rows}
    missing = set(sku_codes) - set(code_to_id.keys())
    if missing:
        print(f"[WARN] product_code not found in product table: {missing}")

    return code_to_id


def run_forecast_pipeline(target_week: date | str) -> int:
    """
    - predictions.csv 에서 target_week 예측을 읽고
    - product_id 매핑 후
    - demand_forecast 에 삭제 없이 UPSERT
      (기존 row 있으면 y_pred / snapshot_at / updated_at 만 갱신)
    """

    if isinstance(target_week, str):
        target_week = date.fromisoformat(target_week)

    print(f"[FORECAST] start pipeline. target_week={target_week}")

    week_df = _load_predictions_for_week(target_week)
    if week_df.empty:
        print(f"[FORECAST] no predictions found for week={target_week}")
        return 0

    for col in ["sku_id", "y_pred"]:
        if col not in week_df.columns:
            raise ValueError(f"predictions.csv 에 '{col}' 컬럼이 없습니다.")

    sku_list = sorted(week_df["sku_id"].astype(str).unique())

    with get_connection() as conn:
        code_to_id = _build_product_map(conn, sku_list)

        insert_sql = """
        INSERT INTO demand_forecast (product_id, target_week, y_pred, snapshot_at, updated_at)
        VALUES (%(product_id)s, %(target_week)s, %(y_pred)s, NOW(), NOW())
        ON DUPLICATE KEY UPDATE
            y_pred      = VALUES(y_pred),
            snapshot_at = NOW(),
            updated_at  = NOW()
        """

        inserted = 0
        with conn.cursor() as cur:
            for _, row in week_df.iterrows():
                sku = str(row["sku_id"])
                product_id = code_to_id.get(sku)
                if product_id is None:
                    continue

                cur.execute(
                    insert_sql,
                    {
                        "product_id": product_id,
                        "target_week": target_week,
                        "y_pred": float(row["y_pred"]),
                    },
                )
                inserted += 1

        conn.commit()

    print(
        f"[FORECAST] upserted {inserted} rows into demand_forecast for week={target_week}"
    )
    return inserted
