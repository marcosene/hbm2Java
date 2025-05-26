import re

def modify_markdown_tables(markdown_content):
    new_lines = []
    lines = markdown_content.splitlines()
    i = 0
    while i < len(lines):
        line = lines[i]
        # Try to detect a table header
        if line.strip().startswith("|") and line.strip().endswith("|") and \
           i + 1 < len(lines) and lines[i+1].strip().startswith("|") and \
           lines[i+1].strip().endswith("|") and ":---" in lines[i+1]:

            header_line = lines[i]
            separator_line = lines[i+1]
            
            # Identify target columns
            header_cells = [cell.strip() for cell in header_line.strip("|").split("|")]
            try:
                jpa_col_idx = header_cells.index("JPA Annotation(s)")
                hibernate_col_idx = header_cells.index("Hibernate Annotation(s) (if needed)")
            except ValueError:
                # Columns not found, skip this table
                new_lines.append(line)
                i += 1
                continue

            # Modify header
            new_header_cells = header_cells[:jpa_col_idx] + \
                               ["JPA/Hibernate Annotation(s)"] + \
                               header_cells[hibernate_col_idx+1:]
            new_lines.append("| " + " | ".join(new_header_cells) + " |")

            # Modify separator
            separator_cells = [cell.strip() for cell in separator_line.strip("|").split("|")]
            # Estimate new column width or use a generic one
            # Simple approach: sum of old ones, or a fixed large one
            # For simplicity, let's make the new column separator generously wide
            new_separator_width = len("JPA/Hibernate Annotation(s)") + 20 # a bit of padding
            
            new_separator_cells = separator_cells[:jpa_col_idx] + \
                                  [":" + "-" * (new_separator_width -1) ] + \
                                  separator_cells[hibernate_col_idx+1:]
            new_lines.append("|" + "|".join(new_separator_cells) + "|")

            i += 2 # Move past header and separator

            # Process data rows
            while i < len(lines) and lines[i].strip().startswith("|") and lines[i].strip().endswith("|"):
                row_line = lines[i]
                row_cells = [cell.strip() for cell in row_line.strip("|").split("|")]

                if max(jpa_col_idx, hibernate_col_idx) < len(row_cells):
                    jpa_content = row_cells[jpa_col_idx].strip()
                    hibernate_content = row_cells[hibernate_col_idx].strip()

                    combined_content = ""
                    if jpa_content and hibernate_content and hibernate_content != "(No direct annotation)" and hibernate_content != "(No direct equivalent at package level)":
                        if jpa_content.endswith("<br/>") or jpa_content.endswith("<br>"):
                             combined_content = jpa_content + " " + hibernate_content
                        else:
                             combined_content = jpa_content + "<br/>" + hibernate_content
                    elif jpa_content:
                        combined_content = jpa_content
                    elif hibernate_content and hibernate_content != "(No direct annotation)" and hibernate_content != "(No direct equivalent at package level)":
                        combined_content = hibernate_content
                    else: # both empty or non-meaningful hibernate content
                        combined_content = jpa_content # or just one of them, or empty string

                    new_row_cells = row_cells[:jpa_col_idx] + \
                                    [combined_content] + \
                                    row_cells[hibernate_col_idx+1:]
                    new_lines.append("| " + " | ".join(new_row_cells) + " |")
                else:
                    # Malformed row or end of table content that looks like a row
                    new_lines.append(row_line)
                i += 1
            continue # continue to next line after table processing
        
        new_lines.append(line)
        i += 1
        
    return "\n".join(new_lines)

if __name__ == "__main__":
    file_path = "HBM_to_JPA_Mapping_Guide.md"
    with open(file_path, "r", encoding="utf-8") as f:
        content = f.read()
    
    modified_content = modify_markdown_tables(content)
    
    with open(file_path, "w", encoding="utf-8") as f:
        f.write(modified_content)
    
    print(f"Successfully modified tables in {file_path}")
