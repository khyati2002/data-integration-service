pipeline {
    agent any

    parameters {
        string(name: 'LOB_NAME', defaultValue: 'new-lob', description: 'Unique name for the new Line of Business')
        string(name: 'ENV', defaultValue: 'dev', description: 'Target environment (e.g., dev, qa, prod)')
        string(name: 'REGION', defaultValue: 'ap-south-1', description: 'Target AWS Region')
    }

    stages {
        stage('Setup LOB Environment') {
            steps {
                script {
                    // Ensure Node.js is available in your Jenkins agent environment
                    sh "node scripts/setup-lob.js --lob '${params.LOB_NAME}' --env '${params.ENV}' --region '${params.REGION}'"
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
                    // Ensure Terragrunt is installed on your Jenkins agent
                    sh 'terragrunt run-all apply --terragrunt-non-interactive'
                }
            }
        }
    }
}
