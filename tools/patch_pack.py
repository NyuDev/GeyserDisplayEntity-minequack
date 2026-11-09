"""Ajoute au pack GeyserDisplayEntity le glissement du cube de bloc (geyser:i_dur), le texte « pour descendre »
des entites du pack, et change sa version.

Usage : python -I patch_pack.py <pack d'origine.mcpack> <pack sortie.mcpack> [version, ex. 1.944.0]
"""
import json
import sys
import zipfile

SRC, DST = sys.argv[1], sys.argv[2]
VERSION = [int(x) for x in sys.argv[3].split('.')] if len(sys.argv) > 3 else None

# Bedrock affiche action.hint.exit.<entite> quand on monte sur une entite ; sans texte, la cle brute
HINT = {
    'fr_FR': 'Accroupis-toi pour descendre',
    'fr_CA': 'Accroupis-toi pour descendre',
    'en_US': 'Sneak to get off',
    'en_GB': 'Sneak to get off',
}
LANG_FILES = {
    'texts/%s.lang' % lang: ''.join('action.hint.exit.%s=%s\n' % (e, text)
                                     for e in ('geyser:item_display', 'geyser:block_display')).encode()
    for lang, text in HINT.items()
}
LANG_FILES['texts/languages.json'] = json.dumps(sorted(HINT)).encode()

# (variable, propriete, angle ?)
FIELDS = [
    ('tx', 'geyser:t_x', False), ('ty', 'geyser:t_y', False), ('tz', 'geyser:t_z', False),
    ('rx', 'geyser:r_x', True), ('ry', 'geyser:r_y', True), ('rz', 'geyser:r_z', True),
    ('sx', 'geyser:s_x', False), ('sy', 'geyser:s_y', False), ('sz', 'geyser:s_z', False),
    ('qx', 'geyser:b_rrx', True), ('qy', 'geyser:b_rry', True), ('qz', 'geyser:b_rrz', True),
]


def prop(p):
    return "q.property('%s')" % p


INIT = [
    "v.gb_id = q.property('geyser:s_id'); v.gb_t = 0; v.gb_d = 0; v.gb_k = 1; v.gb_on = 0;",
    ' '.join('v.gb_%s = %s;' % (v, prop(p)) for v, p, _ in FIELDS),
    ' '.join('v.gb_0%s = v.gb_%s;' % (v, v) for v, _, _ in FIELDS),
]

PRE = [
    # Nouvelle valeur (s_id change a chaque envoi) : on part de ce qui est dessine
    "(q.property('geyser:s_id') != v.gb_id) ? {v.gb_id = q.property('geyser:s_id'); "
    + ' '.join('v.gb_0%s = v.gb_%s;' % (v, v) for v, _, _ in FIELDS)
    + " v.gb_t = 0; v.gb_d = q.property('geyser:i_dur'); v.gb_on = 1;};",
    # Le glissement : lineaire sur i_dur secondes, comme le client Java
    "(v.gb_on) ? {v.gb_t = v.gb_t + q.delta_time; v.gb_k = (v.gb_d > 0.001) ? math.clamp(v.gb_t / v.gb_d, 0, 1) : 1; "
    + ' '.join('v.gb_%s = %s(v.gb_0%s, %s, v.gb_k);' % (v, 'math.lerprotate' if ang else 'math.lerp', v, prop(p))
               for v, p, ang in FIELDS)
    + " v.gb_on = v.gb_k < 1;};",
]


def val(v, p):
    return '(v.gb_on ? v.gb_%s : %s)' % (v, prop(p))


def patch_animation(data):
    anim = json.loads(data)
    bones = anim['animations']['animation.geyser_block']['bones']
    f = {v: p for v, p, _ in FIELDS}
    bones['block_left']['position'] = [val('tx', f['tx']), val('ty', f['ty']), val('tz', f['tz'])]
    bones['block_left']['rotation'] = [val('rx', f['rx']), val('ry', f['ry']), val('rz', f['rz'])]
    bones['block_scale']['scale'] = [val('sx', f['sx']), val('sy', f['sy']), val('sz', f['sz'])]
    bones['block_right']['rotation'] = [val('qx', f['qx']), val('qy', f['qy']), val('qz', f['qz'])]
    return json.dumps(anim, indent='\t').encode()


def patch_entity(data):
    ent = json.loads(data)
    desc = ent['minecraft:client_entity']['description']
    scripts = desc.setdefault('scripts', {})
    scripts['initialize'] = list(scripts.get('initialize', [])) + INIT
    scripts['pre_animation'] = list(scripts.get('pre_animation', [])) + PRE
    return json.dumps(ent, indent='\t').encode()


def patch_manifest(data):
    man = json.loads(data)
    v = man['header']['version']
    nv = VERSION or [v[0], v[1] + 1, 0]
    man['header']['version'] = nv
    for m in man['modules']:
        m['version'] = nv
    print('version du pack :', v, '->', nv)
    return json.dumps(man, indent=4).encode()


done = set()
with zipfile.ZipFile(SRC) as zin, zipfile.ZipFile(DST, 'w', zipfile.ZIP_DEFLATED, compresslevel=9) as zout:
    for info in zin.infolist():
        data = zin.read(info.filename)
        name = info.filename
        if name == 'animations/geyser_block.animation.json':
            data = patch_animation(data); done.add(name)
        elif name in ('entity/block_display.entity.json', 'entity/item_display.entity.json'):
            data = patch_entity(data); done.add(name)
        elif name == 'manifest.json':
            data = patch_manifest(data); done.add(name)
        if name in LANG_FILES:
            continue
        out = zipfile.ZipInfo(name, date_time=info.date_time)
        out.compress_type = zipfile.ZIP_DEFLATED
        zout.writestr(out, data)
    for name, data in LANG_FILES.items():
        out = zipfile.ZipInfo(name, date_time=(2024, 1, 1, 0, 0, 0))
        out.compress_type = zipfile.ZIP_DEFLATED
        zout.writestr(out, data)
print('fichiers modifies :', sorted(done), '+ textes :', sorted(LANG_FILES))
assert len(done) == 4, done
