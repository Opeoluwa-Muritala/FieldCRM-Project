import urllib.request
import urllib.error

url = "http://127.0.0.1:8000/login"
print(f"Testing connection to {url}...")
try:
    with urllib.request.urlopen(url, timeout=5) as response:
        print(f"Response status code: {response.status}")
        print("Response headers:")
        for header, val in response.getheaders():
            print(f"  {header}: {val}")
        html = response.read().decode('utf-8')
        print(f"Response body length: {len(html)} characters")
except urllib.error.URLError as e:
    print(f"Connection failed: {e}")
except Exception as e:
    print(f"Unexpected error: {e}")
