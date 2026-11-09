"""Pots de fleurs du block_render.tsv : la boite exterieure du pot au lieu du seul cube de terre.

Le cube du pack ne dessine qu'une boite par modele, la plus grosse : pour minecraft:flower_pot (et les potted_*),
c'est la terre (6,0,6)-(10,4,10), sans les parois. Un gobelet (pot a l'envers) n'etait qu'un carre de terre.
Ajoute une forme (5,0,5)-(11,6,11) : la terre du pot (meme texture) dessus, la terre cuite ailleurs.
Usage : python -I pot_shape.py <tsv entree> <tsv sortie>
"""
import sys
src, dst = sys.argv[1], sys.argv[2]
lines = open(src, encoding='utf-8').read().splitlines()
shapes = [l for l in lines if l.startswith('shape\t')]
old = None
for l in lines:
    c = l.split('\t')
    if c[0] == 'variant' and c[1] == 'minecraft:flower_pot':
        old = c[3]
assert old is not None
oc = shapes[int(old)].split('\t')
pot, dirt = oc[13], oc[12]          # faces de l'ancienne forme : dessous = terre cuite, dessus = terre
new = len(shapes)
shape = '\t'.join(['shape', str(new), '5', '0', '5', '11', '6', '11', pot, pot, pot, pot, dirt, pot, '0'])
out, n = [], 0
last_shape = max(i for i, l in enumerate(lines) if l.startswith('shape\t'))
for i, l in enumerate(lines):
    c = l.split('\t')
    if c[0] in ('variant', 'part') and c[3] == old and (c[1] == 'minecraft:flower_pot' or c[1].startswith('minecraft:potted_')):
        c[3] = str(new); l = '\t'.join(c); n += 1
    out.append(l)
    if i == last_shape:
        out.append(shape)
open(dst, 'w', encoding='utf-8', newline='\n').write('\n'.join(out) + '\n')
print('forme', new, ':', shape, '|', n, 'variantes')
