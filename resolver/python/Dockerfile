FROM python:3.11-slim AS base

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

WORKDIR /app

COPY requirements.txt /app/requirements.txt
RUN pip install --no-cache-dir --upgrade pip \
 && pip install --no-cache-dir -r requirements.txt

COPY . /app

# Defaults for demo; can be overridden
ENV LINKID_ENV=production \
    LINKID_HOST=0.0.0.0 \
    LINKID_PORT=8080 \
    LINKID_LOG_LEVEL=INFO \
    LINKID_SEED_FILE=seed.json

EXPOSE 8080

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8080"]


