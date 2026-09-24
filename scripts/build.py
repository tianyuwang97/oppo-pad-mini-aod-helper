"""Build with locally installed Android SDK tools, JDK 17, and a private signing key."""
from pathlib import Path
import argparse
import os
import subprocess
import zipfile

parser = argparse.ArgumentParser()
parser.add_argument('--java-home', required=True, type=Path)
parser.add_argument('--android-jar', required=True, type=Path)
parser.add_argument('--build-tools', required=True, type=Path)
parser.add_argument('--keystore', required=True, type=Path)
parser.add_argument('--alias', default='aodhelper')
args = parser.parse_args()
if not os.environ.get('AOD_KEYSTORE_PASSWORD'):
    parser.error('Set AOD_KEYSTORE_PASSWORD in your environment.')
root = Path(__file__).resolve().parents[1]
app = root / 'app'
build = app / 'build'
classes = build / 'classes'
classes.mkdir(parents=True, exist_ok=True)
dist = root / 'dist'
dist.mkdir(exist_ok=True)
ext = '.exe' if os.name == 'nt' else ''
jdk = args.java_home.resolve() / 'bin'
tools = args.build_tools.resolve()
androidjar = args.android_jar.resolve()
env = dict(os.environ, JAVA_HOME=str(args.java_home.resolve()))

def run(*command):
    subprocess.run([str(x) for x in command], check=True, env=env)

run(tools / ('aapt2' + ext), 'compile', '--dir', app / 'res', '-o', build / 'resources.zip')
run(tools / ('aapt2' + ext), 'link', '-o', build / 'base.apk', '-I', androidjar,
    '--manifest', app / 'AndroidManifest.xml', build / 'resources.zip')
run(jdk / ('javac' + ext), '-encoding', 'UTF-8', '-source', '8', '-target', '8',
    '-Xlint:-options', '-classpath', androidjar, '-d', classes, *sorted((app / 'src').rglob('*.java')))
run(jdk / ('jar' + ext), 'cf', build / 'classes.jar', '-C', classes, '.')
run(jdk / ('java' + ext), '-cp', tools / 'lib/d8.jar', 'com.android.tools.r8.D8',
    '--release', '--min-api', '30', '--lib', androidjar, '--output', build, build / 'classes.jar')
with zipfile.ZipFile(build / 'base.apk', 'a') as archive:
    archive.write(build / 'classes.dex', 'classes.dex', compress_type=zipfile.ZIP_DEFLATED)
run(tools / ('zipalign' + ext), '-f', '4', build / 'base.apk', build / 'aligned.apk')
apk = dist / 'OPPO-AOD-Helper.apk'
run(jdk / ('java' + ext), '-jar', tools / 'lib/apksigner.jar', 'sign',
    '--ks', args.keystore.resolve(), '--ks-key-alias', args.alias,
    '--ks-pass', 'env:AOD_KEYSTORE_PASSWORD', '--out', apk, build / 'aligned.apk')
run(jdk / ('java' + ext), '-jar', tools / 'lib/apksigner.jar', 'verify', '--verbose', apk)
print(apk)
