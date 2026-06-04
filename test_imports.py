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
    
    from app.core.database import init_pool, close_pool, db_conn
    print("[OK] Raw SQL database layer imported successfully.")

    from app.domains.auth.router import router as auth_router
    from app.domains.users.router import router as users_router
    from app.domains.loans.router import router as loans_router
    print("[OK] Active domain router modules imported successfully.")
    
    from app.main import app
    print("[OK] FastAPI application instance initialized and imported successfully.")
    
    print("\n[OK] ALL CORE MODULES IMPORTED AND INITIALISED CLEANLY WITH ZERO IMPORT OR SCRIPT SYNTAX ERRORS!")
    sys.exit(0)
except Exception as e:
    print("\n[ERROR] IMPORT ERROR DETECTED: " + str(e))
    import traceback
    traceback.print_exc()
    sys.exit(1)
