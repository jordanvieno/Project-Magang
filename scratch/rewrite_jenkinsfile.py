import re

with open('Jenkinsfile', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Add options
content = content.replace('    triggers {\n', '''    options {
        disableConcurrentBuilds()
        timeout(time: 1, unit: 'HOURS')
        buildDiscarder(logRotator(numToKeepStr: '5'))
    }

    triggers {\n''')

# 2. pollSCM
content = content.replace("pollSCM('* * * * *')", "pollSCM('H/5 * * * *')")

# 3. Helpers
helper_methods = '''
def tg(msg) {
    withCredentials([string(credentialsId: 'Token_Bot_Telegram', variable: 'TG_TOKEN'), string(credentialsId: 'Telegram-Chat-ID', variable: 'TG_CHAT')]) {
        sh """
        curl -s -X POST "https://api.telegram.org/bot${TG_TOKEN}/sendMessage" \\
            --data-urlencode "chat_id=${TG_CHAT}" \\
            --data-urlencode "text=${msg}"
        """
    }
}

def ghStatus(state, desc) {
    withCredentials([string(credentialsId: 'github-status-token', variable: 'GH_TOKEN')]) {
        sh """
        curl -s -X POST \\
          -H "Authorization: token ${GH_TOKEN}" \\
          -H "Accept: application/vnd.github+json" \\
          https://api.github.com/repos/jordanvieno/Project-Magang/statuses/${env.GIT_SHA} \\
          -d '{"state":"${state}","context":"jenkins/quality-gate","description":"${desc}"}'
        """
    }
}

def waitDb(container, user) {
    sh """
    for i in \\$(seq 1 30); do
        docker exec ${container} pg_isready -U ${user} >/dev/null 2>&1 && break
        sleep 1
    done
    docker exec ${container} pg_isready -U ${user} || exit 1
    """
}

pipeline {'''

content = content.replace('pipeline {', helper_methods)

# Replace github status calls
content = re.sub(r"withCredentials\(\[string\(credentialsId: 'github-status-token'.*?'''", "ghStatus('pending', 'Menjalankan unit test & coverage check...')", content, flags=re.DOTALL, count=1)
content = re.sub(r"withCredentials\(\[string\(credentialsId: 'github-status-token'.*?'''", "ghStatus('success', 'Test lolos & coverage memenuhi threshold 70%')", content, flags=re.DOTALL, count=1)
content = re.sub(r"withCredentials\(\[string\(credentialsId: 'github-status-token'.*?'''", "ghStatus('failure', 'Test gagal atau coverage di bawah threshold')", content, flags=re.DOTALL, count=1)


# Fix DB ports and waits
content = content.replace('-p 55432:5432', '-p 127.0.0.1:55432:5432')
content = content.replace('-p 5433:5432', '-p 127.0.0.1:5433:5432')

# Wait fixes
# Test db
content = re.sub(r'echo "Menunggu PostgreSQL siap.*sleep 1\n                    done', 'waitDb("agen46-db-test", "${DB_TEST_USER}")', content, flags=re.DOTALL)
content = re.sub(r'for i in \$\(seq 1 30\); do\s+docker exec agen46-db-dev.*?done', 'waitDb("agen46-db-dev", "${DB_DEV_USER}")', content, flags=re.DOTALL)
content = re.sub(r'for i in \$\(seq 1 20\); do\s+docker exec agen46-db-testing.*?done', 'waitDb("agen46-db-testing", "${DB_SIT_USER}")', content, flags=re.DOTALL)
content = re.sub(r'for i in \$\(seq 1 30\); do\s+docker exec agen46-db-prod.*?done', 'waitDb("agen46-db-prod", "${DB_PROD_USER}")', content, flags=re.DOTALL)

# curl foto
content = content.replace('curl -L -o src/main/resources/static/foto.jpg', 'curl -fsSL --retry 3 -o src/main/resources/static/foto.jpg')

# Nginx config removal
nginx_echo = r"echo 'upstream backend_cluster \{' > nginx-lb\.conf.*?echo '\}' >> nginx-lb\.conf"
content = re.sub(nginx_echo, 'cp /opt/agen46/nginx-lb.conf nginx-lb.conf', content, flags=re.DOTALL)

# Frontend env variables
content = content.replace('agen46-frontend:${BRANCH_NAME}-latest', '-e BACKEND_PORT=80 agen46-frontend:${BRANCH_NAME}-latest')
content = content.replace('agen46-frontend:production-latest', '-e BACKEND_PORT=80 agen46-frontend:production-latest')

# Telegram alerts
content = re.sub(r'withCredentials.*?curl.*?Telegram-Chat-ID.*?\}', 'tg("✅ Pipeline SUKSES — branch: ${env.BRANCH_NAME}, build: #${env.BUILD_NUMBER}")', content, flags=re.DOTALL, count=1)
content = re.sub(r'withCredentials.*?curl.*?Telegram-Chat-ID.*?\}', 'tg("❌ Pipeline GAGAL — branch: ${env.BRANCH_NAME}, build: #${env.BUILD_NUMBER}")', content, flags=re.DOTALL, count=1)
content = re.sub(r'withCredentials.*?curl.*?Telegram-Chat-ID.*?\}', 'tg("⚠️ AUTO-ROLLBACK terjadi di Production! Build #${BUILD_NUMBER} gagal health check, otomatis kembali ke build stabil #${lastStable}")', content, flags=re.DOTALL, count=1)

# Push after trivy scan
# We have a Push step and Scan step. Let's merge them or just move docker push.
containerization_step = '''docker build --no-cache -t ${IMAGE_NAME}:${GIT_SHA} -t ${REGISTRY}/${IMAGE_NAME}:${GIT_SHA} .
                    docker push ${REGISTRY}/${IMAGE_NAME}:${GIT_SHA}
                    docker tag ${IMAGE_NAME}:${GIT_SHA} ${IMAGE_NAME}:${BRANCH_NAME}-${BUILD_NUMBER}
                    docker tag ${IMAGE_NAME}:${GIT_SHA} ${IMAGE_NAME}:${BRANCH_NAME}-latest'''
new_containerization_step = '''docker build --no-cache -t ${IMAGE_NAME}:${GIT_SHA} .'''
content = content.replace(containerization_step, new_containerization_step)

scan_step = '''docker run --rm \\
                  -v /var/run/docker.sock:/var/run/docker.sock \\
                  -v trivy-cache:/root/.cache/ \\
                  aquasec/trivy:latest image \\
                  --severity CRITICAL \\
                  --ignore-unfixed \\
                  --exit-code 1 \\
                  --format table \\
                  ${IMAGE_NAME}:${GIT_SHA}'''
new_scan_step = scan_step + '''
                
                echo 'Fase 1D-bis: Mendorong image ke registry (Setelah lolos scan)'
                withCredentials([usernamePassword(credentialsId: 'REGISTRY_CREDS', passwordVariable: 'REG_PASS', usernameVariable: 'REG_USER')]) {
                    sh \'\'\'
                    docker tag ${IMAGE_NAME}:${GIT_SHA} ${REGISTRY}/${IMAGE_NAME}:${GIT_SHA}
                    docker push ${REGISTRY}/${IMAGE_NAME}:${GIT_SHA}
                    docker tag ${IMAGE_NAME}:${GIT_SHA} ${IMAGE_NAME}:${BRANCH_NAME}-${BUILD_NUMBER}
                    docker tag ${IMAGE_NAME}:${GIT_SHA} ${IMAGE_NAME}:${BRANCH_NAME}-latest
                    \'\'\'
                }'''
content = content.replace(scan_step, new_scan_step)

# docker login 3 times. Just remove the extra ones (in promote image)
content = content.replace('echo "${REG_PASS}" | docker login ${REGISTRY} -u ${REG_USER} --password-stdin\n\n                    docker pull', 'docker pull')

# Unstable post
post_block = '''    post {
        success {'''
new_post_block = '''    post {
        unstable {
            script {
                tg("⚠️ Pipeline UNSTABLE — branch: ${env.BRANCH_NAME}, build: #${env.BUILD_NUMBER}")
            }
            echo "Pipeline UNSTABLE untuk branch: ${env.BRANCH_NAME}"
        }
        success {'''
content = content.replace(post_block, new_post_block)

with open('Jenkinsfile', 'w', encoding='utf-8') as f:
    f.write(content)
