"""Erzeugt die iPhone-/Web-Version (GitHub Pages) aus assets/index.html."""
import shutil, os, json, hashlib
from PIL import Image
OUT="/home/claude/nutrifit-repo/docs"; os.makedirs(OUT,exist_ok=True)
s=open("assets/index.html").read()
head='''<link rel="manifest" href="manifest.webmanifest">
<link rel="apple-touch-icon" href="icon-180.png">
<link rel="icon" type="image/png" href="icon-192.png">
<meta name="apple-mobile-web-app-title" content="NutriFit">
<meta name="apple-mobile-web-app-status-bar-style" content="default">
</head>'''
s=s.replace("</head>",head,1)
sw='''<script>
if("serviceWorker" in navigator&&location.protocol==="https:"){navigator.serviceWorker.register("sw.js").catch(()=>{});}
</script>
</body>'''
s=s.replace("</body>",sw,1)
open(f"{OUT}/index.html","w").write(s)
shutil.copy("assets/zxing.min.js",f"{OUT}/zxing.min.js")
im=Image.open("/tmp/icon512.png")
for n in (180,192,512): im.resize((n,n),Image.LANCZOS).save(f"{OUT}/icon-{n}.png")
# iOS-Homescreen-Icon ohne Transparenz
bg=Image.new("RGB",(180,180),(15,118,110)); i180=im.resize((180,180),Image.LANCZOS); bg.paste(i180,(0,0),i180); bg.save(f"{OUT}/icon-180.png")
json.dump({"name":"NutriFit","short_name":"NutriFit","start_url":"./","scope":"./","display":"standalone","background_color":"#f4f6f5","theme_color":"#0f766e",
  "icons":[{"src":"icon-192.png","sizes":"192x192","type":"image/png"},{"src":"icon-512.png","sizes":"512x512","type":"image/png","purpose":"any maskable"}]},open(f"{OUT}/manifest.webmanifest","w"),ensure_ascii=False,indent=1)
ver=hashlib.md5(s.encode()).hexdigest()[:8]
open(f"{OUT}/sw.js","w").write(f'''// NutriFit Offline-Cache – Version {ver}
const C="nutrifit-{ver}";const FILES=["./","index.html","zxing.min.js","manifest.webmanifest","icon-180.png","icon-192.png"];
self.addEventListener("install",e=>{{e.waitUntil(caches.open(C).then(c=>c.addAll(FILES)));self.skipWaiting()}});
self.addEventListener("activate",e=>{{e.waitUntil(caches.keys().then(ks=>Promise.all(ks.filter(k=>k!==C).map(k=>caches.delete(k)))));self.clients.claim()}});
self.addEventListener("fetch",e=>{{const u=new URL(e.request.url);if(e.request.method!=="GET"||u.origin!==location.origin)return;
  // Netz zuerst (immer neueste Version), offline aus dem Cache
  e.respondWith(fetch(e.request).then(r=>{{const cp=r.clone();caches.open(C).then(c=>c.put(e.request,cp));return r}}).catch(()=>caches.match(e.request).then(r=>r||caches.match("index.html"))))}});
''')
open(f"{OUT}/.nojekyll","w").write("")
print("web ok",ver)
