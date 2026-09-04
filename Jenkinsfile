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
                stage('Frontend Build (Node.js/NPM)') {
                    steps {
                        echo 'Fase 1B: Menyimulasikan bundling aset UI React/Angular...'
                        // Membuat folder simulasi hasil npm run build
                        bat 'if not exist target\\frontend_build mkdir target\\frontend_build'
                        bat 'echo Antarmuka Agen46 BNI > target\\frontend_build\\index.html'
                    }
                }
            }
        }

        // (Biarkan tahap Deploy to SIT dan Approval for Production tetap seperti sebelumnya)
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
                // Parameter /E menyalin seluruh subdirektori (simulasi distribusi file web)
                bat 'C:\\Windows\\System32\\xcopy.exe target\\frontend_build\\* C:\\Server-WEB-Dummy\\ /Y /I /E'
                
                echo 'Deployment menyeluruh ke Production berhasil!'
            }
        }
    }
    }
    
    post {
        always {
            bat 'mvn clean'
        }
    }
