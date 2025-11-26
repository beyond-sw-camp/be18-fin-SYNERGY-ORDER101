from pathlib import Path
import pandas as pd
import numpy as np
from datetime import datetime, timedelta

BASE = Path(__file__).resolve().parent
OUT  = BASE / "external_weather_weekly.csv"

# ---------------------------------------------
# 설정
# ---------------------------------------------
START = datetime(2021, 1, 4)   # 첫 번째 월요일
END   = datetime(2025, 12, 29) # 마지막 월요일

REGION = "본사창고"

np.random.seed(42)

# 계절성 함수
def seasonal_temp(day_of_year):
    """
    일자 기반 기온 곡선 (한국형 탄젠트/사인 기반)
    - 여름 7~8월: 최고
    - 겨울 1~2월: 최저
    """
    return 15 + 10 * np.sin(2 * np.pi * (day_of_year - 200) / 365)

def seasonal_precip(day_of_year):
    """
    강수량 시계열
    - 장마철(6~7월) 집중 강우
    """
    base = 3 + 2 * np.sin(2 * np.pi * (day_of_year - 170) / 365)
    noise = np.random.normal(0, 1)
    return max(0, base + noise)

def compute_degree_days(avg_t):
    # Cooling degree days (기준 24도)
    # Heating degree days (기준 18도)
    cdd = max(0, avg_t - 24)
    hdd = max(0, 18 - avg_t)
    return cdd, hdd

def week_range_iter(start, end):
    cur = start
    while cur <= end:
        yield cur
        cur += timedelta(days=7)

# ---------------------------------------------
# weekly synthetic weather 생성
# ---------------------------------------------
def main():
    rows = []

    for monday in week_range_iter(START, END):
        week_days = [monday + timedelta(days=i) for i in range(7)]

        temps = []
        precs = []

        for d in week_days:
            doy = d.timetuple().tm_yday
            temp = seasonal_temp(doy) + np.random.normal(0, 1.5)
            prec = seasonal_precip(doy)
            temps.append(temp)
            precs.append(prec)

        avg_temp = round(np.mean(temps), 1)
        total_precip = round(np.sum(precs), 1)

        # degree days
        cdd, hdd = compute_degree_days(avg_temp)
        cdd = round(cdd, 1)
        hdd = round(hdd, 1)

        # heat wave flag (일일 최고기온 가정)
        heat_wave = 1 if np.max(temps) >= 33 else 0

        # cold wave flag
        cold_wave = 1 if np.min(temps) <= -8 else 0

        rows.append([
            REGION,
            monday.strftime("%Y-%m-%d"),
            avg_temp,
            cdd,
            hdd,
            total_precip,
            heat_wave,
            cold_wave
        ])

    df = pd.DataFrame(rows, columns=[
        "region",
        "target_date",
        "avg_temp_c",
        "cdd",
        "hdd",
        "precip_mm",
        "heat_wave",
        "cold_wave"
    ])

    df.to_csv(OUT, index=False, encoding="utf-8-sig")
    print(f"Saved synthetic weekly weather → {OUT}")
    print(df.head(10).to_string(index=False))
    print(f"Total weeks: {len(df)}")

if __name__ == "__main__":
    main()
