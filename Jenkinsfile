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
    }

    parameters {
        choice(name: 'DEPLOY_ACTION', choices: ['Release', 'Rollback'], description: 'Release = deploy versi terbaru, Rollback = kembali ke versi lama')
        string(name: 'ROLLBACK_VERSION', defaultValue: '', description: 'Isi nomor build yang mau di-rollback (contoh: 6). Kosongkan kalau Release.')
    }

    stages {
        stage('Security Code Scan (SAST)') {
            when { expression { params.DEPLOY_ACTION != 'Rollback' } }
            steps {
                echo 'Fase 0: Analisis Kualitas & Keamanan Kode (Simulasi SonarQube)...'
                sh '''
                echo "========================================================"
                echo "[SAST SCANNER] Memindai repositori Payment & Integration System..."
                echo "[*] Mengecek Hardcoded Secrets / Passwords... AMAN"
                echo "[*] Mengecek SQL Injection Vulnerabilities... AMAN"
                echo "========================================================"
                echo "Status Quality Gate: PASSED"
                '''
            }
        }

        stage('Build & Unit Test') {
            when { expression { params.DEPLOY_ACTION != 'Rollback' } }
            parallel {
                stage('Backend Build (Maven)') {
                    steps {
                        echo 'Fase 1A: Mengompilasi kode Java Backend...'
                        sh 'mvn clean package'
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
            when { expression { params.DEPLOY_ACTION != 'Rollback' } }
            steps {
                echo 'Fase 1C: Membungkus artefak menjadi Docker Image...'
                sh "docker build --no-cache -t ${IMAGE_NAME}:${BRANCH_NAME}-${BUILD_NUMBER} -t ${IMAGE_NAME}:${BRANCH_NAME}-latest ."
            }
        }

        stage('Deploy to Development') {
            when {
                branch 'developmentlinux'
                expression { params.DEPLOY_ACTION != 'Rollback' }
            }
            steps {
                echo 'Deploy ke environment Development (container lokal)...'
                sh '''
                docker rm -f agen46-dev || true
                docker run -d --name agen46-dev -p 8081:8080 -e APP_ENV=development -e BUILD_NUMBER=${BUILD_NUMBER} ${IMAGE_NAME}:${BRANCH_NAME}-latest
                curl "http://localhost:9000/update?stage=development&build=${BUILD_NUMBER}"
                '''
            }
        }

        stage('Deploy to Testing') {
            when {
                branch 'testing'
                expression { params.DEPLOY_ACTION != 'Rollback' }
            }
            steps {
                echo 'Deploy ke environment Testing/SIT (container lokal)...'
                sh '''
                docker rm -f agen46-testing || true
                docker run -d --name agen46-testing -p 8082:8080 -e APP_ENV=testing -e BUILD_NUMBER=${BUILD_NUMBER} ${IMAGE_NAME}:testing-latest
                curl "http://localhost:9000/update?stage=testing&build=${BUILD_NUMBER}"
                '''
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
                        echo 'Deploy ke environment Production (container lokal)...'
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