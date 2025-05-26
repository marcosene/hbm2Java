import re

def remove_specific_table_rows_and_notes(lines):
    new_lines = []
    i = 0
    while i < len(lines):
        line = lines[i]

        # Check for table rows to remove entirely
        # Row for <discriminator formula="..."> mapping to @DiscriminatorFormula
        if re.search(r"\|\s*`<class name=\"Payment\">\s*<id name=\"id\"/>\s*<discriminator formula=.*?type=\"string\"/>\s*.*?</td>", line, re.IGNORECASE) or \
           re.search(r"\|\s*`<class name=\"Payment\">\s*<id name=\"id\"/>\s*<discriminator formula=.*?type=\"string\"/>", line, re.IGNORECASE) : # Simpler match for the HBM snippet column
            # Skip this line and the next two (assuming 3 lines per row in some formats, or just skip until next header/end of table)
            # This is a heuristic; robust table parsing is complex.
            # We assume simple row-per-line formatting after the initial script transformations.
            
            # Try to consume the entire table row if it's multi-line formatted in the input
            current_line_stripped = line.strip()
            if current_line_stripped.startswith("|") and current_line_stripped.endswith("|"):
                # This is likely a single-line table row. Just skip it.
                i += 1
                continue
            elif current_line_stripped.startswith("|"):
                # This might be a multi-line row. Consume until the row ends.
                while not lines[i].strip().endswith("|") and i < len(lines) -1:
                    i+=1
                i+=1 # skip the line ending with "|"
                continue


        # Notes related to HBM <element> not being implemented within table cells
        # Example: | ... | ... | ... HBM <element> which would map to @ElementCollection ... is not implemented. |
        if re.search(r"HBM `<element>` which would map to `@ElementCollection`.*?is not implemented", line, re.IGNORECASE):
            line = re.sub(r"HBM `<element>` which would map to `@ElementCollection`.*?is not implemented\.", "", line, flags=re.IGNORECASE)
        if re.search(r"HBM `<element>` for mapping basic/embeddable value types \(which would typically use `@ElementCollection`\) is not currently implemented", line, re.IGNORECASE):
            line = re.sub(r"HBM `<element>` for mapping basic/embeddable value types \(which would typically use `@ElementCollection`\) is not currently implemented\.", "", line, flags=re.IGNORECASE)
        
        # Remove note about formula on discriminator not being processed for standard JPA
        if re.search(r"The `formula` attribute for `<discriminator>` is not typically processed for standard JPA annotations", line, re.IGNORECASE):
            line = re.sub(r"The `formula` attribute for `<discriminator>` is not typically processed for standard JPA annotations\.?", "", line, flags=re.IGNORECASE)

        # General phrases indicating non-implementation for targeted features
        # This needs to be targeted carefully to avoid removing useful notes.
        # For now, this is handled by the more specific removals above and the global note removal.

        # Cleanup empty cells or cells with only whitespace/leftover markup potentially
        if "|" in line:
            parts = line.split("|")
            for j in range(len(parts)):
                if j == 0 or j == len(parts) -1: # first and last part (empty due to split)
                    continue
                parts[j] = parts[j].strip()
                if parts[j] == "." or parts[j].lower() == "n/a": # if a cell becomes just a period or n/a
                    parts[j] = " " # Replace with a single space to maintain cell structure
            line = "|".join(parts)
            if line.strip() == "||" or line.strip() == "| |": # If a row becomes empty essentially
                i += 1
                continue # Skip adding this line

        new_lines.append(line)
        i += 1
    return new_lines

def remove_unimplemented_features(markdown_content):
    lines = markdown_content.splitlines()
    
    # 1. Remove standalone "Note on HBM <element> for Collections..."
    standalone_note_pattern = re.compile(
        r"\*\*Note on HBM `<element>` for Collections:\*\* The HBM `<element>` tag, which would typically be used for mapping collections of basic or embeddable types .*?is \*\*not implemented\*\* in this version of the `hbm2java` tool\..*?Therefore, direct mapping of such collections using `<element>` is not supported\.",
        re.DOTALL | re.IGNORECASE
    )
    # Apply this removal to the whole content first
    markdown_content = standalone_note_pattern.sub("", markdown_content)
    
    # Re-split lines after major block removal
    lines = markdown_content.splitlines()

    # 2. Remove HTML comments about non-processed attributes
    # and other specific "not typically processed" comments.
    comments_to_remove_patterns = [
        re.compile(r"<!-- schema, catalog, package attributes for hibernate-mapping are not typically processed for direct annotation generation\. -->", re.IGNORECASE),
        re.compile(r"<!-- Other attributes like 'schema', 'catalog', 'proxy', 'lazy', 'batch-size', 'select-before-update', 'optimistic-lock' for <class> are not typically processed.*? -->", re.IGNORECASE),
        re.compile(r"<!-- The 'column' attribute directly on <version> is not used for column name if a nested <column> is present\. -->", re.IGNORECASE),
        re.compile(r"<!-- Attributes like 'access', 'insert', 'update', 'optimistic-lock', 'node', 'embed-xml' for <component> are not typically processed.*? -->", re.IGNORECASE),
        re.compile(r"<!-- For <properties>, attributes like 'node', 'insert', 'update', 'optimistic-lock' are not typically processed.*? -->", re.IGNORECASE),
        re.compile(r"<!-- Removed update=\"false\" for relationships as it's not commonly used.*?For now, focusing on more direct mappings\. -->", re.IGNORECASE),
        re.compile(r"<!-- Union-subclass is covered in Class Mappings\. -->", re.IGNORECASE),
        re.compile(r"<!-- Other <cache> attributes like 'include' are not typically processed\. -->", re.IGNORECASE),
        re.compile(r"<!-- A nested <cache> element specifically for <natural-id> is not typically parsed.*? -->", re.IGNORECASE),
        re.compile(r"<!-- Only <return-scalar> is typically processed for standard JPA annotation generation from <sql-query> children\..*? -->", re.IGNORECASE),
        re.compile(r"<!-- Attributes for <query> and <sql-query> like .*? are not typically processed.*? -->", re.IGNORECASE),
        re.compile(r"<!-- Persister, filter-def, filter, optimistic-lock attribute on class are not implemented\. -->", re.IGNORECASE),
        re.compile(r"<!-- The following table was the \"Collection Element Mappings\" section\. It is removed because the <element> tag for basic/embeddable collections is not implemented\. -->", re.IGNORECASE),
        re.compile(r"<!-- @org.hibernate.annotations.ListIndexBase\(1\) \(if `base` attribute is present\) -->", re.IGNORECASE)

    ]
    
    temp_lines_after_comments = []
    for line in lines:
        modified_line = line
        for pattern in comments_to_remove_patterns:
            modified_line = pattern.sub("", modified_line)
        if modified_line.strip(): # Only add line if it's not empty after comment removal
            temp_lines_after_comments.append(modified_line)
    lines = temp_lines_after_comments

    # 3. Process table rows and notes (delegated to helper for clarity)
    lines = remove_specific_table_rows_and_notes(lines)

    # Filter out lines that become empty or are just whitespace after processing
    # Also remove multiple blank lines
    final_lines = []
    last_line_blank = False
    for line in lines:
        stripped_line = line.strip()
        if stripped_line:
            final_lines.append(line) # Keep original line with its spacing if not just whitespace
            last_line_blank = False
        elif not last_line_blank:
            final_lines.append("") # Add a single blank line
            last_line_blank = True
            
    # Ensure the document doesn't end with too many blank lines
    while final_lines and final_lines[-1].strip() == "":
        final_lines.pop()

    return "\n".join(final_lines)


if __name__ == "__main__":
    file_path = "HBM_to_JPA_Mapping_Guide.md"
    with open(file_path, "r", encoding="utf-8") as f:
        content = f.read()
    
    modified_content = remove_unimplemented_features(content)
    
    # Add a newline at the end if not present, for POSIX compliance
    if modified_content and not modified_content.endswith("\n"):
        modified_content += "\n"
        
    with open(file_path, "w", encoding="utf-8") as f:
        f.write(modified_content)
    
    print(f"Successfully removed mentions of unimplemented features from {file_path}")
