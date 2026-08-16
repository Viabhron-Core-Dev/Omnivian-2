import os
import urllib.request
import json

api_key = os.environ.get('GEMINI_API_KEY')
if not api_key:
    with open('applet/.env.example', 'r') as f:
        for line in f:
            if line.startswith('GEMINI_API_KEY='):
                api_key = line.split('=')[1].strip().strip('"')
                break

models = ['gemini-3.1-flash', 'gemini-3.1-pro', 'gemini-2.5-flash']

for model in models:
    url = f"https://generativelanguage.googleapis.com/v1beta/models/{model}:generateContent?key={api_key}"
    payload = json.dumps({
        "contents": [{"parts": [{"text": "Hello"}]}]
    }).encode('utf-8')
    req = urllib.request.Request(url, data=payload, headers={'Content-Type': 'application/json'})
    try:
        response = urllib.request.urlopen(req)
        print(f"{model}: {response.getcode()}")
    except urllib.error.HTTPError as e:
        print(f"{model}: {e.code}")
        print(e.read().decode())
