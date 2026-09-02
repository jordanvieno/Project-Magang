stage('Deploy to SIT') {
            when {
                // Membaca nama cabang dari plugin Git
                expression { env.GIT_BRANCH == 'origin/develop' }
            }
            steps {
                echo 'Fase 2 (SIT): Cabang develop terdeteksi. Mengirim artefak ke server System Integration Testing...'
                bat 'echo Deploying to SIT Environment...'
            }
        }

        stage('Approval for Production') {
            when {
                // Gunakan origin/main atau origin/master sesuai nama cabang utamamu
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
                echo 'Fase 3 (PROD): Cabang main terdeteksi. Mengirim artefak ke server utama...'
                bat 'echo Deploying to Production Environment...'
            }
        }