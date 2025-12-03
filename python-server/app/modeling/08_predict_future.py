from pathlib import Path
import pandas as pd
import numpy as np
import joblib
import json
from datetime import datetime, timedelta

BASE = Path(__file__).resolve().parents[1] / "data_pipeline"
FEA = BASE / "features_all.csv"
MODEL_PATH    = BASE / "lightgbm_model.pkl"
FEATURES_PATH = BASE / "lightgbm_features.json"
OUT = BASE / "predictions.csv"

HORIZON_WEEKS = 12
FORECAST_FREQ = "W-MON"
KEYS = ["warehouse_id", "store_id", "sku_id"]
BASE_DATE = datetime(2025, 12, 15)

def _to_week_start_monday(dt: pd.Series) -> pd.Series:
    dt = pd.to_datetime(dt)
    return (dt - pd.to_timedelta(dt.dt.weekday % 7, unit="D")).dt.normalize()


def _add_time_features(df: pd.DataFrame) -> pd.DataFrame:
    dt = pd.to_datetime(df["target_date"])
    df["year"] = dt.dt.year
    df["weekofyear"] = dt.dt.isocalendar().week.astype(int)
    df["month"] = dt.dt.month
    df["sin_week"] = np.sin(2 * np.pi * df["weekofyear"] / 52.0)
    df["cos_week"] = np.cos(2 * np.pi * df["weekofyear"] / 52.0)
    return df


def main():
    model = joblib.load(MODEL_PATH)
    with open(FEATURES_PATH, "r", encoding="utf-8") as f:
        features_used = json.load(f)

    fea = pd.read_csv(FEA, parse_dates=["target_date"])
    fea["target_date"] = _to_week_start_monday(fea["target_date"])
    fea = fea.sort_values(KEYS + ["target_date"])


    first_monday = BASE_DATE + timedelta(days=(7 - BASE_DATE.weekday()) % 7)
    future_dates = pd.date_range(first_monday, periods=HORIZON_WEEKS, freq=FORECAST_FREQ)
    
    lag_cols = [c for c in fea.columns if c.startswith("lag_")]
    ma_cols  = [c for c in fea.columns if c.startswith("ma_")]

    base = (
        fea.groupby(KEYS)
           .tail(60)[[
                "target_date", "actual_order_qty", *KEYS,
                "product_code",
                *lag_cols, *ma_cols,
                "avg_temp_c", "cdd", "hdd", "precip_mm",
                "heat_wave", "cold_wave",
                "year", "weekofyear", "month",
                "sin_week", "cos_week", "msrp_krw", "base_share"
            ]]
           .copy()
    )

    forecasts = []

    for grp, hist in base.groupby(KEYS):
        hist = hist.sort_values("target_date").copy()
        product_code = hist["product_code"].iloc[-1]
        buf = hist[["target_date", "actual_order_qty"]].copy()

        for dt in future_dates:
            row = {k: v for k, v in zip(KEYS, grp)}
            row["product_code"] = product_code
            row["target_date"] = dt

            temp = pd.DataFrame([row])
            temp = _add_time_features(temp)

            for c in lag_cols:
                k = int(c.split("_")[1])
                temp[c] = buf["actual_order_qty"].iloc[-k] if len(buf) >= k else 0.0

            for c in ma_cols:
                k = int(c.split("_")[1])
                temp[c] = buf["actual_order_qty"].tail(k).mean() if len(buf) > 0 else 0.0

            last_weather = hist[hist["target_date"] == hist["target_date"].max()].iloc[0]
            for c in ["avg_temp_c","cdd","hdd","precip_mm","heat_wave","cold_wave"]:
                temp[c] = last_weather[c]

            temp["msrp_krw"] = hist["msrp_krw"].iloc[-1]
            temp["base_share"] = hist["base_share"].iloc[-1]

            X = temp.reindex(columns=features_used, fill_value=0.0)

            y_pred = float(model.predict(X)[0])
            y_pred = max(0, y_pred)

            forecasts.append({**row, "y_pred": round(y_pred)})

            buf = pd.concat(
                [buf, pd.DataFrame([{"target_date": dt, "actual_order_qty": y_pred}])],
                ignore_index=True
            )

    out = pd.DataFrame(forecasts)
    out = out.sort_values(KEYS + ["target_date"]).reset_index(drop=True)
    out.to_csv(OUT, index=False)

    print(f"Forecast saved: {OUT} ({len(out):,} rows)")


if __name__ == "__main__":
    main()
