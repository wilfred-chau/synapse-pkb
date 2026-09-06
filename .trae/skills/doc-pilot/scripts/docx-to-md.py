#!/usr/bin/env python
"""docx-to-md.py - convert a DOCX file to Markdown with real tables.

Usage: python docx-to-md.py <input.docx> <output.md>

Requires: python-docx (pip install python-docx).

This is the PREFERRED converter (tables are rendered as proper Markdown
tables). docx-to-md.ps1 is the fallback for machines without Python; it
flattens tables to '| ' prefixed lines instead.

Limits (same as the .ps1 fallback): images, footnotes and headers/footers
are dropped; inline formatting (bold/italic) is dropped; nested tables are
inlined as plain text. Output is written as UTF-8 with BOM.

ASCII-only source by project convention (see doc-pilot SKILL.md).
"""

import sys

from docx import Document
from docx.oxml.ns import qn
from docx.table import Table
from docx.text.paragraph import Paragraph

W_P = qn('w:p')
W_TBL = qn('w:tbl')


def iter_blocks(doc):
    """Yield Paragraph and Table objects in document order (top level)."""
    for child in doc.element.body.iterchildren():
        if child.tag == W_P:
            yield Paragraph(child, doc)
        elif child.tag == W_TBL:
            yield Table(child, doc)


def para_to_md(p):
    text = p.text.strip()
    if not text:
        return None
    style = (p.style.name or '').lower() if p.style is not None else ''
    if 'heading' in style or 'toc' not in style and style.startswith('h'):
        digits = ''.join(ch for ch in style if ch.isdigit())
        lvl = int(digits) if digits else 1
        lvl = min(max(lvl, 1), 6)
        return '#' * lvl + ' ' + text
    if 'caption' in style:
        return '*' + text + '*'
    return text


def table_to_md(t):
    rows = []
    for row in t.rows:
        cells = []
        last_tc = None
        for c in row.cells:
            tc = c._tc
            if tc is last_tc:  # horizontally merged cell appears twice: keep once
                continue
            last_tc = tc
            cells.append(' '.join(c.text.split()).replace('|', '\\|'))
        if cells:
            rows.append(cells)
    if not rows:
        return None
    width = max(len(r) for r in rows)
    for r in rows:
        r.extend([''] * (width - len(r)))
    lines = ['| ' + ' | '.join(rows[0]) + ' |',
             '| ' + ' | '.join(['---'] * width) + ' |']
    lines.extend('| ' + ' | '.join(r) + ' |' for r in rows[1:])
    return '\n'.join(lines)


def main():
    if len(sys.argv) != 3:
        sys.exit('usage: python docx-to-md.py <input.docx> <output.md>')
    src, dst = sys.argv[1], sys.argv[2]
    doc = Document(src)
    out = [
        '# Converted from DOCX',
        '',
        '> Machine-converted by doc-pilot scripts/docx-to-md.py (python-docx).',
        '> Source of truth is the original DOCX. Images, footnotes, and',
        '> headers/footers are dropped; tables are rendered as Markdown tables.',
        '',
    ]
    n_para = n_tbl = 0
    for block in iter_blocks(doc):
        if isinstance(block, Paragraph):
            md = para_to_md(block)
            if md is None:
                continue
            out.append(md)
            out.append('')
            n_para += 1
        else:
            md = table_to_md(block)
            if md is None:
                continue
            out.append(md)
            out.append('')
            n_tbl += 1
    with open(dst, 'w', encoding='utf-8-sig', newline='\n') as f:
        f.write('\n'.join(out).rstrip('\n') + '\n')
    print('WROTE: %s (%d paragraphs, %d tables)' % (dst, n_para, n_tbl))


if __name__ == '__main__':
    main()