import os
import re
import sys
import xml.etree.ElementTree as ET

base = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'java', 'proxyapp')
forms = ['MainFrame', 'PublicListDialog', 'AccessRulesDialog', 'UsersDialog', 'HttpsDialog', 'WelcomeDialog', 'HelpDialog']
problems = 0


def report(form, msg):
    global problems
    problems += 1
    print('  LOI [%s] %s' % (form, msg))


def direct_children(el):
    sub = el.find('SubComponents')
    if sub is None:
        return []
    return [c for c in sub if c.tag in ('Component', 'Container') and c.get('name')]


def check_form(name):
    form_text = open(os.path.join(base, name + '.form'), encoding='utf-8').read()
    java = open(os.path.join(base, name + '.java'), encoding='utf-8').read()
    root = ET.fromstring(form_text)

    names = []
    custom = set()
    handlers = []
    for el in root.iter():
        if el.tag in ('Component', 'Container') and el.get('name'):
            names.append(el.get('name'))
            for ev in el.findall('Events/EventHandler'):
                handlers.append(ev.get('handler'))
            for av in el.findall('AuxValues/AuxValue'):
                if av.get('name') == 'JavaCodeGenerator_CreateCodeCustom':
                    custom.add(el.get('name'))
    for ev in root.findall('Events/EventHandler'):
        handlers.append(ev.get('handler'))
    if len(names) != len(set(names)):
        report(name, 'ten component bi trung')

    m = re.search(r'//GEN-BEGIN:initComponents(.*?)//GEN-END:initComponents', java, re.S)
    init = m.group(1)
    vm = re.search(r'//GEN-BEGIN:variables(.*?)//GEN-END:variables', java, re.S).group(1)

    for n in names:
        if not re.search(r'\b%s\s*=\s*new\b' % re.escape(n), init):
            report(name, '"%s" khong duoc tao trong initComponents' % n)
        if not re.search(r'private\s+[\w.<>]+\s+%s;' % re.escape(n), vm):
            report(name, '"%s" khong duoc khai bao trong vung variables' % n)
    for dname in re.findall(r'private\s+[\w.<>]+\s+(\w+);', vm):
        if dname not in names:
            report(name, 'bien "%s" co trong .java nhung khong co trong .form' % dname)

    containers = [el for el in root.iter() if el.tag == 'Container' and el.get('name')]
    group_containers = 0
    for c in [root] + containers:
        cname = c.get('name') or 'frame'
        layout = c.find('Layout')
        kids = [k.get('name') for k in direct_children(c)]
        if layout is not None and layout.findall('DimensionLayout'):
            group_containers += 1
            ids = {}
            for dim in layout.findall('DimensionLayout'):
                ids[int(dim.get('dim'))] = [x.get('id') for x in dim.iter('Component')]
            for d in (0, 1):
                for cid in ids.get(d, []):
                    if cid not in kids:
                        report(name, '%s: layout dim%d nhac toi "%s" khong phai con truc tiep' % (cname, d, cid))
                for k in kids:
                    if k not in ids.get(d, []):
                        report(name, '%s: con "%s" thieu trong layout dim%d cua .form' % (cname, k, d))
            lay = 'layout' if c is root else cname + 'Layout'
            if kids:
                if lay not in init:
                    report(name, '%s: .java khong co %s' % (cname, lay))
                    continue
                h = init.index(lay + '.setHorizontalGroup(')
                v = init.index(lay + '.setVerticalGroup(')
                hz = init[h:v]
                vt = init[v:v + 60000]
                end = vt.find('GroupLayout ')
                if end > 0:
                    vt = vt[:end]
                for k in kids:
                    if not re.search(r'addComponent\(%s[,)]' % re.escape(k), hz):
                        report(name, '%s: con "%s" thieu trong layout ngang cua .java' % (cname, k))
                    if not re.search(r'addComponent\(%s[,)]' % re.escape(k), vt):
                        report(name, '%s: con "%s" thieu trong layout doc cua .java' % (cname, k))
        elif layout is not None and layout.get('class', '').endswith('DesignBorderLayout'):
            for k in direct_children(c):
                if k.find('Constraints/Constraint/BorderConstraints') is None:
                    report(name, '%s: con "%s" thieu BorderConstraints' % (cname, k.get('name')))
        elif layout is not None and layout.get('class', '').endswith('JTabbedPaneSupportLayout'):
            for k in direct_children(c):
                tab = k.find('Constraints/Constraint/JTabbedPaneConstraints')
                if tab is None:
                    report(name, '%s: tab "%s" thieu JTabbedPaneConstraints' % (cname, k.get('name')))
                else:
                    title = tab.get('tabName')
                    if not re.search(r'%s\.addTab\("%s", %s\)' % (re.escape(cname), re.escape(title), re.escape(k.get('name'))), init):
                        report(name, '%s: .java khong them tab "%s" -> %s' % (cname, title, k.get('name')))
    top_layout = root.find('Layout')
    if top_layout is not None and top_layout.get('class', '').endswith('DesignBorderLayout'):
        for k in direct_children(root):
            direction = k.find('Constraints/Constraint/BorderConstraints')
            if direction is None:
                report(name, 'frame: con "%s" thieu BorderConstraints' % k.get('name'))
            else:
                const = direction.get('direction').upper()
                if not re.search(r'getContentPane\(\)\.add\(%s, java\.awt\.BorderLayout\.%s\)' % (re.escape(k.get('name')), const), init):
                    report(name, 'frame: .java khong add "%s" vao %s' % (k.get('name'), const))

    for h in handlers:
        if not re.search(r'private void %s\([^)]*\)\s*\{//GEN-FIRST:event_%s' % (re.escape(h), re.escape(h)), java):
            report(name, 'thieu ham xu ly %s (GEN-FIRST)' % h)
        if '//GEN-LAST:event_%s' % h not in java:
            report(name, 'thieu GEN-LAST cho %s' % h)
        if not re.search(r'\b%s\((evt|null)\);' % re.escape(h), init):
            report(name, 'initComponents khong goi %s' % h)
    for h in re.findall(r'GEN-FIRST:event_(\w+)', java):
        if h not in handlers:
            report(name, 'ham %s co trong .java nhung khong co EventHandler trong .form' % h)
    print('%-18s %2d component, %d container GroupLayout, %2d su kien' % (name, len(names), group_containers, len(handlers)))


for f in forms:
    check_form(f)
print('TONG SO LOI:', problems)
sys.exit(1 if problems else 0)
