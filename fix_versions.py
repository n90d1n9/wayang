import os
import re

root_dir = "/Users/bhangun/Workspace/workkayys/Products/Wayang/wayang-platform/Families/wayang"

for subdir, dirs, files in os.walk(root_dir):
    for file in files:
        if file == "pom.xml":
            filepath = os.path.join(subdir, file)
            # Skip the root pom
            if filepath == os.path.join(root_dir, "pom.xml"):
                continue
                
            with open(filepath, 'r') as f:
                content = f.read()
                
            modified = False
            
            # Fix version of wayang-agentic-parent
            pattern = r"(<artifactId>wayang-agentic-parent</artifactId>\s*<version>)[^<]+(</version>)"
            new_content = re.sub(pattern, r"\g<1>1.0.0-SNAPSHOT\g<2>", content)
            
            if new_content != content:
                content = new_content
                modified = True
                
            # Also fix skill-audit's parent
            if "gollek-agent-parent" in content:
                content = content.replace("<artifactId>gollek-agent-parent</artifactId>", "<artifactId>skill-parent</artifactId>")
                content = content.replace("<groupId>tech.kayys.gollek</groupId>", "<groupId>tech.kayys.wayang</groupId>")
                modified = True
                
            if modified:
                with open(filepath, 'w') as f:
                    f.write(content)
                print(f"Fixed version in {os.path.relpath(filepath, root_dir)}")
