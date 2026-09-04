pipeline {
    agent any

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
                
        // TAHAP BARU: Membungkus aplikasi menjadi kontainer untuk TKGI
        stage('Containerization (Docker Build)') {
            steps {
                echo 'Fase 1C: Membungkus artefak Backend menjadi Docker Image...'
                // Mengeksekusi Dockerfile untuk membuat image bernama 'agen46-backend'
                bat 'docker build -t agen46-backend:latest .'
            }
        }

    }
                stage('Frontend Build (Node.js/NPM)') {
                    steps {
                        echo 'Fase 1B: Menyimulasikan bundling aset UI React/Angular...'
                        bat 'if not exist build mkdir build'
                        // Menyisipkan variabel %BUILD_NUMBER% agar isi konten berubah setiap kali pipeline dijalankan
                        bat 'echo Antarmuka Agen46 BNI (Rilis Versi %BUILD_NUMBER%) > build\\index.html'
                    }
                }
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
                echo 'Fase 3A (PROD): Mengirim JAR Backend ke klaster APP...'
                bat 'C:\\Windows\\System32\\xcopy.exe target\\*.jar C:\\Server-Prod-Dummy\\ /Y /I'
                
                echo 'Fase 3B (PROD): Mengirim aset statis Frontend ke klaster WEB...'
                // Mengambil file dari folder 'build' yang baru
                bat 'C:\\Windows\\System32\\xcopy.exe build\\* C:\\Server-WEB-Dummy\\ /Y /I /E'
                
                echo 'Deployment menyeluruh ke Production berhasil!'
            }
        }
    }
    
    post {
        always {
            bat 'mvn clean'
        }
    }
}
