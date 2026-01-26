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
    timestamp: str = None

@app.post("/report")
async def report_issue(report: StreamReport):
    if not report.timestamp:
        report.timestamp = datetime.now().isoformat()
    
    report_data = report.dict()
    
    # Simple file-based storage for reports
    try:
        reports = []
        if os.path.exists(REPORTS_FILE):
            with open(REPORTS_FILE, "r") as f:
                reports = json.load(f)
        
        reports.append(report_data)
        
        with open(REPORTS_FILE, "w") as f:
            json.dump(reports, f, indent=4)
            
        return {"status": "success", "message": "Report received"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
