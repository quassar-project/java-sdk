# Book model test fixtures

All fixtures use the library domain and literal tabs for nesting.

- `library.tara`: books, parts, chapters, a subject index and a reading plan; constructor, facet and inline properties.
- `library.zip`: the same model as a ZIP, with an ignored non-Tara entry.
- `folder/` and `folder.zip`: a library spread across nested files, with references between books.
- `model/critical-edition.tara`: a critical edition with 17 mograms, five nesting levels, two books, contributors, a publisher and a reading collection.
- `critical-library/` and `critical-library.zip`: that critical edition split into books, people, publishers and reading lists. Tests verify equivalence with the single-file model.
- `model/scopes.tara`: editors in nested book and volume scopes, unnamed parts and circular edition references.
- `model/anonymous-mograms.tara`: identical notes belonging to different books.
- `model/sequences.tara`: boolean, numeric, text, reference and mixed sequences, including escaped text and forward references.
- `facet-properties.tara` and `inline-properties.tara`: book properties declared in different positions.
- `cross-file/`: related editions declared in separate files and subfolders.
- `normalization/`: BOM and DSL headers, preserving quoted text.
- `tab-indentation.tara`: sibling chapters indented with tabs.

The critical edition exercises translated and limited-edition facets, decimal prices, anonymous notes, inline citations, forward references to contributors and publishers, circular related-book references, chapter links across parts, and references to nested editions from a reading list.
