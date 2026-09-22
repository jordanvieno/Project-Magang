pipeline {
    agent any

    triggers {
        pollSCM('* * * * *')
    }

    tools {
        maven 'maven-3.9.9'
    }

    environment {
        IMAGE_NAME = 'agen46-backend'
        REGISTRY = 'localhost:5050'
    }

    parameters {
        choice(name: 'DEPLOY_ACTION', choices: ['Release', 'Rollback'], description: 'Release = deploy versi terbaru, Rollback = kembali ke versi lama')
        string(name: 'ROLLBACK_VERSION', defaultValue: '', description: 'Isi nomor build yang mau di-rollback (contoh: 6). Kosongkan kalau Release.')
    }

    stages {

        stage('Test & Quality Gate') {
            when { expression { params.DEPLOY_ACTION != 'Rollback' } }
            steps {
                script {
                    env.GIT_SHA = sh(script: 'git rev-parse HEAD', returnStdout: true).trim()
                }
                withCredentials([string(credentialsId: 'github-status-token', variable: 'GH_TOKEN')]) {
                    sh '''
                    curl -s -X POST \
                      -H "Authorization: token ${GH_TOKEN}" \
                      -H "Accept: application/vnd.github+json" \
                      https://api.github.com/repos/jordanvieno/Project-Magang/statuses/${GIT_SHA} \
                      -d '{"state":"pending","context":"jenkins/quality-gate","description":"Menjalankan unit test & coverage check..."}'
                    '''
                }
                echo 'Menyiapkan PostgreSQL sementara untuk testing...'
                withCredentials([usernamePassword(credentialsId: 'agen46-db-test-credentials', usernameVariable: 'DB_TEST_USER', passwordVariable: 'DB_TEST_PASS')]) {
                    sh '''
                    docker rm -f agen46-db-test || true
                    docker run -d --name agen46-db-test -p 55432:5432 -e POSTGRES_USER=${DB_TEST_USER} -e POSTGRES_PASSWORD=${DB_TEST_PASS} -e POSTGRES_DB=agen46_test postgres:16-alpine

                    echo "Menunggu PostgreSQL siap..."
                    for i in $(seq 1 15); do
                        if docker exec agen46-db-test pg_isready -U ${DB_TEST_USER} > /dev/null 2>&1; then
                            echo "PostgreSQL siap."
                            break
                        fi
                        sleep 1
                    done
                    '''
                    echo 'Fase 0: Menjalankan Unit Test & Coverage Check (JUnit 5 + Jacoco)...'
                    withEnv([
                        'DB_HOST=localhost',
                        'DB_PORT=55432',
                        'DB_NAME=agen46_test'
                    ]) {
                        sh 'DB_USER=${DB_TEST_USER} DB_PASSWORD=${DB_TEST_PASS} mvn clean verify'
                    }
                }
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
                    jacoco execPattern: 'target/jacoco.exec'
                    sh 'docker rm -f agen46-db-test || true'
                }
                success {
                    sh 'sudo /usr/local/bin/deploy-statusserver.sh'
                    withCredentials([string(credentialsId: 'github-status-token', variable: 'GH_TOKEN')]) {
                        sh '''
                        curl -s -X POST \
                          -H "Authorization: token ${GH_TOKEN}" \
                          -H "Accept: application/vnd.github+json" \
                          https://api.github.com/repos/jordanvieno/Project-Magang/statuses/${GIT_SHA} \
                          -d '{"state":"success","context":"jenkins/quality-gate","description":"Test lolos & coverage memenuhi threshold 70%"}'
                        '''
                    }
                    echo 'Status Quality Gate: PASSED'
                }
                failure {
                    withCredentials([string(credentialsId: 'github-status-token', variable: 'GH_TOKEN')]) {
                        sh '''
                        curl -s -X POST \
                          -H "Authorization: token ${GH_TOKEN}" \
                          -H "Accept: application/vnd.github+json" \
                          https://api.github.com/repos/jordanvieno/Project-Magang/statuses/${GIT_SHA} \
                          -d '{"state":"failure","context":"jenkins/quality-gate","description":"Test gagal atau coverage di bawah threshold"}'
                        '''
                    }
                    echo 'Status Quality Gate: FAILED — pipeline dihentikan.'
                }
            }
        }

        stage('Build & Package') {
            when {
                branch 'developmentlinux'
                expression { params.DEPLOY_ACTION != 'Rollback' }
            }
            parallel {
                stage('Backend Build (Maven)') {
                    steps {
                        echo 'Fase 1A: Mengompilasi & packaging Java Backend (test sudah lolos gate, di-skip di sini)...'
                        sh '''
                        mkdir -p src/main/resources/static
                        COMMIT_SHORT=$(git rev-parse --short HEAD)
                        curl -L -o src/main/resources/static/foto.jpg "https://picsum.photos/seed/${COMMIT_SHORT}/400/300"
                        mvn package -DskipTests
                        echo "=== VERIFIKASI: cek META-INF/services di jar hasil build ==="
                        jar tf target/digital-channel-app-1.0-SNAPSHOT.jar | grep "META-INF/services" || echo "PERINGATAN: META-INF/services TIDAK DITEMUKAN di jar!"
                        '''
                    }
                }
                stage('Frontend Build (Simulasi)') {
                    steps {
                        echo 'Fase 1B: Menyimulasikan bundling aset UI...'
                        sh '''
                        mkdir -p build
                        echo "Antarmuka Agen46 BNI (Rilis Versi ${BUILD_NUMBER})" > build/index.html
                        '''
                    }
                }
            }
        }

        stage('Containerization (Docker Build)') {
            when {
                branch 'developmentlinux'
                expression { params.DEPLOY_ACTION != 'Rollback' }
            }
            steps {
                echo 'Fase 1C: Membungkus artefak menjadi Docker Image (single source of truth), push ke registry...'
                sh '''
                docker build --no-cache -t ${IMAGE_NAME}:${GIT_SHA} -t ${REGISTRY}/${IMAGE_NAME}:${GIT_SHA} .
                docker push ${REGISTRY}/${IMAGE_NAME}:${GIT_SHA}
                docker tag ${IMAGE_NAME}:${GIT_SHA} ${IMAGE_NAME}:${BRANCH_NAME}-${BUILD_NUMBER}
                docker tag ${IMAGE_NAME}:${GIT_SHA} ${IMAGE_NAME}:${BRANCH_NAME}-latest
                '''
            }
        }

        stage('Security Scan (Trivy)') {
            when {
                branch 'developmentlinux'
                expression { params.DEPLOY_ACTION != 'Rollback' }
            }
            steps {
                echo 'Fase 1D: Memindai image Docker dengan Trivy untuk kerentanan (CVE)...'
                sh '''
                docker run --rm \
                  -v /var/run/docker.sock:/var/run/docker.sock \
                  -v trivy-cache:/root/.cache/ \
                  aquasec/trivy:latest image \
                  --severity HIGH,CRITICAL \
                  --exit-code 0 \
                  --format table \
                  ${IMAGE_NAME}:${GIT_SHA}
                '''
            }
        }

        stage('Promote Image') {
            when {
                anyOf {
                    branch 'testing'
                    branch 'production'
                }
                expression { params.DEPLOY_ACTION != 'Rollback' }
            }
            steps {
                echo "Fase 1E: Mengambil image yang SAMA persis dari registry (commit ${env.GIT_SHA}), tanpa build ulang..."
                sh '''
                docker pull ${REGISTRY}/${IMAGE_NAME}:${GIT_SHA}
                docker tag ${REGISTRY}/${IMAGE_NAME}:${GIT_SHA} ${IMAGE_NAME}:${BRANCH_NAME}-${BUILD_NUMBER}
                docker tag ${REGISTRY}/${IMAGE_NAME}:${GIT_SHA} ${IMAGE_NAME}:${BRANCH_NAME}-latest
                '''
            }
        }

        stage('Deploy to Development') {
    when {
        branch 'developmentlinux'
        expression { params.DEPLOY_ACTION != 'Rollback' }
    }
    steps {
        echo 'Deploy ke environment Development (container lokal)...'
        withCredentials([usernamePassword(credentialsId: 'agen46-db-dev-credentials', usernameVariable: 'DB_DEV_USER', passwordVariable: 'DB_DEV_PASS')]) {
            sh '''
            docker network create agen46-net || true
            docker rm -f agen46-dev || true
            docker run -d --name agen46-dev --network agen46-net -p 8081:8080 -e APP_ENV=development -e BUILD_NUMBER=${BUILD_NUMBER} -e DB_HOST=agen46-db-dev -e DB_PORT=5432 -e DB_NAME=agen46_dev -e DB_USER=${DB_DEV_USER} -e DB_PASSWORD=${DB_DEV_PASS} ${IMAGE_NAME}:${BRANCH_NAME}-latest
            curl "http://localhost:9000/update?stage=development&build=${BUILD_NUMBER}"
            '''
        }
        script {
            echo 'Smoke test: memverifikasi endpoint Development merespons...'
            sleep(time: 3, unit: 'SECONDS')
            def statusCode = sh(
                script: "curl -s -o /dev/null -w '%{http_code}' http://localhost:8081/api/v1/payments/health || true",
                returnStdout: true
            ).trim()
            echo "Smoke test Development: HTTP status = ${statusCode}"
            if (statusCode != '200') {
                unstable("Smoke test GAGAL di Development — endpoint tidak merespons 200 (status: ${statusCode})")
            } else {
                echo "Smoke test LOLOS."
            }
        }
    }
}
      stage('Approval for Testing') {
    when {
        branch 'testing'
        expression { params.DEPLOY_ACTION != 'Rollback' }
    }
    steps {
        echo 'Menunggu otorisasi sebelum masuk ke environment Testing...'
        input message: 'Build sudah lolos Quality Gate, Security Scan, dan Promote Image. Setujui deployment ke Testing?', ok: 'Deploy ke Testing'
    }
}
       stage('Deploy to Testing') {
    when {
        branch 'testing'
        expression { params.DEPLOY_ACTION != 'Rollback' }
    }
    steps {
        echo 'Deploy ke environment Testing/SIT (container lokal, image hasil promote dari Development)...'
        sh '''
        docker rm -f agen46-testing || true
        docker run -d --name agen46-testing -p 8082:8080 -e APP_ENV=testing -e BUILD_NUMBER=${BUILD_NUMBER} ${IMAGE_NAME}:testing-latest
        curl "http://localhost:9000/update?stage=testing&build=${BUILD_NUMBER}"
        '''
        script {
            echo 'Smoke test: memverifikasi endpoint Testing merespons...'
            sleep(time: 3, unit: 'SECONDS')
            def statusCode = sh(
                script: "curl -s -o /dev/null -w '%{http_code}' http://localhost:8082/api/v1/payments/health || true",
                returnStdout: true
            ).trim()
            echo "Smoke test Testing: HTTP status = ${statusCode}"
            if (statusCode != '200') {
                unstable("Smoke test GAGAL di Testing — endpoint tidak merespons 200 (status: ${statusCode})")
            } else {
                echo "Smoke test LOLOS."
            }
        }
    }
}

        stage('Approval for Production') {
            when {
                branch 'production'
                expression { params.DEPLOY_ACTION != 'Rollback' }
            }
            steps {
                echo 'Menunggu otorisasi rilis ke Production...'
                input message: 'Artefak sudah lolos Testing. Setujui deployment ke Production?', ok: 'Deploy Sekarang'
            }
        }

        stage('Deploy to Production') {
            when { branch 'production' }
            steps {
                script {
                    if (params.DEPLOY_ACTION == 'Rollback') {
                        echo "ROLLBACK MANUAL ke versi production-${params.ROLLBACK_VERSION}..."
                        sh """
                        docker rm -f agen46-prod || true
                        docker run -d --name agen46-prod -p 8083:8080 -e APP_ENV=production -e BUILD_NUMBER=${params.ROLLBACK_VERSION} ${IMAGE_NAME}:production-${params.ROLLBACK_VERSION}
                        curl "http://localhost:9000/update?stage=production-rollback&build=${params.ROLLBACK_VERSION}"
                        """
                    } else {
                        echo 'Deploy ke environment Production (container lokal, image hasil promote dari Testing)...'
                        sh """
                        docker rm -f agen46-prod || true
                        docker run -d --name agen46-prod -p 8083:8080 -e APP_ENV=production -e BUILD_NUMBER=${BUILD_NUMBER} ${IMAGE_NAME}:production-latest
                        """

                        echo 'Menunggu aplikasi siap, melakukan health check...'
                        def healthy = false
                        for (int i = 1; i <= 5; i++) {
                            sleep(time: 3, unit: 'SECONDS')
                            def statusCode = sh(
                                script: "curl -s -o /dev/null -w '%{http_code}' http://localhost:8083/api/v1/payments/health || true",
                                returnStdout: true
                            ).trim()
                            echo "Percobaan ${i}: HTTP status = ${statusCode}"
                            if (statusCode == '200') {
                                healthy = true
                                break
                            }
                        }

                        if (healthy) {
                            echo "Health check LOLOS. Build ${BUILD_NUMBER} sekarang jadi versi stabil."
                            sh """
                            echo ${BUILD_NUMBER} > /var/lib/jenkins/agen46-last-stable-production.txt
                            curl "http://localhost:9000/update?stage=production&build=${BUILD_NUMBER}"
                            """
                        } else {
                            echo "Health check GAGAL setelah 5 percobaan. Menjalankan AUTO-ROLLBACK..."
                            def lastStable = sh(
                                script: "cat /var/lib/jenkins/agen46-last-stable-production.txt 2>/dev/null || echo ''",
                                returnStdout: true
                            ).trim()

                            if (lastStable == '') {
                                error("Tidak ada versi stabil sebelumnya untuk rollback! Deployment dibatalkan, perlu intervensi manual.")
                            } else {
                                sh """
                                docker rm -f agen46-prod || true
                                docker run -d --name agen46-prod -p 8083:8080 -e APP_ENV=production -e BUILD_NUMBER=${lastStable} ${IMAGE_NAME}:production-${lastStable}
                                curl "http://localhost:9000/update?stage=production-rollback-auto&build=${lastStable}"
                                """
                                withCredentials([string(credentialsId: 'Token_Bot_Telegram', variable: 'TG_TOKEN'), string(credentialsId: 'Telegram-Chat-ID', variable: 'TG_CHAT')]) {
                                    sh """
                                    curl -s -X POST "https://api.telegram.org/bot\${TG_TOKEN}/sendMessage" \
                                        -d chat_id=\${TG_CHAT} \
                                        -d text="⚠️ AUTO-ROLLBACK terjadi di Production! Build #${BUILD_NUMBER} gagal health check, otomatis kembali ke build stabil #${lastStable}"
                                    """
                                }
                                error("Auto-rollback dijalankan ke build ${lastStable} karena health check build ${BUILD_NUMBER} gagal.")
                            }
                        }
                    }
                }
            }
        }
    }

    post {
        success {
            script {
                withCredentials([string(credentialsId: 'Token_Bot_Telegram', variable: 'TG_TOKEN'), string(credentialsId: 'Telegram-Chat-ID', variable: 'TG_CHAT')]) {
                    sh """
                    curl -s -X POST "https://api.telegram.org/bot\${TG_TOKEN}/sendMessage" \
                        -d chat_id=\${TG_CHAT} \
                        -d text="✅ Pipeline SUKSES — branch: ${env.BRANCH_NAME}, build: #${env.BUILD_NUMBER}"
                    """
                }
            }
            echo "Pipeline sukses untuk branch: ${env.BRANCH_NAME}"
        }
        failure {
            script {
                withCredentials([string(credentialsId: 'Token_Bot_Telegram', variable: 'TG_TOKEN'), string(credentialsId: 'Telegram-Chat-ID', variable: 'TG_CHAT')]) {
                    sh """
                    curl -s -X POST "https://api.telegram.org/bot\${TG_TOKEN}/sendMessage" \
                        -d chat_id=\${TG_CHAT} \
                        -d text="❌ Pipeline GAGAL — branch: ${env.BRANCH_NAME}, build: #${env.BUILD_NUMBER}"
                    """
                }
            }
            echo "Pipeline GAGAL untuk branch: ${env.BRANCH_NAME}"
        }
        always {
            script {
                if (params.DEPLOY_ACTION != 'Rollback') {
                    sh 'mvn clean'
                }
            }
        }
    }
}