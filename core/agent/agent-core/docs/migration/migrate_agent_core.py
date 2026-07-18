#!/usr/bin/env python3
"""
Migration script: tech.kayys.gollek.agent.* → tech.kayys.wayang.agent.core.*

This script:
1. Moves Java files from gollek.agent to wayang.agent.core
2. Updates package declarations
3. Updates all imports in moved and dependent files
4. Generates detailed migration report
"""

import os
import re
import shutil
from pathlib import Path
from collections import defaultdict
from typing import Dict, List, Tuple, Set

class AgentCoreMigration:
    def __init__(self):
        self.java_root = Path(".")
        self.gollek_base = self.java_root / "tech/kayys/gollek/agent"
        self.wayang_base = self.java_root / "tech/kayys/wayang/agent/core"
        
        # Domain to target mapping
        self.domain_mapping = {
            "orchestrator": "orchestration",
            "tools": "tools",
            "skills/adapter": "skills/adapter",
            "skills/builtin": "skills/builtin",
            "skills/loader": "skills/loader",
            "skills": "skills",
            "memory": "memory",
            "embedding": "inference",
            "inference": "inference",
            "audit": "observability",
            "observability": "observability",
            "security": "security",
            "client": "agent",
            "service": "agent",
            "core": "agent",
            "integration": "agent",
            "coordinator": "coordination",
            "resilience": "resilience",
            "recovery": "resilience",
            "gamelan/graph": "gamelan/graph",
            "gamelan": "gamelan",
            "hitl": "interaction",
            "checkpoint": "agent",
            "prompt": "prompt",
            "registry": "registry",
            "selector": "registry",
            "metrics": "registry",
        }
        
        self.migration_plan = []
        self.file_mappings = {}  # old_package -> new_package
        self.moved_files = []
        self.import_updates = []
        self.errors = []

    def plan_migration(self):
        """Create the migration plan."""
        all_java_files = sorted(self.gollek_base.rglob("*.java"))
        
        for java_file in all_java_files:
            rel_path = java_file.relative_to(self.gollek_base)
            
            # Find which domain this file belongs to
            domain = None
            target_subdir = None
            
            # Check longer paths first
            for key in sorted(self.domain_mapping.keys(), key=lambda x: -len(x.split('/'))):
                if rel_path.parts[0:len(key.split('/'))] == tuple(key.split('/')):
                    domain = key
                    target_subdir = self.domain_mapping[key]
                    break
            
            if domain:
                target_path = self.wayang_base / target_subdir / java_file.name
                self.migration_plan.append({
                    'source': java_file,
                    'source_rel': rel_path,
                    'domain': domain,
                    'target': target_path,
                    'target_rel': target_path.relative_to(self.wayang_base)
                })
        
        return len(self.migration_plan)

    def extract_package(self, java_file: Path) -> str:
        """Extract package declaration from Java file."""
        try:
            with open(java_file, 'r', encoding='utf-8') as f:
                for line in f:
                    match = re.match(r'package\s+([\w\.]+)\s*;', line)
                    if match:
                        return match.group(1)
        except Exception as e:
            self.errors.append(f"Error reading package from {java_file}: {e}")
        return None

    def calculate_new_package(self, old_package: str, domain: str, target_subdir: str) -> str:
        """Calculate new package path based on old package and target subdirectory."""
        # Old: tech.kayys.gollek.agent.X.Y.Z
        # New: tech.kayys.wayang.agent.core.Y.Z
        
        # Remove the gollek.agent.X prefix
        parts = old_package.split('.')
        if 'gollek' in parts and 'agent' in parts:
            idx = parts.index('agent')
            # Skip domain name (e.g., 'orchestrator')
            after_domain = idx + 2  # skip 'agent' and domain name
            remaining = parts[after_domain:] if after_domain < len(parts) else []
            
            # Build new package
            new_pkg = f"tech.kayys.wayang.agent.core.{target_subdir.replace('/', '.')}"
            if remaining:
                new_pkg += "." + ".".join(remaining)
            return new_pkg
        
        return None

    def execute_migration(self):
        """Execute the migration."""
        for item in self.migration_plan:
            source_file = item['source']
            target_file = item['target']
            domain = item['domain']
            target_subdir = item['target_rel'].parent
            
            # Create target directory if needed
            target_file.parent.mkdir(parents=True, exist_ok=True)
            
            # Read source file
            try:
                with open(source_file, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                # Extract old package
                old_package = self.extract_package(source_file)
                if not old_package:
                    self.errors.append(f"Could not extract package from {source_file}")
                    continue
                
                # Calculate new package
                new_package = self.calculate_new_package(old_package, domain, 
                                                         self.domain_mapping[domain])
                if not new_package:
                    self.errors.append(f"Could not calculate new package for {source_file}")
                    continue
                
                # Update package declaration
                content = re.sub(
                    rf'package\s+{re.escape(old_package)}\s*;',
                    f'package {new_package};',
                    content
                )
                
                # Store mapping for import updates
                self.file_mappings[old_package] = new_package
                
                # Write to target
                with open(target_file, 'w', encoding='utf-8') as f:
                    f.write(content)
                
                self.moved_files.append({
                    'source': source_file,
                    'target': target_file,
                    'old_package': old_package,
                    'new_package': new_package
                })
                
            except Exception as e:
                self.errors.append(f"Error processing {source_file}: {e}")

    def update_imports(self):
        """Update imports in all Java files."""
        # Find all Java files in wayang.agent.core
        all_java_files = list(self.wayang_base.rglob("*.java"))
        
        # Also update any other Java files that might import from gollek.agent
        # (but be careful not to change non-agent gollek imports)
        
        for java_file in all_java_files:
            try:
                with open(java_file, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                original_content = content
                
                # Update imports for each mapped package
                for old_pkg, new_pkg in self.file_mappings.items():
                    # Import statements: import tech.kayys.gollek.agent.X.*;
                    old_import = f"import {old_pkg}."
                    new_import = f"import {new_pkg}."
                    
                    if old_import in content:
                        content = content.replace(old_import, new_import)
                    
                    # Specific class imports: import tech.kayys.gollek.agent.X.ClassName;
                    old_import_class = f"import {old_pkg};"
                    new_import_class = f"import {new_pkg};"
                    if old_import_class in content:
                        content = content.replace(old_import_class, new_import_class)
                
                if content != original_content:
                    with open(java_file, 'w', encoding='utf-8') as f:
                        f.write(content)
                    
                    self.import_updates.append({
                        'file': str(java_file),
                        'changes': sum(1 for line in content.split('\n') 
                                      if 'tech.kayys.wayang.agent.core' in line)
                    })
            
            except Exception as e:
                self.errors.append(f"Error updating imports in {java_file}: {e}")

    def generate_report(self) -> str:
        """Generate detailed migration report."""
        lines = [
            "=" * 100,
            "AGENT-CORE MIGRATION REPORT",
            "=" * 100,
            "",
            f"Migration Date: {__import__('datetime').datetime.now().isoformat()}",
            f"Source Namespace: tech.kayys.gollek.agent",
            f"Target Namespace: tech.kayys.wayang.agent.core",
            "",
            "=" * 100,
            "SUMMARY",
            "=" * 100,
            f"Total files migrated: {len(self.moved_files)}",
            f"Total packages updated: {len(self.file_mappings)}",
            f"Total files with import updates: {len(self.import_updates)}",
            f"Errors encountered: {len(self.errors)}",
            "",
        ]
        
        # Group by domain
        by_domain = defaultdict(list)
        for moved in self.moved_files:
            domain = None
            for d in self.domain_mapping.keys():
                if f"/{d}/" in str(moved['source']) or str(moved['source']).endswith(f"/{d}"):
                    domain = d
                    break
            if domain:
                by_domain[domain].append(moved)
        
        lines.append("=" * 100)
        lines.append("MIGRATION BY DOMAIN")
        lines.append("=" * 100)
        for domain in sorted(by_domain.keys()):
            target = self.domain_mapping[domain]
            files = by_domain[domain]
            lines.append(f"\n{domain} → {target}")
            lines.append(f"  Files: {len(files)}")
            for moved in sorted(files, key=lambda x: str(x['source'])):
                lines.append(f"    {moved['source'].relative_to(self.gollek_base)}")
        
        lines.append("\n" + "=" * 100)
        lines.append("PACKAGE MAPPINGS")
        lines.append("=" * 100)
        for old_pkg in sorted(self.file_mappings.keys()):
            new_pkg = self.file_mappings[old_pkg]
            lines.append(f"{old_pkg}")
            lines.append(f"  → {new_pkg}")
        
        if self.errors:
            lines.append("\n" + "=" * 100)
            lines.append("ERRORS")
            lines.append("=" * 100)
            for error in self.errors:
                lines.append(f"  ✗ {error}")
        
        lines.append("\n" + "=" * 100)
        lines.append("NEXT STEPS")
        lines.append("=" * 100)
        lines.append("1. Review moved files in tech/kayys/wayang/agent/core/")
        lines.append("2. Run 'mvn clean compile' to check for any compilation errors")
        lines.append("3. Run tests: 'mvn test'")
        lines.append("4. Once verified, delete tech/kayys/gollek/agent/ directory")
        lines.append("   (marked for review first as per instructions)")
        lines.append("")
        
        return "\n".join(lines)


def main():
    migration = AgentCoreMigration()
    
    print("Planning migration...")
    count = migration.plan_migration()
    print(f"  ✓ Planned migration of {count} files")
    
    print("\nExecuting migration...")
    migration.execute_migration()
    print(f"  ✓ Moved {len(migration.moved_files)} files")
    
    if migration.errors:
        print(f"  ⚠ {len(migration.errors)} errors during migration")
    
    print("\nUpdating imports...")
    migration.update_imports()
    print(f"  ✓ Updated imports in {len(migration.import_updates)} files")
    
    # Generate report
    report = migration.generate_report()
    print("\n" + report)
    
    # Save report
    report_file = Path("MIGRATION_REPORT.txt")
    with open(report_file, 'w', encoding='utf-8') as f:
        f.write(report)
    
    print(f"\nDetailed report saved to: {report_file}")


if __name__ == "__main__":
    main()
