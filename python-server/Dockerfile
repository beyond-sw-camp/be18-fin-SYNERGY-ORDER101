# 1) 가벼운 Python 베이스 이미지
FROM python:3.13-slim

# 2) 필수 OS 패키지 설치
RUN apt-get update && \
    apt-get install -y --no-install-recommends \
        gcc g++ make libgomp1 libstdc++6 libffi-dev libssl-dev build-essential curl \
    && apt-get clean && rm -rf /var/lib/apt/lists/*

# 3) 작업 디렉토리
WORKDIR /app

# 4) poetry 설치
RUN pip install --upgrade pip && pip install poetry

# 5) dependencies 캐싱
COPY pyproject.toml poetry.lock* /app/

RUN poetry config virtualenvs.create false
RUN poetry install --without dev --no-root

# 6) 실제 앱 소스 복사
COPY app /app/app
COPY scripts /app/scripts

# data_pipeline 파일들 명시적으로 복사
COPY app /app/app

# FastAPI 실행 포트
EXPOSE 8000

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000", "--reload", "--loop", "asyncio"]
