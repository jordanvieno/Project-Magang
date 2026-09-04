pipeline {
    agent any
    environment {
        PATH = "C:\\Program Files\\Docker\\Docker\\resources\\bin;${env.PATH}"
    }
    tools {
        maven 'maven-3.9.9'
    }

    stages {
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
                echo 'Fase 3A (PROD): Menjalankan Backend di Kontainer Lokal (Simulasi TKGI)...'
                // Mematikan kontainer lama jika ada agar tidak bentrok
                bat 'docker rm -f agen46-app-server || exit 0'
                // Menyalakan kontainer baru dari image yang baru saja dibuat di port 8080
                bat 'docker run -d -p 8080:8080 --name agen46-app-server agen46-backend:latest'
                
                echo 'Fase 3B (PROD): Menyalakan Nginx Web Server untuk Frontend (Simulasi WEB Cluster)...'
                bat 'docker rm -f agen46-web-server || exit 0'
                // Memasang folder 'build' milikmu ke dalam web server Nginx di port 80
                bat 'docker run -d -p 80:80 --name agen46-web-server -v "%WORKSPACE%\\build":/usr/share/nginx/html nginx:alpine'
                
                echo 'Deployment menyeluruh ke Production berhasil dan server telah menyala!'
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
}
