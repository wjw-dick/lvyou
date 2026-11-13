import sys, json, os
sys.path.insert(0, '/www/server/panel')
sys.path.insert(0, '/www/server/panel/class')
os.chdir('/www/server/panel')
import public, panelSite

d = sys.argv[1]
p = sys.argv[2]
ps = sys.argv[3] if len(sys.argv) > 3 else 'poyang-crab'

g = public.dict_obj()
g.domain = d
g.webname = json.dumps({"domain": d, "domainlist": [], "count": 0})
g.path = p
g.port = '80'
g.ps = ps
g.type_id = '0'
g.project_type = 'PHP'
g.version = '00'
g.ftp = 'false'
g.sql = 'false'
g.deploy_type = 'default'
print(panelSite.panelSite().AddSite(g))
