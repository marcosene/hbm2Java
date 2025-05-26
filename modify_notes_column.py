import re

def simplify_notes(note_text, hbm_snippet, jpa_hibernate_annotations):
    """
    Simplifies the note text by removing redundant information based on HBM and JPA columns.
    """
    if not note_text or note_text.strip() == 'N/A':
        return ""

    simplified_note = note_text
    
    # Patterns for information that is usually crucial and should be kept,
    # even if it superficially matches a removal pattern.
    # These often describe *how* parts of HBM map to parts of an annotation.
    keep_patterns = [
        r"HBM `<param name=\".*?\">` maps to `@Parameter\(name=\".*?\"\)`",
        r"HBM `<param name=\".*?\">` is parsed to extract .*? for `@Parameter\(name=\".*?\"\)`",
        r"The `name` attribute of `@GenericGenerator` and `generator` attribute of `@GeneratedValue` are dynamically generated.*?\.",
        r"The placeholder `\".*?\"` is used here for illustration\.",
        r"If the generator class is not a known standard one, it may default to.*?\.",
        r"See the \*\*Note\*\* at the beginning of this \".*?\" section regarding .*?\.",
        r"This setup implies a shared primary key scenario\.",
        r"If the property's `type` refers to an `@Embeddable` class, the JPA output often uses `@Embedded` and `@AttributeOverrides`.*?\.",
        r"If the `type` is a Hibernate UserType that handles multiple columns, Hibernate's `@Type` annotation along with `@Columns` would be used\.",
        r"For example, \".*?\" maps to `CascadeType.PERSIST` and `CascadeType.MERGE`\.",
        r"Since `FetchType.EAGER` is the JPA default for `@ManyToOne`, the `fetch=FetchType.EAGER` attribute is often omitted.*?\.",
        r"For `@ManyToOne`, this implies LAZY fetching unless `lazy=\"false\"` is also present\.",
        r"Since JPA's default for `@ManyToOne` is EAGER, `fetch=FetchType.LAZY` will be generated if the HBM indicates lazy fetching\.",
        r"As this is the JPA default for `@ManyToOne`, the `fetch=FetchType.EAGER` attribute is often omitted.*?\.",
        r"If the entity's ID is derived from this relationship, `@MapsId` is used\.",
        r"In a shared PK scenario, the join column for the relationship also serves as the primary key\.",
        r"If `property-ref` indicates the `mappedBy` property on the target side.*?\.",
        r"HBM's default for collections is `lazy=\"true\"`\.",
        r"JPA's default for `@OneToMany` and `@ManyToMany` is `FetchType.LAZY`\.",
        r"If `lazy=\"true\"` \(or omitted\) in HBM, the generated JPA `fetch=FetchType.LAZY` attribute is often omitted.*?\.",
        r"The HBM `fetch` attribute influences the `FetchType` on the JPA relationship annotation\.",
        r"If the HBM `fetch` strategy \(combined with `lazy`\) results in a JPA default.*?\.",
        r"Specific content of set \(e\.g\. `<one-to-many>`\) determines full mapping\.", # Keep reference to how mapping is determined
        r"HBM `<element>` is not implemented", # Important note about what's NOT supported
        r"Presence of `<list-index>` \(covered later\) adds `@OrderColumn`\.",
        r"The `schema` and `catalog` attributes on HBM `<map>` are not implemented for annotation generation\.",
        r"Key mapping is via `<map-key>` \(to `@MapKeyColumn`\) or `<map-key-many-to-many>`\.",
        r"Value mapping for entity types is via `<one-to-many>` or `<many-to-many>`\.",
        r"If a nested `<column>` is not present, the discriminator column name defaults to.*?\.",
        r"The `formula` attribute for `<discriminator>` is not typically processed for standard JPA annotations\.",
        r"This specifies the foreign key column in the subclass table that joins to the primary key of the superclass table\.",
        r"This is different from the `pkJoinColumns` attribute used with `@SecondaryTable`\.",
        r"These elements are processed during class and entity parsing\.",
        r"The `region` attribute is not typically processed for standard JPA caching annotations\.",
        r"Nested `<property>` and `<many-to-one>` elements are parsed to become part of the natural ID\.",
        r"The `type` on `@ColumnResult` can be set based on the HBM type\.",
        r"Only `<return-scalar>` is typically processed for standard JPA annotation generation.*?\.",
        r"Other elements like `<return alias=\".*?\" class=\".*?\">` or `<return-join.*?>` might require more complex.*?\.",
        r"Attributes for `<query>` and `<sql-query>` like .*? are not typically processed.*?\.",
        r"If a formula was used, it's not a standard JPA feature and would be handled by Hibernate-specific annotations.*?\.",
        r"Subclasses might need `@AttributeOverride` for inherited properties if column names need to be specified per subclass table.*?\.",
        r"`hbm2java` typically adds these to the relevant entity class\.",
        r"A `resultSetMapping` name is generated\.",
        r"If the join returns another full entity, it would be a separate `@EntityResult`\.",
        r"`hbm2java` derives the Java type from HBM type\.",
        r"If a `<cache>` element is associated with the natural ID in HBM.*?\.",
        r"`hbm2java` handles this\.",
        r"`region` is a separate attribute on Hibernate's `@Cache`\.",
        r"`optimistic-lock=\".*?\"` \(default\) implies use of a `<version>` property\.",
        r"`optimistic-lock=\".*?\"` would check all fields.*?\.",
        r"`optimistic-lock=\".*?\"` checks only modified fields.*?\.",
        r"JPA default with `@Version` is similar to \"version\"\.",
        r"`hbm2java` reflects this in generated code, but no specific annotation for \"version\" itself beyond `@Version` on a property\.",
        r"`hbm2java` maps `<filter-def>` to `@FilterDef` .*? and `<filter>` to `@Filter`.*?\."

    ]

    patterns_to_remove = [
        # Direct restatements of mapping
        r"The `.*?` attribute maps to `@.*?(\(name=.*?\))?`\.", # handles @Table(name=...)
        r"`.*?` maps to `@.*?`\.",
        r"The `.*?` attribute on `<.*?>` maps to `@.*?`\.",
        r"The `.*?` attribute becomes the field name\.",
        r"The `.*?` attribute determines the Java field type.*?\.",
        r"The `name` attribute determines the Fully Qualified Class Name \(FQCN\)\.",
        r"`name` is field name\.",
        r"`type` determines Java field type\.",
        r"maps to a field .*? annotated with `@Version`\.",
        r"The `column` attribute on `<id>` maps to `@Column\(name=...\)\`\.",
        r"A nested `<column>` tag within `<id>` also defines the column name\.",
        r"`generator class=\".*?\"` maps to `GenerationType\..*?`\.",
        r"The `name` and `type` are read from `<version>`\.",
        r"The `name` and `type` are read from the `<version>` tag\.",
        r"The column name \".*?\" is taken from the nested `<column>` tag\.",
        r"`name` is field name\. `type` determines Java field type\.",
        r"`column` attribute maps to `@Column\(name=...\)\`\.",
        r"`length` attribute maps to `@Column\(length=...\)\`\.",
        r"`update` attribute maps to `updatable=false`\.",
        r"`not-null=\"true\"` on nested `<column>` maps to `nullable=false`\.",
        r"`unique=\"true\"` on nested `<column>` maps to `unique=true`\.",
        r"The `sql-type` attribute on a nested `<column>` maps to `columnDefinition` in `@Column`\.",
        r"`precision` and `scale` attributes on a nested `<column>` are mapped to the corresponding attributes in `@Column`\.",
        r"The `name` and `class` attributes are read for the component\.",
        r"Nested `<property>` elements are parsed accordingly\.",
        r"The component `class` becomes an `@Embeddable` Java class\.",
        r"`name` is field name, `class` is target entity\.",
        r"The `column` attribute maps to `@JoinColumn\(name=...\)\`\.",
        r"The `not-null=\"true\"` attribute maps to `nullable=false` on `@JoinColumn`\.",
        r"The `unique=\"true\"` attribute maps to `unique=true` on `@JoinColumn`\.",
        r"The `foreign-key` attribute specifies a custom foreign key constraint name\.",
        r"The `access=\"field\"` attribute forces field-based access for this property\.",
        r"`name` and `class` attributes define the relationship as usual\.",
        r"The `name` attribute and the CDATA content for the HQL query are read\.",
        r"The `name` attribute and CDATA content for the SQL query are read\.",
        r"The `mutable` attribute of `<natural-id>` is read\.",
        r"The `usage` attribute from a `<cache>` element directly under `<class>` is read during entity parsing\.",
        r"The `usage` attribute from a `<cache>` element nested within a collection tag is read during collection parsing\."
    ]
    
    # Split note into sentences/parts to process individually.
    # Using positive lookbehind for period, or <br/>, to keep delimiters for reconstruction if needed.
    sentences = re.split(r'(?<=\.)\s+|(\s*<br/>\s*)', simplified_note)
    
    kept_sentences = []
    temp_buffer = "" # To accumulate parts of a sentence split by <br/>

    for part in sentences:
        if part is None: continue
        
        current_segment = part.strip()
        is_br = bool(re.fullmatch(r"\s*<br/>\s*", part, re.IGNORECASE))

        if not is_br:
            temp_buffer += (" " + current_segment if temp_buffer and current_segment else current_segment)
        
        # Process when we hit a sentence end (period) or a <br/> tag, or end of parts
        if (current_segment.endswith(".") and not is_br) or is_br or (sentences.index(part) == len(sentences) -1 and temp_buffer) :
            sentence_to_process = temp_buffer.strip()
            temp_buffer = "" # Reset buffer

            if not sentence_to_process:
                if is_br and kept_sentences and not kept_sentences[-1].endswith("<br/>"): # Avoid double <br/>
                    kept_sentences.append("<br/>")
                continue

            is_valuable = False
            for kp in keep_patterns:
                if re.search(kp, sentence_to_process, re.IGNORECASE):
                    is_valuable = True
                    break
            
            if is_valuable:
                kept_sentences.append(sentence_to_process)
            else:
                # Apply removal patterns
                modified_sentence = sentence_to_process
                for rp in patterns_to_remove:
                    modified_sentence = re.sub(rp, "", modified_sentence, flags=re.IGNORECASE).strip()
                
                # Further cleanup of common remnants if not valuable
                if modified_sentence.lower() in ["the.", "a.", "the", "a", ".", ","]:
                    modified_sentence = ""
                
                if modified_sentence:
                    kept_sentences.append(modified_sentence)
            
            if is_br and kept_sentences and kept_sentences[-1] != "<br/>": # Add the <br/> that triggered processing
                 # Ensure not to add <br/> if the last kept sentence was already one, or if current kept is empty
                if kept_sentences[-1]: # only add br if there's preceding content
                    kept_sentences.append("<br/>")

    # Reconstruct the note
    # Join sentences, then clean up <br/> tags
    final_note_parts = []
    for i, s in enumerate(kept_sentences):
        s_stripped = s.strip()
        if s_stripped == "<br/>":
            # Only add <br/> if previous isn't <br/> and there's a next non-<br/> part
            if final_note_parts and final_note_parts[-1].strip() != "<br/>" and i + 1 < len(kept_sentences) and kept_sentences[i+1].strip() != "<br/>":
                final_note_parts.append(" <br/> ")
        elif s_stripped:
            final_note_parts.append(s_stripped)

    simplified_note = "".join(final_note_parts).strip()
    # Consolidate multiple <br/> and fix spacing
    simplified_note = re.sub(r"(\s*<br/>\s*){2,}", " <br/> ", simplified_note) 
    simplified_note = re.sub(r"\s+\.", ".", simplified_note) # " ." -> "."
    simplified_note = re.sub(r"\s+,", ",", simplified_note) # " ," -> ","
    simplified_note = simplified_note.replace("..", ".").replace(". .", ".")
    simplified_note = simplified_note.strip(" .,<br/>")


    return simplified_note


def modify_markdown_notes(markdown_content):
    new_lines = []
    lines = markdown_content.splitlines()
    i = 0
    
    header_indices = {}

    while i < len(lines):
        line = lines[i]
        
        is_table_header_row = line.strip().startswith("|") and \
                              line.strip().endswith("|") and \
                              i + 1 < len(lines) and \
                              lines[i+1].strip().startswith("|") and \
                              ":---" in lines[i+1]

        if is_table_header_row:
            header_line = lines[i]
            header_cells = [cell.strip() for cell in header_line.strip("|").split("|")]
            try:
                # Assuming previous script correctly merged JPA/Hibernate cols
                header_indices['hbm'] = header_cells.index("HBM XML Snippet")
                header_indices['jpa_hib'] = header_cells.index("JPA/Hibernate Annotation(s)")
                header_indices['notes'] = header_cells.index("Notes")
            except ValueError:
                header_indices = {} 
                new_lines.append(line)
                i += 1
                continue
            
            new_lines.append(line) 
            new_lines.append(lines[i+1]) 
            i += 2 

            while i < len(lines) and lines[i].strip().startswith("|") and lines[i].strip().endswith("|"):
                row_line = lines[i]
                # Pad row_cells with empty strings if it's shorter than header_cells
                temp_row_cells = [cell.strip() for cell in row_line.strip("|").split("|")]
                while len(temp_row_cells) < len(header_cells):
                    temp_row_cells.append("") # Pad with empty string
                
                # Now, it's safe to access indices if they exist in header_indices
                if header_indices and header_indices.get('notes', -1) < len(temp_row_cells):
                    hbm_snippet = temp_row_cells[header_indices['hbm']] if header_indices.get('hbm', -1) < len(temp_row_cells) else ""
                    jpa_hib_annotations = temp_row_cells[header_indices['jpa_hib']] if header_indices.get('jpa_hib', -1) < len(temp_row_cells) else ""
                    notes_content = temp_row_cells[header_indices['notes']]
                    
                    simplified_note = simplify_notes(notes_content, hbm_snippet, jpa_hib_annotations)
                    temp_row_cells[header_indices['notes']] = simplified_note
                    
                    new_lines.append("| " + " | ".join(temp_row_cells) + " |")
                else:
                    new_lines.append(row_line)
                i += 1
            header_indices = {} 
            continue 
        
        new_lines.append(line)
        i += 1
        
    return "\n".join(new_lines)

if __name__ == "__main__":
    file_path = "HBM_to_JPA_Mapping_Guide.md"
    with open(file_path, "r", encoding="utf-8") as f:
        content = f.read()
    
    modified_content = modify_markdown_notes(content)
    
    with open(file_path, "w", encoding="utf-8") as f:
        f.write(modified_content)
    
    print(f"Successfully modified notes column in {file_path}")
