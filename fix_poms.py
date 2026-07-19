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
                
            # If parent is wayang-agentic-parent, fix its relativePath
            if "<artifactId>wayang-agentic-parent</artifactId>" in content:
                # Calculate depth
                rel_path = os.path.relpath(filepath, root_dir)
                depth = rel_path.count(os.sep)
                correct_relative = "../" * depth + "pom.xml"
                
                # Replace existing relativePath or add it
                if "<relativePath>" in content:
                    content = re.sub(r"<relativePath>.*?</relativePath>", f"<relativePath>{correct_relative}</relativePath>", content)
                else:
                    # Insert after version
                    content = re.sub(r"(<artifactId>wayang-agentic-parent</artifactId>\s*<version>.*?</version>)", r"\1\n        <relativePath>" + correct_relative + "</relativePath>", content)
                    
                with open(filepath, 'w') as f:
                    f.write(content)
                print(f"Fixed {rel_path} -> {correct_relative}")

