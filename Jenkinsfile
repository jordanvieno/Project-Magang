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

        stage('Test & Quality Gate') {
            when { expression { params.DEPLOY_ACTION != 'Rollback' } }
            steps {
                echo 'Fase 0: Menjalankan Unit Test & Coverage Check (JUnit 5 + Jacoco)...'
                sh 'mvn clean verify'
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
                    jacoco execPattern: 'target/jacoco.exec'
                }
                success {
                    echo 'Status Quality Gate: PASSED — test lolos & coverage memenuhi threshold 70%.'
                }
                failure {
                    echo 'Status Quality Gate: FAILED — test gagal atau coverage di bawah threshold. Pipeline dihentikan, tidak lanjut ke deploy.'
                }
            }
        }

        stage('Build & Package') {
            when { expression { params.DEPLOY_ACTION != 'Rollback' } }
            parallel {
                stage('Backend Build (Maven)') {
                    steps {
                         echo 'Fase 1A: Mengompilasi & packaging Java Backend (test sudah lolos gate, di-skip di sini)...'
                         sh '''
                         mkdir -p src/main/resources/static
                         COMMIT_SHORT=$(git rev-parse --short HEAD)
                         curl -L -o src/main/resources/static/foto.jpg "https://picsum.photos/seed/${COMMIT_SHORT}/400/300"
                         mvn package -DskipTests
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
            when { expression { params.DEPLOY_ACTION != 'Rollback' } }
            steps {
                echo 'Fase 1C: Membungkus artefak menjadi Docker Image...'
                sh "docker build --no-cache -t ${IMAGE_NAME}:${BRANCH_NAME}-${BUILD_NUMBER} -t ${IMAGE_NAME}:${BRANCH_NAME}-latest ."