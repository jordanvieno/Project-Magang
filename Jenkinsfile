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
                expression { env.GIT_BRANCH == 'origin/develop' }
            }
            steps {
                echo 'Fase 2 (SIT): Mengirim artefak ke server System Integration Testing...'
                // Menggunakan jalur absolut C:\Windows\System32\xcopy.exe
                bat 'C:\\Windows\\System32\\xcopy.exe target\\*.jar C:\\Server-SIT-Dummy\\ /Y /I'
                echo 'Deployment ke SIT berhasil!'
            }
        }

        stage('Approval for Production') {
            when {
                expression { env.GIT_BRANCH == 'origin/main' } 
            }
            steps {
                echo 'Menunggu otorisasi rilis...'
                input message: 'Validasi artefak siap? Setujui deployment ke server Production?', ok: 'Deploy Sekarang'
            }
        }

        stage('Deploy to Production') {
        when {
            expression { env.GIT_BRANCH == 'origin/main' }
        }
        steps {
            echo 'Fase 3 (PROD): Mengirim artefak ke server utama...'
            bat 'C:\\Windows\\System32\\xcopy.exe target\\*.jar C:\\Server-Prod-Dummy\\ /Y /I'
            echo 'Deployment ke Production berhasil!'
        }
    }
    
    post {
        always {
            bat 'mvn clean'
        }
    }
}