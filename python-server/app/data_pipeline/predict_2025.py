#25년 1년치 예측치 만드는파일. 적재용
import pandas as pd
import numpy as np
from pathlib import Path
import joblib
import json

BASE = Path(__file__).resolve().parent.parent / "data_pipeline"

FEATURES = BASE / "features_all.csv"   
SKU = BASE / "sku_catalog_ml_with_share.csv"
MODEL = BASE / "lightgbm_model.pkl"
FEATURE_LIST = BASE / "lightgbm_features.json"

OUT = BASE / "prediction_2025.csv"

# 2025년 1년(월요일 기준)
FUTURE_DATES = pd.date_range(
    start="2025-01-06",
    end="2025-12-29",
    freq="W-MON"
)

def build_future_features():
    print("Loading features_all.csv ...")
    past = pd.read_csv(FEATURES, parse_dates=["target_date"])

    print("Loading SKU catalog...")
    sku = pd.read_csv(SKU)

    # 마지막 히스토리 기준으로 SKU별로 lag 기반 생성
    last_past = past.sort_values(["sku_id", "target_date"]).groupby("sku_id").tail(12)

    # future df 초안
    fdf = pd.DataFrame([{"target_date": d} for d in FUTURE_DATES])

    # ----------------------------
    # Weather mean 삽입 (여기 수정)
    # ----------------------------
    weather_cols = ["avg_temp_c", "cdd", "hdd", "precip_mm", "heat_wave", "cold_wave"]
    weather_mean = past[weather_cols].mean()

    for col in weather_cols:
        fdf[col] = weather_mean[col]

    # SKU cross join
    fdf["key"] = 0
    sku["key"] = 0
    fdf = fdf.merge(sku, on="key").drop(columns=["key"])

    # 기본 컬럼 지정
    fdf["warehouse_id"] = 1
    fdf["store_id"] = 1

    # SKU별로 lag 생성
    future_rows = []

    for sku_id, g in fdf.groupby("sku_id"):
        history = last_past[last_past["sku_id"] == sku_id].copy()
        hist_vals = list(history["actual_order_qty"].values)

        rows = []
        for date in FUTURE_DATES:
            lag_1 = hist_vals[-1]
            lag_2 = hist_vals[-2] if len(hist_vals) >= 2 else lag_1
            lag_4 = hist_vals[-4] if len(hist_vals) >= 4 else lag_1
            lag_8 = hist_vals[-8] if len(hist_vals) >= 8 else lag_1
            lag_12 = hist_vals[-12] if len(hist_vals) >= 12 else lag_1

            ma_4 = np.mean(hist_vals[-4:]) if len(hist_vals) >= 4 else lag_1
            ma_8 = np.mean(hist_vals[-8:]) if len(hist_vals) >= 8 else lag_1
            ma_12 = np.mean(hist_vals[-12:]) if len(hist_vals) >= 12 else lag_1

            rows.append([
                date, sku_id,
                lag_1, lag_2, lag_4, lag_8, lag_12,
                ma_4, ma_8, ma_12
            ])

            hist_vals.append(lag_1)
            if len(hist_vals) > 12:
                hist_vals.pop(0)

        tmp = pd.DataFrame(rows, columns=[
            "target_date", "sku_id",
            "lag_1", "lag_2", "lag_4", "lag_8", "lag_12",
            "ma_4", "ma_8", "ma_12"
        ])
        for col in weather_cols:
            tmp[col] = weather_mean[col]

        meta = sku[sku["sku_id"] == sku_id].iloc[0].to_dict()
        for k, v in meta.items():
            tmp[k] = v

        # time features
        tmp["year"] = tmp["target_date"].dt.year
        tmp["weekofyear"] = tmp["target_date"].dt.isocalendar().week.astype(int)
        tmp["month"] = tmp["target_date"].dt.month
        tmp["sin_week"] = np.sin(2 * np.pi * tmp["weekofyear"] / 52)
        tmp["cos_week"] = np.cos(2 * np.pi * tmp["weekofyear"] / 52)

        tmp["warehouse_id"] = 1
        tmp["store_id"] = 1

        future_rows.append(tmp)

    future_df = pd.concat(future_rows, ignore_index=True)
    return future_df


def main():
    print("\n=== [1] 미래 Feature 생성: 2025년 전체 ===")
    future_df = build_future_features()

    print("=== [2] 모델 로드 ===")
    model = joblib.load(MODEL)

    with open(FEATURE_LIST, "r", encoding="utf-8") as f:
        feature_cols = json.load(f)

    print("=== [3] 예측 ===")
    future_df["y_pred"] = model.predict(future_df[feature_cols])

    print("=== [4] 저장 ===")
    future_df.to_csv(OUT, index=False)
    print(f"[OK] Saved prediction → {OUT}")


if __name__ == "__main__":
    main()
