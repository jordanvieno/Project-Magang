pipeline {
    agent any

    tools {
        maven 'maven-3.9.9'
    }

    stages {
        stage('Build & Unit Test') {
            steps {
                echo 'Fase 1: Mengompilasi kode dan menjalankan pengujian otomatis...'
                bat 'mvn clean package'
            }
        }

        stage('Deploy to SIT') {
            when {
                branch 'develop'
            }
            steps {
                echo 'Fase 2 (SIT): Cabang develop terdeteksi. Mengirim artefak ke server System Integration Testing...'
                // Simulasi perintah pengiriman ke server SIT
                bat 'echo Deploying to SIT Environment...'
            }
        }
stage('Approval for Production') {
            when {
                // Fleksibel mendeteksi nama cabang yang berakhiran 'main'
                expression { env.GIT_BRANCH != null && env.GIT_BRANCH.endsWith('main') }
            }
            steps {
                echo 'Menunggu otorisasi rilis...'
                input message: 'Validasi artefak siap? Setujui deployment ke server Production?', ok: 'Deploy Sekarang'
            }
        }

        stage('Deploy to Production') {
            when {
                expression { env.GIT_BRANCH != null && env.GIT_BRANCH.endsWith('main') }
            }
            steps {
                echo 'Fase 3 (PROD): Mengirim artefak ke server utama...'
                bat 'C:\\Windows\\System32\\xcopy.exe target\\*.jar C:\\Server-Prod-Dummy\\ /Y /I'
                echo 'Deployment ke Production berhasil!'
            }
        }
    }
    
    post {
        always {
            bat 'mvn clean'
        }
    }
}
