"""Ajoute les objets plats (item/generated et item/handheld) au pack et a block_render.tsv : une plaque de
16 x 16 pixels, epaisse d'un pixel, avec la texture layer0 de l'objet (premiere image si animee).

Usage : python -I flat_items_pack.py <client.jar> <pack entree.mcpack> <pack sortie.mcpack> <tsv entree> <tsv sortie>
"""
import io
import json
import sys
import zipfile

from PIL import Image

JAR, PACK_IN, PACK_OUT, TSV_IN, TSV_OUT = sys.argv[1:6]
jar = zipfile.ZipFile(JAR)
names = set(jar.namelist())


def jload(path):
    return json.loads(jar.read(path))


def loc(ref, kind, ext):
    ns, _, p = ref.partition(':') if ':' in ref else ('minecraft', '', ref)
    return 'assets/%s/%s/%s%s' % (ns, kind, p, ext)


def item_model(node):
    """Le premier modele simple d'une definition d'objet (26.x), None pour un rendu special."""
    if not isinstance(node, dict):
        return None
    t = node.get('type', '').replace('minecraft:', '')
    if t == 'model':
        return node.get('model')
    if t == 'special':
        return None
    for key in ('fallback', 'on_false', 'on_true'):
        if key in node:
            m = item_model(node[key])
            if m:
                return m
    for key in ('cases', 'entries'):
        for c in node.get(key, []):
            m = item_model(c.get('model'))
            if m:
                return m
    for m in node.get('models', []):
        r = item_model(m)
        if r:
            return r
    return None


def flat_texture(model_ref):
    """(layer0, handheld) si le modele finit sur item/generated, sinon None."""
    textures = {}
    handheld = False
    ref = model_ref
    for _ in range(12):
        path = loc(ref, 'models', '.json')
        if path not in names:
            return None
        m = jload(path)
        if m.get('elements'):
            return None
        for k, v in (m.get('textures') or {}).items():
            textures.setdefault(k, v)
        parent = (m.get('parent') or '').replace('minecraft:', '')
        if parent in ('item/handheld', 'item/handheld_rod', 'item/handheld_mace'):
            handheld = True
        if parent in ('builtin/generated', 'item/generated') or ref.replace('minecraft:', '') == 'item/generated':
            layer0 = textures.get('layer0')
            return (layer0, handheld) if layer0 else None
        if not parent or parent.startswith('builtin/') or parent.startswith('block/'):
            return None
        ref = parent
    return None


# Les objets deja dessines (blocs) et le prochain numero de forme
tsv = open(TSV_IN, encoding='utf-8').read().splitlines()
known = {l.split('\t')[1] for l in tsv if l.startswith('item\t')}
shape_count = sum(1 for l in tsv if l.startswith('shape\t'))

pin = zipfile.ZipFile(PACK_IN)
ent_b = json.loads(pin.read('entity/block_display.entity.json'))
ent_i = json.loads(pin.read('entity/item_display.entity.json'))
rc = json.loads(pin.read('render_controllers/geyser_block.render_controllers.json'))
tex_b = ent_b['minecraft:client_entity']['description']['textures']
tex_i = ent_i['minecraft:client_entity']['description']['textures']
arrays = [c['arrays']['textures']['Array.textures'] for c in rc['render_controllers'].values()]
next_tex = len(arrays[0])
assert all(len(a) == next_tex for a in arrays)

new_files = {}
new_lines = []
by_texture = {}
added = 0
for path in sorted(n for n in names if n.startswith('assets/minecraft/items/') and n.endswith('.json')):
    item_id = 'minecraft:' + path.rsplit('/', 1)[1][:-5]
    if item_id in known:
        continue
    model = item_model(jload(path).get('model'))
    if not model:
        continue
    flat = flat_texture(model)
    if not flat:
        continue
    layer0, handheld = flat
    png = loc(layer0, 'textures', '.png')
    if png not in names:
        continue
    if layer0 not in by_texture:
        img = Image.open(io.BytesIO(jar.read(png))).convert('RGBA')
        w, h = img.size
        if h > w:
            img = img.crop((0, 0, w, w))
        buf = io.BytesIO()
        img.save(buf, 'PNG')
        idx = next_tex
        next_tex += 1
        new_files['textures/geyser_display/blocks/%d.png' % idx] = buf.getvalue()
        key = 't%d' % idx
        tex_b[key] = 'textures/geyser_display/blocks/%d' % idx
        tex_i[key] = 'textures/geyser_display/blocks/%d' % idx
        for a in arrays:
            a.append('Texture.' + key)
        by_texture[layer0] = idx
    idx = by_texture[layer0]
    # drapeaux : 4 = objet plat, 8 = tenu a la main (epee, hache...)
    flags = 4 | (8 if handheld else 0)
    # une seule face, au sud (l'avant d'un objet Java) : le pack la dessine des deux cotes, une face nord se
    # verrait a travers, en miroir (objets doubles en X)
    new_lines.append('shape\t%d\t0\t0\t7.5\t16\t16\t8.5\t-1\t%d\t-1\t-1\t-1\t-1\t%d' % (shape_count, idx, flags))
    new_lines.append('item\t%s\t%d' % (item_id, shape_count))
    shape_count += 1
    added += 1

# Le tsv : les formes avant les objets (le chargeur lit les numeros de forme dans l'ordre)
shapes = [l for l in tsv if l.startswith('shape\t')]
rest = [l for l in tsv if not l.startswith('shape\t')]
out = [l for l in rest if l.startswith('#')] + shapes + [l for l in new_lines if l.startswith('shape\t')] \
      + [l for l in rest if not l.startswith('#')] + [l for l in new_lines if l.startswith('item\t')]
open(TSV_OUT, 'w', encoding='utf-8', newline='\n').write('\n'.join(out) + '\n')

with zipfile.ZipFile(PACK_OUT, 'w', zipfile.ZIP_DEFLATED, compresslevel=9) as zout:
    for info in pin.infolist():
        data = pin.read(info.filename)
        if info.filename == 'entity/block_display.entity.json':
            data = json.dumps(ent_b, indent='\t').encode()
        elif info.filename == 'entity/item_display.entity.json':
            data = json.dumps(ent_i, indent='\t').encode()
        elif info.filename == 'render_controllers/geyser_block.render_controllers.json':
            data = json.dumps(rc, indent='\t').encode()
        zi = zipfile.ZipInfo(info.filename, date_time=info.date_time)
        zi.compress_type = zipfile.ZIP_DEFLATED
        zout.writestr(zi, data)
    for name, data in new_files.items():
        zi = zipfile.ZipInfo(name, date_time=(2024, 1, 1, 0, 0, 0))
        zi.compress_type = zipfile.ZIP_DEFLATED
        zout.writestr(zi, data)

print('objets plats ajoutes :', added, '| textures nouvelles :', len(new_files), '| textures au total :', next_tex)
for probe in ('minecraft:tropical_fish', 'minecraft:iron_sword', 'minecraft:arrow', 'minecraft:echo_shard', 'minecraft:cod',
              'minecraft:amethyst_cluster', 'minecraft:sculk_vein', 'minecraft:feather', 'minecraft:fishing_rod'):
    print(probe, 'oui' if any(l == 'item\t%s\t%s' % (probe, l.split('\t')[-1]) for l in new_lines if l.startswith('item\t' + probe + '\t')) else 'non')
