import json
import re

with open('d:/old/GitHub EXPANSION/ZomboidAPI/index.html', 'r', encoding='utf-8') as f:
    content = f.read()

script_start = content.find('<script>')
if script_start != -1:
    script = content[script_start:]
    
    # Extract const apiData = {...};
    pattern = re.compile(r'const apiData = (\{.*?\});(?:\r?\n|$)', re.DOTALL)
    match = pattern.search(script)
    if match:
        data = match.group(1)
        try:
            json.loads(data)
            print("JSON is valid!")
        except Exception as e:
            print("JSON ERROR at:", e)
    else:
        print("Could not find apiData assignment")
else:
    print("Could not find <script>")
