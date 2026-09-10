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

    stages {
        stage('Security Code Scan (SAST)') {
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
            steps {
                echo 'Fase 1C: Membungkus artefak menjadi Docker Image...'
                sh "docker build --no-cache -t ${IMAGE_NAME}:${BRANCH_NAME}-${BUILD_NUMBER} -t ${IMAGE_NAME}:${BRANCH_NAME}-latest ."
            }
        }

        stage('Deploy to Development') {
    when { branch 'developmentlinux' }
    steps {
        echo 'Deploy ke environment Development (container lokal)...'
        sh '''
        docker rm -f agen46-dev || true
        docker run -d --name agen46-dev -p 8081:8080 -e APP_ENV=development -e BUILD_NUMBER=${BUILD_NUMBER} ${IMAGE_NAME}:development-latest
        '''
    }
}

stage('Deploy to Testing') {
    when { branch 'testing' }
    steps {
        echo 'Deploy ke environment Testing/SIT (container lokal)...'
        sh '''
        docker rm -f agen46-testing || true
        docker run -d --name agen46-testing -p 8082:8080 -e APP_ENV=testing -e BUILD_NUMBER=${BUILD_NUMBER} ${IMAGE_NAME}:testing-latest
        '''
    }
}


        stage('Approval for Production') {
            when { branch 'production' }
            steps {
                echo 'Menunggu otorisasi rilis ke Production...'
                input message: 'Artefak sudah lolos Testing. Setujui deployment ke Production?', ok: 'Deploy Sekarang'
            }
        }

        stage('Deploy to Production') {
    when { branch 'production' }
    steps {
        echo 'Deploy ke environment Production (container lokal)...'
        sh '''
        docker rm -f agen46-prod || true
        docker run -d --name agen46-prod -p 8083:8080 -e APP_ENV=production -e BUILD_NUMBER=${BUILD_NUMBER} ${IMAGE_NAME}:production-latest
        '''
    }
}
    }

    post {
        success {
            echo "Pipeline sukses untuk branch: ${env.BRANCH_NAME}"
        }
        failure {
            echo "Pipeline GAGAL untuk branch: ${env.BRANCH_NAME}"
        }
        always {
            sh 'mvn clean'
        }
    }
}