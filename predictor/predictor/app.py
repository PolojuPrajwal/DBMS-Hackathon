"""Peak-hour demand predictor. Spring Boot posts historical entry timestamps; we forecast next-24h arrivals."""
from datetime import datetime, timedelta
import pandas as pd
from fastapi import FastAPI
from pydantic import BaseModel
from sklearn.ensemble import RandomForestRegressor

app = FastAPI(title="Parking Demand Predictor")

class Req(BaseModel):
    entries: list[str] = []
    total_slots: int = 20

def synthetic_history(days=28):
    """Cold-start data: morning and evening peaks, quieter on weekends."""
    start = datetime.now().replace(minute=0, second=0, microsecond=0) - timedelta(days=days)
    rows = []
    for h in range(days * 24):
        t = start + timedelta(hours=h)
        base = 2 + 8 * (8 <= t.hour <= 10) + 9 * (17 <= t.hour <= 19) + 4 * (11 <= t.hour <= 16)
        rows.append((t, max(0, base - (3 if t.weekday() >= 5 else 0))))
    return pd.DataFrame(rows, columns=["ts", "count"])

@app.post("/predict")
def predict(req: Req):
    real = None
    if req.entries:
        ts = pd.to_datetime(pd.Series(req.entries)).dt.floor("h")
        real = ts.value_counts().rename_axis("ts").reset_index(name="count")
    use_real = real is not None and len(real) >= 24
    df = real if use_real else synthetic_history()
    df["hour"], df["dow"] = df.ts.dt.hour, df.ts.dt.dayofweek
    model = RandomForestRegressor(n_estimators=100, random_state=42).fit(df[["hour", "dow"]], df["count"])
    now = datetime.now().replace(minute=0, second=0, microsecond=0)
    future = [now + timedelta(hours=i) for i in range(1, 25)]
    pred = model.predict(pd.DataFrame({"hour": [t.hour for t in future], "dow": [t.weekday() for t in future]}))
    peak = pred.max() * 0.8
    return {"source": "recorded entries" if use_real else "synthetic baseline (not enough data yet)",
            "forecast": [{"time": t.strftime("%a %H:00"), "expected_arrivals": round(float(p), 1),
                          "expected_occupancy_pct": min(100, round(float(p) / max(req.total_slots, 1) * 100)),
                          "peak": bool(p >= peak)} for t, p in zip(future, pred)]}
