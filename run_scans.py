import urllib.request
import json
import os
import sys

PORT = 63653
BASE_URL = f"http://127.0.0.1:{PORT}"

FILES_TO_SCAN = [
    r"c:\Users\LENOVO\Desktop\FieldCRM\backend\app\main.py",
    r"c:\Users\LENOVO\Desktop\FieldCRM\backend\app\core\config.py",
    r"c:\Users\LENOVO\Desktop\FieldCRM\backend\app\core\security.py",
    r"c:\Users\LENOVO\Desktop\FieldCRM\backend\app\db\session.py",
    r"c:\Users\LENOVO\Desktop\FieldCRM\backend\app\db\models.py",
    r"c:\Users\LENOVO\Desktop\FieldCRM\backend\app\api\deps.py",
    r"c:\Users\LENOVO\Desktop\FieldCRM\backend\app\api\v1\auth.py",
    r"c:\Users\LENOVO\Desktop\FieldCRM\backend\app\api\v1\borrowers.py",
    r"c:\Users\LENOVO\Desktop\FieldCRM\backend\app\api\v1\communication.py",
    r"c:\Users\LENOVO\Desktop\FieldCRM\backend\app\api\v1\applications.py",
    r"c:\Users\LENOVO\Desktop\FieldCRM\backend\app\api\v1\collections.py",
    r"c:\Users\LENOVO\Desktop\FieldCRM\backend\app\api\v1\groups.py"
]

def make_post_request(url, data):
    req = urllib.request.Request(
        url, 
        data=json.dumps(data).encode("utf-8"),
        headers={"Content-Type": "application/json"}
    )
    with urllib.request.urlopen(req) as response:
        return json.loads(response.read().decode("utf-8"))

def make_get_request(url):
    with urllib.request.urlopen(url) as response:
        return json.loads(response.read().decode("utf-8"))

try:
    print("Checking SecureCoder Configuration...")
    config = make_get_request(f"{BASE_URL}/config")
    print("Active Backend:", config.get("scannerBackend"))
    
    if not config.get("scannerBackend"):
        print("No active scanner configured. Skipping scans.")
        sys.exit(0)
        
    total_findings = 0
    severity_counts = {"HIGH": 0, "MEDIUM": 0, "LOW": 0}
    cwe_categories = set()
    
    print("\nRunning Security Scans on Core Source Files...")
    for file_path in FILES_TO_SCAN:
        if not os.path.exists(file_path):
            print(f"Skipping missing file: {file_path}")
            continue
            
        print(f"Scanning: {os.path.basename(file_path)}...")
        scan_data = {"filePath": file_path}
        res = make_post_request(f"{BASE_URL}/scan", scan_data)
        
        findings = res.get("findings", [])
        for f in findings:
            severity = f.get("labels", {}).get("severity", "LOW")
            vuln_class = f.get("labels", {}).get("vulnerability_class", "Vulnerability")
            cwe = f.get("labels", {}).get("cwe", "N/A")
            msg = f.get("message", "")
            start_line = f.get("location", {}).get("range", {}).get("textRange", {}).get("startLine", 0)
            end_line = f.get("location", {}).get("range", {}).get("textRange", {}).get("endLine", 0)
            
            total_findings += 1
            severity_counts[severity] = severity_counts.get(severity, 0) + 1
            for c in cwe.split(","):
                cwe_categories.add(c.strip())
                
            print(f"\n**[{severity}] [{vuln_class}]**")
            print(f"File: {file_path}, Lines: {start_line}-{end_line}")
            print(f"CWE: {cwe}")
            print(f"Description: {msg}")
            
    print("\n========================================")
    print("SCAN COMPLETE SUMMARY:")
    print(f"Total Findings Detected: {total_findings}")
    print(f"Severity Breakdown: HIGH={severity_counts.get('HIGH', 0)}, MEDIUM={severity_counts.get('MEDIUM', 0)}, LOW={severity_counts.get('LOW', 0)}")
    print(f"Distinct CWE Classes: {', '.join(sorted(list(cwe_categories)))}")
    print("========================================")
    
except Exception as e:
    print("Scan Execution Failed:", e)
    sys.exit(1)
