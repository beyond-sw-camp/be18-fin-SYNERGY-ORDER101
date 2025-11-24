"""
06_export_to_demand_forecast.py (robust)
- predictions.csv에 ID 컬럼이 일부 없을 수 있으므로
  features_test.csv에서 ID를 보충해 최종 적재 파일을 만든다.
- 출력: demand_forecast_load.csv
"""
from pathlib import Path
import pandas as pd

BASE = Path(__file__).resolve().parent
PRED = BASE / "predictions.csv"
FTE  = BASE / "features_test.csv"
OUT  = BASE / "demand_forecast_load.csv"

ID_KEYS = ["warehouse_id", "store_id", "sku_id", "target_date"]

def main():
    pred = pd.read_csv(PRED)
    # y, y_pred는 필수
    if "y_pred" not in pred.columns:
        raise ValueError("predictions.csv에 y_pred 컬럼이 없습니다.")
    if "target_date" not in pred.columns:
        # 뒤에서 features_test로 보충하므로 통과

        pass

    # 필요시 features_test에서 ID 보충
    need = [c for c in ID_KEYS if c not in pred.columns]
    if need:
        fte = pd.read_csv(FTE, usecols=[c for c in ID_KEYS if c in pd.read_csv(FTE, nrows=0).columns])
        if len(fte) != len(pred):
            # 순서가 다르면 안전하게 인덱스로 정렬한 뒤 결합(우리는 파이프라인상 같음)
            fte = fte.reset_index(drop=True)
            pred = pred.reset_index(drop=True)
        for c in ID_KEYS:
            if c not in pred.columns and c in fte.columns:
                pred[c] = fte[c]
        missing_after = [c for c in ID_KEYS if c not in pred.columns]
        if missing_after:
            # 그래도 없으면 최소한의 기본값으로 채워서 진행
            for c in missing_after:
                if c == "target_date":
                    raise ValueError("target_date가 없어서 내보낼 수 없습니다.")
                pred[c] = 0

    # 스키마 맞추기 (운영 적재 형식 예시)
    out = pred.copy()
    out["product_id"]        = out.get("sku_id")
    out["warehouse_id"]      = out["warehouse_id"].fillna(0).astype(int)
    out["store_id"]          = out["store_id"].fillna(0).astype(int)
    out["target_date"]       = pd.to_datetime(out["target_date"]).dt.date.astype(str)
    out["forecast_qty"]      = out["y_pred"].round(0).astype(int)

    cols = ["warehouse_id","store_id","product_id","target_date","forecast_qty"]
    out = out[cols].sort_values(["target_date","warehouse_id","store_id","product_id"]).reset_index(drop=True)
    out.to_csv(OUT, index=False)
    print(f"saved: {OUT}")

if __name__ == "__main__":
    main()
