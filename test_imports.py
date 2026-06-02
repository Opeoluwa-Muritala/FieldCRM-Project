import sys
import os

# Add backend directory to Python path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "backend"))

try:
    print("Testing importing core application modules...")
    from app.core.config import settings
    print("[OK] Config imported successfully. Project: " + str(settings.PROJECT_NAME))
    
    from app.core import security
    print("[OK] Security utilities imported successfully.")
    
    from app.db import session
    print("[OK] Database session configuration imported successfully.")
    
    from app.db import models
    print("[OK] Database models registered and imported successfully.")
    
    from app.api.v1 import auth, borrowers, communication, applications, collections, groups
    print("[OK] All API v1 router modules imported successfully.")
    
    from app.main import app
    print("[OK] FastAPI application instance initialized and imported successfully.")
    
    print("\n[OK] ALL CORE MODULES IMPORTED AND INITIALISED CLEANLY WITH ZERO IMPORT OR SCRIPT SYNTAX ERRORS!")
    sys.exit(0)
except Exception as e:
    print("\n[ERROR] IMPORT ERROR DETECTED: " + str(e))
    import traceback
    traceback.print_exc()
    sys.exit(1)
