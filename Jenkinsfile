pipeline {
    agent {
        label 'ec2-master'
    }
    
    tools {
        nodejs "nodejs"
    }

    parameters {
        string(name: 'LOB_NAME', defaultValue: 'new-lob', description: 'Unique name for the new Line of Business')
        string(name: 'ENV', defaultValue: 'dev', description: 'Target environment (e.g., dev, qa, prod)')
        string(name: 'REGION', defaultValue: 'ap-south-1', description: 'Target AWS Region')
        string(name: 'TERRAGRUNT_INPUTS', defaultValue: '{}', description: 'JSON string of terragrunt inputs to override')
        string(name: 'FLINK_PROPERTIES', defaultValue: '{}', description: 'JSON string of flink properties to override')
    }

    stages {
        stage('Checkout') {
            steps {
                script {
                   cleanWs()
                    def branchName = "standard-deployment"
                    echo "Checking out branch: ${branchName}"
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: branchName]],
                        userRemoteConfigs: scm.userRemoteConfigs
                    ])
                }
            }
        }
        stage('Install Dependencies') {
            steps {
                script {
                    // Install Node.js and npm
                    sh '''
                        if ! command -v node >/dev/null 2>&1; then
                          curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
                          sudo apt-get install -y nodejs
                        fi
                    '''
                    // Install Terraform
                    sh '''
                        if ! command -v terraform >/dev/null 2>&1; then
                          wget https://releases.hashicorp.com/terraform/1.6.6/terraform_1.6.6_linux_amd64.zip
                          unzip -o terraform_1.6.6_linux_amd64.zip
                          sudo mv terraform /usr/local/bin/
                        fi
                    '''
                    // Install Terragrunt
                    sh '''
                        if ! command -v terragrunt >/dev/null 2>&1; then
                          wget https://github.com/gruntwork-io/terragrunt/releases/download/v0.58.11/terragrunt_linux_amd64
                          chmod +x terragrunt_linux_amd64
                          sudo mv terragrunt_linux_amd64 /usr/local/bin/terragrunt
                        fi
                    '''
                }
            }
        }
        stage('Setup LOB Environment') {
            steps {
                script {
                    // Ensure Node.js is available in your Jenkins agent environment
                    sh "npm install"
                    sh "node scripts/setup-lob.js --lob '${params.LOB_NAME}' --env '${params.ENV}' --region '${params.REGION}' --terragrunt-inputs '${params.TERRAGRUNT_INPUTS}' --flink-properties '${params.FLINK_PROPERTIES}'"
                    
                    // Commit the generated files
                    sh "git add environments/${params.ENV}/${params.REGION}/${params.LOB_NAME}/"
                    sh "git commit -m 'feat: Add/Update LOB ${params.LOB_NAME}'"
                }
            }
        }

        stage('Deploy Infrastructure (Manual Trigger)') {
            // This stage is for demonstrating the next step.
            // You might run this automatically or have a manual approval.
            input {
                message "Deploy infrastructure for ${params.LOB_NAME}?"
                ok "Yes, deploy"
            }
            steps {
                dir("environments/${params.ENV}/${params.REGION}/${params.LOB_NAME}") {

                    withAWS(region: 'ap-south-1', credentials: 'dev_ui_build') {
                        // Ensure Terragrunt is installed on your Jenkins agent
                        // sh 'terragrunt run-all apply --terragrunt-non-interactive'
                        sh 'pwd'
                        sh 'ls -larth'
                    }

                }
            }
        }
    }
}
