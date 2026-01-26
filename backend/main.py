from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from datetime import datetime
import json
import os

app = FastAPI()

REPORTS_FILE = "reports.json"

class StreamReport(BaseModel):
    station_id: str
    stream_url: str
    error_code: int
    error_message: str
    device_info: str
    app_version: str = "unknown"
    user_agent: str = "unknown"
    country: str = "unknown"
    timestamp: str = None
    report_count: int = 1

@app.post("/report")
async def report_issue(report: StreamReport):
    if not report.timestamp:
        report.timestamp = datetime.now().isoformat()
    
    report_data = report.dict()
    
    # Key fields for identifying "duplicate" reports
    dedupe_keys = ["station_id", "stream_url", "error_code", "device_info", "app_version", "user_agent", "country"]
    
    try:
        reports = []
        if os.path.exists(REPORTS_FILE):
            with open(REPORTS_FILE, "r") as f:
                reports = json.load(f)
        
        # Check for existing duplicate
        found = False
        for existing in reports:
            # Check if all dedupe keys match
            if all(existing.get(k) == report_data.get(k) for k in dedupe_keys):
                existing["report_count"] = existing.get("report_count", 1) + 1
                existing["timestamp"] = report_data["timestamp"]  # Update to latest occurrence
                existing["error_message"] = report_data["error_message"] # Update message if it changed
                found = True
                break
        
        if not found:
            reports.append(report_data)
        
        with open(REPORTS_FILE, "w") as f:
            json.dump(reports, f, indent=4)
            
        return {"status": "success", "message": "Report received and merged" if found else "Report received"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
