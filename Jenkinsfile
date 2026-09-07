pipeline {
    agent any
    environment {
        PATH = "C:\\Program Files\\Docker\\Docker\\resources\\bin;${env.PATH}"
    }
    tools {
        maven 'maven-3.9.9'
    }

    stages {
        stage('Security Code Scan (SAST)') {
            steps {
                echo 'Fase 0: Analisis Kualitas & Keamanan Kode (Simulasi SonarQube)...'
                bat '''
                echo ========================================================
                echo [SAST SCANNER] Memindai repositori Payment & Integration System...
                echo [*] Mengecek Hardcoded Secrets / Passwords... AMAN
                echo [*] Mengecek SQL Injection Vulnerabilities... AMAN
                echo [*] Mengecek standar Log Enterprise... AMAN
                echo ========================================================
                echo Status Quality Gate: PASSED
                '''
            }
        }
        stage('Build & Unit Test') {
            parallel {
                stage('Backend Build (Maven)') {
                    steps {
                        echo 'Fase 1A: Mengompilasi kode Java Backend...'
                        bat 'mvn clean package'
                    }
                }
                stage('Frontend Build (Node.js/NPM)') {
                    steps {
                        echo 'Fase 1B: Menyimulasikan bundling aset UI React/Angular...'
                        bat 'if not exist build mkdir build'
                        bat 'echo Antarmuka Agen46 BNI (Rilis Versi %BUILD_NUMBER%) > build\\index.html'
                    }
                }
            }
        }

        // TAHAP BARU: Membungkus aplikasi menjadi kontainer untuk TKGI diletakkan di LUAR blok parallel
        stage('Containerization (Docker Build)') {
            steps {
                echo 'Fase 1C: Membungkus artefak Backend menjadi Docker Image...'
                // Mengeksekusi Dockerfile untuk membuat image bernama 'agen46-backend'
                bat 'docker build --no-cache -t agen46-backend:latest .'
            }
        }

        stage('Deploy to SIT') {
            when {
                expression { env.GIT_BRANCH == 'origin/develop' }
            }
            steps {
                echo 'Fase 2 (SIT): Mengirim artefak ke server System Integration Testing...'
                bat 'C:\\Windows\\System32\\xcopy.exe target\\*.jar C:\\Server-SIT-Dummy\\ /Y /I'
            }
        }

        stage('Approval for Production') {
            when {
                expression { env.GIT_BRANCH != null && env.GIT_BRANCH.endsWith('main') }
            }
            steps {
                echo 'Menunggu otorisasi rilis...'
                input message: 'Validasi artefak siap? Setujui deployment Backend & Frontend ke server Production?', ok: 'Deploy Sekarang'
            }
        }

        stage('Deploy to Production') {
            when {
                expression { env.GIT_BRANCH != null && env.GIT_BRANCH.endsWith('main') }
            }
            steps {
                echo 'Fase 3A (PROD): Simulasi Orkestrasi TKGI untuk Backend...'
                // Menyimulasikan penerapan manifest Kubernetes ke cluster
                bat 'echo [KUBERNETES] Mengeksekusi: kubectl apply -f k8s\\backend-deployment.yaml'
                bat 'echo deployment.apps/agen46-backend-deployment created'
                bat 'echo service/agen46-backend-service created'
                
                echo 'Fase 3B (PROD): Menyalakan Nginx Web Server untuk Frontend (Simulasi WEB Cluster)...'
                bat 'docker rm -f agen46-web-server || exit 0'
                bat 'docker run -d -p 80:80 --name agen46-web-server -v "%WORKSPACE%\\build":/usr/share/nginx/html nginx:alpine'
                
                echo 'Deployment menyeluruh ke Production berhasil disimulasikan!'
            }
        }
    }
   post {
        success {
            echo '=================================================='
            echo ' [NOTIFIKASI ENTERPRISE]: Rilis Berhasil!'
            echo ' Status: Pipeline sukses melewati Quality Gate & Deploy.'
            echo ' Target: Backend (APP), Frontend (WEB), & Docker (TKGI) aman.'
            echo '=================================================='
        }
        failure {
            echo '=================================================='
            echo ' [NOTIFIKASI ENTERPRISE]: Rilis GAGAL!'
            echo ' Status: Terdeteksi kesalahan pada kompilasi atau uji coba.'
            echo ' Tindakan: Harap segera periksa log error di Jenkins.'
            echo '=================================================='
        }
        always {
            echo ' Membersihkan workspace lokal...'
            bat 'mvn clean'
        }
    }
    }
