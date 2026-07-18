#!/usr/bin/env python3
"""
Fix migration: Correct package paths for nested subdirectories
"""

import re
from pathlib import Path

def fix_package_declarations():
    """Fix incorrect package declarations."""
    wayang_base = Path("tech/kayys/wayang/agent/core")
    
    fixes = {
        "tech.kayys.wayang.agent.core.gamelan.graph.graph": 
            "tech.kayys.wayang.agent.core.gamelan.graph",
        
        "tech.kayys.wayang.agent.core.skills.adapter.adapter": 
            "tech.kayys.wayang.agent.core.skills.adapter",
        
        "tech.kayys.wayang.agent.core.skills.builtin.builtin": 
            "tech.kayys.wayang.agent.core.skills.builtin",
        
        "tech.kayys.wayang.agent.core.skills.loader.executor": 
            "tech.kayys.wayang.agent.core.skills.loader",
    }
    
    all_java_files = list(wayang_base.rglob("*.java"))
    changes = []
    
    for java_file in all_java_files:
        try:
            with open(java_file, 'r', encoding='utf-8') as f:
                content = f.read()
            
            original = content
            
            # Fix each incorrect package
            for wrong_pkg, correct_pkg in fixes.items():
                content = re.sub(
                    rf'package\s+{re.escape(wrong_pkg)}\s*;',
                    f'package {correct_pkg};',
                    content
                )
            
            if content != original:
                with open(java_file, 'w', encoding='utf-8') as f:
                    f.write(content)
                
                # Extract what was changed
                for wrong_pkg, correct_pkg in fixes.items():
                    if wrong_pkg in original:
                        changes.append(f"{java_file.relative_to(wayang_base.parent.parent)}")
                        break
        
        except Exception as e:
            print(f"Error processing {java_file}: {e}")
    
    return changes

def fix_imports():
    """Fix imports pointing to the wrong packages."""
    wayang_base = Path("tech/kayys/wayang/agent/core")
    
    fixes = {
        "import tech.kayys.wayang.agent.core.gamelan.graph.graph": 
            "import tech.kayys.wayang.agent.core.gamelan.graph",
        
        "import tech.kayys.wayang.agent.core.skills.adapter.adapter": 
            "import tech.kayys.wayang.agent.core.skills.adapter",
        
        "import tech.kayys.wayang.agent.core.skills.builtin.builtin": 
            "import tech.kayys.wayang.agent.core.skills.builtin",
        
        "import tech.kayys.wayang.agent.core.skills.loader.executor": 
            "import tech.kayys.wayang.agent.core.skills.loader",
    }
    
    all_java_files = list(wayang_base.rglob("*.java"))
    import_changes = []
    
    for java_file in all_java_files:
        try:
            with open(java_file, 'r', encoding='utf-8') as f:
                content = f.read()
            
            original = content
            
            for wrong, correct in fixes.items():
                if wrong in content:
                    content = content.replace(wrong, correct)
                    import_changes.append({
                        'file': str(java_file.relative_to(wayang_base.parent.parent)),
                        'old': wrong,
                        'new': correct
                    })
            
            if content != original:
                with open(java_file, 'w', encoding='utf-8') as f:
                    f.write(content)
        
        except Exception as e:
            print(f"Error processing {java_file}: {e}")
    
    return import_changes

if __name__ == "__main__":
    print("Fixing package declarations...")
    pkg_fixes = fix_package_declarations()
    print(f"  ✓ Fixed package declarations in {len(pkg_fixes)} files")
    
    print("\nFixing imports...")
    import_fixes = fix_imports()
    print(f"  ✓ Fixed imports in {len(import_fixes)} locations")
    
    if import_fixes:
        print("\n  Import fixes:")
        for fix in import_fixes:
            print(f"    {fix['file']}")
