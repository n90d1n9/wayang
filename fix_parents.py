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
            
            # Replace wayang-client-parent and wayang-core-parent with wayang-agentic-parent
            if "<artifactId>wayang-client-parent</artifactId>" in content:
                content = content.replace("<artifactId>wayang-client-parent</artifactId>", "<artifactId>wayang-agentic-parent</artifactId>")
                modified = True
            if "<artifactId>wayang-core-parent</artifactId>" in content:
                content = content.replace("<artifactId>wayang-core-parent</artifactId>", "<artifactId>wayang-agentic-parent</artifactId>")
                modified = True
                
            if modified:
                with open(filepath, 'w') as f:
                    f.write(content)
                print(f"Fixed parent in {os.path.relpath(filepath, root_dir)}")
