import os
import re
import json

source_path = r"E:\ZomboidDecompiler\bin\output\source\zombie"
html_path = r"d:\old\GitHub EXPANSION\ZomboidAPI\index.html"

# Enhanced Regex for parsing
class_pattern = re.compile(
    r'(?:public\s+|private\s+|protected\s+|abstract\s+|final\s+|static\s+)*'
    r'(class|interface|enum)\s+([A-Za-z0-9_]+)'
    r'(?:\s+extends\s+([A-Za-z0-9_,\s<>]+?))?'
    r'(?:\s+implements\s+([A-Za-z0-9_,\s<>]+))?\s*\{'
)

# We are intentionally tracking methods (funciones) and ignoring fields (objetos y demas)
method_pattern = re.compile(
    r'^\s*(?:public\s+|private\s+|protected\s+|abstract\s+|final\s+|static\s+|synchronized\s+|native\s+)*'
    r'(?:<[^>]+>\s+)?'
    r'([A-Za-z0-9_<>\[\]]+)?\s+([A-Za-z0-9_]+)\s*\(([^)]*)\)\s*(?:\{|throws|\;)'
)

api_data = {}
all_class_map = {}

# Check if the source path exists
if not os.path.exists(source_path):
    print(f"Error: {source_path} does not exist.")
    exit(1)

for root, dirs, files in os.walk(source_path):
    for file in files:
        if file.endswith('.java'):
            filepath = os.path.join(root, file)
            rel_dir = os.path.relpath(root, source_path)
            if rel_dir == '.':
                pkg = "zombie"
            else:
                pkg = "zombie." + rel_dir.replace(os.sep, '.')

            if pkg not in api_data:
                api_data[pkg] = []

            with open(filepath, 'r', encoding='utf-8', errors='ignore') as f:
                content = f.read()

            class_match = class_pattern.search(content)
            if class_match:
                class_type = class_match.group(1)
                class_name = class_match.group(2)
                extends_str = class_match.group(3)
                implements_str = class_match.group(4)

                extends = [e.strip() for e in extends_str.split(',')] if extends_str else []
                implements = [i.strip() for i in implements_str.split(',')] if implements_str else []

                fields = [] # "no objetos y demas"
                methods = []

                content_clean = re.sub(r'/\*.*?\*/', '', content, flags=re.DOTALL)
                
                # Track braces to only capture class level fields/methods
                lines = content_clean.split('\n')
                brace_level = 0
                class_declared = False
                
                for line in lines:
                    stripped = line.split('//')[0].strip()
                    if not stripped:
                        continue
                    
                    if ('class ' + class_name in stripped or 
                        'interface ' + class_name in stripped or 
                        'enum ' + class_name in stripped):
                        class_declared = True
                        
                    if class_declared:
                        brace_level += stripped.count('{') - stripped.count('}')
                    
                    if class_declared and brace_level == 1:
                        # Attempt to parse a method
                        mm = method_pattern.match(stripped)
                        if mm and "=" not in stripped.split("(")[0]:
                            ret_type = mm.group(1)
                            m_name = mm.group(2)
                            args = mm.group(3).strip()
                            
                            # Constructors
                            if not ret_type and m_name == class_name:
                                ret_type = ""
                            
                            if ret_type is not None:
                                if ret_type not in ["return", "new", "else", "if"] and m_name not in ["if", "for", "while", "else", "switch", "catch"]:
                                    res_args = ' '.join(args.split()).replace("<", "&lt;").replace(">", "&gt;")
                                    res_ret = ret_type.strip().replace("<", "&lt;").replace(">", "&gt;") if ret_type else class_name
                                    
                                    m_dict = {"return": res_ret, "name": m_name, "args": res_args}
                                    if m_dict not in methods:
                                        methods.append(m_dict)

                api_data[pkg].append({
                    "class": class_name,
                    "extends": extends,
                    "implements": implements,
                    "fields": fields, # Empty
                    "methods": methods
                })
                
                all_class_map[class_name] = pkg

import collections
api_data = collections.OrderedDict(sorted(api_data.items()))
for k in api_data:
    api_data[k] = sorted(api_data[k], key=lambda x: x['class'])

with open(html_path, 'r', encoding='utf-8') as f:
    html_content = f.read()

# Remove old apiData and allClassMap if they exist to prevent duplication
html_content = re.sub(r'const apiData = \{.*?\};\s*', '', html_content, flags=re.DOTALL)
html_content = re.sub(r'const allClassMap = \{.*?\};\s*', '', html_content, flags=re.DOTALL)

json_str_api = json.dumps(api_data)
json_str_map = json.dumps(all_class_map)

injection = f"""const apiData = {json_str_api};
        const allClassMap = {json_str_map};
        const i18n = {{"""

new_html = html_content.replace('const i18n = {', injection)

with open(html_path, 'w', encoding='utf-8') as f:
    f.write(new_html)

print("Updated HTML!")
