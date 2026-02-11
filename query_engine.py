import subprocess
import time

def get_engine_options(jar_path):
    cmd = ["java", "--add-modules=jdk.incubator.vector", "-jar", jar_path]
    proc = subprocess.Popen(cmd, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    
    proc.stdin.write("uci\n")
    proc.stdin.flush()
    
    options = []
    start_time = time.time()
    while time.time() - start_time < 5:
        line = proc.stdout.readline()
        if not line:
            break
        print(line.strip())
        if "uciok" in line:
            break
            
    proc.stdin.write("quit\n")
    proc.stdin.flush()
    proc.terminate()

if __name__ == "__main__":
    get_engine_options("Serendipity-1.0.0.jar")
