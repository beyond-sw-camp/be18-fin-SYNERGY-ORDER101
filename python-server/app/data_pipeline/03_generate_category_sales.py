"""
03_generate_category_sales.py
- 목적: 각 cat_low(20개)에 대해 realistic한 주간 판매량 생성
- 특징:
  * Base level (카테고리별 현실 수요 규모)
  * Seasonality S1 (중간 강도)
  * Trend T3 (카테고리별 장기 추세 반영)
  * Weather 영향 반영 (temperature, cdd/hdd 등)
  * Noise + randomness 포함
"""

import numpy as np
import pandas as pd
from pathlib import Path
from datetime import datetime, timedelta

BASE = Path(__file__).resolve().parent
WEATHER = BASE / "external_weather_weekly.csv"
OUT = BASE / "category_weekly_sales.csv"

# ---------------------------------------------------------
# 카테고리 리스트 (20개)
# ---------------------------------------------------------
CATEGORIES = [
    "TV","냉장고","세탁기","건조기","청소기","에어컨","공기청정기","전자레인지","오븐","식기세척기",
    "커피머신","토스터기","믹서기","노트북","데스크탑","모니터","프린터","라우터","스마트폰","스마트워치"
]

# ---------------------------------------------------------
# 1) Base weekly demand (카테고리 현실적 수요 규모)
#    연간 판매량 기준 → 주간으로 변환
# ---------------------------------------------------------
BASE_LEVEL = {
    "TV": 180,             # 대형가전 중간
    "냉장고": 150,
    "세탁기": 140,
    "건조기": 90,
    "청소기": 220,         # 스틱/로봇 다양 → 많음
    "에어컨": 120,         # 계절성 강함
    "공기청정기": 200,     # 계절 변동 큼
    "전자레인지": 260,
    "오븐": 70,
    "식기세척기": 80,
    "커피머신": 260,
    "토스터기": 180,
    "믹서기": 150,
    "노트북": 330,         # IT는 높음
    "데스크탑": 160,
    "모니터": 300,
    "프린터": 120,
    "라우터": 220,
    "스마트폰": 550,       # 가장 높음
    "스마트워치": 240,
}

# ---------------------------------------------------------
# 2) Trend 설정 (T3 현실형)
#    연도별 판매량 증가/감소율 (연간)
# ---------------------------------------------------------
TREND_RATE = {
    "TV": -0.01,
    "냉장고": -0.01,
    "세탁기": -0.015,
    "건조기": -0.01,
    "청소기": 0.01,
    "에어컨": 0.02,
    "공기청정기": 0.03,   # 변동성 큰 증가
    "전자레인지": 0.0,
    "오븐": 0.0,
    "식기세척기": 0.015,
    "커피머신": 0.015,
    "토스터기": 0.0,
    "믹서기": 0.0,
    "노트북": 0.04,
    "데스크탑": -0.01,
    "모니터": 0.02,
    "프린터": -0.005,
    "라우터": 0.03,
    "스마트폰": 0.05,
    "스마트워치": 0.04,
}

# ---------------------------------------------------------
# 3) Seasonality S1 (중간 강도)
# ---------------------------------------------------------

def seasonality(cat: str, week: int) -> float:
    w = week
    # 기본: 완만한 sin pattern
    base = 1 + 0.1 * np.sin(2 * np.pi * w / 52)

    # 카테고리 특수 시즌 패턴
    if cat == "에어컨":
        return base + 0.7 * np.exp(-0.5 * ((w - 30)/5)**2)        # 여름 peak

    if cat == "공기청정기":
        s1 = 0.5 * np.exp(-0.5 * ((w - 12)/5)**2)                # 봄 미세먼지
        s2 = 0.6 * np.exp(-0.5 * ((w - 48)/4)**2)                # 겨울
        return base + s1 + s2

    if cat == "TV":
        s1 = 0.3 * np.exp(-0.5 * ((w - 11)/4)**2)                # 학기 시작
        s2 = 0.5 * np.exp(-0.5 * ((w - 50)/4)**2)                # 연말 성수기
        return base + s1 + s2

    if cat == "노트북":
        return base + 0.4 * np.exp(-0.5 * ((w - 9)/4)**2)        # 신학기 peak

    if cat == "스마트폰":
        return base + 0.3 * np.exp(-0.5 * ((w - 38)/4)**2)       # 신제품 시즌

    if cat == "프린터":
        return base + 0.2 * np.exp(-0.5 * ((w - 3)/3)**2)        # 연초 수요 증가

    # 나머지는 기본만 적용
    return base

# ---------------------------------------------------------
# 4) Weather 영향 (B 방식)
# ---------------------------------------------------------

def weather_effect(row):
    temp = row["avg_temp_c"]
    cdd = row["cdd"]
    hdd = row["hdd"]

    # 기본 영향
    e = 1.0

    # 덥거나 추우면 에어컨/공기청정기/난방 종류에 영향
    e += 0.01 * cdd
    e += 0.01 * hdd

    return e

# ---------------------------------------------------------
# 날짜 생성
# ---------------------------------------------------------

def generate_weeks(start="2021-01-04", end="2025-12-29"):
    start = datetime.strptime(start, "%Y-%m-%d")
    end = datetime.strptime(end, "%Y-%m-%d")
    weeks = []
    cur = start
    while cur <= end:
        weeks.append(cur)
        cur += timedelta(weeks=1)
    return weeks

# ---------------------------------------------------------
# MAIN
# ---------------------------------------------------------

def main():
    weather = pd.read_csv(WEATHER, parse_dates=["target_date"])

    weeks = generate_weeks()
    rows = []

    for cat in CATEGORIES:
        base = BASE_LEVEL[cat]
        trend = TREND_RATE[cat]

        for dt in weeks:
            week_num = dt.isocalendar().week

            # trend 반영
            year_offset = dt.year - 2021
            trend_factor = (1 + trend) ** year_offset

            # 계절성
            sea = seasonality(cat, week_num)

            # weather join
            w = weather[weather["target_date"] == pd.Timestamp(dt)]
            if len(w) == 1:
                weather_mult = weather_effect(w.iloc[0])
            else:
                weather_mult = 1.0

            # final demand
            demand = base * trend_factor * sea * weather_mult

            # noise
            demand *= np.random.normal(1.0, 0.15)

            rows.append([1, "본사창고", 1, cat, dt.date(), max(0, int(demand))])

    df = pd.DataFrame(rows, columns=[
        "warehouse_id","region","store_id","cat_low","target_date","actual_order_qty"
    ])

    df.to_csv(OUT, index=False, encoding="utf-8-sig")
    print(f"Saved category_weekly_sales.csv → {OUT}")
    print(df.head(10))


if __name__ == "__main__":
    main()
