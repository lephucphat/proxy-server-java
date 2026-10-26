import os
import re
import sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'java', 'proxyapp')

PREF = -2
DEF = -1
MAXV = 32767
ALIGN = {'LEADING': '0', 'TRAILING': '1', 'CENTER': '2', 'BASELINE': '3'}


class Gap:
    def __init__(self, n):
        self.n = n


class Flex:
    pass


class Comp:
    def __init__(self, name, w=None, h=None, gh=False, gv=False):
        self.name = name
        self.w = w
        self.h = h
        self.gh = gh
        self.gv = gv


class Seq:
    def __init__(self, *items):
        self.items = list(items)


class Par:
    def __init__(self, align, *items):
        self.align = align
        self.items = list(items)


def size_of(c, dim):
    size = c.w if dim == 0 else c.h
    grow = c.gh if dim == 0 else c.gv
    if grow:
        return (DEF, size if size else DEF, MAXV)
    if size:
        return (PREF, size, PREF)
    return (PREF, DEF, PREF)


def jnum(v, kind):
    if v == PREF:
        return 'javax.swing.GroupLayout.PREFERRED_SIZE'
    if v == DEF:
        return 'javax.swing.GroupLayout.DEFAULT_SIZE'
    if v == MAXV:
        return 'Short.MAX_VALUE'
    return str(v)


def java_node(node, dim, lay, pad):
    ind = ' ' * pad
    out = []
    if isinstance(node, Gap):
        out.append('%s.addGap(%d, %d, %d)' % (ind, node.n, node.n, node.n))
    elif isinstance(node, Flex):
        out.append('%s.addGap(0, 0, Short.MAX_VALUE)' % ind)
    elif isinstance(node, Comp):
        mn, pf, mx = size_of(node, dim)
        if (mn, pf, mx) == (PREF, DEF, PREF):
            out.append('%s.addComponent(%s)' % (ind, node.name))
        else:
            out.append('%s.addComponent(%s, %s, %s, %s)' % (ind, node.name, jnum(mn, 0), jnum(pf, 1), jnum(mx, 2)))
    elif isinstance(node, Seq):
        out.append('%s.addGroup(%s.createSequentialGroup()' % (ind, lay))
        for it in node.items:
            out.append(java_node(it, dim, lay, pad + 4))
        out[-1] += ')'
    elif isinstance(node, Par):
        out.append('%s.addGroup(%s.createParallelGroup(javax.swing.GroupLayout.Alignment.%s)' % (ind, lay, node.align))
        for it in node.items:
            out.append(java_node(it, dim, lay, pad + 4))
        out[-1] += ')'
    return '\n'.join(out)


def java_layout(lay, root_h, root_v):
    s = '        %s.setHorizontalGroup(\n            %s.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)\n' % (lay, lay)
    s += java_node(root_h, 0, lay, 12) + '\n        );\n'
    s += '        %s.setVerticalGroup(\n            %s.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)\n' % (lay, lay)
    s += java_node(root_v, 1, lay, 12) + '\n        );\n'
    return s


def xml_attr(mn, pf, mx):
    a = ''
    if mn != DEF:
        a += ' min="%d"' % mn
    if pf != DEF:
        a += ' pref="%d"' % pf
    if mx != DEF:
        a += ' max="%d"' % mx
    return a


def xml_node(node, dim, parent_align, pad):
    ind = ' ' * pad
    alignattr = ' alignment="%s"' % parent_align if parent_align is not None else ''
    if isinstance(node, Gap):
        return '%s<EmptySpace min="-2" pref="%d" max="-2" attributes="0"/>\n' % (ind, node.n)
    if isinstance(node, Flex):
        return '%s<EmptySpace min="0" pref="0" max="32767" attributes="0"/>\n' % ind
    if isinstance(node, Comp):
        mn, pf, mx = size_of(node, dim)
        return '%s<Component id="%s"%s%s attributes="0"/>\n' % (ind, node.name, alignattr, xml_attr(mn, pf, mx))
    if isinstance(node, Seq):
        s = '%s<Group type="102"%s attributes="0">\n' % (ind, alignattr)
        for it in node.items:
            s += xml_node(it, dim, None, pad + 4)
        return s + '%s</Group>\n' % ind
    if isinstance(node, Par):
        code = ALIGN[node.align]
        s = '%s<Group type="103" groupAlignment="%s"%s attributes="0">\n' % (ind, code, alignattr)
        for it in node.items:
            s += xml_node(it, dim, code, pad + 4)
        return s + '%s</Group>\n' % ind


def xml_layout(root_h, root_v, pad):
    ind = ' ' * pad
    s = ''
    for dim, root in ((0, root_h), (1, root_v)):
        s += '%s<DimensionLayout dim="%d">\n' % (ind + '  ', dim)
        s += '%s    <Group type="103" groupAlignment="0" attributes="0">\n' % ind
        s += xml_node(root, dim, '0', pad + 8)
        s += '%s    </Group>\n' % ind
        s += '%s</DimensionLayout>\n' % (ind + '  ')
    return s


def esc(t):
    return t.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;').replace('"', '&quot;')


def jstr(t):
    return t.replace('\\', '\\\\').replace('"', '\\"')


class Widget:
    def __init__(self, name, kind, cls, props=None, events=None, title=None, inner=None):
        self.name = name
        self.kind = kind
        self.cls = cls
        self.props = props or []
        self.events = events or []
        self.title = title
        self.inner = inner


def T(text):
    return ('text', 'java.lang.String', text)


def label(name, text, **kw):
    return Widget(name, 'label', 'javax.swing.JLabel', [T(text)] + kw.get('extra', []))


def button(name, text, event=True, extra=None):
    return Widget(name, 'button', 'javax.swing.JButton', [T(text)] + (extra or []),
                  [('actionPerformed', name + 'ActionPerformed')] if event else [])


def check(name, text, event=False, extra=None):
    return Widget(name, 'check', 'javax.swing.JCheckBox', [T(text)] + (extra or []),
                  [('actionPerformed', name + 'ActionPerformed')] if event else [])


def field(name, text=None, extra=None):
    props = ([T(text)] if text is not None else []) + (extra or [])
    return Widget(name, 'field', 'javax.swing.JTextField', props)


def password(name):
    return Widget(name, 'password', 'javax.swing.JPasswordField')


def scroll_area(scroll_name, area_name, readonly=False):
    props = [('columns', 'int', '20'), ('rows', 'int', '5')]
    if readonly:
        props = [('editable', 'boolean', 'false')] + props
    return Widget(scroll_name, 'scroll', 'javax.swing.JScrollPane',
                  inner=Widget(area_name, 'area', 'javax.swing.JTextArea', props))


def scroll_table(scroll_name, table_name):
    return Widget(scroll_name, 'scroll', 'javax.swing.JScrollPane',
                  inner=Widget(table_name, 'table', 'javax.swing.JTable'))


def panel(name, h, v, children, title=None, tab=None, border=None):
    w = Widget(name, 'panel', 'javax.swing.JPanel', title=title)
    w.h = h
    w.v = v
    w.children = children
    w.tab = tab
    w.border = border
    return w


def chart(name):
    return Widget(name, 'chart', 'javax.swing.JPanel')


def build():
    G = 8

    def field_row(lbl, lw, fld, fw, *more):
        return lbl, lw, fld, fw

    top = panel('pnlTop',
                Seq(Gap(12), Comp('btnStart', w=130), Gap(8), Comp('btnStop', w=130), Gap(20),
                    Comp('lblStatus', w=220, gh=True), Gap(12), Comp('btnSystemProxy', w=240), Gap(12)),
                Seq(Gap(10), Par('CENTER', Comp('btnStart', h=36), Comp('btnStop', h=36), Comp('lblStatus'),
                                 Comp('btnSystemProxy', h=36)), Gap(10)),
                [button('btnStart', 'Bật proxy'),
                 button('btnStop', 'Tắt proxy', extra=[('enabled', 'boolean', 'false')]),
                 label('lblStatus', '● Đã dừng'),
                 button('btnSystemProxy', 'Bật proxy hệ thống Windows', extra=[('enabled', 'boolean', 'false')])])

    conn = panel('pnlConn',
                 Seq(Gap(12), Par('LEADING',
                                  Seq(Comp('lblPort', w=170), Gap(8), Comp('txtPort', w=90), Gap(32), Comp('chkLan')),
                                  Seq(Comp('lblLimit', w=170), Gap(8), Comp('txtLimit', w=90), Gap(32), Comp('chkLogFile'))),
                     Gap(12)),
                 Seq(Gap(6), Par('BASELINE', Comp('lblPort'), Comp('txtPort'), Comp('chkLan')), Gap(G),
                     Par('BASELINE', Comp('lblLimit'), Comp('txtLimit'), Comp('chkLogFile')), Gap(10)),
                 [label('lblPort', 'Cổng:'), field('txtPort', '8080'),
                  check('chkLan', 'Cho phép máy khác trong mạng LAN dùng'),
                  label('lblLimit', 'Giới hạn tốc độ (KB/s):', extra=[('toolTipText', 'java.lang.String', '0 = không giới hạn')]),
                  field('txtLimit', '0', extra=[('toolTipText', 'java.lang.String', '0 = không giới hạn')]),
                  check('chkLogFile', 'Ghi nhật ký ra file proxy-log.csv')],
                 title='Kết nối')
    sec = panel('pnlSecurity',
                Seq(Gap(12), Par('LEADING',
                                 Comp('chkAuth'),
                                 Seq(Comp('lblUser', w=170), Gap(8), Comp('txtUser', w=180), Gap(32), Comp('lblPass', w=80), Gap(8),
                                     Comp('txtPass', w=180)),
                                 Seq(Comp('lblAllow', w=170), Gap(8), Comp('txtAllow', w=200, gh=True), Gap(8), Comp('btnUsers', w=170))),
                    Gap(12)),
                Seq(Gap(6), Comp('chkAuth'), Gap(G),
                    Par('BASELINE', Comp('lblUser'), Comp('txtUser'), Comp('lblPass'), Comp('txtPass')), Gap(G),
                    Par('BASELINE', Comp('lblAllow'), Comp('txtAllow'), Comp('btnUsers')), Gap(10)),
                [check('chkAuth', 'Yêu cầu đăng nhập'), label('lblUser', 'Tài khoản:'), field('txtUser', 'admin'),
                 label('lblPass', 'Mật khẩu:'), password('txtPass'),
                 label('lblAllow', 'IP máy được phép:',
                       extra=[('toolTipText', 'java.lang.String', 'Để trống = tất cả. Ví dụ: 192.168.1.0/24, 10.0.0.5')]),
                 field('txtAllow', extra=[('toolTipText', 'java.lang.String', 'Để trống = tất cả. Ví dụ: 192.168.1.0/24, 10.0.0.5')]),
                 button('btnUsers', 'Tài khoản khác...')],
                title='Bảo mật')
    system = panel('pnlSystem',
                   Seq(Gap(12), Par('LEADING', Comp('chkTray'), Comp('chkAutoProxy'), Comp('chkWinStart')), Gap(12)),
                   Seq(Gap(6), Comp('chkTray'), Gap(4), Comp('chkAutoProxy'), Gap(4), Comp('chkWinStart'), Gap(10)),
                   [check('chkTray', 'Đóng cửa sổ thì chạy nền ở khay hệ thống',
                          extra=[('toolTipText', 'java.lang.String', 'Khi proxy đang chạy, bấm X chỉ ẩn cửa sổ xuống khay hệ thống')]),
                    check('chkAutoProxy', 'Tự bật proxy khi mở app'),
                    check('chkWinStart', 'Tự mở cùng Windows', event=True,
                          extra=[('toolTipText', 'java.lang.String', 'Chỉ dùng được khi chạy từ file .exe hoặc .jar')])],
                   title='Hệ thống')
    general = panel('pnlGeneral',
                    Seq(Gap(12), Par('LEADING', Comp('pnlConn', gh=True), Comp('pnlSecurity', gh=True),
                                     Comp('pnlSystem', gh=True), Comp('lblHint', gh=True)), Gap(12)),
                    Seq(Gap(12), Comp('pnlConn'), Gap(10), Comp('pnlSecurity'), Gap(10), Comp('pnlSystem'), Gap(10),
                        Comp('lblHint'), Flex()),
                    [conn, sec, system, label('lblHint', 'Đặt proxy trình duyệt là 127.0.0.1 và cổng đã chọn')],
                    tab='Tổng quan')

    rules = panel('pnlRules',
                  Seq(Gap(12), Par('LEADING', Comp('lblBlock'), Comp('scrollBlock', w=320, gh=True), Comp('btnSaveBlock', w=200)),
                      Gap(16),
                      Par('LEADING', Comp('btnPublic', w=340), Comp('lblPublicHint', w=340), Comp('btnRules', w=340),
                          Comp('lblRulesHint', w=340), Comp('btnHttps', w=340), Comp('lblHttpsHint', w=340)),
                      Gap(12)),
                  Seq(Gap(12), Par('LEADING',
                                   Seq(Comp('lblBlock'), Gap(6), Comp('scrollBlock', h=300, gv=True), Gap(G), Comp('btnSaveBlock', h=32)),
                                   Seq(Comp('btnPublic', h=40), Gap(4), Comp('lblPublicHint'), Gap(20),
                                       Comp('btnRules', h=40), Gap(4), Comp('lblRulesHint'), Gap(20),
                                       Comp('btnHttps', h=40), Gap(4), Comp('lblHttpsHint'), Flex())),
                      Gap(12)),
                  [label('lblBlock', 'Danh sách chặn (mỗi dòng một tên miền):'),
                   scroll_area('scrollBlock', 'txtBlock'),
                   button('btnSaveBlock', 'Lưu danh sách chặn'),
                   button('btnPublic', 'Danh sách chặn công khai...'),
                   label('lblPublicHint', 'Nạp danh sách quảng cáo, theo dõi, mã độc từ file hoặc URL'),
                   button('btnRules', 'Chỉ cho phép & lịch chặn theo giờ...'),
                   label('lblRulesHint', 'Chỉ cho vào một số trang, hoặc chặn theo giờ trong ngày'),
                   button('btnHttps', 'Giải mã HTTPS...'),
                   label('lblHttpsHint', 'Thấy đường dẫn HTTPS để chặn theo đường dẫn, từ khóa')],
                  tab='Quy tắc')

    log = panel('pnlLog',
                Seq(Gap(12), Par('LEADING',
                                 Seq(Comp('lblFilter', w=40), Gap(6), Comp('txtFilter', w=240, gh=True), Gap(12), Comp('chkBlockedOnly'),
                                     Gap(12), Comp('btnClearLog', w=120), Gap(6), Comp('btnExport', w=120)),
                                 Comp('scrollLog', w=700, gh=True)),
                    Gap(12)),
                Seq(Gap(12), Par('BASELINE', Comp('lblFilter'), Comp('txtFilter'), Comp('chkBlockedOnly'),
                                 Comp('btnClearLog'), Comp('btnExport')), Gap(G), Comp('scrollLog', h=360, gv=True), Gap(12)),
                [label('lblFilter', 'Lọc:'),
                 field('txtFilter', extra=[('toolTipText', 'java.lang.String', 'Gõ tên miền, địa chỉ hoặc tên người dùng')]),
                 check('chkBlockedOnly', 'Chỉ hiện dòng bị chặn', event=True),
                 button('btnClearLog', 'Xóa nhật ký'), button('btnExport', 'Xuất CSV'),
                 scroll_table('scrollLog', 'tblLog')],
                tab='Nhật ký')

    stats = panel('pnlStats',
                  Seq(Gap(12), Par('LEADING',
                                   Comp('lblChart'),
                                   Comp('pnlChart', w=700, gh=True),
                                   Seq(Par('LEADING', Comp('lblClients'), Comp('scrollClients', w=300, gh=True)), Gap(12),
                                       Par('LEADING', Comp('lblUsers'), Comp('scrollUsers', w=300, gh=True))),
                                   Seq(Par('LEADING', Comp('lblAlerts'), Comp('scrollAlerts', w=380, gh=True)), Gap(12),
                                       Par('LEADING', Comp('lblTop'), Comp('scrollTop', w=260, gh=True))),
                                   Seq(Comp('btnClearAlerts', w=150), Gap(8), Comp('btnReset', w=190))),
                      Gap(12)),
                  Seq(Gap(10), Comp('lblChart'), Gap(4), Comp('pnlChart', h=170, gv=True), Gap(G),
                      Par('LEADING', Comp('lblClients'), Comp('lblUsers')), Gap(4),
                      Par('LEADING', Comp('scrollClients', h=120, gv=True), Comp('scrollUsers', h=120, gv=True)), Gap(G),
                      Par('LEADING', Comp('lblAlerts'), Comp('lblTop')), Gap(4),
                      Par('LEADING', Comp('scrollAlerts', h=90, gv=True), Comp('scrollTop', h=90, gv=True)), Gap(G),
                      Par('BASELINE', Comp('btnClearAlerts'), Comp('btnReset')), Gap(10)),
                  [label('lblChart', 'Lưu lượng 5 phút gần nhất (tải xuống / tải lên):'),
                   chart('pnlChart'),
                   label('lblClients', 'Theo máy khách:'), label('lblUsers', 'Theo người dùng:'),
                   scroll_table('scrollClients', 'tblClients'), scroll_table('scrollUsers', 'tblUsers'),
                   label('lblAlerts', 'Cảnh báo (mới nhất ở dưới):'), label('lblTop', 'Tên miền truy cập nhiều nhất:'),
                   scroll_area('scrollAlerts', 'txtAlerts', readonly=True), scroll_area('scrollTop', 'txtTop', readonly=True),
                   button('btnClearAlerts', 'Xóa cảnh báo'), button('btnReset', 'Đặt lại thống kê')],
                  tab='Thống kê')

    tabs = Widget('tabs', 'tabs', 'javax.swing.JTabbedPane')
    tabs.children = [general, rules, log, stats]

    labels = ['lblReq', 'lblBlocked', 'lblConn', 'lblDown', 'lblUp', 'lblFail']
    hitems = [Gap(12)]
    for i, n in enumerate(labels):
        if i:
            hitems.append(Gap(28))
        hitems.append(Comp(n))
    hitems += [Flex(), Gap(12)]
    status = panel('pnlStatus', Seq(*hitems),
                   Seq(Gap(6), Par('BASELINE', *[Comp(n) for n in labels]), Gap(6)),
                   [label('lblReq', 'Yêu cầu: 0'), label('lblBlocked', 'Bị chặn: 0'), label('lblConn', 'Kết nối: 0'),
                    label('lblDown', 'Tải xuống: 0 B'), label('lblUp', 'Tải lên: 0 B'), label('lblFail', 'Sai mật khẩu: 0')])
    return top, tabs, status


def props_xml(widget, pad):
    ind = ' ' * pad
    s = ''
    for pn, pt, pv in widget.props:
        s += '%s<Property name="%s" type="%s" value="%s"/>\n' % (ind, pn, pt, esc(pv))
    return s


def events_xml(widget, pad):
    if not widget.events:
        return ''
    ind = ' ' * pad
    s = '%s<Events>\n' % ind
    for ev, handler in widget.events:
        listener = 'java.awt.event.ActionListener'
        params = 'java.awt.event.ActionEvent'
        s += '%s  <EventHandler event="%s" listener="%s" parameters="%s" handler="%s"/>\n' % (ind, ev, listener, params, handler)
    return s + '%s</Events>\n' % ind


BORDER_LAYOUT = 'org.netbeans.modules.form.compat2.layouts.DesignBorderLayout'
TAB_LAYOUT = 'org.netbeans.modules.form.compat2.layouts.support.JTabbedPaneSupportLayout'


def widget_xml(w, pad, constraint=None):
    ind = ' ' * pad
    s = ''
    if w.kind == 'scroll':
        s += '%s<Container class="javax.swing.JScrollPane" name="%s">\n' % (ind, w.name)
        s += '%s  <AuxValues>\n%s    <AuxValue name="autoScrollPane" type="java.lang.Boolean" value="true"/>\n%s  </AuxValues>\n' % (ind, ind, ind)
        s += '\n%s  <Layout class="org.netbeans.modules.form.compat2.layouts.support.JScrollPaneSupportLayout"/>\n' % ind
        s += '%s  <SubComponents>\n' % ind
        inner = w.inner
        if inner.props:
            s += '%s    <Component class="%s" name="%s">\n%s      <Properties>\n%s%s      </Properties>\n%s    </Component>\n' % (
                ind, inner.cls, inner.name, ind, props_xml(inner, pad + 8), ind, ind)
        else:
            s += '%s    <Component class="%s" name="%s">\n%s    </Component>\n' % (ind, inner.cls, inner.name, ind)
        s += '%s  </SubComponents>\n%s</Container>\n' % (ind, ind)
        return s
    if w.kind == 'chart':
        s += '%s<Container class="javax.swing.JPanel" name="%s">\n' % (ind, w.name)
        s += '%s  <AuxValues>\n%s    <AuxValue name="JavaCodeGenerator_CreateCodeCustom" type="java.lang.String" value="new TrafficChart(history)"/>\n%s  </AuxValues>\n' % (ind, ind, ind)
        s += '\n%s  <Layout>\n' % ind
        for dim in (0, 1):
            s += '%s    <DimensionLayout dim="%d">\n%s      <Group type="103" groupAlignment="0" attributes="0">\n' % (ind, dim, ind)
            s += '%s          <EmptySpace min="0" pref="%d" max="32767" attributes="0"/>\n' % (ind, 700 if dim == 0 else 170)
            s += '%s      </Group>\n%s    </DimensionLayout>\n' % (ind, ind)
        s += '%s  </Layout>\n%s</Container>\n' % (ind, ind)
        return s
    if w.kind == 'panel':
        s += '%s<Container class="javax.swing.JPanel" name="%s">\n' % (ind, w.name)
        if w.title:
            s += '%s  <Properties>\n%s    <Property name="border" type="javax.swing.border.Border" editor="org.netbeans.modules.form.editors2.BorderEditor">\n' % (ind, ind)
            s += '%s      <Border info="org.netbeans.modules.form.compat2.border.TitledBorderInfo">\n%s        <TitledBorder title="%s"/>\n%s      </Border>\n%s    </Property>\n%s  </Properties>\n' % (
                ind, ind, esc(w.title), ind, ind, ind)
        if constraint:
            s += constraint_xml(constraint, pad + 2)
        s += '%s  <Layout>\n' % ind
        s += xml_layout(w.h, w.v, pad + 2)
        s += '%s  </Layout>\n' % ind
        s += '%s  <SubComponents>\n' % ind
        for ch in w.children:
            s += widget_xml(ch, pad + 4)
        s += '%s  </SubComponents>\n%s</Container>\n' % (ind, ind)
        return s
    s += '%s<Component class="%s" name="%s">\n' % (ind, w.cls, w.name)
    if w.props:
        s += '%s  <Properties>\n%s%s  </Properties>\n' % (ind, props_xml(w, pad + 4), ind)
    s += events_xml(w, pad + 2)
    s += '%s</Component>\n' % ind
    return s


def constraint_xml(c, pad):
    ind = ' ' * pad
    kind, value = c
    if kind == 'border':
        return ('%s<Constraints>\n%s  <Constraint layoutClass="%s" value="%s$BorderConstraintsDescription">\n'
                '%s    <BorderConstraints direction="%s"/>\n%s  </Constraint>\n%s</Constraints>\n') % (
            ind, ind, BORDER_LAYOUT, BORDER_LAYOUT, ind, value, ind, ind)
    return ('%s<Constraints>\n%s  <Constraint layoutClass="%s" value="%s$JTabbedPaneConstraintsDescription">\n'
            '%s    <JTabbedPaneConstraints tabName="%s">\n%s      <Property name="tabTitle" type="java.lang.String" value="%s"/>\n'
            '%s    </JTabbedPaneConstraints>\n%s  </Constraint>\n%s</Constraints>\n') % (
        ind, ind, TAB_LAYOUT, TAB_LAYOUT, ind, esc(value), ind, esc(value), ind, ind, ind)


def tabs_xml(tabs, pad):
    ind = ' ' * pad
    s = '%s<Container class="javax.swing.JTabbedPane" name="tabs">\n' % ind
    s += constraint_xml(('border', 'Center'), pad + 2)
    s += '\n%s  <Layout class="%s"/>\n%s  <SubComponents>\n' % (ind, TAB_LAYOUT, ind)
    for ch in tabs.children:
        s += widget_xml(ch, pad + 4, ('tab', ch.tab))
    s += '%s  </SubComponents>\n%s</Container>\n' % (ind, ind)
    return s


FORM_HEAD = '''<?xml version="1.0" encoding="UTF-8" ?>

<Form version="1.3" maxVersion="1.9" type="org.netbeans.modules.form.forminfo.JFrameFormInfo">
  <Properties>
    <Property name="defaultCloseOperation" type="int" value="0"/>
    <Property name="title" type="java.lang.String" value="Proxy Server"/>
    <Property name="minimumSize" type="java.awt.Dimension" editor="org.netbeans.beaninfo.editors.DimensionEditor">
      <Dimension value="[900, 600]"/>
    </Property>
    <Property name="preferredSize" type="java.awt.Dimension" editor="org.netbeans.beaninfo.editors.DimensionEditor">
      <Dimension value="[1000, 680]"/>
    </Property>
  </Properties>
  <SyntheticProperties>
    <SyntheticProperty name="formSizePolicy" type="int" value="1"/>
    <SyntheticProperty name="generateCenter" type="boolean" value="false"/>
  </SyntheticProperties>
  <Events>
    <EventHandler event="windowClosing" listener="java.awt.event.WindowListener" parameters="java.awt.event.WindowEvent" handler="formWindowClosing"/>
  </Events>
  <AuxValues>
    <AuxValue name="FormSettings_autoResourcing" type="java.lang.Integer" value="0"/>
    <AuxValue name="FormSettings_autoSetComponentName" type="java.lang.Boolean" value="false"/>
    <AuxValue name="FormSettings_generateFQN" type="java.lang.Boolean" value="true"/>
    <AuxValue name="FormSettings_generateMnemonicsCode" type="java.lang.Boolean" value="false"/>
    <AuxValue name="FormSettings_i18nAutoMode" type="java.lang.Boolean" value="false"/>
    <AuxValue name="FormSettings_layoutCodeTarget" type="java.lang.Integer" value="1"/>
    <AuxValue name="FormSettings_listenerGenerationStyle" type="java.lang.Integer" value="0"/>
    <AuxValue name="FormSettings_variablesLocal" type="java.lang.Boolean" value="false"/>
    <AuxValue name="FormSettings_variablesModifier" type="java.lang.Integer" value="2"/>
  </AuxValues>
  <Layout class="%s"/>
  <SubComponents>
''' % BORDER_LAYOUT


def make_form(top, tabs, status):
    s = FORM_HEAD
    s += widget_xml(top, 4, ('border', 'North'))
    s += tabs_xml(tabs, 4)
    s += widget_xml(status, 4, ('border', 'South'))
    s += '  </SubComponents>\n</Form>\n'
    return s


def all_widgets(top, tabs, status):
    order = []

    def walk(w):
        order.append(w)
        if w.kind == 'scroll':
            order.append(w.inner)
        for ch in getattr(w, 'children', []):
            walk(ch)

    walk(top)
    walk(tabs)
    walk(status)
    return order


def creation(w):
    if w.kind == 'chart':
        return '        %s = new TrafficChart(history);\n' % w.name
    return '        %s = new %s();\n' % (w.name, w.cls.replace('javax.swing.', 'javax.swing.'))


def setter(name, pn, pt, pv):
    sn = 'set' + pn[0].upper() + pn[1:]
    if pt == 'boolean':
        return '        %s.%s(%s);\n' % (name, sn, pv)
    if pt == 'int':
        return '        %s.%s(%s);\n' % (name, sn, pv)
    return '        %s.%s("%s");\n' % (name, sn, jstr(pv))


def make_init(top, tabs, status):
    widgets = all_widgets(top, tabs, status)
    s = '    @SuppressWarnings("unchecked")\n'
    s += '    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents\n'
    s += '    private void initComponents() {\n\n'
    for w in widgets:
        s += creation(w)
    s += '\n        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);\n'
    s += '        setTitle("Proxy Server");\n'
    s += '        setMinimumSize(new java.awt.Dimension(900, 600));\n'
    s += '        setPreferredSize(new java.awt.Dimension(1000, 680));\n'
    s += ('        addWindowListener(new java.awt.event.WindowAdapter() {\n'
          '            public void windowClosing(java.awt.event.WindowEvent evt) {\n'
          '                formWindowClosing(evt);\n            }\n        });\n')
    for w in widgets:
        if w.kind == 'panel' and w.title:
            s += '\n        %s.setBorder(javax.swing.BorderFactory.createTitledBorder("%s"));\n' % (w.name, jstr(w.title))
        for pn, pt, pv in w.props:
            s += setter(w.name, pn, pt, pv)
        for ev, handler in w.events:
            s += ('        %s.addActionListener(new java.awt.event.ActionListener() {\n'
                  '            public void actionPerformed(java.awt.event.ActionEvent evt) {\n'
                  '                %s(evt);\n            }\n        });\n') % (w.name, handler)
    s += '\n'
    for w in widgets:
        if w.kind == 'scroll':
            s += '        %s.setViewportView(%s);\n' % (w.name, w.inner.name)
    for w in widgets:
        if w.kind == 'chart':
            lay = w.name + 'Layout'
            s += '\n        javax.swing.GroupLayout %s = new javax.swing.GroupLayout(%s);\n        %s.setLayout(%s);\n' % (lay, w.name, w.name, lay)
            s += ('        %s.setHorizontalGroup(\n            %s.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)\n'
                  '            .addGap(0, 700, Short.MAX_VALUE)\n        );\n') % (lay, lay)
            s += ('        %s.setVerticalGroup(\n            %s.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)\n'
                  '            .addGap(0, 170, Short.MAX_VALUE)\n        );\n') % (lay, lay)
        if w.kind == 'panel':
            lay = w.name + 'Layout'
            s += '\n        javax.swing.GroupLayout %s = new javax.swing.GroupLayout(%s);\n        %s.setLayout(%s);\n' % (lay, w.name, w.name, lay)
            s += java_layout(lay, w.h, w.v)
    s += '\n'
    for tab in tabs.children:
        s += '        tabs.addTab("%s", %s);\n' % (jstr(tab.tab), tab.name)
    s += '\n        getContentPane().add(pnlTop, java.awt.BorderLayout.NORTH);\n'
    s += '        getContentPane().add(tabs, java.awt.BorderLayout.CENTER);\n'
    s += '        getContentPane().add(pnlStatus, java.awt.BorderLayout.SOUTH);\n\n'
    s += '        pack();\n    }// </editor-fold>//GEN-END:initComponents\n'
    return s


def make_vars(top, tabs, status):
    widgets = all_widgets(top, tabs, status)
    items = []
    for w in widgets:
        cls = w.cls
        if w.kind == 'chart':
            cls = 'javax.swing.JPanel'
        items.append((cls, w.name))
    order = {}
    for cls, _ in items:
        order.setdefault(cls.split('.')[-1], 0)
    items.sort(key=lambda it: (it[0].split('.')[-1], it[1].lower()))
    s = '    // Variables declaration - do not modify//GEN-BEGIN:variables\n'
    for cls, name in items:
        s += '    private %s %s;\n' % (cls, name)
    s += '    // End of variables declaration//GEN-END:variables\n'
    return s, [n for _, n in items]


def splice(java_path, init, variables):
    text = open(java_path, encoding='utf-8', newline='').read()
    nl = '\r\n' if '\r\n' in text else '\n'
    t = text.replace('\r\n', '\n')
    a = t.index('    @SuppressWarnings("unchecked")\n    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents')
    b = t.index('//GEN-END:initComponents')
    b = t.index('\n', b) + 1
    t = t[:a] + init + t[b:]
    a = t.index('    // Variables declaration - do not modify//GEN-BEGIN:variables')
    b = t.index('//GEN-END:variables')
    b = t.index('\n', b) + 1
    t = t[:a] + variables + t[b:]
    open(java_path, 'w', encoding='utf-8', newline='').write(t.replace('\n', nl))


def main():
    top, tabs, status = build()
    form = make_form(top, tabs, status)
    open(os.path.join(ROOT, 'MainFrame.form'), 'w', encoding='utf-8', newline='').write(form)
    init = make_init(top, tabs, status)
    variables, names = make_vars(top, tabs, status)
    splice(os.path.join(ROOT, 'MainFrame.java'), init, variables)
    import xml.dom.minidom as dom
    dom.parse(os.path.join(ROOT, 'MainFrame.form'))
    print('da sinh MainFrame.form va vung GEN cua MainFrame.java: %d component' % len(names))


if __name__ == '__main__':
    main()
