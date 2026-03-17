import os
import re

function_dir = 'engine/src/main/java/com/bearsnake/komodo/engine/functions'

for root, dirs, files in os.walk(function_dir):
    for file in files:
        if file.endswith('Function.java'):
            path = os.path.join(root, file)
            with open(path, 'r') as f:
                content = f.read()
            
            # Skip abstract classes
            if 'public abstract class' in content:
                continue
            
            class_name = file[:-5]
            if class_name in ['Function', 'SubFunction', 'JSubFunction', 'ASubFunction', 'StoreConstantFunction']:
                continue

            # Look for the INSTANCE declaration
            instance_decl = rf'    public static final {class_name} INSTANCE = new {class_name}\(\);'
            
            # Find the line with the INSTANCE declaration
            match = re.search(instance_decl, content)
            if match:
                # We want to ensure there's exactly one blank line before and after.
                # A blank line is a line that contains only whitespace.
                
                # Replace the INSTANCE declaration and all its surrounding whitespace
                # with one blank line, then the INSTANCE line, then another blank line.
                
                # Let's match the INSTANCE declaration plus any whitespace/newlines before and after it.
                # We'll use lookbehind/lookahead to avoid matching too much.
                
                # More robust way: replace the whole block starting from the class declaration to the constructor.
                
                pattern = rf'(public class {class_name}.*?\{{)\s*({instance_decl})\s*(.*?private {class_name}\(\))'
                # Note: re.DOTALL is needed to match across newlines
                replacement = rf'\1\n\n{instance_decl}\n\n\3'
                
                new_content = re.sub(pattern, replacement, content, flags=re.DOTALL)
                
                if new_content != content:
                    with open(path, 'w') as f:
                        f.write(new_content)
                    print(f'Fixed spacing in {file}')
